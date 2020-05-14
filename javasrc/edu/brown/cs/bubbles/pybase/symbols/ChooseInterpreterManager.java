/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */

package edu.brown.cs.bubbles.pybase.symbols;

import edu.brown.cs.bubbles.pybase.PybaseConstants;
import edu.brown.cs.bubbles.pybase.PybaseNature;


/**
 * On a number of cases, we may want to do some action that relies on the python nature, but we are uncertain
 * on which should actually be used (python or jython).
 *
 * So, this class helps in giving a choice for the user.
 *
 * @author Fabio
 */
public class ChooseInterpreterManager implements PybaseConstants {



public static AbstractInterpreterManager chooseInterpreterManager()
{
   AbstractInterpreterManager manager = PybaseNature.getInterpreterManager(PybaseInterpreterType.PYTHON);
   if (manager.isConfigured()) {
      return manager;
    }

   manager = PybaseNature.getInterpreterManager(PybaseInterpreterType.JYTHON);
   if (manager.isConfigured()) {
      return manager;
    }

   manager = PybaseNature.getInterpreterManager(PybaseInterpreterType.IRONPYTHON);
   if (manager.isConfigured()) {
      return manager;
    }

   return null;
}



}	// end of class ChooseInterpreterManager




/* end of ChooseInterpreterManager.java */
