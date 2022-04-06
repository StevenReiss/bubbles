/********************************************************************************/
/*										*/
/*		RebusFactory.java						*/
/*										*/
/*	Factory methods for REpository BUbbles System				*/
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



package edu.brown.cs.bubbles.rebus;

import edu.brown.cs.bubbles.bale.BaleConstants;
import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.bass.BassConstants;
import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.bass.BassName;
import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;

import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JPopupMenu;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;



public class RebusFactory implements RebusConstants, BaleConstants, BassConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private AcceptanceFlagger	acceptance_flagger;

private static RebusFactory	the_factory = null;
private static BudaRoot buda_root = null;

private static AcceptFlag accept_flag = new AcceptFlag("accept_overlay",25);


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public synchronized static RebusFactory getFactory()
{
   if (the_factory == null) {
      the_factory = new RebusFactory();
    }
   return the_factory;
}



private RebusFactory()
{
   acceptance_flagger = new AcceptanceFlagger();
}



/********************************************************************************/
/*										*/
/*	Initialization Methods							*/
/*										*/
/********************************************************************************/

public static void setup()
{
}


public static void initialize(BudaRoot br)
{
   buda_root = br;

   BassFactory.getFactory().addPopupHandler(new SearchExtender());
   BassFactory.getFactory().addPopupHandler(new Acceptor());
   BassFactory.getFactory().addFlagChecker(getFactory().acceptance_flagger);

   BudaRoot.addToolbarButton("DefaultMenu",new SearchButton(),
		 "Code Search",BoardImage.getImage("codesearch"));
   BudaRoot.addToolbarButton("DefaultMenu",new DeleteAllButton(),
	 "Remove All Projects",BoardImage.getImage("deleteall"));
   BudaRoot.addToolbarButton("DefaultMenu",new ExportButton(),
	 "Export Accepted Files",BoardImage.getImage("fileexport"));

   BaleFactory.getFactory().addContextListener(new RebusContexter());

   setupCache();
}


/********************************************************************************/
/*										*/
/*	Create a search bubble							*/
/*										*/
/********************************************************************************/

public static BudaBubble createSearchBubble()
{
   return new RebusSearchBubble();
}


private static class SearchButton implements ActionListener
{

   @Override public void actionPerformed(ActionEvent e)  {
      BudaBubble bb = createSearchBubble();
      BudaBubbleArea bba = buda_root.getCurrentBubbleArea();
      if (bba != null && bb != null) {
	 bba.addBubble(bb,null,null,BudaConstants.PLACEMENT_LOGICAL);
       }
    }

}	// end of inner class SearchButton




private static class ExportButton implements ActionListener
{
   private static File last_directory = null;

   @Override public void actionPerformed(ActionEvent e)  {
       JFileChooser chooser = new JFileChooser(last_directory);
       chooser.setDialogTitle("Select Export Directory");
       chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
       int rvl = chooser.showSaveDialog((Component) e.getSource());
       if (rvl == JFileChooser.APPROVE_OPTION) {
	  last_directory = chooser.getSelectedFile();
	  String cmd = "<BUBBLES DO='REBUSEXPORT'";
	  cmd += " DIR='" + last_directory.getAbsolutePath() + "'";
	  cmd += " ACCEPT='true'";
	  cmd += " LANG='Rebase' />";
	  BoardSetup bs = BoardSetup.getSetup();
	  MintControl mc = bs.getMintControl();
	  mc.send(cmd);
	}
    }

}	// end of inner class SearchButton





/********************************************************************************/
/*										*/
/*	Handle Request to delete all projects					*/
/*										*/
/********************************************************************************/

private static class DeleteAllButton implements ActionListener, Runnable {

   @Override public void actionPerformed(ActionEvent e) {
      BoardThreadPool.start(this);
    }

   @Override public void run() {
      BumpClient bc = BumpClient.getBump();
      bc.delete(null,"PROJECT",null,false);
      RebusSearchBubble.clearAll();
    }

}	// end of inner class DeleteAllButton


/********************************************************************************/
/*										*/
/*	Search buttons								*/
/*										*/
/********************************************************************************/

private static class SearchExtender implements BassConstants.BassPopupHandler {

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

     if (loc != null) {
	if (loc.getProject() != null && loc.getFile() != null) {
	   menu.add(new ExtenderAction(loc,true));
	   menu.add(new ExtenderAction(loc,false));
	 }
	if (loc.getProject() != null) {
	   menu.add(new BuildProjectAction(loc.getProject()));
	 }
      }
   }

}	// end of inner class SearchExpander






private static class ExtenderAction extends AbstractAction {

   private String command_name;
   private String for_project;
   private String for_file;

   private static final long serialVersionUID = 1;

   ExtenderAction(BumpLocation loc,boolean next) {
      super((next ? "Get Next Set of Results" : "Expand this Result"));
      command_name = (next ? "REBUSNEXT" : "REBUSEXPAND");
      for_project = loc.getProject();
      for_file = loc.getFile().getPath();
    }

