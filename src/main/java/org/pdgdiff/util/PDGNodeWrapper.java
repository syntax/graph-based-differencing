package org.pdgdiff.util;

import soot.toolkits.graph.pdg.PDGNode;

import java.util.Objects;

/*
* Wrapper class to act as a helper to VF2 algorithm. This class is used to wrap PDGNode objects and provide custom equals
* funtionality, which is used by the VF2 algorithm to compare nodes. Basically a heuristic equals.
 */
public class PDGNodeWrapper {
    private final PDGNode pdgNode;

    public PDGNodeWrapper(PDGNode pdgNode) {
        this.pdgNode = pdgNode;
    }

    public PDGNode getPDGNode() {
        return pdgNode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PDGNodeWrapper)) return false;
        PDGNodeWrapper other = (PDGNodeWrapper) obj;
        // Use relevant fields of PDGNode for equality comparison
        return pdgNode.getType() == other.pdgNode.getType() &&
                pdgNode.getAttrib() == other.pdgNode.getAttrib() &&
                pdgNode.getNode().equals(other.pdgNode.getNode());  // Assuming getNode() uniquely identifies a node
    }

    @Override
    public int hashCode() {
        return Objects.hash(pdgNode.getType(), pdgNode.getAttrib(), pdgNode.getNode());
    }
}
