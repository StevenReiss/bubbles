/********************************************************************************/
/*										*/
/*		BudaRoot.java							*/
/*										*/
/*	BUblles Display Area root window					*/
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

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardConstants.RunMode;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.swing.SwingDebugGraphics;
import edu.brown.cs.ivy.swing.SwingEventListenerList;
import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.swing.SwingText;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkListener;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.WeakHashMap;

// import com.itextpdf.text.pdf.*;
import gnu.jpdf.PDFJob;



/**
 *	This class provides a top level window for doing bubble management.  It
 *	handles setting up the various subwindows and the communications among
 *	those windows.
 **/

public class BudaRoot extends JFrame implements BudaConstants {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BudaBubbleArea		bubble_area;
private BudaViewport		bubble_view;
private BudaOverviewBar 	bubble_overview;
private BudaTopBar		bubble_topbar;
// private double		scale_factor;
// private MouseScaler		mouse_scaler;
private BudaBubble		search_bubble;
private BudaBubble		docsearch_bubble;
private JPanel			button_panel;
private List<Component>         button_panels;
private Collection<BudaTask>	task_shelf;
private BudaRelations		relation_data;
private BudaChannelSet		cur_channels;
private JPanel			channel_area;
private BudaShareManager	share_manager;
private BudaDemonstration	demo_thread;
private String			demo_text;
private Point			demo_point;
private boolean 		view_setup;
private int			shade_delta;
private boolean 		shade_down;
private boolean 		shade_safe;

private static MouseEvent	last_mouse;


private static SwingEventListenerList<BubbleViewCallback> view_callbacks;
private static SwingEventListenerList<BudaFileHandler> file_handlers;

private static Font DEMO_TEXT_FONT = new Font(Font.SANS_SERIF,Font.BOLD,96);

private static SearchBoxCreator search_creator = null;
private static BudaMenu 	bubble_menu = null;

private static Map<String,BubbleConfigurator>	bubble_config;
private static Map<String,PortConfigurator>	port_config;
private static Map<String,HyperlinkListener>	hyperlink_config;

private static DocBoxCreator	doc_creator = null;

private static boolean debug_graphics = false;

private static final long serialVersionUID = 1L;

private static DataFlavor bubble_flavor;
private static DataFlavor bubble_group_flavor;

private static int color_index;
private static int color_step;
private static Color [] 	group_colors;

private static final int SHADE_UPDATE_TIME = 50;
private static final long SHADE_UP_DOWN_TIME = 2000;
private static final int SHADE_PULL_DOWN_START = 5;	// 30, 30 + 104
private static final int SHADE_PULL_DOWN_END = 30;


static {
   view_callbacks = new SwingEventListenerList<>(BubbleViewCallback.class);
   file_handlers = new SwingEventListenerList<>(BudaFileHandler.class);
   bubble_config = new LinkedHashMap<>();
   port_config = new HashMap<>();
   bubble_flavor = new DataFlavor(BudaDragBubble.class,"Bubble");
   bubble_group_flavor = new DataFlavor(BudaDragBubbleGroup.class,"BubbleGroup");
   hyperlink_config = new HashMap<>();

   port_config.put("BUDA",new DefaultPortConfigurator());
   List<Color> cols = new ArrayList<>();
   for (int i = 1; i < 64; ++i) {
      Color c1 = BoardColors.getColor("Buda.RootGroupColor." + i,null);
      if (c1 == null) continue;
      cols.add(c1);
    }
   group_colors = new Color[cols.size()];
   group_colors = cols.toArray(group_colors);

   int ln = group_colors.length;
   color_index = (int)(Math.random() * ln);
   int st = ln / 3;
   for (int i = 0; i < ln; ++i) {
      for (int j = 0; j < 2; ++j) {
	 int x = st + (j == 0 ? i : ln-i);
	 x %= ln;
	 if (x == 0) continue;
	 if (gcd(x,ln) == 1) {
	    color_step = x;
	    break;
	  }
       }
    }
}


private static int gcd(int x,int y)
{
   if (y == 0) return x;
   return gcd(y,x % y);
}



/********************************************************************************/
/*										*/
/*	Static initialization and setup 					*/
/*										*/
/********************************************************************************/

static {
   Toolkit.getDefaultToolkit().getSystemEventQueue().push(new MouseEventQueue());
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

/**
 *	Create a new root window with the given title.
 **/

public BudaRoot(String ttl)
{
   initialize(null);
   setTitle(ttl);
}



/**
 *	Create a new root window from a saved configuration
 **/

public BudaRoot(Element xml)
{
   initialize(xml);

   setBudaTitle();
}



void setBudaTitle()
{
   String version = BoardSetup.getVersionData();
   String title = "Code Bubbles";
   if (!version.startsWith("Build")) {
      title += " - " + version;
    }
   String ws = BoardSetup.getSetup().getDefaultWorkspace();
   if (ws != null) {
      int idx = ws.lastIndexOf(File.separator);
      if (idx > 0) ws = ws.substring(idx+1);
      title += " for " + ws;
    }
   setTitle(title);
}





private void initialize(Element e)
{
   view_setup = false;

   setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
   addWindowListener(new WindowCloser());

   // scale_factor = 1.0;
   // mouse_scaler = new MouseScaler();
   search_bubble = null;
   docsearch_bubble = null;
   button_panel = null;
   button_panels = new ArrayList<Component>();
   cur_channels = null;
   last_mouse = null;
   demo_thread = null;
   demo_text = null;
   demo_point = null;
   shade_delta = 0;
   shade_down = true;
   shade_safe = true;

   int inputdelta = IvyXml.getAttrInt(e,"DELTA",0);

   setupSwing();

   SwingGridPanel root = new SwingGridPanel();
   root.setInsets(PANEL_BORDER_INSET);

   Element areacfg = IvyXml.getChild(e,"BUBBLEAREA");
   bubble_area = new BudaBubbleArea(this,areacfg,null);

   bubble_area.addBubbleAreaCallback(new ViewportCallback());

   Element viewcfg = IvyXml.getChild(e,"VIEWAREA");
   bubble_view = new BudaViewport(bubble_area,viewcfg,inputdelta);
   bubble_view.addChangeListener(new ViewportHandler());
   root.addGBComponent(bubble_view,0,2,0,1,10,10);

   bubble_overview = new BudaOverviewBar(bubble_area,bubble_view,this);
   root.addGBComponent(bubble_overview,0,1,1,1,10,0);

   Element topelt = IvyXml.getChild(e,"TOPBAR");
   bubble_topbar = new BudaTopBar(this,topelt,bubble_area,bubble_overview);
   root.addGBComponent(bubble_topbar,0,0,1,1,10,0);

   task_shelf = new TreeSet<BudaTask>(new TaskComparator());
   relation_data = new BudaRelations();
   loadHistory(e);
   File hf = BoardSetup.getHistoryFile();
   if (hf != null) {
      Element helt = IvyXml.loadXmlFromFile(hf);
      loadHistory(helt);
    }

   channel_area = new JPanel(new GridLayout(1,1));
   channel_area.setVisible(false);
   root.addGBComponent(channel_area,0,2,0,1,10,10);

   setContentPane(root);

   pack();

   Element shape = IvyXml.getChild(e,"SHAPE");
   if (shape != null) {
      Rectangle r1 = getBounds();
      int x = (int) IvyXml.getAttrDouble(shape,"X",0);
      int y = (int) IvyXml.getAttrDouble(shape,"Y",0);
      int w = (int) IvyXml.getAttrDouble(shape,"WIDTH",0);
      int h = (int) IvyXml.getAttrDouble(shape,"HEIGHT",0);
      w = Math.max(w,r1.width);
      h = Math.max(h,r1.height);
      setLocation(x,y);
      setSize(w,h);
    }

   for (BubbleConfigurator bc : bubble_config.values()) bc.loadXml(bubble_area,e);

   BudaCursorManager.setupDefaults(this);

   setupGlobalActions();
   new CheckpointTimer();		// start checkpointing

   BoardMetrics.setRootWindow(this);
   share_manager = new BudaShareManager();

   if (BoardSetup.getSetup().getRunMode() == RunMode.CLIENT) {
      BoardSetup.getSetup().getMintControl().register("<BUDA SOURCE='SERVER' CMD='_VAR_0' FILE='_VAR_1' />",
	    new ClientHandler());
    }
}



private void setupSwing()
{
   UIDefaults dflts = UIManager.getDefaults();

   Color ttbg = BoardColors.getColor(BUBBLE_TOOLTIP_COLOR_PROP);
   if (ttbg != null) dflts.put("ToolTip.background",ttbg);
   Color ttfg = BoardColors.getColor(BUBBLE_TOOLTIP_TEXT_PROP);
   if (ttfg != null) dflts.put("ToolTip.foreground",ttfg);
   Font ttft = BUDA_PROPERTIES.getFontOption("Buda.tooltip.font",BUBBLE_TOOLTIP_FONT);
   if (ttft != null) dflts.put("ToolTip.font",ttft);
}




/********************************************************************************/
/*										*/
/*	Setup methods -- called after all packages are initialized		*/
/*										*/
/********************************************************************************/

public void restoreConfiguration(Element xml)
{
   SwingUtilities.invokeLater(new RestoreSession(xml));
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

/**
 *	Return the BudaBubbleArea associated with this root window.
 **/

public BudaBubbleArea getCurrentBubbleArea()
{
   // if we have channels, need to get the bubble area from the channel

   if (cur_channels != null) {
      return cur_channels.getBubbleArea();
    }

   return bubble_area;
}


BudaBubbleArea getDefaultBubbleArea()
{
   return bubble_area;
}


BudaBubbleArea getBubbleArea()			{ return bubble_area; }

BudaShareManager getShareManager()		{ return share_manager; }

public BudaTopBar getTopBar()			{ return bubble_topbar; }


/**
 *	These methods are used to add and remove links between bubbles in the
 *	display.  They are equivalent to calling the same methods on the bubble
 *	area and are here as a convenience.
 **/

public void addLink(BudaBubbleLink lnk)
{
   if (lnk == null) return;

   BudaBubble src = lnk.getSource();
   BudaBubbleArea ba = findBudaBubbleArea(src);

   ba.addLink(lnk);
}


/**
 *	Remove the given link.
 **/

public void removeLink(BudaBubbleLink lnk)
{
   BudaBubble src = lnk.getSource();
   BudaBubbleArea ba = findBudaBubbleArea(src);
   ba.removeLink(lnk);
}


public void removeCurrentBubble() 
{
   BudaBubbleArea bba = getCurrentBubbleArea();
   MouseEvent me = last_mouse;
   if (me == null) return;
   
   Component c = (Component) me.getSource();
   if (c != null && c instanceof JDialog) {
      JDialog jd = (JDialog) c;
      jd.setVisible(false);
      return;
    }
   
   Point pt = SwingUtilities.convertPoint(c,me.getPoint(),bba);
   me = new MouseEvent(bba,me.getID(),me.getWhen(),me.getModifiersEx(),
         pt.x,pt.y,me.getClickCount(),me.isPopupTrigger(),
         me.getButton());
   bba.removeCurrentBubble(me);
}



/********************************************************************************/
/*										*/
/*	Help methods								*/
/*										*/
/********************************************************************************/

private static Map<Component,BudaHelp> help_map;

static {
   help_map = new WeakHashMap<>();
}


public static boolean showHelpTips()
{
   return BUDA_PROPERTIES.getBoolean(USE_HELP_TOOLTIPS);
}


public static void registerHelp(Component c,BudaHelpClient h)
{
   help_map.put(c,new BudaHelp(c,h));
}



public static void showHelp(MouseEvent e)
{
   if (e == null) e = last_mouse;

   Component c0 = SwingUtilities.getDeepestComponentAt((Component) e.getSource(),e.getX(),e.getY());

   for (Component c = c0; c != null; c = c.getParent()) {
      BudaHelp bh = help_map.get(c);
      if (bh != null) {
	 MouseEvent nme = SwingUtilities.convertMouseEvent((Component) e.getSource(),e,c);
	 bh.simulateHover(nme);
	 return;
       }
    }
}



public void setDemonstration(BudaDemonstration demo,String text)
{
   BudaHover.removeHovers();

   demo_thread = demo;
   demo_text = text;
   demo_point = null;
   repaint();
}


public void setDemonstrationPoint(Point pt)
{
   if (pt == null) demo_point = null;
   else {
      if (demo_point == null) demo_point = new Point(pt);
      else demo_point.setLocation(pt);
      repaint();
   }
}


/********************************************************************************/
/*										*/
/*	Working set methods							*/
/*										*/
/********************************************************************************/

BudaWorkingSetImpl defineWorkingSet(BudaBubbleArea bba,String lbl,Rectangle rgn,
      WorkingSetGrowth g)
{
   BudaWorkingSetImpl bs = bba.defineWorkingSet(lbl,rgn,g);
   if (bs == null) return null;

   bubble_overview.repaint();
   bubble_topbar.repaint();
   return bs;
}


void removeWorkingSet(BudaWorkingSetImpl ws)
{
   bubble_area.removeWorkingSet(ws);
   bubble_overview.repaint();
   bubble_topbar.repaint();
}


/**
 *	Get the collection of current active working sets.
 **/

public Collection<BudaWorkingSet> getWorkingSets()
{
   return new ArrayList<BudaWorkingSet>(bubble_area.getWorkingSets());
}


Collection<BudaWorkingSetImpl> getWorkingSetImpls()
{
   return bubble_area.getWorkingSets();
}



void handleNewWorkingSet(Component c,String lbl)
{
   BudaBubbleArea bba = findBudaBubbleArea(c);
   Rectangle r = bubble_view.getViewRect();
   defineWorkingSet(bba,lbl,r,null);
}



/**
 *	Return the data transfer flavor for drag and drop bubbles.
 **/

public static DataFlavor getBubbleTransferFlavor()	{ return bubble_flavor; }


/**
 *      Return the data transfer flavor for drag and drop bubble groups
 **/

public static DataFlavor getBubbleGroupTransferFlavor() { return bubble_group_flavor; }




/**
 *	Return the working set for a given point from the top bar
 **/

public BudaWorkingSet findCurrentWorkingSet()
{
   return bubble_topbar.findCurrentWorkingSet();
}



/**
 *	Add a top bar menu item
 *	@param c Component to add to menu (generally a JButton)
 *	@param ws Add to working set menu if true, non-workingset menu if not
 **/

public void addTopBarMenuItem(Component c,boolean ws)
{
   bubble_topbar.addMenuItem(c,ws);
}



/********************************************************************************/
/*										*/
/*	Static methods for getting bubble information				*/
/*										*/
/********************************************************************************/

/**
 *	Return the BudaRoot associated with the given component.
 **/

public static BudaRoot findBudaRoot(Component c)
{
   for (Component p = c; p != null; p = getParentComponent(p)) {
      if (p instanceof BudaRoot) return (BudaRoot) p;
    }

   return null;
}



/**
 *	Return the BudaBubbleArea associated with the given component.
 **/

public static BudaBubbleArea findBudaBubbleArea(Component c)
{
   for (Component p = c; p != null; p = getParentComponent(p)) {
      if (p instanceof BudaBubbleArea) return (BudaBubbleArea) p;
      else if (p instanceof BudaTopBar) return ((BudaTopBar) p).getBubbleArea();
      else if (p instanceof BudaOverviewBar) return ((BudaOverviewBar) p).getBubbleArea();
    }

   return null;
}



/**
 *	Return the BudaBubble associated with a the givne component.
 **/

public static BudaBubble findBudaBubble(Component c)
{
   for (Component p = c; p != null; p = getParentComponent(p)) {
      if (p instanceof BudaBubble && p.getParent() instanceof BudaBubbleArea) return (BudaBubble) p;
    }

   return null;
}



/**
 *	Return the rectangle defining the bubble associated with the given
 *	component.
 **/

public static Rectangle findBudaLocation(Component c)
{
   for (Component p = c; p != null; p = getParentComponent(p)) {
      if (p instanceof BudaBubble && p.isVisible() &&
	     p.getParent() instanceof BudaBubbleArea)
	 return p.getBounds();
    }

   return null;
}



private static Component getParentComponent(Component c)
{
   if (c instanceof JPopupMenu) {
      JPopupMenu pm = (JPopupMenu) c;
      return pm.getInvoker();
    }

   return c.getParent();
}



/********************************************************************************/
/*										*/
/*	Panel methods								*/
/*										*/
/********************************************************************************/

private void addButtonPanel()
{
   button_panel = new ButtonPanel();
   addPanel(button_panel,false);
}




/**
 *	Return the panel for adding buttons
 **/

public synchronized JPanel getButtonPanel()
{
   if (button_panel == null) addButtonPanel();

   return button_panel;
}



/**
 *	Add a button panel button
 **/

public void addButtonPanelButton(JComponent c)
{
   JPanel pnl = getButtonPanel();
   Box bx = Box.createHorizontalBox();
   bx.add(Box.createHorizontalGlue());
   bx.add(c);
   bx.add(Box.createHorizontalGlue());
   pnl.add(bx);
   pnl.add(Box.createVerticalStrut(BUDA_BUTTON_SEPARATION));
   if (c.getBackground() != null && c.getBackground().getAlpha() == 0) {
      c.setForeground(pnl.getForeground());
    }
}



public void addPanel(Component pnl,boolean right)
{
   if (button_panels.isEmpty() && pnl != button_panel) {
      addButtonPanel();
    }
   if (right) button_panels.add(pnl);
   else button_panels.add(0,pnl);

   SwingGridPanel root = (SwingGridPanel) getContentPane();
   int ct = 1;
   for (Component c : button_panels) {
      root.addGBComponent(c,ct,0,1,2,0,0);
      ++ct;
    }

   root.invalidate();
}




/********************************************************************************/
/*										*/
/*	Search methods								*/
/*										*/
/********************************************************************************/

/**
 *	Register a search engine with the root for handling search requests
 **/

public static void registerSearcher(SearchBoxCreator sb)
{
   search_creator = sb;
}


/**
 *	Create a search bubble at the given point with the given initial
 *	search string.
 **/

public void createSearchBubble(Point pt,String proj,String pfx)
{
   createSearchBubble(pt, proj, pfx, true);
}


/**
 *	Create a search bubble at the given point with the given initial
 *	search string with or without accompanying menu
 */

public void createSearchBubble(Point pt,String proj,String pfx,boolean showmenu)
{
   if (search_creator == null) return;

   hideSearchBubble();

   BudaBubble bb = search_creator.createSearch(SearchType.SEARCH_CODE,proj,pfx);
   bb.addComponentListener(new SearchSingleton());

   if (!BUDA_PROPERTIES.getBoolean(SEARCH_ALLOW_MULTIPLE)) search_bubble = bb;

   if (showmenu) getBubbleMenu().createMenuAndSearch(this,pt,bb);
   else {
      BudaConstraint scnst = new BudaConstraint(BudaBubblePosition.STATIC,pt);
      this.add(bb,scnst);
      bb.grabFocus();
    }
}

/**
 *	Hide the search bubble if it is active.
 **/

public void hideSearchBubble()
{
   if (search_bubble != null) search_bubble.setVisible(false);
}


public static void hideSearchBubble(ActionEvent e)
{
   hideSearchBubble(e.getSource());
}

public static void hideSearchBubble(Object src)
{
   if (src == null) return;
   if (src instanceof Component) {
      Component c = (Component) src;
      BudaRoot br = findBudaRoot(c);
      if (br == null || br.search_bubble == null) return;
      for (Component c1 = c; c1 != null; c1 = c1.getParent()) {
	 if (c1 instanceof JPopupMenu && c1 != br.search_bubble) {
	    c1 = ((JPopupMenu) c1).getInvoker();
	    if (c1 == null) break;
	 }
	 if (c1 == br.search_bubble) {
	    br.hideSearchBubble();
	    break;
	  }
       }
    }
}

BudaBubble getPackageExplorer(BudaBubbleArea bba) { return search_creator.getPackageExplorer(bba); }

/**
 *	Hide the package explorer
 *

public void togglePackageExplorer()
{
   search_creator.togglePackageExplorer();
}*/



/**
 *	Create a search bubble for the user project and javadoc together.
 **/

public void createMergedSearchBubble(Point pt,String proj,String pfx)
{
   if (search_creator == null) return;

   if (search_bubble != null) search_bubble.setVisible(false);
   if (docsearch_bubble != null) docsearch_bubble.setVisible(false);

   BudaBubble bb = search_creator.createSearch(SearchType.SEARCH_ALL,proj,pfx);
   bb.addComponentListener(new SearchSingleton());

   if (!BUDA_PROPERTIES.getBoolean(SEARCH_ALLOW_MULTIPLE)) {
      search_bubble = bb;
      docsearch_bubble = bb;
    }

   getBubbleMenu().createMenuAndSearch(this,pt,bb);
}



/**
 *	Create a search bubble for java doc
 **/

public void createDocSearchBubble(Point pt,String proj,String pfx)
{
   if (search_creator == null) return;

   if (docsearch_bubble != null) docsearch_bubble.setVisible(false);

   BudaBubble bb = search_creator.createSearch(SearchType.SEARCH_DOC,proj,pfx);
   bb.addComponentListener(new SearchSingleton());
   if (!BUDA_PROPERTIES.getBoolean(SEARCH_ALLOW_MULTIPLE)) docsearch_bubble = bb;

   BudaConstraint cnst = new BudaConstraint(BudaBubblePosition.STATIC,pt);
   add(bb,cnst);
}



/**
 *	Note that the search bubble has been used.  This can be used to
 *	delete the menu.
 **/

public void noteSearchUsed(JComponent window)
{
   BudaBubble bb = findBudaBubble(window);
   if (bb != null) getBubbleMenu().noteSearchUsed(bb);
}




private class SearchSingleton extends ComponentAdapter {

   @Override public void componentHidden(ComponentEvent e) {
      if (e.getSource() == search_bubble) {
	 search_bubble = null;
       }
      if (e.getSource() == docsearch_bubble) {
	 docsearch_bubble = null;
       }
    }

}	// end of inner class SearchSingleton




/********************************************************************************/
/*										*/
/*	Documentation methods							*/
/*										*/
/********************************************************************************/

/**
 *	Register a routine that can create documentation boxes
 */

public static void registerDocumentationCreator(DocBoxCreator dbc)
{
   doc_creator = dbc;
}



/**
 *	Use the registered documentation creator to build a documentation bubble.
 **/

public static BudaBubble createDocumentationBubble(String name)
{
   if (doc_creator == null) return null;

   return doc_creator.createDocBubble(name);
}




/********************************************************************************/
/*										*/
/*	Menu methods								*/
/*										*/
/********************************************************************************/

/**
 *	Register a menu button and its callback.  These buttons are associated
 *	with the search menu.
 **/

public static void registerMenuButton(String name,ButtonListener action)
{
   registerMenuButton(name, action, null,null);
}

public static void registerMenuButton(String name,ButtonListener action, Icon icon)
{
   registerMenuButton(name,action,icon,null);
}


public static void registerMenuButton(String name,ButtonListener action,String tooltip)
{
   registerMenuButton(name,action,null,tooltip);
}



public static void registerMenuButton(String name,ButtonListener action, Icon icon,String tooltip)
{
   if (name == null || name.length() == 0) return;

   getBubbleMenu().addMenuItem(name,action,icon,tooltip);
}



private static synchronized BudaMenu getBubbleMenu()
{
   if (bubble_menu == null) {
      bubble_menu = new BudaMenu();
    }
   return bubble_menu;
}



/********************************************************************************/
/*										*/
/*	Toolbar methods 							*/
/*										*/
/********************************************************************************/

/**
 * Adds the given button to the menu called name and action l and icon
 */

public static void addToolbarButton(String name, ActionListener l, Image i)
{
   addToolbarButton(name, l, null, i);
}



/**
 * Adds the given button to the menu called name and action l, tooltip, and icon
 */

public static void addToolbarButton(String name, ActionListener l, String tooltip, Image i)
{
   BudaToolbar.addToolbarButton(name,l,tooltip,i);
}



/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

@Override public void paint(Graphics g)
{
   super.paint(g);

   if (demo_thread != null) {
      if (demo_point != null) {
	 Color dpc = BoardColors.getColor("Buda.RootDemoPointColor");
	 g.setColor(dpc);
	 g.fillOval(demo_point.x - 10, demo_point.y - 10, 20, 20);
       }
      if (demo_text != null) {
	 Rectangle r = getBounds();
	 int h = r.height;
	 r.x = 0;
	 r.y = h - 100;
	 r.height = 100;
	 Graphics2D g2 = (Graphics2D) g;
	 Color dtc = BoardColors.getColor("Buda.RootDemoTextColor");
	 g2.setColor(dtc);
	 g2.setFont(DEMO_TEXT_FONT);
	 SwingText.drawText(demo_text,g2,r);
       }
    }
}





/********************************************************************************/
/*										*/
/*	Printing methods							*/
/*										*/
/********************************************************************************/

void printViewport()
{
   PrinterJob pjob = PrinterJob.getPrinterJob();
   PageFormat fmt = pjob.defaultPage();
   fmt.setOrientation(PageFormat.LANDSCAPE);
   fmt = pjob.pageDialog(fmt);
   pjob.setPrintable(bubble_view,fmt);
   if (pjob.printDialog()) {
      try {
	 pjob.print();
       }
      catch (PrinterException ex) {
	 BoardLog.logE("BUDA","Printing problem",ex);
       }
    }
}



public void exportViewportAsPdf(File file) throws Exception
{
   exportAsPdf(file,bubble_view.getBounds());
}



void exportAsPdf2(File file,Rectangle bnds) throws Exception
{
   FileOutputStream fos = new FileOutputStream(file);

   Paper pp = new Paper();
   pp.setSize(bnds.width,bnds.height);
   PageFormat pf = new PageFormat();
   pf.setPaper(pp);

   PDFJob job = new PDFJob(fos);
   Graphics g2 = job.getGraphics(pf);
   print(g2);
   g2.dispose();
   job.end();

   fos.close();
}


void exportAsPdf(File file,Rectangle bnds) throws Exception
{
   FileOutputStream fos = new FileOutputStream(file);

   BufferedImage bi;
   Dimension sz = getSize();
   bi = new BufferedImage(sz.width,sz.height,BufferedImage.TYPE_INT_RGB);
   Graphics2D g2 = bi.createGraphics();
   paint(g2);


   Paper pp = new Paper();
   pp.setSize(sz.width,sz.height);
   PageFormat pf = new PageFormat();
   pf.setPaper(pp);

   PDFJob job = new PDFJob(fos);
   Graphics p2 = job.getGraphics(pf);
   p2.drawImage(bi,0,0,sz.width,sz.height,BoardColors.getColor("Buda.PdfBackground"),this);
   p2.dispose();
   job.end();

   fos.close();
}



/********************************************************************************/
/*										*/
/*	Window manipulation methods						*/
/*										*/
/********************************************************************************/

@Override protected void addImpl(Component c,Object cnst,int idx)
{
   if (c instanceof BudaBubble) {
      // bubble_area.add(c,cnst,idx);
      if (idx > 0) idx = -1;
      getCurrentBubbleArea().add(c,cnst,idx);
    }
   else {
      super.addImpl(c,cnst,idx);
    }
   BudaCursorManager.setupDefaults(c);
}




/********************************************************************************/
/*										*/
/*	Viewport methods							*/
/*										*/
/********************************************************************************/

void setCurrentViewport(int x,int y)
{
   if (cur_channels != null) cur_channels.setViewport(x,y);
   else setViewport(x,y);
}


public void setViewport(int x,int y)
{
   Rectangle v = bubble_view.getBounds();
   Rectangle a = bubble_area.getBounds();

   if (x < 0) x = 0;
   if (x + v.width >= a.width) x = a.width - v.width;
   if (y < 0) y = 0;
   if (y + v.height >= a.height) y = a.height - v.height;

   bubble_view.setViewPosition(new Point(x,y));
   bubble_area.viewportMoved();
}



void moveCurrentViewport(int dx,int dy)
{
   if (cur_channels != null) cur_channels.moveViewport(dx,dy);
   else moveViewport(dx,dy);
}


void moveViewport(int dx,int dy)
{
   Point p = bubble_view.getViewPosition();
   setViewport(p.x + dx, p.y + dy);
}


/**
 *	Return the current viewport rectangle.
 **/

public Rectangle getCurrentViewport()
{
   if (cur_channels != null) return cur_channels.getViewport();
   return getViewport();
}



/**
 *	Return the viewport rectangle of the main display.
 **/

public Rectangle getViewport()
{
   return bubble_view.getViewRect();
}


synchronized Rectangle getShadedViewport()
{
   if (shade_delta != 0) {
      if (!SwingUtilities.isEventDispatchThread()) return null;
      if (!shade_safe) return null;
      Rectangle r1 = new Rectangle(bubble_view.getViewRect());
      int curht = bubble_overview.getHeight() + bubble_topbar.getHeight();
      int d = BUBBLE_OVERVIEW_HEIGHT + BUBBLE_TOP_BAR_HEIGHT;
      int delta = d-curht;
      r1.y += delta;
      r1.height -= delta;
      return r1;
    }
   if (isShadeDown()) return getViewport();

   Rectangle r = new Rectangle(bubble_view.getViewRect());
   int d = BUBBLE_OVERVIEW_HEIGHT + BUBBLE_TOP_BAR_HEIGHT;
   r.height -= d;
   r.y += d;

   return r;
}


double getScaleFactor() 	{ return getCurrentBubbleArea().getScaleFactor(); }



private class ViewportHandler implements ChangeListener {

   @Override public void stateChanged(ChangeEvent e) {
      Rectangle vr = bubble_view.getViewRect();

      // if (scale_factor != 1) {
	 // vr.x /= scale_factor;
	 // vr.y /= scale_factor;
	 // vr.width /= scale_factor;
	 // vr.height /= scale_factor;
       // }
      bubble_area.setViewPosition(vr);
      bubble_overview.setViewPosition(vr);
    }

}	// end of inner class ViewportHandler



private class ViewportCallback implements BubbleAreaCallback {

   @Override public void updateOverview()		{ }

   @Override public void moveDelta(int dx,int dy) {
      Point p = bubble_view.getViewPosition();
      setViewport(p.x + dx,p.y + dy);
    }

}	// end of inner class ViewportCallback



public Point convertPoint(Component src,Point pt,Component dst)
{
   if (src == dst) return pt;

   BudaBubbleArea srcbba = null;
   BudaBubbleArea dstbba = null;

   for (Component p = src; p != null; p = getParentComponent(p)) {
      if (p instanceof BudaBubbleArea) {
	 srcbba = (BudaBubbleArea) p;
	 break;
       }
    }
   for (Component p = dst; p != null; p = getParentComponent(p)) {
      if (p instanceof BudaBubbleArea) {
	 dstbba = (BudaBubbleArea) p;
	 break;
       }
    }

   if ((srcbba == null || srcbba.getScaleFactor() == 1) &&
	  (dstbba == null || dstbba.getScaleFactor() == 1)) {
      return SwingUtilities.convertPoint(src,pt,dst);
    }

   Point p1 = pt;
   if (srcbba != null) {
      p1 = SwingUtilities.convertPoint(src,pt,srcbba);
      double sf = srcbba.getScaleFactor();
      p1.x *= sf;
      p1.y *= sf;
      p1 = SwingUtilities.convertPoint(srcbba,p1,this);
      src = this;
    }
   Point p2 = p1;
   if (dstbba != null) {
      double sf = dstbba.getScaleFactor();
      p2 = SwingUtilities.convertPoint(src,p1,dstbba);
      p2.x /= sf;
      p1.y /= sf;
      src = dstbba;
    }
   if (src != dst) {
      p2 = SwingUtilities.convertPoint(src,p2,dst);
    }

   return p2;
}



/********************************************************************************/
/*										*/
/*	Keyboard methods							*/
/*										*/
/********************************************************************************/

private void setupGlobalActions()
{
   int menumask = SwingText.getMenuShortcutKeyMaskEx();
   int menudown = 0;
   int altdown = 0;
   if (menumask == InputEvent.CTRL_DOWN_MASK) {
      menudown = InputEvent.CTRL_DOWN_MASK;
      altdown = InputEvent.ALT_DOWN_MASK;
    }
   else if (menumask == InputEvent.META_DOWN_MASK) {
      menudown = InputEvent.META_DOWN_MASK;
      altdown = InputEvent.CTRL_DOWN_MASK;
    }
   else {
      menudown = InputEvent.CTRL_DOWN_MASK;
      altdown = InputEvent.ALT_DOWN_MASK;
    }

   registerKeyAction(BudaToolbar.getMenuBarAction(this),"Show menu bar",
			KeyStroke.getKeyStroke(KeyEvent.VK_F1,0));
   // registerKeyAction(new BudaExpose(this,bubble_area),"EXPOSE",
			// KeyStroke.getKeyStroke(KeyEvent.VK_F9,0));
   registerKeyAction(new EscapeHandler(),"REMOVE_BUBBLE_ESCAPE",
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0));
   registerKeyAction(new RemoveHandler(),"REMOVE_BUBBLE",
			KeyStroke.getKeyStroke(KeyEvent.VK_W,menudown));
   registerKeyAction(new SearchKeyHandler(true, true, true), "Group Adjacent Search",
			KeyStroke.getKeyStroke(KeyEvent.VK_F10, menudown));
   registerKeyAction(new SearchKeyHandler(true, true, true, true), "Nongroup Adjacent Search",
			KeyStroke.getKeyStroke(KeyEvent.VK_F10, menudown|InputEvent.SHIFT_DOWN_MASK));
   registerKeyAction(new SearchKeyHandler(true,false,false),"Search in Project",
			KeyStroke.getKeyStroke(KeyEvent.VK_F10,0));
   registerKeyAction(new SearchKeyHandler(true,false,false),"Search in Project",
			KeyStroke.getKeyStroke(KeyEvent.VK_O,menudown));
   registerKeyAction(new SearchKeyHandler(true,true,false),"Search",
			KeyStroke.getKeyStroke(KeyEvent.VK_F11,0));
   registerKeyAction(new SearchKeyHandler(false,true,false),"Search for Documentation",
			KeyStroke.getKeyStroke(KeyEvent.VK_F12,0));
// registerKeyAction(new ZoomHandler(1),"Zoom in",
//	        KeyStroke.getKeyStroke(KeyEvent.VK_PLUS,menudown));
// registerKeyAction(new ZoomHandler(1),"Zoom in",
//              KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS,menudown|InputEvent.SHIFT_DOWN_MASK));
// registerKeyAction(new ZoomHandler(-1),"Zoom out",
//	        KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,menudown));
   registerKeyAction(new ZoomHandler(0),"Reset zoom",
			KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS,menudown));
   registerKeyAction(new FloaterHandler(),"Toggle Bubble Floating",
			KeyStroke.getKeyStroke(KeyEvent.VK_F12, menudown));
   registerKeyAction(new FocusDirectionHandler(FocusDirectionHandler.LEFT, 0.5f),"Move focus to closest bubble on left",
			KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, menudown|altdown));
   registerKeyAction(new FocusDirectionHandler(FocusDirectionHandler.RIGHT, 0.5f),"Move focus to closest bubble on right",
			KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, menudown|altdown));
   registerKeyAction(new FocusDirectionHandler(FocusDirectionHandler.UP, 0.5f),"Move focus to closest bubble above",
			KeyStroke.getKeyStroke(KeyEvent.VK_UP, menudown|altdown));
   registerKeyAction(new FocusDirectionHandler(FocusDirectionHandler.DOWN, 0.5f),"Move focus to closest bubble below",
			KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, menudown|altdown));
   registerKeyAction(new PanHandler(-1,0),"pan left",
			KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, menudown));
   registerKeyAction(new PanHandler(1,0),"pan right",
			KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, menudown));
   registerKeyAction(new PanHandler(0,-1),"pan up",
			KeyStroke.getKeyStroke(KeyEvent.VK_UP, menudown));
   registerKeyAction(new PanHandler(0,1),"pan down",
			KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, menudown));
   registerKeyAction(new SaveHandler(),"save all",
			KeyStroke.getKeyStroke(KeyEvent.VK_S,menudown));
   registerKeyAction(new CommitHandler(),"commit all",
			KeyStroke.getKeyStroke(KeyEvent.VK_F5,0));
   registerKeyAction(new MetricsHandler(),"force metrics dump",
			KeyStroke.getKeyStroke(KeyEvent.VK_PRINTSCREEN, InputEvent.CTRL_DOWN_MASK|InputEvent.SHIFT_DOWN_MASK));
   registerKeyAction(new HelpHandler(),"show help information",
			KeyStroke.getKeyStroke(KeyEvent.VK_HELP,0));
   registerKeyAction(new HelpHandler(),"show help information",
			KeyStroke.getKeyStroke(KeyEvent.VK_SLASH,menudown|InputEvent.SHIFT_DOWN_MASK));
   registerKeyAction(new HelpHandler(),"show help information",
		KeyStroke.getKeyStroke(KeyEvent.VK_F1, menudown|InputEvent.SHIFT_DOWN_MASK));
   registerKeyAction(new PrintHandler(),"print bubble",
	 KeyStroke.getKeyStroke(KeyEvent.VK_P,menudown));
}



