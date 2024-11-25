/********************************************************************************/
/*										*/
/*		NobaseResolver.java						*/
/*										*/
/*	Handle symbol and type resolution for JavaScript			*/
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


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;


class NobaseResolver implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private NobaseScope		global_scope;
private NobaseProject		for_project;

private static Pattern	INTERNAL_NAME = Pattern.compile("_L\\d+");



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseResolver(NobaseProject proj,NobaseScope glbl)
{
   global_scope = glbl;
   for_project = proj;
}




/********************************************************************************/
/*										*/
/*	Remove Resolution methods						*/
/*										*/
/********************************************************************************/

void unresolve(ASTNode node)
{
   UnresolveVisitor uv = new UnresolveVisitor();
   node.accept(uv);
}


private static final class UnresolveVisitor extends ASTVisitor {

   @Override public void postVisit(ASTNode n) {
      NobaseAst.clearResolve(n);
    }

}	// end of inner class UnresolveVisitor



/********************************************************************************/
/*										*/
/*	Resolution methods							*/
/*										*/
/********************************************************************************/

void resolveSymbols(ISemanticData isd)
{
   ASTNode node = isd.getRootNode();
   if (node == null) return;

   unresolve(node);

   List<NobaseMessage> errors = new ArrayList<NobaseMessage>();

   NobaseValuePass vp = new NobaseValuePass(for_project,global_scope,isd.getFileData(),errors);
   for (int i = 0; i < 5; ++i) {
      NobaseMain.logD("BEGIN VALUE PASS " + i);
      node.accept(vp);
      if (!vp.checkChanged()) break;
      vp.setForceDefine();
    }

   isd.addMessages(errors);
}



/********************************************************************************/
/*										*/
/*	Name methods								*/
/*										*/
/********************************************************************************/

static boolean isGeneratedName(NobaseSymbol ns)
{
   return isGeneratedName(ns.getName());
}


static boolean isGeneratedName(String name)
{
   if (name == null) return false;
   if (name.equals("MISSING")) return true;
   if (name.equals("await")) return true;
   if (name.equals("async")) return true;
   Matcher m = INTERNAL_NAME.matcher(name); return m.matches();
}




}	// end of class NobaseResolver




/* end of NobaseResolver.java */

