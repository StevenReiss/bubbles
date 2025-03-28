/********************************************************************************/
/*										*/
/*		BaleApplyEdits.java						*/
/*										*/
/*	Bubble Annotated Language Editor context for applying eclipse edits	*/
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

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.burp.BurpHistory;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;



class BaleApplyEdits implements BaleConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BaleDocument	for_document;
private Map<String,Position> move_targets;
private Map<String,String> source_strings;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleApplyEdits()
{
   for_document = null;
   move_targets = null;
   source_strings = null;
}


BaleApplyEdits(BaleDocument bd)
{
   this();
   for_document = bd;
}



/********************************************************************************/
/*										*/
/*	Editing methods 							*/
/*										*/
/********************************************************************************/

void applyEdits(Element xml)
{
   applyLocalEdits(xml);
   applyResourceEdits(xml);
}



private void applyLocalEdits(Element xml)
{
   if (IvyXml.isElement(xml,"EDITS") || IvyXml.isElement(xml,"RESULT") ||
	 IvyXml.isElement(xml,"REPAIREDIT")) {
      for (Element c : IvyXml.children(xml)) applyLocalEdits(c);
    }
   else if (IvyXml.isElement(xml,"CHANGE")) {
      String typ = IvyXml.getAttrString(xml,"TYPE");
      if (typ == null) return;
      else if (typ.equals("COMPOSITE")) {
	 for (Element c : IvyXml.children(xml)) applyLocalEdits(c);
       }
      else if (typ.equals("EDIT")) {
	 Element r = IvyXml.getChild(xml,"RESOURCE");
	 if (r != null) {
	    String proj = IvyXml.getAttrString(r,"PROJECT");
	    File fil = new File(IvyXml.getAttrString(r,"LOCATION"));
	    BaleDocumentIde bde = BaleFactory.getFactory().getDocument(proj,fil);
	    bde.flushEdits();
	    for_document = bde;
	  }
	 Set<Element> edits = new TreeSet<Element>(new EditSorter());
	 extractEdits(xml,edits);
	 for (Element ed : edits) {
	    applyEdit(ed);
	  } 
       }
    }
   else if (IvyXml.isElement(xml,"EDIT")) {
      String typ = IvyXml.getAttrString(xml,"TYPE");
      if (typ == null) return;
      JTextComponent tc = BaleFactory.getFactory().getTextComponent(for_document);
      BurpHistory bh = BurpHistory.getHistory();
      BoardLog.logD("BALE","Found text component for apply: " + tc);
      bh.beginEditAction(tc);
      try {
	 if (typ.equals("MULTI")) {
	    List<Element> edits = new ArrayList<>();
	    extractEdits(xml,edits);
	    Set<Element> edits1 = new TreeSet<>(new EditSorter());
	    edits1.addAll(edits);
	    for (Element ed : edits1) {
	       addTargets(ed);
	     }
	    for (Element ed : edits1) {
	       applyEdit(ed);
	     }
	  }
	 else applyEdit(xml);
       }
      finally {
	 bh.endEditAction(tc);
       }
    }
}


private void extractEdits(Element xml,Collection<Element> edits)
{
   String typ = IvyXml.getAttrString(xml,"TYPE");
   if (typ == null) return;

   for (Element ed : IvyXml.children(xml,"EDIT")) {
      extractEdits(ed,edits);
    }

   switch (typ) {
      case "MULTI" :
      case "RANGEMARKER" :
      case "CHANGE" :
      case "EDIT" :
	 break;
      default :
	 edits.add(xml);
	 break;
   }
}



private void applyResourceEdits(Element xml)
{
   if (IvyXml.isElement(xml,"EDITS") || IvyXml.isElement(xml,"RESULT")) {
      for (Element c : IvyXml.children(xml)) applyResourceEdits(c);
    }
   if (IvyXml.isElement(xml,"CHANGE")) {
      String typ = IvyXml.getAttrString(xml,"TYPE");
      if (typ == null) return;
      else if (typ.equals("COMPOSITE")) {
	 for (Element c : IvyXml.children(xml)) applyResourceEdits(c);
       }
      else if (typ.equals("RENAMERESOURCE")) {
	 Element r = IvyXml.getChild(xml,"RESOURCE");
	 if (r != null) {
	    BumpClient bc = BumpClient.getBump();
	    bc.saveAll();
	    String newname = IvyXml.getAttrString(xml,"NEWNAME");
	    String proj = IvyXml.getAttrString(r,"PROJECT");
	    File fil = new File(IvyXml.getAttrString(r,"LOCATION"));
	    BaleDocumentIde bde = BaleFactory.getFactory().getDocument(proj,fil);
	    bde.flushEdits();
	    // bc.saveFile(proj,fil);
	    bc.renameResource(proj,fil,newname);
	  }
       }
    }
}


