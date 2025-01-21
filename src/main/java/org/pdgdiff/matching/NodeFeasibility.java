package org.pdgdiff.matching;

import soot.toolkits.graph.pdg.PDGNode;

public class NodeFeasibility {
    public static boolean isSameNodeCategory(PDGNode n1, PDGNode n2) {
        // get unit
        Object node1 = n1.getNode();
        Object node2 = n2.getNode();

        // check for specific categories
        return (isStatement(node1) && isStatement(node2)) ||
                (isDeclaration(node1) && isDeclaration(node2)) ||
                (isControlFlowNode(node1) && isControlFlowNode(node2)) ||
                (isDataNode(node1) && isDataNode(node2));
    }

    private static boolean isStatement(Object node) {
        return node instanceof soot.jimple.Stmt;
    }

    private static boolean isDeclaration(Object node) {
        if (node instanceof soot.Value) {
            soot.Value value = (soot.Value) node;

            // check for local variables
            if (value instanceof soot.jimple.internal.JimpleLocal) {
                return true;
            }

            // check for field references
            return value instanceof soot.jimple.InstanceFieldRef || value instanceof soot.jimple.StaticFieldRef;
        }
        return false;
    }

    private static boolean isControlFlowNode(Object node) {
        return node instanceof soot.jimple.IfStmt || node instanceof soot.jimple.SwitchStmt;
    }

    private static boolean isDataNode(Object node) {
        return node instanceof soot.jimple.AssignStmt || node instanceof soot.jimple.ArrayRef;
    }
}
