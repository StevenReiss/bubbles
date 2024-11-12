/********************************************************************************/
/*										*/
/*		BstyleFixer.java						*/
/*										*/
/*	Fix a particular set of checkstyle problems				*/
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



package edu.brown.cs.bubbles.bstyle;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.brown.cs.bubbles.bale.BaleConstants.BaleFileOverview;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleWindow;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleWindowDocument;
import edu.brown.cs.bubbles.bfix.BfixAdapter;
import edu.brown.cs.bubbles.bfix.BfixCorrector;
import edu.brown.cs.bubbles.bfix.BfixConstants.BfixRunnableFix;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpProblem;

abstract class BstyleFixer
{



/********************************************************************************/
/*										*/
/*	Return the set of fixers to try 					*/
/*										*/
/********************************************************************************/

static List<BstyleFixer> getStyleFixers()
{
   List<BstyleFixer> rslt = new ArrayList<>();

   rslt.add(new PrecededByWhitespace());
   rslt.add(new ArrayBracketPosition());
   rslt.add(new ClassFinal());
   rslt.add(new EmptyXBlock());

   return rslt;
}


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static final String TYPE_PATTERN = "[A-Za-z0-9_<>]+";
private static final String EXPR_PATTERN = "\\p{Graph}+";
private static final String NAME_PATTERN = "[A-Za-z0-9_]+";
private static final String MODIFIER_PATTERN =
	"public|private|protected|static|final|abstract|transient|volatile|" +
	"native|strict|synchronized";
private static final String WHITE_PATTERN = "\\s+";
private static final String OPT_WHITE_PATTERN = "\\s*";

private static final Pattern ARG_PATTERN = Pattern.compile("\\$([0-9]+)\\$");




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BstyleFixer() {

}


/********************************************************************************/
/*										*/
/*	Methods to fix pattern if possible					*/
/*										*/
/********************************************************************************/

abstract BfixRunnableFix findFix(BfixCorrector bc,BumpProblem bp,boolean explicit);



/********************************************************************************/
/*										*/
/*	Fix simplified code patterns						*/
/*										*/
/********************************************************************************/

protected static Pattern generatePattern(String pat)
{
   return generatePattern(pat,null);
}


protected static Pattern generatePattern(String pat,Matcher m)
{
   int flags = 0;
   String pat0 = pat.replace("$T$",TYPE_PATTERN);
   pat0 = pat0.replace("$E$",EXPR_PATTERN);
   pat0 = pat0.replace("$N$",NAME_PATTERN);
   pat0 = pat0.replace("$M$",MODIFIER_PATTERN);
   pat0 = pat0.replace("$W$",WHITE_PATTERN);
   pat0 = pat0.replace("$w$",OPT_WHITE_PATTERN);

   if (m != null) {
      Matcher m0 = ARG_PATTERN.matcher(pat0);
      while (m0.find()) {
	 String pfx = pat0.substring(0,m0.start());
	 String sfx = pat0.substring(m0.end());
	 String itm = pat0.substring(m0.start(1),m0.end(1));
	 int idx = Integer.parseInt(itm);
	 pat0 = pfx + "\\Q" + m.group(idx) + "\\E" + sfx;
	 m0.reset(pat0);
       }
    }

   if (pat0.contains("\\n") || pat0.contains("$L$")) {
      flags |= Pattern.MULTILINE;
      pat0 = pat0.replace("$L$","");
    }

   return Pattern.compile(pat0,flags);
}



/********************************************************************************/
/*										*/
/*	Runnable Fix for style problems 					*/
/*										*/
/********************************************************************************/

private class StyleDoer implements BfixRunnableFix {

   private BfixCorrector for_corrector;
   private int range_start;
   private int range_end;
   private int edit_start;
   private int edit_end;
   private String insert_text;
   private long initial_time;

   StyleDoer(BfixCorrector corr,BumpProblem bp,int rsoff,int reoff,
	 int soff,int eoff,String txt) {
      for_corrector = corr;
      range_start = rsoff;
      range_end = reoff+1;
      edit_start = soff;
      edit_end = eoff;
      insert_text = txt;
      initial_time = corr.getStartTime();
    }

