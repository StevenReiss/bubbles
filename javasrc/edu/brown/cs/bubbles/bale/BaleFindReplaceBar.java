/********************************************************************************/
/*										*/
/*		BaleFindReplaceBar.java 					*/
/*										*/
/*	Bubble Annotated Language Editor Fragment editor find bar		*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Arman Uguray 		      */
/*	Copyright 2012 Brown University -- Steven Reiss 		      */
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bale;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.bowi.BowiFactory;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.burp.BurpHistory;

import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.swing.SwingText;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.Position;
import javax.swing.text.Segment;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;


class BaleFindReplaceBar extends SwingGridPanel implements BaleConstants, BaleConstants.BaleFinder,
	ActionListener, CaretListener, ItemListener
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BaleEditorPane editor_pane;
private BaleDocument for_document;
private JTextField text_field;
private JTextField replace_field;
private JCheckBox is_case_sensitive; // toggles whether the search should be case sensitive
private JLabel number_label; // shows how many occurences of the search text have been found

private String	search_for;
private String searched_for; // stores the most recent search text. used to determine whether it is necessary to run a new search
private List<Position> occurrences_set; // stores the locations of occurrences of the search text.
private int current_index; // stores which occurrence was last highlighted - used to facilitate the arrow functions
private int current_caret_position;
private Highlighter my_highlighter;
private Object my_highlight_tag;
private int last_dir;

private JPanel replace_panel;
private Dimension find_size;
private Dimension replace_size;


private static Icon cancel_icon;
private static Icon next_icon;
private static Icon prev_icon;
private static Icon repl_icon;
private static Icon replall_icon;

static {
   cancel_icon = BoardImage.getIcon("button_cancel",10,10);
   next_icon = BoardImage.getIcon("2dowarrow",10,10);
   prev_icon = BoardImage.getIcon("2uparrow",10,10);
   repl_icon = BoardImage.getIcon("replace",10,10);
   replall_icon = BoardImage.getIcon("replaceall",10,10);
}

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleFindReplaceBar(BaleEditorPane edt,boolean dorep)
{
   setOpaque(true);
   BoardColors.setColors(this,"Bale.FindReplaceTopColor");
   setBorder(new LineBorder(BoardColors.getColor("Bale.FindReplaceBorder"), 1, true));
   setInsets(0);

   editor_pane = edt;
   for_document = (BaleDocument) edt.getDocument();
   search_for = null;
   searched_for = null;
   occurrences_set = null;
   current_index = 0;
   last_dir = 1;
   current_caret_position = edt.getCaretPosition();

   SwingGridPanel topbox = new SwingGridPanel();

   text_field = createTextField(10);
   text_field.setAction(new SearchAction());
   topbox.addGBComponent(text_field,0,0,1,1,10,0);

   JButton b2 = createButton("Prev",prev_icon,"LAST");
   topbox.addGBComponent(b2,1,0,1,1,0,0);

   JButton b3 = createButton("Next",next_icon,"NEXT");
   topbox.addGBComponent(b3,2,0,1,1,0,0);

   JButton b1 = createButton(null,cancel_icon,"DONE");
   topbox.addGBComponent(b1,3,0,1,1,0,0);

   addGBComponent(topbox,0,0,0,1,10,0);

   SwingGridPanel bottombox = new SwingGridPanel();

   is_case_sensitive = new JCheckBox("Case Sensitive?");
   is_case_sensitive.addItemListener(this);
   is_case_sensitive.setSelected(true);
   is_case_sensitive.setHorizontalTextPosition(SwingConstants.LEFT);
   is_case_sensitive.setBorder(null);
   is_case_sensitive.setFocusPainted(false);
   is_case_sensitive.setRolloverEnabled(false);
   bottombox.addGBComponent(is_case_sensitive,0,0,1,1,0,0);

   JLabel spacer = new JLabel();
   bottombox.addGBComponent(spacer,1,0,1,1,10,0);

   number_label = new JLabel("Matches: ??");
   bottombox.addGBComponent(number_label,2,0,1,1,0,0);

   addGBComponent(bottombox,0,1,0,1,10,0);

   SwingGridPanel replacebox = new SwingGridPanel();

   replace_field = createTextField(10);
   replace_field.setAction(new ReplaceAction());
   replacebox.addGBComponent(replace_field,0,0,1,1,10,0);

   JButton b4 = createButton("Repl",repl_icon,"REPL");
   replacebox.addGBComponent(b4,1,0,1,1,0,0);

   JButton b5 = createButton("ReplAll",replall_icon,"REPLALL");
   replacebox.addGBComponent(b5,2,0,1,1,0,0);

   addGBComponent(replacebox,0,2,0,1,10,0);
   replace_panel = replacebox;

   replace_size = getPreferredSize();
   replace_panel.setVisible(false);
   find_size = getPreferredSize();

   my_highlighter = editor_pane.getHighlighter();
   try {
      my_highlight_tag = my_highlighter.addHighlight(0, 0, BaleHighlightContext.getPainter(BaleHighlightType.FIND));
    }
   catch (BadLocationException e) {
      my_highlight_tag = new Object();
      BoardLog.logE("BALE","Problem creating highlight tag",e);
    }

   editor_pane.addCaretListener(new HighlightCanceler());

   setReplace(dorep);
}



