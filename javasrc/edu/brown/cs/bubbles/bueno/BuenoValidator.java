/********************************************************************************/
/*										*/
/*		BuenoValidator.java						*/
/*										*/
/*	description of class							*/
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



package edu.brown.cs.bubbles.bueno;

import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class BuenoValidator implements BuenoConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BuenoValidatorCallback callback_handler;

private ParseCheck		parse_checker;
private Object			check_lock;

private BuenoType		create_type;
private BuenoProperties 	property_set;
private BuenoLocation		insertion_point;

private static final Pattern package_pattern =
   Pattern.compile("[A-Za-z_]\\w*(\\.[A-Za-z_]\\w*)*");

private static final Pattern module_pattern =
   Pattern.compile("[A-Za-z_]\\w*");




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public BuenoValidator(BuenoValidatorCallback cb,BuenoProperties known,
      BuenoLocation insert,BuenoType typ)
{
   callback_handler = cb;
   create_type = typ;
   check_lock = new Object();

   if (known == null) known = new BuenoProperties();
   property_set = known;
   insertion_point = insert;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public BuenoType getCreationType()		{ return create_type; }

public BuenoProperties getProperties()		{ return property_set; }

public String getClassName()
{
   // get the full name of the new class
   StringBuffer buf = new StringBuffer();
   String pkg = insertion_point.getPackage();
   if (pkg != null) {
      buf.append(pkg);
      buf.append(".");
    }
   buf.append(property_set.getStringProperty(BuenoKey.KEY_NAME));

   return buf.toString();
}


public String getMethodName()
{
   // get the full method name for a new method

   StringBuffer buf = new StringBuffer();
   String cls = insertion_point.getClassName();
   buf.append(cls);
   buf.append(".");
   buf.append(property_set.getStringProperty(BuenoKey.KEY_NAME));

   buf.append("(");
   String [] params = property_set.getParameters();
   for (int i = 0; i < params.length; ++i) {
      if (i > 0) buf.append(",");
      int idx = params[i].lastIndexOf(" ");
      String typ = params[i].substring(0,idx);
      buf.append(typ);
    }
   buf.append(")");

   return buf.toString();
}




/********************************************************************************/
/*										*/
/*	Signature generation methods						*/
/*										*/
/********************************************************************************/

public String getSignature()
{
   StringBuffer buf = new StringBuffer();
   
   buf.append(property_set.getModifierString());

   String nm = property_set.getStringProperty(BuenoKey.KEY_NAME);
   if (nm == null) nm = "???";

   switch (create_type) {
      case NEW_CLASS :
      case NEW_INNER_CLASS :
	 buf.append("class ");
	 buf.append(nm);
	 addList(buf,property_set.getExtends(),"extends");
	 addList(buf,property_set.getImplements(),"implements");
	 break;
      case NEW_INTERFACE :
      case NEW_INNER_INTERFACE :
	 buf.append("interface ");
	 buf.append(nm);
	 addList(buf,property_set.getExtends(),"extends");
	 break;
      case NEW_ENUM :
      case NEW_INNER_ENUM :
	 buf.append("enum ");
	 buf.append(nm);
	 addList(buf,property_set.getExtends(),"extends");
	 addList(buf,property_set.getImplements(),"implements");
	 break;
      case NEW_METHOD :
	 String ret = property_set.getStringProperty(BuenoKey.KEY_RETURNS);
	 if (ret == null) ret = "???";
	 buf.append(ret);
	 buf.append(" ");
	 buf.append(property_set.getStringProperty(BuenoKey.KEY_NAME));
	 buf.append("(");
	 addList(buf,property_set.getParameters(),null);
	 buf.append(")");
	 break;
      case NEW_CONSTRUCTOR :
	 buf.append(nm);
	 buf.append("(");
	 addList(buf,property_set.getParameters(),null);
	 buf.append(")");
	 break;
      case NEW_PACKAGE :
         String s = property_set.getStringProperty(BuenoKey.KEY_SIGNATURE);
         return s;
      default :
	 return null;
    }

   return buf.toString();
}


private void addList(StringBuffer buf,String [] elts,String pfx)
{
   if (elts != null && elts.length > 0) {
      if (buf.charAt(buf.length() - 1) != ' ') buf.append(" ");
      if (pfx != null) buf.append(pfx);
      int ct = 0;
      for (String s : elts) {
	 if (ct++ > 0) buf.append(",");
	 if (pfx != null) buf.append(" ");
	 buf.append(s);
       }
      buf.append(" ");
    }
}

/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

public void updateParsing()
{
   synchronized (check_lock) {
      if (parse_checker == null) {
	 parse_checker = new ParseCheck();
	 BoardThreadPool.start(parse_checker);
       }
      else parse_checker.rerun();
    }
}


public boolean checkParsing()
{
   synchronized (check_lock) {
      updateParsing();
      // parse_checker is valid here since we are locked
      return parse_checker.waitForDone();
    }
}


public List<String> checkInterfaces(String nm)
{
   List<String> rslt = new ArrayList<String>();
   StreamTokenizer tok = createStreamTokenizer(nm);
   if (nm == null) return rslt;
   nm = nm.trim();
   if (nm.length() == 0) return rslt;

   try {
      for ( ; ; ) {
	 String typ = parseType(tok);
	 rslt.add(typ);
	 if (!checkNextToken(tok,',')) break;
       }
      parseEnd(tok);
    }
   catch (BuenoException e) {
      return null;
    }

   return rslt;
}


public List<String> checkParameters(String arg)
{
   List<String> rslt = new ArrayList<String>();
   if (arg == null) return rslt;
   arg = arg.trim();
   if (arg.length() == 0) return rslt;

   StreamTokenizer stok = createStreamTokenizer(arg);
   try {
      for ( ; ; ) {
	 String typ = parseType(stok);
	 String anm = null;
	 if (nextToken(stok) == StreamTokenizer.TT_WORD) {
	    anm = stok.sval;
	    while (checkNextToken(stok,'[')) {
	       if (!checkNextToken(stok,']')) throw new BuenoException("Bad array parameter");
	       typ += "[]";
	     }
	  }
	 else throw new BuenoException("Expected agrument name");
	 rslt.add(typ + " " + anm);
	 if (!checkNextToken(stok,',')) break;
       }
      parseEnd(stok);
    }
   catch (BuenoException e) {
      return null;
    }

   return rslt;
}



private class ParseCheck implements Runnable {

   private boolean run_again;
   private Boolean check_valid;

   ParseCheck() {
      run_again = false;
      check_valid = null;
    }

   void rerun() {
      run_again = true;
    }

   boolean waitForDone() {
      synchronized (check_lock) {
	 while (check_valid == null) {
	    try {
	       check_lock.wait();
	     }
	    catch (InterruptedException e) { }
	  }
	 return check_valid;
       }
    }

   @Override public void run() {
      for ( ; ; ) {
         boolean fg = false;
         try {
            switch (create_type) {
               case NEW_CLASS :
               case NEW_INTERFACE :
               case NEW_ENUM :
               case NEW_TYPE :
                  fg = checkClassParsing();
                  break;
               case NEW_PACKAGE :
                  switch (BoardSetup.getSetup().getLanguage()) {
                     case JAVA :
                     case REBUS :
                        fg = checkPackageParsing();
                        break;
                     case PYTHON :
                        fg = checkPythonPackageParsing();
                        break;
                     case JS :
                        // fg = checkJSPackageParsing();
                        fg = false;
                        break;
                   }
                  break;
               case NEW_METHOD :
               case NEW_CONSTRUCTOR :
                  fg = checkMethodParsing();
                  break;
               case NEW_INNER_CLASS :
               case NEW_INNER_INTERFACE :
               case NEW_INNER_ENUM :
               case NEW_INNER_TYPE :
                  fg = checkInnerClassParsing();
                  break;
               case NEW_FIELD :
                  fg = checkFieldParsing();
                  break;
               case NEW_ANNOTATION :
               case NEW_BLOCK_COMMENT :
               case NEW_JAVADOC_COMMENT :
               case NEW_MARQUIS_COMMENT :
               case NEW_GETTER :
               case NEW_GETTER_SETTER :
               case NEW_SETTER :
                  break;
               case NEW_MODULE :
                  fg = checkPythonModuleParsing();
                  break;
             }
          }
         catch (Throwable t) {
            BoardLog.logE("BUENO","Problem validating signature",t);
            fg = false;
          }
         synchronized (check_lock) {
            if (!run_again) {
               parse_checker = null;
               check_valid = fg;
               check_lock.notifyAll();
               break;
             }
            run_again = false;
          }
       }
      if (callback_handler != null) {
         callback_handler.validationDone(BuenoValidator.this,check_valid);
       }
   }

}	// end of inner class ParseCheck



/********************************************************************************/
/*										*/
/*	Methods to parse and check results :: JAVA				*/
/*										*/
/********************************************************************************/

private boolean checkPackageParsing()
{
   String pkg = property_set.getStringProperty(BuenoKey.KEY_PACKAGE);

   if (pkg == null || pkg.length() == 0) return false;

   Matcher m = package_pattern.matcher(pkg);
   if (!m.matches()) return false;

   String sgn = property_set.getStringProperty(BuenoKey.KEY_SIGNATURE);
   if (sgn != null) {
      String onm = property_set.getStringProperty(BuenoKey.KEY_NAME);
      property_set.remove(BuenoKey.KEY_NAME);
      try {
         parseClassSignature(sgn);
       }
      catch (BuenoException e) {
	 return false;
       }
      finally {
         String cnm = property_set.getStringProperty(BuenoKey.KEY_NAME);
         property_set.put(BuenoKey.KEY_CLASS_NAME,cnm);
         property_set.put(BuenoKey.KEY_NAME,onm);
         create_type = BuenoType.NEW_PACKAGE;
       }
    }

   if (property_set.getStringProperty(BuenoKey.KEY_NAME) == null) return false;

   return true;
}



private boolean checkClassParsing()
{
   String sgn = property_set.getStringProperty(BuenoKey.KEY_SIGNATURE);
   if (sgn == null) sgn = getSignature();
   if (sgn != null) {
      try {
	 parseClassSignature(sgn);
      }
      catch (BuenoException e) {
	 return false;
      }
   }

   if (property_set.getStringProperty(BuenoKey.KEY_NAME) == null) return false;

   String prj = property_set.getStringProperty(BuenoKey.KEY_PROJECT);
   if (prj == null && insertion_point != null) prj = insertion_point.getProject();
   String pkg = property_set.getStringProperty(BuenoKey.KEY_PACKAGE);
   if (pkg != null && pkg.startsWith("<")) pkg = null;
   if (pkg == null && insertion_point != null) pkg = insertion_point.getPackage();
   String nm = property_set.getStringProperty(BuenoKey.KEY_NAME);
   if (pkg != null) nm = pkg + "." + nm;
   BumpClient bc = BumpClient.getBump();
   List<BumpLocation> locs = bc.findClassDefinition(prj,nm);
   if (locs != null && locs.size() > 0) return false;

   return true;
}


private boolean checkInnerClassParsing()
{
   String sgn = property_set.getStringProperty(BuenoKey.KEY_SIGNATURE);
   if (sgn == null) sgn = getSignature();

   if (sgn != null) {
      try {
	 parseInnerClassSignature(sgn);
       }
      catch (BuenoException e) {
	 return false;
       }
    }

   if (property_set.getStringProperty(BuenoKey.KEY_NAME) == null) return false;

   return true;
}



private boolean checkMethodParsing()
{
   String sgn = property_set.getStringProperty(BuenoKey.KEY_SIGNATURE);
   if (sgn == null) sgn = getSignature();

   if (sgn != null) {
      try {
	 parseMethodSignature(sgn);
       }
      catch (BuenoException e) {
	 return false;
       }
    }

   if (property_set.getStringProperty(BuenoKey.KEY_NAME) == null) return false;

   return true;
}

private boolean checkFieldParsing()
{
   String sgn = property_set.getStringProperty(BuenoKey.KEY_SIGNATURE);
   if (sgn != null) {
      try {
	 parseFieldSignature(sgn);
       }
      catch (BuenoException e) {
	 return false;
       }
    }

   if (property_set.getStringProperty(BuenoKey.KEY_NAME) == null) return false;

   return true;
}


/********************************************************************************/
/*										*/
/*	Parse checking methods :: PYTHON					*/
/*										*/
/********************************************************************************/

private boolean checkPythonModuleParsing()
{
   String mod = property_set.getStringProperty(BuenoKey.KEY_NAME);
   if (mod == null || mod.length() == 0) return false;
   Matcher m = module_pattern.matcher(mod);
   if (!m.matches()) return false;

   return true;
}




private boolean checkPythonPackageParsing()
{
   String pkg = property_set.getStringProperty(BuenoKey.KEY_PACKAGE);

   if (pkg == null || pkg.length() == 0) return false;

   Matcher m = package_pattern.matcher(pkg);
   if (!m.matches()) return false;

   String mod = property_set.getStringProperty(BuenoKey.KEY_NAME);
   if (mod == null || mod.length() == 0) return false;
   m = module_pattern.matcher(mod);
   if (!m.matches()) return false;

   return true;
}




/********************************************************************************/
/*										*/
/*	Parsing methods for classes						*/
/*										*/
/********************************************************************************/

private void parseClassSignature(String txt) throws BuenoException
{
   StreamTokenizer tok = createStreamTokenizer(txt);

   parseModifiers(tok);

   if (checkNextToken(tok,"class")) {
      create_type = BuenoType.NEW_CLASS;
      property_set.put(BuenoKey.KEY_TYPE,"class");
    }
   else if (checkNextToken(tok,"enum")) {
      create_type = BuenoType.NEW_ENUM;
      property_set.put(BuenoKey.KEY_TYPE,"enum");
    }
   else if (checkNextToken(tok,"interface")) {
      property_set.put(BuenoKey.KEY_TYPE,"interface");
      create_type = BuenoType.NEW_INTERFACE;
    }
   else throw new BuenoException("No class/enum/interface keyword");

   parseName(tok);
   parseGenerics(tok);
   parseExtends(tok);
   parseImplements(tok);
   parseEnd(tok);
}


private void parseInnerClassSignature(String txt) throws BuenoException
{
   StreamTokenizer tok = createStreamTokenizer(txt);

   parseModifiers(tok);

   if (checkNextToken(tok,"class")) {
      create_type = BuenoType.NEW_INNER_CLASS;
    }
   else if (checkNextToken(tok,"enum")) {
      create_type = BuenoType.NEW_INNER_ENUM;
    }
   else if (checkNextToken(tok,"interface")) {
      create_type = BuenoType.NEW_INNER_INTERFACE;
    }
   else throw new BuenoException("No class/enum/interface keyword");

   parseName(tok);
   parseExtends(tok);
   parseImplements(tok);
   parseEnd(tok);
}



private void parseGenerics(StreamTokenizer tok) throws BuenoException
{
   if (!checkNextToken(tok,'<')) return;
   parseType(tok);
   if (!checkNextToken(tok,'>'))
      throw new BuenoException("Unclosed generic specification");
}



private void parseExtends(StreamTokenizer tok) throws BuenoException
{
   if (checkNextToken(tok,"extends")) {
      if (create_type == BuenoType.NEW_INTERFACE) {
	 List<String> rslt = new ArrayList<String>();
	 for ( ; ; ) {
	    String typ = parseType(tok);
	    rslt.add(typ);
	    if (!checkNextToken(tok,',')) break;
	  }
	 property_set.put(BuenoKey.KEY_EXTENDS,rslt);
       }
      else {
	 String typ = parseType(tok);
	 property_set.put(BuenoKey.KEY_EXTENDS,typ);
       }
    }
}



private void parseImplements(StreamTokenizer tok) throws BuenoException
{
   if (checkNextToken(tok,"implements")) {
      if (create_type == BuenoType.NEW_INTERFACE)
	 throw new BuenoException("Interfaces don't use implements");
      List<String> rslt = new ArrayList<String>();
      for ( ; ; ) {
	 String typ = parseType(tok);
	 rslt.add(typ);
	 if (!checkNextToken(tok,',')) break;
       }
      property_set.put(BuenoKey.KEY_IMPLEMENTS,rslt);
    }
}



/********************************************************************************/
/*										*/
/*	Method parsing methods							*/
/*										*/
/********************************************************************************/


private void parseMethodSignature(String txt) throws BuenoException
{
   StreamTokenizer tok = createStreamTokenizer(txt);

   parseModifiers(tok);
   parseReturnType(tok);

   if (checkNextToken(tok,'(')) {
      tok.pushBack();
      String rtyp = property_set.getStringProperty(BuenoKey.KEY_RETURNS);
      String cnm = insertion_point.getClassName();
      int idx1 = cnm.indexOf("<");
      if (idx1 >= 0) cnm = cnm.substring(0,idx1);
      int idx2 = cnm.lastIndexOf(".");
      if (idx2 >= 0) cnm = cnm.substring(idx2+1);
      if (cnm.equals(rtyp)) {
	 create_type = BuenoType.NEW_CONSTRUCTOR;
       }
      property_set.remove(BuenoKey.KEY_RETURNS);
      property_set.put(BuenoKey.KEY_NAME,rtyp);
    }
   else {
      parseName(tok);
    }

   parseArguments(tok);
   parseExceptions(tok);
   parseEnd(tok);
}




private void parseReturnType(StreamTokenizer tok) throws BuenoException
{
   String tnm = parseType(tok);

   property_set.put(BuenoKey.KEY_RETURNS,tnm);
}



private void parseArguments(StreamTokenizer stok) throws BuenoException
{
   if (!checkNextToken(stok,'(')) throw new BuenoException("Parameter list missing");
   List<String> parms = new ArrayList<String>();

   int anum = 1;
   for ( ; ; ) {
      if (checkNextToken(stok,')')) break;
      String typ = parseType(stok);
      String anm = "a" + anum;
      ++anum;
      if (checkNextToken(stok,',') || checkNextToken(stok,')')) {
	 stok.pushBack();
       }
      else if (nextToken(stok) == StreamTokenizer.TT_WORD) {
	 anm = stok.sval;
	 while (checkNextToken(stok,'[')) {
	    if (!checkNextToken(stok,']')) throw new BuenoException("Bad array parameter");
	    typ += "[]";
	  }
       }
      else throw new BuenoException("Expected agrument name");

      parms.add(typ + " " + anm);

      if (checkNextToken(stok,')')) break;
      else if (!checkNextToken(stok,',')) throw new BuenoException("Illegal argument name");
    }

   property_set.put(BuenoKey.KEY_PARAMETERS,parms);
}



private void parseExceptions(StreamTokenizer stok) throws BuenoException
{
   property_set.remove(BuenoKey.KEY_THROWS);

   if (!checkNextToken(stok,"throws")) return;

   List<String> rslt = new ArrayList<String>();

   for ( ; ; ) {
      String typ = parseType(stok);
      rslt.add(typ);
      if (!checkNextToken(stok,',')) break;
    }

   property_set.put(BuenoKey.KEY_THROWS,rslt);
}



/********************************************************************************/
/*										*/
/*	Field parsing methods							*/
/*										*/
/********************************************************************************/

private void parseFieldSignature(String txt) throws BuenoException
{
   StreamTokenizer tok = createStreamTokenizer(txt);

   parseModifiers(tok);
   parseFieldType(tok);
   parseName(tok);
   parseEnd(tok);
}




private void parseFieldType(StreamTokenizer tok) throws BuenoException
{
   String tnm = parseType(tok);

   property_set.put(BuenoKey.KEY_RETURNS,tnm);
}




/********************************************************************************/
/*										*/
/*	Signature parsing methods						*/
/*										*/
/********************************************************************************/

private void parseModifiers(StreamTokenizer stok)
{
   int mods = 0;

   for ( ; ; ) {
      if (nextToken(stok) != StreamTokenizer.TT_WORD) {
	 stok.pushBack();
	 break;
       }
      if (stok.sval.equals("public")) mods |= Modifier.PUBLIC;
      else if (stok.sval.equals("protected")) mods |= Modifier.PROTECTED;
      else if (stok.sval.equals("private")) mods |= Modifier.PRIVATE;
      else if (stok.sval.equals("static")) mods |= Modifier.STATIC;
      else if (stok.sval.equals("abstract")) mods |= Modifier.ABSTRACT;
      else if (stok.sval.equals("final")) mods |= Modifier.FINAL;
      else if (stok.sval.equals("native")) mods |= Modifier.NATIVE;
      else if (stok.sval.equals("synchronized")) mods |= Modifier.SYNCHRONIZED;
      else if (stok.sval.equals("transient")) mods |= Modifier.TRANSIENT;
      else if (stok.sval.equals("volatile")) mods |= Modifier.VOLATILE;
      else if (stok.sval.equals("strictfp")) mods |= Modifier.STRICT;
      else {
	 stok.pushBack();
	 break;
       }
    }

   property_set.put(BuenoKey.KEY_MODIFIERS,mods);
}




private void parseName(StreamTokenizer stok) throws BuenoException
{
   if (nextToken(stok) != StreamTokenizer.TT_WORD) {
      throw new BuenoException("Name missing");
    }

   property_set.put(BuenoKey.KEY_NAME,stok.sval);
}


private String parseType(StreamTokenizer stok) throws BuenoException
{
   String rslt = null;

   if (checkNextToken(stok,"byte") || checkNextToken(stok,"short") ||
	 checkNextToken(stok,"int") || checkNextToken(stok,"long") ||
	 checkNextToken(stok,"char") || checkNextToken(stok,"float") ||
	 checkNextToken(stok,"double") || checkNextToken(stok,"boolean") ||
	 checkNextToken(stok,"void")) {
      rslt = stok.sval;
    }
   else if (checkNextToken(stok,'?')) {
      rslt = "?";
      if (nextToken(stok) != StreamTokenizer.TT_WORD) {
	 stok.pushBack();
       }
      else if (checkNextToken(stok,"extends") || checkNextToken(stok,"super")) {
	 String ext = stok.sval;
	 String ntyp = parseType(stok);
	 rslt = rslt + " " + ext + " " + ntyp;
       }
      else {
	 stok.pushBack();
       }
    }
   else if (nextToken(stok) == StreamTokenizer.TT_WORD) {
      String tnam = stok.sval;
      for ( ; ; ) {
	 if (!checkNextToken(stok,'.')) break;
	 if (nextToken(stok) != StreamTokenizer.TT_WORD)
	    throw new BuenoException("Illegal qualified name");
	 tnam += "." + stok.sval;
       }
      rslt = tnam;
    }
   else throw new BuenoException("Type expected");

   if (checkNextToken(stok,'<')) {
      String ptyp = null;
      for ( ; ; ) {
	 String atyp = parseType(stok);
	 if (ptyp == null) ptyp = atyp;
	 else ptyp += "," + atyp;
	 if (checkNextToken(stok,'>')) break;
	 else if (!checkNextToken(stok,',')) throw new BuenoException("Bad parameterized argument");
       }
      if (ptyp == null) throw new BuenoException("Parameterized type list missing");
      rslt += "<" + ptyp + ">";
    }

   while (checkNextToken(stok,'[')) {
      if (!checkNextToken(stok,']')) throw new BuenoException("Missing right bracket");
      rslt += "[]";
    }

   return rslt;
}




private boolean checkNextToken(StreamTokenizer stok,String tok)
{
   if (nextToken(stok) == StreamTokenizer.TT_WORD && stok.sval.equals(tok)) return true;

   stok.pushBack();
   return false;
}




private boolean checkNextToken(StreamTokenizer stok,char tok)
{
   if (nextToken(stok) == tok) return true;

   stok.pushBack();
   return false;
}




private void parseEnd(StreamTokenizer stok) throws BuenoException
{
   if (nextToken(stok) != StreamTokenizer.TT_EOF) throw new BuenoException("Excess at end");
}




private int nextToken(StreamTokenizer stok)
{
   try {
      return stok.nextToken();
    }
   catch (IOException e) {
      return StreamTokenizer.TT_EOF;
    }
}



private StreamTokenizer createStreamTokenizer(String s)
{
   StreamTokenizer stok = new StreamTokenizer(new StringReader(s));
   stok.wordChars('_','_');
   return stok;
}




}	// end of class BuenoValidator




/* end of BuenoValidator.java */

