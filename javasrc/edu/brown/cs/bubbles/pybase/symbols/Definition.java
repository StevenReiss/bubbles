/********************************************************************************/
/*										*/
/*		Definition.java 						*/
/*										*/
/*	Python Bubbles Base generic definition representation			*/
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
 * Created on Jan 19, 2005
 *
 * @author Fabio Zadrozny
 */

package edu.brown.cs.bubbles.pybase.symbols;

import edu.brown.cs.bubbles.pybase.PybaseNature;

import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.parser.jython.SimpleNode;


/**
 * @author Fabio Zadrozny
 */
public class Definition {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private final int	 line_number;
private final int	 col_number;
private final String	 value_name;
private final AbstractModule for_module;
private final SimpleNode  assign_ast;
private final LocalScope in_scope;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public Definition(int line,int col,String value,SimpleNode ast,LocalScope scope,
		     AbstractModule module)
{
   this(line,col,value,ast,scope,module,false);
}



public Definition(int line,int col,String value,SimpleNode ast,LocalScope scope,
		     AbstractModule module,boolean foundAsLocal)
{
   assert(value != null);
   assert(module != null);

   line_number = line;
   col_number = col;
   value_name = value;
   assign_ast = ast;
   in_scope = scope;
   for_module = module;
}


public Definition(AbstractToken tok,LocalScope scope,AbstractModule module)
{
   this(tok,scope,module,false);
}



public Definition(AbstractToken tok,LocalScope scope,AbstractModule module,
		     boolean foundAsLocal)
{
   assert(tok != null);
   assert(module != null);

   line_number = tok.getLineDefinition();
   col_number = tok.getColDefinition();
   value_name = tok.getRepresentation();
   if (tok instanceof SourceToken) {
      assign_ast = ((SourceToken) tok).getAst();
    }
   else {
      assign_ast = null;
    }
   in_scope = scope;
   for_module = module;
}




/********************************************************************************/
/*										*/
/*	Standard methods							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   StringBuilder buffer = new StringBuilder("Definition=");
   buffer.append(value_name);
   buffer.append(" line=");
   buffer.append(line_number);
   buffer.append(" col=");
   buffer.append(col_number);
   return buffer.toString();
}



@Override public boolean equals(Object obj)
{
   if (!(obj instanceof Definition)) {
      return false;
    }

   Definition d = (Definition) obj;

   if (!value_name.equals(d.value_name)) {
      return false;
    }

   if (col_number != d.col_number) {
      return false;
    }

   if (line_number != d.line_number) {
      return false;
    }

   if (in_scope == d.in_scope) {
      return true;
    }
   if (in_scope == null || d.in_scope == null) {
      return false;
    }

   if (!in_scope.equals(d.in_scope)) {
      return false;
    }


   return true;
}

@Override public int hashCode()
{
   return value_name.hashCode() + col_number + line_number;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public AbstractModule getModule()		{ return for_module; }
public int getLine()				{ return line_number; }
public int getCol()				{ return col_number; }
public SimpleNode getAssignment()		{ return assign_ast; }
public String getValueName()			{ return value_name; }
public LocalScope getScope()			{ return in_scope; }


public String getDocstring(PybaseNature nature,ICompletionCache cache)
{
   if (assign_ast != null) {
      return NodeUtils.getNodeDocString(assign_ast);
    }
   else {
      if (value_name == null || value_name.trim().length() == 0) {
	 return for_module.getDocString();
       }
      else if (nature != null) {
	 AbstractASTManager manager = nature.getAstManager();
	 // It's the identification for some token in a module, let's try to find it
	 String[] headAndTail = FullRepIterable.headAndTail(value_name);
	 String actToken = headAndTail[0];
	 String qualifier = headAndTail[1];

	 AbstractToken[] globalTokens = for_module.getGlobalTokens(new CompletionState(
								      line_number,col_number,actToken,nature,qualifier,cache), manager);

	 for (AbstractToken iToken : globalTokens) {
	    String rep = iToken.getRepresentation();
	    // if the value is file.readlines, when a compiled module is asked, it'll
	    // return
	    // the module __builtin__ with a parent package of __builtin__.file and a
	    // representation  bstract
	    // of readlines, so, the qualifier matches the representation (and not the
	    // full value).
	    // Note that if we didn't have a dot, we wouldn't really need to check that.
	    if (value_name.equals(rep) || qualifier.equals(rep)) {
	       return iToken.getDocStr();
	     }
	  }
       }
    }

   return null;
}




}	// end of class Definition


/* end of Definition.java */
