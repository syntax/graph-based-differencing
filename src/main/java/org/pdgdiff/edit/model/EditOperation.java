package org.pdgdiff.edit.model;

import soot.toolkits.graph.pdg.PDGNode;

/**
 * Base class for all edit operations in the edit script.
 */
public abstract class EditOperation {
    protected PDGNode node;

    public EditOperation(PDGNode node) {
        this.node = node;
    }

    public PDGNode getNode() {
        return node;
    }

    public abstract String getName();

    public abstract String toString();
}
