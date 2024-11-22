/********************************************************************************/
/*										*/
/*		BdocCellRenderer.java						*/
/*										*/
/*	Bubble Environment Documentation cell display				*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bdoc;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.ivy.swing.SwingEditorPane;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumMap;
import java.util.Map;



class BdocCellRenderer extends DefaultTreeCellRenderer implements BdocConstants, TreeCellRenderer {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private static final long serialVersionUID = 1;

private BdocPanel for_panel;
private int	  tree_width;

private boolean is_selected;
private boolean has_focus;
private JComponent	item_component;
private JEditorPane	name_component;
private JEditorPane	desc_component;
private Icon		open_icon;
private Icon		closed_icon;


private Color	select_color = UIManager.getColor("Tree.selectionForeground");
private Color	plain_color = UIManager.getColor("Tree.textForeground");
private Color	select_back = UIManager.getColor("Tree.selectionBackground");
private Color	plain_back = UIManager.getColor("Tree.textBackground");
private Color	border_select = UIManager.getColor("Tree.selectionBorderColor");



private static final Map<ItemRelation,String> RELATION_NAMES;

static {
   RELATION_NAMES = new EnumMap<ItemRelation,String>(ItemRelation.class);
   RELATION_NAMES.put(ItemRelation.PACKAGE_EXCEPTION,"Exceptions ");
   RELATION_NAMES.put(ItemRelation.PACKAGE_ENUM,"Enumerations ");
   RELATION_NAMES.put(ItemRelation.PACKAGE_CLASS,"Classes ");
   RELATION_NAMES.put(ItemRelation.PACKAGE_INTERFACE,"Interfaces ");
   RELATION_NAMES.put(ItemRelation.PACKAGE_ERROR,"Errors ");
   RELATION_NAMES.put(ItemRelation.SUPERTYPE,"Extends ");
   RELATION_NAMES.put(ItemRelation.IMPLEMENTS,"Implements ");
   RELATION_NAMES.put(ItemRelation.NESTED_CLASS,"Nested Classes ");
   RELATION_NAMES.put(ItemRelation.FIELD,"Fields ");
   RELATION_NAMES.put(ItemRelation.CONSTRUCTOR,"Constructors ");
   RELATION_NAMES.put(ItemRelation.METHOD,"Methods ");
   RELATION_NAMES.put(ItemRelation.INHERITED_CLASS,"Inherited Nested Classes ");
   RELATION_NAMES.put(ItemRelation.INHERITED_CLASS,"Inherited Nested Classes ");
   RELATION_NAMES.put(ItemRelation.INHERITED_FIELD,"Inherited Fields ");
   RELATION_NAMES.put(ItemRelation.INHERITED_METHOD,"Inherited Methods ");
   RELATION_NAMES.put(ItemRelation.PARAMETER,"Parameters ");
   RELATION_NAMES.put(ItemRelation.RETURN,"Returns ");
   RELATION_NAMES.put(ItemRelation.THROW,"Throws ");
   RELATION_NAMES.put(ItemRelation.OVERRIDE,"Overrides ");
   RELATION_NAMES.put(ItemRelation.SEE_ALSO,"See Also ");
   RELATION_NAMES.put(ItemRelation.SUBINTERFACES,"Subinterfaces ");
   RELATION_NAMES.put(ItemRelation.IMPLEMENTING_CLASS,"Implementing Classes ");
   RELATION_NAMES.put(ItemRelation.SUBCLASS,"Known Subclasses ");
}






/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BdocCellRenderer(BdocPanel pnl)
{
   for_panel = pnl;

   DefaultTreeCellRenderer dtcr = new DefaultTreeCellRenderer();
   open_icon = dtcr.getDefaultOpenIcon();
   closed_icon = dtcr.getDefaultClosedIcon();
   open_icon = BoardImage.getIcon("docexpand");
   closed_icon = BoardImage.getIcon("doccollapse");

   tree_width = DESCRIPTION_WIDTH;

   setFont(OPTION_FONT);
   setOpaque(false);

   setBackgroundNonSelectionColor(null);
   setBackgroundSelectionColor(null);
   setBackground(BoardColors.transparent());

   setupSubitemDisplay();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void setTreeWidth(int w)			{ tree_width = w; }




/********************************************************************************/
/*										*/
/*	Return component for the cell						*/
/*										*/
/********************************************************************************/

