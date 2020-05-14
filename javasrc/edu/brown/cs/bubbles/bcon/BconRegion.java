/********************************************************************************/
/*										*/
/*		BconRegion.java 						*/
/*										*/
/*	Bubbles Environment Context Viewer abstract region			*/
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

import edu.brown.cs.bubbles.bale.BaleConstants;
import edu.brown.cs.bubbles.buda.BudaBubble;

import javax.swing.text.BadLocationException;
import javax.swing.text.Position;

import java.awt.Component;



abstract class BconRegion implements BconConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

protected BaleConstants.BaleFileOverview for_file;

private boolean 	has_focus;
private boolean 	has_bubble;
private Position	start_position;
private Position	end_position;

protected boolean	is_valid;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected BconRegion(BaleConstants.BaleFileOverview fov)
{
   for_file = fov;
   has_focus = false;
   has_bubble = false;
   start_position = null;
   end_position = null;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

boolean hasFocus()			{ return has_focus; }
void setHasFocus(boolean fg)		{ has_focus = fg; }


boolean hasBubble()			{ return has_bubble; }
void setHasBubble(boolean fg)
{
   has_bubble = fg;
   if (!fg) has_focus = false;
}


int getStartLine()
{
   return for_file.findLineNumber(start_position.getOffset());
}


int getEndLine()
{
   return for_file.findLineNumber(end_position.getOffset());
}


int getStartOffset()			{ return start_position.getOffset(); }
int getEndOffset()			{ return end_position.getOffset(); }




protected void setPosition(int start,int end)
{
   try {
      start_position = for_file.createPosition(start);
      start_position = for_file.savePosition(start_position);
      end_position = for_file.createPosition(end);
      end_position = for_file.savePosition(end_position);
    }
   catch (BadLocationException e) {
      start_position = null;
      end_position = null;
    }
}


boolean isValid()
{
   return start_position != null && end_position != null;
}
		


/********************************************************************************/
/*										*/
/*	Abstract methods							*/
/*										*/
/********************************************************************************/

abstract RegionType getRegionType();
abstract String getRegionName();

String getShortRegionName()			{ return getRegionName(); }

boolean nameMatch(String name)			{ return false; }
boolean createBubble(Component src)		{ return false; }
BudaBubble makeBubble() 			{ return null; }
int getModifiers()				{ return BCON_MODIFIERS_UNDEFINED; }
boolean isComment()				{ return false; }



/********************************************************************************/
/*										*/
/*	Text methods								*/
/*										*/
/********************************************************************************/

String getRegionText()
{
   try {
      int soff = start_position.getOffset();
      int eoff = end_position.getOffset();

      return for_file.getText(soff,eoff-soff);
    }
   catch (BadLocationException e) {
      return null;
    }
}



boolean insertBefore(String txt)
{
   try {
      int soff = start_position.getOffset();

      for_file.insertString(soff,txt,null);
    }
   catch (BadLocationException e) {
      return false;
    }

   return true;
}



boolean insertAfter(String txt)
{
   try {
      int soff = end_position.getOffset();

      for_file.insertString(soff,txt,null);
    }
   catch (BadLocationException e) {
      return false;
    }

   return true;
}



boolean remove()
{
   try {
      int soff = start_position.getOffset();
      int eoff = end_position.getOffset();

      for_file.remove(soff,eoff-soff);
   }
   catch (BadLocationException e) {
      return false;
   }

   return true;
}


}	// end of abstract class BconRegion




/* end of BconRegion.java */
