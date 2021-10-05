/********************************************************************************/
/*										*/
/*		BeamConfigurator.java						*/
/*										*/
/*	Bubble Environment Auxilliary & Missing items bubble configurator	*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.beam;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;

import edu.brown.cs.ivy.xml.IvyXml;

import java.awt.Color;

import org.w3c.dom.Element;



class BeamConfigurator implements BeamConstants, BudaConstants.BubbleConfigurator
{



/********************************************************************************/
/*										*/
/*	Bubble creation methods 						*/
/*										*/
/********************************************************************************/

@Override public BudaBubble createBubble(BudaBubbleArea bba,Element xml)
{
   Element cnt = IvyXml.getChild(xml,"CONTENT");
   String typ = IvyXml.getAttrString(cnt,"TYPE");

   BudaBubble bb = null;

   if (typ.equals("NOTE")) {
      String cnts = IvyXml.getTextElement(cnt,"TEXT");
      String name = IvyXml.getAttrString(cnt,"NAME");
      BeamNoteAnnotation annot = null;
      Element anx = IvyXml.getChild(cnt,"ANNOT");
      if (anx != null) {
	 annot = new BeamNoteAnnotation(anx);
	 if (annot.getDocumentOffset() < 0) annot = null;
       }
      BeamNoteBubble nbb = new BeamNoteBubble(name,cnts,annot);
      bb = nbb;
      Color tc = IvyXml.getAttrColor(cnt,"TOPCOLOR");
      Color bc = IvyXml.getAttrColor(cnt,"BOTTOMCOLOR");
      if (tc != null && bc != null) nbb.setNoteColor(tc,bc);
    }
   else if (typ.equals("FLAG")) {
      String path = IvyXml.getTextElement(cnt,"IMGPATH");
      bb = new BeamFlagBubble(path);
    }
   else if (typ.equals("PROBLEMS")) {
      String typs = IvyXml.getAttrString(cnt,"ERRORTYPES");
      boolean tasks = IvyXml.getAttrBool(cnt,"FORTASKS");
      bb = new BeamProblemBubble(typs,tasks);
    }

   return bb;
}

@Override public boolean matchBubble(BudaBubble bb,Element xml)
{
   Element cnt = IvyXml.getChild(xml,"CONTENT");
   String typ = IvyXml.getAttrString(cnt,"TYPE");
   if (typ.equals("NOTE") && bb instanceof BeamNoteBubble) {
      BeamNoteBubble nb = (BeamNoteBubble) bb;
      String name = IvyXml.getAttrString(cnt,"NAME");
      String bname = nb.getNoteFile().getName();
      if (name.equals(bname)) return true;
    }
   else if (typ.equals("FLAG") && bb instanceof BeamFlagBubble) {
      BeamFlagBubble fb = (BeamFlagBubble) bb;
      String path = IvyXml.getTextElement(cnt,"IMGPATH");
      String bpath = fb.getImagePath();
      if (path.equals(bpath)) return true;
    }
   else if (typ.equals("PROBLEMS") && bb instanceof BeamProblemBubble) {
      String typs = IvyXml.getAttrString(cnt,"ERRORTYPES");
      boolean tasks = IvyXml.getAttrBool(cnt,"FORTASKS");
      BeamProblemBubble pb = (BeamProblemBubble) bb;
      if (pb.matchTypes(typs,tasks)) return true;
    }
      
   return false;
}




/********************************************************************************/
/*										*/
/*	I/O methods								*/
/*										*/
/********************************************************************************/

@Override public void outputXml(BudaXmlWriter xw,boolean history)	{ }
@Override public void loadXml(BudaBubbleArea bba,Element root)		{ }




}	// end of class BeamConfigurator




/* end of BeamConfigurator.java */
