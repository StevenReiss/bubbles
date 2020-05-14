/********************************************************************************/
/*										*/
/*		RebaseJavaLanguage.java 					*/
/*										*/
/*	Basic implementation of Java parsing using JCOMP			*/
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



package edu.brown.cs.bubbles.rebase.newjava;

import edu.brown.cs.bubbles.rebase.RebaseConstants;
import edu.brown.cs.bubbles.rebase.RebaseFile;

import edu.brown.cs.ivy.jcomp.JcompControl;
import edu.brown.cs.ivy.jcomp.JcompProject;
import edu.brown.cs.ivy.jcomp.JcompSemantics;
import edu.brown.cs.ivy.jcomp.JcompSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;


public class RebaseJavaLanguage implements RebaseConstants.RebaseLanguage, RebaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private JcompControl		jcomp_control;
private Map<RebaseFile,RebaseJcompSource> source_map;
private Map<JcompSemantics,RebaseJcompSemantics> semantic_map;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public RebaseJavaLanguage()
{
   jcomp_control = new JcompControl();
   source_map = new WeakHashMap<RebaseFile,RebaseJcompSource>();
   semantic_map = new WeakHashMap<JcompSemantics,RebaseJcompSemantics>();
}



@Override public synchronized RebaseSemanticData getSemanticData(RebaseFile rf)
{
   if (rf == null) return null;

   RebaseJcompSource rjs = getFileSource(rf);

   JcompSemantics js = jcomp_control.getSemanticData(rjs);
   if (js == null) return null;

   RebaseJcompSemantics semout = semantic_map.get(js);
   if (semout == null) {
      semout = new RebaseJcompSemantics(js);
      semantic_map.put(js,semout);
    }
   return semout;
}



@Override public synchronized RebaseProjectSemantics getSemanticData(Collection<RebaseFile> rfs)
{
   List<JcompSource> inputs = new ArrayList<JcompSource>();
   for (RebaseFile rf : rfs) {
      RebaseJcompSource rjs = getFileSource(rf);
      inputs.add(rjs);
    }

   JcompProject jproj = jcomp_control.getProject(inputs);

   return new RebaseJcompProject(jproj);
}






private synchronized RebaseJcompSource getFileSource(RebaseFile rf)
{
   RebaseJcompSource rjs = source_map.get(rf);
   if (rjs == null) {
      rjs = new RebaseJcompSource(rf);
      source_map.put(rf,rjs);
    }
   return rjs;
}









}	// end of class RebaseJavaLanguage




/* end of RebaseJavaLanguage.java */

