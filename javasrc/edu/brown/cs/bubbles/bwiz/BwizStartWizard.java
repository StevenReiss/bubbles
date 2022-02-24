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
import edu.brown.cs.bubbles.board.BoardSetup;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.List;


class BwizStartWizard extends JPanel implements BwizConstants
{


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BwizStartWizard()
{
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

   //UI Panel creating a new class
   JButton classpanel = new WizardButton(new CreateClassAction());
   //UI Panel creating a new interface
   JButton interfacepanel = new WizardButton(new CreateInterfaceAction());
   //UI Panel creating a new enum
   JButton enumpanel = new WizardButton(new CreateEnumAction());
   //UI Panel for opening an assignment
   List<Assignment> asgs = BeduFactory.getFactory().getCreatableAssignments();
   JButton assignmentpanel = null;
   if (asgs != null && asgs.size() > 0) {
      assignmentpanel = new WizardButton(new OpenAssignmentAction());
    }

   //UI layout
   
   GroupLayout.SequentialGroup sg = layout.createSequentialGroup();
   GroupLayout.ParallelGroup pg = layout.createParallelGroup();
   switch (BoardSetup.getSetup().getLanguage()) {
      case JAVA :
      case JAVA_IDEA :
         pg.addComponent(classpanel);
         pg.addComponent(interfacepanel);
         pg.addComponent(enumpanel);   
         break;
      case PYTHON :
      case JS :
         // possibly add a new file button
         break;
      default :
         break;
    }
   if (assignmentpanel != null) pg.addComponent(assignmentpanel);
   sg.addGroup(pg);
   layout.setHorizontalGroup(sg);
   
   sg = layout.createSequentialGroup();
   switch (BoardSetup.getSetup().getLanguage()) {
      case JAVA :
      case JAVA_IDEA :
         sg.addGroup(layout.createParallelGroup().addComponent(classpanel));
         sg.addGroup(layout.createParallelGroup().addComponent(interfacepanel));
         sg.addGroup(layout.createParallelGroup().addComponent(enumpanel));
         break;
      case PYTHON :
      case JS :
         // possibly add a new file button
         break;
      default :
         break;
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

   CreateClassAction() {
      super(CREATE_CLASS_TEXT);
      putValue(SHORT_DESCRIPTION,"Create a new top-level class");
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BwizNewWizard bcwiz = new BwizNewClassWizard(null);
      BwizFactory.getFactory().createBubble(bcwiz,bcwiz.getFocus());
    }

}	// end of inner class CreateClassAction


private class CreateInterfaceAction extends AbstractAction {

CreateInterfaceAction() {
   super(CREATE_INTERFACE_TEXT);
   putValue(SHORT_DESCRIPTION,"Create a new top-level interface");
 }

@Override public void actionPerformed(ActionEvent evt) {
   BwizNewWizard bcwiz = new BwizNewInterfaceWizard(null);
   BwizFactory.getFactory().createBubble(bcwiz,bcwiz.getFocus());
 }

}	// end of inner class CreateInterfaceAction


private class CreateEnumAction extends AbstractAction {

CreateEnumAction() {
   super(CREATE_ENUM_TEXT);
   putValue(SHORT_DESCRIPTION,"Create a new top-level enumeration");
 }

@Override public void actionPerformed(ActionEvent evt) {
   BwizNewWizard bcwiz = new BwizNewEnumWizard(null);
   BwizFactory.getFactory().createBubble(bcwiz,bcwiz.getFocus());
 }

}	// end of inner class CreateEnumAction


private class OpenAssignmentAction extends AbstractAction {

OpenAssignmentAction() {
   super(OPEN_ASSIGNMENT_TEXT);
   putValue(SHORT_DESCRIPTION,"Create a new assignment");
 }

@Override public void actionPerformed(ActionEvent evt) {
   BwizAssignmentWizard bcwiz = new BwizAssignmentWizard();
   BwizFactory.getFactory().createBubble(bcwiz,null);
 }

}	// end of inner class OpenAssignmentAction




}	// end of class BwizStartWizard



/* end of BwizStartWizard.java */






































