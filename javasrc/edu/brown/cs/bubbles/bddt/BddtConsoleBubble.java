/********************************************************************************/
/*										*/
/*		BddtConsoleBubble.java						*/
/*										*/
/*	Bubbles Environment dyanmic debugger tool console bubble implementation */
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


/* SVN: $Id$ */

package edu.brown.cs.bubbles.bddt;

import edu.brown.cs.bubbles.bale.BaleConstants;
import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardFont;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;
import edu.brown.cs.bubbles.bump.BumpLocation;

import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.swing.SwingText;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



class BddtConsoleBubble extends BudaBubble implements BddtConstants, BumpConstants, BudaConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private JScrollPane scroll_pane;
private JTextPane text_pane;
private JTextField input_pane;
private boolean   auto_scroll;
private BddtConsoleController console_control;

private static Pattern LOCATION_PATTERN =
   Pattern.compile("at ([a-zA-Z0-9<>$_.]+)\\(([a-zA-Z0-9_]+\\.java)\\:([0-9]+)\\)");
private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BddtConsoleBubble(BddtConsoleController ctrl,StyledDocument doc)
{
   console_control = ctrl;

   Color bg = BoardColors.getColor("Bddt.console.background");
   Color ibg = BoardColors.getColor("Bddt.console.input.background");
   Color ifg = BoardColors.getColor("Bddt.console.input.foreground");
   Color icg = BoardColors.getColor("Bddt.console.input.caret");

   text_pane = new JTextPane(doc);
   text_pane.setEditable(false);
   text_pane.setBackground(bg);
   String fam = BDDT_PROPERTIES.getString("Console.family",Font.MONOSPACED);
   int sz = BDDT_PROPERTIES.getInt("Console.size",11);
   Font ft = BoardFont.getFont(fam,Font.PLAIN,sz);
   text_pane.setFont(ft);
   text_pane.setForeground(BoardColors.getColor("Bddt.ConsoleTextColor"));

   scroll_pane = new JScrollPane(text_pane);
   scroll_pane.setBorder(null);
   scroll_pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
   scroll_pane.setWheelScrollingEnabled(true);
   int w = BDDT_PROPERTIES.getInt(BDDT_CONSOLE_WIDTH_PROP);
   int h = BDDT_PROPERTIES.getInt(BDDT_CONSOLE_HEIGHT_PROP);
   Dimension d = new Dimension(w,h - 24);
   auto_scroll = true;

   text_pane.setPreferredSize(d);

   input_pane = new JTextField();
   input_pane.setBackground(ibg);
   input_pane.setForeground(ifg);
   input_pane.setCaretColor(icg);
   input_pane.addActionListener(new InputHandler(doc));

   SwingGridPanel pnl = new SwingGridPanel();
   pnl.addGBComponent(scroll_pane,0,0,1,1,10,10);
   pnl.addGBComponent(input_pane,0,1,1,1,1,0);

   setContentPane(pnl,input_pane);

   doc.addDocumentListener(new EndScroll());
   text_pane.addMouseListener(new FocusOnEntry());
   text_pane.addMouseListener(new GotoMouser());
}




/********************************************************************************/
/*										*/
/*	Popup menu handling							*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   JPopupMenu menu = new JPopupMenu();

   Point pt0 = SwingUtilities.convertPoint(this,e.getPoint(),text_pane);
   GotoLine gl = checkForGoto(pt0);
   if (gl != null && gl.isValid()) {
      menu.add(gl);
    }

   JCheckBoxMenuItem sitm = new JCheckBoxMenuItem("Auto Scroll");
   sitm.setState(auto_scroll);
   sitm.addActionListener(new AutoScrollAction());
   menu.add(sitm);

   menu.add(getFloatBubbleAction());

   menu.show(this,e.getX(),e.getY());
}


private GotoLine checkForGoto(Point pt0)
{
   int pos = SwingText.viewToModel2D(text_pane,pt0);
   if (pos >= 0) {
      int start = Math.max(0,pos-100);
      int end = Math.min(text_pane.getDocument().getLength(),pos+100);
      try {
	 String txt = text_pane.getText(start,end-start);
	 int p0 = pos - start;
	 for (int i = p0; i >= 0; --i) {
	    if (i >= txt.length()) continue;
	    if (txt.charAt(i) == '\n') {
	       start = start + i + 1;
	       txt = txt.substring(i+1);
	       break;
	     }
	  }
	 p0 = pos-start;
	 if (p0 < 0) return null;
	 for (int i = p0; i < txt.length(); ++i) {
	    if (txt.charAt(i) == '\n') {
	       txt = txt.substring(0,i);
	       break;

	     }
	  }
	 Matcher m = LOCATION_PATTERN.matcher(txt);
	 if (m.find()) {
	    int spos = m.start();
	    int epos = m.end();
	    if (spos <= p0 && epos >= p0) {
	       int lno = Integer.parseInt(m.group(3));
	       GotoLine gl = new GotoLine(m.group(1),m.group(2),lno);
	       if (gl.isValid()) return gl;
	     }
	  }
       }
      catch (BadLocationException ex) { }
    }
   return null;
}



/********************************************************************************/
/*										*/
/*	handle automatic scrolling to end					*/
/*										*/
/********************************************************************************/

