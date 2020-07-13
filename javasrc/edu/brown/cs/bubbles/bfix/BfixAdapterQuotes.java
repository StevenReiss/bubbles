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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;



class BfixAdapterQuotes extends BfixAdapter implements BfixConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BfixAdapterQuotes()
{
    super("Quote fixer");
}



/********************************************************************************/
/*										*/
/*	Abstract method implementations 					*/
/*										*/
/********************************************************************************/

@Override void addFixers(BfixCorrector corr,BumpProblem bp,boolean explicit,List<BfixFixer> rslt)
{
   List<QuoteFix> fixes = findFixes(corr,bp);
   if (fixes != null && fixes.size() > 0) {
      BoardLog.logD("BFIX","Handle quote problem " + bp.getMessage());
      QuoteFixer qf = new QuoteFixer(corr,bp,fixes);
      rslt.add(qf);
    }
}


@Override String getMenuAction(BfixCorrector corr,BumpProblem bp)
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

   if (msg.startsWith("Invalid character constant")) {   // 'xxxxx"
      int idx = text.indexOf("\"");
      if (idx > 0 && (lnocur > lnoerr || soff + idx + 2 < corr.getCaretPosition())) {
	 QuoteFix qf = new QuoteFix(soff,soff+1,"\"","'");
	 rslt.add(qf);
       }
    }

   if (msg.startsWith("String literal is not properly closed")) {
      for (int i = 1; i < text.length(); ++i) {
	 char c = text.charAt(i);
	 if (c == '\'') {
	    QuoteFix qf = new QuoteFix(soff+i,soff+i+1,"\"","'");
	    rslt.add(qf);
	  }
	 else if (";:+)\n".indexOf(c) >= 0) {
	    QuoteFix qf = new QuoteFix(soff+i-1,soff+i-1,"\"",null);
	    rslt.add(qf);
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

private static class QuoteFix {

   private int edit_start;
   private int edit_end;
   private String edit_insert;
   private String edit_replace;

   QuoteFix(int start,int end,String insert,String replace) {
      edit_start = start;
      edit_end = end;
      edit_insert = insert;
      edit_replace = replace;
    }

   void makeEdit(BaleWindowDocument doc) {
      doc.replace(edit_start,edit_end-edit_start,edit_insert,false,false);
    }

   void makeEdit(BumpClient bc,String proj,File file,String pid,BaleWindowDocument doc) {
      int soff = doc.mapOffsetToEclipse(edit_start);
      int eoff = doc.mapOffsetToEclipse(edit_end);
      bc.editPrivateFile(proj,file,pid,soff,eoff,edit_insert);
    }

   void undoEdit(BumpClient bc,String proj,File file,String pid,BaleWindowDocument doc) {
      int soff = edit_start;
      int eoff = edit_start;
      if (edit_insert != null) eoff += edit_insert.length();
      soff = doc.mapOffsetToEclipse(soff);
      eoff = doc.mapOffsetToEclipse(eoff);
      bc.editPrivateFile(proj,file,pid,soff,eoff,edit_replace);
    }

   int getDelta() {
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
   List<QuoteFix> potential_fixes;

   QuoteFixer(BfixCorrector c,BumpProblem p,List<QuoteFix> fixes) {
      super(c,p);
      initial_time = c.getStartTime();
      potential_fixes = fixes;
    }

   @Override protected String getMemoId() {
      return potential_fixes.toString();
    }
   
   @Override protected RunnableFix findFix() {
      QuoteFix usefix = null;
      BaleWindow win = for_corrector.getEditor();
      BaleWindowDocument doc = win.getWindowDocument();
      String proj = doc.getProjectName();
      File file = doc.getFile();
      String filename = file.getAbsolutePath();
      BumpClient bc = BumpClient.getBump();
      String pid = createPrivateBuffer(proj,filename);
      if (pid == null) return null;
      BoardLog.logD("BFIX","SPELL: using private buffer " + pid);

      try {
	 Collection<BumpProblem> probs = bc.getPrivateProblems(filename,pid);
	 if (probs == null) {
	    BoardLog.logE("BFIX","SPELL: Problem getting errors for " + pid);
	    return null;
	  }
	 int probct = getErrorCount(probs);
	 if (!checkProblemPresent(for_problem,probs)) return null;
	 for (QuoteFix qf : potential_fixes) {
	    bc.beginPrivateEdit(filename,pid);
	    BoardLog.logD("BFIX","SPELL: Try quote edit " + qf);
	    qf.makeEdit(bc,proj,file,pid,doc);
	    probs = bc.getPrivateProblems(filename,pid);
	    bc.beginPrivateEdit(filename,pid);		// undo and wait
	    qf.undoEdit(bc,proj,file,pid,doc);
	    bc.getPrivateProblems(filename,pid);
	    if (probs == null || getErrorCount(probs) >= probct) continue;
	    if (checkAnyProblemPresent(for_problem,probs,0,qf.getDelta())) continue;
	    if (usefix != null) return null;
	    usefix = qf;
	  }
       }
      finally {
	 bc.removePrivateBuffer(proj,filename,pid);
       }

      if (usefix == null) return null;
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

private class QuoteDoer implements RunnableFix {

   private BfixCorrector for_corrector;
   private BumpProblem for_problem;
   private QuoteFix using_fix;
   private long initial_time;

   QuoteDoer(BfixCorrector corr,BumpProblem bp,QuoteFix qf,long t0) {
      for_corrector = corr;
      for_problem = bp;
      using_fix = qf;
      initial_time = t0;
    }

   @Override public void run() {
      BumpClient bc = BumpClient.getBump();
      BaleWindow win = for_corrector.getEditor();
      BaleWindowDocument doc = win.getWindowDocument();
      List<BumpProblem> probs = bc.getProblems(doc.getFile());
      if (!checkProblemPresent(for_problem,probs)) return;
      if (for_corrector.getStartTime() != initial_time) return;
      if (!checkSafePosition(for_corrector,using_fix.getStart(),using_fix.getEnd())) return;
      
      BoardMetrics.noteCommand("BFIX","QuoteCorrection");
      
      using_fix.makeEdit(doc);
      BoardMetrics.noteCommand("BFIX","DoneQuoteCorrection");
    }

   @Override public double getPriority()                { return 0; }
   
}	// end of inner class QuoteDoer


}	// end of class BfixAdapterQuotes




/* end of BfixAdapterQuotes.java */

