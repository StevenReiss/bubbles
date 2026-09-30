/********************************************************************************/
/*										*/
/*		BanalMain.java							*/
/*										*/
/*	Bubbles ANALysis package main program for running independently 	*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.banal;

import java.io.File;

import edu.brown.cs.ivy.exec.IvySetup;
import edu.brown.cs.ivy.file.IvyLog;



public final class BanalMain implements BanalConstants {



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   BanalMain bm = new BanalMain(args);

   bm.process();
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		mint_handle;
private BanalMonitor	the_monitor;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BanalMain(String [] args)
{
   IvyLog.setupLogging("BANAL",false);
   
   mint_handle = null;
   the_monitor = null;

   scanArgs(args);

   IvySetup.setup();
   
   IvyLog.logD("BANAL","BANAL server starting");
}



/********************************************************************************/
/*										*/
/*	Argument processing							*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-m") && i+1 < args.length) {   // -m <mint handle>
	    mint_handle = args[++i];
	  }
	 else if (args[i].startsWith("-S")) {                           // -SERVER
	  }
         else if (args[i].startsWith("-D")) {                           // -Debug
            IvyLog.setLogLevel(IvyLog.LogLevel.DEBUG);
          }
         else if (args[i].startsWith("-O")) {                           // -Output
            IvyLog.useStdErr(true);
          }
         else if (args[i].startsWith("-L") && i+1 < args.length) {      // -L logfile
            String path = args[++i];
            File logf = new File(path);
            IvyLog.setLogFile(logf);
            IvyLog.useStdErr(false);
          }
	 else badArgs();
       }
      else {
	 badArgs();
       }
    }
   
   if (mint_handle == null) badArgs();
}



private void badArgs()
{
   IvyLog.logE("BANAL","banalmain -m <mint_handle>");
   System.exit(1);
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void process()
{
   the_monitor = new BanalMonitor(this,mint_handle);
   the_monitor.server();

   IvyLog.logI("BANAL","Server exiting");

   System.exit(0);
}




}	// end of class BanalMain



/* end of BanalMain.java */


