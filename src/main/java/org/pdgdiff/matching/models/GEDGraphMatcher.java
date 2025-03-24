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
 * match PDGs from the source and dest file.  Similar "outer loop" to VF2GraphMatcher,
 * but calls GEDMatcher internally for each PDG pair.
 */
public class GEDGraphMatcher extends GraphMatcher {

    public GEDGraphMatcher(List<PDG> srcPdgs, List<PDG> dstPdgs) {
        super(srcPdgs, dstPdgs);
    }

    @Override
    public GraphMapping matchPDGLists() {
        List<PDG> unmappedSrcPdgs = new ArrayList<>(srcPdgs);
        List<PDG> unmappedDstPdgs = new ArrayList<>(dstPdgs);

        while (!unmappedSrcPdgs.isEmpty() && !unmappedDstPdgs.isEmpty()) {
            double minDistance = Double.POSITIVE_INFINITY;
            PDG bestSrcPdg = null;
            PDG bestDstPdg = null;
            NodeMapping bestNodeMapping = null;

            // for each unmatched PDG in src and dest, compute the minimal graph-edit distance
            for (PDG srcPdg : unmappedSrcPdgs) {
                for (PDG dstPdg : unmappedDstPdgs) {
                    GEDMatcher ged = new GEDMatcher(srcPdg, dstPdg);
                    GEDResult result = ged.match();  // get (distance, nodeMapping)

                    if (result != null && result.distance < minDistance) {
                        minDistance = result.distance;
                        bestSrcPdg = srcPdg;
                        bestDstPdg = dstPdg;
                        bestNodeMapping = result.nodeMapping;
                    }
                }
            }

            if (bestSrcPdg != null && bestDstPdg != null) {
                //  found the "best" pair, remove them from the unmatched sets
                unmappedSrcPdgs.remove(bestSrcPdg);
                unmappedDstPdgs.remove(bestDstPdg);

                // the chosen mapping in the global GraphMapping
                graphMapping.addGraphMapping(bestSrcPdg, bestDstPdg, bestNodeMapping);
            } else {
                // No good matches remain
                break;
            }
        }

        return graphMapping;
    }
}
