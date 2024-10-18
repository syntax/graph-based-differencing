package org.pdgdiff.edit.model;

import soot.toolkits.graph.pdg.PDGNode;

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
}
