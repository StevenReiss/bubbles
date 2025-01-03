/********************************************************************************/
/*										*/
/*		BtedBubble.java 						*/
/*										*/
/*	Bubble Environment text editor bubble					*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Adam M. Cook 			*/
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


package edu.brown.cs.bubbles.bted;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardFileSystemView;
import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaConstants.BudaBubbleOutputer;
import edu.brown.cs.bubbles.buda.BudaCursorManager;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;
import edu.brown.cs.bubbles.burp.BurpHistory;

import edu.brown.cs.ivy.swing.SwingEditorPane;
import edu.brown.cs.ivy.swing.SwingKey;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.text.PlainDocument;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import jsyntaxpane.DefaultSyntaxKit;


class BtedBubble extends BudaBubble implements BtedConstants, BudaBubbleOutputer {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BtedEditorPane		 text_editor;
private transient  BtedFactory  the_factory;
private File			 current_file;
private RedoAction		 redo_action;
private UndoAction		 undo_action;
private JLabel			 name_label;
private BtedFindBar		 search_bar;
private JScrollPane		 scroll_pane;
private JPanel			 main_panel;
private transient BurpHistory    burp_history;
private transient BtedUndoableEditListener edit_listener;
private StartMode                start_mode;

private static BoardProperties	 bted_props	  = BoardProperties.getProperties("Bted");

private static final long	serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BtedBubble(String path,StartMode mode)
{
   boolean wrapfg = bted_props.getBoolean(WRAPPING);
   // current version of jsyntaxpane doesn't support line wrapping
   // DefaultSyntaxKit.initKit(wrapfg);
   wrapfg = false;
   DefaultSyntaxKit.initKit();

   text_editor = new BtedEditorPane();
   text_editor.setOpaque(false);
   edit_listener = new BtedUndoableEditListener();
   name_label = new JLabel();
   start_mode = mode;

   scroll_pane = new JScrollPane(text_editor);
   scroll_pane.setOpaque(false);
   if (wrapfg) {
      scroll_pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    }
   else {
      scroll_pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

   burp_history = BurpHistory.getHistory();

   the_factory = BtedFactory.getFactory();

   main_panel = new JPanel(new BorderLayout());
   this.setPreferredSize(new Dimension(bted_props.getInt(INITIAL_WIDTH),bted_props
					  .getInt(INITIAL_HEIGHT)));
   main_panel.setPreferredSize(new Dimension(bted_props.getInt(INITIAL_WIDTH) - 10,
						bted_props.getInt(INITIAL_HEIGHT) - 10));
   main_panel.add(scroll_pane, BorderLayout.CENTER);

   this.setupGui();

   if (path == null && mode != StartMode.NEW) {
      if (!this.openFileFromStart(mode)) {
	 this.newFile();
       }
    }
   else if (path == null && mode == StartMode.NEW) {
      this.newFile();
    }
   else {
      FileSystemView fsv = getFileSystemView();
      current_file = fsv.createFileObject(path);
      the_factory.loadFileIntoEditor(current_file, text_editor, edit_listener);
      name_label.setText(current_file.getName());
    }

   this.setContentPane(main_panel, text_editor);
}



/**
 * Overrided to close the document when the bubble is closed
 */
@Override protected void localDispose()
{
   this.onClose();
   super.localDispose();
}



/********************************************************************************/
/*										*/
/*	Window setup								*/
/*										*/
/********************************************************************************/

private void setupGui()
{
   JToolBar toolBar = new JToolBar();
   toolBar.setFloatable(false);
   
   StartMode newmode;
   StartMode openmode;
   switch (start_mode) {
      default :
      case NEW :
      case LOCAL :
         newmode = StartMode.NEW;
         openmode = StartMode.LOCAL;
         break;
      case NEW_REMOTE :
      case REMOTE :
         newmode = StartMode.NEW_REMOTE;
         openmode = StartMode.REMOTE;
         break;
    }

   AbstractAction newFileAction = new NewFileAction();
   AbstractAction newFileBubbleAction = new NewFileBubbleAction(newmode);
   AbstractAction openFileAction = new OpenFileAction();
   AbstractAction openFileBubbleAction = new OpenFileBubbleAction(openmode);
   AbstractAction saveFileAction = new SaveFileAction();
  
   JButton newButton = new JButton(newFileBubbleAction);
   newButton.setIcon(new ImageIcon(BoardImage.getImage("filenew.png")));
   newButton.setMargin(BUTTON_MARGIN);
   newButton.setToolTipText("New File");
   toolBar.add(newButton);

   JButton openButton = new JButton(openFileBubbleAction);
   openButton.setIcon(new ImageIcon(BoardImage.getImage("fileopen.png")));
   openButton.setMargin(BUTTON_MARGIN);
   openButton.setToolTipText("Open File");
   toolBar.add(openButton);

   JButton saveButton = new JButton(saveFileAction);
   saveButton.setIcon(new ImageIcon(BoardImage.getImage("filesave.png")));
   saveButton.setMargin(BUTTON_MARGIN);
   saveButton.setToolTipText("Save File");
   toolBar.add(saveButton);

   JButton saveAsButton = new JButton(new SaveFileAsAction());
   saveAsButton.setIcon(new ImageIcon(BoardImage.getImage("filesaveas.png")));
   saveAsButton.setMargin(BUTTON_MARGIN);
   saveAsButton.setToolTipText("Save As");
   toolBar.add(saveAsButton);

   undo_action = new UndoAction();
   redo_action = new RedoAction();

   JButton undoButton = new JButton(undo_action);
   undoButton.setIcon(new ImageIcon(BoardImage.getImage("undo.png")));
   undoButton.setMargin(BUTTON_MARGIN);
   undoButton.setToolTipText("Undo");
   toolBar.add(undoButton);

   JButton redoButton = new JButton(redo_action);
   redoButton.setIcon(new ImageIcon(BoardImage.getImage("redo.png")));
   redoButton.setMargin(BUTTON_MARGIN);
   redoButton.setToolTipText("Redo");
   toolBar.add(redoButton);

   search_bar = new BtedFindBar(text_editor);
   search_bar.setVisible(false);

   SwingKey.registerKeyAction("TEXTEDIT",main_panel,newFileAction,"menu N");
   SwingKey.registerKeyAction("TEXTEDIT",main_panel,openFileAction,"menu O");
   SwingKey.registerKeyAction("TEXTEDIT",main_panel,saveFileAction,"menu S");
   SwingKey.registerKeyAction("TEXTEDIT",main_panel,undo_action,"menu Z");
   SwingKey.registerKeyAction("TEXTEDIT",main_panel,redo_action,"menu Y");
   SwingKey.registerKeyAction("TEXTEDIT",main_panel,new FindAction(),"menu F");
   SwingKey.registerKeyAction("SEARCHBAR",search_bar,new NextFindAction(),"ENTER");

   JPanel topPanel = new JPanel(new BorderLayout());
   topPanel.add(name_label, BorderLayout.NORTH);
   topPanel.add(toolBar, BorderLayout.CENTER);

   BudaCursorManager.setCursor(scroll_pane,Cursor.getDefaultCursor());

   main_panel.add(topPanel, BorderLayout.NORTH);
   main_panel.add(search_bar, BorderLayout.SOUTH);
}



@Override public void setBounds(Rectangle r)
{
   if (bted_props.getBoolean(WRAPPING)) {
      text_editor.setSize(main_panel.getSize());
    }
   super.setBounds(r);
}

StartMode getMode()                     { return start_mode; }


static void registerDefaultKeys()
{
   SwingKey.registerKeyAction("TEXTEDIT","New File","menu N");
   SwingKey.registerKeyAction("TEXTEDIT","Open File","menu O");
   SwingKey.registerKeyAction("TEXTEDIT","Save File","menu S");
   SwingKey.registerKeyAction("TEXTEDIT","Undo","menu Z");
   SwingKey.registerKeyAction("TEXTEDIT","Redo","menu Y");
   SwingKey.registerKeyAction("TEXTEDIT","Find","menu F");
   SwingKey.registerKeyAction("SEARCHBAR","Find Next","ENTER");
}


/********************************************************************************/
/*										*/
/*	File handling methods							*/
/*										*/
/********************************************************************************/

/**
 * @return the current file
 */
public File getFile()
{
   return current_file;
}



/**
 * Opens a file when the bubble was just opened.
 * @return true if successful
 */

private boolean openFileFromStart(StartMode mode)
{
   JFileChooser chooser = new JFileChooser(BtedFactory.getLastDirectory(),getFileSystemView());
   int returnVal = chooser.showOpenDialog(this);
   if (returnVal == JFileChooser.APPROVE_OPTION) {
      current_file = chooser.getSelectedFile();
      BtedFactory.setLastDirectory(current_file);
      if (!current_file.exists() || !current_file.canRead()) {
	 JOptionPane.showMessageDialog(null,"File " + current_file + " cannot be opened");
	 return false;
       }
      the_factory.loadFileIntoEditor(current_file, text_editor, edit_listener);
      name_label.setText(current_file.getName());
      return true;
    }
   return false;
}



private FileSystemView getFileSystemView()
{
   switch (start_mode) {
      default :
      case NEW :
      case LOCAL :
         return FileSystemView.getFileSystemView();
      case REMOTE :
      case NEW_REMOTE :
         return BoardFileSystemView.getFileSystemView();
    }
}



/**
 * Opens a file when the bubble has already been open
 */
private void openFileFromMenu()
{
   JFileChooser chooser = new JFileChooser(BtedFactory.getLastDirectory());
   int returnVal = chooser.showOpenDialog(this);
   if (returnVal == JFileChooser.APPROVE_OPTION) {
      this.onClose();
      current_file = chooser.getSelectedFile();
      BtedFactory.setLastDirectory(current_file);
      the_factory.reopenBubble(current_file.getPath(), this);
      name_label.setText(current_file.getName());
    }
}


private void openBubbleFromMenu(StartMode mode)
{
   JFileChooser chooser = new JFileChooser(BtedFactory.getLastDirectory(),getFileSystemView());
   int rv = chooser.showOpenDialog(this);
   if (rv == JFileChooser.APPROVE_OPTION) {
      File f = chooser.getSelectedFile();
      BtedFactory.setLastDirectory(f);
      BtedBubble bb = new BtedBubble(f.getPath(),mode);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
      if (bba != null) {
	 bba.addBubble(bb,this,null,BudaConstants.PLACEMENT_PREFER|BudaConstants.PLACEMENT_NEW);
       }
    }
}




/**
 * Saves the file as the current_file unless it is null, in which case
 * the user is prompted for a location.
 */
private void saveFile()
{
   if (current_file == null) {
      saveAsFile();
    }
   else {
      try {
	 OutputStream os = new BufferedOutputStream(new FileOutputStream(current_file));
	 text_editor.setEditable(false);
	 BudaCursorManager.setGlobalCursorForComponent(this,
							  Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	 Writer w = new OutputStreamWriter(os);
	 text_editor.write(w);
	 w.close();
       }
      catch (IOException e) {
	 e.printStackTrace();
       }
      text_editor.setEditable(true);
      BudaCursorManager.resetDefaults(this); // setCursor(Cursor.getDefaultCursor());
    }
}



/**
 * Asks the user for a location and then calls saveFile() to save at this location
 */
private void saveAsFile()
{
   JFileChooser chooser = new JFileChooser(BtedFactory.getLastDirectory(),getFileSystemView());
   int returnVal = chooser.showSaveDialog(this);
   if (returnVal == JFileChooser.APPROVE_OPTION &&
	  !the_factory.isFileOpen(chooser.getSelectedFile())) {
      this.onClose();
      current_file = chooser.getSelectedFile();
      BtedFactory.setLastDirectory(current_file);
      this.saveFile();
      the_factory.reopenBubble(current_file.getPath(), this);
    }
   else {
      // could not save
    }
}



/**
 * Creates a new plain text document
 */
private void newFile()
{
   this.onClose();
   current_file = null;
   name_label.setText("New File");
   burp_history.addEditor(text_editor);
}



/**
 * Creates a new plain text document
 */
private void newFileBubble(StartMode mode)
{
   BtedBubble bb = new BtedBubble(null,mode);
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
   if (bba != null) {
      bba.addBubble(bb,this,null,BudaConstants.PLACEMENT_PREFER|BudaConstants.PLACEMENT_NEW);
    }
}



/**
 * Decreases the document count when closed
 */
protected void onClose()
{
   if (current_file != null) {
      the_factory.decreaseCount(current_file);
    }
}




/********************************************************************************/
/*										*/
/*	BudaBubbleOutputer methods						*/
/*										*/
/********************************************************************************/

@Override public String getConfigurator()
{
   return "BTED";
}

@Override public void outputXml(BudaXmlWriter xw)
{
   xw.field("TYPE", "EDITORBUBBLE");
   if (current_file != null) xw.field("PATH", current_file.getPath());
}



/********************************************************************************/
/*										*/
/*	Listeners and actions							*/
/*										*/
/********************************************************************************/


@Override public void handlePopupMenu(MouseEvent e)
{
   JPopupMenu popup = new JPopupMenu();

   popup.add(getFloatBubbleAction());

   popup.show(this,e.getX(),e.getY());
}




/**
 * Listens for edits to the document
 */
private final class BtedUndoableEditListener implements UndoableEditListener {

   @Override public void undoableEditHappened(UndoableEditEvent e) {
      undo_action.updateUndoState();
      redo_action.updateRedoState();
    }

} // end of class BtedUndoableEditListener



/**
 * Undoes the previous edit
 */
private class UndoAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   UndoAction() {
      super("Undo");
      this.setEnabled(false);
    }

   @Override public void actionPerformed(ActionEvent arg0) {
      burp_history.undo(text_editor);
      this.updateUndoState();
      redo_action.updateRedoState();
    }

   public void updateUndoState() {
      if (burp_history.canUndo(text_editor)) {
	 this.setEnabled(true);
       }
      else {
	 this.setEnabled(false);
       }
    }

} // end of class UndoAction



/**
 * Redoes the previous undo
 */
private class RedoAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   RedoAction() {
      super("Redo");
      this.setEnabled(false);
    }

   @Override public void actionPerformed(ActionEvent arg0) {
      burp_history.redo(text_editor);
      this.updateRedoState();
      undo_action.updateUndoState();
    }

   public void updateRedoState() {
      if (burp_history.canRedo(text_editor)) {
	 this.setEnabled(true);
       }
      else {
	 this.setEnabled(false);
       }
    }

} // end of class RedoAction




private class OpenFileAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   OpenFileAction() {
      super("Open File");
    }
   
