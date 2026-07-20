/********************************************************************************/
/*                                                                              */
/*              BeduAssignmentWizard.java                                       */
/*                                                                              */
/*      Wizard to select an assignment                                          */
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



package edu.brown.cs.bubbles.bedu;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.border.Border;

import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.ivy.swing.SwingGridPanel;

class BeduAssignmentWizard extends SwingGridPanel implements BeduConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String course_name;
private JLabel assignment_description;
private JComboBox<String> drop_down;
private List<Assignment> avail_assignments;

private static final long serialVersionUID = 1;

protected static final Border EMPTY_BORDER=BorderFactory.createEmptyBorder(2,2,2,2);
protected static final Border HOVER_BORDER=BorderFactory.createLineBorder(Color.RED, 2);


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BeduAssignmentWizard()
{
   course_name = BoardSetup.getSetup().getCourseName();
   
   avail_assignments = new ArrayList<Assignment>();
   for (Assignment asg : BeduFactory.getFactory().getCreatableAssignments()) {
      avail_assignments.add(asg);
    }
   
   //Construct the UI
   setupDropDownAll();
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

/*
 * Drop down list of all assignments, past and current
 */
private void setupDropDownAll()
{
   setDropDown();
   setPreferredSize(new Dimension(400,100));
}



/*
 * Drop down list of a single String of items, separated by new-lines
 */
private void setDropDown()
{
   addSectionLabel(course_name+" Assignments");
   
   List<String> asgs = new ArrayList<String>();
   int idx = -1;
   int ct = 0;
   String desc = null;
   for (Assignment asg : avail_assignments) {
      asgs.add(asg.getName());
      if (idx < 0 && asg.isCurrent()) {
	 idx = ct;
	 desc = asg.getDescription();
       }
      ++ct;
    }
   if (idx < 0) idx = 0;
   ChooseAction ca = new ChooseAction();
   drop_down = addChoice("Choose an Assignment",asgs,idx,ca);
   if (desc != null) assignment_description = addDescription("Description",desc);
   addBottomButton("CREATE PROJECT", "CREATE PROJECT", ca);
   addBottomButtons();
}




/********************************************************************************/
/*										*/
/*	Choose Action								*/
/*										*/
/********************************************************************************/

private final class ChooseAction implements ActionListener {
   
   @Override public void actionPerformed(ActionEvent evt) {
      JComboBox<?> dd = drop_down;
      if (dd == null) {
         if (evt.getSource() instanceof JComboBox) {
            dd = (JComboBox<?>) evt.getSource();
          }
       }
      if (dd == null) return;
      
      Assignment sel = null;
      for (Assignment asg : avail_assignments) {
         if (asg.getName().equals(dd.getSelectedItem())) {
            sel = asg;
            break;
          }
       }
      String cmd = evt.getActionCommand();
      if (cmd == null);
      else if (cmd.equals("CREATE PROJECT")) {
         BoardMetrics.noteCommand("BWIZ","CreateAssignmentProject");
         if (sel != null) sel.createProject();
       }
      else if (assignment_description != null) {
         if (sel == null) assignment_description.setText(null);
         else assignment_description.setText(sel.getDescription());
       }
    }
   
}  // end of inner class ChooseAction



/********************************************************************************/
/*                                                                              */
/*      Bubble methods                                                          */
/*                                                                              */
/********************************************************************************/

BudaBubble createBubble(BudaRoot root)
{
   AssignmentWizardBubble bbl = new AssignmentWizardBubble(this,null);
   BudaBubbleArea bba = root.getCurrentBubbleArea();
   bba.addBubble(bbl,null,null,BudaConstants.PLACEMENT_LOGICAL|
         BudaConstants.PLACEMENT_USER|
         BudaConstants.PLACEMENT_MOVETO);
   return bbl;
}


private class AssignmentWizardBubble extends BudaBubble {
   
   private static final long serialVersionUID = 1;
   
   AssignmentWizardBubble(Component c,Component fc) {
      setContentPane(c,fc);
    }

}	// end of inner class WizardBubble


}       // end of class BeduAssignmentWizard




/* end of BeduAssignmentWizard.java */

