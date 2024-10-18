package org.pdgdiff.io;

import org.pdgdiff.edit.model.*;
import soot.toolkits.graph.pdg.PDGNode;

import java.io.IOException;
import java.io.Writer;

/**
 * Formatter to output operations in plain text format.
 */
public class TextOperationFormatter implements OperationFormatter {
    private final Writer writer;

    public TextOperationFormatter(Writer writer) {
        this.writer = writer;
    }

    @Override
    public void startOutput() throws IOException {
        // No-op for text format
    }

    @Override
    public void endOutput() throws IOException {
        writer.flush();
        writer.close();
    }

    @Override
    public void startOperations() throws IOException {
        writer.write("Actions:\n"); // Keeping "Actions" to match GumTree's format
    }

    @Override
    public void endOperations() throws IOException {
        // No-op for text format
    }

    @Override
    public void insertOperation(Insert operation) throws IOException {
        writer.write("Insert: " + nodeToString(operation.getNode()) + "\n");
    }

    @Override
    public void deleteOperation(Delete operation) throws IOException {
        writer.write("Delete: " + nodeToString(operation.getNode()) + "\n");
    }

    @Override
    public void updateOperation(Update operation) throws IOException {
        writer.write("Update: " + nodeToString(operation.getNode()) +
                " from '" + operation.getOldValue() + "' to '" + operation.getNewValue() + "'\n");
    }

    @Override
    public void moveOperation(Move operation) throws IOException {
        writer.write("Move: " + nodeToString(operation.getNode()) + "\n");
    }

    private String nodeToString(PDGNode node) {
        return node.toShortString(); // TODO: adjust
    }
}
