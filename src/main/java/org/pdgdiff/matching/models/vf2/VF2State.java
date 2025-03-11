package org.pdgdiff.matching.models.vf2;

import org.pdgdiff.matching.NodeFeasibility;
import org.pdgdiff.graph.GraphTraversal;
import org.pdgdiff.graph.PDG;
import soot.toolkits.graph.pdg.PDGNode;

import java.util.*;

/**
 * VF2State class to store the state of the VF2 algorithm. This class contains methods to store the current state
 * of the VF2 algorithm and perform operations on the state.
 */
class VF2State {
    private final PDG srcPdg;
    private final PDG dstPdg;
    private final Map<PDGNode, PDGNode> mapping;  // The current partial mapping

    private final Set<PDGNode> T1;  // Nodes in PDG1 that are in the mapping or adjacent to mapped nodes
    private final Set<PDGNode> T2;  // Same for PDG2

    private final Set<PDGNode> unmappedSrcNodes;  // Unmapped nodes in PDG1 (the source pdg)
    private final Set<PDGNode> unmappedDstNodes;  // Unmapped nodes in PDG2 (the dest pdg)

    public VF2State(PDG srcPdg, PDG dstPdg) {
        this.srcPdg = srcPdg;
        this.dstPdg = dstPdg;
        this.mapping = new LinkedHashMap<>();

        this.unmappedSrcNodes = new LinkedHashSet<>(GraphTraversal.collectNodesBFS(srcPdg));
        this.unmappedDstNodes = new LinkedHashSet<>(GraphTraversal.collectNodesBFS(dstPdg));

        this.T1 = new LinkedHashSet<>();
        this.T2 = new LinkedHashSet<>();
    }

