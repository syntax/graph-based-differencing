package org.pdgdiff.matching;

import org.pdgdiff.edit.RecoveryProcessor;

public class StrategySettings {
    protected RecoveryProcessor.RecoveryStrategy recoveryStrategy;
    protected GraphMatcherFactory.MatchingStrategy matchingStrategy;
    protected boolean aggregateRecovery;

    public StrategySettings(RecoveryProcessor.RecoveryStrategy recoveryStrategy, GraphMatcherFactory.MatchingStrategy matchingStrategy, boolean aggregateRecovery) {
        this.recoveryStrategy = recoveryStrategy;
        this.matchingStrategy = matchingStrategy;
        this.aggregateRecovery = aggregateRecovery;
    }

    public RecoveryProcessor.RecoveryStrategy getRecoveryStrategy() {
        return recoveryStrategy;
    }

    public GraphMatcherFactory.MatchingStrategy getMatchingStrategy() {
        return matchingStrategy;
    }


    public boolean isAggregateRecovery() {
        return aggregateRecovery;
    }

    public void setRecoveryStrategy(RecoveryProcessor.RecoveryStrategy recoveryStrategy) {
        this.recoveryStrategy = recoveryStrategy;
    }

    public void setMatchingStrategy(GraphMatcherFactory.MatchingStrategy matchingStrategy) {
        this.matchingStrategy = matchingStrategy;
    }
}
