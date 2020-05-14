/********************************************************************************/
/*										*/
/*		RebaseMain.java 						*/
/*										*/
/*	Repository Bubbles Base Main Program					*/
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



package edu.brown.cs.bubbles.rebase;

// import edu.brown.cs.bubbles.rebase.java.RebaseJavaLanguage;
import edu.brown.cs.bubbles.rebase.newjava.RebaseJavaLanguage;
import edu.brown.cs.bubbles.rebase.word.RebaseWordFactory;

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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class RebaseMain implements RebaseConstants, MintConstants
{


/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   RebaseMain pm = new RebaseMain(args);

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

private File			workspace_directory;
private String			mint_handle;
private MintControl		mint_control;
private boolean 		shutdown_mint;
private int			num_clients;
private Object			send_sema;
private RebaseThreadPool	thread_pool;
private Timer			rebase_timer;
private Pinger			rebase_pinger;

private RebaseProjectManager	project_manager;
private RebaseEditManager	edit_manager;
private Map<SourceLanguage,RebaseLanguage> language_map;

private RebasePreferences	rebase_props;
private RebaseCache		rebase_cache;

private static RebaseMain	rebase_main = null;
private static PrintStream log_file = null;
private static RebaseLogLevel log_level = RebaseLogLevel.DEBUG;
private static boolean use_stderr = true;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

RebaseMain(String [] args)
{
   rebase_main = this;

   mint_handle = System.getProperty("edu.brown.cs.bubbles.MINT");
   if (mint_handle == null) mint_handle = System.getProperty("edu.brown.cs.bubbles.mint");
   if (mint_handle == null) mint_handle = REBASE_MINT_NAME;

   shutdown_mint = false;
   num_clients = 0;
   send_sema = new Object();
   rebase_props = null;

   String hm = System.getProperty("user.home");
   File f1 = new File(hm);
   File f2 = new File(f1,".bubbles");
   File f3 = new File(f2,".ivy");
   if (!f3.exists() || !IvySetup.setup(f3)) IvySetup.setup();
   File f5 = new File(f1,".rebus");
   if (!f5.exists()) f5.mkdirs();
   File f6 = new File(f5,"RebasePrefs.xml");
   workspace_directory = f5;

   scanArgs(args);

   if (log_file == null) {
      try {
	 File f4 = new File(f5,"rebase_log.log");
	 log_file = new PrintStream(new FileOutputStream(f4),true);
       }
      catch (FileNotFoundException e) {
	 log_file = null;
	 RebaseMain.logE("Error initialising file: " + e.getMessage());
       }
    }

   rebase_props = new RebasePreferences(f6);

   project_manager = new RebaseProjectManager(this);
   edit_manager = new RebaseEditManager(this);
   rebase_cache = new RebaseCache();

   language_map = new HashMap<SourceLanguage,RebaseLanguage>();
   language_map.put(SourceLanguage.JAVA,new RebaseJavaLanguage());

   thread_pool = new RebaseThreadPool();
   rebase_timer = new Timer("RebaseTimer",true);
   rebase_pinger = null;

   RebaseMain.logI("STARTING");
}




/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

void start()
{
   CommandHandler hdlr = new CommandHandler();

   mint_control = MintControl.create(mint_handle,MintSyncMode.SINGLE);
   mint_control.register("<BUBBLES DO='_VAR_1' LANG='Rebase' />",hdlr);
   mint_control.register("<REBUS DO='_VAR_1' />",hdlr);
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
	 else if (args[i].startsWith("-w") && i+1 < args.length) {      // -ws <workspace>
	    workspace_directory = new File(args[++i]);
	  }
	 else badArgs();
       }
      else badArgs();
    }
}



