/********************************************************************************/
/*										*/
/*		RebaseWordSpellCheck.java					*/
/*										*/
/*	Find the right word for a misspelling					*/
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



package edu.brown.cs.bubbles.rebase.word;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class RebaseWordSpellCheck implements RebaseWordConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private SpellCost	base_cost;
private SpellNode	trie_root;



/********************************************************************************/
/*										*/
/*	Standard costs								*/
/*										*/
/********************************************************************************/

private static final double  WORD_MAX_SPELL_ERROR =  0.50;

private static final double  WORD_INSERT_COST = 0.70;
private static final double  WORD_END_INSERT_COST = 0.65;
private static final double  WORD_DOUBLE_INSERT_COST = 0.60;

private static final double  WORD_DELETE_COST = 0.70;
private static final double  WORD_END_DELETE_COST = 0.65;
private static final double  WORD_DOUBLE_DELETE_COST = 0.60;

private static final double  WORD_DEFAULT_REPLACE_COST = 0.80;
private static final double  WORD_CORRECT_COST = -0.10;

private static final double  WORD_BASE_COST = 10.0;

private static final int     WORD_SHORT_WORD_LENGTH = 0;




/********************************************************************************/
/*										*/
/*	Replacement cost tables 						*/
/*										*/
/********************************************************************************/

private static final double DEF = WORD_DEFAULT_REPLACE_COST;


