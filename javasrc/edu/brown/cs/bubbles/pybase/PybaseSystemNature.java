/********************************************************************************/
/*										*/
/*		PybaseSystemNature.java 					*/
/*										*/
/*	Python Bubbles Base global nature implementation			*/
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
package edu.brown.cs.bubbles.pybase;

import edu.brown.cs.bubbles.pybase.symbols.AbstractASTManager;
import edu.brown.cs.bubbles.pybase.symbols.AbstractInterpreterManager;
import edu.brown.cs.bubbles.pybase.symbols.AbstractModule;
import edu.brown.cs.bubbles.pybase.symbols.AbstractToken;
import edu.brown.cs.bubbles.pybase.symbols.SystemASTManager;

import java.io.File;


/**
 * This nature is used only as a 'last resort', if we're unable to link a given resource to
 * a project (and thus, we don't have project-related completions and we don't know with what
 * exactly we're dealing with: it's usually only used for external files)
 */
public class PybaseSystemNature extends PybaseNature {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private final AbstractInterpreterManager system_manager;
public final PybaseInterpreter the_interpreter;
private SystemASTManager system_ast_manager;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public PybaseSystemNature(AbstractInterpreterManager manager)
{
   this(manager, manager.getDefaultInterpreterInfo(false));
}



public PybaseSystemNature(AbstractInterpreterManager manager, PybaseInterpreter info)
{
   super(PybaseMain.getPybaseMain());

   this.the_interpreter = info;
   this.system_manager = manager;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public boolean isResourceInPythonpathProjectSources(String resource, boolean addExternal)
{
   return super.isResourceInPythonpath(resource); //no source folders in the system nature (just treat it as default)
}


@Override public String resolveModuleOnlyInProjectSources(String fileAbsolutePath, boolean addExternal)
{
   return super.resolveModule(new File(fileAbsolutePath));
}


@Override public String getVersion()
{
   if (this.the_interpreter != null) {
      String version = this.the_interpreter.getVersion();
      if (version != null && version.startsWith("3")) {
	 switch(this.system_manager.getInterpreterType()) {
	    case PYTHON:
	       return "python 3.0";
	    case JYTHON:
	       return "jython 3.0";
	    case IRONPYTHON:
	       return "ironpython 3.0";
	    default:
	       throw new RuntimeException("Not python nor jython nor iron python?");
	  }
       }
    }
   switch(this.system_manager.getInterpreterType()) {
      case PYTHON:
	 return "python 2.7";
      case JYTHON:
	 return "jython 2.7";
      case IRONPYTHON:
	 return "ironpython 2.7";
      default:
	 throw new RuntimeException("Not python nor jython nor iron python?");
    }
}



@Override public String getDefaultVersion()
{
   return getVersion();
}


@Override public void setVersion(String version, String interpreter)
{
   throw new RuntimeException("Not Implemented: the system nature is read-only.");
}


@Override public PybaseInterpreterType getInterpreterType()
{
   return this.system_manager.getInterpreterType();
}


@Override public File getCompletionsCacheDir()
{
   throw new RuntimeException("Not Implemented");
}


@Override public void saveAstManager()
{
   throw new RuntimeException("Not Implemented: system nature is only transient.");
}


@Override public PybasePathNature getPythonPathNature()
{
   throw new RuntimeException("Not Implemented");
}


@Override public String resolveModule(String file)
{
   if (the_interpreter == null) {
      return null;
    }
   return the_interpreter.getModulesManager().resolveModule(file);
}


@Override public AbstractASTManager getAstManager()
{
   if (system_ast_manager == null) {
      system_ast_manager = new SystemASTManager(this.system_manager, this, this.the_interpreter);
    }
   return system_ast_manager;
}

public void configure() 				{ }
public void deconfigure()				{ }

@Override public PybaseProject getProject()
{
   return null;
}

@Override public void rebuildPath()
{
   throw new RuntimeException("Not Implemented");
}

public void rebuildPath(String defaultSelectedInterpreter)
{
   throw new RuntimeException("Not Implemented");
}

@Override public AbstractInterpreterManager getRelatedInterpreterManager()
{
   return system_manager;
}


@Override public int getGrammarVersion()
{
   PybaseInterpreter info = this.the_interpreter;
   if (info != null) {
      return info.getGrammarVersion();
    }
   else{
      return LATEST_GRAMMAR_VERSION;
    }
}

@Override public PybaseInterpreter getProjectInterpreter()
{
   return this.the_interpreter;
}

@Override public boolean isOkToUse()
{
   return this.system_manager != null && this.the_interpreter != null;
}


/********************************************************************************/
/*										*/
/*	Handle builtin completions						*/
/*										*/
/********************************************************************************/

@Override public AbstractToken[] getBuiltinCompletions()
{
   if (!this.isOkToUse()) {
      return null;
    }
   return this.system_manager.getBuiltinCompletions(this.the_interpreter.getName());
}


@Override public void clearBuiltinCompletions()
{
   this.system_manager.clearBuiltinCompletions(this.the_interpreter.getName());
}




/********************************************************************************/
/*										*/
/*	Handle builtin modules							*/
/*										*/
/********************************************************************************/

@Override public AbstractModule getBuiltinMod()
{
   if (!this.isOkToUse()) {
      return null;
    }
   return this.system_manager.getBuiltinMod(this.the_interpreter.getName());
}


@Override public void clearBuiltinMod()
{
   this.system_manager.clearBuiltinMod(this.the_interpreter.getName());
}



}	// end of class PybaseSystemNature



/* end of PybaseSystemNature.java */
