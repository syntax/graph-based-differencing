package org.pdgdiff.matching.models;

import org.pdgdiff.graph.GraphTraversal;
import org.pdgdiff.graph.model.MyPDG;
import org.pdgdiff.matching.GraphMapping;
import org.pdgdiff.matching.GraphMatcher;
import org.pdgdiff.matching.NodeMapping;
import org.pdgdiff.matching.models.vf2.VF2Matcher;

import java.util.ArrayList;
import java.util.List;

public class VF2GraphMatcher extends GraphMatcher {
    public VF2GraphMatcher(List<MyPDG> list1, List<MyPDG> list2) {
        // TODO: rename these legacy names 'pdglist1' to be more informative i.e. src and dest
        super(list1, list2);
    }

    @Override
    public GraphMapping matchPDGLists() {

        List<MyPDG> unmappedPDGs1 = new ArrayList<>(pdgList1);
        List<MyPDG> unmappedPDGs2 = new ArrayList<>(pdgList2);

        while (!unmappedPDGs1.isEmpty() && !unmappedPDGs2.isEmpty()) {
            double maxScore = Double.NEGATIVE_INFINITY;
            MyPDG bestPdg1 = null;
            MyPDG bestPdg2 = null;
            NodeMapping bestNodeMapping = null;

            // for each pair of unmapped PDGs, compute similarity score
            for (MyPDG pdg1 : unmappedPDGs1) {
                for (MyPDG pdg2 : unmappedPDGs2) {
                    VF2Matcher vf2Matcher = new VF2Matcher(pdg1, pdg2);
                    NodeMapping nodeMapping = vf2Matcher.match();

                    if (nodeMapping != null && !nodeMapping.isEmpty()) {
                        int mappedNodes = nodeMapping.size();
                        int unmappedNodes1 = GraphTraversal.getNodeCount(pdg1) - mappedNodes;
                        int unmappedNodes2 = GraphTraversal.getNodeCount(pdg2) - mappedNodes;

                        // calculate the score that minimizes unmapped nodes, this is my 'similarity' metric as of rn lol
                        // this might be to be improved. TODO look into other metrics/ measures.
                        // TODO might want to add a threshold. possibly not all graphs should be mapped to all graphs!
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
                unmappedPDGs1.remove(bestPdg1);
                unmappedPDGs2.remove(bestPdg2);
                graphMapping.addGraphMapping(bestPdg1, bestPdg2, bestNodeMapping);
            } else {
                break;
            }
        }

        // handling PDGs in src that were not matched
        for (MyPDG pdg1 : unmappedPDGs1) {
            System.out.println("No matching PDG found for PDG: " + pdg1);
        }

        return graphMapping;
    }
}
