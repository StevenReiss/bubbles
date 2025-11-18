/********************************************************************************/
/*										*/
/*		BuenoCreator.java						*/
/*										*/
/*	BUbbles Environment New Objects creator abstract creation methods	*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.bueno;

import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;
import edu.brown.cs.bubbles.bump.BumpLocation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.List;


abstract class BuenoCreator implements BuenoConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static int     tab_size;

private static int	HIDDEN_ABSTRACT = 16384;

static {
   BoardProperties bp = BoardProperties.getProperties("Bueno");
   tab_size = bp.getInt(BUENO_TEMPLATE_TAB_SIZE,8);
}



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected BuenoCreator()
{
}



/********************************************************************************/
/*										*/
/*	Creation methods for packages						*/
/*										*/
/********************************************************************************/

protected void createPackage(BuenoLocation where,BuenoProperties props)
{
   BumpClient bc = BumpClient.getBump();

   String pkg = props.getStringProperty(BuenoKey.KEY_PACKAGE);

   File dir = bc.createNewPackage(where.getProject(),pkg,false);

   setupPackage(dir);
}



protected void setupPackage(File packagedir)
{ }





/********************************************************************************/
/*										*/
/*	Creation methods for classes, interfaces, enums 			*/
/*										*/
/********************************************************************************/

protected void createType(BuenoType typ,BuenoLocation where,BuenoProperties props)
{
   StringBuffer buf = new StringBuffer();

   switch (typ) {
      case NEW_TYPE :
      case NEW_CLASS :
	 setupClass(buf,props);
	 break;
      case NEW_INTERFACE :
	 setupInterface(buf,props);
	 break;
      case NEW_ENUM :
	 setupEnum(buf,props);
	 break;
      case NEW_ANNOTATION :
	 setupAnnotation(buf,props);
	 break;
      default:
	 break;
    }

   String nm = props.getStringProperty(BuenoKey.KEY_PACKAGE);
   if (nm != null) nm += "." + props.getStringProperty(BuenoKey.KEY_NAME);
   else nm = props.getStringProperty(BuenoKey.KEY_NAME);

   BumpClient bc = BumpClient.getBump();
   bc.saveAll();
   File cf = bc.createNewClass(where.getProject(),nm,false,buf.toString());
   where.setFile(cf);
   // bc.compile(false,false,true);
}



protected void setupClass(StringBuffer buf,BuenoProperties props)
{
   props.put(BuenoKey.KEY_TYPE,"class");

   classText(buf,props);
}




protected void setupInterface(StringBuffer buf,BuenoProperties props)
{
   props.put(BuenoKey.KEY_TYPE,"interface");

   classText(buf,props);
}




protected void setupEnum(StringBuffer buf,BuenoProperties props)
{
   props.put(BuenoKey.KEY_TYPE,"enum");

   classText(buf,props);
}




protected void setupAnnotation(StringBuffer buf,BuenoProperties props)
{
   props.put(BuenoKey.KEY_TYPE,"annotation");

   classText(buf,props);
}



/********************************************************************************/
/*										*/
/*	Creation methods for modules						*/
/*										*/
/********************************************************************************/

protected void createModule(BuenoLocation where,BuenoProperties props)
{
   StringBuffer buf = new StringBuffer();

   setupModule(buf,props);

   String nm = props.getStringProperty(BuenoKey.KEY_PACKAGE) + "." +
      props.getStringProperty(BuenoKey.KEY_NAME);

   BumpClient bc = BumpClient.getBump();
   bc.saveAll();
   File cf = bc.createNewClass(where.getProject(),nm,false,buf.toString());
   where.setFile(cf);
   // bc.compile(false,false,true);
}



/********************************************************************************/
/*                                                                              */
/*      Create file                                                             */
/*                                                                              */
/********************************************************************************/

