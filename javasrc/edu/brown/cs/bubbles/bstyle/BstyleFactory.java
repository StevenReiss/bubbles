/********************************************************************************/
/*                                                                              */
/*              BstyleFactory.java                                              */
/*                                                                              */
/*      Bstyle interface inside code bubbles                                    */
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



package edu.brown.cs.bubbles.bstyle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.exec.IvyExecQuery;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;

public class BstyleFactory implements BstyleConstants, MintConstants
{

 
/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private boolean         server_running;
private boolean         server_started;
private Boolean         factory_setup;

private static BstyleFactory the_factory = null;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public synchronized static BstyleFactory getFactory()
{
   if (the_factory == null) {
      the_factory = new BstyleFactory();
    }
   return the_factory;
}


private BstyleFactory()
{
   server_running = false;
   factory_setup = null;
   server_started = false;
}




/********************************************************************************/
/*                                                                              */
/*      Initialization methods                                                  */
/*                                                                              */
/********************************************************************************/

public static void setup()
{
   BoardLog.logD("BSTYLE","Setup called");
}



public static void initialize(BudaRoot root)
{ 
   BoardLog.logD("BSTYLE","Initialize called");
   BoardThreadPool.start(new BstyleStarter());
}



/********************************************************************************//*										*/
/*	Server code								*/
/*										*/
/********************************************************************************/

void startBstyleServer()
{
   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   IvyExec exec = null;
   
   synchronized (this) {
      if (server_running || server_started) return;
      
      long mxmem = Runtime.getRuntime().maxMemory();
      mxmem = Math.min(512*1024*1024L,mxmem);
      BoardProperties bp = BoardProperties.getProperties("Bstyle");
      String dbgargs = bp.getProperty("Bstyle.jvm.args");
      if (dbgargs != null && dbgargs.contains("###")) {
	 int port = (int)(Math.random() * 1000 + 3000);
	 BoardLog.logI("BVCR","Bvcr debugging port " + port);
	 dbgargs = dbgargs.replace("###",Integer.toString(port));
       }
      
      List<String> args = new ArrayList<String>();
      args.add(IvyExecQuery.getJavaPath());
      args.add("-Xmx" + Long.toString(mxmem));
      if (dbgargs != null) {
	 StringTokenizer tok = new StringTokenizer(dbgargs);
	 while (tok.hasMoreTokens()) {
	    args.add(tok.nextToken());
	  }
       }
      String cp = System.getProperty("java.class.path");
      if (!cp.contains("checkstyle.jar")) {
         String cspath = bs.getLibraryPath("checkstyle.jar");
         cp = cp + File.pathSeparator + cspath;
       }
      args.add("-cp");
      args.add(cp);
      args.add("edu.brown.cs.bubbles.bstyle.BstyleMain");
      args.add("-m");
      args.add(bs.getMintName());
      String opts = bp.getProperty("Bstyle.options");
      if (opts != null) {
         StringTokenizer tok = new StringTokenizer(opts);
         while (tok.hasMoreTokens()) {
            args.add(tok.nextToken());
          }
       }
      
      for (int i = 0; i < 100; ++i) {
	 MintDefaultReply rply = new MintDefaultReply();
	 mc.send("<BVCR DO='PING' />",rply,MINT_MSG_FIRST_NON_NULL);
	 String rslt = rply.waitForString(1000);
	 if (rslt != null) {
	    server_running = true;
	    break;
	  }
	 if (i == 0) {
	    try {
	       exec = new IvyExec(args,null,IvyExec.ERROR_OUTPUT);
	       server_started = true;
	       BoardLog.logD("BSTYLE","Run " + exec.getCommand());
             }
	    catch (IOException e) {
	       break;
	     }
	  }
	 else {
	    try {
	       if (exec != null) {
		  // check if process exited (nothing to do)
		  exec.exitValue();
		  break;
		}
	     }
	    catch (IllegalThreadStateException e) { }
	  }
         
	 try {
	    wait(2000);
	  }
	 catch (InterruptedException e) { }
       }
      if (!server_running) {
	 BoardLog.logI("BSTYLE","Unable to start bstyle server: " + args);
       }
    }
   
   if (server_running) {
      BoardThreadPool.start(new ServerSetup());
    }
   else {
      noteSetup(false);
    }
}


private synchronized void noteSetup(boolean fg)
{
   factory_setup = fg;
   notifyAll();
}


@SuppressWarnings("unused")
private synchronized void waitForSetup()
{
   if (!server_started) return;
   
   for ( ; ; ) {
      if (factory_setup != null) break;
      try {
	 wait(5000);
       }
      catch (InterruptedException e) { }
    }
}

private static class BstyleStarter implements Runnable {
   
   @Override public void run() {
      getFactory().startBstyleServer();
    }
   
}	// end of inner class BvcrStarter



private class ServerSetup implements Runnable {

   @Override public void run() {
      // do any setup operations here
      noteSetup(true);
    }

}	// end of inner class ServerSetup



}       // end of class BstyleFactory




/* end of BstyleFactory.java */

