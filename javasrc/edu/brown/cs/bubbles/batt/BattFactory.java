/********************************************************************************/
/*										*/
/*		BattFactory.java						*/
/*										*/
/*	Bubble Automated Testing Tool factory class for bubbles integration	*/
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

import edu.brown.cs.bubbles.bale.BaleConstants;
import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;
import edu.brown.cs.bubbles.bump.BumpLocation;

import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.exec.IvyExecQuery;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.swing.SwingEventListenerList;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;


public class BattFactory implements BattConstants, BudaConstants.ButtonListener,
		MintConstants, BaleConstants, BudaConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BoardProperties 	batt_props;
private boolean 		server_running;
private BattModeler		batt_model;
private ErrorStatus		error_status;
private SwingEventListenerList<BattPopupHandler> popup_handlers;

private static BattFactory	the_factory = null;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public synchronized static BattFactory getFactory()
{
   if (the_factory == null) {
      the_factory = new BattFactory();
    }
   return the_factory;
}



private BattFactory()
{
   batt_props = BoardProperties.getProperties("Batt");
   batt_model = new BattModeler();
   server_running = false;
   error_status = null;
   popup_handlers = new SwingEventListenerList<BattPopupHandler>(BattPopupHandler.class);
   
   if (batt_props.getBoolean("Batt.record.test.status")) {
      new BattRecorder(this);
    }
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

public static void setup()
{
   BudaRoot.addBubbleConfigurator("BATT",new BattConfigurator());
   BudaRoot.registerMenuButton(TEST_BUTTON,getFactory());
}



public static void initialize(BudaRoot br)
{
   BattFactory bf = getFactory();

   switch (BoardSetup.getSetup().getRunMode()) {
      case SERVER :
	 bf.startBattServer();
	 break;
      default:
	 break;
    }

   BaleFactory.getFactory().addContextListener(new BattContexter(bf));

   bf.setupErrorHandler();
}




/********************************************************************************/
/*										*/
/*	Get all test cases for a methods					*/
/*										*/
/********************************************************************************/

public List<BattTest> getAllTestCases(String mthd)
{
   List<BattTest> all = new ArrayList<>();

   if (mthd == null) return null;

   for (BattTestCase btc : batt_model.getAllTests()) {
      UseMode um = btc.usesMethod(mthd);
      switch (um) {
	 case UNKNOWN :
	 case INDIRECT :
	 case DIRECT :
	    all.add(btc);
	    break;
	 case NONE :
	 default:
	    break;
       }
    }

   if (all.size() == 0) return null;

   return all;
}



public List<BattTest> getAllTestCases()
{
   List<BattTest> all = new ArrayList<>();
   for (BattTestCase btc : batt_model.getAllTests()) {
      all.add(btc);
    }
   return all;
}




public BudaBubble createNewTestBubble(BattTestBubbleCallback cbk)
{
   BattNewTestBubble ntb = new BattNewTestBubble(cbk);

   BudaBubble bb = ntb.createNewTestBubble();

   return bb;
}


public BattNewTestPanel createNewTestPanel(BattTestBubbleCallback cbk)
{
   BattNewTestBubble ntb = new BattNewTestBubble(cbk);

   BattNewTestPanel pnl = ntb.createNewTestPanel();

   return pnl;
}



public void addPopupHandler(BattPopupHandler hdlr)
{
   popup_handlers.add(hdlr);
}


public void removePopupHandler(BattPopupHandler hdlr)
{
   popup_handlers.remove(hdlr);
}



public void addBattModelListener(BattModelListener bml)
{
   batt_model.addBattModelListener(bml);
}

public void removedBattModelListener(BattModelListener bml)
{
   batt_model.removeBattModelListener(bml);
}



/********************************************************************************/
/*										*/
/*	Menu button handling							*/
/*										*/
/********************************************************************************/

@Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt)
{
   BudaBubble bb = null;

   if (id.equals(TEST_BUTTON)) {
      bb = createStatusBubble();
    }

   if (bba != null && bb != null) {
      bba.addBubble(bb,null,pt,0);
      bb.grabFocus();
    }
}



