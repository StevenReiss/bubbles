/********************************************************************************/
/*										*/
/*		BumpConstants.java						*/
/*										*/
/*	BUblles Mint Partnership constants					*/
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bump;

import edu.brown.cs.bubbles.bandaid.BandaidConstants;

import edu.brown.cs.ivy.mint.MintConstants;

import org.w3c.dom.Element;

import java.io.File;
import java.util.Collection;
import java.util.EventListener;
import java.util.List;

/**
 *	Definitions for use with the BUMP-ECLIPSE interface
 **/

public interface BumpConstants extends MintConstants {




/********************************************************************************/
/*										*/
/*	Error types								*/
/*										*/
/********************************************************************************/

/**
 *	Error levels returned from the IDE.
 **/

enum BumpErrorType {
   FATAL,
   ERROR,
   WARNING,
   NOTICE
}



/********************************************************************************/
/*										*/
/*	Location types								*/
/*										*/
/********************************************************************************/

/**
 *	Symbol types returned from the IDE.
 **/

enum BumpSymbolType {
   UNKNOWN,
   CLASS,
   INTERFACE,
   ENUM,
   THROWABLE,
   ANNOTATION,
   FIELD,
   ENUM_CONSTANT,
   FUNCTION,
   CONSTRUCTOR,
   STATIC_INITIALIZER,
   MAIN_PROGRAM,
   MODULE,
   PACKAGE,
   PROJECT,
   LOCAL,		// function local variable
   GLOBAL,		// global variable
   EXPORT,		// js: exports.x = y
   IMPORT,		// js: var x = require('x')
   PROGRAM		// js: executable
}



/********************************************************************************/
/*										*/
/*	Quick Fix types 							*/
/*										*/
/********************************************************************************/

/**
 *	Quick fix types returned from the IDE
 **/

enum BumpFixType {
   NONE,
   NEW_METHOD,
   EDIT_FIX,
}



/********************************************************************************/
/*										*/
/*	Callbacks for files							*/
/*										*/
/********************************************************************************/

/**
 *	A callback that can be registered with BUMP to handle IDE-generated
 *	messages related to remote edits, updated ASTs, etc.
 **/

interface BumpFileHandler extends EventListener {

/**
 *	This routine is invoked with an updated elision information which is
 *	a reflection of the AST in Eclipse.
 **/
   void handleElisionData(File file,int id,Element data);


/**
 *	This routine is invoked when a fie that is open by this version of bubbles
 *	is edited either in the IDE or in another running copy of bubbles.
 **/
   void handleRemoteEdit(File file,int len,int off,String txt);

}	// end of interface BumpFileHandler



/********************************************************************************/
/*										*/
/*	Problem Handling classes						*/
/*										*/
/********************************************************************************/

/**
 *	Interface representing an error/warning/todo item/etc. from the IDE
 **/

interface BumpProblem {

/**
 *	Return a unique ID for this problem.
 **/
   String getProblemId();

/**
 *	Return the message associated with the problem.
 **/
   String getMessage();

/**
 *	Return the file if any associated with the problem.
 **/
   File getFile();

/**
 *	Return the line number (0 if there is none) associated with the problem.
 **/
   int getLine();

/**
 *	Return the start offset as per the IDE for the problem.
 **/
   int getStart();

/**
 *	Return the end offset as per the IDE for the problem
 **/
   int getEnd();

/**
 *	Return the type of error (ERROR/WARNING/...)
 **/
   BumpErrorType getErrorType();


/**
 *	Return the edit id that caused this error, 0 if none
 **/
   int getEditId();

/**
 *	Get a list of the fixes suggested by the IDE for this error.  This will
 *	return null if there are no fixes.
 **/
   List<BumpFix> getFixes();

/**
 *	Return project for this problem
 **/
   String getProject();

}	// end of inner interface BumpProblem




/**
 *	Interface representing a quick fix for an error
 **/

interface BumpFix {

/**
 *	Return the type of quick fix.
 **/
   BumpFixType getType();

/**
 *	Return parameters describing the fix.  These are dependent on the fix.
 **/
   String getParameter(String id);

/**
 *	Return the edits associated with the fix.
 **/
   Element getEdits();

/**
 *	Return the relevance of this fix
 **/
   int getRelevance();

}




/**
 *	Interface for callbacks for someone interested in getting problem notifications.
 **/

interface BumpProblemHandler extends EventListener {

