package org.pdgdiff.graph;

import soot.Body;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.*;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.MHGDominatorTree;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.UnitValueBoxPair;

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

    public org.pdgdiff.graph.HashMutablePDG constructPdg(ExceptionalUnitGraph eug) {
        Body body = eug.getBody();

        //soot's api for creating postdominator tree
        MHGDominatorTree<Unit> postdominatorTree = new MHGDominatorTree(new MHGPostDominatorsFinder(eug));

        //get dominance frontiers based on the postdominator tree, equivalent to using it
        DominanceFrontier<Unit> dominanceFrontier = new CytronDominanceFrontier<Unit>(postdominatorTree);
        org.pdgdiff.graph.HashMutablePDG pdg = new org.pdgdiff.graph.HashMutablePDG();
        pdg.setCFG(eug);
        SimpleLocalDefs definitions = new SimpleLocalDefs(eug);
        SimpleLocalUses uses = new SimpleLocalUses(body, definitions);


        for (Unit unit : body.getUnits()) {
            addNode(pdg, unit);

            for (DominatorNode<Unit> dode : dominanceFrontier.getDominanceFrontierOf(postdominatorTree.getDode(unit))) {
                Unit frontier = dode.getGode();
                addNode(pdg, frontier);

                if (pdg.containsEdge(frontier, unit, DependencyTypes.CONTROL_DEPENDENCY)) {
                    continue;
                }
                pdg.addEdge(frontier, unit, DependencyTypes.CONTROL_DEPENDENCY);

            }
            for (UnitValueBoxPair unitValueBoxPair : uses.getUsesOf(unit)) {
                Unit useNode = unitValueBoxPair.unit;
                addNode(pdg, useNode);
                if (pdg.containsEdge(unit, useNode, DependencyTypes.DATA_DEPENDENCY)) {
                    continue;
                }
                pdg.addEdge(unit, unitValueBoxPair.unit, DependencyTypes.DATA_DEPENDENCY);

            }
        }
        return pdg;
    }
}
