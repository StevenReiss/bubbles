/********************************************************************************/
/*										*/
/*		BaleFragmentContent.java					*/
/*										*/
/*	Bubble Annotated Language Editor Fragment content container		*/
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

import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;
import javax.swing.text.Segment;
import javax.swing.undo.UndoableEdit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;



/**
 *	This class implements the buffer for a fragment.  It actually
 *	implements the interface provided by AbstractDocument.Content
 *	for a fragment composed of multiple regions of an underlying
 *	file.  The file is maintained in the base document.  The set
 *	of regions has to be non-overlapping and ORDERED.  Each region
 *	is separated from the next by a newline.  The newline character
 *	may either be the last character of the region (includesEol()
 *	returnes true for the BaleRegion), or may be implicit and inserted
 *	automatically in calls to this class.  Also, to comply with the
 *	AbstractDocument.Content interface, the buffer includes one
 *	extra new line at the end.
 **/


class BaleFragmentContent implements AbstractDocument.Content, BaleConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

protected BaleDocumentIde base_document;
protected List<BaleRegion> fragment_regions;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleFragmentContent(BaleDocumentIde base,Position soff,Position eoff)
{
   base_document = base;
   fragment_regions = new ArrayList<>();
   soff = base.savePosition(soff);
   eoff = base.savePosition(eoff);
   fragment_regions.add(new BaleRegion(soff,eoff));
}



BaleFragmentContent(BaleDocumentIde base,List<BaleRegion> rgns)
{
   // start offset if first character of fragment
   // end offset is one beyond last character of fragment

   base_document = base;
   fragment_regions = new ArrayList<>(rgns);
}



void dispose()				{ }




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

List<BaleRegion> getRegions()			{ return fragment_regions; }

@Override public int length()
{
   int len = 1; 		// new line at the end

   for (BaleRegion br : fragment_regions) {
      len += getRegionLength(br);
    }

   return len;
}


boolean removeRegions(List<BaleRegion> rgns)
{
   List<BaleRegion> newrgns = new ArrayList<>();
   boolean chng = false;

   for (BaleRegion rgn : fragment_regions) {
      int soff0 = rgn.getStart();
      int eoff0 = rgn.getEnd();
      int soff = soff0;
      int eoff = eoff0;
      for (BaleRegion delrgn : rgns) {
	 int dsoff = delrgn.getStart();
	 int deoff = delrgn.getEnd();
	 if (dsoff <= soff) {				// overlaps start
	    soff = Math.max(soff,deoff);
	  }
	 if (deoff >= eoff) {				// overlaps end
	    eoff = Math.min(eoff,dsoff-1);
	  }
       }
      if (soff >= eoff) {
	 chng = true;
	 continue;
       }
      if (soff0 != soff || eoff0 != eoff) {
	 Position srgn = rgn.getStartPosition();
	 Position ergn = rgn.getEndPosition();
	 try {
	    if (soff0 != soff) srgn = BaleStartPosition.createStartPosition(base_document,soff0);
	    if (eoff0 != eoff) ergn = base_document.createPosition(eoff);
	    String txt = base_document.getText(eoff-1,1);
	    // TODO: take into account indented documents where there might be spaces here
	    rgn = new BaleRegion(srgn,ergn,txt.endsWith("\n"));
	    chng = true;
	  }
	 catch (BadLocationException e) {
	    BoardLog.logE("BALE","Porblem creating shortened fragment: " + e);
	  }
       }
      newrgns.add(rgn);
    }

   if (!chng) return false;

   fragment_regions = newrgns;

   return true;
}



void resetRegions(List<BaleRegion> rgns)
{
   fragment_regions = new ArrayList<BaleRegion>(rgns);
}



void reload()
{
   for (Iterator<BaleRegion> it = fragment_regions.iterator(); it.hasNext(); ) {
      BaleRegion rgn = it.next();
      int soff = rgn.getStart();
      int eoff = rgn.getEnd();
      if (soff >= eoff || soff < 0 || eoff < 0) it.remove();
    }
}


/********************************************************************************/
/*										*/
/*	Methods to convert offsets for fragment and document			*/
/*										*/
/********************************************************************************/

int getDocumentOffset(int off)
{
   return getDocumentOffset(off,false);
}


