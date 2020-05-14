/********************************************************************************/
/*										*/
/*		NobaseFile.java 						*/
/*										*/
/*	Implementation of a file						*/
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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


class NobaseFile implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String module_name;
private File for_file;
private NobaseProject for_project;
private IDocument use_document;
private boolean has_changed;
private long last_modified;
private boolean is_library;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseFile(File f,String nm,NobaseProject pp)
{
   for_file = f;
   for_project = pp;
   use_document = new Document();
   module_name = nm;
   loadFile();
   has_changed = false;
   last_modified = 0;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

File getFile()				{ return for_file; }
NobaseProject getProject()		{ return for_project; }
IDocument getDocument() 		{ return use_document; }
String getModuleName()			{ return module_name; }
boolean hasChanged()			{ return has_changed; }
void markChanged()			{ has_changed = true; }
long getLastDateLastModified()		{ return last_modified; }
String getFileName()			{ return for_file.getPath(); }
boolean isLibrary()			{ return is_library; }
void setIsLibrary(boolean fg)		{ is_library = fg; }
String getContents()			{ return use_document.get(); }



/********************************************************************************/
/*										*/
/*	Handle positions							*/
/*										*/
/********************************************************************************/

int getLineNumber(int offset)
{
   if (use_document == null) return 0;
   try {
      return use_document.getLineOfOffset(offset)+1;
    }
   catch (BadLocationException e) {
      NobaseMain.logE("Bad line offset " + offset + " " + use_document.getLength());
    }
   return 0;
}

int getCharPosition(int offset)
{
   if (use_document == null) return 0;
   try {
      int lno = use_document.getLineOfOffset(offset);
      int lstart = use_document.getLineOffset(lno);
      return offset-lstart+1;
    }
   catch (BadLocationException e) {
      NobaseMain.logE("Bad line offset " + offset + " " + use_document.getLength());
    }
   return 0;
}


/********************************************************************************/
/*										*/
/*	Load and save methods							*/
/*										*/
/********************************************************************************/

boolean reload()
{
   if (for_file.lastModified() <= last_modified) return false;

   loadFile();

   return true;
}



private void loadFile()
{
   try (FileReader fr = new FileReader(for_file)) {
      last_modified = for_file.lastModified();
      StringBuffer fbuf = new StringBuffer();
      char [] buf = new char[4096];
      for ( ; ; ) {
	 int sts = fr.read(buf);
	 if (sts < 0) break;
	 fbuf.append(buf,0,sts);
       }
      use_document.set(fbuf.toString());
    }
   catch (IOException e) {
      NobaseMain.logE("Problem reading file",e);
    }
}



boolean commit(boolean refresh,boolean save)
{
   boolean upd = false;
   if (refresh) {
      long lm = for_file.lastModified();
      if (lm > last_modified) {
	 loadFile();
	 upd = true;
       }
    }
   else if (save && has_changed) {
      try {
	 FileWriter fw = new FileWriter(for_file);
	 fw.write(use_document.get());
	 fw.close();
	 last_modified = for_file.lastModified();
	 upd = true;
       }
      catch (IOException e) {
	 NobaseMain.logE("Problem saving file",e);
       }
    }
   has_changed = false;
   return upd;
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   return for_file.getPath();
}


}	// end of class NobaseFile




/* end of NobaseFile.java */

