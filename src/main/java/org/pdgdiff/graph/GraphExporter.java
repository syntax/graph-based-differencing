package org.pdgdiff.graph;

import soot.SootMethod;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.pdg.PDGNode;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GraphExporter {

    public static void clearOutputFolder(String folderPath) {
        File outputFolder = new File(folderPath);
        if (outputFolder.exists()) {
            File[] files = outputFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
    }


    public static void exportPDG(PDG pdg, String dotFileName, String txtFileName) throws IOException {
        UnitGraph cfg = pdg.getCFG();
        SootMethod method = (cfg != null) ? cfg.getBody().getMethod() : null;

        exportPDGToDot(pdg, dotFileName);

        exportPDGToFile(pdg, txtFileName, method.getName());
    }

    public static void exportPDGToFile(PDG pdg, String fileName, String methodName) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName, true))) {
            writer.println("\n\n---------> Method: " + methodName);
            // dump text repr, toString might be overridden in PDG need to check
            writer.println(pdg.toString());
            writer.println("---------> End of PDG for method: " + methodName + "\n\n");
        }
    }

    public static void exportPDGToDot(PDG pdg, String fileName) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.println("digraph PDG {");
            writer.println("  graph [ranksep=2, nodesep=0.1];");
            writer.println("  node [shape=ellipse, style=filled, fillcolor=lightgrey, fontname=Arial, fontsize=12];");
            writer.println("  edge [fontname=Arial, fontsize=10];");

            Set<PDGNode> connectedNodes = new HashSet<>();


           // for each node, print out edges to its successors
            for (PDGNode src : pdg) {
                List<PDGNode> successors = pdg.getSuccsOf(src);
                for (PDGNode tgt : successors) {
                    // todo getLabelsForEdges(...) returns a List<DependencyTypes> which can contain multiple edge labels
                    List<GraphGenerator.DependencyTypes> labels = pdg.getLabelsForEdges(src, tgt);
                    for (GraphGenerator.DependencyTypes depType : labels) {
                        String colour = "black";
                        String depLabel = "UNKNOWN";
                        if (depType == GraphGenerator.DependencyTypes.CONTROL_DEPENDENCY) {
                            colour = "red";
                            depLabel = "CTRL_DEP";
                        } else if (depType == GraphGenerator.DependencyTypes.DATA_DEPENDENCY) {
                            colour = "blue";
                            depLabel = "DATA_DEP";
                        }
                        writer.printf("  %s -> %s [label=\"%s\", color=\"%s\"];\n",
                                getNodeId(src),
                                getNodeId(tgt),
                                depLabel,
                                colour);
                        connectedNodes.add(src);
                        connectedNodes.add(tgt);
                    }
                }
            }

            for (PDGNode node : connectedNodes) {
                String label = escapeSpecialCharacters(removeCFGNodePrefix(node.toString()));
                writer.printf("  %s [label=\"%s\"];%n", getNodeId(node), label);
            }

            writer.println("}");
            System.out.println("PDG exported to DOT file: " + fileName);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // helper methods

    private static String getNodeId(PDGNode node) {
        return "node_" + System.identityHashCode(node);
    }

    private static String removeCFGNodePrefix(String label) {
        String prefix = "Type: CFGNODE: ";
        if (label.startsWith(prefix)) {
            return label.substring(prefix.length());
        }
        return label;
    }

    // to avoid parse errors, otherwise print("") could ruin some things
    private static String escapeSpecialCharacters(String label) {
        return label.replace("\"", "\\\"");
    }
}
