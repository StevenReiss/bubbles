/********************************************************************************/
/*										*/
/*		BddtStackModel.java						*/
/*										*/
/*	Bubbles Environment debugger tool tree model for stack/frame/values	*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.bubbles.bddt;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.swing.SwingEventListenerList;
import edu.brown.cs.ivy.swing.SwingTreeTable;

import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


class BddtStackModel implements SwingTreeTable.TreeTableModel, BddtConstants, BumpConstants {




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private SwingEventListenerList<TreeModelListener> model_listeners;
private AbstractNode		root_node;
private RunEventHandler 	event_handler;
private BddtStackModel		parent_model;
private List<BddtStackModel>	sub_models;
private Lock			model_lock;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BddtStackModel(BddtStackModel parent,ValueTreeNode root)
{
   initialize();

   AbstractNode rn = (AbstractNode) root;

   List<AbstractNode> inodes = new ArrayList<AbstractNode>();
   for (AbstractNode xn = rn; xn != null; xn = xn.getBddtParent()) {
      if (xn == parent.root_node) break;
      inodes.add(xn);
    }
   if (inodes.size() == 0)
      inodes.add(rn);

   root_node = new RootNode((AbstractNode) root,inodes);
   parent_model = parent;

   parent.addChildModel(this);
}



BddtStackModel(BumpThread thr)
{
   initialize();

   root_node = new ThreadNode(null,thr);

   setupUpdate();
}



BddtStackModel(BumpThreadStack stk)
{
   initialize();

   root_node = new StackNode(null,stk);

   setupUpdate();
}



BddtStackModel(BumpStackFrame frm)
{
   initialize();

   root_node = new FrameNode(null,frm);

   setupUpdate();
}



BddtStackModel(BumpRunValue val)
{
   initialize();

   root_node = getValueNode(null,val);

   setupUpdate();
}



private void initialize()
{
   event_handler = null;
   parent_model = null;
   sub_models = new ArrayList<BddtStackModel>();
   model_lock = new ReentrantLock();
   model_listeners = new SwingEventListenerList<>(TreeModelListener.class);
}



void dispose()
{
   if (parent_model != null) {
      parent_model.removeChildModel(this);
    }
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BumpThread getThread()			{ return root_node.getThread(); }
BumpStackFrame getFrame()		{ return root_node.getFrame(); }


String getLabel()			{ return root_node.getLabel(); }

BumpRunValue getRunValue()		{ return root_node.getRunValue(); }


boolean hasBeenFrozen() 		{ return root_node.isFrozen(); }

boolean showValueArea() 		{ return root_node.showValueArea(); }



void addChildModel(BddtStackModel c)
{
   synchronized (sub_models) {
      sub_models.add(c);
    }
}


void removeChildModel(BddtStackModel c)
{
   synchronized (sub_models) {
      sub_models.remove(c);
    }
}



void lock()
{
   if (parent_model != null) {
      parent_model.lock();
    }
   else {
      model_lock.lock();
    }
}



void unlock()
{
   if (parent_model != null) {
      parent_model.unlock();
    }
   else {
      model_lock.unlock();
    }
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



void noteChange(Object src)
{
   Object [] path = new Object[] { root_node };
   TreeModelEvent evt = new TreeModelEvent(src,path);
   if (root_node.isLeaf()) fireTreeNodesChanged(evt);
   else fireTreeStructureChanged(evt);

   List<BddtStackModel> sml;
   synchronized (sub_models) {
      sml = new ArrayList<BddtStackModel>(sub_models);
    }
   for (BddtStackModel sm : sml) {
      sm.fixRoot();
      // sm.noteChange(src);
      sm.noteChange(sm);
    }
}



void fixRoot()
{
   root_node.fixAfterChange();
}



/********************************************************************************/
/*										*/
/*	Freeze and update methods						*/
/*										*/
/********************************************************************************/

private void setupUpdate()
{
   event_handler = new RunEventHandler();

   BumpRunModel rm = BumpClient.getBump().getRunModel();
   rm.addRunEventHandler(event_handler);
}


private void removeUpdate()
{
   if (event_handler != null) {
      BumpRunModel rm = BumpClient.getBump().getRunModel();
      rm.removeRunEventHandler(event_handler);
      event_handler = null;
    }
}



void freeze(int lvls)
{
   if (event_handler != null) removeUpdate();
   else {
      // TODO: need to clone the tree structure here
    }

   root_node.freeze(lvls);
}



