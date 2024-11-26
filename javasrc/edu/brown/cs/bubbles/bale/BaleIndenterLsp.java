/********************************************************************************/
/*										*/
/*		BaleIndenterLsp.java						*/
/*										*/
/*	Indenter that uses back end (LSPBASE) to compute indents		*/
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



package edu.brown.cs.bubbles.bale;

import edu.brown.cs.bubbles.board.BoardSetup;
import edu.brown.cs.bubbles.bump.BumpClient;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;


class BaleIndenterLsp extends BaleIndenter implements BaleConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

enum LineIndentMode { ALL, EMPTY, NON_EMPTY, NONE }

private int indent_space;
private int tab_size;
private int rbr_indent;
private boolean unnest_functions;
private boolean local_split;
private LineIndentMode indent_mode;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleIndenterLsp(BaleDocument bd)
{
   super(bd);
   loadProperties();
}



/********************************************************************************/
/*										*/
/*	Abstract Method Implementations 					*/
/*										*/
/********************************************************************************/

@Override int getUnindentSize()
{
   return indent_space;
}


@Override public int getTabSize()
{
   return tab_size;
}



@Override int getSplitIndentationDelta(int offset)
{
   if (local_split) {
      DefaultIndenter di = new DefaultIndenter(offset,true);
      return di.computeIndent();
   }

   return getIndent(offset,true);
}



@Override int getDesiredIndentation(int offset)
{
   return getIndent(offset,false);
}


private int getIndent(int offset,boolean split)
{
   boolean usedefault = false;
   if (indent_mode == LineIndentMode.ALL) usedefault = false;
   else if (indent_mode == LineIndentMode.NONE) usedefault = true;
   else {
      int pos = offset;
      int spos = pos;
      int epos = pos;
      boolean nonspace = false;
      while (epos < doc_text.length()) {
         char ch = doc_text.charAt(epos);
         if (ch == ' ' || ch == '\t') ++epos;
         else if (ch == '\n') break;
         else {
            nonspace = true;
            break;
          }
       }
      while (!nonspace && spos > 0) {
         char ch = doc_text.charAt(spos);
         if (ch == ' ' || ch == '\t') --spos;
         else if (ch == '\n') {
            if (spos < pos) ++spos;
            break;
          }
         else {
            nonspace = true;
            break;
          }
       }
      if (indent_mode == LineIndentMode.EMPTY) usedefault = nonspace;
      else usedefault = !nonspace;
    }
   
   if (usedefault) {
      DefaultIndenter di = new DefaultIndenter(offset,split);
      return di.computeIndent();
    }

   int eoff = bale_document.mapOffsetToEclipse(offset);

   BumpClient bc = BumpClient.getBump();
   Element rslt = bc.computeIndent(bale_document.getProjectName(),
	 bale_document.getFile(),
	 bale_document.getEditCounter(),
	 eoff,split);

   Element ind = IvyXml.getChild(rslt,"INDENT");
   return IvyXml.getAttrInt(ind,"TARGET");
}



/********************************************************************************/
/*										*/
/*	Compute default indentation						*/
/*										*/
/********************************************************************************/

private class DefaultIndenter {

   private int split_line;
   private int split_offset;
   private int current_line;
   private CharSequence line_text;
   private int line_length;
   private int init_indent;
   private int nest_level;
   private boolean eos_first;
   private boolean rbr_flag;
   private boolean lbr_flag;
   private boolean case_flag;
   private boolean label_flag;
   private boolean cmmt_flag;
   private boolean fct_flag;
   private int cma_line;
   private int last_semi;
   private int last_sig;
   private int nest_pos;
   private int string_char;

   DefaultIndenter(int pos,boolean split) {
      current_line = bale_document.findLineNumber(pos);
      if (split) {
         split_line = current_line;
         split_offset = pos;
       }
      else {
         split_line = -1;
         split_offset = -1;
      }
   
      line_length = 0;
      last_sig = -1;
      nest_level = 0;
      string_char = 0;
      cmmt_flag = false;
      eos_first = false;
      nest_pos = -1;
      fct_flag = false;
      case_flag = false;
      label_flag = false;
      rbr_flag = false;
      cma_line = -1;
      lbr_flag = false;
      last_semi = -1;
      init_indent = -1;
   }

