/********************************************************************************/
/*										*/
/*		BucsFactory.java						*/
/*										*/
/*	Bubbles Code Search factory class					*/
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



package edu.brown.cs.bubbles.bucs;

import edu.brown.cs.bubbles.bale.BaleConstants;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleContextConfig;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleWindow;
import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.bass.BassConstants;
import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.bass.BassName;
import edu.brown.cs.bubbles.batt.BattConstants;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.List;


public class BucsFactory implements BucsConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static BucsFactory the_factory = null;


/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

public static void setup()
{ }


public static void initialize(BudaRoot br)
{
   getFactory().setupCallbacks();
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public synchronized static BucsFactory getFactory()
{
   if (the_factory == null) {
      the_factory = new BucsFactory();
    }
   return the_factory;
}



private BucsFactory()
{
}



private void setupCallbacks()
{
   BoardLog.logD("BUCS","Setup for " + BoardSetup.getSetup().getLanguage());

   switch (BoardSetup.getSetup().getLanguage()) {
      case JAVA :
      case JAVA_IDEA :
	 BaleFactory.getFactory().addContextListener(new BucsContexter());
	 break;
      case REBUS :
	 BassFactory.getFactory().addPopupHandler(new BucsRebusContext());
	 break;
      case PYTHON :
	 break;
      case JS :
	 break;
    }
}




/********************************************************************************/
/*										*/
/*	Create new test bubbles 						*/
/*										*/
/********************************************************************************/

private boolean createTestCaseBubble(BaleContextConfig cfg,BattConstants.NewTestMode md)
{
   String mnm = cfg.getMethodName();
   if (mnm == null) return false;

   List<BumpLocation> locs = BumpClient.getBump().findMethod(null,mnm,false);
   if (locs == null || locs.size() == 0) return false;
   BumpLocation loc = locs.get(0);

   BudaBubble bb = new BucsTestCaseBubble(loc,cfg.getEditor());
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(cfg.getEditor());
   if (bba == null) return false;
   bba.addBubble(bb,cfg.getEditor(),null,
	    BudaConstants.PLACEMENT_PREFER|BudaConstants.PLACEMENT_MOVETO|BudaConstants.PLACEMENT_NEW);

   return true;
}






/********************************************************************************/
/*										*/
/*	Handle context clicks in the editor					*/
/*										*/
/********************************************************************************/

private class BucsContexter implements BaleConstants.BaleContextListener {

   @Override public BudaBubble getHoverBubble(BaleContextConfig cfg) {
      return null;
    }

   @Override public void addPopupMenuItems(BaleContextConfig cfg,JPopupMenu menu) {
      switch (cfg.getTokenType()) {
	 case METHOD_DECL_ID :
	    menu.add(new BucsAction(cfg));
	    break;
	 default :
	    break;
       }
    }

   @Override public String getToolTipHtml(BaleContextConfig cfg) {
      return null;
    }

   @Override public void noteEditorAdded(BaleWindow win) { }
   @Override public void noteEditorRemoved(BaleWindow win) { }

}	// end of inner class BucsContexter




/********************************************************************************/
/*										*/
/*	Handle Rebus requests							*/
/*										*/
/********************************************************************************/

private class BucsRebusContext implements BassConstants.BassPopupHandler {

   @Override public void addButtons(BudaBubble bb,Point where,JPopupMenu menu,
         String name,BassName forname) {
      BumpLocation loc = null;
      if (forname == null) {
         String proj = null;
         int idx = name.indexOf(":");
         if (idx > 0) {
            proj = name.substring(0,idx);
            name = name.substring(idx+1);
          }
         List<BumpLocation> locs = null;
         if (name.length() > 0)
            locs = BumpClient.getBump().findClassDefinition(proj,name);
         if (locs != null && locs.size() > 0) loc = locs.get(0);
         else {
            locs = BumpClient.getBump().findClassDefinition(proj,name + ".*");
            if (locs != null && locs.size() > 0) loc = locs.get(0);
          }
       }
      else {
         switch (forname.getNameType()) {
            case FILE :
            case CLASS :
               loc = forname.getLocation();
               break;
            default :
               break;
          }
       }
   
      BoardLog.logD("BUCS","Rebus context attempt " + loc);
   
      if (loc != null && loc.getFile() != null && loc.getS6Source() != null) {
         BaleConstants.BaleFileOverview fov =  BaleFactory.getFactory().getFileOverview(loc.getProject(),loc.getFile());
         try {
            String ftext = fov.getText(0,fov.getLength());
            boolean swingfg = ftext.contains("javax.swing");
            BoardLog.logD("BUCS","Rebus context check " + swingfg);
   
            if (swingfg) menu.add(new BucsUIAction(bb,loc));
          }
         catch (javax.swing.text.BadLocationException e) { }
       }
    }

}	// end of inner class BucsRebusContext



private static class BucsUIAction extends AbstractAction {

   private transient BumpLocation for_location;
   private BudaBubble source_bubble;

   private static final long serialVersionUID = 1;

   BucsUIAction(BudaBubble bb,BumpLocation loc) {
      super("Show User Interfaces");
      for_location = loc;
      source_bubble = bb;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BucsUserInterfaceBubble bbl = new BucsUserInterfaceBubble(for_location);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(source_bubble);
      if (bba != null) {
	 bba.addBubble(bbl,source_bubble,null,
		  BudaConstants.PLACEMENT_LOGICAL|BudaConstants.PLACEMENT_MOVETO|BudaConstants.PLACEMENT_NEW);
      }
    }

}	// end of inner class BucsUIAction



/********************************************************************************/
/*										*/
/*	Code search action							*/
/*										*/
/********************************************************************************/

private class BucsAction extends AbstractAction {

   private transient BaleContextConfig start_config;

   private static final long serialVersionUID = 1;


   BucsAction(BaleContextConfig cfg) {
      super("Code Search for Method Implementation");
      start_config = cfg;
    }

   @Override public void actionPerformed(ActionEvent e) {
       createTestCaseBubble(start_config,BattConstants.NewTestMode.INPUT_OUTPUT);
    }

}	// end of inner class BucsAction



}	// end of class BucsFactory




/* end of BucsFactory.java */