private JButton createButton(String txt,Icon icn,String cmd)
{
   JButton btn = new JButton(txt,icn);
   btn.setActionCommand(cmd);
   btn.addActionListener(this);
   btn.setBorder(null);
   btn.setFocusPainted(false);
   btn.setContentAreaFilled(false);

   if (txt == null) {
      Dimension dim = new Dimension(icn.getIconWidth() + 2,icn.getIconHeight());
      btn.setSize(dim);
      btn.setPreferredSize(dim);
      btn.setMaximumSize(dim);
    }

   return btn;
}



private JTextField createTextField(int ln)
{
   JTextField tfld = new JTextField(ln);
   tfld.setFont(BALE_PROPERTIES.getFont(BALE_CRUMB_FONT));
   tfld.addCaretListener(this);
   tfld.addKeyListener(new CloseListener());
   // Dimension sz = tfld.getPreferredSize();
   // tfld.setMaximumSize(sz);
   // tfld.setPreferredSize(sz);

   return tfld;
}




/********************************************************************************/
/*										*/
/*	Activate methods							*/
/*										*/
/********************************************************************************/

@Override public Component getComponent()		{ return this; }


@Override public void setVisible(boolean fg)
{
   super.setVisible(fg);

   if (fg) {
      text_field.requestFocus();
    }
   else {
      if (getParent() != null && !(getParent() instanceof BudaBubbleArea)) getParent().setVisible(false);
      editor_pane.requestFocus();
    }
}


@Override public void setReplace(boolean fg)
{
   replace_panel.setVisible(fg);

   Dimension sz = (fg ? replace_size : find_size);
   setMaximumSize(sz);
   setMinimumSize(sz);
   setPreferredSize(sz);
   setSize(sz);
}



/********************************************************************************/
/*										*/
/*	Search routines 							*/
/*										*/
/********************************************************************************/

@Override public void find(int dir,boolean next)
{
   if (BudaRoot.findBudaBubble(editor_pane) == null) {
      BudaBubble my_bub = BudaRoot.findBudaBubble(this);
      if (my_bub != null) my_bub.setVisible(false);
      return;
    }
   BaleElement currentelement = null;
   for_document.baleWriteLock();
   try {
      BowiFactory.startTask();
      last_dir = dir == 0 ? last_dir : dir;
      // if the text field is empty then don't search but also reset the look
      if (search_for == null || search_for.length() == 0) {
	 clearHighlights();
	 searched_for = null;
	 return;
       }
      // if the user changed the text then run a new search
      if (searched_for == null || (is_case_sensitive.isSelected() && !search_for.equals(searched_for))
	     || (!is_case_sensitive.isSelected() && !search_for.equalsIgnoreCase(searched_for))
	     || current_caret_position != editor_pane.getCaretPosition()) {
	 clearHighlights();
	 searched_for = search_for;
	 // find and store the indices of all the occurrences so that going back and forth doesn't require a new search
	 findAllOccurences(search_for, dir);
	 number_label.setText("Matches: " + occurrences_set.size());
	 //current_index = -1;
       }
      if (occurrences_set == null || occurrences_set.size() == 0) {
	 clearHighlights();
	 return;
       }

      // depending on the find direction, either navigate to the next or previous occurrence.
      // wrap around if necessary
      int found = 0;
      if (dir > 0) {
	 current_index++;
	 if (current_index >= occurrences_set.size()) current_index = 0;
       }
      else if (dir < 0) {
	 current_index--;
	 if (current_index < 0) current_index = occurrences_set.size() - 1;
       }
      else if (dir == 0) {
	 current_index = 0;
       }
      found = occurrences_set.get(current_index).getOffset();
      int len = search_for.length();

      try {
	 current_caret_position = found+len;
	 editor_pane.setCaretPosition(found);
	 editor_pane.moveCaretPosition(found+len);
	 my_highlighter.changeHighlight(my_highlight_tag,found,found+len);
	 // Rectangle r = new Rectangle();
	 // r.setFrame(editor_pane.modelToView2D(found+len));
	 // editor_pane.scrollRectToVisible(r);
	 editor_pane.scrollRectToVisible(SwingText.modelToView2D(editor_pane,found+len));
	 currentelement = for_document.getCharacterElement(found);
	 if (currentelement == null) return;
	 BoardMetrics.noteCommand("BALE","Find");

	 if (currentelement.isElided()){
	    currentelement.setElided(false);
	    for_document.handleElisionChange();
	    editor_pane.increaseSizeForElidedElement(currentelement);
	    BoardMetrics.noteCommand("BALE","FindUnElision");
	    BaleEditorBubble.noteElision(editor_pane);
	  }
       }
      catch (BadLocationException e) { }
    }
   finally {
      for_document.baleWriteUnlock();
      BowiFactory.stopTask();
    }
}



