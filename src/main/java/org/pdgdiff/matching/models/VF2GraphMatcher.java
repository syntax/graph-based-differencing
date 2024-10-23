package org.pdgdiff.matching.models;

import org.pdgdiff.matching.GraphMapping;
import org.pdgdiff.matching.GraphMatcher;
import org.pdgdiff.matching.NodeMapping;
import org.pdgdiff.matching.models.vf2.VF2Matcher;
import soot.toolkits.graph.pdg.HashMutablePDG;

import java.util.List;

public class VF2GraphMatcher extends GraphMatcher {
    public VF2GraphMatcher(List<HashMutablePDG> list1, List<HashMutablePDG> list2) {
        super(list1, list2);
    }

    @Override
    public GraphMapping matchPDGLists() {
        for (HashMutablePDG pdg1 : pdgList1) {
            HashMutablePDG match = null;
            NodeMapping nodeMapping = null;

            // Compare pdg1 with each PDG from the second list
            for (HashMutablePDG pdg2 : pdgList2) {
                // Skip if this PDG has already been matched
                if (matchedPDGs.contains(pdg2)) {
                    continue;
                }

                // Use VF2Matcher
                VF2Matcher vf2Matcher = new VF2Matcher(pdg1, pdg2);
                nodeMapping = vf2Matcher.match();

                // If a mapping is found, consider it as a match
                // TODO: need to try ALL mappings and then select best. time complexity unfortunatley going to go 📈
                if (nodeMapping != null && !nodeMapping.isEmpty()) {
                    match = pdg2;
                    break; // Since we found a match, we can break out of the loop
                }
            }

            if (match != null) {
                matchedPDGs.add(match);
                graphMapping.addGraphMapping(pdg1, match, nodeMapping);
            } else {
                System.out.println("No matching PDG found for: " + pdg1.getCFG().getBody().getMethod().getSignature());
            }
        }

        return graphMapping;  // Return the complete GraphMapping
    }
}
