/********************************************************************************/
/*										*/
/*		BwizNewWizard.java						*/
/*										*/
/*	Text field that handles verification as you type			*/
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

import edu.brown.cs.ivy.swing.SwingEventListenerList;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import java.awt.Color;




class BwizVerifiedTextField extends BwizFocusTextField
{



/********************************************************************************/
/*										*/
/*	Private Data								*/
/*										*/
/********************************************************************************/

private BwizConstants.IVerifier verifier;
private boolean previous_failure;
private Color normal_color;
private SwingEventListenerList<BwizConstants.VerificationListener> verification_listeners;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

static BwizVerifiedTextField getStyledField(String text, String tooltip,
					       BwizConstants.IVerifier verifier)
{
   BwizVerifiedTextField field=new BwizVerifiedTextField(text, verifier);

   if (tooltip=="")
      field.setToolTipText(text);
   else
      field.setToolTipText(tooltip);

   return field;
}




BwizVerifiedTextField()
{
   this("", null);
}



BwizVerifiedTextField(String text)
{
   this(text, null);
}



BwizVerifiedTextField(String text, BwizConstants.IVerifier v)
{
   super(text);
   previous_failure = false;
   verifier=v;
   verification_listeners=new SwingEventListenerList<BwizConstants.VerificationListener>(BwizConstants.VerificationListener.class);

   setupVerification();
}



/********************************************************************************/
/*										*/
/*	Verification methods							*/
/*										*/
/********************************************************************************/

void setVerifier(BwizConstants.IVerifier v)
{
   verifier=v;
}



BwizConstants.IVerifier getVerifier()
{
   return verifier;
}



void verify()
{
   if (verify(getText())) {
      verificationSuccess();
    }
   else {
      verificationFailure();
    }
}


private void setupVerification()
{
   getDocument().addDocumentListener(new DocHandler());
}



private boolean verify(String input)
{
   if (verifier!=null)
      return verifier.verify(input);
   else
      return true;
}



private void verificationFailure()
{
   //Currently colors the text red
   if (!previous_failure) {
      normal_color = this.getForeground();
    }

   this.setForeground(BoardColors.getColor("Bwiz.TextFieldError"));
   previous_failure=true;

   fireVerificationFailure();
}



private void verificationSuccess()
{
   //Currently colors the text back to normalColor
   this.setForeground(normal_color);
   previous_failure=false;

   fireVerificationSuccess();
}



private class DocHandler implements DocumentListener {

   @Override public void insertUpdate(DocumentEvent e) {
      try {
	 Document doc = e.getDocument();
	 String data = doc.getText(0, doc.getLength());
	
	 if (!verify(data)) {
	    //Show some message or UI
	    verificationFailure();
	  }
	 else
	    verificationSuccess();
       }
      catch(BadLocationException ex) {}
    }

   @Override public void changedUpdate(DocumentEvent e) {
      try {
	 Document doc = e.getDocument();
	 String data = doc.getText(0, doc.getLength());
	
	 if (!verify(data)) {
	    //Show some message or UI
	    verificationFailure();
	  }
	 else
	    verificationSuccess();
       }
      catch(BadLocationException ex) {}
    }

   @Override public void removeUpdate(DocumentEvent e) {
      try {
	 Document doc = e.getDocument();
	 String data = doc.getText(0, doc.getLength());
	
	 if (!verify(data)) {
	    //Show some message or UI
	    verificationFailure();
	  }
	 else
	    verificationSuccess();
       }
      catch(BadLocationException ex) {}
    }

}	// end of inner class DocHandler




/********************************************************************************/
/*										*/
/*	Listener management							*/
/*										*/
/********************************************************************************/

void addVerificationListener(BwizConstants.VerificationListener listener)
{
   verification_listeners.add(listener);
}

private void fireVerificationSuccess()
{
   for (BwizConstants.VerificationListener listener : verification_listeners)
      {
      listener.verificationEvent(this, true);
    }
}

private void fireVerificationFailure()
{
   for (BwizConstants.VerificationListener listener : verification_listeners)
      {
      listener.verificationEvent(this, false);
    }
}



}	// end of class BwizVerifiedTextField


/* end of BwizVerifiedTextField.java */
