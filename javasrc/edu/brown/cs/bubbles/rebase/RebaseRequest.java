/********************************************************************************/
/*										*/
/*		RebaseRequest.java						*/
/*										*/
/*	Hold information about a user request and its current status		*/
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

import edu.brown.cs.bubbles.rebase.word.RebaseWordFactory;

import edu.brown.cs.ivy.exec.IvyExec;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RebaseRequest implements RebaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private RebaseMain	rebase_main;
private List<String>	search_string;
private SourceType	search_type;
private String		search_task;
private RebaseProject	for_project;
private Map<RebaseRepo,List<URI>>  repo_uris;
private int		current_index;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

RebaseRequest(RebaseMain rm,String rqst,String repos,String task,String proj,SourceType typ)
{
   rebase_main = rm;
   search_string = null;
   search_type = (typ == null ? SourceType.FILE : typ);
   search_task = task;
   current_index = 0;
   for_project = rebase_main.getProject(proj);
   
   if (rqst != null && !rqst.equals("*")) {
      search_string = IvyExec.tokenize(rqst);
    }
   else {
      search_string = RebaseWordFactory.getFactory().getQuery();
    }

   List<RebaseRepo> repol = new ArrayList<RebaseRepo>();
   if (repos != null) repos = repos.toUpperCase();
   if (repos == null || repos.contains("OHLOH")) {
      repol.add(new RebaseRepoOhloh());
    }
   if (repos == null || repos.contains("GITHUB")) {
      repol.add(new RebaseRepoGithub());
    }
   if (repos == null || repos.contains("GREPCODE")) {
      repol.add(new RebaseRepoGrepCode());
    }
   repo_uris = new HashMap<RebaseRepo,List<URI>>();
   for (RebaseRepo rr : repol) {
      List<URI> uris = rr.generateSearchURIs(search_string,for_project);
      if (uris != null) repo_uris.put(rr,uris);
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

List<String> getSearchTokens()		{ return search_string; }
SourceType getSearchType()		{ return search_type; }
String getSearchTask()			{ return search_task; }



/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

void startNextSearch()
{
   if (search_string == null) return;
   
   NextSearch ns = new NextSearch();
   rebase_main.startTask(ns);
}



void doNextSearch()
{
   List<RebaseSource> rslt = new ArrayList<RebaseSource>();

   for (Map.Entry<RebaseRepo,List<URI>> ent : repo_uris.entrySet()) {
      RebaseRepo rr = ent.getKey();
      List<URI> uris = ent.getValue();
      if (uris.size() <= current_index) continue;
      URI uri = uris.get(current_index);
      try {
	 URL url = uri.toURL();
	 String text = RebaseRepo.loadURL(url,true);
	 if (text == null) throw new RebaseException("Nothing loaded");
	 Element doc = Jsoup.parse(text,url.toString());
	 if (!rr.addSources(url,doc,this,null,null,rslt)) {
	    while (uris.size() > current_index+1) {
	       uris.remove(current_index+1);
	     }
	  }
       }
      catch (IOException e) {
	 RebaseMain.logE("Problem converting url " + uri + ": " + e,e);
       }
      catch (RebaseException e) {
	 RebaseMain.logI("Problem loading url " + uri + ": " + e);
       }
    }


   for (RebaseSource src : rslt) {
      String pid = src.getProjectId();
      String pnm = src.getProjectName();
      RebaseProjectManager pm = rebase_main.getProjectManager();
      RebaseProject proj = pm.findNewProject(pid,pnm,src.getRepository());
      RebaseFile rf = new RebaseFile(src);
      if (proj.addFile(rf)) {
	 proj.notePending();
	 // these probably should be done in background
	 switch (search_type) {
	    case FILE :
	       break;
	    case PACKAGE :
	       proj.addPackageFiles(rf);
	       break;
	    case SYSTEM :
	       proj.addSystemFiles(rf);
	       break;
	  }
	 proj.donePending();
       }
    }

   ++current_index;
}


private class NextSearch implements Runnable {

   @Override public void run() {
      doNextSearch();
    }

}	// end of inner class NextSearch




}	// end of class RebaseRequest




/* end of RebaseRequest.java */

