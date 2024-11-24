/********************************************************************************/
/*                                                                              */
/*              BoardMetricResults.java                                         */
/*                                                                              */
/*      Analyze metric output                                                   */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.board;

import edu.brown.cs.ivy.file.IvyFormat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class BoardMetricResults implements BoardConstants
{


/********************************************************************************/
/*                                                                              */
/*      Main program                                                            */
/*                                                                              */
/********************************************************************************/

public static void main(String [] args)
{
   BoardMetricResults bmr = new BoardMetricResults(args);
   bmr.process();
}




/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

enum Analysis { BFIX, SEEDE };

private Analysis        analysis_type;
private List<File>      input_files;
private PrintStream     output_stream;





/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BoardMetricResults(String [] args)
{
   analysis_type = Analysis.BFIX;
   input_files = new ArrayList<File>();
   output_stream = null;
   
   scanArgs(args);
}




/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

private void process()
{
   Analyzer anal = null;
   switch (analysis_type) {
      case BFIX :
      default :
         anal = new BfixAnalyzer();
         break;
      case SEEDE :
         anal = new SeedeAnalyzer();
         break;
    }
   
   try {
      for (File f : input_files) {
         try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            for ( ; ; ) {
               String ln = br.readLine();
               if (ln == null) break;
               processLine(anal,ln);
             }
          }
       }
    }
   catch (IOException e) {
      System.err.println("Problem reading input: " + e);
      e.printStackTrace();
      System.exit(1);
    }
   
   anal.finish();
}



private void processLine(Analyzer anal,String ln)
{
   String [] data = ln.split(",");
   switch (data[0]) {
      case "SESSION" :
         anal.finishSession(getValue(data[2]));
         break;
      default :
         long [] values = new long[data.length-1];
         for (int i = 1; i < data.length; ++i) {
            values[i-1] = getValue(data[i]);
          }
         anal.process(data[0],values);
         break;
    }
}



private long getValue(String d)
{
   try {
      return Long.parseLong(d);
    }
   catch (NumberFormatException e) {
      System.err.println("Unexpected value: " + d);
    }
   
   return 0L;
}



/********************************************************************************/
/*                                                                              */
/*      Argument Scanning methods                                               */
/*                                                                              */
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
         if (args[i].startsWith("-i") && i+1 < args.length) {           // -i <input>
            input_files.add(new File(args[++i]));
          }
         else if (args[i].startsWith("-B")) {
            analysis_type = Analysis.BFIX;
          }
         else if (args[i].startsWith("-S")) {
            analysis_type = Analysis.SEEDE;
          }
         else if (args[i].startsWith("-o") && i+1 < args.length) {       // -o output
            try {
               output_stream = new PrintStream(args[++i]);
             }
            catch (IOException e) {
               badArgs();
             }
          }
         else badArgs();
       }
      else {
         input_files.add(new File(args[i]));
       }
    }
}



private void badArgs()
{
   System.err.println("MetricResults: [-BFIX] input_file");
   System.exit(1);
}




/********************************************************************************/
/*                                                                              */
/*      Analyzer tyeps                                                          */
/*                                                                              */
/********************************************************************************/

private abstract static class Analyzer {
   
   abstract void process(String type,long [] values);
   abstract void finishSession(long time);
   abstract void finish();
   
   protected String fixTime(double v) {
      return IvyFormat.formatTime(v);
    }
   
   protected String fixPct(double v) {
      return IvyFormat.formatPercent(v);
    }
   
   protected String fixNumber(double v) {
      return IvyFormat.formatNumber(v);
    }
   
   protected String fixDouble(double v) {
      v = v / 1000;
      return IvyFormat.formatNumber(v);
    }
   
}       // end of inner class Analyzer




/********************************************************************************/
/*                                                                              */
/*      BFIX analyzer                                                           */
/*                                                                              */
/********************************************************************************/

private static List<String> ITEM_NAMES;

