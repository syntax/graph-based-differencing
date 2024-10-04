package org.pdgdiff.matching.models;

import org.pdgdiff.matching.GraphMapping;
import org.pdgdiff.matching.GraphMatcher;
import org.pdgdiff.matching.NodeMapping;
import org.pdgdiff.matching.models.heuristic.HeuristicMatcher;
import soot.toolkits.graph.pdg.HashMutablePDG;

import java.util.List;

public class HeuristicGraphMatcher extends GraphMatcher {
    public HeuristicGraphMatcher(List<HashMutablePDG> list1, List<HashMutablePDG> list2) {
        super(list1, list2);
    }

    @Override
    public GraphMapping matchPDGLists() {
        HeuristicMatcher heuristicMatcher = new HeuristicMatcher();

        for (HashMutablePDG pdg1 : pdgList1) {
            HashMutablePDG bestMatch = null;
            NodeMapping bestNodeMapping = null; // Track node-level mappings
            double bestScore = Double.NEGATIVE_INFINITY;  // Start with a very low score

            // Compare pdg1 with each PDG from the second list
            for (HashMutablePDG pdg2 : pdgList2) {
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
