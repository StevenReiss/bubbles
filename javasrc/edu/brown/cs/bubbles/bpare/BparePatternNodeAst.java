/********************************************************************************/
/*										*/
/*		BparePatternNodeAst.java					*/
/*										*/
/*	Pattern node for a particular AST					*/
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
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.Type;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;


class BparePatternNodeAst extends BparePatternNode {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private int	ast_type;
private List<BparePatternNode> sub_nodes;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BparePatternNodeAst(BparePattern pp,BparePatternNode par,ASTNode n,
      StructuralPropertyDescriptor xpd)
{
   super(pp,par,n,xpd);

   ast_type = n.getNodeType();

   sub_nodes = new ArrayList<BparePatternNode>();

   for (StructuralPropertyDescriptor spd : BpareStatistics.getStructuralProperties(tree_node)) {
      if (spd.isSimpleProperty()) {
	 Object o = tree_node.getStructuralProperty(spd);
	 BparePatternNode np = new BparePatternNodeProperty(for_pattern,this,spd,o);
	 sub_nodes.add(np);
       }
      else if (spd.isChildProperty()) {
	 ASTNode sn = (ASTNode) tree_node.getStructuralProperty(spd);
	 BparePatternNode np = createNewNode(this,sn,spd);
	 sub_nodes.add(np);
       }
      else if (spd.isChildListProperty()) {
	 BparePatternNode np = new BparePatternNodeList(for_pattern,this,tree_node,spd,false);
	 sub_nodes.add(np);
       }
    }
}



BparePatternNodeAst(BparePattern pp,BparePatternNode par,BparePatternNodeAst p,
      BparePatternNode [] exps)
{
   super(pp,par,p.tree_node,p.property_tag);

   ast_type = p.ast_type;

   sub_nodes = new ArrayList<BparePatternNode>();

   for (BparePatternNode sn : p.sub_nodes) {
      if (exps != null && exps[0] == sn) {
	 exps[0] = null;
	 BparePatternNode nn = sn.expandNode(pp,this);
	 if (nn != null) {
	    exps[0] = nn;
	    sub_nodes.add(nn);
	  }
       }
      else sub_nodes.add(sn.cloneNode(pp,this,exps));
    }
}




@Override BparePatternNode cloneNode(BparePattern pp,BparePatternNode npar,BparePatternNode [] exps)
{
   return new BparePatternNodeAst(pp,npar,this,exps);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override int getSize()
{
   int sz = 1;
   for (BparePatternNode sn : sub_nodes) sz += sn.getSize();
   return sz;
}


@Override int getNumChildren()
{
   return sub_nodes.size();
}

@Override BparePatternNode getChild(int i)
{
   return sub_nodes.get(i);
}



@Override int getNestLevel(int lvl)
{
   int mlvl = -1;

   if (lvl == 0) mlvl = 0;
   else if (tree_node instanceof Expression || tree_node instanceof Statement) {
      mlvl = lvl;
    }

   for (BparePatternNode sn : sub_nodes) {
      int nlvl = sn.getNestLevel(lvl+1);
      if (nlvl > 0 && nlvl > mlvl) mlvl = nlvl;
    }

   return mlvl;
}



@Override int getMaxListSize()
{
   int sz = 0;
   for (BparePatternNode sn : sub_nodes) {
      int ssz = sn.getMaxListSize();
      if (ssz > sz) sz = ssz;
    }
   return sz;
}



@Override double getMinExpandProb(double p,BpareStatistics ps)
{
   p *= ps.getAstProb(ast_type,property_tag);

   double p1 = p;
   for (BparePatternNode sn : sub_nodes) {
      p *= sn.getFixedProb(ps);
    }

   for (BparePatternNode sn : sub_nodes) {
      double p2 = sn.getMinExpandProb(p,ps);
      if (p2 < p1) p1 = p2;
    }

   return p1;
}



@Override double getProb(BpareStatistics ps)
{
   double p = ps.getAstProb(ast_type,property_tag);
   if (p == 0) System.err.println("NEW AST TYPE " + ast_type);
   for (BparePatternNode sn : sub_nodes) {
      p *= sn.getProb(ps);
    }

   return p;
}



/********************************************************************************/
/*										*/
/*	Expansion methods							*/
/*										*/
/********************************************************************************/

@Override List<BparePatternNode> findExpansionNodes(Set<BparePatternNode> done)
{
   if (!done.add(this)) return null;

   List<BparePatternNode> lrslt = new ArrayList<>();
   for (BparePatternNode pp : sub_nodes) {
      List<BparePatternNode> rslt = pp.findExpansionNodes(done);
      if (rslt != null) lrslt.addAll(rslt);
    }

   if (lrslt.isEmpty()) return null;

   return lrslt;
}




@Override Set<String> findParentPatterns()
{
   Set<?> [] sets = new Set[sub_nodes.size()];

   int ct = 0;
   for (BparePatternNode sn : sub_nodes) {
      sets[ct++] = sn.findParentPatterns();
    }

   Set<String> rslt = null;
   for (int i = 0; i < sets.length; ++i) {
      if (sets[i] != null) {
	 for (Iterator<?> it = sets[i].iterator(); it.hasNext(); ) {
	    String s = (String) it.next();
	    StringBuffer buf = new StringBuffer();
	    buf.append("\\(");
	    buf.append(ast_type);
	    int j = 0;
	    for (BparePatternNode sn : sub_nodes) {
	       if (j++ == i) buf.append(s);
	       else sn.outputRegex(buf);
	     }
	    buf.append("\\)");
	    if (rslt == null) rslt = new HashSet<String>();
	    rslt.add(buf.toString());
	  }
       }
    }
   if (rslt == null && tree_node != null) {
      String p = null;
      if (tree_node instanceof Expression) p = "\\(E\\?\\d+\\)";
      else if (tree_node instanceof Statement) {
	 if (!(tree_node instanceof SwitchCase)) p = "\\(S\\?\\d+\\)";
       }
      else if (tree_node instanceof Type) p = "\\(T\\?\\d+\\)";
      if (p != null) {
	 rslt = new HashSet<String>();
	 rslt.add(p);
       }
    }

   return rslt;
}



@Override void expandListSets(int lssz,Stack<BparePattern> workq)
{
   if (lssz == 0 || tree_node == null) return;

   for (StructuralPropertyDescriptor spd : BpareStatistics.getStructuralProperties(tree_node)) {
      if (spd.isChildListProperty() && BpareStatistics.useListProperty(spd)) {
	 List<?> cl = (List<?>) tree_node.getStructuralProperty(spd);
	 if (cl.size() >= lssz) {
	    int ct = cl.size() - lssz;
	    for (int i = 0; i < ct; ++i) {
	       BparePattern pp = new BparePattern(tree_node,spd,i,lssz);
	       workq.add(pp);
	     }
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Testing methods 							*/
/*										*/
/********************************************************************************/

@Override boolean isTested(Set<ASTNode> done)
{
   if (done.contains(tree_node)) return true;

   for (BparePatternNode pp : sub_nodes) {
      if (pp.isTested(done)) return true;
    }

   return false;
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override void outputPatternXml(IvyXmlWriter xw,BpareMatchNormalizer norm,BpareStatistics ps)
{
   xw.begin("NODE");

   xw.field("TYPE","AST");
   xw.field("NODENAME",BpareStatistics.getNodeName(tree_node.getClass()));
   xw.field("NODE",ast_type);
   if (property_tag != null) xw.field("PROPERTY",property_tag.getId());
   if (ps != null) xw.field("PROB",getProb(ps));

   for (BparePatternNode sn : sub_nodes) {
      sn.outputPatternXml(xw,norm,ps);
    }

   xw.end("NODE");
}



@Override void outputPatternString(StringBuffer buf,BpareMatchNormalizer norm)
{
   buf.append("(");
   buf.append(ast_type);
   for (BparePatternNode sn : sub_nodes) {
      sn.outputPatternString(buf,norm);
    }
   buf.append(")");
}



@Override void outputRegex(StringBuffer buf)
{
   buf.append("\\(");
   buf.append(ast_type);
   for (BparePatternNode sn : sub_nodes) sn.outputRegex(buf);
   buf.append("\\)");
}



}	// end of class BparePatternNodeAst




/* end of BparePatternNodeAst.java */

