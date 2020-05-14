/********************************************************************************/
/*										*/
/*		BuenoPythonProject.java 					*/
/*										*/
/*	Python project creation/editing dialogs and actions			*/
/*										*/
/********************************************************************************/



package edu.brown.cs.bubbles.bueno;

import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;

import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;


public class BuenoPythonProject implements BuenoConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		project_name;
private File		project_dir;
private Vector<PathData>   project_paths;
private Map<String,OptionData> project_options;



/********************************************************************************/
/*										*/
/*	Static entries								*/
/*										*/
/********************************************************************************/

public static BudaBubble createNewPythonProjectBubble()
{
   BuenoPythonProject pp = new BuenoPythonProject();
   JPanel pc = pp.getProjectCreator();
   if (pc == null) return null;

   return new BuenoPythonBubble(pc);
}



public static BudaBubble createEditPythonProjectBubble(String proj)
{
   BuenoPythonProject pp = null;
   try {
      pp = new BuenoPythonProject(proj);
    }
   catch (BuenoException e) {
      return null;
    }
   JComponent pc = pp.getProjectEditor();
   if (pc == null) return null;

   return new BuenoPythonBubble(pc);
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BuenoPythonProject()
{
   project_name = null;
   project_dir = null;
   project_paths = new Vector<>();
   project_options = new HashMap<>();
}


private BuenoPythonProject(String nm) throws BuenoException
{
   this();
   project_name = nm;
   loadProject();
}



/********************************************************************************/
/*										*/
/*	Project setup methods							*/
/*										*/
/********************************************************************************/

private void loadProject() throws BuenoException
{
   project_dir = null;
   project_paths = new Vector<>();
   project_options = new HashMap<>();

   Element xml = BumpClient.getBump().getProjectData(project_name);
   if (xml == null) throw new BuenoException("Project " + project_name + " not defined");
   project_dir = new File(IvyXml.getAttrString(xml,"PATH"));
   for (Element pelt : IvyXml.children(xml,"PATH")) {
      PathData pd = new PathData(pelt);
      project_paths.add(pd);
    }
   Element prefs = IvyXml.getChild(xml,"PREFERENCES");
   for (Element pref : IvyXml.children(prefs)) {
      OptionData od = new OptionData(pref);
      project_options.put(od.getKey(),od);
    }
}




/********************************************************************************/
/*										*/
/*	Initial project creation dialog 					*/
/*										*/
/********************************************************************************/

JPanel getProjectCreator()
{
   return new ProjectCreator();
}


private boolean createProject()
{
   BumpClient bc = BumpClient.getBump();
   if (project_dir == null) {
      File f1 = new File(BoardSetup.getSetup().getDefaultWorkspace());
      project_dir = new File(f1,project_name);
    }

   if (!bc.createProject(project_name,project_dir)) return false;

   try {
      loadProject();
    }
   catch (BuenoException e) {
      return false;
    }

   return true;
}


private void updateProject()
{
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("PROJECT");
   xw.field("NAME",project_name);
   xw.field("PATH",project_dir.getPath());
   for (OptionData od : project_options.values()) {
      xw.begin("OPTION");
      xw.field("KEY",od.getKey());
      xw.field("VALUE",od.getValue());
      xw.end("OPTION");
    }
   for (PathData pd : project_paths) {
      xw.begin("PATH");
      if (!pd.isLibrary()) {
	 xw.field("USER",true);
       }
      xw.field("DIRECTORY",pd.getDirectory().getPath());
      xw.end("PATH");
    }
   xw.end("PROJECT");

   BumpClient bc = BumpClient.getBump();
   bc.editProject(project_name,xw.toString());
   xw.close();
}



private void updateFiles()
{
   BumpClient bc = BumpClient.getBump();
   bc.compile(false,false,true);
}




private class ProjectCreator extends SwingGridPanel implements ActionListener, UndoableEditListener {

   private JTextField name_field;
   private JTextField file_field;
   private JTextField source_field;
   private JButton create_button;

   ProjectCreator() {
      beginLayout();
      addBannerLabel("Create PYBLES Python Project");
      name_field = addTextField("Name",null,this,this);
      file_field = null;
      // file_field = addFileField("Project Directory",((File) null),JFileChooser.DIRECTORIES_ONLY,this,this);
      source_field = addFileField("External Source",((File) null),JFileChooser.DIRECTORIES_ONLY,this,this);
      addSeparator();
      addBottomButton("CANCEL","CANCEL",this);
      create_button = addBottomButton("CREATE","CREATE",this);
      addBottomButtons();
      checkStatus();
    }

   @Override public void actionPerformed(ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals("CANCEL")) {
	 removeBubble();
       }
      else if (cmd.equals("CREATE")) {
	 removeBubble();
	 project_name = name_field.getText().trim();
	 project_dir = getProjectFile();
	 if (createProject()) {
	    String sdir = source_field.getText().trim();
	    File sfil = null;
	    if (sdir != null && sdir.length() > 0) {
	       sfil = new File(sdir);
	       project_paths.clear();
	       PathData pd = new PathData(sfil,false);
	       project_paths.add(pd);
	       updateProject();
	     }
	  }
	 updateFiles();
       }
      else checkStatus();
    }

   @Override public void undoableEditHappened(UndoableEditEvent e) {
      if (e.getSource() == name_field) {
	 String txt = name_field.getText().trim();
	 if (file_field != null) file_field.setText(txt);
       }
      checkStatus();
    }

   private void removeBubble() {
      BudaBubble bb = BudaRoot.findBudaBubble(this);
      if (bb != null) bb.setVisible(false);
    }

   private File getProjectFile() {
      String pfx = name_field.getText().trim();
      String dnm = null;
      if (file_field != null) {
	 dnm = file_field.getText().trim();
	 if (dnm.length() == 0) dnm = null;
       }
      if (dnm != null) {
	 File f1 = new File(dnm);
	 if (f1.isAbsolute()) return f1;
	 else pfx = dnm;
       }
      File f1 = new File(BoardSetup.getSetup().getDefaultWorkspace());
      return new File(f1,pfx);
    }


   private void checkStatus() {
      boolean isok = true;
      String pnm = name_field.getText().trim();
      if (!pnm.matches("\\w+")) isok = false;
      File f1 = getProjectFile();
      File f2 = f1.getParentFile();
      if (f1.exists() || !f2.exists() || !f2.isDirectory()) isok = false;

      String snm = source_field.getText().trim();
      if (snm != null && snm.length() > 0) {
	 File f = new File(snm);
	 if (!f.exists() || !f.isDirectory()) isok = false;
       }
      create_button.setEnabled(isok);
    }


}	// end of inner class ProjectCreator




