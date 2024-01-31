/********************************************************************************/
/*										*/
/*		BdocRepository.java						*/
/*										*/
/*	Bubbles Environment Documentation repository of available javadocs	*/
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

import edu.brown.cs.bubbles.bass.BassConstants.BassRepository;
import edu.brown.cs.bubbles.bass.BassName;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.bump.BumpClient;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;



class BdocRepository implements BassRepository, BdocConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Map<String,BdocReference> all_items;
private Set<String>             ref_names;
private Collection<BdocInheritedReference> inherited_items;
private int			ready_count;
private boolean 		cache_repository;
private List<String>		bdoc_props;
private boolean                 have_javase;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BdocRepository()
{
   all_items = new HashMap<String,BdocReference>();
   inherited_items = new ArrayList<BdocInheritedReference>();
   cache_repository = true;
   have_javase = false;
   ref_names = new HashSet<>();

   bdoc_props = new ArrayList<String>();
   Set<String> libdocs = new HashSet<String>();
   List<String> plist = new ArrayList<String>();
   plist.add("Bdoc.javadoc.");
   BumpClient bc = BumpClient.getBump();
   Element e = bc.getAllProjects();
   for (Element pe : IvyXml.children(e,"PROJECT")) {
      String nm = IvyXml.getAttrString(pe,"NAME");
      Element e1 = bc.getProjectData(nm,false,true,false,false,false);
      if (e1 != null) {
	 Element e2 = IvyXml.getChild(e1, "CLASSPATH");
	 for (Element e3 : IvyXml.children(e2,"PATH")) {
	    String jd = IvyXml.getTextElement(e3,"JAVADOC");
	    if (jd != null) {
	       BoardLog.logD("BDOC", "Eclipse javadoc: " + jd);
	       libdocs.add(jd);
	     }
	 }
      }
      if (nm != null) {
	 nm = nm.replace(" ","_");
	 plist.add("Bdoc." + nm + ".javadoc.");
       }
    }
   // should get javadocs eclipse knows for project at this point

   BoardProperties bp = BoardProperties.getProperties("Bdoc");
   for (String s : bp.stringPropertyNames()) {
      boolean use = false;
      for (String ns : plist) {
	 if (s.startsWith(ns)) {
	    use = true;
	    break;
	  }
       }
      if (use) {
	 String nm = bp.getProperty(s);
	 if (nm != null) bdoc_props.add(s);
       }
    }

   handleRemoteAccess();

   File f = BoardSetup.getDocumentationFile();
   String cf = bp.getProperty("Bdoc.doc.file");
   if (cf != null) {
      File xf = new File(cf);
      if (xf.exists()) f = xf;
    }

   if (loadXml(f)) {
      ready_count = 0;
      return;
    }

   ready_count = 2;

   for (String s : bdoc_props) {
      String nm = bp.getProperty(s);
      if (nm != null) {
	 BoardLog.logD("BDOC", "Property Javadoc " + s + ":" + nm);
	 addJavadoc(nm,false);
      }
    }
   waitForSearches();

   for (String s : libdocs) {
      if (!s.startsWith("http") && !s.startsWith("file:")) {
	 if (s.startsWith("/")) {
	    s = "file://" + s;
	  }
	 else continue;
       }
      else if (s.startsWith("file:")) {
	 if (s.startsWith("file:/") && !s.startsWith("file:///")) {
	    s = s.substring(5);
	    s = "file://" + s;
	 }
	 if (!s.endsWith(".jar") && !s.endsWith("/")) s = s + "/";
      }
      addJavadoc(s,true);
    }
   noteSearcherDone();

   noteSearcherDone();
   
   ref_names = null;
}




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

void addJavadoc(String url,boolean optional)
{
   if (url == null) return;

   if (url.contains("/javase/")) {
      if (have_javase) return;
      have_javase = true;
    }
   
   try {
      URI u = new URI(url);
      addJavadoc(u,null,optional);
    }
   catch (URISyntaxException e) { }
}



void addJavadoc(URI u)			{ addJavadoc(u,null,false); }


synchronized void addJavadoc(URI u,String proj,boolean optional)
{
   BoardLog.logD("BDOC","Add javadoc for " + u);
   
   Searcher s = new Searcher(u,proj,optional);

   ++ready_count;

   BoardThreadPool.start(s);
}



