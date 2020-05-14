/********************************************************************************/
/*										*/
/*		BhelpDemo.java							*/
/*										*/
/*	Demonstration root class for bubbles help demonstrations		*/
/*										*/
/********************************************************************************/
/*	Copyright 2012 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2012, Brown University, Providence, RI.				 *
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



package edu.brown.cs.bubbles.bhelp;

import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.buda.*;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;



class BhelpDemo implements BhelpConstants, BudaConstants.BudaDemonstration
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		demo_name;
private List<BhelpAction> help_actions;
private boolean 	demo_stopped;
private BhelpContext	demo_context;
private boolean 	allow_hovers;

private static Boolean	doing_demo = Boolean.FALSE;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BhelpDemo(Element xml)
{
   demo_name = IvyXml.getAttrString(xml,"NAME");
   demo_stopped = false;
   demo_context = null;
   allow_hovers = IvyXml.getAttrBool(xml,"HOVERS");

   help_actions = new ArrayList<BhelpAction>();
   for (Element ea : IvyXml.children(xml,"ACTION")) {
      List<BhelpAction> act = BhelpAction.createAction(ea);
      if (act != null) help_actions.addAll(act);
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getName()			{ return demo_name; }


@Override public void stopDemonstration()
{
   synchronized (doing_demo) {
      if (doing_demo && !demo_stopped) {
	 BoardLog.logD("BHELP","STOPPING DEMO");
	 demo_stopped = true;
	 demo_context.setStopped();
       }
    }
}



/********************************************************************************/
/*										*/
/*	Execution methods							*/
/*										*/
/********************************************************************************/

void executeDemo(BudaBubbleArea bba,boolean silent)
{
    demo_context = new BhelpContext(bba,this);
    demo_stopped = false;

    synchronized (doing_demo) {
       if (doing_demo) return;		// can't do more than one
       doing_demo = true;
     }
    BudaRoot br = demo_context.getBudaRoot();
    br.setVisible(true);
    br.toFront();

    DemoRun dr = new DemoRun(demo_context,silent);
    BoardMetrics.noteCommand("BHELP", "ShowDemo_" + demo_name);
    BoardThreadPool.start(dr);
}



private class DemoRun implements Runnable {

   private BhelpContext using_context;
   private boolean is_silent;

   DemoRun(BhelpContext ctx,boolean silent) {
      using_context = ctx;
      is_silent = silent;
   }

   @Override public void run() {
      BudaRoot br = using_context.getBudaRoot();
      br.setVisible(true);
      br.setDemonstration(BhelpDemo.this,"How-To Demonstration");
      if (!allow_hovers) BudaHover.enableHovers(false);
      
      try {
         for (BhelpAction ba : help_actions) {
            try {
               using_context.checkMouse();
               if (demo_stopped) ba.executeStopped(using_context);
               else {
                  if (!(is_silent && ba.getEquivalentPause() > 0)) {
                     ba.executeAction(using_context);
                   }
                  else {
                     ba = BhelpAction.speechToPause(ba);
                     ba.executeAction(using_context);
                   }
                }
             }
            catch (BhelpException e) {
               if (!demo_stopped) BoardLog.logE("BHELP","Demonstration problem",e);
               demo_stopped = true;
             }
          }
       }
      finally {
         br.setDemonstration(null,null);
         if (!allow_hovers) BudaHover.enableHovers(true);
         synchronized (doing_demo) {
            doing_demo = false;
            demo_context = null;
            demo_stopped = false;
          }
       }
   }

}	// end of inner class DemoRun



}	// end of class BhelpDemo




/* end of BhelpDemo.java */
