/********************************************************************************/
/*										*/
/*		BudaTaskShelf.java						*/
/*										*/
/*	BUblles Display Area top bar						*/
/*										*/
/********************************************************************************/
/*	Copyright 2009-2010 Brown University --  Yu Li	      */
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

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/

package edu.brown.cs.bubbles.buda;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardImage;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeSelectionModel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Calendar;
import java.util.HashMap;



class BudaTaskShelf extends JPanel implements BudaConstants, TreeSelectionListener {


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private JTree task_tree;
private BudaTopBar top_bar;
private Point popup_point;
private HashMap<String, DefaultMutableTreeNode> task_nodes;
private String [] group_by;
private boolean group_by_date;
private JPopupMenu my_popup;
private JPopupMenu my_brother_popup;

private static final long MILLISECS_PER_DAY = 24*60*60*1000;
private static final ImageIcon LEAF_ICON = new ImageIcon(BoardImage.getImage("leaf.png"));
private static final ImageIcon COLLAPSE_ICON = new ImageIcon(BoardImage.getImage("collapse.png"));
private static final ImageIcon EXPAND_ICON = new ImageIcon(BoardImage.getImage("expand.png"));

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BudaTaskShelf(BudaTopBar topbar, BudaTask [] tasks, Point popuppoint)
{
   super();
   top_bar = topbar;
   popup_point = popuppoint;
   task_nodes = new HashMap<String, DefaultMutableTreeNode>();

   group_by_date = BUDA_PROPERTIES.getBoolean(TASK_SHELF_SORT_BY_DATE);

   my_popup = null;
   my_brother_popup = null;

   DefaultMutableTreeNode top;

   if (group_by_date) {
      String[] gb = {"Today","Yesterday","This Week","This Month","Past Six Months", "This Year","More Than a Year"};
      group_by = gb;
      top = getTopNode(true, tasks);
   }
   else {
      String[] gb = {"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z","NUM","SYMB"};//"#","!"};
      group_by = gb;
      top = getTopNode(false, tasks);
   }

   task_tree = new JTree(new DefaultTreeModel(top));
   task_tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
   task_tree.addTreeSelectionListener(this);
   task_tree.setRootVisible(false);
   task_tree.setToggleClickCount(1);
   for (int row=0; row < task_tree.getRowCount(); ++row) task_tree.expandRow(row);

   TreeCellRenderer cellRenderer = new IconCellRenderer();
   task_tree.setCellRenderer(cellRenderer);

   JPanel tree_view = new JPanel();
   tree_view.setLayout(new BorderLayout());
   tree_view.setBackground(BoardColors.getColor("Buda.TaskTreeBackground"));
   tree_view.add(task_tree, BorderLayout.WEST);

   add(tree_view);
   //setPreferredSize(new Dimension(task_tree.getPreferredSize().width+20, (/*tasks.length+*/groups_in_use)*18));

   for (int row=0; row < task_tree.getRowCount(); ++row) task_tree.collapseRow(row);
   task_tree.addTreeExpansionListener(new ExpansionListener());
   //new Hoverer();
}



/********************************************************************************/
/*										*/
/*	Class for tree style							*/
/*										*/
/********************************************************************************/

private static class IconCellRenderer extends DefaultTreeCellRenderer implements TreeCellRenderer
{

   private static final long serialVersionUID = 1;

   public IconCellRenderer() {
      super();
    }

   @Override public Component getTreeCellRendererComponent(JTree tree,
							      Object value, boolean sel,
							      boolean expanded, boolean leaf,
							      int row, boolean hasfocus) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
      Object obj = node.getUserObject();

      if (obj instanceof String) {
	 if (expanded) {
	    setIcon(COLLAPSE_ICON);
	  }
	 else {
	    setIcon(EXPAND_ICON);
	  }
	 setText(obj.toString());
       }
      else {
	 setIcon(LEAF_ICON);
	 BudaTask bt = (BudaTask) obj;
	 setText(bt.toString());
       }
      return this;
    }

}	// end of inner class IconCellRenderer



/********************************************************************************/
/*										*/
/*	Helper methods for setup						*/
/*										*/
/********************************************************************************/

private int getAreaOffset(double x)
{
   Rectangle r = top_bar.getBounds();
   Dimension totsize = top_bar.getBubbleArea().getSize();

   return (int)(x * totsize.width / r.width);
}


private long diffDayPeriods(Calendar start)
{
   long startl	 =  start.getTimeInMillis() +
				start.getTimeZone().getOffset(start.getTimeInMillis() );

   Calendar end = Calendar.getInstance();
   long endl = end.getTimeInMillis() + end.getTimeZone().getOffset( end.getTimeInMillis() );
   return (endl - startl) / MILLISECS_PER_DAY;
}



void setPopup(JPopupMenu jpm)
{
   my_popup = jpm;
   my_popup.setPopupSize(getPreferredSize());
}



