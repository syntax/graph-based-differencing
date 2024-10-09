package org.pdgdiff.edit;

import soot.toolkits.graph.pdg.PDGNode;

public class DeleteEdge extends EditOperation {
    private PDGNode from;
    private PDGNode to;

    public DeleteEdge(PDGNode from, PDGNode to) {
        super(OperationType.DELETE_EDGE);
        this.from = from;
        this.to = to;
    }

    public PDGNode getFrom() {
        return from;
    }

    public PDGNode getTo() {
        return to;
    }

    @Override
    public String toString() {
        return "Delete Edge: " + from.toShortString() + " -> " + to.toShortString();
    }
}
