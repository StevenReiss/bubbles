/********************************************************************************/
/*										*/
/*		BbookTasker.java						*/
/*										*/
/*	Create bubbles for task-related data entry				*/
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

import edu.brown.cs.bubbles.bnote.BnoteConstants;
import edu.brown.cs.bubbles.bnote.BnoteFactory;
import edu.brown.cs.bubbles.bnote.BnoteStore;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;

import edu.brown.cs.ivy.swing.SwingComboBox;
import edu.brown.cs.ivy.swing.SwingGridPanel;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


class BbookTasker implements BbookConstants, BudaConstants, BnoteConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BbookTasker()
{
}



/********************************************************************************/
/*										*/
/*	Create task selection bubble						*/
/*										*/
/********************************************************************************/

void createTaskSelector(BudaBubbleArea bba,BudaBubble bb,Point pt,String proj)
{
   TaskSelector tsc = new TaskSelector(proj);
   TaskBubble nbb = new TaskBubble(tsc);

   bba.addBubble(nbb,bb,pt,PLACEMENT_LEFT|PLACEMENT_ADJACENT|PLACEMENT_EXPLICIT|PLACEMENT_LOGICAL|PLACEMENT_GROUPED,
		    BudaBubblePosition.DIALOG);
}



void createTaskCreator(BudaBubbleArea bba,BudaBubble bb,Point pt,String proj)
{
   TaskCreator tcc = new TaskCreator(proj);
   TaskBubble nbb = new TaskBubble(tcc);

   bba.addBubble(nbb,bb,pt,PLACEMENT_LEFT|PLACEMENT_ADJACENT|PLACEMENT_EXPLICIT|PLACEMENT_LOGICAL,
		    BudaBubblePosition.DIALOG);
}




void createTaskNoter(BudaBubbleArea bba,BudaBubble bb,Point pt,String proj,BnoteTask task)
{
   TaskNoter tnn = new TaskNoter(proj,task);
   TaskBubble nbb = new TaskBubble(tnn);

   bba.addBubble(nbb,bb,pt,PLACEMENT_LEFT|PLACEMENT_ADJACENT|PLACEMENT_EXPLICIT|PLACEMENT_LOGICAL,
		    BudaBubblePosition.DIALOG);
}








/********************************************************************************/
/*										*/
/*	Generic bubble for dialog						*/
/*										*/
/********************************************************************************/

private static class TaskBubble extends BudaBubble {

   TaskBubble(Component c) {
      super(c,BudaBorder.NONE);
    }

   @Override protected void paintContentOverview(Graphics2D g,Shape s) {
      Dimension sz = getSize();
      g.setColor(BoardColors.getColor("Bbook.TaskOverviewColor"));
      g.fillRect(0,0,sz.width,sz.height);
    }

}	// end of inner class TaskBubble



private abstract class TaskDialog extends SwingGridPanel {

   protected TaskDialog() {
      setInsets(4);
      setBackground(BoardColors.getColor("Bbook.TaskBackgroundColor"));
    }

   protected void closeDialog(ActionEvent evt) {
      setVisible(false);
      Component c = (Component) evt.getSource();
      BudaBubble bb = BudaRoot.findBudaBubble(c);
      if (bb != null) {
	 BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bb);
	 if (bba != null) {
	    bba.remove(bb);
	    return;
	  }
       }

      while (c != null) {
	 if (c instanceof Dialog) {
	    c.setVisible(false);
	    break;
	  }
	 c = c.getParent();
       }
   }

}	// end of abstract inner class TaskDialog




/********************************************************************************/
/*										*/
/*	Task Selector bubble implementation					*/
/*										*/
/********************************************************************************/

private class TaskSelector extends TaskDialog implements ActionListener {

   private SwingComboBox<Object> task_box;
   private String current_project;
   private int result_status;
   private JButton done_button;
   private JButton note_button;

