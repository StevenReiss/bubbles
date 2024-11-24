/********************************************************************************/
/*										*/
/*		BparePattern.java						*/
/*										*/
/*	Class to hold a potential pattern for BPARE				*/
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



package edu.brown.cs.bubbles.bpare;

import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.ivy.jcomp.JcompAst;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;




class BparePattern implements BpareConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BparePatternNode root_node;
private List<MatchInfo> match_data;
private BparePatternNode first_change;

private static ASTMatcher ast_matcher = new ASTMatcher();
private static ASTNode	     empty_node;


static {
   AST ast = JcompAst.createNewAst();
   empty_node = ast.newEmptyStatement();
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BparePattern()
{
   first_change = null;
   match_data = new ArrayList<MatchInfo>();
   root_node = null;
}



BparePattern(ASTNode r)
{
   this();
   root_node = new BparePatternNodeAst(this,null,r,(StructuralPropertyDescriptor) null);
}



BparePattern(BparePatternNode r)
{
   this();
   root_node = r;
}




BparePattern(ASTNode r,StructuralPropertyDescriptor spd,int start,int sz)
{
   this();
   root_node = new BparePatternNodeList(this,null,r,spd,start,sz);
}



BparePattern(BparePattern p,BparePatternNode exp)
{
   match_data = new ArrayList<MatchInfo>();

   if (exp == p.root_node) {
      root_node = p.root_node.cloneNode(this,null,null);
      root_node = root_node.expandNode(this,null);
      first_change = null;
    }
   else {
      BparePatternNode [] exps = new BparePatternNode[1];
      exps[0] = exp;	// replaced by newer version in the clone call
      root_node = p.root_node.cloneNode(this,null,exps);
      first_change = exps[0];
    }
}




/********************************************************************************/
/*										*/
/*	Tree matching methods							*/
/*										*/
/********************************************************************************/

int getMatch(ASTNode nn,MatchType typ)
{
   int j = -1;
   for (int i = 0; i < match_data.size(); ++i) {
      MatchInfo mi = match_data.get(i);
      if (!mi.isValid()) {
	 if (j < 0) j = i;
       }
      else if (mi.match(nn,typ)) {
	 mi.incr();
	 return i;
       }
    }

   if (nn == null) nn = empty_node;
   if (j < 0) {
      j = match_data.size();
      match_data.add(new MatchInfo(nn,typ));
    }
   else {
      match_data.set(j,new MatchInfo(nn,typ));
    }

   return j;
}





void removeMatch(int i)
{
   if (i < 0 || i >= match_data.size()) return;

   match_data.get(i).decr();
}




/********************************************************************************/
/*										*/
/*	Pattern testing methods 						*/
/*										*/
/********************************************************************************/

boolean isValidPattern()
{
   int sz = root_node.getSize();
   if (sz < MIN_SIZE || sz > MAX_SIZE) return false;

   int ct = 0;
   int mct = 0;
   for (MatchInfo mi : match_data) {
      int j = mi.getCount();
      if (j != 0) {
	 ++ct;
	 if (j > 1) ++mct;
       }
    }
   if (mct < MIN_MATCH) return false;
   if (ct > MAX_VARIABLE) return false;

   return true;
}


boolean canBeExpanded()
{
   int sz = root_node.getSize();
   if (sz > MAX_SIZE) return false;

   return true;
}



boolean isTested(Set<ASTNode> done)
{
   return root_node.isTested(done);
}


int getSize()
{
   return root_node.getSize();
}


int getNestLevel()
{
   return root_node.getNestLevel(0);
}


int getMaxListSize()
{
   return root_node.getMaxListSize();
}



double getMinExpandProb(BpareStatistics ps)
{
   return root_node.getMinExpandProb(1.0,ps) * ps.scaleMinExpandProb();
}



double getProb(BpareStatistics ps)
{
   return root_node.getProb(ps);
}


ASTNode getActiveNode()
{
   return root_node.getActiveNode();
}



/********************************************************************************/
/*										*/
/*	Probablity from string methods						*/
/*										*/
/********************************************************************************/

static double getProb(String pat,BpareStatistics ps)
{
   ProbScanner scan = new ProbScanner(pat,ps);
   return scan.scan();
}



private static class ProbScanner {

   private String pattern_string;
   private BpareStatistics paca_stats;
   private int	       pattern_index;

   ProbScanner(String p,BpareStatistics ps) {
      pattern_string = p;
      pattern_index = 0;
      paca_stats = ps;
    }

   double scan() {
      double prob = 0;
      try {
	 prob = scanElement(null);
       }
      catch (Exception e) {
	 System.err.println("PACA: problem scanning pattern " + pattern_string + ": " + e);
	 e.printStackTrace();
	 System.exit(1);
       }

      return prob;
    }

   private double scanAst(StructuralPropertyDescriptor spd) throws Exception {
      int c = pattern_string.charAt(pattern_index++);
      if (c == '(') c = pattern_string.charAt(pattern_index++);
      int id = 0;
      while (Character.isDigit(c)) {
	 id = id*10+(c-'0');
	 c = pattern_string.charAt(pattern_index++);
       }
      double prob = paca_stats.getAstProb(id,spd);
      --pattern_index;
      for (StructuralPropertyDescriptor xpd : BpareStatistics.getStructuralProperties(id)) {
	 if (xpd.isSimpleProperty()) {
	    prob *= scanProperty(xpd);
	  }
	 else {
	    prob *= scanElement(xpd);
	  }
       }
      c = pattern_string.charAt(pattern_index++);
      if (c != ')') throw new Exception("AST ) expected");
      return prob;
    }

   private double scanProperty(StructuralPropertyDescriptor spd) throws Exception {
      char c = pattern_string.charAt(pattern_index++);
      if (c != '(') throw new Exception("Property ( expected");
      c = pattern_string.charAt(pattern_index++);
      if (c != '=') throw new Exception("Property = expected");
      StringBuffer buf = new StringBuffer();
      for ( ; ; ) {
	 c = pattern_string.charAt(pattern_index++);
	 if (c == ')') break;
	 buf.append(c);
       }
      return paca_stats.getPropProb(buf.toString(),spd);
    }

   private double scanElement(StructuralPropertyDescriptor spd) throws Exception {
      char c = pattern_string.charAt(pattern_index++);
      if (c != '(') throw new Exception("Element ( expected " + pattern_index);
      c = pattern_string.charAt(pattern_index);
      double prob = 1.0;
      if (Character.isDigit(c)) return scanAst(spd);
      else if (c == '(' || c == ')') {
	 double v0 = paca_stats.getListEmptyProb(spd);
	 if (v0 >= 0) {
	    if (c == ')') prob = v0;
	    else prob = 1.0 - v0;
	  }
	 while (c == '(') {             // list
	    prob *= scanElement(spd);
	    c = pattern_string.charAt(pattern_index);
	  }
       }
      else if (c == '?') {
	 ++pattern_index;
       }
      else if (c == 'Z') {
	 prob = paca_stats.getEmptyProb(spd);
	 if (prob == 0) prob = 1.0;
	 ++pattern_index;
       }
      else if (c == 'N' || c == 'E' || c == 'S' || c == 'T') {
	 ++pattern_index;
	 c = pattern_string.charAt(pattern_index++);
	 if (c != '?') throw new Exception("Any ? expected");
	 c = pattern_string.charAt(pattern_index++);
	 while (Character.isDigit(c)) c = pattern_string.charAt(pattern_index++);
	 --pattern_index;
       }
      c = pattern_string.charAt(pattern_index++);
      if (c != ')') throw new Exception("Element ) expected");
      return prob;
    }

}   // end of subclass PatternScanner




/********************************************************************************/
/*										*/
/*	Pattern expansion methods						*/
/*										*/
/********************************************************************************/

void expandPattern(int lssz,Stack<BparePattern> workq)
{
   if (!canBeExpanded()) return;

   Set<BparePatternNode> done = new HashSet<BparePatternNode>();

   if (lssz > 0 && first_change == null) {
      // TODO: Need to create intermediate patterns for the list elements
      root_node.expandListSets(lssz,workq);
    }

  List<BparePatternNode> pns = root_node.findExpansionNodes(done,first_change);
   if (pns != null) {
      for (BparePatternNode pn : pns) {
	 BparePattern pp = new BparePattern(this,pn);
	 // System.err.println("ADD PATTERN " + pp);
	 workq.push(pp);
	 done.add(pn);
       }
    }
}



Set<String> getParentPatterns()
{
   return root_node.findParentPatterns();
}



/********************************************************************************/
/*										*/
/*	Comparison methods							*/
/*										*/
/********************************************************************************/

int compareTo(BparePattern p,BpareStatistics ps)
{
   int i = getSize() - p.getSize();
   if (i < 0) return -1;
   if (i > 0) return 1;
   i = getNestLevel() - p.getNestLevel();
   if (i < 0) return -1;
   if (i > 0) return 1;
   i = getMaxListSize() - p.getMaxListSize();
   if (i < 0) return -1;
   if (i > 0) return 1;

   if (ps != null) {
      double d = p.getProb(ps) - getProb(ps);
      if (d < 0) return -1;
      if (d > 0) return 1;
    }

   return getPatternString().compareTo(p.getPatternString());
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputPatternXml(IvyXmlWriter xw,BpareStatistics ps)
{
   xw.begin("PATTERN");
   xw.field("SIZE",getSize());
   xw.field("NEST",getNestLevel());
   xw.field("LIST",getMaxListSize());
   if (ps != null) {
      xw.field("MINPROB",getMinExpandProb(ps));
      xw.field("PROB",getProb(ps));
    }

   int ct = 0;
   for (MatchInfo mi : match_data) {
      if (mi.isValid()) ++ct;
    }
   xw.field("VARS",ct);

   BpareMatchNormalizer norm = createNormalizer();

   root_node.outputPatternXml(xw,norm,ps);

   xw.end("PATTERN");
}



void outputPatternString(PrintWriter pw)
{
   pw.println(getPatternString());
}




String getPatternString()
{
   StringBuffer buf = new StringBuffer();
   BpareMatchNormalizer norm = createNormalizer();

   if (root_node == null) buf.append("NULL");
   else root_node.outputPatternString(buf,norm);

   return buf.toString();
}



@Override public String toString()
{
   if (first_change == null) {
      return getPatternString();
    }
   StringBuffer buf = new StringBuffer();
   BpareMatchNormalizer norm = createNormalizer();
   root_node.outputPatternString(buf,norm);
   buf.append(" @ ");
   first_change.outputPatternString(buf,norm);
   return buf.toString();
}



private BpareMatchNormalizer createNormalizer()
{
   List<Integer> li = new ArrayList<Integer>();
   for (MatchInfo mi : match_data) {
      li.add(mi.getCount());
    }

   return new BpareMatchNormalizer(li);
}




/********************************************************************************/
/*										*/
/*	Class to hold match information 					*/
/*										*/
/********************************************************************************/

private static class MatchInfo {

   private ASTNode match_tree;
   private int match_count;
   private MatchType match_type;

   MatchInfo(ASTNode n,MatchType type) {
      match_tree = n;
      match_type = type;
      match_count = 1;
    }

   boolean isValid()			     { return match_count > 0; }

   boolean match(ASTNode n,MatchType mt) {
      if (!isValid()) return false;
      if (n == null || n == empty_node) return false;
      if (mt != match_type) return false;
      if (!ast_matcher.safeSubtreeMatch(match_tree,n)) return false;
      return true;
    }


   int getCount()			     { return match_count; }

   void incr()					     { ++match_count; }
   void decr() {
      if (match_count == 0) return;
      --match_count;
    }

}



}	// end of class BparePattern




/* end of BparePattern.java */