public void registerKeyAction(Action act,String cmd,KeyStroke k)
{
   JPanel cnt = (JPanel) getContentPane();
   cnt.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(k,cmd);
   cnt.getActionMap().put(cmd,act);
}



/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

public boolean handleQuitRequest()
{
   if (!bubble_area.handleQuitRequest()) return false;

   for (BudaFileHandler bfh : file_handlers) {
      if (!bfh.handleQuitRequest()) return false;
    }

   return true;
}



public void handlePropertyChange()
{
   for (BudaFileHandler bfh : file_handlers) {
      bfh.handlePropertyChange();
    }
}



public void handleSaveAllRequest()
{
   // handle all bubbles
   bubble_area.handleSaveAllRequest();

   for (BudaFileHandler bfh : file_handlers) {
      bfh.handleSaveRequest();
    }

   for (BudaFileHandler bfh : file_handlers) {
      bfh.handleSaveDone();
    }
}



public void handleCommitAllRequest()
{
   // handle all bubbles
   bubble_area.handleCommitAllRequest();

   for (BudaFileHandler bfh : file_handlers) {
      bfh.handleCommitRequest();
    }
}



public void handleCheckpointAllRequest()
{
   // handle all bubbles
   bubble_area.handleCheckpointRequest();

   for (BudaFileHandler bfh : file_handlers) {
      bfh.handleCheckpointRequest();
    }

   try {
      saveConfiguration(null);
    }
   catch (IOException e) { }
}


