/********************************************************************************/
/*                                                                              */
/*              BuenoJsModuleDialog.java                                        */
/*                                                                              */
/*      Dialog for creating a JavaScript file/module                            */
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

import java.awt.Point;
import java.io.File;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.ivy.swing.SwingGridPanel;

public class BuenoJsModuleDialog extends BuenoAbstractDialog implements BuenoConstants
{



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public BuenoJsModuleDialog(BudaBubble source,Point locale,
      BuenoProperties known,BuenoLocation insert,
      BuenoBubbleCreator newer)
{
   super(source,locale,known,insert,newer,BuenoType.NEW_FILE);
}


/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override void setupPanel(SwingGridPanel pnl)
{
   StringField sfld = new StringField(BuenoKey.KEY_NAME);
   pnl.addRawComponent("File/Module Name",sfld);
   sfld.addActionListener(this);
}



@Override void doCreate(BudaBubbleArea bba,Point p)
{
   BuenoFactory bf = BuenoFactory.getFactory();
   bf.createNew(create_type,insertion_point,property_set);
   
   String proj = insertion_point.getProject();
   File f = insertion_point.getInsertionFile();
   if (f == null) return;
   
   if (bubble_creator != null) {
      bubble_creator.createBubble(proj,f.getPath(),bba,p);
    }
}



}       // end of class BuenoJsModuleDialog




/* end of BuenoJsModuleDialog.java */

