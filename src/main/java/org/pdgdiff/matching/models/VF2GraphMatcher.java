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
    public VF2GraphMatcher(List<PDG> list1, List<PDG> list2) {
        super(list1, list2);
    }

    @Override
    public GraphMapping matchPDGLists() {
        // TODO: rename these legacy names 'pdglist1' to be more informative i.e. src and dest
        List<PDG> unmappedPDGs1 = new ArrayList<>(pdgList1);
        List<PDG> unmappedPDGs2 = new ArrayList<>(pdgList2);

        while (!unmappedPDGs1.isEmpty() && !unmappedPDGs2.isEmpty()) {
            double maxScore = Double.NEGATIVE_INFINITY;
            PDG bestPdg1 = null;
            PDG bestPdg2 = null;
            NodeMapping bestNodeMapping = null;

            // for each pair of unmapped PDGs, compute similarity score
            for (PDG pdg1 : unmappedPDGs1) {
                for (PDG pdg2 : unmappedPDGs2) {
                    System.out.println("Matching PDG1: " + pdg1.getCFG().getBody().getMethod().getSignature() + ", PDG2: " + pdg2.getCFG().getBody().getMethod().getSignature());
                    VF2Matcher vf2Matcher = new VF2Matcher(pdg1, pdg2);
                    NodeMapping nodeMapping = vf2Matcher.match();
                    System.out.println(nodeMapping);
                    if (nodeMapping != null && !nodeMapping.isEmpty()) {
                        System.out.println(" >> Produced a mapping for " + pdg1.getCFG().getBody().getMethod().getSignature() + " and " + pdg2.getCFG().getBody().getMethod().getSignature());
                        int mappedNodes = nodeMapping.size();
                        int unmappedNodes1 = GraphTraversal.getNodeCount(pdg1) - mappedNodes;
                        int unmappedNodes2 = GraphTraversal.getNodeCount(pdg2) - mappedNodes;

                        // calculate the score that minimizes unmapped nodes, this is my 'similarity' metric as of rn lol
                        // this might be to be improved. TODO look into other metrics/ measures.
                        // TODO might want to add a threshold. possibly not all graphs should be mapped to all graphs!
                        double score = (double) mappedNodes / (mappedNodes + unmappedNodes1 + unmappedNodes2);
                        System.out.println(" >> Score: " + score + " for " + pdg1.getCFG().getBody().getMethod().getSignature() + " and " + pdg2.getCFG().getBody().getMethod().getSignature());
                        if (score > maxScore) {
                            System.out.println("!!!! >> New best score: " + score);
                            maxScore = score;
                            bestPdg1 = pdg1;
                            bestPdg2 = pdg2;
                            bestNodeMapping = nodeMapping;
                        }
                    }
                }
            }

            if (bestPdg1 != null && bestPdg2 != null) {
                unmappedPDGs1.remove(bestPdg1);
                unmappedPDGs2.remove(bestPdg2);
                graphMapping.addGraphMapping(bestPdg1, bestPdg2, bestNodeMapping);
            } else {
                break;
            }
        }

        // handling PDGs in src that were not matched
        for (PDG pdg1 : unmappedPDGs1) {
            System.out.println("No matching PDG found for: " + pdg1.getCFG().getBody().getMethod().getSignature());
        }

        return graphMapping;
    }
}
