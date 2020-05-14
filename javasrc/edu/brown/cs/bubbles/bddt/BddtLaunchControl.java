/********************************************************************************/
/*										*/
/*		BddtLaunchControl.java						*/
/*										*/
/*	Bubbles Environment dyanmic debugger tool process/launch controller	*/
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



package edu.brown.cs.bubbles.bddt;

import edu.brown.cs.bubbles.bale.BaleConstants;
import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardFileSystemView;
import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.swing.SwingEventListenerList;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.xml.IvyXml;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileSystemView;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;



class BddtLaunchControl extends BudaBubble implements BddtConstants, BumpConstants, BaleConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BumpClient		bump_client;
private BumpLaunchConfig	launch_config;
private BumpProcess		cur_process;
private JLabel			state_label;
private LaunchState		launch_state;
private RunEventHandler 	event_handler;
private EditorContextListener	editor_handler;
private Map<BumpThread,ExecutionAnnot> exec_annots;
private BumpStackFrame		active_frame;
private FrameAnnot		frame_annot;
private BddtBubbleManager	bubble_manager;
private int			freeze_count;
private SwingEventListenerList<BddtFrameListener> frame_listeners;
private Action			stepinto_action;
private Action			stepuser_action;
private FileSystemView		file_system;

private JPanel			launch_panel;

private static AtomicInteger expr_counter = new AtomicInteger(0);



/********************************************************************************/
/*										*/
/*	Constructors/destructors						*/
/*										*/
/********************************************************************************/

BddtLaunchControl(BumpLaunchConfig blc)
{
   bump_client = BumpClient.getBump();
   BumpRunModel brm = bump_client.getRunModel();
   launch_config = blc;
   if (blc != null) {
      blc = brm.getLaunchConfiguration(blc.getId());
      if (blc != null) launch_config = blc;
    }

   cur_process = null;
   launch_state = LaunchState.READY;
   exec_annots = new ConcurrentHashMap<BumpThread,ExecutionAnnot>();
   active_frame = null;
   frame_annot = null;
   freeze_count = 0;
   frame_listeners = new SwingEventListenerList<BddtFrameListener>(BddtFrameListener.class);

   file_system = BoardFileSystemView.getFileSystemView();

   setupPanel();

   setContentPane(launch_panel);

   event_handler = new RunEventHandler();
   bump_client.getRunModel().addRunEventHandler(event_handler);
   editor_handler = new EditorContextListener();
   BaleFactory.getFactory().addContextListener(editor_handler);
   bubble_manager = new BddtBubbleManager(this);

   String log = launch_config.getLogFile();
   if (log != null) BddtFactory.getFactory().getConsoleControl().setLogFile(this,log);
}



