/********************************************************************************/
/*										*/
/*		BanalMonitor.java						*/
/*										*/
/*	Monitor for stand-alone Banal implementation				*/
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



package edu.brown.cs.bubbles.banal;

import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;



class BanalMonitor implements BanalConstants, MintConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private MintControl	mint_control;
private BanalProjectManager project_manager;
private boolean 	is_done;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BanalMonitor(BanalMain bm,String mint)
{
   mint_control = MintControl.create(mint,MintSyncMode.ONLY_REPLIES);
   project_manager = new BanalProjectManager(this);
}


/********************************************************************************/
/*										*/
/*	Server setup and run							*/
/*										*/
/********************************************************************************/

void server()
{
   mint_control.register("<BEDROCK SOURCE='ECLIPSE' TYPE='_VAR_0' />",new EclipseHandler());
   mint_control.register("<BUBBLES DO='EXIT' />",new ExitHandler());
   mint_control.register("<BANAL DO='_VAR_0' />",new CommandHandler());

   synchronized (this) {
      while (!is_done) {
	 if (!checkEclipse()) break;
	 try {
	    wait(300000L);
	  }
	 catch (InterruptedException e) { }
       }
    }
}



private synchronized void serverDone()
{
   is_done = true;
   notifyAll();
}



/********************************************************************************/
/*										*/
/*	Check to exit if eclipse goes away					*/
/*										*/
/********************************************************************************/

private boolean checkEclipse()
{
   MintDefaultReply rply = new MintDefaultReply();
   String msg = "<BUBBLES DO='PING' />";
   mint_control.send(msg,rply,MINT_MSG_FIRST_NON_NULL);
   String r = rply.waitForString(30000);
   if (r == null) System.err.println("BANAL: No ping response from Eclipse");
   if (r == null) return false;
   return true;
}



/********************************************************************************/
/*										*/
/*	Compute the package graph						*/
/*										*/
/********************************************************************************/

private String computePackageGraph(String proj,String pkg,boolean mthds,boolean samecls)
{
   BanalPackageGraph pg = new BanalPackageGraph(proj,pkg,mthds,samecls);
   BanalStaticLoader bsl = new BanalStaticLoader(project_manager,pg);
   bsl.process();

   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("GRAPH");
   xw.field("PACKAGE",pkg);
   xw.field("PROJECT",proj);
   xw.field("METHODS",mthds);
   for (BanalPackageNode pn : pg.getAllNodes()) {
      pn.outputXml(xw);
    }
   xw.end("GRAPH");

   return xw.toString();
}



/********************************************************************************/
/*										*/
/*	Compute the package hierarchy						*/
/*										*/
/********************************************************************************/

private String computePackageHierarchy(String proj)
{
   BanalPackageHierarchy pg = new BanalPackageHierarchy(project_manager,proj);
   BanalStaticLoader bsl = new BanalStaticLoader(project_manager,pg);
   bsl.process();

   IvyXmlWriter xw = new IvyXmlWriter();
   pg.output(xw);

   return xw.toString();
}





/********************************************************************************/
/*										*/
/*	Eclipse access methods							*/
/*										*/
/********************************************************************************/

Element getAllProjects()
{
   MintDefaultReply rply = new MintDefaultReply();
   String msg = "<BUBBLES DO='PROJECTS' />";
   mint_control.send(msg,rply,MINT_MSG_FIRST_NON_NULL);

   Element r = rply.waitForXml();

   if (!IvyXml.isElement(r,"RESULT")) return null;

   return r;
}



Element openProject(String proj)
{
   MintDefaultReply rply = new MintDefaultReply();
   String msg = "<BUBBLES DO='OPENPROJECT' PROJECT='" + proj + "'";
   msg += " CLASSES='true' FILES='false' PATHS='false' OPTIONS='false' />";
   mint_control.send(msg,rply,MINT_MSG_FIRST_NON_NULL);

   Element r = rply.waitForXml();

   if (!IvyXml.isElement(r,"RESULT")) return null;

   Element xml = IvyXml.getChild(r,"PROJECT");

   return xml;
}




/********************************************************************************/
/*										*/
/*	Handle messages from Eclipse						*/
/*										*/
/********************************************************************************/

private class EclipseHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      String cmd = args.getArgument(0);
      Element e = msg.getXml();
   
      try {
         if (cmd == null) return;
         else if (cmd.equals("EDIT")) {
            msg.replyTo();
          }
         else if (cmd.equals("RESOURCE")) {
            for (Element re : IvyXml.children(e,"DELTA")) {
               String rtyp = IvyXml.getAttrString(re,"TYPE");
               if (rtyp != null && rtyp.equals("FILE")) {
        	  String proj = IvyXml.getAttrString(re,"PROJECT");
        	  project_manager.invalidate(proj);
        	}
             }
          }
         else if (cmd.equals("PING")) {
            msg.replyTo();			// we don't count for eclipse
          }
         else if (cmd.equals("STOP")) {
            serverDone();
          }
         else {
            msg.replyTo();
          }
       }
      catch (Throwable t) {
         System.err.println("BANAL: Problem processing Eclipse command: " + t);
         t.printStackTrace();
       }
    }

}	// end of inner class EclipseHandler



private class ExitHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      serverDone();
    }

}	// end of inner class ExitHandler




/********************************************************************************/
/*										*/
/*	Handle command requests from bubbles or elsewhere			*/
/*										*/
/********************************************************************************/

private class CommandHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      String cmd = args.getArgument(0);
      Element e = msg.getXml();
      String rply = null;
   
      System.err.println("BANAL: RECEIVED COMMAND " + cmd + ": " + msg.getText());
   
      try {
         if (cmd == null) return;
         else if (cmd.equals("PACKAGEGRAPH")) {
            String proj = IvyXml.getAttrString(e,"PROJECT");
            String pkg = IvyXml.getAttrString(e,"PACKAGE");
            boolean mthds = IvyXml.getAttrBool(e,"METHODS");
            boolean samec = IvyXml.getAttrBool(e,"SAMECLASS");
            rply = computePackageGraph(proj,pkg,mthds,samec);
          }
         else if (cmd.equals("PACKAGEHIERARCHY")) {
            String proj = IvyXml.getAttrString(e,"PROJECT");
            rply = computePackageHierarchy(proj);
          }
         else if (cmd.equals("PING")) {
            rply = "PONG";
          }
         else if (cmd.equals("EXIT")) {
            serverDone();
          }
       }
      catch (Throwable t) {
         System.err.println("BANAL: Problem processing BANALcommand: " + t);
         t.printStackTrace();
       }
   
      if (rply != null) {
         rply = "<RESULT>" + rply + "</RESULT>";
       }
   
      msg.replyTo(rply);
    }

}	// end of inner class CommandHandler




}	// end of class BanalMonitor




/* end of BanalMonitor.java */
