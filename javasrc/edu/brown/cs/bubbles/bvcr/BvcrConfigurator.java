/********************************************************************************/
/*                                                                              */
/*              BvcrConfigurator.java                                           */
/*                                                                              */
/*      Configurator to restore BVCR bubbles at startup                         */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
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



package edu.brown.cs.bubbles.bvcr;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

class BvcrConfigurator implements BvcrConstants, BudaConstants.BubbleConfigurator
{


@Override public BudaBubble createBubble(BudaBubbleArea bba,Element xml) 
{
   Element cnt = IvyXml.getChild(xml,"CONTENT");
   String typ = IvyXml.getAttrString(cnt,"TYPE");
   String proj = IvyXml.getAttrString(cnt,"PROJECT");
   BvcrControlPanel cp = BvcrFactory.getFactory().getControlPanel(proj);
   if (cp == null) return null;
   
   BudaBubble bb = null;
   switch (typ) {
      case "CONTROL" :
         bb = cp.getControlBubble();
         break;
      case "FILECONTROL" :
         bb = new BvcrControlFilePanel(cp);
         break;
      case "VERSIONCONTROL" :
         bb = new BvcrControlVersionPanel(cp);
         break;
      case "VERSIONGRAPH" :
         bb = new BvcrControlGraphPanel(cp);
         break;
    }
   
   return bb;
}


@Override public boolean matchBubble(BudaBubble bb,Element xml)
{
   return false;
}


@Override public void outputXml(BudaXmlWriter xw,boolean history) 
{
   
}


@Override public void loadXml(BudaBubbleArea bba,Element root) 
{
   
}


}       // end of class BvcrConfigurator




/* end of BvcrConfigurator.java */