int getDocumentOffset(int off,boolean edit)
{
   int loff = off;
   BaleRegion lastbr = null;

   for (BaleRegion br : fragment_regions) {
      int blen = getRegionLength(br);
      if (blen < 0) blen = 0;
      if (loff < blen) {
	 int doff = mapViewOffsetToRegion(br,loff,edit) + br.getStart();
	 return doff;
       }
      lastbr = br;
      loff -= blen;
    }

   if (loff <= 1 && lastbr != null) {
      // pointing to extra eol inserted -- really we want to return a BaleStartPosition for this
      return lastbr.getEnd()+loff;
    }
   if (loff == 0 && lastbr == null && fragment_regions.size() > 0) {
      lastbr = fragment_regions.get(0);
      return lastbr.getStart();
    }

   return -1;
}




int getFragmentOffset(int doff)
{
   int toff = 0;

   BaleRegion lrgn = null;

   for (BaleRegion br : fragment_regions) {
      if (doff >= br.getStart() && doff <= br.getEnd()) {
	 int voff = doff - br.getStart();
	 return toff + mapRegionOffsetToView(br,voff);
       }
      toff += getRegionLength(br);
      lrgn = br;
    }

   if (lrgn != null && doff == lrgn.getEnd() + 1) return toff+1;

   return -1;		     // outside of fragment;
}




BaleSimpleRegion getFragmentRegion(int doff,int len)
{
   int toff = 0;
   int start = -1;
   int lused = -1;
   int ltot = 0;
   int epos;

   for (BaleRegion br : fragment_regions) {
      if (doff+len >= br.getStart() && doff < br.getEnd()) {
	 if (doff < br.getStart()) {
	    len -= br.getStart() - doff;		// ignore the part before the segment
	    doff = br.getStart();
	  }
	 int spos = mapRegionOffsetToView(br,doff - br.getStart());
	 if (start < 0) start = spos + toff;
	 if (doff + len < br.getEnd()) {
	    lused = len;
	    epos = mapRegionOffsetToView(br,doff - br.getStart() + len);
	  }
	 else {
	    lused = br.getEnd() - doff;
	    epos = mapRegionOffsetToView(br,br.getEnd() - br.getStart());
	  }
	 doff += lused;
	 len -= lused;
	 ltot += epos - spos;
	 if (len == 0) break;
       }
      toff += getRegionLength(br);
    }

   if (start < 0 || ltot == 0) return null;

   return new BaleSimpleRegion(start,ltot);
}



/********************************************************************************/
/*										*/
/*	Position methods							*/
/*										*/
/********************************************************************************/

@Override public Position createPosition(int off) throws BadLocationException
{
   return new FragPosition(this,off);
}


private Position createBasePosition(int foff) throws BadLocationException
{
   int doff = getDocumentOffset(foff);
   if (doff < 0 && foff > 0) {
      getDocumentOffset(foff);
      BoardLog.logX("BALE","Position outside fragment: " + foff + " " + length());
      throw new BadLocationException("Position outside fragment",foff);
    }
   else if (doff < 0 && foff == 0) {
      StringBuffer buf = new StringBuffer();
      for (BaleRegion br : fragment_regions) {
	 buf.append(br.getStartPosition() + " " + br.getEndPosition() + " " + br.includesEol() + " " +
		       getRegionLength(br) + " ");
       }
      BoardLog.logX("BALE","Bad document contents: " + foff + " " + length() + " " +
		       fragment_regions.size() + " " + buf.toString());
    }

   return base_document.createPosition(doff);
}




/********************************************************************************/
/*										*/
/*	Text methods								*/
/*										*/
/********************************************************************************/

@Override public void getChars(int where,int len,Segment text) throws BadLocationException
{
   Segment ourseg = null;

   int woff = where;
   int wlen = len;
   for (BaleRegion br : fragment_regions) {
      int rln = getRegionLength(br);
      if (woff >= rln) {
	 woff -= rln;
	 continue;
       }
      int rest = rln-woff;
      if (!br.includesEol()) rest++;
      int getln = Math.min(rest,wlen);
      wlen -= getln;
      boolean xfg = getRegionText(br,woff,getln,text);
      if (len == 0) return;		// a request for 0 length actually happens
      woff = 0;

      if (ourseg == null && wlen == 0) {
	 // all from one region
	 if (xfg && text.charAt(len-1) != '\n') {
	    char [] nch = new char[len];
	    System.arraycopy(text.array,text.offset,nch,0,len);
	    nch[len-1] = '\n';
	    text.array = nch;
	    text.offset = 0;
	  }
	 return;
       }

      if (ourseg == null) {
	 ourseg = new Segment();
	 ourseg.array = new char[len];
	 ourseg.offset = 0;
	 ourseg.count = 0;
       }
      System.arraycopy(text.array,text.offset,ourseg.array,ourseg.count,text.count);
      ourseg.count += text.count;
      if (xfg && ourseg.array[ourseg.count-1] != '\n') {
	 ourseg.array[ourseg.count-1] = '\n';
       }
      if (ourseg.count == len) break;
     }

   if (ourseg != null) {
      while (ourseg.count < len) {
	 ourseg.array[ourseg.count++] = '\n';
       }
      text.array = ourseg.array;
      text.count = ourseg.count;
      text.offset = ourseg.offset;
    }
}



