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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

public class PDGComparator {


    private static final int MAX_FILENAME_LENGTH = 255; // probably max, otherwise sometimes have issues
    private static final RecoveryProcessor.RecoveryStrategy RECOVERY_STRATEGY = RecoveryProcessor.RecoveryStrategy.CLEANUP_AND_FLATTEN;


    public static void compareAndPrintGraphSimilarity(List<PDG> pdgList1, List<PDG> pdgList2,
                                                      GraphMatcherFactory.MatchingStrategy strategy, String srcSourceFilePath, String dstSourceFilePath) throws IOException {

        GraphMatcher matcher = GraphMatcherFactory.createMatcher(strategy, pdgList1, pdgList2);
        // for each graph print the size of its nodes and if it has a cycle
        pdgList1.forEach(pdg -> {
            System.out.println("------");
            System.out.println(pdg.getCFG().getBody().getMethod().getSignature());
            System.out.println("Node count" + GraphTraversal.getNodeCount(pdg));
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
        exportGraphMappings(graphMapping, pdgList1, pdgList2, "out/");

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

                    List<EditOperation> recoveredEditScript = RecoveryProcessor.recoverMappings(editScript, RECOVERY_STRATEGY);

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
                SootMethod method = pdg.getCFG().getBody().getMethod();
                String methodSignature = pdg.getCFG().getBody().getMethod().getSignature();
                System.out.println("Unmatched method in List 1 (to be deleted): " + methodSignature);

                List<EditOperation> editScript = EditScriptGenerator.generateDeleteScript(pdg, srcSourceFilePath, method);
                List<EditOperation> recoveredEditScript = RecoveryProcessor.recoverMappings(editScript, RECOVERY_STRATEGY);
                exportEditScript(recoveredEditScript, methodSignature, "DELETION");
            } catch (Exception e) {
                System.err.println("Failed to generate delete script for unmatched method in List 1");
                e.printStackTrace();
            }
        });

        unmatchedInList2.forEach(pdg -> {
            try {
                SootMethod method = pdg.getCFG().getBody().getMethod();
                String methodSignature = pdg.getCFG().getBody().getMethod().getSignature();
                System.out.println("Unmatched method in List 2 (to be added): " + methodSignature);

                List<EditOperation> editScript = EditScriptGenerator.generateAddScript(pdg, dstSourceFilePath, method);
                List<EditOperation> recoveredEditScript = RecoveryProcessor.recoverMappings(editScript, RECOVERY_STRATEGY);
                exportEditScript(recoveredEditScript, "INSERTION", methodSignature);
            } catch (Exception e) {
                System.err.println("Failed to generate add script for unmatched method in List 2");
                e.printStackTrace();
            }
        });
    }


    // these are all a bit hacky, todo refactor to new file maybe called Export


    private static void exportEditScript(List<EditOperation> editScript, String method1Signature, String method2Signature) {
        // Sanitize method names for use in filenames
        String method1Safe = method1Signature.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        String method2Safe = method2Signature.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");

        String outputDir = "out/";
        String filename = outputDir + "editScript_" + method1Safe + "_to_" + method2Safe + ".json";

        // check if too long, otherwise will fail
        if (filename.length() > MAX_FILENAME_LENGTH) {
            String method1Abbrev = generateHash(method1Safe);
            System.out.println("Method name too big to save to file, hashed;" + method1Safe + " -> " + method1Abbrev);
            String method2Abbrev = generateHash(method2Safe);
            System.out.println("Method name too big to save to file, hashed;" + method2Safe + " -> " + method2Abbrev);
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


    private static void exportGraphMappings(GraphMapping graphMapping, List<PDG> pdgList1, List<PDG> pdgList2, String outputDir) {
        String filename = outputDir + "graphMappings.txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("Graph Mappings (Before -> After):\n");

            graphMapping.getGraphMapping().forEach((srcPDG, dstPDG) -> {
                try {
                    String srcMethodSignature = srcPDG.getCFG().getBody().getMethod().getSignature();
                    String dstMethodSignature = dstPDG.getCFG().getBody().getMethod().getSignature();
                    writer.write(srcMethodSignature + " -> " + dstMethodSignature + "\n");
                } catch (IOException e) {
                    System.err.println("Error writing mapping to file: " + e.getMessage());
                }
            });

            writer.write("\nUnmatched Graphs in Source:\n");
            pdgList1.stream()
                    .filter(pdg -> !graphMapping.getGraphMapping().containsKey(pdg))
                    .forEach(pdg -> {
                        try {
                            String methodSignature = pdg.getCFG().getBody().getMethod().getSignature();
                            writer.write(methodSignature + "\n");
                        } catch (IOException e) {
                            System.err.println("Error writing unmatched source graph to file: " + e.getMessage());
                        }
                    });

            writer.write("\nUnmatched Graphs in Destination:\n");
            pdgList2.stream()
                    .filter(pdg -> !graphMapping.getGraphMapping().containsValue(pdg))
                    .forEach(pdg -> {
                        try {
                            String methodSignature = pdg.getCFG().getBody().getMethod().getSignature();
                            writer.write(methodSignature + "\n");
                        } catch (IOException e) {
                            System.err.println("Error writing unmatched destination graph to file: " + e.getMessage());
                        }
                    });

            System.out.println("Graph mappings exported to: " + filename);
        } catch (IOException e) {
            System.err.println("Failed to export graph mappings to " + filename);
        }
    }




    private static String generateHash(String methodName) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(methodName.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString(); // todo might need to send a concatenated part of the hash, but as of rn this seems to be ok length
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return methodName;
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
