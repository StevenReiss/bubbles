/********************************************************************************/
/*										*/
/*		PybaseMain.java 						*/
/*										*/
/*	Python Bubbles Base Main program					*/
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


package edu.brown.cs.bubbles.pybase;

import edu.brown.cs.bubbles.pybase.debug.PybaseDebugManager;

import edu.brown.cs.ivy.exec.IvySetup;
import edu.brown.cs.ivy.file.IvyFile;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class PybaseMain implements PybaseConstants, MintConstants
{


/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   PybaseMain pm = new PybaseMain(args);

   pm.start();

   synchronized (pm) {
      while (!pm.shutdown_mint) {
	 try {
	    pm.wait(5000);
	  }
	 catch (InterruptedException e) { }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private File			root_directory;
private String			mint_handle;
private MintControl		mint_control;
private boolean 		shutdown_mint;
private int			num_clients;
private PybaseEditor		pybase_editor;
private PybaseProjectManager	project_manager;
private PybaseSearch		pybase_search;
private PybaseDebugManager	pybase_debug;
private File			work_directory;
private Object			send_sema;
private PybaseThreadPool	thread_pool;
private Timer			pybase_timer;
private Pinger			pybase_pinger;
private PybasePreferences	system_prefs;

static private PybaseMain		pybase_main;

private static PrintStream log_file = null;
private static PybaseLogLevel log_level = PybaseLogLevel.DEBUG;
private static boolean use_stderr = true;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public static PybaseMain getPybaseMain()	{ return pybase_main; }



private PybaseMain(String [] args)
{
   pybase_main = this;

   mint_handle = System.getProperty("edu.brown.cs.bubbles.MINT");
   if (mint_handle == null) mint_handle = System.getProperty("edu.brown.cs.bubbles.mint");
   if (mint_handle == null) mint_handle = PYBASE_MINT_NAME;
   String rd = System.getProperty("edu.brown.cs.bubbles.pybase.ROOT");
   if (rd == null) rd = "/pro/bubbles";
   root_directory = new File(rd);

   shutdown_mint = false;
   num_clients = 0;
   send_sema = new Object();
   system_prefs = null;

   String hm = System.getProperty("user.home");
   File f1 = new File(hm);
   File f2 = new File(f1,".bubbles");
   File f3 = new File(f2,".ivy");
   if (!f3.exists() || !IvySetup.setup(f3)) IvySetup.setup();

   File f5 = new File(hm,"Pybles");
   work_directory = new File(f5,"workspace");

   scanArgs(args);

   if (log_file == null) {
      try {
	 File f4 = new File(work_directory,"pybase_log.log");
	 log_file = new PrintStream(new FileOutputStream(f4),true);
       }
      catch (FileNotFoundException e) {
	 log_file = null;
	 PybaseMain.logE("Error initialising file: " + e.getMessage());
       }
    }

   logD("CLASSPATH = " + System.getProperty("java.class.path"));

   if (!work_directory.exists()) {
      work_directory.mkdirs();
    }
   File df = new File(work_directory,".metadata");
   if (!df.exists()) df.mkdir();
   df = new File(df,".pybase");
   if (!df.exists()) df.mkdir();
   File preffile = new File(df,"preferences");
   system_prefs = new PybasePreferences(preffile);

   thread_pool = new PybaseThreadPool();
   pybase_timer = new Timer("PybaseTimer",true);
   pybase_pinger = null;

   PybaseMain.logI("STARTING");
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void start()
{
   try {
      project_manager = new PybaseProjectManager(this,work_directory);
      pybase_editor = new PybaseEditor(this);
      pybase_search = new PybaseSearch(this);
      pybase_debug = PybaseDebugManager.getManager();
      project_manager.loadProjects();
    }
   catch (PybaseException e) {
      logE("Problem initializing: " + e,e);
      System.exit(1);
    }

   mint_control = MintControl.create(mint_handle,MintSyncMode.SINGLE);
   mint_control.register("<BUBBLES DO='_VAR_1' LANG='Python' />",new CommandHandler());
}



/********************************************************************************/
/*										*/
/*	Argument scanning methods						*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-m") && i+1 < args.length) {           // -m <mint handle>
	    mint_handle = args[++i];
	  }
	 else if (args[i].startsWith("-ws") && i+1 < args.length) {     // -ws <workspace>
	    work_directory = new File(args[++i]);
            work_directory = IvyFile.getCanonical(work_directory);
	  }
	 else badArgs();
       }
      else badArgs();
    }
}



private void badArgs()
{
   System.err.println("PYBLES: pybasemain [-m <mint>]");
   System.exit(1);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

PybaseProjectManager getProjectManager()		{ return project_manager; }

public PybaseProject getProject(String p) throws PybaseException
{
   return project_manager.findProject(p);
}

public IFileData getFileData(String fnm)
{
   PybaseFileManager pfm = PybaseFileManager.getFileManager();

   return pfm.getFileData(new File(fnm));
}

PybasePreferences getSystemPreferences()		{ return system_prefs; }


public File getWorkSpaceDirectory()			{ return work_directory; }

public File getRootDirectory()		{ return root_directory; }



/********************************************************************************/
/*										*/
/*	Command processing							*/
/*										*/
/********************************************************************************/

private String handleCommand(String cmd,String proj,Element xml) throws PybaseException
{
   PybaseMain.logI("Handle command " + cmd + " for " + proj);

   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("RESULT");

   if (cmd.equals("PING")) {
      xw.text("PONG");
    }
   else if (cmd.equals("PROJECTS")) {
      project_manager.handleListProjects(xw);
    }
   else if (cmd.equals("OPENPROJECT")) {
      project_manager.handleOpenProject(proj,IvyXml.getAttrBool(xml,"FILES",false),
					  IvyXml.getAttrBool(xml,"PATHS",false),
					  IvyXml.getAttrBool(xml,"CLASSES",false),
					  IvyXml.getAttrBool(xml,"OPTIONS",false),xw);
    }
   else if (cmd.equals("BUILDPROJECT")) {
      project_manager.handleBuildProject(proj,IvyXml.getAttrBool(xml,"CLEAN"),
				      IvyXml.getAttrBool(xml,"FULL"),
				      IvyXml.getAttrBool(xml,"REFRESH"),xw);
    }
   else if (cmd.equals("CREATEPROJECT")) {
      project_manager.handleCreateProject(IvyXml.getAttrString(xml,"NAME"),
	    IvyXml.getAttrString(xml,"DIR"),xw);
    }
   else if (cmd.equals("EDITPROJECT")) {
      project_manager.handleEditProject(proj,IvyXml.getChild(xml,"PROJECT"),xw);
    }
   else if (cmd.equals("CREATEPACKAGE")) {
      project_manager.handleCreatePackage(proj,
	    IvyXml.getAttrString(xml,"NAME"),
	    IvyXml.getAttrBool(xml,"FORCE"),xw);
    }
   else if (cmd.equals("FINDPACKAGE")) {
      project_manager.handleFindPackage(proj,
	    IvyXml.getAttrString(xml,"NAME"),xw);
    }
   else if (cmd.equals("CREATECLASS")) {
      project_manager.handleNewModule(proj,
	    IvyXml.getAttrString(xml,"NAME"),
	    IvyXml.getAttrBool(xml,"FORCE"),
	    IvyXml.getTextElement(xml,"CONTENTS"),xw);
    }
   else if (cmd.equals("STARTFILE")) {
      pybase_editor.handleStartFile(proj,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrString(xml,"ID"),
	       IvyXml.getAttrBool(xml,"CONTENTS",false),xw);
    }
   else if (cmd.equals("EDITPARAM")) {
      pybase_editor.handleParameter(IvyXml.getAttrString(xml,"BID","*"),
	    IvyXml.getAttrString(xml,"NAME"),
	    IvyXml.getAttrString(xml,"VALUE"));
    }
   else if (cmd.equals("EDITFILE")) {
      pybase_editor.handleEdit(proj,IvyXml.getAttrString(xml,"BID","*"),
	    IvyXml.getAttrString(xml,"FILE"),
	    IvyXml.getAttrString(xml,"ID"),
	    getEditSet(xml),xw);
    }
   else if (cmd.equals("COMMIT")) {
      pybase_editor.handleCommit(proj,IvyXml.getAttrString(xml,"BID","*"),
	    IvyXml.getAttrBool(xml,"REFRESH",false),
	    IvyXml.getAttrBool(xml,"SAVE",false),
	    getElements(xml,"FILE"),xw);
    }
   else if (cmd.equals("ELIDESET")) {
      pybase_editor.elisionSetup(proj,IvyXml.getAttrString(xml,"BID","*"),
	    IvyXml.getAttrString(xml,"FILE"),
	    IvyXml.getAttrBool(xml,"COMPUTE",true),
	    getElements(xml,"REGION"),xw);
    }
   else if (cmd.equals("PATTERNSEARCH")) {
      pybase_search.handlePatternSearch(proj,IvyXml.getAttrString(xml,"PATTERN"),
	    IvyXml.getAttrEnum(xml,"FOR",SearchFor.NONE),
	    IvyXml.getAttrBool(xml,"DEFS",true),
	    IvyXml.getAttrBool(xml,"REFS",true),
	    IvyXml.getAttrBool(xml,"SYSTEM",false),
	    xw);
    }
   else if (cmd.equals("FINDBYKEY")) {
      pybase_search.handleKeySearch(proj,
	    IvyXml.getAttrString(xml,"FILE"),
	    IvyXml.getAttrString(xml,"KEY"),
	    xw);
    }
   else if (cmd.equals("FINDDEFINITIONS")) {
      pybase_search.handleFindAll(proj,IvyXml.getAttrString(xml,"FILE"),
	    IvyXml.getAttrInt(xml,"START"),IvyXml.getAttrInt(xml,"END"),
	    IvyXml.getAttrBool(xml,"DEFS",true),
	    IvyXml.getAttrBool(xml,"REFS",false),
	    IvyXml.getAttrBool(xml,"IMPLS",false),
	    IvyXml.getAttrBool(xml,"EQUIV",false),
	    IvyXml.getAttrBool(xml,"EXACT",false),
	    IvyXml.getAttrBool(xml,"SYSTEM",false),
	    IvyXml.getAttrBool(xml,"TYPE",false),
	    false,false,
	    xw);
    }
   else if (cmd.equals("FINDREFERENCES")) {
      pybase_search.handleFindAll(proj,IvyXml.getAttrString(xml,"FILE"),
	    IvyXml.getAttrInt(xml,"START"),IvyXml.getAttrInt(xml,"END"),
	    IvyXml.getAttrBool(xml,"DEFS",true),
	    IvyXml.getAttrBool(xml,"REFS",true),
	    IvyXml.getAttrBool(xml,"IMPLS",false),
	    IvyXml.getAttrBool(xml,"EQUIV",false),
	    IvyXml.getAttrBool(xml,"EXACT",false),
	    IvyXml.getAttrBool(xml,"SYSTEM",false),
	    IvyXml.getAttrBool(xml,"TYPE",false),
	    IvyXml.getAttrBool(xml,"RONLY",false),
	    IvyXml.getAttrBool(xml,"WONLY",false),
	    xw);
    }
   else if (cmd.equals("GETFULLYQUALIFIEDNAME")) {
      pybase_search.getFullyQualifiedName(proj,
	    IvyXml.getAttrString(xml,"FILE"),
	    IvyXml.getAttrInt(xml,"START"),
	    IvyXml.getAttrInt(xml,"END"),xw);
    }
   else if (cmd.equals("FINDREGIONS")) {
      pybase_search.getTextRegions(proj,IvyXml.getAttrString(xml,"BID","*"),
	    IvyXml.getAttrString(xml,"FILE"),
	    IvyXml.getAttrString(xml,"CLASS"),
	    IvyXml.getAttrBool(xml,"PREFIX",false),
	    IvyXml.getAttrBool(xml,"STATICS",false),
	    IvyXml.getAttrBool(xml,"COMPUNIT",false),
	    IvyXml.getAttrBool(xml,"IMPORTS",false),
	    IvyXml.getAttrBool(xml,"PACKAGE",false),
	    IvyXml.getAttrBool(xml,"TOPDECLS",false),
	    IvyXml.getAttrBool(xml,"MAIN",false),
            IvyXml.getAttrBool(xml,"FIELDS",false),
	    IvyXml.getAttrBool(xml,"ALL",false),xw);
    }
   else if (cmd.equals("SEARCH")) {
      pybase_search.handleTextSearch(proj,IvyXml.getAttrInt(xml,"FLAGS",0),
	    IvyXml.getTextElement(xml,"PATTERN"),
	    IvyXml.getAttrInt(xml,"MAX",MAX_TEXT_SEARCH_RESULTS),
	    xw);
    }
   else if (cmd.equals("ENTER")) {
      if (num_clients == 0 && pybase_pinger == null) {
	 pybase_pinger = new Pinger();
	 pybase_timer.schedule(pybase_pinger,30000,30000);
       }
      ++num_clients;
      xw.text(Integer.toString(num_clients));
    }
   else if (cmd.equals("EXIT")) {
      if (--num_clients <= 0) {
	 PybaseMain.logD("Stopping application");
	 shutdown_mint = true;
       }
    }
   else if (cmd.equals("LOGLEVEL")) {
      log_level = IvyXml.getAttrEnum(xml,"LEVEL",PybaseLogLevel.ERROR);
    }
   else if (cmd.equals("GETALLNAMES")) {
      project_manager.handleGetAllNames(proj,IvyXml.getAttrString(xml,"BID","*"),
					   getSet(xml,"FILE"),
					   IvyXml.getAttrString(xml,"BACKGROUND"),xw);
    }
   else if (cmd.equals("GETRUNCONFIG")) {
      pybase_debug.getRunConfigurations(xw);
    }
   else if (cmd.equals("NEWRUNCONFIG")) {
      pybase_debug.getNewRunConfiguration(proj,
	    IvyXml.getAttrString(xml,"NAME"),
	    IvyXml.getAttrString(xml,"CLONE"),xw);
    }
   else if (cmd.equals("EDITRUNCONFIG")) {
      pybase_debug.editRunConfiguration(IvyXml.getAttrString(xml,"LAUNCH"),
	    IvyXml.getAttrString(xml,"PROP"),
	    IvyXml.getAttrString(xml,"VALUE"),xw);
    }
   else if (cmd.equals("SAVERUNCONFIG")) {
      pybase_debug.saveRunConfiguration(IvyXml.getAttrString(xml,"LAUNCH"),xw);
    }
   else if (cmd.equals("DELETERUNCONFIG")) {
      pybase_debug.deleteRunConfiguration(IvyXml.getAttrString(xml,"LAUNCH"),xw);
    }
   else if (cmd.equals("GETALLBREAKPOINTS")) {
      pybase_debug.getAllBreakpoints(xw);
    }
   else if (cmd.equals("ADDLINEBREAKPOINT")) {
      pybase_debug.setLineBreakpoint(proj,IvyXml.getAttrString(xml,"BID","*"),
	    IvyXml.getTextElement(xml,"FILE"),
	    IvyXml.getAttrInt(xml,"LINE"),
	    IvyXml.getAttrBool(xml,"SUSPENDVM",false),
	    IvyXml.getAttrBool(xml,"TRACE",false));
    }
   else if (cmd.equals("ADDEXCEPTIONBREAKPOINT")) {
      pybase_debug.setExceptionBreakpoint(proj,IvyXml.getAttrString(xml,"CLASS"),
					     IvyXml.getAttrBool(xml,"CAUGHT",false),
					     IvyXml.getAttrBool(xml,"UNCAUGHT",true),
					     IvyXml.getAttrBool(xml,"CHECKED",false),
					     IvyXml.getAttrBool(xml,"SUSPENDVM",false));
    }
   else if (cmd.equals("EDITBREAKPOINT")) {
      pybase_debug.editBreakpoint(IvyXml.getAttrString(xml,"ID"),
	    IvyXml.getAttrString(xml,"PROP"),
	    IvyXml.getAttrString(xml,"VALUE"),
	    IvyXml.getAttrString(xml,"PROP1"),
	    IvyXml.getAttrString(xml,"VALUE1"),
	    IvyXml.getAttrString(xml,"PROP2"),
	    IvyXml.getAttrString(xml,"VALUE2"));
    }
   else if (cmd.equals("CLEARALLLINEBREAKPOINTS")) {
      pybase_debug.clearLineBreakpoints(proj,null,0);
    }
   else if (cmd.equals("CLEARLINEBREAKPOINT")) {
      pybase_debug.clearLineBreakpoints(proj,IvyXml.getAttrString(xml,"FILE"),
	    IvyXml.getAttrInt(xml,"LINE"));
    }
   else if (cmd.equals("START")) {
      pybase_debug.runProject(IvyXml.getAttrString(xml,"NAME"),xw);
    }
   else if (cmd.equals("DEBUGACTION")) {
      pybase_debug.debugAction(IvyXml.getAttrString(xml,"LAUNCH"),
	    IvyXml.getAttrString(xml,"TARGET"),
	    IvyXml.getAttrString(xml,"PROCESS"),
	    IvyXml.getAttrString(xml,"THREAD"),
	    IvyXml.getAttrString(xml,"FRAME"),
	    IvyXml.getAttrEnum(xml,"ACTION",PybaseDebugAction.NONE),xw);
    }
   else if (cmd.equals("CONSOLEINPUT")) {
      pybase_debug.consoleInput(IvyXml.getAttrString(xml,"LAUNCH"),
	    IvyXml.getTextElement(xml,"INPUT"));
    }
   else if (cmd.equals("GETSTACKFRAMES")) {
      pybase_debug.getStackFrames(IvyXml.getAttrString(xml,"LAUNCH"),
	    IvyXml.getAttrString(xml,"THREAD"),
	    IvyXml.getAttrInt(xml,"COUNT",-1),
	    IvyXml.getAttrInt(xml,"DEPTH",0),xw);
    }
   else if(cmd.equals("VARVAL")) {
      pybase_debug.getVariableValue(IvyXml.getAttrString(xml,"THREAD"),
	    IvyXml.getAttrString(xml,"FRAME"),
	    IvyXml.getTextElement(xml,"VAR"),
	    IvyXml.getAttrInt(xml,"DEPTH",1),xw);
    }
   else if(cmd.equals("VARDETAIL")) {
      pybase_debug.getVariableValue(IvyXml.getAttrString(xml,"THREAD"),
	    IvyXml.getAttrString(xml,"FRAME"),
	    IvyXml.getTextElement(xml,"VAR"),-1,xw);
    }
   else if (cmd.equals("EVALUATE")) {
      pybase_debug.evaluateExpression(proj,IvyXml.getAttrString(xml,"BID","*"),
	    IvyXml.getTextElement(xml,"EXPR"),
	    IvyXml.getAttrString(xml,"THREAD"),
	    IvyXml.getAttrString(xml,"FRAME"),
	    IvyXml.getAttrBool(xml,"IMPLICIT",false),
	    IvyXml.getAttrBool(xml,"BREAK",true),
	    IvyXml.getAttrString(xml,"REPLYID"),xw);
    }
   else if (cmd.equals("PREFERENCES")) { }
   else if (cmd.equals("FINDHIERARCHY")) { }
   else if (cmd.equals("GETHOST")) {
      handleGetHost(xw);
    }
   else {
      xw.close();
      throw new PybaseException("Unknown PYBASE command " + cmd);
    }

   xw.end("RESULT");

   PybaseMain.logD("Result = " + xw.toString());
   String rslt = xw.toString();

   xw.close();
   return rslt;
}



/********************************************************************************/
/*										*/
/*	Methods for extracting items from argument message			*/
/*										*/
/********************************************************************************/

private Set<String> getSet(Element xml,String key)
{
   Set<String> items = null;

   for (Element c : IvyXml.children(xml,key)) {
      String v = IvyXml.getText(c);
      if (v == null || v.length() == 0) continue;
      if (items == null) items = new HashSet<String>();
      items.add(v);
    }

   return items;
}


private List<Element> getElements(Element xml,String key)
{
   List<Element> elts = null;

   for (Element c : IvyXml.children(xml,key)) {
      if (elts == null) elts = new ArrayList<Element>();
      elts.add(c);
    }

   return elts;
}



/********************************************************************************/
/*										*/
/*	Methods for handling edits						*/
/*										*/
/********************************************************************************/

private List<IEditData> getEditSet(Element xml)
{
   List<IEditData> edits = new ArrayList<IEditData>();

   for (Element c : IvyXml.children(xml,"EDIT")) {
      EditDataImpl edi = new EditDataImpl(c);
      edits.add(edi);
    }

   return edits;
}


private static class EditDataImpl implements IEditData {

   private int start_offset;
   private int end_offset;
   private String edit_text;

   EditDataImpl(Element e) {
      start_offset = IvyXml.getAttrInt(e,"START");
      end_offset = IvyXml.getAttrInt(e,"END",start_offset);
      edit_text = IvyXml.getText(e);
      if (edit_text != null && edit_text.length() == 0) edit_text = null;
    }

   @Override public int getOffset()			{ return start_offset; }
   @Override public int getLength()			{ return end_offset - start_offset; }
   @Override public String getText()			{ return edit_text; }

}	// end of innerclass EditDataImpl





/********************************************************************************/
/*										*/
/*	Command handler for MINT						*/
/*										*/
/********************************************************************************/

private class CommandHandler implements MintHandler {

   @Override public void receive(MintMessage msg, MintArguments args) {
      String cmd = args.getArgument(1);
      Element xml = msg.getXml();
      String proj = IvyXml.getAttrString(xml,"PROJECT");
   
      String rslt = null;
   
      try {
         rslt = handleCommand(cmd,proj,xml);
       }
      catch (PybaseException e) {
         String xmsg = "PYBASE: error in command " + cmd + ": " + e;
         PybaseMain.logE(xmsg,e);
         rslt = "<ERROR><![CDATA[" + xmsg + "]]></ERROR>";
       }
      catch (Throwable t) {
         String xmsg = "PYBASE: Problem processing command " + cmd + ": " + t;
         PybaseMain.logE(xmsg);
         t.printStackTrace();
         StringWriter sw = new StringWriter();
         PrintWriter pw = new PrintWriter(sw);
         t.printStackTrace(pw);
         PybaseMain.logE("TRACE: " + sw.toString());
         for (Throwable xt = t.getCause(); xt != null; xt = xt.getCause()) {
            StringWriter xsw = new StringWriter();
            PrintWriter xpw = new PrintWriter(xsw);
            xt.printStackTrace(xpw);
            PybaseMain.logE("CAUSED BY: " + xsw.toString());
          }
   
         rslt = "<ERROR>";
         rslt += "<MESSAGE>" + xmsg + "</MESSAGE>";
         rslt += "<EXCEPTION><![CDATA[" + t.toString() + "]]></EXCEPTION>";
         rslt += "<STACK><![CDATA[" + sw.toString() + "]]></STACK>";
         rslt += "</ERROR>";
       }
   
      msg.replyTo(rslt);
   
      if (shutdown_mint) {
         mint_control.shutDown();
         synchronized (PybaseMain.this) {
            PybaseMain.this.notifyAll();
          }
       }
    }

}	// end of subclass CommandHandler



/********************************************************************************/
/*										*/
/*	Logging methods 							*/
/*										*/
/********************************************************************************/

static public void logE(String msg,Throwable t) { log(PybaseLogLevel.ERROR,msg,t); }

static public void logE(String msg)		{ log(PybaseLogLevel.ERROR,msg,null); }

static public void logW(String msg)		{ log(PybaseLogLevel.WARNING,msg,null); }

static public void logI(String msg)		{ log(PybaseLogLevel.INFO,msg,null); }

static public  void logD(String msg)		{ log(PybaseLogLevel.DEBUG,msg,null); }

static public void logX(String msg)
{
   Throwable t = new Throwable(msg);
   logE(msg,t);
}



static public void log(String msg)		{ logI(msg); }



static public void log(PybaseLogLevel lvl,String msg,Throwable t)
{
   if (lvl.ordinal() > log_level.ordinal()) return;

   String s = lvl.toString().substring(0,1);
   String pfx = "PYBASE:" + s + ": ";

   if (log_file != null) {
      log_file.println(pfx + msg);
      if (t != null) t.printStackTrace(log_file);
    }
   if (use_stderr || log_file == null) {
      System.err.println(pfx + msg);
      if (t != null) t.printStackTrace();
      System.err.flush();
    }
   if (log_file != null) log_file.flush();
}




/********************************************************************************/
/*										*/
/*	Simple processing methods						*/
/*										*/
/********************************************************************************/

private void handleGetHost(IvyXmlWriter xw)
{
   String h1 = null;
   String h2 = null;
   String h3 = null;
   try {
      InetAddress lh = InetAddress.getLocalHost();
      h1 = lh.getHostAddress();
      h2 = lh.getHostName();
      h3 = lh.getCanonicalHostName();
    }
   catch (IOException e) { }
   if (h1 != null) xw.field("ADDR",h1);
   if (h2 != null) xw.field("NAME",h2);
   if (h3 != null) xw.field("CNAME",h3);
}




/********************************************************************************/
/*										*/
/*	Message Routines							*/
/*										*/
/********************************************************************************/

public IvyXmlWriter beginMessage(String typ)
{
   return beginMessage(typ,null);
}


public IvyXmlWriter beginMessage(String typ,String bid)
{
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("PYBASE");
   xw.field("SOURCE","PYBASE");
   xw.field("TYPE",typ);
   if (bid != null) xw.field("BID",bid);

   return xw;
}


public void finishMessage(IvyXmlWriter xw)
{
   xw.end("PYBASE");

   sendMessage(xw.toString());
}


public String finishMessageWait(IvyXmlWriter xw)
{
   return finishMessageWait(xw,0);
}


public String finishMessageWait(IvyXmlWriter xw,long delay)
{
   xw.end("PYBASE");

   return sendMessageWait(xw.toString(),delay);
}



private void sendMessage(String msg)
{
   synchronized (send_sema) {
      PybaseMain.logD("Sending: " + msg);
      if (mint_control != null && !shutdown_mint)
	 mint_control.send(msg);
    }
}



private String sendMessageWait(String msg,long delay)
{
   MintDefaultReply rply = new MintDefaultReply();

   synchronized (send_sema) {
      PybaseMain.logD("Sending/w: " + msg);
      if (mint_control != null && !shutdown_mint) {
	 mint_control.send(msg,rply,MINT_MSG_FIRST_NON_NULL);
       }
      else return null;
    }

   return rply.waitForString(delay);
}



/********************************************************************************/
/*										*/
/*	Pinging task to determine if front end crashed				*/
/*										*/
/********************************************************************************/

private class Pinger extends TimerTask {

   private int error_count;

   Pinger() {
      error_count = 0;
    }

   @Override public void run() {
      if (shutdown_mint) return;
      if (num_clients == 0) return;
      IvyXmlWriter xw = beginMessage("PING");
      String rslt = finishMessageWait(xw,1000);
      if (rslt == null) {
	 ++error_count;
	 if (error_count > 5) {
	    num_clients = 0;
	    shutdown_mint = true;
	    mint_control.shutDown();
	    synchronized (PybaseMain.this) {
	       PybaseMain.this.notifyAll();
	     }
	  }
       }
      else error_count = 0;
    }

}

/********************************************************************************/
/*										*/
/*	Thread pool for background tasks					*/
/*										*/
/********************************************************************************/

public void startTask(Runnable r)
{
   if (r != null) thread_pool.execute(r);
}


public void startTaskDelayed(Runnable r,long delay)
{
   if (r == null) return;

   PybaseDelayExecute pde = new PybaseDelayExecute(r);
   pybase_timer.schedule(pde,delay);
}


public void finishTask(Runnable r)
{
   if (r != null) thread_pool.remove(r);
}



private static class PybaseThreadPool extends ThreadPoolExecutor implements ThreadFactory {

   private static int thread_counter = 0;

   PybaseThreadPool() {
      super(PYBASE_CORE_POOL_SIZE,PYBASE_MAX_POOL_SIZE,
	       PYBASE_POOL_KEEP_ALIVE_TIME,TimeUnit.MILLISECONDS,
	       new LinkedBlockingQueue<Runnable>());

      setThreadFactory(this);
    }

   @Override public Thread newThread(Runnable r) {
      Thread t = new Thread(r,"PybaseWorkerThread_" + (++thread_counter));
      t.setDaemon(true);
      return t;
    }

   @Override protected void afterExecute(Runnable r,Throwable t) {
      super.afterExecute(r,t);
      if (t != null) {
	 logE("Problem with background task " + r.getClass().getName() + " " + r,t);
       }
    }

}	// end of inner class PybaseThreadPool


private class PybaseDelayExecute extends TimerTask {

   private Runnable run_task;

   PybaseDelayExecute(Runnable r) {
      run_task = r;
    }

   @Override public void run() {
      thread_pool.execute(run_task);
    }

}	// end of PybaseDelayExecute

}	// end of class PybaseMain




/* end of PybaseMain.java */


