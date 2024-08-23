/********************************************************************************/
/*										*/
/*		BedrockProjectCreator.java					*/
/*										*/
/*	Code to create and setup a project					*/
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



package edu.brown.cs.bubbles.bedrock;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.StringTokenizer;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class BedrockProjectCreator implements BedrockConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String project_name;
private File project_dir;
private String project_type;
private Map<String,Object> prop_map;

private static final String PROJ_PROP_DIRECTORY = "ProjectDirectory";
private static final String PROJ_PROP_SOURCE = "ProjectSource";
private static final String PROJ_PROP_LIBS = "ProjectLibraries";
private static final String PROJ_PROP_LINKS = "ProjectLinks";
private static final String PROJ_PROP_ANDROID = "ProjectAndroid";
private static final String PROJ_PROP_ANDROID_PKG = "ProjectAndroidPackage";
private static final String PROJ_PROP_JUNIT = "ProjectJunit";
private static final String PROJ_PROP_JUNIT_PATH = "ProjectJunitPath";
private static final String PROJ_PROP_USE_ANDROID = "ProjectUseAndroid";
private static final String PROJ_PROP_CORE_OPTIONS = "ProjectCoreOptions";
private static final String PROJ_PROP_FORMAT_FILE = "ProjectFormatFile";

private final String [] root_files = new String [] {
      "AndroidManifest.xml",
      "res",
      "link.xml",
      "project.properties"
};

private static final String SRC_NAME = "src_";


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BedrockProjectCreator(String pnm,File pdir,String type,Element props)
{
   project_name = pnm;
   project_dir = pdir;
   project_type = type;
   setupPropMap(props);
}


/********************************************************************************/
/*										*/
/*	Work methods								*/
/*										*/
/********************************************************************************/

boolean setupProject()
{
   BedrockPlugin.logD("START SETUP " + project_type + " " + project_dir);

   if (project_type == null) return false;
   project_dir.mkdir();
   File bdir = new File(project_dir,"bin");
   bdir.mkdir();

   BedrockPlugin.logD("SETUP PROJECT " + project_type + " " + bdir);

   boolean fg = true;
   switch (project_type) {
      case "NEW" :
	 fg = setupNewProject();
	 break;
      case "SOURCE" :
	 fg = setupSourceProject();
	 break;
      case "TEMPLATE" :
	 fg = setupTemplateProject();
	 break;
      case "GIT" :
	 fg = setupGitProject();
	 break;
      case "CLONE" :
         fg = setupCloneProject();
         break;
    }

   BedrockPlugin.logD("Project defined.  Write project " + fg);

   if (!fg) return false;

   checkFileProperties();
   if (!generateClassPathFile()) return false;
   if (!generateProjectFile()) return false;
   if (!generateSettingsFile()) return false;
   if (!generateOtherFiles()) return false;

   return true;
}



/********************************************************************************/
/*										*/
/*	Setup a completely new project						*/
/*										*/
/********************************************************************************/

private boolean setupNewProject()
{
   File sdir = new File(project_dir,"src");
   List<File> srcs = new ArrayList<>();
   srcs.add(sdir);
   prop_map.put(PROJ_PROP_SOURCE,srcs);

   if (!sdir.exists() && !sdir.mkdir()) return false;

   Map<String,String> bp = new HashMap<>();
   bp.put("PROJECT",project_name);
   bp.put("AUTHOR",System.getProperty("user.name"));

   String cnm = "Main";
   String pnm = propString("PACKAGE_NAME");
   if (pnm != null && pnm.length() > 0) {
      bp.put("PACKAGE",pnm);
      StringTokenizer tok = new StringTokenizer(pnm,".");
      while (tok.hasMoreTokens()) {
	 sdir = new File(sdir,tok.nextToken());
       }
      if (!sdir.mkdirs()) return false;
      String plast = sdir.getName();
      plast = plast.toLowerCase();
      plast = plast.substring(0,1).toUpperCase() + plast.substring(1);
      cnm = plast + "Main";
    }

   bp.put("CLASS_NAME",cnm);

   try {
      String nm = "resources/templates/scratch.template";
      InputStream ins = BedrockProjectCreator.class.getClassLoader().getResourceAsStream(nm);
      if (ins == null) return false;
      String cnts = IvyFile.loadFile(ins);
      String ncnts = IvyFile.expandText(cnts,bp);
      File f1 = new File(sdir,cnm + ".java");
      FileWriter fw = new FileWriter(f1);
      fw.write(ncnts);
      fw.close();
    }
   catch (IOException e) {
      return false;
    }

   return true;
}



