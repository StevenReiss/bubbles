/********************************************************************************/
/*										*/
/*		PybaseNature.java						*/
/*										*/
/*	Python Bubbles Base implementation of a PythonNature			*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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
/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Mar 11, 2004
 *
 * @author Fabio Zadrozny
 * @author atotic
 */


package edu.brown.cs.bubbles.pybase;

import edu.brown.cs.bubbles.pybase.symbols.ASTManager;
import edu.brown.cs.bubbles.pybase.symbols.AbstractASTManager;
import edu.brown.cs.bubbles.pybase.symbols.AbstractInterpreterManager;
import edu.brown.cs.bubbles.pybase.symbols.AbstractModule;
import edu.brown.cs.bubbles.pybase.symbols.AbstractToken;
import edu.brown.cs.bubbles.pybase.symbols.IronpythonInterpreterManager;
import edu.brown.cs.bubbles.pybase.symbols.JythonInterpreterManager;
import edu.brown.cs.bubbles.pybase.symbols.ModulesManager;
import edu.brown.cs.bubbles.pybase.symbols.ProjectModulesManager;
import edu.brown.cs.bubbles.pybase.symbols.PythonInterpreterManager;
import edu.brown.cs.bubbles.pybase.symbols.StringUtils;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class PybaseNature implements PybaseConstants {


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private PybaseProject		the_project;
private ASTManager ast_manager;
private final PybasePathNature python_path_nature;

private PybaseInterpreterType	interpreter_type;
private Object			set_params_lock;

public static final String DEFAULT_INTERPRETER = "Default";

public static final int GRAMMAR_PYTHON_VERSION_2_4 = 10;
public static final int GRAMMAR_PYTHON_VERSION_2_5 = 11;
public static final int GRAMMAR_PYTHON_VERSION_2_6 = 12;
public static final int GRAMMAR_PYTHON_VERSION_2_7 = 13;
public static final int LATEST_GRAMMAR_VERSION = GRAMMAR_PYTHON_VERSION_2_7;

public static final int GRAMMAR_PYTHON_VERSION_3_0 = 99;


private static File metadata_location = null;

private static List<PybaseNature> all_natures = new ArrayList<PybaseNature>();



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

PybaseNature(PybaseMain pm,PybaseProject proj)
{
   set_params_lock = new Object();
   the_project = null;
   ast_manager = null;
   python_path_nature = new PybasePathNature(proj,this);
   if (proj != null) {
      setProject(proj);
      all_natures.add(this);
    }
}



protected PybaseNature(PybaseMain pm)
{
   this(pm,null);
}


/********************************************************************************/
/*										*/
/*	File/path methods							*/
/*										*/
/********************************************************************************/


public boolean isResourceInPythonpathProjectSources(String absPath,boolean addExternal)
{
   return resolveModuleOnlyInProjectSources(absPath, addExternal) != null;
}



/**
 * Resolve the module given the absolute path of the file in the filesystem.
 *
 * @param abspath the absolute file path
 * @return the module name
 */
public String resolveModule(String abspath)
{
   return ast_manager.getModulesManager().resolveModule(abspath);
}



/**
 * Resolve the module given the absolute path of the file in the filesystem.
 *
 * @param abspath the absolute file path
 * @return the module name
 */
public String resolveModuleOnlyInProjectSources(String abspath,boolean addExternal)
{
   String moduleName = null;

   ModulesManager modulesManager = ast_manager.getModulesManager();
   if (modulesManager instanceof ProjectModulesManager) {
      moduleName = ((ProjectModulesManager) modulesManager)
	    .resolveModuleOnlyInProjectSources(abspath, addExternal);
    }

   return moduleName;
}


/**
 * Can be called to refresh internal info (or after changing the path in the preferences).
 */
public void rebuildPath()
{
   clearCaches(true);

   String paths = the_project.getProjectSourcePath();

   synchronized (set_params_lock) {
      if (ast_manager == null) ast_manager = new ASTManager();
      ast_manager.setProject(the_project,this,false);
      ast_manager.changePythonPath(paths,the_project);
      saveAstManager();
    }
}




public PybaseProject getProject()
{
   return the_project;
}




/********************************************************************************/
/*										*/
/*	Setup project and initialize						*/
/*										*/
/********************************************************************************/

public synchronized void setProject(final PybaseProject project)
{
   if (project == null) return;

   the_project = project;

   rebuildPath();
}


/**
 * Returns the directory that should store completions.
 *
 * @param p
 * @return
 */

public File getCompletionsCacheDir()
{
   File path = the_project.getBasePath();
   path = new File(path,".pybase");
   return path;
}


/**
 * @return the file where the python path helper should be saved.
 */
private File getAstOutputFile()
{
   File completionsCacheDir = getCompletionsCacheDir();
   return new File(completionsCacheDir,"v1_astmanager");
}


/**
 * @return Returns the completionsCache. Note that it can be null.
 */
public AbstractASTManager getAstManager()
{
   return ast_manager;
}

public boolean isOkToUse()
{
   return ast_manager != null && python_path_nature != null;
}


public PybasePathNature getPythonPathNature()
{
   return python_path_nature;
}


/**
 * Returns the Python version of the Project.
 *
 * It's a String in the format "python 2.4", as defined by the constants PYTHON_VERSION_XX and
 * JYTHON_VERSION_XX
 *
 * @note it might have changed on disk (e.g. a repository update).
 * @return the python version for the the_project
 */
public String getVersion()
{
   return the_project.getVersionName();
}

private String getInterpreter()
{
   return the_project.getBinary().getPath();
}


/**
 * @param version: the the_project version given the constants PYTHON_VERSION_XX and
 * JYTHON_VERSION_XX. If null, nothing is done for the version.
 *
 * @param interpreter the interpreter to be set if null, nothing is done to the interpreter.
 */
public void setVersion(String version,String interpreter)
{
}

public String getDefaultVersion()
{
   return PYTHON_VERSION_LATEST;
}


public void saveAstManager()
{
   if (ast_manager == null) return;

   File astOutputFile = getAstOutputFile();
   if (astOutputFile == null) return;

   synchronized (this) {
      ast_manager.saveToFile(astOutputFile);
    }
}



public PybaseInterpreterType getInterpreterType()
{
   if (interpreter_type == null) {
      String version = getVersion();
      if (Versions.ALL_JYTHON_VERSIONS.contains(version)) {
	 interpreter_type = PybaseInterpreterType.JYTHON;
       }
      else if (Versions.ALL_IRONPYTHON_VERSIONS.contains(version)) {
	 interpreter_type = PybaseInterpreterType.IRONPYTHON;
       }
      else {
	 // if others fail, consider it python
	 interpreter_type = PybaseInterpreterType.PYTHON;
       }
    }

   return interpreter_type;
}


public AbstractInterpreterManager getRelatedInterpreterManager()
{
   PybaseInterpreterType itype = getInterpreterType();

   return getInterpreterManager(itype);
}




// ------------------------------------------------------------------------------------------
// LOCAL CACHES
public void clearCaches(boolean global)
{
   interpreter_type = null;
   if (global) ModulesManager.clearCache();
}

public void clearBuiltinCompletions()
{
   getRelatedInterpreterManager().clearBuiltinCompletions(getInterpreter());
}

public AbstractToken[] getBuiltinCompletions()
{
   try {
      return getRelatedInterpreterManager().getBuiltinCompletions(getInterpreter());
    }
   catch (Exception e) {
      throw new RuntimeException(e);
    }
}

public AbstractModule getBuiltinMod()
{
   try {
      return getRelatedInterpreterManager().getBuiltinMod(getInterpreter());
    }
   catch (Exception e) {
      throw new RuntimeException(e);
    }
}

public void clearBuiltinMod()
{
   getRelatedInterpreterManager().clearBuiltinMod(getInterpreter());
}


/**
 * @return the version of the grammar as defined in GRAMMAR_PYTHON...
 */
public int getGrammarVersion()
{
   String version = getVersion();
   if (version == null) {
      return LATEST_GRAMMAR_VERSION;
    }

   List<String> splitted = StringUtils.split(version, ' ');
   if (splitted.size() != 2) {
      return LATEST_GRAMMAR_VERSION;
    }

   String vnum = splitted.get(1);
   return getGrammarVersionFromStr(vnum);
}

/**
 * @return info on the interpreter configured for this nature.
 *
 * @note that an exception will be raised if the
 */
public PybaseInterpreter getProjectInterpreter()
{
   String iname = getInterpreter();
   PybaseInterpreter ret;
   AbstractInterpreterManager iman = getRelatedInterpreterManager();

   if (DEFAULT_INTERPRETER.equals(iname)) {
      ret = iman.getDefaultInterpreterInfo(true);
    }
   else {
      ret = iman.getInterpreterInfo(iname);
    }

   return ret;
}


public String getProjectInterpreterName()
{ if (the_project == null) return null;
   return getInterpreter();
}


    /**
     * @param grammarVersion a string in the format 2.x or 3.x
     * @return the grammar version as given in GRAMMAR_PYTHON_VERSION
     */
public static int getGrammarVersionFromStr(String grammarVersion){
   //Note that we don't have the grammar for all versions, so, we use the one closer to it (which is
   //fine as they're backward compatible).
   if ("2.1".equals(grammarVersion)){
      return GRAMMAR_PYTHON_VERSION_2_4;

    }
   else if ("2.2".equals(grammarVersion)){
      return GRAMMAR_PYTHON_VERSION_2_4;
    }
   else if ("2.3".equals(grammarVersion)){
      return GRAMMAR_PYTHON_VERSION_2_4;
    }
   else if ("2.4".equals(grammarVersion)){
      return GRAMMAR_PYTHON_VERSION_2_4;
    }
   else if ("2.5".equals(grammarVersion)){
      return GRAMMAR_PYTHON_VERSION_2_5;
    }
   else if ("2.6".equals(grammarVersion)){
      return GRAMMAR_PYTHON_VERSION_2_6;
    }
   else if ("2.7".equals(grammarVersion)){
      return GRAMMAR_PYTHON_VERSION_2_7;
    }
   else if ("3.0".equals(grammarVersion)){
      return GRAMMAR_PYTHON_VERSION_3_0;
    }

   if (grammarVersion != null){
      if (grammarVersion.startsWith("3")){
	 return GRAMMAR_PYTHON_VERSION_3_0;
       }
      else if (grammarVersion.startsWith("2")){
	 //latest in the 2.x series
	 return LATEST_GRAMMAR_VERSION;
       }
    }

   PybaseMain.logE("Unable to recognize version: "+grammarVersion+" returning default.");
   return LATEST_GRAMMAR_VERSION;
}


/********************************************************************************/
/*										*/
/*	Interpreter management							*/
/*										*/
/********************************************************************************/

public static List<AbstractInterpreterManager> all_interpreters = new ArrayList<AbstractInterpreterManager>();

private static File  pybles_base = null;


public static AbstractInterpreterManager getInterpreterManager(PybaseInterpreterType itype)
{
   for (AbstractInterpreterManager aim : all_interpreters) {
      if (aim.getInterpreterType() == itype) return aim;
    }

   AbstractInterpreterManager rslt = null;
   PybasePreferences sysprefs = getPreferenceStore();
   switch (itype) {
      case PYTHON:
	 rslt = new PythonInterpreterManager(sysprefs);
	 break;
      case  JYTHON:
	 rslt = new JythonInterpreterManager(sysprefs);
	 break;
      case  IRONPYTHON:
	 rslt = new IronpythonInterpreterManager(sysprefs);
	 break;
      default:
	 throw new RuntimeException(
	       "Unable to find the related interpreter manager for type: "
	       + itype);
    }

   all_interpreters.add(rslt);
   return rslt;
}


public static AbstractInterpreterManager [] getAllInterpreterManagers()
{
   return all_interpreters.toArray(new AbstractInterpreterManager[0]);
}


public static boolean isWindowsPlatform()
{
   return System.getProperty("os.name").toLowerCase().contains("win");
}


public static PybasePreferences getPreferenceStore()
{
   return PybaseMain.getPybaseMain().getSystemPreferences();
}


public static File getScriptWithinPySrc(String cmd)
{
   computePyblesBase();
   File f1 = new File(pybles_base,"PySrc");
   return new File(f1,cmd);
}


private static void computePyblesBase()
{
   if (pybles_base != null) return;

   String indir = null;
   String jardir = null;

   String home = System.getProperty("user.home");
   File f1 = new File(home);
   File f2 = new File(f1,".pybles");
   File f3 = new File(f2,"System.props");
   Element xml = IvyXml.loadXmlFromFile(f3);
   for (Element ent : IvyXml.elementsByTag(xml,"entry")) {
      String k = IvyXml.getAttrString(ent,"key");
      if (k.equals("edu.brown.cs.bubbles.install")) {
	 indir = IvyXml.getText(ent);
       }
      else if (k.equals("edu.brown.cs.bubbles.jar")) {
	 jardir = IvyXml.getText(ent);
       }
    }
   if (indir == null && jardir == null) {
      System.err.println("PYBLES: Can't find pybles base directory");
      System.exit(1);
    }
   if (indir == null) indir = jardir;
   File f10 = new File(indir);
   File f11 = new File(f10,"pybles");

   pybles_base = f11;
}


public static List<PybaseNature> getAllPythonNatures()
{
   return new ArrayList<PybaseNature>(all_natures);
}




/********************************************************************************/
/*										*/
/*	Abstract Python Nature methods						*/
/*										*/
/********************************************************************************/

public boolean isResourceInPythonpath(String absPath) {
   return resolveModule(absPath) != null;
}

/**
* @param resource the resource we want to get the name from
* @return the name of the module in the environment
*/

public String resolveModule(File file) {
   return resolveModule(PybaseFileSystem.getFileAbsolutePath(file));
}



/********************************************************************************/
/*										*/
/*	File methods								*/
/*										*/
/********************************************************************************/

public static File getWorkspaceMetadataFile(String fileName)
{
   if (metadata_location == null) {
       try {
	  File path = PybaseMain.getPybaseMain().getWorkSpaceDirectory();
	  File mdir = new File(path,".metadata");
	  File pdir = new File(mdir,".pybase");
	  metadata_location = pdir;
	}
       catch (Exception e) {
	  throw new RuntimeException("If running in tests, call: setTestPlatformStateLocation", e);
	}
    }

   return new File(metadata_location, fileName);
}



/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/


@Override public String toString()
{
   return "PybaseNature: " + this.the_project;
}



/********************************************************************************/
/*										*/
/*	Versions of python that we know about					*/
/*										*/
/********************************************************************************/

/**
 * Constants persisted. Probably a better way would be disassociating whether it's python/jython and the
 * grammar version to be used (to avoid the explosion of constants below).
 */
public static final String PYTHON_VERSION_2_1 = "python 2.1";
public static final String PYTHON_VERSION_2_2 = "python 2.2";
public static final String PYTHON_VERSION_2_3 = "python 2.3";
public static final String PYTHON_VERSION_2_4 = "python 2.4";
public static final String PYTHON_VERSION_2_5 = "python 2.5";
public static final String PYTHON_VERSION_2_6 = "python 2.6";
public static final String PYTHON_VERSION_2_7 = "python 2.7";
public static final String PYTHON_VERSION_3_0 = "python 3.0";

public static final String JYTHON_VERSION_2_1 = "jython 2.1";
public static final String JYTHON_VERSION_2_2 = "jython 2.2";
public static final String JYTHON_VERSION_2_3 = "jython 2.3";
public static final String JYTHON_VERSION_2_4 = "jython 2.4";
public static final String JYTHON_VERSION_2_5 = "jython 2.5";
public static final String JYTHON_VERSION_2_6 = "jython 2.6";
public static final String JYTHON_VERSION_2_7 = "jython 2.7";
public static final String JYTHON_VERSION_3_0 = "jython 3.0";

public static final String IRONPYTHON_VERSION_2_1 = "ironpython 2.1";
public static final String IRONPYTHON_VERSION_2_2 = "ironpython 2.2";
public static final String IRONPYTHON_VERSION_2_3 = "ironpython 2.3";
public static final String IRONPYTHON_VERSION_2_4 = "ironpython 2.4";
public static final String IRONPYTHON_VERSION_2_5 = "ironpython 2.5";
public static final String IRONPYTHON_VERSION_2_6 = "ironpython 2.6";
public static final String IRONPYTHON_VERSION_2_7 = "ironpython 2.7";
public static final String IRONPYTHON_VERSION_3_0 = "ironpython 3.0";

//NOTE: It's the latest in the 2 series (3 is as if it's a totally new thing)
public static final String JYTHON_VERSION_LATEST = JYTHON_VERSION_2_6;
public static final String PYTHON_VERSION_LATEST = PYTHON_VERSION_2_7;


public static class Versions{

   public static final HashSet<String> ALL_PYTHON_VERSIONS = new HashSet<String>();
   public static final HashSet<String> ALL_JYTHON_VERSIONS = new HashSet<String>();
   public static final HashSet<String> ALL_IRONPYTHON_VERSIONS = new HashSet<String>();
   public static final HashSet<String> ALL_VERSIONS_ANY_FLAVOR = new HashSet<String>();
   public static final List<String> VERSION_NUMBERS = new ArrayList<String>();
   public static final String LAST_VERSION_NUMBER = "2.7";

   static{
      ALL_PYTHON_VERSIONS.add(PYTHON_VERSION_2_1);
      ALL_PYTHON_VERSIONS.add(PYTHON_VERSION_2_2);
      ALL_PYTHON_VERSIONS.add(PYTHON_VERSION_2_3);
      ALL_PYTHON_VERSIONS.add(PYTHON_VERSION_2_4);
      ALL_PYTHON_VERSIONS.add(PYTHON_VERSION_2_5);
      ALL_PYTHON_VERSIONS.add(PYTHON_VERSION_2_6);
      ALL_PYTHON_VERSIONS.add(PYTHON_VERSION_2_7);
      ALL_PYTHON_VERSIONS.add(PYTHON_VERSION_3_0);

      ALL_JYTHON_VERSIONS.add(JYTHON_VERSION_2_1);
      ALL_JYTHON_VERSIONS.add(JYTHON_VERSION_2_2);
      ALL_JYTHON_VERSIONS.add(JYTHON_VERSION_2_3);
      ALL_JYTHON_VERSIONS.add(JYTHON_VERSION_2_4);
      ALL_JYTHON_VERSIONS.add(JYTHON_VERSION_2_5);
      ALL_JYTHON_VERSIONS.add(JYTHON_VERSION_2_6);
      ALL_JYTHON_VERSIONS.add(JYTHON_VERSION_2_7);
      ALL_JYTHON_VERSIONS.add(JYTHON_VERSION_3_0);

      ALL_IRONPYTHON_VERSIONS.add(IRONPYTHON_VERSION_2_1);
      ALL_IRONPYTHON_VERSIONS.add(IRONPYTHON_VERSION_2_2);
      ALL_IRONPYTHON_VERSIONS.add(IRONPYTHON_VERSION_2_3);
      ALL_IRONPYTHON_VERSIONS.add(IRONPYTHON_VERSION_2_4);
      ALL_IRONPYTHON_VERSIONS.add(IRONPYTHON_VERSION_2_5);
      ALL_IRONPYTHON_VERSIONS.add(IRONPYTHON_VERSION_2_6);
      ALL_IRONPYTHON_VERSIONS.add(IRONPYTHON_VERSION_2_7);
      ALL_IRONPYTHON_VERSIONS.add(IRONPYTHON_VERSION_3_0);

      VERSION_NUMBERS.add("2.1");
      VERSION_NUMBERS.add("2.2");
      VERSION_NUMBERS.add("2.3");
      VERSION_NUMBERS.add("2.4");
      VERSION_NUMBERS.add("2.5");
      VERSION_NUMBERS.add("2.6");
      VERSION_NUMBERS.add("2.7");
      VERSION_NUMBERS.add("3.0");

      ALL_VERSIONS_ANY_FLAVOR.addAll(ALL_JYTHON_VERSIONS);
      ALL_VERSIONS_ANY_FLAVOR.addAll(ALL_PYTHON_VERSIONS);
      ALL_VERSIONS_ANY_FLAVOR.addAll(ALL_IRONPYTHON_VERSIONS);
    }
}



}	// end of class PybaseNature




/* end of PybaseNature.java*/

