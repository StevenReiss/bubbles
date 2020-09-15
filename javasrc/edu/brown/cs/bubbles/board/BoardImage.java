/********************************************************************************/
/*										*/
/*		BoardImage.java 						*/
/*										*/
/*	Bubbles attribute and property management image and icon retriever	*/
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



package edu.brown.cs.bubbles.board;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;



/**
 *	This class is used to access images and icons associated with bubbles.
 **/

public class BoardImage implements BoardConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private static Map<String,ImageIcon>	icon_map = new HashMap<String,ImageIcon>();
private static Boolean use_jar = null;



/********************************************************************************/
/*										*/
/*	Image retrieval methods 						*/
/*										*/
/********************************************************************************/

/**
 *	Return the bubbles icon with the given name.  Names correspond to filenames
 *	in the bubbles images directory.
 **/

public static Icon getIcon(String id)
{
   ImageIcon ii = findIcon(id,null);

   return ii;
}




public static Icon getIcon(String id,int w,int h)
{
   Image img = getImage(id);
   if (img == null) return null;
   Image img1 = img.getScaledInstance(w,h,Image.SCALE_SMOOTH);
   return new ImageIcon(img1);
}




public static Icon getIcon(String id,Class<?> base)
{
   ImageIcon i1 = findIcon(id,base);
   
   return i1;
}





/**
 *	Return the bubbles image with the given name.  Names correspond to filenames
 *	in the bubbles images directory.  Note that images can be returned either as
 *	actual Images or as Icons.
 **/

public static Image getImage(String id)
{
   ImageIcon ii = findIcon(id,null);

   if (ii == null) return null;

   return ii.getImage();
}



public static BufferedImage getBufferedImage(String id)
{
   Image img = getImage(id);
   if (img instanceof BufferedImage) return (BufferedImage) img;

   BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

   Graphics2D bGr = bimage.createGraphics();
   bGr.drawImage(img, 0, 0, null);
   bGr.dispose();

   return bimage;
}



/********************************************************************************/
/*										*/
/*	Image manipulation methods						*/
/*										*/
/********************************************************************************/

/**
 * Resizes an icon
 */

public static Icon resizeIcon(Image input, int newWidth, int newHeight)
{
   BufferedImage output = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);

   Graphics g = output.createGraphics();

   g.drawImage(input, 0, 0, newWidth, newHeight, null);
   g.dispose();

   ImageIcon result = new ImageIcon(output);

   return result;
}




/********************************************************************************/
/*										*/
/*	ImageIcon loading methods						*/
/*										*/
/********************************************************************************/

private static synchronized ImageIcon findIcon(String id,Class<?> base)
{
   int idx = id.lastIndexOf(".png");                    // remove .png if it is present
   if (idx > 0) id = id.substring(0,idx);

   if (icon_map.containsKey(id)) return icon_map.get(id);

   ImageIcon ii = null;

   File dir0 = BoardSetup.getPropertyBase();
   if (dir0.exists()) {
      File dir1 = new File(dir0,"images");
      if (dir1.exists()) {
	 File f3 = new File(dir1,id + ".png");
	 if (f3.exists() && f3.canRead()) {
	    ii = new ImageIcon(f3.getAbsolutePath());
	    int sts = ii.getImageLoadStatus();
	    if (sts == MediaTracker.COMPLETE) return ii;
	  }
       }
    }

   if (use_jar == null) {
      URL url = BoardImage.class.getClassLoader().getResource(BOARD_RESOURCE_CHECK);
      if (url != null && url.toString().startsWith("jar")) use_jar = true;
      else use_jar = false;
    }

   // BoardLog.logI("BOARD","Find image " + id + " " + use_jar);

   if (use_jar.booleanValue()) {
      String nm = "images/" + id + ".png";
      URL url = null;
      if (base == null) 
         url = BoardImage.class.getClassLoader().getResource(nm);
      else 
         url = base.getClassLoader().getResource(nm);
      
      if (url == null) {
	 BoardLog.logE("BOARD","Can't find image " + nm + " as resource");
       }
      else {
	 ii = new ImageIcon(url);
       }
    }
   else {
      BoardProperties props = BoardProperties.getProperties("System");
      String root = props.getProperty(BOARD_PROP_INSTALL_DIR);
      if (root == null) return null;
      File f1 = new File(root);
      File f2 = new File(f1,"images");
      File f3 = new File(f2,id + ".png");
      ii = new ImageIcon(f3.getAbsolutePath());
    }

   icon_map.put(id,ii);

   return ii;
}




}	// end of class BoardImage




/* end of BoardImage.java */
