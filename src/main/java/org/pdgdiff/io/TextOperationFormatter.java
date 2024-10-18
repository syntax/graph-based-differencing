package org.pdgdiff.io;

import org.pdgdiff.edit.model.*;
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
        // No operation needed for plain text format
    }

    @Override
    public void endOutput() throws IOException {
        writer.flush();
        writer.close();
    }

    @Override
    public void startOperations() throws IOException {
        writer.write("Actions:\n");
    }

    @Override
    public void endOperations() throws IOException {
        // No operation needed for plain text format
    }

    @Override
    public void insertOperation(Insert operation) throws IOException {
        writer.write(String.format("Insert at line %d: %s\n", operation.getLineNumber(), operation.getCodeSnippet()));
    }

    @Override
    public void deleteOperation(Delete operation) throws IOException {
        writer.write(String.format("Delete at line %d: %s\n", operation.getLineNumber(), operation.getCodeSnippet()));
    }

    @Override
    public void updateOperation(Update operation) throws IOException {
        writer.write(String.format("Update at lines %d -> %d:\n", operation.getOldLineNumber(), operation.getNewLineNumber()));
        writer.write(String.format("Old Code: %s\n", operation.getOldCodeSnippet()));
        writer.write(String.format("New Code: %s\n", operation.getNewCodeSnippet()));
        writer.write("Difference:\n");
        writer.write(operation.getSyntaxDifference().toString() + "\n");
    }

    @Override
    public void moveOperation(Move operation) throws IOException {
        // TODO: implement for a Move operation
//        writer.write(String.format("Move from line %d to line %d: %s\n",
//                operation.getOldLineNumber(),
//                operation.getNewLineNumber(),
//                operation.getCodeSnippet()));
    }
}
