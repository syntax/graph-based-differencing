package org.pdgdiff.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * Maps line numbers to source code snippets for naive parsing when Soot struggles to define a line number for a unit.
 * Used for later hositing of elements when constructing the Edit Script at the source level.
 */
public class SourceCodeMapper {
    private HashMap<Integer, String> lineNumberToCodeMap;

    public SourceCodeMapper(String sourceFilePath) throws IOException {
        lineNumberToCodeMap = new HashMap<>();
        loadSourceCode(sourceFilePath);
    }

    private void loadSourceCode(String sourceFilePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(sourceFilePath));
        String line;
        int lineNumber = 1;
        while ((line = reader.readLine()) != null) {
            lineNumberToCodeMap.put(lineNumber, line);
            lineNumber++;
        }
        reader.close();
    }

    public String getCodeLine(int lineNumber) {
        return lineNumberToCodeMap.getOrDefault(lineNumber, "");
    }

    public int getTotalLines() {
        return lineNumberToCodeMap.size();
    }
}
