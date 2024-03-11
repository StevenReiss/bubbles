/********************************************************************************/
/*										*/
/*		BudaBubbleLink.java						*/
/*										*/
/*	BUblles Display Area representation of an arrow between bubbles 	*/
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
import edu.brown.cs.bubbles.board.BoardMetrics;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;


/**
 *	This class represents a link between two bubbles that is drawn on the display.
 **/

public class BudaBubbleLink implements BudaConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BudaBubble	from_bubble;
private LinkPort	from_port;
private BudaBubble	to_bubble;
private LinkPort	to_port;
private Path2D		drawn_path;

private BasicStroke	link_stroke;
private BudaLinkStyle	link_style;
private Color		link_color;
private Color		focus_color;
private boolean 	has_focus;
private String		link_data;

private List<Point2D>	pivot_points;
private BudaPortEndType start_type;
private BudaPortEndType finish_type;

private boolean 	is_rectilinear;


private static final float [] DASH_ARRAY = new float [] { 10f, 5f };



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

/**
 *	Create a solid rectilinear link from one bubble to another using the given
 *	port locations.
 **/

public BudaBubbleLink(BudaBubble frm,LinkPort fpt,BudaBubble to,LinkPort tpt)
{
   this(frm,fpt,to,tpt,true,BudaLinkStyle.STYLE_SOLID);
}








/**
 *	Create a link from one bubble to another using the designated ports and
 *	letting the caller determine the arc style and whether it is rectilinear or not.
 **/

