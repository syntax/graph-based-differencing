package org.pdgdiff.edit;

import org.pdgdiff.edit.model.*;
import org.pdgdiff.matching.models.heuristic.JaroWinklerSimilarity;
import org.pdgdiff.util.CodeAnalysisUtils;
import org.pdgdiff.util.SourceCodeMapper;
import soot.Modifier;
import soot.SootClass;
import soot.SootMethod;

import java.io.IOException;
import java.util.*;

public class SignatureDiffGenerator {

    public static class ParsedSignature {
        Set<String> modifiers;
        String returnType;
        String methodName;
        List<String> paramTokens;
        List<String> annotations;
        List <String> thrownExceptions;

        ParsedSignature(Set<String> modifiers, String returnType, String methodName, List<String> paramTokens, List<String> annotations, List<String> thrownExceptions) {
            this.modifiers = modifiers;
            this.returnType = returnType;
            this.methodName = methodName;
            this.paramTokens = paramTokens;
            this.annotations = annotations;
            this.thrownExceptions = thrownExceptions;
        }
    }

    public static ParsedSignature parseMethodSignature(SootMethod method, SourceCodeMapper mapper) throws IOException {
        // convert integer modifiers to a set of strings: e.g. {"public", "static"}
        Set<String> modifierSet = new HashSet<>();
        int mods = method.getModifiers();
        String modsString = Modifier.toString(mods); // e.g. "public static final"
        if (!modsString.isEmpty()) {
            // split on whitespace to get indiv tokens
            modifierSet.addAll(Arrays.asList(modsString.split("\\s+")));
        }

        String retType = method.getReturnType() != null ? method.getReturnType().toString() : "";
        String name = method.getName();

        List<SootClass> exceptionClasses = method.getExceptions();
        List<String> thrownExceptions = new ArrayList<>();
        for (SootClass exception : exceptionClasses) {
            thrownExceptions.add(exception.getName());
        }

        // to be populated later, no soot native way to get all the info required afaik
        List<Integer> paramLines = new ArrayList<>();
        List<String> paramTokens = CodeAnalysisUtils.getParamTokensAndLines(method, mapper, paramLines);

        // Annotation tokens (e.g. "@Override") + line nums for reporting
        List<Integer> annoLines = new ArrayList<>();
        List<String> annotations = CodeAnalysisUtils.getMethodAnnotationsWithLines(method, mapper, annoLines);

        return new ParsedSignature(modifierSet, retType, name, paramTokens, annotations, thrownExceptions);
    }