protected void createFile(BuenoLocation where,BuenoProperties props)
{
   StringBuffer buf = new StringBuffer();
   
   String full = props.getFullText();
   if (full != null) {
      buf.append(full);
    }
   else {
      setupModule(buf,props);
    }
   
   String nm = props.getStringProperty(BuenoKey.KEY_NAME);
   File f = where.getFile();
   if (f != null) { 
      File f1 = new File(f,nm);
      nm = f1.getPath();
    }
   
   BumpClient bc = BumpClient.getBump();
   bc.saveAll();
   File cf = bc.createNewClass(where.getProject(),nm,false,buf.toString());
   where.setFile(cf);
   // bc.compile(false,false,true);
}



/********************************************************************************/
/*                                                                              */
/*      Create directory and initial file                                       */
/*                                                                              */
/********************************************************************************/

protected void createDirectory(BuenoLocation where,BuenoProperties props)
{
   BumpClient bc = BumpClient.getBump();
   
   String dir = props.getStringProperty(BuenoKey.KEY_DIRECTORY);
   File f = where.getFile();
   if (f != null) {
      File f1 = new File(f,dir);
      dir = f1.getPath();
    }
   
   File dirf = bc.createNewPackage(where.getProject(),dir,false);
   if (dirf == null) return;
   
   where.setFile(dirf);
}






protected void setupModule(StringBuffer buf,BuenoProperties props)
{
   moduleText(buf,props);
}



/********************************************************************************/
/*										*/
/*	Creation methods for inner classes, etc.				*/
/*										*/
/********************************************************************************/

protected CharSequence createInnerType(BuenoType typ,BuenoProperties props)
{
   StringBuffer buf = new StringBuffer();

   switch (typ) {
      case NEW_INNER_TYPE :
      case NEW_INNER_CLASS :
	 setupInnerClass(buf,props);
	 break;
      case NEW_INNER_INTERFACE :
	 setupInnerInterface(buf,props);
	 break;
      case NEW_INNER_ENUM :
	 setupInnerEnum(buf,props);
	 break;
      default:
	 break;
    }

   return buf;
}



protected void setupInnerClass(StringBuffer buf,BuenoProperties props)
{
   props.put(BuenoKey.KEY_TYPE,"class");

   innerClassText(buf,props);
}




protected void setupInnerInterface(StringBuffer buf,BuenoProperties props)
{
   props.put(BuenoKey.KEY_TYPE,"interface");

   innerClassText(buf,props);
}




protected void setupInnerEnum(StringBuffer buf,BuenoProperties props)
{
   props.put(BuenoKey.KEY_TYPE,"enum");

   innerClassText(buf,props);
}




/********************************************************************************/
/*										*/
/*	Creation methods for methods						*/
/*										*/
/********************************************************************************/

protected CharSequence createMethod(BuenoType typ,BuenoProperties props)
{
   StringBuffer buf = new StringBuffer();
   switch (typ) {
      case NEW_CONSTRUCTOR :
	 setupConstructor(buf,props);
	 break;
      case NEW_METHOD :
	 String c1 = props.getClassName();
	 String prj = props.getProjectName();
	 List<BumpLocation> bls = BumpClient.getBump().findClassDefinition(prj,c1);
	 if (bls != null && bls.size() == 1) {
	    BumpLocation bl = bls.get(0);
	    if (bl.getSymbolType() == BumpConstants.BumpSymbolType.INTERFACE) {
	       int mod = props.getModifiers();
	       mod |= HIDDEN_ABSTRACT;
	       props.put(BuenoKey.KEY_MODIFIERS,mod);
	    }
	 }
	 setupMethod(buf,props);
	 break;
      case NEW_GETTER :
	 setupGetter(buf,props);
	 break;
      case NEW_SETTER :
	 setupSetter(buf,props);
	 break;
      case NEW_GETTER_SETTER :
	 setupGetterSetter(buf,props);
	 break;
      default:
	 break;
    }

   return buf;
}




protected void setupConstructor(StringBuffer buf,BuenoProperties props)
{
   if (props.getStringProperty(BuenoKey.KEY_RETURNS) != null) {
      props.remove(BuenoKey.KEY_RETURNS);
    }

   if (props.getStringProperty(BuenoKey.KEY_CONTENTS) == null) {
      props.put(BuenoKey.KEY_CONTENTS,"// constructor body goes here");
    }

   methodText(buf,props);
}


