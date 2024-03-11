/********************************************************************************/
/*										*/
/*		BuenoGenericEditorPanel.java					*/
/*										*/
/*	description of class							*/
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



package edu.brown.cs.bubbles.bueno;

import java.awt.Component;
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
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

import org.w3c.dom.Element;

import edu.brown.cs.bubbles.board.BoardFileSystemView;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.swing.SwingListPanel;
import edu.brown.cs.ivy.swing.SwingListSet;
import edu.brown.cs.ivy.xml.IvyXml;

class BuenoGenericEditorPanel implements BuenoConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BuenoGenericProjectEditor project_editor;
private EditPanel edit_panel;
private Element tab_xml;
private SwingListSet<BuenoPathEntry> panel_paths;
private Set<BuenoPathEntry> base_paths;
private boolean force_update;


private static File		last_directory;

private static int	dialog_placement = BudaConstants.PLACEMENT_PREFER |
      BudaConstants.PLACEMENT_LOGICAL|
      BudaConstants.PLACEMENT_GROUPED;

private enum PatternOptions { EXCLUDE, INCLUDE };



static {
   BoardProperties bp = BoardProperties.getProperties("Bueno");
   String ld = bp.getProperty("Bueno.library.directory");
   FileSystemView fsv = BoardFileSystemView.getFileSystemView();
   if (ld != null) last_directory = fsv.createFileObject(ld);
   else last_directory = null;
}


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BuenoGenericEditorPanel(BuenoGenericProjectEditor ed,Element tabxml)
{
   project_editor = ed;
   tab_xml = tabxml;
   edit_panel = null;
   base_paths = null;
   panel_paths = null;
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

JPanel getPanel()
{
   if (edit_panel == null) createPanel();

   return edit_panel;
}

boolean hasChanged()	
{
   boolean chng = force_update;
   if (base_paths != null) {
      for (BuenoPathEntry pe : base_paths) {
	 chng |= pe.hasChanged();
       }
    }

   force_update = false;

   return chng;
}


void doUpdate()
{
   // Updates are done as they are found.  This call can do anything else that
   // is needed
   if (edit_panel != null) edit_panel.doUpdate();
}



private void createPanel()
{
   String typ = IvyXml.getAttrString(tab_xml,"TYPE");
   if (typ == null) {
      if (IvyXml.getChild(tab_xml,"FIELD") != null) typ = "FIELDS";
      else typ = "NONE";
    }
   switch (typ) {
      default :
      case "NONE" :
	 return;
      case "SOURCE" :
	 edit_panel = createSourcePanel();
	 break;
      case "LIBRARY" :
      case "PATH" :
	 edit_panel = createPathPanel();
	 break;
      case "FIELDS" :
	 edit_panel = createFieldsPanel();
	 break;
      case "PROJECTS" :
	 edit_panel = createProjectPanel();
	 break;
    }
}




/********************************************************************************/
/*                                                                              */
/*      Generic panel                                                           */
/*                                                                              */
/********************************************************************************/

private abstract class EditPanel extends SwingGridPanel {
   
   private static final long serialVersionUID = 1;
   
   void doUpdate()                              { }
   
}       // end of inner class EditPanel



/********************************************************************************/
/*										*/
/*	Path Panel								*/
/*										*/
/********************************************************************************/

private EditPanel createPathPanel()
{
   return new PathPanel();
}


private class PathPanel extends EditPanel implements ActionListener, ListSelectionListener {

   private JButton edit_button;
   private JButton delete_button;
   private JList<BuenoPathEntry>   path_display;
   private static final long serialVersionUID = 1;
   
   PathPanel() {
      base_paths = project_editor.getLibraryPaths();
      panel_paths = new SwingListSet<>(true);
      for (BuenoPathEntry pe : base_paths) {
	 panel_paths.addElement(pe);
       }

      int y = 0;
      JButton bn = new JButton("New Jar File");
      bn.addActionListener(this);
      addGBComponent(bn,1,y++,1,1,0,0);
      bn = new JButton("New Directory");
      bn.addActionListener(this);
      addGBComponent(bn,1,y++,1,1,0,0);
      edit_button = new JButton("Edit");
      edit_button.addActionListener(this);
      addGBComponent(edit_button,1,y++,1,1,0,0);
      delete_button = new JButton("Delete");
      delete_button.addActionListener(this);
      addGBComponent(delete_button,1,y++,1,1,0,0);
      ++y;

      path_display = new JList<>(panel_paths);
      path_display.setFixedCellWidth(-1);
      path_display.setVisibleRowCount(10);
      path_display.addListSelectionListener(this);
      addGBComponent(new JScrollPane(path_display),0,0,1,y++,1,1);
    }

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd.equals("New Jar File")) {
	 askForNew(new FileNameExtensionFilter("Jar Files","jar"),JFileChooser.FILES_ONLY);
       }
      else if (cmd.equals("New Directory")) {
	 askForNew(new BinaryFileFilter(),JFileChooser.DIRECTORIES_ONLY);
       }
      else if (cmd.equals("Edit")) {
	 BuenoPathEntry pe = path_display.getSelectedValue();
	 if (pe == null) return;
	 BudaBubble bb = new EditLibraryPathEntryBubble(pe);
	 BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
	 BudaBubble rbb = BudaRoot.findBudaBubble(this);
	 if (bba != null) bba.addBubble(bb,rbb,null,dialog_placement);
       }
      else if (cmd.equals("Delete")) {
	 for (BuenoPathEntry pe : path_display.getSelectedValuesList()) {
	    panel_paths.removeElement(pe);
	    project_editor.getLibraryPaths().remove(pe);
	  }
	 force_update = true;
       }
      else BoardLog.logE("BUENO","Unknown path panel command " + cmd);
    }

   @Override public void valueChanged(ListSelectionEvent e) {
      updateButtons();
    }

   private void askForNew(FileFilter ff,int mode) {
      NewPathEntryBubble bb = new NewPathEntryBubble(ff,mode);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
      BudaBubble rbb = BudaRoot.findBudaBubble(this);
      if (bba != null) bba.addBubble(bb,rbb,null,dialog_placement,
	    BudaConstants.BudaBubblePosition.STATIC);
    }

   private void updateButtons() {
      List<BuenoPathEntry> sels = path_display.getSelectedValuesList();
      boolean edok = false;
      for (BuenoPathEntry pe : sels) {
	 if (pe.getPathType() == PathType.LIBRARY) {
	    if (edok) {
	       edok = false;
	       break;
	     }
	    edok = true;
	  }
       }
      edit_button.setEnabled(edok);
      delete_button.setEnabled(sels.size() >= 1);
    }

}	// end of inner class PathPanel


