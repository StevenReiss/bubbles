/********************************************************************************/
/*										*/
/*		BeduCourseData.java						*/
/*										*/
/*	Data for a particular course						*/
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



package edu.brown.cs.bubbles.bedu;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.bump.BumpClient;

import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


class BeduCourseData implements BeduConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String	course_name;
private String	initial_workspace;
private String	workspace_url;
private String	workspace_path;
private boolean ask_workspace;
private String	help_url;
private String	survey_url;
private boolean do_chat;
private List<AssignmentData> course_assignments;

private static DateFormat [] date_formats = new DateFormat [] {
   new SimpleDateFormat("HH:mm dd/MM/yy"),
   new SimpleDateFormat("dd/MM/yy HH:mm")
};




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BeduCourseData(Element xml)
{
   course_name = IvyXml.getAttrString(xml,"NAME");
   Element ws = IvyXml.getChild(xml,"WORKSPACE");
   workspace_path = IvyXml.getTextElement(ws,"PATH");
   initial_workspace = IvyXml.getTextElement(ws,"ZIP");
   workspace_url = IvyXml.getTextElement(ws,"URL");
   ask_workspace = IvyXml.getAttrBool(ws,"ASK");
   help_url = IvyXml.getTextElement(ws,"HELP");
   survey_url = IvyXml.getTextElement(ws,"SURVEY");
   do_chat = IvyXml.getAttrBool(ws,"CHAT");
   course_assignments = new ArrayList<AssignmentData>();
   for (Element ae : IvyXml.children(xml,"ASSIGNMENT")) {
      course_assignments.add(new AssignmentData(ae));
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getName()			{ return course_name; }

List<Assignment> getAllAssignments()
{
   return new ArrayList<Assignment>(course_assignments);
}

String getSurveyUrl()			{ return survey_url; }

String getHelpUrl()			{ return help_url; }

boolean doChat()			{ return do_chat; }




/********************************************************************************/
/*										*/
/*	Methods to setup initial workspace					*/
/*										*/
/********************************************************************************/

void setupWorkspace() throws IOException
{
   File f1 = new File(System.getProperty("user.home"));
   StringTokenizer tok = new StringTokenizer(workspace_path,"/");
   while (tok.hasMoreTokens()) {
      String nm = tok.nextToken();
      f1 = new File(f1,nm);
    }
   if (!f1.mkdirs()) throw new IOException("Problem creating workspace directory");

   loadDirectory(initial_workspace,workspace_url,f1,true);

   BoardSetup setup = BoardSetup.getSetup();
   BoardProperties props = BoardProperties.getProperties("System");
   setup.setDefaultWorkspace(f1.getAbsolutePath());
   props.setProperty("edu.brown.cs.bubbles.workspace",f1.getAbsolutePath());
   setup.setAskWorkspace(false);
   props.setProperty("edu.brown.cs.bubbles.ask_workspace",ask_workspace);
   props.setProperty("edu.brown.cs.bubbles.auto_update",false);

   props.save();
}




private void loadDirectory(String file,String url,File dir,boolean crlf) throws IOException
{
   if (crlf && !System.getProperty("line.separator").equals("\r\n")) crlf = false;

   File cdir = BeduFactory.getCourseDirectory();
   File zipfile = new File(cdir,initial_workspace);
   if (zipfile.exists() && zipfile.canRead()) {
      unzip(zipfile,dir,true);
    }
   else {
      try {
	 URI u = new URI(url);
	 HttpURLConnection c = (HttpURLConnection) u.toURL().openConnection();
	 if (c.getResponseCode() == HttpURLConnection.HTTP_OK) {
	    InputStream ins = c.getInputStream();
	    File tf = File.createTempFile("bubbles",".ws");
	    tf.deleteOnExit();
	    try (FileOutputStream ots = new FileOutputStream(tf)) {
	       byte [] buf = new byte[16384];
	       for ( ; ; ) {
		  int ln = ins.read(buf);
		  if (ln <= 0) break;
		  ots.write(buf,0,ln);
	       }
	    }
	    ins.close();
	    unzip(tf,dir,true);
	    tf.delete();
	 }
      }
      catch (URISyntaxException e) {
	 throw new IOException("BAD URI",e);
      }
    }
}



private void unzip(File zipf,File dir,boolean crlf) throws IOException
{
   ZipFile zip = new ZipFile(zipf);
   Enumeration<?> files = zip.entries();
   while (files.hasMoreElements()) {
      ZipEntry file = (ZipEntry) files.nextElement();
      String nm = file.getName().replace("/",File.separator);
      File f = new File(dir,nm);
      if (file.isDirectory()) { // if its a directory, create it
	 f.mkdir();
	 continue;
       }
      InputStream is1 = zip.getInputStream(file); // get the input stream
      InputStream is = new BufferedInputStream(is1);
      OutputStream fos = new BufferedOutputStream(new FileOutputStream(f));

      while (is.available() > 0) {  // write contents of 'is' to 'fos'
	 int ch = is.read();
	 if (crlf && nm.endsWith(".java") && ch == '\n') fos.write('\r');
	 fos.write(ch);
       }
      fos.close();
      is.close();
    }
   zip.close();
}




/********************************************************************************/
/*										*/
/*	Assignment Information							*/
/*										*/
/********************************************************************************/

private class AssignmentData implements Assignment {

   private String assignment_name;
   private String assignment_description;
   private Date available_date;
   private Date due_date;
   private String project_name;
   private boolean fix_crlf;
   private String initial_project;
   private String initial_url;
   private String handin_script;
   private String assignment_survey;
   private String documentation_url;

   AssignmentData(Element xml) {
      assignment_name = IvyXml.getAttrString(xml,"NAME");
      assignment_description = IvyXml.getTextElement(xml,"DESCRIPTION");
      available_date = getDate(IvyXml.getTextElement(xml,"AVAIL"));
      due_date = getDate(IvyXml.getTextElement(xml,"DUE"));
      project_name = IvyXml.getTextElement(xml,"PROJECT");
      fix_crlf = IvyXml.getAttrBool(xml,"FIXCRLF",true);
      if (!System.getProperty("line.separator").equals("\r\n")) fix_crlf = false;
      initial_project = IvyXml.getTextElement(xml,"ECLIPSE");
      initial_url = IvyXml.getTextElement(xml,"ECLIPSEURL");
      handin_script = IvyXml.getTextElement(xml,"HANDIN");
      documentation_url = IvyXml.getTextElement(xml,"DOCUMENTATION");
      assignment_survey = IvyXml.getTextElement(xml,"SURVEY");
    }

   @Override public String getName()		{ return assignment_name; }
   @Override public String getDescription()	{ return assignment_description; }
   @Override public String getDocumentationUrl() { return documentation_url; }
   @Override public String getSurveyUrl()	{ return assignment_survey; }

   @Override public boolean isCurrent() {
      Date now = new Date();
      if (available_date != null && available_date.after(now)) return false;
      if (due_date != null && due_date.after(now)) return true;
      return false;
    }

   @Override public boolean isPast() {
      Date now = new Date();
      if (available_date != null && available_date.after(now)) return false;
      if (due_date != null && due_date.after(now)) return false;
      return true;
    }

   @Override public boolean isInUserWorkspace() {
      BumpClient bc = BumpClient.getBump();
      Element pelt = bc.getProjectData(project_name);
      if (pelt == null) return false;
      return true;
    }

   @Override public boolean canSubmit() {
      if (handin_script == null) return false;
      if (!isPast() && !isCurrent()) return false;
      if (!isInUserWorkspace()) return false;
      return true;
    }

   @Override public boolean createProject() {
      File ws = new File(BoardSetup.getSetup().getDefaultWorkspace());
      File pdir = new File(ws,project_name);
      if (pdir.exists()) return false;
      try {
	 loadDirectory(initial_project,initial_url,pdir,fix_crlf);
	 BumpClient bc = BumpClient.getBump();
	 bc.importProject(project_name);
       }
      catch (IOException e) {
	 return false;
       }
      return true;
    }

   @Override public String handin() {
      Map<String,String> props = new HashMap<>();
      File ws = new File(BoardSetup.getSetup().getDefaultWorkspace());
      File pdir = new File(ws,project_name);
      props.put("PROJECT",project_name);
      props.put("PROJECTDIR",pdir.getAbsolutePath());
      String cmd = IvyFile.expandName(handin_script,props);
      StringBuffer buf = new StringBuffer();
      buf.append("<html>");
      try {
         IvyExec ex = new IvyExec(cmd,IvyExec.READ_OUTPUT);
         InputStream ins = ex.getInputStream();
         for ( ; ; ) {
            int b = ins.read();
            if (b < 0) break;
            b &= 0xff;
            char c = (char) b;
            buf.append(c);
            if (c == '\n') buf.append("<br>");
          }
         ex.waitFor();
       }
      catch (IOException e) {
         buf.append("<br><b>Problem doing submit: " + e);
       }
   
      return buf.toString();
    }

   private Date getDate(String txt) {
      if (txt == null) return null;
      for (DateFormat df : date_formats) {
	 try {
	    return df.parse(txt);
	 }
	 catch (ParseException e) { }
       }
      BoardLog.logE("BEDU","Can't parse date " + txt);
      return null;
    }

}	// end of inner class AssignmentData


}	// end of class BeduCourseData




/* end of BeduCourseData.java */

