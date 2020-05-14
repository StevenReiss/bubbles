/********************************************************************************/
/*										*/
/*		BaleSimpleRegion.java						*/
/*										*/
/*	Bubble Annotated Language Editor simple region definition		*/
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



class BaleSimpleRegion implements BaleConstants {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private int		start_offset;
private int		region_size;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleSimpleRegion(int offset,int len)
{
   start_offset = offset;
   region_size = len;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

int getStart()				{ return start_offset; }
int getEnd()				{ return start_offset + region_size; }
int getLength() 			{ return region_size; }





}	// end of class BaleSimpleRegion




/* end of BaleSimpleRegion.java */
