/********************************************************************************/
/*                                                                              */
/*              BudaHistory.java                                                */
/*                                                                              */
/*      Maintain a history of bubble actions to allow undo/redo                 */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
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



package edu.brown.cs.bubbles.buda;

import java.awt.Rectangle;
import java.util.LinkedList;

import javax.swing.SwingUtilities;

class BudaHistory implements BudaConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private LinkedList<BudaHistoryEvent>  event_stack;
private int                     stack_pointer;
private BudaBubbleArea          bubble_area;
private boolean                 doing_action;
private int                     nest_depth;
private int                     event_count;
private int                     total_count;

private static int              group_counter = 0;

private static final int        MAX_SIZE = 10;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BudaHistory(BudaBubbleArea bba)
{
   event_stack = new LinkedList<>();
   stack_pointer = 0;
   bubble_area = bba;
   group_counter = 0;
   nest_depth = 0;
   event_count = 0;
   total_count = 0;
   doing_action = false;
}



/********************************************************************************/
/*                                                                              */
/*      Add Event operations                                                    */
/*                                                                              */
/********************************************************************************/

void addBubbleAddEvent(BudaBubble bb)
{ 
   BudaHistoryEvent evt = new BubbleAddEvent(bb);
   addEvent(evt);
}


void addBubbleRemoveEvent(BudaBubble bb)
{
   BudaHistoryEvent evt = new BubbleRemoveEvent(bb);
   addEvent(evt);
}


void addBubbleShapeEvent(BudaBubble bb,Rectangle pos)
{ 
   Rectangle npos = bb.getBounds();
   if (npos.equals(pos)) return;
   
   BudaHistoryEvent evt = new BubbleShapeEvent(bb,pos,npos);
   addEvent(evt);
}


void addBubbleFloatingEvent(BudaBubble bb)
{ }


void addGroupRemoveEvent(BudaBubbleGroup bg)
{ }


void addGroupRestoreEvent(BudaBubbleGroup bg)
{ }


void addGroupMoveEvent(BudaBubbleGroup bg)
{ }


void addLinkAddEvent(BudaBubbleLink bl)
{ }


void addLinkRemoveEvent(BudaBubbleLink bl)
{ }


void addWorkingSetAddEvent(BudaWorkingSet bws)
{ }


void addWorkingSetRemoveEvent(BudaWorkingSet bws)
{ }


void addWorkingSetResizeEvent(BudaWorkingSet bws)
{ }


void addMoveViewportEvent(Rectangle pre,Rectangle post)
{ 
   if (pre == null || post == null || pre.equals(post)) return;
   
   BudaHistoryEvent evt = new ViewportMoveEvent(pre,post);
   addEvent(evt);
}


/********************************************************************************/
/*                                                                              */
/*      Begin-End operations                                                    */
/*                                                                              */
/********************************************************************************/

void begin()
{
   if (doing_action) return;
   
   BudaHistoryEvent evt = BudaHistoryEvent.createStartEvent();
   ++nest_depth;
   addEvent(evt);
}



void end() {
   SwingUtilities.invokeLater(new EndAction());
}

private void localEnd() 
{
   if (nest_depth == 0) return;
   
   ++total_count;
   if (doing_action) return;
   
   int depth = 0;
   boolean haveevt = false;
   for (int i = stack_pointer-1; i >= 0; --i) {
      BudaHistoryEvent hevt = event_stack.get(i);
      if (hevt.isGroupStart()) {
         if (depth <= 0) {
            --nest_depth; 
            if (haveevt) {
               BudaHistoryEvent evt = hevt.getEndEvent();
               addEvent(evt);
             }
            else {
               for (int j = stack_pointer-1; j >= i; --j) {
                  event_stack.remove(j);
                  --stack_pointer;
                }
             }
            break;
          }
         else --depth;
       }
      else if (hevt.getGroupId() > 0) {
         ++depth;
       }
      else haveevt = true;
    }
   
  
}



void clear()
{
   event_stack.clear();
   stack_pointer = 0;
   nest_depth = 0;
   event_count = 0;
   doing_action = false;
}


