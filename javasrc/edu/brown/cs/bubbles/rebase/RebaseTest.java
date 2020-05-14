/********************************************************************************/
/*										*/
/*		RebaseTest.java 						*/
/*										*/
/*	Tests for Rebase package						*/
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



package edu.brown.cs.bubbles.rebase;

import edu.brown.cs.bubbles.rebase.word.RebaseWordBag;

import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.xml.IvyXml;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;



public class RebaseTest implements RebaseConstants, MintConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private RebaseMain	rebase_main;
private MintControl	mint_control;
private MessageHandler	message_handler;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public RebaseTest()
{
   String mint = "REBUS_TEST";
   System.setProperty("edu.brown.cs.bubbles.MINT",mint);
   System.setProperty("edu.brown.cs.bubbles.rebase.ROOT","/pro/bubbles");

   String [] args = new String [] { };
   rebase_main = new RebaseMain(args);
   rebase_main.start();

   message_handler = new MessageHandler();
   mint_control = MintControl.create(mint,MintSyncMode.ONLY_REPLIES);
   mint_control.register("<REBASE SOURCE='REBASE' TYPE='_VAR_0' />",message_handler);
}


/********************************************************************************/
/*										*/
/*	Search Tests								*/
/*										*/
/********************************************************************************/

@Test
public void testSearch01()
{
   RebaseRepo rr = new RebaseRepoOhloh();
   String search = "login dialog jcheckbox";
   List<RebaseSource> rslt = null;
   try {
      rslt = rr.getSources(search,null,null,null);
      Assert.assertTrue("Results returned",rslt.size() > 0);
    }
   catch (Throwable t) {
      Assert.fail("Problem with search: " + t);
      return;
    }

   for (RebaseSource rs : rslt) {
      String txt = rs.getText();
      Assert.assertTrue("Valid result",(txt != null && txt.length() > 0));
      String pth = rs.getPath();
      Assert.assertNotNull("Valid path",pth);
    }
}


@Test
public void testSearch02()
{
   String search = "login dialog jcheckbox";
   RebaseRequest rq = new RebaseRequest(rebase_main,search,null,null,null,SourceType.PACKAGE);
   MessageWaiter mw = new MessageWaiter("RESOURCE");
   message_handler.addWaiter(mw);
   rq.doNextSearch();
   mw.waitForReply();

   mw = new MessageWaiter("RESOURCE");
   message_handler.addWaiter(mw);
   rq.doNextSearch();
   mw.waitForReply();
}


@Test
public void testSearch03()
{
   sendCommand("PING",null,null,null);
   sendCommand("PROJECTS",null,null,null);
   sendCommand("PREFERENCES",null,null,null);
   sendCommand("REBUSSEARCH",null,"REPO='OHLOH' TEXT='login dialog jcheckbox' TYPE='PACKAGE'",
	 null,"RESOURCE");


   sendCommand("PROJECTS",null,null,null);
   sendCommand("GETALLNAMES",null,"BACKGROUND='XXX'",null,"ENDNAMES");

   try {
      Thread.sleep(30000);
    }
   catch (InterruptedException e) { }

   sendCommand("PATTERNSEARCH","Chessboard",
	 "PATTERN='chessboard.ics.Login.actionPerformed(java.awt.event.ActionEvent)' " +
	 "DEFS='true' REFS='false' FOR='METHOD'",null);
   sendCommand("EDITPARAM",null,"NAME='AUTOELIDE' VALUE='TRUE'",null);
   sendCommand("EDITPARAM",null,"NAME='ELIDEDELAY' VALUE='250'",null);
   String file = "/REBUS/Kd2urdQmWUo/OHLOH/OoyzU4RecOI/xTQ4WuI2Jkaoszfys3Ymh2r_P0c/VojtasChess/src/chessboard/ics/Login.java";


   sendCommand("STARTFILE","Chessboard","FILE='" + file + "' ID='1'",null);
   sendCommand("ELIDESET","Chessboard","FILE='" + file + "' COMPUTE='TRUE'",
		  "<REGION START='5303' END='5821' />");
   sendCommand("FINDREFERENCES","Chessboard","FILE='" + file + "' START='5400' END='5400' RONLY='T'",null);
   sendCommand("FINDREFERENCES","Chessboard","FILE='" + file + "' START='5400' END='5400' WONLY='T'",null);
   sendCommand("FINDDEFINITIONS","Chessboard","FILE='" + file + "' START='5400' END='5400'",null);
   sendCommand("GETFULLYQUALIFIEDNAME","Chessboard","FILE='" + file + "' START='5088' END='5088'",null);
}



