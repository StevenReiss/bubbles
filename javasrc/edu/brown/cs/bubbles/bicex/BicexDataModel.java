/********************************************************************************/
/*										*/
/*		BicexDataModel.java						*/
/*										*/
/*	Holder of data for the current execution				*/
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


import java.util.Comparator;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.ivy.swing.SwingEventListenerList;
import edu.brown.cs.ivy.swing.SwingTreeTable;



class BicexDataModel implements SwingTreeTable.TreeTableModel, BicexConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private SwingEventListenerList<TreeModelListener> model_listeners;

private BicexExecution		for_execution;
private ContextNode		root_node;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BicexDataModel(BicexExecution exec,BicexEvaluationContext ctx)
{
   for_execution = exec;
   root_node = new ContextNode(ctx);
   root_node.update(2);
   model_listeners = new SwingEventListenerList<>(TreeModelListener.class);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void setContext(BicexEvaluationContext ctx)
{
   root_node.setContext(ctx);
   noteChange();
}


/********************************************************************************/
/*										*/
/*	Listener methods							*/
/*										*/
/********************************************************************************/

@Override public void addTreeModelListener(TreeModelListener l)
{
   model_listeners.add(l);
}


@Override public void removeTreeModelListener(TreeModelListener l)
{
   model_listeners.remove(l);
}



protected void fireTreeStructureChanged(TreeModelEvent evt)
{
   for (TreeModelListener tml : model_listeners) {
      tml.treeStructureChanged(evt);
    }
}




protected void fireTreeNodesChanged(TreeModelEvent evt)
{
   for (TreeModelListener tml : model_listeners) {
      tml.treeNodesChanged(evt);
    }
}




protected void fireTreeNodesInserted(TreeModelEvent evt)
{
   for (TreeModelListener tml : model_listeners) {
      tml.treeNodesInserted(evt);
    }
}



protected void fireTreeNodesRemoved(TreeModelEvent evt)
{
   for (TreeModelListener tml : model_listeners) {
      tml.treeNodesRemoved(evt);
    }
}



void noteChange()
{
   root_node.noteChange();
   SwingUtilities.invokeLater(new TreeUpdater());
}


private class TreeUpdater implements Runnable {
   
   @Override public void run() {
      Object [] path = new Object[] { root_node };
      TreeModelEvent evt = new TreeModelEvent(BicexDataModel.this,path);
      if (root_node.isLeaf()) fireTreeNodesChanged(evt);
      else fireTreeStructureChanged(evt);
    }
   
}       // end of inner class TreeUpdater



/********************************************************************************/
/*										*/
/*	TreeModel interface							*/
/*										*/
/********************************************************************************/

@Override public Object getChild(Object par,int idx)
{
   AbstractNode an = (AbstractNode) par;
   return an.getChildAt(idx);
}


@Override public int getChildCount(Object  par)
{
   AbstractNode an = (AbstractNode) par;
   return an.getChildCount();
}



@Override public int getIndexOfChild(Object par,Object child)
{
   AbstractNode an = (AbstractNode) par;
   AbstractNode cn = (AbstractNode) child;

   return an.getIndex(cn);
}



@Override public Object getRoot()			{ return root_node; }


@Override public boolean isLeaf(Object node)
{
   AbstractNode an = (AbstractNode) node;

   return an.isLeaf();
}



@Override public void valueForPathChanged(TreePath tp,Object val)
{
   BoardLog.logD("BICEX","Value changed for tree");
}



/********************************************************************************/
/*										*/
/*	TreeTableModel interface						*/
/*										*/
/********************************************************************************/

@Override public int getColumnCount()			{ return 2; }

@Override public String getColumnName(int col)
{
   if (col == 0) return "Name";
   return "Value";
}

@Override public Class<?> getColumnClass(int col)
{
   if (col == 0) return SwingTreeTable.TreeTableModel.class;
   return Object.class;
}


@Override public Object getValueAt(Object node,int col)
{
   if (node == null) return null;

   AbstractNode an = (AbstractNode) node;

   if (col == 0) return an;
   // if (col == 0) return an.getName();
   else if (col == -1) return an;
   else return an.getValue();
}


@Override public boolean isCellEditable(Object node,int col)
{
   return getColumnClass(col) == SwingTreeTable.TreeTableModel.class;
}


@Override public void setValueAt(Object val,Object node,int col)
{ }



/********************************************************************************/
/*										*/
/*	Generalized frame/value node						*/
/*										*/
/********************************************************************************/

abstract class AbstractNode implements TreeNode, BicexTreeNode {

   private AbstractNode parent_node;
   private boolean	children_known;
   private Vector<AbstractNode> child_nodes;
   private Set<AbstractNode> sorted_children;

   protected AbstractNode(AbstractNode par) {
      parent_node = par;
      children_known = false;
      child_nodes = null;
      sorted_children = null;
    }

   abstract String getName();
   abstract void addChildren();
   public Object getValue()					{ return null; }
   String getContextId()                                        { return null; }
   
   BicexValue getBicexValue() {
      return null;
    }
   
   String getLabel() {	
      String lbl = "";
      if (parent_node != null) lbl = parent_node.getLabel() + ".";
      lbl += getName();
      return lbl;
    }

   protected AbstractNode getBicexParent()			{ return parent_node; }

   @Override public TreeNode getParent()			{ return parent_node; }

   @Override public boolean isLeaf()				{ return false; }

   @Override public Enumeration<? extends TreeNode> children()  {
      computeChildren(0);
      return child_nodes.elements();
    }

   @Override public boolean getAllowsChildren() 		{ return true; }

   @Override public TreeNode getChildAt(int idx) {
      computeChildren(0);
      if (child_nodes == null) return null;
      if (idx < 0 || idx >= child_nodes.size()) return null;
      return child_nodes.get(idx);
    }

   @Override public int getChildCount() {
      computeChildren(0);
      if (child_nodes == null) return 0;
      return child_nodes.size();
    }

   @Override public int getIndex(TreeNode tn) {
      computeChildren(0);
      return child_nodes.indexOf(tn);
    }

   @Override public boolean isUpdatedCurrently() {
      return false;
    }
   
   void noteChange() {
      if (isLeaf()) return;
      if (!children_known) return;
      if (getContextId() == null) {
         update(2);
       }
      else {
         for (AbstractNode cn : child_nodes) {
            cn.noteChange();
          }
       }
    }
   
   private synchronized void computeChildren(int lvl) {
      if (children_known) return;
      if (isLeaf() || !getAllowsChildren()) {
         child_nodes = new Vector<AbstractNode>();
       }
      else {
         sorted_children = new TreeSet<AbstractNode>(new ValueSorter());
         addChildren();
         if (sorted_children == null) return;
         child_nodes = new Vector<AbstractNode>(sorted_children);
         if (lvl > 0) for (AbstractNode an : child_nodes) an.update(lvl-1);
         sorted_children = null;
         children_known = true;
       }
    }

   void update(int lvl) {
      if (isLeaf()) return;
      children_known = false;
      computeChildren(lvl);
    }

   protected void addChild(AbstractNode n) {
      if (sorted_children != null) sorted_children.add(n);
      else {
	 if (child_nodes == null) child_nodes = new Vector<AbstractNode>();
	 child_nodes.add(n);
       }
    }

   protected void sortChildren() {
      sorted_children = new TreeSet<AbstractNode>(new ValueSorter());
      sorted_children.addAll(child_nodes);
      child_nodes = new Vector<AbstractNode>(sorted_children);
      sorted_children = null;
      children_known = true;
    }

   @Override public String toString() {
      String nm = getName();
      if (nm == null) return "???";
      // int idx = nm.lastIndexOf("?");
      // if (idx >= 0) nm = nm.substring(idx+1);
      return nm;
    }

}	// end of inner class AbstractNode




/********************************************************************************/
/*										*/
/*	Context node -- root							*/
/*										*/
/********************************************************************************/

private class ContextNode extends AbstractNode {

   BicexEvaluationContext eval_context;

   ContextNode(BicexEvaluationContext ctx) {
      this(ctx,null);
    }

   ContextNode(BicexEvaluationContext ctx,AbstractNode par) {
      super(par);
      eval_context = null;
      if (ctx != null) setContext(ctx);
    }
   
   @Override String getContextId() {
      if (eval_context == null) return null;
      return eval_context.getId();
   }

   @Override String getName() {
      if (eval_context == null) {
         return "Pending";
       }
      return eval_context.getMethod();
    }

   @Override void addChildren() {
      if (eval_context == null) return;
      for (Map.Entry<String,BicexValue> ent : eval_context.getValues().entrySet()) {
         ValueNode vn = new ValueNode(this,ent.getKey(),ent.getValue());
         addChild(vn);
       }
    }

   void setContext(BicexEvaluationContext ctx) {
      eval_context = ctx;
      update(2);
    }

}	// end of inner class ContextNode



/********************************************************************************/
/*										*/
/*	ValueNode -- node representing a value					*/
/*										*/
/********************************************************************************/

private class ValueNode extends AbstractNode {

   private BicexValue for_value;
   private String value_name;

   ValueNode(AbstractNode par,String key,BicexValue val) {
      super(par);
      for_value = val;
      value_name = key;
    }

   @Override String getName()			{ return value_name; }

   @Override public Object getValue() {
      String s = for_value.getStringValue(for_execution.getCurrentTime());
      if (s != null) {
         s = s.replace("\n","\\n");
         s = s.replace("\t","\\t");
         String dt = for_value.getDataType(for_execution.getCurrentTime());
         if (dt != null && dt.equals("java.lang.String")) {
            s = s.replace("\"","\\\"");
            s = "\"" + s + "\"";
          }
       }
      return s;
    }

   @Override public boolean isLeaf() {
      return !for_value.hasChildren(for_execution.getCurrentTime());
    }

   @Override void addChildren() {
      long t = for_execution.getCurrentTime();
      for (Map.Entry<String,BicexValue> ent : for_value.getChildren(t).entrySet()) {
         ValueNode nv = new ValueNode(this,ent.getKey(),ent.getValue());
         addChild(nv);
       }
    }
   
   @Override BicexValue getBicexValue() {
      return for_value;
    }
   
   @Override public boolean isUpdatedCurrently() {
      if (for_value == null) return false;
      long t0 = for_value.getUpdateTime(for_execution.getLineEndTime()-1);
      if (t0 <= 0) return false;
      return for_execution.isCurrentLine(t0);
    }

}



/********************************************************************************/
/*										*/
/*	Value sorter								*/
/*										*/
/********************************************************************************/

private static class ValueSorter implements Comparator<AbstractNode> {

   @Override public int compare(AbstractNode n1,AbstractNode n2) {
      String nm1 = n1.getName();
      String nm2 = n2.getName();
      if (nm1.startsWith("[") && nm2.startsWith("[")) {
         int idx1 = nm1.indexOf("]");
         int idx2 = nm2.indexOf("]");
         try {
            int v1 = Integer.parseInt(nm1.substring(1,idx1));
            int v2 = Integer.parseInt(nm2.substring(1,idx2));
            if (v1 < v2) return -1;
            if (v1 > v2) return 1;
            return 0;
          }
         catch (NumberFormatException e) { }
       }
      return nm1.compareTo(nm2);
    }

}	// end of inner class ValueSorter






}	// end of class BicexDataModel




/* end of BicexDataModel.java */
