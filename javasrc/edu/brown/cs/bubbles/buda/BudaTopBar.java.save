/********************************************************************************/
/*										*/
/*		BudaTopBar.java 						*/
/*										*/
/*	BUblles Display Area top bar						*/
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



package edu.brown.cs.bubbles.buda;

import edu.brown.cs.bubbles.buda.BudaConstants.BudaHelpClient;
import edu.brown.cs.bubbles.board.*;

import edu.brown.cs.ivy.swing.SwingText;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.*;


class BudaTopBar extends JPanel implements ActionListener, BudaConstants, BoardConstants, BudaHelpClient {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BudaRoot buda_root;
private BudaBubbleArea bubble_area;
private BudaOverviewBar overview_area;
private JPopupMenu bubble_menu;
private JPopupMenu workingset_menu;
private String window_label;
private JFileChooser file_chooser;
private JFileChooser ws_chooser;
private JFileChooser pdf_chooser;
private BudaWorkingSetImpl cur_workingset;
private Point popup_point;
private Map<BudaWorkingSetImpl, ChevronButton> chevron_buttons;
private JButton fake_chevron_button;
private BudaTaskShelf task_shelf;
private JMenu task_dummy_menu;
private JMenuItem share_button;
private JMenuItem loadshare_button;
private Collection<BudaShare> open_shares;

private static final long serialVersionUID = 1L;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BudaTopBar(BudaRoot br,Element cfg,BudaBubbleArea bba,BudaOverviewBar bob)
{
   buda_root = br;
   bubble_area = bba;
   overview_area = bob;

   cur_workingset = null;
   window_label = null;
   popup_point = null;
   chevron_buttons = new HashMap<BudaWorkingSetImpl, ChevronButton>();
   fake_chevron_button = null;
   task_shelf = null;
   share_button = null;
   loadshare_button = null;
   open_shares = null;

   Dimension d = new Dimension(0,BUBBLE_TOP_BAR_HEIGHT);
   setMinimumSize(d);
   setPreferredSize(d);
   setMaximumSize(d);

   setBackground(TOP_BAR_COLOR);
   BudaRoot.registerHelp(this,this);

   bubble_menu = new JPopupMenu("Menu");
   task_dummy_menu = new ShelfMenu();
   task_dummy_menu.setPreferredSize(new Dimension(0,0));
   bubble_menu.add(task_dummy_menu);
   addButton(bubble_menu,"Define Working Set","Mark the current display as a working set");
   addButton(bubble_menu,"Load Working Set","Load a saved working set");
   loadshare_button = addButton(bubble_menu,"Load Shared Working Set","Load a shared working set");
   loadshare_button.setEnabled(false);
   //addButton(bubble_menu,"Load Task","Load a saved task");
   addButton(bubble_menu,"Save Configuration","Save the current configuration");
   addButton(bubble_menu,"Clear Bubbles in View","Remove all bubbles in the current view");
   addButton(bubble_menu,"Print","Print the current view");
   addButton(bubble_menu,"Export as PDF","Save the current view as a PDF file");
   bubble_menu.add(new JSeparator());
   addButton(bubble_menu,"Clear All Bubbles","Close all bubbles");
   addButton(bubble_menu,"Save All","Save all open files");
   addButton(bubble_menu,"Quit","Exit from Bubbles");
   bubble_menu.addPopupMenuListener(new ShelfListener());

   workingset_menu = new JPopupMenu("WorkingSet");
   addButton(workingset_menu, "Close and Save to Task Shelf", "Close the working set and save it to the task shelf");
   addButton(workingset_menu,"Export as PDF","Save the working set as a PDF file");
   addButton(workingset_menu,"EMail Working Set","EMail the Working Set");
   addButton(workingset_menu,"EMail as PDF","EMail a PDF image of the working set");
   share_button = addButton(workingset_menu,"Share Working Set","Share this working set dynamically");
   workingset_menu.add(new JSeparator());
   addButton(workingset_menu,"Remove Working Set","Remove the working set, but don't close any bubbles");
   addButton(workingset_menu,"Clear and Remove Working Set","Remove the working set and close its bubbles");
   addButton(workingset_menu,"Save Working Set","Save the working set to a file");
   addButton(workingset_menu,"Save Working Set to Task Shelf","Save the working set to the task shelf");
   addButton(workingset_menu,"Clear Bubbles in Working Set","Remove all bubbles in the working set");
   workingset_menu.add(new JSeparator());
   addButton(workingset_menu,"Clear All Bubbles","Close all bubbles");

   Mouser mm = new Mouser();
   addMouseListener(mm);
   addMouseMotionListener(mm);

   file_chooser = new JFileChooser(System.getProperty("user.dir"));
   file_chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
   FileNameExtensionFilter ff = new FileNameExtensionFilter("Bubbles context files","bubbles");
   file_chooser.setFileFilter(ff);

   ws_chooser = new JFileChooser(System.getProperty("user.dir"));
   ws_chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
   ff = new FileNameExtensionFilter("Bubbles working set files","bset");
   ws_chooser.setFileFilter(ff);

   pdf_chooser = new JFileChooser(System.getProperty("user.dir"));
   pdf_chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
   ff = new FileNameExtensionFilter("PDF Files","pdf");
   pdf_chooser.setFileFilter(ff);

   String ttl = IvyXml.getTextElement(cfg,"LABEL");
   if (ttl != null) setTitle(ttl);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void setTitle(String txt)
{
   window_label = txt;
   buda_root.setTitle(txt);
}


BudaBubbleArea getBubbleArea()			{ return bubble_area; }

BudaOverviewBar getOverviewBar()		{ return overview_area; }

BudaRoot getRoot()		{ return buda_root; }



BudaWorkingSetImpl findCurrentWorkingSet()	{ return cur_workingset; }



void addMenuItem(Component c,boolean ws)
{
   if (ws) workingset_menu.add(c);
   else bubble_menu.add(c);
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

private JMenuItem addButton(JComponent menu,String id,String tt)
{
   JMenuItem itm = new JMenuItem(id);
   itm.addActionListener(this);
   if (tt != null) {
      itm.setToolTipText(tt);
      ToolTipManager.sharedInstance().registerComponent(itm);
    }
   menu.add(itm);

   return itm;
}


/********************************************************************************/
/*										*/
/*	Menu methods								*/
/*										*/
/********************************************************************************/

@Override public void actionPerformed(ActionEvent e)
{
   String cmd = e.getActionCommand();
   BoardMetrics.noteCommand("BUDA","topbar" + cmd);

   if (cmd.equals("Save All")) {
      buda_root.handleSaveAllRequest();
    }
   else if (cmd.equals("Define Working Set")) {
      buda_root.handleNewWorkingSet(this,"title");
    }
   else if (cmd.equals("Save Configuration")) {
      file_chooser.setDialogTitle("Location to save configuration");
      int sts = file_chooser.showSaveDialog(this);
      if (sts == JFileChooser.APPROVE_OPTION) {
	 File f = file_chooser.getSelectedFile();
	 try {
	    buda_root.saveConfiguration(f);
	  }
	 catch (IOException ex) {
	    JOptionPane.showMessageDialog(this,"Configuration save failed");
	  }
       }
    }
   else if (cmd.equals("Quit")) {
      buda_root.handleCloseRequest();
    }
   else if (cmd.equals("Load Working Set")) {
      buda_root.setChannelSet(null);
      ws_chooser.setDialogTitle("Location to load working set from");
      int sts = ws_chooser.showOpenDialog(this);
      if (sts == JFileChooser.APPROVE_OPTION) {
	 File f = ws_chooser.getSelectedFile();
	 if (f != null) {
	    Element xml = IvyXml.loadXmlFromFile(f);
	    if (xml != null) {
	       BudaTask bt = new BudaTask(xml);
	       bt.loadTask(bubble_area,getAreaOffset(popup_point.getX()));
	       buda_root.repaint();
	     }
	  }
       }
    }
   else if (cmd.equals("Clear Bubbles in View")) {
      buda_root.setChannelSet(null);
      Rectangle r = buda_root.getViewport();
      Collection<BudaBubble> bbls = bubble_area.getBubblesInRegion(r);
      for (BudaBubble bb : bbls) {
	 if (bb.isFloating()) continue;
	 bubble_area.userRemoveBubble(bb);
       }
    }
   else if (cmd.equals("Clear Bubbles in Working Set")) {
      cur_workingset.removeBubbles();
    }
   else if (cmd.equals("Remove Working Set")) {
      buda_root.removeWorkingSet(cur_workingset);
      remove(chevron_buttons.get(cur_workingset));
      chevron_buttons.remove(cur_workingset);
    }
   else if (cmd.equals("Close and Save to Task Shelf")) {
      if (cur_workingset.getLabel() == null) {
	 JOptionPane.showMessageDialog(this,"Working set should be named first");
       }
      else {
	 BudaTask bt = cur_workingset.createTask();
	 if (bt != null) buda_root.addTask(bt);
	 buda_root.removeWorkingSet(cur_workingset);
	 remove(chevron_buttons.get(cur_workingset));
	 chevron_buttons.remove(cur_workingset);
       }
   }
   else if (cmd.equals("Clear and Remove Working Set")) {
      if (cur_workingset.getLabel() == null) {
	 BudaTask bt = cur_workingset.createTask();
	 if (bt != null) buda_root.addTask(bt);
       }
      cur_workingset.removeBubbles();
      buda_root.removeWorkingSet(cur_workingset);
      remove(chevron_buttons.get(cur_workingset));
      chevron_buttons.remove(cur_workingset);
    }
   else if (cmd.equals("Save Working Set")) {
      ws_chooser.setDialogTitle("Location to save working set");
      int sts = ws_chooser.showSaveDialog(this);
      if (sts == JFileChooser.APPROVE_OPTION) {
	 File f = ws_chooser.getSelectedFile();
	 if (!f.getPath().endsWith(".bset")) {
	    f = new File(f.getPath() + ".bset");
	  }
	 try {
	    cur_workingset.saveAs(f);
	  }
	 catch (IOException ex) {
	    JOptionPane.showMessageDialog(this,"Working set save failed");
	  }
       }
    }
   else if (cmd.equals("Clear All Bubbles")) {
      for (BudaBubble bb : bubble_area.getBubbles()) {
	 if (!bb.isFloating()) {
	    bubble_area.userRemoveBubble(bb);
	  }
       }
    }
   else if (cmd.equals("EMail Working Set")) {
      cur_workingset.sendMail(null);
    }
   else if (cmd.equals("EMail as PDF")) {
      cur_workingset.sendPDF(null);
    }
   else if (cmd.equals("Save Working Set to Task Shelf")) {
      if (cur_workingset.getLabel() == null) {
	 JOptionPane.showMessageDialog(this,"Working set should be named first");
       }
      else {
	 BudaTask bt = cur_workingset.createTask();
	 if (bt != null) buda_root.addTask(bt);
       }
    }
   else if (cmd.equals("Print")) {
      buda_root.printViewport();
    }
   else if (cmd.equals("Export as PDF")) {
      pdf_chooser.setDialogTitle("Location to store PDF file");
      int sts = pdf_chooser.showSaveDialog(this);
      if (sts == JFileChooser.APPROVE_OPTION) {
	 File f = pdf_chooser.getSelectedFile();
	 if (!f.getPath().endsWith(".pdf")) {
	    f = new File(f.getPath() + ".pdf");
	  }
	 try {
	    if (cur_workingset == null) buda_root.exportViewportAsPdf(f);
	    else {
	       buda_root.exportAsPdf(f,cur_workingset.getRegion());
	     }
	  }
	 catch (Exception ex) {
	    BoardLog.logE("BUDA","PDF FAILURE",ex);
	    JOptionPane.showMessageDialog(this,"PDF export failed");
	  }
       }
    }
   else if (cmd.equals("Save as PDF")) {
      pdf_chooser.setDialogTitle("Location to store PDF file");
      int sts = pdf_chooser.showSaveDialog(this);
      if (sts == JFileChooser.APPROVE_OPTION) {
	 File f = pdf_chooser.getSelectedFile();
	 if (!f.getPath().endsWith(".pdf")) {
	    f = new File(f.getPath() + ".pdf");
	  }
	 try {
	    buda_root.exportViewportAsPdf(f);
	  }
	 catch (Exception ex) {
	    BoardLog.logE("BUDA","PDF FAILURE",ex);
	    JOptionPane.showMessageDialog(this,"PDF export failed");
	  }
       }
    }
   else if (cmd.equals("Share Working Set")) {
      buda_root.getShareManager().createShare(cur_workingset);
    }
   else if (cmd.equals("Stop Sharing Working Set")) {
      buda_root.getShareManager().removeShare(cur_workingset);
    }
   else if (cmd.equals("Load Shared Working Set")) {
      BudaShare [] shrs = createShareArray();
      int sts = JOptionPane.showOptionDialog(this,"Select share to load",
						"Select share to load",
						JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.QUESTION_MESSAGE,
						null,shrs,null);
      if (sts == JOptionPane.CLOSED_OPTION) return;
      int x0 = getAreaOffset(popup_point.getX());
      buda_root.getShareManager().useShare(shrs[sts],bubble_area,x0);
    }
   else {
      BoardLog.logE("BUDA","Unknown top bar command " + cmd);
    }
}




private int getAreaOffset(double x)
{
   Rectangle r = getBounds();
   Dimension totsize = bubble_area.getSize();

   return (int)(x * totsize.width / r.width);
}



private BudaShare [] createShareArray()
{
   BudaShare [] shrs = open_shares.toArray(new BudaShare[open_shares.size()]);
   Arrays.sort(shrs);
   return shrs;
}


/********************************************************************************/
/*										*/
/*	Tool tip methods							*/
/*										*/
/********************************************************************************/

@Override public String getHelpLabel(MouseEvent e)
{
   if (!BudaRoot.showHelpTips()) return null;

   Rectangle r = getBounds();
   Dimension totsize = bubble_area.getSize();
   int x = e.getX();

   BudaWorkingSetImpl cws = null;
   for (BudaWorkingSetImpl ws : buda_root.getWorkingSetImpls()) {
      Rectangle wsr = ws.getRegion();
      int x0 = wsr.x * r.width / totsize.width;
      int x1 = (wsr.x + wsr.width) * r.width / totsize.width;
      if (x > x0 && x < x1) {
	 cws = ws;
	 break;
       }
    }

   if (cws != null) {
      return "topbarworkingset";
    }

   return "topbar";
}




@Override public String getHelpText(MouseEvent e)
{
   return null;
}



/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

@Override protected void paintComponent(Graphics g0)
{
   Graphics2D g = (Graphics2D) g0.create();

   Rectangle r = getBounds();

   Paint p = new GradientPaint(0f, 0f, TOP_BAR_TOP_COLOR, 0f, this.getHeight(), TOP_BAR_BOTTOM_COLOR);

   g.setPaint(p);

   g.fillRect(0,0,r.width,r.height);

   Dimension totsize = bubble_area.getSize();

   Rectangle vp = overview_area.getViewPosition();
   boolean inws = false;
   //modified by Ian Strickman
   //hard to factor out code because widths are all different
   for (BudaWorkingSetImpl ws : buda_root.getWorkingSetImpls()) {
      Rectangle wsr = ws.getRegion();
      if (wsr.intersects(vp)) inws = true;
      Color c = ws.getTopColor();
      int x0 = wsr.x * r.width / totsize.width;
      int x1 = (wsr.x + wsr.width) * r.width / totsize.width;
      int x2 = x1;
      x1 -= 4*r.height/5; // Distance used to make space for the chevron button
      g.setColor(c);
      g.fillRect(x0,0,x2-x0,r.height);
      String lbl = ws.getLabel();
      if (lbl != null && lbl.length() > 0) {
	 Color c1 = ws.getTextColor();
	 g.setColor(c1);
	 Rectangle r1 = new Rectangle(x0,0,x1-x0,r.height);
	 SwingText.drawText(lbl,g,r1);
	 ChevronButton but = chevron_buttons.get(ws);
	 if (but == null) {
	    chevron_buttons.put(ws, new ChevronButton(ws, x1, r.height/2, x2-x1, r.height/2));
	    but = chevron_buttons.get(ws);
	 }
	 else {
	    remove(but);
	    but.setBounds(x1, r.height/2, x2-x1, r.height/2);
	 }
	 add(but);
       }
    }

   if (!inws && BUDA_PROPERTIES.getBoolean(OVERVIEW_FAKE_WS_BOOL)) {
      int x0 = vp.x * r.width / totsize.width;
      int x1 = (vp.x + vp.width) * r.width / totsize.width;
      x1 -= 4*r.height/5;
      int x2 = (vp.x + vp.width) * r.width / totsize.width;
      g.setColor(OVERVIEW_FAKE_TOP_COLOR);
      g.fillRect(x0,0,x2-x0,r.height);
      g.setColor(OVERVIEW_FAKE_TEXT_COLOR);
      SwingText.drawText("Create Task...", g, new Rectangle(x0,0,x1-x0,r.height));
      if (fake_chevron_button == null) {
	 fake_chevron_button = new ChevronButton(null, x1,r.height/2, x2-x1, r.height/2);
       }
      else {
	 remove(fake_chevron_button);
	 fake_chevron_button.setBounds(x1, r.height/2, x2-x1, r.height/2);
       }
      add(fake_chevron_button);
      overview_area.setFakeWorkingSet(true);
      // overview_area.repaint();
    }
   else {
      overview_area.setFakeWorkingSet(false);
      if (fake_chevron_button != null) remove(fake_chevron_button);
      fake_chevron_button = null;
    }
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(BudaXmlWriter xw)
{
   xw.begin("TOPBAR");
   xw.element("SHAPE",getBounds());
   xw.element("LABEL",window_label);
   xw.end("TOPBAR");
}




/********************************************************************************/
/*										*/
/*	Working Set Sizing methods						*/
/*										*/
/********************************************************************************/

private WorkingSetResizeContext startWorkingSetSizing(int x)
{
   Rectangle r = getBounds();
   Dimension totsize = bubble_area.getSize();

   for (BudaWorkingSetImpl ws : buda_root.getWorkingSetImpls()) {
      Rectangle wsr = ws.getRegion();
      int x0 = wsr.x * r.width / totsize.width;
      int x1 = (wsr.x + wsr.width) * r.width / totsize.width;
      if (Math.abs(x-x0) <= TOP_RESIZE_DELTA) {
	 return new WorkingSetResizeContext(ws,x,true);
       }
      if (Math.abs(x-x1) <= TOP_RESIZE_DELTA) {
	 return new WorkingSetResizeContext(ws,x,false);
       }
    }

   return null;
}



//returns a number less than zero if you would resize from the left,
//greater than zero if you would resize from the right
//and zero if you wouldn't resize

private int wouldStartWorkingSetSizing(int x)
{
   Rectangle r = getBounds();
   Dimension totsize = bubble_area.getSize();

   for (BudaWorkingSetImpl ws : buda_root.getWorkingSetImpls()) {
      Rectangle wsr = ws.getRegion();
      int x0 = wsr.x * r.width / totsize.width;
      int x1 = (wsr.x + wsr.width) * r.width / totsize.width;
      if (Math.abs(x-x0) <= TOP_RESIZE_DELTA) {
	 return -1;
       }
      if (Math.abs(x-x1) <= TOP_RESIZE_DELTA) {
	 return 1;
       }
    }

   return 0;
}


private class WorkingSetResizeContext {

   private BudaWorkingSetImpl	working_set;
   private int			initial_x;
   private int			ws_bound;
   private Rectangle		top_bounds;
   private Rectangle		area_bounds;
   private boolean		left_side;
   private double		scale_factor;
   private int			resize_count;

   WorkingSetResizeContext(BudaWorkingSetImpl ws,int x0,boolean left) {
      working_set = ws;
      initial_x = x0;
      left_side = left;
      top_bounds = getBounds();
      area_bounds = bubble_area.getBounds();
      ws.setBeingChanged(true);
      Rectangle r = ws.getRegion();
      if (left_side) ws_bound = r.x;
      else ws_bound = r.x + r.width;
      scale_factor = ((double) area_bounds.width)/(top_bounds.width);
      resize_count = 0;
    }

   void next(MouseEvent e) {
      ++resize_count;
      int x1 = e.getX();
      int dx = x1 - initial_x;
      int bx = (int)(ws_bound + dx * scale_factor + 0.5);
      if (bx < 0) bx = 0;
      if (bx >= area_bounds.width) bx = area_bounds.width-1;
      Rectangle r = working_set.getRegion();
      if (left_side && bx >= r.x + r.width - MIN_WORKING_SET_SIZE)
	 bx = r.x+r.width-MIN_WORKING_SET_SIZE;
      else if (!left_side && bx <= r.x + MIN_WORKING_SET_SIZE)
	 bx = r.x+MIN_WORKING_SET_SIZE;
      if (left_side) {
	 int rx = r.x + r.width;
	 r.x = bx;
	 r.width = rx - bx;
       }
      else r.width = bx - r.x;
      working_set.setRegion(r);
      buda_root.repaint();
    }

   void finish() {
      working_set.setBeingChanged(false);
      if (resize_count > 0) BoardMetrics.noteCommand("BUDA","topbarResizeWorkingSet");
    }

}	// end of inner class WorkingSetResizeContext




/********************************************************************************/
/*										*/
/*	WorkingSet renaming methods						*/
/*										*/
/********************************************************************************/

private void startWorkingSetRename(int x)
{
   Rectangle r = getBounds();
   Dimension totsize = bubble_area.getSize();

   for (BudaWorkingSetImpl ws : buda_root.getWorkingSetImpls()) {
      Rectangle wsr = ws.getRegion();
      int x0 = wsr.x * r.width / totsize.width;
      int x1 = (wsr.x + wsr.width) * r.width / totsize.width;
      if (x > x0 + TOP_RESIZE_DELTA && x < x1 - TOP_RESIZE_DELTA) {
	 handleWorkingSetRename(ws,x0,x1);
	 break;
       }
    }
}



private void handleWorkingSetRename(BudaWorkingSetImpl ws,int x0,int x1)
{
   String txt = ws.getLabel();
   JTextField td = new JTextField(txt);
   Font ft = td.getFont();
   ft = ft.deriveFont(9f);
   td.setFont(ft);
   add(td);
   td.setBounds(x0,0,x1-x0,getHeight());

   new WorkingSetRenamer(ws,td);

   td.grabFocus();
   if (txt == null) td.setCaretPosition(0);
   else {
      td.setSelectionStart(0);
      td.setSelectionEnd(txt.length());
    }
}




private static class WorkingSetRenamer extends MouseAdapter implements ActionListener, FocusListener
{
   private BudaWorkingSetImpl working_set;
   private JTextField	  text_field;

   WorkingSetRenamer(BudaWorkingSetImpl ws,JTextField tf) {
      working_set = ws;
      text_field = tf;
      tf.addActionListener(this);
      tf.addFocusListener(this);
      tf.addMouseListener(this);
      tf.grabFocus();
    }

   @Override public void actionPerformed(ActionEvent e) {
      finishNaming();
    }

   @Override public void focusGained(FocusEvent e)	{ }

   @Override public void focusLost(FocusEvent e) {
      finishNaming();
    }

   @Override public void mouseExited(MouseEvent e) {
      finishNaming();
    }

   private void finishNaming() {
      text_field.removeActionListener(this);
      text_field.removeFocusListener(this);
      text_field.removeMouseListener(this);
      String t = text_field.getText();
      if (t.length() == 0) t = null;
      working_set.setLabel(t);
      Container c = text_field.getParent();
      c.remove(text_field);
      c.repaint();
      BoardMetrics.noteCommand("BUDA","topbarRenameWorkingSet");
    }

}	// end of inner class WorkingSetRenamer




/********************************************************************************/
/*										*/
/*	Popup menu methods							*/
/*										*/
/********************************************************************************/

private void handlePopup(MouseEvent e)
{
   Rectangle r = getBounds();
   Dimension totsize = bubble_area.getSize();
   int x = e.getX();
   popup_point = e.getPoint();

   cur_workingset = null;
   for (BudaWorkingSetImpl ws : buda_root.getWorkingSetImpls()) {
      Rectangle wsr = ws.getRegion();
      int x0 = wsr.x * r.width / totsize.width;
      int x1 = (wsr.x + wsr.width) * r.width / totsize.width;
      if (x > x0 && x < x1) {
	 cur_workingset = ws;
	 break;
       }
    }

   if (cur_workingset == null) {
      open_shares = buda_root.getShareManager().getAllShares();
      loadshare_button.setEnabled(open_shares != null && open_shares.size() > 0);

      bubble_menu.validate();
      buda_root.setChannelSet(null);
      Collection<BudaTask> bt = buda_root.getAllTasks();

      if (bt.size() > 0) {
	 bubble_menu.show(e.getComponent(),e.getX()-bubble_menu.getPreferredSize().width,e.getY());
	 BudaTask [] tsks = new BudaTask[bt.size()];
	 bt.toArray(tsks);
	 task_shelf = new BudaTaskShelf(this, tsks, popup_point);
	 task_dummy_menu.removeAll();
	 task_dummy_menu.add(task_shelf);
	 task_dummy_menu.setPopupMenuVisible(true);
	 task_shelf.setPopup(task_dummy_menu.getPopupMenu());
	 task_shelf.setBrotherPopup(bubble_menu);
      }
      else bubble_menu.show(e.getComponent(),e.getX(),e.getY());
    }
   else {
      if (cur_workingset.isShared())
	 share_button.setText("Stop Sharing Working Set");
      else
	 share_button.setText("Share Working Set");
      workingset_menu.show(e.getComponent(),e.getX(),e.getY());
    }
}



/********************************************************************************/
/*										*/
/*	Working set Cheveron Button methods					*/
/*										*/
/********************************************************************************/

private class ChevronButton extends JButton {

   private static final long serialVersionUID = 1L;

   ChevronButton(BudaWorkingSetImpl ws, int x, int y, int width, int height) {
      super();
      setIcon(new ImageIcon(BoardImage.getImage("dropdown_chevron").getScaledInstance(width, height, Image.SCALE_SMOOTH)));
      setBounds(x, y, width, height);
      addActionListener(new ChevronButtonListener(this,ws));
      setFocusPainted(false);
      setContentAreaFilled(false);
      setBorderPainted(false);
      BudaCursorManager.setCursor(this, new Cursor(Cursor.HAND_CURSOR));
    }

}	// end of inner class ChevronButton



private class ChevronButtonListener implements ActionListener {

   private JButton for_button;
   private BudaWorkingSetImpl my_workingset;

   ChevronButtonListener(JButton btn,BudaWorkingSetImpl ws) {
      for_button = btn;
      my_workingset = ws;
    }

   @Override public void actionPerformed(ActionEvent e) {
      if (my_workingset != null) {
	 cur_workingset = my_workingset;
	 workingset_menu.show(BudaTopBar.this, for_button.getX(),
				 for_button.getY()+for_button.getHeight());
       }
      else {
	 bubble_menu.show(BudaTopBar.this, for_button.getX(),
			     for_button.getY()+for_button.getHeight());
       }
      popup_point = for_button.getLocation();
    }

}	// end of inner class ChevronButtonListener




/********************************************************************************/
/*										*/
/*	Mouse management methods						*/
/*										*/
/********************************************************************************/

private class Mouser extends MouseAdapter {

   private WorkingSetResizeContext resize_context;

   Mouser() {
      resize_context = null;
    }

   @Override public void mousePressed(MouseEvent e) {
      if (checkPopup(e)) return;
      if (e.getButton() == MouseEvent.BUTTON1) {
	 if (wouldStartWorkingSetSizing(e.getX()) != 0) resize_context = startWorkingSetSizing(e.getX());
	 else {
	    Rectangle vp = overview_area.getViewPosition();
	    Rectangle r = BudaTopBar.this.getBounds();
	    Dimension totarea = BudaTopBar.this.bubble_area.getSize();
	    int x0 = vp.x*r.width/totarea.width;
	    int x2 = (vp.x+vp.width)*r.width/totarea.width;
	    if (e.getX() > x0 && e.getX() < x2) {
	       buda_root.handleNewWorkingSet(BudaTopBar.this, "Create Task...");
	       startWorkingSetRename(e.getX());
	    }
	 }
       }
    }

   @Override public void mouseReleased(MouseEvent e) {
      if (resize_context != null) {
	 resize_context.finish();
	 resize_context = null;
       }
      else if (checkPopup(e)) return;
    }

   @Override public void mouseClicked(MouseEvent e) {
      if (checkPopup(e)) return;

      if (e.getButton() == MouseEvent.BUTTON1) buda_root.setChannelSet(null);

      if (e.getButton() == MouseEvent.BUTTON1 && resize_context == null) {
	 startWorkingSetRename(e.getX());
       }
    }

   @Override public void mouseDragged(MouseEvent e) {
      if (resize_context != null) {
	 resize_context.next(e);
       }
    }

   @Override public void mouseMoved(MouseEvent e) {
      if (wouldStartWorkingSetSizing(e.getX()) != 0) BudaCursorManager.setTemporaryCursor(BudaTopBar.this, new Cursor(Cursor.E_RESIZE_CURSOR));
      else BudaCursorManager.setTemporaryCursor(BudaTopBar.this, new Cursor(Cursor.DEFAULT_CURSOR));
   }

   private boolean checkPopup(MouseEvent e) {
      if (e.isPopupTrigger()) {
	 handlePopup(e);
	 return true;
       }
      return false;
   }

}	// end of inner class Mouser





/********************************************************************************/
/*										*/
/*	Shelf menu implementation						*/
/*										*/
/********************************************************************************/

private static class ShelfMenu extends JMenu {

   private static final long serialVersionUID = 1L;

   ShelfMenu() {
      super();
      JPopupMenu popup = getPopupMenu();
      popup.setBorder(null);
   }

   @Override public void setSelected(boolean b) {
      super.setSelected(true);
   }

   @Override protected Point getPopupMenuOrigin() {
      Point pt = super.getPopupMenuOrigin();
      pt.x += 10;
      return pt;
   }

}	// end of inner class ShelfMenu




/********************************************************************************/
/*										*/
/*	Task Shelf killing methods						*/
/*										*/
/********************************************************************************/


private class ShelfListener implements PopupMenuListener {

   @Override public void popupMenuCanceled(PopupMenuEvent e) { }
   @Override public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) { }

   @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
      task_dummy_menu.setPopupMenuVisible(false);
   }

}	// end of class ShelfListener


}	// end of class BudaTopBar




/* end of BudaTopBar.java */

