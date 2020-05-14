/********************************************************************************/
/*										*/
/*		BseanFactory.java						*/
/*										*/
/*	Factory for setting up and interface semantic analysis with bubbles	*/
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



package edu.brown.cs.bubbles.bsean;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.w3c.dom.Element;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaConstants.BudaWorkingSet;
import edu.brown.cs.bubbles.buda.BudaConstants.BubbleViewCallback;
import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.exec.IvyExecQuery;
import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

public class BseanFactory implements BseanConstants, MintConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private boolean server_running;
private boolean server_started;
private Map<String,BseanSession> session_map;
private Set<File> open_files;


private static BseanFactory the_factory = new BseanFactory();



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

public static void setup()
{ }



public static void initialize(BudaRoot br)
{
   switch (BoardSetup.getSetup().getLanguage()) {
      case JS :
      case PYTHON :
      case REBUS :
	 return;
      case JAVA :
	 break;
    }

   BudaRoot.addBubbleViewCallback(new ViewListener());

   BudaRoot.registerMenuButton("Bubble.Start Semantic Analysis",new StartAction());

   // BseanFactory fac = getFactory();
   // fac.start();
}


public static BseanFactory getFactory()
{
   return the_factory;
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BseanFactory()
{
   server_running = false;
   server_started = false;
   session_map = new HashMap<>();
   open_files = new HashSet<>();

   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   mc.register("<FAITEXEC TYPE='_VAR_0' />",new UpdateHandler());
}


/********************************************************************************/
/*										*/
/*	Starting methods							*/
/*										*/
/********************************************************************************/

private void start()
{
   if (!server_running) server_started = false; 		// for debug
   startFait();
   if (!server_running) return;

   try {
      BseanSession s = new BseanSession();
      session_map.put(s.getSessionId(),s);
      s.begin();
      for (File f : open_files) {
	 s.handleEditorAdded(f);
       }
      s.startAnalysis();
    }
   catch (BseanException e) { }
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/


private void handleEditorAdded(BudaBubble bb)
{
   File f = bb.getContentFile();
   if (f == null) return;
   if (!open_files.add(f)) return;

   for (BseanSession ss : session_map.values()) {
      ss.handleEditorAdded(f);
    }
}




/********************************************************************************/
/*										*/
/*	Fait Server communication						*/
/*										*/
/********************************************************************************/

Element sendFaitMessage(String id,String cmd,CommandArgs args,String cnts)
{
   if (!server_running) return null;

   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();

   MintDefaultReply rply = new MintDefaultReply();
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("FAIT");
   xw.field("DO",cmd);
   xw.field("SID",id);
   if (args != null) {
      for (Map.Entry<String,Object> ent : args.entrySet()) {
	 xw.field(ent.getKey(),ent.getValue());
       }
    }
   if (cnts != null) {
      xw.xmlText(cnts);
    }
   xw.end("FAIT");
   String msg = xw.toString();
   xw.close();

   BoardLog.logD("BSEAN","Send to FAIT: " + msg);

   mc.send(msg,rply,MINT_MSG_FIRST_NON_NULL);

   Element rslt = rply.waitForXml(60000);

   BoardLog.logD("BSEAN","Reply from FAIT: " + IvyXml.convertXmlToString(rslt));
   if (rslt == null && (cmd.equals("START") || cmd.equals("BEGIN"))) {
      MintDefaultReply prply = new MintDefaultReply();
      mc.send("<FAIT DO='PING' SID='*' />",rply,MINT_MSG_FIRST_NON_NULL);
      String prslt = prply.waitForString(3000);
      if (prslt == null) {
	 server_running = false;
	 server_started = false;
	 startFait();
	 rply = new MintDefaultReply();
	 mc.send(msg,rply,MINT_MSG_FIRST_NON_NULL);
	 rslt = rply.waitForXml(0);
       }
    }

   return rslt;
}




private void startFait()
{
   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   IvyExec exec = null;
   File wd = new File(bs.getDefaultWorkspace());
   File logf = new File(wd,"fait.log");

   if (server_running || server_started) return;

   BoardProperties bp = BoardProperties.getProperties("Bsean");
   String dbgargs = bp.getProperty("Bsean.jvm.args");
   List<String> args = new ArrayList<String>();
   args.add(IvyExecQuery.getJavaPath());

   if (dbgargs != null) {
      StringTokenizer tok = new StringTokenizer(dbgargs);
      while (tok.hasMoreTokens()) {
	 args.add(tok.nextToken());
       }
    }

   args.add("-cp");
   String xcp = bp.getProperty("Bsean.fait.class.path");
   if (xcp == null) {
      xcp = System.getProperty("java.class.path");
      String ycp = bp.getProperty("Bsean.fait.add.path");
      if (ycp != null) xcp = ycp + File.pathSeparator + xcp;
    }
   else {
      BoardSetup setup = BoardSetup.getSetup();
      StringBuffer buf = new StringBuffer();
      StringTokenizer tok = new StringTokenizer(xcp,":;");
      while (tok.hasMoreTokens()) {
	 String elt = tok.nextToken();
	 if (!elt.startsWith("/") &&  !elt.startsWith("\\")) {
	    if (elt.equals("eclipsejar")) {
	       String ejp = setup.getLibraryPath(elt);
	       File ejr = new File(ejp);
	       if (ejr.exists() && ejr.isDirectory()) {
		  for (File nfil : ejr.listFiles()) {
		     if (nfil.getName().startsWith("org.eclipse.") && nfil.getName().endsWith(".jar")) {
			if (buf.length() > 0) buf.append(File.pathSeparator);
			buf.append(nfil.getPath());
		      }
		   }
		}
	       continue;
	     }
	    else {
	       elt = setup.getLibraryPath(elt);
	     }
	  }
	 if (buf.length() > 0) buf.append(File.pathSeparator);
	 buf.append(elt);
       }
      xcp = buf.toString();
    }

   args.add(xcp);
   args.add("edu.brown.cs.fait.iface.FaitMain");
   args.add("-m");
   args.add(bs.getMintName());
   args.add("-L");
   args.add(logf.getPath());
   if (bp.getBoolean("Bsean.fait.debug")) args.add("-D");
   if (bp.getBoolean("Bsean.fait.trace")) args.add("-T");

   synchronized (this) {
      if (server_started || server_running) return;
      server_started = true;
    }

   for (int i = 0; i < 25; ++i) {
      MintDefaultReply rply = new MintDefaultReply();
      mc.send("<FAIT DO='PING' SID='*' />",rply,MINT_MSG_FIRST_NON_NULL);
      String rslt = rply.waitForString(1000);
      if (rslt != null) {
	 server_running = true;
	 break;
       }
      if (i == 0) {
	 try {
	    exec = new IvyExec(args,null,IvyExec.ERROR_OUTPUT);     // make IGNORE_OUTPUT to clean up otuput
	    BoardLog.logD("BSEAN","Run " + exec.getCommand());
	  }
	 catch (IOException e) {
	    break;
	  }
       }
      else {
	 try {
	    if (exec != null) {
	       exec.exitValue();
	       break;
	     }
	  }
	 catch (IllegalThreadStateException e) { }
       }

      try {
	 Thread.sleep(2000);
       }
      catch (InterruptedException e) { }
    }
   if (!server_running) {
      BoardLog.logE("BSEAN","Unable to start fait server: " + args);
    }
}




/********************************************************************************/
/*										*/
/*	Message handling							*/
/*										*/
/********************************************************************************/

private class UpdateHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      String type = args.getArgument(0);
      // String id = args.getArgument(1);
      // Element xml = msg.getXml();
      String rslt = null;
      try {
	 switch (type) {
	    default :
	       BoardLog.logE("BSEAN","Unknown command " + type + " from Fait");
	       break;
	    case "ERROR" :
	       throw new BseanException("Bad command");
	  }
       }
      catch (BseanException e) {
	 BoardLog.logE("BSEAN","Error processing command",e);
       }
      msg.replyTo(rslt);
    }

}	// end of inner class UpdateHandler



/********************************************************************************/
/*										*/
/*	Monitor the current display						*/
/*										*/
/********************************************************************************/


private static class ViewListener implements BubbleViewCallback {

   @Override public void doneConfiguration()				{ }
   @Override public void focusChanged(BudaBubble bb,boolean set)	{ }
   @Override public void bubbleRemoved(BudaBubble bb)			{ }
   @Override public boolean bubbleActionDone(BudaBubble bb)		{ return false; }
   @Override public void workingSetAdded(BudaWorkingSet ws)		{ }
   @Override public void workingSetRemoved(BudaWorkingSet ws)		{ }
   @Override public void copyFromTo(BudaBubble f,BudaBubble t)		{ }

   @Override public void bubbleAdded(BudaBubble bb) {
      File f = bb.getContentFile();
      if (f == null) return;
      BseanFactory.getFactory().handleEditorAdded(bb);
    }

}


/********************************************************************************/
/*										*/
/*	Handle starting 						       */
/*										*/
/********************************************************************************/

private static class StartAction implements BudaConstants.ButtonListener {

   @Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt) {
      BseanFactory fac = getFactory();
      fac.start();
    }

}	// end of inner class StartAction




}	// end of class BseanFactory




/* end of BseanFactory.java */

