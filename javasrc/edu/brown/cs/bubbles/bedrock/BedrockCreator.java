/********************************************************************************/
/*										*/
/*		BedrockCreator.java						*/
/*										*/
/*	Handle creation-related commands for Bubbles				*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bedrock;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;




class BedrockCreator implements BedrockConstants {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BedrockPlugin	our_plugin;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BedrockCreator(BedrockPlugin bp)
{
   our_plugin = bp;
}




/********************************************************************************/
/*										*/
/*	Class creation methods							*/
/*										*/
/********************************************************************************/

void handleNewClass(String proj,String name,boolean frc,String cnts,IvyXmlWriter xw)
	throws BedrockException
{
   if (name == null) return;

   String pkg = "";
   String itm = name;
   int idx = name.lastIndexOf(".");
   if (idx >= 0) {
      pkg = name.substring(0,idx);
      itm = name.substring(idx+1);
    }

   if (!itm.endsWith(".java")) itm += ".java";

   try {
      IPackageFragment frag = our_plugin.getProjectManager().findPackageFragment(proj,pkg);

      if (frag == null) throw new BedrockException("Package " + pkg + " not found for new class");

      ICompilationUnit icu = frag.createCompilationUnit(itm,cnts,frc,null);
      if (icu == null) throw new BedrockException("Class create failed");
      icu.save(null,true);
      icu.makeConsistent(null);
      BedrockUtil.outputJavaElement(icu,null,false,xw);
    }
   catch (JavaModelException e) {
      System.err.println("Problem with class create: " + e);
      e.printStackTrace();
      throw new BedrockException("Problem with class create",e);
    }
}





/********************************************************************************/
/*										*/
/*	Utility routines							*/
/*										*/
/********************************************************************************/

IJavaProject getJavaProject(String p) throws BedrockException
{
   if (p == null || p.length() == 0) return null;

   IProject ip = our_plugin.getProjectManager().findProject(p);

   return JavaCore.create(ip);
}




}	// end of class BedrockCreator



/* end of BedrockCreator.java */
