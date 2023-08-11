/********************************************************************************/
/*										*/
/*		BassFactory.java						*/
/*										*/
/*	Bubble Augmented Search Strategies factory for search boxes		*/
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bass;

import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.bowi.BowiFactory;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaConstraint;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bueno.BuenoConstants;
import edu.brown.cs.bubbles.bueno.BuenoFactory;
import edu.brown.cs.bubbles.bueno.BuenoJsProject;
import edu.brown.cs.bubbles.bueno.BuenoPythonProject;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;

import edu.brown.cs.ivy.swing.SwingEventListenerList;
import edu.brown.cs.ivy.swing.SwingKey;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;



/**
 *	This class provides the factory methods for creating search bubbles
 *	and their associated repositories.
 **/

public class BassFactory implements BudaRoot.SearchBoxCreator, BassConstants, BudaConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private SwingEventListenerList<BassPopupHandler> popup_handlers;
private SwingEventListenerList<BassFlagger> flag_checkers;


private static BassRepositoryLocation	bass_repository;
private static BassFactory		the_factory;
private static Map<BudaBubbleArea,BassBubble> package_explorers;
private static Map<SearchType,Set<BassRepository>> use_repositories;
private static Map<BassRepository,BassTreeModelBase> repository_map;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

/**
 *	Return the singleton instance of the Bass search bubble factory.
 **/

public static BassFactory getFactory()
{
   return the_factory;
}



