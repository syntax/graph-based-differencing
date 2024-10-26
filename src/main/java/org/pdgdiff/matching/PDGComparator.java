package org.pdgdiff.matching;

import org.pdgdiff.edit.EditDistanceCalculator;
import org.pdgdiff.edit.EditScriptGenerator;
import org.pdgdiff.edit.model.EditOperation;
import org.pdgdiff.io.JsonOperationSerializer;
import org.pdgdiff.io.OperationSerializer;
import soot.SootMethod;
import soot.toolkits.graph.pdg.HashMutablePDG;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

public class PDGComparator {

    public static void compareAndPrintGraphSimilarity(List<HashMutablePDG> pdgList1,
        List<HashMutablePDG> pdgList2,
        String strategy,
        String srcSourceFilePath,
        String dstSourceFilePath,
        Map<HashMutablePDG, SootMethod> pdgToMethodMap1,
        Map<HashMutablePDG, SootMethod> pdgToMethodMap2
    ) {
        // Instantiate the appropriate GraphMatcher
        GraphMatcher matcher = GraphMatcherFactory.createMatcher(strategy, pdgList1, pdgList2);

        // Perform the graph matching between the lists
        GraphMapping graphMapping = matcher.matchPDGLists();

        // Output the graph and node similarity results
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
                    List<EditOperation> editScript = EditScriptGenerator.generateEditScript(
                            srcPDG,
                            dstPDG,
                            graphMapping,
                            srcSourceFilePath,
                            dstSourceFilePath
                    );

                    int editDistance = EditDistanceCalculator.calculateEditDistance(editScript);
                    System.out.println("--- Edit information ---");
                    System.out.println("-- Edit Distance: " + editDistance);

                    System.out.println("-- Edit Script:");
                    for (EditOperation op : editScript) {
                        System.out.println(op);
                    }

                    // Serialize the edit script and export it
                    exportEditScript(editScript, method1, method2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private static void exportEditScript(List<EditOperation> editScript, String method1Signature, String method2Signature) {
        // Sanitize method names for use in filenames
        String method1Safe = method1Signature.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        String method2Safe = method2Signature.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");

        // Define the output directory
        String outputDir = "out/";

        // Define the filename
        String filename = outputDir + "editScript_" + method1Safe + "_to_" + method2Safe + ".json";

        // Create the directory if it doesn't exist
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Serialize to JSON
        try (Writer writer = new FileWriter(filename)) {
            OperationSerializer serializer = new JsonOperationSerializer(editScript);
            serializer.writeTo(writer);
            System.out.println("Edit script exported to: " + filename);
        } catch (Exception e) {
            System.err.println("Failed to export edit script to " + filename);
            e.printStackTrace();
        }
    }
}
