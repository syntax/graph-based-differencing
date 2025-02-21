package org.pdgdiff.edit;

import org.pdgdiff.edit.model.*;

import java.util.*;

// Notes on this implementation:
// Initial idea of graph centrality heuristic: (doesn't actually make all that much sense,)
// Per each connected component, I want to extract the most central nodes of the graph (I assume those with the
// most edges). To be honest, I have no idea if this will generalise to many cases.

public class RecoveryProcessor {

    public enum RecoveryStrategy {
//        GLOBAL_SIMILARITY,  // not recommended
//        CONFLICT_GRAPH,   // not recommended
        LINE_LEVEL_UNIQUENESS, // TODO: invesitage that there is a chance that this is resulting in me loosing some data, i.e when i resolve a conflict I might
                               //   lose some other data that was needed to be represented as changed, just because it doesnt directly map.
        DUPLICATE_CLEANUP,
        CLEANUP, // nb contains LINE_LEVEL_UNIQUNESS
        FLATTEN,
        CLEANUP_AND_FLATTEN,
        FLATTEN_AND_CLEANUP,
        REMOVE_NEGATIVE_LINE_NUMBERS,
        NONE
    }

    public static List<EditOperation> recoverMappings(List<EditOperation> editScript, RecoveryStrategy strategy) {
        switch (strategy) {
//            case GLOBAL_SIMILARITY:
//                return recoverMappingsGlobalSimilarity(editScript);
//            case CONFLICT_GRAPH:
//                return recoverMappingsConflictGraphSimilarity(editScript);
            case LINE_LEVEL_UNIQUENESS:
                return recoverMappingsUniqueLineNums(editScript);
            case DUPLICATE_CLEANUP:
                return recoverMappingsNoDuplicates(editScript);
            case CLEANUP:
                return recoverMappingsCleanup(editScript);
            case FLATTEN:
                return recoverMappingsFlatten(editScript);
            case CLEANUP_AND_FLATTEN:
                return recoverMappingsCleanupAndFlattern(editScript);
            case FLATTEN_AND_CLEANUP: // this is risky and often leads to creation of conflicts that shouldnt exist. cleanup before flattern.
                // todo; prob remove
                return recoverMappingsFlattenAndCleanup(editScript);
            case REMOVE_NEGATIVE_LINE_NUMBERS:
                return removeNegativeLineNumbers(editScript);
            case NONE:
                return editScript;
            default:
                throw new IllegalArgumentException("Unknown recovery strategy: " + strategy);
        }
    }

    private static List<EditOperation> recoverMappingsFlattenAndCleanup(List<EditOperation> editScript) {
        // TODO: remove unless i find new use for this
        List<EditOperation> flattenScript = recoverMappingsFlatten(editScript);
        List<EditOperation> cleanedAndFlatterened = recoverMappingsCleanup(flattenScript);
        List<EditOperation> noNegatives = removeNegativeLineNumbers(cleanedAndFlatterened);
        cleanUpDuplicates(noNegatives);
        return noNegatives;
    }

    private static List<EditOperation> recoverMappingsCleanupAndFlattern(List<EditOperation> editScript) {
        List<EditOperation> cleanedScript = recoverMappingsCleanup(editScript);
        List<EditOperation> flattenedAndCleanedScript = recoverMappingsFlatten(cleanedScript);
        List<EditOperation> noNegatives = removeNegativeLineNumbers(flattenedAndCleanedScript);
        cleanUpDuplicates(noNegatives);
        return noNegatives;
    }

