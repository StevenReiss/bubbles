/********************************************************************************/
/*										*/
/*		BedrockPlugin.java						*/
/*										*/
/*	Main class for the Eclipse-Bubbles interface				*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss, Hsu-Sheng Ko      */
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bedrock;

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

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class BedrockPlugin extends Plugin implements BedrockConstants, MintConstants {


//TODO: verbose error message if setup not correct
//TODO: use .bubbles/System to get location for $(IVY)


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private MintControl mint_control;

private Object send_sema;
private boolean workbench_inited;
private BedrockProject bedrock_project;
private BedrockJava bedrock_java;
private BedrockRuntime bedrock_runtime;
private BedrockBreakpoint bedrock_breakpoint;
private BedrockEditManager bedrock_editor;
private BedrockCall bedrock_call;
private BedrockProblem bedrock_problem;
private BedrockQuickFix quick_fixer;
private boolean shutdown_mint;
private boolean doing_exit;
private int	num_clients;

private static PrintStream log_file = null;
private static BedrockLogLevel log_level = BedrockLogLevel.INFO;
private static boolean use_stderr = false;

private static BedrockPlugin the_plugin = null;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public BedrockPlugin()
{
   if (the_plugin != null) return;

   the_plugin = this;
   workbench_inited = false;
   shutdown_mint = false;
   num_clients = 0;
   doing_exit = false;

   String hm = System.getProperty("user.home");
   File f1 = new File(hm);
   File f2 = new File(f1,".bubbles");
   File f3 = new File(f2,".ivy");
   File f4 = new File(f1,".ivy");

   if (f2.exists()) {
      if (!f3.exists() || !IvySetup.setup(f3)) {
	 if (!f4.exists() || !IvySetup.setup(f4)) {
	    IvySetup.setup();
	  }
       }
    }

   IWorkspace ws = ResourcesPlugin.getWorkspace();

   if (log_file == null) {
      try {
	 long now = System.currentTimeMillis();
	 String logfile = null;
	 for (int i = 0; i < 20; ++i) {
	    if (i == 0) logfile = "bedrock_log.log";
	    else logfile = "bedrock_log_" + i + ".log";
	    File lf = new File(ws.getRoot().getLocation().append(logfile).toOSString());
	    if (!lf.exists()) break;
	    if (now - lf.lastModified() > 30000) break;
	 }
	 String filename = ws.getRoot().getLocation().append(logfile).toOSString();
	 log_file = new PrintStream(new FileOutputStream(filename),true);
       }
      catch (FileNotFoundException e) {
	 log_file = null;
	 BedrockPlugin.logE("Error initialising file: " + e.getMessage());
       }
    }

   BedrockPlugin.logI("STARTING");
   BedrockPlugin.logI("MEMORY " + Runtime.getRuntime().maxMemory() + " " +
			 Runtime.getRuntime().totalMemory());

   System.setProperty("org.eclipse.jdt.ui.codeAssistTimeout","30000");

   mint_control = null;
   send_sema = new Object();

   try {
      bedrock_project = new BedrockProject(this);
      bedrock_java = new BedrockJava(this);
      bedrock_runtime = new BedrockRuntime(this);
      bedrock_breakpoint = new BedrockBreakpoint(this);
      bedrock_editor = new BedrockEditManager(this);
      bedrock_call = new BedrockCall(this);
      bedrock_problem = new BedrockProblem(this);
      quick_fixer = new BedrockQuickFix(this);
    }
   catch (Throwable t) {
      BedrockPlugin.logE("PROBLEM STARTING BEDROCK: " + t,t);
    }

   initWorkbench();
}


private void initWorkbench() {
   if (!workbench_inited && PlatformUI.isWorkbenchRunning()) {
      BedrockPlugin.logI("workbench inited");
      workbench_inited = true;
    }
}




/********************************************************************************/
/*										*/
/*	Mint methods								*/
/*										*/
/********************************************************************************/

String getMintName()
{
   return mint_control.getMintName();
}



private synchronized void setupMint()
{
   IvySetup.setup();

   if (mint_control != null) return;

   String mintname = System.getProperty("edu.brown.cs.bubbles.MINT");
   if (mintname == null) mintname = System.getProperty("edu.brown.cs.bubbles.mint");
   if (mintname == null) {
      IWorkspace ws = ResourcesPlugin.getWorkspace();
      IWorkspaceRoot root = ws.getRoot();
      IPath rootpath = root.getRawLocation();
      String wsname = rootpath.toOSString();
      if (wsname.endsWith(File.separator)) wsname = wsname.substring(0,wsname.length()-1);
      int idx = wsname.lastIndexOf(File.separator);
      if (idx > 0) wsname = wsname.substring(idx+1);
      if (wsname == null) wsname = "";
      else wsname = wsname.replace(" ","_");
      BedrockPlugin.logI("Setting mint for " + wsname + " " + rootpath.toOSString());
      mintname = BEDROCK_MINT_ID;
      mintname = mintname.replace("@@@",wsname);
    }
   if (mintname == null) mintname = BEDROCK_MESSAGE_ID;

   mint_control = MintControl.create(mintname,MintSyncMode.SINGLE);
   mint_control.register("<BUBBLES DO='_VAR_1' />",new CommandHandler());
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public static BedrockPlugin getPlugin() 		{ return the_plugin; }

BedrockProject getProjectManager()			{ return bedrock_project; }

BedrockEditManager getEditManager()			{ return bedrock_editor; }

BedrockBreakpoint getBreakManager()			{ return bedrock_breakpoint; }


void getActiveElements(IJavaElement elt,List<IJavaElement> rslt)
{
   bedrock_editor.getActiveElements(elt,rslt);
}

void waitForEdits()
{
   bedrock_editor.waitForEdits();
}


void getWorkingElements(IJavaElement elt,List<ICompilationUnit> rslt)
{
   bedrock_editor.getWorkingElements(elt,rslt);
}

void getCompilationElements(IJavaElement elt,List<ICompilationUnit> rslt)
{
   bedrock_editor.getCompilationElements(elt,rslt);
}


ICompilationUnit getCompilationUnit(String proj,String file) throws BedrockException
{
   return bedrock_editor.getCompilationUnit(proj,file);
}


ICompilationUnit getBaseCompilationUnit(String proj,String file) throws BedrockException
{
   return bedrock_editor.getBaseCompilationUnit(proj,file);
}


CompilationUnit getAST(String bid,String proj,String file) throws BedrockException
{
   return bedrock_editor.getAST(bid,proj,file);
   // return null;
}


void addFixes(IProblem ip,IvyXmlWriter xw)
{
   bedrock_problem.addFixes(ip,xw);
}




void addFixes(IMarkerDelta ip,IvyXmlWriter xw)
{
   bedrock_problem.addFixes(ip,xw);
}




void addFixes(IMarker ip,IvyXmlWriter xw)
{
   bedrock_problem.addFixes(ip,xw);
}




/********************************************************************************/
/*										*/
/*	Start/stop methods							*/
/*										*/
/********************************************************************************/

@Override public void start(BundleContext ctx) throws Exception
{
   super.start(ctx);

   startBedrock();
}




@Override public void stop(BundleContext ctx) throws Exception
{
   IvyXmlWriter xw = beginMessage("STOP");
   finishMessage(xw);

   BedrockPlugin.logI("Stop called");

   if (!doing_exit) {
      doing_exit = true;
      shutdown_mint = true;
      if (mint_control != null) mint_control.shutDown();
    }

   if (bedrock_project != null) bedrock_project.terminate();

   super.stop(ctx);
}




private void startBedrock()
{
   try {
      BedrockPlugin.logI("Start called");

      initWorkbench();

      bedrock_project.initialize();
      bedrock_runtime.start();
      bedrock_breakpoint.start();
      bedrock_editor.start();

      setupMint();

      bedrock_project.register();
    }
   catch (Throwable t) {
      BedrockPlugin.logE("Problem starting bedrock: " + t);
      t.printStackTrace();
    }
}



private void saveEclipse()
{
   try {
      IWorkspace ws = ResourcesPlugin.getWorkspace();
      BedrockProgressMonitor pm = new BedrockProgressMonitor(this,"Saving Workbench");
      ws.save(true,pm);
      pm.finish();
      BedrockPlugin.logD("WORKSPACE SAVE SUCCEEDED");
    }
   catch (Throwable t) {
      BedrockPlugin.logE("Problem saving workbench: " + t,t);
    }
}



/********************************************************************************/
/*										*/
/*	Methods for sending out messages					*/
/*										*/
/********************************************************************************/

private void sendMessage(String msg)
{
   synchronized (send_sema) {
      if (mint_control != null && !doing_exit)
	 mint_control.send(msg);
    }
}


private String sendMessageWait(String msg,long delay)
{
   MintDefaultReply rply = new MintDefaultReply();

   synchronized (send_sema) {
      if (mint_control != null && !doing_exit) {
	 mint_control.send(msg,rply,MINT_MSG_FIRST_NON_NULL);
       }
      else return null;
    }

   String s = msg;
   if (s.length() > 50) s = s.substring(0,50);
   BedrockPlugin.logD("Send message: " + s);

   return rply.waitForString(delay);
}



IvyXmlWriter beginMessage(String typ)
{
   return beginMessage(typ,null);
}


IvyXmlWriter beginMessage(String typ,String bid)
{
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("BEDROCK");
   xw.field("SOURCE","ECLIPSE");
   xw.field("TYPE",typ);
   if (bid != null) xw.field("BID",bid);

   return xw;
}


void finishMessage(IvyXmlWriter xw)
{
   xw.end("BEDROCK");

   sendMessage(xw.toString());
}



String finishMessageWait(IvyXmlWriter xw)
{
   return finishMessageWait(xw,0);
}


String finishMessageWait(IvyXmlWriter xw,long delay)
{
   xw.end("BEDROCK");

   return sendMessageWait(xw.toString(),delay);
}



/********************************************************************************/
/*										*/
/*	New command processors							*/
/*										*/
/********************************************************************************/

private String handleCommand(String cmd,String proj,Element xml) throws BedrockException
{
   BedrockPlugin.logI("Handle command " + cmd + " for " + proj);
   long start = System.currentTimeMillis();
   String h1,h2,h3;

   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("RESULT");

   if (shutdown_mint && !cmd.equals("PING")) {
      xw.close();
      if (cmd.equals("SAVEWORKSPACE")) return null;
      throw new BedrockException("Command during exit");
    }

   switch (cmd) {
      case "PING" :
	 if (doing_exit || shutdown_mint) xw.text("EXIT");
	 else if (PlatformUI.isWorkbenchRunning()) xw.text("PONG");
	 else xw.text("UNSET");
	 break;
      case "PROJECTS" :
	 bedrock_project.listProjects(xw);
	 break;
      case "OPENPROJECT" :
	 bedrock_project.openProject(proj,IvyXml.getAttrBool(xml,"FILES",false),
	       IvyXml.getAttrBool(xml,"PATHS",false),
	       IvyXml.getAttrBool(xml,"CLASSES",false),
	       IvyXml.getAttrBool(xml,"OPTIONS",false),
	       IvyXml.getAttrBool(xml,"IMPORTS",false),
	       IvyXml.getAttrString(xml,"BACKGROUND"),xw);
	 break;
      case "EDITPROJECT" :
	 bedrock_project.editProject(proj,IvyXml.getAttrBool(xml,"LOCAL"),
	       IvyXml.getChild(xml,"PROJECT"),xw);
	 break;
      case "CREATEPROJECT" :
	 bedrock_project.handleCreateProject(IvyXml.getAttrString(xml,"NAME"),
	       new File(IvyXml.getAttrString(xml,"DIR")),
	       IvyXml.getAttrString(xml,"TYPE"),
	       IvyXml.getChild(xml,"PROPS"),xw);
	 break;
      case "IMPORTPROJECT" :
	 try {
	    bedrock_project.importExistingProject(proj);
	  }
	 catch(Throwable t) {
	    xw.close();
	    throw new BedrockException("Exception constructing project: " + t.getMessage());
	  }
	 break;
      case "BUILDPROJECT" :
	 bedrock_project.buildProject(proj,IvyXml.getAttrBool(xml,"CLEAN"),
	       IvyXml.getAttrBool(xml,"FULL"),
	       IvyXml.getAttrBool(xml,"REFRESH"),xw);
	 break;
      case "CREATEPACKAGE" :
	 bedrock_project.createPackage(proj,IvyXml.getAttrString(xml,"NAME"),
	       IvyXml.getAttrBool(xml,"FORCE",false),xw);
	 break;
      case "FINDPACKAGE" :
	 bedrock_project.findPackage(proj,IvyXml.getAttrString(xml,"NAME"),xw);
	 break;
      case "GETALLNAMES" :
	 bedrock_java.handleGetAllNames(proj,IvyXml.getAttrString(xml,"BID","*"),
	       getSet(xml,"FILE"),
	       IvyXml.getAttrString(xml,"BACKGROUND"),xw);
	 break;
      case "FINDDEFINITIONS" :
	 bedrock_java.handleFindAll(proj,IvyXml.getAttrString(xml,"FILE"),
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
	 break;
      case "FINDREFERENCES" :
	 bedrock_java.handleFindAll(proj,IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrInt(xml,"START"),
	       IvyXml.getAttrInt(xml,"END"),
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
	 break;
      case "PATTERNSEARCH" :
	 bedrock_java.handlePatternSearch(proj,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"PATTERN"),
	       IvyXml.getAttrString(xml,"FOR"),
	       IvyXml.getAttrBool(xml,"DEFS",true),
	       IvyXml.getAttrBool(xml,"REFS",true),
	       IvyXml.getAttrBool(xml,"IMPLS",false),
	       IvyXml.getAttrBool(xml,"EQUIV",false),
	       IvyXml.getAttrBool(xml,"EXACT",false),
	       IvyXml.getAttrBool(xml,"SYSTEM",false),
	       xw);
	 break;
      case "SEARCH" :
	 bedrock_java.textSearch(proj,IvyXml.getAttrInt(xml,"FLAGS",0),
	       IvyXml.getTextElement(xml,"PATTERN"),
	       IvyXml.getAttrInt(xml,"MAX",MAX_TEXT_SEARCH_RESULTS),
	       xw);
	 break;
      case "GETFULLYQUALIFIEDNAME" :
	 bedrock_java.getFullyQualifiedName(proj,IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrInt(xml,"START"),
	       IvyXml.getAttrInt(xml,"END"),xw);
	 break;
      case "CREATECLASS" :
	 bedrock_java.handleNewClass(proj,IvyXml.getAttrString(xml,"NAME"),
	       IvyXml.getAttrBool(xml,"FORCE",false),
	       IvyXml.getTextElement(xml,"CONTENTS"), xw);
	 break;
      case "FINDHIERARCHY" :
	 bedrock_java.handleFindHierarchy(proj,IvyXml.getAttrString(xml,"PACKAGE"),
	       IvyXml.getAttrString(xml,"CLASS"),
	       IvyXml.getAttrBool(xml,"ALL",false), xw);
	 break;
      case "LAUNCHQUERY" :
	 bedrock_runtime.handleLaunchQuery(proj,IvyXml.getAttrString(xml,"QUERY"),
	       IvyXml.getAttrBool(xml,"OPTION",false),xw);
	 break;
      case "GETRUNCONFIG" :
	 bedrock_runtime.getRunConfigurations(xw);
	 break;
      case "NEWRUNCONFIG" :
	 bedrock_runtime.getNewRunConfiguration(proj,
	       IvyXml.getAttrString(xml,"NAME"),
	       IvyXml.getAttrString(xml,"CLONE"),
	       IvyXml.getAttrString(xml,"TYPE","Java Application"),xw);
	 break;
      case "EDITRUNCONFIG" :
	 bedrock_runtime.editRunConfiguration(IvyXml.getAttrString(xml,"LAUNCH"),
	       IvyXml.getAttrString(xml,"PROP"),
	       IvyXml.getAttrString(xml,"VALUE"),xw);
	 break;
      case "SAVERUNCONFIG" :
	 bedrock_runtime.saveRunConfiguration(IvyXml.getAttrString(xml,"LAUNCH"),xw);
	 break;
      case "DELETERUNCONFIG" :
	 bedrock_runtime.deleteRunConfiguration(IvyXml.getAttrString(xml,"LAUNCH"),xw);
	 break;
      case "START" :
	 bedrock_runtime.runProject(IvyXml.getAttrString(xml,"NAME"),
	       IvyXml.getAttrString(xml,"MODE",ILaunchManager.DEBUG_MODE),
	       IvyXml.getAttrBool(xml,"BUILD",true),
	       IvyXml.getAttrBool(xml,"REGISTER",true),
	       IvyXml.getAttrString(xml,"VMARG"),
	       IvyXml.getAttrString(xml,"ID"),
	       xw);
	 break;
      case "GETALLBREAKPOINTS" :
	 bedrock_breakpoint.getAllBreakpoints(xw);
	 break;
      case "ADDLINEBREAKPOINT" :
	 bedrock_breakpoint.setLineBreakpoint(proj,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getTextElement(xml,"FILE"),
	       IvyXml.getAttrString(xml,"CLASS"),
	       IvyXml.getAttrInt(xml,"LINE"),
	       IvyXml.getAttrBool(xml,"SUSPENDVM",false),
	       IvyXml.getAttrBool(xml,"TRACE",false));
	 break;
      case "ADDEXCEPTIONBREAKPOINT" :
	 bedrock_breakpoint.setExceptionBreakpoint(proj,IvyXml.getAttrString(xml,"CLASS"),
	       IvyXml.getAttrBool(xml,"CAUGHT",false),
	       IvyXml.getAttrBool(xml,"UNCAUGHT",true),
	       IvyXml.getAttrBool(xml,"CHECKED",false),
	       IvyXml.getAttrBool(xml,"SUSPENDVM",false),
	       IvyXml.getAttrBool(xml,"SUBCLASS",true));
	 break;
      case "EDITBREAKPOINT" :
	 bedrock_breakpoint.editBreakpoint(IvyXml.getAttrInt(xml,"ID"),
	       IvyXml.getAttrString(xml,"PROP"),
	       IvyXml.getAttrString(xml,"VALUE"),
	       IvyXml.getAttrString(xml,"PROP1"),
	       IvyXml.getAttrString(xml,"VALUE1"),
	       IvyXml.getAttrString(xml,"PROP2"),
	       IvyXml.getAttrString(xml,"VALUE2"));
	 break;
      case "CLEARALLLINEBREAKPOINTS" :
	 bedrock_breakpoint.clearLineBreakpoints(proj,null,null,0);
	 break;
      case "CLEARLINEBREAKPOINT" :
	 bedrock_breakpoint.clearLineBreakpoints(proj,IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrString(xml,"CLASS"),
	       IvyXml.getAttrInt(xml,"LINE"));
	 break;
      case "DEBUGACTION" :
	 bedrock_runtime.debugAction(IvyXml.getAttrString(xml,"LAUNCH"),
	       IvyXml.getAttrString(xml,"TARGET"),
	       IvyXml.getAttrString(xml,"PROCESS"),
	       IvyXml.getAttrString(xml,"THREAD"),
	       IvyXml.getAttrString(xml,"FRAME"),
	       IvyXml.getAttrEnum(xml,"ACTION",BedrockDebugAction.NONE),xw);
	 break;
      case "CONSOLEINPUT" :
	 bedrock_runtime.consoleInput(IvyXml.getAttrString(xml,"LAUNCH"),
	       IvyXml.getTextElement(xml,"INPUT"));
	 break;
      case "GETSTACKFRAMES" :
	 bedrock_runtime.getStackFrames(IvyXml.getAttrString(xml,"LAUNCH"),
	       IvyXml.getAttrString(xml,"THREAD"),
	       IvyXml.getAttrInt(xml,"COUNT",-1),
	       IvyXml.getAttrInt(xml,"DEPTH",0),
	       IvyXml.getAttrInt(xml,"ARRAY",100),xw);
	 break;
      case "VARVAL" :
	 bedrock_runtime.getVariableValue(IvyXml.getAttrString(xml,"THREAD"),
	       IvyXml.getAttrString(xml,"FRAME"),
	       IvyXml.getTextElement(xml,"VAR"),
	       IvyXml.getAttrInt(xml,"DEPTH",1),
	       IvyXml.getAttrInt(xml,"ARRAY",100),xw);
	 break;
      case "VARDETAIL" :
	 bedrock_runtime.getVariableValue(IvyXml.getAttrString(xml,"THREAD"),
	       IvyXml.getAttrString(xml,"FRAME"),
	       IvyXml.getTextElement(xml,"VAR"),-1,
	       IvyXml.getAttrInt(xml,"ARRAY",100),xw);
	 /********
	    bedrock_runtime.getVariableDetails(IvyXml.getAttrString(xml,"THREAD"),
						  IvyXml.getAttrString(xml,"FRAME"),
						  IvyXml.getTextElement(xml,"VAR"),xw);
	 **********/
	 break;
      case "EVALUATE" :
	 bedrock_runtime.evaluateExpression(proj,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getTextElement(xml,"EXPR"),
	       IvyXml.getAttrString(xml,"THREAD"),
	       IvyXml.getAttrString(xml,"FRAME"),
	       IvyXml.getAttrBool(xml,"IMPLICIT",false),
	       IvyXml.getAttrBool(xml,"BREAK",true),
	       IvyXml.getAttrString(xml,"REPLYID"),
	       IvyXml.getAttrInt(xml,"LEVEL"),
	       IvyXml.getAttrInt(xml,"ARRAY"),
	       IvyXml.getAttrString(xml,"SAVEID"),
	       IvyXml.getAttrBool(xml,"ALLFRAMES"),xw);
	 break;
      case "EDITPARAM" :
	 bedrock_editor.handleParameter(IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"NAME"),
	       IvyXml.getAttrString(xml,"VALUE"));
	 break;
      case "STARTFILE" :
	 bedrock_editor.handleStartFile(proj,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrString(xml,"ID"),
	       IvyXml.getAttrBool(xml,"CONTENTS",false),xw);
	 break;
      case "FINISHFILE" :
	 bedrock_editor.handleFinishFile(proj,IvyXml.getAttrString(xml,"BID"),
	       IvyXml.getAttrString(xml,"FILE"));
	 break;
      case "EDITFILE" :
	 bedrock_editor.handleEdit(proj,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrString(xml,"ID"),
	       getEditSet(xml),xw);
	 break;
      case "COMMIT" :
	 bedrock_editor.handleCommit(proj,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrBool(xml,"REFRESH",false),
	       IvyXml.getAttrBool(xml,"SAVE",false),
	       IvyXml.getAttrBool(xml,"COMPILE",false),
	       getElements(xml,"FILE"),xw);
	 break;
      case "CREATEPRIVATE" :
	 bedrock_editor.createPrivateBuffer(proj,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"PID"),
	       IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrString(xml,"FROMPID"),xw);

	 break;
      case "PRIVATEEDIT" :
	 bedrock_editor.handleEdit(proj,IvyXml.getAttrString(xml,"PID","*"),
	       IvyXml.getAttrString(xml,"FILE"),
	       null,
	       getEditSet(xml),xw);
	 break;
      case "REMOVEPRIVATE" :
	 bedrock_editor.removePrivateBuffer(proj,
	       IvyXml.getAttrString(xml,"PID"),
	       IvyXml.getAttrString(xml,"FILE"));
	 break;
      case "DELETE" :
	 bedrock_editor.handleDelete(proj,
	       IvyXml.getAttrString(xml,"WHAT"),
	       IvyXml.getAttrString(xml,"PATH"));
	 break;
      case "GETCOMPLETIONS" :
	 bedrock_editor.handleGetCompletions(proj,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrInt(xml,"OFFSET"),xw);
	 break;
      case "ELIDESET" :
	 bedrock_editor.elisionSetup(proj,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrBool(xml,"COMPUTE",true),
	       getElements(xml,"REGION"),xw);
	 break;
      case "FILEELIDE" :
	 bedrock_editor.fileElide(IvyXml.getBytesElement(xml,"FILE"),xw);
	 break;
      case "RENAME" :
	 bedrock_editor.rename(proj,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrInt(xml,"START"),IvyXml.getAttrInt(xml,"END"),
	       IvyXml.getAttrString(xml,"NAME"),IvyXml.getAttrString(xml,"HANDLE"),
	       IvyXml.getAttrString(xml,"NEWNAME"),
	       IvyXml.getAttrBool(xml,"KEEPORIGINAL",false),
	       IvyXml.getAttrBool(xml,"RENAMEGETTERS",false),
	       IvyXml.getAttrBool(xml,"RENAMESETTERS",false),
	       IvyXml.getAttrBool(xml,"UPDATEHIERARCHY",false),
	       IvyXml.getAttrBool(xml,"UPDATEQUALIFIED",false),
	       IvyXml.getAttrBool(xml,"UPDATEREFS",true),
	       IvyXml.getAttrBool(xml,"UPDATESIMILAR",false),
	       IvyXml.getAttrBool(xml,"UPDATETEXT",false),
	       IvyXml.getAttrBool(xml,"DOEDIT",false),
	       IvyXml.getAttrString(xml,"FILES"),xw);
	 break;
      case "MOVEELEMENT" :
	 bedrock_editor.moveElement(proj,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"WHAT"),
	       IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrInt(xml,"START"),IvyXml.getAttrInt(xml,"END"),
	       IvyXml.getAttrString(xml,"NAME"),IvyXml.getAttrString(xml,"HANDLE"),
	       IvyXml.getAttrString(xml,"TARGET"),
	       IvyXml.getAttrBool(xml,"UPDATEQUALIFIED",true),
	       IvyXml.getAttrBool(xml,"UPDATEREFS",true),
	       IvyXml.getAttrBool(xml,"EDIT",false),xw);
	 break;
      case "RENAMERESOURCE" :
	 bedrock_editor.renameResource(proj,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrString(xml,"NEWNAME"),xw);
	 break;
      case "EXTRACTMETHOD" :
	 bedrock_editor.extractMethod(proj,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrInt(xml,"START"),IvyXml.getAttrInt(xml,"END"),
	       IvyXml.getAttrString(xml,"NEWNAME"),
	       IvyXml.getAttrBool(xml,"REPLACEDUPS",false),
	       IvyXml.getAttrBool(xml,"COMMENTS",false),
	       IvyXml.getAttrBool(xml,"EXCEPTIONS",false),xw);
	 break;
      case "FORMATCODE" :
	 bedrock_editor.formatCode(proj,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrInt(xml,"START"),
	       IvyXml.getAttrInt(xml,"END"),xw);
	 break;
      case "FINDREGIONS" :
	 bedrock_editor.getTextRegions(proj,IvyXml.getAttrString(xml,"BID","*"),
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
      case "FINDBYKEY" :
	 bedrock_editor.findByKey(proj,
	       IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"KEY"),
	       IvyXml.getAttrString(xml,"FILE"), xw);
	 break;
      case "CALLPATH" :
	 bedrock_call.getCallPath(proj,IvyXml.getAttrString(xml,"FROM"),
	       IvyXml.getAttrString(xml,"TO"),
	       IvyXml.getAttrBool(xml,"SHORTEST",false),
	       IvyXml.getAttrInt(xml,"LEVELS",0),xw);
	 break;
      case "FIXIMPORTS" :
	 bedrock_editor.handleFixImports(proj,
	       IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrInt(xml,"DEMAND",0),
	       IvyXml.getAttrInt(xml,"STATICDEMAND",0),
	       IvyXml.getAttrString(xml,"ORDER"),
	       IvyXml.getAttrString(xml,"ADD"),xw);
	       break;
      case "GETEXPECTEDTYPE" :
	 bedrock_editor.handleGetExpectedType(proj,
	       IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrInt(xml,"LINE"),xw);
	 break;
      case "PREFERENCES" :
	 bedrock_project.handlePreferences(proj,xw);
	 break;
      case "SETPREFERENCES" :
	 Element pxml = IvyXml.getChild(xml,"profile");
	 if (pxml == null) pxml = IvyXml.getChild(xml,"OPTIONS");
	 bedrock_project.handleSetPreferences(proj,pxml,xw);
	 break;
      case "LOGLEVEL" :
	 log_level = IvyXml.getAttrEnum(xml,"LEVEL",BedrockLogLevel.ERROR);
	 break;
      case "GETHOST" :
	 h1 = h2 = h3 = null;
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
	 break;
      case "GETPROXY" :
	 getProxyForHost(IvyXml.getAttrString(xml,"HOST"),xw);
	 break;
      case "QUICKFIX" :
	 // done in the front end for now
	 // bedrock_editor.handleCommit(proj,null,false,false,null,null);
	 quick_fixer.handleQuickFix(proj,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrInt(xml,"OFFSET"),
	       IvyXml.getAttrInt(xml,"LENGTH"),
	       getElements(xml,"PROBLEM"),xw);
	 break;
      case "REFACTOR" :
	 bedrock_editor.executeRefactoring(proj,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"FILE"),IvyXml.getAttrString(xml,"ID"),
	       xml,xw);
	 break;
      case "HOVERDATA" :
         break;
      case "ENTER" :
	 BedrockApplication.enterApplication();
	 ++num_clients;
	 xw.text(Integer.toString(num_clients));
	 break;
      case "LANGUAGEDATA" :
	 handleLanguageData(xw);
	 break;

      case "SAVEWORKSPACE" :
	 saveEclipse();
	 xw.text("SAVED");
	 break;
      case "EXIT" :
	 logD("EXIT Request received " + num_clients + " " + doing_exit);
	 if (--num_clients <= 0) {
	    xw.text("EXITING");
	    forceExit();
	  }
	 break;
      default :
	 xw.close();
	 throw new BedrockException("Unknown plugin command " + cmd);
    }

   xw.end("RESULT");

   long delta = System.currentTimeMillis() - start;

   String rslt = xw.toString();

   if (rslt.length() > 1024)
      BedrockPlugin.logD("Result (" + delta + ") = " + rslt.substring(0,1023) + " ...");
   else
      BedrockPlugin.logD("Result (" + delta + ") = " + rslt);

   return rslt;
}



