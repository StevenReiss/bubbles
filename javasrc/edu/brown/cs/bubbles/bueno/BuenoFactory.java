/********************************************************************************/
/*										*/
/*		BuenoFactory.java						*/
/*										*/
/*	BUbbles Environment New Objects creator factory 			*/
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

import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.swing.SwingComboBox;
import edu.brown.cs.ivy.swing.SwingEventListenerList;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.xml.IvyXml;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JFileChooser;
import javax.swing.JTextField;

import org.w3c.dom.Element;



public class BuenoFactory implements BuenoConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private SwingEventListenerList<BuenoInserter> insertion_handlers;
private Map<BuenoMethod,BuenoCreator> creation_map;
private BuenoCreator		cur_creator;
private BuenoMethodCreatorInstance method_creator;
private BuenoClassCreatorInstance class_creator;
private BuenoPackageCreatorInstance package_creator;
private BuenoClassMethodFinder	method_finder;

private static BuenoFactory	the_factory = null;

private static final String WORKSPACE_NAME = " WORKSPACE ";



public static synchronized BuenoFactory getFactory()
{
   if (the_factory == null) {
      the_factory = new BuenoFactory();
    }

   return the_factory;
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BuenoFactory()
{
   insertion_handlers = new SwingEventListenerList<>(BuenoInserter.class);
   creation_map = new HashMap<>();
   creation_map.put(BuenoMethod.METHOD_SIMPLE,new BuenoCreatorSimple());
   creation_map.put(BuenoMethod.METHOD_ECLIPSE,new BuenoCreatorEclipse());
   creation_map.put(BuenoMethod.METHOD_TEMPLATE,new BuenoCreatorTemplate());
   creation_map.put(BuenoMethod.METHOD_USER,new BuenoCreatorUser());

   method_creator = null;
   class_creator = null;
   package_creator = null;
   method_finder = null;

   BoardProperties bp = BoardProperties.getProperties("Bueno");
   BuenoMethod mthd = bp.getEnum(BUENO_CREATION_METHOD,"METHOD_",BuenoMethod.METHOD_TEMPLATE);
   setCreationMethod(mthd);
}



/********************************************************************************/
/*										*/
/*	Initialization methods							*/
/*										*/
/********************************************************************************/

public static void setup()
{
   new BuenoProjectMakerNew();		// default
   new BuenoProjectMakerSource();
   new BuenoProjectMakerTemplate();
   new BuenoProjectMakerGit();
   // new BuenoProjectMakerAnt();

   BudaRoot.registerMenuButton("Create New Project",new CreateListener());

   BudaRoot.registerMenuButton("Admin.Admin.Import Templates",new TemplateImporter());
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public void addInsertionHandler(BuenoInserter bi)
{
   insertion_handlers.add(bi);
}



public void removeInsertionHandler(BuenoInserter bi)
{
   insertion_handlers.remove(bi);
}


public void setCreationMethod(BuenoMethod bm)
{
   cur_creator = creation_map.get(bm);
}


BuenoCreator getCreator()
{
   return cur_creator;
}



/********************************************************************************/
/*										*/
/*	Location creation methods						*/
/*										*/
/********************************************************************************/

public BuenoLocation createLocation(BumpLocation l,boolean before)
{
   return new BuenoLocationBump(l,before);
}


public BuenoLocation createLocation(String proj,String pkgcls,String insert,boolean after)
{
   return new BuenoLocationStatic(proj,pkgcls,insert,after);
}



/********************************************************************************/
/*										*/
/*	Dialog setup methods							*/
/*										*/
/********************************************************************************/

public void setMethodDialog(BuenoMethodCreatorInstance bmc)
{
   method_creator = bmc;
}

public void setClassDialog(BuenoClassCreatorInstance bcc)
{
   class_creator = bcc;
}

public void setPackageDialog(BuenoPackageCreatorInstance pcc)
{
   package_creator = pcc;
}


public void setClassMethodFinder(BuenoClassMethodFinder fdr)
{
   method_finder = fdr;
}



public void createMethodDialog(BudaBubble src,Point loc,BuenoProperties known,
				  BuenoLocation insert,String lbl,
				  BuenoBubbleCreator newer)
{
   if (method_creator != null) {
      if (method_creator.showMethodDialogBubble(src,loc,known,insert,lbl,newer))
	 return;
    }

   BuenoMethodDialog md = new BuenoMethodDialog(src,loc,known,insert,newer);
   if (lbl != null) md.setLabel(lbl);
   md.showDialog();
}




public void createClassDialog(BudaBubble src,Point loc,BuenoType type,
      BuenoProperties known,
      BuenoLocation insert,String lbl,BuenoBubbleCreator newer)
{
   if (class_creator != null) {
      if (class_creator.showClassDialogBubble(src,loc,type,known,insert,lbl,newer))
	 return;
    }

   BuenoClassDialog cd = new BuenoClassDialog(src,loc,type,known,insert,newer);
   if (lbl != null) cd.setLabel(lbl);
   cd.showDialog();
}



public void createPackageDialog(BudaBubble src,Point loc,BuenoType type,
      BuenoProperties known,
      BuenoLocation insert,String lbl,BuenoBubbleCreator newer)
{
   if (package_creator != null) {
      if (package_creator.showPackageDialogBubble(src,loc,type,known,insert,lbl,newer))
	 return;
    }
   
   BuenoPackageDialog pd = new BuenoPackageDialog(src,loc,known,insert,newer);
   if (lbl != null) pd.setLabel(lbl);
   pd.showDialog();
}



public BudaBubble createProjectDialog(String proj)
{
   BuenoProjectDialog dlg = new BuenoProjectDialog(proj);
   BudaBubble bb = dlg.createProjectEditor();
   return bb;
}



public boolean useSeparateTypeButtons()
{
   if (class_creator == null) return false;

   return class_creator.useSeparateTypeButtons();
}


List<BumpLocation> findClassMethods(String name)
{
   if (method_finder == null) return  null;

   return method_finder.findClassMethods(name);
}




/********************************************************************************/
/*										*/
/*	Methods for doing the creation						*/
/*										*/
/********************************************************************************/

public void createNew(BuenoType what,BuenoLocation where,BuenoProperties props)
{
   if (props == null) props = new BuenoProperties();

   CharSequence result = null;

   fixupProps(what,where,props);

   switch (what) {
      case NEW_PACKAGE :
	 cur_creator.createPackage(where,props);
	 break;
      case NEW_MODULE :
	 cur_creator.createModule(where,props);
	 break;
      case NEW_TYPE :
      case NEW_CLASS :
      case NEW_INTERFACE :
      case NEW_ENUM :
      case NEW_ANNOTATION :
	 BuenoClassSetup bcs = new BuenoClassSetup(props,what);
	 bcs.extendProperties();
	 cur_creator.createType(what,where,props);
	 break;
      case NEW_INNER_TYPE :
      case NEW_INNER_CLASS :
      case NEW_INNER_INTERFACE :
      case NEW_INNER_ENUM :
      case NEW_CONSTRUCTOR :
      case NEW_METHOD :
      case NEW_GETTER :
      case NEW_SETTER :
      case NEW_GETTER_SETTER :
      case NEW_FIELD :
      case NEW_MARQUIS_COMMENT :
      case NEW_BLOCK_COMMENT :
      case NEW_JAVADOC_COMMENT :
	 result = setupNew(what,where,props);
	 insertText(where,result.toString(),
	       props.getBooleanProperty(BuenoKey.KEY_REFORMAT));
	 break;
    }
}




public CharSequence setupNew(BuenoType what,BuenoLocation where,BuenoProperties props)
{
   if (props == null) props = new BuenoProperties();

   CharSequence result = null;

   fixupProps(what,where,props);

   switch (what) {
      case NEW_PACKAGE :
      case NEW_MODULE :
      case NEW_TYPE :
      case NEW_CLASS :
      case NEW_INTERFACE :
      case NEW_ENUM :
      case NEW_ANNOTATION :
	 throw new IllegalArgumentException("Can only setup internal items");
	
      case NEW_INNER_TYPE :
      case NEW_INNER_CLASS :
      case NEW_INNER_INTERFACE :
      case NEW_INNER_ENUM :
	 result = cur_creator.createInnerType(what,props);
	 break;
      case NEW_CONSTRUCTOR :
      case NEW_METHOD :
      case NEW_GETTER :
      case NEW_SETTER :
      case NEW_GETTER_SETTER :
	 result = cur_creator.createMethod(what,props);
	 break;
      case NEW_FIELD :
	 result = cur_creator.createField(props);
	 break;
      case NEW_MARQUIS_COMMENT :
      case NEW_BLOCK_COMMENT :
      case NEW_JAVADOC_COMMENT :
	 result = cur_creator.createComment(what,props);
	 break;
    }

  return result;
}


private void fixupProps(BuenoType what,BuenoLocation where,BuenoProperties props)
{
   if (where == null) return;

   String prj = props.getStringProperty(BuenoKey.KEY_PROJECT);
   if (prj == null && where.getProject() != null) {
      props.put(BuenoKey.KEY_PROJECT,where.getProject());
    }
   String pkg = props.getStringProperty(BuenoKey.KEY_PACKAGE);
   if (pkg == null && where.getPackage() != null) {
      props.put(BuenoKey.KEY_PACKAGE,where.getPackage());
    }
   String fil = props.getStringProperty(BuenoKey.KEY_FILE);
   if (fil == null && where.getFile() != null) {
      switch (what) {
	 case NEW_PACKAGE :
	 case NEW_TYPE :
	 case NEW_CLASS :
	 case NEW_INTERFACE :
	 case NEW_ENUM :
	 case NEW_ANNOTATION :
	 case NEW_MODULE :
	    break;
	 default :
	    props.put(BuenoKey.KEY_FILE,where.getFile().getPath());
	    props.put(BuenoKey.KEY_FILETAIL,where.getFile().getName());
	    break;
       }
    }
   String clsnm = props.getStringProperty(BuenoKey.KEY_CLASS_NAME);
   if (clsnm == null && where.getClassName() != null) {
      props.put(BuenoKey.KEY_CLASS_NAME,where.getClassName());
    }
}


/********************************************************************************/
/*										*/
/*	Insertion methods							*/
/*										*/
/********************************************************************************/

boolean insertText(BuenoLocation loc,String text,boolean format)
{
   if (text == null || text.length() == 0) return false;

   for (BuenoInserter bi : insertion_handlers) {
      if (bi.insertText(loc,text,format)) return true;
    }

   return false;
}


/********************************************************************************/
/*										*/
/*	Methods for doing create project					*/
/*										*/
/********************************************************************************/

public BudaBubble getCreateProjectBubble()
{
   switch (BoardSetup.getSetup().getLanguage()) {
      case JAVA :
      case JAVA_IDEA :
	 BuenoProjectCreator bpc = new BuenoProjectCreator();
	 return bpc.createProjectCreationBubble();
      case PYTHON :
	 return BuenoPythonProject.createNewPythonProjectBubble();
      case JS :
	 return BuenoJsProject.createNewJsProjectBubble();
      case REBUS :
	 return null;
      default :
	 return null;
    }
}


private static class CreateListener implements BudaConstants.ButtonListener {

   @Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt) {
      BuenoFactory bf = getFactory();
      BudaBubble bb = bf.getCreateProjectBubble();
      if (bb == null) return;
      bba.addBubble(bb,null,pt,BudaConstants.PLACEMENT_LOGICAL|
		       BudaConstants.PLACEMENT_MOVETO |
		       BudaConstants.PLACEMENT_USER);
    }

}	// end of inner class CreateListener



/********************************************************************************/
/*										*/
/*	Handle importing templates						*/
/*										*/
/********************************************************************************/

private static class TemplateImporter implements BudaConstants.ButtonListener {


   @Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt) {
      TemplateBubble bbl = new TemplateBubble();
      bba.addBubble(bbl,null,pt,BudaConstants.PLACEMENT_USER);
    }

}	// end of inner calss TemplateImporter



