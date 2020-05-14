/********************************************************************************/
/*										*/
/*		SourceToken.java						*/
/*										*/
/*	Python Bubbles Base representation of a source token			*/
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
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */

package edu.brown.cs.bubbles.pybase.symbols;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.Visitor;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.expr_contextType;
import org.python.pydev.parser.jython.ast.keywordType;


/**
 * @author Fabio Zadrozny
 */
public class SourceToken extends AbstractToken {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private SimpleNode	token_ast;
private FunctionDef	is_aliased;

private int[] colLineEndToFirstDot;
private int[] colLineEndComplete;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public SourceToken(SimpleNode node,String rep,String args,String doc,String parentpackage)
{
   super(rep,doc,args,parentpackage,getType(node));
   token_ast = node;
}


public SourceToken(SimpleNode node,String rep,String args,String doc,
		      String parentpackage,TokenType type)
{
   super(rep,doc,args,parentpackage,type);
   token_ast = node;
}


public SourceToken(SimpleNode node,String rep,String doc,String args,
		      String parentpackage,String originalrep,boolean originalhasrep)
{
   super(rep,doc,args,parentpackage,getType(node),originalrep,originalhasrep);
   token_ast = node;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public static TokenType getType(SimpleNode ast)
{
   if (ast instanceof ClassDef) {
      return TokenType.CLASS;
    }
   else if (ast instanceof FunctionDef) {
      return TokenType.FUNCTION;
    }
   else if (ast instanceof Name) {
      Name nm = (Name) ast;
      switch (nm.ctx) {
	 case expr_contextType.Param :
	    return TokenType.PARAM;
	 case expr_contextType.Store :
	    if (nm.parent != null && nm.parent instanceof Assign) {
	       if (nm.parent.parent != null && nm.parent.parent instanceof Module)
		  return TokenType.ATTR;
	       return TokenType.LOCAL;
	    }
	    break;
	 default :
	   break;
       }
      if (nm.parent != null && nm.parent instanceof Call) {
	 Call c = (Call) nm.parent;
	 if (c.func == nm) return TokenType.FUNCTION;
       }
      return TokenType.ATTR;
    }
   else if (ast instanceof Import || ast instanceof ImportFrom) {
      return TokenType.IMPORT;
    }
   else if (ast instanceof keywordType) {
      return TokenType.ATTR;
    }
   else if (ast instanceof Attribute) {
      return TokenType.ATTR;
    }

   return TokenType.UNKNOWN;
}


public void setAst(SimpleNode ast)
{
   token_ast = ast;
}


@Override public SimpleNode getAst()
{
   return token_ast;
}


public SimpleNode getNameOrNameTokAst()
{
   if (token_ast == null) return null;
   if (token_ast instanceof Name) return token_ast;
   if (token_ast instanceof NameTok) return token_ast;
   if (token_ast instanceof Attribute) return ((Attribute) token_ast).attr;
   if (token_ast instanceof FunctionDef) return ((FunctionDef) token_ast).name;
   if (token_ast instanceof ClassDef) return ((ClassDef) token_ast).name;
   return null;
}



public SimpleNode getNameOrNameTokAst(AbstractToken an) 
{
   if (an == null) return getNameOrNameTokAst();
   
   if (token_ast == null) return null;
   if (token_ast instanceof Name) return token_ast;
   if (token_ast instanceof NameTok) return token_ast;
   if (token_ast instanceof Attribute) {
      Attribute att = (Attribute) token_ast;
      SimpleNode s1 = att.attr;
      SimpleNode s2 = att.value;
      if (s2 != null && s2 instanceof Name) {
	 Name n2 = (Name) s2;
	 String nx = n2.id;
	 String ny = an.getRepresentation();
	 if (nx.equals(ny)) return s2;
      }
	
      return s1;
   }
   if (token_ast instanceof FunctionDef) return ((FunctionDef) token_ast).name;
   if (token_ast instanceof ClassDef) return ((ClassDef) token_ast).name;
   return null;
}  
   
 
public static SimpleNode getNameOrNameTokAst(SimpleNode sn)
{
   if (sn == null) return null;
   if (sn instanceof Name) return sn;
   if (sn instanceof NameTok) return sn;
   if (sn instanceof Attribute) {
     //  return ((Attribute) sn ).attr;
      return null;
    }
   if (sn instanceof FunctionDef) return ((FunctionDef) sn).name;
   return null;
}


public Name getNameAst()
{
   if (token_ast == null) return null;
   if (token_ast instanceof Name) return (Name) token_ast;
   if (token_ast instanceof Attribute) {
      Attribute at = (Attribute) token_ast;
      if (at.value instanceof Name) return (Name) at.value;
    }
   return null;
}



public NameTok getNameTokAst()
{
   if (token_ast == null) return null;
   if (token_ast instanceof NameTok) return (NameTok) token_ast;
   if (token_ast instanceof Attribute) return (NameTok)((Attribute) token_ast).attr;
   if (token_ast instanceof FunctionDef) return (NameTok)((FunctionDef) token_ast).name;
   return null;
}


/**
 * @return line starting at 1
 */
@Override public int getLineDefinition()
{
   if (token_ast == null) return 0;

   return NodeUtils.getLineDefinition(getRepresentationNode());
}



private SimpleNode getRepresentationNode()
{
   if (token_ast instanceof Attribute) {
      Attribute attr = (Attribute) token_ast;
      while (attr != null) {
	 String r = NodeUtils.getRepresentationString(attr);
	 if (r != null && r.equals(token_rep)) {
	    return attr;
	  }
	 if (attr.value instanceof Attribute) {
	    attr = (Attribute) attr.value;
	  }
	 else {
	    r = NodeUtils.getRepresentationString(attr.value);
	    if (r != null && r.equals(token_rep)) {
	       return attr.value;
	     }
	    break;
	  }
       }
    }
   return token_ast;
}


@Override public boolean isImport()
{
   if (token_ast instanceof Import || token_ast instanceof ImportFrom) {
      return true;
    }

   return false;
}

@Override public boolean isImportFrom()
{
   return token_ast instanceof ImportFrom;
}

@Override public boolean isWildImport()
{
   return AbstractVisitor.isWildImport(token_ast);
}

@Override public boolean isString()
{
   return AbstractVisitor.isString(token_ast);
}


/**
 * Updates the parameter, type and docstring based on another token (used for aliases).
 */
public void updateAliasToken(SourceToken methodTok)
{
   token_args = methodTok.getArgs();
   token_type = methodTok.getType();
   token_doc = methodTok.getDocStr();
   SimpleNode localAst = methodTok.getAst();
   if (localAst instanceof FunctionDef) {
      is_aliased = (FunctionDef) localAst;
    }
   else {
      is_aliased = methodTok.getAliased();
    }
}

/**
 * @return the function def to which this token is an alias to (or null if it's not an alias).
												*/
public FunctionDef getAliased()
{
   return is_aliased;
}




/********************************************************************************/
/*										*/
/*	Line/column computation and access methods				*/
/*										*/
/********************************************************************************/

@Override public int getColDefinition()
{
   if (token_ast == null) return 0;

   return NodeUtils.getColDefinition(token_ast);
}



public int getLineEnd(boolean getOnlyToFirstDot)
{
   if (getOnlyToFirstDot) {
      if (colLineEndToFirstDot == null) {
	 colLineEndToFirstDot = NodeUtils.getColLineEnd(getRepresentationNode(),
							   getOnlyToFirstDot);
       }
      return colLineEndToFirstDot[0];
    }
   else {
      if (colLineEndComplete == null) {
	 colLineEndComplete = NodeUtils.getColLineEnd(getRepresentationNode(),
							 getOnlyToFirstDot);
       }
      return colLineEndComplete[0];
    }
}



public int getColEnd(boolean getOnlyToFirstDot)
{
   if (getOnlyToFirstDot) {
      if (colLineEndToFirstDot == null) {
	 colLineEndToFirstDot = NodeUtils.getColLineEnd(getRepresentationNode(),
							   getOnlyToFirstDot);
       }
      return colLineEndToFirstDot[1];
    }
   else {
      if (colLineEndComplete == null) {
	 colLineEndComplete = NodeUtils.getColLineEnd(getRepresentationNode(),
							 getOnlyToFirstDot);
       }
      return colLineEndComplete[1];
    }
}


@Override public int[] getLineColEnd()
{
   // note this representaiton may not be accurate depending on which tokens we are dealing with

   if (token_ast instanceof NameTok || token_ast instanceof Name) {
      // those are the ones that we can be certain of...
      return new int[] { getLineDefinition(),
			    getColDefinition() + getRepresentation().length() };
    }
   // compute by looking at children of parent

   return findEndPosition(token_ast);
   // throw new RuntimeException("Unable to get the lenght of the token:"
   //	    + token_ast.getClass().getName());
}



/********************************************************************************/
/*										*/
/*	Comparison methods							*/
/*										*/
/********************************************************************************/

@Override public boolean equals(Object obj)
{
   if (!(obj instanceof SourceToken)) return false;

   SourceToken s = (SourceToken) obj;

   if (!s.getRepresentation().equals(getRepresentation())) return false;
   if (s.getLineDefinition() != getLineDefinition()) return false;
   if (s.getColDefinition() != getColDefinition()) return false;

   return true;
}

@Override public int hashCode()
{
   return 7 * getLineDefinition() * getColDefinition();
}




/********************************************************************************/
/*										*/
/*	End position finding methods						*/
/*										*/
/********************************************************************************/

private int [] findEndPosition(SimpleNode n)
{
   while (n != null) {
      if (n.parent == null) return null;
      EndFinder ef = new EndFinder(n);
      try {
	 n.parent.traverse(ef);
       }
      catch (Throwable t) {
	 return null;
       }
      int [] pos = ef.getEndPosition();
      if (pos != null) return pos;
      n = n.parent;
    }

   return null;
}



private class EndFinder extends Visitor {

   private SimpleNode start_node;
   private int end_line;
   private int end_col;

   EndFinder(SimpleNode n) {
      start_node = n;
      end_line = 0;
      end_col = 0;
    }

   @Override public void traverse(SimpleNode v) 	{ }
   @Override protected Object unhandled_node(SimpleNode n) {
      if (n.beginLine > start_node.beginLine ||
	     (n.beginLine == start_node.beginLine && n.beginColumn > start_node.beginColumn)) {
	 if (end_line == 0 ||
		n.beginLine < end_line ||
		(n.beginLine == end_line && n.beginColumn < end_col)) {
	    end_line = n.beginLine;
	    end_col = n.beginColumn;
	  }
       }
      return null;
    }

   int [] getEndPosition() {
      if (end_line == 0) return null;
      return new int [] { end_line, end_col };
    }

}	// end of inner class EndFinder



}	// end of class SourceToken




/* end of SourceToken.java */
