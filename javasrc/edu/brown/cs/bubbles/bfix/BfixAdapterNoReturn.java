/********************************************************************************/
/*                                                                              */
/*              BfixAdapterNoReturn.java                                        */
/*                                                                              */
/*      Handle missing returns on a new method                                  */
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


class BfixAdapterNoReturn extends BfixAdapter implements BfixConstants {


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static List<BfixErrorPattern> return_patterns;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BfixAdapterNoReturn()
{
   super("Insert Default Return");

   if (return_patterns == null) {
      return_patterns = new ArrayList<>();
      Element xml = BumpClient.getBump().getLanguageData();
      Element fxml = IvyXml.getChild(xml, "FIXES");
      for (Element cxml : IvyXml.children(fxml, "RETURN")) {
	 return_patterns.add(new BfixErrorPattern(cxml));
      }
   }
}


/********************************************************************************/
/*                                                                              */
/*      Abstract methods implementations                                        */
/*                                                                              */
/********************************************************************************/

@Override
public void addFixers(BfixCorrector corr,BumpProblem bp,boolean explicit,List<BfixFixer> rslt)
{
   String rtstmt = getReturnType(corr, bp);
   if (rtstmt == null) return;

   ReturnFixer fixer = new ReturnFixer(corr,bp,rtstmt);
   rslt.add(fixer);
}


@Override
protected String getMenuAction(BfixCorrector corr,BumpProblem bp)
{
   String name = getReturnType(corr, bp);
   return (name == null ? null : "Add Return");
}


/********************************************************************************/
/*                                                                              */
/*      Find the return statement to add if any                                 */
/*                                                                              */
/********************************************************************************/

private String getReturnType(BfixCorrector corr,BumpProblem bp)
{
   if (bp.getErrorType() != BumpErrorType.ERROR) return null;
   BoardLog.logD("BFIX", "RETURN problem " + bp.getMessage());
   
   for (BfixErrorPattern pat : return_patterns) {
      String typ = pat.getMatchResult(bp.getMessage());
      if (typ != null) return typ;
    }
   
   return null;
}


/********************************************************************************/
/*                                                                              */
/*      Fixing code                                                             */
/*                                                                              */
/********************************************************************************/

private static class ReturnFixer extends BfixFixer {

private String		   for_type;
private BaleWindowDocument for_document;
private long		   initial_time;

ReturnFixer(BfixCorrector corr,BumpProblem bp,String typ)
{
   super(corr,bp);
   for_type = typ;
   for_document = corr.getEditor().getWindowDocument();
   initial_time = corr.getStartTime();
}

@Override
protected String getMemoId()
{
   return for_type;
}

@Override
protected BfixRunnableFix findFix()
{
   int soff = for_document.mapOffsetToJava(for_problem.getStart());
   BaleWindowElement elt = for_document.getCharacterElement(soff);

   BaleWindowElement pelt = elt;
   while (pelt != null) {
      if (pelt.getName().equals("Method")) break;
      pelt = pelt.getBaleParent();
   }
   if (pelt == null) return null;

   int foff = pelt.getStartOffset();
   int eoff = pelt.getEndOffset();
   String text = for_document.getWindowText(foff, eoff - foff);

   if (text.contains("return ")) return null;
   int xpos = text.lastIndexOf("}");
   String value = "null";
   switch (for_type) {
      default:
	 break;
      case "int":
      case "short":
      case "byte":
      case "char":
	 value = "0";
	 break;
      case "long":
	 value = "0L";
	 break;
      case "float":
	 value = "0f";
	 break;
      case "double":
	 value = "0.0";
	 break;
      case "boolean":
	 value = "false";
	 break;
      case "void":
	 return null;
   }
   BoardMetrics.noteCommand("BFIX", "ReturnCheck");
   String stmt = "return " + value + ";\n";

   BumpClient bc = BumpClient.getBump();
   String proj = for_document.getProjectName();
   File file = for_document.getFile();
   String filename = file.getAbsolutePath();
   String pid = createPrivateBuffer(proj, filename);
   try {
      Collection<BumpProblem> probs = bc.getPrivateProblems(filename, pid);
      if (probs == null) {
	 BoardLog.logE("BFIX", "SPELL: Problem getting errors for " + pid);
	 return null;
      }
      int probct = getErrorCount(probs);
      if (!checkProblemPresent(for_problem, probs)) {
	 BoardLog.logD("BFIX", "SPELL: Problem went away");
	 return null;
      }
      int inspos = for_document.mapOffsetToEclipse(foff + xpos);
      bc.beginPrivateEdit(filename, pid);
      bc.editPrivateFile(proj, file, pid, inspos, inspos, stmt);
      probs = bc.getPrivateProblems(filename, pid);
      if (probs == null || getErrorCount(probs) > probct) return null;
      int delta = stmt.length();
      if (checkAnyProblemPresent(for_problem, probs, delta, delta)) return null;
   }
   finally {
      bc.removePrivateBuffer(proj, filename, pid);
   }

   if (for_corrector.getStartTime() != initial_time) return null;
   BoardLog.logD("BFIX", "RETURN: DO " + stmt);
   BoardMetrics.noteCommand("BFIX", "RETURNFIX");
   ReturnDoer rd = new ReturnDoer(for_corrector,for_document,for_problem,stmt,
	    initial_time);

   return rd;
}

} // end of inner class ReturnFixer


/********************************************************************************/
/*                                                                              */
/*      Code to actually insert the return                                      */
/*                                                                              */
/********************************************************************************/

private static class ReturnDoer implements BfixRunnableFix {
   
   private BfixCorrector	   for_corrector;
   private BaleWindowDocument for_document;
   private BumpProblem	   for_problem;
   private String		   insert_stmt;
   private long		   initial_time;
   
   ReturnDoer(BfixCorrector corr,BaleWindowDocument doc,BumpProblem bp,String text,long time) {
      for_corrector = corr;
      for_document = doc;
      for_problem = bp;
      insert_stmt = text;
      initial_time = time;
    }
   
   @Override
   public Boolean call() {
      BumpClient bc = BumpClient.getBump();
      List<BumpProblem> probs = bc.getProblems(for_document.getFile());
      if (!checkProblemPresent(for_problem, probs)) return false;
      if (for_corrector.getStartTime() != initial_time) return false;
      int soff = for_document.mapOffsetToJava(for_problem.getStart());
      BaleWindowElement elt = for_document.getCharacterElement(soff);
      
      BaleWindowElement pelt = elt;
      while (pelt != null) {
         if (pelt.getName().equals("Method")) break;
         pelt = pelt.getBaleParent();
       }
      if (pelt == null) return false;
      int foff = pelt.getStartOffset();
      int eoff = pelt.getEndOffset();
      String text = for_document.getWindowText(foff, eoff - foff);
      if (text.contains("return ")) return false;
      int xpos = text.lastIndexOf("}");
      int inspos = foff + xpos;
      if (!checkSafePosition(for_corrector, inspos - 1, inspos + 1)) return false;
      
      BoardMetrics.noteCommand("BFIX", "AddReturn_" + for_corrector.getBubbleId());
      // int eoff0 = for_document.mapOffsetToJava(for_problem.getEnd());
      for_document.replace(inspos, 0, insert_stmt, true, true);
      BoardMetrics.noteCommand("BFIX", "DoneAddReturn_" + for_corrector.getBubbleId());
      
      return true;
    }
   
   @Override
   public double getPriority() {
      return 0;
    }

} // end of inner class ReturnDoer


} // end of class BfixAdapterNoReturn



/* end of BfixAdapterNoReturn.java */

