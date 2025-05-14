/********************************************************************************/
/*										*/
/*		BoardMetricAnalyzer.java					*/
/*										*/
/*	Stand alone program to do analysis of recorded metrics			*/
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

import edu.brown.cs.ivy.exec.IvyExec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


public final class BoardMetricAnalyzer implements BoardConstants {


/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   BoardMetricAnalyzer bma = new BoardMetricAnalyzer(args);

   bma.process();
}





/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

/***
*
*	We want to issue a find command on conifer2 to find and dump the appropriate
*	log files.  Then we only need to read standard out to get all the relevant
*	entries.
*
*	Let the user use find arguments such as -mtime <# days> to specify which
*	files are relevant.  In addition, we should add find arguments to select
*	just command files or files for a particular user.  These should  be gathered
*	from the command line and passed onto find.
*
*	find /vol/web/html/bubbles/uploads -type f -name '*COMMANDS*' -mtime 24 -print -exec cat {} \;
*
****/

private static final String BASE_DIR = "/vol/web/html/bubbles/uploads";
private static final String SSHCMD = "ssh conifer2";
private static final String CMD = "find " + BASE_DIR + " -type f";
// private final static String LISTARGS = "-print -exec cat {} \\;";
private static final String CMD_MATCH = "-name '*COMMAND*'";
private static final String CFG_MATCH = "-name '*CONFIG*'";
private static final String FILEARGS = "-print";


private List<String>	find_args;
private boolean 	use_commands;
private boolean 	use_config;
private List<Analyzer>	analyzer_set;
private String		data_file;
private String		output_data;
private String		output_file;
private PrintStream	output_writer;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BoardMetricAnalyzer(String [] args)
{
   find_args = new ArrayList<String>();
   use_commands = true;
   use_config = false;
   data_file = null;
   output_data = null;
   output_file = null;

   scanArgs(args);
}




