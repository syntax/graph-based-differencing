package org.pdgdiff.matching.models.ged;

import org.pdgdiff.matching.NodeMapping;

public class GEDResult {
    public final double distance;
    public final NodeMapping nodeMapping;

    public GEDResult(double distance, NodeMapping nodeMapping) {
        this.distance = distance;
        this.nodeMapping = nodeMapping;
    }
}