void handleExternalPopups(JPopupMenu menu,BudaBubble bbl,BattTestCase tc)
{
   for (BattPopupHandler hdlr : popup_handlers) {
      hdlr.handlePopupMenu(tc,bbl,menu);
    }
}



/********************************************************************************/
/*										*/
/*	BattBubble setup							*/
/*										*/
/********************************************************************************/

BudaBubble createStatusBubble()
{
   return new BattStatusBubble(batt_model);
}



/********************************************************************************/
/*										*/
/*	Batt agent setup							*/
/*										*/
/********************************************************************************/

void updateTests()
{
   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   MintDefaultReply rply = new MintDefaultReply();
   mc.send("<BATT DO='SHOWALL' />",rply,MINT_MSG_FIRST_NON_NULL);
   Element e = rply.waitForXml();
   if (e != null) batt_model.updateTestModel(e,true);
}



void runTests(RunType rt)
{
   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   mc.send("<BATT DO='RUNTESTS' TYPE='" + rt.toString() + "' />");
}


void stopTest()
{
   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   mc.send("<BATT DO='STOPTEST' />");
}


void runTest(BattTestCase test)
{
   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   mc.send("<BATT DO='RUNTEST' TEST='" + test.getName() + "' />");
}


TestMode getTestMode()
{
   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   MintDefaultReply rply = new MintDefaultReply();
   mc.send("<BATT DO='MODE' />",rply,MINT_MSG_FIRST_NON_NULL);
   Element e = rply.waitForXml();
   if (IvyXml.isElement(e,"RESULT")) {
      String md = IvyXml.getText(e);
      if (md.equals("CONTINUOUS")) return TestMode.CONTINUOUS;
      else if (md.equals("DEMAND")) return TestMode.ON_DEMAND;
      else if (md.equals("ON_DEMAND")) return TestMode.ON_DEMAND;
    }
   return null;
}



void setTestMode(TestMode md)
{
   if (md == null) return;

   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   mc.send("<BATT DO='SETMODE' VALUE='" + md.toString() + "' />");
}



void findNewTests()
{
   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   mc.send("<BATT DO='UPDATE' />");
}



void sendErrorFiles(Collection<File> files)
{
   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("BATT");
   xw.field("DO","ERRORS");
   for (File f : files) {
      xw.textElement("FILE",f);
    }
   xw.end("BATT");
   mc.send(xw.toString());
   xw.close();
}



void startBattServer()
{
   BoardSetup bs = BoardSetup.getSetup();
   MintControl mc = bs.getMintControl();

   synchronized (this) {
      if (server_running) return;

      mc.register("<BATT TYPE='_VAR_0' />",batt_model);

      long mxmem = Runtime.getRuntime().maxMemory();
      mxmem = Math.min(512*1024*1024L,mxmem);

      List<String> args = new ArrayList<>();
      args.add(IvyExecQuery.getJavaPath());
      args.add("-Xmx" + Long.toString(mxmem));
      args.add("-cp");
      args.add(System.getProperty("java.class.path"));
      args.add("edu.brown.cs.bubbles.batt.BattMain");
      if (batt_props.getBoolean("Batt.server.continuous",false)) args.add("-C");
      else args.add("-S");
      args.add("-m");
      args.add(bs.getMintName());

      String s0 = bs.getLibraryPath("junit.jar");
      String s1 = bs.getLibraryPath("battjunit.jar");
      String s2 = bs.getLibraryPath("battagent.jar");
      if (s0 == null || s1 == null || s2 == null) {
	 BoardProperties sp = BoardProperties.getProperties("System");
	 String s3 = sp.getProperty("edu.brown.cs.bubbles.jar");
	 BoardLog.logX("BATT","Missing batt file " + s0 + " " + s1 + " " + s2 + " " + s3);
	 server_running = true;
	 return;
       }
      args.add("-u");
      args.add(s1);
      args.add("-a");
      args.add(s2);
      args.add("-l");
      args.add(s0);

      for (int i = 0; i < 100; ++i) {
	 MintDefaultReply rply = new MintDefaultReply();
	 mc.send("<BATT DO='PING' />",rply,MINT_MSG_FIRST_NON_NULL);
	 String rslt;
	 if (i == 0) rslt = rply.waitForString(1000);
	 else rslt = rply.waitForString();
	 if (rslt != null) {
	    server_running = true;
	    break;
	  }
	 if (i == 0) {
	    try {
	       IvyExec exec = new IvyExec(args,null,IvyExec.ERROR_OUTPUT);
	       BoardLog.logD("BATT","Run " + exec.getCommand());
	    }
	    catch (IOException e) {
	       break;
	     }
	  }
	 try {
	    wait(2000);
	  }
	 catch (InterruptedException e) { }
       }
      if (!server_running) {
	 BoardLog.logE("BATT","Unable to start batt server");
	 server_running = true; 	// don't try again
       }
    }
}




