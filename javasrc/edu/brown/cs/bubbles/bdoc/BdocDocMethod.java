/********************************************************************************/
/*										*/
/*		BdocDocMethod.java						*/
/*										*/
/*	Bubbles Environment Documentation bubbles javadoc field detail		*/
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



class BdocDocMethod extends BdocDocItem implements BdocConstants
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
   DESC,
   DESCTABLE,
   DESCNEW,
   LIST,
   LISTNAME,
   LISTBODY,
   ITEMNAME,
   ITEMDESC,
   FINISH
}


private static Map<String,ItemRelation> method_items;

static {
   method_items = new HashMap<String,ItemRelation>();
   method_items.put("See Also:",ItemRelation.SEE_ALSO);
   method_items.put("Parameters:",ItemRelation.PARAMETER);
   method_items.put("Returns:",ItemRelation.RETURN);
   method_items.put("Overrides:",ItemRelation.OVERRIDE);
   method_items.put("Throws:",ItemRelation.THROW);
   method_items.put("Specified by:",ItemRelation.SPECIFIED_BY);
}





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BdocDocMethod(URL u) throws IOException
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
      addDescriptionComment(new String(text));
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
	 if (t == HTML.Tag.PRE) {
	    parse_state = ParseState.DESC;
	    addDescriptionTag(t,a);
	  }
	 break;
      case DESC :
	 if (t == HTML.Tag.DT) parse_state = ParseState.LIST;
	 else if (t == HTML.Tag.TABLE) {
	    addDescriptionTag(t,a);
	    parse_state = ParseState.DESCTABLE;
	  }
         else if (t == HTML.Tag.DIV) {
            addDescriptionTag(t,a);
            parse_state = ParseState.DESCNEW;
          }
	 else addDescriptionTag(t,a);
	 break;
      case DESCNEW :
      case DESCTABLE :
	 addDescriptionTag(t,a);
	 break;
      case LIST :
	 if (t == HTML.Tag.HR) parse_state = ParseState.FINISH;
	 else if (t == HTML.Tag.B) parse_state = ParseState.LISTNAME;
         else if (t == HTML.Tag.SPAN) parse_state = ParseState.LISTNAME;
	 break;
      case LISTNAME :
	 break;
      case LISTBODY :
	 if (t == HTML.Tag.CODE) {
	    parse_state = ParseState.ITEMNAME;
	    if (cur_item == null) {
	       cur_item = new SubItemImpl(list_relation);
	       addSubitem(cur_item);
	     }
	  }
	 else if (t == HTML.Tag.A) {
	    cur_item = new SubItemImpl(list_relation);
	    addSubitem(cur_item);
	    String hr = getAttribute(HTML.Attribute.HREF,a);
	    cur_item.setUrl(ref_url,hr);
	  }
	 else if (t == HTML.Tag.DD && list_relation == ItemRelation.RETURN) {
	    cur_item = new SubItemImpl(list_relation);
	    addSubitem(cur_item);
	    parse_state = ParseState.ITEMDESC;
	  }
	 break;
       case ITEMNAME :
	  if (t == HTML.Tag.A) {
	     String hr = getAttribute(HTML.Attribute.HREF,a);
	     cur_item.setUrl(ref_url,hr);
	   }
	  else cur_item.addNameTag(t,a);
	  break;
       case ITEMDESC :
	  if (t == HTML.Tag.DD) {
	     parse_state = ParseState.LISTBODY;
	     cur_item = null;
	   }
	  else if (t == HTML.Tag.DT) {
	     parse_state = ParseState.LIST;
	     cur_item = null;
	     list_relation = ItemRelation.NONE;
	   }
	  else cur_item.addDescriptionTag(t,a);
	  break;
      default:
	 break;
    }
   // BoardLog.logD("BDOC","Tag " + t + " " + parse_state + " DESC: " + item_description);
}


@Override public void handleEndTag(HTML.Tag t,int pos)
{
   switch (parse_state) {
      case NONE :
	 break;
      case DESC :
	 addDescriptionEndTag(t);
	 break;
      case DESCTABLE :
	 addDescriptionEndTag(t);
	 if (t == HTML.Tag.TABLE) parse_state = ParseState.DESC;
	 break;
      case DESCNEW :
         addDescriptionEndTag(t);
         if (t == HTML.Tag.DIV) parse_state = ParseState.LIST;
         break;
      case LIST :
	 if (t == HTML.Tag.DL) {
	    list_relation = ItemRelation.NONE;
	    cur_item = null;
	  }
         else if (t == HTML.Tag.UL) {
            parse_state = ParseState.FINISH;
          }
	 break;
      case LISTNAME :
	 if (t == HTML.Tag.B || t == HTML.Tag.SPAN) {
	    if (list_relation == ItemRelation.NONE) parse_state = ParseState.LIST;
	    else parse_state = ParseState.LISTBODY;
	  }
	 break;
      case LISTBODY :
         if (t == HTML.Tag.DL) {
            parse_state = ParseState.LIST;
            list_relation = ItemRelation.NONE;
	    cur_item = null;
          }
         break;
      case ITEMNAME :
	 if (t == HTML.Tag.CODE) parse_state = ParseState.ITEMDESC;
	 else if (t != HTML.Tag.A) cur_item.addNameEndTag(t);
	 break;
      case ITEMDESC :
	 if (t == HTML.Tag.DL) {
	    list_relation = ItemRelation.NONE;
	    parse_state = ParseState.LIST;
	  }
         else if (t == HTML.Tag.A) {
            parse_state = ParseState.LISTBODY;
          }
         else if (t == HTML.Tag.DD) ;
         else cur_item.addDescriptionEndTag(t);
	 break;
      default:
	 break;
    }
   // BoardLog.logD("BDOC","End " + t + " " + parse_state + " DESC: " + item_description);
}


@Override public void handleText(char [] data,int pos)
{
   switch (parse_state) {
      case NONE :
	 break;
      case DESC :
      case DESCTABLE :
      case DESCNEW :
	 addDescription(new String(data));
	 break;
      case LISTNAME :
	 String id = new String(data);
	 ItemRelation ir = method_items.get(id);
	 if (ir != null) list_relation = ir;
	 break;
      case LISTBODY :
	 if (cur_item != null) cur_item.setName(new String(data));
	 break;
      case ITEMNAME :
	 if (cur_item != null) cur_item.setName(new String(data));
	 break;
      case ITEMDESC :
	 if (cur_item != null) cur_item.setDescription(new String(data));
	 break;
      default:
	 break;
    }
}




}	// end of class BdocDocMethod




/* end of BdocDocMethod.java */