private class NewPathEntryBubble extends BudaBubble implements ActionListener {

   private JFileChooser file_chooser;
   private static final long serialVersionUID = 1;
   
   NewPathEntryBubble(FileFilter ff,int mode) {
      FileSystemView fsv = BoardFileSystemView.getFileSystemView();
      file_chooser = new JFileChooser(last_directory,fsv);
      file_chooser.setMultiSelectionEnabled(true);
      file_chooser.addChoosableFileFilter(ff);
      file_chooser.setFileSelectionMode(mode);
      file_chooser.addActionListener(this);
      file_chooser.setOpaque(true);
      setContentPane(file_chooser);
    }

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      File dir = file_chooser.getCurrentDirectory();
      if (dir != null && !dir.equals(last_directory)) {
         last_directory = dir;
         BoardProperties bp = BoardProperties.getProperties("Bueno");
         String dp = dir.getAbsolutePath();
         bp.setProperty("Bueno.library.directory",dp);
         try {
            bp.save();
          }
         catch (IOException e) { }
       }
   
      if (cmd.equals(JFileChooser.APPROVE_SELECTION)) {
         closeWindow(this);
         boolean subdirs = project_editor.useSubdirectories();
         for (File f : file_chooser.getSelectedFiles()) {
            BuenoPathEntry pe = new BuenoPathEntry(f,PathType.LIBRARY,subdirs);
            panel_paths.addElement(pe);
            base_paths.add(pe);
          }
       }
      else if (cmd.equals(JFileChooser.CANCEL_SELECTION)) {
         closeWindow(this);
       }
    }
}	// end of inner class NewPathEntryBubble



