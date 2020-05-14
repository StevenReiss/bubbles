/********************************************************************************/
/*										*/
/*		BaleCompletionContext.java					*/
/*										*/
/*	Bubble Annotated Language Editor context for auto completion		*/
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
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaBubbleLink;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaDefaultPort;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bueno.BuenoConstants;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;
import edu.brown.cs.bubbles.bump.BumpLocation;

import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.swing.SwingText;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.MatteBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



class BaleCompletionContext implements BaleConstants, CaretListener, BuenoConstants, BudaConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BaleEditorPane	for_editor;
private BaleDocument	for_document;
private BalePosition	start_position;
private BalePosition	end_position;
private Collection<BumpCompletion> found_completions;
private Collection<CompletionItem> found_items;
private CompletionGetter getter_thread;
private long		start_time;
private JDialog 	cur_menu;
private EditMouser	edit_mouser;
private EditKeyer	edit_keyer;
private EditFocus	edit_focus;
private CompletionPanel the_panel;

private static Map<String,Boolean> package_names;

private static boolean	       case_insensitive;
private static boolean         use_relevance;

private static long	completion_delay;

private static final int	SHOW_ITEMS = 8;
private static final int	X_DELTA = 0;
private static final int	Y_DELTA = 0;
private static final Pattern	ID_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z_0-9]*");

static {
   completion_delay = BALE_PROPERTIES.getLong(BALE_AUTOCOMPLETE_DELAY,0);
   case_insensitive = BALE_PROPERTIES.getBoolean("Bale.autocomplete.nocase");
   package_names = new ConcurrentHashMap<String,Boolean>();
   use_relevance = BALE_PROPERTIES.getBoolean("Bale.autocomplete.relevance");
}


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleCompletionContext(BaleEditorPane edt,int soff,char ch)
{
   for_editor = edt;
   for_document = edt.getBaleDocument();
   start_time = System.currentTimeMillis();
   cur_menu = null;
   the_panel = null;
   found_items = null;

   try {
      start_position = (BalePosition) for_document.createPosition(soff);
      end_position = (BalePosition) for_document.createPosition(soff+1);
    }
   catch (BadLocationException e) {
      return;
    }

   getter_thread = new CompletionGetter();
   BoardThreadPool.start(getter_thread);

   synchronized (this) {
      for_editor.addCaretListener(this);
      edit_mouser = new EditMouser();
      for_editor.addMouseListener(edit_mouser);
      edit_keyer = new EditKeyer();
      for_editor.addKeyListener(edit_keyer);
      edit_focus = new EditFocus();
      for_editor.addFocusListener(edit_focus);
      for_editor.setCompletionContext(this);
    }
}



/********************************************************************************/
/*										*/
/*	Methods to handle editing						*/
/*										*/
/********************************************************************************/

void handleSelected()
{
   if (the_panel == null) return;
   CompletionItem ci = the_panel.getCurrentCompletion();
   if (ci == null) return;
   handleCompletion(ci);
}



private synchronized void removeContext()
{
   if (for_editor == null) return;

   BoardLog.logD("BALE","Remove autocomplete");

   for_editor.setCompletionContext(null);
   for_editor.removeCaretListener(this);
   for_editor.removeMouseListener(edit_mouser);
   for_editor.removeKeyListener(edit_keyer);
   for_editor.removeFocusListener(edit_focus);
   for_editor = null;
   for_document = null;
   if (cur_menu != null) {
      cur_menu.setVisible(false);
      cur_menu.dispose();
      cur_menu = null;
    }
   if (getter_thread != null) BoardThreadPool.finish(getter_thread);
}



@Override public void caretUpdate(CaretEvent e)
{
   int dot = e.getDot();
   if (dot != end_position.getOffset()) removeContext();
   else if (dot != e.getMark()) removeContext();
   else {
      restrictOptions();
      //TODO: handle moving the menu
    }
}



private class EditMouser extends MouseAdapter {

   @Override public void mousePressed(MouseEvent e) {
      removeContext();
    }

}	// end of inner class EditMouser


private class EditKeyer extends KeyAdapter {

