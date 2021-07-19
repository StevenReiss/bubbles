/********************************************************************************/
/*										*/
/*		BuenoProjectMakerBase.java					*/
/*										*/
/*	description of class							*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
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

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bueno;

import edu.brown.cs.bubbles.bueno.BuenoConstants.BuenoProjectMaker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

abstract class BuenoProjectMakerBase implements BuenoConstants, BuenoProjectMaker
{




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private static final String SRC_NAME = "src_";




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected BuenoProjectMakerBase()
{ }




/********************************************************************************/
/*										*/
/*	Utility Methods 							*/
/*										*/
/********************************************************************************/

protected boolean defineProject(BuenoProjectCreationControl ctrl,BuenoProjectProps props,
      File dir)
{
   Set<File> srcs = new HashSet<File>();
   Set<File> libs = new HashSet<File>();
   Set<File> rsrcs = new HashSet<File>();

   findFiles(dir,srcs,libs,rsrcs);
   Map<File,List<File>> roots = new HashMap<>();
   for (File sf : srcs) {
      String pkg = ctrl.getPackageName(sf);
      File par = sf.getParentFile();
      if (pkg != null) {
	 String [] ps = pkg.split("\\.");
	 for (int i = ps.length-1; par != null && i >= 0; --i) {
	    if (!par.getName().equals(ps[i])) par = null;
	    else par = par.getParentFile();
	  }
       }
      if (par != null) {
	 List<File> lf = roots.get(par);
	 if (lf == null) {
	    lf = new ArrayList<File>();
	    roots.put(par,lf);
	  }
	 lf.add(sf);
       }
    }
   props.getLinks().clear();
   props.getSources().clear();
   props.getLibraries().clear();

   props.getLinks().put(SRC_NAME + "1",dir);
   for (File f : roots.keySet()) {
      props.getSources().add(f);
    }

   for (File lf : libs) {
      props.getLibraries().add(lf);
    }
   for (File lf : rsrcs) {
      props.getLibraries().add(lf);
    }

   return true;
}




protected void findFiles(File dir,Set<File> srcs,Set<File> libs,Set<File> rsrcs)
{
   if (dir.isDirectory()) {
      if (dir.getName().equals("bBACKUP")) return;
      else if (dir.getName().startsWith(".")) return;
      else if (dir.getName().equals("node_modules")) return;
      if (dir.getName().equals("resources")) {
         boolean havesrc = false;
         for (String fnm : dir.list()) {
            if (fnm.endsWith(".java")) havesrc = true;
          }
         if (!havesrc) rsrcs.add(dir);
       }
      if (dir.listFiles() != null) {
	 for (File sf : dir.listFiles()) {
	    findFiles(sf,srcs,libs,rsrcs);
	  }
       }
      return;
    }

   String pnm = dir.getPath();
   try {
      dir = dir.getCanonicalFile();
    }
   catch (IOException e) { }

   if (dir.length() < 10) return;
   if (!dir.isFile()) return;

   if (pnm.endsWith(".java")) srcs.add(dir.getAbsoluteFile());
   else if (pnm.endsWith(".jar")) {
      if (!pnm.contains("javadoc") && !pnm.contains("source"))
	 libs.add(dir.getAbsoluteFile());
    }
}



}	// end of class BuenoProjectMakerBase




/* end of BuenoProjectMakerBase.java */





























































































































































































































































































































































































































































