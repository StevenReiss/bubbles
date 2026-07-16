/********************************************************************************/
/*										*/
/*		BfixAdapterVisibility.java					*/
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

import edu.brown.cs.bubbles.bale.BaleConstants;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.ivy.xml.IvyXml;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

class BfixAdapterVisibility extends BfixAdapter implements BfixConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static List<BfixErrorPattern> visibility_patterns;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BfixAdapterVisibility()
{
   super("Fix reduced visibility issues");
   
   if (visibility_patterns == null) {
      visibility_patterns = new ArrayList<>();
      Element xml = BumpClient.getBump().getLanguageData();
      Element fxml = IvyXml.getChild(xml,"FIXES");
      for (Element cxml : IvyXml.children(fxml,"VISIBILITY")) {
         visibility_patterns.add(new BfixErrorPattern(cxml));
       }
    }
}



/********************************************************************************/
/*										*/
/*	Adapter interface							*/
/*										*/
/********************************************************************************/

@Override 
public void addFixers(BfixCorrector corr,BumpProblem bp,boolean explicit,
      List<BfixFixer> rslt)
{
   String outer = getSuperClass(corr,bp);
   if (outer == null) return;
   VisibilityFixer fixer = findFixer(corr,bp,outer);
   if (fixer != null) rslt.add(fixer);
}


@Override protected String getMenuAction(BfixCorrector corr,BumpProblem bp)
{
   String outer = getSuperClass(corr,bp);
   if (outer != null) return "Change visibility";
   return null;
}


private String getSuperClass(BfixCorrector corr,BumpProblem bp)
{
   if (bp.getErrorType() != BumpErrorType.ERROR) return null;
   for (BfixErrorPattern pat : visibility_patterns) {
      String rslt = pat.getMatchResult(bp.getMessage());
      if (rslt != null) return rslt;
    }
  return null;
}



/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

private VisibilityFixer findFixer(BfixCorrector corr,BumpProblem bp,String outer)
{
   BaleWindowDocument doc = corr.getEditor().getWindowDocument();
   int start = doc.mapOffsetToJava(bp.getStart());
   BaleWindowElement pelt = doc.getCharacterElement(start);
   if (!pelt.isIdentifier()) return null;
   int elstart = pelt.getStartOffset();
   int elend = pelt.getEndOffset();
   String nm = doc.getWindowText(elstart,elend-elstart);
   BumpClient bc = BumpClient.getBump();
   String pat = outer + "." + nm;
   List<BumpLocation> locs = bc.findMethods(null,nm,false,true,false,false);
   if (locs == null || locs.size() == 0) return null;
   BumpLocation fnd = null;
   BumpLocation cur = null;
   for (BumpLocation bl : locs) {
      String n1 = bl.getSymbolName();
      if (n1.endsWith(pat)) fnd = bl;
      else if (bl.getFile().equals(bp.getFile())) {
	 if (bp.getStart() >= bl.getDefinitionOffset() &&
	       bp.getStart() <= bl.getDefinitionEndOffset())
	    cur = bl;
       }
    }
   if (fnd == null || cur == null) return null;
   int mods = fnd.getModifiers();
   String what = null;
   if (Modifier.isPublic(mods)) what = "public";
   else if (Modifier.isProtected(mods)) what = "protected";
   String owhat = null;
   mods = cur.getModifiers();
   if (Modifier.isPublic(mods)) owhat = "public";
   else if (Modifier.isProtected(mods)) owhat = "protected";
   else if (Modifier.isPrivate(mods)) owhat = "private";

   int spos = 0;
   int epos = 0;
   for (BaleWindowElement prelt = pelt;
	prelt != null;
	prelt = prelt.getPreviousCharacterElement()) {
      System.err.println("FOUND " + prelt + " " + prelt.getTokenType());
      if (owhat != null) {
	 if (prelt.getTokenType() == BaleConstants.BaleTokenType.KEYWORD) {
	    int tstart = prelt.getStartOffset();
	    int tlend = prelt.getEndOffset();
	    String tok = doc.getWindowText(tstart,tlend-tstart);
	    if (tok.equals(owhat)) {
	       spos = prelt.getStartOffset();
	       epos = prelt.getEndOffset();
	       break;
	     }
	  }
       }
      else {
	 switch (prelt.getTokenType()) {
	    case EOL :
	    case SEMICOLON :
	       spos = prelt.getEndOffset();
	       epos = spos;
	       break;
	    case IDENTIFIER :
	       int tstart = prelt.getStartOffset();
	       int tlend = prelt.getEndOffset();
	       String tok = doc.getWindowText(tstart,tlend-tstart);
	       if (tok.equals("Override")) {
		  spos = prelt.getEndOffset()+1;
		  epos = spos;
		}
	       break;
            default :
               break;
	  }
	 if (spos > 0) break;
       }
    }

   if (spos == 0 || epos == 0) return null;

   return new VisibilityFixer(corr,bp,spos,epos,what + " ",corr.getStartTime());
}



/********************************************************************************/
/*										*/
/*	Fixing code								*/
/*										*/
/********************************************************************************/

private static class VisibilityFixer extends BfixFixer {

   private int start_pos;
   private int end_pos;
   private String new_token;
   private long initial_time;


   VisibilityFixer(BfixCorrector corr,BumpProblem bp,int spos,int epos,String what,long when) {
      super(corr,bp);
      start_pos = spos;
      end_pos = epos;
      new_token = what;
      initial_time = when;
    }

   @Override protected BfixRunnableFix findFix() {
      BfixEdit edit = new BfixBaseEdit(for_corrector,start_pos,end_pos,new_token);
      BfixCheckAreas sarea = new BfixCheckAreas(start_pos,end_pos+1);
      if (!checkPrivateEdit(edit,sarea,null,false)) {
         return null;
       }
      
      BoardLog.logD("BFIX","VISIBILITY FIX " + new_token);
      BoardMetrics.noteCommand("BFIX","VISIBILITYFIX");
      VisibilityDoer vd = new VisibilityDoer(for_corrector,for_problem,start_pos,end_pos,new_token,initial_time);
      return vd;
    }

}	// end of inner class VisibilityFixer



/********************************************************************************/
/*										*/
/*	Code to change the visibility						*/
/*										*/
/********************************************************************************/

private static class VisibilityDoer extends BfixFixDoer {

   private int start_offset;
   private int end_offset;
   private String new_token;

   VisibilityDoer(BfixCorrector corr,BumpProblem bp,int soff,int eoff,String what,long time) {
      super(corr,bp,time);
      start_offset = soff;
      end_offset = eoff;
      new_token = what;
    }

   @Override public Boolean call() {
      BfixEdit edit = new BfixFixer.BfixBaseEdit(for_corrector,start_offset,end_offset,new_token);
      BfixCheckAreas area = new BfixCheckAreas(start_offset,end_offset+1);
      return testEdit(edit,area,"ChangeVisibility",true);
    }

   @Override public double getRegionOrder()                { return 0; } 
   
}	// end of inner class VisibilityDoer




}	// end of class BfixAdapterVisibility




/* end of BfixAdapterVisibility.java */

