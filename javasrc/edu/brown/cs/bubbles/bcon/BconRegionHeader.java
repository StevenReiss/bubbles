/********************************************************************************/
/*										*/
/*		BconRegionHeader.java						*/
/*										*/
/*	Bubbles Environment Context Viewer internal region for a header 	*/
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



class BconRegionHeader extends BconRegion implements BconConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private String		region_name;
private RegionType	region_type;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BconRegionHeader(BaleConstants.BaleFileOverview fov,int start,int end,String nm,RegionType rt)
{
   super(fov);

   region_name = nm;
   region_type = rt;

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

   return region_type;
}



@Override String getRegionName()
{
   return region_name;
}




}	// end of class BconRegionHeader




/* end of BconRegionHeader.java */
