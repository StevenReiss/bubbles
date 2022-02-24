/********************************************************************************/
/*										*/
/*		BvcrMonitor.java						*/
/*										*/
/*	description of class							*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.bubbles.bvcr;

import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


class BvcrMonitor implements BvcrConstants, MintConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BvcrMain	bvcr_control;
private MintControl mint_control;
private boolean is_done;
private int	delay_count;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BvcrMonitor(BvcrMain bm,String mint)
{
   bvcr_control = bm;
   mint_control = MintControl.create(mint,MintSyncMode.ONLY_REPLIES);
   is_done = false;
   delay_count = 0;
}



/********************************************************************************/
/*										*/
/*	Server implementation							*/
/*										*/
/********************************************************************************/

void server()
{
   mint_control.register("<BEDROCK SOURCE='ECLIPSE' TYPE='_VAR_0' />",new EclipseHandler());
   mint_control.register("<BUBBLES DO='EXIT' />",new ExitHandler());
   mint_control.register("<BVCR DO='_VAR_0' />",new CommandHandler());

   synchronized (this) {
      while (!is_done || delay_count > 0) {
	 checkEclipse();
	 try {
	    wait(300000l);
	  }
	 catch (InterruptedException e) { }
       }
    }
}



private synchronized void serverDone()
{
   System.err.println("BVCR: Server done");

   is_done = true;
   notifyAll();
}



private void checkEclipse()
{
   MintDefaultReply rply = new MintDefaultReply();
   String msg = "<BUBBLES DO='PING' />";
   mint_control.send(msg,rply,MINT_MSG_FIRST_NON_NULL);
   String r = rply.waitForString(120000);
   if (r == null) {
      is_done = true;
      System.err.println("BVCR: Eclipse ping failed");
    }
}



synchronized boolean startDelay()
{
   if (is_done) return false;

   delay_count += 1;

   return true;
}


synchronized void endDelay()
{
   if (delay_count <= 0) return;
   delay_count -= 1;
   if (delay_count == 0) notifyAll();
}



/********************************************************************************/
/*										*/
/*	Project management							*/
/*										*/
/********************************************************************************/

void loadProjects()
{
   MintDefaultReply rply = new MintDefaultReply();

   String msg = "<BUBBLES DO='PROJECTS' />";
   mint_control.send(msg,rply,MINT_MSG_FIRST_NON_NULL);

   Element r = rply.waitForXml();

   if (!IvyXml.isElement(r,"RESULT")) {
      System.err.println("BVCR: Problem getting project information: " +
	    IvyXml.convertXmlToString(r));
      System.exit(2);
    }

   for (Element pe : IvyXml.children(r,"PROJECT")) {
      String pnm = IvyXml.getAttrString(pe,"NAME");
      MintDefaultReply prply = new MintDefaultReply();
      String pmsg = "<BUBBLES DO='OPENPROJECT' PROJECT='" + pnm +
      "' CLASSES='false' FILES='false' PATHS='true' OPTIONS='false' />";
      mint_control.send(pmsg,prply,MINT_MSG_FIRST_NON_NULL);
      Element pr = prply.waitForXml();
      if (!IvyXml.isElement(pr,"RESULT")) {
	 System.err.println("BVCR: Problem opening project " + pnm + ": " +
	       IvyXml.convertXmlToString(pr));
	 continue;
       }
      Element ppr = IvyXml.getChild(pr,"PROJECT");
      BvcrProject bp = new BvcrProject(ppr);
      if (bp.getSourceDirectory() == null) continue;
      bvcr_control.setProject(bp);
    }
}




/********************************************************************************/
/*										*/
/*	Basic command to find changes to a file 				*/
/*										*/
/********************************************************************************/

private void findChanges(String proj,String file,IvyXmlWriter xw)
{
   File f = new File(file);
   bvcr_control.findChanges(proj,f,xw);
}



private void handleFileChanged(String proj,String file)
{
   File f = new File(file);
   bvcr_control.handleFileChanged(proj,f);
}


private void handleFileError(String proj,String file,boolean err)
{ }



private void handleEndUpdate()
{
   bvcr_control.handleEndUpdate();
}



/********************************************************************************/
/*										*/
/*	Basic command to find history of a file 				*/
/*										*/
/********************************************************************************/

private void findHistory(String proj,String file,IvyXmlWriter xw)
{
   File f = null;
   if (file != null) f = new File(file);

   bvcr_control.findHistory(proj,f,xw);
}



/********************************************************************************/
/*										*/
/*	Basic command to find version differences				*/
/*										*/
/********************************************************************************/

private void findFileDiffs(String proj,String file,String vfr,String vto,IvyXmlWriter xw)
{
   File f = new File(file);
   bvcr_control.findFileDiffs(proj,f,vfr,vto,xw);
}


/********************************************************************************/
/*										*/
/*	List projects under version control					*/
/*										*/
/********************************************************************************/

