package org.pdgdiff.graph;

import soot.toolkits.graph.HashMutableEdgeLabelledDirectedGraph;
import soot.toolkits.graph.UnitGraph;


// TODO: this class will be my own version of the HashMutuablePDG that Soot presents, hopefully made slightly more accurate to
// TODO: the original literature.
public class HashMutablePDG extends HashMutableEdgeLabelledDirectedGraph {
    private UnitGraph cfg = null;

    public HashMutablePDG() {
        super();
    }

    public void setCFG(UnitGraph cfg) {
        this.cfg = cfg;
    }

    public UnitGraph getCFG() {
        return cfg;
    }

}