   private String getLine() {
      int lno = current_line;
      int soffset = bale_document.findLineOffset(current_line);
      if (soffset < 0) {
         line_text = "";
         line_length = 0;
         return null;
       }
      
      int eoffset = bale_document.findLineOffset(current_line + 1) - 1;
      if (split_line >= 0) {
         if (lno > split_line) {
            lno -= 1;
            if (lno == split_line) {
               soffset = split_offset;
            }
            else {
               soffset = bale_document.findLineOffset(lno);
               eoffset = bale_document.findLineOffset(lno+1)-1;
            }
         }
         else if (lno == split_line) {
            eoffset = split_offset;
         }
      }
     StringBuffer buf = new StringBuffer();
     int pos = 0;
     for (int i = soffset; i < eoffset; ++i) {
        char c = doc_text.charAt(i);
        if (c == '\t') {
           int d = pos % tab_size;
           int ct = tab_size - d;
           for (int j = 0; j < ct; ++j) buf.append(" ");
        }
        else {
           buf.append(c);
        }
     }
     String txt = buf.toString();
     txt = txt.replace("::","XX");
     line_text = txt;
     line_length = txt.length();
   
     return txt;
   }

   private char getChar(int pos) {
      if (pos >= line_length || pos < 0) return 0;
      return line_text.charAt(pos);
   }

   boolean matchInLine(int pos,String txt) {
      int ln = txt.length();
      for (int i = 0; i < ln; ++i) {
         if (txt.charAt(i) != getChar(pos+i)) return false;
       }
   
      return true;
   }

   boolean isLabel(int pos) {
      int fnd = -1;
      for (int i = pos; i < line_length; ++i) {
         char ch = line_text.charAt(i);
         if (Character.isJavaIdentifierPart(ch) || Character.isWhitespace(ch)) ;
         else if (ch == ':') {
            if (i+1 < line_length && line_text.charAt(i+1) == ':') break;
            fnd = i;
            break;
          }
         else break;
       }
      if (fnd > 0) {
         int lno = current_line;
         int soffset = bale_document.findLineOffset(lno)-1;
         while (soffset >= 0) {
            char c = doc_text.charAt(soffset);
            if (!Character.isWhitespace(c)) {
               if (c == '(' || c == ',') {
                  fnd = -1;
                  break;
                }
               if (c == '{' || c == ';') break;
             }
            --soffset;
          }
       }
      
      return fnd > 0;
   }

   boolean isPrototype(int pos) {
      boolean haveid = false;
      boolean inid = false;
   
      for (int i = pos; i < line_length; ++i) {
         char c = getChar(i);
         if (c == 0 || c <= ')' || c == ',') break;
         if (Character.isJavaIdentifierPart(c) || c == '$') {
            if (!inid && haveid) return true;
            inid = true;
            haveid = true;
          }
         else inid = false;
       }
   
      return false;
   }

