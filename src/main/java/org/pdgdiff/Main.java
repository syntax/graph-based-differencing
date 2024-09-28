package org.pdgdiff;

import org.pdgdiff.client.GraphGenerator;
import org.pdgdiff.matching.PDGComparator;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.G;
import soot.options.Options;
import soot.toolkits.graph.pdg.HashMutablePDG;

import java.util.Collections;

public class Main {
    public static void main(String[] args) {
        System.out.println("Soot PDG for entire program");

        // Initialize Soot
        initializeSoot();

        // Load all classes in the program
        Scene.v().loadNecessaryClasses();

        int classCount = Scene.v().getApplicationClasses().size();
        if (classCount == 0) {
            System.out.println("No application classes found.");
        } else {
            System.out.println("Found " + classCount + " classes.");
        }

        HashMutablePDG pdg1 = null;
        HashMutablePDG pdg2 = null;

        String testClassDir = "org.pdgdiff.testclasses";
        System.out.println("Searching within the directory: " + testClassDir);

        // Iterate over all application classes in the Scene
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            if (sootClass.getName().contains(testClassDir)) {
                // Iterate over all methods in the class
                for (SootMethod method : sootClass.getMethods()) {
                    if (method.isConcrete()) {
                        System.out.println("Class: " + sootClass.getName());
                        System.out.println("  Method: " + method.getName());

                        // Generate the PDG for the method and store it for comparison
                        if (pdg1 == null) {
                            pdg1 = GraphGenerator.generatePDG(sootClass, method);
                        } else if (pdg2 == null) {
                            pdg2 = GraphGenerator.generatePDG(sootClass, method);
                            break; // Assuming you want to compare two PDGs; you can modify this to select different methods.
                        }
                    }
                }
            }
        }

        // If two PDGs are available, perform the graph matching and print the results
        if (pdg1 != null && pdg2 != null) {
            PDGComparator.compareAndPrintGraphSimilarity(pdg1, pdg2);
        }

        // Clean up Soot resources
        G.reset();
    }

    // Method to initialize Soot configuration
    private static void initializeSoot() {
        // Set Soot options
        Options.v().set_prepend_classpath(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_output_format(Options.output_format_jimple);

        // Set the class path to your program's compiled classes
        String classPath = "target/classes";
        Options.v().set_soot_classpath(classPath);
        Options.v().set_process_dir(Collections.singletonList(classPath));

        // Whole program analysis
        Options.v().set_whole_program(true);
    }
}
