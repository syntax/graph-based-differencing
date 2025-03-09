## Graph Based Code differencing

_Objectives of this repository: Create a graph-based differencing approach, with some corresonding tool_

This application is designed to take two Java classes, and produce a graph-based representation of the differences between them. 
It represents both classes as a [Program Dependence Graph](https://dl.acm.org/doi/10.1145/24039.24041) (generated through [Soot](https://github.com/soot-oss/soot)) and uses heuristics to perform graph isomorphism.
The application suggested which methods might have originated from one another and suggests edit scripts between methods in the source and destination file. 

Due to the fact that a PDG is often built from an intermediate representation for the sake of proper analysis, such as LLVM for a C/C++ program or Java Bytecode in this case, some syntactic differences won't be captured, and the algorithm will be limited to the granularity of the intermediate representation. For the purpose of a closer and more accurate
differencing, compiler optimisations are disabled for target comparison files both at the java compiler level and the Soot level. 
In some specific use cases, such as analysis efficiencies or trying to determine if programs are semantically identical, this tool should (🤞) be more useful than other differencing approaches.

**Please note;** This is a current WIP. The project is currently in the early stages of development, and the README will be updated as the project progresses. Documentation, in the form of a complete dissertation report, is in the works.

### How does this work?

![Overview](images/overview.png)

The complete process flow is described in the above visualisation. Data is read in from the source and destination files, and a PDG is generated for each. The PDGs are then compared using a graph isomorphism strategy, and the results are used to generate an pairing between methods. Each method pairing also has a node mapping, which is used to generate a edit script between two methods. A recovery method is applied to this to analyse operations using further heuristics, and these edit scripts are aggregated to create a final delta that summarises the changes between two files. 
Current recommended matching strategies, that are proven to work quite well in most cases include VF2 and GED.

### How can I visualise the changes?

After running the program with the preffered matching engine settings, the diff can be visualised in different ways. Most commonly, 
one can run the _py-visualise_ Flask app to view the diff in its side-by-side, text-based form. Alternatively, remaning more loyal to the graph-based differencing approach, the delta can be viewed at the Jimple level as a singular _delta_
graph. This can be used by exporting the dot file created in the delta-graph folder to a png, or similar.

![Delta](images/refactoredgraph.png)
**NB**: This delta has been edited slightly to ensure its readable on this README and not too large.

### Preliminary list of dependencies
 - Java 8
 - Springboot 2.5.x
 - Maven 3.6.x
 - Soot 4.3.0
 - Python 3.8.x or later

### Getting Started

To run,

Compile dependencies;
```bash
mvn compile
```

Run the application;
```bash
mvn exec:java -Dexec.mainClass="org.pdgdiff.Main"
```

Run the application with arguments for differencing;
```bash
mvn exec:java -Dexec.mainClass="org.pdgdiff.Main" -Dexec.args="<beforeSourcePath> <afterSourcePath> <beforeCompiledDir> <afterCompiledDir> <beforeClassName> <afterClassName>"
```

To run the current Flask frontend;
```bash
cd py-visualise
python3 app.py
```