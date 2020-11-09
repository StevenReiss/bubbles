/********************************************************************************/
/*										*/
/*		BattMonitor.java						*/
/*										*/
/*	Bubble Automated Testing Tool monitor for talking to other tools	*/
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


package edu.brown.cs.bubbles.batt;


import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;




class BattMonitor implements BattConstants, MintConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BattMain		for_batt;
private MintControl		mint_control;
private Map<String,BattProject> project_set;
private List<BattLaunchConfig>	junit_launches;
private boolean 		is_done;
private Map<String,FileState>	file_states;
private Map<String,FileState>	changed_classes;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BattMonitor(BattMain bm,String mint)
{
   for_batt = bm;
   mint_control = MintControl.create(mint,MintSyncMode.ONLY_REPLIES);
   project_set = new HashMap<String,BattProject>();
   junit_launches = new ArrayList<BattLaunchConfig>();
   file_states = new HashMap<String,FileState>();
   changed_classes = new HashMap<String,FileState>();
   is_done = false;

   mint_control.register("<BEDROCK SOURCE='ECLIPSE' TYPE='_VAR_0' />",new EclipseHandler());
   mint_control.register("<BUBBLES DO='EXIT' />",new ExitHandler());
   mint_control.register("<BATT DO='_VAR_0' />",new CommandHandler());
}



/********************************************************************************/
/*										*/
/*	Server implementation							*/
/*										*/
/********************************************************************************/

void server()
{
   synchronized (this) {
      while (!is_done) {
	 checkEclipse();
	 try {
	    wait(60000l);
	  }
	 catch (InterruptedException e) { }
       }
    }

   System.err.println("BATT: Exiting due to no response or exit request");

   for_batt.stopAllTests();
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
   String r = rply.waitForString(60000);
   if (r == null) is_done = true;
}



/********************************************************************************/
/*										*/
/*	Manage file state							*/
/*										*/
/********************************************************************************/

private synchronized void setFileState(String file,FileState state)
{
   FileState fs = file_states.get(file);
   if (fs == null) fs = FileState.STABLE;
   if (fs == state) return;

   System.err.println("BATT: Set file state " + file + " " + state);

   Set<String> rslt = null;
   for (BattProject bp : project_set.values()) {
      rslt = bp.getClassesForFile(file,rslt);
    }

   if (rslt != null) {
      if (state == FileState.CHANGED) {
	 for (String s : rslt) changed_classes.put(s,state);
       }
      else if (state == FileState.ERRORS) {
	 for_batt.addErrors(rslt);
       }
      else if (state == FileState.STABLE) {
	 if (fs == FileState.CHANGED || fs == FileState.EDITED) {
	    for (String s : rslt) {
	       System.err.println("BATT: Changed class: " + s);
	       changed_classes.put(s,state);
	     }
	  }
	 for_batt.removeErrors(rslt);
       }
      else if (state == FileState.EDITED) {
	 for_batt.addErrors(rslt);		// mark the classes as invalid until compile
	 for (String s : rslt) changed_classes.put(s,state);
       }
    }

   file_states.put(file,state);
}



private synchronized void setErrorFiles(Collection<String> files)
{
   for (Map.Entry<String,FileState> ent : file_states.entrySet()) {
      String f = ent.getKey();
      FileState fs = ent.getValue();
      if (files.contains(f)) setFileState(f,FileState.ERRORS);
      else if (fs == FileState.ERRORS) {
	 setFileState(f,FileState.STABLE);
       }
    }
}




private void updateTestState()
{
   Map<String,FileState> chng = null;

   synchronized (this) {
      if (changed_classes.isEmpty()) return;

      chng = changed_classes;
      changed_classes = new HashMap<>();
    }

   for_batt.updateTestsForClasses(chng);
}




/********************************************************************************/
/*										*/
/*	Methods to load project information					*/
/*										*/
/********************************************************************************/

void loadProjects()
{
   MintDefaultReply rply = new MintDefaultReply();

   String msg = "<BUBBLES DO='PROJECTS' />";
   mint_control.send(msg,rply,MINT_MSG_FIRST_NON_NULL);

   Element r = rply.waitForXml();

   if (!IvyXml.isElement(r,"RESULT")) {
      System.err.println("BATT: Problem getting project information: " +
			    IvyXml.convertXmlToString(r));
      System.exit(2);
    }

   for (Element pe : IvyXml.children(r,"PROJECT")) {
      String pnm = IvyXml.getAttrString(pe,"NAME");
      MintDefaultReply prply = new MintDefaultReply();
      String pmsg = "<BUBBLES DO='OPENPROJECT' PROJECT='" + pnm +
	 "' CLASSES='true' FILES='false' PATHS='true' OPTIONS='false' />";
      mint_control.send(pmsg,prply,MINT_MSG_FIRST_NON_NULL);
      Element pr = prply.waitForXml();
      if (!IvyXml.isElement(pr,"RESULT")) {
	 System.err.println("BATT: Problem opening project " + pnm + ": " +
			       IvyXml.convertXmlToString(pr));
	 continue;
       }
      Element ppr = IvyXml.getChild(pr,"PROJECT");
      BattProject bp = new BattProject(ppr);
      project_set.put(pnm,bp);
    }
}


