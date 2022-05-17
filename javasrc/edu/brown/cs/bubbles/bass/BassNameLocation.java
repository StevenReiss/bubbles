/********************************************************************************/
/*										*/
/*		BassNameLocation.java						*/
/*										*/
/*	Bubble Augmented Search Strategies name based on set of locations	*/
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

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.bump.BumpConstants;
import edu.brown.cs.bubbles.bump.BumpLocation;

import javax.swing.Icon;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class BassNameLocation extends BassNameBase implements BassConstants, BumpConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private List<BumpLocation>	location_set;
private BumpLocation		base_location;
private Icon			appropriate_icon;
private String			method_params;
private String                  file_prefix;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BassNameLocation(BumpLocation bl,String prefix)
{
   location_set = new ArrayList<>();
   base_location = bl;
   location_set.add(bl);
   file_prefix = prefix;

   switch (bl.getSymbolType()) {
      case PROJECT :
	 name_type = BassNameType.PROJECT;
	 break;
      case PACKAGE :
	 name_type = BassNameType.PACKAGE;
	 break;
      case CLASS :
	 name_type = BassNameType.CLASS;
	 break;
      case ANNOTATION :
         name_type = BassNameType.ANNOTATION;
         break;
      case THROWABLE :
	 name_type = BassNameType.THROWABLE;
	 break;
      case INTERFACE :
	 name_type = BassNameType.INTERFACE;
	 break;
      case ENUM :
	 name_type = BassNameType.ENUM;
	 break;
      case FUNCTION :
	 name_type = BassNameType.METHOD;
	 break;
      case CONSTRUCTOR :
	 name_type = BassNameType.CONSTRUCTOR;
	 break;
      case ENUM_CONSTANT :
      case FIELD :
      case GLOBAL :
	 name_type = BassNameType.FIELDS;
	 break;
      case MAIN_PROGRAM :
	 name_type = BassNameType.MAIN_PROGRAM;
	 break;
      case STATIC_INITIALIZER :
	 name_type = BassNameType.STATICS;
	 break;
      case MODULE :
	 name_type = BassNameType.MODULE;
	 break;
      case IMPORT :
	 name_type = BassNameType.IMPORTS;
	 break;
      case EXPORT :
	 name_type = BassNameType.EXPORTS;
	 break;
      case PROGRAM :
	 name_type = BassNameType.CODE;
	 break;
      default :
	 name_type = BassNameType.NONE;
	 break;
    }

   method_params = base_location.getParameters();
   if (name_type == BassNameType.METHOD) {
      if (Modifier.isPublic(getModifiers())) appropriate_icon = BoardImage.getIcon("method");
      else if (Modifier.isPrivate(getModifiers())) appropriate_icon = BoardImage.getIcon("private_method");
      else if (Modifier.isProtected(getModifiers())) appropriate_icon = BoardImage.getIcon("protected_method");
      else appropriate_icon = BoardImage.getIcon("default_method");
    }
   else if (name_type == BassNameType.CONSTRUCTOR) {
      if (Modifier.isPublic(getModifiers())) appropriate_icon = BoardImage.getIcon("constructor");
      else if (Modifier.isPrivate(getModifiers())) appropriate_icon = BoardImage.getIcon("private_constructor");
      else if (Modifier.isProtected(getModifiers())) appropriate_icon = BoardImage.getIcon("protected_constructor");
      else appropriate_icon = BoardImage.getIcon("default_constructor");
    }
}



BassNameLocation(BumpLocation bl,BassNameType typ,String pfx)
{
   location_set = new ArrayList<>();
   base_location = bl;
   file_prefix = pfx;
   location_set.add(bl);
   name_type = typ;
}




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