    public boolean isComplete() {
        // once one of the graphs is fully matched (hence this is subgraph isomorphism)
//TODO: consider allowing this:
//        return mapping.size() >= Math.min(GraphTraversal.getNodeCount(srcPdg) * 0.5 , GraphTraversal.getNodeCount(dstPdg) * 0.5);
        return mapping.size() >= Math.min(GraphTraversal.getNodeCount(srcPdg), GraphTraversal.getNodeCount(dstPdg));
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
            PDGNode n1 = selectNode(unmappedSrcNodes);
            for (PDGNode n2 : unmappedDstNodes) {
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
        // TODO there is no point in doing checkSyntacticFeasibility here, as this is already tested when generating the candidates.
        // TODO FIX.
        return checkSyntacticFeasibility(pair) && checkSemanticFeasibility(pair);
    }

    public void addPair(CandidatePair pair) {
        mapping.put(pair.n1, pair.n2);
        unmappedSrcNodes.remove(pair.n1);
        unmappedDstNodes.remove(pair.n2);

        // Update T1 and T2
        updateTerminalSets(pair.n1, pair.n2);
    }

    public void removePair(CandidatePair pair) {
        mapping.remove(pair.n1);
        unmappedSrcNodes.add(pair.n1);
        unmappedDstNodes.add(pair.n2);

        // Recalculate T1 and T2
        recalculateTerminalSets();
    }

    // Helper methods...

    private boolean nodesAreCompatible(PDGNode n1, PDGNode n2) {
        // check if the nodes are of the same semantic category (Stmt, Decl, etc.), todo should move this into semantic check section.
        if (!NodeFeasibility.isSameNodeCategory(n1, n2)) {
            return false;
        }
        // checks from teh following attributes; NORMAL, ENTRY, CONDHEADER, LOOPHEADER
        if (!n1.getAttrib().equals(n2.getAttrib())) {
            return false;
        }

        return true;
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
//        for (PDGNode n1Prime : pair.n1.getBackDependets()) {
//            PDGNode mappedN1Prime = mapping.get(n1Prime);
//            if (mappedN1Prime != null) {
//                // There should be an edge (mappedN1Prime, n2) in pdg2
//                if (!mappedN1Prime.getDependents().contains(pair.n2)) {
//                    return false;
//                }
//            }
//        }
//
//        // For all edges (n1, n1'') in pdg1
//        for (PDGNode n1DoublePrime : pair.n1.getDependents()) {
//            PDGNode mappedN1DoublePrime = mapping.get(n1DoublePrime);
//            if (mappedN1DoublePrime != null) {
//                // There should be an edge (n2, mappedN1DoublePrime) in pdg2
//                if (!pair.n2.getDependents().contains(mappedN1DoublePrime)) {
//                    return false;
//                }
//            }
//        }
//
//        // Ensure no conflicting mappings exist
//        for (Map.Entry<PDGNode, PDGNode> entry : mapping.entrySet()) {
//            PDGNode mappedN1 = entry.getKey();
//            PDGNode mappedN2 = entry.getValue();
//
//            // Check if there is an edge between pair.n1 and mappedN1 in pdg1
//            boolean edgeInPDG1 = pair.n1.getDependents().contains(mappedN1) || pair.n1.getBackDependets().contains(mappedN1);
//            boolean edgeInPDG2 = pair.n2.getDependents().contains(mappedN2) || pair.n2.getBackDependets().contains(mappedN2);
//
//            if (edgeInPDG1 != edgeInPDG2) {
//                return false;
//            }
//        }
//
//        return true;

        // cmp successors in PDG1 vs mapped successors in PDG2
        for (PDGNode succInSrcPdg : srcPdg.getSuccsOf(pair.n1)) {
            PDGNode succMappedInDstPdg = this.getMapping().get(succInSrcPdg);
            if (succMappedInDstPdg != null) {
                boolean dataEdge1 = srcPdg.hasDataEdge(pair.n1, succInSrcPdg);
                boolean dataEdge2 = dstPdg.hasDataEdge(pair.n2, succMappedInDstPdg);
                if (dataEdge1 != dataEdge2) {
                    return false;
                }

                boolean ctrlEdge1 = srcPdg.hasControlEdge(pair.n1, succInSrcPdg);
                boolean ctrlEdge2 = dstPdg.hasControlEdge(pair.n2, succMappedInDstPdg);
                if (ctrlEdge1 != ctrlEdge2) {
                    return false;
                }
            }
        }

        // cmp predecessors in PDG1 vs. mapped predecessors in PDG2
        for (PDGNode predInSrcPdg : srcPdg.getPredsOf(pair.n1)) {
            PDGNode predMappedInDstPdg = this.getMapping().get(predInSrcPdg);
            if (predMappedInDstPdg != null) {
                boolean dataEdge1 = srcPdg.hasDataEdge(predInSrcPdg, pair.n1);
                boolean dataEdge2 = dstPdg.hasDataEdge(predMappedInDstPdg, pair.n2);
                if (dataEdge1 != dataEdge2) {
                    return false;
                }

                boolean ctrlEdge1 = srcPdg.hasControlEdge(predInSrcPdg, pair.n1);
                boolean ctrlEdge2 = dstPdg.hasControlEdge(predMappedInDstPdg, pair.n2);
                if (ctrlEdge1 != ctrlEdge2) {
                    return false;
                }
            }
        }

        // cross-check every existing mapping pair so that edges from (pair.n1->mappedN1) in PDG1 match edges from (pair.n2->mappedN2) in PDG2.
        for (Map.Entry<PDGNode, PDGNode> entry : this.getMapping().entrySet()) {
            PDGNode alreadyMappedN1 = entry.getKey();
            PDGNode alreadyMappedN2 = entry.getValue();

            // Forward edges:
            // if PDG1 has data/control edge from (pair.n1 -> alreadyMappedN1), then PDG2 must have the same edge type from (pair.n2 -> alreadyMappedN2)
            boolean dataEdge1 = srcPdg.hasDataEdge(pair.n1, alreadyMappedN1);
            boolean dataEdge2 = dstPdg.hasDataEdge(pair.n2, alreadyMappedN2);
            if (dataEdge1 != dataEdge2) {
                return false;
            }
            boolean ctrlEdge1 = srcPdg.hasControlEdge(pair.n1, alreadyMappedN1);
            boolean ctrlEdge2 = dstPdg.hasControlEdge(pair.n2, alreadyMappedN2);
            if (ctrlEdge1 != ctrlEdge2) {
                return false;
            }

            // Reverse edges:
            // if PDG1 has data/control edge from (alreadyMappedN1 -> pair.n1), then PDG2 must have the same edge type from (alreadyMappedN2 -> pair.n2).
            dataEdge1 = srcPdg.hasDataEdge(alreadyMappedN1, pair.n1);
            dataEdge2 = dstPdg.hasDataEdge(alreadyMappedN2, pair.n2);
            if (dataEdge1 != dataEdge2) {
                return false;
            }
            ctrlEdge1 = srcPdg.hasControlEdge(alreadyMappedN1, pair.n1);
            ctrlEdge2 = dstPdg.hasControlEdge(alreadyMappedN2, pair.n2);
            if (ctrlEdge1 != ctrlEdge2) {
                return false;
            }
        }

        return true;

    }

    private void updateTerminalSets(PDGNode n1, PDGNode n2) {
        // Add neighbours of n1 to T1 if they are not mapped
        for (PDGNode neighbour : n1.getDependents()) {
            if (!mapping.containsKey(neighbour)) {
                T1.add(neighbour);
            }
        }
        for (PDGNode neighbour : n1.getBackDependets()) {
            if (!mapping.containsKey(neighbour)) {
                T1.add(neighbour);
            }
        }

        // Same for n2
        for (PDGNode neighbour : n2.getDependents()) {
            if (!mapping.containsValue(neighbour)) {
                T2.add(neighbour);
            }
        }
        for (PDGNode neighbour : n2.getBackDependets()) {
            if (!mapping.containsValue(neighbour)) {
                T2.add(neighbour);
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
            for (PDGNode neighbour : mappedNode1.getDependents()) {
                if (!mapping.containsKey(neighbour)) {
                    T1.add(neighbour);
                }
            }
            for (PDGNode neighbour : mappedNode1.getBackDependets()) {
                if (!mapping.containsKey(neighbour)) {
                    T1.add(neighbour);
                }
            }
        }
        for (PDGNode mappedNode2 : mapping.values()) {
            for (PDGNode neighbour : mappedNode2.getDependents()) {
                if (!mapping.containsValue(neighbour)) {
                    T2.add(neighbour);
                }
            }
            for (PDGNode neighbour : mappedNode2.getBackDependets()) {
                if (!mapping.containsValue(neighbour)) {
                    T2.add(neighbour);
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
