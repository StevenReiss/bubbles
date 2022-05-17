/********************************************************************************/
/*                                                                              */
/*              BfixAdapterDeclaration.java                                     */
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



package edu.brown.cs.bubbles.bfix;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.brown.cs.bubbles.board.BoardLog;


class BfixAdapterDeclaration extends BfixAdapter implements BfixConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static Pattern  missing_type_pattern;

static {
   missing_type_pattern = Pattern.compile("([A-Za-z0-9_]+) cannot be resolved to a variable");
}



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BfixAdapterDeclaration()
{
   super("Add Type for Declaration");
}


/********************************************************************************/
/*                                                                              */
/*      Abstract method implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override void addFixers(BfixCorrector corr,BumpProblem bp,boolean explicit,
      List<BfixFixer> rslt)
{
   if (!explicit) return;
   
   String var = getVariableToDeclare(bp);
   if (var == null) return;
   
   DeclFixer fixer = new DeclFixer(corr,bp,var);
   rslt.add(fixer);
}



@Override String getMenuAction(BfixCorrector corr,BumpProblem bp)
{
   String var = getVariableToDeclare(bp);
   if (var != null) return "Make Declaration";
   return null;
}



private String getVariableToDeclare(BumpProblem bp)
{
   if (bp.getErrorType() != BumpErrorType.ERROR) return null;
   Matcher m = missing_type_pattern.matcher(bp.getMessage());
   if (!m.matches()) return null;
   String var = m.group(1);
   return var;
}



/********************************************************************************/
/*                                                                              */
/*      Fixing Cocde                                                            */
/*                                                                              */
/********************************************************************************/

private static class DeclFixer extends BfixFixer {
   
   private String variable_name;
   private BaleWindowDocument for_document;
   private long initial_time;
   
   DeclFixer(BfixCorrector corr,BumpProblem bp,String var) {
      super(corr,bp);
      variable_name = var;
      for_document = corr.getEditor().getWindowDocument();
      initial_time = corr.getStartTime(); 
    }
   
   @Override protected RunnableFix findFix() {
      List<BumpFix> fixes = for_problem.getFixes();
      if (fixes == null || fixes.isEmpty()) return  null;
      for (BumpFix bf : fixes) {
         String what = bf.getParameter("DISPLAY");
         if (what.startsWith("Create local variable '")) {
            BoardLog.logD("BFIX","Fix type: " + bf.getType() + " " +
                  bf.getParameter("DISPLAY"));
          }
       }
      
      return null;
    }
   
}       // end of inner class DeclFixer

}       // end of class BfixAdapterDeclaration




/* end of BfixAdapterDeclaration.java */

