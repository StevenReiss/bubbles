/********************************************************************************/
/*										*/
/*		AssignDefinition.java						*/
/*										*/
/*	Python Bubbles Base assignment definition representation		*/
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
/*
 * Created on Jan 19, 2005
 *
 * @author Fabio Zadrozny
 */

package edu.brown.cs.bubbles.pybase.symbols;

import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.exprType;


public class AssignDefinition extends Definition {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

public final String   target_name;

/**
 * This is the position in the target.
 *
 * e.g. if we have:
 *
 * a, b = someCall()
 *
 * and we're looking for b, target pos would be 1
 * if we were looking for a, target pos would be 0
 */
public final int      target_pos;

/**
 * Determines that a 'global' was added for the target before this assign
 */
public boolean	found_as_global;

/**
 * This is the value node found (can be used later to determine if it's a
 * Call or some regular attribute.
 */
public final exprType node_value;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public AssignDefinition(String value,String target,int targetPos,Assign ast,int line,
	 int col,LocalScope scope,AbstractModule module,exprType nodeValue)
{
   super(line,col,value,ast,scope,module);
   target_name = target;
   target_pos = targetPos;
   node_value = nodeValue;
}




} // end of class AssignDefinition


/* end of AssignDefinition.java */
