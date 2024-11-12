/********************************************************************************/
/*										*/
/*		BfixCorrector.java						*/
/*										*/
/*	Corrector to be attached to an editor					*/
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
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;



public final class BfixCorrector implements BfixConstants, BaleConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BaleWindow		for_editor;
private BaleWindowDocument	for_document;
private DocHandler		event_handler;
private ProblemHandler		problem_handler;
private BfixSmartInsert 	smart_inserter;

private int			start_offset;
private int			end_offset;
private long			start_time;
private int			caret_position;
private Set<BumpProblem>	active_problems;
private String			bubble_id;

private Set<BfixMemo>		pending_fixes;

private static int	MAX_REGION_SIZE = 150;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BfixCorrector(BaleWindow ed,boolean auto)
{
   for_editor = ed;
   for_document = ed.getWindowDocument();
   event_handler = null;
   problem_handler = null;
   bubble_id = null;

   start_offset = -1;
   end_offset = -1;
   start_time = 0;
   caret_position = -1;
   active_problems = new ConcurrentSkipListSet<>(new ProblemComparator());
   pending_fixes = new ConcurrentSkipListSet<>();

   smart_inserter = new BfixSmartInsert(this);

   if (auto) {
      event_handler = new DocHandler();
      problem_handler = new ProblemHandler();
      for_document.addDocumentListener(event_handler);
      for_editor.addCaretListener(event_handler);
      BumpClient.getBump().addProblemHandler(for_document.getFile(),problem_handler);
    }
}



void removeEditor()
{
   if (event_handler != null) {
      for_editor.removeCaretListener(event_handler);
      for_document.removeDocumentListener(event_handler);
      BumpClient.getBump().removeProblemHandler(problem_handler);
      event_handler = null;
      problem_handler = null;
    }
   BfixFactory.getFactory().getChoreManager().removeCorrector(this);
}




/********************************************************************************/
/*										*/
/*	Handle popup menu for local fixes					*/
/*										*/
/********************************************************************************/

void addPopupMenuItems(BaleContextConfig ctx,JPopupMenu menu)
{
   clearRegion();

   BaleWindowDocument bd = (BaleWindowDocument) ctx.getDocument();
   List<BumpProblem> probs = bd.getProblemsAtLocation(ctx.getOffset());
   if (probs != null) {
      boolean add = false;
      for (BumpProblem bp : probs) {
         String name = getFixForProblem(bp);
         if (name != null) {
            FixAction act = new FixAction(name,ctx.getOffset(),ctx.getOffset()+1);
            menu.add(act);
            add = true;
            break;
          }
       }
      if (add) return;
    }
   
   
   BumpClient bc = BumpClient.getBump();
   probs = bc.getProblems(for_document.getFile());
   int haveprobs = 0;
   int startoff = ctx.getSelectionStart();
   int endoff = ctx.getSelectionEnd();
   String fixname = null;
   for (Iterator<BumpProblem> it = probs.iterator(); it.hasNext(); ) {
      BumpProblem bp = it.next();
      fixname = getFixForProblem(bp);
      if (fixname != null) {
         int soff = for_document.mapOffsetToJava(bp.getStart());
         int eoff = for_document.mapOffsetToJava(bp.getEnd());
         if (eoff >= startoff && soff <= endoff) {
            ++haveprobs;
          }
       }
    }
   if (haveprobs == 1) {
      FixAction act = new FixAction(fixname,startoff,endoff+1);
      menu.add(act);
      return;
    }
   else if (haveprobs > 0) {
      FixAction act = new FixAction(null,startoff,endoff+1);
      menu.add(act);
      return;
    }
}



private String getFixForProblem(BumpProblem bp)
{
   BfixFactory fixfactory = BfixFactory.getFactory();
   String name = null;
   for (BfixAdapter fixadapt : fixfactory.getAdapters()) {
      String fixname = fixadapt.getMenuAction(this,bp);
      if (fixname != null) {
         if (name == null) name = fixname;
         else if (name.length() == 0 && fixname.length() > 0) name = fixname;
       }
    }
   return name;
}



private class FixAction extends AbstractAction {

   private int start_correct;
   private int end_correct;

   private static final long serialVersionUID = 1;

   FixAction(String name,int soff,int eoff) {
      super(getFixActionName(name));
      start_correct = soff;
      end_correct = eoff;
    }

   @Override public void actionPerformed(ActionEvent e) {
      fixErrorsInRegion(start_correct,end_correct,true);
    }

}	// end of inner class FixAction


