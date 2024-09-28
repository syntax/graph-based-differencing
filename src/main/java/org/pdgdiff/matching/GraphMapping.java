package org.pdgdiff.matching;

import soot.toolkits.graph.pdg.PDGNode;

import java.util.HashMap;
import java.util.Map;

// TODO: this is just skeleton code.

public class GraphMapping {
    private Map<PDGNode, PDGNode> nodeMapping;

    public GraphMapping() {
        nodeMapping = new HashMap<PDGNode, PDGNode>();
    }

    public void addMapping(PDGNode srcNode, PDGNode dstNode) {
        nodeMapping.put(srcNode, dstNode);
    }

    public PDGNode getMappedNode(PDGNode node) {
        return nodeMapping.get(node);
    }
}
