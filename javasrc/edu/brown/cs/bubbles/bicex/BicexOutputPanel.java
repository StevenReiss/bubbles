/********************************************************************************/
/*										*/
/*		BicexOutputPanel.java						*/
/*										*/
/*	description of class							*/
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



package edu.brown.cs.bubbles.bicex;

import java.awt.BorderLayout;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import edu.brown.cs.bubbles.bicex.BicexOutputModel.FileData;
import edu.brown.cs.bubbles.board.BoardMetrics;

class BicexOutputPanel extends BicexPanel implements BicexConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BicexOutputModel	output_model;
private OutputModel		tree_model;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BicexOutputPanel(BicexEvaluationViewer ev)
{
   super(ev);
}



/********************************************************************************/
/*										*/
/*	Panel setup methods							*/
/*										*/
/********************************************************************************/

@Override protected JComponent setupPanel()
{
   output_model = eval_viewer.getExecution().getOutputModel();
   tree_model = new OutputModel();

   return new OutputLayout();
}



/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

@Override void update()
{
   tree_model.update();
}



@Override void updateTime()
{
   tree_model.update();
}



/********************************************************************************/
/*										*/
/*	Output Panel								*/
/*										*/
/********************************************************************************/

private class OutputLayout extends JPanel {

   private OutputTree	output_tree;
   private static final long serialVersionUID = 1;

   OutputLayout() {
      super(new BorderLayout());
      output_tree = new OutputTree();
      add(output_tree,BorderLayout.CENTER);
      Expander exp = new Expander(output_tree);
      output_tree.addTreeExpansionListener(exp);
      tree_model.addTreeModelListener(exp);
    }
   
}	// end of inner class OutputLayout




/********************************************************************************/
/*										*/
/*	Output file tree							*/
/*										*/
/********************************************************************************/

private class OutputTree extends JTree {

   private static final long serialVersionUID = 1;

   OutputTree() {
      super(tree_model);
      setRootVisible(false);
    }

}	// end of inner class OutputTree



private class Expander implements TreeExpansionListener, TreeModelListener, Runnable {
   
   private Set<String>	expand_set;
   private JTree for_tree;
   
   Expander(JTree tr) {
      expand_set = new HashSet<String>();
      for_tree = tr;
    }
   
   @Override public void treeCollapsed(TreeExpansionEvent evt)	{
      BoardMetrics.noteCommand("BICEX","OutputCollapse");
      Object tn = evt.getPath().getLastPathComponent();
      expand_set.remove(tn.toString());
    }
   
   @Override public void treeExpanded(TreeExpansionEvent evt)	{
      BoardMetrics.noteCommand("BICEX","OutputExpand");
      Object tn = evt.getPath().getLastPathComponent();
      expand_set.add(tn.toString());
    } 
   
   @Override public void treeNodesChanged(TreeModelEvent e)	{ }
   @Override public void treeNodesInserted(TreeModelEvent e)	{ }
   @Override public void treeNodesRemoved(TreeModelEvent e)	{ }
   @Override public void treeStructureChanged(TreeModelEvent e) {
      SwingUtilities.invokeLater(this);
    }
   
   @Override public void run() {
      checkExpand();
    }
   
   private void checkExpand() {
      checkExpandNode(null,tree_model.getRoot());
    }
   
   private void checkExpandNode(TreePath tp,Object vn) {
      boolean exp = false;
      
      if (tp == null) exp = true;
      else if (vn instanceof TreeNode) exp = true;              // root
      else if (vn instanceof BicexOutputModel.FileData) {
         if (expand_set.contains(vn.toString())) exp = true;
       }
      
      if (exp) {
         if (tp == null) tp = new TreePath(vn);
         else tp = tp.pathByAddingChild(vn);
         try {
            for_tree.expandPath(tp);
          }
         catch (ArrayIndexOutOfBoundsException e) {
            return;
          }
         for (int i = 0; i < tree_model.getChildCount(vn); ++i) { 
            checkExpandNode(tp,tree_model.getChild(vn,i)); 
          }
       }
    }
   
}	// end of inner class Expander




/********************************************************************************/
/*										*/
/*	Tree Model based on output						*/
/*										*/
/********************************************************************************/

private class OutputModel extends DefaultTreeModel {

   private List<BicexOutputModel.FileData> file_data;
   private TreeNode root_node;

   private static final long serialVersionUID = 1;

   OutputModel() {
      super(new DefaultMutableTreeNode());
      root_node = (TreeNode) getRoot();
      file_data = null;
    }

   void update() {
       file_data = output_model.getOutputFiles();
       reload();
    }

   @Override public Object getChild(Object par,int idx) {
      if (par == root_node) {
	 return file_data.get(idx);
       }
      else if (par instanceof FileData) {
	 FileData fd = (FileData) par;
	 return getContent(fd,getExecution().getCurrentTime());
       }
      return null;
    }

   @Override public int getChildCount(Object par) {
      if (file_data == null) return 0;
      if (par == root_node) return file_data.size();
      else if (par instanceof FileData) return 1;
      return 0;
    }

   @Override public int getIndexOfChild(Object par,Object child) {
      if (par == root_node) return file_data.indexOf(child);
      return 0;
    }

   @Override public boolean isLeaf(Object node) {
      if (node == root || node instanceof FileData) return false;
      return true;
    }

}	// end of inner class OutputModel



private static String getContent(FileData fd,long when)
{
   StringBuffer buf = new StringBuffer();
   buf.append("<html>");
   buf.append("<table cellpadding='1' cellspacing='2' align='left' style='border: 1px solid black;'>");
   buf.append("<tr><td>Descriptor</td><td>");
   buf.append(fd.getFileDescriptor());
   buf.append("</td></tr>");
   buf.append("<tr><td>Path</td><td>");
   buf.append(fd.getFilePath());
   buf.append("</td></tr>");
   buf.append("<tr><td>Contents</td><td><pre>");
   buf.append(fd.getFileContents(when));
   buf.append("</pre></td></tr>");
   buf.append("</table>");
   return buf.toString();
}




}	// end of class BicexOutputPanel




/* end of BicexOutputPanel.java */

