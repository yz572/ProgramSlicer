package Assignment1;
		
import br.usp.each.saeg.asm.defuse.Variable;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import analysis.*;		
import java.util.*;
import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
/*
 * Problem three: calculating the number of lines in all classes
 */

public class LOCcounter {
   
    public static void main(String[] args){
       
        File f = new File("/Users/yifanzhang/Test/assignment-1-yz572/src/net/sf/freecol/tools" );
        Map map = new HashMap<String,Integer>();  
      //Used to store the counted lines
        Plus(f,map);
        
        getResult(map);
    }

   
    public static void Plus(File f ,Map map){

        File[] files = f.listFiles();


        

        for (File a : files) {
            
         map = lineNumber(a.getAbsolutePath(),map);  
            }//Traverse these files and count the number of lines


    }

   
    public static Map<String,Integer> lineNumber(String f,Map map){
        
    	//Define character stream read file
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(f);
        } catch (IOException e){
            e.printStackTrace();
          
        }

        BufferedReader bufferedReader= new BufferedReader(fileReader);        
        int index = 0;
        

        try {  String line = "";  
           
        while ((line = bufferedReader.readLine()) != null)    
        {
            	
            	 line = line.trim();  
            	 
            
                index++;
            }
            map.put(f,index);    
            

        }catch (IOException e){
            e.printStackTrace();
      
        }finally {
            if(fileReader != null){
                try {
                    fileReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return map;
        }
    }
    
    /*
    Store the lines of the file in a map, and then output the sum of the lines
    */
    public static void getResult(Map map){

        
        Iterator<Map.Entry<String,Integer>> entries =
                map.entrySet().iterator();

        while (entries.hasNext()){
            Map.Entry<String,Integer> entry = entries.next();
            System.out.println(entry.getKey()+"->LOC:"+entry.getValue());
//            sum += entry.getValue();
        }

//        System.out.println("total LOC："+sum);

    }
}