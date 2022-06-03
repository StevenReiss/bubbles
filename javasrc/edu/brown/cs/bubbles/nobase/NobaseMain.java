/********************************************************************************/
/*										*/
/*		NobaseMain.java 						*/
/*										*/
/*	Main program for Node-Bubbles base package				*/
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



package edu.brown.cs.bubbles.nobase;

import edu.brown.cs.ivy.exec.IvySetup;
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


public class NobaseMain implements NobaseConstants, MintConstants
{


/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   NobaseMain pm = new NobaseMain(args);

   java.util.logging.Logger logger = java.util.logging.Logger.getLogger("WebSocket");
   logger.setLevel(java.util.logging.Level.SEVERE);

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
private NobaseEditor		nobase_editor;
private NobaseProjectManager	project_manager;
private NobaseFileManager	file_manager;
private NobaseSearch		nobase_search;
private NobaseDebugManager	nobase_debug;
private IParser 		nobase_parser;
private File			work_directory;
private Object			send_sema;
private NobaseThreadPool	thread_pool;
private Timer			nobase_timer;
private Pinger			nobase_pinger;
private NobasePreferences	system_prefs;

static private NobaseMain		nobase_main;

private static PrintStream log_file = null;
private static NobaseLogLevel log_level = NobaseLogLevel.DEBUG;
private static boolean use_stderr = false;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public static NobaseMain getNobaseMain()	{ return nobase_main; }



private NobaseMain(String [] args)
{
   nobase_main = this;

   mint_handle = System.getProperty("edu.brown.cs.bubbles.MINT");
   if (mint_handle == null) mint_handle = System.getProperty("edu.brown.cs.bubbles.mint");
   if (mint_handle == null) mint_handle = NOBASE_MINT_NAME;
   String rd = System.getProperty("edu.brown.cs.bubbles.nobase.ROOT");
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

   File f5 = new File(hm,"Nobbles");
   work_directory = new File(f5,"workspace");

   scanArgs(args);

   if (log_file == null) {
      try {
	 File f4 = new File(work_directory,"nobase_log.log");
	 log_file = new PrintStream(new FileOutputStream(f4),true);
       }
      catch (FileNotFoundException e) {
	 log_file = null;
	 NobaseMain.logE("Error initialising file: " + e.getMessage());
       }
    }

   logD("CLASSPATH = " + System.getProperty("java.class.path"));

   if (!work_directory.exists()) {
      work_directory.mkdirs();
    }
   File df = new File(work_directory,".metadata");
   if (!df.exists()) df.mkdir();
   df = new File(df,".nobase");
   if (!df.exists()) df.mkdir();
   File preffile = new File(df,"preferences");
   system_prefs = new NobasePreferences(preffile);

   thread_pool = new NobaseThreadPool();
   nobase_timer = new Timer("NobaseTimer",true);
   nobase_pinger = null;

   // nobase_parser = new NobaseCaja();
   nobase_parser = new NobaseJsdt();

   NobaseMain.logI("STARTING");
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void start()
{
   try {
      project_manager = new NobaseProjectManager(this,work_directory);
      nobase_editor = new NobaseEditor(this);
      nobase_search = new NobaseSearch(this);
      nobase_debug = new NobaseDebugManager(this);
      file_manager = new NobaseFileManager(this);
      project_manager.loadProjects();
    }
   catch (NobaseException e) {
      logE("Problem initializing: " + e,e);
      System.exit(1);
    }

   mint_control = MintControl.create(mint_handle,MintSyncMode.SINGLE);
   mint_control.register("<BUBBLES DO='_VAR_1' LANG='Node/JS' />",new CommandHandler());
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
	    try {
	       work_directory = work_directory.getCanonicalFile();
	     }
	    catch (IOException e) {
	       work_directory = work_directory.getAbsoluteFile();
	     }
	  }
         else if (args[i].startsWith("-log") && i+1 < args.length) {     // -log <logfile>
            try {
               log_file = new PrintStream(new FileOutputStream(args[++i]));
               use_stderr = false;
             }
            catch (IOException e) {
               System.err.println("NOBASE: Can't open log file " + args[i]);
             }
          }
         else if (args[i].startsWith("-err")) {                         // -err
            use_stderr = true;
          }
	 else badArgs();
       }
      else badArgs();
    }
}



