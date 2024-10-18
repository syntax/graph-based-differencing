package org.pdgdiff.io;

import org.pdgdiff.edit.model.*;
import javax.xml.stream.*;
import java.io.Writer;

/**
 * Formatter to output operations in XML format.
 */
public class XmlOperationFormatter implements OperationFormatter {
    private final XMLStreamWriter writer;

    public XmlOperationFormatter(Writer writer) throws XMLStreamException {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        this.writer = factory.createXMLStreamWriter(writer);
    }

    @Override
    public void startOutput() throws XMLStreamException {
        writer.writeStartDocument();
    }

    @Override
    public void endOutput() throws XMLStreamException {
        writer.writeEndDocument();
        writer.flush();
        writer.close();
    }

    @Override
    public void startOperations() throws XMLStreamException {
        writer.writeStartElement("actions"); // Start the "actions" element
    }

    @Override
    public void endOperations() throws XMLStreamException {
        writer.writeEndElement(); // End the "actions" element
    }

    @Override
    public void insertOperation(Insert operation) throws XMLStreamException {
        writer.writeStartElement("action");
        writer.writeAttribute("type", "Insert");
        writer.writeAttribute("line", String.valueOf(operation.getLineNumber()));

        writer.writeStartElement("code");
        writer.writeCharacters(operation.getCodeSnippet());
        writer.writeEndElement(); // End "code"

        writer.writeEndElement(); // End "action"
    }

    @Override
    public void deleteOperation(Delete operation) throws XMLStreamException {
        writer.writeStartElement("action");
        writer.writeAttribute("type", "Delete");
        writer.writeAttribute("line", String.valueOf(operation.getLineNumber()));

        writer.writeStartElement("code");
        writer.writeCharacters(operation.getCodeSnippet());
        writer.writeEndElement(); // End "code"

        writer.writeEndElement(); // End "action"
    }

    @Override
    public void updateOperation(Update operation) throws XMLStreamException {
        writer.writeStartElement("action");
        writer.writeAttribute("type", "Update");
        writer.writeAttribute("oldLine", String.valueOf(operation.getOldLineNumber()));
        writer.writeAttribute("newLine", String.valueOf(operation.getNewLineNumber()));

        writer.writeStartElement("oldCode");
        writer.writeCharacters(operation.getOldCodeSnippet());
        writer.writeEndElement(); // End "oldCode"

        writer.writeStartElement("newCode");
        writer.writeCharacters(operation.getNewCodeSnippet());
        writer.writeEndElement(); // End "newCode"

        writer.writeStartElement("difference");

        writer.writeStartElement("message");
        writer.writeCharacters(operation.getSyntaxDifference().getMessage());
        writer.writeEndElement(); // End "message"

        writer.writeStartElement("oldJimple");
        writer.writeCharacters(operation.getSyntaxDifference().getOldJimpleCode());
        writer.writeEndElement(); // End "oldJimple"

        writer.writeStartElement("newJimple");
        writer.writeCharacters(operation.getSyntaxDifference().getNewJimpleCode());
        writer.writeEndElement(); // End "newJimple"

        writer.writeEndElement(); // End "difference"

        writer.writeEndElement(); // End "action"
    }

    @Override
    public void moveOperation(Move operation) throws XMLStreamException {
        // TODO: implement for a Move operation
    }
}