protected void setupMethod(StringBuffer buf,BuenoProperties props)
{
   String returns = props.getStringProperty(BuenoKey.KEY_RETURNS);

   if (returns == null) {
      props.put(BuenoKey.KEY_RETURNS,"void");
    }
   // CHECK IF NAME IS CORRECT HERE
   if (props.getStringProperty(BuenoKey.KEY_CONTENTS) == null) {
      props.put(BuenoKey.KEY_CONTENTS,"// method body goes here");
    }

   if (returns != null && !returns.equals("void") && 
         props.getStringProperty(BuenoKey.KEY_RETURN_STMT) == null) {
      String rstmt = null;
      if (returns.equals("boolean")) rstmt = "return false;";
      else if (returns.equals("int") || returns.equals("float") ||
		  returns.equals("double") || returns.equals("short") ||
		  returns.equals("byte") || returns.equals("char"))
	 rstmt = "return 0;";
      else rstmt = "return null;";
      props.put(BuenoKey.KEY_RETURN_STMT,rstmt);
    }

   methodText(buf,props);
}


protected void setupGetter(StringBuffer buf,BuenoProperties props)
{
   if (props.getStringProperty(BuenoKey.KEY_RETURN_STMT) == null) {
      String fnm = props.getFieldName();
      if (fnm != null) {
	 String rtrn = "return " + fnm + ";";
	 props.put(BuenoKey.KEY_RETURN_STMT,rtrn);
	 if (props.getStringProperty(BuenoKey.KEY_CONTENTS) == null) {
	    props.put(BuenoKey.KEY_CONTENTS,"");
	  }
       }
    }
   setupMethod(buf,props);
}


protected void setupSetter(StringBuffer buf,BuenoProperties props)
{
   String fnm = props.getFieldName();
   String argnm = null;
   String [] args = props.getParameters();
   String ftyp = props.getStringProperty(BuenoKey.KEY_FIELD_TYPE);
   if (args == null || args.length == 0) {
      argnm = fnm.toLowerCase();
      argnm = argnm.replace("_","");
      props.put(BuenoKey.KEY_PARAMETERS,ftyp + " " + argnm);
    }
   else {
      int idx = args[0].lastIndexOf(" ");
      argnm = args[0].substring(idx+1).trim();
    }

   if (props.getStringProperty(BuenoKey.KEY_CONTENTS) == null) {
      String cnts = "";
      if (argnm.equals(fnm)) cnts = "this.";
      cnts += fnm + " = " + argnm + ";";
      props.put(BuenoKey.KEY_CONTENTS,cnts);
    }

   setupMethod(buf,props);
}


protected void setupGetterSetter(StringBuffer buf,BuenoProperties props)
{
   setupGetter(buf,props);
   setupSetter(buf,props);
}



/********************************************************************************/
/*										*/
/*	Creation methods for fields						*/
/*										*/
/********************************************************************************/

protected CharSequence createField(BuenoProperties props)
{
   StringBuffer buf = new StringBuffer();

   setupField(buf,props);

   return buf;
}



protected void setupField(StringBuffer buf,BuenoProperties props)
{
   fieldText(buf,props);
}



/********************************************************************************/
/*										*/
/*	Creation methods for comments						*/
/*										*/
/********************************************************************************/

protected CharSequence createComment(BuenoType typ,BuenoProperties props)
{
   String txt = (String) props.get(BuenoKey.KEY_COMMENT);
   if (txt == null) {
      props.put(BuenoKey.KEY_COMMENT,"<comment here>");
    }

   StringBuffer buf = new StringBuffer();

   switch (typ) {
      case NEW_MARQUIS_COMMENT :
	 setupMarquisComment(buf,props);
	 break;
      case NEW_BLOCK_COMMENT :
	 setupBlockComment(buf,props);
	 break;
      case NEW_JAVADOC_COMMENT :
	 setupJavadocComment(buf,props);
	 break;
      default:
	 break;
    }

   return buf;
}



