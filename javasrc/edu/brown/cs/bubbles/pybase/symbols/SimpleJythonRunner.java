/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 05/08/2005
 */

package edu.brown.cs.bubbles.pybase.symbols;

import edu.brown.cs.bubbles.pybase.PybaseInterpreter;
import edu.brown.cs.bubbles.pybase.PybaseNature;
import edu.brown.cs.bubbles.pybase.PybaseProject;

import org.python.pydev.core.Tuple;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SimpleJythonRunner extends SimpleRunner {

/**
 * Error risen when java is not available to the jython environment
 *
 * @author Fabio
 */
// @SuppressWarnings("serial") 
public static class JavaNotConfiguredException extends
	 RuntimeException {

public JavaNotConfiguredException(String string)
{
   super(string);
}

}

public Tuple<String, String> runAndGetOutputWithJar(String script,String jythonJar,
	 String args,File workingDir,PybaseProject project)
{
   File javaExecutable = findDefaultJavaExecutable();
   if (javaExecutable == null) {
      throw new JavaNotConfiguredException(
	       "Error: the java environment must be configured before jython.\n\n"
			+ "Please make sure that the java executable to be\n"
			+ "used is correctly configured in the preferences at:\n\n"
			+ "Java > Installed JREs.");
   }

   return runAndGetOutputWithJar(javaExecutable, script, jythonJar, args, workingDir,
	    project, null);
}

public Tuple<String, String> runAndGetOutputWithJar(File javaExecutable,String script,
	 String jythonJar,String args,File workingDir,PybaseProject project,
	 String additionalPythonpath)
{
   // "C:\Program Files\Java\jdk1.5.0_04\bin\java.exe" "-Dpython.home=C:\bin\jython21"
   // -classpath "C:\bin\jython21\jython.jar;%CLASSPATH%" org.python.util.jython %ARGS%
   // used just for getting info without any classpath nor pythonpath

   try {

      String javaLoc = javaExecutable.getCanonicalPath();
      String[] s;

      // In Jython 2.5b0, if we don't set python.home, it won't be able to calculate the
// correct PYTHONPATH
      // (see http://bugs.jython.org/issue1214 )

      String pythonHome = new File(jythonJar).getParent().toString();

      if (additionalPythonpath != null) {
	 jythonJar += SimpleRunner.getPythonPathSeparator();
	 jythonJar += additionalPythonpath;
	 s = new String[] { javaLoc, "-Dpython.path=" + additionalPythonpath,
		  "-Dpython.home=" + pythonHome, "-classpath", jythonJar,
		  "org.python.util.jython", script };
      }
      else {
	 s = new String[] { javaLoc, "-Dpython.home=" + pythonHome, "-classpath",
		  jythonJar, "org.python.util.jython", script };
      }

      return runAndGetOutput(s, workingDir, project.getNature());
   }
   catch (RuntimeException e) {
      throw e;
   }
   catch (Exception e) {
      throw new RuntimeException(e);
   }

}

public static String[] makeExecutableCommandStr(String jythonJar,String script,
	 String basePythonPath,String... args) throws IOException
{
   return makeExecutableCommandStrWithVMArgs(jythonJar, script, basePythonPath, "", args);
}

/**
 * @param script
 * @return
 * @throws IOException
 */
public static String[] makeExecutableCommandStrWithVMArgs(String jythonJar,String script,
	 String basePythonPath,String vmArgs,String... args) throws IOException
{

   AbstractInterpreterManager interpreterManager = PybaseNature
	    .getInterpreterManager(PybaseInterpreterType.JYTHON);
   String javaLoc = findDefaultJavaExecutable().getCanonicalPath();

   File file = new File(javaLoc);
   if (file.exists() == false) {
      throw new RuntimeException("The java location found does not exist. " + javaLoc);
   }
   if (file.isDirectory() == true) {
      throw new RuntimeException("The java location found is a directory. " + javaLoc);
   }


   if (!new File(jythonJar).exists()) {
      throw new RuntimeException(StringUtils.format(
	       "Error. The default configured interpreter: %s is does not exist!",
	       jythonJar));
   }
   PybaseInterpreter info = interpreterManager.getInterpreterInfo(jythonJar);

   // pythonpath is: base path + libs path.
   String libs = SimpleRunner.makePythonPathEnvFromPaths(info.lib_set);
   StringBuilder jythonPath = new StringBuilder(basePythonPath);
   String pathSeparator = SimpleRunner.getPythonPathSeparator();
   if (jythonPath.length() != 0) {
      jythonPath.append(pathSeparator);
   }
   jythonPath.append(libs);

   String[] s;
   s = new String[] {
	    javaLoc,
	    // cacheDir, no cache dir if it's not available
	    "-Dpython.path=" + jythonPath.toString(), "-classpath",
	    jythonJar + pathSeparator + jythonPath, vmArgs, "org.python.util.jython",
	    script };

   List<String> asList = new ArrayList<String>(Arrays.asList(s));
   asList.addAll(Arrays.asList(args));
   return asList.toArray(new String[0]);
}


private static File findDefaultJavaExecutable()
{
   // TODO: this needs to be fixed
   return new File("/usr/bin/java");
}

}
