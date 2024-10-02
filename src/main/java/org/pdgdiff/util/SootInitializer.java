package org.pdgdiff.util;

import soot.G;
import soot.Scene;
import soot.options.Options;

import java.util.Collections;

public class SootInitializer {

    // Method to initialize Soot configuration
    public static void initializeSoot() {
        resetSoot();
        // Set Soot options
        Options.v().set_prepend_classpath(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_output_format(Options.output_format_jimple);
        Options.v().set_verbose(true); // Debug output

        // TODO: Maintain code as close to original as possible when compiled
        // TODO: configure compiler and investigate this
        // read https://www.sable.mcgill.ca/soot/tutorial/phase/phase.html
        Options.v().set_keep_line_number(true);
        Options.v().set_no_bodies_for_excluded(true);
        Options.v().setPhaseOption("jb", "use-original-names:true");
        Options.v().setPhaseOption("jb.dce", "enabled:false");  // Disable dead code elimination
        Options.v().setPhaseOption("jop", "enabled:false");     // Disable optimizations like constant folding

        // Set the class path to your program's compiled classes
        String classPath = System.getProperty("user.dir") + "/target/classes";
        Options.v().set_soot_classpath(classPath);
        Options.v().set_process_dir(Collections.singletonList(classPath));

        // Whole program analysis
        Options.v().set_whole_program(false); // Investigating if this stops DCE

        // Load necessary classes
        Scene.v().loadNecessaryClasses();
    }

    // Method to reset Soot (clean up)
    public static void resetSoot() {
        G.reset();
    }
}
