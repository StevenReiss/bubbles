/********************************************************************************/
/*										*/
/*		BicexLineGraphBubble.java					*/
/*										*/
/*	Generate a control-flow graph showing lines executed			*/
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.ivy.petal.PetalArc;
import edu.brown.cs.ivy.petal.PetalArcDefault;
import edu.brown.cs.ivy.petal.PetalArcEnd;
import edu.brown.cs.ivy.petal.PetalArcEndDefault;
import edu.brown.cs.ivy.petal.PetalEditor;
import edu.brown.cs.ivy.petal.PetalLayoutMethod;
import edu.brown.cs.ivy.petal.PetalLevelLayout;
import edu.brown.cs.ivy.petal.PetalModelDefault;
import edu.brown.cs.ivy.petal.PetalNode;
import edu.brown.cs.ivy.petal.PetalNodeDefault;
import edu.brown.cs.ivy.petal.PetalUndoSupport;
import edu.brown.cs.ivy.petal.PetalConstants.PetalNodeShape;
import edu.brown.cs.ivy.petal.PetalCircleLayout;


class BicexLineGraphPanel extends BicexPanel implements BicexConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private LineFlowGraph		line_graph;

private static String DEFAULT_COLOR_PROP = "Bicex.LineGraphDefault";
private static String CURRENT_COLOR_PROP = "Bicex.LineGraphCurrent";





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BicexLineGraphPanel(BicexEvaluationViewer ev)
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
   line_graph = new LineFlowGraph();

   return line_graph;
}



/********************************************************************************/
/*										*/
/*	Interaction methods							*/
/*										*/
/********************************************************************************/