/********************************************************************************/
/*										*/
/*	Batt editor context listener						*/
/*										*/
/********************************************************************************/

private static class BattContexter implements BaleContextListener {

   BattModeler batt_model;

   BattContexter(BattFactory bf) {
      batt_model = bf.batt_model;
    }

   @Override public BudaBubble getHoverBubble(BaleContextConfig cfg) {
      return null;
    }

   @Override public String getToolTipHtml(BaleContextConfig cfg) {
      return null;
    }

   @Override public void addPopupMenuItems(BaleContextConfig cfg,JPopupMenu menu) {
      String mthd = cfg.getMethodName();
      if (mthd == null) return;
      List<BattTestCase> direct = new ArrayList<BattTestCase>();
      List<BattTestCase> all = new ArrayList<BattTestCase>();
      for (BattTestCase btc : batt_model.getAllTests()) {
	 UseMode um = btc.usesMethod(mthd);
	 switch (um) {
	    case DIRECT :
	       direct.add(btc);
	       all.add(btc);
	       break;
	    case INDIRECT :
	       all.add(btc);
	       break;
	    default:
	       break;
	  }
	}

       if (all.size() > 0) {
	  if (direct.size() > 0) {
	     TestDisplayAction tda = new TestDisplayAction("Show Test Cases",direct,cfg.getEditor());
	     menu.add(tda);
	   }
	  if (all.size() != direct.size()) {
	     TestDisplayAction tda = new TestDisplayAction("Show Tests Using Method",all,cfg.getEditor());
	     menu.add(tda);
	   }
	}

       Set<String> classes = new TreeSet<String>();
       if (direct.size() > 0) {
	  for (BattTestCase bct : direct) {
	     String cnm = bct.getClassName();
	     classes.add(cnm);
	   }
	}

       String cnm = mthd;
       int idx1= cnm.indexOf("(");
       if (idx1 > 0) cnm = cnm.substring(0,idx1);
       int idx2 = cnm.lastIndexOf(".");
       if (idx2 > 0) cnm = cnm.substring(0,idx2);
       int idx3 = cnm.lastIndexOf(".");
       boolean isinner = false;
       for ( ; ; ) {
	  int idx4 = cnm.lastIndexOf("$");
	  if (idx4 > 0 && idx4 > idx3) cnm = cnm.substring(0,idx4);
	  else break;
	  isinner = true;
	}
       String tcnm = cnm + "Test";
       String pnm = cfg.getEditor().getContentProject();
       List<BumpLocation> locs = BumpClient.getBump().findTypes(pnm,cnm);
       if (locs != null) {
	  for (BumpLocation loc : locs) {
	     String nm = loc.getSymbolName();
	     if (nm.equals(cnm) && Modifier.isPublic(loc.getModifiers())) {
		List<BumpLocation> cntrs = BumpClient.getBump().findMethods(pnm,cnm,false,true,true,false);
		boolean cok = false;
		if (cntrs.size() == 0) cok = true;
		for (BumpLocation cloc : cntrs) {
		   String prms = cloc.getParameters();
		   if (Modifier.isPublic(loc.getModifiers()) &&  prms != null && prms.equals("()")) cok = true;
		 }
		if (cok) classes.add(cnm);
	      }
	     else if (nm.equals(tcnm) && Modifier.isPublic(loc.getModifiers()) && !isinner) {
		List<BumpLocation> mthds = BumpClient.getBump().findMethod(pnm,mthd,false);
		boolean fok = false;
		for (BumpLocation bl : mthds) {
		   if (Modifier.isPrivate(bl.getModifiers())) fok = false;
		   else fok = true;
		}

		if (fok) classes.add(cnm);
	      }
	   }
	}

       String mnm = mthd;
       int idx = mnm.indexOf("(");
       if (idx >= 0) mnm = mnm.substring(0,idx);
       idx = mnm.lastIndexOf(".");
       if (idx >= 0) mnm = mnm.substring(idx+1);

       for (String c : classes) {
	  menu.add(new NewTestAction(mthd,NewTestMode.USER_CODE,mnm,pnm,c,false,cfg.getEditor()));
	}
       if (!classes.contains(tcnm)) {
	  menu.add(new NewTestAction(mthd,NewTestMode.USER_CODE,mnm,pnm,tcnm,true,cfg.getEditor()));
	}
    }

