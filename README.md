## Graph Based Code differencing

_Objectives of this repository: Create a graph-based differencing approach, with some corresonding tool_

This application is designed to take two Java classes, and produce a graph-based representation of the differences between them. 
It represents both classes as a [Program Dependence Graph](https://dl.acm.org/doi/10.1145/24039.24041) (generated through [Soot](https://github.com/soot-oss/soot)) and uses heuristics (WIP) to perform graph isomorphism.
The application suggested which methods might have originated from one another and suggests edit scripts between methods in the source and destination file. 

Due to the fact that a PDG is often run on an intermediate representation, such as LLVM for a C/C++ program or Java Bytecode in this case, some syntactic differences won't be captured, and the algorithm will be limited to the granularity of the intermediate representation. For the purpose of a closer and more accurate
differencing, compiler optimisations are disabled for target comparison files both at the java compiler level and the Soot level. Soot 
In some use cases, such as analysis efficiencies or trying to determine if programs are semantically identical, this tool will be more useful.

**Please note;** This is a current WIP. The project is currently in the early stages of development, and the README will be updated as the project progresses.

Documentation, in the form of a complete dissertation report, is in the works.

### Preliminary list of dependencies
 - Java 8
 - Springboot 2.5.x
 - Maven 3.6.x
 - Soot 4.3.0

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

Run the front-end (spring-boot, wip);
```bash
mvn spring-boot:run
```

To run test suite (this largely checks heuristics and graph isomorphism algorithms);
```bash
mvn test
```