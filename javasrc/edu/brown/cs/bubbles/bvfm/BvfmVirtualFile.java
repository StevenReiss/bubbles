/********************************************************************************/
/*										*/
/*		BvfmVirtualFile.java						*/
/*										*/
/*	Holder of information about a virtual file (group)			*/
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



package edu.brown.cs.bubbles.bvfm;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleGroup;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class BvfmVirtualFile implements BvfmConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<BvfmFileElement> file_elements;
private String		      group_name;
private Point		      group_center;
private Color		      group_color;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BvfmVirtualFile(Element xml)
{
   this();
   group_name = IvyXml.getAttrString(xml,"NAME");
   group_center = new Point(IvyXml.getAttrInt(xml,"CENTERX"),
	 IvyXml.getAttrInt(xml,"CENTERY"));
   group_color = IvyXml.getAttrColor(xml,"COLOR");
   for (Element fexml : IvyXml.children(xml,"ELEMENT")) {
      BvfmFileElement bfe = BvfmFileElement.createFileElement(fexml);
      if (bfe != null) file_elements.add(bfe);
    }
}


BvfmVirtualFile(BudaBubbleGroup bbg)
{
   this();
   for (BudaBubble bb : bbg.getBubbles()) {
      BvfmFileElement fe = BvfmFileElement.createFileElement(bb);
      if (fe != null) file_elements.add(fe);
    }

   group_name = bbg.getTitle();
   if (group_name != null) {
      group_name = group_name.trim();
      if (group_name.length() == 0) group_name = null;
    }

   group_color = bbg.getColor();
   group_center = bbg.getCenter();
}


private BvfmVirtualFile()
{
   file_elements = new ArrayList<>();
   group_name = null;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

boolean isEmpty()				{ return file_elements.isEmpty(); }

int size()					{ return file_elements.size(); }

String getName()				{ return group_name; }

Iterable<BvfmFileElement> getElements()       { return file_elements; }



/********************************************************************************/
/*										*/
/*	Find common location of all bubbles in group				*/
/*										*/
/********************************************************************************/

String getCommonLocation()
{
   String pfx = null;
   for (BvfmFileElement fe : file_elements) {
      String s = fe.getElementNameKey();
      if (s == null) continue;
      if (pfx == null) pfx = s;
      else {
	 int match = -1;
	 for (int i = 0; i <= pfx.length(); ++i) {
	    char c = 0;
	    if (i == pfx.length()) c = '.';
	    else c = pfx.charAt(i);
	    char c1 = 0;
	    if (i == s.length()) c1 = '.';
	    else if (i > s.length()) break;
	    else c1 = s.charAt(i);
	    if (c != c1) break;
	    if (c == '.' || c == ':') match = i;
	  }
	 if (match < 0) return null;
	 pfx = pfx.substring(0,match);
       }
    }
   if (pfx != null) {
      int idx = pfx.indexOf("(");
      if (idx > 0) {
	 int idx1 = pfx.lastIndexOf(".",idx);
	 pfx = pfx.substring(0,idx1);
       }
      if (!pfx.endsWith(".")) pfx += ".";
    }

   return pfx;
}




/********************************************************************************/
/*										*/
/*	Ouptut methods								*/
/*										*/
/********************************************************************************/

void outputXml(IvyXmlWriter xw)
{
   xw.begin("VIRTUALFILE");
   xw.field("NAME",group_name);
   xw.field("CENTERX",group_center.x);
   xw.field("CENTERY",group_center.y);
   xw.field("COLOR",group_color);
   for (BvfmFileElement fe : file_elements) {
      fe.outputXml(xw);
    }
   xw.end("VIRTUALFILE");
}



}	// end of class BvfmVirtualFile




/* end of BvfmVirtualFile.java */