static {
   ITEM_NAMES = new ArrayList<String>();
   ITEM_NAMES.add("StartImplicitFix");
   ITEM_NAMES.add("SPELLFIX");
   ITEM_NAMES.add("SpellingCorrection");
   ITEM_NAMES.add("SYNTAXFIX");
   ITEM_NAMES.add("SyntaxCorrection");
   ITEM_NAMES.add("IMPORTFIX");
   ITEM_NAMES.add("AddImport");
   ITEM_NAMES.add("QUOTEFIX");
   ITEM_NAMES.add("QuoteCorrection");
   ITEM_NAMES.add("UserCorrect");
   ITEM_NAMES.add("AutoCompleteIt");
   ITEM_NAMES.add("StartExplicitFix");   
   ITEM_NAMES.add("Explicit StartImplicitFix");
   ITEM_NAMES.add("Explicit SPELLFIX");
   ITEM_NAMES.add("Explicit SpellingCorrection");
   ITEM_NAMES.add("Exclicit SYNTAXFIX");
   ITEM_NAMES.add("Explicit SyntaxCorrection");
   ITEM_NAMES.add("Explicit IMPORTFIX");
   ITEM_NAMES.add("Explicit AddImport");
   ITEM_NAMES.add("Explicit QUOTEFIX");
   ITEM_NAMES.add("Explicit QuoteCorrection");
   ITEM_NAMES.add("Explicit UserCorrect");
   ITEM_NAMES.add("Explicit AutoCompleteIt");
   ITEM_NAMES.add("Explicit StartExplicitFix");       
}



private  class BfixAnalyzer extends Analyzer {
   
   private double total_time;
   private double edit_time;
   private double session_edit;
   private double bfix_time;
   private double nofix_time;
   private List<Double> correction_times;
   private List<Integer> edit_lengths;
   private double [] bfix_times;
   private long [] spell_data;
   private long [] import_data;
   
   BfixAnalyzer() {
      total_time = 0;
      edit_time = 0;
      session_edit = 0;
      bfix_time  = 0;
      nofix_time = 0;
      correction_times = new ArrayList<Double>();
      edit_lengths = new ArrayList<Integer>();
      bfix_times = new double[ITEM_NAMES.size()];
    }
   
   @Override void process(String type,long [] data) {
      switch (type) {
         case "CORRECTION" :
            correction_times.add((double) data[6]);
            break;
         case "EDITREGION" :
            session_edit += data[4];
            edit_lengths.add((int) data[2]);
            break;
         case "BFIX" :
            if (session_edit != 0) {
               if (data[1] > 0) {
                  for (int i = 0; i < data.length; ++i) {
                     bfix_times[i] += data[i];
                   }
                  bfix_time += session_edit;
                }
               else nofix_time += session_edit;
             }
            break;
         case "BFIX_SPELL" :
            spell_data = data;
            break;
         case "BFIX_IMPORT" :
            import_data = data;
            break;
       }
    }
   
   @Override void finishSession(long time) {
      if (session_edit == 0) return;
      edit_time += session_edit;
      total_time += time;
      session_edit = 0;
    }
   