/********************************************************************************/
/*										*/
/*	Setup a project using existing source					*/
/*										*/
/********************************************************************************/

private boolean setupSourceProject()
{
   File dir = propFile("SOURCE_DIR");

   BedrockPlugin.logD("SETUP SOURCE PROJECT " + dir);

   if (dir == null) return false;

   return defineProject(dir);
}


private boolean defineProject(File dir)
{
   Set<File> srcs = new HashSet<File>();
   Set<File> libs = new HashSet<File>();
   Set<File> rsrcs = new HashSet<File>();

   findFiles(dir,srcs,libs,rsrcs);
   Map<File,List<File>> roots = new HashMap<>();
   for (File sf : srcs) {
      String pkg = getPackageName(sf);
      File par = sf.getParentFile();
      if (pkg != null) {
	 String [] ps = pkg.split("\\.");
	 for (int i = ps.length-1; par != null && i >= 0; --i) {
	    if (!par.getName().equals(ps[i])) par = null;
	    else par = par.getParentFile();
	  }
       }
      if (par != null) {
	 List<File> lf = roots.get(par);
	 if (lf == null) {
	    lf = new ArrayList<File>();
	    roots.put(par,lf);
	  }
	 lf.add(sf);
       }
    }
   Map<String,File> links = new HashMap<>();
   links.put(SRC_NAME + "1",dir);
   prop_map.put(PROJ_PROP_LINKS,links);
   List<File> srclst = new ArrayList<>();
   for (File f : roots.keySet()) {
      srclst.add(f);
    }
   prop_map.put(PROJ_PROP_SOURCE,srclst);
   List<File> liblst = new ArrayList<>();
   for (File f : libs) {
      liblst.add(f);
    }
   for (File f : rsrcs) {
      liblst.add(f);
    }
   prop_map.put(PROJ_PROP_LIBS,liblst);

   return true;
}



protected void findFiles(File dir,Set<File> srcs,Set<File> libs,Set<File> rsrcs)
{
   if (dir.isDirectory()) {
      if (dir.getName().equals("bBACKUP")) return;
      else if (dir.getName().startsWith(".")) return;
      else if (dir.getName().equals("node_modules")) return;
      if (dir.getName().equals("resources")) {
	 boolean havesrc = false;
	 for (String fnm : dir.list()) {
	    if (fnm.endsWith(".java")) havesrc = true;
	  }
	 if (!havesrc) rsrcs.add(dir);
       }
      if (dir.listFiles() != null) {
	 for (File sf : dir.listFiles()) {
	    findFiles(sf,srcs,libs,rsrcs);
	  }
       }
      return;
    }

   String pnm = dir.getPath();
   dir = IvyFile.getCanonical(dir);

   if (dir.length() < 10) return;
   if (!dir.isFile()) return;

   if (pnm.endsWith(".java")) srcs.add(dir.getAbsoluteFile());
   else if (pnm.endsWith(".jar")) {
      if (!pnm.contains("javadoc") && !pnm.contains("source"))
	 libs.add(dir.getAbsoluteFile());
    }
}



/********************************************************************************/
/*										*/
/*	Setup a project copying existing source 				*/
/*										*/
/********************************************************************************/

