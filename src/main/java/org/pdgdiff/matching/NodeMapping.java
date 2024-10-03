package org.pdgdiff.matching;

import soot.toolkits.graph.pdg.PDGNode;

import java.util.HashMap;
import java.util.Map;

/**
 * NodeMapping class to store mappings between nodes in two PDGs. This class is used to store the mapping between
 * nodes in two PDGs that have been matched by the GraphMatcher.
 */
public class NodeMapping {
    private Map<PDGNode, PDGNode> nodeMapping;

    public NodeMapping() {
        nodeMapping = new HashMap<>();
    }

    // Adds a mapping between a source node and a destination node
    public void addMapping(PDGNode srcNode, PDGNode dstNode) {
        nodeMapping.put(srcNode, dstNode);
    }

    // Retrieves the mapped destination node for a given source node
    public PDGNode getMappedNode(PDGNode node) {
        return nodeMapping.get(node);
    }

    // Exposes the entire node mapping
    public Map<PDGNode, PDGNode> getNodeMapping() {
        return nodeMapping;
    }

    // Pretty print all node mappings for debugging
    public void printMappings() {
        for (Map.Entry<PDGNode, PDGNode> entry : nodeMapping.entrySet()) {
            System.out.println("Source Node: " + entry.getKey() + " --> Mapped to: " + entry.getValue());
        }
    }

    // Check if a node is already mapped
    public boolean isMapped(PDGNode node) {
        return nodeMapping.containsKey(node);
    }
}
