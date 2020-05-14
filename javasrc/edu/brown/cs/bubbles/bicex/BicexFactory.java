/********************************************************************************/
/*										*/
/*		BicexFactory.java						*/
/*										*/
/*	Factory for continuous execution interface				*/
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



package edu.brown.cs.bubbles.bicex;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.w3c.dom.Element;

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleContextConfig;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleContextListener;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleFileOverview;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleWindow;
import edu.brown.cs.bubbles.batt.BattFactory;
import edu.brown.cs.bubbles.batt.BattConstants.BattPopupHandler;
import edu.brown.cs.bubbles.batt.BattConstants.BattTest;
import edu.brown.cs.bubbles.board.*;
import edu.brown.cs.bubbles.bowi.BowiFactory;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaErrorBubble;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaConstants.BubbleViewCallback;
import edu.brown.cs.bubbles.buda.BudaConstants.BudaWorkingSet;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpBreakMode;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpBreakpoint;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpLaunchConfig;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpProcess;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpStackFrame;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpThread;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpThreadStack;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpThreadState;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpThreadStateDetail;
import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.exec.IvyExecQuery;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;


public class BicexFactory implements BicexConstants, MintConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private boolean server_running;
private boolean server_started;
private Map<String,BicexExecution> exec_map;
private BicexExecModel exec_model;

private static BicexFactory	the_factory = new BicexFactory();



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

public static void setup()
{ }


public static void initialize(BudaRoot br)
{
   switch (BoardSetup.getSetup().getLanguage()) {
      case JS :
      case PYTHON :
      case REBUS :
	 return;
      case JAVA :
	 break;
    }

   BudaRoot.registerMenuButton("Bubble.Show Continuous Execution",new StartAction());

   getFactory().exec_model = new BicexExecModel();

   BaleFactory.getFactory().addContextListener(new BicexContextListener());

   getFactory().fixupProjects();

   BattFactory.getFactory().addPopupHandler(new TestPopupHandler());

   BudaRoot.addBubbleViewCallback(new ViewListener());
}


