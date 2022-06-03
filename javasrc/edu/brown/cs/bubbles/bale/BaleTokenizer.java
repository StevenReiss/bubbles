/********************************************************************************/
/*										*/
/*		BaleTokenizer.java						*/
/*										*/
/*	Bubble Annotated Language Editor text tokenizer 			*/
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


package edu.brown.cs.bubbles.bale;



import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


abstract class BaleTokenizer implements BaleConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private String		input_text;
private int		cur_offset;
private int		end_offset;
private BaleTokenState	token_state;
private int		token_start;
private boolean 	ignore_white;
private char            cur_delim;
private int             delim_count;

private static Map<String,BaleTokenType> java_keyword_map;
private static Set<String>	java_op_set;
private static Map<String,BaleTokenType> python_keyword_map;
private static Set<String>	python_op_set;
private static Map<String,BaleTokenType> js_keyword_map;
private static Set<String>	js_op_set;

private static final String OP_CHARS = "=<!~?:>|&+-*/^%\\";




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

static BaleTokenizer create(String text,BaleTokenState start,BoardLanguage bl)
{
   switch (bl) {
      default :
      case REBUS :
      case JAVA :
      case JAVA_IDEA :
	 return new JavaTokenizer(text,start);
      case PYTHON :
	 return new PythonTokenizer(text,start);
      case JS :
	 return new JSTokenizer(text,start);
    }
}



