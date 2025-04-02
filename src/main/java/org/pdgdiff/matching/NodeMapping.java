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

    // adds a mapping between a source node and a destination node
    public void addMapping(PDGNode srcNode, PDGNode dstNode) {
        nodeMapping.put(srcNode, dstNode);
        reverseNodeMapping.put(dstNode, srcNode);
    }

    // exposes the entire node mapping
    public Map<PDGNode, PDGNode> getNodeMapping() {
        return nodeMapping;
    }

    // exposes the reverse node mapping, useful for backwarsd traverse
    public Map<PDGNode, PDGNode> getReverseNodeMapping() {
        return reverseNodeMapping;
    }

    // print all node mappings for debugging
    public void printMappings() {
        for (Map.Entry<PDGNode, PDGNode> entry : nodeMapping.entrySet()) {
            System.out.println("Source Node: " + entry.getKey()
                    + " --> Mapped to: " + entry.getValue());
        }
    }

    public boolean isEmpty() {
        return nodeMapping.isEmpty();
    }

    public int size() {
        return nodeMapping.size();
    }
}