private void badArgs()
{
   System.err.println("REBUS: rebasemain [-m <mint>]");
   System.exit(1);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public static RebaseMain getRebase()		{ return rebase_main; }

RebasePreferences getProperties()		{ return rebase_props; }

public File getWorkspaceDirectory()		{ return workspace_directory; }

public RebaseProjectManager getProjectManager() { return project_manager; }

public RebaseEditManager getEditorManager()	{ return edit_manager; }

public RebaseCache getUrlCache()		
{ 
   if (rebase_cache == null) rebase_cache = new RebaseCache();
   return rebase_cache;
}

public static String getFileContents(RebaseFile rf)
{
   RebaseMain rm = rebase_main;
   return rm.getEditorManager().getContents(rf);
}

public RebaseFile getFileByName(String name)
{
   return project_manager.findFile(name);
}


public RebaseProject getProject(String pid)
{
   return project_manager.findProject(pid);
}


public RebaseSemanticData getSemanticData(RebaseFile rf)
{
   return language_map.get(rf.getLanguage()).getSemanticData(rf);
}


public RebaseProjectSemantics getSemanticData(Collection<RebaseFile> rfs)
{
   if (rfs.size() == 0) return null;
   RebaseLanguage rl = null;
   for (RebaseFile rf : rfs) {
      rl = language_map.get(rf.getLanguage());
      if (rl != null) break;
    }
   if (rl == null) return null;

   RebaseProjectSemantics rs = rl.getSemanticData(rfs);

   return rs;
}



public static String fixFileName(String nm)
{
   if (nm == null) return nm;

   if (File.separatorChar == '/') return nm;

   return nm.replace('\\','/');
}



/********************************************************************************/
/*										*/
/*	Command processing							*/
/*										*/
/********************************************************************************/

private String handleCommand(String cmd,String proj,Element xml) throws RebaseException
{
   RebaseMain.logI("Handle command " + cmd + " for " + proj);

   long start = System.currentTimeMillis();

   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("RESULT");

   switch (cmd) {
      case "PING" :
	 xw.text("PONG");
	 break;
      case "PROJECTS" :
	 project_manager.listProjects(xw);
	 break;
      case "OPENPROJECT" :
	 project_manager.openProject(proj,IvyXml.getAttrBool(xml,"FILES",false),
	       IvyXml.getAttrBool(xml,"PATHS",false),
	       IvyXml.getAttrBool(xml,"CLASSES",false),
	       IvyXml.getAttrBool(xml,"OPTIONS",false),xw);
	 break;
      case "BUILDPROJECT" :
	 project_manager.buildProject(proj,IvyXml.getAttrBool(xml,"CLEAN"),
	       IvyXml.getAttrBool(xml,"FULL"),
	       IvyXml.getAttrBool(xml,"REFRESH"),xw);
	 break;
      case "GETALLNAMES" :
	 project_manager.getAllNames(proj,
	       IvyXml.getAttrString(xml,"BID","*"),
	       getSet(xml,"FILE"),
	       IvyXml.getAttrString(xml,"BACKGROUND"),xw);
	 break;
      case "DELETE" :
	 project_manager.delete(proj,
	       IvyXml.getAttrString(xml,"WHAT"),
	       IvyXml.getAttrString(xml,"PATH"));
	 break;
      case "FINDPACKAGE" :
	 project_manager.findPackage(proj,IvyXml.getAttrString(xml,"NAME"),xw);
	 break;
      case "PREFERENCES" :
	 rebase_props.dumpPreferences(xw);
	 break;
      case "SETPREFERENCES" :
	 Element pxml = IvyXml.getChild(xml,"profile");
	 if (pxml == null) pxml = IvyXml.getChild(xml,"OPTIONS");
	 rebase_props.setPreferences(pxml);
	 break;
      case "STARTFILE" :
	 edit_manager.handleStartFile(proj,
	       IvyXml.getAttrString(xml,"BID","*"),
	       fixFileName(IvyXml.getAttrString(xml,"FILE")),
	       IvyXml.getAttrString(xml,"ID"),
	       IvyXml.getAttrBool(xml,"CONTENTS",false),xw);
	 break;
      case "EDITFILE" :
	 edit_manager.handleEdit(proj,IvyXml.getAttrString(xml,"BID","*"),
	       fixFileName(IvyXml.getAttrString(xml,"FILE")),
	       IvyXml.getAttrString(xml,"ID"),
	       getEditSet(xml),xw);
	 break;
      case "COMMIT" :
	 edit_manager.handleCommit(proj,
	       IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrBool(xml,"REFRESH",false),
	       IvyXml.getAttrBool(xml,"SAVE",false),
	       getElements(xml,"FILE"),xw);
	 break;
      case "ELIDESET" :
	 edit_manager.elisionSetup(proj,IvyXml.getAttrString(xml,"BID","*"),
	       fixFileName(IvyXml.getAttrString(xml,"FILE")),
	       IvyXml.getAttrBool(xml,"COMPUTE",true),
	       getElements(xml,"REGION"),xw);
	 break;
      case "EDITPARAM" :
	 edit_manager.handleParameter(IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"NAME"),
	       IvyXml.getAttrString(xml,"VALUE"));
	 break;
      case "PATTERNSEARCH" :
	 project_manager.patternSearch(proj,
	       IvyXml.getAttrString(xml,"PATTERN"),
	       IvyXml.getAttrString(xml,"FOR"),
	       IvyXml.getAttrBool(xml,"DEFS",true),
	       IvyXml.getAttrBool(xml,"REFS",true),
	       IvyXml.getAttrBool(xml,"SYSTEM",false),xw);
	 break;
      case "FINDBYKEY" :
	 project_manager.findByKey(proj,
	       IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"KEY"),
	       fixFileName(IvyXml.getAttrString(xml,"FILE")), xw);
	 break;
      case "FINDDEFINITIONS" :
	 project_manager.findAll(proj,
	       fixFileName(IvyXml.getAttrString(xml,"FILE")),
	       IvyXml.getAttrInt(xml,"START"),IvyXml.getAttrInt(xml,"END"),
	       IvyXml.getAttrBool(xml,"DEFS",true),
	       IvyXml.getAttrBool(xml,"REFS",false),
	       IvyXml.getAttrBool(xml,"IMPLS",false),
	       IvyXml.getAttrBool(xml,"TYPE",false),
	       false,false,
	       xw);
	 break;
      case "FINDREFERENCES" :
	 project_manager.findAll(proj,fixFileName(IvyXml.getAttrString(xml,"FILE")),
	       IvyXml.getAttrInt(xml,"START"),IvyXml.getAttrInt(xml,"END"),
	       IvyXml.getAttrBool(xml,"DEFS",true),
	       IvyXml.getAttrBool(xml,"REFS",true),
	       IvyXml.getAttrBool(xml,"IMPLS",false),
	       IvyXml.getAttrBool(xml,"TYPE",false),
	       IvyXml.getAttrBool(xml,"RONLY",false),
	       IvyXml.getAttrBool(xml,"WONLY",false),
	       xw);
	 break;
      case "GETFULLYQUALIFIEDNAME" :
	 project_manager.getFullyQualifiedName(proj,fixFileName(IvyXml.getAttrString(xml,"FILE")),
	       IvyXml.getAttrInt(xml,"START"),
	       IvyXml.getAttrInt(xml,"END"),xw);
	 break;
      case "FINDREGIONS" :
	 project_manager.getTextRegions(proj,IvyXml.getAttrString(xml,"BID","*"),
	       fixFileName(IvyXml.getAttrString(xml,"FILE")),
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
      case "SEARCH" :
	 project_manager.textSearch(proj,IvyXml.getAttrInt(xml,"FLAGS",0),
	       IvyXml.getTextElement(xml,"PATTERN"),
	       IvyXml.getAttrInt(xml,"MAX",128),
	       xw);
	 break;
      case "RENAME" :
	 break;
      case "RENAMERESOURCE" :
	 break;
      case "EXTRACTMETHOD" :
	 break;
      case "FORMATCODE" :
	 project_manager.formatCode(proj,IvyXml.getAttrString(xml,"BID","*"),
	       fixFileName(IvyXml.getAttrString(xml,"FILE")),
	       IvyXml.getAttrInt(xml,"START"),
	       IvyXml.getAttrInt(xml,"END"),xw);
	 break;
      case "CALLPATH" :
	 break;
      case "ENTER" :
	 if (num_clients == 0 && rebase_pinger == null) {
	    rebase_pinger = new Pinger();
	    rebase_timer.schedule(rebase_pinger,30000,30000);
	  }
	 ++num_clients;
	 xw.text(Integer.toString(num_clients));
	 break;
      case "EXIT" :
	 RebaseMain.logD("Exit with " + num_clients);
	 if (--num_clients <= 0) {
	    RebaseMain.logD("Stopping application");
	    shutdown_mint = true;
	  }
	 break;
      case "LOGLEVEL" :
	 log_level = IvyXml.getAttrEnum(xml,"LEVEL",RebaseLogLevel.ERROR);
	 break;
      case "GETHOST" :
	 handleGetHost(xw);
	 break;
      case "REBUSSEARCH" :
	 RebaseRequest rq = new RebaseRequest(this,
	       IvyXml.getTextElement(xml,"TEXT"),
	       IvyXml.getTextElement(xml,"REPO"),
	       IvyXml.getTextElement(xml,"TASK"),
	       proj,
	       IvyXml.getAttrEnum(xml,"TYPE",SourceType.FILE));
	 rq.startNextSearch();
	 break;
      case "REBUSSUGGEST" :
	RebaseWordFactory.getFactory().getQuery(xw);
	break;
      case "REBUSNEXT" :
	 project_manager.handleNext(proj,
	       fixFileName(IvyXml.getTextElement(xml,"FILE")));
	 break;
      case "REBUSEXPAND" :
	 project_manager.handleExpand(proj,
	       fixFileName(IvyXml.getTextElement(xml,"FILE")));
	 break;
      case "REBUSACCEPT" :
	 project_manager.handleAccept(proj,
	       fixFileName(IvyXml.getTextElement(xml,"FILE")),
	       IvyXml.getAttrBool(xml,"FLAG",true),
	       IvyXml.getAttrInt(xml,"SPOS"),
	       IvyXml.getAttrInt(xml,"EPOS"));
	 break;
      case "REBUSEXPORT" :
	 project_manager.handleExport(proj,
	       IvyXml.getTextElement(xml,"DIR"),
	       IvyXml.getAttrBool(xml,"ACCEPT"),
	       fixFileName(IvyXml.getTextElement(xml,"FILE")));
	 break;
      case "REBUSREQUERY" :
	 rq = new RebaseRequest(this,
	       null,
	       IvyXml.getTextElement(xml,"REPO"),
	       IvyXml.getTextElement(xml,"TASK"),
	       proj,
	       IvyXml.getAttrEnum(xml,"TYPE",SourceType.FILE));
	 rq.startNextSearch();
	 break;
      case "REBUSCACHE" :
	 rebase_cache.setupCache(IvyXml.getTextElement(xml,"DIR"),
	       IvyXml.getAttrBool(xml,"USE",true));
	 break;

      // commands that don't need to be implemented
      case "FILEELIDE" :
      case "CREATECLASS" :
      case "FINDHIERARCHY" :
      case "GETPROXY" :
      case "QUICKFIX" :
      case "REPOSEARCH" :
      case "CREATEPROJECT" :
      case "EDITPROJECT" :
      case "CREATEPACKAGE" :
      case "GETRUNCONFIG" :
      case "NEWRUNCONFIG" :
      case "EDITRUNCONFIG" :
      case "SAVERUNCONFIG" :
      case "DELETERUNCONFIG" :
      case "GETALLBREAKPOINTS" :
      case "ADDLINEBREAKPOINT" :
      case "ADDEXCEPTIONBREAKPOINT" :
      case "EDITBREAKPOINT" :
      case "CLEARALLLINEBREAKPOINTS" :
      case "CLEARLINEBREAKPOINT" :
      case "START" :
      case "DEBUGACTION" :
      case "CONSOLEINPUT" :
      case "GETSTACKFRAMES" :
      case "VARVAL" :
      case "VARDETAIL" :
      case "EVALUATE" :
	 break;
      default :
	 xw.close();
	 throw new RebaseException("Unknown REBASE command " + cmd);
    }

   xw.end("RESULT");

   long delta = System.currentTimeMillis() - start;

   RebaseMain.logD("Result (" + delta + ") = " + xw.toString());
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
      catch (RebaseException e) {
	 String xmsg = "REBASE: error in command " + cmd + ": " + e;
	 RebaseMain.logE(xmsg,e);
	 rslt = "<ERROR><![CDATA[" + xmsg + "]]></ERROR>";
       }
      catch (Throwable t) {
	 String xmsg = "REBASE: Problem processing command " + cmd + ": " + t;
	 RebaseMain.logE(xmsg);
	 t.printStackTrace();
	 StringWriter sw = new StringWriter();
	 PrintWriter pw = new PrintWriter(sw);
	 t.printStackTrace(pw);
	 RebaseMain.logE("TRACE: " + sw.toString());
	 for (Throwable xt = t.getCause(); xt != null; xt = xt.getCause()) {
	    StringWriter xsw = new StringWriter();
	    PrintWriter xpw = new PrintWriter(xsw);
	    xt.printStackTrace(xpw);
	    RebaseMain.logE("CAUSED BY: " + xsw.toString());
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
	 synchronized (RebaseMain.this) {
	    RebaseMain.this.notifyAll();
	  }
       }
    }

}	// end of subclass CommandHandler



