/********************************************************************************/
/*                                                                              */
/*              BstyleLineOffsets.java                                          */
/*                                                                              */
/*      Handle mapping line/column to position                                  */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Provlocalnce, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.bubbles.bstyle;

import edu.brown.cs.ivy.file.IvyLog;

import com.puppycrawl.tools.checkstyle.api.FileText;

import java.util.Arrays;

class BstyleLineOffsets implements BstyleConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private int []  local_offset;
private int max_local;
private int max_char;
private int newline_adjust;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BstyleLineOffsets(String newline,FileText ft)
{
   newline_adjust = newline.length() - 1;
   local_offset = new int[128];
   local_offset[0] = -1;
   max_local = 1;
   max_char = 0;
   
   setupIde(ft.getFullText(),newline);
}




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

private void setupIde(CharSequence cnts,String nl)
{
   addIde(0);
   
   boolean lastcr = false;
   int i = 0;
   for ( ; ; ++i) {
      int ch = cnts.charAt(i);
      if (ch < 0) break;
      if (nl.equals("\r")) {
         if (ch == '\r') addIde(i+1);
       }
      else {
         if (ch == '\n') addIde(i+1);
         else if (lastcr) addIde(i);
         lastcr = (ch == '\r');
       }
    }
   addIde(i);
   max_char = i;
}



private void addIde(int i)
{
   grow(max_local+1);
   local_offset[max_local++] = i;
}




/********************************************************************************/
/*										*/
/*	Update methods after edits						*/
/*										*/
/********************************************************************************/

synchronized void update(int soff,int eoff,String cnts)
{
   if (cnts == null && soff == eoff) return;
   
   int ct = 0;
   if (cnts != null) {
      for (int idx = cnts.indexOf('\n'); idx >= 0; idx = cnts.indexOf('\n',idx+1)) ++ct;
    }
   
   int idx0 = findIndex(soff);
   int idx1 = findIndex(eoff);
   int oct = idx1-idx0;
   int delta = 0;
   if (cnts != null) delta = cnts.length() - (eoff-soff);
   else delta = soff - eoff;
   
   IvyLog.logD("BSTYLE","UPDATE LINE OFFSETS " + soff + " " + eoff + " " +
         ct + " " + delta + " " + oct);
   
   grow(max_local + ct - oct);	     // ensure we fit
   if (ct > oct) {
      for (int i = max_local-1; i > idx1; --i) {
	 local_offset[i+ct-oct] = local_offset[i] + delta;
       }
    }
   else if (ct < oct || delta != 0) {
      for (int i = idx1+1; i < max_local; ++i) {
	 local_offset[i+ct-oct] = local_offset[i] + delta;
       }
    }
   
   max_local += ct-oct;
   
   int idx2 = idx0+1;
   if (cnts != null) {
      int lct = newline_adjust;
      for (int idx = cnts.indexOf('\n'); idx >= 0; idx = cnts.indexOf('\n',idx+1)) {
	 local_offset[idx2] = soff + idx + 1 + lct;
	 ++idx2;
       }
    }
}


/********************************************************************************/
/*										*/
/*	Methods to find lines and line offsets for Java 			*/
/*										*/
/********************************************************************************/

synchronized int findOffset(int line)
{
   if (line < 0) return 0;
   if (line >= max_local) return max_char;
   return local_offset[line];
}



synchronized int findLine(int off)
{
   int sidx = Arrays.binarySearch(local_offset,0,max_local,off);
   if (sidx < 0) {
      sidx = -sidx - 2;
    }
   if (sidx < 0) {
      if (off < 0) return 0;
      if (off > local_offset[max_local-1]) return max_local;
    }
   return sidx;
}



/********************************************************************************/
/*										*/
/*	Helper methods for maintaining the offset arrays			*/
/*										*/
/********************************************************************************/

private int findIndex(int off)
{
   int sidx = Arrays.binarySearch(local_offset,0,max_local,off);
   if (sidx > 0) return sidx;
   sidx = -sidx - 2;
   if (sidx < 0) sidx = 0;
   return sidx; 
}




private void grow(int max)
{
   int sz = local_offset.length;
   if (sz > max) return;
   while (sz < max) sz *= 2;
   local_offset = Arrays.copyOf(local_offset,sz);
}



}       // end of class BstyleLineOffsets




/* end of BstyleLineOffsets.java */

