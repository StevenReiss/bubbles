/********************************************************************************/
/*                                                                              */
/*              BeduConstants.java                                              */
/*                                                                              */
/*      Constants for educational bubbles (SUDS)                                */
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



package edu.brown.cs.bubbles.bedu;



public interface BeduConstants
{


/********************************************************************************/
/*                                                                              */
/*      External view of an assignment                                          */
/*                                                                              */
/********************************************************************************/

interface Assignment {
   String getName();
   String getDescription();
   String getDocumentationUrl();
   String getSurveyUrl();
   boolean isInUserWorkspace();
   boolean isPast();
   boolean isCurrent();
   boolean canSubmit();
   
   boolean createProject();
   String handin();
}



}       // end of interface BeduConstants




/* end of BeduConstants.java */

