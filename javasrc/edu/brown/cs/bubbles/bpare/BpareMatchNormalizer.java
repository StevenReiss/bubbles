/********************************************************************************/
/*										*/
/*		BpareMatchNormalizer.java					*/
/*										*/
/*	Class holding normalized match numbers in a pattern			*/
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


import java.util.List;



class BpareMatchNormalizer implements BpareConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private int []		match_ids;
private int		next_match;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BpareMatchNormalizer(List<Integer> counts)
{
   int sz = counts.size();
   match_ids = new int[sz];
   next_match = 0;
   for (int i = 0; i < sz; ++i) match_ids[i] = -1;
}




/********************************************************************************/
/*										*/
/*	Conversion method							*/
/*										*/
/********************************************************************************/

int normalize(int i)
{
   if (match_ids[i] < 0) {
      match_ids[i] = next_match++;
    }

   return match_ids[i];
}




}	// end of class BpareMatchNormalizer




/* end of BpareMatchNormalizer.java */

