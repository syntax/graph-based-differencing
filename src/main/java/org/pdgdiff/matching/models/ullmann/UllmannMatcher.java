package org.pdgdiff.matching.models.ullmann;

import org.pdgdiff.graph.GraphTraversal;
import org.pdgdiff.graph.model.MyPDG;
import org.pdgdiff.graph.model.MyPDGNode;
import org.pdgdiff.matching.NodeMapping;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.PDGNode;

import java.util.*;

/**
 * UllmannMatcher class to perform graph matching using Ullmann's Algorithm.
 * This class contains methods to match two PDGs and return the node mappings between them.
 */
public class UllmannMatcher {
    private MyPDG pdg1;
    private MyPDG pdg2;
    private NodeMapping nodeMapping;

    private List<MyPDGNode> nodes1;
    private List<MyPDGNode> nodes2;
    private int n;
    private int m;
    private int[][] M; // Compatibility matrix
    private Stack<int[][]> MStack;

    public UllmannMatcher(MyPDG pdg1, MyPDG pdg2) {
        this.pdg1 = pdg1;
        this.pdg2 = pdg2;
        this.nodeMapping = new NodeMapping();

        this.nodes1 = new ArrayList<>(GraphTraversal.collectNodesBFS(pdg1));
        this.nodes2 = new ArrayList<>(GraphTraversal.collectNodesBFS(pdg2));
        this.n = nodes1.size();
        this.m = nodes2.size();
        this.M = new int[n][m];
        this.MStack = new Stack<>();
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
            MyPDGNode node1 = nodes1.get(i);
            for (int j = 0; j < m; j++) {
                MyPDGNode node2 = nodes2.get(j);
                M[i][j] = nodesAreCompatible(node1, node2) ? 1 : 0;
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
            if (M[depth][j] == 1) {
                if (isFeasible(depth, j)) {
                    int[][] MBackup = copyMatrix(M);
                    // Remove conflicting mappings
                    for (int k = depth + 1; k < n; k++) {
                        M[k][j] = 0;
                    }
                    for (int l = 0; l < m; l++) {
                        if (l != j) {
                            M[depth][l] = 0;
                        }
                    }
                    M[depth][j] = -1; // Mark as selected

                    MStack.push(MBackup);
                    if (matchRecursive(depth + 1)) {
                        return true;
                    }
                    M = MStack.pop();
                }
            }
        }
        return false;
    }

    private boolean isFeasible(int i, int j) {
        // TODO: build more domain specific into thsi
        // Check adjacency compatibility
        MyPDGNode node1 = nodes1.get(i);
        MyPDGNode node2 = nodes2.get(j);

        // For all previously mapped nodes
        for (int k = 0; k < i; k++) {
            int mappedIndex = -1;
            // Find the node in PDG2 that nodes1.get(k) is mapped to
            for (int l = 0; l < m; l++) {
                if (M[k][l] == -1) {
                    mappedIndex = l;
                    break;
                }
            }
            if (mappedIndex != -1) {
                MyPDGNode mappedNode1 = nodes1.get(k);
                MyPDGNode mappedNode2 = nodes2.get(mappedIndex);

                // check if adjacency is preserved
                boolean adjInPDG1 = areAdjacent(node1, mappedNode1);
                boolean adjInPDG2 = areAdjacent(node2, mappedNode2);

                if (adjInPDG1 != adjInPDG2) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean areAdjacent(MyPDGNode n1, MyPDGNode n2) {
        // Check if n1 and n2 are adjacent in the PDG
        return n1.getDependents().contains(n2) || n1.getBackDependets().contains(n2)
                || n2.getDependents().contains(n1) || n2.getBackDependets().contains(n1);
    }

    private void buildNodeMapping() {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (M[i][j] == -1) {
                    nodeMapping.addMapping(nodes1.get(i), nodes2.get(j));
                    break;
                }
            }
        }
    }

    private boolean nodesAreCompatible(MyPDGNode n1, MyPDGNode n2) {
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
