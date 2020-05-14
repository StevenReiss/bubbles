/********************************************************************************/
/*										*/
/*		RebaseFile.java 						*/
/*										*/
/*	File representation for REBUS						*/
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

import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RebaseFile implements RebaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		package_name;
private RebaseSource	source_data;
private String		file_text;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

RebaseFile(RebaseSource fs)
{
   source_data = fs;
   package_name = null;
   file_text = null;
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public String getFileName()		{ return source_data.getPath(); }
SourceLanguage getLanguage()		{ return source_data.getLanguage(); }
public String getProjectId()		{ return source_data.getProjectId(); }
RebaseRepo getRepository()		{ return source_data.getRepository(); }

RebaseRequest getRequest()		{ return source_data.getRequest(); }
public RebaseSource getSource()                 { return source_data; }

public String getProjectName()	
{
   RebaseProject rp = RebaseMain.getRebase().getProject(source_data.getProjectId());
   if (rp == null) return null;
   return rp.getName();
}


public String getPackageName()
{
   if (package_name == null) {
      package_name = findPackageName(getText());;
    }
   return package_name;
}

public String getText()
{
    if (file_text == null) {
       file_text = source_data.getText();
     }
    return file_text;
}

void setText(String newtxt)
{
   file_text = newtxt;
}

void resetText()
{
   file_text = null;
}



/********************************************************************************/
/*										*/
/*	Compute package name from source					 */
/*										*/
/********************************************************************************/

private String findPackageName(String text)
{
   if (text == null) return null;
   String pats = "^\\s*package\\s+([A-Za-z0-9]+(\\s*\\.\\s*[A-Za-z0-9]+)*)\\s*\\;";
   Pattern pat = Pattern.compile(pats,Pattern.MULTILINE);
   Matcher mat = pat.matcher(text);
   if (!mat.find()) 
      return "";

   String pkg = mat.group(1);
   StringTokenizer tok = new StringTokenizer(pkg,". \t\n\f");
   StringBuffer buf = new StringBuffer();
   int ctr = 0;
   while (tok.hasMoreTokens()) {
      String elt = tok.nextToken();
      if (ctr++ > 0) buf.append(".");
      buf.append(elt);
    }
   return buf.toString();
}




}	// end of class RebaseFile




/* end of RebaseFile.java */