/********************************************************************************/
/*										*/
/*	Argument scanning							*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   analyzer_set = new ArrayList<>();

   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("--anal") && i+1 < args.length) {
	 switch (args[++i]) {
	    case "AUTOFIX" :
	       use_commands = true;
	       use_config = false;
	       analyzer_set.add(new EditRegionAnalysis());
	       analyzer_set.add(new UserCorrectionAnalysis());
	       analyzer_set.add(new AutoCorrectionAnalysis());
               analyzer_set.add(new TotalTimeAnalysis());
	       break;
            case "SEEDE" :
               use_commands = true;
               analyzer_set.add(new SeedeAnalysis());
               analyzer_set.add(new TotalTimeAnalysis());
               break;
            case "STATS" :
               use_commands = true;
               use_config = false;
               analyzer_set.add(new CommandUsageAnalysis());
               break;
	    default :
	       badArgs();
	       break;
	  }
       }
      else if (args[i].startsWith("--data") && i+1 < args.length) {
	 data_file = args[++i];
       }
      else if (args[i].startsWith("--out") && i+1 < args.length) {
	 output_file = args[++i];
       }
      else if (args[i].startsWith("--save") && i+1 < args.length) {
	 output_data = args[++i];
       }
      else {
	 find_args.add(args[i]);
       }
    }
}



private void badArgs()
{
   System.err.println("BoardMetrixAnalyzer --analysis {AUTOFIX|SEEDE|STATS} <find options>");
   System.exit(1);
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void process()
{
   String pfx = BASE_DIR + "/";

   String session = null;
   long lastactive = 0;

   try {
      if (output_file == null) output_writer = System.out;
      else output_writer = new PrintStream(output_file);
    }
   catch (IOException e) {
      System.err.println("Can't open output file " + output_file);
      System.exit(1);
    }

   try {
      BufferedReader ins = getReader();
      for ( ; ; ) {
         try {
            String ln = ins.readLine();
            if (ln == null) break;
            if (ln.length() == 0) continue;
            char ch0 = ln.charAt(0);
            if (Character.isDigit(ch0) ||
                  Character.isWhitespace(ch0) ||
                  ch0 == ',') {
               continue;
             }
            if (ln.startsWith(pfx)) {
               String id = ln.substring(pfx.length());
               int idx = id.indexOf("/");
               int idx1 = id.indexOf("/",idx+1);
               String sess = id.substring(0,idx1);
               if (session != null && sess.equals(session)) continue;
               for (Analyzer anal : analyzer_set) {
                  anal.startSession(sess,id);
                }
//             output_writer.println("ENDSESSION");
               lastactive = 0;
               session = sess;
             }
            else {
               String [] data = ln.split(",");
               String timestr = data[data.length-1];
               if (timestr == null || timestr.isEmpty()) continue;
               if (!Character.isDigit(timestr.charAt(0))) continue;
               long time = Long.parseLong(data[data.length-1]);
               String [] args;
               if (data.length == 2) {
                  args = new String [0];
                }
               else {
                  String s = data[1];
                  for (int i = 2; i < data.length-1; ++i) {
                     s = s + "_" + data[i];
                   }
                  args = s.split("_");
                }
               if (data[0].equals("ACTIVE")) {
                  if (args[0].equals("inactive.start")) lastactive = time;
                  else if (args[0].equals("inactive.end") && lastactive > 0) {
                     long delta = time - lastactive;
                     lastactive = 0;
                     for (Analyzer anal : analyzer_set) {
                        anal.inactive(delta,time);
                      }
                   }
                }
               else {
                  for (Analyzer anal : analyzer_set) {
                     anal.processLine(data[0],args,time);
                   }
                }
             }
          }
         catch (Throwable t) {
            System.err.println("Problem processing file " + t);
            t.printStackTrace();
            continue;
          }
       }
      for (Analyzer anal : analyzer_set) {
	 anal.finish();
       }
    }
   catch (IOException ex) {
    }
}


private BufferedReader getReader() throws IOException
{
   if (data_file != null) {
      return new BufferedReader(new FileReader(data_file));
    }

   StringBuffer cmd = new StringBuffer();
   cmd.append(CMD);
   if (use_commands) cmd.append(" " + CMD_MATCH);
   else if (use_config) cmd.append(" " + CFG_MATCH);
   for (String s : find_args) {
      cmd.append(" " + s);
    }
   cmd.append(" " + FILEARGS);
   String cmdstr = SSHCMD + " \"" + cmd + "\"";

   System.err.println("METRIC ANALYZER: Command: " + cmdstr);

   IvyExec ex = new IvyExec(cmdstr,IvyExec.ERROR_OUTPUT|IvyExec.READ_OUTPUT);

   List<String> allfiles = new ArrayList<String>();
   BufferedReader br = new BufferedReader(new InputStreamReader(ex.getInputStream()));
   for ( ; ; ) {
      String fn = br.readLine();
      if (fn == null) break;
      if (fn.length() == 0) continue;
      char ch0 = fn.charAt(0);
      if (Character.isDigit(ch0)) continue;
      allfiles.add(fn);
    }
   br.close();
   Collections.sort(allfiles,new FileSorter());

   File outf = null;
   if (output_data != null) outf = new File(output_data);
   else {
      outf = File.createTempFile("metricdata","in");
      outf.deleteOnExit();
    }
   PrintWriter pw = new PrintWriter(outf);
   for (String s : allfiles) {
      pw.println(s);
      String catcmd = "cat " + s;
      String catstr = SSHCMD + " \"" + catcmd + "\"";
      System.err.println("METRIC ANALYZER: Command: " + catstr);
      IvyExec catex = new IvyExec(catstr,IvyExec.READ_OUTPUT);
      br = new BufferedReader(new InputStreamReader(catex.getInputStream()));
      for ( ; ; ) {
	 String ln = br.readLine();
	 if (ln == null) break;
	 pw.println(ln);
       }
      br.close();
    }
   pw.close();

   return new BufferedReader(new FileReader(outf));
}




private static final class FileSorter implements Comparator<String> {

   @Override public int compare(String f1,String f2) {
      int idx1 = f1.lastIndexOf("/");
      int idx2 = f2.lastIndexOf("/");
      String f1h = f1;
      if (idx1 > 0) {
         f1h = f1.substring(0,idx1);
       }
      String f2h = f2;
      if (idx2 > 0) {
         f2h = f2.substring(0,idx2);
       }
      int cmp = f1h.compareTo(f2h);
      if (cmp != 0) return cmp;
      String [] f1arg = f1.substring(idx1+1).split("_");
      String [] f2arg = f2.substring(idx2+1).split("_");
      boolean fndcnt = false;
      for (int i = 0; i < f1arg.length; ++i) {
         if (fndcnt || f1arg[i].isEmpty() || !Character.isDigit(f1arg[i].charAt(0))) {
            cmp = f1arg[i].compareTo(f2arg[i]);
            if (cmp != 0) return cmp;
          }
         else {
            try {
               int v1 = Integer.parseInt(f1arg[i]);
               int v2 = Integer.parseInt(f2arg[i]);
               if (v1 < v2) return -1;
               else if (v1 > v2) return 1;
               fndcnt = true;
             }
            catch (NumberFormatException e) {
               cmp = f1arg[i].compareTo(f2arg[i]);
               if (cmp != 0) return cmp;
             }
          }
       }
      return 0;
    }
}




/********************************************************************************/
/*										*/
/*	Basic Analyzer								*/
/*										*/
/********************************************************************************/

