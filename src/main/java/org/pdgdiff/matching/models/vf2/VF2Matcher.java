package org.pdgdiff.matching.models.vf2;

import org.pdgdiff.matching.NodeMapping;
import org.pdgdiff.graph.PDG;
import soot.toolkits.graph.pdg.PDGNode;

import java.util.Map;

/**
 * VF2Matcher class to perform graph matching using the VF2 algorithm. This class contains methods to match two PDGs
 * using the VF2 algorithm and return the node mappings between the two PDGs.
 */
public class VF2Matcher {
    private final PDG srcPdg;
    private final PDG dstPdg;
    private final NodeMapping nodeMapping;

    public VF2Matcher(PDG srcPdg, PDG dstPdg) {
        this.srcPdg = srcPdg;
        this.dstPdg = dstPdg;
        this.nodeMapping = new NodeMapping();
    }

    public NodeMapping match() {
        // Initialize state
        VF2State state = new VF2State(srcPdg, dstPdg);
        // Start recursive matching
        if (matchRecursive(state)) {
            return nodeMapping;
        } else {
            // No isomorphism found
            return null;
        }
    }


    // TODO: investigate vf2, i believe it to be too strict. need to figure out way of mapping methods -> methods if they
    // TODO: are similar 'ish', dont' need exact matches when I am effectively looking at version differences.
    private boolean matchRecursive(VF2State state) {
        if (state.isComplete()) {
            // Mapping is complete, transfer mappings to nodeMapping
            for (Map.Entry<PDGNode, PDGNode> entry : state.getMapping().entrySet()) {
                nodeMapping.addMapping(entry.getKey(), entry.getValue());
            }
            return true;
        }

        // Generate candidate pairs
        for (CandidatePair pair : state.generateCandidates()) {
            if (state.isFeasible(pair)) {
                state.addPair(pair);
                if (matchRecursive(state)) {
                    return true;
                }
                state.removePair(pair);
            }
        }
        return false;
    }
}
