/********************************************************************************/
/*										*/
/*		BddtConfigurator.java						*/
/*										*/
/*	Bubbles Environment Dynamic Debugger Tool bubble load configurator	*/
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



package edu.brown.cs.bubbles.bddt;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;



class BddtConfigurator implements BddtConstants, BudaConstants.BubbleConfigurator, BumpConstants
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

   if (typ != null && typ.equals("CONFIGS")) {
      // bb = BeamConfigView.createBubble();
    }
   else if (typ != null && typ.equals("BREAKPOINTBUBBLE")) {
      bb = new BddtBreakpointBubble();
    }
   else if (typ != null && typ.equals("LAUNCHBUBBLE")) {
      String cnm = IvyXml.getAttrString(cnt,"CONFIG");
      BumpClient bc = BumpClient.getBump();
      for (BumpLaunchConfig blc : bc.getRunModel().getLaunchConfigurations()) {
         if (blc.getConfigName().equals(cnm)) {
            bb = new BddtLaunchBubble(blc);
            break;
          }
       }
    }

   // TODO: handle threads bubbles, process bubble, console bubbles


   return bb;
}


@Override public boolean matchBubble(BudaBubble bb,Element xml)
{
   Element cnt = IvyXml.getChild(xml,"CONTENT");
   String typ = IvyXml.getAttrString(cnt,"TYPE");
   if (typ == null) return false;
   else if (typ.equals("CONFIGS")) ;
   else if (typ.equals("BREAKPOINTBUBBLE")) {
      if (bb instanceof BddtBreakpointBubble) return true;
    }
   else if (typ.equals("LAUNCHBUBBLE")) {
      if (bb instanceof BddtLaunchBubble) {
         BddtLaunchBubble lb = (BddtLaunchBubble) bb;
         String cnm = IvyXml.getAttrString(cnt,"CONFIG");
         if (lb.getLaunchConfig().getConfigName().equals(cnm)) return true;
       }
    }
   return false;
}
   



/********************************************************************************/
/*										*/
/*	I/O methods								*/
/*										*/
/********************************************************************************/

@Override public void outputXml(BudaXmlWriter xw,boolean history)
{
   if (!history) {
      BumpLaunchConfig blc = BddtFactory.getFactory().getCurrentLaunchConfig();
      if (blc != null) {
	 xw.begin("BDDT");
	 xw.field("CONFIG",blc.getConfigName());
	 xw.end("BDDT");
       }
    }
}




@Override public void loadXml(BudaBubbleArea bba,Element root)
{
   Element de = IvyXml.getChild(root,"BDDT");
   if (de != null) {
      String cnm = IvyXml.getAttrString(de,"CONFIG");
      BumpRunModel rm = BumpClient.getBump().getRunModel();
      for (BumpLaunchConfig blc : rm.getLaunchConfigurations()) {
	 if (!blc.isWorkingCopy() && blc.getConfigName().equals(cnm)) {
	    BddtFactory.getFactory().setCurrentLaunchConfig(blc);
	    break;
	 }
      }
   }
}




}	// end of class BddtConfigurator




/* end of BddtConfigurator.java */


