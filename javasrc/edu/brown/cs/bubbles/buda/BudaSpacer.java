/********************************************************************************/
/*										*/
/*		BudaSpacer.java 						*/
/*										*/
/*	BUblles Display Area bubble spacer					*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss, Andrew Bragdon    */
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

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;



class BudaSpacer implements BudaConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BudaBubbleArea		bubble_area;
private Configuration		start_config;

private static final int	BUBBLE_SPACE = 6;

private static final long	MAX_SPACING_TIME = 1000;

private static boolean		KEEP_BUBBLES_ON_SCREEN = false;
private static boolean		DONT_GO_OFF_TOP = false;
private static boolean		DONT_GO_OFF_BOTTOM = false;
private static boolean		DONT_GO_OFF_LEFT = false;
private static boolean		DONT_GO_OFF_RIGHT = false;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BudaSpacer(BudaBubbleArea ba)
{
   bubble_area = ba;

   start_config = new Configuration(ba);
}



BudaSpacer(BudaBubbleArea ba,BudaBubble fix)
{
   this(ba);

   start_config.setMoved(fix);
}



BudaSpacer(BudaBubbleArea ba,Collection<BudaBubble> bbs)
{
   this(ba);

   for (BudaBubble bb : bbs) start_config.setMoved(bb);
}



/********************************************************************************/
/*										*/
/*	Execution methods							*/
/*										*/
/********************************************************************************/

void makeRoomFor(BudaBubble bb)
{
   if (bb.isFixed()) return;

   Configuration start = new Configuration(start_config);
   List<Configuration> cfgs = new ArrayList<>();

   Adjustment adj = new Adjustment(new BubbleProxy(bb));

   cfgs.addAll(makeAMove(start,adj,0,System.currentTimeMillis()));

   Configuration best = evaluate(cfgs);
   if (best != null) {
      Collection<BudaBubble> del = new ArrayList<>();
      for (BubbleProxy bp : best.getBubbles()) {
	 if (bp.isMoved()) {
	    Rectangle r = getInnerBounds(bp.getPlacement());
	    bubble_area.addMovingBubble(bp.getBubble());
	    del.add(bp.getBubble());
	    bubble_area.moveBubble(bp.getBubble(),r.getLocation(),false);
	  }
       }
      bubble_area.removeMovingBubbles(del);
    }
}




private Collection<Configuration> makeAMove(Configuration cur,Adjustment mvd,int recurse,long when)
{
   List<Configuration> rslt = new ArrayList<Configuration>();

   makeAMoveInternal(rslt,cur,mvd,recurse,when);

   return rslt;
}




private Collection<Configuration> makeAMoveInternal(List<Configuration> rslt,
						       Configuration cur,
						       Adjustment mvd,int recurse,long when)
{
   if (!mvd.hasChanges()) {
      rslt.add(cur);
    }
   else {
      for (BubbleProxy bp : mvd.getMoved()) {
	 BubbleProxy cpb = cur.getProxy(bp.getBubble());
	 if (cpb != null) cpb.setPlacement(bp.getPlacement());
       }
      for (Adjustment adj : generateMoveSets(cur,mvd)) {
	 makeAMoveInternal(rslt,new Configuration(cur),adj,recurse+1,when);
	 long ts = System.currentTimeMillis() - when;
	 if (ts > MAX_SPACING_TIME && ts < 10000) break;      // avoid debugging problems
       }
    }

   return rslt;
}




