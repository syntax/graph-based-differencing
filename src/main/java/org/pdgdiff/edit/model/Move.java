package org.pdgdiff.edit.model;

import soot.toolkits.graph.pdg.PDGNode;

import java.util.List;

/**
 * Represents a move operation in a PDG.
 */
public class Move extends EditOperation {
    private List<PDGNode> oldPredecessors;
    private List<PDGNode> newPredecessors;

    public Move(PDGNode node, List<PDGNode> oldPredecessors, List<PDGNode> newPredecessors) {
        super(node);
        this.oldPredecessors = oldPredecessors;
        this.newPredecessors = newPredecessors;
    }

    public List<PDGNode> getOldPredecessors() {
        return oldPredecessors;
    }

    public List<PDGNode> getNewPredecessors() {
        return newPredecessors;
    }

    @Override
    public String getName() {
        return "Move";
    }

    @Override
    public String toString() {
        return String.format("Move %s from predecessors %s to %s",
                node.toShortString(),
                oldPredecessors.toString(),
                newPredecessors.toString());
    }
}