@Override protected void localDispose()
{
   bump_client.getRunModel().removeRunEventHandler(event_handler);
   BaleFactory.getFactory().removeContextListener(editor_handler);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BumpProcess getProcess()			{ return cur_process; }
String getProject()				{ return launch_config.getProject(); }
BddtBubbleManager getBubbleManager()		{ return bubble_manager; }

boolean matchProcess(BumpProcess p)
{
   if (cur_process == p) return true;
   if (p != null && cur_process == null) return true;
   return false;
}


private void setProcess(BumpProcess p)
{
   cur_process = p;

   BddtFactory.getFactory().setProcess(BddtLaunchControl.this,p);

   String log = launch_config.getLogFile();
   if (log != null && p != null) BddtFactory.getFactory().getConsoleControl().setLogFile(p,log);
}



boolean frameFileExists(BumpStackFrame frm)
{
   if (frm.getFile() == null) return false;
   File rf = file_system.createFileObject(frm.getFile().getPath());
   return rf.exists();
}


boolean fileExists(File f)
{
   if (f == null) return false;
   File rf = file_system.createFileObject(f.getPath());
   return rf.exists();
}


boolean fileExists(String f)
{
   if (f == null) return false;
   File rf = file_system.createFileObject(f);
   return rf.exists();
}


BumpThread getLastStoppedThread()
{
   return event_handler.getLastStoppedThread();
}



/********************************************************************************/
/*										*/
/*	Methods to set up the display panel					*/
/*										*/
/********************************************************************************/

private void setupPanel()
{
   SwingGridPanel pnl = new SwingGridPanel();

   BoardColors.setColors(pnl,"Bddt.control.background");

   int y = 0;

   String nm = launch_config.getProject() + " : " + launch_config.getConfigName();

   JLabel ttl = new JLabel(nm);
   BoardColors.setColors(ttl,"Bddt.control.background");
   pnl.addGBComponent(ttl,0,y++,0,1,10,0);
   pnl.addGBComponent(new JSeparator(),0,y++,0,1,1,0);

   BoardProperties bp = BoardProperties.getProperties("Bddt");

   JToolBar btnbar = new JToolBar();
   btnbar.add(new PlayAction());
   stepinto_action = new StepIntoAction();
   stepuser_action = new StepUserAction();
   if (bp.getBoolean("Bddt.buttons.StepInto",true)) btnbar.add(stepinto_action);
   if (bp.getBoolean("Bddt.buttons.StepUser",true)) btnbar.add(stepuser_action);
   btnbar.add(new StepOverAction());
   btnbar.add(new StepReturnAction());
   btnbar.add(new PauseAction());
   if (bp.getBoolean("Bddt.buttons.DropToFrame",true)) btnbar.add(new DropToFrameAction());
   btnbar.addSeparator();
   btnbar.add(new StopAction());
   btnbar.addSeparator();
   btnbar.add(new ClearAction());

   btnbar.setFloatable(false);
   btnbar.setMargin(new Insets(2,2,2,2));
   pnl.addGBComponent(btnbar,0,y++,0,1,1,0);

   pnl.addGBComponent(new JSeparator(),0,y++,0,1,1,0);

   synchronized (this) {
      state_label = new JLabel(launch_state.toString());
      BoardColors.setColors(state_label,"Bddt.control.background");
      pnl.addGBComponent(state_label,0,y++,0,1,1,0);
    }

   pnl.addGBComponent(new JSeparator(),0,y++,0,1,1,0);

   JToolBar bblbar = new JToolBar();
   bblbar.add(new ConsoleAction());
   bblbar.add(new ThreadsAction());
   if (bp.getBoolean("Bddt.buttons.History",true)) bblbar.add(new HistoryAction());
   if (bp.getBoolean("Bddt.buttons.Performance",true)) bblbar.add(new PerformanceAction());
   if (bp.getBoolean("Bddt.buttons.Swing",false)) bblbar.add(new SwingAction());

   bblbar.add(new ValueViewerBubbleAction());
   bblbar.add(new EvaluationBubbleAction());
   bblbar.addSeparator();
   bblbar.add(new NewChannelAction());
   bblbar.setFloatable(false);
   bblbar.setMargin(new Insets(2,2,2,2));
   pnl.addGBComponent(bblbar,0,y++,0,1,1,0);

   launch_panel = pnl;
}




/*****************************************fgrep SOLUTIONS ../tests.out/samples/sampletest*.debug
***************************************/
/*										*/
/*	Keystroke handling							*/
/*										*/
/********************************************************************************/

void setupKeys()
{
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
   if (bba == null) return;

   registerKey(bba,new StepUserAction(),KeyStroke.getKeyStroke(KeyEvent.VK_F5,0));
   registerKey(bba,new StepUserAction(),KeyStroke.getKeyStroke(KeyEvent.VK_F5,InputEvent.CTRL_DOWN_MASK));
   registerKey(bba,new StepIntoAction(),KeyStroke.getKeyStroke(KeyEvent.VK_F5,InputEvent.SHIFT_DOWN_MASK));
   registerKey(bba,new StepOverAction(),KeyStroke.getKeyStroke(KeyEvent.VK_F6,0));
   registerKey(bba,new StepReturnAction(),KeyStroke.getKeyStroke(KeyEvent.VK_F7,0));
   registerKey(bba,new PlayAction(),KeyStroke.getKeyStroke(KeyEvent.VK_F8,0));
   registerKey(bba,new PauseAction(),KeyStroke.getKeyStroke(KeyEvent.VK_F8,InputEvent.SHIFT_DOWN_MASK));
}


private void registerKey(BudaBubbleArea bba,Action act,KeyStroke k)
{
   String cmd = (String) act.getValue(Action.NAME);
   bba.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(k,cmd);
   bba.getActionMap().put(cmd,act);
}



/********************************************************************************/
/*										*/
/*	Initial bubble management						*/
/*										*/
/********************************************************************************/

void setupInitialBubbles()
{
   BoardProperties bp = BoardProperties.getProperties("Bddt");

   if (bp.getBoolean("Bddt.show.console")) {
      bubble_manager.createConsoleBubble();
    }
   if (bp.getBoolean("Bddt.show.threads")) {
      bubble_manager.createThreadBubble();
    }
   if (bp.getBoolean("Bddt.show.history")) {
      bubble_manager.createHistoryBubble();
    }
   if (bp.getBoolean("Bddt.show.swing")) {
      bubble_manager.createSwingBubble();
    }
   if (bp.getBoolean("Bddt.show.perf")) {
      bubble_manager.createPerformanceBubble();
    }
   if (bp.getBoolean("Bddt.show.eval")) {
      bubble_manager.createValueViewerBubble();
    }
   if (bp.getBoolean("Bddt.show.interact")) {
      bubble_manager.createInteractionBubble();
    }
}




/********************************************************************************/
/*										*/
/*	Popup menu handling							*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   JPopupMenu menu = new JPopupMenu();

   menu.add(getFloatBubbleAction());

   menu.show(this,e.getX(),e.getY());
}



/********************************************************************************/
/*										*/
/*	State maintenance							*/
/*										*/
/********************************************************************************/

void setLaunchState(LaunchState ls)
{
   setLaunchState(ls,0,0);
}




synchronized void setLaunchState(LaunchState ls,int rct,int tct)
{
   LaunchState prior = launch_state;
   launch_state = ls;

   if (state_label != null) {
      String lbl = ls.toString();
      switch (launch_state) {
	 default :
	    break;
	 case STARTING :
	    lbl = "SAVE, COMPILE, & START";
	    break;
	 case PARTIAL_PAUSE :
	    lbl += " (" + Integer.toString(tct-rct) + "/" + Integer.toString(tct) + ")";
	    break;
       }
      state_label.setText(lbl);
      switch (launch_state) {
	 case PAUSED :
	 case PARTIAL_PAUSE :
	    if (prior == LaunchState.RUNNING) {
	       if (isVisible() && !isShowing()) {
		  Window w = null;
		  for (Container c = this; c != null; c = c.getParent()) {
		     if (c instanceof Window) {
			w = (Window) c;
			break;
		      }
		   }
		  if (w != null) w.toFront();
		}
	     }
	    break;
	 default :
	    break;
       }
    }
}




/********************************************************************************/
/*										*/
/*	Debugging button actions						*/
/*										*/
/********************************************************************************/

private class PlayAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   PlayAction() {
      super("Play",BoardImage.getIcon("debug/play"));
      putValue(SHORT_DESCRIPTION,"Start or continue execution (F8)");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      switch (launch_state) {
	 case READY :
	 case TERMINATED :
	    BoardMetrics.noteCommand("BDDT","StartDebug");
	    setProcess(null);
	    setLaunchState(LaunchState.STARTING);
	    bubble_manager.restart();
	    BoardThreadPool.start(new StartDebug());
	    break;
	 case STARTING :
	 case RUNNING :
	    break;
	 case PAUSED :
	 case PARTIAL_PAUSE :
	    if (cur_process != null) {
	       BoardMetrics.noteCommand("BDDT","ResumeDebug");
	       waitForFreeze();
	       bump_client.resume(cur_process);
	     }
	    break;
       }
    }

}	// end of inner class PlayAction




