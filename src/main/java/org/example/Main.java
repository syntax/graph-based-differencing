package org.example;

import soot.*;
import soot.options.Options;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.PDGNode;

import java.io.FileWriter;
import java.io.IOException;
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

        // Iterate over all application classes in the Scene
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            System.out.println("Class: " + sootClass.getName());

            // Iterate over all methods in the class
            for (SootMethod method : sootClass.getMethods()) {
                if (method.isConcrete()) {
                    System.out.println("  Method: " + method.getName());

                    // Generate the PDG for the method
                    generatePDG(method);
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
//        Options.v().set_process_dir(Collections.singletonList("target/classes"));

        // Set the class path to your program's compiled classes or jarsmvn exec:java -Dexec.mainClass="org.example.Main"
        String classPath = "target/classes";  // path to the bytecode
        Options.v().set_soot_classpath(classPath);
        Options.v().set_process_dir(Collections.singletonList(classPath));

        // Whole program analysis
        Options.v().set_whole_program(true);

        // Set entry points (optional: specify if you want Soot to begin analysis from certain methods)
//        Options.v().set_main_class("target/classes"); // Replace with the main class of your program, if relevant
    }

    // Method to generate PDG for a specific method
    private static void generatePDG(SootMethod method) {
        try {
            Body body = method.retrieveActiveBody();
            UnitGraph unitGraph = new ExceptionalUnitGraph(body);

            // Generate a Program Dependency Graph (PDG)
            HashMutablePDG pdg = new HashMutablePDG(unitGraph);

            // Output or handle the PDG for this method
            System.out.println("PDG for method " + method.getName() + ": " + pdg);
            String dotFileName = method.getName() + "_pdg.dot";

            // Use PDGDotVisualizer to export to DOT format
            PDGNode startNode = pdg.GetStartNode();
            PDGDotVisualizer visualizer = new PDGDotVisualizer(dotFileName, pdg);
//            visualizer.exportToDot();  // Export PDG to DOT

            System.out.println("PDG for method " + method.getName() + " exported to " + dotFileName);
        } catch (Exception e) {
            System.err.println("Error generating PDG for method: " + method.getName());
            e.printStackTrace();
        }
    }

}
