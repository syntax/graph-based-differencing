package org.pdgdiff.edit;

import org.pdgdiff.edit.model.*;
import org.pdgdiff.matching.GraphMapping;
import org.pdgdiff.matching.NodeMapping;
import org.pdgdiff.util.SourceCodeMapper;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.IRegion;
import soot.toolkits.graph.pdg.PDGNode;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates edit scripts based on PDG node mappings.
 */
public class EditScriptGenerator {

    public static List<EditOperation> generateEditScript(
            HashMutablePDG srcPDG,
            HashMutablePDG dstPDG,
            GraphMapping graphMapping,
            String srcSourceFilePath,
            String dstSourceFilePath,
            SootMethod srcMethod,
            SootMethod destMethod
    ) throws IOException {
        // Using set to prevent duplicates, I realise this is probably a logic errorI am rashly fixing but will do for now.
        // order of edit operations does not matter.
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
                            PDGNode node = srcNode;
                            Update update = new Update(node, oldLineNumber, newLineNumber, oldCodeSnippet, newCodeSnippet, syntaxDiff);
                            editScriptSet.add(update);
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

        // Signature comparison

        String oldMethodSignature = srcMethod.getDeclaration();
        String newMethodSignature = destMethod.getDeclaration();

        if (!oldMethodSignature.equals(newMethodSignature)) {
            int oldMethodLineNumber = getMethodLineNumber(srcMethod, srcCodeMapper);
            int newMethodLineNumber = getMethodLineNumber(destMethod, dstCodeMapper);

            Update signatureUpdate = new Update(
                    null, // no specific PDGNode associated for signature update
                    oldMethodLineNumber,
                    newMethodLineNumber,
                    oldMethodSignature,
                    newMethodSignature,
                    // TODO for the future this probably shouldnt be null.
                    null
            );

            editScriptSet.add(signatureUpdate);
        }

        // TODO: use SootClass to collect things like fields and difference those as well. This might have to happen in a different class.

        return new ArrayList<>(editScriptSet);
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

    public static int getMethodLineNumber(SootMethod method, SourceCodeMapper srcCodeMapper) throws IOException {
        int currentLine = method.getJavaSourceStartLineNumber();

        String methodName = method.getName();
        String returnType = method.getReturnType().toString();
        List<Type> parameterTypes = method.getParameterTypes();

        // regex pattern to match the method declaration
        String methodPattern = buildMethodPattern(returnType, methodName, parameterTypes);
        Pattern pattern = Pattern.compile(methodPattern);
        StringBuilder accumulatedLines = new StringBuilder();
        int methodDeclarationLine = -1;

        for (int i = 0; i < 20 && currentLine > 0; currentLine--) {
            String line = srcCodeMapper.getCodeLine(currentLine).trim();
            if (line.isEmpty()) {
                continue;
            }

            accumulatedLines.insert(0, line + " ");

            // checking if these match the regex per that method. (this is a Java.Regex matcher, not one of mine
            // naming a big confusing
            Matcher regexMatcher = pattern.matcher(accumulatedLines.toString());
            if (regexMatcher.find()) {
                methodDeclarationLine = currentLine;
                break;
            }
        }

        return methodDeclarationLine != -1 ? methodDeclarationLine : currentLine;
    }

    // TODO: refactor all the method stuff into its own class, cos this si getting huge
    private static String buildMethodPattern(String returnType, String methodName, List<Type> parameterTypes) {
        StringBuilder paramsPattern = new StringBuilder();
        paramsPattern.append("\\(");
        for (int i = 0; i < parameterTypes.size(); i++) {
            paramsPattern.append(".*");
            if (i < parameterTypes.size() - 1) {
                paramsPattern.append(",");
            }
        }
        paramsPattern.append("\\)");

        String methodPattern = String.format(
                ".*\\b%s\\b\\s+\\b%s\\b\\s*%s.*",
                Pattern.quote(returnType),
                Pattern.quote(methodName),
                paramsPattern
        );
        return methodPattern;
    }


    public static int getNodeLineNumber(PDGNode node) {
        if (node.getType() == PDGNode.Type.CFGNODE) {
            Block block = (Block) node.getNode();
            Unit headUnit = block.getHead();
            return getLineNumber(headUnit);
        } else if (node.getType() == PDGNode.Type.REGION) {
            IRegion region = (IRegion) node.getNode();
            Unit firstUnit = region.getFirst();
            return getLineNumber(firstUnit);
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
        } else if (n1.getType() == PDGNode.Type.REGION) {
            return compareRegionNodes(n1, n2, visitedNodes, srcCodeMapper, dstCodeMapper, nodeMapping);
        }

        return new ComparisonResult(true);
    }

    private static ComparisonResult compareRegionNodes(PDGNode n1, PDGNode n2, Set<PDGNode> visitedNodes,
                                                       SourceCodeMapper srcCodeMapper, SourceCodeMapper dstCodeMapper,
                                                       NodeMapping nodeMapping) {
        IRegion region1 = (IRegion) n1.getNode();
        IRegion region2 = (IRegion) n2.getNode();

        List<Unit> units1 = region1.getUnits();
        List<Unit> units2 = region2.getUnits();

        Set<SyntaxDifference> differences = compareUnitLists(units1, units2, srcCodeMapper, dstCodeMapper);

        // recurively compare child regions
        List<PDGNode> childNodes1 = getRegionChildNodes(n1);
        List<PDGNode> childNodes2 = getRegionChildNodes(n2);

        for (PDGNode child1 : childNodes1) {
            PDGNode child2 = nodeMapping.getMappedNode(child1);
            if (child2 != null) {
                ComparisonResult compResult = nodesAreEqual(child1, child2, visitedNodes, srcCodeMapper, dstCodeMapper, nodeMapping);
                if (!compResult.isEqual) {
                    differences.addAll(compResult.syntaxDifferences);
                }
            } else {
                // node has been deleted
                differences.add(new SyntaxDifference(child1, null, srcCodeMapper, dstCodeMapper));
            }
        }

        for (PDGNode child2 : childNodes2) {
            PDGNode child1 = nodeMapping.getReverseMappedNode(child2);
            if (child1 == null) {
                // node has been inserted
                differences.add(new SyntaxDifference(null, child2, srcCodeMapper, dstCodeMapper));
            }
        }

        if (!differences.isEmpty()) {
            return new ComparisonResult(false, false, differences);
        }

        return new ComparisonResult(true);
    }


    // helper class cos soot classes is not modifiable
    private static List<PDGNode> getRegionChildNodes(PDGNode regionNode) {
        List<PDGNode> childNodes = new ArrayList<>();
        for (PDGNode dependent : regionNode.getDependents()) {
            childNodes.add(dependent);
        }
        return childNodes;
    }

    private static ComparisonResult compareCFGNodes(PDGNode n1, PDGNode n2,
                                                    SourceCodeMapper srcCodeMapper, SourceCodeMapper dstCodeMapper) {
        Block block1 = (Block) n1.getNode();
        Block block2 = (Block) n2.getNode();

        List<Unit> units1 = collectUnits(block1);
        List<Unit> units2 = collectUnits(block2);

        Set<SyntaxDifference> differences = compareUnitLists(units1, units2, srcCodeMapper, dstCodeMapper);

        if (!differences.isEmpty()) {
            // TODO: this as of right now looks for jimple differences. might be useful to make sure differences are not just jimple differences
            return new ComparisonResult(false, false, differences);
        } else {
            // check for move operations based on line numbers
            int lineNumber1 = getNodeLineNumber(n1);
            int lineNumber2 = getNodeLineNumber(n2);

            if (lineNumber1 != lineNumber2) {
                return new ComparisonResult(false, true, differences);
            }
        }

        return new ComparisonResult(true);
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
