/********************************************************************************/
/*										*/
/*		SystemASTManager.java						*/
/*										*/
/*	Python Bubbles Base AST manager for system modules			*/
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


import edu.brown.cs.bubbles.pybase.PybaseInterpreter;
import edu.brown.cs.bubbles.pybase.PybaseNature;
import edu.brown.cs.bubbles.pybase.PybaseProject;

import org.eclipse.jface.text.IDocument;

import java.io.File;


public class SystemASTManager extends AbstractASTManager {

public SystemASTManager(AbstractInterpreterManager manager,PybaseNature nature,
	 PybaseInterpreter info)
{
   modulesManager = info.getModulesManager();
   setNature(nature);
}

@Override public void setProject(PybaseProject project,PybaseNature nature,boolean restoreDeltas)
{
   throw new RuntimeException("Not implemented");
}

@Override public void rebuildModule(File file,IDocument doc,PybaseProject project,
	 PybaseNature nature)
{
   throw new RuntimeException("Not implemented");
}

@Override public void removeModule(File file,PybaseProject project)
{
   throw new RuntimeException("Not implemented");
}

@Override public void changePythonPath(String pythonpath,PybaseProject project)
{
   throw new RuntimeException("Not implemented");
}

@Override public void saveToFile(File astOutputFile)
{
   throw new RuntimeException("Not implemented");
}

}
