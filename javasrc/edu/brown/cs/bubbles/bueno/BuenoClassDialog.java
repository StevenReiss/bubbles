/********************************************************************************/
/*										*/
/*		BuenoClassDialog.java						*/
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
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants.BumpContractType;

import edu.brown.cs.ivy.swing.SwingGridPanel;

import java.awt.Point;
import java.io.File;



class BuenoClassDialog extends BuenoAbstractDialog implements BuenoConstants
{



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BuenoClassDialog(BudaBubble source,Point locale,BuenoType typ,
			    BuenoProperties known,BuenoLocation insert,
			    BuenoBubbleCreator newer)
{
   super(source,locale,known,insert,newer,typ);

   String prj = property_set.getStringProperty(BuenoKey.KEY_PROJECT);
   if (prj == null) {
      prj = insertion_point.getProject();
      if (prj != null) property_set.put(BuenoKey.KEY_PROJECT,prj);
    }

   BumpContractType bct = BumpClient.getBump().getContractType(prj);

   if (bct == null) return;

   if (bct.useContractsForJava()) {
      String imp = "import com.google.java.contract.*;";
      property_set.addToArrayProperty(BuenoKey.KEY_IMPORTS,imp);
    }

   if (bct.useJunit()) {
      String imp = "import org.junit.*;";
      property_set.addToArrayProperty(BuenoKey.KEY_IMPORTS,imp);
    }

   if (bct.useTypeAnnotations()) {
      // String imp = "import javax.annotation.*;";
      // property_set.addToArrayProperty(BuenoKey.KEY_IMPORTS,imp);
      String imp1 = "import edu.brown.cs.karma.*;";
      property_set.addToArrayProperty(BuenoKey.KEY_IMPORTS,imp1);
      imp1 = "import static edu.brown.cs.karma.KarmaUtils.KarmaEvent;";
      property_set.addToArrayProperty(BuenoKey.KEY_IMPORTS,imp1);
    }
}



/********************************************************************************/
/*										*/
/*	Class dialog panel setup						*/
/*										*/
/********************************************************************************/

@Override protected void setupPanel(SwingGridPanel pnl)
{
   StringField sfld = new StringField(BuenoKey.KEY_SIGNATURE);
   pnl.addRawComponent("Class Signature",sfld);
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
   String pkg = insertion_point.getPackage();
   if (pkg != null) {
      buf.append(pkg);
      buf.append(".");
    }
   buf.append(property_set.getStringProperty(BuenoKey.KEY_NAME));

   return buf.toString();
}





}	// end of class BuenoClassDialog



/* end of BuenoClassDialog.java */

