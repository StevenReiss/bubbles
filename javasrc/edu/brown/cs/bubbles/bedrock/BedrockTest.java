/********************************************************************************/
/*										*/
/*		BedrockTest.java						*/
/*										*/
/*	Test program for BEDROCK bubbles-eclipse interface			*/
/*										*/
/********************************************************************************/
/*	Copyright 2006 Brown University -- Steven P. Reiss		      */
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


/* RCS: $Header$ */


/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/



package edu.brown.cs.bubbles.bedrock;


import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;



public class BedrockTest implements MintConstants {



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   BedrockTest bt = new BedrockTest(args);

   int ntest = 0;
   for (int i = 0; i < args.length; ++i) {
      if (args[i].equals("0")) {
	 bt.test00();
	 ++ntest;
       }
      else if (args[i].equals("1")) {
	 bt.test01();
	 ++ntest;
       }
    }

   if (ntest == 0) bt.test00();

   bt.sendCommand("EXIT",null,null,null);
}




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private MintControl	mint_control;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BedrockTest(String [] args)
{
   mint_control = MintControl.create("BUBBLES",MintSyncMode.ONLY_REPLIES);
   mint_control.register("<BEDROCK SOURCE='ECLIPSE' TYPE='_VAR_0' />",new MessageHandler());

   System.err.println("BEDROCKTEST: STARTING");
}





/********************************************************************************/
/*										*/
/*	Test cases								*/
/*										*/
/********************************************************************************/

private void test00()
{
   String proj = "trial";
   String src = "/home/spr/Eclipse/sampleworkspace/trial/src/trial/TrialMain.java";
   String src1 = "/home/spr/Eclipse/sampleworkspace/trial/src/trial/TrialTest.java";

   sendCommand("PING",null,null,null);
   sendCommand("PROJECTS",null,null,null);
   sendCommand("OPENPROJECT",proj,null,null);
   sendCommand("PREFERENCES",proj,null,null);
   sendCommand("BUILDPROJECT",proj,"CLEAN='1' FULL='1' REFRESH='1'",null);
   sendCommand("BUILDPROJECT",proj,"REFRESH='TRUE'",null);
   sendCommand("GETALLNAMES",proj,null,null);
   sendCommand("FINDDEFINITIONS",proj,"FILE='" + src + "' START='88' END='92'",null);
   sendCommand("FINDREFERENCES",proj,"FILE='" + src + "' START='88' END='88'",null);
   sendCommand("SEARCH",proj,"FLAGS='16' PATTERN='System'",null);
   sendCommand("GETFULLYQUALIFIEDNAME",proj,"FILE='" + src + "' START='88' END='92'",null);
   sendCommand("GETRUNCONFIG",proj,null,null);
   sendCommand("CLEARALLLINEBREAKPOINTS",proj,null,null);
   sendCommand("ADDLINEBREAKPOINT",proj,"FILE='" + src + "' LINE='11'",null);
   sendCommand("ADDLINEBREAKPOINT",proj,"FILE='" + src + "' LINE='13'",null);
   sendCommand("GETALLBREAKPOINTS",proj,null,null);
   sendCommand("CLEARLINEBREAKPOINT",proj,"FILE='" + src + "' LINE='13'",null);
   sendCommand("START",proj,"NAME='TrialMain'",null);

   try {
      Thread.sleep(10000);
    }
   catch (InterruptedException e) { }

   sendCommand("CLEARALLLINEBREAKPOINTS",proj,null,null);

   sendCommand("STARTFILE",proj,"FILE='" + src1 + "'",null);
   sendCommand("EDITFILE",proj,"FILE='" + src1 + "'",
		  "<EDIT START='41' END='41'>TrialTest(</EDIT>");
   sendCommand("EDITFILE",proj,"FILE='" + src1 + "' ID='1'",
		  "<EDIT START='51' END='51'><![CDATA[)$$#__CARR_RETURN\n{\nint x;\n}]]></EDIT>");
   sendCommand("EDITFILE",proj,"FILE='" + src1 + "' ID='2'",
		  "<EDIT START='54' END='54'>System.</EDIT>");
   sendCommand("GETCOMPLETIONS",proj,"FILE='" + src1 + "' OFFSET='61'",null);

   synchronized (this) {
      try {
	 wait(10000);
       }
      catch (InterruptedException e) { }
    }
}



private void test01()
{
   String proj = "trial";
   String src2 = "/home/spr/Eclipse/sampleworkspace/trial/src/trial/KeyListenerTest.java";

   sendCommand("STARTFILE",proj,"FILE='" + src2 + "'",null);
   sendCommand("FORMATMETHOD",proj,"FILE='" + src2 + "'",
		  "<REGION START='2473' END='2474' />");
   sendCommand("ELIDESET",proj,"FILE='" + src2 + "' COMPUTE='true'",
		  "<REGION START='1671' END='2000' />" +
		  "<REGION START='2006' END='2192' />" +
		  "<REGION START='1953' END='1955' PRIORITY='1.0' />" +
		  "<REGION START='1808' END='1808' PRIORITY='1.0' />");

   sendCommand("RENAME",proj,"FILE='" + src2 +"' START='1708' END='1708' NEWNAME='hello' UPDATEREFS='true'",null);
   sendCommand("PATTERNSEARCH",proj,"PATTERN='getWhen' FOR='METHOD' DEFS='false' REFS='true'",null);
   sendCommand("CALLPATH",proj,"FROM='trial.PlotSun.main(*)' TO='trial.PlotSun.get4(*)'",null);
   sendCommand("FORMATMETHOD",proj,"FILE='" + src2 + "'",
		  "<REGION START='1671' END='2000' />");
   sendCommand("PATTERNSEARCH",proj,"PATTERN='PlotSun.process' FOR='METHOD' DEFS='true' REFS='false'",null);
   sendCommand("PATTERNSEARCH",null,"PATTERN='PlotSun.process' FOR='METHOD' DEFS='true' REFS='false'",null);

   synchronized (this) {
      try {
	 wait(10000);
       }
      catch (InterruptedException e) { }
    }
}



