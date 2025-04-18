package org.pdgdiff.matching.models.ged;

import org.pdgdiff.matching.NodeFeasibility;
import org.pdgdiff.graph.GraphTraversal;
import org.pdgdiff.graph.PDG;
import org.pdgdiff.matching.NodeMapping;
import soot.toolkits.graph.pdg.PDGNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.pdgdiff.matching.models.heuristic.JaroWinklerSimilarity.JaroWinklerSimilarity;

/**
 * Performs a Graph Edit Distance node alignment between two PDGs.
 * Returns a GEDResult containing the (distance, nodeMapping).
 *
 */
public class GEDMatcher {

    private final PDG srcPdg;
    private final PDG dstPdg;

    public GEDMatcher(PDG srcPdg, PDG dstPdg) {
        this.srcPdg = srcPdg;
        this.dstPdg = dstPdg;
    }

    // find edit distance and return node mappings
    public GEDResult match() {
        List<PDGNode> srcNodes = new ArrayList<>(GraphTraversal.collectNodesBFS(srcPdg));
        List<PDGNode> dstNodes = new ArrayList<>(GraphTraversal.collectNodesBFS(dstPdg));

        int n1 = srcNodes.size();
        int n2 = dstNodes.size();

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
                squareMatrix[i][j] = substitutionCost(srcNodes.get(i), dstNodes.get(j));
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
        for (int i = 0; i < n; i++) {
            // 'assignment[i] = j' means row i is matched to column j. each i, j  in [0..n)
            int j = assignment[i];
            if (j < 0) {
                continue;
            }
            double cost = squareMatrix[i][j];
            totalCost += cost;

            // If i < n1 and j < n2 its within range of 'real' submat=> real node match => substitution
            if (i < n1 && j < n2) {
                nodeMapping.addMapping(srcNodes.get(i), dstNodes.get(j));
            }
            // todo: I believe inserts and deletes shouldn't be added to node mapping,
            //  and that their absence will be handled as insertions/deletions in the final mapping
            // unmatched nodes in Nodemapping will be handled as insertions/ deletions
            // if i < n1 && j >= n2 => deletion (old node i matched to dummy)
            // if i >= n1 && j < n2 => insertion (new node j matched to dummy)
            // if both dummy => ignore
        }

        // penalise matched nodes that have considerable semantic differences by inspecting edges
        double edgePenalty = computeEdgeMismatchPenalty(nodeMapping);
        totalCost += edgePenalty;

        return new GEDResult(totalCost, nodeMapping);
    }

    /**
     * returns substitution cost between two nodes.
     * considers node label similarity and node category similarity.
     */
    private double substitutionCost(PDGNode n1, PDGNode n2) {
        // base cost if categories differ
        if (!NodeFeasibility.isSameNodeCategory(n1, n2)) {
            return 1.0;  // or big penalty
        }

        // compare the node "type" or attribute
        double attributePenalty = n1.getAttrib().equals(n2.getAttrib()) ? 0.0 : 0.8;

        // get the textual content to compare.
        String label1 = extractRelevantLabel(n1);
        String label2 = extractRelevantLabel(n2);

        double sim = JaroWinklerSimilarity(label1, label2); // in [0..1], higher=better
        double stringCost = 1.0 - sim; // bigger difference -> bigger cost

        double alpha = 0.1;  // weighting for syntactic differences, i.e. string difference
        double beta  = 0.9;  // weighting for semantic difference, i.e. attribute difference

        return alpha * stringCost + beta * attributePenalty;
    }

    private String extractRelevantLabel(PDGNode node) {
        // remove beginning of  'Type: CFGNODE: <code begins here>'
        return node.toString().substring(15);
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
        //  can use mappings.getReverseNodeMapping()

        return mismatchCost;
    }
}
