/********************************************************************************/
/*										*/
/*		BicexGraphicsModel.java 					*/
/*										*/
/*	Description of graphics window and drawing information			*/
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



package edu.brown.cs.bubbles.bicex;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;

import org.w3c.dom.Element;

import edu.brown.cs.bubbles.board.BoardLog;

import edu.brown.cs.ivy.xml.IvyXml;

class BicexGraphicsModel implements BicexConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,GraphicsData> window_map;
private boolean have_complete;


enum FieldType {
   NONE, FG, BG, PAINT, STROKE, COMPOSITE, HINTS, FONT, CLIP, TRANSFORM
}


enum CommandType {
   NONE, DRAW_ARC, FILL_ARC, DRAW_LINE, DRAW_OVAL, FILL_OVAL,
   DRAW_POLYGON, FILL_POLYGON, FILL_RECT, DRAW_POLYLINE, DRAW_ROUND_RECT,
   FILL_ROUND_RECT, DRAW, FILL, CLEAR_RECT, COPY_AREA, DRAW_IMAGE,
   DRAW_RENDERED_IMAGE, DRAW_RENDERABLE_IMAGE, DRAW_STRING, DRAW_GLYPH_VECTOR,
   DRAW_RECT, DRAW_3D_RECT, FILL_3D_RECT,
   TRANSFORM, ROTATE, SCALE, SHEAR, TRANSLATE, GET_TRANSFORM, SET_TRANSFORM,
   CONSTRAIN, CLIP_RECT, SET_CLIP, CLIP,
}


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BicexGraphicsModel()
{
   window_map = new LinkedHashMap<>();
   have_complete = false;
}




/********************************************************************************/
/*										*/
/*	Access methopds 							*/
/*										*/
/********************************************************************************/

List<DisplayModel> getActiveModels()
{
   List<DisplayModel> rslt = new ArrayList<>();

   rslt.addAll(window_map.values());

   return rslt;
}




/********************************************************************************/
/*										*/
/*	Update this model							*/
/*										*/
/********************************************************************************/

void update(Element xml,boolean errfg,boolean complete)
{
   Set<String> done = new HashSet<String>();

   for (Element gxml : IvyXml.children(xml,"GRAPHICS")) {
      String name = IvyXml.getAttrString(gxml,"ID");
      GraphicsData gmodel = window_map.get(name);
      if (gmodel == null) {
	 gmodel = new GraphicsData();
	 window_map.put(name,gmodel);
       }
      gmodel.update(gxml);
      done.add(name);
    }
   
   if (!errfg) {
      if (!have_complete || complete) {
         for (Iterator<String> it = window_map.keySet().iterator(); it.hasNext(); ) {
            String name = it.next();
            if (!done.contains(name)) it.remove();
          }
       }
    }
}




/********************************************************************************/
/*										*/
/*	Data for a particular graphics display					*/
/*										*/
/********************************************************************************/

static class GraphicsData implements DisplayModel {

   private String window_name;
   private int window_width;
   private int wirdow_height;
   private List<GraphicsCommand> window_commands;
   private Rectangle base_clip;
   private Map<Integer,Graphics2D> active_graphics;

   GraphicsData() {
      window_name = null;
      window_width = 0;
      wirdow_height = 0;
      window_commands = null;
      active_graphics = null;
    }

   @Override public int getWidth()		{ return window_width; }
   @Override public int getHeight()		{ return wirdow_height; }
   @Override public String getName()		{ return window_name; }
   Rectangle getBaseClip()			{ return base_clip; }

   @Override public boolean useTime() {
      return window_name.startsWith("MAIN_");
    }

   @Override public void paintToTime(Graphics2D g0,long when) {
      if (!useTime()) when = -1;
      active_graphics = new HashMap<>();
      active_graphics.put(0,g0);
      
      Graphics2D g = g0;
      base_clip = g.getClipBounds();
      for (GraphicsCommand gc : window_commands) {
         g = gc.getGraphics(g0,this);
         if (when < 0 || gc.getTime() <= when) {
            gc.paint(g,this);
          }
       }
    }

   void update(Element xml) {
      window_name = IvyXml.getAttrString(xml,"ID");
      window_width = IvyXml.getAttrInt(xml,"WIDTH");
      wirdow_height = IvyXml.getAttrInt(xml,"HEIGHT");
      window_commands = new ArrayList<>();
      for (Element ce : IvyXml.children(xml)) {
         GraphicsCommand cmd = null;
         if (IvyXml.isElement(ce,"DRAW")) {
            cmd = new DrawCommand(ce);
          }
         else if (IvyXml.isElement(ce,"FIELD")) {
            cmd = new FieldCommand(ce);
          }
         else if (IvyXml.isElement(ce,"INDEX")) {
            cmd = new IndexCommand(ce);
          }
         if (cmd != null) window_commands.add(cmd);
       }
    }

