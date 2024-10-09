package org.pdgdiff.edit;

import soot.toolkits.graph.pdg.PDGNode;

public class DeleteNode extends EditOperation {
    private PDGNode node;

    public DeleteNode(PDGNode node) {
        super(OperationType.DELETE_NODE);
        this.node = node;
    }

    public PDGNode getNode() {
        return node;
    }

    @Override
    public String toString() {
        return "Delete Node: " + node.toShortString();
    }
}
