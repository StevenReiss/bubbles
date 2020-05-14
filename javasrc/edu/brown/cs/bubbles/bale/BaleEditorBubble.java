/********************************************************************************/
/*										*/
/*		BaleEditorBubble.java						*/
/*										*/
/*	Bubble Annotated Language Editor editor bubble				*/
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
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardMetrics;
import edu.brown.cs.bubbles.board.BoardThreadPool;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaBubbleLink;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaConstraint;
import edu.brown.cs.bubbles.buda.BudaDefaultPort;
import edu.brown.cs.bubbles.buda.BudaHover;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpLocation;

import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;



class BaleEditorBubble extends BudaBubble implements BaleConstants, BudaConstants
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static boolean callpath_bkg = true;

private static final long serialVersionUID = 1;



/********************************************************************************/
/*										*/
/*	Constructors and destructors						*/
/*										*/
/********************************************************************************/

BaleEditorBubble(BaleFragmentEditor bfe)
{
   BaleDocument bd = bfe.getDocument();
   bd.waitForAst();
   Dimension d;

   int maxwd = BALE_PROPERTIES.getIntOption("Bale.initial.width",BALE_MAX_INITIAL_WIDTH);
   int maxht = BALE_PROPERTIES.getIntOption("Bale.initial.height",BALE_MAX_INITIAL_HEIGHT);

   switch (bfe.getFragmentType()) {
      case NONE :
	 break;
      case METHOD :
	 maxwd = BALE_PROPERTIES.getIntOption("Bale.initial.width.method",maxwd);
	 maxht = BALE_PROPERTIES.getIntOption("Bale.initial.height.method",maxht);
	 break;
      case CLASS :
	 maxwd = BALE_PROPERTIES.getIntOption("Bale.initial.width.class",maxwd);
	 maxht = BALE_PROPERTIES.getIntOption("Bale.initial.height.class",maxht);
	 break;
      case FILE :
	 maxwd = BALE_PROPERTIES.getIntOption("Bale.initial.width.file",maxwd);
	 maxht = BALE_PROPERTIES.getIntOption("Bale.initial.height.file",maxht);
	 break;
      case FIELDS :
	 maxwd = BALE_PROPERTIES.getIntOption("Bale.initial.width.fields",maxwd);
	 maxht = BALE_PROPERTIES.getIntOption("Bale.initial.height.fields",maxht);
	 break;
      case STATICS :
      case IMPORTS :
      case EXPORTS :
      case CODE :
	 maxwd = BALE_PROPERTIES.getIntOption("Bale.initial.width.statics",maxwd);
	 maxht = BALE_PROPERTIES.getIntOption("Bale.initial.height.statics",maxht);
	 break;
      case MAIN :
	 maxwd = BALE_PROPERTIES.getIntOption("Bale.initial.width.main",maxwd);
	 maxht = BALE_PROPERTIES.getIntOption("Bale.initial.height.main",maxht);
	 break;
      case HEADER :
	 maxwd = BALE_PROPERTIES.getIntOption("Bale.initial.width.prefix",maxwd);
	 maxht = BALE_PROPERTIES.getIntOption("Bale.initial.height.prefix",maxht);
	 break;
      case ROFILE:
	 break;
      case ROMETHOD:
	 break;
      default:
	 break;
    }
   int maxbht = Math.max(maxht,BALE_MAX_INITIAL_BUBBLE_HEIGHT);

   GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
   GraphicsDevice gd = ge.getDefaultScreenDevice();
   DisplayMode dm = gd.getDisplayMode();
   maxwd = Math.min(maxwd, dm.getWidth()/2);
   maxht = Math.min(maxht, dm.getHeight()*2/3);

   bd.baleWriteLock();
   try {
      bd.noteOpen();

      d = bfe.getEditor().getPreferredSize();

      d.width += BALE_DELTA_INITIAL_WIDTH;
      d.height += BALE_DELTA_INITIAL_HEIGHT;

      if (d.width > maxwd) {
	 d.width = maxwd;
	 d.height = maxht;
       }
      else if (d.height > maxht) {
	 d.height = maxht;
	 d.width += 20;
       }

      d.width -= BALE_ANNOT_WIDTH;

      bfe.setInitialSize(d);
      // TODO: This call synch(getTreeLock()) which can block
      Dimension d1 = bfe.getPreferredSize();
      d1.height += 6;	// leave space for insets?
      if (d1.height > d.height) d1.height = d.height;

      setContentPane(bfe,bfe.getEditor());

      BoardLog.logD("BALE","OPEN EDITOR ON " + getContentName());

      bfe.setSize(d1);

      d = getPreferredSize();
      if (d.height > maxbht) {
	 d.height = maxbht;
	 setSize(d);
      }

      setInteriorColor(BoardColors.getColor("Bale.EditorBubbleInterior"));

      addComponentListener(new EditorBubbleListener());
    }
   finally { bd.baleWriteUnlock(); }

   dumpInitialMetrics();

   new Hoverer(bfe);
}