private static class TemplateBubble extends BudaBubble {

   TemplateBubble() {
      TemplatePanel pnl = new TemplatePanel();
      setContentPane(pnl);
    }

}	// end of inner class TemplateBubble




private static class TemplatePanel extends SwingGridPanel implements ActionListener {

   private JTextField file_field;
   private SwingComboBox<String> source_field;
   private SwingComboBox<String> forproject_field;
   private String target_project;
   
   TemplatePanel() {
      beginLayout();
      addBannerLabel("Choose Templates to Import");
      
      String ws = BoardSetup.getSetup().getDefaultWorkspace();
      int idx = ws.lastIndexOf(File.separator);
      if (idx > 0) ws = ws.substring(idx+1);
      
      forproject_field = null;
      target_project = null;
      BumpClient bc = BumpClient.getBump();
      Element projects = bc.getAllProjects();
      if (projects != null) {
         SortedSet<String> pset = new TreeSet<String>();
         pset.add(WORKSPACE_NAME);
         for (Element pe : IvyXml.children(projects,"PROJECT")) {
            String pnm = IvyXml.getAttrString(pe,"NAME");
            if (pnm.equals(ws)) continue;
            pset.add(pnm);
          }
         if (pset.size() > 1) {
            forproject_field = addChoice("Templates for Project",pset,pset.first(),null);
          }
         else if (pset.size() == 1) target_project = pset.first();
       }
      
      file_field = addFileField("From File/Directory",(File) null,JFileChooser.FILES_AND_DIRECTORIES,null,null);
      File f1 = BoardSetup.getPropertyBase();
      File f2 = new File(f1,"templates");
      List<String> alts = new ArrayList<>();
      alts.add("Use File Specified Above");
      if (f2 .exists() && f2.listFiles() != null) {
         for (File f3 : f2.listFiles()) {
            if (f3.isDirectory() && f3.listFiles() != null) {
               boolean fnd = false;
               for (File f4 : f3.listFiles()) {
        	  if (f4.isFile() && f4.getName().endsWith(".template")) {
        	     fnd = true;
        	   }
        	}
               if (fnd) alts.add(f3.getName());
             }
          }
       }
      if (alts.size() > 1) {
         source_field = addChoice("Copy from Workspace ",alts,"None",null);
       }
      else source_field = null;
      addBottomButton("Import","IMPORT",this);
      addBottomButtons();
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BudaBubble bbl = BudaRoot.findBudaBubble((Component) evt.getSource());
      bbl.setVisible(false);
      if (forproject_field != null) {
         target_project = (String) forproject_field.getSelectedItem();
       }
      if (source_field == null || source_field.getSelectedIndex() <= 0) {
         String f = file_field.getText();
         if (f == null) return;
         if (f.length() == 0) return;
         File f1 = new File(f);
         if (!f1.exists()) f1 = new File(f.trim());
         loadTemplates(f1);
       }
      else {
         File f1 = BoardSetup.getPropertyBase();
         File f2 = new File(f1,"templates");
         File f3 = new File(f2,source_field.getSelectedItem().toString());
         loadTemplates(f3);
       }
    }

