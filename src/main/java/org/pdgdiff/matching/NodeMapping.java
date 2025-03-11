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
            System.out.println("Source Node: " + entry.getKey()
                    + " --> Mapped to: " + entry.getValue());
        }
    }

    public void printMappingsVerbose() {
        for (Map.Entry<PDGNode, PDGNode> entry : nodeMapping.entrySet()) {
            System.out.println("Source Node: " + entry.getKey().toString()
                    + " --> Mapped to: " + entry.getValue().toString());
        }
    }

    public boolean isEmpty() {
        return nodeMapping.isEmpty();
    }

    public int size() {
        return nodeMapping.size();
    }
}