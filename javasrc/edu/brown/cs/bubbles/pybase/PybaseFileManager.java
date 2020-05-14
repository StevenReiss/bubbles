/********************************************************************************/
/*										*/
/*		PybaseFileManager.java						*/
/*										*/
/*	Python Bubbles Base parser and ast manager				*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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


package edu.brown.cs.bubbles.pybase;


import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;



class PybaseFileManager implements PybaseConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/


private Map<File,FileData> file_data;

private static PybaseFileManager file_manager = null;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

synchronized static PybaseFileManager getFileManager()
{
   if (file_manager == null) {
      file_manager = new PybaseFileManager();
    }
   return file_manager;
}



private PybaseFileManager()
{
   file_data = new HashMap<File,FileData>();
}



/********************************************************************************/
/*										*/
/*	Get file buffer for a file						*/
/*										*/
/********************************************************************************/

IFileData getFileData(File f)
{
   return file_data.get(f);
}


IFileData getFileData(String fnm)
{
   return file_data.get(new File(fnm));
}



IFileData getNewFileData(File f,String nm,PybaseProject pp)
{
   FileData fd = file_data.get(f);

   if (fd == null) {
      fd = new FileData(f,nm,pp);
      file_data.put(f,fd);
    }

   return fd;
}


IFileData getFileData(IDocument d,File f)
{
   return new FileData(d,f);
}



int getFileOffset(File f,int line,int col)
{
   IFileData ifd = getFileData(f);
   if (ifd == null) return 0;

   IDocument doc = ifd.getDocument();
   try {
      int off = doc.getLineOffset(line-1);
      return off + col - 1;
    }
   catch (BadLocationException e) { }

   return 0;
}




/********************************************************************************/
/*										*/
/*	File/Document information holder					*/
/*										*/
/********************************************************************************/

private static class FileData implements IFileData {

   private String module_name;
   private File for_file;
   private IDocument use_document;
   private Map<Object,int []> position_data;
   private boolean has_changed;
   private long last_modified;

   FileData(File f,String nm,PybaseProject pp) {
      for_file = f;
      use_document = new Document();
      module_name = nm;
      loadFile();
      position_data = new IdentityHashMap<Object,int []>();
      has_changed = false;
      last_modified = 0;
    }

   FileData(IDocument d,File f) {
      for_file = f;
      use_document = d;
      module_name = null;
      position_data = new HashMap<Object,int []>();
      has_changed = false;
      last_modified = 0;
    }

   @Override public void reload() {
      int oln = use_document.getLength();
      try {
	 if (oln > 0) use_document.replace(0,oln,"");
       }
      catch (BadLocationException e) {
	 PybaseMain.logE("Problem removing all of document",e);
       }
      loadFile();
    }

   private void loadFile() {
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
         fr.close();
       }
      catch (IOException e) {
         PybaseMain.logE("Problem reading file",e);
       }
    }

   @Override public boolean commit(boolean refresh,boolean save) {
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
	  }
	 catch (IOException e) {
	    PybaseMain.logE("Problem saving file",e);
	  }
       }
      has_changed = false;
      return upd;
    }

   @Override public File getFile()				{ return for_file; }
   @Override public IDocument getDocument()			{ return use_document; }
   @Override public String getModuleName()			{ return module_name; }
   @Override public boolean hasChanged()			{ return has_changed; }
   @Override public void markChanged()				{ has_changed = true; }
   @Override public long getLastDateLastModified()		{ return last_modified; }

   @Override public void clearPositions()		{ position_data.clear(); }

   @Override public void setStart(Object o,int line,int col) {
      if (line == 0) return;
      int off = 0;
      try {
         off = use_document.getLineOffset(line-1) + col-1;
       }
      catch (BadLocationException e) {
         PybaseMain.logE("Bad location for start offset",e);
         return;
       }
      // System.err.println("SET START " + line + " " + col + " " + off + " " + o);
      int [] offs = position_data.get(o);
      if (offs == null) {
         offs = new int [2];
         position_data.put(o,offs);
       }
      offs[0] = off;
    }

   @Override public void setEnd(Object o,int line,int col) {
      if (line == 0) return;
      int off = 0;
      try {
	 off = use_document.getLineOffset(line-1) + col-1;
       }
      catch (BadLocationException e) {
	 PybaseMain.logE("Bad location for end offset",e);
	 return;
       }
      setEnd(o,off);
    }

   @Override public void setEndFromStart(Object o,int line,int col) {
      if (line == 0) return;
      int off = 0;
      try {
	 off = use_document.getLineOffset(line-1) + col-1;
	 if (col == 1) off -= 2;
	 else off -= 1;
       }
      catch (BadLocationException e) {
	 PybaseMain.logE("Bad location for end offset",e);
	 return;
       }
      setEnd(o,off);
    }

   @Override public void setEnd(Object o,int off) {
      // System.err.println("SET END " + off + " " + o);
      int [] offs = position_data.get(o);
      if (offs == null) {
	 offs = new int[2];
	 position_data.put(o,offs);
       }
      offs[1] = off;
    }

  @Override public int getStartOffset(Object o) {
     int [] offs = position_data.get(o);
     if (offs == null) return 0;
     return offs[0];
   }

  @Override public int getEndOffset(Object o) {
     int [] offs = position_data.get(o);
     if (offs == null) return 0;
     return offs[1];
   }

   @Override public int getLength(Object o) {
      int [] offs = position_data.get(o);
      if (offs == null) return 0;
      return offs[1] - offs[0] + 1;
    }

}	// end of class FileData




}	// end of class PybaseFileManager




/* end of PybaseFileManager.java */