/********************************************************************************/
/*										*/
/*	Mint handling routines							*/
/*										*/
/********************************************************************************/

private ReplyHandler sendCommand(String cmd,String proj,String flds,String args)
{
   ReplyHandler rh = new ReplyHandler(cmd);

   String msg = "<BUBBLES DO='" + cmd + "'";
   if (proj != null) msg += " PROJECT='" + proj + "'";
   if (flds != null) msg += " " + flds;
   msg +=  ">";
   if (args != null) msg += args;
   msg += "</BUBBLES>";

   System.err.println("BEDROCKTEST: BEGIN COMMAND " + cmd);
   System.err.println("BEDROCKTEST: SENDING: " + msg);

   mint_control.send(msg,rh,MINT_MSG_FIRST_NON_NULL);

   rh.print();

   return rh;
}




/********************************************************************************/
/*										*/
/*	Routines for handling run events					*/
/*										*/
/********************************************************************************/

private static int action_count = 0;


private void processRunEvent(Element re)
{
   String kind = IvyXml.getAttrString(re,"KIND");
   Element thr = IvyXml.getChild(re,"THREAD");
   Element tgt = IvyXml.getChild(re,"TARGET");

   if (kind.equals("CREATE")) {
      if (thr != null) {
	 // handle new thread
       }
      else if (tgt != null) {
	 // handle new target
       }
    }
   else if (kind.equals("RESUME")) {
    }
   else if (kind.equals("CHANGE")) {
    }
   else if (kind.equals("SUSPEND")) {
      if (thr != null) {
	 String tid = IvyXml.getAttrString(thr,"ID");
	 String ttag = IvyXml.getAttrString(thr,"TAG");
	 String fid = null;
	 ReplyHandler rh = sendCommand("GETSTACKFRAMES",null,"THREAD='" + tid + "'",null);
	 Element sfinfo = rh.waitForXml();
	 Element sfs = IvyXml.getChild(sfinfo,"STACKFRAMES");
	 for (Element sft : IvyXml.children(sfs,"THREAD")) {
	    if (!tid.equals(IvyXml.getAttrString(sft,"ID"))) continue;
	    for (Element sff : IvyXml.children(sft,"STACKFRAME")) {
	       fid = IvyXml.getAttrString(sff,"ID");
	       break;
	     }
	  }

	 switch (action_count) {
	    case 0 :
	       sendCommand("VARVAL",null,"THREAD='" + tid + "' FRAME='" + fid + "' VAR='var'",null);
	       sendCommand("VARVAL",null,"THREAD='" + ttag + "' FRAME='" + fid + "' VAR='args'",null);
	       sendCommand("DEBUGACTION",null,"THREAD='" + tid + "' ACTION='STEP_INTO'",null);
	       break;
	    case 1 :
	       sendCommand("VARVAL",null,"THREAD='" + tid + "' FRAME='" + fid + "' VAR='this'",null);
	       sendCommand("DEBUGACTION",null,"THREAD='" + tid + "' ACTION='STEP_RETURN'",null);
	       break;
	    case 2 :
	       sendCommand("DEBUGACTION",null,"THREAD='" + tid + "' ACTION='STEP_OVER'",null);
	       break;
	    case 3 :
	       sendCommand("DEBUGACTION",null,"THREAD='" + tid + "' ACTION='RESUME'",null);
	       break;
	    case 4 :
	       sendCommand("DEBUGACTION",null,"THREAD='" + tid + "' ACTION='TERMINATE'",null);
	       break;
	  }
	 ++action_count;
       }
    }
}



/********************************************************************************/
/*										*/
/*	Handle messages from eclipse						*/
/*										*/
/********************************************************************************/

public class MessageHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      System.err.println("BEDROCKTEST: MESSAGE FROM ECLIPSE:");
      System.err.println(msg.getText());
      System.err.println("BEDROCKTEST: End of MESSAGE");
      String typ = args.getArgument(0);
      if (typ.equals("RUNEVENT")) {
	 Element elt = msg.getXml();
	 for (Element re : IvyXml.children(elt,"RUNEVENT")) {
	    processRunEvent(re);
	  }
       }
      msg.replyTo();
    }

}	// end of inner class MessageHandler




/********************************************************************************/
/*										*/
/*	Reply handlers								*/
/*										*/
/********************************************************************************/

private static class ReplyHandler extends MintDefaultReply {

   private String cmd_name;

   ReplyHandler(String what) {
      cmd_name = what;
    }

   @Override public synchronized void handleReply(MintMessage msg,MintMessage rply) {
      System.err.println("BEDROCKTEST: Msg reply");
      super.handleReply(msg,rply);
    }

   void print() {
      String rslt = waitForString();
      if (rslt == null) {
	 System.err.println("BEDROCKTEST: NO REPLY FOR " + cmd_name);
       }
      else {
	 System.err.println("BEDROCKTEST: REPLY FOR " + cmd_name + ":");
	 System.err.println(rslt);
	 System.err.println("BEDROCKTEST: End of REPLY");
       }
    }

}	// end of inner class ReplyHandler




}	 // end of class BedrockTest




/* end of BedrockTest.java */