private class PauseAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   PauseAction() {
      super("Pause",BoardImage.getIcon("debug/pause"));
      putValue(SHORT_DESCRIPTION,"Pause execution (shift-F8)");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      switch (launch_state) {
	 case READY :
	 case TERMINATED :
	 case PAUSED :
	    break;
	 case STARTING :
	 case RUNNING :
	 case PARTIAL_PAUSE :
	    if (cur_process != null) {
	       BoardMetrics.noteCommand("BDDT","SuspendDebug");
	       bump_client.suspend(cur_process);
	     }
	    break;
       }
    }

}	// end of inner class PauseAction




private class StopAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   StopAction() {
      super("Stop",BoardImage.getIcon("debug/stop"));
      putValue(SHORT_DESCRIPTION,"Terminate execution");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      switch (launch_state) {
	 case READY :
	 case TERMINATED :
	    break;
	 case STARTING :
	 case RUNNING :
	 case PARTIAL_PAUSE :
	 case PAUSED :
	    if (cur_process != null) {
	       BoardMetrics.noteCommand("BDDT","TerminateDebug");
	       waitForFreeze();
	       bump_client.terminate(cur_process);
	     }
	    break;
       }
    }

}	// end of inner class StopAction




private class ClearAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   ClearAction() {
      super("Clear Bubbles",BoardImage.getIcon("debug/brush"));
      putValue(SHORT_DESCRIPTION,"Clear bubbles from debug workspace");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BddtLaunchControl.this);

      Collection<BudaBubble> bbls;
      BudaWorkingSet ws = BddtFactory.getFactory().getActiveWorkingSet();
      if (ws != null) {
	 bbls = bba.getBubblesInRegion(ws.getRegion());
       }
      else {
	 bbls = bba.getBubbles();
       }

      for (BudaBubble bbl : bbls) {
	 if (bbl.isFloating() || bbl.isFixed()) continue;
	 bba.userRemoveBubble(bbl);
       }
    }

}	// end of inner class ClearAction




private class StepIntoAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   StepIntoAction() {
      super("Step Into",BoardImage.getIcon("debug/stepin"));
      putValue(SHORT_DESCRIPTION,"Step into (Shift-F5)");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      switch (launch_state) {
	 case READY :
	 case TERMINATED :
	    PlayAction pa = new PlayAction();
	    pa.actionPerformed(evt);
	    break;
	 case STARTING :
	 case RUNNING :
	    break;
	 case PARTIAL_PAUSE :
	 case PAUSED :
	    BumpThread bt = event_handler.getLastStoppedThread();
	    if (bt != null) {
	       BoardMetrics.noteCommand("BDDT","StepIntoDebug");
	       waitForFreeze();
	       bump_client.stepInto(bt);
	     }
	    break;
       }
    }

}	// end of inner class StepIntoAction


private class StepUserAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   StepUserAction() {
      super("Step User",BoardImage.getIcon("debug/stepuser"));
      putValue(SHORT_DESCRIPTION,"Step into user code (F5)");
   }

   @Override public void actionPerformed(ActionEvent evt) {
      if ((evt.getModifiers() & ActionEvent.SHIFT_MASK) != 0) {
	 stepinto_action.actionPerformed(evt);
	 return;
       }

      switch (launch_state) {
	 case READY :
	 case TERMINATED :
	    PlayAction pa = new PlayAction();
	    pa.actionPerformed(evt);
	    break;
	 case STARTING :
	 case RUNNING :
	    break;
	 case PARTIAL_PAUSE :
	 case PAUSED :
	    BumpThread bt = event_handler.getLastStoppedThread();
	    if (bt != null) {
	       BoardMetrics.noteCommand("BDDT","StepUserDebug");
	       waitForFreeze();
	       bump_client.stepUser(bt);
	     }
	    break;
      }
   }

}	// end of inner class StepUserAction



private class StepOverAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   StepOverAction() {
      super("Step Over",BoardImage.getIcon("debug/stepover"));
      putValue(SHORT_DESCRIPTION,"Step over (F6)");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      switch (launch_state) {
	 case READY :
	 case TERMINATED :
	    PlayAction pa = new PlayAction();
	    pa.actionPerformed(evt);
	    break;
	 case STARTING :
	 case RUNNING :
	    break;
	 case PARTIAL_PAUSE :
	 case PAUSED :
	    BumpThread bt = event_handler.getLastStoppedThread();
	    if (bt != null) {
	       BoardMetrics.noteCommand("BDDT","StepOverDebug");
	       waitForFreeze();
	       bump_client.stepOver(bt);
	     }
	    break;
       }
    }

}	// end of inner class StepOverAction




private class StepReturnAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   StepReturnAction() {
      super("Step Return",BoardImage.getIcon("debug/stepreturn"));
      putValue(SHORT_DESCRIPTION,"Step until end of frame and return (F7)");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      switch (launch_state) {
	 case READY :
	 case TERMINATED :
	    PlayAction pa = new PlayAction();
	    pa.actionPerformed(evt);
	    break;
	 case STARTING :
	 case RUNNING :
	    break;
	 case PARTIAL_PAUSE :
	 case PAUSED :
	    BumpThread bt = event_handler.getLastStoppedThread();
	    if (bt != null) {
	       BoardMetrics.noteCommand("BDDT","StepReturnDebug");
	       waitForFreeze();
	       bump_client.stepReturn(bt);
	     }
	    break;
       }
    }

}	// end of inner class StepReturnAction




