package org.pdgdiff.matching.models.heuristic;

import org.pdgdiff.graph.model.MyPDG;
import org.pdgdiff.graph.model.MyPDGNode;
import org.pdgdiff.matching.NodeMapping;
import org.pdgdiff.graph.GraphTraversal;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.PDGNode;

import java.util.List;


public class HeuristicMatcher {
    // Compare two individual PDGs
    public double comparePDGs(MyPDG pdg1, MyPDG pdg2, NodeMapping nodeMapping) {
        double totalScore = 0.0;

        // Get nodes from the PDGs
        List<MyPDGNode> nodes1 = getPDGNodes(pdg1);
        List<MyPDGNode> nodes2 = getPDGNodes(pdg2);

        // Match nodes between the two PDGs
        for (int i = 0; i < Math.min(nodes1.size(), nodes2.size()); i++) {
            MyPDGNode node1 = nodes1.get(i);
            MyPDGNode node2 = nodes2.get(i);

            double nodeScore = similarityScore(node1, node2);

            if (nodeScore >= 0.6) {  // Only map nodes if similarity is above threshold
                nodeMapping.addMapping(node1, node2);
            }

            totalScore += nodeScore;
        }

        // Normalize node score by the number of nodes to handle graphs of different sizes
        totalScore = totalScore / Math.max(nodes1.size(), nodes2.size());

        // Include method name similarity
        String methodName1 = pdg1.getMethod().getName();
        String methodName2 = pdg2.getMethod().getName();
        double nameSimilarity = JaroWinklerSimilarity.JaroWinklerSimilarity(methodName1, methodName2);

        totalScore += nameSimilarity * 2;

        return totalScore;
    }

    // Use GraphTraversal to get all nodes in a PDG
    private List<MyPDGNode> getPDGNodes(MyPDG pdg) {
        return GraphTraversal.collectNodesBFS(pdg);
    }

    // Calculate a similarity score between two PDG nodes
    private double similarityScore(MyPDGNode node1, MyPDGNode node2) {
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
        // rather unfortunate typo from soot framework, have tried to open a pr for this in original repo
        if (node1.getBackDependets().size() == node2.getBackDependets().size()) {
            score += 0.5;
        }

        // Extract and compare labels
        String label1 = extractCodeOrLabel(node1);
        String label2 = extractCodeOrLabel(node2);

        // Add a score based on label similarity
        score += compareLabels(label1, label2);

        return score;
    }

    // Extract the actual code or detailed label from the PDGNode's m_node field
    private String extractCodeOrLabel(MyPDGNode node) {
        Object m_node = node.getNode();

        if (m_node instanceof soot.toolkits.graph.Block) {
            return extractCodeFromBlock((soot.toolkits.graph.Block) m_node);
        } else if (m_node instanceof soot.toolkits.graph.pdg.IRegion) {
            return extractDetailsFromRegion((soot.toolkits.graph.pdg.IRegion) m_node);
        } else {
            return node.toShortString();
        }
    }

    // Extract detailed code from a Block
    private String extractCodeFromBlock(soot.toolkits.graph.Block block) {
        StringBuilder codeRepresentation = new StringBuilder();
        for (Object unit : block) {
            codeRepresentation.append(unit.toString()).append("\n");
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

    // Compare node labels using Jaro-Winkler similarity
    protected double compareLabels(String label1, String label2) {
        if (label1 == null || label2 == null) {
            return 0.0;
        }

        double similarity = JaroWinklerSimilarity.JaroWinklerSimilarity(label1, label2);
        return similarity * 2.0;
    }
}