protected void setupMarquisComment(StringBuffer buf,BuenoProperties props)
{
   StringBuffer pbuf = new StringBuffer();
   pbuf.append("/********************************************************************************/\n");
   pbuf.append("/*                                                                              */\n");
   pbuf.append("/*       ${COMMENT}                                                             */\n");
   pbuf.append("/*                                                                              */\n");
   pbuf.append("/********************************************************************************/\n");

   StringReader sr = new StringReader(pbuf.toString());
   try {
      expand(sr,props,buf);
    }
   catch (IOException e) { }
}




protected void setupBlockComment(StringBuffer buf,BuenoProperties props)
{
   StringBuffer pbuf = new StringBuffer();
   pbuf.append("/*\n");
   pbuf.append(" * ${COMMENT}");
   pbuf.append("\n");
   pbuf.append(" */\n");

   StringReader sr = new StringReader(pbuf.toString());
   try {
      expand(sr,props,buf);
    }
   catch (IOException e) { }
}




protected void setupJavadocComment(StringBuffer buf,BuenoProperties props)
{
   String txt = (String) props.get(BuenoKey.KEY_COMMENT);

   buf.append("/**\n");
   buf.append(" *");
   if (txt != null) buf.append("      " + txt);
   buf.append("\n");
   buf.append("**/\n");
}



private void setupBlockComment(StringBuffer buf,BuenoProperties props,String text)
{
   String p = props.getStringProperty(BuenoKey.KEY_COMMENT);
   props.put(BuenoKey.KEY_COMMENT,text);

   setupBlockComment(buf,props);

   if (p == null) props.remove(BuenoKey.KEY_COMMENT);
   else props.put(BuenoKey.KEY_COMMENT,p);
}



private void setupJavadocComment(StringBuffer buf,BuenoProperties props,String text)
{
   String p = props.getStringProperty(BuenoKey.KEY_COMMENT);
   props.put(BuenoKey.KEY_COMMENT,text);

   setupJavadocComment(buf,props);

   if (p == null) props.remove(BuenoKey.KEY_COMMENT);
   else props.put(BuenoKey.KEY_COMMENT,p);
}



/********************************************************************************/
/*										*/
/*	Simple method creation							*/
/*										*/
/********************************************************************************/

protected void methodText(StringBuffer buf,BuenoProperties props)
{
   String full = props.getFullText();
   if (full != null) {
      buf.append(full);
      return;
    }
   
   StringBuffer pbuf = new StringBuffer();
   pbuf.append("\n\n");
   if (props.getBooleanProperty(BuenoKey.KEY_ADD_COMMENT)) {
      pbuf.append("/*\n");
      pbuf.append(" * ${COMMENT}\n");
      pbuf.append(" */\n\n");
    }
   else if (props.getBooleanProperty(BuenoKey.KEY_ADD_JAVADOC)) {
      pbuf.append("/**");
      pbuf.append(" * ${COMMENT}\n");
      pbuf.append("**/\n\n");
    }
   pbuf.append("$(ITAB)$(ATTRIBUTES)$(MODIFIERS)$(RETURNS) $(NAME)($(PARAMETERS))");

   int mods = props.getModifiers();
   if (Modifier.isAbstract(mods) || Modifier.isNative(mods) ||
	    (mods & HIDDEN_ABSTRACT) != 0) {
      pbuf.append(";\n");
    }
   else {
      pbuf.append("\n{\n");
      String cnts = props.getStringProperty(BuenoKey.KEY_CONTENTS);
      if (cnts != null && cnts.length() > 0) pbuf.append("$(TAB)${CONTENTS}\n");
      String rstmt = props.getStringProperty(BuenoKey.KEY_RETURN_STMT);
      if (rstmt != null) {
         if (cnts != null && cnts.length() > 0) pbuf.append("\n");
	 pbuf.append("$(TAB)$(RETURN_STMT)\n");
       }
      pbuf.append("$(ITAB)}\n");
    }

   pbuf.append("\n\n");

   StringReader sr = new StringReader(pbuf.toString());
   try {
      expand(sr,props,buf);
    }
   catch (IOException e) { }
}




/********************************************************************************/
/*										*/
/*	Simple Class creation							*/
/*										*/
/********************************************************************************/

