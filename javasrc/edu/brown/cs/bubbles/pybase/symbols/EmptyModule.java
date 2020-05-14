/********************************************************************************/
/*										*/
/*		EmptyModule.java						*/
/*										*/
/*	Python Bubbles Base empty module representation 			*/
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
 * Created on Dec 20, 2004
 *
 * @author Fabio Zadrozny
 */

package edu.brown.cs.bubbles.pybase.symbols;


import edu.brown.cs.bubbles.pybase.PybaseNature;

import org.python.pydev.core.ICompletionCache;

import java.io.File;


/**
 * @author Fabio Zadrozny
 */
public class EmptyModule extends AbstractModule {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private File		module_file;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public EmptyModule(String name,File f)
{
   super(name);
   module_file = f;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public File getFile()
{
   return module_file;
}


@Override public AbstractToken[] getWildImportedModules()
{
   throw new RuntimeException("Not intended to be called");
}


@Override public AbstractToken[] getTokenImportedModules()
{
   throw new RuntimeException("Not intended to be called");
}


@Override public AbstractToken[] getGlobalTokens()
{
   throw new RuntimeException("Not intended to be called");
}


@Override public String getDocString()
{
   throw new RuntimeException("Not intended to be called");
}


@Override public AbstractToken[] getGlobalTokens(CompletionState state,AbstractASTManager manager)
{
   throw new RuntimeException("Not intended to be called");
}


@Override public Definition[] findDefinition(CompletionState state,int line,int col,
	 PybaseNature nature) throws Exception
{
   throw new RuntimeException("Not intended to be called");
}


@Override public boolean isInDirectGlobalTokens(String tok,
	 ICompletionCache completionCache)
{
   throw new RuntimeException("Not intended to be called");
}


public boolean isInDirectImportTokens(String tok)
{
   throw new RuntimeException("Not implemented");
}




}	// end of class EmptyModule


/* end of EmptyModule.java */
