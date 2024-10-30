package org.pdgdiff.util;

import org.pdgdiff.util.SourceCodeMapper;
import soot.*;
import soot.tagkit.LineNumberTag;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static int getMethodLineNumber(SootMethod method, SourceCodeMapper srcCodeMapper) throws IOException {
        int currentLine = method.getJavaSourceStartLineNumber();

        String methodName = method.getName();
        String returnType = method.getReturnType().toString();
        List<Type> parameterTypes = method.getParameterTypes();

        // regex pattern to match the method declaration
        String methodPattern = buildMethodPattern(returnType, methodName, parameterTypes);
        Pattern pattern = Pattern.compile(methodPattern);
        StringBuilder accumulatedLines = new StringBuilder();
        int methodDeclarationLine = -1;

        for (int i = 0; i < 20 && currentLine > 0; currentLine--) {
            String line = srcCodeMapper.getCodeLine(currentLine).trim();
            if (line.isEmpty()) {
                continue;
            }

            accumulatedLines.insert(0, line + " ");

            // checking if these match the regex per that method. (this is a Java.Regex matcher, not one of mine
            // naming a big confusing
            Matcher regexMatcher = pattern.matcher(accumulatedLines.toString());
            if (regexMatcher.find()) {
                methodDeclarationLine = currentLine;
                break;
            }
        }

        return methodDeclarationLine != -1 ? methodDeclarationLine : currentLine;
    }

    private static String buildMethodPattern(String returnType, String methodName, List<Type> parameterTypes) {
        StringBuilder paramsPattern = new StringBuilder();
        paramsPattern.append("\\(");
        for (int i = 0; i < parameterTypes.size(); i++) {
            paramsPattern.append(".*");
            if (i < parameterTypes.size() - 1) {
                paramsPattern.append(",");
            }
        }
        paramsPattern.append("\\)");

        String methodPattern = String.format(
                ".*\\b%s\\b\\s+\\b%s\\b\\s*%s.*",
                Pattern.quote(returnType),
                Pattern.quote(methodName),
                paramsPattern
        );
        return methodPattern;
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