public static void addHyperlinkListener(String protocol,HyperlinkListener hl)
{
   hyperlink_config.put(protocol,hl);
}

public static HyperlinkListener getListenerForProtocol(String protocol)
{
   return hyperlink_config.get(protocol);
}



/********************************************************************************/
/*										*/
/*	Hide and show methods							*/
/*										*/
/********************************************************************************/

public int getDefaultViewportHeight()
{
   int sz = getRootPane().getHeight();
   sz -= BUBBLE_OVERVIEW_HEIGHT + BUBBLE_TOP_BAR_HEIGHT;
   return sz;
}



void enableWindowShades()
{
   bubble_area.addMouseMotionListener(new OverviewListener());
   // channel_area.addMouseMotionListener(new OverviewListener());
}


void forceShadeDown()
{
   windowShadeDown();
   shade_down = !shade_down;
}

private boolean isShadeDown()
{
   if (shade_delta != 0) return false;
   int curht = bubble_overview.getHeight() + bubble_topbar.getHeight();
   if (curht >= BUBBLE_OVERVIEW_HEIGHT + BUBBLE_TOP_BAR_HEIGHT - 5) return true;
   return false;
}


MouseMotionAdapter getShadeMotionAdapter()
{
   return new OverviewListener();
}


private void windowShadeUp()
{
   setShadeDelta(-1);
}



