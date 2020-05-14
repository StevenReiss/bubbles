/********************************************************************************/
/*										*/
/*		BparePatternNode.java						*/
/*										*/
/*	Instance of a pattern or potential pattern in BPARE			*/
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
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.Type;

import java.util.List;
import java.util.Set;
import java.util.Stack;



abstract class BparePatternNode implements BpareConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected BparePattern		for_pattern;
protected BparePatternNode      parent_node;
protected ASTNode		tree_node;
protected StructuralPropertyDescriptor property_tag;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected BparePatternNode(BparePattern pp,BparePatternNode par,ASTNode n,
      StructuralPropertyDescriptor spd)
{
   for_pattern = pp;
   tree_node = n;
   property_tag = spd;
   parent_node = par;
}




abstract BparePatternNode cloneNode(BparePattern pp,BparePatternNode npar,BparePatternNode [] exps);



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

int getMatchId()				{ return -1; }


int getSize()					{ return 1; }


int getNestLevel(int lvl)			{ return -1; }


BparePatternNode getParent()                    { return parent_node; }
int getNumChildren()                            { return 0; }
BparePatternNode getChild(int i)                { return null; }
int getChildNumber(BparePatternNode cn)
{
   int ct = getNumChildren();
   for (int i = 0; i < ct; ++i) {
      if (getChild(i) == cn) return i;
    }
   return -1;
}


int getMaxListSize()				{ return 0; }


double getMinExpandProb(double p,BpareStatistics ps)	{ return p; }

double getFixedProb(BpareStatistics ps) 		{ return 1.0; }

double getProb(BpareStatistics ps)			{ return 1.0; }

ASTNode getActiveNode() 			{ return tree_node; }



/********************************************************************************/
/*										*/
/*	Normalization methods							*/
/*										*/
/********************************************************************************/

protected BparePatternNode createNewNode(BparePatternNode par,ASTNode n,
      StructuralPropertyDescriptor spd)
{
   BparePatternNode pn = null;

   if (n == null) {
      if (spd.isChildProperty()) {
	 ChildPropertyDescriptor cpd = (ChildPropertyDescriptor) spd;
	 Class<?> c = cpd.getChildType();
	 if (Name.class.isAssignableFrom(c)) ;
	 else if (Expression.class.isAssignableFrom(c))
	    pn = new BparePatternNodeAny(for_pattern,par,MatchType.MATCH_EXPR,n,spd,c);
	 else if (Statement.class.isAssignableFrom(c))
	    pn = new BparePatternNodeAny(for_pattern,par,MatchType.MATCH_STMT,n,spd,c);
	 else if (Type.class.isAssignableFrom(c))
	    pn = new BparePatternNodeAny(for_pattern,par,MatchType.MATCH_TYPE,n,spd,c);
       }
      if (pn == null) pn = new BparePatternNodeEmpty(for_pattern,par,spd);
    }
   else if (n instanceof Name) {
      if (spd.isChildProperty()) {
	 ChildPropertyDescriptor cpd = (ChildPropertyDescriptor) spd;
	 Class<?> c = cpd.getChildType();
	 if (Name.class.isAssignableFrom(c)) {		// if c has to be a name, mark as such
	    pn = new BparePatternNodeName(for_pattern,par,n,spd);
	  }
       }
      if (pn == null) {
	 pn = new BparePatternNodeAny(for_pattern,par,MatchType.MATCH_EXPR,n,spd,Expression.class);
       }
    }
   else if (n instanceof Expression) {
      pn = new BparePatternNodeAny(for_pattern,par,MatchType.MATCH_EXPR,n,spd,Expression.class);
    }
   else if (n instanceof Statement && !(n instanceof SwitchCase)) {
      pn = new BparePatternNodeAny(for_pattern,par,MatchType.MATCH_STMT,n,spd,Statement.class);
    }
   else if (n instanceof Type) {
      pn = new BparePatternNodeAny(for_pattern,par,MatchType.MATCH_TYPE,n,spd,Type.class);
    }
   else {
      pn = new BparePatternNodeAst(for_pattern,par,n,spd);
    }

   return pn;
}



/********************************************************************************/
/*										*/
/*	Methods for expanding a tree						*/
/*										*/
/********************************************************************************/

BparePatternNode expandNode(BparePattern npp,BparePatternNode par)
{
   return null;
}




List<BparePatternNode> findExpansionNodes(Set<BparePatternNode> done)
{
   return null;
}


List<BparePatternNode> findExpansionNodes(Set<BparePatternNode> done,BparePatternNode first)
{
   List<BparePatternNode> rslt = null;
   
   if (first == null) {
      rslt = findExpansionNodes(done);
    }
   else {
      rslt = first.findExpansionNodes(done);
      if (rslt != null) return rslt;
      int ct0 = first.getNumChildren();
      for (int i = 0; i < ct0; ++i) {
         rslt = first.getChild(i).findExpansionNodes(done);
         if (rslt != null) return rslt;
       }
      for (BparePatternNode pn = first.getParent(); pn != null; pn = pn.getParent()) {
         int ct = pn.getNumChildren();
         int cn = pn.getChildNumber(first);
         if (cn < 0) return null;
         while ((++cn) < ct) {
            rslt = pn.getChild(cn).findExpansionNodes(done);
            if (rslt != null) return rslt;
          }
         first = pn;
       }
    }
      
   return rslt;
}






Set<String> findParentPatterns()
{
   return null;
}


void expandListSets(int lssz,Stack<BparePattern> q)	 { }




/********************************************************************************/
/*										*/
/*	Testing methods 							*/
/*										*/
/********************************************************************************/

boolean isTested(Set<ASTNode> done)			{ return false; }



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

abstract void outputPatternXml(IvyXmlWriter xw,BpareMatchNormalizer norm,BpareStatistics ps);


abstract void outputPatternString(StringBuffer buf,BpareMatchNormalizer norm);


abstract void outputRegex(StringBuffer buf);


@Override public String toString()
{
   StringBuffer buf = new StringBuffer();
   outputPatternString(buf,null);
   return buf.toString();
}



}	// end of class BparePatternNode






/* end of BparePatternNode.java */

