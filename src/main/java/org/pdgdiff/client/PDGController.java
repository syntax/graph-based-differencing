package org.pdgdiff.client;

import org.pdgdiff.Main;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

// TODO: Implement this controller to generate PDGs for uploaded classes
@Controller
public class PDGController {

    @PostMapping("/generate")
    public String generatePDG(@RequestParam("class1") String class1Content,
                              @RequestParam("class2") String class2Content,
                              Model model) {
        try {
            // Save the class contents to files
            saveClassToFile(class1Content, "Class1");
            saveClassToFile(class2Content, "Class2");

            // Compile the saved classes
            compileClass("Class1");
            compileClass("Class2");

            // Generate the PDGs for both classes
            String pdg1 = generatePDGForCompiledClass("Class1");
            String pdg2 = generatePDGForCompiledClass("Class2");

            // Add the generated PDGs to the model to display in the result page
            model.addAttribute("pdg1", pdg1);
            model.addAttribute("pdg2", pdg2);

        } catch (Exception e) {
            model.addAttribute("error", "Error generating PDG: " + e.getMessage());
        }

        return "result";  // Return the view displaying PDGs
    }

    private void saveClassToFile(String classContent, String className) throws IOException {
        File file = new File("src/main/java/uploaded_classes/" + className + ".java");
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.write(classContent);
        }
    }

    // TODO: considering soot takes java binaries from target to analysem need to compile the classes first
    private void compileClass(String className) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        String filePath = "src/main/java/uploaded_classes/" + className + ".java";
        compiler.run(null, null, null, filePath);
    }

    // TODO: need
    private String generatePDGForCompiledClass(String className) {
        // Assuming the Main class is used to generate the PDG TODO check this
        try {
            Main.main(new String[]{});
            return "PDG for " + className + " generated successfully!";
        } catch (Exception e) {
            return "Error generating PDG for " + className + ": " + e.getMessage();
        }
    }
}