private static class Analyzer {

   public void startSession(String sess,String id)		{ }

   public void processLine(String src,String [] cmd,long time)	{ }

   public void inactive(long delta,long endtime)		{ }

   public void finish() 					{ }

}	// end of inner class Analyzer




/********************************************************************************/
/*										*/
/*	Total Time Analyzer							*/
/*										*/
/********************************************************************************/

private class TotalTimeAnalysis extends Analyzer {

   private String cur_session;
   private long start_time;
   private long inactive_time;
   private long last_time;

   TotalTimeAnalysis() {
      cur_session = null;
      start_time = 0;
      inactive_time = 0;
      last_time = 0;
    }

   @Override public void startSession(String sess,String id) {
      finishSession();
      cur_session = sess;
      start_time = 0;
      inactive_time = 0;
      last_time = 0;
    }

   @Override public void processLine(String src,String [] cmd,long time) {
      if (start_time == 0) start_time = time;
      last_time = time;
    }

   @Override public void inactive(long delta,long time) {
      inactive_time += delta;
      last_time = time;
    }

   @Override public void finish() {
      finishSession();
    }

   private void finishSession() {
      if (cur_session == null) return;
      long time = last_time - start_time - inactive_time;
      if (time < 0) {
	 System.err.println("NEGATIVE SESSION TIME");
       }
      output_writer.println("SESSION," + cur_session + "," + time);
    }

}	// end of inner class TotalTimeAnalysis




/********************************************************************************/
/*										*/
/*	Edit Region Analysis							*/
/*										*/
/********************************************************************************/

private class EditRegionAnalysis extends Analyzer {

   private Map<String,EditRegion> active_regions;
   private EditRegion		  current_region;
   private Map<EditRegion,Long>   last_active;

   EditRegionAnalysis() {
      active_regions = new HashMap<String,EditRegion>();
      current_region = null;
      last_active = new HashMap<EditRegion,Long>();
    }

   @Override public void startSession(String sess,String id) {
      finishAll();
    }

   @Override public void inactive(long delta,long time) {
      current_region = null;
    }

   @Override public void processLine(String src,String [] cmd,long time) {
      if (src.equals("BURP") && cmd.length == 5 && cmd[0].equals("edit")) {
         String what = cmd[1];
         int dir = 0;
         switch (what) {
            case "addition" :
               dir = 1;
               break;
            case "deletion" :
               dir = -1;
               break;
            case "style change" :
            case "stylechange" :
               return;
            default :
               // System.err.println("UNKNOWN EDIT COMMAND " + what);
               return;
          }
         String id = cmd[2];
         int len = Integer.parseInt(cmd[3]);
         int pos = Integer.parseInt(cmd[4]);
         EditRegion rgn = active_regions.get(id);
         if (rgn == null) {
            rgn = new EditRegion(id,time);
            active_regions.put(id,rgn);
          }
         if (current_region == rgn) {
            setActive(rgn,time);
            rgn.operation(pos,len,dir,time);
          }
       }
      else if (src.equals("BURP") && cmd.length == 4 && cmd[0].startsWith("edit")) {
         String what = cmd[0].substring(4).toLowerCase();
         int dir = 0;
         switch (what) {
            case "addition" :
               dir = 1;
               break;
            case "deletion" :
               dir = -1;
               break;
            case "style change" :
            case "stylechange" :
               return;
            default :
               // System.err.println("UNKNOWN EDIT COMMAND " + what);
               return;
          }
         String id = cmd[2];
         int len = Integer.parseInt(cmd[3]);
         int pos = Integer.parseInt(cmd[4]);
         EditRegion rgn = active_regions.get(id);
         if (rgn == null) {
            rgn = new EditRegion(id,time);
            active_regions.put(id,rgn);
          }
         if (current_region == rgn) {
            setActive(rgn,time);
            rgn.operation(pos,len,dir,time);
          }
       }
      else if (src.equals("BUDA") && cmd[0].equals("removeBubble")) {
         if (cmd.length >= 2) {
            String id = cmd[1];
            EditRegion rgn = active_regions.remove(id);
            if (rgn != null) rgn.finish();
            if (current_region == rgn) current_region = null;
          }
       }
      else if (src.equals("BALE") && cmd[0].equals("Caret") && cmd.length == 3) {
         String id = cmd[2];
         int pos = Integer.parseInt(cmd[1]);
         EditRegion rgn = active_regions.get(id);
         if (rgn == null && pos == 0) return;
         if (rgn != current_region && pos == 0) return;
         if (rgn == null) {
            rgn = new EditRegion(id,time);
            active_regions.put(id,rgn);
          }
         setActive(rgn,time);
         rgn.setCaret(pos);
       }
    }

