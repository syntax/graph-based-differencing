package org.pdgdiff.matching;

import org.pdgdiff.edit.ClassMetadataDiffGenerator;
import org.pdgdiff.edit.EditDistanceCalculator;
import org.pdgdiff.edit.EditScriptGenerator;
import org.pdgdiff.edit.RecoveryProcessor;
import org.pdgdiff.edit.model.EditOperation;
import org.pdgdiff.export.DiffGraphExporter;
import org.pdgdiff.graph.CycleDetection;
import org.pdgdiff.graph.GraphTraversal;
import org.pdgdiff.graph.PDG;
import soot.SootClass;


import soot.SootMethod;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.pdgdiff.export.EditScriptExporter.*;

public class DiffEngine {

    private static final List<EditOperation> aggregatedEditScripts = new ArrayList<>();
    private static final boolean debug = false;


    public static void difference(List<PDG> pdgList1, List<PDG> pdgList2,
                                  StrategySettings strategySettings, String srcSourceFilePath, String dstSourceFilePath) throws IOException {

        GraphMatcher matcher = GraphMatcherFactory.createMatcher(strategySettings.matchingStrategy, pdgList1, pdgList2);
        // for each graph print the size of its nodes and if it has a cycle
        if (debug) pdgList1.forEach(pdg -> {
            System.out.println("------");
            System.out.println(pdg.getCFG().getBody().getMethod().getSignature());
            System.out.println("Node count" + GraphTraversal.getNodeCount(pdg));
            CycleDetection.hasCycle(pdg);
        });
        // perform the actual graph matching
        System.out.println("-> Beginning matching PDGs using strategy: " + strategySettings.matchingStrategy);
        GraphMapping graphMapping = matcher.matchPDGLists();

        // TODO: clean up debug print stmts
        System.out.println("--> Graph matching complete using strategy: " + strategySettings.matchingStrategy);

        // Handle unmatched graphs, i.e. additions or deletions of methods to the versions
        List<PDG> unmatchedInList1 = pdgList1.stream()
                .filter(pdg -> !graphMapping.getGraphMapping().containsKey(pdg))
                .collect(Collectors.toList());

        List<PDG> unmatchedInList2 = pdgList2.stream()
                .filter(pdg -> !graphMapping.getGraphMapping().containsValue(pdg))
                .collect(Collectors.toList());

        // Generate edit scripts for unmatched methods
        generateEditScriptsForUnmatched(unmatchedInList1, unmatchedInList2, srcSourceFilePath, dstSourceFilePath, strategySettings);
        exportGraphMappings(graphMapping, pdgList1, pdgList2, "out/");

        DiffGraphExporter.exportDiffPDGs(
                graphMapping,
                pdgList1,
                pdgList2,
                "out/delta-graphs/"
        );

        graphMapping.getGraphMapping().forEach((srcPDG, dstPDG) -> {
            String method1 = srcPDG.getCFG().getBody().getMethod().getSignature();
            String method2 = dstPDG.getCFG().getBody().getMethod().getSignature();
            System.out.println("---\n> PDG from class 1: " + method1 + " is matched with PDG from class 2: " + method2);
            if (debug) {
                System.out.println(GraphTraversal.getNodeCount(srcPDG));
                CycleDetection.hasCycle(srcPDG);
                System.out.println(GraphTraversal.getNodeCount(dstPDG));
                CycleDetection.hasCycle(dstPDG);
            }
            NodeMapping nodeMapping = graphMapping.getNodeMapping(srcPDG);
            if (nodeMapping != null) {
                System.out.println("--- Node Mapping:");
                nodeMapping.printMappings();

                try {

                    // collecting of 'metadata' of the code, i.e. function signatures and fields, will occur here. it should not have
                    // any impact on the actual matching process, to ensure that this is as semantic and language-agnostic as possible.

                    SootMethod srcObj = srcPDG.getCFG().getBody().getMethod();
                    SootMethod destObj = dstPDG.getCFG().getBody().getMethod();

                    List<EditOperation> editScript = EditScriptGenerator.generateEditScript(srcPDG, dstPDG, graphMapping,
                            srcSourceFilePath, dstSourceFilePath, srcObj, destObj);

                    List<EditOperation> recoveredEditScript = RecoveryProcessor.recoverMappings(editScript, strategySettings.recoveryStrategy);

                    int editDistance = EditDistanceCalculator.calculateEditDistance(recoveredEditScript);
                    System.out.println("--- Edit information ---");
                    System.out.println("-- Edit Distance: " + editDistance);

                    System.out.println("-- Edit Script:");
                    for (EditOperation op : recoveredEditScript) {
                        System.out.println(op);
                    }

                    // serialise and export
                    aggregatedEditScripts.addAll(recoveredEditScript);
                    exportEditScript(recoveredEditScript, method1, method2, strategySettings);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // build edit script for class mappings at this point
        if (!pdgList1.isEmpty() && !pdgList2.isEmpty()) {
            SootClass srcClass = pdgList1.get(0).getCFG().getBody().getMethod().getDeclaringClass();
            SootClass dstClass = pdgList2.get(0).getCFG().getBody().getMethod().getDeclaringClass();

            // todo, if one of these is empty, i need to mark it as an insertion or deletion of the entire class. so need to do a INSERT all or DELET all for clsas metadata
            List<EditOperation> metadataScript = ClassMetadataDiffGenerator.generateClassMetadataDiff(srcClass, dstClass, srcSourceFilePath, dstSourceFilePath);
            aggregatedEditScripts.addAll(metadataScript);
            exportEditScript(metadataScript, "metadata", "metadata", null);
        }

        if (strategySettings.isAggregateRecovery()) {
            List<EditOperation> recAggregatedEditScripts = RecoveryProcessor.recoverMappings(aggregatedEditScripts, strategySettings.recoveryStrategy);
            writeAggregatedEditScript(recAggregatedEditScripts, "out/diff.json", strategySettings);
        } else {
            writeAggregatedEditScript(aggregatedEditScripts, "out/diff.json", strategySettings);
        }
    }

    private static void generateEditScriptsForUnmatched(List<PDG> unmatchedInList1, List<PDG> unmatchedInList2,
                                                        String srcSourceFilePath, String dstSourceFilePath, StrategySettings strategySettings) {
        unmatchedInList1.forEach(pdg -> {
            try {
                SootMethod method = pdg.getCFG().getBody().getMethod();
                String methodSignature = pdg.getCFG().getBody().getMethod().getSignature();
                System.out.println("Unmatched method in List 1 (to be deleted): " + methodSignature);

                List<EditOperation> editScript = EditScriptGenerator.generateDeleteScript(pdg, srcSourceFilePath, method);
                List<EditOperation> recoveredEditScript = RecoveryProcessor.recoverMappings(editScript, strategySettings.recoveryStrategy);
                aggregatedEditScripts.addAll(recoveredEditScript);
                exportEditScript(recoveredEditScript, methodSignature, "DELETION", strategySettings);
            } catch (Exception e) {
                System.err.println("Failed to generate delete script for unmatched method in List 1");
                e.printStackTrace();
            }
        });

        unmatchedInList2.forEach(pdg -> {
            try {
                SootMethod method = pdg.getCFG().getBody().getMethod();
                String methodSignature = pdg.getCFG().getBody().getMethod().getSignature();
                System.out.println("Unmatched method in List 2 (to be added): " + methodSignature);

                List<EditOperation> editScript = EditScriptGenerator.generateAddScript(pdg, dstSourceFilePath, method);
                List<EditOperation> recoveredEditScript = RecoveryProcessor.recoverMappings(editScript, strategySettings.recoveryStrategy);
                aggregatedEditScripts.addAll(recoveredEditScript);
                exportEditScript(recoveredEditScript, "INSERTION", methodSignature, strategySettings);
            } catch (Exception e) {
                System.err.println("Failed to generate add script for unmatched method in List 2");
                e.printStackTrace();
            }
        });
    }


}
