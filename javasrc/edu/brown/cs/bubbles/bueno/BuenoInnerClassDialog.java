/********************************************************************************/
/*										*/
/*		BuenoInnerClassDialog.java					*/
/*										*/
/*	BUbbles Environment New Objects creator new method dialog		*/
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



package edu.brown.cs.bubbles.bueno;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;

import edu.brown.cs.ivy.swing.SwingGridPanel;

import java.awt.Point;
import java.io.File;



public class BuenoInnerClassDialog extends BuenoAbstractDialog implements BuenoConstants
{



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public BuenoInnerClassDialog(BudaBubble source,Point locale,BuenoType typ,
			    BuenoProperties known,BuenoLocation insert,
			    BuenoBubbleCreator newer)
{
   super(source,locale,known,insert,newer,typ);
}



/********************************************************************************/
/*										*/
/*	Class dialog panel setup						*/
/*										*/
/********************************************************************************/

@Override protected void setupPanel(SwingGridPanel pnl)
{
   StringField sfld = new StringField(BuenoKey.KEY_SIGNATURE);
   pnl.addRawComponent("Inner Class Signature",sfld);
   sfld.addActionListener(this);

   /****************************
   pnl.addRawComponent("Class Name",new StringField(BuenoKey.KEY_NAME));

   JPanel options = new JPanel(new GridLayout(1,4));
   options.add(new ProtectionButton("public",Modifier.PUBLIC));
   options.add(new ProtectionButton("protected",Modifier.PROTECTED));
   options.add(new ProtectionButton("default",0));
   options.add(new ProtectionButton("private",Modifier.PRIVATE));
   pnl.addRawComponent("Protection",options);

   options = new JPanel(new FlowLayout(FlowLayout.LEFT));
   options.add(new ModifierButton("Abstract",Modifier.ABSTRACT));
   pnl.addRawComponent("Options",options);
   *****************************/
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

   if (bubble_creator != null) {
      String nm = getClassName();
      bubble_creator.createBubble(proj,nm,bba,p);
    }
}













/********************************************************************************/
/*										*/
/*	Get the full name of the new method					*/
/*										*/
/********************************************************************************/

private String getClassName()
{
   StringBuffer buf = new StringBuffer();
   String cls = insertion_point.getClassName();
   buf.append(cls);
   buf.append("$");
   buf.append(property_set.getStringProperty(BuenoKey.KEY_NAME));

   return buf.toString();
}




}	// end of class BuenoInnerClassDialog



/* end of BuenoInnerClassDialog.java */

