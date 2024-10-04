package org.pdgdiff.matching.models.vf2;

import soot.toolkits.graph.pdg.PDGNode;

class CandidatePair {
    PDGNode n1;
    PDGNode n2;

    public CandidatePair(PDGNode n1, PDGNode n2) {
        this.n1 = n1;
        this.n2 = n2;
    }
}
