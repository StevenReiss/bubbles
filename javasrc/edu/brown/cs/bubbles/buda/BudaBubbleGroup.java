/********************************************************************************/
/*										*/
/*		BudaBubbleGroup.java						*/
/*										*/
/*	BUblles Display Area bubble group					*/
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.buda;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardProperties;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;


public class BudaBubbleGroup implements BudaConstants, ComponentListener,
	Comparable<BudaBubbleGroup> {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Collection<BudaBubble>	group_bubbles;
private Area			group_shape;
private Point2D 		top_point;
private Rectangle		group_bounds;
private String			group_title;
private Shape			title_shape;
private Color			left_color;
private Color			right_color;
private Color			label_color;
private Color			single_left_color;
private Color			single_right_color;
private int			group_index;
private GroupTitle		title_field;
private BoardProperties 	buda_properties;
private RoundRectangle2D.Double title_region;
private double                  scale_factor;

private static boolean		complex_shape = false;

private static int		group_counter = 0;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BudaBubbleGroup()
{
   buda_properties = BoardProperties.getProperties("Buda");

   group_bubbles = new HashSet<BudaBubble>();
   group_shape = null;
   top_point = null;
   left_color = null;
   right_color = null;
   label_color = null;
   single_left_color = BoardColors.getColor(GROUP_SINGLE_LEFT_COLOR_PROP);
   single_right_color = BoardColors.getColor(GROUP_SINGLE_RIGHT_COLOR_PROP);
   group_index = ++group_counter;

   group_title = null;
   title_shape = null;
   title_field = new GroupTitle();
   
   scale_factor = 0;
}




/********************************************************************************/
/*										*/
/*	Bubble methods								*/
/*										*/
/********************************************************************************/

void addBubble(BudaBubble bb)
{
   if (scale_factor == 0) {
      scale_factor = bb.getScaleFactor();
    }
   group_bubbles.add(bb);
   bb.addComponentListener(this);
   clearShape();
}



void removeBubble(BudaBubble bb)
{
   group_bubbles.remove(bb);
   bb.removeComponentListener(this);
   clearShape();
}



public Collection<BudaBubble> getBubbles()
{
   return new ArrayList<BudaBubble>(group_bubbles);
}



boolean isEmpty()			{ return group_bubbles.isEmpty(); }




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void setColor(Color c)
{
   Color c0 = BoardColors.getPaleColor(c);
   setColor(c0,c);
}


void setColor(Color lc,Color rc)
{
   left_color = lc;
   right_color = rc;
   label_color = BoardColors.getPaleColor(right_color,0.625);
}


private void checkColors()
{
   if (left_color != null) return;
   setColor(BudaRoot.getGroupColor(1));
}



boolean isSplit()
{
   Area s = getShape();
   return s != null && !s.isSingular();
}



int getSize()				{ return group_bubbles.size(); }



String getTitle()			{ return group_title; }
void setTitle(String ttl)
{
   if (ttl != null && ttl.length() == 0) ttl = null;

   group_title = ttl;

   if (title_field != null) {
      title_field.setText(group_title);
      Dimension psz;
      if (group_title != null) {
	 JLabel lbl = new JLabel(group_title);
	 lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
	 psz = lbl.getPreferredSize();
       }
      else psz = new Dimension(100,15);

      title_field.setSize(psz);
      title_field.setPreferredSize(psz);
    }
}



JComponent getTitleComponent()		{ return title_field; }




/********************************************************************************/
/*										*/
/*	Comparison methods							*/
/*										*/
/********************************************************************************/

@Override public int compareTo(BudaBubbleGroup g)
{
   int i0 = getSize() - g.getSize();
   if (i0 == 0) i0 = group_index - g.group_index;

   return i0;
}



/********************************************************************************/
/*										*/
/*	Testing methods 							*/
/*										*/
/********************************************************************************/

boolean shouldAdd(BudaBubble b)
{
   /********** Shape intersection code
      Area s = getShape();
      Area s1 = getBubbleShape(b);
      s1.intersect(s);
      if (s1.isEmpty()) return false;
      return true;
   **********/

   if (group_bounds == null) getShape();
   
   if (!b.isVisible()) return false;

   Rectangle bounds1 = getExpandedBounds(b);
   if (!bounds1.intersects(group_bounds)) return false;

   for (BudaBubble bubble : group_bubbles) {
      if (!b.equals(bubble)) {
	 Rectangle bounds2 = getExpandedBounds(bubble);
	 if (bounds1.intersects(bounds2)) return true;
       }
    }

   return false;
}


boolean shouldMerge(BudaBubbleGroup bg)
{
   if (bg == null || bg == this) return false;

   /****** Shape intersection code
      Area s = getShape();
      Area s1 = bg.getShape();
      if (s == null || s1 == null) return false;

      Area s2 = new Area(s);
      s2.intersect(s1);
      if (s2.isEmpty()) return false;

      return true;
   *******/

   if (group_bounds == null) getShape();
   if (bg.group_bounds == null) bg.getShape();
   if (group_bounds == null || bg.group_bounds == null) return false;
   if (!bg.group_bounds.intersects(group_bounds)) return false;

   for (BudaBubble b : bg.group_bubbles) {
      if (shouldAdd(b)) return true;
    }

   return false;

}



BudaRegion correlate(int x,int y)
{
   Area s = getShape();

   if (title_shape != null && title_shape.contains(x,y)) return BudaRegion.GROUP_NAME;

   if (s != null && s.contains(x,y)) return BudaRegion.GROUP;

   return BudaRegion.NONE;
}




/********************************************************************************/
/*                                                                              */
/*      Scaling methods                                                         */
/*                                                                              */
/********************************************************************************/

void setScaleFactor(double sf)
{
   scale_factor = sf;
}


double getAuraSize()
{
   if (scale_factor <= 1) return GROUP_AURA_BUFFER;
   else return GROUP_AURA_BUFFER * scale_factor;
}




/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

void drawGroup(Graphics2D g,boolean overview)
{
   Paint p;
   Point p0 = null;
   Point p1 = null;
   Shape s = null;

   boolean grad = buda_properties.getBoolean(GROUP_DRAW_BACKGROUND_GRADIENT);
   grad = false;
   if (overview) grad = false;

   if (grad) {
      s = getShape();
      if (s == null) return;
      p0 = group_bounds.getLocation();
      p1 = new Point();
      p1.setLocation(p0.getX() + group_bounds.getWidth(),p0.getY());
    }
   else if (group_bubbles.size() == 0) return;

   if (group_bubbles.size() == 1) {
      if (grad) p = new GradientPaint(p0,single_left_color,p1,single_right_color);
      else p = single_right_color;
    }
   else {
      checkColors();
      if (grad) p = new GradientPaint(p0,left_color,p1,right_color);
      else p = right_color;
    }

   Paint px = g.getPaint();

   //added by Ian Strickman
   if (overview) {
      g.setPaint(p);
      if (grad) {
	 g.fill(s);
       }
      else {
	 for (BudaBubble bub : group_bubbles) {
	    s = getBubbleDrawShape(bub);
	    g.fill(s);
	  }
       }
    }

   g.setPaint(p);

   if (grad) {
      g.fill(s);
    }
   else {
      for (BudaBubble bub : group_bubbles) {
	 s = getBubbleDrawShape(bub);
	 g.fill(s);
       }
    }
   g.setPaint(px);

   // handle title region and its associated widget
   if (group_bubbles.size() > 1 && title_field != null && !overview) {
      Point2D top = getTopPoint();
      Dimension sz = title_field.getSize();
      double x0 = (top.getX() - sz.getWidth()/2);
      double y0 = (top.getY() - sz.getHeight()/2);
      Point p2 = new Point((int) x0,(int) y0);
      title_field.setLocation(p2);
      double w0 = sz.getWidth() + 8;
      double h0 = sz.getHeight() + 4;
      if (title_region != null) {
	 g.setColor(BoardColors.getColor("Buda.GroupTitleColor"));
	 g.fill(title_region);
       }
      title_region =
	 new RoundRectangle2D.Double(top.getX() - w0/2,top.getY() - h0/2,w0,h0,w0/2,h0/2);
      g.setColor(label_color);
      g.fill(title_region);
      title_shape = title_region;
      title_field.setVisible(true);
    }
   else if(group_bubbles.size() <= 1 && title_field != null && !overview) {
      setTitle(null);
      title_field.setVisible(false);
    }
   /*else if (title_field != null && !overview) {
      title_field.setVisible(false);
    }*/
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(BudaXmlWriter xw)
{
   if (group_bubbles.size() == 0) return;

   xw.begin("GROUP");
   xw.field("INDEX",group_index);
   if (left_color != null) {
      xw.field("LEFTCOLOR",left_color);
      xw.field("RIGHTCOLOR",right_color);
      xw.field("LABELCOLOR",label_color);
    }
   xw.textElement("TITLE",group_title);
   for (BudaBubble bb : group_bubbles) {
      xw.begin("BUBBLE");
      xw.field("ID",bb.getId());
      xw.end("BUBBLE");
    }
   xw.end("GROUP");
}




/********************************************************************************/
/*										*/
/*	Context menu methods							*/
/*										*/
/********************************************************************************/

void handlePopupMenu(MouseEvent e)
{
   BudaBubble bb1 = null;
   for (BudaBubble bb : group_bubbles) {
      bb1 = bb;
      break;
    }
   if (bb1 == null) return;
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bb1);
   if (bba == null) return;

   JPopupMenu pm = new JPopupMenu();
   pm.add(new RemoveAction());
   pm.show(bba,e.getX(),e.getY());
}



private class RemoveAction extends AbstractAction {

   RemoveAction() {
      super("Remove Group");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BudaBubble bb1 = null;
      for (BudaBubble bb : group_bubbles) {
	 bb1 = bb;
	 break;
       }
      if (bb1 == null) return;
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bb1);
      if (bba == null) return;
      bba.userRemoveGroup(BudaBubbleGroup.this);
    }

}	// end of inner class RemoveAction



