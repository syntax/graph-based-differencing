package org.pdgdiff.edit;

import soot.toolkits.graph.pdg.PDGNode;

public class InsertNode extends EditOperation {
    private PDGNode node;

    public InsertNode(PDGNode node) {
        super(OperationType.INSERT_NODE);
        this.node = node;
    }

    public PDGNode getNode() {
        return node;
    }

    @Override
    public String toString() {
        return "Insert Node: " + node.toShortString();
    }
}
