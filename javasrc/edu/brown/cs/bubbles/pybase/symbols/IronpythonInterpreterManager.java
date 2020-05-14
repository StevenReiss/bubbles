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


import edu.brown.cs.bubbles.pybase.PybaseFileSystem;
import edu.brown.cs.bubbles.pybase.PybaseInterpreter;
import edu.brown.cs.bubbles.pybase.PybasePreferences;

import org.python.pydev.core.Tuple;

import java.io.File;


public class IronpythonInterpreterManager extends AbstractInterpreterManager {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private final String IRONPYTHON_INTERPRETER_PATH = "IRONPYTHON_INTERPRETER_PATH";



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public IronpythonInterpreterManager(PybasePreferences prefs)
{
   super(prefs);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override protected String getPreferenceName()
{
   return IRONPYTHON_INTERPRETER_PATH;
}

@Override public String getInterpreterUIName()
{
   return "IronPython.";
}

@Override public Tuple<PybaseInterpreter, String> internalCreateInterpreterInfo(String executable,boolean askUser)
{
   return doCreateInterpreterInfo(executable, askUser);
}

@Override protected String getPreferencesPageId()
{
   return "edu.brown.cs.bubbles.pybase.interpreterPreferencesPageIronpython";
}

@Override public PybaseInterpreterType getInterpreterType()
{
   return PybaseInterpreterType.IRONPYTHON;
}


public String getManagerRelatedName()
{
   return "ironpython";
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
	 "A jar cannot be used in order to get the info for the iron python interpreter.");
    }

   File script = getInterpreterInfoPy();

   Tuple<String, String> outTup = new SimpleIronpythonRunner()
   .runAndGetOutputWithInterpreter(executable,
				      PybaseFileSystem.getFileAbsolutePath(script), null, null, null);

   PybaseInterpreter info = createInfoFromOutput(outTup, askUser);

   if (info == null) {
      // cancelled
      return null;
    }

   info.restoreCompiledLibs();

   return new Tuple<PybaseInterpreter, String>(info,outTup.o1);
}




}	// end of class IronpythonInterpreterManager




/* end of IronpythonInterpreterManager.java */
