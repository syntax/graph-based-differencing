package org.pdgdiff.matching.models.ged;

import java.util.Arrays;

/**
 * implementation of the Hungarian (Kuhn-Munkres) algorithm for the assignment problem on a cost matrix.
 *
 * For an n x n matrix costMatrix[row][col],
 * returns an int[] assignment where assignment[row] = col,
 * or -1 if unmatched.
 *
 * NB matricies must be square hence PDGs have been padded in calling function when needed.
 *
 * https://www.hungarianalgorithm.com/examplehungarianalgorithm.php
 * https://en.wikipedia.org/wiki/Hungarian_algorithm
 *
 */

public class HungarianAlgorithm {

    public static int[] minimizeAssignment(double[][] costMatrix) {
        int n = costMatrix.length;
        int[] assignment = new int[n];
        int[] partialMatch = new int[n];
        int[] trace = new int[n];
        double[] potentialRows = new double[n];
        double[] potentialCols = new double[n];

        Arrays.fill(assignment, -1);

        for (int i = 1; i < n; i++) {
            partialMatch[0] = i;
            int currCol = 0;
            double[] minCols = new double[n];
            boolean[] used = new boolean[n];
            Arrays.fill(minCols, Double.POSITIVE_INFINITY);
            do {
                used[currCol] = true;
                int currRow = partialMatch[currCol];
                double delta = Double.POSITIVE_INFINITY;
                int nextCol = 0;
                for (int j = 1; j < n; j++) {
                    if (!used[j]) {
                        double cur = costMatrix[currRow][j] - potentialRows[currRow] - potentialCols[j];
                        if (cur < minCols[j]) {
                            minCols[j] = cur;
                            trace[j] = currCol;
                        }
                        if (minCols[j] < delta) {
                            delta = minCols[j];
                            nextCol = j;
                        }
                    }
                }
                for (int j = 0; j < n; j++) {
                    if (used[j]) {
                        potentialRows[partialMatch[j]] += delta;
                        potentialCols[j] -= delta;
                    } else {
                        minCols[j] -= delta;
                    }
                }
                currCol = nextCol;
            } while (partialMatch[currCol] != 0);

            do {
                int nextCol = trace[currCol];
                partialMatch[currCol] = partialMatch[nextCol];
                currCol = nextCol;
            } while (currCol != 0);
        }

        // 'partialMatch[j] = i' means that  column j matched to row i
        for (int j = 1; j < n; j++) {
            assignment[partialMatch[j]] = j;
        }
        return assignment;
    }
}
