/********************************************************************************/
/*                                                                              */
/*              BuenoPythonModuleDialog.java                                    */
/*                                                                              */
/*      Dialog to create a new python module                                    */
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



public class BuenoPythonModuleDialog extends BuenoAbstractDialog implements BuenoConstants
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

public BuenoPythonModuleDialog(BudaBubble source,Point locale,
      BuenoProperties known,BuenoLocation insert,
      BuenoBubbleCreator newer)
{
   super(source,locale,known,insert,newer,BuenoType.NEW_MODULE);
}



/********************************************************************************/
/*										*/
/*	Class dialog panel setup						*/
/*										*/
/********************************************************************************/

@Override protected void setupPanel(SwingGridPanel pnl)
{
   StringField sfld = new StringField(BuenoKey.KEY_NAME);
   pnl.addRawComponent("Module Name",sfld);
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
   bf.createNew(create_type,insertion_point,property_set);
   
   String proj = insertion_point.getProject();
   File f = insertion_point.getInsertionFile();
   if (f == null) return;

   String pkg = property_set.getStringProperty(BuenoKey.KEY_PACKAGE);
   String mod = pkg + "." + property_set.getStringProperty(BuenoKey.KEY_NAME);   
   
   if (bubble_creator != null) {
      bubble_creator.createBubble(proj,mod,bba,p);
    }
}







}       // end of class BuenoPythonModuleDialog




/* end of BuenoPythonModuleDialog.java */

