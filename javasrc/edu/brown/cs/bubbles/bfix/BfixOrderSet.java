/********************************************************************************/
/*                                                                              */
/*              BfixOrderSet.java                                               */
/*                                                                              */
/*      Handle ordering a set of components                                     */
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


class BfixOrderSet implements BfixConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BfixOrderGroup          in_group;
private BfixOrderOrdering       ordering_algorithm;
private BfixOrderSelector       selector_algorithm;
private BfixOrderComment        prefix_comment;
private BfixOrderComment        between_comment;
private BfixOrderComment        suffix_comment;
private double                  javadoc_percent;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BfixOrderSet(BfixOrderGroup grp,Element xml) throws IllegalArgumentException
{
   in_group = grp;
   ordering_algorithm = null;
   selector_algorithm = null;
   prefix_comment = null;
   between_comment = null;
   suffix_comment = null;
   
   javadoc_percent = IvyXml.getAttrDouble(xml,"JAVADOC");
   
   Element order = IvyXml.getChild(xml,"ORDER");
   if (order != null) ordering_algorithm = BfixOrderOrdering.createOrdering(order);
   Element select = IvyXml.getChild(xml,"SELECTOR");
   if (select != null) selector_algorithm = new BfixOrderSelector(select);
   Element prefix = IvyXml.getChild(xml,"PREFIX");
   if (prefix != null) prefix_comment = new BfixOrderComment(prefix);
   Element between = IvyXml.getChild(xml,"BETWEEN");
   if (between != null) between_comment = new BfixOrderComment(between);
   Element suffix = IvyXml.getChild(xml,"SUFFIX");
   if (suffix != null) suffix_comment = new BfixOrderComment(suffix);
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

BfixOrderGroup   getGroup()                     { return in_group; }

BfixOrderComment getPrefixComment()             { return prefix_comment; }

BfixOrderComment getBetweenComment()            { return between_comment; }

BfixOrderComment getSuffixComment()             { return suffix_comment; }

boolean         getNeedsJavadoc()
{
   return javadoc_percent >= 0.25;
}



/********************************************************************************/
/*                                                                              */
/*      Content methods                                                         */
/*                                                                              */
/********************************************************************************/

boolean contains(BfixOrderElement elt)
{
   if (selector_algorithm == null) return true;
   return selector_algorithm.contains(elt);
}




int compareTo(BfixOrderElement b1,BfixOrderElement b2)
{
   if (ordering_algorithm == null) return -1;
   
   return ordering_algorithm.compareTo(b1,b2);
}


}       // end of class BfixOrderSet




/* end of BfixOrderSet.java */

