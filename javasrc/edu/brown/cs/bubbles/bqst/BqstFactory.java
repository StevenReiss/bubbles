/********************************************************************************/
/*										*/
/*		BqstFactory.java						*/
/*										*/
/*	Bubbles questions and forms factory					*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Yu Li			      */
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

package edu.brown.cs.bubbles.bqst;

import javax.swing.JFrame;


/**
 * Factory for question forms
 **/

public class BqstFactory implements BqstConstants {



/********************************************************************************/
/*										*/
/*	Factory methods 							*/
/*										*/
/********************************************************************************/

/**
 * Returns a new form panel
 */

public static BqstPanel createBqstPanel(JFrame root,String formtitle)
{
   return new BqstPanel(root,formtitle);
}



}	// end of class BqstFactory




/* end of BqstFactory.java */


