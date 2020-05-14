/********************************************************************************/
/*										*/
/*		ModulesKey.java 						*/
/*										*/
/*	Python Bubbles Base key representation for a module			*/
/*										*/
/********************************************************************************/
/*	Copyright 2012 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2012, Brown University, Providence, RI.				 *
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

import java.io.File;


/**
 * This class defines the key to use for some module. All its operations are based on its name.
 * The file may be null.
 *
 * @author Fabio Zadrozny
 */

class ModulesKey implements Comparable<ModulesKey> {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected String module_name;
protected File module_file;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

ModulesKey(String name, File f)
{
   this.module_name = name;
   this.module_file = f;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getModuleName()			{ return module_name; }
File getModuleFile()			{ return module_file; }

void setModuleName(String n)		{ module_name = n; }
void setModuleFile(File f)		{ module_file = f; }



/********************************************************************************/
/*										*/
/*	Comparison methods							*/
/*										*/
/********************************************************************************/

@Override public int compareTo(ModulesKey o)
{
   return module_name.compareTo(o.module_name);
}



@Override public boolean equals(Object o)
{
   if (!(o instanceof ModulesKey )){
      return false;
    }

   ModulesKey m = (ModulesKey)o;
   if(!(module_name.equals(m.module_name))){
      return false;
    }

   //consider only the name
   return true;
}


@Override public int hashCode()
{
   return this.module_name.hashCode();
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   if(module_file != null){
      StringBuffer ret = new StringBuffer(module_name);
      ret.append(" - ");
      ret.append(module_file);
      return ret.toString();
    }
   return module_name;
}


/**
 * @return true if any of the parts in this modules key start with the passed string (considering the internal
 * parts lower case).
 */
public boolean hasPartStartingWith(String startingWithLowerCase)
{
   for (String mod : StringUtils.dotSplit(this.module_name.toLowerCase())) {
      if(mod.startsWith(startingWithLowerCase)){
	 return true;
       }
    }
   return false;
}



}	// end of class ModulesKey




/* end of ModulesKey.java */
