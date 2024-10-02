package org.pdgdiff;

import org.pdgdiff.client.GraphExporter;
import org.pdgdiff.client.GraphGenerator;
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

        // Initialize Soot
        SootInitializer.initializeSoot();

        // Load all classes in the program
        Scene.v().loadNecessaryClasses();

        // Hardcode the classes for TestAdder1 and TestAdder2, temporary
        String class1Name = "org.pdgdiff.testclasses.TestAdder1";
        String class2Name = "org.pdgdiff.testclasses.TestAdder2";

        try {
            // Retrieve the classes from the Soot Scene, these are placeholders rn
            SootClass testAdder1 = Scene.v().getSootClass(class1Name);
            SootClass testAdder2 = Scene.v().getSootClass(class2Name);

            // Generate PDGs for all methods in both classes and store in lists
            List<HashMutablePDG> pdgsClass1 = generatePDGsForClass(testAdder1);
            List<HashMutablePDG> pdgsClass2 = generatePDGsForClass(testAdder2);

            // Print the number of PDGs generated for each class
            System.out.println("PDGs generated for " + testAdder1.getName() + ": " + pdgsClass1.size());
            System.out.println("PDGs generated for " + testAdder2.getName() + ": " + pdgsClass2.size());
            // processing
            // TODO: Attempt to match PDGs across classes (see the todo.md file for more details)
            compareMethodsAcrossClasses(testAdder1, testAdder2);

        } catch (Exception e) {
            System.err.println("An error occurred while processing the classes: " + e.getMessage());
            e.printStackTrace();
        }

        // Clean up Soot resources
        SootInitializer.resetSoot();
    }

    private static void compareMethodsAcrossClasses(SootClass class1, SootClass class2) {
        HashMutablePDG pdg1;
        HashMutablePDG pdg2;

        System.out.println("Comparing methods between " + class1.getName() + " and " + class2.getName());

        // Iterate over methods in class1
        for (SootMethod method1 : class1.getMethods()) {
            if (method1.isConcrete()) {
                try {
                    method1.retrieveActiveBody();
                    System.out.println("Successfully retrieved active body for: " + method1.getName() + " in " + class1.getName());

                    // Generate the PDG for the method in class1
                    pdg1 = GraphGenerator.generatePDG(class1, method1);
                } catch (Exception e) {
                    System.err.println("Failed to retrieve body for method: " + method1.getName());
                    e.printStackTrace();
                    continue; // Skip to the next method
                }

                // Iterate over methods in class2 and compare
                for (SootMethod method2 : class2.getMethods()) {
                    if (method2.isConcrete() && method1.getName().equals(method2.getName())) {
                        try {
                            method2.retrieveActiveBody();
                            System.out.println("Successfully retrieved active body for: " + method2.getName() + " in " + class2.getName());

                            // Generate the PDG for the corresponding method in class2
                            pdg2 = GraphGenerator.generatePDG(class2, method2);

                            // Perform the comparison between PDG1 and PDG2
                            if (pdg1 != null && pdg2 != null) {
                                System.out.println("Comparing " + method1.getName() + " between " + class1.getName() + " and " + class2.getName());
                                PDGComparator.compareAndPrintGraphSimilarity(pdg1, pdg2);
                            }

                            // Export the PDG for further analysis (optional)
                            GraphExporter.exportPDG(pdg1, "out/pdg_" + class1.getName() + "_" + method1.getName() + ".dot",
                                    "out/pdg_" + class1.getName() + "_" + method1.getName() + ".txt");
                            GraphExporter.exportPDG(pdg2, "out/pdg_" + class2.getName() + "_" + method2.getName() + ".dot",
                                    "out/pdg_" + class2.getName() + "_" + method2.getName() + ".txt");

                        } catch (Exception e) {
                            System.err.println("Failed to retrieve or generate PDG for method: " + method2.getName());
                            e.printStackTrace();
                        }
                        break; // Stop after finding the corresponding method in class2
                    }
                }
            }
        }
    }

    // Method to generate PDGs for all methods in a given class and store them in a list
    private static List<HashMutablePDG> generatePDGsForClass(SootClass sootClass) {
        List<HashMutablePDG> pdgList = new ArrayList<>();
        System.out.println("Generating PDGs for class: " + sootClass.getName());

        // Iterate over each method in the class
        for (SootMethod method : sootClass.getMethods()) {
            if (method.isConcrete()) {
                try {
                    // Retrieve the active body and generate the PDG
                    method.retrieveActiveBody();
                    System.out.println("Successfully retrieved active body for: " + method.getName() + " in " + sootClass.getName());

                    // Print the Jimple body for inspection
                    System.out.println("Jimple output for method " + method.getName() + ":");
                    System.out.println(method.getActiveBody().toString());

                    // Generate the PDG for the method
                    HashMutablePDG pdg = GraphGenerator.generatePDG(sootClass, method);
                    if (pdg != null) {
                        pdgList.add(pdg);
                        System.out.println("PDG generated for method: " + method.getName());
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