private void windowShadeDown()
{
   setShadeDelta(1);
}



private synchronized void setShadeDelta(int d)
{
   if (shade_delta == d) return;

   int od = shade_delta;
   shade_delta = d;
   if (od == 0) {
      Timer nt = new Timer(SHADE_UPDATE_TIME,new ShadeUpdater());
      nt.setRepeats(true);
      nt.start();
    }
}


private class OverviewListener extends MouseMotionAdapter {

   @Override public void mouseMoved(MouseEvent e) {
      JComponent twin = getRootPane();
      Component win = (Component) e.getSource();
      Point pt1 = SwingUtilities.convertPoint(win,e.getPoint(),twin);

      int ht = bubble_overview.getHeight()+bubble_topbar.getHeight();

      // JComponent pwin = win;
      // if (win.getParent() instanceof JViewport) pwin = (JComponent) win.getParent();
      // Point pt = SwingUtilities.convertPoint(pwin,0,0,twin);
      // int ypos = pt1.y - pt.y;
      int ypos = pt1.y;
//	System.err.println("POINT " + ypos + " " + ht);

      int htgt = BUBBLE_OVERVIEW_HEIGHT + BUBBLE_TOP_BAR_HEIGHT;
      if (ht == 1) {
	 if (ypos < SHADE_PULL_DOWN_START) {
	    windowShadeDown();
	  }
       }
      else if (ypos > htgt + SHADE_PULL_DOWN_END && shade_down) {
	 windowShadeUp();
       }
      else if (ht != htgt) {
	 windowShadeDown();
       }
    }

}	// end of inner class OverviewListener




