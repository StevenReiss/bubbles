/********************************************************************************/
/*										*/
/*		PybaseDebugBreakpoint.java					*/
/*										*/
/*	Handle debugging breakpoint for Bubbles from Python			*/
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
/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.pybase.debug;

import edu.brown.cs.bubbles.pybase.PybaseConstants;
import edu.brown.cs.bubbles.pybase.PybaseException;
import edu.brown.cs.bubbles.pybase.PybaseMain;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.w3c.dom.Element;

import java.io.File;


abstract class PybaseDebugBreakpoint implements PybaseDebugConstants, PybaseConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private boolean 	is_enabled;
private String		break_id;
private int		break_number;
private boolean 	is_tracepoint;
private String		debug_condition;
private boolean 	condition_enabled;

private static IdCounter break_counter = new IdCounter();




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected PybaseDebugBreakpoint()
{
   break_number = break_counter.nextValue();
   break_id = "BREAK_" + Integer.toString(break_number);
   is_enabled = true;
   is_tracepoint = false;
   debug_condition = null;
   condition_enabled = false;
}


protected PybaseDebugBreakpoint(Element xml)
{
   break_number = IvyXml.getAttrInt(xml,"ID");
   break_counter.noteValue(break_number);
   break_id = "BREAK_" + Integer.toString(break_number);
   break_id = IvyXml.getAttrString(xml,"ID");

   is_enabled = IvyXml.getAttrBool(xml,"ENABLED");
   is_tracepoint = IvyXml.getAttrBool(xml,"TRACEPOINT");
   debug_condition = IvyXml.getTextElement(xml,"CONDITION");
   condition_enabled = IvyXml.getAttrBool(xml,"CONDENABLED");
}



/********************************************************************************/
/*										*/
/*	Creation methods							*/
/*										*/
/********************************************************************************/

static PybaseDebugBreakpoint createLineBreakpoint(IFileData fd,int ln) throws PybaseException
{
   return new LineBreakpoint(fd,ln);
}


static PybaseDebugBreakpoint createExceptionBreakpoint(String e,boolean caught,boolean uncaught)
{
   return new ExceptionBreakpoint(e,caught,uncaught);
}