void updateValues()
{
   lock();
   try {
      root_node.update();
    }
   finally { unlock(); }

   SwingUtilities.invokeLater(
      new Runnable() {
	 @Override public void run() {
	    noteChange(this);
	  }
       }
      );
}



/********************************************************************************/
/*										*/
/*	Run event handler							*/
/*										*/
/********************************************************************************/

private class RunEventHandler implements BumpRunEventHandler {

   @Override public void handleLaunchEvent(BumpRunEvent evt)		{ }

   @Override public void handleProcessEvent(BumpRunEvent evt)		{ }

   @Override public void handleThreadEvent(BumpRunEvent evt) {
      if (evt.getThread() != getThread()) return;
      switch (evt.getEventType()) {
	 case THREAD_REMOVE :
	    removeUpdate();
	    break;
	 case THREAD_CHANGE :
	    if (event_handler == null) return;
	    if (getThread().getThreadState().isStopped())
	       updateValues();
	    break;
	 default:
	    break;
       }
    }

   @Override public void handleConsoleMessage(BumpProcess bp,boolean err,boolean eof,String msg)	{ }

}	// end of inner class RunEventHandler




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
   BoardLog.logD("BDDT","Value changed for tree");
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

   if (col == 0) return an.getName();
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

protected abstract class AbstractNode implements ValueTreeNode, TreeNode {

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
   @Override public Object getValue()				{ return null; }
   abstract ValueSetType getType();
   abstract BumpThread getThread();
   @Override public BumpStackFrame getFrame()			{ return null; }
   @Override public BumpRunValue getRunValue()			{ return null; }
   @Override public boolean showValueArea()			{ return false; }

   void fixAfterChange()					{ }
   boolean sameNode(AbstractNode an)				{ return false; }
   boolean matchNode(AbstractNode an)				{ return false; }
   boolean isFrozen()						{ return false; }

   protected AbstractNode getBddtParent()			{ return parent_node; }

   @Override public String getKey()				{ return getName(); }

   @Override public TreeNode getParent()			{ return parent_node; }

   @Override public boolean isLeaf()				{ return false; }

   @Override public Enumeration<AbstractNode> children()  {
      computeChildren();
      return child_nodes.elements();
    }

   @Override public boolean getAllowsChildren() 		{ return true; }

   @Override public TreeNode getChildAt(int idx) {
      computeChildren();
      if (idx < 0 || idx >= child_nodes.size()) return null;
      return child_nodes.get(idx);
    }

   @Override public int getChildCount() {
      if (children_known && getType() == ValueSetType.THREAD && child_nodes.isEmpty())
	 children_known = false;
      computeChildren();
      return child_nodes.size();
    }

   @Override public int getIndex(TreeNode tn) {
      computeChildren();
      return child_nodes.indexOf(tn);
    }

   private synchronized void computeChildren() {
      if (children_known) return;
      if (isLeaf() || !getAllowsChildren()) {
	 child_nodes = new Vector<AbstractNode>();
       }
      else {
	 sorted_children = new TreeSet<AbstractNode>(new ValueSorter());
	 expandChildren();
	 if (sorted_children == null) return;
	 child_nodes = new Vector<AbstractNode>(sorted_children);
	 sorted_children = null;
	 children_known = true;
       }
    }

   protected void expandChildren()					{ }

   void update() {
      if (isLeaf()) return;
      if (!children_known) return;
      // Vector<AbstractNode> ochildren = child_nodes;
      children_known = false;
      computeChildren();
      // flag nodes changed
      // compare new values to old
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

   void freeze(int lvls) {
      if (isLeaf()) return;
      if (!children_known) {
	 computeChildren();
	 lvls -= 1;
       }
      if (lvls > 0) {
	 for (AbstractNode an : child_nodes) {
	    an.freeze(lvls);
	  }
       }
    }

   @Override public String toString() {
      String nm = getName();
      if (nm == null) return "???";
      int idx = nm.lastIndexOf("?");
      if (idx >= 0) nm = nm.substring(idx+1);
      return nm;
    }

   abstract String getLabel();

}	// end of inner class AbstractNode




/********************************************************************************/
/*										*/
/*	Value sorter								*/
/*										*/
/********************************************************************************/

private static class ValueSorter implements Comparator<AbstractNode> {

