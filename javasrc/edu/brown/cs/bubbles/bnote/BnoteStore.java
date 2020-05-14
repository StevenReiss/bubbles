/********************************************************************************/
/*										*/
/*		BnoteStore.java 						*/
/*										*/
/*	Main access to notebook store						*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2009, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bnote;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardSetup;

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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class BnoteStore implements BnoteConstants, MintConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BnoteDatabase	note_db;
private RemoteClient	db_client;
private ExecutorService task_queue;

private static BnoteStore	the_store = null;

private static Set<String>	field_strings;


static {
   field_strings = new HashSet<String>();
   field_strings.add("PROJECT");
   field_strings.add("TYPE");
   field_strings.add("USER");
   field_strings.add("TIME");
   field_strings.add("TASK");
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BnoteStore()
{
   BoardSetup bs = BoardSetup.getSetup();
   switch (bs.getRunMode()) {
      case NORMAL :
	 note_db = new BnoteDatabase();
         task_queue = Executors.newSingleThreadExecutor();
	 break;
      case CLIENT :
	 db_client = new RemoteClient(bs.getMintControl());
	 note_db = new BnoteDatabase(true);
         task_queue = null;
	 break;
      case SERVER :
	 note_db = new BnoteDatabase();
         task_queue = null;
	 MintControl mc2 = bs.getMintControl();
	 mc2.register("<BNOTE CMD='_VAR_0'></BNOTE>",new NoteServer());
	 break;
    }
}



static BnoteStore createStore()
{
   if (the_store == null) {
      the_store = new BnoteStore();
    }
   return the_store;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

boolean isEnabled()
{
   if (db_client != null) return db_client.isEnabled();

   return true;
}



List<BnoteTask> getTasksForProject(String proj)
{
   if (db_client != null) return db_client.getTasksForProject(proj);

   return note_db.getTasksForProject(proj);
}


List<String> getUsersForTask(String proj,BnoteTask task)
{
   if (db_client != null) return db_client.getUsersForTask(proj,task);

   return note_db.getUsersForTask(proj,task);
}


List<Date> getDatesForTask(String proj,BnoteTask task)
{
   if (db_client != null) return db_client.getDatesForTask(proj,task);

   return note_db.getDatesForTask(proj,task);
}


List<String> getNamesForTask(String proj,BnoteTask task)
{
   if (db_client != null) return db_client.getNamesForTask(proj,task);

   return note_db.getNamesForTask(proj,task);
}



List<BnoteEntry> getEntriesForTask(String proj,BnoteTask task)
{
   if (db_client != null) return db_client.getEntriesForTask(proj,task);

   return note_db.getEntriesForTask(proj,task);
}



File getAttachment(String aid)
{
   if (db_client != null) return db_client.getAttachment(aid);

   return note_db.getAttachment(aid);
}


String getAttachmentAsString(String aid)
{
   if (db_client != null) return db_client.getAttachmentAsString(aid);

   return note_db.getAttachmentAsString(aid);
}


BnoteTask findTaskById(int id)
{
   if (db_client != null) return db_client.findTaskById(id);

   return note_db.findTaskById(id);
}




/********************************************************************************/
/*										*/
/*	Static logging entries							*/
/*										*/
/********************************************************************************/

public static BnoteTask defineTask(String name,String proj,String desc)
{
   if (the_store == null) return null;

   return log(proj,null,BnoteEntryType.NEW_TASK,"NAME",name,"DESCRIPTION",desc);
}




public static BnoteTask log(String project,BnoteTask task,BnoteEntryType type,Map<String,Object> values)
{
   if (the_store == null) return null;

   return the_store.enter(project,task,type,values);
}



public static BnoteTask log(String project,BnoteTask task,BnoteEntryType type,Object ... args)
{
   if (the_store == null) return null;

   Map<String,Object> values = new HashMap<String,Object>();
   for (int i = 0; i < args.length-1; i += 2) {
      values.put(args[i].toString(),args[i+1]);
    }

   return log(project,task,type,values);
}



public static boolean attach(BnoteTask task,File file)
{
   if (the_store == null || task == null || file == null) return false;

   long aid = the_store.saveAttachment(file);
   if (aid == 0) return false;

   log(task.getProject(),task,BnoteEntryType.ATTACHMENT,"SOURCE",file,"ATTACHID",aid);

   return true;
}


