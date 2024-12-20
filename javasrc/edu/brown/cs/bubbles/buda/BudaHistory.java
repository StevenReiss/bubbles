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

import java.util.ArrayList;
import java.util.List;

class BudaHistory implements BudaConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<BudaHistoryEvent>  event_stack;
private int                     stack_pointer;
private BudaBubbleArea          bubble_area;
private boolean                 doing_action;

private static int              group_counter = 0;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BudaHistory(BudaBubbleArea bba)
{
   event_stack = new ArrayList<>();
   stack_pointer = 0;
   bubble_area = bba;
   group_counter = 0;
   doing_action = false;
}



/********************************************************************************/
/*                                                                              */
/*      Operations on the history                                               */
/*                                                                              */
/********************************************************************************/

void begin()
{
   BudaHistoryEvent evt = BudaHistoryEvent.createStartEvent();
   addEvent(evt);
}



void end() 
{
   int depth = 0;
   for (int i = group_counter-1; i >= 0; --i) {
      BudaHistoryEvent hevt = event_stack.get(i);
      if (hevt.isGroupStart()) {
         if (depth <= 0) {
            BudaHistoryEvent evt = hevt.getEndEvent();
            addEvent(evt);
            break;
          }
         else --depth;
       }
      else if (hevt.getGroupId() > 0) {
         ++depth;
       }
    }
}



private void addEvent(BudaHistoryEvent evt)
{
   if (doing_action) return;
   
   while (event_stack.size() > stack_pointer) {
      event_stack.removeLast();
    }
   
   event_stack.add(evt);
   ++stack_pointer;
}


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
       }
      
      while (stack_pointer > 0) {
         evt = event_stack.get(--stack_pointer);
         evt.undo(bubble_area);
         if (end == null || evt == end) break;
       } 
    }
   finally {
      doing_action = false;
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
      doing_action = false; 
    }
}



void clear()
{
   event_stack.clear();
   stack_pointer = 0;
   doing_action = false;
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
   
   static BudaHistoryEvent createStartEvent()   { return new GroupEvent(); }
   
   
   
   
}       // end of abstract inner class BudaHistoryEvent


/********************************************************************************/
/*                                                                              */
/*      Group Event -- start/end of a group                                     */
/*                                                                              */
/********************************************************************************/

private static class GroupEvent extends BudaHistoryEvent {

   private int group_id;
   private GroupEvent start_event;
   private GroupEvent end_event;
   
   GroupEvent() {
      group_id = ++group_counter;
      start_event = null;
      end_event = null;
    }
   
   private GroupEvent(GroupEvent start) {
      group_id = start.group_id;
      start_event = start;
    }
   
   @Override protected BudaHistoryEvent getEndEvent() {
      if (end_event == null) {
         end_event = new GroupEvent(this);
       }
      return end_event;
    }
   
   @Override protected BudaHistoryEvent getStartEvent() {
      return start_event;
    }
   
   @Override protected int getGroupId()         { return group_id; }
   @Override protected boolean isGroupStart()   { return start_event == null; }
   
   @Override protected void undo(BudaBubbleArea bba) { }
   
   @Override protected void redo(BudaBubbleArea bba) { }
   
}



}       // end of class BudaHistory




/* end of BudaHistory.java */

