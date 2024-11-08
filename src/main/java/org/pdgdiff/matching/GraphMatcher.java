package org.pdgdiff.matching;

import org.pdgdiff.graph.model.MyPDG;

import java.util.HashSet;
import java.util.List;

/**
 * GraphMatcher abstract class to compare two lists of PDGs and find similarities, aiming to create a 1:1 mapping between
 * PDGs from the two lists. These PDGs represent methods, which each list representing a different class (or file)
 * This class uses a custom similarity score to compare PDG nodes and labels. WIP.
 */
public abstract class GraphMatcher {
    protected final HashSet<MyPDG> matchedPDGs;
    protected List<MyPDG> pdgList1;
    protected List<MyPDG> pdgList2;
    protected GraphMapping graphMapping; // To store graph-level and node-level mappings

    public GraphMatcher(List<MyPDG> list1, List<MyPDG> list2) {
        this.pdgList1 = list1;
        this.pdgList2 = list2;
        this.graphMapping = new GraphMapping();
        this.matchedPDGs = new HashSet<>();
    }

    public abstract GraphMapping matchPDGLists();
}