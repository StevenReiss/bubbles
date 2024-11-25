/********************************************************************************/
/*										*/
/*		NobaseModule.java						*/
/*										*/
/*	Manage node modules							*/
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



package edu.brown.cs.bubbles.nobase;


import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class NobaseModule implements NobaseConstants
{

/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private NobaseProject		    for_project;
private Map<NobaseFile,NobaseValue> export_map;
private Set<NobaseFile> 	    active_files;

private static boolean		    do_debug = true;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseModule(NobaseProject proj)
{
   for_project = proj;
   export_map = new HashMap<>();
   active_files = new HashSet<>();
}



/********************************************************************************/
/*										*/
/*	Interface methods for managing export object				*/
/*										*/
/********************************************************************************/

NobaseValue findRequiresValue(NobaseFile forfile,String nm)
{
   if (do_debug) NobaseMain.logD("REQUIRES: Handle " + nm + " FOR " + forfile);

   NobaseFile result = forfile.getProject().findRequiresFile(forfile,nm);
   if (result == null) {
      String rqnm = "REQUIRE$$$_" + nm;
      NobaseValue nv = forfile.getProject().getGlobalScope().lookupValue(rqnm,false);
      return nv;
    }
   NobaseValue nv = export_map.get(result);
   if (nv == null) {
      for_project.buildIfNeeded(result);
      nv = export_map.get(result);
    }

   if (do_debug) {	
      NobaseMain.logD("REQUIRES: RESULT " + nm + " FOR " + forfile + " = " + nv);
      if (nv != null) {
	 for (Object o : nv.getKnownProperties()) {
	    NobaseMain.logD("\t" + o.toString() + " :: " + nv.getProperty(o,false));
	 }
      }
   }

   return nv;
}



NobaseValue findExportValue(NobaseFile forfile)
{
   active_files.add(forfile);
   NobaseValue nv = export_map.get(forfile);
   if (nv == null) {
      nv = NobaseValue.createObject();
      export_map.put(forfile,nv);
    }
   return nv;
}



void clearExportValue(NobaseFile forfile)
{
    export_map.remove(forfile);
}



NobaseValue finishExportValue(NobaseFile forfile)
{
   active_files.remove(forfile);
   return null;
}




/********************************************************************************/
/*										*/
/*	Requires function implementation					*/
/*										*/
/********************************************************************************/

Evaluator getRequiresEvaluator()
{
   return new Requires();
}



private final class Requires implements Evaluator {

   @Override public NobaseValue evaluate(NobaseFile forfile,List<NobaseValue> args,
         NobaseValue thisval) {
      if (args.size() == 0) return null;
      if (args.get(0) == null) return null;
      Object o = args.get(0).getKnownValue();
      if (o == null) return null;
      if (!(o instanceof String)) return null;
      String nm = o.toString();
      NobaseMain.logD("HANDLE require: " + nm);
      NobaseValue nv = findRequiresValue(forfile,nm);
      if (nv != null) return nv;
      return NobaseValue.createUnknownValue();
    }

}	// end of inner class Requires




}	// end of class NobaseModule




/* end of NobaseModule.java */

