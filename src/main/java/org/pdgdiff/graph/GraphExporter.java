package org.pdgdiff.graph;

import soot.SootMethod;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.pdg.PDGNode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

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

            // print all pdg nodes
            for (PDGNode node : pdg) {
                // todo; as of right now this will print every nodes type which is a CFGNode, this is same for all so usless info
                String nodeId = getNodeId(node);
                String label = escapeSpecialCharacters(node.toString());
                writer.printf("  %s [label=\"%s\"];\n", nodeId, label);
            }

           // for each node, print out edges to its successors
            for (PDGNode src : pdg) {
                List<PDGNode> successors = pdg.getSuccsOf(src);
                for (PDGNode tgt : successors) {
                    // todo getLabelsForEdges(...) returns a List<DependencyTypes> which can contain multiple edge labels
                    List<GraphGenerator.DependencyTypes> labels = pdg.getLabelsForEdges(src, tgt);
                    if (labels != null) {
                        for (GraphGenerator.DependencyTypes depType : labels) {
                            writer.printf("  %s -> %s [label=\"%s\"];\n",
                                    getNodeId(src),
                                    getNodeId(tgt),
                                    depType);
                        }
                    }
                }
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

    // to avoid parse errors, otherwise print("") could ruin some things
    private static String escapeSpecialCharacters(String label) {
        return label.replace("\"", "\\\"");
    }
}
