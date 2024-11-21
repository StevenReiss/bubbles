/********************************************************************************/
/*										*/
/*		BbookFactory.java						*/
/*										*/
/*	Factory for setting up programmers notebook display			*/
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

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.AbstractAction;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;




public class BbookFactory implements BbookConstants, BudaConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BbookTasker		task_manager;
private BbookRegionManager	region_manager;
private List<String>		all_projects;
private BudaRoot		buda_root;

private static BbookFactory	the_factory = new BbookFactory();




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BbookFactory()
{
   task_manager = null;
   region_manager = null;
   all_projects = null;
   buda_root = null;

   if (isEnabled()) {
      task_manager = new BbookTasker();
      region_manager = new BbookRegionManager();
    }
}



public static BbookFactory getFactory() 	{ return the_factory; }



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

public static void setup()
{
   if (isEnabled()) {
      BudaRoot.addBubbleConfigurator("BBOOK",new TaskConfig());
      BudaRoot.registerMenuButton("Show Programmer's Log",new LogShow());
    }
}


public static void initialize(BudaRoot br)
{
   the_factory.buda_root = br;

   if (isEnabled()) {
      br.registerKeyAction(new BbookAction(br),"Programmer's Log","shift F2");
    }
}


private static boolean isEnabled()
{
   if (!BnoteFactory.getFactory().isEnabled()) return false;

   BoardProperties bp = BoardProperties.getProperties("Bbook");

   return bp.getBoolean("Bbook.enable",true);
}



/********************************************************************************/
/*										*/
/*	Logging methods 							*/
/*										*/
/********************************************************************************/

public void log(Component src,BnoteEntryType type,Object... args)
{
   if (src == null || !isEnabled()) return;
   BudaBubble bb = BudaRoot.findBudaBubble(src);
   if (bb == null) return;
   BbookRegion tr = region_manager.findTaskRegion(bb);
   if (tr == null) return;
   BnoteTask task = tr.getTask();
   if (task == null) return;
   BnoteStore.log(task.getProject(),task,type,args);
}



public BnoteTask getCurrentTask(Component src)
{
   if (buda_root == null) return null;

   if (src == null || !isEnabled()) return null;
   BudaBubble bb = BudaRoot.findBudaBubble(src);
   if (bb == null) return null;
   BbookRegion tr = region_manager.findTaskRegion(bb);
   if (tr == null) return null;
   return tr.getTask();
}




public BnoteTask getCurrentTask()
{
   if (buda_root == null) return null;

   BudaBubbleArea bba = buda_root.getCurrentBubbleArea();
   Rectangle r = bba.getViewport();
   BbookRegion tr = region_manager.findTaskRegion(bba,r);
   if (tr == null) return null;
   return tr.getTask();
}




/********************************************************************************/
/*										*/
/*	Bubble creation methods 						*/
/*										*/
/********************************************************************************/

void createTaskSelector(BudaBubbleArea bba,BudaBubble bb,Point pt,String proj)
{
   task_manager.createTaskSelector(bba,bb,pt,proj);
}


BbookRegion findTaskRegion(BudaBubbleArea bba,Rectangle r0)
{
   return region_manager.findTaskRegion(bba,r0);
}



/********************************************************************************/
/*										*/
/*	Updating methods							*/
/*										*/
/********************************************************************************/

void handleSetTask(BnoteTask task,BudaBubbleArea bba,Rectangle loc)
{
   region_manager.handleSetTask(task,bba,loc);
}



synchronized List<String> getProjects()
{
   if (all_projects != null) return all_projects;

   all_projects = new ArrayList<String>();

   Element pxml = BumpClient.getBump().getAllProjects();
   if (pxml != null) {
      Set<String> pset = new TreeSet<String>();
      for (Element pe : IvyXml.children(pxml,"PROJECT")) {
	 String pnm = IvyXml.getAttrString(pe,"NAME");
	 pset.add(pnm);
       }
      all_projects.addAll(pset);
    }

   return all_projects;
}




/********************************************************************************/
/*										*/
/*	Task request action							*/
/*										*/
/********************************************************************************/

private static class BbookAction extends AbstractAction {

   private BudaRoot buda_root;
   private static final long serialVersionUID = 1;
   
   BbookAction(BudaRoot br) {
      super("ProgrammersLogAction");
      buda_root = br;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      if (!isEnabled()) return;
      BudaBubbleArea bba = buda_root.getCurrentBubbleArea();
      if (!bba.isPrimaryArea()) return;
      Point pt = bba.getCurrentMouse();
      Rectangle r0 = new Rectangle(pt);
      BbookRegion tr = getFactory().findTaskRegion(bba,r0);
      if (tr == null) {
	 getFactory().task_manager.createTaskSelector(bba,null,pt,null);
       }
      else if (tr.getTask() != null) {
	 getFactory().task_manager.createTaskNoter(bba,null,pt,tr.getTask().getProject(),tr.getTask());
       }
      else {
	 getFactory().task_manager.createTaskSelector(bba,null,pt,null);
       }
    }

}	// end of inner class BbookAction




/********************************************************************************/
/*										*/
/*	Configurator for saving task set					*/
/*										*/
/********************************************************************************/

private static class TaskConfig implements BubbleConfigurator {

   @Override public BudaBubble createBubble(BudaBubbleArea bba,Element xml) {
      return null;
    }

   @Override public boolean matchBubble(BudaBubble bb,Element xml) {
      if (bb instanceof BbookDisplayBubble) return true;
      return false;
    }

   @Override public void outputXml(BudaXmlWriter xw,boolean history) {
      if (history) return;
      xw.begin("BBOOK");
      getFactory().region_manager.outputTasks(xw);
      xw.end("BBOOK");
    }

   @Override public void loadXml(BudaBubbleArea bba,Element root) {
      if (bba == null) return;		// history loading
      Element e = IvyXml.getChild(root,"BBOOK");
      if (e == null) return;
      getFactory().region_manager.loadTasks(e,bba);
    }

}	// end of inner class TaskConfig



/********************************************************************************/
/*										*/
/*	Handle menu buttons							*/
/*										*/
/********************************************************************************/

private static class LogShow implements ButtonListener {

   LogShow() { }

   @Override public void buttonActivated(BudaBubbleArea bba,String id,Point pt) {
      BudaBubble bb = new BbookQueryBubble(bba,pt);
      bba.addBubble(bb,null,pt,BudaConstants.PLACEMENT_EXPLICIT);
    }

}	// end of inner class LogShow




}	// end of class BbookFactory




/* end of BbookFactory.java */

