package org.pdgdiff.edit;

import org.pdgdiff.edit.model.*;
import org.pdgdiff.matching.GraphMapping;
import org.pdgdiff.matching.NodeMapping;
import soot.Unit;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.IRegion;
import soot.toolkits.graph.pdg.PDGNode;
import soot.Body;
import soot.util.Chain;

import java.util.*;

public class EditScriptGenerator {
    // TODO: THIS SEEMS VERY 'UDPATE' INSTRUCTION HEAVY ATM. need to investigate this, seems to never pick other stuff
    /**
     * Generates a list of edit operations required to transform srcPDG into dstPDG based on the provided node mappings.
     *
     * @param srcPDG       Source PDG
     * @param dstPDG       Target PDG
     * @param graphMapping The mapping between PDGs and their node mappings
     * @return List of EditOperation objects representing the edit script
     */
    public static List<EditOperation> generateEditScript(HashMutablePDG srcPDG, HashMutablePDG dstPDG, GraphMapping graphMapping) {
        List<EditOperation> editScript = new ArrayList<>();
        Set<PDGNode> visitedNodes = new HashSet<>();

        NodeMapping nodeMapping = graphMapping.getNodeMapping(srcPDG);
        if (nodeMapping == null) {
            // No mapping exists; delete all nodes from srcPDG and insert all nodes from dstPDG
            // dont think this condition will ever actually hit as of rn
            for (PDGNode node : srcPDG) {
                editScript.add(new Delete(node));
            }
            for (PDGNode node : dstPDG) {
                editScript.add(new Insert(node));
            }
            return editScript;
        }

        Map<PDGNode, PDGNode> mappings = nodeMapping.getNodeMapping();
        Set<PDGNode> srcNodesMapped = mappings.keySet();
        Set<PDGNode> dstNodesMapped = new HashSet<>(mappings.values());

        // Handle deletions
        for (PDGNode srcNode : srcPDG) {
            if (!srcNodesMapped.contains(srcNode)) {
                editScript.add(new Delete(srcNode));
            }
        }

        // Handle insertions
        for (PDGNode dstNode : dstPDG) {
            if (!dstNodesMapped.contains(dstNode)) {
                editScript.add(new Insert(dstNode));
            }
        }

        // Handle updates and moves
        for (PDGNode srcNode : srcNodesMapped) {
            PDGNode dstNode = mappings.get(srcNode);

            if (!visitedNodes.contains(srcNode) && !visitedNodes.contains(dstNode)) {
                if (!nodesAreEqual(srcNode, dstNode, visitedNodes)) {
                    // syntax or semantic differences detected, generate appropriate update or move operation
                    // TODO: need to perfect this to analyse bodies probably, so i can acc print syntax differences
                    // TODO: as of right now this is only storing the different attribute name and has no recollection of the syntactic changes to the body.
                    editScript.add(new Update(srcNode, srcNode.getAttrib().toString(), dstNode.getAttrib().toString()));
                } else {
                    // if syntax is equal, check for moves (if connections change)
                    // TODO: Need to consider the case where the node is both moved and updated...
                    List<PDGNode> srcPredecessors = srcNode.getBackDependets();
                    List<PDGNode> dstPredecessors = dstNode.getBackDependets();

                    // map predecessors to their counterparts in the other PDG
                    List<PDGNode> mappedSrcPredecessors = mapNodes(srcPredecessors, nodeMapping);
                    List<PDGNode> mappedDstPredecessors = dstPredecessors;

                    if (!new HashSet<>(mappedSrcPredecessors).equals(new HashSet<>(mappedDstPredecessors))) {
                        editScript.add(new Move(srcNode, mappedSrcPredecessors, mappedDstPredecessors));
                    }
                }
                visitedNodes.add(srcNode);
                visitedNodes.add(dstNode);
            }
        }

        return editScript;
    }