private void badArgs()
{
   System.err.println("NOBBLES: nobasemain [-m <mint>]");
   System.exit(1);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

NobaseProjectManager getProjectManager()		{ return project_manager; }

NobaseFileManager getFileManager()			{ return file_manager; }

IParser getParser()					{ return nobase_parser; }

public NobaseProject getProject(String p) throws NobaseException
{
   return project_manager.findProject(p);
}

public NobaseFile getFileData(String fnm)
{
   return file_manager.getFileData(new File(fnm));
}



NobasePreferences getSystemPreferences()		{ return system_prefs; }


public File getWorkSpaceDirectory()			{ return work_directory; }

public File getRootDirectory()				{ return root_directory; }




/********************************************************************************/
/*										*/
/*	Command processing							*/
/*										*/
/********************************************************************************/

private String handleCommand(String cmd,String proj,Element xml) throws NobaseException
{
   logI("Handle command " + cmd + " for " + proj);
   logD("Full command " + IvyXml.convertXmlToString(xml));
   
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("RESULT");

   switch (cmd) {
      case "PING" :
	 xw.text("PONG");
	 break;
      case "PROJECTS" :
	 project_manager.handleListProjects(xw);
	 break;
      case "OPENPROJECT" :
	 project_manager.handleOpenProject(proj,IvyXml.getAttrBool(xml,"FILES",false),
	       IvyXml.getAttrBool(xml,"PATHS",false),
	       IvyXml.getAttrBool(xml,"CLASSES",false),
	       IvyXml.getAttrBool(xml,"OPTIONS",false),xw);
	 break;
      case "BUILDPROJECT" :
	 project_manager.handleBuildProject(proj,IvyXml.getAttrBool(xml,"CLEAN"),
	       IvyXml.getAttrBool(xml,"FULL"),
	       IvyXml.getAttrBool(xml,"REFRESH"),xw);
	 break;
      case "CREATEPROJECT" :
	 project_manager.handleCreateProject(IvyXml.getAttrString(xml,"NAME"),
	       IvyXml.getAttrString(xml,"DIR"),xw);
	 break;
      case "EDITPROJECT" :
	 project_manager.handleEditProject(proj,IvyXml.getChild(xml,"PROJECT"),xw);
	 break;
      case "CREATEPACKAGE" :
	 project_manager.handleCreatePackage(proj,IvyXml.getAttrString(xml,"NAME"),
	       IvyXml.getAttrBool(xml,"FORCE"),xw);
	 break;
      case "FINDPACKAGE" :
	 project_manager.handleFindPackage(proj,IvyXml.getAttrString(xml,"NAME"),xw);
	 break;
      case "CREATECLASS" :
	 project_manager.handleNewModule(proj,IvyXml.getAttrString(xml,"NAME"),
	       IvyXml.getAttrBool(xml,"FORCE"),
	       IvyXml.getTextElement(xml,"CONTENTS"),xw);
	 break;
      case "STARTFILE" :
	 nobase_editor.handleStartFile(proj,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrString(xml,"ID"),
	       IvyXml.getAttrBool(xml,"CONTENTS",false),xw);
	 break;
      case "EDITPARAM" :
	 nobase_editor.handleParameter(IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"NAME"),
	       IvyXml.getAttrString(xml,"VALUE"));
	 break;
      case "EDITFILE" :
	 nobase_editor.handleEdit(proj,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrString(xml,"ID"),
	       getEditSet(xml),xw);
	 break;
      case "COMMIT" :
	 nobase_editor.handleCommit(proj,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrBool(xml,"REFRESH",false),
	       IvyXml.getAttrBool(xml,"SAVE",false),
	       getElements(xml,"FILE"),xw);
	 break;
      case "ELIDESET" :
	 nobase_editor.elisionSetup(proj,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrBool(xml,"COMPUTE",true),
	       getElements(xml,"REGION"),xw);
	 break;
      case "PATTERNSEARCH" :
	 project_manager.handlePatternSearch(proj,IvyXml.getAttrString(xml,"PATTERN"),
	       IvyXml.getAttrString(xml,"FOR"),
	       IvyXml.getAttrBool(xml,"DEFS",true),
	       IvyXml.getAttrBool(xml,"REFS",true),
	       IvyXml.getAttrBool(xml,"SYSTEM",false),xw);
	 break;
      case "FINDBYKEY" :
	 project_manager.handleKeySearch(proj,IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrString(xml,"KEY"),xw);
	 break;
      case "FINDDEFINITIONS" :
	 project_manager.handleFindAll(proj,IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrInt(xml,"START"),IvyXml.getAttrInt(xml,"END"),
	       IvyXml.getAttrBool(xml,"DEFS",true),
	       IvyXml.getAttrBool(xml,"REFS",false),
	       IvyXml.getAttrBool(xml,"IMPLS",false),
	       IvyXml.getAttrBool(xml,"TYPE",false),
	       false,false,xw);
	 break;
      case "FINDREFERENCES" :
	 project_manager.handleFindAll(proj,IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrInt(xml,"START"),IvyXml.getAttrInt(xml,"END"),
	       IvyXml.getAttrBool(xml,"DEFS",true),
	       IvyXml.getAttrBool(xml,"REFS",true),
	       IvyXml.getAttrBool(xml,"IMPLS",false),
	       IvyXml.getAttrBool(xml,"TYPE",false),
	       IvyXml.getAttrBool(xml,"RONLY",false),
	       IvyXml.getAttrBool(xml,"WONLY",false),xw);
	 break;
      case "GETFULLYQUALIFIEDNAME" :
	 project_manager.getFullyQualifiedName(proj,IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrInt(xml,"START"),
	       IvyXml.getAttrInt(xml,"END"),xw);
	 break;
      case "FINDREGIONS" :
	 project_manager.getTextRegions(proj,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrString(xml,"CLASS"),
	       IvyXml.getAttrBool(xml,"PREFIX",false),
	       IvyXml.getAttrBool(xml,"STATICS",false),
	       IvyXml.getAttrBool(xml,"COMPUNIT",false),
	       IvyXml.getAttrBool(xml,"IMPORTS",false),
	       IvyXml.getAttrBool(xml,"PACKAGE",false),
	       IvyXml.getAttrBool(xml,"TOPDECLS",false),
	       IvyXml.getAttrBool(xml,"FIELDS",false),
	       IvyXml.getAttrBool(xml,"ALL",false),xw);
	 break;
      case "GETALLNAMES" :
	 project_manager.handleGetAllNames(proj,IvyXml.getAttrString(xml,"BID","*"),
	       getSet(xml,"FILE"),
	       IvyXml.getAttrString(xml,"BACKGROUND"),xw);
	 break;
      case "SEARCH" :
	 nobase_search.handleTextSearch(proj,IvyXml.getAttrInt(xml,"FLAGS",0),
	       IvyXml.getTextElement(xml,"PATTERN"),
	       IvyXml.getAttrInt(xml,"MAX",MAX_TEXT_SEARCH_RESULTS),xw);
	 break;
      case "GETCOMPLETIONS" :
	 project_manager.getCompletions(proj,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrInt(xml,"OFFSET"),xw);
	  break;
      case "GETRUNCONFIG" :
	 nobase_debug.getRunConfigurations(xw);
	 break;
      case "NEWRUNCONFIG" :
	 nobase_debug.getNewRunConfiguration(proj,
	       IvyXml.getAttrString(xml,"NAME"),
	       IvyXml.getAttrString(xml,"CLONE"),xw);
	 break;
      case "EDITRUNCONFIG" :
	 nobase_debug.editRunConfiguration(IvyXml.getAttrString(xml,"LAUNCH"),
	       IvyXml.getAttrEnum(xml,"PROP",NobaseConfigAttribute.NONE),
	       IvyXml.getAttrString(xml,"VALUE"),xw);
	 break;
      case "SAVERUNCONFIG" :
	 nobase_debug.saveRunConfiguration(IvyXml.getAttrString(xml,"LAUNCH"),xw);
	 break;
      case "DELETERUNCONFIG" :
	 nobase_debug.deleteRunConfiguration(IvyXml.getAttrString(xml,"LAUNCH"),xw);
	 break;
      case "GETALLBREAKPOINTS" :
	 nobase_debug.getAllBreakpoints(xw);
	 break;
      case "ADDLINEBREAKPOINT" :
	 nobase_debug.setLineBreakpoint(proj,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getTextElement(xml,"FILE"),
	       IvyXml.getAttrInt(xml,"LINE"),
	       IvyXml.getAttrBool(xml,"TRACE",false));
	 break;
      case "ADDEXCEPTIONBREAKPOINT" :
	 nobase_debug.setExceptionBreakpoint(proj,IvyXml.getAttrBool(xml,"CAUGHT",false),
	       IvyXml.getAttrBool(xml,"UNCAUGHT",true),
	       IvyXml.getAttrBool(xml,"CHECKED",false));
	 break;
      case "EDITBREAKPOINT" :
	 nobase_debug.editBreakpoint(IvyXml.getAttrString(xml,"ID"),
	       IvyXml.getAttrString(xml,"PROP"),
	       IvyXml.getAttrString(xml,"VALUE"),
	       IvyXml.getAttrString(xml,"PROP1"),
	       IvyXml.getAttrString(xml,"VALUE1"),
	       IvyXml.getAttrString(xml,"PROP2"),
	       IvyXml.getAttrString(xml,"VALUE2"));
	 break;
      case "CLEARALLLINEBREAKPOINTS" :
	 nobase_debug.clearLineBreakpoints(proj,null,0);
	 break;
      case "CLEARLINEBREAKPOINT" :
	 nobase_debug.clearLineBreakpoints(proj,IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrInt(xml,"LINE"));
	 break;
      case "START" :
	 nobase_debug.runProject(IvyXml.getAttrString(xml,"NAME"),xw);
	 break;
      case "DEBUGACTION" :
	 nobase_debug.debugAction(IvyXml.getAttrString(xml,"LAUNCH"),
	       IvyXml.getAttrString(xml,"TARGET"),
	       IvyXml.getAttrString(xml,"FRAME"),
	       IvyXml.getAttrEnum(xml,"ACTION",NobaseDebugAction.NONE),xw);
	 break;
      case "CONSOLEINPUT" :
	 nobase_debug.consoleInput(IvyXml.getAttrString(xml,"LAUNCH"),
	       IvyXml.getTextElement(xml,"INPUT"));
	 break;
      case "GETSTACKFRAMES" :
	 nobase_debug.getStackFrames(IvyXml.getAttrString(xml,"LAUNCH"),
	       IvyXml.getAttrInt(xml,"COUNT",-1),
	       IvyXml.getAttrInt(xml,"DEPTH",0),xw);
	 break;
      case "VARVAL" :
	 nobase_debug.getVariableValue(IvyXml.getAttrString(xml,"FRAME"),
	       IvyXml.getTextElement(xml,"VAR"),
	       IvyXml.getAttrInt(xml,"DEPTH",1),xw);
	 break;
      case "VARDETAIL" :
	 nobase_debug.getVariableValue(IvyXml.getAttrString(xml,"FRAME"),
	       IvyXml.getTextElement(xml,"VAR"),-1,xw);
	 break;
      case "EVALUATE" :
	 nobase_debug.evaluateExpression(proj,
	       IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getTextElement(xml,"EXPR"),
	       IvyXml.getAttrString(xml,"THREAD"),
	       IvyXml.getAttrString(xml,"FRAME"),
	       IvyXml.getAttrBool(xml,"IMPLICIT",false),
	       IvyXml.getAttrBool(xml,"BREAK",true),
	       IvyXml.getAttrString(xml,"REPLYID"),xw);
	 break;
     case "ENTER" :
	 if (num_clients == 0 && nobase_pinger == null) {
	    nobase_pinger = new Pinger();
	    nobase_timer.schedule(nobase_pinger,30000,30000);
	  }
	 ++num_clients;
	 xw.text(Integer.toString(num_clients));
	 break;
      case "EXIT" :
	 if (--num_clients <= 0) {
	    logD("Stopping application");
	    shutdown_mint = true;
	  }
	 break;
      case "LOGLEVEL" :
	 log_level = IvyXml.getAttrEnum(xml,"LEVEL",NobaseLogLevel.ERROR);
	 break;
      case "GETHOST" :
	 handleGetHost(xw);
	 break;
      case "PREFERENCES" :
	 project_manager.handleGetPreferences(proj,xw);
	 break;
      case "SETPREFERENCES" :
	 Element pxml = IvyXml.getChild(xml,"profile");
	 if (pxml == null) pxml = IvyXml.getChild(xml,"OPTIONS");
	 project_manager.handleSetPreferences(proj,pxml);
	 break;
      case "FINDHIERARCHY" :
	 break;
      case "CREATEPRIVATE" :
      case "PRIVATEEDIT" :
      case "REMOVEPRIVATE" :
	 break;
      case "SAVEWORKSPACE" :
         xw.text("SAVED");
         break;
      default :
	 xw.close();
	 throw new NobaseException("Unknown NOBASE command " + cmd);
    }

   xw.end("RESULT");

   logD("Result = " + xw.toString());
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
      if (IvyXml.getAttrBool(e,"ENCODE")) {
	 edit_text = new String(IvyXml.stringToByteArray(edit_text));
       }
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
      catch (NobaseException e) {
         String xmsg = "Error in command " + cmd + ": " + e;
         logE(xmsg,e);
         rslt = "<ERROR><![CDATA[NOBASE: " + xmsg + "]]></ERROR>";
       }
      catch (Throwable t) {
         String xmsg = "Problem processing command " + cmd + ": " + t;
         logE(xmsg,t);
         StringWriter sw = new StringWriter();
         PrintWriter pw = new PrintWriter(sw);
         t.printStackTrace(pw);
         rslt = "<ERROR>";
         rslt += "<MESSAGE>NOBASE: " + xmsg + "</MESSAGE>";
         rslt += "<EXCEPTION><![CDATA[" + t.toString() + "]]></EXCEPTION>";
         rslt += "<STACK><![CDATA[" + sw.toString() + "]]></STACK>";
         rslt += "</ERROR>";
       }
   
      msg.replyTo(rslt);
   
      if (shutdown_mint) {
         mint_control.shutDown();
         synchronized (NobaseMain.this) {
            NobaseMain.this.notifyAll();
          }
       }
    }

}	// end of subclass CommandHandler