   @Override public int compare(AbstractNode n1,AbstractNode n2) {
      if (n1.getType() != n2.getType()) {
	 return n1.getType().ordinal() - n2.getType().ordinal();
       }
      switch (n1.getType()) {
	 default :
	 case THREAD :
	 case STACK :
	    int v = n1.getName().compareTo(n2.getName());
	    if (v != 0) return v;
	    return n1.hashCode() - n2.hashCode();
	 case FRAME :
	    return n1.getFrame().getLevel() - n2.getFrame().getLevel();
	 case VALUE :
	    // locals first
	    if (n1.getRunValue().isLocal() != n2.getRunValue().isLocal()) {
	       if (n1.getRunValue().isLocal()) return -1;
	       return 1;
	     }
	    String nm1 = n1.getName();
	    String nm2 = n2.getName();
	    int idx1 = nm1.lastIndexOf('?');
	    int idx2 = nm2.lastIndexOf('?');
	    if (idx1 >= 0 && idx1 == idx2 && nm2.startsWith(nm1.substring(0,idx1)) &&
		   nm1.length() > idx1+1 && nm2.length() > idx2+1 &&
		   nm1.charAt(idx1+1) == '[' && nm2.charAt(idx2+1) == '[') {
	       int idx3 = nm1.indexOf("]",idx1+1);
	       int idx4 = nm2.indexOf("]",idx2+1);
	       try {
		  int v1 = Integer.parseInt(nm1.substring(idx1+2,idx3));
		  int v2 = Integer.parseInt(nm2.substring(idx2+2,idx4));
		  if (v1 < v2) return -1;
		  if (v1 > v2) return 1;
		}
	       catch (NumberFormatException e) { }
	     }

	    // check if inherited and put last if so
	    // put static after class variables
	    v = nm1.compareTo(nm2);
	    if (v != 0) return v;
	    return n1.hashCode() - n2.hashCode();
       }
    }

}	// end of inner class ValueSorter



/********************************************************************************/
/*										*/
/*	Root node								*/
/*										*/
/********************************************************************************/

private class RootNode extends AbstractNode {

   private AbstractNode base_node;
   private List<AbstractNode> interim_nodes;
   private boolean is_frozen;

   RootNode(AbstractNode base,List<AbstractNode> inodes) {
      super(null);
      base_node = base;
      interim_nodes = inodes;
      is_frozen = false;
    }

   @Override String getName()				{ return base_node.getName(); }
   @Override public Object getValue()			{ return base_node.getValue(); }
   @Override ValueSetType getType()			{ return base_node.getType(); }
   @Override BumpThread getThread()			{ return base_node.getThread(); }
   @Override public BumpStackFrame getFrame()		{ return base_node.getFrame(); }
   @Override public BumpRunValue getRunValue()		{ return base_node.getRunValue(); }
   @Override public boolean showValueArea()		{ return base_node.showValueArea(); }

   @Override boolean isFrozen() 			{ return is_frozen; }

   @Override public TreeNode getParent()		{ return null; }

   @Override public boolean isLeaf()			{ return base_node.isLeaf(); }

   @Override public Enumeration<AbstractNode> children() { return base_node.children(); }

   @Override public boolean getAllowsChildren() 	{ return base_node.getAllowsChildren(); }

   @Override public TreeNode getChildAt(int idx)	{ return base_node.getChildAt(idx); }

   @Override public int getChildCount() 		{ return base_node.getChildCount(); }

   @Override public int getIndex(TreeNode tn)		{ return base_node.getIndex(tn); }

   @Override protected void expandChildren()		{ base_node.expandChildren(); }

   @Override void freeze(int lvls) {
      base_node.freeze(lvls);
      is_frozen = true;
    }

   @Override void update()				{ base_node.update(); }

   @Override String getLabel()				{ return base_node.getLabel(); }

   @Override void fixAfterChange() {
      if (updateInterimNodes()) {
	 if (interim_nodes.size() == 0)
	    System.err.println("BAD INTERIM NODES");
	 else
	 base_node = interim_nodes.get(0);
      }
      else freeze(0);
    }

   boolean updateInterimNodes() {
      int ln = interim_nodes.size();
      LinkedList<AbstractNode> nl = new LinkedList<>();
      AbstractNode pn = null;
      for (int i = ln-1; i >= 0; --i) {
         AbstractNode an = interim_nodes.get(i);
         if (pn == null) {
            pn = an.getBddtParent();
            if (pn == null) return true;
          }
         AbstractNode nn = replaceNode(pn,an,i);
         if (nn == null)
            return false;
         nl.addFirst(nn);
         pn = nn;
      }
      interim_nodes.clear();
      interim_nodes.addAll(nl);
      return true;
   }