private boolean setupTemplateProject()
{
   File dir = propFile("TEMPLATE_DIR");
   Set<File> srcs = new HashSet<File>();
   Set<File> libs = new HashSet<File>();
   Set<File> resources = new HashSet<>();

   findFiles(dir,srcs,libs,resources);
   Map<String,List<File>> roots = new HashMap<String,List<File>>();
   for (File sf : srcs) {
      sf = sf.getAbsoluteFile();
      String pkg = getPackageName(sf);
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

   File sdir = new File(project_dir,"src");

   List<File> srclst = new ArrayList<>();
   srclst.add(sdir);
   prop_map.put(PROJ_PROP_SOURCE,srclst);

   for (Map.Entry<String,List<File>> ent : roots.entrySet()) {
      String pkg = ent.getKey();
      File tdir = sdir;
      String [] ps = pkg.split("\\.");
      for (int i = 0; i < ps.length; ++i) {
	 tdir = new File(tdir,ps[i]);
       }
      if (!tdir.exists() && !tdir.mkdirs()) return false;
      for (File f : ent.getValue()) {
	 File f1 = new File(tdir,f.getName());
	 try {
	    IvyFile.copyFile(f,f1);
	  }
	 catch (IOException e) {
	    BedrockPlugin.logE("Problem copying source files",e);
	  }
       }
    }

   List<File> liblst = propLibraries();
   if (libs.size() > 0) {
      File ldir = new File(project_dir,"lib");
      if (!ldir.mkdirs()) return false;
      for (File lf : libs) {
	 File lf1 = new File(ldir,lf.getName());
	 if (!lf1.exists()) {
	    try {
	       IvyFile.copyFile(lf,lf1);
	     }
	    catch (IOException e) {
	       BedrockPlugin.logE("Problem copying source files",e);
	     }
	    liblst.add(lf1);
	  }
       }
    }
   prop_map.put(PROJ_PROP_LIBS,liblst);

   if (resources.size() > 0) {
      File rdir = new File(project_dir,"resources");
      if (!rdir.mkdirs()) return false;
      for (File rf : resources) {
	 try {
	    IvyFile.copyHierarchy(rf,rdir);
	  }
	 catch (IOException e) {
	    BedrockPlugin.logE("Problem copying source files",e);
	  }
       }
    }

   return true;
}



/********************************************************************************/
/*										*/
/*	Setup a project from GITHUB						*/
/*										*/
/********************************************************************************/

private boolean setupGitProject()
{
   File sdir = propFile("GIT_DIR");
   prop_map.put("SOURCE_DIR",sdir);
   return defineProject(sdir);
}


/********************************************************************************/
/*                                                                              */
/*      Setup project by cloning an existing one                                */
/*                                                                              */
/********************************************************************************/

private boolean setupCloneProject()
{
   File sdir = propFile("WORKSPACE_DIR");
   File f1 = new File(sdir,".classpath");
   File f2 = new File(sdir,".project");
   File f3 = new File(sdir,".settings");
   File f4 = new File(f3,"org.eclipse.jdt.core.prefs");
   File f5 = new File(f3,"org.eclipse.core.resources.prefs");
   if (!f1.exists() || !f2.exists() || !f4.exists() || !f5.exists()) return false;
   
   return true;
}


/********************************************************************************/
/*										*/
/*	Check files for andrioid and junit usage				*/
/*										*/
/********************************************************************************/

private void checkFileProperties()
{
   for (File f : propSources()) {
      boolean fg = checkFileProperties(f);
      if (fg) break;
    }
}



private boolean checkFileProperties(File f)
{
   boolean fg = prop_map.get(PROJ_PROP_ANDROID) != null;
   boolean fg1 = prop_map.get(PROJ_PROP_JUNIT) != null;
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
	       if (pkg != null) prop_map.put(PROJ_PROP_ANDROID_PKG,pkg);
	     }
	    fg = true;
	    prop_map.put(PROJ_PROP_ANDROID,true);
            BedrockPlugin.logD("ANDROID main found in " + f);
	  }
	 if (cnts.contains("import org.junit.") ||
	       cnts.contains("import junit.framework.")) {
	    prop_map.put(PROJ_PROP_JUNIT,true);
	    fg1 = true;
	    if (propBool(PROJ_PROP_USE_ANDROID)) fg = true;
	  }
       }
      catch (IOException e) { }
    }

   return fg && fg1;
}



public String getPackageName(File src)
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
/*	Generate class path file for Eclipse					*/
/*										*/
/********************************************************************************/