private class EndScroll implements DocumentListener, Runnable {

   private boolean	is_queued;

   EndScroll() {
      is_queued = false;
    }

   @Override public void changedUpdate(DocumentEvent e) { }

   @Override public void removeUpdate(DocumentEvent e) { }

   @Override public void insertUpdate(DocumentEvent e) {
      if (!auto_scroll) return;

      synchronized (this) {
	 if (!is_queued) {
	    SwingUtilities.invokeLater(this);
	    is_queued = true;
	  }
       }
    }

   @Override public void run() {
      if (!auto_scroll) return;
   
      synchronized (this) {
         is_queued = false;
       }
   
      AbstractDocument d = (AbstractDocument) text_pane.getDocument();
      d.readLock();
      try {
         int len = d.getLength();
         try {
            Rectangle r = SwingText.modelToView2D(text_pane,len-1);
            if (r != null) {
               Dimension sz = text_pane.getSize();
               r.x = 0;
               r.y += 20;
               if (r.y + r.height > sz.height) r.y = sz.height;
               text_pane.scrollRectToVisible(r);
             }
         }
         catch (BadLocationException ex) {
            BoardLog.logE("BDDT","Problem scrolling to end of console: " + ex);
         }
       }
      finally {
         d.readUnlock();
       }
    }

}	// end of inner class EndScroll




/********************************************************************************/
/*										*/
/*	Action									*/
/*										*/
/********************************************************************************/

private class AutoScrollAction implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      JCheckBoxMenuItem itm = (JCheckBoxMenuItem) evt.getSource();
      auto_scroll = itm.getState();
    }

}	// end of inner class AutoScrollAction



/********************************************************************************/
/*										*/
/*	Input handler								*/
/*										*/
/********************************************************************************/

private class InputHandler implements ActionListener {

   private StyledDocument for_document;

   InputHandler(StyledDocument doc) {
      for_document = doc;
    }

   @Override public void actionPerformed(ActionEvent e) {
      String s = input_pane.getText() + "\n";
      input_pane.setText(null);
      console_control.handleInput(for_document,s);
    }

}	// end of inner class InputHandler



/********************************************************************************/
/*										*/
/*	Handle mouse clicks that do a goto					*/
/*										*/
/********************************************************************************/

private class GotoMouser extends MouseAdapter {

   @Override public void mouseClicked(MouseEvent evt) {
      GotoLine gl = checkForGoto(evt.getPoint());
      if (gl != null && gl.isValid() && evt.getClickCount() == 1) {
         gl.createBubble();
       }
    }

}	// end of inner class GotoMouser




/********************************************************************************/
/*										*/
/*	Go to a line from an error report					*/
/*										*/
/********************************************************************************/

private class GotoLine extends AbstractAction {

   private String class_name;
   private String method_name;
   private boolean is_constructor;
   private int line_number;
   private List<BumpLocation> goto_locs;

   GotoLine(String mthd,String file,int line) {
      super("Go To " + mthd);
      goto_locs = null;
      int idx = mthd.lastIndexOf(".");
      if (idx < 0) return;
      class_name = mthd.substring(0,idx).replace("$",".");
      method_name = mthd.substring(idx+1);
      String nmthd = null;
      if (method_name.equals("<init>")) {
         idx = class_name.lastIndexOf(".");
         if (idx >= 0) method_name = class_name.substring(idx+1);
         else method_name = class_name;
         is_constructor = true;
         nmthd = class_name;
       }
      else {
         is_constructor = false;
         nmthd = class_name + "." + method_name;
       }
      line_number = line;
      BumpClient bc = BumpClient.getBump();
      List<BumpLocation> locs = bc.findMethods(null,nmthd,false,true,is_constructor,false);
      if (locs == null || locs.isEmpty()) return;
      BumpLocation bl0 = locs.get(0);
      File f = bl0.getFile();
      if (!f.exists()) return;
      if (locs.size() > 1) {
         BaleFactory bf = BaleFactory.getFactory();
         BaleConstants.BaleFileOverview bfo = bf.getFileOverview(null,f);
         if (bfo == null) return;
         int loff = bfo.findLineOffset(line_number);
         for (Iterator<BumpLocation> it = locs.iterator(); it.hasNext(); ) {
            BumpLocation bl1 = it.next();
            if (bl1.getOffset() > loff || bl1.getEndOffset() < loff) it.remove();
          }
         if (locs.size() == 0) return;
       }
      goto_locs = locs;
    }

   boolean isValid()			{ return goto_locs != null; }

   @Override public void actionPerformed(ActionEvent e) {
      if (goto_locs != null && goto_locs.size() > 0) createBubble();
    }

   void createBubble() {
      BaleFactory bf = BaleFactory.getFactory();
      bf.createBubbleStack(BddtConsoleBubble.this,null,null,false,goto_locs,null);
    }

}	// end of inner class GotoLine


}	// end of class BddtConsoleBubble




/* end of BddtConsoleBubble.java */

