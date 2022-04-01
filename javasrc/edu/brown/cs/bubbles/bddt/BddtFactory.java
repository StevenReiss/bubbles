/********************************************************************************/
/*										*/
/*		BddtFactory.java						*/
/*										*/
/*	Bubbles Environment dyanmic debugger tool factory and setup class	*/
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
import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.board.BoardMouser;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaChannelSet;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaConstraint;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.swing.SwingGridPanel;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;


/**
 *	This class provides the entries for setting up and providing access to
 *	the various debugging bubbles and environment.
 **/

public class BddtFactory implements BddtConstants, BudaConstants.ButtonListener,
					BumpConstants, BaleConstants, BudaConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BudaChannelSet		debug_channels;
private BumpLaunchConfig	current_configuration;
private JLabel			launch_label;
private BudaRoot		buda_root;
private List<BudaWorkingSet>	working_sets;
private BudaWorkingSet		active_working_set;
private Boolean 		debugging_use_workingset;

private static BddtConsoleController console_controller;
private static BddtHistoryController history_controller;
private static BddtFactory	the_factory;
private static BoardProperties	bddt_properties;



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

/**
 *	This routine is called automatically at startup to initialize the module.
 **/

public static void setup()
{
   the_factory = new BddtFactory();
   console_controller = new BddtConsoleController();
   history_controller = new BddtHistoryController();

   bddt_properties = BoardProperties.getProperties("Bddt");

   BudaRoot.addBubbleConfigurator("BDDT",new BddtConfigurator());

   BudaRoot.registerMenuButton(BDDT_BREAKPOINT_BUTTON, the_factory);
   BudaRoot.registerMenuButton(BDDT_CONFIG_BUTTON,the_factory);
   BudaRoot.registerMenuButton(BDDT_PROCESS_BUTTON,the_factory);

   BddtRepository rep = new BddtRepository();
   BassFactory.registerRepository(BudaConstants.SearchType.SEARCH_LAUNCH_CONFIG,rep);
   BassFactory.registerRepository(BudaConstants.SearchType.SEARCH_EXPLORER,rep);
}


/**
 *	Return the singleton instance of the ddttext viewer factory.
 **/

public static BddtFactory getFactory()
{
   return the_factory;
}



