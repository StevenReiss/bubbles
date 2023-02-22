/********************************************************************************/
/*										*/
/*		BoardColors.java						*/
/*										*/
/*	Provide a common framework for all color choices			*/
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



package edu.brown.cs.bubbles.board;

import edu.brown.cs.ivy.swing.SwingColorSet;

import java.awt.Color;
import java.awt.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.UIDefaults;
import javax.swing.UIManager;

public class BoardColors implements BoardConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,Color>	color_map;
private String			palette_name;
private File			palette_file;
private long			last_modified;
private int			check_count;
private List<URL>		added_palettes;

private Map<Object,Color>	default_colors;

private static BoardColors	the_colors;
public static Color TRANSPARENT = new Color(0,0,0,0);

private static int MAX_COUNT =	16;		// check every so often





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BoardColors()
{
   palette_name = DEFAULT_PALETTE;
   color_map = null;
   palette_file = null;
   check_count = 0;
   default_colors = null;
   added_palettes = new ArrayList<>();
}


synchronized static BoardColors getColors()
{
   if (the_colors == null) {
      the_colors = new BoardColors();
    }
   return the_colors;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public static void setPalette(String name)
{
   if (name == null) name = DEFAULT_PALETTE;
   BoardColors bc = getColors();
   bc.palette_name = name;
   bc.palette_file = null;
   bc.color_map = null;

   forceLoad();

   if (isInverted()) {
      bc.setDefaults(true);
    }
   else {
      bc.setDefaults(false);
    }
}


public static void addPalette(URL u)
{
   BoardColors bc = getColors();
   synchronized (bc) {
      bc.added_palettes.add(u);
      bc.color_map = null;
    }
}


public static String getPalette()
{
   BoardColors bc = getColors();
   return bc.palette_name;
}


public static Color getColor(String id)
{
   return getColor(id,Color.WHITE);
}


public static Color getColor(String id,Color dflt)
{
   if (id == null) return dflt;

   BoardColors bc = getColors();

   Map<String,Color> cm = bc.loadPalette();

   Color c = cm.get(id);
   if (c != null) return c;

   // first use property files
   int idx = id.indexOf(".");
   if (idx >= 0) {
      String pfx = id.substring(0,idx);
      String sfx = id.substring(idx+1);
      BoardProperties bp = BoardProperties.getProperties(pfx);
      if (bp != null) {
	 Color c1 = bp.getColor(id,null);
	 if (c1 == null) c1 = bp.getColor(sfx,null);
	 if (c1 != null) c = c1;
       }
    }

   // finally use default
   if (c == null && dflt != null) {
      BoardLog.logX("BOARD","Color property " + id + " not found");
      c = dflt;
    }

   return c;
}



/********************************************************************************/
/*										*/
/*	Relative color methods							*/
/*										*/
/********************************************************************************/

public static Color getPaleColor(Color c)
{
   return getPaleColor(c,0.125);
}



public static Color getPaleColor(Color c,double v)
{
   if (c == null) c = Color.RED;

   float [] hsb = Color.RGBtoHSB(c.getRed(),c.getGreen(),c.getBlue(),null);
   hsb[1] *= (float) v;

   int cv = Color.HSBtoRGB(hsb[0],hsb[1],hsb[2]);
   int av = c.getRGB() & 0xff000000;
   cv &= 0x00ffffff;
   cv |= av;

   return new Color(cv,true);
}


public static Color getTextColor(Color bkg)
{
   int lum = (bkg.getRed() * 299 + bkg.getGreen() * 587 + bkg.getBlue() * 114)/1000;
   if (lum > 125) return Color.BLACK;
   return Color.WHITE;
}


public static Color transparent()
{
   return TRANSPARENT;
}


public static Color transparent(Color c)
{
   int cx = c.getRGB();
   cx &= 0x00ffffff;
   cx |= 0x40000000;
   return new Color(cx,true);
}


public static Color transparent(Color c,int alpha)
{
   return new Color(c.getRed(),c.getGreen(),c.getBlue(),alpha);
}

public static Color transparent(Color c,double alpha)
{
   return new Color(c.getRed(),c.getGreen(),c.getBlue(),(float) alpha);
}


public static Color between(Color tc,Color bc)
{
   return new Color((tc.getRed() + bc.getRed())/2,
	 (tc.getGreen() + bc.getGreen())/2,
	 (tc.getBlue() + bc.getBlue())/2);
}


public static Color randomColor(double alpha)
{
   if (alpha > 1) alpha = 1;
   if (alpha <= 0) alpha = 0.75;
   int av = (int)(alpha * 255);
   av &= 0xff;
   av <<= 24;

   int rcol = Color.HSBtoRGB((float)(Math.random() * 0.8),0.75f,0.75f);
   rcol &= 0x00ffffff;
   rcol |= av;

   return new Color(rcol,true);
}



public static void setColors(Component c,String nm)
{
   Color bg = getColor(nm);
   setColors(c,bg);
}



public static void setColors(Component c,Color bg)
{
   c.setBackground(bg);
   c.setForeground(getTextColor(bg));
}


public static void setTransparent(Component c,Component par)
{
   c.setBackground(BoardColors.transparent());

   if (par == null) par = c.getParent();
   if (par == null) return;
   Color tc = getTextColor(par.getBackground());
   c.setForeground(tc);
}



public static String toHtmlColor(String nm)
{
   return toHtmlColor(getColor(nm));
}


public static String toHtmlColor(Color cval)
{
   String cv = Integer.toUnsignedString(cval.getRGB(),16);
   int cvlen = cv.length();
   if (cvlen > 6) cv = cv.substring(cvlen-6);
   while (cv.length() < 6) cv = "0" + cv;
   cv = "#" + cv;
   return cv;
}



public static String toColorString(Color cval)
{
   String cv = Integer.toUnsignedString(cval.getRGB(),16);
   int cvlen = cv.length();
   if (cvlen > 6) cv = cv.substring(cvlen-6);
   while (cv.length() < 6) cv = "0" + cv;
   cv = "0x" + cv;
   return cv;
}


public static boolean isInverted()
{
   Color c = getColor("Board.base.color");
   int lum = (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114)/1000;
   if (lum < 125) return true;
   return false;
}



/********************************************************************************/
/*										*/
/*	Palette loading methods 						*/
/*										*/
/********************************************************************************/

public static void forceLoad()
{
   getColors().check_count = MAX_COUNT;
}



synchronized private Map<String,Color> loadPalette()
{
   if (palette_file != null && check_count++ >= MAX_COUNT) {
      if (palette_file.lastModified() > last_modified) {
	 palette_file = null;
	 color_map = null;
       }
      check_count = 0;
    }
   if (color_map != null) return color_map;
   color_map = new HashMap<>();

   InputStream insd = getDefaultPaletteStream();
   loadPaletteData(insd,false);

   InputStream ins = getPaletteStream();
   if (ins != null) {
      loadPaletteData(ins,false);
      createInversePalette();
    }

   for (URL u : added_palettes) {
      addPaletteData(u);
    }

   return color_map;
}




private InputStream getPaletteStream()
{
   palette_file = null;
   last_modified = 0;

   InputStream ins = null;
   if (palette_name.startsWith(File.separator)) {
      File f = new File(palette_name);
      if (!f.exists()) {
	 File f1 = new File(palette_name + ".palette");
	 if (f1.exists()) f = f1;
       }
      if (f.exists() && f.canRead()) {
	 try {
	    return new FileInputStream(f);
	  }
	 catch (IOException e) { }
       }
    }

   File dir0 = BoardProperties.getPropertyDirectory();
   File dir1 = BoardSetup.getSetup().getResourceDirectory();
   File f = new File(dir0,palette_name);
   if (!f.exists()) {
      File f1 = new File(dir0,palette_name + ".palette");
      if (f1.exists()) f = f1;
    }
   if (!f.exists()) {
      File f1 = new File(dir1,palette_name);
      if (f1.exists()) f = f1;
    }
   if (!f.exists()) {
      File f1 = new File(dir1,palette_name + ".palette");
      if (f1.exists()) f = f1;
    }

   if (f.exists() && f.canRead()) {
      try {
	 palette_file = f;
	 last_modified = f.lastModified();
	 return new FileInputStream(f);
       }
      catch (IOException e) { }
    }

   String nm = palette_name;
   if (!nm.endsWith(".palette")) nm = nm + ".palette";
   ins = BoardProperties.getResourceFile(nm);
   if (ins != null) return ins;

   return null;
}



private InputStream getDefaultPaletteStream()
{
   InputStream ins = null;

   String nm = DEFAULT_PALETTE;

// File dir1 = BoardSetup.getSetup().getResourceDirectory();
// File f = new File(dir1,palette_name);
// if (f.exists() && f.canRead()) {
//    try {
// 	 return new FileInputStream(f);
//     }
//    catch (IOException e) { }
//  }

   ins = BoardProperties.getResourceFile(nm);
   if (ins != null) return ins;

   return null;
}



private void loadPaletteData(InputStream ins,boolean added)
{
   if (ins == null) return;

   BufferedReader br = new BufferedReader(new InputStreamReader(ins));
   try {
      for ( ; ; ) {
	 String ln = br.readLine();
	 if (ln == null) break;
	 ln = ln.trim();
	 if (ln.startsWith("#") || ln.startsWith("/")) continue;
	 StringTokenizer tok = new StringTokenizer(ln," \t:");
	 if (!tok.hasMoreTokens()) continue;
	 String nm = tok.nextToken();
	 if (added && color_map.containsKey(nm)) continue;
	 if (!tok.hasMoreTokens()) continue;
	 String col = tok.nextToken();
	 Color c = SwingColorSet.getColorByName(col);
	 if (c != null) {
	    if (added && isInverted()) c = invertColor(c);
	    color_map.put(nm,c);
	  }
	 else BoardLog.logX("BOARD","Color " + col + " not found");
       }
      br.close();
    }
   catch (IOException ex) {
      BoardLog.logE("BOARD","Problem reading palette file",ex);
    }
}


private void addPaletteData(URL u)
{
   try {
      URLConnection uc = u.openConnection();
      InputStream ins = uc.getInputStream();
      loadPaletteData(ins,true);
    }
   catch (IOException ex) {
      BoardLog.logE("BOARD","Problem reading extra palette data for " + u,ex);
    }
}




/********************************************************************************/
/*										*/
/*	Handle creating a dark pallette 					*/
/*										*/
/********************************************************************************/

private void createInversePalette()
{
   if (palette_file == null) return;
   String nm = palette_file.getName();
   if (nm.startsWith("inverse_")) return;
   File f = new File(palette_file.getParentFile(),"inverse_" + nm);
   if (f.exists() && f.lastModified() >= palette_file.lastModified()) return;
   try {
      PrintWriter pw = new PrintWriter(new FileWriter(f));
      for (Map.Entry<String,Color> ent : color_map.entrySet()) {
	 Color c1 = invertColor(ent.getValue());
	 String cv = Integer.toUnsignedString(c1.getRGB(),16);
	 pw.println(ent.getKey() + "\t:\t0x" + cv);
       }
      pw.close();
    }
   catch (IOException e) { }
}



public static Color invertColor(Color base)
{
   int r = base.getRed();
   int g = base.getGreen();
   int b = base.getBlue(); 
   
   int lum = (r * 299 + g * 587 + b * 114)/1000;
   
   int invlum = 255-lum;
   if (invlum >= 192) {
      invlum += (255-invlum)/5;
      if (invlum > 255) invlum = 255;
    }

   int rinv = 0;
   int ginv = 0;
   int binv = 0;

   if (invlum == 0) return Color.BLACK;
   else if (r == 0) {
      if (g == 0 && b == 0) return Color.WHITE;
      else if (g == 0) {
	 binv = invlum * 1000 / 114;
       }
      else {
	 double x =  b * 114.0 / g + 587.0;
	 ginv = (int) (invlum * 1000 / x);
	 binv = b * ginv / g;
       }
    }
   else {
      double x = 299.0 + g * 587.0 / r + b * 114 / r;
      rinv = (int)(invlum * 1000 / x);
      ginv = g * rinv / r;
      binv = b * rinv / r;
    }

   if (binv > 255) {
      int w = (binv-255)*114/(299+587);
      if (w > 180) w = 180;
      binv = 255;
      ginv += w;
      rinv += w;
    }
   if (rinv < 0) rinv = 0;
   else if (rinv > 255) rinv = 255;
   if (binv < 0) binv = 0;
   else if (binv > 255) binv = 255;
   if (ginv < 0) ginv = 0;
   else if (ginv > 255) ginv = 255;

   if (r + g + b > 256*3-200) {
      float [] rslt = Color.RGBtoHSB(rinv,binv,ginv,null);
      int hval = (int)(360*rslt[0]);
      if (hval > 10 && hval < 350 && rslt[1] < 0.3 && rslt[2] < 0.25) {
	 // pastel color
	 rslt[1] = 0.3f;
	 rslt[2] = 0.25f;
	 Color cnew = Color.getHSBColor(rslt[0],rslt[1],rslt[2]);
	 if (base.getAlpha() != 255) {
	    cnew = transparent(cnew,base.getAlpha());
	  }
//	 System.err.println("COMPARE COLOR " + cnew + " " + rinv + " " + ginv + " " + binv);
	 return cnew;
       }
    }

   return new Color(rinv,ginv,binv,base.getAlpha());
}


public static Color invertColorW3(Color base)
{
   int r = base.getRed();
   int g = base.getGreen();
   int b = base.getBlue();
   
   double rsrgb = r / 255.0;
   double gsrgb = g / 255.0;
   double bsrgb = b / 255.0;
   double rr = (rsrgb < 0.03928 ? rsrgb/12.92 : Math.pow((rsrgb + 0.055)/1.055,2.4));
   double gg = (gsrgb < 0.03928 ? gsrgb/12.92 : Math.pow((gsrgb + 0.055)/1.055,2.4)); 
   double bb = (bsrgb < 0.03928 ? bsrgb/12.92 : Math.pow((bsrgb + 0.055)/1.055,2.4));
   double lum = rr * 0.2126 + gg * 0.7152 + bb * 0.0722;
   double invlum = 1.0 - lum; 
   
   double vmin = Math.min(Math.min(rsrgb,gsrgb),bsrgb);
   double rpct = rsrgb - vmin;
   double gpct = gsrgb - vmin;
   double bpct = bsrgb - vmin;
   
   double rinv = 0;
   double ginv = 0;
   double binv = 0;
   
   if (invlum == 0) return Color.BLACK;
   else if (rpct == 0) {
      if (gpct == 0 && bpct == 0) return Color.WHITE;
      else if (gpct == 0) {
         binv = invlum / 0.0722;
       }
      else {
         double x = 0.7152 + bpct/gpct * 0.0722;
         ginv = invlum / x;
         binv = bpct/gpct * ginv;
       }
    }
   else {
      double x = 0.2126 + gpct * 0.7152 / rpct + bpct * 0.0722 / rpct;
      rinv = invlum / x;
      ginv = gpct * rinv / rsrgb;
      binv = bpct * rinv / rsrgb;
    }
   
   // need to scale by adding white in if inverse is > 1.0
   
   int rset = rescale(rinv);
   int gset = rescale(ginv);
   int bset = rescale(binv);
   
   Color cnew = new Color(rset,gset,bset,base.getAlpha());
   
   return cnew;
}


private static int rescale(double v) 
{
   double rx = 0;
   
   if (v * 12.92 <= 0.03928) rx = v * 12.92;
   else rx = Math.pow(v,1/2.4)* 1.055 - 0.055;
   if (rx < 0) rx = 0;
   else if (rx > 1) rx = 1;
   
   int r = (int) (rx * 255 + 0.4);
   
   return r;
   
}



/********************************************************************************/
/*										*/
/*	Handle default colors							*/
/*										*/
/********************************************************************************/

private void setDefaults(boolean inv)
{
   if (inv) {
      boolean savedflts = false;
      if (default_colors == null) {
	 default_colors = new HashMap<>();
	 savedflts = true;
       }
      UIDefaults dflts = UIManager.getDefaults();
      Map<Object,Object> chngs = new HashMap<>();
      for (Map.Entry<Object,Object> ent : dflts.entrySet()) {
	 if (ent.getValue() instanceof Color) {
	    Color c = (Color) ent.getValue();
	    if (savedflts) default_colors.put(ent.getKey(),c);
	    Color cinv = invertColor(c);
	    chngs.put(ent.getKey(),cinv);
	  }
       }
      dflts.putAll(chngs);
    }
   else {
      UIDefaults dflts = UIManager.getDefaults();
      if (default_colors != null) {
	 dflts.putAll(default_colors);
       }
    }
}



}	// end of class BoardColors




/* end of BoardColors.java */

