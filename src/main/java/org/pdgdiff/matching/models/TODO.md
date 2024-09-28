
## **TO SKETCH A VAGUE ALGORITHM PLAN;**
 - Need to define two files as a selection of PDGs (one per method, assuming a file represents a class)
   - For the time being this problem is only going to be concered with Java files.
 - Apply the graph matching algorithm to the two selections of PDGs.
   - At this point, I want to try and match similar PDG with similar PDG. Going to need some heurisitc measure for this;
     - Poss need some heuristic algorithms for larger PDGs, need to give different scores different weights;
       - For example, name should be High. I think its fair to assume that methods with similar names are similar, content should also be high etc.
 - Once we have a selection of PDG_1:PDG_2 matches, can then apply some edit distance on each.
   - This will allow me to represent case by case differences, and present some edit script to the end user.

I imagine this process will be long, but once this program flow is implemented I can look to integrating some UI with the project.


### **Extensions I can think of right now**
 - Extend how the PDG works to be for difference languages / not using a intermediate language.
   - Could probably feasible extend this to work for C/C++ with LLVM 12. (see google doc)
 - Figure out some way to encode an entire program as a PDG. Im not sure if this is feasible in time constraints.
   - Probably involves writing something from the lexer/ parser level. (need to gather CFG of an entire program, this also isnt really a done thing yet.)
 - Extend the graph matching algorithm to be more sophisticated. This will be a never ending improvement.

### **Plans for Graph Matching**

1. **Graph Matching Algorithms**
   - **Subgraph Isomorphism**: Checks if one PDG is a subgraph of another, useful for detecting whether one program contains part of another. Examples include **Ullmann's algorithm** or **VF2**.
   - **Maximum Common Subgraph (MCS)**: Identifies the largest common subgraph between two PDGs. Helps in understanding commonalities, and the differences can be defined as what's not included in the MCS.

2. **Edit Distance-based Approaches**
   - **Graph Edit Distance (GED)**: Calculates the minimum number of operations (insertions, deletions, substitutions of nodes/edges) required to transform one graph into another. GED algorithms include **A\*** search or **bipartite graph matching**.
   - **Tree Edit Distance**: Suitable for hierarchical PDGs. Can be adapted to detect differences in nested control flows or dependency structures.

3. **Delta PDG Construction**
   - Subtract one PDG from the other to compute the **delta PDG** and highlight differences. This would involve identifying any new nodes or edges in one PDG that aren't present in the other.

4. **GumTree-like Heuristic Adaptation**
   - Apply GumTree's tree comparison heuristics to PDGs.
   - **Zhang-Shasha algorithm** could be adapted to handle PDG nodes/edges.
   - **Bottom-up matching** for substructures, followed by **top-down refinement** for smaller differences after matching larger substructures.

5. **Dependency-aware Matching**
   - Focus on control and data dependencies. Group nodes by dependencies to prioritize matching those with similar control or data edges.

6. **Approximate Graph Matching**
   - When exact matching isn't necessary, use **heuristic-based graph matching** to trade precision for speed. Techniques like **spectral matching** or **message passing** are useful for large PDGs where exact matching is costly.
   - Considering graph isomorphic matching is NP-complete, approximate matching can be a good compromise.

7. **Graph Neural Networks (GNNs)**
   - For more complex cases, gna consider using **GNNs** to learn graph node embeddings and compare the graphs. More advanced, but potentially effective for large or complex graphs.



NB: Can probably have some sort of threshold for heurisitics vs exact matching, where if the PDGs are too large, we fall back to approximate matching to avoid time complexity limitations.



