/********************************************************************************/
/*										*/
/*		BconRegionBueno.java						*/
/*										*/
/*	Bubbles Environment Context Viewer bueno location for bcon region	*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.bcon;

import edu.brown.cs.bubbles.bale.BaleConstants.BaleFileOverview;
import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.bueno.BuenoConstants;
import edu.brown.cs.bubbles.bueno.BuenoLocation;

import javax.swing.text.BadLocationException;
import javax.swing.text.Segment;

import java.io.File;



class BconRegionBueno extends BuenoLocation implements BconConstants, BuenoConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		for_project;
private String		for_package;
private String		for_class;
private File		for_file;

private boolean 	is_after;
private BconRegion	for_region;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BconRegionBueno(String proj,String cnm,File f,BconRegion br,boolean after)
{
   for_project = proj;
   int idx = cnm.lastIndexOf(".");
   for_package = cnm.substring(0,idx);
   for_class = cnm;
   for_file = f;

   for_region = br;
   is_after = after;
}





/********************************************************************************/
/*										*/
/*	Access Methods								*/
/*										*/
/********************************************************************************/

@Override public String getProject()			{ return for_project; }

@Override public String getPackage()			{ return for_package; }

@Override public String getClassName()			{ return for_class; }

@Override public File getFile() 			{ return for_file; }

@Override public int getOffset()
{
   BaleFileOverview bov = BaleFactory.getFactory().getFileOverview(for_project,for_file);
   Segment s = new Segment();
   try {
      bov.getText(0,bov.getLength(),s);
    }
   catch (BadLocationException e) {
      return -1;
    }
   
   int idx0 = -1;
   
   if (is_after) {
      int idx = for_region.getEndOffset();
      idx0 = idx;
      while (idx < s.length() && Character.isWhitespace(s.charAt(idx))) {
         if (s.charAt(idx) == '\n') idx0 = idx;
         ++idx;
       }
    }
   else {
      int idx = for_region.getStartOffset();
      idx0 = idx;
      while (idx >= 0 && Character.isWhitespace(s.charAt(idx))) {
         if (s.charAt(idx) == '\n') {
            idx0 = idx;
            break;
          }
         --idx;
       }
    }
      
   return idx0;   
   // if (is_after) return for_region.getEndOffset();
   // else return for_region.getStartOffset();
}



}	// end of class BconRegionBueno



/* end of BconRegionBueno.java */
