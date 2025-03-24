package org.pdgdiff.matching.models;

import org.pdgdiff.graph.GraphTraversal;
import org.pdgdiff.graph.PDG;
import org.pdgdiff.matching.GraphMapping;
import org.pdgdiff.matching.GraphMatcher;
import org.pdgdiff.matching.NodeMapping;
import org.pdgdiff.matching.models.vf2.VF2Matcher;

import java.util.ArrayList;
import java.util.List;

public class VF2GraphMatcher extends GraphMatcher {
    public VF2GraphMatcher(List<PDG> srcPdgs, List<PDG> dstPdgs) {
        super(srcPdgs, dstPdgs);
    }

    @Override
    public GraphMapping matchPDGLists() {
        List<PDG> unmappedSrcPdgs = new ArrayList<>(srcPdgs);
        List<PDG> unmappedDstPdgs = new ArrayList<>(dstPdgs);

        while (!unmappedSrcPdgs.isEmpty() && !unmappedDstPdgs.isEmpty()) {
            double maxScore = Double.NEGATIVE_INFINITY;
            PDG bestSrcPdg = null;
            PDG bestDstPdg = null;
            NodeMapping bestNodeMapping = null;

            // for each pair of unmapped PDGs, compute similarity score
            for (PDG srcPdg : unmappedSrcPdgs) {
                for (PDG dstPdg : unmappedDstPdgs) {
                    VF2Matcher vf2Matcher = new VF2Matcher(srcPdg, dstPdg);
                    NodeMapping nodeMapping = vf2Matcher.match();

                    if (nodeMapping != null && !nodeMapping.isEmpty()) {
                        int mappedNodes = nodeMapping.size();
                        int unmappedSrcNodes = GraphTraversal.getNodeCount(srcPdg) - mappedNodes;
                        int unmappedDstNodes = GraphTraversal.getNodeCount(dstPdg) - mappedNodes;

                        // calculate the score that minimizes unmapped nodes, this is my 'similarity' metric as of rn lol
                        // this might be to be improved. TODO look into other metrics/ measures.
                        // TODO might want to add a threshold. possibly not all graphs should be mapped to all graphs!
                        double score = (double) mappedNodes / (mappedNodes + unmappedSrcNodes + unmappedDstNodes);

                        if (score > maxScore) {
                            maxScore = score;
                            bestSrcPdg = srcPdg;
                            bestDstPdg = dstPdg;
                            bestNodeMapping = nodeMapping;
                        }
                    }
                }
            }

            if (bestSrcPdg != null && bestDstPdg != null) {
                unmappedSrcPdgs.remove(bestSrcPdg);
                unmappedDstPdgs.remove(bestDstPdg);
                graphMapping.addGraphMapping(bestSrcPdg, bestDstPdg, bestNodeMapping);
            } else {
                break;
            }
        }

        // handling PDGs in src that were not matched
        for (PDG pdg1 : unmappedSrcPdgs) {
            System.out.println("No matching PDG found for: " + pdg1.getCFG().getBody().getMethod().getSignature());
        }

        return graphMapping;
    }
}
