/********************************************************************************/
/*										*/
/*		BaleRenameContext.java						*/
/*										*/
/*	Bubble Annotated Language Editor context for renaming			*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.bowi.BowiFactory;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bueno.BuenoConstants;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.burp.BurpHistory;
import edu.brown.cs.ivy.swing.SwingText;
import edu.brown.cs.ivy.swing.SwingTextField;

import org.w3c.dom.Element;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.MatteBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;

import java.awt.Component;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



class BaleRenameContext implements BaleConstants, CaretListener, BuenoConstants, BudaConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BaleEditorPane	for_editor;
private BaleDocument	for_document;
private BaleElement	for_id;
private String		start_name;

private JDialog 	cur_menu;
private JTextField	rename_field;
private JButton 	accept_button;
private EditMouser	edit_mouser;
private EditKeyer	edit_keyer;
private RenamePanel	the_panel;

private static final int	X_DELTA = 0;
private static final int	Y_DELTA = 0;
private static final Pattern	ID_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z_0-9]*");



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleRenameContext(BaleEditorPane edt,int soff)
{
   for_editor = edt;
   for_document = edt.getBaleDocument();
   cur_menu = null;
   the_panel = null;
   start_name = null;

   for_id = for_document.getCharacterElement(soff);
   if (for_id == null || !for_id.isIdentifier()) return;

   int esoff = for_id.getStartOffset();
   int eeoff = for_id.getEndOffset();
   try {
      start_name = for_document.getText(esoff,eeoff-esoff);
    }
   catch (BadLocationException e) { 
      return; 
    }

   for_editor.addCaretListener(this);
   edit_mouser = new EditMouser();
   for_editor.addMouseListener(edit_mouser);
   edit_keyer = new EditKeyer();
   for_editor.addKeyListener(edit_keyer);
   for_editor.setRenameContext(this);

   handleShow();
}



/********************************************************************************/
/*										*/
/*	Methods to handle editing						*/
/*										*/
/********************************************************************************/

private synchronized void removeContext()
{
   if (for_editor == null) return;

   BoardLog.logD("BALE","Remove rename");

   for_editor.setRenameContext(null);
   for_editor.removeCaretListener(this);
   for_editor.removeMouseListener(edit_mouser);
   for_editor.removeKeyListener(edit_keyer);
   for_editor = null;
   for_document = null;
   if (cur_menu != null) {
      cur_menu.setVisible(false);
      cur_menu = null;
    }
}



@Override public void caretUpdate(CaretEvent e)
{
   removeContext();
}



private final class EditMouser extends MouseAdapter {

   @Override public void mousePressed(MouseEvent e) {
      removeContext();
    }

}	// end of inner class EditMouser




private final class EditKeyer extends KeyAdapter {

   @Override public void keyPressed(KeyEvent e) {
      removeContext();
    }

}	// end of inner class EditKeyer




/********************************************************************************/
/*										*/
/*	Classes to handle dialog box						*/
/*										*/
/********************************************************************************/

private synchronized void handleShow()
{
   BaleEditorPane be = for_editor;

   if (be == null) return;	// no longer relevant

   the_panel = new RenamePanel();

   if (for_editor == null) return;

   try {
      int soff = be.getCaretPosition();
      Rectangle r = SwingText.modelToView2D(be,soff);
      Window w = null;
      for (Component c = be; c != null; c = c.getParent()) {
	 if (w == null && c instanceof Window) {
	    w = (Window) c;
	    break;
	  }
       }
      cur_menu = new JDialog(w);
      cur_menu.setUndecorated(true);
      cur_menu.setContentPane(the_panel);
      try {
	 Point p0 = be.getLocationOnScreen();
	 cur_menu.setLocation(p0.x + r.x + X_DELTA,p0.y + r.y + r.height + Y_DELTA);
	 cur_menu.pack();
	 cur_menu.setVisible(true);
	 rename_field.grabFocus();
	 BoardLog.logD("BALE","Show rename");
       }
      catch (IllegalComponentStateException e) {
	 // Editor no longer on the screen -- ignore
       }
    }
   catch (BadLocationException e) {
      removeContext();
    }
}



/********************************************************************************/
/*										*/
/*	Validity checking methods						*/
/*										*/
/********************************************************************************/