private class DropToFrameAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   DropToFrameAction() {
      super("Drop to Frame",BoardImage.getIcon("debug/droptoframe"));
      putValue(SHORT_DESCRIPTION,"Start current frame over");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      switch (launch_state) {
	 case READY :
	 case TERMINATED :
	    PlayAction pa = new PlayAction();
	    pa.actionPerformed(evt);
	    break;
	 case STARTING :
	 case RUNNING :
	    break;
	 case PARTIAL_PAUSE :
	 case PAUSED :
	    BumpThread bt = event_handler.getLastStoppedThread();
	    if (bt != null) {
	       if ((evt.getModifiers() & ActionEvent.SHIFT_MASK) != 0) {
		  waitForFreeze();
		  BumpThreadStack stk = bt.getStack();
		  BumpStackFrame frm = stk.getFrame(1);
		  if (frm != null) {
		     BoardMetrics.noteCommand("BDDT","DropToPriorFrameDebug");
		     bump_client.dropToFrame(frm);
		   }
		}
	       else {
		  BoardMetrics.noteCommand("BDDT","DropToFrameDebug");
		  waitForFreeze();
		  bump_client.dropToFrame(bt);
		}
	     }
	    break;
       }
    }

}	// end of inner class DropToFrameAction




private class StartDebug implements Runnable {

   @Override public void run() {
      bump_client.saveAll();
      BudaRoot br = BudaRoot.findBudaRoot(BddtLaunchControl.this);
      if (br == null) return;
      br.handleSaveAllRequest();

      BumpErrorType etyp = bump_client.getErrorType();

      if (etyp == BumpErrorType.ERROR) {
	 int sts = JOptionPane.showConfirmDialog(BddtLaunchControl.this,
	       "Start debugging with compiler errors?",
	       "Error Check for Run",JOptionPane.YES_NO_OPTION,
	       JOptionPane.QUESTION_MESSAGE);
	 if (sts == JOptionPane.YES_OPTION) etyp = BumpErrorType.WARNING;
       }
      if (etyp == BumpErrorType.ERROR || etyp == BumpErrorType.FATAL) {
	 setLaunchState(LaunchState.READY);
	 return;
       }

      String id = "B_" + Integer.toString(((int)(Math.random() * 100000)));
      BumpProcess bp = bump_client.startDebug(launch_config,id);
      setProcess(bp);
      if (bp != null) setLaunchState(LaunchState.RUNNING);
      else setLaunchState(LaunchState.READY);
    }

}	// end of inner class StartDebug



/********************************************************************************/
/*										*/
/*	Bubble button actions							*/
/*										*/
/********************************************************************************/

private class ConsoleAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   ConsoleAction() {
      super("Console",BoardImage.getIcon("debug/console"));
      putValue(SHORT_DESCRIPTION,"Bring up console bubble");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BoardMetrics.noteCommand("BDDT","CreateConsoleBubble");
      bubble_manager.createConsoleBubble();
    }

}	// end of inner class ConsoleAction



private class ThreadsAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   ThreadsAction() {
      super("Threads",BoardImage.getIcon("debug/threads"));
      putValue(SHORT_DESCRIPTION,"Bring up threads bubble");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BoardMetrics.noteCommand("BDDT","CreateThreadBubble");
      bubble_manager.createThreadBubble();
    }

}	// end of inner class ThreadsAction



private class HistoryAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   HistoryAction() {
      super("History",BoardImage.getIcon("debug/history"));
      putValue(SHORT_DESCRIPTION,"Bring up debug history bubble");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BoardMetrics.noteCommand("BDDT","CreateHistoryBubble");
      bubble_manager.createHistoryBubble();
    }

}	// end of inner class HistoryAction



private class PerformanceAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   PerformanceAction() {
      super("Performance",BoardImage.getIcon("debug/perf"));
      putValue(SHORT_DESCRIPTION,"Bring up performance bubble");
   }

   @Override public void actionPerformed(ActionEvent evt) {
      BoardMetrics.noteCommand("BDDT","CreatePerformanceBubble");
      bubble_manager.createPerformanceBubble();
   }

}	// end of inner class PerformanceAction




private class ValueViewerBubbleAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   ValueViewerBubbleAction() {
      super("Evaluation",BoardImage.getIcon("debug/eval"));
      putValue(SHORT_DESCRIPTION,"Bring up value viewer bubble");
   }

   @Override public void actionPerformed(ActionEvent evt) {
      BoardMetrics.noteCommand("BDDT","CreateValueViewerBubble");
      bubble_manager.createValueViewerBubble();
   }

}	// end of inner class ValueViewerBubbleAction





private class EvaluationBubbleAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   EvaluationBubbleAction() {
      super("Interaction",BoardImage.getIcon("debug/interact"));
      putValue(SHORT_DESCRIPTION,"Bring up evaluation bubble");
   }

   @Override public void actionPerformed(ActionEvent evt) {
      BoardMetrics.noteCommand("BDDT","CreateEvaluationBubble");
      bubble_manager.createInteractionBubble();
   }

}	// end of inner class EvaluationBubbleAction



private class SwingAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   SwingAction() {
      super("Swing",BoardImage.getIcon("debug/swing-icon"));
      putValue(SHORT_DESCRIPTION,"Bring up swing debugging bubble");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BoardMetrics.noteCommand("BDDT","CreateSwingBubble");
      bubble_manager.createSwingBubble();
    }

}	// end of inner class SwingAction



private class NewChannelAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   NewChannelAction() {
      super("New Channel",BoardImage.getIcon("debug/newchannel"));
      putValue(SHORT_DESCRIPTION,"Start a new debugging channel for this configuration");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BoardMetrics.noteCommand("BDDT","NewDebugChannel");
      BddtFactory bf = BddtFactory.getFactory();
      bf.newDebugger(launch_config);
    }

}	// end of inner class ThreadsAction