    private static List<EditOperation> removeNegativeLineNumbers(List<EditOperation> editScript) {
        // sometimes when jimple is hoisted to source code the line numbers will return -1, this is a simple fix to remove these.
        List<EditOperation> cleanedScript = new ArrayList<>();
        for (EditOperation op : editScript) {
            if (op instanceof Update) {
                Update update = (Update) op;
                if (update.getOldLineNumber() >= 0 && update.getNewLineNumber() >= 0) {
                    cleanedScript.add(update);
                }
            } else if (op instanceof Insert) {
                Insert insert = (Insert) op;
                if (insert.getLineNumber() >= 0) {
                    cleanedScript.add(insert);
                }
            } else if (op instanceof Delete) {
                Delete delete = (Delete) op;
                if (delete.getLineNumber() >= 0) {
                    cleanedScript.add(delete);
                }
            } else if (op instanceof Move) {
                Move move = (Move) op;
                if (move.getOldLineNumber() >= 0 && move.getNewLineNumber() >= 0) {
                    cleanedScript.add(move);
                }
            }
        }
        return cleanedScript;
    }

    private static List<EditOperation> recoverMappingsFlatten(List<EditOperation> editScript) {
        // 'flatterning' edit operations involves assessing pairs of insert->delete and delete->insert operations that are similar and can be turned into a update.
        // if identical, these can be changed to be just a MOVE. if similar, they can be changed to be an UPDATE.

        List<EditOperation> flattenedScript = new ArrayList<>(editScript); // not inplace, think this is better design choice, todo change cleanupduplciates to be same

        List<Insert> inserts = new ArrayList<>();
        List<Delete> deletes = new ArrayList<>();
        List<EditOperation> others = new ArrayList<>();

        for (EditOperation op : editScript) {
            if (op instanceof Insert) {
                inserts.add((Insert) op);
            } else if (op instanceof Delete) {
                deletes.add((Delete) op);
            } else {
                others.add(op);
            }
        }

        Set<Insert> usedInserts = new HashSet<>();
        Set<Delete> usedDeletes = new HashSet<>();

        // greedily match each insert with eh best delete.

        for (Insert ins : inserts) {
            double bestSimilarity = -1.0;
            Delete bestDelete = null;

            for (Delete del : deletes) {
                if (usedDeletes.contains(del)) {
                    continue;
                }

                // could implement the following BUT can argue that it doesnt matter too much, still provides a good differencing.
                // if (Math.abs(ins.getLineNumber() - del.getLineNumber()) > 5) continue;

                double similarity = computeSimilarity(ins.getCodeSnippet(), del.getCodeSnippet());
                if (similarity > bestSimilarity) {
                    bestSimilarity = similarity;
                    bestDelete = del;
                }
            }

            double threshold = 0.7;

            if (bestDelete != null && bestSimilarity >= threshold) {
                usedInserts.add(ins);
                usedDeletes.add(bestDelete);
                // Write this as a move if identical.
                if (Objects.equals(ins.getCodeSnippet(), bestDelete.getCodeSnippet())) {
                    if (ins.getLineNumber() != bestDelete.getLineNumber()) {
                        Move move = new Move(
                                bestDelete.getNode(),
                                bestDelete.getLineNumber(),
                                ins.getLineNumber(),
                                ins.getCodeSnippet()
                        );
                        flattenedScript.add(move);
                        flattenedScript.remove(ins);
                        flattenedScript.remove(bestDelete);
                    } else {
                        // dont need a move, literally the same line number.
                        flattenedScript.remove(ins);
                        flattenedScript.remove(bestDelete);
                    }
                } else {
                    // TODO: could properly calculate syntax difference for Update, not sure if that criritical howerver.

                    Update update = new Update(
                            bestDelete.getNode(),
                            bestDelete.getLineNumber(),
                            ins.getLineNumber(),
                            bestDelete.getCodeSnippet(),
                            ins.getCodeSnippet(),
                            new SyntaxDifference("flatten") // this just provides an indicator that this is a flattened operation
                    );
                    flattenedScript.add(update);

                    // 'squash' the inserts and delete operations into the new update.
                    flattenedScript.remove(ins);
                    flattenedScript.remove(bestDelete);
//                }
                }
            } else {
                // conditions not met so retain this insert operation. can remove this else but just for clarity at this stage.
            }
        }

        // cleanup, add inserts and deletes we haevnt resolved.
        for (Insert ins : inserts) {
            if (!usedInserts.contains(ins)) {
                flattenedScript.add(ins);
            }
        }

        for (Delete del : deletes) {
            if (!usedDeletes.contains(del)) {
                flattenedScript.add(del);
            }
        }

        // retain all other ops
        flattenedScript.addAll(others);

        return flattenedScript;
    }

