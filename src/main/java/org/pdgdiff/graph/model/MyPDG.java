package org.pdgdiff.graph.model;

import soot.SootMethod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MyPDG implements Iterable<MyPDGNode> {
    private MyPDGNode startNode;
    private List<MyPDGNode> nodes;
    private String methodSignature;
    private SootMethod method;

    public MyPDG() {
        this.nodes = new ArrayList<>();
    }

    public void setStartNode(MyPDGNode startNode) {
        this.startNode = startNode;
    }

    public MyPDGNode getStartNode() {
        return startNode;
    }

    public void addNode(MyPDGNode node) {
        nodes.add(node);
    }

    public List<MyPDGNode> getNodes() {
        return nodes;
    }

    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public void setMethod(SootMethod method) {
        this.method = method;
    }

    public SootMethod getMethod() {
        return method;
    }

    @Override
    public Iterator<MyPDGNode> iterator() {
        return nodes.iterator();
    }
}
