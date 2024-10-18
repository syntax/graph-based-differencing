package org.pdgdiff.edit;

import org.pdgdiff.edit.model.*;
import org.pdgdiff.matching.GraphMapping;
import org.pdgdiff.matching.NodeMapping;
import org.pdgdiff.util.SourceCodeMapper;
import soot.Unit;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.PDGNode;

import java.io.IOException;
import java.util.*;

/**
 * Generates edit scripts based on PDG node mappings.
 */
public class EditScriptGenerator {

    /**
     * Generates a list of edit operations required to transform srcPDG into dstPDG based on the provided node mappings.
     *
     * @param srcPDG            Source PDG
     * @param dstPDG            Target PDG
     * @param graphMapping      The mapping between PDGs and their node mappings
     * @param srcSourceFilePath Path to the source file of the srcPDG
     * @param dstSourceFilePath Path to the source file of the dstPDG
     * @return List of EditOperation objects representing the edit script
     * @throws IOException If an I/O error occurs
     */
    public static List<EditOperation> generateEditScript(
            HashMutablePDG srcPDG,
            HashMutablePDG dstPDG,
            GraphMapping graphMapping,
            String srcSourceFilePath,
            String dstSourceFilePath
    ) throws IOException {
        List<EditOperation> editScript = new ArrayList<>();

        // Initialize source code mappers
        SourceCodeMapper srcCodeMapper = new SourceCodeMapper(srcSourceFilePath);
        SourceCodeMapper dstCodeMapper = new SourceCodeMapper(dstSourceFilePath);

        NodeMapping nodeMapping = graphMapping.getNodeMapping(srcPDG);
        if (nodeMapping == null) {
            // No mapping exists; delete all nodes from srcPDG and insert all nodes from dstPDG
            for (PDGNode node : srcPDG) {
                int lineNumber = getNodeLineNumber(node);
                String codeSnippet = srcCodeMapper.getCodeLine(lineNumber);
                editScript.add(new Delete(node, lineNumber, codeSnippet));
            }
            for (PDGNode node : dstPDG) {
                int lineNumber = getNodeLineNumber(node);
                String codeSnippet = dstCodeMapper.getCodeLine(lineNumber);
                editScript.add(new Insert(node, lineNumber, codeSnippet));
            }
            return editScript;
        }

        Map<PDGNode, PDGNode> mappings = nodeMapping.getNodeMapping();
        Set<PDGNode> srcNodesMapped = mappings.keySet();
        Set<PDGNode> dstNodesMapped = new HashSet<>(mappings.values());

        // Handle deletions
        for (PDGNode srcNode : srcPDG) {
            if (!srcNodesMapped.contains(srcNode)) {
                int lineNumber = getNodeLineNumber(srcNode);
                String codeSnippet = srcCodeMapper.getCodeLine(lineNumber);
                editScript.add(new Delete(srcNode, lineNumber, codeSnippet));
            }
        }

        // Handle insertions
        for (PDGNode dstNode : dstPDG) {
            if (!dstNodesMapped.contains(dstNode)) {
                int lineNumber = getNodeLineNumber(dstNode);
                String codeSnippet = dstCodeMapper.getCodeLine(lineNumber);
                editScript.add(new Insert(dstNode, lineNumber, codeSnippet));
            }
        }

        // Handle updates
        Set<PDGNode> visitedNodes = new HashSet<>();
        for (PDGNode srcNode : srcNodesMapped) {
            PDGNode dstNode = mappings.get(srcNode);

            if (!visitedNodes.contains(srcNode)) {
                ComparisonResult compResult = nodesAreEqual(srcNode, dstNode, visitedNodes, srcCodeMapper, dstCodeMapper);

                if (!compResult.isEqual) {
                    if (!compResult.syntaxDifferences.isEmpty()) {
                        for (SyntaxDifference syntaxDiff : compResult.syntaxDifferences) {
                            int oldLineNumber = syntaxDiff.getOldLineNumber();
                            int newLineNumber = syntaxDiff.getNewLineNumber();
                            String oldCodeSnippet = syntaxDiff.getOldCodeSnippet();
                            String newCodeSnippet = syntaxDiff.getNewCodeSnippet();
                            PDGNode node = srcNode; // Or syntaxDiff.getOldNode()
                            editScript.add(new Update(node, oldLineNumber, newLineNumber, oldCodeSnippet, newCodeSnippet, syntaxDiff));
                        }
                    }
                }
            }
        }

        return editScript;
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

    private static ComparisonResult nodesAreEqual(PDGNode n1, PDGNode n2, Set<PDGNode> visitedNodes,
                                                  SourceCodeMapper srcCodeMapper, SourceCodeMapper dstCodeMapper) {
        if (visitedNodes.contains(n1) || visitedNodes.contains(n2)) {
            return new ComparisonResult(true);
        }
        visitedNodes.add(n1);
        visitedNodes.add(n2);

        // Compare basic properties
        if (!n1.getType().equals(n2.getType())) {
            return new ComparisonResult(false);
        }

        List<SyntaxDifference> syntaxDifferences = new ArrayList<>();

        if (n1.getType() == PDGNode.Type.CFGNODE) {
            // Compare CFG nodes
            ComparisonResult blockCompResult = compareCFGNodes(n1, n2, srcCodeMapper, dstCodeMapper);
            if (!blockCompResult.isEqual) {
                return blockCompResult;
            }
        } else if (n1.getType() == PDGNode.Type.REGION) {
            // Optionally ignore or handle Region nodes differently
            return new ComparisonResult(true);
        }

        return new ComparisonResult(true);
    }

    private static ComparisonResult compareCFGNodes(PDGNode n1, PDGNode n2,
                                                    SourceCodeMapper srcCodeMapper, SourceCodeMapper dstCodeMapper) {
        Block block1 = (Block) n1.getNode();
        Block block2 = (Block) n2.getNode();

        List<SyntaxDifference> differences = new ArrayList<>();

        // Get units (statements) from each block
        List<Unit> units1 = collectUnits(block1);
        List<Unit> units2 = collectUnits(block2);

        // Compare the units
        differences = compareUnitLists(units1, units2, srcCodeMapper, dstCodeMapper);

        if (!differences.isEmpty()) {
            return new ComparisonResult(false, differences);
        } else {
            return new ComparisonResult(true);
        }
    }

    private static List<Unit> collectUnits(Block block) {
        List<Unit> unitsList = new ArrayList<>();
        Iterator<Unit> iterator = block.iterator();

        while (iterator.hasNext()) {
            Unit unit = iterator.next();
            unitsList.add(unit);
        }

        return unitsList;
    }

    private static List<SyntaxDifference> compareUnitLists(List<Unit> units1, List<Unit> units2,
                                                           SourceCodeMapper srcCodeMapper, SourceCodeMapper dstCodeMapper) {
        List<SyntaxDifference> differences = new ArrayList<>();
        Set<String> processedDifferences = new HashSet<>();

        int i = 0, j = 0;
        while (i < units1.size() && j < units2.size()) {
            Unit unit1 = units1.get(i);
            Unit unit2 = units2.get(j);

            if (unitsAreEqual(unit1, unit2)) {
                i++;
                j++;
            } else {
                SyntaxDifference diff = new SyntaxDifference(unit1, unit2, srcCodeMapper, dstCodeMapper);
                String diffKey = diff.getOldLineNumber() + "_" + diff.getNewLineNumber() + "_" + diff.getOldCodeSnippet() + "_" + diff.getNewCodeSnippet();
                if (!processedDifferences.contains(diffKey)) {
                    differences.add(diff);
                    processedDifferences.add(diffKey);
                }
                i++;
                j++;
            }
        }

        // Handle remaining units in units1 (deletions)
        while (i < units1.size()) {
            SyntaxDifference diff = new SyntaxDifference(units1.get(i), null, srcCodeMapper, dstCodeMapper);
            String diffKey = diff.getOldLineNumber() + "_-1_" + diff.getOldCodeSnippet() + "_null";
            if (!processedDifferences.contains(diffKey)) {
                differences.add(diff);
                processedDifferences.add(diffKey);
            }
            i++;
        }

        // Handle remaining units in units2 (insertions)
        while (j < units2.size()) {
            SyntaxDifference diff = new SyntaxDifference(null, units2.get(j), srcCodeMapper, dstCodeMapper);
            String diffKey = "-1_" + diff.getNewLineNumber() + "_null_" + diff.getNewCodeSnippet();
            if (!processedDifferences.contains(diffKey)) {
                differences.add(diff);
                processedDifferences.add(diffKey);
            }
            j++;
        }

        return differences;
    }

    private static boolean unitsAreEqual(Unit unit1, Unit unit2) {
        if (unit1 == null || unit2 == null) {
            return false;
        }

        // Compare the string representations
        return unit1.toString().equals(unit2.toString());
    }

    private static int getNodeLineNumber(PDGNode node) {
        if (node.getType() == PDGNode.Type.CFGNODE) {
            Block block = (Block) node.getNode();
            Unit headUnit = block.getHead();
            return getLineNumber(headUnit);
        }
        return -1; // Line number not available
    }

    private static int getLineNumber(Unit unit) {
        if (unit == null) {
            return -1;
        }
        LineNumberTag tag = (LineNumberTag) unit.getTag("LineNumberTag");
        if (tag != null) {
            return tag.getLineNumber();
        }
        return -1;
    }
}
