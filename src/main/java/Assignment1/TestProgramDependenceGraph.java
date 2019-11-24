package Assignment1;

import br.usp.each.saeg.asm.defuse.Variable;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

import analysis.*;

import java.util.*;


public class TestProgramDependenceGraph extends Analysis {

    public TestProgramDependenceGraph(ClassNode cn, MethodNode mn) {
		super(cn, mn);
		// TODO Auto-generated constructor stub
	}

	Graph dataDependencies;
    Graph controlDependencies;
    Graph combined;
    
 // Data dependence graph - for computation of overlap
  private Graph dataDependenceGraph;
  // Program dependence graph - (cannot use controlFlowGraph in Analysis since its final)
  private Graph programDependenceGraph;

    public ProgramDependenceGraph(ClassNode cn, MethodNode mn) {
        super(cn, mn);
        dataDependencies = new Graph();
    }

    /**
     * Return a graph representing the Program Dependence Graph of the control
     * flow graph, which is stored in the controlFlowGraph class attribute
     * (this is inherited from the Analysis class).
     *
     * You may wish to use the class that computes the Control Dependence Tree to
     * obtain the control dependences, and may wish to use the code in the
     * dependenceAnalysis.analysis.DataFlowAnalysis class to obtain the data dependences.
     * 
     * @return
     */
    public Graph computeResult() {
       //Complete this
     ControlDependenceTree cdt = new ControlDependenceTree(cn, mn);
     programDependenceGraph = cdt.computeResult();
    
     // Prepare a map of node and variables used by the node
     // this is to avoid multiple calls later in the processing
     Map<String, Collection<Variable>> nodeUsedByVarsMap = new HashMap<String, Collection<Variable>>();
     for (Node node : programDependenceGraph.getNodes()) {
     // skip slicing for non-instruction node
     if (isSkipNode(node)) {
     continue;
     }
     try {
Collection<Variable> usedVars = DataFlowAnalysis.usedBy(cn.name, mn, node.getInstruction());
nodeUsedByVarsMap.put(node.toString(), usedVars);
} catch (AnalyzerException e) {
System.out.println("Analyser exception occured while computing program dependence");
e.printStackTrace();
}
     }
    
     // in addition to the program dependence graph, data dependence graph is also computed
     dataDependenceGraph = new Graph();
    
     // for each node get defined variables and check with all the node's usedBy variables
     // if any variable defined is user by a node add data dependence and add edge in 
     // control flow graph i.e. program dependence graph
     for (Node node : programDependenceGraph.getNodes()) {
     // skip slicing for non-instruction node
     if (isSkipNode(node)) {
     continue;
     }
     try {
     Collection<Variable> definedVars = DataFlowAnalysis.definedBy(cn.name, mn, node.getInstruction());
     for (Node n : programDependenceGraph.getNodes()) {
     // skip slicing for non-instruction node
         if (isSkipNode(n)) {
         continue;
         }
        
         // get user by variables of the node from the map
         Collection<Variable> usedByVars = nodeUsedByVarsMap.get(n.toString());
         for (Variable variable : definedVars){
                    if(usedByVars.contains(variable)){
                     // update data dependence graph : add nodes and edge
                     dataDependenceGraph.addNode(node);
                     dataDependenceGraph.addNode(n);
                     if (node != n) {
                     dataDependenceGraph.addEdge(node, n);
                     // Add an edge to between definedBy node and usedBy node
                     programDependenceGraph.addEdge(node, n);
                     }
                    }
                }
     }
     } catch (AnalyzerException e) {
     System.out.println("Analyser exception occured while computing program dependence");
e.printStackTrace();
}
     }
        return programDependenceGraph;
    } 
    
    /**
     * Compute the set of nodes that belong to a backward slice, computed from a given
     * node in the program dependence graph.
     *
     * @param node
     * @return
     */
    public Set<Node> backwardSlice(Node node){
         //Complete this
Set<Node> sliceNodes = new HashSet<Node>();
        
        // check if the program dependence graph is computed, else compute it
        if (programDependenceGraph == null) {
         computeResult();
        }
        
        // add the original node to the slice
        //sliceNodes.add(node);
        
        // traverse through node and find if any of the nodes contain the original node as part
        // of transitive successors add it to the slice
        for (Node n : programDependenceGraph.getNodes()) {
         if (n != node && programDependenceGraph.getTransitiveSuccessors(n).contains(node)) {
         sliceNodes.add(n);
         }
        }
        return sliceNodes;
    }
    
    /**
     * Compute the Tightness slice-based metric. The proportion of nodes in a control flow graph that occur
     * in every possible slice of that control flow graph.
     * @return
     */
    public double computeTightness(){
         //Complete this
ArrayList<Set<Node>> sliceNodeSet = new ArrayList<Set<Node>>();
    
     // check if the program dependence graph is computed, else compute it
     if (programDependenceGraph == null) {
         computeResult();
        }
    
     // traverse through the program dependence graph
     for (Node node : programDependenceGraph.getNodes()) {
     // skip slicing for non-instruction node
     if (isSkipNode(node)) {
     continue;
     }
    
     // generate backward slice and add it to the Slices set
     Set<Node> slice = backwardSlice(node);
     sliceNodeSet.add(slice);
        }
    
     // find common node set which intersection all the slices computed
     double tightness = 0.0;    
     if (sliceNodeSet.size() > 0) {
     Set<Node> commonNodeSet = sliceNodeSet.get(0);
     for (int index = 1; index < sliceNodeSet.size(); index++) {
     commonNodeSet.retainAll(sliceNodeSet.get(index));
     }
     tightness = commonNodeSet.size() / (double) (programDependenceGraph.getNodes().size());
     }
    
        return tightness;
    }
    
