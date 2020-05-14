/********************************************************************************/
/*										*/
/*		BudaConstraint.java						*/
/*										*/
/*	BUblles Display Area bubble panel constraint for adding new bubbles	*/
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


package edu.brown.cs.bubbles.buda;

import java.awt.Point;


/**
 *	This class represents the information needed to place a bubble in the
 *	bubble region.	The position type indicates whether the bubble floats in
 *	the viewport of exists on the background as well as whether the bubble
 *	can be moved by the bubble spacer.  The user can provide initial x,y
 *	position for the bubble.  Without this, the bubble can be placed anywhere
 *	but is likely to be placed at the current mouse position.
 **/

public class BudaConstraint implements BudaConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BudaBubblePosition pos_type;	// how to interpret position
private Point	pos_location;		// location in bubble area




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

/**
 *	Constraint for movable bubble at the given position in the bubble area.
 **/

public BudaConstraint(int x,int y)
{
   this(BudaBubblePosition.MOVABLE,new Point(x,y));
}



/**
 *	Constraint for movable bubble at the given position in the bubble area.
 **/

public BudaConstraint(Point pt)
{
   this(BudaBubblePosition.MOVABLE,pt);
}



/**
 *	Constraint for a bubble of the given movability at the default position.
 **/

public BudaConstraint(BudaBubblePosition pos)
{
   this(pos,0,0);
}



/**
 *	Constraint for a bubble of the given movability at the given position
 **/

public BudaConstraint(BudaBubblePosition pos,int x,int y)
{
   this(pos,new Point(x,y));
}



/**
 *	Constraint for a bubble of the given movability at the givne position.
 **/

public BudaConstraint(BudaBubblePosition pos,Point loc)
{
   pos_type = pos;
   pos_location = new Point(loc);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BudaBubblePosition getPositionType()		{ return pos_type; }
Point getLocation()				{ return pos_location; }




}	// end of class BudaConstraint




/* end of BudaConstraint.java */
