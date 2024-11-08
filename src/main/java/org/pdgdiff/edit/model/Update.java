package org.pdgdiff.edit.model;

import soot.toolkits.graph.pdg.PDGNode;

import java.util.Objects;

/**
 * Represents an update operation in the edit script.
 */
public class Update extends EditOperation {
    private int oldLineNumber;
    private int newLineNumber;
    private String oldCodeSnippet;
    private String newCodeSnippet;
    private SyntaxDifference syntaxDifference;

    public Update(PDGNode node, int oldLineNumber, int newLineNumber,
                  String oldCodeSnippet, String newCodeSnippet,
                  SyntaxDifference syntaxDifference) {
        super(node);
        this.oldLineNumber = oldLineNumber;
        this.newLineNumber = newLineNumber;
        this.oldCodeSnippet = oldCodeSnippet;
        this.newCodeSnippet = newCodeSnippet;
        this.syntaxDifference = syntaxDifference;
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

    public SyntaxDifference getSyntaxDifference() {
        return syntaxDifference;
    }

    public void setNewCodeSnippet(String newCodeSnippet) {
        this.newCodeSnippet = newCodeSnippet;
    }

    public void setNewLineNumber(int newLineNumber) {
        this.newLineNumber = newLineNumber;
    }

    @Override
    public String getName() {
        return "Update";
    }

    @Override
    public String toString() {
        return String.format("Update at lines %d -> %d:\nOld Code: %s\nNew Code: %s\nDifference: %s",
                oldLineNumber, newLineNumber, oldCodeSnippet, newCodeSnippet, syntaxDifference);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Update)) return false;
        Update other = (Update) obj;
        return oldLineNumber == other.oldLineNumber &&
                newLineNumber == other.newLineNumber &&
                Objects.equals(oldCodeSnippet, other.oldCodeSnippet) &&
                Objects.equals(newCodeSnippet, other.newCodeSnippet) &&
                Objects.equals(syntaxDifference, other.syntaxDifference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldLineNumber, newLineNumber, oldCodeSnippet, newCodeSnippet, syntaxDifference);
    }
}
