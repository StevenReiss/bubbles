/********************************************************************************/
/*										*/
/*		BumpClientPython.java						*/
/*										*/
/*	BUblles Mint Partnership main class for using Python			*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss, Hsu-Sheng Ko	*/
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


/* SVN: $Id$ */



package edu.brown.cs.bubbles.bump;

import edu.brown.cs.bubbles.board.BoardConstants;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardSetup;

import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.exec.IvyExecQuery;

import javax.swing.JOptionPane;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;



/**
 *	This class provides an interface between Code Bubbles and the back-end
 *	IDE.  This particular implementation works for ECLIPSE.
 *
 *	At some point, this should be converted into an interface that can then
 *	be implemented by an appropriate class for each back end IDE.
 *
 **/

public class BumpClientPython extends BumpClient
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private boolean 	python_starting;

private static String [] python_libs = new String [] {
   "pydev.jar",
   "org.eclipse.core.jobs.jar",
   "org.eclipse.core.resources.jar",
   "org.eclipse.core.runtime.jar",
   "org.eclipse.equinox.common.jar",
   "org.eclipse.jface.jar",
   "org.eclipse.jface.text.jar",
   "org.eclipse.text.jar"
};



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BumpClientPython()
{
   python_starting = false;

   mint_control.register("<PYBASE SOURCE='PYBASE' TYPE='_VAR_0' />",new IDEHandler());
}



/********************************************************************************/
/*										*/
/*	Pybase interaction methods						*/
/*										*/
/********************************************************************************/

/**
 *	Return the name of the back end.
 **/

@Override public String getName()		{ return "Python"; }
@Override public String getServerName() 	{ return "PYBASE"; }




/**
 *	Start the back end running.  This routine will return immediately.  If the user
 *	actually needs to use the backend, they should use waitForIDE.
 **/

@Override void localStartIDE()
{
   synchronized (this) {
      if (python_starting) return;
      python_starting = true;
    }

   ensureRunning();
}




private void ensureRunning()
{
   if (tryPing()) return;
   if (BoardSetup.getSetup().getRunMode() == BoardConstants.RunMode.CLIENT) {
      BoardLog.logE("BUMP","Client mode with no Python Back End found");
      JOptionPane.showMessageDialog(null,
				       "Server must be running and accessible before client can be run",
				       "Bubbles Setup Problem",JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }

   String ws = board_properties.getProperty(BOARD_PROP_ECLIPSE_WS);

   String cls = "edu.brown.cs.bubbles.pybase.PybaseMain";

   List<String> argl = new ArrayList<String>();
   argl.add(IvyExecQuery.getJavaPath());
   argl.add("-Xmx1024m");
   argl.add("-Dedu.brown.cs.bubbles.MINT=" + mint_name);

   String cp = System.getProperty("java.class.path");
   for (String s : python_libs) {
      String lib = BoardSetup.getSetup().getLibraryPath(s);
      if (lib == null) continue;
      File f = new File(lib);
      if (f.exists()) cp += File.pathSeparator + lib;
    }
   argl.add("-cp");
   argl.add(cp);

   File f1 = BoardSetup.getSetup().getRootDirectory();
   argl.add("-Dedu.brown.cs.bubbles.pybase.ROOT=" + f1.getAbsolutePath());

   argl.add(cls);
   if (ws != null) {
      argl.add("-ws");
      argl.add(ws);
    }

   String run = null;
   for (String s : argl) {
      if (run == null) run = s;
      else run += " " + s;
    }
   BoardLog.logE("BUMP","RUN: " + run);

   try {
      IvyExec ex = new IvyExec(argl,null,IvyExec.ERROR_OUTPUT);
      boolean eok = false;
      for (int i = 0; i < 200; ++i) {
	 synchronized (this) {
	    try {
	       wait(1000);
	     }
	    catch (InterruptedException e) { }
	  }
	 if (tryPing()) {
	    BoardLog.logI("BUMP","Python Base started successfully");
	    eok = true;
	    break;
	  }
	 if (!ex.isRunning()) {
	    BoardLog.logE("BUMP","Problem starting python");
	    JOptionPane.showMessageDialog(null,
					     "Python (Pybase) could not be started.",
					     "Bubbles Setup Problem",JOptionPane.ERROR_MESSAGE);
	    System.exit(1);
	  }
       }
      if (!eok) {
	 BoardLog.logE("BUMP","Python doesn't seem to start");
	 System.exit(1);
       }
    }
   catch (IOException e) {
      BoardLog.logE("BUMP","Problem running python: " + e);
      System.exit(1);
    }
}



}	// end of class BumpClientPython




/* end of BumpClientPython.java */

