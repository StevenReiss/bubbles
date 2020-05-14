/********************************************************************************/
/*										*/
/*		BowiTaskManager.java						*/
/*										*/
/*	Task manager								*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Ian Strickman		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 * This program and the accompanying materials are made available under the	 *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at								 *
 *	http://www.eclipse.org/legal/epl-v10.html				 *
 *										 *
 ********************************************************************************/

package edu.brown.cs.bubbles.bowi;

import edu.brown.cs.bubbles.buda.BudaRoot;


class BowiTaskManager {

   
/********************************************************************************/
/*                                                                              */
/*      Private storage                                                         */
/*                                                                              */
/********************************************************************************/

private BudaRoot buda_root;
private int wait_cursor;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BowiTaskManager(BudaRoot br) 
{
   buda_root = br;
   wait_cursor = 0;
}



/********************************************************************************/
/*                                                                              */
/*      Action methods                                                          */
/*                                                                              */
/********************************************************************************/

void startTask() 
{
   if (wait_cursor++ == 0) buda_root.startWaitCursor();
}



void stopTask()
{
   if (wait_cursor == 0) return;
   wait_cursor--;
   if (wait_cursor == 0) buda_root.stopWaitCursor();
}




}       // end of class BowiTaskManager
