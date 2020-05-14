/********************************************************************************/
/*										*/
/*		NobaseDebugValue.java						*/
/*										*/
/*	Representation of a JS value						*/
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



package edu.brown.cs.bubbles.nobase;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class NobaseDebugValue implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected String value_description;
private JSONObject preview_data; 

private static NobaseDebugValue undef_value = new JSUndef();
private static NobaseDebugValue null_value = new JSNull();
private static NobaseDebugValue true_value = new JSBool(true);
private static NobaseDebugValue false_value = new JSBool(false);
private static NobaseDebugValue unknown_value = new JSUnknown();

private static final int MAX_VALUE_SIZE = 40960;

private static Pattern FCT_NAME_PAT = Pattern.compile("^function ([A-Za-z0-9_$]+)\\(");





/********************************************************************************/
/*										*/
/*	Static methods								*/
/*										*/
/********************************************************************************/

static NobaseDebugValue getValue(JSONObject val,NobaseDebugRefMap refmap,String name)
{
   NobaseDebugValue rslt = unknown_value;
   
   if (val == null) return null_value;
   
   String valtyp = val.getString("type");
   NobaseMain.logD("GET VALUE " + valtyp + " " + val);
   
   String objid = val.optString("objectId",null);
   if (objid != null && objid.length() > 0) {
      NobaseDebugValue rval = refmap.get(objid);
      if (rval != null) return rval;
    }
   else objid = null;
   
   switch (valtyp) {
      case "object" :
         String subtyp = val.optString("subtype");
         if (subtyp != null && subtyp.equals("null")) return null_value;
         rslt = new JSObject(val);
         break;
      case "function" :
         rslt = new JSFunction(val,name);
         break;
      case "undefined" :
         rslt = undef_value;
         break;
      case "string" :
         rslt = new JSString(val);
         break;
      case "bigint" :
      case "number" :
         rslt = new JSNumber(val);
         break;
      case "boolean" :
         boolean fg = val.getBoolean("value");
         if (fg) rslt = true_value;
         else rslt = false_value;
         break;
      case "symbol" :
         rslt = new JSSymbol(val);
         break;
      default :
         NobaseMain.logE("Unknown value type: " + val);
         break;
    }
   
   if (objid != null && rslt != null) {
      refmap.put(objid,rslt);
    }

   return rslt;
}



static NobaseDebugRefMap createRefs(JSONArray refs,NobaseDebugRefMap rslt)
{
   if (refs == null) return rslt;

   for (int i = 0; i < refs.length(); ++i) {
      JSONObject ref = refs.getJSONObject(i);
      String name = ref.getString("name");
      JSONObject refval = ref.getJSONObject("value");
      String oid = refval.optString("objectId",null);
      if (oid != null) {
         rslt.put(oid,getValue(ref,rslt,name));
       }
    }
   
   return rslt;
}














/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected NobaseDebugValue(JSONObject ref)
{
   preview_data  = ref.optJSONObject("preview");
   String usval = ref.optString("unserializableValue",null);
   
   if (preview_data != null) {
      value_description = preview_data.getString("description");
    }
   else if (usval != null) value_description = usval;
   else {
      value_description =  ref.optString("description",null);
    }
}


protected NobaseDebugValue(String str)
{
   value_description = str;
   preview_data = null;
}



/********************************************************************************/
/*                                                                              */
/*      Creation methods                                                        */
/*                                                                              */
/********************************************************************************/

protected void complete(NobaseDebugRefMap refmap,int lvl) { }



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(String name,int lvl,IvyXmlWriter xw)
{
   xw.begin("VALUE");
   if (name != null) xw.field("NAME",name);
   outputLocalXml(lvl,xw);
   String txt = toString(lvl);
   if (txt.length() > MAX_VALUE_SIZE) {
      txt = txt.substring(0,MAX_VALUE_SIZE) + "...";
    }
   xw.cdataElement("DESCRIPTION",txt);
   xw.end("VALUE");
}


protected void outputLocalXml(int lvl,IvyXmlWriter xw) { }

