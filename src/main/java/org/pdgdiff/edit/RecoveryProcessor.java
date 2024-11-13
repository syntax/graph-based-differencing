package org.pdgdiff.edit;

import org.pdgdiff.edit.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class RecoveryProcessor {

    public enum RecoveryStrategy {
        GLOBAL_SIMILARITY,
        GRAPH_CENTRALITY,
        DUPLICATE_CLEANUP,
        NONE
    }

    public static List<EditOperation> recoverMappings(List<EditOperation> editScript, RecoveryStrategy strategy) {
        switch (strategy) {
            case GLOBAL_SIMILARITY:
                return recoverMappingsGlobalSimilarity(editScript);
            case GRAPH_CENTRALITY:
                return recoverMappingsGraphCentrality(editScript);
            case DUPLICATE_CLEANUP:
                return recoverMappingsNoDuplicates(editScript);
            case NONE:
                return editScript;
                default:
                throw new IllegalArgumentException("Unknown recovery strategy: " + strategy);
        }
    }

    public static List<EditOperation> recoverMappingsGlobalSimilarity(List<EditOperation> editScript) {
        cleanUpDuplicates(editScript);

        // Collect all Update operations
        List<Update> updateOperations = editScript.stream()
                .filter(op -> op instanceof Update)
                .map(op -> (Update) op)
                .collect(Collectors.toList());

        // gen lists of unique old and new code snippets with their corresponding updates
        Map<String, List<Update>> oldCodeToUpdates = new HashMap<>();
        Map<String, List<Update>> newCodeToUpdates = new HashMap<>();


        // Lowk not sure this is the best way, to effectively hash an udpdate into a string, but works somewhat
        for (Update update : updateOperations) {
            String oldKey = update.getOldCodeSnippet() + "||" + update.getOldLineNumber();
            String newKey = update.getNewCodeSnippet() + "||" + update.getNewLineNumber();

            oldCodeToUpdates.computeIfAbsent(oldKey, k -> new ArrayList<>()).add(update);
            newCodeToUpdates.computeIfAbsent(newKey, k -> new ArrayList<>()).add(update);
        }

        // calc similarities between all pairs of old and new code snippets

        List<Assignment> possibleAssignments = new ArrayList<>();
        for (String oldKey : oldCodeToUpdates.keySet()) {
            String oldCode = oldKey.split("\\|\\|")[0];
            for (String newKey : newCodeToUpdates.keySet()) {
                String newCode = newKey.split("\\|\\|")[0];
                double similarity = computeSimilarity(oldCode, newCode);
                possibleAssignments.add(new Assignment(oldKey, newKey, similarity));
            }
        }

        possibleAssignments.sort((a1, a2) -> Double.compare(a2.similarity, a1.similarity));

        double similarityThreshold = 0.7;   // investiage more

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

        List<EditOperation> resolvedEdits = new ArrayList<>();

        // update operations based on resolved assignments
        for (Assignment assignment : assignments) {
            List<Update> updates = oldCodeToUpdates.get(assignment.oldKey);
            String newCode = assignment.newKey.split("\\|\\|")[0];
            int newLineNumber = Integer.parseInt(assignment.newKey.split("\\|\\|")[1]);

            for (Update update : updates) {
                update.setNewCodeSnippet(newCode);
                update.setNewLineNumber(newLineNumber);
                resolvedEdits.add(update);
            }
        }

        // retain unmatched updates without modification
        for (String oldKey : oldCodeToUpdates.keySet()) {
            if (!assignedOldKeys.contains(oldKey)) {
                resolvedEdits.addAll(oldCodeToUpdates.get(oldKey));
            }
        }
        for (String newKey : newCodeToUpdates.keySet()) {
            if (!assignedNewKeys.contains(newKey)) {
                resolvedEdits.addAll(newCodeToUpdates.get(newKey));
            }
        }

        return resolvedEdits;
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

    public static List<EditOperation> recoverMappingsNoDuplicates(List<EditOperation> editScript) {
        cleanUpDuplicates(editScript);
        return editScript;
    }

    private static List<EditOperation> recoverMappingsGraphCentrality(List<EditOperation> editScript) {
        cleanUpDuplicates(editScript);

        List<Update> updateOperations = editScript.stream()
                .filter(op -> op instanceof Update)
                .map(op -> (Update) op)
                .collect(Collectors.toList());

        // build the conflict graph based on line number conflicts
        Map<Update, Set<Update>> conflictGraph = new HashMap<>();
        for (Update update : updateOperations) {
            conflictGraph.putIfAbsent(update, new HashSet<>());
        }

        for (int i = 0; i < updateOperations.size(); i++) {
            Update op1 = updateOperations.get(i);
            for (int j = i + 1; j < updateOperations.size(); j++) {
                Update op2 = updateOperations.get(j);
                if (op1 != op2) {
                    // Check for conflicts on old or new line numbers
                    if (op1.getOldLineNumber() == op2.getOldLineNumber() || op1.getNewLineNumber() == op2.getNewLineNumber()) {
                        conflictGraph.get(op1).add(op2);
                        conflictGraph.get(op2).add(op1);
                    }
                }
            }
        }

        printConflictGraph(conflictGraph);

        // Find connected components in the conflict graph
        List<Set<Update>> connectedComponents = findConnectedComponents(conflictGraph);

        // process each component to resolve conflicts
        List<EditOperation> newEditScript = new ArrayList<>();
        Set<Update> processedUpdates = new HashSet<>();

        // TODO: bulk of the logic...
        // Per each connected component, I want to extract the most central nodes of the graph (I assume those with the
        // most edges). To be honest, I have no idea if this will generealise to many cases.

        // BELOW is an implementation of the Global similarity algorithm, but only on the subset of connected components found.

        for (Set<Update> component : connectedComponents) {
            // collect old and new code snippets within the component
            Map<String, List<Update>> oldCodeToUpdates = new HashMap<>();
            Map<String, List<Update>> newCodeToUpdates = new HashMap<>();

            for (Update update : component) {
                String oldKey = update.getOldCodeSnippet() + "||" + update.getOldLineNumber();
                String newKey = update.getNewCodeSnippet() + "||" + update.getNewLineNumber();

                oldCodeToUpdates.computeIfAbsent(oldKey, k -> new ArrayList<>()).add(update);
                newCodeToUpdates.computeIfAbsent(newKey, k -> new ArrayList<>()).add(update);
            }

            // calc similarities between all pairs of old and new code snippets within the component
            List<Assignment> possibleAssignments = new ArrayList<>();
            for (String oldKey : oldCodeToUpdates.keySet()) {
                String oldCode = oldKey.split("\\|\\|")[0];
                for (String newKey : newCodeToUpdates.keySet()) {
                    String newCode = newKey.split("\\|\\|")[0];
                    double similarity = computeSimilarity(oldCode, newCode);
                    possibleAssignments.add(new Assignment(oldKey, newKey, similarity));
                }
            }

            // sortin assignments by similarity in decreasing order
            possibleAssignments.sort((a1, a2) -> Double.compare(a2.similarity, a1.similarity));

            double similarityThreshold = 0.7; // TODO investigate more

            Set<String> assignedOldKeys = new HashSet<>();
            Set<String> assignedNewKeys = new HashSet<>();
            List<Assignment> assignments = new ArrayList<>();

            // assn updates based on highest similarity scores
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

            // update operations based on resolved assignments
            for (Assignment assignment : assignments) {
                List<Update> updates = oldCodeToUpdates.get(assignment.oldKey);
                String newCode = assignment.newKey.split("\\|\\|")[0];
                int newLineNumber = Integer.parseInt(assignment.newKey.split("\\|\\|")[1]);

                for (Update update : updates) {
                    update.setNewCodeSnippet(newCode);
                    update.setNewLineNumber(newLineNumber);
                    newEditScript.add(update);
                    processedUpdates.add(update);
                }
            }

            // retain unmatched updates within the component without modification
            for (String oldKey : oldCodeToUpdates.keySet()) {
                if (!assignedOldKeys.contains(oldKey)) {
                    for (Update update : oldCodeToUpdates.get(oldKey)) {
                        newEditScript.add(update);
                        processedUpdates.add(update);
                    }
                }
            }
            for (String newKey : newCodeToUpdates.keySet()) {
                if (!assignedNewKeys.contains(newKey)) {
                    for (Update update : newCodeToUpdates.get(newKey)) {
                        if (!processedUpdates.contains(update)) {
                            newEditScript.add(update);
                            processedUpdates.add(update);
                        }
                    }
                }
            }
        }

        // add any remaining operations not processed (non-conflicting updates and other operations)
        for (EditOperation op : editScript) {
            if (!(op instanceof Update) || !processedUpdates.contains(op)) {
                newEditScript.add(op);
            }
        }

        return newEditScript;
    }

    // Find connected components in the conflict graph
    private static List<Set<Update>> findConnectedComponents(Map<Update, Set<Update>> graph) {
        List<Set<Update>> components = new ArrayList<>();
        Set<Update> visited = new HashSet<>();

        for (Update node : graph.keySet()) {
            if (!visited.contains(node)) {
                Set<Update> component = new HashSet<>();
                Stack<Update> stack = new Stack<>();
                stack.push(node);

                while (!stack.isEmpty()) {
                    Update current = stack.pop();
                    if (visited.add(current)) {
                        component.add(current);
                        for (Update neighbor : graph.get(current)) {
                            if (!visited.contains(neighbor)) {
                                stack.push(neighbor);
                            }
                        }
                    }
                }
                components.add(component);
            }
        }
        return components;
    }

    private static void printConflictGraph(Map<Update, Set<Update>> conflictGraph) {
        System.out.println("Conflict Graph:");
        for (Map.Entry<Update, Set<Update>> entry : conflictGraph.entrySet()) {
            Update node = entry.getKey();
            Set<Update> neighbors = entry.getValue();

            System.out.print("Node " + node.getOldLineNumber() + "->" + node.getNewLineNumber() + " is connected to: ");
            if (neighbors.isEmpty()) {
                System.out.println("No conflicts");
            } else {
                neighbors.forEach(neighbor -> System.out.print(neighbor.getOldLineNumber() + "->" + neighbor.getNewLineNumber() + " "));
                System.out.println();
            }
        }
    }


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


    // consider using .hashCode on this one ...
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


    private static int levenshteinDistance(String s, String t) {
        // TODO: refactor into Similartiy file alongside JaroWinkely
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
