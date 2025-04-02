package org.pdgdiff.matching.models;

import org.pdgdiff.graph.GraphTraversal;
import org.pdgdiff.graph.PDG;
import org.pdgdiff.matching.GraphMapping;
import org.pdgdiff.matching.GraphMatcher;
import org.pdgdiff.matching.NodeMapping;
import org.pdgdiff.matching.models.ullmann.UllmannMatcher;

import java.util.ArrayList;
import java.util.List;

public class UllmannGraphMatcher extends GraphMatcher {
    public UllmannGraphMatcher(List<PDG> list1, List<PDG> list2) {
        super(list1, list2);
    }

    @Override
    public GraphMapping matchPDGLists() {
        List<PDG> unmappedPDGs1 = new ArrayList<>(srcPdgs);
        List<PDG> unmappedPDGs2 = new ArrayList<>(dstPdgs);

        while (!unmappedPDGs1.isEmpty() && !unmappedPDGs2.isEmpty()) {
            double maxScore = Double.NEGATIVE_INFINITY;
            PDG bestPdg1 = null;
            PDG bestPdg2 = null;
            NodeMapping bestNodeMapping = null;

            // for each pair of unmapped PDGs, compute similarity score
            for (PDG pdg1 : unmappedPDGs1) {
                for (PDG pdg2 : unmappedPDGs2) {
                    UllmannMatcher ullmannMatcher = new UllmannMatcher(pdg1, pdg2);
                    NodeMapping nodeMapping = ullmannMatcher.match();

                    if (nodeMapping != null && !nodeMapping.isEmpty()) {
                        int mappedNodes = nodeMapping.size();
                        int unmappedNodes1 = GraphTraversal.getNodeCount(pdg1) - mappedNodes;
                        int unmappedNodes2 = GraphTraversal.getNodeCount(pdg2) - mappedNodes;

                        // TODO: this is using same score as vf2 matcher, again not sure if this is ideal!
                        double score = (double) mappedNodes / (mappedNodes + unmappedNodes1 + unmappedNodes2);

                        if (score > maxScore) {
                            maxScore = score;
                            bestPdg1 = pdg1;
                            bestPdg2 = pdg2;
                            bestNodeMapping = nodeMapping;
                        }
                    }
                }
            }

            if (bestPdg1 != null && bestPdg2 != null) {
                // map the best pdg pair found
                unmappedPDGs1.remove(bestPdg1);
                unmappedPDGs2.remove(bestPdg2);
                graphMapping.addGraphMapping(bestPdg1, bestPdg2, bestNodeMapping);
            } else {
                // no more matches found
                break;
            }
        }

        for (PDG pdg1 : unmappedPDGs1) {
            System.out.println("No matching PDG found for: " + pdg1.getCFG().getBody().getMethod().getSignature());
        }

        return graphMapping;
    }
}
