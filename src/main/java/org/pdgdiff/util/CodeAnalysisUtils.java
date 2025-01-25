package org.pdgdiff.util;

import soot.*;
import soot.tagkit.LineNumberTag;

import java.io.Console;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CodeAnalysisUtils {

    // TODO: a lot of these 'parser' methods that parse the file using regex to lookup are O(n)

    public static int getClassLineNumber(SootClass sootClass, SourceCodeMapper codeMapper) throws IOException {
        int lineNumber = sootClass.getJavaSourceStartLineNumber();
        if (lineNumber > 0) {
            return lineNumber;
        }

        // if line number is not directly available, search for it
        String className = sootClass.getShortName();
        String classPattern = String.format(".*\\b(class|interface|enum)\\b\\s+\\b%s\\b.*\\{", Pattern.quote(className));
        Pattern pattern = Pattern.compile(classPattern);

        int totalLines = codeMapper.getTotalLines();
        for (int i = 1; i <= totalLines; i++) {
            String line = codeMapper.getCodeLine(i).trim();
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                return i;
            }
        }

        return -1;
    }

    public static String getClassDeclaration(SootClass sootClass, SourceCodeMapper codeMapper) throws IOException {
        int lineNumber = getClassLineNumber(sootClass, codeMapper);
        if (lineNumber > 0) {
            return codeMapper.getCodeLine(lineNumber).trim();
        }
        return ""; // not found
    }

    public static int getFieldLineNumber(SootField field, SourceCodeMapper codeMapper) throws IOException {
        int lineNumber = field.getJavaSourceStartLineNumber();
        if (lineNumber > 0) {
            return lineNumber;
        }

        String fieldName = field.getName();
        String fieldType = field.getType().toString();

        // parse simple type name (i.e. without full package declaration) String instead of java.lang.String
        String simpleFieldType = fieldType.substring(fieldType.lastIndexOf('.') + 1);

        // regex pattern, possibility of missed case here
        String fieldPattern = String.format(
                ".*\\b(?:public|protected|private|static|final|transient|volatile|abstract|synchronized|native|strictfp|\\s)*\\b%s\\b\\s+\\b%s\\b.*;",
                Pattern.quote(simpleFieldType),
                Pattern.quote(fieldName)
        );
        Pattern pattern = Pattern.compile(fieldPattern);

        int totalLines = codeMapper.getTotalLines();
        for (int i = 1; i <= totalLines; i++) {
            String line = codeMapper.getCodeLine(i).trim();
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                return i;
            }
        }

        return -1;
    }


    public static String getFieldDeclaration(SootField field, SourceCodeMapper codeMapper) throws IOException {
        int lineNumber = getFieldLineNumber(field, codeMapper);
        if (lineNumber > 0) {
            return codeMapper.getCodeLine(lineNumber).trim();
        }
        return "";
    }


    public static int[] getMethodLineRange(SootMethod method, SourceCodeMapper srcCodeMapper) throws IOException {
        int initialLine = method.getJavaSourceStartLineNumber();
        if (initialLine <= 0) {
            return new int[]{-1, -1};
        }

        String methodName = method.getName();
        String methodPattern = String.format(".*\\b%s\\b\\s*\\(.*", Pattern.quote(methodName));
        Pattern signatureStartPattern = Pattern.compile(methodPattern);

        int totalLines = srcCodeMapper.getTotalLines();
        int startLine = initialLine;
        int endLine = initialLine;

        for (int i = initialLine; i > 0; i--) {
            String line = srcCodeMapper.getCodeLine(i).trim();
            if (line.isEmpty()) continue;

            Matcher m = signatureStartPattern.matcher(line);
            if (m.matches()) {
                startLine = i;
                break;
            }
        }

        boolean foundBrace = false;
        for (int i = startLine; i <= totalLines; i++) {
            String line = srcCodeMapper.getCodeLine(i).trim();
            if (line.contains("{")) {
                endLine = i;
                break;
            }
            if (!foundBrace) {
                endLine = i;
            }
        }

        return new int[]{startLine, endLine};
    }

    public static List<Integer> getParameterLineNumbers(SootMethod method, SourceCodeMapper codeMapper) throws IOException {
        List<Integer> paramLines = new ArrayList<>();
        int[] methodRange = getMethodLineRange(method, codeMapper);

        if (methodRange[0] > 0 && methodRange[1] >= methodRange[0]) {
            for (int i = methodRange[0]; i <= methodRange[1]; i++) {
                String line = codeMapper.getCodeLine(i).trim();
                if (line.contains("(") || line.contains(",")) {
                    paramLines.add(i);
                }
            }
        }
        return paramLines;
    }

    public static int getLineNumber(Unit unit) {
        if (unit == null) {
            return -1;
        }
        LineNumberTag tag = (LineNumberTag) unit.getTag("LineNumberTag");
        if (tag != null) {
            return tag.getLineNumber();
        }
        return -1;
    }
}