@Override public String getString(int where,int len) throws BadLocationException
{
   Segment s = new Segment();
   getChars(where,len,s);

   if (s.array == null) return null;

   return new String(s.array,s.offset,s.count);
}




/********************************************************************************/
/*										*/
/*	Region access methods							*/
/*										*/
/********************************************************************************/

protected int getRegionLength(BaleRegion br)
{
   int len = br.getEnd() - br.getStart();

   if (!br.includesEol()) len += 1;

   return len;
}


protected int mapRegionOffsetToView(BaleRegion br,int offset)
{
   return offset;
}



protected int mapViewOffsetToRegion(BaleRegion br,int offset,boolean edit)
{
   return offset;
}



protected boolean getRegionText(BaleRegion br,int off,int len,Segment text)
	throws BadLocationException
{
   int soff = br.getStart();
   int eoff = br.getEnd();
   int doff = off + soff;
   int dlen = len;

   boolean lastbad = false;
   if (br.includesEol()) {
      // if (doff + dlen > eoff) dlen = eoff - off;
      base_document.getText(doff,len,text);
    }
   else {
      if (off + dlen > eoff) dlen = eoff - off + 1;
      base_document.getText(doff,len,text);
      lastbad = (doff + dlen > eoff);	   // if last character might be bad
    }

   return lastbad;
}



protected String fixInsertionString(String s)
{
   // handle adding initial white space where needed

   return s;
}



String getRealInsertionString(String s)
{
   return s;
}




/********************************************************************************/
/*										*/
/*	Editing methods 							*/
/*										*/
/********************************************************************************/

void handleRemove(int where,int len) throws BadLocationException
{
   int woff = where;
   int wlen = len;

   for (BaleRegion br : fragment_regions) {
      int bln = getRegionLength(br);
      if (woff > bln) {
	 woff -= bln;
	 continue;
       }

      int rlen = wlen;
      if (rlen > bln - woff) rlen = bln - woff;
      // if (rlen > br.getEnd() - woff) rlen = br.getEnd() - woff;

      // get end first in case this causes an edit
      int edoff = mapViewOffsetToRegion(br,woff + rlen,true);
      int sdoff = mapViewOffsetToRegion(br,woff,true);
      // start can cause edit too, so reget the end :: only needed if editing
      // edoff = mapViewOffsetToRegion(br,woff + rlen,true);
      noteBeginEdit();
      base_document.remove(br.getStart() + sdoff,edoff - sdoff);
      wlen -= rlen;
      if (wlen > 0 && !br.includesEol()) wlen -= 1;	// ignore extra char between regions
      // handle the case that a whole region was deleted
      // here we probably want to skip the extra new line in the region
      if (wlen <= 0) break;
      woff = 0;
    }

   if (wlen > 0) throw new BadLocationException("Location outside fragment",where+len);
}




void handleInsert(int where,String text,AttributeSet a) throws BadLocationException
{
   int doff = getDocumentOffset(where,true);
   text = fixInsertionString(text);

   noteBeginEdit();

   base_document.insertString(doff,text,a);

   // TODO: if insertion is at end of a region, make sure the region is adjusted
}



@Override public UndoableEdit insertString(int where,String str)
{
   throw new Error("Shouldn't be called");
}

@Override public UndoableEdit remove(int off,int len)
{
   throw new Error("shouldn't be called");
}


protected void noteBeginEdit()				{ }



/********************************************************************************/
/*										*/
/*	Class to hold a position in the fragment				*/
/*										*/
/********************************************************************************/

private static class FragPosition extends BalePosition {

   private BaleFragmentContent base_content;

   FragPosition(BaleFragmentContent cnt,int off) throws BadLocationException {
      base_content = cnt;
      base_position = cnt.createBasePosition(off);
    }

   @Override public int getOffset() {
      return base_content.getFragmentOffset(base_position.getOffset());
    }

   @Override public int getDocumentOffset() {
      return base_position.getOffset();
    }

   @Override public String toString() {
      return "[" + getOffset() + "@" + base_position.toString() + "]";
    }

}	// end of inner class FragPosition




}	// end of class BaleFragmentContent




/* end of BaleFragmentContent.java */
