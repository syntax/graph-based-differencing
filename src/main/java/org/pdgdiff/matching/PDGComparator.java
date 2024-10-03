//package org.pdgdiff.matching;
//
//import org.pdgdiff.util.GraphTraversal;
//import soot.toolkits.graph.pdg.HashMutablePDG;
//
//public class PDGComparator {
//
//    // TODO: This file is lowk just a test, prob shouldnt be here for too long
//    // TODO: Structure of this within entire file is a bit wierd lol
//    // Method to compare two PDGs and print node similarities
//    public static void compareAndPrintGraphSimilarity(HashMutablePDG pdg1, HashMutablePDG pdg2) {
//        // Instantiate the GraphMatcher
//        GraphMatcher matcher = new GraphMatcher(pdg1, pdg2);
//
//        // Perform the graph matching
//        NodeMapping mapping = matcher.match();
//
//        // Retrieve the class and method information from the PDG
//        String class1 = pdg1.getCFG().getBody().getMethod().getDeclaringClass().getName();
//        String method1 = pdg1.getCFG().getBody().getMethod().getName();
//
//        String class2 = pdg2.getCFG().getBody().getMethod().getDeclaringClass().getName();
//        String method2 = pdg2.getCFG().getBody().getMethod().getName();
//
//        // Print which class and method are being compared
//        System.out.println("Matching class: " + class1 + "." + method1 + " and " + class2 + "." + method2);
//
//        System.out.println("Graph one has " + GraphTraversal.collectNodesBFS(pdg1).size() + " nodes");
//        System.out.println("Graph two has " + GraphTraversal.collectNodesDFS(pdg2).size() + " nodes");
//
//        // Output the similarity results (printing node mappings)
//        System.out.println("--> Graph matching complete. Node similarities:");
//
//        // You can iterate over the mapped nodes and print their details
//        mapping.getNodeMapping().forEach((node1, node2) -> {
//            System.out.println("Node 1: " + node1.toShortString() + " is matched with Node 2: " + node2.toShortString());
//        });
//    }
//}

package org.pdgdiff.matching;

import org.pdgdiff.util.GraphTraversal;
import soot.toolkits.graph.pdg.HashMutablePDG;

import java.util.List;

public class PDGComparator {

    // Method to compare two lists of PDGs and print similarities
    public static void compareAndPrintGraphSimilarity(List<HashMutablePDG> pdgList1, List<HashMutablePDG> pdgList2) {
        // Instantiate the GraphMatcher to compare PDG lists
        GraphMatcher matcher = new GraphMatcher(pdgList1, pdgList2);

        // Perform the graph matching between the lists
//        GraphMapping graphMapping = matcher.matchPDGLists();
        System.out.println("Matching PDGs using VF2 algorithm...");
        GraphMapping graphMapping = matcher.matchPDGListsVF2();

        // Print the number of nodes in each graph
//        System.out.println("Graph 1 has " + GraphTraversal.collectNodesBFS(pdgList1.get(0)).size() + " nodes.");
//        System.out.println("Graph 2 has " + GraphTraversal.collectNodesDFS(pdgList2.get(0)).size() + " nodes.");

        // Output the graph and node similarity results
        System.out.println("--> Graph matching complete. Node similarities:");

        graphMapping.getGraphMapping().forEach((pdg1, pdg2) -> {

            String class1 = pdg1.getCFG().getBody().getMethod().getDeclaringClass().getName();
            String method1 = pdg1.getCFG().getBody().getMethod().getName();

            String class2 = pdg2.getCFG().getBody().getMethod().getDeclaringClass().getName();
            String method2 = pdg2.getCFG().getBody().getMethod().getName();

            System.out.println("PDG from class 1: " + class1 + "." + method1 + " is matched with PDG from class 2: " + class2 + "." + method2);
            NodeMapping nodeMapping = graphMapping.getNodeMapping(pdg1);
//            if (nodeMapping != null) {
//                nodeMapping.printMappings();  // Print detailed node mappings between these PDGs
//            }
        });
    }
}

