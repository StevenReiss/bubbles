/********************************************************************************/
/*										*/
/*		BvcrDifferenceFile.java 					*/
/*										*/
/*	Bubble Version Collaboration Repository differences for a file		*/
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

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;


class BvcrDifferenceFile implements BvcrConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<FileChange>	change_set;
private String			base_version;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BvcrDifferenceFile(String ver)
{
   change_set = new ArrayList<FileChange>();
   base_version = ver;
}




BvcrDifferenceFile(Element e)
{
   this(IvyXml.getAttrString(e,"VERSION"));

   for (Element ce : IvyXml.children(e,"CHANGE")) {
      FileChange fc = new FileChange(ce);
      change_set.add(fc);
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

List<BvcrFileChange> getChanges()
{
   return new ArrayList<BvcrFileChange>(change_set);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void addChange(int slin,int tlin,List<String> add,List<String> del)
{
   if (add.size() == 0 && del.size() == 0) return;

   FileChange fc = new FileChange(slin,tlin,add,del);
   change_set.add(fc);
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(IvyXmlWriter xw)
{
   xw.field("VERSION",base_version);

   for (FileChange fc : change_set) {
      fc.outputXml(xw);
    }
}




/********************************************************************************/
/*										*/
/*	File change representation						*/
/*										*/
/********************************************************************************/

private static class FileChange implements BvcrFileChange {

   private int source_line;
   private int target_line;
   private String [] delete_lines;
   private String [] add_lines;

   FileChange(int slno,int tlno,List<String> add,List<String> del) {
      source_line = slno;
      target_line = tlno;
      if (del.size() > 0) {
	 delete_lines = new String[del.size()];
	 delete_lines = del.toArray(delete_lines);
       }
      else delete_lines = null;
      if (add.size() > 0) {
	 add_lines = new String[add.size()];
	 add_lines = add.toArray(add_lines);
       }
      if (source_line < 0 || target_line < 0) {
	 System.err.println("BVCR: Bad source/target line " + slno + " " + tlno + " " + add.size() + " " + del.size());
	 Thread.dumpStack();
       }
    }

   @SuppressWarnings("unused") FileChange(Element e) {
      source_line = IvyXml.getAttrInt(e,"SOURCE");
      target_line = IvyXml.getAttrInt(e,"TARGET");
      delete_lines = null;
      add_lines = null;
      int dct = 0;
      for (Element ce : IvyXml.children(e,"DELETE")) ++dct;
      if (dct > 0) {
	 delete_lines = new String[dct];
	 int i = 0;
	 for (Element ce : IvyXml.children(e,"DELETE")) {
	    delete_lines[i++] = IvyXml.getText(ce);
	  }
       }
      int act = 0;
      for (Element ce : IvyXml.children(e,"INSERT")) ++act;
      if (act > 0) {
	 add_lines = new String[act];
	 int i = 0;
	 for (Element ce : IvyXml.children(e,"INSERT")) {
	    add_lines[i++] = IvyXml.getText(ce);
	  }
       }
    }

   @Override public int getSourceLine() 		{ return source_line; }
   @Override public int getTargetLine() 		{ return target_line; }
   @Override public String [] getDeletedLines() 	{ return delete_lines; }
   @Override public String [] getAddedLines()		{ return add_lines; }

   void outputXml(IvyXmlWriter xw) {
      String typ = null;
      if (add_lines != null && delete_lines != null) typ = "REPLACE";
      else if (add_lines != null) typ = "INSERT";
      else if (delete_lines != null) typ = "DELETE";
      if (typ == null) return;
      xw.begin("CHANGE");
      xw.field("TYPE",typ);
      xw.field("SOURCE",source_line);
      xw.field("TARGET",target_line);
      if (delete_lines != null) {
	 for (String s : delete_lines) {
	    xw.cdataElement("DELETE",s);
	  }
       }
      if (add_lines != null) {
	 for (String s : add_lines) {
	    xw.cdataElement("INSERT",s);
	  }
       }
      xw.end("CHANGE");
    }

}	// end of inner class FileChange



}	// end of class BvcrDifferenceFile




/* end of BvcrDifferenceFile.java */

