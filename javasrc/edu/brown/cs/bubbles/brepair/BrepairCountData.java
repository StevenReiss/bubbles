/********************************************************************************/
/*										*/
/*		BrepairCountData.java						*/
/*										*/
/*	description of class							*/
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



package edu.brown.cs.bubbles.brepair;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.brown.cs.bubbles.batt.BattConstants.BattTestCounts;
import edu.brown.cs.bubbles.bicex.BicexConstants.BicexCountData;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;

class BrepairCountData implements BrepairConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,MethodAccumData>	method_data;
@SuppressWarnings("unused")
private int				pass_total;
private int				fail_total;
@SuppressWarnings("unused")
private int                             user_pass_total;
private int                             user_fail_total;
private List<String>			sorted_methods;

private static double                   scale_factor = 10.0;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BrepairCountData(BattTestCounts tc)
{
   method_data = new HashMap<>();
   pass_total = 0;
   fail_total = 0;
   sorted_methods = null;

   addCountData(tc,false,true);
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

void addCountData(BattTestCounts bcd,boolean pass)
{
   if (bcd == null) return;

   addCountData(bcd,pass,false);
}



private void addCountData(BattTestCounts bcd,boolean pass,boolean force)
{
   if (pass) ++pass_total;
   else ++fail_total;

   for (String mnm : bcd.getMethods()) {
      MethodAccumData mad = method_data.get(mnm);
      if (mad == null) {
	 if (!force) continue;
	 mad = new MethodAccumData(mnm);
	 method_data.put(mnm,mad);
       }
      mad.count(pass);
      for (Integer bid : bcd.getBlocks(mnm)) {
	 int sln = bcd.getBlockStartLine(mnm,bid);
	 int eln = bcd.getBlockEndLine(mnm,bid);
	 mad.addBlock(bid,sln,eln,pass,force);
       }
    }
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

List<String> getSortedMethods()
{
   return sorted_methods;
}



List<String> getSortedBlocks(Object method)
{
   MethodAccumData mad = method_data.get(method);
   if (mad == null) return null;
   return mad.getSortedBlocks();
}


List<String> getSortedBlocks()
{
   SortedSet<BlockAccumData> blks = new TreeSet<>(new BlockComparator());
   for (MethodAccumData mad : method_data.values()) {
      blks.addAll(mad.getBlocks().values());
    }

   List<String> rslt = new ArrayList<String>();
   for (BlockAccumData bad : blks) {
      String rls = bad.getMethod().getName();
      rls += "@" + bad.getStartLine();
      rls += "@" + bad.getEndLine();
      rslt.add(rls);
    }

   return rslt;
}




/********************************************************************************/
/*										*/
/*	Computation methods							*/
/*										*/
/********************************************************************************/

void computeSortedMethods(String testname,int max,double cutoff)
{
   SortedSet<MethodAccumData> mthds = new TreeSet<>(new MethodComparator());
   for (MethodAccumData mad : method_data.values()) {
      mad.computeOchaia();
      BoardLog.logD("BREPAIR","ADD METHOD " + mad.getScore() + " " + mad.getName());
      mthds.add(mad);
    }

   double min = -1;
   sorted_methods = new ArrayList<>();
   for (MethodAccumData mad : mthds) {
      if (mad.getName().startsWith(testname)) continue;
      if (min < 0) {
	 min = mad.getScore() * cutoff;
       }
      if (mad.getScore() < min) break;
      sorted_methods.add(mad.getName());
      if (sorted_methods.size() >= max) min = mad.getScore();
      mad.computeSortedBlocks(min);
    }
}



/********************************************************************************/
/*										*/
/*	Retrieve the set of relevant files					*/
/*										*/
/********************************************************************************/

Set<File> getRelevantFiles()
{
   Set<File> rslt = new HashSet<>();
   Set<String> clss = new HashSet<>();
   BumpClient bc = BumpClient.getBump();

   for (MethodAccumData mad : method_data.values()) {
      String nm = mad.getName();
      int idx = nm.indexOf("(");
      if (idx >= 0) nm = nm.substring(0,idx);
      idx = nm.lastIndexOf(".");
      if (idx < 0) continue;
      String cls = nm.substring(0,idx);
      if (clss.add(cls)) {
	 List<BumpLocation> locs = bc.findAllClasses(cls);
	 if (locs != null) {
	    for (BumpLocation bl : locs) {
	       File f = bl.getFile();
	       rslt.add(f);
	     }
	  }
       }
    }

   return rslt;
}




/********************************************************************************/
/*										*/
/*	User update methods							*/
/*										*/
/********************************************************************************/

void addUserFeedback(BicexCountData bcd,boolean pass)
{
   if (pass) user_pass_total++;
   else user_fail_total++;
   
   for (String method : bcd.keySet()) {
      MethodAccumData mad = getMethodData(method);
      if (mad == null) continue;
      mad.userCount(pass);
      Collection<BlockAccumData> blks = mad.getBlocks().values();
      Set<BlockAccumData> done = new HashSet<BlockAccumData>();
      Map<Integer,int []> lines = bcd.get(method);
      for (Map.Entry<Integer,int []> ent : lines.entrySet()) {
	 int ct = ent.getValue()[0];
	 if (ct == 0) continue;
	 int lno = ent.getKey();
	 for (BlockAccumData bblk : blks) {
	    if (lno >= bblk.getStartLine() && lno <= bblk.getEndLine()) {
               if (done.add(bblk)) {
                  bblk.userCount(pass);
                }
	     }
	  }
       }
    }
}



void addUserFeedback(String method,int sline,int eline,boolean pass)
{
   MethodAccumData mad = getMethodData(method);
   if (mad == null) return;
   mad.userCount(pass);
   Collection<BlockAccumData> blks = mad.getBlocks().values();
   for (BlockAccumData bblk : blks) {
      if (sline != 0) {
         if (bblk.getEndLine() < sline || bblk.getStartLine() > eline) continue;
       }
     bblk.userMark(pass);
    }
}



private MethodAccumData getMethodData(String method)
{
   MethodAccumData mad = method_data.get(method);
   if (mad != null) return mad;

   return null;
}




/********************************************************************************/
/*										*/
/*	Method and block counts 						*/
/*										*/
/********************************************************************************/

private class MethodAccumData {

   private String method_name;
   private int fail_count;
   private int pass_count;
   private int user_fail_count;
   private int user_pass_count;
   private Map<Integer,BlockAccumData> block_data;
   private List<String> active_blocks;
   private double total_score;

   MethodAccumData(String name) {
      method_name = name;
      fail_count = 0;
      pass_count = 0;
      user_fail_count = 0;
      user_pass_count = 0;
      block_data = new HashMap<>();
      total_score = 0;
      active_blocks = null;
    }

   void count(boolean pass) {
      if (pass) pass_count++;
      else fail_count++;
    }

   void userCount(boolean pass) {
      if (pass) user_pass_count++;
      else user_fail_count++;
    }

   void addBlock(Integer bid,int sln,int eln,boolean pass,boolean force) {
      BlockAccumData bad = block_data.get(bid);
      if (bad == null) {
	 if (!force) return;
	 bad = new BlockAccumData(this,bid,sln,eln);
	 block_data.put(bid,bad);
       }
      bad.count(pass);
    }

   double getScore()				{ return total_score; }
   String getName()				{ return method_name; }
   Map<Integer,BlockAccumData> getBlocks()	{ return block_data; }

   double computeOchaia() {
      double a01 = fail_total + user_fail_total*scale_factor - 
           fail_count - user_fail_count*scale_factor;
      // double a00 = pass_total + user_pass_count*scale_factor - 
      //     pass_count - user_pass_count*scale_factor;
      double a11 = fail_count + user_fail_count*scale_factor;
      double a10 = pass_count + user_pass_count*scale_factor;
      double div = Math.sqrt((a11 + a01)*(a11+a10));
      if (div == 0) total_score = 0;
      else total_score = a11/div;
      if (block_data != null && block_data.size() > 0) {
         total_score = 0;
         for (BlockAccumData bad : block_data.values()) {
            double lnscore = bad.computeOchaia();
            if (lnscore > total_score) total_score = lnscore;
          }
         active_blocks = null;
       }
      return total_score;
    }

   void computeSortedBlocks(double cutoff) {
      if (active_blocks == null) {
         SortedSet<BlockAccumData> blks = new TreeSet<>(new BlockComparator());
         for (BlockAccumData bad : block_data.values()) {
            if (bad.getScore() >= cutoff && bad.getStartLine() > 0) {
               blks.add(bad);
               BoardLog.logD("BREPAIR","ADD BLOCK " + bad.getScore() + " " + bad.getStartLine() + " " + method_name);
             }
          }
         active_blocks = new ArrayList<>();
         for (BlockAccumData bad : blks) {
            active_blocks.add(bad.getName());
          }
       }
    }

   List<String> getSortedBlocks() {
      return active_blocks;
    }

}	// end of inner class MethodAccumData







private static class MethodComparator implements Comparator<MethodAccumData>
{
   @Override public int compare(MethodAccumData m1,MethodAccumData m2) {
      double s1 = m1.getScore();
      double s2 = m2.getScore();
      if (s1 > s2) return -1;
      if (s2 > s1) return 1;
      return m1.getName().compareTo(m2.getName());
    }
}



private class BlockAccumData {

   // private int block_index;
   private MethodAccumData for_method;
   private int start_line;
   private int end_line;
   private int fail_count;
   private int pass_count;
   private int user_fail_count;
   private int user_pass_count;
   private double total_score;

   BlockAccumData(MethodAccumData mthd,int bid,int sln,int eln) {
      for_method = mthd;
      start_line = sln;
      end_line = eln;
      fail_count = 0;
      pass_count = 0;
      user_fail_count = 0;
      user_pass_count = 0;
    }

   double getScore()			{ return total_score; }
   int getStartLine()			{ return start_line; }
   int getEndLine()			{ return end_line; }
   String getName() {
      if (start_line == end_line) return "Line " + start_line;
      return "Lines " + start_line + "-" + end_line;
    }
   MethodAccumData getMethod()			{ return for_method; }

   void count(boolean pass) {
      if (pass) ++pass_count;
      else ++fail_count;
    }

   void userCount(boolean pass) {
      if (pass) ++user_pass_count;
      else ++user_fail_count;
    }

   void userMark(boolean pass) {
      if (pass) ++user_pass_count;
      else ++user_fail_count;
    }

   double computeOchaia() {
      double a01 = fail_total + user_fail_total*scale_factor - fail_count - user_fail_count*scale_factor;
      // double a00 = pass_total + user_pass_total*scale_factor - pass_count - user_pass_count*scale_factor;
      double a11 = fail_count + user_fail_count*scale_factor;
      double a10 = pass_count + user_pass_count*scale_factor;
      double div = Math.sqrt((a11 + a01)*(a11+a10));
      if (div == 0) total_score = 0;
      else total_score = a11/div;
      return total_score;
    }

}	// end of inner class BlockAccumData



private static class BlockComparator implements Comparator<BlockAccumData>
{
   @Override public int compare(BlockAccumData m1,BlockAccumData m2) {
      double s1 = m1.getScore();
      double s2 = m2.getScore();
      if (s1 > s2) return -1;
      if (s2 > s1) return 1;
      int sln1 = m1.getStartLine();
      int sln2 = m2.getStartLine();
      if (sln1 < sln2) return -1;
      else if (sln2 < sln1) return 1;
      return m1.getName().compareTo(m2.getName());
    }
}



}	// end of class BrepairCountData




/* end of BrepairCountData.java */

