/********************************************************************************/
/*                                                                              */
/*              BumpClientLsp.java                                              */
/*                                                                              */
/*      Run LSPBASE for various languages                                       */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.bubbles.bump;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

import edu.brown.cs.bubbles.board.BoardConstants;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.exec.IvyExecQuery;

class BumpClientLsp extends BumpClient
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String          for_language;
private boolean         lspbase_starting;

private static String [] lspbase_libs = new String [] {
   "lspbase.jar",
   "ivy.jar",
   "json.jar",
};



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BumpClientLsp(String lang)
{
   for_language = lang;
   lspbase_starting = false;
   
   mint_control.register("<LSPBASE SOURCE='LSPBASE' TYPE='_VAR_0' />",
         new IDEHandler());
}


/********************************************************************************/
/*                                                                              */
/*      LspBase interaction methods                                             */
/*                                                                              */
/********************************************************************************/


/**
 *	Return the name of the back end.
 **/

@Override public String getName()		{ return "DartLsp"; }
@Override public String getServerName() 	{ return "LSPBASE"; }




/**
 *	Start the back end running.  This routine will return immediately.  If the user
 *	actually needs to use the backend, they should use waitForIDE.
 **/

@Override void localStartIDE()
{
   synchronized (this) {
      if (lspbase_starting) return;
      lspbase_starting = true;
    }
   
   ensureRunning();
}




private void ensureRunning()
{
   if (tryPing()) return;
   if (BoardSetup.getSetup().getRunMode() == BoardConstants.RunMode.CLIENT) {
      BoardLog.logE("BUMP","Client mode with no LspBase/" + for_language + " found");
      JOptionPane.showMessageDialog(null,
	    "Server must be running and accessible before client can be run",
	    "Bubbles Setup Problem",JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }
   
   String ws = system_properties.getProperty(BOARD_PROP_WORKSPACE);
   
   String cls = "edu.brown.cs.bubbles.lspbase.LspBaseMain";
   
   List<String> argl = new ArrayList<String>();
   argl.add(IvyExecQuery.getJavaPath());
// argl.add("-Dedu.brown.cs.bubbles.MINT=" + mint_name);
   
   String cp = null;
   for (String s : lspbase_libs) {
      if (s.equals("eclipsejar")) {
	 cp += File.pathSeparator + BoardSetup.getSetup().getEclipsePath();
	 continue;
       }
      String lib = BoardSetup.getSetup().getLibraryPath(s);
      if (lib == null) continue;
      File f = new File(lib);
      if (f.exists()) {
         if (cp == null) cp = lib;
         else cp += File.pathSeparator + lib;
       }
    }
   argl.add("-cp");
   argl.add(cp);
   
   File f1 = BoardSetup.getSetup().getRootDirectory();
   argl.add("-Dedu.brown.cs.bubbles.lspbase.ROOT=" + f1.getAbsolutePath());
   
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
   argl.add("-mint");
   argl.add(mint_name);
   argl.add("-lang");
   argl.add(for_language);
   argl.add("-err");
   
   String run = null;
   for (String s : argl) {
      if (run == null) run = s;
      else run += " " + s;
    }
   BoardLog.logI("BUMP","RUN: " + run);
   
   try {
      IvyExec ex = null;
      if (System.getProperty("LspBase.DEBUG") != null) {
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
	    BoardLog.logI("BUMP","LspBase/" + for_language + " started successfully");
	    eok = true;
	    break;
	  }
	 if (ex != null && !ex.isRunning()) {
	    BoardLog.logE("BUMP","Problem starting javascript back end");
	    JOptionPane.showMessageDialog(null,
		  "LspBase could not be started.",
		  "Bubbles Setup Problem",JOptionPane.ERROR_MESSAGE);
	    System.exit(1);
	  }
       }
      if (!eok) {
	 BoardLog.logE("BUMP","Lspbase/" + for_language + " doesn't seem to start");
	 System.exit(1);
       }
    }
   catch (IOException e) {
      BoardLog.logE("BUMP","Problem running LSPBASE: " + e);
      System.exit(1);
    }
}


private void runLocally(String cmd,List<String> args)
{
   LspbaseThread nt = new LspbaseThread(cmd,args);
   nt.start();
}


private static class LspbaseThread extends Thread {
   
   private String main_class;
   private List<String> arg_list;
   
   LspbaseThread(String cls,List<String> args) {
      super("LspBaseMain");
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
         BoardLog.logE("BUMP","Problem starting lspbase locally",t);
       }
    }
   
}       // end of inner class LspBaseThread



}       // end of class BumpClientLsp




/* end of BumpClientLsp.java */

