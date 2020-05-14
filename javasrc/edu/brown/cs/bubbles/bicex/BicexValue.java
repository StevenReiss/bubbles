/********************************************************************************/
/*                                                                              */
/*              BicexValue.java                                                 */
/*                                                                              */
/*      Generic representation of a value                                       */
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



package edu.brown.cs.bubbles.bicex;

import org.w3c.dom.Element;

import edu.brown.cs.bubbles.board.BoardLog;

import edu.brown.cs.ivy.xml.IvyXml;

import java.util.Collections;
import java.util.List;
import java.util.Map;


abstract class BicexValue implements BicexConstants
{


/********************************************************************************/
/*                                                                              */
/*      Factory methods                                                         */
/*                                                                              */
/********************************************************************************/

static BicexValue createRefValue(Element xml,Map<String,BicexBaseValue> knownvalues,
      Map<String,BicexBaseValue> prevvalues) 
{
   BicexValue rslt = null;
   BicexRefValue ref = null;
   long time0 = -1;
   
   String nm = IvyXml.getAttrString(xml,"NAME");
   if (nm == null) nm = IvyXml.getAttrString(xml,"INDEX");
   
   for (Element velt : IvyXml.children(xml,"VALUE")) {
      BicexBaseValue bv = createBaseValue(velt,knownvalues,prevvalues,nm);
      if (IvyXml.getAttrBool(velt,"NO_TOSTRING")) {
         String tsv = IvyXml.getText(xml);
         if (tsv != null && tsv.length() > 0) {
            bv = BicexBaseValue.createBaseValue(tsv);
          }
       }
      long tv = IvyXml.getAttrInt(velt,"TIME");
      if (rslt == null) {
         rslt = bv;
         time0 = tv;
       }
      else if (ref != null) {
         ref.setValueAt(tv,bv);
       }
      else {
         ref = new BicexRefValue(time0,(BicexBaseValue) rslt);
         ref.setValueAt(tv,bv);
         rslt = ref;
       }
    }
   
   if (nm != null && nm.equals("@toString")) {
      if (rslt == null) {
         String tsv = IvyXml.getText(xml);
         if (tsv != null && tsv.length() > 0) {
            rslt = BicexBaseValue.createBaseValue(tsv);
          }
       }
    }

   return rslt;
}




static BicexBaseValue createBaseValue(Element xml,Map<String,BicexBaseValue> knownvalues,
      Map<String,BicexBaseValue> prevvalues)
{
   return createBaseValue(xml,knownvalues,prevvalues,null);
}



static BicexBaseValue createBaseValue(Element xml,Map<String,BicexBaseValue> knownvalues,
      Map<String,BicexBaseValue> prevvalues,String name)
{
   if (IvyXml.getAttrBool(xml,"OBJECT") || IvyXml.getAttrBool(xml,"ARRAY")) {
      BicexBaseValue rslt = null;
      String oref = IvyXml.getAttrString(xml,"OREF");
      String id = IvyXml.getAttrString(xml,"ID");
      if (oref != null && prevvalues != null) {
         rslt = prevvalues.get(oref);
         if (rslt != null) {
            knownvalues.put(id,rslt);
            return rslt;
          }
         else 
            BoardLog.logE("BICEX", "Missing OREF " + oref);
       }
      rslt = knownvalues.get(id);
      if (rslt != null) return rslt;
    }
   
   return new BicexBaseValue(xml,knownvalues,prevvalues,name);
}


static BicexBaseValue createBaseValue(String s)
{
   return new BicexBaseValue(s);
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

boolean hasChildren(long when)                  { return false; }
Map<String,BicexValue> getChildren(long when)   { return null; }
String getStringValue(long when)                { return null; }
String getDataType(long when)                   { return null; }
String getFullName()                            { return null; }

String getTooltipValue(long when)               { return null; }

List<Integer> getTimeChanges()                  { return Collections.singletonList(0); }
long getUpdateTime(long t)                      { return -1; }

boolean isInitializable()                       { return false; }

boolean isComponent(long when)                  { return false; }



}       // end of interface BicexValue




/* end of BicexValue.java */