void forceExit()
{
   logD("FORCE EXIT");
   doing_exit = true;
   saveEclipse();
   BedrockApplication.stopApplication();
   BedrockPlugin.logD("Stopping application");
   shutdown_mint = true;
}







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
      if (elts == null) elts = new ArrayList<>();
      elts.add(c);
    }

   return elts;
}




/********************************************************************************/
/*										*/
/*	Methods for handling edits						*/
/*										*/
/********************************************************************************/

private List<EditData> getEditSet(Element xml)
{
   List<EditData> edits = new ArrayList<EditData>();

   for (Element c : IvyXml.children(xml,"EDIT")) {
      EditDataImpl edi = new EditDataImpl(c);
      edits.add(edi);
    }

   return edits;
}



private static class EditDataImpl implements EditData {

   private int start_offset;
   private int end_offset;
   private String edit_text;

   EditDataImpl(Element e) {
      start_offset = IvyXml.getAttrInt(e,"START");
      end_offset = IvyXml.getAttrInt(e,"END",start_offset);
      edit_text = IvyXml.getText(e);
      if (edit_text != null && edit_text.length() == 0) edit_text = null;
      if (edit_text != null && IvyXml.getAttrBool(e,"ENCODE")) {
	 byte [] bytes = IvyXml.stringToByteArray(edit_text);
	 edit_text = new String(bytes);
       }
    }