   Graphics2D getGraphicsForIndex(int idx,Graphics2D g0) {
      Graphics2D g1 = active_graphics.get(idx);
      if (g1 == null) {
         if (active_graphics.isEmpty()) active_graphics.put(0,g0);
         else g1 = (Graphics2D) g0.create();
         active_graphics.put(idx,g1);
       }
      return g1;
    }
   
}


/********************************************************************************/
/*										*/
/*	Executable graphics command						*/
/*										*/
/********************************************************************************/

static abstract class GraphicsCommand {

   private long command_time;

   protected GraphicsCommand(Element xml) {
      command_time = IvyXml.getAttrLong(xml,"TIME");
    }

   long getTime()				{ return command_time; }

   void  paint(Graphics2D g,GraphicsData gd)    { }
   
   Graphics2D getGraphics(Graphics2D g,GraphicsData gd)         { return g; }

}	// end of inner class GraphicsCommand



static class DrawCommand extends GraphicsCommand {

   private CommandType draw_type;
   private List<Object> command_args;

   DrawCommand(Element xml) {
      super(xml);
      draw_type = IvyXml.getAttrEnum(xml,"TYPE",CommandType.NONE);
      command_args = new ArrayList<>();
      for (Element ae : IvyXml.children(xml,"ARG")) {
         Object o = decodeArg(ae);
         command_args.add(o);
       }
    }

   @Override void paint(Graphics2D g,GraphicsData gd) {
      BoardLog.logD("BICEX","DRAW " + draw_type + " " + g.getClipBounds());
      switch (draw_type) {
	 case CLEAR_RECT :
	    g.clearRect(getIntArg(0),getIntArg(1),getIntArg(2),getIntArg(3));
	    break;
	 case COPY_AREA :
	    g.copyArea(getIntArg(0),getIntArg(1),getIntArg(2),getIntArg(3),
		  getIntArg(4),getIntArg(5));
	    break;
	 case DRAW :
	    g.draw((Shape) command_args.get(0));
	    break;
	 case DRAW_ARC :
	    g.drawArc(getIntArg(0),getIntArg(1),getIntArg(2),getIntArg(3),
		  getIntArg(4),getIntArg(5));
	    break;
	 case DRAW_OVAL :
	    g.drawOval(getIntArg(0),getIntArg(1),getIntArg(2),getIntArg(3));
	    break;
	 case DRAW_LINE :
	    g.drawLine(getIntArg(0),getIntArg(1),getIntArg(2),getIntArg(3));
	    break;
	 case DRAW_RECT :
	    g.drawRect(getIntArg(0),getIntArg(1),getIntArg(2),getIntArg(3));
	    break;
	 case DRAW_3D_RECT :
	    g.draw3DRect(getIntArg(0),getIntArg(1),getIntArg(2),getIntArg(3),
		  getBooleanArg(4));
	    break;
	 case DRAW_ROUND_RECT :
	    g.drawRoundRect(getIntArg(0),getIntArg(1),getIntArg(2),getIntArg(3),
		  getIntArg(4),getIntArg(5));
	    break;
	 case DRAW_STRING :
	    g.drawString(getStringArg(0),getFloatArg(1),getFloatArg(2));
	    break;
	 case FILL :
	    g.fill((Shape) command_args.get(0));
	    break;
	 case FILL_ARC :
	    g.fillArc(getIntArg(0),getIntArg(1),getIntArg(2),getIntArg(3),
		  getIntArg(4),getIntArg(5));
	    break;
	 case FILL_OVAL :
	    g.fillOval(getIntArg(0),getIntArg(1),getIntArg(2),getIntArg(3));
	    break;
	 case FILL_RECT :
	    g.fillRect(getIntArg(0),getIntArg(1),getIntArg(2),getIntArg(3));
	    break;
	 case FILL_ROUND_RECT :
	    g.fillRoundRect(getIntArg(0),getIntArg(1),getIntArg(2),getIntArg(3),
		  getIntArg(4),getIntArg(5));
	    break;
	 case FILL_3D_RECT :
	    g.fill3DRect(getIntArg(0),getIntArg(1),getIntArg(2),getIntArg(3),
		  getBooleanArg(4));
	    break;
	
	 case DRAW_GLYPH_VECTOR :
	 case DRAW_IMAGE :
	 case DRAW_POLYGON :
	 case DRAW_POLYLINE :
	 case DRAW_RENDERABLE_IMAGE :
	 case DRAW_RENDERED_IMAGE :
	 case FILL_POLYGON :
	 case NONE :
	    break;
	
	 case ROTATE :
	    if (command_args.size() == 1) {
	       g.rotate(getDoubleArg(0));
	     }
	    else {
	       g.rotate(getDoubleArg(0),getDoubleArg(1),getDoubleArg(2));
	     }
	    break;
	 case SCALE :
	    g.scale(getDoubleArg(0),getDoubleArg(1));
	    break;
	 case SHEAR :
	    g.shear(getDoubleArg(0),getDoubleArg(1));
	    break;
	 case TRANSFORM :
	    g.transform((AffineTransform) command_args.get(0));
	    break;
	 case TRANSLATE :
	    g.translate(getDoubleArg(0),getDoubleArg(1));
	    break;
	 case CONSTRAIN :
	    // want to do this at the top level using actual bounds for this display
	    try {
	       Class<?> clz = g.getClass();
	       Method mthd = clz.getMethod("constrain", int.class,int.class,int.class,int.class);
	       mthd.invoke(g, getIntArg(0),getIntArg(1),getIntArg(2),getIntArg(3));
	    }
	    catch (Throwable t) {
	       System.err.println("CHECK: " + t);
	    }
	    break;
	 case GET_TRANSFORM :
	 case SET_TRANSFORM :
	    break;
            
         case CLIP_RECT :
            g.clipRect(getIntArg(0),getIntArg(1),getIntArg(2),getIntArg(3));
            break;
         case CLIP :
            g.clip((Shape) command_args.get(0)); 
            break;
         case SET_CLIP :
            if (command_args.size() == 1) {
               g.setClip((Shape) command_args.get(0));
             }
            else {
               g.setClip(getIntArg(0),getIntArg(1),getIntArg(2),getIntArg(3));
             }
            break;
       }
    }

