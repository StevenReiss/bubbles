/********************************************************************************/
/*										*/
/*		GlobalModelVisitor.java 					*/
/*										*/
/*	Python Bubbles Base visitor for building global context model		*/
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
 * Created on Nov 11, 2004
 *
 * @author Fabio Zadrozny
 */

package edu.brown.cs.bubbles.pybase.symbols;

import edu.brown.cs.bubbles.pybase.PybaseConstants.VisitorType;

import org.python.pydev.core.structure.FastStack;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.List;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.expr_contextType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;


/**
 * This class visits only the global context. Other visitors should visit contexts inside of this one.
 *
 * @author Fabio Zadrozny
 */
public class GlobalModelVisitor extends AbstractVisitor {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private VisitorType		visit_what;
private SourceToken		__all__;
private Assign			__all__assign;
private exprType[]		__all___assign_targets;
private FastStack<Assign>	last_assign = new FastStack<Assign>();
private boolean 		only_allow_tokens_in__all__;
private final Map<String,SourceToken> rep_to_token_with_args = new HashMap<>();




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public GlobalModelVisitor(VisitorType visitWhat,String moduleName,boolean onlyAllowTokensIn__all__)
{
   this(visitWhat,moduleName,onlyAllowTokensIn__all__,false);
}



public GlobalModelVisitor(VisitorType visitWhat,String moduleName,
	 boolean onlyAllowTokensIn__all__,boolean lookingInLocalContext)
{
   visit_what = visitWhat;
   module_name = moduleName;
   only_allow_tokens_in__all__ = onlyAllowTokensIn__all__;
   token_set.add(new SourceToken(new Name("__dict__",expr_contextType.Load,false),"__dict__","","",
	    moduleName));
   if (moduleName != null && moduleName.endsWith("__init__")) {
      token_set.add(new SourceToken(new Name("__path__",expr_contextType.Load,false),"__path__","",
	       "",moduleName));
   }
   if (!lookingInLocalContext && visit_what.match(VisitorType.GLOBAL_TOKENS)) {
      // __file__ is always available for any module
      token_set.add(new SourceToken(new Name("__file__",expr_contextType.Load,false),"__file__","",
	       "",moduleName));
      token_set.add(new SourceToken(new Name("__name__",expr_contextType.Load,false),"__name__","",
	       "",moduleName));
   }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

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
/*	Visitation methods							*/
/*										*/
/********************************************************************************/

@Override protected Object unhandled_node(SimpleNode node) throws Exception
{
   return null;
}



@Override public void traverse(SimpleNode node) throws Exception
{
   node.traverse(this);
}



@Override public Object visitClassDef(ClassDef node) throws Exception
{
   // when visiting the global namespace, we don't go into any inner scope.
   if (visit_what.match(VisitorType.GLOBAL_TOKENS)) {
      addToken(node);
    }
   return null;
}



@Override public Object visitFunctionDef(FunctionDef node) throws Exception
{
   // when visiting the global namespace, we don't go into any inner scope.
   if (visit_what.match(VisitorType.GLOBAL_TOKENS)) {
      addToken(node);
    }
   return null;
}



@Override public Object visitAssign(Assign node) throws Exception
{
   last_assign.push(node);
   node.traverse(this);
   last_assign.pop();
   return null;
}



@Override public Object visitName(Name node) throws Exception
{
   // when visiting the global namespace, we don't go into any inner scope.
   if (visit_what.match(VisitorType.GLOBAL_TOKENS)) {
      if (node.ctx == expr_contextType.Store) {
	 SourceToken added = addToken(node);
	 if (last_assign.size() > 0) {
	    Assign last = last_assign.peek();
	    if (added.getRepresentation().equals("__all__") && __all__assign == null) {
	       __all__ = added;
	       __all__assign = last;
	       __all___assign_targets = last.targets;
	    }
	    else {
	       if (last.value != null) {
		  String rep = NodeUtils.getRepresentationString(last.value);
		  if (rep != null) {
		     SourceToken methodTok = rep_to_token_with_args.get(rep);
		     if (methodTok != null) {
			// The use case is the following: we have a method and an assign to it:
			// def method(a, b):
			// ...
			// other = method
			//
			// and later on, we want the arguments for 'other' to be the same arguments for 'method'.
			added.updateAliasToken(methodTok);
		     }
		  }
	       }
	    }
	 }
      }
      else if (node.ctx == expr_contextType.Load) {
	 if (node.id.equals("__all__")) {
	    // if we find __all__ more than once, let's clear it (we can only have __all__
	    // = list of strings... if later
	    // an append, extend, etc is done in it, we have to skip this heuristic).
	    __all___assign_targets = null;
	 }
      }
   }
   return null;
}



@Override public Object visitImportFrom(ImportFrom node) throws Exception
{
   if (visit_what.match(VisitorType.WILD_MODULES)) {
      makeWildImportToken(node, token_set, module_name);
   }

   if (visit_what.match(VisitorType.ALIAS_MODULES)) {
      makeImportToken(node, token_set, module_name, true);
   }
   return null;
}



@Override public Object visitImport(Import node) throws Exception
{
   if (visit_what.match(VisitorType.ALIAS_MODULES)) {
      makeImportToken(node, token_set, module_name, true);
   }
   return null;
}



@Override public Object visitStr(Str node) throws Exception
{
   if (visit_what.match(VisitorType.MODULE_DOCSTRING)) {
      token_set.add(new SourceToken(node,node.s,"","",module_name));
   }
   return null;
}


@Override protected void finishVisit()
{
   if (only_allow_tokens_in__all__) {
      filterAll(token_set);
   }
}


/********************************************************************************/
/*										*/
/*	Utility methods 							*/
/*										*/
/********************************************************************************/

/**
 * This method will filter the passed tokens given the __all__ that was found when visiting.
 *
 * @param tokens the tokens to be filtered (IN and OUT parameter)
 */
public void filterAll(java.util.List<AbstractToken> tokens)
{
   if (__all__ != null) {
      SimpleNode ast = __all__.getAst();
      // just checking it
      if (__all___assign_targets != null && __all___assign_targets.length == 1
	       && __all___assign_targets[0] == ast) {
	 HashSet<String> validTokensInAll = new HashSet<String>();
	 exprType value = __all__assign.value;
	 exprType[] elts = null;
	 if (value instanceof List) {
	    List valueList = (List) value;
	    if (valueList.elts != null) {
	       elts = valueList.elts;
	    }
	 }
	 else if (value instanceof Tuple) {
	    Tuple valueList = (Tuple) value;
	    if (valueList.elts != null) {
	       elts = valueList.elts;
	    }
	 }

	 if (elts != null) {
	    for (exprType elt : elts) {
	       if (elt instanceof Str) {
		  Str str = (Str) elt;
		  validTokensInAll.add(str.s);
	       }
	    }
	 }


	 if (validTokensInAll.size() > 0) {
	    for (Iterator<AbstractToken> it = tokens.iterator(); it.hasNext();) {
	       AbstractToken tok = it.next();
	       if (!validTokensInAll.contains(tok.getRepresentation())) {
		  it.remove();
	       }
	    }
	 }
      }
   }
}



}	// end of class GlobalModelVisitor



/* end of GlobalModelVisitor.java */