private void findAllOccurences(String text, int dir)
{
   try {
      int carpos = editor_pane.getCaretPosition();
      int soff = 0;
      int eoff = for_document.getLength();
      int len = text.length();
      List<Position> occurrences = new ArrayList<Position>();
      int tlen = eoff-soff;
      try {
	 boolean search = true;
	 Segment segment = new Segment();
	 Position found;
	 Position bestfound = for_document.createPosition(0);
	 int bestdist = for_document.getLength();
	 while (search) {
	    for_document.getText(soff,tlen,segment);
	    int finalloc = tlen-len;
	    if (finalloc < 0) break;
	    for (int i = 0; i <= tlen-len; ++i) {
	       boolean fnd = true;
	       for (int j = 0; fnd && j < len; ++j) {
		  char x = segment.charAt(i+j);
		  char y = search_for.charAt(j);
		  if (is_case_sensitive.isSelected()) fnd = x == y;
		  else{
		     x = Character.toLowerCase(x);
		     y = Character.toLowerCase(y);
		     fnd = x == y;
		   }
		}
	       if (fnd) {
		  found = for_document.createPosition(i+soff);
		  occurrences.add(found);
		  soff = found.getOffset() + len;
		  if (found.getOffset() - carpos < bestdist && found.getOffset() - carpos > 0 && dir > 0) {
		     bestfound = found;
		     bestdist = found.getOffset() - carpos;
		   }
		  else if (carpos - soff < bestdist && carpos - soff > 0 && dir < 0) {
		     bestfound = found;
		     bestdist = carpos - soff;
		   }
		  tlen = eoff - soff;
		  break;
		}
	       else if (i >= tlen-len) {
		  search = false;
		}
	     }
	  }
	 occurrences_set = occurrences;
	 if (dir == 0 || bestfound.getOffset() == 0)  current_index = -1;
	 else if (dir > 0) current_index = occurrences_set.indexOf(bestfound)-1;
	 else current_index = occurrences_set.indexOf(bestfound)+1;
       }
      catch (BadLocationException e) {
	 BoardLog.logE("BALE","Problem with search: " + e);
       }
    }
   finally {}
}


private void replace()
{
   if (occurrences_set == null || occurrences_set.isEmpty()) return;
   if (current_index < 0 || current_index >= occurrences_set.size()) return;
   if (search_for == null) return;

   String s = replace_field.getText();
   if (s == null) s = "";

   Position p = occurrences_set.remove(current_index);
   clearHighlights();
   int ln = search_for.length();
   BurpHistory bh = BurpHistory.getHistory();

   for_document.baleWriteLock();
   try {
      bh.beginEditAction(editor_pane);
      for_document.replace(p.getOffset(),ln,s,null);
    }
   catch (BadLocationException e) {
      BoardLog.logE("BALE","Problem with replace",e);
      return;
    }
   finally {
      bh.endEditAction(editor_pane);
      for_document.baleWriteUnlock();
    }

   --current_index;
   find(1,true);

}


