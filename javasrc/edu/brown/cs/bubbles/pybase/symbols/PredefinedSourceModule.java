/********************************************************************************/
/*										*/
/*		PredefinedSourceModule.java					*/
/*										*/
/*	Python Bubbles Base representation of a predefined source module	*/
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

import edu.brown.cs.bubbles.pybase.PybaseMessage;

import org.python.pydev.parser.jython.SimpleNode;

import java.io.File;
import java.util.List;


public class PredefinedSourceModule extends SourceModule {

public PredefinedSourceModule(String name,File f,SimpleNode n,List<PybaseMessage> parseError)
{
   super(name,f,n,parseError);
}

}
