/********************************************************************************/
/*										*/
/*		HeuristicFindAttrs.java 					*/
/*										*/
/*	Python Bubbles Base visitor for finding attributes			*/
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
 * Created on Dec 21, 2004
 *
 * @author Fabio Zadrozny
 */

package edu.brown.cs.bubbles.pybase.symbols;

import edu.brown.cs.bubbles.pybase.PybaseConstants.LookForType;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.exprType;

import java.util.List;
import java.util.Map;
import java.util.Stack;


/**
 * This class defines how we should find attributes.
 *
 * Heuristics provided allow someone to find an attr inside a function definition (IN_INIT or IN_ANY)
 * or inside a method call (e.g. a method called properties.create(x=0) - that's what I use, so, that's specific).
 * Other uses may be customized later, once we know which other uses are done.
 *
 * @author Fabio Zadrozny
 */

public class HeuristicFindAttrs extends AbstractVisitor {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

/**
 * Whether we should add the attributes that are added as 'self.xxx = 10'
 */
private boolean 		discover_self_attrs = true;

private final Map<String, SourceToken> rep_to_token_with_args;

public Stack<SimpleNode> node_stack	  = new Stack<>();

public static final int  WHITIN_METHOD_CALL  = 0;
public static final int  WHITIN_INIT	 = 1;
public static final int  WHITIN_ANY	  = 2;

public int	       find_where	  = -1;


public static final int  IN_ASSIGN	   = 0;
public static final int  IN_KEYWORDS	 = 1;

public int	       find_how     = -1;

private boolean   entry_point_correct = false;

private boolean   in_assing	   = false;
private boolean   in_func_def	 = false;

/**
 * This is the method that can be used to declare them (e.g. properties.create)
 * It's only used it it is a method call.
 */
public String	    method_call  = "";




/********************************************************************************/
/*										*/
/*	Consructors								*/
/*										*/
/********************************************************************************/

public HeuristicFindAttrs(int where,int how,String methodCall,String moduleName,
	 CompletionState state,Map<String, SourceToken> repToTokenWithArgs)
{
   find_where = where;
   find_how = how;
   method_call = methodCall;
   module_name = moduleName;
   rep_to_token_with_args = repToTokenWithArgs;
   if (state != null) {
      if (state.getLookingFor() == LookForType.CLASSMETHOD_VARIABLE) {
	 discover_self_attrs = false;
      }
   }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override protected Object unhandled_node(SimpleNode node) throws Exception
{
   return null;
}

@Override public void traverse(SimpleNode node) throws Exception {}


@Override protected SourceToken addToken(SimpleNode node)
{
   SourceToken tok = super.addToken(node);
   if (tok.getArgs().length() > 0) {
      rep_to_token_with_args.put(tok.getRepresentation(), tok);
   }
   return tok;
}



/********************************************************************************/
/*										*/
/*	Visitation points							*/
/*										*/
/********************************************************************************/

@Override public Object visitCall(Call node) throws Exception
{
   if (entry_point_correct == false && method_call.length() > 0) {
      entry_point_correct = true;


      if (node.func instanceof Attribute) {
	 List<String> c = StringUtils.dotSplit(method_call);

	 Attribute func = (Attribute) node.func;
	 if (((NameTok) func.attr).id.equals(c.get(1))) {

	    if (func.value instanceof Name) {
	       Name name = (Name) func.value;
	       if (name.id.equals(c.get(0))) {
		  for (int i = 0; i < node.keywords.length; i++) {
		     addToken(node.keywords[i]);
		  }
	       }
	    }
	 }
      }

      entry_point_correct = false;
   }
   return null;
}




@Override public Object visitFunctionDef(FunctionDef node) throws Exception
{
   node_stack.push(node);
   if (entry_point_correct == false) {
      entry_point_correct = true;
      in_func_def = true;

      if (find_where == WHITIN_ANY) {
	 node.traverse(this);

      }
      else if (find_where == WHITIN_INIT && node.name.toString().equals("__init__")) {
	 node.traverse(this);
      }
      entry_point_correct = false;
      in_func_def = false;
   }
   node_stack.pop();

   return null;
}



@Override public Object visitClassDef(ClassDef node) throws Exception
{
   node_stack.push(node);
   Object r = super.visitClassDef(node);
   node_stack.pop();
   return r;
}



@Override public Object visitAssign(Assign node) throws Exception
{
   if (find_how == IN_ASSIGN) {
      in_assing = true;

      exprType value = node.value;
      String rep = NodeUtils.getRepresentationString(value);
      SourceToken methodTok = null;
      if (rep != null) {
	 methodTok = rep_to_token_with_args.get(rep);
	 // The use case is the following: we have a method and an assign to it:
	 // def method(a, b):
	 // ...
	 // other = method
	 //
	 // and later on, we want the arguments for 'other' to be the same arguments for
	 // 'method'.
      }

      for (int i = 0; i < node.targets.length; i++) {
	 if (node.targets[i] instanceof Attribute) {
	    visitAttribute((Attribute) node.targets[i]);

	 }
	 else if (node.targets[i] instanceof Name && in_func_def == false) {
	    String id = ((Name) node.targets[i]).id;
	    if (id != null) {
	       SourceToken added = addToken(node.targets[i]);
	       if (methodTok != null) {
		  added.updateAliasToken(methodTok);
	       }
	    }

	 }
	 else if (node.targets[i] instanceof Tuple && in_func_def == false) {
	    // that's for finding the definition: a,b,c = range(3) inside a class definition
	    Tuple tuple = (Tuple) node.targets[i];
	    for (exprType t : tuple.elts) {
	       if (t instanceof Name) {
		  String id = ((Name) t).id;
		  if (id != null) {
		     addToken(t);
		  }
	       }
	    }

	 }
      }

      in_assing = false;
   }
   return null;
}




@Override public Object visitAttribute(Attribute node) throws Exception
{
   if (find_how == IN_ASSIGN && in_assing) {
      if (node.value instanceof Name) {
	 String id = ((Name) node.value).id;
	 if (id != null) {
	    if (discover_self_attrs) {
	       if (id.equals("self")) {
		  addToken(node);

	       }
	    }
	    else {
	       if (id.equals("cls")) {
		  addToken(node);
	       }
	    }
	 }
      }
   }
   return null;
}




@Override public Object visitIf(If node) throws Exception
{
   node.traverse(this);
   return null;
}



}	// end of class HeuristicFindAttrs



/* end of HeuristicFindAttrs.java */
