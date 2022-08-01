/********************************************************************************/
/*										*/
/*		NobaseSymbol.java						*/
/*										*/
/*	Representation of a javascript symbol (identifier			*/
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
import org.eclipse.wst.jsdt.core.dom.*;


class NobaseSymbol implements NobaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		symbol_name;
private NobaseValue	symbol_value;
private int		num_assignment;
private String		bubbles_name;
private NobaseProject	for_project;
private NobaseFile	for_file;
private ASTNode 	def_node;
private NobaseScope	def_scope;	// scope of definition, not where defined
private SymbolType	symbol_type;
private NobaseType	data_type;
					// might be LOCAL scope for example



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

NobaseSymbol(NobaseProject proj,NobaseFile file,ASTNode def,
      String name,boolean exp)
{
   symbol_name = name;
   symbol_value = NobaseValue.createUndefined();
   num_assignment = 0;
   bubbles_name = null;
   for_project = proj;
   for_file = file;
   def_node = def;
   def_scope = null;
   symbol_type = SymbolType.VAR;
   data_type = null;
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getName()			{ return symbol_name; }
NobaseValue getValue()			{ return symbol_value; }
NobaseType getType()
{
   if (symbol_value == null) {
      symbol_value = NobaseValue.createUndefined();
    }
   return symbol_value.getType();
}
String getBubblesName() 		{ return bubbles_name; }
NobaseProject getProject()		{ return for_project; }
NobaseFile getFileData()		{ return for_file; }
ASTNode getDefNode()			{ return def_node; }
boolean isAssigned()			{ return num_assignment > 0; }

NameType getNameType()
{
   String qnm = bubbles_name;
   if (symbol_value != null && symbol_value.isFunction()) {
     return NameType.FUNCTION;
    }
   else if (symbol_value != null && symbol_value.isClass()) { 
      return NameType.CLASS;
    }
   else if (qnm == null) {
      if (!symbol_name.equals("this") && !symbol_name.equals("undefined")) {
	 NobaseMain.logE("Name missing bubbles name: " + symbol_name + " " + def_node + " " + for_file);
       }
      return NameType.VARIABLE;
    }
   else {
      int idx = qnm.indexOf(".");
      if (idx < 0) return NameType.MODULE;
      idx = qnm.indexOf(".",idx+1);
      if (idx >= 0) return NameType.LOCAL;
    }
   return NameType.VARIABLE;
}

String getHandle() {
   String pnm = getProject().getName() + ":";
   if (symbol_value != null && symbol_value.isFunction())
      return pnm + bubbles_name + "()";
   return pnm + bubbles_name;
}

void setValue(NobaseValue typ)		{ symbol_value = typ; }
void addAssignment()			{ ++num_assignment; }
void setBubblesName(String nm)
{
   bubbles_name = nm;
}

NobaseScope getDefScope()		{ return def_scope; }
void setDefScope(NobaseScope scp)	{ def_scope = scp; }

void setSymbolType(SymbolType st)	{ symbol_type = st; }
SymbolType getSymbolType()		{ return symbol_type; }

void setDataType(NobaseType typ)	{ data_type = typ; }
NobaseType getDataType()		{ return data_type; }




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputNameData(NobaseFile rf,IvyXmlWriter xw)
{
   ASTNode an = getDefNode();
   if (an == null) return;
   if (an instanceof VariableDeclarationFragment) {
      ASTNode pan = an.getParent();
      if (pan instanceof VariableDeclarationStatement) {
         VariableDeclarationStatement vds = (VariableDeclarationStatement) pan;
         if (vds.fragments().size() == 1) an = vds;
       }
    }

   xw.begin("ITEM");
   xw.field("PROJECT",for_project.getName());
   if (rf != null) xw.field("PATH",rf.getFile().getPath());
   xw.field("QNAME",getBubblesName());
   if (symbol_value != null && symbol_value.getType() != null) {
      xw.field("JSTYPE",symbol_value.getType().getName());
    }
   xw.field("TYPE",getExternalTypeName());
   int spos = NobaseAst.getExtendedStartPosition(an);
   int epos = NobaseAst.getExtendedEndPosition(an);
   xw.field("STARTOFFSET",spos);
   xw.field("LENGTH",epos-spos);
   xw.field("ENDOFFSET",epos);
   String hdl = getHandle();
   if (hdl != null) xw.field("HANDLE",hdl);
   xw.end("ITEM");
}


void outputFullName(IvyXmlWriter xw)
{
   xw.begin("FULLYQUALIFIEDNAME");
   xw.field("NAME",getBubblesName());
   if (symbol_value != null) xw.field("JSTYPE",symbol_value.getType().getName());
   xw.field("TYPE",getExternalTypeName());
   xw.end("FULLYQUALIFIEDNAME");
}


private String getExternalTypeName()
{
   String rslt = "Variable";
   if (symbol_value != null) {
      String tnm = symbol_value.getType().getName();
      if (tnm.equalsIgnoreCase("function")) rslt = "Function";
    }
   if (rslt.equals("Variable")) {
      if (bubbles_name == null) rslt = "Local";
      else {
	 int idx = bubbles_name.indexOf(".");
	 if (idx >= 0) {
	    idx = bubbles_name.indexOf(".",idx+1);
	    if (idx > 0) rslt = "Local";
	  }
       }
    }
   return rslt;
}



/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   StringBuffer buf = new StringBuffer();
   if (bubbles_name != null) buf.append(bubbles_name);
   else buf.append(symbol_name);
   if (symbol_value != null) {
      buf.append("=");
      if (symbol_value.getType() == null) buf.append("???");
      else buf.append(symbol_value.getType().getName());
      Object o = symbol_value.getKnownValue();
      if (o != null) {
	 buf.append("[");
	 buf.append(o.toString());
	 buf.append("]");
       }
    }
   return buf.toString();
}



}	// end of class NobaseSymbol




/* end of NobaseSymbol.java */
