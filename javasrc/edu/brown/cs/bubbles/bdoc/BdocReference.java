/********************************************************************************/
/*										*/
/*		BdocReference.java						*/
/*										*/
/*	Bubbles Environment Documentation reference to a javadoc item		*/
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


import edu.brown.cs.bubbles.bass.BassConstants;
import edu.brown.cs.bubbles.bass.BassNameBase;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.bump.BumpLocation;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;

import java.net.MalformedURLException;
import java.net.URL;


class BdocReference extends BassNameBase implements BdocConstants, BassConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private URL	ref_url;
private String	bdoc_name;
private String	name_parameters;
private String	ref_description;
private String	for_project;
private String	key_name;
private BdocRepository for_repository;


private static DescriptionData [] prefix_set = new DescriptionData [] {
   new DescriptionData("Variable in class ",BassNameType.FIELDS),
   new DescriptionData("Variable in record class ",BassNameType.FIELDS),
   new DescriptionData("Variable in enum class ",BassNameType.FIELDS),
   new DescriptionData("Variable in enum ",BassNameType.FIELDS),
   new DescriptionData("Variable in interface ",BassNameType.FIELDS),
   new DescriptionData("Variable in exception ",BassNameType.FIELDS),
   new DescriptionData("Variable in error ",BassNameType.FIELDS),
   new DescriptionData("Variable in annotation type ",BassNameType.FIELDS),
   new DescriptionData("Static variable in class ",BassNameType.FIELDS),
   new DescriptionData("Static variable in record class ",BassNameType.FIELDS),
   new DescriptionData("Static variable in enum class ",BassNameType.FIELDS),
   new DescriptionData("Static variable in enum ",BassNameType.FIELDS),
   new DescriptionData("Static variable in interface ",BassNameType.FIELDS),
   new DescriptionData("Static variable in exception ",BassNameType.FIELDS),
   new DescriptionData("Static variable in error ",BassNameType.FIELDS),
   new DescriptionData("Static variable in annotation type ",BassNameType.FIELDS),
   new DescriptionData("Static variable in annotation interface ",BassNameType.FIELDS),
   new DescriptionData("Element in annotation interface ",BassNameType.FIELDS),
   new DescriptionData("Method in class ",BassNameType.METHOD),
   new DescriptionData("Method in record class ",BassNameType.METHOD),
   new DescriptionData("Method in enum class ",BassNameType.METHOD),
   new DescriptionData("Method in enum ",BassNameType.METHOD),
   new DescriptionData("Method in interface ",BassNameType.METHOD),
   new DescriptionData("Method in exception ",BassNameType.METHOD),
   new DescriptionData("Method in error ",BassNameType.METHOD),
   new DescriptionData("Method in annotation type ",BassNameType.METHOD),
   new DescriptionData("Static method in class ",BassNameType.METHOD),
   new DescriptionData("Static method in record class ",BassNameType.METHOD),
   new DescriptionData("Static method in enum class ",BassNameType.METHOD),
   new DescriptionData("Static method in enum ",BassNameType.METHOD),
   new DescriptionData("Static method in interface ",BassNameType.METHOD),
   new DescriptionData("Static method in exception ",BassNameType.METHOD),
   new DescriptionData("Static method in error ",BassNameType.METHOD),
   new DescriptionData("Static method in annotation type ",BassNameType.METHOD),
   new DescriptionData("Constructor for class ",BassNameType.CONSTRUCTOR),
   new DescriptionData("Constructor for record class ",BassNameType.CONSTRUCTOR),
   new DescriptionData("Constructor for enum class ",BassNameType.CONSTRUCTOR),
   new DescriptionData("Constructor for enum ",BassNameType.CONSTRUCTOR),
   new DescriptionData("Constructor for exception ",BassNameType.CONSTRUCTOR),
   new DescriptionData("Constructor for error ",BassNameType.CONSTRUCTOR),
   new DescriptionData("Constructor for annotation type ",BassNameType.CONSTRUCTOR),
   new DescriptionData("Class in ",BassNameType.CLASS),
   new DescriptionData("Record Class in ",BassNameType.CLASS),
   new DescriptionData("Interface in ",BassNameType.INTERFACE),
   new DescriptionData("Exception in ",BassNameType.THROWABLE),
   new DescriptionData("Error in ",BassNameType.THROWABLE),
   new DescriptionData("Enum in ",BassNameType.ENUM),
   new DescriptionData("Enum Class in ",BassNameType.ENUM),
   new DescriptionData("Enum constant in enum class ",BassNameType.FIELDS),
   new DescriptionData("Annotation Type in ",BassNameType.ANNOTATION),
   new DescriptionData("Annotation Interface in ",BassNameType.ANNOTATION),
   new DescriptionData("package ",BassNameType.NONE),
   new DescriptionData("class ",BassNameType.CLASS),
   new DescriptionData("interface ",BassNameType.CLASS),
   new DescriptionData("module ",BassNameType.MODULE),
   new DescriptionData("Search tag in class ",BassNameType.NONE),
   new DescriptionData("Search tag in enum class ",BassNameType.NONE),
   new DescriptionData("Search tag in record class ",BassNameType.NONE),
};



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BdocReference(BdocRepository br,String proj,URL base,String ref,String desc) throws BdocException
{
   for_repository = br;
   for_project = proj;
   ref = ref.replace(" ","+");

   try {
      ref_url = new URL(base,ref);
    }
   catch (MalformedURLException e) {
      throw new BdocException("Bad URL for reference",e);
    }

   int idx = desc.indexOf("-");
   if (idx < 0) throw new BdocException("No break found in reference name");
   String local = desc.substring(0,idx).trim();
   desc = desc.substring(idx+1).trim();
   String inside = null;

   for (DescriptionData dd : prefix_set) {
      if (desc.startsWith(dd.getPrefix())) {
	 name_type = dd.getNameType();
	 inside = desc.substring(dd.getLength()).trim();
	 break;
       }
    }
   if (inside == null && !desc.contains(" ")) {
      // enum constants show up this way in some versions
      inside = desc;
    }
   if (inside == null && desc.contains("Search tag in ")) {
      name_type = BassNameType.NONE;
      int idx1 = desc.indexOf("Search tag in ") + "Search tag in ".length();
      inside = desc.substring(idx1).trim();
    }
   
   if (inside == null) {
      throw new BdocException("Unknown javadoc index element " + desc);
    }

   setName(local,inside,ref);

   ref_description = null;
}



