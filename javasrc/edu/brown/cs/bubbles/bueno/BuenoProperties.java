/********************************************************************************/
/*										*/
/*		BuenoProperties.java						*/
/*										*/
/*	BUbbles Environment New Objects creator property handling		*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.bueno;

import edu.brown.cs.bubbles.bump.BumpClient;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

public class BuenoProperties extends HashMap<String,Object> implements BuenoConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public BuenoProperties()
{
   put(BuenoKey.KEY_AUTHOR,System.getProperty("user.name"));
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public String getStringProperty(BuenoKey k)
{
   return getStringProperty(k.toString());
}


public String getStringProperty(String k)
{
   Object v = get(k);
   if (v == null) return null;

   return v.toString();
}


public String getProjectName()
{
   return getStringProperty(BuenoKey.KEY_PROJECT);
}


public String getClassName()
{
   return getStringProperty(BuenoKey.KEY_CLASS_NAME);
}


public String getFieldName()
{
   return getStringProperty(BuenoKey.KEY_FIELD_NAME);
}

public String getPackageName()
{
   return getStringProperty(BuenoKey.KEY_PACKAGE);
}


public boolean getBooleanProperty(BuenoKey k)
{
   return getBooleanProperty(k.toString());
}


public boolean getBooleanProperty(String k)
{
   Object v = get(k);
   if (v == null) return false;

   if (v instanceof Boolean) return ((Boolean) v);

   return true;
}


public File getFile(BuenoKey k)
{
   return getFile(k.toString());
}

public File getFile(String k)
{
   Object o = get(k);
   if (o == null) return null;
   else if (o instanceof File) return (File) o;
   else if (o instanceof String) return new File(o.toString());
   return null;
}


public int getModifiers()
{
   Object v = get(BuenoKey.KEY_MODIFIERS);
   if (v == null) return 0;
   if (v instanceof Integer) return ((Integer) v);

   // allow string or set here and decode

   return 0;
}

public String getModifierString()
{
   StringBuffer buf = new StringBuffer();
   int mods = getModifiers();
   addModifier(buf,"@Override",(mods & MODIFIER_OVERRIDES) != 0);
   addModifier(buf,"private",Modifier.isPrivate(mods));
   addModifier(buf,"public",Modifier.isPublic(mods));
   addModifier(buf,"protected",Modifier.isProtected(mods));
   addModifier(buf,"abstract",Modifier.isAbstract(mods));
   addModifier(buf,"static",Modifier.isStatic(mods));
   addModifier(buf,"native",Modifier.isNative(mods));
   addModifier(buf,"final",Modifier.isFinal(mods));
   addModifier(buf,"strictfp",Modifier.isStrict(mods));
   addModifier(buf,"synchronized",Modifier.isSynchronized(mods));
   addModifier(buf,"transient",Modifier.isTransient(mods));
   addModifier(buf,"volatile",Modifier.isVolatile(mods));

   return buf.toString();
}


private void addModifier(StringBuffer buf,String txt,boolean fg)
{
   if (fg) {
      buf.append(txt);
      buf.append(" ");
    }
}


public String [] getParameters()	{ return getArrayProperty(BuenoKey.KEY_PARAMETERS); }
public String [] getExtends()		{ return getArrayProperty(BuenoKey.KEY_EXTENDS); }
public String [] getImplements()	{ return getArrayProperty(BuenoKey.KEY_IMPLEMENTS); }
public String [] getThrows()		{ return getArrayProperty(BuenoKey.KEY_THROWS); }
public String [] getImports()		{ return getArrayProperty(BuenoKey.KEY_IMPORTS); }


public String [] getArrayProperty(BuenoKey k)
{
   return getArrayProperty(k.toString());
}


public String [] getArrayProperty(String k)
{
   Object v = get(k);
   if (v == null) return null;

   if (v instanceof String []) return ((String []) v);

   if (v instanceof String) {
      StringTokenizer tok = new StringTokenizer((String) v,",");
      String [] rslt = new String[tok.countTokens()];
      int idx = 0;
      while (tok.hasMoreTokens()) {
	 rslt[idx++] = tok.nextToken();
       }
      return rslt;
    }

   if (v instanceof List<?>) {
      List<?> l = (List<?>) v;
      String [] rslt = new String[l.size()];
      int idx = 0;
      for (Object o : l) {
	 rslt[idx++] = o.toString();
       }
      return rslt;
    }

   return null;
}


public void addToArrayProperty(BuenoKey k,String v)
{
   addToArrayProperty(k.toString(),v);
}


public void addToArrayProperty(String k,String v)
{
   List<String> rslt = null;
   Object ov = get(k);
   if (ov == null) {
      rslt = new ArrayList<String>();
    }
   else if (ov instanceof String []) {
      rslt = new ArrayList<String>();
      String [] xv = (String []) ov;
      for (String s : xv) rslt.add(s);
    }
   else if (ov instanceof String) {
      rslt = new ArrayList<String>();
      StringTokenizer tok = new StringTokenizer((String) ov,",");
      while (tok.hasMoreTokens()) {
	 rslt.add(tok.nextToken());
       }
    }
   else if (ov instanceof List) {
      @SuppressWarnings("unchecked") List<String> l = (List<String>) ov;
      l.add(v);
    }
   if (rslt != null) {
      rslt.add(v);
      put(k,rslt);
    }
}



String getIndentString()		{ return getIndentProperty(BuenoKey.KEY_INDENT,-1); }
String getInitialIndentString() 	{ return getIndentProperty(BuenoKey.KEY_INITIAL_INDENT,0); }

String getIndentProperty(BuenoKey k,int dflt)
{
   return getIndentProperty(k.toString(),dflt);
}


String getIndentProperty(String k,int dflt)
{
   Object v = get(k);

   if (v != null && v instanceof String) return (String) v;

   int idx = 0;
   if (v instanceof Integer) idx = ((Integer) v);
   else if (dflt >= 0) idx = dflt;
   else {
      idx = BumpClient.getBump().getOptionInt("org.eclipse.jdt.core.formatter.indentation.size");
    }

   StringBuffer buf = new StringBuffer();
   for (int i = 0; i < idx; ++i) buf.append(" ");
   return buf.toString();
}


String getFullText()
{
   String ft = getStringProperty(BuenoKey.KEY_FULLTEXT); 
   if (ft == null || ft.isEmpty()) return null;
   
   return ft;
}


/********************************************************************************/
/*										*/
/*	Key access methods							*/
/*										*/
/********************************************************************************/

public void put(BuenoKey k,Object v)
{
   super.put(k.toString(),v);
}


public Object get(BuenoKey k)
{
   return get(k.toString());
}


public Object remove(BuenoKey k)
{
   return remove(k.toString());
}


}	// end of class BuenoProperties




/* end of BuenoProperties.java */
