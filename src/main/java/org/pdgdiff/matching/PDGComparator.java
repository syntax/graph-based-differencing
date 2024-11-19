package org.pdgdiff.matching;

import com.google.gson.JsonObject;
import org.pdgdiff.edit.ClassMetadataDiffGenerator;
import org.pdgdiff.edit.EditDistanceCalculator;
import org.pdgdiff.edit.EditScriptGenerator;
import org.pdgdiff.edit.RecoveryProcessor;
import org.pdgdiff.edit.model.EditOperation;
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

    public static void compareAndPrintGraphSimilarity(List<HashMutablePDG> pdgList1, List<HashMutablePDG> pdgList2,
                                                      GraphMatcherFactory.MatchingStrategy strategy, String srcSourceFilePath, String dstSourceFilePath) throws IOException {

        GraphMatcher matcher = GraphMatcherFactory.createMatcher(strategy, pdgList1, pdgList2);

        // perform the actual graph matching
        GraphMapping graphMapping = matcher.matchPDGLists();

        // TODO: clean up debug print stmts
        System.out.println("--> Graph matching complete using strategy: " + strategy);

        graphMapping.getGraphMapping().forEach((srcPDG, dstPDG) -> {
            String method1 = srcPDG.getCFG().getBody().getMethod().getSignature();
            String method2 = dstPDG.getCFG().getBody().getMethod().getSignature();
            System.out.println("---\n> PDG from class 1: " + method1 + " is matched with PDG from class 2: " + method2);
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

                    List<EditOperation> recoveredEditScript = RecoveryProcessor.recoverMappings(editScript, RecoveryProcessor.RecoveryStrategy.CONFLICT_GRAPH);

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

    private static void exportEditScript(List<EditOperation> editScript, String method1Signature, String method2Signature) {
        // Sanitize method names for use in filenames
        String method1Safe = method1Signature.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        String method2Safe = method2Signature.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");

        String outputDir = "out/";
        String filename = outputDir + "editScript_" + method1Safe + "_to_" + method2Safe + ".json";

        try (Writer writer = new FileWriter(filename)) {
            OperationSerializer serializer = new JsonOperationSerializer(editScript);
            serializer.writeTo(writer);
            System.out.println("Edit script exported to: " + filename);
        } catch (Exception e) {
            System.err.println("Failed to export edit script to " + filename);
            e.printStackTrace();
        }
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
