/********************************************************************************/
/*										*/
/*		BaleFragmentContentIndent.java					*/
/*										*/
/*	Bubble Annotated Language Editor Fragment indented content container	*/
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



package edu.brown.cs.bubbles.bale;


import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.bump.BumpClient;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;
import javax.swing.text.Segment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 *	This class extends BaleFragmentContent to handle content that is
 *	indented by some minimum amount.
 **/


class BaleFragmentContentIndent extends BaleFragmentContent implements BaleConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private int tab_size;
private int indent_size;
private String indent_string;
private boolean regions_valid;
private DocMonitor doc_monitor;
private Map<BaleRegion,RegionData> region_map;

private static final int	MIN_INDENT = 3;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleFragmentContentIndent(BaleDocumentIde base,Position soff,Position eoff)
{
   super(base,soff,eoff);

   initialize(base);
}



BaleFragmentContentIndent(BaleDocumentIde base,List<BaleRegion> rgns)
{
   super(base,rgns);

   initialize(base);
}




private void initialize(BaleDocumentIde base)
{
   base.baleWriteLock();
   try {
      setupSpacing();
    }
   finally { 
      base.baleWriteUnlock(); 
    }

   doc_monitor = new DocMonitor();
   base_document.addDocumentListener(doc_monitor);
}


@Override void dispose()
{
   base_document.removeDocumentListener(doc_monitor);
}


/********************************************************************************/
/*										*/
/*	Outside update methods							*/
/*										*/
/********************************************************************************/

@Override boolean removeRegions(List<BaleRegion> rgns)
{
   boolean fg = super.removeRegions(rgns);

   if (fg)
      regions_valid = false;

   return fg;
}



@Override void resetRegions(List<BaleRegion> rgns)
{
   super.resetRegions(rgns);

   regions_valid = false;
}



@Override void reload()
{
   super.reload();			// check regions

   base_document.checkWriteLock();
   setupSpacing();
}



/********************************************************************************/
/*										*/
/*	Region access methods							*/
/*										*/
/********************************************************************************/

@Override protected int getRegionLength(BaleRegion br)
{
   int len = br.getEnd() - br.getStart();

   if (!br.includesEol()) len += 1;

   if (region_map != null) {
      validateRegions();
      RegionData rd = region_map.get(br);
      len += rd.getDeltaLength();
    }

   return len;
}


@Override protected int mapRegionOffsetToView(BaleRegion br,int offset)
{
   if (region_map != null) {
      validateRegions();
      RegionData rd = region_map.get(br);
      return rd.getViewOffset(offset);
    }

   return offset;
}



@Override protected int mapViewOffsetToRegion(BaleRegion br,int offset,boolean edit)
{
   if (region_map != null) {
      fixupOffset(br,offset,edit);
      RegionData rd = region_map.get(br);
      return rd.getRegionOffset(br,offset);
    }

   return offset;
}



@Override protected boolean getRegionText(BaleRegion br,int off,int len,Segment text)
	throws BadLocationException
{
   int soff = br.getStart();
   int eoff = br.getEnd();

   if (region_map != null) {
      validateRegions();
      RegionData rd = region_map.get(br);
      Segment rtext = new Segment();
      base_document.getText(soff,eoff-soff,rtext);
      return rd.getText(rtext,off,len,text);
    }

   int doff = off + soff;
   int dlen = len;

   if (doff < 0) return true;

   boolean lastbad = false;
   if (br.includesEol()) {
      if (doff + dlen > eoff) dlen = eoff - off;
      base_document.getText(doff,len,text);
    }
   else {
      if (doff + dlen > eoff) dlen = eoff - off + 1;
      base_document.getText(doff,len,text);
      lastbad = (doff + len > eoff);	   // if last character might be bad
    }

   return lastbad;
}



@Override protected String fixInsertionString(String s)
{
   if (indent_string == null) return s;

   s = s.replace("\n","\n"+indent_string);

   return s;
}



private void fixupOffset(BaleRegion br,int off,boolean edit)
{
   if (edit && region_map != null) {
      validateRegions();
      RegionData rd = region_map.get(br);
      rd.fixupOffset(br,off);
    }
   validateRegions();
}




/********************************************************************************/
/*                                                                              */
/*      Tab methods                                                             */
/*                                                                              */
/********************************************************************************/

@Override int getNextTabPosition(int cpos,BaleTabHandler tbh)
{
   if (indent_string == null) return -1;
   int ind = indent_string.length();
   if (ind == 0) return -1;
   int rslt = tbh.nextTabPosition(cpos+ind);
   rslt -= ind;
   return rslt;
}



