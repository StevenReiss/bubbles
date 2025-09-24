/********************************************************************************/
/*										*/
/*		BucsSearchSolution.java 					*/
/*										*/
/*	description of class							*/
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

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bucs;


import edu.brown.cs.bubbles.bale.BaleConstants.BaleFileOverview;
import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.bubbles.buss.BussConstants.BussEntry;

import edu.brown.cs.ivy.swing.SwingEditorPane;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.border.LineBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


class BucsSearchSolution implements BucsConstants, BussEntry
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BucsSearchResult	for_result;
private BudaBubble		source_bubble;
private BudaBubble		result_bubble;
private JComponent		compact_component;
private BumpLocation		base_location;

static {
   jsyntaxpane.DefaultSyntaxKit.initKit();
}

/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BucsSearchSolution(BucsSearchResult r,BumpLocation base,BudaBubble src)
{
   for_result = r;
   base_location = base;
   source_bubble = src;
   result_bubble = null;
   String sum = computeSummary();
   compact_component = new JLabel(sum);
   compact_component.setBorder(LineBorder.createGrayLineBorder());
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getEntryName()
{
   String nm = for_result.getResultName();
   nm = nm.replace('/','.');
   return nm;
}


@Override public Component getCompactComponent()
{
   return compact_component;
}


@Override public Component getExpandComponent()
{
   return null;
}


@Override public String getExpandText()
{
   return for_result.getCode();
}

@Override public Collection<BumpLocation> getLocations()
{
   return Collections.singletonList(base_location);
}

@Override public BudaBubble getBubble()
{
   if (result_bubble == null) {
      result_bubble = new ResultBubble();
    }

   return result_bubble;
}


@Override public void dispose()
{
   if (result_bubble != null) result_bubble.disposeBubble();
}


/********************************************************************************/
/*										*/
/*	Code to compute a result summary					*/
/*										*/
/********************************************************************************/

private String computeSummary()
{
   StringBuffer buf = new StringBuffer();

   List<String> decls = getDeclarations(for_result.getCode());

   buf.append("<html><body>\n");
   buf.append("<table>\n");
   buf.append("<tr><td>Size:</td><td>");
   buf.append(for_result.getNumLines());
   buf.append(" lines, ");
   buf.append(for_result.getCodeSize());
   buf.append(" characters actual code</td></tr>\n");
   buf.append("<tr><td>Declarations:</td>");
   int ctr = 0;
   for (String s : decls) {
      if (ctr++ > 0) {
	 buf.append("<tr><td></td>");
       }
      buf.append("<td>");
      if (s.length() > 48) {
	 int i = 48;
	 while (i > 30) {
	    char ch = s.charAt(i);
	    if (!Character.isLetterOrDigit(ch)) break;
	    --i;
	 }
	 s = s.substring(0,i) + " ...";
       }
      buf.append(s);
      buf.append("</td></tr>\n");
    }
   buf.append("</table>\n");

   return buf.toString();
}


private List<String> getDeclarations(String cd)
{
   List<String> decls = new ArrayList<String>();
   StreamTokenizer str = new StreamTokenizer(new StringReader(cd));
   str.slashSlashComments(true);
   str.slashStarComments(true);

   StringBuffer decl = null;
   boolean lastwd = false;
   int bracect = 0;
   try {
      while (str.nextToken() != StreamTokenizer.TT_EOF) {
	 switch (str.ttype) {
	    case '{' :
	    case ';' :
	       if (decl != null) {
		  decls.add(decl.toString());
		  decl = null;
		  lastwd = false;
		}
	       break;
	  }
	 switch (str.ttype) {
	    case '{' :
	       ++bracect;
	       break;
	    case '}' :
	       if (bracect > 0) --bracect;
	       break;
	    case StreamTokenizer.TT_WORD :
	       if (bracect == 0 && decl == null) decl = new StringBuffer();
	       if (decl != null) {
		  if (lastwd) decl.append(" ");
		  decl.append(str.sval);
		  lastwd = true;
	       }
	       break;
	    default :
	       if (decl != null) decl.append((char) str.ttype);
	       lastwd = false;
	       break;
	  }
       }
      if (decl != null) {
	 decls.add(decl.toString());
       }
    }
   catch (IOException e) { }		// can't happen

   return decls;
}



/********************************************************************************/
/*										*/
/*	Methods to insert a solution						*/
/*										*/
/********************************************************************************/

private void replaceWithSolution()
{
   base_location.update();
   File f = base_location.getFile();
   String proj = base_location.getProject();
   BaleFileOverview bfo = BaleFactory.getFactory().getFileOverview(proj,f);
   BaleFileOverview bfo1 = null;
   boolean indent = false;
   int doff = base_location.getDefinitionOffset();
   int eoff = base_location.getDefinitionEndOffset();
   
   Document d1 = source_bubble.getContentDocument();
   if (d1 != null && d1 instanceof BaleFileOverview) {
      bfo1 = (BaleFileOverview) d1;
      int doff1 = bfo1.getFragmentOffset(doff);
      int eoff1 = bfo1.getFragmentOffset(eoff);
      if (doff1 >= 0 && eoff1 > 0) {
	 doff = doff1;
	 eoff = eoff1;
	 bfo = bfo1;
	 indent = true;
      }
    }
      
   int [] offsets = computeOffsets(bfo,doff,eoff);

   if (offsets == null) return;

   String newcode = for_result.getCode();

   bfo.replace(offsets[0],offsets[1]-offsets[0],newcode,true,indent);

   // might what to handle the case that this fails
}



private int [] computeOffsets(BaleFileOverview bfo,int soff,int eoff)
{
   String code = null;
   try {
      code = bfo.getText(soff,eoff - soff);
    }
   catch (BadLocationException e) {
      return null;
    }

   int off = 0;
   boolean inlinecmmt = false;
   boolean inareacmmt = false;
   char lastchar = 0;
   for (int i = 0; i < code.length(); ++i) {
      char ch = code.charAt(i);
      if (inlinecmmt) { 			// check for end of comment
	 if (ch == '\n') {
	    inlinecmmt = false;
	  }
       }
      else if (inareacmmt) {
	 if (lastchar == '*' && ch == '/') {
	    inareacmmt = false;
	    ch = ' ';
	  }
       }
      if (Character.isLetterOrDigit(ch) || ch == '@') {
	 if (!inlinecmmt && !inareacmmt) {
	    off = i;
	    break;
	  }
       }
      if (lastchar == '/') {                    // check for start of comment
	 if (ch == '/') {
	    inlinecmmt = true;
	  }
	 else if (ch == '*') {
	    inareacmmt = true;
	  }
       }
      lastchar = ch;
    }
   int xeoff = code.length();
   while (xeoff > 0 && Character.isWhitespace(code.charAt(xeoff-1))) --xeoff;

   if (xeoff <= off) return null;

   int [] bnds = new int[2];
   bnds[0] = soff + off;
   bnds[1] = soff + xeoff;

   return bnds;
}



/********************************************************************************/
/*										*/
/*	Bubble containing solution						*/
/*										*/
/********************************************************************************/

private class ResultBubble extends BudaBubble {

   private static final long serialVersionUID = 1;

   ResultBubble() {
      // String cd = "<html><pre><code class='Java'>" + for_result.getCode() + "</code></pre>";
      JEditorPane jep = new ResultEditor();
      jep.setEditable(false);
      JScrollPane jsp = new JScrollPane(jep);
      jep.setContentType("text/java");
      jep.setText(for_result.getCode());
      jep.setEditable(false);
      Dimension sz = jep.getPreferredSize();
      sz.width = Math.min(420,sz.width);
      sz.height = Math.min(300, sz.height);
      jsp.setSize(sz);
      jep.setPreferredSize(sz);
      setContentPane(jsp,jep);
    }

   @Override public void handlePopupMenu(MouseEvent e) {
      JPopupMenu pm = new JPopupMenu();
      pm.add(new AcceptAction());
      pm.add(new LicenseAction());
      pm.show(this,e.getX(),e.getY());
    }

}	// end of inner class ResultBubble


private class ResultEditor extends SwingEditorPane {

   private static final long serialVersionUID = 1;

   ResultEditor() {
      setOpaque(false);
    }

   @Override protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      Dimension sz = getSize();
      Color tc = BoardColors.getColor("Bucs.SolutionTopColor");
      Color bc = BoardColors.getColor("Bucs.SolutionBottomColor");
      Paint p = new GradientPaint(0f,0f,tc,0f,sz.height,bc);
      Shape r = new Rectangle2D.Float(0,0,sz.width,sz.height);
      g2.setPaint(p);
      g2.fill(r);
      super.paintComponent(g);
    }

}	// end of inner class ResultEditor




/********************************************************************************/
/*										*/
/*	Accept a solution							*/
/*										*/
/********************************************************************************/

private class AcceptAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   AcceptAction() {
      super("Use this solution");
    }

   @Override public void actionPerformed(ActionEvent e) {
      replaceWithSolution();
    }

}	// end of inner class AcceptAction


private class LicenseAction extends AbstractAction {

   private static final long serialVersionUID = 1;

   LicenseAction() {
      super("Show source license");
    }

   @Override public void actionPerformed(ActionEvent e) {
      BucsS6Engine eng = new BucsS6Engine(null);
      String lic = eng.getLicense(for_result.getLicenseUid());
      lic = "<html><pre><code>" + lic;
      JOptionPane.showMessageDialog(getBubble(),lic);
    }

}	// end of inner class LicenseAction




}	// end of class BucsSearchSolution




/* end of BucsSearchSolution.java */

