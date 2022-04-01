/********************************************************************************/
/*                                                                              */
/*              BaleViewHintBox.java                                            */
/*                                                                              */
/*      BoxView to contain hint                                                 */
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



package edu.brown.cs.bubbles.bale;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.BoxView;
import javax.swing.text.Position;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import edu.brown.cs.bubbles.board.BoardLog;


class BaleViewHintBox extends BoxView implements BaleConstants
{


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BaleViewHintBox(BaleElement be)
{
   super(be,View.X_AXIS);
}


/********************************************************************************/
/*                                                                              */
/*      Handle 0 length elements                                                */
/*                                                                              */
/********************************************************************************/

@Override protected void forwardUpdate(DocumentEvent.ElementChange ec,
      DocumentEvent e,Shape a,ViewFactory f)
{
   boolean wasValid = isLayoutValid(View.X_AXIS);
   
   super.forwardUpdate(ec,e,null,f);    // do update, no repaint
   
   if (wasValid && !isLayoutValid(View.X_AXIS) && a != null) {
      Component c = getContainer();
      Rectangle alloc = getInsideAllocation(a);
      if (c != null && alloc != null) {
         c.repaint(alloc.x, alloc.y, alloc.width, alloc.height);
       }
    }
}



@Override public int viewToModel(float x,float y,Shape a,Position.Bias[] bias)
{
   int pos = super.viewToModel(x,y,a,bias);
   
   View v0  = getViewAtPoint((int) x,(int) y,(Rectangle) a);
   if (v0 instanceof BaleViewHint) {
      int npos = -1;
      boolean fnd = false;
      for (int i = 0; i < getViewCount(); ++i) {
         View v = getView(i);
         if (v instanceof BaleViewHint) {
            if (v == v0) {
               if (npos > 0) {
                  pos = npos;
                  break;
                }
               else fnd = true;
             }
          }
         else {
            if (fnd) {
               pos = v.getElement().getStartOffset();
               break;
             }
            npos = v.getElement().getEndOffset();
          }
       }
    }
   
   BoardLog.logD("BALE","POSITION " + x + " " + y + " " + pos);
   
   return pos;
}


@Override public Shape modelToView(int pos,Shape a,Position.Bias b)
        throws BadLocationException
{
   Shape s = super.modelToView(pos,a,b);
   
   return s;
}


@Override protected int getViewIndexAtPosition(int pos) 
{
   for (int i = 0; i < getViewCount(); ++i) {
      View v = getView(i);
      if (v instanceof BaleViewHint) ;
      else return i;
    }
   
   return super.getViewIndexAtPosition(pos);
}


@Override public int getResizeWeight(int axis) 
{
   return 0;
}


@Override public float getMaximumSpan(int axis)
{
    return getPreferredSpan(axis);
}



}       // end of class BaleViewHintBox




/* end of BaleViewHintBox.java */

