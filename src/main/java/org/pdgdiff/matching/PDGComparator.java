package org.pdgdiff.matching;

import com.google.gson.JsonObject;
import org.pdgdiff.edit.ClassMetadataDiffGenerator;
import org.pdgdiff.edit.EditDistanceCalculator;
import org.pdgdiff.edit.EditScriptGenerator;
import org.pdgdiff.edit.RecoveryProcessor;
import org.pdgdiff.edit.model.EditOperation;
import org.pdgdiff.graph.CycleDetection;
import org.pdgdiff.graph.GraphTraversal;
import org.pdgdiff.graph.PDG;
import org.pdgdiff.io.JsonOperationSerializer;
import org.pdgdiff.io.OperationSerializer;
import soot.SootClass;
import soot.toolkits.graph.pdg.HashMutablePDG;


import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import soot.SootMethod;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class PDGComparator {


    private static final int MAX_FILENAME_LENGTH = 255; // probably max, otherwise sometimes have issues
    private static final int SAFE_METHOD_NAME_LENGTH = 50; // length of method name to keep when abbreviating


    public static void compareAndPrintGraphSimilarity(List<PDG> pdgList1, List<PDG> pdgList2,
                                                      GraphMatcherFactory.MatchingStrategy strategy, String srcSourceFilePath, String dstSourceFilePath) throws IOException {

        GraphMatcher matcher = GraphMatcherFactory.createMatcher(strategy, pdgList1, pdgList2);
        // for each graph print the size of its nodes and if it has a cycle
        pdgList1.forEach(pdg -> {
            System.out.println("------");
            System.out.println(pdg.getCFG().getBody().getMethod().getSignature());
            System.out.println(GraphTraversal.getNodeCount(pdg));
            CycleDetection.hasCycle(pdg);
        });
        // perform the actual graph matching
        System.out.println("-> Beginning matching PDGs using strategy: " + strategy);
        GraphMapping graphMapping = matcher.matchPDGLists();

        // TODO: clean up debug print stmts
        System.out.println("--> Graph matching complete using strategy: " + strategy);

        // Handle unmatched graphs, i.e. additions or deletions of methods to the versions
        List<PDG> unmatchedInList1 = pdgList1.stream()
                .filter(pdg -> !graphMapping.getGraphMapping().containsKey(pdg))
                .collect(Collectors.toList());

        List<PDG> unmatchedInList2 = pdgList2.stream()
                .filter(pdg -> !graphMapping.getGraphMapping().containsValue(pdg))
                .collect(Collectors.toList());

        // Generate edit scripts for unmatched methods
        generateEditScriptsForUnmatched(unmatchedInList1, unmatchedInList2, srcSourceFilePath, dstSourceFilePath);

        graphMapping.getGraphMapping().forEach((srcPDG, dstPDG) -> {
            String method1 = srcPDG.getCFG().getBody().getMethod().getSignature();
            String method2 = dstPDG.getCFG().getBody().getMethod().getSignature();
            System.out.println("---\n> PDG from class 1: " + method1 + " is matched with PDG from class 2: " + method2);
            System.out.println(GraphTraversal.getNodeCount(srcPDG));
            CycleDetection.hasCycle(srcPDG);
            System.out.println(GraphTraversal.getNodeCount(dstPDG));
            CycleDetection.hasCycle(dstPDG);
            NodeMapping nodeMapping = graphMapping.getNodeMapping(srcPDG);
            if (nodeMapping != null) {
                System.out.println("--- Node Mapping:");
                nodeMapping.printMappings();

                try {

                    // collecting of 'metadata' of the code, i.e. function signatures and fields, will occur here. it should not have
                    // any impact on the actual matching process, to ensure that this is as semantic and language-agnostic as possible.

                    SootMethod srcObj = srcPDG.getCFG().getBody().getMethod();
                    SootMethod destObj = dstPDG.getCFG().getBody().getMethod();

                    List<EditOperation> editScript = EditScriptGenerator.generateEditScript(srcPDG, dstPDG, graphMapping,
                            srcSourceFilePath, dstSourceFilePath, srcObj, destObj);

                    List<EditOperation> recoveredEditScript = RecoveryProcessor.recoverMappings(editScript, RecoveryProcessor.RecoveryStrategy.CLEANUP_AND_FLATTEN);

                    int editDistance = EditDistanceCalculator.calculateEditDistance(recoveredEditScript);
                    System.out.println("--- Edit information ---");
                    System.out.println("-- Edit Distance: " + editDistance);

                    System.out.println("-- Edit Script:");
                    for (EditOperation op : recoveredEditScript) {
                        System.out.println(op);
                    }

                    // serialise and export
                    exportEditScript(recoveredEditScript, method1, method2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // build edit script for class mappings at this point
        SootClass srcClass = pdgList1.get(0).getCFG().getBody().getMethod().getDeclaringClass();
        SootClass dstClass = pdgList2.get(0).getCFG().getBody().getMethod().getDeclaringClass();

        ClassMetadataDiffGenerator.generateClassMetadataDiff(srcClass, dstClass, srcSourceFilePath, dstSourceFilePath, "out/metadata_diff.json");

        writeAggregatedEditScript();
    }

    private static void generateEditScriptsForUnmatched(List<PDG> unmatchedInList1, List<PDG> unmatchedInList2,
                                                        String srcSourceFilePath, String dstSourceFilePath) {
        unmatchedInList1.forEach(pdg -> {
            try {
                String methodSignature = pdg.getCFG().getBody().getMethod().getSignature();
                System.out.println("Unmatched method in List 1 (to be deleted): " + methodSignature);

                List<EditOperation> editScript = EditScriptGenerator.generateDeleteScript(pdg, srcSourceFilePath);
                exportEditScript(editScript, methodSignature, "DELETION");
            } catch (Exception e) {
                System.err.println("Failed to generate delete script for unmatched method in List 1");
                e.printStackTrace();
            }
        });

        unmatchedInList2.forEach(pdg -> {
            try {
                String methodSignature = pdg.getCFG().getBody().getMethod().getSignature();
                System.out.println("Unmatched method in List 2 (to be added): " + methodSignature);

                List<EditOperation> editScript = EditScriptGenerator.generateAddScript(pdg, dstSourceFilePath);
                exportEditScript(editScript, "INSERTION", methodSignature);
            } catch (Exception e) {
                System.err.println("Failed to generate add script for unmatched method in List 2");
                e.printStackTrace();
            }
        });
    }



    private static void exportEditScript(List<EditOperation> editScript, String method1Signature, String method2Signature) {
        // Sanitize method names for use in filenames
        String method1Safe = method1Signature.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        String method2Safe = method2Signature.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");

        String outputDir = "out/";
        String filename = outputDir + "editScript_" + method1Safe + "_to_" + method2Safe + ".json";

        // check if too long, otherwise will fail
        if (filename.length() > MAX_FILENAME_LENGTH) {
            String method1Abbrev = abbreviate(method1Safe);
            String method2Abbrev = abbreviate(method2Safe);
            filename = outputDir + "editScript_" + method1Abbrev + "_to_" + method2Abbrev + "_concat.json";
        }

        try (Writer writer = new FileWriter(filename)) {
            OperationSerializer serializer = new JsonOperationSerializer(editScript);
            serializer.writeTo(writer);
            System.out.println("Edit script exported to: " + filename);
        } catch (Exception e) {
            System.err.println("Failed to export edit script to " + filename);
            e.printStackTrace();
        }
    }

    private static String abbreviate(String methodName) {
        if (methodName.length() <= SAFE_METHOD_NAME_LENGTH) {
            return methodName;
        }
        return methodName.substring(methodName.length() - SAFE_METHOD_NAME_LENGTH);
    }




    // hacky solution for the time being, just iterates across all json files and creates one edit script
    private static void writeAggregatedEditScript() {
        String outputDir = "out/";
        String outputFileName = outputDir + "diff.json";
        JsonArray consolidatedActions = new JsonArray();

        try {
            List<File> jsonFiles = Files.list(Paths.get(outputDir))
                    .filter(path -> path.toString().endsWith(".json"))
                    .map(java.nio.file.Path::toFile)
                    .collect(Collectors.toList());

            JsonParser parser = new JsonParser();
            for (File file : jsonFiles) {
                if (file.getName().equals("diff.json")) continue;  // skip diff.json
                try (FileReader reader = new FileReader(file)) {
                    JsonObject jsonObject = parser.parse(reader).getAsJsonObject();
                    JsonArray actions = jsonObject.getAsJsonArray("actions");
                    consolidatedActions.addAll(actions);
                } catch (Exception e) {
                    System.err.println("failed to read or parse JSON file: " + file.getName());
                    e.printStackTrace();
                }
            }

            JsonObject aggregatedOutput = new JsonObject();
            aggregatedOutput.add("actions", consolidatedActions);

            try (Writer writer = new FileWriter(outputFileName)) {
                writer.write(aggregatedOutput.toString());
                System.out.println("---> agg edit scripts exported to: " + outputFileName);
            }

        } catch (Exception e) {
            System.err.println("Failed to aggregate JSON files into diff.json");
            e.printStackTrace();
        }
    }

}
