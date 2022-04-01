/********************************************************************************/
/*										*/
/*		BuenoProjectDialog.java 					*/
/*										*/
/*	BUbbles Environment New Objects project path setup dialog		*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.bueno;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardFileSystemView;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;

import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.swing.SwingListSet;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

import java.awt.Component;
import java.awt.Dimension;
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
import java.util.TreeSet;


class BuenoProjectDialog implements BuenoConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String			project_name;
private File			project_dir;
private List<String>		ref_projects;
private Set<String>		refby_projects;
private SwingListSet<PathEntry> library_paths;
private Set<PathEntry>		source_paths;
private Set<PathEntry>		initial_paths;
private Map<String,String>	option_elements;
private Map<String,String>	start_options;
private Map<String,Map<String,String>> option_sets;
private boolean 		optional_error;
private String			current_optionset;
private Set<PrefEntry>		pref_entries;
private ProblemPanel		problem_panel;
private ReferencesPanel 	references_panel;
private ContractPanel		contract_panel;
private boolean 		force_update;
private Set<String>		other_projects;

private static File		last_directory;

private static final int PATH_LENGTH = 40;

private static int	dialog_placement = BudaConstants.PLACEMENT_PREFER |
						BudaConstants.PLACEMENT_LOGICAL|
						BudaConstants.PLACEMENT_GROUPED;

private static String [] compiler_levels = new String[] {
   "1.3", "1.4", "1.5", "1.6", "1.7", "1.8", "9", 
      "10", "11", "12", "13", "14", "15", "16", "17"
};

private static final String SOURCE_OPTION = "org.eclipse.jdt.core.compiler.source";
private static final String TARGET_OPTION = "org.eclipse.jdt.core.compiler.codegen.targetPlatform";
private static final String COMPLIANCE_OPTION = "org.eclipse.jdt.core.compiler.compliance";
private static final String ERROR_OPTION = "org.eclipse.jdt.core.compiler.problem.fatalOptionalError";
private static final String COFOJA_OPTION = "edu.brown.cs.bubbles.bedrock.useContractsForJava";
private static final String TYPE_ANNOT_OPTION = "edu.brown.cs.bubbles.bedrock.useTypeAnnotations";
private static final String JUNIT_OPTION = "edu.brown.cs.bubbles.bedrock.useJunit";
private static final String ASSERT_OPTION = "edu.brown.cs.bubbles.bedrock.useAssertions";
private static final String ANNOT_OPTION = "org.eclipse.jdt.core.compiler.processAnnotations";


static {
   BoardProperties bp = BoardProperties.getProperties("Bueno");
   String ld = bp.getProperty("Bueno.library.directory");
   FileSystemView fsv = BoardFileSystemView.getFileSystemView();
   if (ld != null) last_directory = fsv.createFileObject(ld);
   else last_directory = null;
}




enum PathType {
   NONE,
   SOURCE,
   BINARY,
   LIBRARY
}


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BuenoProjectDialog(String proj)
{
   project_name = proj;
   ref_projects = new ArrayList<String>();
   refby_projects = new HashSet<String>();
   library_paths = new SwingListSet<PathEntry>(true);
   source_paths = new HashSet<PathEntry>();
   option_elements = new HashMap<String,String>();
   option_sets = new HashMap<String,Map<String,String>>();
   initial_paths = new HashSet<PathEntry>();
   pref_entries = new HashSet<PrefEntry>();
   force_update = false;

   BumpClient bc = BumpClient.getBump();
   Element xml = bc.getProjectData(proj);

   if (xml == null) return;

   String dir = IvyXml.getAttrString(xml,"PATH");
   FileSystemView fsv = BoardFileSystemView.getFileSystemView();
   if (dir != null) project_dir = fsv.createFileObject(dir);

   for (Element e : IvyXml.children(xml,"REFERENCES")) {
      String ref = IvyXml.getText(e);
      ref_projects.add(ref);
    }
   for (Element e : IvyXml.children(xml,"USEDBY")) {
      String ref = IvyXml.getText(e);
      refby_projects.add(ref);
    }

   for (Element e : IvyXml.children(xml,"OPTION")) {
      String k = IvyXml.getAttrString(e,"NAME");
      String v = IvyXml.getAttrString(e,"VALUE");
      if (k != null && v != null) option_elements.put(k,v);
      BoardLog.logD("BUENO","Set option " + k + " = " + v);
    }
   for (Element e : IvyXml.children(xml,"PROPERTY")) {
      String q = IvyXml.getAttrString(e,"QUAL");
      String n = IvyXml.getAttrString(e,"NAME");
      String v = IvyXml.getAttrString(e,"VALUE");
      if (q != null && n != null && v != null) option_elements.put(q + "." + n,v);
    }
   start_options = new HashMap<String,String>(option_elements);

   Element cxml = IvyXml.getChild(xml,"RAWPATH");
   for (Element e : IvyXml.children(cxml,"PATH")) {
      PathEntry pe = new PathEntry(e);
      if (!pe.isNested() && pe.getPathType() == PathType.LIBRARY) {
	 library_paths.addElement(pe);
	 initial_paths.add(pe);
       }
      else if (!pe.isNested() && pe.getPathType() == PathType.SOURCE) {
	 source_paths.add(pe);
	 initial_paths.add(pe);
       }
    }

   setupProblemOptions();

   other_projects = new TreeSet<String>();
   xml = bc.getAllProjects();
   if (xml != null) {
      for (Element pe : IvyXml.children(xml,"PROJECT")) {
	 String pnm = IvyXml.getAttrString(pe,"NAME");
	 if (proj.equals(pnm)) continue;
	 if (refby_projects.contains(pnm)) continue;
	 other_projects.add(pnm);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Methods to create an editor bubble					*/
