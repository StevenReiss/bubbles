/********************************************************************************/
/*										*/
/*		ModulesKeyForZip.java						*/
/*										*/
/*	Python Bubbles Base key representation for a zipped module		*/
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

package edu.brown.cs.bubbles.pybase.symbols;

import java.io.File;

/**
 * This is the modules key that should be used if we have an entry in a zip file.
 *
 * @author Fabio
 */

class ModulesKeyForZip extends ModulesKey {

/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private String zip_module_path;
private boolean is_file;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public ModulesKeyForZip(String name, File f, String zipmodulepath, boolean isFile)
{
   super(name, f);
   this.zip_module_path = zipmodulepath;
   this.is_file = isFile;
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

boolean isFile()				{ return is_file; }
String getZipModulePath()			{ return zip_module_path; }


/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   StringBuffer ret = new StringBuffer(module_name);
   if(module_file != null){
      ret.append(" - ");
      ret.append(module_file);
    }
   if(zip_module_path != null){
      ret.append(" - zip path:");
      ret.append(zip_module_path);
    }
   return ret.toString();
}



}	// end of class ModulesKeyForZip




/* end of ModulesKeyForZip.java */
