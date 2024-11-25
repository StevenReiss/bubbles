/********************************************************************************/
/*										*/
/*		BudaDefaultPort.java						*/
/*										*/
/*	BUblles Display Area simple port implementation 			*/
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


package edu.brown.cs.bubbles.buda;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;



/**
 *	Standard implementation of a fixed link port for a bubble.
 **/

public class BudaDefaultPort implements BudaConstants.LinkPort, BudaConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Point2D.Double rel_offset;
private Point2D.Double base_offset;
private BudaPortPosition port_pos;
private boolean        is_any;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

/**
 *	Create a port with any position
 **/

public BudaDefaultPort()
{
   this(BudaPortPosition.BORDER_ANY,true);
}



/**
 *	Create a port at the position corresponding to the given region.  Only
 *	border regions and COMPONENT (e.g. the center) are valid regions here.
 **/

public BudaDefaultPort(BudaPortPosition pp,boolean any)
{
   port_pos = pp;
   is_any = any;

   switch (pp) {
      case NONE :
	 rel_offset = null;
	 return;
      case BORDER_N :
	 setPosition(0,-1);
	 break;
      case BORDER_NE :
	 setPosition(1,-1);
	 break;
      case BORDER_E :
	 setPosition(1,0);
	 break;
      case BORDER_SE :
	 setPosition(1,1);
	 break;
      case BORDER_S :
	 setPosition(0,1);
	 break;
      case BORDER_SW :
	 setPosition(-1,1);
	 break;
      case BORDER_W :
	 setPosition(-1,0);
	 break;
      case BORDER_NW :
	 setPosition(-1,-1);
	 break;
      case BORDER_EW :
      case BORDER_EW_TOP :
      case BORDER_NS :
	 is_any = true;
	 setPosition(0,0);
	 break;
      default :
	 setPosition(0,0);
	 break;
    }

   base_offset = new Point2D.Double(rel_offset.getX(),rel_offset.getY());
}



BudaDefaultPort(Element e)
{
   is_any = IvyXml.getAttrBool(e,"ANY");
   port_pos = IvyXml.getAttrEnum(e,"POSITION",BudaPortPosition.NONE);
   Element off = IvyXml.getChild(e,"RELOFF");
   if (off == null) rel_offset = null;
   else rel_offset = new Point2D.Double(IvyXml.getAttrDouble(off,"X",0),
					   IvyXml.getAttrDouble(off,"Y",0));
   off = IvyXml.getChild(e,"BASEOFF");
   if (off == null) base_offset = null;
   else base_offset = new Point2D.Double(IvyXml.getAttrDouble(off,"X",0),
					    IvyXml.getAttrDouble(off,"Y",0));
}





