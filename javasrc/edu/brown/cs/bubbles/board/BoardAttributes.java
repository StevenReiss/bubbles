/********************************************************************************/
/*										*/
/*		BoardAttributes.java						*/
/*										*/
/*	Bubbles attribute and property management attribute handling		*/
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

import edu.brown.cs.ivy.swing.SwingColorSet;

import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;



/**
 *	This class provides a means for storing an AttributeSet as a property
 *	set and hence in a property file managed by BoardProperties.  It provides
 *	the facilities needed to convert the various attributes from strings in
 *	the properties files to objects of the correct types.  This includes a
 *	full range of colors (using the X11 color names, enumerations, numbers,
 *	TabSets, and alignments.
 *
 *	It actually maintains, for each BoardAttributes object, a set of
 *	different AttributeSets indexed by String id.  This allows the set to
 *	be used to describe a variety of attributes (e.g. the properties of each
 *	type of element for text display).
 *
 *	The AttributeSet objects returned from here are mutable.  Hence users
 *	should request a separate copy (by creating a new BoardAttributes object)
 *	if this is going to be a problem.
 *
 **/

public class BoardAttributes implements BoardConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BoardProperties property_set;
private String base_id;
private Map<String,AttributeSet> attr_sets;

private static Map<String,Object> key_map;
private static Map<String,Object> type_map;
private static Map<String,Integer> align_map;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

/**
 *	Create a new attribute meta set from the property file identified by 'id'.
 **/

