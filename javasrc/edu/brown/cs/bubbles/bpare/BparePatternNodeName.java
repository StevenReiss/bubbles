/********************************************************************************/
/*										*/
/*		BparePatternNodeName.java					*/
/*										*/
/*	Pattern node for a name 						*/
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
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

import java.util.HashSet;
import java.util.Set;


class BparePatternNodeName extends BparePatternNode {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private int		match_id;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BparePatternNodeName(BparePattern pp,BparePatternNode par,ASTNode n,
      StructuralPropertyDescriptor spd)
{
   super(pp,par,n,spd);

   match_id = pp.getMatch(n,MatchType.MATCH_NAME);
}




@Override BparePatternNode cloneNode(BparePattern pp,BparePatternNode par,BparePatternNode [] exps)
{
   return new BparePatternNodeName(pp,par,tree_node,property_tag);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override int getMatchId()				{ return match_id; }



@Override int getNestLevel(int lvl)
{
   int mlvl = -1;

   if (lvl == 0) mlvl = 0;
   else if (tree_node instanceof Expression || tree_node instanceof Statement) {
      mlvl = lvl;
    }

   return mlvl;
}



@Override double getProb(BpareStatistics ps)
{
   return ps.getAstProb(ASTNode.SIMPLE_NAME,property_tag);
}



/********************************************************************************/
/*										*/
/*	Expansion methods							*/
/*										*/
/********************************************************************************/

@Override Set<String> findParentPatterns()
{
   if (property_tag != null && property_tag.isChildProperty()) {
      ChildPropertyDescriptor cpd = (ChildPropertyDescriptor) property_tag;
      Class<?> c = cpd.getChildType();
      if (Name.class.isAssignableFrom(c)) return null;
    }

   Set<String> rslt = new HashSet<String>();
   String p = "\\(E\\?\\d+\\)";
   rslt.add(p);
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

   xw.field("TYPE","NAME");
   if (property_tag != null) xw.field("PROPERTY",property_tag.getId());
   if (ps != null) xw.field("PROB",getProb(ps));
   xw.field("MATCH",norm.normalize(match_id));

   xw.end("NODE");
}



@Override void outputPatternString(StringBuffer buf,BpareMatchNormalizer norm)
{
   buf.append("(N");
   buf.append("?");
   if (norm != null) buf.append(norm.normalize(match_id));
   buf.append(")");
}




@Override void outputRegex(StringBuffer buf)
{
   buf.append("\\(N\\?\\d+\\)");
}




}	// end of class BparePatternNodeName




/* end of BparePatternNodeName.java */