private void setPosition(double x,double y)
{
   if (x < -1) x = -1;
   if (x > 1) x = 1;
   if (y < -1) y = -1;
   if (y > 1) y = 1;
   rel_offset = new Point2D.Double(x,y);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public Point getLinkPoint(BudaBubble bb,Rectangle tgt)
{
   if (rel_offset == null) return null;

   if (is_any) {
      computePosition(bb,tgt.x,tgt.y,tgt.x+tgt.width,tgt.y+tgt.height);
    }

   return computeLinkPoint(bb);
}



@Override public Point getLinkPoint(BudaBubble bb,Point2D tgt)
{
   if (rel_offset == null) return null;

   if (is_any) {
      computePosition(bb,tgt.getX(),tgt.getY(),tgt.getX(),tgt.getY());
    }

   return computeLinkPoint(bb);
}



private Point computeLinkPoint(BudaBubble bb)
{
   Rectangle r = bb.getBounds();

   double px = r.getX() + (rel_offset.getX() + 1.0)/2.0 * r.getWidth();
   double py = r.getY() + (rel_offset.getY() + 1.0)/2.0 * r.getHeight();

   // TODO: take into account border type of the bubble

   return new Point((int) Math.rint(px),(int) Math.rint(py));
}




/********************************************************************************/
/*										*/
/*	Methods for finding port position					*/
/*										*/
/********************************************************************************/

private void computePosition(BudaBubble bb,double tx0,double ty0,double tx1,double ty1)
{
   switch (port_pos) {
      case BORDER_EW :
	 computeEW(bb,tx0,ty0,tx1,ty1);
	 break;
      case BORDER_EW_TOP :
	 computeEWTop(bb,tx0,ty0,tx1,ty1);
	 break;
      case BORDER_NS :
	 computeNS(bb,tx0,ty0,tx1,ty1);
	 break;
      default :
	 computeAny(bb,tx0,ty0,tx1,ty1);
	 break;
    }
}



private void computeAny(BudaBubble bb,double tx0,double ty0,double tx1,double ty1)
{
   Rectangle r = bb.getBounds();
   double px = r.getX() + r.getWidth()/2;
   double py = r.getY() + r.getHeight()/2;
   Shape sh = bb.getShape();
   double tx = (tx0 + tx1)/2;
   double ty = (ty0 + ty1)/2;

   int lx = -1;
   int ly = -1;

   if (ty > r.getY() && ty < r.getY() + r.getHeight()) py = ty;
   if (tx > r.getX() && tx < r.getX() + r.getWidth()) px = tx;

   double t = 0.5;
   double sf = 0.25;
   for ( ; ; ) {
      int ix = (int) (px + t*(tx-px));
      int iy = (int) (py + t*(ty-py));
      if (ix == lx && iy == ly) break;
      lx = ix;
      ly = iy;
      if (sh.contains(ix-r.getX(),iy-r.getY())) t += sf;
      else t -= sf;
      sf /= 2;
    }

   double xv = (lx - (r.getX() + r.getWidth()/2))/r.getWidth()*2;
   double yv = (ly - (r.getY() + r.getHeight()/2))/r.getHeight()*2;
   rel_offset.setLocation(xv,yv);

   switch (port_pos) {
      case NONE :
      case BORDER_ANY :
      case CENTER :
	 return;
      case BORDER_N :
	 if (close(yv,-1)) return;
	 break;
      case BORDER_NE :
	 if (close(yv,-1) || close(xv,1)) return;
	 break;
      case BORDER_E :
	 if (close(xv,1)) return;
	 break;
      case BORDER_SE :
	 if (close(xv,1) || close(yv,1)) return;
	 break;
      case BORDER_S :
	 if (close(yv,1)) return;
	 break;
      case BORDER_SW :
	 if (close(yv,1) || close(xv,-1)) return;
	 break;
      case BORDER_W :
	 if (close(xv,-1)) return;
	 break;
      case BORDER_NW :
	 if (close(xv,-1) || close(yv,-1)) return;
	 break;
      case BORDER_EW_TOP :
	 if (close(xv,-1) || close(xv,1)) return;
	 break;
      case BORDER_NS :
	 if (close(yv,-1) || close(yv,1)) return;
	 break;
      default:
	 break;
    }

   rel_offset.setLocation(base_offset.getX(),base_offset.getY());
}



private static boolean close(double x,double y)
{
   return Math.abs(x-y) < 0.05;
}



/********************************************************************************/
/*										*/
/*	Methods for handling restrict (EW/NS) ports				*/
/*										*/
/********************************************************************************/

private void computeEW(BudaBubble bb,double tx0,double ty0,double tx1,double ty1)
{
   Rectangle r = bb.getBounds();
   double px = r.getX() + r.getWidth()/2;
   double py = r.getY() + r.getHeight()/2;
   Shape sh = bb.getShape();
   double tx = (tx0 + tx1)/2;
   double ty = (ty0 + ty1)/2;

   double xt;
   double yt;

   int lx = -1;
   int ly = -1;

   if (tx1 < r.getX()) xt = r.getX();
   else if (tx0 > r.getX() + r.getWidth()) xt = r.getX() + r.getWidth();
   else xt = r.getX() + r.getWidth();	// overlapping -- need to choose one or the other

   if (ty > r.getY() && ty < r.getY() + r.getHeight()) {
      yt = ty;
      py = yt;
    }
   else {
      double dx = tx - px;
      double delta = dx/1024;
      if (delta > 1) delta = 1;
      if (delta < -1) delta = -1;
      yt = py + (delta * 0.9) * r.getHeight()/2;
    }

   double t = 0.5;
   double sf = 0.25;
   for ( ; ; ) {
      int ix = (int) (px + t*(xt-px));
      int iy = (int) (py + t*(yt-py));
      if (ix == lx && iy == ly) break;
      lx = ix;
      ly = iy;
      if (sh.contains(ix-r.getX(),iy-r.getY())) t += sf;
      else t -= sf;
      sf /= 2;
    }

   double xv = (lx - (r.getX() + r.getWidth()/2))/r.getWidth()*2;
   double yv = (ly - (r.getY() + r.getHeight()/2))/r.getHeight()*2;
   rel_offset.setLocation(xv,yv);
}




private void computeEWTop(BudaBubble bb,double tx0,double ty0,double tx1,double ty1)
{
   Rectangle r = bb.getBounds();
   double px = r.getX() + r.getWidth()/2;
   double py = r.getY() + r.getHeight()/2;
   Shape sh = bb.getShape();
   double xt;
   double yt;

   int lx = -1;
   int ly = -1;

   if (tx1 < r.getX()) xt = r.getX();
   else if (tx0 > r.getX() + r.getWidth()) xt = r.getX() + r.getWidth();
   else xt = r.getX() + r.getWidth();	// overlapping -- need to choose one or the other

   yt = r.getY() + 10;

   double t = 0.5;
   double sf = 0.25;
   for ( ; ; ) {
      int ix = (int) (px + t*(xt-px));
      int iy = (int) (py + t*(yt-py));
      if (ix == lx && iy == ly) break;
      lx = ix;
      ly = iy;
      if (sh.contains(ix-r.getX(),iy-r.getY())) t += sf;
      else t -= sf;
      sf /= 2;
    }

   double xv = (lx - (r.getX() + r.getWidth()/2))/r.getWidth()*2;
   double yv = (ly - (r.getY() + r.getHeight()/2))/r.getHeight()*2;
   rel_offset.setLocation(xv,yv);
}




private void computeNS(BudaBubble bb,double tx0,double ty0,double tx1,double ty1)
{
   Rectangle r = bb.getBounds();
   double px = r.getX() + r.getWidth()/2;
   double py = r.getY() + r.getHeight()/2;
   double tx = (tx0 + tx1)/2;
   double ty = (ty0 + ty1)/2;
   Shape sh = bb.getShape();
   double xt;
   double yt;

   int lx = -1;
   int ly = -1;

   if (ty < r.getY()) yt = r.getY();
   else yt = r.getY() + r.getHeight();

   if (tx > r.getX() && tx < r.getX() + r.getWidth()) {
      xt = tx;
      px = xt;
    }
   else {
      double dy = ty - py;
      double delta = dy/1024;
      if (delta > 1) delta = 1;
      if (delta < -1) delta = -1;
      xt = px + (delta * 0.9) * r.getWidth()/2;
    }

   double t = 0.5;
   double sf = 0.25;
   for ( ; ; ) {
      int ix = (int) (px + t*(xt-px));
      int iy = (int) (py + t*(yt-py));
      if (ix == lx && iy == ly) break;
      lx = ix;
      ly = iy;
      if (sh.contains(ix-r.getX(),iy-r.getY())) t += sf;
      else t -= sf;
      sf /= 2;
    }

   double xv = (lx - (r.getX() + r.getWidth()/2))/r.getWidth()*2;
   double yv = (ly - (r.getY() + r.getHeight()/2))/r.getHeight()*2;
   rel_offset.setLocation(xv,yv);
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public void outputXml(BudaXmlWriter xw)
{
   xw.begin("PORT");
   xw.field("CONFIG","BUDA");
   xw.field("ANY",is_any);
   xw.field("POSITION",port_pos);
   xw.element("RELOFF",rel_offset);
   xw.element("BASEOFF",base_offset);
   xw.end("PORT");
}



/********************************************************************************/
/*										*/
/*	Removal methods 							*/
/*										*/
/********************************************************************************/

@Override public void noteRemoved()			{ }




}	// end of class BudaDefaultPort



/* end of BudaDefaultPort.java */
