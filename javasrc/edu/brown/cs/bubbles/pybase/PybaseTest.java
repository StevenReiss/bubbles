/********************************************************************************/
/*										*/
/*		PybaseTest.java 						*/
/*										*/
/*	description of class							*/
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



package edu.brown.cs.bubbles.pybase;

import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;



public class PybaseTest implements PybaseConstants, MintConstants
{

/********************************************************************************/
/*										*/
/*	Main Program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   PybaseTest pt = new PybaseTest(args);

   pt.runTest();

   try {
      Thread.sleep(10000);
    }
   catch (InterruptedException e) { }

   pt.sendCommand("EXIT",null,null,null);
}




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private MintControl	mint_control;
private String		instance_id;
private int		edit_id;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private PybaseTest(String [] args)
{
   mint_control = MintControl.create("PYBLESTEST",MintSyncMode.ONLY_REPLIES);
   mint_control.register("<PYBASE TYPE='_VAR_0' />",new MessageHandler());
   instance_id = "PYBLES_id";
   edit_id = 1;

   System.err.println("PYBASETEST: STARTING");

   if (!tryPing()) {
      Runner r = new Runner();
      r.start();
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
      super("PybaseRunnerThread");
    }

   @Override public void run() {
      System.err.println("PYBASE: Start run");
      try {
	 PybaseMain.main(new String [] { "-m", "PYBLESTEST", "-ws", "/home/spr/Pybles/test" });
       }
      catch (Throwable t) {
	 System.err.println("PYBASE: Error running: " + t);
	 t.printStackTrace();
       }
      System.err.println("PYBASE: Finish run");
    }

}	// end of inner class Runner




/********************************************************************************/
/*										*/
/*	Actual test code							*/
/*										*/
/********************************************************************************/

private void runTest()
{
   String proj = "testproject";

   sendCommand("PING",null,null,null);
   sendCommand("ENTER",null,null,null);
   sendCommand("LOGLEVEL",null,"LEVEL='DEBUG'",null);
   sendCommand("MONITOR",null,"ON=TRUE",null);
   sendCommand("GETHOST",null,null,null);
   sendCommand("PREFERENCES",null,null,null);
   sendCommand("PROJECTS",null,null,null);
   sendCommand("BUILDPROJECT",proj,null,null);
   sendCommand("OPENPROJECT",proj,null,null);
   sendCommand("GETALLBREAKPOINTS",null,null,null);
   sendCommand("GETRUNCONFIG",null,null,null);
   sendCommand("GETALLNAMES",null,null,null);
   sendCommand("GETALLNAMES",null,"BACKGROUND='NAME_1234'",null);
   sendCommand("EDITPARAM",null,"NAME='AUTOELIDE' VALUE='TRUE'",null);
   sendCommand("EDITPARAM",null,"NAME='ELIDEDELAY' VALUE='250'",null);
   sendCommand("STARTFILE",proj,"FILE='/gpfs/main/home/spr/Pybles/test/testproject/src/genkml.py' ID='" + (edit_id++) + "'",null);
   sendCommand("PATTERNSEARCH",proj,"PATTERN='genkml.genkml()' DEFS='true' REFS='false' FOR='METHOD'",null);
   sendCommand("ELIDESET",proj,"FILE='/gpfs/main/home/spr/Pybles/test/testproject/src/genkml.py' COMPUTE='true'","<REGION START='505' END='1722' />");
   sendCommand("STARTFILE",proj,"FILE='/gpfs/main/home/spr/Pybles/test/testproject/src/scanzips.py' ID='" + (edit_id++) + "'",null);
   sendCommand("PATTERNSEARCH",proj,"PATTERN='scanzips.output(...)' DEFS='true' REFS='false' FOR='METHOD'",null);
   sendCommand("ELIDESET",proj,"FILE='/gpfs/main/home/spr/Pybles/test/testproject/src/scanzips.py' COMPUTE='true'","<REGION START='318' END='655' />");
   sendCommand("FINDDEFINITIONS",proj,"FILE='/gpfs/main/home/spr/Pybles/test/testproject/src/genkml.py' START='548' END='548' RONLY='T' EXACT='true' EQUIV='true'",null);
   sendCommand("FINDREFERENCES",proj,"FILE='/gpfs/main/home/spr/Pybles/test/testproject/src/genkml.py' START='548' END='548' EXACT='true' EQUIV='true'",null);
   sendCommand("GETFULLYQUALIFIEDNAME",proj,"FILE='/gpfs/main/home/spr/Pybles/test/testproject/src/genkml.py' START='1076' END='1076'",null);
   sendCommand("PATTERNSEARCH",proj,"PATTERN='genkml.*' DEFS='true' REFS='false' FOR='FIELD'",null);
   sendCommand("FINDREFERENCES",proj,"FILE='/gpfs/main/home/spr/Pybles/test/testproject/src/genkml.py' START='580' END='580' EXACT='true' EQUIV='true'",null);
   sendCommand("FINDREFERENCES",proj,"FILE='/gpfs/main/home/spr/Pybles/test/testproject/src/genkml.py' START='611' END='611' EXACT='true' EQUIV='true'",null);
   sendCommand("FINDREFERENCES",proj,"FILE='/gpfs/main/home/spr/Pybles/test/testproject/src/genkml.py' START='643' END='643' EXACT='true' EQUIV='true'",null);
   sendCommand("FINDREFERENCES",proj,"FILE='/gpfs/main/home/spr/Pybles/test/testproject/src/genkml.py' START='287' END='287' EXACT='true' EQUIV='true'",null);
   sendCommand("FINDREFERENCES",proj,"FILE='/gpfs/main/home/spr/Pybles/test/testproject/src/genkml.py' START='1076' END='1076' EXACT='true' EQUIV='true'",null);
   sendCommand("FINDREGIONS",proj,"FILE='/gpfs/main/home/spr/Pybles/test/testproject/src/genkml.py' STATICS='T' CLASS='genkml'",null);
   sendCommand("FINDREGIONS",proj,"FILE='/gpfs/main/home/spr/Pybles/test/testproject/src/genkml.py' IMPORTS='T' CLASS='genkml'",null);
   sendCommand("EDITFILE",proj,"FILE='/gpfs/main/home/spr/Pybles/test/testproject/src/genkml.py' ID='" + (edit_id++) + "' NEWLINE='true'","<EDIT START='679' END='679'>#</EDIT>");
   sendCommand("EDITFILE",proj,"FILE='/gpfs/main/home/spr/Pybles/test/testproject/src/scanzips.py' ID='" + (edit_id++) + "' NEWLINE='true'","<EDIT START='342' END='342'><![CDATA[ ]]></EDIT>");
   sendCommand("EDITFILE",proj,"FILE='/gpfs/main/home/spr/Pybles/test/testproject/src/scanzips.py' ID='" + (edit_id++) + "' NEWLINE='true'","<EDIT START='358' END='361'></EDIT>");
   sendCommand("SEARCH",proj,"PATTERN='output' FLAGS='2' MAX='256'",null);
   sendCommand("FINDREFERENCES",proj,"FILE='/gpfs/main/home/spr/Pybles/test/testproject/src/genkml.py' START='635' END='635' EXACT='true' EQUIV='true' RONLY='T'",null);
   sendCommand("FINDREFERENCES",proj,"FILE='/gpfs/main/home/spr/Pybles/test/testproject/src/genkml.py' START='635' END='635' EXACT='true' EQUIV='true' WONLY='T'",null);
   sendCommand("FINDDEFINITIONS",proj,"FILE='/gpfs/main/home/spr/Pybles/test/testproject/src/genkml.py' START='635' END='635'",null);
   sendCommand("FINDDEFINITIONS",proj,"FILE='/gpfs/main/home/spr/Pybles/test/testproject/src/genkml.py' START='650' END='650' IMPLS='T'",null);
   sendCommand("PATTERNSEARCH",proj,"PATTERN='Shape.Point.__init__()' DEFS='true' REFS='false' FOR='METHOD'",null);
   sendCommand("PATTERNSEARCH",proj,"PATTERN='Shape.Point' DEFS='true' REFS='false' FOR='TYPE'",null);
   sendCommand("FINDREGIONS",proj,"CLASS='Shape.Point' FILE='/gpfs/main/home/spr/Pybles/test/testproject/src/Shape.py' COMPUNIT='T'",null);
   sendCommand("FINDREGIONS",proj,"CLASS='Shape' FILE='/gpfs/main/home/spr/Pybles/test/testproject/src/Shape.py' COMPUNIT='T'",null);
   sendCommand("FINDREGIONS",proj,"CLASS='Shape.Point' PREFIX='T'",null);
   sendCommand("PATTERNSEARCH",proj,"PATTERN='Shape.Point.distanceFromOrigin()' DEFS='true' REFS='false' FOR='METHOD'",null);
   sendCommand("FINDREGIONS",proj,"CLASS='Shape' PREFIX='T'",null);
   sendCommand("STARTFILE",proj,"FILE='/gpfs/main/home/spr/Pybles/test/testproject/src/Shape.py' ID='" + (edit_id++) + "'",null);
   sendCommand("ELIDESET",proj,"FILE='/gpfs/main/home/spr/Pybles/test/testproject/src/Shape.py' COMPUTE='true'","<REGION START='175' END='256' />");

   // sendCommand("COMMIT",proj,"SAVE='T'",null);
}



/********************************************************************************/
/*										*/
/*	Messaging methods							*/
/*										*/
/********************************************************************************/

private ReplyHandler sendCommand(String cmd,String proj,String flds,String args)
{
   ReplyHandler rh = new ReplyHandler(cmd);

   String msg = "<BUBBLES DO='" + cmd + "' LANG='Python' BID='" + instance_id + "'";
   if (proj != null) msg += " PROJECT='" + proj + "'";
   if (flds != null) msg += " " + flds;
   msg += ">";
   if (args != null) msg += args;
   msg += "</BUBBLES>";

   System.err.println("PYBASETEST: BEGIN COMMAND " + cmd);
   System.err.println("PYBASETEST: SENDING: " + msg);

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
	 System.err.println("PYBASETEST: No reply for " + cmd_name);
       }
      else {
	 System.err.println("PYBASETEST: Reply for " + cmd_name + ":");
	 System.err.println(rslt);
	 System.err.println("PYBASETEST: End of reply");
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
      System.err.println("PYBASETEST: Message from PYBASE:");
      System.err.println(msg.getText());
      System.err.println("PYBASETEST: End of Message");
      msg.replyTo();
    }

}	// end of inner class MessageHandler


}	// end of class PybaseTest




/* end of PybaseTest.java */

