/********************************************************************************/
/*										*/
/*		PybaseUtil.java 						*/
/*										*/
/*	Utility methods for pybase						*/
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


package edu.brown.cs.bubbles.pybase;

import edu.brown.cs.bubbles.pybase.symbols.AbstractToken;
import edu.brown.cs.bubbles.pybase.symbols.Found;
import edu.brown.cs.bubbles.pybase.symbols.GenAndTok;
import edu.brown.cs.bubbles.pybase.symbols.NodeUtils;
import edu.brown.cs.bubbles.pybase.symbols.SourceToken;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;

import java.io.File;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

class PybaseUtil implements PybaseConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static final Map<TokenType,String> token_type_names;

static {
   token_type_names = new EnumMap<TokenType,String>(TokenType.class);
   token_type_names.put(TokenType.IMPORT,"Import");
   token_type_names.put(TokenType.CLASS,"Class");
   token_type_names.put(TokenType.FUNCTION,"Function");
   token_type_names.put(TokenType.ATTR,"Variable");
   token_type_names.put(TokenType.BUILTIN,"Builtin");
   token_type_names.put(TokenType.PARAM,"Parameter");
   token_type_names.put(TokenType.PACKAGE,"Package");
   token_type_names.put(TokenType.RELATIVE_IMPORT,"RelativeImport");
   token_type_names.put(TokenType.EPYDOC,"EpyDoc");
   token_type_names.put(TokenType.LOCAL,"Local");
   token_type_names.put(TokenType.OBJECT_FOUND_INTERFACE,"ObjectInterface");
}




/********************************************************************************/
/*										*/
/*	Symbol output methods							*/
/*										*/
/********************************************************************************/

static void outputSymbol(PybaseProject pp,IFileData file,Found fnd,IvyXmlWriter xw)
{
   GenAndTok gt = fnd.getSingle();
   AbstractToken tok = gt.getToken();
   outputSymbol(pp,file,tok,null,xw);
}


static void outputSymbol(PybaseProject pp,IFileData file,SimpleNode sn,IvyXmlWriter xw)
{
   outputSymbol(pp,file,null,sn,xw);
}


static void outputProjectSymbol(PybaseProject pp,IvyXmlWriter xw)
{
   xw.begin("ITEM");
   xw.field("TYPE","Project");
   xw.field("NAME",pp.getName());
   xw.field("PROJECT",pp.getName());
   xw.field("PATH",pp.getBasePath().getAbsolutePath());
   xw.field("SOURCE","USERSOURCE");
   xw.field("KEY",pp.getName() + "@");
   xw.end("ITEM");
}




static void outputSearchFor(SimpleNode sn,AbstractToken tok,IvyXmlWriter xw)
{
    String tnm = null;
    if (tok != null) tnm = token_type_names.get(tok.getType());
    if (tnm == null) {
       if (sn instanceof Module) tnm = "Module";
       else tnm = "StaticInitializer";
     }

    if (tnm != null && tok != null) {
       xw.begin("SEARCHFOR");
       xw.field("TYPE",tnm);
       xw.text(tok.getRepresentation());
       xw.end("SEARCHFOR");
     }
}



