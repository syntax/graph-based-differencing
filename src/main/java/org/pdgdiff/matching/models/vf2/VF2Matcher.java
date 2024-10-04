package org.pdgdiff.matching.models.vf2;

import org.pdgdiff.matching.NodeMapping;
import org.pdgdiff.util.PDGNodeWrapper;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.PDGNode;

import java.util.*;

/**
 * VF2Matcher class to perform graph matching using the VF2 algorithm. This class contains methods to match two PDGs
 * using the VF2 algorithm and return the node mappings between the two PDGs.
 * This version includes optimizations to avoid redundant computations and introduce state tracking.
 */
public class VF2Matcher {
    private static final int MAX_RECURSION_DEPTH = 100;  // Set max recursion depth
    private HashMutablePDG pdg1;
    private HashMutablePDG pdg2;
    private NodeMapping nodeMapping;
    private Set<VF2State> visitedStates;  // Track visited states to avoid redundant computations

    public VF2Matcher(HashMutablePDG pdg1, HashMutablePDG pdg2) {
        this.pdg1 = pdg1;
        this.pdg2 = pdg2;
        this.nodeMapping = new NodeMapping();
        this.visitedStates = new HashSet<>();  // Initialize visited states tracking
    }

    public NodeMapping match() {
        // Initialize state
        VF2State state = new VF2State(pdg1, pdg2);
        // Start recursive matching with depth 0
        if (matchRecursive(state, 0)) {
            return nodeMapping;
        } else {
            // No isomorphism found
            return null;
        }
    }

    private boolean matchRecursive(VF2State state, int depth) {
        // Check if we've exceeded the max recursion depth
        if (depth > MAX_RECURSION_DEPTH) {
            System.out.println("Max recursion depth reached: " + depth);
            return false;
        }

        // Avoid revisiting the same state
        if (state.isComplete()) {
            // Mapping is complete, return success
            System.out.println("Mapping complete, returning...");
            for (Map.Entry<PDGNodeWrapper, PDGNodeWrapper> entry : state.getMapping().entrySet()) {
                PDGNode originalNode1 = entry.getKey().getPDGNode();
                PDGNode originalNode2 = entry.getValue().getPDGNode();
                nodeMapping.addMapping(originalNode1, originalNode2);
            }
            return true;
        }

        if (visitedStates.contains(state)) {
            System.out.println("State already visited, skipping...");
            return false;
        }

        // Mark the state as visited
        visitedStates.add(state);

        // Generate candidate pairs
        for (CandidatePair pair : state.generateCandidates()) {
            if (state.isFeasible(pair)) {
                state.addPair(pair);
                if (matchRecursive(state, depth + 1)) {
                    return true;
                }
                state.removePair(pair);
            }
        }
        return false;
    }
}
