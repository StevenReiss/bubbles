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
import edu.brown.cs.ivy.xml.IvyXml;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.w3c.dom.Element;


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
private static List<BfixErrorPattern> ignore_patterns;
private static boolean lookup_types;
private static boolean lookup_keys;

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
   Element xml = BumpClient.getBump().getLanguageData();
   ignore_patterns = new ArrayList<>();
   Element fxml = IvyXml.getChild(xml,"FIXES");
   for (Element cxml : IvyXml.children(fxml,"SPELL")) {
      if (IvyXml.getAttrBool(cxml,"IGNORE")) {
         ignore_patterns.add(new BfixErrorPattern(cxml));
       }
    }
   Element luxml = IvyXml.getChild(fxml,"SPELLLOOKUP");
   if (luxml == null) {
      lookup_types = props.getBoolean("Bfix.correct.spelling.lookup.types");
      lookup_keys = props.getBoolean("Bfix.correct.spelling.lookup.keywords");
    }
   else {
      lookup_types = IvyXml.getAttrBool(luxml,"TYPES");
      lookup_keys = IvyXml.getAttrBool(luxml,"KEYWORDS");
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
   
   if (ignore_patterns == null) {
      ignore_patterns = new ArrayList<>();
      Element xml = BumpClient.getBump().getLanguageData();
      Element fxml = IvyXml.getChild(xml,"FIXES");
      for (Element cxml : IvyXml.children(fxml,"SPELL")) {
         if (IvyXml.getAttrBool(cxml,"IGNORE")) {
            ignore_patterns.add(new BfixErrorPattern(cxml));
          }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Abstract Method Implementations 					*/
/*										*/
/********************************************************************************/

@Override public void addFixers(BfixCorrector corr,BumpProblem bp,boolean explict,List<BfixFixer> rslt)
{
   List<String> cands = getSpellingCandidates(corr,bp);
   if (cands == null || cands.size() == 0) return;

   int size = (explict ? explicit_size : min_size);
   for (String txt : cands) {
      SpellFixer sf = new SpellFixer(corr,bp,txt,size);
      rslt.add(sf);
    }
}



@Override protected String getMenuAction(BfixCorrector corr,BumpProblem bp)
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
   if (eoff == soff) {
      for (BfixErrorPattern pat : ignore_patterns) {
         if (pat.testMatch(bp.getMessage())) return null;
       }
    }

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

   // handle xxx<char>xxx for mistyped char
   String txt1 = null;
   BaleWindowElement nelt = elt.getNextCharacterElement();
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

   // CHECKSTYLE:OFF
   @Override protected BfixRunnableFix findFix() {
   // CHECKSTYLE:ON   
      String proj = for_document.getProjectName();
      File file = for_document.getFile();
      Set<SpellFix> totry = new TreeSet<SpellFix>();
      int minsize = Math.min(fix_size, for_identifier.length()-1);
      minsize = Math.min(minsize,(for_identifier.length()+2)/3);
      boolean checktypes = lookup_types;
      boolean checkkeys = lookup_keys;
      boolean checkcomps = false;
      
      BumpClient bc = BumpClient.getBump();
      Collection<BumpCompletion> cmps = bc.getCompletions(proj,file,-1,for_problem.getStart()+1);
      if (cmps != null) {
         checkcomps = true;
         for (BumpCompletion bcm : cmps) {
            if (bcm.getType() == CompletionType.TYPE_REF) lookup_types = false;
            if (bcm.getType() == CompletionType.KEYWORD) checkkeys = false;
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
      if (totry.size() == 0) {
         cmps = bc.getCompletions(proj,file,-1,for_problem.getStart());
         if (cmps != null) {
            checkcomps = true;
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
      
      if (!checkcomps) return null;
      
      String key = for_identifier;
      if (key.length() > 3 && checktypes) {
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
      
      if (checkkeys) {
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
       }
      
      if (totry.size() == 0) {
         BoardLog.logD("BFIX", "SPELL: No spelling correction found");
         return null;
       }
      
      Map<BfixEdit,SpellFix> edits = new LinkedHashMap<>();
      int soff = for_document.mapOffsetToJava(for_problem.getStart());
      int eoff = soff + for_identifier.length();
      for (SpellFix sf : totry) {
         BfixEdit edit = new BfixBaseEdit(for_corrector,soff,eoff,sf.getText());
         edits.put(edit,sf);
       }
      BfixCheckAreas darea = new BfixCheckAreas(0,-1);
      List<BfixEdit> useedits = findPrivateEdits(edits.keySet(),null,darea);
      if (useedits == null || useedits.size() == 0) return null;
      SpellFix usefix = null;
      for (BfixEdit be : useedits) {
         SpellFix sf = edits.get(be);
         if (usefix != null) {
            BoardLog.logD("BFIX","Multiple spelling corrections " + usefix.getText() +
                  " " + sf.getText() + usefix.getEditCount() + " " + sf.getEditCount());
            if (sf.getEditCount() == usefix.getEditCount()) {
               BoardLog.logD("BFIX","Skip spelling correction due to mutliple edits with same delta");       
               usefix = null;
               break;
             }
          }
         else usefix = sf;
       }
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

private static class SpellDoer extends BfixFixDoer {

   private BaleWindowDocument for_document;
   private SpellFix for_fix;

   SpellDoer(BfixCorrector cor,BaleWindowDocument doc,
         BumpProblem bp,SpellFix fix,long time) {
      super(cor,bp,time);
      for_document = doc;
      for_fix = fix;
    }

   @Override public Boolean call() {
      int soff = for_document.mapOffsetToJava(for_problem.getStart());
      int eoff0 = for_document.mapOffsetToJava(for_problem.getEnd());
      BfixCheckAreas sareas = new BfixCheckAreas(soff,eoff0);
      int len = for_fix.getOriginalText().length();
      int eoff = soff+len;              // Use our length for double ids
      String txt = for_fix.getText(); 
      BfixEdit edit = new BfixBaseEdit(for_corrector,soff,eoff,txt);
      return testEdit(edit,sareas,"SpellingCorrection",false);
    }

   @Override public double getRegionOrder()		{ return 0; } 

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