   @Override public void keyPressed(KeyEvent e) {
      if (the_panel == null) return;
      int code = e.getKeyCode();
      int ch = e.getKeyChar();
      BoardLog.logD("BALE","CONTEXT KEY CODE " + code);
      if (code == KeyEvent.VK_KP_UP || code == KeyEvent.VK_UP) {
	 the_panel.decCurrentIndex();
	 e.consume();
       }
      if (code == KeyEvent.VK_KP_DOWN || code == KeyEvent.VK_DOWN) {
	 the_panel.incCurrentIndex();
	 e.consume();
       }

      if (code == KeyEvent.VK_ENTER) {
	 the_panel.handleCurrentIndex();
	 e.consume();
       }
      if (code == KeyEvent.VK_TAB) {
	 the_panel.handleCurrentIndex();
	 e.consume();
       }
      if (code == KeyEvent.VK_SPACE && (e.isControlDown() || e.isMetaDown())) {
	 the_panel.handleCurrentIndex();
	 e.consume();
       }

      if (ch == '(' || ch == ')' || ch == ' ' || ch == ';' || ch == '*' || ch == ',') {
	 removeContext();
      }
    }

   @Override public void keyTyped(KeyEvent e) {
      if (e.getKeyChar() == KeyEvent.CHAR_UNDEFINED) {
	 removeContext();
       }
    }

}	// end of inner class EditKeyer



private class EditFocus extends FocusAdapter {

  @Override public void focusLost(FocusEvent e) {
     removeContext();
  }

}	// end of inner class EditFocus



/********************************************************************************/
/*										*/
/*	Methods to handle user selection					*/
/*										*/
/********************************************************************************/

private void handleCompletion(CompletionItem ci)
{
   BaleEditorPane be = for_editor;
   if (be == null) return;

   String s = ci.getCompletionText();
   Document d = be.getDocument();

   removeContext();	// this clears for_editor, so do it once we have addition point

   if (ci.getStartIndex() >= 0) {
      int i = be.getBaleDocument().mapOffsetToJava(ci.getStartIndex());
      try {
	 d.remove(i,be.getCaretPosition()-i);
	 d.insertString(be.getBaleDocument().mapOffsetToJava(ci.getStartIndex()),s,null);
       }
      catch (BadLocationException e) { }
    }
   else {
      try {
	 d.insertString(be.getCaretPosition(),s,null);
       }
      catch (BadLocationException e) { }
    }

   ci.accepted(be);
}



/********************************************************************************/
/*										*/
/*	Methods to handle dialog box						*/
/*										*/
/********************************************************************************/

private synchronized void handleFound(Collection<BumpCompletion> fnd,boolean calls)
{
   if (for_editor == null) return;	// no longer relevant
   if (fnd == null || fnd.size() == 0) return;

   found_completions = new TreeSet<>(new CompletionComparator());
   found_completions.addAll(fnd);

   long now = System.currentTimeMillis();
   long delay = start_time + completion_delay - now;
   if (delay < 0) delay = 0;
   Timer t = new Timer((int) delay,new CompletionShower());
   t.setRepeats(false);
   t.start();
}



private synchronized void handleShow()
{
   BaleEditorPane be = for_editor;

   if (be == null) return;	// no longer relevant
   if (!be.isFocusOwner()) {
      removeContext();
      return;
    }

   the_panel = new CompletionPanel();
   found_items = new ArrayList<>();
   for (BumpCompletion bc : found_completions) {
      CompletionItem citm = null;
      if (bc.getCompletion().length() == 0)
	 citm = new CompletionItemBumpCall(bc);
      else
	citm = new CompletionItemBump(bc);
      found_items.add(citm);
    }
   restrictOptions();

   if (for_editor == null) return;

   try {
      int soff = be.getCaretPosition();
      Rectangle r = SwingText.modelToView2D(be,soff);
      Window w = SwingUtilities.windowForComponent(be);
      cur_menu = new JDialog(w);
      cur_menu.setFocusable(false);
      cur_menu.setFocusableWindowState(false);
      cur_menu.setTitle("Completions");
      cur_menu.setUndecorated(true);
      cur_menu.setContentPane(the_panel);
      Point p0 = be.getLocationOnScreen();
      the_panel.setSize();
      // cur_menu.setLocation(p0.x + r.x + X_DELTA,p0.y + r.y + r.height + Y_DELTA);
      cur_menu.pack();
      Dimension d1 = cur_menu.getSize();
      GraphicsConfiguration gc = be.getGraphicsConfiguration();
      Rectangle r2 = gc.getBounds();
      int x = p0.x + r.x + X_DELTA;
      if (x + d1.width >= r2.width) x = p0.x + r.x - X_DELTA - d1.width;
      int y = p0.y + r.y + r.height + Y_DELTA;
      if (y + d1.height >= r2.height) y = p0.y + r.y - d1.height - Y_DELTA;
      cur_menu.setLocation(x,y);

      cur_menu.setVisible(true);
      the_panel.setCurrentIndex(0);
      be.grabFocus();
      BoardLog.logD("BALE","Show autocomplete");
    }
   catch (BadLocationException e) {
      removeContext();
    }
}



