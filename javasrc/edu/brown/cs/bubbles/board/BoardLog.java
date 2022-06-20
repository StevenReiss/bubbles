/********************************************************************************/
/*										*/
/*		BoardLog.java							*/
/*										*/
/*	Bubbles attribute and property management logging methods		*/
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



package edu.brown.cs.bubbles.board;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;



/**
 *	This class is used for logging error and debug information from within
 *	Bubbles.  The information is designed to be used for debugging purposes
 *	and not for normal users.
 **/

public class BoardLog implements BoardConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private LogLevel	log_level;
private File		debug_log;
private File		bedrock_log;
private PrintWriter	debug_writer;
private boolean 	use_stderr;
private boolean 	is_setup;


private static BoardLog the_logger = new BoardLog();



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BoardLog()
{
   log_level = LogLevel.WARNING;
   log_level = LogLevel.DEBUG;
   debug_log = null;
   debug_writer = null;
   bedrock_log = null;
   is_setup = false;
   use_stderr = true;
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

static void setup()
{
   the_logger.setupLogger();
}



private void setupLogger()
{
   if (is_setup) return;
   is_setup = true;

   // at this point the various system parameters are valid
   BoardProperties bp = BoardProperties.getProperties("System");

   String jar = bp.getProperty(BOARD_PROP_JAR_DIR);
   use_stderr = bp.getBoolean(BOARD_PROP_USE_STDERR,(jar == null));

   String logname = "bubbles";
   String lgnm = "bedrock";
   switch (BoardSetup.getSetup().getLanguage()) {
      default :
      case JAVA :
	 logname = "bubbles";
	 lgnm = "bedrock";
	 break;
      case JAVA_IDEA :
         logname = "bibbles";
         lgnm = "bubjet";
         break;
      case PYTHON :
	 logname = "pybles";
	 lgnm = "pybase";
	 break;
      case JS :
	 logname = "nobbles";
	 lgnm = "nobase";
	 break;
      case REBUS :
	 logname = "rebus";
	 lgnm = "rebase";
	 break;
    }

   File wsd = null;
   switch (BoardSetup.getSetup().getRunMode()) {
      case CLIENT :
	 File f1 = BoardSetup.getPropertyBase();
	 File f2 = new File(f1,"logs");
	 f2.mkdirs();
	 wsd = f2;
	 break;
      case NORMAL :
      case SERVER :
	 String wsn = bp.getProperty(BOARD_PROP_WORKSPACE);
	 if (wsn != null) wsd = new File(wsn);
	 if (wsd != null && !wsd.exists()) {
	    f1 = BoardSetup.getPropertyBase();
	    f2 = new File(f1,"logs");
	    f2.mkdirs();
	    wsd = f2;
	  }
	 break;
    }
   if (wsd == null) use_stderr = true;

   if (wsd != null) {
      bedrock_log = new File(wsd,lgnm + "_log.log");
      if (use_stderr) {
	 // doing debugging: use a single file and keep it around
	 debug_log = new File(wsd,logname + "_log.log");
	 File t1 = new File(wsd,lgnm + "_log" + ".save");
	 File t2 = new File(wsd,logname + "_log.save");
	 if (bedrock_log.exists()) bedrock_log.renameTo(t1);
	 if (debug_log.exists()) debug_log.renameTo(t2);
       }
      else {
	 String lognm = logname + "_log.log";
	 debug_log = new File(wsd,lognm);
       }
    }

   debug_writer = null;
   if (debug_log != null) {
      try {
	 FileWriter fw = new FileWriter(debug_log);
	 // if (!use_stderr) debug_log.deleteOnExit();
	 debug_writer = new PrintWriter(fw,true);
       }
      catch (IOException e) {
	 System.err.println("BOARD: Problem creating log file " + debug_log + ": " + e);
	 use_stderr = true;
	 is_setup = false;
	 // problem creating debug output, might want to try a different location
       }
    }

   String lvl = bp.getProperty(BOARD_PROP_LOG_LEVEL);
   if (lvl == null || lvl.length() == 0) log_level = LogLevel.WARNING;
   else {
      switch (Character.toLowerCase(lvl.charAt(0))) {
	 case '0' :
	 case 'e' :
	    log_level = LogLevel.ERROR;
	    break;
	 case '1' :
	 case 'w' :
	    log_level = LogLevel.WARNING;
	    break;
	 case '2' :
	 case 'i' :
	    log_level = LogLevel.INFO;
	    break;
	 case '3' :
	 case 'd' :
	 default :
	    log_level = LogLevel.DEBUG;
	    break;
       }
    }

   log(LogLevel.NONE,"BOARD","Start: " + new Date(),null);
   log(LogLevel.NONE,"BOARD","Version: " + BoardUpdate.getVersionData(),null);
   log(LogLevel.NONE,"BOARD","Logging: " + debug_log,null);
   log(LogLevel.NONE,"BOARD","Bedrock: " + bedrock_log,null);
   log(LogLevel.NONE,"BOARD","Props: " + bp.getProperty(BOARD_PROP_ECLIPSE_FOREGROUND) + " " +
	  bp.getProperty(BOARD_PROP_AUTO_UPDATE) + " @" +
	  BOARD_ARCH + " " + System.getProperty("os.arch"),
	  null);
}




/********************************************************************************/
/*										*/
/*	Logging methods 							*/
/*										*/
/********************************************************************************/

/**
 *	Log an error from the given src package.
 **/

public static void logE(String src,String msg)
{
   the_logger.log(LogLevel.ERROR,src,msg,null);
}

/**
 *	Log an error from the given src package associated with an exception or error.
 **/

public static void logE(String src,String msg,Throwable t)
{
   the_logger.log(LogLevel.ERROR,src,msg,t);

   if (t != null) {
      if (t instanceof OutOfMemoryError) ;
      else BoardMetrics.generateBugReport(src,msg,t);
   }
}



public static void logE(String src,String msg,String id,String trace)
{
   the_logger.log(LogLevel.ERROR,src,msg + "\n" + trace,null);

   if (trace != null) {
      BoardMetrics.generateBugReport(src,msg,id,trace);
   }
}


public static void logX(String src,String msg)
{
   the_logger.log(LogLevel.ERROR,src,msg,null);

   Throwable t = new Throwable(msg);
   t.fillInStackTrace();
   BoardMetrics.generateBugReport(src,msg,t);
}


/**
 *	Log a warning from the given package.
 **/

public static void logW(String src,String msg)
{
   the_logger.log(LogLevel.WARNING,src,msg,null);
}




/**
 *	Log an informational message from the given package
 **/

public static void logI(String src,String msg)
{
   the_logger.log(LogLevel.INFO,src,msg,null);
}




/**
 *	Log debugging information from the given package
 **/

public static void logD(String src,String msg)
{
   the_logger.log(LogLevel.DEBUG,src,msg,null);
}



/**
 *	Log debugging information associated with an error/exception from
 *	the given package
 **/

public static void logD(String src,String msg,Throwable t)
{
   the_logger.log(LogLevel.DEBUG,src,msg,t);
}



/**
 *	Get the current log level
 **/

public static LogLevel getLogLevel()
{
   return the_logger.log_level;
}



/********************************************************************************/
/*										*/
/*	Log access methods							*/
/*										*/
/********************************************************************************/

public static File getBubblesLogFile()
{
   if (the_logger == null) return null;

   return the_logger.debug_log;
}


public static File getBedrockLogFile()
{
   if (the_logger == null) return null;

   return the_logger.bedrock_log;
}





/********************************************************************************/
/*										*/
/*	Debug output methods							*/
/*										*/
/********************************************************************************/

private void log(LogLevel lvl,String src,String msg,Throwable t)
{
   if (lvl.ordinal() > log_level.ordinal()) return;

   if (src == null) src = "BUBBLES";
   String key = lvl.toString().substring(0,1);

   String txt = src + ":" + key + ": " + msg;

   synchronized (this) {
      if (debug_writer != null) debug_writer.println(txt);
      if (use_stderr) System.err.println(txt);

      String pfx = ":EX: ";
      for ( ; t != null; t = t.getCause()) { 
	 txt = src + pfx + t.getMessage();
	 if (debug_writer != null) {
	    debug_writer.println(txt);
	    t.printStackTrace(debug_writer);
	  }
	 if (use_stderr) {
	    System.err.println(txt);
	    t.printStackTrace();
	  }
         pfx = ":EC: ";
       }
    }
}




}	// end of class BoardLog




/* end of BoardLog.java */