private BassFactory()
{
   popup_handlers = new SwingEventListenerList<>(BassPopupHandler.class);
   flag_checkers = new SwingEventListenerList<>(BassFlagger.class);
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

/**
 *	Called by initialization to ensure that the search package is set up correctly.
 **/

public static synchronized void setup()
{
   if (use_repositories != null) return;

   use_repositories = new HashMap<>();
   use_repositories.put(SearchType.SEARCH_ALL,new HashSet<>());

   the_factory = new BassFactory();
   BudaRoot.registerSearcher(the_factory);
   BudaRoot.addBubbleConfigurator("BASS",new BassConfigurator());
   package_explorers = new HashMap<>();

   BudaRoot.registerMenuButton("Package Explorer",new PackageExplorerButton(),"Add/Remove the package explorer panel for easier browsing");
   BudaRoot.registerMenuButton("Text Search",new TextSearchButton(null),"Search for a string or pattern in all files");

   BudaRoot.addToolbarButton("DefaultMenu",new TextSearchButton(null),"Text search",BoardImage.getImage("search"));

   repository_map = new HashMap<>();

   the_factory.addPopupHandler(new BassCreator());
   the_factory.addPopupHandler(new ProjectProps());

   bass_repository = new BassRepositoryLocation();
   registerRepository(SearchType.SEARCH_CODE,bass_repository);
   registerRepository(SearchType.SEARCH_EXPLORER,bass_repository);
}



/**
 *	Called to initialize once BudaRoot is setup
 **/


public static void initialize(BudaRoot br)
{
   BoardLog.logD("BASS","Initialize");

   if (bass_properties.getBoolean(BASS_PACK_ON_START_NAME) &&
	  !BoardSetup.getConfigurationFile().exists()) {
      BudaBubbleArea bba = br.getCurrentBubbleArea();
      addPackageExplorer(bba);
    }

   BuenoFactory bueno = BuenoFactory.getFactory();
   bueno.setClassMethodFinder(new MethodFinder());

   SwingKey.registerKeyAction("ROOT",(JComponent) br.getContentPane(),
         new TextSearchButton(null),"F9");
// br.registerKeyAction(new TextSearchButton(null),"TEXT SEARCH",
// 	 KeyStroke.getKeyStroke(KeyEvent.VK_F9,0));
}



/**
 *	Register a BassRepository for a particular type of search.
 **/

public static void registerRepository(SearchType st,BassRepository br)
{
   Set<BassRepository> sbr = use_repositories.get(st);
   if (sbr == null) {
      sbr = new HashSet<BassRepository>();
      use_repositories.put(st,sbr);
    }
   sbr.add(br);
   use_repositories.get(SearchType.SEARCH_ALL).add(br);
}





/**
 *	Wait for names to be loaded
 **/

public static void waitForNames()
{
   bass_repository.waitForNames();
}



private static BassBubble addPackageExplorer(BudaBubbleArea bba)
{
   BudaRoot br = BudaRoot.findBudaRoot(bba);
   if (br == null) return null;

   BassBubble peb = getFactory().createPackageExplorer(bba);
   if (peb == null) return null;

   Rectangle r = br.getCurrentViewport();
   int rh = br.getDefaultViewportHeight() - 2;
   Dimension d = peb.getPreferredSize();
   d.height = rh;
   peb.setSize(d);
   BudaConstraint bc = new BudaConstraint(BudaBubblePosition.DOCKED,
					     r.x + r.width - d.width,
					     r.y + r.height - rh);
   
   if (SwingUtilities.isEventDispatchThread()) {
      bba.add(peb,bc);
    }
   else {
      BubbleAdder ba = new BubbleAdder(bba,peb,bc);
      SwingUtilities.invokeLater(ba);
    }

   return peb;
}




/********************************************************************************/
/*										*/
/*	Popup handling setup							*/
/*										*/
/********************************************************************************/

/**
 *	Add a new handler for popup menu options in the search box
 **/

public void addPopupHandler(BassPopupHandler ph)
{
   popup_handlers.add(ph);
}


/**
 *	Remove a handler for popup menu options in the search box
 **/

public void removePopupHandler(BassPopupHandler ph)
{
   popup_handlers.remove(ph);
}



void addButtons(Component c,Point where,JPopupMenu m,String fnm,BassName bn)
{
   BudaBubble bb = BudaRoot.findBudaBubble(c);
   if (bb == null) return;

   for (BassPopupHandler ph : popup_handlers) {
      ph.addButtons(bb,where,m,fnm,bn);
    }
}




/**
 *	Add a bubble relative to the search box of the given window
 **/

public void addNewBubble(BudaBubble searchbox,Point loc,BudaBubble bbl)
{
   if (bbl == null || searchbox == null) return;

   Component c = searchbox.getContentPane();
   if (!(c instanceof BassSearchBox)) return;
   BassSearchBox sbox = (BassSearchBox) c;

   int ypos = 0;
   if (loc != null) ypos = loc.y;
   else {
      Rectangle r = BudaRoot.findBudaLocation(searchbox);
      if (r != null) ypos = r.y;
    }

   sbox.addAndLocateBubble(bbl,ypos,loc);
}




/********************************************************************************/
/*										*/
/*	Bubble creation methods 						*/
/*										*/
/********************************************************************************/

/**
 *	Create as search bubble of a given type for the given project with the
 *	given initial search string
 **/

@Override public BassBubble createSearch(SearchType type,String proj,String pfx)
{
   BassRepository br = getRepository(type);

   if (br == null) return null;

   return new BassBubble(br,proj,pfx,true);
}

/**
 *	returns the package explorer if it exists, otherwise null
 */

@Override public BudaBubble getPackageExplorer(BudaBubbleArea bba)
{
   return package_explorers.get(bba);
}


/**
 *	Create a bubble that can be used as the package explorer.
 **/

public BassBubble createPackageExplorer(BudaBubbleArea bba)
{
   BassRepository brm = getRepository(SearchType.SEARCH_EXPLORER);
   BassBubble peb = new BassBubble(brm,null,null,false);
   if (peb != null) package_explorers.put(bba,peb);
   return peb;
}



/**
 *	Create a text search bubble.
 **/

public BassTextBubble createTextSearch(String project)
{
   return new BassTextBubble("",project);
}



public static BassRepository getRepository(BudaConstants.SearchType typ)
{
   Set<BassRepository> sbr = use_repositories.get(typ);
   if (sbr == null) return null;
   List<BassRepository> lbr = null;
   for ( ; ; ) {
      try {
	 lbr = new ArrayList<>(sbr);
	 break;
       }
      catch (ConcurrentModificationException e) { }
    }
   BassRepository rslt = null;
   if (lbr != null) {
      for (BassRepository br : lbr) {
	 if (rslt == null) rslt = br;
	 else rslt = new BassRepositoryMerge(br,rslt);
       }
    }
   
   if (rslt.isEmpty()) return null;
   
   return rslt;
}




/********************************************************************************/
/*										*/
/*	Methods to handle query 						*/
/*										*/
/********************************************************************************/

/**
 *	This returns the name of the bubble associated with the given file at the
 *	given position.  Note that this position is given in terms of the IDE, not
 *	in terms of how Java sees it.  This is used, for example, by error handling
 *	to create a bubble for the code corresponding to an error message.  When there
 *	are multiple symbols spanning the given location (e.g. a class and a method),
 *	this will return the innermost one.
 **/

public BassName findBubbleName(File f,int eclipsepos)
{
   return bass_repository.findBubbleName(f,eclipsepos);
}


public boolean checkMethodName(String proj,String fullname,String args)
{
   return bass_repository.checkMethodName(proj,fullname,args);
}


private static class MethodFinder implements BuenoConstants.BuenoClassMethodFinder {

   @Override public List<BumpLocation> findClassMethods(String cls) {
      return bass_repository.findClassMethods(cls);
    }

}	// end of inner class MethodFinder



public File findActualFile(File f)
{
   return bass_repository.findActualFile(f);
}


Set<File> findAssociatedFiles(String proj,String pfx)
{
   return bass_repository.findAssociatedFiles(proj,pfx);
}




/********************************************************************************/
/*										*/
/*	Flag management routines						*/
/*										*/
/********************************************************************************/

public void addFlagChecker(BassFlagger bf)
{
   flag_checkers.add(bf);
}


public void removeFlagChecker(BassFlagger bf)
{
   flag_checkers.remove(bf);
}


BassFlag getFlagForName(BassName bnm,String name)
{
   BassFlag best = null;

   for (BassFlagger bf : flag_checkers) {
      BassFlag xf = bf.getFlagForName(bnm,name);
      if (xf != null) {
	 if (best == null || best.getPriority() < xf.getPriority()) {
	    best = xf;
	  }
       }
    }

   return best;
}


public void flagsUpdated()
{
   for (BassBubble bb : package_explorers.values()) {
      if (bb == null) continue;
      Component c = bb.getContentPane();
      if (c != null) c.repaint();
   }
}



/********************************************************************************/
/*										*/
/*	Methods to handle package explorer creation				*/
/*										*/
/********************************************************************************/

private static class PackageExplorerButton implements BudaConstants.ButtonListener
{

   PackageExplorerButton()			{ }

   @Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt) {
      BassBubble peb = package_explorers.get(bba);
      if (peb != null && peb.isVisible()) {		// isShowing() ?
         peb.setVisible(false);
         return;
       }
   
      addPackageExplorer(bba);
    }

}	// end of inner class PackageExplorerButton




