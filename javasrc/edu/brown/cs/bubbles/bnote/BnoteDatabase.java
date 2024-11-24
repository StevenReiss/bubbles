/********************************************************************************/
/*										*/
/*		BnoteDatabase.java						*/
/*										*/
/*	Database interface for storing programmers notebook			*/
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
import edu.brown.cs.bubbles.board.BoardThreadPool;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;



class BnoteDatabase implements BnoteConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Connection	note_conn;
private List<TaskImpl>	all_tasks;

private long		id_count;
private long		next_id;
private long		id_request;
private Boolean 	use_begin;
private boolean 	use_streams;

private static Set<String>	ignore_fields;

static {
   ignore_fields = new HashSet<String>();
   ignore_fields.add("PROJECT");
   ignore_fields.add("TYPE");
   ignore_fields.add("USER");
   ignore_fields.add("TIME");
}


// TODO: Add entry to dump the database, add entry to merge a dumped database
//	needing to track what has been added before
//	needing to change IDs



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BnoteDatabase()
{
   id_count = 0;
   next_id = 0;
   id_request = 32;
   use_begin = null;
   all_tasks = new ArrayList<TaskImpl>();
   use_streams = false;

   BnoteConnect bcn = new BnoteConnect();

   note_conn = bcn.getLogDatabase();

   if (note_conn == null) return;

   try {
      Statement st = note_conn.createStatement();
      st.execute("SET TRANSACTION ISOLATION LEVEL SERIALIZABLE");
      st.close();
    }
   catch (SQLException e) { }

   loadTasks();
}


BnoteDatabase(boolean local)
{
   id_count = 0;
   next_id = 0;
   id_request = 32;
   use_begin = null;
   all_tasks = new ArrayList<TaskImpl>();
   use_streams = false;
}



/********************************************************************************/
/*										*/
/*	Add to the database							*/
/*										*/
/********************************************************************************/

synchronized BnoteTask addEntry(String proj,BnoteTask task,BnoteEntryType type,Map<String,Object> values)
{
   if (note_conn == null) return null;

   BoardLog.logD("BNOTE","ADD ENTRY WITH " + values.size() + " VALUES");

   long eid = getNextId();
   if (eid == 0) return null;

   if (proj == null) proj = (String) values.get("PROJECT");

   switch (type) {
      case NONE :
	 return null;
      case NEW_TASK :
	 String nm = (String) values.remove("NAME");
	 String ds = (String) values.remove("DESCRIPTION");
	 task = defineTask(nm,proj,ds);
	 break;
      default:
	 break;
    }

   if (task != null) {
      TaskImpl ti = (TaskImpl) task;
      ti.noteUse();
    }

   NodeInserter ni = new NodeInserter(eid,proj,values,task,type);
   BoardThreadPool.start(ni);

   return task;
}



private synchronized void resetDatabase()
{
   BnoteConnect bcn = new BnoteConnect();
   note_conn = bcn.getLogDatabase();
}



private class NodeInserter implements Runnable {

   private long entry_id;
   private String project_name;
   private Map<String,Object> value_set;
   private BnoteTask for_task;
   private BnoteEntryType entry_type;

   NodeInserter(long eid,String proj,Map<String,Object> values,BnoteTask task,
	BnoteEntryType type) {
      entry_id = eid;
      project_name = proj;
      value_set = values;
      for_task = task;
      entry_type = type;
    }

