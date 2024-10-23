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

    // following are to prevent duplicate entries in edit scripts
    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();
}
