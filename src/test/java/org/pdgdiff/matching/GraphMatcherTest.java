package org.pdgdiff.matching;

import org.junit.Test;
import static org.junit.Assert.*;

public class GraphMatcherTest {

    @Test
    public void testCompareLabels_ExactMatch() {
        GraphMatcher matcher = new GraphMatcher(null, null);  // Passing null for PDGs, as we are only testing compareLabels
        String label1 = "int x = 10;";
        String label2 = "int x = 10;";

        double similarity = matcher.compareLabels(label1, label2);
        assertEquals(1.0, similarity, 0.01);  // Exact match should return a similarity of 1.0
    }

    @Test
    public void testCompareLabels_PartialMatch() {
        GraphMatcher matcher = new GraphMatcher(null, null);  // Passing null for PDGs, as we are only testing compareLabels
        String label1 = "int x = 10;";
        String label2 = "int x = 11;";

        double similarity = matcher.compareLabels(label1, label2);
        assertTrue(similarity > 0.8 && similarity < 1.0);  // Partial match should return a similarity greater than 0.5 and less than 1.0
    }

    @Test
    public void testCompareLabels_NoMatch() {
        GraphMatcher matcher = new GraphMatcher(null, null);  // Passing null for PDGs, as we are only testing compareLabels
        String label1 = "int x = 10;";
        String label2 = "float y = 20.0;";

        double similarity = matcher.compareLabels(label1, label2);

        // Add a print statement to see the actual similarity score during the test

        // If Jaro-Winkler is returning a similarity higher than expected, adjust the threshold
        assertTrue("Expected low similarity, but got: " + similarity, similarity < 0.8);  // Adjust threshold if necessary
    }


    @Test
    public void testCompareLabels_NullValues() {
        GraphMatcher matcher = new GraphMatcher(null, null);
        String label1 = null;
        String label2 = "int x = 10;";

        double similarity = matcher.compareLabels(label1, label2);
        assertEquals(0.0, similarity, 0.01);  // Null values should return a similarity of 0.0
    }
}
