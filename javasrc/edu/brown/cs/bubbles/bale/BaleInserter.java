/********************************************************************************/
/*										*/
/*		BaleInserter.java						*/
/*										*/
/*	Bubble Annotated Language Editor factory for inserting code fragments	*/
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
import edu.brown.cs.bubbles.bueno.BuenoConstants;
import edu.brown.cs.bubbles.bueno.BuenoLocation;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;

import org.w3c.dom.Element;

import javax.swing.text.BadLocationException;
import javax.swing.text.Segment;

import java.io.File;
import java.util.List;



class BaleInserter implements BaleConstants, BudaConstants, BuenoConstants,
		BuenoConstants.BuenoInserter
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BumpClient	bump_client;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleInserter()
{
   bump_client = BumpClient.getBump();
}



/********************************************************************************/
/*										*/
/*	Insertion methods							*/
/*										*/
/********************************************************************************/

@Override public boolean insertText(BuenoLocation loc,String text,boolean format)
{
   BaleDocumentIde doc = findDocumentForLocation(loc);
   if (doc == null) return false;

   doc.baleWriteLock();
   try {
      int offset = findInsertionPoint(doc,loc);
      if (offset < 0) return false;
      doc.insertString(offset,text,null);
      loc.setFile(doc.getFile());

      int tln = text.length();
      String lstxt = doc.getLineSeparator();
      if (lstxt != null && lstxt.length() > 1) {
	 String atxt = text.replace("\n",lstxt);
	 tln = atxt.length();
       }
      loc.setLocation(offset,tln);
      
      if (format) {
         int dsoff = doc.mapOffsetToEclipse(offset);
         int deoff = doc.mapOffsetToEclipse(offset+text.length());
         BumpClient bc = BumpClient.getBump();
         Element edits = bc.format(doc.getProjectName(),doc.getFile(),dsoff,deoff);
         if (edits != null) {
            BaleApplyEdits bae = new BaleApplyEdits(doc);
            bae.applyEdits(edits);
          }
       }
    }
   catch (BadLocationException e) {
      return false;
    }
   finally { doc.baleWriteUnlock(); }

   return true;
}



/********************************************************************************/
/*										*/
/*	Methods to find the document						*/
/*										*/
/********************************************************************************/

private BaleDocumentIde findDocumentForLocation(BuenoLocation loc)
{
   String proj = loc.getProject();
   File f = loc.getFile();
   BaleDocumentIde doc = null;

   if (proj != null && f != null) {
      doc = BaleFactory.getFactory().getDocument(proj,f);
      if (doc != null) return doc;
    }

   String pkg = loc.getPackage();
   String cls = loc.getClassName();
   if (cls != null) {
      if (pkg != null && !cls.startsWith(pkg)) cls = pkg + "." + cls;
      List<BumpLocation> blocs = bump_client.findClassDefinition(proj,cls);
      if (blocs != null && blocs.size() > 0) {
	 BumpLocation bl = blocs.get(0);
	 doc = BaleFactory.getFactory().getDocument(bl.getSymbolProject(),bl.getFile());
	 if (doc != null) return doc;
       }
    }

   return doc;
}



/********************************************************************************/
/*										*/
/*	Methods to find the insertion point					*/
/*										*/
/********************************************************************************/

private int findInsertionPoint(BaleDocumentIde doc,BuenoLocation loc)
{
   int offset = loc.getOffset();
   if (offset >= 0) return offset;

   Segment s = new Segment();
   try {
      doc.getText(0,doc.getLength(),s);
    }
   catch (BadLocationException e) {
      return -1;
    }

   String proj = loc.getProject();

   boolean after = true;
   String item = loc.getInsertAfter();
   if (item == null) {
      item = loc.getInsertBefore();
      after = false;
    }
   if (item != null) {
      List<BumpLocation> locs = null;
      if (item.indexOf("(") > 0) {
	 locs = bump_client.findMethod(proj,item,false);
       }
      else {
	 if (item.indexOf("$") > 0) {
	    locs = bump_client.findClassDefinition(proj,item);
	   }
	 if (locs == null) {
	    locs = bump_client.findFields(proj,item,false,true);
	  }
       }
      if (locs != null && locs.size() > 0) {
	 BumpLocation bl = locs.get(0);
	 BaleRegion rgn = doc.getRegionFromLocation(bl);
	 if (rgn == null) return -1;
	 if (after) {
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
	    int off = rgn.getStart()-1;
	    while (off > 0) {
	       char c = s.charAt(off);
	       if (c == '\n') {
		  offset = off+1;
		  break;
		}
	       off -= 1;
	     }
	  }
       }
      if (offset > 0) return offset;
    }

   item = loc.getInsertAtEnd();
   if (item != null) {
      String pkg = loc.getPackage();
      String cls = loc.getClassName();
      if (pkg != null && cls != null && !cls.startsWith(pkg)) cls = pkg + "." + cls;
      int off = doc.getLength()-1;
      if (cls != null) {
	 List<BumpLocation> blocs = bump_client.findClassDefinition(proj,cls);
	 if (blocs != null && blocs.size() > 0) {
	    BumpLocation bl = blocs.get(0);
	    off = doc.mapOffsetToJava(bl.getEndOffset()-1);
	    if (off > doc.getLength() - 1) off = doc.getLength() - 1;
	  }
       }

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




}	 // end of class BaleInserter



/* end of BaleInserter.java */
