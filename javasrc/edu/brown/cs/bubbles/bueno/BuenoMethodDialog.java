/********************************************************************************/
/*										*/
/*		BuenoMethodDialog.java						*/
/*										*/
/*	BUbbles Environment New Objects creator new method dialog		*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.bueno;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;

import edu.brown.cs.ivy.swing.SwingGridPanel;

import java.awt.Point;
import java.io.File;



public class BuenoMethodDialog extends BuenoAbstractDialog implements BuenoConstants
{



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public BuenoMethodDialog(BudaBubble source,Point locale,
			    BuenoProperties known,BuenoLocation insert,
			    BuenoBubbleCreator newer)
{
   super(source,locale,known,insert,newer,BuenoType.NEW_METHOD);
}



/********************************************************************************/
/*										*/
/*	Method dialog panel setup						*/
/*										*/
/********************************************************************************/

@Override protected void setupPanel(SwingGridPanel pnl)
{
   StringField sfld = new StringField(BuenoKey.KEY_SIGNATURE);
   pnl.addRawComponent("Signature",sfld);
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

   if (bubble_creator != null) {
      String nm = getMethodName();
      bubble_creator.createBubble(proj,nm,bba,p);
    }
}
























/********************************************************************************/
/*										*/
/*	Get the full name of the new method					*/
/*										*/
/********************************************************************************/

private String getMethodName()
{
   StringBuffer buf = new StringBuffer();
   String cls = insertion_point.getClassName();
   buf.append(cls);
   buf.append(".");
   buf.append(property_set.getStringProperty(BuenoKey.KEY_NAME));

   buf.append("(");
   String [] params = property_set.getParameters();
   for (int i = 0; i < params.length; ++i) {
      if (i > 0) buf.append(",");
      int idx = params[i].lastIndexOf(" ");
      String typ = params[i].substring(0,idx);
      buf.append(typ);
    }
   buf.append(")");

   return buf.toString();
}




}	// end of class BuenoMethodDialog



/* end of BuenoMethodDialog.java */
