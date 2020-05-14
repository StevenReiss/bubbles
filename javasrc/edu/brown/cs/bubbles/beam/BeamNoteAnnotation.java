/********************************************************************************/
/*										*/
/*		BeamNoteAnnotation.java 					*/
/*										*/
/*	Bubble Environment Auxilliary & Missing items note annotation		*/
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


/* SVN: $Id$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.beam;

import edu.brown.cs.bubbles.bale.BaleConstants;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleFileOverview;
import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaBubbleLink;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaDefaultPort;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;

import edu.brown.cs.ivy.limbo.LimboFactory;
import edu.brown.cs.ivy.limbo.LimboLine;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.File;


class BeamNoteAnnotation implements BaleConstants.BaleAnnotation {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private File for_file;
private String note_name;
private Position note_position;


private static Color annot_color;
private static Icon  note_icon = null;


static {
   annot_color = BoardColors.getColor("Beam.NoteAnnotationColor");
   annot_color = null;
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BeamNoteAnnotation(File file,BaleFileOverview doc,int off)
{
   for_file = file;
   note_name = null;
   note_position = null;

   try {
      note_position = doc.createPosition(off);
    }
   catch (BadLocationException e) { }
}



BeamNoteAnnotation(Element xml)
{
   for_file = new File(IvyXml.getAttrString(xml,"FILE"));
   note_name = IvyXml.getAttrString(xml,"NAME");
   note_position = null;

   Element lim = IvyXml.getChild(xml,"LIMBO");
   if (lim != null) {
      LimboLine ll = LimboFactory.createFromXml(lim);
      ll.revalidate();
      BaleFileOverview bfo = BaleFactory.getFactory().getFileOverview(null,for_file);
      if (bfo != null) {
	 int lst = bfo.findLineOffset(ll.getLine());
	 try {
	    note_position = bfo.createPosition(lst);
	  }
	 catch (BadLocationException e) { }
       }
    }
}




/********************************************************************************/
/*										*/
/*	Bale annotation access methods						*/
/*										*/
/********************************************************************************/

@Override public File getFile() 			{ return for_file; }

@Override public int getDocumentOffset() {
   if (note_position == null) return -1;
   return note_position.getOffset();
}

@Override public synchronized Icon getIcon(BudaBubble b)
{
   if (note_icon == null) {
      note_icon = BoardImage.getIcon("note");
    }
   return note_icon;
}

@Override public String getToolTip()			{ return "Note Bubble"; }

@Override public Color getLineColor(BudaBubble b)	{ return annot_color; }
@Override public Color getBackgroundColor()		{ return null; }

@Override public boolean getForceVisible(BudaBubble bbl) {
   return false;
}

@Override public int getPriority()			{ return 2; }

@Override public void addPopupButtons(Component c,JPopupMenu m)
{
   m.add(new NoteAction());
   m.add(new RemoveNoteAction());
}



/********************************************************************************/
/*										*/
/*	Local access methods						*/
/*										*/
/********************************************************************************/

void setAnnotationFile(String name)
{
   note_name = name;
}




/********************************************************************************/
/*										*/
/*	Methods to save permanently						*/
/*										*/
/********************************************************************************/

void saveAnnotation(BudaXmlWriter xw)
{
   if (for_file == null) return;
   BaleFileOverview bfo = BaleFactory.getFactory().getFileOverview(null,for_file);
   int lno = bfo.findLineNumber(note_position.getOffset());
   int lst = bfo.findLineOffset(lno);
   LimboLine ll = LimboFactory.createLine(for_file,lno);

   xw.begin("ANNOT");
   xw.field("FILE",for_file);
   xw.field("NAME",note_name);
   xw.field("LINE",lno);
   xw.field("OFFSET",lst);
   ll.writeXml(xw);
   xw.end("ANNOT");
}



/********************************************************************************/
/*										*/
/*	Action associated with note annotations 				*/
/*										*/
/********************************************************************************/

private class NoteAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   NoteAction() {
      super("Show associated note");
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (note_name == null) return;
      BeamNoteBubble nb = new BeamNoteBubble(note_name,null,BeamNoteAnnotation.this);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea((Component) e.getSource());
      BudaBubble eb = BudaRoot.findBudaBubble((Component) e.getSource());
      Rectangle r1 = BudaRoot.findBudaLocation((Component) e.getSource());
      if (r1 == null || bba == null || eb == null) return;
      Rectangle r2 = nb.getBounds();
      int x = r1.x - r2.width - 25;
      int y = r1.y;
      bba.addBubble(nb,x,y);
      BaleFileOverview bfo = BaleFactory.getFactory().getFileOverview(null,for_file);
      int lno = bfo.findLineNumber(note_position.getOffset());
      BudaConstants.LinkPort p1 = BaleFactory.getFactory().findPortForLine(eb,lno);
      BudaConstants.LinkPort p2 = new BudaDefaultPort(BudaConstants.BudaPortPosition.BORDER_EW_TOP,true);
      BudaBubbleLink lnk = new BudaBubbleLink(eb,p1,nb,p2);
      bba.addLink(lnk);
    }

}	// end of inner class NoteAction




private class RemoveNoteAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   RemoveNoteAction() {
      super("Remove note annotation");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea((Component) e.getSource());
      if (bba == null) return;
      for (BudaBubble bb : bba.getBubbles()) {
	 if (bb instanceof BeamNoteBubble) {
	    BeamNoteBubble bnb = (BeamNoteBubble) bb;
	    bnb.clearAnnotation(BeamNoteAnnotation.this);
	  }
       }
      BaleFactory.getFactory().removeAnnotation(BeamNoteAnnotation.this);
    }

}	// end of inner class RemoveNoteAction



}	// end of class BeamNoteAnnotation




/* end of BeamNoteAnnotation.java */