protected void classText(StringBuffer buf,BuenoProperties props)
{
   String full = props.getFullText();
   if (full != null) {
      buf.append(full);
      return;
    }

   String pkg = props.getStringProperty(BuenoKey.KEY_PACKAGE);

   if (pkg != null) {
      buf.append("package " + pkg + ";\n");
    }

   buf.append("\n");

   String [] imps = props.getImports();
   if (imps != null && imps.length > 0) {
      for (String s : imps) {
	 buf.append(s + "\n");
       }
    }
   buf.append("\n");

   String cmmt = props.getStringProperty(BuenoKey.KEY_COMMENT);
   if (props.getBooleanProperty(BuenoKey.KEY_ADD_JAVADOC)) {
      setupJavadocComment(buf,props,cmmt);
    }
   else if (props.getBooleanProperty(BuenoKey.KEY_ADD_COMMENT)) {
      setupBlockComment(buf,props,cmmt);
    }

   int mods = props.getModifiers();
   int ct = 0;
   ct = addModifier(buf,"private",Modifier.isPrivate(mods),ct);
   ct = addModifier(buf,"public",Modifier.isPublic(mods),ct);
   ct = addModifier(buf,"protected",Modifier.isProtected(mods),ct);
   ct = addModifier(buf,"abstract",Modifier.isAbstract(mods),ct);
   ct = addModifier(buf,"static",Modifier.isStatic(mods),ct);
   ct = addModifier(buf,"native",Modifier.isNative(mods),ct);
   ct = addModifier(buf,"final",Modifier.isFinal(mods),ct);
   if (ct > 0) buf.append(" ");

   String typ = props.getStringProperty(BuenoKey.KEY_TYPE);
   if (typ == null) typ = "class";
   String nam = props.getStringProperty(BuenoKey.KEY_NAME);

   buf.append(typ + " " + nam);
   outputList(props.getExtends()," extends ",", ",buf);
   outputList(props.getImplements()," implements ",", ",buf);
   buf.append(" {\n");
   buf.append("\n");

   String cnts = props.getStringProperty(BuenoKey.KEY_CONTENTS);
   if (cnts != null) buf.append(cnts); // insert contents here
   buf.append("\n");
   buf.append("}\t// end of " + typ + " " + nam + "\n");
}



/********************************************************************************/
/*										*/
/*	Simple Class creation							*/
/*										*/
/********************************************************************************/

protected void moduleText(StringBuffer buf,BuenoProperties props)
{
   String cmmt = props.getStringProperty(BuenoKey.KEY_COMMENT);
   if (props.getBooleanProperty(BuenoKey.KEY_ADD_COMMENT)) {
      setupBlockComment(buf,props,cmmt);
    }

   String [] imps = props.getImports();
   if (imps != null && imps.length > 0) {
      for (String s : imps) {
	 buf.append(s + "\n");
       }
    }
   buf.append("\n");
}






/********************************************************************************/
/*										*/
/*	Simple inner class creation						*/
/*										*/
/********************************************************************************/

protected void innerClassText(StringBuffer buf,BuenoProperties props)
{
   String iind = props.getInitialIndentString();

   buf.append("\n\n");

   String cmmt = props.getStringProperty(BuenoKey.KEY_COMMENT);
   if (props.getBooleanProperty(BuenoKey.KEY_ADD_JAVADOC)) {
      setupJavadocComment(buf,props,cmmt);
    }
   else if (props.getBooleanProperty(BuenoKey.KEY_ADD_COMMENT)) {
      setupBlockComment(buf,props,cmmt);
    }

   buf.append(iind);
   int mods = props.getModifiers();
   int ct = 0;
   ct = addModifier(buf,"private",Modifier.isPrivate(mods),ct);
   ct = addModifier(buf,"public",Modifier.isPublic(mods),ct);
   ct = addModifier(buf,"protected",Modifier.isProtected(mods),ct);
   ct = addModifier(buf,"abstract",Modifier.isAbstract(mods),ct);
   ct = addModifier(buf,"static",Modifier.isStatic(mods),ct);
   ct = addModifier(buf,"native",Modifier.isNative(mods),ct);
   ct = addModifier(buf,"final",Modifier.isFinal(mods),ct);
   if (ct > 0) buf.append(" ");

   String typ = props.getStringProperty(BuenoKey.KEY_TYPE);
   if (typ == null) typ = "class";
   String nam = props.getStringProperty(BuenoKey.KEY_NAME);

   buf.append(typ + " " + nam);

   outputList(props.getExtends()," extends ",", ",buf);
   outputList(props.getImplements()," implements ",", ",buf);

   buf.append(" {\n");
   buf.append("\n");
   buf.append("\n");
   buf.append("}\t// end of inner " + typ + " " + nam + "\n");
   buf.append("}\n");
   buf.append("\n\n");
}



