/********************************************************************************/
/*										*/
/*		BfixAdapterSyntax.java						*/
/*										*/
/*	description of class							*/
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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;


class BfixAdapterSyntax extends BfixAdapter implements BfixConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static Map<BfixErrorPattern,String> syntax_patterns;
private static Pattern CONTAINS_TOKEN;

static {
   CONTAINS_TOKEN = Pattern.compile("[A-Z]");
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BfixAdapterSyntax()
{
   super("Syntax fixer");
   
   if (syntax_patterns == null) {
      syntax_patterns = new LinkedHashMap<>();
      Element xml = BumpClient.getBump().getLanguageData();
      Element fxml = IvyXml.getChild(xml,"FIXES");
      for (Element cxml : IvyXml.children(fxml,"SYNTAX")) {
         String typ = IvyXml.getAttrString(fxml,"TYPE");
         if (typ != null) {
            syntax_patterns.put(new BfixErrorPattern(cxml),typ);
          }
       }
    }
}




/********************************************************************************/
/*										*/
/*	Abstract Method Implementations 					*/
/*										*/
/********************************************************************************/

@Override void addFixers(BfixCorrector corr,BumpProblem bp,boolean explicit,List<BfixFixer> rslt)
{
   if (trySyntaxProblem(corr,bp)) {
      BoardLog.logD("BFIX","Handle syntax problem " + bp.getMessage());
      SyntaxFixer sf = new SyntaxFixer(corr,bp);
      rslt.add(sf);
    }
}



@Override String getMenuAction(BfixCorrector corr,BumpProblem bp)
{
   if (trySyntaxProblem(corr,bp)) return "syntax error";

   return null;
}




/********************************************************************************/
/*										*/
/*	Check if a syntax problem fixup is appropriate				*/
/*										*/
/********************************************************************************/

private boolean trySyntaxProblem(BfixCorrector corr,BumpProblem bp)
{
   if (bp.getErrorType() != BumpErrorType.ERROR) return false;
   String msg = bp.getMessage();
   boolean fnd = false;
   for (BfixErrorPattern pat : syntax_patterns.keySet()) {
      if (pat.testMatch(msg)) {
         fnd = true;
         break;
       }
    }
   if (!fnd) return false;

   BaleWindow win = corr.getEditor();
   BaleWindowDocument doc = win.getWindowDocument();

   int soff = doc.mapOffsetToJava(bp.getStart());
   int eoff = doc.mapOffsetToJava(bp.getEnd());
   int lnoerr = doc.findLineNumber(soff);
   int lnocur = doc.findLineNumber(corr.getCaretPosition());

   BaleWindowElement elt = doc.getCharacterElement(soff);
   int eloff = elt.getEndOffset();
   if (eoff + 1 != eloff) return false;
   if (corr.getEndOffset() > 0 && eloff + 1 >= corr.getEndOffset()) return false;
   if (lnoerr == lnocur) return false;

   String txt = doc.getWindowText(soff,eoff-soff+1);
   if (txt == null) return false;

   return true;
}



/********************************************************************************/
/*										*/
/*	Syntax error fixer							*/
/*										*/
/********************************************************************************/


private class SyntaxFixer extends BfixFixer {

   private long initial_time; 

   SyntaxFixer(BfixCorrector corr,BumpProblem bp) {
      super(corr,bp);
      initial_time = corr.getStartTime();
    }

   @Override protected RunnableFix findFix() {
      String msg = for_problem.getMessage();
      int soff = for_problem.getStart();
      int eoff = for_problem.getEnd()+1;
      String ins = null;
      
      BfixErrorPattern usepat = null;
      for (Map.Entry<BfixErrorPattern,String> ent : syntax_patterns.entrySet()) {
         BfixErrorPattern pat = ent.getKey();
         String what = pat.getMatchResult(msg);
         if (what == null) continue;
         switch (ent.getValue()) {
            case "INSERT" :
              ins = what;
              soff = eoff;
              break;
            case "BEFORE" :
               ins = what;
               eoff = soff;
               break;
            case "DELETE" :
               ins = null;
               break;
            case "REPLACE" :
               ins = what;
               break;
          }
         usepat = pat;
         break;     
       }
      if (usepat == null) return null;

      // ensure we cover complete input token
      if (ins != null && ins.length() == 2 && eoff-soff == 1) {
         String tok = usepat.getAltResult(msg);
	 if (ins.startsWith(tok)) ++eoff;
       }

      // ignore if replacement is much shorter than original
      if (ins != null && ins.length() < eoff-soff-2) return null;
      // ignore if removing an actual keyword
      if (ins == null && eoff-soff >= 3) return null;

      // ignore if suggesting a token type
      if (ins != null) {
	 Matcher m = CONTAINS_TOKEN.matcher(ins);
	 if (m.find()) return null;
       }
      // ignore if right brace -- eclipse usually gets this wrong
      if (ins != null && ins.equals("}")) return null;

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

	 bc.beginPrivateEdit(filename,pid);
	 BoardLog.logD("BFIX","SPELL: Try syntax edit " + soff + "," + eoff + "," + ins);
	 int edelta = soff-eoff;
	 if (ins != null) edelta += ins.length();
	 bc.editPrivateFile(proj,file,pid,soff,eoff,ins);
	 probs = bc.getPrivateProblems(filename,pid);
	 bc.beginPrivateEdit(filename,pid);		// undo and wait
	 if (probs == null || getErrorCount(probs) >= probct) return null;
	 if (checkAnyProblemPresent(for_problem,probs,0,edelta)) return null;
       }
      finally {
	 bc.removePrivateBuffer(proj,filename,pid);
       }

      if (for_corrector.getStartTime() != initial_time) return null;
      BoardLog.logD("BFIX","SPELL: DO syntax edit");
      BoardMetrics.noteCommand("BFIX","SYNTAXFIX");
      SyntaxDoer sd = new SyntaxDoer(for_corrector,for_problem,soff,eoff,ins,initial_time);
      return sd;
    }

}	// end of inner class SyntaxFixer




