/********************************************************************************/
/*										*/
/*		BwizStartWizard.java						*/
/*										*/
/*	Startup wizard for bubbles						*/
/*										*/
/********************************************************************************/
/*	Copyright 2013 UCF -- Jared Bott				      */
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

import edu.brown.cs.bubbles.bedu.BeduConstants.Assignment;
import edu.brown.cs.bubbles.bedu.BeduFactory;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.ivy.xml.IvyXml;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.w3c.dom.Element;

import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class BwizStartWizard extends JPanel implements BwizConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Fields                                                          */
/*                                                                              */
/********************************************************************************/

private Element         wizard_data;

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BwizStartWizard(Element wdata)
{
   wizard_data = wdata;
   
   setup();
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

private void setup()
{
   //Main UI panel
   setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
   BoardColors.setColors(this,PANEL_OVERVIEW_COLOR_PROP);
   setAlignmentX(Component.LEFT_ALIGNMENT);
   setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

   /*Panel to do a grid layout.
	* This doesn't work as well as I'd like.
	*/
   JPanel gridpanel=new JPanel();
   GroupLayout layout=new GroupLayout(gridpanel);
   layout.setAutoCreateGaps(true);
   gridpanel.setLayout(layout);
   gridpanel.setOpaque(false);

   //Hardcoded size of window; not so great
   // gridpanel.setPreferredSize(new Dimension(140,85));

   add(gridpanel);
   
   Map<String,Element> types = new HashMap<>();
   for (Element btn : IvyXml.children(wizard_data,"BUTTON")) {
      String typ = IvyXml.getAttrString(btn,"TYPE");
      types.put(typ,btn);
    }

   JButton classpanel = null;
   JButton interfacepanel = null;
   JButton enumpanel = null;
   JButton assignmentpanel = null;
   
   //UI Panel creating a new class
   Element bdata = types.get("NEWCLASS");
   if (bdata != null) {
      classpanel = new WizardButton(new CreateClassAction(bdata));
    }
   //UI Panel creating a new interface
   bdata = types.get("NEWINTERFACE");
   if (bdata != null) {
      interfacepanel = new WizardButton(new CreateInterfaceAction(bdata));
    }
   //UI Panel creating a new enum
   bdata = types.get("NEWENUM");
   if (bdata != null) {
      enumpanel = new WizardButton(new CreateEnumAction(bdata));
    }
   //UI Panel for opening an assignment
   List<Assignment> asgs = BeduFactory.getFactory().getCreatableAssignments();
   bdata = types.get("NEWASSIGNMENT");
   if (asgs != null && asgs.size() > 0 && bdata != null) {
      assignmentpanel = new WizardButton(new OpenAssignmentAction(bdata));
    }

   //UI layout
   
   GroupLayout.SequentialGroup sg = layout.createSequentialGroup();
   GroupLayout.ParallelGroup pg = layout.createParallelGroup();
   if (classpanel != null) pg.addComponent(classpanel);
   if (interfacepanel != null) pg.addComponent(interfacepanel);
   if (enumpanel != null) pg.addComponent(enumpanel);
   if (assignmentpanel != null) pg.addComponent(assignmentpanel);
   sg.addGroup(pg);
   layout.setHorizontalGroup(sg);
   
   sg = layout.createSequentialGroup();
   if (classpanel != null) {
      sg.addGroup(layout.createParallelGroup().addComponent(classpanel));
    }
   if (interfacepanel != null) {
      sg.addGroup(layout.createParallelGroup().addComponent(interfacepanel));
    }
   if (enumpanel != null) {
      sg.addGroup(layout.createParallelGroup().addComponent(enumpanel));
    }
   if (assignmentpanel != null) {
      sg.addGroup(layout.createParallelGroup().addComponent(assignmentpanel));
    }
   layout.setVerticalGroup(sg);
}



/********************************************************************************/
/*										*/
/*	Buttons 								*/
/*										*/
/********************************************************************************/

private class WizardButton extends JButton {

   private static final long serialVersionUID = 1;
   
   WizardButton(Action a) {
      super(a);
      setOpaque(false);
      BoardColors.setColors(this,PANEL_OVERVIEW_COLOR_PROP);
      setMargin(new Insets(0,1,0,1));
      setFont(BWIZ_FONT_BUTTON);
      setAlignmentX(LEFT_ALIGNMENT);
    }
}



/********************************************************************************/
/*										*/
/*	Actions 								*/
/*										*/
/********************************************************************************/

private class CreateClassAction extends AbstractAction {

   private static final long serialVersionUID = 1;
   
   CreateClassAction(Element bdata) {
      super(CREATE_CLASS_TEXT);
      String desc = IvyXml.getAttrString(bdata,"DESCRIPTION",
            "Create a new top-level class");
      putValue(SHORT_DESCRIPTION,desc);
      String lbl = IvyXml.getAttrString(bdata,"LABEL");
      if (lbl != null) putValue(NAME,lbl);
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BwizNewWizard bcwiz = new BwizNewClassWizard(null);
      BwizFactory.getFactory().createBubble(bcwiz,bcwiz.getFocus());
    }

}	// end of inner class CreateClassAction


private class CreateInterfaceAction extends AbstractAction {

   private static final long serialVersionUID = 1;
   
   CreateInterfaceAction(Element bdata) {
      super(CREATE_INTERFACE_TEXT);
      String desc = IvyXml.getAttrString(bdata,"DESCRIPTION",
            "Create a new top-level interface");
      putValue(SHORT_DESCRIPTION,desc);
      String lbl = IvyXml.getAttrString(bdata,"LABEL");
      if (lbl != null) putValue(NAME,lbl);
    }
   
   @Override public void actionPerformed(ActionEvent evt) {
      BwizNewWizard bcwiz = new BwizNewInterfaceWizard(null);
      BwizFactory.getFactory().createBubble(bcwiz,bcwiz.getFocus());
    }

}	// end of inner class CreateInterfaceAction


private class CreateEnumAction extends AbstractAction {
   
   private static final long serialVersionUID = 1;
   
   CreateEnumAction(Element bdata) {
      super(CREATE_ENUM_TEXT);
      String desc = IvyXml.getAttrString(bdata,"DESCRIPTION",
            "Create a new top-level enumeration");
      putValue(SHORT_DESCRIPTION,desc);
      String lbl = IvyXml.getAttrString(bdata,"LABEL");
      if (lbl != null) putValue(NAME,lbl);
    }
   
   @Override public void actionPerformed(ActionEvent evt) {
      BwizNewWizard bcwiz = new BwizNewEnumWizard(null);
      BwizFactory.getFactory().createBubble(bcwiz,bcwiz.getFocus());
    }

}	// end of inner class CreateEnumAction


private class OpenAssignmentAction extends AbstractAction {

   private static final long serialVersionUID = 1;
   
   OpenAssignmentAction(Element bdata) {
      super(OPEN_ASSIGNMENT_TEXT);
      String desc = IvyXml.getAttrString(bdata,"DESCRIPTION",
            "Create a new course assignment");
      putValue(SHORT_DESCRIPTION,desc);
      String lbl = IvyXml.getAttrString(bdata,"LABEL");
      if (lbl != null) putValue(NAME,lbl);
    }
   
   @Override public void actionPerformed(ActionEvent evt) {
      BwizAssignmentWizard bcwiz = new BwizAssignmentWizard();
      BwizFactory.getFactory().createBubble(bcwiz,null);
    }

}	// end of inner class OpenAssignmentAction




}	// end of class BwizStartWizard



/* end of BwizStartWizard.java */






































