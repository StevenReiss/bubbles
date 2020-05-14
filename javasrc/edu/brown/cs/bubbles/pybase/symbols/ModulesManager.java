/********************************************************************************/
/*										*/
/*		ModulesManager.java						*/
/*										*/
/*	Python Bubbles Base modules manager					*/
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

import edu.brown.cs.bubbles.pybase.PybaseFileSystem;
import edu.brown.cs.bubbles.pybase.PybaseInterpreter;
import edu.brown.cs.bubbles.pybase.PybaseMain;
import edu.brown.cs.bubbles.pybase.PybaseNature;
import edu.brown.cs.bubbles.pybase.PybaseProject;
import edu.brown.cs.bubbles.pybase.symbols.ModulesFoundStructure.ZipContents;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.Tuple;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.expr_contextType;
import org.python.pydev.parser.jython.ast.stmtType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 * This class manages the modules that are available
 *
 * @author Fabio Zadrozny
 */
public abstract class ModulesManager {

/**
 *
 */
private static final String  MODULES_MANAGER_V1      = "MODULES_MANAGER_V1\n";

private final static boolean DEBUG_BUILD	     = false;

private final static boolean DEBUG_TEMPORARY_MODULES = false;

private final static boolean DEBUG_ZIP	       = false;

public static final int MAXIMUM_NUMBER_OF_DELTAS = 100;



public ModulesManager()
{}

/**
 * This class is a cache to help in getting the managers that are referenced or referred.
 *
 * It will not actually make any computations (the managers must be set from the outside)
 */
protected static class CompletionCache {
   public ModulesManager[] referencedManagers;

   public ModulesManager[] referredManagers;

   public ModulesManager[] getManagers(boolean referenced)
      {
      if (referenced) {
	 return referencedManagers;
       }
      else {
	 return referredManagers;
       }
    }

