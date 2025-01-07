/********************************************************************************/
/*										*/
/*		BudaOverviewBar.java						*/
/*										*/
/*	BUblles Display Area overview bar					*/
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



package edu.brown.cs.bubbles.buda;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.buda.BudaConstants.BudaHelpClient;

import javax.swing.JPanel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


class BudaOverviewBar extends JPanel implements BudaConstants,
	BudaConstants.BubbleAreaCallback, BudaHelpClient
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BudaRoot	for_root;
private BudaBubbleArea	bubble_area;
private BudaViewport	view_area;
private RoundRectangle2D.Double view_position;
private MouseContext	mouse_context;
private boolean 	fake_working_set;

private static final long serialVersionUID = 1L;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BudaOverviewBar(BudaBubbleArea ba,BudaViewport view,BudaRoot root)
{
   bubble_area = ba;
   view_area = view;
   view_position = null;
   for_root = root;
   fake_working_set = false;

   ba.addBubbleAreaCallback(this);

   Dimension sz = new Dimension(0,BUBBLE_OVERVIEW_HEIGHT);
   setMinimumSize(sz);
   setPreferredSize(sz);
   BudaRoot.registerHelp(this,this);

   setLayout(new FlowLayout(FlowLayout.TRAILING));
   mouse_context = null;
   Mouser mm = new Mouser();
   addMouseListener(mm);
   addMouseMotionListener(mm);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BudaBubbleArea getBubbleArea()			{ return bubble_area; }


Rectangle getViewPosition()
{
   if (view_position == null) return null;

   return new Rectangle((int) view_position.x, (int) view_position.y,
         (int) view_position.width, (int) view_position.height);
}



/********************************************************************************/
/*										*/
/*	Handle changing current position					*/
/*										*/
/********************************************************************************/

void setViewPosition(Rectangle r)
{
   if (view_position != null &&
	  r.x == (int) view_position.getX() && r.y == (int) view_position.getY() &&
	  r.width == (int) view_position.getWidth() && r.height == (int) view_position.getHeight())
      return;
   
   if (BUDA_PROPERTIES.getBoolean(OVERVIEW_STYLIZED_VIEW_BOOL)) {
      view_position = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height,
            OVERVIEW_VIEW_ARCWIDTH, OVERVIEW_VIEW_ARCHEIGHT);
    }
   else {
      view_position = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 0, 0);
    }
   
   for_root.repaint();
}



/********************************************************************************/
/*										*/
/*	Tool Tip Methods							*/
/*										*/
/********************************************************************************/

@Override public String getHelpLabel(MouseEvent e)
{
   if (!BudaRoot.showHelpTips()) return null;

   return "overviewarea";
}



@Override public String getHelpText(MouseEvent e)
{
   return null;
}



/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

@Override protected void paintComponent(Graphics g0)
{
   Graphics2D g = (Graphics2D) g0.create();

   Dimension sz = getSize();
   Dimension fsz = bubble_area.getSize();
   double scalex = sz.getWidth() / fsz.getWidth();
   double scaley = sz.getHeight() / fsz.getHeight();

   Color tc = BoardColors.getColor(OVERVIEW_TOP_COLOR_PROP);
   Color bc= BoardColors.getColor(OVERVIEW_BOTTOM_COLOR_PROP);
   Paint p = new GradientPaint(0f, 0f, tc, 0f, BUBBLE_OVERVIEW_HEIGHT, bc);

   if (g != null) {
      g.setPaint(p);

      g.fillRect(0,0,sz.width,sz.height);

      g.scale(scalex,scaley);

      bubble_area.paintOverview(g);
      //changed by ian strickman
      if (view_position != null) {
	 if (BUDA_PROPERTIES.getBoolean(OVERVIEW_STYLIZED_VIEW_BOOL)){
	    Color c = BoardColors.getColor(OVERVIEW_STYLIZED_VIEW_BORDER_COLOR_PROP);
	    g.setColor(c);
	    g.setStroke(new BasicStroke(100));
	    setViewPosition(new Rectangle((int) view_position.x, (int) view_position.y,
		  (int) view_position.width, (int) view_position.height));
	    g.draw(view_position);
	 }
	 else {
	    Color c = BoardColors.getColor(OVERVIEW_NONSTYLIZED_VIEW_BORDER_COLOR_PROP);
	    g.setColor(c);
	    setViewPosition(new Rectangle((int) view_position.x,
		  (int) view_position.y, (int) view_position.width, (int) view_position.height));
	    g.draw(view_position);
	 }
       }
      if (fake_working_set) drawFakeWorkingSet(g);
    }
}



