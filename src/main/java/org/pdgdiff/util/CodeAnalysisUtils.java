package org.pdgdiff.util;

import soot.*;
import soot.tagkit.LineNumberTag;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This class aims to assist with parsing when Soot struggles.
 * A lot of these functions are to supplement Soot when it struggles to parse, and have a O(n) complexity. As further
 * work this could probably be optimised further.
 */
public class CodeAnalysisUtils {

    public static int getClassLineNumber(SootClass sootClass, SourceCodeMapper codeMapper) {
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

    public static String getClassDeclaration(SootClass sootClass, SourceCodeMapper codeMapper) {
        int lineNumber = getClassLineNumber(sootClass, codeMapper);
        if (lineNumber > 0) {
            return codeMapper.getCodeLine(lineNumber).trim();
        }
        return "";
    }

    public static int getFieldLineNumber(SootField field, SourceCodeMapper codeMapper) {
        int lineNumber = field.getJavaSourceStartLineNumber();
        if (lineNumber > 0) {
            return lineNumber;
        }

        String fieldName = field.getName();
        String fieldType = field.getType().toString();

        // parse simple type name without full package declaration (e.g. String instead of java.lang.String)
        String simpleFieldType = fieldType.substring(fieldType.lastIndexOf('.') + 1);
        // regex pattern, possibility of missed case here
        String fieldPattern = String.format(
                ".*\\b(?:public|protected|private|static|final|transient|volatile|abstract|synchronized|native|strictfp|\\s)*\\b%s\\s*(?:<[^>]+>)?\\s+%s\\b.*;",
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


    public static String getFieldDeclaration(SootField field, SourceCodeMapper codeMapper) {
        int lineNumber = getFieldLineNumber(field, codeMapper);
        if (lineNumber > 0) {
            return codeMapper.getCodeLine(lineNumber).trim();
        }
        return "";
    }


    public static int[] getMethodLineRange(SootMethod method, SourceCodeMapper srcCodeMapper) {
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

    public static List<String> getParamTokensAndLines(
            SootMethod method,
            SourceCodeMapper mapper,
            List<Integer> paramLinesOut
    ) {
        paramLinesOut.clear();
        List<String> paramTokens = new ArrayList<>();
        int[] range = getMethodLineRange(method, mapper);
        if (range[0] < 0 || range[1] < 0) {
            return paramTokens;
        }

        int startLine = range[0];
        int endLine   = range[1];
        int totalLines = mapper.getTotalLines();

        // collect the lines for the signature block
        StringBuilder sb = new StringBuilder();
        for (int ln = startLine; ln <= Math.min(endLine, totalLines); ln++) {
            sb.append(mapper.getCodeLine(ln)).append("\n");
        }
        String signatureText = sb.toString();

        int openParenIndex  = signatureText.indexOf('(');
        int closeParenIndex = signatureText.lastIndexOf(')');
        if (openParenIndex < 0 || closeParenIndex < 0 || closeParenIndex < openParenIndex) {
            return paramTokens; // no parameters
        }

        String paramBlock = signatureText.substring(openParenIndex + 1, closeParenIndex).trim();
        if (paramBlock.isEmpty()) {
            return paramTokens;
        }

        // naive split on commas
        String[] rawParams = paramBlock.split(",");

        // which line contains the param substr is assigned to be line num of that param
        List<String> lines = new ArrayList<>();
        for (int ln = startLine; ln <= endLine; ln++) {
            lines.add(mapper.getCodeLine(ln));
        }

        for (String raw : rawParams) {
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int bestLine = startLine; // fallback
            for (int offset = 0; offset < lines.size(); offset++) {
                if (lines.get(offset).contains(trimmed)) {
                    bestLine = startLine + offset;
                    break;
                }
            }
            paramTokens.add(trimmed);
            paramLinesOut.add(bestLine);
        }
        return paramTokens;
    }

    public static List<String> getMethodAnnotationsWithLines(
            SootMethod method,
            SourceCodeMapper codeMapper,
            List<Integer> annoLinesOut
    ) {
        annoLinesOut.clear();
        List<String> annoTokens = new ArrayList<>();
        int[] range = getMethodLineRange(method, codeMapper);
        if (range[0] <= 0 || range[1] <= 0) {
            return annoTokens;
        }

        int startLine = range[0];
        // climb upward until finding lines not starting with '@' i.e. non annotations
        int lineNum = startLine - 1;
        while (lineNum > 0) {
            String line = codeMapper.getCodeLine(lineNum).trim();
            if (line.startsWith("@")) {
                String[] rawAnnos = line.split("\\s+@");
                for (int i = 0; i < rawAnnos.length; i++) {
                    String annoRaw = (i == 0) ? rawAnnos[i] : "@" + rawAnnos[i];
                    annoRaw = annoRaw.trim();
                    if (!annoRaw.isEmpty()) {
                        annoTokens.add(annoRaw);
                        annoLinesOut.add(lineNum);
                    }
                }
                lineNum--;
            } else {
                break;
            }
        }
        return annoTokens;
    }

    public static List<Integer> getAnnotationsLineNumbers(SootMethod method, SourceCodeMapper codeMapper) {
        List<Integer> annotationLines = new ArrayList<>();
        int[] range = getMethodLineRange(method, codeMapper);
        if (range[0] <= 0) {
            return annotationLines;
        }
        int startLine = range[0];

        // crawl upwards until reaching an empty line or a line that does not start with an @ i.e. non annotations
        int lineNum = startLine - 1;
        while (lineNum > 0) {
            String code = codeMapper.getCodeLine(lineNum).trim();
            if (code.startsWith("@")) {
                annotationLines.add(lineNum);
                lineNum--;
            } else if (code.isEmpty()) {
                break;
            } else {
                break;
            }
        }
        return annotationLines;
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