   public void setManagers(ModulesManager[] ret,boolean referenced)
      {
      if (referenced) {
	 referencedManagers = ret;
       }
      else {
	 referredManagers = ret;
       }
    }
}

/**
 * A stack for keeping the completion cache
 */
protected volatile CompletionCache completionCache     = null;
protected final Object	     lockCompletionCache = new Object();

private volatile int	       completionCacheI    = 0;

/**
 * This method starts a new cache for this manager, so that needed info is kept while the request is happening
 * (so, some info may not need to be re-asked over and over for requests)
 */
public boolean startCompletionCache()
{
   synchronized (lockCompletionCache) {
      if (completionCache == null) {
	 completionCache = new CompletionCache();
       }
      completionCacheI += 1;
    }
   return true;
}

public void endCompletionCache()
{
   synchronized (lockCompletionCache) {
      completionCacheI -= 1;
      if (completionCacheI == 0) {
	 completionCache = null;
       }
      else if (completionCacheI < 0) {
	 throw new RuntimeException("Completion cache negative (request unsynched)");
       }
    }
}

/**
 * Modules that we have in memory. This is persisted when saved.
 *
 * Keys are ModulesKey with the name of the module. Values are AbstractModule objects.
 *
 * Implementation changed to contain a cache, so that it does not grow to much (some out of memo errors
										   * were thrown because of the may size when having too many modules).
 *
 * It is sorted so that we can get things in a 'subtree' faster
 */
protected final ModulesKeyTreeMap<ModulesKey, ModulesKey> modulesKeys	   = new ModulesKeyTreeMap<ModulesKey, ModulesKey>();
protected final Object				    modulesKeysLock  = new Object();

protected static final ModulesManagerCache		cache	    = new ModulesManagerCache();

/**
 * This is the set of files that was found just right after unpickle (it should not be changed after that,
									 * and serves only as a reference cache).
 */
protected final Set<File>				 files	    = new HashSet<File>();

/**
 * Helper for using the pythonpath. Also persisted.
 */
protected PythonPathHelper				pythonPathHelper = new PythonPathHelper();

public PythonPathHelper getPythonPathHelper()
{
   return pythonPathHelper;
}

public void setPythonPathHelper(Object pathHelper)
{
   if (!(pathHelper instanceof PythonPathHelper)) {
      throw new IllegalArgumentException();
    }
   pythonPathHelper = (PythonPathHelper) pathHelper;
}


public void saveToFile(File workspacemetadatafile)
{
   if (workspacemetadatafile.exists() && !workspacemetadatafile.isDirectory()) {
      try {
	 PybaseFileSystem.deleteFile(workspacemetadatafile);
       }
      catch (IOException e) {
	 throw new RuntimeException(e);
       }
    }
   if (!workspacemetadatafile.exists()) {
      workspacemetadatafile.mkdirs();
    }

   File modulesKeysFile = new File(workspacemetadatafile,"modulesKeys");
   File pythonpatHelperFile = new File(workspacemetadatafile,"pythonpath");
   StringBuilder buf;
   synchronized (modulesKeysLock) {
      buf = new StringBuilder(modulesKeys.size() * 50);
      buf.append(MODULES_MANAGER_V1);

      for (Iterator<ModulesKey> iter = this.modulesKeys.keySet().iterator(); iter.hasNext();) {
	 ModulesKey next = iter.next();
	 buf.append(next.getModuleName());
	 if (next.getModuleFile() != null) {
	    buf.append("|");
	    buf.append(next.getModuleFile().toString());
	  }
	 buf.append('\n');
       }
    }
   PybaseFileSystem.writeStrToFile(buf.toString(), modulesKeysFile);

   pythonPathHelper.saveToFile(pythonpatHelperFile);
}



public static void loadFromFile(ModulesManager modulesManager,File workspacemetadatafile)
throws IOException
{
   if (workspacemetadatafile.exists() && !workspacemetadatafile.isDirectory()) {
      throw new IOException("Expecting: " + workspacemetadatafile + " to be a directory.");
    }
   File modulesKeysFile = new File(workspacemetadatafile,"modulesKeys");
   File pythonpatHelperFile = new File(workspacemetadatafile,"pythonpath");
   if (!modulesKeysFile.isFile()) {
      throw new IOException("Expecting: " + modulesKeysFile
			       + " to exist (and be a file).");
    }
   if (!pythonpatHelperFile.isFile()) {
      throw new IOException("Expecting: " + pythonpatHelperFile
			       + " to exist (and be a file).");
    }

   String fileContents = PybaseFileSystem.getFileContents(modulesKeysFile);
   if (!fileContents.startsWith(MODULES_MANAGER_V1)) {
      throw new RuntimeException("Could not load modules manager from " + modulesKeysFile
				    + " (version not detected).");
    }

   fileContents = fileContents.substring(MODULES_MANAGER_V1.length());
   for (String line : StringUtils.iterLines(fileContents)) {
      line = line.trim();
      List<String> split = StringUtils.split(line, '|');
      ModulesKey key = null;
      if (split.size() == 1) {
	 key = new ModulesKey(split.get(0),null);

       }
      else if (split.size() == 2) {
	 key = new ModulesKey(split.get(0),new File(split.get(1)));

       }
      if (key != null) {
	 // restore with empty modules.
	 modulesManager.modulesKeys.put(key, key);
	 if (key.getModuleFile() != null) {
	    modulesManager.files.add(key.getModuleFile());
	  }
       }
    }

   PythonPathHelper helper = new PythonPathHelper();
   helper.loadFromFile(pythonpatHelperFile);

   modulesManager.pythonPathHelper = helper;

   if (modulesManager.pythonPathHelper == null) {
      throw new IOException("Pythonpath helper not properly restored. "
			       + modulesManager.getClass().getName() + " dir:" + workspacemetadatafile);
    }

   if (modulesManager.pythonPathHelper.getPythonpath() == null) {
      throw new IOException("Pythonpath helper pythonpath not properly restored. "
			       + modulesManager.getClass().getName() + " dir:" + workspacemetadatafile);
    }

   if (modulesManager.pythonPathHelper.getPythonpath().size() == 0) {
      throw new IOException("Pythonpath helper pythonpath restored with no contents. "
			       + modulesManager.getClass().getName() + " dir:" + workspacemetadatafile);
    }

   if (modulesManager.modulesKeys.size() < 2) { // if we have to few modules, that may
      // indicate a problem...
      // if the project is really small, modulesManager will be fast, otherwise, it'll fix
      // the problem.
      // Note: changed to a really low value because we now make a check after it's
      // restored anyways.
      throw new IOException("Only " + modulesManager.modulesKeys.size()
			       + " modules restored in I/O. " + modulesManager.getClass().getName()
			       + " dir:" + workspacemetadatafile);
    }

}


/**
 * @return Returns the modules.
 */
protected Map<ModulesKey, AbstractModule> getModules()
{
   throw new RuntimeException("Deprecated");
}


/**
 * Change the pythonpath (used for both: system and project)
 *
 * @param project: may be null
 * @param defaultSelectedInterpreter: may be null
 */
public void changePythonPath(String pythonpath,final PybaseProject project)
{
   pythonPathHelper.setPythonPath(pythonpath);
   ModulesFoundStructure modulesFound = pythonPathHelper.getModulesFoundStructure();

   ModulesKeyTreeMap<ModulesKey, ModulesKey> keys = buildKeysFromModulesFound(modulesFound);

   synchronized (modulesKeysLock) {
      // assign to instance variable
      modulesKeys.clear();
      modulesKeys.putAll(keys);
    }

}


/**
 * @return a tuple with the new keys to be added to the modules manager (i.e.: found in keysFound but not in the
									    * modules manager) and the keys to be removed from the modules manager (i.e.: found in the modules manager but
																		       * not in the keysFound)
 */
public Tuple<List<ModulesKey>,List<ModulesKey>> diffModules(ModulesKeyTreeMap<ModulesKey,ModulesKey> keysFound)
{
   ArrayList<ModulesKey> newKeys = new ArrayList<ModulesKey>();
   ArrayList<ModulesKey> removedKeys = new ArrayList<ModulesKey>();
   Iterator<ModulesKey> it = keysFound.keySet().iterator();

   synchronized (modulesKeysLock) {
      while (it.hasNext()) {
	 ModulesKey next = it.next();
	 if (!modulesKeys.containsKey(next)) {
	    newKeys.add(next);
	  }
       }

      it = modulesKeys.keySet().iterator();
      while (it.hasNext()) {
	 ModulesKey next = it.next();
	 if (!keysFound.containsKey(next)) {
	    removedKeys.add(next);
	  }
       }
    }

   return new Tuple<List<ModulesKey>, List<ModulesKey>>(newKeys,removedKeys);
}


public ModulesKeyTreeMap<ModulesKey,ModulesKey> buildKeysFromModulesFound(ModulesFoundStructure modulesFound)
{
   // now, on to actually filling the module keys
   ModulesKeyTreeMap<ModulesKey, ModulesKey> keys = new ModulesKeyTreeMap<ModulesKey, ModulesKey>();
   int j = 0;

   StringBuilder buffer = new StringBuilder();
   // now, create in memory modules for all the loaded files (empty modules).
   for (Iterator<Map.Entry<File, String>> iterator = modulesFound.regularModules
	   .entrySet().iterator(); iterator.hasNext(); j++) {
      Map.Entry<File, String> entry = iterator.next();
      File f = entry.getKey();
      String m = entry.getValue();

      if (j % 20 == 0) {
	 // no need to report all the time (that's pretty fast now)
	 buffer.setLength(0);
       }

      if (m != null) {
	 // we don't load them at this time.
	 ModulesKey modulesKey = new ModulesKey(m,f);

	 // no conflict (easy)
	 if (!keys.containsKey(modulesKey)) {
	    keys.put(modulesKey, modulesKey);
	  }
	 else {
	    // we have a conflict, so, let's resolve which one to keep (the old one or
	    // this one)
	    if (PythonPathHelper.isValidSourceFile(f.getName())) {
	       // source files have priority over other modules (dlls) -- if both are
	       // source, there is no real way to resolve
	       // this priority, so, let's just add it over.
	       keys.put(modulesKey, modulesKey);
	     }
	  }
       }
    }

   for (ZipContents zipContents : modulesFound.zipContents) {
      for (String filePathInZip : zipContents.found_file_zip_paths) {
	 String modName = StringUtils.stripExtension(filePathInZip).replace('/', '.');
	 if (DEBUG_ZIP) {
	    System.out.println("Found in zip:" + modName);
	  }
	 ModulesKey k = new ModulesKeyForZip(modName,zipContents.zip_file,filePathInZip,
						true);
	 keys.put(k, k);

	 if (zipContents.zip_contents_type == ZipContents.ZIP_CONTENTS_TYPE_JAR) {
	    // folder modules are only created for jars (because for python files, the
	    // __init__.py is required).
	    for (String s : new FullRepIterable(
		    FullRepIterable.getWithoutLastPart(modName))) { // the one without
	       // the last part was already added
	       k = new ModulesKeyForZip(s,zipContents.zip_file,s.replace('.', '/'),false);
	       keys.put(k, k);
	     }
	  }
       }
    }

   onChangePythonpath(keys);
   return keys;
}

/**
      * Subclasses may do more things after the defaults were added to the cache (e.g.: the system modules manager may
										     * add builtins)
      */
protected void onChangePythonpath(SortedMap<ModulesKey, ModulesKey> keys)
{}

/**
      * This is the only method that should remove a module.
      * No other method should remove them directly.
      *
      * @param key this is the key that should be removed
      */
protected void doRemoveSingleModule(ModulesKey key)
{
   synchronized (modulesKeysLock) {
      if (DEBUG_BUILD) {
	 System.out.println("Removing module:" + key + " - " + getClass());
       }
      modulesKeys.remove(key);
      ModulesManager.cache.remove(key, this);
    }
}

/**
      * This method that actually removes some keys from the modules.
      *
      * @param toRem the modules to be removed
      */
protected void removeThem(Collection<ModulesKey> toRem)
{
   // really remove them here.
   for (Iterator<ModulesKey> iter = toRem.iterator(); iter.hasNext();) {
      doRemoveSingleModule(iter.next());
    }
}

public void removeModules(Collection<ModulesKey> toRem)
{
   removeThem(toRem);
}

public AbstractModule addModule(final ModulesKey key)
{
   AbstractModule ret = AbstractModule.createEmptyModule(key);
   doAddSingleModule(key, ret);
   return ret;
}

/**
      * This is the only method that should add / update a module.
      * No other method should add it directly (unless it is loading or rebuilding it).
      *
      * @param key this is the key that should be added
      * @param n
      */
public void doAddSingleModule(final ModulesKey key,AbstractModule n)
{
   if (DEBUG_BUILD) {
      System.out.println("Adding module:" + key + " - " + getClass());
    }
   synchronized (modulesKeysLock) {
      modulesKeys.put(key, key);
      ModulesManager.cache.add(key, n, this);
    }
}

/**
      * @return a set of all module keys
      *
      * Note: addDependencies ignored at this point.
      */
public Set<String> getAllModuleNames(boolean addDependencies,
					String partStartingWithLowerCase)
{
   Set<String> s = new HashSet<String>();
   synchronized (modulesKeysLock) {
      for (ModulesKey key : modulesKeys.keySet()) {
	 if (key.hasPartStartingWith(partStartingWithLowerCase)) {
	    s.add(key.getModuleName());
	  }
       }
    }
   return s;
}

public SortedMap<ModulesKey, ModulesKey> getAllDirectModulesStartingWith(String strStartingWith)
{
   if (strStartingWith.length() == 0) {
      synchronized (modulesKeysLock) {
	 // we don't want it to be backed up by the same set (because it may be changed,
	 // so, we may get
	 // a java.util.ConcurrentModificationException on places that use it)
	 return new ModulesKeyTreeMap<ModulesKey, ModulesKey>(modulesKeys);
       }
    }
   ModulesKey startingWith = new ModulesKey(strStartingWith,null);
   ModulesKey endingWith = new ModulesKey(startingWith + "z",null);
   synchronized (modulesKeysLock) {
      // we don't want it to be backed up by the same set (because it may be changed, so,
      // we may get
      // a java.util.ConcurrentModificationException on places that use it)
      return new ModulesKeyTreeMap<ModulesKey, ModulesKey>(modulesKeys.subMap(
							      startingWith, endingWith));
    }
}

public SortedMap<ModulesKey, ModulesKey> getAllModulesStartingWith(String strStartingWith)
{
   return getAllDirectModulesStartingWith(strStartingWith);
}

public ModulesKey[] getOnlyDirectModules()
{
   synchronized (modulesKeysLock) {
      return modulesKeys.keySet().toArray(new ModulesKey[0]);
    }
}

/**
      * Note: no dependencies at this point (so, just return the keys)
      */
public int getSize(boolean addDependenciesSize)
{
   synchronized (modulesKeysLock) {
      return modulesKeys.size();
    }
}

public AbstractModule getModule(String name,PybaseNature nature,boolean dontSearchInit)
{
   return getModule(true, name, nature, dontSearchInit);
}

/**
      * Note that the access must be synched.
      */
public final Map<String, SortedMap<Integer, AbstractModule>> temporaryModules	   = new HashMap<String, SortedMap<Integer, AbstractModule>>();
private final Object				  lockTemporaryModules = new Object();
private int					   nextHandle	   = 0;

/**
      * Returns the handle to be used to remove the module added later on!
      */
public int pushTemporaryModule(String moduleName,AbstractModule module)
{
   synchronized (lockTemporaryModules) {
      SortedMap<Integer, AbstractModule> map = temporaryModules.get(moduleName);
      if (map == null) {
	 map = new TreeMap<Integer, AbstractModule>(); // small initial size!
	 temporaryModules.put(moduleName, map);
       }
      nextHandle += 1; // Note: don't care about stack overflow!
      map.put(nextHandle, module);
      return nextHandle;
    }

}

public void popTemporaryModule(String moduleName,int handle)
{
   synchronized (lockTemporaryModules) {
      SortedMap<Integer, AbstractModule> stack = temporaryModules.get(moduleName);
      try {
	 if (stack != null) {
	    stack.remove(handle);
	    if (stack.size() == 0) {
	       temporaryModules.remove(moduleName);
	     }
	  }
       }
      catch (Throwable e) {
	 PybaseMain.logE("Problem popping temporary module",e);
       }
    }
}

/**
      * This method returns the module that corresponds to the path passed as a parameter.
      *
      * @param name the name of the module we're looking for  (e.g.: mod1.mod2)
						   * @param dontSearchInit is used in a negative form because initially it was isLookingForRelative, but
						   * it actually defines if we should look in __init__ modules too, so, the name matches the old signature.
						   *
						   * NOTE: isLookingForRelative description was: when looking for relative imports, we don't check for __init__
      * @return the module represented by this name
      */
protected AbstractModule getModule(boolean acceptCompiledModule,String name,
				      PybaseNature nature,boolean dontSearchInit)
{
   synchronized (lockTemporaryModules) {
      SortedMap<Integer, AbstractModule> map = temporaryModules.get(name);
      if (map != null && map.size() > 0) {
	 if (DEBUG_TEMPORARY_MODULES) {
	    System.out.println("Returning temporary module: " + name);
	  }
	 return map.get(map.lastKey());
       }
    }
   AbstractModule n = null;
   ModulesKey keyForCacheAccess = new ModulesKey(null,null);

   if (!dontSearchInit) {
      if (n == null) {
	 keyForCacheAccess.setModuleName(name + ".__init__"); 
	 n = cache.getObj(keyForCacheAccess, this);
	 if (n != null) {
	    name += ".__init__";
	  }
       }
    }
   if (n == null) {
      keyForCacheAccess.setModuleName(name);
      n = cache.getObj(keyForCacheAccess, this);
    }

   if (n instanceof SourceModule) {
      // ok, module exists, let's check if it is synched with the filesystem version...
      SourceModule s = (SourceModule) n;
      if (!s.isSynched()) {
	 // change it for an empty and proceed as usual.
	 n = addModule(createModulesKey(s.getName(), s.getFile()));
       }
    }

   if (n instanceof EmptyModule) {
      EmptyModule e = (EmptyModule) n;

      boolean found = false;

      if (!found && e.getFile() != null) {

	 if (!e.getFile().exists()) {
	    // if the file does not exist anymore, just remove it.
	    keyForCacheAccess.setModuleName(name);
	    keyForCacheAccess.setModuleFile(e.getFile());
	    doRemoveSingleModule(keyForCacheAccess);
	    n = null;


	  }
	 else {
	    // file exists
	    n = checkOverride(name, nature, n);


	    if (n instanceof EmptyModule) {
	       // ok, handle case where the file is actually from a zip file...
	       if (e instanceof EmptyModuleForZip) {
		  EmptyModuleForZip emptyModuleForZip = (EmptyModuleForZip) e;

		  if (PybaseFileSystem.isValidDll(emptyModuleForZip.getPath())) {
		     // .pyd
		     n = new CompiledModule(name,this);
		     n = decorateModule(n, nature);

		   }
		  else if (PythonPathHelper
			      .isValidSourceFile(emptyModuleForZip.getPath())) {
		     // handle python file from zip... we have to create it getting the
		     // contents from the zip file
		     try {
			IDocument doc = PybaseFileSystem.getDocFromZip(emptyModuleForZip.getFile(),
									  emptyModuleForZip.getPath());
			// NOTE: The nature (and so the grammar to be used) must be
			// defined by this modules
			// manager (and not by the initial caller)!!
			n = AbstractModule.createModuleFromDoc(name,
								  emptyModuleForZip.getFile(), doc, getNature(), -1,
								  false);
			SourceModule zipModule = (SourceModule) n;
			zipModule.zip_file_path = emptyModuleForZip.getPath();
			n = decorateModule(n, nature);
		      }
		     catch (Exception exc1) {
			PybaseMain.logE("Problem getting module",exc1);
			n = null;
		      }
		   }


		}
	       else {
		  // regular case... just go on and create it.
		  try {
		     // NOTE: The nature (and so the grammar to be used) must be defined
		     // by this modules
		     // manager (and not by the initial caller)!!
		     n = AbstractModule
			.createModule(name, e.getFile(), getNature(), -1);
		     n = decorateModule(n, nature);
		   }
		  catch (IOException exc) {
		     keyForCacheAccess.setModuleName(name);
		     keyForCacheAccess.setModuleFile(e.getFile());
		     doRemoveSingleModule(keyForCacheAccess);
		     n = null;
		   }
		}
	     }

	  }

       }
      else { // ok, it does not have a file associated, so, we treat it as a builtin (this
	 // can happen in java jars)
	 n = checkOverride(name, nature, n);
	 if (n instanceof EmptyModule) {
	    if (acceptCompiledModule) {
	       n = new CompiledModule(name,this);
	       n = decorateModule(n, nature);
	     }
	    else {
	       return null;
	     }
	  }
       }

      if (n != null) {
	 doAddSingleModule(createModulesKey(name, e.getFile()), n);
       }
      else {
	 PybaseMain.logE("The module " + name + " could not be found nor created!");
       }
    }

   if (n instanceof EmptyModule) {
      throw new RuntimeException("Should not be an empty module anymore!");
    }
   if (n instanceof SourceModule) {
      SourceModule sourceModule = (SourceModule) n;
      // now, here's a catch... it may be a bootstrap module...
      if (sourceModule.isBootstrapModule()) {
	 // if it's a bootstrap module, we must replace it for the related compiled
	 // module.
	 n = new CompiledModule(name,this);
	 n = decorateModule(n, nature);
       }
    }

   return n;
}

/**
      * Called after the creation of any module. Used as a workaround for filling tokens that are in no way
      * available in the code-completion through the regular inspection.
      *
      * The django objects class is the reason why this happens... It's structure for the creation on a model class
						   * follows no real patterns for the creation of the 'objects' attribute in the class, and thus, we have no
						   * real generic way of discovering it (actually, even by looking at the class definition this is very obscure),
						   * so, the solution found is creating the objects by decorating the module with that info.
						   */
private AbstractModule decorateModule(AbstractModule n,PybaseNature nature)
{
   if (n instanceof SourceModule && "django.db.models.base".equals(n.getName())) {
      SourceModule sourceModule = (SourceModule) n;
      SimpleNode ast = sourceModule.getAst();
      for (SimpleNode node : ((Module) ast).body) {
	 if (node instanceof ClassDef
		&& "Model".equals(NodeUtils.getRepresentationString(node))) {
	    Object[][] metaclassAttrs = new Object[][] {
	       {
	       "objects",
	       NodeUtils.makeAttribute("django.db.models.manager.Manager()") },
	       { "DoesNotExist", new Name("Exception",expr_contextType.Load,false) },
	       { "MultipleObjectsReturned", new Name("Exception",expr_contextType.Load,false) }, };

	    ClassDef classDef = (ClassDef) node;
	    stmtType[] newBody = new stmtType[classDef.body.length
	       + metaclassAttrs.length];
	    System.arraycopy(classDef.body, 0, newBody, metaclassAttrs.length,
				classDef.body.length);

	    int i = 0;
	    for (Object[] objAndType : metaclassAttrs) {
	       // Note that the line/col is important so that we correctly acknowledge it
	       // inside the "class Model" scope.
	       Name name = new Name((String) objAndType[0],expr_contextType.Store,false);
	       name.beginColumn = classDef.beginColumn + 4;
	       name.beginLine = classDef.beginLine + 1;
	       newBody[i] = new Assign(new exprType[] { name },(exprType) objAndType[1]);
	       newBody[i].beginColumn = classDef.beginColumn + 4;
	       newBody[i].beginLine = classDef.beginLine + 1;

	       i += 1;
	     }


	    classDef.body = newBody;
	    break;
	  }
       }
    }
   return n;
}


/**
      * Hook called to give clients a chance to override the module created (still experimenting, so, it's not public).
*/
private AbstractModule checkOverride(String name,PybaseNature nature,
					AbstractModule emptyModule)
{
   return emptyModule;
}


private ModulesKey createModulesKey(String name,File f)
{
   ModulesKey newEntry = new ModulesKey(name,f);
   synchronized (modulesKeysLock) {
      Map.Entry<ModulesKey, ModulesKey> oldEntry = modulesKeys.getActualEntry(newEntry);
      if (oldEntry != null) {
	 return oldEntry.getKey();
       }
      else {
	 return newEntry;
       }
    }
}


/**
      * Passes through all the compiled modules in memory and clears its tokens (so that
										    * we restore them when needed).
      */
public static void clearCache()
{
   ModulesManager.cache.clear();
}

/**
      * @param full
      * @return
      */
public String resolveModule(String full)
{
   return pythonPathHelper.resolveModule(full, false);
}

public List<String> getPythonPath()
{
   if (pythonPathHelper == null) {
      return new ArrayList<String>();
    }
   return pythonPathHelper.getPythonpath();
}

abstract public PybaseNature getNature();
abstract public Tuple<AbstractModule,ModulesManager> getModuleAndRelatedModulesManager(String nm,PybaseNature n,boolean sys,boolean init);
abstract public AbstractModule getModule(String nm,PybaseNature n,boolean sys,boolean init);
abstract public AbstractModule getRelativeModule(String nm,PybaseNature n);
abstract public void setPythonNature(PybaseNature n);
abstract public List<String> getCompletePythonPath(PybaseInterpreter ii,AbstractInterpreterManager im);


} // end of class ModulesManager


/* end of ModulesManager.java */
