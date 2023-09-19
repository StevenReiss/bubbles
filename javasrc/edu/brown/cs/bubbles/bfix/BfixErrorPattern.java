/********************************************************************************/
/*                                                                              */
/*              BfixErrorPattern.java                                           */
/*                                                                              */
/*      Map error messages to regex and index                                   */
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



package edu.brown.cs.bubbles.bfix;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;

class BfixErrorPattern implements BfixConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Pattern                 regex_pattern;
private int                     use_index;
private String                  with_item;
private int                     alt_index;
private String                  altwith_item;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BfixErrorPattern(Element xml)
{
   String msg = IvyXml.getText(xml);
   Map<Integer,Integer> indexmap = new HashMap<>();
   boolean matchany = IvyXml.getAttrBool(xml,"ANYWEHRE");
   boolean matchstart = IvyXml.getAttrBool(xml,"START");
   regex_pattern = createPattern(msg,indexmap,matchany,matchstart);
   int use = IvyXml.getAttrInt(xml,"USE");
   if (use >= 0) use_index = indexmap.get(use);
   else use_index = -1;
   with_item = IvyXml.getAttrString(xml,"WITH");
   int alt = IvyXml.getAttrInt(xml,"ALT");
   if (alt >= 0) alt_index = indexmap.get(alt);
   else alt_index = -1;
   altwith_item = IvyXml.getAttrString(xml,"ALTWITH");
}



/********************************************************************************/
/*                                                                              */
/*      Matching methods                                                        */
/*                                                                              */
/********************************************************************************/

String getMatchResult(String msg)
{
   Matcher m = regex_pattern.matcher(msg);
   if (m.find()) {
      if (use_index > 0) return m.group(use_index);
      else if (with_item != null) return with_item;
      else return "";
    }
   
   return null;
}


String getAltResult(String msg)
{
   Matcher m = regex_pattern.matcher(msg);
   if (m.find()) {
      if (alt_index > 0) return m.group(use_index);
      else if (altwith_item != null) return altwith_item;
      else return "";
    }
   
   return null;
}


boolean testMatch(String msg)
{
   Matcher m = regex_pattern.matcher(msg);
   return m.find();
}


/********************************************************************************/
/*                                                                              */
/*      Convert error (output) message to pattern                               */
/*                                                                              */
/********************************************************************************/

private Pattern createPattern(String msg,Map<Integer,Integer> indexmap,
      boolean matchany,boolean matchstart)
{
   StringBuffer buf = new StringBuffer();
   
   if (matchstart) buf.append("^");
   
   int ctr = 1;
   for (int i = 0; i < msg.length(); ++i) {
      char c = msg.charAt(i);
      switch (c) {
         case '"' :
         case '\'' :
         case '*' :
         case '+' :
         case '[' :
         case ']' :
         case '(' :
         case ')' :
         case '|' :
         case '.' :
         case '^' :
         case '$' :
         case '?' :
            buf.append("\\");
            break; 
         case '\\' :
             buf.append(c);
             c  = msg.charAt(++i);
             break;
         case '{' :
            int which = msg.charAt(++i) - '0';
            c = msg.charAt(++i);
            while (c != '}') {
               which = 10*which + (c - '0');
               c = msg.charAt(++i);
             }
            buf.append("(.*)");
            indexmap.put(which,ctr++);
            c = 0;
            break;
       }
      if (c > 0) buf.append(c);
    }
   
   if (!matchany) buf.append("$");
   
   return Pattern.compile(buf.toString());
}

}       // end of class BfixErrorPattern




/* end of BfixErrorPattern.java */

