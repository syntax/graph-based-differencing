package org.pdgdiff.matching.models.ullmann;

import org.pdgdiff.graph.GraphTraversal;
import org.pdgdiff.graph.PDG;
import org.pdgdiff.matching.NodeMapping;
import soot.toolkits.graph.pdg.PDGNode;

import java.util.*;

/**
 * UllmannMatcher class to perform graph matching using Ullmann's Algorithm.
 * This class contains methods to match two PDGs and return the node mappings between them.
 */
public class UllmannMatcher {
    private NodeMapping nodeMapping;

    private List<PDGNode> srcNodes;
    private List<PDGNode> dstNodes;
    private int n;
    private int m;
    private int[][] compatMatrix; // Compatibility matrix
    private Stack<int[][]> matBacklog;

    public UllmannMatcher(PDG srcPdg, PDG dstPdg) {
        this.nodeMapping = new NodeMapping();

        this.srcNodes = new ArrayList<>(GraphTraversal.collectNodesBFS(srcPdg));
        this.dstNodes = new ArrayList<>(GraphTraversal.collectNodesBFS(dstPdg));
        this.n = srcNodes.size();
        this.m = dstNodes.size();
        this.compatMatrix = new int[n][m];
        this.matBacklog = new Stack<>();
    }

    public NodeMapping match() {
        if (n > m) {
            // PDG1 cannot be a subgraph of PDG2
            return null;
        }

        // Initialize compatibility matrix
        initializeM();

        // Start recursive search
        if (matchRecursive(0)) {
            return nodeMapping;
        } else {
            return null;
        }
    }

    private void initializeM() {
        for (int i = 0; i < n; i++) {
            PDGNode node1 = srcNodes.get(i);
            for (int j = 0; j < m; j++) {
                PDGNode node2 = dstNodes.get(j);
                compatMatrix[i][j] = nodesAreCompatible(node1, node2) ? 1 : 0;
            }
        }
    }

    private boolean matchRecursive(int depth) {
        if (depth == n) {
            // All nodes have been matched
            buildNodeMapping();
            return true;
        }

        for (int j = 0; j < m; j++) {
            if (compatMatrix[depth][j] == 1) {
                if (isFeasible(depth, j)) {
                    int[][] MBackup = copyMatrix(compatMatrix);
                    // Remove conflicting mappings
                    for (int k = depth + 1; k < n; k++) {
                        compatMatrix[k][j] = 0;
                    }
                    for (int l = 0; l < m; l++) {
                        if (l != j) {
                            compatMatrix[depth][l] = 0;
                        }
                    }
                    compatMatrix[depth][j] = -1; // Mark as selected

                    matBacklog.push(MBackup);
                    if (matchRecursive(depth + 1)) {
                        return true;
                    }
                    compatMatrix = matBacklog.pop();
                }
            }
        }
        return false;
    }

    private boolean isFeasible(int i, int j) {
        // Check adjacency compatibility
        PDGNode srcNode = srcNodes.get(i);
        PDGNode dstNode = dstNodes.get(j);

        // For all previously mapped nodes
        for (int k = 0; k < i; k++) {
            int mappedIndex = -1;
            // Find the node in PDG2 that nodes1.get(k) is mapped to
            for (int l = 0; l < m; l++) {
                if (compatMatrix[k][l] == -1) {
                    mappedIndex = l;
                    break;
                }
            }
            if (mappedIndex != -1) {
                PDGNode mappedSrcNode = srcNodes.get(k);
                PDGNode mappedDstNode = this.dstNodes.get(mappedIndex);

                // check if adjacency is preserved
                boolean adjInPDG1 = areAdjacent(srcNode, mappedSrcNode);
                boolean adjInPDG2 = areAdjacent(dstNode, mappedDstNode);

                if (adjInPDG1 != adjInPDG2) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean areAdjacent(PDGNode n1, PDGNode n2) {
        // Check if n1 and n2 are adjacent in the PDG
        return n1.getDependents().contains(n2) || n1.getBackDependets().contains(n2)
                || n2.getDependents().contains(n1) || n2.getBackDependets().contains(n1);
    }

    private void buildNodeMapping() {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (compatMatrix[i][j] == -1) {
                    nodeMapping.addMapping(srcNodes.get(i), dstNodes.get(j));
                    break;
                }
            }
        }
    }

    private boolean nodesAreCompatible(PDGNode n1, PDGNode n2) {
        // TODO: add more like VF2
        // compare node types and attributes
        return n1.getType().equals(n2.getType()) && n1.getAttrib().equals(n2.getAttrib());
    }

    private int[][] copyMatrix(int[][] original) {
        int[][] copy = new int[n][m];
        for (int i = 0; i < n; i++) {
            System.arraycopy(original[i], 0, copy[i], 0, m);
        }
        return copy;
    }
}
