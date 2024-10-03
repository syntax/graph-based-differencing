package org.pdgdiff.client;

import soot.Body;
import soot.SootClass;
import soot.SootMethod;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.pdg.HashMutablePDG;

/**
 * GraphGenerator class to generate a Program Dependency Graph (PDG) for a specific method
 */
public class GraphGenerator {

    // Method to generate PDG for a specific method
    public static HashMutablePDG generatePDG(SootClass sootClass, SootMethod method) {
        try {
            Body body = method.retrieveActiveBody();
            // TODO: invesigate optimisations applied a thtis point, probably.
            UnitGraph unitGraph = new ExceptionalUnitGraph(body);

            // Generate a Program Dependency Graph (PDG)
            HashMutablePDG pdg = new HashMutablePDG(unitGraph);

            System.out.println("PDG for method " + method.getName() + " generated");

            // Optionally: traverse or export the PDG if needed
            // TODO condsider.
            return pdg;
        } catch (Exception e) {
            System.err.println("Error generating PDG for method: " + method.getName());
            e.printStackTrace();
            return null;
        }
    }
}
