/********************************************************************************/
/*										*/
/*		PybaseRunner.java						*/
/*										*/
/*	Class to run python for debugging					*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.bubbles.pybase.debug;

import edu.brown.cs.bubbles.pybase.PybaseException;
import edu.brown.cs.bubbles.pybase.PybaseMain;

import java.io.File;
import java.io.IOException;
import java.net.Socket;


public class PybaseRunner implements PybaseDebugConstants
{


/********************************************************************************/
/*										*/
/*	Static method to launch a run configuration				*/
/*										*/
/********************************************************************************/

public static PybaseDebugger runDebug(PybaseLaunchConfig config) throws IOException, PybaseException
{
   PybaseDebugger dbg = new PybaseDebugger(config);
   dbg.startConnect();
   Process p = createProcess(config,dbg);
   PybaseDebugTarget tgt = new PybaseDebugTarget(dbg,p);
   Socket socket = null;
   try {
      socket = dbg.waitForConnect(p);
      if (socket == null) {
	 dbg.dispose();
	 throw new PybaseException("Debugger not set up");
       }
    }
   catch (Exception ex) {
      p.destroy();
      String msg = "Unexpected error setting up the debugger";
      throw new PybaseException(msg,ex);
    }
   tgt.startTransmission(socket);
   tgt.initialize();
   tgt.addConsoleInputListener();
   dbg.setTarget(tgt);

   return dbg;
}


/********************************************************************************/
/*										*/
/*	Process creation							*/
/*										*/
/********************************************************************************/

private static Process createProcess(PybaseLaunchConfig cfg,PybaseDebugger dbg)
	throws IOException, PybaseException
{
   String [] env = cfg.getEnvironment();

   String encoding = cfg.getEncoding();
   if (encoding != null && encoding.trim().length() > 0) {
      String [] s = new String[env.length + 3];
      System.arraycopy(env,0,s,0,env.length);
      s[s.length-3] = "PYDEV_COMPLETER_PYTHONPATH=" + cfg.getPySrcPath();
      s[s.length-2] = "PYDEV_CONSOLE_ENCODING=" + encoding;
      s[s.length-1] = "PYTHONIOENCODING=" + encoding;
      env = s;
    }
   String [] cmdline = cfg.getCommandLine(dbg);
   File wdir = cfg.getWorkingDirectory();
   Runtime rt = Runtime.getRuntime();

   StringBuffer buf = new StringBuffer();
   for (String s : cmdline) {
      buf.append(s);
      buf.append(" ");
    }
   PybaseMain.logI("RUN " + buf.toString());

   Process p = rt.exec(cmdline,env,wdir);
   return p;
}



}	// end of class PybaseRunner




/* end of PybaseRunner.java */