   @Override public void noteEditorAdded(BaleWindow cfg)	{ }
   @Override public void noteEditorRemoved(BaleWindow cfg)	{ }

}	// end of inner class BattContexter




private static class TestDisplayAction extends AbstractAction {

   private List<BattTestCase> test_cases;
   private JComponent source_area;

   TestDisplayAction(String id,List<BattTestCase> cases,JComponent src) {
      super(id);
      test_cases = cases;
      source_area = src;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BoardMetrics.noteCommand("BDDT",getValue(Action.NAME).toString());
      List<BumpLocation> locs = new ArrayList<BumpLocation>();
      BumpClient bc = BumpClient.getBump();
      for (BattTestCase btc : test_cases) {
         String c = btc.getClassName();
         String m = btc.getMethodName();
         m = c + "." + m;
         List<BumpLocation> fl = bc.findMethod(null,m,false);
         if (fl == null) continue;
         locs.addAll(fl);
       }
      if (locs.size() > 0) {
         BaleFactory.getFactory().createBubbleStack(source_area,null,null,false,locs,BudaConstants.BudaLinkStyle.NONE);
       }
    }

}	// end of inner class TestDisplayAction




private static class NewTestAction extends AbstractAction {

   private JComponent source_area;
   private NewTestMode test_mode;
   private String method_name;
   private String in_class;
   private String in_project;
   private boolean create_class;

   NewTestAction(String mthd,NewTestMode mode,String nm,String proj,String cls,boolean newcls,JComponent src) {
      super(getButtonName(mode,nm,cls,newcls));
      source_area = src;
      test_mode = mode;
      method_name = mthd;
      in_project = proj;
      in_class = cls;
      create_class = newcls;
      int idx = mthd.lastIndexOf("(");
      if (idx > 0) idx = mthd.lastIndexOf(".",idx);
      else idx = mthd.lastIndexOf(".");
      if (idx >= 0) mthd = mthd.substring(idx+1);
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BoardMetrics.noteCommand("BDDT","NewTest_" + test_mode);
      BumpClient bc = BumpClient.getBump();
      List<BumpLocation> locs = bc.findMethod(in_project,method_name,false);
      if (locs == null || locs.size() == 0) return;
      BumpLocation loc = locs.get(0);
      BattNewTestBubble ntb = new BattNewTestBubble(method_name,loc,in_project,in_class,create_class,test_mode);
      BudaBubble bb = ntb.createNewTestBubble();
      if (bb == null) return;
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(source_area);
      if (bba == null) return;
      bba.addBubble(bb,source_area,null,PLACEMENT_PREFER|PLACEMENT_MOVETO|PLACEMENT_NEW);
    }

