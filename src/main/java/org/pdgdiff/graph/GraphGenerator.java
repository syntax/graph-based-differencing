package org.pdgdiff.graph;

import soot.Body;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.*;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.MHGDominatorTree;
import soot.toolkits.graph.pdg.PDGNode;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.UnitValueBoxPair;

import java.util.HashMap;
import java.util.Map;

/**
 * GraphGenerator class to generate a Program Dependency Graph (PDG) for a specific method
 */
public class GraphGenerator {

    // enum for dependency types
    public enum DependencyTypes {
        CONTROL_DEPENDENCY,
        DATA_DEPENDENCY
    }
    // Method to generate PDG for a specific method
    public static HashMutablePDG generatePDG(SootClass sootClass, SootMethod method) {
        try {
            Body body = method.retrieveActiveBody();
            // TODO: invesigate optimisations applied a thtis point, probably.
            UnitGraph unitGraph = new ExceptionalUnitGraph(body);

            HashMutablePDG pdg = new HashMutablePDG(unitGraph);

            System.out.println("PDG for method " + method.getName() + " generated");

            return pdg;
        } catch (Exception e) {
            System.err.println("Error generating PDG for method: " + method.getName());
            e.printStackTrace();
            return null;
        }
    }

    public PDG constructPdg(SootClass sootClass, SootMethod method) {
        Body body = method.retrieveActiveBody();
        UnitGraph eug = new ExceptionalUnitGraph(body);

        //soot's api for creating postdominator tree
        MHGDominatorTree<Unit> postdominatorTree = new MHGDominatorTree(new MHGPostDominatorsFinder(eug));

        //get dominance frontiers based on the postdominator tree, equivalent to using it
        DominanceFrontier<Unit> dominanceFrontier = new CytronDominanceFrontier<>(postdominatorTree);
        PDG pdg = new PDG();
        pdg.setCFG(eug);

        SimpleLocalDefs definitions = new SimpleLocalDefs(eug);
        SimpleLocalUses uses = new SimpleLocalUses(body, definitions);

        Map<Unit, PDGNode> unitToNodeMap = new HashMap<>();


        for (Unit unit : body.getUnits()) {
            PDGNode node = addOrGetNode(pdg, unit, unitToNodeMap);

            //add control dependencies based on dominance frontier
            for (DominatorNode<Unit> dode : dominanceFrontier.getDominanceFrontierOf(postdominatorTree.getDode(unit))) {
                Unit frontier = dode.getGode();
                PDGNode frontierNode = addOrGetNode(pdg, frontier, unitToNodeMap);

                if (!pdg.containsEdge(frontierNode, node, DependencyTypes.CONTROL_DEPENDENCY)) {
                    pdg.addEdge(frontierNode, node, DependencyTypes.CONTROL_DEPENDENCY);
                }
            }

            // add data dependencies based on uses
            for (UnitValueBoxPair unitValueBoxPair : uses.getUsesOf(unit)) {
                Unit useUnit = unitValueBoxPair.unit;
                PDGNode useNode = addOrGetNode(pdg, useUnit, unitToNodeMap);

                if (!pdg.containsEdge(node, useNode, DependencyTypes.DATA_DEPENDENCY)) {
                    pdg.addEdge(node, useNode, DependencyTypes.DATA_DEPENDENCY);
                }
            }
        }

        return pdg;
    }

    private PDGNode addOrGetNode(PDG pdg, Unit unit, Map<Unit, PDGNode> unitToNodeMap) {
        PDGNode node = unitToNodeMap.get(unit);
        if (node == null) {
            // create a new PDGNode for this Unit
            node = new PDGNode(unit, PDGNode.Type.CFGNODE);
            unitToNodeMap.put(unit, node);

            // add the node to the PDG if it is not already there
            if (!pdg.containsNode(node)) {
                pdg.addNode(node);
            }
        }
        return node;
    }

}
