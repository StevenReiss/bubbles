/********************************************************************************/
/*										*/
/*		BwizFactory.java						*/
/*										*/
/*	Factory to set up wizards as part of bubbles				*/
/*										*/
/********************************************************************************/
/*	Copyright 2013 Brown University -- Annalia Sunderland		      */
/*	Copyright 2013 Brown University -- Steven P. Reiss		      */
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

import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bueno.BuenoConstants.BuenoBubbleCreator;
import edu.brown.cs.bubbles.bueno.BuenoConstants.BuenoClassCreatorInstance;
import edu.brown.cs.bubbles.bueno.BuenoConstants.BuenoMethodCreatorInstance;
import edu.brown.cs.bubbles.bueno.BuenoConstants.BuenoPackageCreatorInstance;
import edu.brown.cs.bubbles.bueno.BuenoConstants.BuenoType;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.bubbles.bueno.BuenoFactory;
import edu.brown.cs.bubbles.bueno.BuenoLocation;
import edu.brown.cs.bubbles.bueno.BuenoProperties;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;

import org.w3c.dom.Element;




public final class BwizFactory implements BwizConstants, BudaConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BudaRoot	buda_root;

private static BwizFactory the_factory;




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

public static void setup()
{
   the_factory = new BwizFactory();
}



public static void initialize(BudaRoot br)
{
   the_factory.setupWizards(br);
}



public static BwizFactory getFactory()
{
   return the_factory;
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BwizFactory()
{
   buda_root = null;
}



/********************************************************************************/
/*										*/
/*	Panel setup methods							*/
/*										*/
/********************************************************************************/

private void setupWizards(BudaRoot br)
{
   buda_root = br;

   Element data = BumpClient.getBump().getLanguageData();
   Element pdata = IvyXml.getChild(data,"PROJECT");
   Element wdata = IvyXml.getChild(pdata,"WIZARDS");
   if (wdata != null) {
      BwizStartWizard pnl = new BwizStartWizard(wdata);
      br.addPanel(pnl,false);
    }

   CreatorWizard mc = new CreatorWizard(wdata);
   BuenoFactory.getFactory().setMethodDialog(mc);
   BuenoFactory.getFactory().setClassDialog(mc);
   BuenoFactory.getFactory().setPackageDialog(mc);
}



/********************************************************************************/
/*										*/
/*	Bubble methods								*/
/*										*/
/********************************************************************************/

void createBubble(Component c,Component fc)
{
   WizardBubble bb = new WizardBubble(c,fc);
   BudaBubbleArea bba = buda_root.getCurrentBubbleArea();
   bba.addBubble(bb,null,null,PLACEMENT_LOGICAL|PLACEMENT_USER|PLACEMENT_MOVETO);
}



private class WizardBubble extends BudaBubble {
   
   private static final long serialVersionUID = 1;
   
   WizardBubble(Component c,Component fc) {
      setContentPane(c,fc);
    }

}	// end of inner class WizardBubble




/********************************************************************************/
/*										*/
/*	Methods for using new method wizard dialog				*/
/*										*/
/********************************************************************************/

private class CreatorWizard implements BuenoMethodCreatorInstance,
	BuenoClassCreatorInstance, BuenoPackageCreatorInstance {


   private Element wizard_data;
   
   CreatorWizard(Element wdata) {
      wizard_data = wdata;
    }
   
   @Override public boolean showMethodDialogBubble(BudaBubble src,Point loc,
        					      BuenoProperties known,
        					      BuenoLocation insert,
        					      String lbl,
        					      BuenoBubbleCreator newer) {
      BoardProperties bp = BoardProperties.getProperties("Bwiz");
      if (!bp.getBoolean("Bwiz.use.method.dialog") || wizard_data == null) return false;
   
      BwizNewWizard bcwiz = new BwizNewMethodWizard(insert);
      bcwiz.setBubbleCreator(newer);
      WizardBubble bb = new WizardBubble(bcwiz,bcwiz.getFocus());
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(src);
      if (bba == null) bba = buda_root.getCurrentBubbleArea();
      bba.addBubble(bb,src,loc,PLACEMENT_PREFER|PLACEMENT_GROUPED|PLACEMENT_MOVETO|
        	       PLACEMENT_LOGICAL);
   
      return true;
    }

  @Override public boolean showClassDialogBubble(BudaBubble src,Point loc,
	BuenoType typ,
	 BuenoProperties known,
	 BuenoLocation insert,
	 String lbl,
	 BuenoBubbleCreator newer) {
      BoardProperties bp = BoardProperties.getProperties("Bwiz");
      if (!bp.getBoolean("Bwiz.use.class.dialog") || wizard_data == null) return false;

      BwizNewWizard ccwiz = null;
      switch (typ) {
	 case NEW_CLASS :
	    ccwiz = new BwizNewClassWizard(insert);
	    break;
	 case NEW_INTERFACE :
	    ccwiz = new BwizNewInterfaceWizard(insert);
	    break;
	 case NEW_ENUM :
	    ccwiz = new BwizNewEnumWizard(insert);
	    break;
	 default :
	    return false;
       }

      ccwiz.setBubbleCreator(newer);
      WizardBubble bb = new WizardBubble(ccwiz,ccwiz.getFocus());
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(src);
      if (bba == null) bba = buda_root.getCurrentBubbleArea();
      bba.addBubble(bb,src,loc,PLACEMENT_PREFER|PLACEMENT_GROUPED|PLACEMENT_MOVETO|
	    PLACEMENT_LOGICAL);

      return true;
    }

  @Override public boolean showPackageDialogBubble(BudaBubble src,Point loc,
	BuenoType typ,
        BuenoProperties known,
        BuenoLocation insert,
        String lbl,
        BuenoBubbleCreator newer) {
     BoardProperties bp = BoardProperties.getProperties("Bwiz");
     if (!bp.getBoolean("Bwiz.use.package.dialog") || wizard_data == null) return false;
     
     BwizNewWizard ccwiz = new BwizNewPackageWizard(insert);
     Dimension d = ccwiz.getPreferredSize();
     d.width = Math.max(d.width,500);
     ccwiz.setPreferredSize(d);
     
     ccwiz.setBubbleCreator(newer);
     WizardBubble bb = new WizardBubble(ccwiz,ccwiz.getFocus());
     BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(src);
     if (bba == null) bba = buda_root.getCurrentBubbleArea();
     bba.addBubble(bb,src,loc,PLACEMENT_PREFER|PLACEMENT_GROUPED|PLACEMENT_MOVETO|
           PLACEMENT_LOGICAL);
     
     return true;
   }
  
  @Override public boolean useSeparateTypeButtons() {
     return IvyXml.getAttrBool(wizard_data,"SEPARATE_TYPES");
   }

}	// end of inner class MethodCreator




}	// end of class BwizFactory




/* end of BwizFactory.java */
