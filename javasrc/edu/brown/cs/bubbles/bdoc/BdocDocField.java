/********************************************************************************/
/*										*/
/*		BdocDocField.java						*/
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



class BdocDocField extends BdocDocItem implements BdocConstants
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
   LIST,
   LISTNAME,
   FINISH
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BdocDocField(URL u) throws IOException
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
   if (parse_state == ParseState.NONE && t == HTML.Tag.PRE) {
      parse_state = ParseState.DESC;
      addDescriptionTag(t,a);
    }
   else if (parse_state == ParseState.DESC && t == HTML.Tag.DT) {
      parse_state = ParseState.LIST;
    }
   else if (parse_state == ParseState.DESC) {
      addDescriptionTag(t,a);
    }
   else if (parse_state == ParseState.LIST && t == HTML.Tag.HR) {
      parse_state = ParseState.FINISH;
    }
   else if (parse_state == ParseState.LIST && t == HTML.Tag.B && list_relation == ItemRelation.NONE) {
      parse_state = ParseState.LISTNAME;
    }
   else if (parse_state == ParseState.LIST && list_relation != ItemRelation.NONE && t == HTML.Tag.A) {
      cur_item = new SubItemImpl(list_relation);
      String hr = getAttribute(HTML.Attribute.HREF,a);
      cur_item.setUrl(ref_url,hr);
    }

}


@Override public void handleEndTag(HTML.Tag t,int pos)
{
   if (parse_state == ParseState.DESC) {
      addDescriptionEndTag(t);
    }
   else if (parse_state == ParseState.LISTNAME && t == HTML.Tag.B) {
      parse_state = ParseState.LIST;
    }
   else if (parse_state == ParseState.LIST && t == HTML.Tag.A && cur_item != null) {
      addSubitem(cur_item);
      cur_item = null;
    }
}


@Override public void handleText(char [] data,int pos)
{
   if (parse_state == ParseState.DESC) {
      addDescription(new String(data));
    }
   else if (parse_state == ParseState.LISTNAME) {
      String id = new String(data);
      if (id.equals("See Also:")) {
	 list_relation = ItemRelation.SEE_ALSO;
       }
    }
   else if (parse_state == ParseState.LIST && cur_item != null) {
      cur_item.setName(new String(data));
    }
}




}	// end of class BdocDocField




/* end of BdocDocField.java */



