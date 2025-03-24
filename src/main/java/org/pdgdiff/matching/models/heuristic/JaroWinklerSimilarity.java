package org.pdgdiff.matching.models.heuristic;


/**
* calculate % similarity between strings using Jaro-Winkler algorithm
**/
public class JaroWinklerSimilarity {

    public static double JaroWinklerSimilarity(String s1, String s2) {
        double jaro = jaroSimilarity(s1, s2);
        int prefixLength = commonPrefixLength(s1, s2);
        double SCALING_FACTOR = 0.1;

        return jaro + (prefixLength * SCALING_FACTOR * (1 - jaro));
    }

    // returns a double which is a similarity score between 0 and 1
    public static double jaroSimilarity(String s1, String s2) {
        if (s1.equals(s2)) {
            return 1.0;
        }

        if (s1.isEmpty() || s2.isEmpty()) {
            return 0.0;
        }

        // max distance for matching characters is floor(max(|s1|, |s2|) / 2) - 1
        int matchDistance = Math.max(s1.length(), s2.length()) / 2 - 1;

        boolean[] s1Matches = new boolean[s1.length()];
        boolean[] s2Matches = new boolean[s2.length()];

        // counting matches and transpositions
        int matches = 0;
        int transpositions = 0;

        for (int i = 0; i < s1.length(); i++) {
            int start = Math.max(0, i - matchDistance);
            int end = Math.min(i + matchDistance + 1, s2.length());

            for (int j = start; j < end; j++) {
                if (!s2Matches[j] && s1.charAt(i) == s2.charAt(j)) {
                    s1Matches[i] = true;
                    s2Matches[j] = true;
                    matches++;
                    break;
                }
            }
        }

        if (matches == 0) {
            return 0.0;
        }

        int k = 0;
        for (int i = 0; i < s1.length(); i++) {
            if (s1Matches[i]) {
                while (!s2Matches[k]) {
                    k++;
                }
                if (s1.charAt(i) != s2.charAt(k)) {
                    transpositions++;
                }
                k++;
            }
        }

        transpositions /= 2;

        // final similarity formula
        return ((matches / (double) s1.length()) +
                (matches / (double) s2.length()) +
                ((matches - transpositions) / (double) matches)) / 3.0;
    }

    public static int commonPrefixLength(String s1, String s2) {
        int prefixLength = 0;
        int maxPrefixLength = Math.min(4, Math.min(s1.length(), s2.length()));

        for (int i = 0; i < maxPrefixLength; i++) {
            if (s1.charAt(i) == s2.charAt(i)) {
                prefixLength++;
            } else {
                break;
            }
        }

        return prefixLength;
    }
}

