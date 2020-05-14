/********************************************************************************/
/*                                                                              */
/*              BfixOrderComment.java                                           */
/*                                                                              */
/*      Representation of blanks/comments in an set order                       */
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



class BfixOrderComment implements BfixConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String comment_template;
private int    blanks_before;
private int    blanks_after;

   

/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BfixOrderComment(Element xml) 
{
   blanks_before = IvyXml.getAttrInt(xml,"BEFORE",0);
   blanks_after = IvyXml.getAttrInt(xml,"AFTER",0);
   String cmmt = IvyXml.getText(xml,false);
   if (cmmt != null) {
      int idx = cmmt.indexOf("\n");
      if (idx >= 0 && Character.isWhitespace(cmmt.charAt(0))) {
         cmmt = cmmt.substring(idx+1);
       }
      if (!cmmt.endsWith("\n")) cmmt = cmmt + "\n";
      else {
         idx = cmmt.lastIndexOf("\n");
         if (idx > 0 && idx < cmmt.length()) {
            cmmt = cmmt.substring(0,idx+1);
          }
       }      
    }
   StringBuffer buf = new StringBuffer();
   for (int i = 0; i < blanks_before; ++i) buf.append("\n");
   if (cmmt != null) buf.append(cmmt);
   for (int i = 0; i < blanks_after; ++i) buf.append("\n");
   comment_template = buf.toString();
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getAddedText(BfixOrderNewElement elt) 
{
   return comment_template;
}



}       // end of class BfixOrderComment




/* end of BfixOrderComment.java */

