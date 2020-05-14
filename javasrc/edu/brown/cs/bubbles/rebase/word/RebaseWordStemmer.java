/********************************************************************************/
/*										*/
/*		RebaseWordStemmer.java						*/
/*										*/
/*	Class to do word stemming						*/
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
/*
 File: Stemmer.java

 Copyright 2010 - The Cytoscape Consortium (www.cytoscape.org)

 Code written by: Layla Oesper
 Authors: Layla Oesper, Ruth Isserlin, Daniele Merico

 This library is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this project.  If not, see <http://www.gnu.org/licenses/>.
 */


/*
Porter stemmer in Java. The original paper is in

    Porter, 1980, An algorithm for suffix stripping, Program, Vol. 14,
    no. 3, pp 130-137,

See also http://www.tartarus.org/~martin/PorterStemmer

History:

Release 1

Bug 1 (reported by Gonzalo Parra 16/10/99) fixed as marked below.
The words 'aed', 'eed', 'oed' leave k at 'a' for step 3, and b[k-1]
is then out outside the bounds of b.

Release 2

Similarly,

Bug 2 (reported by Steve Dyrdahl 22/2/00) fixed as marked below.
'ion' by itself leaves j = -1 in the test for 'ion' in step 5, and
b[j] is then outside the bounds of b.

Release 3

Considerably revised 4/9/00 in the light of many helpful suggestions
from Brian Goetz of Quiotix Corporation (brian@quiotix.com).

Release 4

*/


package edu.brown.cs.bubbles.rebase.word;



/**
* Stemmer, implementing the Porter Stemming Algorithm
*
* The Stemmer class transforms a word into its root form.  The input
* word can be provided a character at time (by calling add()), or at once
* by calling one of the various stem(something) methods.
*/

