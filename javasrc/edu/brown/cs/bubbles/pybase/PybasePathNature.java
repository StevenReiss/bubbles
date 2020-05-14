/********************************************************************************/
/*										*/
/*		PybasePathNature.java						*/
/*										*/
/*	Python Bubbles Base implementation of a PythonPathNature		*/
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
 * Created on Jun 2, 2005
 *
 * @author Fabio Zadrozny
 */


package edu.brown.cs.bubbles.pybase;

import edu.brown.cs.bubbles.pybase.symbols.AbstractASTManager;
import edu.brown.cs.bubbles.pybase.symbols.AbstractInterpreterManager;
import edu.brown.cs.bubbles.pybase.symbols.ModulesManager;
import edu.brown.cs.bubbles.pybase.symbols.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author Fabio Zadrozny
 */

public class PybasePathNature {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private volatile PybaseProject	 the_project;
private volatile PybaseNature	  the_nature;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

PybasePathNature(PybaseProject proj,PybaseNature nat)
{
   the_project = proj;
   the_nature = nat;
}



public void setProject(PybaseProject project,PybaseNature nature)
{
   the_project = project;
   the_nature = nature;
}


public PybaseNature getNature()
{
   return the_nature;
}


/**
 * Returns a list of paths with the complete pythonpath for this nature.
 *
 * This includes the pythonpath for the project, all the referenced projects and the
 * system.
 */
public List<String> getCompleteProjectPythonPath(PybaseInterpreter interpreter,
							      AbstractInterpreterManager manager)
{
   ModulesManager projectModulesManager = getProjectModulesManager();
   if (projectModulesManager == null) return null;
   return projectModulesManager.getCompletePythonPath(interpreter, manager);
}


private ModulesManager getProjectModulesManager()
{
   PybaseNature nature = the_nature;
   if (nature == null) return null;

   AbstractASTManager astManager = nature.getAstManager();
   if (astManager == null) return null;

   return astManager.getModulesManager();
}


public Set<String> getProjectSourcePathSet()
{
   String projectSourcePath;
   PybaseNature nature = the_nature;
   if (nature == null) return new HashSet<String>();
   projectSourcePath = getProjectSourcePath();

   return new HashSet<String>(StringUtils.splitAndRemoveEmptyTrimmed(projectSourcePath,'|'));
}



public String getProjectSourcePath()
{
   if (the_project == null) return "";

   return the_project.getProjectSourcePath();
}


public Map<String, String> getVariableSubstitution()
{
   return getVariableSubstitution(true);
}


public Map<String, String> getVariableSubstitution(boolean addinterpretersubs)
{
   Map<String, String> rslt = new HashMap<String, String>();

   return rslt;
}



}	// end of class PybasePathNature




/* end of PybasePathNature.java */
