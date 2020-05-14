/********************************************************************************/
/*                                                                              */
/*              BucsUserFile.java                                               */
/*                                                                              */
/*      User data file information holder                                       */
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



package edu.brown.cs.bubbles.bucs;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.io.File;


class BucsUserFile implements BucsConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private File            user_file;
private String          access_name;
private String          context_name;
private UserFileType    file_mode;

private static int      file_counter = 0;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BucsUserFile(File f,String unm,UserFileType ft)
{
   this();
   set(f,unm,ft);
}


BucsUserFile() 
{
   user_file = null;
   access_name = null;
   file_mode = UserFileType.READ;
   
   ++file_counter;
   context_name = "6sUserFile_" + file_counter;
}



/********************************************************************************/
/*                                                                              */
/*      Setup Methods                                                           */
/*                                                                              */
/********************************************************************************/

void set(File local,String name,UserFileType ft)
{
   user_file = local;
   if (user_file != null && (name == null || name.length() == 0)) 
      name = user_file.getName();
   if (name.startsWith("/s6/")) {
      name = name.substring(4);
    }
   else if (name.startsWith("s:")) {
      name = name.substring(2);
    }
   access_name = name;
   file_mode = ft;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

File getFile()                                  { return user_file; }
String getFileName() 
{
   if (user_file == null) return null;
   return user_file.getPath();
}

String getAccessName()                          { return access_name; }
String getJarName()                             { return context_name; }
UserFileType getFileMode()                      { return file_mode; }

boolean isValid()
{
   return user_file != null && access_name != null;
}


/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

void addEntry(IvyXmlWriter xw) 
{
   xw.begin("USERFILE");
   xw.field("NAME",access_name);
   xw.field("JARNAME",context_name);
   xw.field("ACCESS",file_mode);
   xw.end("USERFILE");
}



@Override public String toString()
{
   return access_name + " <= " + user_file.getPath() + " (" + file_mode + ")";
}



}       // end of class BucsUserFile




/* end of BucsUserFile.java */

