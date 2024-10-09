package org.pdgdiff.edit;

public abstract class EditOperation {
    public enum OperationType {
        INSERT_NODE,
        DELETE_NODE,
        INSERT_EDGE,
        DELETE_EDGE
    }

    private OperationType type;

    public EditOperation(OperationType type) {
        this.type = type;
    }

    public OperationType getType() {
        return type;
    }

    public abstract String toString();
}