@Override protected void localDispose()
{
   BaleFragmentEditor bfe = (BaleFragmentEditor) getContentPane();
   if (bfe != null) {
      bfe.setVisible(false);
      bfe.dispose();
    }
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public String getContentProject()
{
   BaleFragmentEditor bfe = (BaleFragmentEditor) getContentPane();
   if (bfe == null) return null;
   BaleDocument bd = bfe.getDocument();
   return bd.getProjectName();
}


@Override public File getContentFile()
{
   BaleFragmentEditor bfe = (BaleFragmentEditor) getContentPane();
   if (bfe == null) return null;
   BaleDocument bd = bfe.getDocument();
   return bd.getFile();
}


@Override public String getContentName()
{
   BaleFragmentEditor bfe = (BaleFragmentEditor) getContentPane();
   if (bfe == null) return null;
   BaleDocument bd = bfe.getDocument();
   return bd.getFragmentName();
}


@Override public BudaContentNameType getContentType()
{
   BaleFragmentEditor bfe = (BaleFragmentEditor) getContentPane();
   BudaContentNameType nty = BudaContentNameType.NONE;
   if (bfe == null) return nty;

   switch (bfe.getFragmentType()) {
      case NONE :
	 break;
      case METHOD :
	 nty = BudaContentNameType.METHOD;
	 break;
      case CLASS :
	 nty = BudaContentNameType.CLASS;
	 break;
      case FILE :
	 nty = BudaContentNameType.FILE;
	 break;
      case FIELDS :
      case STATICS :
      case MAIN :
      case HEADER :
      case IMPORTS :
      case EXPORTS :
      case CODE :
	 nty = BudaContentNameType.CLASS_ITEM;
	 break;
      case ROFILE:
	 break;
      case ROMETHOD:
	 break;
      default:
	 break;
    }

   return nty;
}



@Override public Document getContentDocument()
{
   BaleFragmentEditor bfe = (BaleFragmentEditor) getContentPane();
   if (bfe == null) return null;
   return bfe.getDocument();
}







/********************************************************************************/
/*										*/
/*	Handle context menu							*/
/*										*/
/********************************************************************************/

@Override public void handlePopupMenu(MouseEvent e)
{
   BaleFragmentEditor bfe = (BaleFragmentEditor) getContentPane();
   if (bfe != null) bfe.handleContextMenu(e);
}



//necessary because componentHidden(...) isn't activated when this bubble is setVisible(false)
@Override public void setVisible(boolean b)
{
   super.setVisible(b);
   if (getContentPane() != null)
      ((BaleFragmentEditor) getContentPane()).hideFindBar();
}



/********************************************************************************/
/*										*/
/*	User and automatic requests						*/
/*										*/
/********************************************************************************/

@Override public boolean handleQuitRequest()
{
   // Handled by BALE FACTORY

   return true;
}



/********************************************************************************/
/*										*/
/*	Handle Bubble Connections						*/
/*										*/
/********************************************************************************/

@Override public boolean connectTo(BudaBubble bb,MouseEvent evt)
{
   if (!(bb instanceof BaleEditorBubble)) return false;
   if ((evt.getModifiersEx() & (InputEvent.CTRL_DOWN_MASK|InputEvent.META_DOWN_MASK)) == 0) return false;

   BaleFragmentEditor bfe1 = (BaleFragmentEditor) getContentPane();
   BaleFragmentEditor bfe2 = (BaleFragmentEditor) bb.getContentPane();

   if (bfe1.getFragmentType() != BaleFragmentType.METHOD ||
	  bfe2.getFragmentType() != BaleFragmentType.METHOD) return false;

   BaleDocument bd1 = bfe1.getDocument();
   BaleDocument bd2 = bfe2.getDocument();

   String proj = bd1.getProjectName();
   String mn1 = bd1.getFragmentName();
   String mn2 = bd2.getFragmentName();

   CallPathCompute cpc = new CallPathCompute(proj,mn1,mn2,bb);

   if (callpath_bkg) BoardThreadPool.start(cpc);
   else cpc.run();

   return true;
}



private class CallPathCompute implements Runnable {

   private String project_name;
   private String source_name;
   private String target_name;
   private BudaBubble target_bubble;
   private List<BumpLocation> location_set;

   CallPathCompute(String proj,String from,String to,BudaBubble tbb) {
      project_name = proj;
      source_name = from;
      target_name = to;
      target_bubble = tbb;
      location_set = null;
    }

   @Override public void run() {
      if (location_set == null) {
	 BumpClient bc = BumpClient.getBump();
	 location_set = bc.getCallPath(project_name,source_name,target_name);
	 if (location_set != null) {
	    // add the bubbles in the swing thread
	    SwingUtilities.invokeLater(this);
	  }
       }
      else {
	 int ct = location_set.size();
	 BaleEditorBubble b0 = BaleEditorBubble.this;
	 BaleFactory bf = BaleFactory.getFactory();
	 for (int i = 1; b0 != null && i < ct-1; ++i) {
	    BumpLocation bloc = location_set.get(i);
	    Position spos = getPosition(b0,location_set.get(i-1));
	    b0	= (BaleEditorBubble)
	       bf.createLocationEditorBubble(b0,spos,null,false,bloc,true,true,true);
	  }
	 if (b0 != null) {
	    Position spos = getPosition(b0,location_set.get(ct-2));
	    BudaRoot root = BudaRoot.findBudaRoot(b0);
	    BudaConstants.LinkPort port0;
	    if (spos == null) port0 = new BudaDefaultPort(BudaPortPosition.BORDER_EW,true);
	    else port0 = new BaleLinePort(b0,spos,"Call Path Link");
	    BudaConstants.LinkPort port1 = new BudaDefaultPort(BudaPortPosition.BORDER_EW_TOP,true);
	    BudaBubbleLink lnk = new BudaBubbleLink(b0,port0,target_bubble,port1);
	    if (root != null) root.addLink(lnk);
	  }
       }
    }

   private Position getPosition(BaleEditorBubble b0,BumpLocation sloc) {
      Position spos = null;
      try {
	 BaleFragmentEditor bfe = (BaleFragmentEditor) b0.getContentPane();
	 BaleDocument bfd = bfe.getDocument();
	 int offset = bfd.mapOffsetToJava(sloc.getOffset());
	 if (offset >= 0) spos = bfd.createPosition(offset);
       }
      catch (BadLocationException e) { }
      return spos;
    }

   @Override public String toString() {
      return "BALE_CallPathCompute_" + source_name + "_" + target_name;
    }

}	// end of inner class CallPathCompute




private class EditorBubbleListener extends ComponentAdapter {


   @Override public void componentMoved(ComponentEvent e) {
      if (getContentPane() != null) {
         ((BaleFragmentEditor) getContentPane()).relocateFindBar();
       }
    }

   @Override public void componentResized(ComponentEvent e) {
      if (getContentPane() != null) {
	 ((BaleFragmentEditor) getContentPane()).relocateFindBar();
       }
    }

}	// end of inner class EditorBubbleListener




/********************************************************************************/
/*										*/
/*	Preview method								*/
/*										*/
/********************************************************************************/

private void createPreview(BudaBubble bb, int xpos, int ypos)
{
   BudaRoot root = BudaRoot.findBudaRoot(this);
   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(this);
   Rectangle loc = BudaRoot.findBudaLocation(this);
   if (bba == null || loc == null || root == null) return;

   int x0 = xpos + 50;
   int y0 = ypos + 25;

   Dimension bsz = bb.getSize();
   Rectangle r = root.getCurrentViewport();

   if (x0 + bsz.width > r.x + r.width) x0 = r.x + r.width - bsz.width;
   if (y0 + bsz.height > r.y + r.height) y0 = r.y + r.height - bsz.height;
   bb.setTransient(true);

   bba.add(bb, new BudaConstraint(BudaBubblePosition.HOVER, x0, y0));
}



/********************************************************************************/
/*										*/
/*	Hoverer 								*/
/*										*/
/********************************************************************************/

private class Hoverer extends BudaHover {

   private BudaBubble preview_bubble;
   private BaleEditorPane editor_pane;

   Hoverer(BaleFragmentEditor bfe) {
      super(bfe.getEditor());
      preview_bubble = null;
      editor_pane = bfe.getEditor();
    }

   @Override public void handleHover(MouseEvent e) {
      if (!BALE_PROPERTIES.getBoolean(BALE_PREVIEW_ENABLE)) return;

      preview_bubble = editor_pane.getHoverBubble(e);

      if (preview_bubble != null && editor_pane.isShowing()) {
	 Component c0 = (Component) e.getSource();
	 BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(c0);
	 if (bba == null) return;
	 try {
	    Point pt = SwingUtilities.convertPoint(c0,e.getPoint(),bba);
	    createPreview(preview_bubble, pt.x, pt.y);
	  }
	 catch (Throwable t) {
	    // we can get here is the bubble went away in the interim
	  }
      }
    }

   @Override public void endHover(MouseEvent e) {
      if (preview_bubble != null){
	 preview_bubble.setVisible(false);
	 preview_bubble.disposeBubble();
	 preview_bubble = null;
      }
    }

}	// end of inner class Hoverer




/********************************************************************************/
/*										*/
/*	Metrics handling							*/
/*										*/
/********************************************************************************/

private void dumpInitialMetrics()
{
   BaleFragmentEditor bfe = (BaleFragmentEditor) getContentPane();
   if (bfe == null) return;
   BaleDocument bd = bfe.getDocument();
   int eln = bd.findLineNumber(bd.getLength()-1);
   BoardMetrics.noteCommand("BALE","NewBubbleLOC_" + getHashId() + "_" + eln);

   reportStatistics();
}


@Override protected void noteResize(int owd,int oht)
{
   super.noteResize(owd,oht);

   reportStatistics();
}



private void reportStatistics()
{
   BaleFragmentEditor bfe = (BaleFragmentEditor) getContentPane();
   if (bfe == null) return;
   BaleMetrics bm = new BaleMetrics();
   bm.computeMetrics(bfe);
}


static void noteElision(Component c)
{
   BudaBubble bb = BudaRoot.findBudaBubble(c);
   if (bb == null || !(bb instanceof BaleEditorBubble)) return;
   BaleEditorBubble be = (BaleEditorBubble) bb;
   be.reportStatistics();
}




}	// end of class BaleEditorBubble




/* end of BaleEditorBubble.java */
