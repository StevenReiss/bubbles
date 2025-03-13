/********************************************************************************/
/*                                                                              */
/*              BuenoGenericProject.java                                        */
/*                                                                              */
/*      Project creation/editing for all languages                              */
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



package edu.brown.cs.bubbles.bueno;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.w3c.dom.Element;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.xml.IvyXml;

class BuenoGenericProject implements BuenoConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Element         project_data;
private BuenoProperties create_props;
private BuenoGenericProjectMaker create_type;
private List<BuenoGenericProjectMaker> create_types;
private BuenoGenericProjectMaker default_type;
private Map<BuenoGenericProjectMaker,JPanel> panel_map;
private BuenoGenericProjectEditor project_editor;
      
private JButton         create_button;

private static final String NAME_PAT = "\\p{Alpha}\\w*";


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BuenoGenericProject(Element xml)
{
   project_data = xml;
   create_props = null;
   panel_map = new HashMap<>();
   
   create_types = new ArrayList<>();
   default_type = null;
   Element create = IvyXml.getChild(xml,"CREATE");
   for (Element typelt : IvyXml.children(create,"TYPE")) {
      BuenoGenericProjectMaker pm = new BuenoGenericProjectMaker(xml,typelt);
      create_types.add(pm);
      if (default_type == null || IvyXml.getAttrBool(typelt,"DEFAULT")) default_type = pm;
    }
   create_type = default_type;
}



/********************************************************************************/
/*                                                                              */
/*      Project Creation bubble                                                 */
/*                                                                              */
/********************************************************************************/

BudaBubble createProjectCreationBubble()
{
   create_props = new BuenoProperties();
   
   try {
      return new CreateBubble();
    }
   catch (BuenoException e) {
      return null;
    }
}


private class CreateBubble extends BudaBubble {

   private static final long serialVersionUID = 1;
   
   CreateBubble() throws BuenoException {
      JPanel cnts = getCreationPanel();
      setContentPane(cnts);
   }

}	// end of inner class CreateBubble


private JPanel getCreationPanel()
{
   SwingGridPanel pnl = new SwingGridPanel();
   pnl.beginLayout();
   pnl.addBannerLabel("Bubbles " + IvyXml.getAttrString(project_data,"LANGUAGE") + 
         " Project Creator");
   String projnm = create_props.getStringProperty(PROJ_PROP_NAME);
   NameAction na = new NameAction();
   pnl.addTextField("Project Name",projnm,24,na,na);
   pnl.addSeparator();
   
   pnl.addChoice("Project Type",create_types,create_type,new TypeAction());
   pnl.addSeparator();
   
   for (BuenoGenericProjectMaker pm : create_types) {
      JPanel pmpnl = pm.createPanel(this,create_props);
      panel_map.put(pm,pmpnl);
      pmpnl.setVisible(false);
      pnl.addLabellessRawComponent(pm.getName(),pmpnl);
    }
   
   pnl.addSeparator();
   create_button = pnl.addBottomButton("CREATE","CREATE PROJECT",new CreateAction());
   pnl.addBottomButtons();
   
   setVisibilities();
   checkStatus();
   
   return pnl; 
}


private void setVisibilities()
{
   Container par = null;
   for (BuenoGenericProjectMaker pm : create_types) {
      JPanel pmpnl = panel_map.get(pm);
      if (pm == create_type) {
         if (!pmpnl.isVisible()) {
            pmpnl.setVisible(true);
            pm.resetPanel(pmpnl,create_props);
            par = pmpnl.getParent();
          }
       }
      else {
         pmpnl.setVisible(false);
       }
    }
   if (par != null) {
      if (par.getParent() != null) par = par.getParent();
      par.invalidate();
      par.validate();
      Dimension sz = par.getPreferredSize();
      par.setSize(sz);
    }
}



public void checkStatus()
{
   File pdir = create_props.getFile(PROJ_PROP_DIRECTORY);
   boolean isok = pdir != null;
   isok &= create_type.checkStatus(create_props);
   if (create_button != null) create_button.setEnabled(isok);
}


private final class NameAction implements ActionListener, UndoableEditListener {
   
   @Override public void actionPerformed(ActionEvent evt) {
      setProjectDirectory();
      checkStatus();
    }
   
   @Override public void undoableEditHappened(UndoableEditEvent evt) {
      Document d = (Document) evt.getSource();
      String txt = null;
      try {
         txt = d.getText(0,d.getLength());
       }
      catch (BadLocationException e) { }
      create_props.put(PROJ_PROP_NAME,txt);
      setProjectDirectory();
      
      checkStatus();
    }
   
   private void setProjectDirectory() {
      String pnm = create_props.getStringProperty(PROJ_PROP_NAME);
      File pdir = null;
      if (pnm != null && pnm.length() > 0 && pnm.matches(NAME_PAT)) {
         BoardSetup bs = BoardSetup.getSetup();
         File f1 = new File(bs.getDefaultWorkspace());
         File f2 = new File(f1,pnm);
         if (!f2.exists()) pdir = f2;
       }
      create_props.put(PROJ_PROP_DIRECTORY,pdir);
    }
   
}	// end of inner class NameAction



private final class CreateAction implements ActionListener, Runnable {
   
   @Override public void actionPerformed(ActionEvent evt) {
      JComponent c = (JComponent) evt.getSource();
      BudaBubble bb = BudaRoot.findBudaBubble(c);
      if (bb != null) bb.setVisible(false);
      BoardThreadPool.start(this);
    }
   
   @Override public void run() {
      if (!createProject()) {
         // put up error message
       }
      else {
	 // possibly bring up project editor dialog here on success
      }
   }
   
   
}	// end of inner class CreateAction


