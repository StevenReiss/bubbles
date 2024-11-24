/********************************************************************************/
/*										*/
/*		BoardSplash.java						*/
/*										*/
/*	Bubbles attribute and property management splash screen 		*/
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

// animation author: Rachel Gollub, 1995

/* SVN: $Id$ */



package edu.brown.cs.bubbles.board;


import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Toolkit;


class BoardSplash {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private JFrame		splash_frame;
private BubblePanel	bubble_panel;
private JLabel		current_task;
private Image		splash_image;
private Image		brown_image;
private Image		bubbles_image;

private boolean 	show_bubbles;

private static final Color  FIRST_COLOR = new Color(0,0,255);
private static final Color  SECOND_COLOR = new Color(128,128,255);

private static boolean scale_image = false;
private static int splash_width = 450;
private static int splash_height = 300;
private static int splash_font = 9;
private static float splash_task = 12f;
private static int splash_brown = 24;
private static int splash_bubbles_w = 64;
private static int splash_bubbles_h = 96;
private static int splash_delta = 24;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BoardSplash()
{
   splash_image = BoardImage.getImage("newsplash2");
   brown_image = BoardImage.getImage("cslogo.png");
   bubbles_image = BoardImage.getImage("codebubbleslogo.png");

   show_bubbles = Math.random() > 0.99;
   bubble_panel = null;
   splash_frame = null;

   if (splash_image != null) setup();
}




/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

void start()
{
   if (splash_frame == null) return;

   splash_frame.setVisible(true);
   if (show_bubbles) bubble_panel.start();
}


void setCurrentTask(String id)
{
   if (current_task == null) return;
   
   current_task.setText(id);
}

String getCurrentTask()
{
   if (current_task == null) return null;
   
   return current_task.getText();
}



void setPercentDone(int v)
{ }



void remove()
{
   if (splash_frame == null) return;

   if (show_bubbles) bubble_panel.stop();
   splash_frame.setVisible(false);
}



/********************************************************************************/
/*										*/
/*	Graphics setup methods							*/
/*										*/
/********************************************************************************/

private void setup()
{
   Toolkit tk = Toolkit.getDefaultToolkit();
   Dimension ssz = tk.getScreenSize();
   if (ssz.width >= 3840 && ssz.height >= 2160) {
      splash_font = 12;
      splash_task = 15;
      splash_delta = 27;
      scale_image = true;
    }
   
   JPanel pnl = new JPanel(new BorderLayout());
   bubble_panel = new BubblePanel();
   pnl.add(bubble_panel,BorderLayout.CENTER);
   current_task = new JLabel("",SwingConstants.LEFT);
   current_task.setForeground(Color.black);
   if (scale_image) {
      Font f1 = current_task.getFont();
      f1 = f1.deriveFont(splash_task);
      current_task.setFont(f1);
    }
   
   pnl.setBackground(new Color(211,232,248));
   pnl.add(current_task,BorderLayout.SOUTH);

   splash_frame = new JFrame();
   splash_frame.setContentPane(pnl);
   splash_frame.setUndecorated(true);
   splash_frame.setResizable(false);
   splash_frame.setAlwaysOnTop(false); //amc6 switched from true to false

   splash_frame.pack();

   Dimension wsz = splash_frame.getSize();
   int xpos = ssz.width/2 - wsz.width/2;
   int ypos = ssz.height/2 - wsz.height/2;
   splash_frame.setLocation(xpos,ypos);
}



/********************************************************************************/
/*										*/
/*	Color methods								*/
/*										*/
/********************************************************************************/

Color getBubbleColor(int v0,int v1)
{
   double v = v0/255.0;

   int c0 = FIRST_COLOR.getRGB();
   int c1 = SECOND_COLOR.getRGB();

   int c2 = (int) (c0 * v + c1 * (1-v));

   return new Color(c2);
}



/********************************************************************************/
/*										*/
/*	Drawing methods 							*/
/*										*/
/********************************************************************************/

private void drawSplash(Graphics g,JPanel obs)
{
      String vname = "version " + BoardSetup.getVersionData();
      int idx = vname.indexOf(" @ ");
      if (idx > 0) vname = vname.substring(0,idx);
      String copyr = "Copyright 2010 by Brown University.  All Rights Reserved";
      Graphics2D g2 = (Graphics2D) g;
      Paint p = new GradientPaint(0,0,Color.WHITE,0,splash_height,new Color(211,232,248));
      Rectangle r1 = new Rectangle(0,0,splash_width,splash_height);
      g2.setPaint(p);
      g2.fill(r1);
      g2.setColor(new Color(88,88,88));
      r1.width -= 1;
      g2.draw(r1);
      g2.drawImage(brown_image,splash_brown,splash_brown,obs);
      g2.drawImage(bubbles_image,splash_bubbles_w,splash_bubbles_h,obs);
      g2.setPaint(new Color(88,179,255));
      Font f1 = new Font(Font.SANS_SERIF,Font.PLAIN,splash_font);
      g2.setFont(f1);
      g2.drawString(vname,256,200);
      g2.setColor(new Color(88,88,88));
      
      g2.drawString(copyr,24,splash_height-splash_delta);
}




/********************************************************************************/
/*										*/
/*	Bubbles animation							*/
/*										*/
/********************************************************************************/

private class BubblePanel extends JPanel implements Runnable {

