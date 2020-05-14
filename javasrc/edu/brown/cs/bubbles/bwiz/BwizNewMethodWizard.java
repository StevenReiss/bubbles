/********************************************************************************/
/*                                                                              */
/*              BwizNewMethodWizard.java                                        */
/*                                                                              */
/*      Wizard to create a new method                                           */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
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

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bwiz;

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.bueno.BuenoFactory;
import edu.brown.cs.bubbles.bueno.BuenoLocation;
import edu.brown.cs.bubbles.bueno.BuenoProperties;

import java.awt.Point;


class BwizNewMethodWizard extends BwizNewWizard
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BwizNewMethodWizard(BuenoLocation loc)
{
   super(loc,BuenoType.NEW_METHOD);
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override protected int getAccessibilityInfo()
{
   return SHOW_PRIVATE|SHOW_PROTECTED|SHOW_ABSTRACT|SHOW_FINAL;
}

@Override protected IVerifier getVerifier()
{
   return new ParameterVerifier();
}

@Override protected Creator getCreator()        { return new MethodCreator(); }

@Override protected String getNameText()        { return "Method Name"; }
@Override protected String getNameHoverText() 
{
   return "<html>Enter the new method name.";
}

@Override protected String getSecondText()      { return "Return Type"; }
@Override protected String getSecondHoverText() 
{
   return "This method's return type";
}

@Override protected String getListText()        { return "Parameters: "; }
@Override protected String getListHoverText()
{
   return "Any method parameters. Press enter to add a parameter.";
}


/********************************************************************************/
/*                                                                              */
/*      Method Creator                                                          *//*                                                                              */
/********************************************************************************/

private class MethodCreator extends Creator {
   
   @Override protected BudaBubble doCreate(BudaBubbleArea bba,Point pt,String fullname,BuenoProperties bp) {
      BudaBubble nbbl = null;
      BuenoLocation bl = at_location;
      BuenoFactory bf = BuenoFactory.getFactory();
      String proj = property_set.getProjectName();
      String pkg = property_set.getPackageName();
      
      String cls = new_validator.getClassName();
      String fmthd = new_validator.getMethodName();
      
      if (bl == null) bl = bf.createLocation(proj,pkg,cls,true);
      bf.createNew(BuenoType.NEW_METHOD,bl,bp);
      if (bubble_creator == null)
         nbbl = BaleFactory.getFactory().createMethodBubble(proj,fmthd);
      else
         bubble_creator.createBubble(proj,fmthd,bba,pt);
      
      return nbbl;
    }
   
}       // end of inner class EnumCreator 



}       // end of class BwizNewMethodWizard




/* end of BwizNewMethodWizard.java */

