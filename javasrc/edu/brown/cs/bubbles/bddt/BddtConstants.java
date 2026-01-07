/********************************************************************************/
/*										*/
/*		BddtConstants.java						*/
/*										*/
/*	Bubbles Environment dynamic debugger tool constants			*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.bddt;

import edu.brown.cs.bubbles.board.BoardFont;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpRunValue;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpStackFrame;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpThread;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpThreadStack;

import javax.swing.Action;
import javax.swing.tree.TreeNode;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.EventListener;



public interface BddtConstants {



BoardProperties BDDT_PROPERTIES = BoardProperties.getProperties("Bddt");



/********************************************************************************/
/*										*/
/*	Graphic definitions							*/
/*										*/
/********************************************************************************/

/**
 *	Basic color for a debugging channel
 **/

String BDDT_CHANNEL_TOP_COLOR_PROP = "Bddt.ChannelTopColor";

String BDDT_CHANNEL_BOTTOM_COLOR_PROP = "Bddt.ChannelBottomColor";


/**
 *	Color (top) for the debugging panel.
 **/
String BDDT_PANEL_TOP_COLOR_PROP = "Bddt.PanelTopColor";


/**
 *	Color (bottom) for the debugging panel.
 **/
String BDDT_PANEL_BOTTOM_COLOR_PROP = "Bddt.PanelBottomColor";


/**
 *	Colors for the configuration panel
 **/

String BDDT_CONFIG_TOP_COLOR_PROP = "Bddt.ConfigTopColor";
String BDDT_CONFIG_BOTTOM_COLOR_PROP = "Bddt.ConfigBottomColor";

int	BDDT_CONFIG_WIDTH = BDDT_PROPERTIES.getInt("Bddt.config.width",450);
int	BDDT_CONFIG_HEIGHT = BDDT_PROPERTIES.getInt("Bddt.config.height",150);


/**
 * Constants for the process panel
 **/

int   BDDT_PROCESS_WIDTH = BDDT_PROPERTIES.getInt("Bddt.process.width",350);
int   BDDT_PROCESS_HEIGHT = BDDT_PROPERTIES.getInt("Bddt.process.height",100);

String BDDT_PROCESS_TOP_COLOR_PROP = "Bddt.ProcessTopColor";
String BDDT_PROCESS_BOTTOM_COLOR_PROP = "Bddt.ProcessBottomColor";


/**
 * Constants for the threads panel
 */

int   BDDT_THREADS_WIDTH = BDDT_PROPERTIES.getInt("Bddt.threads.width",400);
int   BDDT_THREADS_HEIGHT = BDDT_PROPERTIES.getInt("Bddt.threads.height",200);

String BDDT_THREADS_TOP_COLOR_PROP = "Bddt.ThreadTopColor";
String BDDT_THREADS_BOTTOM_COLOR_PROP = "Bddt.ThreadBottomColor";


/**
 * Constants for the stack/frame/value panesl
 */

int   BDDT_STACK_WIDTH = BDDT_PROPERTIES.getInt("Bddt.stack.width",400);
int   BDDT_STACK_HEIGHT = BDDT_PROPERTIES.getInt("Bddt.stack.height",200);
int   BDDT_STACK_VALUE_HEIGHT = BDDT_PROPERTIES.getInt("Bddt.stack.value.height",50);

String BDDT_STACK_TOP_COLOR_PROP = "Bddt.StackTopColor";
String BDDT_STACK_BOTTOM_COLOR_PROP = "Bddt.StackBottomColor";

String BDDT_STACK_FROZEN_TOP_COLOR_PROP = "Bddt.StackFrozenTopColor";
String BDDT_STACK_FROZEN_BOTTOM_COLOR_PROP = "Bddt.StackFrozenBottomColor";

String BDDT_STACK_EXTINCT_TOP_COLOR_PROP = "Bddt.StackExtinctTopColor";
String BDDT_STACK_EXTINCT_BOTTOM_COLOR_PROP = "Bddt.StackExtinctBottomColor";


/**
 *	Constants for execution (method) bubbles
 **/

String BDDT_LINK_COLOR_PROP = "Bddt.LinkColor";



/**
 *	Constants for history viewer
 **/

int   BDDT_HISTORY_WIDTH = BDDT_PROPERTIES.getInt("Bddt.history.width",300);
int   BDDT_HISTORY_HEIGHT = BDDT_PROPERTIES.getInt("Bddt.history.height",200);


/**
 *	Constants for stop trace viewer
 **/

int   BDDT_STOP_TRACE_WIDTH = BDDT_PROPERTIES.getInt("Bddt.stop.trace.width",300);
int   BDDT_STOP_TRACE_HEIGHT = BDDT_PROPERTIES.getInt("Bddt.stop.trace.height",200);



String BDDT_CONFIG_BUTTON = "Bubble.Debug.Configurations";


String BDDT_PROCESS_BUTTON = "Bubble.Debug.Current Processes";


String BDDT_TOOLBAR_MENU_BUTTON = "DefaultMenu";
String BDDT_TOOLBAR_RUN_BUTTON = "DebugRun";