/********************************************************************************/
/*										*/
/*	Methods to handle freeze in background					*/
/*										*/
/********************************************************************************/

private synchronized void waitForFreeze()
{
   while (freeze_count > 0) {
      try {
	 wait(10000);
       }
      catch (InterruptedException e) { }
    }
}


synchronized void startFreeze()
{
   ++freeze_count;
}



synchronized void doneFreeze()
{
   --freeze_count;
   if (freeze_count < 0) freeze_count = 0;
   if (freeze_count <= 0) notifyAll();
}



/********************************************************************************/
/*										*/
/*	Event handling								*/
/*										*/
/********************************************************************************/

private class RunEventHandler implements BumpRunEventHandler {

   private Map<BumpThread,BumpThreadState> thread_states;
   private BumpThread		last_stopped;

   RunEventHandler() {
      thread_states = new HashMap<BumpThread,BumpThreadState>();
      last_stopped = null;
    }

   BumpThread getLastStoppedThread()		{ return last_stopped; }

   @Override public void handleLaunchEvent(BumpRunEvent evt) { }

   @Override synchronized public void handleProcessEvent(BumpRunEvent evt) {
      BumpLaunchConfig elc = evt.getLaunchConfiguration();
      switch (evt.getEventType()) {
	 case PROCESS_ADD :
	    if (cur_process == null && launch_state == LaunchState.STARTING &&
		   (elc == null || elc == launch_config)) {
	       setProcess(evt.getProcess());
	       last_stopped = null;
	       BddtFactory.getFactory().getConsoleControl().clearConsole(cur_process);
	       BddtFactory.getFactory().getHistoryControl().clearHistory(cur_process);
	     }
	    break;
	 case PROCESS_REMOVE :
	    if (cur_process == evt.getProcess()) {
	       setLaunchState(LaunchState.TERMINATED);
	       thread_states.clear();
	       setProcess(null);
	       last_stopped = null;
	     }
	    else if (cur_process == null && launch_state == LaunchState.STARTING &&
			(elc == null || elc == launch_config)) {
	       setLaunchState(LaunchState.TERMINATED);
	       thread_states.clear();
	       last_stopped = null;
	     }
	    break;
	 default:
	    break;
       }
    }

   @Override synchronized public void handleThreadEvent(BumpRunEvent evt) {
      if (evt.getProcess() != cur_process) return;
      BumpThread bt = evt.getThread();
      BumpThreadState ost = thread_states.get(bt);
      BumpThreadState nst = bt.getThreadState();

      switch (evt.getEventType()) {
	 case THREAD_ADD :
	    nst = BumpThreadState.RUNNING;
	    //$FALL-THROUGH$
	 case THREAD_CHANGE :
	    thread_states.put(bt,nst);
	    if (bt.getThreadState() != ost) {
	       handleThreadStateChange(bt,ost);
	       if (bt.getThreadState().isStopped()) last_stopped = bt;
	       else if (last_stopped == bt) last_stopped = null;
	     }
	    break;
	 case THREAD_REMOVE :
	    removeExecutionAnnot(bt);
	    if (bt == last_stopped) last_stopped = null;
	    thread_states.remove(bt);
	    break;
	 case THREAD_TRACE :
	 case THREAD_HISTORY :
	    return;
	 default:
	    break;
       }

      int tct = thread_states.size();
      int rct = 0;
      for (Map.Entry<BumpThread,BumpThreadState> ent : thread_states.entrySet()) {
	 BumpThreadState bts = ent.getValue();
	 if (bts.isStopped() && last_stopped == null) last_stopped = ent.getKey();
	 else if (bts.isRunning()) ++rct;
       }
      if (tct == 0) setLaunchState(LaunchState.TERMINATED);
      else if (rct == 0) setLaunchState(LaunchState.PAUSED);
      else if (rct == tct) setLaunchState(LaunchState.RUNNING);
      else setLaunchState(LaunchState.PARTIAL_PAUSE,rct,tct);
    }

   @Override public void handleConsoleMessage(BumpProcess proc,boolean err,boolean eof,String msg) { }

}	// end of inner class RunEventHandler



/********************************************************************************/
/*										*/
/*	Methods to handle thread state changes					*/
/*										*/
/********************************************************************************/

private void handleThreadStateChange(BumpThread bt,BumpThreadState ost)
{
   // BoardLog.logD("BDDT","Thread state change " + bt.getThreadState() + " " + ost);
   if (bt.getThreadState().isStopped() && (ost != null && !ost.isStopped())) {
      addExecutionAnnot(bt);
      BumpThreadStack stk = bt.getStack();
      if (stk != null) {
	 CreateBubble cb = new CreateBubble(bt);
	 SwingUtilities.invokeLater(cb);
       }
    }
   else if (!bt.getThreadState().isStopped()) {
      removeExecutionAnnot(bt);
    }
}



private class CreateBubble implements Runnable {

   private BumpThread for_thread;

   CreateBubble(BumpThread bt) {
      for_thread = bt;
    }

   @Override public void run() {
      bubble_manager.createExecBubble(for_thread);
    }

}	// end of inner class CreateBubble



/********************************************************************************/
/*										*/
/*	Execution annotations							*/
/*										*/
/********************************************************************************/

private void addExecutionAnnot(BumpThread bt)
{
   removeExecutionAnnot(bt);

   BumpThreadStack stk = bt.getStack();
   if (stk != null && stk.getNumFrames() > 0) {
      BumpStackFrame bsf = stk.getFrame(0);
      if (frameFileExists(bsf) && bsf.getLineNumber() > 0) {
	 ExecutionAnnot ea = new ExecutionAnnot(bt,bsf);
	 exec_annots.put(bt,ea);
	 BaleFactory.getFactory().addAnnotation(ea);
	 setActiveFrame(bsf);
      }
    }
}


