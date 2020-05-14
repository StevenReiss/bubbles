/********************************************************************************/
/*										*/
/*		SystemModulesManager.java					*/
/*										*/
/*	Python Bubbles Base modules manager for system modules			*/
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

import edu.brown.cs.bubbles.pybase.PybaseConstants;
import edu.brown.cs.bubbles.pybase.PybaseFileSystem;
import edu.brown.cs.bubbles.pybase.PybaseInterpreter;
import edu.brown.cs.bubbles.pybase.PybaseMain;
import edu.brown.cs.bubbles.pybase.PybaseNature;
import edu.brown.cs.bubbles.pybase.PybaseParser;
import edu.brown.cs.bubbles.pybase.PybaseSystemNature;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.DeltaSaver;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.cache.LRUCache;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;


/**
 * @author Fabio Zadrozny
 */
public final class SystemModulesManager extends ModulesManagerWithBuild implements PybaseConstants {

/**
 * The system modules manager may have a nature if we create a SystemASTManager
 */
private transient PybaseNature pybase_nature;

/**
 * This is the place where we store the info related to this manager
 */
private PybaseInterpreter  interpreter_info;

public SystemModulesManager(PybaseInterpreter info)
{
   interpreter_info = info;
}

public void setInfo(PybaseInterpreter info)
{
   // Should only be used in tests (in general the info should be passed in the
   // constructor and never changed again).
   this.interpreter_info = info;
}

@Override public void endProcessing()
{
   save();
}


public String[] getBuiltins()
{
   return this.interpreter_info.getBuiltins();
}


@Override public void setPythonNature(PybaseNature nature)
{
   assert (nature instanceof PybaseSystemNature);
   assert (((PybaseSystemNature) nature).the_interpreter == this.interpreter_info);

   this.pybase_nature = nature;
}

@Override public PybaseNature getNature()
{
   if (pybase_nature == null) {
      AbstractInterpreterManager manager = getInterpreterManager();
      pybase_nature = new PybaseSystemNature(manager,this.interpreter_info);
    }
   return pybase_nature;
}

public AbstractInterpreterManager getInterpreterManager()
{
   PybaseInterpreterType interpreterType = this.interpreter_info.getInterpreterType();
   return PybaseNature.getInterpreterManager(interpreterType);
}

public SystemModulesManager getSystemModulesManager()
{
   return this; // itself
}

@Override public AbstractModule getModule(String name,PybaseNature nature,boolean checkSystemManager,
				   boolean dontSearchInit)
{
   return getModule(name, nature, dontSearchInit);
}


public String resolveModule(String full,boolean checkSystemManager)
{
   return super.resolveModule(full);
}


@Override public List<String> getCompletePythonPath(PybaseInterpreter interpreter,
					     AbstractInterpreterManager manager)
{
   if (interpreter == null) {
      throw new RuntimeException("The interpreter must be specified (received null)");
    }
   else {
      return interpreter.getPythonPath();
    }
}

@Override public AbstractModule getRelativeModule(String name,PybaseNature nature)
{
   return super.getModule(name, nature, true);
}


/**
 * Called after the pythonpath is changed.
 */
@Override protected void onChangePythonpath(SortedMap<ModulesKey, ModulesKey> keys)
{
   // create the builtin modules
   String[] builtins = getBuiltins();
   if (builtins != null) {
      for (int i = 0; i < builtins.length; i++) {
	 String name = builtins[i];
	 final ModulesKey k = new ModulesKey(name,null);
	 keys.put(k, k);
       }
    }
}

/**
 * This is a cache with the name of a builtin pointing to itself (so, it works basically as a set), it's used
													  * so that when we find a builtin that does not have a __file__ token we do not try to recreate it again later.
													  */
private final LRUCache<String, String> builtinsNotConsidered = new LRUCache<String, String>(500);

/**
 * @return true if there is a token that has rep as its representation.
 */
private boolean contains(AbstractToken[] tokens,String rep)
{
   for (AbstractToken token : tokens) {
      if (token.getRepresentation().equals(rep)) {
	 return true;
       }
    }
   return false;
}

/**
 * Files only get here if we were unable to parse them.
 */
private transient Map<File, Long> predefinedFilesNotParsedToTimestamp;


public AbstractModule getBuiltinModule(String name,PybaseNature nature,boolean dontSearchInit)
{
   AbstractModule n = null;

   // check for supported builtins these don't have files associated.
   // they are the first to be passed because the user can force a module to be builtin,
   // because there
   // is some information that is only useful when you have builtins, such as os and
   // wxPython (those can
   // be source modules, but they have so much runtime info that it is almost impossible
   // to get useful information
   // from statically analyzing them).
   String[] builtins = getBuiltins();
   if (builtins == null || this.interpreter_info == null) {
      // still on startup
      return null;
    }

   // for temporary access (so that we don't generate many instances of it)
   ModulesKey keyForCacheAccess = new ModulesKey(null,null);

   // A different choice for users that want more complete information on the libraries
   // they're dealing
   // with is using predefined modules. Those will
   File predefinedModule = this.interpreter_info.getPredefinedModule(name);
   if (predefinedModule != null && predefinedModule.exists()) {
      keyForCacheAccess.setModuleName(name);
      keyForCacheAccess.setModuleFile(predefinedModule);
      n = cache.getObj(keyForCacheAccess, this);
      if ((n instanceof PredefinedSourceModule)) {
	 PredefinedSourceModule predefinedSourceModule = (PredefinedSourceModule) n;
	 if (predefinedSourceModule.isSynched()) {
	    return n;
	  }
	 // otherwise (not PredefinedSourceModule or not synched), just keep going to
	 // create
	 // it as a predefined source module
       }

      boolean tryToParse = true;
      Long lastModified = null;
      if (predefinedFilesNotParsedToTimestamp == null) {
	 predefinedFilesNotParsedToTimestamp = new HashMap<File, Long>();
       }
      else {
	 Long lastTimeChanged = predefinedFilesNotParsedToTimestamp.get(predefinedModule);
	 if (lastTimeChanged != null) {
	    lastModified = predefinedModule.lastModified();
	    if (lastTimeChanged == lastModified) {
	       tryToParse = false;
	     }
	    else {
	       predefinedFilesNotParsedToTimestamp.remove(predefinedModule);
	     }
	  }
       }

      if (tryToParse) {
	 IDocument doc;
	 try {
	    doc = PybaseFileSystem.getDocFromFile(predefinedModule);
	    PybaseNature pn = getNature();
	    PybaseParser pp = new PybaseParser(pn.getProject(),doc,predefinedModule);
	    ISemanticData sd = pp.parseDocument(false);
	    if (sd.getMessages() != null && sd.getMessages().size() > 0) {
	       if (lastModified == null) {
		  lastModified = predefinedModule.lastModified();
		}
	       predefinedFilesNotParsedToTimestamp.put(predefinedModule, lastModified);
	       PybaseMain.logD("Unable to parse: " + predefinedModule + " " + sd.getMessages().get(0));
	     }
	    else if (sd.getRootNode() != null) {
	       n = new PredefinedSourceModule(name,predefinedModule,sd.getRootNode(),sd.getMessages());
	       doAddSingleModule(keyForCacheAccess, n);
	       return n;
	     }
	    // keep on going
	  }
	 catch (Throwable e) {
	    PybaseMain.logE("Problem getting builtin",e);
	  }
       }
    }

   boolean foundStartingWithBuiltin = false;
   StringBuilder buffer = null;

   for (int i = 0; i < builtins.length; i++) {
      String forcedBuiltin = builtins[i];
      if (name.startsWith(forcedBuiltin)) {
	 if (name.length() > forcedBuiltin.length()
		&& name.charAt(forcedBuiltin.length()) == '.') {
	    foundStartingWithBuiltin = true;

	    keyForCacheAccess.setModuleName(name);
	    n = cache.getObj(keyForCacheAccess, this);

	    if (n == null && dontSearchInit == false) {
	       if (buffer == null) {
		  buffer = new StringBuilder();
		}
	       else {
		  buffer.setLength(0);
		}
	       keyForCacheAccess.setModuleName(buffer.append(name).append(".__init__").toString());
	       n = cache.getObj(keyForCacheAccess, this);
	     }

	    if (n instanceof EmptyModule || n instanceof SourceModule) {
	       // it is actually found as a source module, so, we have to 'coerce' it to a
	       // compiled module
	       n = new CompiledModule(name,this);
	       doAddSingleModule(new ModulesKey(n.getName(),null), n);
	       return n;
	     }
	  }

	 if (name.equals(forcedBuiltin)) {
	    keyForCacheAccess.setModuleName(name);
	    n = cache.getObj(keyForCacheAccess, this);

	    if (n == null || n instanceof EmptyModule || n instanceof SourceModule) {
	       // still not created or not defined as compiled module (as it should be)
	       n = new CompiledModule(name,this);
	       doAddSingleModule(new ModulesKey(n.getName(),null), n);
	       return n;
	     }
	  }
	 if (n instanceof CompiledModule) {
	    return n;
	  }
       }
    }
   if (foundStartingWithBuiltin) {
      if (builtinsNotConsidered.getObj(name) != null) {
	 return null;
       }

      // ok, just add it if it is some module that actually exists
      n = new CompiledModule(name,this);
      AbstractToken[] globalTokens = n.getGlobalTokens();
      // if it does not contain the __file__, this means that it's not actually a module
      // (but may be a token from a compiled module, so, clients wanting it must get the
      // module
      // first and only then go on to this token).
      // done: a cache with those tokens should be kept, so that we don't actually have to
      // create
      // the module to see its return values (because that's slow)
      if (globalTokens.length > 0 && contains(globalTokens, "__file__")) {
	 doAddSingleModule(new ModulesKey(name,null), n);
	 return n;
       }
      else {
	 builtinsNotConsidered.add(name, name);
	 return null;
       }
    }

   if (nature != null) {
      n = findLibraryModule(name,nature);
    }

   return n;
}


/**
 * In the system modules manager, we also have to check for the builtins
 */
@Override public AbstractModule getModule(String name,PybaseNature nature,boolean dontSearchInit)
{
   AbstractModule n = getBuiltinModule(name, nature, dontSearchInit);
   if (n != null) {
      return n;
    }

   return super.getModule(name, nature, dontSearchInit);
}

public AbstractModule getModuleWithoutBuiltins(String name,PybaseNature nature,
						  boolean dontSearchInit)
{
   return super.getModule(name, nature, dontSearchInit);
}


@Override public Tuple<AbstractModule, ModulesManager> getModuleAndRelatedModulesManager(String name,
										  PybaseNature nature,boolean checkSystemManager,boolean dontSearchInit)
{
   AbstractModule module = this.getModule(name, nature, checkSystemManager, dontSearchInit);
   if (module != null) {
      return new Tuple<AbstractModule, ModulesManager>(module,this);
    }
   return null;
}


public void load() throws IOException
{
   final File workspacemetadatafile = getIoDirectory();
   ModulesManager.loadFromFile(this, workspacemetadatafile);

   try {
      this.deltaSaver = new DeltaSaver<ModulesKey>(this.getIoDirectory(),
						      "v1_sys_astdelta",readFromFileMethod,toFileMethod);
    }
   catch (Exception e) {
      PybaseMain.logE("Problem loading system modules manager",e);
    }
   deltaSaver.processDeltas(this); // process the current deltas (clears current deltas
   // automatically and saves it when the processing is concluded)
}


public void save()
{
   final File workspacemetadatafile = getIoDirectory();
   if (deltaSaver != null) {
      deltaSaver.clearAll(); // When save is called, the deltas don't need to be used
      // anymore.
    }
   this.saveToFile(workspacemetadatafile);

   this.deltaSaver = new DeltaSaver<ModulesKey>(this.getIoDirectory(),"v1_sys_astdelta",
						   readFromFileMethod,toFileMethod);

}


public File getIoDirectory()
{
   File workfile = PybaseNature.getWorkspaceMetadataFile(interpreter_info.getExeAsFileSystemValidPath());
   return workfile;
}



/**
 * @param keysFound
 */
public void updateKeysAndSave(ModulesKeyTreeMap<ModulesKey, ModulesKey> keysFound)
{
   synchronized (modulesKeysLock) {
      modulesKeys.clear();
      modulesKeys.putAll(keysFound);
    }
   this.save();
}


/********************************************************************************/
/*										*/
/*	Library lookup								*/
/*										*/
/********************************************************************************/

private AbstractModule findLibraryModule(String name,PybaseNature nature)
{
   AbstractModule n = null;
   ModulesKey keyForCacheAccess = new ModulesKey(null,null);

   for (String p : interpreter_info.lib_set) {
      File f = new File(p);
      File f1 = new File(f,name + ".py");
      if (f1.exists()) {
	 keyForCacheAccess.setModuleName(name);
	 keyForCacheAccess.setModuleFile(f1);
	 n = cache.getObj(keyForCacheAccess, this);
	 if ((n instanceof PredefinedSourceModule)) {
	    PredefinedSourceModule predefinedSourceModule = (PredefinedSourceModule) n;
	    if (predefinedSourceModule.isSynched()) {
	       return n;
	     }
	    // otherwise (not PredefinedSourceModule or not synched), just keep going to
	    // create
	    // it as a predefined source module
	  }

	 boolean tryToParse = true;
	 Long lastModified = null;
	 if (predefinedFilesNotParsedToTimestamp == null) {
	    predefinedFilesNotParsedToTimestamp = new HashMap<File, Long>();
	  }
	 else {
	    Long lastTimeChanged = predefinedFilesNotParsedToTimestamp.get(f1);
	    if (lastTimeChanged != null) {
	       lastModified = f1.lastModified();
	       if (lastTimeChanged == lastModified) {
		  tryToParse = false;
		}
	       else {
		  predefinedFilesNotParsedToTimestamp.remove(f1);
		}
	     }
	  }

	 if (tryToParse) {
	    IDocument doc;
	    try {
	       doc = PybaseFileSystem.getDocFromFile(f1);
	       PybaseParser pp = new PybaseParser(nature.getProject(),doc,f1);
	       ISemanticData sd = pp.parseDocument(false);
	       if (sd.getMessages() != null && sd.getMessages().size() > 0) {
		  if (lastModified == null) {
		     lastModified = f1.lastModified();
		   }
		  predefinedFilesNotParsedToTimestamp.put(f1, lastModified);
		  PybaseMain.logD("Unable to parse: " + f1 + " " + sd.getMessages().get(0));

		}
	       else if (sd.getRootNode() != null) {
		  n = new PredefinedSourceModule(name,f1,sd.getRootNode(),sd.getMessages());
		  doAddSingleModule(keyForCacheAccess, n);
		  return n;
		}
	       // keep on going
	     }
	    catch (Throwable e) {
	       PybaseMain.logE("Problem getting library module",e);
	     }
	  }
       }
    }

   return null;
}



}	// end of class SystemModulesManager



/* end of SystemModulesManager.java */
