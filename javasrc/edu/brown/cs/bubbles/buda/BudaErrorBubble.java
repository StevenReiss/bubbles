/********************************************************************************/
/*										*/
/*		BudaErrorBubble.java						*/
/*										*/
/*	Bubble for displaying error messages					*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Andrew Kovacs			*/
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

package edu.brown.cs.bubbles.buda;

import edu.brown.cs.bubbles.board.BoardColors;

import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;



public class BudaErrorBubble extends BudaBubble {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Color	text_color;


private static final long serialVersionUID = 1L;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public BudaErrorBubble(final String errmsg)
{
   text_color = BoardColors.getColor("Buda.ErrorTextColor");

   setContentPane(new ErrorContentPane(errmsg));
}



public BudaErrorBubble(final String errmsg,Color c)
{
   text_color = c;

   setContentPane(new ErrorContentPane(errmsg));
}



/********************************************************************************/
/*										*/
/*	Content pane								*/
/*										*/
/********************************************************************************/

private class ErrorContentPane extends JPanel
{
   private static final long serialVersionUID = 1L;

   private ErrorContentPane(String errmsg) {
      JLabel errlabel = new JLabel(errmsg);
      errlabel.setForeground(text_color);
      errlabel.addMouseListener(new QuitAction());
      add(errlabel);
    }

}	// end of inner class ErrorContentPane


private class QuitAction extends MouseAdapter {

   @Override public void mouseClicked(MouseEvent e) {
      setVisible(false);
    }

}	// end of inner calss QuitAction






}	// end of class BudaErrorBubble



/* end of BudaErrorBubble.java */
