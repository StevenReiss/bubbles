/********************************************************************************/
/*										*/
/*		EmptyModuleForZip.java						*/
/*										*/
/*	Python Bubbles Base empty module from a zip file			*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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

package edu.brown.cs.bubbles.pybase.symbols;

import java.io.File;


/**
 * An empty module representing a path in a zip file.
 *
 * @author Fabio
 */
public class EmptyModuleForZip extends EmptyModule {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String	      path_in_zip;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public EmptyModuleForZip(String name,File f,String pathInZip,boolean isFile)
{
   super(name,f);
   path_in_zip = pathInZip;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getPath()			{ return path_in_zip; }



}	// end of class EmptyModuleForZip


/* end of EmptyModuleForZip.java */