/********************************************************************************/
/*										*/
/*	Internal logging entries						*/
/*										*/
/********************************************************************************/

private BnoteTask enter(String project,BnoteTask task,BnoteEntryType type,Map<String,Object> values)
{
   if (db_client != null) {
      return db_client.enter(project,task,type,values);
    }

   if (task_queue == null) {
      return note_db.addEntry(project,task,type,values);
    }
   
   TaskEntry te = new TaskEntry(project,task,type,values);
   Future<BnoteTask> fe = task_queue.submit(te);
   
   switch (type) {
      case NEW_TASK :
         try {
            return fe.get();
          }
         catch (ExecutionException e) { }
         catch (InterruptedException e) { }
         break;
      default :
	 break;
    }
   
   return null;
}



/********************************************************************************/
/*										*/
/*	Attachment methods							*/
/*										*/
/********************************************************************************/

private long saveAttachment(File f)
{
   long len = f.length();
   if (len == 0 || len > MAX_ATTACHMENT_SIZE) return 0;

   if (db_client != null) return db_client.saveAttachment(f);

   try {
      InputStream ins = new FileInputStream(f);
      return note_db.saveAttachment(f.getPath(),ins,(int) len);
    }
   catch (IOException e) {
      BoardLog.logD("BNOTE","Problem reading attachment: " + e);
    }
   return 0;
}




/********************************************************************************/
/*                                                                              */
/*      Entry representation                                                    */
/*                                                                              */
/********************************************************************************/

private class TaskEntry implements Callable<BnoteTask> {
   
   private String project_name;
   private BnoteTask for_task;
   private BnoteEntryType entry_type;
   private Map<String,Object> entry_values;
   
   TaskEntry(String proj,BnoteTask task,BnoteEntryType typ,Map<String,Object> vals) {
      project_name = proj;
      for_task = task;
      entry_type = typ;
      entry_values = new HashMap<>(vals);
    }
   
   @Override public BnoteTask call() {
      Thread.currentThread().setName("BnoteTaskThread");
      return note_db.addEntry(project_name,for_task,entry_type,entry_values);
    }
   
}       // end of inner class TaskEntry




/********************************************************************************/
/*										*/
/*	Server for handling remote requests					*/
/*										*/
/********************************************************************************/

private class NoteServer implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      Element xml = msg.getXml();
      BoardLog.logD("BNOTE","Remove Message: " + msg.getText());
   
      String proj = IvyXml.getAttrString(xml,"PROJECT");
      long tid = IvyXml.getAttrLong(xml,"TASKID");
      BnoteTask task = null;
      if (tid > 0) task = note_db.findTaskById(tid);
      String qid = IvyXml.getAttrString(xml,"QID");
   
      IvyXmlWriter xw = new IvyXmlWriter();
      xw.begin("RESULT");
      String cmd = args.getArgument(0);
      if (cmd.equals("ISENABLED")) {
         xw.field("VALUE",isEnabled());
       }
      else if (cmd.equals("TASKS")) {
         List<BnoteTask> lbt = getTasksForProject(proj);
         if (lbt != null) {
            for (BnoteTask bt : lbt) {
               bt.outputXml(xw);
             }
          }
       }
      else if (cmd.equals("TASKUSER")) {
         List<String> lst = getUsersForTask(proj,task);
         if (lst != null) {
            for (String s : lst) xw.textElement("USER",s);
          }
       }
      else if (cmd.equals("TASKDATE")) {
         List<Date> lst = getDatesForTask(proj,task);
         if (lst != null) {
            for (Date s : lst) {
               xw.begin("DATE");
               xw.field("VALUE",s);
               xw.end("DATE");
             }
          }
       }
      else if (cmd.equals("TASKNAME")) {
         List<String> lst = getNamesForTask(proj,task);
         if (lst != null) {
            for (String s : lst) xw.textElement("NAME",s);
          }
       }
      else if (cmd.equals("TASKENTRY")) {
         List<BnoteEntry> lst = getEntriesForTask(proj,task);
         if (lst != null) {
            for (BnoteEntry be : lst) {
               be.outputXml(xw);
             }
          }
       }
      else if (cmd.equals("ATTACHFILE")) {
         File f = getAttachment(qid);
         if (f != null) xw.field("FILE",f.getPath());
       }
      else if (cmd.equals("ATTACHSTR")) {
         String c = getAttachmentAsString(qid);
         if (c != null) xw.cdataElement("CONTENTS",c);
       }
      else if (cmd.equals("FINDTASK")) {
         long v = Long.parseLong(qid);
         BnoteTask bt = note_db.findTaskById(v);
         if (bt != null) bt.outputXml(xw);
       }
      else if (cmd.equals("ENTER")) {
         tid = IvyXml.getAttrLong(xml,"TASK");
         BnoteTask bt = (tid <= 0 ? null : note_db.findTaskById(tid));
         BnoteEntryType typ = IvyXml.getAttrEnum(xml,"TYPE",BnoteEntryType.NONE);
         Map<String,Object> vmap = new HashMap<>();
         for (Element de : IvyXml.children(xml,"DATA")) {
            String k = IvyXml.getAttrString(de,"KEY");
            // will have to special case some fields here (esp attachments)
            vmap.put(k,IvyXml.getText(de));
          }
         BnoteTask nt = enter(proj,bt,typ,vmap);
         if (nt != null) nt.outputXml(xw);
      }
      else if (cmd.equals("ATTACH")) {
         File f = new File("/tmp/garbagegoeshere");
         long v = saveAttachment(f);
         xw.field("VALUE",v);
       }
   
      xw.end("RESULT");
      BoardLog.logD("BNOTE","SERVER REPLT: " + xw.toString());
      msg.replyTo(xw.toString());
    }

}	// end of inner class NoteServer





