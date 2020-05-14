/********************************************************************************/
/*										*/
/*		BparePatternNodeProperty.java					*/
/*										*/
/*	Pattern node for a property value					*/
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

import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

import java.util.List;
import java.util.Set;


class BparePatternNodeProperty extends BparePatternNode {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Object	property_value;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BparePatternNodeProperty(BparePattern pp,BparePatternNode par,
      StructuralPropertyDescriptor spd,Object o)
{
   super(pp,par,null,spd);

   property_value = o;
}



@Override BparePatternNode cloneNode(BparePattern pp,BparePatternNode par,BparePatternNode [] exps)
{
   return new BparePatternNodeProperty(pp,par,property_tag,property_value);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override double getFixedProb(BpareStatistics ps)
{
   return ps.getPropProb(getText(),property_tag);
}



String getText()
{
   if (property_value == null) return "";

   String v = property_value.toString();
   if (v.equalsIgnoreCase("true")) v = "t";
   else if (v.equalsIgnoreCase("false")) v = "f";

   return v;
}


@Override double getProb(BpareStatistics ps)
{
   return getFixedProb(ps);
}



/********************************************************************************/
/*										*/
/*	Expansion methods							*/
/*										*/
/********************************************************************************/

@Override List<BparePatternNode> findExpansionNodes(Set<BparePatternNode> done)
{
   if (!done.add(this)) return null;

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

   xw.field("TYPE","PROPERTY");
   xw.field("PROPERTY",property_tag.getId());
   if (ps != null) xw.field("PROB",getProb(ps));
   xw.text(getText());

   xw.end("NODE");
}



@Override void outputPatternString(StringBuffer buf,BpareMatchNormalizer norm)
{
   buf.append("(=");
   buf.append(getText());
   buf.append(")");
}




@Override void outputRegex(StringBuffer buf)
{
   buf.append("\\Q(=");
   buf.append(getText());
   buf.append(")\\E");
}




}	// end of class BparePatternNodeProperty




/* end of BparePatternNodeProperty.java */