private synchronized void restrictOptions()
{
   BaleEditorPane be = for_editor;
   if (be == null) return;
   if (found_items == null) return;

   int off0 = start_position.getOffset() + 1;
   int off1 = be.getCaretPosition();
   if (found_items.isEmpty()) {
      removeContext();
      return;
    }
   else {
      CompletionItem ci = found_items.iterator().next();
      off0 = be.getBaleDocument().mapOffsetToJava(ci.getStartIndex());
    }

   try {
      String text1 = null;
      if (off0 < off1) {
	 text1 = be.getText(off0,off1-off0);
       }
      else if (off0 == off1) text1 = null;
      else {
	 removeContext();
	 return;
       }

      List<CompletionItem> rslt = new ArrayList<CompletionItem>();
      if (text1 == null || text1.length() == 0) rslt.addAll(found_items);
      else {
	 for (CompletionItem ci : found_items) {
	    if (ci.canStartWith(text1)) rslt.add(ci);
	  }
       }
      if (rslt.size() == 0) {
	 CompletionItem ci = getNewItem(text1);
	 if (ci != null) rslt.add(ci);
       }

      if (rslt.size() == 0) removeContext();
      else the_panel.setItems(rslt);
      the_panel.setCurrentIndex(0);
    }
   catch (BadLocationException e) {
      removeContext();
    }
}



private CompletionItem getNewItem(String text)
{
   if (text == null || text.length() == 0) return null;
   if (!isValidId(text)) return null;

   BaleDocument doc = for_editor.getBaleDocument();
   String proj = doc.getProjectName();

   String cls = null;
   Set<String> done = new HashSet<>();
   for (BumpCompletion bc : found_completions) {
      if (bc.getType() == BumpConstants.CompletionType.METHOD_REF) {
	 String dcls = bc.getDeclaringType();
	 if (dcls == null) continue;
	 int i2 = dcls.indexOf("<");
	 if (i2 > 0) dcls = dcls.substring(0,i2);
	 int i1 = dcls.lastIndexOf(".");
	 if (i1 < 0) {
	    cls = dcls;
	    break;
	  }
	 else {
	    String pnm = dcls.substring(0,i1);
	    if (done.contains(pnm)) continue;
	    done.add(pnm);
	    if (isPackage(proj,pnm)) {
	       cls = dcls;
	       break;
	    }
	  }
       }
    }

   if (cls == null) return null;

   return new CompletionItemNewMethod(cls,text);
}





private boolean isPackage(String proj,String pnm)
{
   String key = proj + "@" + pnm;
   if (package_names.containsKey(key)) return package_names.get(key);

   BumpClient bcc = BumpClient.getBump();
   List<BumpLocation> locs = bcc.findPackage(proj,pnm);
   boolean fg = (locs != null && locs.size() > 0);
   package_names.put(key,fg);

   return fg;
}




private static boolean isValidId(String text)
{
   if (text == null) return false;

   Matcher m = ID_PATTERN.matcher(text);

   return m.matches();
}



/********************************************************************************/
/*										*/
/*	Methods to get the completion set asynchronously			*/
/*										*/
/********************************************************************************/

private class CompletionGetter implements Runnable {

   CompletionGetter() { }