static void outputSymbol(PybaseProject pp,IFileData file,AbstractToken tok,SimpleNode sn,IvyXmlWriter xw)
{
    xw.begin("ITEM");
    if (pp != null) xw.field("PROJECT",pp.getName());
    if (file != null) xw.field("PATH",file.getFile().getAbsolutePath());
    String tnm = null;
    if (tok != null) tnm = token_type_names.get(tok.getType());
    if (tnm == null) {
       if (sn instanceof Module) tnm = "Module";
       else if (NodeUtils.isIfMainNode(sn)) tnm = "PythonMain";
       else tnm = "StaticInitializer";
     }
    if (tnm != null) xw.field("TYPE",tnm);
    if (tok != null) xw.field("HID",tok.hashCode());
    else xw.field("HID",sn.hashCode());

    if (tok != null) xw.field("NAME",tok.getRepresentation());
    else xw.field("NAME","<clinit>");

    SourceToken st = null;
    if (sn == null && tok instanceof SourceToken) {
       st = (SourceToken) tok;
       sn = st.getAst();
       if (sn instanceof Name && sn.parent != null && sn.parent instanceof Assign) sn = sn.parent;
     }
    if (sn != null) {
       // want to take into account specialsBefore and After at this point
       xw.field("LINE",sn.beginLine);
       xw.field("COL",sn.beginColumn);
       if (st != null) {
	  // xw.field("LINEX",st.getLineEnd(false));
	  // xw.field("COLX",st.getColEnd(false));
	  // xw.field("LINEY",st.getLineEnd(true));
	  // xw.field("COLY",st.getColEnd(true));
	  if (st.getAliased() != null) {
	     xw.field("FUNCTION",st.getAliased().name.toString());
	   }
	}
       if (file != null) {
	  File f = file.getFile();
	  if (f != null) {
	     int off1 = file.getStartOffset(sn);
	     int off2 = file.getEndOffset(sn);
	     xw.field("STARTOFFSET",off1);
	     xw.field("ENDOFFSET",off2);
	     xw.field("LENGTH",off2-off1+1);
	   }
	}

     }
    else if (tok != null) {
       xw.field("LINE",tok.getLineDefinition());
       xw.field("COL",tok.getColDefinition());
     }

    String hdl = null;
    String pkg = null;
    if (tok != null) {
       String docs = tok.getDocStr();
       if (docs != null && docs.length() > 0) {
	  xw.field("DOC",docs);      // this should be textElement
	}
       // xw.field("ORIG",tok.getOriginalRep());
       hdl = tok.getRepresentation();
       pkg = tok.getParentPackage();
     }
    else if (file != null) {
       pkg = file.getModuleName();
       hdl = "<clinit>";
     }

    // TODO: need to construct full name prefix here
    String ctx = getContextName(sn);

    if (pkg != null && pkg.length() > 0) {
       hdl = pkg + "." + ctx + hdl;
       xw.field("PACKAGE",pkg);
       xw.field("QNAME",hdl);
     }
    else {
       hdl = ctx + hdl;
    }

    if (tok != null) {
       xw.field("IMPORT",tok.isImport());
       xw.field("IMPORTFROM",tok.isImportFrom());
       // xw.field("STRING",tok.isString());
       // xw.field("WILD",tok.isWildImport());
     }

    if (tok == null) {
       // clear handle here?
     }
    else {
       switch (tok.getType()) {
	  case ATTR :
	     break;
	  case FUNCTION :
	     hdl += "()";
	     break;
	  case CLASS :
	     break;
	  case UNKNOWN :
	     if (sn != null && sn instanceof Module) break;
	     hdl = null;
	     break;
	  default :
	     hdl = null;
	     break;
	}
     }
    if (hdl != null) xw.field("HANDLE",hdl);

    xw.end("ITEM");
 }



public static String getContextName(AbstractToken tok)
{
   SimpleNode sn = null;

   if (tok instanceof SourceToken) {
       SourceToken st = (SourceToken) tok;
       sn = st.getAst();
       if (sn instanceof Name && sn.parent != null && sn.parent instanceof Assign) sn = sn.parent;
     }

   return getContextName(sn);
}



public static String getContextName(SimpleNode sn)
{
   String pfx = "";

   if (sn != null) sn = sn.parent;

   while (sn != null) {
      if (sn instanceof ClassDef) {
	 NameTok ntt = (NameTok) (((ClassDef) sn).name);
	 pfx = ntt.id + "." + pfx;
      }
      else if (sn instanceof FunctionDef) {
	 NameTok ntt = (NameTok) (((FunctionDef) sn).name);
	 pfx = ntt.id + "." + pfx;
      }

      sn = sn.parent;
   }

   return pfx;
}



/********************************************************************************/
/*										*/
/*	Search output methods							*/
/*										*/
/********************************************************************************/