void reloadProjects()
{
   for (Map.Entry<String,BattProject> ent : project_set.entrySet()) {
      String pnm = ent.getKey();
      BattProject bp = ent.getValue();
      MintDefaultReply prply = new MintDefaultReply();
      String pmsg = "<BUBBLES DO='OPENPROJECT' PROJECT='" + pnm +
	 "' CLASSES='true' FILES='false' PATHS='true' OPTIONS='false' />";
      mint_control.send(pmsg,prply,MINT_MSG_FIRST_NON_NULL);
      Element pr = prply.waitForXml();
      if (!IvyXml.isElement(pr,"RESULT")) {
	 System.err.println("BATT: Problem opening project " + pnm + ": " +
			       IvyXml.convertXmlToString(pr));
	 continue;
       }
      Element ppr = IvyXml.getChild(pr,"PROJECT");
      bp.loadProject(ppr);
    }
}




/********************************************************************************/
/*										*/
/*	Methods to load configuration information				*/
/*										*/
/********************************************************************************/

void loadConfigurations()
{
   MintDefaultReply rply = new MintDefaultReply();

   String msg = "<BUBBLES DO='GETRUNCONFIG' />";
   mint_control.send(msg,rply,MINT_MSG_FIRST_NON_NULL);

   Element r = rply.waitForXml();
   if (!IvyXml.isElement(r,"RESULT")) {
      System.err.println("BATT: Problem getting launch information: " +
	    IvyXml.convertXmlToString(r));
      System.exit(2);
    }

   for (Element lce : IvyXml.children(r,"CONFIGURATION")) {
     Element te = IvyXml.getChild(lce,"TYPE");
     if (te == null) continue;
     String tnm = IvyXml.getAttrString(te,"NAME");
     if (!tnm.equals("JUnit")) continue;
     BattLaunchConfig blc = new BattLaunchConfig(lce);
     junit_launches.add(blc);
   }
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

List<BattProject> getProjects()
{
   return new ArrayList<BattProject>(project_set.values());
}



/********************************************************************************/
/*										*/
/*	Send messages								*/
/*										*/
/********************************************************************************/

void sendMessage(String typ,String cnts)
{
   StringBuffer buf = new StringBuffer();
   buf.append("<BATT TYPE='" + typ + "'>");
   if (cnts != null) buf.append(cnts);
   buf.append("</BATT>");

   System.err.println("BATT: Sending message: " + buf);

   mint_control.send(buf.toString());
}



void sendMessageAndWait(String typ,String cnts)
{
   StringBuffer buf = new StringBuffer();
   buf.append("<BATT TYPE='" + typ + "'>");
   if (cnts != null) buf.append(cnts);
   buf.append("</BATT>");

   System.err.println("BATT: Sending message: " + buf);

   MintDefaultReply rply = new MintDefaultReply();

   mint_control.send(buf.toString(),rply,MINT_MSG_FIRST_NON_NULL);

   rply.waitFor();
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
         else if (cmd.equals("FILECHANGE")) {
            setFileState(IvyXml.getAttrString(e,"FILE"),FileState.EDITED);
            updateTestState();
          }
         else if (cmd.equals("FILEERROR")) {
            FileState fs = null;
            Element msgs = IvyXml.getChild(e,"MESSAGES");
            if (msgs != null) {
               for (Element pm : IvyXml.children(msgs,"PROBLEM")) {
        	  if (IvyXml.getAttrBool(pm,"ERROR")) fs = FileState.ERRORS;
        	}
             }
            if (fs != null) {
               setFileState(IvyXml.getAttrString(e,"FILE"),fs);
               updateTestState();
             }
          }
         else if (cmd.equals("EDITERROR")) {
            FileState fs = FileState.EDITED;
            Element msgs = IvyXml.getChild(e,"MESSAGES");
            if (msgs != null) {
               for (Element pm : IvyXml.children(msgs,"PROBLEM")) {
        	  if (IvyXml.getAttrBool(pm,"ERROR")) fs = FileState.ERRORS;
        	}
             }
            setFileState(IvyXml.getAttrString(e,"FILE"),fs);
            updateTestState();
          }
         else if (cmd.equals("BUILDDONE")) {
            for (Element de : IvyXml.children(e,"DELTA")) {
               Element re = IvyXml.getChild(de,"RESOURCE");
               String rtyp = IvyXml.getAttrString(re,"TYPE");
               if (rtyp != null && rtyp.equals("FILE")) {
        	  String fp = IvyXml.getAttrString(re,"LOCATION");
        	  FileState fs = FileState.STABLE;
        	  for (Element me : IvyXml.children(de,"MARKER")) {
        	     for (Element pe : IvyXml.children(me,"PROBLEM")) {
        		if (IvyXml.getAttrBool(pe,"ERROR")) fs = FileState.ERRORS;
        	      }
        	   }
        	  System.err.println("BATT: Note " + fp + " BUILT " + fs);
        	  setFileState(fp,fs);
        	}
             }
            updateTestState();
          }
         else if (cmd.equals("LAUNCHCONFIGEVENT")) {
            // handle changes to saved launch configurations
          }
         else if (cmd.equals("RESOURCE")) {
            for (Element de : IvyXml.children(e,"DELTA")) {
               Element re = IvyXml.getChild(de,"RESOURCE");
               String rtyp = IvyXml.getAttrString(re,"TYPE");
               if (rtyp != null && rtyp.equals("FILE")) {
        	  String fp = IvyXml.getAttrString(re,"LOCATION");
        	  System.err.println("BATT: Note " + fp + " CHANGED");
        	  setFileState(fp,FileState.CHANGED);
        	}
             }
            updateTestState();
          }
         else if (cmd.equals("STOP")) {
            serverDone();
          }
         else if (cmd.equals("EDIT")) {
            msg.replyTo();
          }
         else {
            msg.replyTo();
          }
       }
      catch (Throwable t) {
         System.err.println("BATT: Problem processing Eclipse command: " + t);
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
   
      System.err.println("BATT: RECEIVED COMMAND " + cmd + ": " + msg.getText());
   
      try {
         if (cmd == null) return;
         else if (cmd.equals("SETMODE")) {
            String v = IvyXml.getAttrString(e,"VALUE");
            if (v.equals("DEMAND")) for_batt.setMode(TestMode.ON_DEMAND);
            else if (v.equals("ON_DEMAND")) for_batt.setMode(TestMode.ON_DEMAND);
            else if (v.equals("CONTINUOUS")) for_batt.setMode(TestMode.CONTINUOUS);
          }
         else if (cmd.equals("RUNTESTS")) {
            String v = IvyXml.getAttrString(e,"TYPE");
            if (v == null || v.equals("ALL")) {
               for_batt.runAllTests();
             }
            else if (v.equals("FAIL")) {
               Collection<BattTestCase> fails = for_batt.findFailingTests();
               for_batt.runSelectedTests(fails);
             }
            else if (v.equals("PENDING")) {
               Collection<BattTestCase> pends = for_batt.findPendingTests();
               for_batt.runSelectedTests(pends);
             }
            else {
               for_batt.runAllTests();
             }
            for_batt.doTests();
          }
         else if (cmd.equals("RUNTEST")) {
            Collection<BattTestCase> lst = new ArrayList<>();
            String t = IvyXml.getAttrString(e,"TEST");
            BattTestCase btc = for_batt.findTestCase(t);
            if (btc != null) {
               lst.add(btc);
             }
            else {
               for (Element telt : IvyXml.children(e,"TEST")) {
                  String t1 = IvyXml.getAttrString(telt,"NAME");
                  if (t1 != null) { 
                     BattTestCase btc1 = for_batt.findTestCase(t1);
                     if (btc1 != null) lst.add(btc1);
                   }
                }
             }
            if (!lst.isEmpty()) {
               for_batt.runSelectedTests(lst);
               for_batt.doTests();
             }
          }
         else if (cmd.equals("STOPTEST")) {
            for_batt.stopTests();
          }
         else if (cmd.equals("SHOWALL")) {
            rply = for_batt.showAllTests();
          }
         else if (cmd.equals("SHOW")) {
            String t = IvyXml.getAttrString(e,"TEST");
            BattTestCase btc = for_batt.findTestCase(t);
            if (btc != null) {
               Collection<BattTestCase> lst = new ArrayList<>();
               lst.add(btc);
               rply = for_batt.showSelectedTests(lst);
             }
          }
         else if (cmd.equals("ERRORS")) {
            Set<String> files = new HashSet<>();
            for (Element fe : IvyXml.children(e,"FILE")) {
               files.add(IvyXml.getText(fe));
             }
            setErrorFiles(files);
          }
         else if (cmd.equals("UPDATE")) {
            for_batt.updateTests();
          }
         else if (cmd.equals("PING")) {
            rply = "PONG";
          }
         else if (cmd.equals("MODE")) {
            rply = for_batt.getMode().toString();
          }
         else if (cmd.equals("EXIT")) {
            serverDone();
          }
       }
      catch (Throwable t) {
         System.err.println("BATT: Problem processing BATT command: " + t);
         t.printStackTrace();
       }
   
      if (rply != null) {
         rply = "<RESULT>" + rply + "</RESULT>";
       }
   
      msg.replyTo(rply);
    }

}	// end of inner class CommandHandler




}	// end of class BattMonitor




/* end of BattMonitor.java */
