/********************************************************************************/
/*										*/
/*		BconClassPanel.java						*/
/*										*/
/*	Bubbles Environment Context Viewer class or file panel			*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.bcon;

import edu.brown.cs.bubbles.bale.BaleConstants.BaleFileOverview;
import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaConstraint;
import edu.brown.cs.bubbles.buda.BudaCursorManager;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;
import edu.brown.cs.bubbles.bueno.BuenoConstants;
import edu.brown.cs.bubbles.bueno.BuenoFactory;
import edu.brown.cs.bubbles.bueno.BuenoFieldDialog;
import edu.brown.cs.bubbles.bueno.BuenoInnerClassDialog;
import edu.brown.cs.bubbles.bueno.BuenoLocation;
import edu.brown.cs.bubbles.bueno.BuenoProperties;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.bubbles.burp.BurpHistory;

import edu.brown.cs.ivy.swing.SwingEnumButton;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.swing.SwingTextField;

import org.w3c.dom.Element;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Segment;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;



class BconClassPanel implements BconConstants, BconConstants.BconPanel,
	BumpConstants.BumpFileHandler, BumpConstants.BumpChangeHandler,
	BuenoConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private String			for_project;
private String			for_class;
private File			for_file;
private BaleFileOverview	bale_file;
private BumpClient		bump_client;
private Collection<BconRegion>	region_set;
private Selector		cur_selector;
private ClassListModel		list_model;
private ClassList		class_list;
private SwingEnumButton<Protections> protect_options;
private boolean 		inner_class;

private SwingGridPanel		class_panel;
private Point			popup_point;

private static BoardProperties	bcon_props = BoardProperties.getProperties("Bcon");


private static final int	MIN_LINES = 2;
private static final int	MAX_COMMENT_SIZE = 40;
private static final int	MAX_INITIAL_WIDTH = 350;


private static final int	MOD_PUBLIC = Modifier.PUBLIC;
private static final int	MOD_PRIVATE = Modifier.PRIVATE;
private static final int	MOD_PROTECTED = Modifier.PROTECTED;
private static final int	MOD_PACKAGE = 0x10000;

private static final int	MOD_STATIC = Modifier.STATIC;
private static final int	MOD_NONSTATIC = 0x200000;
private static final int	MOD_ABSTRACT = Modifier.ABSTRACT;
private static final int	MOD_NONABSTRACT = 0x400000;

private static final int	MOD_FIELD = 0x20000;
private static final int	MOD_METHOD = 0x40000;
private static final int	MOD_INITIALIZER = 0x80000;
private static final int	MOD_CLASS = 0x100000;
private static final int	MOD_INTERFACE = Modifier.INTERFACE;
private static final int	MOD_COMMENT = 0x800000;
private static final int	MOD_PACKAGEDECL = 0x1000000;
private static final int	MOD_IMPORT = 0x2000000;
private static final int	MOD_TOPDECL = 0x4000000;

private static final int	MOD_PROTECTION = MOD_PUBLIC | MOD_PRIVATE |
						 MOD_PROTECTED | MOD_PACKAGE;


private static enum Protections {
   PUBLIC,
   PROTECTED,
   PACKAGE,
   PRIVATE
}



private static final int	ALL_MODS =
	MOD_PUBLIC | MOD_PRIVATE | MOD_PROTECTED | MOD_PACKAGE |
	MOD_STATIC | MOD_NONSTATIC | MOD_ABSTRACT | MOD_NONABSTRACT |
	MOD_FIELD | MOD_METHOD | MOD_INITIALIZER | MOD_CLASS |
	MOD_INTERFACE | MOD_COMMENT |
	MOD_PACKAGEDECL | MOD_IMPORT | MOD_TOPDECL;


private static final DataFlavor region_flavor = new DataFlavor(ListTransfer.class,"File Region Set");




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BconClassPanel(String proj,File f,String cls,boolean inner)
{
   for_project = proj;
   for_class = cls;
   for_file = f;
   bump_client = BumpClient.getBump();
   bale_file = BaleFactory.getFactory().getFileOverview(proj,f);
   cur_selector = new Selector(ALL_MODS);
   list_model = new ClassListModel();
   inner_class = inner;

   region_set = new ConcurrentSkipListSet<>(new RegionComparator());

   setupElements();

   setupPanel();

   bump_client.addFileHandler(for_file,this);
   bump_client.addChangeHandler(this);
}




@Override public void dispose()
{
   bump_client.removeFileHandler(this);
   bump_client.removeChangeHandler(this);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public JComponent getComponent()		{ return class_panel; }

boolean isValid()
{
   if (bale_file == null) return false;
   if (bale_file.getLength() == 0) return false;
   return true;
}



/********************************************************************************/
/*                                                                              */
/*      Selection methods                                                       */
/*                                                                              */
/********************************************************************************/

void showElement(String name)
{
   if (name == null) return;
   
   name = normalizeName(name);
   
   BconRegion show = null;
   for (BconRegion rgn : region_set) {
      String rnm = rgn.getRegionName();
      rnm = normalizeName(rnm);
      if (rnm.equals(name))  {
         show = rgn;
         break;
       } 
    }
   if (show == null) {
      System.err.println("Problem finding region for " + name);
   }
   if (show == null) return;
   if (class_list == null) return;
   
   class_list.setSelectedValue(show,true);
}


