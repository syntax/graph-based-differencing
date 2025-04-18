package org.pdgdiff;

import org.pdgdiff.edit.RecoveryProcessor;
import org.pdgdiff.graph.GraphExporter;
import org.pdgdiff.graph.GraphGenerator;
import org.pdgdiff.graph.PDG;
import org.pdgdiff.matching.GraphMatcherFactory;
import org.pdgdiff.matching.DiffEngine;
import org.pdgdiff.matching.StrategySettings;
import org.pdgdiff.util.SootInitializer;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

import java.util.*;

import static org.pdgdiff.export.EditScriptExporter.copyResultsToOutput;

public class Main {

    enum FILE_VERSION {
        SOURCE,
        DEST
    }

    public static void main(String[] args) {
        System.out.println("Starting PDG Diff...");
        String class1Name, class2Name;
        String srcSourceFilePath, dstSourceFilePath;
        String beforeDir, afterDir;

        // defaults
        GraphMatcherFactory.MatchingStrategy matchingStrategy = GraphMatcherFactory.MatchingStrategy.VF2;
        org.pdgdiff.edit.RecoveryProcessor.RecoveryStrategy recoveryStrategy =  RecoveryProcessor.RecoveryStrategy.CLEANUP_AND_FLATTEN;
        boolean aggregateRecovery = true;


        if (args.length < 6) {
            System.out.println("WARNING: Insufficient arguments provided.");
            System.out.println("Usage: java org.pdgdiff.Main <beforeSourcePath> <afterSourcePath> <beforeCompiledDir> <afterCompiledDir> <beforeClassName> <afterClassName> [<matchingStrategy>] [<recoveryStrategy>]");
            System.out.println("Using Maven: mvn clean compile && mvn exec:java -Dexec.mainClass=\"org.pdgdiff.Main\" -Dexec.args=\"<beforeSourcePath> <afterSourcePath> <beforeCompiledDir> <afterCompiledDir> <beforeClassName> <afterClassName>\"\n");
            System.out.println("> Using hardcoded information due to insufficient arguments...");

            //  !!!! To run on datasets, use the following !!!!

            // NESTED CLASSES

//            String commit = "918ef4a7ca8362efd45f67636bc8bd094f5a4414";
//            String project = "signal-server";
//            String filename = "IterablePair";
//
//
//            beforeDir = "./benchmark/datasets/gh-java/before/" + project + "/" + commit + "/compiled";
//            afterDir = "./benchmark/datasets/gh-java/after/" + project + "/" + commit + "/compiled";
//            class1Name = "org.whispersystems.textsecuregcm.util.IterablePair";
//            class2Name = "org.whispersystems.textsecuregcm.util.IterablePair";
//            srcSourceFilePath = "./benchmark/datasets/gh-java/before/" + project + "/" + commit + "/" + filename +".java";
//            dstSourceFilePath = "./benchmark/datasets/gh-java/after/" + project + "/" + commit + "/" + filename +".java";



            //./gumtree webdiff ../../soot-pdg/benchmark/datasets/gh-java/before/google-guava/bbab2ce3c162b244119bdc22a990d7b75fdef0af/Objects.java ../../soot-pdg/benchmark/datasets/gh-java/after/google-guava/bbab2ce3c162b244119bdc22a990d7b75fdef0af/Objects.java

            // !!!! to use on local test classes, use the following !!!!
            class1Name = "org.pdgdiff.testclasses.TestFileBefore";
            class2Name = "org.pdgdiff.testclasses.TestFileAfter";

            srcSourceFilePath = "src/main/java/org/pdgdiff/testclasses/TestFileBefore.java";
            dstSourceFilePath = "src/main/java/org/pdgdiff/testclasses/TestFileAfter.java";

            beforeDir = System.getProperty("user.dir") + "/target/classes";
            afterDir = System.getProperty("user.dir") + "/target/classes";

        } else {
            // parsing the arguments from the cli
            // as an example;
            // mvn exec:java -Dexec.mainClass="org.pdgdiff.Main" -Dexec.args="./src/main/java/org/pdgdiff/testclasses/TestFileBefore.java ./src/main/java/org/pdgdiff/testclasses/TestFileAfter.java ./target/classes ./target/classes org.pdgdiff.testclasses.TestFileBefore org.pdgdiff.testclasses.TestFileAfter"
            srcSourceFilePath = args[0];
            dstSourceFilePath = args[1];
            beforeDir = args[2];
            afterDir = args[3];
            class1Name = args[4];
            class2Name = args[5];


            // optionally parse strategy, otherwise default
            if (args.length >= 7) {
                try {
                    matchingStrategy = GraphMatcherFactory.MatchingStrategy.valueOf(args[6].toUpperCase());
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid matching strategy provided, using default: VF2");
                }
            }
            if (args.length >= 8) {
                try {
                    recoveryStrategy = org.pdgdiff.edit.RecoveryProcessor.RecoveryStrategy.valueOf(args[7].toUpperCase());
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid recovery strategy provided, using default: CLEANUP_AND_FLATTEN");
                }
            }

        }



        GraphExporter.clearOutputFolder("out");
        SootInitializer.initializeSoot(beforeDir);

        // Load all classes in the program
        Scene.v().loadNecessaryClasses();

        try {
            // retrieve the classes from the Soot Scene
            SootInitializer.initializeSoot(beforeDir);
            Scene.v().loadNecessaryClasses();
            Map<String, SootClass> beforeClasses = collectNestedClassesByFqn(class1Name);

            SootInitializer.initializeSoot(afterDir);
            Scene.v().loadNecessaryClasses();
            Map<String, SootClass> afterClasses = collectNestedClassesByFqn(class2Name);


            if (beforeClasses.size() == 1 && afterClasses.size() == 1) {
                // standard case, where only one class in each

                System.out.println("\nDetected exactly ONE class in BEFORE and AFTER => using single-class logic.");

                SootClass beforeFile = beforeClasses.values().iterator().next();
                SootClass afterFile  = afterClasses.values().iterator().next();

                List<PDG> pdgsClass1 = generatePDGsForClass(beforeFile, FILE_VERSION.SOURCE);
                List<PDG> pdgsClass2 = generatePDGsForClass(afterFile,  FILE_VERSION.DEST);

                System.out.println("PDGs generated for " + beforeFile.getName() + ": " + pdgsClass1.size());
                System.out.println("PDGs generated for " + afterFile.getName()  + ": " + pdgsClass2.size());

                if (pdgsClass1.isEmpty() || pdgsClass2.isEmpty()) {
                    System.out.println("ERROR: No PDGs generated for one or both classes. Probably no concrete methods. Exiting...");
                    return;
                }
                StrategySettings strategySettings = new StrategySettings(recoveryStrategy, matchingStrategy, aggregateRecovery);
                try {
                    DiffEngine.difference(pdgsClass1, pdgsClass2, strategySettings, srcSourceFilePath, dstSourceFilePath);

                    copyResultsToOutput(srcSourceFilePath, dstSourceFilePath);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("An error occurred during single-class diff: " + e.getMessage());
                }
            } else {
                // multi-class version
                // NOTE: neglecting this a bit because its a uncommon case, but need to handle class insertions and deletions properly,
                //  e,g, need to handle field insertions deletions. also for some reason, nested class line nums seem to be slightly mismatched. for whatever reason.
                // TODO: improvemnet: handle class comparison etc properly.

                Map<String, List<PDG>> beforePdgsMap = new HashMap<>();
                for (Map.Entry<String, SootClass> entry : beforeClasses.entrySet()) {
                    String fqn = entry.getKey();
                    SootClass sc = entry.getValue();
                    List<PDG> pdgs = generatePDGsForClass(sc, FILE_VERSION.SOURCE);
                    beforePdgsMap.put(fqn, pdgs);
                }

                Map<String, List<PDG>> afterPdgsMap = new HashMap<>();
                for (Map.Entry<String, SootClass> entry : afterClasses.entrySet()) {
                    String fqn = entry.getKey();
                    SootClass sc = entry.getValue();
                    List<PDG> pdgs = generatePDGsForClass(sc, FILE_VERSION.DEST);
                    afterPdgsMap.put(fqn, pdgs);
                }

                System.out.println("\nPDGs for 'before' file total: " + beforePdgsMap.values().stream().mapToInt(List::size).sum());
                System.out.println("PDGs for 'after' file total: " + afterPdgsMap.values().stream().mapToInt(List::size).sum());


                if (beforePdgsMap.isEmpty() || afterPdgsMap.isEmpty()) {
                    System.out.println("ERROR: No PDGs generated for one or both classes. There are probably no concrete methods in the class. Exiting...");
                    return;
                }

                StrategySettings strategySettings = new StrategySettings(recoveryStrategy, matchingStrategy, aggregateRecovery);

                Set<String> allFqns = new HashSet<>();
                System.out.println("Before classes: " + beforePdgsMap.keySet());
                System.out.println("After classes: " + afterPdgsMap.keySet());

                allFqns.addAll(beforePdgsMap.keySet());
                allFqns.addAll(afterPdgsMap.keySet());

                System.out.println("All classes: " + allFqns);

                for (String fqn : allFqns) {
                    // nb: this obviously assumes classes have the same name when making this comparison
                    System.out.println("\n=== Comparing class: " + fqn + " ===");

                    List<PDG> beforePdgs = beforePdgsMap.getOrDefault(fqn, Collections.emptyList());
                    List<PDG> afterPdgs = afterPdgsMap.getOrDefault(fqn, Collections.emptyList());

                    // if that class has no pdgs in the before, but has some in the after, then it was added
                    System.out.println("Before PDGs: " + beforePdgs);
                    System.out.println("After PDGs: " + afterPdgs);
                    if (beforePdgs.isEmpty() && !afterPdgs.isEmpty()) {
                        // this class is added
                        System.out.println("Class " + fqn + " was ADDED in 'after'.");
                        DiffEngine.difference(Collections.emptyList(), afterPdgs, strategySettings,
                                srcSourceFilePath, dstSourceFilePath);
                    } else if (!beforePdgs.isEmpty() && afterPdgs.isEmpty()) {
                        // the class is deleted
                        System.out.println("Class " + fqn + " was DELETED in 'after'.");
                        DiffEngine.difference(beforePdgs, Collections.emptyList(), strategySettings,
                                srcSourceFilePath, dstSourceFilePath);
                    } else {
                        // class existed in both, so normal method-level PDG diff
                        System.out.println("Class " + fqn + " exists in both. Diffing method-level PDGs...");
                        DiffEngine.difference(beforePdgs, afterPdgs, strategySettings, srcSourceFilePath, dstSourceFilePath);
                    }
                }

                copyResultsToOutput(srcSourceFilePath, dstSourceFilePath);

                SootInitializer.resetSoot();
            }

        } catch (Exception e) {
            System.err.println("An error occurred while processing the classes: " + e.getMessage());
        }

        SootInitializer.resetSoot();
    }

