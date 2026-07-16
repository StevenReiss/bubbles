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

import edu.brown.cs.bubbles.bfix.BfixFixer.BfixBaseEdit;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.ivy.xml.IvyXml;

import java.util.ArrayList;
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
   
   int usepos = foff + xpos;
   BfixEdit edit = new BfixBaseEdit(for_corrector,usepos,usepos,stmt);
   int delta = stmt.length();
   BfixCheckAreas dareas = new BfixCheckAreas(delta,delta);
   if (!checkPrivateEdit(edit,null,dareas,true)) {
      return null;
    }
  
   BoardLog.logD("BFIX", "RETURN: DO " + stmt);
   BoardMetrics.noteCommand("BFIX", "RETURNFIX");
   ReturnDoer rd = new ReturnDoer(for_corrector,for_document,for_problem,
         stmt,initial_time);

   return rd;
}

} // end of inner class ReturnFixer


/********************************************************************************/
/*                                                                              */
/*      Code to actually insert the return                                      */
/*                                                                              */
/********************************************************************************/

private static class ReturnDoer extends BfixFixDoer {
   
   private BaleWindowDocument  for_document;
   private String	        insert_stmt;
   
   ReturnDoer(BfixCorrector corr,BaleWindowDocument doc,BumpProblem bp,String text,long time) {
      super(corr,bp,time);
      for_document = doc;
      insert_stmt = text;
    }
   
   @Override
   public Boolean call() {
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
      
      BfixCheckAreas sareas = new BfixCheckAreas(inspos-1,inspos+1);
      BfixEdit edit = new BfixBaseEdit(for_corrector,inspos,inspos,insert_stmt);
      return testEdit(edit,sareas,"AddReturn");
    }
   
   @Override public double getRegionOrder()             { return 0; }  

}       // end of inner class ReturnDoer


}       // end of class BfixAdapterNoReturn



/* end of BfixAdapterNoReturn.java */

