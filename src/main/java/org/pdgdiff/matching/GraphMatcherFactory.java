package org.pdgdiff.matching;

import org.pdgdiff.matching.models.HeuristicGraphMatcher;
import org.pdgdiff.matching.models.UllmannGraphMatcher;
import org.pdgdiff.matching.models.VF2GraphMatcher;
import soot.toolkits.graph.pdg.HashMutablePDG;

import java.util.List;

public class GraphMatcherFactory {
    public static GraphMatcher createMatcher(String strategy, List<HashMutablePDG> pdgList1, List<HashMutablePDG> pdgList2) {
        switch (strategy.toLowerCase()) {
            case "vf2":
                return new VF2GraphMatcher(pdgList1, pdgList2);
            case "heuristic":
                return new HeuristicGraphMatcher(pdgList1, pdgList2);
            case "ullmann":
                return new UllmannGraphMatcher(pdgList1, pdgList2);
            default:
                throw new IllegalArgumentException("Unknown matching strategy: " + strategy);
        }
    }
}