public BoardAttributes(String id)
{
   base_id = id;
   property_set = BoardProperties.getProperties(id);
   attr_sets = new HashMap<String,AttributeSet>();

   loadAttributes();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

/**
 *	Return the actual attribute set for a given type.
 **/

public AttributeSet getAttributes(String id)
{
   return attr_sets.get(id.toLowerCase());
}





public void reload()
{
   attr_sets = new HashMap<String,AttributeSet>();
   loadAttributes();
}




/********************************************************************************/
/*										*/
/*	Loading methods 							*/
/*										*/
/********************************************************************************/

private void loadAttributes()
{
   for (String key : property_set.stringPropertyNames()) {
      int idx = key.indexOf(".");
      if (idx >= 0) {
	 String id = key.substring(0,idx);
	 // if (id.equals(base_id)) continue;
	 setupSet(id);
       }
    }
}



private AttributeSet setupSet(String id)
{
   String lid = id.toLowerCase();

   AttributeSet as = attr_sets.get(lid);
   if (as != null) {
      if (as == SimpleAttributeSet.EMPTY) {
	 BoardLog.logE("BOARD","Attribute sets are cyclic");
	 System.exit(1);
       }
      return as;
    }

   // prevent infinite recursion
   attr_sets.put(lid,SimpleAttributeSet.EMPTY);

   AttributeSet pas = null;
   String par = property_set.getProperty(id + "." + BOARD_ATTR_PARENT);
   if (par != null) pas = setupSet(par);

   SimpleAttributeSet sas = new SimpleAttributeSet();
   if (pas != null) sas.setResolveParent(pas);
   attr_sets.put(lid,sas);

   for (String pnm : property_set.stringPropertyNames()) {
      if (pnm.toLowerCase().startsWith(lid + ".")) {
	 int idx = pnm.indexOf(".");
	 String anm = pnm.substring(idx+1).toLowerCase();
	 if (anm.equals(BOARD_ATTR_PARENT)) continue;
         if (anm.contains(".")) continue;
	 Object akey = key_map.get(anm);
	 if (akey == null) akey = anm;
	 String v = property_set.getProperty(pnm);
	 Object typ = type_map.get(anm);
	 Object aval = v;
	 if (typ != null) aval = convertType(typ,v,pnm);

	 sas.addAttribute(akey,aval);
       }
    }

   return sas;
}




private Object convertType(Object typ,String v,String pnm)
{
   if (typ == Boolean.class || typ.equals("Boolean")) {
      if (v != null && v.length() > 0 && "tTyY1".indexOf(v.charAt(0)) >= 0) return Boolean.TRUE;
      return Boolean.FALSE;
    }
   else if (typ == Integer.class || typ.equals("Integer")) {
      try {
	 return Integer.valueOf(v);
       }
      catch (NumberFormatException e) {
	 BoardLog.logE("BOARD","bad integer attribute value: " + e);
       }
      return null;
    }
   else if (typ == Float.class || typ.equals("Float")) {
      try {
	 return Float.valueOf(v);
       }
      catch (NumberFormatException e) {
	 BoardLog.logE("BOARD","bad floating attribute value: " + e);
       }
      return null;
    }
   else if (typ == Color.class || typ.equals("Color")) {
      Color c = null;
      if (v.equals("^")) {
	 v = base_id + ".attr." + pnm.toLowerCase();
	 c = BoardColors.getColor(v);
       }
      else if (v.startsWith("^")) {
	 v = v.substring(1);
	 c = BoardColors.getColor(v);
       }
      else {
	 c = SwingColorSet.getColorByName(v);
       }
      return c;
    }
   else if (typ instanceof Class<?> && ((Class<?>) typ).isEnum()) {
      Class<?> c = (Class<?>) typ;
      Object [] ecs = c.getEnumConstants();
      for (int i = 0; i < ecs.length; ++i) {
	 if (ecs[i].toString().toLowerCase().equals(v.toLowerCase())) return ecs[i];
       }
      return null;
    }
   else if (typ.equals("TABSET")) {
      List<TabStop> stops = new ArrayList<TabStop>();
      try {
	 StringTokenizer tok = new StringTokenizer(v,",; ");
	 while (tok.hasMoreTokens()) {
	    int loc = Integer.parseInt(tok.nextToken());
	    // TODO: need to adjust for font size
	    stops.add(new TabStop(loc));
	  }
	 TabStop [] stopa = new TabStop[stops.size()];
	 stopa = stops.toArray(stopa);
	 return new TabSet(stopa);
       }
      catch (NumberFormatException e) {
	 BoardLog.logE("BOARD","bad tab set attribute value: " + v);
       }
      return null;
    }
   else if (typ.equals("ALIGN")) {
      return align_map.get(v.toLowerCase());
    }
   else {
      BoardLog.logE("BOARD","Unknown attribute type " + typ);
    }
   return null;
}




/********************************************************************************/
/*										*/
/*	Routines to save updated attribute sets 				*/
/*										*/
/********************************************************************************/

/**
 *	This saves the suite of attribute sets back in their original file.
 *
 *	NOTE: NOT YET IMPLEMENTED
 **/

public void save()
{
   //TODO: implement save
}


/********************************************************************************/
/*										*/
/*	Static definitions for attributes and their properies			*/
/*										*/
/********************************************************************************/

static {
   key_map = new HashMap<String,Object>();
   type_map = new HashMap<String,Object>();
   align_map = new HashMap<String,Integer>();

   key_map.put("fontfamily",StyleConstants.FontFamily);
   key_map.put("family",StyleConstants.Family);
   key_map.put("fontsize",StyleConstants.FontSize);
   type_map.put("fontsize",Integer.class);
   key_map.put("size",StyleConstants.Size);
   type_map.put("size",Integer.class);
   key_map.put("bold",StyleConstants.Bold);
   type_map.put("bold",Boolean.class);
   key_map.put("italic",StyleConstants.Italic);
   type_map.put("italic",Boolean.class);
   key_map.put("underline",StyleConstants.Underline);
   type_map.put("underline",Boolean.class);
   key_map.put("strikethrough",StyleConstants.StrikeThrough);
   type_map.put("strikethrough",Boolean.class);
   key_map.put("superscript",StyleConstants.Superscript);
   type_map.put("superscript",Boolean.class);
   key_map.put("subscript",StyleConstants.Subscript);
   type_map.put("subscript",Boolean.class);
   key_map.put("foreground",StyleConstants.Foreground);
   type_map.put("foreground",Color.class);
   key_map.put("background",StyleConstants.Background);
   type_map.put("background",Color.class);
   key_map.put("firstlineindent",StyleConstants.FirstLineIndent);
   type_map.put("firstlineindent",Float.class);
   key_map.put("rightindent",StyleConstants.RightIndent);
   type_map.put("rightindent",Float.class);
   key_map.put("leftindent",StyleConstants.LeftIndent);
   type_map.put("leftindent",Float.class);
   key_map.put("linespacing",StyleConstants.LineSpacing);
   type_map.put("linespacing",Float.class);
   key_map.put("spaceabove",StyleConstants.SpaceAbove);
   type_map.put("spaceabove",Float.class);
   key_map.put("spacebelow",StyleConstants.SpaceBelow);
   type_map.put("spacebelow",Float.class);
   key_map.put("alignment",StyleConstants.Alignment);
   type_map.put("alignment","ALIGN");
   key_map.put("tabset",StyleConstants.TabSet);
   type_map.put("tabset","TABSET");
   key_map.put("tabsize",BOARD_ATTR_TAB_SIZE);
   type_map.put("tabsize",Integer.class);
   key_map.put("highlightstyle",BOARD_ATTR_HIGHLIGHT_STYLE);
   type_map.put("highlightstyle",BoardHighlightStyle.class);
   key_map.put("highlightcolor",BOARD_ATTR_HIGHLIGHT_COLOR);
   type_map.put("highlightcolor",Color.class);

   align_map.put("left",StyleConstants.ALIGN_LEFT);
   align_map.put("center",StyleConstants.ALIGN_CENTER);
   align_map.put("right",StyleConstants.ALIGN_RIGHT);
   align_map.put("justified",StyleConstants.ALIGN_JUSTIFIED);
}




}	// end of class BoardAttributes




/* end of BoardAttributes.java */