   @Override public void finish() {
      finishAll();
    }

   private void setActive(EditRegion rgn,long time) {
      if (rgn != current_region) {
	 Long prev = last_active.get(rgn);
	 if (prev != null) rgn.inactive(time - prev);
       }
      else {
	 Long prev = last_active.get(rgn);
	 long delta = time-prev;
	 if (delta > IDLE_TIME) {
	    rgn.inactive(delta);
	  }
       }
      last_active.put(rgn,time);
      current_region = rgn;
    }

   private void finishAll() {
      for (EditRegion rgn : active_regions.values()) {
	 rgn.finish();
       }
      active_regions.clear();
    }

}	// end of inner class EditRegionAnalysis



private static final int	BEFORE_DELTA = 4;
private static final int	AFTER_DELTA = 10;
private static final int	MIN_OPERATIONS = 10;
private static final int	MIN_SIZE = 10;
private static final int	IDLE_TIME = 10000;

private class EditRegion {

   private int	  start_pos;
   private int	  end_pos;
   private int	  cur_pos;
   private long   start_time;
   private long   end_time;
   private long   inactive_time;
   private long   pending_inactive;
   private int	  num_operations;
   private int	  dot_pos;

   EditRegion(String eid,long time) {
      start_time = time;
      end_time = time;
      initialize();
    }

   private void initialize() {
      start_pos = -1;
      end_pos = -1;
      cur_pos = -1;
      start_time = 0;
      end_time = 0;
      inactive_time = 0;
      pending_inactive = 0;
      num_operations = 0;
    }

   private void begin(int pos,long time) {
      start_pos = pos;
      end_pos = pos;
      cur_pos = pos;
      start_time = time;
      inactive_time = 0;
      pending_inactive = 0;
    }

   void operation(int pos,int len,int dir,long time) {
      if (start_pos < 0) begin(pos,time);
      else if (pos < start_pos) {
	 if (start_pos - pos > BEFORE_DELTA) {
	    finish();
	    begin(pos,time);
	  }
	 else start_pos = pos;
       }
      if (pos > end_pos) {
	 if (pos - end_pos > AFTER_DELTA) {
	    finish();
	    begin(pos,time);
	  }
	 else end_pos = pos;
       }
      inactive_time += pending_inactive;
      pending_inactive = 0;
      if (dot_pos > 0) {
	 cur_pos = dot_pos;
       }
      else {
	 cur_pos = pos + len*dir;
       }
      if (cur_pos < start_pos) start_pos = cur_pos;
      if (cur_pos > end_pos) end_pos = cur_pos;

      end_time = time;
      ++num_operations;
      dot_pos = -1;
    }

   void setCaret(int pos) {
      dot_pos = pos;
    }

   void inactive(long delta) {
      pending_inactive += delta;
    }

   void finish() {
      if (start_pos < 0) return;
      if ((end_time - start_time) < inactive_time) {
	 System.err.println("TOO MUCH INACTIVE TIME");
       }
      if (num_operations >= MIN_OPERATIONS && end_pos - start_pos >= MIN_SIZE) {
	 output_writer.println("EDITREGION," + start_pos + "," + end_pos + "," +
				  (end_pos - start_pos) + "," + num_operations + "," +
				  (end_time - start_time - inactive_time));
       }
      initialize();
    }

}	// end of inner class EditRegion




/********************************************************************************/
/*										*/
/*	User Correction Analyzer						*/
/*										*/
/********************************************************************************/

private class UserCorrectionAnalysis extends Analyzer {

   private Map<String,CorrectionRegion>  active_regions;
   private boolean auto_correction;

   UserCorrectionAnalysis() {
      active_regions = new HashMap<>();
      auto_correction = false;
    }

   @Override public void startSession(String sess,String id) {
      finishAll();
    }