public static BicexFactory getFactory()
{
   return the_factory;
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BicexFactory()
{
   server_running = false;
   server_started = false;
   exec_map = new HashMap<String,BicexExecution>();
   exec_model = null;

   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   mc.register("<SEEDEXEC TYPE='_VAR_0' ID='_VAR_1' />",new UpdateHandler());
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BicexExecModel getExecModel()		{ return exec_model; }



void removeExecution(BicexExecution ex)
{
   exec_map.remove(ex.getExecId());
}



/********************************************************************************/
/*										*/
/*	Methods to run seede w/o user interface 				*/
/*										*/
/********************************************************************************/

public BicexRunner runSeedeOnProcess(BumpProcess bp)
{
   the_factory.startSeede();
   try {
      BicexExecution be = new BicexExecution(bp);
      the_factory.exec_map.put(be.getExecId(),be);
      return be;
    }
   catch (BicexException e) {
      BoardLog.logE("BICEX","Problem starting SEEDE");
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	Methods to show Seede window for fixed execution			*/
/*										*/
/********************************************************************************/

public JComponent showExecution(BicexResult rslt)
{
   return createExecution(rslt);
}


public BudaBubble showExecutionBubble(BicexResult rslt)
{
   BicexEvaluationViewer bev = createExecution(rslt);
   if (bev == null) return null;

   return new BicexEvaluationBubble(bev);
}



private BicexEvaluationViewer createExecution(BicexResult rslt)
{
   if (rslt == null) return null;

   BicexEvaluationResult erslt = (BicexEvaluationResult) rslt;
   BicexExecution bex = erslt.getExecution();
   BicexEvaluationViewer bev = new BicexEvaluationViewer(bex);
   long time = bex.getCurrentContext().getEndTime();
   bex.setCurrentTime(time);

   return bev;
}




public JComponent showExecution(BicexRunner br)
{
   return createExecution(br);
}


public BudaBubble showExecutionBubble(BicexRunner br)
{
   BicexEvaluationViewer bev = createExecution(br);
   if (bev == null) return null;

   return new BicexEvaluationBubble(bev);
}


private BicexEvaluationViewer createExecution(BicexRunner br)
{
   if (br == null) return null;

   BicexExecution bex = (BicexExecution) br;
   BicexEvaluationViewer bev = new BicexEvaluationViewer(bex);
   BicexEvaluationContext ctx = bex.getCurrentContext();
   if (ctx != null) {
      bex.setCurrentContext(null);
      bex.setCurrentContext(ctx);
      long time = ctx.getEndTime();
      bex.setCurrentTime(time);
    }

   return bev;
}


public void gotoContext(JComponent bbl,String ctxid,int sline,int eline)
{
   BicexResultContext ctx = findContext(bbl,ctxid,sline,eline);
   if (ctx == null) return;
   gotoContext(bbl,ctx);
}


public void gotoContext(JComponent bbl,BicexResultContext rctx)
{
   if (rctx == null) return;

   BicexExecution bex = null;

   if (bbl instanceof BicexEvaluationViewer) {
      BicexEvaluationViewer bev = (BicexEvaluationViewer) bbl;
      bex = bev.getExecution();
    }
   else if (bbl instanceof BicexEvaluationBubble) {
      BicexEvaluationBubble bev = (BicexEvaluationBubble) bbl;
      bex = bev.getExecution();
    }

   if (bex != null) {
      BicexEvaluationContext ctx = (BicexEvaluationContext) rctx;
      bex.setCurrentContext(ctx);
    }
}


public BicexResultContext findContext(JComponent bbl,String ctxid,int sline,int eline)
{
   BicexEvaluationContext ctx = null;

   if (bbl instanceof BicexEvaluationViewer) {
      BicexEvaluationViewer bev = (BicexEvaluationViewer) bbl;
      ctx = bev.findContext(ctxid,sline,eline);
    }
   else if (bbl instanceof BicexEvaluationBubble) {
      BicexEvaluationBubble bev = (BicexEvaluationBubble) bbl;
      ctx = bev.findContext(ctxid,sline,eline);
    }

   return ctx;
}


public BicexResultContext getCurrentContext(JComponent bbl)
{
   BicexExecution bex = null;

   if (bbl instanceof BicexEvaluationViewer) {
      BicexEvaluationViewer bev = (BicexEvaluationViewer) bbl;
      bex = bev.getExecution();
    }
   else if (bbl instanceof BicexEvaluationBubble) {
      BicexEvaluationBubble bev = (BicexEvaluationBubble) bbl;
      bex = bev.getExecution();
    }

   if (bex != null) {
      return bex.getCurrentContext();
    }

   return null;
}



public long getCurrentTime(JComponent bbl)
{
   BicexExecution bex = null;

   if (bbl instanceof BicexEvaluationViewer) {
      BicexEvaluationViewer bev = (BicexEvaluationViewer) bbl;
      bex = bev.getExecution();
    }
   else if (bbl instanceof BicexEvaluationBubble) {
      BicexEvaluationBubble bev = (BicexEvaluationBubble) bbl;
      bex = bev.getExecution();
    }

   if (bex != null) {
      return bex.getCurrentTime();
    }

   return 0;
}



public void addPopupHandler(JComponent bbl,BicexPopupCallback cb)
{
   if (bbl instanceof BicexEvaluationBubble) {
      BicexEvaluationBubble ev = (BicexEvaluationBubble) bbl;
      ev.addPopupListener(cb);
    }
   else if (bbl instanceof BicexEvaluationViewer) {
      BicexEvaluationViewer ev = (BicexEvaluationViewer) bbl;
      ev.addPopupListener(cb);
    }
}


public void removePopupHandler(JComponent bbl,BicexPopupCallback cb)
{
   if (bbl instanceof BicexEvaluationBubble) {
      BicexEvaluationBubble ev = (BicexEvaluationBubble) bbl;
      ev.removePopupListener(cb);
    }
   else if (bbl instanceof BicexEvaluationViewer) {
      BicexEvaluationViewer ev = (BicexEvaluationViewer) bbl;
      ev.removePopupListener(cb);
    }
}



public void addToPopupMenu(JComponent bbl,MouseEvent evt,JPopupMenu menu)
{
   if (bbl instanceof BicexEvaluationViewer) {
      BicexEvaluationViewer ev = (BicexEvaluationViewer) bbl;
      ev.addToPopupMenu(evt,menu);
    }
}




/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

static Set<File> computeRegionFiles(BudaBubbleArea bba,Point pt) {
   Set<File> rslt = new HashSet<>();
   Rectangle r = new Rectangle(pt.x,pt.y,10,10);
   Rectangle rgn = bba.computeRegion(r);
   for (BudaBubble bb : bba.getBubblesInRegion(rgn)) {
      File f = bb.getContentFile();
      if (f == null) continue;
      rslt.add(f);
    }
   return rslt;
}


private void handleEditorAdded(BudaBubble bw)
{
   for (BicexExecution bex : exec_map.values()) {
      bex.handleEditorAdded(bw);
    }

}



/********************************************************************************/
/*										*/
/*	Seede communication							*/
/*										*/
/********************************************************************************/

Element sendSeedeMessage(String id,String cmd,CommandArgs args,String cnts)
{
   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();

   MintDefaultReply rply = new MintDefaultReply();
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("SEEDE");
   xw.field("DO",cmd);
   xw.field("SID",id);
   if (args != null) {
      for (Map.Entry<String,Object> ent : args.entrySet()) {
	 xw.field(ent.getKey(),ent.getValue());
       }
    }
   if (cnts != null) {
      xw.xmlText(cnts);
    }
   xw.end("SEEDE");
   String msg = xw.toString();
   xw.close();

   BoardLog.logD("BICEX","Send to SEEDE: " + msg);

   mc.send(msg,rply,MINT_MSG_FIRST_NON_NULL);

   Element rslt = rply.waitForXml(60000);

   BoardLog.logD("BICEX","Reply from SEEDE: " + IvyXml.convertXmlToString(rslt));
   if (rslt == null && (cmd.equals("START") || cmd.equals("BEGIN"))) {
      MintDefaultReply prply = new MintDefaultReply();
      mc.send("<SEEDE DO='PING' SID='*' />",rply,MINT_MSG_FIRST_NON_NULL);
      String prslt = prply.waitForString(3000);
      if (prslt == null) {
	 server_running = false;
	 server_started = false;
	 startSeede();
	 rply = new MintDefaultReply();
	 mc.send(msg,rply,MINT_MSG_FIRST_NON_NULL);
	 rslt = rply.waitForXml(0);
       }
   }

   return rslt;
}




private void startSeede()
{
   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   IvyExec exec = null;
   File wd = new File(bs.getDefaultWorkspace());
   File logf = new File(wd,"seede.log");

   synchronized (this) {
      if (server_running || server_started) return;
      BoardProperties bp = BoardProperties.getProperties("Bicex");
      String dbgargs = bp.getProperty("Bicex.jvm.args");
      List<String> args = new ArrayList<String>();
      args.add(IvyExecQuery.getJavaPath());

      if (dbgargs != null) {
	 StringTokenizer tok = new StringTokenizer(dbgargs);
	 while (tok.hasMoreTokens()) {
	    args.add(tok.nextToken());
	  }
       }

      args.add("-cp");
      String xcp = bp.getProperty("Bicex.seede.class.path");
      if (xcp == null) {
	 xcp = System.getProperty("java.class.path");
	 String ycp = bp.getProperty("Bicex.seede.add.path");
	 if (ycp != null) xcp = ycp + File.pathSeparator + xcp;
       }
      else {
	 BoardSetup setup = BoardSetup.getSetup();
	 StringBuffer buf = new StringBuffer();
	 StringTokenizer tok = new StringTokenizer(xcp,":;");
	 while (tok.hasMoreTokens()) {
	    String elt = tok.nextToken();
	    if (!elt.startsWith("/") &&  !elt.startsWith("\\")) {
	       if (elt.equals("eclipsejar")) {
		  String ejp = setup.getLibraryPath(elt);
		  File ejr = new File(ejp);
		  if (ejr.exists() && ejr.isDirectory()) {
		     for (File nfil : ejr.listFiles()) {
			if (nfil.getName().startsWith("org.eclipse.") && nfil.getName().endsWith(".jar")) {
			   if (buf.length() > 0) buf.append(File.pathSeparator);
			   buf.append(nfil.getPath());
			}
		     }
		   }
		  continue;
		}
	       else {
		  elt = setup.getLibraryPath(elt);
		}
	     }
	    if (buf.length() > 0) buf.append(File.pathSeparator);
	    buf.append(elt);
	  }
	 xcp = buf.toString();
       }

      args.add(xcp);
      args.add("edu.brown.cs.seede.sesame.SeedeMain");
      args.add("-m");
      args.add(bs.getMintName());
      args.add("-L");
      args.add(logf.getPath());
      if (bp.getBoolean("Bicex.seede.debug")) args.add("-D");
      if (bp.getBoolean("Bicex.seede.trace")) args.add("-T");

      for (int i = 0; i < 100; ++i) {
	 MintDefaultReply rply = new MintDefaultReply();
	 mc.send("<SEEDE DO='PING' SID='*' />",rply,MINT_MSG_FIRST_NON_NULL);
	 String rslt = rply.waitForString(1000);
	 if (rslt != null) {
	    server_running = true;
	    break;
	  }
	 if (i == 0) {
	    try {
	       exec = new IvyExec(args,null,IvyExec.ERROR_OUTPUT);     // make IGNORE_OUTPUT to clean up otuput
	       server_started = true;
	       BoardLog.logD("BICEX","Run " + exec.getCommand());
	     }
	    catch (IOException e) {
	       break;
	     }
	  }
	 else {
	    try {
	       if (exec != null) {
		  exec.exitValue();
		  break;
		}
	     }
	    catch (IllegalThreadStateException e) { }
	  }

	 try {
	    wait(2000);
	  }
	 catch (InterruptedException e) { }
       }
      if (!server_running) {
	 BoardLog.logE("BICEX","Unable to start seede server: " + args);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Project fix ups 							*/
/*										*/
/********************************************************************************/

private void fixupProjects()
{
   BumpClient bc = BumpClient.getBump();

   Element xml = bc.getAllProjects();
   if (xml != null) {
      for (Element pe : IvyXml.children(xml,"PROJECT")) {
	 String pnm = IvyXml.getAttrString(pe,"NAME");
	 boolean havepoppy = false;
	 boolean haveseede = false;
	 Element delpath = null;
	 Element opxml = bc.getProjectData(pnm,false,true,false,false,false);
	 if (opxml != null) {
	    Element cpe = IvyXml.getChild(opxml,"CLASSPATH");
	    for (Element rpe : IvyXml.children(cpe,"PATH")) {
	       String bn = null;
	       String ptyp = IvyXml.getAttrString(rpe,"TYPE");
	       if (ptyp != null && ptyp.equals("SOURCE")) {
		  bn = IvyXml.getTextElement(rpe,"OUTPUT");
		}
	       else {
		  bn = IvyXml.getTextElement(rpe,"BINARY");
		}
	       if (bn == null) continue;
	       if (bn.contains("poppy.jar")) {
		  File bfn = new File(bn);
		  if (bfn.exists()) {
		     havepoppy = true;
		   }
		  else {
		     delpath = rpe;
		   }
		}
	       if (bn.contains("seede")) haveseede = true;
	     }
	    if (haveseede && !havepoppy) {
	       opxml = bc.getProjectData(pnm,false,false,true,false,false);
	       Element cp = IvyXml.getChild(cpe,"CLASSES");
	       for (Element fe : IvyXml.children(cp,"TYPE")) {
		  String cnm = IvyXml.getAttrString(fe,"NAME");
		  if (cnm.startsWith("edu.brown.cs.seede.poppy.")) {
		     havepoppy = true;
		     break;
		   }
		}
	     }
	    if (delpath != null) {
	       IvyXmlWriter xwp = new IvyXmlWriter();
	       xwp.begin("PROJECT");
	       xwp.field("NAME",pnm);
	       xwp.begin("PATH");
	       xwp.field("TYPE","LIBRARY");
	       xwp.field("DELETE",true);
	       String src = IvyXml.getTextElement(delpath,"SOURCE");
	       if (src != null) xwp.textElement("SOURCE",src);
	       src = IvyXml.getTextElement(delpath,"OUTPUT");
	       if (src != null) xwp.textElement("OUTPUT",src);
	       src = IvyXml.getTextElement(delpath,"BINARY");
	       if (src != null) xwp.textElement("BINARY",src);
	       xwp.end("PATH");
	       xwp.end("PROJECT");
	       String cnts = xwp.toString();
	       xwp.close();
	       bc.editProject(pnm,cnts);
	     }
	    if (!havepoppy) {
	       String poppyjarnm = BoardSetup.getSetup().getLibraryPath("poppy.jar");
	       File poppyjar = new File(poppyjarnm);
	       if (poppyjar.exists()) {
		  IvyXmlWriter xwp = new IvyXmlWriter();
		  xwp.begin("PROJECT");
		  xwp.field("NAME",pnm);
		  xwp.begin("PATH");
		  xwp.field("TYPE","LIBRARY");
		  xwp.field("NEW",true);
		  xwp.field("BINARY",poppyjar.getPath());
		  xwp.field("EXPORTED",false);
		  xwp.field("OPTIONAL",true);
		  xwp.end("PATH");
		  xwp.end("PROJECT");
		  String cnts = xwp.toString();
		  xwp.close();
		  bc.editProject(pnm,cnts);
		}
	     }
	  }
       }
    }
}




/********************************************************************************/
/*										*/
/*	Button Actions								*/
/*										*/
/********************************************************************************/

private static class StartAction implements BudaConstants.ButtonListener {

   @Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt) {
      BumpProcess bp = findProcess(bba,pt);
   
      BoardMetrics.noteCommand("BICEX","Start");
   
      BowiFactory.startTask();
   
      SeedeStarter ss = new SeedeStarter(bp,bba,pt);
      BoardThreadPool.start(ss);
    }

   private BumpProcess findProcess(BudaBubbleArea bba,Point pt) {
      Object proc = bba.getProperty("Bddt.process");
      if (proc != null) {
	 BumpProcess bp = (BumpProcess) proc;
	 if (bp.isRunning()) return bp;
       }
      BicexExecModel emdl = getFactory().exec_model;
      List<BumpProcess> active = emdl.getActiveProcesses();
      if (active.size() == 0) return null;
      if (active.size() == 1) return active.get(0);
      Set<File> activefiles = computeRegionFiles(bba,pt);
      if (activefiles.isEmpty()) return null;

      for (Iterator<BumpProcess> it = active.iterator(); it.hasNext(); ) {
	 BumpProcess bp = it.next();
	 List<BumpStackFrame> frms = emdl.getActiveFrames(bp);
	 if (frms == null) it.remove();
	 else if (!bp.getName().startsWith("B_")) it.remove();
	 else {
	    boolean fnd = false;
	    for (BumpStackFrame f : frms) {
	       if (activefiles.contains(f.getFile())) fnd = true;
	     }
	    if (!fnd) it.remove();
	  }
       }
      if (active.size() == 1) return active.get(0);

      return null;
    }

}	// end of inner class StartAction



private static class SeedeStarter implements Runnable {

   private BumpProcess	for_process;
   private BudaBubbleArea bubble_area;
   private BudaBubble	eval_bubble;
   private BudaBubble	near_bubble;
   private Point at_point;

   SeedeStarter(BumpProcess bp,BudaBubbleArea bba,Point pt) {
      for_process = bp;
      bubble_area = bba;
      eval_bubble = null;
      at_point = pt;
      near_bubble = null;
    }

   SeedeStarter(BumpProcess bp,BudaBubble near) {
      for_process = bp;
      bubble_area = BudaRoot.findBudaBubbleArea(near);
      eval_bubble = null;
      at_point = null;
      near_bubble = near;
    }

   @Override public void run() {
      if (eval_bubble != null) {
         // running in Swing thread
         BoardMetrics.noteCommand("BICEX","BubbleVisible");
         BoardUserReport.noteReport("seede");
         bubble_area.addBubble(eval_bubble,near_bubble,at_point,
        			  BudaConstants.PLACEMENT_LOGICAL|BudaConstants.PLACEMENT_NEW|
        			  BudaConstants.PLACEMENT_MOVETO|BudaConstants.PLACEMENT_USER);
       }
      else {
         if (for_process == null || !for_process.isRunning()) {
            setupBubble(new BudaErrorBubble("No running process available for continuous evaluation"));
            BowiFactory.stopTask();
          }
         else {
            try {
               the_factory.startSeede();
               BicexExecution be = new BicexExecution(for_process);
               the_factory.exec_map.put(be.getExecId(),be);
               BicexEvaluationViewer bev = new BicexEvaluationViewer(be);
               setupBubble(new BicexEvaluationBubble(bev));
               Set<File> files;
               if (at_point == null && near_bubble != null) {
        	  files = computeRegionFiles(bubble_area,near_bubble.getLocation());
        	}
               else {
        	  files = computeRegionFiles(bubble_area,at_point);
        	}
               if (!files.isEmpty()) be.addFiles(files);
               be.startContinuousExecution();
             }
            catch (BicexException e) {
               BoardLog.logE("BICEX","Problem starting SEEDE: " + e.getMessage(),e);
               setupBubble(new BudaErrorBubble("Problem starting SEEDE"));
             }
            finally {
               BowiFactory.stopTask();
             }
          }
       }
    }

   private void setupBubble(BudaBubble bbl) {
      BudaBubble obbl = eval_bubble;
      if (obbl != null) obbl.setVisible(false);
      eval_bubble = bbl;
      SwingUtilities.invokeLater(this);
    }

}	// end of inner class SeedeStarter






/********************************************************************************/
/*										*/
/*	Message handling							*/
/*										*/
/********************************************************************************/

private class UpdateHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      String type = args.getArgument(0);
      String id = args.getArgument(1);
      Element xml = msg.getXml();
      BicexExecution bex = exec_map.get(id);
      if (bex == null) return;
      String rslt = null;
      try {
	 switch (type) {
	    case "EXEC" :
	       BoardMetrics.noteCommand("BICEX","ExecReturned");
	       bex.handleResult(msg.getXml());
	       break;
	    case "RESET" :
	       BoardMetrics.noteCommand("BICEX","ExecReset");
	       bex.handleReset();
	       break;
	    case "INPUT" :
	       BoardMetrics.noteCommand("BICEX","InputRequest");
	       rslt = bex.handleInput(IvyXml.getAttrString(xml,"FILE"));
	       break;
	    case "INITIALVALUE" :
	       BoardMetrics.noteCommand("BICEX","InitialValueRequest");
	       rslt = bex.handleInitialValue(IvyXml.getAttrString(xml,"WHAT"));
	       break;
	    default :
	       BoardLog.logE("BICEX","Unknown command " + type + " from Seede");
	       break;
	  }
       }
      catch (BicexException e) { }
      msg.replyTo(rslt);
    }

}	// end of inner class UpdateHandler




/********************************************************************************/
/*										*/
/*	Context methods 							*/
/*										*/
/********************************************************************************/

private BicexExecution getExecution(BaleContextConfig cfg)
{
   for (BicexExecution be : exec_map.values()) {
      BicexEvaluationContext ctx = be.getCurrentContext();
      if (ctx == null) continue;
      String mthd = ctx.getMethod();
      if (mthd == null) continue;
      String mthd1 = cfg.getMethodName();
      if (mthd1 == null) continue;
      if (mthd.equals(mthd1)) return be;
      int idx1 = mthd.indexOf("(");
      int idx2 = mthd1.indexOf("(");
      if (idx1 == idx2 && mthd.substring(0,idx1).equals(mthd1.substring(0,idx1))) return be;
    }
   return null;
}




/********************************************************************************/
/*										*/
/*	Editor context listener 						*/
/*										*/
/********************************************************************************/

private static class BicexContextListener implements BaleContextListener {

   @Override public BudaBubble getHoverBubble(BaleContextConfig cfg) {
      return null;
    }

   @Override public void addPopupMenuItems(BaleContextConfig cfg,JPopupMenu m) {
    }

   @Override public String getToolTipHtml(BaleContextConfig cfg) {
      String name = cfg.getToken();
      if (name == null) return null;
      BicexExecution bex = getFactory().getExecution(cfg);
      if (bex == null) return null;
      BicexEvaluationContext ctx = bex.getCurrentContext();
      if (ctx == null) return null;
      String ctxfile = ctx.getFileName();
      if (ctxfile == null) return null;
      File edf = cfg.getEditor().getContentFile();
      if (!edf.getPath().equals(ctxfile)) return null;

      BicexValue val = null;
      String vnm = null;

      switch (cfg.getTokenType()) {
	 case FIELD_ID :
	 case STATIC_FIELD_ID :
	 case LOCAL_ID :
	 case LOCAL_DECL_ID :
	 case FIELD_DECL_ID :
	    vnm = ctx.getValueName(name,cfg.getLineNumber());
	    if (vnm != null) val = ctx.getValues().get(vnm);
	    break;
	 default :
	    break;
       }
      if (val == null) return null;

      List<Integer> times = val.getTimeChanges();
      StringBuffer buf = new StringBuffer();
      buf.append("H: ");
      int idx = 0;
      for (int i = times.size()-1; i >= 0; --i) {
	 int tv = times.get(i);
	 if (tv > bex.getCurrentTime()) continue;
	 if (tv == 0 && vnm != null && vnm.contains("@") && times.size() > 1) continue;
	 String bv = val.getStringValue(tv+1);
	 if (idx++ > 0) buf.append(" &larr; ");
	 buf.append(bv);
      }
      return IvyXml.htmlSanitize(buf.toString());
    }

   @Override public void noteEditorAdded(BaleWindow win)	{ }

   @Override public void noteEditorRemoved(BaleWindow win)	{ }

}



/********************************************************************************/
/*										*/
/*	Bubble listener 							*/
/*										*/
/********************************************************************************/

private static class ViewListener implements BubbleViewCallback {

   @Override public void doneConfiguration()				{ }
   @Override public void focusChanged(BudaBubble bb,boolean set)	{ }
   @Override public void bubbleRemoved(BudaBubble bb)			{ }
   @Override public boolean bubbleActionDone(BudaBubble bb)		{ return false; }
   @Override public void workingSetAdded(BudaWorkingSet ws)		{ }
   @Override public void workingSetRemoved(BudaWorkingSet ws)		{ }
   @Override public void copyFromTo(BudaBubble f,BudaBubble t)		{ }

   @Override public void bubbleAdded(BudaBubble bb) {
      File f = bb.getContentFile();
      if (f == null) return;
      BicexFactory.getFactory().handleEditorAdded(bb);
    }

}


/********************************************************************************/
/*										*/
/*	Handle Test case options						*/
/*										*/
/********************************************************************************/

private static class TestPopupHandler implements BattPopupHandler {

   @Override public void handlePopupMenu(BattTest test,BudaBubble bbl,JPopupMenu menu) {
      if (test == null) return;

      // don't offer to show the test if there are compiler errors
      BumpClient bc = BumpClient.getBump();
      switch (bc.getErrorType()) {
	 case FATAL :
	 case ERROR :
	    return;
	 default :
	    break;
       }

      menu.add(new TestRunnerAction(bbl,test));
    }

}	// end of inner class TestHandler



private static class TestRunnerAction extends AbstractAction {

   private BudaBubble near_bubble;
   private BattTest test_case;
   private BumpProcess debug_process;

   private static final long serialVersionUID = 1;

   TestRunnerAction(BudaBubble bbl,BattTest test) {
      super("Show Execution for " + test.getMethodName());
      near_bubble = bbl;
      test_case = test;
      debug_process = null;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BumpClient bc = BumpClient.getBump();

      BumpLaunchConfig launch = BattFactory.getLaunchConfigurationForTest(test_case);
      if (launch == null) {
	 showError("Couldn't create launch for test case");
	 return;
       }

      String cls = test_case.getClassName();
      String mthd = test_case.getMethodName();

      BowiFactory.startTask();
      BumpBreakpoint bpt = null;
      BumpLocation loc = null;
      List<BumpBreakpoint> origbpts = bc.getAllBreakpoints();
      int lno = 0;

      List<BumpLocation> locs = bc.findMethod(null,cls + "." + mthd,false);
      if (locs == null || locs.isEmpty()) {
	 showError("Couldn't find method " + cls + "." + mthd + " for test case");
	 return;
       }
      loc = locs.get(0);
      BaleFileOverview bfo = BaleFactory.getFactory().getFileOverview(null,loc.getFile());
      lno = bfo.findLineNumber(loc.getDefinitionOffset());

      boolean addfg = bc.getBreakModel().addLineBreakpoint(null,loc.getFile(),null,lno,BumpBreakMode.SUSPEND_THREAD);
      if (!addfg) {
	 BoardLog.logX("BICEX","Failed to create breakpoint " +  lno + " " + loc.getFile() + " " +
	       locs.size() + " " + addfg);
	 showError("Failed to create breakpoint at start of " + mthd);
	 return;
       }
      for (int i = 0; i < 10; ++i) {
	 List<BumpBreakpoint> newbpts = bc.getAllBreakpoints();
	 newbpts.removeAll(origbpts);
	 if (newbpts.size() > 0) {
	    bpt = newbpts.get(0);
	    break;
	  }
	 else {
	    try {
	       Thread.sleep(500);		   // wait for asynch breakpoint to be reported
	     }
	    catch (InterruptedException e) { }
	  }
       }

      if (bpt == null) {
	 BoardLog.logX("BICEX","Failed to find breakpoint " +  lno + " " + loc.getFile() + " " +
	       locs.size() + " " + addfg);
	 showError("Failed to create breakpoint at start of " + mthd);
	 return;
       }

      debug_process = bc.startDebug(launch,null);
      if (debug_process != null) {
	 if (!waitForBreak()) {
	    bc.terminate(debug_process);
	    debug_process = null;
	  }
       }
      bc.getBreakModel().removeBreakpoint(bpt.getBreakId());
      boolean havebpt = false;			   // remove excess breakpoints from failed runs
      for (BumpBreakpoint bp : origbpts) {
	 File file = bp.getFile();
	 if (file.equals(loc.getFile())) {
	    if (bp.getLineNumber() == lno) {
	       if (!havebpt) havebpt = true;
	       else {
		  bc.getBreakModel().removeBreakpoint(bp.getBreakId());
		}
	     }
	  }
       }

      BoardMetrics.noteCommand("BICEX","StartTest");
      SeedeStarter ss = new SeedeStarter(debug_process,near_bubble);
      BoardThreadPool.start(ss);
    }

   private void showError(String msg) {
      BudaErrorBubble bbl = new BudaErrorBubble(msg);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(near_bubble);
      bba.addBubble(bbl,near_bubble,null,
	    BudaConstants.PLACEMENT_LOGICAL|BudaConstants.PLACEMENT_NEW|
	    BudaConstants.PLACEMENT_MOVETO|BudaConstants.PLACEMENT_USER);
    }

   private boolean waitForBreak() {
      BumpClient bc = BumpClient.getBump();

      int ctr = 0;

      for ( ; ; ) {
	 boolean ready = false;
	 boolean stopped = false;
	 for (BumpThread bt : debug_process.getThreads()) {
	    if (bt.getThreadState() == BumpThreadState.STOPPED &&
		  bt.getThreadDetails() == BumpThreadStateDetail.BREAKPOINT) {
	       stopped = true;
	       BumpThreadStack stk = bt.getStack();
	       BumpStackFrame frm = stk.getFrame(0);
	       String nm = test_case.getClassName() + "." + test_case.getMethodName();
	       if (frm.getMethod().equals(nm)) {
		  ready = true;
		  break;
		}
	     }
	  }
	 if (ready || !debug_process.isRunning()) break;
	 else if (stopped) {
	    bc.resume(debug_process);
	    ctr = 0;
	  }

	 if (++ctr > 60) return false;

	 try {
	    Thread.sleep(1000);
	  }
	 catch (InterruptedException e) { }
       }

      return true;
    }

}	// end of inner class TestRunnerAction




}	// end of class BicexFactory




/* end of BicexFactory.java */