/********************************************************************************/
/*										*/
/*	Component management methods						*/
/*										*/
/********************************************************************************/

@Override public void componentHidden(ComponentEvent e)
{
   clearShape();
}


@Override public void componentShown(ComponentEvent e)
{
   clearShape();
}


@Override public void componentMoved(ComponentEvent e)
{
   // TODO: if the whole group moved, then you don't need to clear the shape
   // However, if a single bubble did you do.  This needs to track the set of
   // all bubbles in the group and their movement.  If they all move and by the
   // same amount, then the shape can be moved.  Otherwise it has to be recomputed.

   clearShape();
}


@Override public void componentResized(ComponentEvent e)
{
   clearShape();
}



/********************************************************************************/
/*										*/
/*	Shape Management methods						*/
/*										*/
/********************************************************************************/

private void clearShape()
{
   group_shape = null;
   group_bounds = null;
   top_point = null;
}



Area getShape()
{
   if (group_shape == null || group_bounds == null) {
      for (BudaBubble b : group_bubbles) {
	 Area s0 = getBubbleShape(b);
	 if (group_shape == null) group_shape = s0;
	 else group_shape.add(s0);
       }

      //TODO: SMOOTH THE SHAPE HERE
      if (group_shape == null) group_bounds = null;
      else group_bounds = group_shape.getBounds();
      top_point = null;
    }

   return group_shape;
}



