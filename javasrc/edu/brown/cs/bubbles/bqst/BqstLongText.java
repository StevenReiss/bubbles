/********************************************************************************/
/*										*/
/*		BqstLongText.java						*/
/*										*/
/*	Bubbles questions and forms long text field implementation		*/
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

import javax.swing.JScrollPane;
import javax.swing.JTextArea;


/**
 * This class is subclass of BqstQuestion, and it implements a long text question.
 **/

class BqstLongText extends BqstQuestion implements BqstConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private JTextArea	 longtext_area;

private static final long serialVersionUID = 1L;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected BqstLongText(String qst,String help,boolean required)
{
   super(qst,QuestionType.LONGTEXT,help);
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
   longtext_area = new JTextArea();
   longtext_area.setLineWrap(true);
   longtext_area.setWrapStyleWord(true);
   JScrollPane sp = new JScrollPane(longtext_area);
   q_cons.ipady = TEXTAREA_HEIGHT;
   q_cons.gridy++;
   this.add(sp, q_cons);
   this.setPreferredSize(LONGTEXT_SIZE);
}



/********************************************************************************/
/*										*/
/*	Get Answer methods							*/
/*										*/
/********************************************************************************/

@Override String getAnswer()
{
   if (q_required && longtext_area.getText().equals("")) return null;
   return longtext_area.getText();
}



/********************************************************************************/
/*										*/
/*	Reset methods								*/
/*										*/
/********************************************************************************/

@Override void reset()
{
   markFilled();
   longtext_area.setText("");
}



}	// end of class BqstLongText




/* end of BqstLongText.java */
