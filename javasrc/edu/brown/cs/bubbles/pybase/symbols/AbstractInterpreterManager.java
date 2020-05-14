/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 17, 2005
 *
 * @author Fabio Zadrozny
 */

package edu.brown.cs.bubbles.pybase.symbols;

import edu.brown.cs.bubbles.pybase.PybaseConstants;
import edu.brown.cs.bubbles.pybase.PybaseInterpreter;
import edu.brown.cs.bubbles.pybase.PybaseMain;
import edu.brown.cs.bubbles.pybase.PybaseNature;
import edu.brown.cs.bubbles.pybase.PybasePathNature;
import edu.brown.cs.bubbles.pybase.PybasePreferences;

import org.python.pydev.core.Tuple;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


/**
 * Does not write directly in INTERPRETER_PATH, just loads from it and works with it.
 *
 * @author Fabio Zadrozny
 */

public abstract class AbstractInterpreterManager implements PybaseConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/


private final PybasePreferences 	interpreter_prefs;
private volatile PybaseInterpreter[]	interpreter_infos_from_persisted_string;
private Object				cache_lock;
private String				persisted_cache;
private PybaseInterpreter[]		persisted_cache_ret;
private String				persisted_string;

private final Map<String, AbstractToken[]> builtin_completions;
private final Map<String, AbstractModule>  builtin_mod;
private final Map<String, PybaseInterpreter> exe_to_info;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public AbstractInterpreterManager(PybasePreferences prefs)
{
   builtin_completions = new HashMap<String,AbstractToken[]>();
   builtin_mod = new HashMap<String,AbstractModule>();
   exe_to_info = new HashMap<String,PybaseInterpreter>();

   interpreter_infos_from_persisted_string = null;
   cache_lock = new Object();
   persisted_cache = null;
   persisted_cache_ret = null;
   persisted_string = null;

   interpreter_prefs = prefs;
   // Just called to force the information to be recreated!
   getInterpreterInfos();
}



/********************************************************************************/
/*										*/
/*	Completion methods							*/
/*										*/
/********************************************************************************/

public void clearBuiltinCompletions(String projectInterpreterName)
{
   builtin_completions.remove(projectInterpreterName);
}


public AbstractToken[] getBuiltinCompletions(String projectInterpreterName)
{
   // Cache with the internal name.
   projectInterpreterName = getInternalName(projectInterpreterName);
   if (projectInterpreterName == null) {
      return null;
    }

   AbstractToken[] toks = builtin_completions.get(projectInterpreterName);

   if (toks == null || toks.length == 0) {
      AbstractModule builtMod = getBuiltinMod(projectInterpreterName);
      if (builtMod != null) {
	 toks = builtMod.getGlobalTokens();
	 builtin_completions.put(projectInterpreterName, toks);
       }
    }
   return builtin_completions.get(projectInterpreterName);
}



public AbstractModule getBuiltinMod(String projectInterpreterName)
{
   // Cache with the internal name.
   projectInterpreterName = getInternalName(projectInterpreterName);
   if (projectInterpreterName == null) {
      return null;
    }
   AbstractModule mod = builtin_mod.get(projectInterpreterName);
   if (mod != null) return mod;

   PybaseInterpreter interpreterInfo = getInterpreterInfo(projectInterpreterName);
   SystemModulesManager modulesManager = interpreterInfo.getModulesManager();

   mod = modulesManager.getBuiltinModule("__builtin__", null, false);
   if (mod == null) {
      // Python 3.0 has builtins and not __builtin__
      mod = modulesManager.getBuiltinModule("builtins", null, false);
    }
   if (mod != null) {
      builtin_mod.put(projectInterpreterName, mod);
    }

   return builtin_mod.get(projectInterpreterName);
}



public void clearBuiltinMod(String projectInterpreterName)
{
   builtin_mod.remove(projectInterpreterName);
}