/*										*/
/********************************************************************************/

public BudaBubble createProjectEditor()
{
   SwingGridPanel pnl = new SwingGridPanel();

   BoardColors.setColors(pnl,"Bueno.project.editor.background");
   
   JLabel lbl = new JLabel("Properties for Project " + project_name,SwingConstants.CENTER);
   BoardColors.setTransparent(lbl,pnl);
   pnl.addGBComponent(lbl,0,0,0,1,0,0);

   JTabbedPane tbp = new JTabbedPane(SwingConstants.TOP);
   BoardColors.setColors(tbp,"Bueno.project.editor.background");
   
   tbp.addTab("Libraries",new PathPanel());
   problem_panel = new ProblemPanel();
   tbp.addTab("Compiler",problem_panel);
   if (other_projects != null && other_projects.size() != 0) {
      references_panel = new ReferencesPanel();
      tbp.addTab("References",references_panel);
    }
   else references_panel = null;
   contract_panel = new ContractPanel();
   tbp.addTab("Contracts",contract_panel);
   pnl.addGBComponent(tbp,0,1,0,1,1,1);

   Box bx = Box.createHorizontalBox();
   bx.add(Box.createHorizontalGlue());
   JButton apply = new JButton("Apply Changes");
   apply.addActionListener(new ProjectEditor());
   bx.add(apply);
   bx.add(Box.createHorizontalGlue());
   pnl.addGBComponent(bx,0,2,0,1,0,0);

   Dimension d = pnl.getPreferredSize();
   if (d.width < 400) {
      d.width = 400;
      pnl.setPreferredSize(d);
    }

   return new ProjectBubble(pnl);
}



private static class ProjectBubble extends BudaBubble {

   ProjectBubble(Component c) {
      setContentPane(c);
    }

}	// end of inner class ProjectBubble



/********************************************************************************/
/*										*/
/*	Utility methods 							*/
/*										*/
/********************************************************************************/

private void closeWindow(Component c)
{
   BudaBubble bb = BudaRoot.findBudaBubble(c);
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(c);
   if (bba != null && bb != null) bba.removeBubble(bb);
}




/********************************************************************************/
/*										*/
/*	Path panel								*/
/*										*/
/********************************************************************************/

private class PathPanel extends SwingGridPanel implements ActionListener, ListSelectionListener {

   private JButton edit_button;
   private JButton delete_button;
   private JList<PathEntry>   path_display;

