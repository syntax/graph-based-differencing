package org.pdgdiff.matching;

import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.PDGNode;

import java.util.List;

public class GraphMatcher {
    private HashMutablePDG graph1;
    private HashMutablePDG graph2;
    private GraphMapping mapping;

    public GraphMatcher(HashMutablePDG g1, HashMutablePDG g2) {
        this.graph1 = g1;
        this.graph2 = g2;
        this.mapping = new GraphMapping();
    }

    // This method is the entry point for the matching process
    public GraphMapping match() {
        PDGNode startNode1 = graph1.GetStartNode();
        PDGNode startNode2 = graph2.GetStartNode();

        // Perform matching starting from the root nodes
        if (startNode1 != null && startNode2 != null) {
            matchNodes(startNode1, startNode2);
        }

        return mapping;
    }

    // This is a recursive method to match nodes in the PDG
    // This is a beginning of a simple matching algorithm, to check if nodes are semantically similar
    // TODO: Allow setting of a matching model from matching.models
    // TODO :private void matchNodes(PDGNode node1, PDGNode node2, Model matchingModel)
    private void matchNodes(PDGNode node1, PDGNode node2) {
        // Match nodes only if they meet a similarity threshold
        if (similarityScore(node1, node2) >= 2.0) {
            // Add the matched nodes to the mapping
            mapping.addMapping(node1, node2);

            // Match both dependents and back dependents (data and control dependencies)
            List<PDGNode> dependents1 = node1.getDependents();
            List<PDGNode> dependents2 = node2.getDependents();
            List<PDGNode> backDependents1 = node1.getBackDependets();
            List<PDGNode> backDependents2 = node2.getBackDependets();

            // Match dependent nodes recursively
            matchDependents(dependents1, dependents2);
            matchDependents(backDependents1, backDependents2);
        }
    }

    private void matchDependents(List<PDGNode> dependents1, List<PDGNode> dependents2) {
        // You can refine this to find the best matches between dependents
        for (int i = 0; i < Math.min(dependents1.size(), dependents2.size()); i++) {
            matchNodes(dependents1.get(i), dependents2.get(i));
        }
    }

    // Calculate a similarity score between two nodes
    private double similarityScore(PDGNode node1, PDGNode node2) {
        double score = 0.0;

        // Compare node types
        if (node1.getType() == node2.getType()) {
            score += 1.0;
        }

        // Compare node attributes
        if (node1.getAttrib() == node2.getAttrib()) {
            score += 0.5;
        }

        // Compare the number of dependents and back dependents
        if (node1.getDependents().size() == node2.getDependents().size()) {
            score += 0.5;
        }
        if (node1.getBackDependets().size() == node2.getBackDependets().size()) {
            score += 0.5;
        }
        // TODO: Add more attributes like Node Labels, node labels contain the actual code a node represents
        // This is pretty critical but im not exactly sure how to represent these sections, perhaps represent them in turn as
        // an abstract syntax tree which I can then compare?
        return score;
    }
}
