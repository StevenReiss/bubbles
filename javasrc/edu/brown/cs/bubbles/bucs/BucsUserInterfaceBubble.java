/********************************************************************************/
/*										*/
/*		BucsUserInterfaceBubble.java					*/
/*										*/
/*	Bubble to show potential user interfaces for a source			*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.bubbles.bucs;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.bump.BumpLocation;

import edu.brown.cs.ivy.swing.SwingText;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


class BucsUserInterfaceBubble extends BudaBubble implements BucsConstants, ActionListener
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BumpLocation	bump_location;
private JLabel		status_label;
private JButton 	left_button;
private JButton 	right_button;
private ImageViewer	image_viewer;

private List<UIImage>	image_list;
private int		image_index;



private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BucsUserInterfaceBubble(BumpLocation loc)
{
   bump_location = loc;
   status_label = null;
   image_list = null;
   image_index = 0;

   JPanel pnl = setupPanel();

   setInteriorColor(BoardColors.getColor("Buces.UserInterior"));
   setContentPane(pnl);

   startUISearch();
}


/********************************************************************************/
/*										*/
/*	Methods to setup the contents						*/
/*										*/
/********************************************************************************/

private JPanel setupPanel()
{
   JPanel  pnl = new JPanel(new BorderLayout());

   String cnts = "User Interfaces for ";
   cnts += bump_location.getSymbolName();
   JLabel top = new JLabel(cnts);
   pnl.add(top,BorderLayout.NORTH);

   status_label = new JLabel("Setting up...");
   pnl.add(status_label,BorderLayout.SOUTH);

   Font ft = pnl.getFont();
   ft = ft.deriveFont(48.0f);

   left_button = new JButton("<");
   left_button.setFont(ft);
   left_button.setEnabled(false);
   left_button.addActionListener(this);
   pnl.add(left_button,BorderLayout.WEST);

   right_button = new JButton(">");
   right_button.setFont(ft);
   right_button.setEnabled(false);
   right_button.addActionListener(this);
   pnl.add(right_button,BorderLayout.EAST);

   image_viewer = new ImageViewer();
   pnl.add(image_viewer,BorderLayout.CENTER);

   return pnl;
}


/********************************************************************************/
/*										*/
/*	Methods to query for user interfaces					*/
/*										*/
/********************************************************************************/

private void startUISearch()
{
   BucsS6Engine eng = new BucsS6Engine(bump_location);
   eng.startUISearch(new SearchRequest());
}


private class SearchRequest implements BucsSearchRequest {

   SearchRequest() { }

   @Override public void handleSearchFailed() {
      image_index = 0;
      image_list = new ArrayList<UIImage>();
      image_viewer.repaint();
      status_label.setText("UI Search Failed");
    }

   @Override public void handleSearchSucceeded(List<BucsSearchResult> result) {
    }

   @Override public void handleSearchInputs(List<BucsSearchInput> result) {
      List<UIImage> rslt = new ArrayList<UIImage>();
      for (BucsSearchInput sr : result) {
	 UIImage img = new UIImage(sr);
	 rslt.add(img);
       }

      status_label.setText("Returned " + rslt.size() + " result(s)");

      image_index = 0;
      image_list = rslt;
      if (image_list.size() > 1) {
	 left_button.setEnabled(true);
	 right_button.setEnabled(true);
       }
      image_viewer.repaint();
    }

}	// end of inner class SearchRequest



/********************************************************************************/
/*										*/
/*	Action handling 							*/
/*										*/
/********************************************************************************/

@Override public void actionPerformed(ActionEvent evt)
{
   if (evt.getSource() == left_button) {
      if (image_list == null || image_list.size() < 2) return;
      image_index = (image_index + image_list.size() - 1) % image_list.size();
      image_viewer.repaint();
    }
   else if (evt.getSource() == right_button) {
      if (image_list == null || image_list.size() < 2) return;
      image_index = (image_index + 1) % image_list.size();
      image_viewer.repaint();
    }
}


/********************************************************************************/
/*										*/
/*	ImageViewer panel							*/
/*										*/
/********************************************************************************/

private class ImageViewer extends JPanel {

   private final static long serialVersionUID = 1;

   ImageViewer() {
      setMinimumSize(new Dimension(100,100));
      setPreferredSize(new Dimension(200,200));
    }

   @Override public void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g;

      Rectangle r = getBounds();
      r.x = r.y = 0;
      g.clearRect(0,0,r.width,r.height);

      if (image_list == null || image_index < 0) {
	 SwingText.drawText("Waiting for results ...",g2,r);
       }
      else if (image_list.size() == 0) {
	 SwingText.drawText("No User Interfaces found",g2,r);
       }
      else {
	 UIImage uimage = image_list.get(image_index);
	 BufferedImage img = uimage.getScaledImage(r.width,r.height);
	 int dx = (r.width - img.getWidth())/2;
	 int dy = (r.height - img.getHeight())/2;
	 g.drawImage(img,dx,dy,this);
       }
    }

}	// end of inner class ImageViewer



/********************************************************************************/
/*										*/
/*	Result contains 							*/
/*										*/
/********************************************************************************/

private static class UIImage {

   private BufferedImage orig_image;
   private BufferedImage scaled_image;
   private int scaled_width;
   private int scaled_height;

   UIImage(BucsSearchInput rslt) {
      orig_image = rslt.getImage();
      scaled_image = null;
      scaled_width = 0;
      scaled_height = 0;
    }

   BufferedImage getScaledImage(int w,int h) {
      if (orig_image == null) return null;
      if (scaled_width == w && scaled_height == h && scaled_image != null) return scaled_image;

      double uw = orig_image.getWidth();
      double uh = orig_image.getHeight();
      double ws = w/uw;
      double hs = h/uh;
      double scale = Math.min(ws,hs);
      if (scale > 1) scale = 1;
      if (scale != 1) {
	 int nw = (int)(uw*scale);
	 int nh = (int)(uh*scale);
	 scaled_image = new BufferedImage(nw,nh,BufferedImage.TYPE_INT_ARGB);
	 Graphics g = scaled_image.createGraphics();
	 g.drawImage(orig_image,0,0,nw,nh,null);
       }
      else {
	 scaled_image = orig_image;
      }

      scaled_height = h;
      scaled_width = w;
	
      return scaled_image;
    }

}	// end of inner class UIImage



}	// end of class BucsUserInterfaceBubble




/* end of BucsUserInterfaceBubble.java */

