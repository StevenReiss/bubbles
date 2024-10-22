/********************************************************************************/
/*                                                                              */
/*              BmvnModel.java                                                  */
/*                                                                              */
/*      Abstract library tool model                                             */
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



package edu.brown.cs.bubbles.bmvn;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.SwingUtilities;

import edu.brown.cs.bubbles.beam.BeamFactory;
import edu.brown.cs.bubbles.beam.BeamNoteBubble;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.ivy.exec.IvyExec;

abstract class BmvnModel implements BmvnConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BmvnProject     for_project;
private File            basis_file;
private String          model_name;
private long            last_modified;
private boolean         is_root;

protected enum ExecMode {
   NO_OUTPUT,
   USE_STDOUT,
   USE_STDERR,
   USE_STDOUT_STDERR,
}


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected BmvnModel(BmvnProject proj,File basis,BmvnTool tool)
{
   for_project = proj;
   basis_file = basis;
   last_modified = basis.lastModified();
   model_name = tool.getToolName();
   
   is_root = true;
   File basedir = basis.getParentFile();
   for (File dir = basedir.getParentFile(); dir != null; dir = dir.getParentFile()) {
      if (!dir.canWrite()) break;
      File f1 = new File(dir,tool.getFileName());
      if (f1.exists() && f1.canWrite()) is_root = false;
    }
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

protected BmvnProject getProject()              { return for_project; }
protected File getFile()                        { return basis_file; }
protected String getName()                      { return model_name; }

protected String getLabel()     
{
   String lbl = model_name;
   if (!is_root) lbl += " in " + getFile().getParentFile().getName(); 
   return lbl; 
}

protected boolean isRoot()                      { return is_root; }

protected boolean isRelevant(String name)       
{
   String path = basis_file.getPath();
   String n1 = name.replace(".",File.separator);
   if (path.contains(n1)) return true;
   if (!is_root) return false;
   int idx = name.lastIndexOf(".");
   if (idx < 0) return true;
   String n2 = name.substring(0,idx);
   n2 = n2.replace(".",File.separator);
   if (path.contains(n2)) return false;
   return true;
}


protected boolean needsUpdate()
{
   long now = basis_file.lastModified();
   if (now <= last_modified) return false;
   last_modified = now;
   return true;
}



/********************************************************************************/
/*                                                                              */
/*      Work methods                                                            */
/*                                                                              */
/********************************************************************************/

boolean canCheckLibraries()                     { return false; }
void doCheckLibraries(BudaBubble b,Point w)     { }

boolean canUpdateLibraries()                    { return false; }
void doUpdateLibraries(BudaBubble b,Point w)    { }

List<BmvnCommand> getCommands(String name,BudaBubble b,Point w) 
{ 
   return new ArrayList<>(); 
}

void executeCommand(BmvnCommand cmd)            { }

boolean canAddLibrary()                         { return false; }
void doAddLibrary(BudaBubble rel,Point where)   { }

boolean canRemoveLibrary()                      { return false; }
void doRemoveLibrary(BudaBubble b,Point w)      { }
   



/********************************************************************************/
/*                                                                              */
/*      Buttons for model                                                       */
/*                                                                              */
/********************************************************************************/

void addButtons(String name,BudaBubble relbbl,Point where,JMenu menu)
{
   if (canAddLibrary()) {
      menu.add(new AddLibraryAction(relbbl,where));
    }
   if (canRemoveLibrary()) {
      menu.add(new RemoveLibraryAction(relbbl,where));
    }
   if (canCheckLibraries()) {
      menu.add(new CheckLibraryAction(relbbl,where));
    }
   if (canUpdateLibraries()) {
      menu.add(new UpdateLibraryAction(relbbl,where));
    }
   
   List<BmvnCommand> cmds = getCommands(name,relbbl,where);
   if (cmds != null && !cmds.isEmpty()) {
      String lbl = "Execute " + getLabel() + " ...";
      JMenu menu2 = new JMenu(lbl);
      for (BmvnCommand cmd : cmds) {
         cmd.putValue(Action.NAME,cmd.getName());
         menu2.add(cmd);
       }
      menu.add(menu2);
    }
}







/********************************************************************************/
/*                                                                              */
/*      Actions                                                                 */
/*                                                                              */
/********************************************************************************/

private class CheckLibraryAction extends AbstractAction {
   
   private BudaBubble relative_bubble;
   private Point relative_point;
   
   private static final long serialVersionUID = 1;
   
   CheckLibraryAction(BudaBubble relbbl,Point where) {
      super("Check " + model_name + " Library Class Path");
      relative_bubble = relbbl;
      relative_point = where;
    }
   
   @Override public void actionPerformed(ActionEvent evt) {
      doCheckLibraries(relative_bubble,relative_point);
    }
  
}       // end of inner class CheckLibraryAction



private class UpdateLibraryAction extends AbstractAction {

   private BudaBubble relative_bubble;
   private Point relative_point;
   
   private static final long serialVersionUID = 1;
   
   UpdateLibraryAction(BudaBubble relbbl,Point where) {
      super("Update " + model_name + " Library Versions");
      relative_bubble = relbbl;
      relative_point = where;
    }
   
   @Override public void actionPerformed(ActionEvent evt) {
      doUpdateLibraries(relative_bubble,relative_point);
    }
   
}       // end of inner class UpdateLibraryAction




private class AddLibraryAction extends AbstractAction {

   private BudaBubble relative_bubble;
   private Point relative_point;
   
   private static final long serialVersionUID = 1;
   
   AddLibraryAction(BudaBubble relbbl,Point where) {
      super("Add " + model_name + " Library");
      relative_bubble = relbbl;
      relative_point = where;
    }
   
   @Override public void actionPerformed(ActionEvent evt) {
      doAddLibrary(relative_bubble,relative_point);
    }

}       // end of inner class AddLibraryAction



private class RemoveLibraryAction extends AbstractAction {
   
   private BudaBubble relative_bubble;
   private Point relative_point;
   
   private static final long serialVersionUID = 1;
   
   RemoveLibraryAction(BudaBubble relbbl,Point where) {
      super("Remove " + model_name + " Library");
      relative_bubble = relbbl;
      relative_point = where;
    }
   
   @Override public void actionPerformed(ActionEvent evt) {
      doRemoveLibrary(relative_bubble,relative_point);
    }
   
}       // end of inner class RemoveLibraryAction



/********************************************************************************/
/*                                                                              */
/*      Command utilities                                                       */
/*                                                                              */
/********************************************************************************/

protected void runCommand(String cmd,File wd,ExecMode mode,
      BudaBubble relbbl,Point relpt)
{
   CommandRunner cr = new CommandRunner(cmd,wd,mode,relbbl,relpt);
   BoardThreadPool.start(cr);
}



private class CommandRunner implements Runnable {
   
   private String run_command;
   private File working_directory;
   private ExecMode exec_mode;
   private BudaBubble relative_bubble;
   private Point relative_point;
   private String show_result;
   
   CommandRunner(String cmd,File wd,ExecMode mode,BudaBubble bb,Point pt) {
      run_command = cmd;
      working_directory = wd;
      exec_mode = mode;
      relative_bubble = bb;
      relative_point = pt;
      show_result = null;
    }
   
   @Override public void run() {
      if (show_result != null) {
         createResultBubble();
       }
      else {
         String rslt = executeCommand();
         if (rslt != null && !rslt.trim().isEmpty()) {
            show_result = rslt;
            SwingUtilities.invokeLater(this);
          }
       }
    }
   
   protected String executeCommand() {
      String rslt = null;
      int fgs = 0;
      switch (exec_mode) {
         case NO_OUTPUT :
            fgs = IvyExec.IGNORE_OUTPUT;
            break;
         case USE_STDERR :
            fgs = IvyExec.READ_ERROR;
            break;
         case USE_STDOUT :
            fgs = IvyExec.READ_OUTPUT;
            break;
         case USE_STDOUT_STDERR :
            fgs = IvyExec.ERROR_OUTPUT | IvyExec.READ_OUTPUT;
            break;
       }
      try {
         IvyExec exec = new IvyExec(run_command,working_directory,fgs);
         Reader rdr = null;
         switch (exec_mode) {
            case NO_OUTPUT :
               break;
            case USE_STDERR :
               rdr = new InputStreamReader(exec.getErrorStream());
               break;
            case USE_STDOUT :
            case USE_STDOUT_STDERR :
               rdr = new InputStreamReader(exec.getInputStream());
               break;
          } 
         if (rdr != null) {
            StringBuffer buf = new StringBuffer();
            BufferedReader br = new BufferedReader(rdr);
            for ( ; ; ) {
               String ln = br.readLine();
               if (ln == null) break;
               ln = filterOutputLine(ln);
               if (ln != null) {
                  buf.append(ln);
                  buf.append("\n");
                }
             }
            rslt = buf.toString();
          }
         int sts = exec.waitFor();
         rslt = filterCommandOutput(sts,rslt);
         if (rslt == null) return null;
       }
      catch (IOException e) {
         rslt = "Error executing " + run_command;
       }
      return rslt;
    }
   
   private void createResultBubble() {
      if (show_result == null || show_result.trim().isEmpty()) return;
      BeamFactory bf = BeamFactory.getFactory();
      BeamNoteBubble bb = bf.createNoteBubble(show_result);
      if (bb == null) return;
      bb.setEditable(false);
      bb.setNoteColor(BoardColors.getColor("Bmvn.status.top"),
            BoardColors.getColor("Bmvn.status.bottom"));
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(relative_bubble);
      if (bba != null) {
         bba.addBubble(bb,relative_bubble,relative_point,
               BudaConstants.PLACEMENT_LOGICAL|BudaConstants.PLACEMENT_MOVETO);
       }
    }
   
}       // end of inner class CommandRunner




protected String filterOutputLine(String line)
{
   return line;
}


protected String filterCommandOutput(int sts,String cnts)
{
   if (sts == 0 || cnts == null) return null;
   return cnts;
}



}       // end of class BmvnModel




/* end of BmvnModel.java */

