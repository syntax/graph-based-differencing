package org.pdgdiff.graph;

import org.pdgdiff.client.PDGDotVisualizer;
import soot.SootMethod;
import soot.toolkits.graph.pdg.HashMutablePDG;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.File;


/**
 * GraphExporter class to export the PDG to both DOT and text formats. This class contains methods to export the PDG
 * to a DOT file and a text file for each class.
 */
public class GraphExporter {


    public static void clearOutputFolder(String folderPath) {
        File outputFolder = new File(folderPath);
        if (outputFolder.exists()) {
            File[] files = outputFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }

    }

    /**
     * Export the PDG to both DOT format and text format
     * @param pdg The PDG to export
     * @param dotFileName The filename for the DOT file
     * @param txtFileName The filename for the text file
     */
    public static void exportPDG(HashMutablePDG pdg, String dotFileName, String txtFileName) throws IOException {
        // Get the method associated with the PDG via the UnitGraph in HashMutablePDG
        SootMethod method = pdg.getCFG().getBody().getMethod();

        // Export the PDG to a DOT file
        exportPDGToDot(method, dotFileName);

        // Export the PDG to a text file
        exportPDGToFile(pdg, txtFileName, method.getName());
    }


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
