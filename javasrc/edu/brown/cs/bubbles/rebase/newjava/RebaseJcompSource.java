/********************************************************************************/
/*                                                                              */
/*              RebaseJcompSource.java                                          */
/*                                                                              */
/*      Representation of a source for Jcomp                                    */
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



package edu.brown.cs.bubbles.rebase.newjava;

import edu.brown.cs.bubbles.rebase.RebaseFile;
import edu.brown.cs.bubbles.rebase.RebaseMain;

import edu.brown.cs.ivy.jcomp.JcompSource;

class RebaseJcompSource implements JcompSource
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private RebaseFile      base_file;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

RebaseJcompSource(RebaseFile rf)
 {
   base_file = rf;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/


@Override public String getFileContents()
{
   String txt = RebaseMain.getFileContents(base_file);
   
   return txt;
}



@Override public String getFileName() 
{
   return base_file.getFileName();
}



RebaseFile getRebaseFile()              { return base_file; }



}       // end of class RebaseJcompSource




/* end of RebaseJcompSource.java */

