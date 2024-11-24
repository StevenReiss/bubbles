/********************************************************************************/
/*                                                                              */
/*              BgtaConfigurator.java                                           */
/*                                                                              */
/*      Bubbles attribute and property management main setup routine            */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2009 Brown University -- Ian Strickman                      */
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


package edu.brown.cs.bubbles.bgta;



import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants.BubbleConfigurator;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;




class BgtaConfigurator implements BubbleConfigurator {


/********************************************************************************/
/*                                                                              */
/*      Bubble creation methods                                                 */
/*                                                                              */
/********************************************************************************/

@Override public BudaBubble createBubble(BudaBubbleArea bba,Element xml)
{
   Element cnt = IvyXml.getChild(xml,"CONTENT");
   String typ = IvyXml.getAttrString(cnt,"TYPE");

   BudaBubble bb = null;
   if (typ == null) return bb;

   if (typ.equals("CHAT")) {
      String friend = IvyXml.getAttrString(cnt,"NAME");
      String username = IvyXml.getAttrString(cnt,"USERNAME");
      String password = IvyXml.getAttrString(cnt,"PASSWORD");
      String server = IvyXml.getAttrString(cnt,"SERVER");
      BgtaFactory bf = BgtaFactory.getFactory();
      bb = bf.createChatBubble(friend, username, password, server);
    }
   return bb;
}


@Override public boolean matchBubble(BudaBubble bb,Element xml)
{
   Element cnt = IvyXml.getChild(xml,"CONTENT");
   String typ = IvyXml.getAttrString(cnt,"TYPE");
   if (typ.equals("CHAT") && bb instanceof BgtaBubble) {
      String frnd = IvyXml.getAttrString(cnt,"NAME");
      BgtaBubble gb = (BgtaBubble) bb;
      if (gb.getUsername().equals(frnd)) return true;
    }
   return false;
}
   


@Override public void loadXml(BudaBubbleArea bba,Element root)          { }

@Override public void outputXml(BudaXmlWriter xw,boolean history)       { }




}       // end of class BgtaConfigurator




/* end of BgtaConfigurator.java */
