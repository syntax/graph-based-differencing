package org.pdgdiff.io;

import org.pdgdiff.edit.model.EditOperation;
import org.pdgdiff.matching.StrategySettings;

import java.io.Writer;
import java.util.List;

public class JsonOperationSerializer extends OperationSerializer {

    public JsonOperationSerializer(List<EditOperation> editScript, StrategySettings settings) {
        super(editScript, settings);
    }

    @Override
    protected OperationFormatter newFormatter(Writer writer) {
        return new JsonOperationFormatter(writer);
    }
}
