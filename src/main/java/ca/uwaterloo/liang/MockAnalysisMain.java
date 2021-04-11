package ca.uwaterloo.liang;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uwaterloo.liang.util.Utility;
import soot.G;
import soot.PackManager;
import soot.PointsToAnalysis;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Transformer;
import soot.Unit;
import soot.jimple.JimpleBody;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;
import soot.tagkit.AnnotationTag;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class MockAnalysisMain extends SceneTransformer {
    private static String benchmark;
    private static String output_path;
    
    private static final Logger logger = LoggerFactory.getLogger(PackManager.class);
    public static void main(String[] args) throws IOException {
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.myTransform", new MockAnalysisMain()) {
        });
        Options.v().set_prepend_classpath(true);
        Options.v().set_verbose(true);
        Options.v().set_whole_program(true);
        Options.v().set_output_format(1); // Output format in .jimple file
        Options.v().set_allow_phantom_refs(true);
        List<String> pd = new ArrayList<>();
        pd.add("-main-class");
        pd.add(args[0]);
        pd.add("-process-dir");
        pd.add(args[1]);
        pd.add("-process-dir");
        pd.add(args[2]);
        Options.v().set_soot_classpath(args[3]);
        MockAnalysisMain.benchmark = args[4];
        MockAnalysisMain.output_path = args[5];
        System.out.println("args[0]: " + args[0]);
        System.out.println("args[1]: " + args[1]);
        System.out.println("args[2]: " + args[2]);
        System.out.println("args[3]: " + args[3]);
        System.out.println("args[4]: " + args[4]);
        System.out.println("args[5]: " + args[5]);
        soot.Main.main(pd.toArray(new String[0]));
    }
        
    private CallGraph myCallGraph;
    
    private PointsToAnalysis myPointsToAnalysis;
    
    /**
     * All the classes of the application to analyze
     */ 
    private HashMap<String, SootClass> myAppClasses;
    private Collection<SootClass> colAppClasses;
    
    /**
     * All the methods of the applications classes to analyze
     */
    private ArrayList<SootMethod> myAppMethods;
    
    /**
     * Each class mapped to its corresponding methods
     */
    private HashMap<String, ArrayList<SootMethod>> myClassMethods;
    
    
    /**
     * Each method mapped to its summary
     */
    private HashMap<SootMethod, ProcSummary> myProcSummaries;
        
    /**
     * Each method mapped to its callees
     */
    private HashMap<String, ArrayList<SootMethod> > myCallees;
        
    private MockAnalysis myMAnalysis;
        
    public MockAnalysisMain() {
        super();
        
        myProcSummaries = new HashMap<SootMethod, ProcSummary>();
            
        myClassMethods = new HashMap<String, ArrayList<SootMethod> >();
            
        myCallees = new HashMap<String, ArrayList<SootMethod> >();
                
        myAppMethods = new ArrayList<SootMethod>();
        
        myAppClasses = new HashMap<String, SootClass>();
    }


    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        // TODO Auto-generated method stub
        Iterator<SootClass> itAppClasses = Scene.v().getApplicationClasses().iterator();
        while(itAppClasses.hasNext()) {
            SootClass nextClass = itAppClasses.next();
            myAppClasses.put(nextClass.getName(), nextClass);
        }
        colAppClasses = myAppClasses.values();
    
        myCallGraph = Scene.v().getCallGraph();
     
        // get points to analysis object
        myPointsToAnalysis = Scene.v().getPointsToAnalysis();
        
        //Compute summaries of all methods present in the call graph
        
        for (SootClass sc : colAppClasses) {
            myAppMethods.addAll(sc.getMethods());           
            myClassMethods.put(sc.getName(), new ArrayList<SootMethod>(sc.getMethods()) );
        } 
        
        ProcSummary mockSummary = null;  
           
        ExceptionalUnitGraph aCfg = null;
            
        boolean mySAInst = false;
            
        G.v().out.println("Number of methods to be analyzed: " + myAppMethods.size() );
        
        for (SootMethod method : myAppMethods) {   
            if (method.hasActiveBody() && isTestCase(method)) {
                JimpleBody body = (JimpleBody) method.getActiveBody();
                
                System.out.println("Test Method Body: ");
                for(Unit u : body.getUnits()){
                    System.out.println(u.toString());
                }
                mockSummary = new ProcSummary(method);
                
                aCfg = new ExceptionalUnitGraph(method.getActiveBody());
                
                if (mySAInst){
                     myMAnalysis.analyze(aCfg, method);
                } else {
                     myMAnalysis = new MockAnalysis(aCfg);
                     mySAInst = true;
                }
                
                mockSummary.setPossiblyMocks( myMAnalysis.getPossiblyMocks() );           
                
                mockSummary.setInvokedMethods( myMAnalysis.getInvokedMethods() );
                    
                myCallees.put(method.getSignature(), myMAnalysis.getInvokedMethods());
                
                myProcSummaries.put(method, mockSummary);
            }
        }
            
        printOutput();
    }
        
    private void printOutput() {
        StringBuffer msg = new StringBuffer();
                    
        for(SootClass nc : colAppClasses) {     
            msg.append( Utility.printPossiblyMocks(nc, myProcSummaries) );
        }   
            
        G.v().out.println(msg);
                    
        /*try 
            {
                //FileWriter f = new FileWriter ("callgraph.txt");
                //f.write(myCallGraph.toString());
                //f.close();
                Utility.generateDOTOutput(colAppClasses, myProcSummaries, "analysis.dot");
            }
            catch(Exception e) {
                e.printStackTrace();
                G.v().out.println(e.toString());
            }*/
    }
    
    private static boolean isTestCase(SootMethod sm) {
        // JUnit 3
        if (sm.getName().toLowerCase().startsWith("test") && sm.getParameterCount() == 0 && sm.getReturnType().toString() == "void") {
            //System.out.println("Test case found: " + sm.getSubSignature());
            return true;
        }

        // JUnit 4+

        List<soot.tagkit.Tag> smTags = sm.getTags();
        soot.tagkit.VisibilityAnnotationTag tag = (soot.tagkit.VisibilityAnnotationTag) sm
                .getTag("VisibilityAnnotationTag");
        if (tag != null) {
            for (AnnotationTag annotation : tag.getAnnotations()) {
                if (annotation.getType().equals("Lorg/junit/Test;")) {
                    //System.out.println("Test case found: " + sm.getSignature());
                    return true;
                }
            }
        }
        return false;
    }
}
