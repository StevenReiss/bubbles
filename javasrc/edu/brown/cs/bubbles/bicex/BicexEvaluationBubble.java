/********************************************************************************/
/*                                                                              */
/*              BicexEvaluationBubble.java                                      */
/*                                                                              */
/*      description of class                                                    */
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

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bicex;

import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

import edu.brown.cs.bubbles.buda.BudaBubble;

class BicexEvaluationBubble extends BudaBubble implements BicexConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BicexEvaluationViewer   eval_viewer;


private static final long serialVersionUID = 1;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BicexEvaluationBubble(BicexEvaluationViewer bev)
{
   eval_viewer = bev;
   
   setContentPane(bev);
}



@Override protected void localDispose()
{
   if (eval_viewer != null) {
      eval_viewer.localDispose();
      eval_viewer = null;
    }
}



/********************************************************************************/
/*                                                                              */
/*      Menuing methods                                                         */
/*                                                                              */
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent evt)
{
   JPopupMenu menu = new JPopupMenu();
   
   eval_viewer.addToPopupMenu(evt,menu);
   
   menu.add(this.getFloatBubbleAction());
   
   menu.show(this,evt.getX(),evt.getY());
}



/********************************************************************************/
/*                                                                              */
/*      Methods to pass on to evaluation viewer                                 */
/*                                                                              */
/********************************************************************************/

void addPopupListener(BicexPopupCallback cb)
{
   if (eval_viewer != null) eval_viewer.addPopupListener(cb);
}


void removePopupListener(BicexPopupCallback cb)
{
   if (eval_viewer != null) eval_viewer.removePopupListener(cb);
}


BicexEvaluationContext findContext(String id,int sline,int eline)
{
   if (eval_viewer == null) return null;
   
   return eval_viewer.findContext(id,sline,eline);
}

BicexExecution getExecution()
{
   if (eval_viewer == null) return null;
   
   return eval_viewer.getExecution();
}




}       // end of class BicexEvaluationBubble




/* end of BicexEvaluationBubble.java */

