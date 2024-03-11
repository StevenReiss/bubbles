/********************************************************************************/
/*										*/
/*		BddtStopTraceBubble.java					*/
/*										*/
/*	Bubbles Environment dyanmic debugger tool history viewer		*/
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


import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



class BddtStopTraceBubble extends BudaBubble implements BddtConstants, BumpConstants,
		BudaConstants, BumpConstants.BumpRunEventHandler, ChangeListener
{




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private StopTracePanel	draw_area;
private JScrollPane	scroll_pane;
private JSlider 	time_slider;
private BumpThread	for_thread;
private boolean 	have_data;
private RootNode	tree_root;
private long		base_time;
private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BddtStopTraceBubble(BddtLaunchControl ctrl,BumpThread thrd)
{
   for_thread = thrd;
   have_data = false;
   tree_root = new RootNode();
   base_time = 0;

   setupPanel();

   BumpClient.getBump().getRunModel().addRunEventHandler(this);
   thrd.requestHistory();

   getContentPane().addMouseListener(new FocusOnEntry());
}



@Override protected void localDispose()
{
   BumpClient.getBump().getRunModel().removeRunEventHandler(this);
}



/********************************************************************************/
/*										*/
/*	Methods to setup the display						*/
/*										*/
/********************************************************************************/

private void setupPanel()
{
   SwingGridPanel pnl = new SwingGridPanel();

   draw_area = new StopTracePanel();
   scroll_pane = new JScrollPane(draw_area);

   String ttl = "Thread History before `" + for_thread.getName() + "' stopped";
   JLabel lbl = new JLabel(ttl);
   lbl.setHorizontalAlignment(SwingConstants.CENTER);
   pnl.addGBComponent(lbl,0,0,1,1,10,0);

   Dimension d = new Dimension(BDDT_STOP_TRACE_WIDTH,BDDT_STOP_TRACE_HEIGHT);
   draw_area.setSize(d);
   pnl.addGBComponent(scroll_pane,0,1,1,1,10,10);

   time_slider = new JSlider();
   time_slider.addChangeListener(this);
   pnl.addGBComponent(time_slider,0,2,1,1,10,0);

   setContentPane(pnl);
}




/********************************************************************************/
/*										*/
/*	Handle user actions							*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   // JPopupMenu popup = new JPopupMenu();
   // Point pt = SwingUtilities.convertPoint(getContentPane().getParent(),e.getPoint(),draw_area);
}



/********************************************************************************/
/*										*/
/*	Run Event management							*/
/*										*/
/********************************************************************************/

@Override public void handleThreadEvent(BumpRunEvent evt)
{
   if (evt.getEventType() == BumpRunEventType.THREAD_HISTORY) {
      if (!have_data && evt.getThread() == for_thread) {
	 have_data = true;
	 Element xml = (Element) evt.getEventData();
	 System.err.println("DISPLAY TRACES: " + IvyXml.convertXmlToString(xml));
	 tree_root.setup(xml);
	 long start = IvyXml.getAttrLong(xml,"START");
	 long end = IvyXml.getAttrLong(xml,"STOP");
	 base_time = start;
	 time_slider.setMinimum(0);
	 time_slider.setMaximum((int)(end-start));
	 time_slider.setValue((int)((end - start)/2));
       }
    }
}


/********************************************************************************/
/*										*/
/*	Slider event management 						*/
/*										*/
/********************************************************************************/

@Override public void stateChanged(ChangeEvent e)
{
   if (base_time == 0 || tree_root == null) return;

   int v = time_slider.getValue();
   long time = v + base_time;

   tree_root.updateStack(time);
   draw_area.updateTree();
}




/********************************************************************************/
/*										*/
/*	Panel for drawing history						*/
/*										*/
/********************************************************************************/

private class StopTracePanel extends JTree implements TreeExpansionListener {

   private DefaultTreeModel tree_model;
   private Set<String> expanded_threads;
   private static final long serialVersionUID = 1;
   
   StopTracePanel() {
      tree_model = new DefaultTreeModel(tree_root);
      expanded_threads = new HashSet<>();
      setModel(tree_model);
      setEditable(false);
      setRootVisible(false);
      setToolTipText("StopTrace Panel");
      addTreeExpansionListener(this);
      setScrollsOnExpand(true);
    }

   @Override public String getToolTipText(MouseEvent e) {
      return null;
    }

   void updateTree() {
      Set<String> oexp = new HashSet<String>(expanded_threads);
      expanded_threads.clear();
      tree_model.setRoot(tree_root);
      Object [] elts = new Object[2];
      elts[0] = tree_root;
      for (Enumeration<?> e = tree_root.children(); e.hasMoreElements(); ) {
	 ThreadNode tn = (ThreadNode) e.nextElement();
	 if (oexp.contains(tn.getThreadName())) {
	    elts[1] = tn;
	    TreePath tp = new TreePath(elts);
	    expandPath(tp);
	    expanded_threads.add(tn.getThreadName());
	  }
       }
   }

   @Override public void treeCollapsed(TreeExpansionEvent evt) {
      ThreadNode tn = (ThreadNode) evt.getPath().getLastPathComponent();
      expanded_threads.remove(tn.getThreadName());
    }

   @Override public void treeExpanded(TreeExpansionEvent evt) {
      ThreadNode tn = (ThreadNode) evt.getPath().getLastPathComponent();
      expanded_threads.add(tn.getThreadName());
    }

}	// end of inner class StopTracePanel




/********************************************************************************/
/*										*/
/*     Tree Model for stacks							*/
/*										*/
/********************************************************************************/

private class RootNode implements TreeNode {

   private List<ThreadNode> thread_nodes;

   RootNode() {
      thread_nodes = new ArrayList<ThreadNode>();
    }

   void setup(Element xml) {
      for (Element td : IvyXml.children(xml,"THREAD")) {
	 ThreadNode tn = new ThreadNode(this,td);
	 thread_nodes.add(tn);
       }
    }

   void updateStack(long when) {
      for (ThreadNode tn : thread_nodes) tn.updateStack(when);
    }

   @Override public Enumeration<ThreadNode> children()	{ return Collections.enumeration(thread_nodes); }
   @Override public boolean getAllowsChildren() 	{ return true; }
   @Override public TreeNode getChildAt(int idx)	{ return thread_nodes.get(idx); }
   @Override public int getChildCount() 		{ return thread_nodes.size(); }
   @Override public int getIndex(TreeNode tn)		{ return thread_nodes.indexOf(tn); }
   @Override public TreeNode getParent()		{ return null; }
   @Override public boolean isLeaf()			{ return false; }

}	// end of inner class RootNode



private static class ThreadNode implements TreeNode {

   private RootNode	     parent_node;
   private Element	     thread_data;
   private List<StackNode>   stack_nodes;
   private String	     last_state;

   ThreadNode(RootNode rn,Element xml) {
      parent_node = rn;
      stack_nodes = new ArrayList<>();
      thread_data = xml;
    }

   void updateStack(long when) {
      stack_nodes.clear();
      Element trace = null;
      last_state = null;
      long best = 0;
      for (Element td : IvyXml.children(thread_data,"TRACE")) {
	 long w = IvyXml.getAttrLong(td,"WHEN");
	 if (w >= when && (best == 0 || w < best)) {
	    trace = td;
	    best = w;
	  }
       }
      if (trace != null) {
	 last_state = IvyXml.getAttrString(trace,"STATE");
	 for (Element c : IvyXml.children(trace,"STACK")) {
	    StackNode sn = new StackNode(this,c);
	    stack_nodes.add(sn);
	  }
       }
    }

   String getThreadName()				{ return IvyXml.getAttrString(thread_data,"NAME"); }

   @Override public Enumeration<StackNode> children()	{ return Collections.enumeration(stack_nodes); }
   @Override public boolean getAllowsChildren() 	{ return true; }
   @Override public TreeNode getChildAt(int idx)	{ return stack_nodes.get(idx); }
   @Override public int getChildCount() 		{ return stack_nodes.size(); }
   @Override public int getIndex(TreeNode tn)		{ return stack_nodes.indexOf(tn); }
   @Override public TreeNode getParent()		{ return parent_node; }
   @Override public boolean isLeaf()			{ return false; }

   @Override public String toString() {
      String nm = getThreadName();
      if (last_state != null) nm += " (" + last_state + ")";
      return nm;
   }

}	// end of inner class ThreadNode



private static class StackNode implements TreeNode {

   private Element stack_data;
   private ThreadNode stack_thread;

   StackNode(ThreadNode tn,Element xml) {
      stack_thread = tn;
      stack_data = xml;
    }

   @Override public Enumeration<? extends TreeNode> children() { return null; }
   @Override public boolean getAllowsChildren() 	{ return false; }
   @Override public TreeNode getChildAt(int idx)	{ return null; }
   @Override public int getChildCount() 		{ return 0; }
   @Override public int getIndex(TreeNode tn)		{ return -1; }
   @Override public TreeNode getParent()		{ return stack_thread; }
   @Override public boolean isLeaf()			{ return true; }

   @Override public String toString() {
      return IvyXml.getAttrString(stack_data,"CLASS") + "." +
	   IvyXml.getAttrString(stack_data,"METHOD") + "(" +
	      IvyXml.getAttrString(stack_data,"LINE") + ")";
   }

}	// end of inner class StackNode



}	// end of class BddtStopTraceBubble




/* end of BddtStopTraceBubble.java */

