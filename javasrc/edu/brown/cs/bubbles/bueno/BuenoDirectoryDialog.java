/********************************************************************************/
/*                                                                              */
/*              BuenoDirectoryDialog.java                                       */
/*                                                                              */
/*      description of class                                                    */
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



package edu.brown.cs.bubbles.bueno;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;

import java.awt.Point;
import java.io.File;

import edu.brown.cs.ivy.swing.SwingGridPanel;

class BuenoDirectoryDialog extends BuenoAbstractDialog implements BuenoConstants
{

/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BuenoDirectoryDialog(BudaBubble source,Point locale,
      BuenoProperties known,BuenoLocation insert,
      BuenoBubbleCreator newer)
{
   super(source,locale,known,insert,newer,BuenoType.NEW_DIRECTORY);
}

/********************************************************************************/
/*										*/
/*	Package dialog panel setup						*/
/*										*/
/********************************************************************************/

@Override protected void setupPanel(SwingGridPanel pnl)
{
   String dir = property_set.getStringProperty(BuenoKey.KEY_DIRECTORY); 
   if (dir == null) {
      File f = insertion_point.getFile();
      if (f != null && !f.isDirectory()) {
         f = f.getParentFile();
       }
      if (f != null) {
         dir = f.getPath() + "/";
       }
    }
   
   StringField sfld = new StringField(BuenoKey.KEY_DIRECTORY);
   pnl.addRawComponent("Directory Name",sfld);
   sfld.addActionListener(this);
   
   sfld = new StringField(BuenoKey.KEY_NAME);
   pnl.addRawComponent("Initial File/Module",sfld);
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
   bf.createNew(BuenoType.NEW_DIRECTORY,insertion_point,property_set);
   
   // create the initial file
   bf.createNew(BuenoType.NEW_FILE,insertion_point,property_set);
   
   String proj = insertion_point.getProject();
   File f = insertion_point.getInsertionFile();
   if (f == null) return;
   
   String pkg = property_set.getStringProperty(BuenoKey.KEY_PACKAGE);
   String cls = pkg + "." + property_set.getStringProperty(BuenoKey.KEY_NAME);
   
   if (bubble_creator != null) {
      bubble_creator.createBubble(proj,cls,bba,p);
    }
}



}       // end of class BuenoDirectoryDialog




/* end of BuenoDirectoryDialog.java */

