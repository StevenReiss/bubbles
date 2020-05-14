/********************************************************************************/
/*										*/
/*		PybaseDebugVariableCollection.java				*/
/*										*/
/*	Handle debugging container variable/value for Bubbles from Python	*/
/*										*/
/********************************************************************************/
/*	Copyright 2006 Brown University -- Steven P. Reiss		      */
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
/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.pybase.debug;



public class PybaseDebugVariableCollection extends PybaseDebugVariable {



/********************************************************************************/
/*										*/
/*	Local storage								*/
/*										*/
/********************************************************************************/




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

PybaseDebugVariableCollection(PybaseDebugTarget tgt,String nm,String typ,String val,String loc)
{
   super(tgt,nm,typ,val,loc);
}




}	// end of class PybaseDebugVariableCollection




/* end of PybaseDebugVariableCollection.java */
