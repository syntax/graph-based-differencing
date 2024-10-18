package org.pdgdiff.io;

import org.pdgdiff.edit.model.*;
import com.google.gson.stream.JsonWriter;
import soot.toolkits.graph.pdg.PDGNode;

import java.io.IOException;
import java.io.Writer;

/**
 * Formatter to output operations in JSON format.
 */
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
        writer.name("actions").beginArray(); // Keeping "actions" to match GumTree's format for benchmarkin
    }

    @Override
    public void endOperations() throws IOException {
        writer.endArray();
    }

    @Override
    public void insertOperation(Insert operation) throws IOException {
        writer.beginObject();
        writer.name("action").value("Insert");
        writer.name("node").value(nodeToString(operation.getNode()));
        writer.endObject();
    }

    @Override
    public void deleteOperation(Delete operation) throws IOException {
        writer.beginObject();
        writer.name("action").value("Delete");
        writer.name("node").value(nodeToString(operation.getNode()));
        writer.endObject();
    }

    @Override
    public void updateOperation(Update operation) throws IOException {
        writer.beginObject();
        writer.name("action").value("Update");
        writer.name("node").value(nodeToString(operation.getNode()));
        writer.name("oldValue").value(operation.getOldValue());
        writer.name("newValue").value(operation.getNewValue());
        writer.name("differences").beginArray();
        for (SyntaxDifference diff : operation.getSyntaxDifferences()) {
            writer.value(diff.toString());
        }
        writer.endArray();
        writer.endObject();
    }

    @Override
    public void moveOperation(Move operation) throws IOException {
        writer.beginObject();
        writer.name("action").value("Move");
        writer.name("node").value(nodeToString(operation.getNode()));
        writer.name("oldPredecessors").beginArray();
        for (PDGNode pred : operation.getOldPredecessors()) {
            writer.value(nodeToString(pred));
        }
        writer.endArray();
        writer.name("newPredecessors").beginArray();
        for (PDGNode pred : operation.getNewPredecessors()) {
            writer.value(nodeToString(pred));
        }
        writer.endArray();
        writer.endObject();
    }

    private String nodeToString(PDGNode node) {
        // TODO can change htis method if I want to add more, as of rn just wrapping the .toString
        return node.toShortString();
    }
}
