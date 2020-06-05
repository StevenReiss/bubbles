/********************************************************************************/
/*										*/
/*		BqstShortText.java						*/
/*										*/
/*	Bubbles questions and forms short text field implementation		*/
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

import javax.swing.JTextField;

import edu.brown.cs.ivy.swing.SwingTextField;




/**
 * This class is subclass of BqstQuestion, and it implements a short text question.
 **/
class BqstShortText extends BqstQuestion implements BqstConstants {




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private JTextField	shorttext_field;

private static final long serialVersionUID = 1L;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected BqstShortText(String qst,String help,boolean required)
{
   super(qst,QuestionType.SHORTTEXT,help);
   this.setRequired(required);
}




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

@Override void setup()
{
   super.setup();
   shorttext_field = new SwingTextField();
   q_cons.ipady = TEXTFIELD_HEIGHT;
   q_cons.gridy++;
   this.add(shorttext_field, q_cons);
   this.setPreferredSize(SHORTTEXT_SIZE);
}




/********************************************************************************/
/*										*/
/*	Get Answer methods							*/
/*										*/
/********************************************************************************/

@Override String getAnswer()
{
   if (q_required && shorttext_field.getText().equals("")) return null;
   return shorttext_field.getText();
}



/********************************************************************************/
/*										*/
/*	Reset methods								*/
/*										*/
/********************************************************************************/

@Override void reset()
{
   markFilled();
   shorttext_field.setText("");
}


} // end of class BqstShortText




/* end of BqstShortText.java */
