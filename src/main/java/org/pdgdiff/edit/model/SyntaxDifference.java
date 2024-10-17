package org.pdgdiff.edit.model;

import soot.Unit;
import soot.toolkits.graph.pdg.PDGNode;

/**
 * Represents a syntax difference between two Units or PDGNodes.
 */
public class SyntaxDifference {
    private Unit oldUnit;
    private Unit newUnit;
    private PDGNode oldNode;
    private PDGNode newNode;
    private String message;

    // Constructors for Unit differences
    public SyntaxDifference(Unit oldUnit, Unit newUnit) {
        this.oldUnit = oldUnit;
        this.newUnit = newUnit;
    }

    // Constructors for PDGNode differences (e.g., REGION nodes)
    public SyntaxDifference(PDGNode oldNode, PDGNode newNode) {
        this.oldNode = oldNode;
        this.newNode = newNode;
    }

    // Constructor for general messages
    public SyntaxDifference(String message) {
        this.message = message;
    }

    public Unit getOldUnit() {
        return oldUnit;
    }

    public Unit getNewUnit() {
        return newUnit;
    }

    public PDGNode getOldNode() {
        return oldNode;
    }

    public PDGNode getNewNode() {
        return newNode;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        if (message != null) {
            return message;
        } else if (oldUnit != null || newUnit != null) {
            return String.format("Unit Difference: '%s' -> '%s' [Old line num: %s, New line num: %s]",
                    oldUnit == null ? "null" : oldUnit.toString(),
                    newUnit == null ? "null" : newUnit.toString(),
                    oldUnit == null ? "null" : oldUnit.getTags().toString(),
                    newUnit == null ? "null" : newUnit.getTags().toString());
        } else {
            return String.format("Node Difference: '%s' -> '%s'",
                    oldNode == null ? "null" : oldNode.toShortString(),
                    newNode == null ? "null" : newNode.toShortString());
        }
    }
}