   private void loadTemplates(File f) {
      if (!f.exists()) return;
      File t = BoardSetup.getPropertyBase();
      File t1 = new File(t,"templates");
      if (target_project == null || target_project.equals(WORKSPACE_NAME)) {
         String ws = BoardSetup.getSetup().getDefaultWorkspace();
         int idx = ws.lastIndexOf(File.separator);
         if (idx > 0) ws = ws.substring(idx+1);
         target_project = ws;
       }
      target_project = target_project.toLowerCase();
      File t2 = new File(t1,target_project);
      t2.mkdirs();
      if (f.isDirectory()) {
         for (File f1 : f.listFiles()) {
            if (f1.getName().endsWith(".template")) {
               File t3 = new File(t2,f1.getName());
               try {
                  IvyFile.copyFile(f1,t3);
                }
               catch (IOException e) { }
             }
          }
       }
      else if (f.getName().endsWith(".zip") || f.getName().endsWith(".jar")) {
         try {
            ZipFile zf = new ZipFile(f);
            for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements(); ) {
               ZipEntry ze = e.nextElement();
               String nm = ze.getName();
               if (nm.endsWith(".template")) {
                  File t3 = new File(t2,nm);
                  InputStream ins = zf.getInputStream(ze);
                  IvyFile.copyFile(ins,t3);
                }
             }
            zf.close();
          }
         catch (IOException e) { }
       }
   
    }

}	// end of inner class TemplatePanel


}	// end of class BuenoFactory




/* end of BuenoFactory.java */
