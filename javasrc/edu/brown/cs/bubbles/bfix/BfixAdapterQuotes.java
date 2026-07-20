/********************************************************************************/
/*										*/
/*		BfixAdapterQuotes.java						*/
/*										*/
/*	Handle problesm with mistyped quotes					*/
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



package edu.brown.cs.bubbles.bfix;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.ivy.xml.IvyXml;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.w3c.dom.Element;



class BfixAdapterQuotes extends BfixAdapter implements BfixConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static List<BfixErrorPattern> quote_patterns;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BfixAdapterQuotes()
{
    super("Quote fixer");
    if (quote_patterns == null) {
       quote_patterns = new ArrayList<>();
       Element xml = BumpClient.getBump().getLanguageData();
       Element fxml = IvyXml.getChild(xml,"FIXES");
       for (Element cxml : IvyXml.children(fxml,"QUOTE")) {
          quote_patterns.add(new BfixErrorPattern(cxml));
        }
     }
}



/********************************************************************************/
/*										*/
/*	Abstract method implementations 					*/
/*										*/
/********************************************************************************/

@Override public void addFixers(BfixCorrector corr,BumpProblem bp,boolean explicit,List<BfixFixer> rslt)
{
   List<QuoteFix> fixes = findFixes(corr,bp);
   if (fixes != null && fixes.size() > 0) {
      BoardLog.logD("BFIX","Handle quote problem " + bp.getMessage());
      QuoteFixer qf = new QuoteFixer(corr,bp,fixes);
      rslt.add(qf);
    }
}


@Override protected String getMenuAction(BfixCorrector corr,BumpProblem bp)
{
   List<QuoteFix> fixes = findFixes(corr,bp);
   if (fixes != null && fixes.size() > 0) return "quote error";

   return null;
}




/********************************************************************************/
/*										*/
/*	Find potential fixes							*/
/*										*/
/********************************************************************************/

