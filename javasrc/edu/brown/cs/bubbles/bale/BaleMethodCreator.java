/********************************************************************************/
/*										*/
/*		BaleMethodCreator.java						*/
/*										*/
/*	Bubble Annotated Language Editor factory for creating new methods	*/
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bale;


import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;

import javax.swing.text.BadLocationException;
import javax.swing.text.Segment;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.StringTokenizer;



class BaleMethodCreator implements BaleConstants, BudaConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private String	for_project;
private String	class_name;
private String	method_name;
private String	method_params;
private String	method_returns;
private int	method_mods;
private boolean add_comment;
private BumpClient bump_client;

private String	insert_after;
private BumpLocation class_loc;


private static BaleFactory	bale_factory = BaleFactory.getFactory();



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleMethodCreator(String proj,String name,String params,String returns,
		     int modifiers,
		     boolean comment,
		     String after)
{
   for_project = proj;
   method_params = params;
   method_returns = returns;
   method_mods = modifiers;
   add_comment = comment;
   insert_after = after;
   class_loc = null;
   bump_client = BumpClient.getBump();

   int idx = name.lastIndexOf(".");
   if (idx < 0) {
      class_name = null;
      method_name = name;
    }
   else {
      class_name = name.substring(0,idx);
      method_name = name.substring(idx+1);
    }
}




/********************************************************************************/
/*										*/
/*	Entries to insert the method						*/
/*										*/
/********************************************************************************/

BumpLocation insertMethod()
{
   if (class_name == null) return null;

   BaleDocumentIde doc = findDocumentForClass();
   if (doc == null) return null;

   String mtxt = createMethodText();

   doc.baleWriteLock();
   try {
      int offset = -1;
      offset = findInsertionPoint(doc);
      if (offset < 0) return null;
      doc.insertString(offset,mtxt,null);
      return findLocation(doc,offset,mtxt.length());
    }
   catch (BadLocationException e) {
      return null;
    }
   finally { doc.baleWriteUnlock(); }

}



private BumpLocation findLocation(BaleDocumentIde doc,int offset,int len)
{
   List<BumpLocation> locs = bump_client.findMethod(for_project,method_name,false);
   if (locs == null) return null;

   for (BumpLocation loc : locs) {
      BaleRegion rgn = doc.getRegionFromLocation(loc);
      if (rgn == null) continue;
      int rs = rgn.getStart();
      int re = rgn.getEnd();

      if (offset < re && offset + len > rs) {
	 // regions overlap -- use it

	 return loc;
       }
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	Helper methods for finding document and position			*/
/*										*/
/********************************************************************************/

private BaleDocumentIde findDocumentForClass()
{
   List<BumpLocation> locs = bump_client.findClassDefinition(for_project,class_name);
   if (locs == null || locs.isEmpty()) return null;

   BumpLocation loc = locs.get(0);
   class_loc = loc;

   return bale_factory.getDocument(loc.getSymbolProject(),loc.getFile());
}



private int findInsertionPoint(BaleDocumentIde doc)
{
   Segment s = new Segment();
   try {
      doc.getText(0,doc.getLength(),s);
    }
   catch (BadLocationException e) {
      return -1;
    }

   int offset = -1;

   if (insert_after != null) {
      List<BumpLocation> locs = bump_client.findMethod(for_project,insert_after,false);
      if (locs == null || locs.size() == 0) return -1;
      BumpLocation loc = locs.get(0);
      BaleRegion rgn = doc.getRegionFromLocation(loc);
      int off = rgn.getEnd();
      int lasteol = -1;
      while (off < doc.getLength()) {
	 char c = s.charAt(off);
	 if (!Character.isWhitespace(c) && lasteol >= 0) break;
	 else if (c == '\n') lasteol = off;
	 ++off;
       }
      if (lasteol < 0) return -1;
      offset = lasteol+1;
    }
   else {
      int off;
      if (class_loc != null) {
	 off = doc.mapOffsetToJava(class_loc.getEndOffset()-1);
      }
      else off = doc.getLength()-1;

      if (off > doc.getLength()-1) off = doc.getLength()-1;
      boolean havebrace = false;
      // TODO: need to take comments into account
      while (off > 0) {
	 char c = s.charAt(off);
	 if (!havebrace && c == '}') havebrace = true;
	 else if (havebrace && c == '\n') {
	    offset = off+1;
	    break;
	 }
	 --off;
      }
   }
   if (offset < 0 || offset >= doc.getLength()) return -1;

   return offset;
}



/********************************************************************************/
/*										*/
/*	Methods for creating method text					*/
/*										*/
/********************************************************************************/

private String createMethodText()
{
   StringBuffer buf = new StringBuffer();

   if (add_comment) {
      buf.append("\n\n/**\n * " + method_name + "\n *\n **/");
    }
   buf.append("\n\n");

   int ct = 0;
   ct = addModifier(buf,"private",Modifier.isPrivate(method_mods),ct);
   ct = addModifier(buf,"public",Modifier.isPublic(method_mods),ct);
   ct = addModifier(buf,"protected",Modifier.isProtected(method_mods),ct);
   ct = addModifier(buf,"abstract",Modifier.isAbstract(method_mods),ct);
   ct = addModifier(buf,"static",Modifier.isStatic(method_mods),ct);
   ct = addModifier(buf,"native",Modifier.isNative(method_mods),ct);
   ct = addModifier(buf,"final",Modifier.isFinal(method_mods),ct);
   ct = addModifier(buf,"strictfp",Modifier.isStrict(method_mods),ct);
   ct = addModifier(buf,"syncronized",Modifier.isSynchronized(method_mods),ct);
   ct = addModifier(buf,"native",Modifier.isNative(method_mods),ct);

   if (ct > 0) buf.append(" ");
   if (method_returns == null || method_returns.trim().equals("")) buf.append("void");
   else buf.append(method_returns);
   buf.append(" ");

   buf.append(method_name);
   buf.append("(");
   if (method_params != null) {
      StringTokenizer tok = new StringTokenizer(method_params,",");
      ct = 0;
      if(tok.hasMoreTokens()) buf.append(tok.nextToken().trim());
      while (tok.hasMoreTokens()) {
	 buf.append(", ");
	 buf.append(tok.nextToken().trim());
	 //buf.append(" a" + ct);
	 ++ct;
       }
    }
   buf.append(")");

   if (Modifier.isAbstract(method_mods) || Modifier.isNative(method_mods)) {
      buf.append(";\n");
    }
   else {
      buf.append("\n{\n   // method body goes here");
      // TODO: if return type != void, add a return statement here
      if(method_returns != null && !method_returns.equals("void")){
	 if(method_returns.equals("boolean")) buf.append("\n    return false;");
	 else if(method_returns.equals("int") || method_returns.equals("float") || method_returns.equals("double") || method_returns.equals("short") || method_returns.equals("byte"))
	    buf.append("\n    return 0;");
	 else if(method_returns.equals("char")) buf.append("\n    return '\u0000';");
	 else buf.append("\n    return null;");
      }
      buf.append("    \n}\n");
    }

   buf.append("\n\n");

   return buf.toString();
}



private int addModifier(StringBuffer buf,String txt,boolean add,int ct)
{
   if (!add) return ct;

   if (ct > 0) buf.append(" ");
   buf.append(txt);
   return ct+1;
}




}	// end of class BaleMethodCreator




/* end of BaleMethodCreator.java */