   PathPanel() {
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

      path_display = new JList<PathEntry>(library_paths);
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
	 PathEntry pe = path_display.getSelectedValue();
	 if (pe == null) return;
	 EditPathEntryBubble bb = new EditPathEntryBubble(pe);
	 BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
	 BudaBubble rbb = BudaRoot.findBudaBubble(this);
	 if (bba != null) bba.addBubble(bb,rbb,null,dialog_placement);
       }
      else if (cmd.equals("Delete")) {
	 for (PathEntry pe : path_display.getSelectedValuesList()) {
	    library_paths.removeElement(pe);
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
      List<PathEntry> sels = path_display.getSelectedValuesList();
      boolean edok = false;
      for (PathEntry pe : sels) {
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



/********************************************************************************/
/*										*/
/*	Methods for adding paths						*/
/*										*/
/********************************************************************************/

private class NewPathEntryBubble extends BudaBubble implements ActionListener {

   private JFileChooser file_chooser;

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
         bp.setProperty("Bueno.library.directory",dir.getAbsolutePath());
         try {
            bp.save();
          }
         catch (IOException e) { }
       }
   
      if (cmd.equals(JFileChooser.APPROVE_SELECTION)) {
         closeWindow(this);
         for (File f : file_chooser.getSelectedFiles()) {
            PathEntry pe = new PathEntry(f);
            library_paths.addElement(pe);
          }
       }
      else if (cmd.equals(JFileChooser.CANCEL_SELECTION)) {
         closeWindow(this);
       }
    }
}	// end of inner class NewPathEntryBubble



private static class BinaryFileFilter extends FileFilter {

   @Override public String getDescription()	{ return "Java Class File Directory"; }

   @Override public boolean accept(File f) {
      if (!f.isDirectory()) return false;

      return true;
    }



}	// end of inner class BinaryFileFilter







/********************************************************************************/
/*										*/
/*	Methods for path element editing					*/
/*										*/
/********************************************************************************/

private static class EditPathEntryBubble extends BudaBubble implements ActionListener {

   private PathEntry	for_path;

   EditPathEntryBubble(PathEntry pp) {
      for_path = pp;
      SwingGridPanel pnl = new SwingGridPanel();
      pnl.beginLayout();
      pnl.addBannerLabel("Edit Project Path Entry");
      switch (for_path.getPathType()) {
         case LIBRARY :
            pnl.addFileField("Library",for_path.getBinaryPath(),0,this,null);
            pnl.addFileField("Source Attachment",for_path.getSourcePath(),0,this,null);
            pnl.addFileField("Java Doc Attachment",for_path.getJavadocPath(),0,this,null);
            break;
         default:
            break;
       }
      pnl.addBoolean("Exported",for_path.isExported(),this);
      pnl.addBoolean("Optional",for_path.isOptional(),this);
   
      pnl.addBottomButton("Close","Close",this);
      pnl.addBottomButtons();
      setContentPane(pnl);
    }

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd.equals("Library")) {
	 JTextField tf = (JTextField) evt.getSource();
	 for_path.setBinaryPath(tf.getText());
       }
      else if (cmd.equals("Source Attachment")) {
	 JTextField tf = (JTextField) evt.getSource();
	 for_path.setSourcePath(tf.getText());
       }
      else if (cmd.equals("Java Doc Attachment")) {
	 JTextField tf = (JTextField) evt.getSource();
	 for_path.setJavadocPath(tf.getText());
       }
      else if (cmd.equals("Exported")) {
	 JCheckBox cbx = (JCheckBox) evt.getSource();
	 for_path.setExported(cbx.isSelected());
       }
      else if (cmd.equals("Optional")) {
	 JCheckBox cbx = (JCheckBox) evt.getSource();
	 for_path.setOptional(cbx.isSelected());
       }
      else if (cmd.equals("Close")) {
	 setVisible(false);
       }
    }

}	// end of inner class NewPathEntryBubble



/********************************************************************************/
/*										*/
/*	Path Information							*/
/*										*/
/********************************************************************************/

private static class PathEntry implements Comparable<PathEntry> {

   private PathType path_type;
   private String source_path;
   private String output_path;
   private String binary_path;
   private String javadoc_path;
   private boolean is_exported;
   private boolean is_optional;
   private boolean is_nested;
   private boolean is_new;
   private boolean is_modified;
   private int	   entry_id;

   PathEntry(Element e) {
      path_type = IvyXml.getAttrEnum(e,"TYPE",PathType.NONE);
      source_path = IvyXml.getTextElement(e,"SOURCE");
      output_path = IvyXml.getTextElement(e,"OUTPUT");
      binary_path = IvyXml.getTextElement(e,"BINARY");
      javadoc_path = IvyXml.getTextElement(e,"JAVADOC");
      entry_id = IvyXml.getAttrInt(e,"ID");
      is_exported = IvyXml.getAttrBool(e,"EXPORTED");
      is_nested = IvyXml.getAttrBool(e,"NESTED");
      is_optional = IvyXml.getAttrBool(e,"OPTIONAL");
      is_new = false;
      is_modified = false;
    }

