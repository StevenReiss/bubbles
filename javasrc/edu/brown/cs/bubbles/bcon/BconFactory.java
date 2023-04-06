/********************************************************************************/
/*										*/
/*		BconFactory.java						*/
/*										*/
/*	Bubbles Environment Context Viewer factory and setup class		*/
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



package edu.brown.cs.bubbles.bcon;

import edu.brown.cs.bubbles.bale.BaleConstants.BaleContextConfig;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleContextListener;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleWindow;
import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;


/**
 *	This class provides the entries for setting up and providing access to
 *	the various context view bubbles.
 **/

public class BconFactory implements BconConstants, BudaConstants.ButtonListener
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<BudaBubbleArea,BconOverviewPanel> current_panel;


private static BconFactory	the_factory = null;




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

/**
 *	This routine is called automatically at startup to initialize the module.
 **/

public static void setup()
{
   BudaRoot.addBubbleConfigurator("BCON",new BconConfigurator());
   BudaRoot.registerMenuButton(BCON_BUTTON,getFactory());
   BconRepository br = new BconRepository();
   BassFactory.registerRepository(BudaConstants.SearchType.SEARCH_CODE,br);
   BassFactory.registerRepository(BudaConstants.SearchType.SEARCH_EXPLORER,br);
}


public static void initialize(BudaRoot br)
{
   BaleFactory.getFactory().addContextListener(new ContextHandler());
}

/**
 *	Return the singleton instance of the context viewer factory.
 **/

public static synchronized BconFactory getFactory()
{
   if (the_factory == null) {
      the_factory = new BconFactory();
    }
   return the_factory;
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BconFactory()
{
   current_panel = new HashMap<BudaBubbleArea,BconOverviewPanel>();
}




/********************************************************************************/
/*										*/
/*	Factory methods 							*/
/*										*/
/********************************************************************************/

/**
 *	Create an overview bubble to show the files that are currently active
 *	and their regions and bubbles.
 **/

public BudaBubble createOverviewBubble(Component source,Point pt)
{
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(source);
   if (bba == null) return null;

   BconOverviewPanel pnl = new BconOverviewPanel(bba,pt);
   current_panel.put(bba,pnl);

   return new BconBubble(pnl);
}



/**
 *	Create a class panel bubble showing all class information
 **/

public BudaBubble createClassBubble(Component source,String proj,File f,String cls,boolean inner)
{
   return createClassBubble(source,proj,f,cls,inner,null);
}


public BudaBubble createClassBubble(Component source,String proj,File f,String cls,boolean inner,
      String element)
{
   BconClassPanel pnl = new BconClassPanel(proj,f,cls,inner);

   if (!pnl.isValid()) return null;

   pnl.showElement(element);

   return new BconBubble(pnl);
}



/**
 *	Create a package panel bubble showing package relationships
 **/

public BudaBubble createPackageBubble(Component source,String proj,String pkg)
{
   if (pkg != null) {
      int idx = pkg.indexOf(":");
      if (idx >= 0) pkg = pkg.substring(idx+1);
      if (pkg.length() == 0) pkg = null;
    }
   
   BconPackagePanel pnl = new BconPackagePanel(proj,pkg);

   return new BconBubble(pnl);
}



/********************************************************************************/
/*										*/
/*	Menu button handling							*/
/*										*/
/********************************************************************************/

@Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt)
{
   BudaBubble bb = null;

   if (id.equals(BCON_BUTTON)) {
      bb = createOverviewBubble(bba,pt);
    }

   if (bb != null && bba != null) {
      bba.addBubble(bb,null,pt,BudaConstants.PLACEMENT_LOGICAL|BudaConstants.PLACEMENT_MOVETO|
		       BudaConstants.PLACEMENT_NEW|BudaConstants.PLACEMENT_USER);
    }
}



/********************************************************************************/
/*										*/
/*	Handle right-click menu options 					*/
/*										*/
/********************************************************************************/

private static class ContextHandler implements BaleContextListener {

   ContextHandler() { }

   @Override public BudaBubble getHoverBubble(BaleContextConfig cfg) {
      return null;
    }

   @Override public String getToolTipHtml(BaleContextConfig cfg) {
      return null;
    }

   @Override public void addPopupMenuItems(BaleContextConfig cfg,JPopupMenu menu) {
      if (cfg.getOffset() >= 0) {
	 menu.add(new ContextAction(cfg));
       }
    }

   @Override public void noteEditorAdded(BaleWindow win)		{ }
   @Override public void noteEditorRemoved(BaleWindow win)		{ }

}	// end of inner class ContextHandler



private static class ContextAction extends AbstractAction {

   private BaleContextConfig context_config;

   private static final long serialVersionUID = 1;

   ContextAction(BaleContextConfig cfg) {
      super("Show Context of Bubble");
      context_config = cfg;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      if (context_config.getEditor() == null) return;
      BudaBubble bbl = context_config.getEditor();
      File fil = bbl.getContentFile();
      String method = context_config.getMethodName();
      String cname = bbl.getContentName();
      String proj = bbl.getContentProject();
      System.err.println("CONTEXT: " + proj + " " + cname + " " + method);
      BconFactory fac = BconFactory.getFactory();
      String cls = cname;
      if (method == null) {
         if (cname.endsWith(">")) {
            int idx = cname.lastIndexOf(".");
            if (idx > 0) cls = cname.substring(0,idx);
          }
       }
      else {
         int idx = method.indexOf("(");
         int idx1 = method.lastIndexOf(".",idx);
         cls = method.substring(0,idx1);
         method = method.substring(idx1+1);
       }
      String fnm = fil.getName();
      if (fnm.endsWith(".java")) fnm = fnm.substring(0,fnm.length()-5);
      boolean inner = !cls.endsWith(fnm);
      BudaBubble nbbl = fac.createClassBubble(bbl,proj,fil,cls,inner,cname);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bbl);
      if (bba != null && nbbl != null) {
         bba.addBubble(nbbl,bbl,null,
        	  BudaConstants.PLACEMENT_PREFER|BudaConstants.PLACEMENT_GROUPED|BudaConstants.PLACEMENT_NEW);
       }
    }

}	// end of inner class ContextAction





}	// end of class BconFactory



/* end of BconFactory.java */