   AbstractNode replaceNode(AbstractNode pn,AbstractNode xn,int idx) {
      if (pn == null) return null;
      AbstractNode newbase = null;
      int ct = pn.getChildCount();
      for (int i = 0; i < ct; ++i) {
	 AbstractNode cn = (AbstractNode) pn.getChildAt(i);
	 if (cn == xn) return cn;
	 if (cn.getType() == xn.getType() && cn.sameNode(xn))
	    newbase = cn;
	 else if (i == idx && cn.getType() == xn.getType() && cn.matchNode(xn))
	    newbase = cn;
       }
      if (newbase == null) {
	 BoardLog.logW("BDDT","REPLACEMENT FOR " + pn + " :: " + xn + " NOT FOUND");
      }
      return newbase;
    }

}	// end of inner class RootNode




/********************************************************************************/
/*										*/
/*	Thread tree node							*/
/*										*/
/********************************************************************************/

private class ThreadNode extends AbstractNode {

   private BumpThread		for_thread;

   ThreadNode(AbstractNode par,BumpThread thr) {
      super(par);
      for_thread = thr;
    }

   @Override protected void expandChildren() {
      BumpThreadStack stk = for_thread.getStack();
      if (stk == null) return;
      for (int i = 0; i < stk.getNumFrames(); ++i) {
	 addChild(new FrameNode(this,stk.getFrame(i)));
       }
    }

   @Override String getName()		{ return for_thread.getName(); }

   @Override ValueSetType getType()	{ return ValueSetType.THREAD; }

   @Override BumpThread getThread()	{ return for_thread; }

   @Override String getLabel()		{ return for_thread.getName(); }

   @Override boolean sameNode(AbstractNode an) {
      if (an instanceof ThreadNode) {
	 return for_thread == an.getThread();
       }
      return false;
    }

}	// end of inner class StackTreeNode





/********************************************************************************/
/*										*/
/*	Stack tree node 							*/
/*										*/
/********************************************************************************/

private class StackNode extends AbstractNode {

   private BumpThreadStack	for_stack;

   StackNode(AbstractNode par,BumpThreadStack stk) {
      super(par);
      for_stack = stk;
    }

   @Override protected void expandChildren() {
      for (int i = 0; i < for_stack.getNumFrames(); ++i) {
	 addChild(new FrameNode(this,for_stack.getFrame(i)));
       }
    }

   @Override String getName()		{ return for_stack.getThread().getName(); }

   @Override ValueSetType getType()	{ return ValueSetType.STACK; }

   @Override BumpThread getThread()	{ return for_stack.getThread(); }

   @Override String getLabel()		{ return getName(); }

   @Override boolean sameNode(AbstractNode an) {
      if (an instanceof StackNode) {
	 StackNode sn = (StackNode) an;
	 return for_stack == sn.for_stack;
       }
      return false;
    }

}	// end of inner class StackTreeNode





/********************************************************************************/
/*										*/
/*	Frame tree node 							*/
/*										*/
/********************************************************************************/

private class FrameNode extends AbstractNode {

   private BumpStackFrame for_frame;

   FrameNode(AbstractNode par,BumpStackFrame frm) {
      super(par);
      for_frame = frm;
    }

   @Override protected void expandChildren() {
      CategoryNode thisnode = null;
      CategoryNode thisstatic = null;
      CategoryNode supernode = null;
      CategoryNode superstatic = null;

      for (String s : for_frame.getVariables()) {
	 if (s.startsWith("$")) continue;
	 BumpRunValue v = for_frame.getValue(s);
	 if (v == null) continue;
	 AbstractNode an = getValueNode(this,v);
	 if (an == null) continue;
	 if (v.isLocal() || v.getDeclaredType() == null) addChild(an);
	 else if (v.getDeclaredType().equals(for_frame.getFrameClass())) {
	    if (v.isStatic()) {
	       if (thisstatic == null) {
		  thisstatic = new CategoryNode(this,"<this.static>");
		  addChild(thisstatic);
		}
	       thisstatic.addChild(an);
	     }
	    else {
	       if (thisnode == null) {
		  thisnode = new CategoryNode(this,"this");
		  addChild(thisnode);
		}
	       thisnode.addChild(an);
	     }
	  }
	 else {
	    if (v.isStatic()) {
	       if (superstatic == null) {
		  superstatic = new CategoryNode(this,"<super.static>");
		  addChild(superstatic);
		}
	       superstatic.addChild(an);
	     }
	    else {
	       if (supernode == null) {
		  supernode = new CategoryNode(this,"<super>");
		  addChild(supernode);
		}
	       supernode.addChild(an);
	     }
	  }
       }

      if (thisnode != null) thisnode.sortChildren();
      if (thisstatic != null) thisstatic.sortChildren();
      if (supernode != null) supernode.sortChildren();
      if (superstatic != null) superstatic.sortChildren();
    }

