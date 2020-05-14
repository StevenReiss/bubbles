/********************************************************************************/
/*                                                                              */
/*              BrepairFaultAnnotation.java                                     */
/*                                                                              */
/*      Annotation showing location of a fault                                  */
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



package edu.brown.cs.bubbles.brepair;

import java.awt.Color;
import java.awt.Component;
import java.io.File;

import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleAnnotation;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleFileOverview;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaRoot;

class BrepairFaultAnnotation implements BaleAnnotation, BrepairConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BrepairFaultBubble for_bubble;
private Position     start_pos;
private BaleFileOverview for_document;

private File    for_file;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BrepairFaultAnnotation(BrepairFaultBubble bbl,File file,int line)
{
   for_bubble = bbl;
   for_file = file;
   for_document = BaleFactory.getFactory().getFileOverview(null,for_file,false);
   start_pos = null;
   int off = for_document.findLineOffset(line);
   try {
      start_pos = for_document.createPosition(off);
    }
   catch (BadLocationException e) { }
}



/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override public int getPriority()                      { return 20; }


@Override public Color getLineColor(BudaBubble bb) {
   return BoardColors.getColor("Brepair.AnnotColor"); 
}



@Override public boolean getForceVisible(BudaBubble bb)
{
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bb);
   BudaBubbleArea bba1 = BudaRoot.findBudaBubbleArea(for_bubble);
   if (bba != bba1) return false;
   return false;    
}



@Override public void addPopupButtons(Component c,JPopupMenu m)         { }


@Override public String getToolTip()
{
   return null;
}



@Override public Icon getIcon(BudaBubble arg0)
{
   return null;
}



@Override public File getFile()
{
   return for_file;
}

   

@Override public Color getBackgroundColor()
{
   return null;
}



@Override public int getDocumentOffset()
{
   return start_pos.getOffset();
}






}       // end of class BrepairFaultAnnotation




/* end of BrepairFaultAnnotation.java */

