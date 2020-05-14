/********************************************************************************/
/*										*/
/*		BqstConstants.java						*/
/*										*/
/*	Bubbles questions and forms constants					*/
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

import java.awt.Dimension;


/**
 * Interface defining constants for Bqst
 */

public interface BqstConstants {


/**
 * Background color of form dialog
 */
String BG_COLOR_PROP = "Bqst.Background";



/********************************************************************************/
/*										*/
/*	Types of Questions							*/
/*										*/
/********************************************************************************/

/**
 * Enumeration for different types of questions.
 */
enum QuestionType {
   LONGTEXT, SHORTTEXT, MULTICHOICE, CHECKBOX, HIDDEN
};



/********************************************************************************/
/*										*/
/*	Constants for component size						*/
/*										*/
/********************************************************************************/

/**
 *  Size of form dialog
 */
int	  FORM_WIDTH	   = 400;
int	  FORM_HEIGHT	   = 500;
Dimension FORM_SIZE	= new Dimension(FORM_WIDTH,FORM_HEIGHT);


/**
 *  Height of JTextArea
 */
int	  TEXTAREA_HEIGHT  = 80;

/**
 *  Height of JTextfield
 */
int	  TEXTFIELD_HEIGHT = 8;

/**
 *  Size of multi-choice question panel
 */
Dimension MULTICHOICE_SIZE = new Dimension(330,20);

/**
 *  Size of long text question panel
 */
Dimension LONGTEXT_SIZE    = new Dimension(330,180);

/**
 *  Size of short text question panel
 */
Dimension SHORTTEXT_SIZE   = new Dimension(330,100);

/**
 *  Size of check-box question panel
 */
Dimension CHECKBOX_SIZE    = new Dimension(330,20);

/**
 *  Size of submit button panel
 */
Dimension BUTTONPANE_SIZE  = new Dimension(330,60);



/********************************************************************************/
/*										*/
/*	Language settings for form						*/
/*										*/
/********************************************************************************/

/**
 *  Form instruction
 */
String	  INSTRUCTION_TEXT = "<html>Please complete the form below. Required fields marked<FONT color=red>*</FONT></html>";

/**
 *  Thank you message after submitting
 */
String	  THANKS_TEXT	   = "<html><font color=red>Thank you! Your form has been submitted.</font></html>";

/**
 *  Required fielded unfilled message
 */
String	  UNFILLED_TEXT    = "<html><font color=red>You have questions that still needs to be filled out.</font></html>";


}	// end of interface BqstConstants




/* end of BqstConstants.java */