   @Override public int getOffset()			{ return start_offset; }
   @Override public int getLength()			{ return end_offset - start_offset; }
   @Override public String getText()			{ return edit_text; }

}	// end of innerclass EditDataImpl



/********************************************************************************/
/*										*/
/*	Get language data							*/
/*										*/
/********************************************************************************/

private void handleLanguageData(IvyXmlWriter xw)
{
   Element xml = getLanguageData();
   logD("LANGUAGE DATA " + IvyXml.convertXmlToString(xml));
   xw.writeXml(xml);
}



Element getLanguageData()
{
   String nm = "resources/launches-java.xml";
   InputStream ins = BedrockPlugin.class.getClassLoader().getResourceAsStream(nm);
   BedrockPlugin.logD("Language data " + nm + " " + ins + " " +
			 BedrockPlugin.class.getClassLoader().getResource(nm));
   Element xml = IvyXml.loadXmlFromStream(ins);
   if (xml == null) {
      String nm1 = "launches-java.xml";
      ins = BedrockPlugin.class.getClassLoader().getResourceAsStream(nm1);
      BedrockPlugin.logD("Language data " + nm1 + " " + ins + " " +
			    BedrockPlugin.class.getClassLoader().getResource(nm1));
      xml = IvyXml.loadXmlFromStream(ins);
    }
   if (xml == null) {
      ins = BedrockPlugin.class.getClassLoader().getResourceAsStream(nm);
      try {
	 String txt = IvyFile.loadFile(ins);
	 BedrockPlugin.logE("BAD language resource file:\n" + txt);
      }
      catch (IOException e) {
	 BedrockPlugin.logE("Bad language resource read " + e);
      }
    }

   return xml;
}




