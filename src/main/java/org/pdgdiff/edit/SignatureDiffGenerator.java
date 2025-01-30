package org.pdgdiff.edit;

import org.pdgdiff.edit.model.*;
import org.pdgdiff.matching.models.heuristic.JaroWinklerSimilarity;
import org.pdgdiff.util.CodeAnalysisUtils;
import org.pdgdiff.util.SourceCodeMapper;
import soot.Modifier;
import soot.SootMethod;

import java.io.IOException;
import java.util.*;

public class SignatureDiffGenerator {

    public static class ParsedSignature {
        Set<String> modifiers;
        String returnType;
        String methodName;
        List<String> paramTypes;
        List<String> annotations;

        ParsedSignature(Set<String> modifiers, String returnType, String methodName, List<String> paramTypes, List<String> annotations) {
            this.modifiers = modifiers;
            this.returnType = returnType;
            this.methodName = methodName;
            this.paramTypes = paramTypes;
            this.annotations = annotations;
        }
    }

    public static ParsedSignature parseMethodSignature(SootMethod method) {
        // convert integer modifiers to a set of strings: e.g. {"public", "static"}
        Set<String> modifierSet = new HashSet<>();
        int mods = method.getModifiers();
        String modsString = Modifier.toString(mods); // e.g. "public static final"
        if (!modsString.isEmpty()) {
            // split on whitespace to get individual tokens
            modifierSet.addAll(Arrays.asList(modsString.split("\\s+")));
        }

        String retType = method.getReturnType() != null ? method.getReturnType().toString() : "";
        String name = method.getName();

        List<String> paramList = new ArrayList<>();
        for (soot.Type t : method.getParameterTypes()) {
            paramList.add(t.toString());
        }
        System.out.println("paramList: " + paramList);

        // to be populated later. there is no soot native way to get annotations
        List<String> annotations = new ArrayList<>();

        return new ParsedSignature(modifierSet, retType, name, paramList, annotations);
    }


    static List<EditOperation> compareSignatures(
            ParsedSignature oldSig, ParsedSignature newSig,
            SootMethod oldMethod, SootMethod newMethod,
            SourceCodeMapper oldMapper, SourceCodeMapper newMapper
    ) throws IOException {
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

        List<Integer> oldParamLines = CodeAnalysisUtils.getParameterLineNumbers(oldMethod, oldMapper);
        List<Integer> newParamLines = CodeAnalysisUtils.getParameterLineNumbers(newMethod, newMapper);

        if (oldParamLines.size() == 1 && newParamLines.size() == 1) {
            // avoid accidently marking a inserted param as a insert to the entire line, if the param changed adn multiple params exist on the same line
            if (!oldSig.paramTypes.equals(newSig.paramTypes)) {
                SyntaxDifference diff = new SyntaxDifference("Parameter list changed");
                ops.add(
                        new Update(null, oldParamLines.get(0), newParamLines.get(0),
                                oldMapper.getCodeLine(oldParamLines.get(0)),newMapper.getCodeLine(newParamLines.get(0)), diff)
                );
            }
        } else {
            // handle multi line parameters
            ops.addAll(
                    compareStringListsDP(oldSig.paramTypes, newSig.paramTypes, oldParamLines, newParamLines)
            );
        }


        List<Integer> oldAnnotationLines = CodeAnalysisUtils.getAnnotationsLineNumbers(oldMethod, oldMapper);
        List<Integer> newAnnotationLines = CodeAnalysisUtils.getAnnotationsLineNumbers(newMethod, newMapper);

        // NB this is not accounting for field annotations. todo fix
        // overwrite annotations using line numbers, unfortunately soot does not provide a way to get annotations

        oldSig.annotations = new ArrayList<>();
        newSig.annotations = new ArrayList<>();
        for (int i = 0; i < oldAnnotationLines.size(); i++) {
            oldSig.annotations.add(oldMapper.getCodeLine(oldAnnotationLines.get(i)));
        }
        for (int i = 0; i < newAnnotationLines.size(); i++) {
            newSig.annotations.add(newMapper.getCodeLine(newAnnotationLines.get(i)));
        }


        System.out.println("for method " + oldSig.methodName + " old annotations: " + oldSig.annotations);
        System.out.println("for method " + newSig.methodName + " new annotations: " + newSig.annotations);
        if (oldSig.annotations.size() == 1 && newSig.annotations.size() == 1) {
            if (oldSig.annotations != newSig.annotations) {
                SyntaxDifference diff = new SyntaxDifference("Annotation changed");
                ops.add(
                        new Update(null, oldAnnotationLines.get(0), newAnnotationLines.get(0),
                                oldSig.annotations.toString(), newSig.annotations.toString(), diff)
                );
            }
        } else {
            System.out.println("oldAnnotationLines: " + oldAnnotationLines);
            System.out.println("Using DP Algo now....");
            ops.addAll(
                    compareStringListsDP(oldSig.annotations, newSig.annotations, oldAnnotationLines, newAnnotationLines)
            );
        }


        return ops;
    }

