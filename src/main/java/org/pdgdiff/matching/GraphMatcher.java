package org.pdgdiff.matching;

import org.pdgdiff.matching.models.JaroWinklerSimilarity;
import org.pdgdiff.util.GraphTraversal;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.PDGNode;

import java.util.HashSet;
import java.util.List;

/**
 * GraphMatcher class to compare two lists of PDGs and find similarities, aiming to create a 1:1 mapping between
 * PDGs from the two lists. These PDGs represent methods, which each list representing a different class (or file)
 * This class uses a custom similarity score to compare PDG nodes and labels. WIP.
 */
public class GraphMatcher {
    private final HashSet<HashMutablePDG> matchedPDGs;
    private List<HashMutablePDG> pdgList1;
    private List<HashMutablePDG> pdgList2;
    private GraphMapping graphMapping; // To store graph-level and node-level mappings

    public GraphMatcher(List<HashMutablePDG> list1, List<HashMutablePDG> list2) {
        this.pdgList1 = list1;
        this.pdgList2 = list2;
        this.graphMapping = new GraphMapping(); // Initialize GraphMapping
        this.matchedPDGs = new HashSet<>();  // Initialize the set to track matched PDGs
    }

    // Entry point for matching lists of PDGs
    public GraphMapping matchPDGLists() {
        for (HashMutablePDG pdg1 : pdgList1) {
            HashMutablePDG bestMatch = null;
            NodeMapping nodeMapping = new NodeMapping(); // Track node-level mappings

            double bestScore = Double.NEGATIVE_INFINITY;  // Start with a very low score

            // Compare pdg1 with each PDG from the second list
            for (HashMutablePDG pdg2 : pdgList2) {
                // Skip if this PDG has already been matched
                if (matchedPDGs.contains(pdg2)) {
                    continue;
                }

                // Compute similarity score between the two PDGs
                double score = comparePDGs(pdg1, pdg2, nodeMapping);

                // Prioritize name similarity first
                if (score > bestScore) {
                    bestScore = score;
                    bestMatch = pdg2;
                }
            }

            if (bestMatch != null) {
                // Add the best match along with node mapping to the GraphMapping
                matchedPDGs.add(bestMatch);
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

        // normalise node score by the number of nodes to handle graphs of different sizes
        totalScore = totalScore / Math.max(nodes1.size(), nodes2.size());

        String methodName1 = pdg1.getCFG().getBody().getMethod().getName();
        String methodName2 = pdg2.getCFG().getBody().getMethod().getName();
        double nameSimilarity = JaroWinklerSimilarity.JaroWinklerSimilarity(methodName1, methodName2);

        totalScore += nameSimilarity * 2;

        return totalScore;
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
        return similarity * 2.0;
    }
}
