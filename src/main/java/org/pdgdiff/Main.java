package org.pdgdiff;

import org.pdgdiff.client.GraphExporter;
import org.pdgdiff.client.GraphGenerator;
import org.pdgdiff.matching.PDGComparator;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.G;
import soot.options.Options;
import soot.toolkits.graph.pdg.HashMutablePDG;

import java.io.IOException;
import java.util.Collections;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting PDG Diff...");

        // Initialize Soot
        initializeSoot();

        // Load all classes in the program
        Scene.v().loadNecessaryClasses();

        // Hardcode the classes for TestAdder1 and TestAdder2
        String class1Name = "org.pdgdiff.testclasses.TestAdder1";
        String class2Name = "org.pdgdiff.testclasses.TestAdder2";

        try {
            // Retrieve the classes from the Soot Scene
            SootClass testAdder1 = Scene.v().getSootClass(class1Name);
            SootClass testAdder2 = Scene.v().getSootClass(class2Name);

            // Generate PDGs for methods and compare
            compareMethodsAcrossClasses(testAdder1, testAdder2);

        } catch (Exception e) {
            System.err.println("An error occurred while processing the classes: " + e.getMessage());
            e.printStackTrace();
        }

        // Clean up Soot resources
        G.reset();
    }

    // Method to compare methods across two classes
    private static void compareMethodsAcrossClasses(SootClass class1, SootClass class2) {
        HashMutablePDG pdg1;
        HashMutablePDG pdg2;

        System.out.println("Comparing methods between " + class1.getName() + " and " + class2.getName());

        // Iterate over methods in TestAdder1
        for (SootMethod method1 : class1.getMethods()) {
            if (method1.isConcrete()) {
                try {
                    method1.retrieveActiveBody();
                    System.out.println("Successfully retrieved active body for: " + method1.getName() + " in " + class1.getName());

                    // Generate the PDG for the method
                    pdg1 = GraphGenerator.generatePDG(class1, method1);
                } catch (Exception e) {
                    System.err.println("Failed to retrieve body for method: " + method1.getName());
                    e.printStackTrace();
                    continue; // Skip to the next method
                }

                // Iterate over methods in TestAdder2 and compare
                for (SootMethod method2 : class2.getMethods()) {
                    if (method2.isConcrete() && method1.getName().equals(method2.getName())) {
                        try {
                            method2.retrieveActiveBody();
                            System.out.println("Successfully retrieved active body for: " + method2.getName() + " in " + class2.getName());

                            // Generate the PDG for the corresponding method in TestAdder2
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
                        break; // Stop after finding the corresponding method in TestAdder2
                    }
                }
            }
        }
    }

    // Method to initialize Soot configuration
    private static void initializeSoot() {
        // Set Soot options
        Options.v().set_prepend_classpath(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_output_format(Options.output_format_jimple);
        Options.v().set_verbose(true); // Debug output

        // Set the class path to your program's compiled classes
        String classPath = System.getProperty("user.dir") + "/target/classes";
        Options.v().set_soot_classpath(classPath);
        Options.v().set_process_dir(Collections.singletonList(classPath));

        // Whole program analysis
        Options.v().set_whole_program(true);
    }
}
