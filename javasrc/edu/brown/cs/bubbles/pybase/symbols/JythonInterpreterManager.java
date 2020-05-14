/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 08/08/2005
 *
 * @author Fabio Zadrozny
 */

package edu.brown.cs.bubbles.pybase.symbols;

import edu.brown.cs.bubbles.pybase.PybaseFileSystem;
import edu.brown.cs.bubbles.pybase.PybaseInterpreter;
import edu.brown.cs.bubbles.pybase.PybasePreferences;

import org.python.pydev.core.Tuple;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class JythonInterpreterManager extends AbstractInterpreterManager {

private final String JYTHON_INTERPRETER_PATH = "JYTHON_INTERPRETER_PATH";

public JythonInterpreterManager(PybasePreferences preferences)
{
   super(preferences);
}

@Override protected String getPreferenceName()
{
   return JYTHON_INTERPRETER_PATH;
}

@Override public String getInterpreterUIName()
{
   return "Jython";
}

@Override public Tuple<PybaseInterpreter, String> internalCreateInterpreterInfo(
	 String executable,boolean askUser)
{
   return doCreateInterpreterInfo(executable, askUser);
}

@Override protected String getPreferencesPageId()
{
   return "edu.brown.cs.bubbles.pybase.interpreterPreferencesPageJython";
}

/**
 * This is the method that creates the interpreter info for jython. It gets the info on the jython side and on the java side
 *
 * @param executable the jar that should be used to get the info
 * @return the interpreter info, with the default libraries and jars
 */
public static Tuple<PybaseInterpreter, String> doCreateInterpreterInfo(String executable,
	 boolean askUser)
{
   boolean isJythonExecutable = PybaseInterpreter.isJythonExecutable(executable);

   if (!isJythonExecutable) {
      throw new RuntimeException(
	       "In order to get the info for the jython interpreter, a jar is needed (e.g.: jython.jar)");
   }
   File script = getInterpreterInfoPy();

   // gets the info for the python side
   Tuple<String, String> outTup = new SimpleJythonRunner().runAndGetOutputWithJar(
	    PybaseFileSystem.getFileAbsolutePath(script), executable, null, null, null);

   String output = outTup.o1;

   PybaseInterpreter info = createInfoFromOutput(outTup, askUser);
   if (info == null) {
      // cancelled
      return null;
   }
   // the executable is the jar itself
   info.executable_or_jar = executable;

   // we have to find the jars before we restore the compiled libs
   List<File> jars = findDefaultJavaJars();
   for (File jar : jars) {
      info.lib_set.add(PybaseFileSystem.getFileAbsolutePath(jar));
   }

   // java, java.lang, etc should be found now
   info.restoreCompiledLibs();


   return new Tuple<PybaseInterpreter, String>(info,output);
}

@Override public PybaseInterpreterType getInterpreterType()
{
   return PybaseInterpreterType.JYTHON;
}

public String getManagerRelatedName()
{
   return "jython";
}


static List<File> findDefaultJavaJars()
{
   // TODO: fix this
   return new ArrayList<File>();
}


}
