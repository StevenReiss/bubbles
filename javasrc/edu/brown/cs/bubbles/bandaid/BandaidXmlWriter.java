/********************************************************************************/
/*										*/
/*		BandaidXmlWriter.java						*/
/*										*/
/*	Bubbles ANalsysis DynAmic Information Data xml output class		*/
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


/* SVN: $Id$ */



package edu.brown.cs.bubbles.bandaid;


import java.io.*;
import java.util.Vector;



class BandaidXmlWriter extends PrintWriter
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private Vector<String>	element_stack;
private OpenState	open_state;
private String		indent_string;
private Writer		base_writer;


enum OpenState {
   DONE,
   OPEN,
   CLOSED,
   TEXT
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BandaidXmlWriter(Writer w)
{
   super(w);
   base_writer = w;
   element_stack = new Vector<String>();
   // indent_string = "  ";
   indent_string = null;	     // save space
   open_state = OpenState.DONE;
}




BandaidXmlWriter()
{
   this(new StringWriter());
}




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void begin(String elt)
{
   switch (open_state) {
      case DONE :
	 break;
      case OPEN :
	 println(">");
	 break;
      case CLOSED :
	 break;
      default:
	 break;
    }

   indent();
   print("<");
   print(elt);
   open_state = OpenState.OPEN;
   element_stack.addElement(elt);
}




void end()
{
   int ln = element_stack.size();
   if (ln == 0) throw new Error("End with no corresponding begin");
   String elt = element_stack.lastElement();
   element_stack.setSize(ln-1);

   if (open_state == OpenState.DONE) return;
   else if (open_state == OpenState.OPEN) {
      println(" />");
    }
   else if (elt != null) {
      if (open_state != OpenState.TEXT) indent();
      print("</");
      print(elt);
      println(">");
    }
   if (ln == 1) open_state = OpenState.DONE;
   else open_state = OpenState.CLOSED;
}



void field(String elt,String val)
{
   if (open_state != OpenState.OPEN) throw new Error("Field must be specified inside an element");

   print(" ");
   print(elt);
   print("='");
   outputXmlString(val);
   print("'");
}



void field(String elt,boolean fg)		     { field(elt,String.valueOf(fg)); }
void field(String elt,int v)			     { field(elt,String.valueOf(v)); }
void field(String elt,long v)			     { field(elt,Long.toString(v)); }
void field(String elt,double v) 		     { field(elt,Double.toString(v)); }



void xmlText(String t)
{
   if (t == null) return;

   switch (open_state) {
      case DONE :
	 break;
      case OPEN :
	 print(">");
	 break;
      case CLOSED :
	 break;
      default:
	 break;
    }
   open_state = OpenState.TEXT;

   print(t);
}





/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public CharSequence getContents()
{
   if (base_writer instanceof StringWriter) {
      StringWriter sw = (StringWriter) base_writer;
      return sw.getBuffer();
    }
   return base_writer.toString();
}

@Override public String toString()
{
   return base_writer.toString();
}




/********************************************************************************/
/*										*/
/*	Helper methods								*/
/*										*/
/********************************************************************************/

private void indent()
{
   if (indent_string != null) {
      int ln = element_stack.size();
      for (int i = 0; i < ln; ++i) print(indent_string);
    }
}




private void outputXmlString(String s)
{
   if (s == null) return;
   for (int i = 0; i < s.length(); ++i) {
      char c = s.charAt(i);
      switch (c) {
	 case '&' :
	    print("&amp;");
	    break;
	 case '<' :
	    print("&lt;");
	    break;
	 case '>' :
	    print("&gt;");
	    break;
	 case '"' :
	    print("&quot;");
	    break;
	 case '\'' :
	    print("&apos;");
	    break;
	 default :
	    print(c);
	    break;
       }
    }
}




}	// end of class BandaidXmlWriter