private void removeExecutionAnnot(BumpThread bt)
{
   ExecutionAnnot ea = exec_annots.remove(bt);
   if (ea != null) BaleFactory.getFactory().removeAnnotation(ea);
}


void setActiveFrame(BumpStackFrame frm)
{
   if (frm == null) {
      active_frame = null;
      if (frame_annot != null) {
	 BaleFactory.getFactory().removeAnnotation(frame_annot);
	 frame_annot = null;
       }
    }
   else if (active_frame != null && active_frame.match(frm)) {
      for (BddtFrameListener fl : frame_listeners) {
	 fl.setActiveFrame(frm);
       }
    }
   else {
      if (frame_annot != null) {
	 BaleFactory.getFactory().removeAnnotation(frame_annot);
	 frame_annot = null;
       }
      active_frame = frm;
      if (frm.getLevel() != 0 && frm.getFile() != null) {
	 frame_annot = new FrameAnnot(frm);
	 BaleFactory.getFactory().addAnnotation(frame_annot);
       }
      for (BddtFrameListener fl : frame_listeners) {
	 fl.setActiveFrame(active_frame);
       }
    }
}


BumpStackFrame getActiveFrame() 		{ return active_frame; }
void addFrameListener(BddtFrameListener fl)	{ frame_listeners.add(fl); }
void removeFrameListener(BddtFrameListener fl)	{ frame_listeners.remove(fl); }



private class ExecutionAnnot implements BaleAnnotation {

   private BumpThread for_thread;
   private BumpStackFrame for_frame;
   private BaleFileOverview for_document;
   private Position execute_pos;
   private Color annot_color;
   private Color except_color;
   private File for_file;

   ExecutionAnnot(BumpThread th,BumpStackFrame frm) {
      for_thread = th;
      for_frame = frm;
      for_file = frm.getFile();
      boolean lcl = frm.isSystem();

      for_document = BaleFactory.getFactory().getFileOverview(null,for_file,lcl);
      int off = for_document.findLineOffset(frm.getLineNumber());
      annot_color = BoardColors.getColor(BDDT_EXECUTE_ANNOT_COLOR_PROP);
      except_color = BoardColors.getColor(BDDT_EXECUTE_EXCEPT_COLOR_PROP);

      execute_pos = null;
      try {
	 execute_pos = for_document.createPosition(off);
       }
      catch (BadLocationException e) {
	 BoardLog.logE("BDDT","Bad execution position",e);
       }
    }



   @Override public int getDocumentOffset()	{ return execute_pos.getOffset(); }
   @Override public File getFile()		{ return for_file; }

   @Override public Icon getIcon(BudaBubble b) {
      if (for_thread.getExceptionType() != null) return BoardImage.getIcon("execexcept");
      return BoardImage.getIcon("exec");
    }

   @Override public String getToolTip() {
      String exc = for_thread.getExceptionType();
      if (exc == null) {
	 return "Thread " + for_thread.getName() + " stopped at " + for_frame.getLineNumber();
       }
      else {
	 return "Thread " + for_thread.getName() + " stopped at " + for_frame.getLineNumber() +
	    " due to " + exc;
       }
    }

   @Override public Color getLineColor(BudaBubble bbl) {
      BumpStackFrame frm = bubble_manager.getFrameForBubble(bbl);
      if (frm != null && frm != for_frame) return null;

      if (for_thread.getExceptionType() != null) return except_color;
      return annot_color;
    }
   @Override public Color getBackgroundColor()			{ return null; }

   @Override public boolean getForceVisible(BudaBubble bb) {
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bb);
      BudaBubbleArea bba1 = BudaRoot.findBudaBubbleArea(BddtLaunchControl.this);
      return (bba == bba1);
    }

   @Override public int getPriority()				{ return 20; }

   @Override public void addPopupButtons(Component c,JPopupMenu m) { }

}	// end of inner class ExecutionAnnot



private class FrameAnnot implements BaleAnnotation {

   private BumpThread for_thread;
   private BumpStackFrame for_frame;
   private BaleFileOverview for_document;
   private Position execute_pos;
   private Color annot_color;
   private File for_file;

   FrameAnnot(BumpStackFrame frm) {
      for_thread = frm.getThread();
      for_frame = frm;
      for_file = frm.getFile();
      for_document = BaleFactory.getFactory().getFileOverview(null,for_file);
      execute_pos = null;
      if (for_document == null) return;
      int off = for_document.findLineOffset(frm.getLineNumber());
      annot_color = BoardColors.getColor(BDDT_FRAME_ANNOT_COLOR_PROP);

      try {
	 execute_pos = for_document.createPosition(off);
       }
      catch (BadLocationException e) {
	 BoardLog.logE("BDDT","Bad execution position",e);
       }
    }



   @Override public int getDocumentOffset()	{ return execute_pos.getOffset(); }
   @Override public File getFile()		{ return for_file; }

   @Override public Icon getIcon(BudaBubble bbl) {
      return BoardImage.getIcon("exec");
    }

   @Override public String getToolTip() {
      return "Thread " + for_thread.getName() + " frame at " + for_frame.getLineNumber();
    }

   @Override public Color getLineColor(BudaBubble bbl)		{ return annot_color; }
   @Override public Color getBackgroundColor()			{ return null; }

   @Override public boolean getForceVisible(BudaBubble bb) {
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bb);
      BudaBubbleArea bba1 = BudaRoot.findBudaBubbleArea(BddtLaunchControl.this);
      return (bba == bba1);
    }

   @Override public int getPriority()				{ return 20; }

   @Override public void addPopupButtons(Component c,JPopupMenu m) { }

}	// end of inner class FrameAnnot



