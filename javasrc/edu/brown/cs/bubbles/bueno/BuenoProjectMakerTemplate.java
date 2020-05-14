/********************************************************************************/
/*                                                                              */
/*              BuenoProjectMakerTemplate.java                                  */
/*                                                                              */
/*      Create a new project by duplicating sources                             */
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

import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.swing.SwingGridPanel;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;




class BuenoProjectMakerTemplate extends BuenoProjectMakerBase
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static final String SRC_DIR = PROJ_PROP_BASE;
private static final String SRC_FIELD = "TemplateField";
   
   
   
/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BuenoProjectMakerTemplate()
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
   return "Create Project Copying Existing Source";
}



@Override public boolean checkStatus(BuenoProjectProps props)
{
   File sdir = props.getFile(SRC_DIR);
   if (sdir == null) return false;
   if (!sdir.exists()) return false;
   return true;
}




/********************************************************************************/
/*                                                                              */
/*      Interaction methods                                                     */
/*                                                                              */
/********************************************************************************/

@Override public JPanel createPanel(BuenoProjectCreationControl ctrl,BuenoProjectProps props)
{
   NewActions cact = new NewActions(ctrl,props);
   
   SwingGridPanel pnl = new SwingGridPanel();
   pnl.beginLayout();
   JTextField srcfld = pnl.addFileField("Template Directory",props.getFile(SRC_DIR),
         JFileChooser.DIRECTORIES_ONLY,cact,cact);   
   props.put(SRC_FIELD,srcfld);
   pnl.addSeparator();
   
   return pnl;
}   


@Override public void resetPanel(BuenoProjectProps props)
{
   JTextField srcfld = (JTextField) props.get(SRC_FIELD);
   File sdir = props.getFile(SRC_DIR);
   if (srcfld != null && sdir != null) {
      srcfld.setText(sdir.getPath());
    }
}



private class NewActions implements ActionListener, UndoableEditListener {
   
   private BuenoProjectCreationControl project_control;
   private BuenoProjectProps project_props;
   
   NewActions(BuenoProjectCreationControl ctrl,BuenoProjectProps props) {
      project_control = ctrl;
      project_props = props;
    }
   
   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd.equals("Template Directory")) {
         JTextField tfld = (JTextField) evt.getSource();
         project_props.put(SRC_DIR,new File(tfld.getText()));
       }
      project_control.checkStatus();
    }
   
   @Override public void undoableEditHappened(UndoableEditEvent evt) {
      JTextField tfld = (JTextField) project_props.get(SRC_FIELD);
      if (tfld != null && tfld.getDocument() == evt.getSource()) {
	 project_props.put(SRC_DIR,new File(tfld.getText()));
	 project_control.checkStatus();
       }
    }
   
}       // end of inner class NewActions


/********************************************************************************/
/*                                                                              */
/*      Project Creation methods                                                */
/*                                                                              */
/********************************************************************************/

@Override public boolean setupProject(BuenoProjectCreationControl ctrl,BuenoProjectProps props)
{
   File dir = props.getFile(SRC_DIR);
   Set<File> srcs = new HashSet<File>();
   Set<File> libs = new HashSet<File>();
   Set<File> resources = new HashSet<>();
   
   findFiles(dir,srcs,libs,resources);
   Map<String,List<File>> roots = new HashMap<String,List<File>>();
   for (File sf : srcs) {
      sf = sf.getAbsoluteFile();
      String pkg = ctrl.getPackageName(sf);
      if (pkg == null) continue;
      File par = sf.getParentFile();
      String [] ps = pkg.split("\\.");
      for (int i = ps.length-1; par != null && i >= 0; --i) {
         if (!par.getName().equals(ps[i])) par = null;
         else par = par.getParentFile();
       }
      if (par != null) {
         List<File> lf = roots.get(pkg);
         if (lf == null) {
            lf = new ArrayList<File>();
            roots.put(pkg,lf);
          }
         lf.add(sf);
       }
    }
   
   File pdir = props.getFile(PROJ_PROP_DIRECTORY);
   File sdir = new File(pdir,"src");
   props.getSources().clear();
   props.getSources().add(sdir);
   
   for (Map.Entry<String,List<File>> ent : roots.entrySet()) {
      String pkg = ent.getKey();
      File tdir = sdir;
      String [] ps = pkg.split("\\.");
      for (int i = 0; i < ps.length; ++i) {
         tdir = new File(tdir,ps[i]);
       }
      if (!tdir.exists() && !tdir.mkdirs()) {
         BoardLog.logX("BUENO","Unable to create source directory " + tdir);
         return false;
      }
      for (File f : ent.getValue()) {
         File f1 = new File(tdir,f.getName());
         try {
            IvyFile.copyFile(f,f1);
          }
         catch (IOException e) {
            BoardLog.logE("BUENO","Problem copying source files",e);
          }
       }
    }
      
   if (libs.size() > 0) {
      File ldir = new File(pdir,"lib");
      if (!ldir.mkdirs()) return false;
      for (File lf : libs) {
         File lf1 = new File(ldir,lf.getName());
         if (!lf1.exists()) {
            try {
               IvyFile.copyFile(lf,lf1);
             }
            catch (IOException e) {
               BoardLog.logE("BUENO","Problem copying source files",e);
             }
            props.getLibraries().add(lf1);
          }
       }
    }
   
   if (resources.size() > 0) {
      File rdir = new File(pdir,"resources");
      if (!rdir.mkdirs()) return false;
      for (File rf : resources) {
         try {
            IvyFile.copyHierarchy(rf,rdir);
          }
         catch (IOException e) {
            BoardLog.logE("BUENO","Problem copying source files",e);
          }
       }
    }
   
   return true;
}











}       // end of class BuenoProjectMakerTemplate




/* end of BuenoProjectMakerTemplate.java */

