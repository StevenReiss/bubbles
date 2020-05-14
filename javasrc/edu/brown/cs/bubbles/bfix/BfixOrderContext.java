/********************************************************************************/
/*                                                                              */
/*              BfixOrderContext.java                                           */
/*                                                                              */
/*      Hold an order context (list of sets of elements)                        */
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

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bfix;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;



class BfixOrderContext implements BfixConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<BfixOrderGroup>    content_items;
private OrderContext            order_context;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BfixOrderContext(Element xml)
{
   order_context = IvyXml.getAttrEnum(xml,"TYPE",OrderContext.CLASS);
   content_items = new ArrayList<BfixOrderGroup>();
   for (Element e : IvyXml.children(xml,"SET")) {
      BfixOrderGroup bos = new BfixOrderGroup(e);
      content_items.add(bos);
    }
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

BfixOrderSet findSetForElement(BfixOrderElement elt,OrderContext ctx) 
{
   if (ctx != null && ctx != order_context) return null;
   
   for (BfixOrderGroup bog : content_items) {
      BfixOrderSet bos = bog.findSetForElement(elt);
      if (bos != null) return bos;
    }
   
   return null;
}




}       // end of class BfixOrderContext




/* end of BfixOrderContext.java */