   @Override String getName() {
      return for_frame.getDisplayString();
    }

   @Override public String getKey()	{ return for_frame.getMethod(); }

   @Override ValueSetType getType()	{ return ValueSetType.FRAME; }

   @Override BumpThread getThread()	{ return for_frame.getThread(); }
   @Override public BumpStackFrame getFrame()	{ return for_frame; }

   @Override String getLabel() {
      return getThread().getName() + " :: " + for_frame.getMethod() + " @ " + for_frame.getLineNumber();
    }

   @Override boolean sameNode(AbstractNode an) {
      if (an instanceof FrameNode) {
	 FrameNode fn = (FrameNode) an;
	 if (for_frame.getId().equals(fn.for_frame.getId())) return true;
       }
      return false;
    }

   @Override boolean matchNode(AbstractNode an) {
      if (an instanceof FrameNode) {
	 FrameNode fn = (FrameNode) an;
	 if (for_frame.getId().equals(fn.for_frame.getId())) return true;
	 if (for_frame.getMethod().equals(fn.for_frame.getMethod())) return true;
       }
      return false;
    }



}	// end of inner class FrameNode




/********************************************************************************/
/*										*/
/*	Frame Category tree node						*/
/*										*/
/********************************************************************************/

private class CategoryNode extends AbstractNode {

   private String category_name;

   CategoryNode(AbstractNode par,String name) {
      super(par);
      category_name = name;
    }

   @Override protected void expandChildren()		{ }

   @Override String getName()				{ return category_name; }

   @Override ValueSetType getType()			{ return ValueSetType.CATEGORY; }
   @Override BumpThread getThread()			{ return getBddtParent().getThread(); }
   @Override public BumpStackFrame getFrame()		{ return getBddtParent().getFrame(); }

   @Override String getLabel() {
      return getThread().getName() + " :: " + getFrame().getMethod() +
	 " @ " + getFrame().getLineNumber() + " :: " + category_name;
    }

   @Override boolean sameNode(AbstractNode an) {
      if (an instanceof CategoryNode) {
	 CategoryNode cn = (CategoryNode) an;
	 return category_name.equals(cn.category_name);
       }
      return false;
    }

}	// end of inner class CategoryNode




/********************************************************************************/
/*										*/
/*	Value tree node 							*/
/*										*/
/********************************************************************************/

private AbstractNode getValueNode(AbstractNode par,BumpRunValue val)
{
   if (val == null) return null;

   switch (val.getKind()) {
      case UNKNOWN :
	 return null;
      case PRIMITIVE :
	 return new PrimitiveValueNode(par,val);
      case STRING :
	 return new StringValueNode(par,val);
      case CLASS :
	 return new ClassValueNode(par,val);
      case OBJECT :
	 return new ObjectValueNode(par,val);
      case ARRAY :
	 return new ArrayValueNode(par,val);
    }

   return null;
}



private abstract class ValueNode extends AbstractNode {

   protected BumpRunValue for_value;

   ValueNode(AbstractNode par,BumpRunValue val) {
      super(par);
      for_value = val;
    }

   @Override String getName()			{ return for_value.getName(); }

   @Override public Object getValue()		{ return for_value.getValue(); }

   @Override ValueSetType getType()		{ return ValueSetType.VALUE; }

   @Override BumpThread getThread()		{ return for_value.getThread(); }
   @Override public BumpStackFrame getFrame()	{ return for_value.getFrame(); }
   @Override public BumpRunValue getRunValue()	{ return for_value; }
   @Override public boolean showValueArea()	{ return true; }

   @Override String getLabel() {
      return getThread().getName() + " :: " + getFrame().getMethod() +
	 " @ " + getFrame().getLineNumber() + " :: " + for_value.getName();
    }

   @Override boolean sameNode(AbstractNode an) {
      if (an instanceof ValueNode) {
	 return getName().equals(an.getName());
       }
      return false;
    }

}	// end of abstract inner class ValueNode




/********************************************************************************/
/*										*/
/*	Primitive value tree node						*/
/*										*/
/********************************************************************************/

private class PrimitiveValueNode extends ValueNode {

