/********************************************************************************/
/*										*/
/*		BdocDocClass.java						*/
/*										*/
/*	Bubbles Environment Documentation bubbles javadoc class item		*/
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
import java.util.HashMap;
import java.util.Map;



class BdocDocClass extends BdocDocItem implements BdocConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private ParseState	parse_state;
private ItemRelation	list_relation;
private SubItemImpl	cur_item;

enum ParseState {
   NONE,
   READY,
   SUPER,
   LIST,
   LISTNAME,
   LISTITEMS,
   DATA,
   DESC,
   BODY,
   BODYELT,
   BODYHDR,
   BODYNAME,
   BODYLINK,
   BODYDESC,
   BODYNAMETBL,
   FINISH
}



private static Map<String,ItemRelation> class_items;

static {
   class_items = new HashMap<String,ItemRelation>();
   class_items.put("All Implemented Interfaces:",ItemRelation.IMPLEMENTS);
   class_items.put("Type Parameters:",ItemRelation.TYPE_PARAMETERS);
   class_items.put("See Also:",ItemRelation.SEE_ALSO);
   class_items.put("All Known Subinterfaces:",ItemRelation.SUBINTERFACES);
   class_items.put("All Known Implementing Classes:",ItemRelation.IMPLEMENTING_CLASS);
   class_items.put("Direct Known Subclasses:",ItemRelation.SUBCLASS);
   class_items.put("nested_class_summary",ItemRelation.NESTED_CLASS);
   class_items.put("field_summary",ItemRelation.FIELD);
   class_items.put("constructor_summary",ItemRelation.CONSTRUCTOR);
   class_items.put("method_summary",ItemRelation.METHOD);
   class_items.put("nested_classes_inherited_from_",ItemRelation.INHERITED_CLASS);
   class_items.put("fields_inherited_from_",ItemRelation.INHERITED_FIELD);
   class_items.put("methods_inherited_from_",ItemRelation.INHERITED_METHOD);
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BdocDocClass(URL u) throws IOException
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
   parse_state = ParseState.NONE;
   list_relation = ItemRelation.NONE;
   cur_item = null;
}


@Override public void handleComment(char [] text,int pos)
{
   if (parse_state == ParseState.DESC) {
      String txt = new String(text);
      if (txt.contains("========")) parse_state = ParseState.DATA;
      else addDescriptionComment(new String(text));
    }
}


@Override public void handleSimpleTag(HTML.Tag t,MutableAttributeSet a,int pos)
{
   handleStartTag(t,a,pos);
}



@Override public void handleStartTag(HTML.Tag t,MutableAttributeSet a,int pos)
{
   switch (parse_state) {
      case NONE :
	 if (t == HTML.Tag.H2) parse_state = ParseState.READY;	// start of class
	 break;
      case READY :
	 if (t == HTML.Tag.PRE) parse_state = ParseState.SUPER; // superclass
	 else if (t == HTML.Tag.DT) parse_state = ParseState.DATA;
         else if (t == HTML.Tag.DL) {           // for interfaces
            parse_state = ParseState.LIST;
            list_relation = ItemRelation.NONE;
            cur_item = null;
         }
	 break;
      case SUPER :
	 if (t == HTML.Tag.A) { 				// superclass item
	    cur_item = new SubItemImpl(ItemRelation.SUPERTYPE);
	    String hr = getAttribute(HTML.Attribute.HREF,a);
	    cur_item.setUrl(ref_url,hr);
	    addSubitem(cur_item);
	  }
	 break;
      case DATA :
	 if (t == HTML.Tag.PRE) {
	    parse_state = ParseState.DESC;
	    // addDescriptionTag(HTML.Tag.DL,null);
	    // addDescriptionTag(HTML.Tag.DT,null);
	    addDescriptionTag(t,a);
	  }
	 else if (t == HTML.Tag.DL) {
	    parse_state = ParseState.LIST;
	    list_relation = ItemRelation.NONE;
	    cur_item = null;
	  }
	 else if (t == HTML.Tag.A) {
	    String nm = getAttribute(HTML.Attribute.NAME,a);
	    if (nm != null) {
	       setListTypeFromName(nm);
	       if (list_relation != ItemRelation.NONE) parse_state = ParseState.BODY;
	       else if (nm.contains("_detail")) parse_state = ParseState.FINISH;
	     }
	  }
	 break;
      case LIST :
	 if (t == HTML.Tag.B) parse_state = ParseState.LISTNAME;
	 else if (t == HTML.Tag.DD) parse_state = ParseState.LISTITEMS;
	 break;
      case LISTITEMS :
	 if (t == HTML.Tag.A && list_relation != ItemRelation.NONE && cur_item == null) {
	    cur_item = new SubItemImpl(list_relation);
	    String hr = getAttribute(HTML.Attribute.HREF,a);
	    cur_item.setUrl(ref_url,hr);
	    addSubitem(cur_item);
	  }
	 break;
      case DESC :
	 if (t == HTML.Tag.DL) {
	    parse_state = ParseState.LIST;
	    list_relation = ItemRelation.NONE;
	    cur_item = null;
	  }
	 else if (t == HTML.Tag.DT) {
	    addDescriptionTag(HTML.Tag.BR,a);
	  }
	 else addDescriptionTag(t,a);
	 break;
      case BODY :
	 if (t == HTML.Tag.TR) parse_state = ParseState.BODYELT;
	 break;
      case BODYELT :
	 if (t == HTML.Tag.TH) parse_state = ParseState.BODYHDR;
	 else if (t == HTML.Tag.TD && list_relation != ItemRelation.NONE) {
	    if (cur_item == null) {
	       cur_item = new SubItemImpl(list_relation);
	       addSubitem(cur_item);
	       parse_state = ParseState.BODYNAME;
	     }
	  }
	 break;
      case BODYNAME :
	 if (t == HTML.Tag.BR) parse_state = ParseState.BODYDESC;
	 else if (t == HTML.Tag.TD) cur_item.setName(" ");
	 else {
	    cur_item.addNameTag(t,a);
	    if (t == HTML.Tag.B) parse_state = ParseState.BODYLINK;
	    else if (t == HTML.Tag.CODE && isCommaList())
	       parse_state = ParseState.BODYLINK;
	    else if (t == HTML.Tag.TABLE) {
	       parse_state = ParseState.BODYNAMETBL;
	     }
	  }
	 break;
      case BODYNAMETBL :
	 cur_item.addNameTag(t,a);
	 break;
      case BODYLINK :
	 if (t == HTML.Tag.A && list_relation != ItemRelation.NONE) {
	    String hr = getAttribute(HTML.Attribute.HREF,a);
	    if (hr != null) cur_item.setUrl(ref_url,hr);
	  }
	 else cur_item.addNameTag(t,a);
	 break;
      case BODYDESC :
	 cur_item.addDescriptionTag(t,a);
	 break;
      default:
	 break;

    }
}


