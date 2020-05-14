/********************************************************************************/
/*										*/
/*		BconConfigurator.java						*/
/*										*/
/*	Bubbles Environment Context Viewer configurator for loading bubbles	*/
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


/* SVN: $Id$ */



package edu.brown.cs.bubbles.bcon;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.awt.Point;
import java.io.File;
import java.util.List;


class BconConfigurator implements BconConstants, BudaConstants.BubbleConfigurator
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

   if (typ.equals("OVERVIEW")) {
      Element ept = IvyXml.getChild(cnt,"POINT");
      Point p = new Point((int) IvyXml.getAttrDouble(ept,"X",0),(int) IvyXml.getAttrDouble(ept,"Y",0));
      bb = BconFactory.getFactory().createOverviewBubble(bba,p);
    }
   else if (typ.equals("CLASS")) {
      BumpClient bc = BumpClient.getBump();
      String proj = IvyXml.getAttrString(cnt,"PROJECT");
      String cls = IvyXml.getAttrString(cnt,"CLASS");
      String fnm = IvyXml.getAttrString(cnt,"FILE");
      boolean ifg = IvyXml.getAttrBool(cnt,"INNER");
      List<BumpLocation> locs = bc.findClassDefinition(proj, cls);
      if (locs == null || locs.size() == 0) return null;
      
      bb = BconFactory.getFactory().createClassBubble(bba,proj,new File(fnm),cls,ifg);
    }
   else if (typ.equals("PACKAGE")) {
      String proj = IvyXml.getAttrString(cnt,"PROJECT"); 
      String pkg = IvyXml.getAttrString(cnt,"PACKAGE");
      bb = BconFactory.getFactory().createPackageBubble(bba,proj,pkg);
   }

   return bb;
}


@Override public boolean matchBubble(BudaBubble bb,Element xml)
{
   Element cnt = IvyXml.getChild(xml,"CONTENT");
   String typ = IvyXml.getAttrString(cnt,"TYPE");
   
   if (typ.equals("OVERVIEW") && (bb.getContentPane() instanceof BconBubble)) {
      BconBubble cb = (BconBubble) bb;
      if (cb.getPanel() instanceof BconOverviewPanel)
         return true;
    }
   else if (typ.equals("CLASS")) {
      BconBubble cb = (BconBubble) bb;
      if (cb.getPanel() instanceof BconClassPanel)
         return true;
    }
   else if (typ.equals("PACKAGE")) {
      BconBubble cb = (BconBubble) bb;
      if (cb.getPanel() instanceof BconPackagePanel)
         return true;
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




}	// end of class BconConfigurator




/* end of BconConfigurator.java */


