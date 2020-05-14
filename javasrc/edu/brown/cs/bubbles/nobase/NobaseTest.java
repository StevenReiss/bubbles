/********************************************************************************/
/*										*/
/*		NobaseTest.java 						*/
/*										*/
/*	Test interface for Nobase						*/
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



package edu.brown.cs.bubbles.nobase;

import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.io.File;


public class NobaseTest implements NobaseConstants, MintConstants
{


/********************************************************************************/
/*										*/
/*	Main Program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   NobaseTest nt = new NobaseTest(args);

   nt.runTestTrack();
   nt.runTest();

   try {
      Thread.sleep(10000);
    }
   catch (InterruptedException e) { }

   nt.sendCommand("EXIT",null,null,null);
}


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private MintControl	mint_control;
private String		instance_id;
private int		edit_id;
private Element 	last_runevent;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private NobaseTest(String [] args)
{
   mint_control = MintControl.create("NOBBLESTEST",MintSyncMode.ONLY_REPLIES);
   mint_control.register("<NOBASE TYPE='_VAR_0' />",new MessageHandler());
   instance_id = "NOBBLES_id";
   edit_id = 1;
}



/********************************************************************************/
/*										*/
/*	Actual test code							*/
/*										*/
/********************************************************************************/

private void runTest()
{
   deleteAll(new File("/u/spr/Nobbles/test/TwitterExample"));
   deleteAll(new File("/u/spr/Nobbles/test/Track"));
   deleteAll(new File("/u/spr/Nobbles/test/.projects"));

   start();

   String proj = "TwitterExample";

   sendCommand("PING",null,null,null);
   sendCommand("ENTER",null,null,null);
   sendCommand("LOGLEVEL",null,"LEVEL='DEBUG'",null);
   sendCommand("MONITOR",null,"ON=TRUE",null);
   sendCommand("GETHOST",null,null,null);
   sendCommand("PREFERENCES",null,null,null);
   sendCommand("PROJECTS",null,null,null);
   sendCommand("GETALLBREAKPOINTS",null,null,null);
   sendCommand("GETRUNCONFIG",null,null,null);
   sendCommand("GETALLNAMES",null,null,null);
   sendCommand("PROJECTS",null,null,null);
   sendCommand("CREATEPROJECT",null,"NAME='TwitterExample' DIR='/u/spr/Nobbles/test/TwitterExample'",null);
   sendCommand("OPENPROJECT",proj,"PATH='true' OPTIONS='true'",null);
   sendCommand("EDITPROJECT",proj,"LOCAL='true'",
	 "<PROJECT NAME='TwitterExample' PATH='/gpfs/main/home/spr/Nobbles/test/TwitterExample'>" +
	 "<PATH USER='true' DIRECTORY='/home/spr/home/twiex' />" +
	 "</PROJECT>");
   sendCommand("PROJECTS",null,null,null);
   sendCommand("OPENPROJECT",proj,null,null);

   sendCommand("BUILDPROJECT",proj,"REFRESH='false' CLEAN='false' FULL='false'",null);
   sendCommand("BUILDPROJECT",proj,"REFRESH='false' CLEAN='false' FULL='false'",null);
   sendCommand("GETALLNAMES",null,"BACKGROUND='NAME_1234'",null);
   sendCommand("COMMIT",proj,"SAVE='T'",null);

   sendCommand("PATTERNSEARCH",proj,"PATTERN='database.createSession()' DEFS='true' REFS='false' FOR='METHOD'",null);
   sendCommand("FINDDEFINITIONS",proj,"FILE='/gpfs/main/home/spr/home/twiex/output.js' START='2437' END='2437' IMPLS='T'",null);


   sendCommand("EDITPARAM",null,"NAME='AUTOELIDE' VALUE='TRUE'",null);
   sendCommand("EDITPARAM",null,"NAME='ELIDEDELAY' VALUE='250'",null);
   sendCommand("PATTERNSEARCH",proj,"PATTERN='search.getMaxZip()' DEFS='true' REFS='false' FOR='METHOD'",null);
   sendCommand("PATTERNSEARCH",proj,"PATTERN='server.errorHandler()' DEFS='true' REFS='false' FOR='METHOD'",null);
   sendCommand("STARTFILE",proj,"FILE='/gpfs/main/home/spr/home/twiex/server.js' ID='" + (edit_id++) + "'",null);
   sendCommand("ELIDESET",proj,"FILE='/gpfs/main/home/spr/home/twiex/server.js' COMPUTE='true'","<REGION START='2358' END='2515' />");
   sendCommand("PATTERNSEARCH",proj,"PATTERN='server.start()' DEFS='true' REFS='false' FOR='METHOD'",null);
   sendCommand("ELIDESET",proj,"FILE='/gpfs/main/home/spr/home/twiex/server.js' COMPUTE='true'","<REGION START='2358' END='2515' /><REGION START='4060' END='4955' />");
   sendCommand("FINDREFERENCES",proj,"FILE='/gpfs/main/home/spr/home/twiex/server.js' START='4088' END='4088' RONLY='T' EXACT='true' EQUIV='true'",null);
   sendCommand("FINDREFERENCES",proj,"FILE='/gpfs/main/home/spr/home/twiex/server.js' START='4088' END='4088' EXACT='true' EQUIV='true'",null);
   sendCommand("FINDREFERENCES",proj,"FILE='/gpfs/main/home/spr/home/twiex/server.js' START='4093' END='4093' EXACT='true' EQUIV='true'",null);
   sendCommand("FINDREFERENCES",proj,"FILE='/gpfs/main/home/spr/home/twiex/server.js' START='2389' END='2389' EXACT='true' EQUIV='true'",null);
   sendCommand("FINDREFERENCES",proj,"FILE='/gpfs/main/home/spr/home/twiex/server.js' START='2389' END='2389' RONLY='T' EXACT='true' EQUIV='true'",null);
   sendCommand("GETFULLYQUALIFIEDNAME",proj,"FILE='/gpfs/main/home/spr/home/twiex/server.js' START='4217' END='4217' EXACT='true' EQUIV='true'",null);
   sendCommand("FINDREGIONS",proj,"CLASS='output' FIELDS='T' FILE='/gpfs/main/home/spr/home/twiex/output.js'",null);
   sendCommand("FINDREGIONS",proj,"CLASS='output' PREFIX='T' FILE='/gpfs/main/home/spr/home/twiex/output.js'",null);
   sendCommand("FINDREGIONS",proj,"CLASS='output' STATICS='T' FILE='/gpfs/main/home/spr/home/twiex/output.js'",null);
   sendCommand("FINDREGIONS",proj,"CLASS='output' IMPORTS='T' PACKAGE='T' TOPDELCS='T' FILE='/gpfs/main/home/spr/home/twiex/output.js'",null);
   sendCommand("PATTERNSEARCH",proj,"PATTERN='database.createSession()' DEFS='true' REFS='false' FOR='METHOD'",null);
   sendCommand("STARTFILE",proj,"FILE='/gpfs/main/home/spr/home/twiex/output.js' ID='" + (edit_id++) + "'",null);
   sendCommand("ELIDESET",proj,"FILE='/gpfs/main/home/spr/home/twiex/output.js' COMPUTE='true'","<REGION START='7359' END='8064' />");

   sendCommand("FINDDEFINITIONS",proj,"FILE='/gpfs/main/home/spr/home/twiex/output.js' START='5321' END='5321' IMPLS='T'",null);
   sendCommand("FINDREFERENCES",proj,"FILE='/gpfs/main/home/spr/home/twiex/output.js' START='7372' END='7372' EXACT='T' EQUIV='T'",null);

   sendCommand("FINDDEFINITIONS",proj,"FILE='/gpfs/main/home/spr/home/twiex/output.js' START='5523' END='5523' IMPLS='T'",null);
   sendCommand("GETFULLYQUALIFIEDNAME",proj,"FILE='/gpfs/main/home/spr/home/twiex/output.js' START='5523' END='5523' EXACT='true' EQUIV='true'",null);
   sendCommand("FINDDEFINITIONS",proj,"FILE='/gpfs/main/home/spr/home/twiex/output.js' START='5535' END='5535' IMPLS='T'",null);
   sendCommand("GETFULLYQUALIFIEDNAME",proj,"FILE='/gpfs/main/home/spr/home/twiex/output.js' START='5535' END='5535' EXACT='true' EQUIV='true'",null);
   sendCommand("FINDDEFINITIONS",proj,"FILE='/gpfs/main/home/spr/home/twiex/output.js' START='5415' END='5415' IMPLS='T'",null);

   // no item returned on this
   sendCommand("PATTERNSEARCH",proj,"PATTERN='search.addTable()' DEFS='true' REFS='false' FOR='METHOD'",null);

   sendCommand("STARTFILE",proj,"FILE='/gpfs/main/home/spr/home/twiex/bad.js' ID='" + (edit_id++) + "'",null);

   ReplyHandler rh;
   sendCommand("GETRUNCONFIG",proj,null,null);
   rh = sendCommand("NEWRUNCONFIG",proj,"NAME='server'",null);
   Element reply = rh.waitForXml();
   Element config = IvyXml.getChild(reply,"CONFIGURATION");
   String cid = IvyXml.getAttrString(config,"ID");
   String launchid = "LAUNCH='" + cid + "'";
   sendCommand("EDITRUNCONFIG",proj,launchid + "' PROP='FILE' VALUE='/gpfs/main/home/spr/home/twiex/server.js'",null);
   sendCommand("EDITRUNCONFIG",proj,launchid + " PROP='MAIN_TYPE' VALUE='server'",null);
   sendCommand("EDITRUNCONFIG",proj,launchid + " PROP='WD' VALUE='/gpfs/main/home/spr/home/twiex'",null);

   long waittime = 10000;
   rh = sendCommand("START",null,"NAME='" + cid + "'",null);
   reply = rh.waitForXml();
   config = IvyXml.getChild(reply,"LAUNCH");
   String targetid = "LAUNCH='" + IvyXml.getAttrString(config,"ID") + "'";
   String stacktid = targetid + " DEPTH='1'";
   waitForRunEvent(waittime);
   sendCommand("DEBUGACTION",null,targetid + " ACTION='STEP_OVER'",null);
   waitForRunEvent(waittime);
   sendCommand("GETSTACKFRAMES",null,stacktid,null);
   sendCommand("DEBUGACTION",null,targetid + " ACTION='STEP_INTO'",null);
   waitForRunEvent(waittime);
   sendCommand("GETSTACKFRAMES",null,stacktid,null);
   sendCommand("DEBUGACTION",null,targetid+" ACTION='STEP_OVER'",null);
   waitForRunEvent(waittime);
   sendCommand("GETSTACKFRAMES",null,stacktid,null);
   sendCommand("DEBUGACTION",null,targetid + "' ACTION='STEP_OVER'",null);
   waitForRunEvent(waittime);
   sendCommand("GETSTACKFRAMES",null,stacktid,null);
   sendCommand("DEBUGACTION",null,targetid+" ACTION='RESUME'",null);
   waitForRunEvent(waittime);
   delay(10000);
   sendCommand("DEBUGACTION",null,targetid+" ACTION='SUSPEND'",null);
   waitForRunEvent(10000);
   sendCommand("GETSTACKFRAMES",null,stacktid,null);
   sendCommand("DEBUGACTION",null,"LAUNCH='TARGET_1' ACTION='TERMINATE'",null);

   sendCommand("DELETERUNCONFIG",null,launchid,null);

   sendCommand("COMMIT",proj,"SAVE='T'",null);
}



private void runTestTrack()
{
   deleteAll(new File("/u/spr/Nobbles/test/Track"));
   deleteAll(new File("/u/spr/Nobbles/test/TwitterExample"));
   deleteAll(new File("/u/spr/Nobbles/test/.projects"));

   start();

   String proj = "Track";

   sendCommand("PING",null,null,null);
   sendCommand("ENTER",null,null,null);
   sendCommand("LOGLEVEL",null,"LEVEL='DEBUG'",null);
   sendCommand("MONITOR",null,"ON=TRUE",null);
   sendCommand("GETHOST",null,null,null);
   sendCommand("PREFERENCES",null,null,null);
   sendCommand("PROJECTS",null,null,null);
   sendCommand("GETALLBREAKPOINTS",null,null,null);
   sendCommand("GETRUNCONFIG",null,null,null);
   sendCommand("GETALLNAMES",null,null,null);
   sendCommand("PROJECTS",null,null,null);
   sendCommand("CREATEPROJECT",null,"NAME='Track' DIR='/u/spr/Nobbles/test/Track'",null);
   sendCommand("OPENPROJECT",proj,"PATH='true' OPTIONS='true'",null);
   sendCommand("EDITPROJECT",proj,"LOCAL='true'",
	 "<PROJECT NAME='Track' PATH='/gpfs/main/home/spr/Nobbles/test/Track'>" +
	 "<PATH USER='true' DIRECTORY='/research/people/spr/track/server' />" +
	 "</PROJECT>");
   sendCommand("PROJECTS",null,null,null);
   sendCommand("OPENPROJECT",proj,null,null);

   sendCommand("BUILDPROJECT",proj,"REFRESH='false' CLEAN='false' FULL='false'",null);
   sendCommand("BUILDPROJECT",proj,"REFRESH='false' CLEAN='false' FULL='false'",null);
   sendCommand("GETALLNAMES",null,"BACKGROUND='NAME_1234'",null);
   sendCommand("COMMIT",proj,"SAVE='T'",null);

   sendCommand("PATTERNSEARCH",proj,"PATTERN='server.start()' DEFS='true' REFS='false' FOR='METHOD'",null);
   sendCommand("FINDDEFINITIONS",proj,"FILE='/research/people/spr/track/server/server.js' START='2437' END='2437' IMPLS='T'",null);

   sendCommand("COMMIT",proj,"SAVE='T'",null);
}


private void delay(int time)
{
   try {
      Thread.sleep(time);
    }
   catch (InterruptedException e) { }
}


private void waitForRunEvent(long wait)
{
   long now = System.currentTimeMillis();
   synchronized (this) {
      for ( ; ; ) {
	 if (wait > 0) {
	    long delta = System.currentTimeMillis() - now;
	    if (delta > wait) {
	       NobaseMain.logI("NOBASETEST: Wait timed out");
	       return;
	     }
	  }
	 if (last_runevent == null) {
	    try {
	       wait(1000);
	     }
	    catch (InterruptedException e) { }
	  }
	 if (last_runevent != null) {
	    String kind = IvyXml.getAttrString(last_runevent,"KIND");
	    last_runevent = null;
	    if (kind != null && kind.equals("SUSPEND")) break;
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	File management utilities						*/
/*										*/
/********************************************************************************/

private void deleteAll(File f)
{
   if (!f.exists()) return;
   if (f.isDirectory()) {
      for (File c : f.listFiles()) {
	 deleteAll(c);
       }
      if (!f.delete()) {
	 NobaseMain.logE("NOBASETEST: Can't delete directory " + f);
       }
    }
   else if (!f.delete()) {
      NobaseMain.logE("NOBASETEST: Can't delete file " + f);
    }
}




/********************************************************************************/
/*										*/
/*	Run the server methods							*/
/*										*/
/********************************************************************************/

private void start()
{
   if (!tryPing()) {
      Runner r = new Runner();
      r.start();
      NobaseMain.logI("NOBASETEST: STARTING");
      for (int i = 0; i < 100; ++i) {
	 if (tryPing()) break;
	 try {
	    Thread.sleep(1000);
	  }
	 catch (InterruptedException e) { }
       }
    }
}




private class Runner extends Thread {

   Runner() {
      super("NobaseRunnerThread");
    }

   @Override public void run() {
      try {
	 NobaseMain.main(new String [] { "-m", "NOBBLESTEST", "-ws", "/home/spr/Nobbles/test",
	       "-log", "/pro/bubbles/nobase/src/test.log"});
	 NobaseMain.logI("NOBASETEST: Start run");
       }
      catch (Throwable t) {
	 NobaseMain.logE("NOBASETEST: Error running: " + t);
	 t.printStackTrace();
       }
      NobaseMain.logI("NOBASETEST: Finish run");
    }

}	// end of inner class Runner




/********************************************************************************/
/*										*/
/*	Messaging methods							*/
/*										*/
/********************************************************************************/

private ReplyHandler sendCommand(String cmd,String proj,String flds,String args)
{
   ReplyHandler rh = new ReplyHandler(cmd);

   String msg = "<BUBBLES DO='" + cmd + "' LANG='Node/JS' BID='" + instance_id + "'";
   if (proj != null) msg += " PROJECT='" + proj + "'";
   if (flds != null) msg += " " + flds;
   msg += ">";
   if (args != null) msg += args;
   msg += "</BUBBLES>";

   NobaseMain.logI("NOBASETEST: BEGIN COMMAND " + cmd);
   NobaseMain.logI("NOBASETEST: SENDING: " + msg);

   synchronized (this) {
      last_runevent = null;
    }

   mint_control.send(msg,rh,MINT_MSG_FIRST_NON_NULL);

   rh.print();

   return rh;
}


private boolean tryPing()
{
   ReplyHandler rh = sendCommand("PING",null,null,null);
   String s = rh.waitForString();
   return s != null;
}



/********************************************************************************/
/*										*/
/*	Reply handler								*/
/*										*/
/********************************************************************************/

private static class ReplyHandler extends MintDefaultReply {

   private String cmd_name;

   ReplyHandler(String what) {
      cmd_name = what;
    }

   void print() {
      String rslt = waitForString();
      if (rslt == null) {
	 NobaseMain.logI("NOBASETEST: No reply for " + cmd_name);
       }
      else {
	 NobaseMain.logI("NOBASETEST: Reply for " + cmd_name + ":");
	 NobaseMain.logI(rslt);
	 NobaseMain.logI("NOBASETEST: End of reply");
       }
    }

}	// end of inner class ReplyHandler



/********************************************************************************/
/*										*/
/*	Message handler 							*/
/*										*/
/********************************************************************************/

private class MessageHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      NobaseMain.logI("NOBASETEST: Message from NOBASE:");
      NobaseMain.logI(msg.getText());
      NobaseMain.logI("NOBASETEST: End of Message");
      Element xml = msg.getXml();
      switch (IvyXml.getAttrString(xml,"TYPE")) {
	 case "RUNEVENT" :
	    NobaseTest test = NobaseTest.this;
	    synchronized (test) {
	       Element re = IvyXml.getChild(xml,"RUNEVENT");
	       String kind = IvyXml.getAttrString(re,"KIND");
	       if (kind.equals("RESUME") || kind.equals("SUSPEND")) {
		  test.last_runevent = re;
		  test.notifyAll();
		}
	     }
	    break;
       }
      msg.replyTo();
    }

}	// end of inner class MessageHandler




}	// end of class NobaseTest



/* end of NobaseTest.java */