   @Override public void finish() {
      finishAll();
    }

   @Override public void inactive(long delta,long time) {
      for (CorrectionRegion rgn : active_regions.values()) {
	 rgn.inactive(delta);
       }
    }

   @Override public void processLine(String src,String [] cmd,long time) {
      if (src.equals("BURP") && cmd.length == 5 && cmd[0].equals("edit")) {
	 String what = cmd[1];
	 int dir = 0;
	 switch (what) {
	    case "addition" :
	       dir = 1;
	       break;
	    case "deletion" :
	       dir = -1;
	       break;
	    default :
	       return;
	  }
	 String id = cmd[2];
	 int len = Integer.parseInt(cmd[3]);
	 int pos = Integer.parseInt(cmd[4]);
	 CorrectionRegion rgn = active_regions.get(id);
	 if (rgn == null) {
	    rgn = new CorrectionRegion();
	    active_regions.put(id,rgn);
	  }
	 if (!auto_correction) rgn.operation(pos,len,dir,time);
       }
      else if (src.equals("BUDA") && cmd[0].equals("removeBubble")) {
	 if (cmd.length > 1) {
	    String id = cmd[1];
	    active_regions.remove(id);
	  }
       }
      else if (src.equals("BFIX")) {
	 switch (cmd[0]) {
	    case "SpellingCorrection" :
	    case "SyntaxCorrection" :
	    case "QuoteCorrection" :
	    case "AddImport" :
	       auto_correction = true;
	       break;
	    case "DoneSpellingCorrection" :
	    case "DoneSyntaxCorrection" :
	    case "DoneQuoteCorrection" :
	    case "DoneAddImport" :
	       auto_correction = false;
	       break;
	  }
       }
    }

   private void finishAll() {
      active_regions.clear();
      auto_correction = false;
    }

}	// end of inner class UserCorrectionAnalysis




private enum CorrectionState { NONE, ACTIVE, EDIT, OTHER };


private class CorrectionRegion {

   private static final int START_DELTA = 2;
   private static final int END_DELTA = 12;
   private static final int ACTIVE_DELTA = 4;
   private static final int MIN_ACTIVE = 8;
   private static final int MIN_EDIT = 1;

   private int start_pos;
   private int end_pos;
   private long edit_start_time;
   private long edit_inactive_time;
   private int edit_start_pos;
   private int edit_end_pos;
   private int active_ops;
   private int edit_ops;
   private CorrectionState cur_state;

   CorrectionRegion() {
      start_pos = -1;
      end_pos = -1;
      edit_start_time = 0;
      edit_inactive_time = 0;
      active_ops = 0;
      edit_ops = 0;
      edit_start_pos = -1;
      edit_end_pos = -1;
      cur_state = CorrectionState.NONE;
    }

   void inactive(long delta) {
      if (edit_start_time > 0)
	 edit_inactive_time += delta;
    }

   void operation(int pos,int len,int dir,long time) {
      switch (cur_state) {
	 case NONE :
	    reset(pos);
	    break;
	 case ACTIVE :
	    if (pos < start_pos - START_DELTA || pos > end_pos + END_DELTA) {
	       reset(pos);
	       ++active_ops;
	     }
	    else if (Math.abs(pos - end_pos) < ACTIVE_DELTA) {
	       // still editing at the end
	       ++active_ops;
	     }
	    else if (active_ops > MIN_ACTIVE) {
	       resetEdit(pos,time);
	       ++edit_ops;
	     }
	    else {
	       cur_state = CorrectionState.OTHER;
	     }
	    updatePos(pos,len,dir);
	    break;
	 case EDIT :
	    if (pos < edit_start_pos - START_DELTA || pos > edit_end_pos + len + START_DELTA) {
	       if (Math.abs(pos - end_pos) < ACTIVE_DELTA) {
		  finish(time);
		  cur_state = CorrectionState.ACTIVE;
		  updatePos(pos,len,dir);
		}
	     }
	    else if (pos >= end_pos) {
	       active_ops += edit_ops;
	       cur_state = CorrectionState.ACTIVE;
	       updatePos(pos,len,dir);
	     }
	    else {
	       ++edit_ops;
	       updatePos(pos,len,dir);
	     }
	    break;
	 case OTHER :
	    if (pos < start_pos - START_DELTA || pos > end_pos + END_DELTA) {
	       reset(pos);
	       ++active_ops;
	     }
	    else if (pos >= end_pos) {
	       active_ops = 0;
	       cur_state = CorrectionState.ACTIVE;
	     }
	    updatePos(pos,len,dir);
	    break;
       }
    }