static void outputSearchMatch(ISemanticData isd,AbstractToken base,AbstractToken atok,IvyXmlWriter xw)
{
   SourceToken tok = null;
   if (atok instanceof SourceToken) tok = (SourceToken) atok;
   if (tok == null) return;
   SimpleNode sn = tok.getNameOrNameTokAst(base);
   if (sn == null) return;

   xw.begin("MATCH");

   xw.field("LINE",sn.beginLine);
   xw.field("COL",sn.beginColumn);

   if (isd.getFileData() != null) {
      IFileData ifd = isd.getFileData();
      File f = isd.getFileData().getFile();
      if (f != null) {
	 xw.field("FILE",f.getPath());
	 int off = ifd.getStartOffset(sn);
	 xw.field("STARTOFFSET",off);
	 int xoff = ifd.getEndOffset(sn);
	 xw.field("ENDOFFSET",xoff);
	 xw.field("LENGTH",xoff-off+1);
       }
    }
   if (base == null) sn = null;

   PybaseProject pp = isd.getProject();
   if (pp != null) xw.field("PROJECT",pp.getName());
   outputSymbol(isd.getProject(),isd.getFileData(),tok,sn,xw);
   xw.end("MATCH");
}



/********************************************************************************/
/*										*/
/*	Handle problem output							*/
/*										*/
/********************************************************************************/

static void outputProblem(PybaseMessage m,ISemanticData isd,IvyXmlWriter xw)
{
   IFileData ifd = isd.getFileData();
   IDocument doc = ifd.getDocument();
   int sln = m.getStartLine(doc);
   int scl = m.getStartCol(doc);
   int eln = m.getEndLine(doc);
   int ecl = m.getEndCol(doc);
   String msg = m.getMessage();
   List<String> ls = m.getAdditionalInfo();

   xw.begin("PROBLEM");
   xw.field("PROJECT",isd.getProject().getName());
   xw.field("FILE",ifd.getFile().getPath());
   xw.field("MSGID",m.getType());
   xw.field("MESSAGE",msg);
   xw.field("LINE",sln);

   switch (m.getSeverity()) {
      case INFO :
      default :
	 break;
      case WARNING :
	 xw.field("WARNING",true);
	 break;
      case ERROR :
	 xw.field("ERROR",true);
	 break;
    }

   try {
      xw.field("START",doc.getLineOffset(sln-1) + scl - 1);
      xw.field("END",doc.getLineOffset(eln-1) + ecl - 1);
    }
   catch (BadLocationException e) { }

   if (ls != null) {
      for (String s : ls) {
	 xw.textElement("ARG",s);
       }
    }
   xw.end("PROBLEM");
}



/********************************************************************************/
/*										*/
/*	Handle matching 							*/
/*										*/
/********************************************************************************/

static String convertWildcardToRegex(String s)
{
   if (s == null) return null;

   StringBuffer nb = new StringBuffer(s.length()*8);
   int brct = 0;
   boolean qtfg = false;
   boolean bkfg = false;
   String star = null;

   star = "\\w*";

   nb.append('^');

   for (int i = 0; i < s.length(); ++i) {
      char c = s.charAt(i);
      if (bkfg) {
	 if (c == '\\') qtfg = true;
	 else if (!qtfg && c == ']') bkfg = false;
	 else { nb.append(c); qtfg = false; continue; }
       }
      if (c == '/' || c == '\\') {
	 if (File.separatorChar == '\\') nb.append("\\\\");
	 else nb.append(File.separatorChar);
       }
      else if (c == '@') nb.append(".*");
      else if (c == '*') nb.append(star);
      else if (c == '.') nb.append("\\.");
      else if (c == '{') { nb.append("("); ++brct; }
      else if (c == '}') { nb.append(")"); --brct; }
      else if (brct > 0 && c == ',') nb.append('|');
      else if (c == '?') nb.append(".");
      else if (c == '[') { nb.append(c); bkfg = true; }
      else nb.append(c);
    }

   nb.append('$');

   return nb.toString();
 }


}	// end of class PybaseUtil




/* end of PybaseUtil.java */