    // left to right dynamic programming approach to try and match up parameters (or annos), basically a edit distance optimiation
    // nb soot gives us parameter types, not names

    private static List<EditOperation> compareStringListsDP(
            List<String> oldEntries,  // old parameter types OR old annotation lines
            List<String> newEntries,  // new parameter types OR new annotation lines
            List<Integer> oldEntriesLines,  // old parameter line numbers OR old annotation line numbers
            List<Integer> newEntriesLines   // new parameter line numbers OR new annotation line numbers
    ) {
        List<EditOperation> ops = new ArrayList<>();

        int m = oldEntries.size();
        int n = newEntries.size();

        // DP table to store minimal edit distance and operation
        double[][] dp = new double[m + 1][n + 1];
        String[][] opsTable = new String[m + 1][n + 1];

        // init DP table
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;  // cost of deleting all remaining oldEntries
            opsTable[i][0] = "DELETE";
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;  // cost of inserting all remaining newEntries
            opsTable[0][j] = "INSERT";
        }

        // populate DP table
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                // nb params will be matching on type, and update cost considered on sim

                if (oldEntries.get(i - 1).equals(newEntries.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1];  // NO change
                    opsTable[i][j] = "NO_CHANGE";
                } else {
                    // compute costs for possible operations
                    String oldString = oldEntries.get(i - 1);
                    String newString = newEntries.get(j - 1);
                    double deleteCost = dp[i - 1][j] + 1;
                    double insertCost = dp[i][j - 1] + 1;
                    // NB for parameters -> my list of old/newEntries is just a list of their types... so this wont work
                    // as expected for example when differencing a changed name of a param. todo fix
                    // for Annotations -> using the entire line as the string hence will be good
                    double updateCost = dp[i - 1][j - 1] + (1 - JaroWinklerSimilarity.jaroSimilarity(oldString, newString));

                    // choose the minimum cost operation
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

        //backtracking
        int i = m, j = n;
        while (i > 0 || j > 0) {
            String operation = opsTable[i][j];

            if ("NO_CHANGE".equals(operation)) {
                // entries match; no operation needed
                i--;
                j--;
            } else if ("DELETE".equals(operation)) {
                int lineNumber = oldEntriesLines.get(i - 1);
                String entry = oldEntries.get(i - 1);
                ops.add(new Delete(null, lineNumber, entry));
                i--;
            } else if ("INSERT".equals(operation)) {
                int lineNumber = newEntriesLines.get(j - 1);
                String entry = newEntries.get(j - 1);
                ops.add(new Insert(null, lineNumber, entry));
                j--;
            } else if ("UPDATE".equals(operation)) {
                int oldLine = oldEntriesLines.get(i - 1);
                int newLine = newEntriesLines.get(j - 1);
                String oldEntry = oldEntries.get(i - 1);
                String newEntry = newEntries.get(j - 1);
                SyntaxDifference diff = new SyntaxDifference("String Lists changed from" + oldEntry + " to " + newEntry);
                ops.add(new Update(null, oldLine, newLine, oldEntry, newEntry, diff));
                i--;
                j--;
            }
        }

        Collections.reverse(ops);
        return ops;
    }
}
