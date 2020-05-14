/********************************************************************************/
/*										*/
/*		BparePatternNodeList.java					*/
/*										*/
/*	Pattern node for a list component of an AST				*/
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
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


class BparePatternNodeList extends BparePatternNode {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<BparePatternNode> sub_nodes;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BparePatternNodeList(BparePattern pp,BparePatternNode par,ASTNode n,
      StructuralPropertyDescriptor spd,boolean expand)
{
   super(pp,par,n,spd);

   sub_nodes = null;

   if (expand) {
      sub_nodes = new ArrayList<BparePatternNode>();
      List<?> cl = (List<?>) tree_node.getStructuralProperty(property_tag);
      for (Iterator<?> it = cl.iterator(); it.hasNext(); ) {
	 ASTNode sn = (ASTNode) it.next();
	 BparePatternNode np = createNewNode(par,sn,property_tag);
	 sub_nodes.add(np);
       }
    }
}



BparePatternNodeList(BparePattern pp,BparePatternNode par,ASTNode n,
      StructuralPropertyDescriptor spd,int first,int ct)
{
   super(pp,par,n,spd);

   sub_nodes = new ArrayList<BparePatternNode>();
   List<?> cl = (List<?>) tree_node.getStructuralProperty(property_tag);
   for (int i = 0; i < ct; ++i) {
      ASTNode sn = (ASTNode) cl.get(first+i);
      BparePatternNode np = createNewNode(this,sn,property_tag);
      sub_nodes.add(np);
    }
}



BparePatternNodeList(BparePattern pp,BparePatternNode npar,BparePatternNodeList p,
      BparePatternNode [] exps)
{
   super(pp,npar,p.tree_node,p.property_tag);

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




@Override BparePatternNode cloneNode(BparePattern pp,BparePatternNode par,BparePatternNode [] exps)
{
   if (sub_nodes == null) {
      return new BparePatternNodeList(pp,par,tree_node,property_tag,false);
    }

   return new BparePatternNodeList(pp,par,this,exps);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override int getSize()
{
   int sz = 1;
   if (sub_nodes != null) {
      sz += 1;
      for (BparePatternNode sn : sub_nodes) sz += sn.getSize();
    }
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

   if (sub_nodes != null) {
      mlvl = lvl;
      for (BparePatternNode sn : sub_nodes) {
	 int nlvl = sn.getNestLevel(lvl+1);
	 if (nlvl > 0 && nlvl > mlvl) mlvl = nlvl;
       }
    }

   return mlvl;
}



@Override int getMaxListSize()
{
   if (sub_nodes == null) return 0;

   int sz = sub_nodes.size();
   for (BparePatternNode sn : sub_nodes) {
      int ssz = sn.getMaxListSize();
      if (ssz > sz) sz = ssz;
    }

   return sz;
}



@Override double getMinExpandProb(double p,BpareStatistics ps)
{
   if (sub_nodes == null) return p;

   double p1 = p;
   for (BparePatternNode sn : sub_nodes) {
      double p2 = sn.getMinExpandProb(p,ps);
      if (p2 < p1) p1 = p2;
    }

   return p1;
}




@Override double getProb(BpareStatistics ps)
{
   double p = 1;

   if (sub_nodes != null) {
      double v0 = ps.getListEmptyProb(property_tag);
      if (v0 >= 0) {
	 if (sub_nodes.isEmpty()) p = v0;
	 else p = 1.0 - v0;
       }
      for (BparePatternNode sn : sub_nodes) {
	 p *= sn.getProb(ps);
       }
    }

   return p;
}




/********************************************************************************/
/*										*/
/*	Expansion methods							*/
/*										*/
/********************************************************************************/

@Override BparePatternNode expandNode(BparePattern npp,BparePatternNode par)
{
   if (sub_nodes != null) return null;

   return new BparePatternNodeList(npp,par,tree_node,property_tag,true);
}



@Override List<BparePatternNode> findExpansionNodes(Set<BparePatternNode> done)
{
   if (!done.add(this)) return null;

   List<BparePatternNode> lrslt = new ArrayList<BparePatternNode>();
   
   if (sub_nodes != null) {
      for (BparePatternNode pp : sub_nodes) {
	 List<BparePatternNode> rslt = pp.findExpansionNodes(done);
	 if (rslt != null) lrslt.addAll(rslt);
       }
    }
   else {
      if (!BpareStatistics.expandListProperty(property_tag)) return null;
      lrslt.add(this);
    }
   
   if (lrslt.isEmpty()) return null;

   return lrslt;
}




@Override Set<String> findParentPatterns()
{
   if (sub_nodes == null) return null;

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
   if (rslt == null) {
      String p = "\\(\\?\\)";
      rslt = new HashSet<String>();
      rslt.add(p);
    }

   return rslt;
}






/********************************************************************************/
/*										*/
/*	Testing methods 							*/
/*										*/
/********************************************************************************/

@Override boolean isTested(Set<ASTNode> done)
{
   if (sub_nodes == null) return false;

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
   ChildListPropertyDescriptor clpd = (ChildListPropertyDescriptor) property_tag;

   xw.begin("NODE");

   if (sub_nodes == null) {
      xw.field("TYPE","LIST");
      xw.field("ELEMENTTYPE",BpareStatistics.getNodeName(clpd.getElementType()));
      xw.field("PROPERTY",property_tag.getId());
      if (ps != null) xw.field("PROB",getProb(ps));
    }
   else {
      xw.field("TYPE","ASTLIST");
      xw.field("ELEMENTTYPE",BpareStatistics.getNodeName(clpd.getElementType()));
      xw.field("PROPERTY",property_tag.getId());
      if (ps != null) xw.field("PROB",getProb(ps));

      for (BparePatternNode sn : sub_nodes) {
	 sn.outputPatternXml(xw,norm,ps);
       }
    }

   xw.end("NODE");
}



@Override void outputPatternString(StringBuffer buf,BpareMatchNormalizer norm)
{
   buf.append("(");
   if (sub_nodes == null) buf.append("?");
   else {
      for (BparePatternNode sn : sub_nodes) {
	 sn.outputPatternString(buf,norm);
       }
    }
   buf.append(")");
}




@Override void outputRegex(StringBuffer buf)
{
   buf.append("\\(");
   if (sub_nodes == null) buf.append("\\?");
   else {
      for (BparePatternNode sn : sub_nodes) {
	 sn.outputRegex(buf);
       }
    }
   buf.append("\\)");
}




}	// end of class BparePatternNodeList




/* end of BparePatternNodeList.java */