   @Override public void run() {
      while (note_conn != null) {
	 Connection c = note_conn;
	 if (c == null) break;

	 String unm = null;
	 if (value_set.get("USER") != null) unm = value_set.get("USER").toString();
	 if (unm == null) unm = System.getProperty("user.name");

	 BoardLog.logD("BNOTE","ADD ENTRY WITH " + value_set.size() + " VALUES");

	 try {
	    PreparedStatement s = c.prepareStatement("INSERT INTO Entry VALUES (?,?,?,?,?,DEFAULT)");
	    s.setLong(1,entry_id);
	    s.setString(2,project_name);
	    if (for_task != null) s.setLong(3,for_task.getTaskId());
	    else s.setLong(3,0);
	    s.setString(4,entry_type.toString());
	    s.setString(5,unm);
	    s.executeUpdate();
	    s.close();

	    for (Map.Entry<String,Object> ent : value_set.entrySet()) {
	       if (ignore_fields.contains(ent.getKey())) continue;
	       s = c.prepareStatement("INSERT INTO Prop VALUES (?,?,?)");
	       s.setLong(1,entry_id);
	       s.setString(2,ent.getKey());
	       s.setString(3,ent.getValue().toString());
	       s.executeUpdate();
	       s.close();
	     }
	    // BoardLog.logD("BNOTE","OPERATION SUCCEEDED");
	    break;
	  }
	 catch (SQLException e) {
	    BoardLog.logD("BNOTE","OPERATION FAILED: " + e);
	    resetDatabase();
	    if (note_conn == null) {
	       BoardLog.logE("BNOTE","Problem saving notebook entry " + entry_id + " " +
		     project_name + " " + entry_type + " " + unm + " " + value_set.size(),e);
	       break;
	     }
	  }
       }
    }
}




/********************************************************************************/
/*										*/
/*	Attachment methods							*/
/*										*/
/********************************************************************************/

synchronized long saveAttachment(String anm,InputStream ins,int len)
{
   long id = getNextId();

   BoardLog.logD("BNOTE","Save attachment " + anm + " " + id + " " + len);

   if (id == 0 || len > MAX_ATTACHMENT_SIZE) return 0;

   try {
      PreparedStatement s = note_conn.prepareStatement("INSERT INTO Attachment VALUES (?,?,?)");
      s.setLong(1,id);
      s.setString(2,anm);
      if (!use_streams) {
	 try {
	    if (len > 0) s.setBlob(3,ins,len);
	    else s.setBlob(3,ins);
	  }
	 catch (SQLException e) {
	    use_streams = true;
	  }
       }
      if (use_streams) {
	 if (len > 0) s.setBinaryStream(3,ins,len);
	 else s.setBinaryStream(3,ins);
       }
      s.executeUpdate();
      s.close();
    }
   catch (SQLException e) {
      BoardLog.logE("BNOTE","Problem saving attachment",e);
    }

   return id;
}



synchronized File getAttachment(String aid)
{
   File outf = null;

   BoardLog.logD("BNOTE","Finding attachment " + aid);

   long id = 0;

   try {
      id = Long.parseLong(aid);
    }
   catch (NumberFormatException e) {
      BoardLog.logE("BNOTE","Bad attachment id " + aid);
      return null;
    }

   try {
      String q = "SELECT A.source,A.data FROM Attachment A WHERE A.id = ?";
      PreparedStatement s = note_conn.prepareStatement(q);
      s.setLong(1,id);
      ResultSet rs = s.executeQuery();
      if (!rs.next()) {
	 BoardLog.logE("BNOTE","Attachment " + aid + " not found");
	 return null;
       }
      String snm = rs.getString(1);
      InputStream ins = null;
      if (!use_streams) {
	 try {
	    Blob data = rs.getBlob(2);
	    ins = data.getBinaryStream();
	  }
	 catch (SQLException e) {
	    use_streams = true;
	  }
       }
      if (use_streams || ins == null) ins = rs.getBinaryStream(2);
      int idx = snm.lastIndexOf(".");
      String kind = "";
      if (idx > 0) kind = snm.substring(idx);
      outf = File.createTempFile("BnoteBlob",kind);
      outf.deleteOnExit();
      try (OutputStream fos = new FileOutputStream(outf)) {
         byte [] buf = new byte[16384];
         for ( ; ; ) {
            int ln = ins.read(buf);
            if (ln < 0) break;
            fos.write(buf,0,ln);
          }
       }
      ins.close();
      s.close();
    }
   catch (SQLException e) {
      outf = null;
      BoardLog.logE("BNOTE","Problem accessing attachment",e);
    }
   catch (IOException e) {
      outf = null;
      BoardLog.logE("BNOTE","Problem accessing attachment",e);
    }

   return outf;
}



