/********************************************************************************/
/*										*/
/*		BdynFactory.java						*/
/*										*/
/*	Factory for Bubbles DYNamic views					*/
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



package edu.brown.cs.bubbles.bdyn;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.awt.Color;
import java.awt.Point;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class BdynFactory implements BdynConstants, BumpConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<BumpProcess,BdynProcess>	process_map;
private BdynCallbacks			callback_set;
private static OptionSet		bdyn_options = new OptionSet();


private static BdynFactory	the_factory = new BdynFactory();




/********************************************************************************/
/*										*/
/*	Setup Methods								*/
/*										*/
/********************************************************************************/

public static void setup()
{
}



public static void initialize(BudaRoot br)
{
   BudaRoot.registerMenuButton("Bubble.Show Task Visualization",new TaskAction());

   getFactory().callback_set.setup();

   BumpClient.getBump().getTrieData(null);              // ensure we get trie data
}

/**
 *	Return the singleton instance of the factory
 **/

public static BdynFactory getFactory()
{
   return the_factory;
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BdynFactory()
{
   process_map = new HashMap<BumpProcess,BdynProcess>();
   callback_set = new BdynCallbacks();
   ProcessHandler ph = new ProcessHandler();
   BumpClient.getBump().getRunModel().addRunEventHandler(ph);
}



/********************************************************************************/
/*										*/
/*	Handle new processes							*/
/*										*/
/********************************************************************************/

private void setupProcess(BumpRunEvent evt)
{
   BdynProcess bp = new BdynProcess(evt.getProcess());
   process_map.put(evt.getProcess(),bp);
}


BdynProcess getBdynProcess(BumpProcess bp)
{
   return process_map.get(bp);
}



/********************************************************************************/
/*										*/
/*	Option methods								*/
/*										*/
/********************************************************************************/

static BdynOptions getOptions() 		{ return bdyn_options; }


private class ProcessHandler implements BumpRunEventHandler {

   @Override public synchronized void handleProcessEvent(BumpRunEvent evt) {
      BumpProcess proc = evt.getProcess();
      if (proc == null) return;
      BdynProcess bp = process_map.get(proc);
   
      switch (evt.getEventType()) {
         case PROCESS_ADD :
            if (bp == null) setupProcess(evt);
            break;
         case PROCESS_REMOVE :
            if (bp != null) {
               process_map.remove(proc);
               BumpTrieNode tn = bp.getTrieRoot();
               if (tn != null) callback_set.updateCallbacks(tn);
               bp.finish();
             }
            break;
         case PROCESS_TRACE :
            if (bp != null) {
               Element xml = (Element) evt.getEventData();
               bp.handleTraceEvent(xml);
             }
            break;
         default :
            break;
       }
    }

}	// end of inner class ProcessHandler




/********************************************************************************/
/*										*/
/*	Button actions								*/
/*										*/
/********************************************************************************/

private static class TaskAction implements BudaConstants.ButtonListener {

   @Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt) {
      BdynTaskWindow tw = new BdynTaskWindow();
      BudaBubble bb = tw.getBubble();
      bba.addBubble(bb,null,pt,BudaConstants.PLACEMENT_LOGICAL);

      Object proc = bba.getProperty("Bddt.process");
      if (proc != null) {
	 BumpProcess bp = (BumpProcess) proc;
	 if (bp.isRunning()) tw.setProcess(bp);
       }
    }

}	// end of inner class TaskAction





/********************************************************************************/
/*										*/
/*	Methods to handle callback information					*/
/*										*/
/********************************************************************************/

BdynCallback getCallback(int id)
{
   return callback_set.getCallback(id);
}



void resetCallbacks()
{
   callback_set.clear();
}


Set<Color> getKnownColors()
{
   return callback_set.getKnownColors();
}

void saveCallbacks()
{
   callback_set.saveCallbacks();
}



/********************************************************************************/
/*										*/
/*	Option implementation							*/
/*										*/
/********************************************************************************/

private static class OptionSet implements BdynOptions {

   private boolean use_key_callback;
   private boolean use_main_callback;
   private boolean use_main_task;

   OptionSet() {
      use_key_callback = true;
      use_main_callback = false;
      use_main_task = false;
    }

   @Override public boolean useKeyCallback()		{ return use_key_callback; }
   @Override public boolean useMainCallback()		{ return use_main_callback; }
   @Override public boolean useMainTask()		{ return use_main_task; }

   @Override public void setUseKeyCallback(boolean fg)	{ use_key_callback = fg; }
   @Override public void setUseMainCallback(boolean fg) { use_main_callback = fg; }
   @Override public void setUseMainTask(boolean fg)	{ use_main_task = fg; }

   @Override public void save(IvyXmlWriter xw) {
      xw.field("KEYCB",use_key_callback);
      xw.field("MAINCB",use_main_callback);
      xw.field("MAINTK",use_main_task);
    }

   @Override public void load(Element xml) {
      use_key_callback = IvyXml.getAttrBool(xml,"KEYCB",use_key_callback);
      use_main_callback = IvyXml.getAttrBool(xml,"MAINCB",use_main_callback);
      use_main_task = IvyXml.getAttrBool(xml,"MAINTK",use_main_task);
    }

}	// end of inner class OptionSet


}	// end of class BdynFactory




/* end of BdynFactory.java */

