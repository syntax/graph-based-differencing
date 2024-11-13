package org.pdgdiff.graph;

import soot.Unit;
import soot.tagkit.LineNumberTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.PDGNode;

import java.util.*;

// implements Tarjans algorithm for detection of strongly connected components
public class CycleDetection {

    private static boolean debug = true;
    private static int index = 0;
    // usin IdentityHashMap because PDGNode doesn't implement equals and hashCode
    private static Map<PDGNode, Integer> indices = new IdentityHashMap<>();
    private static Map<PDGNode, Integer> lowLinks = new IdentityHashMap<>();
    private static Deque<PDGNode> stack = new ArrayDeque<>();
    private static Set<Set<PDGNode>> stronglyConnectedComponents = new HashSet<>();

    public static void setLogging(boolean enable) {
        debug = enable;
    }

    public static boolean hasCycle(HashMutablePDG pdg) {
        if (debug) System.out.println("[CycleDetection] Detecting cycles using Tarjan's Algorithm");

        index = 0;
        indices.clear();
        lowLinks.clear();
        stack.clear();
        stronglyConnectedComponents.clear();

        List<PDGNode> allNodes = GraphTraversal.collectNodesBFS(pdg);

        // Tarjan's algorithm starting from each node
        for (PDGNode node : allNodes) {
            if (!indices.containsKey(node)) {
                strongConnect(node, pdg);
            }
        }

        // check if any strongly connected component is a cycle
        boolean hasCycle = false;
        int maxCycleSize = 0;
        List<Set<PDGNode>> maxSizeSCCs = new ArrayList<>();

        for (Set<PDGNode> scc : stronglyConnectedComponents) {
            if (scc.size() > 1 || hasSelfLoop(scc, pdg)) {
                hasCycle = true;

                int sccSize = scc.size();
                if (sccSize > maxCycleSize) {
                    maxCycleSize = sccSize;
                    maxSizeSCCs.clear();
                    maxSizeSCCs.add(scc);
                } else if (sccSize == maxCycleSize) {
                    maxSizeSCCs.add(scc);
                }

                if (debug) {
                    System.out.println("[CycleDetection] Cycle detected in SCC:");
                    for (PDGNode node : scc) {
                        int lineNumber = getLineNumberFromPDGNode(node);
                        if (lineNumber != -1) {
                            System.out.println("  Node: " + node.toShortString() + " at line " + lineNumber);
                        } else {
                            System.out.println("  Node: " + node.toShortString() + " (line number not available)");
                        }
                    }
                }
            }
        }

        if (hasCycle && debug) {
            System.out.println("[CycleDetection] Largest cycle size: " + maxCycleSize);
            for (Set<PDGNode> scc : maxSizeSCCs) {
                System.out.println("[CycleDetection] -> Largest cycle detected in this SCC:");
                for (PDGNode node : scc) {
                    int lineNumber = getLineNumberFromPDGNode(node);
                    if (lineNumber != -1) {
                        System.out.println("  Node: " + node.toShortString() + " at line " + lineNumber);
                    } else {
                        System.out.println("  Node: " + node.toShortString() + " (line number not available)");
                    }
                }
            }
        }

        if (!hasCycle && debug) {
            System.out.println("[CycleDetection] No cycles detected in graph");
        }

        return hasCycle;
    }

    // gather SCCs
    private static void strongConnect(PDGNode node, HashMutablePDG pdg) {
        // set the depth index for node to the smallest unused index
        indices.put(node, index);
        lowLinks.put(node, index);
        index++;
        stack.push(node);

        for (PDGNode dependent : pdg.getSuccsOf(node)) {
            if (!indices.containsKey(dependent)) {
                // succ has not yet been visited; recurse on it
                strongConnect(dependent, pdg);
                lowLinks.put(node, Math.min(lowLinks.get(node), lowLinks.get(dependent)));
            } else if (stack.contains(dependent)) {
                // succ is in stack and hence in the current SCC
                lowLinks.put(node, Math.min(lowLinks.get(node), indices.get(dependent)));
            }
        }

        // if node is a root node, pop the stack and generate an SCC
        if (lowLinks.get(node).equals(indices.get(node))) {
            Set<PDGNode> scc = Collections.newSetFromMap(new IdentityHashMap<>());
            PDGNode w;
            do {
                w = stack.pop();
                scc.add(w);
            } while (w != node); // Should be identiy comparison
            stronglyConnectedComponents.add(scc);
        }
    }

    private static boolean hasSelfLoop(Set<PDGNode> scc, HashMutablePDG pdg) {
        for (PDGNode node : scc) {
            for (PDGNode succ : pdg.getSuccsOf(node)) {
                if (node == succ) { // should be identity comparison
                    return true;
                }
            }
        }
        return false;
    }

    // method  to extract the line number from a PDGNode, prob alr got this lol
    private static int getLineNumberFromPDGNode(PDGNode node) {
        if (node.getType() == PDGNode.Type.CFGNODE) {
            Block block = (Block) node.getNode();
            Unit headUnit = block.getHead();
            if (headUnit != null) {
                Tag tag = headUnit.getTag("LineNumberTag");
                if (tag instanceof LineNumberTag) {
                    LineNumberTag lineNumberTag = (LineNumberTag) tag;
                    return lineNumberTag.getLineNumber();
                }
            }
        }
        return -1;
    }
}
