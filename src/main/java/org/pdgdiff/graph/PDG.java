    package org.pdgdiff.graph;

    import soot.toolkits.graph.HashMutableEdgeLabelledDirectedGraph;
    import soot.toolkits.graph.UnitGraph;
    import soot.toolkits.graph.pdg.PDGNode;

    import java.util.*;

    // TODO: this class will be my own version of the HashMutuablePDG that Soot presents, hopefully made slightly more accurate to
    // TODO: the original literature.
    public class PDG extends HashMutableEdgeLabelledDirectedGraph<PDGNode, GraphGenerator.DependencyTypes> {
        private UnitGraph cfg = null;
        protected PDGNode startNode = null;
        private List<PDGNode> orderedNodes;

        public PDG() {
            super();
        }

        public void setCFG(UnitGraph cfg) {
            this.cfg = cfg;
        }

        public UnitGraph getCFG() {
            return cfg;
        }

        public PDGNode getStartNode() {
            return startNode;
        }

        public void setOrderedNodes(List<PDGNode> orderedNodes) {
            this.orderedNodes = orderedNodes;
        }


        public List<PDGNode> getNodes() {
            // alternatively could use nodeToPreds method here, not sure
            // order seems to be non-det which is a bit worrying.
            return orderedNodes;
        }

        public List<Set<PDGNode>> getConnectedComponents() {
            Set<PDGNode> visited = new HashSet<>();
            List<Set<PDGNode>> components = new ArrayList<>();

            for (PDGNode node : this.getNodes()) {
                if (!visited.contains(node)) {
                    Set<PDGNode> component = new HashSet<>();
                    exploreComponent(node, visited, component);
                    components.add(component);
                }
            }
            return components;
        }

        private void exploreComponent(PDGNode node, Set<PDGNode> visited, Set<PDGNode> component) {
            Stack<PDGNode> stack = new Stack<>();
            stack.push(node);
            visited.add(node);

            while (!stack.isEmpty()) {
                PDGNode current = stack.pop();
                component.add(current);

                for (PDGNode neighbor : this.getSuccsOf(current)) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        stack.push(neighbor);
                    }
                }

                for (PDGNode neighbor : this.getPredsOf(current)) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        stack.push(neighbor);
                    }
                }
            }
        }

        public List<PDGNode> getIsolatedNodes() {
            List<PDGNode> isolatedNodes = new ArrayList<>();

            for (PDGNode node : this.getNodes()) {
                List<PDGNode> successors = this.getSuccsOf(node);
                List<PDGNode> predecessors = this.getPredsOf(node);

                if ((successors == null || successors.isEmpty()) && (predecessors == null || predecessors.isEmpty())) {
                    isolatedNodes.add(node);
                }
            }

            return isolatedNodes;
        }
    }