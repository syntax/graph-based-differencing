package org.pdgdiff.matching;

import org.pdgdiff.matching.models.HeuristicGraphMatcher;
import org.pdgdiff.matching.models.UllmannGraphMatcher;
import org.pdgdiff.matching.models.VF2GraphMatcher;
import soot.toolkits.graph.pdg.HashMutablePDG;

import java.util.List;

public class GraphMatcherFactory {

    public enum MatchingStrategy {
        VF2,
        HEURISTIC,
        ULLMANN
    }

    public static GraphMatcher createMatcher(MatchingStrategy strategy, List<HashMutablePDG> srcPDGs, List<HashMutablePDG> destPDGs) {
        switch (strategy) {
            case VF2:
                return new VF2GraphMatcher(srcPDGs, destPDGs);
            case HEURISTIC:
                return new HeuristicGraphMatcher(srcPDGs, destPDGs);
            case ULLMANN:
                return new UllmannGraphMatcher(srcPDGs, destPDGs);
            default:
                throw new IllegalArgumentException("Unknown matching strategy: " + strategy);
        }
    }
}
