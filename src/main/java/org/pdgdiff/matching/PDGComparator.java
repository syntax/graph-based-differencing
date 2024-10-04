package org.pdgdiff.matching;

import soot.toolkits.graph.pdg.HashMutablePDG;

import java.util.List;


// TODO: This file is lowk just a test, prob shouldnt be here for too long
// TODO: Structure of this within entire file is a bit wierd lol


public class PDGComparator {

    public static void compareAndPrintGraphSimilarity(List<HashMutablePDG> pdgList1, List<HashMutablePDG> pdgList2, String strategy) {
        // Instantiate the appropriate GraphMatcher
        GraphMatcher matcher = GraphMatcherFactory.createMatcher(strategy, pdgList1, pdgList2);

        // Perform the graph matching between the lists
        GraphMapping graphMapping = matcher.matchPDGLists();

        // Output the graph and node similarity results
        System.out.println("--> Graph matching complete using strategy: " + strategy);

        graphMapping.getGraphMapping().forEach((pdg1, pdg2) -> {
            String method1 = pdg1.getCFG().getBody().getMethod().getSignature();
            String method2 = pdg2.getCFG().getBody().getMethod().getSignature();
            System.out.println("PDG from class 1: " + method1 + " is matched with PDG from class 2: " + method2);
//            NodeMapping nodeMapping = graphMapping.getNodeMapping(pdg1);
//            if (nodeMapping != null) {
//                nodeMapping.printMappings();  // Print detailed node mappings between these PDGs
//            }
        });
    }
}