private class ShadeUpdater implements ActionListener {

   private long last_time;
   private double move_delta;

   ShadeUpdater() {
      last_time = System.currentTimeMillis();
      long updown = BUDA_PROPERTIES.getLong("Buda.shade.time",SHADE_UP_DOWN_TIME);
      if (updown <= 0) updown = 1;
      move_delta = BUBBLE_OVERVIEW_HEIGHT + BUBBLE_TOP_BAR_HEIGHT;
      move_delta /= updown;
    }

   @Override public void actionPerformed(ActionEvent e) {
      long now = System.currentTimeMillis();
      long delta = now-last_time;
      int move = (int)(move_delta * delta * shade_delta);

      boolean done = false;
      int curht = bubble_overview.getHeight() + bubble_topbar.getHeight();
      int startht = curht;
      curht += move;
      if (curht < 1) {
	 curht = 1;
	 move = curht - startht;
	 done = true;
       }
      else if (curht > BUBBLE_OVERVIEW_HEIGHT + BUBBLE_TOP_BAR_HEIGHT) {
	 curht = BUBBLE_OVERVIEW_HEIGHT + BUBBLE_TOP_BAR_HEIGHT;
	 move = curht - startht;
	 done = true;
       }

      for (Component c : button_panels) {
	 Dimension csz = c.getSize();
	 csz.height = curht;
	 c.setSize(csz);
	 c.setMinimumSize(csz);
	 c.setMaximumSize(csz);
	 c.setPreferredSize(csz);
       }

      Dimension topsz = bubble_topbar.getSize();
      if (curht < BUBBLE_TOP_BAR_HEIGHT) topsz.height = curht;
      else topsz.height = BUBBLE_TOP_BAR_HEIGHT;
      bubble_topbar.setMinimumSize(topsz);
      bubble_topbar.setPreferredSize(topsz);
      bubble_topbar.setMaximumSize(topsz);
      bubble_topbar.setSize(topsz);

      Dimension ovrsz = bubble_overview.getSize();
      if (curht <= BUBBLE_TOP_BAR_HEIGHT) ovrsz.height = 0;
      else ovrsz.height = curht - BUBBLE_TOP_BAR_HEIGHT;
      bubble_overview.setMinimumSize(ovrsz);
      bubble_overview.setPreferredSize(ovrsz);
      bubble_overview.setMaximumSize(ovrsz);
      bubble_overview.setSize(ovrsz);

      shade_safe = false;
      Point rv = bubble_view.getViewPosition();
      rv.y += move;
      bubble_view.setViewPosition(rv);

      Rectangle r = bubble_view.getBounds();
      Rectangle rtop = bubble_view.getParent().getBounds();
      r.height = rtop.height - curht;
      r.y = curht;
      bubble_view.setBounds(r);
      shade_safe = done;

      BudaBubble toolbar = BudaToolbar.getToolbar(bubble_area);
      for (BudaBubble bbl : bubble_area.getBubbles()) {
	 if (bbl.isFloating() && bbl != toolbar) {
	    Rectangle rbbl = bbl.getBounds();
	    rbbl.y -= move;
	    bbl.setBounds(rbbl);
	  }
       }
      if (toolbar.isVisible()) {
	 Rectangle rbbl = toolbar.getBounds();
	 rbbl.y -= move;
	 // System.err.println("TOOL BOUNDS " + rbbl + " " + move);
	 toolbar.setBounds(rbbl);
       }

      // revalidate();

      synchronized (BudaRoot.this) {
	 if (shade_delta == 0 || done) {
	    shade_delta = 0;
	    Timer nt = (Timer) e.getSource();
	    nt.stop();
	    return;
	  }
       }
    }

}	// end of inner class ShadeUpdater




/********************************************************************************/
/*										*/
/*	Action handlers 							*/
/*										*/
/********************************************************************************/

private class EscapeHandler extends AbstractAction {

   private static final long serialVersionUID = 1;

   @Override public void actionPerformed(ActionEvent e) {
      if (demo_thread != null) {
         demo_thread.stopDemonstration();
         return;
       }
   
      removeCurrentBubble();
    }

}	// end of inner class EscapeHandler



private class RemoveHandler extends AbstractAction {

   private static final long serialVersionUID = 1;

   @Override public void actionPerformed(ActionEvent e) {
      removeCurrentBubble();
    }

}	// end of inner class RemoveHandler




private class SearchKeyHandler extends AbstractAction {

   private boolean doc_search;
   private boolean proj_search;
   private boolean non_grouping;
   private boolean from_bubble;

   private static final long serialVersionUID = 1;

   SearchKeyHandler(boolean proj,boolean doc, boolean from) {
      proj_search = proj;
      doc_search = doc;
      from_bubble = from;
      non_grouping = false;
    }

   SearchKeyHandler(boolean proj, boolean doc, boolean from, boolean nogroup) {
      proj_search = proj;
      doc_search = doc;
      from_bubble = from;
      non_grouping = nogroup;
   }

   @Override public void actionPerformed(ActionEvent e) {
      Point pt = MouseInfo.getPointerInfo().getLocation();//getCurrentBubbleArea().getCurrentMouse();
      SwingUtilities.convertPointFromScreen(pt, getCurrentBubbleArea());
      if (pt == null) return;

      BudaBubble focbub = getCurrentBubbleArea().getFocusBubble();
      if (focbub != null) {
      Rectangle bubarea = focbub.getBounds();
      SwingUtilities.convertRectangle(BudaRoot.this, bubarea, getCurrentBubbleArea());
      if (from_bubble && bubarea.contains(pt) && focbub != search_bubble) {
	  if (!non_grouping) pt.x = bubarea.x+bubarea.width+BUBBLE_CREATION_NEAR_SPACE;
	  else pt.x = bubarea.x+bubarea.width+BUBBLE_CREATION_SPACE;
	  pt.y = bubarea.y;
	  }
       }

      if (doc_search && proj_search) createMergedSearchBubble(pt,null,null);
      else if (proj_search) createSearchBubble(pt,null,null,false);
      else createDocSearchBubble(pt,null,null);
    }

}	// end of inner class SearchKeyHandler



private static class MetricsHandler extends AbstractAction {

   private static final long serialVersionUID = 1;

   @Override public void actionPerformed(ActionEvent e) {
      BoardMetrics.forceDump();
    }

}	// end of inner class MetricsHandler



private static class HelpHandler extends AbstractAction {

   private static final long serialVersionUID = 1;

   HelpHandler() {
      super("Help");
    }

   @Override public void actionPerformed(ActionEvent e) {
      showHelp(null);
    }

}	// end of inner class HelpHandler




private class SaveHandler extends AbstractAction {

   private static final long serialVersionUID = 1;

   SaveHandler() {
      super("Save All");
    }

   @Override public void actionPerformed(ActionEvent e) {
      handleSaveAllRequest();
    }

}	// end of inner class HelpHandler




private class CommitHandler extends AbstractAction {

   private static final long serialVersionUID = 1;

   CommitHandler() {
      super("Commit All");
    }

   @Override public void actionPerformed(ActionEvent e) {
      handleCommitAllRequest();
    }

}	// end of inner class HelpHandler




/********************************************************************************/
/*										*/
/*	Action methods for moving focus or bubbles				*/
/*										*/
/********************************************************************************/

private class ZoomHandler extends AbstractAction implements ActionListener {

   private int zoom_direction;

   private static final long serialVersionUID = 1;

   ZoomHandler(int dir) {
      zoom_direction = dir;
    }

   @Override public void actionPerformed(ActionEvent e) {
      double v = getCurrentBubbleArea().getScaleFactor();
      if (zoom_direction > 0) v /= 0.9;
      else if (zoom_direction < 0) v *= 0.9;
      else v = 1.0;
      getCurrentBubbleArea().setScaleFactor(v);
    }

}	// end of inner class ZoomHandler




private class FloaterHandler extends AbstractAction implements ActionListener {
   private static final long serialVersionUID = 1L;

   @Override public void actionPerformed(ActionEvent e) {
      Point pt = bubble_area.getCurrentMouse();
      if (pt == null) return;
      BudaBubbleArea focbubarea = getCurrentBubbleArea();
      BudaBubble focbub = focbubarea.getFocusBubble();
      if (focbub != null) {
	 if(focbub.isFloating()) focbubarea.setBubbleFloating(focbub,false);
	 else focbubarea.setBubbleFloating(focbub,true);
	 focbubarea.repaint();
       }
   }

}	// end of inner class FloaterHandler



private class FocusDirectionHandler extends AbstractAction implements ActionListener {

   static final int LEFT = 0;
   static final int RIGHT = 1;
   static final int UP = 2;
   static final int DOWN = 3;

   private int my_dir;
   private float bad_dist_factor;
   private static final long serialVersionUID = 1L;

   FocusDirectionHandler(int dir, float scale) {
      my_dir = dir;
      bad_dist_factor = scale;
   }

