package org.example;

import soot.*;
import soot.options.Options;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.pdg.HashMutablePDG;

import java.util.Iterator;

public class Main {
    public static void main(String[] args) {
        System.out.println("Soot PDG for entire program");

        // Initialize Soot
        initializeSoot();

        // Load all classes in the program
        Scene.v().loadNecessaryClasses();

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

        // Set the class path to your program's compiled classes or jars
        String classPath = "path/to/your/classes";  // Replace this with your actual class path
        Options.v().set_soot_classpath(classPath);

        // Whole program analysis
        Options.v().set_whole_program(true);

        // Set entry points (optional: specify if you want Soot to begin analysis from certain methods)
        Options.v().set_main_class("your.main.Class"); // Replace with the main class of your program, if relevant
    }

    // Method to generate PDG for a specific method
    private static void generatePDG(SootMethod method) {
        try {
            Body body = method.retrieveActiveBody();
            UnitGraph unitGraph = new ExceptionalUnitGraph(body);

            // Generate a Program Dependency Graph (PDG)
            HashMutablePDG pdg = new HashMutablePDG(unitGraph);

            // Output or handle the PDG for this method
            System.out.println("    PDG for method " + method.getName() + ": " + pdg);
        } catch (Exception e) {
            System.err.println("Error generating PDG for method: " + method.getName());
            e.printStackTrace();
        }
    }
}