    /**
     * Compute the Overlap slice-based metric: How many statements per output-slice are unique to that slice?
     * Output slices are computed by computing backward slices from the set of nodes with incoming data dependencies,
     * but with no outgoing data dependencies.
     * @return
     */
    public double computeOverlap(){
    	//Complete thisCollection<Node> preds = new HashSet<Node>();
   	 Collection<Node> slice = getOutputNodes();
   	 List<Node> list = new ArrayList<Node>();
   	 for(Node n:slice) {
   		 Set<Node> ss = backwardSlice(n);
   		 list.addAll(ss);
   	 }
   	Map<String, Integer> map = new HashMap<String,Integer>();
   	 for(Node a : list) {
   		 String b = a.toString();
   		 Integer count = map.get(b);
   		 map.put(b,(count == null)?1 : count +1);
   	 }
   	 int count = 0;
   	 for(Map.Entry<String, Integer> entry: map.entrySet()) {
   		 if(entry.getValue()==1) {
   			 count++;
   		 }//Elements that only appear once through loop statistics

   	 }
   	 return (double)count/(double)slice.size();
   }//The

    private void computeCombinedControlDataDependence() {
        combined = new Graph();
        for(Node n : controlFlowGraph.getNodes()){
            combined.addNode(n);
        }
        for(Node n : controlFlowGraph.getNodes()){
            for(Node o : controlDependencies.getSuccessors(n)){
                combined.addEdge(n,o);
            }
            for(Node o : dataDependencies.getSuccessors(n)){
                combined.addEdge(n,o);
            }
        }
    }

    private void computeControlDependencies() {
        ControlDependenceTree cdt = new ControlDependenceTree(cn,mn);
        controlDependencies = cdt.computeResult();
    }

    /**
     * Compute data dependences between cfg nodes.
     * @return
     */
    protected void computeDataDependenceEdges(){
        //Instantiate our DataFlowAnalysis, which we will use to compute defs and uses for instructions.
        DataFlowAnalysis dfa = new DataFlowAnalysis();
        try {
            //Iterate through all of the cfg instructions
            for(Node n : controlFlowGraph.getNodes()) {
                dataDependencies.addNode(n);
                //For each instruction...
                Collection<Node> defs = new HashSet<Node>();
                //... define a set (defs) that define variables that are used at this node.
                if (n.getInstruction() == null)
                    continue;
                //For each variable that is used at this node...
                for (Variable use : dfa.usedBy(cn.name, mn, n.getInstruction())) {
                    // Search backwards through the graph for all nearest defs of that variable
                    defs.addAll(bfs(dfa, controlFlowGraph, n, use, new HashSet<Object>()));
                }


                //For each definition of a variable, create a data dependence edge.
                for (Node def : defs) {
                    if (def.equals(n))
                        continue;
                    dataDependencies.addNode(def);
                    dataDependencies.addEdge(def, n);
                }
            }

        }catch (AnalyzerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    /**
     * A breadth-first search backwards through a control flow graph.
     * @param dfa - Data flow analysis for the cfg in question
     * @param cfg - CFG we are searching through.
     * @param n - Node in the CFG we are commencing our search from.
     * @param use - Variable we are considering.
     * @param done - Set of nodes explored as part of search so far
     * @return
     * @throws AnalyzerException
     */
    private Collection<Node> bfs(DataFlowAnalysis dfa, Graph cfg, Node n, Variable use,Collection<Object> done) throws AnalyzerException {
        // Create an empty set of nodes that will eventually contain the nodes we are interested in.
        Collection<Node> defs = new HashSet<Node>();
        //For all immediate predecessors in the graph of n:
        for(Node pred : cfg.getPredecessors(n)){
            //Skip loops.
            if(done.contains(pred))
                continue;
            //Keep track of which nodes we have have already visited to avoid visiting them again.
            done.add(pred);
            //skip null instructions (entry and exit)
            if(pred.getInstruction()== null)
                continue;
            // If the set of variables that is defined at this instruction contains the variable we are interested in (use)
            if(dfa.definedBy(cn.name, mn, pred.getInstruction()).contains(use)){
                //Add this node to the set of defs. We don't need to look at any of this node's predecessors.
                defs.add(pred);
                continue;
            }
            //Otherwise
            else{
                //Check this nodes predecessors and recurse. Add the defs that are eventually found further down the line.
                defs.addAll(bfs(dfa,cfg,pred,use,done));
            }

        }
        return defs;
    }

    private Set<Node> transitivePredecessors(Node m, Set<Node> done){
        Set<Node> preds = new HashSet<Node>();
        for(Node n : combined.getPredecessors(m)){
            if(!done.contains(n)) {
                preds.add(n);
                done.add(n);
                preds.addAll(transitivePredecessors(n, done));
            }

        }
        return preds;
    }

    Collection<Node> getOutputNodes(){
        Collection<Node> criteria = new HashSet<Node>();
        for(Node n : dataDependencies.getNodes()){
            if(dataDependencies.getSuccessors(n).isEmpty())
                criteria.add(n);
        }
        return criteria;
    }
    
    private boolean isSkipNode(Node node) {
     String name = node.toString();
     return (name.equalsIgnoreCase("\"Start\"") 
     || name.equalsIgnoreCase("\"Entry\"") 
     || name.equalsIgnoreCase("\"Exit\""));
    }

    
}