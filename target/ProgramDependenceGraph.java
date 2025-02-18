package Assignment1;

import br.usp.each.saeg.asm.defuse.Variable;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

import analysis.*;

import java.util.*;


public class ProgramDependenceGraph extends Analysis {

    Graph dataDependencies;
    Graph controlDependencies;
    Graph combined;

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
    	computeControlDependencies();
        computeDataDependenceEdges();
        computeCombinedControlDataDependence();
           return combined;

    } 
    //Call the written method and return directly to combined
    
    /**
     * Compute the set of nodes that belong to a backward slice, computed from a given
     * node in the program dependence graph.
     *
     * @param node
     * @return
     */
    public Set<Node> backwardSlice(Node node/*,Set<Node> done*/){
       Set<Node> preds = new HashSet<Node>();
       preds = transitivePredecessors(node, preds);
       preds.add(node);
//       for(Node n : combined.getPredecessors(node)){
//           
//    	   do{ ;
//    	   preds.add(n);
//           done.add(n);
//           preds.addAll(transitivePredecessors(n, done));
//    		   
//    	   }while(!done.contains(n));
//    	   
 //   	   if(!done.contains(n)) {
//               preds.add(n);
//               done.add(n);
//               preds.addAll(transitivePredecessors(n, done));
//           }

       
       return preds;
       
   }

    
    /**
     * Compute the Tightness slice-based metric. The proportion of nodes in a control flow graph that occur
     * in every possible slice of that control flow graph.
     * @return
     */
    public double computeTightness(){
       Collection<Node> outputNodes = getOutputNodes();
       Collection<Node> done = new HashSet<Node>();
       for(Node n : outputNodes){
    	   Set <Node> bcksli = backwardSlice(n);
    	   if(done.isEmpty()) {
    		   done.addAll(bcksli);
    		   //Add files to collection by detecting non empty
    	   }else {
    		  done.retainAll(bcksli);
    		   //Keep the same elements and delete the rest
    	   }
    	   
       }
       double interscetion = done.size();
       double all = controlFlowGraph.getNodes().size();
       return interscetion/all;
       //The output is the intersection / total slice of all nodes
       
       //return (double)getCyclematicComplexity(ProgramDependenceGraphTest.graph) / (double)getNodeCount(ProgramDependenceGraphTest.graph) / 2.0;
        //Complete this
    }
    
    private static int getNodeCount(Graph cfg) 
    {
    	return cfg.getNodes().size();
    }
    
    public static int getCyclematicComplexity(Graph cfg)
    {
    	int branchCount = 0;
    	for(Node n : cfg.getNodes()) {
    		if (cfg.getSuccessors(n).size()>1)
    		{
    			branchCount ++;
    		}
    	}
    	return branchCount + 1;
    	
    
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
   }//The final output is all unaffected unique points / total slices
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

    
}
