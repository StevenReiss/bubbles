/********************************************************************************/
/*                                                                              */
/*              BfixFixer.java                                                  */
/*                                                                              */
/*      Generic form of a routine to find a fix                                 */
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

import edu.brown.cs.bubbles.bale.BaleConstants.BaleWindowDocument;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpProblem;

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.SwingUtilities;

public abstract class BfixFixer implements Runnable, BfixConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

protected BfixCorrector         for_corrector;
protected BumpProblem           for_problem;
private FixAdapter              subfix_data;
private BfixMemo                fix_memo;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected BfixFixer(BfixCorrector bc,BumpProblem bp)       
{
   for_corrector = bc;
   for_problem = bp;
   subfix_data = null;
   fix_memo = null;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

void setSubFixData(FixAdapter sd)
{
   subfix_data = sd;
}


BfixMemo getMemo()
{
   if (fix_memo == null) {
      fix_memo = new BfixMemo(for_problem,getClass(),getMemoId());
    }
   
   return fix_memo;
}


protected String getMemoId()
{
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Running methods                                                         */
/*                                                                              */
/********************************************************************************/

@Override public final void run()
{
   for_corrector.removePending(getMemo());
   
   BfixRunnableFix r = findFix();
   if (subfix_data == null) {
      if (r != null) SwingUtilities.invokeLater(r);
    }
   else {
      subfix_data.noteFix(r);
    }
}



protected abstract BfixRunnableFix findFix();



/********************************************************************************/
/*                                                                              */
/*      Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

protected String createPrivateBuffer(String proj,String filename)
{
   BumpClient bc = BumpClient.getBump();
   String pid = null;
   if (subfix_data == null) {
      pid = bc.createPrivateBuffer(proj,filename,null);
    }
   else {
      pid = bc.createPrivateBuffer(proj,filename,null,subfix_data.getPrivateBufferId());
    }
   
   return pid;
}



protected List<BfixEdit> findPrivateEdits(Collection<BfixEdit> edits,CheckAreas areas)
{
   BaleWindowDocument doc = for_corrector.getEditor().getWindowDocument();
   String proj = doc.getProjectName();
   BumpClient bc = BumpClient.getBump();
   File file = doc.getFile();
   String filename = file.getAbsolutePath();
   String pid = bc.createPrivateBuffer(proj, filename, null);
   List<BfixEdit> rslt = null;
   List<BfixEdit> alt = null;
   try {
      for (BfixEdit edit : edits) {
         Boolean fg = checkOneEdit(bc,pid,filename,doc,edit,areas);
         if (fg == null) {
            if (rslt == null) {
               if (alt == null) alt = new ArrayList<>();
               alt.add(edit);
             }
          }
         else if (fg) {
            if (rslt == null) rslt = new ArrayList<>();
            alt = null;
            rslt.add(edit);
          }
       }
    }
   finally {
      bc.removePrivateBuffer(proj,filename,pid);
    }
   
   if (rslt == null && alt != null) rslt = alt;
   
   return rslt;
}


protected boolean checkPrivateEdit(BfixEdit edit,CheckAreas areas)
{
   BaleWindowDocument doc = for_corrector.getEditor().getWindowDocument();
   String proj = doc.getProjectName();
   BumpClient bc = BumpClient.getBump();
   File file = doc.getFile();
   String filename = file.getAbsolutePath();
   String pid = bc.createPrivateBuffer(proj, filename, null);
   try {
      Boolean fg = checkOneEdit(bc,pid,filename,doc,edit,areas);
      if (fg == null) fg = false;
      return fg;
    }
   finally {
      bc.removePrivateBuffer(proj,filename,pid);
    }
}


private Boolean checkOneEdit(BumpClient bc,String pid,String filename,
      BaleWindowDocument doc,BfixEdit edit,CheckAreas areas)
{
   Collection<BumpProblem> probs = bc.getPrivateProblems(filename, pid);
   if (probs == null) return false;
   int probct = BfixAdapter.getErrorCount(probs);
   if (!BfixAdapter.checkProblemPresent(for_problem,probs)) return false;
   
   bc.beginPrivateEdit(filename, pid);
   edit.makeEdit(pid);
   probs = bc.getPrivateProblems(filename,pid);
   
   if (edit.unmakeEdit(pid)) {
      bc.getPrivateProblems(filename,pid);
    }
   
   if (probs == null || BfixAdapter.getErrorCount(probs) > probct) {
      return false;
    }
   for (Point p : areas.getAreas()) {
      if (BfixAdapter.checkAnyProblemPresent(for_problem,probs,
            p.x,p.y)) {
         return false;
       }
    }
   if (BfixAdapter.getErrorCount(probs) == probct) {
      return null;
    }
   
   return true;
}



/********************************************************************************/
/*                                                                              */
/*      Handle multiple fix attempts                                            */
/*                                                                              */
/********************************************************************************/

protected void checkForFurtherFix(BfixCorrector cor,Runnable okfix,String pid,
      BumpProblem bp)
{
   FutureCallback fc = new FutureCallback(okfix);
   BfixSubFix nsf = new BfixSubFix(cor,bp,pid,fc);
   cor.checkProblemFixable(nsf);
}




private class FutureCallback implements CanFixCallback
{
   private Runnable fix_to_make;
   
   FutureCallback(Runnable r) {
      fix_to_make = r;
    }
   
   @Override public void canFix(boolean fg) {
      if (subfix_data != null) subfix_data.noteStatus(fg);
      else if (fg) {
         SwingUtilities.invokeLater(fix_to_make);
       }
    }
   
}       // end of inner class FutureCallback


/********************************************************************************/
/*                                                                              */
/*      Edit representation                                                     */
/*                                                                              */
/********************************************************************************/

protected interface BfixEdit {
   
   
   void doEdit(boolean format,boolean indent);
   
   void makeEdit(String pid);
   
   boolean unmakeEdit(String pid);
   
}       // end of interface BfixEdit



protected class BfixBaseEdit implements BfixEdit {
   
   private int start_offset;
   private int end_offset;
   private String insert_text;
   private String undo_text;
   private boolean can_undo;
   
   BfixBaseEdit(int soff,int eoff,String ins,String undo) {
      start_offset = soff;
      end_offset = eoff;
      insert_text = ins;
      undo_text = undo;
      can_undo = true;
    }
   
   BfixBaseEdit(int soff,int eoff,String ins) {
      start_offset = soff;
      end_offset = eoff;
      insert_text = ins;
      undo_text = null;
      can_undo = false;
    }
   
   @Override public void doEdit(boolean format,boolean indent) {
      BaleWindowDocument doc = for_corrector.getEditor().getWindowDocument();
      doc.replace(start_offset,end_offset - start_offset, insert_text,
            format,indent);
    }
   
   @Override public void makeEdit(String pid) {
      BaleWindowDocument doc = for_corrector.getEditor().getWindowDocument();
      String proj = doc.getProjectName();
      File file = doc.getFile();
      int soff = doc.mapOffsetToEclipse(start_offset);
      int eoff = doc.mapOffsetToEclipse(end_offset);
      BumpClient bc = BumpClient.getBump();
      bc.editPrivateFile(proj,file,pid,soff,eoff,insert_text);
    }
   
   
   @Override public boolean unmakeEdit(String pid) {
      if (!can_undo) return false;
      BaleWindowDocument doc = for_corrector.getEditor().getWindowDocument();
      String proj = doc.getProjectName();
      File file = doc.getFile();
      int soff = doc.mapOffsetToEclipse(start_offset);
      int eoff = end_offset;
      if (insert_text != null) eoff += insert_text.length();
      eoff = doc.mapOffsetToEclipse(eoff);
      String txt = undo_text;
      if (txt.isEmpty()) txt = null;
      BumpClient bc = BumpClient.getBump();
      bc.editPrivateFile(proj,file,pid,soff,eoff,txt);
      return true;
    }
   
}       // end of inner class BfixBaseEdit



protected class CheckAreas {

   private List<Point> check_areas;
   
   CheckAreas(int... bounds) {
      check_areas = new ArrayList<>();
      for (int i = 0; i+1 < bounds.length; i += 2) {
         check_areas.add(new Point(bounds[i],bounds[i+1]));
       }
    }
   
   List<Point> getAreas()                       { return check_areas; }

}       // end of inner class CheckAreas+


}       // end of class BfixFixer




/* end of BfixFixer.java */

