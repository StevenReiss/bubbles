/********************************************************************************/
/*										*/
/*		BudaGroupSpacer.java						*/
/*										*/
/*	BUblles Display Area group spacer					*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss, Andrew Bragdon, Hsu-Sheng Ko */
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

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;


class BudaGroupSpacer implements BudaConstants {

/********************************************************************************/
/*										*/
/*	Private Storage 					*/
/*										*/
/********************************************************************************/

private BudaBubbleArea		bubble_area;
private Configuration		start_config;

private static final int	BUBBLE_SPACE = (int) Math.ceil(GROUP_AURA_BUFFER);

private static final long	MAX_SPACING_TIME = 1500;

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

BudaGroupSpacer(BudaBubbleArea ba)
{
   bubble_area = ba;

   start_config = new Configuration(ba);
}

BudaGroupSpacer(BudaBubbleArea ba,BudaBubbleGroup fix)
{
   this(ba);

   start_config.setMoved(fix);
}

/********************************************************************************/
/*										*/
/*	Execution methods							*/
/*										*/
/********************************************************************************/

void makeRoomFor(BudaBubbleGroup bg)
{
   Configuration start = new Configuration(start_config);
   List<Configuration> cfgs = new ArrayList<Configuration>();

   Adjustment adj = new Adjustment(new GroupProxy(bg));

   cfgs.addAll(makeAMove(start,adj,0,System.currentTimeMillis()));

   Configuration best = evaluate(cfgs);

   if (best != null) {
      for (GroupProxy gp : best.getBubbleGroups()) {
	 if (gp.isMoved()) {
	    Movement movement = gp.getMovement();

	    bubble_area.removeMovingBubbles(gp.bubble_group.getBubbles());

	    for (BudaBubble bb : gp.bubble_group.getBubbles()) {
	       Point destination = bb.getLocation();
	       destination.x += movement.getX();
	       destination.y += movement.getY();

	       bubble_area.moveBubble(bb, destination, false);
	     }
	  }
       }
    }
}

private Collection<Configuration> makeAMove(Configuration cur,Adjustment mvd,int recurse,long when)
{
   List<Configuration> rslt = new ArrayList<Configuration>();

   if (!mvd.hasChanges()) {
      rslt.add(cur);
    }
   else {
      for (GroupProxy gp : mvd.getMoved()) {
	 GroupProxy cpb = cur.getProxy(gp.getBubbleGroup());
	 cpb.setPlacement(gp.getPlacement());
       }
      for (Adjustment adj : generateMoveSets(cur,mvd)) {
	 rslt.addAll(makeAMove(new Configuration(cur),adj,recurse+1,when));
	 long ts = System.currentTimeMillis() - when;
	 if (ts > MAX_SPACING_TIME && ts < 10000) break;      // avoid debugging problems
       }
    }

   return rslt;
}

private Collection<Adjustment> generateMoveSets(Configuration cur,Adjustment mvd)
{
   // bubbles overlap : this is a failed adjustment
   if (mvd.hasOverlap()){
      return new ArrayList<Adjustment>();
    }

   Map<GroupProxy,List<GroupProxy>> overlapped = new HashMap<GroupProxy,List<GroupProxy>>();

   // go through each bubble group to see if it is in the path of a bubble that is being moved.
   // If a bubble is in the path and that bubble has already been moved, then this can't
   //    be a valid configuration so we return
   // Otherwise, we collect all the moved bubble that overlap bubbles yet to be moved.
   for (GroupProxy gp : cur.getBubbleGroups()) {

      if (!mvd.contains(gp)) {
	 // bp overlaps with something in moved
	 List<GroupProxy> ovl = this.findOverlappingBubbleGroup(gp, mvd.getMoved());

	 if (ovl.size() > 0) {
	    if (gp.isMoved()) {
	       if (gp.overlaps(cur.getBubbleGroups(),true)) {
		  // this adjustment is invalid -- it generates a cycle of movements
		  return new ArrayList<Adjustment>();
		}
	     }
	    else overlapped.put(gp,ovl);
	  }
       }
    }

   // now we generate moved bubbles for each bubble that are yet to be moved

   Map<GroupProxy,List<GroupProxy>> possibles = new HashMap<GroupProxy,List<GroupProxy>>();
   Map<GroupProxy,List<Movement>> cascaded = new HashMap<GroupProxy,List<Movement>>();

   for (Map.Entry<GroupProxy,List<GroupProxy>> ent : overlapped.entrySet()) {
      List<GroupProxy> adj = generateBubbleMoves(cur,ent.getKey(),ent.getValue());
      List<Movement> adjmv = getBubbleProxyMovements(adj);
      cascaded.put(ent.getKey(),adjmv);
      possibles.put(ent.getKey(),adj);
    }

   for (Map.Entry<GroupProxy,List<GroupProxy>> poss : possibles.entrySet()) {
      for (Map.Entry<GroupProxy,List<Movement>> moves : cascaded.entrySet()) {
	 if (moves.getKey().getBubbleGroup() != poss.getKey().getBubbleGroup()) {
	    poss.getValue().addAll(moveBubbleGroup(poss.getKey(),moves.getValue(), mvd.getMoved()));
	  }
       }
    }

   return generateMoveSetsFromBubbleMoves(0,cur,new Adjustment(),possibles);
}

private Collection<Adjustment> generateMoveSetsFromBubbleMoves(
   int idx,
   Configuration cur,
   Adjustment changes,
   Map<GroupProxy,List<GroupProxy>> possibles)
{
   List<Adjustment> rslt = new ArrayList<Adjustment>();

   if (idx == possibles.size())
	   rslt.add(changes);
   else {
      int count = 0;
      for (Map.Entry<GroupProxy,List<GroupProxy>> choice : possibles.entrySet()) {
	 if (count++ == idx) {
	    for (GroupProxy move : choice.getValue()) {
	       // skip movement if it will overlap a moved bubble
	       //   OR if it overlaps another bubble being moved
	       if (move.overlaps(cur.getBubbleGroups(),true) ||
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

private List<GroupProxy> generateBubbleMoves(Configuration cur,GroupProxy group,
					     List<GroupProxy> gpList)
{
   List<GroupProxy> movements = new ArrayList<GroupProxy>();
   if (gpList.isEmpty()) return movements;

   Point dir = new Point(0,0);
   SortedMap<Double,GroupProxy> best = new TreeMap<Double,GroupProxy>();

   int offestright = 0;
   int offestleft = 0;
   int offestdown = 0;
   int offestup = 0;

   for (GroupProxy gp2 : gpList){
      Movement mv = gp2.getMovement();
      dir.setLocation(Math.abs(dir.x) > Math.abs(mv.getX()) ? dir.x : mv.getX(),
			 Math.abs(dir.y) > Math.abs(mv.getY()) ? dir.y : mv.getY());

      Rectangle unionRect = group.getPlacement().union(gp2.getPlacement());

      if (mv.getX() >= 0){
	 Area movingArea = adjustAreaForMovement(new Point(1, 0), unionRect, group);
	 Area movedArea = adjustAreaForMovement(new Point(-1, 0), unionRect, gp2);

	 movingArea.intersect(movedArea);

	 int currentOffset = getMaxOffestX(movingArea);

	 if (currentOffset > offestright)
	    offestright = currentOffset;
      }

      if (mv.getX() <= 0){
	 Area movingArea = adjustAreaForMovement(new Point(-1, 0), unionRect, group);
	 Area movedArea = adjustAreaForMovement(new Point(1, 0), unionRect, gp2);

	 movingArea.intersect(movedArea);

	 int currentOffset = getMaxOffestX(movingArea);

	 if (currentOffset > offestleft)
		 offestleft = currentOffset;
       }

      if (mv.getY() >= 0){
	 Area movingArea = adjustAreaForMovement(new Point(0, 1), unionRect, group);
	 Area movedArea = adjustAreaForMovement(new Point(0, -1), unionRect, gp2);

	 movingArea.intersect(movedArea);

	 int currentOffset = getMaxOffestY(movingArea);

	 if (currentOffset > offestdown)
		 offestdown = currentOffset;
       }
      if (mv.getY() <= 0){
	 Area movingArea = adjustAreaForMovement(new Point(0, -1), unionRect, group);
	 Area movedArea = adjustAreaForMovement(new Point(0, 1), unionRect, gp2);

	 movingArea.intersect(movedArea);

	 int currentOffset = getMaxOffestY(movingArea);

	 if (currentOffset > offestup)
	    offestup = currentOffset;
       }
    }

   Point tl = group.getPlacement().getLocation();
   Dimension sz = group.getPlacement().getSize();

   // right
   if (dir.x >= 0) {
      int x0 = tl.x + offestright;

      GroupProxy gp2 = new GroupProxy(group,new Rectangle(x0,tl.y,sz.width,sz.height),false);

      if (bubbleOnScreen(gp2)) {
	 double key = gp2.getMovementDistance();

	 while (best.containsKey(key)) key += 0.000001;
	    best.put(key,gp2);
       }
    }

   // down
   if (dir.y >= 0) {
      int y0 = tl.y + offestdown;

      GroupProxy gp2 = new GroupProxy(group,new Rectangle(tl.x,y0,sz.width,sz.height),false);

      if (bubbleOnScreen(gp2)) {
	 double key = gp2.getMovementDistance();

	 while (best.containsKey(key)) key += 0.000001;
	    best.put(key,gp2);
       }
    }

   // left
   if (dir.x <= 0) {
      int x0 = tl.x - offestleft;

      GroupProxy gp2 = new GroupProxy(group,new Rectangle(x0,tl.y,sz.width,sz.height),false);

      if (bubbleOnScreen(gp2)) {
	 double key = gp2.getMovementDistance();

	 while (best.containsKey(key)) key += 0.000001;
	    best.put(key,gp2);
       }
    }

   // up
   if (dir.y <= 0) {
      int y0 = tl.y - offestup;

      GroupProxy gp2 = new GroupProxy(group,new Rectangle(tl.x,y0,sz.width,sz.height),false);

      if (bubbleOnScreen(gp2)) {
	 double key = gp2.getMovementDistance();

	 while (best.containsKey(key)) key += 0.000001;
	    best.put(key,gp2);
       }
    }

   // allow the bubbles to move freely after this in debug mode
   //bubble.getBubble().setMovement(BudaMovement.ANY);

   // keep only two best moves
   for (GroupProxy gp : best.values()) {
      if (movements.size() >= 2) break;
      movements.add(gp);
    }

   return movements;
}

private Area adjustAreaForMovement(Point dir, Rectangle bounds, GroupProxy gp){
   Area resultArea = new Area();
   Movement mv = gp.getMovement();

   if (dir.x > 0) {
      for (BudaBubble bb : gp.getBubbleGroup().getBubbles()){
	 Rectangle rect = getFullBounds(bb);

	 rect.x += mv.getX();
	 rect.y += mv.getY();

	 if (rect.x <= (bounds.x + bounds.width)){
	    rect.width = (bounds.x + bounds.width) - rect.x;

	    resultArea.add(new Area(rect));
	  }
       }
    }
else if (dir.x < 0) {
      for (BudaBubble bb : gp.getBubbleGroup().getBubbles()){
	 Rectangle rect = getFullBounds(bb);

	 rect.x += mv.getX();
	 rect.y += mv.getY();

	 if (rect.x <= (bounds.x + bounds.width)){
	    rect.width = (rect.x + rect.width) - bounds.x;
	    rect.x = bounds.x;

	    resultArea.add(new Area(rect));
	  }
	}
     }
else if (dir.y > 0) {
	for (BudaBubble bb : gp.getBubbleGroup().getBubbles()){
	   Rectangle rect = getFullBounds(bb);

	   rect.x += mv.getX();
	   rect.y += mv.getY();

	   if (rect.y <= (bounds.y + bounds.height)){
	      rect.height = (bounds.y + bounds.height) - rect.y;

	      resultArea.add(new Area(rect));
	    }
	}
     }
else if (dir.y < 0) {
       for (BudaBubble bb : gp.getBubbleGroup().getBubbles()){
	  Rectangle rect = getFullBounds(bb);

	  rect.x += mv.getX();
	  rect.y += mv.getY();

	  if (rect.y <= (bounds.y + bounds.height)){
	     rect.height = (rect.y + rect.height) - bounds.y;
	     rect.y = bounds.y;

	     resultArea.add(new Area(rect));
	   }
	}
     }

   return resultArea;
}

private int getMaxOffestX(Area area){
   Set<Integer> keys = new HashSet<Integer>();

   List<BoundEntry> boundEntryList = new ArrayList<BoundEntry>();

   float[] pos = new float[2];
   Point prevPoint = new Point();
   Point startPoint = new Point();

   for (PathIterator i = area.getPathIterator(null); !i.isDone(); i.next()) {
      int type = i.currentSegment(pos);
      int x = (int) pos[0];
      int y = (int) pos[1];

      switch (type) {
	 case PathIterator.SEG_MOVETO:
	    prevPoint = new Point(x, y);
	    startPoint = prevPoint;
	    keys.add(y);

	    break;
	 case PathIterator.SEG_LINETO:
	    if (x == prevPoint.x){
	       int lowerBound = Math.min(prevPoint.y, y);
	       int upperBound = Math.max(prevPoint.y, y);

	       BoundEntry be = new BoundEntry(lowerBound, upperBound, x);
	       boundEntryList.add(be);
	     }

	    prevPoint = new Point(x, y);

	    keys.add(y);

	    break;

	 case PathIterator.SEG_CLOSE:
	    if (x == startPoint.x){
	       int lowerBound = Math.min(startPoint.y, y);
	       int upperBound = Math.max(startPoint.y, y);

	       BoundEntry be = new BoundEntry(lowerBound, upperBound, x);
	       boundEntryList.add(be);
	     }

	    keys.add(y);

	    break;
       }
    }

   int maxOffset = 0;

   for (Integer key : keys){
      int[] bounds = {Integer.MAX_VALUE, Integer.MIN_VALUE};

      for (BoundEntry be : boundEntryList){
	 if (be.isInRange(key.intValue())){
	    if (be.getValue() > bounds[1])
	       bounds[1] = be.getValue();
	    if (be.getValue() < bounds[0])
	       bounds[0] = be.getValue();
	  }
       }

      maxOffset = Math.max(maxOffset, bounds[1] - bounds[0]);
    }

   return maxOffset;
}

private int getMaxOffestY(Area area){
   Set<Integer> keys = new HashSet<Integer>();

   List<BoundEntry> boundEntryList = new ArrayList<BoundEntry>();

   float[] pos = new float[2];
   Point prevPoint = new Point();
   Point startPoint = new Point();

   for (PathIterator i = area.getPathIterator(null); !i.isDone(); i.next()) {
      int type = i.currentSegment(pos);
      int x = (int) pos[0];
      int y = (int) pos[1];

      switch (type) {
	 case PathIterator.SEG_MOVETO:
	    prevPoint = new Point(x, y);
	    startPoint = prevPoint;
	    keys.add(x);

	    break;
	 case PathIterator.SEG_LINETO:
	    if (y == prevPoint.y){
	       int lowerBound = Math.min(prevPoint.x, x);
	       int upperBound = Math.max(prevPoint.x, x);

	       BoundEntry be = new BoundEntry(lowerBound, upperBound, y);
	       boundEntryList.add(be);
	     }

	    prevPoint = new Point(x, y);

	    keys.add(x);

	    break;
	 case PathIterator.SEG_CLOSE:
	    if (y == startPoint.y){
	       int lowerBound = Math.min(startPoint.x, x);
	       int upperBound = Math.max(startPoint.x, x);

	       BoundEntry be = new BoundEntry(lowerBound, upperBound, y);
	       boundEntryList.add(be);
	     }

	    keys.add(x);

	    break;
       }
    }

   int maxOffset = 0;

   for (Integer key : keys){
      int[] bounds = {Integer.MAX_VALUE, Integer.MIN_VALUE};

      for (BoundEntry be : boundEntryList){
	 if (be.isInRange(key.intValue())){
	    if (be.getValue() > bounds[1])
	       bounds[1] = be.getValue();
	    if (be.getValue() < bounds[0])
	       bounds[0] = be.getValue();
	  }
       }

      maxOffset = Math.max(maxOffset, bounds[1] - bounds[0]);
    }

   return maxOffset;
}

private boolean bubbleOnScreen(GroupProxy gp)
{
   Rectangle r = gp.getPlacement();
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

private List<GroupProxy> moveBubbleGroup(GroupProxy gp,List<Movement> movements, Iterable<GroupProxy> moved)
{
   List<GroupProxy> rslt = new ArrayList<GroupProxy>();
   Rectangle r0 = gp.getPlacement();

   for (Movement m : movements) {
      Rectangle r = new Rectangle(r0.x + m.getX(),r0.y + m.getY(),r0.width,r0.height);

      GroupProxy newGp = new GroupProxy(gp,r,gp.isMoved());

      if (!newGp.overlaps(moved, false))
	 rslt.add(newGp);
    }

   return rslt;
}

private List<GroupProxy> findOverlappingBubbleGroup(GroupProxy gp, Iterable<GroupProxy> gpList)
{
   List<GroupProxy> overlapped = new ArrayList<GroupProxy>();

   for (GroupProxy gp2 : gpList) {
      Area area = gp.getArea();
      if (area == null) continue;
      area.intersect(gp2.getArea());

      if (!area.isEmpty()) overlapped.add(gp2);
    }

   return overlapped;
}

private List<Movement> getBubbleProxyMovements(Iterable<GroupProxy> mvd)
{
   List<Movement> rslt = new ArrayList<Movement>();
   for (GroupProxy gp : mvd) rslt.add(gp.getMovement());
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

private static Rectangle getFullBounds(BudaBubbleGroup bg)
{
   if (bg.getShape() == null) {
      return new Rectangle(0,0,0,0);
    }

   Rectangle place = bg.getShape().getBounds();
   place.x -= BUBBLE_SPACE;
   place.y -= BUBBLE_SPACE;
   place.width += 2*BUBBLE_SPACE;
   place.height += 2*BUBBLE_SPACE;

   return place;
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


/********************************************************************************/
/*										*/
/*	BoundEntry								*/
/*										*/
/********************************************************************************/

private static class BoundEntry {

   private int lower_bound;
   private int upper_bound;
   private int bound_value;

   BoundEntry(int lowerBound, int upperBound, int value){
      lower_bound = lowerBound;
      upper_bound = upperBound;
      bound_value = value;
    }

   public boolean isInRange(int key){
      return (key >= lower_bound && key <= upper_bound);
    }

   public int getValue(){
      return bound_value;
    }

}	// end of inner class BoundEntry




/********************************************************************************/
/*										*/
/*	Adjustment								*/
/*										*/
/********************************************************************************/

private static class Adjustment {

   private Collection<GroupProxy> moved_bubble_groups;

   Adjustment() {
      moved_bubble_groups = new ArrayList<GroupProxy>();
    }

   Adjustment(GroupProxy bp) {
      this();
      moved_bubble_groups.add(bp);
    }

   Adjustment(Adjustment a) {
      this();
      for (GroupProxy bp : a.getMoved()) {
	 moved_bubble_groups.add(new GroupProxy(bp));
       }
    }

   boolean hasChanges() 		{ return moved_bubble_groups.size() > 0; }
   Iterable<GroupProxy> getMoved()	{ return moved_bubble_groups; }

   void add(GroupProxy gp)		{ moved_bubble_groups.add(gp); }

   boolean contains(GroupProxy g) {
      for (GroupProxy gp : moved_bubble_groups) {
	 if (gp.getBubbleGroup() == g.getBubbleGroup()) return true;
       }
      return false;
    }

   boolean hasOverlap() {
      for (GroupProxy gp1 : moved_bubble_groups) {
	 for (GroupProxy gp2 : moved_bubble_groups) {
	    if (gp1 != gp2 && gp1.overlaps(gp2)) return true;
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

   private Point move_direction;

   Movement(int x,int y) {
      move_direction = new Point(x,y);
    }

   int getX()					{ return move_direction.x; }
   int getY()					{ return move_direction.y; }

}	// end of inner class Movement




/********************************************************************************/
/*										*/
/*	Configuration								*/
/*										*/
/********************************************************************************/

private static class Configuration {

   private Map<BudaBubbleGroup, GroupProxy> group_set;
   private BudaBubbleArea bubble_area;

   Configuration(BudaBubbleArea ba) {
      bubble_area = ba;
      group_set = new HashMap<>();
      for (BudaBubbleGroup bg : ba.getBubbleGroups()) {
         group_set.put(bg, new GroupProxy(bg));
       }
    }

   Configuration(Configuration c) {
      bubble_area = c.bubble_area;
      group_set = new HashMap<>();
      for (GroupProxy gp : c.group_set.values()) {
          GroupProxy ngp = new GroupProxy(gp);
          group_set.put(ngp.getBubbleGroup(), ngp);
       }
    }

   GroupProxy getProxy(BudaBubbleGroup bg) {
      return group_set.get(bg);
    }

   double getScore() {
      double score = 0;
      for (GroupProxy bg : group_set.values()) {
	 if (bg.isOffScreen(bubble_area.getHeight())) score += 1000000;
	    score += bg.getMovementScore();
       }
      return score;
    }

   void setMoved(BudaBubbleGroup bg) {
      GroupProxy gp = getProxy(bg);
      if (gp != null) gp.setMoved();
    }

   Iterable<GroupProxy> getBubbleGroups()		{ return group_set.values(); }
}	// end of inner class Configuration




/********************************************************************************/
/*										*/
/*	Holder of information for a bubble during placement			*/
/*										*/
/********************************************************************************/

private static class GroupProxy {

   private BudaBubbleGroup	bubble_group;
   private Rectangle	start_place;
   private Rectangle	curr_place;
   private boolean	is_moved;

   GroupProxy(BudaBubbleGroup bg) {
      bubble_group = bg;
      start_place = getFullBounds(bg);
      curr_place = new Rectangle(start_place);
      is_moved = false;
    }

   GroupProxy(GroupProxy bp,Rectangle p,boolean mvd) {
      bubble_group = bp.bubble_group;
      start_place = bp.start_place;
      curr_place = new Rectangle(p);
      is_moved = mvd;
    }

   GroupProxy(GroupProxy bp) {
      bubble_group = bp.bubble_group;
      start_place = bp.start_place;
      curr_place = new Rectangle(bp.curr_place);
      is_moved = bp.is_moved;
    }

   BudaBubbleGroup getBubbleGroup()				{ return bubble_group; }

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

   Area getArea(){
      Movement move = getMovement();
      Area a = bubble_group.getShape();
      if (a == null) return null;

      return a.createTransformedArea(AffineTransform.getTranslateInstance(move.getX(), move.getY()));
    }

   void setPlacement(Rectangle r) {
      curr_place.setBounds(r);
      is_moved = true;
    }

   Movement getMovement() {
      return new Movement(curr_place.x - start_place.x,curr_place.y - start_place.y);
    }

   boolean overlaps(GroupProxy gp) {
      Area intersectArea = gp.getArea();
      Area thisArea = getArea();

      if (!intersectArea.getBounds().intersects(thisArea.getBounds()))
	 return false;

      intersectArea.intersect(thisArea);

      if (!intersectArea.isEmpty()) {
	 return true;
       }
      return false;
    }

   boolean overlaps(Iterable<GroupProxy> all,boolean movedonly) {
      Area thisArea = getArea();

      for (GroupProxy gp : all) {
	 if (gp == this)
	    continue;

	 Area intersectArea = gp.getArea();

	 if ((movedonly && !gp.isMoved()) || !intersectArea.getBounds().intersects(thisArea.getBounds()))
	    continue;

	 intersectArea.intersect(thisArea);
	 if (!intersectArea.isEmpty()) {
	    return true;
	  }
       }
      return false;
    }

}	// end of inner class GroupProxy

}	// end of class BudaGroupSpacer



/* end of BudaGroupSpacer.java */