/********************************************************************************/
/*										*/
/*	Methods to manage removal of initial space				*/
/*										*/
/********************************************************************************/

private void setupSpacing()
{
   // get the tab_size from appropriate property
   String v = BALE_PROPERTIES.getProperty("indent.tabulation.size");
   if (v == null) v = BumpClient.getBump().getOption("org.eclipse.jdt.core.formatter.tabulation.size");
   if (v == null) v = BALE_PROPERTIES.getProperty("Bale.tabsize");
   tab_size = 8;
   try {
      if (v != null) tab_size = Integer.parseInt(v);
    }
   catch (NumberFormatException e) { }

   region_map = null;
   regions_valid = true;
   indent_size = 0;
   indent_string = null;

   // determine minimum indent
   Segment s = new Segment();
   int minsp = -1;
   for (BaleRegion br : fragment_regions) {
      int soff = br.getStart();
      int eoff = br.getEnd();
      try {
	 base_document.getText(soff,eoff-soff,s);
       }
      catch (BadLocationException e) {
	 BoardLog.logE("BALE","Problem getting region text " + soff + " " + eoff + " " +
			  s.length() + " " + base_document.getLength(),e);
	 // should this be "throw e;"
	 continue;
       }

      int ln = s.length();

      boolean sol = true;
      int ind = 0;
      for (int i = 0; i < ln && (minsp < 0 || minsp >= MIN_INDENT); ++i) {
	 char c = s.charAt(i);
	 if (sol) {
	    switch (c) {
	       case ' ' :
		  ind += 1;
		  break;
	       case '\t' :
		  ind = nextTabPosition(ind);
		  break;
	       case '\n' :
		  ind = 0;
		  break;
	       default :
		  if (minsp < 0 || minsp > ind) minsp = ind;
		  sol = false;
		  break;
	     }
	  }
	 else if (c == '\n') {
	    sol = true;
	    ind = 0;
	  }
       }

      if (minsp >= 0 && minsp < MIN_INDENT) break;
    }

   if (minsp <= 0 || minsp < MIN_INDENT) return;

   indent_size = minsp;
   indent_string = "";
   for (int i = 0; i < minsp; ++i) indent_string += " ";

   region_map = new HashMap<>();
   regions_valid = false;
   for (BaleRegion br : fragment_regions) {
      for (int i = getRegionLength(br)-1; i >= 0; --i) {
	 fixupOffset(br,i,true);
       }
    }
}



private void validateRegions()
{
   if (region_map == null) return;

   synchronized (region_map) {
      if (regions_valid) return;

      Segment s = new Segment();

      region_map.clear();

      for (BaleRegion br : fragment_regions) {
	 RegionData rd = new RegionData(indent_size);
	 region_map.put(br,rd);
	 int soff = br.getStart();
	 int eoff = br.getEnd();
	 if (eoff < soff) continue;
	 try {
	    base_document.getText(soff,eoff-soff,s);
	  }
	 catch (BadLocationException e) {
	    BoardLog.logE("BALE","Problem getting region indentation",e);
	    continue;
	  }
	 int ln = s.length();

	 LineData ld;
	 boolean sol = true;
	 for (int i = 0; i < ln; ++i) {
	    char c = s.charAt(i);
	    if (sol) {
	       ld = computeLineData(i,s);
	       rd.add(ld);
	       sol = false;
	     }
	    if (c == '\n') sol = true;
	  }
	 if (!sol) {
	    ld = computeLineData(ln,s);
	    rd.add(ld);
	    ++ln;
	  }
       }

      regions_valid = true;
    }
}





/********************************************************************************/
/*										*/
/*	Setup the indentation changes per line					*/
/*										*/
/********************************************************************************/

private LineData computeLineData(int offset,Segment s)
{
   int delchar = 0;
   int addchar = 0;
   int indent = 0;
   int ln = s.length();
   boolean havetab = false;

   for (int i = offset; indent < indent_size; ++i) {
      char c = (i < ln ? s.charAt(i) : '\n');
      if (c == ' ') {
	 ++indent;
	 ++delchar;
       }
      else if (c == '\t') {
	 havetab = true;
	 indent = nextTabPosition(indent);
	 if (indent > indent_size) {
	    delchar = (i-offset+1);
	    addchar = indent - indent_size;
	    break;
	  }
	 else {
	    ++delchar;
	  }
       }
      else if (c == '\n') {
	 if (havetab) {
	    delchar = (i-offset);
	    addchar = indent_size;
	  }
	 else {
	    addchar = indent_size - (i-offset);
	  }
	 break;
       }
      else {
	 BoardLog.logE("BALE","Minimum space compute incorrectly " + ln + " " + 
               offset + " " + i + " " + s.toString());
	 break;
       }
    }

   return new LineData(offset,addchar,delchar);
}




