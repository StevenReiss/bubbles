/********************************************************************************/
/*										*/
/*		BumpException.java						*/
/*										*/
/*	BUblles Mint Partnership exception class				*/
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


package edu.brown.cs.bubbles.bump;



/**
 *	Exception that indicates a problem during BUMP/ECLIPSE processing.
 **/

public class BumpException extends Exception implements BumpConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BumpException(String msg)
{
   super(msg);
}



BumpException(String msg,Throwable cause)
{
   super(msg,cause);
}



}	// end of class BumpException




/* end of BumpException.java */


