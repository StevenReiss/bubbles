/********************************************************************************/
/*                                                                              */
/*              RebaseJavaLanguage.java                                         */
/*                                                                              */
/*      Main access point for java-based semantics                              */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
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

/* SVN: $Id$ */



package edu.brown.cs.bubbles.rebase.java;

import edu.brown.cs.bubbles.rebase.RebaseConstants;
import edu.brown.cs.bubbles.rebase.RebaseFile;

import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;

public class RebaseJavaLanguage implements RebaseConstants.RebaseLanguage, RebaseJavaConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<RebaseFile,RebaseJavaFile>  semantic_map;
private RebaseJavaContext base_context;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public RebaseJavaLanguage()
{
   semantic_map = new WeakHashMap<RebaseFile,RebaseJavaFile>();
   base_context = new RebaseJavaContext(null);
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public synchronized RebaseSemanticData getSemanticData(RebaseFile rf)
{
   RebaseJavaFile rjf = semantic_map.get(rf);
   if (rjf == null) {
      rjf = new RebaseJavaFile(rf);
      semantic_map.put(rf,rjf);
    }
   return rjf;
}



@Override public synchronized RebaseProjectSemantics getSemanticData(Collection<RebaseFile> rfs)
{
   RebaseJavaRoot root = new RebaseJavaRoot(base_context);
   for (RebaseFile rf : rfs) {
      RebaseJavaFile rjf = semantic_map.get(rf);
      if (rjf == null) {
         rjf = new RebaseJavaFile(rf);
         semantic_map.put(rf,rjf);
       }
      root.addFile(rjf);
    }
   
   return root;
}




}       // end of class RebaseJavaLanguage




/* end of RebaseJavaLanguage.java */

