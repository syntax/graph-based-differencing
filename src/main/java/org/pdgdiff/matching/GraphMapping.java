package org.pdgdiff.matching;

import soot.toolkits.graph.pdg.PDGNode;

import java.util.HashMap;
import java.util.Map;

// TODO: this is just skeleton code.

public class GraphMapping {
    private Map<PDGNode, PDGNode> nodeMapping;

    public GraphMapping() {
        nodeMapping = new HashMap<>();
    }

    public void addMapping(PDGNode node1, PDGNode node2) {
        nodeMapping.put(node1, node2);
    }

    public PDGNode getMappedNode(PDGNode node) {
        return nodeMapping.get(node);
    }
}