private final class EndAction implements Runnable {

   private int base_count;

   EndAction() {
      base_count = total_count;
    }
   
   @Override public void run() {
      if (base_count == total_count) {
         localEnd();
       }
      else {
         base_count = total_count;
         SwingUtilities.invokeLater(this);
       }
    }

}       // end of inner class FinishedAction




/********************************************************************************/
/*                                                                              */
/*      Undo/redo operations                                                    */
/*                                                                              */
/********************************************************************************/

void undo()
{
   if (doing_action) return;
   
   doing_action = true;
   try {
      BudaHistoryEvent end = null;
      if (stack_pointer <= 0) return;
      BudaHistoryEvent evt = event_stack.get(stack_pointer-1);
      int id = evt.getGroupId();
      if (id > 0 && !evt.isGroupStart()) {
         end = evt.getStartEvent();
         --stack_pointer;
         --event_count;
       }
      
      while (stack_pointer > 0) {
         evt = event_stack.get(--stack_pointer);
         evt.undo(bubble_area);
         if (end == null || evt == end) break;
       } 
    }
   finally {
      SwingUtilities.invokeLater(new FinishedAction());
    }
}




void redo()
{
   if (doing_action) return;
   
   doing_action = true;
   try {
      BudaHistoryEvent end = null;
      if (stack_pointer >= event_stack.size()) return;
      BudaHistoryEvent evt = event_stack.get(stack_pointer);
      int id = evt.getGroupId();
      if (id > 0 && evt.isGroupStart()) {
         end = evt.getEndEvent();
       }
      
      while (stack_pointer < event_stack.size()) {
         evt = event_stack.get(stack_pointer++);
         evt.redo(bubble_area);
         if (end == null || evt == end) break;
       }
    }
   finally {
      SwingUtilities.invokeLater(new FinishedAction());
    }
}


private final class FinishedAction implements Runnable {
   
   private int base_count;
   
   FinishedAction() {
      base_count = total_count;
    }
   
   @Override public void run() {
      if (base_count == total_count) {
         doing_action = false;
       }
      else {
         base_count = total_count;
         SwingUtilities.invokeLater(this);
       }
    }
   
}       // end of inner class FinishedAction




/********************************************************************************/
/*                                                                              */
/*      Utility methods                                                         */
/*                                                                              */
/********************************************************************************/

private void addEvent(BudaHistoryEvent evt)
{
   ++total_count;
         
   if (doing_action) return;
   
   while (event_stack.size() > stack_pointer) {
      event_stack.removeLast();
    }
   
   if (nest_depth == 0) {
      ++event_count;
      while (event_count >= MAX_SIZE) removeFirst();
    }
   
   event_stack.add(evt);
   ++stack_pointer;
}


private void removeFirst()
{ 
   if (stack_pointer <= 0) {
      clear();
      return;
    }
   
   BudaHistoryEvent evt = event_stack.removeFirst();
   --stack_pointer;
   if (evt.isGroupStart()) {
      BudaHistoryEvent eevt = evt.getEndEvent();
      while (stack_pointer > 0 && !event_stack.isEmpty()) {
         BudaHistoryEvent xevt = event_stack.removeFirst();
         --stack_pointer;
         if (xevt == eevt) break;
       }
    }
   --event_count;
}



/********************************************************************************/
/*                                                                              */
/*      BudaHistoryEvent -- generic history event                               */
/*                                                                              */
/********************************************************************************/

abstract static class BudaHistoryEvent {
   
   protected abstract void undo(BudaBubbleArea bba);
   protected abstract void redo(BudaBubbleArea bba);
   
   protected int getGroupId()                   { return 0; }
   protected boolean isGroupStart()             { return false; }
   protected BudaHistoryEvent getEndEvent()     { return null; }
   protected BudaHistoryEvent getStartEvent()   { return null; }
   
   static BudaHistoryEvent createStartEvent()   { return new GroupStartEvent(); }
   
}       // end of abstract inner class BudaHistoryEvent



/********************************************************************************/
/*                                                                              */
/*      Group Event -- start/end of a group                                     */
/*                                                                              */
/********************************************************************************/

