package org.pdgdiff.io;

import org.pdgdiff.edit.model.*;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.Writer;

public class JsonOperationFormatter implements OperationFormatter {
    private final JsonWriter writer;

    public JsonOperationFormatter(Writer writer) {
        this.writer = new JsonWriter(writer);
        this.writer.setIndent("  ");
    }

    @Override
    public void startOutput() throws IOException {
        writer.beginObject();
    }

    @Override
    public void endOutput() throws IOException {
        writer.endObject();
        writer.close();
    }

    @Override
    public void startOperations() throws IOException {
        writer.name("actions").beginArray();
    }

    @Override
    public void endOperations() throws IOException {
        writer.endArray();
    }

    @Override
    public void insertOperation(Insert operation) throws IOException {
        writer.beginObject();
        writer.name("action").value("Insert");
        writer.name("line").value(operation.getLineNumber());
        writer.name("code").value(operation.getCodeSnippet());
        writer.endObject();
    }

    @Override
    public void deleteOperation(Delete operation) throws IOException {
        writer.beginObject();
        writer.name("action").value("Delete");
        writer.name("line").value(operation.getLineNumber());
        writer.name("code").value(operation.getCodeSnippet());
        writer.endObject();
    }

    @Override
    public void updateOperation(Update operation) throws IOException {
        writer.beginObject();
        writer.name("action").value("Update");
        writer.name("oldLine").value(operation.getOldLineNumber());
        writer.name("newLine").value(operation.getNewLineNumber());
        writer.name("oldCode").value(operation.getOldCodeSnippet());
        writer.name("newCode").value(operation.getNewCodeSnippet());

        writer.name("difference").beginObject();
        writer.name("message").value(operation.getSyntaxDifference().getMessage());
        writer.name("oldJimple").value(operation.getSyntaxDifference().getOldJimpleCode());
        writer.name("newJimple").value(operation.getSyntaxDifference().getNewJimpleCode());
        writer.endObject();

        writer.endObject();
    }

    @Override
    public void moveOperation(Move operation) throws IOException {
        // TODO: implement for a Move operation
    }
}
