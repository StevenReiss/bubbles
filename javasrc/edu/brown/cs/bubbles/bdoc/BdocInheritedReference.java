/********************************************************************************/
/*										*/
/*		BdocInherited Reference.java					*/
/*										*/
/*	Bubbles Environment Documentation reference to an inherited item	*/
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
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.bump.BumpLocation;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.net.URL;


class BdocInheritedReference extends BassNameBase implements BdocConstants, BassConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BdocReference base_reference;
private String	sub_class;
private String	bdoc_name;
private String	key_name;
private String	name_head;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BdocInheritedReference(BdocReference br,String cls)
{
   base_reference = br;
   sub_class = cls;
   key_name = null;

   bdoc_name = sub_class + "." + base_reference.getLocalName();
   bdoc_name = bdoc_name.replace("$",".");
   name_type = br.getNameType();
   name_head = BDOC_DOC_PREFIX + sub_class;
}



/********************************************************************************/
/*										*/
/*	BassNameBase methods							*/
/*										*/
/********************************************************************************/

@Override public String getProject()		{ return base_reference.getProject(); }

@Override protected String getSymbolName()
{
   return BDOC_DOC_PREFIX + bdoc_name;
}

@Override protected String getParameters()	{ return base_reference.getParameters(); }

@Override protected String getKey()
{
   if (key_name == null) {
      key_name = bdoc_name;
      if (getParameters() != null) key_name += getParameters();
    }
   return key_name;
}


@Override protected String getLocalName()	{ return base_reference.getLocalName(); }

@Override public String getNameHead()		{ return name_head; }


/**
 * Priority Method
 */

@Override public int getSortPriority() { return BDOC_DOC_PRIORITY_INHERIT; }


/**
 * Method to access the digested name of the doc
 */
String getDigestedName()	 { return bdoc_name; }



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

URL getReferenceUrl()			{ return base_reference.getReferenceUrl(); }


@Override public String getDisplayName()
{
   return super.getDisplayName() + " (doc)";
}


BdocReference findRelatedReference(String newurl)
{
   return base_reference.findRelatedReference(newurl);
}



BdocReference findRelatedReference(URL u)
{
   return base_reference.findRelatedReference(u);
}



/********************************************************************************/
/*										*/
/*	Name matching methods							*/
/*										*/
/********************************************************************************/

boolean matchName(String nm)
{
   int idx = nm.indexOf("(");
   if (idx < 0) return nm.equals(bdoc_name);
   if (!nm.startsWith(bdoc_name)) return false;
   if (bdoc_name.length() != idx) return false;

   return BumpLocation.compareParameters(getParameters(),nm.substring(idx));
}



/********************************************************************************/
/*										*/
/*	Bubble creation methods 						*/
/*										*/
/********************************************************************************/

@Override public BudaBubble createBubble()
{
   return base_reference.createBubble();
}


@Override public BudaBubble createPreviewBubble()
{
   return base_reference.createPreviewBubble();
}


@Override public String createPreviewString()
{
   return base_reference.createPreviewString();
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(IvyXmlWriter xw)
{
   xw.begin("INHERIT");
   xw.field("TYPE",name_type);
   if (bdoc_name != null) xw.field("NAME",bdoc_name);
   if (getParameters() != null) xw.field("PARAMS",getParameters());
   xw.field("BASE",base_reference.getDigestedName());
   xw.end("INHERIT");
}




}	// end of class BdocReference



/* end of BdocReference.java */
