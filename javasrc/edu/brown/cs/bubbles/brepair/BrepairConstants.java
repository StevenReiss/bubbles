/********************************************************************************/
/*                                                                              */
/*              BrepairConstants.java                                           */
/*                                                                              */
/*      Constants for handling automatic bug repair in code bubbles             */
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



public interface BrepairConstants
{

   
/********************************************************************************/
/*                                                                              */
/*      Parameters for fault localization                                       */
/*                                                                              */
/********************************************************************************/

double CUTOFF_VALUE = 0.25;
int MAX_METHODS = 5;



/********************************************************************************/
/*                                                                              */
/*      Drawing Parameters                                                      */
/*                                                                              */
/********************************************************************************/

String BREPAIR_TOP_COLOR_ID = "Brepair.TopColor";
String BREPAIR_BOTTOM_COLOR_ID = "Brepair.BottomColor";


}       // end of interface BrepairConstants




/* end of BrepairConstants.java */