   @Override public void actionPerformed(ActionEvent evt) {
       BoardSetup bs = BoardSetup.getSetup();
       MintControl mc = bs.getMintControl();
       String cmd = "<BUBBLES DO='" + command_name + "' PROJECT='" + for_project +
	  "' FILE='" + for_file + "' LANG='Rebase' />";
       mc.send(cmd);
    }

}	// end of inner class ExtenderAction


private static class BuildProjectAction extends AbstractAction {

   private String project_name;

   private static final long serialVersionUID = 1;

   BuildProjectAction(String proj) {
      super("Build Project " + proj);
      project_name = proj;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BumpClient bc = BumpClient.getBump();
      bc.build(project_name, true,true,false);
    }

}	// end of inner class BuildProjectAction







/********************************************************************************/
/*										*/
/*	Contexter for identifier buttons					*/
/*										*/
/********************************************************************************/

private static class RebusContexter implements BaleContextListener {

   RebusContexter() { }

   @Override public BudaBubble getHoverBubble(BaleContextConfig cfg) {
      return null;
    }

   @Override public String getToolTipHtml(BaleContextConfig cfg){
      return null;
    }

   @Override public void addPopupMenuItems(BaleContextConfig cfg,JPopupMenu menu) {
      if (cfg.getToken() == null) return;

      switch (cfg.getTokenType()) {
	 case UNDEF_CALL_ID :
	 case UNDEF_ID :
	    break;
	 case TYPE_ID :
	 case FIELD_ID :
	 case STATIC_FIELD_ID :
	 case ANNOTATION_ID :
	    // check if undefined?
	    break;
	 case LOCAL_ID :
	 case CLASS_DECL_ID :
	 case METHOD_DECL_ID :
	 case LOCAL_DECL_ID :
	 case FIELD_DECL_ID :
	 case CONST_ID :
	 case NONE :
	 default :
	    return;
       }
      menu.add(new RebusFindItem(cfg));
    }

   @Override public void noteEditorAdded(BaleWindow win)        { }
   @Override public void noteEditorRemoved(BaleWindow win)      { }
   
}	// end of inner class RebusContexter


private static class RebusFindItem extends AbstractAction {

   private transient BaleContextConfig context_config;

   private static final long serialVersionUID = 1;

   RebusFindItem(BaleContextConfig cfg) {
      super("Search Project for Definition of " + cfg.getToken());
      context_config = cfg;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BudaBubble bb = context_config.getEditor();
      BaleFileOverview bfo = context_config.getDocument();
      String proj = bb.getContentProject();
      int off = context_config.getOffset();
      int foff = bfo.mapOffsetToEclipse(off);
   
      File f = bb.getContentFile();
      StringTokenizer tok = new StringTokenizer(f.getPath(),"/\\");
      String repo = null;
      for (int i = 0; i < 3; ++i) {
         if (tok.hasMoreTokens()) repo = tok.nextToken();
       }
   
      BumpClient bc = BumpClient.getBump();
      String fnm = bc.getFullyQualifiedName(proj,bb.getContentFile(),foff,foff,30000);
      String fnm1 = null;
      if (fnm == null) fnm = context_config.getToken();
      if (fnm == null) return;
   
      switch (context_config.getTokenType()) {
         case UNDEF_CALL_ID :
         case CALL_ID :
            fnm = "mdef:" + fnm;
            break;
         case TYPE_ID :
            fnm1 = "idef:" + fnm;
            fnm = "cdef:" + fnm;
            break;
         default :
            break;
       }
   
      if (fnm != null) startSearch(repo,proj,fnm);
      if (fnm1 != null) startSearch(repo,proj,fnm1);
    }

   private void startSearch(String repo,String proj,String q) {
      String cmd = "<BUBBLES DO='REBUSSEARCH'";
      cmd += " TEXT='" + q + "'";
      cmd += " REPO='" + repo + "'";
      cmd += " TYPE='FILE'";
      cmd += " PROJECT='" + proj + "'";
      cmd += " LANG='Rebase' />";

      BoardLog.logD("REBUS","Send Command: " + cmd);

      BoardSetup bs = BoardSetup.getSetup();
      MintControl mc = bs.getMintControl();
      MintDefaultReply mdr = new MintDefaultReply();
      mc.send(cmd,mdr,MintConstants.MINT_MSG_FIRST_NON_NULL);

      String rply = mdr.waitForString();
      System.err.println("REBUS SEARCH REPLY = " + rply);
   }

}	// end of inner class RebusFindItem



/********************************************************************************/
/*										*/
/*	Acceptance manager							*/
/*										*/
/********************************************************************************/

private static class Acceptor implements BassConstants.BassPopupHandler {

