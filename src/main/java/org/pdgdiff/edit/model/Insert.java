package org.pdgdiff.edit.model;

import soot.toolkits.graph.pdg.PDGNode;

public class Insert extends EditOperation {
    private int lineNumber;
    private String codeSnippet;

    public Insert(PDGNode node, int lineNumber, String codeSnippet) {
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
        return "Insert";
    }

    @Override
    public String toString() {
        return String.format("Insert at line %d: %s", lineNumber, codeSnippet);
    }
}