   @Override void finish() {
      Collections.sort(correction_times);
      int cln = correction_times.size();
      double ctot = 0;
      for (Double d : correction_times) ctot += d;
      
      Collections.sort(edit_lengths);
      int eln = edit_lengths.size();
      double ltot = 0;
      for (Integer i : edit_lengths) ltot += i;
      
      if (bfix_time > 0) {
         double div = bfix_time / (1000*60*60);
         for (int i = 0; i < bfix_times.length; ++i) {
            bfix_times[i] /= div;
          }
       }
      
      PrintStream out = output_stream;
      if (out == null) out = System.out;
      
      out.println("Total Time   : " + fixTime(total_time));
      out.println("Editing Time : " + fixTime(edit_time));
      out.println("Bfix Time    : " + fixTime(bfix_time));
      out.println("No fix time  : " + fixTime(nofix_time));
      out.println("Editing Pct  : " + fixPct(edit_time/total_time));
      out.println("Bfix Pct     : " + fixPct(bfix_time/total_time));
      out.println("Bfix Edit Pct: " + fixPct(bfix_time/edit_time));
   
      out.println("Correction min : " + fixTime(correction_times.get(0)));
      out.println("Correction max : " + fixTime(correction_times.get(cln-1)));
      out.println("Correction avg : " + fixTime(ctot/cln));
      out.println("Correction med : " + fixTime(correction_times.get(cln/2)));
      
      out.println("Edit Length min: " + fixNumber(edit_lengths.get(0)));
      out.println("Edit length max: " + fixNumber(edit_lengths.get(eln-1)));
      out.println("Edit Length avg: " + fixNumber(ltot/eln));
      out.println("Edit length med: " + fixNumber(edit_lengths.get(eln/2)));
      
      for (int i = 0; i < bfix_times.length; ++i) {
         if (bfix_times[i] != 0) {
            out.println(ITEM_NAMES.get(i) + " : " + fixNumber(bfix_times[i]));
          }
       }
      
      out.println("Spell size avg : " + fixDouble(spell_data[1]));
      out.println("Spell size var : " + fixDouble(spell_data[2]));
      out.println("Spell size std : " + fixDouble(spell_data[3]));
      out.println("Spell size mod : " + spell_data[5]);
      out.println("Spell size cnt : " + fixPct(((double) spell_data[4])/spell_data[0]));
      
      out.println("Import size avg: " + fixDouble(import_data[1]));
      out.println("Import size var: " + fixDouble(import_data[2]));
      out.println("Import size std: " + fixDouble(import_data[3]));
      out.println("Import size mod: " + import_data[5]);
      out.println("Import size cnt: " + fixPct(((double) import_data[4])/import_data[0]));
      
      if (output_stream != null) output_stream.close();
    }
   
}       // end of inner class BfixAnalyzer



private  class SeedeAnalyzer extends Analyzer {
   
   // use after 4/1/2018
   
   private long total_time;
   private double num_start;
   private double num_reset;
   private double num_exec;
   private double num_goto;
   
   SeedeAnalyzer() {
      total_time = 0;
      num_start = 0;
      num_reset = 0;
      num_exec = 0;
      num_goto = 0;
    }
   
   @Override void process(String type,long [] data) {
      if (!type.startsWith("BICEX_")) return;
      type = type.substring(6);
      switch (type) {
         case "Exec reset" :
         case "ExecReset" :
            num_reset += data[0];
            break;
         case "Exec returned" :
         case "ExecReturned" :
            num_exec += data[0];
            break;
         case "GotoContext" :
         case "GotoTime" :
         case "GotoTimeContext" :
         case "GotoSource" :
            num_goto += data[0];
            break;
         case "StartTest" :
            num_start += data[0];
            break;
         case "Start" :
            num_start += data[0];
            break;
         case "EmptyResult" :
            // count number of empty results
            break;
         case "Result" :
            // get time and length statistics over actual results
            // separate values for results with and without errors
            break;
            
         case "Input Request" :
         case "InputRequest" :
         case "AddOpenEditors" :
         case "InitialValueRequest" :
         case "BubbleVisible" :
         case "ChangeTime" :
         case "OutputCollapse" :
         case "OutputExpand" :
         case "ValueCollapse" :
         case "ValueExpand" :
         case "SetInitialValue" :
         case "CreateGraphics" :
         case "ExpandVariable" :
         case "ShowToString" :
            break;
         default :
            System.err.println("Unknown BICEX value: " + type);
            break;
       }
    }
   
   @Override void finishSession(long time) {
      total_time += time;
    }
   
   @Override void finish() {
      PrintStream out = output_stream;
      if (out == null) out = System.out;
      
      double hrs = total_time / 1000 / 3600;
      num_start = num_start/2;                          // remove debugging runs ???
      
      out.println("Total Time    : " + fixTime(total_time));
      out.println("Number of Uses: " + num_start);
      out.println("Uses/hour     : " + fixNumber(num_start / hrs));
      out.println("Runs / Use    : " + fixNumber(num_exec / num_start));
      out.println("Aborted / Use : " + fixNumber(num_reset / num_start));
      out.println("Finished / Use: " + fixNumber((num_exec-num_reset) / num_start));
      out.println("Goto / Use    : " + fixNumber(num_goto / num_start));  
      
      if (output_stream != null) output_stream.close();
    }
   
}       // end of inner class BfixAnalyzer






}       // end of class BoardMetricResults




/* end of BoardMetricResults.java */