private Rectangle getExpandedBounds(BudaBubble b)
{
   Rectangle bnds = b.getBounds();
   
   double sz = getAuraSize();

   bnds.x -= sz;
   bnds.y -= sz;
   bnds.width += 2*sz-1;
   bnds.height += 2*sz-1;

   return bnds;
}



private Area getBubbleDrawShape(BudaBubble b)
{
   if (complex_shape) return getBubbleShape(b);

   Rectangle br = b.getBounds();
   double sz = getAuraSize();
   double x0=br.getX() - sz;
   double y0=br.getY() - sz;
   double width=br.getWidth() + 2*sz-1;
   double height=br.getHeight() + 2*sz-1;

   return new Area(new RoundRectangle2D.Double(x0, y0, width, height, GROUP_AURA_ARC, GROUP_AURA_ARC));
}


private Area getBubbleShape(BudaBubble b)
{
   if (!complex_shape) {
      Rectangle br = b.getBounds();
      double sz = getAuraSize();
      double x0 = br.getX() - sz;
      double y0 = br.getY() - sz;
      double width = br.getWidth() + 2*sz-1;
      double height = br.getHeight() + 2*sz-1;
      return new Area(new Rectangle2D.Double(x0, y0, width, height));
    }

   Rectangle br = b.getBounds();
   Path2D.Double p2 = new Path2D.Double();
   double bn = GROUP_BORDER_NEAR; // + (group_bubbles.size()-1)*2;
   double bf = GROUP_BORDER_FAR; // + (group_bubbles.size()-1)*4;

   // Shapes around outside
   double x0 = br.getX();
   double y0 = br.getY() - bn;
   double x1 = br.getX() + br.getWidth() - 1;
   double y1 = y0;
   double x2 = x1 + bn;
   double y2 = br.getY();
   double x3 = x2;
   double y3 = br.getY() + br.getHeight() - 1;
   double x4 = x1;
   double y4 = y3 + bn;
   double x5 = x0;
   double y5 = y4;
   double x6 = br.getX() - bn;
   double y6 = y3;
   double x7 = x6;
   double y7 = y2;

   // Control points for arcs
   double x01 = (x0 + x1)/2;
   double y01 = y0 - (bf-bn) * 2;
   double x12 = (x1 + x2)/2;
   double y12 = (y1 + y2)/2;
   double x23 = x2 + (bf-bn) * 2;
   double y23 = (y2 + y3)/2;
   double x34 = (x3 + x4)/2;
   double y34 = (y3 + y4)/2;
   double x45 = x01;
   double y45 = y4 + (bf-bn) * 2;
   double x56 = (x5 + x6)/2;
   double y56 = (y5 + y6)/2;
   double x67 = x6 - (bf-bn) * 2;
   double y67 = y23;
   double x70 = (x0 + x7)/2;		// x7
   double y70 = (y0 + y7)/2;		// y0

   p2.moveTo(x0,y0);
   p2.quadTo(x01,y01,x1,y1);
   p2.quadTo(x12,y12,x2,y2);
   p2.quadTo(x23,y23,x3,y3);
   p2.quadTo(x34,y34,x4,y4);
   p2.quadTo(x45,y45,x5,y5);
   p2.quadTo(x56,y56,x6,y6);
   p2.quadTo(x67,y67,x7,y7);
   p2.quadTo(x70,y70,x0,y0);

   return new Area(p2);
}



