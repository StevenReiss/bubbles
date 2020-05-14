/********************************************************************************/
/*										*/
/*		ASTManager.java 						*/
/*										*/
/*	Python Bubbles Base user code AST manager				*/
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
 * Created on Nov 9, 2004
 *
 * @author Fabio Zadrozny
 */

package edu.brown.cs.bubbles.pybase.symbols;

import edu.brown.cs.bubbles.pybase.PybaseNature;
import edu.brown.cs.bubbles.pybase.PybaseProject;

import org.eclipse.jface.text.IDocument;

import java.io.File;
import java.io.IOException;



public final class ASTManager extends AbstractASTManager {


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public ASTManager()			{}



/********************************************************************************/
/*										*/
/*	Project management methods						*/
/*										*/
/********************************************************************************/

@Override public void setProject(PybaseProject project,PybaseNature nature,boolean restoredeltas)
{
   getProjectModulesManager().setProject(project, nature, restoredeltas);
}



/********************************************************************************/
/*										*/
/*	Modules manager methods 						*/
/*										*/
/********************************************************************************/

@Override public ModulesManager getModulesManager()
{
   return getProjectModulesManager();
}



private synchronized ProjectModulesManager getProjectModulesManager()
{
   if (modulesManager == null) {
      modulesManager = new ProjectModulesManager();
   }
   return (ProjectModulesManager) modulesManager;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public void changePythonPath(String pythonpath,final PybaseProject project)
{
   getProjectModulesManager().changePythonPath(pythonpath, project);
}



@Override public void rebuildModule(File f,IDocument doc,final PybaseProject project,
	 PybaseNature nature)
{
   getProjectModulesManager().rebuildModule(f, doc, project, nature);
}



@Override public void removeModule(File file,PybaseProject project)
{
   getProjectModulesManager().removeModule(file, project);
}



public int getSize()
{
   return getProjectModulesManager().getSize(true);
}



/********************************************************************************/
/*										*/
/*	I/O methods								*/
/*										*/
/********************************************************************************/

@Override public void saveToFile(File astoutputfile)
{
   modulesManager.saveToFile(astoutputfile);
}



public static ASTManager loadFromFile(File astoutputfile)
	 throws IOException
{
   ASTManager astmanager = new ASTManager();
   ProjectModulesManager projectmodulesmanager = new ProjectModulesManager();
   ModulesManager.loadFromFile(projectmodulesmanager, astoutputfile);
   astmanager.modulesManager = projectmodulesmanager;
   return astmanager;
}



}	// end of class ASTManager



/* end of ASTManager.java */
