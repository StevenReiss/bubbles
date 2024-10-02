/********************************************************************************/
/*										*/
/*		BaleCompactFragment						*/
/*										*/
/*	Bubble Annotated Language Editor fragment for bubbles stack		*/
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

import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardFont;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.bubbles.buss.BussConstants;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.swing.SwingRoundedCornerLabel;

import javax.swing.JLabel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Segment;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


class BaleCompactFragment extends JLabel implements BaleConstants, BussConstants {



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private BaleDocument base_document;
private String	item_name;
private transient Map<Integer,LineData> line_data;
private int title_width;
private int content_width;
private transient List<ContentData> content_data_list;

private SwingRoundedCornerLabel rounded_corner_label = null;

private final static long serialVersionUID = 1;

private final static String HIGHLIGHT_START = "<span style='background:$(HILITE)'>";
private final static String HIGHLIGHT_END = "</span>";
private final static Font COMPACT_FONT = BoardFont.getFont(Font.MONOSPACED,Font.PLAIN,9);
private final static String COMPACT_FONT_PROP = "Bale.compact.font";

private static int INIT_HEIGHT = 22;

private static final int ELLIPSIS_LENGTH = getStringWidth("...");



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleCompactFragment(BaleDocument bd,Collection<BumpLocation> locs,int wd)
{
   base_document = bd;

   Font ft = BALE_PROPERTIES.getFont(COMPACT_FONT_PROP,COMPACT_FONT);
   setFont(ft);

   item_name = getTitle(locs);
   line_data = new TreeMap<Integer,LineData>();
   title_width = wd;

   for (BumpLocation bl : locs) {
      switch (bl.getSymbolType()) {
	 case CLASS :
	 case INTERFACE :
	 case ENUM :
	 case THROWABLE :
	 case MODULE :
	    if (bl.getDefinitionOffset() == bl.getOffset() &&
	       bl.getDefinitionEndOffset() == bl.getEndOffset()) {
	       // add dummy line
	       // continue;
	    }
	    break;
	 default:
	    break;
       }

      int soff = bd.mapOffsetToJava(bl.getOffset());
      int eoff = bd.mapOffsetToJava(bl.getEndOffset());
      int slno = bd.findLineNumber(soff);
      int elno = bd.findLineNumber(eoff);
      if (slno != elno) {
	 eoff = bd.findLineOffset(slno+1) - 2;
	 elno = slno;
       }
      if (eoff <= soff) {
	 BoardLog.logW("BALE","Match had no text " + soff + " " + eoff);
	 continue;
       }

      LineData ld = line_data.get(slno);
      if (ld == null) {
	 ld = new LineData(slno);
	 line_data.put(slno,ld);
       }
      ld.addHighlight(soff,eoff);
    }

   setLayout(null);
}




/********************************************************************************/
/*										*/
/*	External setup/access methods						*/
/*										*/
/********************************************************************************/

void init(int contentWidth)
{
   content_data_list = new ArrayList<ContentData>();
   content_width = contentWidth;

   initContentData();
   setupLabel();

   setSize(rounded_corner_label.getSize());
   setPreferredSize(getSize());
}




private static int getStringWidth(String s)
{
   JLabel label = new JLabel("<html><font size='10pt' face='monospace'>" + htmlEncode(s) + "</font></html>");
   Dimension d = label.getPreferredSize();
   return d.width;
}



/********************************************************************************/
/*										*/
/*	Methods to create HTML for a fragment					*/
/*										*/
/********************************************************************************/

private void initContentData()
{
   for (LineData ld : line_data.values()) {
      int lno = ld.getLineNumber();
      int soff = base_document.findLineOffset(lno);
      int eoff = base_document.findLineOffset(lno+1);
      Segment s = new Segment();
      try {
	 base_document.getText(soff,eoff-soff,s);

	 int index = 0;
	 List<String> strList = new ArrayList<String>();

	 for (BaleSimpleRegion bsr : ld.getHighlights()) {
	    int hoff = bsr.getStart() - soff;

	    if (index >= s.length()) continue;
	    if (hoff >= s.length()) hoff = s.length();
	    if (hoff < index) continue;

	    strList.add(s.subSequence(index, hoff).toString());
	    strList.add(s.subSequence(hoff, hoff + bsr.getLength()).toString());

	    index = hoff + bsr.getLength();
	 }

	 strList.add(s.subSequence(index, eoff - soff).toString());

	 String[] aryStr = new String[strList.size()];
	 strList.toArray(aryStr);

	 ContentData contentData = new ContentData(content_width, aryStr);
	 content_data_list.add(contentData);
       }
       catch (BadLocationException e) { }
    }
}



private void setupLabel()
{
   if(rounded_corner_label != null)
      remove(rounded_corner_label);

   StringBuffer buf = new StringBuffer();

   buf.append("<html>");
   buf.append("<table border='0'>");
   buf.append("<tr>");

   item_name = trimString(item_name, title_width);

   int ctr = 0;

   for (ContentData contentData : content_data_list) {
      if (ctr++ == 0) {
	 buf.append("<th");
	 buf.append(" width='" + title_width + "' align='LEFT'><font size='10pt' face='monospace'>");
	 buf.append(item_name);
	 buf.append("</font></th>");
       }
      else {
	 buf.append("<th></td>");
       }

      buf.append(contentData.getResultHtml());
    }

   buf.append("</table>");
   buf.append("</html>");

   int[] aryseparateloc = new int[1];
   aryseparateloc[0] = title_width + 1;

   int[] aryseparateheight;
   if (line_data.size() > 1) aryseparateheight = new int[line_data.size() - 1];
   else aryseparateheight = new int[0];

   for (int i=0; i < aryseparateheight.length; i++) {
      aryseparateheight[i] = (i + 1) * INIT_HEIGHT;
    }

   Color tc = BoardColors.getColor(BUSS_STACK_TOP_COLOR_PROP);
   Color bc = BoardColors.getColor(BUSS_STACK_BOTTOM_COLOR_PROP);
   rounded_corner_label = new SwingRoundedCornerLabel(tc,bc,aryseparateloc,aryseparateheight,1);
   rounded_corner_label.setFont(BoardFont.getFont(Font.MONOSPACED,Font.PLAIN,10));
   rounded_corner_label.setSize(title_width + content_width + 1, line_data.size() * INIT_HEIGHT);
   rounded_corner_label.setText(buf.toString());

   add(rounded_corner_label);
}



private static String htmlEncode(String s)
{
   String resultString = s.replace("<", "&lt;");
   resultString = resultString.replace(">", "&gt;");

   return resultString;
}



/********************************************************************************/
/*										*/
/*	Methods to fix up strings for the display				*/
/*										*/
/********************************************************************************/

private String trimString(String s, int length)
{
   int sLength = getStringWidth(s);

   if (sLength < length) return s;

   length -= ELLIPSIS_LENGTH;

   StringBuffer sb = new StringBuffer(s.substring(0, (int)(s.length() * (double) length / sLength)));
   sb.append("...");

   return sb.toString();
}



private int[] computeCodeSegementOffset(String strBeforeHighlight, String firstHighlightStr,
					   String strAfterHighlight, int length)
{
   int[] aryOffset = new int[2];

   int sLength = getStringWidth(firstHighlightStr);

   if (sLength > length) {
      length -= ELLIPSIS_LENGTH;

      aryOffset[0] = strBeforeHighlight.length();
      aryOffset[1] = (int)((1 - (double) length / sLength) * firstHighlightStr.length()) + strAfterHighlight.length();

      return aryOffset;
    }

   int beforeLength = (length - sLength) / 2;
   int afterLength = (length - sLength) / 2;

   int beforeSLength = getStringWidth(strBeforeHighlight);
   int afterSLength = getStringWidth(strAfterHighlight);

   if (afterSLength > beforeSLength) {
      if (beforeSLength > beforeLength) {
	 beforeLength -= ELLIPSIS_LENGTH;
	 aryOffset[0] = (int)(strBeforeHighlight.length() * (1 - (double) beforeLength / beforeSLength));
       }
      else {
	 aryOffset[0] = 0;
	 afterLength += (beforeLength - beforeSLength);
       }

      if (afterSLength > afterLength) {
	 afterLength -= ELLIPSIS_LENGTH;
	 aryOffset[1] = (int)(strAfterHighlight.length() * (1 - (double) afterLength / afterSLength));
       }
      else {
	 aryOffset[1] = 0;
       }
    }
   else {
      if (afterSLength > afterLength) {
	 afterLength -= ELLIPSIS_LENGTH;
	 aryOffset[1] = (int)(strAfterHighlight.length() * (1 - (double) afterLength / afterSLength));
       }
      else {
	 aryOffset[1] = 0;
	 beforeLength += (afterLength - afterSLength);
       }

      if (beforeSLength > beforeLength) {
	 beforeLength -= ELLIPSIS_LENGTH;
	 aryOffset[0] = (int)(strBeforeHighlight.length() * (1 - (double) beforeLength / beforeSLength));
       }
      else {
	 aryOffset[0] = 0;
       }
    }

   return aryOffset;
}



/********************************************************************************/
/*										*/
/*	Title Width Computation 						*/
/*										*/
/********************************************************************************/

private static String getTitle(Collection<BumpLocation> locs)
{
   String nm = null;

   for (BumpLocation bl : locs) {
      nm = bl.getSymbolName();
      if (nm == null) return "File";
      int idx0 = nm.lastIndexOf('.');
      nm = nm.substring(idx0+1);
      if (nm.equals("<clinit>")) return "Static Initializer";
      String p = bl.getParameters();
      if (p != null) nm += simplifyParameters(p);
      break;
    }

   return nm;
}



private static String simplifyParameters(String p)
{
   if (p == null) return null;

   int lvl = 0;
   int n = p.length();
   StringBuffer buf = new StringBuffer();
   int start = -1;

   for (int i = 0; i < n; ++i) {
      char ch = p.charAt(i);
      if (lvl == 0) {
	 if (ch == ')' || ch == ',') {
	    if (start > 0) {
	       addParam(p.substring(start,i),buf);
	       start = -1;
	     }
	    buf.append(ch);
	  }
	 else if (ch == '(') {
	    buf.append(ch);
	  }
	 else if (ch == '<') ++lvl;
	 else if (start < 0) start = i;
       }
      else if (ch == '<') ++lvl;
      else if (ch == '>') --lvl;
    }

   return buf.toString();
}



private static void addParam(String p,StringBuffer buf)
{
   int eidx = p.indexOf('<');
   if (eidx > 0) p = p.substring(0,eidx);
   int sidx = p.lastIndexOf('.');
   if (sidx > 0) p = p.substring(sidx+1);

   buf.append(p);
}




/********************************************************************************/
/*										*/
/*	Class to hold information about lines to display			*/
/*										*/
/********************************************************************************/

private static class LineData {

