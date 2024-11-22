package org.pdgdiff.client;

import soot.Body;
import soot.SootMethod;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.util.dot.DotGraph;
import soot.util.cfgcmd.CFGToDotGraph;

public class PDGDotVisualizer {
    private String fileName;

    public PDGDotVisualizer(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("Please provide a valid filename.");
        }

        this.fileName = fileName;
    }

    // Export the PDG of a given method to DOT format
    public void exportToDot(SootMethod method) {
        if (method == null) {
            System.out.println("Method is null!");
            return;
        }

        try {
            // Retrieve the method body and create an ExceptionalUnitGraph
            Body body = method.retrieveActiveBody();
            ExceptionalUnitGraph exceptionalUnitGraph = new ExceptionalUnitGraph(body);

            // Generate the PDG using the ExceptionalUnitGraph
            HashMutablePDG pdg = new HashMutablePDG(exceptionalUnitGraph);

            // Use CFGToDotGraph to create a DOT graph from the PDG
            CFGToDotGraph pdgForMethod = new CFGToDotGraph();
            DotGraph pdgDot = pdgForMethod.drawCFG(pdg, body);

            // Export the DOT graph to the specified file
            pdgDot.plot(fileName);
            System.out.println("PDG exported to " + fileName);

        } catch (RuntimeException e) {
            System.out.println("Error exporting PDG: " + e.getMessage());
        }
    }
}