/********************************************************************************/
/*										*/
/*	Simple field creation							*/
/*										*/
/********************************************************************************/

protected void fieldText(StringBuffer buf,BuenoProperties props)
{
   String full = props.getFullText();
   if (full != null) {
      buf.append(full);
      return;
    }
   
   String iind = props.getInitialIndentString();

   String cmmt = props.getStringProperty(BuenoKey.KEY_COMMENT);
   if (props.getBooleanProperty(BuenoKey.KEY_ADD_JAVADOC)) {
      setupJavadocComment(buf,props,cmmt);
    }
   else if (props.getBooleanProperty(BuenoKey.KEY_ADD_COMMENT)) {
      setupBlockComment(buf,props,cmmt);
    }

   buf.append(iind);
   int mods = props.getModifiers();
   int ct = 0;
   ct = addModifier(buf,"private",Modifier.isPrivate(mods),ct);
   ct = addModifier(buf,"public",Modifier.isPublic(mods),ct);
   ct = addModifier(buf,"protected",Modifier.isProtected(mods),ct);
   ct = addModifier(buf,"abstract",Modifier.isAbstract(mods),ct);
   ct = addModifier(buf,"static",Modifier.isStatic(mods),ct);
   ct = addModifier(buf,"final",Modifier.isFinal(mods),ct);
   ct = addModifier(buf,"strictfp",Modifier.isStrict(mods),ct);
   if (ct > 0) buf.append(" ");

   String typ = props.getStringProperty(BuenoKey.KEY_RETURNS);
   if (typ == null) typ = "int";
   String nam = props.getStringProperty(BuenoKey.KEY_NAME);

   buf.append(typ + " " + nam);

   String val = props.getStringProperty(BuenoKey.KEY_INITIAL_VALUE);
   if (val != null) {
      buf.append(" = ");
      buf.append(val);
    }

   buf.append(";\n");
}



/********************************************************************************/
/*										*/
/*	Utility routines							*/
/*										*/
/********************************************************************************/

private static int addModifier(StringBuffer buf,String txt,boolean add,int ct)
{
   if (!add) return ct;

   if (ct > 0) buf.append(" ");
   buf.append(txt);
   return ct+1;
}



/********************************************************************************/
/*										*/
/*	Template finding methods						*/
/*										*/
/********************************************************************************/

static Reader findTemplate(String id,BuenoProperties props)
{
   if (id == null) return null;

   // try project-specific template first
   String prj = props.getStringProperty(BuenoKey.KEY_PROJECT);
   InputStreamReader rslt = checkTemplateForProject(prj,id);
   if (rslt != null) return rslt;
   
   // try workspace-specific template next
   String ws = BoardSetup.getSetup().getDefaultWorkspace();
   int idx = ws.lastIndexOf(File.separator);
   if (idx > 0) ws = ws.substring(idx+1);
   rslt = checkTemplateForProject(ws,id);
   if (rslt != null) return rslt;
   
   // try user default template next
   rslt = checkTemplateForProject("DEFAULT",id);
   if (rslt != null) return rslt;
   
   rslt = checkTemplateForLanguage(id);
   if (rslt != null) return rslt;
  
   // else use default template
   String pnm = "templates/" + id + ".template";
   InputStream ins = BoardProperties.getResourceFile(pnm);
   if (ins != null) return new InputStreamReader(ins);
   
   return null;
}


