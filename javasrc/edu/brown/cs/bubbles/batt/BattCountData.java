/********************************************************************************/
/*                                                                              */
/*              BattCountData.java                                              */
/*                                                                              */
/*      Coverage information for a test case                                    */
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



package edu.brown.cs.bubbles.batt;

import edu.brown.cs.bubbles.batt.BattConstants.BattTestCounts;
import edu.brown.cs.bubbles.bump.BumpLocation;

import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class BattCountData implements BattConstants, BattTestCounts
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<String,MethodCountData>     method_data;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BattCountData(Element e) 
{
   method_data = new HashMap<String,MethodCountData>();
   
   for (Element me : IvyXml.children(e,"METHOD")) {
      MethodCountData mcd = new MethodCountData(me);
      method_data.put(mcd.getName(),mcd);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

FileState usesClasses(Map<String,FileState> clsset,FileState st) 
{
   for (MethodCountData mcd : method_data.values()) {
      st = mcd.usesClasses(clsset,st);
    }
   return st;
}




UseMode getMethodUsage(String mthd)
{
   MethodCountData mcd = method_data.get(mthd);
   if (mcd == null) {
      int idx0 = mthd.indexOf("(");
      if (idx0 < 0) return UseMode.NONE;
      String mthd0 = mthd.substring(0,idx0);
      String mthd1 = mthd.substring(idx0);
      for (Map.Entry<String,MethodCountData> ent : method_data.entrySet()) {
         String nm = ent.getKey();
         nm = nm.replace('/','.');
         int idx = nm.indexOf("(");
         if (idx < 0) continue;
         if (mthd0.equals(nm.substring(0,idx))) {
            if (BumpLocation.compareParameters(mthd1,nm.substring(idx))) {
               mcd = ent.getValue();
               method_data.put(mthd,mcd);
               break;
             }
          }
       }
    }
   
   if (mcd != null) {
      if (mcd.getTopCount() > 0) return UseMode.DIRECT;
      if (mcd.getCalledCount() > 0) return UseMode.INDIRECT;
    }
   return UseMode.NONE;
}



/********************************************************************************/
/*                                                                              */
/*      Data access methos                                                      */
/*                                                                              */
/********************************************************************************/

@Override public Collection<String> getMethods()
{
   return method_data.keySet();
}


@Override public Collection<Integer> getBlocks(String method)
{
   MethodCountData mcd = method_data.get(method);
   if (mcd == null) return null;
   return mcd.getBlocks();
}


@Override public int getBlockStartLine(String method,int bid)
{
   MethodCountData mcd = method_data.get(method);
   if (mcd == null) return 0;
   BlockCountData bcd = mcd.getBlockData(bid);
   if (bcd == null) return 0;
   return bcd.getStartLine();
}


@Override public int getBlockEndLine(String method,int bid)
{
   MethodCountData mcd = method_data.get(method);
   if (mcd == null) return 0;
   BlockCountData bcd = mcd.getBlockData(bid);
   if (bcd == null) return 0;
   return bcd.getEndLine();
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

void report(IvyXmlWriter xw) 
{
   xw.begin("COVERAGE");
   for (MethodCountData mcd : method_data.values()) {
      mcd.report(xw);
    }
   xw.end("COVERAGE");
}




/********************************************************************************/
/*                                                                              */
/*      Class to hold method-level coverage data                                */
/*                                                                              */
/********************************************************************************/

private static class MethodCountData {
   
   private String class_name;
   private String method_name;
   private int start_line;
   private int end_line;
   private int called_count;
   private int top_count;
   private Map<String,Integer> calls_counts;
   private Map<Integer,BlockCountData> block_data;
   
   MethodCountData(Element e) {
      class_name = null;
      method_name = computeMethodName(e);
      start_line = IvyXml.getAttrInt(e,"START");
      end_line = IvyXml.getAttrInt(e,"END");
      called_count = IvyXml.getAttrInt(e,"COUNT");
      top_count = IvyXml.getAttrInt(e,"TOP");
      calls_counts = new HashMap<String,Integer>();
      block_data = new HashMap<Integer,BlockCountData>();
      for (Element be : IvyXml.children(e,"CALLS")) {
         int ct = IvyXml.getAttrInt(be,"CALLCOUNT");
         String nm = computeMethodName(be);
         calls_counts.put(nm,ct);
       }
      for (Element be : IvyXml.children(e,"BLOCK")) {
         BlockCountData bcd = new BlockCountData(be);
         block_data.put(bcd.getBlockIndex(),bcd);
       }
    }
   
   String getName()			{ return method_name; }
   int getCalledCount() 		{ return called_count; }
   int getTopCount()			{ return top_count; }
   
   Collection<Integer> getBlocks()      { return block_data.keySet(); }
   BlockCountData getBlockData(int id)  { return block_data.get(id); }
   
   private String computeMethodName(Element e) {
      String nm = IvyXml.getAttrString(e,"NAME");
      nm = nm.replace('/','.');
      String dsc = IvyXml.getAttrString(e,"SIGNATURE");
      if (dsc != null) {
         int idx = dsc.lastIndexOf(")");
         if (idx >= 0) dsc = dsc.substring(0,idx+1);
         dsc = IvyFormat.formatTypeName(dsc);
         nm = nm + dsc;
       }
      return nm;
    }
   
   FileState usesClasses(Map<String,FileState> clsset,FileState fs) {
      if (class_name == null) {
	 if (method_name == null) return fs;
	 int i1 = method_name.indexOf("(");
	 int i2 = method_name.lastIndexOf(".",i1);
	 if (i2 > 0) class_name = method_name.substring(0,i2);
	 else return fs;
       }
      FileState fs1 = clsset.get(class_name);
      if (fs1 == null) return fs;
      return fs1.merge(fs);
    }
   
   void report(IvyXmlWriter xw) {
      xw.begin("METHOD");
      xw.field("NAME",method_name);
      xw.field("START",start_line);
      xw.field("END",end_line);
      xw.field("COUNT",called_count);
      xw.field("TOP",top_count);
      for (Map.Entry<String,Integer> ent : calls_counts.entrySet()) {
	 xw.begin("CALLS");
	 xw.field("NAME",ent.getKey());
	 xw.field("COUNT",ent.getValue());
	 xw.end("CALLS");
       }
      for (BlockCountData bcd : block_data.values()) {
	 bcd.report(xw);
       }
      xw.end("METHOD");
    }
   
}	// end of inner class MethodCountData




/********************************************************************************/
/*                                                                              */
/*      Class to hold block-level coverage data                                 */
/*                                                                              */
/********************************************************************************/

private static class BlockCountData {
   
   private int block_index;
   private int start_line;
   private int end_line;
   private int enter_count;
   private Map<Integer,Integer> branch_counts;
   
   BlockCountData(Element e) {
      block_index = IvyXml.getAttrInt(e,"INDEX");
      if (block_index < 0) block_index = IvyXml.getAttrInt(e,"ID");
      start_line = IvyXml.getAttrInt(e,"START");
      end_line = IvyXml.getAttrInt(e,"END");
      enter_count = IvyXml.getAttrInt(e,"COUNT");
      branch_counts = new HashMap<>();
      for (Element be : IvyXml.children(e,"BRANCH")) {
         int to = IvyXml.getAttrInt(be,"TOBLOCK");
         int ct = IvyXml.getAttrInt(be,"COUNT");
         branch_counts.put(to,ct);
       }
    }
   
   int getBlockIndex()				{ return block_index; }
   int getStartLine()                           { return start_line; }
   int getEndLine()                             { return end_line; }
   
   void report(IvyXmlWriter xw) {
      xw.begin("BLOCK");
      xw.field("ID",block_index);
      xw.field("START",start_line);
      xw.field("END",end_line);
      xw.field("COUNT",enter_count);
      for (Map.Entry<Integer,Integer> ent : branch_counts.entrySet()) {
         xw.begin("BRANCH");
         xw.field("TOBLOCK",ent.getKey());
         xw.field("COUNT",ent.getValue());
         xw.end("BRANCH");
       }
      xw.end("BLOCK");
    }
   
}	// end of inner class BlockCountData





}       // end of class BattCountData




/* end of BattCountData.java */