   void handleProblemAdded(BumpProblem bp);
   void handleProblemRemoved(BumpProblem bp);
   void handleProblemsDone();
   void handleClearProblems();

}	// end of inner interface BumpProblemHandler



/**
 *	The set of all valid parameters for quick fixes.
 **/

String [] FIX_PARAMETERS = new String [] {
   "NAME", "CLASS", "PARAMS", "RETURN"
};


interface BumpContractType {

   boolean useContractsForJava();
   boolean useJunit();
   boolean enableAssertions();
   boolean useTypeAnnotations();

}



/********************************************************************************/
/*										*/
/*	Breakpoint handling classes						*/
/*										*/
/********************************************************************************/

/**
 *	Interface for a breakpoint returned from the IDE.
 **/

interface BumpBreakpoint {

/**
 *	Unique ID representing this breakpoint.
 **/
   String getBreakId();

/**
 *	File if any associated with the breakpoint.
 **/
   File getFile();

/**
 *	Line number associated with the breakpoint; 0 indicates none.
 **/
   int getLineNumber();

/**
 *	Get a property associated with the breakpoint.	Different types of breakpoints
 *	have different associated properties.  The breakpoint type can be obtained using
 *	the property "TYPE".
 **/
   String getProperty(String id);

/**
 *	Get a boolean property associated with the breakpoint, false if not defined.
 **/
   boolean getBoolProperty(String id);

/**
 *	Get an integer property associated with the breakpoint, 0 if not defined.
 **/
   int getIntProperty(String id);

   String getDescription();

}	// end of inner interface BumpBreakpoint




/**
 *	Callback class for those interested in learing about additions/deletions or
 *	changes to breakpoints.
 **/

interface BumpBreakpointHandler extends EventListener {

   void handleBreakpointAdded(BumpBreakpoint bp);
   void handleBreakpointRemoved(BumpBreakpoint bp);
   void handleBreakpointChanged(BumpBreakpoint bp);

}	// end of inner interface BumpBreakpointHandler




/**
 *	Specify the mode of a breakpoint.  A breakpoint can either stop all the
 *	threads (SUSPEND_VM), or it can just suspend the thread in which it occurs
 *	(SUSPEND_THREAD).  If DEFAULT is specified, it will defer to the default
 *	setting in BbptFactory.
 **/

enum BumpBreakMode {
   DEFAULT,
   SUSPEND_THREAD,
   SUSPEND_VM,
   TRACE;

   boolean isSuspendVm()	{ return this == SUSPEND_VM; }
   boolean isSuspendThread()	{ return this != SUSPEND_VM; }
   boolean isTrace()		{ return this == TRACE; }
}


/**
 *	Determines how exception breakpoints are interpreted.  CAUGHT implies that
 *	a break occurs when the exception is caught; UNCAUGHT only causes a breakpoint
 *	when the exception is not caught; ALL breaks in either case.  DEFAULT defers
 *	to the current default setting in BbptFactory.
 **/

enum BumpExceptionMode {
   DEFAULT,
   CAUGHT,
   UNCAUGHT,
   ALL;

   boolean isCaught()		{ return this == CAUGHT || this == ALL; }
   boolean isUncaught() 	{ return this == UNCAUGHT || this == ALL; }
}






interface BumpBreakModel {

   boolean addLineBreakpoint(String proj,File file,String cls,int line,BumpBreakMode mode);
   void clearLineBreakpoint(String proj,File file,String cls,int line);
   void toggleBreakpoint(String proj,File file,int line,BumpBreakMode mode);