public BudaBubbleLink(BudaBubble frm,LinkPort fpt,BudaBubble to,LinkPort tpt,boolean rect,
			 BudaLinkStyle sty)
{
   from_bubble = frm;
   from_port = fpt;
   to_bubble = to;
   to_port = tpt;
   is_rectilinear = rect;
   start_type = BudaPortEndType.END_NONE;
   finish_type = BudaPortEndType.END_ARROW;
   setStyle(LINK_WIDTH,sty);
   link_data = null;
   link_color = BoardColors.getColor(LINK_COLOR_PROP);
   focus_color = BoardColors.getColor(LINK_FOCUS_COLOR_PROP);
   drawn_path = null;
   has_focus = false;
   pivot_points = null;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public BudaBubble getSource()				{ return from_bubble; }
public BudaBubble getTarget()				{ return to_bubble; }


boolean usesBubble(BudaBubble b)
{
   return from_bubble == b || to_bubble == b;
}


void setWidth(float w)
{
   link_stroke = new BasicStroke(w,BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND);
   setStyle(w,link_style);
}



void setStyle(BudaLinkStyle sty)
{
   setStyle(link_stroke.getLineWidth(),sty);
}


/**
 *	Set the style for the link.
 **/

public void setStyle(float w,BudaLinkStyle sty)
{
   link_style = sty;
   if (w < 0) w = link_stroke.getLineWidth();

   switch (sty) {
      case STYLE_SOLID :
      case STYLE_FLIP_SOLID :
	 link_stroke = new BasicStroke(w,BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND);
	 break;
      case STYLE_DASHED :
      case STYLE_FLIP_DASHED :
	 link_stroke = new BasicStroke(w,BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND,
					  1.0f,DASH_ARRAY,0);
	 break;
      case STYLE_REFERENCE :
      case STYLE_FLIP_REFERENCE :
	 link_stroke = new BasicStroke(w,BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND);//DoubleStroke(w);
	 finish_type = BudaPortEndType.END_TRIANGLE_FILLED;
	 //TODO: make more different somehow...
	 break;
      default:
	 break;
    }

   switch (sty) {
      case STYLE_FLIP_SOLID :
      case STYLE_FLIP_DASHED :
      case STYLE_FLIP_REFERENCE :
	 flip();
	 break;
      default :
	 break;
    }
}


/**
 *	Set the data to be displayed for the link
 **/

public void setLinkData(String txt)
{
   link_data = txt;
}




/**
 *	Set the color to be used for the link
 **/

public void setColor(Color c)
{
   if (c == null) c = BoardColors.getColor(LINK_COLOR_PROP);
   link_color = c;
}



/**
 *	Set the focus color for the link
 **/

public void setFocusColor(Color c)
{
   if (c == null) c = BoardColors.getColor(LINK_FOCUS_COLOR_PROP);
   focus_color = c;
}



void setHasFocus(boolean fg)
{
   has_focus = fg;
}


boolean getHasFocus()				{ return has_focus; }


public void setEndTypes(BudaPortEndType st,BudaPortEndType et)
{
   if (st != null) start_type = st;
   if (et != null) finish_type = et;
}


public void flip()
{
   setEndTypes(finish_type,start_type);
}



/********************************************************************************/
/*										*/
/*	Pivot methods								*/
/*										*/
/********************************************************************************/

boolean isRectilinear() 			{ return is_rectilinear; }
void setRectilinear(boolean fg) 		{ is_rectilinear = fg; }

void clearPivots()				{ pivot_points = null; }

void addPivot(double x,double y)
{
   if (pivot_points == null) pivot_points = new ArrayList<Point2D>();
   pivot_points.add(new Point2D.Double(x,y));
}



Point getSourcePoint()
{
   return from_port.getLinkPoint(from_bubble,to_bubble.getBounds());
}


Point getTargetPoint(Point2D src)
{
   if (src == null) return null;

   return to_port.getLinkPoint(to_bubble,src);
}




/********************************************************************************/
/*										*/
/*	Context menu processing 						*/
/*										*/
/********************************************************************************/

void handlePopupMenu(MouseEvent e)
{
   JPopupMenu pm = new JPopupMenu();
   pm.add(new RemoveAction());
   JMenu m = new JMenu("Style ...");
   m.add(new StyleAction("Solid",BudaLinkStyle.STYLE_SOLID));
   m.add(new StyleAction("Dashed",BudaLinkStyle.STYLE_DASHED));
   m.add(new StyleAction("Reference",BudaLinkStyle.STYLE_REFERENCE));
   m.add(new StyleAction("Flip Solid",BudaLinkStyle.STYLE_FLIP_SOLID));
   m.add(new StyleAction("Flip Dashed",BudaLinkStyle.STYLE_FLIP_DASHED));
   m.add(new StyleAction("Flip Reference",BudaLinkStyle.STYLE_FLIP_REFERENCE));
   pm.add(m);
   pm.add(new CollapseAction());
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(from_bubble);
   if (bba != null) pm.show(bba,e.getX(),e.getY());
}


private class RemoveAction extends AbstractAction {
   
   private static final long serialVersionUID = 1;
   
   RemoveAction() {
      super("Remove");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(from_bubble);
      BoardMetrics.noteCommand("BUDA","actionRemoveLink");
      if (bba != null) bba.removeLink(BudaBubbleLink.this);
   }

}	// end of inner class RemoveAction


private class StyleAction extends AbstractAction {

   BudaLinkStyle set_style;
   private static final long serialVersionUID = 1;
   
   StyleAction(String id,BudaLinkStyle sty) {
      super(id);
      set_style = sty;
    }

   @Override public void actionPerformed(ActionEvent e) {
      setStyle(set_style);
      BoardMetrics.noteCommand("BUDA","actionStyleLink");
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(from_bubble);
      if (bba != null) bba.repaint();
    }

}	// end of inner class StyleAction


private class CollapseAction extends AbstractAction {
   
   private static final long serialVersionUID = 1;
   
   CollapseAction() {
      super("Collapse");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.noteCommand("BUDA","actionCollapseLink");
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(from_bubble);
      List<BudaBubbleLink> lnks = new ArrayList<>();
      lnks.add(BudaBubbleLink.this);
      if (bba != null) bba.collapseLinks(lnks);
    }

}	// end of inner class CollapseAction



/********************************************************************************/
/*										*/
/*	Methods to handle remove						*/
/*										*/
/********************************************************************************/

void noteRemoved()
{
   clearPivots();

   if (from_port != null) from_port.noteRemoved();
   if (to_port != null) to_port.noteRemoved();
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void drawLink(Graphics2D g,boolean overview)
{
   Point2D src = getSourcePoint();
   if (src == null) return;
   Point2D tgt = getTargetPoint(src);
   if (tgt == null) return;

   Point2D second = null;
   Point2D penult = null;
   Path2D.Double pa = new Path2D.Double();
   pa.moveTo(src.getX(),src.getY());
   if (pivot_points == null) {
      second = tgt;
      penult = src;
    }
   else {
      for (Point2D pv : pivot_points) {
	 if (second == null) second = pv;
	 penult = pv;
	 pa.lineTo(pv.getX(),pv.getY());
       }
    }
   pa.lineTo(tgt.getX(),tgt.getY());

   g.setStroke(link_stroke);
   if (has_focus) g.setColor(focus_color);
   else g.setColor(link_color);
   g.draw(pa);

   if (link_data != null) {
      // draw link name here
    }

   if (!overview) {
      drawEnd(g,second,src,start_type);
      drawEnd(g,penult,tgt,finish_type);
      drawn_path = pa;
    }
}



/********************************************************************************/
/*										*/
/*	End point drawing methods						*/
/*										*/
/********************************************************************************/

private void drawEnd(Graphics2D g,Point2D from,Point2D to,BudaPortEndType typ)
{
   switch (typ) {
      default :
      case END_NONE :
	 break;
      case END_ARROW :
	 drawArrow(g,from,to);
	 break;
      case END_CIRCLE :
	 drawCircle(g,from,to,false);
	 break;
      case END_TRIANGLE :
	 drawTriangle(g,from,to,false);
	 break;
      case END_SQUARE :
	 drawSquare(g,from,to,false);
	 break;
      case END_ARROW_FILLED :
      case END_TRIANGLE_FILLED :
	 drawTriangle(g,from,to,true);
	 break;
      case END_CIRCLE_FILLED :
	 drawCircle(g,from,to,true);
	 break;
      case END_SQUARE_FILLED :
	 drawSquare(g,from,to,false);
	 break;
    }
}



private void drawCircle(Graphics g,Point2D fp,Point2D tp,boolean fill)
{
   double d = fp.distance(tp);

   if (d == 0) return;

   double t = BUDA_LINK_END_SIZE/d;
   double cx0 = tp.getX() + t*(fp.getX() - tp.getX());
   double cy0 = tp.getY() + t*(fp.getY() - tp.getY());

   int x0 = (int)(cx0 - BUDA_LINK_END_SIZE+1);
   int y0 = (int)(cy0 - BUDA_LINK_END_SIZE+1);
   int wd = (int)(2 * BUDA_LINK_END_SIZE - 1);
   int ht = (int)(2 * BUDA_LINK_END_SIZE - 1);

   if (fill) g.fillOval(x0,y0,wd,ht);
   else g.drawOval(x0,y0,wd,ht);
}





private void drawTriangle(Graphics g,Point2D fp,Point2D tp,boolean fill)
{
   double d = fp.distance(tp);

   if (d == 0) return;

   double t = BUDA_LINK_END_SIZE*1.25/d;
   double cx0 = tp.getX() + 2*t*(fp.getX() - tp.getX());
   double cy0 = tp.getY() + 2*t*(fp.getY() - tp.getY());
   double cx1 = cx0 + t*(fp.getY() - tp.getY());
   double cy1 = cy0 - t*(fp.getX() - tp.getX());
   double cx2 = cx0 - t*(fp.getY() - tp.getY());
   double cy2 = cy0 + t*(fp.getX() - tp.getX());

   Polygon p = new Polygon();
   p.addPoint((int) tp.getX(),(int) tp.getY());
   p.addPoint((int) cx1,(int) cy1);
   p.addPoint((int) cx2,(int) cy2);

   if (fill) g.fillPolygon(p);
   else g.drawPolygon(p);
}




private void drawArrow(Graphics g,Point2D fp,Point2D tp)
{
   double d = fp.distance(tp);

   if (d == 0) return;

   double t = BUDA_LINK_END_SIZE/d;
   double cx0 = tp.getX() + 2*t*(fp.getX() - tp.getX());
   double cy0 = tp.getY() + 2*t*(fp.getY() - tp.getY());
   double cx1 = cx0 + t*(fp.getY() - tp.getY());
   double cy1 = cy0 - t*(fp.getX() - tp.getX());
   double cx2 = cx0 - t*(fp.getY() - tp.getY());
   double cy2 = cy0 + t*(fp.getX() - tp.getX());

   g.drawLine((int) cx1,(int) cy1,(int) tp.getX(),(int) tp.getY());
   g.drawLine((int) cx2,(int) cy2,(int) tp.getX(),(int) tp.getY());
}



private void drawSquare(Graphics g,Point2D fp,Point2D tp,boolean fill)
{
   double d = fp.distance(tp);

   if (d == 0) return;

   double dx = (tp.getX() - fp.getX())/d*BUDA_LINK_END_SIZE*0.5;
   double dy = (fp.getY() - tp.getY())/d*BUDA_LINK_END_SIZE*0.5;

   Polygon p = new Polygon();
   p.addPoint((int)(tp.getX()-dy),(int)(tp.getY()-dx));
   p.addPoint((int)(tp.getX()+dy),(int)(tp.getY()+dx));
   p.addPoint((int)(tp.getX()-2*dx-dy),(int)(tp.getY()-2*dy-dx));
   p.addPoint((int)(tp.getX()-2*dx+dy),(int)(tp.getY()-2*dy+dx));

   if (fill) g.fillPolygon(p);
   else g.drawPolygon(p);
}


/********************************************************************************/
/*										*/
/*	Correlation methods							*/
/*										*/
/********************************************************************************/

BudaRegion correlate(int x,int y)
{
   if (drawn_path == null) return BudaRegion.NONE;

   Rectangle tgt = new Rectangle(x-1,y-1,3,3);

   double [] coords = new double[6];
   double lx = 0;
   double ly = 0;
   Line2D l2 = new Line2D.Double();
   for (PathIterator pi = drawn_path.getPathIterator(null,1.0); !pi.isDone(); pi.next()) {
      int typ = pi.currentSegment(coords);
      if (typ == PathIterator.SEG_LINETO) {
	 l2.setLine(lx,ly,coords[0],coords[1]);
	 if (l2.intersects(tgt)) return BudaRegion.LINK;
       }
      lx = coords[0];
      ly = coords[1];
    }

   return BudaRegion.NONE;
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(BudaXmlWriter xw)
{
   xw.begin("LINK");
   xw.field("RECT",is_rectilinear);
   xw.field("STYLE",link_style);
   xw.field("WIDTH",link_stroke.getLineWidth());
   xw.begin("FROM");
   xw.field("ID",from_bubble.getId());
   from_port.outputXml(xw);
   xw.end("FROM");
   xw.begin("TO");
   xw.field("ID",to_bubble.getId());
   to_port.outputXml(xw);
   xw.end("TO");
   xw.end("LINK");
}





}	// end of class BudaBubbleLink




/* end of BudaBubbleLink.java */
