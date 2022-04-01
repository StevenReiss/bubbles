/********************************************************************************/
/*										*/
/*		BaleIndenterJava.java						*/
/*										*/
/*	Bubble Annotated Language Editor indentation computations for Java	*/
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


/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/



package edu.brown.cs.bubbles.bale;




class BaleIndenterJava extends BaleIndenter implements BaleConstants {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

// private boolean	pref_use_tabs;
private int		pref_tab_size;
private int		pref_indentation_size;
private int		pref_block_indent;
private int		pref_method_body_indent;
private int		pref_type_indent;
private int		pref_continuation_indent;
private boolean 	pref_array_dims_deep_indent;
private int		pref_array_indent;
private boolean 	pref_array_deep_indent;
private boolean 	pref_ternary_deep_align;
private int		pref_ternary_indent;
private int		pref_case_indent;
private int		pref_assignment_indent;
private int		pref_case_block_indent;
private int		pref_simple_indent;
private int		pref_bracket_indent;
private boolean 	pref_method_decl_deep_indent;
private int		pref_method_decl_indent;
private boolean 	pref_method_call_deep_indent;
private int		pref_method_call_indent;
private boolean 	pref_parenthesis_deep_indent;
private int		pref_parenthesis_indent;
private boolean 	pref_indent_braces_for_blocks;
private boolean 	pref_indent_braces_for_arrays;
private boolean 	pref_indent_braces_for_methods;
private boolean 	pref_indent_braces_for_types;
private boolean 	pref_has_generics;
private int		pref_rbrace_indent;
private boolean 	pref_indent_inner_class;
private long		setup_time;

private BaleElement	cur_element;
private int		cur_indent;
private int		cur_align;
private int		cur_offset;
private int		previous_offset;
private BaleTokenType	cur_token;
private int		cur_line;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleIndenterJava(BaleDocument bd)
{
   super(bd);

   setup_time = 0;

   setupPreferences();
}


/********************************************************************************/
/*										*/
/*	Preference methods							*/
/*										*/
/********************************************************************************/

private void setupPreferences()
{
   if (setup_time != 0 && setup_time > BaleFactory.getFormatTime()) return;
   setup_time = System.currentTimeMillis();

   int tz = getOptionInt("tabulation.size",8);
   if (tz != pref_tab_size) {
      if (pref_tab_size != 0) BaleTabHandler.setBaseTabSize(tz);
      pref_tab_size = tz;
    }
   pref_indentation_size = getOptionInt("indentation.size",4);
   pref_block_indent = (getOptionBool("indent_statements_compare_to_block",true) ? 1 : 0);
   pref_method_body_indent = (getOptionBool("indent_staements_compare_to_body",true) ? 1 : 0);
   pref_type_indent = (getOptionBool("indent_body_declarations_compare_to_type_header",true) ? 1 : 0);
   pref_continuation_indent = getOptionInt("continuation_indent",2);
   int opts = getOptionInt("alignment_for_expressions_in_array_initializer",0);
   pref_array_indent = ((opts & 0x3) == 2 ? 1 : pref_continuation_indent);
   pref_array_dims_deep_indent = true;
   pref_array_deep_indent = ((opts & 0x3) == 1);
   opts = getOptionInt("alignment_for_condition_expression",80);
   pref_ternary_deep_align = ((opts & 0x3) == 1);
   pref_ternary_indent = (((opts & 0x3) == 2) ? 1 : pref_continuation_indent);
   pref_indent_braces_for_blocks = getOption("brace_position_for_block","eol").equals("next_line_shifted");
   pref_indent_braces_for_arrays = getOption("brace_position_for_array_initializer","eol").equals("next_line_shifted");
   pref_indent_braces_for_methods = getOption("brace_position_for_method_declaration","eol").equals("next_line_shifted");
   pref_indent_braces_for_types = getOption("brace_position_for_type_declaration","eol").equals("next_line_shifted");
   pref_case_indent = (getOptionBool("indent_switchstatements_compare_to_switch",true) ? pref_block_indent : 0);
   pref_assignment_indent = pref_block_indent;
   pref_case_block_indent = (getOptionBool("indent_switchstatements_compare_to_cases",true) ? pref_block_indent : 0);
   pref_simple_indent = ((pref_indent_braces_for_blocks && pref_block_indent == 0) ? 1 : pref_block_indent);
   pref_bracket_indent = pref_block_indent;
   opts = getOptionInt("alignment_for_parameters_in_method_declaration",16);
   pref_method_decl_deep_indent = ((opts & 0x3) == 1);
   pref_method_decl_indent = ((opts & 0x3) == 2 ? 1 : pref_continuation_indent);
   opts = getOptionInt("alignment_for_arguments_in_method_declaration",16);
   pref_method_call_deep_indent = ((opts & 0x3) == 1);
   pref_method_call_indent = ((opts & 0x3) == 2 ? 1 : pref_continuation_indent);
   pref_parenthesis_deep_indent = false;
   pref_parenthesis_indent = pref_continuation_indent;
   pref_has_generics = true;
   pref_rbrace_indent = getOptionInt("rbrace",0);
   pref_indent_inner_class = getOptionBool("innerclass",false);
}



@Override protected int getTabSize()			{ return pref_tab_size; }



/********************************************************************************/
/*										*/
/*	Methods to return the indentation for the line at the given offset	*/
/*										*/
/********************************************************************************/

@Override int getDesiredIndentation(int offset)
{
   setupPreferences();

   bale_document.readLock();
   try {
      return findLineReferenceIndentation(offset);
    }
   finally { bale_document.readUnlock(); }
}



@Override int getCurrentIndentation(int offset)
{
   setupPreferences();

   bale_document.readLock();
   try {
      return getLeadingWhitespaceLength(offset);
    }
   finally { bale_document.readUnlock(); }
}



@Override int getSplitIndentationDelta(int offset)
{
   setupPreferences();

   bale_document.readLock();
   try {
      int off0 = findReferenceIndentation(offset);
      int off1 = getPositionWhitespaceLength(offset);
      return off0 - off1;
    }
   finally { bale_document.readUnlock(); }
}



@Override int getUnindentSize()
{
   return pref_indentation_size;
}




/********************************************************************************/
/*										*/
/*	Top level indentation checking methods for a line			*/
/*										*/
/********************************************************************************/

private int findLineReferenceIndentation(int offset)
{
   int lno = bale_document.findLineNumber(offset);
   int loff = bale_document.findLineOffset(lno);

   return findReferenceIndentation(loff);
}


private int findReferenceIndentation(int offset)
{
   int pos = findReferencePosition(offset);
   int indent = cur_align;

   if (indent < 0) {
      if (pos >= 0) indent = getLeadingWhitespaceLength(pos);
      else indent = 0;
    }

   if (cur_indent != 0) {
      indent += pref_indentation_size * cur_indent;
      if (indent < 0) indent = 0;
    }

   return indent;
}


private int findReferencePosition(int offset)
{
   boolean danglingelse = false;
   boolean unindent = false;
   boolean indent = false;
   boolean matchbrace = false;
   boolean matchparen = false;
   boolean matchcase = false;

   cur_indent = 0;
   cur_align = -1;

   BaleElement elt = bale_document.getActualCharacterElement(offset);
   setCurrent(elt);

   BaleElement pelt = getPreviousElement(elt);
   BaleTokenType ptyp = getToken(pelt);
   boolean bracelessblockstart = false;
   switch (ptyp) {
      case DO :
      case ELSE :
	 bracelessblockstart = true;
	 break;
      case RPAREN :
	 BaleElement melt = findOpeningPeer(pelt,BaleTokenType.LPAREN,BaleTokenType.RPAREN);
	 melt = getPreviousElement(melt);
	 if (melt != null) {
	    switch (melt.getTokenType()) {
	       case IF :
	       case FOR :
	       case WHILE :
		  bracelessblockstart = true;
		  break;
	       default:
		  break;
	     }
	  }
	 break;
//    case OP :
//    case CHARLITERAL :
//    case STRING :
//    case NUMBER :
//    case IDENTIFIER :
//       indent = true;
//       break;
      default:
	 break;
    }

   if (elt != null && elt.isComment()) return findCommentPosition();
   if (elt != null && elt.isEmpty()) {
      if (elt.isEndOfLine()) elt = null;
      else elt = getNextElement(elt,true);
    }
   if (elt != null) setCurrent(elt);

   BaleTokenType tt = getToken(elt);
   switch (tt) {
      case ELSE :
	 danglingelse = true;
	 break;
      case CASE :
      case DEFAULT :
	 matchcase = true;
	 break;
      case LBRACE :
	 if (bracelessblockstart && !pref_indent_braces_for_blocks) unindent = true;
	 else if ((ptyp == BaleTokenType.COLON || ptyp == BaleTokenType.EQUAL ||
		      ptyp == BaleTokenType.RBRACKET) && !pref_indent_braces_for_arrays)
	    unindent = true;
	 else if (!bracelessblockstart && pref_indent_braces_for_methods)
	    indent = true;
	 break;
      case RBRACE :
	 matchbrace = true;
	 break;
      case RPAREN :
	 matchparen = true;
	 break;
      default:
	 break;
    }

   int ref = findReferencePosition(danglingelse,matchbrace,matchparen,matchcase);

   if (unindent) cur_indent--;
   if (indent) cur_indent++;

   return ref;
}



private int findCommentPosition()
{
   return cur_offset;
}



/********************************************************************************/
/*										*/
/*	Main indentation computation method					*/
/*										*/
/********************************************************************************/

@SuppressWarnings("fallthrough")
private int findReferencePosition(boolean danglingelse,boolean matchbrace,
      boolean matchparen,boolean matchcase)
{
   cur_indent = 0; // the indentation modification type filter text
   cur_align = -1; // base line alignment
   BaleElement start = cur_element;

   // forward cases
   // an unindentation happens sometimes if the next token is special,namely on braces,parens and case labels
   // align braces,but handle the case where we align with the method declaration start instead of
   // the opening brace.

   if (matchbrace) {
      if (skipScope(BaleTokenType.LBRACE,BaleTokenType.RBRACE)) {
	 // if cur_element is first token on the line, return the indent of the line
	 // if the opening brace is not on the start of the line,skip to the start
	 int pos = skipToStatementStart(true,true,false);
	 cur_indent = 0; // indent is aligned with reference position
	 if (pref_rbrace_indent > 0 && pos > 0) {
	    int wp = getLeadingWhitespaceLength(pos);
	    if (wp > 0) cur_align = wp + pref_rbrace_indent;
	  }
	 return pos;
       }
      else {
	 // if we can't find the matching brace,the heuristic is to unindent
	 // by one against the normal position
	 int pos = findReferencePosition(danglingelse,false,matchparen,matchcase);
	 cur_indent--;
	 return pos;
       }
    }

   // align parenthesis
   if (matchparen) {
      if (skipScope(BaleTokenType.LPAREN,BaleTokenType.RPAREN)) return cur_offset;
      else {
	 // if we can't find the matching paren,the heuristic is to unindent
	 // by one against the normal position
	 int pos = findReferencePosition(danglingelse,matchbrace,false,matchcase);
	 cur_indent--;
	 return pos;
       }
    }

   // the only reliable way to get case labels aligned (due to many different styles of using braces in a block)
   // is to go for another case statement,or the scope opening brace
   if (matchcase) {
      return matchCaseAlignment();
    }

   previousToken();
   boolean eos = false;

   switch (cur_token) {
      case RBRACE:
         eos = true;
      // fall through
      case RANGLE :
	 // skip the block and fall through
	 // if we can't complete the scope,reset the scan position
	 BaleElement ce = cur_element;
	 if (!skipScope()) setCurrent(ce);
      // fall through
      case SEMICOLON:
         eos = true;
	 // this is the 90% case: after a statement block
	 // the end of the previous statement / block previous.end
	 // search to the end of the statement / block before the previous; the token just after that is previous.start
	 return skipToStatementStart(danglingelse,false,eos);

	 // scope introduction: special treat who special is
      case LPAREN:
      case LBRACE:
      case LBRACKET:
	 return handleScopeIntroduction(start);

      case EOF:
      case NONE :
	 return -1;

      case EQUAL:
	 // indent assignments
	 cur_indent = pref_assignment_indent;
	 return cur_offset;

      case COLON:
	 // TODO handle ternary deep indentation
	 cur_indent = pref_case_block_indent;
	 return cur_offset;

      case QUESTIONMARK:
	 if (pref_ternary_deep_align) {
	    setFirstElementAlignment(cur_offset,start);
	    return cur_offset;
	  }
	 else {
	    cur_indent = pref_ternary_indent;
	    return cur_offset;
	  }

	 // indentation for blockless introducers:
      case DO :
      case WHILE:
      case ELSE:
	 cur_indent = pref_simple_indent;
	 return cur_offset;

      case TRY :
	 return skipToStatementStart(danglingelse,false,false);

      case RPAREN:
	 if (skipScope(BaleTokenType.LPAREN,BaleTokenType.RPAREN)) {
	    BaleElement scope = cur_element;
	    previousToken();
	    if (cur_token == BaleTokenType.IF || cur_token == BaleTokenType.WHILE ||
		   cur_token == BaleTokenType.FOR) {
	       cur_indent = pref_simple_indent;
	       return cur_offset;
	     }
	    setCurrent(scope);
	    if (looksLikeMethodDecl()) {
	       return skipToStatementStart(danglingelse,false,false);
	     }
	    if (cur_token == BaleTokenType.CATCH) {
	       return skipToStatementStart(danglingelse,false,false);
	     }
	    setCurrent(scope);
	    if (looksLikeAnonymousTypeDecl()) {
	       return skipToStatementStart(danglingelse,false,false);
	     }
	  }
	 // restore
	 setCurrent(start);
	 return skipToPreviousListItemOrListStart();

         
      case COMMA:
	 // inside a list of some type
	 // easy if there is already a list item before with its own indentation - we just align
	 // if not: take the start of the list ( LPAREN,LBRACE,LBRACKET ) and either align or
	 // indent by list-indent
      default:
	 // inside whatever we don't know about: similar to the list case:
	 // if we are inside a continued expression,then either align with a previous line that has indentation
	 // or indent from the expression start line (either a scope introducer or the start of the expr).
         setCurrent(start);
	 return skipToPreviousListItemOrListStart();
    }
}




/********************************************************************************/
/*										*/
/*	Utility scanning / query methods					*/
/*										*/
/********************************************************************************/

private int getBlockIndent(boolean ismethodbody,boolean istypebody,boolean isinnerclass)
{
   if (istypebody) {
      int v = pref_type_indent + (pref_indent_braces_for_types ? 1 : 0);
      if (v == 0 && pref_indent_inner_class && isinnerclass) v = 1;
      return v;
    }
   else if (ismethodbody)
      return pref_method_body_indent + (pref_indent_braces_for_methods ? 1 : 0);

   return cur_indent;
}



private boolean isConditional()
{
   while (true) {
      previousToken();
      switch (cur_token) {
	 // search for case labels,which consist of (possibly qualified) identifiers or numbers
	 case IDENTIFIER :
	 case KEYWORD :
	 case TYPEKEY :
	 case OTHER :			      // dots for qualified constants
	 case DOT :
	 case NUMBER :
	 case CHARLITERAL :
	 case STRING :
	 case LONGSTRING :
	 case CONTINUE :
	 case PASS :
	 case RAISE :
         case IMPORT :
         case PACKAGE :
	    continue;
	 case CASE:
	 case DEFAULT :
	    return false;
	 default:
	    return true;
       }
    }
}



private int matchCaseAlignment()
{
   while (true) {
      previousToken();
      switch (cur_token) {
	 // invalid cases: another case label or an LBRACE must come before a case
	 // -> bail out with the current position
	 case LPAREN :
	 case LBRACKET :
	 case EOF :
	 case NONE :
	    return cur_offset;
	 case LBRACE:
	    // opening brace of switch statement
	    cur_indent = pref_case_indent;
	    return cur_offset;
	 case CASE :
	 case DEFAULT :
	    // align with previous label
	    cur_indent = 0;
	    return cur_offset;

	    // scopes: skip them
	 case RPAREN :
	 case RBRACKET :
	 case RBRACE :
	 case RANGLE :
	    skipScope();
	    break;

	 default:
	    // keep searching
	    continue;
       }
    }
}




private int handleScopeIntroduction(BaleElement bound)
{
   switch (cur_token) {
      // scope introduction: special treat who special is
      case LPAREN :
	 BaleElement celt = cur_element;
	 int pos = cur_offset; // store

	 // special: method declaration deep indentation
	 if (looksLikeMethodDecl()) {
	    if (pref_method_decl_deep_indent) return setFirstElementAlignment(pos,bound);
	    else {
	       cur_indent = pref_method_decl_indent;
	       return pos;
	     }
	  }
	 else {
	    setCurrent(celt);
	    if (looksLikeMethodCall()) {
	       if (pref_method_call_deep_indent) return setFirstElementAlignment(pos,bound);
	       else {
		  cur_indent = pref_method_call_indent;
		  return pos;
		}
	     }
	    else if (pref_parenthesis_deep_indent)
	       return setFirstElementAlignment(pos,bound);
	  }

	 // normal: return the parenthesis as reference
	 cur_indent = pref_parenthesis_indent;
	 return pos;

      case LBRACE :
	 celt = cur_element;
	 pos = cur_offset; // store

	 // special: array initializer
	 if (looksLikeArrayInitializerIntro()) {
	    if (pref_array_deep_indent) return setFirstElementAlignment(pos,bound);
	    else cur_indent = pref_array_indent;
	  }
	 else cur_indent = pref_block_indent;
         
	 // normal: skip to the statement start before the scope introducer
	 // opening braces are often on differently ending indents than e.g. a method definition
	 if (looksLikeArrayInitializerIntro() && !pref_indent_braces_for_arrays
               || !pref_indent_braces_for_blocks) {
	    setCurrent(celt);
	    return skipToStatementStart(true,true,false); // set to true to match the first if
	  }
	 else return pos;
         
      case LBRACKET :
	 celt = cur_element;
	 pos = cur_offset; // store

	 // special: method declaration deep indentation
	 if (pref_array_dims_deep_indent) {
	    return setFirstElementAlignment(pos,bound);
	  }

	 // normal: return the bracket as reference
	 cur_indent = pref_bracket_indent;
	 return pos; // restore

      default:
	 return -1; // dummy
    }
}




private int setFirstElementAlignment(int scopeintroduceroffset,BaleElement bound)
{
   //TODO: This doesn't really match the original code
   cur_align = getLeadingWhitespaceLength(scopeintroduceroffset);

   return cur_align;
}




/********************************************************************************/
/*										*/
/*	Methods to scan backward over a unit of some sort			*/
/*										*/
/********************************************************************************/

private int skipToPreviousListItemOrListStart()
{
   int startline = cur_line;
   int startposition = cur_offset;
   BaleElement start = cur_element;

   while (true) {
      previousToken();

      // if any line item comes with its own indentation,adapt to it
      if (cur_line < startline-1) {
         int ind = 0;
// 	 ind = getLineIndent(startline);
         // handle starting at empty line
         if (ind == 0) ind = getLineIndent(startline-1);       
	 if (ind >= 0) cur_align = ind;
         // cur_indent = pref_continuation_indent;
	 return startposition;
       }

      switch (cur_token) {
	 // scopes: skip them
	 case RPAREN :
	 case RBRACKET :
	 case RBRACE :
	 case RANGLE :
	    skipScope();
	    break;

	    // scope introduction: special treat who special is
	 case LPAREN :
	 case LBRACE :
	 case LBRACKET :
            if (cur_line == startline-1) {
               cur_indent = pref_continuation_indent;
               return cur_offset;
             }
	    return handleScopeIntroduction(start);
            
	 case SEMICOLON :
            if (cur_line == startline-1) cur_indent = pref_continuation_indent;
	    return cur_offset;

	 case QUESTIONMARK :
	    if (pref_ternary_deep_align) {
	       setFirstElementAlignment(cur_offset - 1,cur_element);
	     }
	    else {
	       cur_indent = pref_ternary_indent;
	     }
	    return cur_offset;
	 case EOF :
	 case NONE :
	    return 0;
	 default:
	    break;
       }
    }
}




private boolean skipScope(BaleTokenType open,BaleTokenType close)
{
   int ct = 0;
   for ( ; ; ) {
      BaleTokenType tt = previousToken();
      if (cur_element == null) break;
      if (tt == close) ++ct;
      else if (tt == open) {
	 --ct;
	 if (ct < 0) return true;
       }
    }

   return false;
}



@SuppressWarnings("fallthrough")
private boolean skipScope()
{
   switch (cur_token) {
      case RPAREN :
	 return skipScope(BaleTokenType.LPAREN,BaleTokenType.RPAREN);
      case RBRACKET :
	 return skipScope(BaleTokenType.LBRACKET,BaleTokenType.RBRACKET);
      case RBRACE :
	 return skipScope(BaleTokenType.LBRACE,BaleTokenType.RBRACE);
      case RANGLE :
	 if (!pref_has_generics) return false;
	 BaleElement stored = cur_element;
	 previousToken();
	 switch (cur_token) {
	    case IDENTIFIER :
	       if (!isGenericStarter(getTokenContent())) break;
	       // fall through
	    case QUESTIONMARK :
	    case RANGLE :
	       if (skipScope(BaleTokenType.LANGLE,BaleTokenType.RANGLE))
		  return true;
	    default:
	       break;
	  }
	 // <> are harder to detect - restore the position if we fail
	 setCurrent(stored);
	 return false;

      default:
	 return false;
    }
}




private boolean skipNextIF()
{
   while (true) {
      previousToken();
      switch (cur_token) {
	 // scopes: skip them
	 case RPAREN :
	 case RBRACKET :
	 case RBRACE :
	 case RANGLE :
	    skipScope();
	    break;

	 case IF :
	    // found it,return
	    return true;

	 case ELSE :
	    // recursively skip else-if blocks
	    skipNextIF();
	    break;

	    // shortcut scope starts
	 case LPAREN :
	 case LBRACE :
	 case LBRACKET :
	 case EOF :
	 case NONE :
	    return false;
	 default:
	    break;
       }
    }
}




@SuppressWarnings("fallthrough")
private int skipToStatementStart(boolean danglingelse,boolean isinblock,boolean ateos)
{
   final int NOTHING = 0;
   final int READ_PARENS = 1;
   final int READ_IDENT = 2;
   int maybemethodbody = NOTHING;
   boolean istypebody = false;
   boolean innerclass = false;

   while (true) {
      previousToken();
      if (isinblock) {
	 switch (cur_token) {
	    // exit on all block introducers
	    case IF :
	    case ELSE :
	    case CATCH :
	    case DO :
	    case WHILE :
	    case FINALLY :
	    case FOR :
	    case TRY :
	       return cur_offset;
	    case STATIC :
	       maybemethodbody= READ_IDENT; // treat static blocks like methods
	       break;
	    case SYNCHRONIZED:
	       // if inside a method declaration, use body indentation
	       // else use block indentation.
	       if (maybemethodbody != READ_IDENT) return cur_offset;
	       break;
	    case CLASS:
	    case INTERFACE:
	    case ENUM:
	       istypebody = true;
	       break;
	    case SWITCH:
	       cur_indent = pref_case_indent;
	       return cur_offset;
	    default:
	       break;
	  }
       }

      switch (cur_token) {
	 // scope introduction through: LPAREN, LBRACE, LBRACKET
	 // search stop on SEMICOLON, RBRACE, COLON, EOF
	 // -> the next token is the start of the statement (i.e. previousPos when backward scanning)
	 case LPAREN:
	 case LBRACE:
	 case LBRACKET:
	 case SEMICOLON:
	 case EOF:
	 case NONE :
	    if (isinblock) cur_indent = getBlockIndent(maybemethodbody == READ_IDENT, istypebody, innerclass);
	    else if (cur_indent == 0 && cur_token == BaleTokenType.LPAREN) cur_indent = 1;	// handle for
	    // else: cur_indent set by previous calls
            else if (cur_token == BaleTokenType.NONE && !ateos) {
               cur_indent = 1;
             }
	    return previous_offset;
	 case COLON:
	    int pos = previous_offset;
	    if (!isConditional()) return pos;
	    break;
	 case RBRACE:
	    // RBRACE is a little tricky: it can be the end of an array definition, but
	    // usually it is the end of a previous block
	    pos = previous_offset; // store state
	    if (skipScope() && looksLikeArrayInitializerIntro())  continue;
	    else {
	       if (isinblock)
		  cur_indent = getBlockIndent(maybemethodbody == READ_IDENT, istypebody, innerclass);
	       return pos; // it's not - do as with all the above
	     }
	    // scopes: skip them
	 case RPAREN:
	    if (isinblock) maybemethodbody = READ_PARENS;
	    // fall through
	 case RBRACKET:
	 case RANGLE :
	    pos = previous_offset;
	    if (skipScope()) break;
	    else return pos;

	    // IF / ELSE: align the position after the conditional block with the if
	    // so we are ready for an else, except if danglingElse is false
	    // in order for this to work, we must skip an else to its if
	 case IF:
	    if (danglingelse) return cur_offset;
	    else break;
	 case ELSE:
	    // skip behind the next if, as we have that one covered
	    pos = cur_offset;
	    if (skipNextIF()) break;
	    else return pos;

	 case DO:
	    // align the WHILE position with its do
	    return cur_offset;

	 case WHILE:
	    // this one is tricky: while can be the start of a while loop
	    // or the end of a do - while
	    BaleElement ce = cur_element;
	    if (hasMatchingDo()) {
	       // continue searching from the DO on
	     }
	    else {
	       // continue searching from the WHILE on
	       setCurrent(ce);
	     }
	    break;
	 case IDENTIFIER :
	 case KEYWORD :
	 case TYPEKEY :
	 case CONTINUE :
	 case PASS :
	 case RAISE :
         case IMPORT :
         case PACKAGE :
	    if (maybemethodbody == READ_PARENS) maybemethodbody = READ_IDENT;
	    if (cur_element != null && cur_element.getName().equals("ClassDeclMemberId"))
	       innerclass = true;
	    break;

	 default:
	    // keep searching
       }
    }
}




private boolean skipBrackets()
{
   if (cur_token == BaleTokenType.RBRACKET) {
      previousToken();
      if (cur_token == BaleTokenType.LBRACKET) return true;
    }

   return false;
}



/********************************************************************************/
/*										*/
/*	Methods to guess at constructs						*/
/*										*/
/********************************************************************************/

private static boolean isGenericStarter(CharSequence identifier)
{
   /* This heuristic allows any identifiers if they start with an upper
      * case. This will fail when a comparison is made with constants:
      *
      * if (MAX > foo)
      *
      * will try to find the matching '<' which will never come
      *
      * Also, it will fail on lower case types and type variables
      */
   int length= identifier.length();
   if (length > 0 && Character.isUpperCase(identifier.charAt(0))) {
      for (int i= 0; i < length; i++) {
	 if (identifier.charAt(i) == '_') return false;
       }
      return true;
    }

   return false;
}



private boolean looksLikeArrayInitializerIntro()
{
   BaleElement celt = cur_element;
   try {
      previousToken();
      if (cur_token == BaleTokenType.EQUAL || skipBrackets()) return true;
      return false;
    }
   finally {
      setCurrent(celt);
    }
}



@SuppressWarnings("fallthrough")
private boolean hasMatchingDo()
{
   previousToken();

   switch (cur_token) {
      case RBRACE :
	 skipScope();
	 // fall through
      case SEMICOLON :
	 skipToStatementStart(false,false,false);
	 return cur_token == BaleTokenType.DO;
      default:
	 break;
    }

   return false;
}



private boolean looksLikeMethodDecl()
{
   /*
    * TODO This heuristic does not recognize package private constructors
    * since those do have neither type nor visibility keywords.
    * One option would be to go over the parameter list,but that might
    * be empty as well,or not typed in yet - hard to do without an AST...
    */

   previousToken();
   if (cur_token == BaleTokenType.IDENTIFIER) { // method name
      do previousToken();
      while (skipBrackets()); // optional brackets for array valued return types

      // return type name
      if (cur_token == BaleTokenType.IDENTIFIER || cur_token == BaleTokenType.TYPEKEY) return true;
    }

   return false;
}



private boolean looksLikeAnonymousTypeDecl()
{
   previousToken();
   if (cur_token == BaleTokenType.IDENTIFIER || cur_token == BaleTokenType.TYPEKEY) {
      previousToken();
      while (cur_token == BaleTokenType.DOT) { // dot of qualification
	 previousToken();
	 if (cur_token != BaleTokenType.IDENTIFIER && cur_token != BaleTokenType.TYPEKEY)
	    return false ;		// qualificating name
	 previousToken();
       }
      return cur_token == BaleTokenType.NEW;
    }
   return false;
}



private boolean looksLikeMethodCall()
{
   // TODO [5.0] add awareness for constructor calls with generic types: new ArrayList<String>()
   previousToken();
   return cur_token == BaleTokenType.IDENTIFIER; // method name
}


private CharSequence getTokenContent()
{
   return getTokenContent(cur_element);
}



/********************************************************************************/
/*										*/
/*	Token scanning methods							*/
/*										*/
/********************************************************************************/

private void setCurrent(BaleElement e)
{
   cur_element = e;
   cur_token = getToken(e);
   previous_offset = cur_offset;

   if (e == null) {
      cur_offset = -1;
      cur_line = 0;
    }
   else {
      cur_offset = e.getStartOffset();
      cur_line = bale_document.findLineNumber(cur_offset);
    }
}



private BaleTokenType previousToken()
{
   setCurrent(getPreviousElement(cur_element));
   return cur_token;
}




/********************************************************************************/
/*										*/
/*	Option access methods							*/
/*										*/
/********************************************************************************/

private String getOption(String id,String dflt)
{
   String v = BALE_PROPERTIES.getProperty("Bale.indent." + id);
   if (v != null) return v;
   v = bump_client.getOption("org.eclipse.jdt.core.formatter." + id);
   if (v != null) return v;
   return dflt;
}


private boolean getOptionBool(String id,boolean dflt)
{
   String v = getOption(id,null);
   if (v == null || v.length() == 0) return dflt;

   if ("yYtT1".indexOf(v.charAt(0)) >= 0) return true;
   return false;
}


private int getOptionInt(String id,int dflt)
{
   String v = getOption(id,null);
   if (v != null) {
      try {
	 return Integer.parseInt(v);
       }
      catch (NumberFormatException e) { }
    }

   return dflt;
}




}	// end of class BaleIndenterJava




/* end of BaleIndenterJava.java */