   void addExceptionBreakpoint(String proj,String cls,BumpExceptionMode enmod,BumpBreakMode mode,
	boolean subclasses);

   void enableBreakpoint(File file,int line);
   void disableBreakpoint(File file,int line);
   void enableAllBreakpoints(File file);
   void disableAllBreakpoints(File file);

   BumpBreakpoint findBreakpoint(File file,int line);
   BumpBreakpoint findBreakpoint(Element xml);
   boolean removeBreakpoint(String id);

   void setDefaultBreakMode(BumpBreakMode mode);
   void setDefaultExceptionMode(BumpExceptionMode mode);

}	// end of inner interface BumpBreakModel




/********************************************************************************/
/*										*/
/*	Thread states								*/
/*										*/
/********************************************************************************/

enum BumpThreadState {
   NONE,
   NEW,
   RUNNING,
   RUNNING_SYNC,
   RUNNING_IO,
   RUNNING_SYSTEM,
   BLOCKED,
   DEADLOCKED,
   WAITING,
   TIMED_WAITING,
   IDLE,
   STOPPED,
   STOPPED_SYNC,
   STOPPED_IO,
   STOPPED_WAITING,
   STOPPED_IDLE,
   STOPPED_TIMED,
   STOPPED_SYSTEM,
   STOPPED_BLOCKED,
   STOPPED_DEADLOCK,
   EXCEPTION,
   UNKNOWN,
   DEAD;

   public BumpThreadState getStopState() {
      switch (this) {
         case DEAD :
         case EXCEPTION :
         default :
            return this;
         case NONE :
         case NEW :
         case RUNNING :
            return STOPPED;
         case RUNNING_SYNC :
            return STOPPED_SYNC;
         case RUNNING_IO :
            return STOPPED_IO;
         case RUNNING_SYSTEM :
            return STOPPED_SYSTEM;
         case BLOCKED :
            return STOPPED_BLOCKED;
         case DEADLOCKED :
            return STOPPED_DEADLOCK;
         case WAITING :
            return STOPPED_WAITING;
         case TIMED_WAITING :
            return STOPPED_TIMED;
         case IDLE :
            return STOPPED_IDLE;
       }
    }

   public BumpThreadState getExceptionState() {
      switch (this) {
	 case DEAD :
	    return DEAD;
	 default :
	    return EXCEPTION;
       }
    }

   public BumpThreadState getRunState() {
      switch (this) {
	 case DEAD :
	 default :
	    return this;
	 case NONE :
	 case NEW :
	 case STOPPED :
	 case EXCEPTION :
	    return RUNNING;
	 case STOPPED_SYNC :
	    return RUNNING_SYNC;
	 case STOPPED_IO :
	    return RUNNING_IO;
	 case STOPPED_SYSTEM :
	    return RUNNING_SYSTEM;
	 case STOPPED_BLOCKED :
	    return BLOCKED;
	 case STOPPED_WAITING :
	    return WAITING;
	 case STOPPED_TIMED :
	    return TIMED_WAITING;
	 case STOPPED_DEADLOCK :
	    return DEADLOCKED;
	 case STOPPED_IDLE :
	    return IDLE;
       }
    }

   public boolean isRunning() {
      switch (this) {
	 case NEW :
	 case RUNNING :
	 case RUNNING_SYNC :
	 case RUNNING_IO :
	 case RUNNING_SYSTEM :
	 case WAITING :
	 case TIMED_WAITING :
	 case IDLE :
	 case BLOCKED :
	 case DEADLOCKED :
	    return true;
	 default :
	    break;
       }
      return false;
    }

   public boolean isStopped() {
      switch (this) {
	 case STOPPED :
	 case STOPPED_SYNC :
	 case STOPPED_IO :
	 case STOPPED_WAITING :
	 case STOPPED_SYSTEM :
	 case STOPPED_BLOCKED :
	 case STOPPED_TIMED :
	 case STOPPED_IDLE :
	 case EXCEPTION :
	    return true;
	 default :
	    break;
       }
      return false;
    }

