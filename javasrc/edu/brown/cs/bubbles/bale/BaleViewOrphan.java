/********************************************************************************/
/*										*/
/*		BaleViewOrphan.java						*/
/*										*/
/*	Bubble Annotated Language Editor view for orphaned windows		*/
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bale;


import javax.swing.text.LabelView;
import javax.swing.text.Segment;



class BaleViewOrphan extends LabelView implements BaleConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		region_text = "< This bubble has been orphaned >";



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleViewOrphan(BaleElement e)
{
   super(e);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public Segment getText(int p0,int p1)
{
   char [] chrs = region_text.toCharArray();
   Segment s = new Segment(chrs,0,chrs.length);

   return s;
}





}	// end of class BaleViewOrphan




/* end of BaleViewOrphan.java */
