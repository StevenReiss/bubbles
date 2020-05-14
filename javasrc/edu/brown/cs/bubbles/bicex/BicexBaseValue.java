/********************************************************************************/
/*										*/
/*		BicexValue.java 						*/
/*										*/
/*	Holder of a returned value						*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
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

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bicex;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;



class BicexBaseValue extends BicexValue implements BicexConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String	value_type;
private String	value_text;
private Map<String,BicexValue> sub_values;
private BicexValue [] array_values;
private boolean can_initialize;
private boolean is_component;
private String full_name;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BicexBaseValue(Element xml,Map<String,BicexBaseValue> knownvalues,Map<String,BicexBaseValue> prevvalues,
      String name)
{
   value_type = IvyXml.getAttrString(xml,"TYPE");
   value_text = null;
   sub_values = null;
   array_values = null;
   can_initialize = IvyXml.getAttrBool(xml,"CANINIT");
   is_component = IvyXml.getAttrBool(xml,"COMPONENT");
   full_name = IvyXml.getAttrString(xml,"NAME");
   if (full_name == null) full_name = name;

   if (IvyXml.getAttrBool(xml,"NULL")) {
      value_text = "null";
    }
   else if (IvyXml.getAttrBool(xml,"OBJECT")) {
      String id = IvyXml.getAttrString(xml,"ID");
      if (id != null) knownvalues.put(id,this);
      sub_values = new TreeMap<>();
      for (Element fld : IvyXml.children(xml,"FIELD")) {
	 String nm = IvyXml.getAttrString(fld,"NAME");
	 int idx = nm.lastIndexOf(".");
	 if (idx > 0) nm = nm.substring(idx+1);
	 BicexValue cv = BicexValue.createRefValue(fld,knownvalues,prevvalues);
	 if (cv != null) {
	    sub_values.put(nm,cv);
	  }
       }
      value_text = value_type + " (" + id + ")";
    }
   else if (IvyXml.getAttrBool(xml,"ARRAY")) {
      String id = IvyXml.getAttrString(xml,"ID");
      if (id != null) knownvalues.put(id,this);
      int dim = IvyXml.getAttrInt(xml,"SIZE");
      if (dim < 0) dim = 0;
      array_values = new BicexValue[dim];
      BicexValue dflt = null;
      for (Element aelt : IvyXml.children(xml,"ELEMENT")) {
	 if (IvyXml.getAttrBool(aelt,"DEFAULT")) {
	    Element velt = IvyXml.getChild(aelt, "VALUE");
	    dflt = BicexValue.createBaseValue(velt,knownvalues,prevvalues);
	    continue;
	  }
	 int idx = IvyXml.getAttrInt(aelt,"INDEX");
	 BicexValue cv = BicexValue.createRefValue(aelt,knownvalues,prevvalues);
	 if (cv != null) {
	    array_values[idx] = cv;
	  }
       }
      if (dflt != null) {
	 for (int i = 0; i < dim; ++i) {
	    if (array_values[i] == null) {
	       array_values[i] = dflt;
	     }
	  }
       }
      value_text = value_type + "[" + dim + "]";
      if (id != null) value_text += " (" + id + ")";
    }
   else if (value_type.equals("boolean")) {
      value_text = IvyXml.getText(xml);
      if (value_text == null || value_text.equals("0")) value_text = "false";
      else value_text = "true";
    }
   else {
      value_text = IvyXml.getText(xml);
    }
}


BicexBaseValue(String s)
{
   value_type = "java.lang.String";
   value_text = s;
   sub_values = null;
   array_values = null;
   can_initialize = false;
   is_component = false;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override boolean hasChildren(long when)
{
   if (sub_values != null || array_values != null) return true;
   return false;
}


@Override Map<String,BicexValue> getChildren(long when)
{
   if (sub_values == null && array_values != null) {
      new LinkedHashMap<String,BicexValue>();
      Map<String,BicexValue> rslt = new LinkedHashMap<>();
      for (int i = 0; i < array_values.length; ++i) {
	 if (array_values[i] != null) {
	    rslt.put("[" + i + "]",array_values[i]);
	  }
       }
      sub_values = rslt;
    }

   return sub_values;
}

@Override String getStringValue(long when)
{
   switch (value_type) {
      case "char" :
	 try {
	    int iv = Integer.parseInt(value_text);
	    return "'" + ((char) iv) + "' " + value_text;
	  }
	 catch (NumberFormatException e) { }
	 break;
      case "java.lang.StringBuilder" :
      case "java.lang.StringBuffer" :
	 try {
	    BicexValue bv1 = sub_values.get("count");
	    if (bv1 == null) return "";
	    String sval = bv1.getStringValue(when);
	    if (sval == null) return "";
	    int ct = Integer.parseInt(sval);
	    if (ct == 0) return "";
	    BicexValue bv2 = sub_values.get("value");
	    if (bv2 == null) return "";
	    StringBuilder bldr = new StringBuilder();
	    Map<String,BicexValue> chars = bv2.getChildren(when);
	    if (chars == null) return "";
	    for (int i = 0; i < ct; ++i) {
	       String sv = chars.get("[" + i + "]").getStringValue(when);
	       int idx = sv.lastIndexOf("'");
	       if (idx > 0) {
		  sv = sv.substring(idx+2);
	       }
	       int ch = Integer.parseInt(sv);
	       bldr.append((char) ch);
	     }
	    return bldr.toString();
	  }
	 catch (NumberFormatException e) { }
	 break;
    }

   return value_text;
}

@Override String getDataType(long when)
{
   return value_type;
}

@Override String getTooltipValue(long when)
{
   StringBuffer buf = new StringBuffer();

   if (value_text != null) buf.append(value_text);
   else if (array_values != null) {
      buf.append("[");
      for (int i = 0; i < Math.min(array_values.length,10); ++i) {
	 if (i > 0) buf.append(",");
	 buf.append(array_values[i].getStringValue(when));
       }
      if (array_values.length > 10) buf.append(",...");
      buf.append("]");
    }
   else if (sub_values != null) {
      buf.append("{");
      int ct = 0;
      for (Map.Entry<String,BicexValue> ent : sub_values.entrySet()) {
	 if (ct++ > 0) buf.append("; ");
	 buf.append(ent.getKey());
	 buf.append(ent.getValue().getStringValue(when));
       }
      buf.append("}");
    }

   buf.append(" [" + value_type + "]");
   return buf.toString();
}

@Override boolean isInitializable()
{
   return can_initialize;
}


@Override boolean isComponent(long when)
{
   return is_component;
}

@Override public String getFullName()           
{
   return full_name;
}



}	// end of class BicexValue




/* end of BicexValue.java */