/********************************************************************************/
/*										*/
/*	Logging methods 							*/
/*										*/
/********************************************************************************/

static public void logE(String msg,Throwable t) { log(NobaseLogLevel.ERROR,msg,t); }

static public void logE(String msg)		{ log(NobaseLogLevel.ERROR,msg,null); }

static public void logW(String msg)		{ log(NobaseLogLevel.WARNING,msg,null); }

static public void logI(String msg)		{ log(NobaseLogLevel.INFO,msg,null); }

static public  void logD(String msg)		{ log(NobaseLogLevel.DEBUG,msg,null); }

static public  void logDX(String msg)
{
   log(NobaseLogLevel.DEBUG,msg,new Throwable(msg));
}


static public void logX(String msg)
{
   Throwable t = new Throwable(msg);
   logE(msg,t);
}



static public void log(String msg)		{ logI(msg); }



static public void log(NobaseLogLevel lvl,String msg,Throwable t)
{
   if (lvl.ordinal() > log_level.ordinal()) return;

   String s = lvl.toString().substring(0,1);
   String pfx = "NOBASE:" + s + ": ";

   if (log_file != null) {
      log_file.println(pfx + msg);
      dumpTrace(null,t);
    }
   if (use_stderr || log_file == null) {
      System.err.println(pfx + msg);
      dumpTrace(null,t);
      System.err.flush();
    }
  
   if (log_file != null) log_file.flush();
}



