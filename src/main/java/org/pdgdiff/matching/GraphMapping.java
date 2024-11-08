package org.pdgdiff.matching;

import org.pdgdiff.graph.model.MyPDG;


import java.util.HashMap;
import java.util.Map;

/**
 * GraphMapping class to store mappings between PDGs. This class is used to store the mapping between
 * PDGs in two lists that have been matched by the GraphMatcher.
 */
public class GraphMapping {
    private Map<MyPDG, MyPDG> graphMapping;
    private Map<MyPDG, NodeMapping> nodeMappings; // TODO: make heuristic algo find node mappings

    public GraphMapping() {
        this.graphMapping = new HashMap<>();
        this.nodeMappings = new HashMap<>();
    }

    // Adds a mapping between two PDGs
    public void addGraphMapping(MyPDG srcPDG, MyPDG dstPDG, NodeMapping nodeMapping) {
        graphMapping.put(srcPDG, dstPDG);
        nodeMappings.put(srcPDG, nodeMapping);
    }

    // Retrieves the mapped PDG for a given PDG
    public MyPDG getMappedGraph(MyPDG srcPDG) {
        return graphMapping.get(srcPDG);
    }

    // Retrieves the node mapping for a given PDG pair
    public NodeMapping getNodeMapping(MyPDG srcPDG) {
        return nodeMappings.get(srcPDG);
    }

    // Exposes the entire graph mapping
    public Map<MyPDG, MyPDG> getGraphMapping() {
        return graphMapping;
    }

    // Pretty print all graph mappings for debugging
    public void printGraphMappings() {
        for (Map.Entry<MyPDG, MyPDG> entry : graphMapping.entrySet()) {
            System.out.println("Source PDG: " + entry.getKey() + " --> Mapped to: " + entry.getValue());
            NodeMapping nodeMapping = nodeMappings.get(entry.getKey());
            if (nodeMapping != null) {
                System.out.println("Node Mappings for this PDG:");
                nodeMapping.printMappings(); // Print node-level mappings for this PDG pair
            }
        }
    }
}