   @Override public Boolean call() {
      BaleWindow win = for_corrector.getEditor();
      BaleWindowDocument doc = win.getWindowDocument();
      BaleFileOverview view = doc.getBaseWindowDocument();
      if (for_corrector.getStartTime() != initial_time) return false;
      int soff = view.mapOffsetToJava(edit_start);
      int eoff = view.mapOffsetToJava(edit_end);
      if (!BfixAdapter.checkSafePosition(for_corrector,range_start,range_end)) return false;

      BoardLog.logD("BSTYLE","Making style fix " + soff + " " + eoff + " " + insert_text);

      BoardMetrics.noteCommand("BSTYLE","StyleCorrection_" + for_corrector.getBubbleId());
      doc.replace(soff,eoff-soff,insert_text,false,false);
      BoardMetrics.noteCommand("BSTYLE","DoneStyleCorrection_" + for_corrector.getBubbleId());

      return true;
    }

   @Override public double getPriority()			{ return 0; }

}	// end of inner class StyleDoer



/********************************************************************************/
/*										*/
/*     GenericPatternFixer -- generic pattern based fixer			*/
/*										*/
/********************************************************************************/

abstract static class GenericPatternFixer extends BstyleFixer {

   private Pattern error_pattern;
   private String code_pattern;

   GenericPatternFixer(String p1,String p2) {
      error_pattern = generatePattern("Style: " + p1 + "\\.");
      code_pattern = p2;
    }

   @Override BfixRunnableFix findFix(BfixCorrector corr,BumpProblem bp,boolean explicit) {
      Matcher m0 = error_pattern.matcher(bp.getMessage());
      if (!m0.find()) return null;
      BaleWindow win = corr.getEditor();
      BaleWindowDocument doc = win.getWindowDocument();
      int lsoff = doc.findLineOffset(bp.getLine());
      int leoff = doc.findLineOffset(bp.getLine()+1);
      String text = doc.getWindowText(lsoff,leoff-lsoff);
      Pattern find = generatePattern(code_pattern,m0);
      Matcher m1 = find.matcher(text);
      if (!m1.find()) return null;
      return buildFix(corr,bp,lsoff,m1);
    }

   protected abstract BfixRunnableFix buildFix(BfixCorrector corr,BumpProblem bp,
	 int lsoff,Matcher m1);

}	// end of inner class GenericPatternFixer



/********************************************************************************/
/*										*/
/*	Sample Fix: 'X' is preceded by whitespace                               */
/*										*/
/********************************************************************************/

private static class PrecededByWhitespace extends GenericPatternFixer {

   PrecededByWhitespace() {
      super("'([^']+)' is preceded with whitespace","(\\s+)$1$");
    }

   @Override protected BfixRunnableFix buildFix(BfixCorrector corr,BumpProblem bp,int lsoff,Matcher m1) {
      int s0 = m1.start() + lsoff;
      int e0 = m1.end(1) + lsoff;
      int re0 = m1.end() + lsoff;
      return new StyleDoer(corr,bp,s0,re0,s0,e0,null);
    }

}	// end of inner class PrecededByWhitespace


private static class ArrayBracketPosition extends GenericPatternFixer {

   ArrayBracketPosition() {
      super("Array brackets at illegal position","$T$(\\s+)($N$)(\\s*)\\[\\]");
    }

   @Override protected BfixRunnableFix buildFix(BfixCorrector corr,BumpProblem bp,int lsoff,Matcher m1) {
      int s0 = m1.start(1);
      int e0 = m1.end();
      int re0 = m1.start();
      String txt = "[] " + m1.group(2);
      return new StyleDoer(corr,bp,s0,e0,re0,e0,txt);
    }

}	// end of inner class ArrayBracketPosition


private static class ClassFinal extends GenericPatternFixer {

   ClassFinal() {
      super("Class ($N$) should be declared final","class $1$");
    }

   @Override protected BfixRunnableFix buildFix(BfixCorrector corr,BumpProblem bp,int lsoff,Matcher m1) {
      int s0 = m1.start();
      int e0 = m1.start();
      int rs0 = m1.start();
      int re0 = m1.end();
      String txt = "final ";
      return new StyleDoer(corr,bp,s0,e0,rs0,re0,txt);
    }

}	// end of inner class ClassFinal


private static class EmptyXBlock extends GenericPatternFixer {

   EmptyXBlock() {
      super("Empty ($N$) block","$1$\\s*($E$)\\s+(\\{\\s*\\})");
    }

   @Override protected BfixRunnableFix buildFix(BfixCorrector corr,BumpProblem bp,int lsoff,Matcher m1) {
      int s0 = m1.start(2);
      int e0 = m1.start(2);
      int rs0 = m1.start();
      int re0 = m1.end();
      String txt = ";";
      return new StyleDoer(corr,bp,s0,e0,rs0,re0,txt);
    }

}	// end of innter class EmptyXBlock



}	// end of class BstyleFixer




/* end of BstyleFixer.java */

