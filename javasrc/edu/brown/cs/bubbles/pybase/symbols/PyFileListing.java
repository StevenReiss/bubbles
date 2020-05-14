/********************************************************************************/
/*										*/
/*		PyFileListing.java						*/
/*										*/
/*	Python Bubbles Base file list container 				*/
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

import edu.brown.cs.bubbles.pybase.PybaseMain;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;



/**
 * Helper class for finding out about python files below some source folder.
 *
 * @author Fabio
 */

class PyFileListing {



/********************************************************************************/
/*										*/
/*	Exposed subclass for holding inforamtion on a single file		*/
/*										*/
/********************************************************************************/

static final class PyFileInfo {

   private final String rel_path;
   private final File	the_file;

   PyFileInfo(File file,String path) {
      the_file = file;
      rel_path = path;
    }

   File getFile()			{ return the_file; }
   String getPackageName()		{ return rel_path; }
   String getModuleName(StringBuilder temp) {
      String scannedmodulename = getPackageName();
      String modname;
      String name = the_file.getName();
      if (scannedmodulename.length() != 0) {
	 temp.setLength(0);
	 modname = temp.append(scannedmodulename).append('.')
	    .append(PythonPathHelper.stripExtension(name)).toString();
       }
      else {
	 modname = PythonPathHelper.stripExtension(name);
       }
      return modname;
    }

}	// end of inner class PyFileInfo




/********************************************************************************/
/*										*/
/*	Static methods to get sets of files					*/
/*										*/
/********************************************************************************/

static PyFileListing getPyFilesBelow(File file,FileFilter filter,
					       boolean checkhasinit)
{
   PyFileListing result = new PyFileListing();
   return getPyFilesBelow(result, file, filter, true, 0, checkhasinit, "",
			     new HashSet<File>());
}




private static PyFileListing getPyFilesBelow(PyFileListing result,File file,
						FileFilter filter,boolean addsubfolders,int level,boolean checkhasinit,
						String currmodulerep,Set<File> canonicalfolders)
{
   if (file != null && file.exists()) {
      // only check files that actually exist

      if (file.isDirectory()) {
	 if (level != 0) {
	    StringBuilder newmodulerep = new StringBuilder(currmodulerep);
	    if (newmodulerep.length() != 0) {
	       newmodulerep.append(".");
	     }
	    newmodulerep.append(file.getName());
	    currmodulerep = newmodulerep.toString();
	  }

	 // check if it is a symlink loop
	 try {
	    File canonicalizeddir = file.getCanonicalFile();
	    if (!canonicalizeddir.equals(file)) {
	       if (canonicalfolders.contains(canonicalizeddir)) {
		  return result;
		}
	     }
	    canonicalfolders.add(canonicalizeddir);
	  }
	 catch (IOException e) {
	    PybaseMain.logE("Problem canonicalizing file", e);
	  }

	 File[] files = null;

	 if (filter != null) files = file.listFiles(filter);
	 else files = file.listFiles();

	 boolean hasinit = false;

	 List<File> folderslater = new LinkedList<File>();

	 for (File file2 : files) {
	    if (file2.isFile()) {
	       result.addPyFileInfo(new PyFileInfo(file2,currmodulerep));
	       if (checkhasinit && hasinit == false) {
		  // only check if it has __init__ if really needed
		  if (PythonPathHelper.isValidInitFile(file2.getName())) {
		     hasinit = true;
		   }
		}
	     }
	    else {
	       folderslater.add(file2);
	     }
	  }

	 if (!checkhasinit || hasinit || level == 0) {
	    result.addFolder(file);
	    for (File folder : folderslater) {
	       if (folder.isDirectory() && addsubfolders) {
		  getPyFilesBelow(result, folder, filter, addsubfolders, level + 1,
				     checkhasinit, currmodulerep, canonicalfolders);
		}
	     }
	  }
       }
      else if (file.isFile()) {
	 result.addPyFileInfo(new PyFileInfo(file,currmodulerep));
       }
      else {
	 throw new RuntimeException("Not dir nor file... what is it?");
       }
    }

   return result;
}




/********************************************************************************/
/*										*/
/*	Private Data representing the file list 				*/
/*										*/
/********************************************************************************/

private List<PyFileInfo>	pyfile_infos;
private List<File>		folders_found;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

PyFileListing()
{
   pyfile_infos = new ArrayList<PyFileInfo>();
   folders_found = new ArrayList<File>();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public Collection<PyFileInfo> getFoundPyFileInfos()
{
   return pyfile_infos;
}

public Collection<File> getFoundFolders()
{
   return folders_found;
}

private void addPyFileInfo(PyFileInfo info)
{
   pyfile_infos.add(info);
}


private void addFolder(File f)
{
   folders_found.add(f);
}


}	// end of class PyFileListing




/* end of PyFileListing.java */