private void handleListProjects(IvyXmlWriter xw)
{
   for (BvcrProject bp : bvcr_control.getProjects()) {
      BvcrVersionManager bvm = bvcr_control.getManager(bp.getName());
      if (bvm == null) continue;
      xw.begin("PROJECT");
      xw.field("NAME",bp.getName());
      xw.field("DIRECTORY",bp.getSourceDirectory());
      xw.field("REPONAME",bvm.getRepositoryName());
      xw.field("ROOT",bvm.getRootDirectory());
      xw.field("TYPE",bvm.getRepoType());
      xw.end("PROJECT");
    }
}



/********************************************************************************/
/*										*/
/*	List all versions associated with project				*/
/*										*/
/********************************************************************************/

private void handleListVersions(IvyXmlWriter xw,String proj)
{
   BvcrVersionManager bvm = bvcr_control.getManager(proj);
   if (bvm == null) return;
   bvm.findProjectHistory(xw);
}



/********************************************************************************/
/*										*/
/*	Show status of new/changed files in the project 			*/
/*										*/
/********************************************************************************/

private void handleShowChangedFiles(IvyXmlWriter xw,String proj,boolean ign)
{
   BvcrVersionManager bvm = bvcr_control.getManager(proj);
   if (bvm == null) return;
   bvm.showChangedFiles(xw,ign);
}



/********************************************************************************/
/*										*/
/*	Handle request to update a set of files 				*/
/*										*/
/********************************************************************************/

private void handleIgnoreFiles(IvyXmlWriter xw,String proj,List<String> files)
{
   BvcrVersionManager bvm = bvcr_control.getManager(proj);
   if (bvm == null) return;
   bvm.ignoreFiles(xw,files);
}



private void handleAddFiles(IvyXmlWriter xw,String proj,List<String> files)
{
   BvcrVersionManager bvm = bvcr_control.getManager(proj);
   if (bvm == null) return;
   bvm.addFiles(xw,files);
}


private void handleRemoveFiles(IvyXmlWriter xw,String proj,List<String> files)
{
   BvcrVersionManager bvm = bvcr_control.getManager(proj);
   if (bvm == null) return;
   bvm.removeFiles(xw,files);
}



private void handleCommit(IvyXmlWriter xw,String proj,String msg)
{
   BvcrVersionManager bvm = bvcr_control.getManager(proj);
   if (bvm == null) return;
   bvm.doCommit(xw,msg);
}




private void handlePush(IvyXmlWriter xw,String proj)
{
   BvcrVersionManager bvm = bvcr_control.getManager(proj);
   if (bvm == null) return;
   bvm.doPush(xw);
}



private void handleUpdate(IvyXmlWriter xw,String proj,boolean keep,boolean remove)
{
   BvcrVersionManager bvm = bvcr_control.getManager(proj);
   if (bvm == null) return;
   bvm.doUpdate(xw,keep,remove);
}



void handleSetVersion(IvyXmlWriter xw,String proj,String ver)
{
   BvcrVersionManager bvm = bvcr_control.getManager(proj);
   if (bvm == null) return;
   bvm.doSetVersion(xw,ver);
}


private void handleStash(IvyXmlWriter xw,String proj,String msg)
{
   BvcrVersionManager bvm = bvcr_control.getManager(proj);
   if (bvm == null) return;
   bvm.doStash(xw,msg);
}