// from in rows, to in columns
//
private static final double PHON_REPLACE_COST[][] = {
    //	   a   b   c   d   e   f   g   h   i   j   k   l   m   n   o   p   q   r   s   t   u   v   w   z   y   z   -   0   1   2   3   4   5   6   7   8   9
/* a */  {0.0,DEF,DEF,DEF,0.8,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* b */  {DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.5,DEF,DEF,DEF,DEF,DEF,0.6,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* c */  {DEF,DEF,0.0,DEF,DEF,DEF,0.5,DEF,DEF,DEF,0.5,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* d */  {DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.5,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* e */  {DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,0.8,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* f */  {DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* g */  {DEF,DEF,0.5,DEF,DEF,DEF,0.0,DEF,DEF,DEF,0.5,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* h */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* i */  {DEF,DEF,DEF,DEF,0.8,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* j */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* k */  {DEF,DEF,0.5,DEF,DEF,DEF,0.5,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* l */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* m */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,0.5,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* n */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.5,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* o */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* p */  {DEF,0.5,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* q */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* r */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* s */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,0.5,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* t */  {DEF,DEF,DEF,0.5,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* u */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* v */  {DEF,0.6,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* w */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* x */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* y */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* z */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.5,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* - */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* 0 */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* 1 */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* 2 */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* 3 */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF},
/* 4 */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF},
/* 5 */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF},
/* 6 */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF},
/* 7 */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF},
/* 8 */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF},
/* 9 */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0}
};



private static final double UPP = 0.6;
private static final double DWN = 0.55;
private static final double RIT = 0.5;
private static final double LFT = 0.5;
private static final double DGN = 0.7;


private static final double KEYS_REPLACE_COST[][] = {
    //	   a   b   c   d   e   f   g   h   i   j   k   l   m   n   o   p   q   r   s   t   u   v   w   z   y   z   -   0   1   2   3   4   5   6   7   8   9
/* a */  {0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,UPP,DEF,RIT,DEF,DEF,DEF,DGN,DEF,DEF,DWN,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* b */  {DEF,0.0,DEF,DEF,DEF,DEF,UPP,DGN,DEF,DEF,DEF,DEF,DEF,RIT,DEF,DEF,DEF,DEF,DEF,DEF,DEF,LFT,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* c */  {DEF,DEF,0.0,UPP,DEF,DGN,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,RIT,DEF,LFT,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* d */  {DEF,DEF,DWN,0.0,UPP,RIT,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DGN,LFT,DEF,DEF,DEF,DEF,DGN,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* e */  {DEF,DEF,DEF,DWN,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,RIT,DGN,DEF,DEF,DEF,LFT,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* f */  {DEF,DEF,DGN,LFT,DEF,0.0,RIT,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,UPP,DEF,DGN,DEF,DWN,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* g */  {DEF,DWN,DEF,DEF,DEF,LFT,0.0,RIT,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,UPP,DEF,DGN,DEF,DEF,DGN,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* h */  {DEF,DGN,DEF,DEF,DEF,DEF,LFT,0.0,DEF,RIT,DEF,DEF,DEF,DWN,DEF,DEF,DEF,DEF,DEF,DEF,DGN,DEF,DEF,DEF,UPP,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* i */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DGN,DWN,DEF,DEF,RIT,DEF,DEF,DEF,DEF,DEF,DEF,LFT,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* j */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,LFT,DGN,0.0,RIT,DEF,DWN,DWN,DEF,DEF,DEF,DEF,DEF,DEF,UPP,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* k */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,UPP,LFT,0.0,RIT,DWN,DEF,DGN,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* l */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,LFT,0.0,DEF,DEF,UPP,DGN,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* m */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,UPP,UPP,DEF,0.0,LFT,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* n */  {DEF,LFT,DEF,DEF,DEF,DEF,DEF,UPP,DEF,UPP,DEF,DEF,RIT,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* o */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,LFT,DEF,DGN,DWN,DEF,DEF,0.0,RIT,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* p */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DGN,DEF,DEF,LFT,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* q */  {DWN,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,RIT,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* r */  {DEF,DEF,DEF,DGN,LFT,DWN,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,RIT,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* s */  {LFT,DEF,DEF,RIT,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,UPP,DWN,DEF,DWN,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* t */  {DEF,DEF,DEF,DEF,DEF,DGN,DWN,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,LFT,DEF,0.0,DEF,DEF,DEF,DEF,RIT,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* u */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DGN,RIT,DWN,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,LFT,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* v */  {DEF,RIT,LFT,DEF,DEF,UPP,UPP,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* w */  {DGN,DEF,DEF,DEF,RIT,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,LFT,DEF,DWN,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* x */  {DEF,DEF,RIT,UPP,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,UPP,DEF,DEF,DEF,DEF,0.0,DEF,LFT,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* y */  {DEF,DEF,DEF,DEF,DEF,DEF,DWN,DWN,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,LFT,RIT,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* z */  {UPP,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,UPP,DEF,DEF,DEF,DEF,RIT,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* - */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* 0 */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,LFT},
/* 1 */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,0.0,RIT,DEF,DEF,DEF,DEF,DEF,DEF,DEF},
/* 2 */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,LFT,0.0,RIT,DEF,DEF,DEF,DEF,DEF,DEF},
/* 3 */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,LFT,0.0,RIT,DEF,DEF,DEF,DEF,DEF},
/* 4 */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,LFT,0.0,RIT,DEF,DEF,DEF,DEF},
/* 5 */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,LFT,0.0,RIT,DEF,DEF,DEF},
/* 6 */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,LFT,0.0,RIT,DEF,DEF},
/* 7 */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,LFT,0.0,RIT,DEF},
/* 8 */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,LFT,0.0,RIT},
/* 9 */  {DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,DEF,RIT,DEF,DEF,DEF,DEF,DEF,DEF,DEF,LFT,0.0}
};




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

RebaseWordSpellCheck()
{
   base_cost = new SpellCost();
   trie_root = new SpellNode();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

SpellCost	 getCost()		 { return base_cost; }
void		 setCost(SpellCost c)	 { base_cost = c; }




/********************************************************************************/
/*										*/
/*	Methods to add to trie							*/
/*										*/
/********************************************************************************/

void addWord(String word)
{
   trie_root.findNewNode(word,0);
}





/********************************************************************************/
/*										*/
/*	Methods to check spelling						*/
/*										*/
/********************************************************************************/

/**
 *	Check for the proper spelling of a word.  This routine returns null if the
 *	word is spelled properly.  Otherwise, it returns an eumeration of alternative
 *	spellings.  Note that this enumeration could have 0 elements if no spelling
 *	exists.
 **/


List<SpellWord> checkSpelling(String word)
{
   if (word.length() > 20) {
      return Collections.emptyList();
    }

   return trie_root.checkSpelling(word,base_cost);
}


String findBestSpelling(String word)
{
   List<SpellWord> rslt = checkSpelling(word);
   if (rslt == null || rslt.size() == 0) return null;
   return rslt.get(0).getText();
}



/********************************************************************************/
/*										*/
/*	TrieNode implementation 						*/
/*										*/
/********************************************************************************/

private static class SpellNode {

   private Next next_nodes;
   private String result_word;

   SpellNode() {
      next_nodes = null;
      result_word = null;
    }

   SpellNode findNewNode(String word,int idx) {
      if (idx >= word.length()) {
         result_word = word;
         return this;
       }     
      SpellNode nn;
      int cidx = getIndex(word,idx);
      if (next_nodes == null) {
         nn = new SpellNode();
         next_nodes = new SingleNext(cidx,nn);
       }
      else {
         nn = next_nodes.getNext(cidx);
         if (nn == null) {
            nn = new SpellNode();
            next_nodes = next_nodes.addNext(cidx,nn);
          }
       }
      return nn.findNewNode(word,idx+1);
    }

   private int getIndex(String word,int idx) {
      char c = word.charAt(idx);
      int v = 26;
      if (c >= 'a' && c <= 'z') v = c-'a';
      else if (c >= 'A' && c <= 'Z') v = c-'A';
      else if (c >= '0' && c <= '9') v = 27 + (c-'0');
   
      return v;
    }

   List<SpellWord> checkSpelling(String word,SpellCost cost) {
      int len = word.length();
      List<SpellWord> v = new ArrayList<SpellWord>();
   
      findAlternatives(v,word,0,WORD_BASE_COST,cost,len);
      
      Collections.sort(v);
   
      if (v.size() > 0) {
         SpellWord wd0 = v.get(0);
         if (wd0.getText().equals(word)) return null;
       }
   
      return v;
    }



   private void findAlternatives(List<SpellWord> rslt,String word,int index,double score,
        			 SpellCost cost,int delta) {
      if (delta == 0) {
         if (result_word != null && score <= cost.getMaxCost()) {
            rslt.add(new SpellWord(result_word,score));
          }
         return;
       }   
      
      char c = word.charAt(index);
      double delc = cost.getDeleteCost(word, index);
      double dels = score - (delta-1)*cost.getCorrectCost(word) + delc;
      if (dels < cost.getMaxCost()) {
         findAlternatives(rslt,word,index+1,score+delc,cost,delta-1);
       }
   
      if (next_nodes == null) return;
   
      int cidx = getIndex(word,index);
      for (int i = 0; i < 37; ++i) {
         SpellNode nxt = next_nodes.getNext(i);
         if (nxt != null) {
            char nc = (i == 26 ? '-' : (char) ((i > 26) ? ('0' + (i-27)) : ('a' + i)));
            if (cidx == 26 && i == 26) nc = c;
            double rcst = cost.getReplaceCost(word,index,nc);
            double reps = score + (delta-1)*cost.getCorrectCost(word) + rcst;
            if (reps <= cost.getMaxCost()) {
               nxt.findAlternatives(rslt,word,index+1,score+rcst,cost,delta-1);
             }
            double icst = cost.getInsertCost(word, index, nc);
            double inss = score + (delta)*cost.getCorrectCost(word) + icst;
            if (inss <= cost.getMaxCost()) {
               nxt.findAlternatives(rslt,word,index,score+icst,cost,delta);
             }
          }
       }
    }

}	// end of class SpellNode



/********************************************************************************/
/*										*/
/*	Classes to handle children of a tree node				*/
/*										*/
/********************************************************************************/

static private interface Next {

   public SpellNode getNext(int idx);
   public Next addNext(int idx,SpellNode nxt);

}	// end of inner interface Next


static private class SingleNext implements Next {

   private int node_index;
   private SpellNode next_node;

   SingleNext(int idx,SpellNode nn) {
      node_index = idx;
      next_node = nn;
    }

   @Override public Next addNext(int idx,SpellNode nxt) {
      return new DoubleNext(node_index,next_node,idx,nxt);
    }

   @Override public SpellNode getNext(int idx) {
      if (idx == node_index) return next_node;
      return null;
    }

}	// end of inner class SingleNext



static private class DoubleNext implements Next {

   private int first_index;
   private SpellNode first_node;
   private int second_index;
   private SpellNode second_node;

   DoubleNext(int idx0,SpellNode n0,int idx1,SpellNode n1) {
      first_index = idx0;
      first_node = n0;
      second_index = idx1;
      second_node = n1;
    }

   @Override public Next addNext(int idx,SpellNode nxt) {
      Next nx = new ArrayNext(first_index,first_node);
      nx = nx.addNext(second_index,second_node);
      nx = nx.addNext(idx,nxt);
      return nx;
    }

   @Override public SpellNode getNext(int idx) {
      if (idx == first_index) return first_node;
      else if (idx == second_index) return second_node;
      else return null;
    }

}	//end of subclass DoubleNext



static private class ArrayNext implements Next {

   private SpellNode [] next_nodes;

   ArrayNext(int idx,SpellNode nd) {
      next_nodes = new SpellNode[37];
      next_nodes[idx] = nd;
    }

   @Override public Next addNext(int idx,SpellNode nxt) {
      next_nodes[idx] = nxt;
      return this;
    }

   @Override public SpellNode getNext(int idx) {
      return next_nodes[idx];
    }

}	// end of subclass ArrayNext



/********************************************************************************/
/*										*/
/*	Classes to handle alternative with value				*/
/*										*/
/********************************************************************************/

private static class SpellWord implements Comparable<SpellWord> {

   private String  base_word;
   private double  spell_value;

   SpellWord(String wd,double val) {
      base_word = wd;
      spell_value = val;
    }

   String getText()		    { return base_word; }
   
   @Override public int compareTo(SpellWord w) {
      double d0 = spell_value - w.spell_value;
      if (d0 < 0) return -1;
      if (d0 > 0) return 1;
      return base_word.compareTo(w.base_word);
    }

}	// end of inner class SpellWord




/********************************************************************************/
/*										*/
/*	Cost to compute spelling cost						*/
/*										*/
/********************************************************************************/

private static class SpellCost {

   private double  max_cost;
   private double  insert_cost;
   private double  end_insert_cost;
   private double  double_insert_cost;
   private double  delete_cost;
   private double  end_delete_cost;
   private double  double_delete_cost;
   private double  replace_cost;
   private double  correct_cost;

   SpellCost() {
      max_cost = WORD_MAX_SPELL_ERROR;
      insert_cost = WORD_INSERT_COST;
      end_insert_cost = WORD_END_INSERT_COST;
      double_insert_cost = WORD_DOUBLE_INSERT_COST;

      delete_cost = WORD_DELETE_COST;
      end_delete_cost = WORD_END_DELETE_COST;
      double_delete_cost = WORD_DOUBLE_DELETE_COST;

      replace_cost = WORD_DEFAULT_REPLACE_COST;

      correct_cost = WORD_CORRECT_COST;
    }

   double getMaxCost()			{ return max_cost + WORD_BASE_COST; }

   double getInsertCost(String word, int index, char letter) {
      double ret = insert_cost;
      if (word.charAt(index) == letter) {
	 ret = double_insert_cost;
       }
      else if ((word.length() > WORD_SHORT_WORD_LENGTH) &&
		  ((index == 0) || (index == (word.length() - 1)))) {
	 ret = end_insert_cost;
       }
      return ret;
    }

   double getDeleteCost(String word, int index) {
      double ret = delete_cost;
      if ((word.length() > WORD_SHORT_WORD_LENGTH) &&
	     ((index == 0) || (index == (word.length() - 1)))) {
	 ret = end_delete_cost;
       }
      else if ((index > 0) && (word.charAt(index-1) == word.charAt(index))) {
	 ret = double_delete_cost;
       }
      return ret;
    }

   double getCorrectCost(String word) {
      return correct_cost;
    }


   double getReplaceCost(String word, int index, char to) {
      double ret = replace_cost;
      char frm = word.charAt(index);
      if (frm == to) {
	 ret = correct_cost;
       }
      else {
	 int f_index = normChar(frm);
	 int t_index = normChar(to);
	 if ((f_index == 26) && (to == ' ')) {
	    ret = correct_cost;
	  }
	 else {
	    ret = Math.min(PHON_REPLACE_COST[f_index][t_index],
			      KEYS_REPLACE_COST[f_index][t_index]);
	  }
       }
      return ret;
    }

   private int normChar(char c) {
      int ret = 26;
      if (c >= 'a' && c <= 'z') ret = c-'a';
      else if (c >= 'A' && c <= 'Z') ret = c-'A';
      else if (c >= '0' && c <= '9') c = (char)(27 + (c-'0'));
      return ret;
    }

}	// end of inner class SpellCost





}	// end of class RebaseWordSpellCheck




/* end of SpellCheck.java */

