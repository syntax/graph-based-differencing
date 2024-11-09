package org.pdgdiff.matching;

import org.pdgdiff.graph.model.MyPDGNode;

import java.util.HashMap;
import java.util.Map;

/**
 * NodeMapping class to store mappings between nodes in two PDGs. This class is used to store the mapping between
 * nodes in two PDGs that have been matched by the GraphMatcher.
 */
public class NodeMapping {
    private Map<MyPDGNode, MyPDGNode> nodeMapping;
    private Map<MyPDGNode, MyPDGNode> reverseNodeMapping;

    public NodeMapping() {
        nodeMapping = new HashMap<>();
        reverseNodeMapping = new HashMap<>();
    }

    // Adds a mapping between a source node and a destination node
    public void addMapping(MyPDGNode srcNode, MyPDGNode dstNode) {
        nodeMapping.put(srcNode, dstNode);
        reverseNodeMapping.put(dstNode, srcNode);
    }

    // Retrieves the mapped destination node for a given source node
    public MyPDGNode getMappedNode(MyPDGNode node) {
        return nodeMapping.get(node);
    }

    // Retrieves the mapped source node for a given destination node
    public MyPDGNode getReverseMappedNode(MyPDGNode node) {
        return reverseNodeMapping.get(node);
    }

    // Exposes the entire node mapping
    public Map<MyPDGNode, MyPDGNode> getNodeMapping() {
        return nodeMapping;
    }

    // Exposes the reverse node mapping
    public Map<MyPDGNode, MyPDGNode> getReverseNodeMapping() {
        return reverseNodeMapping;
    }

    // Pretty print all node mappings for debugging
    public void printMappings() {
        for (Map.Entry<MyPDGNode, MyPDGNode> entry : nodeMapping.entrySet()) {
            System.out.println("Source Node: " + entry.getKey().toShortString()
                    + " --> Mapped to: " + entry.getValue().toShortString());
        }
    }

    public void printMappingsVerbose() {
        for (Map.Entry<MyPDGNode, MyPDGNode> entry : nodeMapping.entrySet()) {
            System.out.println("Source Node: " + entry.getKey().toString()
                    + " --> Mapped to: " + entry.getValue().toString());
        }
    }

    // Check if a node is already mapped
    public boolean isMapped(MyPDGNode node) {
        return nodeMapping.containsKey(node);
    }

    // Check if a destination node is already reverse-mapped
    public boolean isReverseMapped(MyPDGNode node) {
        return reverseNodeMapping.containsKey(node);
    }

    public boolean isEmpty() {
        return nodeMapping.isEmpty();
    }

    public int size() {
        return nodeMapping.size();
    }
}