   @Override public void actionPerformed(ActionEvent e) {
      openFileFromMenu();
    }

} // end of class OpenFileAction



private class OpenFileBubbleAction extends AbstractAction {

   private StartMode open_mode;
   
   private static final long serialVersionUID = 1;
   
   OpenFileBubbleAction(StartMode mode) {
      open_mode = mode;
    }

   @Override public void actionPerformed(ActionEvent e) {
      openBubbleFromMenu(open_mode);
    }

} // end of class OpenFileBubbleAction



private class NewFileAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   NewFileAction() {
      super("New File");
    }
   
   @Override public void actionPerformed(ActionEvent e) {
      newFile();
    }

} // end of class NewFileAction



private class NewFileBubbleAction extends AbstractAction {

   private StartMode new_mode;
   
   private static final long serialVersionUID = 1;
   
   NewFileBubbleAction(StartMode mode) {
      new_mode = mode;
    }

   @Override public void actionPerformed(ActionEvent e) {
      newFileBubble(new_mode);
    }

} // end of class NewFileAction



private class SaveFileAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   SaveFileAction() {
      super("Save File");
    }
   
   @Override public void actionPerformed(ActionEvent e) {
      saveFile();
    }

} // end of class saveFileAction



private final class SaveFileAsAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   @Override public void actionPerformed(ActionEvent e) {
      saveAsFile();
    }

} // end of class SaveFileAsAction



