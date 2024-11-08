package org.pdgdiff.matching.models.vf2;

import org.pdgdiff.graph.model.MyPDGNode;
import soot.toolkits.graph.pdg.PDGNode;

class CandidatePair {
    MyPDGNode n1;
    MyPDGNode n2;

    public CandidatePair(MyPDGNode n1, MyPDGNode n2) {
        this.n1 = n1;
        this.n2 = n2;
    }
}