   public boolean isException() {
      switch (this) {
         case EXCEPTION :
            return true;
         default:
            break;
       }
      return false;
    }
}




/********************************************************************************/
/*										*/
/*	Run event handling							*/
/*										*/
/********************************************************************************/

interface BumpRunModel {

   Iterable<BumpLaunchConfig> getLaunchConfigurations();
   BumpLaunchConfig getLaunchConfiguration(String id);
   BumpLaunchConfig createLaunchConfiguration(String name,BumpLaunchType typ);
   Iterable<BumpProcess> getProcesses();
   List<BumpLaunchType> getLaunchTypes();
   void addRunEventHandler(BumpRunEventHandler reh);
   void removeRunEventHandler(BumpRunEventHandler reh);

}	// end of inner interface BumpRunModel



enum BumpThreadStateDetail {
   NONE,
   BREAKPOINT,
   CLIENT_REQUEST,
   EVALUATION,
   EVALUATION_IMPLICIT,
   STEP_END,
   STEP_INTO,
   STEP_OVER,
   STEP_RETURN,
   CONTENT
}


enum BumpThreadType {
   UNKNOWN,
   SYSTEM,
   JAVA,
   UI,
   USER,
}


interface BumpRunEvent {

   BumpRunEventType getEventType();
   BumpLaunchConfig getLaunchConfiguration();
   BumpLaunch getLaunch();
   BumpProcess getProcess();
   BumpThread getThread();
   long getWhen();
   Object getEventData();

}	// end of inner innerface BumpRunEvent




enum BumpRunEventType {
   LAUNCH_ADD,
   LAUNCH_REMOVE,
   LAUNCH_CHANGE,
   PROCESS_ADD,
   PROCESS_REMOVE,
   PROCESS_CHANGE,
   PROCESS_PERFORMANCE,
   PROCESS_TRIE,
   PROCESS_SWING,
   PROCESS_TRACE,
   THREAD_ADD,
   THREAD_REMOVE,
   THREAD_CHANGE,
   THREAD_TRACE,
   THREAD_HISTORY,
   HOTCODE_SUCCESS,
   HOTCODE_FAILURE,
}



enum BumpValueKind {
   UNKNOWN,
   PRIMITIVE,
   STRING,
   CLASS,
   OBJECT,
   ARRAY,
   SCOPE
}




interface BumpLaunchType {
   String getName();
   String getDescription();
   List<BumpLaunchConfigField> getFields();
   boolean useDebugArgs();
   boolean isTestCase();
}


enum BumpLaunchConfigFieldType {
   UNKNOWN,
   BOOLEAN,
   STRING,
   INTEGER,
   CHOICE,
   PRESET,
}

interface BumpLaunchConfigField {

   String getFieldName();
   String getDescription();
   BumpLaunchConfigFieldType getType();
   String getEvaluate();
   String getArgField();
   int getNumRows();
   int getMin();
   int getMax();
   String getDefaultValue();

}	// end of interface BumpLaunchConfigField


enum BumpConsoleMode {
   STDOUT, STDERR, SYSTEM
}

interface BumpRunEventHandler extends EventListener {

   default void handleLaunchEvent(BumpRunEvent evt)			{ }
   default void handleProcessEvent(BumpRunEvent evt)			{ }
   default void handleThreadEvent(BumpRunEvent evt)			{ }


   default void handleConsoleMessage(BumpProcess proc,
	 BumpConsoleMode mode,boolean iseof,String msg) 		{ }

}	// end of inner interface BumpRunEventHandler


/**
 *	User-setup runnable
 **/

interface BumpLaunchConfig {

   String getProject();
   String getMainClass();
   String getArguments();
   String getVMArguments();
   boolean getStopInMain();
   String getConfigName();
   String getId();
   BumpLaunchType getLaunchType();
   String getTestName();
   String getRemoteHost();
   int getRemotePort();
   boolean isWorkingCopy();
   String getContractArgs();
   String getLogFile();
   String getWorkingDirectory();

