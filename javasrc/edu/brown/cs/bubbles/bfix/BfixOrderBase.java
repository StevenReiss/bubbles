/********************************************************************************/
/*                                                                              */
/*              BfixOrderOverall.java                                           */
/*                                                                              */
/*      Holder of overall ordering from learning                                */
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

import java.util.EnumMap;
import java.util.Map;



class BfixOrderBase implements BfixConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<OrderContext,BfixOrderContext>  context_orders;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BfixOrderBase(Element xml) 
{
   context_orders = new EnumMap<OrderContext,BfixOrderContext>(OrderContext.class);
   
   for (Element c : IvyXml.children(xml,"CONTEXT")) {
      OrderContext octx = IvyXml.getAttrEnum(c,"TYPE",OrderContext.CLASS);
      BfixOrderContext boc = new BfixOrderContext(c);
      context_orders.put(octx,boc);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

BfixOrderSet findSetForElement(BfixOrderElement elt)
{
   OrderContext ctx = null;
   for (BfixOrderElement p = elt.getParent(); p != null; p = p.getParent()) {
      switch (p.getElementType()) {
         case "CLASS" :
         case "ENUM" :
            if (p.getParent() == null) ctx = OrderContext.CLASS;
            else ctx = OrderContext.INNER_CLASS;
            break;
         case "INTERFACE" :
            if (p.getParent() == null) ctx = OrderContext.INTERFACE;
            else ctx = OrderContext.INNER_INTERFACE;
            break;
         default :
            break;
       }
      if (ctx != null) break;
    }
   if (ctx == null) return null;
   BfixOrderContext boc = context_orders.get(ctx);
   BfixOrderSet bos = boc.findSetForElement(elt,ctx);
   
   return bos;
}

}       // end of class BfixOrderOverall




/* end of BfixOrderOverall.java */

