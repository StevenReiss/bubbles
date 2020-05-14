/********************************************************************************/
/*										*/
/*		BedrockProblem.java						*/
/*										*/
/*	Problem management methods for Eclipse :: quick fix suggestions 	*/
/*										*/
/********************************************************************************/
/*	Copyright 2006 Brown University -- Steven P. Reiss		      */
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



package edu.brown.cs.bubbles.bedrock;


import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.compiler.IProblem;



class BedrockProblem implements BedrockConstants {





/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BedrockProblem(BedrockPlugin bp)
{ }



/********************************************************************************/
/*										*/
/*	Methods to add fixes for a problem					*/
/*										*/
/********************************************************************************/

void addFixes(IProblem ip,IvyXmlWriter xw)
{
   if (ip == null) return;

   addFixes(getId(ip),getArguments(ip),xw);
}




void addFixes(IMarkerDelta imd,IvyXmlWriter xw)
{
   if (imd == null) return;
   if (!imd.getType().equals("org.eclipse.jdt.core.problem")) return;

   addFixes(getId(imd),getArguments(imd),xw);
}




void addFixes(IMarker im,IvyXmlWriter xw)
{
   if (im == null) return;
   try {
      if (!im.getType().equals("org.eclipse.jdt.core.problem")) return;
    }
   catch (CoreException e) {
      BedrockPlugin.logE("Problem getting marker type: " + e);
      return;
    }

   addFixes(getId(im),getArguments(im),xw);
}




/********************************************************************************/
/*										*/
/*	Actual fixup generation methods 					*/
/*										*/
/********************************************************************************/

private void addFixes(int id,String [] args,IvyXmlWriter xw)
{
   switch (id) {
      case IProblem.UndefinedMethod :
	 if (args == null) return;
	 xw.begin("FIX");
	 xw.field("TYPE","NEW_METHOD");
	 xw.field("CLASS",args[0]);
	 xw.field("NAME",args[1]);
	 if (args.length > 2) xw.field("PARAMS",args[2]);
	 // TODO: determine return type
	 xw.end("FIX");
	 break;
      default :
	 break;
    }
}





/********************************************************************************/
/*										*/
/*	Generic access methods							*/
/*										*/
/********************************************************************************/

private int getId(IProblem ip)			{ return ip.getID(); }

private int getId(IMarkerDelta imd)		{ return imd.getAttribute("id",0); }

private int getId(IMarker imd)			{ return imd.getAttribute("id",0); }


private String [] getArguments(IProblem ip)	{ return ip.getArguments(); }

private String [] getArguments(IMarkerDelta imd)
{
   String v = imd.getAttribute("arguments","");
   int idx = v.indexOf(":");
   if (idx < 0) return null;
   int narg = 0;
   try { narg = Integer.parseInt(v.substring(0,idx)); }
   catch (NumberFormatException e) { return null; }
   String [] rslt;
   if (narg == 0) rslt = new String [0];
   else rslt = v.substring(idx+1).split("#");
   if (rslt.length != narg) {
      BedrockPlugin.logE("Problem parsing arguments " + narg + " " + rslt.length + " " + v);
   }
   return rslt;
}


private String [] getArguments(IMarker im)
{
   String v = im.getAttribute("arguments","");
   int idx = v.indexOf(":");
   if (idx < 0) return null;
   int narg = 0;
   try { narg = Integer.parseInt(v.substring(0,idx)); }
   catch (NumberFormatException e) { return null; }
   String [] rslt;
   if (narg == 0) rslt = new String [0];
   else rslt = v.substring(idx+1).split("#");
   if (rslt.length != narg) {
      BedrockPlugin.logE("Problem parsing arguments " + narg + " " + rslt.length + " " + v);
   }
   return rslt;
}




}	// end of class BedrockProblem




/* end of BedrockProblem.java */
