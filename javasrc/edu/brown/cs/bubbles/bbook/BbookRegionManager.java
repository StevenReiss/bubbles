/********************************************************************************/
/*										*/
/*		BbookRegionManager.java 					*/
/*										*/
/*	Manager of task regions for programmers notebook display		*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2009, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bbook;

import edu.brown.cs.bubbles.bnote.BnoteFactory;
import edu.brown.cs.bubbles.bnote.BnoteStore;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;



public class BbookRegionManager implements BbookConstants, BudaConstants, BumpConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<TaskRegion>	task_regions;
private boolean 		config_done;
private ChangeListener		change_listener;
private Map<Document,Boolean>	noedit_set;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BbookRegionManager()
{
   task_regions = new ArrayList<TaskRegion>();
   config_done = false;

   change_listener = new ChangeListener();

   noedit_set = new WeakHashMap<Document,Boolean>();

   BudaRoot.addBubbleViewCallback(new BubbleViewer());
   BumpClient.getBump().addChangeHandler(new FileHandler());
}



/********************************************************************************/
/*										*/
/*	Bubble management helper methods					*/
/*										*/
/********************************************************************************/

private static boolean isBubbleRelevant(BudaBubble bb)
{
   if (bb.isTransient() || bb.isFloating() || (bb.isFixed() && !bb.isUserPos())) return false;
   if (bb.getContentProject() == null) return false;
   if (bb.getContentName() == null) return false;
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bb);
   if (bba == null || !bba.isPrimaryArea()) return false;
   return true;
}




/********************************************************************************/
/*										*/
/*	Bubble view callback handler						*/
/*										*/
/********************************************************************************/

private class BubbleViewer implements BudaConstants.BubbleViewCallback
{
   @Override public void bubbleAdded(BudaBubble bb) {
      if (isBubbleRelevant(bb)) handleBubbleAdded(bb);
    }

   @Override public void bubbleRemoved(BudaBubble bb) {
      handleBubbleRemoved(bb);
    }

   @Override public void workingSetAdded(BudaWorkingSet ws) {
      if (isRelevant(ws)) handleWorkingSetAdded(ws);
    }

   @Override public void workingSetRemoved(BudaWorkingSet ws) {
      if (isRelevant(ws)) handleWorkingSetRemoved(ws);
    }

   @Override public void doneConfiguration() {
      config_done = true;
    }

   @Override public void copyFromTo(BudaBubble f,BudaBubble t) {
      handleBubbleCopy(f,t);
    }

   private boolean isRelevant(BudaWorkingSet ws) {
      BudaBubbleArea bba = ws.getBubbleArea();
      if (!bba.isPrimaryArea()) return false;
      return true;
    }

}	// end of inner class BubbleViewer



/********************************************************************************/
/*										*/
/*	Bump file change callback handler					*/
/*										*/
/********************************************************************************/

private class FileHandler implements BumpChangeHandler {

   @Override public void handleFileAdded(String proj,String file)	{ }
   @Override public void handleFileRemoved(String proj,String file)	{ }
   @Override public void handleFileStarted(String proj,String file)	{ }
   @Override public void handleProjectOpened(String proj)		{ }

   @Override public void handleFileChanged(String proj,String file) {
      noteFileChanged(new File(file));
    }

}	// end of inner class FileHandler




/********************************************************************************/
/*										*/
/*	Editing change callback handler 					*/
/*										*/
/********************************************************************************/

private class ChangeListener implements DocumentListener {

   @Override public void changedUpdate(DocumentEvent e) {
      noteDocumentChanged(e.getDocument());
    }

   @Override public void insertUpdate(DocumentEvent e) {
      noteDocumentChanged(e.getDocument());
    }

   @Override public void removeUpdate(DocumentEvent e) {
      noteDocumentChanged(e.getDocument());
    }

}	// end of inner class ChangeListener



/********************************************************************************/
/*										*/
/*	Locate task regions							*/
/*										*/
/********************************************************************************/

