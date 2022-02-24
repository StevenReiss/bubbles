/********************************************************************************/
/*										*/
/*		BumpLocation.java						*/
/*										*/
/*	BUblles Mint Partnership location holder				*/
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

import edu.brown.cs.bubbles.board.BoardFileSystemView;
import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import javax.swing.filechooser.FileSystemView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 *	Representation of a location returned from Eclipse.  A location is used to
 *	indicate the result of a search (in which case it returns both the symbol
 *	that the search was for and the method/etc. containing it).  It is also used
 *	whenever a request calls for a source pointer or region.
 **/

public class BumpLocation implements BumpConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		project_name;
private File		file_location;
private int		file_offset;
private int		file_length;
private BumpSymbolType	symbol_type;
private String		symbol_name;
private int		symbol_offset;
private int		symbol_length;
private String		symbol_key;
private String		symbol_handle;
private int		symbol_flags;
private String		symbol_project;
private BumpSymbolType	source_type;
private String		symbol_return;
private String          symbol_parameters;
private String		s6_source;

private static FileSystemView file_system = null;

private static Map<String,BumpSymbolType> symbol_map;

static {
   symbol_map = new HashMap<String,BumpSymbolType>();
   symbol_map.put("Class",BumpSymbolType.CLASS);
   symbol_map.put("Throwable",BumpSymbolType.THROWABLE);
   symbol_map.put("Interface",BumpSymbolType.INTERFACE);
   symbol_map.put("Enum",BumpSymbolType.ENUM);
   symbol_map.put("Field",BumpSymbolType.FIELD);
   symbol_map.put("EnumConstant",BumpSymbolType.ENUM_CONSTANT);
   symbol_map.put("Function",BumpSymbolType.FUNCTION);
   symbol_map.put("Constructor",BumpSymbolType.CONSTRUCTOR);
   symbol_map.put("StaticInitializer",BumpSymbolType.STATIC_INITIALIZER);
   symbol_map.put("Exception",BumpSymbolType.CLASS);
   symbol_map.put("PythonMain",BumpSymbolType.MAIN_PROGRAM);
   symbol_map.put("Package",BumpSymbolType.PACKAGE);
   symbol_map.put("Project",BumpSymbolType.PROJECT);
   symbol_map.put("Local",BumpSymbolType.LOCAL);
   symbol_map.put("Variable",BumpSymbolType.GLOBAL);
   symbol_map.put("Module",BumpSymbolType.MODULE);
   symbol_map.put("Import",BumpSymbolType.IMPORT);
   symbol_map.put("Export",BumpSymbolType.EXPORT);
   symbol_map.put("JSCode",BumpSymbolType.PROGRAM);
}




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public static BumpLocation getLocationFromXml(Element xml)
{
   if (xml == null) return null;

   String fnm = IvyXml.getTextElement(xml,"FILE");
   int offset = IvyXml.getAttrInt(xml,"OFFSET");
   int length = IvyXml.getAttrInt(xml,"LENGTH");
   if (length < 0) {
      int eff = IvyXml.getAttrInt(xml,"ENDOFFSET");
      if (eff > 0) length = eff - offset;
    }
   Element mi = IvyXml.getChild(xml,"ITEM");
   String pnm = IvyXml.getAttrString(xml,"PROJECT");
   if (pnm == null) pnm = IvyXml.getAttrString(mi,"PROJECT");
   String srctyp = IvyXml.getAttrString(xml,"TYPE");

   BumpLocation bl = new BumpLocation(pnm,fnm,offset,length,srctyp,mi);

   return bl;
}



BumpLocation(String proj,String file,int off,int len,Element itm)
{
   this(proj,file,off,len,null,itm);
}