static PybaseDebugBreakpoint createBreakpoint(Element e) throws PybaseException
{
   BreakType bt = IvyXml.getAttrEnum(e,"TYPE",BreakType.NONE);
   switch (bt) {
      case NONE :
	 break;
      case LINE :
	 return new LineBreakpoint(e);
      case EXCEPTION :
	 return new ExceptionBreakpoint(e);
   }

   return null;
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getId()					{ return break_id; }

public boolean isEnabled()			{ return is_enabled; }
public String getCondition()			{ return debug_condition; }
public boolean isConditionEnabled()		{ return condition_enabled; }

public void setConditionEnabled(boolean e)	{ condition_enabled = e; }
public void setCondition(String c)
{
   if (c != null && c.trim().length() == 0) c = null;
   debug_condition = c;
}

void setProperty(String p,String v)
{
   if (p == null) return;
   if (p.equals("ENABLE") || p.equals("ENABLED")) {
      if (v == null) is_enabled = true;
      else is_enabled = Boolean.parseBoolean(v);
    }
   else if (p.equals("DISABLE") || p.equals("DISABLED")) {
      is_enabled = false;
    }
}

abstract BreakType getType();

public File getFile()				{ return null; }
public int getLine()				{ return -1; }
public String getException()			{ return null; }
public boolean isCaught()			{ return false; }
public boolean isUncaught()			{ return false; }



/********************************************************************************/
/*										*/
/*	Output Methods								*/
/*										*/
/********************************************************************************/

void outputXml(IvyXmlWriter xw)
{
   xw.begin("BREAKPOINT");
   xw.field("TYPE",getType());
   xw.field("ID",break_number);
   xw.field("ENABLED",is_enabled);
   xw.field("CONDENABLED",condition_enabled);
   xw.field("TRACEPOINT",is_tracepoint);

   outputLocalXml(xw);

   if (debug_condition != null) xw.cdataElement("CONDITION",debug_condition);
   xw.end("BREAKPOINT");
}

protected abstract void outputLocalXml(IvyXmlWriter xw);



void outputBubbles(IvyXmlWriter xw)
{
   xw.begin("BREAKPOINT");
   xw.field("TYPE",getType());
   xw.field("ENABLED",is_enabled);
   xw.field("ID",break_id);
   xw.field("SUSPEND", "VM");
   xw.field("HITCOUNT",0);
   xw.field("TRACEPOINT",is_tracepoint);

   outputLocalBubbles(xw);

   if (debug_condition != null) {
      xw.begin("CONDITION");
      xw.field("ENABLED",condition_enabled);
      xw.text(debug_condition);
      xw.end("CONDITION");
    }

   xw.end("BREAKPOINT");
}

protected abstract void outputLocalBubbles(IvyXmlWriter xw);




/********************************************************************************/
/*										*/
/*	Line breakpoint specifics						*/
/*										*/
/********************************************************************************/

private static class LineBreakpoint extends PybaseDebugBreakpoint
{
   private IFileData	   file_data;
   private Position	   file_position;

   LineBreakpoint(IFileData fd,int line) throws PybaseException {
      file_data = fd;
      IDocument d = fd.getDocument();
      try {
	 int off = d.getLineOffset(line);
	 file_position = new Position(off);
	 d.addPosition(file_position);
       }
      catch (BadLocationException ex) {
	 throw new PybaseException("Bad breakpoint location",ex);
       }
    }

   LineBreakpoint(Element xml) throws PybaseException {
      super(xml);
      PybaseMain pm = PybaseMain.getPybaseMain();
      String fnm = IvyXml.getTextElement(xml,"FILE");
      file_data = pm.getFileData(fnm);
      if (file_data == null) throw new PybaseException("File " + fnm + " not found");
      int line = IvyXml.getAttrInt(xml,"LINE");
      IDocument d = file_data.getDocument();
      try {
	 int off = d.getLineOffset(line);
	 file_position = new Position(off);
	 d.addPosition(file_position);
       }
      catch (BadLocationException ex) {
	 throw new PybaseException("Bad breakpoint location",ex);
       }
    }

   @Override BreakType getType()			{ return BreakType.LINE; }

   @Override public File getFile()		{ return file_data.getFile(); }

   @Override public int getLine() {
      int off = file_position.getOffset();
      try {
	 return file_data.getDocument().getLineOfOffset(off);
       }
      catch (BadLocationException ex) {
	 return -1;
       }
    }

   @Override protected void outputLocalXml(IvyXmlWriter xw) {
      xw.field("FILE",file_data.getFile());
      xw.field("LINE",getLine());
      xw.field("OFFSET",file_position.getOffset());
    }

   @Override protected void outputLocalBubbles(IvyXmlWriter xw) {
      xw.field("LINE",getLine());
      xw.field("FILE",file_data.getFile());
      xw.field("STARTPOS",file_position.getOffset());
      xw.field("ENDPOS",file_position.getOffset() + file_position.getLength());
    }

}	// end of inner class LineBreakpoint



/********************************************************************************/
/*										*/
/*	Exception breakpoint specifics						*/
/*										*/
/********************************************************************************/

private static class ExceptionBreakpoint extends PybaseDebugBreakpoint
{
   private String	exception_name;
   private boolean	is_caught;
   private boolean	is_uncaught;

   ExceptionBreakpoint(String e,boolean c,boolean u) {
      exception_name = e;
      is_caught = c;
      is_uncaught = u;
    }

   ExceptionBreakpoint(Element xml) {
      super(xml);
      exception_name = IvyXml.getAttrString(xml,"EXCEPTION");
      is_caught = IvyXml.getAttrBool(xml,"ISCAUGHT");
      is_uncaught = IvyXml.getAttrBool(xml,"ISUNCAUGHT");
    }

   @Override BreakType getType()			{ return BreakType.EXCEPTION; }
   @Override public String getException() 	{ return exception_name; }
   @Override public boolean isCaught()		{ return is_caught; }
   @Override public boolean isUncaught()		{ return is_uncaught; }

   @Override protected void outputLocalXml(IvyXmlWriter xw) {
      xw.field("EXCEPTION",exception_name);
      xw.field("ISCAUGHT",is_caught);
      xw.field("ISUNCAUGHT",is_uncaught);
    }

   @Override protected void outputLocalBubbles(IvyXmlWriter xw) {
      xw.field("EXCEPTION",exception_name);
      xw.field("ISCAUGHT",is_caught);
      xw.field("ISUNCAUGHT",is_uncaught);
    }

}	// end of inner class LineBreakpoint



}	// end of class PybaseDebugBreakpoint




/* end of PybaseDebugBreakpoint.java */
