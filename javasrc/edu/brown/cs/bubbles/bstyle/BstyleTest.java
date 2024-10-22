/********************************************************************************/
/*                                                                              */
/*              BstyleTest.java                                                 */
/*                                                                              */
/*      Test driver for bstyle                                                  */
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
import java.util.Map;
import java.util.Random;

import org.junit.Test;
import org.w3c.dom.Element;

import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.mint.MintReply;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

public class BstyleTest implements BstyleConstants, MintConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static MintControl       mint_control;
private static Random random_gen = new Random();


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public BstyleTest()
{
   
}


/********************************************************************************/
/*                                                                              */
/*      CATRE test                                                              */
/*                                                                              */
/********************************************************************************/

@Test
public void bstyleTestCatre()
{
   runServerTest("catre","catre");
}



/********************************************************************************/
/*                                                                              */
/*      Generic testing routines                                                */
/*                                                                              */
/********************************************************************************/

private void runServerTest(String dir,String pid)
{
   try {
      runServerTest1(dir,pid);
    }
   catch (Throwable t) {
      IvyLog.logE("BSTYLE","Test failed",t);
      throw t;
    }
}


private void runServerTest1(String dir,String pid)
{
   int rint = random_gen.nextInt(1000000);
   
   if (dir == null) dir = pid;
   String mid = "BSTYLE_TEST_" + pid.toUpperCase() + "_" + rint;
   
   setupBedrock(dir,mid,pid);
   
   File log = new File("/vol/spr");
   if (!log.exists()) {
      log = new File("/Users/spr/bstyle");
    }
   String loghead = log.getAbsolutePath() + "/";
   
   try {
      String [] args = new String[] { "-m", mid, "-DEBUG","-OUTPUT",
	    "-LOG", loghead + "servertest" + dir + ".log" };
      
      BstyleMain.main(args); 
    }
   catch (Throwable t) {
      System.err.println("PROBLEM RUNNING TEST");
      t.printStackTrace();
    }
   finally {
      shutdownBedrock();
    }
}



/********************************************************************************/
/*										*/
/*	Bubbles Messaging methods						*/
/*										*/
/********************************************************************************/

private static Element sendBubblesXmlReply(String cmd,String proj,Map<String,Object> flds,String cnts)
{
   MintDefaultReply mdr = new MintDefaultReply();
   sendBubblesMessage(cmd,proj,flds,cnts,mdr);
   Element pxml = mdr.waitForXml();
   IvyLog.logD("BSTYLE",
         "RECEIVE from BUBBLES: " + IvyXml.convertXmlToString(pxml));

   return pxml;
}



private static void sendBubblesMessage(String cmd)
{
   sendBubblesMessage(cmd,null,null,null,null);
}


private static void sendBubblesMessage(String cmd,String proj,Map<String,Object> flds,String cnts)
{
   sendBubblesMessage(cmd,proj,flds,cnts,null);
}


private static void sendBubblesMessage(String cmd,String proj,Map<String,Object> flds,String cnts,
      MintReply rply)
{
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("BUBBLES");
   xw.field("DO",cmd);
   xw.field("BID",SOURCE_ID); 
   if (proj != null && proj.length() > 0) xw.field("PROJECT",proj);
   if (flds != null) {
      for (Map.Entry<String,Object> ent : flds.entrySet()) {
	 xw.field(ent.getKey(),ent.getValue());
       }
    }
   xw.field("LANG","eclipse");
   if (cnts != null) xw.xmlText(cnts);
   xw.end("BUBBLES");
   
   String xml = xw.toString();
   xw.close();
   
   IvyLog.logD("BSTYLE","SEND to BUBBLES: " + xml);
   
   int fgs = MINT_MSG_NO_REPLY;
   if (rply != null) fgs = MINT_MSG_FIRST_NON_NULL;
   mint_control.send(xml,rply,fgs);
}




/********************************************************************************/
/*										*/
/*	Bedrock setup / shutdown methods					*/
/*										*/
/********************************************************************************/

private static void setupBedrock(String dir,String mint,String proj)
{
   mint_control = MintControl.create(mint,MintSyncMode.ONLY_REPLIES);
   mint_control.register("<BEDROCK SOURCE='ECLIPSE' TYPE='_VAR_0' />",new TestEclipseHandler());
   
   System.err.println("SETTING UP BEDROCK");
   File ec1 = new File("/u/spr/eclipse-oxygenx/eclipse/eclipse");
   File ec2 = new File("/u/spr/Eclipse/" + dir);
   if (!ec1.exists()) {
      ec1 = new File("/vol/Developer/java-2023-06/Eclipse.app/Contents/MacOS/eclipse");
      ec2 = new File("/Users/spr/Eclipse/" + dir);
    }
   if (!ec1.exists()) {
      System.err.println("Can't find bubbles version of eclipse to run");
      System.exit(1);
    }
   
   String cmd = ec1.getAbsolutePath();
   cmd += " -application edu.brown.cs.bubbles.bedrock.application";
   cmd += " -data " + ec2.getAbsolutePath();
   cmd += " -bhide";
   cmd += " -nosplash";
   cmd += " -vmargs -Dedu.brown.cs.bubbles.MINT=" + mint;
   // cmd += " -Xdebug -Xrunjdwp:transport=dt_socket,address=32328,server=y,suspend=n";
   // cmd += " -Xmx16000m";
   
   System.err.println("RUN: " + cmd);
   
   try {
      for (int i = 0; i < 250; ++i) {
	 if (pingEclipse()) {
	    CommandArgs args = new CommandArgs("LEVEL","DEBUG");
	    sendBubblesMessage("LOGLEVEL",null,args,null);
	    sendBubblesMessage("ENTER");
	    Element pxml = sendBubblesXmlReply("OPENPROJECT",proj,null,null);
	    if (!IvyXml.isElement(pxml,"PROJECT")) pxml = IvyXml.getChild(pxml,"PROJECT");
	    return;
	  }
	 if (i == 0) new IvyExec(cmd);
	 else {
	    try { Thread.sleep(100); } catch (InterruptedException e) { }
	  }
       }
    }
   catch (IOException e) { }
   
   throw new Error("Problem running Eclipse: " + cmd);
}




private static class TestEclipseHandler implements MintHandler {
   
   @Override public void receive(MintMessage msg,MintArguments args) {
      String cmd = args.getArgument(0);
      switch (cmd) {
         case "PING" :
            msg.replyTo("<PONG/>");
            break;
         case "ELISION" :
         case "RESOURCE" :
            break;
         default :
            msg.replyTo();
            break;
       }
    }

}	// end of inner class TestEclipseHandler



private static void shutdownBedrock()
{
   System.err.println("SHUT DOWN BEDROCK");
   sendBubblesMessage("EXIT");
   mint_control = null;
}



private static boolean pingEclipse()
{
   MintDefaultReply mdr = new MintDefaultReply();
   sendBubblesMessage("PING",null,null,null,mdr);
   String r = mdr.waitForString(500);
   return r != null;
}




}       // end of class BstyleTest




/* end of BstyleTest.java */