   private transient Thread drawing_thread;
   private int		bubble_count;
   private int		this_bubble;
   private int		bubble_stepper;
   private int [][]	bubble_record;

   private static final int MAX_BUBBLES = 125;
   private static final long SLEEP_TIME = 50;

   private static final long serialVersionUID = 1;


   BubblePanel() {
      setOpaque(false);
      int wd = splash_width;
      int ht = splash_height;
      Dimension sz = new Dimension(wd,ht+splash_delta - 4); //amc6
      setMinimumSize(sz);
      setPreferredSize(sz);
      setMaximumSize(sz);
      drawing_thread = null;
      bubble_count = 0;
      this_bubble = 0;
      bubble_stepper = 4;
      bubble_record = new int[MAX_BUBBLES][5];
    }

   void start() {
      if (drawing_thread == null) {
	 drawing_thread = new Thread(this,"SplashDrawing");
	 drawing_thread.start();
       }
    }

   void stop() {
      if (drawing_thread != null) {
	 drawing_thread.interrupt();
	 drawing_thread = null;
       }
    }

   @Override public void run() {
      while (drawing_thread != null) {
	 try {
	    Thread.sleep(SLEEP_TIME);
	  }
	 catch (InterruptedException e) {
	    break;
	  }
	 repaint();
       }
      drawing_thread = null;
    }


   private void moveBubble(int x, int y, int r, int step, Graphics g) {
      int i;

      for (i=x-r; i<=x+r; i++) {     // Draws the upper edge of a circle
	 g.drawLine(i, y - (int) (Math.sqrt(r*r - ((i-x)*(i-x)))),
		       i, y + step - (int) (Math.sqrt(r*r - ((i-x)*(i-x)))));
       }
      g.setColor(getBackground());
      for (i=x-r; i<=x+r; i++) {     // Draws the lower edge of the circle
	 g.drawLine(i, y + (int) (Math.sqrt(r*r - ((i-x)*(i-x)))),
		       i, y + step + (int) (Math.sqrt(r*r - ((i-x)*(i-x)))));
       }
    }

   @Override public void paint(Graphics g) {
      int i;
      int j;
      int tmp;
      Dimension d = getSize();
      Color col;
   
      drawSplash(g,this);
      // g.drawImage(splash_image,0,0,this);
   
      if (bubble_count < MAX_BUBBLES || this_bubble < MAX_BUBBLES) {
         bubble_record[this_bubble][0]=(int) (Math.random() * d.width);
         bubble_record[this_bubble][1]=d.height+50;
         bubble_record[this_bubble][2]=(int) (Math.random() * d.width)/20;
         bubble_record[this_bubble][3]=(int) (Math.random() * 255);
         bubble_record[this_bubble][4]=(int) (Math.random() * 255);
         col = getBubbleColor(bubble_record[this_bubble][3],bubble_record[this_bubble][4]);
         g.setColor(col);
         g.fillOval(bubble_record[this_bubble][0]-bubble_record[this_bubble][2],
        	       bubble_record[this_bubble][1]-bubble_record[this_bubble][2],
        	       bubble_record[this_bubble][2]*2,bubble_record[this_bubble][2]*2);
         if (bubble_count < MAX_BUBBLES) {
            bubble_count++;
            this_bubble++;
          }
         else this_bubble = MAX_BUBBLES;
       }
   
      for (i=0; i<bubble_count; i++) {
         if (i%5 <= bubble_stepper) { // Steps each bubble at a different speed
            bubble_record[i][1] -= 1;
            col = new Color(bubble_record[i][3], bubble_record[i][4], 255);
            g.setColor(col);
            moveBubble(bubble_record[i][0], bubble_record[i][1], bubble_record[i][2], 1, g);
            for (j=0; j<i; j++) {   // Checks for touching bubbles, pops one
               tmp = ((bubble_record[i][1]-bubble_record[j][1])*(bubble_record[i][1]-bubble_record[j][1]) +
        		  (bubble_record[i][0]-bubble_record[j][0])*(bubble_record[i][0]-bubble_record[j][0]));
               if (j != i && Math.sqrt(tmp) < bubble_record[i][2] + bubble_record[j][2]) {
        	  g.setColor(getBackground());
        	  for (tmp = bubble_record[i][2]; tmp >= -1; tmp = tmp - 2) {
        	     g.fillOval(bubble_record[i][0]-(bubble_record[i][2]-tmp),
        			   bubble_record[i][1]-(bubble_record[i][2]-tmp),
        			   (bubble_record[i][2]-tmp)*2, (bubble_record[i][2]-tmp)*2);
                   }
        	  col = getBubbleColor(bubble_record[j][3],bubble_record[j][4]);
        	  g.setColor(col);
        	  g.fillOval(bubble_record[j][0]-bubble_record[j][2], bubble_record[j][1]-bubble_record[j][2],
        			bubble_record[j][2]*2, bubble_record[j][2]*2);
        	  bubble_record[i][1] = -1; bubble_record[i][2]=0;
        	}
             }
          }
         if (bubble_record[i][1]+bubble_record[i][2] < 0 && bubble_count >= MAX_BUBBLES) {
            this_bubble = i;
          }
         bubble_stepper=(int) (Math.random()*10);
         col = null;
       }
    }

}	// end of inner class BubblePanel



}	// end of class BoardSpash




/* end of BoardSplash.java */

