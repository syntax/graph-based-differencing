package org.pdgdiff.graph.model;

// single piece of code
public class MyUnit {

    private String code;
    private int lineNumber;

    public MyUnit(String code, int lineNumber) {
        this.code = code;
        this.lineNumber = lineNumber;
    }

    public String getCode() {
        return code;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public String toString() {
        return code;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MyUnit)) return false;
        MyUnit other = (MyUnit) obj;
        return code.equals(other.code) && lineNumber == other.lineNumber;
    }

    @Override
    public int hashCode() {
        return code.hashCode() * 31 + lineNumber;
    }
}