    private static List<PDGNode> mapNodes(List<PDGNode> nodes, NodeMapping nodeMapping) {
        List<PDGNode> mappedNodes = new ArrayList<>();
        for (PDGNode node : nodes) {
            PDGNode mappedNode = nodeMapping.getMappedNode(node);
            if (mappedNode != null) {
                mappedNodes.add(mappedNode);
            }
        }
        return mappedNodes;
    }

    private static boolean nodesAreEqual(PDGNode n1, PDGNode n2, Set<PDGNode> visitedNodes) {
        if (visitedNodes.contains(n1) || visitedNodes.contains(n2)) {
            return true;
        }
        visitedNodes.add(n1);
        visitedNodes.add(n2);

        // compare basic properties
        if (!n1.getType().equals(n2.getType())) {
            return false;
        }

        // cmp syntax (node contents) for CFGNODEs and REGIONs
        if (n1.getType() == PDGNode.Type.CFGNODE) {
            // for CFGNODE, compare the Block representation (check actual instructions or statements)
            // TODO: pretty sure this cast wont acc work, investigate
            Block block1 = (Block) n1.getNode();
            Block block2 = (Block) n2.getNode();

            // cmp the statements in the block
            if (!compareBlockContents(block1, block2)) {
                return false; // Syntax differs
            }
        } else if (n1.getType() == PDGNode.Type.REGION) {
            // for REGION, compare region IDs or other IRegion-related attributes
            IRegion region1 = (IRegion) n1.getNode();
            IRegion region2 = (IRegion) n2.getNode();
            if (region1.getID() != region2.getID()) {
                return false; // Different regions
            }
        }

        // compare additional attributes (e.g., header/entry/loop conditions)
        if (!n1.getAttrib().equals(n2.getAttrib())) {
            return false;
        }

        // compare semantic structure (predecessors/successors)
        return compareDependencies(n1, n2, visitedNodes);
    }

    private static boolean compareBlockContents(Block block1, Block block2) {
        Body body1 = block1.getBody();
        Body body2 = block2.getBody();

        // get the unit chains (the list of instructions/statements in the body)
        Chain<Unit> units1 = body1.getUnits();
        Chain<Unit> units2 = body2.getUnits();

        // convert chains to iterators
        Iterator<Unit> block1Units = units1.iterator(block1.getHead(), block1.getTail());
        Iterator<Unit> block2Units = units2.iterator(block2.getHead(), block2.getTail());

        // cmp the contents of both blocks, unit by unit
        while (block1Units.hasNext() && block2Units.hasNext()) {
            Unit unit1 = block1Units.next();
            Unit unit2 = block2Units.next();

            // cmp the string representations of the units (you can refine this if necessary)
            if (!unit1.toString().equals(unit2.toString())) {
                return false; // Syntax differs
            }
        }

        // if one block has more units than the other, they are different
        if (block1Units.hasNext() || block2Units.hasNext()) {
            return false;
        }

        // if no differences were found, the blocks are considered equal
        return true;
    }


    private static boolean compareDependencies(PDGNode n1, PDGNode n2, Set<PDGNode> visitedNodes) {
        // cmp both back dependencies (control or data flow) and forward dependencies
        List<PDGNode> n1Dependents = n1.getDependents();
        List<PDGNode> n1BackDependents = n1.getBackDependets();
        List<PDGNode> n2Dependents = n2.getDependents();
        List<PDGNode> n2BackDependents = n2.getBackDependets();

        // cmp forward dependencies
        if (!compareNodeLists(n1Dependents, n2Dependents, visitedNodes)) {
            return false;
        }

        // cmp backward dependencies
        return compareNodeLists(n1BackDependents, n2BackDependents, visitedNodes);
    }

    private static boolean compareNodeLists(List<PDGNode> list1, List<PDGNode> list2, Set<PDGNode> visitedNodes) {
        // TODO simple comparison for now; this can be extended for more sophisticated checks
        if (list1.size() != list2.size()) {
            return false;
        }

        for (int i = 0; i < list1.size(); i++) {
            if (!nodesAreEqual(list1.get(i), list2.get(i), visitedNodes)) {
                return false;
            }
        }

        return true;
    }
}
