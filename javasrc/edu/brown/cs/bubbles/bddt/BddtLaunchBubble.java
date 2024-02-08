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
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.JTextField;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



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
private JButton 		debug_button;
private JButton 		save_button;
private JButton 		revert_button;
private JButton 		clone_button;
private SwingComboBox<String>	project_name;
private JTextComponent		log_file;
private JTextComponent		working_directory;
private boolean 		doing_load;
private Map<JComponent,String> component_items;


private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BddtLaunchBubble(BumpLaunchConfig cfg)
{
   bump_client = BumpClient.getBump();
   launch_config = cfg;
   edit_config = null;
   doing_load = false;
   setupPanel();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BumpLaunchConfig getLaunchConfig()
{
   return launch_config;
}



/********************************************************************************/
/*										*/
/*	Panel methods								*/
/*										*/
/********************************************************************************/

private void setupPanel()
{
   doing_load = true;

   component_items = new HashMap<>();
   
   SwingGridPanel pnl = new SwingGridPanel();
   BoardColors.setColors(pnl,"Bddt.launch.background");
   pnl.beginLayout();
   pnl.addBannerLabel("Launch " + launch_config.getLaunchType().getDescription());
   launch_name = pnl.addTextField("Launch Name",launch_config.getConfigName(),this,this);
   component_items.put(launch_name,"NAME");

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

   JComponent focus = null;
   BumpLaunchType blt = launch_config.getLaunchType();
   for (BumpLaunchConfigField fld : blt.getFields()) {
      JComponent cmp = null;
      String what = fld.getFieldName();
      String val = launch_config.getAttribute(what);
      switch (fld.getType()) {
	 case STRING :
	    JTextArea jta = pnl.addTextArea(fld.getDescription(),val,
                  fld.getNumRows(),24,this);
	    cmp = jta;
	    focus = jta;
	    break;
	 case INTEGER  :
	    int ivl = Integer.parseInt(val);
	    SwingNumericField snf = pnl.addNumericField(fld.getDescription(),
		  fld.getMin(),fld.getMax(),ivl,this);
	    cmp = snf;
	    break;
	 case CHOICE :
	    List<ChoiceItem> vals = getChoiceValues(blt,fld);
	    SwingComboBox<ChoiceItem> cmb = pnl.addChoice(fld.getDescription(),
		  vals,val,false,this);
//          if (val == null) {
//             ChoiceItem cval = (ChoiceItem) cmb.getSelectedItem();
//             if (cval == null && vals.size() > 0) cval = vals.get(0);
//             if (cval != null) {
//               edit_config = edit_config.setAttribute(what,cval.getValue());
//              }
//           }
	    cmp = cmb;
	    break;
	 case BOOLEAN :
	    boolean bvl = (val == null ? false : "Tt1Yy".indexOf(val.charAt(0)) >= 0);
	    JCheckBox cbx = pnl.addBoolean(fld.getDescription(),bvl,this);
	    cmp = cbx;
	    break;
         case PRESET :
	 default :
	    break;
       }
      if (cmp != null) component_items.put(cmp, what);
    }

   FileSystemView fsv = BoardFileSystemView.getFileSystemView();
   log_file = pnl.addFileField("Record Output",launch_config.getLogFile(),
				  JFileChooser.FILES_ONLY,null,fsv,this,null,this);
   component_items.put(log_file,"CAPTURE_IN_FILE");
   working_directory = pnl.addFileField("Working Directory",launch_config.getWorkingDirectory(),
					   JFileChooser.DIRECTORIES_ONLY,null,fsv,this,null,this);
   pnl.addSeparator();
   debug_button = pnl.addBottomButton("Debug","DEBUG",this);
   save_button = pnl.addBottomButton("Save","SAVE",this);
   revert_button = pnl.addBottomButton("Revert","REVERT",this);
   clone_button = pnl.addBottomButton("Clone","CLONE",this);
   pnl.addBottomButtons();
   fixButtons();

   doing_load = false;

   setContentPane(pnl,focus);
}




private List<ChoiceItem> getChoiceValues(BumpLaunchType lt,BumpLaunchConfigField fld)
{
   List<ChoiceItem> rslt = new ArrayList<>();

   String eval = fld.getEvaluate();
   String arg = fld.getArgField();
   String argv = null;
   if (arg != null) {
      argv = launch_config.getAttribute(arg);
   }
   BumpClient bc = BumpClient.getBump();
   String proj = launch_config.getProject();
   Element xml = bc.doLaunchQuery(proj,eval,argv);
   for (Element e : IvyXml.children(xml,"OPTION")) {
      String v = IvyXml.getAttrString(e, "VALUE");
      String d = IvyXml.getAttrString(e, "DISPLAY",v);
      rslt.add(new ChoiceItem(d,v));
   }

   return rslt;
}

private static class ChoiceItem {

   private String display_value;
   private String use_value;

   ChoiceItem(String disp,String use) {
      display_value = disp;
      use_value = use;
   }

   @Override public String toString()			{ return display_value; }

   public String getValue()				{ return use_value; }

   @Override public boolean equals(Object o) {
      if (o instanceof ChoiceItem) return o == this;
      else if (o instanceof String) {
	 String s = (String) o;
	 if (s.equals(display_value) || s.equals(use_value)) return true;
      }
      return false;
   }
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
   if (project_name != null) project_name.setSelectedItem(launch_config.getProject());
   if (log_file != null) log_file.setText(launch_config.getLogFile());
   if (working_directory != null) working_directory.setText(launch_config.getWorkingDirectory());

   for (Map.Entry<JComponent,String> ent : component_items.entrySet()) {
      String val = launch_config.getAttribute(ent.getValue());
      JComponent cmp = ent.getKey();
      if (cmp instanceof JTextArea) {
	 JTextArea ta = (JTextArea) cmp;
	 ta.setText(val);
      }
      else if (cmp instanceof SwingNumericField) {
	 SwingNumericField snf = (SwingNumericField) cmp;
	 int ival = Integer.parseInt(val);
	 snf.setValue(ival);
      }
      else if (cmp instanceof JCheckBox) {
	 JCheckBox cbx = (JCheckBox) cmp;
	 boolean bvl = launch_config.getBoolAttribute(ent.getValue());
	 cbx.setSelected(bvl);
      }
      else if (cmp instanceof SwingComboBox<?>) {
	 SwingComboBox<?> cmbo = (SwingComboBox<?>) cmp;
	 cmbo.setSelectedItem(val);
      }
   }

   recomputeStarts();

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
/*	Bubble output methods							*/
/*										*/
/********************************************************************************/

@Override public String getConfigurator()
{
   return "BDDT";
}


/********************************************************************************/
/*										*/
/*	Pop-up methods								*/
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
   
   String itm = component_items.get(e.getSource());
   
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
      for (JComponent jcmp : component_items.keySet()) {
         handleFieldSet(jcmp);
       }
      // need to ensure fields are set to shown values
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
   else if (itm != null && launch_config != null) {
      JComponent cmp = (JComponent) e.getSource();
      handleFieldSet(cmp);
      BumpLaunchType lt = edit_config.getLaunchType();
      for (BumpLaunchConfigField fld : lt.getFields()) {
         if (fld.getEvaluate() != null && fld.getEvaluate().equals("START") &&
               itm.equals(fld.getArgField())) {
            recomputeStarts();
          }
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
	 recomputeStarts();
       }
    }
   else if (cmd.equals("comboBoxEdited")) ;
   else BoardLog.logE("BDDT","Undefined launch config action: " + cmd);
   
   fixButtons();
}


private void handleFieldSet(JComponent cmp)
{
   String itm = component_items.get(cmp);
   if (itm == null) return;
   if (edit_config != null || launch_config != null) {
      BumpLaunchConfig cfg = edit_config;
      if (cfg == null) cfg = launch_config;
      String oval = cfg.getAttribute(itm);
      String nval = oval;
      if (cmp instanceof JTextArea) {
         JTextArea ta = (JTextArea) cmp;
         nval = ta.getText();
       }
      else if (cmp instanceof JTextField) {
         JTextField ta = (JTextField) cmp;
         nval = ta.getText();
       }
      else if (cmp instanceof SwingNumericField) {
         SwingNumericField snf = (SwingNumericField) cmp;
         int ival = (int) snf.getValue();
         nval = Integer.toString(ival);
       }
      else if (cmp instanceof JCheckBox) {
         JCheckBox cbx = (JCheckBox) cmp;
         boolean bval = cbx.isSelected();
         nval = Boolean.toString(bval);
       }
      else if (cmp instanceof SwingComboBox<?>) {
         SwingComboBox<?> cmbo = (SwingComboBox<?>) cmp;
         ChoiceItem ci = (ChoiceItem) cmbo.getSelectedItem();
         if (ci != null) {
            nval = ci.getValue();
          }
       }
      if (nval != null && nval.isEmpty()) nval = null;
      if (nval != null && !nval.equals(oval)) {
         if (edit_config == null) edit_config = launch_config;
         edit_config = edit_config.setAttribute(itm,nval);
       }
    }
}



@SuppressWarnings("unchecked")
private void recomputeStarts()
{
   BumpLaunchType lt = null;
   BumpLaunchConfig cfg = edit_config;
   if (cfg == null) cfg = launch_config;
   if (cfg != null) {
      lt = cfg.getLaunchType();
    }

   if (lt != null) {
      for (BumpLaunchConfigField fld : lt.getFields()) {
	 if (fld.getEvaluate() != null && fld.getEvaluate().equals("START")) {
	    SwingComboBox<ChoiceItem> combo = null;
	    for (JComponent cmp : component_items.keySet()) {
	       if (component_items.get(cmp).equals(fld.getFieldName())) {
		  combo = (SwingComboBox<ChoiceItem>) cmp;
		  break;
	       }
	    }
	    if (combo != null) {
	       List<ChoiceItem> vals = getChoiceValues(lt,fld);
	       String val = cfg.getAttribute(fld.getFieldName());
	       combo.removeAllItems();
	       ChoiceItem st0 = null;
	       ChoiceItem sel = null;
	       for (ChoiceItem itm : vals) {
		  if (st0 == null) st0 = itm;
		  else if (itm.getValue().equals(val)) {
		     sel = itm;
		  }
		  combo.addItem(itm);
	       }
	       if (sel == null) sel = st0;
	       combo.setSelectedItem(sel);
	       combo.repaint();
	    }
	 }
      }
   }
}



@Override public void undoableEditHappened(UndoableEditEvent e)
{
   Document doc = (Document) e.getSource();

   if (doing_load) return;

   boolean fnd = false;
   for (JComponent cmp : component_items.keySet()) {
      if (cmp instanceof JTextComponent) {
	 JTextComponent tc = (JTextComponent) cmp;
	 if (tc.getDocument() == doc) {
	    String val = tc.getText();
	    String itm = component_items.get(cmp);
            if (edit_config == null) edit_config = launch_config;
	    if (edit_config != null) edit_config = edit_config.setAttribute(itm,val);
            fnd = true;
	    break;
	 }
      }
   }

   if (fnd) ; 
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
