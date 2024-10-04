package org.pdgdiff.matching.models.vf2;

import org.pdgdiff.graph.GraphTraversal;
import org.pdgdiff.util.PDGNodeWrapper;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.PDGNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.*;

/*
 * VF2State class to store the state of the VF2 algorithm. This class contains methods to store the current state
 * of the VF2 algorithm and perform operations on the state.
 * TODO improvements to be made here
 */
class VF2State {
    private HashMutablePDG pdg1;
    private HashMutablePDG pdg2;
    private Map<PDGNodeWrapper, PDGNodeWrapper> mapping;  // The current partial mapping

    private Set<PDGNodeWrapper> T1;  // Nodes in PDG1 that are in the mapping or adjacent to mapped nodes
    private Set<PDGNodeWrapper> T2;  // Same for PDG2

    private Set<PDGNodeWrapper> unmapped1;  // Unmapped nodes in PDG1 (src)
    private Set<PDGNodeWrapper> unmapped2;  // Unmapped nodes in PDG2 (dst)

    public VF2State(HashMutablePDG pdg1, HashMutablePDG pdg2) {
        this.pdg1 = pdg1;
        this.pdg2 = pdg2;
        this.mapping = new HashMap<>();

        // Wrapping the PDGNodes in PDGNodeWrapper
        this.unmapped1 = new HashSet<>(wrapPDGNodes(GraphTraversal.collectNodesBFS(pdg1)));
        this.unmapped2 = new HashSet<>(wrapPDGNodes(GraphTraversal.collectNodesBFS(pdg2)));

        this.T1 = new HashSet<>();
        this.T2 = new HashSet<>();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        VF2State other = (VF2State) obj;
        return Objects.equals(mapping, other.mapping) &&
                Objects.equals(T1, other.T1) &&
                Objects.equals(T2, other.T2) &&
                Objects.equals(unmapped1, other.unmapped1) &&
                Objects.equals(unmapped2, other.unmapped2);
    }

    // Override hashCode() to ensure consistency with equals()
    @Override
    public int hashCode() {
        return Objects.hash(mapping, T1, T2, unmapped1, unmapped2);
    }

    // Helper method to wrap PDGNode objects in PDGNodeWrapper
    private Set<PDGNodeWrapper> wrapPDGNodes(List<PDGNode> nodes) {
        Set<PDGNodeWrapper> wrappedNodes = new HashSet<>();
        for (PDGNode node : nodes) {
            wrappedNodes.add(new PDGNodeWrapper(node));  // Wrapping each PDGNode
        }
        return wrappedNodes;
    }

    public boolean isComplete() {
        // I believe this is sufficient, as partial mapping is sufficient
        return unmapped1.isEmpty() || unmapped2.isEmpty();  // Check if all nodes are mapped
    }

    public Map<PDGNodeWrapper, PDGNodeWrapper> getMapping() {
        return mapping;
    }

    public List<CandidatePair> generateCandidates() {
        List<CandidatePair> candidates = new ArrayList<>();

        if (!T1.isEmpty() && !T2.isEmpty()) {
            // Pick nodes from T1 and T2
            for (PDGNodeWrapper n1 : T1) {
                for (PDGNodeWrapper n2 : T2) {
                    if (nodesAreCompatible(n1, n2) && !mapping.containsKey(n1) && !mapping.containsValue(n2)) {
                        candidates.add(new CandidatePair(n1, n2));
                    }
                }
            }
        } else {
            // If T1 and T2 are empty, pick any unmapped nodes
            for (PDGNodeWrapper n1 : unmapped1) {
                for (PDGNodeWrapper n2 : unmapped2) {
                    if (nodesAreCompatible(n1, n2) && !mapping.containsKey(n1) && !mapping.containsValue(n2)) {
                        candidates.add(new CandidatePair(n1, n2));
                    }
                }
            }
        }

        return candidates;
    }

