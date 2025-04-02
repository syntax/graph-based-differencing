package org.pdgdiff.matching;

import org.pdgdiff.graph.PDG;

import java.util.HashSet;
import java.util.List;

/**
 * GraphMatcher abstract class to compare two lists of PDGs and find similarities, aiming to create a 1:1 mapping between
 * PDGs from the two lists. A PDGs represents a methods, with each list representing a different class
 */
public abstract class GraphMatcher {
    protected final HashSet<PDG> matchedPDGs;
    protected List<PDG> srcPdgs;
    protected List<PDG> dstPdgs;
    protected GraphMapping graphMapping; // to store graph-level and node-level mappings

    public GraphMatcher(List<PDG> srcPdgs, List<PDG> dstPdgs) {
        this.srcPdgs = srcPdgs;
        this.dstPdgs = dstPdgs;
        this.graphMapping = new GraphMapping();
        this.matchedPDGs = new HashSet<>();
    }

    public abstract GraphMapping matchPDGLists();
}