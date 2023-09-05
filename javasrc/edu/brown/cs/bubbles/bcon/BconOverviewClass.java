/********************************************************************************/
/*										*/
/*		BconOverviewClass.java						*/
/*										*/
/*	Bubbles Environment Context Viewer class information for overview	*/
/*										*/
/********************************************************************************/
/*	Copyright 2010 Brown University -- Steven P. Reiss		      */
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


/* SVI: $Id$ */



package edu.brown.cs.bubbles.bcon;

import edu.brown.cs.bubbles.bale.BaleConstants;
import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Segment;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;



class BconOverviewClass implements BconConstants
{



/********************************************************************************/
/*										*/
/*	Private storage 							*/
/*										*/
/********************************************************************************/

private String			for_class;
private File			for_file;
private BaleConstants.BaleFileOverview bale_file;
private BumpClient		bump_client;
private boolean 		lines_valid;
private Collection<BconRegion>	region_set;
private ArrayList<BconLine>	line_set;
private boolean 		is_vertical;




/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BconOverviewClass(BconOverviewPanel pnl,String proj,File f,String cls,boolean vert)
{
   for_class = cls;
   for_file = f;
   is_vertical = vert;

   lines_valid = false;

   bale_file = null;
   bale_file = BaleFactory.getFactory().getFileOverview(proj,f);

   setupLines();

   region_set = new ArrayList<>();
   bump_client = BumpClient.getBump();

   addAllRegions(proj,cls);

   // set up region properties
   for (BudaBubble bb : pnl.getActiveBubbles()) {
      File bf = bb.getContentFile();
      if (bf == for_file) {
	 Collection<BconRegion> rgns = findRegions(bb.getContentType(),bb.getContentName());
	 if (rgns != null) {
	    for (BconRegion br : rgns) {
	       br.setHasBubble(true);
	       if (bb.hasFocus()) br.setHasFocus(true);
	     }
	  }
       }
    }

   bale_file.addDocumentListener(new FileListener());
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BconRegion findRegion(BudaConstants.BudaContentNameType typ,String name)
{
   if (name == null) return null;

   for (BconRegion br : region_set) {
      boolean ok = true;
      switch (br.getRegionType()) {
	 case REGION_UNKNOWN :
	    ok = false;
	    break;
	 case REGION_CLASS :
	    ok = (typ == BudaConstants.BudaContentNameType.CLASS);
	    break;
	 case REGION_FIELD :
	    ok = (typ == BudaConstants.BudaContentNameType.FIELD);
	    break;
	 case REGION_CONSTRUCTOR :
	 case REGION_METHOD :
	    ok = (typ == BudaConstants.BudaContentNameType.METHOD);
	    break;
	 case REGION_INITIALIZER :
	    ok = (typ == BudaConstants.BudaContentNameType.CLASS_ITEM);
	    break;
	 default:
	    break;
       }
      if (!ok) continue;

      if (br.nameMatch(name)) return br;

      String nm = br.getRegionName();
      if (nm.equals(name)) return br;
      else if (nm.replace("$",".").equals(name)) return br;
    }

   return null;
}



Collection<BconRegion> findRegions(BudaConstants.BudaContentNameType typ,String name)
{
   if (name == null) return null;

   Collection<BconRegion> rslt = new ArrayList<>();

   for (BconRegion br : region_set) {
      boolean ok = true;
      boolean outer = false;
      boolean nameok = false;
      switch (br.getRegionType()) {
	 case REGION_UNKNOWN :
	    ok = false;
	    break;
	 case REGION_CLASS :
	    ok = (typ == BudaConstants.BudaContentNameType.CLASS);
	    break;
	 case REGION_FIELD :
	    ok = (typ == BudaConstants.BudaContentNameType.FIELD);
	    if (typ == BudaConstants.BudaContentNameType.CLASS_ITEM && name.endsWith("< FIELDS >")) {
	       ok = true;
	       nameok = true;
	     }
	    break;
	 case REGION_CONSTRUCTOR :
	 case REGION_METHOD :
	    ok = (typ == BudaConstants.BudaContentNameType.METHOD);
	    break;
	 case REGION_INITIALIZER :
	    ok = (typ == BudaConstants.BudaContentNameType.CLASS_ITEM);
	    break;
	 default:
	    break;
       }

      switch (typ) {
	 case CLASS :
	    ok = true;
	    outer = true;
	    break;
	 case FILE :
	    ok = true;
	    nameok = true;
	    break;
	 default:
	    break;
       }

      if (!ok) continue;

      String nm = br.getRegionName();
      if (nameok || br.nameMatch(name)) rslt.add(br);
      else if (nm.equals(name)) rslt.add(br);
      else if (nm.replace("$",".").equals(name)) rslt.add(br);
      else if (outer && nm.startsWith(name + ".")) rslt.add(br);
      else if (outer && nm.startsWith(name + "$")) rslt.add(br);
    }

   return rslt;
}



int getLineCount()
{
   setupLines();

   return line_set.size();
}



String getClassName()			{ return for_class; }




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

private void addAllRegions(String proj,String cls)
{
   List<BumpLocation> locs = bump_client.findAllDeclarations(proj,null,cls,false);
   if (locs != null) {
      for (BumpLocation bl : locs) {
	 BconRegion br = new BconRegionLocation(bale_file,bl);
	 if (br.getRegionType() != RegionType.REGION_UNKNOWN) region_set.add(br);
	 if (br.getRegionType() == RegionType.REGION_CLASS) {
	    addAllRegions(proj,br.getRegionName());
	  }
       }
    }
}



/********************************************************************************/
/*										*/
/*	Action methods								*/
/*										*/
/********************************************************************************/

void handleMousePress(MouseEvent e,int ln)
{
   BconRegion rgn = findRegionForLine(ln);

   if (e.getButton() == MouseEvent.BUTTON1 && rgn != null) {
      rgn.createBubble((Component) e.getSource());
    }
}



private BconRegion findRegionForLine(int ln)
{
   //TODO: if the regions are ordered by start line, then a start line > ln
   //	can end the search

   BconRegion rgn = null;
   int delta = -1;
   for (BconRegion br : region_set) {
      if (ln >= br.getStartLine() && ln <= br.getEndLine()) {
	 int d0 = br.getEndLine() - br.getStartLine();
	 if (rgn == null || d0 < delta) {
	    rgn = br;
	    delta = d0;
	  }
       }
    }

   return rgn;
}




/********************************************************************************/
/*										*/
/*	Methods to set up internal structures					*/
/*										*/
/********************************************************************************/

private void setupLines()
{
   if (lines_valid) return;

   line_set = new ArrayList<>();

   Segment s = new Segment();
   try {
      bale_file.getText(0,bale_file.getLength(),s);
    }
   catch (BadLocationException e) {
      BoardLog.logE("BCON","Bad location accessing file: " + e);
    }

   int pos = 0;
   LineType lt = LineType.LINE_UNKNOWN;
   int ln = s.length();
   for (int i = 0; i < ln; ++i) {
      char c = s.charAt(i);
      if (c == '\n') {
	 if (lt == LineType.LINE_UNKNOWN) lt = LineType.LINE_EMPTY;
	 line_set.add(new BconLine(lt,pos));
	 pos = 0;
	 lt = LineType.LINE_UNKNOWN;
       }
      else if (c == '\t') {
	 pos = (pos + 8) & ~7;
       }
      else if (Character.isWhitespace(c)) pos += 1;
      else {
	 if (lt == LineType.LINE_UNKNOWN) {
	    if (c == '/' && (s.charAt(i+1) == '/' || s.charAt(i+1) == '*')) {
	       lt = LineType.LINE_COMMENT;
	     }
	    else lt = LineType.LINE_CODE;
	  }
	 ++pos;
       }
    }

   lines_valid = true;
}




/********************************************************************************/
/*										*/
/*	Painting methods							*/
/*										*/
/********************************************************************************/

void paint(Graphics2D g,double x0,double y0,double wd,double ht)
{
   Rectangle2D.Double r = new Rectangle2D.Double();

   Color focuscolor = BoardColors.getColor(BCON_FOCUS_COLOR_PROP);
   Color existscolor = BoardColors.getColor(BCON_EXISTS_COLOR_PROP);
   Color notexistscolor = BoardColors.getColor(BCON_NOTEXISTS_COLOR_PROP);
   Color codecolor = BoardColors.getColor(BCON_CODE_COLOR_PROP);
   Color cmmtcolor = BoardColors.getColor(BCON_COMMENT_COLOR_PROP);

   for (BconRegion br : region_set) {
      int ln0 = br.getStartLine()-1;
      int ln1 = br.getEndLine()-1;
      setRectangle(r,ln0,ln1,x0,y0,wd,ht);
      if (br.hasFocus()) g.setColor(focuscolor);
      else if (br.hasBubble()) g.setColor(existscolor);
      else g.setColor(notexistscolor);
      g.fill(r);
    }

   int idx = 0;
   for (BconLine ld : line_set) {
      double len = ld.getLineLength();
      len /= 80.0;
      if (len > 0) {
	 if (len > 1) len = 1;
	 setLine(r,idx,len,x0,y0,wd,ht);
	 switch (ld.getLineType()) {
	    case LINE_UNKNOWN :
	    case LINE_EMPTY :
	    case LINE_CODE :
	       g.setColor(codecolor);
	       break;
	    case LINE_COMMENT :
	       g.setColor(cmmtcolor);
	       break;
	  }
	 g.fill(r);
       }
      ++idx;
    }

   for (BconRegion br : region_set) {
      int ln0 = br.getStartLine()-1;
      int ln1 = br.getEndLine()-1;
      setRectangle(r,ln0,ln1,x0,y0,wd,ht);
      if (br.hasFocus()) g.setColor(focuscolor);
      else if (br.hasBubble()) g.setColor(existscolor);
      else g.setColor(notexistscolor);
      g.draw(r);
    }
}



private void setRectangle(Rectangle2D r,int ln0,int ln1,double x0,double y0,double wd,double ht)
{
   double lct = getLineCount();
   double lht = (is_vertical ? ht / lct : wd/lct);

   if (is_vertical) {
      r.setRect(x0,y0+ln0*lht,wd,(ln1-ln0+1)*lht);
    }
   else {
      r.setRect(x0+ln0*lht,y0,(ln1-ln0+1)*lht,ht);
    }
}



private void setLine(Rectangle2D r,int line,double length,double x0,double y0,double wd,double ht)
{
   double lct = getLineCount();
   double lht = (is_vertical ? ht / lct : wd/lct);

   if (is_vertical) {
      r.setRect(x0,y0+line*lht,length*wd,lht);
    }
   else {
      r.setRect(x0+line*lht,y0,lht,length*ht);
    }
}




/********************************************************************************/
/*										*/
/*	Tool tip methods							*/
/*										*/
/********************************************************************************/

String computeToolTip(int ln0,int ln1)
{
   int ln = (ln0 + ln1)/2;
   BconRegion rgn = findRegionForLine(ln);

   if (ln0 < 0) ln0 = 0;
   if (ln1 >= line_set.size()) ln1 = line_set.size();

   int off0 = bale_file.findLineOffset(ln0);
   int off1 = bale_file.findLineOffset(ln1+1);
   if (off0 < 0) off0 = 0;
   if (off1 < 0) return null;

   String s = null;
   try {
      s = bale_file.getText(off0,off1-off0);
    }
   catch (BadLocationException e) { }

   String rslt = "<html>Class = " + for_class;
   if (rgn != null) rslt += "<br>Region = " + rgn.getRegionName();
   if (s != null) rslt += "<hr /><br><pre>" + s + "</pre>";

   return rslt;
}




/********************************************************************************/
/*										*/
/*	File listener								*/
/*										*/
/********************************************************************************/

private static class FileListener implements DocumentListener {

   @Override public void changedUpdate(DocumentEvent e) {
      // update data
    }

   @Override public void insertUpdate(DocumentEvent e) {
      // update data
    }

   @Override public void removeUpdate(DocumentEvent e) {
      // update data
    }

}	// end of inner class FileListener




}	// end of class BconOverviewClass




/* end of BconOverviewClass.java */

