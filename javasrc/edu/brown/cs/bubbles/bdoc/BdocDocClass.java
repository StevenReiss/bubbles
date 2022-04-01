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



import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URL;


class BdocDocClass extends BdocDocItem implements BdocConstants
{


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
/*                                                                              */
/*      JSoup parsing methods                                                   */
/*                                                                              */
/********************************************************************************/

@Override void extractItem(Element e0)
{
   for (Element sups : e0.select(".inheritance")) {
      String suptxt = sups.text();
      Element suplnk = sups.select("a").first();
      if (suplnk == null) continue;
      SubItemImpl sitm = new SubItemImpl(ItemRelation.SUPERTYPE);
      String hr = suplnk.attr("href");
      sitm.setUrl(ref_url,hr);
      sitm.setName(suptxt);
      addSubitem(sitm);
    }
   Element desc = e0.select(".class-description").first();
   if (desc == null) desc = e0.select(".classDescription").first();
   if (desc == null) desc = e0.select(".description").first();
   scanSubitems(desc);
   scanSignature(e0,".type-signature");
   scanBody(e0);
}

 

}	// end of class BdocDocClass




/* end of BdocDocClass.java */
