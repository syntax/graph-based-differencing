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

    enum FILE_VERSION {
        SOURCE,
        DEST
    }

    public static void main(String[] args) {
        System.out.println("Starting PDG Diff...");
        String class1Name, class2Name;
        String srcSourceFilePath, dstSourceFilePath;
        String beforeDir, afterDir;


        if (args.length < 6) {
            System.out.println("Insufficient arguments provided.");
            System.out.println("Usage: java org.pdgdiff.Main <beforeSourcePath> <afterSourcePath> <beforeCompiledDir> <afterCompiledDir> <beforeClassName> <afterClassName>");
            System.out.println("Using Maven: mvn clean compile && mvn exec:java -Dexec.mainClass=\"org.pdgdiff.Main\" -Dexec.args=\"<beforeSourcePath> <afterSourcePath> <beforeCompiledDir> <afterCompiledDir> <beforeClassName> <afterClassName>\"\n");
            System.out.println("Using hardcoded information");

            //  !!!! To run on datasets, use the following !!!!
//            String commit = "605e88d4bfb7e5afa56ae70fe16bb0e973865124";
//            String project = "signal-server";
//            String filename = "KeysController";
//
//            beforeDir = "./benchmark/datasets/gh-java/before/" + project + "/" + commit + "/compiled";
//            afterDir = "./benchmark/datasets/gh-java/after/" + project + "/" + commit + "/compiled";
//            class1Name = "org.whispersystems.textsecuregcm.controllers.KeysController";
//            class2Name = "org.whispersystems.textsecuregcm.controllers.KeysController";
//            srcSourceFilePath = "./benchmark/datasets/gh-java/before/" + project + "/" + commit + "/" + filename +".java";
//            dstSourceFilePath = "./benchmark/datasets/gh-java/after/" + project + "/" + commit + "/" + filename +".java";



            // !!!! to use on local test classes, use the following !!!!
            class1Name = "org.pdgdiff.testclasses.TestAdder1";
            class2Name = "org.pdgdiff.testclasses.TestAdder2";

            srcSourceFilePath = "src/main/java/org/pdgdiff/testclasses/TestAdder1.java";
            dstSourceFilePath = "src/main/java/org/pdgdiff/testclasses/TestAdder2.java";

            beforeDir = System.getProperty("user.dir") + "/target/classes";
            afterDir = System.getProperty("user.dir") + "/target/classes";

        } else {
            // as an example;
            //  mvn exec:java -Dexec.mainClass="org.pdgdiff.Main" -Dexec.args="./src/main/java/org/pdgdiff/testclasses/TestAdder1.java ./src/main/java/org/pdgdiff/testclasses/TestAdder2.java ./target/classes ./target/classes org.pdgdiff.testclasses.TestAdder1 org.pdgdiff.testclasses.TestAdder2"
            srcSourceFilePath = args[0];
            dstSourceFilePath = args[1];
            beforeDir = args[2];
            afterDir = args[3];
            class1Name = args[4];
            class2Name = args[5];
        }



        // clear out folder
        GraphExporter.clearOutputFolder("out");



        // init Soot
        SootInitializer.initializeSoot(beforeDir);

        // Load all classes in the program
        Scene.v().loadNecessaryClasses();

        try {
            // Retrieve the classes from the Soot Scene
            SootInitializer.initializeSoot(beforeDir);
            Scene.v().loadNecessaryClasses();
            SootClass beforeFile = Scene.v().getSootClass(class1Name);
            List<PDG> pdgsClass1 = generatePDGsForClass(beforeFile, FILE_VERSION.SOURCE);

            SootInitializer.initializeSoot(afterDir);
            Scene.v().loadNecessaryClasses();
            SootClass afterFile = Scene.v().getSootClass(class2Name);
            List<PDG> pdgsClass2 = generatePDGsForClass(afterFile, FILE_VERSION.DEST);

            // Print the number of PDGs generated for each class
            System.out.println("PDGs generated for " + beforeFile.getName() + ": " + pdgsClass1.size());
            System.out.println("PDGs generated for " + afterFile.getName() + ": " + pdgsClass2.size());

            if (!pdgsClass1.isEmpty() && !pdgsClass2.isEmpty()) {
                PDGComparator.compareAndPrintGraphSimilarity(pdgsClass1, pdgsClass2, GraphMatcherFactory.MatchingStrategy.ULLMANN, srcSourceFilePath, dstSourceFilePath);
            }

            copyResultsToOutput(srcSourceFilePath, dstSourceFilePath);

        } catch (Exception e) {
            System.err.println("An error occurred while processing the classes: " + e.getMessage());
        }

        // Clean up Soot resources
        SootInitializer.resetSoot();
    }

    // Method to generate PDGs for all methods in a given class and store them in a list
    private static List<PDG> generatePDGsForClass(SootClass sootClass, FILE_VERSION version) {
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
                    pdgList.add(pdg);
                    System.out.println("PDG generated for method: " + method.getName());

                    // likely pdg functions will clash across src/dest of the same file name, so need to specify these.
                    String baseFileName;
                    if (version == FILE_VERSION.SOURCE) {
                        baseFileName = "out/src_pdg" + sootClass.getName() + "_" + method.getName();
                    } else {
                        baseFileName = "out/dst_pdg_" + sootClass.getName() + "_" + method.getName();
                    }
                    GraphExporter.exportPDG(pdg, baseFileName + ".dot", baseFileName + ".txt");
                } catch (Exception e) {
                    System.err.println("Failed to retrieve body or generate PDG for method: " + method.getName());
                }
            }
        }
        return pdgList;
    }

    private static void copyResultsToOutput(String beforeSourceDir, String afterSourceDir) {
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
