/********************************************************************************/
/*										*/
/*		BaleDocumentBase.java						*/
/*										*/
/*	Bubble Annotated Language Editor base document				*/
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

import edu.brown.cs.bubbles.board.BoardLog;

import edu.brown.cs.ivy.limbo.LimboFactory;
import edu.brown.cs.ivy.limbo.LimboLine;

import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;
import javax.swing.text.Segment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;



// abstract class BaleDocumentBase extends BaleDocument implements Document, BaleConstants {
abstract class BaleDocumentBase extends BaleHistory.BaleAbstractDocument
	 implements Document, BaleConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/


private WeakHashMap<BalePosition,Object> known_positions;

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BaleDocumentBase()
{
   // known_positions might be used and hence set in super call
   if (known_positions == null) known_positions = new WeakHashMap<>();
}



/********************************************************************************/
/*										*/
/*	Editing methods 							*/
/*										*/
/********************************************************************************/

@Override protected void postRemoveUpdate(AbstractDocument.DefaultDocumentEvent chng)
{
   BaleElementEvent ee = elementRemove(chng.getOffset(),chng.getLength());
   if (ee != null) chng.addEdit(ee);
}



@Override protected void insertUpdate(AbstractDocument.DefaultDocumentEvent chng,AttributeSet attr)
{
   BaleElementEvent ee = elementInsertString(chng.getOffset(),chng.getLength());
   if (ee != null) chng.addEdit(ee);
}




/********************************************************************************/
/*										*/
/*	Global Position management						*/
/*										*/
/********************************************************************************/

@Override public synchronized Position createPosition(int offs) throws BadLocationException
{
   return savePosition(super.createPosition(offs));
}



private Position createInternalPosition(int offs) throws BadLocationException
{
   return super.createPosition(offs);
}



/********************************************************************************/
/*										*/
/*	Local Position management						*/
/*										*/
/********************************************************************************/

@Override public BalePosition savePosition(Position p)
{
   BalePosition bp = super.savePosition(p);

   if (known_positions == null) known_positions = new WeakHashMap<BalePosition,Object>();

   synchronized (known_positions) {
      known_positions.put(bp,Boolean.TRUE);
   }

   return bp;
}



BaleSavedPositions savePositions() throws IOException
{
   SavedPositions sp = new SavedPositions();

   sp.createFile();
   if (known_positions != null) {
      synchronized (known_positions) {
	 for (BalePosition bp : known_positions.keySet()) {
	    sp.addPosition(bp);
	 }
      }
   }
   sp.clearFile();

   return sp;
}



void resetPositions(BaleSavedPositions bsp)
{
   SavedPositions sp = (SavedPositions) bsp;

   sp.createFile();
   sp.updatePositions();
   sp.clearFile();
   sp.finish();
}




private class SavedPositions implements BaleSavedPositions
{
   private File temp_file;
   private int [] line_starts;
   private LimboLine [] limbo_lines;
   private Map<BalePosition,SavePos> position_data;

   SavedPositions() throws IOException {
      temp_file = File.createTempFile("BALE",".txt");
      temp_file.deleteOnExit();
      line_starts = null;
      position_data = new HashMap<BalePosition,SavePos>();
      limbo_lines = null;
    }

   void createFile() {
      int ln = getLength();
      Segment sg = new Segment();
      sg.setPartialReturn(false);
      try {
	 getText(0,ln,sg);
       }
      catch (BadLocationException e) {
	 BoardLog.logE("BALE","Problem getting text for temp file: " + e);
	 return;
       }

      try {
	 FileWriter fw = new FileWriter(temp_file);
	 fw.write(sg.toString());
	 fw.close();
       }
      catch (IOException e) {
	 BoardLog.logE("BALE","Problem creating temp file: " + e);
       }

      boolean haveeol = true;
      List<Integer> lpos = new ArrayList<Integer>();
      for (int i = 0; i < ln; ++i) {
	 if (haveeol) {
	    lpos.add(i);
	    haveeol = false;
	  }
	 if (sg.charAt(i) == '\n') haveeol = true;
       }
      if (haveeol) lpos.add(ln);
      line_starts = new int[lpos.size()];
      if (limbo_lines == null) {
	 limbo_lines = new LimboLine[lpos.size()+1];
	 Arrays.fill(limbo_lines,null);
       }
      int ct = 0;
      for (Integer iv : lpos) {
	 line_starts[ct++] = iv;
       }
    }

