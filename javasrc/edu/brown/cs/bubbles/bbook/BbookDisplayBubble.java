/********************************************************************************/
/*										*/
/*		BbookDisplayBubble.java 					*/
/*										*/
/*	Bubble with programmer's logbook display interface                      */
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2009, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bbook;

import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.ivy.swing.SwingEditorPane;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import java.awt.Desktop;
import java.awt.Dimension;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;



class BbookDisplayBubble extends BudaBubble implements BbookConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BbookDisplayBuilder	display_builder;
private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BbookDisplayBubble(BbookDisplayBuilder bld)
{
   display_builder = bld;

   JComponent pnl = setupDisplay();

   Dimension d = pnl.getPreferredSize();
   if (d.width > 500) d.width = 500;
   if (d.height > 400) d.height = 400;
   JScrollPane jsp = new JScrollPane(pnl);
   jsp.setPreferredSize(d);
   jsp.setSize(d);

   setContentPane(jsp);
}



/********************************************************************************/
/*										*/
/*	Display Setup								*/
/*										*/
/********************************************************************************/

private JComponent setupDisplay()
{
   String html = "<html>Generating requested notebook display ...";
      // display_builder.generateHtml();

   JEditorPane edp = new SwingEditorPane("text/html",html);
   edp.setEditable(false);
   edp.addHyperlinkListener(new Linker());

   ComputeDisplay cd = new ComputeDisplay(edp);
   BoardThreadPool.start(cd);
   
   return edp;
}



/********************************************************************************/
/*                                                                              */
/*      Display computation                                                     */
/*                                                                              */
/********************************************************************************/

private class ComputeDisplay implements Runnable {
   
   private JEditorPane editor_pane;
   
   ComputeDisplay(JEditorPane edt) {
      editor_pane = edt;
    }
   
   @Override public void run() {
      String html = display_builder.generateHtml();
      editor_pane.setText(html);
    }
      
}

/********************************************************************************/
/*										*/
/*	Hyperlink management							*/
/*										*/
/********************************************************************************/

private static final class Linker implements HyperlinkListener {

   @Override public void hyperlinkUpdate(HyperlinkEvent e) {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
         URL u = e.getURL();
         try {
            Desktop.getDesktop().browse(u.toURI());
          }
         catch (IOException ex) { }
         catch (URISyntaxException ex) { }
       }
    }

}	// end of inner class Linker





}	// end of class BbookDisplayBubble




/* end of BbookDisplayBubble.java */

