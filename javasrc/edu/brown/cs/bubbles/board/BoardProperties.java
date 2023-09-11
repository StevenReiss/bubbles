/********************************************************************************/
/*										*/
/*		BoardProperties.java						*/
/*										*/
/*	Bubbles attribute and property management property handling		*/
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

import edu.brown.cs.ivy.file.IvyFileLocker;
import edu.brown.cs.ivy.swing.SwingColorSet;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


/**
 * This class is used to access various property sets used by Bubbles. The propertie are
 * stored on a per-user basis, generally in the directoy $(user.home)/.bubbles where
 * $(user.home) is the Java system property for "user.home". Each property set is stored
 * as XML data in its own file with the name of the property set. Property sets are shared
 * and can be edited and saved as needed. Property sets should be declared in the list in
 * BoardConstants.java and the default values should be placed in $BUBBLES/lib.
 **/

public class BoardProperties extends Properties implements BoardConstants {


/********************************************************************************/
/*										*/
/* Private Storage								*/
/*										*/
/********************************************************************************/

private String	prop_name;
private File	prop_file;

private static File			   base_directory;
private static Map<String,BoardProperties> loaded_properties;
private static String			   prop_base;
private static IvyFileLocker		   file_locker;


private static final long		   serialVersionUID = 1;



static {
   prop_base = System.getProperty("edu.brown.cs.bubbles.BASE");
   if (prop_base == null) prop_base = System.getenv("BUBBLES_PROPBASE");
   if (prop_base == null) {
      prop_base = System.getProperty("user.home") + File.separator + BOARD_PROP_BASE;
    }

   base_directory = new File(prop_base);
   if (!base_directory.exists()) base_directory.mkdir();
   loaded_properties = new HashMap<String,BoardProperties>();
   file_locker = new IvyFileLocker(base_directory);
}



/********************************************************************************/
/*										*/
/*	Property Set Access methods						*/
/*										*/
/********************************************************************************/

/**
 * Return the property set with the given name. System properties have the name "System".
 * Other packages in bubbles can define their own property sets as needed.
 **/

public synchronized static BoardProperties getProperties(String id)
{
   if (id.endsWith(".props")) {
      int idx = id.lastIndexOf(".");
      id = id.substring(0,idx);
    }

   BoardProperties bp = loaded_properties.get(id);
   if (bp == null) {
      bp = new BoardProperties(id);
      loaded_properties.put(id, bp);
    }

   return bp;
}


/********************************************************************************/
/*										*/
/*	Library access methods							*/
/*										*/
/********************************************************************************/

/**
 * Return an input stream for a library file with the given name. If no such file/resource
 * exists, returns null.
 **/

public static InputStream getResourceFile(String name)
{
   // allow user version in ~/.bubbles

   String pname = name.replace("/",File.separator);

   File dir0 = new File(prop_base);
   if (dir0.exists()) {
      File f3 = new File(dir0,pname);
      if (f3.exists() && f3.canRead()) {
	 try {
	    return new FileInputStream(f3);
	  }
	 catch (IOException e) {}
       }
    }

   BoardProperties sp = getProperties("System");
   String dir = sp.getProperty(BOARD_PROP_INSTALL_DIR);

   URL url = BoardProperties.class.getClassLoader().getResource(BOARD_RESOURCE_CHECK);

   // use jar version if available
   if (url != null) {
      String nm = "resources/" + name;
      InputStream ins = BoardProperties.class.getClassLoader().getResourceAsStream(nm);
      if (ins != null) return ins;
      dir = sp.getProperty(BOARD_PROP_JAR_DIR);
    }

   // use whats in the resources directory
   File f1 = new File(dir);
   File f2 = new File(f1,"resources");
   File f3 = new File(f2,pname);

   try {
      return new FileInputStream(f3);
    }
   catch (IOException e) { }

   return null;
}


/********************************************************************************/
/*										*/
/*	Property reset methods							*/
/*										*/
/********************************************************************************/

/**
 * Restore all the properties to their initial default. This may not change properties
 * that are currently active or that are read at initialization. These changes will take
 * effect at the next restart.
 **/

public synchronized static void resetDefaultProperties()
{
   BoardSetup setup = BoardSetup.getSetup();

   setup.resetProperties();

   for (Map.Entry<String,BoardProperties> ent : loaded_properties.entrySet()) {
      String id = ent.getKey();
      if (id.equals("System")) continue;
      BoardProperties bp = ent.getValue();
      bp.clear();
      bp.loadProperties(id);
    }
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BoardProperties(String id)
{
   prop_name = id;
   prop_file = null;

   loadProperties(id);
}



BoardProperties(InputStream ins) throws IOException
{
   prop_name = null;
   prop_file = null;

   loadFromXML(ins);
   ins.close();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

/**
 * Return the nae of this property set
 **/

public String getName()
{
   return prop_name;
}



/**
 *	Set the directory containing user property files
 **/

public static void setPropertyDirectory(String dir)
{
   if (dir == null) return;
   
   setPropertyDirectory(new File(dir));
}


public static void setPropertyDirectory(File base)
{
   if (!base.exists()) {
      if (!base.mkdirs()) {
	 BoardLog.logE("BOARD", "Unable to use user property directory " + base);
	 return;
       }
    }

   base_directory = base;
   prop_base = base.getPath();
   System.setProperty("edu.brown.cs.bubbles.BASE",prop_base);

   loaded_properties = new HashMap<>();

   BoardLog.logD("BOARD","Set property directory " + base);
}


public static File getPropertyDirectory()
{
   return base_directory;
}




/********************************************************************************/
/*										*/
/*	Value access methods							*/
/*										*/
/********************************************************************************/

/**
 * Return the value of the boolean property with the given name.
 **/

public boolean getBoolean(String prop)
{
   return getBoolean(prop, false);
}


/**
 * Return the value of the boolean property with the given name or, if it doesn't exist,
 * the given default value.
 **/

public boolean getBoolean(String prop,boolean dflt)
{
   String s = getProperty(prop);
   if (s != null && s.length() > 0) {
      char ch = s.charAt(0);
      return "tT1yY".indexOf(ch) >= 0;
    }
   return dflt;
}


/**
 * Returns the value of the string property with the given name or, if it doesn't exist,
 * the given default is returned.
 **/

public String getString(String prop,String dflt)
{
   String s = getProperty(prop);
   if (s == null) s = dflt;
   return s;
}



/**
 * Returns the value of the string property with the given name or, if it doesn't exist,
 * an empty string. This is here for consistency and simply replicates the functionality
 * of getProperty(String)
 **/

public String getString(String prop)
{
   String s = getProperty(prop);
   if (s != null) {
      return s;
    }
   return "";
}



/**
 * Return the value of the integer property with the given name or -1 if it doesn't exist.
 **/

public int getInt(String prop)
{
   return getInt(prop, -1);
}


/**
 * Return the value of the integer property with the given name or the given default value
 * if it doesn't.
 **/

public int getInt(String prop,int dflt)
{
   String s = getProperty(prop);
   if (s != null && s.length() > 0) {
      try {
	 return Integer.parseInt(s);
       }
      catch (NumberFormatException e) {}
    }
   return dflt;
}


/**
 * Return the value of the integer property with the given name or -1 if it doesn't exist.
 **/

public long getLong(String prop)
{
   return getLong(prop, -1);
}


/**
 * Return the value of the integer property with the given name or the given default value
 * if it doesn't.
 **/

public long getLong(String prop,long dflt)
{
   String s = getProperty(prop);
   if (s != null && s.length() > 0) {
      try {
	 return Long.parseLong(s);
       }
      catch (NumberFormatException e) {}
    }
   return dflt;
}


/**
 * Return the value of the float property with the given name or -1 if it doesn't exist.
 **/

public float getFloat(String prop)
{
   return getFloat(prop, -1);
}


/**
 * Return the value of the integer property with the given name or the given default value
 * if it doesn't.
 **/

public float getFloat(String prop,float dflt)
{
   String s = getProperty(prop);
   if (s != null && s.length() > 0) {
      try {
	 return Float.parseFloat(s);
       }
      catch (NumberFormatException e) {}
    }
   return dflt;
}


/**
 * Return the value of the float property with the given name or -1 if it doesn't exist.
 **/

public double getDouble(String prop)
{
   return getDouble(prop, -1);
}


/**
 * Return the value of the integer property with the given name or the given default value
 * if it doesn't.
 **/

public double getDouble(String prop,double dflt)
{
   String s = getProperty(prop);
   if (s != null && s.length() > 0) {
      try {
	 return Double.parseDouble(s);
       }
      catch (NumberFormatException e) {}
    }
   return dflt;
}


/**
 * Return the value of the color with the given name or black if it doesn't exist
 **/

public Color getColor(String prop)
{
   return getColor(prop, Color.black);
}


/**
 * Return the value of the color with the given name or the default value if it doesn't
 * exist
 **/

public Color getColor(String prop,Color dflt)
{
   String s = getProperty(prop);

   Color c = SwingColorSet.getColorByName(s);
   if (c == null) c = dflt;

   return c;
}



/**
 * Return the value of the font with the given name, or a serif size 12 font if it doesn't
 * exist
 **/

public Font getFont(String prop)
{
   return getFont(prop,BoardFont.getFont("Serif",Font.PLAIN,12));
}


/**
 * Return the value of the font with the given name, or the given default if it doesn't
 * exist
 **/

public Font getFont(String prop,Font dflt)
{
   String s = getProperty(prop);
   if (s == null) return dflt;
   String[] split = s.split("/");
   String fam = split[0];
   int md = dflt.getStyle();
   if (split.length >= 2) md = Integer.parseInt(split[1]);
   int sz = dflt.getSize();
   if (split.length >= 3) sz = Integer.parseInt(split[2]);
   return BoardFont.getFont(fam,md,sz);
}



/**
 *	Return the value of an enum of the given type.	Prefix allows a shortened
 *	form of the enum
 **/

@SuppressWarnings("unchecked")
public <T extends Enum<T>> T getEnum(String prop,String prefix,T dflt)
{
   Enum<?> v = dflt;
   String s = getProperty(prop);
   if (s == null || s.length() == 0) return dflt;
   if (prefix != null && !s.startsWith(prefix)) s = prefix + s;
   Object [] vals = dflt.getClass().getEnumConstants();
   if (vals == null) return dflt;
   for (int i = 0; i < vals.length; ++i) {
      Enum<?> e = (Enum<?>) vals[i];
      if (e.name().equalsIgnoreCase(s)) {
	 v = e;
	 break;
       }
    }

   return (T) v;
}



/********************************************************************************/
/*										*/
/*	Options values access methods -- recorded as user options		*/
/*										*/
/********************************************************************************/

/**
 * Return the value of the boolean property with the given name.
 **/

public boolean getBooleanOption(String prop)
{
   return getBooleanOption(prop, false);
}


/**
 * Return the value of the boolean property with the given name or, if it doesn't exist,
 * the given default value.
 **/

public boolean getBooleanOption(String prop,boolean dflt)
{
   String s = getOptionProperty(prop);
   if (s != null && s.length() > 0) {
      char ch = s.charAt(0);
      return "tT1yY".indexOf(ch) >= 0;
    }

   return dflt;
}


/**
 * Returns the value of the string property with the given name or, if it doesn't exist,
 * an empty string. This is here for consistency and simply replicates the functionality
 * of getProperty(String)
 **/

public String getStringOption(String prop)
{
   String s = getOptionProperty(prop);
   if (s != null) {
      return s;
    }
   return "";
}


/**
 * Return the value of the integer property with the given name or -1 if it doesn't exist.
 **/

public int getIntOption(String prop)
{
   return getIntOption(prop, -1);
}


/**
 * Return the value of the integer property with the given name or the given default value
 * if it doesn't.
 **/

public int getIntOption(String prop,int dflt)
{
   String s = getOptionProperty(prop);
   if (s != null && s.length() > 0) {
      try {
	 return Integer.parseInt(s);
       }
      catch (NumberFormatException e) {}
    }
   return dflt;
}


/**
 * Return the value of the color with the given name or black if it doesn't exist
 **/

public Color getColorOption(String prop)
{
   return getColorOption(prop, Color.black);
}


/**
 * Return the value of the color with the given name or the default value if it doesn't
 * exist
 **/

public Color getColorOption(String prop,Color dflt)
{
   String s = getOptionProperty(prop);
   if (s != null) {
      Color rslt = SwingColorSet.getColorByName(s);
      if (rslt != null) return rslt;
    }

   return dflt;
}


/**
 * Return the value of the font with the given name, or a serif size 12 font if it doesn't
 * exist
 **/

public Font getFontOption(String prop)
{
   return getFontOption(prop,BoardFont.getFont("Serif",Font.PLAIN,12));
}


/**
 * Return the value of the font with the given name, or the given default if it doesn't
 * exist
 **/

public Font getFontOption(String prop,Font dflt)
{
   String s = getOptionProperty(prop);
   if (s == null) return dflt;

   String[] split = s.split("/");
   return BoardFont.getFont(split[0],Integer.parseInt(split[1]),Integer.parseInt(split[2]));
}




private String getOptionProperty(String prop)
{
   return super.getProperty(prop);
}


/********************************************************************************/
/*										*/
/*	Value setting methods							*/
/*										*/
/********************************************************************************/

/**
 * Set the value of the given property to the given boolean value.
 **/

public void setProperty(String prop,boolean val)
{
   setProperty(prop, Boolean.toString(val));
}


/**
 * Set the value of the given property to the given integer value.
 **/

public void setProperty(String prop,int val)
{
   setProperty(prop, Integer.toString(val));

}


/**
 * Set the value of a property to a color value.
 **/

public void setProperty(String prop,Color val)
{
   setProperty(prop, Integer.toHexString(val.getRGB()));
}


/**
 * Set the value of a property to a font value
 **/

public void setProperty(String prop,Font val)
{
   setProperty(prop, val.getFamily() + "/" + val.getStyle() + "/" + val.getSize());
}



@Override public synchronized Object setProperty(String key,String value)
{
   if (key == null) return null;
   if (value == null) return remove(key);
   else return super.setProperty(key,value);
}




/********************************************************************************/
/*										*/
/*	Default setting methods 						*/
/*										*/
/********************************************************************************/

/**
 * Set the default value of the given property to the given boolean value.
 **/

public void setDefaultProperty(String prop,boolean val)
{
   String s = getProperty(prop);

   if (s == null) setProperty(prop, Boolean.toString(val));
}


/**
 * Set the default value of the given property to the given integer value.
 **/

public void setDefaultProperty(String prop,int val)
{
   String s = getProperty(prop);
   if (s == null) setProperty(prop, Integer.toString(val));
}


/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

/**
 * Store the current set of properties in the local cache for the current user.
 **/

public void save() throws IOException
{
   BoardLog.logD("BOARD","Save properties " + prop_file);

   file_locker.lock();
   try {
      FileOutputStream fos = new FileOutputStream(prop_file);
      storeToXML(fos, null);
      fos.close();
    }
   catch (IOException e) {
      BoardLog.logE("BOARD","Problem storing properties",e);
      throw e;
    }
   catch (Throwable t) {
      BoardLog.logE("BOARD","Problem storing properties for " + this,t);
    }
   finally {
      file_locker.unlock();
    }
}



/********************************************************************************/
/*										*/
/*	Property loading from resource files					*/
/*										*/
/********************************************************************************/

private void loadProperties(String id)
{
   // lock and unlock properties
   file_locker.lock();
   try {
      prop_file = loadIfPossible(id);
      loadIfPossible(id + "." + BOARD_ARCH);

      if (id.equals("System") || id.equals("Board")) return;       // these need to be inited before we can use them

      String s = BoardSetup.getSetup().getLanguage().toString().toLowerCase();
      int idx = s.indexOf("_");
      if (idx > 0) s = s.substring(0,idx);
      loadIfPossible(id + "." + s);
      loadIfPossible(id + "." + s + "." + BOARD_ARCH);

      String cnm = BoardSetup.getSetup().getCourseName();
      if (cnm != null) {
	 loadIfPossible(id + "." + cnm);
       }
      String anm = BoardSetup.getSetup().getCourseAssignment();
      if (anm != null) {
	 loadIfPossible(id + "." + cnm + "." + anm);
	 loadIfPossible(id + "." + cnm + "@" + anm);
       }
    }
   finally {
      file_locker.unlock();
    }
}



private File loadIfPossible(String id)
{
   File f1 = new File(base_directory,id);
   if (!loadFileProps(f1)) {
      f1 = new File(base_directory,id + ".props");
      loadFileProps(f1);
    }
   return f1;
}



private boolean loadFileProps(File f)
{
   if (!f.exists() || !f.canRead()) return false;

   try {
      FileInputStream ins = new FileInputStream(f);
      loadFromXML(ins);
      prop_file = f;
      return true;
    }
   catch (IOException e) {
      BoardLog.logE("BOARD", "Problem reading property file: " + e);
    }

   return false;
}



} // end of class BoardProperties



/* end of BoardProperties.java */