public static void initialize(BudaRoot br)
{
   if (the_factory.current_configuration == null) {
      the_factory.setCurrentLaunchConfig(null);
    }

   the_factory.setupDebugging(br);

   BaleFactory.getFactory().addContextListener(new DebugContextListener());
}





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BddtFactory()
{
   debug_channels = null;
   working_sets = null;
   active_working_set = null;
   debugging_use_workingset = null;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BddtConsoleController getConsoleControl()		{ return console_controller; }
BddtHistoryController getHistoryControl()		{ return history_controller; }

BudaWorkingSet getActiveWorkingSet()
{
   boolean useworkingset = bddt_properties.getBoolean("Bddt.use.workingset");
   if (!useworkingset) return null;
   return active_working_set;
}





/********************************************************************************/
/*										*/
/*	Methods to setup up a debugging process 				*/
/*										*/
/********************************************************************************/

public void newDebugger(BumpLaunchConfig blc)
{
   BudaBubbleArea bba = null;

   String label = blc.getProject() + " : " + blc.getConfigName();

   if (debugging_use_workingset) {
      bba = buda_root.getCurrentBubbleArea();
      if (active_working_set == null && working_sets.size() > 0) {
	 active_working_set = working_sets.get(0);
	 active_working_set.setLabel(label);
	 for (BudaBubble bbl : bba.getBubblesInRegion(active_working_set.getRegion())) {
	    bba.removeBubble(bbl);
	  }
	 // might need to remove some floating bubbles as well
       }
      else {
	 createDebuggerWorkingSet(label);
       }
    }
   else {
      if (debug_channels.getNumChannels() == 1 && debug_channels.isChannelEmpty()) {
	 bba = debug_channels.getBubbleArea();
	 debug_channels.setChannelName(label);
       }
      else bba = debug_channels.addChannel(label);
      // Register Component Listener to terminate on close
    }

   if (debugging_use_workingset) {
      active_working_set.setProperty("Bddt.debug",Boolean.TRUE);
    }
   else {
      bba.setProperty("Bddt.debug",Boolean.TRUE);
    }

   setCurrentLaunchConfig(blc);

   BddtLaunchControl ctrl = new BddtLaunchControl(blc);
   console_controller.setupConsole(ctrl);

   if (debugging_use_workingset) {
      Rectangle r = active_working_set.getRegion();
      // save space for tool bar
      int y0 = Math.max(BDDT_LAUNCH_CONTROL_Y,36);
      bba.addBubble(ctrl,null,
	    new Point(r.x + BDDT_LAUNCH_CONTROL_X,r.y + y0),
	    PLACEMENT_EXPLICIT,BudaBubblePosition.MOVABLE);
    }
   else {
      BudaBubblePosition bbp = BudaBubblePosition.MOVABLE;
      if (bddt_properties.getBoolean(BDDT_PROPERTY_FLOAT_LAUNCH_CONTROL)) {
	 bbp = BudaBubblePosition.FLOAT;
       }
      bba.addBubble(ctrl,null,new Point(BDDT_LAUNCH_CONTROL_X,BDDT_LAUNCH_CONTROL_Y),
	    PLACEMENT_EXPLICIT,bbp);
    }

   BudaRoot br = BudaRoot.findBudaRoot(bba);
   if (br == null) return;

   if (debugging_use_workingset) {
      Rectangle r = active_working_set.getRegion();
      Rectangle r1 = bba.getVisibleRect();
      r.width = r1.width;
      r.height = r1.height;
      bba.scrollRectToVisible(r);
      if (bddt_properties.getBoolean(BDDT_PROPERTY_FLOAT_LAUNCH_CONTROL)) {
	 if (active_working_set != null) ctrl.setWorkingSet(active_working_set);
	 else ctrl.setFixed(true);
       }
    }
   else {
      br.setCurrentChannel(ctrl);
    }

   ctrl.setupKeys();
   ctrl.setupInitialBubbles();
}


void setProcess(BddtLaunchControl ctrl,BumpProcess p)
{
   boolean useworkingset = bddt_properties.getBoolean("Bddt.use.workingset");
   if (useworkingset) {
      if (active_working_set != null) {
	 active_working_set.setProperty("Bddt.process",p);
       }
    }
   else {
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(ctrl);
      if (bba != null) {
	 bba.setProperty("Bddt.process",p);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

BumpLaunchConfig getCurrentLaunchConfig()
{
   if (current_configuration == null) return current_configuration;

   return current_configuration;
}


void setCurrentLaunchConfig(BumpLaunchConfig blc)
{
   BumpRunModel brm = BumpClient.getBump().getRunModel();

   if (blc == null) {
      for (BumpLaunchConfig xlc : brm.getLaunchConfigurations()) {
	 if (!xlc.isWorkingCopy()) blc = xlc;
	 break;
       }
    }
   else {
      BumpLaunchConfig xblc = brm.getLaunchConfiguration(blc.getId());
      if (xblc != null) blc = xblc;
    }

   current_configuration = blc;
   if (launch_label != null && blc != null) {
      launch_label.setText(blc.getConfigName());
   }
}




private void setupDebugging(BudaRoot br)
{
   if (debugging_use_workingset != null) return;
   debugging_use_workingset = bddt_properties.getBoolean("Bddt.use.workingset");

   buda_root = br;

   setupDebuggingWorkingSet();
   setupDebuggingChannels();

   setupDebuggingPanel();
}



private void setupDebuggingWorkingSet()
{
   working_sets = new ArrayList<>();
   active_working_set = null;
   BudaRoot.addBubbleViewCallback(new DebugBubbleChecker());
}



private void setupDebuggingChannels()
{
   String dflt = null;
   debug_channels = new BudaChannelSet(buda_root,BDDT_CHANNEL_TOP_COLOR_PROP,
	 BDDT_CHANNEL_BOTTOM_COLOR_PROP,dflt);
   BudaBubbleArea bba = debug_channels.getBubbleArea();
   if (bba != null) {
      bba.setProperty("Bddt.debug",Boolean.TRUE);
      if (bddt_properties.getBoolean("Bddt.grow.down")) {
	 Dimension d = bba.getSize();
	 d.height *= 2;
	 bba.setSize(d);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Debugging panel 							*/
/*										*/
/********************************************************************************/

private void setupDebuggingPanel()
{
   SwingGridPanel pnl = new DebuggingPanel();
   PanelHandler hdlr = new PanelHandler();

   JLabel top = new JLabel("Debug",SwingConstants.CENTER);
   BoardColors.setTransparent(top,pnl);
   pnl.addGBComponent(top,0,0,0,1,1,0);

   JButton btn = defineButton("debug","Switch to the debugging perspective",hdlr);
   BoardColors.setTransparent(btn,pnl);
   pnl.addGBComponent(btn,1,1,1,1,0,0);

   btn = defineButton("new","<html>Create a new debugging channel for current configuration" +
	 " or switch configurations (right click)",hdlr);
   BoardColors.setTransparent(btn,pnl);
   pnl.addGBComponent(btn,2,1,1,1,0,0);
   btn.addMouseListener(new ConfigSelector());

   launch_label = new JLabel();
   BoardColors.setTransparent(launch_label,pnl);
   if (current_configuration != null) {
      launch_label.setText(current_configuration.getConfigName());
    }

   pnl.addGBComponent(launch_label,0,3,0,0,1,1);

   buda_root.addPanel(pnl,true);
   buda_root.registerKeyAction(hdlr,"DEBUG","F2");
}



private static class DebuggingPanel extends SwingGridPanel
{
   private static final long serialVersionUID = 1;

   DebuggingPanel() {
      super();
      BoardColors.setColors(this,BDDT_PANEL_TOP_COLOR_PROP);
    }

   @Override protected void paintComponent(Graphics g0) {
      super.paintComponent(g0);
      Graphics2D g = (Graphics2D) g0.create();
      Color tc = BoardColors.getColor(BDDT_PANEL_TOP_COLOR_PROP);
      Color bc = BoardColors.getColor(BDDT_PANEL_BOTTOM_COLOR_PROP);
      if (tc.equals(bc)) {
	 g.setColor(tc);
       }
      else {
	 Paint p = new GradientPaint(0f, 0f, tc, 0f, this.getHeight(), bc);
	 g.setPaint(p);
       }
      g.fillRect(0, 0, this.getWidth() , this.getHeight());
    }

}	// end of inner class DebuggingPanel



private JButton defineButton(String name,String info,PanelHandler hdlr)
{
   JButton btn = new JButton(BoardImage.getIcon("debug/" + name + ".png"));
   btn.setToolTipText(info);
   btn.setActionCommand(name.toUpperCase());
   btn.setMargin(new Insets(0,1,0,1));
   btn.setOpaque(false);
   btn.setBackground(BoardColors.transparent());
   btn.addActionListener(hdlr);

   return btn;
}



private class PanelHandler extends AbstractAction implements ActionListener {

   private Rectangle prior_viewport;

   PanelHandler() {
      prior_viewport = null;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BudaRoot br = null;
      if (e.getSource() instanceof JButton) {
	 JButton btn = (JButton) e.getSource();
	 br = BudaRoot.findBudaRoot(btn);
      }
      else if (e.getSource() instanceof Component) {
	 Component c = (Component) e.getSource();
	 br = BudaRoot.findBudaRoot(c);
      }
      if (br == null) return;
      String cmd = e.getActionCommand();

      // fix up command
      if (cmd == null) cmd = "DEBUG";
      boolean useworkingset = debugging_use_workingset;
      if (cmd.equals("DEBUG")) {
	 if (useworkingset) {
	    if (working_sets.isEmpty()) cmd = "NEW";
	  }
	 else {
	    if (debug_channels.isChannelEmpty() ||
	      (debug_channels.getNumChannels() == 0 && br.getChannelSet() != debug_channels)) {
	       cmd = "NEW";
	     }
	  }
       }
      BoardLog.logD("BDDT","Panel command " + cmd);
      if (cmd.equals("DEBUG")) {
	 if (current_configuration == null) {
	    createConfiguration();
	  }
	 else {
	    BoardMetrics.noteCommand("BDDT","GotoDebug");
	    if (debug_channels != null && br.getChannelSet() == debug_channels)
	       useworkingset = false;
	    if (useworkingset) {
	       if (active_working_set == null) {
		  setPriorViewport(br);
		  active_working_set = working_sets.get(0);
		}
	       else if (prior_viewport != null) {
		  br.setViewport(prior_viewport.x,prior_viewport.y);
		}
	     }
	    else {
	       if (br.getChannelSet() == debug_channels) br.setChannelSet(null);
	       else br.setChannelSet(debug_channels);
	     }
	  }
       }
      else if (cmd.equals("NEW")) {
	 setPriorViewport(br);
	 if (current_configuration == null) setCurrentLaunchConfig(null);
	
	 if (current_configuration != null) {
	    BoardMetrics.noteCommand("BDDT","NewDebug");
	    newDebugger(current_configuration);
	  }
	 else {
	    createConfiguration();
	  }
       }
    }

   private void setPriorViewport(BudaRoot br) {
      Rectangle r = br.getViewport();
      boolean overlap = false;
      for (BudaWorkingSet bws : working_sets) {
	 if (bws.getRegion().intersects(r)) overlap = true;
       }
      if (!overlap) prior_viewport = br.getViewport();
    }

   private void createConfiguration() {
      CreateConfigAction cca = new CreateConfigAction(BumpLaunchConfigType.JAVA_APP);
      ActionEvent act = new ActionEvent(this,0,"NEW");
      cca.actionPerformed(act);
    }

}	// end of inner class PanelHandler



/********************************************************************************/
/*										*/
/*	Bubble making methods (for non-debug mode)				*/
/*										*/
/********************************************************************************/

BudaBubble makeConsoleBubble(BudaBubble src,BumpProcess proc)
{
   if (proc == null) return null;

   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(src);
   if (bba == null) return null;

   BudaBubble bb = null;
   bb = console_controller.createConsole(proc);
   Rectangle r = src.getBounds();
   int x = r.x;
   int y = r.y + r.height + 20;

   bba.addBubble(bb,BudaBubblePosition.MOVABLE,x,y);

   return bb;
}




/********************************************************************************/
/*										*/
/*	Button handling 							*/
/*										*/
/********************************************************************************/

@Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt)
{
   BudaRoot br = BudaRoot.findBudaRoot(bba);
   BudaBubble bb = null;

   if (id.equals(BDDT_CONFIG_BUTTON)) {
      bb = new BddtConfigView();
    }
   else if (id.equals(BDDT_PROCESS_BUTTON)) {
      bb = new BddtProcessView();
   }
   else if (id.equals(BDDT_BREAKPOINT_BUTTON)) {
      bb = new BddtBreakpointBubble();
    }

   if (br != null && bb != null) {
      BudaConstraint bc = new BudaConstraint(pt);
      br.add(bb,bc);
      bb.grabFocus();
    }
}




/********************************************************************************/
/*										*/
/*	Debugger working set methods						*/
/*										*/
/********************************************************************************/

private static final int CLEAR_SIZE = 1024;
private static final int DEBUG_SIZE = 4096;


private void createDebuggerWorkingSet(String label)
{
   BudaBubbleArea bba = buda_root.getCurrentBubbleArea();
   Rectangle r = buda_root.getCurrentViewport();
   Rectangle dims = bba.getBounds();

   int endloc = r.x + r.width;
   while (endloc < dims.width) {
      Rectangle chkr = new Rectangle(dims);
      chkr.x = endloc;
      chkr.width = CLEAR_SIZE + DEBUG_SIZE;
      Collection<BudaBubble> bbls = bba.getBubblesInRegion(chkr);
      if (bbls.isEmpty()) break;
      for (BudaBubble bbl : bbls) {
	 Rectangle rbbl = bbl.getBounds();
	 endloc = Math.max(endloc,rbbl.x + rbbl.width);
       }
    }
   if (endloc + CLEAR_SIZE > dims.width) endloc = dims.width;
   else endloc = endloc + CLEAR_SIZE;

   Color c = BoardColors.getColor("Bddt.ChannelTopColor");
   Rectangle rgn = new Rectangle(endloc,0,DEBUG_SIZE,r.height);
   BudaWorkingSet ws = bba.createWorkingSet(label,rgn,BudaConstants.WorkingSetGrowth.GROW,c);
   working_sets.add(ws);
   active_working_set = ws;
   ws.setProperty("Bddt.debug",Boolean.TRUE);
}


/********************************************************************************/
/*										*/
/*	Button actions for selecting configurator				*/
/*										*/
/********************************************************************************/

void addNewConfigurationActions(JPopupMenu menu)
{
   switch (BoardSetup.getSetup().getLanguage()) {
      case JAVA :
      case JAVA_IDEA :
	 menu.add(new CreateConfigAction(BumpLaunchConfigType.JAVA_APP));
	 menu.add(new CreateConfigAction(BumpLaunchConfigType.REMOTE_JAVA));
	 menu.add(new CreateConfigAction(BumpLaunchConfigType.JUNIT_TEST));
	 break;
      case PYTHON :
	 menu.add(new CreateConfigAction(BumpLaunchConfigType.PYTHON));
	 break;
      case JS :
	 menu.add(new CreateConfigAction(BumpLaunchConfigType.JS));
	 break;
      case REBUS :
	 break;
    }
}



private class ConfigSelector extends BoardMouser {

   @Override public void mouseClicked(MouseEvent e) {
      if (e.getButton() == MouseEvent.BUTTON3) {
         JPopupMenu menu = new JPopupMenu();
         BumpClient bc = BumpClient.getBump();
         BumpRunModel bm = bc.getRunModel();
         Collection<BumpLaunchConfig> blcs = new TreeSet<BumpLaunchConfig>(new ConfigComparator());
         for (BumpLaunchConfig blc : bm.getLaunchConfigurations()) {
            if (!blc.isWorkingCopy()) blcs.add(blc);
          }
         for (BumpLaunchConfig blc : blcs) {
            menu.add(new ConfigAction(blc));
          }
         addNewConfigurationActions(menu);
         menu.show((Component) e.getSource(),e.getX(),e.getY());
       }
    }

}	// end of inner class ConfigSelector




private class ConfigAction extends AbstractAction {

   private BumpLaunchConfig for_config;

   ConfigAction(BumpLaunchConfig blc) {
      super(blc.getConfigName());
      for_config = blc;
    }

   @Override public void actionPerformed(ActionEvent e) {
      setCurrentLaunchConfig(for_config);
      BoardMetrics.noteCommand("BDDT","GoToDebug");
      newDebugger(for_config);
    }

}	// end of inner class ConfigAction




private static class CreateConfigAction extends AbstractAction {

   private BumpLaunchConfigType config_type;

   CreateConfigAction(BumpLaunchConfigType typ) {
      super("Create New " + typ.getEclipseName() + " Configuration");
      config_type = typ;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BumpClient bc = BumpClient.getBump();
      BumpRunModel brm = bc.getRunModel();
      BumpLaunchConfig blc = brm.createLaunchConfiguration(null,config_type);
      if (blc != null) {
	 BumpLaunchConfig blc1 = blc.save();
	 if (blc1 != null) blc = blc1;
	 BddtLaunchBubble bb = new BddtLaunchBubble(blc);
	 BudaBubbleArea bba = the_factory.buda_root.getCurrentBubbleArea();
	 Rectangle r = bba.getVisibleRect();
	 int xp = r.x + (r.width/2);
	 int yp = r.y + (r.height/2);
	 Dimension sz = bb.getPreferredSize();
	 xp -= sz.width/2;
	 yp -= sz.height/2;
	 Point ctr = new Point(xp,yp);
	 bba.addBubble(bb,null,ctr,PLACEMENT_NEW|PLACEMENT_USER|PLACEMENT_MOVETO);
       }
    }

}	// end of inner class CreateConfigAction



private static class ConfigComparator implements Comparator<BumpLaunchConfig> {

   @Override public int compare(BumpLaunchConfig l1,BumpLaunchConfig l2) {
      return l1.getConfigName().compareTo(l2.getConfigName());
    }

}	// end of inner class ConfigComparator




/********************************************************************************/
/*										*/
/*	General context commands for debugging					*/
/*										*/
/********************************************************************************/

private static class DebugContextListener implements BaleContextListener {

   @Override public BudaBubble getHoverBubble(BaleContextConfig cfg) {
      return null;
    }

   @Override public void addPopupMenuItems(BaleContextConfig cfg,JPopupMenu m) {
      if (cfg.inAnnotationArea()) {
	 // check if line has code that can be breakpointed
	 m.add(new BreakpointAction(cfg,true));
	 m.add(new BreakpointAction(cfg,false));
       }
    }

   @Override public String getToolTipHtml(BaleContextConfig cfg) {
      return null;
    }

   @Override public void noteEditorAdded(BaleWindow win)	{ }
   @Override public void noteEditorRemoved(BaleWindow win)	{ }

}	// end of inner class EditorContextListener



private static class BreakpointAction extends AbstractAction {

   private BumpBreakMode break_mode;
   private BaleContextConfig bale_context;

   BreakpointAction(BaleContextConfig cfg,boolean brk) {
      super(brk ? "Toggle Breakpoint" : "Toggle Tracepoint");
      break_mode = (brk ? BumpBreakMode.DEFAULT : BumpBreakMode.TRACE);
      bale_context = cfg;
    }

   @Override public void actionPerformed(ActionEvent e) {
      BumpBreakModel mdl = BumpClient.getBump().getBreakModel();
      mdl.toggleBreakpoint(bale_context.getEditor().getContentProject(),
            bale_context.getEditor().getContentFile(),
            bale_context.getLineNumber(),break_mode);
      BoardMetrics.noteCommand("BDDT","ANNOT_" + e.getActionCommand());
    }

}	// end of inner class BreakpointAction



/********************************************************************************/
/*										*/
/*	Detect changes to the debugger working set				*/
/*										*/
/********************************************************************************/

private class DebugBubbleChecker implements BubbleViewCallback {

   @Override public void workingSetRemoved(BudaWorkingSet ws) {
      if (working_sets.contains(ws)) {
	 working_sets.remove(ws);
	 if (active_working_set == ws) active_working_set = ws;
       }
    }

}	// end of inner class DebugBubbleChecker




}	// end of class BddtFactory



/* end of BddtFactory.java */