   private int line_number;
   private List<BaleSimpleRegion> line_highlights;

   LineData(int lno) {
      line_number = lno;
      line_highlights = new ArrayList<>();
    }

   void addHighlight(int soff,int eoff) {
      int idx = 0;
      for (int i = 0; i < line_highlights.size(); ++i) {
	 BaleSimpleRegion br = line_highlights.get(i);
	 if (br.getStart() > soff) break;
	 ++idx;
       }
      line_highlights.add(idx,new BaleSimpleRegion(soff,eoff-soff));
    }

   int getLineNumber()				{ return line_number; }
   List<BaleSimpleRegion> getHighlights()	{ return line_highlights; }

}	// end of inner class LineData



/********************************************************************************/
/*										*/
/*	Class to hold the content						*/
/*										*/
/********************************************************************************/

private class ContentData {

   private int local_width;

   private String[] ary_str;

   private String before_highlight_str;
   private String first_highlight_str;
   private String after_highlight_str;

   private String result_html;

   ContentData(int contentwidth, String[] arystr) {
      ary_str = arystr;
      String firststr = ary_str[0] + "A";
      String laststr = "A" + ary_str[ary_str.length - 1];
      ary_str[0] = firststr.trim();
      ary_str[0] = firststr.substring(0, firststr.length() - 1);
      ary_str[ary_str.length - 1] = laststr.trim().substring(1);
      before_highlight_str = ary_str[0];
      if (ary_str.length > 1) first_highlight_str = ary_str[1];
      else first_highlight_str = "";
      StringBuffer afterhighlightstrbuf = new StringBuffer();

      for(int i = 2;i<ary_str.length;i++) {
	 afterhighlightstrbuf.append(ary_str[i]);
       }

      after_highlight_str = afterhighlightstrbuf.toString();
      updateWidth(contentwidth);
    }

