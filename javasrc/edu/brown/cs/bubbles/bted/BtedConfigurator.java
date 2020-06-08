/********************************************************************************/
/*										*/
/*		BtedConfigurator.java						*/
/*										*/
/*	Bubble Environment text editor facility bubble configurator		*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Adam M. Cook 			*/
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


package edu.brown.cs.bubbles.bted;

import edu.brown.cs.bubbles.bted.BtedConstants.StartMode;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;


class BtedConfigurator implements BudaConstants.BubbleConfigurator {



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

   if (typ.equals("EDITORBUBBLE")) {
      String path = IvyXml.getAttrString(cnt,"PATH");
      StartMode mode = IvyXml.getAttrEnum(cnt,"MODE",StartMode.LOCAL);
      bb = new BtedBubble(path,mode);
   }

   return bb;
}

@Override public boolean matchBubble(BudaBubble bb,Element xml) 
{
   Element cnt = IvyXml.getChild(xml,"CONTENT");
   String typ = IvyXml.getAttrString(cnt,"TYPE");
   if (typ.equals("EDITORBUBBLE") && bb instanceof BtedBubble) {
      String path = IvyXml.getAttrString(cnt,"PATH");
      String bpath = ((BtedBubble) bb).getFile().getPath();
      if (path.equals(bpath)) return true;
    }
   return false;
}




/********************************************************************************/
/*										*/
/*	I/O methods								*/
/*										*/
/********************************************************************************/

@Override public void loadXml(BudaBubbleArea bba,Element root)
{}

@Override public void outputXml(BudaXmlWriter xw,boolean history)
{}



}	// end of class BtedConfigurator

/* end of BtedConfigurator.java */
