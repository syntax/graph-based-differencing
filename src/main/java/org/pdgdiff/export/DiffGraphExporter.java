package org.pdgdiff.export;

import org.pdgdiff.graph.GraphGenerator;
import org.pdgdiff.graph.PDG;
import org.pdgdiff.matching.GraphMapping;
import org.pdgdiff.matching.NodeMapping;
import soot.Unit;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.pdg.PDGNode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class DiffGraphExporter {

    /**
     * This generated a 'delta' dot file, i.e. a way of representing the changes that have happeend on one graph and
     * taken it to another
     *
     * @param graphMapping the PDG-to-PDG matching (this incldues node mappings)
     * @param pdgListSrc   all PDGs from the 'before' version
     * @param pdgListDst   all PDGs from the 'after' version
        * @param outputDir    the directory to write the dot files to
     */
    public static void exportDiffPDGs(
            GraphMapping graphMapping,
            List<PDG> pdgListSrc,
            List<PDG> pdgListDst,
            String outputDir
    ) {
        File outDir = new File(outputDir);
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        // one pdg diff's dot file for each matched pair
        Map<PDG, PDG> matchedPairs = graphMapping.getGraphMapping();
        for (Map.Entry<PDG, PDG> entry : matchedPairs.entrySet()) {
            PDG srcPDG = entry.getKey();
            PDG dstPDG = entry.getValue();
            NodeMapping nodeMapping = graphMapping.getNodeMapping(srcPDG);

            String srcMethod = (srcPDG.getCFG() != null)
                    ? srcPDG.getCFG().getBody().getMethod().getName()
                    : "UnknownSrcMethod";
            String dstMethod = (dstPDG.getCFG() != null)
                    ? dstPDG.getCFG().getBody().getMethod().getName()
                    : "UnknownDstMethod";

            String dotFileName = "diff_" + srcMethod + "_TO_" + dstMethod + ".dot";
            File dotFile = new File(outDir, dotFileName);

            exportSingleDiffPDG(srcPDG, dstPDG, nodeMapping, dotFile);
        }

        // identify unmatched PDGs in source vs. destination
        List<PDG> unmatchedInSrc = pdgListSrc.stream()
                .filter(pdg -> !matchedPairs.containsKey(pdg))
                .collect(Collectors.toList());
        List<PDG> unmatchedInDst = pdgListDst.stream()
                .filter(pdg -> !matchedPairs.containsValue(pdg))
                .collect(Collectors.toList());

        // todo handle this, maybe just print entire graph in red or green lol
        for (PDG pdg : unmatchedInSrc) {
            // handle delete PDG (e.g., entire method removed)
            // ...
        }
        for (PDG pdg : unmatchedInDst) {
            // handle add PDG (e.g., entire method added)
            // ...
        }
    }

    /**
     * exprts a single .dot file showing the diff between one src PDG and one dst PDG
     * (uses the node mapping to color-code matched/added/deleted edges & nodes).
     *
     * This aims to follow similar logic to Editscriptgeneration
     */
    private static void exportSingleDiffPDG(
            PDG srcPDG,
            PDG dstPDG,
            NodeMapping nodeMapping,
            File outputDotFile
    ) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputDotFile))) {
            writer.println("digraph PDG_DIFF {");
            writer.println("  rankdir=TB;");
            writer.println("  node [shape=box, style=filled, fontname=Arial];");
            writer.println("  edge [fontname=Arial];");

            Map<PDGNode, PDGNode> srcToDst = nodeMapping.getNodeMapping();
            Map<PDGNode, PDGNode> dstToSrc = nodeMapping.getReverseNodeMapping();

            Set<PDGNode> srcNodes = new HashSet<>();
            srcPDG.iterator().forEachRemaining(srcNodes::add);
            Set<PDGNode> dstNodes = new HashSet<>();
            dstPDG.iterator().forEachRemaining(dstNodes::add);

            // map to store node details (label and color) keyed by their dot id.
            Map<String, NodeData> nodeDataMap = new HashMap<>();

            // Process nodes from source PDG (matched or deleted nodes)
            for (PDGNode srcNode : srcNodes) {
                PDGNode dstNode = srcToDst.get(srcNode);
                String nodeId = getNodeId(srcNode, true);

                if (dstNode == null) {
                    // node was deleted in dst
                    String label = removePrefix(srcNode.toString());
                    String color = "#FFCCCC"; // red for deletion
                    nodeDataMap.put(nodeId, new NodeData(createNodeLabel(label, srcNode), color));
                } else {
                    // matched (possible unchanged, moved, or updated)
//                    boolean changed = nodeContentChanged(srcNode, dstNode);
                    String label, color;
                    // label shows both sides
                    if (Objects.equals(removePrefix(srcNode.toString()), removePrefix(dstNode.toString()))) {
                        label = removePrefix(srcNode.toString());
                        color = "lightgrey"; // grey for unchanged
                    } else {
                        label = String.format("%s!NEWLINE!----!NEWLINE!%s",
                                removePrefix(srcNode.toString()),
                                removePrefix(dstNode.toString()));
                        color = "#FFCC99"; // orange for update
                    }
                    nodeDataMap.put(nodeId, new NodeData(createNodeLabel(label, srcNode, dstNode), color));
                }
            }

            // processing nodes added in destination
            for (PDGNode dstNode : dstNodes) {
                if (!dstToSrc.containsKey(dstNode)) {
                    String nodeId = getNodeId(dstNode, false);
                    String label = removePrefix(dstNode.toString());
                    String color = "#CCFFCC"; // green-ish for addition
                    nodeDataMap.put(nodeId, new NodeData(createNodeLabel(label, dstNode), color));
                }
            }

            // process edges and record dependency labels
            Map<EdgeKey, Set<String>> edgeMap = new HashMap<>();
            Set<String> connectedNodeIds = new HashSet<>();

            // process edges from the source PDG
            for (PDGNode srcNode : srcNodes) {
                for (PDGNode succ : srcPDG.getSuccsOf(srcNode)) {
                    String srcId = getMergedNodeId(srcNode, true, srcToDst);
                    String tgtId = getMergedNodeId(succ, true, srcToDst);
                    EdgeKey key = new EdgeKey(srcId, tgtId);

                    // get dependency types for the edge in srcPDG.
                    List<GraphGenerator.DependencyTypes> depTypes = srcPDG.getEdgeLabels(srcNode, succ);
                    String depLabel = depTypes.stream()
                            .map(DiffGraphExporter::mapDependencyType)
                            .collect(Collectors.joining(","));
                    edgeMap.computeIfAbsent(key, k -> new HashSet<>()).add("src:" + depLabel);

                    connectedNodeIds.add(srcId);
                    connectedNodeIds.add(tgtId);
                }
            }

            // process edges from the destination PDG.
            for (PDGNode dstNode : dstNodes) {
                for (PDGNode succ : dstPDG.getSuccsOf(dstNode)) {
                    String srcId = getMergedNodeId(dstNode, false, dstToSrc);
                    String tgtId = getMergedNodeId(succ, false, dstToSrc);
                    EdgeKey key = new EdgeKey(srcId, tgtId);

                    List<GraphGenerator.DependencyTypes> depTypes = dstPDG.getEdgeLabels(dstNode, succ);
                    String depLabel = depTypes.stream()
                            .map(DiffGraphExporter::mapDependencyType)
                            .collect(Collectors.joining(","));
                    edgeMap.computeIfAbsent(key, k -> new HashSet<>()).add("dst:" + depLabel);

                    connectedNodeIds.add(srcId);
                    connectedNodeIds.add(tgtId);
                }
            }

            for (String nodeId : connectedNodeIds) {
                NodeData data = nodeDataMap.get(nodeId);
                if (data != null) {
                    writer.printf("  %s [label=%s, fillcolor=\"%s\"];%n",
                            nodeId, data.label, data.color);
                }
            }

            // write edges with colour and a combined label
            for (Map.Entry<EdgeKey, Set<String>> entry : edgeMap.entrySet()) {
                EdgeKey key = entry.getKey();
                Set<String> sources = entry.getValue();
                String color;
                if (sources.stream().anyMatch(s -> s.startsWith("src:"))
                        && sources.stream().anyMatch(s -> s.startsWith("dst:"))) {
                    color = "black";
                } else if (sources.stream().anyMatch(s -> s.startsWith("src:"))) {
                    color = "red";
                } else {
                    color = "green";
                }
                String edgeLabel = sources.stream()
                        .map(s -> s.substring(4))
                        .distinct()
                        .collect(Collectors.joining("/"));
                writer.printf("  %s -> %s [color=%s, label=\"%s\"];%n",
                        key.srcId, key.tgtId, color, edgeLabel);
            }

            writer.println("}");
            System.out.println("Created PDG diff: " + outputDotFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // this is overloaded, depending on udpate or single-line number operation
    private static String createNodeLabel(String originalLabel, PDGNode node) {
        return createNodeLabel(originalLabel, node, null);
    }


    private static String createNodeLabel(String originalLabel, PDGNode node1, PDGNode node2) {
        int lineNum = getNodeLineNumber(node1);
        int lineNum2 = -1;
        if (node2 != null) {
            lineNum2 = getNodeLineNumber(node2);
        }
        String safeLabel = escape(originalLabel);

        StringBuilder sb = new StringBuilder();
        sb.append("<");
        sb.append("<b>").append(safeLabel).append("</b>");
        if (lineNum != -1 && lineNum2 == -1) {
            sb.append("<br/>")
                    .append("<font point-size=\"10\" color=\"gray\">")
                    .append("Line: ").append(lineNum)
                    .append("</font>");
        } else if(lineNum != -1) {
            sb.append("<br/>")
                    .append("<font point-size=\"10\" color=\"gray\">")
                    .append("Line: ").append(lineNum)
                    .append(" -&gt; ")
                    .append("Line: ").append(lineNum2)
                    .append("</font>");
        }

        sb.append(">");
        return sb.toString();
    }

    // helper classes and methods

    private static int getNodeLineNumber(PDGNode node) {
        if (node.getType() == PDGNode.Type.CFGNODE) {
            Object underlying = node.getNode();
            if (underlying instanceof Unit) {
                Unit unit = (Unit) underlying;
                LineNumberTag tag = (LineNumberTag) unit.getTag("LineNumberTag");
                if (tag != null) {
                    return tag.getLineNumber();
                }
            }
        }
        return -1;
    }

    private static class NodeData {
        String label;
        String color;
        NodeData(String label, String color) {
            this.label = label;
            this.color = color;
        }
    }

    private static String mapDependencyType(GraphGenerator.DependencyTypes depType) {
        if (depType == GraphGenerator.DependencyTypes.CONTROL_DEPENDENCY) {
            return "CTRL_DEP";
        } else if (depType == GraphGenerator.DependencyTypes.DATA_DEPENDENCY) {
            return "DATA_DEP";
        } else {
            return "UNKNOWN";
        }
    }

    // generates a node ID
    private static String getNodeId(PDGNode node, boolean isSrc) {
        String prefix = isSrc ? "SRC_" : "DST_";
        return prefix + System.identityHashCode(node);
    }

    // generates a node ID for merged nodes
    private static String getMergedNodeId(PDGNode node, boolean isSourceNode, Map<PDGNode, PDGNode> mapping) {
        PDGNode mappedNode = mapping.get(node);
        if (mappedNode != null) {
            if (isSourceNode) {
                return getNodeId(node, true);
            } else {
                return getNodeId(mappedNode, true);
            }
        } else {
            return getNodeId(node, isSourceNode);
        }
    }

    // removes the prefix from a node label
    private static String removePrefix(String label) {
        String prefix = "Type: CFGNODE: ";
        return label.startsWith(prefix) ? label.substring(prefix.length()) : label;
    }

    // for dot formatting
    private static String escape(String text) {
        return text.replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "\\\"")
                .replace("!NEWLINE!", "<br/>");

    }

    private static class EdgeKey {
        final String srcId;
        final String tgtId;

        EdgeKey(String srcId, String tgtId) {
            this.srcId = srcId;
            this.tgtId = tgtId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EdgeKey edgeKey = (EdgeKey) o;
            return Objects.equals(srcId, edgeKey.srcId) && Objects.equals(tgtId, edgeKey.tgtId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(srcId, tgtId);
        }
    }
}