/**
 * Makes the search bar visible if it was not visible
 * and hides it if it was visible.
 */
private class FindAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   FindAction() {
      super("Find");
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (search_bar.isVisible()) {
	 search_bar.setVisible(false);
	 text_editor.grabFocus();
       }
      else {
	 search_bar.setVisible(true);
	 search_bar.grabFocus();
       }
    }

} // end of class FindAction



/**
 * If the search bar is visible, it will search for the
 * next item in the text box.
 */
private class NextFindAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   NextFindAction() {
      super("Find Next");
    }

   @Override public void actionPerformed(ActionEvent arg0) {
      if (search_bar.isVisible()) {
	 search_bar.search(SearchMode.NEXT);
       }
    }

} // end of class NextFindAction




/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

/**
 * Subclasses JEditorPane to allow gradient painting.
 */

private static class BtedEditorPane extends SwingEditorPane {

   private static final long serialVersionUID = 1;

   BtedEditorPane() {
      super("text/plain",null);
      int tvl = bted_props.getIntOption("Bted.tabsize");
      if (tvl > 0) {
         getDocument().putProperty(PlainDocument.tabSizeAttribute,tvl);
       }
      setFont(bted_props.getFont(EDITOR_FONT_PROP,EDITOR_FONT));
    }

   @Override protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      Dimension sz = getSize();
      Paint p = new GradientPaint(0f,0f,BoardColors.getColor(TOP_COLOR),0f,sz.height,
				     BoardColors.getColor(BOTTOM_COLOR));
      Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
      g2.setPaint(p);
      g2.fill(r);
      super.paintComponent(g);
    }

} // end of class BtedEditorPane




/**
 * Paints the bubble on the overview panel
 */
@Override protected void paintContentOverview(Graphics2D g,Shape s)
{
   Dimension sz = getSize();
   g.setColor(BoardColors.getColor(OVERVIEW_COLOR));
   g.fillRect(0, 0, sz.width, sz.height);
}



}	// end of class BtedBubble




/* end of BtedBubble.java */
