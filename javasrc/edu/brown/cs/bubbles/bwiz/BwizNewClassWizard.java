/********************************************************************************/
/*										*/
/*		BwizNewClassWizard.java 					*/
/*										*/
/*	Wizard to create a new class						*/
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



package edu.brown.cs.bubbles.bwiz;

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.bueno.BuenoFactory;
import edu.brown.cs.bubbles.bueno.BuenoLocation;
import edu.brown.cs.bubbles.bueno.BuenoProperties;

import java.awt.Point;


class BwizNewClassWizard extends BwizNewWizard
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BwizNewClassWizard(BuenoLocation loc)
{
   super(loc,BuenoType.NEW_CLASS);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override protected int getAccessibilityInfo()
{
   return SHOW_ABSTRACT|SHOW_FINAL;
}

@Override protected Creator getCreator()	{ return new ClassCreator(); }

@Override protected String getNameText()	{ return "Class Name"; }
@Override protected String getNameHoverText()
{
   return "<html>Enter the new class name." +
    " Such names usually start with an uppercase letter";
}

@Override protected String getSecondText()	{ return "Extends"; }
@Override protected String getSecondHoverText()
{
   return "This class's parent (super) class";
}

@Override protected String getListText()	{ return "Implements: "; }
@Override protected String getListHoverText()
{
   return "Any interfaces implemented by this class. Press ENTER " +
   "after you type the interface name to add an interface.";
}



/********************************************************************************/
/*										*/
/*	Class Creator								*/
/*										*/
/********************************************************************************/

private class ClassCreator extends Creator {

   @Override protected BudaBubble doCreate(BudaBubbleArea bba,Point pt,String fullname,BuenoProperties bp) {
      BudaBubble nbbl = null;
      BuenoLocation bl = at_location;
      BuenoFactory bf = BuenoFactory.getFactory();
      String proj = property_set.getProjectName();
      String pkg = property_set.getPackageName();
   
      if (bl == null) bl = bf.createLocation(proj,pkg,null,true);
      bf.createNew(BuenoType.NEW_CLASS,bl,bp);
      if (bubble_creator == null)
         nbbl = BaleFactory.getFactory().createFileBubble(proj,null,fullname);
      else
         bubble_creator.createBubble(proj,fullname,bba,pt);
      return nbbl;
    }

}	// end of inner class ClassCreator



}	// end of class BwizNewClassWizard




/* end of BwizNewClassWizard.java */

