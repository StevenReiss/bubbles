/********************************************************************************/
/*                                                                              */
/*              BvcrAutoPush.java                                               */
/*                                                                              */
/*      Handle Automatic Push on Save                                           */
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



package edu.brown.cs.bubbles.bvcr;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;

class BvcrAutoCommit implements BvcrConstants, MintConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BvcrControlPanel control_panel;
private Set<String>      files_added;
private Set<String>      files_removed;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BvcrAutoCommit(BvcrControlPanel pnl)
{
   control_panel = pnl;
   control_panel.addUpdateListener(new UpdateManager());
   BumpClient.getBump().addChangeHandler(new FileChangeManager());
   BudaRoot.addFileHandler(new SaveManager());
   files_removed = null;
   files_added = null;
}



/********************************************************************************/
/*                                                                              */
/*      Auto update methods                                                     */
/*                                                                              */
/********************************************************************************/

private void startAutoUpdate()
{
   control_panel.waitForUpdate();
   
   // MUST BE GIT for now
   switch (control_panel.getVcrType()) {
      case "GIT" :
         break;
      default :
         return;
    }
   
   String proj = control_panel.getProject();
   BoardProperties bp = BoardProperties.getProperties("Bvcr");
   boolean fg = bp.getBoolean("Bvcr.auto.update");
   fg = bp.getBoolean("Bvcr.auto.update." + proj,fg);
   if (!fg) return;
   
   boolean needupdate = false;
   for (BvcrControlFileStatus sts : control_panel.getFiles()) {
      switch (sts.getFileState()) {
         case ADDED :
         case DELETED :
         case COPIED :
         case MODIFIED :
         case RENAMED :
            needupdate = true;
            break;
         default :
         case IGNORED :
         case UNTRACKED :
         case UNMERGED :
         case UNMODIFIED :
            break;
       }
    }
   if (files_added != null) {
      List<String> files = new ArrayList<>();
      for (String file : files_added) {
         BvcrControlFileStatus sts = control_panel.getFileMap().get(file);
         switch (sts.getFileState()) {
            default :
               break;
            case UNTRACKED :
               files.add(file);
               needupdate = true;
               break;
          }
       }
      fileCommand("FILEADD",files);
    }
   if (files_removed != null) {
      List<String> files = new ArrayList<>();
      for (String file : files_removed) {
         BvcrControlFileStatus sts = control_panel.getFileMap().get(file);
         switch (sts.getFileState()) {
            case ADDED :
            case MODIFIED :
            case UNMODIFIED :
            case UNMERGED :
            case COPIED :
            case RENAMED :
               files.add(file);
               needupdate = true;
               break;
            case UNTRACKED :
            case IGNORED :
            case DELETED :
               break;
          }
       }
      fileCommand("FILERM",files);
    }
   if (needupdate && control_panel.hasChanged()) doCommit();
   files_added = null;
   files_removed = null;
}



private void fileCommand(String cmd,List<String> files)
{
   if (files.isEmpty()) return;
   
   MintControl mc = BoardSetup.getSetup().getMintControl();
   MintDefaultReply rply = new MintDefaultReply();
   String xcmd = "<BVCR DO='" + cmd + "' PROJECT='" + control_panel.getProject() + "'>";
   for (String fn : files) {
      xcmd += "<FILE NAME='" + fn + "'/>";
    }
   xcmd += "</BVCR>";
   mc.send(xcmd,rply,MintConstants.MINT_MSG_FIRST_NON_NULL);
   String cnts = rply.waitForString();
   BoardLog.logD("BVCR","Reply from BVCR " + cmd + ": " + cnts);
}



private void doCommit()
{
   String msg = "Automatic edit commit at " + new Date().toString();
   control_panel.commitVersion(msg);
}


private void handleProjectUpdated()
{
   
}




/********************************************************************************/
/*										*/
/*	File Change manager							*/
/*										*/
/********************************************************************************/

private final class FileChangeManager implements BumpConstants.BumpChangeHandler {
   
   
   
   
   
   @Override public void handleFileAdded(String proj,String file) {
      if (proj.equals(control_panel.getProject())) {
         if (files_added == null) files_added = new HashSet<>();
         files_added.add(file);
       }
    }
   
   @Override public void handleFileRemoved(String proj,String file) {
      if (proj.equals(control_panel.getProject())) {
         if (files_added != null && files_added.contains(file)) {
            files_added.remove(file);
            if (files_added.size() == 0) files_added = null;
          }
         else {
            if (files_removed == null) files_removed = new HashSet<>();
            files_removed.add(file);
          }
       }
    }
   
}	// end of inner class FileChangeManager



private final class SaveManager implements BudaConstants.BudaFileHandler {
   
   @Override public void handleSaveRequest()                    { }
   
   @Override public void handleSaveDone() {
      startAutoUpdate();
    }
   
   @Override public void handleCommitRequest()                  { }
   
   @Override public void handleCheckpointRequest()              { }
   
   @Override public boolean handleQuitRequest()                 { return true; }
   
   @Override public void handlePropertyChange()                { }
   
}


private final class UpdateManager implements BvcrProjectUpdated {
   
   @Override public void projectUpdated(BvcrControlPanel pnl) {
      if (pnl == control_panel) {
         handleProjectUpdated();
       }
    }
}

}       // end of class BvcrAutoPush




/* end of BvcrAutoPush.java */