/********************************************************************************/
/*										*/
/*	Constants for console bubbles						*/
/*										*/
/********************************************************************************/

Font BDDT_CONSOLE_FONT = BoardFont.getFont(Font.MONOSPACED, Font.PLAIN, 11);
String BDDT_CONSOLE_WIDTH_PROP = "Bddt.console.width";
String BDDT_CONSOLE_HEIGHT_PROP = "Bddt.console.height";


int BDDT_CONSOLE_MAX_LINES = 1000;



/********************************************************************************/
/*										*/
/*	Constants for performance bubbles					*/
/*										*/
/********************************************************************************/

Font BDDT_PERF_FONT = BoardFont.getFont(Font.SERIF, Font.PLAIN, 11);
int BDDT_PERF_WIDTH = BDDT_PROPERTIES.getInt("Bddt.perf.width",500);
int BDDT_PERF_HEIGHT = BDDT_PROPERTIES.getInt("Bddt.perf.height",200);

String BDDT_PERF_TOP_COLOR_PROP = "Bddt.PerfTopColor";
String BDDT_PERF_BOTTOM_COLOR_PROP = "Bddt.PerfBottomColor";




/********************************************************************************/
/*										*/
/*	Breakpoint display constants						*/
/*										*/
/********************************************************************************/
//amc6

/**
 * Initial size of the breakpoint bubble
 **/
Dimension BDDT_BREAKPOINT_INITIAL_SIZE = new Dimension(
      BDDT_PROPERTIES.getInt("Bddt.break.width",320),
      BDDT_PROPERTIES.getInt("Bddt.break.height",130));

/**
 * Minimum width of columns
 **/
int[] BDDT_BREAKPOINT_COLUMN_WIDTHS = {40,130,80,45};

/**
 * Color to use in the overview area to display the breakpoint bubble.
 **/
String BDDT_BREAKPOINT_OVERVIEW_COLOR_PROP = "Bddt.BreakpointOverviewColor";

/**
 * Color at the top of the Breakpoint bubble.
 */
String BDDT_BREAKPOINT_TOP_COLOR_PROP = "Bddt.BreakpointTopColor";

/**
 * Color at the bottom of the Breakpoint bubble.
 */
String BDDT_BREAKPOINT_BOTTOM_COLOR_PROP = "Bddt.BreakpointBottomColor";

/**
 * Color displayed when a row is selected in the breakpoint bubble.
 */
String BDDT_BREAKPOINT_SELECTION_COLOR_PROP = "Bddt.BreakpointSelectionColor";

/**
 * Name of the button on the top-level menu for creating breakpoint bubbles
 */
String BDDT_BREAKPOINT_BUTTON = "Bubble.Debug.Breakpoint List";

/**
 * The margins on buttons in the breakpoints bubble
 */

Insets BDDT_BREAKPOINT_BUTTON_MARGIN = new Insets(0,0,0,0);




/********************************************************************************/
/*										*/
/*	Execution annotation constants						*/
/*										*/
/********************************************************************************/

/**
 *	Color for execution annotations
 **/
String BDDT_EXECUTE_ANNOT_COLOR_PROP = "Bddt.ExecutionAnnotation.color";
String BDDT_EXECUTE_EXCEPT_COLOR_PROP = "Bddt.ExecutionAnnotation.exception.color";
String BDDT_FRAME_ANNOT_COLOR_PROP = "Bddt.FrameAnnotation.color";


String BDDT_LAUNCH_OVERVIEW_COLOR_PROP = "Bddt.LaunchOverviewColor";



interface BddtFrameListener extends EventListener {

   void setActiveFrame(BumpStackFrame frm);

}	// end of interface BddtFrameListener



/********************************************************************************/
/*										*/
/*	Evaluation area constants						*/
/*										*/
/********************************************************************************/

/**
 * Initial size of the evaluation bubble
 **/

Dimension BDDT_EVALUATION_INITIAL_SIZE = new Dimension(
      BDDT_PROPERTIES.getInt("Bddt.evaluation.width",320),
      BDDT_PROPERTIES.getInt("Bddt.evaluation.height",130));
String BDDT_PROPERTY_FLOAT_EVALUATION = "Evaluation.float";
String BDDT_EVALUATION_COLOR_PROP = "Bddt.EvaluationColor";
String BDDT_EVALUATION_OUTLINE_PROP = "Bddt.EvaluationOutline";


Dimension BDDT_INTERACTION_INITIAL_SIZE = new Dimension(
      BDDT_PROPERTIES.getInt("Bddt.interaction.width",320),
      BDDT_PROPERTIES.getInt("Bddt.interaction.height",130));
String BDDT_PROPERTY_FLOAT_INTERACTION = "Interaction.float";
String BDDT_INTERACTION_COLOR_PROP = "Bddt.InteractionColor";
String BDDT_INTERACTION_OUTLINE_PROP = "Bddt.InteractionOutline";




/********************************************************************************/
/*										*/
/*	Launch control definitions						*/
/*										*/
/********************************************************************************/

enum LaunchState {
   READY,
   STARTING,
   RUNNING,
   PAUSED,
   PARTIAL_PAUSE,
   TERMINATED
}


