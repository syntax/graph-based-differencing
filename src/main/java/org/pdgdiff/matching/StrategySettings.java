package org.pdgdiff.matching;

import org.pdgdiff.edit.RecoveryProcessor;

public class StrategySettings {
    protected RecoveryProcessor.RecoveryStrategy recoveryStrategy;
    protected GraphMatcherFactory.MatchingStrategy matchingStrategy;

    public StrategySettings(RecoveryProcessor.RecoveryStrategy recoveryStrategy, GraphMatcherFactory.MatchingStrategy matchingStrategy) {
        this.recoveryStrategy = recoveryStrategy;
        this.matchingStrategy = matchingStrategy;
    }

    public RecoveryProcessor.RecoveryStrategy getRecoveryStrategy() {
        return recoveryStrategy;
    }

    public GraphMatcherFactory.MatchingStrategy getMatchingStrategy() {
        return matchingStrategy;
    }

    public void setRecoveryStrategy(RecoveryProcessor.RecoveryStrategy recoveryStrategy) {
        this.recoveryStrategy = recoveryStrategy;
    }

    public void setMatchingStrategy(GraphMatcherFactory.MatchingStrategy matchingStrategy) {
        this.matchingStrategy = matchingStrategy;
    }
}
