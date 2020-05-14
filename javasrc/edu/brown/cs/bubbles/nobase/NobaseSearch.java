/********************************************************************************/
/*										*/
/*		NobaseSearch.java						*/
/*										*/
/*	Search management for nobase						*/
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

import org.eclipse.jface.text.IDocument;

import org.eclipse.wst.jsdt.core.dom.*;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;




class NobaseSearch implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private NobaseMain	nobase_main;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseSearch(NobaseMain nm)
{
   nobase_main = nm;
}





/********************************************************************************/
/*										*/
/*	Text Search commands							*/
/*										*/
/********************************************************************************/

void handleTextSearch(String proj,int fgs,String pat,int maxresult,IvyXmlWriter xw)
	throws NobaseException
{
   Pattern pp = null;
   try {
      pp = Pattern.compile(pat,fgs);
    }
   catch (PatternSyntaxException e) {
      pp = Pattern.compile(pat,fgs|Pattern.LITERAL);
    }

   Pattern filepat = null;

   List<ISemanticData> sds = nobase_main.getProjectManager().getAllSemanticData(proj);
   int rct = 0;
   for (ISemanticData sd : sds) {
      if (sd == null) continue;
      NobaseFile ifd = sd.getFileData();
      if (filepat != null) {
	 String fnm = ifd.getFile().getPath();
	 Matcher m = filepat.matcher(fnm);
	 if (!m.matches()) continue;
       }
      IDocument d = ifd.getDocument();
      String s = d.get();
      Matcher m = pp.matcher(s);
      while (m.find()) {
	 if (++rct > maxresult) break;
	 xw.begin("MATCH");
	 xw.field("STARTOFFSET",m.start());
	 xw.field("LENGTH",m.end() - m.start());
	 xw.field("FILE",ifd.getFile().getPath());
	 FindOuterVisitor ov = new FindOuterVisitor(m.start(),m.end());
	 ASTNode root = sd.getRootNode();
	 if (root != null) {
	    try {
	       root.accept(ov);
	       ASTNode itm = ov.getItem();
	       if (itm != null) {
		  NobaseSymbol sym = NobaseAst.getDefinition(itm);
		  if (sym != null) {
		     NobaseUtil.outputName(sym,xw);
		   }
		}
	     }
	    catch (Exception e) { }
	  }
	 // find method here
	 xw.end("MATCH");
       }
    }
}



private class FindOuterVisitor extends ASTVisitor {

   private int start_offset;
   private int end_offset;
   private ASTNode item_found;

   FindOuterVisitor(int start,int end) {
      start_offset = start;
      end_offset = end;
      item_found = null;
    }

   ASTNode getItem()		{ return item_found; }

   @Override public boolean visit(JavaScriptUnit n) {
      return checkNode(n);
    }

   @Override public boolean visit(FunctionExpression n) {
      return checkNode(n);
    }

   private boolean checkNode(ASTNode n) {
      int soff = n.getStartPosition();
      int eoff = soff + n.getLength();	    if (soff < start_offset && eoff > end_offset) {
	 item_found = n;
       }
      if (start_offset > eoff) return false;
      if (end_offset < soff) return false;
      return true;
    }

}	// end of inner class FindOuterVisitor









}	// end of class NobaseSearch




/* end of NobaseSearch.java */

