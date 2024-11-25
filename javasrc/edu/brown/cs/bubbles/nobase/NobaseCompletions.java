/********************************************************************************/
/*										*/
/*		NobaseCompletions.java						*/
/*										*/
/*	Handle finding completions						*/
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

import edu.brown.cs.ivy.xml.IvyXmlWriter;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.DefaultASTVisitor;
import org.eclipse.wst.jsdt.core.dom.QualifiedName;
import org.eclipse.wst.jsdt.core.dom.SimpleName;



class NobaseCompletions implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private ISemanticData	semantic_data;
private NobaseFile	current_file;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseCompletions(ISemanticData isd)
{
   semantic_data = isd;
   if (isd == null) current_file = null;
   else current_file = isd.getFileData();
}



/********************************************************************************/
/*										*/
/*	Work methods							       */
/*										*/
/********************************************************************************/

void findCompletions(int offset,IvyXmlWriter xw)
{
   if (semantic_data == null) return;
   FindAstLocation finder = new FindAstLocation(offset);
   ASTNode root = semantic_data.getRootNode();
   if (root == null) return;
   root.accept(finder);
   ASTNode node = finder.getRelevantNode();
   if (node == null) return;
   NobaseScope scp = findScope(node);
   if (scp == null) return;
   if (current_file == null) return;

   Set<NobaseSymbol> syms = new HashSet<NobaseSymbol>();
   int spos = node.getStartPosition();
   int epos = spos + node.getLength();

   if (node instanceof SimpleName) {
      String pfx = null;
      SimpleName id = (SimpleName) node;
      pfx = id.getIdentifier();
      int sidx = id.getStartPosition();
      int eidx = sidx + id.getLength();
      if (eidx != offset && sidx < offset && pfx.length() > offset-sidx) {
	 pfx = pfx.substring(0,offset-sidx);
       }
      while (scp != null) {
	 for (NobaseSymbol sym : scp.getDefinedNames()) {
	    if (pfx == null || sym.getName().startsWith(pfx)) {
	       if (pfx == null || !sym.getName().equals(pfx))
		  syms.add(sym);
	    }
	 }
	 if (scp.getScopeType() == ScopeType.MEMBER) break;
	 scp = scp.getParent();
      }
   }
   else if (node instanceof QualifiedName) {
      spos = offset;
      epos = offset;
      for (NobaseSymbol sym : scp.getDefinedNames()) {
	 syms.add(sym);
      }
   }

   xw.begin("COMPLETIONS");

   for (NobaseSymbol sym : syms) {
      xw.begin("COMPLETION");
      xw.field("KIND","OTHER");
      xw.field("NAME",sym.getName());
      xw.field("TEXT",sym.getName());
      xw.field("REPLACE_START",spos);
      xw.field("REPLACE_END",epos);
      xw.field("RELVEANCE",1);
      xw.end("COMPLETION");
   }

   xw.end("COMPLETIONS");
}




/********************************************************************************/
/*										*/
/*	Ast node finder 							*/
/*										*/
/********************************************************************************/

private class FindAstLocation extends DefaultASTVisitor {

   private int for_offset;
   private ASTNode inner_node;

   FindAstLocation(int off) {
      for_offset = off;
      inner_node = null;
    }

   ASTNode getRelevantNode()			{ return inner_node; }

   @Override public boolean visitNode(ASTNode n) {
      int soff = n.getStartPosition();
      int eoff = soff + n.getLength();
      if (eoff < for_offset) return false;
      if (soff > for_offset) return false;
      inner_node = n;
      return true;
    }

}	// end of inner class FindAstLocation




/********************************************************************************/
/*										*/
/*	Find scope for ast node 						*/
/*										*/
/********************************************************************************/

NobaseScope findScope(ASTNode n)
{
   while (n != null) {
      NobaseScope scp = NobaseAst.getScope(n);
      if (scp != null) return scp;
      n = n.getParent();
    }
   return null;
}

}	// end of class NobaseCompletions




/* end of NobaseCompletions.java */