/********************************************************************************/
/*										*/
/*	Evaluation string methods						*/
/*										*/
/********************************************************************************/

String getEvaluationString(BumpStackFrame frm,BumpRunValue rv,String id)
{
   BoardLanguage bl = BoardSetup.getSetup().getLanguage();
   switch (bl) {
      case JAVA :
      default :
	 return getEvaluationStringJava(frm,rv,id);
      case JS :
	 return getEvaluationStringJS(frm,rv,id);
    }
}



private String getEvaluationStringJava(BumpStackFrame frm,BumpRunValue rv,String id)
{
   if (rv == null && frm != null) {
      rv = frm.getValue(id);
      if (rv == null) {
	 BumpRunValue rv1 = frm.getValue("this");
	 if (rv1 != null) {
	    rv = rv1.getValue("this?" + id);
	  }
       }
    }

   if (rv != null) {
      String s = formatRunValue(rv,0);
      if (s != null) return s;
    }

   if (frm == null) {
      if (rv == null) return null;
      return rv.getDetail();
    }

   String expr = "(" + id.replace('?','.') + ").toString()";
   if (rv != null && rv.getKind() == BumpValueKind.ARRAY) {
      if (rv.getLength() <= 100) {
	 expr = "java.util.Arrays.toString(" + id + ")";
       }
    }
   else if (rv != null && rv.getKind() == BumpValueKind.OBJECT && rv.getValue().equals("null")) {
      return "null";
    }

   BumpThreadState ts = frm.getThread().getThreadState();
   if (!ts.isStopped()) return null;

   EvaluationListener el = new EvaluationListener();
   if (frm.evaluateInternal(expr,null,el)) {
      //TODO: format the result a bit if it is too long
      BumpRunValue rrv = el.getResult();
      if (rrv != null) return IvyXml.htmlSanitize(rrv.getValue());
    }

   return null;
}



private String getEvaluationStringJS(BumpStackFrame frm,BumpRunValue rv,String id)
{
   if (rv != null) {
      String s = formatRunValue(rv,0);
      if (s != null) return s;
    }

   if (frm == null) {
      if (rv == null) return null;
      return rv.getDetail();
    }

   String expr = id;
   if (rv != null && rv.getKind() == BumpValueKind.OBJECT && rv.getValue().equals("null")) {
      return "null";
    }

   BumpThreadState ts = frm.getThread().getThreadState();
   if (!ts.isStopped()) return null;

   EvaluationListener el = new EvaluationListener();
   if (frm.evaluateInternal(expr,null,el)) {
      //TODO: format the result a bit if it is too long
      BumpRunValue rrv = el.getResult();
      if (rrv != null) return IvyXml.htmlSanitize(rrv.getValue());
    }

   return null;
}




ExpressionValue evaluateExpression(BumpStackFrame frm,String uexpr)
{
   if (frm == null) return null;

   String expr = uexpr;
   String saveid = "BEX_" + expr_counter.incrementAndGet();

   BumpThreadState ts = frm.getThread().getThreadState();
   if (!ts.isStopped()) return null;

   EvaluationListener el = new EvaluationListener();
   if (frm.evaluateInternal(expr,saveid,el)) {
      return el;
    }

   return null;
}



private static String formatRunValue(BumpRunValue rv,int lvl)
{
   if (rv == null) return null;

   switch (rv.getKind()) {
      default :
      case CLASS :
      case UNKNOWN :
	 break;
      case OBJECT :
	 String s = rv.getDetail();
	 if (s != null) {
	    s = IvyXml.htmlSanitize(s);
	    s = s.replace("&#0a;","<br>");
	    s = s.replace("&#x0a;","<br>");
	    if (lvl > 0) {
	       Collection<String> vars = rv.getVariables();
	       if (vars != null && vars.size() > 0) {
		  StringBuffer buf = new StringBuffer();
		  buf.append("<br><table cellpadding='0' style='margin-left:20px;'>");
		  for (String var : vars) {
		     BumpRunValue srv = rv.getValue(var);
		     if (srv != null) {
			String sr = formatRunValue(srv,lvl-1);
			buf.append("<tr><td><b>");
			String pvar = var;
			int idx = pvar.lastIndexOf("?");
			if (idx > 0) pvar = pvar.substring(idx+1);
			buf.append(pvar);
			buf.append("</b></td><td>:</td><td>");
			buf.append(sr);
			buf.append("</td></tr>");
		      }
		   }
		  buf.append("</table>");
		  s += buf.toString();
		}
	     }
	    return s;
	  }
	 break;
      case ARRAY :
	 if (rv.getLength() <= 100) {
	    s = rv.getDetail();
	    if (s != null) return s;
	  }
	 else {
	    IvyLog.logI("BDDT","Handle large arrays " + rv.getLength());
	  }
	 break;
      case PRIMITIVE :
	 return IvyXml.htmlSanitize(rv.getValue());
      case STRING :
	 String sx = IvyXml.htmlSanitize(rv.getValue());
	 sx = sx.replace("&#0a;","<br>");
	 sx = sx.replace("&#x0a;","<br>");
	 sx = "&quot;" + sx + "&quot;";
	 return sx;
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	Handle contextual operations on debugger editors			*/
/*										*/
/********************************************************************************/

private class EditorContextListener implements BaleFactory.BaleContextListener {

   @Override public BudaBubble getHoverBubble(BaleContextConfig cfg) {
      return null;
    }

   @Override public void addPopupMenuItems(BaleContextConfig cfg,JPopupMenu m) {
      if (isRelevant(cfg)) m.add(new ValueAction(cfg));
    }

   @Override public String getToolTipHtml(BaleContextConfig cfg) {
      if (isRelevant(cfg)) {
	 String id = cfg.getToken();
	 BumpStackFrame frm = bubble_manager.getFrameForBubble(cfg.getEditor());
	 if (frm != null) {
	    BumpRunValue rv = null;
	    switch (cfg.getTokenType()) {
	       case FIELD_ID :
	       case FIELD_DECL_ID :
		  BumpRunValue rv1 = frm.getValue("this");
		  if (rv1 != null) {
		     rv = rv1.getValue("this?" + id);
		   }
		  break;
	       case STATIC_FIELD_ID :
		  BumpRunValue rv2 = frm.getValue("this");
		  if (rv2 != null) {
		     String typ = rv2.getType();
		     typ = typ.replace("$",".");
		     // TODO: need to get static value here
		   }
		  break;
	       default :
		  break;
	     }
	    if (rv == null) rv = frm.getValue(id);
	    String st = getEvaluationString(frm,rv,id);
	    return st;
	  }
       }
      else if (isRelevantFrame(cfg)) {
	 BumpStackFrame frm = bubble_manager.getFrameForBubble(cfg.getEditor());
	 if (frm != null) {
	    StringBuffer buf = new StringBuffer();
	    buf.append("Thread: " + frm.getThread().getName());
	    buf.append("<br>Line: " + frm.getLineNumber());
	    return buf.toString();
	  }
       }
      return null;
    }

   @Override public void noteEditorAdded(BaleWindow win)	{ }
   @Override public void noteEditorRemoved(BaleWindow win)	{ }

   private boolean isRelevant(BaleContextConfig cfg) {
      BudaBubble bb = cfg.getEditor();
      if (bb == null) return false;
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bb);
      BudaBubbleArea bba1 = BudaRoot.findBudaBubbleArea(BddtLaunchControl.this);
      if (bba == null || bba != bba1) return false;
      if (cur_process == null || !cur_process.isRunning()) return false;
      if (cfg.getToken() == null) return false;
      switch (cfg.getTokenType()) {
	 case FIELD_ID :
	 case LOCAL_ID :
	 case STATIC_FIELD_ID :
	 case LOCAL_DECL_ID :
	 case FIELD_DECL_ID :
	 case CONST_ID :
	    break;
	 default :
	    return false;
       }
      return true;
    }

   private boolean isRelevantFrame(BaleContextConfig cfg) {
      BudaBubble bb = cfg.getEditor();
      if (bb == null) return false;
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bb);
      BudaBubbleArea bba1 = BudaRoot.findBudaBubbleArea(BddtLaunchControl.this);
      if (bba == null || bba != bba1) return false;
      if (cur_process == null || !cur_process.isRunning()) return false;
      return true;
    }

}	// end of inner class EditorContextListener