/********************************************************************************/
/*										*/
/*	Methods to handle text search request					*/
/*										*/
/********************************************************************************/

private static class TextSearchButton extends AbstractAction implements BudaConstants.ButtonListener,
	ActionListener
{
   private String for_project;
   private final static long serialVersionUID = 1;

   TextSearchButton(String proj) {
      super("Text Search" + (proj == null ? "" : " in " + proj));
      for_project = proj;
    }

   @Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt) {
      BowiFactory.startTask();
      try {
         BudaRoot br = BudaRoot.findBudaRoot(bba);
         if (br == null) return;
         BudaBubble bb = the_factory.createTextSearch(for_project);
         BudaConstraint bc = new BudaConstraint(BudaBubblePosition.STATIC,pt);
         br.add(bb,bc);
         bb.grabFocus();
       }
      finally {
         BowiFactory.stopTask();
       }
    }

   @Override public void actionPerformed(ActionEvent evt) {
      Component c = (Component) evt.getSource();
      if (c == null) return;
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(c);
      if (bba == null) {
         BudaRoot broot = BudaRoot.findBudaRoot(c);
         bba = broot.getCurrentBubbleArea();
       }
      BudaBubble bb = the_factory.createTextSearch(for_project);
      bba.addBubble(bb,c,null,BudaConstants.PLACEMENT_LOGICAL);
    }

}	// end of inner class TextSearchButton



