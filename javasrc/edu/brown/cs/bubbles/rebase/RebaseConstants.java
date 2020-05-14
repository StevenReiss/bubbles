/********************************************************************************/
/*										*/
/*		RebaseConstants.java						*/
/*										*/
/*	Constants for REpository BUbbles System base implementation		*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
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

/* SVN: $Id$ */



package edu.brown.cs.bubbles.rebase;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.util.Collection;
import java.util.List;
import java.util.Set;



public interface RebaseConstants
{


/********************************************************************************/
/*										*/
/*	Messaging definitions							*/
/*										*/
/********************************************************************************/

// Must Match BumpConstants.BUMP_MINT_NAME
String	REBASE_MINT_NAME = "BUBBLES_" + System.getProperty("user.name").replace(" ","_");



/********************************************************************************/
/*										*/
/*	Logging constants							*/
/*										*/
/********************************************************************************/

enum RebaseLogLevel {
   NONE,
   ERROR,
   WARNING,
   INFO,
   DEBUG
}


/********************************************************************************/
/*										*/
/*	Thread pool information 						*/
/*										*/
/********************************************************************************/

int REBASE_CORE_POOL_SIZE = 2;
int REBASE_MAX_POOL_SIZE = 8;
long REBASE_POOL_KEEP_ALIVE_TIME = 2*60*1000;



/********************************************************************************/
/*										*/
/*	Edit information							*/
/*										*/
/********************************************************************************/

interface IEditData {

   public int getOffset();
   public int getLength();
   public String getText();

}	// end of subinterface EditData


/********************************************************************************/
/*										*/
/*	Source Information							*/
/*										*/
/********************************************************************************/

enum SourceType {
   FILE, PACKAGE, SYSTEM
}

enum SourceLanguage {
   JAVA
}

interface RebaseSource {

   SourceType getSourceType();
   SourceLanguage getLanguage();
   RebaseRepo getRepository();
   RebaseRequest getRequest();
   String getText();
   String getPath();
   String getProjectId();
   String getProjectName();
   RebaseSource getBaseSource();
   String getS6Source();
 
   void setSourceType(SourceType st);

}	// end of inner interface RebaseSource



/********************************************************************************/
/*										*/
/*	Caching data								*/
/*										*/
/********************************************************************************/

String CACHE_DIRECTORY = "/ws/volfred/s6/cache";
String CACHE_URL_FILE = "URL";
String CACHE_DATA_FILE = "DATA";



/********************************************************************************/
/*										*/
/*	Classes to be implemented						*/
/*										*/
/********************************************************************************/

interface RebaseElider {

   void clearElideData();
   void addElidePriority(int soff,int eoff,double p);
   void addElideRegion(int soff,int eoff);
   void noteEdit(int soff,int len,int rlen);
   boolean computeElision(RebaseSemanticData sd,IvyXmlWriter xw);

}	// end of inner interface RebaseElider


interface RebaseLanguage {

   RebaseSemanticData getSemanticData(RebaseFile rf);
   RebaseProjectSemantics getSemanticData(Collection<RebaseFile> rfs);

}	// end of inner interface RebaseLanguage


interface RebaseSemanticData {

   RebaseElider createElider();
   List<RebaseMessage> getMessages();
   RebaseFile getFile();
   void reparse();
   
   void getTextRegions(String txt,String cls,boolean pfx,boolean statics,
         boolean compunit,boolean imports,boolean pkgfg,boolean topdecls,
         boolean fields,boolean all,IvyXmlWriter xw) throws RebaseException;
   
   void formatCode(String txt,int spos,int epos,IvyXmlWriter xw);
   
   boolean definesClass(String cls);
   
   Set<String> getRelatedPackages();

}	// end of inner interface RebaseSemanticData


interface RebaseProjectSemantics {
   
  void resolve(); 
  void outputAllNames(Set<String> files,IvyXmlWriter xw);
  
  RebaseSearcher findSymbols(String pattern,String kind);
  RebaseSearcher findSymbolAt(String file,int soff,int eoff);
  RebaseSearcher findSymbolByKey(String proj,String file,String key);
  RebaseSearcher findTypes(RebaseSearcher rs);
  void outputLocations(RebaseSearcher rs,boolean def,boolean ref,
        boolean impl,boolean ronly,boolean wonly,IvyXmlWriter xw);
  void outputFullName(RebaseSearcher rs,IvyXmlWriter xw);
  void outputContainer(RebaseFile file,int soff,int eoff,IvyXmlWriter xw);
  
  List<RebaseMessage> getMessages();
  
}


interface RebaseSearcher { 
   void outputSearchFor(IvyXmlWriter xw);
}


enum RebaseSymbolKind {
   NONE,
   CLASS,
   INTERFACE,
   ENUM,
   METHOD,
   CONSTRUCTOR,
   FIELD,
   PACKAGE,
   ANNOTATION,
   LOCAL,
}




interface RebaseSymbol {
   
}


enum MessageSeverity {
   FATAL,
   ERROR,
   WARNING,
   NOTICE
}

}	// end of interface RebaseConstants




/* end of RebaseConstants.java */





















