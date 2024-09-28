package org.pdgdiff.client;

import soot.SootMethod;
import soot.toolkits.graph.pdg.HashMutablePDG;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class GraphExporter {

    // Method to export PDG to a file for each class
    public static void exportPDGToFile(HashMutablePDG pdg, String fileName, String methodName) throws IOException {
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

    // Method to export PDG to a DOT file using PDGDotVisualizer for a given method
    public static void exportPDGToDot(SootMethod method, String fileName) {
        if (method == null) {
            System.out.println("Method is null, cannot export.");
            return;
        }

        // Use the PDGDotVisualizer class to export the PDG to a DOT file
        PDGDotVisualizer visualizer = new PDGDotVisualizer(fileName);
        visualizer.exportToDot(method);  // Pass the method to export the PDG as DOT
        System.out.println("PDG for method " + method.getName() + " exported to " + fileName);
    }
}