   private static String getButtonName(NewTestMode mode,String nm,String cls,boolean newcls) {
      String typ = null;
      switch (mode) {
         case INPUT_OUTPUT :
            typ = "Input-Output Test";
            break;
         case CALL_SEQUENCE :
            typ = "Call Sequence Test";
            break;
         case USER_CODE :
            typ = "Test Method";
            break;
       }
      typ = "Create New " + typ + " in ";
      if (newcls) typ += " new class ";
      typ += cls;
   
      return typ;
    }

}	// end of inner class NewTestAction




/********************************************************************************/
/*										*/
/*	Methods to create a launch configuration				*/
/*										*/
/********************************************************************************/

public static BumpLaunchConfig getLaunchConfigurationForTest(BattTest btc)
{
   BumpClient bc = BumpClient.getBump();
   BumpRunModel brm = bc.getRunModel();

   for (BumpLaunchConfig blc : brm.getLaunchConfigurations()) {
      if (!blc.isWorkingCopy() && blc.getConfigType() == BumpLaunchConfigType.JUNIT_TEST) {
	 if (blc.getTestName() != null && blc.getTestName().equals(btc.getMethodName()) &&
	       btc.getClassName().equals(blc.getMainClass()))
	    return blc;
       }
    }

   String nm = btc.getName();
   int idx = nm.indexOf("(");
   if (idx >= 0) nm = nm.substring(0,idx);

   String pnm = null;
   String cnm = btc.getClassName();
   List<BumpLocation> locs = bc.findAllClasses(cnm);
   if (locs != null && locs.size() > 0) {
      BumpLocation loc = locs.get(0);
      pnm = loc.getProject();
    }

   BumpLaunchConfig blc = brm.createLaunchConfiguration(nm,BumpLaunchConfigType.JUNIT_TEST);
   if (blc == null) return null;

   BumpLaunchConfig blc1 = blc;
   if (pnm != null) blc1 = blc1.setProject(pnm);
   blc1 = blc1.setMainClass(btc.getClassName());
   blc1 = blc1.setTestName(btc.getMethodName());
   blc1 = blc1.setJunitKind("junit4");
   blc1 = blc1.setAttribute("org.eclipse.debug.core.preferred_launchers",
	 "{[debug]=org.eclipse.jdt.junit.launchconfig}");
   blc = blc1.save();

   return blc;
}




/********************************************************************************/
/*										*/
/*	ErrorStatus -- maintain file error status				*/
/*										*/
/********************************************************************************/

private void setupErrorHandler()
{
   error_status = new ErrorStatus();
   BumpClient.getBump().addProblemHandler(null,error_status);
}



private class ErrorStatus implements BumpConstants.BumpProblemHandler {

   private Set<File> error_files;
   private Set<File> check_files;

   ErrorStatus() {
      error_files = new ConcurrentSkipListSet<File>();
      check_files = new ConcurrentSkipListSet<File>();
      for (BumpProblem bp : BumpClient.getBump().getAllProblems()) {
	 addProblem(bp);
       }
    }

   @Override public void handleProblemAdded(BumpProblem bp) {
      addProblem(bp);
    }

   @Override public void handleProblemRemoved(BumpProblem bp) {
      if (isError(bp)) {
         check_files.add(bp.getFile());
       }
    }

   @Override public void handleProblemsDone() {
      for (File f : check_files) {
	 int ct = 0;
	 for (BumpProblem bp : BumpClient.getBump().getProblems(f)) {
	    if (isError(bp)) ++ct;
	  }
	 if (ct > 0) error_files.add(f);
	 else error_files.remove(f);
       }
      check_files.clear();
      sendErrorFiles(error_files);
    }

   @Override public void handleClearProblems() {
      check_files.addAll(error_files);
    }

   private boolean isError(BumpProblem bp) {
      switch (bp.getErrorType()) {
	 case ERROR :
	 case FATAL :
	    return true;
	 default :
	    return false;
       }
    }

   private void addProblem(BumpProblem bp) {
      if (isError(bp)) {
	 File bf = bp.getFile();
	 check_files.remove(bf);
	 error_files.add(bf);
       }
    }

}	// end of inner class ErrorStatus




}	// end of class BattFactory




/* end of BattFactory.java */


