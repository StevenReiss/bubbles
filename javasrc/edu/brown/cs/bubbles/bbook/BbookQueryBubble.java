/********************************************************************************/
/*										*/
/*		BbookQueryBubble.java						*/
/*										*/
/*	Bubble with programmer's logbook query interface                        */
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
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;

import edu.brown.cs.ivy.swing.SwingGridPanel;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;


class BbookQueryBubble extends BudaBubble implements BbookConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BbookRegion	task_region;
private String		current_project;
private BnoteTask	current_task;
private JComboBox<Object> task_selector;
private String		current_user;
private JComboBox<String> user_selector;
private String		current_class;
private String		current_method;
private JComboBox<String> class_selector;
private JComboBox<String> method_selector;
private Orderings	current_order;

private boolean 	show_users;

private static final long serialVersionUID = 1;






/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BbookQueryBubble(BudaBubbleArea bba,Point pt)
{
   Rectangle r0 = new Rectangle(pt);
   task_region = BbookFactory.getFactory().findTaskRegion(bba,r0);
   if (task_region == null) current_task = null;
   else current_task = task_region.getTask();
   if (current_task == null) current_project = null;
   else current_project = current_task.getProject();
   current_user = null;
   current_class = null;
   current_method = null;
   current_order = Orderings.TASK;

   List<String> usrs = BnoteFactory.getFactory().getUsersForTask(null,null);
   show_users = usrs != null && usrs.size() > 2;

   task_selector = null;
   user_selector = null;
   class_selector = null;
   method_selector = null;

   JComponent pnl = setupQueryPanel();
   setContentPane(pnl);
}



/********************************************************************************/
/*										*/
/*	Query panel setup							*/
/*										*/
/********************************************************************************/

private JPanel setupQueryPanel()
{
   SwingGridPanel pnl = new SwingGridPanel();
   pnl.setInsets(4);
   pnl.setBackground(BoardColors.getColor("Bbook.PanelColor")); 

   pnl.beginLayout();
   pnl.addBannerLabel("Programmer's Notebook Display");
   pnl.addSeparator();

   pnl.addSectionLabel("Query By");
   addProjectSelector(pnl);
   addTaskSelector(pnl);
   addUserSelector(pnl);
   addNameSelector(pnl);
   pnl.addSeparator();

   pnl.addSectionLabel("Display Properties");
   pnl.addChoice("Order By",current_order,new OrderHandler());
   pnl.addSeparator();

   DisplayHandler dh = new DisplayHandler();
   pnl.addBottomButton("Cancel","Cancel",dh);
   pnl.addBottomButton("Display","Display",dh);
   pnl.addBottomButtons();

   return pnl;
}




/********************************************************************************/
/*										*/
/*	Project management methods						*/
/*										*/
/********************************************************************************/

private void addProjectSelector(SwingGridPanel pnl)
{
   List<String> plist = BbookFactory.getFactory().getProjects();

   if (plist.size() < 2) return;
   plist.add(0,"< Any Project >");
   int idx = 0;
   if (current_project != null) {
      idx = plist.indexOf(current_project);
      if (idx < 0) {
	 idx = 0;
	 current_project = null;
       }
    }

   pnl.addChoice("Project",plist,idx,new ProjectHandler());
}


private final class ProjectHandler implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      JComboBox<?> cbx = (JComboBox<?>) evt.getSource();
      String pnm = (String) cbx.getSelectedItem();
      if (pnm == null || pnm.equals("< Any Project >")) {
         if (current_project == null) return;
         current_project = null;
       }
      else if (pnm.equals(current_project)) return;
      else {
         current_project = pnm;
       }
      resetTasks();
    }

}	// end of inner class ProjectHandler




/********************************************************************************/
/*										*/
/*	Task selector methods							*/
/*										*/
/********************************************************************************/

private void addTaskSelector(SwingGridPanel pnl)
{
   task_selector = pnl.addChoice("Task",new Object[0],0,new TaskHandler());

   resetTasks();
}


