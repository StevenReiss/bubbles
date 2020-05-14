/********************************************************************************/
/*										*/
/*		BvcrProject .java						     */
/*										*/
/*	description of class							*/
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


/* SVN: $Id$ */



package edu.brown.cs.bubbles.bvcr;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;


class BvcrProject implements BvcrConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		project_name;
private String		source_directory;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BvcrProject(Element xml)
{
   project_name = IvyXml.getAttrString(xml,"NAME");
   source_directory = null;

   Element cps = IvyXml.getChild(xml,"CLASSPATH");
   for (Element cp : IvyXml.children(cps,"PATH")) {
      String typ = IvyXml.getAttrString(cp,"TYPE");
      if (typ.equals("SOURCE") && !IvyXml.getAttrBool(cp,"NESTED")) {
	 if (source_directory == null) {
	    source_directory = IvyXml.getTextElement(cp,"SOURCE");
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getName()		{ return project_name; }

String getSourceDirectory()	{ return source_directory; }


}	// end of class BvcrProject




/* end of BvcrProject.java */