    private static Map<String, SootClass> collectNestedClassesByFqn(String fqn) {
        Map<String, SootClass> result = new HashMap<>();
        for (SootClass sc : Scene.v().getApplicationClasses()) {
            String fullName = sc.getName(); // e.g. "org.whispersystems.textsecuregcm.util.IterablePair$ParallelIterator" aka fqn
            if (fullName.equals(fqn) || fullName.startsWith(fqn + "$")) {
                result.put(fullName, sc);
            }
        }
        System.out.println("Found " + result.size() + " classes in Soot that match FQN: " + fqn);
        for (SootClass sc : result.values()) {
            System.out.println("  -> " + sc.getName());
        }
        return result;
    }

    /**
     * method to generate PDGs for all methods in a given class and store them in a list
     */
    private static List<PDG> generatePDGsForClass(SootClass sootClass, FILE_VERSION version) {
        if (sootClass.getName().matches(".*\\$\\d+")) {
            // for now, ignoring differencing of anon or synthetic classes as often marked as 'DANGLING' by soot
            System.out.println("Skipping anonymous/synthetic class: " + sootClass.getName());
            return Collections.emptyList();
        }

        List<PDG> pdgList = new ArrayList<>();
        System.out.println("Generating PDGs for class: " + sootClass.getName());
        for (SootMethod method : sootClass.getMethods()) {
            if (method.isConcrete()) {
                try {
                    // retrieve the active body and generate the PDG
                    method.retrieveActiveBody();
                    System.out.println("Successfully retrieved active body for: " + method.getName() + " in " + sootClass.getName());

                    PDG pdg = GraphGenerator.constructPdg(method);
                    pdgList.add(pdg);
                    System.out.println("PDG generated for method: " + method.getName());

                    // likely pdg function names will clash across src/dest of the same file name, so need to specify version
                    String baseFileName;
                    if (version == FILE_VERSION.SOURCE) {
                        baseFileName = "out/src_pdg" + sootClass.getName() + "_" + method.getName();
                    } else {
                        baseFileName = "out/dst_pdg_" + sootClass.getName() + "_" + method.getName();
                    }
                    GraphExporter.exportPDG(pdg, baseFileName + ".dot", baseFileName + ".txt");
                } catch (Exception e) {
                    System.err.println("Failed to retrieve body or generate PDG for method: " + method.getName());
                    e.printStackTrace();
                }
            }
        }
        return pdgList;
    }
}