private final class TypeAction implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      JComboBox<?> fld = (JComboBox<?>) evt.getSource();
      Object obj = fld.getSelectedItem();
      if (obj == null) return;
      create_type = (BuenoGenericProjectMaker) obj;
      setVisibilities();
    }
}




/********************************************************************************/
/*                                                                              */
/*      Code to actually create the project                                     */
/*                                                                              */
/********************************************************************************/

private boolean createProject()
{
   String pnm = create_props.getStringProperty(PROJ_PROP_NAME);
   File pdir = create_props.getFile(PROJ_PROP_DIRECTORY);
   
   if (!pdir.mkdir()) return false;
   File bdir = new File(pdir,"bin");
   if (!bdir.mkdir()) return false;
   
   String giturl = create_props.getStringProperty("GIT_URL");
   File gitdir = create_props.getFile("GIT_DIR");
   if (gitdir == null) gitdir = pdir;
   if (giturl != null && gitdir != null) {
      if (!gitClone(giturl,gitdir)) return false;
    }
   
   create_props.put(PROJ_PROP_JUNIT_PATH,
         BoardSetup.getSetup().getLibraryPath("junit.jar"));
   create_props.put(PROJ_PROP_USE_ANDROID,
         BumpClient.getBump().getOptionBool("bedrock.useAndroid",false));
   BoardProperties props = BoardProperties.getProperties("Bueno");
   create_props.put(PROJ_PROP_CORE_OPTIONS,
         props.getProperty("Bueno.problem.set.data.1"));
   File f1 = BoardSetup.getPropertyBase();
   File f2 = new File(f1,"formats.xml");
   create_props.put(PROJ_PROP_FORMAT_FILE,f2);
   create_props.put(PROJ_PROP_LANGUAGE,create_type.getLanguage());
   create_props.put(PROJ_PROP_TYPE,create_type.getName());
   
   BumpClient bc = BumpClient.getBump();
   if (!bc.createProject(pnm,pdir,create_type.getName(),create_props)) {
      try {
	 IvyFile.remove(pdir);
       }
      catch (IOException e) { }
      return false;
    }
   
   return true;
}



private boolean gitClone(String giturl,File dir)
{
   if (!dir.exists() && !dir.mkdir()) {
      return false;
    }
   if (!dir.isDirectory()) {
      return false;
    }
   BoardProperties vcrprop = BoardProperties.getProperties("Bvcr");
   String gitcmd = vcrprop.getProperty("bvcr.git.command","git");
   String cmd = gitcmd + " clone -q " + giturl + " " + dir;
   try {
      BoardLog.logD("BUENO","Issue GIT command: " + cmd);
      IvyExec ex = new IvyExec(cmd,dir,IvyExec.IGNORE_OUTPUT);
      int sts = ex.waitFor();
      if (sts > 0) {
         return false;
       }
    }
   catch (IOException e) {
      return false;
    }  
   
   return true;
}




/********************************************************************************/
/*                                                                              */
/*      Project editor                                                          */
/*                                                                              */
/********************************************************************************/

BudaBubble createProjectEditorBubble(String proj)
{
   Element editxml = IvyXml.getChild(project_data,"EDIT");
   BumpClient bc = BumpClient.getBump();
   Element pxml = bc.getProjectData(proj);
   
   create_props = new BuenoProperties();
   
   try {
      return new EditorBubble(pxml,editxml);
    }
   catch (BuenoException e) {
      return null;
    }
}


private class EditorBubble extends BudaBubble {
   
   private static final long serialVersionUID = 1;
   
   EditorBubble(Element projxml,Element editxml) throws BuenoException {
      JPanel cnts = getEditorPanel(projxml,editxml);
      setContentPane(cnts);
    }
   
}	// end of inner class CreateBubble


private JPanel getEditorPanel(Element projxml,Element editxml)
{
   SwingGridPanel pnl = new SwingGridPanel();
   BoardColors.setColors(pnl,"Bueno.project.editor.background");
   
   String pnm = IvyXml.getAttrString(projxml,"NAME");
   
   JLabel lbl = new JLabel("Properties for Project " + pnm,SwingConstants.CENTER);
   BoardColors.setTransparent(lbl,pnl);
   pnl.addGBComponent(lbl,0,0,0,1,0,0);
   
   JTabbedPane tbp = new JTabbedPane(SwingConstants.TOP);
   BoardColors.setColors(tbp,"Bueno.project.editor.background");
   
   project_editor = new BuenoGenericProjectEditor(editxml,projxml);
   for (Element tabelt : IvyXml.children(editxml,"TAB")) {
      BuenoGenericEditorPanel ep = project_editor.generateTabPanel(tabelt);
      JPanel tpnl = ep.getPanel();
      if (tpnl != null) {
         String nm = IvyXml.getAttrString(tabelt,"LABEL");
         tbp.addTab(nm,tpnl);
       }
    }
   
   pnl.addGBComponent(tbp, 0, 1, 0, 1, 1, 1);
   
   Box bx = Box.createHorizontalBox();
   bx.add(Box.createHorizontalGlue());
   JButton apply = new JButton("Apply Changes");
   apply.addActionListener(new EditListener());
   bx.add(apply);
   bx.add(Box.createHorizontalGlue());
   pnl.addGBComponent(bx,0,2,0,1,0,0);
   
   Dimension d = pnl.getPreferredSize();
   if (d.width < 400) {
      d.width = 400;
      pnl.setPreferredSize(d);
    }
   
   return pnl; 
}



private final class EditListener implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      project_editor.saveProject();
    }
}





}       // end of class BuenoGenericProject




/* end of BuenoGenericProject.java */

