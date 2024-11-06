/********************************************************************************/
/*                                                                              */
/*              BfixStyleFixer.java                                             */
/*                                                                              */
/*      Fix CheckStyle errors                                                   */
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class BfixStyleFixer extends BfixAdapter
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static Map<Pattern,StyleGenerator> fix_map;

static {
   fix_map = new LinkedHashMap<>(); 
}

/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BfixStyleFixer() 
{
   super("StyleFixer"); 
}


/********************************************************************************/
/*                                                                              */
/*      Try to handle a fix that doesn't need checking                          */
/*                                                                              */
/********************************************************************************/

RunnableFix fixStyleProblem(BumpProblem bp,boolean explicit)
{
   if (!bp.getCategory().equals("BSTYLE")) return null;
   
   for (Map.Entry<Pattern,StyleGenerator> ent : fix_map.entrySet()) {
      Pattern p = ent.getKey();
      Matcher m = p.matcher(bp.getMessage());
      if (m.matches()) {
         RunnableFix rf = ent.getValue().findFix(bp,m);
         if (rf != null) return rf;
       }
    }
   
   
   return null;
}


/********************************************************************************/
/*                                                                              */
/*      Handle fixes that do need checking                                      */
/*                                                                              */
/********************************************************************************/

// If any fixers are added here, be sure to add this to Bfix.props

@Override void addFixers(BfixCorrector bc,BumpProblem bp,boolean explicit,
      List<BfixFixer> rslts)
{
   // check if this is a style problem that needs double checking
}



/********************************************************************************/
/*                                                                              */
/*      Generic style fix generator                                             */
/*                                                                              */
/********************************************************************************/

private abstract class StyleGenerator {
   
   abstract RunnableFix findFix(BumpProblem bp,Matcher m);
   
}       // end of inner class StyleGenerator





}       // end of class BfixStyleFixer




/* end of BfixStyleFixer.java */

