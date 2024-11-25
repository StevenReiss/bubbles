/********************************************************************************/
/*										*/
/*		BussCellRenderer.java						*/
/*										*/
/*	BUbble Stack Strategies bubble stack cell display			*/
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


package edu.brown.cs.bubbles.buss;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaConstants;

import edu.brown.cs.ivy.swing.SwingRoundedCornerLabel;

import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;

class BussCellRenderer implements BussConstants, BudaConstants, TreeCellRenderer {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private int content_width;

private BussBubble buss_bubble;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BussCellRenderer(int contentwidth, BussBubble bussbubble)
{
   content_width = contentwidth;
   buss_bubble = bussbubble;
}




/********************************************************************************/
/*										*/
/*	Return component for the cell						*/
/*										*/
/********************************************************************************/

@Override public Component getTreeCellRendererComponent(JTree t,Object val,
							   boolean sel,boolean exp,
							   boolean leaf,int row,boolean focus)
{
   if (!leaf) {
      if (sel) {
	 if (buss_bubble.getEditorBubble() != null) {
	    buss_bubble.getLayeredPane().remove(buss_bubble.getEditorBubble());
	  }
	 buss_bubble.setEditorBubble(null);
       }
      return simpleComponent(t,val,sel,exp,leaf,row,focus);
    }

   BussTreeNode tn = (BussTreeNode) val;
   BussEntry ent = tn.getEntry();

   if (!sel) {
      Component c = ent.getCompactComponent();
      return c;
    }

   BudaBubble editorbubble = ent.getBubble();
   if (editorbubble == null) {
      return simpleComponent(t,val,sel,exp,leaf,row,focus);
    }

   editorbubble.setPreferredSize(editorbubble.getSize());
   editorbubble.setVisible(true);

   int diffY = 0;

   JPanel panel = new JPanel();
   panel.setPreferredSize(editorbubble.getSize());

   if (buss_bubble.getEditorBubble() == editorbubble){
      return panel;
    }

   Rectangle selectedItemRect = t.getRowBounds(row);

   if (buss_bubble.getEditorBubble() != null) {
      buss_bubble.getLayeredPane().remove(buss_bubble.getEditorBubble());

      if (buss_bubble.getEditorBubble().getLocation().y < selectedItemRect.y) {
	 Component c = buss_bubble.getSelectedEntry().getCompactComponent();
	 int h1 = 12;
	 if (c != null) h1 = c.getPreferredSize().height;
	 diffY = -buss_bubble.getEditorBubble().getPreferredSize().height + h1;
      }
    }

   buss_bubble.setEditorBubble(editorbubble);
   buss_bubble.setSelectedEntry(ent);

   editorbubble.setLocation(selectedItemRect.getLocation());

   buss_bubble.getLayeredPane().add(editorbubble, Integer.valueOf(1), 0);
   Dimension dim = (Dimension) buss_bubble.getStackBoxDim().clone();
   int h1 = 12;
   Component c = ent.getCompactComponent();
   if (c != null) h1 = c.getPreferredSize().height;
   dim.height = dim.height + editorbubble.getPreferredSize().height - h1;

   buss_bubble.getLayeredPane().setPreferredSize(dim);
   buss_bubble.setStackBoxSize(dim);

   buss_bubble.setViewportLocation(diffY);

   return panel;
}



/********************************************************************************/
/*										*/
/*	Handle simple components						*/
/*										*/
/********************************************************************************/

private Component simpleComponent(JTree t,Object val,boolean sel,boolean exp,
				boolean leaf,int row,boolean focus)
{
   String s = t.convertValueToText(val,sel,exp,leaf,row,focus);

   int[] aryseparateloc = new int[0];

   if (s.lastIndexOf(".") > 0) {
      String packagename = " " + s.substring(0, s.lastIndexOf("."));
      String classname = " " + s.substring(s.lastIndexOf(".") + 1);

      SwingRoundedCornerLabel packagelabel = new SwingRoundedCornerLabel(aryseparateloc, new int[0], 1);
      packagelabel.setFont(t.getFont());
      packagelabel.setForeground(BoardColors.getColor(BUSS_PACKAGE_LABEL_COLOR_PROP));

      packagelabel.setSize(content_width, 21);
      packagelabel.setPreferredSize(packagelabel.getSize());
      packagelabel.setText(packagename);

      SwingRoundedCornerLabel classnamelabel = new SwingRoundedCornerLabel(aryseparateloc, new int[0], 1);
      classnamelabel.setFont(t.getFont());
      classnamelabel.setForeground(BoardColors.getColor(BUSS_CLASS_LABEL_COLOR_PROP));

      classnamelabel.setSize(content_width, 21);
      classnamelabel.setPreferredSize(classnamelabel.getSize());
      classnamelabel.setText(classname);

      JPanel panel = new JPanel();

      panel.setPreferredSize(new Dimension(content_width, 42));

      panel.setLayout(null);
      packagelabel.setLocation(0, 0);
      classnamelabel.setLocation(0, 21);

      panel.add(packagelabel);
      panel.add(classnamelabel);

      return panel;
    }

   SwingRoundedCornerLabel roundedcornerlabel = new SwingRoundedCornerLabel(aryseparateloc, new int[0], 1);
   roundedcornerlabel.setFont(t.getFont());

   roundedcornerlabel.setSize(content_width, 21);
   roundedcornerlabel.setPreferredSize(roundedcornerlabel.getSize());
   roundedcornerlabel.setText(" " + s);

   return roundedcornerlabel;
}



}	// end of class BussCellRenderer




/* end of BussCellRenderer.java */