private static class GroupStartEvent extends BudaHistoryEvent {
   
   private int group_id;
   private BudaHistoryEvent end_event;
   
   GroupStartEvent() {
      group_id = ++group_counter;
      end_event = null;
    }
   
   @Override protected BudaHistoryEvent getEndEvent() {
      if (end_event == null) {
         end_event = new GroupEndEvent(this); 
       }
      return end_event;
    }
   
   @Override protected int getGroupId()         { return group_id; }
   @Override protected boolean isGroupStart()   { return true; }
   
   @Override protected void undo(BudaBubbleArea bba) { }
   
   @Override protected void redo(BudaBubbleArea bba) { }
   
   @Override public String toString() {
      return "Group Start " + group_id;
    }
   
}       // end of inner class GroupStartEvent


private static final class GroupEndEvent extends BudaHistoryEvent {
   
   private int group_id;
   private GroupStartEvent start_event;
   
   private GroupEndEvent(GroupStartEvent start) {
      group_id = start.group_id;
      start_event = start;
    }
   
   @Override protected BudaHistoryEvent getStartEvent() {
      return start_event;
    }
   
   @Override protected int getGroupId()         { return group_id; }
   
   @Override protected void undo(BudaBubbleArea bba) { }
   
   @Override protected void redo(BudaBubbleArea bba) { }
   
   @Override public String toString() {
      return "Group End " + group_id;
    }
   
}       // end of inner class GroupEndEvent



/********************************************************************************/
/*                                                                              */
/*      Bubble-Related history events                                           */
/*                                                                              */
/********************************************************************************/

private abstract static class BubbleEvent extends BudaHistoryEvent {
   
   private BudaBubble for_bubble;
   
   protected BubbleEvent(BudaBubble bb) {
      for_bubble = bb;
    }
   
   protected BudaBubble getBubble()             { return for_bubble; }
   
}       // end of inner class BubbleEvent



private static class BubbleAddEvent extends BubbleEvent {
   
   BubbleAddEvent(BudaBubble bb) {
      super(bb);
    }
   
   @Override protected void undo(BudaBubbleArea bba) {
      getBubble().setVisible(false);
    }
   
   @Override protected void redo(BudaBubbleArea bba) {
      getBubble().setVisible(true);
    }
   
}       // end of inner class BubbleAddEvent



private static class BubbleRemoveEvent extends BubbleEvent {

   BubbleRemoveEvent(BudaBubble bb) {
      super(bb);
    }
   
   @Override protected void undo(BudaBubbleArea bba) {
      getBubble().setVisible(true);
    }
   
   @Override protected void redo(BudaBubbleArea bba) {
      getBubble().setVisible(false);
    }

}       // end of inner class BubbleAddEvent



private static class BubbleShapeEvent extends BubbleEvent {
    
   private Rectangle pre_bounds;
   private Rectangle post_bounds;
   
   BubbleShapeEvent(BudaBubble bb,Rectangle pre,Rectangle post) {
      super(bb);
      pre_bounds = new Rectangle(pre);
      post_bounds = new Rectangle(post);
    }
   
   @Override protected void undo(BudaBubbleArea bba) {
      getBubble().setBounds(pre_bounds);
    }
   
   @Override protected void redo(BudaBubbleArea bba) {
      getBubble().setBounds(post_bounds);
    }
   
}       // end of inner class BubbleShapeEvent


private static class ViewportMoveEvent extends BudaHistoryEvent {
   
   private Rectangle pre_bounds;
   private Rectangle post_bounds;
   
   ViewportMoveEvent(Rectangle pre,Rectangle post) {
      pre_bounds = pre;
      post_bounds = post;
    }
   
   @Override protected void undo(BudaBubbleArea bba) {
      bba.resetViewport(pre_bounds);
    }
   
   @Override protected void redo(BudaBubbleArea bba) {
      bba.resetViewport(post_bounds);
    }
   
}       // end of inner class ViewportMoveEvent



}       // end of class BudaHistory




/* end of BudaHistory.java */

