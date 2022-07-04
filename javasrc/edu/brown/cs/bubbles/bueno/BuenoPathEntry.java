/********************************************************************************/
/*                                                                              */
/*              BuenoPathEntry.java                                             */
/*                                                                              */
/*      Information about a path entry                                          */
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



package edu.brown.cs.bubbles.bueno;

import java.io.File;

import javax.swing.filechooser.FileSystemView;

import org.w3c.dom.Element;

import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.board.BoardConstants.BoardLanguage;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;



class BuenoPathEntry implements Comparable<BuenoPathEntry>, BuenoConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private PathType path_type;
private String source_path;
private String output_path;
private String binary_path;
private String javadoc_path;
private boolean is_exported;
private boolean is_optional;
private boolean is_nested;
private boolean is_new;
private boolean is_modified;
private boolean is_excluded;
private int	   entry_id;

private static BoardLanguage cur_language = null;
private static final int PATH_LENGTH = 40;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BuenoPathEntry(Element e)
{
   if (cur_language == null) {
      cur_language = BoardSetup.getSetup().getLanguage();
    }
   
   path_type = IvyXml.getAttrEnum(e,"TYPE",PathType.NONE);
   source_path = IvyXml.getTextElement(e,"SOURCE");
   output_path = IvyXml.getTextElement(e,"OUTPUT");
   binary_path = IvyXml.getTextElement(e,"BINARY");
   javadoc_path = IvyXml.getTextElement(e,"JAVADOC");
   entry_id = IvyXml.getAttrInt(e,"ID",0);
   is_exported = IvyXml.getAttrBool(e,"EXPORTED");
   is_nested = IvyXml.getAttrBool(e,"NEST");
   is_optional = IvyXml.getAttrBool(e,"OPTIONAL");
   is_new = false;
   is_modified = false;
   
   if (cur_language == BoardLanguage.JS) {   
      if (source_path == null && binary_path != null) {
         source_path = binary_path;
         binary_path = null;
       }
      if (source_path ==  null) {
         // handle old style definitions
         source_path = IvyXml.getTextElement(e,"DIR");
         if (!IvyXml.getAttrBool(e,"USER")) path_type = PathType.LIBRARY;
         else if (IvyXml.getAttrBool(e,"EXCLUDE")) path_type = PathType.EXCLUDE;
         else path_type = PathType.SOURCE;
       }
    }
}


BuenoPathEntry(File f,PathType typ,boolean nest)
{
   path_type = typ;
   source_path = null;
   output_path = null;
   binary_path = null;
   if (f != null) {
      if (cur_language == BoardLanguage.JS) source_path = f.getPath();
      else if (path_type == PathType.LIBRARY) binary_path = f.getPath();
      else source_path = f.getPath();
    }
   is_exported = false;
   is_optional = false;
   is_nested = nest;
   is_new = true;
   is_modified = true;
   entry_id = 0;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

boolean isNested()				{ return is_nested; }
PathType getPathType()				{ return path_type; }
String getBinaryPath()				{ return binary_path; }
String getSourcePath()				{ return source_path; }
String getJavadocPath()			{ return javadoc_path; }
boolean isExported() 				{ return is_exported; }
boolean isOptional() 				{ return is_optional; }
boolean isExcluded()                            { return is_excluded; }
boolean hasChanged() 				{ return is_new || is_modified; }
boolean isLibrary()                             { return path_type == PathType.LIBRARY; }
boolean isRecursive()                           { return is_nested; }
  


void setBinaryPath(String p)
{
   if (p == null || p.length() == 0 || p.equals(binary_path)) return;
   binary_path = p;
   is_modified = true;
}

void setSourcePath(String p) 
{
   if (p == null || p.length() == 0 || p.equals(source_path)) return;
   source_path = p;
   is_modified = true;
}

void setJavadocPath(String p) 
{
   if (p == null || p.length() == 0 || p.equals(javadoc_path)) return;
   javadoc_path = p;
   is_modified = true;
}

void setExported(boolean fg) 
{
   if (fg == is_exported) return;
   is_exported = fg;
   is_modified = true;
}

void setOptional(boolean fg)
{
   if (fg == is_optional) return;
   is_optional = fg;
   is_modified = true;
}

void setType(PathType typ) 
{
   if (typ == path_type) return;
   path_type = typ;
   is_modified = true;
}

void setNested(boolean fg) 
{
   if (fg == is_nested) return;
   is_nested = fg;
   is_modified = true;
}



/********************************************************************************/
/*                                                                              */
/*      Comparison methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override public int compareTo(BuenoPathEntry pe)
{
   int cmp = toString().compareTo(pe.toString());
   if (cmp == 0) {
      cmp = binary_path.compareTo(pe.binary_path);
    }
   return cmp;
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

void outputXml(IvyXmlWriter xw,boolean del)
{
   xw.begin("PATH");
   if (del) xw.field("DELETE",true);
   if (entry_id != 0) xw.field("ID",entry_id);
   xw.field("TYPE",path_type);
   xw.field("NEW",is_new);
   xw.field("MODIFIED",is_modified);
   xw.field("EXPORTED",is_exported);
   xw.field("OPTIONAL",is_optional);
   xw.field("NEST",is_nested);
   if (source_path != null) xw.textElement("SOURCE",source_path);
   if (output_path != null) xw.textElement("OUTPUT",output_path);
   if (binary_path != null) xw.textElement("BINARY",binary_path);
   if (javadoc_path != null) xw.textElement("JAVADOC",javadoc_path);
   xw.end("PATH");
}



/********************************************************************************/
/*                                                                              */
/*      toString methods for path display                                       */
/*                                                                              */
/********************************************************************************/

@Override public String toString() {
   switch (cur_language) {
      default :
      case JAVA :
      case JAVA_IDEA :
      case REBUS: 
         return javaToString();
      case JS :
         return jsToString();
      case PYTHON :
         return pythonToString();
    }
}

private String javaToString()
{
   FileSystemView fsv = FileSystemView.getFileSystemView();
   switch (path_type) {
      case LIBRARY :
      case BINARY :
         if (binary_path == null) break;
         if (binary_path.length() <= PATH_LENGTH) return binary_path;
         File f = fsv.createFileObject(binary_path);
         String rslt = f.getName();
         for ( ; ; ) {
            File f1 = f.getParentFile();
            if (f1 == null) {
               rslt = File.separator + rslt;
               break;
             }
            String pname = f1.getName();
            String rslt1 = pname + File.separator + rslt;
            if (rslt1.length() >= PATH_LENGTH) {
               rslt = "..." + File.separator + rslt;
               break;
             }
            rslt = rslt1;
            f = f1;
          }
         return rslt;
      case SOURCE :
         if (source_path != null) {
            File f2 = fsv.createFileObject(source_path);
            return f2.getName() + " (SOURCE)";
          }
         break;
      default:
         break;
    }
   return path_type.toString() + " " + source_path + " " + output_path + " " + binary_path;
}


private String pythonToString() 
{
   return javaToString();
}


private String jsToString()
{
   FileSystemView fsv = FileSystemView.getFileSystemView();
   File f2 = fsv.createFileObject(source_path);
   String rslt = path_type.toString() + " " + f2.getName(); 
   if (is_nested) rslt += " (NESTED)";
   return rslt;
}



}       // end of class BuenoPathEntry




/* end of BuenoPathEntry.java */