/********************************************************************************/
/*										*/
/*	Logging methods 							*/
/*										*/
/********************************************************************************/

static public void logE(String msg,Throwable t) { log(RebaseLogLevel.ERROR,msg,t); }

static public void logE(String msg)		{ log(RebaseLogLevel.ERROR,msg,null); }

static public void logW(String msg)		{ log(RebaseLogLevel.WARNING,msg,null); }

static public void logI(String msg)		{ log(RebaseLogLevel.INFO,msg,null); }

static public  void logD(String msg)		{ log(RebaseLogLevel.DEBUG,msg,null); }

static public void logX(String msg)
{
   Throwable t = new Throwable(msg);
   logE(msg,t);
}



static public void log(String msg)		{ logI(msg); }



static public void log(RebaseLogLevel lvl,String msg,Throwable t)
{
   if (lvl.ordinal() > log_level.ordinal()) return;

   String s = lvl.toString().substring(0,1);
   String pfx = "REBASE:" + s + ": ";

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
   xw.begin("REBASE");
   xw.field("SOURCE","REBASE");
   xw.field("TYPE",typ);
   if (bid != null) xw.field("BID",bid);

   return xw;
}


public void finishMessage(IvyXmlWriter xw)
{
   xw.end("REBASE");

   sendMessage(xw.toString());
}


