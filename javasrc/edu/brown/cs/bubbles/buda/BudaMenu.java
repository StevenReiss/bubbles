/********************************************************************************/
/*										*/
/*		BudaMenu.java							*/
/*										*/
/*	BUblles Display Area bubble menu					*/
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


package edu.brown.cs.bubbles.buda;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardMetrics;

import edu.brown.cs.ivy.swing.SwingGridPanel;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;



class BudaMenu implements BudaConstants, BudaConstants.BubbleViewCallback {




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Map<String,Set<MenuItem>>  menu_groups;
private List<MenuData>		   active_menus;

private static final int	MENU_DELTA = 15;





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BudaMenu()
{
   menu_groups = new LinkedHashMap<String,Set<MenuItem>>();
   active_menus = new ArrayList<MenuData>();
   BudaRoot.addBubbleConfigurator("BUDAMENU",new MenuConfigurator());
   BudaRoot.addBubbleViewCallback(this);
}



/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

void addMenuItem(String id,ButtonListener callback,Icon icon,String tooltip)
{
   String pfx = "*";
   String name = id;
   String order = "";

   int idx1 = id.indexOf("#");
   if (idx1 >= 0) {
      order = id.substring(0,idx1);
      id = id.substring(idx1+1);
    }
   int idx = id.indexOf(".");
   if (idx >= 0) {
      pfx = id.substring(0,idx);
      name = id.substring(idx+1);
    }

   Set<MenuItem> itms = menu_groups.get(pfx);
   if (itms == null) {
      itms = new TreeSet<MenuItem>();
      menu_groups.put(pfx,itms);
    }

   itms.add(new MenuItem(id,name,callback,icon,tooltip,order));
}




/********************************************************************************/
/*										*/
/*	Methods to handle creating search and menu together			*/
/*										*/
/********************************************************************************/

void createMenuAndSearch(BudaRoot br,Point pt,BudaBubble search)
{
   MenuPanel menu = createMenu(pt,search.getWidth());
   if (menu != null) {
      menu.setTransparent();
      Dimension sz = menu.getPreferredSize(); //search.getPreferredSize();
      //Point p1 = new Point(pt.x + sz.width + MENU_DELTA,pt.y);//-sz.height-MENU_DELTA);
      Point p1 = new Point(pt.x, pt.y-sz.height-MENU_DELTA);
      Rectangle viewport = br.getCurrentViewport();
      if (!viewport.contains(p1)) {
         p1 = new Point(pt.x, pt.y+search.getSize().height+MENU_DELTA);
       }
      BudaBubble bb = new MenuBubble(menu);
      MenuData md = new MenuData(search,bb);
      active_menus.add(md);
      BudaConstraint mcnst = new BudaConstraint(BudaBubblePosition.DIALOG,p1);
      br.add(bb,mcnst);
      Dimension sd = search.getSize();
      if (sd.width < menu.getWidth()) {
         sd.width = menu.getWidth();
         search.setSize(sd);
       }
    }

   BudaConstraint scnst = new BudaConstraint(BudaBubblePosition.DIALOG,pt);
   br.add(search,scnst);

   search.grabFocus();
}


void noteSearchUsed(BudaBubble bb)
{
   for (MenuData md : active_menus) {
      if (md.searchUsed(bb)) return;
    }
}


void noteMenuUsed(Component c)
{
   BudaBubble bb = BudaRoot.findBudaBubble(c);
   if (bb == null) return;

   for (MenuData md : active_menus) {
      if (md.menuUsed(bb)) break;
    }

   bb.setVisible(false);
}



/********************************************************************************/
/*										*/
/*	Methods to build the actual menu					*/
/*										*/
/********************************************************************************/

MenuPanel createMenu(Point pt,int width)
{
   if (menu_groups.size() == 0) return null;

   Set<Object> done1 = new HashSet<>();
   int itemcount = 0;
   for (Set<MenuItem> itms : menu_groups.values()) {
      itemcount += countItems(itms,done1);
    }
         
   MenuPanel pnl = new MenuPanel(itemcount);
   
   Set<MenuItem> done = new HashSet<>();
   int ct = 0;
   for (Set<MenuItem> itms : menu_groups.values()) {
      if (ct++ > 0) pnl.add(new JSeparator());
      addPopupItems(pt,itms,pnl,null,done);
    }

   Dimension d = pnl.getPreferredSize();
   BoardColors.setColors(pnl,BUDA_MENU_COLOR_PROP);
   pnl.setSize(d);
   // pnl.setVisible(true);

   return pnl;
}



private void addPopupItems(Point pt,Set<MenuItem> itms,JComponent menu,String pfx,Set<MenuItem> done)
{
   int pln = (pfx == null ? 0 : pfx.length());

   for (MenuItem mi : itms) {
      if (done.contains(mi)) continue;
      String nm = mi.getName();
      String tnm = nm.substring(pln);
      int idx = tnm.indexOf(".");
      if (idx < 0) {
	 MenuBtn btn = new MenuBtn(mi,pt);
	 menu.add(btn);
       }
      else {
	 String xnm = tnm.substring(0,idx);
	 String npfx = (pfx == null ? xnm : pfx + xnm) + ".";
	 MenuSubBtn m = new MenuSubBtn(xnm);
	 if (menu instanceof MenuPanel) {
	    MenuSubBar mb = new MenuSubBar(m);
	    menu.add(mb);
	  }
	 else menu.add(m);
	 Set<MenuItem> nitem = new TreeSet<MenuItem>();
	 for (MenuItem xmi : itms) {
	    if (xmi.getName().startsWith(npfx)) nitem.add(xmi);
	  }
	 addPopupItems(pt,nitem,m.getPopupMenu(),npfx,done);
       }
      done.add(mi);
    }
}



private int countItems(Set<MenuItem> itms,Set<Object> done)
{
   int count = 0;
   for (MenuItem mi : itms) {
      if (done.contains(mi)) continue;
      String tnm = mi.getName(); 
      int idx = tnm.indexOf(".");
      if (idx > 0) {
         String xnm = tnm.substring(0,idx);
         if (done.contains(xnm)) continue;
         done.add(xnm);
       }
      ++count;
      done.add(mi);
    }
   
   return count;
}



/********************************************************************************/
/*										*/
/*	View callback								*/
/*										*/
/********************************************************************************/

@Override public void bubbleRemoved(BudaBubble bb)
{
   for (MenuData md : active_menus) {
      if (md.searchUsed(bb) || md.menuUsed(bb)) break;
    }
}



/********************************************************************************/
/*										*/
/*	Class to hold a menu item						*/
/*										*/
/********************************************************************************/

private static class MenuItem implements Comparable<MenuItem> {

