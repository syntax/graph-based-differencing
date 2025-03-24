package org.pdgdiff.graph;

import soot.toolkits.graph.HashMutableEdgeLabelledDirectedGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.pdg.PDGNode;

import java.util.List;

/**
 * Program Dependency Graph (PDG) class that extends the Soot HashMutableEdgeLabelledDirectedGraph. this is
 * similar to the soot HashMutablePDG, but removes some abstractions that remove the granularity of the PDG and allows
 * for specific edge types to be added to the graph.
 */
public class PDG extends HashMutableEdgeLabelledDirectedGraph<PDGNode, GraphGenerator.DependencyTypes> {
    private UnitGraph cfg = null;
    protected PDGNode startNode = null;

    public PDG() {
        super();
    }

    public void setCFG(UnitGraph cfg) {
        this.cfg = cfg;
    }

    public UnitGraph getCFG() {
        return cfg;
    }

    public PDGNode getStartNode() {
        return startNode;
    }

    public boolean hasDataEdge(PDGNode src, PDGNode tgt) {
        return this.containsEdge(src, tgt, GraphGenerator.DependencyTypes.DATA_DEPENDENCY);
    }

    public boolean hasControlEdge(PDGNode src, PDGNode tgt) {
        return this.containsEdge(src, tgt, GraphGenerator.DependencyTypes.CONTROL_DEPENDENCY);
    }

    public List<GraphGenerator.DependencyTypes> getEdgeLabels(PDGNode src, PDGNode tgt) {
        return this.getLabelsForEdges(src, tgt);
    }
}