@Override public void handleEndTag(HTML.Tag t,int pos)
{
   switch (parse_state) {
      case SUPER :
	 if (t == HTML.Tag.PRE) parse_state = ParseState.DATA;
	 else if (t == HTML.Tag.A) cur_item = null;
	 break;
      case LIST :
	 if (t == HTML.Tag.A) cur_item = null;
	 else if (t == HTML.Tag.DL) parse_state = ParseState.DATA;
	 break;
      case LISTNAME :
	 if (t == HTML.Tag.B) parse_state = ParseState.LIST;
	 break;
      case LISTITEMS :
	 if (t == HTML.Tag.DD) {
	    parse_state = ParseState.LIST;
	    list_relation = ItemRelation.NONE;
	    cur_item = null;
	  }
	 break;
      case DESC :
	 if (t == HTML.Tag.DL) parse_state = ParseState.DATA;
	 else {
	    addDescriptionEndTag(t);
	    addDescription(" ");
	  }
	 break;
      case BODY :
	 if (t == HTML.Tag.TABLE) {
	    parse_state = ParseState.DATA;
	    list_relation = ItemRelation.NONE;
	    cur_item = null;
	  }
	 break;
      case BODYELT :
      case BODYHDR :
	 if (t == HTML.Tag.TR) {
	    cur_item = null;
	    parse_state = ParseState.BODY;
	  }
	 break;
      case BODYDESC :
	 if (t == HTML.Tag.TD) {
	    cur_item = null;
	    parse_state = ParseState.BODY;
	  }
	 else cur_item.addDescriptionEndTag(t);
	 break;
      case BODYNAME :
	 if (t == HTML.Tag.TR) {
	    cur_item = null;
	    parse_state = ParseState.BODY;
	  }
	 else if (t == HTML.Tag.TD) ;
	 else cur_item.addNameEndTag(t);
	 break;
      case BODYNAMETBL :
	 cur_item.addNameEndTag(t);
	 if (t == HTML.Tag.TABLE) {
	    parse_state = ParseState.BODYNAME;
	 }
	 break;
      case BODYLINK :
	 if (t != HTML.Tag.A) cur_item.addNameEndTag(t);
	 if (t == HTML.Tag.B) parse_state = ParseState.BODYNAME;
	 if (t == HTML.Tag.CODE && isCommaList()) {
	    cur_item.addNameEndTag(t);
	    parse_state = ParseState.BODYNAME;
	  }
	 break;
      default:
	 break;
    }
}


@Override public void handleText(char [] data,int pos)
{
   switch (parse_state) {
      case SUPER :
      case LISTITEMS :
	 String s = new String(data);
	 if (s.startsWith(",")) cur_item = null;
	 else if (cur_item != null) cur_item.setName(s);
	 break;
      case LISTNAME :
	 setListTypeFromName(new String(data));
	 break;
      case DESC :
	 addDescription(new String(data));
	 break;
      case BODYNAME :
      case BODYLINK :
	 s = new String(data);
	 if (s.equals(", ") && isCommaList()) {
	    cur_item.addNameEndTag(HTML.Tag.CODE);
	    cur_item = new SubItemImpl(list_relation);
	    addSubitem(cur_item);
	    cur_item.addNameTag(HTML.Tag.CODE,null);
	    parse_state = ParseState.BODYLINK;
	  }
	 else if (cur_item != null) cur_item.setName(new String(data));
	 break;
      case BODYDESC :
	 if (cur_item != null) cur_item.setDescription(new String(data));
	 break;
      default:
	 break;
    }
}



private void setListTypeFromName(String nm)
{
   ItemRelation it = class_items.get(nm);
   if (it == null) {
      int idx = nm.indexOf("_class_");
      if (idx < 0) idx = nm.indexOf("_interface_");
      if (idx >= 0) nm = nm.substring(0,idx+1);
      it = class_items.get(nm);
    }
   if (it == null) it = ItemRelation.NONE;

   list_relation = it;
}



private boolean isCommaList()
{
   if (list_relation == null) return false;

   if (list_relation == ItemRelation.INHERITED_METHOD) return true;
   if (list_relation == ItemRelation.INHERITED_FIELD) return true;
   if (list_relation == ItemRelation.INHERITED_CLASS) return true;

   return false;
}



}	// end of class BdocDocClass




/* end of BdocDocClass.java */