class RebaseWordStemmer {

   
/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/



private char[]	stem_buffer;
private int	stem_index; /* offset into b */
private int     index_end; /* offset to end of stemmed word */
private int     stem_pos1;
private int     stem_pos2;

private static final int INC = 50;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

RebaseWordStemmer()
{
   stem_buffer = new char[INC];
   stem_index = 0;
   index_end = 0;
}



/**
 * Add a character to the word being stemmed.  When you are finished
 * adding characters, you can call stem(void) to stem the word.
 */

void add(char ch)
{
   ch = Character.toLowerCase(ch);
   if (stem_index == stem_buffer.length) {
      char[] new_b = new char[stem_index + INC];
      for (int c = 0; c < stem_index; c++)
	 new_b[c] = stem_buffer[c];
      stem_buffer = new_b;
    }
   stem_buffer[stem_index++] = ch;
}


/** Adds wLen characters to the word being stemmed contained in a portion
 * of a char[] array. This is like repeated calls of add(char ch), but
 * faster.
 */

void add(char[] w,int wLen)
{
   if (stem_index + wLen >= stem_buffer.length) {
      char[] new_b = new char[stem_index + wLen + INC];
      for (int c = 0; c < stem_index; c++)
	 new_b[c] = stem_buffer[c];
      stem_buffer = new_b;
    }
   
   for (int c = 0; c < wLen; c++) {
      Character ch = w[c];
      ch = Character.toLowerCase(ch);
      stem_buffer[stem_index++] = ch;
    }
}


void add(String s)
{
   char [] chs = s.toCharArray();
   add(chs,chs.length);
}



/**
 * After a word has been stemmed, it can be retrieved by toString(),
 * or a reference to the internal buffer can be retrieved by getResultBuffer
 * and getResultLength (which is generally more efficient.)
 */
@Override public String toString()
{
   return new String(stem_buffer,0,index_end);
}

/**
 * Returns the length of the word resulting from the stemming process.
 */
int getResultLength()
{
   return index_end;
}

/**
 * Returns a reference to a character buffer containing the results of
 * the stemming process.  You also need to consult getResultLength()
 * to determine the length of the result.
 */
char[] getResultBuffer()
{
   return stem_buffer;
}


/********************************************************************************/
/*                                                                              */
/*      Utility methods                                                        */
/*                                                                              */
/********************************************************************************/


/* cons(i) is true <=> b[i] is a consonant. */

private final boolean cons(int i)
{
   switch (stem_buffer[i]) {
      case 'a':
      case 'e':
      case 'i':
      case 'o':
      case 'u':
	 return false;
      case 'y':
	 return (i == 0) ? true : !cons(i - 1);
      default:
	 return true;
   }
}

/* m() measures the number of consonant sequences between 0 and j. if c is
   a consonant sequence and v a vowel sequence, and <..> indicates arbitrary
   presence,

      <c><v>	   gives 0
      <c>vc<v>	   gives 1
      <c>vcvc<v>   gives 2
      <c>vcvcvc<v> gives 3
      ....
*/

private final int m()
{
   int n = 0;
   int i = 0;
   while (true) {
      if (i > stem_pos1) return n;
      if (!cons(i)) break;
      i++;
    }
   i++;
   while (true) {
      while (true) {
	 if (i > stem_pos1) return n;
	 if (cons(i)) break;
	 i++;
       }
      i++;
      n++;
      while (true) {
	 if (i > stem_pos1) return n;
	 if (!cons(i)) break;
	 i++;
       }
      i++;
    }
}



/* vowelinstem() is true <=> 0,...j contains a vowel */

private final boolean vowelinstem()
{
   int i;
   for (i = 0; i <= stem_pos1; i++)
      if (!cons(i)) return true;
   return false;
}



/* doublec(j) is true <=> j,(j-1) contain a double consonant. */

private final boolean doublec(int j)
{
   if (j < 1) return false;
   if (stem_buffer[j] != stem_buffer[j - 1]) return false;
   return cons(j);
}



/* cvc(i) is true <=> i-2,i-1,i has the form consonant - vowel - consonant
   and also if the second c is not w,x or y. this is used when trying to
   restore an e at the end of a short word. e.g.

      cav(e), lov(e), hop(e), crim(e), but
      snow, box, tray.

*/

private final boolean cvc(int i)
{
   if (i < 2 || !cons(i) || cons(i - 1) || !cons(i - 2)) return false;
   
   int ch = stem_buffer[i];
   if (ch == 'w' || ch == 'x' || ch == 'y') return false;
   
   return true;
}


private final boolean ends(String s)
{
   int l = s.length();
   int o = stem_pos2 - l + 1;
   if (o < 0) return false;
   for (int i = 0; i < l; i++) {
      if (stem_buffer[o + i] != s.charAt(i)) return false;
    }
   stem_pos1 = stem_pos2 - l;
   return true;
}

/* setto(s) sets (j+1),...k to the characters in the string s, readjusting
   k. */

private final void setto(String s)
{
   int l = s.length();
   int o = stem_pos1 + 1;
   for (int i = 0; i < l; i++)
      stem_buffer[o + i] = s.charAt(i);
   stem_pos2 = stem_pos1 + l;
}

/* r(s) is used further down. */

private final void r(String s)
{
   if (m() > 0) setto(s);
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/


/* step1() gets rid of plurals and -ed or -ing. e.g.

       caresses  ->  caress
       ponies	 ->  poni
       ties	 ->  ti
       caress	 ->  caress
       cats	 ->  cat

       feed	 ->  feed
       agreed	 ->  agree
       disabled  ->  disable

       matting	 ->  mat
       mating	 ->  mate
       meeting	 ->  meet
       milling	 ->  mill
       messing	 ->  mess

       meetings  ->  meet

*/

private final void step1()
{
   if (stem_buffer[stem_pos2] == 's') {
      if (ends("sses")) stem_pos2 -= 2;
      else if (ends("ies")) setto("i");
      else if (stem_buffer[stem_pos2 - 1] != 's') stem_pos2--;
    }
   if (ends("eed")) {
      if (m() > 0) stem_pos2--;
    }
   else if ((ends("ed") || ends("ing")) && vowelinstem()) {
      stem_pos2 = stem_pos1;
      if (ends("at")) setto("ate");
      else if (ends("bl")) setto("ble");
      else if (ends("iz")) setto("ize");
      else if (doublec(stem_pos2)) {
	 stem_pos2--;
	 {
	    int ch = stem_buffer[stem_pos2];
	    if (ch == 'l' || ch == 's' || ch == 'z') stem_pos2++;
          }
       }
      else if (m() == 1 && cvc(stem_pos2)) setto("e");
    }
}


/* step2() turns terminal y to i when there is another vowel in the stem. */

private final void step2()
{
   if (ends("y") && vowelinstem()) stem_buffer[stem_pos2] = 'i';
}

/* step3() maps double suffices to single ones. so -ization ( = -ize plus
   -ation) maps to -ize etc. note that the string before the suffix must give
   m() > 0. */

private final void step3()
{
   if (stem_pos2 == 0) return; /* For Bug 1 */
   switch (stem_buffer[stem_pos2 - 1]) {
      case 'a':
	 if (ends("ational")) {
	    r("ate");
	    break;
          }
	 if (ends("tional")) {
	    r("tion");
	    break;
          }
	 break;
      case 'c':
	 if (ends("enci")) {
	    r("ence");
	    break;
          }
	 if (ends("anci")) {
	    r("ance");
	    break;
          }
	 break;
      case 'e':
	 if (ends("izer")) {
	    r("ize");
	    break;
          }
	 break;
      case 'l':
	 if (ends("bli")) {
	    r("ble");
	    break;
          }
	 if (ends("alli")) {
	    r("al");
	    break;
          }
	 if (ends("entli")) {
	    r("ent");
	    break;
          }
	 if (ends("eli")) {
	    r("e");
	    break;
          }
	 if (ends("ousli")) {
	    r("ous");
	    break;
          }
	 break;
      case 'o':
	 if (ends("ization")) {
	    r("ize");
	    break;
          }
	 if (ends("ation")) {
	    r("ate");
	    break;
          }
	 if (ends("ator")) {
	    r("ate");
	    break;
          }
	 break;
      case 's':
	 if (ends("alism")) {
	    r("al");
	    break;
          }
	 if (ends("iveness")) {
	    r("ive");
	    break;
          }
	 if (ends("fulness")) {
	    r("ful");
	    break;
          }
	 if (ends("ousness")) {
	    r("ous");
	    break;
          }
	 break;
      case 't':
	 if (ends("aliti")) {
	    r("al");
	    break;
          }
	 if (ends("iviti")) {
	    r("ive");
	    break;
          }
	 if (ends("biliti")) {
	    r("ble");
	    break;
          }
	 break;
      case 'g':
	 if (ends("logi")) {
	    r("log");
	    break;
          }
    }
}

/* step4() deals with -ic-, -full, -ness etc. similar strategy to step3. */

private final void step4()
{
   switch (stem_buffer[stem_pos2]) {
      case 'e':
	 if (ends("icate")) {
	    r("ic");
	    break;
          }
	 if (ends("ative")) {
	    r("");
	    break;
          }
	 if (ends("alize")) {
	    r("al");
	    break;
          }
	 break;
      case 'i':
	 if (ends("iciti")) {
	    r("ic");
	    break;
          }
	 break;
      case 'l':
	 if (ends("ical")) {
	    r("ic");
	    break;
          }
	 if (ends("ful")) {
	    r("");
	    break;
          }
	 break;
      case 's':
	 if (ends("ness")) {
	    r("");
	    break;
          }
	 break;
    }
}

/* step5() takes off -ant, -ence etc., in context <c>vcvc<v>. */

private final void step5()
{
   if (stem_pos2 == 0) return; /* for Bug 1 */
   switch (stem_buffer[stem_pos2 - 1]) {
      case 'a':
	 if (ends("al")) break;
	 return;
      case 'c':
	 if (ends("ance")) break;
	 if (ends("ence")) break;
	 return;
      case 'e':
	 if (ends("er")) break;
	 return;
      case 'i':
	 if (ends("ic")) break;
	 return;
      case 'l':
	 if (ends("able")) break;
	 if (ends("ible")) break;
	 return;
      case 'n':
	 if (ends("ant")) break;
	 if (ends("ement")) break;
	 if (ends("ment")) break;
	 /* element etc. not stripped before the m */
	 if (ends("ent")) break;
	 return;
      case 'o':
	 if (ends("ion") && stem_pos1 >= 0 && (stem_buffer[stem_pos1] == 's' || stem_buffer[stem_pos1] == 't')) break;
	 /* j >= 0 fixes Bug 2 */
	 if (ends("ou")) break;
	 return;
	 /* takes care of -ous */
      case 's':
	 if (ends("ism")) break;
	 return;
      case 't':
	 if (ends("ate")) break;
	 if (ends("iti")) break;
	 return;
      case 'u':
	 if (ends("ous")) break;
	 return;
      case 'v':
	 if (ends("ive")) break;
	 return;
      case 'z':
	 if (ends("ize")) break;
	 return;
      default:
	 return;
    }
   if (m() > 1) stem_pos2 = stem_pos1;
}

/* step6() removes a final -e if m() > 1. */

private final void step6()
{
   stem_pos1 = stem_pos2;
   if (stem_buffer[stem_pos2] == 'e') {
      int a = m();
      if (a > 1 || a == 1 && !cvc(stem_pos2 - 1)) stem_pos2--;
    }
   if (stem_buffer[stem_pos2] == 'l' && doublec(stem_pos2) && m() > 1) stem_pos2--;
}



/********************************************************************************/
/*                                                                              */
/*      Top level methods                                                       */
/*                                                                              */
/********************************************************************************/

/** Stem the word placed into the Stemmer buffer through calls to add().
 * Returns true if the stemming process resulted in a word different
 * from the input.  You can retrieve the result with
 * getResultLength()/getResultBuffer() or toString().
 */

String stem()
{
   stem_pos2 = stem_index - 1;
   if (stem_pos2 > 1) {
      step1();
      step2();
      step3();
      step4();
      step5();
      step6();
    }
   index_end = stem_pos2 + 1;
   stem_index = 0;
   
   return new String(stem_buffer,0,index_end);
}



String stem(String s)
{
   stem_index = 0;
   add(s);
   return stem();
}



}       // end of class RebaseWordStemmer




/* end of RebaseWordStemmer.java */
