/********************************************************************************/
/*										*/
/*		ModulesFoundStructure.java					*/
/*										*/
/*	Python Bubbles Base representation of modules found			*/
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;


/**
 * This class contains all the information we need from the folders beneath the pythonpath.
 *
 * @author Fabio
 */
public class ModulesFoundStructure {

public static final boolean DEBUG_ZIP = false;

/**
 * Inner class to contain what we found within a zip file.
 *
 * @author Fabio
 */
public static class ZipContents {

public ZipContents(File zipFile)
{
   zip_file = zipFile;
}

public static int	      ZIP_CONTENTS_TYPE_JAR		 = 1;

public static int	      ZIP_CONTENTS_TYPE_PY_ZIP	      = 2;

/**
 * See constants.
 *
 * Basically it must be one type or the other... if it has .java or .class, it's considered a jar.
 *
 * Now, if it has any .py it is considered a py zip.
 *
 * That's because the handling of that file can be very different from one situation to the other:
 * - If it's a jar, we're going to rely on JDT, which should not be a requisite for those that don't use jython
 * - If it has .py files, we're going to do the handling without any dependency
 */
public int		     zip_contents_type;

/**
 * May be any zip file (.zip, .jar, .egg, etc)
 */
public File		    zip_file;

/**
 * These are the paths found within the zip file that are valid to be in the pythonpath.
 *
 * If it is a jar file, those are the .class files.
 * If it is a zip file with .py files, those are the actual .py files (or .pyd files -- dlls)
 *
 * Does not support mixing both
 */
public Set<String>	     found_file_zip_paths		  = new HashSet<String>();

/**
 * These are the paths for the folders for the files found to be in the pythonpath.
 */
public Set<String>	     found_folder_zip_paths		= new HashSet<String>();

// these are only 'temporarily' filled if it's not a Jar. Note that these contains the
// contents for the whole zip,
// and not only the ones that are in the pythonpath
// They are cleared when consolidatePythonpathInfo is called.
public TreeMap<String, String> py_files_lower_to_regular	     = new TreeMap<String, String>(); // .py
// / .pyd / .class

public TreeSet<String>	 pyfolders_lower		       = new TreeSet<String>(); // folders

public TreeSet<String>	 py_init_files_lower_without_extension = new TreeSet<String>(); // __init__.py
// (full path in zip)

/**
 * Given the temporary info found, goes on to fill the actual found modules.
 */
public void consolidatePythonpathInfo()
{
   StringBuilder buffer = new StringBuilder();
   for (Map.Entry<String, String> entry : py_files_lower_to_regular.entrySet()) {
      String key = entry.getKey();
      int index = StringUtils.rFind(key, '/');
      boolean add = true;
      if (index != -1) {
	 // If it's in the root, we don't need to check for __init__
	 buffer.setLength(0);
	 buffer.append(key.substring(0, index));
	 if (zip_contents_type == ZIP_CONTENTS_TYPE_PY_ZIP) {
	    // we don't need to check for __init__ if we have a jar
	    if (buffer.length() > 0) {
	       buffer.append("/");
	       buffer.append("__init__");
	       add = py_init_files_lower_without_extension.contains(buffer.toString());
	    }
	 }
      }

      if (add) {
	 String filePath = entry.getValue();
	 if (DEBUG_ZIP) {
	    System.out.println("Zip: Found in pythonpath:" + filePath);
	 }
	 found_file_zip_paths.add(filePath);
	 found_folder_zip_paths.add(StringUtils.stripFromLastSlash(filePath));
      }
   }
   py_files_lower_to_regular = null;
   pyfolders_lower = null;
   py_init_files_lower_without_extension = null;
}

}

/**
 * Contains: file found -> module name it was found with.
 */
public Map<File, String> regularModules = new HashMap<File, String>();

/**
 * For each zip, there should be one entry.
 */
public List<ZipContents> zipContents	= new ArrayList<ZipContents>();

} // end of class ModulesFoundStructure


/* end of ModulesFoundStructure.java */
