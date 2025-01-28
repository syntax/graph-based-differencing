package org.pdgdiff.edit;

import org.pdgdiff.edit.model.*;
import org.pdgdiff.io.JsonOperationSerializer;
import org.pdgdiff.io.OperationSerializer;
import org.pdgdiff.util.CodeAnalysisUtils;
import org.pdgdiff.util.SourceCodeMapper;
import soot.Modifier;
import soot.SootClass;
import soot.SootField;
import soot.util.Chain;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class ClassMetadataDiffGenerator {

    public static void generateClassMetadataDiff(
            SootClass srcClass,
            SootClass dstClass,
            String srcSourceFilePath,
            String dstSourceFilePath,
            String outputFileName
    ) throws IOException {
        Set<EditOperation> editScriptSet = new HashSet<>();

        SourceCodeMapper srcCodeMapper = new SourceCodeMapper(srcSourceFilePath);
        SourceCodeMapper dstCodeMapper = new SourceCodeMapper(dstSourceFilePath);

        // cmp class metadata
        compareClassMetadata(srcClass, dstClass, srcCodeMapper, dstCodeMapper, editScriptSet);

        // cmp fields
        compareFields(srcClass, dstClass, srcCodeMapper, dstCodeMapper, editScriptSet);

        // convert the set to a list and output
        List<EditOperation> editScript = new ArrayList<>(editScriptSet);
        exportEditScript(editScript, outputFileName);
    }

    private static void compareClassMetadata(
            SootClass srcClass,
            SootClass dstClass,
            SourceCodeMapper srcCodeMapper,
            SourceCodeMapper dstCodeMapper,
            Set<EditOperation> editScriptSet
    ) throws IOException {
        // compare class modifiers
        if (srcClass.getModifiers() != dstClass.getModifiers()) {
            int srcClassLineNumber = CodeAnalysisUtils.getClassLineNumber(srcClass, srcCodeMapper);
            int dstClassLineNumber = CodeAnalysisUtils.getClassLineNumber(dstClass, dstCodeMapper);

            String srcClassDeclaration = CodeAnalysisUtils.getClassDeclaration(srcClass, srcCodeMapper);
            String dstClassDeclaration = CodeAnalysisUtils.getClassDeclaration(dstClass, dstCodeMapper);

            EditOperation classUpdate = new Update(
                    null, // no node associated
                    srcClassLineNumber,
                    dstClassLineNumber,
                    srcClassDeclaration,
                    dstClassDeclaration,
                    new SyntaxDifference("ClassMetadataDiff: Class modifiers differ")
            );

            editScriptSet.add(classUpdate);
        }
    }


    // in an ideal world this would also be able to compare uses of a field in the entire body, then I would be able to
    // account for rename refactors in the code base quite cleverly, maybe somethnig to look into.

    private static void compareFields(
            SootClass srcClass,
            SootClass dstClass,
            SourceCodeMapper srcCodeMapper,
            SourceCodeMapper dstCodeMapper,
            Set<EditOperation> editScriptSet
    ) throws IOException {
        Chain<SootField> srcFields = srcClass.getFields();
        Chain<SootField> dstFields = dstClass.getFields();

        Map<String, SootField> srcFieldMap = new HashMap<>();
        Map<String, SootField> dstFieldMap = new HashMap<>();

        // TODO: look into using soots own 'Chain' for this
        for (SootField field : srcFields) {
            srcFieldMap.put(field.getName(), field);
        }

        for (SootField field : dstFields) {
            dstFieldMap.put(field.getName(), field);
        }

        // Matching fields by name, type, and modifiers to try and report update instructions where sensible
        Set<String> matchedFields = new HashSet<>();

        // firstly attempting to match by name
        for (SootField srcField : srcFields) {
            String fieldName = srcField.getName();
            SootField dstField = dstFieldMap.get(fieldName);

            if (dstField != null) {
                matchedFields.add(fieldName);

                if (!fieldsAreEqual(srcField, dstField)) {
                    // update if field types or modifiers differ
                    int oldLineNumber = CodeAnalysisUtils.getFieldLineNumber(srcField, srcCodeMapper);
                    int newLineNumber = CodeAnalysisUtils.getFieldLineNumber(dstField, dstCodeMapper);
                    String oldCodeSnippet = CodeAnalysisUtils.getFieldDeclaration(srcField, srcCodeMapper);
                    String newCodeSnippet = CodeAnalysisUtils.getFieldDeclaration(dstField, dstCodeMapper);

                    EditOperation fieldUpdate = new Update(
                            null,
                            oldLineNumber,
                            newLineNumber,
                            oldCodeSnippet,
                            newCodeSnippet,
                            new SyntaxDifference("ClassMetadataDiff: Field " + fieldName + " differs")
                    );
                    editScriptSet.add(fieldUpdate);
                }
            }
        }

        // secondary matching by type / modifier
        for (SootField srcField : srcFields) {
            String fieldName = srcField.getName();
            if (matchedFields.contains(fieldName)) continue;

            // look for a destination field with similar properties
            SootField bestMatch = null;
            for (SootField dstField : dstFields) {
                if (matchedFields.contains(dstField.getName())) continue;

                if (fieldsAreSimilar(srcField, dstField)) {
                    bestMatch = dstField;
                    break;
                }
            }

            if (bestMatch != null) {
                // field has a close match, so treat as an update
                matchedFields.add(bestMatch.getName());
                int oldLineNumber = CodeAnalysisUtils.getFieldLineNumber(srcField, srcCodeMapper);
                int newLineNumber = CodeAnalysisUtils.getFieldLineNumber(bestMatch, dstCodeMapper);
                String oldCodeSnippet = CodeAnalysisUtils.getFieldDeclaration(srcField, srcCodeMapper);
                String newCodeSnippet = CodeAnalysisUtils.getFieldDeclaration(bestMatch, dstCodeMapper);

                EditOperation fieldUpdate = new Update(
                        null,
                        oldLineNumber,
                        newLineNumber,
                        oldCodeSnippet,
                        newCodeSnippet,
                        new SyntaxDifference("ClassMetadataDiff: Field " + fieldName + " differs")
                );
                editScriptSet.add(fieldUpdate);
            } else {
                // no similar field found, treat as a delete
                int lineNumber = CodeAnalysisUtils.getFieldLineNumber(srcField, srcCodeMapper);
                String codeSnippet = CodeAnalysisUtils.getFieldDeclaration(srcField, srcCodeMapper);
                editScriptSet.add(new Delete(null, lineNumber, codeSnippet));
            }
        }

        // cleanup with insertion operations
        for (SootField dstField : dstFields) {
            if (!matchedFields.contains(dstField.getName())) {
                int lineNumber = CodeAnalysisUtils.getFieldLineNumber(dstField, dstCodeMapper);
                String codeSnippet = CodeAnalysisUtils.getFieldDeclaration(dstField, dstCodeMapper);
                editScriptSet.add(new Insert(null, lineNumber, codeSnippet));
            }
        }
    }


    private static boolean fieldsAreSimilar(SootField field1, SootField field2) {
        // check if same protectness and type
        // cannot compare actual objects (getType()) because these are loaded in difference Soot Scenes, and hence dont
        // hash as expected with .equals(), so using the string repr of each!!!
        // todo check isStatic, isFinal, etc. and consider name, annotations, initial values
        return  ((field1.getModifiers() & Modifier.PUBLIC) == (field2.getModifiers() & Modifier.PUBLIC) ||
                (field1.getModifiers() & Modifier.PRIVATE) == (field2.getModifiers() & Modifier.PRIVATE) ||
                (field1.getModifiers() & Modifier.PROTECTED) == (field2.getModifiers() & Modifier.PROTECTED))
                & (field1.getType().toString().equals(field2.getType().toString()));
    }

    private static boolean fieldsAreEqual(SootField field1, SootField field2) {
        // cmp field types
        if (!field1.getType().equals(field2.getType())) {
            return false;
        }
        // cmp modifiers
        if (field1.getModifiers() != field2.getModifiers()) {
            return false;
        }
        // TODO: cmp annotations or initial values if necessary
        return true;
    }

    private static void exportEditScript(List<EditOperation> editScript, String outputFileName) {
        try (Writer writer = new FileWriter(outputFileName)) {
            OperationSerializer serializer = new JsonOperationSerializer(editScript);
            serializer.writeTo(writer);
            System.out.println("Class metadata diff exported to: " + outputFileName);
        } catch (Exception e) {
            System.err.println("Failed to export class metadata diff to " + outputFileName);
            e.printStackTrace();
        }
    }
}
