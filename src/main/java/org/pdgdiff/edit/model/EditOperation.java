package org.pdgdiff.edit.model;

import soot.toolkits.graph.pdg.PDGNode;

public abstract class EditOperation {
    protected PDGNode node;

    public EditOperation(PDGNode node) {
        this.node = node;
    }

    public PDGNode getNode() {
        return node;
    }

    public abstract String getName();

    @Override
    public abstract String toString();
}
