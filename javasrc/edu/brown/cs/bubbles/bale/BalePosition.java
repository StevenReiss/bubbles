/********************************************************************************/
/*										*/
/*		BalePosition.java						*/
/*										*/
/*	Bubble Annotated Language Editor position implementation		*/
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


import javax.swing.text.Position;



class BalePosition implements Position, BaleConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected Position	base_position;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BalePosition(Position p)
{
   base_position = p;
}


protected BalePosition()
{
   base_position = null;
}




/********************************************************************************/
/*										*/
/*	Access methodes 							*/
/*										*/
/********************************************************************************/

@Override public int getOffset()
{
   return base_position.getOffset();
}


void reset(Position p)
{
   base_position = p;
}


int getDocumentOffset()
{
   return getOffset();
}



void savePosition(BaleDocument bd)
{
   base_position = bd.savePosition(base_position);
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   return "[" + getOffset() + "]";
}



}	// end of class BalePosition




/* end of BalePosition.java */