private BaleTokenizer(String text,BaleTokenState start)
{
   input_text = (text == null ? "" : text);
   cur_offset = 0;
   end_offset = (text == null ? 0 : input_text.length());
   token_state = start;
   ignore_white = false;
   cur_delim = 0;
   delim_count = 0;
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void setIgnoreWhitespace(boolean fg)		{ ignore_white = fg; }

abstract protected BaleTokenType getKeyword(String s);
abstract protected boolean isOperator(String s);
protected boolean useSlashStarComments()                { return true; }
protected boolean useSlashSlashComments()               { return true; }
protected boolean useHashComments()                     { return false; }
protected int useMultilineCount()                       { return 0; }
protected boolean isStringStart(char c)                 { return c == '"'; }
protected boolean isMultilineStringStart(char c)        { return false; }

static Collection<String> getKeywords(BoardLanguage bl)
{
   switch (bl) {
      default :
      case JAVA :
      case JAVA_IDEA :
      case REBUS :
	 return java_keyword_map.keySet();
      case PYTHON :
	 return python_keyword_map.keySet();
      case JS :
	 return js_keyword_map.keySet();
    }
}



/********************************************************************************/
/*										*/
/*	Scanning methods							*/
/*										*/
/********************************************************************************/

List<BaleToken> scan()
{
   List<BaleToken> rslt = new ArrayList<BaleToken>();

   while (cur_offset < end_offset) {
      rslt.add(getNextToken());
    }

   return rslt;
}



/********************************************************************************/
/*										*/
/*	Tokenizing methods							*/
/*										*/
/********************************************************************************/

BaleToken getNextToken(int start,int end)
{
   return getNextToken(start,end,BaleTokenState.NORMAL);
}



BaleToken getNextToken(int start,int end,BaleTokenState st)
{
   int poff = cur_offset;
   int peoff = end_offset;
   BaleTokenState pst = token_state;
   boolean pwh = ignore_white;

   cur_offset = start;
   end_offset = end;
   token_state = st;
   ignore_white = true;

   BaleToken t = getNextToken();

   cur_offset = poff;
   end_offset = peoff;
   token_state = pst;
   ignore_white = pwh;

   return t;
}




/********************************************************************************/
/*										*/
/*	Next token methods							*/
/*										*/
/********************************************************************************/

BaleToken getNextToken()
{
   token_start = cur_offset;

   switch (token_state) {
      case NORMAL :
	 break;
      case IN_LINE_COMMENT :
	 return scanLineComment();		// added by amc6
      case IN_MULTILINE_STRING :
	 return scanMultiLineString(cur_delim,delim_count);
      case IN_FORMAL_COMMENT :
	 return scanComment(true);
      case IN_COMMENT :
	 return scanComment(false);
      case IN_STRING :
	 return scanString(cur_delim);
    }

   //TODO: keep a flag to identify unary operators
   //TODO: instanceof should be separate

   char ch = nextChar();
   if (ignore_white) {
      while (Character.isWhitespace(ch)) {
	 token_start = cur_offset;
	 ch = nextChar();
       }
    }

   if (ch == '\r') {
      ch = nextChar();
      if (ch != '\n') backup();
      return buildToken(BaleTokenType.EOL);
    }
   else if (ch == '\n') return buildToken(BaleTokenType.EOL);
   else if (Character.isWhitespace(ch)) {
      for ( ; ; ) {
	 ch = nextChar();
	 if (ch < 0 || ch == 0xffff || ch == '\n' || ch == '\r' || !Character.isWhitespace(ch))
	    break;
       }
      backup();
      return buildToken(BaleTokenType.SPACE);
    }
   else if (Character.isJavaIdentifierStart(ch)) {
      for ( ; ; ) {
	 ch = nextChar();
	 if (ch < 0 || ch == 0xffff || !Character.isJavaIdentifierPart(ch)) break;
       }
      backup();
      String text = getInputString(token_start,cur_offset);
      BaleTokenType tt = getKeyword(text);
      if (tt != null) return buildToken(tt);
      else return buildToken(BaleTokenType.IDENTIFIER);
    }
   else if (Character.isDigit(ch)) {
      if (ch == '0') {
	 ch = nextChar();
	 if (ch == 'x' || ch == 'X') {
	    int ct = 0;
	    for ( ; ; ) {
	       ch = nextChar();
	       if (ch < 0 || ch == 0xffff || Character.digit(ch,16) < 0) break;
	       ++ct;
	     }
	    backup();
	    if (ct == 0) return buildToken(BaleTokenType.BADNUMBER);
	    return buildToken(BaleTokenType.NUMBER);
	  }
	 backup();
       }
      for ( ; ; ) {
	 ch = nextChar();
	 if (!Character.isDigit(ch)) break;
       }
      if (ch == 'l' || ch == 'L' || ch == 'f' || ch == 'F' || ch == 'd' || ch == 'D') {
	 return buildToken(BaleTokenType.NUMBER);
       }
      else if (ch != '.' && ch != 'e' || ch != 'E') {
	 backup();
	 return buildToken(BaleTokenType.NUMBER);
       }
      else {
	 int ct = 1;
	 if (ch == '.') {
	    for ( ; ; ) {
	       ch = nextChar();
	       if (ch < 0 || ch == 0xffff || !Character.isDigit(ch)) break;
	     }
	  }
	 if (ch == 'e' || ch == 'E') {
	    ch = nextChar();
	    if (ch == '+' || ch == '-') ch = nextChar();
	    ct = 0;
	    for ( ; ; ) {
	       ch = nextChar();
	       if (ch < 0 || ch == 0xffff || !Character.isDigit(ch)) break;
	       ++ct;
	     }
	  }
	 if (ch != 'f' && ch != 'F' && ch != 'd' && ch != 'D') backup();
	 if (ct == 0) return buildToken(BaleTokenType.BADNUMBER);
	 return buildToken(BaleTokenType.NUMBER);
       }
    }
   else if (isStringStart(ch)) {
      int ct = useMultilineCount();
      if (ct > 0) {
         int bup = 0;
         for (int i = 0; i < ct; ++i) {
            char ch1 = nextChar();
            bup++;
            if (ch1 != ch) break;
            if (i == ct-1) {
               return scanMultiLineString(ch,ct);
             }
          }
         for (int i = 0; i < bup; ++i) backup();
       }
      return scanString(ch);
    }
   else if (isMultilineStringStart(ch)) {
      return scanMultiLineString(ch,1);
    }
   else if (ch == '\'') {
      for ( ; ; ) {
	 ch = nextChar();
	 if (ch == '\'') return buildToken(BaleTokenType.CHARLITERAL);
	 else if (ch == '\n' || ch == '\r' || ch == 0xffff) {
	    backup();
	    return buildToken(BaleTokenType.BADCHARLIT);
	  }
	 else if (ch == '\\') {
	    ch = nextChar();
	    if (ch == '\n' || ch == '\r' || ch == 0xffff) {
	       backup();
	       return buildToken(BaleTokenType.BADCHARLIT);
	     }
	  }
       }
    }
   else if (ch == '#' && useHashComments()) {
      return scanLineComment();
    }
   else if (ch == '/') {
      ch = nextChar();
      if (ch == '/') {
	  return scanLineComment(); //added by amc6
       }
      else if (ch == '*' && useSlashStarComments()) {
	 boolean formal = false;
	 ch = nextChar();
	 if (ch == '*') {
	    ch = nextChar();
	    if (ch == '/') {
	       backup();
	     }
	    else formal = true;
	    backup();
	  }
	 else backup();
	 return scanComment(formal);
       }
      else {
	 if (ch != '=') backup();
	 return buildToken(BaleTokenType.OP);		     // / or /=
       }

    }
   else if (ch == '{') return buildToken(BaleTokenType.LBRACE);
   else if (ch == '}') return buildToken(BaleTokenType.RBRACE);
   else if (ch == '(') return buildToken(BaleTokenType.LPAREN);
   else if (ch == ')') return buildToken(BaleTokenType.RPAREN);
   else if (ch == '[') return buildToken(BaleTokenType.LBRACKET);
   else if (ch == ']') return buildToken(BaleTokenType.RBRACKET);
   else if (ch == ';') return buildToken(BaleTokenType.SEMICOLON);
   else if (ch == ',') return buildToken(BaleTokenType.COMMA);
   else if (ch == '\\') return buildToken(BaleTokenType.BACKSLASH);
   else if (ch == '.') {
      if (nextChar() == '.') {
	 if (nextChar() == '.') return buildToken(BaleTokenType.OP);
	 else backup();
       }
      else backup();
      return buildToken(BaleTokenType.DOT);
    }
   else if (ch == '@') return buildToken(BaleTokenType.AT);
   else if (ch == '?') return buildToken(BaleTokenType.QUESTIONMARK);
   else if (OP_CHARS.indexOf(ch) >= 0) {
      boolean eql = (ch == '=');
      int nch = 1;
      char fch = ch;
      for ( ; ; ) {
	 ch = nextChar();
	 if (ch < 0 || ch == 0xffff || OP_CHARS.indexOf(ch) < 0) break;
	 String op = getInputString(token_start,cur_offset);
	 if (!isOperator(op)) break;
	 eql = (ch == '=');
         ++nch;
       }
      backup();
      if (eql) return buildToken(BaleTokenType.EQUAL);
      if (nch == 1) {
         if (fch == ':') return buildToken(BaleTokenType.COLON);
         if (fch == '<') return buildToken(BaleTokenType.LANGLE);
         if (fch == '>') return buildToken(BaleTokenType.RANGLE);
       }
      return buildToken(BaleTokenType.OP);
    }
   else if (ch == -1 || ch == 0xffff) return buildToken(BaleTokenType.EOF);

   return buildToken(BaleTokenType.OTHER);
}


private Token scanLineComment()    //added by amc6
{
   token_state = BaleTokenState.IN_LINE_COMMENT;

   if (BALE_PROPERTIES.getBoolean(COMMENT_WRAPPING)) {
      for ( ; ; ) {
	 char ch = nextChar();
	 if (ch < 0 || ch == '\n' || ch == '\r' || ch == 0xffff) {
	    backup();
	    token_state = BaleTokenState.NORMAL;
	    return buildToken(BaleTokenType.LINECOMMENT);
	  }
	 else if (Character.isWhitespace(ch)) {
	    for ( ; ; ) {
	       ch = nextChar();
	       if (ch < 0 || ch == 0xffff || ch == '\n' || ch == '\r' || !Character.isWhitespace(ch)) break;
	     }
	    backup();
	    return buildToken(BaleTokenType.LINECOMMENT);
	  }
	 else {
	    for ( ; ; ) {
	       ch = nextChar();
	       if (ch < 0 || ch == '\n' || ch == 0xffff || ch == '\r' || Character.isWhitespace(ch)) {
		  backup();
		  return buildToken(BaleTokenType.LINECOMMENT);
		}
	     }
	  }
       }
    }
   else {
      for ( ; ; ) {
	 char ch = nextChar();
	 if (ch < 0 || ch == '\n' || ch == '\r' || ch == 0xffff) break;
       }
      backup();
      token_state = BaleTokenState.NORMAL;
      return buildToken(BaleTokenType.LINECOMMENT);
    }
}


private Token scanComment(boolean formalstart)
{
   boolean havestar = false;
   boolean formal = false;
   if (token_state == BaleTokenState.IN_FORMAL_COMMENT) formal = true;

   for ( ; ; ) {
      char ch = nextChar();
      if (havestar && ch == '/') {
	 token_state = BaleTokenState.NORMAL;
	 if (formal) return buildToken(BaleTokenType.ENDFORMALCOMMENT);
	 else return buildToken(BaleTokenType.ENDCOMMENT);
       }
      else if (ch < 0 || ch == '\n' || ch == 0xffff) {
	 if (formalstart) token_state = BaleTokenState.IN_FORMAL_COMMENT;
	 else token_state = BaleTokenState.IN_COMMENT;
	 if (formalstart) return buildToken(BaleTokenType.EOLFORMALCOMMENT);
	 else return buildToken(BaleTokenType.EOLCOMMENT);
       }
      else if (ch == '\r') {
	 ch = nextChar();
	 if (ch != '\n') backup();
	 if (formalstart) token_state = BaleTokenState.IN_FORMAL_COMMENT;
	 else token_state = BaleTokenState.IN_COMMENT;
	 if (formal) return buildToken(BaleTokenType.EOLFORMALCOMMENT);
	 else return buildToken(BaleTokenType.EOLCOMMENT);
       }
      else if (ch == '*') havestar = true;
      else {  //added by amc6
	 havestar = false;
	 if (formalstart) formal = true;
	 if (BALE_PROPERTIES.getBoolean(COMMENT_WRAPPING)) {
	    if (formalstart) token_state = BaleTokenState.IN_FORMAL_COMMENT;
	    else token_state = BaleTokenState.IN_COMMENT;

	    if (Character.isWhitespace(ch)) {
	       for ( ; ; ) {
		  ch = nextChar();
		  if (ch < 0 || ch == 0xffff || ch == '\n' || ch == '\r' || !Character.isWhitespace(ch)) break;
		}
	       backup();
	       if (formal) return buildToken(BaleTokenType.ENDFORMALCOMMENT);
	       else return buildToken(BaleTokenType.ENDCOMMENT);
	     }
	  }
       }
    }
}



private Token scanString(char delim)
{
   int havetext = 0;
   cur_delim = delim;
   delim_count = 1;

   for ( ; ; ) {
      char ch = nextChar();
      if (ch == delim) {
	 token_state = BaleTokenState.NORMAL;
	 return buildToken(BaleTokenType.STRING);
      }
      else if (ch == '\n' || ch == '\r' || ch == 0xffff) {
	 backup();
	 token_state = BaleTokenState.NORMAL;
	 return buildToken(BaleTokenType.BADSTRING);
       }
      else if (ch == '\\') {
	 ch = nextChar();
	 if (ch == '\n' || ch == '\r' || ch == 0xffff) {
	    backup();
	    token_state = BaleTokenState.NORMAL;
	    return buildToken(BaleTokenType.BADSTRING);
	  }
       }
      else if (BALE_PROPERTIES.getBoolean(STRING_WRAPPING)) {
	 if (Character.isWhitespace(ch)) {
	    if (havetext > 0) {
	       backup();
	       token_state = BaleTokenState.IN_STRING;
	       return buildToken(BaleTokenType.STRING);
	     }
	  }
	 else ++havetext;
       }
    }
}



private Token scanMultiLineString(char delim,int ct)
{
   int fnd = 0;
   cur_delim = delim;
   delim_count = ct;

   for ( ; ; ) {
      char ch = nextChar();
      if (ch == cur_delim) {
	 token_state = BaleTokenState.NORMAL;
	 if (++fnd == delim_count) return buildToken(BaleTokenType.LONGSTRING);
       }
      else fnd = 0;
      if (ch < 0 || ch == 0xffff) {
	 token_state = BaleTokenState.NORMAL;
	 return buildToken(BaleTokenType.BADSTRING);
       }
      else if (ch == '\n') {
	 token_state = BaleTokenState.IN_MULTILINE_STRING;
	 return buildToken(BaleTokenType.LONGSTRING);
       }
      else if (ch == '\r') {
	 ch = nextChar();
	 if (ch != '\n') backup();
	 token_state = BaleTokenState.IN_MULTILINE_STRING;
	 return buildToken(BaleTokenType.LONGSTRING);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Character methods							*/
/*										*/
/********************************************************************************/

private char nextChar()
{
   if (cur_offset >= end_offset) {
      ++cur_offset;		// still need to do this to handle backup
      return (char) -1;
    }

   return input_text.charAt(cur_offset++);
}



private void backup()
{
   cur_offset--;
}



private String getInputString(int start,int end)
{
   return input_text.substring(start,end);
}



/********************************************************************************/
/*										*/
/*	Token building methods							*/
/*										*/
/********************************************************************************/

private Token buildToken(BaleTokenType t)
{
   return new Token(t,token_start,cur_offset);
}



/********************************************************************************/
/*										*/
/*	Token subclass								*/
/*										*/
/********************************************************************************/

private static class Token implements BaleToken {

   private BaleTokenType token_type;
   private int start_offset;
   private int token_length;

   Token(BaleTokenType tt,int soff,int coff) {
      token_type = tt;
      start_offset = soff;
      token_length = coff - soff;
    }

   @Override public BaleTokenType getType()		{ return token_type; }
   @Override public int getStartOffset()		{ return start_offset; }
   @Override public int getLength()			{ return token_length; }

}	// end of inner class Token




/********************************************************************************/
/*										*/
/*	Java tokenizer								*/
/*										*/
/********************************************************************************/

private static class JavaTokenizer extends BaleTokenizer {

   JavaTokenizer(String text,BaleTokenState start) {
      super(text,start);
    }

   @Override protected BaleTokenType getKeyword(String s) { return java_keyword_map.get(s); }
   @Override protected boolean isOperator(String s)	{ return java_op_set.contains(s); }
   @Override protected int useMultilineCount()          { return 3; }

}	// end of inner class JavaTokenizer


static {
   java_keyword_map = new HashMap<String,BaleTokenType>();
   java_keyword_map.put("abstract",BaleTokenType.KEYWORD);
   java_keyword_map.put("assert",BaleTokenType.KEYWORD);
   java_keyword_map.put("boolean",BaleTokenType.TYPEKEY);
   java_keyword_map.put("break",BaleTokenType.BREAK);
   java_keyword_map.put("byte",BaleTokenType.TYPEKEY);
   java_keyword_map.put("case",BaleTokenType.CASE);
   java_keyword_map.put("catch",BaleTokenType.CATCH);
   java_keyword_map.put("char",BaleTokenType.TYPEKEY);
   java_keyword_map.put("class",BaleTokenType.CLASS);
   java_keyword_map.put("const",BaleTokenType.KEYWORD);
   java_keyword_map.put("continue",BaleTokenType.KEYWORD);
   java_keyword_map.put("default",BaleTokenType.DEFAULT);
   java_keyword_map.put("do",BaleTokenType.DO);
   java_keyword_map.put("double",BaleTokenType.TYPEKEY);
   java_keyword_map.put("else",BaleTokenType.ELSE);
   java_keyword_map.put("enum",BaleTokenType.ENUM);
   java_keyword_map.put("extends",BaleTokenType.KEYWORD);
   java_keyword_map.put("false",BaleTokenType.KEYWORD);
   java_keyword_map.put("final",BaleTokenType.KEYWORD);
   java_keyword_map.put("finally",BaleTokenType.FINALLY);
   java_keyword_map.put("float",BaleTokenType.TYPEKEY);
   java_keyword_map.put("for",BaleTokenType.FOR);
   java_keyword_map.put("goto",BaleTokenType.GOTO);
   java_keyword_map.put("if",BaleTokenType.IF);
   java_keyword_map.put("implements",BaleTokenType.KEYWORD);
   java_keyword_map.put("import",BaleTokenType.IMPORT);
   java_keyword_map.put("instanceof",BaleTokenType.KEYWORD);
   java_keyword_map.put("int",BaleTokenType.TYPEKEY);
   java_keyword_map.put("interface",BaleTokenType.INTERFACE);
   java_keyword_map.put("long",BaleTokenType.TYPEKEY);
   java_keyword_map.put("native",BaleTokenType.KEYWORD);
   java_keyword_map.put("new",BaleTokenType.NEW);
   java_keyword_map.put("null",BaleTokenType.KEYWORD);
   java_keyword_map.put("package",BaleTokenType.PACKAGE);
   java_keyword_map.put("private",BaleTokenType.KEYWORD);
   java_keyword_map.put("protected",BaleTokenType.KEYWORD);
   java_keyword_map.put("public",BaleTokenType.KEYWORD);
   java_keyword_map.put("return",BaleTokenType.RETURN);
   java_keyword_map.put("short",BaleTokenType.TYPEKEY);
   java_keyword_map.put("static",BaleTokenType.STATIC);
   java_keyword_map.put("strictfp",BaleTokenType.KEYWORD);
   java_keyword_map.put("super",BaleTokenType.KEYWORD);
   java_keyword_map.put("switch",BaleTokenType.SWITCH);
   java_keyword_map.put("synchronized",BaleTokenType.SYNCHRONIZED);
   java_keyword_map.put("this",BaleTokenType.KEYWORD);
   java_keyword_map.put("throw",BaleTokenType.KEYWORD);
   java_keyword_map.put("throws",BaleTokenType.KEYWORD);
   java_keyword_map.put("transient",BaleTokenType.KEYWORD);
   java_keyword_map.put("true",BaleTokenType.KEYWORD);
   java_keyword_map.put("try",BaleTokenType.TRY);
   java_keyword_map.put("void",BaleTokenType.TYPEKEY);
   java_keyword_map.put("volatile",BaleTokenType.KEYWORD);
   java_keyword_map.put("while",BaleTokenType.WHILE);

   java_op_set = new HashSet<String>();
   java_op_set.add("=");
   java_op_set.add("<");
   java_op_set.add("!");
   java_op_set.add("~");
   java_op_set.add("?");
   java_op_set.add(":");
   java_op_set.add("==");
   java_op_set.add("<=");
   java_op_set.add(">=");
   java_op_set.add("!=");
   java_op_set.add("||");
   java_op_set.add("&&");
   java_op_set.add("++");
   java_op_set.add("--");
   java_op_set.add("+");
   java_op_set.add("-");
   java_op_set.add("*");
   java_op_set.add("/");
   java_op_set.add("&");
   java_op_set.add("|");
   java_op_set.add("^");
   java_op_set.add("%");
   java_op_set.add("<<");
   java_op_set.add("+=");
   java_op_set.add("-=");
   java_op_set.add("*=");
   java_op_set.add("/=");
   java_op_set.add("&=");
   java_op_set.add("|=");
   java_op_set.add("^=");
   java_op_set.add("%=");
   java_op_set.add("<<=");
   java_op_set.add(">>=");
   java_op_set.add(">>>=");
   java_op_set.add(">>");
   java_op_set.add(">>>");
   java_op_set.add(">");
   java_op_set.add("::");
   java_op_set.add("->");
}




/********************************************************************************/
/*										*/
/*	Python tokenizer							*/
/*										*/
/********************************************************************************/

private static class PythonTokenizer extends BaleTokenizer {

   PythonTokenizer(String text,BaleTokenState start) {
      super(text,start);
    }

   @Override protected BaleTokenType getKeyword(String s) { return python_keyword_map.get(s); }
   @Override protected boolean isOperator(String s)	{ return python_op_set.contains(s); }
   @Override protected boolean useSlashSlashComments()	{ return false; }
   @Override protected boolean useSlashStarComments()	{ return false; }
   @Override protected boolean useHashComments()	{ return true; }
   @Override protected int useMultilineCount()          { return 3; }
   @Override protected boolean isStringStart(char c) {
      return c == '"' || c == '\'';
    }
   
}	// end of inner class PythonTokenizer



static {
   python_keyword_map = new HashMap<String,BaleTokenType>();
   python_keyword_map.put("and",BaleTokenType.KEYWORD);
   python_keyword_map.put("as",BaleTokenType.KEYWORD);
   python_keyword_map.put("assert",BaleTokenType.KEYWORD);
   python_keyword_map.put("break",BaleTokenType.BREAK);
   python_keyword_map.put("class",BaleTokenType.CLASS);
   python_keyword_map.put("continue",BaleTokenType.CONTINUE);
   python_keyword_map.put("def",BaleTokenType.KEYWORD);
   python_keyword_map.put("del",BaleTokenType.KEYWORD);
   python_keyword_map.put("elif",BaleTokenType.ELSE);
   python_keyword_map.put("else",BaleTokenType.ELSE);
   python_keyword_map.put("except",BaleTokenType.KEYWORD);
   python_keyword_map.put("False",BaleTokenType.KEYWORD);
   python_keyword_map.put("finally",BaleTokenType.FINALLY);
   python_keyword_map.put("for",BaleTokenType.FOR);
   python_keyword_map.put("from",BaleTokenType.KEYWORD);
   python_keyword_map.put("global",BaleTokenType.KEYWORD);
   python_keyword_map.put("if",BaleTokenType.IF);
   python_keyword_map.put("import",BaleTokenType.KEYWORD);
   python_keyword_map.put("in",BaleTokenType.KEYWORD);
   python_keyword_map.put("is",BaleTokenType.KEYWORD);
   python_keyword_map.put("lambda",BaleTokenType.KEYWORD);
   python_keyword_map.put("None",BaleTokenType.KEYWORD);
   python_keyword_map.put("nonlocal",BaleTokenType.KEYWORD);
   python_keyword_map.put("not",BaleTokenType.KEYWORD);
   python_keyword_map.put("or",BaleTokenType.KEYWORD);
   python_keyword_map.put("pass",BaleTokenType.PASS);
   python_keyword_map.put("raise",BaleTokenType.RAISE);
   python_keyword_map.put("return",BaleTokenType.RETURN);
   python_keyword_map.put("True",BaleTokenType.KEYWORD);
   python_keyword_map.put("try",BaleTokenType.TRY);
   python_keyword_map.put("while",BaleTokenType.WHILE);
   python_keyword_map.put("with",BaleTokenType.KEYWORD);
   python_keyword_map.put("yield",BaleTokenType.KEYWORD);

   python_op_set = new HashSet<String>();
   python_op_set.add("=");
   python_op_set.add("<");
   python_op_set.add("~");
   python_op_set.add(":");
   python_op_set.add("==");
   python_op_set.add("<=");
   python_op_set.add(">=");
   python_op_set.add("!=");
   python_op_set.add("+");
   python_op_set.add("-");
   python_op_set.add("*");
   python_op_set.add("/");
   python_op_set.add("&");
   python_op_set.add("|");
   python_op_set.add("^");
   python_op_set.add("%");
   python_op_set.add("<<");
   python_op_set.add("+=");
   python_op_set.add("-=");
   python_op_set.add("*=");
   python_op_set.add("/=");
   python_op_set.add("&=");
   python_op_set.add("|=");
   python_op_set.add("^=");
   python_op_set.add("%=");
   python_op_set.add("<<=");
   python_op_set.add(">>=");
   python_op_set.add(">>>=");
   python_op_set.add(">>");
   python_op_set.add(">");
   python_op_set.add("//");
   python_op_set.add("**");
   python_op_set.add("->");
   python_op_set.add("//=");
   python_op_set.add("**=");
   python_op_set.add("\\");
}



/********************************************************************************/
/*										*/
/*	JavaScript tokenizer							*/
/*										*/
/********************************************************************************/

private static class JSTokenizer extends BaleTokenizer {

   JSTokenizer(String text,BaleTokenState start) {
      super(text,start);
    }

   @Override protected BaleTokenType getKeyword(String s) { return js_keyword_map.get(s); }
   @Override protected boolean isOperator(String s)	{ return js_op_set.contains(s); }
   @Override protected boolean isStringStart(char c) {
      return c == '"' || c == '\'';
    }
   @Override protected boolean isMultilineStringStart(char c) {
      return c == '`';
    }

}	// end of inner class JSTokenizer


static {
   js_keyword_map = new HashMap<String,BaleTokenType>();
   js_keyword_map.put("break",BaleTokenType.BREAK);
   js_keyword_map.put("case",BaleTokenType.CASE);
   js_keyword_map.put("catch",BaleTokenType.CATCH);
   js_keyword_map.put("class",BaleTokenType.CLASS);
   js_keyword_map.put("const",BaleTokenType.KEYWORD);
   js_keyword_map.put("continue",BaleTokenType.KEYWORD);
   js_keyword_map.put("debugger",BaleTokenType.KEYWORD);
   js_keyword_map.put("default",BaleTokenType.DEFAULT);
   js_keyword_map.put("delete",BaleTokenType.KEYWORD);
   js_keyword_map.put("do",BaleTokenType.DO);
   js_keyword_map.put("else",BaleTokenType.ELSE);
   js_keyword_map.put("enum",BaleTokenType.ENUM);
   js_keyword_map.put("export",BaleTokenType.KEYWORD);
   js_keyword_map.put("extends",BaleTokenType.KEYWORD);
   js_keyword_map.put("false",BaleTokenType.KEYWORD);
   js_keyword_map.put("finally",BaleTokenType.FINALLY);
   js_keyword_map.put("for",BaleTokenType.FOR);
   js_keyword_map.put("function",BaleTokenType.FUNCTION);
   js_keyword_map.put("if",BaleTokenType.IF);
   js_keyword_map.put("in",BaleTokenType.KEYWORD);
   js_keyword_map.put("implements",BaleTokenType.KEYWORD);
   js_keyword_map.put("import",BaleTokenType.KEYWORD);
   js_keyword_map.put("instanceof",BaleTokenType.KEYWORD);
   js_keyword_map.put("interface",BaleTokenType.INTERFACE);
   js_keyword_map.put("let",BaleTokenType.KEYWORD);
   js_keyword_map.put("new",BaleTokenType.NEW);
   js_keyword_map.put("null",BaleTokenType.KEYWORD);
   js_keyword_map.put("package",BaleTokenType.KEYWORD);
   js_keyword_map.put("private",BaleTokenType.KEYWORD);
   js_keyword_map.put("protected",BaleTokenType.KEYWORD);
   js_keyword_map.put("public",BaleTokenType.KEYWORD);
   js_keyword_map.put("return",BaleTokenType.RETURN);
   js_keyword_map.put("static",BaleTokenType.STATIC);
   js_keyword_map.put("super",BaleTokenType.KEYWORD);
   js_keyword_map.put("switch",BaleTokenType.SWITCH);
   js_keyword_map.put("this",BaleTokenType.KEYWORD);
   js_keyword_map.put("throw",BaleTokenType.KEYWORD);
   js_keyword_map.put("true",BaleTokenType.KEYWORD);
   js_keyword_map.put("try",BaleTokenType.TRY);
   js_keyword_map.put("typeof",BaleTokenType.KEYWORD);
   js_keyword_map.put("var",BaleTokenType.KEYWORD);
   js_keyword_map.put("void",BaleTokenType.TYPEKEY);
   js_keyword_map.put("while",BaleTokenType.WHILE);
   js_keyword_map.put("with",BaleTokenType.KEYWORD);
   js_keyword_map.put("yield",BaleTokenType.KEYWORD);

   js_op_set = new HashSet<String>();
   js_op_set.add("=");
   js_op_set.add("<");
   js_op_set.add("!");
   js_op_set.add("~");
   js_op_set.add("?");
   js_op_set.add(":");
   js_op_set.add("==");
   js_op_set.add("===");
   js_op_set.add("<=");
   js_op_set.add(">=");
   js_op_set.add("!=");
   js_op_set.add("!==");
   js_op_set.add("||");
   js_op_set.add("&&");
   js_op_set.add("++");
   js_op_set.add("--");
   js_op_set.add("+");
   js_op_set.add("-");
   js_op_set.add("*");
   js_op_set.add("/");
   js_op_set.add("&");
   js_op_set.add("|");
   js_op_set.add("^");
   js_op_set.add("%");
   js_op_set.add("<<");
   js_op_set.add("+=");
   js_op_set.add("-=");
   js_op_set.add("*=");
   js_op_set.add("/=");
   js_op_set.add("&=");
   js_op_set.add("|=");
   js_op_set.add("^=");
   js_op_set.add("%=");
   js_op_set.add("<<=");
   js_op_set.add(">>=");
   js_op_set.add(">>>=");
   js_op_set.add(">>");
   js_op_set.add(">>>");
   js_op_set.add(">");
}





}	// end of class BaleTokenizer




/* end of BaleTokenizer.java */
