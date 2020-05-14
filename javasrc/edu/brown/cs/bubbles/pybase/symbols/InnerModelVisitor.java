/********************************************************************************/
/*										*/
/*		InnerModelVisitor.java						*/
/*										*/
/*	Python Bubbles Base visitor for building inner model			*/
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

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * This class is used to visit the inner context of class or a function.
 *
 * @author Fabio Zadrozny
 */
public class InnerModelVisitor extends AbstractVisitor {

/**
 * List that contains heuristics to find attributes.
 */
private final List<HeuristicFindAttrs> attrs_heuristics       = new ArrayList<HeuristicFindAttrs>();

private final Map<String, SourceToken> rep_to_token_with_args = new HashMap<String, SourceToken>();


@Override protected SourceToken addToken(SimpleNode node)
{
   SourceToken tok = super.addToken(node);
   if (tok.getArgs().length() > 0) {
      rep_to_token_with_args.put(tok.getRepresentation(), tok);
   }
   return tok;
}


public InnerModelVisitor(String moduleName,CompletionState state)
{
   module_name = moduleName;
   attrs_heuristics.add(new HeuristicFindAttrs(HeuristicFindAttrs.WHITIN_METHOD_CALL,
	    HeuristicFindAttrs.IN_KEYWORDS,"properties.create",moduleName,state,
	    rep_to_token_with_args));
   attrs_heuristics.add(new HeuristicFindAttrs(HeuristicFindAttrs.WHITIN_ANY,
	    HeuristicFindAttrs.IN_ASSIGN,"",moduleName,state,rep_to_token_with_args));
}

/**
 * This should be changed as soon as we know what should we visit.
 */
private static int VISITING_NOTHING = -1;

/**
 * When visiting class, get attributes and methods
 */
private static int VISITING_CLASS   = 0;

/**
 * Initially, we're visiting nothing.
 */
private int	visiting	 = VISITING_NOTHING;


/**
 * @see org.python.pydev.parser.jython.ast.VisitorBase#unhandled_node(org.python.pydev.parser.jython.SimpleNode)
 */
@Override protected Object unhandled_node(SimpleNode node) throws Exception
{
   return null;
}

/**
 * @see org.python.pydev.parser.jython.ast.VisitorBase#traverse(org.python.pydev.parser.jython.SimpleNode)
 */
@Override public void traverse(SimpleNode node) throws Exception
{
   node.traverse(this);
}

@Override public Object visitClassDef(ClassDef node) throws Exception
{
   if (visiting == VISITING_NOTHING) {
      visiting = VISITING_CLASS;
      node.traverse(this);

   }
   else if (visiting == VISITING_CLASS) {
      // that's a class within the class we're visiting
      addToken(node);
   }

   return null;
}

@Override public Object visitFunctionDef(FunctionDef node) throws Exception
{
   if (visiting == VISITING_CLASS) {
      addToken(node);

      // iterate heuristics to find attributes
      for (Iterator<HeuristicFindAttrs> iter = attrs_heuristics.iterator(); iter
	       .hasNext();) {
	 HeuristicFindAttrs element = iter.next();
	 element.visitFunctionDef(node);
	 addElementTokens(element);
      }

   }
   return null;
}

/**
 * @see org.python.pydev.parser.jython.ast.VisitorBase#visitAssign(org.python.pydev.parser.jython.ast.Assign)
 */
@Override public Object visitAssign(Assign node) throws Exception
{
   if (visiting == VISITING_CLASS) {

      // iterate heuristics to find attributes
      for (Iterator<HeuristicFindAttrs> iter = attrs_heuristics.iterator(); iter
	       .hasNext();) {
	 HeuristicFindAttrs element = iter.next();
	 element.visitAssign(node);
	 addElementTokens(element);
      }
   }
   return null;
}

/**
 * @see org.python.pydev.parser.jython.ast.VisitorBase#visitCall(org.python.pydev.parser.jython.ast.Call)
 */
@Override public Object visitCall(Call node) throws Exception
{
   if (visiting == VISITING_CLASS) {

      // iterate heuristics to find attributes
      for (Iterator<HeuristicFindAttrs> iter = attrs_heuristics.iterator(); iter
	       .hasNext();) {
	 HeuristicFindAttrs element = iter.next();
	 element.visitCall(node);
	 addElementTokens(element);
      }
   }
   return null;
}

/**
 * @param element
 */
private void addElementTokens(HeuristicFindAttrs element)
{
   token_set.addAll(element.token_set);
   element.token_set.clear();
}


} // end of class InnerModelVisitor


/* end of InnerModelVisitor.java */
