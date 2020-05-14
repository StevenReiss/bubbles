/********************************************************************************/
/*										*/
/*		BqstHiddenField.java						*/
/*										*/
/*	Bubbles questions and forms hidden field implementation 		*/
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



/**
 * This class is subclass of BqstQuestion.
 * It implements a hidden question, which is invisible to user but visible in output file.
 **/

class BqstHiddenField extends BqstQuestion implements BqstConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private String	    q_hiddenmsg;

private static final long serialVersionUID = 1L;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected BqstHiddenField(String qst,String msg)
{
   super(qst,QuestionType.HIDDEN,null);
   q_hiddenmsg = msg;
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

@Override void setup()
{}



/********************************************************************************/
/*										*/
/*	Get Answer methods							*/
/*										*/
/********************************************************************************/

@Override String getAnswer()
{
   return q_hiddenmsg;
}



/********************************************************************************/
/*										*/
/*	Reset methods								*/
/*										*/
/********************************************************************************/

@Override void reset()
{}



}	// end of class BqstHiddenField




/* end of BqstHiddenField.java */
