package org.pdgdiff.util;

import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.PDGNode;

import java.util.*;

public class GraphTraversal {

    private static boolean debug = false;


    public static void setLogging(boolean enable) {
        debug = enable;
    }


    // Method to traverse the graph using a breadth-first search
    public static int traverseGraphBFS(HashMutablePDG pdg) {
        if (debug) System.out.println("[BFS] Traversing graph" );
        // TODO Add logic to actually traverse the graph nodes

        PDGNode start_node = pdg.GetStartNode();

        if (start_node == null) {
            if (debug) System.out.println("[BFS] No start node found in the PDG.");
            return -1;
        }

        Queue<PDGNode> queue = new LinkedList<PDGNode>();
        Set<PDGNode> visited = new HashSet<PDGNode>();

        queue.add(start_node);
        visited.add(start_node);

        // begin BFS
        while (!queue.isEmpty()) {
            PDGNode current_node = queue.poll();
            if (debug) System.out.println("[BFS] Visiting node: " + current_node.toShortString());

            // Add dependents to the queue
            List<PDGNode> dependents = current_node.getDependents();
            for (PDGNode dependent : dependents) {
                if (!visited.contains(dependent)) {
                    queue.add(dependent);
                    visited.add(dependent);
                }
            }
        }

        if (debug) System.out.println("[BFS] BFS Graph traversal complete.");
        return visited.size();
    }

    // Method to traverse the graph using a depth-first search
    public static int traverseGraphDFS(HashMutablePDG pdg) {
        if (debug) System.out.println("[DFS] Traversing graph");

        PDGNode start_node = pdg.GetStartNode();

        if (start_node == null) {
            if (debug) System.out.println("[DFS] No start node found in the PDG.");
            return -1;
        }

        Stack<PDGNode> stack = new Stack<PDGNode>();
        Set<PDGNode> visited = new HashSet<PDGNode>();

        stack.push(start_node);
        visited.add(start_node);

        // begin DFS
        while (!stack.isEmpty()) {
            PDGNode current_node = stack.pop();
            if (debug) System.out.println("[DFS] Visiting node: " + current_node.toShortString());

            // Add dependents to the stack
            List<PDGNode> dependents = current_node.getDependents();
            for (PDGNode dependent : dependents) {
                if (!visited.contains(dependent)) {
                    stack.push(dependent);
                    visited.add(dependent);
                }
            }
        }

        if (debug) System.out.println("[DFS] DFS Graph traversal complete.");
        return visited.size();
    }
}