/********************************************************************************/
/*										*/
/*	Project editing methods 						*/
/*										*/
/********************************************************************************/

JComponent getProjectEditor()
{
   return new ProjectEditor();
}



private class ProjectEditor extends SwingGridPanel implements ActionListener {

   ProjectEditor() {
      addBannerLabel("Edit PYBLES Python Project " + project_name);
      JTabbedPane tabs = new JTabbedPane();
      addLabellessRawComponent("TABS",tabs);
      PackagePanel ppnl = new PackagePanel();
      tabs.addTab("Packages",ppnl);
      OptionPanel opnl = new OptionPanel();
      tabs.addTab("Options",opnl);
    }

   @Override public void actionPerformed(ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals("CANCEL")) {
	 removeBubble();
       }
      else if (cmd.equals("ACCEPT")) {
	 updateProject();
       }
    }

   private void removeBubble() {
      BudaBubble bb = BudaRoot.findBudaBubble(this);
      if (bb != null) bb.setVisible(false);
    }

}	// end of inner class ProjectEditor





/********************************************************************************/
/*										*/
/*	Path/package editing display						*/
/*										*/
/********************************************************************************/

private class PackagePanel extends SwingGridPanel implements ActionListener, ListSelectionListener {

   private JButton edit_button;
   private JButton delete_button;
   private JList<PathData> path_display;

