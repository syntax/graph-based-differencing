package org.pdgdiff.edit.model;

import org.pdgdiff.edit.EditScriptGenerator;
import org.pdgdiff.util.SourceCodeMapper;
import soot.Unit;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.pdg.PDGNode;

import java.util.Objects;

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

    public SyntaxDifference(PDGNode oldNode, PDGNode newNode,
                            SourceCodeMapper oldSourceMapper, SourceCodeMapper newSourceMapper) {
        this.oldNode = oldNode;
        this.newNode = newNode;
        this.oldLineNumber = getNodeLineNumber(oldNode);
        this.newLineNumber = getNodeLineNumber(newNode);
        this.oldCodeSnippet = getNodeCodeSnippet(oldNode, oldSourceMapper);
        this.newCodeSnippet = getNodeCodeSnippet(newNode, newSourceMapper);
    }

    // Constructor for general messages
    public SyntaxDifference(String message) {
        this.message = message;
    }

    // Getters
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
            return String.format(
                    "Unit Difference at lines %d -> %d:\nOld Code: '%s'\nNew Code: '%s'\nOld Jimple: '%s'\nNew Jimple: '%s'",
                    oldLineNumber, newLineNumber,
                    oldCodeSnippet == null ? "null" : oldCodeSnippet.trim(),
                    newCodeSnippet == null ? "null" : newCodeSnippet.trim(),
                    oldJimpleCode == null ? "null" : oldJimpleCode.trim(),
                    newJimpleCode == null ? "null" : newJimpleCode.trim());
        } else if (oldNode != null || newNode != null) {
            return String.format(
                    "Node Difference at lines %d -> %d:\nOld Code: '%s'\nNew Code: '%s'",
                    oldLineNumber, newLineNumber,
                    oldCodeSnippet == null ? "null" : oldCodeSnippet.trim(),
                    newCodeSnippet == null ? "null" : newCodeSnippet.trim());
        } else {
            return "Unknown Difference";
        }
    }

    // Helper methods
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

    private int getNodeLineNumber(PDGNode node) {
        if (node == null) {
            return -1;
        }
        return EditScriptGenerator.getNodeLineNumber(node);
    }

    private String getNodeCodeSnippet(PDGNode node, SourceCodeMapper codeMapper) {
        int lineNumber = getNodeLineNumber(node);
        if (lineNumber != -1) {
            return codeMapper.getCodeLine(lineNumber);
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SyntaxDifference)) return false;
        SyntaxDifference that = (SyntaxDifference) obj;
        return oldLineNumber == that.oldLineNumber &&
                newLineNumber == that.newLineNumber &&
                Objects.equals(oldCodeSnippet, that.oldCodeSnippet) &&
                Objects.equals(newCodeSnippet, that.newCodeSnippet) &&
                Objects.equals(oldJimpleCode, that.oldJimpleCode) &&
                Objects.equals(newJimpleCode, that.newJimpleCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldLineNumber, newLineNumber, oldCodeSnippet, newCodeSnippet, oldJimpleCode, newJimpleCode);
    }
}
