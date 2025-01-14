package org.pdgdiff.edit.model;

import soot.toolkits.graph.pdg.PDGNode;

import java.util.Objects;

public class Insert extends EditOperation {
    private int lineNumber;
    private String codeSnippet;

    public Insert(PDGNode node, int lineNumber, String codeSnippet) {
        super(node);
        this.lineNumber = lineNumber;
        this.codeSnippet = codeSnippet;
    }

    public PDGNode getNode() { return node; }

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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Insert)) return false;
        Insert other = (Insert) obj;
        return lineNumber == other.lineNumber &&
                Objects.equals(codeSnippet, other.codeSnippet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineNumber, codeSnippet);
    }
}