/********************************************************************************/
/*										*/
/*	Tab handling								*/
/*										*/
/********************************************************************************/

private int nextTabPosition(int pos)
{
   int npos = (pos + 1 + tab_size);
   int r = npos % tab_size;
   npos -= r;

   return npos;
}



/********************************************************************************/
/*										*/
/*	Hold region data for removing initial spaces				*/
/*										*/
/********************************************************************************/

private class RegionData {

   private int num_lines;
   private LineData [] line_data;

   RegionData(int minsp) {
      line_data = new LineData[128];
      // add(new LineData(-1,0,0));		   // dummy line 0
    }

   int getDeltaLength() {
      int d = 0;
      for (int i = 0; i < num_lines; ++i) {
	 d += line_data[i].getViewDelta();
       }
      return d;
    }

   int getViewOffset(int off) {
      // return offset in view given offset in region
      int d = 0;
      for (int i = 0; i < num_lines; ++i) {
	 if (off < line_data[i].getOffset() + line_data[i].getDelete()) break;
	 d += line_data[i].getViewDelta();
       }
      return off + d;
    }

   int getRegionOffset(BaleRegion br,int off) {
      // get offset in region given offset in view]
      if (num_lines == 0) return 0;
      int spos = 0;
      int line = 0;
      for (int i = 1; i < num_lines; ++i) {
	 int npos = spos + line_data[i].getOffset() - line_data[i-1].getOffset() +
	    line_data[i-1].getViewDelta();
	 if (off < npos) break;
	 spos = npos;
	 line = i;
       }
      int delta = off - spos - line_data[line].getAdd();
      if (delta < 0) delta = 0; 	// inside virtual space
      return line_data[line].getOffset() + line_data[line].getDelete() + delta;
    }

   void fixupOffset(BaleRegion br,int off) {
      // get offset in region given offset in view]
      if (num_lines == 0) return;
      int spos = 0;
      int line = 0;
      for (int i = 1; i < num_lines; ++i) {
	 int npos = spos + line_data[i].getOffset() - line_data[i-1].getOffset() +
	    line_data[i-1].getViewDelta();
	 if (off < npos) break;
	 spos = npos;
	 line = i;
       }
      int delta = off - spos - line_data[line].getAdd();
      if (delta >= 0) return;

      int off0 = line_data[line].getOffset() + br.getStart();
      int len0 = line_data[line].getDelete();
      int len1 = line_data[line].getAdd();
      if (len0 != 0) len1 += indent_size;
      try {
	 if (len1 > 0) {
	    String s = "";
	    for (int i = 0; i < len1; ++i) s += " ";
	    base_document.insertString(off0,s,null);
	  }
	 if (len0 > 0) base_document.remove(off0+len1,len0);
       }
      catch (BadLocationException e) {
	 BoardLog.logE("BALE","Problem fixing line indent",e);
	 delta = 0;
       }
      regions_valid = false;
    }

