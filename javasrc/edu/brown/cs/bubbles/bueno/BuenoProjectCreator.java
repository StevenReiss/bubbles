/********************************************************************************/
/*										*/
/*		BuenoProjectCreator.java					*/
/*										*/
/*	Handle creation of new projects 					*/
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

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bueno;

import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;

import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import org.w3c.dom.Element;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StreamTokenizer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


public class BuenoProjectCreator implements BuenoConstants, BuenoConstants.BuenoProjectCreationControl
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private ProjProps	project_props;

private BuenoProjectMaker project_type;

private Map<BuenoProjectMaker,JPanel> panel_map;

private JTextField	name_field;
private JComboBox<String> type_field;
private JButton 	create_button;

static private Map<String,BuenoProjectMaker> type_names = new TreeMap<String,BuenoProjectMaker>();
static BuenoProjectMaker	default_type = null;

private static final String NAME_PAT = "\\p{Alpha}\\w*";



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public BuenoProjectCreator()
{
   project_props = new ProjProps();
   panel_map = new HashMap<>();
   project_type = default_type;
}


static void addProjectMaker(BuenoProjectMaker pm)
{
   type_names.put(pm.getLabel(),pm);
   if (default_type == null) default_type = pm;
}



/********************************************************************************/
/*										*/
/*	Methods to create an interaction bubble 				*/
/*										*/
/********************************************************************************/

public BudaBubble createProjectCreationBubble()
{
   return new CreateBubble();
}




/********************************************************************************/
/*										*/
/*	Bubble for creation							*/
/*										*/
/********************************************************************************/

private class CreateBubble extends BudaBubble {

   CreateBubble() {
      JPanel cnts = getJavaCreationPanel();
      setContentPane(cnts);
    }

}	// end of inner class CreateBubble




/********************************************************************************/
/*										*/
/*	Setup panel definitions 						*/
/*										*/
/********************************************************************************/

private JPanel getJavaCreationPanel()
{
   SwingGridPanel pnl = new SwingGridPanel();
   CreationActions cact = new CreationActions();

   pnl.beginLayout();
   pnl.addBannerLabel("Bubbles Java Project Creator");
   pnl.addSeparator();

   String projnm = project_props.getString(PROJ_PROP_NAME);
   name_field = pnl.addTextField("Project Name",projnm,24,cact,cact);
   pnl.addSeparator();


   int idx = 0;
   for (BuenoProjectMaker bpm : type_names.values()) {
      if (bpm == project_type) break;
      ++idx;
    }
   type_field = pnl.addChoice("Project Type",type_names.keySet(),idx,cact);
   pnl.addSeparator();

   for (BuenoProjectMaker bpm : type_names.values()) {
      JPanel spnl = bpm.createPanel(this,project_props);
      panel_map.put(bpm,spnl);
      spnl.setVisible(false);
      pnl.addLabellessRawComponent(bpm.getLabel(),spnl);
    }

   create_button = pnl.addBottomButton("CREATE","CREATE PROJECT",cact);
   pnl.addBottomButtons();

   setVisibilities();
   checkStatus();

   return pnl;
}




