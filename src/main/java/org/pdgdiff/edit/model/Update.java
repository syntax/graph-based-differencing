package org.pdgdiff.edit.model;

import soot.toolkits.graph.pdg.PDGNode;

/**
 * Represents an update operation in a PDG.
 */
public class Update extends EditOperation {
    private String oldValue;
    private String newValue;

    public Update(PDGNode node, String oldValue, String newValue) {
        super(node);
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    @Override
    public String getName() {
        return "Update";
    }

    @Override
    public String toString() {
        return String.format("Update %s from '%s' to '%s'", node.toShortString(), oldValue, newValue);
    }
}
