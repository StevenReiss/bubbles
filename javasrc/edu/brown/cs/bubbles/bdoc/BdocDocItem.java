/********************************************************************************/
/*										*/
/*		BdocDocItem.java						*/
/*										*/
/*	Bubbles Environment Documentation bubbles javadoc generic item		*/
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


package edu.brown.cs.bubbles.bdoc;

import edu.brown.cs.bubbles.board.BoardLog;

import edu.brown.cs.ivy.xml.IvyXml;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;




abstract class BdocDocItem implements BdocConstants
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

private static Map<String,ItemRelation> sub_types;

private static final String [] HTML_REMOVALS = new String [] { "<P>", "<DL>", "<DD>", "</DD>", "</P>", "<HR>", " ", "\n" };


static {
   sub_types = new HashMap<>();
   sub_types.put("All Implemented Interfaces:",ItemRelation.IMPLEMENTS);
   sub_types.put("Type Parameters:",ItemRelation.TYPE_PARAMETERS);
   sub_types.put("See Also:",ItemRelation.SEE_ALSO);
   sub_types.put("All Known Subinterfaces:",ItemRelation.SUBINTERFACES);
   sub_types.put("All Known Implementing Classes:",ItemRelation.IMPLEMENTING_CLASS);
   sub_types.put("Direct Known Subclasses:",ItemRelation.SUBCLASS);
   sub_types.put("nested_class_summary",ItemRelation.NESTED_CLASS);
   sub_types.put("field_summary",ItemRelation.FIELD);
   sub_types.put("constructor_summary",ItemRelation.CONSTRUCTOR);
   sub_types.put("method_summary",ItemRelation.METHOD);
   sub_types.put("nested_classes_inherited_from_",ItemRelation.INHERITED_CLASS);
   sub_types.put("fields_inherited_from_",ItemRelation.INHERITED_FIELD);
   sub_types.put("methods_inherited_from_",ItemRelation.INHERITED_METHOD);
   sub_types.put("Parameters:",ItemRelation.PARAMETER);
   sub_types.put("Returns:",ItemRelation.RETURN);
   sub_types.put("Overrides:",ItemRelation.OVERRIDE);
   sub_types.put("Throws:",ItemRelation.THROW);
   sub_types.put("Specified by:",ItemRelation.SPECIFIED_BY);
}



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
   BoardLog.logD("BDOC","Work on " + u + " " + u.getRef());
   scanItem(u);
}


void scanItem(URL u)
{
   Document doc = null;
   try {
      doc = Jsoup.parse(u,10000);
    }
   catch (IOException e) { }
   if (doc == null) return;
   String id = u.getRef();
   Element start = null;
   if (id != null) {
      start = doc.getElementById(id);
      if (start == null) {
         id = id.replace("%3C","<");
         id = id.replace("%3E",">");
         start = doc.getElementById(id);
       }
      if (start != null && start.tagName().equals("a")) {
	 start = start.nextElementSibling();
       }
    }
   else {
      start = doc.getElementsByTag("main").first();
    }
   if (start != null) extractItem(start);
   else {
      BoardLog.logX("BDOC","No item found for " + u);
    }
}


void extractItem(Element elt)	    { }




/********************************************************************************/
/*										*/
/*	Description methods							*/
/*										*/
/********************************************************************************/

protected void addRawHtmlDivToDescription(Element e0,boolean br)
{
   if (item_description != null && e0 != null) {
      String html = e0.html();
      if (html != null) {
	 item_description.append("<div>");
	 item_description.append(html);
	 item_description.append("</div>");
	 if (br) item_description.append("<br/>");
       }
    }
}



/********************************************************************************/
/*										*/
/*	General subitm handling 						*/
/*										*/
/********************************************************************************/

protected void scanSubitems(Element e0)
{
   if (e0 == null) return;

   for (Element dtitm : e0.select(".notes > dt")) {
      scanSubItem(dtitm);
    }
   for (Element dtitm : e0.select(".blockList dt")) {
      scanSubItem(dtitm);
    }
}


protected void scanSignature(Element e0,String typ)
{
   if (e0 == null) return;
   Element sign = e0.select(typ).first();
   if (sign == null) sign = e0.select("pre").first();
   addRawHtmlDivToDescription(sign,true);
}


protected void scanBody(Element e0)
{
   Element body = e0.select(".block").first();
   addRawHtmlDivToDescription(body,false);
}



protected void scanSubItem(Element dt)
{
   if (dt == null) return;

   String typ = dt.text();
   ItemRelation lr = getListTypeFromName(typ);
   if (lr == ItemRelation.NONE) return;
   for (Element nitm = dt.nextElementSibling(); nitm != null; nitm = nitm.nextElementSibling()) {
      if (nitm.tagName().equals("dt")) break;
      if (nitm.tagName().equals("dd")) {
	 Element sitmlnk = nitm.select("a").first();
	 if (sitmlnk == null) continue;
	 String hr = sitmlnk.attr("href");
	 if (hr == null) continue;
	 SubItemImpl curitm = new SubItemImpl(lr);
	 curitm.setName(sitmlnk.text());
	 curitm.setUrl(ref_url,hr);
	 addSubitem(curitm);
       }
    }
}



protected ItemRelation getListTypeFromName(String nm)
{
   ItemRelation it = sub_types.get(nm);
   if (it == null) {
      int idx = nm.indexOf("_class_");
      if (idx < 0) idx = nm.indexOf("_interface_");
      if (idx >= 0) nm = nm.substring(0,idx+1);
      it = sub_types.get(nm);
    }
   if (it == null) it = ItemRelation.NONE;

   return it;
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

   void setDescription(String s) {
      if (item_desc == null) item_desc = new StringBuffer();
      addText(item_desc,s);
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