private boolean generateClassPathFile()
{
   BedrockPlugin.logD("GENERATE CLASS PATH FILE IN " + project_dir);

   File cpf = new File(project_dir,".classpath");
   if (project_type.equals("CLONE")) {
      File f1 = propFile("WORKSPACE_DIR");
      File f2 = new File(f1,".classpath");
      try {
         IvyFile.copyFile(f2,cpf);
       }
      catch (IOException e) { 
         BedrockPlugin.logE("Problem copying class path file",e);
         return false;
       }
      return true;
    }
   
   try {
      IvyXmlWriter xw = new IvyXmlWriter(cpf);
      xw.outputHeader();
      xw.begin("classpath");
      for (File sdir : propSources()) {
	 xw.begin("classpathentry");
	 xw.field("kind","src");
	 xw.field("path",getFilePath(sdir));
	 xw.end("classpathentry");
       }
      if (!propBool(PROJ_PROP_ANDROID)) {
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
      for (File f : propLibraries()) {
	 xw.begin("classpathentry");
	 xw.field("kind","lib");
	 xw.field("path",getFilePath(f));
	 if (f.getName().contains("junit")) havejunit = true;
	 xw.end("classpathentry");
       }

      if (propBool(PROJ_PROP_ANDROID)) {
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

      if (propBool(PROJ_PROP_JUNIT) && !havejunit) {
	 String path = propString(PROJ_PROP_JUNIT_PATH);
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
   catch (Throwable e) {
      BedrockPlugin.logE("Problem writing class path file",e);
      return false;
    }

   return true;
}



private String getFilePath(File f)
{
   File pdir = propFile(PROJ_PROP_DIRECTORY);
   pdir = pdir.getAbsoluteFile();
   pdir = IvyFile.getCanonical(pdir);

   f = f.getAbsoluteFile();
   f = IvyFile.getCanonical(f);

   String p1 = f.getPath();
   String p2 = pdir.getPath();
   String p3 = p2 + File.separator;
   if (p1.startsWith(p3)) {
      int ln = p2.length()+1;
      return p1.substring(ln);
    }

   for (Map.Entry<String,File> ent : propLinks().entrySet()) {
      File f3 = ent.getValue().getAbsoluteFile();
      f3 = IvyFile.getCanonical(f3);
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
/*	Generate .project file for Eclipse					*/
/*										*/
/********************************************************************************/

private boolean generateProjectFile()
{
   BedrockPlugin.logD("GENERATE PROJECT FILE");
   File f1 = new File(project_dir,".project");
   
   if (project_type.equals("CLONE")) {
      File f0 = propFile("WORKSPACE_DIR");
      File f2 = new File(f0,".project");
      try {
         IvyFile.copyFile(f2,f1);
       }
      catch (IOException e) {
         BedrockPlugin.logE("Problem copying class path file",e);
         return false;
       }
      return true;
    }
   
   try {
      IvyXmlWriter xw = new IvyXmlWriter(f1);
      xw.outputHeader();
      xw.begin("projectDescription");
      xw.textElement("name",project_name);
      xw.textElement("comment","Generated by Code Bubbles");
      xw.begin("buildSpec");

      if (propBool(PROJ_PROP_ANDROID)) {
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

      if (propBool(PROJ_PROP_ANDROID)) {
	 xw.begin("buildCommand");
	 xw.textElement("name","com.android.ide.eclipse.adt.ApkBuilder");
	 xw.begin("arguments");
	 xw.end("arguments");
	 xw.end("buildCommand");
       }

      xw.end("buildSpec");
      xw.begin("natures");
      if (propBool(PROJ_PROP_ANDROID)) {
	 xw.textElement("nature","com.android.ide.eclipse.adt.AndroidNature");
       }
      xw.textElement("nature","org.eclipse.jdt.core.javanature");
      xw.end("natures");
      xw.begin("linkedResources");
      for (Map.Entry<String,File> ent : propLinks().entrySet()) {
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
   catch (Throwable e) {
      BedrockPlugin.logE("Problem careating project file",e);
      return false;
    }

   return true;
}



/********************************************************************************/
/*										*/
/*	Generate .settings file for Eclipse					*/
/*										*/
/********************************************************************************/

private boolean generateSettingsFile()
{
   boolean status = true;
   
   BedrockPlugin.logD("WRITE SETTINGS FILE");

   File sdir = new File(project_dir,".settings");
   sdir.mkdirs();
   File opts = new File(sdir,"org.eclipse.jdt.core.prefs");
   File popts = new File(sdir,"org.eclipse.core.resources.prefs");
   if (project_type.equals("CLONE")) {
      File f1 = propFile("WORKSPACE_DIR");
      File f2 = new File(f1,".settings");
      File f3 = new File(f2,"org.eclipse.jdt.core.prefs");
      File f4 = new File(f2,"org.eclipse.core.resources.prefs");
      try {
         IvyFile.copyFile(f3,opts);
         IvyFile.copyFile(f4,popts);
       }
      catch (IOException e) {
         BedrockPlugin.logE("Problem copying settings file",e);
         return false;
       }
      return true;
    }
   
   String copts = propString(PROJ_PROP_CORE_OPTIONS);
   String fopts = null;
   File f2 = propFile(PROJ_PROP_FORMAT_FILE);
   if (f2 != null && f2.exists()) {
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
   catch (Throwable e) {
      BedrockPlugin.logE("Problem creating settings file",e);
      status = false;
    }
   
   try (PrintWriter pw = new PrintWriter(new FileWriter(popts))) {
      pw.println("eclispe.preferences.version=1");
      pw.println("encoding/<project>=UTF-8");
   }
   catch (Throwable e) {
      BedrockPlugin.logE("Problem creating project settings file",e);
      status = false;
   }
   
   return status;
}



/********************************************************************************/
/*										*/
/*	 Generate miscellaneous Eclipse files (e.g. for Android)		*/
/*										*/
/********************************************************************************/

private boolean generateOtherFiles()
{
   if (!propBool(PROJ_PROP_ANDROID)) return true;
   File root = findAndroidRoot();

   BedrockPlugin.logD("WRITE OTHER FILES " + root);

   File f1 = new File(project_dir,"project.properties");
   File f2 = new File(project_dir,"res");
   File f3 = new File(project_dir,"lint.xml");
   File f4 = new File(project_dir,"AndroidManifest.xml");

   if (root != null && !project_dir.equals(root)) {
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
	 String pkg = propString(PROJ_PROP_ANDROID_PKG);
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
   catch (Throwable e) {
      BedrockPlugin.logE("Problem writing other files",e);
      return false;
    }
   return true;
}



private File findAndroidRoot()
{
   Set<File> done = new HashSet<>();
   Map<String,File> lnks = propLinks();
   for (File f : lnks.values()) {
      File rslt = findAndroidRoot(f,done);
      if (rslt != null) return rslt;
    }

   return null;
}



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




/********************************************************************************/
/*										*/
/*	Property Methods							*/
/*										*/
/********************************************************************************/

private void setupPropMap(Element props)
{
   prop_map = new HashMap<>();
   for (Element pelt : IvyXml.children(props,"PROP")) {
      String pnm = IvyXml.getAttrString(pelt,"NAME");
      Object fval = getPropertyValue(pelt);
      if (fval != null) prop_map.put(pnm,fval);
    }
}


private Object getPropertyValue(Element pelt)
{
   String typ = IvyXml.getAttrString(pelt,"TYPE");
   String val = IvyXml.getTextElement(pelt,"VALUE");
   if (val == null) return null;

   Object fval = null;
   switch (typ) {
      case "int" :
	 fval = Integer.valueOf(val);
	 break;
      case "String" :
	 fval = val;
	 break;
      case "boolean" :
	 fval = Boolean.valueOf(val);
	 break;
      case "File" :
	 fval = new File(val);
	 break;
      case "List" :
	 List<Object> l = new ArrayList<>();
	 for (Element lelt : IvyXml.children(pelt,"PROP")) {
	    Object v = getPropertyValue(lelt);
	    if (v != null) l.add(v);
	  }
	 fval = l;
	 break;
      case "Map" :
	 Map<String,Object> m = new HashMap<>();
	 for (Element melt : IvyXml.children(pelt,"PROP")) {
	    String k = IvyXml.getAttrString(melt,"NAME");
	    Object v = getPropertyValue(melt);
	    if (k != null && v != null) m.put(k,v);
	  }
	 fval = m;
	 break;
    }

   return fval;
}



private String propString(String k)
{
   Object v = prop_map.get(k);
   if (v == null) return null;
   return v.toString();
}

private boolean propBool(String k)
{
   Object v = prop_map.get(k);
   if (v == null) return false;
   if (v instanceof Boolean) return ((Boolean) v);
   return false;
}


private List<File> propSources()
{
   return propFiles(PROJ_PROP_SOURCE);
}

private List<File> propLibraries()
{
   return propFiles(PROJ_PROP_LIBS);
}

private Map<String,File> propLinks()
{
   Map<String,File> rslt = new HashMap<>();
   Map<?,?> lnks = (Map<?,?>) prop_map.get(PROJ_PROP_LINKS);
   if (lnks != null) {
      for (Map.Entry<?,?> ent : lnks.entrySet()) {
	 String k = ent.getKey().toString();
	 Object v1 = ent.getValue();
	 File v = null;
	 if (v1 instanceof File) v = (File) v1;
	 else if (v1 instanceof String) v = new File((String) v1);
	 if (v != null) rslt.put(k,v);
       }
    }
   return rslt;
}



private List<File> propFiles(String k)
{
   List<?> ps = (List<?>) prop_map.get(k);
   List<File> rslt = new ArrayList<>();
   if (ps != null) {
      for (Object o : ps) {
	 if (o instanceof File) rslt.add((File) o);
	 else if (o instanceof String) rslt.add(new File((String) o));
       }
    }
   return rslt;
}



private File propFile(String k) 
{
   Object o = prop_map.get(k);
   if (o == null) return null;
   if (o instanceof File) return (File) o;
   return new File(o.toString());
}



}	// end of class BedrockProjectCreator




/* end of BedrockProjectCreator.java */

