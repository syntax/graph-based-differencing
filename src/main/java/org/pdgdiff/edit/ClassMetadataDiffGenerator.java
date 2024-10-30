package org.pdgdiff.edit;

import org.pdgdiff.edit.model.Delete;
import org.pdgdiff.edit.model.EditOperation;
import org.pdgdiff.edit.model.Insert;
import org.pdgdiff.edit.model.Update;
import org.pdgdiff.io.JsonOperationSerializer;
import org.pdgdiff.io.OperationSerializer;
import org.pdgdiff.util.CodeAnalysisUtils;
import org.pdgdiff.util.SourceCodeMapper;
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
                    null
            );

            editScriptSet.add(classUpdate);
        }
    }

    private static void compareFields(
            SootClass srcClass,
            SootClass dstClass,
            SourceCodeMapper srcCodeMapper,
            SourceCodeMapper dstCodeMapper,
            Set<EditOperation> editScriptSet
    ) throws IOException {
        Chain<SootField> srcFields = srcClass.getFields();
        Chain<SootField> dstFields = dstClass.getFields();

        // TODO: prob can use soots own chain here for optimised lookup, but map is also O(1)
        Map<String, SootField> srcFieldMap = new HashMap<>();
        for (SootField field : srcFields) {
            srcFieldMap.put(field.getSignature(), field);
        }

        Map<String, SootField> dstFieldMap = new HashMap<>();
        for (SootField field : dstFields) {
            dstFieldMap.put(field.getSignature(), field);
        }

        // deletions and updates
        for (SootField srcField : srcFields) {
            String signature = srcField.getSignature();
            SootField dstField = dstFieldMap.get(signature);

            if (dstField == null) {
                // delete
                int lineNumber = CodeAnalysisUtils.getFieldLineNumber(srcField, srcCodeMapper);
                String codeSnippet = CodeAnalysisUtils.getFieldDeclaration(srcField, srcCodeMapper);
                editScriptSet.add(new Delete(null, lineNumber, codeSnippet));
            } else {
                if (!fieldsAreEqual(srcField, dstField)) {
                    // update
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
                            null
                    );
                    editScriptSet.add(fieldUpdate);
                }
            }
        }

        // insertion
        for (SootField dstField : dstFields) {
            String signature = dstField.getSignature();
            if (!srcFieldMap.containsKey(signature)) {
                int lineNumber = CodeAnalysisUtils.getFieldLineNumber(dstField, dstCodeMapper);
                String codeSnippet = CodeAnalysisUtils.getFieldDeclaration(dstField, dstCodeMapper);
                editScriptSet.add(new Insert(null, lineNumber, codeSnippet));
            }
        }
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
