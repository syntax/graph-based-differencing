package org.pdgdiff.io;

import org.pdgdiff.edit.model.*;
import java.io.Writer;
import java.util.List;

/**
 * Serializer for plain text format.
 */
public class TextOperationSerializer extends OperationSerializer {

    public TextOperationSerializer(List<EditOperation> editScript) {
        super(editScript);
    }

    @Override
    protected OperationFormatter newFormatter(Writer writer) {
        return new TextOperationFormatter(writer);
    }
}