   PrimitiveValueNode(AbstractNode par,BumpRunValue val) {
      super(par,val);
    }

   @Override public boolean isLeaf()		{ return true; }
   @Override public boolean getAllowsChildren() { return false; }

}	// end of inner class PrimitiveValueNode




/********************************************************************************/
/*										*/
/*	String value tree node							*/
/*										*/
/********************************************************************************/

private class StringValueNode extends ValueNode {

   StringValueNode(AbstractNode par,BumpRunValue val) {
      super(par,val);
    }

   @Override public boolean isLeaf()		{ return true; }
   @Override public boolean getAllowsChildren() { return false; }
   @Override public Object getValue() {
      String v = super.getValue().toString();
      if (v != null && v.startsWith("<html>")) {
	 v = "HTML:" + v;
       }
      return v;
    }

}	// end of inner class StringValueNode




/********************************************************************************/
/*										*/
/*	Class value tree node							*/
/*										*/
/********************************************************************************/

private class ClassValueNode extends ValueNode {

   ClassValueNode(AbstractNode par,BumpRunValue val) {
      super(par,val);
    }

   @Override public boolean isLeaf()		{ return true; }
   @Override public boolean getAllowsChildren() { return false; }

}	// end of inner class ClassValueNode




/********************************************************************************/
/*										*/
/*	Object value tree node							*/
/*										*/
/********************************************************************************/

private class ObjectValueNode extends ValueNode {

   ObjectValueNode(AbstractNode par,BumpRunValue val) {
      super(par,val);
    }

   @Override public boolean isLeaf()		{ return !for_value.hasContents(); }
   @Override public boolean getAllowsChildren() { return for_value.hasContents(); }

   @Override public Object getValue() {
      if (for_value.getType().equals("null")) return "null";
      else if (for_value.getType().equals("java.lang.String")) {
	 return "\"" + for_value.getValue() + "\"";
       }
      return for_value.getType() + " " + for_value.getValue();
    }

   @Override protected void expandChildren() {
      CategoryNode thisstatic = null;
      CategoryNode supernode = null;
      CategoryNode superstatic = null;

      for (String s : for_value.getVariables()) {
	 BumpRunValue v = for_value.getValue(s);
	 if (v == null) continue;
	 AbstractNode an = getValueNode(this,v);
	 if (an == null) continue;
	 if (v.getDeclaredType() == null) addChild(an);
	 else if (v.getDeclaredType().equals(for_value.getType())) {
	    if (v.isStatic()) {
	       if (thisstatic == null) {
		  thisstatic = new CategoryNode(this,"<static>");
		  addChild(thisstatic);
		}
	       thisstatic.addChild(an);
	     }
	    else addChild(an);
	  }
	 else {
	    if (v.isStatic()) {
	       if (superstatic == null) {
		  superstatic = new CategoryNode(this,"<super.static>");
		  addChild(superstatic);
		}
	       superstatic.addChild(an);
	     }
	    else {
	       if (supernode == null) {
		  supernode = new CategoryNode(this,"<super>");
		  addChild(supernode);
		}
	       supernode.addChild(an);
	     }
	  }
       }

      if (thisstatic != null) thisstatic.sortChildren();
      if (supernode != null) supernode.sortChildren();
      if (superstatic != null) superstatic.sortChildren();
    }


}	// end of inner class ObjectValueNode




/********************************************************************************/
/*										*/
/*	Array value tree node							*/
/*										*/
/********************************************************************************/

private class ArrayValueNode extends ValueNode {

   ArrayValueNode(AbstractNode par,BumpRunValue val) {
      super(par,val);
      // want to compute the range here, possibly automatically create children
      // that correspond to subranges of varying size as is done in eclipse
    }

   @Override public boolean isLeaf()		{ return !for_value.hasContents(); }
   @Override public boolean getAllowsChildren() { return for_value.hasContents(); }
   @Override public Object getValue() {
      String typ = for_value.getType();
      int idx = typ.lastIndexOf("[]");
      return typ.substring(0,idx) + "[" + for_value.getLength() + "] " + for_value.getValue();
   }

   @Override protected void expandChildren() {
      for (String s : for_value.getVariables()) {
	 AbstractNode an = getValueNode(this,for_value.getValue(s));
	 if (an != null) addChild(an);
       }
    }

}	// end of inner class ArrayValueNode




}	// end of class BddtStackModel




/* end of BddtStackModel.java */

