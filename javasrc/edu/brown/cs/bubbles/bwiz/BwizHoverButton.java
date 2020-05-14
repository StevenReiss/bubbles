/********************************************************************************/
/*										*/
/*		BwizHoverButton.java						*/
/*										*/
/*	Button with hover capabilities						*/
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

import javax.swing.JButton;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;


class BwizHoverButton extends JButton implements MouseListener
{

/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected Color text_color;
protected Color hover_color;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BwizHoverButton()
{
   super();

   text_color = getForeground();
   setHoverColor(text_color);
   addMouseListener(this);
}

//Create a button that changes text color when the mouse is over it
BwizHoverButton(String text, Color color, Color hoverColor)
{
   super(text);

   setTextColor(color);
   setHoverColor(hoverColor);
   addMouseListener(this);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

Color getHoverColor()				{ return hover_color; }

void setHoverColor(Color color) 		{ hover_color = color; }

Color getTextColor()				{ return text_color; }


void setTextColor(Color color)
{
   text_color = color;
   setForeground(text_color);
}



/********************************************************************************/
/*										*/
/*	Mouse handling								*/
/*										*/
/********************************************************************************/

@Override public void mouseClicked(MouseEvent e)		{ }

@Override public void mousePressed(MouseEvent e)		{ }

@Override public void mouseReleased(MouseEvent e)		{ }

@Override public void mouseEntered(MouseEvent e)
{
   if (e.getSource() == this) {
      setForeground(hover_color);
    }
}


@Override public void mouseExited(MouseEvent e)
{
   if (e.getSource() == this) {
      setForeground(text_color);
    }
}



}	// end of class BwizHoverButton




/* end of BwizHoverButton.java */
