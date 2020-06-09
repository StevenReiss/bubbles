/********************************************************************************/
/*										*/
/*		BudaToolbar.java						*/
/*										*/
/*	BUblles Display Area tool bar display					*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Alex Hills			      */
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

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class BudaToolbar implements BudaConstants {


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private static List<MenuButton> the_buttons = new ArrayList<MenuButton>();
private static Map<BudaBubbleArea, Toolbar> menu_map = new HashMap<BudaBubbleArea, Toolbar>();

private static final int TOOLBAR_Y_DELTA = 5;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

/**
 * Adds a button to all menu bars (current and future)
 */
static void addToolbarButton(String name, ActionListener l, String tooltip, Image i)
{
   the_buttons.add(new MenuButton(name, l, tooltip, i));
   addButton(name,l,tooltip,i);
}


/********************************************************************************/
/*										*/
/*	Addition methods							*/
/*										*/
/********************************************************************************/

/**
 * Adds a button to the current menu bar
 */

private static void addButton(String name, ActionListener l, String tooltip, Image i)
{
   for (Toolbar m : menu_map.values()) {
      m.addButton(name, l, tooltip, i);
   }
}


static Toolbar getToolbar(BudaBubbleArea bba)
{
   Toolbar b = menu_map.get(bba);

   if (b == null) {
      b = new Toolbar();
      menu_map.put(bba, b);
      b.setVisible(false);
    }

   return b;
}




/********************************************************************************/
/*										*/
/*	Nested classes for display						*/
/*										*/
/********************************************************************************/

private static class Toolbar extends BudaBubble implements NoFreeze
{
   private List<MenuPanel> menu_panels;
   private JPanel main_panel;

   private static final long serialVersionUID = 1;

   Toolbar() {
      super(BudaBorder.NONE);
      setTransient(true);
      setFloating(true);
      setResizable(false);
      setOpaque(false);
      menu_panels = new ArrayList<MenuPanel>();
      main_panel = new JPanel();
      main_panel.setOpaque(false);
      setContentPane(main_panel);
      setOpaque(false);
      for (MenuButton b : the_buttons) {
	 addButton(b.getName(),b.getListener(),b.getTooltip(),b.getImage());
       }
    }

   void addButton(String name, ActionListener l, String tooltip, Image i) {
      JButton b = new JButton();

      if (i != null) {
	 BufferedImage bi = new BufferedImage(BUDA_MENU_BUTTON_ICON_WIDTH, BUDA_MENU_BUTTON_ICON_HEIGHT, BufferedImage.TYPE_INT_ARGB);
	 Graphics2D g2 = bi.createGraphics();
	 g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	 g2.drawImage(i, 0, 0, bi.getWidth(), bi.getHeight(),null);
	 g2.dispose();
	 ImageIcon icon = new ImageIcon(bi);
	 b.setIcon(icon);
       }

      if (tooltip != null) b.setToolTipText(tooltip);

      b.addActionListener(l);
      b.setOpaque(false);
      b.setMargin(new Insets(0,0,0,0));
      b.setBackground(BoardColors.getColor("Buda.ToolbarBackground"));
      b.setBorder(null);
      b.setFocusPainted(false);

      MenuPanel panel=null;
      for (MenuPanel p : menu_panels) {
	 if (name.equals(p.getName())) panel = p;
       }
      if (panel==null) {
	 panel = new MenuPanel(name);
	 main_panel.add(panel);
	 menu_panels.add(panel);
       }
      panel.addButton(b);
      panel.revalidate();
      panel.repaint();
    }


   @Override public void paintComponent(Graphics g) {
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
      Rectangle r;
      if (bba != null) {
	 r = bba.getViewport();
	 if (getSize().getWidth()>r.width) {
	    setSize(new Dimension(r.width,getSize().height));
	  }
       }
      super.paintComponent(g);
    }

}	// end of inner class Toolbar




private static class MenuPanel extends JPanel {

   private String menu_name;

   private static final long serialVersionUID = 1;

   MenuPanel(String name) {
      menu_name = name;
      setLayout(new GridBagLayout());
      setBackground(BoardColors.getColor("Buda.ToolbarMenuBackground"));
    }

   @Override public String getName()		{ return menu_name; }

   private void addButton(JButton b) {
      GridBagConstraints c;
      int i = 0;		// TODO: compute row here based on count
      c = new GridBagConstraints(GridBagConstraints.RELATIVE,i,
				    1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
				    new Insets(3, 3, 3, 3), 2, 2);
      add(b,c);
    }

}	// end of inner class MenuPanel



/**
 * Gets the action that shows a menu bar
 */

static Action getMenuBarAction(BudaRoot r)
{
   return new MenuListener(r);
}




/********************************************************************************/
/*										*/
/*	Handler for menu hotkey 						*/
/*										*/
/********************************************************************************/

private static class MenuListener extends AbstractAction implements ActionListener {

   private BudaRoot for_root;
   private static final long serialVersionUID = 1;

   MenuListener(BudaRoot r) {
      super("Toggle Menu Bar");
      for_root = r;
   }

   @Override public void actionPerformed(ActionEvent arg0) {
      BudaBubbleArea bba = for_root.getCurrentBubbleArea();
      if (bba == null) return;
   
      Toolbar pnl = getToolbar(bba);
      if (pnl.isVisible()) {
         pnl.setVisible(false);
         return;
      }
      Rectangle r = for_root.getShadedViewport();
      if (r == null) return;
      
      BudaConstraint bc = new BudaConstraint(BudaConstants.BudaBubblePosition.STATIC,
        	  r.x, r.y + TOOLBAR_Y_DELTA);
   
      Rectangle pnlrect = new Rectangle(r.x, r.y, pnl.getPreferredSize().width, pnl.getPreferredSize().height);
      Collection<BudaBubble> bubbles = bba.getBubblesInRegion(pnlrect);
      if (bubbles.contains(for_root.getPackageExplorer(bba)))
         bc = new BudaConstraint(BudaConstants.BudaBubblePosition.STATIC,
        	  r.x + r.width - pnl.getPreferredSize().width, r.y + TOOLBAR_Y_DELTA);
   
   
      pnl.revalidate();
      bba.add(pnl,bc,0);
       // bba.setLayer(pnl, JLayeredPane.DRAG_LAYER+2); // this causes it to not be placed correctly on shade down
      pnl.setVisible(true);
      if (pnl.getContentPane() != null) {
         Dimension d = pnl.getContentPane().getPreferredSize();
         if (d != null) pnl.setSize(new Dimension(d.width+3,d.height+8));
       }
      pnl.revalidate();
      pnl.repaint();
   }

}	// end of inner class MenuListener



/********************************************************************************/
/*										*/
/*	Information holder for mentu buttons					*/
/*										*/
/********************************************************************************/

private static class MenuButton
{
   private String the_name;
   private ActionListener the_listener;
   private String the_tooltip;
   private Image the_image;

   MenuButton(String n, ActionListener list, String tt, Image img) {
      the_name = n;
      the_listener = list;
      the_tooltip = tt;
      the_image = img;
    }

   String getName() {
      return the_name;
    }

   ActionListener getListener() {
      return the_listener;
    }

   String getTooltip() {
      return the_tooltip;
    }

   Image getImage() {
      return the_image;
    }

}	// end	of inner class MenuButton



}	// end of class BudaToolbar




/* end of BudaToolbar.java*/
