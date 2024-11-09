package org.pdgdiff;

import org.pdgdiff.graph.GraphExporter;
import org.pdgdiff.graph.GraphGenerator;
import org.pdgdiff.graph.model.MyPDG;
import org.pdgdiff.graph.model.MyPDGNode;
import org.pdgdiff.matching.PDGComparator;
import org.pdgdiff.util.SootInitializer;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.toolkits.graph.pdg.HashMutablePDG;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting PDG Diff...");
        // Clear out folder
        GraphExporter.clearOutputFolder("out");
        // Initialize Soot
        SootInitializer.initializeSoot();

        // Load all classes in the program
        Scene.v().loadNecessaryClasses();

        // Hardcode the classes for TestAdder1 and TestAdder2
        String class1Name = "org.pdgdiff.testclasses.TestAdder1";
        String class2Name = "org.pdgdiff.testclasses.TestAdder2";

        String srcSourceFilePath = "src/main/java/org/pdgdiff/testclasses/TestAdder1.java";
        String dstSourceFilePath = "src/main/java/org/pdgdiff/testclasses/TestAdder2.java";

        try {
            // Retrieve the classes from the Soot Scene
            SootClass testAdder1 = Scene.v().getSootClass(class1Name);
            SootClass testAdder2 = Scene.v().getSootClass(class2Name);

            // Generate PDGs for all methods in both classes and store in lists
            List<MyPDG> pdgsClass1 = generatePDGsForClass(testAdder1);
            List<MyPDG> pdgsClass2 = generatePDGsForClass(testAdder2);

            // Print the number of PDGs generated for each class
            System.out.println("PDGs generated for " + testAdder1.getName() + ": " + pdgsClass1.size());
            System.out.println("PDGs generated for " + testAdder2.getName() + ": " + pdgsClass2.size());

            if (!pdgsClass1.isEmpty() && !pdgsClass2.isEmpty()) {
                PDGComparator.compareAndPrintGraphSimilarity(pdgsClass1, pdgsClass2, "vf2", srcSourceFilePath, dstSourceFilePath);
            }

        } catch (Exception e) {
            System.err.println("An error occurred while processing the classes: " + e.getMessage());
            e.printStackTrace();
        }

        // Clean up Soot resources
        SootInitializer.resetSoot();
    }

    // Method to generate PDGs for all methods in a given class and store them in a list
    private static List<MyPDG> generatePDGsForClass(SootClass sootClass) {
        List<MyPDG> pdgList = new ArrayList<>();
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
                    MyPDG pdg = GraphGenerator.generatePDG(sootClass, method);
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
}
