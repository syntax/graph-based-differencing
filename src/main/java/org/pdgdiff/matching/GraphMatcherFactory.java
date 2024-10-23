package org.pdgdiff.matching;

import org.pdgdiff.matching.models.HeuristicGraphMatcher;
import org.pdgdiff.matching.models.UllmannGraphMatcher;
import org.pdgdiff.matching.models.VF2GraphMatcher;
import soot.toolkits.graph.pdg.HashMutablePDG;

import java.util.List;

public class GraphMatcherFactory {
    public static GraphMatcher createMatcher(String strategy, List<HashMutablePDG> srcPDGs, List<HashMutablePDG> destPDGs) {
        switch (strategy.toLowerCase()) {
            case "vf2":
                return new VF2GraphMatcher(srcPDGs, destPDGs);
            case "heuristic":
                return new HeuristicGraphMatcher(srcPDGs, destPDGs);
            case "ullmann":
                return new UllmannGraphMatcher(srcPDGs, destPDGs);
            default:
                throw new IllegalArgumentException("Unknown matching strategy: " + strategy);
        }
    }
}
