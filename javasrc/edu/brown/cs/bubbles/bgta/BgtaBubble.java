/********************************************************************************/
/*                                                                              */
/*              BgtaBubble.java                                                 */
/*                                                                              */
/*      Bubbles attribute and property management main setup routine            */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2009 Brown University -- Ian Strickman                      */
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


package edu.brown.cs.bubbles.bgta;


import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardFont;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaCursorManager;
import edu.brown.cs.ivy.swing.SwingTextPane;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;



public class BgtaBubble extends BudaBubble implements BgtaConstants, DocumentListener {



/********************************************************************************/
/*                                                                              */
/*      Private storage                                                         */
/*                                                                              */
/********************************************************************************/

private BgtaManager       the_manager;
private String            chat_username;
private BoardProperties   my_props;
private BgtaLoggingArea   logging_area;
private BgtaDraftingArea  draft_area;
private BgtaLabel         bubble_label;
private BgtaChat          my_chat;
private JScrollPane       log_pane;
private JSeparator        draft_sep;
private JTextPane         history_area;
private JScrollPane       history_pane;
private JSeparator        history_sep;
private Dimension         initial_size;
private Dimension         previous_size;
private boolean           alt_color;
private boolean           alt_color_is_on;
private boolean           history_visible;
private boolean           history_loaded;

private static final long serialVersionUID = 1L;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private BgtaBubble(String username)
{
   history_visible = false;
   history_loaded = false;
   chat_username = username;
   my_props = BgtaFactory.getBgtaProperties();
   alt_color = my_props.getBoolean(BGTA_ALT_COLOR_UPON_RECEIVE);
   alt_color_is_on = alt_color;
   ChatPanel pan = new ChatPanel();
   GridBagConstraints c = new GridBagConstraints();

   logging_area = new BgtaLoggingArea(this);
   draft_area = new BgtaDraftingArea(logging_area,this);
   log_pane = new JScrollPane(logging_area,ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
           ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

   BudaCursorManager.setCursor(log_pane,new Cursor(Cursor.HAND_CURSOR));
   log_pane.setOpaque(false);
   log_pane.getViewport().setOpaque(false);
   log_pane.setBorder(new EmptyBorder(0,0,0,0));

   bubble_label = new BgtaLabel(chat_username,getName(chat_username));
   JButton showHistoryButton = new JButton("Show History");
   showHistoryButton.addActionListener(new ActionListener() {
       @Override public void actionPerformed(ActionEvent e) {
           if (!history_visible) {
               showHistory();
               ((JButton) e.getSource()).setText("Hide History");
           }
           else {
               hideHistory();
               ((JButton) e.getSource()).setText("Show History");
           }
           history_visible = !history_visible;
       }
   });
   bubble_label.setButton(showHistoryButton);

   history_area = new SwingTextPane();
   history_area.setOpaque(false);
   history_pane = new JScrollPane(history_area,
         ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
         ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
   history_pane.setOpaque(false);
   history_pane.getViewport().setOpaque(false);
   history_pane.setBorder(new EmptyBorder(0,0,0,0));

   c.fill = GridBagConstraints.BOTH;
   c.gridwidth = 3;
   c.gridx = 0;
   c.gridy = 0;
   c.weighty = 0.0;
   c.weightx = 1.0;
   pan.add(bubble_label, c);

   c.gridwidth = 1;
   c.gridy = 1;
   c.weighty = 1.0;
   if (history_visible)
       c.weightx = 0.5;
   else
       c.weightx = 0.0;
   pan.add(log_pane, c);

   c.weighty = 0.0;
   c.gridy = 2;
   draft_sep = new JSeparator(SwingConstants.HORIZONTAL);
   pan.add(draft_sep, c);

   c.gridy = 3;
   pan.add(draft_area, c);

   // Add history panel
   c.weightx = 0.0;
   c.weighty = 1.0;
   c.gridheight = 3;
   c.gridwidth = 1;
   c.gridx = 1;
   c.gridy = 1;
   history_sep = new JSeparator(SwingConstants.VERTICAL);
   pan.add(history_sep,c);
   c.gridx = 2;
   if (history_visible)
       c.weightx = 1.0;
   else
       c.weightx = 1.0;
   pan.add(history_pane,c);
   history_sep.setVisible(false);
   history_area.setVisible(false);
   history_pane.setVisible(false);
   history_pane.getViewport().setVisible(false);

   pan.setFocusable(true);
   pan.addMouseListener(new BudaConstants.FocusOnEntry(draft_area));
   logging_area.addMouseListener(new BudaConstants.FocusOnEntry(draft_area));
   draft_area.setFocusable(true);
   draft_area.addMouseListener(new BudaConstants.FocusOnEntry(draft_area));

   setContentPane(pan, draft_area);
}

protected BgtaBubble(BgtaChat chat)
{
   this(chat.getUsername());
   setChat(chat);
}

BgtaBubble(String username,BgtaManager man)
{
   this(username);
   the_manager = man;
   bubble_label.setPresence(man.getRoster().getPresence(chat_username));
   the_manager.addPresenceListener(bubble_label);

   if (!the_manager.hasChat(chat_username)) {
      setChat(the_manager.startChat(chat_username,this));
    }
   else {
      setChat(the_manager.getExistingChat(chat_username));
      if (my_chat != null) {
         the_manager.addDuplicateBubble(this);
       }
    }


}

@Override public void setVisible(boolean vis)
{
   super.setVisible(vis);
   if (the_manager != null) {
         if (!vis) {
          the_manager.removeBubble(this);
       }
      else {
          the_manager.addDuplicateBubble(this);
       }
   }
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getUsername()                            { return chat_username; }

BgtaLoggingArea getLog()                        { return logging_area; }

/**
 * Associates a chat object with this bubble.
 * Also notifies the drafting and logging areas
 * that the chat and Document have changed. Finally,
 * it registers this bubble as a listener to the new
 * chat's Document.
 *
 * @param chat A BgtaChat this bubble connects with
 */
void setChat(BgtaChat chat)
{
   if (chat == null)
       return;
   my_chat = chat;
   Document doc = chat.getDocument();
   logging_area.setDocument(doc);
   draft_area.setChat(my_chat);

   // Register bubble as document listener.
   doc.addDocumentListener(this);
}

/********************************************************************************/
/*                            */
/* Helper methods                           */
/*                            */
/********************************************************************************/

/**
 * Creates a more displayable version of a username. This
 * is accomplished by tearing off anything after an @, and replacing periods
 * and underscores with spaces.
 *
 * @param username The username to be transformed into just a name
 *
 * @return The resulting String
 */
private String getName(String username)
{
    String name = username;
    int idx = name.indexOf("@");
    if (idx > 0)
        name = name.substring(0, idx);
    name = whiteSpaceAwareReplace(name,".");
    name = whiteSpaceAwareReplace(name,"_");
    return name;
}

/**
 * Replaces all occurrences of toreplace with a space. This method checks
 * to make sure that there isn't a space next to an occurrence of toreplace
 * already before replacing it. If there is, it simply removes the occurrence
 * of toreplace, leaving the already present space to fill the void.
 *
 * @param input A String
 * @param toreplace The String to replace
 *
 * @return The resulting String
 */
private String whiteSpaceAwareReplace(String input,String toreplace)
{
    String current = input;
    while (current.indexOf(toreplace) != -1) {
        if (current.charAt(current.indexOf(toreplace) + 1) != ' ') {
            String back = current.substring(current.indexOf(toreplace) + 1);
            String front = current.substring(0, current.indexOf(toreplace));
            current = front + " " + back;
        }
        else {
            String back = current.substring(current.indexOf(toreplace) + 1);
            String front = current.substring(0, current.indexOf(toreplace));
            current = front + back;
        }
    }
    return current;
}

/********************************************************************************/
/*                                                                              */
/*      Messaging methods                                                       */
/*                                                                              */
/********************************************************************************/

/**
 * Routes a message created elsewhere to the
 * drafting area, which will actually send it
 * to the participating user. Right now, this
 * is only used for sending metadata.
 *
 * @param msg A String
 */
public void sendMessage(String msg)
{
   draft_area.send(msg);
}


/********************************************************************************/
/*                                                                              */
/*      Metadata processing                                                     */
/*                                                                              */
/********************************************************************************/

void processMetadata(String data)
{
    JButton accept = new JButton("Load Task");
    accept.setFont(BoardFont.getFont(accept.getFont().getFontName(),Font.PLAIN,10));
    Dimension d = new Dimension(BGTA_DATA_BUTTON_WIDTH,BGTA_DATA_BUTTON_HEIGHT);
    accept.setPreferredSize(d);
    accept.setSize(d);
    accept.setMinimumSize(d);
    Element xml = IvyXml.loadXmlFromURL(data);
    accept.addActionListener(new XMLListener(xml));
    logging_area.setCaretPosition(logging_area.getDocument().getLength());
    bubble_label.setButton(accept);
}



/********************************************************************************/
/*                                                                              */
/*      Button press methods                                                    */
/*                                                                              */
/********************************************************************************/

void pressedButton(String msg)
{
    try {
        logging_area.getDocument().insertString(logging_area.getDocument().getLength(),msg,null);
     }
    catch (BadLocationException e) {}
    bubble_label.setButton(null);
}

private final class XMLListener implements ActionListener {

    private Element _xml;

    private XMLListener(Element xml) {
        _xml = xml;
    }

    @Override public void actionPerformed(ActionEvent e) {
        BgtaFactory.addTaskToRoot(_xml);
        pressedButton(BGTA_TASK_DESCRIPTION);
    }

}       // end of private class XMLListener

void showHistory()
{
   // Load this history if it hasn't been loaded yet.
   if (!history_loaded) {
      my_chat.loadHistory(history_area);
      history_loaded = true;
        }

   // Reset sizing policy
   GridBagLayout lay = (GridBagLayout) ((ChatPanel) getContentPane()).getLayout();
   GridBagConstraints c = new GridBagConstraints();
   c.fill = GridBagConstraints.BOTH;
   c.gridx = 0;
   c.gridy = 1;
   c.gridwidth = 1;
   c.gridheight = 1;
   c.weighty = 1.0;
   c.weightx = 0.0;
   lay.setConstraints(log_pane,c);
   c.gridheight = 3;
   c.gridx = 2;
   c.weightx = 1.0;
   lay.setConstraints(history_pane,c);

   // Show history pane
   history_pane.setVisible(true);
   history_sep.setVisible(true);
   history_area.setVisible(true);
   history_pane.getViewport().setVisible(true);

   // Widen the bubble
   previous_size = getSize();
   Dimension size = new Dimension(previous_size);
   size.width += initial_size.width;
   setSize(size);
   setPreferredSize(size);
   setMinimumSize(size);
}

void hideHistory()
{
   // Reset sizing policy
   GridBagLayout lay = (GridBagLayout) ((ChatPanel) getContentPane()).getLayout();
   GridBagConstraints c = new GridBagConstraints();
   c.fill = GridBagConstraints.BOTH;
   c.gridx = 0;
   c.gridy = 1;
   c.gridwidth = 1;
   c.gridheight = 1;
   c.weighty = 1.0;
   c.weightx = 1.0;
   lay.setConstraints(log_pane,c);
   c.gridheight = 3;
   c.gridx = 2;
   c.weightx = 0.0;
   lay.setConstraints(history_pane,c);

   // Hide history pane
   history_pane.setVisible(false);
   history_sep.setVisible(false);
   history_area.setVisible(false);
   history_pane.getViewport().setVisible(false);

   // Narrow the bubble
   setSize(previous_size);
   setPreferredSize(previous_size);
   setMinimumSize(previous_size);
}


/********************************************************************************/
/*                                                                              */
/*      Alt color methods                                                       */
/*                                                                              */
/********************************************************************************/

void setAltColorIsOn(boolean ison)
{
   if (!alt_color) return;
   alt_color_is_on = ison;
   repaint();
}


boolean reloadAltColor()
{
   alt_color = my_props.getBoolean(BGTA_ALT_COLOR_UPON_RECEIVE);
   if (!alt_color) alt_color_is_on = false;
   return alt_color;
}


boolean getAltColorIsOn()
{
   return alt_color_is_on;
}



/********************************************************************************/
/*                                                                              */
/* Document listener methods                                                    */
/*                                                                              */
/********************************************************************************/

@Override public void changedUpdate(DocumentEvent e)
{
    if (isVisible())
        repaint();
    logging_area.setCaretPosition(logging_area.getDocument().getLength());
}


@Override public void insertUpdate(DocumentEvent e)
{
   if (isVisible())
      repaint();
   logging_area.setCaretPosition(logging_area.getDocument().getLength());
}


@Override public void removeUpdate(DocumentEvent e)
{
    if (isVisible())
        repaint();
    logging_area.setCaretPosition(logging_area.getDocument().getLength());
}



/********************************************************************************/
/*                                                                              */
/*      Object methods                                                  */
/*                                                                              */
/********************************************************************************/

@Override public String toString()
{
   return "BgtaBubble - Username: " + chat_username;
}



/********************************************************************************/
/*                                                                              */
/*      Chat Panel implementation                                               */
/*                                                                              */
/********************************************************************************/

private class ChatPanel extends JPanel implements BgtaConstants {

   private static final long serialVersionUID = 1L;

   ChatPanel() {
      super(new GridBagLayout());
      setOpaque(false);
    }

   @Override public void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      Dimension sz = getSize();
      if (initial_size == null)
         initial_size = sz;
      Paint p;
      Color tc;
      Color bc;
      if (!alt_color_is_on) {
         tc = BoardColors.getColor(BGTA_BUBBLE_TOP_COLOR_PROP);
         bc = BoardColors.getColor(BGTA_BUBBLE_BOTTOM_COLOR_PROP);
       }
      else {
         tc = BoardColors.getColor(BGTA_ALT_BUBBLE_TOP_COLOR_PROP);
         bc = BoardColors.getColor(BGTA_ALT_BUBBLE_BOTTOM_COLOR_PROP);
       } 
      p = new GradientPaint(0f,0f,tc,0f,sz.height,bc);
      Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
      g2.setPaint(p);
      g2.fill(r);

      super.paintComponent(g);
    }

}       // end of private class ChatPanel



}       // end of class BgtaBubble



/* end of BgtaBubble.java */