   private void finish(long time) {
      long delta = time - edit_start_time - edit_inactive_time;
      if (delta >= 1000 && delta <= 30000 && edit_ops >= MIN_EDIT) {
	 output_writer.println("CORRECTION," + start_pos + "," + end_pos + "," +
				  edit_start_pos + "," + edit_end_pos + "," + active_ops + "," +
				  edit_ops + "," + (time - edit_start_time - edit_inactive_time));
       }

      resetEdit(-1,0);
    }


   private void updatePos(int pos,int len,int dir) {
      if (dir > 0) pos += len;
      if (start_pos > pos) start_pos = pos;
      if (end_pos < pos) end_pos = pos;

      if (cur_state == CorrectionState.ACTIVE) {
	 end_pos = pos;
       }

      if (edit_start_pos > 0) {
	 if (edit_start_pos > pos) edit_start_pos = pos;
	 if (edit_end_pos < pos) edit_end_pos = pos;
       }
    }

   private void reset(int pos) {
      start_pos = pos;
      end_pos = pos;
      edit_start_time = 0;
      edit_inactive_time = 0;
      edit_start_pos = -1;
      edit_end_pos = -1;
      active_ops = 0;
      edit_ops = 0;
      cur_state = (pos < 0 ? CorrectionState.NONE : CorrectionState.ACTIVE);
    }

   private void resetEdit(int pos,long time) {
      edit_start_time = time;
      edit_inactive_time = 0;
      edit_ops = 0;
      edit_start_pos = pos;
      edit_end_pos = pos;
      cur_state = CorrectionState.EDIT;
    }

}	// end of inner class CorrectionRegion




/********************************************************************************/
/*										*/
/*	Auto Correciotn analysis						*/
/*										*/
/********************************************************************************/

private class AutoCorrectionAnalysis extends Analyzer {

   private Map<String,int []>		item_counts;
   private Map<String,int []>		explicit_counts;
   private AutoCorrectStatData			spell_stats;
   private AutoCorrectStatData			import_stats;
   private boolean			in_explicit;
   private boolean			have_data;

   AutoCorrectionAnalysis() {
      in_explicit = false;
      item_counts = new LinkedHashMap<String,int []>();
      item_counts.put("StartImplicitFix",new int[] { 0 });
      item_counts.put("SPELLFIX",new int[] { 0 });
      item_counts.put("SpellingCorrection",new int [] { 0 });
      item_counts.put("SYNTAXFIX",new int [] { 0 });
      item_counts.put("SyntaxCorrection",new int [] { 0 });
      item_counts.put("IMPORTFIX",new int [] { 0 });
      item_counts.put("AddImport",new int[] { 0 });
      item_counts.put("QUOTEFIX",new int [] { 0 });
      item_counts.put("QuoteCorrection",new int [] { 0 });
      item_counts.put("UserCorrect",new int [] { 0 });
      item_counts.put("AutoCompleteIt",new int [] { 0 });
      item_counts.put("StartExplicitFix",new int [] { 0 });
   
      explicit_counts = new LinkedHashMap<String,int []>();
      for (String s : item_counts.keySet()) {
         explicit_counts.put(s,new int [] { 0 });
       }
      have_data = false;
      spell_stats = new AutoCorrectStatData();
      import_stats = new AutoCorrectStatData();
    }

   @Override public void startSession(String sess,String id)	{ finish(false); }
   @Override public void inactive(long delta,long time) { }
   @Override public void processLine(String src,String [] cmd,long time) {
      if (cmd.length == 0) return;
      
      if (!in_explicit) {
         int [] ctr = item_counts.get(cmd[0]);
         if (ctr != null) ctr[0]++;
         if (src.equals("BFIX") && cmd[0].equals("StartExplicitFix")) in_explicit = true;
       }
      else {
         int [] ctr = explicit_counts.get(cmd[0]);
         if (ctr != null) ctr[0]++;
         if (src.equals("BALE")) in_explicit = false;
       }
      switch (cmd[0]) {
         case "SpellCheck" :
            spell_stats.add(cmd[1]);
            break;
         case "ImportCheck" :
            import_stats.add(cmd[1]);
            break;
         default :
            break;
       }
      have_data = true;
    }

   @Override public void finish() {
      finish(true);
    }

