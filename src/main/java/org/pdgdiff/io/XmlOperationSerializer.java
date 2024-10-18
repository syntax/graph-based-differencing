package org.pdgdiff.io;

import org.pdgdiff.edit.model.*;
import java.io.Writer;
import java.util.List;

/**
 * Serializer for XML format.
 */
public class XmlOperationSerializer extends OperationSerializer {

    public XmlOperationSerializer(List<EditOperation> editScript) {
        super(editScript);
    }

    @Override
    protected OperationFormatter newFormatter(Writer writer) throws Exception {
        return new XmlOperationFormatter(writer);
    }
}