synchronized String getAttachmentAsString(String aid)
{
   long id = 0;

   try {
      id = Long.parseLong(aid);
    }
   catch (NumberFormatException e) {
      return null;
    }

   try {
      String q = "SELECT A.data FROM Attachment A WHERE A.id = ?";
      PreparedStatement s = note_conn.prepareStatement(q);
      s.setLong(1,id);
      ResultSet rs = s.executeQuery();
      if (!rs.next()) return null;
      Blob data = rs.getBlob(1);
      byte [] bytes = data.getBytes(0,(int) data.length());
      String rslt = new String(bytes);
      s.close();
      return rslt;
    }
   catch (SQLException e) {
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Task methods								*/
/*										*/
/********************************************************************************/

synchronized TaskImpl defineTask(String name,String proj,String desc)
{
   if (note_conn == null) return null;

   BoardLog.logD("BNOTE","DEFINE TASK");

   TaskImpl ti = null;

   long tid = getNextId();
   if (tid == 0) return null;

   ti = new TaskImpl(tid,name,proj,desc);
   // if server, need to send new task message
   all_tasks.add(ti);

   TaskInserter ins = new TaskInserter(ti);
   BoardThreadPool.start(ins);

   return ti;
}


private class TaskInserter implements Runnable {

   private TaskImpl for_task;

   TaskInserter(TaskImpl ti) {
      for_task = ti;
    }

   @Override public void run() {
      String q = "INSERT INTO Task VALUES (?,?,?,?)";

      while (note_conn != null) {
	 try {
	    PreparedStatement s = note_conn.prepareStatement(q);
	    s.setLong(1,for_task.getTaskId());
	    s.setString(2,for_task.getName());
	    s.setString(3,for_task.getDescription());
	    s.setString(4,for_task.getProject());
	    s.executeUpdate();
	    s.close();
	    break;
	  }
	 catch (SQLException e) {
	    BoardLog.logE("BNOTE","Problem defining new task",e);
	    resetDatabase();
	  }
       }
    }
}





private void loadTasks()
{
   all_tasks = new ArrayList<TaskImpl>();

   String q = "SELECT T.id,T.name,T.project,T.description FROM Task T";

   while (note_conn != null) {
      try {
	 PreparedStatement s = note_conn.prepareStatement(q);
	 ResultSet rs = s.executeQuery();
	 while (rs.next()) {
	    int id = rs.getInt(1);
	    String nm = rs.getString(2);
	    String pr = rs.getString(3);
	    String d = rs.getString(4);
	    TaskImpl ti = new TaskImpl(id,nm,pr,d);
	    // if server, need to send new task message
	    all_tasks.add(ti);
	  }
	 s.close();
	 break;
       }
      catch (SQLException e) {
	 BoardLog.logE("BNOTE","Problem loading tasks",e);
	 resetDatabase();
       }
    }
}




List<BnoteTask> getTasksForProject(String proj)
{
   List<BnoteTask> rslt = new ArrayList<>();

   for (TaskImpl ti : all_tasks) {
      if (proj == null || proj.equals(ti.getProject())) {
	 rslt.add(ti);
       }
    }

   return rslt;
}



synchronized List<String> getUsersForTask(String proj,BnoteTask task)
{
   List<String> rslt = new ArrayList<String>();

   BoardLog.logD("BNOTE","GET USERS FOR TASK");

   String q = "SELECT DISTINCT E.username FROM Entry E";
   if (proj != null || task != null) {
      q += " WHERE ";
      if (proj != null) q += "E.project = ?";
      if (task != null) {
	 if (proj != null) q += " AND ";
	 q += "E.taskid = ?";
       }
    }
   q += " ORDER BY E.username";

   try {
      PreparedStatement s = note_conn.prepareStatement(q);
      if (proj != null) {
	 s.setString(1,proj);
	 if (task != null) s.setLong(2,task.getTaskId());
       }
      else if (task != null) {
	 s.setLong(1,task.getTaskId());
       }
      ResultSet rs = s.executeQuery();
      while (rs.next()) {
	 String unm = rs.getString(1);
	 rslt.add(unm);
       }
      s.close();
    }
   catch (SQLException e) {
      BoardLog.logE("BNOTE","Problem getting user set",e);
    }

   return rslt;
}



synchronized List<Date> getDatesForTask(String proj,BnoteTask task)
{
   if (note_conn == null) return null;

   List<Date> rslt = new ArrayList<Date>();

   BoardLog.logD("BNOTE","GET DATES FOR TASK");

   String q = "SELECT DISTINCT E.time FROM Entry E";
   if (proj != null || task != null) {
      q += " WHERE ";
      if (proj != null) q += "E.project = ?";
      if (task != null) {
	 if (proj != null) q += " AND ";
	 q += "E.taskid = ?";
       }
    }
   q += " ORDER BY E.time";

   try {
      PreparedStatement s = note_conn.prepareStatement(q);
      if (proj != null) {
	 s.setString(1,proj);
	 if (task != null) s.setLong(2,task.getTaskId());
       }
      else if (task != null) {
	 s.setLong(1,task.getTaskId());
       }
      ResultSet rs = s.executeQuery();
      while (rs.next()) {
	 Date unm = rs.getTimestamp(1);
	 rslt.add(unm);
       }
      s.close();
    }
   catch (SQLException e) {
      BoardLog.logE("BNOTE","Problem getting user set",e);
    }

   return rslt;
}



synchronized List<String> getNamesForTask(String proj,BnoteTask task)
{
   Set<String> rslt = new TreeSet<String>();

   BoardLog.logD("BNOTE","GET NAMES FOR TASK");

   String q = "SELECT P.value FROM Prop P";
   if (proj != null || task != null) q += ", Entry E";
   q += " WHERE P.id = 'NAME'";
   if (proj != null || task != null) {
      q += " AND E.id = P.entry";
      if (proj != null) q += " AND E.project = ?";
      if (task != null) {
	 q += " AND E.taskid = ?";
       }
    }

   try {
      PreparedStatement s = note_conn.prepareStatement(q);
      if (proj != null) {
	 s.setString(1,proj);
	 if (task != null) s.setLong(2,task.getTaskId());
       }
      else if (task != null) {
	 s.setLong(1,task.getTaskId());
       }
      ResultSet rs = s.executeQuery();
      while (rs.next()) {
	 String unm = rs.getString(1);
	 rslt.add(unm);
       }
      s.close();
    }
   catch (SQLException e) {
      BoardLog.logE("BNOTE","Problem getting name set",e);
    }

   return new ArrayList<String>(rslt);
}



synchronized List<BnoteEntry> getEntriesForTask(String proj,BnoteTask task)
{
   List<BnoteEntry> rslt = new ArrayList<BnoteEntry>();

   BoardLog.logD("BNOTE","GET ENTRIES FOR TASK");

   String q = "SELECT * FROM Entry E";
   if (proj != null || task != null) {
      q += " WHERE ";
      if (proj != null) q += "E.project = ?";
      if (task != null) {
	 if (proj != null) q += " AND ";
	 q += "E.taskid = ?";
       }
    }
   q += " ORDER BY time";

   try {
      PreparedStatement s = note_conn.prepareStatement(q);
      if (proj != null) {
	 s.setString(1,proj);
	 if (task != null) s.setLong(2,task.getTaskId());
       }
      else if (task != null) {
	 s.setLong(1,task.getTaskId());
       }
      ResultSet rs = s.executeQuery();
      while (rs.next()) {
	 EntryImpl ei = new EntryImpl(rs);
	 rslt.add(ei);
       }
      s.close();
    }
   catch (SQLException e) {
      BoardLog.logE("BNOTE","Problem getting name set",e);
    }

   return rslt;
}



BnoteTask findTaskById(long id)
{
   if (id <= 0 || note_conn == null) return null;

   for (TaskImpl ti : all_tasks) {
      if (ti.getTaskId() == id) return ti;
    }

   return null;
}


BnoteTask findTaskById(Element xml)
{
   long id = IvyXml.getAttrLong(xml,"ID");
   BnoteTask bt = findTaskById(id);
   if (bt == null) {
      TaskImpl ti = new TaskImpl(xml);
      all_tasks.add(ti);
      bt = ti;
    }
   return bt;
}


BnoteEntry createEntry(Element xml)
{
   return new EntryImpl(xml);
}




/********************************************************************************/
/*										*/
/*	Id methods								*/
/*										*/
/********************************************************************************/

private synchronized long getNextId()
{
   if (note_conn == null) return 0;

   while (id_count <= 0 && note_conn != null) {
      BoardLog.logD("BNOTE","GET NEXT ID");
      if (use_begin == null) {
	 try {
	    Statement st = note_conn.createStatement();
	    st.execute("BEGIN");
	    st.execute("COMMIT");
	    st.close();
	    use_begin = true;
	  }
	 catch (SQLException e) {
	    // BoardLog.logD("BNOTE","Attempt to do BEGIN/COMMIT failed: " + e);
	    use_begin = false;
	  }
       }
      try {
	 Statement st = note_conn.createStatement();
	 if (use_begin) {
	    st.executeUpdate("BEGIN");
	  }
	 ResultSet rs = st.executeQuery("SELECT nextid FROM IdNumber");
	 if (rs.next()) next_id = rs.getLong(1);
	 else next_id = 0;
	 id_count = id_request;
	 long next = next_id + id_request;
	 String upd = "UPDATE IdNumber SET nextid = " + next;
	 st.executeUpdate(upd);
	 if (use_begin) {
	    st.executeUpdate("COMMIT");
	  }
	 st.close();
	 break;
       }
      catch (SQLException e) {
	 BoardLog.logE("BNOTE","Problem getting more ids: " + e);
	 resetDatabase();
	 if (note_conn == null) break;
       }
    }

   if (id_count == 0) return 0;

   --id_count;
   return next_id++;
}



/********************************************************************************/
/*										*/
/*	Task implementation							*/
/*										*/
/********************************************************************************/

private class TaskImpl implements BnoteTask, BnoteValue {

   private long task_id;
   private String task_name;
   private String task_project;
   private String task_description;
   private Date start_date;
   private Date end_date;

   TaskImpl(long tid,String nm,String p,String d) {
      task_id = tid;
      task_name = nm;
      task_project = p;
      task_description = d;
      start_date = null;
      end_date = null;
    }

   TaskImpl(Element xml) {
      task_id = IvyXml.getAttrLong(xml,"ID");
      task_name = IvyXml.getAttrString(xml,"NAME");
      task_project = IvyXml.getAttrString(xml,"PROJECT");
      task_description = IvyXml.getTextElement(xml,"DESCRIPTION");
      start_date = IvyXml.getAttrDate(xml,"START");
      end_date = IvyXml.getAttrDate(xml,"END");
    }

   @Override public long getTaskId()			{ return task_id; }
   @Override public String getName()			{ return task_name; }
   @Override public String getProject() 		{ return task_project; }
   @Override public String getDescription()		{ return task_description; }

   @Override public String toString()			{ return task_name; }

   @Override public String getDatabaseValue() {
      return Long.toString(task_id);
    }

   @Override public Date getFirstTime() {
      if (start_date == null) loadDates();
      return start_date;
    }

   @Override public Date getLastTime() {
      if (end_date == null) loadDates();
      return end_date;
    }

   @Override public void outputXml(IvyXmlWriter xw) {
      xw.begin("TASK");
      xw.field("ID",task_id);
      xw.field("NAME",task_name);
      xw.field("PROJECT",task_project);
      xw.field("START",getFirstTime());
      xw.field("END",getLastTime());
      xw.cdataElement("DESCRIPTION",task_description);
      xw.end("TASK");
    }

   void noteUse()					{ end_date = new Date(); }

   private void loadDates() {
      List<Date> dts = getDatesForTask(task_project,this);
      if (dts == null || dts.size() == 0) {
         start_date = new Date();
         end_date = start_date;
       }
      else {
	 start_date = dts.get(0);
	 end_date = dts.get(dts.size()-1);
       }
    }

}	// end of inner class TaskImpl



/********************************************************************************/
/*										*/
/*	Entry implementation							*/
/*										*/
/********************************************************************************/

private class EntryImpl implements BnoteEntry {

   private int	entry_id;
   private String entry_project;
   private BnoteTask entry_task;
   private BnoteEntryType entry_type;
   private String entry_user;
   private Date entry_time;
   private Map<String,String> prop_set;

   EntryImpl(ResultSet rs) throws SQLException {
      entry_id = rs.getInt("id");
      entry_project = rs.getString("project");
      int tid = rs.getInt("taskid");
      entry_task = findTaskById(tid);
      entry_user = rs.getString("username");
      entry_time = rs.getTimestamp("time");
      String typ = rs.getString("type");
      entry_type = BnoteEntryType.NONE;
      if (typ != null) {
	 try {
	    entry_type = Enum.valueOf(BnoteEntryType.class,typ);
	  }
	 catch (IllegalArgumentException e) { }
       }
    }

   EntryImpl(Element xml) {
      entry_id = IvyXml.getAttrInt(xml,"ID");
      entry_project = IvyXml.getAttrString(xml,"PROJECT");
      Element tel = IvyXml.getChild(xml,"TASK");
      entry_task = findTaskById(tel);
      entry_user = IvyXml.getAttrString(xml,"USER");
      entry_time = IvyXml.getAttrDate(xml,"TIME");
      entry_type = IvyXml.getAttrEnum(xml,"TYPE",BnoteEntryType.NONE);
      prop_set = new HashMap<String,String>();
      for (Element pel : IvyXml.children(xml,"PROP")) {
	 String k = IvyXml.getAttrString(pel,"KEY");
	 String v = IvyXml.getAttrString(pel,"VALUE");
	 prop_set.put(k,v);
       }
    }


   @Override public String getProject() 		{ return entry_project; }
   @Override public BnoteTask getTask() 		{ return entry_task; }
   @Override public BnoteEntryType getType()		{ return entry_type; }
   @Override public String getUser()			{ return entry_user; }
   @Override public Date getTime()			{ return entry_time; }

   @Override public String getProperty(String id) {
      loadProperties();
      return prop_set.get(id);
    }

   @Override public Set<String> getPropertyNames() {
      loadProperties();
      return prop_set.keySet();
    }

   private void loadProperties() {
      synchronized (BnoteDatabase.this) {
	 if (prop_set != null) return;
	 prop_set = new HashMap<String,String>();
	 try {
	    String q = "SELECT P.id, P.value FROM Prop P WHERE P.entry = ?";
	    PreparedStatement s = note_conn.prepareStatement(q);
	    s.setInt(1,entry_id);
	    ResultSet rs = s.executeQuery();
	    while (rs.next()) {
	       String k = rs.getString(1);
	       String v = rs.getString(2);
	       prop_set.put(k,v);
	    }
	    s.close();
	 }
	 catch (SQLException e) {
	    BoardLog.logE("BNOTE","Problem getting properties",e);
	 }
      }
    }

   @Override public void outputXml(IvyXmlWriter xw) {
      xw.begin("ENTRY");
      xw.field("ID",entry_id);
      xw.field("PROJECT",entry_project);
      xw.field("USER",entry_user);
      xw.field("TIME",entry_time);
      xw.field("TYPE",entry_type);
      if (entry_task != null) entry_task.outputXml(xw);
      if (prop_set != null) {
	 for (Map.Entry<String,String> ent : prop_set.entrySet()) {
	    xw.begin("PROP");
	    xw.field("KEY",ent.getKey());
	    xw.field("VALUE",ent.getValue());
	  }
       }
    }


}	// end of inner class EntryImpl



}	// end of class BnoteDatabase




/* end of BnoteDatabase.java */


