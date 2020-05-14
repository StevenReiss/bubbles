/********************************************************************************/
/*										*/
/*		BumpBreakImpl.java						*/
/*										*/
/*	BUblles Mint Partnership problem description holder			*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bump;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.io.File;
import java.util.HashMap;
import java.util.Map;



class BumpBreakImpl implements BumpConstants.BumpBreakpoint, BumpConstants {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String	breakpoint_id;
private Map<String,Object> break_props;
private String	break_desc;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BumpBreakImpl(Element d)
{
   breakpoint_id = IvyXml.getAttrString(d,"ID");

   setProperties(d);
}



/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

boolean update(Element d)
{
   Map<String,Object> oprops = break_props;

   setProperties(d);

   return break_props.equals(oprops);
}



/********************************************************************************/
/*										*/
/*	Property setting methods						*/
/*										*/
/********************************************************************************/

private void setProperties(Element d)
{
   break_props = new HashMap<>();
   break_desc = null;

   break_props.put("ENABLED",Boolean.valueOf(IvyXml.getAttrBool(d,"ENABLED",false)));
   break_props.put("HITCOUNT",IvyXml.getAttrInteger(d,"HITCOUNT"));
   break_props.put("SUSPEND",IvyXml.getAttrString(d,"SUSPEND"));
   break_props.put("CLASS",IvyXml.getAttrString(d,"CLASS"));
   String fnm = IvyXml.getAttrString(d,"FILE");
   if (fnm != null) break_props.put("FILE",new File(fnm));
   break_props.put("LINE",IvyXml.getAttrInteger(d,"LINE"));
   break_props.put("TRACEPOINT",Boolean.valueOf(IvyXml.getAttrBool(d,"TRACEPOINT")));

   String typ = IvyXml.getAttrString(d,"TYPE");
   if (typ == null) return;
   break_props.put("TYPE",typ);

   if (typ.equals("CLASSPREPARE")) {
      break_props.put("INTERFACE",Boolean.valueOf(IvyXml.getAttrBool(d,"INTERFACE")));
      break_desc = "Class Load " + break_props.get("CLASS");
    }
   else if (typ.equals("EXCEPTION")) {
      break_props.put("CAUGHT",Boolean.valueOf(IvyXml.getAttrBool(d,"ISCAUGHT")));
      break_props.put("UNCAUGHT",Boolean.valueOf(IvyXml.getAttrBool(d,"ISUNCAUGHT")));
      break_props.put("EXCEPTION",IvyXml.getAttrString(d,"EXCEPTION"));
      StringBuffer buf = new StringBuffer();
      int ct = 0;
      for (Element e : IvyXml.children(d,"EXCLUDE")) {
	 if (ct++ > 0) buf.append(",");
	 buf.append(IvyXml.getText(e));
       }
      if (ct > 0) break_props.put("EXCLUDE",buf.toString());
      buf = new StringBuffer();
      ct = 0;
      for (Element e : IvyXml.children(d,"INCLUDE")) {
	 if (ct++ > 0) buf.append(",");
	 buf.append(IvyXml.getText(e));
       }
      if (ct > 0) break_props.put("INCLUDE",buf.toString());
      break_desc = "Throw " + break_props.get("EXCEPTION");
    }
   else if (typ.equals("METHOD")) {
      break_props.put("ENTRY",Boolean.valueOf(IvyXml.getAttrBool(d,"ENTRY")));
      break_props.put("EXIT",Boolean.valueOf(IvyXml.getAttrBool(d,"EXIT")));
      break_props.put("NATIVE",Boolean.valueOf(IvyXml.getAttrBool(d,"NATIVE")));
      Element me = IvyXml.getChild(d,"METHOD");
      String mnm = IvyXml.getAttrString(me,"CLASS") + "." + IvyXml.getAttrString(me,"NAME");
      break_props.put("METHOD",mnm);
      break_props.put("SIGNATURE",IvyXml.getText(me));
      break_desc = "Method " + break_props.get("METHOD");
      if (getBoolProperty("ENTRY")) break_desc += " Enter";
      if (getBoolProperty("EXIT")) break_desc += " Exit";
    }
   else if (typ.equals("METHODENTRY")) {
      break_props.put("ENTRY",Boolean.TRUE);
      break_props.put("EXIT",Boolean.FALSE);
      break_props.put("NATIVE",Boolean.FALSE);
      Element me = IvyXml.getChild(d,"METHOD");
      break_props.put("METHOD",IvyXml.getAttrString(me,"NAME"));
      break_props.put("SIGNATURE",IvyXml.getText(me));
      break_desc = "Method " + getProperty("METHOD") + " Enter";
    }
   else if (typ.equals("STRATUMLINE")) {
      Element se = IvyXml.getChild(d,"SOURCE");
      break_props.put("SOURCE",IvyXml.getAttrString(se,"NAME"));
      break_props.put("PATH",IvyXml.getText(se));
      break_props.put("PATTERN",IvyXml.getTextElement(d,"PATTERN"));
      String sv = IvyXml.getTextElement(d,"STRATUM");
      if (sv != null) break_props.put("STRATUM",sv);
    }
   else if (typ.equals("TARGETPATTERN")) {
      break_props.put("SOURCE",IvyXml.getAttrString(d,"SOURCE"));

    }
   else if (typ.equals("WATCHPOINT")) {
      break_props.put("FIELD",IvyXml.getAttrString(d,"FIELD"));
    }
   else if (typ.equals("LINE")) {
      Element ce = IvyXml.getChild(d,"CONDITION");
      if (ce != null) {
	 break_props.put("CONDITION",IvyXml.getText(ce));
	 break_props.put("CONDENABLE",Boolean.valueOf(IvyXml.getAttrBool(ce,"ENABLED")));
	 break_props.put("CONDSUSPEND",Boolean.valueOf(IvyXml.getAttrBool(ce,"SUSPEND")));
       }
      break_desc = "Line " + getProperty("LINE") + ":" + getProperty("FILE");
    }
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getBreakId()			{ return breakpoint_id; }
@Override public File getFile() 			{ return (File) break_props.get("FILE"); }
@Override public int getLineNumber()			{ return getIntProperty("LINE"); }
@Override public String getDescription()		{ return break_desc; }


@Override public String getProperty(String id)
{
   Object o = break_props.get(id);
   if (o == null) return null;
   return o.toString();
}



@Override public boolean getBoolProperty(String id)
{
   Boolean bv = (Boolean) break_props.get(id);
   if (bv == null) return false;
   return bv.booleanValue();
}


@Override public int getIntProperty(String id)
{
   Integer iv = (Integer) break_props.get(id);
   if (iv == null) return 0;
   return iv.intValue();
}




/********************************************************************************/
/*                                                                              */
/*      Matching methods                                                        */
/*                                                                              */
/********************************************************************************/

boolean isEquivalentTo(BumpBreakpoint bp)
{
   BumpBreakImpl bbi = (BumpBreakImpl) bp;
   for (Map.Entry<String,Object> ent : break_props.entrySet()) {
      String key = ent.getKey();
      if (key.equals("EXCEPTION")) continue;
      Object val = ent.getValue();
      if (val != null) {
         Object v1 = bbi.break_props.get(key);
         if (!val.equals(v1)) return false;
       }
    }
   return true;
}


/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   if (break_desc != null) return break_desc;

   StringBuffer buf = new StringBuffer();
   buf.append("BREAK ");
   buf.append(getProperty("TYPE"));
   buf.append(" ");
   buf.append(getLineNumber());
   buf.append(" ");
   buf.append(getFile());
   buf.append(" ");
   buf.append(getBreakId());

   return buf.toString();
}



}	// end of class BumpBreakImpl




/* end of BumpBreakImpl.java */




