package org.pdgdiff.io;

import org.pdgdiff.edit.RecoveryProcessor;
import org.pdgdiff.edit.model.EditOperation;
import org.pdgdiff.matching.GraphMatcherFactory;
import org.pdgdiff.matching.Settings;

import java.io.Writer;
import java.util.List;

public class JsonOperationSerializer extends OperationSerializer {

    public JsonOperationSerializer(List<EditOperation> editScript, Settings settings) {
        super(editScript, settings);
    }

    @Override
    protected OperationFormatter newFormatter(Writer writer) {
        return new JsonOperationFormatter(writer);
    }
}