   PathEntry(File f) {
      path_type = PathType.LIBRARY;
      source_path = null;
      output_path = null;
      binary_path = (f == null ? null : f.getPath());
      is_exported = false;
      is_optional = false;
      is_nested = false;
      is_new = true;
      is_modified = true;
      entry_id = 0;
    }

   boolean isNested()					{ return is_nested; }
   PathType getPathType()				{ return path_type; }
   String getBinaryPath()				{ return binary_path; }
   String getSourcePath()				{ return source_path; }
   String getJavadocPath()				{ return javadoc_path; }
   boolean isExported() 				{ return is_exported; }
   boolean isOptional() 				{ return is_optional; }
   boolean hasChanged() 				{ return is_new || is_modified; }

   void setBinaryPath(String p) {
      if (p == null || p.length() == 0 || p.equals(binary_path)) return;
      binary_path = p;
      is_modified = true;
    }

   void setSourcePath(String p) {
      if (p == null || p.length() == 0 || p.equals(source_path)) return;
      source_path = p;
      is_modified = true;
    }

   void setJavadocPath(String p) {
       if (p == null || p.length() == 0 || p.equals(javadoc_path)) return;
       javadoc_path = p;
       is_modified = true;
     }

   void setExported(boolean fg) {
      if (fg == is_exported) return;
      is_exported = fg;
      is_modified = true;
    }

   void setOptional(boolean fg) {
      if (fg == is_optional) return;
      is_optional = fg;
      is_modified = true;
    }

   void outputXml(IvyXmlWriter xw,boolean del) {
      xw.begin("PATH");
      if (del) xw.field("DELETE",true);
      if (entry_id != 0) xw.field("ID",entry_id);
      xw.field("TYPE",path_type);
      xw.field("NEW",is_new);
      xw.field("MODIFIED",is_modified);
      xw.field("EXPORTED",is_exported);
      xw.field("OPTIONAL",is_optional);
      if (source_path != null) xw.textElement("SOURCE",source_path);
      if (output_path != null) xw.textElement("OUTPUT",output_path);
      if (binary_path != null) xw.textElement("BINARY",binary_path);
      if (javadoc_path != null) xw.textElement("JAVADOC",javadoc_path);
      xw.end("PATH");
    }

   @Override public String toString() {
      FileSystemView fsv = BoardFileSystemView.getFileSystemView();
      switch (path_type) {
         case LIBRARY :
         case BINARY :
            if (binary_path == null) break;
            if (binary_path.length() <= PATH_LENGTH) return binary_path;
            File f = fsv.createFileObject(binary_path);
            String rslt = f.getName();
            for ( ; ; ) {
               File f1 = f.getParentFile();
               if (f1 == null) {
                  rslt = File.separator + rslt;
                  break;
                }
               String pname = f1.getName();
               String rslt1 = pname + File.separator + rslt;
               if (rslt1.length() >= PATH_LENGTH) {
                  rslt = "..." + File.separator + rslt;
                  break;
                }
               rslt = rslt1;
               f = f1;
             }
            return rslt;
         case SOURCE :
            if (source_path != null) {
               File f2 = fsv.createFileObject(source_path);
               return f2.getName() + " (SOURCE)";
             }
            break;
         default:
            break;
       }
      return path_type.toString() + " " + source_path + " " + output_path + " " + binary_path;
    }

   @Override public int compareTo(PathEntry pe) {
      int cmp = toString().compareTo(pe.toString());
      if (cmp == 0) {
         cmp = binary_path.compareTo(pe.binary_path);
       }
      return cmp;
    }

}	// end of inner class PathEntry




/********************************************************************************/
/*										*/
/*	Handle preference setting request							   */
/*										*/
/********************************************************************************/

private static class PrefEntry {

    private String qual_name;
    private String item_name;
    private String pref_value;

    PrefEntry(String q,String n,String v) {
       qual_name = q;
       item_name = n;
       pref_value = v;
     }

    void outputXml(IvyXmlWriter xw) {
	xw.begin("XPREF");
	xw.field("NODE",qual_name);
	xw.field("KEY",item_name);
	xw.field("VALUE",pref_value);
	xw.end("XPREF");
     }

}	// end of inner class PrefEntry

