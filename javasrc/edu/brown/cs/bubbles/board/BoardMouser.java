/********************************************************************************/
/*                                                                              */
/*              BoardMouser.java                                                */
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



package edu.brown.cs.bubbles.board;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

public class BoardMouser extends MouseAdapter
{

@Override public void mouseDragged(MouseEvent e) {
   dispatchToParent(e);
}


@Override public void mouseMoved(MouseEvent e) {
   dispatchToParent(e);
}

@Override public void mouseWheelMoved(MouseWheelEvent e) {
   dispatchToParent(e);
}

@Override public void mouseClicked(MouseEvent e) {
   dispatchToParent(e);
}

@Override public void mouseEntered(MouseEvent e) {
   dispatchToParent(e);
}

@Override public void mouseExited(MouseEvent e) {
   dispatchToParent(e);
}

@Override public void mousePressed(MouseEvent e) {
   dispatchToParent(e);
}

@Override public void mouseReleased(MouseEvent e) {
   dispatchToParent(e);
}



/********************************************************************************/
/*                                                                              */
/*      Dispatch even to parent                                                 */
/*                                                                              */
/********************************************************************************/

public void dispatchToParent(MouseEvent e) {
   Component c = e.getComponent();
   if (c != null) {
      Component c1 = c.getParent();
      if (c1 != null) c1.dispatchEvent(e);
    }
}



}       // end of class BoardMouser




/* end of BoardMouser.java */