private Collection<Adjustment> generateMoveSets(Configuration cur,Adjustment mvd)
{
   // bubbles overlap : this is a failed adjustment
   if (mvd.hasOverlap()) return new ArrayList<>();

   Map<BubbleProxy,List<Movement>> overlapped = new HashMap<>();
   List<Movement> mvmts = getBubbleProxyMovements(mvd.getMoved());

   // go through each bubble to see if it is in the path of a bubble that is being moved.
   // If a bubble is in the path and that bubble has already been moved, then this can't be a valid configuration so we return
   // Otherwise, we collect all the movement paths that overlap each bubble.
   for (BubbleProxy bp : cur.getBubbles()) {
      if (!mvd.contains(bp)) {
	 // bp overlaps with something in moved
	 List<Movement> ovl = findMovementsForOverlappingBubble(bp,mvmts);
	 if (ovl.size() > 0) {
	    if (bp.isMoved()) {
	       if (bp.overlaps(cur.getBubbles(),true)) {
		  // this adjustment is invalid -- it generates a cycle of movements
		  return new ArrayList<>();
		}
	     }
	    else overlapped.put(bp,ovl);
	  }
       }
    }

   // now we generate Movements for each Bubble that overlapped one or
   //  more Movements of other Bubbles.

   Map<BubbleProxy,List<BubbleProxy>> possibles = new HashMap<>();
   Map<BubbleProxy,List<Movement>> cascaded = new HashMap<>();

   for (Map.Entry<BubbleProxy,List<Movement>> ent : overlapped.entrySet()) {
      List<BubbleProxy> adj = generateBubbleMoves(cur,ent.getKey(),ent.getValue());
      List<Movement> adjmv = getBubbleProxyMovements(adj);
      cascaded.put(ent.getKey(),adjmv);
      possibles.put(ent.getKey(),adj);
    }

   // this adds the movement of every bubble to every other bubble.  It's a way
   //	of creating group-like movements
   // It adds a lot to the combinatorical complexity, but it gets better results

   for (Map.Entry<BubbleProxy,List<BubbleProxy>> poss : possibles.entrySet()) {
      for (Map.Entry<BubbleProxy,List<Movement>> moves : cascaded.entrySet()) {
	 if (moves.getKey().getBubble() != poss.getKey().getBubble()) {
	    poss.getValue().addAll(moveBubble(poss.getKey(),moves.getValue()));
	  }
       }
    }

   return generateMoveSetsFromBubbleMoves(0,cur,new Adjustment(),possibles);
}




private Collection<Adjustment> generateMoveSetsFromBubbleMoves(
   int idx,
   Configuration cur,
   Adjustment changes,
   Map<BubbleProxy,List<BubbleProxy>> possibles)
{
   List<Adjustment> rslt = new ArrayList<Adjustment>();

   if (idx == possibles.size()) rslt.add(changes);
   else {
      int count = 0;
      for (Map.Entry<BubbleProxy,List<BubbleProxy>> choice : possibles.entrySet()) {
	 if (count++ == idx) {
	    for (BubbleProxy move : choice.getValue()) {
	       // skip movement if it will overlap a moved bubble
	       //   OR if it overlaps another bubble being moved
	       if (move.overlaps(cur.getBubbles(),true) ||
		      move.overlaps(changes.getMoved(),false)) continue;
	       Adjustment adj = new Adjustment(changes);
	       adj.add(move);
	       rslt.addAll(generateMoveSetsFromBubbleMoves(idx+1,cur,adj,possibles));
	     }
	  }
       }
    }

   return rslt;
}




