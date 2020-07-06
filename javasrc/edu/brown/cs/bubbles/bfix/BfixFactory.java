/********************************************************************************/
/*										*/
/*		BfixFactory.java						*/
/*										*/
/*	Factory interface for automatic bubbles fixer				*/
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



package edu.brown.cs.bubbles.bfix;

import edu.brown.cs.bubbles.bale.BaleConstants;
import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.BoardConstants;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.JPopupMenu;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;


public class BfixFactory implements BfixConstants, BaleConstants,
	BudaConstants.ButtonListener
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/**************************************************ao******************************/

private Map<BfixCorrector,Boolean>	all_correctors;
private Contexter			context_handler;
private List<BfixAdapter>		all_adapters;
private BfixOrderBase			base_order;
private BfixChoreManager		chore_manager;

private static BoardProperties bfix_props = BoardProperties.getProperties("Bfix");
private static BfixFactory the_factory = null;



public synchronized static BfixFactory getFactory()
{
   if (the_factory == null) {
      the_factory = new BfixFactory();
    }
   return the_factory;
}



/********************************************************************************/
/*										*/
/*	Initialization methods							*/
/*										*/
/********************************************************************************/

public static void setup()
{
   // do nothing
}


public static void initialize(BudaRoot br)
{
   // wait for Bale to be setup
   getFactory();

   if (BoardSetup.getSetup().getRunMode() == BoardConstants.RunMode.SERVER) return;

   BudaRoot.registerMenuButton(BFIX_CHORE_MANAGER_BUTTON,getFactory());
   BudaRoot.registerMenuButton(BFIX_MODEL_UPDATE_BUTTON,getFactory());
   BudaRoot.addFileHandler(new SaveHandler());
   // add hooks for chore bubble
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BfixFactory()
{
   all_correctors = new WeakHashMap<>();
   context_handler = new Contexter();
   BaleFactory.getFactory().addContextListener(context_handler);
   BoardSetup.RunMode runmode = BoardSetup.getSetup().getRunMode();

   all_adapters = new ArrayList<>();
   if (bfix_props != null && runmode != BoardSetup.RunMode.SERVER) {
      SortedMap<Integer,String> adapts = new TreeMap<>();
      for (String s : bfix_props.stringPropertyNames()) {
	 if (s.startsWith("Bfix.adapter.") && s.lastIndexOf(".") == 12) {
	    int idx = s.lastIndexOf(".");
	    try {
	       int v = Integer.parseInt(s.substring(idx+1));
	       adapts.put(v,s);
	     }
	    catch (NumberFormatException e) { }
	  }
       }
      Set<Class<?>> done = new HashSet<>();
      for (String s : adapts.values()) {
	 String val = bfix_props.getProperty(s);
	 Class<?> c = null;
	 try {
	    c = Class.forName(val);
	  }
	 catch (ClassNotFoundException e) {
	    try {
	       c = Class.forName("edu.brown.cs.bubbles.bifx.BfixAdapter" + val);
	     }
	    catch (ClassNotFoundException e1) {
	       BoardLog.logE("BFIX","Adapter " + val + " not found");
	    }
	  }
	 if (c != null && done.add(c)) {
	    try {
	       BfixAdapter adpt = (BfixAdapter) c.getDeclaredConstructor().newInstance();
	       all_adapters.add(adpt);
	     }
	    catch (Throwable e) { }
	  }
       }
    }

   Element xml = IvyXml.loadXmlFromStream(BoardProperties.getLibraryFile(ORDER_FILE));
   base_order = new BfixOrderBase(xml);

   chore_manager = new BfixChoreManager();
}



/********************************************************************************/
/*										*/
/*	Creation methods							*/
/*										*/
/********************************************************************************/

public BfixCorrector findCorrector(BaleWindow bw)
{
   BfixCorrector rslt = null;

   synchronized (all_correctors) {
      for (BfixCorrector bc : all_correctors.keySet()) {
	 if (bc.getEditor() == bw) {
	    rslt = bc;
	    break;
	  }
       }
      if (rslt == null) {
	 rslt = new BfixCorrector(bw,true);
	 all_correctors.put(rslt,true);
       }
    }

   return rslt;
}



BfixCorrector findCorrector(BaleWindowDocument doc)
{
   for (BfixCorrector bc : all_correctors.keySet()) {
      if (bc.getEditor().getWindowDocument() == doc) return bc;
    }
   return null;
}


void removeCorrector(BaleWindow bw)
{
   synchronized (all_correctors) {
      for (Iterator<BfixCorrector> it = all_correctors.keySet().iterator();
	 it.hasNext(); ) {
	 BfixCorrector bc = it.next();
	 if (bc.getEditor() == bw) {
	    bc.removeEditor();
	    it.remove();
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

List<BfixAdapter> getAdapters()
{
   if (bfix_props == null) return all_adapters;
   
   List<BfixAdapter> use = new ArrayList<>();
   
   for (BfixAdapter bf : all_adapters) {
      String prop = bf.getClass().getName();
      int idx = prop.lastIndexOf(".");
      prop = prop.substring(idx+1);
      prop = "Bfix.enable." + prop;
      if (bfix_props.getBoolean(prop,true)) use.add(bf);
    }
   
   return use;
}



BfixChoreManager getChoreManager()		{ return chore_manager; }


BfixOrderBase getBaseOrder()
{
   return base_order;
}


/********************************************************************************/
/*										*/
/*	Fix up methods -- called from BaleEditorKit using reflection            */
/*										*/
/********************************************************************************/

public static void fixErrorsInRegion(BaleWindowDocument doc,int soff,int eoff)
{
   BfixFactory bf = getFactory();
   BfixCorrector bc = bf.findCorrector(doc);
   if (bc == null) return;

   bc.fixErrorsInRegion(soff,eoff,true);
}




/********************************************************************************/
/*										*/
/*	Handle context menus							*/
/*										*/
/********************************************************************************/

private class Contexter implements BaleContextListener {

   @Override public BudaBubble getHoverBubble(BaleContextConfig cfg) {
      return null;
    }

   @Override public String getToolTipHtml(BaleContextConfig cfg) {
      return null;
    }


   @Override public void addPopupMenuItems(BaleContextConfig ctx,JPopupMenu menu) {
      if (ctx.inAnnotationArea()) return;
   
      BfixCorrector corr = null;
      synchronized (all_correctors) {
         for (BfixCorrector bc : all_correctors.keySet()) {
            BudaBubble bbl = bc.getEditor().getBudaBubble();
            if (bbl == ctx.getEditor()) {
               corr = bc;
               break;
             }
          }
       }
      if (corr != null) {
         corr.addPopupMenuItems(ctx,menu);
       }
    }

   @Override public void noteEditorAdded(BaleWindow win) {
      findCorrector(win);
    }

   @Override public void noteEditorRemoved(BaleWindow win) {
      removeCorrector(win);
    }

}



/********************************************************************************/
/*										*/
/*	Button action methods							*/
/*										*/
/********************************************************************************/

@Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt)
{
   if (id.equals(BFIX_CHORE_MANAGER_BUTTON)) {
      if (bba == null) return;
      BfixChoreBubble bbl = new BfixChoreBubble(getChoreManager());
      bba.addBubble(bbl,null,pt,
	    BudaConstants.PLACEMENT_LOGICAL|BudaConstants.PLACEMENT_MOVETO|
	    BudaConstants.PLACEMENT_NEW|BudaConstants.PLACEMENT_USER);
    }
   else if (id.equals(BFIX_MODEL_UPDATE_BUTTON)) {
      BfixAdapterImports.updateImports();
   }
}



/********************************************************************************/
/*                                                                              */
/*      Updating methods                                                        */
/*                                                                              */
/********************************************************************************/

private static class SaveHandler implements BudaConstants.BudaFileHandler {

   @Override public void handleSaveRequest()            { }
   
   @Override public void handleSaveDone() {
      // update import models
    }
   
   @Override public void handleCommitRequest() {
      // update import models
   }
   
   

   @Override public void handleCheckpointRequest()      { }

   @Override public boolean handleQuitRequest()         { return true; }

   @Override public void handlePropertyChange()         { }

}       // end of inner class SaveHandler



}	// end of class BfixFactory




/* end of BfixFactory.java */
