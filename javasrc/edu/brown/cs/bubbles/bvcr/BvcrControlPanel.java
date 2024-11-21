/********************************************************************************/
/*										*/
/*		BvcrControlPanel.java						*/
/*										*/
/*	Control panel for version management					*/
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



package edu.brown.cs.bubbles.bvcr;

import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.swing.SwingEventListenerList;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;



class BvcrControlPanel implements BvcrConstants, MintConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private enum UpdateType { ABORT, UPDATE, UPDATE_KEEP, UPDATE_REMOVE };

private String	for_project;
private String	vcr_type;
private File	root_directory;
private Map<String,BvcrControlFileStatus> status_map;
private Map<String,BvcrControlVersion> version_map;
private int	update_count;
private SwingEventListenerList<BvcrProjectUpdated> update_listeners;
private boolean  do_ignored;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BvcrControlPanel(String proj,String typ,String dir)
{
   for_project = proj;
   vcr_type = typ;
   root_directory = new File(dir);
   version_map = null;
   status_map = null;
   BumpClient.getBump().addChangeHandler(new FileChangeManager());
   update_listeners = new SwingEventListenerList<>(BvcrProjectUpdated.class);
   do_ignored = false;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getProject()				{ return for_project; }

String getVcrType()				{ return vcr_type; }

Map<String,BvcrControlFileStatus>  getFileMap() { return status_map; }

Set<BvcrControlFileStatus> getFiles()
{
   if (status_map == null) waitForUpdate();
   if (status_map == null) return new HashSet<BvcrControlFileStatus>();
   return new HashSet<BvcrControlFileStatus>(status_map.values());
}

Map<String,BvcrControlVersion> getVersionMap()	{ return version_map; }

Set<BvcrControlVersion> getVersions()
{
   return new HashSet<BvcrControlVersion>(version_map.values());
}

File getRootDirectory() 			{ return root_directory; }

void setIncludeIgnored(boolean fg)
{
   if (do_ignored == fg) return ;
   do_ignored = fg;
   if (fg) startUpdate();
}



/********************************************************************************/
/*										*/
/*	Listener methods							*/
/*										*/
/********************************************************************************/

void addUpdateListener(BvcrProjectUpdated upd)
{
   startUpdate();
   update_listeners.add(upd);
}


void removeUpdateListener(BvcrProjectUpdated upd)
{
   update_listeners.remove(upd);
}



/********************************************************************************/
/*										*/
/*	Bubble creation methods 						*/
/*										*/
/********************************************************************************/

BudaBubble getControlBubble()
{
   ControlPanelBubble bbl = new ControlPanelBubble(this);

   return bbl;
}




/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

void startUpdate()
{
   synchronized (this) {
      if (update_count > 2) return;
      if (update_count++ == 0) {
	 Updater upd = new Updater();
	 BoardThreadPool.start(upd);
       }
    }
}



void waitForUpdate()
{
   synchronized (this) {
      while (update_count > 0) {
	 try {
	    wait();
	  }
	 catch (InterruptedException e) { }
       }
    }
}



void updateCompleted()
{
   synchronized (this) {
      notifyAll();
    }

   for (BvcrProjectUpdated upd : update_listeners) {
      upd.projectUpdated(this);
    }
}



private class Updater implements Runnable {

   Updater() {	}

   @Override public void run() {
      for ( ; ; ) {
	 BoardSetup bs = BoardSetup.getSetup();
	 MintControl mc = bs.getMintControl();
	 MintDefaultReply rply = new MintDefaultReply();
	 String cmd = "<BVCR DO='VERSIONS' PROJECT='" + for_project + "' />";
	 mc.send(cmd,rply,MINT_MSG_FIRST_NON_NULL);
	 Element ever = rply.waitForXml();
	 Map<String,BvcrControlVersion> newvers = new HashMap<String,BvcrControlVersion>();
	 for (Element e : IvyXml.children(ever,"VERSION")) {
	    BvcrControlVersion vv = new BvcrControlVersion(e);
	    String fnm = vv.getName();
	    newvers.put(fnm,vv);
	    for (String s : vv.getAlternativeNames()) newvers.put(s,vv);
	    for (String s : vv.getAlternativeIds()) newvers.put(s,vv);
	  }
	 version_map = newvers;

	 rply = new MintDefaultReply();
	 cmd = "<BVCR DO='CHANGES' PROJECT='" + for_project + "'";
	 if (do_ignored) cmd += " IGNORED='T'";
	 cmd += " />";

	 mc.send(cmd,rply,MINT_MSG_FIRST_NON_NULL);
	 Element efil = rply.waitForXml();
	 Map<String,BvcrControlFileStatus> newfiles = new HashMap<String,BvcrControlFileStatus>();
	 for (Element e : IvyXml.children(efil,"FILE")) {
	    BvcrControlFileStatus fsts = new BvcrControlFileStatus(e);
	    newfiles.put(fsts.getFileName(),fsts);
	  }
	 status_map = newfiles;

	 synchronized (BvcrControlPanel.this) {
	    if (--update_count <= 0) break;
	  }
       }
      updateCompleted();
    }
}



/********************************************************************************/
/*										*/
/*	Version changing methods						*/
/*										*/
/********************************************************************************/

void commitVersion(String msg)
{
   if (!hasChanged()) {
      JOptionPane.showMessageDialog(null,"Project is up-to-date");
      return;
    }

   if (msg == null) {
      msg = getCommitMessage();
      if (msg == null) return;
    }

   MintControl mc = BoardSetup.getSetup().getMintControl();
   MintDefaultReply rply = new MintDefaultReply();
   String cmd = "<BVCR DO='COMMIT' PROJECT='" + for_project + "' MESSAGE='" + msg + "' />";
   mc.send(cmd,rply,MINT_MSG_FIRST_NON_NULL);
   Element rslt = rply.waitForXml();
   if (rslt == null) {
        // do something here
    }
   // check statsus

   startUpdate();
}


private static String getCommitMessage()
{
   SwingGridPanel pnl = new SwingGridPanel();
   pnl.beginLayout();
   pnl.addBannerLabel("Commit");
   JTextArea area = pnl.addTextArea("Message",null,null);
   int fg = JOptionPane.showOptionDialog(null,pnl,"Commit Message",
	 JOptionPane.OK_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE,
	 null,null,null);
   if (fg != JOptionPane.OK_OPTION) return null;
   String txt = area.getText();
   if (txt == null) return null;
   txt = txt.trim();
   if (txt.length() == 0) return null;
   return txt;
}



void pushVersion()
{
   if (hasChanged()) {
      MintControl mc = BoardSetup.getSetup().getMintControl();
      MintDefaultReply rply = new MintDefaultReply();
      String cmd = "<BVCR DO='COMMIT' PROJECT='" + for_project + "' />";
      mc.send(cmd,rply,MINT_MSG_FIRST_NON_NULL);
      Element rslt = rply.waitForXml();
      if (rslt != null) { }
    }

   MintControl mc = BoardSetup.getSetup().getMintControl();
   MintDefaultReply rply = new MintDefaultReply();
   String cmd = "<BVCR DO='PUSH' PROJECT='" + for_project + "' />";
   mc.send(cmd,rply,MINT_MSG_FIRST_NON_NULL);
   Element rslt = rply.waitForXml();
   if (rslt != null) { }

   startUpdate();
}



void setVersion(BvcrControlVersion tover)
{
   if (tover == null) return;

   if (safeForUpdate("Set Version",false) == UpdateType.ABORT) return;

   MintControl mc = BoardSetup.getSetup().getMintControl();
   MintDefaultReply rply = new MintDefaultReply();
   String cmd = "<BVCR DO='SETVERSION' PROJECT='" + for_project + "' VERSION='"  + tover + "' />";
   mc.send(cmd,rply,MINT_MSG_FIRST_NON_NULL);
   Element rslt = rply.waitForXml();
   if (IvyXml.isElement(rslt,"ERROR")) {
      // handle error here
    }

   startUpdate();

   BumpClient bc = BumpClient.getBump();
   bc.compile(false,false,true);
}


private UpdateType safeForUpdate(String what,boolean allowuse)
{
   if (!hasChanged()) return UpdateType.UPDATE;

   String [] choices = new String [] { "Discard Changes", "Cancel " + what, "Stash Changes" };
   if (allowuse) {
      choices = new String [] { "Discard Changes", "Keep Changes", "Cancel " + what, "Stash Changes" };
    }
   // need to add options for update: Use Changes
   Object o = JOptionPane.showInputDialog(null,"Current source changed and not saved",
	 "Confirm " + what,JOptionPane.ERROR_MESSAGE,
	 null,choices,"Cancel Set Version");
   if (o == null) return UpdateType.ABORT;
   if (o.equals("Cancel " + what)) return UpdateType.ABORT;
   else if (o.equals("Stash Changes")) {
      String s = JOptionPane.showInputDialog(null,"Message for Stashing Current Changes");
      if (s == null) return UpdateType.ABORT;
      MintControl mc = BoardSetup.getSetup().getMintControl();
      MintDefaultReply rply = new MintDefaultReply();
      String cmd = "<BVCR DO='STASH' PROJECT='" + for_project + "' MESSAGE='" + s + "' />";
      mc.send(cmd,rply,MINT_MSG_FIRST_NON_NULL);
      Element rslt = rply.waitForXml();
      if (rslt == null) return UpdateType.ABORT;
      if (IvyXml.isElement(rslt,"RESULT")) return UpdateType.UPDATE;
      return UpdateType.ABORT;
    }
   else if (o.equals("Discard Changes")) return UpdateType.UPDATE_REMOVE;
   else if (o.equals("Keep Changes")) return UpdateType.UPDATE_KEEP;
   return UpdateType.ABORT;
}



void updateVersion()
{
   UpdateType typ = safeForUpdate("Update Version",true);
   if (typ == UpdateType.ABORT) return;

   MintControl mc = BoardSetup.getSetup().getMintControl();
   MintDefaultReply rply = new MintDefaultReply();
   String cmd = "<BVCR DO='UPDATE' PROJECT='" + for_project + "'";
   switch (typ) {
      case ABORT :
	 return;
      case UPDATE :
	 break;
      case UPDATE_KEEP :
	 cmd += " KEEP='T'";
	 break;
      case UPDATE_REMOVE :
	 cmd += " DISCARD='T'";
	 break;
    }
   cmd += "/>";
   mc.send(cmd,rply,MINT_MSG_FIRST_NON_NULL);
   Element rslt = rply.waitForXml();
   if (IvyXml.isElement(rslt,"ERROR")) {
      // check whether to use original or new code and redo
    }

   startUpdate();

   BumpClient bc = BumpClient.getBump();
   bc.compile(false,false,true);
}



void newVersion()
{
   // get optional name for the new version
   // need a back end command
}



boolean hasChanged()
{
   waitForUpdate();

   Collection<BvcrControlFileStatus> files = getFiles();
   for (BvcrControlFileStatus file : files) {
      switch (file.getFileState()) {
	 case ADDED :
	 case COPIED :
	 case DELETED :
	 case MODIFIED :
	 case RENAMED :
	    return true;
	 default :
	    break;
       }
    }

   return false;
}




/********************************************************************************/
/*										*/
/*	Class containing the control bubble					*/
/*										*/
/********************************************************************************/

private static class ControlPanelBubble extends BudaBubble implements BvcrProjectUpdated,
		BudaBubble.BudaBubbleOutputer
{
   private BvcrControlPanel control_panel;
   private JButton version_button;
   private JLabel version_label;
   private JLabel files_label;
   private static final long serialVersionUID = 1;
   
   ControlPanelBubble(BvcrControlPanel ctrl) {
      control_panel = ctrl;
      JPanel pnl = getBubblePanel(ctrl);
      setContentPane(pnl);
      ctrl.addUpdateListener(this);
    }

   @Override protected void localDispose() {
      control_panel.removeUpdateListener(this);
    }

   @Override public void projectUpdated(BvcrControlPanel ctrl) {
      String id = "HEAD";
      version_button.setEnabled(false);
      if (ctrl.getVersionMap() != null) {
         version_button.setEnabled(true);
         BvcrControlVersion vv = ctrl.getVersionMap().get("HEAD");
         if (vv != null) {
            id = vv.getName();
            for (String s : vv.getAlternativeIds()) {
               if (s.length() < id.length()) id = s;
            }
            boolean havename = false;
            for (String  s : vv.getAlternativeNames()) {
               if (!s.equals("HEAD")) {
                  if (!havename || s.length() < id.length()) {
                     id = s;
                     havename = true;
                   }
                }
             }
          }
       }
      version_label.setText(id);
   
      int mct = 0;
      int dct = 0;
      int nct = 0;
      int uct = 0;
      if (ctrl.getFileMap() != null) {
         for (BvcrControlFileStatus fsts : ctrl.getFileMap().values()) {
            switch (fsts.getFileState()) {
               case ADDED :
        	  nct++;
        	  break;
               case COPIED :
               case MODIFIED :
        	  mct++;
        	  break;
               case DELETED :
        	  dct++;
        	  break;
               case UNTRACKED :
        	  uct++;
        	  break;
               default :
        	  break;
             }
          }
       }
      String cts = "" + mct + "/" + nct + "/" + dct + "/" + uct;
      files_label.setText(cts);
    }

   @Override public String getConfigurator()		   { return "BVCR"; }

   @Override public void outputXml(BudaXmlWriter xw) {
      xw.field("TYPE","CONTROL");
      xw.field("PROJECT",control_panel.getProject());
    }

   @Override public void handlePopupMenu(MouseEvent e) {
      JPopupMenu menu = new JPopupMenu();
      menu.add(getFloatBubbleAction());
      menu.show(this,e.getX(),e.getY());
    }

   private JPanel getBubblePanel(BvcrControlPanel ctrl) {
      SwingGridPanel pnl = new SwingGridPanel();
      JLabel ttl = new JLabel(ctrl.getVcrType() + " Control for " + ctrl.getProject(),
            SwingConstants.CENTER);
      Font ft = ttl.getFont();
      ft = ft.deriveFont(ft.getSize2D()+4f);
      ft = ft.deriveFont(Font.BOLD);
      ttl.setFont(ft);
      int yct = 0;
      pnl.addGBComponent(ttl,0,yct++,0,1,0,0);
   
      JLabel l1 = new JLabel("Version:");
      JLabel l2 = new JLabel("HEAD");
      version_label = l2;
      l2.setFont(l2.getFont().deriveFont(Font.BOLD));
      JButton b3 = new JButton("View");
      version_button = b3;
      b3.setEnabled(false);
      b3.addActionListener(new VersionPanelAction(ctrl));
      pnl.addGBComponent(l1,0,yct,1,1,0,0);
      pnl.addGBComponent(l2,1,yct,1,1,10,0);
      pnl.addGBComponent(b3,2,yct,1,1,0,0);
      ++yct;
   
      l1 = new JLabel("Files (M/N/D/U):");
      l2 = new JLabel("0/0/0");
      files_label = l2;
      l2.setFont(l2.getFont().deriveFont(Font.BOLD));
      b3 = new JButton("View");
      b3.addActionListener(new FilePanelAction(ctrl));
      pnl.addGBComponent(l1,0,yct,1,1,0,0);
      pnl.addGBComponent(l2,1,yct,1,1,10,0);
      pnl.addGBComponent(b3,2,yct,1,1,0,0);
      ++yct;
   
      JSeparator s1 = new JSeparator();
      pnl.addGBComponent(s1,0,yct++,0,1,10,0);
   
      JButton b1 = new JButton("Commit");
      b1.addActionListener(new CommitAction(ctrl));
      JButton b2 = new JButton("Push");
      b2.addActionListener(new PushAction(ctrl));
      b3 = new JButton("Update");
      b3.addActionListener(new UpdateAction(ctrl));
      pnl.addGBComponent(b1,0,yct,1,1,0,0);
      pnl.addGBComponent(b2,1,yct,1,1,10,0);
      pnl.addGBComponent(b3,2,yct,1,1,0,0);
      ++yct;
   
      return pnl;
    }

}	// end of inner class ControlPanelBubble




/********************************************************************************/
/*										*/
/*	File Change manager							*/
/*										*/
/********************************************************************************/

private class FileChangeManager implements BumpConstants.BumpChangeHandler {

   
   

   @Override public void handleFileChanged(String proj,String file) {
      startUpdate();
    }
   @Override public void handleFileAdded(String proj,String file) {
      startUpdate();
    }
   @Override public void handleFileRemoved(String proj,String file) {
      startUpdate();
    }

}	// end of inner class FileChangeManager



/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

private static class VersionPanelAction extends AbstractAction {

   private BvcrControlPanel control_panel;
   private static final long serialVersionUID = 1;
   
   VersionPanelAction(BvcrControlPanel cpnl) {
      control_panel = cpnl;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      Component btn = (Component) evt.getSource();
      BvcrControlVersionPanel pnl = new BvcrControlVersionPanel(control_panel);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(btn);
      BudaBubble bbl = BudaRoot.findBudaBubble(btn);
      if (bba == null) return;
      bba.addBubble(pnl,bbl,null,BudaConstants.PLACEMENT_LOGICAL);
    }

}	// end of inner class VersionPanelAction




private static class FilePanelAction extends AbstractAction {

   private BvcrControlPanel control_panel;
   private static final long serialVersionUID = 1;
   
   FilePanelAction(BvcrControlPanel cpnl) {
      control_panel = cpnl;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      Component btn = (Component) evt.getSource();
      BvcrControlFilePanel pnl = new BvcrControlFilePanel(control_panel);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(btn);
      BudaBubble bbl = BudaRoot.findBudaBubble(btn);
      if (bba == null) return;
      bba.addBubble(pnl,bbl,null,BudaConstants.PLACEMENT_LOGICAL);
    }

}	// end of inner class FilePanelAction


private static class CommitAction extends AbstractAction {

   private BvcrControlPanel control_panel;
   private static final long serialVersionUID = 1;
   
   CommitAction(BvcrControlPanel pnl) {
      control_panel = pnl;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      String msg = getCommitMessage();
      if (msg == null) return;
      control_panel.commitVersion(msg);
    }

}	//end of inner class CommitAction



private static class PushAction extends AbstractAction {

   private BvcrControlPanel control_panel;
   private static final long serialVersionUID = 1;
   
   PushAction(BvcrControlPanel pnl) {
      control_panel = pnl;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      control_panel.pushVersion();
    }

}	//end of inner class PushAction




private static class UpdateAction extends AbstractAction {

   private BvcrControlPanel control_panel;
   private static final long serialVersionUID = 1;
   
   UpdateAction(BvcrControlPanel pnl) {
      control_panel = pnl;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      control_panel.updateVersion();
    }

}	//end of inner class UpdateAction





}	// end of class BvcrControlPanel




/* end of BvcrControlPanel.java */

