package org.pdgdiff.edit.model;

import soot.toolkits.graph.pdg.PDGNode;

/**
 * Represents a delete operation in a PDG.
 */
public class Delete extends EditOperation {
    public Delete(PDGNode node) {
        super(node);
    }

    @Override
    public String getName() {
        return "Delete";
    }

    @Override
    public String toString() {
        return String.format("Delete %s", node.toShortString());
    }
}
