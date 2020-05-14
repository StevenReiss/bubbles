/********************************************************************************/
/*										*/
/*		DuplicationChecker.java 					*/
/*										*/
/*	Python Bubbles Base duplicate symbol checker				*/
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
 * Created on 24/07/2005
 */

package edu.brown.cs.bubbles.pybase.symbols;

import edu.brown.cs.bubbles.pybase.PybaseSemanticVisitor;

import org.python.pydev.core.structure.FastStack;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.decoratorsType;

import java.util.HashMap;
import java.util.Map;


/**
 * Used to check for duplicated signatures
 *
 * @author Fabio
 */

public final class DuplicationChecker {


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private FastStack<Map<String, String>> dup_stack = new FastStack<Map<String, String>>();
private PybaseSemanticVisitor		  pybase_visitor;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public DuplicationChecker(PybaseSemanticVisitor visitor)
{
   pybase_visitor = visitor;
   startScope("", null);
}


/********************************************************************************/
/*										*/
/*	Entry methods								*/
/*										*/
/********************************************************************************/

public void beforeClassDef(ClassDef node)
{
   startScope(NodeUtils.getRepresentationString(node), node);
}

public void afterClassDef(ClassDef node)
{
   endScope(NodeUtils.getRepresentationString(node));
}


public void beforeFunctionDef(FunctionDef node)
{
   startScope(NodeUtils.getRepresentationString(node), node);
}

public void afterFunctionDef(FunctionDef node)
{
   endScope(NodeUtils.getRepresentationString(node));
}


/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void startScope(String name,SimpleNode node)
{
   checkDuplication(name, node);
   Map<String, String> item = new HashMap<>();
   dup_stack.push(item);
}


private void endScope(String name)
{
   dup_stack.pop();
   dup_stack.peek().put(name, name);
}


private void checkDuplication(String name,SimpleNode node)
{
   if (dup_stack.size() > 0) {
      if (!pybase_visitor.getScope().getPrevScopeItems().getIsInSubSubScope()) {
	 String exists = dup_stack.peek().get(name);
	 if (exists != null) {
	    if (node instanceof FunctionDef) {
	       FunctionDef functionDef = (FunctionDef) node;
	       if (functionDef.decs != null && functionDef.decs.length > 0) {
		  for (decoratorsType dec : functionDef.decs) {
		     if (dec.func != null) {
			String fullRepresentationString = NodeUtils
				 .getFullRepresentationString(dec.func);
			if (fullRepresentationString.startsWith(name + ".")) {
			   return;
			}
		     }
		  }
	       }

	    }
	    SourceToken token = AbstractVisitor.makeToken(node, "");
	    pybase_visitor.onAddDuplicatedSignature(token, name);
	 }
      }
   }
}


}	// end of class DuplicationChecker


/* end of DuplicationsChecker.java */
