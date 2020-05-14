/********************************************************************************/
/*										*/
/*		RebaseWordConstants.java					*/
/*										*/
/*	Constants for word processing for rebase search 			*/
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



public interface RebaseWordConstants {


/********************************************************************************/
/*										*/
/*	Files									*/
/*										*/
/********************************************************************************/

String WORD_LIST_FILE = "words";



/********************************************************************************/
/*                                                                              */
/*      Word options                                                            */
/*                                                                              */
/********************************************************************************/

enum WordOptions {
   SPLIT_CAMELCASE,             // split camel case words
   SPLIT_UNDERSCORE,            // split words on underscores
   SPLIT_NUMBER,                // split words on numbers
   SPLIT_COMPOUND,              // split compound words
   STEM,                        // do stemming
   STEM_DICTIONARY,             // check if stem is an actual word
   PLURAL,                      // plural -> singular
   VOWELLESS,                   // add abbreviations from dropping vowels
   SPELLING,                    // add misspelling
}



/********************************************************************************/
/*                                                                              */
/*      Computation options                                                     */
/*                                                                              */
/********************************************************************************/

enum TermOptions {
   TERM_NORMAL,                 // df(wd) = f(wd)
   TERM_BOOLEAN,                // df(wd) = (f(wd) > 0 ? 1 : 0)
   TERM_LOG,                    // df(wd) = log(1+f(wd))
   TERM_AUGMENTED,              // df(wd) = 0.5*f(wd)/MAX(f(wd))
   WORDS_ONLY,                  // only suggest dictionary words
}



}	// end of interface RebaseWordConstants




/* end of RebaseWordConstants.java */