   @Override public void run() {
      if (start_position == null || for_document == null) return;
      int spos = for_document.mapOffsetToEclipse(start_position.getOffset())+1;
      Collection<BumpCompletion> completions = null;

      int ctr = for_document.getEditCounter();
      BumpClient bcc = BumpClient.getBump();
      completions = bcc.getCompletions(for_document.getProjectName(),
					  for_document.getFile(),
					  ctr,spos);
      if (completions == null) {
	 removeContext();
	 return;
       }

      List<BumpCompletion> callcomps = null;

      for (Iterator<BumpCompletion> it = completions.iterator(); it.hasNext(); ) {
	 BumpCompletion bc = it.next();
	 switch (bc.getType()) {
	    case METHOD_REF :
	       if (bc.getCompletion() == null || bc.getCompletion().length() == 0) {
		  it.remove();
		  if (bc.getSignature() != null && bc.getCompletion() != null) {
		     if (callcomps == null) callcomps = new ArrayList<BumpCompletion>();
		     callcomps.add(bc);
		   }
		}
	       break;
	    case TYPE_REF :
	    case FIELD_REF :
	       if (bc.getCompletion() == null || bc.getCompletion().length() == 0) it.remove();
	       break;
	    default :
	       if (bc.getCompletion() == null || bc.getCompletion().length() == 0) it.remove();
	       // else it.remove();
	       break;
	  }
       }

      if (completions.size() == 0 && callcomps != null) {
	 handleFound(callcomps,true);
       }
      else  if (completions.size() == 0) {
	 removeContext();
      }
      else {
	 handleFound(completions,false);
       }
    }

   @Override public String toString() {
      String s = "BALE_CompletionGetter";
      if (for_document != null) s += "_" + for_document.getFile();
      if (start_position != null) s += "_" + start_position.getOffset();
      return s;
    }


}	// end of inner class CompletionGetter



private class CompletionShower implements ActionListener {

   @Override public void actionPerformed(ActionEvent e) {
      handleShow();
    }

}	// end of inner class CompletionShower



private static class CompletionComparator implements Comparator<BumpCompletion> {

   @Override public int compare(BumpCompletion c1,BumpCompletion c2) {
      int v = 0;
      
      if (use_relevance) {
         int r1 = c1.getRelevance();
         int r2 = c2.getRelevance();
         v = Integer.compare(r2,r1);
         if (v != 0) return v;
       }
      
      if (c1.getName() == null && c2.getName() == null) v = 0;
      else if (c1.getName() == null) v = -1;
      else if (c2.getName() == null) v = 1;
      else if (case_insensitive) v = c1.getName().compareToIgnoreCase(c2.getName());
      else v = c1.getName().compareTo(c2.getName());
      
      if (v != 0) return v;
      if (c1.getSignature() != null && c2.getSignature() != null) {
	 v = c1.getSignature().compareTo(c2.getSignature());
	 if (v != 0) return v;
       }
      String t1 = c1.getDeclaringType();
      String t2 = c2.getDeclaringType();
      if (t1 != null && t2 != null) {
	 v = t1.compareTo(t2);
	 if (v != 0) return v;
      }

      return 0;
    }

}	// end of inner class CompletionComparator




/********************************************************************************/
/*										*/
/*	Class to hold the completion options					*/
/*										*/
/********************************************************************************/

private class CompletionPanel extends JPanel implements MouseListener {

   CompletionList item_list;

   private static final long serialVersionUID = 1;

   CompletionPanel() {
      super(new BorderLayout());
      setFocusable(false);
      item_list = new CompletionList();
      item_list.setFocusable(false);
      item_list.setFocusable(true);
      item_list.setCellRenderer(new CompletionListCellRenderer());
      JScrollPane sp = new JScrollPane(item_list);
      sp.setFocusable(false);
      sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      add(sp,BorderLayout.CENTER);
      item_list.addMouseListener(this);
      setBorder(new MatteBorder(5,5,5,5,BoardColors.getColor("Bale.CompletionBorder")));
    }

   void setSize()				{ item_list.setSize(); }
   void setItems(List<CompletionItem> v) {
      item_list.setItems(v);
      item_list.setSize();
    }

   void handleCurrentIndex(){
      CompletionItem ci = item_list.getSelectedValue();
      if (ci == null) return;
      handleCompletion(ci);
   }

   void setCurrentIndex(int i){
      item_list.setSelectedIndex(i);
      item_list.ensureIndexIsVisible(i);
   }

   void decCurrentIndex(){
      if (item_list.getSelectedIndex() > 0)
      {
	 setCurrentIndex(item_list.getSelectedIndex() -1);
      }
   }

   void incCurrentIndex(){
      if (item_list.getSelectedIndex() < (item_list.getModel().getSize()-1))
      {
	 setCurrentIndex(item_list.getSelectedIndex()+1);
      }
   }

   CompletionItem getCurrentCompletion() {
      return item_list.getCurrentCompletion();
    }

   @Override public void mouseClicked(MouseEvent arg0) {
      super.processMouseEvent(arg0);
      CompletionItem ci = item_list.getSelectedValue();
      if (ci == null) return;
      handleCompletion(ci);
   }

