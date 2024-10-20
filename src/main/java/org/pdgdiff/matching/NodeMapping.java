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
    private Map<PDGNode, PDGNode> reverseNodeMapping;

    public NodeMapping() {
        nodeMapping = new HashMap<>();
        reverseNodeMapping = new HashMap<>();
    }

    // Adds a mapping between a source node and a destination node
    public void addMapping(PDGNode srcNode, PDGNode dstNode) {
        nodeMapping.put(srcNode, dstNode);
        reverseNodeMapping.put(dstNode, srcNode);
    }

    // Retrieves the mapped destination node for a given source node
    public PDGNode getMappedNode(PDGNode node) {
        return nodeMapping.get(node);
    }

    // Retrieves the mapped source node for a given destination node
    public PDGNode getReverseMappedNode(PDGNode node) {
        return reverseNodeMapping.get(node);
    }

    // Exposes the entire node mapping
    public Map<PDGNode, PDGNode> getNodeMapping() {
        return nodeMapping;
    }

    // Exposes the reverse node mapping
    public Map<PDGNode, PDGNode> getReverseNodeMapping() {
        return reverseNodeMapping;
    }

    // Pretty print all node mappings for debugging
    public void printMappings() {
        for (Map.Entry<PDGNode, PDGNode> entry : nodeMapping.entrySet()) {
            System.out.println("Source Node: " + entry.getKey().toShortString()
                    + " --> Mapped to: " + entry.getValue().toShortString());
        }
    }

    public void printMappingsVerbose() {
        for (Map.Entry<PDGNode, PDGNode> entry : nodeMapping.entrySet()) {
            System.out.println("Source Node: " + entry.getKey().toString()
                    + " --> Mapped to: " + entry.getValue().toString());
        }
    }

    // Check if a node is already mapped
    public boolean isMapped(PDGNode node) {
        return nodeMapping.containsKey(node);
    }

    // Check if a destination node is already reverse-mapped
    public boolean isReverseMapped(PDGNode node) {
        return reverseNodeMapping.containsKey(node);
    }

    public boolean isEmpty() {
        return nodeMapping.isEmpty();
    }
}