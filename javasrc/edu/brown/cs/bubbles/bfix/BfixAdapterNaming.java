/********************************************************************************/
/*                                                                              */
/*              BfixAdapterNaming.java                                          */
/*                                                                              */
/*      Handle naming errors                                                    */
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



package edu.brown.cs.bubbles.bfix;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.w3c.dom.Element;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.ivy.xml.IvyXml;

class BfixAdapterNaming extends BfixAdapter implements BfixConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static List<BfixErrorPattern> naming_patterns;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BfixAdapterNaming()
{
   super("Fix naming issues");
   
   if (naming_patterns == null) {
      naming_patterns = new ArrayList<>();
      Element xml = BumpClient.getBump().getLanguageData();
      Element fxml = IvyXml.getChild(xml,"FIXES");
      for (Element cxml : IvyXml.children(fxml,"NAMING")) {
         naming_patterns.add(new BfixErrorPattern(cxml));
       }
    }
}


/********************************************************************************/
/*                                                                              */
/*      Adapter interface                                                       */
/*                                                                              */
/********************************************************************************/

@Override 
public void addFixers(BfixCorrector corr,BumpProblem bp,boolean explicit,
      List<BfixFixer> rslt)
{
   NamingFixer fixer = findFixer(corr,bp);
   if (fixer != null) rslt.add(fixer);
}


@Override protected String getMenuAction(BfixCorrector corr,BumpProblem bp)
{
   return null;
}



/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

private NamingFixer findFixer(BfixCorrector corr,BumpProblem bp)
{
   BaleWindowDocument doc = corr.getEditor().getWindowDocument();
   int start = doc.mapOffsetToJava(bp.getStart());
   BaleWindowElement pelt = doc.getCharacterElement(start);
   if (!pelt.isIdentifier()) return null;
   int elstart = pelt.getStartOffset();
   int elend = pelt.getEndOffset();
   String repl = null;
   String with = null;
   for (BfixErrorPattern pat : naming_patterns) {
      repl = pat.getMatchResult(bp.getMessage());
      if (repl != null) {
         with = pat.getAltResult(bp.getMessage());
       }
    }
   
   if (repl == null || with == null) return null;
   
   return new NamingFixer(corr,bp,elstart,elend,with,corr.getStartTime());
}



/********************************************************************************/
/*										*/
/*	Fixing code								*/
/*										*/
/********************************************************************************/

private static class NamingFixer extends BfixFixer {
   
   private int start_pos;
   private int end_pos;
   private String new_token;
   private long initial_time;
   
   NamingFixer(BfixCorrector corr,BumpProblem bp,int spos,int epos,String what,long when) {
      super(corr,bp);
      start_pos = spos;
      end_pos = epos;
      new_token = what;
      initial_time = when;
    }
   
   @Override protected BfixRunnableFix findFix() {
      if (for_corrector.getStartTime() != initial_time) return null;
      BumpClient bc = BumpClient.getBump();
      BaleWindowDocument doc = for_corrector.getEditor().getWindowDocument();
      String proj = doc.getProjectName();
      File file = doc.getFile();
      String filename = file.getAbsolutePath();
      String pid = bc.createPrivateBuffer(proj,filename,null);
      try {
         Collection<BumpProblem> probs = bc.getPrivateProblems(filename, pid);
         if (probs == null) return null;
         if (!checkProblemPresent(for_problem,probs)) return null;
         int inspos = doc.mapOffsetToEclipse(start_pos);
         int epos = doc.mapOffsetToEclipse(end_pos);
         bc.beginPrivateEdit(filename, pid);
         bc.editPrivateFile(proj, file,
               pid, inspos, epos, new_token);
         Collection<BumpProblem> nprobs = bc.getPrivateProblems(filename,pid);
         if (nprobs.size() >= probs.size()) return null;
       }
      finally {
         bc.removePrivateBuffer(proj,filename,pid);
       }
      
      if (for_corrector.getStartTime() != initial_time) return null;
      if (!checkSafePosition(for_corrector,start_pos,end_pos+1)) return null;
      
      BoardLog.logD("BFIX","NAMING FIX " + new_token);
      BoardMetrics.noteCommand("BFIX","NAMINGFIX");
      
      NamingDoer nd = new NamingDoer(for_corrector,for_problem,start_pos,end_pos,new_token,initial_time);
  
      return nd;
    }
   
}	// end of inner class NamingFixer



/********************************************************************************/
/*										*/
/*	Code to change the visibility						*/
/*										*/
/********************************************************************************/

private static class NamingDoer implements BfixRunnableFix {
   
   private BfixCorrector for_corrector;
   private BumpProblem for_problem;
   private int start_offset;
   private int end_offset;
   private String new_token;
   private long initial_time;
   
   NamingDoer(BfixCorrector corr,BumpProblem bp,int soff,int eoff,String what,long time) {
      for_corrector = corr;
      for_problem = bp;
      start_offset = soff;
      end_offset = eoff;
      new_token = what;
      initial_time = time;
    }
   
   @Override public Boolean call() {
      BumpClient bc = BumpClient.getBump();
      BaleWindowDocument doc = for_corrector.getEditor().getWindowDocument();
      File f = doc.getFile();
      List<BumpProblem> probs = bc.getProblems(f);
      if (!checkProblemPresent(for_problem,probs)) return false;
      if (for_corrector.getStartTime() != initial_time) return false;
      if (!checkSafePosition(for_corrector,start_offset,end_offset+1)) return false;
      
      int inspos = doc.mapOffsetToEclipse(start_offset);
      int epos = doc.mapOffsetToEclipse(end_offset);
      inspos = start_offset;
      epos = end_offset;
      
      BoardMetrics.noteCommand("BFIX","ChangeNaming_" + for_corrector.getBubbleId());
      doc.replace(inspos,epos-inspos,new_token,true,true);
      BoardMetrics.noteCommand("BFIX","DoneChangeNaming_" + for_corrector.getBubbleId());
      
      return true;
    }
   
   @Override public double getPriority()                { return 0; }
   
}	// end of inner class NamingDoer



}       // end of class BfixAdapterNaming




/* end of BfixAdapterNaming.java */