BumpLocation(String proj,String file,int off,int len,String srctyp,Element itm)
{
   if (file_system == null) file_system = BoardFileSystemView.getFileSystemView();

   project_name = proj;
   if (file == null) file_location = null;
   else file_location = file_system.createFileObject(file);
   file_offset = off;
   file_length = len;

   if (srctyp == null) source_type = BumpSymbolType.UNKNOWN;
   else {
      source_type = symbol_map.get(srctyp);
      if (source_type == null) source_type = BumpSymbolType.UNKNOWN;
    }

   symbol_type = BumpSymbolType.UNKNOWN;
   symbol_name = null;
   symbol_offset = 0;
   symbol_length = 0;
   symbol_key = null;
   symbol_handle = null;
   symbol_flags = -1;
   symbol_project = project_name;
   s6_source = null;

   if (itm != null) {
      symbol_name = IvyXml.getAttrString(itm,"QNAME");
      if (symbol_name == null) symbol_name = IvyXml.getAttrString(itm,"NAME");
      String typ = IvyXml.getAttrString(itm,"TYPE");
      if (typ != null) {
	 symbol_type = symbol_map.get(typ);
	 if (symbol_type == null) symbol_type = BumpSymbolType.UNKNOWN;
       }
      if (source_type == BumpSymbolType.UNKNOWN) source_type = symbol_type;
      symbol_offset = IvyXml.getAttrInt(itm,"STARTOFFSET");
      symbol_length = IvyXml.getAttrInt(itm,"LENGTH");
      symbol_key = IvyXml.getAttrString(itm,"KEY");
      symbol_handle = IvyXml.getAttrString(itm,"HANDLE");
      if (symbol_key == null) symbol_key = symbol_handle;
      else if (symbol_handle == null) symbol_handle = symbol_key;
      symbol_flags = IvyXml.getAttrInt(itm,"FLAGS");
      symbol_project = IvyXml.getAttrString(itm,"PROJECT");
      symbol_return = IvyXml.getAttrString(itm,"RETURNTYPE");
      symbol_return = IvyFormat.formatTypeName(symbol_return);
      symbol_parameters = IvyXml.getAttrString(itm,"PARAMETERS");
      symbol_parameters = IvyFormat.formatTypeName(symbol_parameters);
      if (file_location == null) {
	 String fnm = IvyXml.getAttrString(itm,"PATH");
	 if (fnm != null) file_location = file_system.createFileObject(fnm);
	 if (file_offset == 0) file_offset = symbol_offset;
	 if (file_length == 0) file_length = symbol_length;
       }
      s6_source = IvyXml.getAttrString(itm,"S6");
    }
   else {
      symbol_offset = file_offset;
      symbol_length = file_length;
    }
}



/********************************************************************************/
/*										*/
/*	Access methoeds 							*/
/*										*/
/********************************************************************************/

/**
 *	Return the project name associated with the search/location.
 **/

public String getProject()			{ return project_name; }



/**
 *	Return the file associated with the search/location/region.
 **/

public File getFile()				{ return file_location; }



/**
 *	Return the starting offset of the matched item
 **/

public int getOffset()				{ return file_offset; }



/**
 *	Return the ending offset of the matched item.
 **/

public int getEndOffset()			{ return file_offset + file_length; }



/**
 *	Return the full name of the symbol the match is defined in.
 **/

public String getSymbolName()			{ return symbol_name; }



/**
 *	Return the symbol type of the symbol the match is defined in.
 **/

public BumpSymbolType getSymbolType()		{ return symbol_type; }



/**
 *	Return the project associated with the location of the match.
 **/

public String getSymbolProject()		{ return symbol_project; }



/**
 *	Return the offset of the enclosing definition.
 **/

public int getDefinitionOffset()		{ return symbol_offset; }



/**												   PB
 *	Return the end offset of the enclosing definition.
 **/

public int getDefinitionEndOffset()		{ return symbol_offset + symbol_length; }



/**
 *	Return the symbol key (unique id) for the enclosing definition.
 **/

public String getKey()				{ return symbol_key; }



/**
 *	Return the Java modifiers for the enclosing definition.
 **/

