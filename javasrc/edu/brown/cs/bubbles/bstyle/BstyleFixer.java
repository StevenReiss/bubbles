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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static final String TYPE_PATTERN = "[A-Za-z0-9_<>]+";
private static final String EXPR_PATTERN = "\\p{Print}+";
private static final String NAME_PATTERN = "[A-Za-z0-9_\\.]+";
private static final String LHS_PATTERN = "[^ \\t\\n=]+";
private static final String MODIFIER_PATTERN =
	"public|private|protected|static|final|abstract|transient|volatile|" +
	"native|strict|synchronized";
private static final String WHITE_PATTERN = "\\s+";
private static final String OPT_WHITE_PATTERN = "\\s*";
private static final String BOOLEAN_PATTERN = "true|false";

private static final Pattern ARG_PATTERN = Pattern.compile("\\$([0-9]+)\\$");

private static List<BstyleFixer> style_fixers;
private static Map<String,List<BstyleFixer>> fixers_map;
private static Set<String> ignore_errors;

static {
   style_fixers = new ArrayList<>();
   fixers_map = new HashMap<>();
   ignore_errors = new HashSet<>();
   
   ignore_errors.add("MemberName");
   ignore_errors.add("HiddenField");
   ignore_errors.add("StaticVariableName");
   ignore_errors.add("AvoidStarImport");
   ignore_errors.add("LineLength");

   BstyleFixer precededbywhite = new PrecededByWhitespace();
   BstyleFixer notfollowedbywhite = new NotFollowedByWhite();
   BstyleFixer previousline = new PreviousLine();
   BstyleFixer followedbywhite = new FollowedByWhitespace();

   addFixer("NoWhitespaceBefore",precededbywhite);
   addFixer("NoWhitespaceAfter",followedbywhite);
   addFixer("ArrayTypeStyle",new ArrayBracketPosition());
   addFixer("FinalClass",new ClassFinal());
   addFixer("EmptyBlock",new EmptyXBlock());
   addFixer("NewlineAtEndOfFile",new FileNewline());
   addFixer("ModifierOrder",new ModifierOrder());
   addFixer("ParenPad",followedbywhite,precededbywhite);
   addFixer("MethodParenPad",precededbywhite);
   addFixer("InnerAssignment",new InnerAssignments());
   addFixer("UpperEll",new UppercaseL());
   addFixer("SimplifyBooleanExpression",
         new ExpressionSimplified(),
         new ExpressionAndTrue());
   addFixer("RightCurly",new LineByItself());
   addFixer("LeftCurly",new LineBreakAfter(),previousline);
   addFixer("WhitespaceAfter",new CastNotFollowedByWhite(),notfollowedbywhite);
   addFixer("EmptyForIteratorPad",notfollowedbywhite);
   addFixer("TypecastParenPad",precededbywhite);
   addFixer("NeedBraces",new MustUseBraces());
   addFixer("MultipleVariableDeclarations",new OnlyOneDefinition());
   addFixer("RedundantModifier",new RedundantModifier());
   addFixer("OperatorWrap",previousline);
   addFixer("SimplifyBooleanReturn",new ConditionalLogic());
   addFixer("MultipleVariableDeclarations",new VarDeclarations());
   addFixer("GenericWhitespace",followedbywhite,precededbywhite);
}