   @Override public void addButtons(BudaBubble bb,Point where,JPopupMenu menu,
	 String name,BassName forname) {
      int idx = name.indexOf(":");
      if (idx < 0 || idx >= name.length()-1) return;
      if (name.charAt(idx+1) != '.') {
	 name = name.substring(0,idx) + ":." + name.substring(idx+1);
       }

      AcceptanceFlagger af = getFactory().acceptance_flagger;
      boolean fg = af.getFlagForName(forname,name) != null;
      menu.add(new AcceptanceToggle(name,forname,fg));
    }

}	// end of inner class SearchExpander




private static class AcceptanceToggle extends AbstractAction {

   private String	for_name;
   private transient BassName	bass_name;

   private static final long serialVersionUID = 1;

   AcceptanceToggle(String nm,BassName bnm,boolean fg) {
      super(fg ? "Remove Acceptance" : "Accept Search Result");
      for_name = nm;
      bass_name = bnm;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      getFactory().acceptance_flagger.toggleAcceptance(for_name,bass_name);
    }
}


private static class AcceptanceFlagger implements BassFlagger {

   private Set<String>		   accepted_items;
   private Map<String,BassName>    accepted_names;
   private Map<String,BassFlag>    accepted_map;

   AcceptanceFlagger() {
      accepted_items = new HashSet<String>();
      accepted_names = new HashMap<String,BassName>();
      accepted_map = null;
    }

   @Override synchronized public BassFlag getFlagForName(BassName bnm,String nm) {
      if (accepted_map == null) computeAcceptedMap();
      if (!nm.contains(":.") && bnm != null) {
         nm = bnm.getProject() + ":." + nm;
       }
      return accepted_map.get(nm);
    }

   synchronized void toggleAcceptance(String name,BassName bnm) {
      if (accepted_map == null) computeAcceptedMap();
      // TODO: should notify back end

      if (accepted_map.containsKey(name)) {
	 for (Iterator<String> it = accepted_items.iterator(); it.hasNext(); ) {
	    String itm = it.next();
	    if (itm.startsWith(name)) {
	       noteAccept(itm,false);
	       it.remove();
	       accepted_names.remove(itm);
	     }
	  }
       }
      else {
	 accepted_items.add(name);
	 if (bnm != null) accepted_names.put(name,bnm);
	 noteAccept(name,true);
       }

      accepted_map = null;
    }


   private void computeAcceptedMap() {
      accepted_map = new HashMap<String,BassFlag>();
      for (String s : accepted_items) {
	 for ( ; ; ) {
	    accepted_map.put(s,accept_flag);
	    int idx = s.lastIndexOf(".");
	    if (idx < 0) break;
	    s = s.substring(0,idx);
	  }
       }
    }

   private void noteAccept(String nm,boolean fg) {
      BassName bnm = accepted_names.get(nm);
      BumpLocation loc = null;
      String proj = null;
      if (bnm != null) {
         proj = bnm.getProject();
         loc = bnm.getLocation();
       }
      else {
         int idx = nm.indexOf(":");
         if (idx > 0) {
            proj = nm.substring(0,idx);
            nm = nm.substring(idx+1);
            if (nm.startsWith(".")) nm = nm.substring(1);
            if (nm.length() > 0) {
               List<BumpLocation> locs = BumpClient.getBump().findClassDefinition(proj,nm);
               if (locs != null && locs.size() > 0) loc = locs.get(0);
             }
          }
       }
        
      String cmd = "<BUBBLES DO='REBUSACCEPT'";
      cmd += " PROJECT='" + proj + "'";
      cmd += " FLAG='" + Boolean.toString(fg) + "'";
      if (loc != null) {
         cmd += " FILE='" + loc.getFile().getAbsolutePath() + "'";
         cmd += " SPOS='" + loc.getOffset() + "'";
         cmd += " EPOS='" + loc.getEndOffset() + "'";
       }
      cmd += " LANG='Rebase' />";
   
      BoardSetup bs = BoardSetup.getSetup();
      MintControl mc = bs.getMintControl();
      mc.send(cmd);
   }
}	// end of inner class AcceptanceFlagger



private static class AcceptFlag implements BassFlag {

   private Icon overlay_icon;
   private int flag_priority;

   AcceptFlag(String icn,int pri) {
      overlay_icon = BoardImage.getIcon(icn);
      flag_priority = pri;
    }

   @Override public Icon getOverlayIcon()		{ return overlay_icon; }
   @Override public int getPriority()			{ return flag_priority; }

}	// end of inner class AcceptFlag



/********************************************************************************/
/*										*/
/*	Caching setup								*/
/*										*/
/********************************************************************************/

private static void setupCache()
{
   boolean use = false;
   BoardProperties bp = BoardProperties.getProperties("Rebus");
   String cachedir = bp.getProperty("Rebus.cache.directory");
   if (cachedir == null || cachedir.equals("")) use = false;
   else use = bp.getBoolean("Rebus.cache.use",true);

   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   String cmd = "<BUBBLES DO='REBUSCACHE' DIR='" + cachedir + "'";
   cmd += " USE='" + Boolean.toString(use) + "'";
   cmd += " LANG='Rebase' />";
   mc.send(cmd);
}




}	// end of class RebusFactory




/* end of RebusFactory.java */

