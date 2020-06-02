/********************************************************************************/
/*										*/
/*		BassConfigurator.java						*/
/*										*/
/*	Bubble Augmented Search Strategies class for handing configutations	*/
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bass;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;



class BassConfigurator implements BassConstants, BudaConstants.BubbleConfigurator
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
   boolean sv = IvyXml.getAttrBool(cnt,"STATIC");

   BudaBubble bb = null;

   if (typ == null) ;
   else if (typ.equals("SEARCH")) {
      BassFactory bf = BassFactory.getFactory();
      if (sv) {
	 bb = bf.createPackageExplorer(bba);
       }
      else {
	 String proj = IvyXml.getAttrString(cnt,"PROJECT");
	 String pfx = IvyXml.getAttrString(cnt,"PREFIX");
	 String txt = IvyXml.getAttrString(cnt,"TEXT");
	 BudaConstants.SearchType st = IvyXml.getAttrEnum(cnt,"TYPE",BudaConstants.SearchType.SEARCH_CODE);
	 bb = bf.createSearch(st,proj,pfx);
	 if (bb != null && txt != null) {
	    BassSearchBox sb = (BassSearchBox) bb.getContentPane();
	    sb.setDefaultText(txt);
	  }
       }
    }
   else if (typ.equals("TEXTSEARCH")) {
      BassFactory bf = BassFactory.getFactory();
      String proj = IvyXml.getAttrString(cnt,"PROJECT");
      bb = bf.createTextSearch(proj);
    }

   return bb;
}



@Override public boolean matchBubble(BudaBubble bb,Element xml) 
{
   Element cnt = IvyXml.getChild(xml,"CONTENT");
   String typ = IvyXml.getAttrString(cnt,"TYPE");
   
   if (typ == null) ;
   else if (typ.equals("SEARCH") && bb instanceof BassBubble) return true;
   else if (typ.equals("TEXTSEARCH") && bb instanceof BassTextBubble) return true;
   
   return false;
}
      


/********************************************************************************/
/*										*/
/*	I/O methods								*/
/*										*/
/********************************************************************************/

@Override public void outputXml(BudaXmlWriter xw,boolean history)
{
   if (!history) {
      xw.begin("BASSDATA");
      BassTextSearch.outputStatic(xw);
      xw.end("BASSDATA");
    }
}


@Override public void loadXml(BudaBubbleArea bba,Element root)
{
   Element e = IvyXml.getChild(root,"BASSDATA");
   if (e != null) {
      BassTextSearch.loadStatic(e);
    }
}



}	// end of class BassConfigurator




/* end of BassConfigurator.java */
