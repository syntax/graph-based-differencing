package org.pdgdiff.matching.models;

import org.pdgdiff.matching.NodeMapping;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.PDGNode;

import java.util.HashMap;
import java.util.Map;

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
            for (Map.Entry<PDGNode, PDGNode> entry : state.getMapping().entrySet()) {
                nodeMapping.addMapping(entry.getKey(), entry.getValue());
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
