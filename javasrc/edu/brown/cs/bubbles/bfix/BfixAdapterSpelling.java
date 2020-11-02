/********************************************************************************/
/*										*/
/*		BfixAdapterSpelling.java					*/
/*										*/
/*	Handle spelling correction						*/
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
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;

import edu.brown.cs.ivy.file.IvyStringDiff;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;


class BfixAdapterSpelling extends BfixAdapter implements BfixConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private int min_size;
private int explicit_size;
private static List<SpellProblem> spell_problems;

static {
   BoardProperties props = BoardProperties.getProperties("Bfix");
   spell_problems = new ArrayList<SpellProblem>();
   for (String s : props.stringPropertyNames()) {
      if (s.startsWith("Bfix.correct.spelling.problem")) {
	 String txt = props.getString(s,null);
	 StringTokenizer tok = new StringTokenizer(txt,";");
	 while (tok.hasMoreTokens()) {
	    String desc = tok.nextToken().trim();
	    SpellProblem sp = new SpellProblem(desc);
	    spell_problems.add(sp);
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BfixAdapterSpelling()
{
   super("Spelling correction");
   BoardProperties props = BoardProperties.getProperties("Bfix");
   min_size = props.getInt("Bfix.correct.spelling.edits",2);
   explicit_size = props.getInt("Bfix.correct.spelling.user",min_size);
   spell_problems = new ArrayList<SpellProblem>();
   for (String s : props.stringPropertyNames()) {
      if (s.startsWith("Bfix.correct.spelling.problem")) {
	 String txt = props.getString(s,null);
	 StringTokenizer tok = new StringTokenizer(txt,";");
	 while (tok.hasMoreTokens()) {
	    String desc = tok.nextToken().trim();
	    SpellProblem sp = new SpellProblem(desc);
	    spell_problems.add(sp);
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Abstract Method Implementations 					*/
/*										*/
/********************************************************************************/

@Override void addFixers(BfixCorrector corr,BumpProblem bp,boolean explict,List<BfixFixer> rslt)
{
   List<String> cands = getSpellingCandidates(corr,bp);
   if (cands == null || cands.size() == 0) return;

   int size = (explict ? explicit_size : min_size);
   for (String txt : cands) {
      SpellFixer sf = new SpellFixer(corr,bp,txt,size);
      rslt.add(sf);
    }
}



@Override String getMenuAction(BfixCorrector corr,BumpProblem bp)
{
   List<String> cands = getSpellingCandidates(corr,bp);

   if (cands == null || cands.size() == 0) return null;

   return cands.get(0);
}


/********************************************************************************/
/*										*/
/*	Get candidates for spelling correction					*/
/*										*/
/********************************************************************************/

List<String> getSpellingCandidates(BfixCorrector corr,BumpProblem bp)
{
   if (bp.getErrorType() != BumpErrorType.ERROR) return null;
   BaleWindowDocument document = corr.getEditor().getWindowDocument();
   BoardLog.logD("BFIX","SPELL: try problem " + bp.getMessage());
   int soff = document.mapOffsetToJava(bp.getStart());
   int eoff = document.mapOffsetToJava(bp.getEnd());
   if (eoff == soff && bp.getMessage().startsWith("Syntax error")) return null;

   BaleWindowElement elt = document.getCharacterElement(soff);
   // need to have an identifier to correct
   if (!elt.isIdentifier()) return null;
   // can't be working on the identifier at this point
   int elstart = elt.getStartOffset();
   int eloff = elt.getEndOffset();
   if (eoff + 1 != eloff && eoff != eloff) return null;
   if (corr.getEndOffset() > 0 && eloff + 1 >= corr.getEndOffset()) return null;

   String txt = document.getWindowText(elstart,eloff-elstart);
   if (txt == null) return null;
   List<String> rslt = new ArrayList<String>();
   rslt.add(txt);

   String txt1 = null;
   BaleWindowElement nelt = elt.getNextCharacterElement();
// if (nelt != null && nelt.getTokenType() != BaleTokenType.LANGLE) {
   if (nelt != null) {
      BaleWindowElement nnelt = nelt.getNextCharacterElement();
      if (nnelt != null && nnelt.isIdentifier() && (nnelt.getStartOffset() - elt.getEndOffset()) == 1) {
	 int neloff = nnelt.getEndOffset();
	 if (corr.getEndOffset() < 0 || neloff + 1 < corr.getEndOffset()) {
	    txt1 = document.getWindowText(elstart,neloff - elstart);
	  }
       }
    }
   if (txt1 != null) rslt.add(txt1);

   return rslt;
}




/********************************************************************************/
/*										*/
/*	Spell fixing worker							*/
/*										*/
/********************************************************************************/

private static class SpellFixer extends BfixFixer {

   private BaleWindowDocument for_document;
   private String for_identifier;
   private long initial_time;
   private int fix_size;

   SpellFixer(BfixCorrector corr,BumpProblem bp,String txt,int min) {
      super(corr,bp);
      for_document = corr.getEditor().getWindowDocument();
      for_identifier = txt;
      initial_time = corr.getStartTime();
      fix_size = min;
    }

   @Override protected String getMemoId()	{ return for_identifier; }

   @Override protected RunnableFix findFix() {
      String proj = for_document.getProjectName();
      File file = for_document.getFile();
      String filename = file.getAbsolutePath();
      Set<SpellFix> totry = new TreeSet<SpellFix>();
      int minsize = Math.min(fix_size, for_identifier.length()-1);
      minsize = Math.min(minsize,(for_identifier.length()+2)/3);
      
      BumpClient bc = BumpClient.getBump();
      Collection<BumpCompletion> cmps = bc.getCompletions(proj,file,-1,for_problem.getStart());
      if (cmps == null) return null;
      for (BumpCompletion bcm : cmps) {
         String txt = bcm.getCompletion();
         if (txt == null || txt.length() == 0) continue;
         double d = IvyStringDiff.stringDiff(for_identifier,txt);
         if (d <= minsize && d > 0) {
            BoardLog.logD("BFIX","SPELL: Consider replacing " + for_identifier + " WITH " + txt);
            SpellFix sf = new SpellFix(for_identifier,txt,d);
            totry.add(sf);
          }
       }
      if (totry.size() == 0) {
         cmps = bc.getCompletions(proj,file,-1,for_problem.getStart()+1);
         if (cmps != null) {
            for (BumpCompletion bcm : cmps) {
               String txt = bcm.getCompletion();
               if (txt == null || txt.length() == 0) continue;
               double d = IvyStringDiff.stringDiff(for_identifier,txt);
               if (d <= minsize && d > 0) {
                  BoardLog.logD("BFIX","SPELL: Consider replacing " + for_identifier + " WITH " + txt);
                  SpellFix sf = new SpellFix(for_identifier,txt,d);
                  totry.add(sf);
                }
             }
          }
       }
      
      String key = for_identifier;
      if (key.length() > 3) {
         int start = 0;
         for (int i = 0; i < 3; ++i) {
            if (Character.isJavaIdentifierPart(key.charAt(i))) ++start;
            else break;
          }
         key = key.substring(0,start) + "*";
         List<BumpLocation> rslt = bc.findTypes(proj,key);
         if (rslt != null) {
            for (BumpLocation bl : rslt) {
               String nm = bl.getSymbolName();
               int idx = nm.lastIndexOf(".");
               if (idx >= 0) nm = nm.substring(idx+1);
               double d = IvyStringDiff.stringDiff(for_identifier,nm);
               if (d <= minsize && d > 0) {
                  BoardLog.logD("BFIX","SPELL: Consider replacing " + for_identifier + " WITH " + nm);
                  SpellFix sf = new SpellFix(for_identifier,nm,d);
                  totry.add(sf);
                }
             }
          }
       }
      Collection<String> keys = for_corrector.getEditor().getKeywords();
      
      // this should be done by finding annotation types
      // keys.add("Override");
      // keys.add("SuppressWarnings");
      
      for (String s : keys) {
         double d = IvyStringDiff.stringDiff(for_identifier,s);
         if (d <= minsize && d > 0) {
            BoardLog.logD("BFIX","SPELL: Consider replacing " + for_identifier + " WITH " + s);
            SpellFix sf = new SpellFix(for_identifier,s,d);
            totry.add(sf);
          }
       }
      
      // remove problematic cases
      for (Iterator<SpellFix> it = totry.iterator(); it.hasNext(); ) {
         SpellFix sf = it.next();
         String txt = sf.getText();
         boolean rem = false;
         for (SpellProblem sp : spell_problems) {
            if (sp.ignore(for_identifier,txt)) {
               rem = true;
               break;
             }
          }
         if (rem) {
            it.remove();
            break;
          }
         // if (for_identifier.equals("put") && txt.equals("get")) it.remove();
         // else if (for_identifier.startsWith("set") && txt.startsWith("get")) it.remove();
         // else if (for_identifier.equals("List") && txt.equals("int")) it.remove();
         // else if (for_identifier.equals("is") && txt.equals("if")) it.remove();
         // else if (txt.equals("if")) it.remove();
         // else if (for_identifier.equals("add") && txt.equals("do")) it.remove();
         // else if (for_identifier.equals("min") && txt.equals("sin")) it.remove();
         // else if (for_identifier.equals(txt + "2D")) it.remove();
         // else if (txt.equals(for_identifier + "2D")) it.remove();
         // else if (for_identifier.equals("IOException")) it.remove();
       }
      
      if (totry.size() == 0) {
         BoardLog.logD("BFIX", "SPELL: No spelling correction found");
         return null;
       }
      
      String pid = createPrivateBuffer(proj,filename);
      if (pid == null) return null;
      BoardLog.logD("BFIX","SPELL: using private buffer " + pid);
      SpellFix usefix = null;
      BoardMetrics.noteCommand("BFIX","SpellCheck_" + totry.size());
      SpellFix badfix = null;
      boolean havebad = false;
      
      try {
         Collection<BumpProblem> oprobs = bc.getPrivateProblems(filename,pid);
         if (oprobs == null) {
            BoardLog.logE("BFIX","SPELL: Problem getting errors for " + pid);
            return null;
          }
         int probct = getErrorCount(oprobs,for_problem);
         if (!checkProblemPresent(for_problem,oprobs)) {
            BoardLog.logD("BFIX","SPELL: Spelling problem went away");
            return null;
          }
         int soff = for_problem.getStart();
         int eoff = soff + for_identifier.length();
         
         for (SpellFix sf : totry) {
            bc.beginPrivateEdit(filename,pid);
            BoardLog.logD("BFIX","SPELL: Try replacing " + for_identifier + " WITH " + sf.getText());
            bc.editPrivateFile(proj,file,pid,soff,eoff,sf.getText());
            Collection<BumpProblem> probs = bc.getPrivateProblems(filename,pid);
            bc.beginPrivateEdit(filename,pid);		// undo and wait
            bc.editPrivateFile(proj,file,pid,soff,soff+sf.getText().length(),for_identifier);
            bc.getPrivateProblems(filename,pid);
            
            int edelta = sf.getText().length() - for_identifier.length();
            if (checkAnyProblemPresent(for_problem,probs,0,edelta)) continue;
            if (checkAnyProblemPresent(probs,for_problem.getFile(),soff,eoff+edelta)) continue;
            if (probs == null || getErrorCount(probs) >= probct) continue;
            else if (getErrorCount(probs) == probct) {
               double score = sf.getEditCount();
               score /= for_identifier.length();
               if (score < 0.1) {
                  if (havebad) badfix = null;
                  else {
                     havebad = true;
                     badfix = sf;
                   }
                }
               continue;
             }
            
            if (usefix != null) {
               if (sf.getEditCount() > usefix.getEditCount()) break;
               // multiple edits of same length seem to work out -- ignore.
               return null;
             }
            else usefix = sf;
          }
       }
      finally {
         bc.removePrivateBuffer(proj,filename,pid);
       }
      if (havebad && badfix != null && usefix == null) usefix = badfix;
      
      if (usefix == null) return null;
      if (for_corrector.getStartTime() != initial_time) return null;
      BoardLog.logD("BFIX","SPELL: DO replace " + for_identifier + " WITH " + usefix.getText());
      BoardMetrics.noteCommand("BFIX","SPELLFIX");
      SpellDoer sd = new SpellDoer(for_corrector,for_document,for_problem,usefix,initial_time);
      return sd;
   }

}	// end of class BfixAdapterSpelling



/********************************************************************************/
/*										*/
/*	SpellFix -- potential spelling fixup					*/
/*										*/
/********************************************************************************/

private static class SpellFix implements Comparable<SpellFix> {

   private String original_text;
   private String new_text;
   private double text_delta;

   SpellFix(String orig,String txt,double d) {
      original_text = orig;
      new_text = txt;
      text_delta = d;
    }

   String getText()			{ return new_text; }
   String getOriginalText()		{ return original_text; }
   double getEditCount()		{ return text_delta; }

   @Override public int compareTo(SpellFix sf) {
      double d = getEditCount() - sf.getEditCount();
      if (d < 0) return -1;
      if (d > 0) return 1;
      return new_text.compareTo(sf.new_text);
    }

}	// end of inner class SpellFix



/********************************************************************************/
/*										*/
/*	Class to perform the spelling correction				*/
/*										*/
/********************************************************************************/

private static class SpellDoer implements RunnableFix {

   private BfixCorrector for_corrector;
   private BaleWindowDocument for_document;
   private BumpProblem for_problem;
   private SpellFix for_fix;
   private long initial_time;

   SpellDoer(BfixCorrector cor,BaleWindowDocument doc,
	 BumpProblem bp,SpellFix fix,long time) {
      for_corrector = cor;
      for_document = doc;
      for_problem = bp;
      for_fix = fix;
      initial_time = time;
    }

   @Override public void run() {
      BumpClient bc = BumpClient.getBump();
      List<BumpProblem> probs = bc.getProblems(for_document.getFile());
      if (!checkProblemPresent(for_problem,probs)) return;
      if (for_corrector.getStartTime() != initial_time) return;
   
      int soff = for_document.mapOffsetToJava(for_problem.getStart());
      int eoff0 = for_document.mapOffsetToJava(for_problem.getEnd());
      if (!checkSafePosition(for_corrector,soff,eoff0)) return;
   
      BoardMetrics.noteCommand("BFIX","SpellingCorrection_" + for_corrector.getBubbleId());
      BoardLog.logD("BFIX","SPELL Making correction " + for_fix.getText() + " for " + for_fix.getOriginalText());
   
      int len = for_fix.getOriginalText().length();
      int eoff = soff+len-1;
      String txt = for_fix.getText();
   
      for_document.replace(soff,eoff-soff+1,txt,false,false);
      BoardMetrics.noteCommand("BFIX", "DoneSpellingCorrection_" + for_corrector.getBubbleId());
    }

   @Override public double getPriority()                { return 0; }
   
}	// end of inner class SpellDoer



/********************************************************************************/
/*										*/
/*	Spelling problem cases							*/
/*										*/
/********************************************************************************/

private static class SpellProblem {

   private String original_identifier;
   private String target_identifier;
   private boolean starts_with;
   private String using_suffix;

   SpellProblem(String desc) {
      desc = desc.trim();
      if (desc.startsWith("^")) {
         starts_with = true;
         desc = desc.substring(1);
       }
      else starts_with = false;
      original_identifier = null;
      target_identifier = null;
      using_suffix = null;
      StringTokenizer tok = new StringTokenizer(desc,",");
      if (tok.hasMoreTokens()) {
         original_identifier = tok.nextToken().trim();
         if (original_identifier.equals("*")) original_identifier = null;
       }
      if (tok.hasMoreTokens()) {
         target_identifier = tok.nextToken().trim();
         if (target_identifier.equals("*")) target_identifier = null;
       }
      if (tok.hasMoreTokens()) {
         using_suffix = tok.nextToken().trim();
         if (using_suffix.equals("*")) using_suffix = null;
       }
    }

   boolean ignore(String orig,String txt) {
      if (original_identifier != null) {
         if (starts_with && !orig.startsWith(original_identifier)) return false;
         else if (!starts_with && !orig.equals(original_identifier)) return false;
       }
      if (target_identifier != null) {
         if (starts_with && !txt.startsWith(target_identifier)) return false;
         if (!starts_with && !txt.equals(target_identifier)) return false;
       }
      if (using_suffix != null) {
         if (!orig.equals(txt + using_suffix) && !txt.equals(orig + using_suffix))
            return false;
       }
      return true;
    }

   @Override public String toString() {
      StringBuffer buf = new StringBuffer();
      if (starts_with) buf.append("^");
      if (original_identifier != null) buf.append(original_identifier);
      else buf.append("*");
      if (target_identifier != null) {
         buf.append(",");
         buf.append(target_identifier);
       }
      else if (using_suffix != null) buf.append(",*");
      if (using_suffix != null) {
         buf.append(",");
         buf.append(using_suffix);
       }
      return buf.toString();
    }

}	// end of inner class SpellProblem



}	// end of class BfixAdapterSpelling


/* end of BfixAdapterSpelling.java */

