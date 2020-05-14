/********************************************************************************/
/*										*/
/*		BtedFindBar.java						*/
/*										*/
/*	Bubble Environment text editor find bar 				*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Adam M. Cook, Steven P. Reiss	*/
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


package edu.brown.cs.bubbles.bted;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardLog;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.Segment;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class BtedFindBar extends JPanel implements ActionListener, DocumentListener,
	 BtedConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private JTextField		   search_field;
private Highlighter		   search_highlighter;
private Highlighter.HighlightPainter highlight_paint;
private JEditorPane		  for_editor;
private JButton 		  exit_button;
private JButton 		  prev_button;
private JButton 		  next_button;
private int			  current_index;

private static final long	    serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructores								*/
/*										*/
/********************************************************************************/

BtedFindBar(JEditorPane editor)
{
   for_editor = editor;
   exit_button = new JButton(BoardImage.getIcon("cancel.png"));
   prev_button = new JButton(BoardImage.getIcon("back.png"));
   next_button = new JButton(BoardImage.getIcon("next.png"));

   exit_button.setMargin(BUTTON_MARGIN);
   prev_button.setMargin(BUTTON_MARGIN);
   next_button.setMargin(BUTTON_MARGIN);

   exit_button.addActionListener(this);
   prev_button.addActionListener(this);
   next_button.addActionListener(this);

   search_field = new JTextField(10);
   search_field.setEditable(true);
   Dimension size = search_field.getPreferredSize();
   search_field.setSize(size);
   search_field.setMinimumSize(size);
   search_field.getDocument().addDocumentListener(this);

   this.add(exit_button);
   this.add(prev_button);
   this.add(next_button);
   this.add(search_field);

   search_highlighter = new DefaultHighlighter();
   highlight_paint = new DefaultHighlighter.DefaultHighlightPainter(
      BoardColors.getColor(HIGHLIGHT_COLOR));
   for_editor.setHighlighter(search_highlighter);

   current_index = 0;
}




/********************************************************************************/
/*										*/
/*	Search action methods							*/
/*										*/
/********************************************************************************/

/**
 * Searches for the next item in the editor
 * @param mode - determines which way to search.
 * Legal parameters are:
 * SearchMode.NEXT
 * SearchMode.PREVIOUS_ITEM
 * SearchMode.FIRST_ITEM
 */

void search(SearchMode mode)
{
   search_highlighter.removeAllHighlights();

   String searchtext = search_field.getText();
   if (searchtext.length() <= 0) {
      return;
    }

   doSearch(mode,searchtext);

   if (current_index >= 0) {
      search_field.setBackground(BoardColors.getColor("Bted.FindBackground"));
      try {
	 int end = current_index + searchtext.length();
	 search_highlighter.addHighlight(current_index, end, highlight_paint);
	 for_editor.setCaretPosition(end);
       }
      catch (BadLocationException e) {
	 e.printStackTrace();
       }
    }
   else {
      search_field.setBackground(BoardColors.getColor(NOT_FOUND_COLOR));
    }

}



@Override public void actionPerformed(ActionEvent e)
{
   if (e.getSource() == exit_button) {
      this.setVisible(false);
    }
   else if (e.getSource() == next_button) {
      this.search(SearchMode.NEXT);
    }
   else if (e.getSource() == prev_button) {
      this.search(SearchMode.PREVIOUS);
    }
}



/********************************************************************************/
/*										*/
/*	Search methods								*/
/*										*/
/********************************************************************************/

private void doSearch(SearchMode mode,String txt)
{
   int offset = 0;
   Segment s = new Segment();
   Document d = for_editor.getDocument();
   int len = d.getLength();
   int d0 = 0;
   int d1 = 1;
   int d2 = len;

   try {
      switch (mode) {
	 case NEXT :
	    offset = current_index+1;
	    d.getText(offset,len-offset,s);
	    d2 = s.length();
	    break;
	 case PREVIOUS :
	    d.getText(0,current_index,s);
	    d0 = s.length()-1;
	    d1 = -1;
	    d2 = -1;
	    break;
	 case FIRST :
	    d.getText(0,len,s);
	    break;
       }
    }
   catch (BadLocationException e) {
      BoardLog.logE("BTED","Problem getting search text: " + e);
      current_index = -1;
      return;
    }

   if (s.length() < txt.length()) {
      current_index = -1;
      return;
    }

   for (int i = d0; i != d2; i += d1) {
      boolean fnd = true;
      for (int j = 0; fnd && j < txt.length(); ++j) {
	 if (s.charAt(i+j) != txt.charAt(j)) fnd = false;
       }
      if (fnd) {
	 current_index = i + offset;
	 return;
       }
    }

   current_index = -1;
}



/********************************************************************************/
/*										*/
/*	Callbacks on document events for search bar				*/
/*										*/
/********************************************************************************/

@Override public void changedUpdate(DocumentEvent e)
{
   this.search(SearchMode.FIRST);
}

@Override public void insertUpdate(DocumentEvent e)
{
   this.search(SearchMode.FIRST);
}

@Override public void removeUpdate(DocumentEvent e)
{
   this.search(SearchMode.FIRST);
}


@Override public void grabFocus()
{
   search_field.grabFocus();
}




}	// end of class BtedFindBar




/* end of BtedFindBar.java */
