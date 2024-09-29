package org.pdgdiff.matching;

import org.pdgdiff.matching.models.JaroWinklerSimilarity;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.PDGNode;
import soot.toolkits.graph.pdg.IRegion;

import java.util.List;

public class GraphMatcher {
    private HashMutablePDG graph1;
    private HashMutablePDG graph2;
    private GraphMapping mapping;

    public GraphMatcher(HashMutablePDG g1, HashMutablePDG g2) {
        this.graph1 = g1;
        this.graph2 = g2;
        this.mapping = new GraphMapping();
    }

    // This method is the entry point for the matching process
    // TODO: need to think a bit more about this. Each class has a selection of PDGs. For each class, I need to compare each PDG with
    // TODO: each other, and then within each PDG, I need to compare each node with each other.
    public GraphMapping match() {
        PDGNode startNode1 = graph1.GetStartNode();
        PDGNode startNode2 = graph2.GetStartNode();

        // Perform matching starting from the root nodes
        if (startNode1 != null && startNode2 != null) {
            matchNodes(startNode1, startNode2);
        }

        return mapping;
    }

    // Recursive method to match nodes in the PDG
    private void matchNodes(PDGNode node1, PDGNode node2) {
        // Match nodes only if they meet a similarity threshold
        if (similarityScore(node1, node2) >= 0.6) {  // Threshold for similarity
            // Add the matched nodes to the mapping
            mapping.addMapping(node1, node2);

            // Match both dependents and back dependents (data and control dependencies)
            List<PDGNode> dependents1 = node1.getDependents();
            List<PDGNode> dependents2 = node2.getDependents();
            List<PDGNode> backDependents1 = node1.getBackDependets();
            List<PDGNode> backDependents2 = node2.getBackDependets();

            // Match dependent nodes recursively
            matchDependents(dependents1, dependents2);
            matchDependents(backDependents1, backDependents2);
        }
    }

    private void matchDependents(List<PDGNode> dependents1, List<PDGNode> dependents2) {
        // You can refine this to find the best matches between dependents
        for (int i = 0; i < Math.min(dependents1.size(), dependents2.size()); i++) {
            matchNodes(dependents1.get(i), dependents2.get(i));
        }
    }

    // Calculate a similarity score between two nodes (returns a similarity percentage)
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

        // **Extract the actual content from the node's m_node field**
        String label1 = extractCodeOrLabel(node1);  // Extract the content/label for node1
        String label2 = extractCodeOrLabel(node2);  // Extract the content/label for node2

        // Add a score based on label similarity
        System.out.println("Comparing labels from node: " + node1.toShortString() + " and " + node2.toShortString());
        score += compareLabels(label1, label2);  // Custom comparison logic for partial/full matches

        return score;
    }


    /*
    Code below is used for matching similarity of actual syntax each node has. This is going to be pretty critical
    and needs investigation / running past supervisor. Perhaps granularity of the graph is too high, but the paper
    (PDG and its use in optimisation) believes in defining regions to lump together section with similar control/data
    flow.

    A bit of a conflict of interest there of using a PDG vs code differencing.
     */

    // Extract the actual code or detailed label from the PDGNode's m_node field
    private String extractCodeOrLabel(PDGNode node) {
        Object m_node = node.getNode();  // Access m_node from PDGNode

        if (m_node instanceof Block) {
            Block block = (Block) m_node;
            return extractCodeFromBlock(block);  // Extract code from the block
        } else if (m_node instanceof IRegion) {
            IRegion region = (IRegion) m_node;
            return extractDetailsFromRegion(region);  // Extract region details
        } else {
            return node.toShortString();  // Fallback to the short string
        }
    }

    // Extract detailed code from a Block
    private String extractCodeFromBlock(Block block) {
        StringBuilder codeRepresentation = new StringBuilder();
        for (Object unit : block) {
            codeRepresentation.append(unit.toString()).append("\n");  // Append each statement in the block
        }
        return codeRepresentation.toString();
    }

    // Extract detailed information from a Region
    private String extractDetailsFromRegion(IRegion region) {
        StringBuilder regionDetails = new StringBuilder("Region ID: " + region.getID() + "\n");

        List<Block> blocks = region.getBlocks();
        for (Block block : blocks) {
            regionDetails.append(extractCodeFromBlock(block)).append("\n");
        }

        return regionDetails.toString();
    }

    // Compare node labels (strings) using Jaro-Winkler similarity and return a percentage
    protected double compareLabels(String label1, String label2) {
        if (label1 == null || label2 == null) {
            return 0.0;
        }

        // TODO: Assess Jaro-Winkler similarity as a metric, seems to generally just return high scores even if non similar
        // Calculate the Jaro-Winkler similarity
        double similarity = JaroWinklerSimilarity.JaroWinklerSimilarity(label1, label2);
        System.out.println("Similarity between labels: " + similarity);
        // Return the similarity score as a percentage
        return similarity;
    }
}
