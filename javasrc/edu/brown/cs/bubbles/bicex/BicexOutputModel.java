/********************************************************************************/
/*                                                                              */
/*              BicexOutputModel.java                                           */
/*                                                                              */
/*      Hold information about output files                                     */
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



package edu.brown.cs.bubbles.bicex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;

class BicexOutputModel implements BicexConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<Integer,FileData>   file_map;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BicexOutputModel() 
{
   file_map = new HashMap<>();
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

List<FileData> getOutputFiles()
{
   return new ArrayList<FileData>(file_map.values());
}



/********************************************************************************/
/*                                                                              */
/*      Update methods                                                          */
/*                                                                              */
/********************************************************************************/
 
void update(Element xml)
{
   if (xml == null) return;
   if (!IvyXml.isElement(xml,"IOMODEL")) {
      xml = IvyXml.getChild(xml,"IOMODEL");
    }
   file_map.clear();
   for (Element oute : IvyXml.children(xml,"OUTPUT")) {
      FileData fd = new FileData(oute);
      file_map.put(fd.getFileDescriptor(),fd);
    }
}



/********************************************************************************/
/*                                                                              */
/*      File Information holder                                                 */
/*                                                                              */
/********************************************************************************/

static class FileData {
   
   private int file_fd;
   private String file_path;
   private boolean is_binary;
   private List<WriteData> file_writes;
   private long last_time;
   private String last_content;
   
   FileData(Element xml) {
      file_fd = IvyXml.getAttrInt(xml,"FD");
      file_path = IvyXml.getAttrString(xml,"PATH");
      is_binary = IvyXml.getAttrBool(xml,"BINARY");
      file_writes = new ArrayList<>();
      for (Element we : IvyXml.children(xml,"WRITE")) {
         WriteData wd = new WriteData(we,is_binary);
         file_writes.add(wd);
       }
      last_time = -1;
      last_content = null;
    }
   
   int getFileDescriptor()                      { return file_fd; }
   String getFilePath()                         { return file_path; }
   String getFileContents(long when) {
      if (last_time == when) return last_content;
      last_time = when;
      StringBuilder buf = new StringBuilder();
      for (WriteData wd : file_writes) {
         if (wd.getTime() > when) break;
         buf.append(wd.getContent());
       }
      last_content = buf.toString();
      return last_content;
    }
   
   @Override public String toString() {
      if (file_path != null) {
         return file_path + " (" + file_fd + ")";
       }
      else return "(" + file_fd + ")";
    }
   
}       // end of inner class FileData



private static class WriteData {
   
   private long write_time;
   private String write_text;
   
   WriteData(Element xml,boolean bin) {
      write_time = IvyXml.getAttrLong(xml,"WHEN");
      write_text = IvyXml.getTextElement(xml,"DATA");
    }
   
   long getTime()                               { return write_time; }
   String getContent()                          { return write_text; }
   
}       // end of inner class WriteData




}       // end of class BicexOutputModel




/* end of BicexOutputModel.java */

