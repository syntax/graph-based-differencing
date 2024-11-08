package org.pdgdiff.edit.model;

import org.pdgdiff.graph.model.MyPDGNode;
import soot.toolkits.graph.pdg.PDGNode;

public abstract class EditOperation {
    protected MyPDGNode node;

    public EditOperation(MyPDGNode node) {
        this.node = node;
    }

    public MyPDGNode getNode() {
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
