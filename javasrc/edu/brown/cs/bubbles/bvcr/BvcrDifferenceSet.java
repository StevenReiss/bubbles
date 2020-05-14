/********************************************************************************/
/*										*/
/*		BvcrDifferenceSet.java						*/
/*										*/
/*	Bubble Version Collaboration Repository set of file differences 	*/
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

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


class BvcrDifferenceSet implements BvcrConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<File,FileData>	file_set;
private BvcrMain		bvcr_main;
private BvcrProject		for_project;

private BvcrDifferenceFile	file_diffs;
private int			source_start;
private int			target_start;
private List<String>		del_lines;
private List<String>		add_lines;
private Set<File>		files_todo;
private String			start_version;
private String			end_version;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BvcrDifferenceSet(BvcrMain bm,BvcrProject proj)
{
   bvcr_main = bm;
   for_project = proj;
   file_set = new HashMap<File,FileData>();
   file_diffs = null;
   source_start = 0;
   del_lines = null;
   add_lines = null;
   files_todo = null;
   start_version = null;
   end_version = null;
}




/********************************************************************************/
/*										*/
/*	Methods for managing files						*/
/*										*/
/********************************************************************************/

void handleFileChanged(File f)
{
   if (files_todo == null) return;	// will do everything
   files_todo.add(f);
}


boolean computationNeeded()
{
   if (files_todo == null) return true;
   return files_todo.size() > 0;
}


List<File> getFilesToCompute()
{
   if (files_todo == null) return null; 	// indicate all

   ArrayList<File> rslt = new ArrayList<File>(files_todo);
   return rslt;
}



/********************************************************************************/
/*										*/
/*	Handle specific version for specific file				*/
/*										*/
/********************************************************************************/

void setForFileDifference(File f,String v0,String v1)
{
   files_todo = new HashSet<File>();
   files_todo.add(f);
   start_version = v0;
   end_version = v1;
}


String getStartVersion()			{ return start_version; }
String getEndVersion()				{ return end_version; }




/********************************************************************************/
/*										*/
/*	Methods for setting up a difference set 				*/
/*										*/
/********************************************************************************/

void beginFile(String file,String ver)
{
   finishFile();
   source_start = 0;
   target_start = 0;
   add_lines = new ArrayList<String>();
   del_lines = new ArrayList<String>();
   File f = new File(file);
   FileData fd = new FileData(f,ver);
   file_diffs = fd.getDifferences();
   file_set.put(f,fd);
   if (files_todo != null) files_todo.remove(f);
}


void finish()
{
   finishFile();

   file_diffs = null;
   source_start = 0;
   del_lines = null;
   add_lines = null;
   if (files_todo == null) files_todo = new HashSet<File>();
}


void noteDelete(int slin,int tlin,String txt)
{
   if (source_start > 0 && source_start + del_lines.size() == slin) {
      del_lines.add(txt);
      return;
    }

   finishEntry();
   source_start = slin;
   target_start = tlin;
   del_lines.add(txt);
}



void noteInsert(int slin,int tlin,String txt)
{
   if (target_start > 0 && target_start + add_lines.size() == tlin) {
      add_lines.add(txt);
      return;
    }

   finishEntry();
   source_start = slin;
   target_start = tlin;
   add_lines.add(txt);
}



void finishFile()
{
   finishEntry();
   file_diffs = null;
}



private void finishEntry()
{
   if (source_start == 0 || file_diffs == null) return;

   file_diffs.addChange(source_start,target_start,add_lines,del_lines);

   source_start = 0;
   target_start = 0;
   add_lines.clear();
   del_lines.clear();
}



/********************************************************************************/
/*										*/
/*	File methods								*/
/*										*/
/********************************************************************************/

String getRelativePath(File f)
{
   String f1 = f.getPath();
   File pf1 = bvcr_main.getRootDirectory(for_project.getName());
   String p1 = pf1.getPath();
   if (f1.startsWith(p1)) {
      int ln = p1.length();
      f1 = f1.substring(ln);
    }
   else {
      System.err.println("BVCR: File " + f + " not in source directory " + p1);
    }

   return f1;
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(IvyXmlWriter xw)
{
   xw.begin("DIFFERENCES");
   xw.field("PROJECT",for_project.getName());
   xw.field("USER",System.getProperty("user.name"));
   xw.field("ROOT",bvcr_main.getRootDirectory(for_project.getName()));

   for (FileData fd : file_set.values()) {
      fd.outputXml(xw);
    }

   xw.end("DIFFERENCES");
}




/********************************************************************************/
/*										*/
/*	File information							*/
/*										*/
/********************************************************************************/

private class FileData {

   private File for_file;
   private long last_check;
   private BvcrDifferenceFile data_diffs;

   FileData(File f,String version) {
      for_file = f;
      last_check = f.lastModified();
      data_diffs = new BvcrDifferenceFile(version);
    }

   // long getLastChecked()			{ return last_check; }
   BvcrDifferenceFile getDifferences()		{ return data_diffs; }

   void outputXml(IvyXmlWriter xw) {
      xw.begin("FILE");
      String fd = getRelativePath(for_file);
      xw.field("NAME",fd);
      xw.field("DLM",last_check);
      data_diffs.outputXml(xw);
      xw.end("FILE");
    }

}	// end of inner class FileData



}	// end of class BvcrDifferenceSet




/* end of BvcrDifferenceSet.java */
