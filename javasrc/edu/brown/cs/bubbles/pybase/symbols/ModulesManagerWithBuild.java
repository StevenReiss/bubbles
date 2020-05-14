/********************************************************************************/
/*										*/
/*		ModulesManagerWithBuild.java					*/
/*										*/
/*	Python Bubbles Base modules manager with auto compile			*/
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

package edu.brown.cs.bubbles.pybase.symbols;

import edu.brown.cs.bubbles.pybase.PybaseFileSystem;
import edu.brown.cs.bubbles.pybase.PybaseNature;
import edu.brown.cs.bubbles.pybase.PybaseProject;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.DeltaSaver;
import org.python.pydev.core.IDeltaProcessor;
import org.python.pydev.core.callbacks.ICallback;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public abstract class ModulesManagerWithBuild extends ModulesManager implements
	 IDeltaProcessor<ModulesKey> {

/**
 * Determines whether we are testing it.
 */
public static boolean			  IN_TESTS	   = false;

/**
 * Used to process deltas (in case we have the process killed for some reason)
 */
protected volatile DeltaSaver<ModulesKey>      deltaSaver;

protected static ICallback<ModulesKey, String> readFromFileMethod = new ICallback<ModulesKey, String>()
{

   @Override public ModulesKey call(
	    String arg)
   {
      List<String> split = StringUtils.split(arg, '|');
      if (split.size() == 1) {
	 return new ModulesKey(
		  split.get(0),
		  null);
      }
      if (split.size() == 2) {
	 return new ModulesKey(
		  split.get(0),
		  new File(
			   split.get(1)));
      }

      return null;
   }
};

protected static ICallback<String, ModulesKey> toFileMethod	  = new ICallback<String, ModulesKey>()
{

   @Override public String call(
	    ModulesKey arg)
   {
      StringBuilder buf = new StringBuilder();
      buf.append(arg.getModuleName());
      if (arg.getModuleFile() != null) {
	 buf.append("|");
	 buf.append(arg.getModuleFile().toString());
      }
      return buf
      .toString();
   }
};


@Override public void processUpdate(ModulesKey data)
{
   // updates are ignored because we always start with 'empty modules' (so, we don't
// actually generate them -- updates are treated as inserts).
   throw new RuntimeException("Not impl");
}

@Override public void processDelete(ModulesKey key)
{
   doRemoveSingleModule(key);
}

@Override public void processInsert(ModulesKey key)
{
   addModule(key);
}


@Override public void doRemoveSingleModule(ModulesKey key)
{
   super.doRemoveSingleModule(key);
   if (deltaSaver != null && !IN_TESTS) { // we don't want deltas in tests
      // overridden to add delta
      deltaSaver.addDeleteCommand(key);
      checkDeltaSize();
   }
}


@Override public void doAddSingleModule(ModulesKey key,AbstractModule n)
{
   super.doAddSingleModule(key, n);
   if ((deltaSaver != null && !IN_TESTS) && !(key instanceof ModulesKeyForZip)) {
      // we don't want deltas in tests nor in zips modules
      // overridden to add delta
      deltaSaver.addInsertCommand(key);
      checkDeltaSize();
   }
}

/**
 * If the delta size is big enough, save the current state and discard the deltas.
 */
private void checkDeltaSize()
{
   if (deltaSaver != null && deltaSaver.availableDeltas() > MAXIMUM_NUMBER_OF_DELTAS) {
      endProcessing();
      deltaSaver.clearAll();
   }
}

// end delta processing


public void removeModule(File file,PybaseProject project)
{
   if (file == null) {
      return;
   }

   if (file.isDirectory()) {
      removeModulesBelow(file, project);

   }
   else {
      if (file.getName().startsWith("__init__.")) {
	 removeModulesBelow(file.getParentFile(), project);
      }
      else {
	 removeModulesWithFile(file);
      }
   }
}


/**
 * @param file
 */
private void removeModulesWithFile(File file)
{
   if (file == null) {
      return;
   }

   List<ModulesKey> toRem = new ArrayList<ModulesKey>();
   synchronized (modulesKeysLock) {

      for (Iterator<ModulesKey> iter = modulesKeys.keySet().iterator(); iter.hasNext();) {
	 ModulesKey key = iter.next();
	 if (key.getModuleFile() != null && key.getModuleFile().equals(file)) {
	    toRem.add(key);
	 }
      }

      removeThem(toRem);
   }
}

/**
 * removes all the modules that have the module starting with the name of the module from
 * the specified file.
 */
private void removeModulesBelow(File file,PybaseProject project)
{
   if (file == null) {
      return;
   }

   String absolutePath = PybaseFileSystem.getFileAbsolutePath(file);
   List<ModulesKey> toRem = new ArrayList<ModulesKey>();

   synchronized (modulesKeysLock) {

      for (ModulesKey key : modulesKeys.keySet()) {
	 if (key.getModuleFile() != null
		  && PybaseFileSystem.getFileAbsolutePath(key.getModuleFile()).startsWith(
			   absolutePath)) {
	    toRem.add(key);
	 }
      }

      removeThem(toRem);
   }
}


// ------------------------ building

public void rebuildModule(File f,IDocument doc,final PybaseProject project,
	 PybaseNature nature)
{
   final String m = pythonPathHelper.resolveModule(PybaseFileSystem
	    .getFileAbsolutePath(f));
   if (m != null) {
      addModule(new ModulesKey(m,f));


   }
   else if (f != null) { // ok, remove the module that has a key with this file, as it can
// no longer be resolved
      synchronized (modulesKeysLock) {
	 Set<ModulesKey> toRemove = new HashSet<ModulesKey>();
	 for (Iterator<ModulesKey> iter = modulesKeys.keySet().iterator(); iter.hasNext();) {
	    ModulesKey key = iter.next();
	    if (key.getModuleFile() != null && key.getModuleFile().equals(f)) {
	       toRemove.add(key);
	    }
	 }
	 removeThem(toRemove);
      }
   }
}


}