static private void dumpTrace(String pfx,Throwable t) {
   if (t == null) return;
   if (log_file != null) {
      if (pfx != null) log_file.print(pfx);
      t.printStackTrace(log_file);
    }
   if (log_file == null || use_stderr) {
      if (pfx != null) System.err.print(pfx);
      t.printStackTrace();
    }
   dumpTrace("CAUSED BY: ",t.getCause());
}




/********************************************************************************/
/*										*/
/*	Simple process		methods 					*/
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
   xw.begin("NOBASE");
   xw.field("SOURCE","NOBASE");
   xw.field("TYPE",typ);
   if (bid != null) xw.field("BID",bid);

   return xw;
}


public void finishMessage(IvyXmlWriter xw)
{
   xw.end("NOBASE");

   sendMessage(xw.toString());
}


public String finishMessageWait(IvyXmlWriter xw)
{
   return finishMessageWait(xw,0);
}


public String finishMessageWait(IvyXmlWriter xw,long delay)
{
   xw.end("NOBASE");

   return sendMessageWait(xw.toString(),delay);
}



private void sendMessage(String msg)
{
   synchronized (send_sema) {
      NobaseMain.logD("Sending: " + msg);
      if (mint_control != null && !shutdown_mint)
	 mint_control.send(msg);
    }
}