void addLocation(BumpLocation bl)
{
   if (base_location == null) base_location = bl;

   location_set.add(bl);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getProject()			{ return base_location.getSymbolProject(); }

@Override public String getSubProject()
{
   return file_prefix;
}

@Override public int getModifiers()			{ return base_location.getModifiers(); }

@Override protected String getKey()			{ return base_location.getKey(); }
@Override protected String getSymbolName()		{ return base_location.getSymbolName(); }
@Override protected String getParameters()		{ return method_params; }

File getFile()						{ return base_location.getFile(); }

int getEclipseStartOffset() {
   switch (name_type) {
      case HEADER :
	 if (!base_location.getKey().contains("$")) return 0;
	 break;
      case FILE :
	 return 0;
      default :
	 break;
   }
   return base_location.getDefinitionOffset();
}
int getEclipseEndOffset()				{ return base_location.getDefinitionEndOffset(); }
BumpSymbolType getSymbolType()				{ return base_location.getSymbolType(); }

@Override public BumpLocation getLocation()		{ return base_location; }
@Override public Collection<BumpLocation> getLocations()
{
   if (location_set == null || location_set.size() <= 1) return null;
   return location_set;
}



@Override public String getLocalName()
{
   switch (name_type) {
      case FIELDS :
	 return "< FIELDS >";
      case STATICS :
	 return "< INITIALIZERS >";
      case MAIN_PROGRAM :
	 return "< MAIN >";
      case IMPORTS :
      case EXPORTS :
      case HEADER :
	 return "< PREFIX >";
      case FILE :
	 return "< FILE >";
      case MODULE :
	 return "< MODULE >";
      case CLASS :
      case ENUM :
      case INTERFACE :
      case THROWABLE :
      case ANNOTATION :
	 return "< BODY OF " + super.getLocalName() + " >";
      default:
	 break;
    }

   return super.getLocalName();
}


@Override public String getNameHead()
{
   switch (name_type) {
      case CLASS :
      case ENUM :
      case INTERFACE :
      case THROWABLE :
      case ANNOTATION :
      case HEADER :
      case IMPORTS :
      case EXPORTS :
      case FILE :
      case OTHER_CLASS :
      case MODULE :
      case VIRTUAL_CODE :
	 String nm = getUserSymbolName();
	 nm = stripTemplates(nm);
	 return nm;			// we add <PREFIX> or <BODY> for classes
      default:
	 break;
    }

   return super.getNameHead();
}



@Override public String getFullName()
{
   switch (name_type) {
      case STATICS :
      case MAIN_PROGRAM :
      case FIELDS :
      case CLASS :
      case THROWABLE :
      case ENUM :
      case INTERFACE :
      case ANNOTATION :
      case HEADER :
      case IMPORTS :
      case EXPORTS :
      case OTHER_CLASS :
      case FILE :
      case MODULE :
	 return getNameHead() + ". " + getLocalName();
      default:
	 break;
    }

   return super.getFullName();
}




/********************************************************************************/
/*										*/
/*	Bubble creation methods 						*/
/*										*/
/********************************************************************************/

@Override public BudaBubble createBubble()
{
   BudaBubble bb = null;
   BaleFactory bf = BaleFactory.getFactory();

   //TODO: This should be done in BALE, possibly indirect through BUDA

   switch (name_type) {
      case CONSTRUCTOR :
      case METHOD :
	 bb = bf.createMethodBubble(getProject(),getFullName());
	 break;
      case FIELDS :
	 bb = bf.createFieldsBubble(getProject(),getFile(),getNameHead());
	 break;
      case STATICS :
	 bb = bf.createStaticsBubble(getProject(),getNameHead(),getFile());
	 break;
      case MAIN_PROGRAM :
	 bb = bf.createMainProgramBubble(getProject(),getNameHead(),getFile());
	 break;
      case CLASS :
      case ENUM :
      case INTERFACE :
      case THROWABLE :
      case ANNOTATION :
	 bb = bf.createClassBubble(getProject(),getNameHead());
	 break;
      case HEADER :
	 bb = bf.createClassPrefixBubble(getProject(),getFile(),getNameHead());
	 break;
      case FILE :
	 bb = bf.createFileBubble(getProject(),getFile(),getNameHead());
	 break;
      case PROJECT :
	 break;
      case MODULE :
	 bb = bf.createFileBubble(getProject(),getFile(),getNameHead());
	 break;
      default :
	 BoardLog.logW("BASS","NO BUBBLE FOR " + getKey());
	 break;
    }

   return bb;
}



@Override public BudaBubble createPreviewBubble()
{
   switch (name_type) {
      case PROJECT :
      case PACKAGE :
      case FILE :
      case MODULE :
      default :
	 break;
      case CONSTRUCTOR :
      case METHOD :
      case FIELDS :
      case STATICS :
      case MAIN_PROGRAM :
      case CLASS :
      case ENUM :
      case INTERFACE :
      case THROWABLE :
      case ANNOTATION :
      case HEADER :
	 return createBubble();
    }

   return null;
}


@Override public String createPreviewString()
{
   switch (name_type) {
      case FILE :
	 return "Show file bubble for " + getNameHead();
      default:
	 break;
    }

   return null;
}



/********************************************************************************/
/*										*/
/*	Display methods 							*/
/*										*/
/********************************************************************************/

@Override public Icon getDisplayIcon()
{
   switch (name_type) {
      case CONSTRUCTOR :
      case METHOD :
	 return appropriate_icon;
      default:
	 break;
   }
								
   return null;
}









}	// end of class BassNameLocation




/* end of BassNameLocation.java */
