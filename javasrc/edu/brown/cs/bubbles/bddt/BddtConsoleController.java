/********************************************************************************/
/*										*/
/*		BddtConsoleController.java					*/
/*										*/
/*	Bubbles Environment dyanmic debugger tool console controller		*/
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

import edu.brown.cs.bubbles.board.BoardAttributes;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpConsoleMode;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpProcess;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpRunModel;

import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Segment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.WeakHashMap;


class BddtConsoleController implements BddtConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<BddtLaunchControl,ConsoleDocument> launch_consoles;
private Map<Object,File> log_files;
private Map<String,ConsoleDocument> process_consoles;
private AttributeSet stdout_attrs;
private AttributeSet stderr_attrs;
private AttributeSet stdin_attrs;
private AttributeSet sysout_attrs;
private LinkedList<ConsoleMessage> message_queue;


enum TextMode { STDOUT, STDERR, STDIN, SYSTEM, EOF };



/********************************************************************************/
/*										*/
/*	Constructor								*/
/*										*/
/********************************************************************************/

BddtConsoleController()
{
   launch_consoles = new HashMap<>();
   log_files = new WeakHashMap<>();
   process_consoles = new HashMap<>();
   message_queue = new LinkedList<>();

   ConsoleThread ct = new ConsoleThread();
   ct.start();

   BumpClient bc = BumpClient.getBump();
   BumpRunModel rm = bc.getRunModel();

   rm.addRunEventHandler(new ConsoleHandler());

   BoardAttributes atts = new BoardAttributes("Bddt");
   stdout_attrs = atts.getAttributes("StdOut");
   stderr_attrs = atts.getAttributes("StdErr");
   stdin_attrs = atts.getAttributes("StdIn");
   sysout_attrs = atts.getAttributes("SysOut");
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void setupConsole(BddtLaunchControl blc)
{
   getDocument(blc);
}


void setLogFile(BddtLaunchControl blc,String fnm)
{
   if (fnm == null) log_files.remove(blc);
   else {
      File f = new File(fnm);
      log_files.put(blc,f);
    }
}



void setLogFile(BumpProcess bp,String fnm)
{
   if (fnm == null) log_files.remove(bp);
   else {
      File f = new File(fnm);
      log_files.put(bp.getId(),f);
    }
}



/********************************************************************************/
/*										*/
/*	Edit methods								*/
/*										*/
/********************************************************************************/

private void queueConsoleMessage(BumpProcess bp,TextMode mode,boolean eof,String msg)
{
   ConsoleMessage last = null;
   synchronized (message_queue) {
      last = message_queue.peekLast();
      if (last != null && last.getProcess() == bp && last.getTextMode() == mode &&
	    last.isEof() == eof && last.getNumLines() < BDDT_CONSOLE_MAX_LINES/2) {
	 last.merge(msg);
	 return;
       }
      int qct = 0;
      for (ConsoleMessage cm : message_queue) qct += cm.getNumLines();
      while (qct > BDDT_CONSOLE_MAX_LINES) {
         ConsoleMessage cm = message_queue.removeFirst();
         qct -= cm.getNumLines();
         if (qct == BDDT_CONSOLE_MAX_LINES) break;
         else if (qct < BDDT_CONSOLE_MAX_LINES) {
            cm.trimMessage(BDDT_CONSOLE_MAX_LINES - qct);
            message_queue.addFirst(cm);
            break;
          }
       }
      message_queue.add(new ConsoleMessage(bp,msg,mode,eof));
      if (last == null) message_queue.notifyAll();
    }
}




private static int countTextLines(String msg)
{
   int lct = 0;
   if (msg != null) {
      for (int idx = msg.indexOf("\n"); idx > 0; idx = msg.indexOf("\n",idx+1)) ++lct;
   }
   return lct;
}




private void processConsoleMessage(ConsoleMessage msg)
{
   BumpProcess bp = msg.getProcess();

   if (msg.isEof()) {
      synchronized (launch_consoles) {
	 if (bp != null)  {
	    addText(bp,TextMode.EOF,"\n [ Process Terminated ]\n");
	    process_consoles.remove(bp.getId());
	  }
       }
    }
   else {
      addText(bp,msg.getTextMode(),msg.getText());
    }
}




private void addText(BumpProcess process,TextMode mode,String message)
{
   String pid = (process == null ? "*" : process.getId());

   ConsoleDocument doc = getDocument(pid,false);

   if (doc != null && message != null) {
      ConsoleAdder ca = new ConsoleAdder(doc,mode,message);
      SwingUtilities.invokeLater(ca);
    }
}


private class ConsoleAdder implements Runnable {

   private ConsoleDocument console_document;
   private TextMode text_mode;
   private String add_message;

   ConsoleAdder(ConsoleDocument doc,TextMode md,String msg) {
      console_document = doc;
      text_mode = md;
      add_message = msg;
    }

   @Override public void run() {
      console_document.addText(text_mode,add_message);
    }

}	// end of inner class ConsoleAdder



/********************************************************************************/
/*										*/
/*	Input methods								*/
/*										*/
/********************************************************************************/

void clearConsole(BumpProcess bp)
{
   String pid = (bp == null ? "*" : bp.getId());
   ConsoleDocument doc = getDocument(pid,false);
   if (doc != null) {
      doc.clear();
      // Consider outputing a header line here
    }
   else {
      BoardLog.logD("BDDT","No console found for process " + pid);
   }
}


void handleInput(Document d,String input)
{
   BumpProcess bp = null;
   BddtLaunchControl blc = null;
   synchronized (launch_consoles) {
      for (Map.Entry<BddtLaunchControl,ConsoleDocument> ent : launch_consoles.entrySet()) {
	 if (ent.getValue() == d) {
	    blc = ent.getKey();
	    bp = blc.getProcess();
	    break;
	  }
       }
    }
   if (bp == null) return;

   BumpClient bc = BumpClient.getBump();
   bc.consoleInput(bp.getLaunch(),input);
   queueConsoleMessage(bp,TextMode.STDIN,false,input);
}




/********************************************************************************/
/*										*/
/*	Creation methods							*/
/*										*/
/********************************************************************************/


private ConsoleDocument getDocument(BddtLaunchControl ctrl)
{
   synchronized (launch_consoles) {
      ConsoleDocument doc = launch_consoles.get(ctrl);
      if (doc == null) {
	 doc = new ConsoleDocument(log_files.get(ctrl));
	 launch_consoles.put(ctrl,doc);
       }
      return doc;
    }
}




private ConsoleDocument getDocument(String pid,boolean force)
{
   synchronized (launch_consoles) {
      ConsoleDocument doc = process_consoles.get(pid);
      if (doc != null) return doc;
      for (Map.Entry<BddtLaunchControl,ConsoleDocument> ent : launch_consoles.entrySet()) {
	 BddtLaunchControl blc = ent.getKey();
	 BumpProcess bp = blc.getProcess();
	 if (bp != null && bp.getId().equals(pid)) {
	    doc = ent.getValue();
	    process_consoles.put(pid,doc);
	    return doc;
	  }
       }
      if (force) {
	 doc = new ConsoleDocument(log_files.get(pid));
	 process_consoles.put(pid,doc);
       }
      return doc;
    }
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BddtConsoleBubble createConsole(BddtLaunchControl blc)
{
   ConsoleDocument doc = getDocument(blc);

   BddtConsoleBubble b = new BddtConsoleBubble(this,doc);

   return b;
}




BddtConsoleBubble createConsole(BumpProcess bp)
{
   ConsoleDocument doc = getDocument(bp.getId(),true);

   BddtConsoleBubble b = new BddtConsoleBubble(this,doc);

   return b;
}




/********************************************************************************/
/*										*/
/*	Model event handling							*/
/*										*/
/********************************************************************************/

private class ConsoleHandler implements BumpConstants.BumpRunEventHandler {

   @Override public void handleConsoleMessage(BumpProcess bp,BumpConsoleMode mode,boolean eof,String msg) {
      TextMode md = TextMode.STDOUT;
      if (mode == BumpConsoleMode.STDERR) md = TextMode.STDERR;
      else if (mode == BumpConsoleMode.SYSTEM) md = TextMode.SYSTEM;
      BoardLog.logD("BDDT","CONSOLE: " + md + " " + msg);
      queueConsoleMessage(bp,md,eof,msg);
   }

}	// end of inner class ConsoleHandler




/********************************************************************************/
/*										*/
/*	ConsoleBuffer implementation						*/
/*										*/
/********************************************************************************/

private class ConsoleDocument extends DefaultStyledDocument {

   private int		line_count;
   private int		max_length;
   private int		line_length;
   private File 	log_file;
   private Writer	log_writer;
   private static final long serialVersionUID = 1;
   
   ConsoleDocument(File logfile) {
      line_count = 0;
      max_length = 0;
      line_length = 0;
      log_file = logfile;
      log_writer = null;
    }

   synchronized void clear() {
      writeLock();
      try {
	 line_count = 0;
	 max_length = 0;
	 line_length = 0;
	 remove(0,getLength());
       }
      catch (BadLocationException e) {
	 BoardLog.logE("BDDT","Problem clearing console",e);
       }
      finally { writeUnlock(); }

      log_writer = null;
      try {
	 if (log_file != null) log_writer = new FileWriter(log_file);
       }
      catch (IOException e) {
	 BoardLog.logE("BDDT","Problem creating log writer: " + e);
       }
    }

   synchronized void addText(TextMode mode,String txt) {
      if (log_writer != null) {
         try {
            log_writer.write(txt);
          }
         catch (IOException e) {
            BoardLog.logE("BDDT","Problem writing log file: " + e);
            log_writer = null;
          }
       }
   
      int lns = countLines(txt);
      writeLock();
      try {
         while (line_count+lns >= BDDT_CONSOLE_MAX_LINES) {
            int lidx = -1;
            try {
               Segment s = new Segment();
               int ln = max_length+2;
               int dln = getLength();
               if (ln > dln) ln = dln;
               getText(0,ln,s);
               
               int delct = 0;
               int idx = -1;
               for (int i = lidx+1; i < s.length(); ++i) {
                  if (s.charAt(i) == '\n') {
                     idx = i;
                     ++delct;
                     if (line_count + lns - delct < BDDT_CONSOLE_MAX_LINES) break;
                   }
                }
               if (idx >= 0) {
                  remove(0,idx+1);
                  line_count -= delct;
                }
               else break;
             }
            catch (BadLocationException e) {
               BoardLog.logE("BDDT","Problem remove line from console",e);
             }
          }
         
         try {
            AttributeSet attrs = null;
            switch (mode) {
               case STDERR :
                  attrs = stderr_attrs;
                  break;
               default :
               case EOF :
               case SYSTEM :
                  attrs = sysout_attrs;
                  break;
               case STDOUT :
                  attrs = stdout_attrs;
                  break;
               case STDIN :
                  attrs = stdin_attrs;
                  break;
             }
            insertString(getLength(),txt,attrs);
            line_count += lns;
            if (txt.length() > max_length) max_length = txt.length();
          }
         catch (BadLocationException e) {
            BoardLog.logE("BDDT","Problem adding line to console",e);
          }
       }
      finally { writeUnlock(); }
      
      if (mode == TextMode.EOF) finish();
    }

   private void finish() {
      if (log_writer != null) {
	 try {
	    log_writer.close();
	  }
	 catch (IOException e) { }
	 log_writer = null;
       }
    }

   private int countLines(String txt) {
      int ct = 0;
      int lidx = 0;
      for (int idx = txt.indexOf("\n"); idx >= 0; idx = txt.indexOf("\n",idx+1)) {
         line_length += idx-lidx;
         if (line_length > max_length) max_length = line_length;
         ++ct;
         line_length = 0;
         lidx = idx;
       }
      line_length = txt.length() - lidx;
      if (line_length > max_length) max_length = line_length;
   
      return ct;
    }

}	// end of inner class ConsoleDocument





private static class ConsoleMessage {

   private BumpProcess for_process;
   private String message_text;
   private TextMode text_mode;
   private int num_lines;
   private boolean is_eof;

   ConsoleMessage(BumpProcess bp,String text,TextMode mode,boolean eof) {
      for_process = bp;
      message_text = text;
      text_mode = mode;
      is_eof = eof;
      num_lines = countTextLines(text);
    }

   TextMode getTextMode()		{ return text_mode; }
   boolean isEof()			{ return is_eof; }
   String getText()			{ return message_text; }
   BumpProcess getProcess()		{ return for_process; }
   int getNumLines()                    { return num_lines; }
   
   void merge(String t) { 
      int nln = countTextLines(t);
      if (nln > BDDT_CONSOLE_MAX_LINES) {
         int idx = t.indexOf("\n");
         message_text = t.substring(idx+1);
         num_lines = nln;
         trimMessage(BDDT_CONSOLE_MAX_LINES);
       }
      else {
         message_text += t;
         num_lines += nln;
         if (num_lines > BDDT_CONSOLE_MAX_LINES) trimMessage(BDDT_CONSOLE_MAX_LINES);
       }
    }
   
   void trimMessage(int ct) {
      int start = 0;
      while (num_lines > ct) {
         start = message_text.indexOf("\n",start)+1;
         --num_lines;
       }
      if (start > 0) message_text = message_text.substring(start);
    }
   
}	// end of inner class ConsoleMessage



/********************************************************************************/
/*										*/
/*	Thread to handle console updates					*/
/*										*/
/********************************************************************************/

private class ConsoleThread extends Thread {

   ConsoleThread() {
      super("BddtConsoleControllerThread");
    }

   @Override public void run() {
      for ( ; ; ) {
         ConsoleMessage msg;
         synchronized (message_queue) {
            while (message_queue.isEmpty()) {
               try {
                  message_queue.wait(10000);
                }
               catch (InterruptedException e) { }
             }
            msg = message_queue.removeFirst();
          }
         processConsoleMessage(msg);
       }
   }

}	// end of inner class ConsoleThread



}	// end of class BddtConsoleController




/* end of BddtConsoleController.java */
