/********************************************************************************/
/*										*/
/*		AssignAnalysis.java						*/
/*										*/
/*	Python Bubbles Base analysis for assignments				*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 * This program and the accompanying materials are made available under the	 *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at								 *
 *	http://www.eclipse.org/legal/epl-v10.html				 *
 *										 *
 ********************************************************************************/
/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */

package edu.brown.cs.bubbles.pybase.symbols;

import edu.brown.cs.bubbles.pybase.PybaseConstants;
import edu.brown.cs.bubbles.pybase.PybaseMain;

import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Return;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.jython.ast.stmtType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


/**
 * This class is used to analyse the assigns in the code and bring actual completions for them.
 */
public class AssignAnalysis implements PybaseConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

/**
 * The user should be able to configure that, but let's leave it hard-coded until the next release...
 *
 * Names of methods that will return instance of the passed class -> index of class parameter.
 */
private final static Map<String, Integer> CALLS_FOR_ASSIGN_WITH_RESULTING_CLASS = new HashMap<String, Integer>();

static {
   // method factory that receives parameter with class -> class parameter index
   CALLS_FOR_ASSIGN_WITH_RESULTING_CLASS.put("adapt".toLowerCase(), 2);
   CALLS_FOR_ASSIGN_WITH_RESULTING_CLASS.put("GetSingleton".toLowerCase(), 1);
   CALLS_FOR_ASSIGN_WITH_RESULTING_CLASS.put("GetImplementation".toLowerCase(), 1);
   CALLS_FOR_ASSIGN_WITH_RESULTING_CLASS.put("GetAdapter".toLowerCase(), 1);
   CALLS_FOR_ASSIGN_WITH_RESULTING_CLASS.put("get_adapter".toLowerCase(), 1);
}




/**
 * If we got here, either there really is no definition from the token
 * or it is not looking for a definition. This means that probably
 * it is something like.
 *
 * It also can happen in many scopes, so, first we have to check the current
 * scope and then pass to higher scopes
 *
 * e.g. foo = Foo()
 *	    foo. | Ctrl+Space
 *
 * so, first thing is discovering in which scope we are (Storing previous scopes so
 * that we can search in other scopes as well).
 */
public AssignCompletionInfo getAssignCompletions(AbstractASTManager manager,
	 AbstractModule module,CompletionState state)
{
   ArrayList<AbstractToken> ret = new ArrayList<AbstractToken>();
   Definition[] defs = new Definition[0];
   if (module instanceof SourceModule) {
      SourceModule s = (SourceModule) module;

      try {
	 defs = s.findDefinition(state, state.getLine() + 1, state.getCol() + 1,
		  state.getNature());
	 for (int i = 0; i < defs.length; i++) {
	    // go through all definitions found and make a merge of it...
	    Definition definition = defs[i];

	    if (state.getLine() == definition.getLine() && state.getCol() == definition.getCol()) {
	       // Check the module
	       if (definition.getModule() != null && definition.getModule().equals(s)) {
		  // initial and final are the same
		  if (state.checkFoudSameDefinition(definition.getLine(),definition.getCol(),
			   definition.getModule())) {
		     // We found the same place we found previously (so, we're recursing
		     // here... Just go on)
		     continue;
		  }
	       }
	    }
	    AssignDefinition assignDefinition = null;
	    if (definition instanceof AssignDefinition) {
	       assignDefinition = (AssignDefinition) definition;
	    }
	    if (definition.getAssignment() instanceof FunctionDef) {
	       addFunctionDefCompletionsFromReturn(manager, state, ret, s, definition);
	    }
	    else {
	       addNonFunctionDefCompletionsFromAssign(manager, state, ret, s, definition,
			assignDefinition);
	    }
	 }
      }
      catch (CompletionRecursionException e) {
	 // thats ok
      }
      catch (Exception e) {
	 PybaseMain.logE("Problem getting assign completions",e);
	 throw new RuntimeException("Error when getting assign completions for:"
		  + module.getName(),e);
      }
      catch (Throwable t) {
	 throw new RuntimeException("A throwable exception has been detected "
		  + t.getClass());
      }
   }
   return new AssignCompletionInfo(defs,ret);
}


private void addFunctionDefCompletionsFromReturn(AbstractASTManager manager,
	 CompletionState state,ArrayList<AbstractToken> ret,SourceModule s,Definition definition)
	 throws CompletionRecursionException
{
   FunctionDef functionDef = (FunctionDef) definition.getAssignment();
   for (Return return1 : ReturnVisitor.findReturns(functionDef)) {
      CompletionState copy = state.getCopy();
      String act = NodeUtils.getFullRepresentationString(return1.value);
      if (act == null) {
	 return; // may happen if the return we're seeing is a return without anything
      }
      copy.setActivationToken(act);
      copy.setLine(return1.value.beginLine - 1);
      copy.setCol(return1.value.beginColumn - 1);
      AbstractModule module = definition.getModule();

      state.checkDefinitionMemory(module, definition);

      AbstractToken[] tks = manager.getCompletionsForModule(module, copy);
      if (tks.length > 0) {
	 ret.addAll(Arrays.asList(tks));
      }
   }
}


