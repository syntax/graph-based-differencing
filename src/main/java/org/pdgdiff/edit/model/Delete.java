package org.pdgdiff.edit.model;

import org.pdgdiff.graph.model.MyPDGNode;
import soot.toolkits.graph.pdg.PDGNode;

import java.util.Objects;

public class Delete extends EditOperation {
    private int lineNumber;
    private String codeSnippet;

    public Delete(MyPDGNode node, int lineNumber, String codeSnippet) {
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Delete)) return false;
        Delete other = (Delete) obj;
        return lineNumber == other.lineNumber &&
                Objects.equals(codeSnippet, other.codeSnippet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineNumber, codeSnippet);
    }
}
