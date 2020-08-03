/********************************************************************************/
/*										*'/
/*		BpareMonitor.java						*/
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



package edu.brown.cs.bubbles.bpare;

import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.xml.IvyXml;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.w3c.dom.Element;


class BpareMonitor implements BpareConstants, MintConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BpareMain	bpare_control;
private MintControl	mint_control;
private boolean 	is_done;
private TrieCreator	trie_creator;
private ProjectWaiter	project_waiter;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BpareMonitor(BpareMain bm,String mint)
{
   bpare_control = bm;
   mint_control = MintControl.create(mint,MintSyncMode.ONLY_REPLIES);
   is_done = false;
   trie_creator = null;
   project_waiter = new ProjectWaiter();
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
      System.err.println("BPARE: Problem getting project information: " +
	    IvyXml.convertXmlToString(r));
      System.exit(2);
    }

   for (Element pe : IvyXml.children(r,"PROJECT")) {
      String pnm = IvyXml.getAttrString(pe,"NAME");
      MintDefaultReply prply = new MintDefaultReply();
      String pmsg = "<BUBBLES DO='OPENPROJECT' PROJECT='" + pnm +
	 "' CLASSES='false' FILES='true' PATHS='false' OPTIONS='false' BACKGROUND='BPARE' />";
      mint_control.send(pmsg,prply,MINT_MSG_FIRST_NON_NULL);
      project_waiter.addProject(pnm);
    }

   project_waiter.waitForAll();
}



private void handleProjectData(Element pe)
{
   String pnm = IvyXml.getAttrString(pe,"NAME");

   BpareProject bp = new BpareProject(pe);
   if (bp.isValid()) bpare_control.addProject(bp);

   project_waiter.noteProject(pnm);
}


private class ProjectWaiter {

   private int num_projects;

   ProjectWaiter() {
      num_projects = 0;
    }

   synchronized void addProject(String nm) {
      ++num_projects;
    }

   synchronized void noteProject(String nm) {
      --num_projects;
      if (num_projects == 0) notifyAll();
    }

   synchronized void waitForAll() {
      while (num_projects > 0 && !is_done) {
	 try {
	    wait(60000);
	  }
	 catch (InterruptedException e) { }
       }
    }

}	// end if inner class ProjectWaiter




/********************************************************************************/
/*										*/
/*	Request Processing							*/
/*										*/
/********************************************************************************/

private String getSuggestion(String proj,String stmts) throws BpareException
{
   BpareProject bp = bpare_control.getProject(proj);
   if (bp == null) throw new BpareException("Project " + proj + " not defined");

   ASTNode n = BpareBuilder.getStatementsAst(stmts);
   System.err.println("BPARE: Handle suggestsions for: " + n);
   return null;
}




private String getSuggestion(String proj,String cnts,int offset) throws BpareException
{
   if (trie_creator != null) trie_creator.waitForDone();

   BpareProject bp = bpare_control.getProject(proj);
   if (bp == null) throw new BpareException("Project " + proj + " not defined");

   ASTNode cu = BpareBuilder.getCompilationUnitAst(cnts);

   LocationFinder lf = new LocationFinder(ASTNode.BLOCK,offset);
   cu.accept(lf);
   ASTNode n = lf.getResult();
   if (n == null) return null;

   BpareTrie bt = bp.getTrie();
   bt.match(n);

   return null;
}



private static class LocationFinder extends ASTVisitor {

   private int find_offset;
   private int node_type;
   private ASTNode best_node;

   LocationFinder(int type,int offset) {
      find_offset = offset;
      node_type = type;
      best_node = null;
    }

   ASTNode getResult()				{ return best_node; }

   @Override public boolean preVisit2(ASTNode n) {
      int spos = n.getStartPosition();
      int epos = n.getLength() + spos;
      if (find_offset < spos || find_offset > epos) return false;
      if (n.getNodeType() == node_type || node_type == 0) best_node = n;

      return true;
    }

}	// end of inner class LocationFinder




/********************************************************************************/
/*										*/
/*	Server implementation							*/
/*										*/
/********************************************************************************/

