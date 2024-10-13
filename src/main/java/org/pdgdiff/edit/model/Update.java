package org.pdgdiff.edit.model;

import soot.toolkits.graph.pdg.PDGNode;

import java.util.List;

/**
 * Represents an update operation in a PDG.
 */
public class Update extends EditOperation {
    private String oldValue;
    private String newValue;
    private List<SyntaxDifference> syntaxDifferences;

    public Update(PDGNode node, String oldValue, String newValue, List<SyntaxDifference> syntaxDifferences) {
        super(node);
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.syntaxDifferences = syntaxDifferences;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public List<SyntaxDifference> getSyntaxDifferences() {
        return syntaxDifferences;
    }

    @Override
    public String getName() {
        return "Update";
    }

    @Override
    public String toString() {
        return String.format("Update %s from '%s' to '%s' with syntax differences: %s",
                node.toShortString(), oldValue, newValue, syntaxDifferences);
    }
}