public String finishMessageWait(IvyXmlWriter xw)
{
   return finishMessageWait(xw,0);
}


public String finishMessageWait(IvyXmlWriter xw,long delay)
{
   xw.end("REBASE");

   return sendMessageWait(xw.toString(),delay);
}



private void sendMessage(String msg)
{
   synchronized (send_sema) {
      RebaseMain.logD("Sending: " + msg);
      if (mint_control != null && !shutdown_mint)
	 mint_control.send(msg);
    }
}



private String sendMessageWait(String msg,long delay)
{
   MintDefaultReply rply = new MintDefaultReply();

   synchronized (send_sema) {
      RebaseMain.logD("Sending/w: " + msg);
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
	    synchronized (RebaseMain.this) {
	       RebaseMain.this.notifyAll();
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

   RebaseDelayExecute pde = new RebaseDelayExecute(r);
   rebase_timer.schedule(pde,delay);
}


public void finishTask(Runnable r)
{
   if (r != null) thread_pool.remove(r);
}



private static class RebaseThreadPool extends ThreadPoolExecutor implements ThreadFactory {

   private static int thread_counter = 0;

   RebaseThreadPool() {
      super(REBASE_CORE_POOL_SIZE,REBASE_MAX_POOL_SIZE,
	       REBASE_POOL_KEEP_ALIVE_TIME,TimeUnit.MILLISECONDS,
	       new LinkedBlockingQueue<Runnable>());

      setThreadFactory(this);
    }

   @Override public Thread newThread(Runnable r) {
      Thread t = new Thread(r,"RebaseWorkerThread_" + (++thread_counter));
      t.setDaemon(true);
      return t;
    }

   @Override protected void afterExecute(Runnable r,Throwable t) {
      super.afterExecute(r,t);
      if (t != null) {
	 logE("Problem with background task " + r.getClass().getName() + " " + r,t);
       }
    }

}	// end of inner class RebaseThreadPool


private class RebaseDelayExecute extends TimerTask {

   private Runnable run_task;

   RebaseDelayExecute(Runnable r) {
      run_task = r;
    }

   @Override public void run() {
      thread_pool.execute(run_task);
    }

}	// end of RebaseDelayExecute



}	// end of class RebaseMain




/* end of RebaseMain.java */