private static String getFixActionName(String name) {
   if (name == null) return "Auto Fix Problems in Selection";
   else if (name.length() == 0) return "Auto Fix";
   else return "Auto Fix '" + name + "'";
}






/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public BaleWindow getEditor()			{ return for_editor; }

int getEndOffset()			{ return end_offset; }

public long getStartTime()		{ return start_time; }

int getCaretPosition()			{ return caret_position; }

public String getBubbleId()
{
   if (bubble_id == null) {
      BudaBubble bbl = for_editor.getBudaBubble();
      if (bbl != null) bubble_id = bbl.getHashId();
      else bubble_id = "?";
    }
   return bubble_id;
}


BfixSmartInsert getInserter()
{
   return smart_inserter;
}


/********************************************************************************/
/*										*/
/*	Edit commands								*/
/*										*/
/********************************************************************************/

void fixErrorsInRegion(int startoff,int endoff,boolean force)
{
   Set<BumpProblem> done = new HashSet<BumpProblem>();

   // need to maintain region bounds correctly after fixes

   for ( ; ; ) {
      List<BumpProblem> totry = new ArrayList<>();
      List<BumpProblem> sytletry = new ArrayList<>();
      BumpClient bc = BumpClient.getBump();
      List<BumpProblem> probs = bc.getProblems(for_document.getFile());
      BoardLog.logD("BFIX","Found " + probs.size() + " problems");
      if (probs.isEmpty()) return;
      for (Iterator<BumpProblem> it = probs.iterator(); it.hasNext(); ) {
	 BumpProblem bp = it.next();
	 if (done.contains(bp)) continue;
	 int soff = for_document.mapOffsetToJava(bp.getStart());
	 int eoff = for_document.mapOffsetToJava(bp.getEnd());
	 if (eoff < startoff || soff > endoff) {
	    it.remove();
	    continue;
	 }
         if (bp.getCategory().equals("BSTYLE")) sytletry.add(bp);
	 else totry.add(bp);
      }

      if (totry.isEmpty() && sytletry.isEmpty()) return;

      boolean fnd = false;
      for (BumpProblem bp : totry) {
         BoardLog.logD("BFIX","Work on problem " + bp);
	 RegionFixer fx = new RegionFixer(bp);
	 checkProblemFixable(fx);
	 BfixRunnableFix rslt = fx.waitForDone();
         
	 if (rslt != null) {
	    BoardMetrics.noteCommand("BFIX","UserCorrect_" + getBubbleId());
	    RunAndWait rw = new RunAndWait(rslt);
            rw.runFix();
	    done.add(bp);
	    if (rw.waitForDone()) {
               fnd = true;
               break;
             }	
          }
      }
      for (BumpProblem bp : sytletry) {
         BoardLog.logD("BFIX","Work on style problem " + bp);
         BfixRunnableFix rslt = checkStyleProblemFixable(bp,force); 
         if (rslt != null) { 
	    BoardMetrics.noteCommand("BFIX","UserCorrect_" + getBubbleId());
	    RunAndWait rw = new RunAndWait(rslt);
            rw.runFix();
	    done.add(bp);
	    if (rw.waitForDone()) {
               fnd = true;
               break;
             }	
          }      
       }
      if (!fnd) return;
      // need to wait for errors to change here
    }
}





void checkProblemFixable(FixAdapter subfix)
{
   BfixFactory fixfac = BfixFactory.getFactory();
   List<BfixFixer> fixes = new ArrayList<>();
   for (BfixAdapter fixadapt : fixfac.getAdapters()) {
      fixadapt.addFixers(this,subfix.getProblem(),true,fixes);
    }
   subfix.noteFixersAdded(fixes.size());
   for (BfixFixer fis : fixes) {
      fis.setSubFixData(subfix);
      fixfac.startTask(fis);
    }
}


BfixRunnableFix checkStyleProblemFixable(BumpProblem bp,boolean explicit)
{
   BfixFactory fixfac = BfixFactory.getFactory();
   for (BfixAdapter fixadapt : fixfac.getAdapters()) {
      BfixRunnableFix rf = fixadapt.findStyleFixer(this,bp,explicit); 
      if (rf != null) return rf;
    }
   
   return null;
}




private class RunAndWait implements BumpProblemHandler {

   private BfixRunnableFix fixer_run;
   private boolean  is_done;
   private boolean  done_status;

