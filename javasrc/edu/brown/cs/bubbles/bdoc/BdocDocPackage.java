/********************************************************************************/
/*										*/
/*		BdocDocPackage.java						*/
/*										*/
/*	Bubbles Environment Documentation bubbles javadoc package item		*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bdoc;


import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;

import java.io.IOException;
import java.net.URL;



class BdocDocPackage extends BdocDocItem implements BdocConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private ParseState	parse_state;
private SubItemImpl	cur_item;
private ItemRelation	item_relation;

enum ParseState {
   NONE,
   HEADER,
   ITEM_TYPE,
   ITEM_TYPENAME,
   CLASS_BEGIN,
   CLASS_TAG,
   CLASS_DESC,
   DESCRIPTION
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BdocDocPackage(URL u) throws IOException
{
   super(u);

   loadUrl(u);
}



/********************************************************************************/
/*										*/
/*	Parsing methods 							*/
/*										*/
/********************************************************************************/

@Override protected void handleStartDocument()
{
   parse_state = ParseState.HEADER;
   item_relation = ItemRelation.NONE;
   cur_item = null;
}


@Override public void handleComment(char [] text,int pos)
{
   String s = new String(text);
   switch (parse_state) {
      case DESCRIPTION :
	 addDescriptionComment(s);
	 break;
      default:
	 break;
    }
}



@Override public void handleSimpleTag(HTML.Tag tag,MutableAttributeSet a,int pos)
{
   handleStartTag(tag,a,pos);
}


@Override public void handleStartTag(HTML.Tag tv,MutableAttributeSet a,int pos)
{
   switch (parse_state) {
      case HEADER :
	 if (tv == HTML.Tag.H2) parse_state = ParseState.NONE;
	 break;
      case NONE :
	 if (tv == HTML.Tag.TD) parse_state = ParseState.CLASS_BEGIN;
	 else if (tv == HTML.Tag.TH) parse_state = ParseState.ITEM_TYPE;
	 else if (tv == HTML.Tag.A) {
	    String nm = getAttribute(HTML.Attribute.NAME,a);
	    if (nm != null && nm.equals("package_description"))
	       parse_state = ParseState.DESCRIPTION;
	  }
	 break;
      case CLASS_BEGIN :
	 if (tv == HTML.Tag.A) {
	    parse_state = ParseState.CLASS_TAG;
	    if (item_relation != ItemRelation.NONE) {
	       cur_item = new SubItemImpl(item_relation);
	       cur_item.setUrl(ref_url,getAttribute(HTML.Attribute.HREF,a));
	     }
	  }
	 break;
      case DESCRIPTION :
	 if (tv == HTML.Tag.DL) parse_state = ParseState.NONE;
	 else addDescriptionTag(tv,a);
	 break;
      case ITEM_TYPE :
	 if (tv == HTML.Tag.B) parse_state = ParseState.ITEM_TYPENAME;
	 break;
      default:
	 break;
    }
}



@Override public void handleEndTag(HTML.Tag tv,int pos)
{
   switch (parse_state) {
      case CLASS_BEGIN :
	 if (tv == HTML.Tag.TD) parse_state = ParseState.NONE;
	 break;
      case CLASS_TAG :
	 if (tv == HTML.Tag.TR) parse_state = ParseState.NONE;
	 else if (tv == HTML.Tag.A) parse_state = ParseState.CLASS_DESC;
	 break;
      case CLASS_DESC :
	 if (tv == HTML.Tag.TR) parse_state = ParseState.NONE;
	 break;
      case ITEM_TYPE :
	 if (tv == HTML.Tag.TH) parse_state = ParseState.NONE;
	 break;
      case ITEM_TYPENAME :
	 if (tv == HTML.Tag.B) parse_state = ParseState.ITEM_TYPE;
	 break;
      case NONE :
	 if (tv == HTML.Tag.TABLE) item_relation = ItemRelation.NONE;
	 break;
      case DESCRIPTION :
	 addDescriptionEndTag(tv);
	 break;
      default:
	 break;
    }
}


@Override public void handleText(char [] data,int pos)
{
   String s;

   switch (parse_state) {
      case CLASS_TAG :
	 if (cur_item != null) {
	    s = new String(data).trim();
	    cur_item.setName(s);
	    addSubitem(cur_item);
	  }
	 break;
      case CLASS_DESC :
	 if (cur_item != null) {
	    s = new String(data).trim();
	    cur_item.setDescription(s);
	  }
	 break;
      case ITEM_TYPE :
	 s = new String(data).trim();
	 if (s.startsWith("Exception")) {
	    item_relation = ItemRelation.PACKAGE_EXCEPTION;
	  }
	 else if (s.startsWith("Enum")) {
	    item_relation = ItemRelation.PACKAGE_ENUM;
	  }
	 else if (s.startsWith("Class")) {
	    item_relation = ItemRelation.PACKAGE_CLASS;
	  }
	 else if (s.startsWith("Interface")) {
	    item_relation = ItemRelation.PACKAGE_INTERFACE;
	  }
	 else if (s.startsWith("Error")) {
	    item_relation = ItemRelation.PACKAGE_ERROR;
	  }
	 else {
	    item_relation = ItemRelation.NONE;
	  }
	 break;
      case DESCRIPTION :
	 addDescription(new String(data));
	 break;
      default:
	 break;
    }
}



@Override protected void handleEndDocument()
{ }



}	// end of class BdocDocPackage




/* end of BdocDocPackage.java */


