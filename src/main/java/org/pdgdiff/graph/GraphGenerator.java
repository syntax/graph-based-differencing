package org.pdgdiff.graph;

import org.pdgdiff.graph.model.MyPDG;
import org.pdgdiff.graph.model.MyPDGNode;
import org.pdgdiff.graph.model.MyPDGNodeType;
import soot.Body;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.PDGNode;

import java.util.HashMap;
import java.util.Map;

public class GraphGenerator {

    public static MyPDG generatePDG(SootClass sootClass, SootMethod method) {
        try {
            Body body = method.retrieveActiveBody();
            UnitGraph unitGraph = new ExceptionalUnitGraph(body);
            HashMutablePDG sootPDG = new HashMutablePDG(unitGraph);

            // Convert Soot PDG to MyPDG
            MyPDG myPDG = new MyPDG();
            myPDG.setMethodSignature(method.getSignature());
            myPDG.setMethod(method);

            // Map to keep track of node correspondences
            Map<PDGNode, MyPDGNode> nodeMap = new HashMap<>();

            // Create MyPDGNodes
            for (PDGNode sootNode : sootPDG) {
                MyPDGNodeType type = MyPDGNodeType.valueOf(sootNode.getType().name());
                String attrib = sootNode.getAttrib() != null ? sootNode.getAttrib().toString() : "";

                // Extract line number if available
                int lineNumber = extractLineNumber(sootNode);

                MyPDGNode myNode = new MyPDGNode(sootNode.getNode(), type, attrib);
                myNode.setLineNumber(lineNumber);
                nodeMap.put(sootNode, myNode);
                myPDG.addNode(myNode);
            }

            // Set start node
            PDGNode sootStartNode = sootPDG.GetStartNode();
            MyPDGNode myStartNode = nodeMap.get(sootStartNode);
            myPDG.setStartNode(myStartNode);

            // Set dependents and back-dependents
            for (PDGNode sootNode : sootPDG) {
                MyPDGNode myNode = nodeMap.get(sootNode);

                for (PDGNode dep : sootNode.getDependents()) {
                    myNode.addDependent(nodeMap.get(dep));
                }

                for (PDGNode backDep : sootNode.getBackDependets()) {
                    myNode.addBackDependent(nodeMap.get(backDep));
                }
            }

            System.out.println("PDG for method " + method.getName() + " generated");

            return myPDG;
        } catch (Exception e) {
            System.err.println("Error generating PDG for method: " + method.getName());
            e.printStackTrace();
            return null;
        }
    }

    private static int extractLineNumber(PDGNode node) {
        Object nodeObj = node.getNode();
        if (nodeObj instanceof Unit) {
            Unit unit = (Unit) nodeObj;
            LineNumberTag tag = (LineNumberTag) unit.getTag("LineNumberTag");
            if (tag != null) {
                return tag.getLineNumber();
            }
        }
        return -1;
    }
}
