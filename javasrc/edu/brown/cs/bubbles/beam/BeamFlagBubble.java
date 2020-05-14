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

private JLabel _FlagLabel = null;
private ImageIcon _Icon = null;
private Image _OverviewImage;
private int     flag_size;

private JLabel _ChevronLabel = null;

private JPanel _Panel = null;

private Hashtable<JMenuItem, String> _ContextMenuHash = null;

private static String [] _DefaultIcons = {"Fixed", "Flag", "Warning"};
private static String [] _AdditionalIcons = {"Action", "Bomb", "Bug", "Clock",
						"Database", "Fish", "Idea", "Investigate",
						"Link", "Star"};

private String _ImagePath = "";

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
   
   _ImagePath = imagePath;

   this.setResizable(false);

   // Setup image label

   Image img = BoardImage.getImage(imagePath);
   _OverviewImage = img;
   _Icon = new ImageIcon(img);	//new ImageIcon(imagePath);
   _FlagLabel = new JLabel(_Icon);
   _FlagLabel.setSize(flag_size, flag_size);

   _Panel = new JPanel();
   _Panel.setSize(flag_size, flag_size);
   BudaCursorManager.setCursor(_Panel,Cursor.getDefaultCursor());

   _Panel.setLayout(null);

   _Panel.setBackground(BoardColors.transparent());
   _Panel.setBorder(null);
   _FlagLabel.setBorder(null);

   setBorderColor(BoardColors.transparent(), BoardColors.transparent());

   // Setup chevron label

   _ChevronLabel = new JLabel(new ImageIcon(BoardImage.getImage("dropdown_chevron.png")));
   _ChevronLabel.setSize(16, 16);
   _Panel.add(_ChevronLabel);
   _ChevronLabel.setVisible(false);
   _ChevronLabel.setLocation(flag_size - 17, flag_size - 17);

   // Add flag label

   _Panel.add(_FlagLabel);

   // Setup events

   _Panel.addMouseListener(new FlagMouseEvents());
   _ChevronLabel.addMouseListener(new ChevronMouseEvents());

   setContentPane(_Panel);

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
   xw.cdataElement("IMGPATH", _ImagePath);
}


String getImagePath()			{ return _ImagePath; }


/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

private void changeIcon(String iconPath)
{
   Image img = BoardImage.getImage(iconPath);
   _Icon = new ImageIcon(img);	//new ImageIcon(imagePath);
   _OverviewImage = _Icon.getImage();
   _FlagLabel.setIcon(_Icon);

   _ImagePath = iconPath;

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

   if (_OverviewImage == _Icon.getImage() && bba != null) {
      _OverviewImage = _OverviewImage.getScaledInstance(bba.getHeight()/6,bba.getHeight()/6, Image.SCALE_SMOOTH);
    }

   AffineTransform gtrans = g.getTransform();

   AffineTransform sc = AffineTransform.getScaleInstance(OVERVIEW_SCALE_X/gtrans.getScaleX(), 1);
   AffineTransform tr = AffineTransform.getTranslateInstance(-getSize().width/1.5, -getSize().height/1.5);
   sc.concatenate(tr);
   g.drawImage(_OverviewImage, sc, null);


   /***************
   at.scale(3,3);		// make it large enough to see in the overview
   Dimension d = getSize();
   at.translate(-d.width/3,-d.height/3);
   /****************
   AffineTransform affine = g.getTransform();
   AffineTransform a = g.getTransform();
   //at = a;
   sc.setToScale(OVERVIEW_SCALE_X/a.getScaleX(), 1);
   //at = new AffineTransform(OVERVIEW_SCALE_X, affine.getShearY(), affine.getShearX(), affine.getScaleY(), affine.getTranslateX(), affine.getTranslateY());
   //g.setTransform(a);
   g.drawImage(_OverviewImage,sc,null);
   g.setTransform(affine);*/
}



/********************************************************************************/
/*										*/
/*	Mouse handling								*/
/*										*/
/********************************************************************************/

private class ChevronMouseEvents extends MouseAdapter
{

   @Override public void mouseClicked(MouseEvent arg0) {

      JPopupMenu menu = new JPopupMenu();
      _ContextMenuHash = new Hashtable<JMenuItem, String>();

      for (int i = 0; i < _DefaultIcons.length; i++) {
	 JMenuItem item = menu.add(_DefaultIcons[i]);

	 String path = "flags/default/" + _DefaultIcons[i] + ".png";

	 Icon icon = BoardImage.getIcon(path,22,22);
	 item.setIcon(icon);

	 item.addActionListener(new FlagContextMenuEvents());

	 _ContextMenuHash.put(item, path);
       }

      menu.addSeparator();

      for (int i = 0; i < _AdditionalIcons.length; i++) {
	 JMenuItem item = menu.add(_AdditionalIcons[i]);

	 String path = "flags/additional/" + _AdditionalIcons[i] + ".png";

	 Icon icon = BoardImage.getIcon(path,22,22);
	 item.setIcon(icon);

	 item.addActionListener(new FlagContextMenuEvents());

	 _ContextMenuHash.put(item, path);
       }

      menu.show(_ChevronLabel, 0, 0);
    }

}	// end of inner calss ChevronMouseEvents



private class FlagContextMenuEvents implements ActionListener
{

   @Override public void actionPerformed(ActionEvent arg0) {
      JMenuItem item = (JMenuItem) arg0.getSource();
      if (_ContextMenuHash.containsKey(item)) {
	 changeIcon(_ContextMenuHash.get(item));
       }
    }

}	// end of inner class FlagCOntextMenuEvents



private class FlagMouseEvents extends MouseAdapter
{

   @Override public void mouseEntered(MouseEvent arg0) {
      _ChevronLabel.setVisible(true);

      repaint();
    }

   @Override public void mouseExited(MouseEvent arg0) {
      if ((0 <= arg0.getX()) && (arg0.getX() < flag_size) &&
            (0 <= arg0.getY()) && (arg0.getY() < flag_size)) { }
      else {
	 _ChevronLabel.setVisible(false);
	 repaint();
       }
    }

}	// end of inner class FlagMouseEvents



}	// end of class BeamFlagBubble



/* end of BeamFlagBubble.java */
