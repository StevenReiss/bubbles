/********************************************************************************/
/*                                                                              */
/*              BstyleConfigBubble.java                                         */
/*                                                                              */
/*      Simple bubble to let user specify checkstyle configuration file         */
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



package edu.brown.cs.bubbles.bstyle;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.ivy.swing.SwingComboBox;
import edu.brown.cs.ivy.swing.SwingGridPanel;


class BstyleConfigBubble extends BudaBubble implements BstyleConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Set<String>             project_set;
private Map<String,String>      name_map;
private SwingComboBox<String>   for_button;
private SwingComboBox<String>   use_button;
private Map<String,String>      dflt_map;


private static final String     NO_CHECKING = "No Checking";
private static final String     USE_DEFAULT = "Use Default";



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BstyleConfigBubble(Set<String> projects)
{
   project_set = projects;
   name_map = new HashMap<>();
   dflt_map = new HashMap<>();
   for_button = null;
   use_button = null;
   
   JPanel pnl = setupPanel();
   setContentPane(pnl);
}



/********************************************************************************/
/*                                                                              */
/*      Define the interior panel                                               */
/*                                                                              */
/********************************************************************************/

JPanel setupPanel()
{
   SwingGridPanel pnl = new SwingGridPanel();
   
   Set<String> files = new TreeSet<>();
   String dflt = NO_CHECKING;
   name_map.put(dflt,"*");
   files.add(NO_CHECKING);
   String usedflt = USE_DEFAULT;
   name_map.put(usedflt,"");
   
   files.add("sun_checks.xml");
   files.add("google_checks.xml");
   BoardProperties bp = BoardProperties.getProperties("Bstyle");
   for (String p : bp.stringPropertyNames()) {
      if (p.startsWith("Bstyle.config.file.")) {
         String val = getValueName(bp.getString(p));
         files.add(val);
         int idx = p.lastIndexOf(".");
         String pnm = p.substring(idx+1);
         if (project_set.contains(pnm)) dflt_map.put(pnm,val);
       }
      else if (p.equals("Bstyle.config.file")) {
         String val = getValueName(bp.getString(p));
         files.add(val);
         dflt = val;
       }
    }
   for (String s : project_set) {
      if (!dflt_map.containsKey(s)) dflt_map.put(s,USE_DEFAULT);
    }
   
   String p0 = null;
   List<String> foroptions = new ArrayList<>();
   for (String s : project_set) {
      String lbl = "Project " + s;
      foroptions.add(lbl);
      p0 = lbl;
      name_map.put(lbl,s);
    }
   if (project_set.size() > 1) {
      String lbl = "All Projects in Workspace";
      foroptions.add(lbl);
      name_map.put(lbl,"*ALL*");
    }
   String lbl = "Default (all workspaces)";
   foroptions.add(lbl);
   name_map.put(lbl,"*DFLT*");
   
   List<String> useoptions = new ArrayList<>();
   useoptions.addAll(files);
   // ADD OPTION TO USE DEFAULT
   
   pnl.beginLayout();
   pnl.addBannerLabel("Select CheckStyle Configuration Files");
   
   for_button = pnl.addChoice("For: ",foroptions,p0,
         new ProjectAction());
   String p1 = dflt_map.get(p0);
   if (p1 == null) p1 = dflt;
   use_button = pnl.addChoice("Use: ",useoptions,p1,
         new FileAction());
  
   pnl.addSeparator();
   pnl.addBottomButton("Add Configuration","CONFIG",
         new AddConfigAction(useoptions));
   pnl.addBottomButton("Apply","APPLY",new ApplyAction());
   pnl.addBottomButtons();
   
   return pnl;
}



private String getValueName(String val)
{
   if (val == null || val.equals("*") || val.isEmpty()) {
      return  NO_CHECKING;
    }
   
   String rslt = val;
   int idx = val.lastIndexOf(File.separator);
   if (idx < 0) {
      name_map.put(val,val);
    }
   else {
      rslt = val.substring(idx+1);
      name_map.put(rslt,val);
    }
   
   return rslt;
}




/********************************************************************************/
/*                                                                              */
/*      Action settings                                                         */
/*                                                                              */
/********************************************************************************/

private final class ProjectAction implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      String proj = (String) for_button.getSelectedItem();
      if (proj == null) return;
      String aproj = name_map.get(proj);
      String what = dflt_map.get(aproj);
      if (what != null) use_button.setSelectedItem(what);
    }

}       // end of inner class ProjectAction


private final class FileAction implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
    }

}       // end of inner class FileAction




private final class AddConfigAction implements ActionListener {
   
   private Set<String> use_options;
   
   AddConfigAction(List<String> opts) {
      use_options = new TreeSet<>(opts);
    }
   
   @Override public void actionPerformed(ActionEvent evt) {
      Component c = (Component) evt.getSource();
      JFileChooser chooser = new JFileChooser();
      chooser.setFileFilter(new ConfigFileFilter());
      int rval = chooser.showDialog(c,"Add Configuration File");
      if (rval == JFileChooser.APPROVE_OPTION) {
         File f = chooser.getSelectedFile();
         if (f == null) return;
         String fnm = f.getAbsolutePath();
         int idx = fnm.lastIndexOf(File.separator);
         String xnm = fnm.substring(idx+1);
         boolean add = name_map.containsKey(xnm);
         name_map.put(xnm,fnm);
         if (add) {
            use_options.add(xnm);
            use_button.setContents(use_options);
          }
       }
    }
   
}       // end of inner class AddConfigAction


private final class ConfigFileFilter extends FileFilter {
   
   @Override public String getDescription() {
      return "Checkstyle configuration file";
    }
   
   @Override public boolean accept(File f) {
      if (!f.canRead()) return false;
      String fp = f.getName();
      if (fp.endsWith(".xml")) {
         return true;
       }
      return false;
    }
   
}       // end of inner class ConfigFileFilter



private final class ApplyAction implements ActionListener {
   
   @Override public void actionPerformed(ActionEvent evt) {
      BoardProperties bp = BoardProperties.getProperties("Bstyle");
      String file = (String) use_button.getSelectedItem();
      String proj = (String) for_button.getSelectedItem();
      String afile = name_map.get(file);
      String aproj = name_map.get(proj);
      
      if (aproj.equals("*DFLT*")) {
         if (afile.isEmpty()) afile = "*";
         bp.setProperty("Bstyle.config.file",afile);
       }
      else if (aproj.equals("*ALL")) {
         for (String p : project_set) {
            setProjectValue(bp,p,afile);
          }
       }
      else {
         setProjectValue(bp,aproj,afile);
       }
      
      try {
         bp.save();
       }
      catch (IOException e) { }
      
      // should check and start bstyle server here
    }
   
   private void setProjectValue(BoardProperties bp,String proj,String val) {
      String pnm = "Bstyle.config.file." + proj;
      if (val.isEmpty()) {
         // use default
         bp.remove(pnm);
       }
      else {
         bp.setProperty(pnm,val);
       }
    }

}       // end of inner class ApplyAction



}       // end of class BstyleConfigBubble


/* end of BstyleConfigBubble.java */