/********************************************************************************/
/*										*/
/*	Methods for handling proxy requests					*/
/*										*/
/********************************************************************************/

private void getProxyForHost(String host,IvyXmlWriter xw)
{
   try {
      Bundle bdl = getBundle();
      if (bdl == null) return;
      BundleContext ctx = bdl.getBundleContext();
      ServiceReference<?> svr = ctx.getServiceReference("org.eclipse.core.net.proxy.IProxyService");
      if (svr == null) return;
      IProxyService ips = (IProxyService) ctx.getService(svr);
      if (ips == null) return;

      URI uri = new URI(host);
      IProxyData [] pds = ips.select(uri);
      for (IProxyData pd : pds) {
	 xw.begin("PROXY");
	 xw.field("TYPE",pd.getType());
	 xw.field("PORT",pd.getPort());
	 xw.field("HOST",pd.getHost());
	 if (pd.isRequiresAuthentication()) {
	    xw.field("USER",pd.getUserId());
	    xw.field("PWD",pd.getPassword());
	  }
	 xw.end("PROXY");
       }
    }
   catch (Throwable t) {
      logD("Problem getting proxy information for " + host + ": " + t);
    }
}




/********************************************************************************/
/*										*/
/*	Mint handlers								*/
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
      catch (BedrockException e) {
	 String xmsg = "BEDROCK: error in command " + cmd + ": " + e;
	 BedrockPlugin.logE(xmsg,e);
	 rslt = "<ERROR><![CDATA[" + xmsg + "]]></ERROR>";
       }
      catch (Throwable t) {
	 String xmsg = "BEDROCK: Problem processing command " + cmd + ": " + t + " " +
	    doing_exit + " " + shutdown_mint + " " +  num_clients;
	 BedrockPlugin.logE(xmsg);
	 System.err.println(xmsg);
	 t.printStackTrace();
	 StringWriter sw = new StringWriter();
	 PrintWriter pw = new PrintWriter(sw);
	 t.printStackTrace(pw);
	 Throwable xt = t;
	 for (	; xt.getCause() != null; xt = xt.getCause());
	 if (xt != null && xt != t) {
	    rslt += "\n";
	    xt.printStackTrace(pw);
	  }
	 BedrockPlugin.logE("TRACE: " + sw.toString());
	 rslt = "<ERROR>";
	 rslt += "<MESSAGE>" + xmsg + "</MESSAGE>";
	 rslt += "<EXCEPTION><![CDATA[" + t.toString() + "]]></EXCEPTION>";
	 rslt += "<STACK><![CDATA[" + sw.toString() + "]]></STACK>";
	 rslt += "</ERROR>";
       }

      msg.replyTo(rslt);

      if (shutdown_mint) mint_control.shutDown();
    }

}	// end of subclass CommandHandler