void server()
{
   mint_control.register("<BEDROCK SOURCE='ECLIPSE' TYPE='_VAR_0' />",new EclipseHandler());
   mint_control.register("<BUBBLES DO='EXIT' />",new ExitHandler());
   mint_control.register("<BPARE DO='_VAR_0' />",new CommandHandler());

   loadProjects();

   switch (bpare_control.getPatternType()) {
      case TRIE :
	 trie_creator = new TrieCreator();
	 trie_creator.start();
	 break;
      default:
	 break;
    }

   synchronized (this) {
      while (!is_done) {
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
   is_done = true;
   notifyAll();
}



private void checkEclipse()
{
   MintDefaultReply rply = new MintDefaultReply();
   String msg = "<BUBBLES DO='PING' />";
   mint_control.send(msg,rply,MINT_MSG_FIRST_NON_NULL);
   String r = rply.waitForString(30000);
   if (r == null) is_done = true;
}




private class ExitHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      serverDone();
    }

}	// end of inner class ExitHandler



private class EclipseHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      String cmd = args.getArgument(0);

      try {
	 if (cmd == null) return;
	 else if (cmd.equals("PING")) {
	    msg.replyTo();			// we don't count for eclipse
	  }
	 else if (cmd.equals("STOP")) {
	    serverDone();
	  }
	 else if (cmd.equals("PROJECTDATA")) {
	    Element xml = msg.getXml();
	    for (Element pe : IvyXml.children(xml,"PROJECT")) {
	       handleProjectData(pe);
	     }
	  }
	 else {
	    msg.replyTo();
	  }
       }
      catch (Throwable t) {
	 System.err.println("BPARE: Problem processing Eclipse command: " + t);
	 t.printStackTrace();
       }
    }

}	// end of inner class EclipseHandler



/********************************************************************************/
/*										*/
/*	Handle command requests from bubbles or elsewhere			*/
/*										*/
/********************************************************************************/

private class CommandHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      String cmd = args.getArgument(0);
      String rply = null;
   
      System.err.println("BPARE: RECEIVED COMMAND " + cmd + ": " + msg.getText());
   
      try {
         if (cmd == null) return;
         else if (cmd.equals("PING")) {
            rply = "PONG";
          }
         else if (cmd.equals("EXIT")) {
            serverDone();
          }
         else if (cmd.equals("SUGGEST")) {
            Element xml = msg.getXml();
            String proj = IvyXml.getTextElement(xml,"PROJECT");
            String stmts = IvyXml.getTextElement(xml,"TEXT");
            if (stmts == null) {
               byte [] bts = IvyXml.getBytesElement(xml,"FILE");
               String cnts = new String(bts);
               int offset = IvyXml.getAttrInt(xml,"OFFSET");
               rply = getSuggestion(proj,cnts,offset);
             }
            else {
               rply = getSuggestion(proj,stmts);
             }
          }
       }
      catch (Throwable t) {
         System.err.println("BPARE: Problem processing BPARE command: " + t);
         t.printStackTrace();
       }
   
      if (rply != null) {
         rply = "<RESULT>" + rply + "</RESULT>";
       }
   
      msg.replyTo(rply);
    }

}	// end of inner class CommandHandler



/********************************************************************************/
/*										*/
/*	Background trie creator 						*/
/*										*/
/********************************************************************************/

private class TrieCreator extends Thread {

   private boolean is_finished;

   TrieCreator() {
      super("Create Tries");
      is_finished = false;
    }

   @Override public void run() {
      for (BpareProject bp : bpare_control.getProjects()) {
	 BpareBuilder bb = new BpareBuilder(bp,bpare_control.getPatternType());
	 bb.process();
       }
      synchronized (this) {
	 is_finished = true;
	 notifyAll();
       }
    }

   synchronized void waitForDone() {
      while (!is_finished) {
	 try {
	    wait(2000);
	  }
	 catch (InterruptedException e) { }
       }
    }

}	// end of innter class TrieCreator






}	// end of class BpareMonitor




/* end of BpareMonitor.java */

