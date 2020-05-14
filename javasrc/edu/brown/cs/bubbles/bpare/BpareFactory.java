/********************************************************************************/
/*										*/
/*		BpareFactory.java						*/
/*										*/
/*	Factory for Bubbles pattern-assisted recommendation engine		*/
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



package edu.brown.cs.bubbles.bpare;

import edu.brown.cs.bubbles.bale.BaleConstants;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleContextConfig;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleFileOverview;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleWindow;
import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaRoot;

import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.exec.IvyExecQuery;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.text.BadLocationException;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;



public class BpareFactory implements BpareConstants, MintConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private boolean 	server_running;

private static BpareFactory	the_factory = null;

private static String []	eclipse_jars = new String [] {
   "org.eclipse.jdt.core.jar",
   "org.eclipse.osgi.jar",
   "org.eclipse.core.contenttype.jar",
   "org.eclipse.equinox.preferences.jar",
   "org.eclipse.core.jobs.jar",
   "org.eclipse.core.runtime.jar",
   "org.eclipse.equinox.common.jar",
   "org.eclipse.jface.jar",
   "org.eclipse.jface.text.jar",
   "org.eclipse.text.jar",
   "org.eclipse.core.resources.jar",
};




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public synchronized static BpareFactory getFactory()
{
   if (the_factory == null) the_factory = new BpareFactory();
   return the_factory;
}



private BpareFactory()
{
   server_running = false;
}



/********************************************************************************/
/*										*/
/*	Setup methods (called by BEAM)						*/
/*										*/
/********************************************************************************/

public static  void setup()
{
   // work is done by the static initializer
}



public static void initialize(BudaRoot br)
{
   getFactory().startBpareServer();

   BaleFactory.getFactory().addContextListener(new BpareContexter());
}




/********************************************************************************/
/*										*/
/*	Methods to get suggestions from the server				*/
/*										*/
/********************************************************************************/

Element getSuggestions(String proj,File file,int spos,int epos)
{
   if (!server_running) return null;

   BaleFileOverview fov = BaleFactory.getFactory().getFileOverview(proj,file);
   String txt = null;
   try {
      txt = fov.getText(0,fov.getLength());
    }
   catch (BadLocationException e) {
      return null;
    }

   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   MintDefaultReply rply = new MintDefaultReply();
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("BPARE");
   xw.field("DO","SUGGEST");
   xw.field("NAME",file.getPath());
   xw.field("PROJECT",proj);
   xw.field("OFFSET",spos);
   xw.field("LENGTH",epos-spos+1);
   xw.bytesElement("FILE",txt.getBytes());
   xw.end("BPARE");
   mc.send(xw.toString(),rply,MINT_MSG_FIRST_NON_NULL);
   xw.close();
   Element e = rply.waitForXml();

   // probably should do something with e here

   return e;
}



/********************************************************************************/
/*										*/
/*	Server code								*/
/*										*/
/********************************************************************************/

void startBpareServer()
{
   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   IvyExec exec = null;

   synchronized (this) {
      if (server_running) return;

      long mxmem = Runtime.getRuntime().maxMemory();
      mxmem = Math.min(2*1024*1024*1024L,mxmem);

      StringBuffer buf = new StringBuffer();
      buf.append(System.getProperty("java.class.path"));
      for (String s : eclipse_jars) {
	 String lp = BoardSetup.getSetup().getLibraryPath(s);
	 buf.append(File.pathSeparator);
	 buf.append(lp);
       }

      List<String> args = new ArrayList<String>();
      args.add(IvyExecQuery.getJavaPath());
      args.add("-Xmx" + Long.toString(mxmem));
      args.add("-cp");
      args.add(buf.toString());
      args.add("edu.brown.cs.bubbles.bpare.BpareMain");
      args.add("-S");
      args.add("-t");
      args.add("-m");
      args.add(bs.getMintName());

      for (int i = 0; i < 100; ++i) {
	 MintDefaultReply rply = new MintDefaultReply();
	 mc.send("<BPARE DO='PING' />",rply,MINT_MSG_FIRST_NON_NULL);
	 String rslt = rply.waitForString(1000);
	 if (rslt != null) {
	    server_running = true;
	    break;
	  }
	 if (i == 0) {
	    try {
	       exec = new IvyExec(args,null,IvyExec.ERROR_OUTPUT);
	       BoardLog.logD("BPARE","Run " + exec.getCommand());
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
	 BoardLog.logE("BPARE","Unable to start bpare server");
	 server_running = true; 	// don't try again
       }
    }
}



/********************************************************************************/
/*										*/
/*	Context listener							*/
/*										*/
/********************************************************************************/

private static class BpareContexter implements BaleConstants.BaleContextListener {

   @Override public BudaBubble getHoverBubble(BaleContextConfig cfg) {
      return null;
    }

   @Override public void addPopupMenuItems(BaleContextConfig cfg,JPopupMenu menu) {
      if (cfg.inAnnotationArea()) return;

      menu.add(new BpareAction(cfg));
    }

   @Override public String getToolTipHtml(BaleContextConfig cfg) {
      return null;
    }

   @Override public void noteEditorAdded(BaleWindow win)       { }
   @Override public void noteEditorRemoved(BaleWindow win)     { }

}	// end of inner class BpareContexter


private static class BpareAction extends AbstractAction {

   private String project_name;
   private File for_file;
   private int file_position;

   BpareAction(BaleContextConfig cfg) {
      super("Show Possible Suggestions");
      project_name = cfg.getEditor().getContentProject();
      for_file = cfg.getEditor().getContentFile();
      file_position = cfg.getDocumentOffset();
    }

   @Override public void actionPerformed(ActionEvent e) {
      Element rslt = the_factory.getSuggestions(project_name,for_file,file_position,file_position);
      System.err.println("BPARE: Suggest: " + IvyXml.convertXmlToString(rslt));
    }

}	// end of inner class BpareAction


}	// end of class BpareFactory




/* end of BpareFactory.java */

