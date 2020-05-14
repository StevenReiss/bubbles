/********************************************************************************/
/*										*/
/*		BudaRouter.java 						*/
/*										*/
/*	BUblles Display Area bubble link routing algorithms			*/
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


/* SVN: $Id$ */



package edu.brown.cs.bubbles.buda;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;



class BudaRouter implements BudaConstants {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<BudaBubbleLink> work_links;

private SortedMap<Double,HPath> h_paths;
private SortedMap<Double,VPath> v_paths;
private List<HPath> new_hpaths;
private List<VPath> new_vpaths;

private static final double ON_SIDE = 5.0;
private static final double TARGET_DELTA = 10;
private static final double CHANNEL_SIZE = 5;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BudaRouter(BudaBubbleArea bba)
{
   work_links = bba.getAllLinks();
   h_paths = new TreeMap<Double,HPath>();
   v_paths = new TreeMap<Double,VPath>();
}




/********************************************************************************/
/*										*/
/*	Computation methods for all links					*/
/*										*/
/********************************************************************************/

void computeRoutes()
{
   for (BudaBubbleLink bbl : work_links) {
      bbl.clearPivots();
    }

   for (BudaBubbleLink bbl : work_links) {
      if (bbl.isRectilinear()) {
	 computeRoute(bbl);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Computation method for a single rectilinear link			*/
/*										*/
/********************************************************************************/

private void computeRoute(BudaBubbleLink bbl)
{
   Point src = bbl.getSourcePoint();
   if (src == null) return;
   Point tgt = bbl.getTargetPoint(src);
   if (tgt == null) return;

   BudaBubble sbbl = bbl.getSource();
   BudaBubble tbbl = bbl.getTarget();
   Rectangle srect = sbbl.getBounds();
   Rectangle trect = tbbl.getBounds();

   new_vpaths = new ArrayList<VPath>();
   new_hpaths = new ArrayList<HPath>();

   boolean validleft = Math.abs(src.getX() - srect.getX()) < ON_SIDE;
   boolean validright = Math.abs(src.getX() - srect.getX() - srect.getWidth()) < ON_SIDE;
   boolean validup = Math.abs(src.getY() - srect.getY()) < ON_SIDE;
   boolean validdown = Math.abs(src.getY() - srect.getY() - srect.getHeight()) < ON_SIDE;
   boolean tgtright = Math.abs(tgt.getX() - trect.getX()) < ON_SIDE;
   boolean tgtleft = Math.abs(tgt.getX() - trect.getX() - trect.getWidth()) < ON_SIDE;
   boolean tgtdown = Math.abs(tgt.getY() - trect.getY()) < ON_SIDE;
   boolean tgtup = Math.abs(tgt.getY() - trect.getY() - trect.getHeight()) < ON_SIDE;

   double x0 = src.getX();
   double y0 = src.getY();

   for (int i = 0; i < 6; ++i) {
      // first check if we actually need more pivots
      if (y0 == tgt.getY()) {
	 if (x0 < tgt.getX() && validright && tgtright) break;
	 if (x0 > tgt.getX() && validleft && tgtleft) break;
       }
      if (x0 == tgt.getX()) {
	 if (y0 > tgt.getY() && validup && tgtup) break;
	 if (y0 < tgt.getY() && validdown && tgtdown) break;
       }

      // then, if we have multiple options, determine actual direction
      if (validleft && validright) {
	 if (tgt.getX() > x0) validleft = false;
	 else if (tgt.getX() == x0 && tgtleft) validleft = false;
	 else if (tgt.getX() == x0 && tgtright) validright = false;
	 else validright = false;
       }
      if (validup && validdown) {
	 if (tgt.getY() >= y0) validup = false;
	 else validdown = false;
       }

      if (validleft) {
	 double x1;
	 if (tgt.getX() < x0) {
	    if (tgtup || tgtdown) x1 = tgt.getX();
	    else if (tgtleft) {
	       x1 = (x0 + tgt.getX())/2;
	       if (x1 < x0 - TARGET_DELTA) x1 = x0 - TARGET_DELTA;
	     }
	    else x1 = tgt.getX() - TARGET_DELTA;
	  }
	 else x1 = x0 - TARGET_DELTA;
	 x0 = addHPivot(bbl,x0,y0,x1,tgt.getY());
	 validleft = validright = false;
	 validup = validdown = true;
       }
      else if (validright) {
	 double x1;
	 if (tgt.getX() > x0) {
	    if (tgtup || tgtdown) x1 = tgt.getX();
	    else if (tgtright) {
	       x1 = (x0 + tgt.getX())/2;
	       if (x1 > x0 + TARGET_DELTA) x1 = x0 + TARGET_DELTA;
	     }
	    else x1 = tgt.getX() + TARGET_DELTA;
	  }
	 else x1 = x0 + TARGET_DELTA;
	 x0 = addHPivot(bbl,x0,y0,x1,tgt.getY());
	 validleft = validright = false;
	 validup = validdown = true;
       }
      else if (validup) {
	 double y1;
	 if (tgt.getY() < y0) {
	    if (tgtleft || tgtright) y1 = tgt.getY();
	    else if (tgtup) {
	       y1 = (y0 + tgt.getY())/2;
	       if (y1 < y0 - TARGET_DELTA) y1 = y0 - TARGET_DELTA;
	     }
	    else y1 = tgt.getY() - TARGET_DELTA;
	  }
	 else y1 = y0 - TARGET_DELTA;
	 y0 = addVPivot(bbl,x0,y0,y1,tgt.getX());
	 validleft = validright = true;
	 validup = validdown = false;
       }
      else if (validdown) {
	 double y1;
	 if (tgt.getY() > y0) {
	    if (tgtleft || tgtright) y1 = tgt.getY();
	    else if (tgtdown) {
	       y1 = (y0 + tgt.getY())/2;
	       if (y0 > y0 + TARGET_DELTA) y1 = y0 + TARGET_DELTA;
	     }
	    else y1 = tgt.getY() + TARGET_DELTA;
	  }
	 else y1 = y0 + TARGET_DELTA;
	 y0 = addVPivot(bbl,x0,y0,y1,tgt.getX());
	 validleft = validright = true;
	 validup = validdown = false;
       }
    }

   for (HPath hp : new_hpaths) h_paths.put(hp.getPosition(),hp);
   for (VPath vp : new_vpaths) v_paths.put(vp.getPosition(),vp);
   new_hpaths = null;
   new_vpaths = null;

   return;
}



private double addHPivot(BudaBubbleLink bbl,double x0,double y0,double x1,double yt)
{
   double x2 = x1;

   for (int i = 0; ; ++i) {
      if ((x0 < x1 && x2 > x0) || (x0 > x1 && x2 < x0)) {
	 SortedMap<Double,VPath> vmap = v_paths.subMap(x2-CHANNEL_SIZE/2,x2+CHANNEL_SIZE/2);
	 if (vmap.isEmpty()) break;
	 boolean ovlap = false;
	 for (VPath vp : vmap.values()) {
	    if (vp.overlaps(y0,yt)) {
	       ovlap = true;
	    }
	 }
	 if (!ovlap) break;
       }
      if ((i&1) == 0) x2 = x1 + (i/2+1)*CHANNEL_SIZE;
      else x2 = x1 - (i/2+1)*CHANNEL_SIZE;
    }

   x2 = Math.round(x2);
   bbl.addPivot(x2,y0);

   HPath hp = h_paths.get(y0);
   if (hp != null) hp.extend(x0,x2);
   else {
      hp = new HPath(y0,x0,x2);
      new_hpaths.add(hp);
    }

   return x2;
}



private double addVPivot(BudaBubbleLink bbl,double x0,double y0,double y1,double xt)
{
   double y2 = y1;

   for (int i = 0; ; ++i) {
      if ((y0 < y1 && y2 > y0) || (y0 > y1 && y2 < y0)) {
	 SortedMap<Double,HPath> hmap = h_paths.subMap(y2-CHANNEL_SIZE/2,y2+CHANNEL_SIZE/2);
	 boolean ovlap = false;
	 for (HPath hp : hmap.values()) {
	    if (hp.overlaps(x0,xt)) {
	      ovlap = true; 
	    }
	  }
	 if (!ovlap) break;
       }

      if ((i&1) == 0) y2 = y1 + (i/2+1)*CHANNEL_SIZE;
      else y2 = y1 - (i/2+1)*CHANNEL_SIZE;
    }

   y2 = Math.round(y2);
   bbl.addPivot(x0,y2);

   VPath vp = v_paths.get(x0);
   if (vp != null) vp.extend(y0,y2);
   else {
      vp = new VPath(x0,y0,y2);
      new_vpaths.add(vp);
    }

   return y2;
}



/********************************************************************************/
/*										*/
/*	Holder for occupied paths						*/
/*										*/
/********************************************************************************/

private static class VPath {

   private double x_pos;
   private double y_from;
   private double y_to;

   VPath(double x,double y0,double y1) {
      x_pos = x;
      y_from = Math.min(y0,y1);
      y_to = Math.max(y0,y1);
    }

   void extend(double y0,double y1) {
      y_from = Math.min(y_from,Math.min(y0,y1));
      y_to = Math.max(y_to,Math.max(y0,y1));
    }

   double getPosition() 		{ return x_pos; }
   
   boolean overlaps(double y0,double y1) {
      double ymin = Math.min(y0,y1);
      double ymax = Math.max(y0,y1);
      if (ymin >= y_to || ymax <= y_from) return false;
      return true;
    }
}	// end of inner class VPath




private static class HPath {

   private double y_pos;
   private double x_from;
   private double x_to;

   HPath(double y,double x0,double x1) {
      y_pos = y;
      x_from = Math.min(x0,x1);
      x_to = Math.max(x0,x1);
    }

   void extend(double x0,double x1) {
      x_from = Math.min(x_from,Math.min(x0,x1));
      x_to = Math.max(x_to,Math.max(x0,x1));
    }

   double getPosition() 		{ return y_pos; }
   
   boolean overlaps(double x0,double x1) {
      double xmin = Math.min(x0,x1);
      double xmax = Math.max(x0,x1);
      if (xmin >= x_to || xmax <= x_from) return false;
      return true;
    }

}	// end of inner class HPath




}	// end of class BudaRouter




/* end of BudaRouter.java */
