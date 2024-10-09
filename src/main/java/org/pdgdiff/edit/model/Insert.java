package org.pdgdiff.edit.model;

import soot.toolkits.graph.pdg.PDGNode;

/**
 * Represents an insert operation in a PDG.
 */
public class Insert extends EditOperation {
    public Insert(PDGNode node) {
        super(node);
    }

    @Override
    public String getName() {
        return "Insert";
    }

    @Override
    public String toString() {
        return String.format("Insert %s", node.toShortString());
    }
}
