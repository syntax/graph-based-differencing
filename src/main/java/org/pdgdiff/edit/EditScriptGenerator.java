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

/**
 * Generates edit scripts based on PDG node mappings.
 */
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
                ComparisonResult compResult = nodesAreEqual(srcNode, dstNode, new HashSet<>());

                if (!compResult.isEqual) {
                    // syntax or semantic differences detected, generate appropriate update operation
                    // TODO: cut getAttrib or add more features to update class, not sure
                    if (!compResult.syntaxDifferences.isEmpty()) {
                        editScript.add(new Update(srcNode, srcNode.getAttrib().toString(), dstNode.getAttrib().toString(), compResult.syntaxDifferences));
                    }
                } else {
                    // if syntax is equal, check for moves (if connections change)
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

    private static class ComparisonResult {
        public boolean isEqual;
        public List<SyntaxDifference> syntaxDifferences;

        public ComparisonResult(boolean isEqual) {
            this.isEqual = isEqual;
            this.syntaxDifferences = new ArrayList<>();
        }

        public ComparisonResult(boolean isEqual, List<SyntaxDifference> syntaxDifferences) {
            this.isEqual = isEqual;
            this.syntaxDifferences = syntaxDifferences;
        }
    }

    private static ComparisonResult nodesAreEqual(PDGNode n1, PDGNode n2, Set<PDGNode> visitedNodes) {
        if (visitedNodes.contains(n1) || visitedNodes.contains(n2)) {
            return new ComparisonResult(true);
        }
        visitedNodes.add(n1);
        visitedNodes.add(n2);

        // compare basic properties
        if (!n1.getType().equals(n2.getType())) {
            return new ComparisonResult(false);
        }

        List<SyntaxDifference> syntaxDifferences = new ArrayList<>();

        if (n1.getType() == PDGNode.Type.CFGNODE) {
            // cmp CFGNODEs
            Block block1 = (Block) n1.getNode();
            Block block2 = (Block) n2.getNode();

            ComparisonResult blockCompResult = compareBlockContents(block1, block2);
            if (!blockCompResult.isEqual) {
                return blockCompResult;
            }
        } else if (n1.getType() == PDGNode.Type.REGION) {
            // cmp REGION nodes
            ComparisonResult regionCompResult = compareRegions(n1, n2, visitedNodes);
            if (!regionCompResult.isEqual) {
                return regionCompResult;
            }
        }

        // compare additional attributes (e.g., header/entry/loop conditions)
        if (!n1.getAttrib().equals(n2.getAttrib())) {
            syntaxDifferences.add(new SyntaxDifference(n1, n2));
            return new ComparisonResult(false, syntaxDifferences);
        }

        // compare dependencies (if needed)
        boolean dependenciesEqual = compareDependencies(n1, n2, visitedNodes);
        if (!dependenciesEqual) {
            syntaxDifferences.add(new SyntaxDifference(n1, n2));
            return new ComparisonResult(false, syntaxDifferences);
        }

        return new ComparisonResult(true);
    }

    private static ComparisonResult compareBlockContents(Block block1, Block block2) {
        List<SyntaxDifference> differences = new ArrayList<>();

        Body body1 = block1.getBody();
        Body body2 = block2.getBody();

        // get the unit chains (the list of instructions/statements in the body)
        Chain<Unit> units1 = body1.getUnits();
        Chain<Unit> units2 = body2.getUnits();

        // create lists of units from head to tail (inclusive)
        List<Unit> unitsList1 = collectUnits(units1, block1.getHead(), block1.getTail());
        List<Unit> unitsList2 = collectUnits(units2, block2.getHead(), block2.getTail());

        differences = compareUnitLists(unitsList1, unitsList2);

        if (!differences.isEmpty()) {
            return new ComparisonResult(false, differences);
        } else {
            return new ComparisonResult(true);
        }
    }

    private static List<Unit> collectUnits(Chain<Unit> unitsChain, Unit head, Unit tail) {
        List<Unit> unitsList = new ArrayList<>();
        Iterator<Unit> iterator = unitsChain.iterator(head, tail);

        while (iterator.hasNext()) {
            Unit unit = iterator.next();
            unitsList.add(unit);
            if (unit == tail) {
                break;
            }
        }

        return unitsList;
    }

    private static ComparisonResult compareRegions(PDGNode regionNode1, PDGNode regionNode2, Set<PDGNode> visitedNodes) {
        List<SyntaxDifference> differences = new ArrayList<>();

        IRegion region1 = (IRegion) regionNode1.getNode();
        IRegion region2 = (IRegion) regionNode2.getNode();

        // Get the units from each region
        List<Unit> units1 = region1.getUnits();
        List<Unit> units2 = region2.getUnits();

        // Compare units
        differences = compareUnitLists(units1, units2);

        if (!differences.isEmpty()) {
            return new ComparisonResult(false, differences);
        } else {
            return new ComparisonResult(true);
        }
    }

    private static List<SyntaxDifference> compareUnitLists(List<Unit> units1, List<Unit> units2) {
        List<SyntaxDifference> differences = new ArrayList<>();

        // remove any units that are not significant (e.g., labels, line numbers)
        // TODO: use line numbers to roll abck to src
        List<Unit> filteredUnits1 = filterUnits(units1);
        List<Unit> filteredUnits2 = filterUnits(units2);

        if (filteredUnits1.size() != filteredUnits2.size()) {
            differences.add(new SyntaxDifference("Region unit counts differ"));
            return differences;
        }

        for (int i = 0; i < filteredUnits1.size(); i++) {
            Unit unit1 = filteredUnits1.get(i);
            Unit unit2 = filteredUnits2.get(i);

            if (!unit1.toString().equals(unit2.toString())) {
                differences.add(new SyntaxDifference(unit1, unit2));
            }
        }

        return differences;
    }

    private static List<Unit> filterUnits(List<Unit> units) {
        List<Unit> filteredUnits = new ArrayList<>();
        for (Unit unit : units) {
            // TODO: filter out units that are not significant, doing all atm
            filteredUnits.add(unit);
        }
        return filteredUnits;
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

        // sort the lists to ensure consistent order
        // time complexity overhead but im scared of non-determinism lol!
        List<PDGNode> sortedList1 = new ArrayList<>(list1);
        List<PDGNode> sortedList2 = new ArrayList<>(list2);

        sortedList1.sort(Comparator.comparingInt(Object::hashCode));
        sortedList2.sort(Comparator.comparingInt(Object::hashCode));

        for (int i = 0; i < sortedList1.size(); i++) {
            PDGNode node1 = sortedList1.get(i);
            PDGNode node2 = sortedList2.get(i);

            ComparisonResult compResult = nodesAreEqual(node1, node2, visitedNodes);
            if (!compResult.isEqual) {
                return false;
            }
        }

        return true;
    }
}
