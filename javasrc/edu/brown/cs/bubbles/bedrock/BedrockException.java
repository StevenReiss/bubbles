/********************************************************************************/
/*										*/
/*		BedrockException.java						*/
/*										*/
/*	Generic exception for use with BEDROCK interface to eclipse		*/
/*										*/
/********************************************************************************/
/*	Copyright 2007 Brown University -- Steven P. Reiss		      */
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


/* RCS: $Header$ */


/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/



package edu.brown.cs.bubbles.bedrock;






public class BedrockException extends Exception {



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

public BedrockException(String msg)
{
   super(msg);
}



public BedrockException(String msg,Throwable cause)
{
   super(msg,cause);
}



}	// end of class BedrockException




/* end of BedrockException.java */
