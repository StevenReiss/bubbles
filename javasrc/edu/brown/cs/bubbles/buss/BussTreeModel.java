/********************************************************************************/
/*										*/
/*		BussTreeModel.java						*/
/*										*/
/*	BUbble Stack Strategies bubble stack tree model 			*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.buss;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import java.util.Enumeration;
import java.util.Vector;



class BussTreeModel extends DefaultTreeModel implements TreeModel, BussConstants
{




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Branch			root_node;
private BussTreeImpl		cur_selection;

private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BussTreeModel()
{
   super(new Branch("ALL",null));

   root_node = (Branch) getRoot();
   cur_selection = null;
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

void addEntry(BussEntry ent)
{
   String nm = ent.getEntryName();
   String [] comps = nm.split("\\.");
   int ncomp = comps.length;

   Branch p = root_node;
   for (int i = 0; i < ncomp-1; ++i) {
      p = p.insertBranch(comps[i]);
    }

   TreeLeaf tl = new TreeLeaf(ent,p);
   p.addChild(tl);
}




void setup()
{
   root_node.collapseSingletons();
}




/********************************************************************************/
/*										*/
/*	Selection methods							*/
/*										*/
/********************************************************************************/

void setSelection(BussTreeNode tn)
{

   if (cur_selection != null) nodeChanged(cur_selection);
   
   if (tn == null) {
      cur_selection = null;
      return;
    }

   BussTreeImpl bti = (BussTreeImpl) tn;
   if (bti.isLeaf()) {
      cur_selection = bti;
      nodeChanged(cur_selection);
    }
   else cur_selection = null;
}



/********************************************************************************/
/*										*/
/*	Remove methods (after drag)						*/
/*										*/
/********************************************************************************/

void removeEntry(BussEntry be)
{
   root_node.removeEntry(this,be);
}



/********************************************************************************/
/*										*/
/*	Cleanup methods 							*/
/*										*/
/********************************************************************************/

void disposeBubbles()
{
   root_node.disposeBubbles();
}



/********************************************************************************/
/*										*/
/*	Tree node representation						*/
/*										*/
/********************************************************************************/

private static abstract class BussTreeImpl implements BussTreeNode, TreeNode {

   protected Branch parent_node;

   protected BussTreeImpl(Branch par) {
      parent_node = par;
    }

   @Override public TreeNode getParent()		{ return parent_node; }

   @Override public BussEntry getEntry()		{ return null; }

   abstract String getLocalName();

   void collapseSingletons()				{ }

   @Override public String toString()			{ return getLocalName(); }

   boolean removeEntry(BussTreeModel mdl,BussEntry be)	{ return false; }

   void disposeBubbles()				{ }

}	// end of inner class BussTreeNode




private static class TreeLeaf extends BussTreeImpl {

   private BussEntry for_entry;

   TreeLeaf(BussEntry ent,Branch par) {
      super(par);
      for_entry = ent;
    }

   @Override public boolean getAllowsChildren() 	{ return false; }
   @Override public Enumeration<TreeNode> children()	{ return null; }
   @Override public TreeNode getChildAt(int idx)	{ return null; }
   @Override public int getChildCount() 		{ return 0; }
   @Override public int getIndex(TreeNode tn)		{ return -1; }
   @Override public boolean isLeaf()			{ return true; }

   @Override public BussEntry getEntry()		{ return for_entry; }

   @Override String getLocalName() {
      String nm = for_entry.getEntryName();
      int idx = nm.indexOf("(");
      if (idx < 0) idx = nm.lastIndexOf(".");
      else idx = nm.lastIndexOf(".",idx);
      return nm.substring(idx+1);
    }

   @Override void disposeBubbles() {
      for_entry.dispose();
    }

}	// end of inner class TreeLeaf



private static class Branch extends BussTreeImpl {

   private String local_name;
   private Vector<TreeNode> child_nodes;

   Branch(String name,Branch par) {
      super(par);
      local_name = name;
      child_nodes = new Vector<TreeNode>();
    }

   @Override public boolean getAllowsChildren() 	{ return true; }
   @Override public Enumeration<TreeNode> children()	{ return child_nodes.elements(); }
   @Override public TreeNode getChildAt(int idx) {
      if (idx > child_nodes.size()) return null;
      return child_nodes.get(idx);
    }
   @Override public int getChildCount() 		{ return child_nodes.size(); }
   @Override public int getIndex(TreeNode tn)		{ return child_nodes.indexOf(tn); }
   @Override public boolean isLeaf()			{ return false; }

   void addChild(TreeNode n)				{ child_nodes.add(n); }

   Branch insertBranch(String nm) {
      int idx = 0;
      for (TreeNode tn : child_nodes) {
	 BussTreeImpl bti = (BussTreeImpl) tn;
	 String tnm = bti.getLocalName();
	 int c = nm.compareTo(tnm);
	 if (c < 0) {
	    Branch b = new Branch(nm,this);
	    child_nodes.insertElementAt(b,idx);
	    return b;
	  }
	 else if (c == 0) return (Branch) tn;
	 ++idx;
       }
      Branch b = new Branch(nm,this);
      child_nodes.add(b);
      return b;
    }

   @Override String getLocalName()				{ return local_name; }

   @Override void collapseSingletons() {
      if (parent_node != null) {
	 Branch cn = this;
	 StringBuffer buf = null;
	 while (cn.child_nodes.size() == 1) {
	    TreeNode tn = cn.getChildAt(0);
	    if (tn.isLeaf()) break;
	    if (buf == null) {
	       buf = new StringBuffer();
	       buf.append(cn.getLocalName());
	     }
	    else {
	       buf.append(".");
	       buf.append(cn.getLocalName());
	     }
	    cn = (Branch) tn;
	  }
	 if (cn != this) {
	    int idx = parent_node.getIndex(this);
	    if (buf != null) cn.local_name = buf.toString() + "." + cn.local_name;
	    cn.parent_node = parent_node;
	    parent_node.child_nodes.set(idx,cn);
	    cn.collapseSingletons();
	    return;
	  }
       }
      for (TreeNode tn : child_nodes) {
	 BussTreeImpl ti = (BussTreeImpl) tn;
	 ti.collapseSingletons();
       }
    }

   @Override boolean removeEntry(BussTreeModel mdl,BussEntry be) {
      for (TreeNode tn : child_nodes) {
         BussTreeImpl btn = (BussTreeImpl) tn;
         if (tn.isLeaf() && btn.getEntry() == be) {
            TreeNode [] rem = new TreeNode[1];
            int [] idx = new int[1];
            rem[0] = tn;
            idx[0] = getIndex(tn);
            child_nodes.remove(tn);
            mdl.nodesWereRemoved(this,idx,rem);
            return true;
          }
         else if (btn.removeEntry(mdl,be)) {
            if (tn.getChildCount() == 0) {
               TreeNode [] rem = new TreeNode[1];
               int [] idx = new int[1];
               rem[0] = tn;
               idx[0] = getIndex(tn);
               child_nodes.remove(tn);
               mdl.nodesWereRemoved(this,idx,rem);
             }
            return true;
          } 
       }
      return false;
    }

   @Override void disposeBubbles() {
      for (TreeNode tn : child_nodes) {
	 BussTreeImpl bti = (BussTreeImpl) tn;
	 bti.disposeBubbles();
       }
    }

}	// end of inner class Branch



}	// end of class BussTreeModel




/* end of BussTreeModel.java */