private static InputStreamReader checkTemplateForProject(String prj,String id) 
{
   if (prj == null) return null;
   
   String xnm = "templates/" + prj + "/" + id + ".template";
   InputStream ins = BoardProperties.getResourceFile(xnm);
   if (ins != null) return new InputStreamReader(ins);
   xnm = "templates/" + prj.toLowerCase() + "/" + id + ".template";
   ins = BoardProperties.getResourceFile(xnm);
   if (ins != null) return new InputStreamReader(ins);   
   
   return null;
}


private static InputStreamReader checkTemplateForLanguage(String id) 
{
   String lang = BoardSetup.getSetup().getLanguage().getName();
   
   String xnm = "templates/" + id + "_" + lang + ".template";
   InputStream ins = BoardProperties.getResourceFile(xnm);
   if (ins != null) return new InputStreamReader(ins);
   
   return null;
}




/********************************************************************************/
/*										*/
/*	Template expansion routines						*/
/*										*/
/********************************************************************************/

static void expand(Reader from,BuenoProperties props,StringBuffer buf) throws IOException
{
   BufferedReader br = new BufferedReader(from);
   String eol = "\n";

   for ( ; ; ) {
      String ln = br.readLine();
      if (ln == null) break;
      ln = expandTabs(ln,tab_size);
      for (int i = 0; i < ln.length(); ++i) {
	 char c = ln.charAt(i);
	 if (c == '$' && ln.length() > i+1 && ln.charAt(i+1) == '(') {
	    StringBuffer tok = new StringBuffer();
	    for (i = i+2; i < ln.length() && ln.charAt(i) != ')'; ++i) {
	       tok.append(ln.charAt(i));
	     }
	    if (i >= ln.length()) throw new IOException("Unterminated token");
	    String rslt = getValue(tok.toString(),props,eol);
	    if (rslt != null) buf.append(rslt);
	  }
	 else if (c == '$' && ln.length() > i+1 && ln.charAt(i+1) == '{') {
	    StringBuffer tok = new StringBuffer();
	    for (i = i+2; i < ln.length() && ln.charAt(i) != '}'; ++i) {
	       tok.append(ln.charAt(i));
	     }
	    if (i >= ln.length()) throw new IOException("Unterminated token");
	    String sfx = ln.substring(i+1);
	    int lst = buf.length()-1;
	    while (lst > 0 && buf.charAt(lst-1) != '\n') --lst;
	    String pfx = buf.substring(lst);
	    getFormattedValue(tok.toString(),props,pfx,sfx,eol,buf);
	    break;
	  }
	 else buf.append(c);
       }
      buf.append(eol);
    }
}



private static String getValue(String key,BuenoProperties props,String eol)
{
   StringBuffer buf = new StringBuffer();
   int ct = 0;

   if (key.equals("MODIFIERS")) {
      int mods = props.getModifiers();
      ct = addModifier(buf,"@Override",(mods & MODIFIER_OVERRIDES) != 0,ct);
      ct = addModifier(buf,"private",Modifier.isPrivate(mods),ct);
      ct = addModifier(buf,"public",Modifier.isPublic(mods),ct);
      ct = addModifier(buf,"protected",Modifier.isProtected(mods),ct);
      ct = addModifier(buf,"abstract",Modifier.isAbstract(mods),ct);
      ct = addModifier(buf,"static",Modifier.isStatic(mods),ct);
      ct = addModifier(buf,"native",Modifier.isNative(mods),ct);
      ct = addModifier(buf,"final",Modifier.isFinal(mods),ct);
      ct = addModifier(buf,"strictfp",Modifier.isStrict(mods),ct);
      ct = addModifier(buf,"synchronized",Modifier.isSynchronized(mods),ct);
      ct = addModifier(buf,"transient",Modifier.isTransient(mods),ct);
      ct = addModifier(buf,"volatile",Modifier.isVolatile(mods),ct);
      if (ct > 0) buf.append(" ");
   }
   else if (key.equals("PARAMETERS")) {
      outputList(props.getParameters(),null,",",buf);
   }
   else if (key.equals("IMPORTS")) {
      outputList(props.getImports(),null,eol,buf);
   }
   else if (key.equals("THROWS")) {
      outputList(props.getThrows(),"throws ",", ",buf);
   }
   else if (key.equals("EXTENDS")) {
      outputList(props.getExtends()," extends ",", ",buf);
   }
   else if (key.equals("IMPLEMENTS")) {
      outputList(props.getImplements()," implements ",", ",buf);
   }
   else if (key.equals("DATE")) {
      buf.append(new Date().toString());
    }
   else if (key.equals("PACKAGE")) {
      String v = props.getStringProperty(BuenoKey.KEY_PACKAGE);
      if (v != null) buf.append("package " + v + ";");
    }
   else if (key.equals("ITAB")) {
      buf.append(props.getInitialIndentString());
    }
   else if (key.equals("TAB")) {
      buf.append(props.getInitialIndentString());
      buf.append(props.getIndentString());
    }
   else if (key.equals("ATTRIBUTES")) {
      String v = props.getStringProperty(BuenoKey.KEY_ATTRIBUTES);
      if (v != null) buf.append(v + " ");
    }
   else if (key.equals("AUTHOR")) {
      String v = props.getStringProperty(BuenoKey.KEY_AUTHOR);
      if (v == null) v = System.getProperty("user.name");
      if (v != null) buf.append(v);
    }
   else {
      String pnm = BUENO_PROPERTY_HEAD + key;
      BoardProperties bp = BoardProperties.getProperties("Bueno");
      String v = bp.getProperty(pnm);
      if (v != null) buf.append(v);
      else {
	 String nm = "KEY_" + key;
	 try {
	    BuenoKey k = BuenoKey.valueOf(nm);
	    v = props.getStringProperty(k);
	    if (v != null) buf.append(v);
	  }
	 catch (IllegalArgumentException e) { }
       }
   }

   return buf.toString();
}