private List<BubbleProxy> generateBubbleMoves(Configuration cur,BubbleProxy bubble,
						 List<Movement> tomove)
{
   List<BubbleProxy> movements = new ArrayList<BubbleProxy>();
   if (tomove.isEmpty()) return movements;

   Rectangle rct = null;
   Point dir = new Point(0,0);
   SortedMap<Double,BubbleProxy> best = new TreeMap<Double,BubbleProxy>();
   for (Movement mv : tomove) {
      if (rct == null) rct = new Rectangle(mv.getBounds());
      else rct = rct.union(mv.getBounds());
      dir.setLocation(Math.abs(dir.x) > Math.abs(mv.getX()) ? dir.x : mv.getX(),
			 Math.abs(dir.y) > Math.abs(mv.getY()) ? dir.y : mv.getY());
    }
   if (rct == null) return movements;

   Point tl = bubble.getPlacement().getLocation();
   Dimension sz = bubble.getPlacement().getSize();

   // right
   if (bubble.getBubble().getMovement().okRight() && dir.x >= 0) {
      int x0;
      if (dir.x == 0 || Math.abs(rct.x + rct.width - tl.x) < Math.abs(dir.x))
	 x0 = rct.x + rct.width;
      else
	 x0 = tl.x + dir.x;

      BubbleProxy bp2 = new BubbleProxy(bubble,new Rectangle(x0,tl.y,sz.width,sz.height),false);
      if (bubbleOnScreen(bp2)) {

	 double key = bp2.getMovementDistance();
	 while (best.containsKey(key)) key += 0.000001;
	 best.put(key,bp2);
       }
    }

   // down
   if (bubble.getBubble().getMovement().okDown() && dir.y >= 0) {
      int y0;
      if (dir.y == 0 || Math.abs(rct.y + rct.height - tl.y) < Math.abs(dir.y))
	 y0 = rct.y + rct.height;
      else
	 y0 = tl.y + dir.y;
      BubbleProxy bp2 = new BubbleProxy(bubble,new Rectangle(tl.x,y0,sz.width,sz.height),false);
      if (bubbleOnScreen(bp2)) {
	 double key = bp2.getMovementDistance();
	 while (best.containsKey(key)) key += 0.000001;
	 best.put(key,bp2);
       }
    }

   // left
   if (bubble.getBubble().getMovement().okLeft() && dir.x <= 0) {
      int x0;
      if (dir.x == 0 || Math.abs(rct.x - 1 - sz.width - tl.x) < Math.abs(dir.x))
	 x0 = rct.x - 1 - sz.width;
      else
	 x0 = tl.x + dir.x;

      BubbleProxy bp2 = new BubbleProxy(bubble,new Rectangle(x0,tl.y,sz.width,sz.height),false);
      if (bubbleOnScreen(bp2)) {
	 double key = bp2.getMovementDistance();
	 while (best.containsKey(key)) key += 0.000001;
	 best.put(key,bp2);
       }
    }

   // up
   if (bubble.getBubble().getMovement().okUp() && dir.y <= 0) {
      int y0;
      if (dir.y == 0 || Math.abs(tl.y - (rct.y - sz.height - 1)) < Math.abs(dir.y))
	 y0 = rct.y - sz.height - 1;
      else
	 y0 = tl.y + dir.y;
      BubbleProxy bp2 = new BubbleProxy(bubble,new Rectangle(tl.x,y0,sz.width,sz.height),false);
      if (bubbleOnScreen(bp2)) {
	 double key = bp2.getMovementDistance();
	 while (best.containsKey(key)) key += 0.000001;
	 best.put(key,bp2);
       }
    }

   // allow the bubbles to move freely after this in debug mode
   bubble.getBubble().setMovement(BudaMovement.ANY);

   // keep only two best moves
   for (BubbleProxy bp : best.values()) {
      if (movements.size() >= 2) break;
      movements.add(bp);
    }

   return movements;
}



private boolean bubbleOnScreen(BubbleProxy bp)
{
   Rectangle r = bp.getPlacement();
   Rectangle bnd = bubble_area.getBounds();

   if ((KEEP_BUBBLES_ON_SCREEN || DONT_GO_OFF_BOTTOM) && r.y + r.height > bnd.height)
      return false;
   if ((KEEP_BUBBLES_ON_SCREEN || DONT_GO_OFF_TOP) && r.y < 0)
      return false;
   if ((KEEP_BUBBLES_ON_SCREEN || DONT_GO_OFF_LEFT) && r.x < 0)
      return false;
   if ((KEEP_BUBBLES_ON_SCREEN || DONT_GO_OFF_RIGHT) && r.x + r.width > bnd.width)
      return false;

   return true;
}