private class RemoteClient {

   private MintControl	mint_control;

   RemoteClient(MintControl mc) {
      mint_control = mc;
    }

   boolean isEnabled() {
      Element xml = sendMessage("ISENABLED",null,null,null);
      if (IvyXml.isElement(xml,"RESULT")) return IvyXml.getAttrBool(xml,"VALUE");
      return false;
    }

   List<BnoteTask> getTasksForProject(String proj) {
      Element xml = sendMessage("TASKS",proj,null,null);
      if (IvyXml.isElement(xml,"RESULT")) {
	 List<BnoteTask> rslt = new ArrayList<BnoteTask>();
	 for (Element txml : IvyXml.children(xml,"TASK")) {
	    BnoteTask bt = note_db.findTaskById(txml);
	    rslt.add(bt);
	  }
	 return rslt;
       }
      return null;
    }

   List<String> getUsersForTask(String proj,BnoteTask task) {
      Element xml = sendMessage("TASKUSER",proj,task,null);
      if (IvyXml.isElement(xml,"RESULT")) {
	 List<String> rslt = new ArrayList<String>();
	 for (Element sxml : IvyXml.children(xml,"USER")) {
	    String s = IvyXml.getText(sxml);
	    rslt.add(s);
	  }
	 return rslt;
       }
      return null;
    }

   List<Date> getDatesForTask(String proj,BnoteTask task) {
      Element xml = sendMessage("TASKDATE",proj,task,null);
      if (IvyXml.isElement(xml,"RESULT")) {
	 List<Date> rslt = new ArrayList<Date>();
	 for (Element sxml : IvyXml.children(xml,"DATE")) {
	    Date d = IvyXml.getAttrDate(sxml,"VALUE");
	    if (d != null) rslt.add(d);
	  }
	 return rslt;
       }
      return null;
    }

   List<String> getNamesForTask(String proj,BnoteTask task) {
      Element xml = sendMessage("TASKNAME",proj,task,null);
      if (IvyXml.isElement(xml,"RESULT")) {
	 List<String> rslt = new ArrayList<String>();
	 for (Element sxml : IvyXml.children(xml,"NAME")) {
	    String s = IvyXml.getText(sxml);
	    rslt.add(s);
	  }
	 return rslt;
       }
      return null;
    }

   List<BnoteEntry> getEntriesForTask(String proj,BnoteTask task) {
      Element xml = sendMessage("TASKENTRY",proj,task,null);
      if (IvyXml.isElement(xml,"RESULT")) {
	 List<BnoteEntry> rslt = new ArrayList<BnoteEntry>();
	 for (Element sxml : IvyXml.children(xml,"ENTRY")) {
	    BnoteEntry be = note_db.createEntry(sxml);
	    rslt.add(be);
	  }
	 return rslt;
       }
      return null;
    }

