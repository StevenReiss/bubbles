/********************************************************************************/
/*										*/
/*		BumpClientRebus.java						*/
/*										*/
/*	Repository Search client for bubbles					*/
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


public class BumpClientRebus extends BumpClientJava
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private boolean 	rebus_starting;

private static String [] rebus_libs = new String [] {
   "jsoup.jar",
   "junit.jar",
   "asm6.jar",
   "org.eclipse.jdt.core.jar",
   "org.eclipse.core.jobs.jar",
   "org.eclipse.core.resources.jar",
   "org.eclipse.core.runtime.jar",
   "org.eclipse.core.contenttype.jar",
   "org.eclipse.equinox.preferences.jar",
   "org.eclipse.equinox.common.jar",
   "org.eclipse.jface.jar",
   "org.eclipse.jface.text.jar",
   "org.eclipse.text.jar",
   "org.eclipse.osgi.jar",
};



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BumpClientRebus()
{
   rebus_starting = false;

   mint_control.register("<REBASE SOURCE='REBASE' TYPE='_VAR_0' />",new IDEHandler());
}



/********************************************************************************/
/*										*/
/*	Eclipse interaction methods						*/
/*										*/
/********************************************************************************/

/**
 *	Return the name of the back end.
 **/

@Override public String getName()		{ return "Rebase"; }
@Override public String getServerName() 	{ return "REBASE"; }




/**
 *	Start the back end running.  This routine will return immediately.  If the user
 *	actually needs to use the backend, they should use waitForIDE.
 **/

@Override void localStartIDE()
{
   synchronized (this) {
      if (rebus_starting) return;
      rebus_starting = true;
    }

   ensureRunning();
}




private void ensureRunning()
{
   if (tryPing()) return;
   if (BoardSetup.getSetup().getRunMode() == BoardConstants.RunMode.CLIENT) {
      BoardLog.logE("BUMP","Client mode with no Rebus Back End found");
      JOptionPane.showMessageDialog(null,
	    "Server must be running and accessible before client can be run",
	    "Bubbles Setup Problem",JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }

   String ws = board_properties.getProperty(BOARD_PROP_ECLIPSE_WS);
   String cls = "edu.brown.cs.bubbles.rebase.RebaseMain";

   List<String> argl = new ArrayList<String>();
   argl.add(IvyExecQuery.getJavaPath());

   String opts = board_properties.getProperty("edu.brown.cs.bubbles.rebase.options");
   if (opts != null) {
      StringTokenizer tok = new StringTokenizer(opts);
      while (tok.hasMoreTokens()) {
	 String opt = tok.nextToken();
	 argl.add(opt);
       }
    }

   argl.add("-Xmx1024m");
   argl.add("-Dedu.brown.cs.bubbles.MINT=" + mint_name);

   String cp = System.getProperty("java.class.path");
   for (String s : rebus_libs) {
      String lib = BoardSetup.getSetup().getLibraryPath(s);
      if (lib == null) continue;
      File f = new File(lib);
      if (f.exists()) cp += File.pathSeparator + lib;
    }
   argl.add("-cp");
   argl.add(cp);

   File f1 = BoardSetup.getSetup().getRootDirectory();
   argl.add("-Dedu.brown.cs.bubbles.rebase.ROOT=" + f1.getAbsolutePath());

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
   BoardLog.logI("BUMP","RUN: " + run);

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
	    BoardLog.logI("BUMP","Rebus Base started successfully");
	    eok = true;
	    break;
	  }
	 if (!ex.isRunning()) {
	    BoardLog.logE("BUMP","Problem starting rebus");
	    JOptionPane.showMessageDialog(null,
		  "Rebus (Rebase) could not be started.",
		  "Bubbles Setup Problem",JOptionPane.ERROR_MESSAGE);
	    System.exit(1);
	  }
       }
      if (!eok) {
	 BoardLog.logE("BUMP","Rebus doesn't seem to start");
	 System.exit(1);
       }
    }
   catch (IOException e) {
      BoardLog.logE("BUMP","Problem running rebus: " + e);
      System.exit(1);
    }
}



}	// end of class BumpClientRebus




/* end of BumpClientRebus.java */