private void closeWindow(Component c)
{
   BudaBubble bb = BudaRoot.findBudaBubble(c);
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(c);
   if (bba != null && bb != null) bba.removeBubble(bb);
}


private static class BinaryFileFilter extends FileFilter {

   @Override public String getDescription()	{ return "Java Class File Directory"; }

   @Override public boolean accept(File f) {
      if (!f.isDirectory()) return false;

      return true;
    }

}	// end of inner class BinaryFileFilter


private class EditLibraryPathEntryBubble extends BudaBubble implements ActionListener {

   private BuenoPathEntry	for_path;
   private static final long serialVersionUID = 1;
   
   EditLibraryPathEntryBubble(BuenoPathEntry pp) {
      for_path = pp;
      SwingGridPanel pnl = new SwingGridPanel();
      pnl.beginLayout();
      pnl.addBannerLabel("Edit Project Path Entry");
      pnl.addFileField("Directory",for_path.getSourcePath(),JFileChooser.DIRECTORIES_ONLY,null,null);
      pnl.addBottomButton("Close","Close",this);
      pnl.addBottomButtons();
      setContentPane(pnl);
    }

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd.equals("Directory")) {
	 JTextField tf = (JTextField) evt.getSource();
	 for_path.setSourcePath(tf.getText());
	 force_update = true;
       }
      else if (cmd.equals("Close")) {
	 setVisible(false);
       }
    }

}	// end of inner class EditJSPathEntryBubble



/********************************************************************************/
/*										*/
/*	Path Panel								*/
/*										*/
/********************************************************************************/

private EditPanel createSourcePanel()
{
   return new SourcePanel();
}


private class SourcePanel extends EditPanel implements ActionListener, ListSelectionListener {

   private JButton edit_button;
   private JButton delete_button;
   private JList<BuenoPathEntry> path_display;
   private static final long serialVersionUID = 1;
   
   SourcePanel() {
      base_paths = project_editor.getSourcePaths();
      panel_paths = new SwingListSet<>(true);
      for (BuenoPathEntry pe : base_paths) {
         panel_paths.addElement(pe);
       }
   
      int y = 0;
      if (IvyXml.getAttrBool(tab_xml,"MULTIPLE")) {
         JButton bn = new JButton("New Source Directory");
         bn.addActionListener(this);
         addGBComponent(bn,1,y++,1,1,0,0);
       }
      edit_button = new JButton("Edit");
      edit_button.addActionListener(this);
      addGBComponent(edit_button,1,y++,1,1,0,0);
      if (IvyXml.getAttrBool(tab_xml,"MULTIPLE")) {
         delete_button = new JButton("Delete");
         delete_button.addActionListener(this);
         addGBComponent(delete_button,1,y++,1,1,0,0);
       }
      else delete_button = null;
   
      ++y;
   
      path_display = new JList<>(panel_paths);
      path_display.setVisibleRowCount(10);
      path_display.addListSelectionListener(this);
      addGBComponent(new JScrollPane(path_display),0,0,1,y++,1,1);
    }

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd.equals("New Source Directory")) {
	 askForNew(new BinaryFileFilter(),JFileChooser.DIRECTORIES_ONLY);
       }
      else if (cmd.equals("Edit")) {
	 BuenoPathEntry pe = path_display.getSelectedValue();
	 if (pe == null) return;
	 BudaBubble bb = new EditSourcePathEntryBubble(pe);
	 BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
	 BudaBubble rbb = BudaRoot.findBudaBubble(this);
	 if (bba != null) bba.addBubble(bb,rbb,null,dialog_placement);
       }
      else if (cmd.equals("Delete")) {
	 for (BuenoPathEntry pe : path_display.getSelectedValuesList()) {
	    panel_paths.removeElement(pe);
	    project_editor.getLibraryPaths().remove(pe);
	  }
	 force_update = true;
       }
      else BoardLog.logE("BUENO","Unknown path panel command " + cmd);
    }

   @Override public void valueChanged(ListSelectionEvent e) {
      updateButtons();
    }

   private void askForNew(FileFilter ff,int mode) {
      NewPathEntryBubble bb = new NewPathEntryBubble(ff,mode);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
      BudaBubble rbb = BudaRoot.findBudaBubble(this);
      if (bba != null) bba.addBubble(bb,rbb,null,dialog_placement,
	    BudaConstants.BudaBubblePosition.STATIC);
    }

   private void updateButtons() {
      List<BuenoPathEntry> sels = path_display.getSelectedValuesList();
      boolean edok = false;
      if (sels != null && !sels.isEmpty()) edok = true;
      edit_button.setEnabled(edok);
      boolean candel = sels.size() >= 1 && panel_paths.getSize() > 1;
      if (delete_button != null) delete_button.setEnabled(candel);
    }

}	// end of inner class SourcePanel




