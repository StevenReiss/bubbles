/********************************************************************************/
/*                                                                              */
/*              BstyleFixAdapter.java                                           */
/*                                                                              */
/*      BFIX adapter to fix style problems automatically or on demand           */
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



package edu.brown.cs.bubbles.bstyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.brown.cs.bubbles.bfix.BfixAdapter;
import edu.brown.cs.bubbles.bfix.BfixConstants;
import edu.brown.cs.bubbles.bfix.BfixCorrector;
import edu.brown.cs.bubbles.bfix.BfixFixer;
import edu.brown.cs.bubbles.board.BoardLog;

public class BstyleFixAdapter extends BfixAdapter implements BstyleConstants, BfixConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<BstyleFixer> fixer_set;
private Map<String,List<BstyleFixer>> fixer_map;
private Set<String> ignore_errors;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public BstyleFixAdapter()
{
   super("StyleFixer");
   
   fixer_set = BstyleFixer.getStyleFixers(); 
   fixer_map = BstyleFixer.getStyleFixerMap(); 
   ignore_errors = BstyleFixer.getIgnoreErrors(); 
}


/********************************************************************************/
/*                                                                              */
/*      Handle fixes that might need checking                                   */
/*                                                                              */
/********************************************************************************/

@Override 
public void addFixers(BfixCorrector bc,BumpProblem bp,boolean explicit,
      List<BfixFixer> rslts)
{
   // check if this is a style problem that needs double checking
   // if so return a fixer using bstylefixer
}


/********************************************************************************/
/*                                                                              */
/*      Handle style problems                                                   */
/*                                                                              */
/********************************************************************************/

public BfixRunnableFix findStyleFixer(BfixCorrector bc,BumpProblem bp,boolean explicit)
{
   if (!bp.getCategory().equals("BSTYLE")) return null;
   
   BoardLog.logD("BSTYLE","Work on problem " + bp.getData() + " " + bp.getMessage());
   
   String d = bp.getData();
   if (d == null) d = "";
   if (ignore_errors.contains(d)) return null;
   
   List<BstyleFixer> totry = fixer_map.get(d);
   if (totry != null) {
      for (BstyleFixer bf : totry) {
         BfixRunnableFix rf = bf.findFix(bc,bp,explicit);  
         if (rf != null) return rf;
       }
    }
   
   for (BstyleFixer bf : fixer_set) {
      if (totry != null && totry.contains(bf)) continue;
      BfixRunnableFix rf = bf.findFix(bc,bp,explicit);  
      if (rf != null) {
         if (d != null && !d.isEmpty()) {
            BoardLog.logE("BSTYLE","Missing fixer for type " + d + " " +
                  bf.getClass());
            if (totry == null) {
               totry = new ArrayList<>();
               fixer_map.put(d,totry);
             }
            totry.add(bf);
          }
         return rf;
       }
    }
   
   return null;
}



}       // end of class BstyleFixAdapter




/* end of BstyleFixAdapter.java */