private static void outputList(String [] itms,String pfx,String sep,StringBuffer buf)
{
   if (itms == null) return;
   int ct = 0;
   for (String s : itms) {
      if (ct++ != 0) buf.append(sep);
      else if (pfx != null) buf.append(pfx);
      buf.append(s.trim());
   }
}



private static void getFormattedValue(String key,BuenoProperties props,String pfx,
      String sfx,String eol,StringBuffer buf)
{
   int p0 = pfx.length() + key.length() + 3;   // ${ ... }
   int p2 = 0;
   while (p2 < sfx.length() && !Character.isWhitespace(sfx.charAt(p2))) ++p2;
   String adder = sfx.substring(0,p2);
   int p1 = p2;
   while (p1 < sfx.length() && Character.isWhitespace(sfx.charAt(p1))) ++p1;
   if (p1 >= sfx.length()) sfx = null;
   else sfx = sfx.substring(p1);
   p0 += p1;					// this is where sfx starts

   String value = getValue(key,props,eol);
   if (value == null) value = "";
   value += adder;
   value = value.replace(eol,"\n");

   int idx = value.indexOf("\n");
   while (idx >= 0) {
      String v0 = value.substring(0,idx);
      value = value.substring(idx+1);
      v0 = v0.trim();
      buf.append(v0);
      int v1 = v0.length() + pfx.length();
      if (sfx != null) {
	 if (v1 >= p0) {
            buf.append(" ");
          }
	 else {
            for (int i = v1; i < p0; ++i) {
               buf.append(" ");
             }
          }
	 buf.append(sfx);
      }
      buf.append(eol);
      buf.append(pfx);
      idx = value.indexOf("\n");
   }

   value = value.trim();
   buf.append(value);
   int v2 = value.length() + pfx.length();
   if (sfx != null) {
      if (v2 >= p0) {
         buf.append(" ");
       }
      else {
         for (int i = v2; i < p0; ++i) {
            buf.append(" ");
          }
       }
      buf.append(sfx);
   }
}




private static String expandTabs(String self,int tabstop)
{
   int index;

   while ((index = self.indexOf('\t')) != -1) {
      StringBuilder builder = new StringBuilder(self);
      int count = tabstop - index % tabstop;
      builder.deleteCharAt(index);
      for (int i = 0; i < count; i++) {
	 builder.insert(index," ");
       }
      self = builder.toString();
    }

   return self;
}




}	// end of class BuenoCreator




/* end of BuenoCreator.java */