   TaskSelector(String proj) {
      task_box = null;
      current_project = proj;
      result_status = 0;
   
      beginLayout();
      addBannerLabel("Programmer's Log Book");
      addSeparator();
   
      List<String> plist = BbookFactory.getFactory().getProjects();
      if (proj == null && plist.size() > 0) {
         proj = plist.get(0);
         current_project = proj;
       }
      addChoice("Project",plist,proj,this);
   
      task_box = addChoice("Task",(Collection<Object>) null,0,this);
      setupTasks();
   
      addBottomButton("Cancel","Cancel",this);
      note_button = addBottomButton("Add Note","Add Note",this);
      addBottomButton("New Task","New Task",this);
      done_button = addBottomButton("Done","Done",this);
      updateButtons();
      addBottomButtons();
   }

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      Rectangle loc = null;
      BnoteTask task = null;
      BudaBubbleArea bba = null;
      if (cmd.equals("Project")) {
         JComboBox<?> cbx = (JComboBox<?>) evt.getSource();
         String p = (String) cbx.getSelectedItem();
         if (p != null && !p.equals(current_project)) {
            current_project = p;
            setupTasks();
          }
       }
      else if (cmd.equals("Task")) { }
      else if (cmd.equals("Cancel")) {
         result_status = -1;
         closeDialog(evt);
       }
      else if (cmd.equals("Done") || cmd.equals("New Task")) {
         loc = BudaRoot.findBudaLocation(this);
         bba = BudaRoot.findBudaBubbleArea(this);
         closeDialog(evt);
         if (task_box != null && current_project != null) {
            Object tobj = task_box.getSelectedItem();
            if (tobj == null) return;
            if (tobj instanceof BnoteTask) task = (BnoteTask) tobj;
            if (tobj.equals("< New Task >") || cmd.equals("New Task")) {
               result_status = 1;
             }
            else result_status = 3;
          }
       }
      else if (cmd.equals("Add Note")) {
         loc = BudaRoot.findBudaLocation(this);
         bba = BudaRoot.findBudaBubbleArea(this);
         closeDialog(evt);
         result_status = 2;
         if (task_box != null) {
            Object tobj = task_box.getSelectedItem();
            if (tobj == null) return;
            if (tobj instanceof BnoteTask) task = (BnoteTask) tobj;
            if (current_project != null) {
               if (tobj.equals("< New Task >")) result_status = 1;
             }
         }
       }
   
      switch (result_status) {
         case 0 :			// edits only
            updateButtons();
            break;
         case 1 :			// new task
            if (loc != null && bba != null)
               createTaskCreator(bba,null,loc.getLocation(),current_project);
            break;
         case 2 :			// add note
            if (task != null && loc != null && bba != null) {
               BbookFactory.getFactory().handleSetTask(task,bba,loc);
             }
            if (task != null && current_project != null && loc != null && bba != null) {
               createTaskNoter(bba,null,loc.getLocation(),current_project,task);
             }
            break;
         case 3 :			// set task
            if (task != null && loc != null && bba != null) {
               BbookFactory.getFactory().handleSetTask(task,bba,loc);
             }
            break;
       }
    }

   private void setupTasks() {
      if (task_box == null || current_project == null) return;
      task_box.removeAllItems();
      List<BnoteTask> tasks = BnoteFactory.getFactory().getTasksForProject(current_project);
      Collections.sort(tasks,new TaskTimeCompare());
      if (tasks != null && tasks.size() > 0) {
	 for (BnoteTask t : tasks) {
	    task_box.addItem(t);
	  }
       }
      else {
	 task_box.addItem("< New Task >");
       }
      task_box.setSelectedIndex(0);
      updateButtons();
    }

   private void updateButtons() {
      if (current_project != null && task_box != null && task_box.getSelectedItem() != null) {
	 if (done_button != null) done_button.setEnabled(true);
	 if (note_button != null) note_button.setEnabled(task_box.getSelectedIndex() > 0);
       }
      else {
	 if (done_button != null) done_button.setEnabled(false);
	 if (note_button != null) note_button.setEnabled(false);
       }
    }

}	// end of inner class TaskSelector



private static class TaskTimeCompare implements Comparator<BnoteTask> {

   @Override public int compare(BnoteTask t1,BnoteTask t2) {
      long v1 = t1.getLastTime().getTime();
      long v2 = t2.getLastTime().getTime();
      if (v2 > v1) return 1;
      if (v2 < v1) return -1;
      return 0;
    }

}	// end of inner class TaskTimeCompare




/********************************************************************************/
/*										*/
/*	New Task Definition dialog implementation				*/
/*										*/
/********************************************************************************/

private class TaskCreator extends TaskDialog implements ActionListener {

   private JTextField name_field;
   private JTextArea  desc_field;
   private String current_project;
   private int result_status;

