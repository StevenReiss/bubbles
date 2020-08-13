/********************************************************************************/
/*										*/
/*		BassNameBase.java						*/
/*										*/
/*	Bubble Augmented Search Strategies name base implementation		*/
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bass;

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.bump.BumpConstants;
import edu.brown.cs.bubbles.bump.BumpLocation;

import java.util.Collection;

import javax.swing.Icon;


/**
 *	This class provides a default implementation of BassConstants.BassName that
 *	should simplify implementations.  In particular, it provides methods for
 *	putting together the various strings that are needed by BassName from
 *	simpler components.
 **/

abstract public class BassNameBase implements BassName, BassConstants, BumpConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

/**
 *	The type of object that this name represents.  This is generally set by
 *	the subclass constructor.
 **/
protected BassNameType	name_type;

private   String	full_name;
private   String	param_name;

private static final String SPLIT_PATTERN = "[.]";



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

/**
 *	Default constructor.
 **/
protected BassNameBase()
{
   name_type = BassNameType.NONE;
   full_name = null;
   param_name = null;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override abstract public String getProject();

/**
 *	Return the local name of the symbol.  This should be a fully qualified name, but
 *	does not include parameters for methods.
 **/

abstract protected String getSymbolName();


/**
 *	Return the parameter list for a method, null for anything else.  The parameter
 *	list is given in the form of '(type,type,...,type)', where the types should be
 *	fully qualified if possible.
 **/

abstract protected String getParameters();



/**
 *	Return a unique string that identifies this symbol.  Two symbols are considered
 *	the same if they have the same key.
 **/

abstract protected String getKey();


@Override public BassNameType getNameType()	{ return name_type; }

@Override public int getModifiers()		{ return BASS_MODIFIERS_UNDEFINED; }

@Override public int getSortPriority()		{ return BASS_DEFAULT_SORT_PRIORITY; }


/**
 *	Returns the symbol name that is shown to the user.  This might differ from the
 *	internal symbol name.  For example, in Java, nested classes are separated by
 *	a dollar sign rather than a dot.
 **/

protected String getUserSymbolName()
{
   String s = getSymbolName();
   s = s.replace('$','.');

   return s;
}




/**
 *	Return the local name, i.e. the tail part of the name without any qualifications.
 **/

protected String getLocalName()
{
   String nm = getSymbolName();
   int idx = nm.lastIndexOf(".");
   if (idx >= 0) return nm.substring(idx+1);

   return nm;
}


@Override public String getName()		  { return getUserSymbolName(); }


/**
 *	Return the enclosing name.  This is the full name minus the local name, e.g.
 *	for a method or field it is the name of the enclosing class and for a class it
 *	is the name of the enclosing package.
 **/

@Override public String getNameHead()
{
   String nm = getUserSymbolName();

   int idx = nm.lastIndexOf(".");
   if (idx >= 0) return nm.substring(0,idx);

   return null;
}


/**
 *	Return the name of the associated class.  This may either be the name of the
 *	symbol if the symbol is a type, or it can be the name of the enclosing class for
 *	a method or field.
 **/

@Override public String getClassName()
{
   switch (name_type) {
      case NONE :
      case PACKAGE :
      case MODULE :
	 break;
      case CLASS :
      case INTERFACE :
      case ENUM :
      case THROWABLE :
      case ANNOTATION :
	 String nm1 = getSymbolName();
	 nm1 = nm1.replace('$','.');
	 return nm1;
      case FIELDS :
      case METHOD :
      case CONSTRUCTOR :
      case STATICS :
      case MAIN_PROGRAM :
	 String nm = getSymbolName();
	 int idx1 = nm.lastIndexOf(".");
	 if (idx1 <= 0) return null;
	 int idx2 = nm.lastIndexOf(".",idx1-1);
	 String cnm = nm.substring(idx2+1,idx1);
	 cnm = cnm.replace('$','.');
	 return cnm;
      default:
	 break;
    }

   return null;
}



@Override public String getPackageName()
{
   switch (name_type) {
      case NONE :
	 break;
      case PACKAGE :
	 return getName();
      case CLASS :
      case INTERFACE :
      case ENUM :
      case THROWABLE :
      case MODULE :
      case ANNOTATION :
	 return getNameHead();
      case FIELDS :
      case METHOD :
      case CONSTRUCTOR :
      case STATICS :
      case MAIN_PROGRAM :
	 String nm = getSymbolName();
	 int idx1 = nm.lastIndexOf(".");
	 if (idx1 <= 0) return null;
	 int idx2 = nm.lastIndexOf(".",idx1-1);
	 if (idx2 < 0) return null;
	 return nm.substring(0,idx2);
      default:
	 break;
    }

   return null;
}




@Override public String [] getNameComponents()
{
   StringBuilder buf = new StringBuilder();
   String p = getProject();
   String sp = getSubProject();
   if (p != null) {
      buf.append(p);
      if (sp == null) buf.append(":.");
      else buf.append(":.");
    }
   if (sp != null) {
      buf.append(sp);
      buf.append(":.");
    }
   String nh = getNameHead();
   if (nh != null) {
      buf.append(nh);
      buf.append(".");
      buf.append(getLocalName());
    }
   String nm = buf.toString();

   String [] nms = nm.split(SPLIT_PATTERN);

   return nms;
}



@Override public String getNameWithParameters()
{
   if (param_name == null) {
      String lnm = getLocalName();
      String ps = getParameters();
      if (ps != null) lnm += ps;
      param_name = lnm;
    }

   return param_name;
}



@Override public String getFullName()
{
   if (full_name == null) {
      full_name = getNameHead() + "." + getNameWithParameters();
    }

   return full_name;
}



@Override public Icon getDisplayIcon()			{ return null; }

@Override public String getDisplayName()
{
   return getNameWithParameters();
}




@Override public BumpLocation getLocation()		{ return null; }
@Override public Collection<BumpLocation> getLocations() { return null; }





/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

protected String stripTemplates(String nm)
{
   int idx = nm.indexOf("<");
   if (idx < 0) return nm;

   StringBuffer buf = new StringBuffer(nm.substring(0,idx));
   int lvl = 0;
   for (int i = idx; i < nm.length(); ++i) {
      char ch = nm.charAt(i);
      if (ch == '<') {
	 ++lvl;
       }
      else if (ch == '>') {
	 --lvl;
       }
      else if (lvl <= 0) buf.append(ch);
    }

   return buf.toString();
}




/********************************************************************************/
/*										*/
/*	Bubble creation methods 						*/
/*										*/
/*										*/
/********************************************************************************/

@Override abstract public BudaBubble createBubble();

@Override public BudaBubble createPreviewBubble()
{
   return null;
}


@Override public String createPreviewString()
{
   String s = getLocalName();
   int idx = s.indexOf("#");
   if (idx < 0) return s;

   return s.substring(idx+1);
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   return getName();
}




/********************************************************************************/
/*										*/
/*	Hashing methods 							*/
/*										*/
/********************************************************************************/

@Override public int hashCode()
{
   return getKey().hashCode() ^ name_type.hashCode();
}



@Override public boolean equals(Object o)
{
   if (o instanceof BassNameBase) {
      BassNameBase bn = (BassNameBase) o;
      if (getKey().equals(bn.getKey()) && name_type == bn.name_type) return true;
    }

   return false;
}




}	// end of class BassNameBase




/* end of BassNameBase.java */