   private void updateWidth(int contentWidth) {
      local_width = contentWidth;

      int[] aryoffset = computeCodeSegementOffset(before_highlight_str, first_highlight_str, after_highlight_str, (int) (local_width * 0.95));

      generateHTML(aryoffset[0], before_highlight_str.length() + first_highlight_str.length() + after_highlight_str.length() - aryoffset[1]);
   }

   private void generateHTML(int startIndex, int endIndex) {
      StringBuffer htmlBuf = new StringBuffer("<td height='" + INIT_HEIGHT + "'><font size='10pt' face='monospace'>");
      // StringBuffer htmlBuf = new StringBuffer("<td height='" + INIT_HEIGHT + "'><code>");
      StringBuffer middleHtmlBuf = new StringBuffer();
      
      Map<String,String> subs = new HashMap<>();
      subs.put("HILITE",BoardColors.toHtmlColor("Bale.compact.hilite"));
      
      String hilitestart = IvyFile.expandText(HIGHLIGHT_START,subs);
   
      int index = 0;
      int currentLoc = 0;
   
      int startAryIndex = 0;
      int endAryIndex = 0;
   
      int startStrIndex = 0;
      int endStrIndex = 0;
   
      for ( ; index < ary_str.length; index++) {
         if (startIndex >= currentLoc && startIndex < (currentLoc + ary_str[index].length())){
            startAryIndex = index;
            startStrIndex = startIndex - currentLoc;
            break;
          }
   
         currentLoc += ary_str[index].length();
       }
   
      for ( ; index < ary_str.length; index++) {
         if (endIndex >= currentLoc && endIndex <= (currentLoc + ary_str[index].length())){
            endAryIndex = index;
            endStrIndex = endIndex - currentLoc;
            break;
          }
   
         currentLoc += ary_str[index].length();
   
         if (index == startAryIndex) continue;
   
         if ((index & 1) == 1) {
            middleHtmlBuf.append(hilitestart);
            middleHtmlBuf.append(htmlEncode(ary_str[index]));
            middleHtmlBuf.append(HIGHLIGHT_END);
          }
         else{
            middleHtmlBuf.append(htmlEncode(ary_str[index]));
          }
       }
   
      if(startIndex > 0) htmlBuf.append("...");
   
      if (startAryIndex == endAryIndex) {
         if ((startAryIndex % 2) == 1) {
            htmlBuf.append(hilitestart);
            htmlBuf.append(htmlEncode(ary_str[startAryIndex].substring(startStrIndex, endStrIndex)));
            htmlBuf.append(HIGHLIGHT_END);
          }
         else {
            htmlBuf.append(htmlEncode(ary_str[startAryIndex]));
          }
       }
      else{
         if ((startAryIndex % 2) == 1) {
            htmlBuf.append(hilitestart);
            htmlBuf.append(htmlEncode(ary_str[startAryIndex].substring(startStrIndex)));
            htmlBuf.append(HIGHLIGHT_END);
          }
         else {
            htmlBuf.append(htmlEncode(ary_str[startAryIndex].substring(startStrIndex)));
          }
   
         htmlBuf.append(middleHtmlBuf);
   
         if ((endAryIndex % 2) == 1) {
            htmlBuf.append(hilitestart);
            htmlBuf.append(htmlEncode(ary_str[endAryIndex].substring(0, endStrIndex)));
            htmlBuf.append(HIGHLIGHT_END);
          }
         else{
            htmlBuf.append(htmlEncode(ary_str[endAryIndex].substring(0, endStrIndex)));
          }
       }
   
      if (endAryIndex < ary_str.length - 1 ||
             endStrIndex < (ary_str[ary_str.length - 1].length() - 1))
         htmlBuf.append("...");
   
      htmlBuf.append("</font></td></tr>");
      // htmlBuf.append("</code></td></tr>");
   
      result_html = htmlBuf.toString();
    }

   String getResultHtml()			{ return result_html; }

}	// end of inner class ContentData


}	// end of class BaleCompactFragment



/* end of BaleCompactFragment.java */
