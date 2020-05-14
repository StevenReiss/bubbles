/********************************************************************************/
/*										*/
/*		BuenoLocationBump.java						*/
/*										*/
/*	BUbbles Environment New Objects creator location from Bump		*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.bueno;

import edu.brown.cs.bubbles.bump.BumpLocation;



class BuenoLocationBump extends BuenoLocation
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BumpLocation	bump_location;
private boolean 	is_before;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BuenoLocationBump(BumpLocation loc,boolean before)
{
   bump_location = loc;
   is_before = before;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getProject()
{
   return bump_location.getProject();
}



@Override public String getPackage()
{
   return null;
}



@Override public String getClassName()
{
   return null;
}



@Override public int getOffset()
{
   if (is_before) return -1;
   return -1;
}




}	// end of class BuenoLocaitonBump



/* end of BuenoLocationBump.java */
