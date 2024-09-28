package org.pdgdiff;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;

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

        // Iterate over all application classes in the Scene
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            System.out.println("Class: " + sootClass.getName());
            if (sootClass.getName().contains("org.pdgdiff.testclasses")) {
                // Iterate over all methods in the class
                for (SootMethod method : sootClass.getMethods()) {
                    if (method.isConcrete()) {
                        System.out.println("  Method: " + method.getName());

                        // Generate the PDG for the method
                        generatePDG(sootClass, method);
                    }
                }
            }
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
    private static void generatePDG(SootClass sootClass, SootMethod method) {
        try {
            Body body = method.retrieveActiveBody();
            UnitGraph unitGraph = new ExceptionalUnitGraph(body);

            // Generate a Program Dependency Graph (PDG)
            HashMutablePDG pdg = new HashMutablePDG(unitGraph);

            // Output or handle the PDG for this method
            System.out.println("PDG for method " + method.getName() + ": " + pdg);

            GraphTraversal graphTraversal = new GraphTraversal();
            graphTraversal.traverseGraphBFS(pdg);
            graphTraversal.traverseGraphDFS(pdg);

            // Write the PDG to a file for the class
            String classFileName = sootClass.getName().replace('.', '_') + "_pdg.txt";
            exportPDGToFile(pdg, "out/" + classFileName, method.getName());

            System.out.println("PDG for method " + method.getName() + " exported to " + classFileName);
        } catch (Exception e) {
            System.err.println("Error generating PDG for method: " + method.getName());
            e.printStackTrace();
        }
    }

    // Method to export PDG to a file for each class
    private static void exportPDGToFile(HashMutablePDG pdg, String fileName, String methodName) throws IOException {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(fileName, true));
            writer.println("---------> Method: " + methodName);

            // Write the raw .toString() output of each PDGNode to the file
            writer.println(pdg.toString());  // Output the raw toString() of the PDGNode

            writer.println("---------> End of PDG for method: " + methodName);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}
