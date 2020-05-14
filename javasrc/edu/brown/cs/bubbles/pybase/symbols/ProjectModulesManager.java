/********************************************************************************/
/*										*/
/*		ProjectModulesManager.java					*/
/*										*/
/*	Python Bubbles Base modules manager for user modules			*/
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
 * Created on May 24, 2005
 *
 * @author Fabio Zadrozny
 */

package edu.brown.cs.bubbles.pybase.symbols;

import edu.brown.cs.bubbles.pybase.PybaseInterpreter;
import edu.brown.cs.bubbles.pybase.PybaseMain;
import edu.brown.cs.bubbles.pybase.PybaseNature;
import edu.brown.cs.bubbles.pybase.PybaseProject;

import org.python.pydev.core.DeltaSaver;
import org.python.pydev.core.Tuple;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 * @author Fabio Zadrozny
 */
public final class ProjectModulesManager extends ModulesManagerWithBuild {


private static final boolean   DEBUG_MODULES = false;

// these attributes must be set whenever this class is restored.
private volatile PybaseProject	    for_project;
private volatile PybaseNature the_nature;


public ProjectModulesManager()
{}

public void setProject(PybaseProject project,PybaseNature nature,boolean restoreDeltas)
{
   this.for_project = project;
   this.the_nature = nature;
   File completionsCacheDir = this.the_nature.getCompletionsCacheDir();
   if (completionsCacheDir == null) {
      return; // project was deleted.
   }

   this.deltaSaver = new DeltaSaver<ModulesKey>(completionsCacheDir,"v1_astdelta",
	    readFromFileMethod,toFileMethod);

   if (!restoreDeltas) {
      deltaSaver.clearAll(); // remove any existing deltas
   }
   else {
      deltaSaver.processDeltas(this); // process the current deltas (clears current deltas
// automatically and saves it when the processing is concluded)
   }
}


// ------------------------ delta processing


@Override public void endProcessing()
{
   // save it with the updated info
   the_nature.saveAstManager();
}


// ------------------------ end delta processing


@Override public void setPythonNature(PybaseNature nature)
{
   this.the_nature = nature;
}

@Override public PybaseNature getNature()
{
   return the_nature;
}

/**
 * @param defaultSelectedInterpreter
 */
public SystemModulesManager getSystemModulesManager()
{
   if (the_nature == null) {
      PybaseMain.logI("Nature still not set in getSystemsModulesManager");
      return null; // still not set (initialization)
   }
   try {
      return the_nature.getProjectInterpreter().getModulesManager();
   }
   catch (Exception e1) {
      return null;
   }
}

@Override public Set<String> getAllModuleNames(boolean addDependencies,
	 String partStartingWithLowerCase)
{
   if (addDependencies) {
      Set<String> s = new HashSet<String>();
      ModulesManager[] managersInvolved = this.getManagersInvolved(true);
      for (int i = 0; i < managersInvolved.length; i++) {
	 s.addAll(managersInvolved[i].getAllModuleNames(false, partStartingWithLowerCase));
      }
      return s;
   }
   else {
      return super.getAllModuleNames(addDependencies, partStartingWithLowerCase);
   }
}

/**
 * @return all the modules that start with some token (from this manager and others involved)
 */
@Override public SortedMap<ModulesKey, ModulesKey> getAllModulesStartingWith(
	 String strStartingWith)
{
   SortedMap<ModulesKey, ModulesKey> ret = new TreeMap<ModulesKey, ModulesKey>();
   ModulesManager[] managersInvolved = this.getManagersInvolved(true);
   for (int i = 0; i < managersInvolved.length; i++) {
      ret.putAll(managersInvolved[i].getAllDirectModulesStartingWith(strStartingWith));
   }
   return ret;
}


@Override public AbstractModule getModule(String name,PybaseNature nature,boolean dontSearchInit)
{
   return getModule(name, nature, true, dontSearchInit);
}

/**
 * When looking for relative, we do not check dependencies
 */
@Override public AbstractModule getRelativeModule(String name,PybaseNature nature)
{
   return super.getModule(false, name, nature, true); // cannot be a compiled module
}

@Override public AbstractModule getModule(String name,PybaseNature nature,boolean checkSystemManager,
	 boolean dontSearchInit)
{
   Tuple<AbstractModule, ModulesManager> ret = getModuleAndRelatedModulesManager(name, nature,
	    checkSystemManager, dontSearchInit);
   if (ret != null) {
      return ret.o1;
   }
   return null;
}


/**
 * @return a tuple with the AbstractModule requested and the ModulesManager that contained that module.
 */
@Override public Tuple<AbstractModule, ModulesManager> getModuleAndRelatedModulesManager(String name,
	 PybaseNature nature,boolean checkSystemManager,boolean dontSearchInit)
{

   AbstractModule module = null;

   ModulesManager[] managersInvolved = this.getManagersInvolved(true); // only get the
// system manager here (to avoid recursion)

   for (ModulesManager m : managersInvolved) {
      if (m instanceof SystemModulesManager) {
	 module = ((SystemModulesManager) m).getBuiltinModule(name, nature, dontSearchInit);
	 if (module != null) {
	    if (DEBUG_MODULES) {
	       System.out.println("Trying to get:" + name + " - " + " returned builtin:"
			+ module + " - " + m.getClass());
	    }
	    return new Tuple<AbstractModule, ModulesManager>(module,m);
	 }
      }
   }

   for (ModulesManager m : managersInvolved) {
      if (m instanceof ProjectModulesManager) {
	 ProjectModulesManager pM = (ProjectModulesManager) m;
	 module = pM.getModuleInDirectManager(name, nature, dontSearchInit);

      }
      else if (m instanceof SystemModulesManager) {
	 SystemModulesManager systemModulesManager = (SystemModulesManager) m;
	 module = systemModulesManager.getModuleWithoutBuiltins(name, nature,
		  dontSearchInit);

      }
      else {
	 throw new RuntimeException("Unexpected: " + m);
      }

      if (module != null) {
	 if (DEBUG_MODULES) {
	    System.out.println("Trying to get:" + name + " - " + " returned:" + module
		     + " - " + m.getClass());
	 }
	 return new Tuple<AbstractModule, ModulesManager>(module,m);
      }
   }
   if (DEBUG_MODULES) {
      System.out.println("Trying to get:" + name + " - " + " returned:null - "
	       + this.getClass());
   }
   return null;
}

/**
 * Only searches the modules contained in the direct modules manager.
 */
public AbstractModule getModuleInDirectManager(String name,PybaseNature nature,
	 boolean dontSearchInit)
{
   return super.getModule(name, nature, dontSearchInit);
}

public String resolveModuleOnlyInProjectSources(String fileAbsolutePath,
	 boolean addExternal)
{
   Set<String> projectSourcePath = the_nature.getPythonPathNature().getProjectSourcePathSet();

   return this.pythonPathHelper.resolveModule(fileAbsolutePath, new ArrayList<String>(
	    projectSourcePath));
}


@Override public String resolveModule(String full)
{
   return resolveModule(full, true);
}

public String resolveModule(String full,boolean checkSystemManager)
{
   ModulesManager[] managersInvolved = this.getManagersInvolved(checkSystemManager);
   for (ModulesManager m : managersInvolved) {

      String mod;
      if (m instanceof ProjectModulesManager) {
	 ProjectModulesManager pM = (ProjectModulesManager) m;
	 mod = pM.resolveModuleInDirectManager(full);

      }
      else {
	 mod = m.resolveModule(full);
      }

      if (mod != null) {
	 return mod;
      }
   }
   return null;
}

public String resolveModuleInDirectManager(String full)
{
   return super.resolveModule(full);
}

@Override public int getSize(boolean addDependenciesSize)
{
   if (addDependenciesSize) {
      int size = 0;
      ModulesManager[] managersInvolved = this.getManagersInvolved(true);
      for (int i = 0; i < managersInvolved.length; i++) {
	 size += managersInvolved[i].getSize(false);
      }
      return size;
   }
   else {
      return super.getSize(addDependenciesSize);
   }
}


public String[] getBuiltins()
{
   String[] builtins = null;
   SystemModulesManager systemModulesManager = getSystemModulesManager();
   if (systemModulesManager != null) {
      builtins = systemModulesManager.getBuiltins();
   }
   return builtins;
}


/**
 * @param checkSystemManager whether the system manager should be added
 * @param referenced true if we should get the referenced projects
 *			 false if we should get the referencing projects
 * @return the Managers that this project references or the ones that reference this project (depends on 'referenced')
 *
 * Change in 1.3.3: adds itself to the list of returned managers
 */
private synchronized ModulesManager[] getManagers(boolean checkSystemManager,
	 boolean referenced)
{
   if (this.completionCache != null) {
      ModulesManager[] ret = this.completionCache.getManagers(referenced);
      if (ret != null) {
	 return ret;
      }
   }
   ArrayList<ModulesManager> list = new ArrayList<ModulesManager>();
   SystemModulesManager systemModulesManager = getSystemModulesManager();
   if (systemModulesManager == null) {
      // may happen in initialization
      return new ModulesManager[] {};
   }

   // add itself 1st
   list.add(this);

   // get the projects 1st
   if (for_project != null) {
      Set<PybaseProject> projs;
      if (referenced) {
	 projs = getReferencedProjects(for_project);
      }
      else {
	 projs = getReferencingProjects(for_project);
      }
      addModuleManagers(list, projs);
   }

   // the system is the last one we add
   // http://sourceforge.net/tracker/index.php?func=detail&aid=1687018&group_id=85796&atid=577329
   if (checkSystemManager && systemModulesManager != null) {
      list.add(systemModulesManager);
   }

   ModulesManager[] ret = list.toArray(new ModulesManager[list.size()]);
   if (this.completionCache != null) {
      this.completionCache.setManagers(ret, referenced);
   }
   return ret;
}


public static Set<PybaseProject> getReferencingProjects(PybaseProject project)
{
   HashSet<PybaseProject> memo = new HashSet<PybaseProject>();
   getProjectsRecursively(project, false, memo);
   memo.remove(project); // shouldn't happen unless we've a cycle...
   return memo;
}

public static Set<PybaseProject> getReferencedProjects(PybaseProject project)
{
   HashSet<PybaseProject> memo = new HashSet<PybaseProject>();
   getProjectsRecursively(project, true, memo);
   memo.remove(project); // shouldn't happen unless we've a cycle...
   return memo;
}

/**
 * @param project the project for which we want references.
 * @param referenced whether we want to get the referenced projects or the ones referencing this one.
 * @param memo (out) this is the place where all the projects will e available.
 *
 * Note: the project itself will not be added.
 */
private static void getProjectsRecursively(PybaseProject project,boolean referenced,
	 HashSet<PybaseProject> memo)
{
   PybaseProject[] projects = null;
   if (project == null || !project.isOpen() || !project.exists()
	    || memo.contains(project)) {
      return;
   }
   if (referenced) {
      projects = project.getReferencedProjects();
   }
   else {
      projects = project.getReferencingProjects();
   }

   if (projects != null) {
      for (PybaseProject p : projects) {
	 if (!memo.contains(p)) {
	    memo.add(p);
	    getProjectsRecursively(p, referenced, memo);
	 }
      }
   }
}


/**
 * @param list the list that will be filled with the managers
 * @param projects the projects that should have the managers added
 */
private void addModuleManagers(ArrayList<ModulesManager> list,
	 Collection<PybaseProject> projects)
{
   for (PybaseProject project : projects) {
      PybaseNature nature = project.getNature();
      if (nature != null) {
	 AbstractASTManager otherProjectAstManager = nature.getAstManager();
	 if (otherProjectAstManager != null) {
	    ModulesManager projectModulesManager = otherProjectAstManager
		     .getModulesManager();
	    if (projectModulesManager != null) {
	       list.add(projectModulesManager);
	    }
	 }
	 else {
	    String msg = "No ast manager configured for :" + project.getName();
	    PybaseMain.logW(msg);
	 }
      }
   }
}


/**
 * @return Returns the managers that this project references(does not include itself).
 */
public ModulesManager[] getManagersInvolved(boolean checkSystemManager)
{
   return getManagers(checkSystemManager, true);
}

/**
 * @return Returns the managers that reference this project (does not include itself).
 */
public ModulesManager[] getRefencingManagersInvolved(boolean checkSystemManager)
{
   return getManagers(checkSystemManager, false);
}


@Override public List<String> getCompletePythonPath(PybaseInterpreter interpreter,
	 AbstractInterpreterManager manager)
{
   List<String> l = new ArrayList<String>();
   ModulesManager[] managersInvolved = getManagersInvolved(true);
   for (ModulesManager m : managersInvolved) {
      if (m instanceof SystemModulesManager) {
	 SystemModulesManager systemModulesManager = (SystemModulesManager) m;
	 l.addAll(systemModulesManager.getCompletePythonPath(interpreter, manager));

      }
      else {
	 PythonPathHelper h = m.getPythonPathHelper();
	 if (h != null) {
	    l.addAll(h.getPythonpath());
	 }
      }
   }
   return l;
}


}
