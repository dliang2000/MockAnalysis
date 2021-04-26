package ca.uwaterloo.liang;
/* Soot - a J*va Optimization Framework
 * Copyright (C) 2003 Navindra Umanee <navindra@cs.mcgill.ca>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

import java.util.*;

import ca.uwaterloo.liang.collection.CollectionModelEffect;
import soot.*;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.Expr;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.JArrayRef;
import soot.options.*;
import soot.tagkit.AnnotationTag;
import soot.toolkits.graph.*;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import soot.util.Chain;

/**
 * Find all locals that are mocks
 * 
 *
 * @author David Liang
 **/

/**
 * Flow analysis to determine all locals guaranteed to be defined at a
 * given program point.
 **/
public class MockExprAnalysis extends ForwardFlowAnalysis<Unit, FlowSet<Map<Expr, MockStatus>>> {
    
    private static ArrayList<SootMethod> emptyInvokedMethods = new ArrayList<SootMethod>();
    
    private static FlowSet<Map<Expr, MockStatus>> emptyFlowSet = new ArraySparseSet() ;
    
    private static ArrayList<InvokeExpr> emptyInvokeExprs = new ArrayList<InvokeExpr>();
    
    private static ArrayList<InvokeExpr> emptyInvokeExprsOnMocks = new ArrayList<InvokeExpr>();
    
    private static HashMap<Unit, HashMap<Expr, MockStatus>> emptyMustMocks = new HashMap<Unit, HashMap<Expr, MockStatus>>();
    
    //Contains all the invoked methods by the method under analysis
    // private ArrayList<SootMethod> myInvokedMethods;
    
    //Contains all method invocations
    private ArrayList<InvokeExpr> myTotalInvokeExprs;
    
  //Contains all method invocations on mocks
    private ArrayList<InvokeExpr> myInvokeExprsOnMocks;
    
  //Contains all the invoked methods by the method under analysis
   // private HashSet<SootField> myAnnotatedMocks;
    
    // For each unit x local, will store a boolean for if it is a must mock,
    // if is a must mock within Collection, or if it is a must mock within Array.
    private HashMap<Unit, HashMap<Expr, MockStatus>> mustMocks;

    @SuppressWarnings("unchecked")
    public MockExprAnalysis(ExceptionalUnitGraph graph) {
        super(graph);
        
        // myInvokedMethods = (ArrayList<SootMethod>) emptyInvokedMethods.clone();
        
        mustMocks = (HashMap<Unit, HashMap<Expr, MockStatus>>) emptyMustMocks.clone();
        
        myTotalInvokeExprs = (ArrayList<InvokeExpr>) emptyInvokeExprs.clone();
        
        myInvokeExprsOnMocks = (ArrayList<InvokeExpr>) emptyInvokeExprsOnMocks.clone();
        
        doAnalysis();
    }
    
    public void analyze(ExceptionalUnitGraph graph, SootMethod aCurrentSootMethod) {
        this.graph = graph;
        
        // myInvokedMethods = (ArrayList<SootMethod>) emptyInvokedMethods.clone();
        
        mustMocks = (HashMap<Unit, HashMap<Expr, MockStatus>>) emptyMustMocks.clone();
        
        myTotalInvokeExprs = (ArrayList<InvokeExpr>) emptyInvokeExprs.clone();
        
        myInvokeExprsOnMocks = (ArrayList<InvokeExpr>) emptyInvokeExprsOnMocks.clone();
        
        doAnalysis();
    
    }
    
    @Override
    protected FlowSet<Map<Expr, MockStatus>> newInitialFlow() { 
        return emptyFlowSet.clone();
    }
    
    @Override
    protected FlowSet<Map<Expr, MockStatus>> entryInitialFlow() { 
        return emptyFlowSet.clone();
    }
    
    @Override
    protected void flowThrough(FlowSet<Map<Expr, MockStatus>> in, Unit unit, FlowSet<Map<Expr, MockStatus>> out) {        
        // Performs kills
        kill(in, unit, out);
        // Performs gens
        gen(in, unit, out);
        // Perform gens for casted expr
        genCastExprLocal(in, unit, out);
        // Find array container stores mustMock objects.
        propagateMocknessToContainingArray(in, unit, out);
        // Find collection container stores mustMock objects.
        propagateMocknessToContainingCollection(in, unit, out);
    }

