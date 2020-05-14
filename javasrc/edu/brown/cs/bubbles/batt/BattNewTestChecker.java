/********************************************************************************/
/*										*/
/*		BattNewTestChecker.java 					*/
/*										*/
/*	Bubble Automated Testing Tool class for checking new test specs 	*/
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


package edu.brown.cs.bubbles.batt;

import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.bubbles.org.objectweb.asm.Type;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;


class BattNewTestChecker implements BattConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BattNewTestChecker()
{
}




/********************************************************************************/
/*										*/
/*	Call test checking							*/
/*										*/
/********************************************************************************/

String checkCallTest(BumpLocation mthd,String args,String rslt)
{
   StreamTokenizer stok = getTokenizer(args);
   
   checkNextToken(stok,'(');
   
   String ktyp = mthd.getKey();
   int idx = ktyp.indexOf("(");
   ktyp = ktyp.substring(idx);
   Type [] atyps = Type.getArgumentTypes(ktyp);
   Type rtyp = Type.getReturnType(ktyp);
   
   for (int i = 0; i < atyps.length; ++i) {
      if (i > 0) checkNextToken(stok,',');
      Value v = parseTypedValue(stok,atyps[i]);
      if (v == null) return "Bad parameter " + i + " value";
    }
   if (atyps.length == 0) checkNextToken(stok,"void");
   checkNextToken(stok,')');
   if (!checkEnd(stok)) return "Parameter mismatch";
   
   stok = getTokenizer(rslt);
   Value r = parseTypedValue(stok,rtyp);
   if (rtyp != null && rtyp != Type.VOID_TYPE && r == null)
      return "Bad return value";
   if (!checkEnd(stok)) return "Return mismatch";
   
   return null;
}



String generateCallTestCode(List<BattCallTest> tests)
{
   return null;
}




/********************************************************************************/
/*										*/
/*	Parsing methods 							*/
/*										*/
/********************************************************************************/