@Test
public void testSearch05()
{
   sendCommand("REBUSSEARCH",null,"REPO='OHLOH' TEXT='stemmer english' TYPE='FILE'",
	 null,"RESOURCE");

   try {
      Thread.sleep(15000);
    }
   catch (InterruptedException e) { }

   String proj = "Digital_Learning_Sciences_(DLS)";
   String file = "/REBUS/3P-z4juiknk/OHLOH/pkl9onnMtQ0/aOD7PihHEavg30S503QNYBu8kOY/dlese-tools-project/src/org/dlese/dpc/index/Stemmer.java";
   sendCommand("STARTFILE",proj,"FILE='" + file + "' ID='1'",null);
   sendCommand("FINDDEFINITIONS",proj,"FILE='" + file + "' START='16493' END='16493' IMPLS='T'",null);

   String p1 = "neon-plugins";
   sendCommand("PATTERNSEARCH",p1,
		  "PATTERN='org.neontoolkit.upm.labeltranslator.util.Stemmer.&lt;init&gt;(java.lang.String)' DEFS='true' REFS='false' FOR='METHOD'",
		  null);

   sendCommand("REBUSEXPAND",proj,"FILE='" + file + "'",null);
}



@Test
public void testSearch06()
{
   RebaseRepo rr = new RebaseRepoGithub();
   String search = "login dialog jcheckbox";
   List<RebaseSource> rslt = null;
   try {
      rslt = rr.getSources(search,null,null,null);
      Assert.assertTrue("Results returned",rslt.size() > 0);
    }
   catch (Throwable t) {
      Assert.fail("Problem with search: " + t);
      return;
    }
   
   for (RebaseSource rs : rslt) {
      String txt = rs.getText();
      Assert.assertTrue("Valid result",(txt != null && txt.length() > 0));
      String pth = rs.getPath();
      Assert.assertNotNull("Valid path",pth);
    }
}


@Test
public void testSearch07()
{
   RebaseRepo rr = new RebaseRepoGrepCode();
   String search = "login dialog jcheckbox";
   List<RebaseSource> rslt = null;
   try {
      rslt = rr.getSources(search,null,null,null);
      Assert.assertTrue("Results returned",rslt.size() > 0);
    }
   catch (Throwable t) {
      Assert.fail("Problem with search: " + t);
      return;
    }
   
   for (RebaseSource rs : rslt) {
      String txt = rs.getText();
      Assert.assertTrue("Valid result",(txt != null && txt.length() > 0));
      String pth = rs.getPath();
      Assert.assertNotNull("Valid path",pth);
    }
}




/********************************************************************************/
/*										*/
/*	Word bag tests								*/
/*										*/
/********************************************************************************/

@Test
public void testSearch04() throws Exception
{
   File f = new File("/pro/bubbles/rebase/src/RebaseMain.java");
   StringBuffer buf = new StringBuffer();
   try (FileReader fr = new FileReader(f)) {
      char [] cbuf = new char[16384];
      for ( ; ; ) {
         int ln = fr.read(cbuf);
         if (ln <= 0) break;
         buf.append(cbuf,0,ln);
       }
    }

   File f1 = new File("/tmp/wordbag.test");
   RebaseWordBag wordbag = new RebaseWordBag(buf.toString());
   wordbag.outputBag(f1);
   RebaseWordBag bag1 = new RebaseWordBag();
   bag1.inputBag(f1);

   double v = wordbag.cosine(bag1);
   Assert.assertEquals("Cosine of self after write/read",v,1.0,0.01);
}