/********************************************************************************/
/*										*/
/*	Evaluation handlers							*/
/*										*/
/********************************************************************************/

private static class EvaluationListener implements BumpEvaluationHandler, ExpressionValue {

   private boolean is_done;
   private BumpRunValue result_value;
   private String  error_value;

   EvaluationListener() {
      is_done = false;
      result_value = null;
      error_value = null;
    }

   @Override public BumpRunValue getResult() {
      waitForResult();
      return result_value;
    }

   @Override public String getError() {
      waitForResult();
      return error_value;
    }

   @Override public boolean isValid() {
      waitForResult();
      return error_value == null;
    }

   @Override public void evaluationResult(String eid,String expr,BumpRunValue v) {
      result_value = v;
      synchronized (this) {
	 is_done = true;
	 notifyAll();
       }
    }

   @Override public void evaluationError(String eid,String expr,String error) {
      error_value = error;
      synchronized (this) {
	 is_done = true;
	 notifyAll();
       }
    }

   @Override public String formatResult() {
      String s = formatRunValue(result_value,1);
      if (s != null) return s;
      if (result_value == null) return null;
      return result_value.getValue();
    }

   private synchronized void waitForResult() {
      long start = System.currentTimeMillis();
      while (!is_done) {
	 try {
	    wait(1000l);
	  }
	 catch (InterruptedException e) { }
	 if ((System.currentTimeMillis() - start) > 60000) {
	    evaluationError(null,null,"Operation Timeout");
	  }
       }
    }

}	// end of class EvaluationListener




private class ValueEvalListener implements BumpEvaluationHandler, Runnable {

   private BaleContextConfig config_context;
   private BumpRunValue run_value;

   ValueEvalListener(BaleContextConfig ctx) {
      config_context = ctx;
   }

   @Override public void evaluationResult(String eid,String ex,BumpRunValue v) {
      run_value = v;
      SwingUtilities.invokeLater(this);
   }

   @Override public void evaluationError(String eid,String ex,String er) { }

   @Override public void run() {
      if (run_value == null) return;
      BddtStackView bsv = new BddtStackView(BddtLaunchControl.this,run_value,false);
      if (!bsv.isStackValid()) return;
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BddtLaunchControl.this);
      if (bba != null) {
	 bba.addBubble(bsv,config_context.getEditor(),null,
			  PLACEMENT_BELOW|PLACEMENT_LOGICAL|PLACEMENT_GROUPED|PLACEMENT_MOVETO);
       }
    }


}	// end of inner class ValueEvalListener



private class ValueAction extends AbstractAction {

   private BaleContextConfig context_config;

   ValueAction(BaleContextConfig cfg) {
      super("Show value of " + cfg.getToken());
      context_config = cfg;
   }

   @Override public void actionPerformed(ActionEvent evt) {
      BumpStackFrame frm = bubble_manager.getFrameForBubble(context_config.getEditor());
      if (frm == null) return;
      BoardMetrics.noteCommand("BDDT","ShowValue");
      ValueEvalListener el = new ValueEvalListener(context_config);
      frm.evaluateInternal(context_config.getToken(),null,el);
   }

}	// end of inner class ValueAction





}	// end of class BddtLaunchControl




/* end of BddtLaunchControl.java */
