/********************************************************************************/
/*										*/
/*		BddtHistoryBubble.java						*/
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


import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.swing.SwingText;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class BddtHistoryBubble extends BudaBubble implements BddtConstants, BumpConstants, BudaConstants,
		BddtConstants.BddtHistoryListener
{




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BddtHistoryData history_data;
private BddtLaunchControl for_control;
private HistoryGraph	history_graph;
private long		last_update;
private boolean 	update_needed;
private boolean 	restart_needed;

private double		x_scale;
private double		y_scale;
private HistoryPanel	draw_area;
private JScrollPane	scroll_pane;

private Map<BumpThread,Color> thread_colors;
private Map<LinkType,Stroke> type_strokes;
private Stroke		arrow_stroke;

private static final long serialVersionUID = 1;

private static final float [] DASHED = new float [] { 8,2 };
private static final float [] DOTTED = new float [] { 2,2 };



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BddtHistoryBubble(BddtLaunchControl ctrl,BddtHistoryData hd)
{
   history_data = hd;
   for_control = ctrl;
   history_graph = null;
   last_update = 0;
   update_needed = true;
   restart_needed = true;
   x_scale = 1;
   y_scale = 1;
   thread_colors = new HashMap<BumpThread,Color>();
   type_strokes = new EnumMap<LinkType,Stroke>(LinkType.class);
   type_strokes.put(LinkType.ENTER,new BasicStroke(1,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,
						      1.0f,DOTTED,0));
   type_strokes.put(LinkType.RETURN,new BasicStroke(1,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,
						       1.0f,DASHED,0));
   type_strokes.put(LinkType.NEXT,new BasicStroke(1));
   arrow_stroke = new BasicStroke(1);

   updateGraph();

   hd.addHistoryListener(this);

   setupPanel();

   draw_area.addMouseListener(new FocusOnEntry());
}



@Override protected void localDispose()
{
   history_data.removeHistoryListener(this);
}



/********************************************************************************/
/*										*/
/*	Methods to build the initial history graph				*/
/*										*/
/********************************************************************************/

private void updateGraph()
{
   synchronized (this) {
      update_needed = false;
      if (restart_needed || history_graph == null) {
	 history_graph = new HistoryGraph();
	 last_update = 0;
	 restart_needed = false;
       }
    }

   synchronized (history_graph) {
      for (BumpThread bt : history_data.getThreads()) {
	 history_graph.addThreadItems(history_data.getItems(bt),last_update);
       }
      last_update = history_graph.finishBuild();
    }
}



/********************************************************************************/
/*										*/
/*	Methods to setup the display						*/
/*										*/
/********************************************************************************/

private void setupPanel()
{
   draw_area = new HistoryPanel();
   scroll_pane = new JScrollPane(draw_area);

   Dimension d = new Dimension(BDDT_HISTORY_WIDTH,BDDT_HISTORY_HEIGHT);
   draw_area.setPreferredSize(d);
   draw_area.setSize(d);

   setContentPane(scroll_pane);
}




/********************************************************************************/
/*										*/
/*	Handle user actions							*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   JPopupMenu popup = new JPopupMenu();
   Point pt = SwingUtilities.convertPoint(getContentPane().getParent(),e.getPoint(),draw_area);

   BddtHistoryItem itm = getItemAtPoint(pt.x,pt.y);
   if (itm != null) {
      popup.add(new GotoSourceAction(itm));
      popup.add(new GotoStackAction(itm));
    }
   else {
      GraphObject go = getObjectAtPoint(pt.x,pt.y);
      if (go != null && go.getValue() != null) {
	 popup.add(new GotoValueAction(go));
       }
    }

   popup.add(getFloatBubbleAction());

   popup.show(draw_area,pt.x,pt.y);
}



private class GotoSourceAction extends AbstractAction {

   private BddtHistoryItem for_item;
   private static final long serialVersionUID = 1;
   
   GotoSourceAction(BddtHistoryItem itm) {
      super("Show source");
      for_item = itm;
   }

   @Override public void actionPerformed(ActionEvent e) {
      BumpThreadStack stk = for_item.getStack();
      BumpStackFrame frame= stk.getFrame(0);
      BudaBubble bb = null;
      if (for_control.frameFileExists(frame)) {
	 String mid = frame.getMethod() + frame.getSignature();
	 bb = BaleFactory.getFactory().createMethodBubble(for_control.getProject(),mid);
       }
      else {
	 bb = new BddtLibraryBubble(frame);
       }

      if (bb != null) {
	 BoardMetrics.noteCommand("BDDT","HistorySource");
	 BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BddtHistoryBubble.this);
	 if (bba != null) {
	    bba.addBubble(bb,BddtHistoryBubble.this,null,
			     PLACEMENT_RIGHT|PLACEMENT_GROUPED|PLACEMENT_LOGICAL|PLACEMENT_MOVETO);
	  }
       }
   }

}	// end of inner class GotoSourceAction




private class GotoStackAction extends AbstractAction {

   private BumpThreadStack for_stack;
   private static final long serialVersionUID = 1;
   
   GotoStackAction(BddtHistoryItem itm) {
      super("Show stack");
      for_stack = itm.getStack();
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BDDT","HistoryStack");
      BddtStackView sv = new BddtStackView(for_control,for_stack);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BddtHistoryBubble.this);
      if (bba != null) {
	 bba.addBubble(sv,BddtHistoryBubble.this,null,
			  PLACEMENT_RIGHT|PLACEMENT_GROUPED|PLACEMENT_LOGICAL|PLACEMENT_MOVETO);
       }
    }

}	// end of inner class GotoStackAction




private class GotoValueAction extends AbstractAction {

   private BumpRunValue for_value;
   private static final long serialVersionUID = 1;
   
   GotoValueAction(GraphObject go) {
      super("Show this value");
      for_value = go.getValue();
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BDDT","HistoryValue");
      BddtStackView sv = new BddtStackView(for_control,for_value,false);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(BddtHistoryBubble.this);
      if (bba != null) {
	 bba.addBubble(sv,BddtHistoryBubble.this,null,
			  PLACEMENT_RIGHT|PLACEMENT_LOGICAL|PLACEMENT_GROUPED|PLACEMENT_MOVETO);
       }
    }

}	// end of inner class GotoValueAction




/********************************************************************************/
/*										*/
/*	History update callbacks						*/
/*										*/
/********************************************************************************/

@Override public void handleHistoryStarted()
{
   synchronized (this) {
      update_needed = true;
      restart_needed = true;
      thread_colors.clear();
    }

   draw_area.repaint();
}



@Override public void handleHistoryUpdated()
{
   synchronized (this) {
      update_needed = true;
    }

   draw_area.repaint();
}



/********************************************************************************/
/*										*/
/*	History graph representation						*/
/*										*/
/********************************************************************************/

private static class HistoryGraph {

   private List<GraphObject> graph_objects;
   private Map<String,GraphObject> object_map;
   private long last_time;
   private long start_time;

   HistoryGraph() {
      graph_objects = new ArrayList<>();
      object_map = new HashMap<>();
      last_time = 0;
      start_time = 0;
    }

   int getNumObjects()				{ return graph_objects.size(); }
   GraphObject getObject(int i) 		{ return graph_objects.get(i); }
   long getStartTime()				{ return start_time; }
   long getEndTime()				{ return last_time; }

   GraphObject getObject(BddtHistoryItem hi) {
      BumpRunValue rv = hi.getThisValue();
      GraphObject go = null;
      if (rv != null) {
	 String key = rv.getValue();
	 go = object_map.get(key);
	 if (go == null) {
	    go = new GraphObject(rv);
	    graph_objects.add(go);
	    object_map.put(key,go);
	  }
       }
      else {
	 String cnm = hi.getClassName();
	 go = object_map.get(cnm);
	 if (go == null) {
	    go = new GraphObject(cnm);
	    graph_objects.add(go);
	    object_map.put(cnm,go);
	  }
       }
      return go;
    }

   void addThreadItems(Iterable<BddtHistoryItem> itms,long since) {
      GraphObject lastobj = null;
      BddtHistoryItem lastitem = null;
      for (BddtHistoryItem hi : itms) {
	 GraphObject go = getObject(hi);
	 if (since == 0 || hi.getTime() > since) {
	    last_time = Math.max(last_time,hi.getTime());
	    if (start_time == 0) start_time = last_time;
	    else start_time = Math.min(start_time,hi.getTime());

	    if (go == null) continue;
	    if (lastobj == null) {		   // first time
	       go.startBlock(hi);
	     }
	    else if (lastobj == go) {		   // step inside the same object
	       go.extendBlock(hi);
	     }
	    else if (lastitem != null && hi.isInside(lastitem)) {	   // step/call into a new object
	       go.startBlock(hi);
	       lastobj.addLink(go,LinkType.ENTER,hi);
	     }
	    else if (lastitem != null && lastitem.isInside(hi)) {	   // return to prior object
	       go.extendBlock(hi);
	       // end prior block??
	       lastobj.addLink(go,LinkType.RETURN,hi);
	     }
	    else {
	       lastobj.finish(hi);
	       go.startBlock(hi);
	       lastobj.addLink(go,LinkType.NEXT,hi);
	     }
	  }
	 lastobj = go;
	 lastitem = hi;
       }
    }

   long finishBuild() {
      Collections.sort(graph_objects,new HistoryCompare());
      return last_time;
    }

}	// end of inner class HistoryGraph



private static class GraphObject {

   private BumpRunValue this_value;
   private String class_name;
   private Map<BumpThread,GraphBlock> in_blocks;
   private List<GraphBlock> all_blocks;
   private List<GraphLink> out_links;
   private long start_time;

   GraphObject(BumpRunValue rv) {
      this_value = rv;
      class_name = rv.getType();
      initialize();
    }

   GraphObject(String cnm) {
      this_value = null;
      class_name = cnm;
      initialize();
    }

   private void initialize() {
      in_blocks = new HashMap<>();
      all_blocks = new ArrayList<>();
      out_links = new ArrayList<>();
      start_time = 0;
    }

   long getStartTime()				{ return start_time; }
   String getName() {
      if (this_value == null) return class_name;
      return class_name + " " + this_value.getValue();
    }
   List<GraphBlock> getBlocks() 		{ return all_blocks; }
   List<GraphLink> getLinks()			{ return out_links; }
   BumpRunValue getValue()			{ return this_value; }

   void startBlock(BddtHistoryItem hi) {
      if (start_time == 0) start_time = hi.getTime();
      else start_time = Math.min(start_time,hi.getTime());
      GraphBlock gb = new GraphBlock(hi);
      all_blocks.add(gb);
      in_blocks.put(hi.getThread(),gb);
    }

   void extendBlock(BddtHistoryItem hi) {
      GraphBlock gb = in_blocks.get(hi.getThread());
      if (gb == null) startBlock(hi);
      else gb.addItem(hi);
    }

   void finish(BddtHistoryItem hi) {
      GraphBlock gb = in_blocks.get(hi.getThread());
      if (gb != null) {
	 gb.finish(hi.getTime());
	 in_blocks.remove(hi.getThread());
      }
    }

   void addLink(GraphObject to,LinkType typ,BddtHistoryItem hi) {
      GraphLink lnk = new GraphLink(to,typ,hi);
      out_links.add(lnk);
    }

   long getReturnTime(GraphObject to) {
      for (GraphLink lnk : out_links) {
	 if (lnk.getType() == LinkType.RETURN) {
	    GraphObject go = lnk.getToObject();
	    if (go == to) return lnk.getTime();
	    else {
	       long rt = go.getReturnTime(to);
	       if (rt != 0) return Math.min(lnk.getTime(),rt);
	     }
	  }
       }
      return 0;
    }

}	// end of inner class GraphObject



private static class GraphBlock {

   private List<BddtHistoryItem> for_items;
   private long start_time;
   private long last_time;
   private long end_time;

   GraphBlock(BddtHistoryItem hi) {
      start_time = hi.getTime();
      last_time = start_time;
      end_time = 0;
      for_items = new ArrayList<BddtHistoryItem>();
      for_items.add(hi);
    }

   long getStartTime()				{ return start_time; }
   long getEndTime() {
      if (end_time != 0) return end_time;
      return last_time;
    }
   List<BddtHistoryItem> getItems()		{ return for_items; }

   BumpThread getThread() {
      if (for_items.isEmpty()) return null;
      BddtHistoryItem bi = for_items.get(0);
      return bi.getThread();
    }

   void addItem(BddtHistoryItem hi) {
      last_time = hi.getTime();
      for_items.add(hi);
    }

   void finish(long when) {
      end_time = when;
    }

}	// end of inner class GraphBlock



private static class GraphLink {

   private GraphObject to_object;
   private LinkType link_type;
   private BddtHistoryItem for_item;

   GraphLink(GraphObject to,LinkType typ,BddtHistoryItem hi) {
      to_object = to;
      link_type = typ;
      for_item = hi;
    }

   LinkType getType()			{ return link_type; }
   GraphObject getToObject()		{ return to_object; }
   long getTime()			{ return for_item.getTime(); }
   BumpThread getThread()		{ return for_item.getThread(); }
   BddtHistoryItem getItem()		{ return for_item; }

}	// end of inner class GraphLink




/********************************************************************************/
/*										*/
/*	Graph sorter for history objects					*/
/*										*/
/********************************************************************************/

private static final class HistoryCompare implements Comparator<GraphObject>
{

   @Override public int compare(GraphObject t0,GraphObject t1) {
      long x0 = t1.getReturnTime(t0);
      long x1 = t0.getReturnTime(t1);
      if (x0 == x1) {			// either same or == 0
	 long d = t0.getStartTime() - t1.getStartTime();
	 if (d < 0) return -1;
	 else if (d > 0) return 1;
	 else if (t0.getName() == null) return -1;
	 else if (t1.getName() == null) return 1;
	 else return t0.getName().compareTo(t1.getName());
       }
      else if (x0 == 0) return -1;
      else if (x1 == 0) return 1;
      else if (x0 < x1) return 1;
      else return -1;
    }

}	// end of inner class HistoryCompare




/********************************************************************************/
/*										*/
/*	Drawing methods 							*/
/*										*/
/********************************************************************************/

private double		left_right_margin;
private double		object_width = 0;
private double		object_hspacing;
private double		active_width;
private double		top_bottom_margin;
private double		object_height;
private double		time_start;
private double		time_end;
private double		arrow_size;
private Map<GraphObject,Double> graph_locations;


private void handlePaint(Graphics2D g)
{
   checkSizes();
   Dimension csz = draw_area.getSize();

   synchronized (this) {
      if (update_needed) updateGraph();
    }

   synchronized (history_graph) {
      int nobj = history_graph.getNumObjects();
      double twid = nobj * GRAPH_OBJECT_WIDTH + 2 * GRAPH_LEFT_RIGHT_MARGIN +
	 (nobj - 1) * GRAPH_OBJECT_H_SPACE;
      double sx = csz.width / twid;
      left_right_margin = GRAPH_LEFT_RIGHT_MARGIN * sx;
      object_width = GRAPH_OBJECT_WIDTH * sx;
      object_hspacing = GRAPH_OBJECT_H_SPACE * sx;
      active_width = GRAPH_ACTIVE_WIDTH * sx;
      arrow_size = 3 / y_scale;

      double tht = 2 * GRAPH_TOP_BOTTOM_MARGIN + GRAPH_OBJECT_V_SPACE + GRAPH_TIME_SPACE;
      double sy = csz.height /tht;
      top_bottom_margin = GRAPH_TOP_BOTTOM_MARGIN * sy;
      object_height = GRAPH_OBJECT_HEIGHT * sy;
      time_start = top_bottom_margin + object_height + GRAPH_OBJECT_V_SPACE * sy;
      time_end = csz.height - top_bottom_margin;

      graph_locations = new HashMap<GraphObject,Double>();

      for (int i = 0; i < nobj; ++i) {
	 GraphObject go = history_graph.getObject(i);
	 drawObject(g,i,go);
       }

      for (int i = 0; i < nobj; ++i) {
	 GraphObject go = history_graph.getObject(i);
	 drawLinks(g,go);
       }
    }
}



private void drawObject(Graphics2D g,int idx,GraphObject go)
{
   double x0 = left_right_margin + (object_width + object_hspacing) * idx;
   Rectangle2D r = new Rectangle2D.Double(x0,top_bottom_margin,object_width,object_height);
   graph_locations.put(go,x0 + object_width/2);

   g.setColor(BoardColors.getColor("Bddt.HistoryBackground")); 
   g.fill(r);
   g.setColor(BoardColors.getColor("Bddt.HistoryBorder"));
   g.draw(r);
   g.setColor(BoardColors.getColor("Bddt.HistoryText")); 
   SwingText.drawText(go.getName(),g,r);

   double x1 = x0 + object_width/2;
   double y1 = top_bottom_margin + object_height;
   double y2 = time_end;
   Line2D tl = new Line2D.Double(x1,y1,x1,y2);
   g.setColor(BoardColors.getColor("Bddt.HistoryLine"));
   g.draw(tl);

   for (GraphBlock gb : go.getBlocks()) {
      drawBlock(g,x1,gb);
    }
}


private void drawBlock(Graphics2D g,double x,GraphBlock gb)
{
   double y0 = getTimeY(gb.getStartTime());
   double y1 = getTimeY(gb.getEndTime());

   double x0 = x - active_width/2;
   Color c = getThreadBlockColor(gb.getThread());
   Rectangle2D r = new Rectangle2D.Double(x0,y0,active_width,y1-y0);
   g.setColor(c);
   g.fill(r);
}




private void drawLinks(Graphics2D g,GraphObject go)
{
   double x0 = graph_locations.get(go);

   for (GraphLink gl : go.getLinks()) {
      GraphObject got = gl.getToObject();
      double x1 = graph_locations.get(got);
      double y0 = getTimeY(gl.getTime());
      double x3;
      double x4;
      if (x0 < x1) {
	 x3 = x0 + active_width/2;
	 x4 = x1 - active_width/2;
       }
      else {
	 x3 = x0 - active_width/2;
	 x4 = x1 + active_width/2;
       }
      Stroke sk = type_strokes.get(gl.getType());
      Color c = getThreadBlockColor(gl.getThread());
      g.setColor(c);
      g.setStroke(sk);
      Line2D ln = new Line2D.Double(x3,y0,x4,y0);
      g.draw(ln);
      drawArrow(g,x3,y0,x4,y0);
    }
}



private double getTimeY(long t0)
{
   double y0 = t0 - history_graph.getStartTime();
   y0 = y0/(history_graph.getEndTime() - history_graph.getStartTime() + 1) *
      (time_end - time_start) + time_start;
   return y0;
}


private Color getThreadBlockColor(BumpThread bt)
{
   if (bt == null) return BoardColors.getColor("Bddt.HistoryNoThread");

   synchronized (thread_colors) {
      Color c = thread_colors.get(bt);
      if (c == null) {
	 double v;
	 int ct = thread_colors.size();
	 if (ct == 0) v = 0;
	 else if (ct == 1) v = 1;
	 else {
	    v = 0.5;
	    int p0 = ct-1;
	    int p1 = 1;
	    for (int p = p0; p > 1; p /= 2) {
	       v /= 2.0;
	       p0 -= p1;
	       p1 *= 2;
	     }
	    if ((p0 & 1) == 0) p0 = 2*p1 - p0 + 1;
	    v = v * p0;
	  }
	 float h = (float) (v * 0.8);
	 float s = 0.7f;
	 float b = 1.0f;
	 int rgb = Color.HSBtoRGB(h,s,b);
	 rgb |= 0xc0000000;
	 c = new Color(rgb,true);
	 thread_colors.put(bt,c);
       }
      return c;
    }
}



private void checkSizes()
{
   Dimension vsz = scroll_pane.getViewport().getViewSize();
   Dimension csz = draw_area.getSize();
   Dimension nsz = new Dimension((int) (vsz.width * x_scale),(int) (vsz.height * y_scale));
   boolean chng = false;

   if (nsz.width > csz.width) {
      if (vsz.width >= csz.width) {
	 csz.width = vsz.width;
	 chng = true;
	 x_scale = 1;
       }
    }
   else if (nsz.width < csz.width) {
      if (x_scale == 1) {
	 csz.width = vsz.width;
	 chng = true;
       }
    }

   if (nsz.height > csz.height) {
      if (vsz.height >= csz.height) {
	 csz.height = vsz.height;
	 chng = true;
	 y_scale = 1;
       }
    }
   else if (nsz.height < csz.height) {
      if (y_scale == 1) {
	 csz.height = vsz.height;
	 chng = true;
       }
    }

   if (chng) draw_area.setSize(csz);
}



private void drawArrow(Graphics2D g,double fx,double fy,double tx,double ty)
{
   double d = Point2D.distance(fx,fy,tx,ty);

   if (d == 0) return;

   double t = arrow_size/d;
   double cx0 = tx + 2*t*(fx - tx);
   double cy0 = ty + 2*t*(fy - ty);
   double cx1 = cx0 + t*(fy - ty);
   double cy1 = cy0 - t*(fx - tx);
   double cx2 = cx0 - t*(fy - ty);
   double cy2 = cy0 + t*(fx - tx);

   g.setStroke(arrow_stroke);
   Line2D.Double l2 = new Line2D.Double(cx1,cy1,tx,ty);
   g.draw(l2);
   l2.setLine(cx2,cy2,tx,ty);
   g.draw(l2);
}



/********************************************************************************/
/*										*/
/*	Correlation methods							*/
/*										*/
/********************************************************************************/

private BddtHistoryItem getItemAtPoint(int x,int y)
{
   if (object_width == 0) return null;

   double t = history_graph.getStartTime() + (y - time_start)/(time_end - time_start) *
      (history_graph.getEndTime() - history_graph.getStartTime() + 1);
   if (t < history_graph.getStartTime() || t > history_graph.getEndTime()) return null;

   GraphObject gobj = getObjectAtPoint(x,y);

   // correlate with blocks
   if (gobj != null) {
      GraphBlock gb0 = null;
      double dtime = 0;
      for (GraphBlock gb : gobj.getBlocks()) {
	 double dt = gb.getEndTime() - gb.getStartTime();
	 if (t >= gb.getStartTime() && t <= gb.getEndTime()) {
	    if (gb0 == null || dt < dtime) {
	       gb0 = gb;
	       dtime = dt;
	     }
	  }
       }
      if (gb0 != null) {
	 BddtHistoryItem bi0 = null;
	 dtime = 0;
	 for (BddtHistoryItem bhi : gb0.getItems()) {
	    double dt = t - bhi.getTime();
	    if (dt >= 0) {
	       if (bi0 == null || dt < dtime) {
		  bi0 = bhi;
		  dtime = dt;
		}
	     }
	  }
	 if (bi0 != null) return bi0;
       }
    }

   // correlate with links
   double delta = (history_graph.getEndTime() - history_graph.getStartTime()) / (time_end - time_start) * 3;

   double dtime = 0;
   GraphLink gl0 = null;
   for (int i = 0; i < history_graph.getNumObjects(); ++i) {
      GraphObject go = history_graph.getObject(i);
      double x0 = graph_locations.get(go);
      for (GraphLink gl : go.getLinks()) {
	 double x1 = graph_locations.get(gl.getToObject());
	 if (x > x0 && x < x1) {
	    double dt = Math.abs(t - gl.getTime());
	    if (gl0 == null || dt < dtime) {
	       gl0 = gl;
	       dtime = dt;
	     }
	  }
       }
    }
   if (gl0 != null && dtime <= delta) {
      return gl0.getItem();
    }

   return null;
}



private GraphObject getObjectAtPoint(int x,int y)
{
   if (object_width == 0) return null;

   GraphObject gobj = null;

   int idx = (int) ((x - left_right_margin)/(object_width + object_hspacing));
   if (idx >= 0 && idx < history_graph.getNumObjects()) {
      double xoff = x - (left_right_margin + idx*(object_width + object_hspacing));
      xoff -= object_width/2;
      if (Math.abs(xoff) <= active_width) gobj = history_graph.getObject(idx);
    }

   return gobj;
}



/********************************************************************************/
/*										*/
/*	Panel for drawing history						*/
/*										*/
/********************************************************************************/

private class HistoryPanel extends JComponent {
   
   private static final long serialVersionUID = 1;
   
   HistoryPanel() {
      setToolTipText("History Panel");
    }

   @Override protected void paintComponent(Graphics g) {
      handlePaint((Graphics2D) g);
    }

   @Override public String getToolTipText(MouseEvent e) {
      BddtHistoryItem itm = getItemAtPoint(e.getX(),e.getY());
      if (itm != null) {
	 StringBuffer buf = new StringBuffer();
	 BumpThreadStack stk = itm.getStack();
	 BumpStackFrame frm = stk.getFrame(0);
	 if (frm == null) return null;
	 buf.append(frm.getMethod() + " at " + frm.getLineNumber());
	 return buf.toString();
      }
      GraphObject go = getObjectAtPoint(e.getX(),e.getY());
      if (go != null) {
	 return go.getName();
      }
      return null;
    }

}	// end of inner class HistoryPanel




}	// end of class BddtHistoryBubble




/* end of BddtHistoryBubble.java */
