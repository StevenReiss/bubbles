/********************************************************************************/
/*										*/
/*		BaleConfigurator.java						*/
/*										*/
/*	Bubble Annotated Language Editor bubble configuration creator		*/
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bale;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.text.BadLocationException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;



class BaleConfigurator implements BaleConstants, BudaConstants.BubbleConfigurator,
	BudaConstants.PortConfigurator
{



/********************************************************************************/
/*										*/
/*	Bubble creation methods 						*/
/*										*/
/********************************************************************************/

@Override public BudaBubble createBubble(BudaBubbleArea bba,Element xml)
{
   Element cnt = IvyXml.getChild(xml,"CONTENT");
   String typ = IvyXml.getAttrString(cnt,"TYPE");
   if (typ == null) return null;

   BudaBubble bb = null;
   BaleFactory bf = BaleFactory.getFactory();

   if (typ.equals("FRAGMENT")) {
      BaleFragmentType ftyp = IvyXml.getAttrEnum(cnt,"FRAGTYPE",BaleFragmentType.NONE);
      String proj = IvyXml.getAttrString(cnt,"PROJECT");
      String name = IvyXml.getAttrString(cnt,"NAME");
      String filn = IvyXml.getAttrString(cnt,"FILE");
      File file = (filn == null ? null : new File(filn));
      if (name.endsWith("(...)")) {
         int idx1 = name.lastIndexOf("(");
         name = name.substring(0,idx1);
       }
      int idx = name.lastIndexOf(".");
      if (name.length() == 0) name = null;
      String head = name;
      if (name != null && idx > 0) head = name.substring(0,idx);
      switch (ftyp) {
	 case NONE :
	    break;
	 case ROFILE :
	    bb = bf.createFileBubble(proj,new File(head),null,null,0);
	    break;
	 case FILE :
	    bb = bf.createFileBubble(proj,file,head);
	    break;
	 case METHOD :
	    if (name != null) bb = bf.createMethodBubble(proj,name);
	    break;
	 case FIELDS :
	    bb = bf.createFieldsBubble(proj,file,head);
	    break;
	 case STATICS :
	    bb = bf.createStaticsBubble(proj,head,file);
	    break;
	 case MAIN :
	    bb = bf.createMainProgramBubble(proj,head,file);
	    break;
	 case HEADER :
	    bb = bf.createClassPrefixBubble(proj,file,head);
	    break;
	 case CLASS :
	    bb = bf.createClassBubble(proj,name);
	    break;
         case IMPORTS :
         case EXPORTS :
         case CODE :
            // TODO: do something here
            break;
	 case ROMETHOD:
	    break;
	 default:
	    break;
       }
    }

   Element eld = IvyXml.getChild(cnt, "ELISIONS");
   if (eld != null && bb != null) {
      List<BaleElisionData> elides = new ArrayList<BaleElisionData>();
      for (Element ex : IvyXml.children(eld,"ELISION")) {
	 ElideData ed = new ElideData(ex);
	 elides.add(ed);
       }
      BaleDocument bd = (BaleDocument) bb.getContentDocument();
      bd.applyElisions(elides);
    }

   return bb;
}


@Override public boolean matchBubble(BudaBubble bb,Element xml)
{
   Element cnt = IvyXml.getChild(xml,"CONTENT");
   String typ = IvyXml.getAttrString(cnt,"TYPE");

   if (typ.equals("FRAGMENT") && bb instanceof BaleEditorBubble) {
      BaleEditorBubble eb = (BaleEditorBubble) bb;
      String proj = IvyXml.getAttrString(cnt,"PROJECT");
      String file = IvyXml.getAttrString(cnt,"FILE");
      String name = IvyXml.getAttrString(cnt,"NAME");
      BaleDocument bd = (BaleDocument) eb.getContentDocument();
      if (bd.getFile().getPath().equals(file) &&
	    bd.getProjectName().equals(proj) &&
	    name.equals(bd.getFragmentName()))
	 return true;
    }

   return false;
}

/********************************************************************************/
/*										*/
/*	Port creation methods							*/
/*										*/
/********************************************************************************/

@Override public BudaConstants.LinkPort createPort(BudaBubble bb,Element xml)
{
   if (bb == null || !(bb instanceof BaleEditorBubble)) return null;

   BaleEditorBubble beb = (BaleEditorBubble) bb;
   BaleFragmentEditor bfe = (BaleFragmentEditor) beb.getContentPane();
   BaleDocument bd = bfe.getDocument();

   int lno = IvyXml.getAttrInt(xml,"LINE");
   int off = bd.findLineOffset(lno);

   try {
      return new BaleLinePort(beb,bd.createPosition(off),null);
    }
   catch (BadLocationException e) { }

   return null;
}



/********************************************************************************/
/*										*/
/*	I/O methods								*/
/*										*/
/********************************************************************************/

@Override public void outputXml(BudaXmlWriter xw,boolean history)	{ }
@Override public void loadXml(BudaBubbleArea bba,Element root)		{ }



/********************************************************************************/
/*										*/
/*	Elision information							*/
/*										*/
/********************************************************************************/

private static class ElideData implements BaleElisionData {

   private int start_offset;
   private int end_offset;
   private String element_name;

   ElideData(Element xml) {
      start_offset = IvyXml.getAttrInt(xml,"START");
      end_offset = IvyXml.getAttrInt(xml,"END");
      element_name = IvyXml.getAttrString(xml,"NAME");
    }

   @Override public int getStartOffset()	{ return start_offset; }
   @Override public int getEndOffset()		{ return end_offset; }
   @Override public String getElementName()	{ return element_name; }

}	// end of inner class ElideData



}	// end of class BaleConfigurator




/* end of BaleConfigurator.java */
