package org.pdgdiff.graph;


import org.pdgdiff.graph.model.MyPDG;
import org.pdgdiff.graph.model.MyPDGNode;

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
    public static List<MyPDGNode> collectNodesBFS(MyPDG pdg) {
        if (debug) System.out.println("[BFS] Traversing graph");

        MyPDGNode startNode = pdg.getStartNode();
        List<MyPDGNode> nodeList = new ArrayList<>();

        if (startNode == null) {
            if (debug) System.out.println("[BFS] No start node found in the PDG.");
            return nodeList;
        }

        Queue<MyPDGNode> queue = new LinkedList<>();
        Set<MyPDGNode> visited = new HashSet<>();

        queue.add(startNode);
        visited.add(startNode);
        nodeList.add(startNode);

        // Begin BFS
        while (!queue.isEmpty()) {
            MyPDGNode currentNode = queue.poll();
            if (debug) System.out.println("[BFS] Visiting node: " + currentNode.toShortString());

            // Add dependents to the queue
            List<MyPDGNode> dependents = currentNode.getDependents();
            for (MyPDGNode dependent : dependents) {
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
    public static List<MyPDGNode> collectNodesDFS(MyPDG pdg) {
        if (debug) System.out.println("[DFS] Traversing graph");

        MyPDGNode startNode = pdg.getStartNode();
        List<MyPDGNode> nodeList = new ArrayList<>();

        if (startNode == null) {
            if (debug) System.out.println("[DFS] No start node found in the PDG.");
            return nodeList;
        }

        Stack<MyPDGNode> stack = new Stack<>();
        Set<MyPDGNode> visited = new HashSet<>();

        stack.push(startNode);
        visited.add(startNode);
        nodeList.add(startNode);

        // Begin DFS
        while (!stack.isEmpty()) {
            MyPDGNode currentNode = stack.pop();
            if (debug) System.out.println("[DFS] Visiting node: " + currentNode.toShortString());

            // Add dependents to the stack
            List<MyPDGNode> dependents = currentNode.getDependents();
            for (MyPDGNode dependent : dependents) {
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

    public static int getNodeCount(MyPDG pdg) {
        List<MyPDGNode> nodeList = collectNodesBFS(pdg);
        return nodeList.size();
    }

    public static int getNodeCount(List<MyPDGNode> nodeList) { return nodeList.size(); }
}