   void addPosition(BalePosition bp) {
      int off = bp.getDocumentOffset();
      if (off >= getLength()) {
	 position_data.put(bp,new SavePos(null,-1,off));
       }
      else if (off == 0) {
	 position_data.put(bp,new SavePos(null,0,off));
       }
      else {
	 int idx = Arrays.binarySearch(line_starts,off);
	 int lpos = -1;
	 int cpos = -1;
	 if (idx >= 0) {
	    lpos = idx;
	    cpos = 0;
	  }
	 else {
	    lpos = -idx - 1 - 1;
	    if (lpos < 0) {
	       lpos = 0;
	       cpos = 0;
	     }
	    else {
	       cpos = off - line_starts[lpos];
	     }
	  }
	 LimboLine ln = limbo_lines[lpos];
	 if (ln == null) {
	    ln = LimboFactory.createLine(temp_file,lpos+1);
	    limbo_lines[lpos] = ln;
	  }

	 position_data.put(bp,new SavePos(ln,cpos,off));
       }
      // BoardLog.logD("BALE","SAVE POSN " + bp.hashCode() + " == " + bp + " " + findLineNumber(bp.getOffset()));
    }

   void updatePositions() {
      for (int i = 0; i < limbo_lines.length; ++i) {
	 if (limbo_lines[i] != null) limbo_lines[i].forceValidate();
       }

      for (Map.Entry<BalePosition,SavePos> ent : position_data.entrySet()) {
	 BalePosition bp = ent.getKey();
	 SavePos sp = ent.getValue();

	 LimboLine ll = sp.getLimbo();
	 if (ll == null || ll.getLine() <= 0 || ll.getLine() > line_starts.length) {
	    try {
	       if (bp.getOffset() == 0) bp.reset(createInternalPosition(0));
	       else if (bp.getOffset() < 0) bp.reset(createInternalPosition(getEndPosition().getOffset()));
	       else if (sp.getOffset() == 0) bp.reset(createInternalPosition(0));
	       else if (sp.getOffset() < 0) bp.reset(createInternalPosition(getEndPosition().getOffset()));
	       else {
		  int ln = (ll == null ? -2 : ll.getLine());
		  BoardLog.logX("BALE","Bad reset position " + ln + " " +
				   line_starts.length + " " + sp.getOffset() + " " + bp + " " +
				   getEndPosition());
		}
	     }
	    catch (BadLocationException e) {
	       BoardLog.logE("BALE","Problem updating default position: " + e);
	     }
	    continue;
	  }

	 int ln = ll.getLine();
	 int npos = line_starts[ln-1] + sp.getOffset();
	 // BoardLog.logD("BALE","RESET POSN " + bp.hashCode() + " -> " + npos);
	 try {
	    bp.reset(createInternalPosition(npos));
	  }
	 catch (BadLocationException e) {
	    BoardLog.logE("BALE","Problem updating position: " + e);
	  }
       }
    }

   void clearFile() {
      temp_file.delete();
      line_starts = null;
    }

   void finish() {
      LimboFactory.removeFile(temp_file);
      limbo_lines = null;
    }

}	// end of inner class SavedPositions



private static class SavePos {

   private LimboLine tagged_line;
   private int char_offset;

   SavePos(LimboLine ln,int coff,int prioroff) {
      tagged_line = ln;
      char_offset = coff;
    }

   LimboLine getLimbo() 		{ return tagged_line; }
   int getOffset()			{ return char_offset; }

}	// end of inner class SavePos




}	// end of class BaleDocumentBase




/* end of BaleDocumentBase.java */
