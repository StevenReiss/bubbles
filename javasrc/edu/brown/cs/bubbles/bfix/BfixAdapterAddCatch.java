/********************************************************************************/
/*                                                                              */
/*              BfixAdapterAddCatch.java                                        */
/*                                                                              */
/*      description of class                                                    */
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




class BfixAdapterAddCatch extends BfixAdapter implements BfixConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static List<BfixErrorPattern>  catch_patterns;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BfixAdapterAddCatch()
{
   super("Insert Catch Clauses");
   
   if (catch_patterns == null) {
      catch_patterns = new ArrayList<>();
      Element xml = BumpClient.getBump().getLanguageData();
      Element fxml = IvyXml.getChild(xml,"FIXES");
      for (Element cxml : IvyXml.children(fxml,"CATCH")) {
         catch_patterns.add(new BfixErrorPattern(cxml));
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Abstract method implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override public void addFixers(BfixCorrector corr,BumpProblem bp,boolean explicit,
      List<BfixFixer> rslt)
{
   String ctype = getCatchType(corr,bp);
   if (ctype == null) return;
   
   CatchFixer fixer = new CatchFixer(corr,bp,ctype);
   rslt.add(fixer);
}




@Override protected String getMenuAction(BfixCorrector corr,BumpProblem bp) 
{
   String ctyp = getCatchType(corr,bp);
   if (ctyp != null && !ctyp.equals("*")) return "Add Catch";
   return null;
}



private String getCatchType(BfixCorrector corr,BumpProblem bp)
{
   if (bp.getErrorType() != BumpErrorType.ERROR) return null;
   
   for (BfixErrorPattern pat : catch_patterns) {
      String rslt = pat.getMatchResult(bp.getMessage());
      if (rslt != null) return rslt;
    }
   
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

private static BaleWindowElement findTryStatement(BaleWindowDocument doc,int soff)
{
   BaleWindowElement elt = doc.getCharacterElement(soff);
   BaleWindowElement pelt = elt;
   while (pelt != null) {
      if (pelt.getName().equals("SplitStatement")) {
         BaleWindowElement celt = doc.getCharacterElement(pelt.getStartOffset());
         while (celt.getName().equals("Indent")) {
            celt = celt.getNextCharacterElement();
          }
         if (celt.getName().equals("Keyword")) {
            BaleTokenType tt = celt.getTokenType();
            if (tt == BaleTokenType.TRY) break;
          }
       }
      pelt = pelt.getBaleParent();
    }
   
   return pelt;
}




/********************************************************************************/
/*                                                                              */
/*      Fixing code                                                             */
/*                                                                              */
/********************************************************************************/

private static class CatchFixer extends BfixFixer {
   
   private String for_type;
   private BaleWindowDocument for_document;
   private long initial_time;
   
   CatchFixer(BfixCorrector corr,BumpProblem bp,String typ) {
      super(corr,bp);
      for_type = typ;
      if (typ != null && typ.equals("*")) for_type = null;
      for_document = corr.getEditor().getWindowDocument();
      initial_time = corr.getStartTime();
    }
   
   @Override protected BfixRunnableFix findFix() {
      int soff = for_document.mapOffsetToJava(for_problem.getStart());
      BaleWindowElement pelt = findTryStatement(for_document,soff);
      if (pelt == null) return null;
      if (for_type == null) {
         findTypeForCatch(pelt);
         if (for_type == null) return null;
       }
      if (!checkCatchInsert(pelt)) return null;
      if (for_corrector.getStartTime() != initial_time) return null;
      BoardLog.logD("BFIX","CATCH " + for_type);
      BoardMetrics.noteCommand("BFIX","CATCHFIX");
      int eoff = pelt.getEndOffset();
      String insert = "catch (" + for_type + " _ex) {\n}\n";
      BfixEdit edit = new BfixBaseEdit(for_corrector,eoff,eoff,insert);
      BfixCheckAreas areas = new BfixCheckAreas(eoff-1,eoff+1);
      
      CatchDoer cd = new CatchDoer(for_corrector,for_problem,edit,areas,initial_time);
      return cd; 
    }
   
   private void findTypeForCatch(BaleWindowElement pelt) {
      int foff = pelt.getStartOffset();
      int eoff = pelt.getEndOffset();
      BumpClient bc = BumpClient.getBump();
      String proj = for_document.getProjectName();
      File file = for_document.getFile();
      String filename = file.getAbsolutePath();
      String pid = bc.createPrivateBuffer(proj, filename, null);
      try {
         Collection<BumpProblem> probs = bc.getPrivateProblems(filename, pid);
         if (probs == null) return;
         if (!checkProblemPresent(for_problem,probs)) return;
         int inspos = for_document.mapOffsetToEclipse(eoff);
         int spos = for_document.mapOffsetToEclipse(foff);
         bc.beginPrivateEdit(filename, pid);
         bc.editPrivateFile(proj, file, pid, inspos, inspos, " finally { }");
         probs = bc.getPrivateProblems(filename,pid);
         for (BumpProblem bp : probs) {
            if (bp.getStart() >= spos && bp.getStart() <= inspos) {
               for (BfixErrorPattern pat : catch_patterns) {
                  String rslt = pat.getMatchResult(bp.getMessage());
                  if (rslt != null && !rslt.equals("*")) {
                     for_type = rslt;
                     return;
                   }
                }
             }
          }
       }
      finally {
         bc.removePrivateBuffer(proj, filename, pid);
       }
    }
   
   private boolean checkCatchInsert(BaleWindowElement pelt) {
      String insert = " catch (" + for_type + " ___x___) { }";
      int foff = pelt.getStartOffset();
      int eoff = pelt.getEndOffset();
      BfixEdit edit = new BfixBaseEdit(for_corrector,eoff,eoff,insert);
      BfixCheckAreas areas = new BfixCheckAreas(foff,eoff);   
      return checkPrivateEdit(edit,areas,null,true); 
    }
   
}       // end of inner class CatchFixer




/********************************************************************************/
/*                                                                              */
/*      Code to actually add the catch statement                                */
/*                                                                              */
/********************************************************************************/

private static class CatchDoer extends BfixFixDoer { 
   
   private BfixEdit for_edit;
   private BfixCheckAreas for_areas;
   
   CatchDoer(BfixCorrector corr,BumpProblem bp,
         BfixEdit edit,BfixCheckAreas areas,long time) {
      super(corr,bp,time);
      for_edit = edit;
      for_areas = areas;
    }

   @Override public Boolean call() {
      return testEdit(for_edit,for_areas,"AddCatch");
    }

   @Override public double getRegionOrder()                        { return 0; } 
   
}       // end of inner class CatchDoer





}       // end of class BfixAdapterAddCatch




/* end of BfixAdapterAddCatch.java */

