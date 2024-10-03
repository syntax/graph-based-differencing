package org.pdgdiff.util;

import soot.G;
import soot.Scene;
import soot.options.Options;

import java.util.Collections;

/**
 * SootInitializer class to initialize Soot with the necessary configurations for PDG generation.
 */
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
//        Options.v().set_no_bodies_for_excluded(true);
        Options.v().setPhaseOption("jb", "use-original-names:true");

        Options.v().setPhaseOption("jb.dce", "enabled:false");  // Disable dead code elimination
        Options.v().setPhaseOption("jb.dae", "enabled:false");  // Disable dead assignment elimination
        Options.v().setPhaseOption("jb.uce", "enabled:false");  // Disable unreachable code elimination
        Options.v().setPhaseOption("jb.cp", "enabled:false");  // Disable const propagation
        Options.v().setPhaseOption("jb.ule", "enabled:false");  // Disable unused local elimination
        Options.v().setPhaseOption("jop", "enabled:false");     // Disable optimizations like constant folding
        Options.v().setPhaseOption("wjop", "enabled:false");    // Disable whole-program optimizations

        Options.v().setPhaseOption("jb.tr", "enabled:false");   // Disable transformation (use original control flow)
        Options.v().setPhaseOption("bb", "enabled:false");      // Disable basic block merging or splitting
        Options.v().setPhaseOption("jap", "enabled:false");     // Disable aggregation
        Options.v().setPhaseOption("jtp.ls", "enabled:false");  // Disable loop simplification
        Options.v().setPhaseOption("jop.uce", "enabled:false"); // Disable unreachable code elimination (for good measure)
        Options.v().setPhaseOption("jop.cpf", "enabled:false");

        // Set the class path to your program's compiled classes
        String classPath = System.getProperty("user.dir") + "/target/classes";
        Options.v().set_soot_classpath(classPath);
        Options.v().set_process_dir(Collections.singletonList(classPath));

        // Whole program analysis
        Options.v().set_whole_program(false); // Investigating if this stops DCE
        Options.v().set_no_bodies_for_excluded(true);

        // Load necessary classes
        Scene.v().loadNecessaryClasses();
    }

    // Method to reset Soot (clean up)
    public static void resetSoot() {
        G.reset();
    }
}
