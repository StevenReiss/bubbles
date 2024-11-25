/********************************************************************************/
/*										*/
/*		NobasePathSpec.java						*/
/*										*/
/*	Information about a project path					*/
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



package edu.brown.cs.bubbles.nobase;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.io.File;

class NobasePathSpec implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private File directory_file;
private boolean is_user;
private boolean is_exclude;
private boolean is_nested;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobasePathSpec(Element xml)
 {
   String fnm = IvyXml.getTextElement(xml,"SOURCE");
   if (fnm == null) fnm = IvyXml.getTextElement(xml,"BINARY");
   if (fnm == null) {
      // old form
      fnm = IvyXml.getTextElement(xml,"DIR");
      is_user = IvyXml.getAttrBool(xml,"USER");
      is_exclude = IvyXml.getAttrBool(xml,"EXCLUDE");
    }
   else {
      is_user = true;
      is_exclude = false;
      String typ = IvyXml.getAttrString(xml,"TYPE");
      switch (typ) {
         case "LIBRARY" :
            is_user = false;
            break;
         default :
         case "INCLUDE" :
            break;
         case "EXCLUDE" :
            is_exclude = true;
            break;
       }
    }
   directory_file = new File(fnm);
   is_nested = IvyXml.getAttrBool(xml,"SUBDIRS");
}



NobasePathSpec(File f,boolean u,boolean e,boolean n)
{
   directory_file = f;
   is_user = u;
   is_exclude = e;
   is_nested = n;
}



/********************************************************************************/
/*										*/
/*	AccessMethods								*/
/*										*/
/********************************************************************************/

File getFile()				{ return directory_file; }

boolean isUser()			{ return is_user; }

boolean isExclude()			{ return is_exclude; }

boolean isNested()			{ return is_nested; }


void setProperties(boolean usr,boolean exc,boolean nest)
{
   is_user = usr;
   is_exclude = exc;
   is_nested = nest;
}



/********************************************************************************/
/*										*/
/*	Matching methods							*/
/*										*/
/********************************************************************************/

boolean match(File path)
{
   if (!directory_file.isAbsolute()) {
      String par = directory_file.getParent();
      if (par == null || par.equals("*") || par.equals("**")) {
	 if (path.getName().equals(directory_file.getName())) return true;
       }
    }
   else if (path == null) return false;
   else if (path.equals(directory_file)) return true;

   return false;
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

public void outputXml(IvyXmlWriter xw)
{
   xw.begin("PATH");
   xw.field("ID",hashCode());
   xw.field("SOURCE",directory_file.getPath());
   if (!is_user) xw.field("TYPE","LIBRARY");
   else if (is_exclude) xw.field("TYPE","EXCLUDE");
   else xw.field("TYPE","INCLUDE");
   xw.field("SUBDIRS",is_nested);
   xw.end("PATH");
}




}	// end of class NobasePathSpec




/* end of NobasePathSpec.java */

