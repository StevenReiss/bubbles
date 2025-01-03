/********************************************************************************/
/*										*/
/*		BeamFlagBubble.java						*/
/*										*/
/*	Encapsulates the flag bubble functionality				*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Andrew Bragdon		      */
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



package edu.brown.cs.bubbles.beam;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaCursorManager;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.buda.BudaXmlWriter;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.Hashtable;


class BeamFlagBubble extends BudaBubble implements BeamConstants,
		BudaConstants.BudaBubbleOutputer
{




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private JLabel flag_label = null;
private ImageIcon flag_icon = null;
private transient Image overview_image;
private int     flag_size;

private JLabel chevron_label = null;

private JPanel flag_panel = null;

private Hashtable<JMenuItem, String> context_menu_hash = null;

private static final String [] DEFAULT_ICONS = {"Fixed", "Flag", "Warning"};
private static final String [] ADDITIONAL_ICONS = {"Action", "Bomb", "Bug", "Clock",
						"Database", "Fish", "Idea", "Investigate",
						"Link", "Star"};

private String image_path = "";

private static final double OVERVIEW_SCALE_X = 0.03;
private static final long serialVersionUID = 1;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BeamFlagBubble(String imagePath)
{
   super(null,BudaBorder.NONE);
   
   BoardProperties bp = BoardProperties.getProperties("Beam");
   flag_size = bp.getInt("Beam.flag.size",128);
   
   image_path = imagePath;

   this.setResizable(false);

   // Setup image label

   Image img = BoardImage.getImage(imagePath);
   overview_image = img;
   flag_icon = new ImageIcon(img);	//new ImageIcon(imagePath);
   flag_label = new JLabel(flag_icon);
   flag_label.setSize(flag_size, flag_size);

   flag_panel = new JPanel();
   flag_panel.setSize(flag_size, flag_size);
   BudaCursorManager.setCursor(flag_panel,Cursor.getDefaultCursor());

   flag_panel.setLayout(null);

   flag_panel.setBackground(BoardColors.transparent());
   flag_panel.setBorder(null);
   flag_label.setBorder(null);

   setBorderColor(BoardColors.transparent(), BoardColors.transparent());

   // Setup chevron label

   chevron_label = new JLabel(new ImageIcon(BoardImage.getImage("dropdown_chevron.png")));
   chevron_label.setSize(16, 16);
   flag_panel.add(chevron_label);
   chevron_label.setVisible(false);
   chevron_label.setLocation(flag_size - 17, flag_size - 17);

   // Add flag label

   flag_panel.add(flag_label);

   // Setup events

   flag_panel.addMouseListener(new FlagMouseEvents());
   chevron_label.addMouseListener(new ChevronMouseEvents());

   setContentPane(flag_panel);

   setShouldFreeze(false);
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public String getConfigurator()	  { return "BEAM"; }

@Override public void outputXml(BudaXmlWriter xw) {
   xw.field("TYPE","FLAG");
   xw.cdataElement("IMGPATH", image_path);
}


String getImagePath()			{ return image_path; }


/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

private void changeIcon(String iconPath)
{
   Image img = BoardImage.getImage(iconPath);
   flag_icon = new ImageIcon(img);	//new ImageIcon(imagePath);
   overview_image = flag_icon.getImage();
   flag_label.setIcon(flag_icon);

   image_path = iconPath;

   BudaRoot br = BudaRoot.findBudaRoot(this);
   if (br != null) br.repaint();
}



/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

protected void paintContentOverview(Graphics2D g)
{
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);

   if (overview_image == flag_icon.getImage() && bba != null) {
      overview_image = overview_image.getScaledInstance(bba.getHeight()/6,bba.getHeight()/6, Image.SCALE_SMOOTH);
    }

   AffineTransform gtrans = g.getTransform();

   AffineTransform sc = AffineTransform.getScaleInstance(OVERVIEW_SCALE_X/gtrans.getScaleX(), 1);
   AffineTransform tr = AffineTransform.getTranslateInstance(-getSize().width/1.5, -getSize().height/1.5);
   sc.concatenate(tr);
   g.drawImage(overview_image, sc, null);
}



/********************************************************************************/
/*										*/
/*	Mouse handling								*/
/*										*/
/********************************************************************************/

private final class ChevronMouseEvents extends MouseAdapter
{

   @Override public void mouseClicked(MouseEvent arg0) {
   
      JPopupMenu menu = new JPopupMenu();
      context_menu_hash = new Hashtable<JMenuItem, String>();
   
      for (int i = 0; i < DEFAULT_ICONS.length; i++) {
         JMenuItem item = menu.add(DEFAULT_ICONS[i]);
   
         String path = "flags/default/" + DEFAULT_ICONS[i] + ".png";
   
         Icon icon = BoardImage.getIcon(path,22,22);
         item.setIcon(icon);
   
         item.addActionListener(new FlagContextMenuEvents());
   
         context_menu_hash.put(item, path);
       }
   
      menu.addSeparator();
   
      for (int i = 0; i < ADDITIONAL_ICONS.length; i++) {
         JMenuItem item = menu.add(ADDITIONAL_ICONS[i]);
   
         String path = "flags/additional/" + ADDITIONAL_ICONS[i] + ".png";
   
         Icon icon = BoardImage.getIcon(path,22,22);
         item.setIcon(icon);
   
         item.addActionListener(new FlagContextMenuEvents());
   
         context_menu_hash.put(item, path);
       }
   
      menu.show(chevron_label, 0, 0);
    }

}	// end of inner calss ChevronMouseEvents



private final class FlagContextMenuEvents implements ActionListener
{

   @Override public void actionPerformed(ActionEvent arg0) {
      JMenuItem item = (JMenuItem) arg0.getSource();
      if (context_menu_hash.containsKey(item)) {
	 changeIcon(context_menu_hash.get(item));
       }
    }

}	// end of inner class FlagCOntextMenuEvents



private final class FlagMouseEvents extends MouseAdapter
{

   @Override public void mouseEntered(MouseEvent arg0) {
      chevron_label.setVisible(true);

      repaint();
    }

   @Override public void mouseExited(MouseEvent arg0) {
      if ((0 <= arg0.getX()) && (arg0.getX() < flag_size) &&
            (0 <= arg0.getY()) && (arg0.getY() < flag_size)) {
         // do nothing
       }
      else {
	 chevron_label.setVisible(false);
	 repaint();
       }
    }

}	// end of inner class FlagMouseEvents



}	// end of class BeamFlagBubble



/* end of BeamFlagBubble.java */
