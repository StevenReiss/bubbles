/********************************************************************************/
/*										*/
/*		CompletionCache.java						*/
/*										*/
/*	Python Bubbles Base completion cache representation			*/
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

import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.cache.CacheMapWrapper;
import org.python.pydev.core.cache.LRUMap;


/**
 * Default completion cache implementation
 *
 * @author Fabio
 */
public final class CompletionCache extends CacheMapWrapper<Object, Object> implements ICompletionCache {

public CompletionCache()
{
   super(new LRUMap<Object, Object>(200));
}



} // end of class CompletionCache



/* end of CompletionCache.java */
