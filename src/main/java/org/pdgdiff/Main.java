package org.pdgdiff;

import org.pdgdiff.graph.GraphExporter;
import org.pdgdiff.graph.GraphGenerator;
import org.pdgdiff.matching.PDGComparator;
import org.pdgdiff.util.SootInitializer;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.toolkits.graph.pdg.HashMutablePDG;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

            Map<HashMutablePDG, SootMethod> pdgToMethodMapClass1 = generatePDGsForClass(testAdder1);
            Map<HashMutablePDG, SootMethod> pdgToMethodMapClass2 = generatePDGsForClass(testAdder2);

            // Extract lists of PDGs for legacy functions
            List<HashMutablePDG> pdgsClass1 = new ArrayList<>(pdgToMethodMapClass1.keySet());
            List<HashMutablePDG> pdgsClass2 = new ArrayList<>(pdgToMethodMapClass2.keySet());


            // Print the number of PDGs generated for each class
            System.out.println("PDGs generated for " + testAdder1.getName() + ": " + pdgsClass1.size());
            System.out.println("PDGs generated for " + testAdder2.getName() + ": " + pdgsClass2.size());

            if (!pdgsClass1.isEmpty() && !pdgsClass2.isEmpty()) {
                PDGComparator.compareAndPrintGraphSimilarity(
                        pdgsClass1,
                        pdgsClass2,
                        "vf2",
                        srcSourceFilePath,
                        dstSourceFilePath,
                        pdgToMethodMapClass1,
                        pdgToMethodMapClass2
                );
            }

        } catch (Exception e) {
            System.err.println("An error occurred while processing the classes: " + e.getMessage());
            e.printStackTrace();
        }

        // Clean up Soot resources
        SootInitializer.resetSoot();
    }

    // Method to generate PDGs for all methods in a given class and store them in a list
    private static Map<HashMutablePDG, SootMethod> generatePDGsForClass(SootClass sootClass) {
        // TODO : continue to investagte getting more metadata from soot
        Map<HashMutablePDG, SootMethod> pdgToMethodMap = new HashMap<>();
        System.out.println("Generating PDGs for class: " + sootClass.getName());
        for (SootMethod method : sootClass.getMethods()) {
            if (method.isConcrete()) {
                try {
                    method.retrieveActiveBody();
                    System.out.println("Successfully retrieved active body for: " + method.getName() + " in " + sootClass.getName());

                    HashMutablePDG pdg = GraphGenerator.generatePDG(sootClass, method);
                    if (pdg != null) {
                        pdgToMethodMap.put(pdg, method);
                        System.out.println("PDG generated for method: " + method.getName());

                        String baseFileName = "out/pdg_" + sootClass.getName() + "_" + method.getName();
                        GraphExporter.exportPDG(pdg, baseFileName + ".dot", baseFileName + ".txt");
                    }
                } catch (Exception e) {
                    System.err.println("Failed to retrieve body or generate PDG for method: " + method.getName());
                    e.printStackTrace();
                }
            }
        }
        return pdgToMethodMap;
    }

}
