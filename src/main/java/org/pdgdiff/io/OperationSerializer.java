package org.pdgdiff.io;

import org.pdgdiff.edit.RecoveryProcessor.RecoveryStrategy;
import org.pdgdiff.edit.model.*;
import org.pdgdiff.matching.GraphMatcherFactory.MatchingStrategy;
import org.pdgdiff.matching.Settings;

import java.io.Writer;
import java.util.List;

public abstract class OperationSerializer {
    protected List<EditOperation> editScript;
    protected Settings settings;


    public OperationSerializer(List<EditOperation> editScript, Settings settings) {
        this.editScript = editScript;
        this.settings = settings;
    }

    protected abstract OperationFormatter newFormatter(Writer writer) throws Exception;

    public void writeTo(Writer writer) throws Exception {
        OperationFormatter formatter = newFormatter(writer);

        formatter.startOutput();

        formatter.startOperations();
        for (EditOperation op : editScript) {
            if (op instanceof Insert) {
                formatter.insertOperation((Insert) op);
            } else if (op instanceof Delete) {
                formatter.deleteOperation((Delete) op);
            } else if (op instanceof Update) {
                formatter.updateOperation((Update) op);
            } else if (op instanceof Move) {
                formatter.moveOperation((Move) op);
            }
        }
        formatter.endOperations();
        formatter.endOutput();
    }
}
