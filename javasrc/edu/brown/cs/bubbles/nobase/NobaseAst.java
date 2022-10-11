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

import java.awt.Point;
import java.util.List;

import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;



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





static Point getExtendedPosition(ASTNode n,NobaseFile f)
{
   ASTNode usen = n;
   switch (n.getNodeType()) {
      case ASTNode.VARIABLE_DECLARATION_FRAGMENT :
         ASTNode p = n.getParent();
         switch (p.getNodeType()) {
            case ASTNode.VARIABLE_DECLARATION_STATEMENT :
               VariableDeclarationStatement vds = (VariableDeclarationStatement) p;
               if (vds.fragments().get(0) == n) usen = p;
               break;
            default :
               System.err.println("CHECK HERE");
               break;
          }
         break;
      case ASTNode.FUNCTION_DECLARATION :
      case ASTNode.SINGLE_VARIABLE_DECLARATION :
      case ASTNode.TYPE_DECLARATION_STATEMENT :
      case ASTNode.VARIABLE_DECLARATION_STATEMENT :
         break;
      case ASTNode.TYPE_DECLARATION :
         p = n.getParent();
         switch (p.getNodeType()) {
            case ASTNode.TYPE_DECLARATION_STATEMENT :
               usen = p;
               break;
            default :
               break;
          }
         break;
      default :
         System.err.println("CHECK HERE");
         break;
    }
   
   int p3 = -1;
   JavaScriptUnit n0 = (JavaScriptUnit) usen.getRoot();
   ASTNode n1 = usen.getParent();
   StructuralPropertyDescriptor spd = usen.getLocationInParent();
   if (n1 != null && spd.isChildListProperty()) {
      List<?> plst = (List<?>) n1.getStructuralProperty(spd);
      int idx = plst.indexOf(usen);
      if (idx > 0) {
         ASTNode n2 = (ASTNode) plst.get(idx-1);
         p3 = n2.getStartPosition() + n2.getLength() + 1;
       }
    }
   
   int p1 = n0.getExtendedStartPosition(usen);
   int p2 = n0.getExtendedLength(usen);
   if (p3 > 0 && p3 < p1) {
      String cnts = f.getContents();
      while (Character.isWhitespace(cnts.charAt(p3))) ++p3;
      p2 += (p1-p3);
      p1 = p3;
    }
   
   return new Point(p1,p1 + p2);
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
