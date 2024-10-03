package org.pdgdiff.util;

import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.PDGNode;

import java.util.*;

/**
 * GraphTraversal class to perform graph traversal on a PDG. This class contains methods to traverse the PDG
 * using both breadth-first search (BFS) and depth-first search (DFS) algorithms.
 */
public class GraphTraversal {

    private static boolean debug = false;

    public static void setLogging(boolean enable) {
        debug = enable;
    }

    // Method to traverse the graph using a breadth-first search and collect all nodes
    public static List<PDGNode> collectNodesBFS(HashMutablePDG pdg) {
        if (debug) System.out.println("[BFS] Traversing graph");

        PDGNode start_node = pdg.GetStartNode();
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

    // Method to traverse the graph using a depth-first search and collect all nodes
    public static List<PDGNode> collectNodesDFS(HashMutablePDG pdg) {
        if (debug) System.out.println("[DFS] Traversing graph");

        PDGNode start_node = pdg.GetStartNode();
        List<PDGNode> nodeList = new ArrayList<>();

        if (start_node == null) {
            if (debug) System.out.println("[DFS] No start node found in the PDG.");
            return nodeList;
        }

        Stack<PDGNode> stack = new Stack<>();
        Set<PDGNode> visited = new HashSet<>();

        stack.push(start_node);
        visited.add(start_node);
        nodeList.add(start_node);

        // Begin DFS
        while (!stack.isEmpty()) {
            PDGNode current_node = stack.pop();
            if (debug) System.out.println("[DFS] Visiting node: " + current_node.toShortString());

            // Add dependents to the stack
            List<PDGNode> dependents = current_node.getDependents();
            for (PDGNode dependent : dependents) {
                if (!visited.contains(dependent)) {
                    stack.push(dependent);
                    visited.add(dependent);
                    nodeList.add(dependent);
                }
            }
        }

        if (debug) System.out.println("[DFS] DFS Graph traversal complete.");
        return nodeList;
    }

    public static int getNodeCount(HashMutablePDG pdg) {
        List<PDGNode> nodeList = collectNodesBFS(pdg);
        return nodeList.size();
    }

    // Optionally, if you have already collected nodes and want to avoid traversal:
    public static int getNodeCount(List<PDGNode> nodeList) {
        return nodeList.size();
    }

}
