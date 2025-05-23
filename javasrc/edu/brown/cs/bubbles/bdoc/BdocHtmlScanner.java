/********************************************************************************/
/*                                                                              */
/*              BdocHtmlScanner.java                                            */
/*                                                                              */
/*      Handle Jsoup-based HTML scanning                                        */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
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



package edu.brown.cs.bubbles.bdoc;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.ivy.file.IvyFile;

class BdocHtmlScanner implements BdocConstants
{


/********************************************************************************/
/*                                                                              */
/*      Scanning of Index Files                                                 */
/*                                                                              */
/********************************************************************************/

static List<BdocReference> scanIndex(URI u,BdocRepository repo,String project,boolean opt)
{
   List<BdocReference> rslt = new ArrayList<>();
   String text = null;
   Document doc = null;
   try {
      // this is more reliable than using Jsoup.parse on a URL for some reason
      URL url = u.toURL();
      InputStream ins = url.openStream();
      text = IvyFile.loadFile(ins);
      doc = Jsoup.parse(text);
    }
   catch (Throwable t) {
      if (!opt) BoardLog.logE("BDOC","Problem scanning index " + u,t);
    }
   
   if (doc == null) {
      try {
         doc = Jsoup.parse(u.toURL(),100000);
       }
      catch (Throwable e) { 
         if (!opt) BoardLog.logE("BDOC","Problem scanning index " + u,e);
       }
    }
   if (doc == null) return rslt;
   
   for (Element el : doc.select("dt")) {
      String refurl = null;
      String reftitle = null;
      String refdesc = null;
      for (Element ela : el.select("a")) {
         String href = ela.attr("href");
         if (href == null) break;
         href = href.replace(" ","+");
         refurl = href;
         break;
       }
      if (refurl == null) continue;
      reftitle = el.text();
      for (Element eld = el.nextElementSibling(); eld !=  null; eld = eld.nextElementSibling()) {
         if (eld.tagName().equals("dd")) {
            refdesc = eld.text();
            try {
               BdocReference bref = new BdocReference(repo,project,u,refurl,reftitle);
               switch (bref.getNameType()) {
                  case NONE :
                     break;
                  case MODULE :
                     break;
                  default :
                     bref.addDescription(refdesc);
                     rslt.add(bref);
                     break;
                }
             }
            catch (BdocException e) {
               BoardLog.logE("BDOC","Problem with javadoc reference: " + e);
             }
            break;
          }
         else if (eld.tagName().equals("dt")) {
            break;
          }
       }
    }
   
   return rslt;
}





}       // end of class BdocHtmlScanner




/* end of BdocHtmlScanner.java */