/**
 * This method will look into the right side of an assign and its definition and will try to gather the tokens for
 * it, knowing that it is dealing with a non-function def token for the definition found.
 *
 * @param ret the place where the completions should be added
 * @param assignDefinition may be null if it was not actually found as an assign
 */
private void addNonFunctionDefCompletionsFromAssign(AbstractASTManager manager,
	 CompletionState state,ArrayList<AbstractToken> ret,SourceModule sourceModule,
	 Definition definition,AssignDefinition assignDefinition)
	 throws CompletionRecursionException
{
   AbstractModule module;
   if (definition.getAssignment() instanceof ClassDef) {
      state.setLookingFor(LookForType.UNBOUND_VARIABLE);
      ret.addAll(((SourceModule) definition.getModule()).getClassToks(state, manager,
	       definition.getAssignment()));
   }
   else {
      boolean lookForAssign = true;

      // ok, see what we can do about adaptation here...
      // pyprotocols does adapt(xxx, Interface), so, knowing the type of the interface can
      // get us to nice results...
      // the user can usually have other factory methods that do that too. E.g.:
      // GetSingleton(Class) may return an
      // expected class and so on, so, this should be configured somehow
      if (assignDefinition != null) {
	 Assign assign = (Assign) assignDefinition.getAssignment();
	 if (assign.value instanceof Call) {
	    Call call = (Call) assign.value;
	    String lastPart = FullRepIterable.getLastPart(assignDefinition.getValueName());
	    Integer parameterIndex = CALLS_FOR_ASSIGN_WITH_RESULTING_CLASS.get(lastPart
		     .toLowerCase());
	    if (parameterIndex != null && call.args.length >= parameterIndex) {
	       String rep = NodeUtils
			.getFullRepresentationString(call.args[parameterIndex - 1]);

	       HashSet<AbstractToken> hashSet = new HashSet<AbstractToken>();
	       List<String> lookForClass = new ArrayList<String>();
	       lookForClass.add(rep);

	       manager.getCompletionsForClassInLocalScope(sourceModule, state, true,
			false, lookForClass, hashSet);
	       if (hashSet.size() > 0) {
		  lookForAssign = false;
		  ret.addAll(hashSet);
	       }
	    }
	 }


	 if (lookForAssign && assignDefinition.found_as_global) {
	    // it may be declared as a global with a class defined in the local scope
	    AbstractToken[] allLocalTokens = assignDefinition.getScope().getAllLocalTokens();
	    for (AbstractToken token : allLocalTokens) {
	       if (token.getRepresentation().equals(assignDefinition.getValueName())) {
		  if (token instanceof SourceToken) {
		     SourceToken srcToken = (SourceToken) token;
		     if (srcToken.getAst() instanceof ClassDef) {
			List<AbstractToken> classToks = ((SourceModule) assignDefinition.getModule())
				 .getClassToks(state, manager, srcToken.getAst());
			if (classToks.size() > 0) {
			   lookForAssign = false;
			   ret.addAll(classToks);
			   break;
			}
		     }
		  }
	       }
	    }
	 }
      }


      if (lookForAssign) {
	 // TODO: we might want to extend that later to check the return of some function
	 // for code-completion purposes...
	 state.setLookingFor(LookForType.ASSIGN);
	 CompletionState copy = state.getCopy();
	 if (definition.getAssignment() instanceof Attribute) {
	    copy.setActivationToken(NodeUtils.getFullRepresentationString(definition.getAssignment()));
	 }
	 else {
	    copy.setActivationToken(definition.getValueName());
	 }
	 copy.setLine(definition.getLine());
	 copy.setCol(definition.getCol());
	 module = definition.getModule();

	 state.checkDefinitionMemory(module, definition);

	 if (assignDefinition != null) {
	    Collection<AbstractToken> interfaceForLocal = assignDefinition.getScope()
		     .getInterfaceForLocal(assignDefinition.target_name);
	    ret.addAll(interfaceForLocal);
	 }

	 AbstractToken[] tks = manager.getCompletionsForModule(module, copy);
	 ret.addAll(Arrays.asList(tks));
      }
   }
}




/********************************************************************************/
/*										*/
/*	Visitor to find returns 						*/
/*										*/
/********************************************************************************/

private static class ReturnVisitor extends VisitorBase {

    static List<Return> findReturns(FunctionDef functionDef) {
       ReturnVisitor visitor = new ReturnVisitor();
       try {
	  for (stmtType b : functionDef.body){
	     if (b != null) {
		b.accept(visitor);
	      }
	   }
	}
       catch (Exception e) {
	  throw new RuntimeException(e);
	}
       return visitor.ret;
     }

    private ArrayList<Return> ret = new ArrayList<Return>();

    @Override public Object visitReturn(Return node) throws Exception {
       ret.add(node);
       return null;
     }

    @Override public void traverse(SimpleNode node) throws Exception {
       node.traverse(this);
     }

    @Override public Object visitClassDef(ClassDef node) throws Exception {
       return null;
     }

    @Override public Object visitFunctionDef(FunctionDef node) throws Exception {
       return null;
     }

    @Override protected Object unhandled_node(SimpleNode node) throws Exception {
       return null;
     }

}	// end of inner class ReturnVIsitor




} // end of class AbstractAnalysis


/* end of AbstractAnalysis.java */
