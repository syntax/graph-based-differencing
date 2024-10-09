package org.pdgdiff.edit;

import soot.toolkits.graph.pdg.PDGNode;

public class InsertEdge extends EditOperation {
    private PDGNode from;
    private PDGNode to;

    public InsertEdge(PDGNode from, PDGNode to) {
        super(OperationType.INSERT_EDGE);
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
        return "Insert Edge: " + from.toShortString() + " -> " + to.toShortString();
    }
}