private String normalizeName(String nm)
{
   nm = nm.replace("$",".");
   
   int idx = nm.indexOf("(");
   if (idx > 0) {
      List<String> args = new ArrayList<String>();
      StringBuffer buf = null;
      int lvl = 0;
      int i = idx+1;
      for ( ; i < nm.length(); ++i) {
         char ch = nm.charAt(i);
         if (ch == '<') ++lvl;
         else if (ch == '>') --lvl;
         else if (ch == ',' && lvl == 0) {
            if (buf != null) args.add(buf.toString());
            buf = null;
          }
         else if (ch == ')' && lvl == 0) {
            if (buf != null) args.add(buf.toString());
            buf = null;
            break;
          }
         else if (lvl == 0) {
            if (buf == null) buf = new StringBuffer();
            buf.append(ch);
          }
       }
      StringBuffer rslt = new StringBuffer();
      rslt.append(nm.substring(0,idx+1));
      int ct = 0;
      for (String s : args) {
          int idx1 = s.lastIndexOf(".");
          if (idx1 > 0) s = s.substring(idx1+1);
          if (ct++ > 0) rslt.append(",");
          rslt.append(s);
       }
      rslt.append(")");
      nm = rslt.toString();
    }
       
   return nm;
}




/********************************************************************************/
/*										*/
/*	Menu methods								*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   BudaBubble bbl = BudaRoot.findBudaBubble(class_panel);
   if (bbl == null) return;
   Point pt = SwingUtilities.convertPoint(bbl,e.getPoint(),class_list);
   int row = class_list.locationToIndex(pt);
   if (row < 0) return;
   BconRegion br = list_model.getElementAt(row);
   if (br == null) return;
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(class_list);
   if (bba == null) return;
   popup_point = SwingUtilities.convertPoint(class_list,pt,bba);

   JPopupMenu popup = new JPopupMenu();

   JMenu m1 = (JMenu) popup.add(new JMenu("Insert comment ..."));
   m1.add(new CommentCreator(BuenoType.NEW_MARQUIS_COMMENT,br,true));
   m1.add(new CommentCreator(BuenoType.NEW_BLOCK_COMMENT,br,true));
   m1.add(new CommentCreator(BuenoType.NEW_JAVADOC_COMMENT,br,true));
   m1.add(new CommentCreator(BuenoType.NEW_MARQUIS_COMMENT,br,false));
   m1.add(new CommentCreator(BuenoType.NEW_BLOCK_COMMENT,br,false));
   m1.add(new CommentCreator(BuenoType.NEW_JAVADOC_COMMENT,br,false));

   m1 = (JMenu) popup.add(new JMenu("Insert method ..."));
   m1.add(new MethodCreator(br,true));
   m1.add(new MethodCreator(br,false));

   m1 = (JMenu) popup.add(new JMenu("Insert field ..."));
   m1.add(new FieldCreator(br,true));
   m1.add(new FieldCreator(br,false));

   m1 = (JMenu) popup.add(new JMenu("Insert inner class ..."));
   m1.add(new TypeCreator(br,true));
   m1.add(new TypeCreator(br,false));

   popup.add(new ClassBubbleAction());
   popup.add(new DeleteAction(br));
   popup.add(new ResetAction());
   popup.add(BurpHistory.getUndoAction());

   popup.show(class_list,pt.x,pt.y);
}




/********************************************************************************/
/*										*/
/*	Panel setup methods							*/
/*										*/
/********************************************************************************/