   @Override public void mouseEntered(MouseEvent e)		{ }

   @Override public void mouseExited(MouseEvent e)		{ }

   @Override public void mousePressed(MouseEvent e)		{ }

   @Override public void mouseReleased(MouseEvent e)		{ }

}	// end of inner class CompletionPanel




private static class CompletionList extends JList<CompletionItem> {

   CompletionModel item_model;

   private static final long serialVersionUID = 1;


   CompletionList() {
      item_model = new CompletionModel();
      setModel(item_model);
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

   void setSize() {
      int sz = item_model.getSize();
      sz = Math.min(sz,SHOW_ITEMS);
      setVisibleRowCount(sz);
    }

   void setItems(List<CompletionItem> v) {
      item_model.removeAllElements();
      for (CompletionItem ci : v) item_model.addItem(ci);
    }

   CompletionItem getCurrentCompletion() {
      if (item_model.getSize() == 0) return null;
      return item_model.getElementAt(0);
    }

}	// end of inner class CompletionList




private static class CompletionModel extends DefaultListModel<CompletionItem> {

   private static final long serialVersionUID = 1;


   CompletionModel()				{ }

   void addItem(CompletionItem ci)		{ addElement(ci); }

}	// end of inner class CompletionModel





private static class CompletionListCellRenderer extends DefaultListCellRenderer {

   private static final long serialVersionUID = 1;

   @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
     {
	 Component renderedcomp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
	 CompletionItem ci = (CompletionItem) value;
	 Icon icn = ci.getIcon();
	 if (icn != null) ((JLabel) renderedcomp).setIcon(icn);

	 return renderedcomp;
     }

}	// end of inner class CompletionListCellRenderer




/********************************************************************************/
/*										*/
/*	Item management 							*/
/*										*/
/********************************************************************************/

private static abstract class CompletionItem {

   protected String param_types;
   protected String return_type;


   abstract String getCompletionText();
   abstract int getStartIndex();
   abstract boolean canStartWith(String text);
   abstract int getRelevance();
   Icon getIcon()					{ return null; }

   void accepted(BaleEditorPane editor) 		{ }

   protected void getSignatureObjects(String sgn) {
      param_types = "";
      return_type = "";

      if (sgn == null) return;
      String s = IvyFormat.formatTypeName(sgn);
      if (s == null) return;

      int parenindex = s.indexOf('(');
      if (parenindex >= 0) {
	 return_type = s.substring(0,parenindex);
	 return_type = shortenType(return_type);
	 String temp = s.substring(parenindex+1,s.length()-1);
	 param_types = shortenType(temp);
       }
    }

   protected String shortenType(String typ) {
      StringTokenizer tok = new StringTokenizer(typ,".<>,",true);
      StringBuilder buf = new StringBuilder();
      String pfx = null;
      while (tok.hasMoreTokens()) {
	 String q = tok.nextToken();
	 if ("<>,".contains(q)) {
	    if (pfx != null) buf.append(pfx);
	    pfx = null;
	    buf.append(q);
	  }
	 else if (q.equals(".")) pfx = null;
	 else pfx = q;
       }
      if (pfx != null) buf.append(pfx);
      return buf.toString();
    }

}	// end of inner class CompletionItem



private static class CompletionItemNewMethod extends CompletionItem implements
	BuenoConstants.BuenoBubbleCreator
{
   private String class_name;
   private String method_name;
   private BaleEditorPane link_editor;
   private Position link_point;

   CompletionItemNewMethod(String cnm,String nm) {
      class_name = cnm;
      method_name = nm;
      link_editor = null;
      link_point = null;
    }

   @Override String getCompletionText() 	{ return "("; }

   @Override int getStartIndex()                { return -1; }
   
   @Override int getRelevance()                 { return 1; }

   @Override boolean canStartWith(String text) {
      method_name = text;
      if (text.length() == 0 || !isValidId(text)) return false;
      return true;
    }

   @Override Icon getIcon()			{ return BoardImage.getIcon("default_method"); }

   @Override public String toString() {
      return "Create new method " + method_name;
    }

   @Override void accepted(BaleEditorPane editor) {
      link_editor = editor;
      BaleDocument doc = editor.getBaleDocument();
      try {
	 link_point = doc.createPosition(editor.getCaretPosition());
       }
      catch (BadLocationException e) { return; }

      String proj = doc.getProjectName();

      BaleFactory.getFactory().createNewMethod(proj,class_name + "." + method_name,null,null,0,true,null,
						  editor,link_point,true,true);
    }

   @Override public void createBubble(String proj,String name,BudaBubbleArea bba,Point p) {
      BudaBubble bb = BaleFactory.getFactory().createMethodBubble(proj,name);
      if (bb == null) return;

      bba.addBubble(bb,null,p,PLACEMENT_MOVETO);
      // bba.add(bb,new BudaConstraint(p));

      if (link_editor != null) {
	 BudaBubble bbo = BudaRoot.findBudaBubble(link_editor);
	 BudaRoot root = BudaRoot.findBudaRoot(bbo);
	 if (bbo != null && root != null && bbo.isShowing() && link_point != null) {
	    BudaConstants.LinkPort p0 = new BaleLinePort(link_editor,link_point,null);
	    BudaConstants.LinkPort p1 = new BudaDefaultPort(BudaPortPosition.BORDER_EW_TOP,true);
	    BudaBubbleLink lnk = new BudaBubbleLink(bbo,p0,bb,p1);
	    root.addLink(lnk);
	  }
       }
    }

}	// end of inner class CompletionItemNewMethod



private static class CompletionItemBump extends CompletionItem {

