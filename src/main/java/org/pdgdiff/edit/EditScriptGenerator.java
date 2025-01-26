package org.pdgdiff.edit;

import org.pdgdiff.edit.model.*;
import org.pdgdiff.graph.PDG;
import org.pdgdiff.matching.GraphMapping;
import org.pdgdiff.matching.NodeMapping;
import org.pdgdiff.util.CodeAnalysisUtils;
import org.pdgdiff.util.SourceCodeMapper;
import soot.Modifier;
import soot.SootMethod;
import soot.Unit;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.pdg.IRegion;
import soot.toolkits.graph.pdg.PDGNode;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.pdgdiff.edit.SignatureDiffGenerator.ParsedSignature;

import static java.util.Collections.max;
import static java.util.Collections.min;
import static org.pdgdiff.edit.SignatureDiffGenerator.compareSignatures;
import static org.pdgdiff.edit.SignatureDiffGenerator.parseMethodSignature;
import static org.pdgdiff.graph.GraphTraversal.collectNodesBFS;

/**
 * Generates edit scripts based on PDG node mappings.
 */
public class EditScriptGenerator {

    public static List<EditOperation> generateEditScript(
            PDG srcPDG,
            PDG dstPDG,
            GraphMapping graphMapping,
            String srcSourceFilePath,
            String dstSourceFilePath,
            SootMethod srcMethod,
            SootMethod destMethod
    ) throws IOException {
        // Using a set to prevent duplicates (order does not matter for now).
        Set<EditOperation> editScriptSet = new HashSet<>();

        SourceCodeMapper srcCodeMapper = new SourceCodeMapper(srcSourceFilePath);
        SourceCodeMapper dstCodeMapper = new SourceCodeMapper(dstSourceFilePath);

        NodeMapping nodeMapping = graphMapping.getNodeMapping(srcPDG);

        Map<PDGNode, PDGNode> mappings = nodeMapping.getNodeMapping();
        Set<PDGNode> srcNodesMapped = mappings.keySet();
        Set<PDGNode> dstNodesMapped = new HashSet<>(mappings.values());

        Set<PDGNode> visitedNodes = new HashSet<>();

        // process mapped nodes for updates or moves
        for (PDGNode srcNode : srcNodesMapped) {
            PDGNode dstNode = mappings.get(srcNode);

            if (!visitedNodes.contains(srcNode)) {
                ComparisonResult compResult = nodesAreEqual(srcNode, dstNode, visitedNodes, srcCodeMapper, dstCodeMapper, nodeMapping);

                if (!compResult.isEqual) {
                    // think this move detetion doesnt work lowk...
                    if (compResult.isMove) {
                        int oldLineNumber = getNodeLineNumber(srcNode);
                        int newLineNumber = getNodeLineNumber(dstNode);
                        String codeSnippet = srcCodeMapper.getCodeLine(oldLineNumber);
                        editScriptSet.add(new Move(srcNode, oldLineNumber, newLineNumber, codeSnippet));
                    } else if (!compResult.syntaxDifferences.isEmpty()) {
                        for (SyntaxDifference syntaxDiff : compResult.syntaxDifferences) {
                            int oldLineNumber = syntaxDiff.getOldLineNumber();
                            int newLineNumber = syntaxDiff.getNewLineNumber();
                            String oldCodeSnippet = syntaxDiff.getOldCodeSnippet();
                            String newCodeSnippet = syntaxDiff.getNewCodeSnippet();
                            if (oldCodeSnippet.equals(newCodeSnippet)) {
                                Move move  = new Move(srcNode, oldLineNumber, newLineNumber, oldCodeSnippet);
                                editScriptSet.add(move);
                            } else {
                                Update update = new Update(srcNode, oldLineNumber, newLineNumber, oldCodeSnippet, newCodeSnippet, syntaxDiff);
                                editScriptSet.add(update);
                            }
                        }
                    }
                }
            }
        }

        // handle deletions
        for (PDGNode srcNode : srcPDG) {
            if (!srcNodesMapped.contains(srcNode) && !visitedNodes.contains(srcNode)) {
                int lineNumber = getNodeLineNumber(srcNode);
                String codeSnippet = srcCodeMapper.getCodeLine(lineNumber);
                editScriptSet.add(new Delete(srcNode, lineNumber, codeSnippet));
            }
        }

        // handle insertions
        for (PDGNode dstNode : dstPDG) {
            if (!dstNodesMapped.contains(dstNode) && !visitedNodes.contains(dstNode)) {
                int lineNumber = getNodeLineNumber(dstNode);
                String codeSnippet = dstCodeMapper.getCodeLine(lineNumber);
                editScriptSet.add(new Insert(dstNode, lineNumber, codeSnippet));
            }
        }

        // structural signature diff
        if (!srcMethod.getDeclaration().equals(destMethod.getDeclaration())) {
            ParsedSignature oldSig = parseMethodSignature(srcMethod);
            ParsedSignature newSig = parseMethodSignature(destMethod);

            List<EditOperation> signatureDiffs =
                    compareSignatures(oldSig, newSig, srcMethod, destMethod, srcCodeMapper, dstCodeMapper);

            editScriptSet.addAll(signatureDiffs);
        }

        return new ArrayList<>(editScriptSet);
    }


