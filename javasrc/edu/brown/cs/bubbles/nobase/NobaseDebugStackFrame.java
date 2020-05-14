/********************************************************************************/
/*										*/
/*		NobaseDebugStackFrame.java					*/
/*										*/
/*	Representation of a javascript stack frame				*/
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


import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class NobaseDebugStackFrame implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String	frame_id;
private int     frame_index;
private String	frame_function;
private String	frame_file;
private int	source_line;
private int	source_column;
private List<ScopeData> frame_scopes;
private NobaseDebugRefMap ref_map;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseDebugStackFrame(JSONObject json,NobaseDebugRefMap refmap,int idx)
{
   ref_map = refmap;
   frame_id = json.getString("callFrameId");
   frame_index = idx;
   JSONObject loc = json.getJSONObject("location");
   int fid = loc.getInt("scriptId");
   frame_file = refmap.getTarget().getFileForScriptId(fid);
   source_line = loc.getInt("lineNumber");
   source_column = loc.optInt("columnNumber");
   frame_scopes = new ArrayList<>();
   
   JSONArray arr = json.getJSONArray("scopeChain");
   for (int i = 0; i < arr.length(); ++i) {
      JSONObject scp = arr.getJSONObject(i);
      ScopeData sd = new ScopeData(refmap,scp);
      frame_scopes.add(sd);
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

int getIndex()				{ return frame_index; }



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(IvyXmlWriter xw,int ctr,int lvl)
{
   for (ScopeData sd : frame_scopes) {
      sd.loadData(ref_map,lvl);
    }
   
   xw.begin("STACKFRAME");
   xw.field("METHOD",frame_function);
   xw.field("SIGNATURE","()");
   xw.field("ID",frame_index);
   xw.field("FRAMEID",frame_id);
   xw.field("LINENO",source_line);
   xw.field("COLUMN",source_column);
   xw.field("LEVEL",frame_index);
   xw.field("FILE",frame_file);
   xw.field("FILETYPE","JS");
   for (ScopeData sd : frame_scopes) {
      sd.outputXml(lvl,xw);
    }
   xw.end("STACKFRAME");
}



/********************************************************************************/
/*                                                                              */
/*      Scope data                                                              */
/*                                                                              */
/********************************************************************************/

private static class ScopeData {
   
   private String scope_type;
   private NobaseDebugValue scope_object;
   private String scope_name;
   private String scope_file;
   private int start_line;
   private int end_line;
   
   ScopeData(NobaseDebugRefMap refmap,JSONObject sd) {
      scope_type = sd.optString("type",null);
      scope_name = sd.optString("name",null);
      JSONObject loc = sd.optJSONObject("location");
      if (loc != null) {
         int sid = loc.getInt("scirptId");
         scope_file = refmap.getTarget().getFileForScriptId(sid);
         start_line = loc.getInt("lineNumber");
         loc = sd.getJSONObject("endLocation");
         end_line = loc.getInt("lineNumber");
       }
      else {
         scope_file = null;
         start_line = 0;
         end_line = 0;
       }
      JSONObject obj = sd.getJSONObject("object");
      scope_object = NobaseDebugValue.getValue(obj,refmap,null); 
    }
   
   void loadData(NobaseDebugRefMap refmap,int lvl) {
      scope_object.complete(refmap,lvl+1);
    }
   
   void outputXml(int lvl,IvyXmlWriter xw) {
      xw.begin("SCOPE");
      xw.field("TYPE",scope_type);
      if (scope_name != null) xw.field("NAME",scope_name);
      if (scope_file != null) {
         xw.field("FILE",scope_file);
         xw.field("STARTLINE",start_line);
         xw.field("ENDLINE",end_line);
       }
      scope_object.outputXml(scope_name,lvl,xw);
      xw.end("SCOPE");
    }

}       // end of inner class ScopeData



   
}	// end of class NobaseDebugStackFrame




/* end of NobaseDebugStackFrame.java */

