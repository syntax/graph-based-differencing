package org.pdgdiff.matching.models;

import org.pdgdiff.util.GraphTraversal;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.PDGNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


import java.util.*;


/*
 * VF2State class to store the state of the VF2 algorithm. This class contains methods to store the current state
 * of the VF2 algorithm and perform operations on the state.
 * TODO: STILL IN THE WORKS
 */
class VF2State {
    private HashMutablePDG pdg1;
    private HashMutablePDG pdg2;
    private Map<PDGNode, PDGNode> mapping;  // The current partial mapping

    private Set<PDGNode> T1;  // Nodes in PDG1 that are in the mapping or adjacent to mapped nodes
    private Set<PDGNode> T2;  // Same for PDG2

    private Set<PDGNode> unmapped1;  // Unmapped nodes in PDG1
    private Set<PDGNode> unmapped2;  // Unmapped nodes in PDG2

    public VF2State(HashMutablePDG pdg1, HashMutablePDG pdg2) {
        this.pdg1 = pdg1;
        this.pdg2 = pdg2;
        this.mapping = new HashMap<>();

        this.unmapped1 = new HashSet<>(GraphTraversal.collectNodesBFS(pdg1));
        this.unmapped2 = new HashSet<>(GraphTraversal.collectNodesBFS(pdg2));

        this.T1 = new HashSet<>();
        this.T2 = new HashSet<>();
    }

    public boolean isComplete() {
        return mapping.size() == GraphTraversal.getNodeCount(pdg1);
    }

    public Map<PDGNode, PDGNode> getMapping() {
        return mapping;
    }

    public List<CandidatePair> generateCandidates() {
        List<CandidatePair> candidates = new ArrayList<>();

        if (!T1.isEmpty() && !T2.isEmpty()) {
            // Pick nodes from T1 and T2
            for (PDGNode n1 : T1) {
                for (PDGNode n2 : T2) {
                    if (nodesAreCompatible(n1, n2)) {
                        candidates.add(new CandidatePair(n1, n2));
                    }
                }
            }
        } else {
            // If T1 and T2 are empty, pick any unmapped nodes
            for (PDGNode n1 : unmapped1) {
                for (PDGNode n2 : unmapped2) {
                    if (nodesAreCompatible(n1, n2)) {
                        candidates.add(new CandidatePair(n1, n2));
                    }
                }
            }
        }

        return candidates;
    }

    public boolean isFeasible(CandidatePair pair) {
        // Implement feasibility checks:
        // - Syntactic feasibility: node attributes match
        // - Semantic feasibility: the mapping is consistent with the graph structure
        return checkSyntacticFeasibility(pair) && checkSemanticFeasibility(pair);
    }

    public void addPair(CandidatePair pair) {
        mapping.put(pair.n1, pair.n2);
        unmapped1.remove(pair.n1);
        unmapped2.remove(pair.n2);

        // Update T1 and T2
        updateTerminalSets(pair.n1, pair.n2);
    }

    public void removePair(CandidatePair pair) {
        mapping.remove(pair.n1);
        unmapped1.add(pair.n1);
        unmapped2.add(pair.n2);

        // Recalculate T1 and T2
        recalculateTerminalSets();
    }

    // Helper methods...

    private boolean nodesAreCompatible(PDGNode n1, PDGNode n2) {
        // Compare node types, labels, attributes
        return n1.getType() == n2.getType() && n1.getAttrib() == n2.getAttrib();
        // TODO: Add more detailed comparison/ metrics
    }

    private boolean checkSyntacticFeasibility(CandidatePair pair) {
        // Ensure that the nodes can be mapped based on their attributes
        return nodesAreCompatible(pair.n1, pair.n2);
    }

    private boolean checkSemanticFeasibility(CandidatePair pair) {
        // Ensure that the mapping preserves the graph structure
        // For each neighbor of n1, check that the corresponding neighbor in n2 exists
        // and is consistent with the current mapping
        // TODO: fix
        return true;  // Simplified for now
    }

    private void updateTerminalSets(PDGNode n1, PDGNode n2) {
        // Add neighbors of n1 to T1 if they are not mapped
        for (PDGNode neighbor : n1.getDependents()) {
            if (!mapping.containsKey(neighbor)) {
                T1.add(neighbor);
            }
        }
        // Same for n2
        for (PDGNode neighbor : n2.getDependents()) {
            if (!mapping.containsValue(neighbor)) {
                T2.add(neighbor);
            }
        }
        // Remove n1 and n2 from T1 and T2
        T1.remove(n1);
        T2.remove(n2);
    }

    private void recalculateTerminalSets() {
        T1.clear();
        T2.clear();
        for (PDGNode mappedNode1 : mapping.keySet()) {
            for (PDGNode neighbor : mappedNode1.getDependents()) {
                if (!mapping.containsKey(neighbor)) {
                    T1.add(neighbor);
                }
            }
        }
        for (PDGNode mappedNode2 : mapping.values()) {
            for (PDGNode neighbor : mappedNode2.getDependents()) {
                if (!mapping.containsValue(neighbor)) {
                    T2.add(neighbor);
                }
            }
        }
    }
}