   boolean getText(Segment brtext,int voff,int len,Segment s) {
      s.array = new char[len+1];
      s.count = 0;
      s.offset = 0;

      if (len == 0) return false;

      int spos = 0;
      int line = 0;

      // first find the line where the data starts
      for (int i = 1; i < num_lines; ++i) {
	 int npos = spos + line_data[i].getOffset() - line_data[i-1].getOffset() +
	    line_data[i-1].getViewDelta();
	 if (voff < npos) break;
	 spos = npos;
	 line = i;
       }

      // now add the data
      int pos = spos;
      for (int i = line+1; i < num_lines; ++i) {
	 int rpos = line_data[i-1].getOffset();
	 rpos += line_data[i-1].getDelete();
	 int npos = spos + line_data[i].getOffset() - line_data[i-1].getOffset() +
	    line_data[i-1].getViewDelta();
	 int addct = (line_data[i-1].getDelete() > 0 ? line_data[i-1].getAdd() : 0);
	 while (pos < npos) {
	    char c;
	    if (pos-spos < addct) c = ' ';
	    else c = brtext.charAt(rpos + pos - spos - addct);
	    if (pos >= voff) s.array[s.count++] = c;
	    if (s.count == len) break;
	    ++pos;
	  }
	 spos = npos;
	 if (s.count == len) break;
       }

      boolean lastbad = false;

      if (s.count < len && num_lines > 0) {
	 if (pos < voff) pos = voff;
	 int rpos = line_data[num_lines-1].getOffset();
	 rpos += line_data[num_lines-1].getDelete();
	 int addct = line_data[num_lines-1].getAdd();
	 while (s.count < len) {
	    char c;
	    int idx = rpos + pos - spos -addct;
	    if (pos-spos < addct) c = ' ';
	    else if (idx < brtext.length()) c = brtext.charAt(idx);
	    else {
	       lastbad = true;
	       c = '\n';
	     }
	    s.array[s.count++] = c;
	    ++pos;
	  }
       }
      else if (s.count < len) {
	 while (s.count < len) {
	    s.array[s.count++] = '\n';
	  }
	 lastbad = true;
       }

      return lastbad;
    }

   void add(LineData ld) {
      grow(num_lines+1);
      line_data[num_lines++] = ld;
    }

   private void grow(int max) {
      int sz = line_data.length;
      if (sz > max) return;
      while (sz < max) sz *= 2;
      line_data = Arrays.copyOf(line_data,sz);
    }

}	// end of inner class RegionData



/********************************************************************************/
/*										*/
/*	Line data: information on what needs to be changed per line		*/
/*										*/
/********************************************************************************/

private static class LineData {

   private int delete_chars;		// initial characters to delete
   private int add_chars;		// spaces to add
   private int line_offset;		// offset of the line

   LineData(int offset,int add,int del) {
      line_offset = offset;
      add_chars = add;
      delete_chars = del;
    }

   int getOffset()			{ return line_offset; }
   int getAdd() 			{ return add_chars; }
   int getDelete()			{ return delete_chars; }

   int getViewDelta() {
      if (delete_chars == 0) return 0;
      return add_chars - delete_chars;
    }

}	// end of inner class LineData



/********************************************************************************/
/*										*/
/*	Position methods							*/
/*										*/
/********************************************************************************/

@Override public Position createPosition(int off) throws BadLocationException
{
   return createBalePosition(off);
}



private Position createBalePosition(int off) throws BadLocationException
{
   if (region_map == null) {
      return super.createPosition(off);
    }

   int loff = off;
   int blen = 0;
   BaleRegion lastbr = null;

   for (BaleRegion br : fragment_regions) {
      blen = getRegionLength(br);
      if (loff < blen) return new IndentPosition(br,loff);
      loff -= blen;
      lastbr = br;
    }

   if (loff == 0) {
      return new IndentPosition(lastbr,blen);
    }
   else if (loff == 1) {
      return new IndentPosition(lastbr,blen+1);
   }

   throw new BadLocationException("Location outside of fragment",off);
}



private class IndentPosition extends BalePosition {

   private int local_offset;

   IndentPosition(BaleRegion br,int off) throws BadLocationException {
      RegionData rd = region_map.get(br);
      validateRegions();
      int roff = rd.getRegionOffset(br,off);
      int voff = rd.getViewOffset(roff);
      local_offset = off - voff;
      base_position = base_document.createPosition(br.getStart() + roff);
    }

   @Override public int getOffset() {
      int offset = getFragmentOffset(base_position.getOffset());
      return offset + local_offset;
    }

   @Override public int getDocumentOffset() {
      return base_position.getOffset();
    }

   @Override public String toString() {
      return "[" + getOffset() + "@" + base_position.toString() + "/" + local_offset + "]";
    }

}	// end of inner class FragPosition




/********************************************************************************/
/*										*/
/*	Monitor the base document						*/
/*										*/
/********************************************************************************/

@Override protected void noteBeginEdit()
{
   regions_valid = false;
}



private final class DocMonitor implements DocumentListener {

   @Override public void changedUpdate(DocumentEvent e) 	{ }

   // these might be smarter and only invalidate if needed
   
   @Override public void insertUpdate(DocumentEvent e) {
      if (region_map != null) regions_valid = false;
    }

   @Override public void removeUpdate(DocumentEvent e) {
      if (region_map != null) regions_valid = false;
    }

}	// end of inner class DocMonitor



}	// end of class BaleFragmentContentIndent




/* end of BaleFragmentContentIndent.java */











