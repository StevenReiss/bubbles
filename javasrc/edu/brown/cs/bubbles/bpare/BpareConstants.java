/********************************************************************************/
/*										*/
/*		BpareConstants.java						*/
/*										*/
/*	Bubble Pattern-Assisted Recommendation Engine constant definitions	*/
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




public interface BpareConstants {



/********************************************************************************/
/*										*/
/*	Constants defining valid patterns					*/
/*										*/
/********************************************************************************/

int	MIN_SIZE = 1; // 20;			// min # of pattern nodes
int	MAX_SIZE = 50;			// max # of pattern nodes
int	MIN_MATCH = 0;			// min # of match pairs
int	MAX_VARIABLE = 10;		// max # of variables

int	LIST_SET_SIZE = 6;		// if non-zero, consider all sets of
					// consecutive elements in a list of this size



/********************************************************************************/
/*                                                                              */
/*      Pattern types                                                           */
/*                                                                              */
/********************************************************************************/

enum PatternType {
   STRING,                      // tree patterns ala paca
   TRIE                         // accumulate trees into a trie
}



/********************************************************************************/
/*										*/
/*	Match type constants							*/
/*										*/
/********************************************************************************/

enum MatchType {
   MATCH_STMT,
   MATCH_EXPR,
   MATCH_TYPE,
   MATCH_NAME
}


/********************************************************************************/
/*										*/
/*	Constants for reporting 						*/
/*										*/
/********************************************************************************/

int	MIN_CUTOFF = 0;
int	MATCH_SIZE = 0;
int	NEST_LEVEL = 0;
double	MIN_PROB = 0;
int	LIST_SIZE = 0;
int	MIN_REPORT_SIZE = 0;






}	// end of interface BpareConstants



/* end of BpareConstants.java */
