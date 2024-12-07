/********************************************************************************/
/*										*/
/*		BvcrFileVersion.java						*/
/*										*/
/*	Bubble Version Collaboration Repository representation of a file version*/
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


package edu.brown.cs.bubbles.bvcr;


import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

class BvcrFileVersion implements BvcrConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private File				for_file;
private String				version_id;
private Date				version_time;
private String				version_author;
private String				version_message;
private String                          version_body;
private Collection<String>		alternative_ids;
private Collection<BvcrFileVersion>	prior_versions;
private Collection<String>		prior_names;





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BvcrFileVersion(File f,String id,Date when,String auth,String msg)
{
   for_file = f;
   version_id = id;
   version_time = when;
   version_author = auth;
   version_message = msg;
   version_body = null;

   alternative_ids = null;
   prior_versions = new HashSet<BvcrFileVersion>();
   prior_names = null;
}



BvcrFileVersion(Element xml,Map<String,BvcrFileVersion> versions)
{
   for_file = new File(IvyXml.getAttrString(xml,"FILE"));
   version_id = IvyXml.getAttrString(xml,"ID");
   version_time = new Date(IvyXml.getAttrLong(xml,"TIME"));
   version_author = IvyXml.getAttrString(xml,"AUTHOR");
   version_message = IvyXml.getTextElement(xml,"MESSAGE");
   version_body = IvyXml.getTextElement(xml,"BODY");

   if (version_id != null && versions != null) versions.put(version_id,this);

   alternative_ids = null;
   prior_versions = new HashSet<BvcrFileVersion>();
   prior_names = null;

   for (Element e : IvyXml.children(xml,"PRIOR")) {
      String id = IvyXml.getAttrString(e,"ID");
      BvcrFileVersion ov = versions.get(id);
      if (ov == null) {
	 if (prior_names == null) prior_names = new ArrayList<String>();
	 prior_names.add(id);
       }
      else addPriorVersion(ov);
    }

   for (Element e : IvyXml.children(xml,"ALTERNATIVE")) {
      String id = IvyXml.getText(e);
      addAlternativeId(id,versions);
    }
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void addPriorVersion(BvcrFileVersion v)
{
   prior_versions.add(v);
}



void addAlternativeId(String id,Map<String,BvcrFileVersion> versions)
{
   if (id == null) return;
   if (alternative_ids == null) alternative_ids = new HashSet<String>();
   alternative_ids.add(id);
   if (versions != null) versions.put(id,this);
}



void addAlternativeName(String id)
{
   if (alternative_ids == null) alternative_ids = new HashSet<String>();
   alternative_ids.add(id);
}


void addVersionBody(String b)           { version_body = b; }

String getVersionId()			{ return version_id; }

Date getVersionTime()			{ return version_time; }

String getAuthor()			{ return version_author; }

String getMessage()			{ return version_message; }

String getFullMessage() 
{
   String msg = version_message;
   if (version_body != null) {
      msg += "\n" + version_body;
    }
   return msg;
}



Collection<BvcrFileVersion> getPriorVersions(Map<String,BvcrFileVersion> versions)
{
   if (prior_names != null && versions != null) {
      for (Iterator<String> it = prior_names.iterator(); it.hasNext(); ) {
	 String id = it.next();
	 BvcrFileVersion ov = versions.get(id);
	 if (ov != null) {
	    addPriorVersion(ov);
	    it.remove();
	  }
       }
      if (prior_names.size() == 0) prior_names = null;
    }

   return prior_versions;
}


Collection<String> getAlternativeIds()
{
   if (alternative_ids == null) return new ArrayList<String>();
   return alternative_ids;
}


boolean isHead()
{
   if (alternative_ids == null) return false;
   return alternative_ids.contains("HEAD");
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(IvyXmlWriter xw)
{
   xw.begin("VERSION");
   xw.field("FILE",for_file);
   xw.field("ID",version_id);
   xw.field("TIME",version_time.getTime());
   xw.field("AUTHOR",version_author);

   for (BvcrFileVersion fv : getPriorVersions(null)) {
      xw.begin("PRIOR");
      xw.field("ID",fv.getVersionId());
      xw.end("PRIOR");
    }

   if (alternative_ids != null) {
      for (String s : alternative_ids) {
	 xw.textElement("ALTERNATIVE",s);
       }
    }

   xw.cdataElement("MESSAGE",version_message);
   if (version_body != null) xw.cdataElement("BODY",version_body);

   xw.end("VERSION");
}



@Override public String toString()
{
   StringBuffer buf = new StringBuffer();
   
   String id = version_id;
   if (alternative_ids != null) {
      for (String s : alternative_ids) {
         if (id == null || s.length() < id.length() - 2) id = s;
       }
    }
   if (id != null && id.length() > 10) {
      id = id.substring(0,8) + "..";
    }
   if (id == null) id = "CURRENT";
   
   String time = null;
   if (version_time != null) {
      SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yy");
      if (System.currentTimeMillis() - version_time.getTime() < 48*60*60*1000) {
         fmt = new SimpleDateFormat("MM/dd kk:mm");
       }
      time = fmt.format(version_time);
    }
   
   String auth = version_author;
   if (auth != null) {
      int idx = auth.indexOf("(");
      if (idx >= 0) auth = auth.substring(0,idx).trim();
    }
   
   if (id != null) buf.append(id);
   if (time != null) {
      if (buf.length() > 0) buf.append(" ");
      buf.append("@ ");
      buf.append(time);
    }
   if (auth != null) {
      if (buf.length() > 0) buf.append(" ");
      buf.append("(");
      buf.append(auth);
      buf.append(")");
    }
   
   return buf.toString();
}

}	// end of class BvcrFileVersion




/* end of BvcrFileVersion.java */
