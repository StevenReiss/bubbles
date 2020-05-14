/********************************************************************************/
/*										*/
/*		BparePatternNodeEmpty.java					*/
/*										*/
/*	Pattern node for an non-existant optional subtree			*/
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

import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.Type;

import java.util.HashSet;
import java.util.Set;


class BparePatternNodeEmpty extends BparePatternNode {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BparePatternNodeEmpty(BparePattern pp,BparePatternNode par,StructuralPropertyDescriptor spd)
{
   super(pp,par,null,spd);
}



@Override BparePatternNode cloneNode(BparePattern pp,BparePatternNode par,BparePatternNode [] exps)
{
   return new BparePatternNodeEmpty(pp,par,property_tag);
}




/********************************************************************************/
/*										*/
/*	Probability methods							*/
/*										*/
/********************************************************************************/

@Override double getProb(BpareStatistics ps)
{
   double p = ps.getEmptyProb(property_tag);
   if (p == 0) p = 1.0;

   return p;
}



/********************************************************************************/
/*										*/
/*	Expansion methods							*/
/*										*/
/********************************************************************************/

@Override Set<String> findParentPatterns()
{
   if (property_tag == null) return null;

   String p = null;
   ChildPropertyDescriptor cpd = (ChildPropertyDescriptor) property_tag;
   if (Expression.class.isAssignableFrom(cpd.getChildType())) p = "E";
   else if (Statement.class.isAssignableFrom(cpd.getChildType())) p = "S";
   else if (Type.class.isAssignableFrom(cpd.getChildType())) p = "T";
   if (p != null) {
      p = "\\(" + p + "\\?\\d+\\)";
      Set<String> rslt = new HashSet<String>();
      rslt.add(p);
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override void outputPatternXml(IvyXmlWriter xw,BpareMatchNormalizer norm,BpareStatistics ps)
{
   xw.begin("NODE");

   xw.field("TYPE","EMPTY");
   if (property_tag != null) xw.field("PROPERTY",property_tag.getId());
   if (ps != null) xw.field("PROB",getProb(ps));

   xw.end("NODE");
}



@Override void outputPatternString(StringBuffer buf,BpareMatchNormalizer norm)
{
   buf.append("(Z)");
}



@Override void outputRegex(StringBuffer buf)
{
   buf.append("\\(Z\\)");
}




}	// end of class BparePatternNodeEmpty




/* end of BparePatternNodeEmpty.java */