private class EditSourcePathEntryBubble extends BudaBubble implements ActionListener {

   private BuenoPathEntry for_path;
   private SwingListSet<SourcePattern> source_patterns;
   private PatternPanel pattern_panel;
   private static final long serialVersionUID = 1;

   EditSourcePathEntryBubble(BuenoPathEntry pp) {
      for_path = pp;
      source_patterns = new SwingListSet<>();
      for (String s : pp.getExcludes()) {
	 source_patterns.addElement(new SourcePattern(s,true));
       }
      for (String s : pp.getIncludes()) {
	 source_patterns.addElement(new SourcePattern(s,false));
       }

      SwingGridPanel pnl = new SwingGridPanel();
      pnl.beginLayout();
      pnl.addBannerLabel("Edit Project Source Entry");
      pnl.addFileField("Directory",for_path.getSourcePath(),JFileChooser.DIRECTORIES_ONLY,null,null);
      if (IvyXml.getAttrBool(tab_xml,"SUBDIRS")) {
	 pnl.addBoolean("Include Subdirectories",for_path.isRecursive(),this);
       }
      pattern_panel = new PatternPanel(for_path,source_patterns);
      pnl.addLabellessRawComponent("PATTERNS",pattern_panel,true,true);

      pnl.addBottomButton("Close","Close",this);
      pnl.addBottomButtons();
      setContentPane(pnl);
    }

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd.equals("Directory")) {
	 JTextField tf = (JTextField) evt.getSource();
	 for_path.setSourcePath(tf.getText());
       }
      else if (cmd.equals("Include Subdirectories")) {
	 JCheckBox cbx = (JCheckBox) evt.getSource();
	 for_path.setUseSubdirs(cbx.isSelected());
       }
      else if (cmd.equals("Close")) {
	 setVisible(false);
       }
    }

}	// end of inner class EditSourcePathEntryBubble



private class SourcePattern {

   private boolean is_exclude;
   private String source_pattern;

   SourcePattern(String pat,boolean exc) {
      source_pattern = pat;
      is_exclude = exc;
    }

   boolean isExclude()				{ return is_exclude; }
   String getPattern()				{ return source_pattern; }

   @Override public int hashCode() {
      int hc = source_pattern.hashCode();
      if (is_exclude) hc += 100;
      return hc;
    }

   @Override public boolean equals(Object v) {
      if (v instanceof SourcePattern) {
	 SourcePattern sv = (SourcePattern) v;
	 return sv.source_pattern.equals(source_pattern) && sv.is_exclude == is_exclude;
       }
      return false;
    }

   @Override public String toString() {
      String what = is_exclude ? "Exclude" : "Include";
      return "[" + what + "]  " + source_pattern;
    }

}	// end of inner class SourcePattern



