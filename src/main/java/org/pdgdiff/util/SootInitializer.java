package org.pdgdiff.util;

import soot.G;
import soot.Scene;
import soot.options.Options;

import java.util.Collections;

public class SootInitializer {

    // Method to initialize Soot configuration
    public static void initializeSoot() {
        // Set Soot options
        Options.v().set_prepend_classpath(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_output_format(Options.output_format_jimple);
        Options.v().set_verbose(true); // Debug output

        // Set the class path to your program's compiled classes
        String classPath = System.getProperty("user.dir") + "/target/classes";
        Options.v().set_soot_classpath(classPath);
        Options.v().set_process_dir(Collections.singletonList(classPath));

        // Whole program analysis
        Options.v().set_whole_program(true);

        // Load necessary classes
        Scene.v().loadNecessaryClasses();
    }

    // Method to reset Soot (clean up)
    public static void resetSoot() {
        G.reset();
    }
}