private synchronized void noteSearcherDone()
{
   if (ready_count == 1) addHierarchyLinks();

   --ready_count;

   if (ready_count == 0) {
      notifyAll();
      File f = BoardSetup.getDocumentationFile();
      if (f != null && cache_repository) outputXml(f);
      ref_names = null;
    }
   else if (ready_count == 2) notifyAll();
}


private synchronized void waitForSearches()
{
   while (ready_count > 2) {
      try {
	 wait();
      }
      catch(InterruptedException e) { }
   }
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public synchronized Iterable<BassName> getAllNames()
{
   waitForReady();

   ArrayList<BassName> rslt = new ArrayList<BassName>(all_items.values());

   rslt.addAll(inherited_items);

   return rslt;
}


@Override public boolean isEmpty()
{
   waitForReady();
   return all_items.isEmpty() && inherited_items.isEmpty();
}


@Override public boolean includesRepository(BassRepository br)	{ return br == this; }



BdocReference findReference(URI u)
{
   waitForReady();

   String uid = u.toString();

   return all_items.get(uid);
}



synchronized BdocReference findReferenceFromName(String name)
{
   waitForReady();

   for (BdocReference br : all_items.values()) {
      if (br.matchName(name)) return br;
    }

   //TODO: This might have to be more approximate

   return null;
}




synchronized void waitForReady()
{
   while (ready_count > 0) {
      try {
	 wait();
       }
      catch (InterruptedException e) { }
    }
}




/********************************************************************************/
/*										*/
/*	Loading methods 							*/
/*										*/
/********************************************************************************/

private void loadJavadoc(URI u,String p,boolean optional)
{
   URI u1;

   try {
      u1 = new URI(u.getScheme(),u.getAuthority(),u.getPath() + "/index-all.html",null,null);
      if (loadJavadocFile(u1,p,optional)) return;
    }
   catch (URISyntaxException e) {
      BoardLog.logE("BDOC","Bad javadoc url: " + e);
      return;
    }

   for (int i = 1; i <= 27; ++i) {
      try {
	 u1 = new URI(u.getScheme(),u.getAuthority(),u.getPath() + "/index-files/index-" + i + ".html"); 
	 loadJavadocFile(u1,p,optional);
       }
      catch (URISyntaxException e) { }
    }
}




private boolean loadJavadocFile(URI u,String p,boolean optional)
{
   BoardLog.logD("BDOC","Load documentation from " + u);
   List<BdocReference> refs = BdocHtmlScanner.scanIndex(u,this,p);
   if (refs.isEmpty()) return false;
   for (BdocReference br : refs) {
      if (checkReference(br)) addReference(br);
    }

   return true;
}



private synchronized void addReference(BdocReference br)
{
   all_items.put(br.getReferenceUrl().toString(),br);
   if (ref_names != null) ref_names.add(br.toString());
}


private synchronized boolean checkReference(BdocReference br)
{
   switch (br.getNameType()) {
      case CLASS :
      case INTERFACE :
      case ENUM :
      case ANNOTATION :
      case THROWABLE :
      case OTHER_CLASS :
      case CONSTRUCTOR :
      case METHOD :
      case MAIN_PROGRAM :
      case FIELDS :
      case VARIABLES :
      case PACKAGE :
      case STATICS :
         break;
      case NONE :
         return false;
      case MODULE :
         return false;
      default :
         BoardLog.logE("BDOC","Unexpected name type " + br.getNameType() + " " + br);
         return false;
    }
   
   if (ref_names == null) return true;
   return !ref_names.contains(br.toString());
}




/********************************************************************************/
/*										*/
/*	Methods to handle hierarchical links					*/
/*										*/
/********************************************************************************/

private void addHierarchyLinks()
{
   Set<String> pkgs = new HashSet<String>();
   Set<String> clss = new HashSet<String>();

   for (BdocReference br : all_items.values()) {
      switch (br.getNameType()) {
	 case PACKAGE :
	    pkgs.add(br.getDigestedName());
	    break;
	 case CLASS :
	 case INTERFACE :
	 case ANNOTATION :
	 // case ENUM :
	    clss.add(br.getDigestedName());
	    break;
	 default :
	    break;
       }
    }

   BumpClient bc = BumpClient.getBump();
   Hierarchy hier = new Hierarchy();

   for (String pkg : pkgs) {
      Element xml = bc.getTypeHierarchy(null,pkg,null,false);
      hier.loadRelations(xml);
    }

   MemberInfo mif = new MemberInfo();
   for (BdocReference br : all_items.values()) {
      mif.addReference(br);
    }

   for (String cls : hier.getClasses()) {
      if (!clss.contains(cls)) continue;
      Set<String> done = new HashSet<String>();
      for (String scls : hier.getSupers(cls)) {
	 if (scls.equals("java.lang.Object")) continue;
	 for (String mthd : mif.getMethods(scls)) {
	    if (!done.contains(mthd) && mif.getReferences(cls,mthd) == null) {
	       done.add(mthd);
	       for (BdocReference base : mif.getReferences(scls,mthd)) {
		  BdocInheritedReference bir = new BdocInheritedReference(base,cls);
		  inherited_items.add(bir);
		}
	     }
	  }
       }
    }
}




private static class Hierarchy {

   private Map<String,Set<String>>	class_hierarchy;

   Hierarchy() {
      class_hierarchy = new HashMap<>();
    }

   Collection<String> getClasses()		{ return class_hierarchy.keySet(); }

   Collection<String> getSupers(String cls)	{ return class_hierarchy.get(cls); }

   void loadRelations(Element xml) {
      for (Element ce : IvyXml.children(xml,"TYPE")) {
         String nm = IvyXml.getAttrString(ce,"NAME");
   
         Set<String> sups = class_hierarchy.get(nm);
         if (sups == null) {
            sups = new LinkedHashSet<String>();
            class_hierarchy.put(nm,sups);
          }
   
         // String k = IvyXml.getAttrString(ce,"KIND");
         // might want to restrict to classes
         for (Element se : IvyXml.children(ce,"SUPERTYPE")) {
            String sn = IvyXml.getAttrString(se,"NAME");
            sups.add(sn);
          }
         for (Element se : IvyXml.children(ce,"EXTENDIFACE")) {
            String sn = IvyXml.getAttrString(se,"NAME");
            sups.add(sn);
          }
       }
    }

}	// end of inner class Hierarchy




private static class MemberInfo {

   private Map<String,Map<String,List<BdocReference>>> ref_byclass;

   MemberInfo() {
      ref_byclass = new HashMap<String,Map<String,List<BdocReference>>>();
    }

   Collection<String> getMethods(String cls) {
      Map<String,List<BdocReference>> mls = ref_byclass.get(cls);
      if (mls == null) return new ArrayList<String>();
      return mls.keySet();
    }

   List<BdocReference> getReferences(String cls,String mthd) {
      Map<String,List<BdocReference>> mls = ref_byclass.get(cls);
      if (mls == null) return null;
      return mls.get(mthd);
    }

   void addReference(BdocReference br) {
      switch (br.getNameType()) {
         case METHOD :
            break;
         case FIELDS :
         case VARIABLES :
            return;				// might want to do inherited fields
         case CONSTRUCTOR :
            return;
         default :
            return;
       }
   
      String nm = br.getDigestedName(); 	// without parameters
      int idx = nm.lastIndexOf(".");
      if (idx < 0) return;
      String cls = nm.substring(0,idx);
      String itm = nm.substring(idx+1);
      Map<String,List<BdocReference>> mems = ref_byclass.get(cls);
      if (mems == null) {
         mems = new HashMap<String,List<BdocReference>>();
         ref_byclass.put(cls,mems);
       }
      List<BdocReference> refs = mems.get(itm);
      if (refs == null) {
         refs = new ArrayList<BdocReference>(2);
         mems.put(itm,refs);
       }
      refs.add(br);
    }

}	// end of inner class MemberInfo




/********************************************************************************/
/*										*/
/*	Input/Output methods							*/
/*										*/
/********************************************************************************/

private void outputXml(File f)
{
   try {
      IvyXmlWriter xw = new IvyXmlWriter(f);

      xw.outputHeader();

      xw.begin("JAVADOC");
      xw.field("WHEN",System.currentTimeMillis());

      BoardProperties bp = BoardProperties.getProperties("Bdoc");
      for (String s : bdoc_props) {
	 String nm = bp.getProperty(s);
	 if (nm != null) xw.textElement("SOURCE",nm);
       }

      for (BdocReference br : all_items.values()) {
	 br.outputXml(xw);
       }

      for (BdocInheritedReference ibr : inherited_items) {
	 ibr.outputXml(xw);
       }

      xw.end("JAVADOC");

      xw.close();
    }
   catch (IOException e) {
      BoardProperties bpx = BoardProperties.getProperties("System");
      BoardLog.logE("BDOC","Problem outputing repository: " + f + " " +
		       BoardSetup.getPropertyBase() + " " +
		       bpx.getProperty("edu.brown.cs.bubbles.workspace") + " " +
		       bpx.getProperty("edu.brown.cs.bubbles.install"),e);
    }
}


private boolean loadXml(File f)
{
   if (f == null || !f.exists()) return false;

   Set<String> sources = new HashSet<String>();
   BoardProperties bp = BoardProperties.getProperties("Bdoc");
   boolean check = bp.getBoolean("Bdoc.check.dates",true);

   // check if stored documentation file is out of date
   long dlm = f.lastModified();
   for (String pnm : bdoc_props) {
      String nm = bp.getProperty(pnm);
      if (nm != null) {
	 sources.add(nm);
	 try {
	    URI u = new URI(nm);
	    if (u.getScheme().equals("file")) {
	       File f0 = new File(u.getPath());
	       if (!f0.exists() || (check && f0.lastModified() > dlm)) {
		  BoardLog.logD("BDOC","Update doc because of " + f0);
		  return false;
		}
	     }
	    else if (u.getScheme().equals("http")) {
	       try {
		  URL url = u.toURL();
		  HttpURLConnection c = (HttpURLConnection) url.openConnection();
		  c.setIfModifiedSince(dlm);
		  c.setInstanceFollowRedirects(true);
		  int cd = c.getResponseCode();
		  if (cd == HttpURLConnection.HTTP_OK) {
		     long ndlm = c.getLastModified();
		     if (check && ndlm > dlm) {
			BoardLog.logD("BDOC","Update doc because of " + u);
			return false;
		      }
		   }
		  else if (cd >= 400) {
		     BoardLog.logD("BDOC","Update doc because of error " + cd + " on " + u);
		     return false;
		   }
		}
	       catch (IOException e) {
		  // ignore bad connection -- use cached value
		}
	     }
	  }
	 catch (URISyntaxException e) {
	    BoardLog.logD("BDOC","Update doc because of uri syntax error " + e);
	    return false;
	  }
       }
     }

   // if stored file is current, load it
   SAXParserFactory spf = SAXParserFactory.newInstance();
   spf.setValidating(false);
   spf.setXIncludeAware(false);
   spf.setNamespaceAware(false);

   BdocLoader ldr = new BdocLoader(sources);

   try {
      SAXParser sp = spf.newSAXParser();
      FileInputStream fis = new FileInputStream(f);
      InputSource ins = new InputSource(fis);
      ins.setEncoding("UTF-8");
      sp.parse(ins,ldr);
      fis.close();
    }
   catch (SAXException e) {
      BoardLog.logE("BDOC","Problem parsing saved repository");
      return false;
    }
   catch (ParserConfigurationException e) {
      BoardLog.logE("BDOC","Problem configuring parser",e);
      return false;
    }
   catch (IOException e) {
      BoardLog.logE("BDOC","Problem reading saved repository",e);
      return false;
    }

   return ldr.isValid();
}



/********************************************************************************/
/*										*/
/*	Handle remote access of documentation file				*/
/*										*/
/********************************************************************************/

private void handleRemoteAccess()
{
   BoardSetup bs = BoardSetup.getSetup();

   switch (bs.getRunMode()) {
      case CLIENT :
	 BumpClient bc = BumpClient.getBump();
	 File f = BoardSetup.getDocumentationFile();
	 if (f.exists()) break;
	 bc.getRemoteFile(f,"BDOC",null);
	 // ignore failures -- we'll just load the doc ourselves
	 break;
      default:
	 break;
    }
}





/********************************************************************************/
/*										*/
/*	Searcher thread 							*/
/*										*/
/********************************************************************************/

private class Searcher implements Runnable {

   private URI base_url;
   private String for_project;
   private boolean is_optional;

   Searcher(URI u,String p,boolean optional) {
      base_url = u;
      for_project = p;
      is_optional = optional;
    }

   @Override public void run() {
      loadJavadoc(base_url,for_project,is_optional);
      noteSearcherDone();
    }

   @Override public String toString() {
      return "BDOC_Searcher_" + base_url;
    }

}	// end of inner class Searcher



/********************************************************************************/
/*										*/
/*	BdocLoader class to handle XML parsing					*/
/*										*/
/********************************************************************************/

enum TextItem {
   NONE, SOURCE, URL, DESCRIPTION
}



private class BdocLoader extends DefaultHandler {

   private Map<String,BdocReference> base_map;
   private Set<String> source_list;
   private TextItem cur_text;
   private String doc_type;
   private String doc_name;
   private String doc_params;
   private String doc_url;
   private String doc_description;
   private String doc_project;
   private StringBuffer cur_buffer;
   private Boolean is_valid;

   BdocLoader(Set<String> sources) {
      source_list = sources;
      cur_text = TextItem.NONE;
      cur_buffer = null;
      is_valid = null;
      base_map = new HashMap<String,BdocReference>();
    }

   boolean isValid() {
      return is_valid == Boolean.TRUE;
    }

   @Override public void startElement(String uri,String lnm,String qnm,Attributes attrs) {
      if (qnm.equals("SOURCE")) {
	 cur_text = TextItem.SOURCE;
       }
      else if (qnm.equals("BDOC")) {
	 if (is_valid == null) {
	    is_valid = Boolean.valueOf(source_list.isEmpty());
	  }
	 if (is_valid) {
	    doc_type = attrs.getValue("TYPE");
	    doc_name = attrs.getValue("NAME");
	    doc_params = attrs.getValue("PARAMS");
	    doc_project = attrs.getValue("PROJECT");
	  }
       }
      else if (is_valid == Boolean.TRUE) {
	 if (qnm.equals("INHERIT")) {
	    String nm = attrs.getValue("NAME");
	    String prms = attrs.getValue("PARAMS");
	    String base = attrs.getValue("BASE");
	    String k = base;
	    if (prms != null) k += prms;
	    BdocReference br = base_map.get(k);
	    int idx = nm.lastIndexOf(".");
	    if (idx >= 0 && br != null) {
	       String cls = nm.substring(0,idx);
	       BdocInheritedReference bir = new BdocInheritedReference(br,cls);
	       inherited_items.add(bir);
	     }
	  }
	 else if (qnm.equals("URL")) cur_text = TextItem.URL;
	 else if (qnm.equals("DESCRIPTION")) cur_text = TextItem.DESCRIPTION;
       }
    }

   @Override public void endElement(String uri,String lnm,String qnm) {
      switch (cur_text) {
	 case NONE :
	    break;
	 case SOURCE :
	    if (cur_buffer != null) {
	       String src = cur_buffer.toString();
	       if (!source_list.remove(src)) is_valid = false;
	     }
	    break;
	 case URL :
	    if (cur_buffer != null) doc_url = cur_buffer.toString();
	    break;
	 case DESCRIPTION :
	    if (cur_buffer != null) doc_description = cur_buffer.toString();
	    break;
       }
      cur_text = TextItem.NONE;
      cur_buffer = null;
      if (qnm.equals("BDOC")) {
	 if (is_valid) {
	    BdocReference br = new BdocReference(BdocRepository.this,
						    doc_type,doc_name,doc_params,
						    doc_description,doc_project,doc_url);
	    addReference(br);
	    String k = br.getDigestedName();
	    if (br.getParameters() != null) k += br.getParameters();
	    base_map.put(k,br);
	  }
       }
    }

   @Override public void characters(char [] ch,int start,int len) {
      if (cur_text != TextItem.NONE) {
	 if (cur_buffer == null) cur_buffer = new StringBuffer();
	 cur_buffer.append(ch,start,len);
       }
    }

}	// end of inner class BdocLoader



}	// end of class BdocRepository




/* end of BdocRepository.java */