private void drawFakeWorkingSet(Graphics2D g)
{
   Dimension sz = bubble_area.getSize();
   Point loc = getLocation();
   Color ct = BoardColors.getColor(OVERVIEW_FAKE_TOP_COLOR_PROP);
   Color cb = BoardColors.getColor(OVERVIEW_FAKE_BOTTOM_COLOR_PROP);
   Paint p = new GradientPaint((int) view_position.x, loc.y, ct,
	 (int) (view_position.x + view_position.width), loc.y + sz.height, cb);
   g.setPaint(p);
   g.fillRect((int) view_position.x, loc.y, (int) view_position.width, sz.height);
}




void setFakeWorkingSet(boolean b)
{
   if (fake_working_set == b) return;

   fake_working_set = b;
   repaint();
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(BudaXmlWriter xw)
{
   xw.begin("OVERVIEW");
   xw.element("SHAPE",getBounds());
   xw.end("OVERVIEW");
}



/********************************************************************************/
/*										*/
/*	Area update calls							*/
/*										*/
/********************************************************************************/

@Override public void updateOverview()
{
   repaint();
}


@Override public void moveDelta(int dx,int dy)				{ }



/********************************************************************************/
/*										*/
/*	Methods to handle dragging the display area				*/
/*										*/
/********************************************************************************/

private void handleMouseEvent(MouseEvent e)
{
   for_root.setCurrentChannel(bubble_area);

   RoundRectangle2D.Double r = view_position;
   Dimension sz = getSize();
   Dimension fsz = bubble_area.getSize();

   double sx = sz.getWidth() / fsz.getWidth();
   double sy = sz.getHeight() / fsz.getHeight();
   double x0 = e.getX() / sx;
   double y0 = e.getY() / sy;

   Collection<BudaBubble> bbls = bubble_area.getBubblesInRegion(new Rectangle((int) x0,(int) y0,1,1));
   BudaBubble bb = null;
   BudaBubbleGroup bg = null;
   if (bbls.size() > 0) {
      for (BudaBubble bbx : bbls) {
	 if (bb == null || bb.isFixed()) bb = bbx;
       }
      for (BudaBubbleGroup bgx : bubble_area.getBubbleGroups()) {
	 if (bgx.getBubbles().contains(bb)) bg = bgx;
      }
   }

   if (e.getID() == MouseEvent.MOUSE_PRESSED && e.getButton() == MouseEvent.BUTTON3 &&
	    bb != null) {
      mouse_context = new BubbleMoveContext(e,bb,bg);
    }
   else if (r.contains((int) x0,(int) y0) && e.getID() == MouseEvent.MOUSE_PRESSED &&
	  e.getButton() == MouseEvent.BUTTON3) {
      mouse_context = new PanelMoveContext(e);
    }
   else if (e.getID() == MouseEvent.MOUSE_PRESSED && e.getButton() == MouseEvent.BUTTON1) {
      double x1 = x0 - r.width/2;
      double y1 = y0 - r.height/2;
      if (x1 < 0) x1 = 0;
      if (y1 < 0) y1 = 0;
      BoardMetrics.noteCommand("BUDA","overviewSet");
      setViewport((int) x1,(int) y1);
      mouse_context = new PanelMoveContext(e);
    }
}




private abstract class MouseContext {

   protected Point initial_mouse;

   MouseContext(MouseEvent e) {
      initial_mouse = e.getPoint();
    }

   void finish() {
      BudaCursorManager.resetDefaults(BudaOverviewBar.this);
    }

   abstract void next(MouseEvent e);

}	// end of inner class MouseContext



private class PanelMoveContext extends MouseContext {

   private Point initial_position;
   private double scale_x;
   private double scale_y;
   private Dimension area_bounds;
   private int move_count;

   PanelMoveContext(MouseEvent e) {
      super(e);
      initial_position = new Point((int) view_position.x, (int) view_position.y);
      Dimension sz = getSize();
      area_bounds = bubble_area.getSize();
      scale_x = sz.getWidth() / area_bounds.getWidth();
      scale_y = sz.getHeight() / area_bounds.getHeight();
      move_count = 0;
    }

   @Override void next(MouseEvent e) {
      ++move_count;
      BudaCursorManager.setGlobalCursorForComponent(BudaOverviewBar.this, 
            Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
      Point p0 = e.getPoint();
      double x0 = initial_position.x + (p0.x - initial_mouse.x)/scale_x;
      double y0 = initial_position.y + (p0.y - initial_mouse.y)/scale_y;
      if (x0 < 0) x0 = 0;
      if (x0 > area_bounds.width - view_position.width) x0 = area_bounds.width - view_position.width;
      if (y0 < 0) y0 = 0;
      if (y0 > area_bounds.height - view_position.height) y0 = area_bounds.height - view_position.height;

      setViewport((int) x0,(int) y0);
    }

   @Override void finish() {
      super.finish();
      if (move_count > 0) BoardMetrics.noteCommand("BUDA","overviewMove");
    }

}	// end of inner class PanelMoveContext




private class BubbleMoveContext extends MouseContext {

   private double scale_x;
   private double scale_y;
   private int move_count;
   private Map<BudaBubble,Point> initial_position;

   BubbleMoveContext(MouseEvent e,BudaBubble bb,BudaBubbleGroup bg) {
      super(e);
      Dimension sz = getSize();
      Dimension bounds = bubble_area.getSize();
      scale_x = sz.getWidth() / bounds.getWidth();
      scale_y = sz.getHeight() / bounds.getHeight();
      initial_position = new HashMap<>();
      if (bg != null) {
	 for (BudaBubble bbx : bg.getBubbles()) addInitialPosition(bbx);
       }
      else if (bb != null) addInitialPosition(bb);
      move_count = 0;
    }

   @Override void next(MouseEvent e) {
      ++move_count;
      BudaCursorManager.setGlobalCursorForComponent(BudaOverviewBar.this, 
            Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
      Point p0 = e.getPoint();
      int x1 = p0.x;
      int y1 = p0.y;
      if (x1 < 0) x1 = 0;
      x1 = Math.min(x1,BudaOverviewBar.this.getWidth());
      if (y1 < 0) y1 = 0;
      y1 = Math.min(y1,BudaOverviewBar.this.getHeight());

      for (Map.Entry<BudaBubble,Point> ent : initial_position.entrySet()) {
	 BudaBubble bb = ent.getKey();
	 Point ip = ent.getValue();
	 double x0 = ip.x + (x1 - initial_mouse.x)/scale_x;
	 double y0 = ip.y + (y1 - initial_mouse.y)/scale_y;
	 bb.setLocation(new Point((int) x0,(int) y0));
       }
    }

   @Override void finish() {
      super.finish();
      if (move_count > 0) {
         BudaBubble sbb = null;
         for (BudaBubble bb : initial_position.keySet()) {
            sbb = bb;
            break;
          }
         if (sbb != null) {
            if (initial_position.size() > 1) bubble_area.fixupBubbleGroup(sbb);
            else bubble_area.fixupBubble(sbb);
          }
         BoardMetrics.noteCommand("BUDA","overviewMove");
       }
    }

   private void addInitialPosition(BudaBubble bb) {
      initial_position.put(bb,bb.getLocation());
    }

}	// end of inner class PanelMoveContext




/********************************************************************************/
/*										*/
/*	Viewport methods							*/
/*										*/
/********************************************************************************/

private void setViewport(int x,int y)
{
   if (bubble_area == for_root.getDefaultBubbleArea()) {
      // overview for root can have side effects
      for_root.setViewport(x,y);
    }
   else {
      // need to handle overview for channels separately
      Rectangle v = view_area.getBounds();
      Rectangle a = bubble_area.getBounds();

      if (x < 0) x = 0;
      if (x + v.width >= a.width) x = a.width - v.width;
      if (y < 0) y = 0;
      if (y + v.height >= a.height) y = a.height - v.height;

      view_area.setViewPosition(new Point(x,y));
    }
}



/********************************************************************************/
/*										*/
/*	Mouse handling								*/
/*										*/
/********************************************************************************/

private final class Mouser extends MouseAdapter {

   @Override public void mouseClicked(MouseEvent e) {
      handleMouseEvent(e);
    }

   @Override public void mousePressed(MouseEvent e) {
      handleMouseEvent(e);
    }

   @Override public void mouseDragged(MouseEvent e) {
      if (mouse_context != null) mouse_context.next(e);
    }

   @Override public void mouseReleased(MouseEvent e) {
      if (mouse_context != null) {
	 mouse_context.finish();
	 mouse_context = null;
       }
    }

}	 // end of inner class Mouser

}	// end of class BudaOverviewBar




/* end of BudaOverviewBar.java */