TaskRegion findTaskRegion(BudaBubble bb)
{
   if (bb == null) return null;

   synchronized (task_regions) {
      for (TaskRegion tr : task_regions) {
	 if (tr.contains(bb)) return tr;
       }
    }

   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bb);
   if (bba == null) return null;

   Rectangle r1 = BudaRoot.findBudaLocation(bb);
   if (r1 == null) return null;

   return findTaskRegion(bba,r1);
}


TaskRegion findTaskRegion(BudaBubbleArea bba,Rectangle r1)
{
   if (r1 == null) return null;

   synchronized (task_regions) {
      for (TaskRegion tr : task_regions) {
	 if (tr.contains(bba,r1)) return tr;
       }
    }

   return null;
}




/********************************************************************************/
/*										*/
/*	Task updating								*/
/*										*/
/********************************************************************************/

void handleSetTask(BnoteTask task,BudaBubbleArea bba,Rectangle r)
{
   TaskRegion tr = null;

   synchronized (task_regions) {
      tr = findTaskRegion(bba,r);
      if (tr == null) {
	 tr = new TaskRegion(bba,r);
	 if (tr.size() == 0) return;
	 task_regions.add(tr);
       }
    }

   BnoteTask otask = tr.getTask();

   tr.setTask(task);

   if (otask == null && task != null) {
      for (BudaBubble bb : tr.getBubbles()) {
	 String b1 = bb.getContentProject();
	 String b2 = bb.getContentName();
	 File f3 = bb.getContentFile();
	 if (b1 != null && b2 != null && f3 != null) {
	    BnoteStore.log(task.getProject(),task,BnoteEntryType.OPEN,"INPROJECT",b1,"NAME",b2,"FILE",f3.getPath());
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Task region updating							*/
/*										*/
/********************************************************************************/

private void handleBubbleAdded(BudaBubble bb)
{
   boolean create = false;
   TaskRegion tr = null;

   synchronized (task_regions) {
      tr = findTaskRegion(bb);
      if (tr == null) {
	 tr = new TaskRegion(bb);
	 task_regions.add(tr);
	 create =  true;
       }
      else tr.addBubble(bb);
    }

   boolean show = BoardProperties.getProperties("Bbook").getBoolean("Bbook.autoshow",true);
   if (create && config_done && show) {
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bb);
      if (bba != null) {
	 BbookFactory.getFactory().createTaskSelector(bba,bb,null,bb.getContentProject());
       }
    }

   BnoteTask task = tr.getTask();
   if (task != null && config_done) {
      String b1 = bb.getContentProject();
      String b2 = bb.getContentName();
      File f3 = bb.getContentFile();
      if (b1 != null && b2 != null && f3 != null) {
	 BnoteStore.log(task.getProject(),task,BnoteEntryType.OPEN,"INPROJECT",b1,"NAME",b2,"FILE",f3.getPath());
       }
    }

   Document d = getBubbleDocument(bb);
   if (d != null) noedit_set.put(d,Boolean.TRUE);
}




private Document getBubbleDocument(BudaBubble bb)
{
   if (bb == null) return null;
   return bb.getContentDocument();
}



private void handleBubbleRemoved(BudaBubble bb)
{
   TaskRegion tr = findTaskRegion(bb);
   if (tr == null) return;

   BnoteTask task = tr.getTask();
   if (task != null) {
      String b1 = bb.getContentProject();
      String b2 = bb.getContentName();
      File f3 = bb.getContentFile();
      if (b1 != null && b2 != null && f3 != null) {
	 BnoteStore.log(task.getProject(),task,BnoteEntryType.CLOSE,"INPROJECT",b1,"NAME",b2,"FILE",f3);
      }
    }

   Document d = getBubbleDocument(bb);
   if (d != null) noedit_set.remove(d);

   if (tr.removeBubble(bb)) {
      synchronized (task_regions) {
	 task_regions.remove(tr);
       }
    }
}



private void handleBubbleCopy(BudaBubble f,BudaBubble t)
{
   TaskRegion tr = findTaskRegion(t);
   if (tr == null) return;

   BnoteTask task = tr.getTask();
   if (task != null && f != null) {
      String b1 = t.getContentProject();
      String b2 = t.getContentName();
      File f3 = t.getContentFile();
      String c1 = f.getContentName();

      if (b1 != null && b2 != null && f3 != null && c1 != null) {
	 BnoteStore.log(task.getProject(),task,BnoteEntryType.COPY,"INPROJECT",b1,
			   "NAME",b2,"FILE",f3,"SOURCE",c1);
       }
    }
}



private void handleWorkingSetAdded(BudaWorkingSet ws)
{
   synchronized (task_regions) {
      for (TaskRegion tr : task_regions) {
	 if (tr.contains(ws)) {
	    tr.setWorkingSet(ws);
	  }
       }
    }
}


private void handleWorkingSetRemoved(BudaWorkingSet ws)
{
   synchronized (task_regions) {
      for (TaskRegion tr : task_regions) {
	 if (tr.getWorkingSet() == ws) {
	    tr.setWorkingSet(null);
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Updates for files and editing						*/
/*										*/
/********************************************************************************/

private void noteFileChanged(File f)
{
   List<TaskRegion> upds = new ArrayList<>();

   synchronized (task_regions) {
      for (TaskRegion tr : task_regions) {
	 if (tr.contains(f)) upds.add(tr);
       }
    }

   for (TaskRegion tr : upds) {
      BnoteTask task = tr.getTask();
      if (task == null) return;
      boolean used = false;
      for (BudaBubble bb : tr.getBubbles()) {
	 String b1 = bb.getContentProject();
	 String b2 = bb.getContentName();
	 File f3 = bb.getContentFile();
	 if (b1 != null && b2 != null && f3 != null && f3.equals(f)) {
	    Document d = getBubbleDocument(bb);
	    if (noedit_set.get(d) == Boolean.FALSE) {
	       BnoteStore.log(task.getProject(),task,BnoteEntryType.SAVE,"INPROJECT",b1,"NAME",b2,"FILE",f3.getPath());
	       noedit_set.put(d,Boolean.TRUE);
	     }
	    used = true;
	  }
       }
      if (!used) {
	 BnoteStore.log(task.getProject(),task,BnoteEntryType.SAVE,"FILE",f);
       }
    }
}



private void noteDocumentChanged(Document d)
{
   Boolean fg = noedit_set.get(d);
   noedit_set.put(d,Boolean.FALSE);
   if (fg != Boolean.TRUE) return;

   synchronized (task_regions) {
      for (TaskRegion tr : task_regions) {
	 BudaBubble bb = tr.contains(d);
	 if (bb == null) continue;
	 BnoteTask task = tr.getTask();
	 if (task == null) continue;
	 String b1 = bb.getContentProject();
	 String b2 = bb.getContentName();
	 File f3 = bb.getContentFile();
	 if (b1 != null && b2 != null && f3 != null) {
	    BnoteStore.log(task.getProject(),task,BnoteEntryType.EDIT,
		  "INPROJECT",b1,"NAME",b2,"FILE",f3);
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Task output and setup							*/
/*										*/
/********************************************************************************/

void outputTasks(BudaXmlWriter xw)
{
   List<TaskRegion> ltr = new ArrayList<TaskRegion>();

   synchronized (task_regions) {
      ltr.addAll(task_regions);
    }

   for (TaskRegion tr : ltr) {
      tr.outputXml(xw);
    }
}



void loadTasks(Element xml,BudaBubbleArea bba)
{

   for (Element e : IvyXml.children(xml,"REGION")) {
      int tid = IvyXml.getAttrInt(e,"TASK");
      BnoteTask task = BnoteFactory.getFactory().findTaskById(tid);
      if (task != null) {
	 Element ar = IvyXml.getChild(e,"AREA");
	 Rectangle r = new Rectangle(IvyXml.getAttrInt(ar,"X"),IvyXml.getAttrInt(ar,"Y"),
					IvyXml.getAttrInt(ar,"WIDTH"),IvyXml.getAttrInt(ar,"HEIGHT"));
	  TaskRegion tr = findTaskRegion(bba,r);
	   if (tr == null) {
	      tr = new TaskRegion(bba,r);
	      synchronized (task_regions) {
		 task_regions.add(tr);
	       }
	    }
	   tr.setTask(task);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Task region information 						*/
/*										*/
/********************************************************************************/

private class TaskRegion implements BbookRegion
{
   private BudaBubbleArea bubble_area;
   private BnoteTask region_task;
   private Rectangle region_area;
   private Set<BudaBubble> active_bubbles;
   private BudaWorkingSet working_set;
   private boolean is_active;

   TaskRegion(BudaBubble bb) {
      bubble_area = BudaRoot.findBudaBubbleArea(bb);
      region_task = null;
      region_area = bubble_area.computeRegion(bb);
      is_active = false;
      initializeBubbles(bb);
    }

   TaskRegion(BudaBubbleArea bba,Rectangle r) {
      bubble_area = bba;
      region_task = null;
      region_area = bubble_area.computeRegion(r);
      is_active = false;
      initializeBubbles(null);
    }

   private void initializeBubbles(BudaBubble b0) {
      active_bubbles = new HashSet<BudaBubble>();
      for (BudaBubble bb : bubble_area.getBubblesInRegion(region_area)) {
	 if (isBubbleRelevant(bb)) {
	    if (b0 == null) b0 = bb;
	    noteBubble(bb);
	  }
       }
      working_set = null;
      if (b0 != null) {
	 working_set = bubble_area.findWorkingSetForBubble(b0);
       }
    }

   int size()				{ return active_bubbles.size(); }
   @Override public BnoteTask getTask() { return region_task; }
   BudaWorkingSet getWorkingSet()	{ return working_set; }

   boolean contains(BudaBubble bb) {
      return active_bubbles.contains(bb);
    }

   boolean contains(BudaBubbleArea bba,Rectangle r1) {
      if (bba != bubble_area) return false;
      int space = bubble_area.getRegionSpace();
      if (r1.x + r1.width >= region_area.x - space &&
	     r1.x < region_area.x + region_area.width + space)
	 return true;
      return false;
    }

   boolean contains(BudaWorkingSet ws) {
      if (ws == working_set) return true;
      if (ws.getBubbleArea() != bubble_area) return false;
      Rectangle r1 = ws.getRegion();
      if (r1.x <= region_area.x + region_area.width && r1.x + r1.width >= region_area.x)
	 return true;
      return false;
    }

   boolean contains(File f) {
      if (f == null) return false;
      for (BudaBubble bb : active_bubbles) {
	 if (f.equals(bb.getContentFile())) return true;
       }
      return false;
    }

   BudaBubble contains(Document d) {
      if (d == null) return null;
      for (BudaBubble bb : active_bubbles) {
	 Document d1 = getBubbleDocument(bb);
	 if (d1 == d) return bb;
       }
      return null;
    }

   void addBubble(BudaBubble bb) {
      region_area = bubble_area.computeRegion(bb);
      noteBubble(bb);
    }

   void noteBubble(BudaBubble bb) {
      is_active = true;
      active_bubbles.add(bb);
      Document d = bb.getContentDocument();
      if (d != null) d.addDocumentListener(change_listener);
    }

   boolean removeBubble(BudaBubble nbb) {
      if (!active_bubbles.remove(nbb)) return false;
      Rectangle r0 = null;
      for (BudaBubble bb : active_bubbles) {
	 Rectangle r1 = BudaRoot.findBudaLocation(bb);
	 if (r1 == null) continue;
	 if (r0 == null) r0 = new Rectangle(r1);
	 else r0 = r0.union(r1);
       }
      if (r0 == null) return is_active;
      region_area = bubble_area.computeRegion(r0);
      return false;
    }

   void setWorkingSet(BudaWorkingSet ws)		{ working_set = ws; }

   void setTask(BnoteTask task) 			{ region_task = task; }

   List<BudaBubble> getBubbles() {
      return new ArrayList<>(active_bubbles);
    }

   void outputXml(BudaXmlWriter xw) {
      if (region_task != null) {
	 xw.begin("REGION");
	 xw.field("TASK",region_task.getTaskId());
	 xw.element("AREA",region_area);
	 xw.end("REGION");
       }
    }

}	// end of inner class TaskRegion



}	// end of class BbookRegionManager




/* end of BbookRegionManager.java */

