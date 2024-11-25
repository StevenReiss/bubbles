/********************************************************************************/
/*                                                                              */
/*              BvcrControlVersion.java                                         */
/*                                                                              */
/*      Version information for the control panel                               */
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

import java.util.Date;
import java.util.HashSet;
import java.util.Set;



class BvcrControlVersion implements BvcrConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String version_name;
private Date version_date;
private String version_author;
private Set<String> prior_ids;
private Set<String> alternative_names;
private Set<String> alternative_ids;
private String version_message;
private String version_body;
   


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BvcrControlVersion(Element xml)
{
   version_name = IvyXml.getAttrString(xml,"NAME");
   version_date = IvyXml.getAttrDate(xml,"DATE");
   version_author = IvyXml.getAttrString(xml,"AUTHOR");
   version_message = IvyXml.getTextElement(xml,"MSG");
   if (version_message == null) version_message = IvyXml.getTextElement(xml,"MESSAGE");
   version_body = IvyXml.getTextElement(xml,"BODY");
   prior_ids = new HashSet<String>();
   for (Element e : IvyXml.children(xml,"PRIOR")) {
      prior_ids.add(IvyXml.getTextElement(e,"ID"));
    }
   alternative_names = new HashSet<String>();
   alternative_ids = new HashSet<String>();
   for (Element e : IvyXml.children(xml,"ALTERNATIVE")) {
      String altnm = IvyXml.getTextElement(e,"NAME");
      if (altnm != null) alternative_names.add(altnm);
      String altid = IvyXml.getTextElement(e,"ID");
      if (altid != null) alternative_ids.add(altid);   
    }
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getName()                        { return version_name; }

Set<String> getAlternativeNames()       { return alternative_names; }

Set<String> getAlternativeIds()         { return alternative_ids; }

Set<String> getPriorIds()               { return prior_ids; }

Date getDate()                          { return version_date; }

String getAuthor()                      { return version_author; }

String getBestName()
{
   String nm = null;
   for (String s : alternative_names) {
      if (nm == null || nm.length() > s.length()) nm = s;
    }
   if (nm != null) return nm;
   String id = getName();
   for (String s : alternative_ids) {
      if (s.length() < id.length()) id = s;
    }
   return id;
}


String getMessage() 
{
   if (version_message != null) return version_message;
   return version_body;
}



/********************************************************************************/
/*                                                                              */
/*      Output Methods                                                          */
/*                                                                              */
/********************************************************************************/

String getHtmlDescription()
{
   StringBuffer buf = new StringBuffer();
   String nm = getBestName();
   
   buf.append("<table style='border: 1px solid black; border-collapse: collapse;'>");
   
   buf.append("<caption style='font-weight: bold;'>Version " + nm + "</caption>");
   
   outputRow("Full Version",version_name,buf);
   outputRow("Date",getDate().toString(),buf);
   outputRow("Author",version_author,buf);
   for (String s : alternative_names) {
      if (!s.equals(nm)) outputRow("Version Name",s,buf);
    }
   for (String s : alternative_ids) {
      if (!s.equals(version_name) && !s.equals(nm))
         outputRow("Version Id",s,buf);
    }
   String txt = version_body;
   if (txt == null) txt = version_message;
   outputRow("Message",txt,buf);
   
   buf.append("</table>");
   
   return buf.toString();
}

private void outputRow(String key,String val,StringBuffer buf) 
{
   buf.append("<tr><td style='border: 1px solid black;'>");
   buf.append(key + ":");
   buf.append("</td><td style='border: 1px solid black;'>");
   buf.append(val);
   buf.append("</td></tr>");
}




}       // end of class BvcrControlVersion




/* end of BvcrControlVersion.java */

