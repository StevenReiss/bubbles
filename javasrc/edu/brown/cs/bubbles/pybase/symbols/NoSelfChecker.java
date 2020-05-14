/********************************************************************************/
/*										*/
/*		NoSelfChecker.java						*/
/*										*/
/*	Python Bubbles Base self variable checking				*/
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
/*
 * Created on 28/08/2005
 */


package edu.brown.cs.bubbles.pybase.symbols;


import edu.brown.cs.bubbles.pybase.PybaseConstants.ScopeType;
import edu.brown.cs.bubbles.pybase.PybaseSemanticVisitor;

import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.structure.FastStack;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.jython.ast.exprType;

import java.util.HashMap;
import java.util.Map;



public final class NoSelfChecker {


/********************************************************************************/
/*										*/
/*	Expected value exposed clas						*/
/*										*/
/********************************************************************************/

public static class Expected {

   private String expected_value;
   private String received_value;

   public Expected(String expected,String received) {
      expected_value = expected;
      received_value = received;
    }

   public String getExpected()			{ return expected_value; }
   public String getReceived()			{ return received_value; }
   public void setExpected(String v)		{ expected_value = v; }

}	// end of public inner class Expected



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private FastStack<ScopeType>	cur_scope;
private FastStack<HashMap<String,Tuple<Expected,FunctionDef>>> maybe_no_self_defined_items;
private FastStack<String>	class_bases;
private String			module_name;
private PybaseSemanticVisitor	the_visitor;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public NoSelfChecker(PybaseSemanticVisitor visitor,String moduleName)
{
   the_visitor = visitor;
   module_name = moduleName;
   cur_scope = new FastStack<ScopeType>();
   cur_scope.push(ScopeType.GLOBAL); // we start in the global scope
   maybe_no_self_defined_items = new FastStack<HashMap<String,Tuple<Expected,FunctionDef>>>();
   class_bases = new FastStack<String>();
}




/********************************************************************************/
/*										*/
/*	Public entry points from visitation					*/
/*										*/
/********************************************************************************/

public void beforeClassDef(ClassDef node)
{
   cur_scope.push(ScopeType.CLASS);

   StringBuilder buf = new StringBuilder();
   for (exprType base : node.bases) {
      if (buf.length() > 0) {
	 buf.append(",");
       }
      String rep = NodeUtils.getRepresentationString(base);
      if (rep != null) {
	 buf.append(FullRepIterable.getLastPart(rep));
       }
    }
   class_bases.push(buf.toString());
   maybe_no_self_defined_items.push(new HashMap<String,Tuple<Expected,FunctionDef>>());
}



public void afterClassDef(ClassDef node)
{
   cur_scope.pop();
   class_bases.pop();
   createMessagesForStack(maybe_no_self_defined_items);
}



/**
 * when a class is declared inside a function scope, it must start with self if it does
 * not start with the self parameter, unless it has a staticmethod decoration or is
 * later assigned to a staticmethod.
 *
 * @param node
 */
public void beforeFunctionDef(FunctionDef node)
{
   if (cur_scope.peek() == ScopeType.CLASS) {
      // let's check if we have to start with self or cls
      boolean startsWithSelf = false;
      boolean startsWithCls = false;
      String received = "";
      if (node.args != null) {

	 if (node.args.args.length > 0) {
	    exprType arg = node.args.args[0];

	    if (arg instanceof Name) {
	       Name n = (Name) arg;

	       if (n.id.equals("self")) {
		  startsWithSelf = true;
		}
	       else if (n.id.equals("cls")) {
		  startsWithCls = true;
		}
	       received = n.id;
	     }
	  }
       }

      boolean isStaticMethod = false;
      boolean isClassMethod = false;
      if (node.decs != null) {
	 for (decoratorsType dec : node.decs) {

	    if (dec != null) {
	       String rep = NodeUtils.getRepresentationString(dec.func);

	       if (rep != null) {

		  if (rep.equals("staticmethod")) {
		     isStaticMethod = true;
		   }
		  else if (rep.equals("classmethod")) {
		     isClassMethod = true;
		   }
		}
	     }
	  }
       }

      // didn't have staticmethod decorator either
      String rep = NodeUtils.getRepresentationString(node);
      if (rep.equals("__new__")) {

	 // __new__ could start wit cls or self
	 if (!startsWithCls && !startsWithSelf) {
	    maybe_no_self_defined_items.peek().put(
	       rep,
	       new Tuple<Expected,FunctionDef>(
		  new Expected("self or cls",received),node));
	  }

       }
      else if (!startsWithSelf && !startsWithCls && !isStaticMethod && !isClassMethod) {
	 maybe_no_self_defined_items.peek().put(rep,new Tuple<Expected,FunctionDef>(new Expected("self",received),node));
       }
      else if (startsWithCls && !isClassMethod && !isStaticMethod) {
	 String classBase = class_bases.peek();
	 if (rep.equals("__init__") && "type".equals(classBase)) {
	    // ok, in this case, cls is expected
	  }
	 else {
	    maybe_no_self_defined_items.peek() .put(rep,
						       new Tuple<Expected,FunctionDef>(
						       new Expected("self",received),node));
	  }
       }
    }

   cur_scope.push(ScopeType.METHOD);
}



public void afterFunctionDef(FunctionDef node)
{
   cur_scope.pop();
}



public void visitAssign(Assign node)
{
   // we're looking for xxx = staticmethod(xxx)
   if (node.targets.length == 1) {
      exprType t = node.targets[0];
      String rep = NodeUtils.getRepresentationString(t);
      if (rep == null) {
	 return;
       }

      if (cur_scope.peek() != ScopeType.CLASS) {
	 // we must be in a class scope
	 return;
       }

      Tuple<Expected,FunctionDef> tup = maybe_no_self_defined_items.peek().get(rep);
      if (tup == null) {
	 return;
       }

      FunctionDef def = tup.o2;
      if (def == null) {
	 return;
       }

      // ok, it may be a staticmethod, let's check its value (should be a call)
      exprType expr = node.value;
      if (expr instanceof Call) {
	 Call call = (Call) expr;
	 if (call.args.length == 1) {
	    String argRep = NodeUtils.getRepresentationString(call.args[0]);
	    if (argRep != null && argRep.equals(rep)) {
	       String funcCall = NodeUtils.getRepresentationString(call.func);
	       if (def != null && funcCall != null && funcCall.equals("staticmethod")) {
		  // ok, finally... it is a staticmethod after all...
		  maybe_no_self_defined_items.peek().remove(rep);
		}
	       else if (funcCall != null && funcCall.equals("classmethod")) {
		  // ok, finally... it is a classmethod after all...
		  tup.o1.setExpected("cls");
		}
	     }
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

/**
 * @param stack
 * @param shouldBeDefined
 */
private void createMessagesForStack(FastStack<HashMap<String,Tuple<Expected,FunctionDef>>> stack)
{
   HashMap<String,Tuple<Expected,FunctionDef>> noDefinedItems = stack.pop();
   for (Map.Entry<String,Tuple<Expected,FunctionDef>> entry : noDefinedItems.entrySet()) {
      Expected expected = entry.getValue().o1;
      if (!expected.getExpected().equals(expected.getReceived())) {
	 SourceToken token = AbstractVisitor.makeToken(entry.getValue().o2,module_name);
	 the_visitor.onAddNoSelf(token,
				    new Object[] { token,entry.getValue().o1.getExpected() });
       }
    }
}




}	// end of class NoSelfChecker




/* end of NoSelfChecker.java */
