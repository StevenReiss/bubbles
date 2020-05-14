/********************************************************************************/
/*										*/
/*		BuenoCreatorTemplate.java					*/
/*										*/
/*	BUbbles Environment New Objects creator simple creation methods 	*/
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


import java.io.IOException;
import java.io.Reader;



class BuenoCreatorTemplate extends BuenoCreator implements BuenoConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BuenoCreatorTemplate()
{ }



/********************************************************************************/
/*										*/
/*	Template usage methods							*/
/*										*/
/********************************************************************************/

@Override protected void classText(StringBuffer buf,BuenoProperties props)
{
   String typ = props.getStringProperty(BuenoKey.KEY_TYPE);
   Reader r = findTemplate(typ,props);
   if (r == null) r = findTemplate("type",props);
   if (r != null) {
      try {
	 expand(r,props,buf);
	 return;
       }
      catch (IOException e) { }
    }
   super.classText(buf,props);
}



@Override protected void moduleText(StringBuffer buf,BuenoProperties props)
{
   Reader r = findTemplate("module",props);
   if (r != null) {
      try {
	 expand(r,props,buf);
	 return;
       }
      catch (IOException e) { }
    }

   super.moduleText(buf,props);
}





@Override protected void innerClassText(StringBuffer buf,BuenoProperties props)
{
   String typ = props.getStringProperty(BuenoKey.KEY_TYPE);
   Reader r = findTemplate("inner" + typ,props);
   if (r == null) r = findTemplate("innertype",props);
   if (r != null) {
      try {
	 expand(r,props,buf);
	 return;
       }
      catch (IOException e) { }
    }
   super.innerClassText(buf,props);
}



@Override protected void methodText(StringBuffer buf,BuenoProperties props)
{
   Reader r = null;

   if (props.getStringProperty(BuenoKey.KEY_RETURNS) == null)
      r = findTemplate("constructor",props);
   if (r == null) r = findTemplate("method",props);
   if (r != null) {
      try {
	 expand(r,props,buf);
	 return;
       }
      catch (IOException e) { }
    }
   super.methodText(buf,props);
}



@Override protected void fieldText(StringBuffer buf,BuenoProperties props)
{
   Reader r = findTemplate("field",props);
   if (r != null) {
      try {
	 expand(r,props,buf);
	 return;
       }
      catch (IOException e) { }
    }
   super.fieldText(buf,props);
}



@Override protected void setupMarquisComment(StringBuffer buf,BuenoProperties props)
{
   Reader r = findTemplate("marquis",props);
   if (r != null) {
      try {
	 expand(r,props,buf);
	 return;
       }
      catch (IOException e) { }
    }
   super.setupMarquisComment(buf,props);
}



@Override protected void setupBlockComment(StringBuffer buf,BuenoProperties props)
{
   Reader r = findTemplate("block",props);
   if (r != null) {
      try {
	 expand(r,props,buf);
	 return;
       }
      catch (IOException e) { }
    }
   super.setupBlockComment(buf,props);
}



@Override protected void setupJavadocComment(StringBuffer buf,BuenoProperties props)
{
   Reader r = findTemplate("javadoc",props);
   if (r != null) {
      try {
	 expand(r,props,buf);
	 return;
       }
      catch (IOException e) { }
    }
   super.setupJavadocComment(buf,props);
}




}	// end of class BuenoCreatorTemplate




/* end of BuenoCreatorTemplate.java */
