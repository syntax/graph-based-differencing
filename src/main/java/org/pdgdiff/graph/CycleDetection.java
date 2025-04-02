package org.pdgdiff.graph;

import soot.Unit;
import soot.tagkit.LineNumberTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.pdg.PDGNode;
import soot.toolkits.graph.pdg.PDGRegion;

import java.util.*;

// implements Tarjans algorithm for detection of strongly connected components
public class CycleDetection {

    private static boolean debug = true;
    private static int index = 0;
    // usin IdentityHashMap because PDGNode doesnt implement equals and hashCode
    private static Map<PDGNode, Integer> indices = new IdentityHashMap<>();
    private static Map<PDGNode, Integer> lowLinks = new IdentityHashMap<>();
    private static Deque<PDGNode> stack = new ArrayDeque<>();
    private static Set<Set<PDGNode>> stronglyConnectedComponents = new HashSet<>();

    public static void setLogging(boolean enable) {
        debug = enable;
    }

    public static boolean hasCycle(PDG pdg) {
        if (debug) System.out.println("[CycleDetection] Detecting cycles using Tarjan's Algorithm");

        index = 0;
        indices.clear();
        lowLinks.clear();
        stack.clear();
        stronglyConnectedComponents.clear();

        List<PDGNode> allNodes = GraphTraversal.collectNodesBFS(pdg);

        // tarjan's algorithm starting from each node
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
                            System.out.println("  Node: " + node + " at line " + lineNumber);
                        } else {
                            System.out.println("  Node: " + node + " (line number not available)");
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
                        System.out.println("  Node: " + node + " at line " + lineNumber);
                    } else {
                        System.out.println("  Node: " + node + " (line number not available)");
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
    private static void strongConnect(PDGNode node, PDG pdg) {
        indices.put(node, index);
        lowLinks.put(node, index);
        index++;
        stack.push(node);

        List<PDGNode> successors = pdg.getSuccsOf(node);

        for (PDGNode dependent : successors) {
            if (!indices.containsKey(dependent)) {
                strongConnect(dependent, pdg);
                lowLinks.put(node, Math.min(lowLinks.get(node), lowLinks.get(dependent)));
            } else if (stack.contains(dependent)) {
                lowLinks.put(node, Math.min(lowLinks.get(node), indices.get(dependent)));
            }
        }

        // if node is a root node, pop the stack and generate an SCC
        if (lowLinks.get(node).equals(indices.get(node))) {
            Set<PDGNode> scc = new HashSet<>();
            PDGNode w;
            do {
                w = stack.pop();
                scc.add(w);
            } while (w != node);
            stronglyConnectedComponents.add(scc);
        }
    }

    private static boolean hasSelfLoop(Set<PDGNode> scc, PDG pdg) {
        for (PDGNode node : scc) {
            for (PDGNode succ : pdg.getSuccsOf(node)) {
                if (node == succ) { // should be identity comparison
                    return true;
                }
            }
        }
        return false;
    }

    private static int getLineNumberFromPDGNode(PDGNode node) {
        if (node.getType() == PDGNode.Type.CFGNODE) {
            Unit headUnit = (Unit) node.getNode();
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
