package org.example;

import soot.toolkits.graph.pdg.PDGNode;
import soot.util.dot.DotGraph;
import soot.toolkits.graph.pdg.HashMutablePDG;
import java.util.HashSet;
import java.util.List;

// TODO: Fix this as it doesnt really work. Im sure I could write a better traversal and export logic.
public class PDGDotVisualizer {
    private DotGraph dotPDG;
    private HashSet<PDGNode> visited = new HashSet<>();
    private String fileName;
    private HashMutablePDG pdg;

    // Constructor for PDGDotVisualizer
    public PDGDotVisualizer(String fileName, HashMutablePDG pdg) {
        this.fileName = fileName;
        this.pdg = pdg;
        this.dotPDG = new DotGraph("PDG");

        if (this.fileName == null || this.fileName.isEmpty()) {
            System.out.println("Please provide a valid filename");
        }
        if (this.pdg == null) {
            System.out.println("PDG is null!");
        }
    }

    // Export the PDG to DOT format
    public void exportToDot() {
        if (this.pdg != null && this.fileName != null) {
            PDGNode startNode = pdg.GetStartNode();
            graphTraverse(startNode);
            dotPDG.plot(this.fileName);
            System.out.println("PDG exported to " + this.fileName);
        } else {
            System.out.println("Parameters not properly initialized!");
        }
    }

    // Traversal logic to export PDG
    private void graphTraverse(PDGNode currentNode) {
        if (currentNode == null || visited.contains(currentNode)) {
            return;
        }

        visited.add(currentNode);

        // Add current node to DOT graph
        dotPDG.drawNode(currentNode.toShortString());

        // Traverse dependents and add edges
        List<PDGNode> dependents = currentNode.getDependents();
        for (PDGNode dep : dependents) {
            dotPDG.drawEdge(currentNode.toShortString(), dep.toShortString());
            if (!visited.contains(dep)) {
                graphTraverse(dep);
            }
        }

        // Traverse back-dependents and add edges (for loops or back-dependencies)
        List<PDGNode> backDependents = currentNode.getBackDependets();
        for (PDGNode backDep : backDependents) {
            dotPDG.drawEdge(currentNode.toShortString(), backDep.toShortString());
            if (!visited.contains(backDep)) {
                graphTraverse(backDep);
            }
        }
    }
}