/********************************************************************************/
/*										*/
/*	Methods to handle repository mapping					*/
/*										*/
/********************************************************************************/

public static void reloadRepository(BassRepository br)
{
   synchronized (repository_map) {
      for (BassRepository abr : repository_map.keySet()) {
	 if (abr.includesRepository(br)) {
	    BassTreeModelBase tmb = getModelBase(abr);
	    tmb.requestRebuild();
	    // tmb.rebuild();
	  }
       }
    }
}



static BassTreeModelBase getModelBase(BassRepository br)
{
   synchronized (repository_map) {
      BassTreeModelBase tmb = repository_map.get(br);
      if (tmb == null) {
	 tmb = new BassTreeModelBase(br);
	 repository_map.put(br,tmb);
       }
      return tmb;
    }
}



/********************************************************************************/
/*										*/
/*	Class to handle project property dialog 				*/
/*										*/
/********************************************************************************/

private static class ProjectProps implements BassPopupHandler {

   @Override public void addButtons(BudaBubble bb,Point where,JPopupMenu menu,
        			       String fullname,BassName bn) {
      if (bn != null) return;
      if (fullname.startsWith("@")) return;
   
      int idx = fullname.indexOf(":");
      if (idx <= 0) return;
      String proj = fullname.substring(0,idx);
   
      switch (BoardSetup.getSetup().getLanguage()) {
         case JAVA :
         case JAVA_IDEA :
            // menu.add(new EclipseProjectAction(proj));
            menu.add(new ProjectAction(proj,bb,where));
            menu.add(new NewProjectAction(bb,where));
            menu.add(new BassImportProjectAction());
            break;
         case PYTHON :
            menu.add(new PythonProjectAction(proj,bb,where));
            menu.add(new NewPythonProjectAction(bb,where));
            break;
         case JS:
            menu.add(new JSProjectAction(proj,bb,where));
            menu.add(new NewJSProjectAction(bb,where));
            break;
         case DART :
            // TOOD: add dart buttons for project
            break;     
         case REBUS :
            break;
       }
      
      menu.add(new TextSearchButton(proj));
    }

}	// end of inner class ProjectProps



@SuppressWarnings("unused")
private static class EclipseProjectAction extends AbstractAction {

   private String for_project;

   private static final long serialVersionUID = 1;

   EclipseProjectAction(String proj) {
      super("Eclipse Project Properties for " + proj);
      for_project = proj;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","EclipseProjectProperties");
      BudaRoot.hideSearchBubble(e);
      BumpClient bc = BumpClient.getBump();
      bc.saveAll();
      bc.editProject(for_project);
    }

}	// end of inner class EclipseProjectAction



private static class ProjectAction extends AbstractAction {

   private String for_project;
   private BudaBubble rel_bubble;
   private Point rel_point;

   private static final long serialVersionUID = 1;

   ProjectAction(String proj,BudaBubble rel,Point pt) {
      super("Edit Properties of Project " + proj);
      for_project = proj;
      rel_bubble = rel;
      rel_point = pt;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","EditProjectProperties");
      BudaRoot.hideSearchBubble(e);
      BudaBubble bb = BuenoFactory.getFactory().createProjectDialog(for_project);
      if (bb == null) return;
      BassFactory.getFactory().addNewBubble(rel_bubble,rel_point,bb);
    }

}	// end of inner class ProjectAction



private static class PythonProjectAction extends AbstractAction {

   private String for_project;
   private BudaBubble rel_bubble;
   private Point rel_point;

   private static final long serialVersionUID = 1;