   RunAndWait(BfixRunnableFix r) {
      fixer_run = r;
      is_done = false;
      done_status = false;
    }

  void runFix() {
     is_done = false;
     done_status = false;
     BumpClient.getBump().addProblemHandler(for_document.getFile(),this);
     try {
        if (SwingUtilities.isEventDispatchThread()) {
           done_status = fixer_run.call();
         }
        else {
           SwingRunner sr = new SwingRunner(fixer_run);
           SwingUtilities.invokeAndWait(sr);
           done_status = sr.getResult();
         }
      }
     catch (Exception e) {
        done_status = false;
      }
     
     if (!done_status) handleProblemsDone();
   }

   synchronized boolean waitForDone() {
      if (!is_done) {
         try {
            wait(20000);
          }
         catch (InterruptedException e) { }
       }
      return done_status;
   }

   @Override public void handleProblemRemoved(BumpProblem bp)	{ }
   @Override public void handleProblemAdded(BumpProblem bp)	{ }
   @Override public void handleClearProblems()			{ }

   @Override public void handleProblemsDone() {
      synchronized (this) {
         is_done = true;
         notifyAll();
      }
      BumpClient.getBump().removeProblemHandler(this);
   }

}	// end of inner class RunAndWait

private class SwingRunner implements Runnable {

   private BfixRunnableFix fixer_run;
   private boolean fixer_result;
   
   SwingRunner(BfixRunnableFix rf) {
      fixer_run = rf;
      fixer_result = false;
    }
   
   boolean getResult()                  { return fixer_result; }
   
   @Override public void run() {
      try {
         fixer_result = fixer_run.call();
       }
      catch (Exception e) {
         fixer_result = false;
       }
    }
   
}       // end of inner class SwingRunner




/********************************************************************************/
/*										*/
/*	Region management							*/
/*										*/
/********************************************************************************/

private void clearRegion()
{
   if (start_offset == -1) return;

   BoardLog.logD("BFIX", "Clear corrector region");
   start_offset = -1;
   end_offset = -1;
   start_time = 0;
   caret_position = -1;
   active_problems.clear();
   BoardMetrics.noteCommand("BFIX","ClearRegion");
}


private void handleTyped(int off,int len)
{
   if (!checkFocus()) return;

   caret_position = off+len;

   if (start_offset < 0) {
      int lno = for_document.findLineNumber(off);
      start_offset = for_document.findLineOffset(lno);
      end_offset = start_offset+1;
      start_time = System.currentTimeMillis();
      BoardMetrics.noteCommand("BFIX","StartRegion");
    }

   end_offset = Math.max(end_offset+len,caret_position);

   while (end_offset - start_offset > MAX_REGION_SIZE) {
      int lno = for_document.findLineNumber(start_offset);
      int npos = for_document.findLineOffset(lno+1);
      if (npos < 0) break;
      else if (npos < end_offset) {
	 if (npos <= start_offset) break;
	 start_offset = npos;
       }
      else break;
    }
}


private void handleBackspace(int off)
{
   if (start_offset < 0) return;
   if (!checkFocus()) return;

   caret_position = off;
   start_offset = Math.min(start_offset,caret_position);
   // end_offset = Math.max(start_offset,end_offset-1);
}



private void addProblem(BumpProblem bp)
{
   if (start_offset < 0) return;

   int soff = for_document.mapOffsetToJava(bp.getStart());
   if (soff < 0) {
      BoardLog.logD("BFIX","Problem has no offset: " + bp.getStart() + " " + bp.getEnd() + 
            " " + bp.getLine());
      return;
    }

   BoardLog.logD("BFIX","PROBLEM "+ bp.getMessage() + " " + start_offset + " " + 
         end_offset + " " + soff);
   
   if (start_offset >= 0 && soff >= start_offset && soff <= end_offset) {
      BoardLog.logD("BFIX","Consider problem " + bp.getMessage());
      active_problems.add(bp);
    }
}


private void removeProblem(BumpProblem bp)
{
   active_problems.remove(bp);
}


private boolean checkFocus()
{
   BudaBubble bbl = for_editor.getBudaBubble();
   if (bbl == null) {
      clearRegion();
      return false;
    }
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bbl);
   if (bba.getFocusBubble() == bbl) return true;
   clearRegion();
   return false;
}




/********************************************************************************/
/*										*/
/*	Find something to fix							*/
/*										*/
/********************************************************************************/

