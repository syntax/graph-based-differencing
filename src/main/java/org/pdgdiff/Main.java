package org.pdgdiff;

import org.pdgdiff.graph.GraphExporter;
import org.pdgdiff.graph.GraphGenerator;
import org.pdgdiff.graph.PDG;
import org.pdgdiff.matching.GraphMatcherFactory;
import org.pdgdiff.matching.PDGComparator;
import org.pdgdiff.util.SootInitializer;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.toolkits.graph.pdg.HashMutablePDG;

import javax.security.sasl.SaslServer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        System.out.println("Starting PDG Diff...");
        // Clear out folder
        GraphExporter.clearOutputFolder("out");

        //  !!!! To run on datasets, use the following !!!!
        String beforeDir = "./benchmark/datasets/gh-java/before/signal-server/f9d7c1de573fb10559fc5bc69b40f02b25f48769/compiled";
        String afterDir = "./benchmark/datasets/gh-java/after/signal-server/f9d7c1de573fb10559fc5bc69b40f02b25f48769/compiled";
        String class1Name = "org.whispersystems.textsecuregcm.storage.StoredMessageManager";
        String class2Name = "org.whispersystems.textsecuregcm.storage.StoredMessageManager";
        String srcSourceFilePath = "./benchmark/datasets/gh-java/before/signal-server/f9d7c1de573fb10559fc5bc69b40f02b25f48769/StoredMessageManager.java";
        String dstSourceFilePath = "./benchmark/datasets/gh-java/after/signal-server/f9d7c1de573fb10559fc5bc69b40f02b25f48769/StoredMessageManager.java";


        // !!!! to use on local test classes, use the following !!!!
//        String class1Name = "org.pdgdiff.testclasses.TestAdder1";
//        String class2Name = "org.pdgdiff.testclasses.TestAdder2";
//
//        String srcSourceFilePath = "src/main/java/org/pdgdiff/testclasses/TestAdder1.java";
//        String dstSourceFilePath = "src/main/java/org/pdgdiff/testclasses/TestAdder2.java";
//
//        String beforeDir = System.getProperty("user.dir") + "/target/classes";
//        String afterDir = System.getProperty("user.dir") + "/target/classes";

        // Initialize Soot
        SootInitializer.initializeSoot(beforeDir);

        // Load all classes in the program
        Scene.v().loadNecessaryClasses();

        try {
            // Retrieve the classes from the Soot Scene
            SootInitializer.initializeSoot(beforeDir);
            Scene.v().loadNecessaryClasses();
            SootClass beforeFile = Scene.v().getSootClass(class1Name);
            List<PDG> pdgsClass1 = generatePDGsForClass(beforeFile);

            SootInitializer.initializeSoot(afterDir);
            Scene.v().loadNecessaryClasses();
            SootClass afterFile = Scene.v().getSootClass(class2Name);
            List<PDG> pdgsClass2 = generatePDGsForClass(afterFile);

            // Print the number of PDGs generated for each class
            System.out.println("PDGs generated for " + beforeFile.getName() + ": " + pdgsClass1.size());
            System.out.println("PDGs generated for " + afterFile.getName() + ": " + pdgsClass2.size());

            if (!pdgsClass1.isEmpty() && !pdgsClass2.isEmpty()) {
                PDGComparator.compareAndPrintGraphSimilarity(pdgsClass1, pdgsClass2, GraphMatcherFactory.MatchingStrategy.VF2, srcSourceFilePath, dstSourceFilePath);
            }

            copyResultsToOutput(srcSourceFilePath, dstSourceFilePath);

        } catch (Exception e) {
            System.err.println("An error occurred while processing the classes: " + e.getMessage());
            e.printStackTrace();
        }

        // Clean up Soot resources
        SootInitializer.resetSoot();
    }

    // Method to generate PDGs for all methods in a given class and store them in a list
    private static List<PDG> generatePDGsForClass(SootClass sootClass) {
        List<PDG> pdgList = new ArrayList<>();
        System.out.println("Generating PDGs for class: " + sootClass.getName());
        // TODO investigate getting metadata from here.
        // Iterate over each method in the class
        for (SootMethod method : sootClass.getMethods()) {
            if (method.isConcrete()) {
                try {
                    // Retrieve the active body and generate the PDG
                    method.retrieveActiveBody();
                    System.out.println("Successfully retrieved active body for: " + method.getName() + " in " + sootClass.getName());

                    // Generate the PDG for the method
                    PDG pdg = GraphGenerator.constructPdg(sootClass, method);
                    if (pdg != null) {
                        pdgList.add(pdg);
                        System.out.println("PDG generated for method: " + method.getName());

                        // Export the PDG for this method to a .dot and .txt file
                        String baseFileName = "out/pdg_" + sootClass.getName() + "_" + method.getName();
                        GraphExporter.exportPDG(pdg, baseFileName + ".dot", baseFileName + ".txt");

                    }
                } catch (Exception e) {
                    System.err.println("Failed to retrieve body or generate PDG for method: " + method.getName());
                    e.printStackTrace();
                }
            }
        }
        return pdgList;
    }

    private static void copyResultsToOutput(String beforeSourceDir, String afterSourceDir) {
        // Copy the results to the output folder
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
