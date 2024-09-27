## Graph Based Code differencing

_Objectives of this repository: Create a graph-based differencing approach, with some corresonding tool_

This application is designed to take two Java classes, and produce a graph-based representation of the differences between them. 
It represents both classes as a [Program Dependence Graph](https://dl.acm.org/doi/10.1145/24039.24041) (generated through [Soot](https://github.com/soot-oss/soot) )and uses heuristics (WIP) to perform graph isomorphism.
The graph is to be represented as a JSON object, which can be visualized using a tool like [D3.js](https://d3js.org/), or as a .dot file for human readability.


### Preliminary list of dependencies
 - Java 8
 - Springboot 2.5.x
 - Maven 3.6.x
 - Soot 4.3.0