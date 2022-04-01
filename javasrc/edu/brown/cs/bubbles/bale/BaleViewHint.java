/********************************************************************************/
/*                                                                              */
/*              BaleViewHint.java                                               */
/*                                                                              */
/*      LabelView for a hint                                                    */
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

import javax.swing.text.LabelView;
import javax.swing.text.Segment;

class BaleViewHint extends LabelView implements BaleConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BaleViewHint(BaleElement e) 
{
   super(e);
}

/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public Segment getText(int p0,int p1)
{
   char [] chrs = ((BaleElement) getElement()).getHintText().toCharArray();
   Segment s = new Segment(chrs,0,chrs.length);
   return s;
}



}       // end of class BaleViewHint




/* end of BaleViewHint.java */

