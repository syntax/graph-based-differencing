package org.pdgdiff.matching;

import org.pdgdiff.graph.PDG;

import java.util.HashMap;
import java.util.Map;

/**
 * GraphMapping class to store mappings between PDGs. This class is used to store the mapping between
 * PDGs in two lists that have been matched by the GraphMatcher. For each PDG mapping, a node mapping
 * is also stored.
 */
public class GraphMapping {
    private Map<PDG, PDG> graphMapping;
    private Map<PDG, NodeMapping> nodeMappings;

    public GraphMapping() {
        this.graphMapping = new HashMap<>();
        this.nodeMappings = new HashMap<>();
    }

    public void addGraphMapping(PDG srcPDG, PDG dstPDG, NodeMapping nodeMapping) {
        graphMapping.put(srcPDG, dstPDG);
        nodeMappings.put(srcPDG, nodeMapping);
    }

    // retrieves the node mapping for a given PDG pair
    public NodeMapping getNodeMapping(PDG srcPDG) {
        return nodeMappings.get(srcPDG);
    }

    // exposes the entire graph mapping
    public Map<PDG, PDG> getGraphMapping() {
        return graphMapping;
    }

    // pretty print all graph mappings for debugging (redundant otherwise)
    public void printGraphMappings() {
        for (Map.Entry<PDG, PDG> entry : graphMapping.entrySet()) {
            System.out.println("Source PDG: " + entry.getKey() + " --> Mapped to: " + entry.getValue());
            NodeMapping nodeMapping = nodeMappings.get(entry.getKey());
            if (nodeMapping != null) {
                System.out.println("Node Mappings for this PDG:");
                nodeMapping.printMappings(); // Print node-level mappings for this PDG pair
            }
        }
    }
}