   BumpCompletion bump_completion;

   CompletionItemBump(BumpCompletion bc) {
      bump_completion = bc;
      getSignatureObjects(bc.getSignature());
    }

   @Override public String toString() {
      String comp = bump_completion.getCompletion();
      if (comp.indexOf(')') >= 0) {
	 comp = comp.substring(0,comp.length()-1);
	 comp += param_types + ") : " + return_type;
	 return comp;
       }
      return comp;
    }
   
   @Override int getStartIndex() {
      return bump_completion.getReplaceStart();
    }
   
   @Override int getRelevance() {
      return bump_completion.getRelevance();
    }
   
   @Override boolean canStartWith(String txt) {
      String compl = bump_completion.getCompletion();
      if (compl.startsWith(txt)) return true;
      if (case_insensitive) {
	 if (compl.toLowerCase().startsWith(txt.toLowerCase())) return true;
       }
      if (bump_completion.getType() == CompletionType.PACKAGE_REF) return false;
      int i = compl.lastIndexOf('.');
      while (i != -1) {
	 if (compl.startsWith(txt,i+1)) return true;
	 i = compl.lastIndexOf('.',i-1);
       }
      return false;
    }

   @Override String getCompletionText() {
      String toreturn = bump_completion.getCompletion();
      if (toreturn == null) return null;
      int ln = toreturn.length();
      if (ln > 1 && toreturn.charAt(ln-1) == ')' &&
	     (param_types != null && param_types.length() > 0)){
	 toreturn = toreturn.substring(0, toreturn.length()-1);
       }
      return toreturn;
   }

   @Override Icon getIcon() {
      if (bump_completion.isPublic()) return BoardImage.getIcon("method");
      else if (bump_completion.isPrivate()) return BoardImage.getIcon("private_method");
      else if (bump_completion.isProtected()) return BoardImage.getIcon("protected_method");
      else return BoardImage.getIcon("default_method");
    }


}	// end of inner class CompletionItemBump




private static class CompletionItemBumpCall extends CompletionItem {

   BumpCompletion bump_completion;

   CompletionItemBumpCall(BumpCompletion bc) {
      bump_completion = bc;
      getSignatureObjects(bc.getSignature());
    }

   @Override public String toString() {
      String comp = bump_completion.getName() + "(" + param_types + ") : " + return_type;
      return comp;
    }

   @Override int getStartIndex() {
      return bump_completion.getReplaceStart();
    }
   
   @Override int getRelevance() {
      return bump_completion.getRelevance();
    }

   @Override boolean canStartWith(String txt) {
      return false;
    }

   @Override String getCompletionText() {
      return "";
    }

   @Override Icon getIcon() {
      if (bump_completion.isPublic()) return BoardImage.getIcon("method");
      else if (bump_completion.isPrivate()) return BoardImage.getIcon("private_method");
      else if (bump_completion.isProtected()) return BoardImage.getIcon("protected_method");
      else return BoardImage.getIcon("default_method");
    }


}	// end of inner class CompletionItemBumpCall


}	// end of class BaleCompletionContext




/* end of BaleCompletionContext.java */













