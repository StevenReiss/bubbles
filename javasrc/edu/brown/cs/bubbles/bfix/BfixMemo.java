/********************************************************************************/
/*										*/
/*		BfixMemo.java							*/
/*										*/
/*	Object describing (memoizing) a pending fix				*/
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



package edu.brown.cs.bubbles.bfix;

import edu.brown.cs.bubbles.bump.BumpConstants.BumpProblem;

class BfixMemo implements BfixConstants, Comparable<BfixMemo>
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BumpProblem for_problem;
private Class<?> for_adapter;
private String id_string;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BfixMemo(BumpProblem bp,Class<?> ba,String id)
{
   for_problem = bp;
   for_adapter = ba;
   id_string = id;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

boolean match(BumpProblem bp,Class<?> ba,String id)
{
   if (for_problem != bp || for_adapter != ba) return false;
   if (id == null && id_string == null) return true;
   if (id == null || id_string == null) return false;
   return id.equals(id_string);
}



/********************************************************************************/
/*										*/
/*	Comparable methods							*/
/*										*/
/********************************************************************************/

@Override public int compareTo(BfixMemo m)
{
   if (m == this) return 0;
   int d = for_problem.hashCode() - m.for_problem.hashCode();
   if (d != 0) return d;
   d = for_adapter.hashCode() - m.for_adapter.hashCode();
   if (d != 0) return d;
   if (id_string == null && m.id_string == null) return 0;
   if (id_string == null) return -1;
   if (m.id_string == null) return 1;
   return id_string.compareTo(m.id_string);
}



/********************************************************************************/
/*										*/
/*	Hashcode and equals for sets						*/
/*										*/
/********************************************************************************/

@Override public int hashCode()
{
   int hc = for_problem.hashCode() ^ for_adapter.hashCode();
   if (id_string != null) hc += id_string.hashCode();
   return hc;
}

@Override public boolean equals(Object o)
{
   if (o == null) return false;
   if (o instanceof BfixMemo) {
      BfixMemo bm = (BfixMemo) o;
      return match(bm.for_problem,bm.for_adapter,bm.id_string);
    }
   return false;
}



}	// end of class BfixMemo




/* end of BfixMemo.java */



























































































