    private static List<EditOperation> recoverMappingsCleanup(List<EditOperation> editScript) {
//        cleanUpDuplicates(editScript);

        List<EditOperation> flattenedScript = new ArrayList<>(editScript); // not inplace, think this is better design choice, todo change cleanupduplciates to be same

        // if an update operation is described over two lines,
        // and either a delete operations conflicts with a update operation on the old line number, or an insert operation conflicts with an update operation on the new line number,
        // then remove the delete and insert operations, and change the update operation to a move operation
        // todo: to be honest could prob rewrite something in teh edit script generator to avoid this behaviour. but this will do for now.
        Set<Integer> oldLineNumbers = new HashSet<>();
        Set<Integer> newLineNumbers = new HashSet<>();
        for (EditOperation op : flattenedScript) {
            if (op instanceof Update) {
                Update update = (Update) op;
                oldLineNumbers.add(update.getOldLineNumber());
                newLineNumbers.add(update.getNewLineNumber());
            }
        }

        Iterator<EditOperation> it = flattenedScript.iterator();
        while (it.hasNext()) {
            EditOperation op = it.next();
            if (op instanceof Delete) {
                Delete del = (Delete) op;
                if (oldLineNumbers.contains(del.getLineNumber())) {
                    it.remove();
                }
            }
            if (op instanceof Insert) {
                Insert ins = (Insert) op;
                if (newLineNumbers.contains(ins.getLineNumber())) {
                    it.remove();
                }
            }
        }

        flattenedScript = enforceLineLevelUniqueness(flattenedScript);

        // if an insertion and a deletion are near each other in line number, and have very similar code snippets, instead change a insertion and deletion pair to a update
        return flattenedScript;
    }

//    public static List<EditOperation> recoverMappingsGlobalSimilarity(List<EditOperation> editScript) {
//        // TODO: Probably want to get rid of this method, it is pretty much inferior in all ways to conflict graph method.
//        cleanUpDuplicates(editScript);
//
//        List<Update> updateOperations = editScript.stream()
//                .filter(op -> op instanceof Update)
//                .map(op -> (Update) op)
//                .collect(Collectors.toList());
//
//        Map<String, List<Update>> oldCodeToUpdates = new HashMap<>();
//        Map<String, List<Update>> newCodeToUpdates = new HashMap<>();
//
//
//        for (Update update : updateOperations) {
//            String oldKey = update.getOldCodeSnippet() + "||" + update.getOldLineNumber();
//            String newKey = update.getNewCodeSnippet() + "||" + update.getNewLineNumber();
//
//            oldCodeToUpdates.computeIfAbsent(oldKey, k -> new ArrayList<>()).add(update);
//            newCodeToUpdates.computeIfAbsent(newKey, k -> new ArrayList<>()).add(update);
//        }
//
//        // calc similarities between all pairs of old and new code snippets
//        List<Assignment> possibleAssignments = new ArrayList<>();
//        for (String oldKey : oldCodeToUpdates.keySet()) {
//            String oldCode = oldKey.split("\\|\\|")[0];
//            for (String newKey : newCodeToUpdates.keySet()) {
//                String newCode = newKey.split("\\|\\|")[0];
//                double similarity = computeSimilarity(oldCode, newCode);
//                possibleAssignments.add(new Assignment(oldKey, newKey, similarity));
//            }
//        }
//
//        possibleAssignments.sort((a1, a2) -> Double.compare(a2.similarity, a1.similarity));
//
//        double similarityThreshold = 0.7;   // investiage more
//
//        Set<String> assignedOldKeys = new HashSet<>();
//        Set<String> assignedNewKeys = new HashSet<>();
//        List<Assignment> assignments = new ArrayList<>();
//
//        for (Assignment assignment : possibleAssignments) {
//            if (assignment.similarity < similarityThreshold) {
//                break;
//            }
//            if (!assignedOldKeys.contains(assignment.oldKey) && !assignedNewKeys.contains(assignment.newKey)) {
//                assignedOldKeys.add(assignment.oldKey);
//                assignedNewKeys.add(assignment.newKey);
//                assignments.add(assignment);
//            }
//        }
//
//        List<EditOperation> resolvedEdits = new ArrayList<>();
//
//        for (Assignment assignment : assignments) {
//            List<Update> updates = oldCodeToUpdates.get(assignment.oldKey);
//            String newCode = assignment.newKey.split("\\|\\|")[0];
//            int newLineNumber = Integer.parseInt(assignment.newKey.split("\\|\\|")[1]);
//
//            for (Update update : updates) {
//                update.setNewCodeSnippet(newCode);
//                update.setNewLineNumber(newLineNumber);
//                resolvedEdits.add(update);
//            }
//        }
//
//        // retain unmatched updates without modification
//        for (String oldKey : oldCodeToUpdates.keySet()) {
//            if (!assignedOldKeys.contains(oldKey)) {
//                resolvedEdits.addAll(oldCodeToUpdates.get(oldKey));
//            }
//        }
//        for (String newKey : newCodeToUpdates.keySet()) {
//            if (!assignedNewKeys.contains(newKey)) {
//                resolvedEdits.addAll(newCodeToUpdates.get(newKey));
//            }
//        }
//
//        return resolvedEdits;
//    }
//


//    private static class Assignment {
//        String oldKey; // combined old code and line number
//        String newKey; // combiend new code and line number
//        double similarity;
//
//        public Assignment(String oldKey, String newKey, double similarity) {
//            this.oldKey = oldKey;
//            this.newKey = newKey;
//            this.similarity = similarity;
//        }
//    }

