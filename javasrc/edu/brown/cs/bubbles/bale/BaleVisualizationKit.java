/********************************************************************************/
/*										*/
/*		BaleVisualizationKit.java					*/
/*										*/
/*	Bubble Annotated Language Editor constant definitions			*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Yu Li				*/
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


package edu.brown.cs.bubbles.bale;

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class BaleVisualizationKit implements BaleConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static BaleVisualizationKit visual_kit = new BaleVisualizationKit();




/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private static final String []	GRADIENT_COLORS;
private static final int	NUM_COLORS;

static {
   List<String> colors = new ArrayList<>();
   for (int i = 0; i < 32; ++i) {
      String nm = VISUALIZATION_GRADIENT_COLORS + i;
      Color c1 = BoardColors.getColor(nm,null);
      if (c1 == null) continue;
      colors.add(nm);
    }
   if (colors.size() == 0) colors.add("Bale.visualization.default.color");
   NUM_COLORS = colors.size();
   GRADIENT_COLORS = colors.toArray(new String[NUM_COLORS]);
}



/********************************************************************************/
/*										*/
/*	Static entries								*/
/*										*/
/********************************************************************************/

static BaleVisualizationKit getVisualizationKit()
{
   return visual_kit;
}


/********************************************************************************/
/*										*/
/*	Visualization gradient methods						*/
/*										*/
/********************************************************************************/

/**
 * Get gradient paint
 **/

GradientPaint getGradientPaint(Dimension bsize, String indication)
{
   float[] d;
   if (BALE_PROPERTIES.getString(VISUALIZATION_GRADIENT_DIRECTION).equals("vertical")) {
      d =  new float[]{0f, bsize.height, 0f, 0f};
    }
   else if (BALE_PROPERTIES.getString(VISUALIZATION_GRADIENT_DIRECTION).equals("horizontal")) {
      d = new float[]{bsize.width, 0f, 0f, 0f};
    }
   else if (BALE_PROPERTIES.getString(VISUALIZATION_GRADIENT_DIRECTION).equals("skew")) {
      d = new float[]{bsize.width, bsize.height, bsize.width/2f, bsize.height/2f };
    }
   else {
      d = new float[]{0f,0f,0f,bsize.height};
    }

   int idx = 0;
   if (indication != null) idx = (indication.hashCode() % NUM_COLORS + NUM_COLORS) % NUM_COLORS;
   Color c = BoardColors.getColor(GRADIENT_COLORS[idx]);

   return new GradientPaint(d[0], d[1], c, d[2], d[3],BoardColors.getColor("Bale.visualization.base.color"));
}



/********************************************************************************/
/*										*/
/* Visualization Icon methods							*/
/*										*/
/********************************************************************************/

/**
 * Get icon by indication
 **/
BufferedImage getIcon(String indication)
{
   if (indication != null) {
      int hc = indication.hashCode() % NUM_ICONS;
      String id = "visualization/" + BALE_PROPERTIES.getString(VISUALIZATION_ICON_SIZE)
		   +"_leaf/leaf" + Math.abs(hc) + ".png";
      Image image = BoardImage.getImage(id);
      if (image == null) return null;

      BufferedImage buf = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
      Graphics2D g2 = buf.createGraphics();
      float op = BALE_PROPERTIES.getInt(VISUALIZATION_ICON_TRANSPARENT)/100f;
      AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, op);
      g2.setComposite(ac);
      g2.drawImage(image, 0, 0, null);

      return buf;
    }
   else return null;
}

/**
 * Get location of icon by user's option
 **/
Point getLocation(Dimension bsize, Dimension isize)
{
   int x = 0;
   int y = 0;
   if (BALE_PROPERTIES.getString(VISUALIZATION_ICON_LOCATION).equals("center")) {
      x = (bsize.width-isize.width)/2;
      y = (bsize.height-isize.height)/2;
   }
   else if (BALE_PROPERTIES.getString(VISUALIZATION_ICON_LOCATION).equals("topright")) {
      x = bsize.width-isize.width;
      y = 0;
   }
   else if (BALE_PROPERTIES.getString(VISUALIZATION_ICON_LOCATION).equals("topleft")) {
      x = 0;
      y = 0;
   }
   else if (BALE_PROPERTIES.getString(VISUALIZATION_ICON_LOCATION).equals("bottomright")) {
      x = bsize.width-isize.width;
      y = bsize.height - isize.height;
   }
   else if (BALE_PROPERTIES.getString(VISUALIZATION_ICON_LOCATION).equals("bottomleft")) {
      x = 0;
      y = bsize.height - isize.height;
   }
   return new Point(x,y);
}



/********************************************************************************/
/*										*/
/*	<comment here>								*/
/*										*/
/********************************************************************************/

static String getIndication(String proj,String from,String key)
{
   if (from == null) return null;

   // convert from to a class name
   int idx = from.indexOf("(");
   if (idx > 0) {
      from = from.substring(0,idx);
      idx = from.lastIndexOf(".");
      if (idx > 0) from = from.substring(0,idx);
    }
    else {
       idx = from.indexOf(".<");
       if (idx > 0) {
	  from = from.substring(0,idx);
	}
     }

   if (BALE_PROPERTIES.getString(key).equals("package")) {
      for ( ; ; ) {
	 idx = from.lastIndexOf(".");
	 if (idx < 0) return null;
	 from = from.substring(0,idx);
	 if (checkPackage(proj,from)) break;
       }
    }

   idx = from.lastIndexOf(".");
   if (idx >= 0) from = from.substring(idx+1);
   if (from.length() == 0) return null;

   return from;
}


private static Map<String,Boolean> known_packages = new HashMap<>();

private static boolean checkPackage(String proj,String pkg)
{
   Boolean bv = known_packages.get(pkg);
   if (bv != null) return bv.booleanValue();

   boolean rslt = false;
   List<BumpLocation> bl = BumpClient.getBump().findPackage(proj,pkg);
   if (bl == null || bl.isEmpty()) rslt = false;
   else rslt = true;

   known_packages.put(pkg,rslt);

   return rslt;
}





}	// end of class BaleVisualizationKit



/* end of BaleVisualizationKit.java */