   @Override public void actionPerformed(ActionEvent e) {
      Iterable<BudaBubble> ibb = getCurrentBubbleArea().getBubbles();
      BudaBubble focbub = getCurrentBubbleArea().getFocusBubble();
      if (focbub == null) return;
      BudaBubble bestcandidate = null;
      BudaBubble notbadcandidate = null;
      int shortdist = getViewport().width/2;
      int badshortdist = shortdist;
      for(BudaBubble bb : ibb) {
	 if (bb != focbub) {
	    int appropriatecalc = 0;
	    int baddist = 0;
	    switch (my_dir) {
	       case LEFT:
		  appropriatecalc = focbub.getX() - bb.getX() - bb.getWidth();
		  baddist = Math.abs(focbub.getY() - bb.getY());
		  break;
	       case RIGHT:
		  appropriatecalc = bb.getX() - focbub.getX() - focbub.getWidth();
		  baddist = Math.abs(focbub.getY() - bb.getY());
		  break;
	       case UP:
		  appropriatecalc = focbub.getY() - bb.getY() - bb.getHeight();
		  baddist = Math.abs(focbub.getX() - bb.getX());
		  break;
	       case DOWN:
		  appropriatecalc = bb.getY() - focbub.getY() - focbub.getHeight();
		  baddist = Math.abs(focbub.getX() - bb.getX());
		  break;
	    }
	    if (appropriatecalc < shortdist && appropriatecalc > 0 && (int)(baddist*bad_dist_factor) < appropriatecalc) {
	       bestcandidate = bb;
	       shortdist = appropriatecalc;
	       badshortdist = appropriatecalc;
	    }
	    else if (appropriatecalc < badshortdist && appropriatecalc > 0) {
	       notbadcandidate = bb;
	       badshortdist = appropriatecalc;
	    }
	 }
      }
      if (bestcandidate != null) bestcandidate.grabFocus();
      else if (notbadcandidate != null) notbadcandidate.grabFocus();
   }

}	// end of inner class FocusDirectionHandler



private class PanHandler extends AbstractAction implements ActionListener {

   private static final long serialVersionUID = 1L;

   private double pan_scale;
   private int x_direction;
   private int y_direction;

   PanHandler(int horiz, int vert) {
      this(0.33, horiz, vert);
   }

   PanHandler(double scale, int horiz, int vert) {
      pan_scale = scale;
      x_direction = horiz == 0 ? 0 : horiz > 0 ? 1 : -1;
      y_direction = vert == 0 ? 0 : vert > 0 ? 1 : -1;
   }

   @Override public void actionPerformed(ActionEvent e) {
      int xmove = x_direction * (int)(getViewport().width*pan_scale);
      int ymove = y_direction * (int)(getViewport().height*pan_scale);
      moveViewport(xmove, ymove);
   }

}	// end of inner class PanHandler\



private class PrintHandler extends AbstractAction  {

   private static final long serialVersionUID = 1;

