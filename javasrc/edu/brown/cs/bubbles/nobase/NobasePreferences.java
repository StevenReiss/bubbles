/********************************************************************************/
/*										*/
/*		NobasePreferences.java						*/
/*										*/
/*	description of class							*/
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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class NobasePreferences implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,String> pref_props;
private File base_file;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobasePreferences(File preffile)
{
   this();
   base_file = preffile;
   Element e = IvyXml.loadXmlFromFile(preffile);
   if (e != null) loadXml(e);
}

NobasePreferences(NobasePreferences par)
{
   this();
   pref_props.putAll(par.pref_props);
}

private NobasePreferences()
{
   pref_props = new HashMap<>();
   base_file = null;
}




/********************************************************************************/
/*										*/
/*	General property methods						*/
/*										*/
/********************************************************************************/

public String getProperty(String prop,String dflt)
{
   if (pref_props.containsKey(prop)) return pref_props.get(prop);

   return dflt;
}



public void setProperty(String prop,String value)
{
   pref_props.put(prop,value);
}




/********************************************************************************/
/*										*/
/*	I/O methods								*/
/*										*/
/********************************************************************************/

void outputXml(IvyXmlWriter xw)
{
   xw.begin("PREFERENCES");

   for (Map.Entry<String,String> ent : pref_props.entrySet()) {
      xw.begin("PREF");
      xw.field("NAME",ent.getKey());
      xw.field("VALUE",ent.getValue());
      xw.field("OPTS",true);
      xw.end("PREF");
    }

   xw.end("PREFERENCES");
}



void loadXml(Element xml)
{
   Element prefs;
   if (IvyXml.isElement(xml,"PREFERENCES")) prefs = xml;
   else prefs = IvyXml.getChild(xml,"PREFERENCES");

   for (Element pel : IvyXml.children(prefs,"PROP")) {
      String k = IvyXml.getAttrString(pel,"NAME");
      if (k == null) k = IvyXml.getAttrString(pel,"KEY");
      String v = IvyXml.getAttrString(pel,"VALUE");
      pref_props.put(k,v);
    }
   for (Element pel : IvyXml.children(prefs,"PREF")) {
      String k = IvyXml.getAttrString(pel,"NAME");
      if (k == null) k = IvyXml.getAttrString(pel,"KEY");
      String v = IvyXml.getAttrString(pel,"VALUE");
      pref_props.put(k,v);
    }
}



void dumpPreferences(IvyXmlWriter xw)
{
   outputXml(xw);
}



void setPreferences(Element xml)
{
   for (Element opt : IvyXml.children(xml,"OPTION")) {
      String nm = IvyXml.getAttrString(opt,"NAME");
      if (nm == null) nm = IvyXml.getAttrString(opt,"KEY");
      String val = IvyXml.getAttrString(opt,"VALUE");
      pref_props.put(nm,val);
    }

   flush();
}


public void flush()
{
   if (base_file != null) {
      try {
	 IvyXmlWriter xw = new IvyXmlWriter(base_file);
	 dumpPreferences(xw);
	 xw.close();
       }
      catch (IOException e) {
	 NobaseMain.logE("Problem saving preferences file " + base_file,e);
       }
    }
}



}	// end of class NobasePreferences




/* end of NobasePreferences.java */

