/********************************************************************************/
/*                                                                              */
/*              BvfmFileElement.java                                            */
/*                                                                              */
/*      Virtual fiel element (bubble in a group)                                */
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



package edu.brown.cs.bubbles.bvfm;

import org.w3c.dom.Element;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;
import edu.brown.cs.bubbles.buda.BudaConstants.BudaBubbleOutputer;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

abstract class BvfmFileElement implements BvfmConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

protected boolean       is_valid;
private Element         config_xml;


/********************************************************************************/
/*                                                                              */
/*      Static creation methods                                                 */
/*                                                                              */
/********************************************************************************/

static BvfmFileElement createFileElement(BudaBubble bbl)
{
   BvfmFileElement elt = null;
   
   switch (bbl.getContentType()) {
      case METHOD :
      case CLASS :
      case CLASS_ITEM : 
      case FIELD :
      case FILE :
          elt = new EditElement(bbl);
          break;
      case NONE :
      case OVERVIEW :
         BudaBubbleOutputer bbo = bbl.getBubbleOutputer();
         if (bbo != null) {
            elt = new MiscElement(bbl);
          }
         break;
      case NOTE :
         elt = new NoteElement(bbl);
         break;
    }
   
   if (elt != null && !elt.is_valid) elt = null;
   
   return elt;
}


static BvfmFileElement createFileElement(Element xml)
{
   BvfmFileElement elt = null;
   
   String typ = IvyXml.getAttrString(xml,"TYPE");
   if (typ == null) return null;
   switch (typ) {
      case "EDIT" :
         elt = new EditElement(xml);
         break;
      case "NOTE" :
         elt = new NoteElement(xml);
         break;
      case "MISC" :
         elt = new MiscElement(xml);
         break;
      default :
         BoardLog.logE("BVFM","Unknown file element type " + typ);
         break;
    }
   
   return elt;
}




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected BvfmFileElement(BudaBubble bbl) 
{
   this();
   BudaBubbleOutputer bbo = bbl.getBubbleOutputer();
   if (bbo != null) {
      try (BudaXmlWriter xw = new BudaXmlWriter()) {
         bbl.outputBubbleXml(xw);
         config_xml = IvyXml.convertStringToXml(xw.toString());
       }
    }
}


protected BvfmFileElement(Element xml)
{
   this();
   config_xml = IvyXml.getChild(xml,"BUBBLE");
}


private BvfmFileElement()
{
   is_valid = true;
   config_xml = null;
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getElementNameKey()                      { return null; }


BudaBubble createBubble(BudaBubbleArea bba)
{
   if (config_xml == null) return null;
   
   BudaRoot br = BudaRoot.findBudaRoot(bba);
   BudaBubble bbl = br.createConfigBubble(bba,config_xml);
   
   return bbl;
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

final void outputXml(IvyXmlWriter xw) 
{
   if (!is_valid) return;
   xw.begin("ELEMENT");
   localOutputXml(xw);
   xw.writeXml(config_xml);
   xw.end("ELEMENT");
}


protected abstract void localOutputXml(IvyXmlWriter xw);



/********************************************************************************/
/*                                                                              */
/*      Bale Editor File element                                                */
/*                                                                              */
/********************************************************************************/

private static class EditElement extends BvfmFileElement {
  
   private String content_project;
   private String content_file;
   private String content_name;
   
   EditElement(BudaBubble bbl) { 
      super(bbl);
      content_project = bbl.getContentProject();
      content_file = bbl.getContentFile().getPath();
      content_name = bbl.getContentName();
    }
   
   EditElement(Element xml) {
      super(xml);
      content_project = IvyXml.getAttrString(xml,"PROJECT");
      content_file = IvyXml.getAttrString(xml,"FILE");
      content_name = IvyXml.getAttrString(xml,"NAME");
    }
   
   @Override String getElementNameKey() {
      if (content_project == null || content_file == null || content_name == null) return null;
      return content_project + ":." + content_name;
    }
   
   @Override protected void localOutputXml(IvyXmlWriter xw) {
       xw.field("TYPE","EDIT");
       xw.field("PROJECT",content_project);
       xw.field("FILE",content_file);
       xw.field("NAME",content_name);
    }
   
}       // end of inner class EditElement



/********************************************************************************/
/*                                                                              */
/*      Note element for note bubbles                                           */
/*                                                                              */
/********************************************************************************/

private static class NoteElement extends BvfmFileElement {
   
   NoteElement(BudaBubble bbl) { 
      super(bbl);
    }
   
   NoteElement(Element xml) {
      super(xml);
    }
   
   @Override protected void localOutputXml(IvyXmlWriter xw) {
      xw.field("TYPE","NOTE");
    }
   
}       // end of inner class NoteElement



/********************************************************************************/
/*                                                                              */
/*      Miscellaneous element for other bubbles                                 */
/*                                                                              */
/********************************************************************************/

private static class MiscElement extends BvfmFileElement {

   MiscElement(BudaBubble bbl) { 
      super(bbl);
    }
   
   MiscElement(Element xml) {
      super(xml);
    }
   
   @Override protected void localOutputXml(IvyXmlWriter xw) {
      xw.field("TYPE","MISC");
    }
   
}       // end of inner class MiscElement




}       // end of class BvfmFileElement




/* end of BvfmFileElement.java */

