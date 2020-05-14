/********************************************************************************/
/*										*/
/*		BpareTrie.java							*/
/*										*/
/*	Hold a trie of trees representing patterns				*/
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

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;



class BpareTrie implements BpareConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private TrieChoice	root_node;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BpareTrie()
{
   root_node = new TrieChoice();
}



/********************************************************************************/
/*										*/
/*	Trie builder methods							*/
/*										*/
/********************************************************************************/

void addToTrie(ASTNode n)
{
   root_node.addNode(n,n);
}

void outputXml(IvyXmlWriter xw)
{
   root_node.outputTrie(xw);
}



/********************************************************************************/
/*										*/
/*	Trie matching methods							*/
/*										*/
/********************************************************************************/

void match(ASTNode n)
{
   MatchResult mr = new MatchResult();

   root_node.match(n,1.0,mr);

   System.err.println("BPARE: Match result for " + n + " : ");
   mr.printResult();
}




/********************************************************************************/
/*										*/
/*	Trie node								*/
/*										*/
/********************************************************************************/

private static class TrieNode {

   private Map<Object,TrieChoice> option_sets;
   private int	    node_count;
   private Object   node_value;
   private Set<ASTNode> ref_set;

   TrieNode() {
      option_sets = new HashMap<Object,TrieChoice>();
      ref_set = new HashSet<ASTNode>();
    }

   int getCount()			{ return node_count; }

   void addBase(ASTNode n)		{ ref_set.add(n); }

   void addNode(ASTNode n,ASTNode base) {
      ++node_count;
      if (n == null) node_value = null;
      else {
	 node_value = Integer.valueOf(n.getNodeType());
	 for (Object ospd : n.structuralPropertiesForType()) {
	    StructuralPropertyDescriptor spd = (StructuralPropertyDescriptor) ospd;
	    TrieChoice tc = option_sets.get(spd);
	    if (tc == null) {
	       tc = new TrieChoice();
	       option_sets.put(spd,tc);
	     }
	    if (spd.isSimpleProperty()) {
	       Object opv = n.getStructuralProperty(spd);
	       String v = (opv == null ? null : opv.toString());
	       tc.addNode(v,base);
	     }
	    else if (spd.isChildListProperty()) {
	       List<?> lv = (List<?>) n.getStructuralProperty(spd);
	       tc.addNode(lv,base);
	     }
	    else if (spd.isChildProperty()) {
	       ASTNode cn = (ASTNode) n.getStructuralProperty(spd);
	       tc.addNode(cn,base);
	     }
	  }
       }
    }

   void addNode(String v,ASTNode base) {
      ++node_count;
      node_value = v;
    }

   void addNode(List<?> v,ASTNode base) {
      ++node_count;
      int ct = v.size();
      for (int i = 0; i < ct; ++i) {
	 TrieChoice tc = option_sets.get(i);
	 if (tc == null) {
	    tc = new TrieChoice();
	    option_sets.put(i,tc);
	  }
	 ASTNode ni = (ASTNode) v.get(i);
	 tc.addNode(ni,base);
       }
    }

   void match(ASTNode n,double pr,MatchResult mr) {
      mr.addResult(ref_set,pr);
      if (n == null) return;
      for (Object ospd : n.structuralPropertiesForType()) {
	 StructuralPropertyDescriptor spd = (StructuralPropertyDescriptor) ospd;
	 TrieChoice tc = option_sets.get(spd);
	 if (tc == null) return;
	 if (spd.isSimpleProperty()) {
	     Object opv = n.getStructuralProperty(spd);
	     String v = (opv == null ? null : opv.toString());
	     tc.match(v,pr,mr);
	  }
	 else if (spd.isChildListProperty()) {
	    List<?> lv = (List<?>) n.getStructuralProperty(spd);
	   tc.match(lv,pr,mr);
	  }
	 else if (spd.isChildProperty()) {
	    ASTNode cn = (ASTNode) n.getStructuralProperty(spd);
	    tc.match(cn,pr,mr);
	  }
       }
    }

   void match(String v,double pr,MatchResult mr) {
       mr.addResult(ref_set,pr);
    }

