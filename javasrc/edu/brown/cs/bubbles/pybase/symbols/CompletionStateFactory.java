/********************************************************************************/
/*										*/
/*		CompletionStateFactory.java					*/
/*										*/
/*	Python Bubbles Base completion state representation factory		*/
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

import edu.brown.cs.bubbles.pybase.PybaseNature;

import org.python.pydev.core.ICompletionCache;


public class CompletionStateFactory {

/**
 * @return a default completion state for globals (empty act. token)
 */
public static CompletionState getEmptyCompletionState(PybaseNature nature,
	 ICompletionCache completionCache)
{
   return new CompletionState(-1,-1,"",nature,"",completionCache);
}

/**
 * @return a default completion state for globals (act token defined)
 */
public static CompletionState getEmptyCompletionState(String token,PybaseNature nature,
	 ICompletionCache completionCache)
{
   return new CompletionState(-1,-1,token,nature,"",completionCache);
}

/**
 * @param line: start at 0
 * @param col: start at 0
 * @return a default completion state for globals (act token defined)
 */
public static CompletionState getEmptyCompletionState(String token,PybaseNature nature,
	 int line,int col,ICompletionCache completionCache)
{
   return new CompletionState(line,col,token,nature,"",completionCache);
}

} // end of class CompletionStateFactory


/* end of CompletionStateFactory.java */
