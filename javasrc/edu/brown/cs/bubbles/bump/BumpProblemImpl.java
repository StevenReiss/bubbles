/********************************************************************************/
/*										*/
/*		BumpProblemImpl.java						*/
/*										*/
/*	BUblles Mint Partnership problem description holder			*/
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


package edu.brown.cs.bubbles.bump;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



class BumpProblemImpl implements BumpConstants.BumpProblem, BumpConstants {




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String	problem_id;
private int	problem_msgid;
private String	problem_message;
private String	for_project;
private File	file_name;
private int	line_number;
private int	start_position;
private int	end_position;
private BumpErrorType error_type;
private int	edit_id;
private List<BumpFix> problem_fixes;
private boolean computed_fixes;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BumpProblemImpl(Element d,String id,int eid,String proj)
{
   problem_id = id;
   problem_msgid = IvyXml.getAttrInt(d,"MSGID");
   problem_message = IvyXml.getTextElement(d,"MESSAGE");
   problem_message = IvyXml.decodeXmlString(problem_message);
   for_project = proj;
   String fnm = IvyXml.getTextElement(d,"FILE");
   if (fnm == null) file_name = null;
   else file_name = new File(fnm);
   line_number = IvyXml.getAttrInt(d,"LINE",0);
   start_position = IvyXml.getAttrInt(d,"START");
   end_position = IvyXml.getAttrInt(d,"END");
   error_type = BumpErrorType.NOTICE;
   if (IvyXml.getAttrBool(d,"ERROR")) error_type = BumpErrorType.ERROR;
   else if (IvyXml.getAttrBool(d,"WARNING")) error_type = BumpErrorType.WARNING;
   edit_id = eid;

   setupFixes(d);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getProblemId()				{ return problem_id; }
@Override public String getMessage()				{ return problem_message; }
@Override public File getFile() 				{ return file_name; }
@Override public int getLine()					{ return line_number; }
@Override public int getStart() 				{ return start_position; }
@Override public int getEnd()					{ return end_position; }
@Override public BumpErrorType getErrorType()			{ return error_type; }
@Override public int getEditId()				{ return edit_id; }
@Override public String getProject()				{ return for_project; }

@Override synchronized public List<BumpFix> getFixes()
{
   if (!computed_fixes) {
      Element r = BumpClient.getBump().computeQuickFix(this,start_position,end_position-start_position,true);
      BoardLog.logD("BUMP","FOUND FIXES: " + IvyXml.convertXmlToString(r));
      for (Element f : IvyXml.children(r,"FIX")) {
	 EditFix ef = new EditFix(f);
	 if (problem_fixes == null) problem_fixes = new ArrayList<>();
	 problem_fixes.add(ef);
       }
      computed_fixes = true;
    }
   return problem_fixes;
}

int getMessageId()						{ return problem_msgid; }



/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

void setEditId(int eid) 				{ edit_id = eid; }

void update(Element e)
{
   problem_message = IvyXml.getTextElement(e,"MESSAGE");
   problem_message = IvyXml.decodeXmlString(problem_message);
   end_position = IvyXml.getAttrInt(e,"END");
   setupFixes(e);
}




private void setupFixes(Element d)
{
   problem_fixes = null;
   computed_fixes = false;

   for (Element e : IvyXml.children(d,"FIX")) {
      FixImpl fi = new FixImpl(e);
      if (fi.getType() != BumpFixType.NONE) {
	 if (problem_fixes == null) problem_fixes = new ArrayList<>();
	 problem_fixes.add(fi);
       }
    }
}




/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   return "PROBLEM:" + getProblemId() + ":" + getMessage() + "@" + getFile() + ":" + getLine();
}



/********************************************************************************/
/*										*/
/*	Problem Fix reprsentation						*/
/*										*/
/********************************************************************************/

private class FixImpl implements BumpConstants.BumpFix {

   private BumpFixType fix_type;
   private Map<String,String> fix_attrs;

   FixImpl(Element e) {
      fix_type = IvyXml.getAttrEnum(e,"TYPE",BumpFixType.NONE);
      fix_attrs = new HashMap<String,String>(4);
      if (for_project != null) fix_attrs.put("PROJECT",for_project);
      for (String s : FIX_PARAMETERS) {
	 String v = IvyXml.getTextElement(e,s);
	 if (v != null) fix_attrs.put(s,v);
       }
    }

   @Override public BumpFixType getType()		{ return fix_type; }
   @Override public String getParameter(String id)	{ return fix_attrs.get(id); }
   @Override public Element getEdits()			{ return null; }
   @Override public int getRelevance()			{ return 50; }

}	// end of inner class FixImpl


private class EditFix implements BumpConstants.BumpFix {

   private Map<String,String> fix_attrs;
   private Element fix_edits;
   private int fix_relevance;

   EditFix(Element e) {
      fix_attrs = new HashMap<String,String>();
      if (for_project != null) fix_attrs.put("PROJECT",for_project);
      fix_attrs.put("DISPLAY",IvyXml.getAttrString(e,"DISPLAY"));
      fix_edits = IvyXml.getChild(e,"EDIT");
      fix_relevance = IvyXml.getAttrInt(e,"RELEVANCE");
    }

   @Override public BumpFixType getType()		{ return BumpFixType.EDIT_FIX; }
   @Override public String getParameter(String id)	{ return fix_attrs.get(id); }
   @Override public Element getEdits()			{ return fix_edits; }
   @Override public int getRelevance()			{ return fix_relevance; }

}	// edn of inner class EditFix



}	// end of class BumpProblemImpl




/* end of BumpProblemImpl.java */