   String getAttribute(String name);
   boolean getBoolAttribute(String name);

   BumpLaunchConfig clone(String name);
   BumpLaunchConfig save();
   void delete();

   BumpLaunchConfig setConfigName(String name);
   BumpLaunchConfig setProject(String pnm);
   BumpLaunchConfig setMainClass(String cnm);
   BumpLaunchConfig setArguments(String args);
   BumpLaunchConfig setVMArguments(String args);
   BumpLaunchConfig setStopInMain(boolean fg);

   BumpLaunchConfig setTestName(String name);

   BumpLaunchConfig setRemoteHostPort(String host,int port);
   BumpLaunchConfig setRemoteHost(String host);
   BumpLaunchConfig setRemotePort(int port);

   BumpLaunchConfig setLogFile(String name);
   BumpLaunchConfig setWorkingDirectory(String name);

   BumpLaunchConfig setAttribute(String name,String value);

}	// end of inner interface BumpLaunch


/**
 *	Active launch
 **/

interface BumpLaunch {
   BumpLaunchConfig getConfiguration();
   boolean isDebug();
   String getId();
}



/**
 *	Active process
 **/

interface BumpProcess {
   BumpLaunch getLaunch();
   Iterable<BumpThread> getThreads();
   String getId();
   String getName();
   boolean isRunning();
   void requestSwingData(int x,int y);
   boolean isDummy();
}	// end of inner interface BumpProcess


/**
 *	Thread in an active process
 **/

interface BumpThread {
   String getName();
   String getGroupName();
   BumpThreadType getThreadType();
   boolean isDaemonThread();
   BumpThreadState getThreadState();
   BumpThreadStateDetail getThreadDetails();
   BumpLaunch getLaunch();
   BumpProcess getProcess();
   String getId();
   long getCpuTime();
   long getUserTime();
   long getBlockTime();
   long getWaitTime();
   int getBlockCount();
   int getWaitCount();
   String getExceptionType();

   BumpThreadStack getStack();
   BumpBreakpoint getBreakpoint();

   void requestHistory();

}	// end of inner interface BumpThread



/**
 *	Representation of the stack
 **/

interface BumpThreadStack {

   int getNumFrames();
   BumpThread getThread();
   BumpStackFrame getFrame(int idx);

}	// end of inner interface BumpThreadStack



interface BumpStackFrame {

   BumpThread getThread();
   String getFrameClass();
   String getMethod();
   String getSignature();
   String getRawSignature();
   File getFile();
   int getLineNumber();
   String getId();
   int getLevel();
   boolean isStatic();
   boolean isSystem();
   boolean isSynthetic();

   Collection<String> getVariables();
   BumpRunValue getValue(String var);

   boolean evaluate(String expr,BumpEvaluationHandler hdlr);
   boolean evaluateInternal(String expr,String saveid,BumpEvaluationHandler hdlr);

   boolean match(BumpStackFrame frm);
   boolean sameFrame(BumpStackFrame frm);
   String getDisplayString();

}	// end of inner interface BumpStackFrame



interface BumpRunValue {

   BumpValueKind getKind();
   String getName();
   String getType();
   String getValue();
   String getDeclaredType();
   String getActualType();
   boolean hasContents();
   boolean isLocal();
   boolean isStatic();
   int getLength();

   Collection<String> getVariables();
   BumpRunValue getValue(String var);
   String getDetail();

   BumpStackFrame getFrame();
   BumpThread getThread();

}	// end of inner interface BumpRunValue




interface BumpThreadFilter extends EventListener {