   private String full_id;
   private String item_name;
   private Icon menu_icon;
   private ButtonListener call_back;
   private String tool_tip;
   private String order_text;

   MenuItem(String id,String nm,ButtonListener cb, Icon ii,String tt,String order) {
      full_id = id;
      item_name = nm;
      call_back = cb;
      menu_icon = ii;
      tool_tip = tt;
      order_text = (order == null ? "" : order) + "#" + item_name;
    }

   String getId()				{ return full_id; }
   String getName()				{ return item_name; }
   Icon getIcon()				{ return menu_icon; }
   ButtonListener getCallback() 		{ return call_back; }
   String getToolTip()				{ return tool_tip; }

   @Override public int compareTo(MenuItem mi) {
      return order_text.compareTo(mi.order_text);
    }

}	// end of inner class MenuItem




/********************************************************************************/
/*										*/
/*	MenuPanel class 							*/
/*										*/
/********************************************************************************/

private static class MenuPanel extends SwingGridPanel implements FocusListener,
		BudaBubbleOutputer
{

   private Color fg_color;
   private Color fgt_color;
   private int row_count;
   private int col_count;
   private int total_items;
   private int item_count;
   
   private static final long serialVersionUID = 1;

   MenuPanel(int totitms) {
      fg_color = getForeground();
      fgt_color = BoardColors.transparent(fg_color);
      setInsets(1);
      setOpaque(false);
   
      addMouseListener(new FocusOnEntry());
      addFocusListener(this);
      setFocusable(true);
      row_count = 0;
      col_count = 0;
      item_count = 0;
      total_items = totitms;
    }

   @SuppressWarnings("unused")
   @Override public Component add(Component c) {
      if (c instanceof JSeparator) {
         if (item_count == 0 || MENU_COLUMNS > 1) return c;
         addGBComponent(c,col_count,row_count++,1,1,10,10);
       }
      else {
         addGBComponent(c,col_count,row_count++,1,1,10,10);
         ++item_count;
         if (item_count >= (total_items+MENU_COLUMNS-1)/MENU_COLUMNS) {
            item_count = 0;
            ++col_count;
            row_count = 0;
          }
       }
      return c;
    }


   void setTransparent() {
      for (Component c : getComponents()) {
         if (c instanceof MenuComponent) {
            MenuComponent b = (MenuComponent) c;
            b.setTransparent(fgt_color);
          }
         else if (c instanceof JSeparator) {
            colorSeparator((JSeparator) c,true);
         }
       }
    }

   void setNontransparent() {
      for (Component c : getComponents()) {
         if (c instanceof MenuComponent) {
            MenuComponent b = (MenuComponent) c;
            b.setNontransparent(fg_color);
          }
         else if (c instanceof JSeparator) {
            colorSeparator((JSeparator) c,false);
         }
       }
    }

   private void colorSeparator(JSeparator js,boolean transp) {  }
   
   @Override public void focusGained(FocusEvent e)	{ setNontransparent(); }
   @Override public void focusLost(FocusEvent e)	{ setTransparent(); }

   @Override public String getConfigurator()		{ return "BUDAMENU"; }
   @Override public void outputXml(BudaXmlWriter xw) {
      xw.field("TYPE","MENU");
    }

}	// end of inner class MenuPanel




private interface MenuComponent {