static int getEditSize(Element xml)
{
   int ct = 0;
   if (xml == null) return 0;

   if (IvyXml.isElement(xml,"EDITS") || IvyXml.isElement(xml,"RESULT")) {
      for (Element c : IvyXml.children(xml)) ct += getEditSize(c);
    }
   else if (IvyXml.isElement(xml,"CHANGE")) {
      String typ = IvyXml.getAttrString(xml,"TYPE");
      if (typ == null) return 0;
      else if (typ.equals("COMPOSITE")) {
	 for (Element c : IvyXml.children(xml)) ct += getEditSize(c);
       }
      else if (typ.equals("EDIT")) {
	 for (Element ed : IvyXml.children(xml,"EDIT")) {
	    ct += getEditSize(ed);
	  }
       }
    }
   else if (IvyXml.isElement(xml,"EDIT")) {
      String typ = IvyXml.getAttrString(xml,"TYPE");
      if (typ == null) return 0;
      if (typ.equals("MULTI")) {
	 for (Element ed : IvyXml.children(xml,"EDIT")) {
	    ct += getEditSize(ed);
	  }
       }
      else {
	 String txt = IvyXml.getTextElement(xml,"TEXT");
	 if (txt != null) ct += txt.length();
	 ct += IvyXml.getAttrInt(xml,"LENGTH");
       }
    }
   return ct;
}



/********************************************************************************/
/*										*/
/*	Sorter to do edits from bottom to top					*/
/*										*/
/********************************************************************************/

private static final class EditSorter implements Comparator<Element> {

   @Override public int compare(Element e1,Element e2) {
      int off1 = IvyXml.getAttrInt(e1,"OFFSET");
      int off2 = IvyXml.getAttrInt(e2,"OFFSET");
      if (off2 != off1) return off2-off1;

      int ct1 = IvyXml.getAttrInt(e1,"COUNTER");
      int ct2 = IvyXml.getAttrInt(e2,"COUNTER");
      return ct2-ct1;
    }

}	// end of inner class EditSorter




/********************************************************************************/
/*										*/
/*	Method to do an actual edit						*/
/*										*/
/********************************************************************************/

private void applyEdit(Element ed)
{
   if (for_document == null) return;

   int off = IvyXml.getAttrInt(ed,"OFFSET");
   int len = IvyXml.getAttrInt(ed,"LENGTH");
   int eoff = off+len;
   int off1 = for_document.mapOffsetToJava(off);
   int off2 = for_document.mapOffsetToJava(eoff);
   int len1 = off2-off1;

   String typ = IvyXml.getAttrString(ed,"TYPE");

   switch (typ) {
      case "INSERT" :
      case "REPLACE" :
      case "DELETE" :
	 for_document.nextEditCounter();
	 String txt = IvyXml.getTextElement(ed,"TEXT");
	 try {
	    for_document.replace(off1,len1,txt,null);
	  }
	 catch (BadLocationException e) {
	    BoardLog.logE("BALE","Problem applying Eclipse text edit",e);
	  } 
	 break;
      case "MOVETARGET" :
	 break;
      case "MOVESOURCE" :
	 try {
	    String mtxt = for_document.getText(off1,len1);
	    Position tgt = move_targets.get(IvyXml.getAttrString(ed,"TARGET"));
	    int tgt1 = for_document.mapOffsetToJava(tgt.getOffset());
	    for_document.replace(tgt1,0,mtxt,null);
	    for_document.replace(off1,len1,null,null);
	    String rslt = for_document.getText(0,for_document.getLength());
	    System.err.println("EDIT RESULT: " + off1 + " " + len1 + " " + tgt1 + " " +
		  mtxt + " " + rslt);
	  }
	 catch (BadLocationException e) {
	    BoardLog.logE("BALE","Problem applying Eclipse text edit",e);
	  }
	 break;
      case "COPYSOURCE" :
	 break;
      case "COPYTARGET" :
         String tgt = source_strings.get(IvyXml.getAttrString(ed,"SOURCE"));
         try {
            for_document.replace(off1,len1,tgt,null);
          }
         catch (BadLocationException e) {
            BoardLog.logE("BALE","Problem applying Eclipse text edit",e);
          }
	 break;
      case "MULTI" :
      case "RANGEMARKER" :
      default :
	 for (Element ce : IvyXml.children(ed,"EDIT")) {
	    applyEdit(ce);
	  }
	 break;
    }
}


private void addTargets(Element xml)
{
   switch (IvyXml.getAttrString(xml,"TYPE")) {
      case "INSERT" :
      case "REPLACE" :
      case "DELETE" :
	 break;
      case "MULTI" :
      default :
	 for (Element ce : IvyXml.children(xml,"EDIT")) {
	    addTargets(ce);
	  }
	 break;
      case "MOVETARGET" :
	 if (move_targets == null) move_targets = new HashMap<>();
	 int off = IvyXml.getAttrInt(xml,"OFFSET");
	 try {
	    Position pos = for_document.createPosition(off);
	    move_targets.put(IvyXml.getAttrString(xml,"ID"),pos);
	  }
	 catch (BadLocationException e) {
	    BoardLog.logE("BALE","Problem applying Eclipse text edit",e);
	  } 
	 break;
      case "MOVESOURCE" :
      case "COPYSOURCE" :
         if (source_strings == null) source_strings = new HashMap<>();
         int off0 = IvyXml.getAttrInt(xml,"OFFSET");
         int off1 = for_document.mapOffsetToJava(off0);
         int len = IvyXml.getAttrInt(xml,"LENGTH");
         int off2 = for_document.mapOffsetToJava(off0+len);
         try {
            String s = for_document.getText(off1,off2-off1);
            source_strings.put(IvyXml.getAttrString(xml,"ID"),s);
          }
         catch (BadLocationException e) {
            BoardLog.logE("BALE","Problem applying Eclipse text edit",e);
          }
         break;
    }
}




}	// end of class BaleApplyEdits




/* end of BaleApplyEdits.java */
