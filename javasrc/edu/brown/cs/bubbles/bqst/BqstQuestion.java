/********************************************************************************/
/*										*/
/*		BqstQuestion.java						*/
/*										*/
/*	Bubbles questions and forms generic widget implementation		*/
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

import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;


/**
 * This class is abstract class for different types of question
 **/

abstract class BqstQuestion extends JPanel implements BqstConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		q_title;
private String		q_help;
protected boolean	q_required	 = true;
private JLabel		q_label;
protected GridBagConstraints q_cons;

private static final long    serialVersionUID = 1L;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected BqstQuestion(String qst,QuestionType qt,String help)
{
   q_title = qst;
   q_help = help;
   q_cons = new GridBagConstraints();
   q_cons.gridx = 0;
   q_cons.gridy = 0;
   q_cons.weightx = 20;
   q_cons.fill = GridBagConstraints.HORIZONTAL;
}



/********************************************************************************/
/*										*/
/*	Help methods for setting up						*/
/*										*/
/********************************************************************************/

/**
 * This method should be overridden
 **/
void setup()
{
   this.setLayout(new GridBagLayout());
   this.setOpaque(false);
   addQuestionText();
   addHelpText();
}



/**
 * create label for question
 **/
private void addQuestionText()
{
   String labeltext = "";
   if (q_required) labeltext = "<html>" + q_title + "<FONT color=red>*</FONT></html>";
   else labeltext = "<html>" + q_title + "</html>";
   q_label = new JLabel(labeltext);
   q_cons.gridy++;
   this.add(q_label, q_cons);
}



/**
 * create label for help message
 **/
private void addHelpText()
{
   if (q_help != null) {
      JLabel helpmsg = new JLabel("<html><FONT COLOR=GRAY>" + q_help + "</FONT></html>");
      q_cons.gridy++;
      this.add(helpmsg, q_cons);
    }
}

/**
 * reset method
 **/
abstract void reset();



/********************************************************************************/
/*										*/
/*	Set and get methods							*/
/*										*/
/********************************************************************************/

/**
 * Set whether this question is required or not
 **/
void setRequired(boolean isrequired)
{
   q_required = isrequired;
}

/**
 * Get question title
 **/
String getTitle()
{
   return q_title;
}

/**
 * Get question answer
 **/
abstract String getAnswer();




/********************************************************************************/
/*										*/
/*	Methods for changing the label color					*/
/*										*/
/********************************************************************************/

void markUnfilled()
{
   if (q_label != null && q_required)
      q_label.setText("<html><font color=red>" + q_title + "*</font></html>");
}



void markFilled()
{
   if (q_label != null && q_required)
      q_label.setText("<html>" + q_title + "<font color=red>*</font></html>");
}


}	// end of BqstQuestion



/* end of BqstQuestion.java */