    /**
     * Kill the locals that are no longer mock objects because they are redefined.
     * It is done by performing<br/>
     * out <- in - killSet<br/>
     */
    private void kill(FlowSet<Map<Expr, MockStatus>> in, Unit unit, FlowSet<Map<Expr, MockStatus>> out) {
        FlowSet<Map<Expr, MockStatus>> killSet = emptyFlowSet.clone();
        Stmt aStmt = (Stmt) unit;
        if (doesNotCreateMock(aStmt)) {
            HashMap<Expr, MockStatus> running_result = new HashMap<Expr, MockStatus>();
            List<ValueBox> defBoxes = unit.getDefBoxes();
            for (ValueBox vb: defBoxes) {
                if (vb.getValue() instanceof Expr) {
                    Expr ex = (Expr) vb.getValue();
                    for (Map<Expr, MockStatus> element : in) {
                        if (element.containsKey(ex)) {
                            killSet.add(element);
                            MockStatus status = new MockStatus(false);
                            running_result.put(ex, status);
                        }
                    }
                }
            }
            mustMocks.put(unit, running_result);
        }
        in.difference(killSet, out);
    }

    /**
     * Add locals which are assigned to the return value 
     * from mock creation API to out FlowSet
     */
    private void gen(FlowSet<Map<Expr, MockStatus>> in, Unit unit, FlowSet<Map<Expr, MockStatus>> out) {
        Stmt aStmt = (Stmt) unit;
        
        // First way to create mock: Mock Annotation
        if (aStmt.containsFieldRef()) {
            SootField sf = aStmt.getFieldRef().getField();
            //System.out.println("SootField: " + sf);
            if (MockAnnotationTransformer.getAnnotatedMocks().contains(sf)) {
                //System.out.println("myAnnotatedMocks contain the mock wanted");
                HashMap<Expr, MockStatus> running_result = new HashMap<Expr, MockStatus>();
                List<ValueBox> defBoxes = unit.getDefBoxes();
                for (ValueBox vb: defBoxes) {
                    if (vb.getValue() instanceof Expr) {
                        Expr ex = (Expr) vb.getValue();
                        MockStatus status = new MockStatus(true);
                        running_result.put(ex, status);
                    }
                }
                out.add(running_result);
                mustMocks.put(unit, running_result); 
            }
        }
        
        // Second way to create mock: Mock libraries' API. Example: mock(A.class)
        if (aStmt.containsInvokeExpr()) {
            InvokeExpr invkExpr = aStmt.getInvokeExpr();
            SootMethod sootMethod = invkExpr.getMethod();
            
            if (isMockAPI(sootMethod)) {
                HashMap<Expr, MockStatus> running_result = new HashMap<Expr, MockStatus>();
                List<ValueBox> defBoxes = unit.getDefBoxes();
                for (ValueBox vb: defBoxes) {
                    if (vb.getValue() instanceof Expr) {
                        Expr ex = (Expr) vb.getValue();
                        MockStatus status = new MockStatus(true);
                        running_result.put(ex, status);
                    }
                }
                out.add(running_result);
                mustMocks.put(unit, running_result); 
            }
        }
    }
    
