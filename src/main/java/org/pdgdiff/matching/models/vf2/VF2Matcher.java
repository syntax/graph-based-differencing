package org.pdgdiff.matching.models.vf2;

import org.pdgdiff.matching.NodeMapping;
import org.pdgdiff.util.PDGNodeWrapper;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.PDGNode;

import java.util.Map;


/**
 * VF2Matcher class to perform graph matching using the VF2 algorithm. This class contains methods to match two PDGs
 * using the VF2 algorithm and return the node mappings between the two PDGs.
 * TODO: Check I have done this correctly, quite tired at time of implementation (its 11pm :( )
 */
public class VF2Matcher {
    private HashMutablePDG pdg1;
    private HashMutablePDG pdg2;
    private NodeMapping nodeMapping;

    public VF2Matcher(HashMutablePDG pdg1, HashMutablePDG pdg2) {
        this.pdg1 = pdg1;
        this.pdg2 = pdg2;
        this.nodeMapping = new NodeMapping();
    }

    public NodeMapping match() {
        // Initialize state
        VF2State state = new VF2State(pdg1, pdg2);
        // Start recursive matching
        if (matchRecursive(state)) {
            return nodeMapping;
        } else {
            // No isomorphism found
            return null;
        }
    }

    private boolean matchRecursive(VF2State state) {
        if (state.isComplete()) {
            // Mapping is complete, transfer mappings to nodeMapping
            // Unwrap the PDGNodeWrapper and map the original PDGNode
            for (Map.Entry<PDGNodeWrapper, PDGNodeWrapper> entry : state.getMapping().entrySet()) {
                PDGNode originalNode1 = entry.getKey().getPDGNode(); // Unwrap PDGNodeWrapper
                PDGNode originalNode2 = entry.getValue().getPDGNode(); // Unwrap PDGNodeWrapper
                nodeMapping.addMapping(originalNode1, originalNode2); // Add to NodeMapping
            }
            return true;
        }

        // Generate candidate pairs
        for (CandidatePair pair : state.generateCandidates()) {
            if (state.isFeasible(pair)) {
                state.addPair(pair);
                if (matchRecursive(state)) {
                    return true;
                }
                state.removePair(pair);
            }
        }
        return false;
    }
}