    private static List<EditOperation> enforceLineLevelUniqueness(List<EditOperation> editScript) {
        // grp all Update ops by their oldLineNumber
        Map<Integer, List<Update>> updatesByOldLine = new HashMap<>();
        for (EditOperation op : editScript) {
            if (op instanceof Update) {
                Update u = (Update) op;
                updatesByOldLine.computeIfAbsent(u.getOldLineNumber(), k -> new ArrayList<>())
                        .add(u);
            }
        }

        // keep only the "best" update for each oldLineNumber
        // if there is a conflict (i.e. multiple updates with distinct newLineNumbers).
        List<EditOperation> finalEdits = new ArrayList<>(editScript);

        for (Map.Entry<Integer, List<Update>> e : updatesByOldLine.entrySet()) {
            List<Update> conflicts = e.getValue();
            if (conflicts.size() <= 1) continue; // no conflict


            Update bestUpdate = pickBestUpdate(conflicts);
            // rm all but the best from finalEdits
            for (Update u : conflicts) {
                if (u != bestUpdate) {
                    finalEdits.remove(u);
                }
            }
        }

        // similarly ensure dont have multiple old lines mapping to the same new lineNumber
        // grp by newLineNumber
        Map<Integer, List<Update>> updatesByNewLine = new HashMap<>();
        for (EditOperation op : finalEdits) {
            if (op instanceof Update) {
                Update u = (Update) op;
                updatesByNewLine.computeIfAbsent(u.getNewLineNumber(), k -> new ArrayList<>())
                        .add(u);
            }
        }
        for (Map.Entry<Integer, List<Update>> e : updatesByNewLine.entrySet()) {
            List<Update> conflicts = e.getValue();
            if (conflicts.size() <= 1) continue;
            Update bestUpdate = pickBestUpdate(conflicts);
            for (Update u : conflicts) {
                if (u != bestUpdate) {
                    finalEdits.remove(u);
                }
            }
        }

        return finalEdits;
    }