public int getModifiers()			{ return symbol_flags; }



/**
 *	Return the symbol type of the enclosing definition
 **/

public BumpSymbolType getSourceType()		{ return source_type; }



/**
 *	Return the parameter list of the enclosing definition if it has one.
 **/

public String getParameters()
{
   return symbol_parameters;
}


/**
 *	return the return type of a function or constructor
 **/

public String getReturnType()
{
   return symbol_return;
}


/********************************************************************************/
/*										*/
/*	Name matching methods							*/
/*										*/
/********************************************************************************/

/**
 *	This routine matches two names that mich include both the method name
 *	and the set of parameters.  It is complicated and takes into account the
 *	fact that eclipse will often not provide the full resolved class name
 *	for parameters.
 **/

public boolean nameMatch(String nm)
{
   int idx = nm.indexOf("(");
   if (idx < 0) {				// no parameters case -- exact match
      return nm.equals(symbol_name);
    }

   // check if base name matches
   if (symbol_name.length() != idx) return false;
   if (!nm.startsWith(symbol_name)) {
      String xnm = symbol_name.replace('$','.');
      if (!nm.startsWith(xnm)) return false;
    }

   return compareParameters(nm.substring(idx),getParameters());
}




/**
 *	This routine matches two parameter lists for equality.	It takes into
 *	account the fact that eclipse sometimes returns unqualified type names
 *	for parameter types.
 **/

public static boolean compareParameters(String s0,String s1)
{
   List<String> lp0 = getParameterList(s0);
   List<String> lp1 = getParameterList(s1);

   if (lp0.size() != lp1.size()) return false;
   for (int i = 0; i < lp0.size(); ++i) {
      String p0 = lp0.get(i);
      String p1 = lp1.get(i);
      if (p0.equals(p1)) continue;
      int idx = p0.indexOf("<");
      if (idx > 0) {
	 p0 = p0.substring(0, idx);
      }
      idx = p1.indexOf("<");
      if (idx > 0) {
	 p1 = p1.substring(0,idx);
      }
      if (p1.equals("java.lang.Object") && p0.length() == 1) continue;  // handle generics
      if (!p1.endsWith(p0)) return false;
      int p0len = p0.length();
      int p1len = p1.length();
      if (p1.charAt(p1len-p0len-1) != '.') return false;
    }
   return true;
}




public static List<String> getParameterList(String arr)
{
   List<String> rslt = new ArrayList<String>();
   if (arr == null) return rslt;

   int start = -1;
   int lvl = 0;

   for (int i = 0; i < arr.length(); ++i) {
      char c = arr.charAt(i);
      if (c == '(') continue;
      if (lvl == 0) {
	 if (c == ')' || c == ',') {
	    if (start >= 0) rslt.add(arr.substring(start,i).trim());
	    start = -1;
	  }
	 else if (start < 0) start = i;
       }
      if (c == '<') ++lvl;
      else if (c == '>') --lvl;
    }

   return rslt;
}




/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

public void update()
{
   // TODO: the result is not accurate if there has been editing sometimes
   //	 We need to determine when this works and when it fails
   if (symbol_handle != null) {
      List<BumpLocation> rslt = BumpClient.getBump().findByKey(project_name,symbol_handle,
								  file_location);
      if (rslt == null || rslt.size() == 0) return;

      BumpLocation newl = rslt.get(0);
//	BoardLog.logD("BUMP","UPDATE CHECK " + this + " :: " + newl + " :: " +
//	       symbol_offset + "/" + symbol_length + " " +
//	       newl.symbol_offset + "/" + newl.symbol_length);
      symbol_offset = newl.symbol_offset;
      symbol_length = newl.symbol_length;
    }
}


public String getS6Source()			{ return s6_source; }



/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   return symbol_name + "@" + file_offset + ":" + file_length;
}



}	// end of class BumpLocation




/* end of BumpLocation.java */

