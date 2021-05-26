/********************************************************************************/
/*										*/
/*		BddtLaunchBubble.java						*/
/*										*/
/*	Bubble Environment launch configuration bubble				*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Adam M. Cook, Steven P. Reiss	*/
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




package edu.brown.cs.bubbles.bddt;

import edu.brown.cs.bubbles.bass.BassConstants;
import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.bass.BassName;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardFileSystemView;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaConstants.BudaBubbleOutputer;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.swing.SwingComboBox;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.swing.SwingNumericField;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JPopupMenu;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;



class BddtLaunchBubble extends BudaBubble implements BddtConstants, BudaConstants, BassConstants,
	BumpConstants, BudaBubbleOutputer, ActionListener, UndoableEditListener {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BumpClient		bump_client;
private BumpLaunchConfig	launch_config;
private BumpLaunchConfig	edit_config;
private JTextComponent		launch_name;
private JTextComponent		arg_area;
private JTextComponent		vmarg_area;
private JButton 		debug_button;
private JButton 		save_button;
private JButton 		revert_button;
private JButton 		clone_button;
private SwingComboBox<String>   project_name;
private SwingComboBox<String>   start_class;
private JCheckBox		stop_in_main;
private JTextComponent		test_class;
private JTextComponent		test_name;
private JTextComponent		host_name;
private JTextComponent		log_file;
private JTextComponent		working_directory;
private SwingNumericField	port_number;
private boolean 		doing_load;
private Map<String,String>      project_map;



private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Cnostructors								*/
/*										*/
/********************************************************************************/

BddtLaunchBubble(BumpLaunchConfig cfg)
{
   bump_client = BumpClient.getBump();
   launch_config = cfg;
   edit_config = null;
   doing_load = false;
   project_map = null;

   setupPanel();
}




/********************************************************************************/
/*										*/
/*	Panel methods								*/
/*										*/
/********************************************************************************/

private void setupPanel()
{
   doing_load = true;

   SwingGridPanel pnl = new SwingGridPanel();
   BoardColors.setColors(pnl,"Bddt.launch.background");
   pnl.beginLayout();
   launch_name = pnl.addTextField("Launch Name",launch_config.getConfigName(),null,this);

   String lp = launch_config.getProject();
   Element pxml = bump_client.getAllProjects();
   project_name = null;
   if (pxml != null) {
      List<String> pnms = new ArrayList<String>();
      for (Element pe : IvyXml.children(pxml,"PROJECT")) {
	 String pnm = IvyXml.getAttrString(pe,"NAME");
	 pnms.add(pnm);
       }
      if (pnms.size() > 1 || lp == null) {
	 project_name = pnl.addChoice("Project",pnms,lp,this);
	 String xp = (String) project_name.getSelectedItem();
	 if (xp != null && (lp == null || !xp.equals(lp))) {
	    BumpLaunchConfig blc = launch_config.setProject(xp);
	    if (blc != null) launch_config = blc;
	  }
       }
    }

   Collection<String> starts = getStartClassesForProject();

   start_class = null;
   arg_area = null;
   vmarg_area = null;
   stop_in_main = null;
   test_class = null;
   test_name = null;
   host_name = null;
   port_number = null;
   log_file = null;
   working_directory = null;

   switch (launch_config.getConfigType()) {
      case JAVA_APP :
	 start_class = pnl.addChoice("Start Class",starts,launch_config.getMainClass(),false,this);
	 // start_class.setEditable(true);
         start_class.setCaseSensitive(false);
	 if (launch_config.getMainClass() == null) {
	    String s = (String) start_class.getSelectedItem();
	    if (s != null) {
	       if (edit_config == null) edit_config = launch_config;
	       if (edit_config != null && start_class != null) {
		  edit_config = edit_config.setMainClass(s);
		}
	     }
	  }
	 stop_in_main = pnl.addBoolean("Stop in Main",launch_config.getStopInMain(),this);
	 arg_area = pnl.addTextArea("Arguments",launch_config.getArguments(),2,24,this);
	 vmarg_area = pnl.addTextArea("VM Arguments",launch_config.getVMArguments(),1,24,this);
	 break;
      case JUNIT_TEST :
	 test_class = pnl.addTextField("Test Class",launch_config.getMainClass(),null,this);
	 test_name = pnl.addTextField("Test Name",launch_config.getTestName(),null,this);
	 arg_area = pnl.addTextArea("Arguments",launch_config.getArguments(),2,24,this);
	 vmarg_area = pnl.addTextArea("VM Arguments",launch_config.getVMArguments(),1,24,this);
	 break;
      case REMOTE_JAVA :
	 host_name = pnl.addTextField("Remote Host",launch_config.getRemoteHost(),null,this);
	 port_number = pnl.addNumericField("Remote Port",
	       1000,65536,launch_config.getRemotePort(),this);
	 break;
      case PYTHON :
	 start_class = pnl.addChoice("Module to Run",starts,launch_config.getMainClass(),true,this);
	 arg_area = pnl.addTextArea("Arguments",launch_config.getArguments(),2,24,this);
	 vmarg_area = pnl.addTextArea("VM Arguments",launch_config.getVMArguments(),1,24,this);
	 break;
      case JS :
	 start_class = pnl.addChoice("Module to Run",starts,launch_config.getMainClass(),true,this);
	 if (launch_config.getMainClass() == null) {
	    String s = (String) start_class.getSelectedItem();
	    if (s != null) launch_config.setMainClass(s);
	 }
	 arg_area = pnl.addTextArea("Arguments",launch_config.getArguments(),2,24,this);
	 vmarg_area = pnl.addTextArea("VM Arguments",launch_config.getVMArguments(),1,24,this);
	 break;
      default:
	 break;
    }

   FileSystemView fsv = BoardFileSystemView.getFileSystemView(); 
   log_file = pnl.addFileField("Record Output",launch_config.getLogFile(),
				  JFileChooser.FILES_ONLY,null,fsv,null,null,this);
   working_directory = pnl.addFileField("Working Directory",launch_config.getWorkingDirectory(),
					   JFileChooser.DIRECTORIES_ONLY,null,fsv,null,null,this);
   pnl.addSeparator();
   debug_button = pnl.addBottomButton("Debug","DEBUG",this);
   save_button = pnl.addBottomButton("Save","SAVE",this);
   revert_button = pnl.addBottomButton("Revert","REVERT",this);
   clone_button = pnl.addBottomButton("Clone","CLONE",this);
   pnl.addBottomButtons();
   fixButtons();

   doing_load = false;

   setContentPane(pnl,arg_area);
}



private void getStartClasses()
{
   project_map = new TreeMap<>();

   BassRepository br = BassFactory.getRepository(BassConstants.SearchType.SEARCH_CODE);
   for (BassName bn : br.getAllNames()) {
      switch (launch_config.getConfigType()) {
         case UNKNOWN :
            break;
         case JAVA_APP :
            if (bn.getName().endsWith(".main") &&
                  bn.getNameType() == BassNameType.METHOD &&
                  Modifier.isPublic(bn.getModifiers()) &&
                  Modifier.isStatic(bn.getModifiers())) {
               String cn = bn.getClassName();
               String pn = bn.getPackageName();
               if (pn != null) cn = pn + "." + cn;
               project_map.put(cn,bn.getProject());
             }
            break;
         case JUNIT_TEST :
         case REMOTE_JAVA :
            break;
         case PYTHON :
            if (bn.getNameType() == BassNameType.MODULE) {
               String cn = bn.getPackageName();
               project_map.put(cn,bn.getProject());
             }
            break;
         case JS :
            if (bn.getNameType() == BassNameType.MODULE) {
               String cn = bn.getPackageName();
               project_map.put(cn,bn.getProject());
             }
            break;
       }
    }
}


private Collection<String> getStartClassesForProject()
{
   if (project_map == null) getStartClasses();
   if (project_name == null) return project_map.keySet();
   String pnm = (String) project_name.getSelectedItem();
   if (pnm == null || pnm.length() == 0) return project_map.keySet();
   
   List<String> rslt = new ArrayList<>();
   for (String s : project_map.keySet()) {
      if (pnm.equals(project_map.get(s))) rslt.add(s);
    }
   
   return rslt;
}



private void fixButtons()
{
   if (edit_config == launch_config) edit_config = null;

   if (debug_button == null) return;

   boolean working = (edit_config != null || launch_config.isWorkingCopy());

   debug_button.setEnabled(!working);
   save_button.setEnabled(working);
   revert_button.setEnabled(edit_config != null);
   clone_button.setEnabled(true);
}


private void reload()
{
   doing_load = true;

   if (launch_name != null) launch_name.setText(launch_config.getConfigName());
   if (arg_area != null) arg_area.setText(launch_config.getArguments());
   if (vmarg_area != null) vmarg_area.setText(launch_config.getVMArguments());
   if (start_class != null) start_class.setSelectedItem(launch_config.getMainClass());
   if (test_class != null) test_class.setText(launch_config.getMainClass());
   if (test_name != null) test_name.setText(launch_config.getTestName());
   if (host_name != null) host_name.setText(launch_config.getRemoteHost());
   if (port_number != null) port_number.setValue(launch_config.getRemotePort());
   
   doing_load = false;
}


private void reloadRepository()
{
   BassRepository  brep = BassFactory.getRepository(BudaConstants.SearchType.SEARCH_LAUNCH_CONFIG);
   BassFactory.reloadRepository(brep);
}


private String getNewName()
{
   return launch_config.getConfigName() + " (2)";
}



/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

@Override protected void paintContentOverview(Graphics2D g,Shape s)
{
   Dimension sz = getSize();
   g.setColor(BoardColors.getColor(BDDT_LAUNCH_OVERVIEW_COLOR_PROP));
   g.fillRect(0, 0, sz.width, sz.height);
}




/********************************************************************************/
/*										*/
/*	Bubble outputer methods 						*/
/*										*/
/********************************************************************************/

@Override public String getConfigurator()
{
   return "BDDT";
}


/********************************************************************************/
/*										*/
/*	Popup methods								*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   JPopupMenu menu = new JPopupMenu();

   menu.add(getFloatBubbleAction());

   menu.show(this,e.getX(),e.getY());
}



@Override public void outputXml(BudaXmlWriter xw)
{
   xw.field("TYPE","LAUNCHBUBBLE");
   xw.field("CONFIG",launch_config.getConfigName());
}



/********************************************************************************/
/*										*/
/*	Button handler								*/
/*										*/
/********************************************************************************/

@Override public void actionPerformed(ActionEvent e)
{
   String cmd = e.getActionCommand();

   if (cmd.equals("DEBUG")) {
      if (edit_config != null) {
	 BumpLaunchConfig blc = edit_config.save();
	 if (blc != null) {
	    launch_config = blc;
	    edit_config = null;
	  }
	 reload();
         reloadRepository();
       }
      BddtFactory bf = BddtFactory.getFactory();
      bf.newDebugger(launch_config);
    }
   else if (cmd.equals("SAVE")) {
      BumpLaunchConfig blc = null;
      if (edit_config != null) blc = edit_config.save();
      else if (launch_config.isWorkingCopy()) blc = launch_config.save();
      if (blc != null) {
	 launch_config = blc;
	 edit_config = null;
       }
      reload();
      reloadRepository();
    }
   else if (cmd.equals("REVERT")) {
      if (edit_config != null) {
	 edit_config = null;
	 reload();
       }
    }
   else if (cmd.equals("CLONE")) {
      BumpLaunchConfig blc = launch_config.clone(getNewName());
      if (edit_config != null) {
	 blc = blc.setArguments(edit_config.getArguments());
	 blc = blc.setVMArguments(edit_config.getVMArguments());
	 blc = blc.setMainClass(edit_config.getMainClass());
       }
      if (blc != null) {
	 BddtLaunchBubble bbl = new BddtLaunchBubble(blc);
	 BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
	 Rectangle loc = BudaRoot.findBudaLocation(this);
	 bba.addBubble(bbl,loc.x + loc.width + 25,loc.y);
       }
    }
   else if (doing_load) return;
   else if (cmd.equals("Start Class")) {
      if (edit_config == null) edit_config = launch_config;
      if (edit_config != null && start_class != null) {
         String cls = (String) start_class.getSelectedItem();
	 edit_config = edit_config.setMainClass(cls);
         if (cls != null) {
            String proj = project_map.get(cls);
            if (proj != null && project_name != null) project_name.setSelectedItem(proj);
          }
       }
    }
   else if (cmd.equals("Module to Run")) {
      if (edit_config == null) edit_config = launch_config;
      if (edit_config != null && start_class != null) {
         edit_config = edit_config.setMainClass((String) start_class.getSelectedItem());
       }
    }
   else if (cmd.equals("Stop in Main")) {
      if (edit_config == null) edit_config = launch_config;
      if (edit_config != null && stop_in_main != null) {
	 edit_config = edit_config.setStopInMain(stop_in_main.isSelected());
       }
    }
   else if (cmd.equals("Project")) {
      if (project_name == null) return;
      String pnm = (String) project_name.getSelectedItem();
      if (pnm != null && edit_config != null && edit_config.getProject().equals(pnm)) return;
      if (pnm != null && edit_config == null && launch_config != null &&
	    launch_config.getProject().equals(pnm)) return;

      if (edit_config == null) edit_config = launch_config;
      if (edit_config != null) {
	 edit_config = edit_config.setProject(pnm);
	 if (start_class != null) {
            Collection<String> starts = getStartClassesForProject();
	    String nm = edit_config.getMainClass();
	    boolean fnd = false;
	    start_class.removeAllItems();
            String st0 = null;
	    for (String s : starts) {
               if (st0 == null) st0 = s;
               if (s.equals(nm)) {
                  nm = s;               // so objects == in selection
                  fnd = true;
                }
               start_class.addItem(s);
	     }
	    if (!fnd) {
               nm = st0;
             }	   
            start_class.setSelectedItem(nm);
            start_class.repaint();
	  }
       }
    }
   else if (cmd.equals("Remote Host")) { }
   else if (cmd.equals("Remote Port")) {
      if (edit_config == null) edit_config = launch_config;
      if (host_name != null && port_number != null) {
	 edit_config = edit_config.setRemoteHostPort(host_name.getText(),(int) port_number.getValue());
       }
    }
   else if (cmd.equals("comboBoxEdited")) ;
   else BoardLog.logE("BDDT","Undefined launch config action: " + cmd);

   fixButtons();
}


@Override public void undoableEditHappened(UndoableEditEvent e)
{
   Document doc = (Document) e.getSource();

   if (doing_load) return;

   if (isArea(arg_area,doc)) {
      if (edit_config == null) edit_config = launch_config;
      edit_config = edit_config.setArguments(arg_area.getText());
    }
   else if (isArea(vmarg_area,doc)) {
      if (edit_config == null) edit_config = launch_config;
      edit_config = edit_config.setVMArguments(vmarg_area.getText());
    }
   else if (isArea(test_name,doc)) {
      if (edit_config == null) edit_config = launch_config;
      edit_config = edit_config.setTestName(test_name.getText());
    }
   else if (isArea(test_class,doc)) {
      if (edit_config == null) edit_config = launch_config;
      edit_config = edit_config.setMainClass(test_class.getText());
    }
   else if (isArea(host_name,doc) || isArea(port_number,doc)) {
      if (edit_config == null) edit_config = launch_config;
      if (host_name != null && port_number != null) {
	 edit_config = edit_config.setRemoteHostPort(host_name.getText(),(int) port_number.getValue());
       }
    }
   else if (isArea(launch_name,doc)) {
      if (edit_config == null) edit_config = launch_config;
      edit_config = edit_config.setConfigName(launch_name.getText().trim());
    }
   else if (isArea(log_file,doc)) {
      if (edit_config == null) edit_config = launch_config;
      String nm = log_file.getText().trim();
      if (nm.length() == 0) nm = null;
      edit_config = edit_config.setLogFile(nm);
    }
   else if (isArea(working_directory,doc)) {
      if (edit_config == null) edit_config = launch_config;
      String nm = working_directory.getText().trim();
      if (nm.length() == 0) nm = null;
      edit_config = edit_config.setWorkingDirectory(nm);
    }

   fixButtons();
}


private boolean isArea(JTextComponent tc,Document d)
{
   if (tc == null) return false;
   if (tc.getDocument() == d) return true;
   return false;
}



}	// end of class BddtLaunchBubble




/* end of BddtLaunchBubble.java */
