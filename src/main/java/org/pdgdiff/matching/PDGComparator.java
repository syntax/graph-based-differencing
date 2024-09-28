package org.pdgdiff.matching;

import org.pdgdiff.util.GraphTraversal;
import soot.toolkits.graph.pdg.HashMutablePDG;

public class PDGComparator {

    // TODO: This file is lowk just a test, prob shouldnt be here for too long
    // TODO: Structure of this within entire file is a bit wierd lol
    // Method to compare two PDGs and print node similarities
    public static void compareAndPrintGraphSimilarity(HashMutablePDG pdg1, HashMutablePDG pdg2) {
        // Instantiate the GraphMatcher
        GraphMatcher matcher = new GraphMatcher(pdg1, pdg2);

        // Perform the graph matching
        GraphMapping mapping = matcher.match();

        // Retrieve the class and method information from the PDG
        String class1 = pdg1.getCFG().getBody().getMethod().getDeclaringClass().getName();
        String method1 = pdg1.getCFG().getBody().getMethod().getName();

        String class2 = pdg2.getCFG().getBody().getMethod().getDeclaringClass().getName();
        String method2 = pdg2.getCFG().getBody().getMethod().getName();

        // Print which class and method are being compared
        System.out.println("Matching class: " + class1 + "." + method1 + " and " + class2 + "." + method2);

        System.out.println("Graph one has " + GraphTraversal.traverseGraphDFS(pdg1) + " nodes");
        System.out.println("Graph two has " + GraphTraversal.traverseGraphDFS(pdg2) + " nodes");

        // Output the similarity results (printing node mappings)
        System.out.println("--> Graph matching complete. Node similarities:");

        // You can iterate over the mapped nodes and print their details
        mapping.getNodeMapping().forEach((node1, node2) -> {
            System.out.println("Node 1: " + node1.toShortString() + " is matched with Node 2: " + node2.toShortString());
        });
    }
}