    // this could be changed a lot, just will do for now
    private static Update pickBestUpdate(List<Update> candidates) {
        double bestScore = Double.NEGATIVE_INFINITY;
        Update bestU = null;
        for (Update u : candidates) {
            double sim = computeSimilarity(u.getOldCodeSnippet(), u.getNewCodeSnippet());
            if (sim > bestScore) {
                bestScore = sim;
                bestU = u;
            }
        }
        return bestU;

        // todo: ideally would use int[] methodRange = CodeAnalysisUtils.getMethodLineRange(method, codeMapper);
        //  and create some notation of where the element is in the method, relative, as another metric
        //  but this is not available in this context, just using edit script here.
    }


    public static List<EditOperation> recoverMappingsNoDuplicates(List<EditOperation> editScript) {
        cleanUpDuplicates(editScript);
        return editScript;
    }

    public static List<EditOperation> recoverMappingsUniqueLineNums(List<EditOperation> editScript) {
        return enforceLineLevelUniqueness(editScript);
    }

//    private static List<EditOperation> recoverMappingsConflictGraphSimilarity(List<EditOperation> editScript) {
//        cleanUpDuplicates(editScript);
//
//        List<Update> updateOperations = editScript.stream()
//                .filter(op -> op instanceof Update)
//                .map(op -> (Update) op)
//                .collect(Collectors.toList());
//
//        // build the conflict graph based on line number conflicts
//        Map<Update, Set<Update>> conflictGraph = new HashMap<>();
//        for (Update update : updateOperations) {
//            conflictGraph.putIfAbsent(update, new HashSet<>());
//        }
//
//        for (int i = 0; i < updateOperations.size(); i++) {
//            Update op1 = updateOperations.get(i);
//            for (int j = i + 1; j < updateOperations.size(); j++) {
//                Update op2 = updateOperations.get(j);
//                if (op1 != op2) {
//                    // Check for conflicts on old or new line numbers, add an edge where two updates conflict on a line number
//                    if (op1.getOldLineNumber() == op2.getOldLineNumber() || op1.getNewLineNumber() == op2.getNewLineNumber()) {
//                        conflictGraph.get(op1).add(op2);
//                        conflictGraph.get(op2).add(op1);
//                    }
//                }
//            }
//        }
//
////        printConflictGraph(conflictGraph);
//
//        List<Set<Update>> connectedComponents = findConnectedComponents(conflictGraph);
//
//        // process each component to resolve conflicts
//        List<EditOperation> newEditScript = new ArrayList<>();
//        Set<Update> processedUpdates = new HashSet<>();
//
//        // BELOW is an implementation of the Global similarity algorithm, but only on the subset of connected components found.
//
//        for (Set<Update> component : connectedComponents) {
//            // collect old and new code snippets within the component
//            Map<String, List<Update>> oldCodeToUpdates = new HashMap<>();
//            Map<String, List<Update>> newCodeToUpdates = new HashMap<>();
//
//            for (Update update : component) {
//                String oldKey = update.getOldCodeSnippet() + "||" + update.getOldLineNumber();
//                String newKey = update.getNewCodeSnippet() + "||" + update.getNewLineNumber();
//
//                oldCodeToUpdates.computeIfAbsent(oldKey, k -> new ArrayList<>()).add(update);
//                newCodeToUpdates.computeIfAbsent(newKey, k -> new ArrayList<>()).add(update);
//            }
//
//            // calc similarities between all pairs of old and new code snippets within the component
//            List<Assignment> possibleAssignments = new ArrayList<>();
//            for (String oldKey : oldCodeToUpdates.keySet()) {
//                String oldCode = oldKey.split("\\|\\|")[0];
//                for (String newKey : newCodeToUpdates.keySet()) {
//                    String newCode = newKey.split("\\|\\|")[0];
//                    double similarity = computeSimilarity(oldCode, newCode);
//                    possibleAssignments.add(new Assignment(oldKey, newKey, similarity));
//                }
//            }
//
//            // sortin assignments by similarity in decreasing order
//            possibleAssignments.sort((a1, a2) -> Double.compare(a2.similarity, a1.similarity));
//
//            double similarityThreshold = 0.7; // TODO investigate more
//
//            Set<String> assignedOldKeys = new HashSet<>();
//            Set<String> assignedNewKeys = new HashSet<>();
//            List<Assignment> assignments = new ArrayList<>();
//
//            // assn updates based on highest similarity scores
//            for (Assignment assignment : possibleAssignments) {
//                if (assignment.similarity < similarityThreshold) {
//                    break;
//                }
//                if (!assignedOldKeys.contains(assignment.oldKey) && !assignedNewKeys.contains(assignment.newKey)) {
//                    assignedOldKeys.add(assignment.oldKey);
//                    assignedNewKeys.add(assignment.newKey);
//                    assignments.add(assignment);
//                }
//            }
//
//            // update operations based on resolved assignments
//            for (Assignment assignment : assignments) {
//                List<Update> updates = oldCodeToUpdates.get(assignment.oldKey);
//                String newCode = assignment.newKey.split("\\|\\|")[0];
//                int newLineNumber = Integer.parseInt(assignment.newKey.split("\\|\\|")[1]);
//
//                for (Update update : updates) {
//                    update.setNewCodeSnippet(newCode);
//                    update.setNewLineNumber(newLineNumber);
//                    newEditScript.add(update);
//                    processedUpdates.add(update);
//                }
//            }
//
//            // retain unmatched updates within the component without modification
//            for (String oldKey : oldCodeToUpdates.keySet()) {
//                if (!assignedOldKeys.contains(oldKey)) {
//                    for (Update update : oldCodeToUpdates.get(oldKey)) {
//                        newEditScript.add(update);
//                        processedUpdates.add(update);
//                    }
//                }
//            }
//            for (String newKey : newCodeToUpdates.keySet()) {
//                if (!assignedNewKeys.contains(newKey)) {
//                    for (Update update : newCodeToUpdates.get(newKey)) {
//                        if (!processedUpdates.contains(update)) {
//                            newEditScript.add(update);
//                            processedUpdates.add(update);
//                        }
//                    }
//                }
//            }
//        }
//
//        // add any remaining operations not processed (non-conflicting updates and other operations)
//        for (EditOperation op : editScript) {
//            if (!(op instanceof Update) || !processedUpdates.contains(op)) {
//                newEditScript.add(op);
//            }
//        }
//
//        return newEditScript;
//    }