   PythonProjectAction(String proj,BudaBubble rel,Point pt) {
      super("Edit Properties of Project " + proj);
      for_project = proj;
      rel_bubble = rel;
      rel_point = pt;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","EditPythonProjectProperties");
      BudaRoot.hideSearchBubble(e);
      BudaBubble bb = null;
      bb = BuenoPythonProject.createEditPythonProjectBubble(for_project);
      if (bb == null) return;
      BassFactory.getFactory().addNewBubble(rel_bubble,rel_point,bb);
    }

}	// end of inner class PythonProjectAction


private static class JSProjectAction extends AbstractAction {

   private String for_project;
   private BudaBubble rel_bubble;
   private Point rel_point;

   private static final long serialVersionUID = 1;

   JSProjectAction(String proj,BudaBubble rel,Point pt) {
      super("Edit Properties of Project " + proj);
      for_project = proj;
      rel_bubble = rel;
      rel_point = pt;
   }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","EditJSProjectProperties");
      BudaRoot.hideSearchBubble(e);
      BudaBubble bb = BuenoFactory.getFactory().createProjectDialog(for_project);
      if (bb == null) return;
      BassFactory.getFactory().addNewBubble(rel_bubble,rel_point,bb);
   }

}	// end of inner class JSProjectAction



private static class NewProjectAction extends AbstractAction {

   private BudaBubble rel_bubble;
   private Point rel_point;

   private static final long serialVersionUID = 1;

   NewProjectAction(BudaBubble bb,Point pt) {
      super("Create New Project");
      rel_bubble = bb;
      rel_point = pt;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","CreateProject");
      BudaRoot.hideSearchBubble(e);
      BudaBubble bbl = BuenoFactory.getFactory().getCreateProjectBubble();
      if (bbl == null) return;
      BassFactory.getFactory().addNewBubble(rel_bubble,rel_point,bbl);
    }

}	// end of inner class ProjectAction



private static class NewPythonProjectAction extends AbstractAction {

   private BudaBubble rel_bubble;
   private Point rel_point;

   private static final long serialVersionUID = 1;

   NewPythonProjectAction(BudaBubble bb,Point pt) {
      super("Create New Project");
      rel_bubble = bb;
      rel_point = pt;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","CreatePythonProject");
      BudaRoot.hideSearchBubble(e);

      BudaBubble bb = null;
      bb = BuenoPythonProject.createNewPythonProjectBubble();
      if (bb != null) {
	 BassFactory.getFactory().addNewBubble(rel_bubble,rel_point,bb);
       }
    }

}	// end of inner class ProjectAction


private static class NewJSProjectAction extends AbstractAction {

   private BudaBubble rel_bubble;
   private Point rel_point;

   private static final long serialVersionUID = 1;

   NewJSProjectAction(BudaBubble bb,Point pt) {
      super("Create New Project");
      rel_bubble = bb;
      rel_point = pt;
   }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BASS","CreateJSProject");
      BudaRoot.hideSearchBubble(e);
      BudaBubble bb = null;
      bb = BuenoJsProject.createNewJsProjectBubble();
      if (bb != null) {
         BassFactory.getFactory().addNewBubble(rel_bubble,rel_point,bb);
       }
   }

}	// end of inner class ProjectAction



/********************************************************************************/
/*                                                                              */
/*      Add package explorer correctly                                          */
/*                                                                              */
/********************************************************************************/

private static class BubbleAdder implements Runnable {
   
   private BudaBubbleArea bubble_area;
   private BudaBubble buda_bubble;
   private BudaConstraint buda_constraint;
   
   BubbleAdder(BudaBubbleArea bba,BudaBubble bb,BudaConstraint bc) {
      bubble_area = bba;
      buda_bubble = bb;
      buda_constraint = bc;
    }
   
   
   @Override public void run() {
      bubble_area.add(buda_bubble,buda_constraint);
    }
   
}       // end of inner class AddPackageExplorer


}	// end of class BassFactory




/* end of BassFactory.java */
