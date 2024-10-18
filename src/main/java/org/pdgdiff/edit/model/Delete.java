package org.pdgdiff.edit.model;

import soot.toolkits.graph.pdg.PDGNode;

public class Delete extends EditOperation {
    private int lineNumber;
    private String codeSnippet;

    public Delete(PDGNode node, int lineNumber, String codeSnippet) {
        super(node);
        this.lineNumber = lineNumber;
        this.codeSnippet = codeSnippet;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getCodeSnippet() {
        return codeSnippet;
    }

    @Override
    public String getName() {
        return "Delete";
    }

    @Override
    public String toString() {
        return String.format("Delete at line %d: %s", lineNumber, codeSnippet);
    }
}