int BDDT_LAUNCH_CONTROL_X = BDDT_PROPERTIES.getInt("Bddt.launch.control.x",16);
int BDDT_LAUNCH_CONTROL_Y = BDDT_PROPERTIES.getInt("Bddt.launch.control.y",26);

String BDDT_PROPERTY_FLOAT_LAUNCH_CONTROL = "LaunchControl.float";



String BDDT_PROPERTY_FLOAT_CONSOLE = "Console.float";
String BDDT_PROPERTY_FLOAT_THREADS = "Threads.float";
String BDDT_PROPERTY_FLOAT_HISTORY = "History.float";
String BDDT_PROPERTY_FLOAT_SWING = "Swing.float";
String BDDT_PROPERTY_FLOAT_PERFORMANCE = "Performance.float";




/********************************************************************************/
/*										*/
/*	Value definitions							*/
/*										*/
/********************************************************************************/

enum ValueSetType {
   THREAD,			// thread
   STACK,			// stack for a thread
   FRAME,			// single frame
   VALUE,			// variable value
   CATEGORY			// set of values
}



String	BDDT_PROPERTY_FREEZE_LEVELS = "Freeze.levels";



interface ValueTreeNode extends TreeNode {

   String getKey();
   BumpStackFrame getFrame();
   Object getValue();
   BumpRunValue getRunValue();
   boolean showValueArea();

}	// end of inner interface ValueTreeNode




interface ExpressionValue {

   BumpRunValue getResult();
   String getError();
   boolean isValid();

   String formatResult();

}	// end of inner interface ExpressionValue



/********************************************************************************/
/*										*/
/*	Repository definitions							*/
/*										*/
/********************************************************************************/

/**
 *	Search box name prefix for launch configurations
 **/

String BDDT_LAUNCH_CONFIG_PREFIX = "zzzzzz#@Launch Configurations.";



/**
 *	Search box name prefix for processes
 **/

String BDDT_PROCESS_PREFIX = "zzzzzz#@Processes.";



/********************************************************************************/
/*										*/
/*	History definitions							*/
/*										*/
/********************************************************************************/

interface BddtHistoryData {

   Iterable<BumpThread> getThreads();
   Iterable<BddtHistoryItem> getItems(BumpThread bt);
   void addHistoryListener(BddtHistoryListener bl);
   void removeHistoryListener(BddtHistoryListener bl);

}


interface BddtHistoryItem {

   BumpThread getThread();
   BumpThreadStack getStack();
   BumpRunValue getThisValue();
   String getClassName();
   long getTime();

   boolean isInside(BddtHistoryItem bhi);

}	// end of inner interface BddtHistoryItem



interface BddtHistoryListener extends EventListener {

   void handleHistoryStarted();
   void handleHistoryUpdated();

}



/********************************************************************************/
/*										*/
/*	History Graph parameters						*/
/*										*/
/********************************************************************************/

int	GRAPH_DEFAULT_WIDTH = 400;
int	GRAPH_DEFAULT_HEIGHT = 300;


double	GRAPH_LEFT_RIGHT_MARGIN = 5;
double	GRAPH_OBJECT_WIDTH = 100;
double	GRAPH_OBJECT_HEIGHT = 24;
double	GRAPH_OBJECT_H_SPACE = 10;
double	GRAPH_OBJECT_V_SPACE = 10;
double	GRAPH_TOP_BOTTOM_MARGIN = 5;
double	GRAPH_ACTIVE_WIDTH = 20;
double	GRAPH_TIME_SPACE = GRAPH_DEFAULT_HEIGHT - 2 * GRAPH_TOP_BOTTOM_MARGIN -
				GRAPH_OBJECT_HEIGHT - GRAPH_OBJECT_V_SPACE;


enum LinkType {
   ENTER,
   RETURN,
   NEXT
}



/********************************************************************************/
/*										*/
/*	Bubble types								*/
/*										*/
/********************************************************************************/

enum BubbleType {
   BDDT,		// launch control, frames, etc
   THREADS,		// thread view
   CONSOLE,		// console
   HISTORY,		// debugging history
   SWING,		// swing debugging
   PERF,		// performance view
   EVAL,		// evaluation
   EXEC,		// bubble for current execution point
   FRAME,		// bubble for user selection up the call stack
   VALUES,		// stack frame values bubble
   STOP_TRACE,		// trace of stacks just before debugger stop
   USER,		// bubble created by the user
   INTERACT,		// interaction bubble
   AUX,                 // auxilliary bubbles from separate package
}


interface BddtAuxBubbleAction extends Action {
   
   String getAuxType();
   BudaBubble createBubble();
   Object getLaunchId();              
   BddtAuxBubbleAction clone(Object lid);
   default boolean forBubbleBar()                       { return true; }
   default boolean isFixed()                            { return true; }
   
   default void actionPerformed(ActionEvent evt) {
      BddtFactory.getFactory().addFixedBubble(this); 
    }
}

interface BddtAuxBubble {
   String getAuxType();
}




}	// end of interface BddtConstants



/* end of BddtConstants.java */
