package org.pdgdiff.matching;

import org.pdgdiff.graph.PDG;
import org.pdgdiff.matching.models.GEDGraphMatcher;
import org.pdgdiff.matching.models.UllmannGraphMatcher;
import org.pdgdiff.matching.models.VF2GraphMatcher;

import java.util.List;

public class GraphMatcherFactory {

    public enum MatchingStrategy {
        VF2,
        ULLMANN,
        GED
    }

    public static GraphMatcher createMatcher(MatchingStrategy strategy, List<PDG> srcPDGs, List<PDG> destPDGs) {
        switch (strategy) {
            case VF2:
                return new VF2GraphMatcher(srcPDGs, destPDGs);
            case ULLMANN:
                return new UllmannGraphMatcher(srcPDGs, destPDGs);
            case GED:
                return new GEDGraphMatcher(srcPDGs, destPDGs);
            default:
                throw new IllegalArgumentException("Unknown matching strategy: " + strategy);
        }
    }
}
