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


package edu.brown.cs.bubbles.bale;



import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.w3c.dom.Element;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.ivy.xml.IvyXml;


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
private char		cur_delim;
private int		delim_count;
protected int		cur_flags;
private TokenData       token_data;


private static Map<BoardLanguage,TokenData> language_data = new HashMap<>();
private static Map<BoardLanguage,BaleTokenizer> default_tokenizers = new HashMap<>();

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
      case JAVA :
      case JAVA_IDEA :
      case DART :
	 return new DefaultTokenizer(text,start,bl);
      case JS :
	 return new JSTokenizer(text,start,bl);
    }
}



private BaleTokenizer(String text,BaleTokenState start,BoardLanguage bl)
{
   input_text = (text == null ? "" : text);
   cur_offset = 0;
   end_offset = (text == null ? 0 : input_text.length());
   token_state = start;
   ignore_white = false;
   cur_delim = 0;
   delim_count = 0;
   cur_flags = 0;
   
   token_data = language_data.get(bl);
   if (token_data == null) {
      token_data = new TokenData(bl);
      language_data.put(bl,token_data);
    }
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

void setIgnoreWhitespace(boolean fg)			{ ignore_white = fg; }

protected boolean useSlashStarComments()		 { return token_data.slashstar_comments; }
protected boolean useSlashSlashComments()	 	 { return token_data.slashslash_comments; }
protected boolean useHashComments()			{ return token_data.hash_comments; }
protected boolean isStringStart(char c) 		{ return token_data.string_chars.indexOf(c) >= 0; }

static Collection<String> getKeywords(BoardLanguage bl)
{
   BaleTokenizer bt = default_tokenizers.get(bl);
   if (bt == null) {
      bt = create(null,BaleTokenState.NORMAL,bl);
      default_tokenizers.put(bl,bt);
    }
   return bt.token_data.keyword_map.keySet();
}


protected BaleTokenType getKeyword(String s)		{ return token_data.keyword_map.get(s); }
protected boolean isOperator(String s)		{ return token_data.op_set.contains(s); }
protected int useMultilineCount()			{ return token_data.multiline_count; }
protected boolean isMultilineStringStart(char c)	{ return c != 0 && c == token_data.multiline_string; }



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

// CHECKSTYLE:OFF
BaleToken getNextToken()
// CHECKSTYLE:ON
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
	 for (int i = 0; i < ct-1; ++i) {
	    char ch1 = nextChar();
	    bup++;
	    if (ch1 != ch) break;
	    if (i == ct-2) {
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
   else if (ch == '}') {
      cur_flags = 0;
      return buildToken(BaleTokenType.RBRACE);
    }
   else if (ch == '(') return buildToken(BaleTokenType.LPAREN);
   else if (ch == ')') return buildToken(BaleTokenType.RPAREN);
   else if (ch == '[') return buildToken(BaleTokenType.LBRACKET);
   else if (ch == ']') return buildToken(BaleTokenType.RBRACKET);
   else if (ch == ';') {
      cur_flags = 0;
      return buildToken(BaleTokenType.SEMICOLON);
    }
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
	 return buildToken(BaleTokenType.EOLSTRING);
       }
      else if (ch == '\r') {
	 ch = nextChar();
	 if (ch != '\n') backup();
	 token_state = BaleTokenState.IN_MULTILINE_STRING;
	 return buildToken(BaleTokenType.EOLSTRING);
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
/*                                                                              */
/*      Language-specific token information                                     */
/*                                                                              */
/********************************************************************************/


private static class TokenData {
   private Map<String,BaleTokenType> keyword_map;
   private Set<String> op_set;
   private int multiline_count;
   private char multiline_string;
   private String string_chars;
   private boolean slashslash_comments;
   private boolean slashstar_comments;
   private boolean hash_comments; 
   
   TokenData(BoardLanguage bl) {
      keyword_map = new HashMap<>();
      Element xml = BumpClient.getBump().getLanguageData(bl);
      Element exml = IvyXml.getChild(xml,"EDITING");
      Element kxml = IvyXml.getChild(exml,"KEYWORDS");
      for (Element kwd : IvyXml.children(kxml,"KEYWORD")) {
         String nm = IvyXml.getAttrString(kwd,"NAME");
         String typ = IvyXml.getAttrString(kwd,"TYPE");
         BaleTokenType bt = BaleTokenType.valueOf(typ);
         keyword_map.put(nm,bt);
       }
      op_set = new HashSet<>();
      Element oxml = IvyXml.getChild(exml,"OPERATORS");
      String opset = IvyXml.getText(oxml);
      if (opset != null) {
         StringTokenizer tok = new StringTokenizer(opset);
         while (tok.hasMoreTokens()) {
            op_set.add(tok.nextToken());
          }
       }
      Element txml = IvyXml.getChild(exml,"TOKENS");
      String mls = IvyXml.getTextElement(txml,"MUTLILINE");
      if (mls != null) {
         mls = mls.trim();
         if (mls.isEmpty()) mls = null;
       }
      multiline_count = 0;
      multiline_string = (char) 0;
      if (mls != null && mls.length() > 1) multiline_count = mls.length();
      else if (mls != null) multiline_string = mls.charAt(0);
      String sc = IvyXml.getTextElement(txml,"STRING");
      if (sc == null || sc.trim().isEmpty()) string_chars = "\"";
      else string_chars = sc.trim();
      String cmmts = IvyXml.getTextElement(txml,"COMMENTS");
      if (cmmts == null) cmmts = "// /*";
      slashslash_comments = cmmts.contains("//");
      slashstar_comments = cmmts.contains("/*");
      hash_comments = cmmts.contains("#"); 
    }
   
}       // end of inner class TokenData



/********************************************************************************/
/*										*/
/*	Default tokenizer -- use language data					*/
/*										*/
/********************************************************************************/

private static class DefaultTokenizer extends BaleTokenizer {

   DefaultTokenizer(String text,BaleTokenState start,BoardLanguage bl) {
      super(text,start,bl);
    }

}	// end of inner class JavaTokenizer



/********************************************************************************/
/*										*/
/*	JavaScript tokenizer							*/
/*										*/
/********************************************************************************/

private static final int INSIDE_FOR = 1;
private static final int INSIDE_IMPORT = 2;


private static class JSTokenizer extends BaleTokenizer {

   JSTokenizer(String text,BaleTokenState start,BoardLanguage bl) {
      super(text,start,bl);
    }

   @Override protected BaleTokenType getKeyword(String s) {
      if (s.equals("for")) cur_flags = INSIDE_FOR;
      else if (s.equals("import")) cur_flags = INSIDE_IMPORT;
      switch (cur_flags) {
	 case INSIDE_FOR :
	    if (s.equals("of") || s.equals("in")) return BaleTokenType.KEYWORD;
	    break;
	 case INSIDE_IMPORT :
	    if (s.equals("as") || s.equals("from")) return BaleTokenType.KEYWORD;
	    break;
       }
      return super.getKeyword(s);
    }

}	// end of inner class JSTokenizer




}	// end of class BaleTokenizer




/* end of BaleTokenizer.java */