   void setTransparent(Color c);
   void setNontransparent(Color c);

}



private class MenuBtn extends JMenuItem implements MenuComponent, ActionListener {

   private Point start_point;
   private MenuItem for_item;

   private static final long serialVersionUID = 1;

   MenuBtn(MenuItem mi,Point pt) {
      String nm = mi.getName();
      int idx = nm.lastIndexOf(".");
      if (idx > 0) nm = nm.substring(idx+1);
      if (mi.getIcon() != null) setIcon(mi.getIcon());
      setText(nm);
      for_item = mi;
      start_point = pt;
      BoardColors.setColors(this,BUDA_MENU_BACKGROUND_COLOR_PROP);
      Font ft = BUDA_PROPERTIES.getFont(BUBBLE_MENU_FONT_NAME,BUBBLE_MENU_FONT);
      setFont(ft);
      ToolTipManager.sharedInstance().registerComponent(this); 
      // setContentAreaFilled(false);
      // enabling this cause the buttons to be green on the mac
      // setBorderPainted(false);
      setActionCommand(mi.getId());
      addActionListener(this);
    }


   @Override public void setTransparent(Color fg) {
      setSelected(false);
      setContentAreaFilled(false);
      setOpaque(false);
      setForeground(BoardColors.transparent(getForeground()));
    }

   @Override public void setNontransparent(Color fg) {
      setOpaque(true);
      setContentAreaFilled(true);
      BoardColors.setColors(this,BUDA_MENU_BACKGROUND_COLOR_PROP);
      // setForeground(fg);
    }

   @Override public void actionPerformed(ActionEvent evt) {
      Component c = (Component) evt.getSource();
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(c);
      ButtonListener bc = for_item.getCallback();
      noteMenuUsed(this);          // was after callback
      if (bc != null && bba != null) {
         BoardMetrics.noteCommand("BUDA","menu_" + for_item.getName());
         bc.buttonActivated(bba,for_item.getId(),start_point);
       }
    }

   @Override public String getToolTipText() {
      String tt = for_item.getToolTip();
      if (tt != null) return tt;
      return for_item.getId();
    }

}	// end of inner class MenuBtn



private static class MenuSubBtn extends JMenu implements MenuComponent {

   private static final long serialVersionUID = 1;


   MenuSubBtn(String nm) {
      super(nm);
      putClientProperty("JButton.buttonType","toolbar");
      Font ft = BUDA_PROPERTIES.getFont(BUBBLE_MENU_FONT_NAME,BUBBLE_MENU_FONT);
      setFont(ft);
      setOpaque(false);
    }

   @Override public void setTransparent(Color fg) {
      setOpaque(false);
      setForeground(fg);
    }

   @Override public void setNontransparent(Color fg) {
      setOpaque(true);
      setForeground(fg);
    }
   
   @Override public void setPopupMenuVisible(boolean b) {
      boolean isVisible = isPopupMenuVisible();
      if (b != isVisible) {
	 if (b && isShowing()) {
	    getPopupMenu().show(this, getWidth(), 0);
          }
         else {
	    getPopupMenu().setVisible(false);
          }
       }
    }
   
}	// end of inner class MenuSubBtn



private static final char ARROW = '\u25b6';
private static final int         MENU_COLUMNS = 2;



private static class MenuSubBar extends JMenuBar implements MenuComponent {

