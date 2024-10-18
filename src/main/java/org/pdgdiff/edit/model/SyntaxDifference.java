package org.pdgdiff.edit.model;

import org.pdgdiff.util.SourceCodeMapper;
import soot.Unit;
import soot.tagkit.LineNumberTag;
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

    private int oldLineNumber;
    private int newLineNumber;
    private String oldCodeSnippet;
    private String newCodeSnippet;

    private String oldJimpleCode;
    private String newJimpleCode;

    // Constructors for Unit differences
    public SyntaxDifference(Unit oldUnit, Unit newUnit,
                            SourceCodeMapper oldSourceMapper, SourceCodeMapper newSourceMapper) {
        this.oldUnit = oldUnit;
        this.newUnit = newUnit;
        this.oldLineNumber = getLineNumber(oldUnit);
        this.newLineNumber = getLineNumber(newUnit);
        this.oldCodeSnippet = oldSourceMapper.getCodeLine(oldLineNumber);
        this.newCodeSnippet = newSourceMapper.getCodeLine(newLineNumber);
        this.oldJimpleCode = oldUnit != null ? oldUnit.toString() : null;
        this.newJimpleCode = newUnit != null ? newUnit.toString() : null;
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

    public int getOldLineNumber() {
        return oldLineNumber;
    }

    public int getNewLineNumber() {
        return newLineNumber;
    }

    public String getOldCodeSnippet() {
        return oldCodeSnippet;
    }

    public String getNewCodeSnippet() {
        return newCodeSnippet;
    }

    public String getOldJimpleCode() {
        return oldJimpleCode;
    }

    public String getNewJimpleCode() {
        return newJimpleCode;
    }

    @Override
    public String toString() {
        if (message != null) {
            return message;
        } else if (oldUnit != null || newUnit != null) {
            return String.format("Unit Difference at lines %d -> %d:\nOld Code: '%s'\nNew Code: '%s'\nOld Jimple: '%s'\nNew Jimple: '%s'",
                    oldLineNumber, newLineNumber,
                    oldCodeSnippet == null ? "null" : oldCodeSnippet.trim(),
                    newCodeSnippet == null ? "null" : newCodeSnippet.trim(),
                    oldJimpleCode == null ? "null" : oldJimpleCode.trim(),
                    newJimpleCode == null ? "null" : newJimpleCode.trim());
        } else {
            return String.format("Node Difference: '%s' -> '%s'",
                    oldNode == null ? "null" : oldNode.toShortString(),
                    newNode == null ? "null" : newNode.toShortString());
        }
    }

    private int getLineNumber(Unit unit) {
        if (unit == null) {
            return -1;
        }
        LineNumberTag tag = (LineNumberTag) unit.getTag("LineNumberTag");
        if (tag != null) {
            return tag.getLineNumber();
        }
        return -1;
    }
}