@Override void handlePopupMenu(JPopupMenu menu,MouseEvent evt)
{
   Node n = line_graph.findNode(evt.getPoint());
   Arc a = line_graph.findArc(evt.getPoint());
   if (n == null && a == null) return;

   if (n == null && a != null) n = (Node) a.getTarget();
   if (n == null) return;

   int lno = n.getLine();
   BicexValue bv = getRootContext().getValues().get("*LINE*");
   List<Integer> times = bv.getTimeChanges();
   long now = getExecution().getCurrentTime();
   long prev = -1;
   long next = -1;
   long first = -1;
   long last = -1;
   for (Integer t : times) {
      String xv = bv.getStringValue(t+1);
      int flin = Integer.parseInt(xv);
      if (flin == 0 || flin != lno) continue;
      if (t < now) {
	 if (first < 0 && prev > 0) first = prev;
	 prev = t;
       }
      else if (t > now && next < 0) next = t;
      else if (t > now) last = t;
    }
   if (first > 0) menu.add(getTimeAction("Go To First " + lno,first+1));
   if (prev > 0) menu.add(getTimeAction("Go To Previous " + lno,prev+1));
   if (next > 0) menu.add(getTimeAction("Go To Next " + lno,next+1));
   if (last > 0) menu.add(getTimeAction("Go To Final " + lno,next+1));
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override boolean allowMouseWheelScrolling()		{ return false; }



/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

@Override void update()
{
   line_graph.update();

   updateTime();
}



@Override void updateTime()
{
   if (getContext() == null) return;

   BicexValue bv = getContext().getValues().get("*LINE*");
   if (bv == null) return;
   int lno = -1;
   try {
      lno = Integer.parseInt(bv.getStringValue(getExecution().getCurrentTime()));
    }
   catch (NumberFormatException e) { }

   Node n = line_graph.findNode(lno);
   line_graph.setCurrentNode(n);
}



/********************************************************************************/
/*										*/
/*	Line graph display							*/
/*										*/
/********************************************************************************/

private class LineFlowGraph extends JPanel {

   private PetalEditor		petal_editor;
   private PetalModelDefault	petal_model;
   private PetalLayoutMethod	layout_method;
   private Node 		current_node;

   private static final long serialVersionUID = 1;
   private static final boolean alt_layout = false;


   LineFlowGraph() {
      super(new BorderLayout());
      setPreferredSize(new Dimension(300,300));
      PetalUndoSupport.getSupport().blockCommands();
      petal_model = new Model();
      petal_editor = new PetalEditor(petal_model);
      PetalLevelLayout levels = new PetalLevelLayout(petal_editor);
      levels.setSplineArcs(false);
      levels.setLevelX(true);
      levels.setOptimizeLevels(true);
      layout_method = levels;
      PetalLayoutMethod alt = new PetalCircleLayout(petal_editor);
      if (alt_layout) layout_method = alt;
      add(petal_editor,BorderLayout.CENTER);
      petal_editor.addZoomWheeler();
      current_node = null;
   }

   void update() {
      Map<Integer,Node> nodes = new HashMap<Integer,Node>();
      Map<Long,Arc> arcs = new HashMap<Long,Arc>();
      BicexValue bv = getRootContext().getValues().get("*LINE*");
   
      petal_model.clear();
   
      if (bv != null) {
         List<Integer> times = bv.getTimeChanges();
        
         Map<Integer,Set<Integer>> linemap = new HashMap<>();
         Integer pline = null;
         int ctr = 0;
         for (Integer t : times) {
            String xv = bv.getStringValue(t+1);
            int line = Integer.parseInt(xv);
            if (line == 0) continue;
            if (pline != null) {
               if (pline == line && ctr <= 1) continue;
               Set<Integer> sval = linemap.get(pline);
               if (sval == null) {
        	  sval = new HashSet<>();
        	  linemap.put(pline,sval);
        	}
               sval.add(line);
             }
            pline = line;
            ++ctr;
          }
        
         Node prior = null;
         ctr = 0;
         for (Integer t : times) {
            String xv = bv.getStringValue(t+1);
            int line = Integer.parseInt(xv);
            if (line == 0) continue;
            Node next = nodes.get(line);
            if (next == null) {
               if (ctr == 0) next = new Node(line,PetalNodeShape.TRIANGLE_DOWN);
               else {
        	  Set<Integer> lset = linemap.get(line);
        	  if (lset == null) next = new Node(line,PetalNodeShape.TRIANGLE);
        	  else if (lset.size() > 1) next = new Node(line,PetalNodeShape.DIAMOND);
        	  else next = new Node(line,PetalNodeShape.RECTANGLE);
        	}
               nodes.put(line,next);
               petal_model.addNode(next);
            }
            if (prior == next && ctr <= 1) continue;
            if (prior != null) {
               long aid = prior.getLine()*1000000 + line;
               Arc a = arcs.get(aid);
               if (a == null) {
        	  a = new Arc(prior,next);
        	  Set<Integer> lset = linemap.get(line);
        	  if (lset != null && lset.contains(prior.getLine())) {
        	     a.setSplineArc(true);
        	  }
        	  arcs.put(aid,a);
        	  petal_model.addArc(a);
               }
               else a.addInstance();
            }
            prior = next;
            ++ctr;
          }
       }
   
      petal_model.fireModelUpdated();
      petal_editor.commandLayout(layout_method);
      Dimension dim = petal_editor.getPreferredSize();
      // dim.height += 8;
      setSize(dim);
      setPreferredSize(dim);
      setMinimumSize(dim);
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

   Node findNode(int ln) {
      for (PetalNode pn : petal_model.getNodes()) {
	 Node n = (Node) pn;
	 if (n.getLine() == ln) return n;
       }
      return null;
    }

   void setCurrentNode(Node n) {
      if (n == current_node) return;
      if (current_node != null) {
         Color bkg = BoardColors.getColor(DEFAULT_COLOR_PROP);
	 current_node.getComponent().setBackground(bkg);
       }
      current_node = n;
      if (current_node != null) {
         Color bkg = BoardColors.getColor(CURRENT_COLOR_PROP);
	 current_node.getComponent().setBackground(bkg);
	 scrollRectToVisible(current_node.getComponent().getBounds());
       }
    }

}	// end of inner class LineFlowGraph




private class Model extends PetalModelDefault {

}	// end of inner class Model



private class Node extends PetalNodeDefault {

   private int line_number;

   private static final long serialVersionUID = 1;

   Node(int line,PetalNodeShape shape) {
      super(shape,Integer.toString(line));
      line_number = line;
      Color bkg = BoardColors.getColor(DEFAULT_COLOR_PROP);
      getComponent().setBackground(bkg);
    }

   int getLine()		{ return line_number; }

   @Override public String getToolTip(Point pt) {
      BicexEvaluationContext ctx = getContext();
      return line_number + " in " + ctx.getMethod();
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




}	// end of class BicexLineGraphBubble




/* end of BicexLineGraphBubble.java */

