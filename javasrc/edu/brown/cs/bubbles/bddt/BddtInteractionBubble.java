   /********************************************************************************/
/*										*/
/*		BddtInteractionBubble.java					*/
/*										*/
/*	Bubble Environment interactive expression bubble			*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Adam M. Cook, Steven P. Reiss	*/
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

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.bump.BumpConstants;

import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.swing.SwingText;

import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.IOException;




class BddtInteractionBubble extends BudaBubble implements BddtConstants, BudaConstants,
	BumpConstants, BddtConstants.BddtFrameListener
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BddtLaunchControl	for_control;
private JEditorPane		display_area;
private JTextField		input_field;
private BumpStackFrame		active_frame;
private BumpStackFrame		last_frame;

private Element 		body_element;
private Element 		frame_element;
private int			frame_counter;
private String			last_frameid;

private Color			background_color;
private Color			outline_color;


private static final long serialVersionUID = 1;

private static String header_string;

static {
   header_string = "<html><body id='body'>";
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BddtInteractionBubble(BddtLaunchControl ctrl)
{
   super(null,BudaBorder.RECTANGLE);

   for_control = ctrl;
   active_frame = null;
   last_frame = null;

   background_color = BoardColors.getColor(BDDT_INTERACTION_COLOR_PROP);
   outline_color = BoardColors.getColor(BDDT_INTERACTION_OUTLINE_PROP);
   display_area = new InteractEditor("text/html",header_string);
   display_area.setEditable(false);
   display_area.setOpaque(false);
   display_area.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES,Boolean.TRUE);
   body_element = findElementById("body");
   frame_element = null;
   frame_counter = 0;
   last_frameid = null;

   input_field = new JTextField();
   input_field.addActionListener(new ExprTypein());

   JScrollPane scrl = new JScrollPane(display_area);
   scrl.setPreferredSize(BDDT_INTERACTION_INITIAL_SIZE);

   SwingGridPanel pnl = new InteractPanel();
   pnl.addGBComponent(scrl,0,0,0,1,10,10);
   pnl.addGBComponent(input_field,1,1,0,1,10,0);
   JLabel prompt = new JLabel(BoardImage.getIcon("debug/interactprompt"));
   pnl.addGBComponent(prompt,0,1,1,1,0,0);

   setContentPane(pnl,input_field);
   pnl.addMouseListener(new FocusOnEntry(input_field));
   for_control.addFrameListener(this);
   setActiveFrame(for_control.getActiveFrame());
}




@Override protected void localDispose()
{
   for_control.removeFrameListener(this);
}



/********************************************************************************/
/*										*/
/*	Activation routines							*/
/*										*/
/********************************************************************************/

@Override public void setActiveFrame(BumpStackFrame frm)
{
   active_frame = frm;
}




/********************************************************************************/
/*										*/
/*	Menu management 							*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   JPopupMenu popup = new JPopupMenu();

   popup.add(getFloatBubbleAction());

   popup.show(this,e.getX(),e.getY());
}




/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

private void evaluate(String expr)
{
   if (active_frame == null) return;
   if (!active_frame.match(last_frame)) {
      String ftxt = getFrameHtml(active_frame.getDisplayString());
      insertBeforeEnd(body_element,ftxt);
      last_frame = active_frame;
      frame_element = findElementById(last_frameid);
    }

   StringBuffer buf = new StringBuffer();

   ExpressionValue ev = for_control.evaluateExpression(active_frame,expr);
   if (ev != null) {
      if (ev.isValid()) {
	 buf.append("<div align='left' style='color:green;'>");
         buf.append("<span style='color:black;'>");
         buf.append(expr);
         buf.append(" = ");
         buf.append("</span>");
	 buf.append(ev.formatResult());
	 buf.append("</div>");
       }
      else {
         buf.append("<div align='left'>");
         buf.append(expr);
         buf.append(" = ");
	 buf.append("<span style='color:red;'>");
	 buf.append(ev.getError().trim());
	 buf.append("</span>");
         buf.append("</div>");
       }
    }

   insertBeforeEnd(frame_element,buf.toString());

   // String txt = display_area.getText();
   // System.err.println("RESULT: " + txt);
}




private String getFrameHtml(String desc)
{
   StringBuffer buf = new StringBuffer();

   if (last_frame != null) buf.append("<br>");

   ++frame_counter;
   last_frameid = "frame_" + frame_counter;

   buf.append("<p ALIGN='CENTER'><b>");
   buf.append(desc);
   buf.append("</b></p><div ALIGN='LEFT' id='" + last_frameid + "'></div>");

   return buf.toString();
}



/********************************************************************************/
/*										*/
/*	Insertion methods							*/
/*										*/
/********************************************************************************/

private Element findElementById(String id)
{
   HTMLDocument hd = (HTMLDocument) display_area.getDocument();
   return hd.getElement(id);
}



private void insertBeforeEnd(Element e,String txt)
{
   HTMLDocument hd = (HTMLDocument) display_area.getDocument();
   try {
      hd.insertBeforeEnd(e,txt);
      Rectangle2D r2 = SwingText.modelToView2D(display_area,hd.getLength());
      Rectangle r = r2.getBounds();
      
      display_area.scrollRectToVisible(r);
      // String xtxt = hd.getText(0, hd.getLength());
      // System.err.println("RESULT IS " + xtxt);
    }
   catch (IOException ex) {
      BoardLog.logE("BDDT","Problem inserting evaluation output",ex);
    }
   catch (BadLocationException ex) {
      BoardLog.logE("BDDT","Problem inserting evaluation output",ex);
    }
}





/********************************************************************************/
/*										*/
/*	Implementations of widgets for drawing					*/
/*										*/
/********************************************************************************/

private class InteractPanel extends SwingGridPanel {

   InteractPanel() {
      setOpaque(false);
    }

   @Override public void paintComponent(Graphics g0) {
      Graphics2D g = (Graphics2D) g0;
      g.setColor(outline_color);
      Dimension sz = getSize();
      Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
      g.fill(r);
      super.paintComponent(g0);
    }

}	// end of inner class InteractPanel



private class InteractEditor extends JEditorPane implements CaretListener {

   InteractEditor(String typ,String cnts) {
      super(typ,cnts);
      addCaretListener(this);
    }

   @Override public void paintComponent(Graphics g0) {
      Graphics2D g = (Graphics2D) g0;
      g.setColor(background_color);
      Dimension sz = getSize();
      Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
      g.fill(r);
      super.paintComponent(g);
    }
   
   @Override public void caretUpdate(CaretEvent e) {
      if (getSelectionStart() != getSelectionEnd()) copy();
    }

}	// end of inner class InteractEditor




/********************************************************************************/
/*										*/
/*	Type in action listener 						*/
/*										*/
/********************************************************************************/

private class ExprTypein implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      JTextField tfld = (JTextField) evt.getSource();
      String expr = tfld.getText();
      if (expr == null || expr.length() == 0) return;

      evaluate(expr);

      tfld.setText("");
    }

}	// end of inner class ExprTypein





}	// end of class BddtInteractionBubble




/* end of BddtInteractionBubble.java */