private StreamTokenizer getTokenizer(String s)
{
   if (s == null) s = "";

   StreamTokenizer stok = new StreamTokenizer(new StringReader(s));

   stok.slashStarComments(true);
   stok.slashSlashComments(true);

   return stok;
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




private boolean checkEnd(StreamTokenizer stok)
{
   if (nextToken(stok) == StreamTokenizer.TT_EOF) return true;

   stok.pushBack();
   return false;
}







/********************************************************************************/
/*                                                                              */
/*      Value parsing                                                          */
/*                                                                              */
/********************************************************************************/

private Value parseTypedValue(StreamTokenizer stok,Type jt) 
{
   Value rslt = null;
   
   if (jt == null) return null;
   if (jt == Type.VOID_TYPE) {
      checkNextToken(stok,"void");
      // might want to return void value
      return null;
    }
   String typ = jt.getClassName();
   
   nextToken(stok);
   
   if (isNumericType(jt)) {
      if (stok.ttype == StreamTokenizer.TT_NUMBER) {
	 if (jt == Type.FLOAT_TYPE) rslt = new ValueLiteral(Double.toString(stok.nval) + "f");
	 else if (jt == Type.DOUBLE_TYPE) rslt = new ValueLiteral(Double.toString(stok.nval));
	 else if (jt == Type.BYTE_TYPE) {
	    byte cv = (byte) stok.nval;
	    rslt = new ValueLiteral("((byte) " + cv + ")");
	  }
	 else if (jt == Type.SHORT_TYPE) {
	    short cv = (short) stok.nval;
	    rslt = new ValueLiteral("((short) " + cv + ")");
	  }
	 else if (jt == Type.CHAR_TYPE) {
	    char cv = (char) stok.nval;
	    rslt = new ValueLiteral("((char) " + cv + ")");
	  }
	 else rslt = new ValueLiteral(Long.toString((long) stok.nval));
       }
      else if (stok.ttype == '\'') {
	 rslt = new ValueLiteral("'" + stok.sval + "'");
       }
      else {
	 return null;
       }
    }
   else if (jt == Type.BOOLEAN_TYPE) {
      boolean val = false;
      if (stok.ttype == StreamTokenizer.TT_WORD) {
	 if (stok.sval.startsWith("t") || stok.sval.startsWith("T")) val = true;
       }
      else return null;
      rslt = new ValueLiteral(Boolean.toString(val));
    }
   else if (typ.equals("java/lang/String") ||
         (typ.equals("java/lang/Object") && stok.ttype == '"')) {
      if (stok.ttype == '"' || stok.ttype == '\'' || stok.ttype == StreamTokenizer.TT_WORD) {
	 rslt = new ValueString(stok.sval);
       }
      else return null;
    }
   else if (stok.ttype == '[' && jt.getSort() == Type.ARRAY) {
      Type bjt = jt.getElementType();
      List<Value> vals = new ArrayList<Value>();
      while (!checkNextToken(stok,']')) {
         Value sv = parseTypedValue(stok,bjt);
         if (sv == null) return null;
	 vals.add(sv);
	 checkNextToken(stok,',');
	 if (stok.ttype == StreamTokenizer.TT_EOF) return null;
       }
      StringBuffer cbuf = new StringBuffer();
      for (Value v : vals) {
	 if (v.getCode() != null) {
	    cbuf.append(v.getCode());
	    cbuf.append(";\n");
	  }
       }
      if (jt.getSort() == Type.ARRAY) {
	 StringBuffer lbuf = new StringBuffer();
	 lbuf.append("new " + bjt.getInternalName().replace("/",".") + "[] {");
	 int ct = 0;
	 for (Value v : vals) {
	    if (ct++ != 0) lbuf.append(" , ");
	    lbuf.append(v.getValue());
	  }
	 lbuf.append("}");
	 rslt = new ValueLiteral(lbuf.toString(),cbuf.toString());
       }
    }
   else {
      StringBuffer buf = new StringBuffer();
      String var = null;
      if (stok.ttype == StreamTokenizer.TT_WORD) {
	 var = stok.sval;
	 nextToken(stok);
	 if (stok.ttype != '{') {
	    buf.append(var);
	    buf.append(" ");
	    var = null;
	  }
       }
      int lvl = 0;
      for ( ; ; ) {
	 int ttyp = stok.ttype;
	 if (ttyp == '{' || ttyp == '(') {
	    ++lvl;
	    buf.append((char) ttyp);
	  }
	 else if (ttyp == '}' || ttyp == ')') {
	    --lvl;
	    buf.append((char) ttyp);
	    if (lvl == 0) break;
	  }
	 else if (ttyp == '"' || ttyp == '\'') {
	    buf.append((char) ttyp);
	    buf.append(stok.sval);
	    buf.append((char) ttyp);
	  }
	 else if (ttyp == ',' && lvl == 0) {
            stok.pushBack();
            break;
	  }
	 else if (ttyp == StreamTokenizer.TT_EOF) {
	    stok.pushBack();
	    break;
	  }
	 else if (ttyp == StreamTokenizer.TT_WORD) {
	    buf.append(" ");
	    buf.append(stok.sval);
	    buf.append(" ");
	  }
	 else if (ttyp == StreamTokenizer.TT_NUMBER) {
	    Double db = Double.valueOf(stok.nval);
	    if (db.doubleValue() == db.longValue()) {
	       buf.append(db.longValue());
	     }
	    else buf.append(db.doubleValue());
	  }
	 else {
	    buf.append((char) ttyp);
	  }
	 nextToken(stok);
       }
      String s = buf.toString().trim();
      if (s.startsWith("{")) s = s.substring(1,s.length()-1);
      if (var == null) {
	 rslt = new ValueLiteral(s);
       }
      else return null;
    }
   
   return rslt;
}


private boolean isNumericType(Type t)
{
   switch (t.getSort()) {
      case Type.BYTE :
      case Type.CHAR :
      case Type.DOUBLE :
      case Type.FLOAT :
      case Type.INT :
      case Type.LONG :
      case Type.SHORT :
         return true;
    }
   
   return false;
}




/********************************************************************************/
/*                                                                              */
/*      Value Representations                                                   */
/*                                                                              */
/********************************************************************************/

private abstract static class Value {
   
   String getCode()			{ return null; }
   abstract String getValue();
   
}	// end of subclass Value




private static class ValueLiteral extends Value {
   
   private String literal_value;
   private String code_value;
   
   ValueLiteral(String s) {
      literal_value = s;
      code_value = null;
    }
   
   ValueLiteral(String s,String c) {
      literal_value = s;
      if (c != null && c.trim().length() == 0) c = null;
      code_value = c;
    }
   
   @Override String getCode()			{ return code_value; }
   @Override String getValue()			{ return literal_value; }
   
}	// end of subclass ValueLiteral



private static class ValueString extends Value {
   
   private String string_value;
   
   ValueString(String s) {
      string_value = s;
    }
   
   @Override String getValue() {
      // handle special characters in the string
      return "\"" + string_value + "\"";
    }
   
}	// end of subclass ValueString



}       // end of class BattNewTestChecker




/* end of BattNewTestChecker.java */