   PackagePanel() {
      int y = 0;
      JButton bn = new JButton("New Package Directory");
      addGBComponent(bn,1,y++,1,1,0,0);
      edit_button = new JButton("Edit");
      edit_button.addActionListener(this);
      addGBComponent(edit_button,1,y++,1,1,0,0);
      delete_button = new JButton("Delete");
      delete_button.addActionListener(this);
      addGBComponent(delete_button,1,y++,1,1,0,0);
      ++y;
   
      path_display = new JList<PathData>(project_paths);
      path_display.setVisibleRowCount(5);
      addGBComponent(new JScrollPane(path_display),0,0,1,y++,1,1);
    }

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd.equals("New Package Directory")) {
	 // handle new directory
       }
      else if (cmd.equals("Edit")) {
	 // handle editing path
       }
      else if (cmd.equals("Delete")) {
	 // handle deleting paths
       }
    }

   @Override public void valueChanged(ListSelectionEvent e) {
      updateButtons();
    }

   private void updateButtons() {
      List<PathData> sels = path_display.getSelectedValuesList();
      boolean edok = false;
      for (PathData pe : sels) {
	 if (pe.isLibrary()) {
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

}	// end of inner class PackagePanel




/********************************************************************************/
/*										*/
/*	Option editing display							*/
/*										*/
/********************************************************************************/

private static Map<String,String> error_descriptions;
private static String [] severity_set = new String [] { "IGNORE", "WARNING", "ERROR" };
private static Map<String,String> error_values;

static {
   error_descriptions = new LinkedHashMap<String,String>();
   error_descriptions.put("ASSIGNMENT_TO_BUILT_IN_SYMBOL","Assignment to a built in name");
   error_descriptions.put("DUPLICATED_SIGNATURE","Duplicate function/method signature");
   error_descriptions.put("INDENTATION_PROBLEM","Problem with indentation");
   error_descriptions.put("NO_EFFECT_STATEMENT","Statement with no effect");
   error_descriptions.put("NO_SELF","Self should be the first parameter");
   error_descriptions.put("REIMPORT","Import redefinition");
   error_descriptions.put("UNDEFINED_IMPORT_VARIABLE","Undefined variable from import");
   error_descriptions.put("UNDEFINED_VARIABLE","Undefined variable");
   error_descriptions.put("UNUSED_IMPORT","Unused import");
   error_descriptions.put("UNUSED_PARAMETER","Unused parameter");
   error_descriptions.put("UNUSED_VARIABLE","Unused variable");
   error_descriptions.put("UNUSED_WILD_IMPORT","Unused in wild import");
   error_descriptions.put("SYNTAX_ERROR","Syntax error");

   error_values = new HashMap<String,String>();
   error_values.put("IGNORE","NONE");
   error_values.put("WARNING","WARNING");
   error_values.put("ERROR","ERROR");
}


private class OptionPanel extends SwingGridPanel implements ActionListener {

   OptionPanel() {
      addSectionLabel("Error Settings");
      for (Map.Entry<String,String> ent : error_descriptions.entrySet()) {
	 OptionData od = project_options.get("ErrorType." + ent.getKey());
	 if (od != null) {
	    addChoice(ent.getValue(),severity_set,od.getValue(),this);
	  }
       }
    }

   @Override public void actionPerformed(ActionEvent evt) {
      String what = evt.getActionCommand();
      JComboBox<?> op = (JComboBox<?>) evt.getSource();
      String v = (String) op.getSelectedItem();
      v = error_values.get(v);
      for (Map.Entry<String,String> ent : error_descriptions.entrySet()) {
         if (ent.getValue().equals(what)) {
            BumpClient bc = BumpClient.getBump();
            IvyXmlWriter xw = new IvyXmlWriter();
            xw.begin("PROJECT");
            xw.field("NAME",project_name);
            xw.begin("OPTION");
            xw.field("KEY","ErrorType." + ent.getKey());
            xw.field("VALUE",v);
            xw.end("OPTION");
            xw.end("PROJECT");
            bc.editProject(project_name,xw.toString());
            xw.close();
          }
       }
    }

}	// end of inner class OptionPanel




/********************************************************************************/
/*										*/
/*	Bubble container							*/
/*										*/
/********************************************************************************/

private static class BuenoPythonBubble extends BudaBubble {

   BuenoPythonBubble(JComponent pnl) {
      setContentPane(pnl,null);
    }

}	// end of inner class BuenoPythonBubble


/********************************************************************************/
/*										*/
/*	Path Data								*/
/*										*/
/********************************************************************************/

private static class PathData {

   private File path_directory;
   private boolean is_library;

   PathData(Element xml) {
      path_directory = new File(IvyXml.getAttrString(xml,"DIR"));
      is_library = false;
    }

   PathData(File dir,boolean lib) {
      path_directory = dir;
      is_library = false;
    }



   boolean isLibrary()			{ return is_library; }
   File getDirectory()			{ return path_directory; }

   @Override public String toString() {
      return path_directory.toString();
    }

}	// end of inner class PathData




/********************************************************************************/
/*										*/
/*	Option Data								*/
/*										*/
/********************************************************************************/

private static class OptionData {

   private String option_name;
   private String option_value;

   OptionData(Element xml) {
      if (IvyXml.isElement(xml,"SEVERITY")) {
	 option_name = "ErrorType." + IvyXml.getAttrString(xml,"TYPE");
	 option_value = IvyXml.getAttrString(xml,"VALUE");
       }
      else if (IvyXml.isElement(xml,"PROP")) {
	 option_name = IvyXml.getAttrString(xml,"KEY");
	 option_value = IvyXml.getAttrString(xml,"VALUE");
       }
    }

   String getKey()				{ return option_name; }
   String getValue()				{ return option_value; }

}	// end of inner class OptionData;


}	// end of class BuenoPythonProject




/* end of BuenoPythonProject.java */