   private void finish(boolean fg) {
      if (!have_data) return;
      output_writer.print("BFIX");
      for (Map.Entry<String,int []> ent : item_counts.entrySet()) {
	 int [] ctr = ent.getValue();
	 output_writer.print("," + ctr[0]);
	 ctr[0] = 0;
       }
      for (Map.Entry<String,int []> ent : explicit_counts.entrySet()) {
	 int [] ctr = ent.getValue();
	 output_writer.print("," + ctr[0]);
	 ctr[0] = 0;
       }
      output_writer.println();

      if (fg) {
	 spell_stats.output(output_writer,"SPELL");
	 import_stats.output(output_writer,"IMPORT");
       }

      have_data = false;
    }

}	// end of inner class AutoCorrectionAnalysis



private static class AutoCorrectStatData {

   private Map<Integer,int []>	data_set;

   AutoCorrectStatData() {
      data_set = new HashMap<Integer,int []>();
    }

   void add(int v) {
      int [] ct = data_set.get(v);
      if (ct == null) {
	 ct = new int[] { 0 };
	 data_set.put(v,ct);
       }
      ct[0] += 1;
    }


   void add(String s) {
      try {
	 add(Integer.parseInt(s));
       }
      catch (NumberFormatException e) { }
    }

   void output(PrintStream out,String what) {
      double tot = 0;
      double tot2 = 0;
      double cnt = 0;
      List<Integer> mode = new ArrayList<Integer>();
      int modect = 0;
      for (Map.Entry<Integer,int []> ent : data_set.entrySet()) {
	 int ct = ent.getValue()[0];
	 int v = ent.getKey();
	 tot += v*ct;
	 tot2 += (v*v)*ct;
	 cnt += ct;
	 if (ct > modect) {
	    mode = new ArrayList<Integer>();
	    modect = ct;
	  }
	 if (ct == modect) mode.add(v);
       }
      if (cnt == 0) return;

      double var = (tot2 - tot*tot/cnt)/(cnt-1);

      out.print("BFIX_" + what);
      out.print("," + ((int) cnt));
      out.print("," + intval(tot/cnt));
      out.print("," + intval(var));
      out.print("," + intval(Math.sqrt(var)));
      out.print("," + modect);
      for (Integer v : mode) {
	 out.print("," + v);
       }
      out.println();
    }


   int intval(double v) {
      return (int) (v*1000);
    }

}	// end of inner class StatData




/********************************************************************************/
/*                                                                              */
/*      SEEDE usage analysis                                                    */
/*                                                                              */
/********************************************************************************/

private class SeedeAnalysis extends Analyzer {
   
   private Map<String,int []> op_counts;
   
   SeedeAnalysis(){
      op_counts = new HashMap<>();
    }
   
   @Override public void processLine(String src,String [] cmd,long time) {
      if (!src.equals("BICEX")) return;
      String what = cmd[0];
      int [] cnt = op_counts.get(what);
      if (cnt == null) {
         cnt = new int[1];
         op_counts.put(what,cnt);
       }
      ++cnt[0];
    }
   
   @Override public void finish() {
      for (Map.Entry<String,int []> ent : op_counts.entrySet()) {
         output_writer.println("BICEX_" + ent.getKey() + "," + ent.getValue()[0]);
       }
    }
   
}       // end of inner class SeedeAnalysis



/********************************************************************************/
/*                                                                              */
/*      Command usage analyzer                                                  */
/*                                                                              */
/********************************************************************************/


private static Set<String> edit_commands = Set.of("addition",
      "deletion",
      "stylechange",
      "style change");

private final class CommandUsageAnalysis extends Analyzer { 
   
   private Map<String,CommandUsageStats> usage_map;
   private long last_time;
   private long session_start;
   private long session_inactive;
   private long total_active;
   
   CommandUsageAnalysis() {
      usage_map = new TreeMap<>();
      last_time = 0;
      session_start = 0;
      session_inactive = 0;
      total_active = 0;
    }
   
   @Override public void startSession(String sess,String id) {
      finishSession();
      last_time = 0;
      session_start = 0;
      session_inactive = 0;
    }
   
