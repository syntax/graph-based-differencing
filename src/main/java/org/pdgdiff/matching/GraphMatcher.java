package org.pdgdiff.matching;

import org.pdgdiff.matching.models.JaroWinklerSimilarity;
import org.pdgdiff.util.GraphTraversal;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.PDGNode;

import java.util.List;

public class GraphMatcher {
    private List<HashMutablePDG> pdgList1;
    private List<HashMutablePDG> pdgList2;
    private GraphMapping graphMapping; // To store graph-level and node-level mappings

    public GraphMatcher(List<HashMutablePDG> list1, List<HashMutablePDG> list2) {
        this.pdgList1 = list1;
        this.pdgList2 = list2;
        this.graphMapping = new GraphMapping(); // Initialize GraphMapping
    }

    // Entry point for matching lists of PDGs
    public GraphMapping matchPDGLists() {
        for (HashMutablePDG pdg1 : pdgList1) {
            HashMutablePDG bestMatch = null;
            NodeMapping nodeMapping = new NodeMapping(); // Track node-level mappings
            double bestScore = Double.MAX_VALUE;

            // Compare pdg1 with each PDG from the second list
            for (HashMutablePDG pdg2 : pdgList2) {
                // Compute similarity score between the two PDGs
                double score = comparePDGs(pdg1, pdg2, nodeMapping);

                // Keep track of the best match based on the similarity score
                if (score < bestScore) {
                    bestScore = score;
                    bestMatch = pdg2;
                }
            }

            if (bestMatch != null) {
                // Add the best match along with node mapping to the GraphMapping
                graphMapping.addGraphMapping(pdg1, bestMatch, nodeMapping);
            }
        }

        return graphMapping;  // Return the complete GraphMapping
    }

    // Use GraphTraversal to get all nodes in a PDG
    private List<PDGNode> getPDGNodes(HashMutablePDG pdg) {
        // Use BFS traversal to collect all nodes
        return GraphTraversal.collectNodesBFS(pdg);
    }

    // Compare two individual PDGs
    private double comparePDGs(HashMutablePDG pdg1, HashMutablePDG pdg2, NodeMapping nodeMapping) {
        double totalScore = 0.0;

        // Get nodes from the PDGs using the traversal method
        List<PDGNode> nodes1 = getPDGNodes(pdg1);
        List<PDGNode> nodes2 = getPDGNodes(pdg2);

        // Match nodes between the two PDGs
        for (int i = 0; i < Math.min(nodes1.size(), nodes2.size()); i++) {
            PDGNode node1 = nodes1.get(i);
            PDGNode node2 = nodes2.get(i);

            double nodeScore = similarityScore(node1, node2);  // Use your existing similarity logic

            if (nodeScore >= 0.6) {  // Only map nodes if similarity is above threshold
                nodeMapping.addMapping(node1, node2);  // Track node-level mappings
            }

            totalScore += nodeScore;
        }

        // Normalize score by the number of nodes to handle graphs of different sizes
        return totalScore / Math.max(nodes1.size(), nodes2.size());
    }

    // Calculate a similarity score between two PDG nodes
    private double similarityScore(PDGNode node1, PDGNode node2) {
        double score = 0.0;

        // Compare node types
        if (node1.getType() == node2.getType()) {
            score += 1.0;
        }

        // Compare node attributes
        if (node1.getAttrib() == node2.getAttrib()) {
            score += 0.5;
        }

        // Compare the number of dependents and back dependents
        if (node1.getDependents().size() == node2.getDependents().size()) {
            score += 0.5;
        }
        if (node1.getBackDependets().size() == node2.getBackDependets().size()) {
            score += 0.5;
        }

        // Extract and compare labels
        String label1 = extractCodeOrLabel(node1);  // Extract the content/label for node1
        String label2 = extractCodeOrLabel(node2);  // Extract the content/label for node2

        // Add a score based on label similarity
        score += compareLabels(label1, label2);  // Custom comparison logic for partial/full matches

        return score;
    }

    // Extract the actual code or detailed label from the PDGNode's m_node field
    private String extractCodeOrLabel(PDGNode node) {
        Object m_node = node.getNode();  // Access m_node from PDGNode

        if (m_node instanceof soot.toolkits.graph.Block) {
            soot.toolkits.graph.Block block = (soot.toolkits.graph.Block) m_node;
            return extractCodeFromBlock(block);  // Extract code from the block
        } else if (m_node instanceof soot.toolkits.graph.pdg.IRegion) {
            soot.toolkits.graph.pdg.IRegion region = (soot.toolkits.graph.pdg.IRegion) m_node;
            return extractDetailsFromRegion(region);  // Extract region details
        } else {
            return node.toShortString();  // Fallback to the short string
        }
    }

    // Extract detailed code from a Block
    private String extractCodeFromBlock(soot.toolkits.graph.Block block) {
        StringBuilder codeRepresentation = new StringBuilder();
        for (Object unit : block) {
            codeRepresentation.append(unit.toString()).append("\n");  // Append each statement in the block
        }
        return codeRepresentation.toString();
    }

    // Extract detailed information from a Region
    private String extractDetailsFromRegion(soot.toolkits.graph.pdg.IRegion region) {
        StringBuilder regionDetails = new StringBuilder("Region ID: " + region.getID() + "\n");

        List<soot.toolkits.graph.Block> blocks = region.getBlocks();
        for (soot.toolkits.graph.Block block : blocks) {
            regionDetails.append(extractCodeFromBlock(block)).append("\n");
        }

        return regionDetails.toString();
    }

    // Compare node labels (strings) using Jaro-Winkler similarity and return a percentage
    protected double compareLabels(String label1, String label2) {
        if (label1 == null || label2 == null) {
            return 0.0;
        }

        // Calculate the Jaro-Winkler similarity
        double similarity = JaroWinklerSimilarity.JaroWinklerSimilarity(label1, label2);
        return similarity;
    }
}
