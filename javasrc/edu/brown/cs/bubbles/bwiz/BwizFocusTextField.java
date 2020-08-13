/********************************************************************************/
/*										*/
/*		BwizFocusTextField.java 					*/
/*										*/
/*	Text field that selects all of contents when focused			*/
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
import edu.brown.cs.ivy.swing.SwingTextField;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;



class BwizFocusTextField extends SwingTextField
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private boolean 	showing_hint;
private String		hint_text;



/********************************************************************************/
/*										*/
/*	Static creation methods 						*/
/*										*/
/********************************************************************************/

//Creates a BwizFocusTextField with the default styling and the parameter text as the tooltip

static BwizFocusTextField getStyledField(String text)
{
   return getStyledField(text, text);
}



//Creates a BwizFocusTextField with the default styling and a tooltip as specified

static BwizFocusTextField getStyledField(String text, String tooltip)
{
   BwizFocusTextField field = new BwizFocusTextField("",text);
   field.setToolTipText(tooltip);

   return field;
}






/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BwizFocusTextField()
{
   showing_hint = false;
   hint_text = null;

   addListener();
}



BwizFocusTextField(String text)
{
   super(text);

   showing_hint = false;
   hint_text = null;

   addListener();
}



BwizFocusTextField(String text,String hint)
{
   this();

   hint_text = hint;

   if (text != null) {
      setText(text);
      showing_hint = false;
    }
   else {
      setText(hint);
      showing_hint = true;
    }
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

private void addListener()
{
   this.addFocusListener(new Focuser());
}


private void focusSelection()
{
   selectAll();
}



/********************************************************************************/
/*										*/
/*	Hint methods								*/
/*										*/
/********************************************************************************/

@Override public String getText()
{
   if (showing_hint) return "";
   return super.getText();
}





/********************************************************************************/
/*										*/
/*	Focus manager								*/
/*										*/
/********************************************************************************/

private class Focuser extends FocusAdapter {

   @Override public void focusGained(FocusEvent e) {
      if (hint_text != null) {
         if (showing_hint) {
            BwizFocusTextField.super.setText("");
            showing_hint = false;
          }
         setForeground(BoardColors.getColor("Bwiz.FocusTextColor"));
       }
      else {
         setForeground(BoardColors.getColor("Bwiz.FocusTextColor"));
         focusSelection();
       }
    }

   @Override public void focusLost(FocusEvent e) {
      if (BwizFocusTextField.super.getText().isEmpty()) {
         BwizFocusTextField.super.setText(hint_text);
         showing_hint = true;
         setForeground(BoardColors.getColor("Bwiz.NoFocusTextColor"));
       }
      else showing_hint = false;
    }

}	// end of inner class Focuser




}	// end of class BwizFocusTextField



/* end of BwizFocusTextField.java */