   // CHECKSTYLE:OFF
   @SuppressWarnings("fallthrough")
   int computeIndent()
   // CHECKSTYLE:ON   
   {
      getLine();
      int x = 0;
   
      // get initial indent
      // TODO -- handle tabs
      for ( ; getChar(x) == ' '; ++x);
      char c = getChar(x);
      if (x < line_length) init_indent = x;
      if (c == '#') return 0;
      if (c == '}') {
         nest_level = 1;
         eos_first = true;
         rbr_flag = true;
       }
      else if (c == '{') lbr_flag = true;
      else if (matchInLine(x,"case ")) case_flag = true;
      else if (matchInLine(x,"default :")) case_flag = true;
      else if (matchInLine(x,"default:")) case_flag = true;
      else if (isLabel(x)) label_flag = true;
   
      x = -1;
      loop: for ( ; ; ) {
         while (x <= 0) {
            int lstrfg = 0;
            --current_line;
            fct_flag = false;
            if (current_line < 0) break loop;
            if (getLine() == null) break loop;
            if (getChar(0) == '#') x = 0;
            else {
               for (x = 0; x < line_length; ++x) {
                  char chx = getChar(x);
                  if (chx == '"' || chx == '\'') {
                     if (x == 0 || getChar(x-1) == '\\') continue;
                     if (lstrfg == 0) lstrfg = chx;
                     else if (lstrfg == chx) lstrfg = 0;
                   }
                  else if (lstrfg == 0 && chx == '/' && getChar(x+1) == '/') break;
                }
             }
          }
         --x;
         c = getChar(x);
         
         if (string_char != 0) {
            if (c == string_char && (x == 0 || getChar(x-1) != '\\')) {
               string_char = 0;
               c = 'X';
             }
            else continue;
          }
         if (cmmt_flag) {
            if (c == '/' && getChar(x+1) == '*') cmmt_flag = false;
            continue;
          }
         if (c == '/' && getChar(x-1) == '*') {
            cmmt_flag = true;
            --x;
            continue;
          }
         
         switch (c) {
            case ' ' :
               break;
            case '"' :
            case '\'' :
               string_char = c;
               break;
               
            case '(' :
               if (last_semi != current_line && getChar(x+1) != ')' &&
                     !isPrototype(x+1)) {
                  fct_flag = true;
                }
               /* FALLTHROUGH */
            case '{' :
               if (last_sig < 0 && last_semi < 0) case_flag = false;
               if (nest_level > 0) {
                  --nest_level;
                  if (nest_level == 0) last_sig = x;
                }
               else if (last_sig < 0 && x > 0) nest_pos = x;
               else if (x == 0 && last_sig < 0) {
                  nest_pos = 0;
                  break loop;
                }
               else {
                  cma_line = -1;
                  break loop;
                }
               break;
               
            case '}' :
               if (nest_level == 0) {
                  if (x == 0) {
                     last_sig = -1;
                     nest_pos = -1;
                     break loop;
                   }
                  if (last_sig >= 0) break loop;
                  eos_first = true;
                }
               ++nest_level;
               break;
               
            case ')' :
               ++nest_level;
               break;
               
            case ':' :
               if (nest_level > 0) break;
               int i = 0;
               while (getChar(i) == ' ') ++i;
               if (matchInLine(i,"case ") ||
                     matchInLine(i,"default ") ||
                     matchInLine(i,"default:")) {
                  while (Character.isJavaIdentifierPart(getChar(i))) ++i;
                  while (getChar(i) == ' ') ++i;
                  if (i == x) x = 0;
                }
               else {
                  if (last_sig < 0) nest_pos = i;
                  last_sig = i;
                  if (cma_line > 0) break;
                  break loop;
                }
               break;
               
            case ';' :
               last_semi = current_line;
               if (nest_level > 0) break;
               if (last_sig >= 0) break loop;
               eos_first = true;
               break;
               
            case ',' :
               if (nest_level == 0) {
                  if (cma_line >= 0 && last_sig >= 0) break loop;
                  cma_line = current_line;
                }
               break;
               
            default :
               if (nest_level > 0) break;
               last_sig = x;
               if (x == 0) {
                  if (fct_flag && unnest_functions) nest_pos = 0;
                  last_sig = -1;
                  if (getChar(0) == '/' && getChar(1) == '*' && nest_pos < 0) {
                     last_sig = init_indent;
                     eos_first = true;
                   }
                  break loop;
                }
               break;
          }
       }
      
      int l4 = indent_space;		// default indent
      int l5 = indent_space;		// indent with no last significant char
      int l6 = rbr_indent;		// indent for }
      
      int rslt = last_sig;
      if (last_sig < 0 && nest_pos < 0) rslt = 0;
      else if (last_sig < 0) rslt = l5;
      else if (nest_pos >= 0) rslt = last_sig + l5;
      else if (!eos_first && (cma_line < 0 || cma_line == current_line)) {
         rslt = last_sig + l4;
       }
      
      if (lbr_flag && rslt == l5) rslt = 0;
      if (rbr_flag && rslt != 0) rslt += l6;
      else if (case_flag) rslt -= l5;
      else if (label_flag) rslt -= l5;
      
      if (rslt < 0) rslt = 0;
      
      return rslt;
   }
	
}


/********************************************************************************/
/*										*/
/*	Property methods							*/
/*										*/
/********************************************************************************/

private void loadProperties()
{
   BoardSetup bs = BoardSetup.getSetup();
   String lang = bs.getLanguage().toString().toLowerCase();

   indent_space = BALE_PROPERTIES.getInt("Bale.indent",4);
   indent_space = BALE_PROPERTIES.getInt("Bale." + lang + ".indent",indent_space);

   tab_size = BALE_PROPERTIES.getInt("Bale.tabsize",8);
   tab_size = BALE_PROPERTIES.getInt("Bale." + lang + ".tabsize",tab_size);

   rbr_indent = BALE_PROPERTIES.getInt("Bale.rbrIndent",0);
   rbr_indent = BALE_PROPERTIES.getInt("Bale." + lang + ".rbrIndent",rbr_indent);

   unnest_functions = BALE_PROPERTIES.getBoolean("Bale.unnestFunctions",true);
   unnest_functions = BALE_PROPERTIES.getBoolean("Bale." + lang + ".unnestFunctions",unnest_functions);

   BumpClient bc = BumpClient.getBump();
   local_split = !bc.getOptionBool("lspbase.lsp.splitIndent",false);
   indent_mode = LineIndentMode.ALL;
   String md = bc.getOption("lspbase.lsp.lineIndent");
   if (md != null) {
      md = md.toLowerCase();
      switch (md) {
         case "all" :
         case "yes" :
            indent_mode = LineIndentMode.ALL;
            break;
         default :
         case "none" :
         case "no" :
            indent_mode = LineIndentMode.NONE;
            break;
         case "empty" :
            indent_mode = LineIndentMode.EMPTY;
            break;
         case "nonempty" :
         case "text" :
         case "non-empty" :
            indent_mode = LineIndentMode.NON_EMPTY;
            break;
       }
    }
}




}	// end of class BaleIndenterLsp




/* end of BaleIndenterLsp.java */

