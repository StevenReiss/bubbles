/********************************************************************************/
/*										*/
/*		BdocDocItem.java						*/
/*										*/
/*	Bubbles Environment Documentation bubbles javadoc generic item		*/
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

import edu.brown.cs.bubbles.board.BoardLog;

import edu.brown.cs.ivy.xml.IvyXml;

import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;


abstract class BdocDocItem extends HTMLEditorKit.ParserCallback implements BdocConstants
{


/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

protected StringBuffer		item_description;
private   String		description_text;
protected List<SubItemImpl>	sub_items;
protected URL			ref_url;

private static ParserDelegator	parser_delegator = new ParserDelegator();



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected BdocDocItem(URL u)
{
   ref_url = u;

   item_description = new StringBuffer();
   sub_items = new ArrayList<SubItemImpl>();
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

private static final String [] HTML_REMOVALS = new String [] { "<P>", "<DL>", "<DD>", "</DD>", "</P>", "<HR>", " ", "\n" };



String getDescription()
{
   if (description_text == null) {
      description_text = item_description.toString();
      // TODO: reformat initial <PRE>...</PRE> area to not be fixed width
      for ( ; ; ) {
	 boolean fnd = false;
	 for (String s : HTML_REMOVALS) {
	    if (description_text.endsWith(s) || description_text.endsWith(s.toLowerCase())) {
	       int ln0 = description_text.length();
	       int ln1 = s.length();
	       description_text = description_text.substring(0,ln0-ln1);
	       fnd = true;
	     }
	  }
	 if (!fnd) break;
       }
    }
   return description_text;
}


List<SubItem> getItems(ItemRelation r)
{
   List<SubItem> rslt = null;

   for (SubItemImpl si : sub_items) {
      if (si.getRelation() == r) {
	 if (rslt == null) rslt = new ArrayList<SubItem>();
	 rslt.add(si);
       }
    }

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Parsing methods 							*/
/*										*/
/********************************************************************************/

void loadUrl(URL u) throws IOException
{
   URLConnection c = u.openConnection();
   InputStream ins = c.getInputStream();
   Reader inr = new BufferedReader(new InputStreamReader(ins));
   handleStartDocument();
   
   BoardLog.logD("BDOC","Work on " + u + " " + u.getRef());
   
   String id = u.getRef();
   if (id == null) {
      parser_delegator.parse(inr,this,true);
    }
   else {
      FragmentHandler fh = new FragmentHandler(id,this);
      parser_delegator.parse(inr,fh,true);
    }

   handleEndDocument();
   ins.close();
}



protected void handleStartDocument()			{ }
protected void handleEndDocument()			{ }



/********************************************************************************/
/*										*/
/*	Description methods							*/
/*										*/
/********************************************************************************/

protected void addDescription(String txt)
{
   addText(item_description,txt);
}


protected void addDescriptionComment(String txt)
{
   addComment(item_description,txt);
}


protected void addDescriptionTag(HTML.Tag t,MutableAttributeSet a)
{
   addTag(item_description,t,a);
}


protected void addDescriptionEndTag(HTML.Tag t)
{
   addEndTag(item_description,t);
}




/********************************************************************************/
/*										*/
/*	Subitem methods 							*/
/*										*/
/********************************************************************************/

void addSubitem(SubItemImpl itm)
{
   sub_items.add(itm);
}




/********************************************************************************/
/*										*/
/*	Parsing helper methods							*/
/*										*/
/********************************************************************************/

protected static String getAttribute(HTML.Attribute k,MutableAttributeSet a)
{
   return (String) a.getAttribute(k);
}


protected static String getHtmlString(String s)
{
   StringWriter sw = new StringWriter();
   IvyXml.outputXmlString(s,sw);
   String r = sw.toString();
   r = r.replace("&apos;","'");
   return r;
}



protected static void addText(StringBuffer buf,String txt)
{
   if (buf == null || txt == null || txt.length() == 0) return;

   buf.append(getHtmlString(txt));
}



protected static void addComment(StringBuffer buf,String txt)
{
   if (buf == null) return;

   buf.append("<!-- ");
   buf.append(txt);
   buf.append(" -->");
}


protected static void addTag(StringBuffer buf,HTML.Tag t,MutableAttributeSet a)
{
   if (buf == null) return;

   buf.append("<");
   buf.append(t.toString());
   if (a != null) {
      for (Enumeration<?> e = a.getAttributeNames(); e.hasMoreElements(); ) {
	 Object nm = e.nextElement();
	 Object vl = a.getAttribute(nm);
	 buf.append(" ");
	 buf.append(nm.toString());
	 buf.append("='");
	 buf.append(vl.toString());
	 buf.append("'");
       }
    }
   buf.append(">");
}


protected static void addEndTag(StringBuffer buf,HTML.Tag t)
{
   if (buf == null) return;

   buf.append("</");
   buf.append(t.toString());
   buf.append(">");
}



/********************************************************************************/
/*										*/
/*	Representation for a dummy HTML callback to handle fragments		*/
/*										*/
/********************************************************************************/

private static class FragmentHandler extends HTMLEditorKit.ParserCallback {

   private String fragment_name;
   private HTMLEditorKit.ParserCallback real_callback;
   private boolean is_active;

   FragmentHandler(String id,HTMLEditorKit.ParserCallback cb) {
      fragment_name = id;
      real_callback = cb;
      is_active = false;
    }

   @Override public void flush() throws BadLocationException	{ real_callback.flush(); }

   @Override public void handleComment(char [] data,int pos) {
      if (is_active) real_callback.handleComment(data,pos);
    }

   @Override public void handleEndOfLineString(String eol)	{ real_callback.handleEndOfLineString(eol); }

   @Override public void handleEndTag(HTML.Tag t,int pos) {
      if (is_active) real_callback.handleEndTag(t,pos);
    }

   @Override public void handleError(String msg,int pos) {
      if (is_active) real_callback.handleError(msg,pos);
    }

   @Override public void handleSimpleTag(HTML.Tag t,MutableAttributeSet a,int pos) {
      if (is_active) real_callback.handleSimpleTag(t,a,pos);
    }

   @Override public void handleText(char [] data,int pos) {
      if (is_active) real_callback.handleText(data,pos);
    }

   @Override public void handleStartTag(HTML.Tag t,MutableAttributeSet a,int pos) {
      if (t == HTML.Tag.A) {
         String nm = getAttribute(HTML.Attribute.NAME,a);
         if (nm != null) {
            nm = nm.replace(" ","+");
            if (is_active) {
               if (nm.contains("_") || nm.contains("(") || nm.endsWith("-")) {
                  is_active = false;
                }
             }
            else {
               is_active = nm.equals(fragment_name);
               return;
             }
          }
       }
      if (is_active) real_callback.handleStartTag(t,a,pos);
    }

}	// end of inner class FragmentHandler




/********************************************************************************/
/*										*/
/*	Representation for an internal item					*/
/*										*/
/********************************************************************************/

protected static class SubItemImpl implements SubItem {

   private StringBuffer item_name;
   private String relative_url;
   private URL item_url;
   private ItemRelation item_relation;
   private StringBuffer item_desc;

   SubItemImpl(ItemRelation rel) {
      item_name = null;
      item_url = null;
      item_relation = rel;
      item_desc = null;
    }

   void setUrl(URL base,String offset) {
      offset = offset.replace(" ","+");
      relative_url = offset;
      try {
	 item_url = new URL(base,offset);
       }
      catch (MalformedURLException e) {
	 BoardLog.logE("BDOC","Bad url " + offset);
       }
    }

   void setName(String s) {
      if (item_name == null) item_name = new StringBuffer();
      addText(item_name,s);
    }

   void addNameTag(HTML.Tag t,MutableAttributeSet a) {
      if (item_name == null) item_name = new StringBuffer();
      addTag(item_name,t,a);
    }

   void addNameEndTag(HTML.Tag t) {
      if (item_name != null) addEndTag(item_name,t);
    }

   void setDescription(String s) {
      if (item_desc == null) item_desc = new StringBuffer();
      addText(item_desc,s);
    }

   void addDescriptionTag(HTML.Tag t,MutableAttributeSet a) {
      if (item_desc == null) item_desc = new StringBuffer();
      addTag(item_desc,t,a);
    }

   void addDescriptionEndTag(HTML.Tag t) {
      if (item_desc != null) addEndTag(item_desc,t);
    }

   ItemRelation getRelation()			{ return item_relation; }

   @Override public String getName() {
      if (item_name == null) return null;
      return item_name.toString();
    }

   @Override public URL getItemUrl()		{ return item_url; }
   @Override public String getRelativeUrl()	{ return relative_url; }
   @Override public String getDescription() {
      if (item_desc == null) return null;
      return item_desc.toString();
    }

   @Override public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("<html>");
      if (item_url != null) buf.append("<a href='" + item_url.toString() + "'>");
      if (item_name != null) buf.append(item_name.toString());
      if (item_url != null) buf.append("</a>");
      if (item_desc != null) buf.append(item_desc.toString());
   
   
      return buf.toString();
    }


}	// end of inner class SubItemImpl





}	// end of abstract class BdocDocItem




/* end of BdocDocItem.java */

