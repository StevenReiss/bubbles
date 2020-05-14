/********************************************************************************/
/*										*/
/*		BudaRelations.java						*/
/*										*/
/*	BUblles Display Area holder of relations that can be queried		*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.bubbles.buda;


import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;




class BudaRelations implements BudaConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<GroupSet>		group_sets;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BudaRelations()
{
   group_sets = new ArrayList<GroupSet>();
}




/********************************************************************************/
/*										*/
/*	Load methods								*/
/*										*/
/********************************************************************************/

void loadRelations(Element xml)
{
   Element ge = IvyXml.getChild(xml,"GROUPS");
   if (ge != null) {
      for (Element e : IvyXml.children(ge,"GROUP")) {
	 GroupSet gs = new GroupSet(e);
	 group_sets.add(gs);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void addGroup(BudaBubbleGroup bg)
{
   if (bg.getTitle() == null) return;

   GroupSet gs = new GroupSet(bg);
   group_sets.add(gs);
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(BudaXmlWriter xw)
{
   xw.begin("RELATIONS");
   xw.begin("GROUPS");
   for (GroupSet gs : group_sets) {
      gs.outputXml(xw);
    }
   xw.end("GROUPS");
   xw.end("RELATIONS");
}



/********************************************************************************/
/*										*/
/*	Class to hold a group set						*/
/*										*/
/********************************************************************************/

private static class GroupSet {

   private String group_name;
   private List<BubbleInfo> group_elements;

   GroupSet(Element e) {
      group_name = IvyXml.getAttrString(e,"NAME");
      group_elements = new ArrayList<>();
      for (Element ce : IvyXml.children(e,"ITEM")) {
         group_elements.add(new BubbleInfo(ce));
       }
    }

   GroupSet(BudaBubbleGroup bbg) {
      group_name = bbg.getTitle();
      group_elements = new ArrayList<BubbleInfo>();
      for (BudaBubble bb : bbg.getBubbles()) {
	 BubbleInfo bi = new BubbleInfo(bb);
	 if (bi.isValid()) group_elements.add(bi);
       }
    }

   void outputXml(IvyXmlWriter xw) {
      xw.begin("GROUP");
      xw.field("NAME",group_name);
      for (BubbleInfo bi : group_elements) {
	 bi.outputXml(xw);
       }
      xw.end("GROUP");
    }

}	// end of inner class GroupSet




/********************************************************************************/
/*										*/
/*	Class to hold information about a bubble				*/
/*										*/
/********************************************************************************/

private static class BubbleInfo {

   private String project_name;
   private String project_file;
   private String content_name;
   private BudaContentNameType name_type;

   BubbleInfo(BudaBubble bb) {
      project_name = bb.getContentProject();
      if (bb.getContentFile() == null) project_file = null;
      else project_file = bb.getContentFile().getPath();
      content_name = bb.getContentName();
      name_type = bb.getContentType();
    }

   BubbleInfo(Element e) {
      project_name = IvyXml.getAttrString(e,"PROJECT");
      project_file = IvyXml.getAttrString(e,"FILE");
      content_name = IvyXml.getAttrString(e,"NAME");
      name_type = IvyXml.getAttrEnum(e,"TYPE",BudaContentNameType.NONE);
    }

   void outputXml(IvyXmlWriter xw) {
      xw.begin("ITEM");
      xw.field("PROJECT",project_name);
      xw.field("FILE",project_file);
      xw.field("NAME",content_name);
      xw.field("TYPE",name_type);
      xw.end("ITEM");
    }

   boolean isValid() {
      return project_file != null && content_name != null &&
	 name_type != BudaContentNameType.NONE;
    }

}	// end of inner class BubbleInfo




}	// end of class BudaRelations




/* end of BudaRelations.java */
