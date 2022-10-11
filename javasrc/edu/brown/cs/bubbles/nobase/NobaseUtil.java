/********************************************************************************/
/*										*/
/*		NobaseUtil.java 						*/
/*										*/
/*	Utility and output methods for NOBASE					*/
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

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.wst.jsdt.core.dom.ASTNode;

import java.awt.Point;
import java.io.File;
import java.util.List;


abstract class NobaseUtil implements NobaseConstants
{



/********************************************************************************/
/*										*/
/*	Problem output methods							*/
/*										*/
/********************************************************************************/

static void outputProblem(NobaseMessage m,ISemanticData isd,IvyXmlWriter xw)
   {
      NobaseFile ifd = isd.getFileData();
      IDocument doc = ifd.getDocument();
      int sln = m.getStartLine(doc);
      int scl = m.getStartCol(doc);
      int eln = m.getEndLine(doc);
      int ecl = m.getEndCol(doc);
      String msg = m.getMessage();
      List<String> ls = m.getAdditionalInfo();

      xw.begin("PROBLEM");
      xw.field("PROJECT",isd.getProject().getName());
      xw.field("FILE",ifd.getFile().getPath());
     // xw.field("MSGID",m.getType());
      xw.field("MESSAGE",msg);
      xw.field("LINE",sln);

      switch (m.getSeverity()) {
	 case INFO :
	 default :
	    break;
	 case WARNING :
	    xw.field("WARNING",true);
	    break;
	 case ERROR :
	    xw.field("ERROR",true);
	    break;
       }

      try {
	 if (sln != 0) {
	    xw.field("START",doc.getLineOffset(sln-1) + scl - 1);
	    xw.field("END",doc.getLineOffset(eln-1) + ecl - 1);
	  }
       }
      catch (BadLocationException e) { }

      if (ls != null) {
	 for (String s : ls) {
	    xw.textElement("ARG",s);
	  }
       }
      xw.end("PROBLEM");
    }



/********************************************************************************/
/*										*/
/*	Symbol otuput methods							*/
/*										*/
/********************************************************************************/

static void outputProjectSymbol(NobaseProject pp,IvyXmlWriter xw)
{
   xw.begin("ITEM");
   xw.field("TYPE","Project");
   xw.field("NAME",pp.getName());
   xw.field("PROJECT",pp.getName());
   xw.field("PATH",pp.getBasePath().getAbsolutePath());
   xw.field("SOURCE","USERSOURCE");
   xw.field("KEY",pp.getName() + "@");
   xw.end("ITEM");
}


static void outputModuleSymbol(NobaseProject pp,IvyXmlWriter xw,NobaseFile file,
      ASTNode root)
{
   File f = file.getFile();
   xw.begin("ITEM");
   if (pp != null) xw.field("PROJECT",pp.getName());
   xw.field("PATH",file.getFile().getAbsolutePath());
   String tnm = file.getModuleName();
   xw.field("NAME",tnm);
   xw.field("TYPE","Module");
   if (f.exists()) {
      // ensure we get the whole file in all cases
      xw.field("LINE",1);
      xw.field("COL",1);
      xw.field("STARTOFFSET",0);
      int len = (int) f.length();
      xw.field("ENDOFFSET",len-1);
      xw.field("LENGTH",len);
    }
   else if (root != null) {
      xw.field("LINE",NobaseAst.getLineNumber(root));
      xw.field("COL",NobaseAst.getColumn(root));
      int off1 = root.getStartPosition();
      int off2 = off1 + root.getLength();
      xw.field("STARTOFFSET",off1);
      xw.field("ENDOFFSET",off2);
      xw.field("LENGTH",off2-off1+1);
    }
   String pnm = (pp == null ? "" : pp.getName() + ":");
   xw.field("QNAME",tnm);
   xw.field("HANDLE",pnm + tnm);
   xw.end("ITEM");
}



static void outputName(NobaseSymbol nm,IvyXmlWriter xw)
{
   NobaseFile file = nm.getFileData();

   if (nm.getNameType() == NameType.MODULE) {
      outputModuleSymbol(nm.getProject(),xw,file,nm.getDefNode());
      return;
    }

   xw.begin("ITEM");
   xw.field("PROJECT",nm.getProject().getName());
   xw.field("PATH",file.getFile().getAbsolutePath());
   String dnm = nm.getName();
   xw.field("NAME",dnm);
   switch (nm.getNameType()) {
      case MODULE :
	 xw.field("TYPE","Module");
	 break;
      case FUNCTION :
	 xw.field("TYPE","Function");
	 break;
      case CLASS :
         xw.field("TYPE","Class");
         break;
      case LOCAL :
	 xw.field("TYPE","Local");
	 break;
      case VARIABLE :
	 xw.field("TYPE","Variable");
	 break;
    }
   ASTNode root = nm.getDefNode();
   if (root != null) {
      xw.field("LINE",NobaseAst.getLineNumber(root));
      xw.field("COL",NobaseAst.getColumn(root));
      int off1x = root.getStartPosition();
      int off2x = off1x + root.getLength();
      Point pt =  NobaseAst.getExtendedPosition(root,file);
      int off1 = pt.x;
      int off2 = pt.y;
      xw.field("STARTOFFSET",off1);
      xw.field("ENDOFFSET",off2);
      xw.field("LENGTH",off2-off1+1);
      if (off1 != off1x || off2 != off2x) {
         NobaseMain.logD("OFFSETS DIFFER " + nm.getName() + " " + 
               off1 + " " + off1x + " " + off2 + " " + off2x);
       }
    }
   xw.field("QNAME",nm.getBubblesName());
   xw.field("HANDLE",nm.getHandle());
   xw.end("ITEM");
}






/********************************************************************************/
/*										*/
/*	Search support								*/
/*										*/
/********************************************************************************/

/********************************************************************************/
/*										*/
/*	Handle matching 							*/
/*										*/
/********************************************************************************/

static String convertWildcardToRegex(String s)
{
   if (s == null) return null;

   StringBuffer nb = new StringBuffer(s.length()*8);
   int brct = 0;
   boolean qtfg = false;
   boolean bkfg = false;
   String star = null;

   star = "\\w*";

   nb.append('^');

   for (int i = 0; i < s.length(); ++i) {
      char c = s.charAt(i);
      if (bkfg) {
	 if (c == '\\') qtfg = true;
	 else if (!qtfg && c == ']') bkfg = false;
	 else { nb.append(c); qtfg = false; continue; }
       }
      if (c == '/' || c == '\\') {
	 if (File.separatorChar == '\\') nb.append("\\\\");
	 else nb.append(File.separatorChar);
       }
      else if (c == '@') nb.append(".*");
      else if (c == '*') nb.append(star);
      else if (c == '.') nb.append("\\.");
      else if (c == '{') { nb.append("("); ++brct; }
      else if (c == '}') { nb.append(")"); --brct; }
      else if (brct > 0 && c == ',') nb.append('|');
      else if (c == '?') nb.append(".");
      else if (c == '[') { nb.append(c); bkfg = true; }
      else nb.append(c);
    }

   nb.append('$');

   return nb.toString();
}


/********************************************************************************/
/*                                                                              */
/*      Scan numbers from strings                                               */
/*                                                                              */
/********************************************************************************/

static Number convertStringToNumber(String nv)
{
   if (nv == null) return Long.valueOf(0);
   
   Number val = null;
   int radix = 10;
   if (nv.startsWith("0x") || nv.startsWith("0X")) {
      radix = 16;
      nv = nv.substring(2);
    }
   else if (nv.startsWith("0o") || nv.startsWith("0O")) {
      radix = 8;;
      nv = nv.substring(2);
    }
   else if (nv.startsWith("0b") || nv.startsWith("0B")) {
      radix = 2;
      nv = nv.substring(2);
    }
   else if (nv.contains(".") || nv.contains("e") || nv.contains("E")) {
      radix = 0;
    }
   
   try {
      if (radix == 0) val = Double.valueOf(nv);
      else val = Long.valueOf(nv,radix);
    }
   catch (NumberFormatException e) {
      NobaseMain.logD("Bad number value " + nv);
      val = Long.valueOf(0);
    }
   
   return val;
}




}	// end of class NobaseUtil




/* end of NobaseUtil.java */

