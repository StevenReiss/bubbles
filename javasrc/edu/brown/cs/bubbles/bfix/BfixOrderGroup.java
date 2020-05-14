/********************************************************************************/
/*                                                                              */
/*              BfixOrderGroup.java                                             */
/*                                                                              */
/*      Group of elements that go together                                      */
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




class BfixOrderGroup implements BfixConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<BfixOrderSet>      group_elements;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BfixOrderGroup(Element xml)
{
   group_elements = new ArrayList<>();
   for (Element c : IvyXml.children(xml,"ELEMENT")) {
      BfixOrderSet bos = new BfixOrderSet(this,c);
      group_elements.add(bos);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

BfixOrderSet findSetForElement(BfixOrderElement elt)
{
   for (BfixOrderSet bos : group_elements) {
      if (bos.contains(elt)) return bos;
    }
   
   return null;
}

}       // end of class BfixOrderGroup




/* end of BfixOrderGroup.java */

