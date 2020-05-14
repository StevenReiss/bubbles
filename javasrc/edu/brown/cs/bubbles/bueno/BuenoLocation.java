/********************************************************************************/
/*										*/
/*		BuenoLocation.java						*/
/*										*/
/*	BUbbles Environment New Objects creator location holder 		*/
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


import java.io.File;




public abstract class BuenoLocation implements BuenoConstants
{



/********************************************************************************/
/*										*/
/*	Private fields								*/
/*										*/
/********************************************************************************/

private File		insert_file;
private int		insert_offset;
private int		insert_length;



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public String getProject()			{ return null; }

public String getPackage()			{ return null; }



/**
 *	This should return the fully qualified class name, with $ separating nested classes.
 **/

public String getClassName()			{ return null; }



public int getOffset()				{ return -1; }

public File getFile()				{ return null; }

public String getInsertAfter()			{ return null; }

public String getInsertBefore() 		{ return null; }

public String getInsertAtEnd()			{ return null; }




/********************************************************************************/
/*										*/
/*	Methods to record the actual location					*/
/*										*/
/********************************************************************************/

public void setFile(File f)			{ insert_file = f; }

public void setLocation(int offset,int len)
{
   insert_offset = offset;
   insert_length = len;
}


public File getInsertionFile()			{ return insert_file; }
public int getInsertionOffset() 		{ return insert_offset; }
public int getInsertionLength() 		{ return insert_length; }



/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

public String getTitle(BuenoType typ)
{
   String ttl = null;

   switch (typ) {
      case NEW_METHOD :
	 ttl = "Create New Method " + getMemberWhere();
	 break;
      case NEW_CLASS :
	 ttl = "Create New Class " + getClassWhere();
	 break;
      case NEW_INTERFACE :
	 ttl = "Create New Interface " + getClassWhere();
	 break;
      case NEW_ENUM :
	 ttl = "Create New Enum " + getClassWhere();
	 break;
      case NEW_TYPE :
	 ttl = "Create New Class/Interface/Enum " + getClassWhere();
	 break;
      case NEW_INNER_TYPE :
	 ttl = "Create New Inner Class/Interface/Enum " + getMemberWhere();
	 break;
      case NEW_FIELD :
	 ttl = "Create New Field " + getMemberWhere();
	 break;
      case NEW_PACKAGE :
	 ttl = "Create New Package in Project " + getProject();
	 break;
      case NEW_MODULE :
	 ttl = "Create New Module in Project " + getProject();
	 break;
      default:
	 break;
    }

   return ttl;
}



public String getTitleLocation(BuenoType typ)
{
   String ttl = null;

   switch (typ) {
      case NEW_METHOD :
      case NEW_INNER_TYPE :
      case NEW_FIELD :
	 ttl = getMemberWhere();
	 break;
      case NEW_CLASS :
      case NEW_INTERFACE :
      case NEW_ENUM :
      case NEW_TYPE :
	 ttl = getClassWhere();
	 break;
      default:
	 break;
    }

   return ttl;
}



private String getMemberWhere()
{
   String ttl = "";

   String whr = getInsertAfter();
   if (whr != null) ttl += "After " + getTitleName(whr);
   else {
      whr = getInsertBefore();
      if (whr != null) ttl += "Before " + getTitleName(whr);
      else {
	 whr = getInsertAtEnd();
	 if (whr != null) ttl += "At end of " + getTitleName(whr);
      }
   }

   return ttl;
}


private String getClassWhere()
{
   String ttl = "";

   String whr = getPackage();
   if (whr == null || whr.length() == 0) {
      ttl = "In default package";
    }
   else {
      ttl = "In " + whr;
    }

   return ttl;
}


private String getTitleName(String s)
{
    int idx = s.indexOf("(");
    if (idx >= 0) s = s.substring(0,idx);
    idx = s.lastIndexOf(".");
    if (idx >= 0) s = s.substring(idx+1);

    return s;
}




}	// end of abstract class BuenoLocation




/* end of BuenoLocation.java */