    public boolean isFeasible(CandidatePair pair) {
//        System.out.println("Checking feasibility for pair");
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

    private boolean nodesAreCompatible(PDGNodeWrapper n1, PDGNodeWrapper n2) {
        return n1.getPDGNode().getType() == n2.getPDGNode().getType() &&
                n1.getPDGNode().getAttrib() == n2.getPDGNode().getAttrib() &&
                pdg1.getSuccsOf(n1.getPDGNode()).size() == pdg2.getSuccsOf(n2.getPDGNode()).size() &&
                pdg1.getPredsOf(n1.getPDGNode()).size() == pdg2.getPredsOf(n2.getPDGNode()).size();
    }

    private boolean checkSyntacticFeasibility(CandidatePair pair) {
        return nodesAreCompatible(pair.n1, pair.n2);
    }

    private boolean checkSemanticFeasibility(CandidatePair pair) {
        // TODO: investigate this function as it seems to be non-deterministic and only works sometimes, maybe
        // TODO: sometimes it just takes longer to complete that other times... not quite sure. ugh!
        PDGNodeWrapper n1 = pair.n1;
        PDGNodeWrapper n2 = pair.n2;
        // Get successors and predecessors once (cache these)
        List<PDGNodeWrapper> succs1 = new ArrayList<>(wrapPDGNodes(pdg1.getSuccsOf(n1.getPDGNode())));
        List<PDGNodeWrapper> succs2 = new ArrayList<>(wrapPDGNodes(pdg2.getSuccsOf(n2.getPDGNode())));
        List<PDGNodeWrapper> preds1 = new ArrayList<>(wrapPDGNodes(pdg1.getPredsOf(n1.getPDGNode())));
        List<PDGNodeWrapper> preds2 = new ArrayList<>(wrapPDGNodes(pdg2.getPredsOf(n2.getPDGNode())));

        // Check successors
//        System.out.println("Checking successors");
        for (PDGNodeWrapper m1 : succs1) {
            PDGNodeWrapper m2 = mapping.get(m1);
            if (m2 != null && !succs2.contains(m2)) {
                return false;
            }
        }

        // Check predecessors
//        System.out.println("Checking predecessors");
        for (PDGNodeWrapper m1 : preds1) {
            PDGNodeWrapper m2 = mapping.get(m1);
            if (m2 != null && !preds2.contains(m2)) {
                return false;
            }
        }

        return true;
    }

    private void updateTerminalSets(PDGNodeWrapper n1, PDGNodeWrapper n2) {
        // Add neighbors of n1 to T1 if they are not mapped
        for (PDGNode neighbor : n1.getPDGNode().getDependents()) {
            PDGNodeWrapper wrappedNeighbor = new PDGNodeWrapper(neighbor);
            if (!mapping.containsKey(wrappedNeighbor)) {
                T1.add(wrappedNeighbor);
            }
        }
        // Same for n2
        for (PDGNode neighbor : n2.getPDGNode().getDependents()) {
            PDGNodeWrapper wrappedNeighbor = new PDGNodeWrapper(neighbor);
            if (!mapping.containsValue(wrappedNeighbor)) {
                T2.add(wrappedNeighbor);
            }
        }
        // Remove n1 and n2 from T1 and T2
        T1.remove(n1);
        T2.remove(n2);
    }

    private void recalculateTerminalSets() {
        T1.clear();
        T2.clear();
        for (PDGNodeWrapper mappedNode1 : mapping.keySet()) {
            for (PDGNode neighbor : mappedNode1.getPDGNode().getDependents()) {
                PDGNodeWrapper wrappedNeighbor = new PDGNodeWrapper(neighbor);
                if (!mapping.containsKey(wrappedNeighbor)) {
                    T1.add(wrappedNeighbor);
                }
            }
        }
        for (PDGNodeWrapper mappedNode2 : mapping.values()) {
            for (PDGNode neighbor : mappedNode2.getPDGNode().getDependents()) {
                PDGNodeWrapper wrappedNeighbor = new PDGNodeWrapper(neighbor);
                if (!mapping.containsValue(wrappedNeighbor)) {
                    T2.add(wrappedNeighbor);
                }
            }
        }
    }
}
