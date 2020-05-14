/********************************************************************************/
/*                                                                              */
/*              BvcrFileStatus.java                                             */
/*                                                                              */
/*      Holde the status for a version-controled file                                                  */
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



package edu.brown.cs.bubbles.bvcr;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;


class BvcrControlFileStatus implements BvcrConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String file_name;
private FileState file_state;
private boolean is_unpushed;
private String new_name;


   
/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BvcrControlFileStatus(Element xml) 
{
   file_name = IvyXml.getTextElement(xml,"NAME");
   new_name = IvyXml.getTextElement(xml,"NEWNAME");
   file_state = IvyXml.getAttrEnum(xml,"STATE",FileState.UNMODIFIED);
   is_unpushed = IvyXml.getAttrBool(xml,"UNPUSHED");
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getFileName()                            { return file_name; }

FileState getFileState()                        { return file_state; }
boolean getUnpushed()                           { return is_unpushed; }
String getNewFileName()                         { return new_name; }




}       // end of class BvcrFileStatus




/* end of BvcrFileStatus.java */