    /**
     * Add locals that are CastExpr of mustMock locals
     * to the out FlowSet
     */
    private void genCastExprLocal(FlowSet<Map<Expr, MockStatus>> in, Unit unit, FlowSet<Map<Expr, MockStatus>> out) {
        if (unit instanceof AssignStmt) {
            AssignStmt assign = (AssignStmt) unit;
            if (assign.getRightOp() instanceof CastExpr) {
                //System.out.println("Assignment: " + assign);
                CastExpr ce = (CastExpr) assign.getRightOp();
                if (ce.getOp() instanceof Expr) {
                    Expr right_op_expr = (Expr) ce.getOp();
                    for (Map<Expr, MockStatus> element : in) {
                        if (element.containsKey(right_op_expr) && element.get(right_op_expr).getMustMock() 
                                && assign.getLeftOp() instanceof Expr) {
                            HashMap<Expr, MockStatus> running_result = new HashMap<Expr, MockStatus>();
                            MockStatus status = new MockStatus(true);
                            Expr left_op_expr = (Expr) assign.getLeftOp();
                            //System.out.println("Casted Local: " + left_op_local);
                            running_result.put(left_op_expr, status);
                            out.add(running_result);
                            mustMocks.put(unit, running_result); 
                            //System.out.println("MustMock Local: " + right_op_local);
                            //System.out.println("MustMock Casted Local: " + left_op_local);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * When unit writes to an Array, propagates mockness 
     * from the local being written to the array to the array itself.
     */
    private void propagateMocknessToContainingArray(FlowSet<Map<Expr, MockStatus>> in, 
                                Unit unit, FlowSet<Map<Expr, MockStatus>> out) {
        Stmt aStmt = (Stmt) unit;
        List<Expr> exprs = new ArrayList<Expr>();
        if (aStmt.containsArrayRef() && aStmt instanceof AssignStmt) {
            //System.out.println("ArrayRef Statement: " + aStmt);
            ValueBox arrayRef = aStmt.getArrayRefBox();
            /*//ValueBox fieldRef = aStmt.getFieldRefBox();
            
            if (arrayRef.getValue() instanceof Local) {
                Local local = (Local) arrayRef.getValue();
                System.out.println("JArrayRef Array Ref value: " + arrayRef.getValue());
                locals.add(local);
            }*/
            AssignStmt assign = (AssignStmt) aStmt;
            if (assign.getRightOp() instanceof Expr) {
                Expr expr = (Expr) assign.getRightOp();
                //System.out.println("Assignment right op: " + local);
                exprs.add(expr);
            }
        }
        
        for (Expr e: exprs) {
            for (Map<Expr, MockStatus> element : in) {
                if (element.containsKey(e) && element.get(e).getMustMock()) {
                    List<ValueBox> db = aStmt.getDefBoxes();
                    for (ValueBox box : db) {
                        List<ValueBox> innerBoxes = box.getValue().getUseBoxes();
                        for (ValueBox innerBox : innerBoxes) {
                            HashMap<Expr, MockStatus> running_result = new HashMap<Expr, MockStatus>();
                            if (innerBox.getValue() instanceof Expr) {
                                Expr arrayBaseExpr = (Expr) innerBox.getValue();
                                //System.out.println("Def Inner Use Box value: " + innerBox.getValue());
                                MockStatus status = new MockStatus(false, true, false);
                                running_result.put(arrayBaseExpr, status);
                                out.add(running_result);
                                mustMocks.put(unit, running_result); 
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * When unit writes to a Collection, propagates mockness 
     * from the local being written to the Collection to the Collection itself.
     */
    private void propagateMocknessToContainingCollection(FlowSet<Map<Expr, MockStatus>> in, 
                                Unit unit, FlowSet<Map<Expr, MockStatus>> out) {
        Stmt aStmt = (Stmt) unit;
        List<Expr> exprs = new ArrayList<Expr>();
        
        Hierarchy hierarchy = Scene.v().getActiveHierarchy();
        RefType target = RefType.v("java.util.Collection");
        boolean isCollection = false;
        if (aStmt.containsInvokeExpr()) {
            List<ValueBox> ub = aStmt.getUseBoxes();
            SootMethod sm = aStmt.getInvokeExpr().getMethod();
            
            for (ValueBox box : ub) {
                List<ValueBox> innerBoxes = box.getValue().getUseBoxes();
                for (ValueBox innerBox : innerBoxes) {
                    // The useBox that is a container (to be refined)
                    if (innerBox.getValue().getType() instanceof RefType && innerBox.getValue() instanceof Expr) {
                        RefType ref = (RefType) innerBox.getValue().getType();
                        SootClass sc = ref.getSootClass();
                        List<SootClass> classes;
                        
                        if (sc.isInterface()) {
                            if (isCollectionASuperInterface(hierarchy, sc) && isReadEffect(sm)) {
                                isCollection = true;
                                exprs.add((Expr) innerBox.getValue());
                                G.v().out.println("Statement: " + aStmt.toString());
                                G.v().out.println("InnerBox value: " + (Expr) innerBox.getValue());
                                G.v().out.println("SootClass Type: " + sc.getType());
                            }
                        } else {
                            Chain<SootClass> interfaces = sc.getInterfaces();
                            for (SootClass curr_interface : interfaces) {
                                if(isCollectionASuperInterface(hierarchy, curr_interface) && isReadEffect(sm)) {
                                    isCollection = true;
                                    exprs.add((Expr) innerBox.getValue());
                                    G.v().out.println("Statement: " + aStmt.toString());
                                    G.v().out.println("InnerBox value: " + (Expr) innerBox.getValue());
                                    G.v().out.println("SootClass Type: " + sc.getType());
                                }
                            }
                        }                        
                    }
                }
            }
            // The unit has a container useBox, now we check if the non-container useBox 
            // is a mustMock local
            List<ValueBox> invoke_ub = aStmt.getInvokeExpr().getUseBoxes();
            if (isCollection) {
                for (ValueBox box : invoke_ub) {
                    if (box.getValue() instanceof Expr) {
                        Expr col_expr = (Expr) box.getValue();
                        //System.out.println("col_local: " + col_local);
                        for (Map<Expr, MockStatus> element : in) {
                            if (element.containsKey(col_expr) && element.get(col_expr).getMustMock()) {
                                //System.out.println("col_local found in hashmap: " + col_local);
                                HashMap<Expr, MockStatus> running_result = new HashMap<Expr, MockStatus>();
                                for (Expr e: exprs) {
                                    if (!e.equals(col_expr)) {
                                        MockStatus status = new MockStatus(false, false, true);
                                        running_result.put(e, status);
                                        out.add(running_result);
                                        mustMocks.put(unit, running_result);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }   
    }
    
    public void updateInvocations(ExceptionalUnitGraph graph) {
        UnitPatchingChain units = graph.getBody().getUnits();
        
        for (Unit unit : units) {
            Stmt aStmt = (Stmt) unit;
            if (aStmt.containsInvokeExpr()) {
                InvokeExpr invkExpr = aStmt.getInvokeExpr();
                if (invkExpr instanceof InstanceInvokeExpr) {
                    // Add InvokeExpr to myTotalInvokeExprs
                    myTotalInvokeExprs.add(invkExpr);
                    
                    InstanceInvokeExpr iie = (InstanceInvokeExpr) invkExpr;
                    Value val = iie.getBase();
                    
                    // If the base of the invokeExpr is an instanceof Local, 
                    // and can be found in the in FlowSet, then InvokeExpr is 
                    // on a mock
                    if (val instanceof Expr) {
                        Expr e = (Expr) val;
                        for (Map<Expr, MockStatus> element : getFlowAfter(unit)) {
                            if (element.containsKey(e) && element.get(e).getMustMock()) { //must be an invocation on MustMock
                                myInvokeExprsOnMocks.add(invkExpr);
                            }
                        }
                    }
                }
            }
        }
        
    }
    
    public ArrayList<InvokeExpr> getTotalInvokeExprs() {
        return myTotalInvokeExprs;
    }
    
    public ArrayList<InvokeExpr> getInvokeExprsOnMocks() {
        return myInvokeExprsOnMocks;
    }
    
    public HashMap<Unit, HashMap<Expr, MockStatus>> getMustMocks() {
        return mustMocks;
    }
    
    @Override
    protected void merge(FlowSet<Map<Expr, MockStatus>> in1, FlowSet<Map<Expr, MockStatus>> in2, FlowSet<Map<Expr, MockStatus>> out) {
        in1.intersection(in2, out);
    }
    
    @Override
    protected void copy(FlowSet<Map<Expr, MockStatus>> srcSet, FlowSet<Map<Expr, MockStatus>> destSet) {
        srcSet.copy(destSet);
    }
    
//    @Override
//    public FlowSet<Map<Local, MockStatus>> getFlowAfter(Unit u) {
//        
//        return null;
//        
//    }
    
    private static boolean isReadEffect(SootMethod sm) {
        List<String> reads = CollectionModelEffect.READ.getMethods();
        
        for (String read : reads) {
            if (sm.getSubSignature().contains(read))
                return true;
        }
        return false;
    }
    
    private static boolean isCollectionASuperInterface(Hierarchy hierarchy, SootClass sc) {
        RefType collection = RefType.v("java.util.Collection");
        
        List<SootClass> classes = hierarchy.getSuperinterfacesOf(sc);
        for (SootClass curr_class : classes) {
            if (curr_class.getType().equals(collection)) {
                return true;
            }
        }
        return false;
    }
    
    private static boolean doesNotCreateMock(Stmt stmt) {
        return (!stmt.containsInvokeExpr() || !isMockAPI(stmt.getInvokeExpr().getMethod()));
    }
    
    public static List<String> MOCKITO_VERIFIES = Collections.unmodifiableList(
            Arrays.asList(new String[] {"java.lang.Object verify(java.lang.Object)", 
                                        "java.lang.Object verify(java.lang.Object,org.mockito.verification.VerificationMode)"}));
    
    private static boolean isMockAPI(SootMethod method) {
        return (method.getSubSignature().equals(MockLibrary.EASYMOCK.subSignature()) 
                                || method.getSubSignature().equals(MockLibrary.MOCKITO.subSignature())
                                || method.getSubSignature().equals(MockLibrary.POWERMOCK.subSignature())
                                ||  MOCKITO_VERIFIES.contains(method.getSubSignature()) );
    }
}