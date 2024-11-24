/********************************************************************************/
/*										*/
/*		BqstCheckBoxes.java						*/
/*										*/
/*	Bubbles questions and forms check box implementation			*/
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

import javax.swing.JCheckBox;
import javax.swing.JTextField;

import edu.brown.cs.ivy.swing.SwingTextField;

import java.awt.Dimension;
import java.util.ArrayList;


/**
 * This class is subclass of BqstQuestion, and it implements a check box question.
 **/

class BqstCheckBoxes extends BqstQuestion implements BqstConstants {




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private String[]	     q_options;
private ArrayList<JCheckBox> q_buttons;
private boolean 	     q_others;
private JTextField	     q_field;

private static final long    serialVersionUID = 1L;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected BqstCheckBoxes(String qst,String help,String[] options,boolean required,
			    boolean others)
{
   super(qst,QuestionType.CHECKBOX,help);
   setRequired(required);
   q_options = options;
   q_buttons = new ArrayList<JCheckBox>();
   q_others = others;
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

@Override void setup()
{
   super.setup();
   for (int i = 0; i < q_options.length; i++) {
      JCheckBox cb = new JCheckBox(q_options[i]);
      cb.setOpaque(false);
      q_cons.gridy++;
      q_buttons.add(cb);
      add(cb, q_cons);
    }

   int extra = 0;
   if (q_others) {
      extra = 30;
      JCheckBox cb = new JCheckBox("Others: ");
      cb.setOpaque(false);
      q_cons.gridy++;
      q_buttons.add(cb);
      add(cb, q_cons);

      q_field = new SwingTextField();
      q_cons.ipady = TEXTFIELD_HEIGHT;
      q_cons.gridy++;
      add(q_field, q_cons);
    }

   setPreferredSize(new Dimension(CHECKBOX_SIZE.width,CHECKBOX_SIZE.height *
				     q_buttons.size() + 90 + extra));
}



/********************************************************************************/
/*										*/
/*	Get answer methods							*/
/*										*/
/********************************************************************************/

@Override String getAnswer()
{
   String answer = "";

   for (int i = 0; i < q_buttons.size(); i++) {
      if (q_buttons.get(i).isSelected()) {
	 answer += q_buttons.get(i).getText() + " ";
       }
    }

   if (q_others && q_buttons.get(q_buttons.size() - 1).isSelected())
      answer += q_field .getText();

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
   for (int i = 0; i < q_buttons.size(); i++) {
      q_buttons.get(i).setSelected(false);
    }
   if (q_others) q_field.setText("");
}



} // end of class BqstCheckBoxes




/* end of BqstCheckBoxes.java */
