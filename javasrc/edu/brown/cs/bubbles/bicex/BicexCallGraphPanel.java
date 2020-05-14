/********************************************************************************/
/*										*/
/*		BicexCallGraphBubble.java					*/
/*										*/
/*	Call graph viewer for continuous execution				*/
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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.ivy.petal.PetalArc;
import edu.brown.cs.ivy.petal.PetalArcDefault;
import edu.brown.cs.ivy.petal.PetalArcEnd;
import edu.brown.cs.ivy.petal.PetalArcEndDefault;
import edu.brown.cs.ivy.petal.PetalEditor;
import edu.brown.cs.ivy.petal.PetalLevelLayout;
import edu.brown.cs.ivy.petal.PetalModelDefault;
import edu.brown.cs.ivy.petal.PetalNode;
import edu.brown.cs.ivy.petal.PetalNodeDefault;
import edu.brown.cs.ivy.petal.PetalUndoSupport;

class BicexCallGraphPanel extends BicexPanel implements BicexConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private CallGraph		call_graph;
private boolean 		call_tree;

private static final String DEFAULT_BACKGROUND_PROP = "Bicex.CallGraphBackground";
private static final String CURRENT_BACKGROUND_PROP = "Bicex.CallGraphCurrent";



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BicexCallGraphPanel(BicexEvaluationViewer ev,boolean tree)
{
   super(ev);
   call_tree = tree;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override boolean allowMouseWheelScrolling()		{ return false; }



/********************************************************************************/
/*										*/
/*	Panel setup methods							*/
/*										*/
/********************************************************************************/

@Override protected JComponent setupPanel()
{
   call_graph = new CallGraph();
   return call_graph;
}



/********************************************************************************/
/*										*/
/*	Interaction methods							*/
/*										*/
/********************************************************************************/

@Override void handlePopupMenu(JPopupMenu menu,MouseEvent evt)
{
   Node n = call_graph.findNode(evt.getPoint());
   Arc a = call_graph.findArc(evt.getPoint());
   if (n == null && a == null) return;

   if (n != null) {
      BicexEvaluationContext ctx = n.getContext();
      if (ctx != null) {
	 menu.add(getContextAction(ctx));
	 // menu.add(getSourceAction(ctx));
       }
    }
}




/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

@Override void update()
{
   BoardThreadPool.start(new Updater());
}



private class Updater implements Runnable {

   @Override public void run() {
      call_graph.update();
      updateTime();
    }

}	// end of inner class Updater



@Override void updateTime()
{
   BicexExecution bex = getExecution();
   if (bex == null) return;
   BicexEvaluationContext ctx = eval_viewer.getContextForTime(getRootContext(),
	 bex.getCurrentTime());

   Node cur = call_graph.findNode(ctx);

   if (cur == null) return;

   call_graph.setCurrent(cur);
}








/********************************************************************************/
/*										*/
/*	Call graph display							*/
/*										*/
/********************************************************************************/

private class CallGraph extends JPanel {

   private PetalEditor		petal_editor;
   private PetalModelDefault	petal_model;
   private PetalLevelLayout	layout_method;
   private Node 		current_node;

   private static final long serialVersionUID = 1;


   CallGraph() {
      super(new BorderLayout());
      setPreferredSize(new Dimension(300,300));
      PetalUndoSupport.getSupport().blockCommands();
      petal_model = new Model();
      petal_editor = new PetalEditor(petal_model);
      layout_method = new PetalLevelLayout(petal_editor);
      layout_method.setSplineArcs(false);
      layout_method.setLevelX(false);
      add(petal_editor,BorderLayout.CENTER);
      current_node = null;
      petal_editor.addZoomWheeler();
    }

   void update() {
      petal_model.clear();
      BicexEvaluationContext root = getRootContext();
      Node n = new Node(root);
      petal_model.addNode(n);
      Map<String,Node> known = (call_tree ? null : new HashMap<String,Node>());
      if (known != null) {
	 known.put(root.getMethod(),n);
       }
      addChildren(n,root,known);

      if (petal_model.getNodes().length > 10000) {
	 petal_model.clear();
	 petal_model.addNode(n);
       }

      petal_model.fireModelUpdated();
      petal_editor.commandLayout(layout_method);
      Dimension d = petal_editor.getPreferredSize();
      setPreferredSize(d);
      setSize(d);
      setMinimumSize(d);
      repaint();
    }

   Node findNode(Point pt) {
      PetalNode pn = petal_editor.findNode(pt);
      if (pn != null) return (Node) pn;
      return null;
    }

   Arc findArc(Point pt) {
      PetalArc pa = petal_editor.findArc(pt);
      if (pa != null) return (Arc) pa;
      return null;
    }

   Node findNode(BicexEvaluationContext ctx) {
      if (ctx == null) return null;
      for (PetalNode pn : petal_model.getNodes()) {
	 Node n = (Node) pn;
	 if (n == null || n.getContext() == null) continue;
	 if (n.getContext() == ctx) return n;
	 if (n.getContext() == null) continue;
	 if (!call_tree && n.getContext().getMethod().equals(ctx.getMethod())) return n;
       }
      return null;
    }

   void setCurrent(Node n) {
      if (current_node == n) return;
      if (current_node != null) {
	 Color bkg = BoardColors.getColor(DEFAULT_BACKGROUND_PROP);
	 current_node.getComponent().setBackground(bkg);
       }
      current_node = n;
      if (current_node != null) {
	 Color bkg = BoardColors.getColor(CURRENT_BACKGROUND_PROP);
	 current_node.getComponent().setBackground(bkg);
	 scrollRectToVisible(current_node.getComponent().getBounds());
       }
    }

   private void addChildren(Node n,BicexEvaluationContext ctx,Map<String,Node> known) {
      if (ctx.getInnerContexts() != null) {
	 for (BicexEvaluationContext cctx : ctx.getInnerContexts()) {
	    Node n1 = null;
	    if (known != null) {
	       n1 = known.get(cctx.getMethod());
	     }
	    if (n1 == null) {
	       n1 = new Node(cctx);
	       petal_model.addNode(n1);
	       if (known != null) known.put(cctx.getMethod(),n1);
	     }
	    Arc a = null;
	    if (known != null) {
	       for (PetalArc pa : petal_model.getArcsFromNode(n)) {
		  if (pa.getTarget() == n1) {
		     a = (Arc) pa;
		     a.addInstance();
		     break;
		   }
		}
	     }
	    if (a == null) {
	       a = new Arc(n,n1);
	       petal_model.addArc(a);
	     }
	    addChildren(n1,cctx,known);
	  }
       }
    }

}	// end of inner class CallGraph




private class Model extends PetalModelDefault {

}	// end of inner class Model



private class Node extends PetalNodeDefault {

   private BicexEvaluationContext for_context;

   private static final long serialVersionUID = 1;

   Node(BicexEvaluationContext ctx) {
      super(ctx.getShortName());
      for_context = ctx;
      Color bkg = BoardColors.getColor(DEFAULT_BACKGROUND_PROP);
      getComponent().setBackground(bkg);
    }

   BicexEvaluationContext getContext()		{ return for_context; }

   @Override public String getToolTip(Point pt) {
      return for_context.getMethod();
    }

}	// end of inner class Node






private class Arc extends PetalArcDefault
{
   private int arc_weight;

   private static final long serialVersionUID = 1;


   Arc(Node f,Node t) {
      super(f,t);
      arc_weight = 1;
      setTargetEnd(new PetalArcEndDefault(PetalArcEnd.PETAL_ARC_END_ARROW));
    }

   void addInstance() {
      arc_weight++;
      float wt = (float)(1+Math.log(arc_weight)/Math.log(2));
      setStroke(new BasicStroke(wt));
    }


}	// end of inner class Arc

}	// end of class BicexCallGraphBubble




/* end of BicexCallGraphBubble.java */

