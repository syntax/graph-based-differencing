package org.pdgdiff;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;

import org.pdgdiff.matching.GraphMatcher;
import org.pdgdiff.matching.GraphMapping;
import org.pdgdiff.util.GraphTraversal;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.G;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.pdg.HashMutablePDG;

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

        // Iterate over all application classes in the Scene
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            System.out.println("Class: " + sootClass.getName());
            if (sootClass.getName().contains("org.pdgdiff.testclasses")) {
                // Iterate over all methods in the class
                for (SootMethod method : sootClass.getMethods()) {
                    if (method.isConcrete()) {
                        System.out.println("  Method: " + method.getName());

                        // Generate the PDG for the method and store it for comparison
                        if (pdg1 == null) {
                            pdg1 = generatePDG(sootClass, method);
                        } else if (pdg2 == null) {
                            pdg2 = generatePDG(sootClass, method);
                            break; // Assuming you want to compare two PDGs; you can modify this to select different methods.
                        }
                    }
                }
            }
        }

        // If two PDGs are available, perform the graph matching and print the results
        if (pdg1 != null && pdg2 != null) {
            compareAndPrintGraphSimilarity(pdg1, pdg2);
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

    // Method to generate PDG for a specific method
    private static HashMutablePDG generatePDG(SootClass sootClass, SootMethod method) {
        try {
            Body body = method.retrieveActiveBody();
            UnitGraph unitGraph = new ExceptionalUnitGraph(body);

            // Generate a Program Dependency Graph (PDG)
            HashMutablePDG pdg = new HashMutablePDG(unitGraph);

//            System.out.println("PDG for method " + method.getName() + ": " + pdg);
            System.out.println("PDG for method " + method.getName() + "generated");

            // Optionally: traverse or export the PDG if needed
            return pdg;
        } catch (Exception e) {
            System.err.println("Error generating PDG for method: " + method.getName());
            e.printStackTrace();
            return null;
        }
    }

    // Method to compare two PDGs and print node similarities
    private static void compareAndPrintGraphSimilarity(HashMutablePDG pdg1, HashMutablePDG pdg2) {
        // Instantiate the GraphMatcher
        GraphMatcher matcher = new GraphMatcher(pdg1, pdg2);

        // Perform the graph matching
        GraphMapping mapping = matcher.match();
        System.out.println("Graph one has " + GraphTraversal.traverseGraphDFS(pdg1) + " nodes");
        System.out.println("Graph two has " + GraphTraversal.traverseGraphDFS(pdg2) + " nodes");
        // Output the similarity results (printing node mappings)
        System.out.println("Graph matching complete. Node similarities:");

        // You can iterate over the mapped nodes and print their details
        mapping.getNodeMapping().forEach((node1, node2) -> {
            System.out.println("Node 1: " + node1 + " is matched with Node 2: " + node2);
        });
    }

// Method to export PDG to a file for each class
    private static void exportPDGToFile(HashMutablePDG pdg, String fileName, String methodName) throws IOException {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(fileName, true));
            writer.println("\n\n---------> Method: " + methodName);

            // Write the raw .toString() output of each PDGNode to the file
            writer.println(pdg.toString());  // Output the raw toString() of the PDGNode

            writer.println("---------> End of PDG for method: " + methodName + "\n\n");
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}