    public static List<EditOperation> generateAddScript(PDG pdg, String sourceFilePath, SootMethod method) throws IOException {
        SourceCodeMapper codeMapper = new SourceCodeMapper(sourceFilePath);
        List<EditOperation> editOperations = new ArrayList<>();

        // insert the method signature lines (approx.), handling for annoataions
        int[] methodRange = CodeAnalysisUtils.getMethodLineRange(method, codeMapper);
        List<Integer> annotationLines = CodeAnalysisUtils.getAnnotationsLineNumbers(method, codeMapper);
        if(min(annotationLines) < methodRange[0]) {
            methodRange[0] = min(annotationLines);
        }
        if (methodRange[0] > 0 && methodRange[1] >= methodRange[0]) {
            for (int i = methodRange[0]; i <= methodRange[1]; i++) {
                String signatureLine = codeMapper.getCodeLine(i);
                editOperations.add(new Insert(null, i, signatureLine));
            }
        }

        editOperations.addAll(
                collectNodesBFS(pdg).stream()
                        .map(node -> {
                            int lineNumber = getNodeLineNumber(node);
                            String codeSnippet = codeMapper.getCodeLine(lineNumber);
                            return new Insert(node, lineNumber, codeSnippet);
                        })
                        .collect(Collectors.toList())
        );

        return editOperations;
    }

    public static List<EditOperation> generateDeleteScript(PDG pdg, String sourceFilePath, SootMethod method) throws IOException {
        SourceCodeMapper codeMapper = new SourceCodeMapper(sourceFilePath);
        List<EditOperation> editOperations = new ArrayList<>();

        // delete the method signature lines (approx.)
        int[] methodRange = CodeAnalysisUtils.getMethodLineRange(method, codeMapper);
        List<Integer> annotationLines = CodeAnalysisUtils.getAnnotationsLineNumbers(method, codeMapper);
        if(min(annotationLines) < methodRange[0]) {
            methodRange[0] = min(annotationLines);
        }
        if (methodRange[0] > 0 && methodRange[1] >= methodRange[0]) {
            for (int i = methodRange[0]; i <= methodRange[1]; i++) {
                String signatureLine = codeMapper.getCodeLine(i);
                editOperations.add(new Delete(null, i, signatureLine));
            }
        }

        editOperations.addAll(
                collectNodesBFS(pdg).stream()
                        .map(node -> {
                            int lineNumber = getNodeLineNumber(node);
                            String codeSnippet = codeMapper.getCodeLine(lineNumber);
                            return new Delete(node, lineNumber, codeSnippet);
                        })
                        .collect(Collectors.toList())
        );

        return editOperations;
    }



    private static class ComparisonResult {
        public boolean isEqual;
        public boolean isMove;
        public Set<SyntaxDifference> syntaxDifferences;

