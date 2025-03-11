package org.pdgdiff.matching.models;

import org.pdgdiff.graph.PDG;
import org.pdgdiff.matching.GraphMapping;
import org.pdgdiff.matching.GraphMatcher;
import org.pdgdiff.matching.NodeMapping;
import org.pdgdiff.matching.models.heuristic.HeuristicMatcher;

import java.util.List;

public class HeuristicGraphMatcher extends GraphMatcher {
    public HeuristicGraphMatcher(List<PDG> list1, List<PDG> list2) {
        super(list1, list2);
    }

    @Override
    public GraphMapping matchPDGLists() {
        HeuristicMatcher heuristicMatcher = new HeuristicMatcher();

        for (PDG pdg1 : srcPdgs) {
            PDG bestMatch = null;
            NodeMapping bestNodeMapping = null; // Track node-level mappings
            double bestScore = Double.NEGATIVE_INFINITY;  // Start with a very low score

            // Compare pdg1 with each PDG from the second list
            for (PDG pdg2 : dstPdgs) {
                // Skip if this PDG has already been matched
                if (matchedPDGs.contains(pdg2)) {
                    continue;
                }

                NodeMapping nodeMapping = new NodeMapping();
                double score = heuristicMatcher.comparePDGs(pdg1, pdg2, nodeMapping);

                // Keep track of the best match based on the similarity score
                if (score > bestScore) {
                    bestScore = score;
                    bestMatch = pdg2;
                    bestNodeMapping = nodeMapping;
                }
            }

            if (bestMatch != null) {
                // Add the best match along with node mapping to the GraphMapping
                matchedPDGs.add(bestMatch);
                graphMapping.addGraphMapping(pdg1, bestMatch, bestNodeMapping);
            }
        }

        return graphMapping;  // Return the complete GraphMapping
    }
}
