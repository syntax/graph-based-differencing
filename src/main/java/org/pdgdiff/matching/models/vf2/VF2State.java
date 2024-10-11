package org.pdgdiff.matching.models.vf2;

import org.pdgdiff.graph.GraphTraversal;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.PDGNode;

import java.util.*;

/**
 * VF2State class to store the state of the VF2 algorithm. This class contains methods to store the current state
 * of the VF2 algorithm and perform operations on the state.
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
        this.mapping = new LinkedHashMap<>();

        this.unmapped1 = new LinkedHashSet<>(GraphTraversal.collectNodesBFS(pdg1));
        this.unmapped2 = new LinkedHashSet<>(GraphTraversal.collectNodesBFS(pdg2));

        this.T1 = new LinkedHashSet<>();
        this.T2 = new LinkedHashSet<>();
    }

    public boolean isComplete() {
        // once one of the graphs is fully matched (hence this is subgraph isomorphism)
        return mapping.size() >= Math.min(GraphTraversal.getNodeCount(pdg1), GraphTraversal.getNodeCount(pdg2));
    }

    public Map<PDGNode, PDGNode> getMapping() {
        return mapping;
    }

    public List<CandidatePair> generateCandidates() {
        // TODO: If non determinism prevails, consider implementing a sort on these candidates
        // TODO: probably need to sort by id e.g. CFGNODE 1 sorta thing. should hopefully work,
        // If not implementing this here, possibly need to implement it in the matchRecursvie function.
        List<CandidatePair> candidates = new ArrayList<>();

        if (!T1.isEmpty() && !T2.isEmpty()) {
            // Pick nodes from T1 and T2
            PDGNode n1 = selectNode(T1);
            for (PDGNode n2 : T2) {
                if (nodesAreCompatible(n1, n2)) {
                    candidates.add(new CandidatePair(n1, n2));
                }
            }
        } else {
            // If T1 and T2 are empty, pick any unmapped nodes
            PDGNode n1 = selectNode(unmapped1);
            for (PDGNode n2 : unmapped2) {
                if (nodesAreCompatible(n1, n2)) {
                    candidates.add(new CandidatePair(n1, n2));
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
        return n1.getType().equals(n2.getType()) && n1.getAttrib().equals(n2.getAttrib());
    }

    private boolean checkSyntacticFeasibility(CandidatePair pair) {
        // Ensure that the nodes can be mapped based on their attributes
        return nodesAreCompatible(pair.n1, pair.n2);
    }

    private boolean checkSemanticFeasibility(CandidatePair pair) {
        // TODO: investigate this sometimes hanging...
        // Ensure that the mapping preserves the graph structure
        // Check consistency of predecessors and successors

        // For all edges (n1', n1) in pdg1
        // NB getBackDependets typo exists in original soot code
        for (PDGNode n1Prime : pair.n1.getBackDependets()) {
            PDGNode mappedN1Prime = mapping.get(n1Prime);
            if (mappedN1Prime != null) {
                // There should be an edge (mappedN1Prime, n2) in pdg2
                if (!mappedN1Prime.getDependents().contains(pair.n2)) {
                    return false;
                }
            }
        }

        // For all edges (n1, n1'') in pdg1
        for (PDGNode n1DoublePrime : pair.n1.getDependents()) {
            PDGNode mappedN1DoublePrime = mapping.get(n1DoublePrime);
            if (mappedN1DoublePrime != null) {
                // There should be an edge (n2, mappedN1DoublePrime) in pdg2
                if (!pair.n2.getDependents().contains(mappedN1DoublePrime)) {
                    return false;
                }
            }
        }

        // Ensure no conflicting mappings exist
        for (Map.Entry<PDGNode, PDGNode> entry : mapping.entrySet()) {
            PDGNode mappedN1 = entry.getKey();
            PDGNode mappedN2 = entry.getValue();

            // Check if there is an edge between pair.n1 and mappedN1 in pdg1
            boolean edgeInPDG1 = pair.n1.getDependents().contains(mappedN1) || pair.n1.getBackDependets().contains(mappedN1);
            boolean edgeInPDG2 = pair.n2.getDependents().contains(mappedN2) || pair.n2.getBackDependets().contains(mappedN2);

            if (edgeInPDG1 != edgeInPDG2) {
                return false;
            }
        }

        return true;
    }

    private void updateTerminalSets(PDGNode n1, PDGNode n2) {
        // Add neighbors of n1 to T1 if they are not mapped
        for (PDGNode neighbor : n1.getDependents()) {
            if (!mapping.containsKey(neighbor)) {
                T1.add(neighbor);
            }
        }
        for (PDGNode neighbor : n1.getBackDependets()) {
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
        for (PDGNode neighbor : n2.getBackDependets()) {
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
            for (PDGNode neighbor : mappedNode1.getBackDependets()) {
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
            for (PDGNode neighbor : mappedNode2.getBackDependets()) {
                if (!mapping.containsValue(neighbor)) {
                    T2.add(neighbor);
                }
            }
        }
    }

    private PDGNode selectNode(Set<PDGNode> nodeSet) {
        // TODO: implement a more sophisticated node selection strategy here
        // ATM return any node from the set
        return nodeSet.iterator().next();
    }
}