private List<QuoteFix> findFixes(BfixCorrector corr,BumpProblem bp)
{
   if (bp.getErrorType() != BumpErrorType.ERROR) return null;
   String msg = bp.getMessage();

   List<QuoteFix> rslt = new ArrayList<QuoteFix>();

   BaleWindow win = corr.getEditor();
   BaleWindowDocument doc = win.getWindowDocument();

   int soff = doc.mapOffsetToJava(bp.getStart());
   int cpos = corr.getCaretPosition();
   if (cpos <= soff) return null;

   int lnoerr = doc.findLineNumber(soff);
   int lnocur = doc.findLineNumber(cpos);

   String text = doc.getWindowText(soff,cpos-soff);
   if (lnocur > lnoerr) {
      int idx = text.indexOf("\n");
      if (idx >= 0) text = text.substring(0,idx);
    }

   for (BfixErrorPattern pat : quote_patterns) {
      String mrslt = pat.getMatchResult(msg);
      if (mrslt == null) continue;
      if (mrslt.equals("'")) {
         int idx = text.indexOf("\"");
         if (idx > 0 && (lnocur > lnoerr || soff + idx + 2 < corr.getCaretPosition())) {
            QuoteFix qf = new QuoteFix(corr,soff,soff+1,"\"","'");
            rslt.add(qf);
          }
       }
      else if (mrslt.equals("\"")) {
         for (int i = 1; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (c == '\'') {
               QuoteFix qf = new QuoteFix(corr,soff+i,soff+i+1,"\"","'");
               rslt.add(qf);
             }
            else if (";:+)\n".indexOf(c) >= 0) {
               QuoteFix qf = new QuoteFix(corr,soff+i-1,soff+i-1,"\"",null);
               rslt.add(qf);
             }
          }
       }
    }

   if (rslt.size() == 0) return null;

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Potential quote fix							*/
/*										*/
/********************************************************************************/

private static class QuoteFix implements BfixEdit {

   private BfixCorrector for_corrector;
   private int edit_start;
   private int edit_end;
   private String edit_insert;
   private String edit_replace;

   QuoteFix(BfixCorrector corr,int start,int end,String insert,String replace) {
      for_corrector = corr;
      edit_start = start;
      edit_end = end;
      edit_insert = insert;
      edit_replace = replace;
    }
   
   @Override public void doEdit(boolean format,boolean indent) {
      BaleWindowDocument doc = for_corrector.getEditor().getWindowDocument();
      doc.replace(edit_start,edit_end-edit_start,edit_insert,false,false);
    }
   
   @Override public boolean makeEdit(String pid) {
      BaleWindowDocument doc = for_corrector.getEditor().getWindowDocument();
      File file = doc.getFile();
      String proj = doc.getProjectName();
      int soff = doc.mapOffsetToEclipse(edit_start);
      int eoff = doc.mapOffsetToEclipse(edit_end); 
      BumpClient bc = BumpClient.getBump();
      bc.editPrivateFile(proj,file,pid,soff,eoff,edit_insert);
      return true;
    }
   
   @Override public boolean unmakeEdit(String pid) {
      BaleWindowDocument doc = for_corrector.getEditor().getWindowDocument();
      File file = doc.getFile();
      String proj = doc.getProjectName();
      int soff = edit_start;
      int eoff = edit_start;
      if (edit_insert != null) eoff += edit_insert.length();
      soff = doc.mapOffsetToEclipse(soff);
      eoff = doc.mapOffsetToEclipse(eoff); 
      BumpClient bc = BumpClient.getBump();
      bc.editPrivateFile(proj,file,pid,soff,eoff,edit_replace);
      return true;
    }
   
   @Override public int getDelta() {
      int delta = edit_start - edit_end;;
      if (edit_insert != null) delta += edit_insert.length();
      return delta;
    }
   int getStart()                       { return edit_start; }
   int getEnd()                         { return edit_end; }

   @Override public String toString() {
      String rslt = "EDIT (" + edit_start + ":" + edit_end + ")";
      if (edit_insert != null) rslt += " [" + edit_insert + "]";
      return rslt;
    }

}	// end of inner class QuoteFix




/********************************************************************************/
/*										*/
/*	Quote error fixer							*/
/*										*/
/********************************************************************************/

private class QuoteFixer extends BfixFixer {

   private long initial_time;
   private List<QuoteFix> potential_fixes;

   QuoteFixer(BfixCorrector c,BumpProblem p,List<QuoteFix> fixes) {
      super(c,p);
      initial_time = c.getStartTime();
      potential_fixes = fixes;
    }

   @Override protected String getMemoId() {
      return potential_fixes.toString();
    }
   
   @Override protected BfixRunnableFix findFix() {
      BfixCheckAreas darea = new BfixCheckAreas(0,-1);
      Collection<BfixEdit> eds = new ArrayList<>(potential_fixes);
      List<BfixEdit> useedits = findPrivateEdits(eds,null,darea,false);
      if (useedits == null || useedits.size() != 1) return null;
      QuoteFix usefix = (QuoteFix) useedits.get(0); 
      if (for_corrector.getStartTime() != initial_time) return null;
      
      BoardLog.logD("BFIX","SPELL: DO quote edit " + usefix);
      BoardMetrics.noteCommand("BFIX","QUOTEFIX");
      QuoteDoer sd = new QuoteDoer(for_corrector,for_problem,usefix,initial_time);
      return sd;
    }

}	// end of inner class QuoteFixer



/********************************************************************************/
/*										*/
/*	Class to fix the quote error						*/
/*										*/
/********************************************************************************/

private class QuoteDoer extends BfixFixDoer {

   private QuoteFix using_fix;

   QuoteDoer(BfixCorrector corr,BumpProblem bp,QuoteFix qf,long t0) {
      super(corr,bp,t0);
      using_fix = qf;
    }

   @Override public Boolean call() {
      BfixCheckAreas sareas = new BfixCheckAreas(using_fix.getStart(),using_fix.getEnd());
      return testEdit(using_fix,sareas,"QuoteCorrection");
    }

   @Override public double getRegionOrder()                { return 0; } 
   
}	// end of inner class QuoteDoer


}	// end of class BfixAdapterQuotes




/* end of BfixAdapterQuotes.java */