protected String toString(int lvl)
{
   return toString();
}



@Override public String toString() 
{
   return value_description;
}



/********************************************************************************/
/*										*/
/*	Null values								*/
/*										*/
/********************************************************************************/

private static class JSNull extends NobaseDebugValue {

   JSNull() { super("null"); }

   @Override protected void outputLocalXml(int lvl,IvyXmlWriter xw) {
      xw.field("KIND","PRIVITIVE");
    }

}	// end of inner class JSNull



/********************************************************************************/
/*										*/
/*	Undefined values							*/
/*										*/
/********************************************************************************/

private static class JSUndef extends NobaseDebugValue {

   JSUndef() { super("undefined"); }

   @Override protected void outputLocalXml(int lvl,IvyXmlWriter xw) {
      xw.field("KIND","PRIVITIVE");
    }
   
}	// end of inner class JSUndef




/********************************************************************************/
/*										*/
/*	Boolean values								*/
/*										*/
/********************************************************************************/

private static class JSBool extends NobaseDebugValue {

   JSBool(boolean b) {
      super(Boolean.toString(b));
    }

    @Override protected void outputLocalXml(int lvl,IvyXmlWriter xw) {
      xw.field("KIND","PRIVITIVE");
    }
    
}	// end of inner class JSBool



/********************************************************************************/
/*										*/
/*	Numeric values								*/
/*										*/
/********************************************************************************/

private static class JSNumber extends NobaseDebugValue {

   private Number number_value;

   JSNumber(JSONObject ref) {
      super(ref);
      number_value = (Number) ref.opt("value");
      if (number_value == null) {
         String usv = ref.optString("unserializableValue");
         switch (usv) {
            case "Infinity" :
               number_value = Double.valueOf(Double.POSITIVE_INFINITY);
               break;
            case "-Infinity" :
               number_value = Double.valueOf(Double.NEGATIVE_INFINITY);
               break;
            case "NaN" :
               number_value = Double.valueOf(Double.NaN);
               break;
            default :
               NobaseMain.logE("Unknown special value: " + usv);
               break;
          }
         value_description = number_value.toString();
       }
      else {
         try {
            value_description = JSONObject.numberToString(number_value);
          }
         catch (JSONException e) {
            value_description = number_value.toString();
          }
       }
    }

   @Override protected void outputLocalXml(int lvl,IvyXmlWriter xw) {
      xw.field("KIND","PRIVITIVE");
      xw.field("NUMBER",number_value.toString());
    }

}	// end of inner class JSNumber



/********************************************************************************/
/*										*/
/*	String values								*/
/*										*/
/********************************************************************************/

private static class JSString extends NobaseDebugValue {

   private String string_value;

   JSString(JSONObject ref) {
      super(ref);
      string_value = ref.getString("value");
    }

   @Override protected void outputLocalXml(int lvl,IvyXmlWriter xw) {
      xw.field("KIND","STRING");
    }

   @Override public String toString()		{ return string_value; }

}	// end of inner class JSString




/********************************************************************************/
/*										*/
/*	Object values								*/
/*										*/
/********************************************************************************/

private static class JSObject extends NobaseDebugValue {

   protected Map<String,NobaseDebugValue> field_map;
   protected String class_name;
   protected String object_id;
   protected String subtype_hint;
   
   JSObject(JSONObject ref) {
      super(ref);
      field_map = null;
      class_name = ref.optString("className",null);
      object_id = ref.getString("objectId");
      subtype_hint = ref.optString("subtype",null);
    }

