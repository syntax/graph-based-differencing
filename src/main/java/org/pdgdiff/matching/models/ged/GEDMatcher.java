package org.pdgdiff.matching.models.ged;

import org.pdgdiff.matching.NodeFeasibility;
import org.pdgdiff.graph.GraphTraversal;
import org.pdgdiff.graph.PDG;
import org.pdgdiff.matching.NodeMapping;
import soot.toolkits.graph.pdg.PDGNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Performs a Graph Edit Distance node alignment between two PDGs.
 * Returns a GEDResult containing the (distance, nodeMapping).
 *
 */
public class GEDMatcher {

    private PDG pdg1;
    private PDG pdg2;

    public GEDMatcher(PDG pdg1, PDG pdg2) {
        this.pdg1 = pdg1;
        this.pdg2 = pdg2;
    }

    // find edit distance and return node mappings
    public GEDResult match() {
        List<PDGNode> nodes1 = new ArrayList<>(GraphTraversal.collectNodesBFS(pdg1));
        List<PDGNode> nodes2 = new ArrayList<>(GraphTraversal.collectNodesBFS(pdg2));

        int n1 = nodes1.size();
        int n2 = nodes2.size();

        // create square cost mat of n x n size, must be square for Hungarian algo
        // NOTE because its square there is going to be some dummy nodes (where its padded, pdg prob doesnt produce square mat)
        int n = Math.max(n1, n2);
        double[][] squareMatrix = new double[n][n];

        // deletion and insertion costs, todo tune these
        double insertionCostVal = 1.0;
        double deletionCostVal  = 1.0;

        // fill the "real" submatrix of the cost matrix (where i < n1 and j < n2) with substitution costs for each node pair
        for (int i = 0; i < n1; i++) {
            for (int j = 0; j < n2; j++) {
                // todo: subs cost needs tweaks to better this algo
                squareMatrix[i][j] = substitutionCost(nodes1.get(i), nodes2.get(j));
            }
        }

        // fill extra non-match rows/columns with insertion/deletion costs
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {

                // if within real submatrix, i.e. no dummies,  just covered it in loop above
                if (i < n1 && j < n2) {
                    continue;
                }

                // i < n1 but j >= n2 => "dummy" node in PDG2 => old node i must be deleted
                if (i < n1 && j >= n2) {
                    squareMatrix[i][j] = deletionCostVal;
                }
                // i >= n1 but j < n2 => "dummy" node in PDG1 => new node j must be inserted
                else if (i >= n1 && j < n2) {
                    squareMatrix[i][j] = insertionCostVal;
                }
                // i >= n1 && j >= n2 => both dummy => set cost = 0 or could change this to some small cost
                else if (i >= n1 && j >= n2) {
                    squareMatrix[i][j] = 0.0;
                }
            }
        }

        // solving the assignment on the n x n square matrix
        int[] assignment = HungarianAlgorithm.minimizeAssignment(squareMatrix);

        NodeMapping nodeMapping = new NodeMapping();
        double totalCost = 0.0;


        // checking for real vs dummy nodes
        boolean[] usedCols = new boolean[n];
        for (int i = 0; i < n; i++) {
            // 'assignment[i] = j' means row i is matched to column j. each i, j  in [0..n)
            int j = assignment[i];
            if (j < 0) {
                continue;
            }
            double cost = squareMatrix[i][j];
            totalCost += cost;
            usedCols[j] = true;

            // If i < n1 and j < n2 its within range of 'real' submat=> real node match => substitution
            if (i < n1 && j < n2) {
                nodeMapping.addMapping(nodes1.get(i), nodes2.get(j));
            }
            // todo: not really sure how to handle this lol, but should complete. current nodemapping doesnt
            // unmatched nodes in Nodemapping will be handled as insertions/ deletions where approriate.
            // if i < n1 && j >= n2 => deletion (old node i matched to dummy)
            // if i >= n1 && j < n2 => insertion (new node j matched to dummy)
            // if both dummy => ignore
        }

        // now we have a raw "nodeMapping" for the real-> real pairs.
        // todo optionally penalize mismatch in edges, could get rid of this but its more strict to graph
        double edgePenalty = computeEdgeMismatchPenalty(nodeMapping);
        totalCost += edgePenalty;

        return new GEDResult(totalCost, nodeMapping);
    }

    /**
     * returns substitution cost between two nodes. tbc and changd:
     * e.g. compare text, type, adjacency structure, etc.
     */
    // SEMANTIC FEASIBILITY ETC
    private double substitutionCost(PDGNode n1, PDGNode n2) {
        // if attributes match, cost is small. Otherwise, cost is bigger
        // todo add ot this. dont think attrib ever really varies so much bar loop headers vs stmts
        if (n1.getAttrib().equals(n2.getAttrib())) {
            return 0.2;  // slight difference
        } else if (NodeFeasibility.isSameNodeCategory(n1, n2)) {
            return 0.5;  // moderate mismatch
        } else {
            return 1.0;  // bigger mismatch
        }
    }

    /**
     * check for edges and inforce mismatch penalty
     */
    private double computeEdgeMismatchPenalty(NodeMapping mapping) {
        double mismatchCost = 0.0;
        double edgePenalty = 0.5;  // this is complete guess work.

        Map<PDGNode, PDGNode> forwardMap = mapping.getNodeMapping();

        // for each mapped edge (n1->m1) in old, see if (n2->m2) exists in new
        for (PDGNode oldSrc : forwardMap.keySet()) {
            PDGNode newSrc = forwardMap.get(oldSrc);

            for (PDGNode oldTgt : oldSrc.getDependents()) {
                PDGNode newTgt = forwardMap.get(oldTgt);
                if (newTgt != null) {
                    // if the new edge does not exist, penalize
                    if (!newSrc.getDependents().contains(newTgt)) {
                        mismatchCost += edgePenalty;
                    }
                }
            }
        }
        // todo possibly add checks for edges that are in new pdg, but not in old pdg (vice versa)
        // can use mappings.getReverseNodeMapping()

        return mismatchCost;
    }
}
