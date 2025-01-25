package org.pdgdiff.edit;

import org.pdgdiff.edit.model.*;
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

        ParsedSignature(Set<String> modifiers, String returnType, String methodName, List<String> paramTypes) {
            this.modifiers = modifiers;
            this.returnType = returnType;
            this.methodName = methodName;
            this.paramTypes = paramTypes;
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

        return new ParsedSignature(modifierSet, retType, name, paramList);
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
            // avoid accidently marking a inserted param as a insert to the entire line, if the param
            if (oldSig.paramTypes != newSig.paramTypes) {
                SyntaxDifference diff = new SyntaxDifference("Parameter list changed");
                ops.add(
                        new Update(null, oldLine, newLine,
                                oldSig.paramTypes.toString(), newSig.paramTypes.toString(), diff)
                );
            }
        } else {
            // handle multi line parameters
            ops.addAll(
                    compareParameterLists(oldSig.paramTypes, newSig.paramTypes, oldParamLines, newParamLines)
            );
        }
        return ops;
    }

    // left to right dynamic programming approach to try and match up parameters, basically a edit distance optimiation
    // nb soot gives us parameter types, not names
    private static List<EditOperation> compareParameterLists(
            List<String> oldParams,  // old parameter types
            List<String> newParams,  // new parameter types
            List<Integer> oldParamLines,  // old parameter line numbers
            List<Integer> newParamLines   // new parameter line numbers
    ) {
        List<EditOperation> ops = new ArrayList<>();

        int m = oldParams.size();
        int n = newParams.size();

        // DP table to store minimal edit distance and operation
        int[][] dp = new int[m + 1][n + 1];
        String[][] opsTable = new String[m + 1][n + 1];

        // init DP table
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;  // cost of deleting all remaining oldParams
            opsTable[i][0] = "DELETE";
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;  // cost of inserting all remaining newParams
            opsTable[0][j] = "INSERT";
        }

        // populate DP table
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (oldParams.get(i - 1).equals(newParams.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1];  // NO change
                    opsTable[i][j] = "NO_CHANGE";
                } else {
                    // compute costs for possible operations
                    int deleteCost = dp[i - 1][j] + 1;
                    int insertCost = dp[i][j - 1] + 1;
                    int updateCost = dp[i - 1][j - 1] + 1;

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
                // params match; no operation needed
                i--;
                j--;
            } else if ("DELETE".equals(operation)) {
                int lineNumber = oldParamLines.get(i - 1);
                String param = oldParams.get(i - 1);
                ops.add(new Delete(null, lineNumber, param));
                i--;
            } else if ("INSERT".equals(operation)) {
                int lineNumber = newParamLines.get(j - 1);
                String param = newParams.get(j - 1);
                ops.add(new Insert(null, lineNumber, param));
                j--;
            } else if ("UPDATE".equals(operation)) {
                int oldLine = oldParamLines.get(i - 1);
                int newLine = newParamLines.get(j - 1);
                String oldParam = oldParams.get(i - 1);
                String newParam = newParams.get(j - 1);
                SyntaxDifference diff = new SyntaxDifference("Parameter changed from " + oldParam + " to " + newParam);
                ops.add(new Update(null, oldLine, newLine, oldParam, newParam, diff));
                i--;
                j--;
            }
        }

        Collections.reverse(ops);
        return ops;
    }
}
