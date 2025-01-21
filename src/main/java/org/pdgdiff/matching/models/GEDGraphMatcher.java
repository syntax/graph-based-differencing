package org.pdgdiff.matching.models;

import org.pdgdiff.graph.PDG;
import org.pdgdiff.matching.GraphMapping;
import org.pdgdiff.matching.GraphMatcher;
import org.pdgdiff.matching.NodeMapping;
import org.pdgdiff.matching.models.ged.GEDMatcher;
import org.pdgdiff.matching.models.ged.GEDResult;

import java.util.ArrayList;
import java.util.List;

/**
 * A GraphMatcher that uses a Graph Edit Distance approach to
 * match PDGs from list1 and list2.  Similar "outer loop" to VF2GraphMatcher,
 * but calls GEDMatcher internally for each PDG pair.
 */
public class GEDGraphMatcher extends GraphMatcher {

    public GEDGraphMatcher(List<PDG> list1, List<PDG> list2) {
        super(list1, list2);
    }

    @Override
    public GraphMapping matchPDGLists() {
        // Copy the PDG lists so we can track unmatched pairs
        List<PDG> unmappedPDGs1 = new ArrayList<>(pdgList1);
        List<PDG> unmappedPDGs2 = new ArrayList<>(pdgList2);

        while (!unmappedPDGs1.isEmpty() && !unmappedPDGs2.isEmpty()) {
            double minDistance = Double.POSITIVE_INFINITY;
            PDG bestPdg1 = null;
            PDG bestPdg2 = null;
            NodeMapping bestNodeMapping = null;

            // For each unmatched PDG in src and dest, compute the minimal graph-edit distance
            for (PDG pdg1 : unmappedPDGs1) {
                for (PDG pdg2 : unmappedPDGs2) {
                    // Use the GEDMatcher on this pair
                    GEDMatcher ged = new GEDMatcher(pdg1, pdg2);
                    GEDResult result = ged.match();  // get (distance, nodeMapping)

                    if (result != null && result.distance < minDistance) {
                        minDistance = result.distance;
                        bestPdg1 = pdg1;
                        bestPdg2 = pdg2;
                        bestNodeMapping = result.nodeMapping;
                    }
                }
            }

            if (bestPdg1 != null && bestPdg2 != null) {
                //  found the "best" pair, remove them from the unmatched sets
                unmappedPDGs1.remove(bestPdg1);
                unmappedPDGs2.remove(bestPdg2);

                // the chosen mapping in the global GraphMapping
                graphMapping.addGraphMapping(bestPdg1, bestPdg2, bestNodeMapping);
            } else {
                // No good matches remain
                break;
            }
        }

        return graphMapping;
    }
}
