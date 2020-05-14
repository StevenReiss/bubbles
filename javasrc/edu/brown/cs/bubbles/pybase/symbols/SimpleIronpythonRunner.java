/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * @author Fabio Zadrozny
 */

package edu.brown.cs.bubbles.pybase.symbols;

import edu.brown.cs.bubbles.pybase.PybaseProject;

import org.python.pydev.core.Tuple;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 *
 * This class has some useful methods for running an iron python script.
 *
 * Interesting reading for http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html  -
 * Navigate yourself around pitfalls related to the Runtime.exec() method
 *
 * @author Fabio Zadrozny
 */
public class SimpleIronpythonRunner extends SimpleRunner {


/**
 * Execute the script specified with the interpreter for a given project
 *
 * @param script the script we will execute
 * @param args the arguments to pass to the script
 * @param workingDir the working directory
 * @param project the project that is associated to this run
 *
 * @return a string with the output of the process (stdout)
 */
public Tuple<String, String> runAndGetOutputFromPythonScript(String interpreter,
	 String script,String[] args,File workingDir,PybaseProject project)
{
   String[] parameters = addInterpreterToArgs(interpreter, script, args);
   return runAndGetOutput(parameters, workingDir, project.getNature());
}

/**
 * @param script the script to run
 * @param args the arguments to be passed to the script
 * @return the string with the command to run the passed script with iron python
 */
public static String[] makeExecutableCommandStr(String interpreter,String script,
	 String[] args)
{
   String[] s = addInterpreterToArgs(interpreter, script, args);

   List<String> asList = new ArrayList<String>(Arrays.asList(s));
   asList.addAll(Arrays.asList(args));

   return asList.toArray(new String[0]);
}

private static String[] addInterpreterToArgs(String interpreter,String script,
	 String[] args)
{
   return preparePythonCallParameters(interpreter, script, args, true);
}

/**
 * Execute the string and format for windows if we have spaces...
 *
 * The interpreter can be specified.
 *
 * @param interpreter the interpreter we want to use for executing
 * @param script the python script to execute
 * @param args the arguments to the script
 * @param workingDir the directory where the script should be executed
 *
 * @return the stdout of the run (if any)
 */
public Tuple<String, String> runAndGetOutputWithInterpreter(String interpreter,
	 String script,String[] args,File workingDir,PybaseProject project)
{
   String[] s = preparePythonCallParameters(interpreter, script, args, true);
   return runAndGetOutput(s, workingDir, project.getNature());
}

/**
 * Creates array with what should be passed to Runtime.exec to run iron python.
 *
 * @param interpreter interpreter that should do the run
 * @param script iron python script to execute
 * @param args additional arguments to pass to iron python
 * @return the created array
 */
public static String[] preparePythonCallParameters(String interpreter,String script,
	 String[] args,boolean scriptExists)
{
   if (scriptExists) {
      File file = new File(script);
      if (file.exists() == false) {
	 throw new RuntimeException("The script passed for execution (" + script
		  + ") does not exist.");
      }
   }

   // Note that we don't check it (interpreter could be just the string 'ipy')
// file = new File(interpreter);
// if(file.exists() == false){
// throw new
// RuntimeException("The interpreter passed for execution ("+interpreter+") does not exist.");
// }

   String defaultVmArgs = "";
   List<String> defaultVmArgsSplit = new ArrayList<String>();
   if (defaultVmArgs != null) {
      defaultVmArgsSplit = StringUtils.split(defaultVmArgs, ' ');
   }


   if (args == null) {
      args = new String[0];
   }

   List<String> call = new ArrayList<String>();
   call.add(interpreter);
   call.addAll(defaultVmArgsSplit);
   call.add(script);
   call.addAll(Arrays.asList(args));

   return call.toArray(new String[0]);
}


}
