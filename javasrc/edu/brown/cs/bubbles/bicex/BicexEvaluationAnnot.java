/********************************************************************************/
/*                                                                              */
/*              BicexEvaluationAnnot.java                                       */
/*                                                                              */
/*      Handle annotations for current evaluation viewer                        */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.bubbles.bicex;

import java.awt.Color;
import java.awt.Component;
import java.io.File;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleAnnotation;
import edu.brown.cs.bubbles.bale.BaleConstants.BaleFileOverview;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardImage;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaRoot;

class BicexEvaluationAnnot extends BicexPanel implements BicexConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private EvalAnnot       current_annotation;
private int             current_line;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BicexEvaluationAnnot(BicexEvaluationViewer ev)
{
   super(ev);
   
   current_annotation = null;
   current_line = 0;
}



/********************************************************************************/
/*                                                                              */
/*       Panel methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override protected JComponent setupPanel()     { return null; }

@Override void update()
{
   current_line = 0;
   checkAnnotation();
}


@Override void updateTime()
{
   checkAnnotation();
}
   

@Override void removePanel()
{
   removeAnnotation();
}



/********************************************************************************/
/*                                                                              */
/*      Annotation method                                                       */
/*                                                                              */
/********************************************************************************/

private void checkAnnotation()
{
   if (getContext() == null) return;
   BicexValue lnv = getContext().getValues().get("*LINE*");
   if (lnv == null || getContext().getFileName() == null) {
      removeAnnotation();
      return;
    }
   int lno = 0;
   String lnx = lnv.getStringValue(getExecution().getCurrentTime());
   if (lnx != null) {
      lno = Integer.parseInt(lnx);
    }
   if (lno != current_line || lno == 0) {
      removeAnnotation();
    }
   if (lno > 0) {
      EvalAnnot ea = new EvalAnnot(lno);
      synchronized (this) {
         current_annotation = ea;
         BaleFactory.getFactory().addAnnotation(current_annotation);
       }
    }
}


private synchronized void removeAnnotation()
{
   if (current_annotation != null) {
      BaleFactory.getFactory().removeAnnotation(current_annotation);
      current_annotation = null;
    }
}



/********************************************************************************/
/*                                                                              */
/*      Annotation for current line                                             */
/*                                                                              */
/********************************************************************************/

private class EvalAnnot implements BaleAnnotation {
   
   private BicexEvaluationContext eval_context;
   private BaleFileOverview for_document;
   private Position execute_pos; 
   private Color annot_color;
   private File for_file;
   
   EvalAnnot(int lno) {
      eval_context = getContext();
      for_file = new File(eval_context.getFileName());
      execute_pos = null;
     
      for_document = BaleFactory.getFactory().getFileOverview(null,for_file,false);
      int off = for_document.findLineOffset(lno);
      execute_pos = null;
      try {
         execute_pos = for_document.createPosition(off);
       }
      catch (BadLocationException e) {
         BoardLog.logE("BICEX","Bad execution position",e);
       }
      
      annot_color = BoardColors.getColor(BICEX_EXECUTE_ANNOT_COLOR_PROP);
    }
   
   @Override public int getDocumentOffset()	{ return execute_pos.getOffset(); }
   @Override public File getFile()		{ return for_file; }
   
   @Override public Icon getIcon(BudaBubble b) {
      return BoardImage.getIcon("seedexec");
    }
   
   @Override public String getToolTip() {
      if (execute_pos == null) return null;
      return "Continuous execution at " + current_line;
    }
   
   @Override public Color getLineColor(BudaBubble bbl) {
      return annot_color;
    }
   
   @Override public Color getBackgroundColor()			{ return null; }
   
   @Override public boolean getForceVisible(BudaBubble bb) {
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bb);
      BudaBubbleArea bba1 = BudaRoot.findBudaBubbleArea(eval_viewer);
      if (bba != bba1) return false;
      return false;                     // don't force this line to be visible
    }
   
   @Override public int getPriority()				{ return 20; }
   
   @Override public void addPopupButtons(Component c,JPopupMenu m) { }
   
}	// end of inner class EvalAnnot

}       // end of class BicexEvaluationAnnot




/* end of BicexEvaluationAnnot.java */

