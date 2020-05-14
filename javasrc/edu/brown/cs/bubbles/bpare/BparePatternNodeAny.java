/********************************************************************************/
/*										*/
/*		BparePatternNodeAny.java					*/
/*										*/
/*	Pattern node representing anything					*/
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
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


class BparePatternNodeAny extends BparePatternNode {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private MatchType	match_type;
private int		match_id;
private Class<?>		base_class;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BparePatternNodeAny(BparePattern pp,BparePatternNode par,MatchType typ,ASTNode n,
      StructuralPropertyDescriptor spd,Class<?> base)
{
   super(pp,par,n,spd);

   base_class = base;

   match_type = typ;
   match_id = pp.getMatch(n,typ);
}




@Override BparePatternNode cloneNode(BparePattern pp,BparePatternNode npar,BparePatternNode [] exps)
{
   return new BparePatternNodeAny(pp,npar,match_type,tree_node,property_tag,base_class);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override int getMatchId()				{ return match_id; }

@Override double getProb(BpareStatistics ps)
{
   Class<?> c = base_class;

   if (property_tag != null && property_tag.isChildProperty()) {
      ChildPropertyDescriptor cpd = (ChildPropertyDescriptor) property_tag;
      c = cpd.getChildType();
    }
   else if (property_tag != null && property_tag.isChildListProperty()) {
      ChildListPropertyDescriptor clpd = (ChildListPropertyDescriptor) property_tag;
      c = clpd.getElementType();
    }

   if (c == base_class) return 1.0;

   double p1 = ps.getSuperProb(c);
   double p2 = ps.getSuperProb(base_class);

   if (p2 > p1) return 1.0;

   return p2/p1;
}



/********************************************************************************/
/*										*/
/*	Expansion methods							*/
/*										*/
/********************************************************************************/

@Override BparePatternNode expandNode(BparePattern npp,BparePatternNode npar)
{
   if (tree_node == null) {
      return new BparePatternNodeEmpty(npp,npar,property_tag);
    }
   else if (tree_node instanceof Name) {
      return new BparePatternNodeName(npp,npar,tree_node,property_tag); 
    }

   return new BparePatternNodeAst(npp,npar,tree_node,property_tag);
}




@Override List<BparePatternNode> findExpansionNodes(Set<BparePatternNode> done)
{
   if (!done.add(this)) return null;

   List<BparePatternNode> rslt = new ArrayList<BparePatternNode>();
   rslt.add(this);
   
   return rslt;
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override void outputPatternXml(IvyXmlWriter xw,BpareMatchNormalizer norm,BpareStatistics ps)
{
   xw.begin("NODE");

   xw.field("TYPE",match_type);
   if (property_tag != null) xw.field("PROPERTY",property_tag.getId());
   if (ps != null) xw.field("PROB",getProb(ps));
   xw.field("MATCH",norm.normalize(match_id));

   xw.end("NODE");
}



@Override void outputPatternString(StringBuffer buf,BpareMatchNormalizer norm)
{
   buf.append("(");
   buf.append(getKey());
   buf.append("?");
   if (norm != null) buf.append(norm.normalize(match_id));
   buf.append(")");
}



@Override void outputRegex(StringBuffer buf)
{
   buf.append("\\(");
   buf.append(getKey());
   buf.append("\\?\\d+\\)");
}



private String getKey()
{
   String key = match_type.toString();
   if (key.startsWith("MATCH_")) key = key.substring(6);
   key = key.substring(0,1);
   return key;
}



}	// end of class BparePatternNodeAny




/* end of BparePatternNodeAny.java */

