package org.pdgdiff.io;

import org.pdgdiff.edit.model.*;
import org.pdgdiff.matching.Settings;

/**
 * Interface defining methods to format edit operations.
 */
public interface OperationFormatter {

    void writeInfo(Settings settings) throws Exception;

    void startOutput() throws Exception;
    void endOutput() throws Exception;

    void startOperations() throws Exception;
    void endOperations() throws Exception;

    void insertOperation(Insert operation) throws Exception;
    void deleteOperation(Delete operation) throws Exception;
    void updateOperation(Update operation) throws Exception;
    void moveOperation(Move operation) throws Exception;
}
