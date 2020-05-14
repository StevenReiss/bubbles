/********************************************************************************/
/*										*/
/*		BuenoLocationStatic.java					*/
/*										*/
/*	BUbbles Environment New Objects creator location provided by name	*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.bueno;

import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;

import java.io.File;
import java.util.List;



class BuenoLocationStatic extends BuenoLocation
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private String		project_name;
private String		class_name;
private String		package_name;
private String		insert_name;
private File		location_file;
private boolean 	insert_after;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BuenoLocationStatic(String proj,String nm,String ins,boolean after)
{
   BumpClient bcc = BumpClient.getBump();

   project_name = proj;
   package_name = null;
   class_name = null;
   location_file = null;
   insert_name = ins;
   insert_after = after;

   List<BumpLocation> locs = null;
   if (nm != null && nm.indexOf("<") < 0) locs = bcc.findPackage(proj,nm);
   if (locs != null && locs.size() > 0) {
      BumpLocation loc = locs.get(0);
      if (loc.getProject() != null) project_name = loc.getProject();
      location_file = loc.getFile();
      class_name = null;
      package_name = nm;
    }
   else if (nm != null) {
      locs = bcc.findClassDefinition(proj,nm);
      if (locs != null && locs.size() > 0) {
	 BumpLocation loc = locs.get(0);	// Is this always correct?
	 if (loc.getProject() != null) project_name = loc.getProject();
	 location_file = loc.getFile();
	 String snm = loc.getSymbolName();
	 class_name = snm;
	 String key = loc.getKey();
	 int ct = 0;
	 for (int i = key.indexOf('$'); i >= 0; i = key.indexOf('$',i+1)) ct++;
	 int idx = snm.lastIndexOf(".");
	 if (ct > 0) {
	    for (int j = 0; idx > 0 && j < ct; ++j) {
	       idx = snm.lastIndexOf(".",idx-1);
	     }
	  }
	 if (idx > 0) package_name = snm.substring(0,idx);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getProject()			{ return project_name; }

@Override public String getPackage()			{ return package_name; }

@Override public String getClassName()			{ return class_name; }

@Override public File getFile() 			{ return location_file; }

@Override public String getInsertAfter()
{
   if (insert_after) return insert_name;
   return null;
}

@Override public String getInsertBefore()
{
   if (!insert_after) return insert_name;
   return null;
}


@Override public String getInsertAtEnd()
{
   if (package_name == null) return class_name;
   if (class_name != null) return package_name + "." + class_name;

   return null;
}



}	// end of class BuenoLocationStatic




/* end of BuenoLocationStatic.java */