        public ComparisonResult(boolean isEqual) {
            this.isEqual = isEqual;
            this.isMove = false;
            this.syntaxDifferences = new HashSet<>();
        }

        public ComparisonResult(boolean isEqual, boolean isMove, Set<SyntaxDifference> syntaxDifferences) {
            this.isEqual = isEqual;
            this.isMove = isMove;
            this.syntaxDifferences = syntaxDifferences;
        }
    }


    public static int getNodeLineNumber(PDGNode node) {
        if (node.getType() == PDGNode.Type.CFGNODE) {
            Unit headUnit = (Unit) node.getNode();
            return getLineNumber(headUnit);
        }
        return -1;
    }

    private static ComparisonResult nodesAreEqual(PDGNode n1, PDGNode n2, Set<PDGNode> visitedNodes,
                                                  SourceCodeMapper srcCodeMapper, SourceCodeMapper dstCodeMapper,
                                                  NodeMapping nodeMapping) {
        if (visitedNodes.contains(n1)) {
            return new ComparisonResult(true);
        }
        visitedNodes.add(n1);
        visitedNodes.add(n2);

        if (!n1.getType().equals(n2.getType())) {
            return new ComparisonResult(false);
        }

        if (n1.getType() == PDGNode.Type.CFGNODE) {
            return compareCFGNodes(n1, n2, srcCodeMapper, dstCodeMapper);
        }

        return new ComparisonResult(true);
    }

    private static ComparisonResult compareCFGNodes(PDGNode n1, PDGNode n2,
                                                    SourceCodeMapper srcCodeMapper, SourceCodeMapper dstCodeMapper) {
//        Block block1 = (Block) n1.getNode();
//        Block block2 = (Block) n2.getNode();

        Unit unit1 = (Unit) n1.getNode();
        Unit unit2 = (Unit) n2.getNode();

        List<Unit> units1 = Collections.singletonList(unit1);
        List<Unit> units2 = Collections.singletonList(unit2);

        Set<SyntaxDifference> differences = compareUnitLists(units1, units2, srcCodeMapper, dstCodeMapper);

        if (!differences.isEmpty()) {
            // TODO: this as of right now looks for jimple differences. might be useful to make sure differences are not just jimple differences
            return new ComparisonResult(false, false, differences);
        } else {
            // check for move operations based on line numbers
            int lineNumber1 = getNodeLineNumber(n1);
            int lineNumber2 = getNodeLineNumber(n2);
            if (lineNumber1 != lineNumber2 && lineNumber1 != -1 && lineNumber2 != -1) {
                return new ComparisonResult(false, true, differences);
            }
        }

        return new ComparisonResult(true);
    }

    private static Set<SyntaxDifference> compareUnitLists(List<Unit> units1, List<Unit> units2,
                                                          SourceCodeMapper srcCodeMapper, SourceCodeMapper dstCodeMapper) {
        Set<SyntaxDifference> differences = new HashSet<>();

        int i = 0, j = 0;
        while (i < units1.size() && j < units2.size()) {
            Unit unit1 = units1.get(i);
            Unit unit2 = units2.get(j);

            if (unitsAreEqual(unit1, unit2)) {
                i++;
                j++;
            } else {
                SyntaxDifference diff = new SyntaxDifference(unit1, unit2, srcCodeMapper, dstCodeMapper);
                differences.add(diff);
                i++;
                j++;
            }
        }

        // Handle remaining units in units1 (deletions)
        while (i < units1.size()) {
            SyntaxDifference diff = new SyntaxDifference(units1.get(i), null, srcCodeMapper, dstCodeMapper);
            differences.add(diff);
            i++;
        }

        // Handle remaining units in units2 (insertions)
        while (j < units2.size()) {
            SyntaxDifference diff = new SyntaxDifference(null, units2.get(j), srcCodeMapper, dstCodeMapper);
            differences.add(diff);
            j++;
        }

        return differences;
    }

    private static boolean unitsAreEqual(Unit unit1, Unit unit2) {
        if (unit1 == null || unit2 == null) {
            return false;
        }
        // compares the actual body representation of the units
        return unit1.toString().equals(unit2.toString());
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
