/********************************************************************************/
/*										*/
/*		RebaseJavaAst.java						*/
/*										*/
/*	Auxilliary methods for using ASTs in Rebase				*/
/*										*/
/********************************************************************************/
/*	Copyright 2007 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2007, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/



package edu.brown.cs.bubbles.rebase.java;

import edu.brown.cs.bubbles.rebase.RebaseMain;
import edu.brown.cs.bubbles.rebase.RebaseRequest;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.SimpleName;


abstract class RebaseJavaAst implements RebaseJavaConstants {



/********************************************************************************/
/*										*/
/*	Scope Properties							*/
/*										*/
/********************************************************************************/

static RebaseJavaScope getJavaScope(ASTNode n)
{
   return (RebaseJavaScope) n.getProperty(PROP_JAVA_SCOPE);
}


static void setJavaScope(ASTNode n,RebaseJavaScope s)
{
   n.setProperty(PROP_JAVA_SCOPE,s);
}



/********************************************************************************/
/*										*/
/*	Type properties 							*/
/*										*/
/********************************************************************************/

static RebaseJavaType getJavaType(ASTNode n)
{
   if (n == null) return null;

   return (RebaseJavaType) n.getProperty(PROP_JAVA_TYPE);
}


static void setJavaType(ASTNode n,RebaseJavaType t)
{
   n.setProperty(PROP_JAVA_TYPE,t);
}


static String getJavaTypeName(ASTNode n)
{
   RebaseJavaType jt = getJavaType(n);
   if (jt == null) return null;
   return jt.getName();
}




/********************************************************************************/
/*										*/
/*	Symbol reference properties						*/
/*										*/
/********************************************************************************/

static RebaseJavaSymbol getReference(ASTNode n)
{
   return (RebaseJavaSymbol) n.getProperty(PROP_JAVA_REF);
}


static void setReference(ASTNode n,RebaseJavaSymbol js)
{
   n.setProperty(PROP_JAVA_REF,js);
   js.noteUsed();
}



/********************************************************************************/
/*										*/
/*	Symbol definition properties						*/
/*										*/
/********************************************************************************/

static RebaseJavaSymbol getDefinition(ASTNode n)
{
   return (RebaseJavaSymbol) n.getProperty(PROP_JAVA_DEF);
}



static void setDefinition(ASTNode n,RebaseJavaSymbol t)
{
   n.setProperty(PROP_JAVA_DEF,t);
}



/********************************************************************************/
/*										*/
/*	Expression type properties						*/
/*										*/
/********************************************************************************/

static RebaseJavaType getExprType(ASTNode n)
{
   return (RebaseJavaType) n.getProperty(PROP_JAVA_ETYPE);
}


static void setExprType(ASTNode n,RebaseJavaType t)
{
   n.setProperty(PROP_JAVA_ETYPE,t);
}



/********************************************************************************/
/*										*/
/*	Source methods								*/
/*										*/
/********************************************************************************/

static RebaseSource getSource(ASTNode n)
{
   return (RebaseSource) n.getProperty(PROP_JAVA_SOURCE);
}



static void setSource(ASTNode n,RebaseSource s)
{
   n.setProperty(PROP_JAVA_SOURCE,s);
}




/********************************************************************************/
/*										*/
/*	Request methods 							*/
/*										*/
/********************************************************************************/

static RebaseRequest getSearchRequest(ASTNode n)
{
   return (RebaseRequest) n.getProperty(PROP_JAVA_REQUEST);
}



static void setSearchRequest(ASTNode n,RebaseRequest s)
{
   n.setProperty(PROP_JAVA_REQUEST,s);
}




/********************************************************************************/
/*										*/
/*	Root methods								*/
/*										*/
/********************************************************************************/

static void setKeep(ASTNode n)
{
   n = n.getRoot();
   n.setProperty(PROP_JAVA_KEEP,Boolean.TRUE);
}



static boolean isKeep(ASTNode n)
{
   n = n.getRoot();
   return n.getProperty(PROP_JAVA_KEEP) == Boolean.TRUE;
}



static RebaseJavaTyper getTyper(ASTNode n)
{
   n = n.getRoot();
   return (RebaseJavaTyper) n.getProperty(PROP_JAVA_TYPER);
}



/********************************************************************************/
/*										*/
/*	General property methods						*/
/*										*/
/********************************************************************************/

static void clearAll(ASTNode n)
{
   if (n == null) return;

   n.setProperty(PROP_JAVA_TYPE,null);
   n.setProperty(PROP_JAVA_SCOPE,null);
   n.setProperty(PROP_JAVA_REF,null);
   n.setProperty(PROP_JAVA_ETYPE,null);
   n.setProperty(PROP_JAVA_DEF,null);
   n.setProperty(PROP_JAVA_RESOLVED,null);
   n.setProperty(PROP_JAVA_TYPER,null);
}



/********************************************************************************/
/*										*/
/*	Methods for handling names						*/
/*										*/
/********************************************************************************/

static Name getQualifiedName(AST ast,String s)
{
   synchronized (ast) {
      int idx = s.lastIndexOf(".");
      if (idx < 0) {
	 try {
	    return ast.newSimpleName(s);
	  }
	 catch (IllegalArgumentException e) {
	    RebaseMain.logE("ILLEGAL NAME: `" + s + "'");
	    return ast.newSimpleName("REBASE_ILLEGAL_NAME");
	  }
       }
      else {
	 try {
	    return ast.newQualifiedName(getQualifiedName(ast,s.substring(0,idx)),
					   ast.newSimpleName(s.substring(idx+1)));
	  }
	 catch (IllegalArgumentException e) {
	    System.err.println("PROBLEM CREATING NEW NAME FOR " + s + ": " + e);
	    throw e;
	  }
       }
    }
}



static SimpleName getSimpleName(AST ast,String s)
{
   synchronized (ast) {
      try {
	 return ast.newSimpleName(s);
       }
      catch (IllegalArgumentException e) {
	 System.err.println("PROBLEM CREATING NEW SIMPLE NAME FOR " + s + ": " + e);
	 throw e;
       }
    }
}




/********************************************************************************/
/*										*/
/*	Methods for creating special AST nodes					*/
/*										*/
/*	These are needed because some AST operations are not thread safe	*/
/*										*/
/********************************************************************************/

static NumberLiteral newNumberLiteral(AST ast,int v)
{
   return newNumberLiteral(ast,Integer.toString(v));
}


static NumberLiteral newNumberLiteral(AST ast,long v)
{
   return newNumberLiteral(ast,Long.toString(v));
}


static NumberLiteral newNumberLiteral(AST ast,String v)
{
   synchronized (ast) {
      return ast.newNumberLiteral(v);
    }
}



}	// end of abstract class RebaseJavaAst




/* end of RebaseJavaAst.java */
