package org.pdgdiff.matching;

import org.junit.Test;
import static org.junit.Assert.*;

public class GraphMatcherTest {
    // TODO: FIX THIS TESTING / AUTOMATE LARGER TESTING
    @Test
    public void testCompareLabels_ExactMatch() {
        GraphMatcher matcher = new GraphMatcher(null, null);
        String label1 = "int x = 10;";
        String label2 = "int x = 10;";

        double similarity = matcher.compareLabels(label1, label2) / 2;
        assertEquals(1.0, similarity, 0.01);
    }

    @Test
    public void testCompareLabels_PartialMatch() {
        GraphMatcher matcher = new GraphMatcher(null, null);
        String label2 = "int x = 11;";

        double similarity = matcher.compareLabels(label1, label2)/ 2;
        assertTrue(similarity > 0.8 && similarity < 1.0);
    }

    @Test
    public void testCompareLabels_NoMatch() {
        GraphMatcher matcher = new GraphMatcher(null, null);
        String label1 = "int x = 10;";
        String label2 = "float y = 20.0;";

        double similarity = matcher.compareLabels(label1, label2) / 2;

        assertTrue("Expected low similarity, but got: " + similarity, similarity < 0.8);
    }


    @Test
    public void testCompareLabels_NullValues() {
        GraphMatcher matcher = new GraphMatcher(null, null);
        String label1 = null;
        String label2 = "int x = 10;";

        double similarity = matcher.compareLabels(label1, label2) / 2;
        assertEquals(0.0, similarity, 0.01);
    }
}
