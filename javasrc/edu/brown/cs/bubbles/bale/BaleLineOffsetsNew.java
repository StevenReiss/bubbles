/********************************************************************************/
/*										*/
/*		BaleLineOffsets.java						*/
/*										*/
/*	Bubble Annotated Language Editor ide-java line offset management	*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bale;

import edu.brown.cs.bubbles.board.BoardLog;

import javax.swing.text.Segment;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;


class BaleLineOffsetsNew implements BaleConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private int [] java_offset;
private int max_java;
private int newline_adjust;
private int [] ide_offset;
private int max_ide;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleLineOffsetsNew(String newline,Segment src,Reader input)
{
   newline_adjust = newline.length() - 1;
   java_offset = new int[128];
   ide_offset = new int[128];
   java_offset[0] = -1; 			     // dummy first element
   ide_offset[0] = -1;
   max_java = 1;
   max_ide = 1;

   setupSource(src);
   setupIde(input,newline);

   if (max_ide != max_java)
      throw new IllegalArgumentException("Files don't match " + max_ide + " " + max_java);
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

private void setupSource(Segment sg)
{
   addJava(0);
   int ln = sg.length();
   for (int i = 0; i < ln; ++i) {
      if (sg.charAt(i) == '\n') addJava(i+1);
    }
}



private void addJava(int i)
{
   grow(max_java+1);
   java_offset[max_java++] = i;
}



private void setupIde(Reader r,String nl)
{
   addIde(0);

   boolean lastcr = false;
   try {
      for (int i = 0; ; ++i) {
	 int ch = r.read();
	 if (ch < 0) break;
	 if (nl.equals("\r")) {
	    if (ch == '\r') addIde(i+1);
	  }
	 else {
	    if (ch == '\n') addIde(i+1);
	    else if (lastcr)
	       addIde(i);
	    lastcr = (ch == '\r');
	  }
       }
      r.close();
    }
   catch (IOException e) {
      BoardLog.logE("BALE","Problem reading input file: " + e);
    }
}



private void addIde(int i)
{
   grow(max_ide+1);
   ide_offset[max_ide++] = i;
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

   int xchar = ide_offset[idx1] - ide_offset[idx0] - (java_offset[idx1] - java_offset[idx0]);
   int idelta = delta - xchar + ct*newline_adjust;

   grow(max_java + ct - oct);	     // ensure we fit
   if (ct > oct) {
      for (int i = max_java-1; i > idx1; --i) {
	 java_offset[i+ct-oct] = java_offset[i] + delta;
	 ide_offset[i+ct-oct] = ide_offset[i] + idelta;
       }
    }
   else if (ct < oct || delta != 0) {
      for (int i = idx1+1; i < max_java; ++i) {
	 java_offset[i+ct-oct] = java_offset[i] + delta;
	 ide_offset[i+ct-oct] = ide_offset[i] + idelta;
       }
    }

   max_java += ct-oct;
   max_ide += ct-oct;

   int ioff = soff - java_offset[idx0] + ide_offset[idx0];

   int idx2 = idx0+1;
   if (cnts != null) {
      int lct = newline_adjust;
      for (int idx = cnts.indexOf('\n'); idx >= 0; idx = cnts.indexOf('\n',idx+1)) {
	 java_offset[idx2] = soff + idx + 1;
	 ide_offset[idx2] = ioff + idx + 1 + lct;
	 ++idx2;
	 lct += newline_adjust;
       }
    }
}



/********************************************************************************/
/*										*/
/*	Methods to map between IDE and Java offsets				*/
/*										*/
/********************************************************************************/

synchronized int findEclipseOffset(int off)
{
   int sidx = Arrays.binarySearch(java_offset,0,max_java,off);
   if (sidx < 0) sidx = -sidx - 2;
   if (sidx == 0) return off;

   int v0 = off - java_offset[sidx] + ide_offset[sidx];

   return v0;
}



synchronized int findJavaOffset(int off)
{
   int sidx = Arrays.binarySearch(ide_offset,0,max_ide,off);
   if (sidx < 0) sidx = -sidx - 2;

   if (sidx < 0) sidx = 0;

   int v0 = (sidx == 0 ? off : off - ide_offset[sidx] + java_offset[sidx]);

   return v0;
}



/********************************************************************************/
/*										*/
/*	Methods to find lines and line offsets for Java 			*/
/*										*/
/********************************************************************************/

synchronized int findOffset(int line)
{
   if (line < 0 || line >= max_java) return -1;
   return java_offset[line];
}



synchronized int findLine(int off)
{
   int sidx = Arrays.binarySearch(java_offset,0,max_java,off);
   if (sidx < 0) sidx = -sidx - 2;
   return sidx;
}



/********************************************************************************/
/*										*/
/*	Helper methods for maintaining the offset arrays			*/
/*										*/
/********************************************************************************/

private int findIndex(int off)
{
   int sidx = Arrays.binarySearch(java_offset,0,max_java,off);
   if (sidx > 0) return sidx;
   sidx = -sidx - 2;
   if (sidx < 0) sidx = 0;
   return sidx;
}




private void grow(int max)
{
   int sz = java_offset.length;
   if (sz > max) return;
   while (sz < max) sz *= 2;
   java_offset = Arrays.copyOf(java_offset,sz);
   ide_offset = Arrays.copyOf(ide_offset,sz);
}



}	// end of class BaleLineOffsets




/* end of BaleLineOffsets.java */
