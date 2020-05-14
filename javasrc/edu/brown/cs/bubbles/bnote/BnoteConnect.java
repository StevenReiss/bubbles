/********************************************************************************/
/*										*/
/*		BnoteConnect.java						*/
/*										*/
/*	Database interface for handling programmer's log database connections   */
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
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;

import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.exec.IvyExecQuery;
import edu.brown.cs.ivy.file.IvyFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;


class BnoteConnect implements BnoteConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BoardProperties 	bnote_props;
private boolean 		database_okay;
private Connection		bnote_conn;

private String			database_type;
private String			database_host;
private String			database_user;
private String			database_password;
private String			database_prefix;
private String			database_default;
private String			database_name;
private String			database_texttype;
private String			database_longtype;
private String			database_blobtype;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BnoteConnect()
{
   bnote_props = BoardProperties.getProperties("Bnote");
   database_okay = setupAccess();
   bnote_conn = null;
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

private boolean setupAccess()
{
   String pfx = "Bnote.notebook.database";
   String ws = BoardSetup.getSetup().getDefaultWorkspace();
   ws = ws.replace(File.separator,".");
   String sfx = "";

   for ( ; ; ) {
      String typ = bnote_props.getProperty(pfx + ".type" + "." + ws);
      if (typ != null) {
	 sfx = "." + ws;
	 break;
       }
      int idx = ws.indexOf(".");
      if (idx < 0) break;
      ws = ws.substring(idx+1);
    }

   database_type = bnote_props.getProperty(pfx + ".type" + sfx);
   if (database_type == null) return false;

   database_host = bnote_props.getProperty(pfx + ".host" + sfx);
   if (database_host == null) database_host = bnote_props.getProperty(pfx + ".host");

   database_user = bnote_props.getProperty(pfx + ".user" + sfx);
   if (database_user == null) database_user = bnote_props.getProperty(pfx + ".user");
   database_password = bnote_props.getProperty(pfx + ".password" + sfx);
   if (database_password == null) database_password = bnote_props.getProperty(pfx + ".password");
   database_name = bnote_props.getProperty(pfx + ".name" + sfx);
   if (database_name == null) database_name = bnote_props.getProperty(pfx + ".name");

   switch (database_type) {
      case "postgresql" :
      case "mysql" :
	 if (database_name == null || database_name.contains("/")) {
	    database_name = "programmerslog_$(WSNAME)";
	  }
	 break;
      default :
      case "embed" :
	 if (database_name == null || !database_name.contains("/")) {
	    database_name = "$(WORK)/ProgrammersLog";
	  }
	 break;
    }

   Map<String,String> keys = new HashMap<String,String>();
   keys.put("WORKSPACE",BoardSetup.getSetup().getDefaultWorkspace());
   String wsnm = BoardSetup.getSetup().getDefaultWorkspace();
   int idx1 = wsnm.lastIndexOf(File.separator);
   if (idx1 >= 0) wsnm = wsnm.substring(idx1+1);
   keys.put("WSNAME",wsnm);

   keys.put("BUBBLES",BoardSetup.getPropertyBase().getPath());
   keys.put("WORK",BoardSetup.getBubblesWorkingDirectory().getPath());
   keys.put("PLUGIN",BoardSetup.getBubblesPluginDirectory().getPath());
   database_host = IvyFile.expandName(database_host,keys);
   database_name = IvyFile.expandName(database_name,keys);

   database_prefix = bnote_props.getProperty("Bnote.database.prefix." + database_type);
   database_default = bnote_props.getProperty("Bnote.database.default." + database_type);
   database_texttype = bnote_props.getProperty("Bnote.database.text." + database_type,"text");
   database_longtype = bnote_props.getProperty("Bnote.database.long." + database_type,"bigint");
   database_blobtype = bnote_props.getProperty("Bnote.database.blob." + database_type,"blob");

   String jdbc = bnote_props.getProperty("Bnote.database.jdbc." + database_type);
   if (jdbc == null) return false;

   try {
      // Class.forName(jdbc).newInstance();
      Class.forName(jdbc);
      BoardLog.logD("BNOTE",database_type + " database driver loaded");
    }
   catch (Throwable t) {
      BoardLog.logD("BNOTE","Can't load " + database_type + " database driver",t);
      return false;
    }

   if (database_type.equals("embed")) {
      checkEmbedPermissions();
    }
   if (database_type.equals("derby")) {
      setupDerbyServer();
    }

   return true;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getDatabaseType()			{ return database_type; }




/********************************************************************************/
/*										*/
/*	Database connection methods						*/
/*										*/
/********************************************************************************/

Connection getLogDatabase()
{
   if (!database_okay) return null;
   if (bnote_conn != null) return bnote_conn;

   String url = "jdbc:" + database_prefix;
   if (!url.endsWith(":")) url += ":";
   if (database_host != null) {
      url += "//" + database_host + "/";
    }
   url += database_name;

   Properties props = new Properties();
   if (database_user != null) props.put("user",database_user);
   if (database_password != null) props.put("password",database_password);

   try {
      bnote_conn = DriverManager.getConnection(url,props);
    }
   catch (SQLException e) {
      BoardLog.logW("BNOTE","Problem opening database: " + e);
   }

   if (bnote_conn != null) return bnote_conn;

   if (database_default == null) ;
   else if (database_default.startsWith("?")) {
      StringTokenizer tok = new StringTokenizer(database_default,"?&=");
      while (tok.hasMoreTokens()) {
	 String k = tok.nextToken();
	 String v = tok.nextToken();
	 props.put(k,v);
       }
      try {
	 bnote_conn = DriverManager.getConnection(url,props);
       }
      catch (SQLException e) {
	 // TODO: Should warn the user here
	 BoardLog.logE("BNOTE","Problem creating database: " + e,e);
      }
    }
   else {
      String durl = "jdbc:" + database_prefix;
      if (!durl.endsWith(":")) durl += ":";
      if (database_host != null) {
	 durl += "//" + database_host + "/";
       }
      durl += database_default;
      Connection conn = null;
      try {
	 conn = DriverManager.getConnection(durl,props);
	 Statement st = conn.createStatement();
	 try {
	    st.execute("DROP DATABASE " + database_name);
	  }
	 catch (SQLException e) { }
	 st.execute("CREATE DATABASE " + database_name);
	 st.close();
	 conn.close();
	 bnote_conn = DriverManager.getConnection(url,props);
       }
      catch (SQLException e) {
	 BoardLog.logE("BNOTE","Problem creating log database",e);
      }
    }

   if (bnote_conn == null) {
      database_okay = false;
      return null;
    }

   try {
      setupDatabase();
    }
   catch (SQLException e) {
      BoardLog.logE("BNOTE","Problem setting up database",e);
      database_okay = false;
      bnote_conn = null;
    }

   return bnote_conn;
}




/********************************************************************************/
/*										*/
/*	Define a new database							*/
/*										*/
/********************************************************************************/

private void setupDatabase() throws SQLException
{
   Statement s = bnote_conn.createStatement();
   s.execute("CREATE TABLE IdNumber ( nextid " + database_longtype + ")");
   s.execute("INSERT INTO IdNumber VALUES ( 1 )");
   s.execute("CREATE TABLE Entry (id " + database_longtype + "," +
		"project varchar(64),taskid " + database_longtype + ",type varchar(16)," +
		"username varchar(64),time timestamp default CURRENT_TIMESTAMP)");
   s.execute("CREATE TABLE Prop (entry " + database_longtype + ",id varchar(32)," +
		"value " + database_texttype + ")");
   s.execute("CREATE TABLE Attachment (id " + database_longtype + "," +
		"source varchar(1024),data " + database_blobtype + ")");
   s.execute("CREATE TABLE Task (id " + database_longtype + "," +
		"name varchar(1024),description " + database_texttype + "," +
		"project varchar(64))");
   s.close();
}




/********************************************************************************/
/*										*/
/*	Handle derby server setup						*/
/*										*/
/********************************************************************************/

private void setupDerbyServer()
{
   BoardSetup bs = BoardSetup.getSetup();
   String dlib1 = bs.getLibraryPath("derby.jar");
   String dlib2 = bs.getLibraryPath("derbynet.jar");
   String dlib3 = bs.getLibraryPath("derbyclient.jar");
   String cp = dlib1 + File.pathSeparator + dlib2 + File.pathSeparator + dlib3;

   File pbase = BoardSetup.getPropertyBase();

   File flock = new File(pbase,"derby.lock");
   try {
      for (int i = 0; i < 100; ++i) {
	 try {
	    if (flock.createNewFile()) {
	       break;
	     }
	  }
	 catch (IOException e) { }
	 try {
	    Thread.sleep(100);
	  }
	 catch (InterruptedException e) { }
       }
      flock.deleteOnExit();	// if it was already there, take it over

      String host = database_host;
      if (host == null) {
	 File f2 = new File(pbase,".derby");
	 if (f2.exists()) {
	    try {
	       BufferedReader br = new BufferedReader(new FileReader(f2));
	       host = br.readLine();
	       br.close();
	     }
	    catch (IOException e) {
	       host = null;
	     }
	  }
       }
      if (host != null) {
	 try {
	    String cmd = "java -cp " + cp + " org.apache.derby.drda.NetworkServerControl ping -h " + host;
	    IvyExec exec = new IvyExec(cmd);
	    int sts = exec.waitFor();
	    if (sts == 0) {
	       database_host = host;
	       return;			// things are working
	     }
	  }
	 catch (IOException e) {
	    host = null;
	  }
       }

      host = IvyExecQuery.getHostName();
      try {
	 String cmd = "sh -c 'java -cp " + cp + " org.apache.derby.drda.NetworkServerControl start -h " +
	    host + " -noSecurityManager &'";
	 new IvyExec(cmd,IvyExec.IGNORE_OUTPUT);
	 String cmdp = "java -cp " + cp + " org.apache.derby.drda.NetworkServerControl ping -h " + host;
	 int sts = 1;
	 for (int i = 0; i < 100; ++i) {
	    IvyExec execp = new IvyExec(cmdp);
	    sts = execp.waitFor();
	    if (sts == 0) break;
	    try {
	       Thread.sleep(100);
	    }
	    catch (InterruptedException e) { }
	 }

	 if (sts == 0) {
	    File f2 = new File(pbase,".derby");
	    FileWriter fw = new FileWriter(f2);
	    PrintWriter pw = new PrintWriter(fw);
	    pw.println(host);
	    fw.close();
	    database_host = host;
	  }
       }
      catch (IOException e) {
       }
    }
   finally {
      flock.delete();
    }
}



private void checkEmbedPermissions()
{
   boolean remove = false;
   File f1 = new File(database_name);
   if (f1.exists() && !f1.canWrite()) remove = true;

   if (remove) {
      try {
	 IvyFile.remove(f1);
       }
      catch (IOException e) {
	 // we tried
       }
    }
}




}	// end of class BnoteConnect




/* end of BnoteConnect.java */
