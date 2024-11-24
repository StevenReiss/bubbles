/********************************************************************************/
/*										*/
/*		BqstMultiChoices.java						*/
/*										*/
/*	Bubbles questions and forms multiple choice box implementation		*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Yu Li			      */
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


/* SVN: $Id$ */

package edu.brown.cs.bubbles.bqst;

import edu.brown.cs.ivy.swing.SwingTextField;
import java.awt.Dimension;
import java.util.ArrayList;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JRadioButton;
import javax.swing.JTextField;


/**
 * This class is subclass of BqstQuestion, and it implements a multiple choices question.
 **/

class BqstMultiChoices extends BqstQuestion implements BqstConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String[]		q_options;
private Icon[]			q_icons;
private Icon[]			q_sicons;
private ArrayList<JRadioButton> q_buttons;
private boolean 		q_others;
private JTextField		q_field;

private static final long	serialVersionUID = 1L;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected BqstMultiChoices(String qst,String help,String[] options,Icon[] icons,
			      Icon[] sicons,boolean required,boolean others)
{
   super(qst,QuestionType.MULTICHOICE,help);
   setRequired(required);
   q_options = options;
   q_buttons = new ArrayList<JRadioButton>();
   q_others = others;
   if (icons != null && icons.length == q_options.length) {
      if (sicons != null && icons.length == sicons.length) {
	 q_icons = icons;
	 q_sicons = sicons;

       }
    }
}




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

@Override void setup()
{
   super.setup();
   ButtonGroup bg = new ButtonGroup();
   for (int i = 0; i < q_options.length; i++) {
      JRadioButton button = new JRadioButton(q_options[i]);
      if (q_icons != null) {
	 button.setIcon(q_icons[i]);
	 button.setSelectedIcon(q_sicons[i]);
       }

      button.setOpaque(false);
      if (i == 0) button.setSelected(true);
      q_cons.gridy++;
      bg.add(button);
      q_buttons.add(button);
      add(button, q_cons);
    }

   int extra = 0;
   if (q_others) {
      extra = 30;
      JRadioButton button = new JRadioButton("Others: ");
      button.setOpaque(false);
      q_cons.gridy++;
      bg.add(button);
      q_buttons.add(button);
      add(button, q_cons);

      q_field = new SwingTextField();
      q_cons.ipady = TEXTFIELD_HEIGHT;
      q_cons.gridy++;
      add(q_field, q_cons);
    }

   setPreferredSize(new Dimension(MULTICHOICE_SIZE.width,
         MULTICHOICE_SIZE.height * q_buttons.size() + 90 + extra));
}


/********************************************************************************/
/*										*/
/*	Get Answer methods							*/
/*										*/
/********************************************************************************/

@Override String getAnswer()
{
   String answer = "";

   if (q_others && q_buttons.get(q_buttons.size() - 1).isSelected())
      answer = "Others: " + q_field.getText();
   else {
      for (int i = 0; i < q_buttons.size(); i++) {
	 if (q_buttons.get(i).isSelected()) {
	    answer = q_buttons.get(i).getText();
	  }
       }
    }

   if (q_required && answer.equals("")) return null;
   return answer;
}



/********************************************************************************/
/*										*/
/*	Reset methods								*/
/*										*/
/********************************************************************************/

@Override void reset()
{
   markFilled();
   q_buttons.get(0).setSelected(true);

   if (q_others) q_field.setText("");
}



}	// end of class BqstMultiChoices



/* end of BqstMultiChoices.java */