   File getAttachment(String aid) {
      Element xml = sendMessage("ATTACHFILE",null,null,aid);
      if (IvyXml.isElement(xml,"RESULT")) {
	 return new File(IvyXml.getAttrString(xml,"FILE"));
       }
      return null;
    }

   String getAttachmentAsString(String aid) {
      Element xml = sendMessage("ATTACHSTR",null,null,aid);
      if (IvyXml.isElement(xml,"RESULT")) {
	 return IvyXml.getTextElement(xml,"CONTENTS");
       }
      return null;
    }

   BnoteTask findTaskById(long id) {
      Element xml = sendMessage("FINDTASK",null,null,Long.toString(id));
      if (IvyXml.isElement(xml,"RESULT")) {
	 return note_db.findTaskById(IvyXml.getChild(xml,"TASK"));
       }
      return null;
    }

   long saveAttachment(File f) {
      IvyXmlWriter xw = new IvyXmlWriter();
      xw.begin("BNOTE");
      xw.field("CMD","ATTACH");
      // need to encode the file here
      xw.end("BNOTE");

      MintDefaultReply mdr = new MintDefaultReply();
      mint_control.send(xw.toString(),mdr,MINT_MSG_FIRST_NON_NULL);
      xw.close();

      Element xml = mdr.waitForXml();
      if (IvyXml.isElement(xml,"RESULT")) {
	 return IvyXml.getAttrLong(xml,"VALUE");
       }
      return 0;
    }

   BnoteTask enter(String proj,BnoteTask task,BnoteEntryType typ,Map<String,Object> values) {
      IvyXmlWriter xw = new IvyXmlWriter();
      if (proj != null) values.put("PROJECT",proj);
      if (typ != null) values.put("TYPE",typ);
      if (!values.containsKey("USER")) values.put("USER",System.getProperty("user.name"));
      values.put("TIME",System.currentTimeMillis());
      if (task != null) values.put("TASK",task);
   
      xw.begin("BNOTE");
      xw.field("CMD","ENTER");
   
      for (Map.Entry<String,Object> ent : values.entrySet()) {
         String k = ent.getKey();
         if (!field_strings.contains(k)) continue;
         Object v = ent.getValue();
         if (v instanceof BnoteTask) xw.field(k,((BnoteTask)v).getTaskId());
         else xw.field(k,v.toString());
       }
   
      for (Map.Entry<String,Object> ent : values.entrySet()) {
         String k = ent.getKey();
         if (field_strings.contains(k)) continue;
         Object v = ent.getValue();
         xw.begin("DATA");
         xw.field("KEY",k);
         // hande to special case some fields here
         xw.cdata(v.toString());
         xw.end("DATA");
       }
   
      xw.end("BNOTE");
   
      MintDefaultReply mdr = new MintDefaultReply();
      mint_control.send(xw.toString(),mdr,MINT_MSG_FIRST_NON_NULL);
      xw.close();
      Element xml = mdr.waitForXml();
      if (IvyXml.isElement(xml,"RESULT")) {
         Element txml = IvyXml.getChild(xml,"TASK");
         if (txml != null) return note_db.findTaskById(txml);
       }
      return null;
    }

   private Element sendMessage(String cmd,String proj,BnoteTask task,String id) {
      String txt = "<BNOTE CMD='" + cmd + "'";
      if (proj != null) txt += " PROJECT='" + proj + "'";
      if (task != null) txt += " TASKID='" + task.getTaskId() + "'";
      if (id != null) txt += " QID='" + id + "'";
      txt += " >";
      txt += "</BNOTE>";
      MintDefaultReply mdr = new MintDefaultReply();
      mint_control.send(txt,mdr,MINT_MSG_FIRST_NON_NULL);
      Element rslt = mdr.waitForXml();
      BoardLog.logD("BNOTE","REPLY " + IvyXml.convertXmlToString(rslt));
      return rslt;
    }

}	// end of inner class RemoteClient




}	// end of class BnoteStore




/* end of BnoteStore.java */

