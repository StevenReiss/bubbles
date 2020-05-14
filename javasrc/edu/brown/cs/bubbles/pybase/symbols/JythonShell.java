/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 13/08/2005
 */

package edu.brown.cs.bubbles.pybase.symbols;

import edu.brown.cs.bubbles.pybase.PybaseConstants;
import edu.brown.cs.bubbles.pybase.PybaseException;
import edu.brown.cs.bubbles.pybase.PybaseFileSystem;
import edu.brown.cs.bubbles.pybase.PybaseInterpreter;
import edu.brown.cs.bubbles.pybase.PybaseMain;
import edu.brown.cs.bubbles.pybase.PybaseNature;

import java.io.IOException;


public class JythonShell extends AbstractShell implements PybaseConstants {

public JythonShell() throws IOException, PybaseException
{
   super(PybaseNature.getScriptWithinPySrc("pycompletionserver.py"));
}


/**
 * Will create the jython shell and return a string to be shown to the user with the jython shell command line.
 */
@Override protected synchronized String createServerProcess(PybaseInterpreter jythonJar,
	 int pWrite,int pRead) throws IOException
{
   String script = PybaseFileSystem.getFileAbsolutePath(serverFile);
   String[] executableStr = SimpleJythonRunner.makeExecutableCommandStr(
	    jythonJar.getExecutableOrJar(), script, "", String.valueOf(pWrite),
	    String.valueOf(pRead));

   AbstractInterpreterManager manager = PybaseNature
	    .getInterpreterManager(PybaseInterpreterType.JYTHON);

   String[] envp = null;
   try {
      envp = SimpleRunner.getEnvironment(null, jythonJar, manager, true);
   }
   catch (PybaseException e) {
      PybaseMain.logE("Problem creating jython server", e);
   }

   process = SimpleRunner.createProcess(executableStr, envp, serverFile.getParentFile());

   return SimpleRunner.getArgumentsAsStr(executableStr);
}


}
