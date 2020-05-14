/********************************************************************************/
/*										*/
/*		AssignCompletionInfo.java					*/
/*										*/
/*	Python Bubbles Base assignment information structure			*/
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

import java.util.ArrayList;


public class AssignCompletionInfo {

public final ArrayList<AbstractToken> possible_completions;
public final Definition[]      given_defs;

public AssignCompletionInfo(Definition[] defs,ArrayList<AbstractToken> ret)
{
   given_defs = defs;
   possible_completions = ret;
}




} // end of class AssignCompletionInfo


/* end of AssignCompletionInfo.java */
