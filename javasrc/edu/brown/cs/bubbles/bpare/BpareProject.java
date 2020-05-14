/********************************************************************************/
/*										*/
/*		BpareProject.java						*/
/*										*/
/*	Project information holder for BPARE					*/
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



package edu.brown.cs.bubbles.bpare;

import edu.brown.cs.bubbles.board.BoardSetup;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;


class BpareProject implements BpareConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		project_name;
private File		workspace_dir;
private Collection<File> file_set;
private BpareTrie	pattern_trie;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BpareProject(Element xml)
{
   project_name = IvyXml.getAttrString(xml,"NAME");
   file_set = new HashSet<File>();
   String wsd = IvyXml.getAttrString(xml,"WORKSPACE");
   workspace_dir = null;
   if (wsd != null) workspace_dir = new File(wsd);
   pattern_trie = null;

   Element cps = IvyXml.getChild(xml,"FILES");
   // here we are typing a comment which apparently is fairly fast, which it shouldn't be
   for (Element cp : IvyXml.children(cps,"FILE")) {
      if (IvyXml.getAttrBool(cp,"SOURCE")) {
	 String fp0 = IvyXml.getTextElement(cp,"PATH");
	 File f0 = new File(fp0);
	 try {
	    f0 = f0.getCanonicalFile();
	 }
	 catch (IOException e) { }
	 if (f0.exists() && f0.canRead()) file_set.add(f0);
	 else {
	    String fp1 = IvyXml.getText(cp);
	    File f1 = new File(fp1);
	    try {
	       f1 = f1.getCanonicalFile();
	    }
	    catch (IOException e) { }
	    if (f1.exists() && f1.canRead()) file_set.add(f1);
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Access Methods								*/
/*										*/
/********************************************************************************/

String getName()			{ return project_name; }
Iterable<File> getSourceFiles() 	{ return file_set; }
boolean isValid()			{ return file_set.size() > 0; }
void setTrie(BpareTrie bt)		{ pattern_trie = bt; }
BpareTrie getTrie()			{ return pattern_trie; }

File getDataFile(String typ)
{
   File wd = BoardSetup.getBubblesWorkingDirectory(workspace_dir);
   wd = new File(wd,"bpare");
   if (!wd.exists()) wd.mkdirs();
   if (!wd.exists() || !wd.isDirectory() || !wd.canRead()) return null;

   String nm = "";
   if (typ != null) nm = "." + typ;

   // wd = new File("/ws/volfred/bpare");

   File df = new File(wd,project_name + nm + ".bpare");

   return df;
}



}	// end of class BpareProject




/* end of BpareProject.java */