   private Object decodeArg(Element xml) {
      switch (IvyXml.getAttrString(xml,"TYPE")) {
         case "int" :
            return Integer.valueOf(IvyXml.getAttrInt(xml,"VALUE",0));
         case "boolean" :
            return Boolean.valueOf(IvyXml.getAttrBool(xml,"VALUE"));
         case "float" :
            return Float.valueOf(IvyXml.getAttrFloat(xml,"VALUE",0));
         case "double" :
            return Double.valueOf(IvyXml.getAttrDouble(xml,"VALUE",0));
         case "byte[]" :
            break;
         case "char[]" :
            break;
         case "java.awt.Image" :
            break;
         case "java.awt.ImageObserver" :
            break;
         case "int[]" :
            break;
         case "java.text.AttributedCharacterIterator" :
            break;
         case "java.lang.String" :
            return IvyXml.getTextElement(xml,"VALUE");
         case "java.awt.font.GlyphVector" :
            break;
         case "java.awt.image.BufferedImage" :
            break;
         case "java.awt.geom.AffineTransform" :
            Element telt = IvyXml.getChild(xml,"TRANSFORM");
            if (telt == null) telt = xml;
            return new AffineTransform(IvyXml.getAttrDouble(telt,"M00"),
        	     IvyXml.getAttrDouble(telt,"M10"),
        	     IvyXml.getAttrDouble(telt,"M01"),
        	     IvyXml.getAttrDouble(telt,"M11"),
        	     IvyXml.getAttrDouble(telt,"M02"),
        	     IvyXml.getAttrDouble(telt,"M12"));
         case "java.awt.Rectangle" :
            Element relt = IvyXml.getChild(xml,"RECT");
            if (relt == null) relt = xml;
            return new Rectangle(IvyXml.getAttrInt(relt,"X"),
                  IvyXml.getAttrInt(relt,"Y"),
                  IvyXml.getAttrInt(relt,"WIDTH"),
                  IvyXml.getAttrInt(relt,"HEIGHT"));
         case "java.awt.image.renderable.RenderableImage" :
            break;
         case "java.awt.image.RenderedImage" :
            break;
         case "sun.awt.image.ToolkitImage" :
            switch (IvyXml.getAttrString(xml,"KIND")) {
               case "URL" :
                  try {
                     URL u = new URL(IvyXml.getAttrString(xml,"PROTOCOL"),
                           IvyXml.getAttrString(xml,"HOST"),
                           IvyXml.getAttrInt(xml,"PORT"),
                           IvyXml.getAttrString(xml,"PATH"));
                     ImageIcon icn = new ImageIcon(u);
                     return icn.getImage();
                   }
                  catch (MalformedURLException e) { }
                  break;
               default :
                  break;
             }
            break;
         // need to handle other shapes
       }
      return null;
    }

   private int getIntArg(int idx) {
      return ((Number) command_args.get(idx)).intValue();
    }

   private float getFloatArg(int idx) {
      return ((Number) command_args.get(idx)).floatValue();
    }