/********************************************************************************/
/*										*/
/*	Mint handling routines							*/
/*										*/
/********************************************************************************/

private ReplyHandler sendCommand(String cmd,String proj,String flds,String args)
{
   return sendCommand(cmd,proj,flds,args,null);
}


private ReplyHandler sendCommand(String cmd,String proj,String flds,String args,String resp)
{
   ReplyHandler rh = new ReplyHandler(cmd,resp);

   String msg = "<BUBBLES DO='" + cmd + "'";
   if (proj != null) msg += " PROJECT='" + proj + "'";
   if (flds != null) msg += " " + flds;
   msg += " LANG='Rebase'";
   msg +=  ">";
   if (args != null) msg += args;
   msg += "</BUBBLES>";

   System.err.println("BEDROCKTEST: BEGIN COMMAND " + cmd);
   System.err.println("BEDROCKTEST: SENDING: " + msg);

   MessageWaiter mw = rh.getWaiter();
   if (mw != null) {
      message_handler.addWaiter(mw);
    }

   mint_control.send(msg,rh,MINT_MSG_FIRST_NON_NULL);

   rh.print();

   if (mw != null ) mw.waitForReply();

   return rh;
}



/********************************************************************************/
/*										*/
/*	Handle messages from eclipse						*/
/*										*/
/********************************************************************************/

class MessageHandler implements MintHandler {

   private List<MessageWaiter> wait_fors;

   MessageHandler() {
      wait_fors = new ArrayList<MessageWaiter>();
    }

   synchronized void addWaiter(MessageWaiter mw) {
      wait_fors.add(mw);
    }

   @Override public void receive(MintMessage msg,MintArguments args) {
      System.err.println("REBASETEST: MESSAGE FROM REBASE:");
      System.err.println(msg.getText());
      System.err.println("REBASETEST: END OF MESSAGE");
      Element xml = msg.getXml();
      String cmd = IvyXml.getAttrString(xml,"TYPE");

      msg.replyTo();

      synchronized (this) {
	 for (Iterator<MessageWaiter> it = wait_fors.iterator(); it.hasNext(); ) {
	    MessageWaiter mw = it.next();
	    if (mw.getCommand().equals(cmd)) {
	       it.remove();
	       mw.noteReply(msg.getText());
	     }
	  }
       }
    }

}	// end of inner class Message Handler


private static class MessageWaiter {

   private String wait_command;
   private String reply_text;
   private boolean have_reply;

   MessageWaiter(String cmd) {
      wait_command = cmd;
      reply_text = null;
      have_reply = false;
    }

   String getCommand()				{ return wait_command; }

   synchronized void noteReply(String txt) {
      reply_text = txt;
      have_reply = true;
      notifyAll();
    }

   synchronized String waitForReply() {
      while (!have_reply) {
	 try {
	    wait();
	  }
	 catch (InterruptedException e) { }
       }
      return reply_text;
    }

}	// end of inner class MessageWaiter




/********************************************************************************/
/*										*/
/*	Reply handlers								*/
/*										*/
/********************************************************************************/

private static class ReplyHandler extends MintDefaultReply {

   private String cmd_name;
   private MessageWaiter wait_for;

   ReplyHandler(String what,String resp) {
      cmd_name = what;
      wait_for = null;
      if (resp != null) wait_for = new MessageWaiter(resp);
    }

   @Override public synchronized void handleReply(MintMessage msg,MintMessage rply) {
      // System.err.println("REBASETEST: Msg reply");
      super.handleReply(msg,rply);
    }

   MessageWaiter getWaiter()			{ return wait_for; }

   void print() {
      String rslt = waitForString();
      if (rslt == null) {
	 System.err.println("REBASETEST: NO REPLY FOR " + cmd_name);
       }
      else {
	 System.err.println("REBASETEST: REPLY FOR " + cmd_name + ":");
	 System.err.println(rslt);
	 System.err.println("REBASETEST: End of REPLY");
       }
    }

}	// end of inner class ReplyHandler






}	// end of class RebaseTest




/* end of RebaseTest.java */

