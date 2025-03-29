package org.pdgdiff.util;

import soot.G;
import soot.Scene;
import soot.options.Options;

import java.util.Collections;

/**
 * SootInitializer class to initialize Soot, the static analysis framework of this specific implementation of the
 * approach with the necessary configurations for PDG generation.
 */
public class SootInitializer {

    public static void initializeSoot(String dir) {
        resetSoot();

        // setting soot options
        Options.v().set_prepend_classpath(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_output_format(Options.output_format_jimple);
        Options.v().set_verbose(true); // Debug output

        // The following phase options are configured to preserve the original code structure, as well as poss.
        // read https://www.sable.mcgill.ca/soot/tutorial/phase/phase.html
        // in some cases however this is not possible because of how soot constructs Jimple, this is a limitation of
        // the implementation of this approach
        Options.v().set_keep_line_number(true);

        Options.v().setPhaseOption("jb", "use-original-names:true");
        Options.v().setPhaseOption("jb", "use-original-bytecode:true");
        Options.v().setPhaseOption("jj", "simplify-off:true");

        Options.v().setPhaseOption("jb.dce", "enabled:false");  // Disable dead code elimination
        Options.v().setPhaseOption("jb.dae", "enabled:false");  // Disable dead assignment elimination
        Options.v().setPhaseOption("jb.uce", "enabled:false");  // Disable unreachable code elimination
        Options.v().setPhaseOption("jb.cp", "enabled:false");  // Disable const propagation
        Options.v().setPhaseOption("jb.ule", "enabled:false");  // Disable unused local elimination
        Options.v().setPhaseOption("jop", "enabled:false");     // Disable optimizations like const folding
        Options.v().setPhaseOption("wjop", "enabled:false");    // Disable whole-program optimizations

        Options.v().setPhaseOption("jb.tr", "enabled:false");   // Disable transformation on control flow
        Options.v().setPhaseOption("bb", "enabled:false");      // Disable basic block merging or splitting
        Options.v().setPhaseOption("jap", "enabled:false");     // Disable aggregation
        Options.v().setPhaseOption("jtp.ls", "enabled:false");  // Disable loop simplification
        Options.v().setPhaseOption("jop.uce", "enabled:false"); // Disable unreachable code elimination
        Options.v().setPhaseOption("jop.cpf", "enabled:false");


        Options.v().set_soot_classpath(dir);
        Options.v().set_process_dir(Collections.singletonList(dir));

        Options.v().set_whole_program(true);
        Options.v().set_no_bodies_for_excluded(true);


        // finally loading necessary classes into the soot scene
        Scene.v().loadNecessaryClasses();
    }


    // Method to reset Soot (clean up)
    public static void resetSoot() {
        G.reset();
    }
}