   private double getDoubleArg(int idx) {
      return ((Number) command_args.get(idx)).doubleValue();
    }

   private boolean getBooleanArg(int idx) {
      return ((Boolean) command_args.get(idx)).booleanValue();
    }


   private String getStringArg(int idx) {
      return command_args.get(idx).toString();
    }

}	// end of inner class DrawCommand



static class FieldCommand extends GraphicsCommand {

   private FieldType field_type;
   private Object field_value;

   FieldCommand(Element xml) {
      super(xml);
      field_type = IvyXml.getAttrEnum(xml,"TYPE",FieldType.NONE);
      for (Element ce : IvyXml.children(xml)) {
	 field_value = decodeField(ce);
	 break;
       }
    }

   @Override void paint(Graphics2D g,GraphicsData gd) {
      if (field_value == null) return;
      BoardLog.logD("BICEX","FIELD " + field_type + " " + g.getClipBounds());

      switch (field_type) {
	 case PAINT :
	    g.setPaint((Paint) field_value);
	    break;
	 case FG :
	    g.setColor((Color) field_value);
	    break;
	 case BG :
	    g.setBackground((Color) field_value);
	    break;
	 case CLIP :
	    Shape r = (Shape) field_value;
	    Shape r1 = gd.getBaseClip();
	    Shape r2 = g.getTransform().createTransformedShape(r1);
	    try {
	       r1 = g.getTransform().createInverse().createTransformedShape(r1);
	    }
	    catch (Throwable t) { }
	    BoardLog.logD("BICEX","CLIPVALUE " + r1.getBounds() + " " + r2.getBounds() + " " + r +
                  " " + g.getClip());
	    // g.setClip(r1);
	    // g.clip(r);
	    // g.setClip(r);
	    break;
         case TRANSFORM :
            BoardLog.logD("BICEX","TRANSFORM " + g.getTransform() + " " + field_value);
            break;
	 case FONT :
	    g.setFont((Font) field_value);
	    break;
	 case COMPOSITE :
	    g.setComposite((Composite) field_value);
	    break;
	 case STROKE :
	    g.setStroke((Stroke) field_value);
	    break;
	 case HINTS :
	    break;
	 case NONE :
	    break;
       }
    }

   private Object decodeField(Element xml) {
      Object rslt = null;
      if (IvyXml.isElement(xml,"COLOR")) {
	 rslt = IvyXml.getAttrColor(xml,"VALUE");
       }
      else if (IvyXml.isElement(xml,"FONT")) {
	 float fsz = IvyXml.getAttrFloat(xml,"SIZE");
	 int intsz = (int) fsz;
	 rslt = new Font(IvyXml.getAttrString(xml,"NAME"),IvyXml.getAttrInt(xml,"STYLE"),
	       intsz);
	 if (intsz != fsz) {
	    rslt = ((Font) rslt).deriveFont(fsz);
	  }
       }
      else if (IvyXml.isElement(xml,"RECT")) {
	 rslt = new Rectangle(IvyXml.getAttrInt(xml,"X"),IvyXml.getAttrInt(xml,"Y"),
	       IvyXml.getAttrInt(xml,"WIDTH"),IvyXml.getAttrInt(xml,"HEIGHT"));
       }
      else if (IvyXml.isElement(xml,"ALPHACOMP")) {
	 rslt = AlphaComposite.getInstance(IvyXml.getAttrInt(xml,"RULE"),
	       IvyXml.getAttrFloat(xml,"ALPHA"));
       }
      else if (IvyXml.isElement(xml,"BASICSTROKE")) {
	 // handle stroke
       }
      else if (IvyXml.isElement(xml,"HINTS")) {
	 // handle hints
       }
      else if (IvyXml.isElement(xml,"TRANSFORM")) {
         rslt = new AffineTransform(IvyXml.getAttrDouble(xml,"M00"),IvyXml.getAttrDouble(xml,"M10"),
                  IvyXml.getAttrDouble(xml,"M01"),IvyXml.getAttrDouble(xml,"M11"), 
                  IvyXml.getAttrDouble(xml,"M02"),IvyXml.getAttrDouble(xml,"M12"));
      }

      return rslt;
    }

}	// end of inner class FieldCommand


static class IndexCommand extends GraphicsCommand {
   
   private int graphics_index;
   
   IndexCommand(Element xml) {
      super(xml);
      graphics_index = IvyXml.getAttrInt(xml,"VALUE");
    }
   
   @Override public Graphics2D getGraphics(Graphics2D g,GraphicsData gd) {
      return gd.getGraphicsForIndex(graphics_index,g);
    }
   
}       //end of innter class IndexCommand



}	// end of class BicexGraphicsModel




/* end of BicexGraphicsModel.java */

