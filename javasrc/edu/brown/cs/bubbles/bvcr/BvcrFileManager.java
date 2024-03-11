/********************************************************************************/
/*										*/
/*		BvcrFileManager.java						*/
/*										*/
/*	Handle file updates and annotations					*/
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



package edu.brown.cs.bubbles.bvcr;

import edu.brown.cs.bubbles.bale.BaleConstants.BaleAnnotation;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleFileOverview;
import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;

import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


class BvcrFileManager implements BvcrConstants, BumpConstants.BumpChangeHandler
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Set<File>	active_files;
private DifferenceMap	difference_map;
private List<BvcrAnnotation> active_annots;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BvcrFileManager()
{
   active_files = new HashSet<File>();
   difference_map = new DifferenceMap();
   active_annots = new ArrayList<BvcrAnnotation>();
   BumpClient.getBump().addChangeHandler(this);
}



/********************************************************************************/
/*										*/
/*	File handling methods							*/
/*										*/
/********************************************************************************/

@Override public void handleFileStarted(String proj,String file)
{
   File f = new File(file);
   if (!active_files.add(f)) return;
   Updater upd = new Updater(proj,f);
   BoardThreadPool.start(upd);
}





/********************************************************************************/
/*										*/
/*	Methods for setting up annotations					*/
/*										*/
/********************************************************************************/

private void setupAnnotations(String proj,File f)
{
   BaleFactory bf = BaleFactory.getFactory();
   for (Iterator<BvcrAnnotation> it = active_annots.iterator(); it.hasNext(); ) {
      BvcrAnnotation ba = it.next();
      if (ba.getFile().equals(f)) {
	 it.remove();
	 bf.removeAnnotation(ba);
       }
    }

   Map<String,BvcrDifferenceFile> diffs = difference_map.get(f);
   if (diffs == null) return;
   BitSet lineset = new BitSet();
   for (BvcrDifferenceFile bdf : diffs.values()) {
      for (BvcrFileChange bfc : bdf.getChanges()) {
	 int ln = bfc.getSourceLine();
	 if (ln <= 0) continue;
	 String [] dels = bfc.getDeletedLines();
	 int ct = (dels == null ? 1 : dels.length);
	 if (ct == 0) ct = 1;
	 for (int i = 0; i < ct; ++i) {
	    lineset.set(ln+i);
	  }
       }
    }

   for (int ln = lineset.nextSetBit(0); ln >= 0; ln = lineset.nextSetBit(ln+1)) {
      try {
	 BvcrAnnotation annot = new BvcrAnnotation(proj,f,ln);
	 active_annots.add(annot);
	 bf.addAnnotation(annot);
       }
      catch (BadLocationException e) {
	 BoardLog.logD("BVCR","Problem setting up annotation: " + e);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Tool tip methods							*/
/*										*/
/********************************************************************************/

private String computeToolTipText(String proj,File f,int lno)
{
   Map<String,BvcrDifferenceFile> diffs = difference_map.get(f);
   if (diffs == null) return null;

   StringBuffer buf = new StringBuffer();

   for (Map.Entry<String,BvcrDifferenceFile> ent : diffs.entrySet()) {
      String user = ent.getKey();
      BvcrDifferenceFile bdf = ent.getValue();

      for (BvcrFileChange bfc : bdf.getChanges()) {
	 int ln = bfc.getSourceLine();
	 String [] dels = bfc.getDeletedLines();
	 int ct = (dels == null ? 1 : dels.length);
	 if (ct == 0) ct = 1;
	 if (lno >= ln && lno < lno+ct) {
	    if (buf.length() > 0) buf.append("<br>");
	    buf.append("Change by user " + user);
	    String [] ins = bfc.getAddedLines();
	    if (dels != null && ins != null) {
	       buf.append(" Replaced " + dels.length + " with " + ins.length);
	     }
	    else if (dels != null) {
	       buf.append(" Deleted " + dels.length);
	     }
	    else if (ins != null) {
	       buf.append(" Inserted " + ins.length);
	     }
	  }
       }
    }

   if (buf.length() > 0) return buf.toString();

   return null;
}



/********************************************************************************/
/*										*/
/*	Handle getting update information on a file				*/
/*										*/
/********************************************************************************/

private class Updater implements Runnable {

   private String for_project;
   private File for_file;
   private boolean annots_ready;

   Updater(String proj,File f) {
      for_project = proj;
      for_file = f;
      annots_ready = false;
    }

   @Override public void run() {
      if (!annots_ready) {
         Element e = BvcrFactory.getFactory().getChangesForFile(for_project,for_file.getPath());
         if (e == null) return;
         int ctr = 0;
         Element cs = IvyXml.getChild(e,"CHANGESET");
         for (Element uc : IvyXml.children(cs,"USERCHANGE")) {
            String unm = IvyXml.getAttrString(uc,"USER");
            BvcrDifferenceFile bdf = new BvcrDifferenceFile(uc);
            difference_map.add(for_file,unm,bdf);
            ++ctr;
          }
         if (ctr > 0) {
            annots_ready = true;
            SwingUtilities.invokeLater(this);
          }
       }
      else {
         setupAnnotations(for_project,for_file);
       }
    }

}	// end of inner class Updater



/********************************************************************************/
/*										*/
/*	Holder of all difference						*/
/*										*/
/********************************************************************************/

private class DifferenceMap extends HashMap<File,Map<String,BvcrDifferenceFile>> {

   private static final long serialVersionUID = 1;
   
   DifferenceMap() { }

   void add(File file,String user,BvcrDifferenceFile dif) {
      if (dif == null) return;
      Map<String,BvcrDifferenceFile> m1 = get(file);
      if (m1 == null) {
         m1 = new HashMap<String,BvcrDifferenceFile>();
         put(file,m1);
       }
      m1.put(user,dif);
    }

}	// end of inner class DifferenceMap




/********************************************************************************/
/*										*/
/*	Annotation definition							*/
/*										*/
/********************************************************************************/

private class BvcrAnnotation implements BaleAnnotation {

   private String for_project;
   private File for_file;
   private int line_number;
   private Position file_pos;
   

   BvcrAnnotation(String proj,File f,int line) throws BadLocationException {
      for_project = proj;
      for_file = f;
      line_number = line;
      BaleFileOverview bfo = BaleFactory.getFactory().getFileOverview(proj,f);
      int off = bfo.findLineOffset(line);
      file_pos = bfo.createPosition(off);
    }

   @Override public File getFile()		        { return for_file; }
   @Override public int getDocumentOffset()	        { return file_pos.getOffset(); }
   @Override public Icon getIcon(BudaBubble b)          { return null; }
   @Override public Color getLineColor(BudaBubble b)	{ return null; }
   @Override public int getPriority()		        { return 5; }


   @Override public boolean getForceVisible(BudaBubble bb)	{ return false; }
   @Override public void addPopupButtons(Component c,JPopupMenu m) { }

   @Override public Color getBackgroundColor() {
      // compute background color based on user
      return BoardColors.getColor("Bvcr.AnnotationColor");
    }

   @Override public String getToolTip() {
      return computeToolTipText(for_project,for_file,line_number);
    }

}	// end of inner class BvcrAnnotation



}	// end of class BvcrFileManager




/* end of BvcrFileManager.java */