   @Override public void processLine(String src,String [] cmd,long time) {
      if (cmd.length == 0) return;
      if (session_start == 0) session_start = time;
      String carg = cmd[0];
      String xarg = null;
      if (carg.startsWith("edit") && carg.length() > 4) {
         xarg = carg.substring(4);
         carg = "edit";
       }
      switch (carg) {
         case "edit" :
            String x = (xarg == null ? cmd[1] : xarg);
            x = x.toLowerCase();
            if (x.equals("aggiunta")) x = "addition";
            if (x.equals("eliminazione")) x = "deletion";
            if (x.equals("borttagning")) x = "deletion";
            if (x.startsWith("till") && x.endsWith("gg")) x = "addition";      
            if (x.equals("\u6dfb\u52a0")) x = "addition";
            if (x.equals("\u5220\u9664")) x = "deletion";
            if (x.equals("ajout")) x = "addition";
            if (x.startsWith("supresi") && x.endsWith("n")) x = "deletion";
            if (x.startsWith("adici") && x.endsWith("n")) x = "addition";
            if (x.startsWith("l") && x.endsWith("schen")) x = "deletion";
            if (x.startsWith("hinzuf") && x.endsWith("gen")) x = "addition";
            if (x.startsWith("formatvorlagen")) x = "stylechange";
            if (x.startsWith("adi") && x.endsWith("o")) x = "addition";
            if (x.startsWith("exclus") && x.endsWith("o")) x = "deletion";
            if (x.startsWith("agregaci") && x.endsWith("n")) x = "addition";
            if (x.startsWith("cambiamento")) x = "stylechange";
            if (x.equals("suppression")) x = "deletion";
            if (!edit_commands.contains(x)) { 
               x = "unknown"; 
             }
            x = x.replace(" ","");
            x = x.substring(0,1).toUpperCase() + x.substring(1);
            carg += x;
            break;
         case "VIS" :
         case "CNT" :
            return;
       }
      if (carg.startsWith("NewBubbleLOC")) {
          carg = carg.substring(0,9);
       }
      else if (carg.equals("OTHERVALUE")) return;
      
      double delta = time - last_time;
      last_time = time;
      if (last_time == delta) return;           // last_time was 0
      if (delta < 0) {
         delta = 0;                             // items out of order (threeds...)
         last_time -= delta;
       }
      if (src.equals("PROPERTY") || src.equals("BRACT")) return;
      carg = src + ":" + carg;
      CommandUsageStats stats = getStats(carg);
      stats.addTime(delta);
    }
   
   @Override public void inactive(long delta,long time) {
      session_inactive += delta;
      last_time = time;
    }
   
   @Override public void finish() {
      finishSession();
      output_writer.println("COMMAND,Name,Count,Average (ms),StdDev,Min,Max,Total,Percent"); 
      for (CommandUsageStats stats : usage_map.values()) {
         stats.output(total_active);
       }
      last_time = 0;
    }
   
   private CommandUsageStats getStats(String id) {
      CommandUsageStats stats = usage_map.get(id);
      if (stats == null) {
         stats = new CommandUsageStats(id);
         usage_map.put(id,stats);
       }
      return stats;
    }
   
   private void finishSession() {
      if (session_start == 0) return;
      long active = last_time - session_start - session_inactive;
      if (active < 0) {
         return;
       }
      total_active += active;
      CommandUsageStats stats = getStats("SessionTotal");
      stats.addTime(last_time - session_start);
      stats = getStats("SessionActive");
      stats.addTime(active);
      stats = getStats("SessionInactive");
      stats.addTime(session_inactive);
      session_start = 0;
      session_inactive = 0;
    }
   
}       // end of inner class CommandUsageAnalyzer



private final class CommandUsageStats {
   
   private String command_name;
   private double total_time;
   private double total_time2;
   private int num_times;
   private double min_time;
   private double max_time;
   
   CommandUsageStats(String name) {
      command_name = name;
      total_time = 0;
      total_time2 = 0;
      num_times = 0;
      min_time = -1;
      max_time = -1;
    }
   
   void addTime(double t) {
      num_times++;
      total_time += t;
      total_time2 += t*t;
      if (num_times == 1) {
         min_time = t;
         max_time = t;
       }
      else {
         min_time = Math.min(min_time,t);
         max_time = Math.max(max_time,t);
       }
    }
  
   void output(double total) {
      double avg = total_time / num_times;
      double var = (total_time2/num_times - avg * avg)/num_times;
      double stdev = Math.sqrt(var);
      double pct = total_time / total * 100;
      
      output_writer.println("COMMAND," + command_name + "," +
            num_times + "," + avg + "," +  stdev + "," +
            min_time + "," + max_time + "," + total_time + 
            "," + pct);
    }
   
}       // end of inner class CommandUsageStats


}	// end of class BoardMetrixAnalyzer





/* end of BoardMetrixAnalyzer.java */
