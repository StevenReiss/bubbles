/********************************************************************************/
/*										*/
/*		BedrockStartup.java						*/
/*										*/
/*	Handle eclipse startup actions						*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss, Hsu-Sheng Ko      */
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


import org.eclipse.ui.IStartup;



public class BedrockStartup implements IStartup, BedrockConstants
{


/********************************************************************************/
/*										*/
/*	Startup callback from IStartup						*/
/*										*/
/*	This will be effective once we switch our libraries to the newer	*/
/*	versions of Eclipse.  Then it should have @Override			*/
/*										*/
/********************************************************************************/

public void earlyStartup()
{
   BedrockPlugin.logI("Startup called");
}



}	// end of class BedrockStatup



/* end of BedrockStartup.java */