    static List<EditOperation> compareSignatures(
            ParsedSignature oldSig, ParsedSignature newSig,
            SootMethod oldMethod, SootMethod newMethod,
            SourceCodeMapper oldMapper, SourceCodeMapper newMapper
    ) {
        List<EditOperation> ops = new ArrayList<>();

        // these are approx'd and could actually return slightly off numbers if hard to parse.
        int[] oldRange = CodeAnalysisUtils.getMethodLineRange(oldMethod, oldMapper);
        int[] newRange = CodeAnalysisUtils.getMethodLineRange(newMethod, newMapper);

        int oldLine = (oldRange[0] > 0) ? oldRange[0] : -1;
        int newLine = (newRange[0] > 0) ? newRange[0] : -1;

        // cmp modifiers todo test this, not sure how useful this is
        Set<String> removedModifiers = new HashSet<>(oldSig.modifiers);
        removedModifiers.removeAll(newSig.modifiers);

        Set<String> addedModifiers = new HashSet<>(newSig.modifiers);
        addedModifiers.removeAll(oldSig.modifiers);

        for (String mod : removedModifiers) {
            ops.add(new Delete(
                    null, oldLine,
                    "Removed modifier: " + mod
            ));
        }
        for (String mod : addedModifiers) {
            ops.add(new Insert(
                    null, newLine,
                    "Added modifier: " + mod
            ));
        }

        // cmp return type
        if (!oldSig.returnType.equals(newSig.returnType)) {
            SyntaxDifference diff = new SyntaxDifference(
                    "Return type changed from " + oldSig.returnType + " to " + newSig.returnType
            );
            ops.add(
                    new Update(null, oldLine, newLine,
                            oldSig.returnType, newSig.returnType, diff)
            );
        }

        // cmp method name
        if (!oldSig.methodName.equals(newSig.methodName)) {
            SyntaxDifference diff = new SyntaxDifference(
                    "Method name changed from " + oldSig.methodName + " to " + newSig.methodName
            );
            ops.add(
                    new Update(null, oldLine, newLine,
                            oldSig.methodName, newSig.methodName, diff)
            );
        }

        List<Integer> oldParamLines = new ArrayList<>();
        List<String> oldParamTokens = CodeAnalysisUtils.getParamTokensAndLines(oldMethod, oldMapper, oldParamLines);

        List<Integer> newParamLines = new ArrayList<>();
        List<String> newParamTokens = CodeAnalysisUtils.getParamTokensAndLines(newMethod, newMapper, newParamLines);;


        ops.addAll(compareStringListsDP(oldParamTokens, newParamTokens,
                oldParamLines, newParamLines,
                "Parameter changed"));
//
//        if (oldParamLines.size() == 1 && newParamLines.size() == 1) {
//            // TODO  : avoid accidently marking a inserted param as a insert to the entire line, if the param changed adn multiple params exist on the same li
//                      This is debatable, if i mark just one side as an insert it will be more equatable with gumtree. However, I do think its less useful as a tool. hard to know.
//            if (!oldSig.paramTypes.equals(newSig.paramTypes)) {
//                SyntaxDifference diff = new SyntaxDifference("Parameter list changed");
//                ops.add(
//                        new Update(null, oldParamLines.get(0), newParamLines.get(0),
//                                oldMapper.getCodeLine(oldParamLines.get(0)),newMapper.getCodeLine(newParamLines.get(0)), diff)
//                );
//            }
//        } else {
//            // handle multi line parameters;
//            ops.addAll(
//                    compareStringListsDP(oldSig.paramTypes, newSig.paramTypes, oldParamLines, newParamLines)
//            );
//        }


//        List<Integer> oldAnnotationLines = CodeAnalysisUtils.getAnnotationsLineNumbers(oldMethod, oldMapper);
//        List<Integer> newAnnotationLines = CodeAnalysisUtils.getAnnotationsLineNumbers(newMethod, newMapper);
//
//        // NB this is not accounting for field annotations. todo fix
//        // overwrite annotations using line numbers, unfortunately soot does not provide a way to get annotations
//
//        oldSig.annotations = new ArrayList<>();
//        newSig.annotations = new ArrayList<>();
//        for (int i = 0; i < oldAnnotationLines.size(); i++) {
//            oldSig.annotations.add(oldMapper.getCodeLine(oldAnnotationLines.get(i)));
//        }
//        for (int i = 0; i < newAnnotationLines.size(); i++) {
//            newSig.annotations.add(newMapper.getCodeLine(newAnnotationLines.get(i)));
//        }
//
//
//        if (oldSig.annotations.size() == 1 && newSig.annotations.size() == 1) {
//            if (!Objects.equals(oldSig.annotations.get(0), newSig.annotations.get(0))) {
//                SyntaxDifference diff = new SyntaxDifference("Annotation changed");
//                ops.add(
//                        new Update(null, oldAnnotationLines.get(0), newAnnotationLines.get(0),
//                                oldSig.annotations.get(0), newSig.annotations.get(0), diff)
//                );
//            }
//        } else {
//            ops.addAll(
//                    compareStringListsDP(oldSig.annotations, newSig.annotations, oldAnnotationLines, newAnnotationLines)
//            );
//        }

        List<Integer> oldAnnoLines = new ArrayList<>();
        List<String> oldAnnoTokens = CodeAnalysisUtils.getMethodAnnotationsWithLines(oldMethod, oldMapper, oldAnnoLines);


        List<Integer> newAnnoLines = new ArrayList<>();
        List<String> newAnnoTokens = CodeAnalysisUtils.getMethodAnnotationsWithLines(newMethod, newMapper, newAnnoLines);

        ops.addAll(compareStringListsDP(oldAnnoTokens, newAnnoTokens,
                oldAnnoLines, newAnnoLines,
                "Annotation changed"));


        List<String> oldExceptions = oldSig.thrownExceptions;
        List<String> newExceptions = newSig.thrownExceptions;

        // following are being classified as deletes in order to remain more consitent with gumtree, but perhaps
        // they should be updates (esp based on how other bits of this impl are treating these sorta changes)

        Set<String> removedExceptions = new HashSet<>(oldExceptions);
        removedExceptions.removeAll(newExceptions);
        // todo: again should this be deletes or updates...
        for (String ex : removedExceptions) {
            ops.add(new Delete(null, oldLine, "Removed exception from func sig: " + ex));
        }

        Set<String> addedExceptions = new HashSet<>(newExceptions);
        addedExceptions.removeAll(oldExceptions);
        // todo: again should this be inserts or updates...
        for (String ex : addedExceptions) {
            ops.add(new Insert(null, newLine, "Added exception from func sig: " + ex));
        }

        return ops;
    }