/********************************************************************************/
/*										*/
/*	Problem panel								*/
/*										*/
/********************************************************************************/

private void setupProblemOptions()
{
   BoardProperties bp = BoardProperties.getProperties("Bueno");
   Map<Integer,String> opts = new TreeMap<Integer,String>();
   for (String s : bp.stringPropertyNames()) {
      if (s.startsWith("Bueno.problem.set.name.")) {
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
      Map<String,String> vmap = new HashMap<String,String>();
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

   optional_error = false;
   current_optionset = null;
   Map<String,String> usermap = new HashMap<String,String>();
   for (Map.Entry<String,String> ent : option_elements.entrySet()) {
      String k = ent.getKey();
      String v = ent.getValue();
      if (k.equals(ERROR_OPTION)) {
	 optional_error = v.equals("enabled");
       }
      else if (k.startsWith("org.eclipse.jdt.core.compiler.problem.")) {
	 usermap.put(k,v);
       }
    }
   for (Map.Entry<String,Map<String,String>> ent : option_sets.entrySet()) {
      if (compatibleSets(usermap,ent.getValue())) {
	 if (current_optionset == null) current_optionset = ent.getKey();
       }
    }
   if (current_optionset == null) {
      current_optionset = "Current Settings";
      option_sets.put(current_optionset,usermap);
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



private class ProblemPanel extends SwingGridPanel implements ActionListener {

   private boolean needs_update;

   ProblemPanel() {
      needs_update = false;
      beginLayout();
      addBannerLabel("Compiler Problem Settings");
      addChoice("Option Set",option_sets.keySet(),current_optionset,this);
      addBoolean("Warnings as Errors",optional_error,this);
      addChoice("Java Source Version",compiler_levels,
            option_elements.get(SOURCE_OPTION),this);
      addChoice("Java Target Version",compiler_levels,
            option_elements.get(TARGET_OPTION),this);
      addChoice("Java Compliance Version",compiler_levels,
               option_elements.get(COMPLIANCE_OPTION),this);
    }

   boolean needsUpdate()			{ return needs_update; }

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd.equals("Option Set")) {
         JComboBox<?> cbx = (JComboBox<?>) evt.getSource();
         String nopt = (String) cbx.getSelectedItem();
         if (nopt == null || nopt.equals(current_optionset)) return;
         current_optionset = nopt;
         Map<String,String> oval = option_sets.get(nopt);
         for (Map.Entry<String,String> ent : oval.entrySet()) {
            option_elements.put(ent.getKey(),ent.getValue());
          }
         needs_update = true;
       }
      else if (cmd.equals("Warnings as Errors")) {
         JCheckBox cbx = (JCheckBox) evt.getSource();
         boolean fg = cbx.isSelected();
         if (fg == optional_error) return;
         optional_error = fg;
         needs_update = true;
       }
      else if (cmd.equals("Java Source Version")) {
         JComboBox<?> cbx = (JComboBox<?>) evt.getSource();
         String nval = (String) cbx.getSelectedItem();
         String oval = option_elements.get(SOURCE_OPTION);
         if (nval == null || nval.equals(oval)) return;
         option_elements.put(SOURCE_OPTION,nval);
         needs_update = true;
       }
      else if (cmd.equals("Java Target Version")) {
         JComboBox<?> cbx = (JComboBox<?>) evt.getSource();
         String nval = (String) cbx.getSelectedItem();
         String oval = option_elements.get(TARGET_OPTION);
         if (nval == null || nval.equals(oval)) return;
         option_elements.put(TARGET_OPTION,nval);
         needs_update = true;
       }
      else if (cmd.equals("Java Compliance Version")) {
         JComboBox<?> cbx = (JComboBox<?>) evt.getSource();
         String nval = (String) cbx.getSelectedItem();
         String oval = option_elements.get(COMPLIANCE_OPTION);
         if (nval == null || nval.equals(oval)) return;
         option_elements.put(COMPLIANCE_OPTION,nval);
         needs_update = true;
       }
      else BoardLog.logE("BUENO","Unknown problem panel command " + cmd);
    }



}	// end of inner class ProblemPanel



/********************************************************************************/
/*										*/
/*	Handle contract setup							*/
/*										*/
/********************************************************************************/

private void setupContractsForJava()
{
   FileSystemView fsv = BoardFileSystemView.getFileSystemView();
   BoardSetup bs = BoardSetup.getSetup();
   String path = bs.getRemoteLibraryPath("cofoja.jar");
   File p1 = fsv.createFileObject(project_dir,".apt_generated");
   String anm = p1.getAbsolutePath();

   String snm = null;
   boolean fnd = false;
   boolean sfnd = false;
   for (PathEntry pe : initial_paths) {
      if (pe.getSourcePath() != null && !pe.isOptional() && snm == null) snm = pe.getSourcePath();
      if (pe.getBinaryPath() != null && pe.getBinaryPath().equals(path)) fnd = true;
      if (pe.getSourcePath() != null && pe.getSourcePath().equals(anm)) sfnd = true;
    }
   if (!fnd) {
      PathEntry pe = new PathEntry(fsv.createFileObject(path));
      library_paths.addElement(pe);
    }
   if (!sfnd) {
      PathEntry pe = new PathEntry((File) null);
      pe.setSourcePath(anm);
      pe.setOptional(true);
      library_paths.addElement(pe);
    }

   option_elements.put(ANNOT_OPTION,"enabled");

   PrefEntry pe = new PrefEntry("org.eclispe.jdt.apt.core","org.eclipse.jdt.apt.reconcileEnabled","true");
   pref_entries.add(pe);
   pe = new PrefEntry("org.eclipse.jdt.apt.core","org.eclipse.jdt.apt.aptEnabled","true");
   pref_entries.add(pe);
   pe = new PrefEntry("org.eclipse.jdt.apt.core","org.eclipse.jdt.apt.genSrcDir",".apt_generated");
   pref_entries.add(pe);
   pe = new PrefEntry("org.eclipse.jdt.apt.processorOptions","com.google.java.contract.classoutput",snm);
   pref_entries.add(pe);
   pe = new PrefEntry("org.eclipse.jdt.apt.processorOptions","com.google.java.contract.classpath",path);
   pref_entries.add(pe);

   option_elements.put(COFOJA_OPTION,"true");
}



private void setupJunit()
{
   BoardSetup bs = BoardSetup.getSetup();
   String path = bs.getRemoteLibraryPath("junit.jar");

   boolean fnd = false;
   for (PathEntry pe : initial_paths) {
      if (pe.getBinaryPath() != null && pe.getBinaryPath().contains("junit")) fnd = true;
    }
   if (!fnd) {
      FileSystemView fsv = BoardFileSystemView.getFileSystemView();
      PathEntry pe = new PathEntry(fsv.createFileObject(path));
      library_paths.addElement(pe);
    }

   option_elements.put(JUNIT_OPTION,"true");
}



private void setupAnnotations()
{
   BoardSetup bs = BoardSetup.getSetup();
   String path = bs.getRemoteLibraryPath("annotations.jar");

   boolean fnd = false;
   for (PathEntry pe : initial_paths) {
      if (pe.getBinaryPath() != null && pe.getBinaryPath().contains("annotations")) fnd = true;
    }
   if (!fnd) {
      FileSystemView fsv = BoardFileSystemView.getFileSystemView();
      PathEntry pe = new PathEntry(fsv.createFileObject(path));
      library_paths.addElement(pe);
    }

   option_elements.put(TYPE_ANNOT_OPTION,"true");
}






private class ContractPanel extends SwingGridPanel implements ActionListener {

   private JButton cofoja_button;
   private JButton junit_button;
   private JButton annot_button;
   private JCheckBox assert_button;
   private boolean setup_cofoja;
   private boolean setup_junit;
   private boolean setup_annot;

   ContractPanel() {
      setup_cofoja = false;
      setup_junit = false;
      setup_annot = false;
      beginLayout();
      addBannerLabel("Contract Checking");
//    cofoja_button = new JButton("Enable Contracts For Java");
//    cofoja_button.setEnabled(!option_elements.containsKey(COFOJA_OPTION));
//    cofoja_button.addActionListener(this);
//    addLabellessRawComponent("COFOJA",cofoja_button,true,false);
   
      junit_button = new JButton("Enable JUNIT Testing");
      junit_button.setEnabled(!option_elements.containsKey(JUNIT_OPTION));
      junit_button.addActionListener(this);
      addLabellessRawComponent("JUNIT",junit_button,true,false);
   
      annot_button = new JButton("Enable Type Annotations");
      annot_button.setEnabled(!option_elements.containsKey(TYPE_ANNOT_OPTION));
      annot_button.addActionListener(this);
      addLabellessRawComponent("ANNOT",annot_button,true,false);
   
      assert_button = new JCheckBox("Enable Assertions");
      assert_button.setSelected(option_elements.containsKey(ASSERT_OPTION));
      assert_button.addActionListener(this);
      addLabellessRawComponent("ASSERT",assert_button,true,false);
      addExpander();
    }

   boolean setupCofoja()		{ return setup_cofoja; }
   boolean setupJunit() 		{ return setup_junit; }
   boolean enableAssertions()		{ return assert_button.isSelected(); }
   boolean setupAnnoations()		{ return setup_annot; }

   @Override public void actionPerformed(ActionEvent evt) {
      if (evt.getSource() == cofoja_button) {
	 cofoja_button.setEnabled(false);
	 setup_cofoja = true;
       }
      else if (evt.getSource() == junit_button) {
	 junit_button.setEnabled(false);
	 setup_junit = true;
       }
      else if (evt.getSource() == annot_button) {
	 annot_button.setEnabled(false);
	 setup_annot = true;
       }
      else if (evt.getSource() == assert_button) {
	 // no need to do anything
       }
    }

}	// end of inner class ContractPanel




private class ReferencesPanel extends SwingGridPanel implements ActionListener {

   private Set<String> use_refs;

   ReferencesPanel() {
      beginLayout();
      use_refs = new HashSet<String>(ref_projects);
      for (String s : other_projects) {
         addBoolean(s,use_refs.contains(s),this);
       }
    }

   Set<String> getUsedReferences()	{ return use_refs; }

   @Override public void actionPerformed(ActionEvent evt) {
      String proj = evt.getActionCommand();
      JCheckBox cbx = (JCheckBox) evt.getSource();
      if (cbx.isSelected()) use_refs.add(proj);
      else use_refs.remove(proj);
    }

}	// end of inner class ReferencesPanel




/********************************************************************************/
/*										*/
/*	<comment here>								*/
/*										*/
/********************************************************************************/

private boolean anythingChanged()
{
   if (force_update) return true;

   for (PathEntry pe : library_paths) {
      if (pe.hasChanged()) return true;
    }
   if (problem_panel.needsUpdate()) return true;

   return false;
}



private class ProjectEditor implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      Set<PathEntry> dels = new HashSet<PathEntry>(initial_paths);
      dels.removeAll(source_paths);
      boolean chng = false;
   
      if (contract_panel.setupCofoja()) {
         setupContractsForJava();
       }
      if (contract_panel.setupJunit()) {
         setupJunit();
       }
      if (contract_panel.setupAnnoations()) {
         setupAnnotations();
       }
      option_elements.put(ASSERT_OPTION,Boolean.toString(contract_panel.enableAssertions()));
   
      IvyXmlWriter xw = new IvyXmlWriter();
      xw.begin("PROJECT");
      xw.field("NAME",project_name);
      for (PathEntry pe : library_paths) {
         pe.outputXml(xw,false);
         dels.remove(pe);
       }
      for (PathEntry pe : dels) {
         pe.outputXml(xw,true);
       }
   
      for (Map.Entry<String,String> ent : option_elements.entrySet()) {
         String k = ent.getKey();
         String v = ent.getValue();
         if (k == null || v == null) continue;
         if (start_options != null) {
            String ov = start_options.get(k);
            if (v.equals(ov)) continue;
          }
         chng = true;
         xw.begin("OPTION");
         xw.field("NAME",k);
         xw.field("VALUE",v);
         xw.end("OPTION");
       }
   
      for (PrefEntry pe : pref_entries) {
          pe.outputXml(xw);
          chng = true;
       }
   
      if (references_panel != null) {
         int ct = ref_projects.size();
         xw.begin("REFERENCES");
         for (String s : references_panel.getUsedReferences()) {
            xw.textElement("PROJECT",s);
            if (!ref_projects.contains(s)) chng = true;
            --ct;
          }
         if (ct != 0) chng = true;
         xw.end("REFERENCES");
       }
   
      xw.end("PROJECT");
   
      closeWindow(problem_panel);
   
      BumpClient bc = BumpClient.getBump();
   
      if (chng || anythingChanged())
         bc.editProject(project_name,xw.toString());
   
      force_update = false;
    }

}	// end of inner class ProjectEditor




}	// end of class BuenoProjectDialog




/* end of BuenoProjectDialog.java */
