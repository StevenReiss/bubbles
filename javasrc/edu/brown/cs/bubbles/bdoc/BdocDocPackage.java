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

import org.jsoup.nodes.Element;

import edu.brown.cs.bubbles.board.BoardLog;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


class BdocDocPackage extends BdocDocItem implements BdocConstants
{


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
/*                                                                              */
/*      JSoup extraction methods                                                */
/*                                                                              */
/********************************************************************************/

@Override void extractItem(Element e0)
{
   if (e0 == null) return;
   
   Element desc = e0.select(".package-description").first();
   if (desc == null) desc = e0.select(".description").first();
   if (desc == null) desc = e0.select(".contentContainer section").first();
   scanSubitems(desc);
   scanSignature(e0,".package-signature");
   scanBody(e0);  
  // handle Java17 code
   List<ItemRelation> typs = new ArrayList<>();
   for (Element belt : e0.select(".table-tabs < button")) {
      String typ = belt.text();
      switch (typ) {
         case "All Classes and Interfaces" :
            typs.add(ItemRelation.NONE);
            break;
         case "Interfaces" :
            typs.add(ItemRelation.PACKAGE_INTERFACE);
            break;
         case "Classes" :
            typs.add(ItemRelation.PACKAGE_CLASS);
            break;
         case "Enum Classes" :
            typs.add(ItemRelation.PACKAGE_ENUM);
            break;
         case "Exceptions" :
            typs.add(ItemRelation.PACKAGE_EXCEPTION);
            break;
         case "Errors" :
            typs.add(ItemRelation.PACKAGE_ERROR);
            break;
         default :
            BoardLog.logE("BDOC","Unknown tab button " + typ);
            typs.add(ItemRelation.NONE);
            break;
       }
    }
   for (Element celt : e0.select(".col-first > a")) {
      String hr = celt.attr("href");
      if (hr == null) continue;
      ItemRelation ir = ItemRelation.NONE;
      String nm = celt.text();
      Element div = celt.parent();
      for (int i = 1; i < typs.size(); ++i) {
         String key = ".class-summary-tab" + i;
         if (div.classNames().contains(key)) {
            ir = typs.get(i);
          }
       }
      if (ir == ItemRelation.NONE) continue;
      Element ddiv = div.nextElementSibling();
      SubItemImpl curitm = new SubItemImpl(ir);
      curitm.setName(nm);
      curitm.setUrl(ref_url,hr);
      curitm.setDescription(ddiv.html());
      addSubitem(curitm);
    }
   
   if (typs.size() == 0) {
      // handle Java10 description
      for (Element tbl : e0.select(".typeSummary")) {
         String what = tbl.select("span").first().text();
         ItemRelation ir = ItemRelation.NONE;
         switch (what) {
            case "Class Summary" :
               ir = ItemRelation.PACKAGE_CLASS;
               break;
            case "Interface Summary" :
               ir = ItemRelation.PACKAGE_INTERFACE;
               break;
            case "Enum Summary" :
               ir = ItemRelation.PACKAGE_ENUM;
               break;
            case "Exception Summary" :
               ir = ItemRelation.PACKAGE_EXCEPTION;
               break;
            case "Error Summary" :
               ir = ItemRelation.PACKAGE_ERROR;
               break;
            default :
               BoardLog.logE("BDOC","Unknown table caption " + what);
               break;
          }
         if (ir == ItemRelation.NONE) continue;
         for (Element telt : tbl.select("tr")) {
            Element pfx = telt.select("th > a").first();
            Element sfx = telt.select("td > div").first();
            String nm = pfx.text();
            String hr = pfx.attr("href");
            if (hr == null) continue;
            SubItemImpl curitm = new SubItemImpl(ir);
            curitm.setName(nm);
            curitm.setUrl(ref_url,hr);
            curitm.setDescription(sfx.html());
            addSubitem(curitm);
          }
       }
    }
}




}	// end of class BdocDocPackage




/* end of BdocDocPackage.java */