private void checkForElementToFix()
{
   List<BumpProblem> totry = new ArrayList<>();
   List<BumpProblem> styletry = new ArrayList<>();

   if (start_offset < 0) return;
   if (active_problems.isEmpty()) return;
   for (Iterator<BumpProblem> it = active_problems.iterator(); it.hasNext(); ) {
      BumpProblem bp = it.next();
      int soff = for_document.mapOffsetToJava(bp.getStart());
      if (soff < start_offset) {
	 it.remove();
	 continue;
       }
      if (bp.getCategory().equals("BSTYLE")) styletry.add(bp);
      else totry.add(bp);
    }

   if (totry.isEmpty() && styletry.isEmpty()) return;

   BfixFactory fixfactory = BfixFactory.getFactory();
   List<BfixFixer> fixers = new ArrayList<BfixFixer>();
   List<BfixChore> chores = new ArrayList<BfixChore>();
   int numfnd = 0;
   for (BumpProblem bp : totry) {
      for (BfixAdapter fixadapt : fixfactory.getAdapters()) {
	 fixadapt.addFixers(this,bp,false,fixers);
	 fixadapt.addChores(this,bp,chores);
       }
      if (!fixers.isEmpty()) {
	 for (BfixFixer fix : fixers) {
	    if (addPending(fix.getMemo())) {
	       ++numfnd;
               fixfactory.startTask(fix);
	     }
	    else {
	       BoardLog.logD("BFIX", "Discard duplicate fix " + fix);
	     }
	  }
	 break;
       }
      else if (!chores.isEmpty()) {
	 ChoreAdder adder = new ChoreAdder(chores);
	 fixfactory.startTask(adder);
       }
      else {
	 recordError(bp);
       }
    }
   for (BumpProblem bp : styletry) {
      BfixRunnableFix fix = checkStyleProblemFixable(bp,false);
      if (fix != null) {
         RunAndWait rw = new RunAndWait(fix);
         rw.runFix();
         break;
       }
    }
   
   if (numfnd > 0) BoardMetrics.noteCommand("BFIX","StartImplicitFix" + "_" + numfnd +
	 "_" + start_offset + "_" + end_offset + "_" + caret_position);
}



private synchronized void recordError(BumpProblem bp)
{
   int soff = for_document.mapOffsetToJava(bp.getStart());
   int lnoerr = for_document.findLineNumber(soff);
   int lnocur = for_document.findLineNumber(caret_position);
   if (lnoerr+1 == lnocur && BfixAdapter.checkProblemPresent(bp,active_problems)) {
      try {
	 if (lnoerr > 1) lnoerr -= 1;
	 int lstart = for_document.findLineOffset(lnoerr);
	 int lend = for_document.findLineOffset(lnocur+1);
	 String s1 = for_document.getWindowText(lstart,lend-lstart);
	 if (s1 == null) s1 = "???";
	 File f = new File("/vol/spr/fixbugs");
	 PrintWriter pw = new PrintWriter(new FileWriter(f,true));
	 if (BfixAdapter.checkProblemPresent(bp, active_problems)) {
	    pw.println("------------------------");
	    pw.println(bp.getMessage());
	    pw.println(soff + " " + bp.getStart() + " " + bp.getEnd() + " " + bp.getLine() + " " +
		     bp.getProblemId() + " " + bp.getErrorType() + " " + bp.getFile());
	    pw.println(start_offset + " " + caret_position + " " + end_offset + " " +
		     start_time + " " + lstart + " " + lnoerr + " " + lnocur);
	    pw.println(s1);
	    pw.close();
	 }
       }
      catch (IOException e) { }
    }
}



/********************************************************************************/
/*										*/
/*	Handle editor events							*/
/*										*/
/********************************************************************************/

private class DocHandler implements DocumentListener, CaretListener {

   @Override public void changedUpdate(DocumentEvent e) {
      int len = e.getLength();
      int dlen = e.getDocument().getLength();
      if (len != dlen) {
	 BoardLog.logD("BFIX","SPELL: Clear for changed update");
	 clearRegion();
       }
    }

   @Override public void insertUpdate(DocumentEvent e) {
      int off = e.getOffset();
      int len = e.getLength();
      if (len == 0) return;
      else if (start_offset < 0 && len == 1) {
	 handleTyped(off,1);
       }
      else if (start_offset > 0 && off >= start_offset-2 && off <= end_offset+2 && len < 128) {
	 handleTyped(off,len);
	 SwingUtilities.invokeLater(new Checker());
       }
      else {
	 BoardLog.logD("BFIX","SPELL: Clear for insert update");
	 clearRegion();
       }
      smart_inserter.noteChange();
    }

