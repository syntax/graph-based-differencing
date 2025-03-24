package org.pdgdiff.graph;

import soot.toolkits.graph.pdg.PDGNode;

import java.util.*;

/**
 * this class is used to traverse the graph using bfs and collect all nodes.
 */
public class GraphTraversal {

    private static boolean debug = false;

    public static void setLogging(boolean enable) {
        debug = enable;
    }

    public static List<PDGNode> collectNodesBFS(PDG pdg) {
        if (debug) System.out.println("[BFS] Traversing graph");

        PDGNode start_node = pdg.getStartNode();
        List<PDGNode> nodeList = new ArrayList<>();

        if (start_node == null) {
            if (debug) System.out.println("[BFS] No start node found in the PDG.");
            return nodeList;
        }

        Queue<PDGNode> queue = new LinkedList<>();
        Set<PDGNode> visited = new HashSet<>();

        queue.add(start_node);
        visited.add(start_node);
        nodeList.add(start_node);

        // Begin BFS
        while (!queue.isEmpty()) {
            PDGNode current_node = queue.poll();
            if (debug) System.out.println("[BFS] Visiting node: " + current_node.toShortString());

            // Add dependents to the queue
            List<PDGNode> dependents = current_node.getDependents();
            for (PDGNode dependent : dependents) {
                if (!visited.contains(dependent)) {
                    queue.add(dependent);
                    visited.add(dependent);
                    nodeList.add(dependent);
                }
            }
        }

        if (debug) System.out.println("[BFS] BFS Graph traversal complete.");
        return nodeList;
    }

    public static int getNodeCount(PDG pdg) {
        List<PDGNode> nodeList = collectNodesBFS(pdg);
        return nodeList.size();
    }

}
