package org.pdgdiff.matching.models.vf2;

import org.pdgdiff.util.PDGNodeWrapper;
import soot.toolkits.graph.pdg.PDGNode;

class CandidatePair {
    PDGNodeWrapper n1;
    PDGNodeWrapper n2;

    public CandidatePair(PDGNodeWrapper n1, PDGNodeWrapper n2) {
        this.n1 = n1;
        this.n2 = n2;
    }
}