@Override public Component getTreeCellRendererComponent(JTree t,Object val,boolean sel,boolean exp,
							   boolean leaf,int row,boolean focus)
{
   if (val instanceof DefaultMutableTreeNode) {
      val = ((DefaultMutableTreeNode) val).getUserObject();
    }

   if (!leaf) {
      if (val instanceof ItemRelation && RELATION_NAMES.get(val) != null) {
	 val = RELATION_NAMES.get(val);
       }
      return treeComponent(t,val,sel,exp,leaf,row,focus);
    }
   
   if (val instanceof SubItem) {
      SubItem si = (SubItem) val;
      if (si.getDescription() != null && si.getDescription().length() > 0) {
         name_component.setText(si.getName());
         desc_component.setText(si.getDescription());
         return item_component;
       }
    }
   
   return simpleComponent(t,val,sel,exp,leaf,row,focus);
}




/********************************************************************************/
/*										*/
/*	Handle subitem display							*/
/*										*/
/********************************************************************************/

private void setupSubitemDisplay()
{
   item_component = new JPanel(new BorderLayout());
   item_component.setDoubleBuffered(false);
   item_component.setOpaque(false);

   name_component = new ItemNamePane();
   name_component.addHyperlinkListener(new DocLinker());
   desc_component = new ItemDescPane();
   desc_component.addHyperlinkListener(new DocLinker());

   JLabel indent = new JLabel("    ");

   item_component.add(name_component,BorderLayout.NORTH);
   item_component.add(indent,BorderLayout.WEST);
   item_component.add(desc_component,BorderLayout.CENTER);
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

   Color fg;
   if (sel) fg = select_color;
   else fg = plain_color;
   setForeground(fg);

   is_selected = sel;
   has_focus = focus;
   setText(s);

   return this;
}



/********************************************************************************/
/*										*/
/*	Handle internal components						*/
/*										*/
/********************************************************************************/

private Component treeComponent(JTree t,Object val,boolean sel,boolean exp,
				   boolean leaf,int row,boolean focus)
{
   String s = t.convertValueToText(val,sel,exp,leaf,row,focus);

   Color fg;
   if (sel) fg = select_color;
   else fg = plain_color;
   setForeground(fg);

   is_selected = sel;
   has_focus = focus;
   setText(s);

   if (exp) setIcon(closed_icon);
   else setIcon(open_icon);

   return this;
}



/********************************************************************************/
/*										*/
/*	Handle painting 							*/
/*										*/
/********************************************************************************/

@Override public void paint(Graphics g)
{
   Color bg;

   if (is_selected) bg = select_back;
   else {
      bg = plain_back;
      if (bg == null) bg = getBackground();
    }

   if (has_focus) {
      paintFocus(g,0,0,getWidth(),getHeight(),bg);
    }

   super.paint(g);
}



private void paintFocus(Graphics g, int x, int y, int w, int h, Color notcolor)
{
   Color bs = border_select;

   if (bs != null && is_selected) {
      g.setColor(bs);
      g.drawRect(x, y, w - 1, h - 1);
    }
}




/********************************************************************************/
/*										*/
/*	Hyperlink manager							*/
/*										*/
/********************************************************************************/

private final class DocLinker implements HyperlinkListener {

   @Override public void hyperlinkUpdate(HyperlinkEvent e) {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
         try {
            URI u = e.getURL().toURI();
            String lbl = e.getDescription();
            for_panel.createLinkBubble(lbl,u);
         }
         catch (URISyntaxException ex) { }
       }
    }

}	// end of inner class DocLinker




/********************************************************************************/
/*										*/
/*	ItemNamePane -- handle item name					*/
/*										*/
/********************************************************************************/

private class ItemNamePane extends JEditorPane {

   private static final long serialVersionUID = 1;


   ItemNamePane() {
      super("text/html","");
      setEditable(false);
      setFont(ITEM_DESC_FONT);
      setForeground(BoardColors.getColor("Bdoc.ItemForeground")); 
      putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES,Boolean.TRUE);
      setOpaque(false);
    }

   @Override public Dimension getPreferredSize() {
      return BdocPanel.computeEditorSize(this,tree_width - 24);
    }

}	// end of inner class ItemNamePane




/********************************************************************************/
/*										*/
/*	ItemDescPane -- handle item descrition					*/
/*										*/
/********************************************************************************/

private class ItemDescPane extends SwingEditorPane {

   private static final long serialVersionUID = 1;

   ItemDescPane() {
      super("text/html","");
      setEditable(false);
      setFont(ITEM_NAME_FONT);
      putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES,Boolean.TRUE);
      setOpaque(false);
    }

   @Override public Dimension getPreferredSize() {
      return BdocPanel.computeEditorSize(this,tree_width - 48);
    }

   @Override public void repaint()					{ }
   @Override public void repaint(long t,int x,int y,int w,int h)	{ }
   @Override public void repaint(Rectangle r)				{ }

}	// end of inner class ItemDescPane




}	// end of class BdocCellRenderer




/* end of BdocCellRenderer.java */