private static void addFixer(String when,BstyleFixer... fixers)
{
   for (BstyleFixer f : fixers) {
      if (when != null) {
         StringTokenizer tok = new StringTokenizer(when);
         while (tok.hasMoreTokens()) {
            String w = tok.nextToken();
            List<BstyleFixer> bfl = fixers_map.get(w);
            if (bfl == null) {
               bfl = new ArrayList<>();
               fixers_map.put(w,bfl);
             }
            bfl.add(f);
            ignore_errors.remove(w);
          }
         if (!style_fixers.contains(f)) {
            style_fixers.add(f);
          }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BstyleFixer() { }



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/


/********************************************************************************/
/*										*/
/*	Return the set of fixers to try 					*/
/*										*/
/********************************************************************************/

static List<BstyleFixer> getStyleFixers()
{
   return style_fixers;
}

static Map<String,List<BstyleFixer>> getStyleFixerMap() 
{
   return fixers_map;
}

static Set<String> getIgnoreErrors()
{
   return ignore_errors;
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
   if (pat == null) return null;
   
   int flags = 0;
   String pat0 = pat.replace("$T$",TYPE_PATTERN);
   pat0 = pat0.replace("$E$",EXPR_PATTERN);
   pat0 = pat0.replace("$N$",NAME_PATTERN);
   pat0 = pat0.replace("$M$",MODIFIER_PATTERN);
   pat0 = pat0.replace("$W$",WHITE_PATTERN);
   pat0 = pat0.replace("$w$",OPT_WHITE_PATTERN);
   pat0 = pat0.replace("$B$",BOOLEAN_PATTERN);
   pat0 = pat0.replace("$L$",LHS_PATTERN);

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

   try {
      return Pattern.compile(pat0,flags);
    }
   catch (PatternSyntaxException e) {
      BoardLog.logE("BSTYLE","Bad regex pattern " + pat + " = " + pat0,e);
      return Pattern.compile(Pattern.quote("*IGNORE ME -- BAD PATTERN*"));
    }
}




/********************************************************************************/
/*										*/
/*     GenericPatternFixer -- generic pattern based fixer			*/
/*										*/
/********************************************************************************/

abstract static class GenericPatternFixer extends BstyleFixer {

   private Pattern error_pattern;
   private Pattern data_pattern;
   private String code_pattern;
   private boolean explicit_only;
   GenericPatternFixer(String p1,String p2) {
      this(p1,p2,false);
    }
   
   GenericPatternFixer(String p1,String p2,boolean explicit) {
      if (p1.contains(" ")) {
         error_pattern = generatePattern("Style: " + p1 + "\\.");
         data_pattern = null;
       }
      else {
         data_pattern = Pattern.compile(Pattern.quote(p1));
         error_pattern = null;
       }
      code_pattern = p2;
      explicit_only = explicit;
    }

   @Override BfixRunnableFix findFix(BfixCorrector corr,BumpProblem bp,boolean explicit) {
      Matcher m0 = useFix(bp,explicit);
      if (m0 == null) {
         BoardLog.logD("BSTYLE","No match for " + this.getClass());
         return null;
       }
      
      BoardLog.logD("BSTYLE","Match error message for problem " + this.getClass());
      
      BaleWindow win = corr.getEditor();
      BaleWindowDocument doc = win.getWindowDocument();
      int l0 = getStartLine(bp.getLine());
      int l1 = getEndLine(bp.getLine());
      int lsoff = doc.findLineOffset(l0);
      int leoff = doc.findLineOffset(l1+1);
      String text = doc.getWindowText(lsoff,leoff-lsoff);
      Pattern find = generatePattern(code_pattern,m0);
   
      BoardLog.logD("BSTYLE","Use pattern " + find);
      Matcher m1 = null;
      if (find != null) {
         m1 = find.matcher(text);
         if (!m1.find()) {
            BoardLog.logD("BSTYLE","Pattern doesn't match " + text);
            return null;
          }
         if (!checkStillApplicable(text)) return null;
       }
      return buildFix(corr,bp,lsoff,m1);
    }
   
   protected Matcher useFix(BumpProblem bp,boolean explicit) {
      if (explicit_only && !explicit) return null;
      Matcher m0 = null;
      if (error_pattern != null) {
         m0 = error_pattern.matcher(bp.getMessage());
       }
      else if (data_pattern != null) {
         m0 = data_pattern.matcher(bp.getData());
       }
      else return null;
      if (!m0.find()) return null;
      return m0;
    }

   protected BfixRunnableFix buildFix(BfixCorrector corr,BumpProblem bp,
         int lsoff,Matcher m1) {
      String txt = getEditReplace(m1);
      if (txt == null) return null;
      if (txt.isEmpty()) txt = null;
      int s0 = getEditStart(m1)+lsoff;
      int e0 = getEditEnd(m1)+lsoff;
      int rs0 = getCheckStart(m1)+lsoff;
      int re0 = getCheckEnd(m1)+lsoff;
      return new StyleDoer(corr,bp,rs0,re0,s0,e0,txt);
    }
   
   protected String getEditReplace(Matcher m1)          { return ""; }
   protected int getEditStart(Matcher m1)               { return m1.start(); }
   protected int getEditEnd(Matcher m1)                 { return m1.end(); }
   protected int getCheckStart(Matcher m1)              { return m1.start(); }
   protected int getCheckEnd(Matcher m1)                { return m1.end(); }
   protected int getStartLine(int line)                 { return line; }
   protected int getEndLine(int line)                   { return line; } 
   protected boolean checkStillApplicable(String line)  { return true; }
   
}	// end of inner class GenericPatternFixer






/********************************************************************************/
/*										*/
/*	Various fix classes                                                     */
/*										*/
/********************************************************************************/

private static class PrecededByWhitespace extends GenericPatternFixer {

   PrecededByWhitespace() {
      super("'([^']+)' is preceded with whitespace","(\\s+)$1$");
    }

   @Override protected int getEditEnd(Matcher m1)       { return m1.end(1); }
   
}	// end of inner class PrecededByWhitespace


private static class ArrayBracketPosition extends GenericPatternFixer {

   ArrayBracketPosition() {
      super("Array brackets at illegal position","$T$(\\s+)($N$)(\\s*)\\[\\]");
    }
   
   @Override protected int getEditStart(Matcher m1)     { return m1.start(1); }
   @Override protected String getEditReplace(Matcher m1) {
      return " [] " + m1.group(2);
    }

}	// end of inner class ArrayBracketPosition


private static class ClassFinal extends GenericPatternFixer {

   ClassFinal() {
      super("Class ($N$) should be declared as final","class $1$");
    }

   @Override protected int getEditEnd(Matcher m1)       { return m1.start(); }
   @Override protected String getEditReplace(Matcher m) { return "final "; }
   @Override protected boolean checkStillApplicable(String line) {
      if (line.contains("final")) return false;
      return true;
    }

}	// end of inner class ClassFinal


private static class EmptyXBlock extends GenericPatternFixer {

   EmptyXBlock() {
      super("Empty ($N$) block","$1$\\s*\\(($E$)\\)\\s+(\\{\\s*\\})");
    }
   
   @Override protected int getEditStart(Matcher m)      { return m.start(2); }
   @Override protected int getEditEnd(Matcher m)        { return m.end(2); }
   @Override protected String getEditReplace(Matcher m) { return ";"; }

}	// end of innter class EmptyXBlock


private static class FileNewline extends GenericPatternFixer {

   FileNewline() {
      super("NewlineAtEndOfFile",null);
    }
   
   @Override BfixRunnableFix findFix(BfixCorrector corr,BumpProblem bp,boolean explicit) {
      Matcher m0 = useFix(bp,explicit);
      if (m0 == null) return null;
      
      BoardLog.logD("BSTYLE","Looking at file missing new line");
      BaleWindow win = corr.getEditor();
      BaleWindowDocument doc = win.getWindowDocument();
      BaleFileOverview doc0 = doc.getBaseWindowDocument();
      int off = doc0.getLength();
      return new StyleDoer(corr,bp,off,off,off,off,"\n");
    }
   
}       // end of inner class FileNewline




private static final String [] MOD_ORDER = {
   "public", "protected", "private", "abstract",
   "default", "static", "sealed", "non-sealed",
   "final", "transient", "volatile", "synchronized",
   "native", "strictfp" 
};


private static class ModifierOrder extends GenericPatternFixer {

   ModifierOrder() {
      super("'$N$' modifier out of order with the JLS suggestions","((($M$)\\s+)+)\\S");
    }
   
   @Override protected int getEditStart(Matcher m)      { return m.start(1); }
   @Override protected int getEditEnd(Matcher m)        { return m.end(1); }
   
   @Override protected String getEditReplace(Matcher m) {
      String modstr = m.group(1);
      Set<String> mods = new HashSet<>();
      StringTokenizer tok = new StringTokenizer(modstr);
      while (tok.hasMoreTokens()) {
         mods.add(tok.nextToken());
       }
      StringBuffer buf = new StringBuffer();
      for (String s : MOD_ORDER) {
         if (mods.contains(s)) {
            buf.append(s);
            buf.append(" ");
          }
       }
      
      BoardLog.logD("BSTYLE","MODIFIER EDIT REPLACE " + modstr + " " +
            mods.size() + " " + buf.toString());
      
      return buf.toString();
    }
   
}       // end of inner class ModifierOrder



private static class FollowedByWhitespace extends GenericPatternFixer {

   FollowedByWhitespace() {
      super("'([^']+)' is followed by whitespace","$1$(\\s+)");
    }
   
   @Override protected int getEditStart(Matcher m)      { return m.start(1); }

}       // end of inner class FollowedByWhitespace


private static class InnerAssignments extends GenericPatternFixer {

   InnerAssignments() {
      super("Inner assignments should be avoided","^(\\s*)(($N$\\s*\\=\\s*)+)($E$)\\;");
    }
   
   @Override protected int getEditStart(Matcher m)      { return m.start(2); }
   @Override protected int getEditEnd(Matcher m)        { return m.end(4); }
   @Override protected int getCheckStart(Matcher m)     { return m.start(2); }
   @Override protected String getEditReplace(Matcher m) {
      String ind = m.group(1);
      String asgstr = m.group(2);
      String expr = m.group(4);
      List<String> terms = new ArrayList<>();
      StringTokenizer tok = new StringTokenizer(asgstr,"=");
      while (tok.hasMoreTokens()) {
         String term = tok.nextToken().trim();
         if (!term.isEmpty()) terms.add(term);
       }
      
      StringBuffer buf = new StringBuffer();
      if (expr.matches("\\-?[0-9a-zA-Z_\\.]+")) {
         // simple expression
         for (int i = 0; i < terms.size(); ++i) {
            String t = terms.get(i);
            if (i > 0) buf.append(";\n" + ind);
            buf.append(t);
            buf.append(" = ");
            buf.append(expr);
          }
       }
      else {
         String e = expr;
         for (int i = 0; i < terms.size(); ++i) {
            String t = terms.get(i);
            if (i > 0) buf.append(";\n" + ind);
            buf.append(t);
            buf.append(" = ");
            buf.append(e);
            e = t;
          }
       }
      
      BoardLog.logD("BSTYLE","Inner assignment result: " + buf.toString() + " " +
            ind.length() + " " + m.end(4) + " " + expr + " " + terms.size() + " " +
            asgstr);
      
      return buf.toString();
    }
   
   
   
}       // end of inner class InnerAssignments



private static class UppercaseL extends GenericPatternFixer {

   UppercaseL() {
      super("Should use uppercase 'L'","[0-9]+(l)");
    }
   
   @Override protected int getEditStart(Matcher m)      { return m.start(1); }
   @Override protected String getEditReplace(Matcher m) { return "L"; }

}	// end of innter class UppercaseL



private static class ExpressionSimplified extends GenericPatternFixer {

   ExpressionSimplified() {
      super("Expression can be simplified","\\(([^?]+)\\?\\s*($B$)\\s*\\:\\s*($B$)\\s*\\)");
    }
   
   @Override protected String getEditReplace(Matcher m) {
      String expr = m.group(1).trim();
      String btrue = m.group(2);
      String bfalse = m.group(3);
      
      String e = null;
      if (btrue.equals("true") && bfalse.equals("false")) {
         if (expr.trim().startsWith("(")) e = expr;
         else e = "(" + expr + ")";
       }
      else if (btrue.equals("false") && bfalse.equals("true")) {
         if (expr.trim().startsWith("(")) e = "!" + expr;
         else e = "!(" + expr + ")";
       }
      else return null;
      
      return e;
    }
   
}       // end of inner class ExpressionSimplified



private static class ExpressionAndTrue extends GenericPatternFixer {

   ExpressionAndTrue() {
      super("Expression can be simplified","\\s*\\&\\&\\s*true");
    }
   
   @Override protected String getEditReplace(Matcher m) { return ""; }

}       // end of inner class ExpressionSimplified


private static class LineByItself extends GenericPatternFixer {
   
   LineByItself() {
      super("'([^']+)' at column ([0-9]+) should be alone on a line",
            "(\\s*)$1$(\\s*)(\\S)",true);
    }
   
   @Override protected int getEditStart(Matcher m)      { return m.start(2); }
   @Override protected int getEditEnd(Matcher m)        { return m.start(3); }
   @Override protected String getEditReplace(Matcher m) { return "\n"; }
 
}       // end of inner class LineByItself


private static class LineBreakAfter extends GenericPatternFixer {
   
   LineBreakAfter() {
      super("'\\{' at column ([0-9]+) should have line break after",
            "(\\s*)([^{]*)(\\{)(\\s*)([^}]*)\\}",true);
    }
   
   @Override protected int getEditStart(Matcher m)      { return m.start(4); }
   @Override protected String getEditReplace(Matcher m) { 
      String cnts = m.group(5);
      if (cnts != null) cnts = cnts.trim();
      if (cnts == null || cnts.isEmpty()) cnts = "// do nothing";
      return "\n" + m.group(1) + "   " + cnts + "\n" + m.group(1) + "}";
    }
   
}       // end of inner class LineBreakAfter


private static class CastNotFollowedByWhite extends GenericPatternFixer {
   
   CastNotFollowedByWhite() {
      super("'typecast' is not followed by whitespace",
            "\\)([A-Za-z0-9(])");
    }
   @Override protected int getEditStart(Matcher m)      { return m.start(1); }
   @Override protected int getEditEnd(Matcher m)        { return m.start(1); }
   @Override protected String getEditReplace(Matcher m) { return " "; }
   
}       // end of innter class CastNotFollowedByWhite


private static class NotFollowedByWhite extends GenericPatternFixer {
   
   NotFollowedByWhite() {
      super("'([^']+)' is not followed by whitespace",
            "(\\s*)($1$)(\\S)");
    }
   
   @Override protected int getEditStart(Matcher m)      { return m.start(3); }
   @Override protected int getEditEnd(Matcher m)        { return m.start(3); }
   @Override protected String getEditReplace(Matcher m) { return " "; }
 
}       // end of inner class NotFollowedByWhite


private static class MustUseBraces extends GenericPatternFixer {
   
   MustUseBraces() {
      super("'($N$)' construct must use '\\{\\}'s",
            "^(\\h*)$1$\\s*(\\(($E$)\\))\\h*\\n(\\s*$E$)\\;(\\s*)$");
    }
   
   @Override protected int getEndLine(int lno)          { return lno+1; }
   @Override protected int getEditStart(Matcher m)      { return m.end(2); }
   @Override protected int getEditEnd(Matcher m)        { return m.start(4); }
   @Override protected String getEditReplace(Matcher m) {
      return " {\\n" + m.group(3) + ";\\n" + m.group(1) + "}";
    }
   
}       // end of inner class MustUseBraces
      
     
private static class OnlyOneDefinition extends GenericPatternFixer {
   
   OnlyOneDefinition() {
      super("Only one variable definition per line allowed",
            "(\\s*)($T$)(\\s+$N$\\s*[,;])+",true);
    }
   
   @Override protected String getEditReplace(Matcher m) {
      // need to construct declaration with multiple lines
      // also check that the end is with a ;
      return null;
    }
   
}       // end of inner class OnlyOneDefinition
      
      
private static class RedundantModifier extends GenericPatternFixer {
   
   RedundantModifier() {
      super("Redundant '($N$)' modifier",
            "\\s*($1$)(\\s)");
    }
   
   @Override protected int getEditStart(Matcher m)      { return m.start(1); }
   
}       // end of inner class RedundantModifier


private static class PreviousLine extends GenericPatternFixer {
   
   PreviousLine() {
      super("'([^']+)' (at column ([0-9]+ ))?should be on the previous line",
         "\\h*\\n(\\h*)($1$)(\\s*)",true);
    }
   
   @Override protected int getStartLine(int line)       { return line-1; }
   @Override protected int getEditEnd(Matcher m1)       { return m1.end(3); }      
   @Override protected String getEditReplace(Matcher m1) {
      return " " + m1.group(2) + "\n" + m1.group(1);
    }
   
}       // end of inner class PreviousLine
      


private static class ConditionalLogic extends GenericPatternFixer {
   
   ConditionalLogic() {
      super("Conditional logic can be removed",
      "if\\s*($E$)\\s*return\\s+($B$);\\s*else\\s+return\\s+($B$);");
    }
   
   @Override protected String getEditReplace(Matcher m1) {
      String b1 = m1.group(2);
      String b2 = m1.group(3);
      if (b1.equals("true") && b2.equals("false")) {
         return "return " + m1.group(1) + ";";
       }
      else if (b1.equals("false") && b2.equals("true")) {
         return "return !(" + m1.group(1) + ");";
       }
      return null;
    }
   
}       // end of inner class ConditionalLogic


private static class VarDeclarations extends GenericPatternFixer {

   VarDeclarations() {
      super("Each variable declaration must be in its own statement",
            "^(\\s*)(($M$)\\s+)*($T$)\\s+($N$)(\\s*,\\s*$N$)+;");
    }
   
   @Override protected int getEditStart(Matcher m)      { return m.start(6); }
   @Override protected String getEditReplace(Matcher m) {
      StringBuffer buf = new StringBuffer();
      String pfx = m.group(2);
      if (pfx == null) pfx = "";
      pfx += m.group(4);
      
      buf.append(";");
      String others = m.group(6);
      StringTokenizer tok = new StringTokenizer(others,",");
      while (tok.hasMoreTokens()) {
         String var = tok.nextToken().trim();
         buf.append("\n" + m.group(1) + pfx + " " + var + ";");
       }
      
      return buf.toString();
    }
   
}       // end of inner class VarDeclarations



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




}	// end of class BstyleFixer




/* end of BstyleFixer.java */

