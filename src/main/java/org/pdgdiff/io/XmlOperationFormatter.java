package org.pdgdiff.io;

import org.pdgdiff.edit.model.*;
import soot.toolkits.graph.pdg.PDGNode;

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
        writer.writeStartElement("actions"); // Keeping "actions" to match GumTree's format for benchmarkin
    }

    @Override
    public void endOperations() throws XMLStreamException {
        writer.writeEndElement();
    }

    @Override
    public void insertOperation(Insert operation) throws XMLStreamException {
        writer.writeEmptyElement("action");
        writer.writeAttribute("type", "Insert");
        writer.writeAttribute("node", nodeToString(operation.getNode()));
    }

    @Override
    public void deleteOperation(Delete operation) throws XMLStreamException {
        writer.writeEmptyElement("action");
        writer.writeAttribute("type", "Delete");
        writer.writeAttribute("node", nodeToString(operation.getNode()));
    }

    @Override
    public void updateOperation(Update operation) throws XMLStreamException {
        writer.writeStartElement("action");
        writer.writeAttribute("type", "Update");
        writer.writeAttribute("node", nodeToString(operation.getNode()));
        writer.writeStartElement("oldValue");
        writer.writeCharacters(operation.getOldValue());
        writer.writeEndElement();
        writer.writeStartElement("newValue");
        writer.writeCharacters(operation.getNewValue());
        writer.writeEndElement();
        writer.writeEndElement();
    }

    @Override
    public void moveOperation(Move operation) throws XMLStreamException {
        writer.writeStartElement("action");
        writer.writeAttribute("type", "Move");
        writer.writeAttribute("node", nodeToString(operation.getNode()));
        // TODO: include more details
        writer.writeEndElement();
    }

    private String nodeToString(PDGNode node) {
        return node.toShortString(); // TODO: adjust
    }
}
