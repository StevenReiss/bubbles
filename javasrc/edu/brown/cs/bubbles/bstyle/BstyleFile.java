/********************************************************************************/
/*                                                                              */
/*              BstyleFile.java                                                 */
/*                                                                              */
/*      Representation of an individual file                                    */
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



package edu.brown.cs.bubbles.bstyle;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.w3c.dom.Element;

import com.puppycrawl.tools.checkstyle.api.FileText;
import com.puppycrawl.tools.checkstyle.utils.CommonUtil;

import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.xml.IvyXml;

class BstyleFile implements BstyleConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BstyleMain      bstyle_main;
private IDocument       edit_document;
private IDocument       read_document;
private String          file_project;
private File            for_file;
private File            user_file;
private FileText        file_text;
private String          newline_string;
private boolean         has_errors;

private static AtomicInteger    edit_counter = new AtomicInteger();



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BstyleFile(BstyleMain bm,String proj,File f)
{
   bstyle_main = bm;
   user_file = f;
   for_file = IvyFile.getCanonical(f);
   file_project = proj;
   edit_document = null;
   read_document = null;
   file_text = null;
   newline_string = null;
   has_errors = false;
}



/********************************************************************************/
/*                                                                              */
/*      Access  methods                                                         */
/*                                                                              */
/********************************************************************************/

String getProject()                
{
   return file_project;
}


File getFile()
{
   return for_file;
}

File getUserFile()
{
   return user_file;
}

boolean getHasErrors()
{
   return has_errors;
}


boolean setHasErrors(boolean fg)
{
   boolean rslt = (fg != has_errors);
   
   has_errors = fg;
   if (fg) file_text = null;
   
   return rslt;
}


String getNewLineString()
{
   return newline_string;
}


synchronized FileText getFileText()      
{
   if (has_errors) return null;
   if (file_text != null) return file_text;
   
   if (edit_document != null) {
      String fulltext = edit_document.get();
      List<String> lines = new ArrayList<>();
      try (BufferedReader rdr = new BufferedReader(new StringReader(fulltext))) {
         for ( ; ; ) {
            String line = rdr.readLine();
            if (line == null) break;
            lines.add(line);
          }
       }
      catch (IOException e) { }
//    file_text = new FileText(for_file,lines);
      String [] linearr = lines.toArray(CommonUtil.EMPTY_STRING_ARRAY);
      file_text = new FileText(for_file,fulltext,linearr);
    }
   else {
      try {
         file_text = new FileText(for_file,"UTF-8");
       }
      catch (IOException e) {
         IvyLog.logE("BSTYLE","Problem reading source file " + for_file,e);
       }
    }
   
   return file_text;
}


Point getStartAndEndPosition(int lno,int coffset)
{
   String linetext = null;
   IDocument doc = edit_document;
   if (doc == null) {
      doc = read_document;
      if (doc == null) {
         try {
            String cnts = IvyFile.loadFile(for_file);
            read_document = new Document(cnts);
            doc = read_document;
          }
         catch (IOException e) {
            IvyLog.logE("BSTYLE","Problem reading source file " + for_file,e);
            return null;
          }
       }
    }
   
   if (doc != null) {
      try {
         int start = doc.getLineOffset(lno-1);
         int nxt = doc.getLineOffset(lno);
         linetext = doc.get(start,nxt-start);
         int cend = coffset+1;
         if (coffset < linetext.length()) {
            char c = linetext.charAt(coffset);
            if (Character.isJavaIdentifierPart(c)) {
               for (int i = coffset+1; i < linetext.length(); ++i) {
                  char c1 = linetext.charAt(i);
                  if (Character.isJavaIdentifierPart(c1)) cend = i+1;
                  else break;
                }
             }
          }
         return new Point(start+coffset,start+cend);
       }
      catch (BadLocationException e) {
         IvyLog.logE("BSTYLE","Problem converting  line/col to position");
       }
    }
   
   if (lno == 1 && coffset == 0) {
      return new Point(0,0);
    }
   
   return null;
}




/********************************************************************************/
/*                                                                              */
/*      Start file from buffer                                                  */
/*                                                                              */
/********************************************************************************/

synchronized void startFile()
{
   if (edit_document != null) return;
   
   CommandArgs args = new CommandArgs("FILE",for_file.getPath(),"CONTENTS",true);
   Element open = bstyle_main.sendCommandWithXmlReply("STARTFILE",
         file_project,args,null);
   if (file_project == null) file_project = IvyXml.getAttrString(open,"PROJECT");
   byte [] cnts = IvyXml.getBytesElement(open,"CONTENTS");
   String cn = new String(cnts);
   String linesep = IvyXml.getTextElement(open,"LINESEP");
   if (linesep != null) {
      if (linesep.equals("LF")) newline_string = "\n";
      else if (linesep.equals("CRLF")) newline_string = "\r\n";
      else if (linesep.equals("CR")) newline_string = "\r";
      else newline_string = "\n";
    }
   
   edit_document = new Document(cn);
   file_text = null;
   read_document = null;
}


/********************************************************************************/
/*                                                                              */
/*      Action methods                                                          */
/*                                                                              */
/********************************************************************************/

synchronized void editFile(int len,int off,String txt,boolean complete)
{
   if (edit_document == null) {
      try {
         String cnts = IvyFile.loadFile(for_file);
         edit_document = new Document(cnts);
         read_document = null;
       }
      catch (IOException e) {
         return;
       }
    }   
   
   edit_counter.incrementAndGet();
   
   if (complete) len = edit_document.getLength();
   try {
      edit_document.replace(off,len,txt);
    }
   catch (BadLocationException e) {
      IvyLog.logE("BSTYLE","Problem doing file edit",e);
    }
   
   file_text = null;
}



}       // end of class BstyleFile




/* end of BstyleFile.java */

