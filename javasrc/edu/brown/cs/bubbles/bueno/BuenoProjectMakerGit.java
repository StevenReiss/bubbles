/********************************************************************************/
/*                                                                              */
/*              BuenoProjectMakerGit.java                                       */
/*                                                                              */
/*      Create new project by cloning a git repo                                */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
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

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bueno;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardProperties;

import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.swing.SwingGridPanel;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;



class BuenoProjectMakerGit extends BuenoProjectMakerBase
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static final String     GIT_URL = "GitUrl";
private static final String     GIT_URL_FIELD = "GitUrlField";
private static final String     GIT_DIR = "GitDir";
private static final String     GIT_DIR_FIELD = "GitDirField";

private static final Pattern    SSH_PAT = Pattern.compile("\\w+@\\w+(\\.\\w+)*\\:\\w+(\\/\\w+)*");




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BuenoProjectMakerGit()
{
   BuenoProjectCreator.addProjectMaker(this);
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String getLabel()
{
   return "Project From GIT Repo";
}


@Override public boolean checkStatus(BuenoProjectProps props)
{
   String url = props.getString(GIT_URL);
   if (url == null || url.length() == 0) return false;
   if (url.contains("@")) {
      if (!SSH_PAT.matcher(url).matches()) return false;
   }
   else if (url.startsWith("/")) {
      File f = new File(url);
      if (!f.exists() || !f.isDirectory() || !url.endsWith(".git")) return false;
   }
   else if (url.contains(":")) {
      try {
         new URL(url);
      }
      catch (MalformedURLException e) { 
         return false;
      }
   }
   else return false;
   
   File dir = props.getFile(GIT_DIR);
   if (dir == null) return false;
   File par = dir.getParentFile();
   if (par == null) return false;
   if (!par.exists() || !par.isDirectory() || !par.canWrite()) return false;
   
   return true;
}



/********************************************************************************/
/*                                                                              */
/*      Interaction methods                                                     */
/*                                                                              */
/********************************************************************************/

@Override public JPanel createPanel(BuenoProjectCreationControl ctrl,BuenoProjectProps props)
{
   GitActions uact = new GitActions(ctrl,props);
   
   SwingGridPanel pnl = new SwingGridPanel();
   pnl.beginLayout();
   JTextField urlfld = pnl.addTextField("GIT Repo URL",props.getString(GIT_URL),48,uact,uact);
   props.put(GIT_URL_FIELD,urlfld);
   JTextField dirfld = pnl.addFileField("Target Directory",props.getString(GIT_DIR),32,uact,uact);
   props.put(GIT_DIR_FIELD,dirfld);
   pnl.addSeparator();
   
   return pnl;
}



@Override public void resetPanel(BuenoProjectProps props)
{
   JTextField urlfld = (JTextField) props.get(GIT_URL_FIELD);
   if (urlfld != null) {
      urlfld.setText(props.getString(GIT_URL));
    }
   JTextField dirfld = (JTextField) props.get(GIT_DIR_FIELD);
   if (dirfld != null) {
      dirfld.setText(props.getString(GIT_DIR));
    }
}



private class GitActions implements ActionListener, UndoableEditListener {
   
   private BuenoProjectCreationControl project_control;
   private BuenoProjectProps project_props;
   
   GitActions(BuenoProjectCreationControl ctrl,BuenoProjectProps props) {
      project_control = ctrl;
      project_props = props;
    }
   
   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd.equals("GIT Repo URL")) {
         JTextField tfld =(JTextField) evt.getSource();
	 project_props.put(GIT_URL,tfld.getText());
       }
      else if (cmd.equals("Target Directory")) {
         JTextField tfld =(JTextField) evt.getSource();
         project_props.put(GIT_DIR,new File(tfld.getText()));
       }
      project_control.checkStatus();
    }
   
   @Override public void undoableEditHappened(UndoableEditEvent evt) {
      JTextField tfld = (JTextField) project_props.get(GIT_URL_FIELD);
      JTextField dfld = (JTextField) project_props.get(GIT_DIR_FIELD);
      if (tfld != null && tfld.getDocument() == evt.getSource()) {
         project_props.put(GIT_URL,tfld.getText());
         project_control.checkStatus();
       }
      else if (dfld != null && dfld.getDocument() == evt.getSource()) {
         project_props.put(GIT_DIR,new File(dfld.getText()));
         project_control.checkStatus();
       }
    }
   
}	// end of inner class GitActions



/********************************************************************************/
/*                                                                              */
/*      Project Setup methods                                                   */
/*                                                                              */
/********************************************************************************/

@Override public boolean setupProject(BuenoProjectCreationControl ctrl,BuenoProjectProps props)
{
   File dir = props.getFile(GIT_DIR);
   if (dir == null) {
      File pdir = props.getFile(PROJ_PROP_DIRECTORY);
      dir = new File(pdir,"base");
    }
   if (!dir.exists() && !dir.mkdir()) {
      JOptionPane.showMessageDialog(null,"Can't create base directory for git clone " + dir);
      return false;
    }
   if (!dir.isDirectory()) {
      JOptionPane.showMessageDialog(null,"Base directory for git clone not a directory: " + dir);
      return false;
    }
   BoardProperties vcrprop = BoardProperties.getProperties("Bvcr");
   String gitcmd = vcrprop.getProperty("bvcr.git.command","git");
   String giturl = props.getString(GIT_URL);
   String cmd = gitcmd + " clone -q " + giturl + " " + dir;
   try {
      BoardLog.logD("BUENO","Issue GIT command: " + cmd);
      IvyExec ex = new IvyExec(cmd,dir,IvyExec.IGNORE_OUTPUT);
      int sts = ex.waitFor();
      if (sts > 0) {
         JOptionPane.showMessageDialog(null,"Problem with GIT clone operation");
         return false;
       }
    }
   catch (IOException e) {
      JOptionPane.showMessageDialog(null,"Problem with GIT clone command");
      return false;
    }    
   
   return defineProject(ctrl,props,dir);
}




}       // end of class BuenoProjectMakerGit




/* end of BuenoProjectMakerGit.java */