   @Override protected void complete(NobaseDebugRefMap refmap,int lvl) {
      if (field_map != null) return;
      if (class_name != null && class_name.equals("process")) return;
      field_map = new HashMap<>();
      NobaseDebugTarget tgt = refmap.getTarget();
      JSONObject proprslt = tgt.getObjectProperties(object_id);
      if (proprslt == null) {
         NobaseMain.logE("No properties for : " + object_id);
         return;
       }
      JSONArray props = proprslt.getJSONArray("result");
      for (int i = 0; i < props.length(); ++i) {
         JSONObject prop = props.getJSONObject(i);
         String name = prop.getString("name");
         JSONObject oval = prop.optJSONObject("value");
         // TODO: usually there is a set and a get value -- what should we report
         if (oval == null) oval = prop.optJSONObject("set");
         if (oval == null) oval = prop.optJSONObject("get");
         if (oval != null) {
            NobaseDebugValue val = getValue(oval,refmap,name);
            if (val != null) field_map.put(name,val);
          }
         else {
            NobaseMain.logE("FOUND NULL VALUE: " + i + " " + prop);
          }
         // if oval == null --> internal function without value
       }
      if (lvl > 0) {
         for (NobaseDebugValue fval : field_map.values()) {
            fval.complete(refmap,lvl-1);
          }
       }
    }

   @Override protected void outputLocalXml(int lvl,IvyXmlWriter xw) {
      xw.field("KIND","OBJECT");
      if (class_name != null) xw.field("TYPE",class_name);
      if (subtype_hint != null) xw.field("SUBTYPE",subtype_hint);
      xw.textElement("ID",object_id);
      outputFields(lvl,xw);
    }

   protected void outputFields(int lvl,IvyXmlWriter xw) {
      if (lvl < 0 || field_map == null) return;
      for (Map.Entry<String,NobaseDebugValue> ent : field_map.entrySet()) {
         NobaseDebugValue val = ent.getValue();
         if (val != null) {
            val.outputXml(ent.getKey(),lvl-1,xw);
          }
       }
    }

   @Override public String toString(int lvl) {
      if (lvl < 0 || field_map == null) return "<object>";
      StringBuffer buf = new StringBuffer();
      buf.append("{ ");
      int ct = 0;
      for (Map.Entry<String,NobaseDebugValue> ent : field_map.entrySet()) {
         NobaseDebugValue val = ent.getValue();
         if (ct++ > 0) buf.append(",");
         if (val != null) {
            buf.append(ent.getKey());
            buf.append(":");
            buf.append(val.toString(lvl-1));
          }
       }
      return buf.toString();
    }


}	// end of inner class JSObject




/********************************************************************************/
/*										*/
/*	Function values 							*/
/*										*/
/********************************************************************************/

private static class JSFunction extends JSObject implements NobaseDebugFunction {

   private String function_name;
   
   JSFunction(JSONObject ref,String name) {
      super(ref);
      function_name = name;
      if (value_description != null) {
         Matcher mat = FCT_NAME_PAT.matcher(value_description);
         if (mat.find()) {
            function_name = mat.group(1);
          }
       }
    }
   
   @Override protected void complete(NobaseDebugRefMap refmap,int lvl) {
      if (function_name != null) return;
      super.complete(refmap,0);
      NobaseDebugValue nameval = field_map.get("name");
      if (nameval != null) function_name = nameval.toString();
      // might want to try getting name property here
      return;
    }

   @Override public String getName() {
      return function_name;
    }
   
}	// end of inner class JSFunction



/********************************************************************************/
/*                                                                              */
/*      Symbol values                                                           */
/*                                                                              */
/********************************************************************************/

private static class JSSymbol extends NobaseDebugValue {
   
   private String object_id;
   
   JSSymbol(JSONObject ref) {
      super(ref);
      object_id = ref.getString("objectId");
    }
   
   @Override protected void outputLocalXml(int lvl,IvyXmlWriter xw) {
      xw.field("KIND","SYMBOL");
      xw.textElement("ID",object_id);
    }
   
}       // end of inner class JSSYmbol




/********************************************************************************/
/*										*/
/*	Frame values								*/
/*										*/
/********************************************************************************/

private static class JSUnknown extends NobaseDebugValue {

   JSUnknown() {
      super("<???>");
    }

  @Override protected void outputLocalXml(int lvl,IvyXmlWriter xw) {
      xw.field("KIND","UNKNNOWN");
    }
  
}	// end of inner class JSUnknown




}	// end of class NobaseDebugValue




/* end of NobaseDebugValue.java */