private void resetTasks()
{
   if (task_selector == null) return;

   List<BnoteTask> tasks = BnoteFactory.getFactory().getTasksForProject(current_project);
   task_selector.removeAllItems();
   task_selector.addItem("< Any Task >");
   if (tasks != null) {
      for (BnoteTask t : tasks) task_selector.addItem(t);
    }
   if (current_task == null) task_selector.setSelectedIndex(0);
   else if (tasks != null) {
      int idx = tasks.indexOf(current_task);
      if (idx >= 0) task_selector.setSelectedIndex(idx+1);
      else {
	 current_task = null;
	 task_selector.setSelectedIndex(0);
       }
    }
}



private final class TaskHandler implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      Object itm = task_selector.getSelectedItem();
      if (itm == null || itm.equals("< Any Task >")) {
         if (current_task == null) return;
         current_task = null;
       }
      else if (itm.equals(current_task)) return;
      else {
         current_task = (BnoteTask) itm;
       }
      resetUsers();
      resetNames();
    }

}	// end of inner class TaskHandler




/********************************************************************************/
/*										*/
/*	User selectors								*/
/*										*/
/********************************************************************************/

private void addUserSelector(SwingGridPanel pnl)
{
   if (!show_users) return;

   user_selector = pnl.addChoice("User",new String [0],0,new UserHandler());

   resetUsers();
}


private void resetUsers()
{
   if (user_selector == null) return;

   List<String> usrs = BnoteFactory.getFactory().getUsersForTask(current_project,current_task);
   user_selector.removeAllItems();
   user_selector.addItem("< Any User >");
   if (usrs != null) {
      for (String u : usrs) user_selector.addItem(u);
    }

   if (current_user == null || usrs == null) user_selector.setSelectedIndex(0);
   else {
      int idx = usrs.indexOf(current_user);
      if (idx >= 0) user_selector.setSelectedIndex(idx+1);
      else {
	 current_user = null;
	 user_selector.setSelectedIndex(0);
       }
    }
}



private final class UserHandler implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      String unm = (String) user_selector.getSelectedItem();
      if (unm == null || unm.equals("< Any User >")) {
         if (current_user == null) return;
         current_user = null;
       }
      else if (unm.equals(current_user)) return;
      else {
         current_user = unm;
       }
      // reset next selectors
    }

}	// end of inner class UserHandler




/********************************************************************************/
/*										*/
/*	Name selector methods							*/
/*										*/
/********************************************************************************/

private void addNameSelector(SwingGridPanel pnl)
{
   NameHandler nh = new NameHandler();
   class_selector = pnl.addChoice("Class",new String[0],0,nh);
   method_selector = pnl.addChoice("Method",new String[0],0,nh);

   resetNames();
}



private void resetNames()
{
   if (class_selector == null || method_selector == null) return;

   List<String> nms = BnoteFactory.getFactory().getNamesForTask(current_project,current_task);
   if (nms == null) nms = new ArrayList<String>();
   List<String> mnms = createMethods(nms);
   List<String> cnms = createClasses(nms);

   class_selector.removeAllItems();
   for (String c : cnms) class_selector.addItem(c);
   if (current_class == null) class_selector.setSelectedIndex(0);
   else {
      int idx = cnms.indexOf(current_class);
      if (idx >= 0) class_selector.setSelectedIndex(idx+1);
      else {
	 current_class = null;
	 class_selector.setSelectedIndex(0);
       }
    }

   method_selector.removeAllItems();
   for (String m : mnms) method_selector.addItem(m);
   if (current_method == null) method_selector.setSelectedIndex(0);
   else {
      int idx = mnms.indexOf(current_method);
      if (idx >= 0) method_selector.setSelectedIndex(idx+1);
      else {
	 current_method = null;
	 method_selector.setSelectedIndex(0);
       }
    }
}



private List<String> createMethods(Collection<String> nms)
{
   Set<String> rslt = new TreeSet<>();
   for (String nm : nms) {
      int idx1 = nm.indexOf("(");
      int idx2;
      if (idx1 < 0) idx2 = nm.lastIndexOf(".");
      else idx2 = nm.lastIndexOf(".",idx1);
      if (idx2 < 0) continue;
      String mnm = nm.substring(idx2+1);
      rslt.add(mnm);
    }

   List<String> rtn = new ArrayList<>();
   rtn.add("< Any Method >");
   rtn.addAll(rslt);

   return rtn;
}


