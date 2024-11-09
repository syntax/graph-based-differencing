package org.pdgdiff.edit;

import org.pdgdiff.edit.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class RecoveryProcessor {

    public enum RecoveryStrategy {
        GLOBAL_SIMILARITY,
        GRAPH_CENTRALITY,
        NONE
    }

    public static List<EditOperation> recoverMappings(List<EditOperation> editScript, RecoveryStrategy strategy) {
        switch (strategy) {
            case GLOBAL_SIMILARITY:
                return recoverMappingsGlobalSimilarity(editScript);
            case GRAPH_CENTRALITY:
                return recoverMappingsGraphCentrality(editScript);
            case NONE:
                return editScript;
            default:
                throw new IllegalArgumentException("Unknown recovery strategy: " + strategy);
        }
    }

    // TODO: some wierd behaviour around adds/deletes, but it's a start. Need to investigate the fact this has removed an insert
    // TODO: and replaced a update with a insert/delete when it shouldnt be.
    public static List<EditOperation> recoverMappingsGlobalSimilarity(List<EditOperation> editScript) {
        // collect all Update operations
        List<Update> updateOperations = editScript.stream()
                .filter(op -> op instanceof Update)
                .map(op -> (Update) op)
                .collect(Collectors.toList());

        // gen lists of unique old and new code snippets with their corresponding updates
        Map<String, List<Update>> oldCodeToUpdates = new HashMap<>();
        Map<String, List<Update>> newCodeToUpdates = new HashMap<>();

        for (Update update : updateOperations) {
            String oldKey = update.getOldCodeSnippet() + "||" + update.getOldLineNumber();
            String newKey = update.getNewCodeSnippet() + "||" + update.getNewLineNumber();

            oldCodeToUpdates.computeIfAbsent(oldKey, k -> new ArrayList<>()).add(update);
            newCodeToUpdates.computeIfAbsent(newKey, k -> new ArrayList<>()).add(update);
        }

        List<String> oldCodeKeys = new ArrayList<>(oldCodeToUpdates.keySet());
        List<String> newCodeKeys = new ArrayList<>(newCodeToUpdates.keySet());

        // cmp similarities between all pairs of old and new code snippets
        List<Assignment> possibleAssignments = new ArrayList<>();
        for (String oldKey : oldCodeKeys) {
            String oldCode = oldKey.split("\\|\\|")[0];
            for (String newKey : newCodeKeys) {
                String newCode = newKey.split("\\|\\|")[0];
                double similarity = computeSimilarity(oldCode, newCode);
                possibleAssignments.add(new Assignment(oldKey, newKey, similarity));
            }
        }

        // sort assignments by decreasing similarity
        possibleAssignments.sort((a1, a2) -> Double.compare(a2.similarity, a1.similarity));

        double similarityThreshold = 0.3; // TODO play with this but 0.3 seems ok

        Set<String> assignedOldKeys = new HashSet<>();
        Set<String> assignedNewKeys = new HashSet<>();

        List<Assignment> assignments = new ArrayList<>();

        for (Assignment assignment : possibleAssignments) {
            if (assignment.similarity < similarityThreshold) {
                break;
            }
            if (!assignedOldKeys.contains(assignment.oldKey) && !assignedNewKeys.contains(assignment.newKey)) {
                assignedOldKeys.add(assignment.oldKey);
                assignedNewKeys.add(assignment.newKey);
                assignments.add(assignment);
            }
        }

        // update the Update operations based on the new assignments
        List<EditOperation> newEditScript = new ArrayList<>();

        // for assigned pairs, update the Update operations
        for (Assignment assignment : assignments) {
            List<Update> updates = oldCodeToUpdates.get(assignment.oldKey);
            String newCode = assignment.newKey.split("\\|\\|")[0];
            int newLineNumber = Integer.parseInt(assignment.newKey.split("\\|\\|")[1]);

            for (Update update : updates) {
                update.setNewCodeSnippet(newCode);
                update.setNewLineNumber(newLineNumber);
                newEditScript.add(update);
            }
        }

        // for unmatched old code snippets, convert to Delete operations TODO : THINK THIS IS TOO HASTY
        for (String oldKey : oldCodeKeys) {
            if (!assignedOldKeys.contains(oldKey)) {
                List<Update> updates = oldCodeToUpdates.get(oldKey);
                for (Update update : updates) {
                    Delete deleteOp = new Delete(update.getNode(),
                            update.getOldLineNumber(), update.getOldCodeSnippet());
                    newEditScript.add(deleteOp);
                }
            }
        }

        // for unmatched new code snippets, create Insert operations TODO : THINK THIS IS TOO HASTY
        for (String newKey : newCodeKeys) {
            if (!assignedNewKeys.contains(newKey)) {
                String newCode = newKey.split("\\|\\|")[0];
                int newLineNumber = Integer.parseInt(newKey.split("\\|\\|")[1]);
                Insert insertOp = new Insert(null, newLineNumber, newCode);
                newEditScript.add(insertOp);
            }
        }

        cleanUpDuplicates(newEditScript);

        return newEditScript;
    }


    private static class Assignment {
        String oldKey; // combined old code and line number
        String newKey; // combiend new code and line number
        double similarity;

        public Assignment(String oldKey, String newKey, double similarity) {
            this.oldKey = oldKey;
            this.newKey = newKey;
            this.similarity = similarity;
        }
    }

    private static List<EditOperation> recoverMappingsGraphCentrality(List<EditOperation> editScript) {
        // TODO: implement this

        return editScript;
    }

    // Helper functions


    private static void cleanUpDuplicates(List<EditOperation> editScript) {
        Set<String> uniqueOperations = new HashSet<>();
        Iterator<EditOperation> iterator = editScript.iterator();
        while (iterator.hasNext()) {
            EditOperation op = iterator.next();
            String key = generateOperationKey(op);
            if (uniqueOperations.contains(key)) {
                iterator.remove();
            } else {
                uniqueOperations.add(key);
            }
        }
    }

    private static String generateOperationKey(EditOperation op) {
        if (op instanceof Update) {
            Update update = (Update) op;
            return "Update-" + update.getOldLineNumber() + "-" + update.getNewLineNumber() + "-"
                    + update.getOldCodeSnippet() + "-" + update.getNewCodeSnippet();
        } else if (op instanceof Insert) {
            Insert insert = (Insert) op;
            return "Insert-" + insert.getLineNumber() + "-" + insert.getCodeSnippet();
        } else if (op instanceof Delete) {
            Delete delete = (Delete) op;
            return "Delete-" + delete.getLineNumber() + "-" + delete.getCodeSnippet();
        } else if (op instanceof Move) {
            Move move = (Move) op;
            return "Move-" + move.getOldLineNumber() + "-" + move.getNewLineNumber() + "-"
                    + move.getCodeSnippet();
        }
        return op.toString();
    }

    private static double computeSimilarity(String s1, String s2) {
        String norm1 = normalizeCode(s1);
        String norm2 = normalizeCode(s2);

        int maxLength = Math.max(norm1.length(), norm2.length());
        if (maxLength == 0) return 1.0;

        int distance = levenshteinDistance(norm1, norm2);
        return 1.0 - (double) distance / maxLength;
    }

    private static String normalizeCode(String code) {
        // rm whitespace and comments
        return code.replaceAll("\\s+", "").replaceAll("//.*|/\\*((.|\\n)(?!=*/))+\\*/", "");
    }


    // Pretty boring algorithm for string similarity, but works
    private static int levenshteinDistance(String s, String t) {
        int m = s.length();
        int n = t.length();

        int[][] d = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= n; j++) {
            d[0][j] = j;
        }

        for (int i = 1; i <= m; i++) {
            char s_i = s.charAt(i - 1);
            for (int j = 1; j <= n; j++) {
                char t_j = t.charAt(j - 1);
                int cost = (s_i == t_j) ? 0 : 1;
                d[i][j] = Math.min(
                        Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1),
                        d[i - 1][j - 1] + cost
                );
            }
        }
        return d[m][n];
    }
}
