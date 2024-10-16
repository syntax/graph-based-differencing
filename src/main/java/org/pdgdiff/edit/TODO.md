PRESSING TODOS; -> must complete for completion of this project

 - Need to add some way of moving edit scripts from being in respects to nodes to become in respects to the actually source code.
   - Per each method, i need to devise some way of describing each edit script in respects to the source code.
     - Because i am not doing entire program, probably need to tell Adriana that this might be a limitation of this project, and that I probably wont beat gumtree.
 - When anaylsis syntax differences I need to actually have some way of representing which part of the line is wrong. 

 - Probably need to add some sort of thresholding to the graph matching, but probably not the node mappings.
   - If two graphs have been matched to each other as being similar, and then I have two sets of nodes, with quantity x and y, I want to map every node from the smaller set to the larger set as best I can.
   - There are three options. Src and dest and the same size, src is bigger or dest is bigger. 
     - If source is bigger, i want to map all from dest to source and then depict the deletion of some source nodes/ elements.
     - If dest is bigger, i want to map all from source to dest and then depict the addition of some dest nodes/ elements.
   - Graph matching currently has similar logic to node mapping, but I dont think necessarily all methods should be mapped. I think this is probably too much!
 
 - Need to build out a testing suite. This should use probably the same test cases as gumtree, but I need to run gumtree on all of it to see if I am getting similar sized edit scripts to it.

 - Invesigate if I need to difference both regions and cfgnodes. I am pretty sure just cfg nodes are enough to represent the differences in the source code, even if they are not enough to represent the excat differences in PDG's.


LESS PRESSING TODOS;

 - Want to add other matching algorithsm.
 - Upgrade VF2 to VF3
 - Add some sort of composite matching techniques.
 - Build out a UI, that allows user input and does the rest.