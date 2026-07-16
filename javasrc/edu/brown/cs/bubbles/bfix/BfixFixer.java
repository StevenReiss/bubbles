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

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleWindowDocument;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpProblem;

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.SwingUtilities;

import org.w3c.dom.Element;

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



protected List<BfixEdit> findPrivateEdits(Collection<BfixEdit> edits,
      BfixCheckAreas safepos,BfixCheckAreas probareas) 
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
         Boolean fg = checkOneEdit(bc,pid,filename,doc,edit,
               safepos,probareas);
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


protected Boolean checkPrivateEdit(BfixEdit edit,BfixCheckAreas safepos,
      BfixCheckAreas probareas,Boolean dflt)
{
   BaleWindowDocument doc = for_corrector.getEditor().getWindowDocument();
   String proj = doc.getProjectName();
   BumpClient bc = BumpClient.getBump();
   File file = doc.getFile();
   String filename = file.getAbsolutePath();
   String pid = bc.createPrivateBuffer(proj, filename, null);
   try {
      Boolean fg = checkOneEdit(bc,pid,filename,doc,edit,safepos,probareas);
      if (fg == null) fg = dflt;
      return fg;
    }
   finally {
      bc.removePrivateBuffer(proj,filename,pid);
    }
}


private Boolean checkOneEdit(BumpClient bc,String pid,String filename,
      BaleWindowDocument doc,BfixEdit edit,
      BfixCheckAreas safepos,BfixCheckAreas probareas)
{
   Collection<BumpProblem> probs = bc.getPrivateProblems(filename, pid);
   if (probs == null) return false;
   int probct = BfixAdapter.getErrorCount(probs);
   if (!BfixAdapter.checkProblemPresent(for_problem,probs)) return false;
   
   if (safepos != null) {
      for (Point p : safepos.getAreas()) {
         if (!BfixAdapter.checkSafePosition(for_corrector,p.x,p.y)) {
            return false;
          }
       }
    }
   
   bc.beginPrivateEdit(filename, pid);
   edit.makeEdit(pid);
   probs = bc.getPrivateProblems(filename,pid);
   
   if (edit.unmakeEdit(pid)) {
      bc.getPrivateProblems(filename,pid);
    }
   
   if (probs == null || BfixAdapter.getErrorCount(probs) > probct) {
      return false;
    }
   if (probareas != null) {
      for (Point p : probareas.getAreas()) {
         int px = p.x;
         if (px < 0) px = edit.getDelta();
         int py = p.y;
         if (py < 0) py = edit.getDelta();
         if (BfixAdapter.checkAnyProblemPresent(for_problem,probs,
              px,py)) {
            return false;
          }
       }
    }
   if (BfixAdapter.getErrorCount(probs) == probct) {
      return null;
    }
   
   return true;
}



/********************************************************************************/
/*                                                                              */
/*      Edit representation                                                     */
/*                                                                              */
/********************************************************************************/

public static class BfixBaseEdit implements BfixEdit {
   
   private BfixCorrector for_corrector;
   private int start_offset;
   private int end_offset;
   private String insert_text;
   private String undo_text;
   private boolean can_undo;
   
   public BfixBaseEdit(BfixCorrector corr,int soff,int eoff,String ins,String undo) {
      for_corrector = corr;
      start_offset = soff;
      end_offset = eoff;
      insert_text = ins;
      undo_text = undo;
      can_undo = true;
    }
   
   public BfixBaseEdit(BfixCorrector corr,int soff,int eoff,String ins) {
      for_corrector = corr;
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
   
   @Override public boolean makeEdit(String pid) {
      BaleWindowDocument doc = for_corrector.getEditor().getWindowDocument();
      String proj = doc.getProjectName();
      File file = doc.getFile();
      int soff = doc.mapOffsetToEclipse(start_offset);
      int eoff = doc.mapOffsetToEclipse(end_offset);
      BumpClient bc = BumpClient.getBump();
      bc.editPrivateFile(proj,file,pid,soff,eoff,insert_text);
      return true;
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
   
   @Override public int getDelta() {
      int delta = start_offset - end_offset;
      if (insert_text != null) delta += insert_text.length();
      return delta;
    }
   
}       // end of inner class BfixBaseEdit



public static class BfixGroupEdits implements BfixEdit {
   
   private BfixCorrector for_corrector;
   private Element edit_set;
   
   public BfixGroupEdits(BfixCorrector corr,Element xml) {
      for_corrector = corr;
      edit_set = xml;
    }
   
   @Override public void doEdit(boolean fmt,boolean indent) {
      BaleWindowDocument doc = for_corrector.getEditor().getWindowDocument();
      File file = doc.getFile();
      BaleFactory.getFactory().applyEdits(file,edit_set);
    }

   @Override public boolean makeEdit(String pid)        { return false; }
   
   @Override public boolean unmakeEdit(String pid)      { return false; }
   
}       // end of inner class BfixGroupEdits



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


}       // end of class BfixFixer




/* end of BfixFixer.java */