private List<String> createClasses(Collection<String> nms)
{
   Set<String> fulls = new HashSet<>();
   for (String nm : nms) {
      int idx1 = nm.indexOf("(");
      int idx2;
      if (idx1 < 0) idx2 = nm.lastIndexOf(".");
      else idx2 = nm.lastIndexOf(".",idx1);
      if (idx2 < 0) continue;
      String cnm = nm.substring(0,idx2);
      fulls.add(cnm);
    }

   Map<String,Set<String>> found = new TreeMap<String,Set<String>>();
   found.put("< Any Class >",fulls);

   for (String cnm : fulls) {
      addClassSets(cnm,-1,found,fulls);
    }

   List<String> rslt = new ArrayList<String>();
   found.remove("< Any Class >");
   rslt.add("< Any Class >");
   rslt.addAll(found.keySet());

   return rslt;
}


private void addClassSets(String nm,int idx,Map<String,Set<String>> found,Set<String> last)
{
   int idx1 = nm.indexOf(".",idx+1);
   String key;
   if (idx1 < 0) key = nm;
   else key = nm.substring(0,idx1+1);
   Set<String> nxt = new HashSet<String>();
   for (String s : last) {
      if (s.startsWith(key)) nxt.add(s);
    }
   if (nxt.size() == last.size()) {
      if (idx > 0) {
	 String lkey = nm.substring(0,idx+1);
	 found.remove(lkey);
	 found.put(key,nxt);
       }
    }
   else {
      found.put(key,nxt);
    }
   if (idx1 > 0) addClassSets(nm,idx1,found,nxt);
}






private final class NameHandler implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      JComboBox<?> cbx = (JComboBox<?>) evt.getSource();
      if (evt.getActionCommand().equals("Class")) {
	 current_class = (String) cbx.getSelectedItem();
	 if (current_class != null && current_class == "< Any Class >") current_class = null;
       }
      else {
	 current_method = (String) cbx.getSelectedItem();
	 if (current_method != null && current_method == "< Any Method >") current_method = null;
       }
    }

}	// end of inner class NameHandler



/********************************************************************************/
/*										*/
/*	Ordering methods							*/
/*										*/
/********************************************************************************/

private final class OrderHandler implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      JComboBox<?> cbx = (JComboBox<?>) evt.getSource();
      current_order = (Orderings) cbx.getSelectedItem();
    }

}	// end of inner class OrderHandler



/********************************************************************************/
/*										*/
/*	Display Setup methods							*/
/*										*/
/********************************************************************************/

private void closeDialog(ActionEvent evt) {
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




private final class DisplayHandler implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      if (cmd.equals("Cancel")) {
         closeDialog(evt);
      }
      else {
         BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BbookQueryBubble.this);
         Rectangle r0 = BudaRoot.findBudaLocation(BbookQueryBubble.this);
         closeDialog(evt);
         Map<String,Object> props = new HashMap<String,Object>();
         if (current_project != null) props.put("PROJECT",current_project);
         if (current_task != null) props.put("TASK",current_task);
         if (current_user != null) props.put("USER",current_user);
         if (current_class != null) props.put("CLASS",current_class);
         if (current_method != null) props.put("METHOD",current_method);
         props.put("ORDERBY",current_order);
         BbookDisplayBuilder bld = new BbookDisplayBuilder(props);
         BudaBubble nbb = new BbookDisplayBubble(bld);
         if (bba != null && r0 != null) {
            bba.addBubble(nbb,null,r0.getLocation(),BudaConstants.PLACEMENT_LOGICAL);
          }
      }
   }

}	// end of inner class DisplayHandler



/********************************************************************************/
/*										*/
/*	Paint methods								*/
/*										*/
/********************************************************************************/

@Override protected void paintContentOverview(Graphics2D g,Shape s)
{
   Dimension sz = getSize();
   g.setColor(BoardColors.getColor("Bbook.OverviewColor"));
   g.fillRect(0,0,sz.width,sz.height);
}




}	// end of class BbookQueryBubble




/* end of BbookQueryBubble.java */

