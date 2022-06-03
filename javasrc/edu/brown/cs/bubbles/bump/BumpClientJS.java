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
import java.lang.reflect.Method;
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
   "eclipsejar",
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

   String ws = system_properties.getProperty(BOARD_PROP_WORKSPACE);

   String cls = "edu.brown.cs.bubbles.nobase.NobaseMain";

   List<String> argl = new ArrayList<String>();
   argl.add(IvyExecQuery.getJavaPath());
   argl.add("-Dedu.brown.cs.bubbles.MINT=" + mint_name);

   String cp = System.getProperty("java.class.path");
   for (String s : nobase_libs) {
      if (s.equals("eclipsejar")) {
	 cp += File.pathSeparator + BoardSetup.getSetup().getEclipsePath();
	 continue;
       }
      String lib = BoardSetup.getSetup().getLibraryPath(s);
      if (lib == null) continue;
      File f = new File(lib);
      if (f.exists()) cp += File.pathSeparator + lib;
    }
   argl.add("-cp");
   argl.add(cp);

   File f1 = BoardSetup.getSetup().getRootDirectory();
   argl.add("-Dedu.brown.cs.bubbles.nobase.ROOT=" + f1.getAbsolutePath());

   String eopt = system_properties.getProperty(BOARD_PROP_ECLIPSE_VM_OPTIONS);
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
   BoardLog.logI("BUMP","RUN: " + run);

   try {
      IvyExec ex = null;
      if (System.getProperty("Nobase.DEBUG") != null) {
         runLocally(cls,argl);
       }
      else {
         ex = new IvyExec(argl,null,IvyExec.ERROR_OUTPUT);
       }
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
	 if (ex != null && !ex.isRunning()) {
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


private void runLocally(String cmd,List<String> args)
{
  NobaseThread nt = new NobaseThread(cmd,args);
  nt.start();
}


private static class NobaseThread extends Thread {
   
   private String main_class;
   private List<String> arg_list;
   
   NobaseThread(String cls,List<String> args) {
      super("NobaseMain");
      main_class = cls;
      arg_list = args;
    }
   
   @Override public void run() {
      boolean fnd = false;
      List<String> newargs = new ArrayList<>();
      for (String s : arg_list) {
         if (s.equals(main_class)) fnd = true;
         else if (fnd) {
            newargs.add(s);
          }
         else if (s.startsWith("-D")) {
            String a = s.substring(2);
            int idx = a.indexOf("=");
            String var = a.substring(0,idx);
            String val = a.substring(idx+1).trim();
            System.setProperty(var,val);
          }
       }
      String [] argarr = newargs.toArray(new String[newargs.size()]);
      try {
         Class<?> start = Class.forName(main_class);
         Method m = start.getMethod("main",argarr.getClass());
         m.invoke(null,(Object) argarr);
       }
      catch (Throwable t) {
         BoardLog.logE("BUMP","Problem starting nobase locally",t);
       }
    }
   
}       // end of inner class BumpClientJS



}	// end of class BumpClientJS




/* end of BumpClientJS.java */

