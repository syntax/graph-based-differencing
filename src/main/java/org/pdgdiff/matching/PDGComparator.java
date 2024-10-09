package org.pdgdiff.matching;

import org.pdgdiff.edit.EditDistanceCalculator;
import org.pdgdiff.edit.model.EditOperation;
import org.pdgdiff.edit.EditScriptGenerator;
import soot.toolkits.graph.pdg.HashMutablePDG;

import java.util.List;

public class PDGComparator {

    public static void compareAndPrintGraphSimilarity(List<HashMutablePDG> pdgList1, List<HashMutablePDG> pdgList2, String strategy) {
        // Instantiate the appropriate GraphMatcher
        GraphMatcher matcher = GraphMatcherFactory.createMatcher(strategy, pdgList1, pdgList2);

        // Perform the graph matching between the lists
        GraphMapping graphMapping = matcher.matchPDGLists();

        // Output the graph and node similarity results
        System.out.println("--> Graph matching complete using strategy: " + strategy);

        graphMapping.getGraphMapping().forEach((srcPDG, dstPDG) -> {
            String method1 = srcPDG.getCFG().getBody().getMethod().getSignature();
            String method2 = dstPDG.getCFG().getBody().getMethod().getSignature();
            System.out.println("---\n> PDG from class 1: " + method1 + " is matched with PDG from class 2: " + method2);
            NodeMapping nodeMapping = graphMapping.getNodeMapping(srcPDG);
            if (nodeMapping != null) {
                System.out.println("--- Node Mapping:");
                nodeMapping.printMappings();

                List<EditOperation> editScript = EditScriptGenerator.generateEditScript(srcPDG, dstPDG, graphMapping);

                int editDistance = EditDistanceCalculator.calculateEditDistance(editScript);
                System.out.println("--- Edit information ---");
                System.out.println("-- Edit Distance: " + editDistance);

                System.out.println("-- Edit Script:");
                for (EditOperation op : editScript) {
                    System.out.println(op);
                }
            }
        });
    }
}