   void match(List<?> v,double pr,MatchResult mr) {
      // should be able to match for empty list
      // should be able to match for missing elements
      // should be able to match for skipped elements (start at k)

      int ct = v.size();
      for (int i = 0; i < ct; ++i) {
	 TrieChoice tc = option_sets.get(i);
	 if (tc == null) return;
	 ASTNode ni = (ASTNode) v.get(i);
	 tc.match(ni,pr,mr);
       }
    }

   void outputTrie(IvyXmlWriter xw) {
      xw.begin("NODE");
      xw.field("COUNT",node_count);
      if (node_value != null) xw.field("VALUE",node_value.toString());
      for (Map.Entry<Object,TrieChoice> ent : option_sets.entrySet()) {
	 xw.begin("CHILD");
	 if (ent.getKey() != null) xw.field("ID",ent.getKey().toString());
	 ent.getValue().outputTrie(xw);
	 xw.end("CHILD");
       }
      xw.end("NODE");
    }

}	// end of inner class TrieNode


private static class TrieChoice {

   private Map<Object,TrieNode> choice_sets;

   TrieChoice() {
      choice_sets = new HashMap<Object,TrieNode>();
    }

   void addNode(ASTNode n,ASTNode base) {
      int nt = (n == null ? 0 : n.getNodeType());
      Integer ntv = Integer.valueOf(nt);
      TrieNode tn = choice_sets.get(ntv);
      if (tn == null) {
	 tn = new TrieNode();
	 choice_sets.put(ntv,tn);
       }
      tn.addBase(base);
      tn.addNode(n,base);
    }

   void addNode(String v,ASTNode base) {
      TrieNode tn = choice_sets.get(v);
      if (tn == null) {
	 tn = new TrieNode();
	 choice_sets.put(v,tn);
       }
      tn.addBase(base);
      tn.addNode(v,base);
    }

   void addNode(List<?> v,ASTNode base) {
      TrieNode tn = choice_sets.get(0);
      if (tn == null) {
	 tn = new TrieNode();
	 choice_sets.put(0,tn);
       }
      tn.addBase(base);
      tn.addNode(v,base);
    }

   void match(ASTNode n,double p,MatchResult mr) {
      int nt = (n == null ? 0 : n.getNodeType());
      Integer ntv = Integer.valueOf(nt);
      TrieNode tn = choice_sets.get(ntv);
      if (tn == null) return;
      tn.match(n,p,mr);
    }

   void match(String v,double pr,MatchResult mr) {
      TrieNode tn = choice_sets.get(v);
      if (tn == null) return;
      tn.match(v,pr,mr);
    }

   void match(List<?> lv,double pr,MatchResult mr) {
      TrieNode tn = choice_sets.get(0);
      if (tn == null)  return;
      tn.match(lv,pr,mr);
    }

   void outputTrie(IvyXmlWriter xw) {
      for (Map.Entry<Object,TrieNode> ent : choice_sets.entrySet()) {
	 xw.begin("CHOICE");
	 if (ent.getKey() != null) xw.field("VALUE",ent.getKey().toString());
	 xw.field("COUNT",ent.getValue().getCount());
	 ent.getValue().outputTrie(xw);
	 xw.end("CHOICE");
       }
    }

}	// end of inner class TrieChoice



/********************************************************************************/
/*										*/
/*	Match result								*/
/*										*/
/********************************************************************************/

private static class MatchResult {

   private Map<ASTNode,double []> value_map;
   private static final int MAX_REF_SIZE = 200;

   MatchResult() {
      value_map = new HashMap<ASTNode,double []>();
    }

   void addResult(Collection<ASTNode> refs,double priority) {
      if (refs == null || refs.size() > MAX_REF_SIZE || priority == 0) return;
      for (ASTNode r : refs) {
	 double [] v = value_map.get(r);
	 if (v == null) {
	    v = new double[1];
	    v[0] = 0;
	    value_map.put(r,v);
	  }
	 v[0] += priority;
       }
    }

   void printResult() {
      System.err.println("BPARE: RESULT MAP: ");
      for (Map.Entry<ASTNode,double []> ent : value_map.entrySet()) {
	 System.err.println("   " + ent.getValue()[0] + ": " + ent.getKey());
       }
    }


}	// end of inner class MatchResult




}	// end of class BpareTrie




/* end of BpareTrie.java */