private Point2D getTopPoint()
{
   if (top_point == null) {
      Rectangle bounds=new Rectangle(Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0);

      for (BudaBubble b : group_bubbles) {
	 if (b.getY() < bounds.y) {
	    bounds = b.getBounds();
	  }
       }

      double topy = bounds.getY() - getAuraSize();
      double topx = bounds.getCenterX();
      top_point=new Point2D.Double(topx, topy-5);
   }

   /********** Shape-specific code
   if (top_point == null) {
      double [] coords = new double[6];
      double topx = 0;
      double topy = 0;
      double lastx = 0;
      double lasty = 0;
      int pno = 0;
      Area s = getShape();

      // find the top-center point for a bubble label
      for (PathIterator pi = s.getPathIterator(null,1.0); !pi.isDone(); pi.next()) {
	 pi.currentSegment(coords);
	 if (pno == 0 || coords[1] < topy) {
	    topx = coords[0];
	    topy = coords[1];
	  }
	 else if (coords[1] == topy && topy == lasty && topx == lastx) {
	    topx = (topx + coords[0])/2.0;
	  }
	 lastx = coords[0];
	 lasty = coords[1];
	 ++pno;

	 top_point = new Point2D.Double(topx,topy);
       }
    }
   ***********/

   return top_point;
}



/********************************************************************************/
/*										*/
/*	Class for title region							*/
/*										*/
/********************************************************************************/

private class GroupTitle extends JTextField implements ActionListener, FocusListener, NoBubble
{

   private Dimension orig_size;

   private static final long serialVersionUID = 1L;

   GroupTitle() {
      super(1);
      orig_size = null;
      Dimension sz = new Dimension(100,15);
      setSize(sz);
      setMinimumSize(sz);
      setHorizontalAlignment(SwingConstants.CENTER);
      setPreferredSize(sz);
      setOpaque(false);
      setBorder(null);
      setFont(getFont().deriveFont(Font.BOLD));
      setVisible(false);
      setFocusable(true);
      addActionListener(this);
      addFocusListener(this);
      addMouseListener(new FocusOnEntry());
    }

   @Override public void actionPerformed(ActionEvent e) {
      String t = getText();
      String ottl = getTitle();
      setTitle(t);
      getParent().repaint();
      orig_size = null;
      if (ottl == null && t != null && t.length() > 0 && group_bubbles.size() > 1) {
	 for (BudaBubble bb : group_bubbles) {
	    BudaRoot br = BudaRoot.findBudaRoot(bb);
	    if (br != null) {
	       br.noteNamedBubbleGroup(BudaBubbleGroup.this);
	       break;
	     }
	  }
       }
    }

   @Override public void focusGained(FocusEvent e) {
      checkColors();
      setBackground(label_color);
      setOpaque(true);
      orig_size = getSize();
      Dimension sz = getSize();
      //if (sz.height < 15) sz.height = 15;
      //if (sz.width < 100) sz.width = 100;
      setSize(sz);
      repaint();
      getParent().repaint();
    }

   @Override public void focusLost(FocusEvent e) {
      checkColors();
      setBackground(label_color);
      setOpaque(false);
      String t = getText();
      if (!t.equals(group_title)) {
	  setTitle(t);
	  orig_size = null;
	  getParent().repaint();
       }
      else if (orig_size != null) setSize(orig_size);
    }

}	// end of inner class GroupTitle



}	// end of class BudaBubbleArea




/* end of BudaBubbleArea.java */


