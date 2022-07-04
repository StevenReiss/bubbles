/********************************************************************************/
/*										*/
/*		NobaseAst.java							*/
/*										*/
/*	Abstract AST Definitions for NOBASE javascript				*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
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

/* SVN: $Id$ */



package edu.brown.cs.bubbles.nobase;

import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;



class NobaseAst implements NobaseConstants
{



/********************************************************************************/
/*										*/
/*	Property methods							*/
/*										*/
/********************************************************************************/

static void setReference(ASTNode n,NobaseSymbol s)
{
   n.setProperty(AST_PROPERTY_REF,s);
}



static NobaseSymbol getReference(ASTNode n)
{
   return (NobaseSymbol) n.getProperty(AST_PROPERTY_REF);
}



static void setDefinition(ASTNode n,NobaseSymbol s)
{
   n.setProperty(AST_PROPERTY_DEF,s);
}



static NobaseSymbol getDefinition(ASTNode n)
{
   return (NobaseSymbol) n.getProperty(AST_PROPERTY_DEF);
}



static void setNobaseName(ASTNode n,NobaseName s)
{
   n.setProperty(AST_PROPERTY_NAME,s);
}



static NobaseName getNobaseName(ASTNode n)
{
   return (NobaseName) n.getProperty(AST_PROPERTY_NAME);
}



static void setScope(ASTNode n,NobaseScope s)
{
   n.setProperty(AST_PROPERTY_SCOPE,s);
}



static NobaseScope getScope(ASTNode n)
{
   return (NobaseScope) n.getProperty(AST_PROPERTY_SCOPE);
}


static void setType(ASTNode n,NobaseType s)
{
   n.setProperty(AST_PROPERTY_TYPE,s);
}



static NobaseType getType(ASTNode n)
{
   return (NobaseType) n.getProperty(AST_PROPERTY_TYPE);
}


static boolean setNobaseValue(ASTNode n,NobaseValue s)
{
   NobaseValue nv = getNobaseValue(n);
   n.setProperty(AST_PROPERTY_VALUE,s);
   return (s != nv);
}



static NobaseValue getNobaseValue(ASTNode n)
{
   return (NobaseValue) n.getProperty(AST_PROPERTY_VALUE);
}



static void clearResolve(ASTNode n)
{
   n.setProperty(AST_PROPERTY_REF,null);
   n.setProperty(AST_PROPERTY_DEF,null);
   n.setProperty(AST_PROPERTY_NAME,null);
   n.setProperty(AST_PROPERTY_SCOPE,null);
   n.setProperty(AST_PROPERTY_TYPE,null);
   n.setProperty(AST_PROPERTY_VALUE,null);
}



/********************************************************************************/
/*										*/
/*	Position methods							*/
/*										*/
/********************************************************************************/

static int getStartPosition(ASTNode n)
{
   return n.getStartPosition();
}


static int getEndPosition(ASTNode  n)
{
   return n.getStartPosition() + n.getLength();
}


static int getExtendedStartPosition(ASTNode n)
{
   JavaScriptUnit ju = (JavaScriptUnit) n.getRoot();
   return ju.getExtendedStartPosition(n);
}


static int getExtendedEndPosition(ASTNode n)
{
   JavaScriptUnit ju = (JavaScriptUnit) n.getRoot();
   return ju.getExtendedStartPosition(n) + ju.getExtendedLength(n);
}



static int getLineNumber(ASTNode n)
{
   if (n == null) return 0;
   
   JavaScriptUnit ju = (JavaScriptUnit) n.getRoot();
   int lno = ju.getLineNumber(n.getStartPosition());
   if (lno < 0 && n.getParent() != null) {
      lno = ju.getLineNumber(n.getParent().getStartPosition());
    }
   if (lno < 0) {
      NobaseMain.logE("No line number for ASTNode " + n);
      lno = 0;
    }
   
   return lno;
}


static int getEndLine(ASTNode n)
{
   JavaScriptUnit ju = (JavaScriptUnit) n.getRoot();
   return ju.getLineNumber(n.getStartPosition()+n.getLength());
}


static int getColumn(ASTNode n)
{
   JavaScriptUnit ju = (JavaScriptUnit) n.getRoot();
   return ju.getColumnNumber(n.getStartPosition());
}


static int getEndColumn(ASTNode n)
{
   JavaScriptUnit ju = (JavaScriptUnit) n.getRoot();
   return ju.getColumnNumber(n.getStartPosition()+n.getLength());
}





}	// end of class NobaseAst




/* end of NobaseAst.java */