/********************************************************************************/
/*										*/
/*	Handle messages from eclipse						*/
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
         else if (cmd.equals("FILEERROR")) {
            boolean haserrs = false;
            Element msgs = IvyXml.getChild(e,"MESSAGES");
            if (msgs != null) {
               for (Element pm : IvyXml.children(msgs,"PROBLEM")) {
        	  if (IvyXml.getAttrBool(pm,"ERROR")) haserrs = true;
        	}
             }
            String proj = IvyXml.getAttrString(e,"PROJECT");
            synchronized (this) {
               handleFileError(proj,IvyXml.getAttrString(e,"FILE"),haserrs);
             }
          }
         else if (cmd.equals("LAUNCHCONFIGEVENT")) {
            // handle changes to saved launch configurations
          }
         else if (cmd.equals("RESOURCE")) {
            synchronized (this) {
               for (Element re : IvyXml.children(e,"DELTA")) {
        	  String rtyp = IvyXml.getAttrString(re,"TYPE");
        	  if (rtyp != null && rtyp.equals("FILE")) {
        	     String fp = IvyXml.getAttrString(re,"LOCATION");
        	     String proj = IvyXml.getAttrString(re,"PROJECT");
        	     handleFileChanged(proj,fp);
        	   }
        	}
               handleEndUpdate();
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
         System.err.println("BVCR: Problem processing Eclipse command: " + t);
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

      System.err.println("BVCR: RECEIVED COMMAND " + cmd + ": " + msg.getText());

      try {
	 if (cmd == null) return;
	 IvyXmlWriter xw = new IvyXmlWriter();
	 switch (cmd) {
	    case "FINDCHANGES" :
	       synchronized (this) {
		  String proj = IvyXml.getAttrString(e,"PROJECT");
		  String file = IvyXml.getAttrString(e,"FILE");
		  findChanges(proj,file,xw);
		}
	       break;
	    case "HISTORY" :
	       synchronized (this) {
		  String proj = IvyXml.getAttrString(e,"PROJECT");
		  String file = IvyXml.getAttrString(e,"FILE");
		  findHistory(proj,file,xw);
		}
	       break;
	    case "FILEDIFFS" :
	       synchronized (this) {
		  String proj = IvyXml.getAttrString(e,"PROJECT");
		  String file = IvyXml.getAttrString(e,"FILE");
		  String vfrom = IvyXml.getAttrString(e,"FROM");
		  String vto = IvyXml.getAttrString(e,"TO");
		  findFileDiffs(proj,file,vfrom,vto,xw);
		}
	       break;
	    case "PROJECTS" :
	       synchronized (this) {
		  handleListProjects(xw);
		}
	       break;
	    case "CHANGES" :
	       synchronized (this) {
		  String proj = IvyXml.getAttrString(e,"PROJECT");
		  boolean ign = IvyXml.getAttrBool(e,"IGNORED");
		  handleShowChangedFiles(xw,proj,ign);
		}
	       break;
	    case "FILEIGNORE" :
	       synchronized (this) {
		  String proj = IvyXml.getAttrString(e,"PROJECT");
		  List<String> files = new ArrayList<String>();
		  for (Element fe : IvyXml.children(e,"FILE")) {
		     String s = IvyXml.getText(fe);
		     if (s == null || s.trim().length() == 0) {
			s = IvyXml.getAttrString(fe,"NAME");
		      }
		     if (s != null) files.add(s);
		   }
		  handleIgnoreFiles(xw,proj,files);
		}
	       break;
	    case "FILEADD" :
	       synchronized (this) {
		  String proj = IvyXml.getAttrString(e,"PROJECT");
		  List<String> files = new ArrayList<String>();
		  for (Element fe : IvyXml.children(e,"FILE")) {
		     String s = IvyXml.getText(e);
		     if (s == null || s.length() <= 0) {
			s = IvyXml.getAttrString(fe,"NAME");
		      }
		     if (s != null) files.add(s);
		   }
		  handleAddFiles(xw,proj,files);
		}
	       break;
	    case "FILERM" :
	       synchronized (this) {
		  String proj = IvyXml.getAttrString(e,"PROJECT");
		  List<String> files = new ArrayList<String>();
		  for (Element fe : IvyXml.children(e,"FILE")) {
		     String s = IvyXml.getText(e);
		     if (s == null || s.length() <= 0) {
			s = IvyXml.getAttrString(fe,"NAME");
		      }
		     if (s != null) files.add(s);
		   }
		  handleRemoveFiles(xw,proj,files);
		}
	       break;
	    case "VERSIONS" :
	       synchronized (this) {
		  String proj = IvyXml.getAttrString(e,"PROJECT");
		  handleListVersions(xw,proj);
		}
	       break;
	    case "COMMIT" :
	       synchronized (this) {
		  String proj = IvyXml.getAttrString(e,"PROJECT");
		  String cmsg = IvyXml.getAttrString(e,"MESSAGE");
		  handleCommit(xw,proj,cmsg);
		}
	       break;
	    case "STASH" :
	       synchronized (this) {
		  String proj = IvyXml.getAttrString(e,"PROJECT");
		  String smsg = IvyXml.getAttrString(e,"MESSAGE");
		  handleStash(xw,proj,smsg);
		}
	       break;
	    case "PUSH" :
	       synchronized (this) {
		  String proj = IvyXml.getAttrString(e,"PROJECT");
		  handlePush(xw,proj);
		}
	       break;
	    case "UPDATE" :
	       synchronized (this) {
		  String proj = IvyXml.getAttrString(e,"PROJECT");
		  boolean keep = IvyXml.getAttrBool(e,"KEEP");
		  boolean remove = IvyXml.getAttrBool(e,"DISCARD");
		  handleUpdate(xw,proj,keep,remove);
		}
	       break;
	    case "SETVERSION" :
	       synchronized (this) {
		  String proj = IvyXml.getAttrString(e,"PROJECT");
		  String name = IvyXml.getAttrString(e,"VERSION");
		  handleStash(xw,proj,name);
		}
	       break;
	    case "PING" :
	       rply = "PONG";
	       break;
	    case "EXIT" :
	       xw.close();
	       xw = null;
	       serverDone();
	       break;
	  }
	 if (rply == null && xw != null) rply = xw.toString();
       }
      catch (Throwable t) {
	 System.err.println("BVCR: Problem processing BVCR command: " + t);
	 t.printStackTrace();
       }

      if (rply != null) {
	 rply = "<RESULT>" + rply + "</RESULT>";
       }

      System.err.println("BVCR: RESULT: " + rply);

      msg.replyTo(rply);
    }

}	// end of inner class CommandHandler




}	// end of class BvcrMonitor




/* end of BvcrMonitor.java */