public void clearCaches()
{
   builtin_mod.clear();
   builtin_completions.clear();
   clearInterpretersFromPersistedString();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public boolean isConfigured()
{
   String defaultInterpreter = getDefaultInterpreterInfo(false).getExecutableOrJar();
   if (defaultInterpreter == null) {
      return false;
    }
   if (defaultInterpreter.length() == 0) {
      return false;
    }
   return true;
}




public PybaseInterpreter getDefaultInterpreterInfo(boolean autoConfigureIfNotConfigured)
{
   PybaseInterpreter[] interpreters = getInterpreterInfos();
   String errorMsg = null;
   if (interpreters.length > 0) {
      PybaseInterpreter defaultInfo = interpreters[0];
      String interpreter = defaultInfo.getExecutableOrJar();
      if (interpreter == null) {
	 errorMsg = "The configured interpreter for " + getInterpreterUIName()
	    + " is null, some error happened getting it.";
       }
      return defaultInfo;
    }
   else {
      errorMsg = getInterpreterUIName() + " not configured.";
    }

   throw new RuntimeException(errorMsg);
}




/********************************************************************************/
/*										*/
/*	Abstract methods for actual interpreter managers			*/
/*										*/
/********************************************************************************/

/**
 * @return the preference name where the options for this interpreter manager should be stored
 */
protected abstract String getPreferenceName();


/**
 * @return
 */
protected abstract String getPreferencesPageId();


/**
 * @return a message to show to the user when there is no configured interpreter
 */
public abstract String getInterpreterUIName();



/**
 * Given an executable, should create the interpreter info that corresponds to it
 *
 * @param executable the executable that should be used to create the info
 *
 * @return the interpreter info for the executable
 */
protected abstract Tuple<PybaseInterpreter,String> internalCreateInterpreterInfo(String executable,
										    boolean askUser);


public abstract PybaseInterpreterType getInterpreterType();



/********************************************************************************/
/*										*/
/*	Interpreter information methods 					*/
/*										*/
/********************************************************************************/

public PybaseInterpreter[] getInterpreterInfos()
{
   return internalRecreateCacheGetInterpreterInfos();

}

private PybaseInterpreter[] internalRecreateCacheGetInterpreterInfos()
{
   PybaseInterpreter[] interpreters = interpreter_infos_from_persisted_string;
   if (interpreters == null) {
      synchronized (cache_lock) {
	 if (interpreter_infos_from_persisted_string != null) {
	    // Some other thread restored it while we're locked.
	    interpreters = interpreter_infos_from_persisted_string;

	  }
	 else {
	    interpreters = getInterpretersFromPersistedString(getPersistedString());
	    try {
	       exe_to_info.clear();
	       for (PybaseInterpreter info : interpreters) {
		  info.startBuilding();
		  exe_to_info.put(info.getExecutableOrJar(), info);
		}

	     }
	    finally {
	       interpreter_infos_from_persisted_string = interpreters;
	     }
	  }
       }
    }
   return interpreters;
}




private void clearInterpretersFromPersistedString()
{
   synchronized (cache_lock) {
      if (interpreter_infos_from_persisted_string != null) {
	 for (PybaseInterpreter info : interpreter_infos_from_persisted_string) {
	    try {
	       info.stopBuilding();
	     }
	    catch (Throwable e) {
	       PybaseMain.logE("Problem clearing interpreters", e);
	     }
	  }
	 exe_to_info.clear();
	 interpreter_infos_from_persisted_string = null;
       }
    }

}



public PybaseInterpreter createInterpreterInfo(String executable,boolean askUser)
{

   // ok, we have to get the info from the executable (and let's cache results for future use)...
   Tuple<PybaseInterpreter, String> tup = null;
   PybaseInterpreter info;
   try {
      tup = internalCreateInterpreterInfo(executable, askUser);
      if (tup == null) {
	 // Canceled (in the dialog that asks the user to choose the valid paths)
	 return null;
       }
      info = tup.o1;

    }
   catch (RuntimeException e) {
      PybaseMain.logE("Problem clearing interpreter info", e);
      throw e;
    }
   catch (Exception e) {
      PybaseMain.logE("Problem clearing interpreter info 1", e);
      throw new RuntimeException(e);
    }
   if (info.executable_or_jar == null || info.executable_or_jar.trim().length() == 0) {
      // it is null or empty
      String reasonCreation = "The interpreter (or jar): '" + executable
	 + "' is not valid - info.executable found: " + info.executable_or_jar
	 + "\n";
      if (tup != null) {
	 reasonCreation += "The standard output gotten from the executed shell was: >>"
	    + tup.o2 + "<<";
       }
      final String reason = reasonCreation;
      throw new RuntimeException(reason);
    }

   return info;

}



protected static PybaseInterpreter createInfoFromOutput(Tuple<String, String> outTup,
							   boolean askUser)
{
   if (outTup.o1 == null || outTup.o1.trim().length() == 0) {
      throw new RuntimeException(
	 "No output was in the standard output when trying to create the interpreter info.\n"
	    + "The error output contains:>>" + outTup.o2 + "<<");
    }
   PybaseInterpreter info = PybaseInterpreter.fromString(outTup.o1, askUser);
   return info;
}



public PybaseInterpreter getInterpreterInfo(String nameOrExecutableOrJar)
{
   synchronized (cache_lock) {
      if (interpreter_infos_from_persisted_string == null) {
	 internalRecreateCacheGetInterpreterInfos(); // recreate cache!
       }
      for (PybaseInterpreter info : exe_to_info.values()) {
	 if (info != null) {
	    if (info.matchNameBackwardCompatible(nameOrExecutableOrJar)) {
	       return info;
	     }
	  }
       }
    }

   throw new RuntimeException(StringUtils.format("Interpreter: %s not found",
						    nameOrExecutableOrJar));
}


public PybaseInterpreter[] getInterpretersFromPersistedString(String persisted)
{
   synchronized (cache_lock) {
      if (persisted == null || persisted.trim().length() == 0) {
	 return new PybaseInterpreter[0];
       }

      if (persisted_cache == null || persisted_cache.equals(persisted) == false) {
	 List<PybaseInterpreter> ret = new ArrayList<PybaseInterpreter>();

	 try {
	    List<PybaseInterpreter> list = new ArrayList<PybaseInterpreter>();
	    String[] strings = persisted.split("&&&&&");

	    // first, get it...
	    for (String string : strings) {
	       try {
		  list.add(PybaseInterpreter.fromString(string, false));
		}
	       catch (Exception e) {
		  // ok, its format might have changed
		  String errMsg = "Interpreter storage changed.\r\n"
		     + "Please restore it (window > preferences > Pydev > Interpreter)";
		  PybaseMain.logE(errMsg, e);

		  return new PybaseInterpreter[0];
		}
	     }

	    // then, put it in the list to be returned
	    for (PybaseInterpreter info : list) {
	       if (info != null && info.executable_or_jar != null) {
		  ret.add(info);
		}
	     }

	    // and at last, restore the system info
	    for (final PybaseInterpreter info : list) {
	       try {
		  info.getModulesManager().load();
		}
	       catch (Exception e) {
		  PybaseMain.logD("Restoring info for: " + info.getExecutableOrJar() + ": " + e);
		  PybaseMain.logD("Finished restoring information for: "
				     + info.executable_or_jar + " at: "
				     + info.getModulesManager().getIoDirectory());
		}
	     }

	  }
	 catch (Exception e) {
	    PybaseMain.logE("Problem getting interpreter info", e);
	    // ok, some error happened (maybe it's not configured)
	    return new PybaseInterpreter[0];
	  }

	 persisted_cache = persisted;
	 persisted_cache_ret = ret.toArray(new PybaseInterpreter[0]);
       }
    }
   return persisted_cache_ret;
}



/**
  * @param executables executables that should be persisted
  * @return string to persist with the passed executables.
  */
public static String getStringToPersist(PybaseInterpreter[] executables)
{
   StringBuilder buf = new StringBuilder();
   for (PybaseInterpreter info : executables) {
      if (info != null) {
	 buf.append(info.toString());
	 buf.append("&&&&&");
       }
    }

   return buf.toString();
}



protected static File getInterpreterInfoPy()
{
   File script = PybaseNature.getScriptWithinPySrc("interpreterInfo.py");
   if (!script.exists()) {
      throw new RuntimeException("The file specified does not exist: " + script);
    }
   return script;
}

public String getPersistedString()
{
   if (persisted_string == null) {
      persisted_string = interpreter_prefs.getProperty(getPreferenceName(), "");
    }
   return persisted_string;
}



public void setInfos(PybaseInterpreter[] infos,Set<String> interpreterNamesToRestore)
{
   String s = AbstractInterpreterManager.getStringToPersist(infos);
   interpreter_prefs.setProperty(getPreferenceName(), s);
   try {
      interpreter_prefs.flush();
    }
   catch (Exception e) {
      String message = e.getMessage();
      if (message == null || message.indexOf("File name not specified") == -1) {
	 PybaseMain.logE("Problem setting interpreter", e);
       }
    }

   PybaseInterpreter[] interpreterInfos;

   try {
      synchronized (cache_lock) {
	 clearInterpretersFromPersistedString();
	 persisted_string = s;
	 // After setting the preference, get the actual infos (will be recreated).
	 interpreterInfos = internalRecreateCacheGetInterpreterInfos();

	 restorePythonpathForInterpreters(interpreterNamesToRestore);
	 // When we call performOk, the editor is going to store its values, but after
	 // actually restoring the modules, we need to serialize the SystemModulesManager
	 // This method persists all the modules managers that are within this interpreter manager
	 // (so, all the SystemModulesManagers will be saved -- and can be later restored).

	 for (PybaseInterpreter info : exe_to_info.values()) {
	    try {
	       SystemModulesManager modulesManager = info.getModulesManager();
	       Object pythonPathHelper = modulesManager.getPythonPathHelper();
	       if (!(pythonPathHelper instanceof PythonPathHelper)) {
		  continue;
		}
	       PythonPathHelper pathHelper = (PythonPathHelper) pythonPathHelper;
	       List<String> pythonpath = pathHelper.getPythonpath();
	       if (pythonpath == null || pythonpath.size() == 0) {
		  continue;
		}
	       modulesManager.save();
	     }
	    catch (Throwable e) {
	       PybaseMain.logE("Problem setting up interpreter", e);
	     }
	  }
       }

      // Now, last step is updating the natures (the call must NOT be locked in this case).
      restorePythonpathForNatures();

      // We also need to restart our code-completion shell after doing that, as we may have new environment variables!
      // And in jython, changing the classpath also needs to restore it.
      for (PybaseInterpreter interpreter : interpreterInfos) {
	 AbstractShell.stopServerShell(interpreter, AbstractShell.COMPLETION_SHELL);
       }
    }
   finally {
      AbstractShell.restartAllShells();
    }
}



private void restorePythonpathForInterpreters(Set<String> interpretersNamesToRestore)
{
   for (String interpreter : exe_to_info.keySet()) {
      if (interpretersNamesToRestore != null) {
	 if (!interpretersNamesToRestore.contains(interpreter)) {
	    continue; // only restore the ones specified
	  }
       }
      PybaseInterpreter info;
      info = getInterpreterInfo(interpreter);
      info.restorePythonpath(); // that's it, info.modulesManager contains the  SystemModulesManager
    }
}


private void restorePythonpathForNatures()
{
   PybaseInterpreter defaultInterpreterInfo;
   defaultInterpreterInfo = getDefaultInterpreterInfo(false);

   StringBuilder buf = new StringBuilder();
   // Also notify that all the natures had the pythonpath changed (it's the system  pythonpath, but still,
   // clients need to know about it)
   List<PybaseNature> pythonNatures;
   try {
      pythonNatures = PybaseNature.getAllPythonNatures();
    }
   catch (IllegalStateException e1) {
      // java.lang.IllegalStateException: Workspace is closed.
      return;
    }
   for (PybaseNature nature : pythonNatures) {
      try {
	 // If they have the same type of the interpreter manager, notify.
	 if (getInterpreterType() == nature.getInterpreterType()) {
	    PybasePathNature pythonPathNature = nature.getPythonPathNature();

	    String complete = pythonPathNature.getProjectSourcePath();

	    PybaseNature n = nature;
	    String projectInterpreterName = n.getProjectInterpreterName();
	    PybaseInterpreter info;
	    if (PybaseNature.DEFAULT_INTERPRETER.equals(projectInterpreterName)) {
	       // if it's the default, let's translate it to the outside world
	       info = defaultInterpreterInfo;
	     }
	    else {
	       synchronized (cache_lock) {
		  info = exe_to_info.get(projectInterpreterName);
		}
	     }

	    boolean makeCompleteRebuild = false;
	    if (info != null) {
	       Properties stringSubstitutionVariables = info
		  .getStringSubstitutionVariables();
	       if (stringSubstitutionVariables != null) {
		  Enumeration<Object> keys = stringSubstitutionVariables.keys();
		  while (keys.hasMoreElements()) {
		     Object key = keys.nextElement();
		     buf.setLength(0);
		     buf.append("${");
		     buf.append(key.toString());
		     buf.append("}");

		     if (complete.indexOf(buf.toString()) != -1) {
			makeCompleteRebuild = true;
			break;
		      }
		   }
		}
	     }

	    if (!makeCompleteRebuild) {
	       // just notify that it changed
	       nature.clearCaches(true);
	     }
	    else {
	       // Rebuild the whole info.
	       nature.rebuildPath();
	     }
	  }
       }
      catch (Throwable e) {
	 PybaseMain.logE("Problem restoring nature", e);
       }
    }
}





private String getInternalName(String projectInterpreterName)
{
   if (PybaseNature.DEFAULT_INTERPRETER.equals(projectInterpreterName)) {
      // if it's the default, let's translate it to the outside world
      return getDefaultInterpreterInfo(true).getExecutableOrJar();
    }
   return projectInterpreterName;
}



}	// end of class AbstractInterpreterManager




/* end of AbstractInterpreterManager.java */