private class PatternPanel extends SwingListPanel<SourcePattern> {

   private BuenoPathEntry for_path;
   private SwingListSet<SourcePattern> source_patterns;
   private static final long serialVersionUID = 1;
   
   PatternPanel(BuenoPathEntry path,SwingListSet<SourcePattern> pats) {
      super(pats);
      for_path = path;
    }

   protected SourcePattern createNewItem() {
      SourcePatternPanel spnl = new SourcePatternPanel(null);
      int sts = JOptionPane.showOptionDialog(this,spnl,"New Source Pattern",
	    JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE,
	    null,null,null);
      if (sts == JOptionPane.OK_OPTION) {
	 SourcePattern npat = spnl.getResult();
	 source_patterns.addElement(npat);
	 for_path.addPattern(npat.getPattern(),npat.isExclude());
	 return npat;
       }
      return null;
    }

   protected SourcePattern editItem(Object opat) {
      if (opat instanceof SourcePattern) {
	 SourcePattern pat = (SourcePattern) opat;
	 SourcePatternPanel spnl = new SourcePatternPanel(pat);
	 int sts = JOptionPane.showOptionDialog(this,spnl,"Edit Source Pattern",
	       JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE,
	       null,null,null);
	 if (sts == JOptionPane.OK_OPTION) {
	    SourcePattern npat = spnl.getResult();
	    source_patterns.removeElement(pat);
	    for_path.removePattern(pat.getPattern(),pat.isExclude());
	    source_patterns.addElement(npat);
	    for_path.addPattern(npat.getPattern(),npat.isExclude());
	    return npat;
	  }
       }
      return null;
    }

   protected SourcePattern deleteItem(Object opat) {
      if (opat != null && opat instanceof SourcePattern) {
	 SourcePattern pat = (SourcePattern) opat;
	 for_path.removePattern(pat.getPattern(),pat.isExclude());
       }
      return null;
    }

}	// end of inner class PatternPanel



private class SourcePatternPanel extends SwingGridPanel {

   private JTextField pattern_field;
   private JComboBox<PatternOptions> option_field;
   private static final long serialVersionUID = 1;
   
   SourcePatternPanel(SourcePattern pat) {
      String txt = pat == null ? null : pat.getPattern();
      pattern_field = addTextField("Pattern",txt,24,null,null);
      PatternOptions typ = PatternOptions.EXCLUDE;
      if (pat != null && !pat.isExclude()) typ = PatternOptions.INCLUDE;
      if (IvyXml.getAttrBool(tab_xml,"EXCLUDE") && IvyXml.getAttrBool(tab_xml,"INCLUDE")) {
	 option_field =  addChoice("Type",typ,null);
       }
    }

   SourcePattern getResult() {
      String txt = pattern_field.getText().trim();
      if (txt.isEmpty()) return null;
      boolean exl = true;
      if (option_field != null) {
	 exl = option_field.getSelectedItem() == PatternOptions.EXCLUDE;
       }
      return new SourcePattern(txt,exl);
    }

}	// end of inner class NewSourcePatternBubble




/********************************************************************************/
/*										*/
/*     Referenced Project Panel 						*/
/*										*/
/********************************************************************************/

private EditPanel createProjectPanel()
{
   if (project_editor.getOtherProjects().isEmpty()) return null;

   return new ReferencesPanel();
}


private class ReferencesPanel extends EditPanel implements ActionListener {

   private static final long serialVersionUID = 1;
   
   ReferencesPanel() {
      beginLayout();
      Set<String> refs = project_editor.getReferencedProjects();
      for (String s : project_editor.getOtherProjects()) {
         boolean fg = refs.contains(s);
         addBoolean(s,fg,this);
       }
      addExpander();
    }

   @Override public void actionPerformed(ActionEvent evt) {
      String proj = evt.getActionCommand();
      JCheckBox cbx = (JCheckBox) evt.getSource();
      Set<String> refs = project_editor.getReferencedProjects();
      if (cbx.isSelected()) refs.add(proj);
      else refs.remove(proj);
    }

}	// end of inner class ReferencesPanel



