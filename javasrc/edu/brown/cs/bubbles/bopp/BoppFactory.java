/********************************************************************************/
/*										*/
/*		BoppFactory.java						*/
/*										*/
/*	Factory for setting up the option panel 				*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Alexander Hills		      */
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




package edu.brown.cs.bubbles.bopp;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaConstraint;
import edu.brown.cs.bubbles.buda.BudaRoot;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Factory for options panel
 **/

public class BoppFactory implements BoppConstants, BudaConstants {


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private static BoppFactory the_factory = new BoppFactory();
private static BudaRoot    buda_root = null;
private static BoppOptionSet option_set = null;
private static Map<BudaBubbleArea,BoppOptionPanel> options_panel = new HashMap<BudaBubbleArea,BoppOptionPanel>();
private static List<BoppOptionNew> changed_options = new ArrayList<BoppOptionNew>();



/**
 * returns the factory
 *
 */

public static BoppFactory getFactory()
{
   return the_factory;
}



/********************************************************************************/
/*										*/
/*	Initialization methods							*/
/*										*/
/********************************************************************************/

/**
 * setup method (currently does nothing)
 */

public static void setup()
{
   // do nothing
}


/**
 *	Setup method called after buda is setup
 **/

public static void initialize(BudaRoot br)
{
   buda_root = br;

   Icon chevron = BoardImage.getIcon("dropdown_chevron",
					BUDA_BUTTON_RESIZE_WIDTH, BUDA_BUTTON_RESIZE_HEIGHT);

   option_set = new BoppOptionSet(br);

   JButton btn1 = new JButton("Options",chevron);
   btn1.setIconTextGap(0);
   btn1.setMargin(BOPP_BUTTON_INSETS);
   Font ft = btn1.getFont();
   ft = ft.deriveFont(10f);
   btn1.setHorizontalTextPosition(SwingConstants.LEADING);
   btn1.setFont(ft);
   btn1.setOpaque(false);
   btn1.setBackground(BoardColors.transparent());
   btn1.setToolTipText("Options for Code Bubbles");

   btn1.addActionListener(new OptionsListenerNew(br));

   br.addButtonPanelButton(btn1);
}



/********************************************************************************/
/*										*/
/*	Panel setup methods							*/
/*										*/
/********************************************************************************/

static void repaintBubbleArea()
{
   buda_root.getCurrentBubbleArea().repaint();
}

/**
 * Returns a new options panel
 */

public static BoppOptionPanel getBoppPanelNew(BudaBubbleArea area)
{
   BoppOptionPanel pnl =  new BoppOptionPanel(option_set);
   List<BoppOptionNew> opts = null;

   synchronized (changed_options) {
      opts = new ArrayList<>(changed_options);
    }

   for (BoppOptionNew opt : opts) {
      pnl.handleOptionChange(opt);
    }

   return pnl;
}


static void handleOptionChange(BoppOptionNew opt)
{
   synchronized (changed_options) {
      changed_options.add(opt);
    }

   for (BoppOptionPanel pnl : options_panel.values()) {
      pnl.handleOptionChange(opt);
    }
   
   BoardMetrics.noteCommand("ChangeOption_" + opt.getOptionName());
}

/********************************************************************************/
/*										*/
/*	Handler for options button						*/
/*										*/
/********************************************************************************/

private static class OptionsListenerNew implements ActionListener {

   OptionsListenerNew(BudaRoot br) {
    }

   @Override public void actionPerformed(ActionEvent evt) {
      BudaBubbleArea bba = buda_root.getCurrentBubbleArea();
      if (bba == null) return;
   
      BoppOptionPanel pnl = options_panel.get(bba);
      if (pnl == null) {
         pnl = BoppFactory.getBoppPanelNew(bba);
         options_panel.put(bba,pnl);
       }
      else if (pnl.getPanel().getParent() != null &&
            pnl.getPanel().getParent().isVisible()) {
         pnl.getPanel().getParent().setVisible(false);
         return;
       }
      
      BoardMetrics.noteCommand("ShowOptions");
   
      JPanel jp = pnl.getPanel();
      Rectangle r = bba.getViewport();
      if (r.width == 0) return;
      Dimension d = jp.getPreferredSize();
      BudaConstraint bc = new BudaConstraint(BudaConstants.BudaBubblePosition.STATIC,r.x  + r.width - d.width,r.y);
      jp.setSize(d);
      BudaBubble xbb = pnl.getBubble();
      bba.add(xbb, bc, 0);
      jp.setVisible(true);
    }

}	// end of inner class OptionsListener


}	// end of class BoppFactory



/* end of BoppFactory.java */