private void setupPanel()
{
   class_panel = new ClassPanel();

   JLabel ttl = new JLabel(for_class);
   class_panel.addGBComponent(ttl,0,0,0,1,10,0);

   JToolBar tools = new JToolBar();
   tools.setFloatable(false);
   protect_options = new SwingEnumButton<Protections>(Protections.PRIVATE);
   protect_options.addValueListener(new ProtectionUpdater());

   tools.add(protect_options);
   JToggleButton jte = new JToggleButton("Methods");
   jte.addActionListener(new OptionUpdater(MOD_METHOD));
   jte.setSelected(cur_selector.check(MOD_METHOD));
   tools.add(jte);
   jte = new JToggleButton("Comments");
   jte.addActionListener(new OptionUpdater(MOD_COMMENT));
   jte.setSelected(cur_selector.check(MOD_COMMENT));
   tools.add(jte);
   jte = new JToggleButton("Types");
   jte.addActionListener(new OptionUpdater(MOD_CLASS|MOD_INTERFACE));
   jte.setSelected(cur_selector.check(MOD_CLASS|MOD_INTERFACE));
   tools.add(jte);
   jte = new JToggleButton("Fields");
   jte.addActionListener(new OptionUpdater(MOD_FIELD));
   jte.setSelected(cur_selector.check(MOD_FIELD));
   tools.add(jte);
   jte = new JToggleButton("Imports");
   jte.addActionListener(new OptionUpdater(MOD_IMPORT));
   jte.setSelected(cur_selector.check(MOD_IMPORT));
   tools.add(jte);

   class_panel.addGBComponent(tools,0,1,0,1,10,0);

   class_list = new ClassList();
   class_list.setVisibleRowCount(10);
   class_list.setDragEnabled(true);
   class_list.setDropMode(DropMode.INSERT);
   JScrollPane sp = new JScrollPane(class_list);
   sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
   Dimension d = sp.getPreferredSize();
   if (d.width > MAX_INITIAL_WIDTH) {
      d.width = MAX_INITIAL_WIDTH;
      sp.setPreferredSize(d);
    }

   class_panel.addGBComponent(sp,0,2,0,1,10,10);

   JTextField tfld = new SwingTextField();
   class_panel.addGBComponent(tfld,0,3,0,1,10,0);
   tfld.addActionListener(new FilterAction());

   BudaCursorManager.setCursor(class_panel,Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
}




/********************************************************************************/
/*										*/
/*	Button update methods							*/
/*										*/
/********************************************************************************/

private void updateSelection(int off,int on)
{
   cur_selector.reset(off,on);
   list_model.invalidate();
   class_panel.repaint();
}



private class ProtectionUpdater implements ChangeListener {

   @Override public void stateChanged(ChangeEvent evt) {
      Protections p = protect_options.getValue();
      int off = MOD_PROTECTION;

      int on = 0;
      switch (p) {
	 case PUBLIC :
	    on = MOD_PUBLIC;
	    break;
	 case PACKAGE :
	    on = MOD_PUBLIC | MOD_PACKAGE;
	    break;
	 case PROTECTED :
	    on = MOD_PUBLIC | MOD_PACKAGE | MOD_PROTECTED;
	    break;
	 case PRIVATE :
	    on = MOD_PUBLIC | MOD_PACKAGE | MOD_PROTECTED | MOD_PRIVATE;
	    break;
       }

      updateSelection(off,on);
    }

}	// end of inner class ProtectionUpdater



private class OptionUpdater implements ActionListener {

   private int using_flag;

   OptionUpdater(int fg) {
      using_flag = fg;
    }

   @Override public void actionPerformed(ActionEvent evt) {
      JToggleButton btn = (JToggleButton) evt.getSource();
      if (btn.isSelected()) {
	 updateSelection(using_flag,using_flag);
       }
      else {
	 updateSelection(using_flag,0);
       }
    }

}	// end of inner class OptionUpdater



/********************************************************************************/
/*										*/
/*	Methods to setup class/file elements					*/
/*										*/
/********************************************************************************/

private void setupElements()
{
   // should this be incremental to avoid jumping?

   region_set = new TreeSet<BconRegion>(new RegionComparator());

   String cnm = for_class;
   int idx = cnm.indexOf("<");
   if (idx > 0) cnm = cnm.substring(0,idx);

   List<BumpLocation> locs = bump_client.findAllDeclarations(for_project,null,cnm,false);
   if (locs != null) {
      for (BumpLocation bl : locs) {
	 BconRegion br = new BconRegionLocation(bale_file,bl);
	 if (br.getRegionType() != RegionType.REGION_UNKNOWN) region_set.add(br);
       }
    }

   int p0 = 0;
   int p1 = bale_file.getLength();
   if (inner_class) {
      List<BumpLocation> loc1 = bump_client.findClassDefinition(for_project,for_class);
      if (loc1 != null && loc1.size() > 0) {
	 BumpLocation loc0 = loc1.get(0);
	 p0 = bale_file.mapOffsetToJava(loc0.getOffset());
	 p1 = bale_file.mapOffsetToJava(loc0.getEndOffset());
       }
    }

   Segment s = new Segment();
   try {
      bale_file.getText(p0,p1-p0,s);
    }
   catch (BadLocationException e) {
      BoardLog.logE("BCON","Problem reading file",e);
    }

   commentScan(s,p0);
   headerScan(s,p0);

   list_model.invalidate();

   if (class_panel != null) class_panel.repaint();
}



/********************************************************************************/
/*										*/
/*	Comment scanning methods						*/
/*										*/
/********************************************************************************/

private void commentScan(Segment s,int offset)
{
   BconTokenizer btok = new BconTokenizer(s);
   BconToken cmmtstart = null;		// current comment start
   BconToken laststart = null;		// possible start of adjacent comments
   BconToken lastblock = null;
   int numline = 0;
   int blockline = 0;
   String cmmtname = null;		// label for current comment
   boolean iscopyright = false; 	// current is copyright
   boolean isjavadoc = false;		// current is javadoc
   boolean lasteol = true;		// prior token was an eol
   boolean forceout = false;

   for ( ; ; ) {
      BconToken tok = btok.getNextToken();
      if (tok == null) break;
      if (forceout && cmmtstart != null) {
	 outputComment(offset,cmmtstart,tok,iscopyright,isjavadoc,cmmtname);
	 cmmtstart = null;
	 iscopyright = false;
	 forceout = false;
       }

      switch (tok.getTokenType()) {
	 case EOL :
	    if (lasteol && iscopyright) forceout = true;
	    if (cmmtstart != null) numline++;
	    lasteol = true;
	    break;
	 case BLOCK_CMMT :
	 case DOC_CMMT :
	 case LINE_CMMT :
	    String ctxt = getCommentText(s,tok);
	    if (lasteol && cmmtstart == null) {
	       cmmtstart = tok;
	       laststart = null;
	       cmmtname = ctxt;
	       iscopyright = checkCopyright(ctxt);
	       isjavadoc = (tok.getTokenType() == BconTokenType.DOC_CMMT);
	       if (iscopyright) isjavadoc = false;
	       numline = 0;
	     }
	    else if (lasteol) {
	       if (cmmtname == null) {
		  cmmtname = ctxt;
		  iscopyright = checkCopyright(ctxt);
		}
	       else if (!iscopyright && lastblock != null && checkCopyright(ctxt)) {
		  if (blockline >= MIN_LINES) {
		     outputComment(offset,cmmtstart,lastblock,iscopyright,isjavadoc,cmmtname);
		   }
		  cmmtstart = lastblock;
		  laststart = null;
		  cmmtname = ctxt;
		  iscopyright = true;
		  isjavadoc = false;
		  numline = numline-blockline;
		}
	       else if (ctxt == null) {
		  lastblock = tok;
		  blockline = numline;
		}
	       isjavadoc &= (tok.getTokenType() == BconTokenType.DOC_CMMT);
	       laststart = tok;
	     }
	    lasteol = false;
	    break;
	 default:
	 case OTHER :
	    if (cmmtstart != null && numline >= MIN_LINES && cmmtname != null) {
	       BconToken etok = (lasteol ? tok : laststart);
	       outputComment(offset,cmmtstart,etok,iscopyright,isjavadoc,cmmtname);
	     }
	    cmmtstart = null;
	    lasteol = false;
	    iscopyright = false;
	    break;
       }
    }
}



private static final String START_CHARS = "$@&#%()<";
private static final String LEGAL_CHARS = "-,_$@'\"+&#%^()[]|<>\\.;:/=";


private String getCommentText(Segment seg,BconToken tok)
{
   StringBuffer buf = new StringBuffer();
   boolean intext = false;

   for (int i = 0; i < tok.getLength(); ++i) {
      char c = seg.charAt(tok.getStart() + i);
      if (!intext && (Character.isLetter(c) || START_CHARS.indexOf(c) >= 0)) intext = true;
      if (intext) {
	 if (Character.isLetterOrDigit(c)) buf.append(c);
	 else if (Character.isWhitespace(c)) buf.append(' ');
	 else if (LEGAL_CHARS.indexOf(c) >= 0) buf.append(c);
	 else break;
       }
    }

   if (buf.length() == 0) return null;

   return buf.toString().trim();
}



private boolean checkCopyright(String txt)
{
   if (txt == null) return false;

   txt = txt.trim().toLowerCase();
   if (txt.startsWith("copyright")) return true;

   return false;
}



/********************************************************************************/
/*										*/
/*	Methods for creating comment regions					*/
/*										*/
/********************************************************************************/

private void outputComment(int offset,BconToken start,BconToken end,boolean cpy,boolean jdoc,String nm)
{
   if (nm == null) nm = "<empty>";

   if (nm.startsWith("$") || cpy) jdoc = false;

   int spos = start.getStart() + offset;
   int epos = end.getStart() + offset;

   if (jdoc) {
      int idx = nm.indexOf(". ");
      if (idx > 0) nm = nm.substring(0,idx);
    }

   for (BconRegion br : region_set) {
      if (br.getStartOffset() < spos && br.getEndOffset() >= epos + end.getLength()) {
	 return;
      }
    }

   if (!jdoc) {
      for (BconRegion br : region_set) {
	 if (br.getStartOffset() == spos && epos < br.getEndOffset()) {
	    br.setPosition(epos,br.getEndOffset());
	    break;
	  }
       }
    }

   BconRegion brc = new BconRegionComment(bale_file,spos,epos,nm,cpy);
   region_set.add(brc);
}



/********************************************************************************/
/*										*/
/*	Scan for header elements						*/
/*										*/
/********************************************************************************/

private void headerScan(Segment s,int offset)
{
   if (!inner_class) {
      List<BumpLocation> locs = bump_client.findClassHeader(for_project,for_file,for_class,true,false);
      if (locs != null) {
	 for (BumpLocation bl : locs) {
	    addHeaderRegion(bl,RegionType.REGION_PACKAGE,s,offset);
	 }
      }
      locs = bump_client.findClassHeader(for_project,for_file,for_class,false,true);
      if (locs != null) {
	 for (BumpLocation bl : locs) {
	    addHeaderRegion(bl,RegionType.REGION_IMPORT,s,offset);
	 }
      }
    }

   List<BumpLocation> locs = bump_client.findClassHeader(for_project,for_file,for_class,false,false);
   if (locs != null) {
      for (BumpLocation bl : locs) {
	 addHeaderRegion(bl,RegionType.REGION_CLASS_HEADER,s,offset);
       }
    }
}



void addHeaderRegion(BumpLocation bl,RegionType rt,Segment s,int offset)
{
   int spos = bale_file.mapOffsetToJava(bl.getOffset())-offset;
   int epos = bale_file.mapOffsetToJava(bl.getEndOffset())-offset;
   if (spos < 0 || epos >= s.length() || epos < spos) return;

   if (rt == RegionType.REGION_CLASS_HEADER) {
      for (BconRegion br : region_set) {
	 if (br.getStartOffset() <= spos+offset && br.getEndOffset() > spos+offset) {
	    spos = br.getStartOffset()-offset;
	  }
       }
   }

   StringBuffer buf = new StringBuffer();
   for (int i = spos; i < epos; ++i) {
      char ch = s.charAt(i);
      if (ch == '\n') break;
      buf.append(s.charAt(i));
    }

   // go to start of line if possible
   while (spos > 0) {
      char c = s.charAt(spos-1);
      if (c == '\n') break;
      if (!Character.isWhitespace(c)) break;
      --spos;
    }

   // go to end of line if possible
   while (epos < s.length()) {
      char c = s.charAt(epos);
      if (c == '\n') {
	 ++epos;
	 break;
       }
      else if (!Character.isWhitespace(c)) break;
      ++epos;
    }

   BconRegion br = new BconRegionHeader(bale_file,spos+offset,epos+offset,buf.toString(),rt);

   region_set.add(br);
}



/********************************************************************************/
/*										*/
/*	Comparator for region sort order					*/
/*										*/
/********************************************************************************/

private static class RegionComparator implements Comparator<BconRegion> {

   @Override public int compare(BconRegion r1,BconRegion r2) {
      int v = r1.getStartOffset() - r2.getStartOffset();
      return v;
    }

}	// end of inner class RegionComparator




/********************************************************************************/
/*										*/
/*	List widget for classes 						*/
/*										*/
/********************************************************************************/

private class ClassList extends JList<BconRegion> {

   private static final long serialVersionUID = 1;

   ClassList() {
      super(list_model);
      setCellRenderer(new RegionRenderer());
      setDragEnabled(true);
      setDropMode(DropMode.INSERT);
      setTransferHandler(new ListMover());
      Font ft = getFont();
      float sz = bcon_props.getFloat(BCON_CLASS_FONT_SIZE,9f);
      ft = ft.deriveFont(sz);
      ft = ft.deriveFont(Font.BOLD);
      setFont(ft);
      setToolTipText("Class Elements");
      addMouseListener(new ClassMouser());
    }

   @Override public boolean getScrollableTracksViewportWidth()		{ return true; }

   @Override public String getToolTipText(MouseEvent evt) {
      int idx = locationToIndex(evt.getPoint());
      BconRegion br = list_model.getElementAt(idx);
      if (br == null) return null;
      String txt = br.getRegionText();
      return "<html><pre>" + txt + "</pre></html>";
    }

}	// end of inner class ClassList




private class ClassMouser extends MouseAdapter {

   @Override public void mouseClicked(MouseEvent e) {
      JList<?> lst = (JList<?>) e.getSource();
      if (e.getClickCount() == 2) {
	 int idx = lst.locationToIndex(e.getPoint());
	 BconRegion br = list_model.getElementAt(idx);
	 if (br != null) br.createBubble(lst);
       }
    }

}	// end of inner class ClassMouser




/********************************************************************************/
/*										*/
/*	Selector of what to show						*/
/*										*/
/********************************************************************************/

private static class Selector {

   private int	show_modifier;

   Selector(int show) {
      show_modifier = show;
    }

   void reset(int off,int on) {
      show_modifier &= ~off;
      show_modifier |= on;
    }

   boolean approve(BconRegion br) {
      switch (br.getRegionType()) {
	 case REGION_UNKNOWN :
	    return false;
	 case REGION_CLASS :
	    if (!check(MOD_CLASS)) return false;
	    break;
	 case REGION_METHOD :
	 case REGION_CONSTRUCTOR :
	    if (!check(MOD_METHOD)) return false;
	    break;
	 case REGION_FIELD :
	    if (!check(MOD_FIELD)) return false;
	    break;
	 case REGION_INITIALIZER :
	    if (!check(MOD_INITIALIZER)) return false;
	    break;
	 case REGION_COMMENT :
	 case REGION_COPYRIGHT :
	    if (!check(MOD_COMMENT)) return false;
	    break;
	 case REGION_PACKAGE :
	    if (!check(MOD_PACKAGEDECL)) return false;
	    break;
	 case REGION_IMPORT :
	    if (!check(MOD_IMPORT)) return false;
	    break;
	 case REGION_CLASS_HEADER :
	    if (!check(MOD_TOPDECL)) return false;
	    break;
       }
      int mods = br.getModifiers();
      if (mods != BCON_MODIFIERS_UNDEFINED) {
	 if (Modifier.isStatic(mods)) {
	    if (!check(MOD_STATIC)) return false;
	  }
	 else if (!check(MOD_NONSTATIC)) return false;
	 if (Modifier.isAbstract(mods)) {
	    if (!check(MOD_ABSTRACT)) return false;
	  }
	 else if (!check(MOD_NONABSTRACT)) return false;
	 if (Modifier.isPublic(mods)) {
	    if (!check(MOD_PUBLIC)) return false;
	  }
	 else if (Modifier.isProtected(mods)) {
	    if (!check(MOD_PROTECTED)) return false;
	  }
	 else if (Modifier.isPrivate(mods)) {
	    if (!check(MOD_PRIVATE)) return false;
	  }
	 else if (!check(MOD_PACKAGE)) return false;
       }
      return true;
    }

   boolean check(int fg) {
      return (show_modifier & fg) != 0;
    }

}	// end of inner class Selector



/********************************************************************************/
/*										*/
/*	Drawing panel								*/
/*										*/
/********************************************************************************/

private class ClassPanel extends SwingGridPanel implements BudaConstants.BudaBubbleOutputer {

   @Override public String getConfigurator()		{ return "BCON"; }
   @Override public void outputXml(BudaXmlWriter xw) {
      xw.field("TYPE","CLASS");
      xw.field("PROJECT",for_project);
      xw.field("CLASS",for_class);
      xw.field("FILE",for_file.getPath());
      xw.field("INNER",inner_class);
    }

}	// end of inner class ClassPanel




/********************************************************************************/
/*										*/
/*	List model for selective display					*/
/*										*/
/********************************************************************************/

private class ClassListModel extends AbstractListModel<BconRegion> {

   private List<BconRegion> use_regions;
   private int cur_size;
   private boolean is_valid;

   private static final long serialVersionUID = 1;

   private ClassListModel() {
      use_regions = new ArrayList<>();
      cur_size = 0;
      is_valid = false;
    }

   @Override public BconRegion getElementAt(int idx) {
      validate();
      if (idx < 0 || idx >= use_regions.size()) return null;
      return use_regions.get(idx);
    }

   @Override public int getSize() {
      validate();
      return use_regions.size();
    }

   void invalidate() {
      is_valid = false;
      if (cur_size >= 0) fireContentsChanged(this,0,cur_size-1);
    }

   private void validate() {
      if (is_valid) return;

      int osz = use_regions.size();
      is_valid = true;
      use_regions.clear();
      for (BconRegion br : region_set) {
	 if (cur_selector.approve(br)) use_regions.add(br);
       }
      cur_size = use_regions.size();

      if (osz > 0) fireIntervalRemoved(this,0,osz-1);
      if (cur_size > 0) fireIntervalAdded(this,0,cur_size-1);
    }

}	// end of inner class ClassListModel



/********************************************************************************/
/*										*/
/*	Cell renderer for BconRegions						*/
/*										*/
/********************************************************************************/

private static class RegionRenderer extends DefaultListCellRenderer {

   private static final long serialVersionUID = 1;

   RegionRenderer() { }

   @Override public Component getListCellRendererComponent(JList<?> l,Object v,int ldx,boolean issel,boolean hasfoc) {
      BconRegion br = (BconRegion) v;
   
      Color classcolor = BoardColors.getColor(BCON_CLASS_CLASS_COLOR_PROP);
      Color methodcolor = BoardColors.getColor(BCON_CLASS_METHOD_COLOR_PROP);
      Color fieldcolor = BoardColors.getColor(BCON_CLASS_FIELD_COLOR_PROP);
      Color initcolor = BoardColors.getColor(BCON_CLASS_INITIALIZER_COLOR_PROP);
      Color cmmtcolor = BoardColors.getColor(BCON_CLASS_COMMENT_COLOR_PROP);
      Color hdrcolor = BoardColors.getColor(BCON_CLASS_HEADER_COLOR_PROP);
      Color importcolor = BoardColors.getColor(BCON_CLASS_IMPORT_COLOR_PROP);
   
      String pfx = "  ";
      String nam = null;
   
      int mods = br.getModifiers();
      if (mods != BCON_MODIFIERS_UNDEFINED) {
         // TODO: should use icons here
         if (Modifier.isPublic(mods)) pfx = "+ ";
         else if (Modifier.isProtected(mods)) pfx = "# ";
         else if (Modifier.isPrivate(mods)) pfx = "- ";
         else pfx = "  ";
   
         if (Modifier.isStatic(mods)) pfx += "static ";
         if (Modifier.isAbstract(mods)) pfx += "abstract ";
       }
   
      int idx;
      Color c = null;
      String rnm = br.getRegionName();
      switch (br.getRegionType()) {
         case REGION_CLASS :
            String typ = (Modifier.isInterface(mods) ? "interface " : "class ");
            idx = rnm.indexOf("$");
            if (idx < 0) idx = rnm.lastIndexOf(".");
            if (idx < 0) nam = typ + rnm;
            else nam = typ + rnm.substring(idx+1);
            c = classcolor;
            break;
         case REGION_METHOD :
         case REGION_CONSTRUCTOR :
            idx = rnm.indexOf("(");
            idx = rnm.lastIndexOf(".",idx);
            if (idx < 0) nam = rnm;
            else nam = rnm.substring(idx+1);
            // TODO: simplify arguments
            c = methodcolor;
            break;
         case REGION_FIELD :
            idx = rnm.lastIndexOf(".");
            if (idx < 0) nam = rnm;
            else nam = rnm.substring(idx+1);
            c = fieldcolor;
            break;
         case REGION_INITIALIZER :
            nam = "< Static Initializer >";
            c = initcolor;
            break;
         case REGION_COMMENT :
         case REGION_COPYRIGHT :
            if (rnm.length() >= MAX_COMMENT_SIZE) nam = rnm.substring(0,MAX_COMMENT_SIZE);
            else nam = rnm;
            c = cmmtcolor;
            break;
         case REGION_IMPORT :
            c = importcolor;
            nam = rnm;
            break;
         case REGION_PACKAGE :
         case REGION_CLASS_HEADER :
            c = hdrcolor;
            nam = rnm;
            break;
         default:
            break;
        }
   
      if (pfx != null) nam = pfx + nam;
   
      Component comp = super.getListCellRendererComponent(l,nam,ldx,issel,hasfoc);
   
      if (c != null && !issel) comp.setForeground(c);
   
      return comp;
    }

}	// end of inner class RegionRenderer



/********************************************************************************/
/*										*/
/*	Transfer handler for drag and drop					*/
/*										*/
/********************************************************************************/

private class ListMover extends TransferHandler {

   private static final long serialVersionUID = 1;


   ListMover()				{ }

   @Override public boolean canImport(TransferHandler.TransferSupport sup) {
      if (sup.isDataFlavorSupported(region_flavor)) return true;
      return super.canImport(sup);
   }

   @Override protected Transferable createTransferable(JComponent c) {
      return new ListTransfer((JList<?>) c);
    }

   @Override protected void exportDone(JComponent src,Transferable d,int act) {
      if (act == MOVE) {
         try {
            ListTransfer lt = (ListTransfer) d.getTransferData(region_flavor);
            lt.removeText();
          }
         catch (IOException e) { }
         catch (UnsupportedFlavorException e) { }
       }
      BudaRoot br = BudaRoot.findBudaRoot(class_panel);
      if (br != null) br.handleSaveAllRequest();
      setupElements();
    }

   @Override public int getSourceActions(JComponent c) {
      return COPY_OR_MOVE;
    }

   @Override public boolean importData(TransferHandler.TransferSupport sup) {
      Transferable trn = sup.getTransferable();
      try {
	 ListTransfer lt = (ListTransfer) trn.getTransferData(region_flavor);
	 JList.DropLocation loc = (JList.DropLocation) sup.getDropLocation();
	 String txt = lt.getText(getComponent());
	 int idx = loc.getIndex();
	 BconRegion br = list_model.getElementAt(idx);
	 if (lt.sameRegion(br)) return false;
	 if (br == null && idx > 0) {
	    br = list_model.getElementAt(idx-1);
	    br.insertAfter(txt);
	 }
	 else if (br != null)
	    br.insertBefore(txt);
       }
      catch (IOException e) {
	 return false;
      }
      catch (UnsupportedFlavorException e) {
	 return false;
      }

      return true;
    }

}	// end of inner class ListMover



private class ListTransfer implements Transferable, BudaConstants.BudaDragBubble {

   private List<BconRegion> list_regions;
   private boolean can_delete;

   ListTransfer(JList<?> lst) {
      can_delete = false;
      list_regions = new ArrayList<BconRegion>();
      for (Object o : lst.getSelectedValuesList()) {
	 list_regions.add((BconRegion) o);
      }
    }

   @Override public Object getTransferData(DataFlavor f) {
      if (f == region_flavor || f == BudaRoot.getBubbleTransferFlavor()) return this;
      return null;
    }

   @Override public DataFlavor [] getTransferDataFlavors() {
      return new DataFlavor [] { region_flavor, BudaRoot.getBubbleTransferFlavor() };
    }

   @Override public boolean isDataFlavorSupported(DataFlavor f) {
      if (f == null) return false;
      return f.equals(region_flavor) || f.equals(BudaRoot.getBubbleTransferFlavor());
    }

   @Override public BudaBubble [] createBubbles() {
      List<BudaBubble> bbls = new ArrayList<>();
      for (BconRegion br : list_regions) {
         BudaBubble bb = br.makeBubble();
         if (bb != null) bbls.add(bb);
       }
      BudaBubble [] rslt = new BudaBubble[bbls.size()];
      rslt = bbls.toArray(rslt);
      return rslt;
    }

   String getText(Component c) {
      StringBuffer buf = new StringBuffer();
      for (BconRegion br : list_regions) {
	 String rtxt = br.getRegionText();
	 if (rtxt != null) buf.append(rtxt);
       }
      if (c == getComponent()) can_delete = true;
      return buf.toString();
    }

   void removeText() {
      if (can_delete) {
	 for (BconRegion br : list_regions) {
	    br.remove();
	  }
       }
    }

   boolean sameRegion(BconRegion br) {
      for (BconRegion xbr : list_regions) {
	 if (xbr == br) return true;
       }
      return false;
    }


}	// end of inner class ListTransfer




/********************************************************************************/
/*										*/
/*	Creation actions							*/
/*										*/
/********************************************************************************/

private String getButtonName(BconRegion br,BuenoType typ,boolean before)
{
   String rslt = "Insert ";
   switch (typ) {
      case NEW_INNER_TYPE :
	 rslt += "Class/Interface/Enum";
	 break;
      case NEW_METHOD :
	 rslt += "Method";
	 break;
      case NEW_FIELD :
	 rslt += "Field";
	 break;
      case NEW_MARQUIS_COMMENT :
	 rslt += "Marquis Comment";
	 break;
      case NEW_BLOCK_COMMENT :
	 rslt += "Block Comment";
	 break;
      case NEW_JAVADOC_COMMENT :
	 rslt += "JavaDoc Comment";
	 break;
      default:
	 break;
    }
   if (before) rslt += " before ";
   else rslt += " after ";
   String rnm = br.getRegionName();
   if (rnm != null) {
      int idx1 = rnm.indexOf("(");
      int idx2 = rnm.indexOf("<");
      if (idx1 > 0 && idx2 > 0 && idx2 < idx1) idx1 = idx2;
      if (idx1 < 0 && idx2 > 0) idx1 = idx2;
      if (idx1 > 0) rnm = rnm.substring(0,idx1);
      idx1 = rnm.lastIndexOf(".");
      if (idx1 > 0) rnm = rnm.substring(idx1+1);
      rslt += rnm;
    }

   return rslt;
}



private abstract class AbstractCreator extends AbstractAction {

   protected BuenoType new_type;
   protected boolean is_before;
   protected BconRegion for_region;
   protected BuenoProperties property_set; 
   
   private static final long serialVersionUID = 1L;


   AbstractCreator(BuenoType typ,BconRegion br,boolean before) {
      super(getButtonName(br,typ,before));
      new_type = typ;
      for_region = br;
      is_before = before;
      property_set = new BuenoProperties();
    }

   BudaBubble getBubble() {
      return BudaRoot.findBudaBubble(class_panel);
    }

   BuenoLocation getLocation() {
      return new BconRegionBueno(for_project,for_class,for_file,for_region,!is_before);
    }

}



private class CommentCreator extends AbstractCreator {

   CommentCreator(BuenoType typ,BconRegion br,boolean before) {
      super(typ,br,before);
   }

   @Override public void actionPerformed(ActionEvent e) {
      BuenoFactory bf = BuenoFactory.getFactory();
      bf.createNew(new_type,getLocation(),property_set);
      setupElements();
    }

}	// end of inner class CommentCreator



private class MethodCreator extends AbstractCreator implements BuenoBubbleCreator {

   MethodCreator(BconRegion br,boolean before) {
      super(BuenoType.NEW_METHOD,br,before);
    }

   @Override public void actionPerformed(ActionEvent e) {
      BuenoFactory.getFactory().createMethodDialog(getBubble(),popup_point,
        					      property_set,getLocation(),null,
        					      this);
    }

   @Override public void createBubble(String proj,String name,BudaBubbleArea bba,Point p) {
      BudaBubble bb = BaleFactory.getFactory().createMethodBubble(proj,name);
      if (bb != null) bba.add(bb,new BudaConstraint(p));
      setupElements();
    }

}	// end of inner class MethodCreator



private class FieldCreator extends AbstractCreator implements BuenoBubbleCreator {

   FieldCreator(BconRegion br,boolean before) {
      super(BuenoType.NEW_FIELD,br,before);
    }

   @Override public void actionPerformed(ActionEvent e) {
      BuenoFieldDialog bfd = new BuenoFieldDialog(getBubble(),popup_point,
						     property_set,getLocation(),this);
      bfd.showDialog();
    }

   @Override public void createBubble(String proj,String name,BudaBubbleArea bba,Point p) {
      setupElements();
    }

}	// end of inner class FieldCreator



private class TypeCreator extends AbstractCreator implements BuenoBubbleCreator {

   TypeCreator(BconRegion br,boolean before) {
      super(BuenoType.NEW_INNER_TYPE,br,before);
    }

   @Override public void actionPerformed(ActionEvent e) {
      BuenoInnerClassDialog bcd = new BuenoInnerClassDialog(getBubble(),popup_point,
							       new_type,
							       property_set,getLocation(),this);
      bcd.showDialog();
    }

   @Override public void createBubble(String proj,String name,BudaBubbleArea bba,Point p) {
      BudaBubble bb = BaleFactory.getFactory().createClassBubble(proj,name);
      if (bb != null) bba.add(bb,new BudaConstraint(p));
      setupElements();
    }

}	// end of inner class TypeCreator



/********************************************************************************/
/*										*/
/*	Methods for handling updates						*/
/*										*/
/********************************************************************************/

@Override public void handleElisionData(File f,int id,Element data)	{ }

@Override public void handleRemoteEdit(File f,int len,int off,String txt)
{
   // if edit is significant, might want to update
}


@Override public void handleFileChanged(String proj,String file)
{
   if (file.equals(for_file.getPath())) {
      setupElements();
    }
}






@Override public void handleFileRemoved(String proj,String file)
{
   if (file.equals(for_file.getPath())) {
      setupElements();
    }
}



/********************************************************************************/
/*										*/
/*	Other Actions								*/
/*										*/
/********************************************************************************/


private class ClassBubbleAction extends AbstractAction {

   ClassBubbleAction() {
      super("Open Class Bubble");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BudaBubble bb = BaleFactory.getFactory().createClassBubble(for_project,for_class);
      if (bb == null) return;
      Rectangle r = BudaRoot.findBudaLocation(class_panel);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(class_panel);
      if (bba != null && r != null) bba.addBubble(bb,r.x + r.width + 40, r.y);
    }

}	// end of inner class ClassBubbleAction




private static class DeleteAction extends AbstractAction {

   private BconRegion delete_region;

   DeleteAction(BconRegion br) {
      super("Delete " + br.getShortRegionName());
      delete_region = br;
    }

   @Override public void actionPerformed(ActionEvent e) {
      delete_region.remove();
    }

}	// end of inner class DeleteAction



private class ResetAction extends AbstractAction {

   ResetAction() {
      super("Update Regions");
    }

   @Override public void actionPerformed(ActionEvent e) {
      setupElements();
    }

}	// end of inner class ResetAction


private static class FilterAction implements ActionListener {

   @Override public void actionPerformed(ActionEvent evt) {
      JTextField tfld = (JTextField) evt.getSource();
      String txt = tfld.getText();
      if (txt == null || txt.length() == 0) return;
      // TODO: handle typein in class panel
    }
}

}	// end of class BconClassPanel




/* end of BconClassPanel.java */
