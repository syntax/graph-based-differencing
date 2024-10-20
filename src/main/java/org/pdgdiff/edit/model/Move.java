package org.pdgdiff.edit.model;

import soot.toolkits.graph.pdg.PDGNode;

import java.util.Objects;

/**
 * Represents a move operation in the edit script.
 */
public class Move extends EditOperation {
    // TODO: Fix this new version of move, it doesnt work lol.
    private int oldLineNumber;
    private int newLineNumber;
    private String codeSnippet;

    public Move(PDGNode node, int oldLineNumber, int newLineNumber, String codeSnippet) {
        super(node);
        this.oldLineNumber = oldLineNumber;
        this.newLineNumber = newLineNumber;
        this.codeSnippet = codeSnippet;
    }

    public int getOldLineNumber() {
        return oldLineNumber;
    }

    public int getNewLineNumber() {
        return newLineNumber;
    }

    public String getCodeSnippet() {
        return codeSnippet;
    }

    @Override
    public String getName() {
        return "Move";
    }

    @Override
    public String toString() {
        return String.format("Move from line %d to line %d: %s", oldLineNumber, newLineNumber, codeSnippet);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Move)) return false;
        Move other = (Move) obj;
        return oldLineNumber == other.oldLineNumber &&
                newLineNumber == other.newLineNumber &&
                Objects.equals(codeSnippet, other.codeSnippet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldLineNumber, newLineNumber, codeSnippet);
    }

}
