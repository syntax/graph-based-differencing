package org.pdgdiff.io;

import org.pdgdiff.edit.model.*;
import java.io.Writer;
import java.util.List;
/**
 * Serializer for JSON format.
 */
public class JsonOperationSerializer extends OperationSerializer {

    public JsonOperationSerializer(List<EditOperation> editScript) {
        super(editScript);
    }

    @Override
    protected OperationFormatter newFormatter(Writer writer) {
        return new JsonOperationFormatter(writer);
    }
}