/********************************************************************************/
/*										*/
/*	Logging methods 							*/
/*										*/
/********************************************************************************/

static void logE(String msg,Throwable t) { log(BedrockLogLevel.ERROR,msg,t); }

static void logE(String msg)		{ log(BedrockLogLevel.ERROR,msg,null); }

static void logW(String msg)		{ log(BedrockLogLevel.WARNING,msg,null); }

static void logI(String msg)		{ log(BedrockLogLevel.INFO,msg,null); }

static void logD(String msg)		{ log(BedrockLogLevel.DEBUG,msg,null); }

static void logX(String msg)
{
   try {
      throw new Error();
    }
   catch (Error x) {
      log(BedrockLogLevel.DEBUG,msg,x);
    }
}

static void logEX(String msg)
{
   try {
      throw new Error();
    }
   catch (Error x) {
      log(BedrockLogLevel.ERROR,msg,x);
    }
}



static void log(String msg)		{ logI(msg); }



static void log(BedrockLogLevel lvl,String msg,Throwable t)
{
   if (lvl.ordinal() > log_level.ordinal()) return;

   String pfx = "BEDROCK:" + lvl.toString().substring(0,1) + ": ";

   if (log_file != null) {
      log_file.println(pfx + msg);
      if (t != null) {
	 t.printStackTrace(log_file);
	 Throwable r = null;
	 for (r = t.getCause(); r != null && r.getCause() != null; r = r.getCause());
	 if (r != null) r.printStackTrace(log_file);
       }
    }
   if (use_stderr || log_file == null) {
      System.err.println(pfx + msg);
      if (t != null) t.printStackTrace();
    }
   if (log_file != null) log_file.flush();
}


static void setLogLevel(BedrockLogLevel lvl)
{
   log_level = lvl;
}



}	// end of class BedrockPlugin




/* end of BedrockPlugin.java */