/********************************************************************************/
/*										*/
/*	Fields panel								*/
/*										*/
/********************************************************************************/

private EditPanel createFieldsPanel()
{
   return new FieldsPanel();
}


private class FieldsPanel extends EditPanel implements ActionListener {

    private Map<String,Map<String,String>> option_sets;
    private Map<String,Object> cur_values;
    private static final long serialVersionUID = 1;
    
    FieldsPanel() {
       option_sets = new HashMap<>();
       cur_values = new HashMap<>();
       beginLayout();
       String desc = IvyXml.getAttrString(tab_xml,"DESCRIPTION");
       if (desc != null) addBannerLabel(desc);
       for (Element felt : IvyXml.children(tab_xml,"FIELD")) {
          String typ = IvyXml.getAttrString(felt,"TYPE");
          String name = IvyXml.getAttrString(felt,"NAME");
          String lbl = IvyXml.getAttrString(felt,"DESCRIPTION");
          if (name == null) name = lbl;
          JCheckBox cbx = null;
          switch (typ) {
             case "OPTIONSET" :
        	addOptionSet(name,lbl,felt);
        	break;
             case "BOOLEAN" :
        	cbx = addBoolean(name,lbl,felt);
        	break;
             case "CHOICE" :
        	addChoice(name,lbl,felt);
        	break;
           }
          if (IvyXml.getAttrBool(felt,"SETONLY")) {
             String opt = IvyXml.getAttrString(felt,"OPTION");
             if (opt == null) continue;
             String val = project_editor.getOptions().get(opt);
             if (val == null || val.isEmpty()) continue;
             if ("1tTyY".indexOf(val.substring(0,1)) >= 0) {
        	if (cbx != null) cbx.setEnabled(false);
              }
           }
        }
       addExpander();
     }

    @Override public void actionPerformed(ActionEvent evt) {
       String cmd = evt.getActionCommand();
       JComboBox<?> cbx;
       for (Element felt : IvyXml.children(tab_xml,"FIELD")) {
	  String typ = IvyXml.getAttrString(felt,"TYPE");
	  String name = IvyXml.getAttrString(felt,"NAME");
	  String lbl = IvyXml.getAttrString(felt,"DESCRIPTION");
	  if (name == null) name = lbl;
	  if (cmd.equals(lbl)) {
	     switch (typ) {
		case "OPTIONSET" :
		   cbx = (JComboBox<?>) evt.getSource();
		   String nopt = (String) cbx.getSelectedItem();
		   String cur = (String) cur_values.get(name);
		   if (nopt == null || nopt.equals(cur)) return;
		   cur_values.put(name,nopt);
		   Map<String,String> opts = project_editor.getOptions();
		   for (Map.Entry<String,String> ent : option_sets.get(nopt).entrySet()) {
		      opts.put(ent.getKey(),ent.getValue());
		    }
		   force_update = true;
		   break;
		case "BOOLEAN" :
		   JCheckBox chbx = (JCheckBox) evt.getSource();
		   boolean fg = chbx.isSelected();
		   boolean cvl = (Boolean) cur_values.get(name);
		   if (cvl == fg) return;
		   cur_values.put(name,fg);
		   force_update = true;
		   break;
		case "CHOICE" :
		   cbx = (JComboBox<?>) evt.getSource();
		   String nval = (String) cbx.getSelectedItem();
		   String optnm = IvyXml.getAttrString(felt,"OPTION");
		   String oval = (String) cur_values.get(name);
		   if (nval == null || nval.equals(oval)) return;
		   project_editor.getOptions().put(optnm,nval);
		   cur_values.put(name,nval);
		   force_update = true;
		   break;
	      }
	   }
	}
     }