private boolean isRenameValid()
{
   String ntext = rename_field.getText();
   if (!isValidId(ntext)) return false;
   if (ntext.equals(start_name)) return false;

   return true;
}



private static boolean isValidId(String text)
{
   if (text == null) return false;

   Matcher m = ID_PATTERN.matcher(text);

   return m.matches();
}



/********************************************************************************/
/*										*/
/*	Class to hold the renaming dialog					*/
/*										*/
/********************************************************************************/

private class RenamePanel extends JPanel {

   private static final long serialVersionUID = 1;

   RenamePanel() {
      setFocusable(false);
      int len = start_name.length() + 4;
      rename_field = new SwingTextField(start_name,len);
      RenameListener rl = new RenameListener();
      rename_field.addActionListener(rl);
      rename_field.addCaretListener(rl);
      add(rename_field);
      accept_button = new JButton(new AcceptAction());
      accept_button.setEnabled(false);
      add(accept_button);
      JButton cb = new JButton(new CancelAction());
      add(cb);
      setBorder(new MatteBorder(5,5,5,5,BoardColors.getColor("Bale.RenameBorderColor")));
    }

}	// end of inner class CompletionPanel




/********************************************************************************/
/*										*/
/*	Handle the renaming							*/
/*										*/
/********************************************************************************/

private class RenameDoer implements Runnable {

   private String rename_text;
   private Element rename_edits;
   private int rename_phase;

   RenameDoer() {
      rename_text = rename_field.getText();
      rename_edits = null;
      rename_phase = 0;
    }
	
   @Override public void run() {
      BudaRoot br = BudaRoot.findBudaRoot(for_editor);
      BumpClient bc = BumpClient.getBump();
      BaleEditorPane oed = for_editor;
   
      switch (rename_phase) {
         case 0 :
            BaleElement id = for_id;
            BaleDocument doc = for_document;
            if (id == null || doc == null) return;
            removeContext();
            
            BowiFactory.startTask();
            
            if (br != null) br.handleSaveAllRequest();
            
            int soff = doc.mapOffsetToEclipse(id.getStartOffset());
            int eoff = doc.mapOffsetToEclipse(id.getEndOffset());
            
            rename_edits = bc.rename(doc.getProjectName(),doc.getFile(),soff,eoff,rename_text);
            rename_phase = 1;
            SwingUtilities.invokeLater(this);
            break;
         case 1 :
            if (rename_edits != null) {
               BurpHistory.getHistory().beginEditAction(oed);
               try {
                  BaleApplyEdits bae = new BaleApplyEdits();
                  bae.applyEdits(rename_edits);
                }
               catch (Throwable t) {
                  BowiFactory.stopTask();
                  throw t;
                }
               finally {
                  BurpHistory.getHistory().endEditAction(oed);
                }
             }
            rename_phase = 2;
            BoardThreadPool.start(this);
            break;
         case 2 :
            try {
               br = BudaRoot.findBudaRoot(for_editor);
               if (br != null) {
                  br.handleSaveAllRequest();
                  bc.compile(false,true,true);
                }
             }
            finally {
               BowiFactory.stopTask();
             }
            break;
      }
   }

}	// end of inner class RanameDoer



/********************************************************************************/
/*										*/
/*	Actions 								*/
/*										*/
/********************************************************************************/

private final class RenameListener implements ActionListener, CaretListener {

   @Override public void actionPerformed(ActionEvent e) {
      if (!isRenameValid()) return;
      BoardThreadPool.start(new RenameDoer());
      // rename();
    }

   @Override public void caretUpdate(CaretEvent e) {
      accept_button.setEnabled(isRenameValid());
    }

}	// end of inner class RenameListener



private class AcceptAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   AcceptAction() {
      super(null,BoardImage.getIcon("accept"));
      putValue(LONG_DESCRIPTION,"Accept the rename");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BoardThreadPool.start(new RenameDoer());
      // rename();
    }

}	// end of inner class AcceptAction




private class CancelAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   CancelAction() {
      super(null,BoardImage.getIcon("cancel"));
      putValue(LONG_DESCRIPTION,"Cancel the rename");
    }

   @Override public void actionPerformed(ActionEvent e) {
      removeContext();
    }

}	// end of inner class CancelAction




}	// end of class BaleRenameContext




/* end of BaleRenameContext.java */
