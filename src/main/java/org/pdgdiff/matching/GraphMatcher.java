package org.pdgdiff.matching;

import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.PDGNode;

import java.util.List;

// TODO: this is just skeleton code.
public class GraphMatcher {
    private HashMutablePDG graph1;
    private HashMutablePDG graph2;
    private GraphMapping mapping;

    public GraphMatcher(HashMutablePDG g1, HashMutablePDG g2) {
        this.graph1 = g1;
        this.graph2 = g2;
        this.mapping = new GraphMapping();
    }

    public GraphMapping match() {
        PDGNode startNode1 = graph1.GetStartNode();
        PDGNode startNode2 = graph2.GetStartNode();

        // Perform matching based on node structure, attributes, and dependencies
        if (startNode1 != null && startNode2 != null) {
            matchNodes(startNode1, startNode2);
        }

        return mapping;
    }

    private void matchNodes(PDGNode node1, PDGNode node2) {
        if (node1.getType() == node2.getType()) {
            mapping.addMapping(node1, node2);

            List<PDGNode> dependents1 = node1.getDependents();
            List<PDGNode> dependents2 = node2.getDependents();

            // Simple exact matching (for now)
            for (int i = 0; i < Math.min(dependents1.size(), dependents2.size()); i++) {
                matchNodes(dependents1.get(i), dependents2.get(i));
            }
        }
    }
}