   @Override public void actionPerformed(ActionEvent e) {
      BudaBubbleArea bba = getCurrentBubbleArea();
      BudaBubble bbl = bba.getFocusBubble();
      if (bbl == null) return;
      PrinterJob pjob = PrinterJob.getPrinterJob();
      PageFormat fmt = pjob.defaultPage();
      fmt.setOrientation(PageFormat.PORTRAIT);
      fmt = pjob.pageDialog(fmt);
      pjob.setPrintable(bbl,fmt);
      if (pjob.printDialog()) {
	 try {
	    pjob.print();
	  }
	 catch (PrinterException ex) {
	    BoardLog.logE("BUDA","Window printing problem",ex);
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Configuration methods							*/
/*										*/
/********************************************************************************/

/**
 *	Save the current configuration as an XML document in the given file.
 **/

public void saveConfiguration(File f) throws IOException
{
   if (BoardSetup.getSetup().isServerMode()) {
      String msg = "<BUDA SOURCE='SERVER' CMD='SAVECONFIG'";
      if (f != null) msg += " FILE='" + f.getAbsolutePath() + "'";
      else msg += " FILE='*'";
      msg += "/>";
      BoardSetup.getSetup().getMintControl().send(msg);
      return;
    }

   File hf = BoardSetup.getHistoryFile();
   if (f == null) f = BoardSetup.getConfigurationFile();

   BoardLog.logD("BUDA","Save configuration " + f);

   File hf1 = new File(hf.getPath() + ".new");
   BudaXmlWriter hxw = new BudaXmlWriter(hf1);
   outputHistory(hxw);
   hxw.close();
   hf.delete();
   hf1.renameTo(hf);

   File f1 = new File(f.getPath() + ".new");
   BudaXmlWriter xw = new BudaXmlWriter(f1);
   outputXml(xw,false);
   xw.close();
   f.delete();
   f1.renameTo(f);
}



/**
 *	Add a configurator with the given key for creating/saving bubbles.
 **/

public static void addBubbleConfigurator(String key,BubbleConfigurator bc)
{
   bubble_config.put(key,bc);
}



/**
 *	Add a port configurator for creating LinkPorts of particular types.
 **/

public static void addPortConfigurator(String key,PortConfigurator pc)
{
   port_config.put(key,pc);
}



BudaBubble createBubble(BudaBubbleArea bba,Element e,Rectangle delta)
{
   return createBubble(bba,e,delta,0);
}



public BudaBubble createConfigBubble(BudaBubbleArea bba,Element e)
{
   return createBubble(bba,e,null,0,false);
}



BudaBubble createBubble(BudaBubbleArea bba,Element e,Rectangle delta,int dx)
{
   return createBubble(bba,e,delta,dx,true);
}

BudaBubble createBubble(BudaBubbleArea bba,Element e,Rectangle delta,int dx,boolean upd)
{
   String key = IvyXml.getAttrString(e,"CONFIG");
   if (key == null) {
      // handle local bubbles
      return null;
    }

   BubbleConfigurator bc = bubble_config.get(key);
   BudaBubble bb = null;
   if (bc != null) bb = bc.createBubble(bba,e);
   if (bb == null) return null;

   bb.setBorderColor(IvyXml.getAttrColor(e,"BORDER"),IvyXml.getAttrColor(e,"FOCUS"));
   bb.setInteriorColor(IvyXml.getAttrColor(e,"INTERIOR"));
   bb.setCreationTime(IvyXml.getAttrLong(e,"CTIME"));

   Dimension size = new Dimension(IvyXml.getAttrInt(e,"W"),IvyXml.getAttrInt(e,"H"));
   bb.setSize(size);
   BudaBubblePosition pos = BudaBubblePosition.MOVABLE;
   int x = IvyXml.getAttrInt(e,"X") + dx;
   int y = IvyXml.getAttrInt(e,"Y");

   Dimension bbasize = bba.getSize();
   if (x + size.width < 0 || x > bbasize.width || y + size.height < 0 || y > bbasize.height)
      return null;

   boolean docked = IvyXml.getAttrBool(e,"DOCKED");
   if (IvyXml.getAttrBool(e,"FIXED")) pos = BudaBubblePosition.FIXED;
   else if (!IvyXml.getAttrBool(e,"FLOAT")) docked = false;
   
   if (IvyXml.getAttrBool(e,"FLOAT") || docked) {
      Rectangle va = bubble_view.getViewRect();
      pos = docked ? BudaBubblePosition.DOCKED : BudaBubblePosition.FLOAT;
      if (delta != null) {
	 x -= delta.x;
	 y -= delta.y;
       }
      if (x + size.width > va.width) x = va.width - size.width;
      if (y + size.height > va.height) y = va.height - size.height;
      if (x < 0) x = 0;
      if (y < 0) y = 0;
    }

   bb.setTransient(false);		// transient bubbles aren't saved

   if (upd) {
      BudaConstraint cnst = new BudaConstraint(pos,x,y);
      add(bb,cnst);
    }
   else {
      bb.setLocation(x,y);
    }

   return bb;
}


int matchConfiguration(String key,Element e,BudaBubble bb)
{
   if (key == null) return 0;

   BudaBubbleOutputer bbo = bb.getBubbleOutputer();
   if (bbo == null) return 0;
   if (!bbo.getConfigurator().equals(key)) return 0;
   // could ask bbo to output xml to a BudaXmlWriter and then compare

   BubbleConfigurator bc = bubble_config.get(key);
   if (bc != null && bc.matchBubble(bb,e)) return 2;

   return 1;
}



/**
 *	Create a link port given the configuration XML.
 **/

public LinkPort createPort(BudaBubble bb,Element e)
{
   String key = IvyXml.getAttrString(e,"CONFIG");

   if (key == null) {
      return null;
    }

   PortConfigurator pc = port_config.get(key);
   LinkPort bp = null;
   if (pc != null) bp = pc.createPort(bb,e);

   return bp;
}



private static class DefaultPortConfigurator implements PortConfigurator {

   @Override public LinkPort createPort(BudaBubble bb,Element e) {
      return new BudaDefaultPort(e);
    }

}	// end of inner class DefaultPortConfigurator




/********************************************************************************/
/*										*/
/*	Input/Output methods							*/
/*										*/
/********************************************************************************/

/**
 *	Saves the current configuration into a file so that it can be reloaded
 *	or shared.
 **/

public void outputXml(BudaXmlWriter xw,boolean history)
{
   xw.begin("ROOT");
   // xw.field("DEFAULTHT",getDefaultViewportHeight());
   // xw.field("ROOTHT",getRootPane().getHeight());
   // xw.field("VIEWHT",bubble_view.getHeight());
   // xw.field("TOPHT",bubble_topbar.getHeight());
   // xw.field("OVERHT",bubble_overview.getHeight());

   int d1 = BUBBLE_TOP_BAR_HEIGHT + BUBBLE_OVERVIEW_HEIGHT;
   d1 -=  bubble_topbar.getHeight() + bubble_overview.getHeight();
   int d2 = bubble_view.getHeight() - getDefaultViewportHeight();
   xw.field("DELTA",d1);
   xw.field("DELTA1",d2);

   xw.element("SHAPE",getBounds());
   bubble_topbar.outputXml(xw);
   bubble_overview.outputXml(xw);
   bubble_view.outputXml(xw);
   bubble_area.outputXml(xw);
   if (history) {
      xw.begin("TASKS");
      for (BudaTask bt : getAllTasks()) {
	 bt.outputXml(xw);
       }
      xw.end("TASKS");
      relation_data.outputXml(xw);
    }
   for (BubbleConfigurator bc : bubble_config.values()) bc.outputXml(xw,history);

   xw.end("ROOT");
}



void outputHistory(BudaXmlWriter xw)
{
   xw.begin("HISTORY");
   xw.begin("TASKS");
   for (BudaTask bt : getAllTasks()) {
      bt.outputXml(xw);
    }
   xw.end("TASKS");
   relation_data.outputXml(xw);

   for (BubbleConfigurator bc : bubble_config.values()) bc.outputXml(xw,true);

   xw.end("HISTORY");
}



private void setupSession(Element config)
{
   if (config != null) {
      Rectangle delta = null;

      Element view = IvyXml.getChild(config,"VIEWAREA");
      Element viewpos = IvyXml.getChild(view,"VIEW");
      if (viewpos != null) {
	 int px = (int) IvyXml.getAttrDouble(viewpos,"X",0);
	 int py = (int) IvyXml.getAttrDouble(viewpos,"Y",0);
	 int pw = (int) IvyXml.getAttrDouble(viewpos,"WIDTH",0);
	 int ph = (int) IvyXml.getAttrDouble(viewpos,"HEIGHT",0);
	 if (pw != 0 && ph != 0) delta = new Rectangle(px,py,pw,ph);
       }

      Element bubbles = IvyXml.getChild(config,"BUBBLEAREA");
      if (bubbles != null) {
	 bubble_area.configure(bubbles,delta);
       }
    }

   noteConfigureDone();
}



private void setupView(Element config)
{
   int delta = IvyXml.getAttrInt(config,"DELTA",0);
   Element vacfg = IvyXml.getChild(config,"VIEWAREA");
   Element viewcfg = IvyXml.getChild(vacfg,"VIEW");
   int px = (int) IvyXml.getAttrDouble(viewcfg,"X",BUBBLE_DISPLAY_START_X);
   int py = (int) IvyXml.getAttrDouble(viewcfg,"Y",BUBBLE_DISPLAY_START_Y);
   py += delta;
   int mintop = BUBBLE_OVERVIEW_HEIGHT + BUBBLE_TOP_BAR_HEIGHT;
   if (py < mintop) py = mintop;

   setViewport(px,py);
}


public synchronized void waitForSetup()
{
   while (!view_setup) {
      try {
	 wait(1000);
       }
      catch (InterruptedException e) { }
    }
}


private synchronized void doneSetup()
{
   view_setup = true;

   if (BUDA_PROPERTIES.getBoolean("Buda.show.tool.menu")) {
      Action act = BudaToolbar.getMenuBarAction(this);
      act.actionPerformed(null);
    }

   if (BUDA_PROPERTIES.getBoolean("Buda.overview.shades")) {
      enableWindowShades();
    }

   notifyAll();
}


private class RestoreSession implements Runnable {

   private Element session_config;

   RestoreSession(Element config) {
      session_config = config;
    }

   @Override public void run() {
      setupSession(session_config);
      setupView(session_config);
      
      doneSetup();
    }

}	// end of inner class RestoreSession




private void loadHistory(Element e)
{
   // TODO: Need to monitor this file as it might be shared

   if (e == null) return;

   Element taskcfg = IvyXml.getChild(e,"TASKS");
   if (taskcfg != null) {
      for (Element te : IvyXml.children(taskcfg,"TASK")) {
	 task_shelf.add(new BudaTask(te));
       }
    }

   relation_data.loadRelations(IvyXml.getChild(e,"RELATIONS"));

   for (BubbleConfigurator bc : bubble_config.values()) bc.loadXml(null,e);
}



/********************************************************************************/
/*										*/
/*	New bubble creation methods						*/
/*										*/
/********************************************************************************/

public static void registerBubbler(String name,BudaBubbler bbb)
{ }


public static BudaBubbler getBubbler(String id)
{
   return null;
}




/********************************************************************************/
/*										*/
/*	Channel set methods							*/
/*										*/
/********************************************************************************/

/**
 *	Set the current channel set to the designated one.  This is used, for
 *	example, to change to a debugging perspective.	Setting the channel set
 *	to null goes back to the default context.
 **/

public void setChannelSet(BudaChannelSet cs)
{
   if (cur_channels == cs) return;

   cur_channels = cs;
   if (cs == null) {
      bubble_view.setVisible(true);
      channel_area.setVisible(false);
      for (Component c : channel_area.getComponents()) c.setVisible(false);
    }
   else {
      bubble_view.setVisible(false);
      channel_area.setVisible(true);
      for (Component c : channel_area.getComponents()) c.setVisible(false);
      cs.getComponent().setVisible(true);
    }
}



/**
 *	Set the current channel (and its channel set)
 **/

public void setCurrentChannel(Component c)
{
   BudaBubbleArea bba = findBudaBubbleArea(c);
   if (bba == null) return;

   BudaChannelSet cs = bba.getChannelSet();
   if (cs != null) cs.setBubbleArea(bba);

   setChannelSet(cs);
}



/**
 *	Return the current channel set
 **/

public BudaChannelSet getChannelSet()			{ return cur_channels; }



void addChannelSet(BudaChannelSet cs)
{
   cs.getComponent().setVisible(false);
   channel_area.add(cs.getComponent());
}



/********************************************************************************/
/*										*/
/*	Color methods								*/
/*										*/
/********************************************************************************/

static Color getGroupColor(double alpha)
{
   Color c0 = group_colors[color_index];
   color_index = (color_index + color_step) % group_colors.length;
   if (alpha == 1) return c0;

   return BoardColors.transparent(c0,alpha);
}




/********************************************************************************/
/*										*/
/*	Task shelf methods							*/
/*										*/
/********************************************************************************/

/**
 *	Create a new BudaTask from its XML description and add it to the task shelf
 **/

public void addTask(Element xml)
{
   BudaTask bt = new BudaTask(xml);

   addTask(bt);
}



void addTask(BudaTask t)
{
   if (t == null) return;

   synchronized (task_shelf) {
      task_shelf.add(t);
    }
}



void removeTask(BudaTask t)
{
   if (t == null) return;

   synchronized (task_shelf) {
      task_shelf.remove(t);
    }
}



Collection<BudaTask> getAllTasks()
{
   synchronized (task_shelf) {
      return new ArrayList<BudaTask>(task_shelf);
    }
}



private static class TaskComparator implements Comparator<BudaTask> {

   @Override public int compare(BudaTask t1,BudaTask t2) {
      String name1 = t1.getName();
      String name2 = t2.getName();
      if (Character.isLetter(name1.charAt(0))) {
	 if (Character.isLetter(name2.charAt(0))) {
	    return name1.compareTo(name2);
	 }
	 return -1;
       }
      if (Character.isLetter(name2.charAt(0))) {
	 return 1;
       }
      return name1.compareTo(name2);
      //return t1.getName().compareTo(t2.getName());
    }

}	// end of inner class TaskComparator




/********************************************************************************/
/*										*/
/*	Relationship methods							*/
/*										*/
/********************************************************************************/

void noteNamedBubbleGroup(BudaBubbleGroup bg)
{
   relation_data.addGroup(bg);
}

/********************************************************************************/
/*										*/
/*	Callback methods for bubbles						*/
/*										*/
/********************************************************************************/

/**
 *	Add a callback for detecting bubble addition/removal/focus changes.
 **/

public static void addBubbleViewCallback(BubbleViewCallback cb)
{
   view_callbacks.add(cb);
}



/**
 *	Remove a callback for detecting bubble addition/removal/focus changes.
 **/

public static void removeBubbleViewCallback(BubbleViewCallback cb)
{
   view_callbacks.remove(cb);
}



void noteFocusChanged(BudaBubble bb,boolean set)
{
   bubble_overview.repaint();

   for (BubbleViewCallback cb : view_callbacks) {
      cb.focusChanged(bb,set);
    }
}



void noteBubbleAdded(BudaBubble bb)
{
   for (BubbleViewCallback cb : view_callbacks) {
      cb.bubbleAdded(bb);
    }
}



void noteBubbleRemoved(BudaBubble bb)
{
   for (BubbleViewCallback cb : view_callbacks) {
      cb.bubbleRemoved(bb);
    }
}


boolean noteBubbleActionDone(BudaBubble bb)
{
   boolean fg = false;

   for (BubbleViewCallback cb : view_callbacks) {
      fg |= cb.bubbleActionDone(bb);
    }

   return fg;
}


public void noteBubbleCopy(BudaBubble frm,BudaBubble to)
{
   for (BubbleViewCallback cb : view_callbacks) {
      cb.copyFromTo(frm,to);
    }
}



void noteWorkingSetAdded(BudaWorkingSet ws)
{
   for (BubbleViewCallback cb : view_callbacks) {
      cb.workingSetAdded(ws);
    }
}



void noteWorkingSetRemoved(BudaWorkingSet ws)
{
   for (BubbleViewCallback cb : view_callbacks) {
      cb.workingSetRemoved(ws);
    }
}



void noteConfigureDone()
{
   for (BubbleViewCallback cb : view_callbacks) {
      cb.doneConfiguration();
    }
}


void noteNamedGroup(BudaBubbleGroup grp,String oldname)
{
   for (BubbleViewCallback cb : view_callbacks) {
      cb.noteNamedGroup(grp,oldname);
    }
}



void addGroupButtons(BudaBubbleGroup grp,JPopupMenu menu) 
{
   for (BubbleViewCallback cb : view_callbacks) {
      cb.addGroupButtons(grp,menu);
    }
}




/********************************************************************************/
/*										*/
/*	Callback methods for files						*/
/*										*/
/********************************************************************************/


/**
 *	Register a new file handler
 **/

public static void addFileHandler(BudaFileHandler fh)
{
   file_handlers.add(fh);
}



/**
 *	Remove a file handler from callbacks
 **/

public static void removeFileHandler(BudaFileHandler fh)
{
   file_handlers.remove(fh);
}




/********************************************************************************/
/*										*/
/*	Task Manager methods							*/
/*										*/
/********************************************************************************/

/**
 * sets cursor to wait cursor for all components in the frame
 */
public void startWaitCursor() {
   BudaCursorManager.setGlobalCursorForComponent(this,new Cursor(Cursor.WAIT_CURSOR));
}

/**
 * returns cursor to default
 */

public void stopWaitCursor() {
   BudaCursorManager.resetDefaults(this);
}



/********************************************************************************/
/*										*/
/*	Window closing methods							*/
/*										*/
/********************************************************************************/

void handleCloseRequest(boolean halt)
{
   boolean save = BUDA_PROPERTIES.getBoolean("Buda.close.save");
   if (BUDA_PROPERTIES.getBoolean("Buda.close.ask")) {
      SwingGridPanel pnl = new SwingGridPanel();
      pnl.beginLayout();
      pnl.addBannerLabel("Exit from Code Bubbles");
      pnl.addSeparator();
      JCheckBox savebox = null;
      if (BUDA_PROPERTIES.getBoolean("Buda.ask.save.on.close",true)) {
	 savebox = pnl.addBoolean("Save Any Changes",save,null);
       }
      JCheckBox askbox = pnl.addBoolean("Always exit without prompt",false,null);
      int sts = JOptionPane.showConfirmDialog(this,pnl,"Close Confirmation",
						 JOptionPane.OK_CANCEL_OPTION,
						 JOptionPane.QUESTION_MESSAGE);
      if (sts == JOptionPane.CANCEL_OPTION) return;
      if ((savebox != null && savebox.isSelected() != save) || askbox.isSelected()) {
	 if (savebox != null) save = savebox.isSelected();
	 BUDA_PROPERTIES.setProperty("Buda.close.save",Boolean.toString(save));
	 BUDA_PROPERTIES.setProperty("Buda.close.ask",Boolean.toString(!askbox.isSelected()));
	 try {
	    BUDA_PROPERTIES.save();
	  }
	 catch (IOException e) { }
       }
    }

   if (save) handleSaveAllRequest();

   if (handleQuitRequest()) {
      JOptionPane opt = new JOptionPane("Saving Workspace -- Please be patient",JOptionPane.INFORMATION_MESSAGE);
      JDialog dlg = opt.createDialog(this,"Saving Workspace");
      dlg.setModal(false);
      dlg.setVisible(true);
      saveWorkspace();
      if (halt) BoardThreadPool.start(new Stopper(200));
    }
}



void handleChangeWorkspaceRequest()
{
   handleCloseRequest(false);
   BoardSetup.getSetup().restartForNewWorkspace();
}




private void saveWorkspace()
{
   // BoardSetup setup = BoardSetup.getSetup();
   // if (setup.getRunMode() == RunMode.CLIENT) {
      // MintControl ctrl = setup.getMintControl();
      // MintDefaultReply hdlr = new MintDefaultReply();
      // ctrl.send("<BUMP TYPE='SAVEWORKSPACE'>",hdlr,MintConstants.MINT_MSG_FIRST_NON_NULL);
      // hdlr.waitForString(300000);
    // }
   // else {
      // BumpClient.getBump().saveWorkspace();
    // }

   BumpClient.getBump().saveWorkspace();
}


private class Stopper implements Runnable {

   private long stop_delay;

   Stopper(long delay) {
      stop_delay = delay;
    }

   @Override public void run() {
      BoardLog.logD("BUDA","Starting stopper " + stop_delay);
      if (stop_delay > 0) {
         try {
            Thread.sleep(stop_delay);
          }
         catch (InterruptedException e) {  }
       }
      System.exit(0);
    }

}	// end of inner class Stopper



private class WindowCloser extends WindowAdapter {

   @Override public void windowClosing(WindowEvent e) {
      handleCloseRequest(true);
    }

}	// end of inner class WindowCloser



/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

@Override public Graphics getGraphics()
{
   Graphics2D g2 = (Graphics2D) super.getGraphics();

   if (debug_graphics) {
      SwingDebugGraphics sg2 = new SwingDebugGraphics(g2);
      sg2.setDebugOptions(SwingDebugGraphics.LOG_OPTION);
      g2 = sg2;
    }

   return g2;
}



/********************************************************************************/
/*										*/
/*	Button Panel								*/
/*										*/
/********************************************************************************/

/**
 * This class models the ButtonPanel that contains the "Report Bugs"
 * and the "Options" buttons.
 **/

private class ButtonPanel extends JPanel
{

   private static final long serialVersionUID = 1;


   public ButtonPanel() {
      super();
      this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      this.add(Box.createVerticalStrut(BUDA_BUTTON_SEPARATION));
      BoardColors.setColors(this,BUTTON_PANEL_TOP_COLOR_PROP);
      Dimension d =  new Dimension(0,BUBBLE_OVERVIEW_HEIGHT+BUBBLE_TOP_BAR_HEIGHT);
      setMinimumSize(d);
    }

   @Override protected void paintComponent(Graphics g0) {
      super.paintComponent(g0);
      Graphics2D g = (Graphics2D) g0.create();
      Color tc = BoardColors.getColor(BUTTON_PANEL_TOP_COLOR_PROP);
      Color bc = BoardColors.getColor(BUTTON_PANEL_BOTTOM_COLOR_PROP);
      Paint p = new GradientPaint(0f, 0f,tc, 0f, BUBBLE_OVERVIEW_HEIGHT, bc);

      g.setPaint(p);
      g.fillRect(0, 0, this.getWidth() , this.getHeight());
    }

}	// end of inner class ButtonPanel





/********************************************************************************/
/*										*/
/*	Checkpoint timer							*/
/*										*/
/********************************************************************************/

private class CheckpointTimer implements ActionListener {

   CheckpointTimer() {
      if (BUBBLE_CHECKPOINT_TIME > 0) {
	 javax.swing.Timer timer = new javax.swing.Timer(BUBBLE_CHECKPOINT_TIME,this);
	 timer.setRepeats(true);
	 timer.start();
       }
    }

   @Override public void actionPerformed(ActionEvent e) {
      handleCheckpointAllRequest();
    }

}	// end of inner class CheckpointTimer




/********************************************************************************/
/*										*/
/*	Event queue to intercept mouse events					*/
/*										*/
/********************************************************************************/

private static class MouseEventQueue extends EventQueue {

   private BudaBubbleArea drag_area = null;
   private Component base_component = null;

   @Override protected void dispatchEvent(AWTEvent e) {
      // only want mouse events for buttons other than 1
      if (!(e instanceof MouseEvent)) {
         if (e instanceof InputEvent) {
            BudaHover.removeHovers();
            InputEvent ie = (InputEvent) e;
            BoardMetrics.noteActive(ie.getWhen());
          }
         resend(e);
         return;
       }
   
      MouseEvent me = (MouseEvent) e;
      BoardMetrics.noteActive(me.getWhen());
      last_mouse = me;
   
      if (drag_area == null) {
         if (me.getButton() == MouseEvent.BUTTON1 ||
        	(me.getID() != MouseEvent.MOUSE_CLICKED && me.getID() != MouseEvent.MOUSE_PRESSED)) {
            resend(e);
            return;
          }
       }
   
      if (me.getID() == MouseEvent.MOUSE_DRAGGED || me.getID() == MouseEvent.MOUSE_RELEASED) {
         if (drag_area == null) {
            resend(e);
            return;
          }
       }
      else if (drag_area != null && me.getID() != MouseEvent.MOUSE_EXITED) {
         clearDragArea();
       }
   
      if (drag_area == null) setDragArea(me);
   
      if (drag_area != null) {
         Point pt = SwingUtilities.convertPoint(me.getComponent(),me.getPoint(),drag_area);
         me.translatePoint(pt.x-me.getX(),pt.y-me.getY());
         if (me.getID() == MouseEvent.MOUSE_DRAGGED || me.getID() == MouseEvent.MOUSE_MOVED)
            drag_area.processMouseMotionEvent(me);
         else
            drag_area.processMouseEvent(me);
         if (me.getID() == MouseEvent.MOUSE_RELEASED || me.getID() == MouseEvent.MOUSE_CLICKED)
            clearDragArea();
   
         return;
       }
   
      resend(e);
      return;
    }

   private void setDragArea(MouseEvent me) {
      if (drag_area != null) return;

      base_component = SwingUtilities.getDeepestComponentAt(me.getComponent(),me.getX(),me.getY());
      for (Component comp = base_component; comp != null; comp = comp.getParent()) {
	 if (!base_component.isEnabled()) base_component = comp;
	 if (comp instanceof BudaBubbleArea) {
	    drag_area = (BudaBubbleArea) comp;
	    break;
	  }
       }

      if (drag_area == null) base_component = null;
    }

   private void clearDragArea() {
      if (drag_area == null) return;

      if (base_component != null) {
	 for (Component comp = base_component; comp != drag_area; comp = comp.getParent()) {
	    if (comp == null) break;
	    comp.setEnabled(true);
	  }
       }

      drag_area = null;
    }

   private void resend(AWTEvent e) {
      try {
         super.dispatchEvent(e);
       }
      catch (Throwable t) {
         BoardLog.logE("BUDA","Problem processing user interface event: " + t,t);
       }
    }

}	// end of inner class MouseEventQueue



/********************************************************************************/
/*										*/
/*	Handle server->client messages						*/
/*										*/
/********************************************************************************/

private class ClientHandler implements MintHandler {

   @Override public void receive(MintMessage msg,MintArguments args) {
      String cmd = args.getArgument(0);
      String file = args.getArgument(1);
      if (file.equals("*")) file = null;
      String rply = null;

      try {
	 if (cmd == null) ;
	 else if (cmd.equals("SAVECONFIG")) {
	    if (file == null) saveConfiguration(null);
	    else {
	       File f = new File(file);
	       saveConfiguration(f);
	     }
	  }
	 else {
	    BoardLog.logD("BUDA","Unknown SERVER message " + cmd);
	  }
       }
      catch (Throwable t) {
	 BoardLog.logE("BUDA","Problem processing SERVER message " + cmd,t);
       }

      msg.replyTo(rply);
    }

}	// end of inner class ClientHandler




}	// end of class BudaRoot




/* end of BudaRoot.java */
