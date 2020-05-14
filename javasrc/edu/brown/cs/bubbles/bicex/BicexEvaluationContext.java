/********************************************************************************/
/*										*/
/*		BicexEvaluationContext.java					*/
/*										*/
/*	Hold the evaluation context for a particular method call		*/
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

import java.util.*;

import edu.brown.cs.bubbles.bicex.BicexConstants.BicexResultContext;
import edu.brown.cs.bubbles.board.BoardLog;

import edu.brown.cs.ivy.xml.IvyXml;


class BicexEvaluationContext implements BicexConstants, BicexResultContext
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		context_id;
private BicexEvaluationContext parent_context;
private List<BicexEvaluationContext> child_contexts;
private String		method_name;
private String		file_name;
private Map<String,BicexValue> value_map;
private long		start_time;
private long		end_time;





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BicexEvaluationContext(BicexEvaluationContext par,
      Element xml,Map<String,BicexBaseValue> knownvalues,
      Map<String,BicexBaseValue> prevvalues)
{
   method_name = IvyXml.getAttrString(xml,"METHOD");
   file_name = IvyXml.getAttrString(xml,"FILE");
   context_id = IvyXml.getAttrString(xml,"ID");
   start_time = IvyXml.getAttrLong(xml,"START");
   end_time = IvyXml.getAttrLong(xml,"END");

   parent_context = par;
   child_contexts = null;
   value_map = new HashMap<>();

   for (Element var : IvyXml.children(xml,"VARIABLE")) {
      String nm = IvyXml.getAttrString(var,"NAME");
      int line = IvyXml.getAttrInt(var,"LINE");
      try {
	 BicexValue cv = BicexRefValue.createRefValue(var,knownvalues,prevvalues);
	 if (line > 0) value_map.put(nm + "@" + line,cv);
	 else value_map.put(nm,cv);
      }
      catch (Throwable t) {
	 BoardLog.logE("BICEX", "Problem creating value",t);
      }
    }

   for (Element ctx : IvyXml.children(xml,"CONTEXT")) {
      if (child_contexts == null) child_contexts = new ArrayList<>();
      BicexEvaluationContext cctx = new BicexEvaluationContext(this,ctx,knownvalues,prevvalues);
      child_contexts.add(cctx);
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getId()					{ return context_id; }

@Override public String getMethod()		{ return method_name; }

@Override public String getShortName()
{
   String s = method_name;
   if (s == null) return null;

   int idx = s.indexOf("(");
   if (idx > 0) s = s.substring(0, idx);

   idx = s.lastIndexOf(".");
   if (idx > 0) {
      if (s.endsWith("<init>")) {
	 s = s.substring(0,idx);
	 idx = s.lastIndexOf(".");
      }
   }
   s = s.substring(idx+1);

   return s;
}


String getFullShortName()
{
   String s = method_name;
   if (s == null) return null;

   String rslt = getShortName();
   int idx = s.indexOf("(");
   if (idx < 0) return rslt;

   StringBuffer buf = new StringBuffer();
   buf.append(rslt);
   buf.append("(");
   String args = s.substring(idx+1);
   int ct = 0;
   StringTokenizer tok = new StringTokenizer(args,",)");
   while (tok.hasMoreTokens()) {
      String type = tok.nextToken();
      int idx1 = type.lastIndexOf(".");
      if (idx1 >= 0) type = type.substring(idx1+1);
      if (ct++ > 0) buf.append(",");
      buf.append(type);
   }
   buf.append(")");

   return buf.toString();
}


String getFileName()				{ return file_name; }

BicexEvaluationContext getParent()		{ return parent_context; }

Map<String,BicexValue> getValues()		{ return value_map; }

void addValue(String id,BicexValue v)		{ value_map.put(id,v); }

String getValueName(String id,int lno)
{
   String rslt = null;
   int bestline = 0;

   for (String s : value_map.keySet()) {
      if (s.startsWith("*")) continue;
      String var = s;
      int vln = 0;
      int idx = s.indexOf("@");
      if (idx > 0) {
	 vln = Integer.parseInt(var.substring(idx+1));
	 var = var.substring(0,idx);
       }
      if (var.equals(id)) {
	 if (rslt == null) {
	    rslt = s;
	    bestline = vln;
	  }
	 else if (vln > 0 && vln > bestline && vln <= lno) {
	    rslt = s;
	    bestline = vln;
	  }
       }
    }
   return rslt;
}



Collection<BicexEvaluationContext> getInnerContexts()
{
   return child_contexts;
}

long getStartTime()				{ return start_time; }

long getEndTime()				{ return end_time; }



/********************************************************************************/
/*										*/
/*	Compute execution count data						*/
/*										*/
/********************************************************************************/

@Override public BicexCountData getCountData()
{
   CountData bcd = new CountData();

   addCountData(this,bcd);

   return bcd;
}



private void addCountData(BicexEvaluationContext ctx,CountData cd)
{
   String method = ctx.getMethod();
   Map<Integer,int []> cts = cd.get(method);
   if (cts == null) {
      cts = new HashMap<>();
      cd.put(method,cts);
    }
   BicexValue lnv = ctx.getValues().get("*LINE*");
   if (lnv != null) {
      List<Integer> times = lnv.getTimeChanges();
      for (Integer t : times) {
	 String xv = lnv.getStringValue(t+1);
	 int line = Integer.parseInt(xv);
	 if (line == 0) continue;
	 int [] ct = cts.get(line);
	 if (ct == null) {
	    ct = new int[1];
	    cts.put(line,ct);
	    ct[0] = 0;
	  }
	 ++ct[0];
       }
    }
   if (ctx.getInnerContexts() != null) {
      for (BicexEvaluationContext cctx : ctx.getInnerContexts()) {
	 addCountData(cctx,cd);
       }
    }
}


private class CountData extends HashMap<String,Map<Integer,int []>> implements BicexCountData {

   private static final long serialVersionUID = 1;

   CountData() {
    }

}	// end of inner class CountData




}	// end of class BicexEvaluationContext




/* end of BicexEvaluationContext.java */