    // Find connected components in the conflict graph
//    private static List<Set<Update>> findConnectedComponents(Map<Update, Set<Update>> graph) {
//        List<Set<Update>> components = new ArrayList<>();
//        Set<Update> visited = new HashSet<>();
//
//        for (Update node : graph.keySet()) {
//            if (!visited.contains(node)) {
//                Set<Update> component = new HashSet<>();
//                Stack<Update> stack = new Stack<>();
//                stack.push(node);
//
//                while (!stack.isEmpty()) {
//                    Update current = stack.pop();
//                    if (visited.add(current)) {
//                        component.add(current);
//                        for (Update neighbor : graph.get(current)) {
//                            if (!visited.contains(neighbor)) {
//                                stack.push(neighbor);
//                            }
//                        }
//                    }
//                }
//                components.add(component);
//            }
//        }
//        return components;
//    }



    private static void cleanUpDuplicates(List<EditOperation> editScript) {
        // todo: think better convert this into a function as opposed to a procedure
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

//    private static void printConflictGraph(Map<Update, Set<Update>> conflictGraph) {
//        System.out.println("Conflict Graph:");
//        for (Map.Entry<Update, Set<Update>> entry : conflictGraph.entrySet()) {
//            Update node = entry.getKey();
//            Set<Update> neighbors = entry.getValue();
//
//            System.out.print("Node " + node.getOldLineNumber() + "->" + node.getNewLineNumber() + " is connected to: ");
//            if (neighbors.isEmpty()) {
//                System.out.println("No conflicts");
//            } else {
//                neighbors.forEach(neighbor -> System.out.print(neighbor.getOldLineNumber() + "->" + neighbor.getNewLineNumber() + " "));
//                System.out.println();
//            }
//        }
//    }

}