   TaskCreator(String proj) {
      current_project = proj;
      result_status = 0;

      beginLayout();
      addBannerLabel("Programmer's Log Book");
      addSeparator();

      List<String> plist = BbookFactory.getFactory().getProjects();
      addChoice("Project",plist,proj,this);

      name_field = addTextField("New Task Name",null,24,this,null);
      desc_field = addTextArea("Description",null,6,32,null);
      desc_field.setLineWrap(true);

      // add a button for use existing task
      addBottomButton("Cancel","Cancel",this);
      addBottomButton("Create","Create",this);
      addBottomButtons();
   }

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd.equals("Cancel")) {
	 result_status = -1;
	 closeDialog(evt);
       }
      else if (cmd.equals("Done") || cmd.equals("Create")) {
	 BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
	 Rectangle loc = BudaRoot.findBudaLocation(this);
	 closeDialog(evt);
	 if (bba == null || loc == null) return;
	 if (name_field != null && current_project != null && name_field.getText().length() > 0) {
	    BnoteTask task = BnoteStore.defineTask(name_field.getText(),current_project,
						      desc_field.getText());
	    if (task != null) BbookFactory.getFactory().handleSetTask(task,bba,loc);
	  }
       }
      else if (cmd.equals("Project")) {
	 JComboBox<?> cbx = (JComboBox<?>) evt.getSource();
	 current_project = (String) cbx.getSelectedItem();
       }
      else if (cmd.equals("New Task Name")) ;
      else {
	 BoardLog.logE("BBOOK","Unexpected action " + cmd);
      }

      if (result_status == 0) updateButtons();
    }

   private void updateButtons() {
      JButton done = (JButton) getComponentForLabel("Done");
      if (current_project != null && name_field != null && name_field.getText().length() > 0) {
	 if (done != null) done.setEnabled(true);
       }
      else {
	 if (done != null) done.setEnabled(false);
       }
    }

}	// end of inner class TaskCreator





/********************************************************************************/
/*										*/
/*	Dialog for adding task notes						*/
/*										*/
/********************************************************************************/

private class TaskNoter extends TaskDialog implements ActionListener {

   private JTextArea  note_field;
   private JTextField attach_field;
   private JCheckBox screen_shot;
   private BnoteTask current_task;
   private String current_project;
   private int result_status;

   TaskNoter(String proj,BnoteTask task) {
      current_project = proj;
      current_task = task;
      result_status = 0;
   
      beginLayout();
      addBannerLabel("Programmer's Log Book");
      addSeparator();
   
      addDescription("Project",proj);
      addDescription("Task",task.toString());
   
      note_field = addTextArea("Task Notes",null,4,40,null);
      attach_field = addFileField("Attachment",(String) null,0,null,null);
      screen_shot = addBoolean("Attach Screen Shot",false,null);
   
      addBottomButton("Cancel","Cancel",this);
      addBottomButton("Change Task","Change Task",this);
      addBottomButton("New Task","New Task",this);
      addBottomButton("Done","Done",this);
      addBottomButtons();
    }

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd.equals("Cancel")) {
         result_status = -1;
         closeDialog(evt);
       }
      else if (cmd.equals("Done")) {
         BudaRoot br = BudaRoot.findBudaRoot(this);
         closeDialog(evt);
         if (note_field != null && note_field.getText().length() > 0) {
            BnoteStore.log(current_project,current_task,BnoteEntryType.NOTE,"NOTE",note_field.getText());
          }
         if (attach_field != null && attach_field.getText().length() > 0) {
            File f1 = new File(attach_field.getText());
            if (f1.exists() && f1.canRead()) {
               BnoteStore.attach(current_task,f1);
             }
          }
         if (screen_shot != null && screen_shot.isSelected() && br != null) {
            File f1 = getScreenShot(br);
            if (f1.exists() && f1.canRead()) {
               BnoteStore.attach(current_task,f1);
               f1.delete();
             }
          }
       }
      else if (cmd.equals("Change Task")) {
         BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
         Rectangle r = BudaRoot.findBudaLocation(this);
         closeDialog(evt);
         if (bba != null && r != null)
            createTaskSelector(bba,null,r.getLocation(),current_project);
       }
      else if (cmd.equals("New Task")) {
         BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
         Rectangle r = BudaRoot.findBudaLocation(this);
         closeDialog(evt);
         if (bba != null && r != null)
            createTaskCreator(bba,null,r.getLocation(),current_project);
       }
   
      if (result_status == 0) updateButtons();
    }

   private void updateButtons() {
      JButton done = (JButton) getComponentForLabel("Done");
      if (note_field != null && note_field.getText().length() > 0) {
	 if (done != null) done.setEnabled(true);
       }
      else {
	 if (done != null) done.setEnabled(false);
       }
    }

}	// end of inner class TaskNoter



/********************************************************************************/
/*										*/
/*	Screen shot methods							*/
/*										*/
/********************************************************************************/

File getScreenShot(BudaRoot br)
{
   Dimension sz = br.getSize();
   BufferedImage bi = new BufferedImage(sz.width,sz.height,BufferedImage.TYPE_INT_RGB);
   Graphics2D g2 = bi.createGraphics();
   br.paint(g2);

   try {
      File f = File.createTempFile("BubblesScreenShot",".png");
      f.deleteOnExit();
      ImageIO.write(bi,"png",f);
      return f;
    }
   catch (IOException e) {
      BoardLog.logE("BBOOK","Problem dumping screen",e);
    }

   return null;
}




}	// end of class BbookTasker




/* end of BbookTasker.java */

