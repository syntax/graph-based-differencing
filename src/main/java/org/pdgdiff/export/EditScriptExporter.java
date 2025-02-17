package org.pdgdiff.export;

import org.pdgdiff.edit.model.EditOperation;
import org.pdgdiff.graph.PDG;
import org.pdgdiff.io.JsonOperationSerializer;
import org.pdgdiff.io.OperationSerializer;
import org.pdgdiff.matching.GraphMapping;
import org.pdgdiff.matching.StrategySettings;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.pdgdiff.export.ExportUtils.generateHash;

public class EditScriptExporter {

    private static final int MAX_FILENAME_LENGTH = 255; // probably max, otherwise sometimes have issues


    public static void exportEditScript(List<EditOperation> editScript, String method1Signature, String method2Signature, StrategySettings strategySettings) {
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
            OperationSerializer serializer = new JsonOperationSerializer(editScript, strategySettings);
            serializer.writeTo(writer);
            System.out.println("Edit script exported to: " + filename);
        } catch (Exception e) {
            System.err.println("Failed to export edit script to " + filename);
            e.printStackTrace();
        }
    }


    public static void exportGraphMappings(GraphMapping graphMapping, List<PDG> pdgList1, List<PDG> pdgList2, String outputDir) {
        String filename = outputDir + "graphMappings.txt";

        // for multi-class graph matchings, we append to the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true))) {
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



    public static void writeAggregatedEditScript(List<EditOperation> aggregatedEditScripts, String filename, StrategySettings strategySettings) {
        try (Writer writer = new FileWriter(filename)) {
            OperationSerializer serializer = new JsonOperationSerializer(aggregatedEditScripts, strategySettings);
            serializer.writeTo(writer);
            System.out.println("Edit script exported to: " + filename);
        } catch (Exception e) {
            System.err.println("Failed to export edit script to " + filename);
            e.printStackTrace();
        }
    }

    public static void copyResultsToOutput(String beforeSourceDir, String afterSourceDir) {
        try {
            Files.copy(Paths.get(beforeSourceDir), Paths.get("py-visualise/testclasses/TestAdder1.java"), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(Paths.get(afterSourceDir), Paths.get("py-visualise/testclasses/TestAdder2.java"), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(Paths.get("out/diff.json"), Paths.get("py-visualise/out/diff.json"), StandardCopyOption.REPLACE_EXISTING);
            System.out.println(" --> results copied to python visualiser");
        } catch (IOException e) {
            System.err.println("An error occurred while copying the source files to the output folder: " + e.getMessage());
            e.printStackTrace();

        }
    }
}