private void replaceAll()
{
   if (occurrences_set == null || occurrences_set.isEmpty()) return;
   if (current_index < 0 || current_index >= occurrences_set.size()) return;
   if (search_for == null) return;

   String s = replace_field.getText();
   if (s == null) s = "";

   clearHighlights();
   int ln = search_for.length();

   BurpHistory bh = BurpHistory.getHistory();
   for_document.baleWriteLock();
   try {
      bh.beginEditAction(editor_pane);
      while (!occurrences_set.isEmpty()) {
	 int idx = occurrences_set.size();
	 Position p = occurrences_set.remove(idx-1);
	 try {
	    for_document.replace(p.getOffset(),ln,s,null);
	  }
	 catch (BadLocationException e) {
	    BoardLog.logE("BALE","Problem with replaceall",e);
	  }
       }
    }
   finally {
      bh.endEditAction(editor_pane);
      for_document.baleWriteUnlock();
    }

}


private void clearHighlights()
{
   if (my_highlighter == null) return;	// can be called before constructor completes
   if (my_highlight_tag == null) return;

   try {
      my_highlighter.changeHighlight(my_highlight_tag, 0, 0);
    }
   catch (BadLocationException ble) {}
   catch (NullPointerException e) { }
}



/********************************************************************************/
/*										*/
/*	Action Handlers 							*/
/*										*/
/********************************************************************************/

@Override public void actionPerformed(ActionEvent e)
{
   String cmd = e.getActionCommand();

   if (cmd.equals("DONE")) {
      try {
	 my_highlighter.changeHighlight(my_highlight_tag, 0, 0);
       } catch (BadLocationException ble) {}
	 setVisible(false);
    }
   else if (cmd.equals("NEXT")) {
      find(1,true);
      text_field.grabFocus();
    }
   else if (cmd.equals("LAST")) {
      find(-1,true);
      text_field.grabFocus();
    }
   else if (cmd.equals("REPL")) {
      replace();
   }
   else if (cmd.equals("REPLALL")) {
      replaceAll();
   }
   else BoardLog.logD("BALE","SEARCH ACTION: " + cmd);
}




@Override public void caretUpdate(CaretEvent e)
{
   JTextField tfld = (JTextField) e.getSource();

   if (tfld == text_field) {
      String txt = tfld.getText();
      if (txt.equals(search_for)) return;
      search_for = txt;
      //find(last_direction,false);
    }
}



@Override public void itemStateChanged(ItemEvent e) {
   Object source = e.getItemSelectable();
   if (source == is_case_sensitive) {
      searched_for = null;
    }
}


@Override protected void paintComponent(Graphics g0) {
   super.paintComponent(g0);
   Graphics2D g = (Graphics2D) g0.create();
   Color tc = BoardColors.getColor("Bale.FindReplaceTopColor");
   Color bc = BoardColors.getColor("Bale.FindReplaceBottomColor");
   BoardColors.setColors(this,tc);
   Paint p = new GradientPaint(0f, 0f, tc, 0f, getHeight(), bc);
   g.setPaint(p);
   g.fillRect(0, 0, getWidth() , getHeight());
}



private class SearchAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   SearchAction() {
      super("SearchAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      find(last_dir,true);
    }

}	// end of inner class SearchAction



private class ReplaceAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   ReplaceAction() {
      super("ReplaceAction");
    }

   @Override public void actionPerformed(ActionEvent e) {
      replace();
    }

}	// end of inner class ReplaceAction




private class CloseListener extends KeyAdapter {

   private int modifier_key;

   CloseListener() {
      modifier_key = SwingText.getMenuShortcutKeyMaskEx();
    }

   @Override public void keyPressed(KeyEvent e) {
      if (KeyEvent.getKeyText(e.getKeyCode()).equals("F") && e.getModifiersEx() == modifier_key){
	 clearHighlights();
	 setVisible(false);
	 if (editor_pane.isVisible()) editor_pane.grabFocus();
       }
    }

   @Override public void keyReleased(KeyEvent e) {
      if (e.getSource() == text_field) {
	 if (!KeyEvent.getKeyText(e.getKeyCode()).equals("Enter")) find(0, true);
      }
    }

}	// end of inner class CloseListener




private class HighlightCanceler implements CaretListener {

   @Override public void caretUpdate(CaretEvent e) {
      if (current_caret_position != editor_pane.getCaretPosition()) clearHighlights();
    }

}	// end of inner class HighlightCanceler



}	// end of class BaleFindReplaceBar





/* end of BaleFindReplaceBar.java */