private List<BubbleProxy> moveBubble(BubbleProxy bp,List<Movement> movements)
{
   List<BubbleProxy> rslt = new ArrayList<>();
   Rectangle r0 = bp.getPlacement();

   for (Movement m : movements) {
      Rectangle r = new Rectangle(r0.x + m.getX(),r0.y + m.getY(),r0.width,r0.height);
      rslt.add(new BubbleProxy(bp,r,bp.isMoved()));
    }

   return rslt;
}



private List<Movement> findMovementsForOverlappingBubble(BubbleProxy bp,List<Movement> mvmts)
{
   List<Movement> overlapped = new ArrayList<Movement>();
   Rectangle r0 = bp.getPlacement();

   for (Movement m : mvmts) {
      if (m.getBounds().intersects(r0)) overlapped.add(m);
    }

   return overlapped;
}




private List<Movement> getBubbleProxyMovements(Iterable<BubbleProxy> mvd)
{
   List<Movement> rslt = new ArrayList<Movement>();
   for (BubbleProxy bp : mvd) rslt.add(bp.getMovement());
   return rslt;
}



private Configuration evaluate(List<Configuration> cfgs)
{
   double bestv = -1;
   Configuration bestc = null;

   for (Configuration c : cfgs) {
      double score = c.getScore();
      if (bestv < 0 || score < bestv) {
	 bestv = score;
	 bestc = c;
       }
    }

   return bestc;
}




private static Rectangle getFullBounds(BudaBubble b)
{
   Rectangle place = b.getBounds();
   place.x -= BUBBLE_SPACE;
   place.y -= BUBBLE_SPACE;
   place.width += 2*BUBBLE_SPACE;
   place.height += 2*BUBBLE_SPACE;

   return place;
}




private static Rectangle getInnerBounds(Rectangle r)
{
   Rectangle place = new Rectangle(r);
   place.x += BUBBLE_SPACE;
   place.y += BUBBLE_SPACE;
   place.width -= 2*BUBBLE_SPACE;
   place.height -= 2*BUBBLE_SPACE;

   return place;
}




/********************************************************************************/
/*										*/
/*	Adjustment								*/
/*										*/
/********************************************************************************/

private static class Adjustment {

   private Collection<BubbleProxy> moved_bubbles;

   Adjustment() {
      moved_bubbles = new ArrayList<BubbleProxy>();
    }

   Adjustment(BubbleProxy bp) {
      this();
      moved_bubbles.add(bp);
    }

   Adjustment(Adjustment a) {
      this();
      for (BubbleProxy bp : a.getMoved()) {
	 moved_bubbles.add(new BubbleProxy(bp));
       }
    }

   boolean hasChanges() 		{ return moved_bubbles.size() > 0; }
   Iterable<BubbleProxy> getMoved()	{ return moved_bubbles; }

   void add(BubbleProxy bp)		{ moved_bubbles.add(bp); }

   boolean contains(BubbleProxy b) {
      for (BubbleProxy bp : moved_bubbles) {
	 if (bp.getBubble() == b.getBubble()) return true;
       }
      return false;
    }

   boolean hasOverlap() {
      for (BubbleProxy bp1 : moved_bubbles) {
	 for (BubbleProxy bp2 : moved_bubbles) {
	    if (bp1 != bp2 && bp1.getPlacement().intersects(bp2.getPlacement())) return true;
	  }
       }
      return false;
    }

}	// end of inner class Adjustment




/********************************************************************************/
/*										*/
/*	Movement								*/
/*										*/
/********************************************************************************/

private static class Movement {

   private Rectangle move_bounds;
   private Point     move_direction;

   Movement(Rectangle bnds,int x,int y) {
      move_bounds = new Rectangle(bnds);
      move_direction = new Point(x,y);
    }

   Rectangle getBounds()			{ return move_bounds; }
   int getX()					{ return move_direction.x; }
   int getY()					{ return move_direction.y; }

}	// end of inner class Movement




