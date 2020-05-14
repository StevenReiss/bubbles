/********************************************************************************/
/*										*/
/*		BwizException.java						*/
/*										*/
/*	Exception for use in BWIZ package					*/
/*										*/
/********************************************************************************/
/*	Copyright 2013 Brown University -- Annalia Sunderland		      */
/*	Copyright 2013 Brown University -- Steven P. Reiss		      */
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

/* SVN: $Id$ */


package edu.brown.cs.bubbles.bwiz;




class BwizException extends Exception
{



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BwizException(String msg)
{
   super(msg);
}



BwizException(String msg,Throwable cause)
{
   super(msg,cause);
}




}	// end of class BwizException




/* end of BwizException.java */