   private MenuSubBtn menu_btn;

   private static final long serialVersionUID = 1;

   MenuSubBar(MenuSubBtn btn) {
      menu_btn = btn;

      if (BUBBLE_MENU_FONT.canDisplay(ARROW)) {
	 menu_btn.setText(menu_btn.getText() + " " + ARROW);
       }
      else {
	 menu_btn.setText(menu_btn.getText() + " >");
       }

      menu_btn.setHorizontalTextPosition(SwingConstants.LEFT);
      setMargin(new Insets(0,0,0,0));
      setBorderPainted(false);
      setFont(BUBBLE_MENU_FONT);
      add(btn);
    }

   @Override public void setTransparent(Color fg) {
      setOpaque(false);
      menu_btn.setTransparent(fg);
    }

   @Override public void setNontransparent(Color fg) {
      menu_btn.setNontransparent(fg);
    }

}	// end of inner class MenuSubBar




/********************************************************************************/
/*										*/
/*	Menu Bubble class							*/
/*										*/
/********************************************************************************/

private static class MenuBubble extends BudaBubble {

   private static final long serialVersionUID = 1;

   MenuBubble(JComponent cmp) {
      super(cmp,BudaBorder.NONE);
      setOpaque(false);
      BoardColors.setColors(this,BUDA_MENU_COLOR_PROP);
      setTransient(true);
    }

}	// end of inner class MenuBubble



/********************************************************************************/
/*										*/
/*	MenuData : manage currently active menus and search boxes		*/
/*										*/
/********************************************************************************/

private class MenuData extends ComponentAdapter {

   private BudaBubble search_bubble;
   private BudaBubble menu_bubble;

   MenuData(BudaBubble sb,BudaBubble mb) {
      search_bubble = sb;
      menu_bubble = mb;
      search_bubble.addComponentListener(this);
      menu_bubble.addComponentListener(this);
    }

   boolean searchUsed(BudaBubble bb) {
      if (bb != search_bubble) return false;
   
      menu_bubble.setVisible(false);
      return true;
    }

   boolean menuUsed(BudaBubble bb) {
      if (bb != menu_bubble) return false;

      search_bubble.setVisible(false);
      return true;
    }

   @Override public void componentShown(ComponentEvent e) {
      if (e.getSource() == search_bubble) {
	 // doesn't work for some reason: wrong focus or invisible?
	 // need to give focus to text area
	 search_bubble.grabFocus();
       }
    }

   @Override public void componentHidden(ComponentEvent e) {
      if (e.getSource() == search_bubble) {
	 menu_bubble.setVisible(false);
       }
      else if (e.getSource() == menu_bubble) {
	 active_menus.remove(this);
       }
    }

}	// end of inner class MenuData



/********************************************************************************/
/*										*/
/*	Configurator for menu bubbles						*/
/*										*/
/********************************************************************************/

private final class MenuConfigurator implements BubbleConfigurator {

   @Override public BudaBubble createBubble(BudaBubbleArea bba,Element xml) {
      Element cnt = IvyXml.getChild(xml,"CONTENT");
      String typ = IvyXml.getAttrString(cnt,"TYPE");
   
      BudaBubble bb = null;
   
      if (typ.equals("MENU")) {
         Point p = new Point(IvyXml.getAttrInt(xml,"X"),IvyXml.getAttrInt(xml,"Y"));
         MenuPanel menu = createMenu(p,0);
         if (menu != null) {
            menu.setTransparent();
            bb = new MenuBubble(menu);
          }
       }
   
      return bb;
    }

   @Override public boolean matchBubble(BudaBubble bb,Element xml) {
      Element cnt = IvyXml.getChild(xml,"CONTENT");
      String typ = IvyXml.getAttrString(cnt,"TYPE");
      if (typ.equals("MENU") && bb.getContentPane() instanceof MenuPanel) return true;
      return false;
    }

   @Override public void outputXml(BudaXmlWriter xw,boolean history)	{ }
   @Override public void loadXml(BudaBubbleArea bba,Element root)	{ }


}	// end of inner class MenuConfigurator



}	// end of class BudaMenu




/* end of BudaMenu.java */
