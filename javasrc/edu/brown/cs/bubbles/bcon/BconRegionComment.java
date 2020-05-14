/********************************************************************************/
/*										*/
/*		BconRegionComment.java						*/
/*										*/
/*	Bubbles Environment Context Viewer internal region for a block comment	*/
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



package edu.brown.cs.bubbles.bcon;

import edu.brown.cs.bubbles.bale.BaleConstants;



class BconRegionComment extends BconRegion implements BconConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private boolean 	is_copyright;
private String		region_name;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BconRegionComment(BaleConstants.BaleFileOverview fov,int start,int end,String nm,boolean cpy)
{
   super(fov);

   is_copyright = cpy;

   if (nm == null) region_name = null;
   else region_name = "/* " + nm;

   setPosition(start,end);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override RegionType getRegionType()
{
   if (!isValid()) return RegionType.REGION_UNKNOWN;
   if (is_copyright) return RegionType.REGION_COPYRIGHT;

   return RegionType.REGION_COMMENT;
}



@Override String getRegionName()
{
   return region_name;
}


@Override boolean isComment()				{ return true; }





}	// end of class BconRegionComment




/* end of BconRegionComment.java */
