/********************************************************************************/
/*										*/
/*		FindScopeVisitor.java						*/
/*										*/
/*	Python Bubbles Base visitor for finding scopes				*/
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
 * Created on Jan 20, 2005
 *
 * @author Fabio Zadrozny
 */

package edu.brown.cs.bubbles.pybase.symbols;

import org.python.pydev.core.structure.FastStack;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Module;


/**
 * @author Fabio Zadrozny
 */
public class FindScopeVisitor extends AbstractVisitor {



/********************************************************************************/
/*										*/
/*	Private stoarge 							*/
/*										*/
/********************************************************************************/

private FastStack<SimpleNode> stack_scope = new FastStack<SimpleNode>();
private LocalScope	for_scope = new LocalScope(new FastStack<SimpleNode>());
private boolean is_found = false;
private int		for_line;
private int		for_col;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected FindScopeVisitor()
{ }



public FindScopeVisitor(int line,int col)
{
   for_line = line;
   for_col = col;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

LocalScope getScope()			{ return for_scope; }




/********************************************************************************/
/*										*/
/*	Visitation methods							*/
/*										*/
/********************************************************************************/

@Override protected Object unhandled_node(SimpleNode node) throws Exception
{
   // the line passed in starts at 1 and the lines for the visitor nodes start at 0
   if (!is_found && !(node instanceof Module)) {
      if (for_line <= node.beginLine) {
	 // scope is locked at this time.
	 is_found = true;
	 int original = for_scope.getIfMainLine();
	 for_scope = new LocalScope(stack_scope.createCopy());
	 for_scope.setIfMainLine(original);
      }
   }
   else {
      if (for_scope.getScopeEndLine() == -1 && for_line < node.beginLine
	       && for_col >= node.beginColumn) {
	 for_scope.setScopeEndLine(node.beginLine);
      }
   }
   return node;
}



@Override public void traverse(SimpleNode node) throws Exception
{
   node.traverse(this);
}


@Override public Object visitIf(If node) throws Exception
{
   checkIfMainNode(node);
   return super.visitIf(node);
}



@Override public Object visitClassDef(ClassDef node) throws Exception
{
   if (!is_found) {
      stack_scope.push(node);
      node.traverse(this);
      stack_scope.pop();
   }
   return null;
}



@Override public Object visitFunctionDef(FunctionDef node) throws Exception
{
   if (!is_found) {
      stack_scope.push(node);
      node.traverse(this);
      stack_scope.pop();
   }
   return null;
}

@Override public Object visitModule(Module node) throws Exception
{
   stack_scope.push(node);
   return super.visitModule(node);
}



/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

protected void checkIfMainNode(If node)
{
   boolean isIfMainNode = NodeUtils.isIfMainNode(node);
   if (isIfMainNode) {
      for_scope.setIfMainLine(node.beginLine);
   }
}



} // end of class FindScopeVisitor


/* end of FindScopeVisitor.java */