    // left to right dynamic programming approach to try and match up parameters (or annos), basically a edit distance optimiation
    // nb soot gives parameter types, not names


    // generic DP function used for params and for annotations
    private static List<EditOperation> compareStringListsDP(
            List<String> oldEntries,  // old parameter types or old annotation lines
            List<String> newEntries,  // new parameter types or new annotation lines
            List<Integer> oldEntriesLines,  // old parameter line numbers or old annotation line numbers
            List<Integer> newEntriesLines,   // new parameter line numbers or new annotation line numbers
            String label
    ) {
        List<EditOperation> ops = new ArrayList<>();
        int m = oldEntries.size();
        int n = newEntries.size();

        double[][] dp = new double[m + 1][n + 1];
        String[][] opsTable = new String[m + 1][n + 1];

        // init DP table
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
            opsTable[i][0] = "DELETE";
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
            opsTable[0][j] = "INSERT";
        }
        opsTable[0][0] = "NO_CHANGE";

        // fill DP
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                String oldStr = oldEntries.get(i - 1);
                String newStr = newEntries.get(j - 1);

                if (oldStr.equals(newStr)) {
                    dp[i][j] = dp[i - 1][j - 1];
                    opsTable[i][j] = "NO_CHANGE";
                } else {
                    double deleteCost = dp[i - 1][j] + 1;
                    double insertCost = dp[i][j - 1] + 1;

                    double similarity = JaroWinklerSimilarity.jaroSimilarity(oldStr, newStr);
                    double updateCost = dp[i - 1][j - 1] + (1.0 - similarity);

                    if (deleteCost <= insertCost && deleteCost <= updateCost) {
                        dp[i][j] = deleteCost;
                        opsTable[i][j] = "DELETE";
                    } else if (insertCost <= deleteCost && insertCost <= updateCost) {
                        dp[i][j] = insertCost;
                        opsTable[i][j] = "INSERT";
                    } else {
                        dp[i][j] = updateCost;
                        opsTable[i][j] = "UPDATE";
                    }
                }
            }
        }

        // backtrack
        int i = m, j = n;
        while (i > 0 || j > 0) {
            String operation = opsTable[i][j];
            if ("NO_CHANGE".equals(operation)) {
                i--;
                j--;
            } else if ("DELETE".equals(operation)) {
                int oldLineNum = oldEntriesLines.get(i - 1);
                String entry = oldEntries.get(i - 1);
                ops.add(new Delete(null, oldLineNum, entry));
                i--;
            } else if ("INSERT".equals(operation)) {
                int newLineNum = newEntriesLines.get(j - 1);
                String entry = newEntries.get(j - 1);
                ops.add(new Insert(null, newLineNum, entry));
                j--;
            } else if ("UPDATE".equals(operation)) {
                int oldLineNum = oldEntriesLines.get(i - 1);
                int newLineNum = newEntriesLines.get(j - 1);
                String oldEntry = oldEntries.get(i - 1);
                String newEntry = newEntries.get(j - 1);

                SyntaxDifference diff = new SyntaxDifference(
                        label + " from \"" + oldEntry + "\" to \"" + newEntry + "\""
                );
                ops.add(new Update(null, oldLineNum, newLineNum, oldEntry, newEntry, diff));
                i--;
                j--;
            }
        }

        Collections.reverse(ops);
        return ops;
    }
}
