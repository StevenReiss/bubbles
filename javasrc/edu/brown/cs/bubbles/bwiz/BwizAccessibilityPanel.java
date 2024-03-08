/********************************************************************************/
/*										*/
/*		BwizAccessibilityPanel.java					*/
/*										*/
/*	Panel with accessibiliy options 					*/
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

import edu.brown.cs.bubbles.board.BoardColors;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;



class BwizAccessibilityPanel extends JPanel implements BwizConstants
{

/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private JRadioButton public_button;
private JRadioButton default_button;
private JRadioButton protected_button;
private JRadioButton private_button;
private JCheckBox    abstract_checkbox;
private JCheckBox    final_checkbox;
private JCheckBox    override_checkbox;
private JRadioButton class_button;
private JRadioButton interface_button;

private ButtonGroup radio_group;
private ButtonGroup class_group;

protected GridBagConstraints use_constraints;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public BwizAccessibilityPanel(int fgs)
{
   use_constraints = new GridBagConstraints();

   setup(fgs);
   addTypeActionListener(new TypeListener());
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

/* sets up the public and default options on the accessibility panel */
protected void setup(int fgs)
{
   setLayout(new GridBagLayout());
   setOpaque(false);
   setAlignmentX(Component.LEFT_ALIGNMENT);

   //RadioButton group
   radio_group = new ButtonGroup();
   class_group = new ButtonGroup();

   Color bg = BoardColors.getColor("Bwiz.AccessButtonBackground");
   //Getting the string from an enum
   public_button = new JRadioButton(Accessibility.PUBLIC.toString(),false);
   public_button.setActionCommand(Accessibility.PUBLIC.toString());
   public_button.setAlignmentX(Component.LEFT_ALIGNMENT);
   public_button.setFont(BWIZ_FONT_OPTION);
   public_button.setBackground(bg);
   radio_group.add(public_button);

   default_button = new JRadioButton(Accessibility.DEFAULT.toString(),true);
   default_button.setActionCommand(Accessibility.DEFAULT.toString());
   default_button.setAlignmentX(Component.LEFT_ALIGNMENT);
   default_button.setFont(BWIZ_FONT_OPTION);
   default_button.setBackground(bg);
   radio_group.add(default_button);

   if ((fgs & SHOW_PRIVATE) != 0) {
      private_button = new JRadioButton(Accessibility.PRIVATE.toString(),true);
      private_button.setActionCommand(Accessibility.PRIVATE.toString());
      private_button.setAlignmentX(Component.LEFT_ALIGNMENT);
      private_button.setFont(BWIZ_FONT_OPTION);
      private_button.setBackground(bg);
      radio_group.add(private_button);
    }
   else private_button = null;

   if ((fgs & SHOW_PROTECTED) != 0) {
      protected_button = new JRadioButton(Accessibility.PROTECTED.toString(),false);
      protected_button.setActionCommand(Accessibility.PROTECTED.toString());
      protected_button.setAlignmentX(Component.LEFT_ALIGNMENT);
      protected_button.setFont(BWIZ_FONT_OPTION);
      protected_button.setBackground(bg);
      radio_group.add(protected_button);
    }
   else protected_button = null;
   
   if ((fgs & SHOW_CLASS) != 0) {
      class_button = new JRadioButton("class",true);
      class_button.setActionCommand("class");
      class_button.setAlignmentX(Component.LEFT_ALIGNMENT);
      class_button.setFont(BWIZ_FONT_OPTION);
      class_button.setBackground(bg);
      class_group.add(class_button);
    }
   else class_button = null;
   
   if ((fgs & SHOW_INTERFACE) != 0) {
      interface_button = new JRadioButton("interface",true);
      interface_button.setActionCommand("interface");
      interface_button.setAlignmentX(Component.LEFT_ALIGNMENT);
      interface_button.setFont(BWIZ_FONT_OPTION);
      interface_button.setBackground(bg);
      class_group.add(interface_button);
    }
   else interface_button = null;
   
   if ((fgs & SHOW_ABSTRACT) != 0) {
      abstract_checkbox = new JCheckBox("abstract", false);
      abstract_checkbox.setBackground(bg);
      abstract_checkbox.setAlignmentY(Component.TOP_ALIGNMENT);
      abstract_checkbox.setFont(BWIZ_FONT_OPTION);
    }
   else abstract_checkbox = null;

   if ((fgs & SHOW_FINAL) != 0) {
      final_checkbox = new JCheckBox("final", false);
      final_checkbox.setBackground(bg);
      final_checkbox.setAlignmentY(Component.TOP_ALIGNMENT);
      final_checkbox.setFont(BWIZ_FONT_OPTION);
    }
   else final_checkbox = null;
   
   if ((fgs & SHOW_OVERRIDES) != 0) {
      override_checkbox = new JCheckBox("@Override", false);
      override_checkbox.setBackground(bg);
      override_checkbox.setAlignmentY(Component.TOP_ALIGNMENT);
      override_checkbox.setFont(BWIZ_FONT_OPTION);
    }
   else override_checkbox = null;
   
   //UI layout
   use_constraints.fill = GridBagConstraints.NONE;
   use_constraints.gridx = 0;
   use_constraints.gridy = 0;
   use_constraints.gridwidth = 1;
   use_constraints.gridheight = 1;
   use_constraints.weightx = 0.5;
   use_constraints.anchor = GridBagConstraints.LINE_START;

   if (class_button != null) {
      add(class_button, use_constraints);
      use_constraints.gridy += 1;
      class_button.setSelected(true);
    }
   
   if (interface_button != null) {
      add(interface_button, use_constraints);
      use_constraints.gridy += 1;
    }
   
   if (public_button != null) {
      add(public_button, use_constraints);
      use_constraints.gridy += 1;
    }

   if (default_button != null) {
      add(default_button, use_constraints);
      use_constraints.gridy += 1;
    }

   if (private_button != null) {
      add(private_button, use_constraints);
      use_constraints.gridy += 1;
      private_button.setSelected(true);
    }

   if (protected_button != null) {
      add(protected_button, use_constraints);
      use_constraints.gridy += 1;
    }

   use_constraints.gridx = 1;
   use_constraints.gridy = 0;
   if (abstract_checkbox != null) {
      add(abstract_checkbox,use_constraints);
      use_constraints.gridy += 1;
    }
   if (final_checkbox != null) {
      add(final_checkbox,use_constraints);
      use_constraints.gridy += 1;
    }
   if (override_checkbox != null) {
      add(override_checkbox,use_constraints);
      use_constraints.gridy += 1;
    }
}



/********************************************************************************/
/*										*/
/*	Selection management							*/
/*										*/
/********************************************************************************/

public void setSelected(Accessibility a)
{
   switch (a) {
      case PRIVATE :
	 private_button.setSelected(true);
	 break;
      case PROTECTED :
	 protected_button.setSelected(true);
	 break;
      case PUBLIC :
	 public_button.setSelected(true);
	 break;
      case DEFAULT :
	 default_button.setSelected(true);
	 break;
    }
}



public void clearSelection()
{
   public_button.setSelected(false);

   default_button.setSelected(false);

   if (private_button != null) {
      private_button.setSelected(false);
      protected_button.setSelected(false);
   }
}



/********************************************************************************/
/*										*/
/*	Action management							*/
/*										*/
/********************************************************************************/

public void addAccessibilityActionListener(ActionListener listener)
{
   if (public_button != null) public_button.addActionListener(listener);
   if (default_button != null) default_button.addActionListener(listener);
   if (private_button != null) private_button.addActionListener(listener);
   if (protected_button != null) protected_button.addActionListener(listener);
}


public void addTypeActionListener(ActionListener listener)
{
   if (class_button != null) class_button.addActionListener(listener);
   if (interface_button != null) interface_button.addActionListener(listener);
}


public void addModifierListener(ItemListener listener)
{
   if (abstract_checkbox != null) abstract_checkbox.addItemListener(listener);
   if (final_checkbox != null) final_checkbox.addItemListener(listener);
   if (override_checkbox != null) override_checkbox.addItemListener(listener);
}



private class TypeListener implements ActionListener {
   
   @Override public void actionPerformed(ActionEvent evt) {
     String cmd = evt.getActionCommand();
     if (cmd.equalsIgnoreCase("interface")) {
        if (abstract_checkbox != null) abstract_checkbox.setEnabled(false);
        if (final_checkbox != null) final_checkbox.setEnabled(false);
      }
     else {
        if (abstract_checkbox != null) abstract_checkbox.setEnabled(true);
        if (final_checkbox != null) final_checkbox.setEnabled(true);
      }
    }
}


}	// end of class BwizAccessibilityPanel



/* end of BwizAccessibilityPanel.java */
