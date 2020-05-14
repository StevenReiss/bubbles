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


public class PythonInterpreterManager extends AbstractInterpreterManager {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private final String PYTHON_INTERPRETER_PATH = "INTERPRETER_PATH_NEW";


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public PythonInterpreterManager(PybasePreferences preferences)
{
   super(preferences);
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override protected String getPreferenceName()
{
   return PYTHON_INTERPRETER_PATH;
}

@Override public String getInterpreterUIName()
{
   return "Python";
}

@Override public Tuple<PybaseInterpreter,String> internalCreateInterpreterInfo(String executable,boolean askUser)
{
   return doCreateInterpreterInfo(executable, askUser);
}

@Override protected String getPreferencesPageId()
{
   return "edu.brown.cs.bubbles.pybase.interpreterPreferencesPagePython";
}


@Override public PybaseInterpreterType getInterpreterType()
{
   return PybaseInterpreterType.PYTHON;
}


public String getManagerRelatedName()
{
   return "python";
}



/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

public static Tuple<PybaseInterpreter, String> doCreateInterpreterInfo(String executable,
									  boolean askUser)
{
   boolean isJythonExecutable = PybaseInterpreter.isJythonExecutable(executable);
   if (isJythonExecutable) {
      throw new RuntimeException(
	 "A jar cannot be used in order to get the info for the python interpreter.");
    }

   File script = getInterpreterInfoPy();

   Tuple<String, String> outTup = new SimplePythonRunner()
   .runAndGetOutputWithInterpreter(executable,
				      PybaseFileSystem.getFileAbsolutePath(script), null, null,null);

   PybaseInterpreter info = createInfoFromOutput(outTup, askUser);

   if (info == null) {
      // cancelled
      return null;
    }

   info.restoreCompiledLibs();

   return new Tuple<PybaseInterpreter, String>(info,outTup.o1);
}


}	// end of class PythonInterpreterManager



/* end of PythonInterpreterManager.java */