private String sendMessageWait(String msg,long delay)
{
   MintDefaultReply rply = new MintDefaultReply();

   synchronized (send_sema) {
      NobaseMain.logD("Sending/w: " + msg);
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
	    synchronized (NobaseMain.this) {
	       NobaseMain.this.notifyAll();
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

   NobaseDelayExecute pde = new NobaseDelayExecute(r);
   nobase_timer.schedule(pde,delay);
}


public void finishTask(Runnable r)
{
   if (r != null) thread_pool.remove(r);
}



private static class NobaseThreadPool extends ThreadPoolExecutor implements ThreadFactory {

   private static int thread_counter = 0;

   NobaseThreadPool() {
      super(NOBASE_CORE_POOL_SIZE,NOBASE_MAX_POOL_SIZE,
	    NOBASE_POOL_KEEP_ALIVE_TIME,TimeUnit.MILLISECONDS,
	    new LinkedBlockingQueue<Runnable>());

      setThreadFactory(this);
    }

   @Override public Thread newThread(Runnable r) {
      Thread t = new Thread(r,"NobaseWorkerThread_" + (++thread_counter));
      t.setDaemon(true);
      return t;
    }

   @Override protected void afterExecute(Runnable r,Throwable t) {
      super.afterExecute(r,t);
      if (t != null) {
	 logE("Problem with background task " + r.getClass().getName() + " " + r,t);
       }
    }

}	// end of inner class NobaseThreadPool


private class NobaseDelayExecute extends TimerTask {

   private Runnable run_task;

   NobaseDelayExecute(Runnable r) {
      run_task = r;
    }

   @Override public void run() {
      thread_pool.execute(run_task);
    }

}	// end of NobaseDelayExecute





}	// end of class NobaseMain




/* end of NobaseMain.java */