    private void addOptionSet(String name,String label,Element felt) {
       String opt = IvyXml.getAttrString(felt,"OPTION");
       String pfx = IvyXml.getAttrString(felt,"PREFIX");
       BoardProperties bp = BoardProperties.getProperties("Bueno");
       Map<Integer,String> opts = new TreeMap<>();
       for (String s : bp.stringPropertyNames()) {
          if (s.startsWith(opt + ".name.")) {
             int idx = s.lastIndexOf(".");
             try {
        	int v = Integer.parseInt(s.substring(idx+1));
        	opts.put(v,s);
              }
             catch (NumberFormatException e) { }
           }
        }
       for (String onm : opts.values()) {
          String nm = bp.getProperty(onm);
          String vnm = onm.replaceFirst(".name.",".data.");
          String vls = bp.getProperty(vnm);
          if (nm == null || vls == null) continue;
          Map<String,String> vmap = new HashMap<>();
          for (StringTokenizer tok = new StringTokenizer(vls," \n\t,"); tok.hasMoreTokens(); ) {
             String kv = tok.nextToken();
             int idx = kv.indexOf("=");
             if (idx < 0) continue;
             String k = kv.substring(0,idx);
             String v = kv.substring(idx+1);
             vmap.put(k,v);
           }
          option_sets.put(nm,vmap);
        }
    
       String curoption = null;
       Map<String,String> usermap = new HashMap<>();
       for (Map.Entry<String,String> ent : project_editor.getOptions().entrySet()) {
          String k = ent.getKey();
          String v = ent.getValue();
          if (pfx == null || k.startsWith(pfx)) {
             usermap.put(k,v);
           }
        }
       for (Map.Entry<String,Map<String,String>> ent : option_sets.entrySet()) {
          if (compatibleSets(usermap,ent.getValue())) {
             if (curoption == null) curoption = ent.getKey();
           }
        }
       if (curoption == null) {
          curoption = "Current Settings";
          option_sets.put(curoption,usermap);
        }
    
       if (name != null) project_editor.getOptions().put(name,curoption);
       cur_values.put(name,curoption);
       addChoice(label,option_sets.keySet(),curoption,this);
     }

    private JCheckBox addBoolean(String name,String label,Element felt) {
       boolean fg = false;
       String opt = IvyXml.getAttrString(felt,"OPTION");
       if (opt != null) {
	  String oval = project_editor.getOptions().get(opt);
	  String tr = IvyXml.getAttrString(felt,"TRUE");
	  if (oval == null || oval.isEmpty()) fg = false;
	  else if (tr != null) fg = oval.equals(tr);
	  else fg = "1TtYy".indexOf(oval.substring(0,1)) >= 0;
	}
       cur_values.put(name,fg);
       return addBoolean(label,fg,this);
     }

    private void addChoice(String name,String label,Element felt) {
       List<String> opts = new ArrayList<>();
       String lst = IvyXml.getAttrString(felt,"CHOICES");
       if (lst != null) {
	  BoardProperties bp = BoardProperties.getProperties("Bueno");
	  String vals = bp.getString(lst);
	  StringTokenizer tok = new StringTokenizer(vals);
	  while (tok.hasMoreTokens()) {
	     String v = tok.nextToken();
	     opts.add(v);
	   }
	}
       String optnm = IvyXml.getAttrString(felt,"OPTION");
       String val = project_editor.getOptions().get(optnm);
       addChoice(label,opts,val,this);
       cur_values.put(name,val);
     }


}



private boolean compatibleSets(Map<String,String> umap,Map<String,String> kmap)
{
   Set<String> xtra = new HashSet<String>();
   boolean compat = true;

   for (Map.Entry<String,String> ent : umap.entrySet()) {
      String k = ent.getKey();
      String v = ent.getValue();
      if (kmap.containsKey(k)) {
	 if (!kmap.get(k).equals(v))
	    compat = false;
       }
      else xtra.add(k);
    }

   for (String s : xtra) {
      kmap.put(s,umap.get(s));
    }

   return compat;
}




}	// end of class BuenoGenericEditorPanel




/* end of BuenoGenericEditorPanel.java */