   @Override public void removeUpdate(DocumentEvent e) {
      int off = e.getOffset();
      int len = e.getLength();
      if (len == 1) {
	 handleBackspace(off);
       }
      else if (off == caret_position -1) {
	 for (int i = 0; i < len; ++i) {
	    handleBackspace(off);
	 }
      }
      else {
	 BoardLog.logD("BFIX","SPELL: Clear for remove update");
	 clearRegion();
       }
      smart_inserter.noteChange();
    }

   @Override public void caretUpdate(CaretEvent e) {
      int off = e.getDot();
      if (off == caret_position) return;
      if (caret_position < 0) return;
      if (off >= start_offset && off <= end_offset+2) {
	 caret_position = off;
	 return;
       }
      BoardLog.logD("BFIX","SPELL: Clear for caret update");
      clearRegion();
    }

}	// end of inner class DocHandler





/********************************************************************************/
/*										*/
/*	Handle Compilation Events						*/
/*										*/
/********************************************************************************/

private class ProblemHandler implements BumpConstants.BumpProblemHandler {

   @Override public void handleProblemAdded(BumpProblem bp) {
      addProblem(bp);
    }

   @Override public void handleProblemRemoved(BumpProblem bp) {
      removeProblem(bp);
    }

   @Override public void handleClearProblems() {
      active_problems.clear();
    }

   @Override public void handleProblemsDone() {
      SwingUtilities.invokeLater(new Checker());
    }

}	// end of inner class ProblemHandler



private class ProblemComparator implements Comparator<BumpProblem> {

   @Override public int compare(BumpProblem p1,BumpProblem p2) {
      int d = p1.getStart() - p2.getStart();
      if (d < 0) return -1;
      if (d > 0) return 1;
      return p1.getProblemId().compareTo(p2.getProblemId());
    }
}


private class Checker implements Runnable {

   @Override public void run() {
      checkForElementToFix();
    }

}	// end of inner class Checker



/********************************************************************************/
/*										*/
/*	Runnable for adding a set of chores					*/
/*										*/
/********************************************************************************/

private class ChoreAdder implements Runnable {

   private List<BfixChore> chore_list;

   ChoreAdder(List<BfixChore> todos) {
      chore_list = new ArrayList<BfixChore>(todos);
      Collections.reverse(chore_list);
    }

   @Override public void run() {
      BfixChoreManager tm = BfixFactory.getFactory().getChoreManager();
      for (BfixChore t : chore_list) {
	 if (t.validate(false)) tm.addChore(t);
       }
    }

}	// end of inner class ChoreAdder




/********************************************************************************/
/*										*/
/*	Keep track of pending fixes						*/
/*										*/
/********************************************************************************/

boolean addPending(BfixMemo bm)
{
   return pending_fixes.add(bm);
}


void removePending(BfixMemo bm)
{
   pending_fixes.remove(bm);
}




/********************************************************************************/
/*										*/
/*	Handle fixes for region 						*/
/*										*/
/********************************************************************************/

private static class RegionFixer implements FixAdapter {

   private BumpProblem the_problem;
   private BfixRunnableFix fix_found;
   private int num_fixers;
   private boolean is_done;

   RegionFixer(BumpProblem bp) {
      the_problem = bp;
      fix_found = null;
      num_fixers = 0;
    }

   @Override public BumpProblem getProblem()			{ return the_problem; }

   @Override public void noteFixersAdded(int ct) {
      if (ct <= 0) noteDone();
      else num_fixers += ct;
    }

   @Override public void noteStatus(boolean fg) {
      if (--num_fixers == 0) noteDone();
    }

   @Override public String getPrivateBufferId() 		{ return null; }
   @Override synchronized public void noteFix(BfixRunnableFix fix) {
      if (fix_found == null && fix != null) fix_found = fix;
      else if (fix_found != null && fix != null) {
         if (fix_found.getPriority() < fix.getPriority()) {
            fix_found = fix;
          }
       }
      noteStatus(fix != null);
    }

   synchronized BfixRunnableFix waitForDone() {
      while (!is_done && fix_found == null) {
         try {
            wait(5000);
          }
         catch (InterruptedException e) { }
       }
      return fix_found;
    }

   private synchronized void noteDone() {
      is_done = true;
      notifyAll();
    }

}	// end of inner class RegionFixer


}	// end of class BfixCorrector




/* end of BfixCorrector.java */

