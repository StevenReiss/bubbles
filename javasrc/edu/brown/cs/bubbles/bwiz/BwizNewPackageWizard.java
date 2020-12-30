/********************************************************************************/
/*                                                                              */
/*              BwizNewPackageWizard.java                                       */
/*                                                                              */
/*      Wizard for creating a package                                           */
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

import java.awt.Point;
import java.io.File;

import java.lang.reflect.Modifier;

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.bueno.BuenoFactory;
import edu.brown.cs.bubbles.bueno.BuenoLocation;
import edu.brown.cs.bubbles.bueno.BuenoProperties;

class BwizNewPackageWizard extends BwizNewWizard
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

BwizNewPackageWizard(BuenoLocation loc)
{
   super(loc,BuenoType.NEW_PACKAGE);
   String s = property_set.getPackageName();
   if (s != null) setInitialName(s + ".");
   property_set.put(BuenoKey.KEY_TYPE,"class");
   
// if (s != null) {
//    int idx = s.lastIndexOf(".");
//    if (idx >= 0) {
//       String fnm = s.substring(0,idx+1);
//       setInitialName(fnm);
//     }
//  }
}


/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override protected int getAccessibilityInfo()
{
   return SHOW_CLASS|SHOW_INTERFACE|SHOW_ABSTRACT|SHOW_FINAL;
}


@Override protected Creator getCreator()
{
   return new PackageCreator();
}


@Override protected String getNameText()
{
   return "Package Name";
}

@Override protected String getNameHoverText()
{
   return "<html>Enter the new full package name." +
      " Such names are generally all lower case.";
}


@Override protected String getSecondText()
{
   return "Initial Class/Interface Name";
}


@Override protected String getSecondHoverText()
{
   return "<html>A new package must contain at least one file." +
      " Enter the name of the initial class or interface." +
      " Such names usually start with an uppercase letter.";
}



@Override protected String getListText()                { return null; }
@Override protected String getListHoverText()           { return null; }



/********************************************************************************/
/*                                                                              */
/*      Update methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public void updateSignature()
{
   property_set.put(BuenoKey.KEY_PACKAGE,property_set.get(BuenoKey.KEY_NAME));
   StringBuffer buf = new StringBuffer();
   int mods = property_set.getModifiers();
   if (Modifier.isPublic(mods)) buf.append("public ");
   if (Modifier.isAbstract(mods)) buf.append("abstract ");
   if (Modifier.isFinal(mods)) buf.append("final ");
   buf.append(property_set.getStringProperty(BuenoKey.KEY_TYPE));
   buf.append(" ");
   String cnm = property_set.getStringProperty(BuenoKey.KEY_CLASS_NAME);
   if (cnm == null || cnm.length() == 0) cnm = "???";
   buf.append(cnm);
   property_set.put(BuenoKey.KEY_SIGNATURE,buf.toString());   
   
   super.updateSignature();
}

/********************************************************************************/
/*                                                                              */
/*      Package creator                                                         */
/*                                                                              */
/********************************************************************************/

private class PackageCreator extends Creator {

   @Override protected BudaBubble doCreate(BudaBubbleArea bba,Point p,String fullname,
         BuenoProperties bp) {
      BuenoFactory bf = BuenoFactory.getFactory();
    
      BuenoType ctyp = BuenoType.NEW_TYPE;
      switch (property_set.getStringProperty(BuenoKey.KEY_TYPE)) {
         default :
            break;
         case "class" :
            ctyp = BuenoType.NEW_CLASS;
            break;
         case "interface" :
            ctyp = BuenoType.NEW_INTERFACE;
            break;
         case "enum" :
            ctyp = BuenoType.NEW_ENUM;
            break;
       }
      String cnm = property_set.getStringProperty(BuenoKey.KEY_CLASS_NAME);
      property_set.put(BuenoKey.KEY_NAME,cnm);
      
      // create the package directory
      bf.createNew(BuenoType.NEW_PACKAGE,at_location,property_set);
      
      // create the initial type
      bf.createNew(ctyp,at_location,property_set);
      
      String proj = at_location.getProject();
      String pkg = property_set.getStringProperty(BuenoKey.KEY_PACKAGE); 
      File f = at_location.getInsertionFile();
      if (f == null) return null;
      String cls = pkg + "." + property_set.getStringProperty(BuenoKey.KEY_CLASS_NAME);
      
      BudaBubble nbbl = null;
      if (bubble_creator != null) {
         bubble_creator.createBubble(proj,cls,bba,p);
       } 
      else {
         nbbl = BaleFactory.getFactory().createFileBubble(proj,null,cls);
       }
      
      return nbbl;
    }
}




}       // end of class BwizNewPackageWizard




/* end of BwizNewPackageWizard.java */

