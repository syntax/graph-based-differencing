package org.pdgdiff.graph;

import soot.Body;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.toolkits.graph.*;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.MHGDominatorTree;
import soot.toolkits.graph.pdg.PDGNode;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.UnitValueBoxPair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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


    public static PDG constructPdg(SootClass sootClass, SootMethod method) {
        Body body = method.retrieveActiveBody();
        System.out.println("Generating PDG for method: " + method.getName());
        UnitGraph eug = new ExceptionalUnitGraph(body);

        // soots api for creating postdominator tree
        MHGDominatorTree<Unit> postdominatorTree = new MHGDominatorTree(new MHGPostDominatorsFinder(eug));

        //get dominance frontiers based on the postdominator tree, equivalent to using it
        DominanceFrontier<Unit> dominanceFrontier = new CytronDominanceFrontier<>(postdominatorTree);

        PDG pdg = new PDG();
        pdg.setCFG(eug);

        SimpleLocalDefs definitions = new SimpleLocalDefs(eug);
        SimpleLocalUses uses = new SimpleLocalUses(body, definitions);

        Map<Unit, PDGNode> unitToNodeMap = new HashMap<>();

        PDGNode startNode = null;

        // building a list of units in the order they appear to properly detect the backedges
        List<Unit> allUnits = new ArrayList<>(body.getUnits());
        Map<Unit, Integer> unitOrder = new HashMap<>();
        int idx = 0;
        for (Unit u : allUnits) {
            unitOrder.put(u, idx++);
        }

        for (Unit unit : body.getUnits()) {

            PDGNode node = addOrGetNode(pdg, unit, unitToNodeMap);

            //add control dependencies based on dominance frontier
            for (DominatorNode<Unit> dode : dominanceFrontier.getDominanceFrontierOf(postdominatorTree.getDode(unit))) {
                Unit frontier = dode.getGode();
                PDGNode frontierNode = addOrGetNode(pdg, frontier, unitToNodeMap);

                if (!pdg.containsEdge(frontierNode, node, DependencyTypes.CONTROL_DEPENDENCY)) {
                    // TODO: this isnt probably bang on, but need some 'start node' to be set. taking the first unit often leads to disconnected graphs
                    if (startNode == null) {
                        startNode = frontierNode;
                        pdg.startNode = startNode;
                    }
                    pdg.addEdge(frontierNode, node, DependencyTypes.CONTROL_DEPENDENCY);
                    frontierNode.addDependent(node);

                    System.out.println("Control Dependency: " + frontierNode + " -> " + node);

                    // preliminary but if fronteid is earlier or equal to 'unit', treat it as a backedge
                    if (isBackEdge(frontier, unit, unitOrder)) {
                        node.addBackDependent(frontierNode);
                        System.out.println("  => (Back edge) " + node + " <- " + frontierNode);
                    }
                }
            }

            // add data dependencies based on uses
            for (UnitValueBoxPair unitValueBoxPair : uses.getUsesOf(unit)) {
                Unit useUnit = unitValueBoxPair.unit;
                PDGNode useNode = addOrGetNode(pdg, useUnit, unitToNodeMap);

                if (!pdg.containsEdge(node, useNode, DependencyTypes.DATA_DEPENDENCY)) {
                    // TODO: this isnt probably bang on, but need some 'start node' to be set. taking the first unit often leads to disconnected graphs
                    if (startNode == null) {
                        startNode = node;
                        pdg.startNode = startNode;
                    }
                    pdg.addEdge(node, useNode, DependencyTypes.DATA_DEPENDENCY);
                    node.addDependent(useNode);
                    System.out.println("Data Dependency: " + node + " -> " + useNode);

                    // preliminary but if 'useUnit' is earlier or equal to 'unit', treat it as a backedge
                    if (isBackEdge(useUnit, unit, unitOrder)) {
                        useNode.addBackDependent(node);
                        System.out.println("  => (Data Back edge) " + useNode + " <- " + node);
                    }
                }
            }
        }
        return pdg;
    }

    private static boolean isBackEdge(Unit frontier, Unit unit, Map<Unit, Integer> orderMap) {
        // return true if 'frontier' is same or earlier in the ordering => likely a backward/loop edge.
        return orderMap.get(frontier) <= orderMap.get(unit);
    }

    private static PDGNode addOrGetNode(PDG pdg, Unit unit, Map<Unit, PDGNode> unitToNodeMap) {
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
