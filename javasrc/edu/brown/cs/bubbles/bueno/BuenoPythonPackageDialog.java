/********************************************************************************/
/*                                                                              */
/*              BuenoPythonPackageDialog.java                                   */
/*                                                                              */
/*      Dialog for creating a python package                                    */
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



package edu.brown.cs.bubbles.bueno;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;

import edu.brown.cs.ivy.swing.SwingGridPanel;

import java.awt.Point;
import java.io.File;



public class BuenoPythonPackageDialog extends BuenoAbstractDialog implements BuenoConstants
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

public BuenoPythonPackageDialog(BudaBubble source,Point locale,
      BuenoProperties known,BuenoLocation insert,
      BuenoBubbleCreator newer)
{
   super(source,locale,known,insert,newer,BuenoType.NEW_PACKAGE);
}



/********************************************************************************/
/*                                                                              */
/*      Package dialog panel setup                                              */
/*                                                                              */
/********************************************************************************/

@Override protected void setupPanel(SwingGridPanel pnl)
{
   String pkg = property_set.getStringProperty(BuenoKey.KEY_PACKAGE);
   if (pkg == null) {
      pkg = insertion_point.getPackage();
      if (pkg != null) {
	 int idx = pkg.lastIndexOf(".");
	 if (idx >= 0) property_set.put(BuenoKey.KEY_PACKAGE,pkg.substring(0,idx+1));
       }
    }
   
   StringField sfld = new StringField(BuenoKey.KEY_PACKAGE);
   pnl.addRawComponent("Package Name",sfld);
   sfld.addActionListener(this);
   
   BooleanField bfld = new BooleanField(BuenoKey.KEY_CREATE_INIT);
   pnl.addRawComponent("Create __init__ module",bfld);
   
   sfld = new StringField(BuenoKey.KEY_NAME);
   pnl.addRawComponent("Initial Module",sfld);
   sfld.addActionListener(this);
}



/********************************************************************************/
/*										*/
/*	Creation methods							*/
/*										*/
/********************************************************************************/

@Override protected void doCreate(BudaBubbleArea bba,Point p)
{
   BuenoFactory bf = BuenoFactory.getFactory();
   bf.createNew(BuenoType.NEW_PACKAGE,insertion_point,property_set);
   
   // create the initial class
   bf.createNew(BuenoType.NEW_MODULE,insertion_point,property_set);
   
   String proj = insertion_point.getProject();
   File f = insertion_point.getInsertionFile();
   if (f == null) return;
   
   String pkg = property_set.getStringProperty(BuenoKey.KEY_PACKAGE);
   String cls = pkg + "." + property_set.getStringProperty(BuenoKey.KEY_NAME);
   
   if (bubble_creator != null) {
      bubble_creator.createBubble(proj,cls,bba,p);
    }
   
   if (property_set.getBooleanProperty(BuenoKey.KEY_CREATE_INIT)) {
      property_set.put(BuenoKey.KEY_NAME,"__init__");
      bf.createNew(BuenoType.NEW_MODULE,insertion_point,property_set);
      f = insertion_point.getInsertionFile();
      if (f != null) {
         cls = pkg + ".__init__";
         if (bubble_creator != null) {
            bubble_creator.createBubble(proj,cls,bba,p);
          }
       }
    }
}







}       // end of class BuenoPythonPackageDialog




/* end of BuenoPythonPackageDialog.java */