BdocReference(BdocRepository br,Element xml)
{
   for_repository = br;
   name_type = IvyXml.getAttrEnum(xml,"TYPE",BassNameType.NONE);
   bdoc_name = IvyXml.getAttrString(xml,"NAME");
   name_parameters = IvyXml.getAttrString(xml,"PARAMS");
   ref_description = IvyXml.getAttrString(xml,"DESCRIPTION");
   key_name = IvyXml.getAttrString(xml,"KEY");
   for_project = IvyXml.getAttrString(xml,"PROJECT");
   String url = IvyXml.getAttrString(xml,"URL");
   if (url == null) ref_url = null;
   else {
      try {
	 ref_url = new URL(url);
       }
      catch (MalformedURLException e) { }
    }
}



BdocReference(BdocRepository br,String nt,String nm,String p,String d,String pro,String url)
{
   for_repository = br;
   if (nt == null) name_type = BassNameType.NONE;
   else if (nt == "MODULE") name_type = BassNameType.NONE;
   else {
      try {
	 name_type = Enum.valueOf(BassNameType.class,nt);
       }
      catch (IllegalArgumentException e) {
	 name_type = BassNameType.NONE;
       }
    }
   if (nm.contains("class ") || nm.contains(".base") || nm.contains("base.")) {
      System.err.println("CHECK HERE");
    }
   bdoc_name = nm;
   name_parameters = p;
   ref_description = d;
   key_name = null;
   for_project = pro;
   if (url == null) ref_url = null;
   else {
      try {
	 ref_url = new URL(url);
       }
      catch (MalformedURLException e) { }
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

URL getReferenceUrl()			{ return ref_url; }


@Override public String getDisplayName()
{
   return super.getDisplayName() + " (doc)";
}


BdocReference findRelatedReference(String newurl)
{
   try {
      URL u = new URL(ref_url,newurl);
      return for_repository.findReference(u);
    }
   catch (MalformedURLException e) { }

   return null;
}



BdocReference findRelatedReference(URL u)
{
   return for_repository.findReference(u);
}



/********************************************************************************/
/*										*/
/*	Description methods							*/
/*										*/
/********************************************************************************/

void addDescription(String s)
{
   if (ref_description == null) ref_description = s;
   else ref_description += s;
}




/********************************************************************************/
/*										*/
/*	Name interpretation methods						*/
/*										*/
/********************************************************************************/

private void setName(String lcl,String inside,String ref) throws BdocException
{
   int idx;
   
   key_name = null;
   name_parameters = null;
   idx = lcl.indexOf("(");
   if (idx >= 0) {
      name_parameters = lcl.substring(idx).trim();
      lcl = lcl.substring(0,idx).trim();
    }

   switch (name_type) {
      case PACKAGE :
	 bdoc_name = lcl;
	 return;
      case CLASS :
      case ENUM :
      case INTERFACE :
      case THROWABLE :
      case ANNOTATION :
	 bdoc_name = inside + "." + lcl.replace('.','$');
	 return;
      case CONSTRUCTOR :
	 idx = lcl.indexOf(".");
	 if (idx >= 0) {
	    lcl = lcl.substring(idx+1);
	  }
	 break;
      default:
	 break;
    }

   idx = ref.lastIndexOf("#");
   if (idx >= 0) ref = ref.substring(0,idx);
   idx = ref.lastIndexOf(".");
   if (idx < 0) throw new BdocException("No extension on reference URL");
   ref = ref.substring(0,idx);
   idx = ref.lastIndexOf("/");
   if (idx < 0) throw new BdocException("No path on reference URL");
   ref = ref.substring(idx+1);
   idx = -1;

   for ( ; ; ) {
      idx = ref.indexOf(".",idx+1);
      if (idx < 0) break;
      int jidx = inside.lastIndexOf(".");
      if (jidx < 0) break;
      inside = inside.substring(0,jidx) + "$" + inside.substring(jidx+1);
    }

   bdoc_name = inside + "." + lcl;
}




/********************************************************************************/
/*										*/
/*	BassNameBase methods							*/
/*										*/
/********************************************************************************/

@Override public String getProject()		{ return null; }
@Override protected String getSymbolName()	{ return BDOC_DOC_PREFIX + bdoc_name; }
@Override protected String getParameters()	{ return name_parameters; }

@Override protected String getKey()
{
   if (key_name == null) {
      key_name = bdoc_name;
      if (name_parameters != null) key_name += name_parameters;
    }
   return key_name;
}


@Override protected String getLocalName()
{
   switch (name_type) {
      case CLASS :
	 return " (class)";
      case ENUM :
	 return " (enum)";
      case INTERFACE :
	 return " (interface)";
      case PACKAGE :
	 return " (package)";
      case THROWABLE :
	 return " (throwable)";
      case ANNOTATION :
         return " (annotation)";
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
      case PACKAGE :
      case ANNOTATION :
	 String nm = getUserSymbolName();
	 int idx = nm.indexOf("<");
	 if (idx > 0) nm = nm.substring(0,idx);
	 return nm;
      default:
	 break;
    }

   return super.getNameHead();
}

/**
 * Priority Method
 */

@Override public int getSortPriority() { return BDOC_DOC_PRIORITY; }


/**
 * Method to access the digested name of the doc
 */
String getDigestedName()	 { return bdoc_name; }



/********************************************************************************/
/*										*/
/*	Name matching methods							*/
/*										*/
/********************************************************************************/

boolean matchName(String nm)
{
   int idx = nm.indexOf("(");
   
   String bnm = bdoc_name;
   boolean mtch = nm.startsWith(bnm);
   if (!mtch) {
      int idx1 = bnm.indexOf('<');
      if (idx1 > 1) {
	 bnm = bnm.substring(0,idx1);
	 mtch = nm.startsWith(bnm);
       }
    }
   if (!mtch) return false;
   
   if (idx < 0) {
      // if there are no parameters, insist on exact match
      return (bnm.length() == nm.length());
    }
     
   if (bnm.length() != idx) return false;

   return BumpLocation.compareParameters(name_parameters,nm.substring(idx));
}



/********************************************************************************/
/*										*/
/*	Bubble creation methods 						*/
/*										*/
/********************************************************************************/

@Override public BudaBubble createBubble()
{
   try {
      return new BdocBubble(this);
    }
   catch (BdocException e) {
      BoardLog.logE("BDOC","Problem creating doc bubble",e);
    }

   return null;
}


@Override public BudaBubble createPreviewBubble()
{
   return createBubble();
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(IvyXmlWriter xw)
{
   xw.begin("BDOC");
   xw.field("TYPE",name_type);
   if (bdoc_name != null) xw.field("NAME",bdoc_name);
   if (name_parameters != null) xw.field("PARAMS",name_parameters);
   if (for_project != null) xw.field("PROJECT",for_project);
   if (ref_url != null) xw.textElement("URL",ref_url.toExternalForm());
   if (ref_description != null) xw.textElement("DESCRIPTION",ref_description);
   xw.end("BDOC");
}




/********************************************************************************/
/*										*/
/*	Description Data for parsing						*/
/*										*/
/********************************************************************************/

private static class DescriptionData {

   private String data_prefix;
   private BassNameType name_type;

   DescriptionData(String pfx,BassNameType nt) {
      data_prefix = pfx;
      name_type = nt;
    }

   String getPrefix()			{ return data_prefix; }
   BassNameType getNameType()		{ return name_type; }
   int getLength()			{ return data_prefix.length(); }

}	// end of inner class DescriptionData




}	// end of class BdocReference



/* end of BdocReference.java */