void setBrotherPopup(JPopupMenu bro)
{
   my_brother_popup = bro;
}



/********************************************************************************/
/*										*/
/*	Action callbacks							*/
/*										*/
/********************************************************************************/

@Override public void valueChanged(TreeSelectionEvent e)
{
   DefaultMutableTreeNode node = (DefaultMutableTreeNode) task_tree.getLastSelectedPathComponent();

   if (node.getUserObject() instanceof BudaTask) {
      BudaTask bt = (BudaTask) node.getUserObject();
      bt.loadTask(top_bar.getBubbleArea(),getAreaOffset(popup_point.getX()));
      if (my_popup != null) my_popup.setVisible(false);
      if (my_brother_popup != null) my_brother_popup.setVisible(false);
    }
}


/********************************************************************************/
/*										*/
/*	Setup Methods								*/
/*										*/
/********************************************************************************/

private DefaultMutableTreeNode getTopNode(boolean bydate, BudaTask [] tasks)
{
   if (bydate) return setupByDate(tasks);
   return setupAlphabetically(tasks);
}



private DefaultMutableTreeNode setupByDate(BudaTask [] tasks) {
   DefaultMutableTreeNode top = new DefaultMutableTreeNode("Task Shelf");

   DefaultMutableTreeNode category;
   for (int i=0; i<group_by.length; i++) {
      category = new DefaultMutableTreeNode(group_by[i]);
      top.add(category);
      task_nodes.put(group_by[i], category);
    }

   Calendar start = Calendar.getInstance();
   for (int i=0; i<tasks.length;i++) {
      start.setTime(tasks[i].getDate());
      if (diffDayPeriods(start)==0) {
	  task_nodes.get("Today").add(new DefaultMutableTreeNode(tasks[i]));
       }
      else if (diffDayPeriods(start)==1) {
	  task_nodes.get("Yesterday").add(new DefaultMutableTreeNode(tasks[i]));
       }
      else if (diffDayPeriods(start)>1 && diffDayPeriods(start)<7) {
	  task_nodes.get("This Week").add(new DefaultMutableTreeNode(tasks[i]));
       }
      else if (diffDayPeriods(start)>=7 && diffDayPeriods(start)<31) {
	  task_nodes.get("This Month").add(new DefaultMutableTreeNode(tasks[i]));
       }
      else if (diffDayPeriods(start)>31 && diffDayPeriods(start)<183) {
	  task_nodes.get("Past Six Months").add(new DefaultMutableTreeNode(tasks[i]));
       }
      else if (diffDayPeriods(start)>=183 && diffDayPeriods(start)<365) {
	  task_nodes.get("This Year").add(new DefaultMutableTreeNode(tasks[i]));
       }
      else{
	  task_nodes.get("More Than a Year").add(new DefaultMutableTreeNode(tasks[i]));
       }
    }

   //Update the number of children for each category.
   for(int i=0; i<tasks.length; i++) {
      category = task_nodes.get(group_by[i]);
      category.setUserObject(group_by[i]+"("+category.getChildCount()+")");
    }

   return top;
}



private DefaultMutableTreeNode setupAlphabetically(BudaTask [] tasks)
{
   DefaultMutableTreeNode top = new DefaultMutableTreeNode("Task Shelf");

   for (int i=0; i<tasks.length; ++i) {
      if (tasks[i].getName().length() <= 0) continue;
      char idc = Character.toUpperCase(tasks[i].getName().charAt(0));

      String id;
      if (Character.isLetter(idc)) id = Character.toString(idc);
      else if (Character.isDigit(idc)) id = "NUM";//"#";
      else id = "SYMB";//"!";

      DefaultMutableTreeNode cat = task_nodes.get(id);
      if (cat == null) {
	 cat = new DefaultMutableTreeNode(id);
	 top.add(cat);
	 task_nodes.put(id, cat);
       }
      cat.add(new DefaultMutableTreeNode(tasks[i]));
    }

   DefaultMutableTreeNode category;

   //Update the number of children for each category.
   for (int i=0; i<group_by.length; i++) {
      category = task_nodes.get(group_by[i]);
      if (category != null) category.setUserObject(group_by[i]+"("+category.getChildCount()+")");
    }
   return top;
}



private class ExpansionListener implements TreeExpansionListener {

   @Override public void treeCollapsed(TreeExpansionEvent e) {
      Dimension d = task_tree.getPreferredSize();
      d.width +=20;
      d.height+=10;
      my_popup.setPopupSize(getPreferredSize());//d);
      my_popup.pack();
   }

   @Override public void treeExpanded(TreeExpansionEvent e) {
      Dimension d = task_tree.getPreferredSize();
      d.width +=20;
      d.height+=10;
      my_popup.setPopupSize(getPreferredSize());//d);
      my_popup.pack();
   }

}	// end of inner class ExpansionListener




}	// end of class BudaTaskShelf



/* end of BudaTaskShelf.java */