   BumpRunEvent handleThreadEvent(BumpThread bt,BumpRunEvent evt);

}

String	BUMP_BANDAID_TRAILER = BandaidConstants.BANDAID_TRAILER;




/********************************************************************************/
/*										*/
/*	Completion handling classes						*/
/*										*/
/********************************************************************************/

/**
 *	The types of completions that may be reported by the IDE.
 **/

enum CompletionType {
   OTHER,
   ANNOTATION_ATTRIBUTE_REF,
   ANONYMOUS_CLASS_DECLARATION,
   FIELD_IMPORT,
   FIELD_REF,
   FIELD_REF_WITH_CASTED_RECEIVER,
   JAVADOC_BLOCK_TAG,
   JAVADOC_FIELD_REF,
   JAVADOC_INLINE_TAG,
   JAVADOC_METHOD_REF,
   JAVADOC_PARAM_REF,
   JAVADOC_TYPE_REF,
   JAVADOC_VALUE_REF,
   KEYWORD,
   LABEL_REF,
   LOCAL_VARIABLE_REF,
   METHOD_DECLARATION,
   METHOD_IMPORT,
   METHOD_NAME_REFERENCE,
   METHOD_REF,
   METHOD_REF_WITH_CASTED_RECEIVER,
   PACKAGE_REF,
   POTENTIAL_METHOD_DECLARATION,
   TYPE_IMPORT,
   TYPE_REF,
   VARIABLE_DECLARATION
}




/**
 *	Information representing a completion from the IDE.
 **/

interface BumpCompletion {

   boolean isPublic();
   boolean isPrivate();
   boolean isAbstract();
   boolean isFinal();
   boolean isNative();
   boolean isProtected();
   boolean isStatic();
   boolean isSynchronized();
   boolean isStrict();
   boolean isTransient();
   boolean isVolatile();

   CompletionType getType();

   String getSignature();
   String getDeclaringType();

   String getCompletion();
   String getName();

   int getReplaceStart();
   int getReplaceEnd();

   int getRelevance();

   // int getRelevance();

}	// end of inner interface BumpCompletion



/********************************************************************************/
/*										*/
/*	Callbacks for file change notification					*/
/*										*/
/********************************************************************************/

interface BumpChangeHandler extends EventListener {

   default void handleFileStarted(String proj,String file)              { }
   default void handleFileChanged(String project,String file)           { }
   default void handleFileAdded(String project,String file)             { }
   default void handleFileRemoved(String project,String file)           { }
   default void handleProjectOpened(String project)                     { }

}



/********************************************************************************/
/*										*/
/*	Callbacks for editor requests						*/
/*										*/
/********************************************************************************/

interface BumpOpenEditorBubbleHandler extends EventListener {
   static final int DELAY_TIME = 300;

   void handleOpenEditorBubble(String projname, String resourcepath, String type);

}	// end of inner class BumpOpenEditorBubbleHandler



interface BumpEvaluationHandler extends EventListener {

   void evaluationResult(String eid,String expr,BumpRunValue val);
   void evaluationError(String eid,String expr,String error);

}	// end of inner class BumpEvaluationHandler



/********************************************************************************/
/*										*/
/*	Callbacks for progress requests 					*/
/*										*/
/********************************************************************************/

interface BumpProgressHandler extends EventListener {

   void handleProgress(long serialno,String id,String kind,String task,String subtask,double work);

}	// end of inner class BumpProgressHandler



/********************************************************************************/
/*										*/
/*	Trie Performance Data							*/
/*										*/
/********************************************************************************/

// index into TrieNode performance arrays
int BUMP_TRIE_OP_RUN = 0;			// count while running
int BUMP_TRIE_OP_IO = 1;			// count doing I/O
int BUMP_TRIE_OP_WAIT = 2;			// count doing wait

int BUMP_TRIE_OP_COUNT = 3;


interface BumpTrieData {
   BumpTrieNode getRoot();
   double getBaseSamples();
   double getTotalSamples();
   double getBaseTime();
}


interface BumpTrieNode {

   BumpTrieNode getParent();
   Collection<BumpTrieNode> getChildren();
   int [] getCounts();
   Collection<BumpThread> getThreads();
   int [] getThreadCounts(BumpThread th);

   String getClassName();
   String getMethodName();
   int getLineNumber();
   String getFileName();

   int [] getTotals();
   void computeTotals();

}



}	// end of interface BumpConstants




/* end of BumpConstants.java */
