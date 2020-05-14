/********************************************************************************/
/*										*/
/*		BussConfigurator.java						*/
/*										*/
/*	BUbble Stack Strategies bubble stack configuration handler		*/
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


package edu.brown.cs.bubbles.buss;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;



class BussConfigurator implements BussConstants, BudaConstants.BubbleConfigurator
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

   BudaBubble bb = null;

   if (typ.equals("STACK")) {
      // reconstruct bubble stack
    }

   return bb;
}


@Override public boolean matchBubble(BudaBubble bb,Element xml)         { return false; }


/********************************************************************************/
/*										*/
/*	I/O methods								*/
/*										*/
/********************************************************************************/

@Override public void outputXml(BudaXmlWriter xw,boolean history)	{ }
@Override public void loadXml(BudaBubbleArea bba,Element root)		{ }




}	// end of class BussConfigurator




/* end of BussConfigurator.java */

