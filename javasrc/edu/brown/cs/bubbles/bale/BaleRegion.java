/********************************************************************************/
/*										*/
/*		BaleRegion.java 						*/
/*										*/
/*	Bubble Annotated Language Editor region definition			*/
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



class BaleRegion implements BaleConstants {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Position	start_position;
private Position	end_position;
private int		region_size;
private boolean 	includes_eol;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleRegion(Position start,Position end)
{
   this(start,end,false);
}



BaleRegion(Position start,Position end,boolean eol)
{
   start_position = start;
   end_position = end;
   region_size = -1;
   includes_eol = eol;
}



// Note that this constructor should only be used for fixed size regions, e.g.
// cursors, or for temporary regions that won't be used over edits.
// The end bound might not be maintained correctly.

BaleRegion(Position start,int len)
{
   start_position = start;
   end_position = null;
   region_size = len;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

Position getStartPosition()		{ return start_position; }
Position getEndPosition()		{ return end_position; }

boolean includesEol()			{ return includes_eol; }

int getStart()				{ return start_position.getOffset(); }

int getEnd()
{
   if (end_position != null) return end_position.getOffset();

   return start_position.getOffset() + region_size;
}


void reset(Position start,Position end)
{
   start_position = start;
   end_position = end;
   region_size = -1;
}


void reset(Position start,int len)
{
   start_position = start;
   end_position = null;
   region_size = len;
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   StringBuffer buf = new StringBuffer();
   buf.append("{");
   buf.append(start_position.toString());
   if (end_position != null) {
      buf.append("-");
      buf.append(end_position.toString());
    }
   else {
      buf.append("@");
      buf.append(region_size);
    }
   if (includes_eol) buf.append("&");
   buf.append("}");

   return buf.toString();
}



}	// end of class BaleRegion




/* end of BaleRegion.java */