/********************************************************************************/
/*										*/
/*	Class to fix the syntax error						*/
/*										*/
/********************************************************************************/

private class SyntaxDoer implements RunnableFix {

   private BfixCorrector for_corrector;
   private BumpProblem for_problem;
   private int edit_start;
   private int edit_end;
   private String insert_text;
   private long initial_time;

   SyntaxDoer(BfixCorrector corr,BumpProblem bp,int soff,int eoff,String txt,long t0) {
      for_corrector = corr;
      for_problem = bp;
      edit_start = soff;
      edit_end = eoff;
      insert_text = txt;
      initial_time = t0;
    }

   @Override public Boolean call() {
      BumpClient bc = BumpClient.getBump();
      BaleWindow win = for_corrector.getEditor();
      BaleWindowDocument doc = win.getWindowDocument();
      List<BumpProblem> probs = bc.getProblems(doc.getFile());
      if (!checkProblemPresent(for_problem,probs)) return false;
      if (for_corrector.getStartTime() != initial_time) return false;
      int soff = doc.mapOffsetToJava(edit_start);
      int eoff = doc.mapOffsetToJava(edit_end);
      if (!checkSafePosition(for_corrector,edit_start,edit_end)) return false;

      BoardLog.logD("BFIX","SYNTAX: make fix using " + insert_text);

      BoardMetrics.noteCommand("BFIX","SyntaxCorrection_" + for_corrector.getBubbleId());
      doc.replace(soff,eoff-soff,insert_text,false,false);
      BoardMetrics.noteCommand("BFIX","DoneSyntaxCorrection_" + for_corrector.getBubbleId());
      
      return true;
    }

   @Override public double getPriority()                { return 0; }
   
}	// end of inner class SyntaxDoer




}	// end of class BfixAdapterSyntax




/* end of BfixAdapterSyntax.java */
