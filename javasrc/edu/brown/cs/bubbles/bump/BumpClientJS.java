/********************************************************************************/
/*										*/
/*		BumpClientJS.java						*/
/*										*/
/*	BUbbles Mint Partnership main class for using Node/JS			*/
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
import java.util.StringTokenizer;


class BumpClientJS extends BumpClient
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private boolean 	nobase_starting;

private static String [] nobase_libs = new String [] {
   "json.jar",
   "asm.jar",
   "jsoup.jar",
   "org.eclipse.jface.text.jar",
   "org.eclispe.jface.jar",
   "org.eclipse.equinox.common.jar",
   "org.eclipse.core.resource.jar",
   "org.eclipse.core.runtime.jar",
   "org.eclipse.core.jobs.jar",
   "org.eclipse.text.jar",
   "org.eclipse.wst.jsdt.core.jar",
   "org.eclipse.wst.jsdt.debug.core.jar",
   "org.eclipse.osgi.jar",
   "org.eclipse.osgi.util.jar",
   "com.google.guava.jar",
   "com.google.javascript.jar",
};


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BumpClientJS()
{
   nobase_starting = false;

   mint_control.register("<NOBASE SOURCE='NOBASE' TYPE='_VAR_0' />",new IDEHandler());
}



/********************************************************************************/
/*										*/
/*	Nobase interaction methods						*/
/*										*/
/********************************************************************************/

/**
 *	Return the name of the back end.
 **/

@Override public String getName()		{ return "Node/JS"; }
@Override public String getServerName() 	{ return "NOBASE"; }




/**
 *	Start the back end running.  This routine will return immediately.  If the user
 *	actually needs to use the backend, they should use waitForIDE.
 **/

@Override void localStartIDE()
{
   synchronized (this) {
      if (nobase_starting) return;
      nobase_starting = true;
    }

   ensureRunning();
}




private void ensureRunning()
{
   if (tryPing()) return;
   if (BoardSetup.getSetup().getRunMode() == BoardConstants.RunMode.CLIENT) {
      BoardLog.logE("BUMP","Client mode with no Node/JS Back End found");
      JOptionPane.showMessageDialog(null,
	    "Server must be running and accessible before client can be run",
	    "Bubbles Setup Problem",JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }

   String ws = board_properties.getProperty(BOARD_PROP_ECLIPSE_WS);

   String cls = "edu.brown.cs.bubbles.nobase.NobaseMain";

   List<String> argl = new ArrayList<String>();
   argl.add(IvyExecQuery.getJavaPath());
   argl.add("-Dedu.brown.cs.bubbles.MINT=" + mint_name);

   String cp = System.getProperty("java.class.path");
   for (String s : nobase_libs) {
      String lib = BoardSetup.getSetup().getLibraryPath(s);
      if (lib == null) continue;
      File f = new File(lib);
      if (f.exists()) cp += File.pathSeparator + lib;
    }
   argl.add("-cp");
   argl.add(cp);

   File f1 = BoardSetup.getSetup().getRootDirectory();
   argl.add("-Dedu.brown.cs.bubbles.nobase.ROOT=" + f1.getAbsolutePath());

   String eopt = board_properties.getProperty(BOARD_PROP_ECLIPSE_VM_OPTIONS);
   if (eopt != null) {
      StringTokenizer tok = new StringTokenizer(eopt," ");
      while (tok.hasMoreTokens()) argl.add(tok.nextToken());
   }

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
	    BoardLog.logI("BUMP","Node/JS Base started successfully");
	    eok = true;
	    break;
	  }
	 if (!ex.isRunning()) {
	    BoardLog.logE("BUMP","Problem starting javascript back end");
	    JOptionPane.showMessageDialog(null,
		  "Node/JS (Nobase) could not be started.",
		  "Bubbles Setup Problem",JOptionPane.ERROR_MESSAGE);
	    System.exit(1);
	  }
       }
      if (!eok) {
	 BoardLog.logE("BUMP","Node/JS doesn't seem to start");
	 System.exit(1);
       }
    }
   catch (IOException e) {
      BoardLog.logE("BUMP","Problem running Node/JS: " + e);
      System.exit(1);
    }
}



}	// end of class BumpClientJS




/* end of BumpClientJS.java */

