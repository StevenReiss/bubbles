/********************************************************************************/
/*										*/
/*		BfixAdapter.java						*/
/*										*/
/*	Adapter to handle a set of error messages				*/
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

import java.io.File;
import java.util.Collection;
import java.util.List;

public abstract class BfixAdapter implements BfixConstants, BaleConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String adapter_name;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected BfixAdapter(String name)
{
   adapter_name = name;
}




/********************************************************************************/
/*										*/
/*	Abstract Action methods 						*/
/*										*/
/********************************************************************************/

public void addFixers(BfixCorrector bc,BumpProblem bp,boolean explict,List<BfixFixer> rslts)
{ }

public void addChores(BfixCorrector bc,BumpProblem bp,List<BfixChore> rslts)
{ }

public BfixRunnableFix findStyleFixer(BfixCorrector bc,BumpProblem bp,boolean explicit)
{
   return null;
}


// note that the action might try multiple fixers -- generally only one action
// should be generated, but there might be multiple things to try in parallel

protected String getMenuAction(BfixCorrector bc,BumpProblem bp)
{
   return null;
}




/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

public static boolean checkProblemPresent(BumpProblem prob,Collection<BumpProblem> bpl)
{
   for (BumpProblem bp : bpl) {
      if (!bp.getProblemId().equals(prob.getProblemId())) continue;
      if (bp.getStart() != prob.getStart()) continue;
      if (Math.abs(bp.getEnd() - prob.getEnd()) > 2) continue;
      if (!bp.getFile().equals(prob.getFile())) continue;
      return true;
    }

   return false;
}



static int getErrorCount(Collection<BumpProblem> bpl)
{
   int ct = 0;
   if (bpl == null) return -1;

   for (BumpProblem bp : bpl) {
      if (bp.getErrorType() == BumpErrorType.ERROR) ++ct;
    }

   return ct;
}


static int getErrorCount(Collection<BumpProblem> bpl,BumpProblem base)
{
   int ct = 0;
   if (bpl == null) return -1;
   int pct = 0;
   
   for (BumpProblem bp : bpl) {
      if (bp.getErrorType() == BumpErrorType.ERROR) {
         if (base != null)  {
            if (base.getStart() == bp.getStart() &&
                  base.getEnd() == bp.getEnd()) {
               ++pct;
             }
          }
         ++ct;
       }
    }
   
   if (pct > 1) ct -= (pct-1);
   
   return ct;
}




static boolean checkAnyProblemPresent(BumpProblem prob,Collection<BumpProblem> bpl,int sdelta,int edelta)
{
   if (bpl == null) return false;
   for (BumpProblem bp : bpl) {
      if (!bp.getFile().equals(prob.getFile())) continue;
      if (bp.getErrorType() != BumpErrorType.ERROR) continue;
      if (bp.getStart() < prob.getEnd()+edelta && bp.getEnd() > prob.getStart()+sdelta) return true;
    }

   return false;
}



static boolean checkAnyProblemPresent(Collection<BumpProblem> bpl,File file,int start,int end)
{
   if (bpl == null) return false;
   for (BumpProblem bp : bpl) {
      if (!bp.getFile().equals(file)) continue;
      if (bp.getErrorType() != BumpErrorType.ERROR) continue;
      if (bp.getStart() < end && bp.getEnd() > start) return true;
    }
   
   return false;
}



public static boolean checkSafePosition(BfixCorrector bc,int start,int end)
{
   int pos = bc.getCaretPosition();
   if (pos >= 0 && pos >= start && pos <= end+1) {
      BoardLog.logD("BFIX","Fix stopped because of cursor in the way");
      return false;
    }
   return true;
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   return "Adapter: " + adapter_name;
}



}	// end of class BfixAdapter

/* end of BfixAdapter.java */