/********************************************************************************/
/*										*/
/*	Configuration								*/
/*										*/
/********************************************************************************/

private static class Configuration {

   private Map<BudaBubble,BubbleProxy>	bubble_set;
   private BudaBubbleArea bubble_area;

   Configuration(BudaBubbleArea ba) {
      bubble_area = ba;
      bubble_set = new HashMap<BudaBubble,BubbleProxy>();
      for (BudaBubble bb : ba.getBubbles()) {
         if (!bb.isFixed() && !bb.isFloating()) {
            bubble_set.put(bb,new BubbleProxy(bb));
          }
       }
    }

   Configuration(Configuration c) {
      bubble_area = c.bubble_area;
      bubble_set = new HashMap<>();
      for (BubbleProxy bp : c.bubble_set.values()) {
         BubbleProxy nbp = new BubbleProxy(bp);
         bubble_set.put(nbp.getBubble(),nbp);
       }
    }

   BubbleProxy getProxy(BudaBubble bb) {
      return bubble_set.get(bb);
    }

   double getScore() {
      double score = 0;
      for (BubbleProxy bp : bubble_set.values()) {
	 if (bp.isOffScreen(bubble_area.getHeight())) score += 1000000;
	 score += bp.getMovementScore();
       }
      return score;
    }

   void setMoved(BudaBubble bb) {
      BubbleProxy bp = getProxy(bb);
      if (bp != null) bp.setMoved();
    }

   Iterable<BubbleProxy> getBubbles()		{ return bubble_set.values(); }


}	// end of inner class Configuration




/********************************************************************************/
/*										*/
/*	Holder of information for a bubble during placement			*/
/*										*/
/********************************************************************************/

private static class BubbleProxy {

   private BudaBubble	for_bubble;
   private Rectangle	start_place;
   private Rectangle	curr_place;
   private boolean	is_moved;

   BubbleProxy(BudaBubble bb) {
      for_bubble = bb;
      start_place = getFullBounds(bb);
      curr_place = new Rectangle(start_place);
      is_moved = false;
    }

   BubbleProxy(BubbleProxy bp,Rectangle p,boolean mvd) {
      for_bubble = bp.for_bubble;
      start_place = bp.start_place;
      curr_place = new Rectangle(p);
      is_moved = mvd;
    }

   BubbleProxy(BubbleProxy bp) {
      for_bubble = bp.for_bubble;
      start_place = bp.start_place;
      curr_place = new Rectangle(bp.curr_place);
      is_moved = bp.is_moved;
    }

   BudaBubble getBubble()				{ return for_bubble; }

   boolean isOffScreen(int maxy) {
      return (curr_place.y + curr_place.height/2 > maxy);
    }

   double getMovementScore() {
      double x0 = start_place.x - curr_place.x;
      x0 *= x0;
      double y0 = start_place.y - curr_place.y;
      y0 *= y0;
      return Math.abs(x0) + Math.abs(y0);
    }

   double getMovementDistance() {
      return Point2D.distance(start_place.x,start_place.y,curr_place.x,curr_place.y);
    }

   void setMoved()					{ is_moved = true; }
   boolean isMoved()					{ return is_moved; }

   Rectangle getPlacement()				{ return curr_place; }
   void setPlacement(Rectangle r) {
      curr_place.setBounds(r);
      is_moved = true;
    }

   Movement getMovement() {
      Rectangle u = new Rectangle(start_place);
      u = u.union(curr_place);
      return new Movement(u,curr_place.x - start_place.x,curr_place.y - start_place.y);
    }

   boolean overlaps(Iterable<BubbleProxy> all,boolean movedonly) {
      for (BubbleProxy bp : all) {
	 if (bp.getBubble() != for_bubble && (!movedonly || bp.isMoved())) {
	    if (bp.getPlacement().intersects(curr_place)) return true;
	  }
       }
      return false;
    }

}	// end of inner class BubbleProxy





}	// end of class BudaSpacer




/* end of BudaSpacer.java */