private void setVisibilities()
{
   if (type_field == null) return;

   project_type = type_names.get(type_field.getSelectedItem());
   Container par = null;
   for (BuenoProjectMaker bpm : type_names.values()) {
      JPanel spnl = panel_map.get(bpm);
      if (bpm == project_type) {
	 if (!spnl.isVisible()) {
	    spnl.setVisible(true);
	    bpm.resetPanel(project_props);
	    par = spnl.getParent();
	  }
       }
      else {
	 spnl.setVisible(false);
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




@Override public void checkStatus()
{
   File pdir = project_props.getFile(PROJ_PROP_DIRECTORY);
   boolean isok = pdir != null;
   isok &= project_type.checkStatus(project_props);
   if (create_button != null) create_button.setEnabled(isok);
}



private class CreationActions implements ActionListener, UndoableEditListener {

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd == null) ;
      else if (cmd.equals("CREATE") || cmd.equals("CREATE PROJECT")) {
         if (createProject()) {
            JComponent c = (JComponent) evt.getSource();
            BudaBubble bb = BudaRoot.findBudaBubble(c);
            if (bb != null) bb.setVisible(false);
            // possibly bring up project editor dialog here
          }
         return;
       }
      else if (cmd.equals("Project Name")) {
         setProjectDirectory();
       }
      else if (cmd.equals("Project Type")) {
         setVisibilities();
       }
   
      checkStatus();
    }

   @Override public void undoableEditHappened(UndoableEditEvent evt) {
      if (name_field != null && name_field.getDocument() == evt.getSource()) {
	 project_props.put(PROJ_PROP_NAME,name_field.getText());
	 setProjectDirectory();
       }

      checkStatus();
    }

   private void setProjectDirectory() {
      String pnm = project_props.getString(PROJ_PROP_NAME);
      File pdir = null;
      if (pnm != null && pnm.length() > 0 && pnm.matches(NAME_PAT)) {
         BoardSetup bs = BoardSetup.getSetup();
         File f1 = new File(bs.getDefaultWorkspace());
         File f2 = new File(f1,pnm);
         if (!f2.exists()) pdir = f2;
       }
      project_props.put(PROJ_PROP_DIRECTORY,pdir);
    }

}	// end of inner class CreationActions



/********************************************************************************/
/*										*/
/*	Top level creation methods						*/
/*										*/
/********************************************************************************/

private boolean createProject()
{
   String pnm = project_props.getString(PROJ_PROP_NAME);
   File pdir = project_props.getFile(PROJ_PROP_DIRECTORY);
   if (!pdir.mkdir()) return false;
   File bdir = new File(pdir,"bin");
   if (!bdir.mkdir()) return false;

   if (!project_type.setupProject(this,project_props)) {
      try {
	 IvyFile.remove(pdir);
      }
      catch (IOException e) { }
      return false;
   }

   checkFileProperties();

   if (!generateClassPathFile()) return false;
   if (!generateProjectFile()) return false;
   if (!generateSettingsFile()) return false;
   if (!generateOtherFiles()) return false;

   BumpClient bc = BumpClient.getBump();
   bc.importProject(pnm);

   return true;
}



private void checkFileProperties()
{
   for (File f : project_props.getSources()) {
      boolean fg = checkFileProperties(f);
      if (fg) break;
    }
}


private boolean checkFileProperties(File f)
{
   boolean fg = project_props.get(PROJ_PROP_ANDROID) != null;
   boolean fg1 = project_props.get(PROJ_PROP_JUNIT) != null;
   if (f.isDirectory()) {
      File [] subs = f.listFiles();
      if (subs != null) {
	 for (File f1 : subs) {
	    fg |= checkFileProperties(f1);
	    fg1 = fg;
	    if (fg) break;
	  }
       }
    }
   else if (f.getName().toLowerCase().endsWith(".java")) {
      try {
	 String cnts = IvyFile.loadFile(f);
	 if (cnts.contains("import android.")) {
	    if (f.getName().equals("MainActivity.java")) {
	       String pkg = getPackageName(f);
	       if (pkg != null) project_props.put(PROJ_PROP_ANDROID_PKG,pkg);
	     }
	    fg = true;
	    project_props.put(PROJ_PROP_ANDROID,true);
	  }
	 if (cnts.contains("import org.junit.") || cnts.contains("import junit.framework.")) {
	    project_props.put(PROJ_PROP_JUNIT,true);
	    fg1 = true;
	    if (!BumpClient.getBump().getOptionBool("bedrock.useAndroid")) fg = true;
	  }
       }
      catch (IOException e) { }
    }

   return fg && fg1;
}


/********************************************************************************/
/*										*/
/*	Generic output methods							*/
/*										*/
/********************************************************************************/

@Override public BuenoProjectProps getProperties()	{ return project_props; }

@Override public boolean generateClassPathFile()
{
   File pdir = project_props.getFile(PROJ_PROP_DIRECTORY);

   File cpf = new File(pdir,".classpath");
   try {
      IvyXmlWriter xw = new IvyXmlWriter(cpf);
      xw.outputHeader();
      xw.begin("classpath");
      for (File sdir : project_props.getSources()) {
	 xw.begin("classpathentry");
	 xw.field("kind","src");
	 xw.field("path",getFilePath(project_props,sdir));
	 xw.end("classpathentry");
       }
      if (project_props.get(PROJ_PROP_ANDROID) == null) {
	 xw.begin("classpathentry");
	 xw.field("kind","con");
	 xw.field("path","org.eclipse.jdt.launching.JRE_CONTAINER");
	 xw.end("classpathentry");
       }

      xw.begin("classpathentry");
      xw.field("kind","output");
      xw.field("path","bin");
      xw.end("classpathentry");
      boolean havejunit = false;
      for (File f : project_props.getLibraries()) {
	 xw.begin("classpathentry");
	 xw.field("kind","lib");
	 xw.field("path",getFilePath(project_props,f));
	 if (f.getName().contains("junit")) havejunit = true;
	 xw.end("classpathentry");
       }

      if (project_props.get(PROJ_PROP_ANDROID) != null) {
	 xw.begin("classpathentry");
	 xw.field("kind","con");
	 xw.field("path","com.android.ide.eclipse.adt.ANDROID_FRAMEWORK");
	 xw.end("classpathentry");
	 xw.begin("classpathentry");
	 xw.field("kind","con");
	 xw.field("exported","true");
	 xw.field("path","com.android.ide.eclipse.adt.LIBRARIES");
	 xw.end("classpathentry"); xw.begin("classpathentry");
	 xw.field("kind","con");
	 xw.field("exported","true");
	 xw.field("path","com.android.ide.eclipse.adt.DEPENDENCIES");
	 xw.end("classpathentry");
       }

      if (project_props.get(PROJ_PROP_JUNIT) == Boolean.TRUE && !havejunit) {
	 String path = BoardSetup.getSetup().getLibraryPath("junit.jar");
	 if (path != null) {
	    xw.begin("classpathentry");
	    xw.field("kind","lib");
	    xw.field("path",path);
	    xw.end("classpathentry");
	  }
       }

      xw.end("classpath");
      xw.close();
    }
   catch (IOException e) {
      return false;
    }

   return true;
}



@Override public boolean generateProjectFile()
{
   File pdir = project_props.getFile(PROJ_PROP_DIRECTORY);
   String pnm = project_props.getString(PROJ_PROP_NAME);

   try {
      File f1 = new File(pdir,".project");
      IvyXmlWriter xw = new IvyXmlWriter(f1);
      xw.outputHeader();
      xw.begin("projectDescription");
      xw.textElement("name",pnm);
      xw.textElement("comment","Generated by Code Bubbles");
      xw.begin("buildSpec");

      if (project_props.get(PROJ_PROP_ANDROID) != null) {
	 xw.begin("buildCommand");
	 xw.textElement("name","com.android.ide.eclipse.adt.ResourceManagerBuilder");
	 xw.begin("arguments");
	 xw.end("arguments");
	 xw.end("buildCommand");
	 xw.begin("buildCommand");
	 xw.textElement("name","com.android.ide.eclipse.adt.PreCompilerBuilder");
	 xw.begin("arguments");
	 xw.end("arguments");
	 xw.end("buildCommand");
       }

      xw.begin("buildCommand");
      xw.textElement("name","org.eclipse.jdt.core.javabuilder");
      xw.begin("arguments");
      xw.end("arguments");
      xw.end("buildCommand");

      if (project_props.get(PROJ_PROP_ANDROID) != null) {
	 xw.begin("buildCommand");
	 xw.textElement("name","com.android.ide.eclipse.adt.ApkBuilder");
	 xw.begin("arguments");
	 xw.end("arguments");
	 xw.end("buildCommand");
       }

      xw.end("buildSpec");
      xw.begin("natures");
      if (project_props.get(PROJ_PROP_ANDROID) != null) {
	 xw.textElement("nature","com.android.ide.eclipse.adt.AndroidNature");
       }
      xw.textElement("nature","org.eclipse.jdt.core.javanature");
      xw.end("natures");
      xw.begin("linkedResources");
      for (Map.Entry<String,File> ent : project_props.getLinks().entrySet()) {
	 xw.begin("link");
	 xw.textElement("name",ent.getKey());
	 xw.textElement("type","2");
	 String xnm = ent.getValue().getPath();
	 xnm = xnm.replace("\\","/");
	 xw.textElement("location",xnm);
	 xw.end("link");
       }
      xw.end("linkedResources");
      xw.end("projectDescription");
      xw.close();
    }
   catch (IOException e) {
      return false;
    }
   return true;
}



@Override public boolean generateSettingsFile()
{
   File pdir = project_props.getFile(PROJ_PROP_DIRECTORY);
   File sdir = new File(pdir,".settings");
   sdir.mkdirs();
   File opts = new File(sdir,"org.eclipse.jdt.core.prefs");
   
   BoardProperties props = BoardProperties.getProperties("Bueno");
   String copts = props.getProperty("Bueno.problem.set.data.1");
   
   String fopts = null;
   File f1 = BoardSetup.getPropertyBase();
   File f2 = new File(f1,"formats.xml");
   if (f2.exists()) {
      StringBuffer fbuf = new StringBuffer();
      Element optxml = IvyXml.loadXmlFromFile(f2);
      for (Element setxml : IvyXml.elementsByTag(optxml,"setting")) {
         String id = IvyXml.getAttrString(setxml,"id");
         String val = IvyXml.getAttrString(setxml,"value");
         fbuf.append(id);
         fbuf.append("=");
         fbuf.append(val);
         fbuf.append("\n");
       }
      fopts = fbuf.toString();
    }
   
   try (PrintWriter pw = new PrintWriter(new FileWriter(opts))) {
      pw.println("eclipse.preferences.version=1");
      String v = System.getProperty("java.specification.version");
      pw.println("org.eclipse.jdt.core.compiler.compliance=" + v);
      pw.println("org.eclipse.jdt.core.compiler.source=" + v);
      pw.println("org.eclipse.jdt.core.compiler.codegen.targetPlatform=" + v);
      if (copts != null) pw.println(copts);
      if (fopts != null) pw.println(fopts);
    }
   catch (IOException e) {
      return false;
    }

   return true;
}



@Override public boolean generateOtherFiles()
{
   File pdir = project_props.getFile(PROJ_PROP_DIRECTORY);
   // String pnm = project_props.getString(PROJ_PROP_NAME);

   // look in source directory for these and just copy or link if present

   // FILES: project.properties (*), lint.xml, AndroidManifest.xml (*), res (*)

   if (project_props.get(PROJ_PROP_ANDROID) == null) return true;
   File root = findAndroidRoot();

   File f1 = new File(pdir,"project.properties");
   File f2 = new File(pdir,"res");
   File f3 = new File(pdir,"lint.xml");
   File f4 = new File(pdir,"AndroidManifest.xml");

   if (root != null && !pdir.equals(root)) {
      File f1r = new File(root,"project.properties");
      createLink(f1r,f1);
      File f2r = new File(root,"res");
      createLink(f2r,f2);
      File f3r = new File(root,"lint.xml");
      createLink(f3r,f3);
      File f4r = new File(root,"AndroidManifest.xml");
      createLink(f4r,f4);
    }

   try {
      if (!f1.exists()) {
	 PrintWriter pw = new PrintWriter(new FileWriter(f1));
	 pw.println("# Project target.");
	 pw.println("target=android-22");
	 pw.println("android.library.reference.1=../appcompat_v7");
	 pw.close();
       }
      if (!f2.exists()) {
	 f2.mkdir();
       }
      if (!f4.exists()) {
	 IvyXmlWriter xw = new IvyXmlWriter(f4);
	 xw.outputHeader();
	 xw.begin("manifest");
	 xw.field("xmlns:android","http://schemas.android.com/apk/res/android");
	 String pkg = project_props.getString(PROJ_PROP_ANDROID_PKG);
	 if (pkg != null) xw.field("package",pkg);
	 xw.field("android:versionCode",1);
	 xw.field("android.versionName","1.0");
	 xw.begin("uses-sdk");
	 xw.field("android:minSdkVersion",8);
	 xw.field("android:targetSdkVersion",21);
	 xw.end();
	 xw.begin("application");
	 xw.field("android:allowBackups",true);
	 xw.field("android:icon","@drawable/ic_launcher");
	 xw.field("android:label","@string/app_name");
	 xw.field("android:theme","@style/AppTheme");
	 xw.begin("activity");
	 xw.field("android:name",".MainActivity");
	 xw.field("android:label","@string/app_name");
	 xw.begin("intent-filter");
	 xw.begin("action");
	 xw.field("android:name","android.intent.action.MAIN");
	 xw.end();
	 xw.begin("category");
	 xw.field("android:name","android.intent.category.LAUNCHER");
	 xw.end();
	 xw.end();
	 xw.end();
	 xw.end();
	 xw.end();
	 xw.close();
       }
    }
   catch (IOException e) {
      return false;
    }
   return true;
}



private void createLink(File f1,File f2)
{
   if (!f1.exists() || !f1.canRead()) return;
   Path p1 = Paths.get(f1.getAbsolutePath());
   Path p2 = Paths.get(f2.getAbsolutePath());
   try {
      Files.createSymbolicLink(p2,p1);
      return;
    }
   catch (IOException e) { }

   try {
      if (f1.isDirectory()) return;
      IvyFile.copyFile(f1,f2);
    }
   catch (IOException e) { }
}



private File findAndroidRoot()
{
   Set<File> done = new HashSet<File>();
   Map<String,File> lnks = project_props.getLinks();
   for (File f : lnks.values()) {
      File rslt = findAndroidRoot(f,done);
      if (rslt != null) return rslt;
    }

   return null;
}


private final String [] root_files = new String [] {
      "AndroidManifest.xml",
      "res",
      "link.xml",
      "project.properties"
};


private File findAndroidRoot(File f,Set<File> done)
{
   if (!f.isDirectory()) return null;
   if (done.contains(f)) return null;
   done.add(f);

   int ct = 0;
   for (int i = 0; i < root_files.length; ++i) {
      File f1 = new File(f,root_files[i]);
      if (f1.exists()) ++ct;
    }
   if (ct >= 2) return f;
   File [] subfiles = f.listFiles();
   if (subfiles != null) {
      for (File f2 : subfiles) {
	 File f3 = findAndroidRoot(f2,done);
	 if (f3 != null) return f3;
       }
    }

   return null;
}



@Override public String getPackageName(File src)
{
   try {
      FileReader fis = new FileReader(src);
      StreamTokenizer str = new StreamTokenizer(fis);
      str.slashSlashComments(true);
      str.slashStarComments(true);
      str.eolIsSignificant(false);
      str.lowerCaseMode(false);
      str.wordChars('_','_');
      str.wordChars('$','$');

      StringBuilder pkg = new StringBuilder();

      for ( ; ; ) {
	 int tid = str.nextToken();
	 if (tid == StreamTokenizer.TT_WORD) {
	    if (str.sval.equals("package")) {
	       for ( ; ; ) {
		  int nid = str.nextToken();
		  if (nid != StreamTokenizer.TT_WORD) break;
		  pkg.append(str.sval);
		  nid = str.nextToken();
		  if (nid != '.') break;
		  pkg.append(".");
		}
	       break;
	     }
	    else break;
	  }
	 else if (tid == StreamTokenizer.TT_EOF) {
	    return null;
	 }
       }

      fis.close();
      return pkg.toString();
    }
   catch (IOException e) {
    }

   return null;
}





/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

private String getFilePath(BuenoProjectProps props,File f)
{
   File pdir = props.getFile(PROJ_PROP_DIRECTORY);
   pdir = pdir.getAbsoluteFile();
   try {
      pdir = pdir.getCanonicalFile();
   }
   catch (IOException e) { }

   f = f.getAbsoluteFile();
   try {
      f = f.getCanonicalFile();
   }
   catch (IOException e) { }

   String p1 = f.getPath();
   String p2 = pdir.getPath();
   String p3 = p2 + File.separator;
   if (p1.startsWith(p3)) {
      int ln = p2.length()+1;
      return p1.substring(ln);
    }

   for (Map.Entry<String,File> ent : props.getLinks().entrySet()) {
      File f3 = ent.getValue().getAbsoluteFile();
      try {
	 f3 = f3.getCanonicalFile();
      }
      catch (IOException e) { }
      p2 = f3.getPath();
      p3 = p2 + File.separator;
      if (p1.equals(p2)) {
	 return ent.getKey();
      }
      else if (p1.startsWith(p3)) {
	 int ln = p2.length();
	 return ent.getKey() + File.separator + p1.substring(ln);
       }
    }

   return p1;
}




/********************************************************************************/
/*										*/
/*	Property holder 							*/
/*										*/
/********************************************************************************/

private static class ProjProps extends HashMap<String,Object> implements BuenoProjectProps {

   ProjProps()				{ }

   @Override public String getString(String k)		{ return (String) get(k); }
   @Override public File getFile(String k)		{ return (File) get(k); }
   @Override public Object get(String k)		{ return super.get(k); }
   @Override public Object remove(String k)		{ return super.remove(k); }
   @Override public Object put(String k,Object v) {
      if (v == null) return super.remove(k);
      else return super.put(k,v);
    }

   @SuppressWarnings("unchecked")
   @Override public List<File> getLibraries() {
      List<File> lf = (List<File>)get(PROJ_PROP_LIBS);
      if (lf == null) {
	 lf = new ArrayList<File>();
	 super.put(PROJ_PROP_LIBS,lf);
       }
      return lf;
    }
   @SuppressWarnings("unchecked")
   @Override public List<File> getSources() {
      List<File> lf = (List<File>)get(PROJ_PROP_SOURCE);
      if (lf == null) {
	 lf = new ArrayList<File>();
	 super.put(PROJ_PROP_SOURCE,lf);
       }
      return lf;
   }

   @SuppressWarnings("unchecked")
   @Override public Map<String,File> getLinks() {
      Map<String,File> lnks = (Map<String,File>) get(PROJ_PROP_LINKS);
      if (lnks == null) {
	 lnks = new HashMap<>();
	 super.put(PROJ_PROP_LINKS,lnks);
       }
      return lnks;
    }

}	// end of inner class ProjProps


}	// end of class BuenoProjectCreator




/* end of BuenoProjectCreator.java */

