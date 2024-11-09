package org.pdgdiff.graph.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MyPDGNode {
    private Object node;        // this will either by block or region
    private MyPDGNodeType type;
    private String attrib;
    private List<MyPDGNode> dependents;
    private List<MyPDGNode> backDependents;
    private int lineNumber;

    public MyPDGNode(Object node, MyPDGNodeType type, String attrib) {
        this.node = node;
        this.type = type;
        this.attrib = attrib;
        this.dependents = new ArrayList<>();
        this.backDependents = new ArrayList<>();
    }

    public Object getNode() {
        return node;
    }

    public MyPDGNodeType getType() {
        return type;
    }

    public String getAttrib() {
        return attrib;
    }

    public List<MyPDGNode> getDependents() {
        return dependents;
    }

    public List<MyPDGNode> getBackDependets() {
        return backDependents;
    }

    // add dependent nodes
    public void addDependent(MyPDGNode node) {
        this.dependents.add(node);
    }

    public void addBackDependent(MyPDGNode node) {
        this.backDependents.add(node);
    }

    // Utility methods
    public String toShortString() {
        return "Node[type=" + type + ", attrib=" + attrib + "]";
    }

    @Override
    public String toString() {
        return "MyPDGNode{" +
                "type=" + type +
                ", attrib='" + attrib + '\'' +
                '}';
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    // equals and hashCode for correct functioning in collections
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MyPDGNode that = (MyPDGNode) o;
        return Objects.equals(node, that.node) &&
                type == that.type &&
                Objects.equals(attrib, that.attrib);
    }

    @Override
    public int hashCode() {
        return Objects.hash(node, type, attrib);
    }
}
