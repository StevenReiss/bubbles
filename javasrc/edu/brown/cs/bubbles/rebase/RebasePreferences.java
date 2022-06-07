/********************************************************************************/
/*                                                                              */
/*              RebasePreferences.java                                          */
/*                                                                              */
/*      Handle preferences and properties for REBUS base implementation         */
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



package edu.brown.cs.bubbles.rebase;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;


class RebasePreferences implements RebaseConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private File            property_file;
private Properties      property_set;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

RebasePreferences(File propfile)
{
   property_file = propfile;
   property_set = new Properties();
   
   String root = System.getProperty("edu.brown.cs.bubbles.rebase.ROOT");
   File f1 = new File(root);
   File f2 = new File(f1,"resources");
   File f3 = new File(f2,"rebusprops.xml");
   loadProperties(f3);
   loadProperties(property_file);
   
   saveProperties();
}




/********************************************************************************/
/*                                                                              */
/*      Handle preference setting                                               */
/*                                                                              */
/********************************************************************************/

void setPreferences(Element xml)
{
   if (xml == null) return;
   
   for (Element opt : IvyXml.children(xml,"OPTION")) {
      String nm = IvyXml.getAttrString(opt,"NAME");
      String vl = IvyXml.getAttrString(opt,"VALUE");
      property_set.put(nm,vl);
    }
   
   saveProperties();
}



/********************************************************************************/
/*                                                                              */
/*      Handle Preference dumping                                               */
/*                                                                              */
/********************************************************************************/

void dumpPreferences(IvyXmlWriter xw)
{
   xw.begin("PREFERENCES");
   for (Map.Entry<Object,Object> ent : property_set.entrySet()) {
      String nm = ent.getKey().toString();
      String vl = ent.getValue().toString();
      xw.begin("PREF");
      xw.field("NAME",nm);
      xw.field("VALUE",vl);
      xw.field("OPTS",true);
      xw.end("PREF");
    }
   xw.end("PREFERENCES");
}


/********************************************************************************/
/*                                                                              */
/*      Handle propretie input and output                                       */
/*                                                                              */
/********************************************************************************/

private void loadProperties(File f)
{
   try {
      FileInputStream fis = new FileInputStream(f);
      property_set.loadFromXML(fis);
    }
   catch (IOException e) { }
}



private void saveProperties()
{
   try {
      FileOutputStream fos = new FileOutputStream(property_file);
      property_set.storeToXML(fos,null);
      fos.close();
    }
   catch (IOException e) { 
      RebaseMain.logE("Problem writing preferences",e);
    }
}




}       // end of class RebasePreferences




/* end of RebasePreferences.java */

