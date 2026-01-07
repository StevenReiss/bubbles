/********************************************************************************/
/*										*/
/*		BddtBubbleManager.java						*/
/*										*/
/*	Bubbles debugger bubble management routines				*/
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



package edu.brown.cs.bubbles.bddt;

import edu.brown.cs.bubbles.bale.BaleFactory;
import edu.brown.cs.bubbles.board.BoardColors;
import edu.brown.cs.bubbles.board.BoardLog;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaBubbleLink;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaDefaultPort;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpClient;
import edu.brown.cs.bubbles.bump.BumpConstants;
import edu.brown.cs.bubbles.bump.BumpLocation;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JComponent;



class BddtBubbleManager implements BddtConstants, BudaConstants, BumpConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private BddtLaunchControl	launch_control;
private BudaBubbleArea		bubble_area;
private Map<BudaBubble,BubbleData> bubble_map;

private static int		console_width = BDDT_PROPERTIES.getInt(BDDT_CONSOLE_WIDTH_PROP);
private static int		console_height = BDDT_PROPERTIES.getInt(BDDT_CONSOLE_HEIGHT_PROP);


private static BoardProperties bddt_properties = BoardProperties.getProperties("Bddt");

private static boolean	delete_old = true;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BddtBubbleManager(BddtLaunchControl blc)
{
   launch_control = blc;
   bubble_area = null;
   bubble_map = new ConcurrentHashMap<>();
}



/********************************************************************************/
/*										*/
/*	Bubble creation entries 						*/
/*										*/
/********************************************************************************/

BudaBubble createExecBubble(BumpThread bt)
{
   boolean godown = bddt_properties.getBoolean("Bddt.grow.down");
   
   BoardLog.logD("BDDT","Start to create execution bubble for " + bt.getId());

   BumpThreadStack stk = bt.getStack();
   if (stk == null) return null;

   BumpStackFrame usefrm = stk.getFrame(0);
   BudaBubble bb = createSourceBubble(stk,0,BubbleType.EXEC,false,godown);
   if (bb == null && stk != null && bddt_properties.getBoolean("Bddt.show.user.bubble")) {
      BumpStackFrame frm = stk.getFrame(0);
      if (frm != null) {
	 BubbleData bd = findClosestBubble(bt,stk,frm,godown);
	 if (bd != null && bd.match(bt,stk,frm)) return bd.getBubble();
	 int lvl = -1;
	 if (bd != null) lvl = bd.aboveLevel(bt,stk,frm);
	 int mx = stk.getNumFrames();
	 if (lvl > 0) mx = mx-lvl;

	 for (int i = 1; i < mx; ++i) {
	    BumpStackFrame frame = stk.getFrame(i);
	    if (bd != null && bd.getFrame() != null && matchFrameMethod(bd.getFrame(),frame)) {
	       usefrm = frame;
	       break;
	     }	
	    if (launch_control.frameFileExists(frame) && !frame.isSystem()) {
	       bb = createSourceBubble(stk,i,BubbleType.EXEC,false,godown);
	       usefrm = frame;
	       break;
	    }
	  }
       }
    }

   if (bb != null) {
      BubbleData bd = bubble_map.get(bb);
      if (bd == null || bd.getBubbleType() != BubbleType.EXEC)
	 return bb;
      if (bddt_properties.getBoolean("Bddt.show.values")) {
	 BddtStackView sv = new BddtStackView(launch_control,bt);
	 if (usefrm == null) sv.expandFirst();
	 else sv.expandFrame(usefrm);
	 BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bb);
	 if (bba == null || stk == null) return bb;
	 BubbleData nbd = new BubbleData(sv,bt,stk,stk.getFrame(0),BubbleType.FRAME);
	 bubble_map.put(sv,nbd);
	 int place = (godown ? PLACEMENT_RIGHT : PLACEMENT_BELOW);
	 bba.addBubble(sv,bb,null,place|PLACEMENT_GROUPED|PLACEMENT_EXPLICIT);
	 bd.setAssocBubble(sv);
       }
    }

   if (bb == null) {
      BoardLog.logD("BDDT","No bubble found for stopping point");
    }
   
   return bb;
}



void createUserStackBubble(BubbleData bd,boolean godown)
{
   if (bd == null) return;
   BudaBubble bb = bd.getBubble();
   if (bb == null || bd.getBubbleType() != BubbleType.USER) return;
   BumpThread bt = bd.getThread();
   if (bt == null) return;
   BumpThreadStack stk = bt.getStack();
   if (stk == null) return;
   if (bd.getAssocBubble() != null) return;
   if (bddt_properties.getBoolean("Bddt.show.values")) {
      BddtStackView sv = new BddtStackView(launch_control,bt);
      BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(bb);
      if (bba == null) return;
      BubbleData nbd = new BubbleData(sv,bt,stk,stk.getFrame(0),BubbleType.FRAME);
      bubble_map.put(sv,nbd);
      int place = (godown ? PLACEMENT_RIGHT : PLACEMENT_BELOW);
      bba.addBubble(sv,bb,null,place|PLACEMENT_GROUPED|PLACEMENT_EXPLICIT);
      bd.setAssocBubble(sv);
   }
}



// CHECKSTYLE:OFF
private BudaBubble createSourceBubble(BumpThreadStack stk,int frm,BubbleType typ,boolean frc,boolean godown)
// CHECKSTYLE:ON   
{
   setupBubbleArea();

   if (stk == null) return null;

   BumpThread bt = stk.getThread();

   boolean libbbl = frc || bddt_properties.getBoolean("Bddt.show.library.bubbles");

   // find user stack frame for the stack
   if (stk.getNumFrames() <= frm) return null;
   BumpStackFrame frame = stk.getFrame(frm);

   int xpos = -1;
   int ypos = -1;
   BudaBubble link = null;
   int linkline = -1;
   boolean linkto = true;

   BubbleData bd = findClosestBubble(bt,stk,frame,godown);
   if (bd != null && bd.match(bt,stk,frame)) {
      if (bd.getBubbleType() == BubbleType.USER) {
	 createUserStackBubble(bd,godown);
       }
      bd.update(stk,frame);
      showBubble(bd.getBubble());
      return null;
    }

   if (bd != null) {
      Rectangle r = BudaRoot.findBudaLocation(bd.getBubble());
      BudaBubble abb = bd.getAssocBubble();
      if (abb != null) {
	 Rectangle r1 = BudaRoot.findBudaLocation(abb);
	 if (r == null) r = r1;
	 else if (r1 != null) r = r.union(r1);
       }
      if (r != null && bd.aboveLevel(bt,stk,frame) >= 0) {		// calling bubble
	 if (godown) {
	    xpos = r.x;
	    ypos = r.y + r.height + 40;
	  }
	 else {
	    xpos = r.x + r.width + 40;
	    ypos = r.y;
	  }
	 link = bd.getBubble();
	 linkline = bd.getLineNumber();
       }
      else if (r != null && bd.getBubbleType() == BubbleType.EXEC && bd.getThread() == bt) {
	 if (godown) {
	    xpos = r.x;
	    ypos = r.y + r.height + 40;
	  }
	 else {
	    xpos = r.x + r.width + 40;
	    ypos = r.y;
	  }
	 link = bd.getBubble();
	 linkline = bd.getLineNumber();
	 linkto = false;
       }
      else if (r != null) {			// new thread
	 // xpos = r.x + r.width + 40;
	 // ypos = r.y;
	 if (godown) {
	    xpos = r.x + r.width + 40;
	    ypos = r.y;
	  }
	 else {
	    xpos = r.x;
	    ypos = r.y + r.height + 40;
	  }
       }
      else {
	 xpos = 100;
	 ypos = 100;
       }
    }
   else {
      xpos = 100;
      ypos = 100;
      for (BubbleData bdx : bubble_map.values()) {
	 switch (bdx.getBubbleType()) {
	    case BDDT :
	    case CONSOLE :
	    case HISTORY :
	    case SWING :
	    case PERF :
	    case THREADS :
	    case EVAL :
	    case INTERACT :
            case AUX :
	       Rectangle rx = BudaRoot.findBudaLocation(bdx.getBubble());
	       if (rx != null) {
		  xpos = Math.max(xpos,rx.x + rx.width + 40);
		  ypos = Math.min(ypos,rx.y);
		}
	       continue;
	    default:
	       break;
	  }
       }
    }

   if (ypos < 0) ypos = 0;
   if (xpos < 0) xpos = 0;

   String proj = launch_control.getProject();
   BudaBubble bb = null;
   if (frame.getFile() != null && (!frame.isSystem() || libbbl)) {
      String mthd = frame.getMethod();
      if (mthd != null && mthd.length() > 0) {
	 String sgn = frame.getSignature();
	 String mid = mthd;
	 if (sgn != null) mid += sgn;
	 if (frame.isSystem() && launch_control.frameFileExists(frame) && libbbl) {
	    bb = BaleFactory.getFactory().createSystemMethodBubble(proj,mid,
		  frame.getFile(),frame.getLineNumber());
	    if (bb == null) {
	       bb = new BddtLibraryBubble(frame);
	     }
	  }
	 else if (!frame.isSystem()) {
	    bb = BaleFactory.getFactory().createMethodBubble(proj,mid);
	    if (bb == null) {
	       bb = BaleFactory.getFactory().createMethodBubble(null,mid);
	     }
	  }
       }
      else {
	 bb = BaleFactory.getFactory().createFileBubble(proj, frame.getFile(),null);
      }
    }
   else if (libbbl) {
      bb = new BddtLibraryBubble(frame);
    }

   if (bb == null) {
      BoardLog.logD("BDDT","No bubble created for " + frame.getMethod() + frame.getSignature());
    }

   if (bb != null) {
      int ht = Math.max(bb.getHeight(),BDDT_STACK_HEIGHT);
      int wd = bb.getWidth() + BDDT_STACK_WIDTH + BudaConstants.BUBBLE_CREATION_NEAR_SPACE;
      Point xpt = findSpaceForBubble(xpos,ypos,ht,wd,godown);
      if (xpt != null) {
	 xpos = xpt.x;
	 ypos = xpt.y;
       }
      // TODO: want to ensure space available here -- if not move the point
      BubbleData nbd = new BubbleData(bb,bt,stk,frame,typ);
      bubble_map.put(bb,nbd);
      BudaRoot root = BudaRoot.findBudaRoot(bubble_area);
      bubble_area.addBubble(bb,null,new Point(xpos,ypos),PLACEMENT_EXPLICIT|PLACEMENT_NEW);

      if (link != null && link.isShowing()) {
	 LinkPort port0;
	 if (link instanceof BddtLibraryBubble || linkline <= 0) {
	    port0 = new BudaDefaultPort(BudaPortPosition.BORDER_EW,true);
	 }
	 else {
	    port0 = BaleFactory.getFactory().findPortForLine(link,linkline);
	 }
	 if (port0 != null) {
	    LinkPort port1 = new BudaDefaultPort(BudaPortPosition.BORDER_EW_TOP,true);
	    BudaBubbleLink lnk = new BudaBubbleLink(link,port0,bb,port1);
	    lnk.setColor(BoardColors.getColor(BDDT_LINK_COLOR_PROP));
	    root.addLink(lnk);
	    if (!linkto) {
	       lnk.setEndTypes(BudaPortEndType.END_ARROW,BudaPortEndType.END_NONE);
	     }
	 }
      }
      showBubble(bb);
    }

   return bb;
}





void restart()
{
   setupBubbleArea();

   Collection<BubbleData> bbls = new ArrayList<BubbleData>(bubble_map.values());
   for (BubbleData bd : bbls) {
      switch (bd.getBubbleType()) {
	 case BDDT :
	 case THREADS :
	 case CONSOLE :
	 case HISTORY :
	 case SWING :
	 case PERF :
	 case EVAL :
	 case INTERACT :
         case AUX :
	    break;
	 case EXEC :
	 case FRAME :
	 case STOP_TRACE :
	    if (bd.canRemove()) bubble_area.userRemoveBubble(bd.getBubble());
	    break;
	 case VALUES :
	    bubble_area.userRemoveBubble(bd.getBubble());
	    break;
	 case USER :
	    break;
       }
    }
}



/********************************************************************************/
/*										*/
/*	Entries to create auxilliary bubbles					*/
/*										*/
/********************************************************************************/

BudaBubble createThreadBubble()
{
   setupBubbleArea();

   Collection<BubbleData> bbls = new ArrayList<BubbleData>(bubble_map.values());
   for (BubbleData bd : bbls) {
      switch (bd.getBubbleType()) {
	 case THREADS :
	    BudaBubble tbd = bd.getBubble();
	    if (tbd.isFloating() && tbd.isShowing()) return tbd;
	    if (tbd.isFloating()) {
	       tbd.setVisible(true);
	       return tbd;
	     }
	    break;
	 default :
	    break;
       }
    }

   if (bubble_area == null) return null;

   BudaBubble bb = new BddtThreadView(launch_control);
   Rectangle r = launch_control.getBounds();
   int x = r.x;
   int y = r.y + r.height + console_height + 20 + 20;

   positionBubble(bb,x,y,bddt_properties.getBoolean(BDDT_PROPERTY_FLOAT_THREADS));

   return bb;
}



BudaBubble createConsoleBubble()
{
   setupBubbleArea();

   Collection<BubbleData> bbls = new ArrayList<BubbleData>(bubble_map.values());
   for (BubbleData bd : bbls) {
      switch (bd.getBubbleType()) {
	 case CONSOLE :
	    BudaBubble tbd = bd.getBubble();
	    if (tbd.isFloating() && tbd.isShowing()) return tbd;
	    if (tbd.isFloating()) {
	       tbd.setVisible(true);
	       return tbd;
	     }
	    break;
	 default :
	    break;
       }
    }

   if (bubble_area == null) return null;
   BudaBubble bb = BddtFactory.getFactory().getConsoleControl().createConsole(launch_control);

   Rectangle r = launch_control.getBounds();
   int x = r.x;
   int y = r.y + r.height + 20;

   positionBubble(bb,x,y,bddt_properties.getBoolean(BDDT_PROPERTY_FLOAT_CONSOLE));

   return bb;
}




BudaBubble createHistoryBubble()
{
   setupBubbleArea();

   Collection<BubbleData> bbls = new ArrayList<BubbleData>(bubble_map.values());
   for (BubbleData bd : bbls) {
      switch (bd.getBubbleType()) {
	 case HISTORY :
	    BudaBubble tbd = bd.getBubble();
	    if (tbd.isFloating() && tbd.isShowing()) return tbd;
	    if (tbd.isFloating()) {
	       tbd.setVisible(true);
	       return tbd;
	     }
	    break;
	 default :
	    break;
       }
    }

   if (bubble_area == null) return null;
   BudaBubble bb = BddtFactory.getFactory().getHistoryControl().createHistory(launch_control);

   Rectangle r = launch_control.getBounds();
   int x = r.x;
   int y = r.y + r.height + console_height + 20 + BDDT_STACK_HEIGHT + 20 + 20;

   positionBubble(bb,x,y,bddt_properties.getBoolean(BDDT_PROPERTY_FLOAT_HISTORY));

   return bb;
}




BudaBubble createSwingBubble()
{
   setupBubbleArea();

   Collection<BubbleData> bbls = new ArrayList<BubbleData>(bubble_map.values());
   for (BubbleData bd : bbls) {
      switch (bd.getBubbleType()) {
	 case SWING :
	    BudaBubble tbd = bd.getBubble();
	    if (tbd.isFloating() && tbd.isShowing()) return tbd;
	    if (tbd.isFloating()) {
	       tbd.setVisible(true);
	       return tbd;
	     }
	    break;
	 default :
	    break;
       }
    }

   if (bubble_area == null) return null;
   BudaBubble bb = new BddtSwingPanel(launch_control);

   Rectangle r = launch_control.getBounds();
   int x = r.x + r.width + 20;
   int y = r.y;

   positionBubble(bb,x,y,bddt_properties.getBoolean(BDDT_PROPERTY_FLOAT_SWING));

   return bb;
}


BudaBubble createAuxBubble(BddtAuxBubbleAction aux)
{
   setupBubbleArea();
   Collection<BubbleData> bbls = new ArrayList<BubbleData>(bubble_map.values());
   
   Rectangle r = launch_control.getBounds();
   int x = r.x;
   int y = r.y + r.height + 20;
   for (BubbleData bd : bbls) {
      BudaBubble tbd = bd.getBubble();
      switch (bd.getBubbleType()) {
	 case AUX :
            BddtAuxBubble auxbb = (BddtAuxBubble) tbd;
            String typ = auxbb.getAuxType();  
            if (!typ.equals(aux.getAuxType())) break;
	    if (tbd.isFloating() && tbd.isShowing()) return tbd;
	    if (tbd.isFloating()) {
	       tbd.setVisible(true);
	       return tbd;
	     }
	    break;
         case USER :
         case EXEC :
         case FRAME :
         case VALUES :
         case INTERACT :
            continue;
	 default :
	    break;
       }
      if (tbd.isFixed() || tbd.isFloating()) {
        Rectangle r1 = tbd.getBounds();
        if (Math.abs(r1.getX()-x) < 300) {
           y = Math.max(y,r1.y + r1.height + 20);
         }
       }
    }
   
   if (bubble_area == null) return null;
   BudaBubble bb = aux.createBubble();
   
   positionBubble(bb,x,y,aux.isFixed());
   
   bb.repaint();
   
   return bb;
}



BudaBubble createPerformanceBubble()
{
   setupBubbleArea();

   Collection<BubbleData> bbls = new ArrayList<>(bubble_map.values());
   for (BubbleData bd : bbls) {
      switch (bd.getBubbleType()) {
	 case PERF :
	    BudaBubble tbd = bd.getBubble();
	    if (tbd.isFloating() && tbd.isShowing()) return tbd;
	    if (tbd.isFloating()) {
	       tbd.setVisible(true);
	       return tbd;
	     }
	    break;
	 default :
	    break;
       }
    }

   if (bubble_area == null) return null;
   BudaBubble bb = launch_control.createPerfBubble();

   Rectangle r = launch_control.getBounds();
   int x = r.x + BDDT_HISTORY_WIDTH + 20;
   int y = r.y + r.height + console_height + 20 + BDDT_STACK_HEIGHT + 20 + 20;

   positionBubble(bb,x,y,bddt_properties.getBoolean(BDDT_PROPERTY_FLOAT_PERFORMANCE));

   return bb;
}




BudaBubble createValueViewerBubble()
{
   setupBubbleArea();

   Collection<BubbleData> bbls = new ArrayList<BubbleData>(bubble_map.values());
   for (BubbleData bd : bbls) {
      switch (bd.getBubbleType()) {
	 case EVAL :
	    BudaBubble tbd = bd.getBubble();
	    if (tbd.isFloating() && tbd.isShowing()) return tbd;
	    if (tbd.isFloating()) {
	       tbd.setVisible(true);
	       return tbd;
	     }
	    break;
	 default :
	    break;
       }
    }

   if (bubble_area == null) return null;
   BudaBubble bb = new BddtEvaluationBubble(launch_control);

   Rectangle r = launch_control.getBounds();
   int x = r.x + console_width + 20;
   int y = r.y;

   positionBubble(bb,x,y,bddt_properties.getBoolean(BDDT_PROPERTY_FLOAT_EVALUATION));

   return bb;
}




BudaBubble createInteractionBubble()
{
   setupBubbleArea();

   Collection<BubbleData> bbls = new ArrayList<BubbleData>(bubble_map.values());
   for (BubbleData bd : bbls) {
      switch (bd.getBubbleType()) {
	 case INTERACT :
	    BudaBubble tbd = bd.getBubble();
	    if (tbd.isFloating() && tbd.isShowing()) return tbd;
	    if (tbd.isFloating()) {
	       tbd.setVisible(true);
	       return tbd;
	     }
	    break;
	 default :
	    break;
       }
    }

   if (bubble_area == null) return null;
   BudaBubble bb = new BddtInteractionBubble(launch_control);

   Rectangle r = launch_control.getBounds();
   int x = r.x + BDDT_STACK_WIDTH + 20;
   int y = r.y + r.height + console_height + 20 + 20;

   positionBubble(bb,x,y,bddt_properties.getBoolean(BDDT_PROPERTY_FLOAT_EVALUATION));

   return bb;
}



private void positionBubble(BudaBubble bb,int x,int y,boolean fixed)
{
   if (fixed) {
      bubble_area.addBubble(bb,BudaBubblePosition.FLOAT,x,y);
      BudaWorkingSet ws = BddtFactory.getFactory().getActiveWorkingSet();
      if (ws != null) bb.setWorkingSet(ws);
    }
   else {
      bubble_area.addBubble(bb,BudaBubblePosition.MOVABLE,x,y);
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

BumpStackFrame getFrameForBubble(BudaBubble bb)
{
   BubbleData bd = bubble_map.get(bb);
   if (bd == null) return null;
   if (bd.getFrame() == null) { 		// recompute as needed
      BumpStackFrame frm = launch_control.getActiveFrame();
      if (frm == null || bd.getBubble() == null) return null;
      if (bd.getBubble().getContentType() == BudaContentNameType.METHOD) {
	 String s1 = bd.getBubble().getContentName();
	 if (s1 != null) {
	    int idx1 = s1.indexOf("(");
	    String s1a = s1;
	    String s1b = null;
	    if (idx1 > 0) {
	       s1a = s1.substring(0,idx1);
	       s1b = s1.substring(idx1);
	     }
	    BumpThread bt = frm.getThread();
	    BumpThreadStack stk = bt.getStack();
	    if (stk != null) {
	       BumpStackFrame xfrm = null;
	       for (int i = 0; i < stk.getNumFrames(); ++i) {
		  BumpStackFrame sfrm = stk.getFrame(i);
		  if (sameMethod(s1a,sfrm.getMethod()) &&
			(s1b == null || BumpLocation.compareParameters(s1b,sfrm.getSignature()))) {
		     if (xfrm == null || sfrm == frm) xfrm = sfrm;
		   }
		}
	       if (xfrm != null) bd.update(stk,xfrm);
	     }
	 }
      }
    }

   return bd.getFrame();
}




private static boolean sameMethod(String m1,String m2)
{
   if (m1.equals(m2)) return true;
   m2 = m2.replace("$",".");
   m1 = m1.replace("$",".");
   if (m1.equals(m2)) return true;
   int idx1 = m1.indexOf(".<init>");
   int idx2 = m2.indexOf(".<init>");
   if (idx1 < 0 && idx2 < 0) return false;
   if (idx1 >= 0 && idx2 >= 0) return false;
   if (idx1 >= 0) {
      int idx1a = m1.lastIndexOf(".",idx1-1);
      String nm = m1.substring(idx1a+1,idx1);
      m1 = m1.substring(0,idx1+1) + nm;
   }
   if (idx2 >= 0) {
      int idx2a = m2.lastIndexOf(".",idx2-1);
      String nm = m2.substring(idx2a+1,idx2);
      m2 = m2.substring(0,idx2+1) + nm;
   }
   return m1.equals(m2);
}



/********************************************************************************/
/*										*/
/*	Bubble support routines 						*/
/*										*/
/********************************************************************************/

private void setupBubbleArea()
{
   if (bubble_area != null) return;

   bubble_area = BudaRoot.findBudaBubbleArea(launch_control);
   if (bubble_area == null) return;

   JComponent c2 = (JComponent) bubble_area.getParent().getParent().getParent();
   c2.addContainerListener(new ChannelListener());

   for (BudaBubble bb : bubble_area.getBubbles()) {
      if (bubble_map.get(bb) == null) {
	 bubble_map.put(bb,new BubbleData(bb));
       }
    }

   BudaRoot.addBubbleViewCallback(new BubbleUpdater());
}




private BubbleData findClosestBubble(BumpThread bt,BumpThreadStack stk,BumpStackFrame frm,boolean godown)
{
   BubbleData best = null;

   // first try to find an exact match
   for (BubbleData bd : bubble_map.values()) {
      if (isBubbleRelevant(bd) && bd.match(bt,stk,frm)) {
	 best = bd;
	 break;
       }
    }

   // next try to find a user bubble that matches
   if (best == null) {
      for (BubbleData bd : bubble_map.values()) {
	 if (isBubbleRelevant(bd) && bd.matchUser(bt,stk,frm)) {
	    best = bd;
	    break;
	 }
      }
   }

   if (best == null) {			// try to find recent user bubble
      String mthd = frm.getMethod();
      String mid = mthd + frm.getSignature();
      long when = 0;
      for (BubbleData bd : bubble_map.values()) {
	 if (bd.getBubbleType() == BubbleType.EXEC && isBubbleRelevant(bd))
	    when = Math.max(when,bd.getLastTime());
       }
      for (BubbleData bd : bubble_map.values()) {
	 if (bd.getBubbleType() != BubbleType.USER) continue;
	 if (bd.getLastTime() < when) continue;
	 if (!isBubbleRelevant(bd)) continue;
	 BudaBubble bb = bd.getBubble();
	 if (bb == null) continue;
	 if (bb.getContentName() == null) continue;
	 if (bb.getContentName() == mid) {
	    best = bd;
	    when = bd.getLastTime();
	  }
       }
    }


   if (best == null) {
      // alternatively, try to find a bubble we are below
      int blvl = -1;
      for (BubbleData bd : bubble_map.values()) {
	 if (bd.getBubbleType() != BubbleType.EXEC) continue;
	 if (!isBubbleRelevant(bd)) continue;
	 int lvl = bd.aboveLevel(bt,stk,frm);
	 if (lvl > 0 && blvl < lvl) {
	    best = bd;
	    blvl = lvl;
	  }
       }
    }

   if (best != null) {
      // we have found an entry relevant to the stack
      if (delete_old) {
	 Collection<BubbleData> bbls = new ArrayList<BubbleData>(bubble_map.values());
	 for (BubbleData bd : bbls) {
	    if (!isBubbleRelevant(bd)) continue;
	    if (bd != best && bd.isBelow(best) && bd.canRemove() &&
		  bd.getBubbleType() == BubbleType.EXEC) {
	       bubble_area.userRemoveBubble(bd.getBubble());
	       BudaBubble sb = bd.getAssocBubble();
	       if (sb != null) {
		  bubble_area.userRemoveBubble(sb);
		  bd.setAssocBubble(null);
		}
	     }
	  }
       }
      return best;
    }

   // find rightmost/bottommost bubble for the current thread
   int rmost = -1;
   for (BubbleData bd : bubble_map.values()) {
      if (!isBubbleRelevant(bd)) continue;
      if (bd.getThread() == bt && bd.getBubbleType() == BubbleType.EXEC) {
	 BudaBubble bb = bd.getBubble();
	 Rectangle r = BudaRoot.findBudaLocation(bb);
	 if (r == null) continue;
	 int coord = (godown ? r.y + r.height : r.x + r.width);
	 if (coord > rmost) {
	    rmost = coord;
	    best = bd;
	  }
       }
    }

   if (best == null) {
      // if first bubble for this thread, use most recent bubble
      long tmost = -1;
      for (BubbleData bd : bubble_map.values()) {
	 if (!isBubbleRelevant(bd)) continue;
	 long btim = bd.getLastTime();
	 if (btim < 0) continue;
	 switch (bd.getBubbleType()) {
	    case BDDT :
	    case CONSOLE :
	    case HISTORY :
	    case SWING :
	    case PERF :
	    case THREADS :
	    case STOP_TRACE :
	    case EVAL :
	    case INTERACT :
            case AUX :
	       continue;
	    default:
	       break;
	  }
	 if (bd.getBubble().isFixed()) continue;
	 if (best == null) {
	    tmost = btim;
	    best = bd;
	  }
	 else if (best.getBubbleType() != BubbleType.EXEC) {
	    if (bd.getBubbleType() == BubbleType.EXEC || btim > tmost) {
	       tmost = btim;
	       best = bd;
	     }
	  }
	 else if (bd.getBubbleType() == BubbleType.EXEC && btim > tmost) {
	    tmost = btim;
	    best = bd;
	  }
       }
    }

   return best;
}



private void showBubble(BudaBubble bb)
{
   bubble_area.scrollBubbleVisible(bb);
}


private boolean isBubbleRelevant(BubbleData bd)
{
   BudaWorkingSet ws = BddtFactory.getFactory().getActiveWorkingSet();
   if (ws == null) return true;

   BudaBubble bb = bd.getBubble();
   if (ws.getRegion().intersects(bb.getBounds())) return true;

   return false;
}



/********************************************************************************/
/*										*/
/*	Utility functions							*/
/*										*/
/********************************************************************************/

private static boolean matchFrameMethod(BumpStackFrame sf1,BumpStackFrame sf2)
{
   if (sf1.getMethod() != null) {
      if (!sf1.getMethod().equals(sf2.getMethod())) return false;
    }
   else if (sf2.getMethod() != null) return false;

   if (sf1.getSignature() != null) {
      if (!sf1.getSignature().equals(sf2.getSignature())) return false;
    }
   else if (sf2.getSignature() != null) return false;

   return true;
}



/********************************************************************************/
/*										*/
/*	Find space for bubble							*/
/*										*/
/********************************************************************************/

private Point findSpaceForBubble(int xpos,int ypos,int ht,int wd,boolean godown)
{
   Point rslt = null;
   for ( ; ; ) {
      Rectangle r = new Rectangle(xpos-5,ypos-5, wd+10,ht+10);
      int ct = 0;
      int maxx = -1;
      int maxy = -1;
      for (BudaBubble bb : bubble_area.getBubblesInRegion(r)) {
	 if (bb.isFixed() || bb.isTransient() || !bb.isShowing()) continue;
	 maxx = Math.max(maxx,bb.getX() + bb.getWidth() + BudaConstants.BUBBLE_CREATION_SPACE);
	 maxy = Math.max(maxy,bb.getY() + bb.getHeight() + BudaConstants.BUBBLE_CREATION_SPACE);
	 ++ct;
       }
      if (ct == 0) return rslt;
      if (godown) ypos = maxy;
      else xpos = maxx;
      if (rslt == null) rslt = new Point(xpos,ypos);
      else rslt.setLocation(xpos,ypos);
    }
}


/********************************************************************************/
/*										*/
/*	Handle user changes to the bubble area					*/
/*										*/
/********************************************************************************/

private final class BubbleUpdater implements BubbleViewCallback {

   @Override public void bubbleAdded(BudaBubble bb) {
      if (bb.isTransient()) return;
      if (BudaRoot.findBudaBubbleArea(bb) != bubble_area) return;
      BudaWorkingSet ws = BddtFactory.getFactory().getActiveWorkingSet();
      if (ws != null) {
         if (!ws.getRegion().intersects(bb.getBounds())) return;
       }
      if (bubble_map.get(bb) == null) {
         bubble_map.put(bb,new BubbleData(bb));
       }
    }

   @Override public void bubbleRemoved(BudaBubble bb) {
      bubble_map.remove(bb);
    }

}


private final class ChannelListener implements ContainerListener {

   @Override public void componentAdded(ContainerEvent e) { }

   @Override public void componentRemoved(ContainerEvent e) {
      Component c = e.getChild();
      if (c == null) return;
      boolean fnd = false;
      for (Component c1 = bubble_area; c1 != null; c1 = c1.getParent()) {
	 if (c == c1) {
	    fnd = true;
	    break;
	  }
       }
      if (!fnd) return;
      if (launch_control == null) return;
      BumpClient bc = BumpClient.getBump();
      bc.terminate(launch_control.getProcess());
    }

}	// end of inner class ChannelListener



/********************************************************************************/
/*										*/
/*	Information associated with a bubble					*/
/*										*/
/********************************************************************************/

private static class BubbleData {

   private BumpThread	base_thread;
   private BumpThreadStack for_stack;
   private BumpStackFrame for_frame;
   private int		frame_level;
   private int		stack_depth;
   private BudaBubble	for_bubble;
   private BubbleType	bubble_type;
   private long 	last_used;
   private boolean	can_remove;
   private BudaBubble	assoc_bubble;

   BubbleData(BudaBubble bb) {
      for_bubble = bb;
      base_thread = null;
      for_stack = null;
      for_frame = null;
      frame_level = -1;
      stack_depth = -1;
      if (bb instanceof BddtConsoleBubble) bubble_type = BubbleType.CONSOLE;
      else if (bb instanceof BddtLaunchControl) bubble_type = BubbleType.BDDT;
      else if (bb instanceof BddtThreadView) bubble_type = BubbleType.THREADS;
      else if (bb instanceof BddtHistoryBubble) bubble_type = BubbleType.HISTORY;
      else if (bb instanceof BddtSwingPanel) bubble_type = BubbleType.SWING;
      else if (bb instanceof BddtPerfViewTable.PerfBubble) bubble_type = BubbleType.PERF;
      else if (bb instanceof BddtStopTraceBubble) bubble_type = BubbleType.STOP_TRACE;
      else if (bb instanceof BddtEvaluationBubble) bubble_type = BubbleType.EVAL;
      else if (bb instanceof BddtInteractionBubble) bubble_type = BubbleType.INTERACT;
      else if (bb instanceof BddtAuxBubble) bubble_type = BubbleType.AUX;
      else bubble_type = BubbleType.USER;
      last_used = System.currentTimeMillis();
      can_remove = false;
      assoc_bubble = null;
    }

   BubbleData(BudaBubble bb,BumpThread bt,BumpThreadStack stk,BumpStackFrame sf,BubbleType typ) {
      for_bubble = bb;
      base_thread = bt;
      for_stack = stk;
      for_frame = sf;
      stack_depth = stk.getNumFrames();
      frame_level = -1;
      for (int i = 0; i < stk.getNumFrames(); ++i) {
	 if (stk.getFrame(i) == for_frame) {
	    frame_level = i;
	    break;
	  }
       }
      bubble_type = typ;
      last_used = System.currentTimeMillis();
      can_remove = true;
      assoc_bubble = null;
    }

   long getLastTime()					{ return last_used; }
   BumpThread getThread()				{ return base_thread; }
   boolean canRemove()					{ return can_remove; }
   BudaBubble getBubble()				{ return for_bubble; }
   int getLineNumber()					{ return for_frame.getLineNumber(); }
   BumpStackFrame getFrame()				{ return for_frame; }
   BubbleType getBubbleType()				{ return bubble_type; }

   void setAssocBubble(BudaBubble bb)			{ assoc_bubble = bb; }
   BudaBubble getAssocBubble()				{ return assoc_bubble; }

   void update(BumpThreadStack stk,BumpStackFrame sf) {
      last_used = System.currentTimeMillis();
      for_stack = stk;
      for_frame = sf;
      if (for_bubble instanceof BddtLibraryBubble) {
	 BddtLibraryBubble lbb = (BddtLibraryBubble) for_bubble;
	 lbb.resetFrame(sf);
       }
    }

   boolean match(BumpThread bt,BumpThreadStack stk,BumpStackFrame frm) {
      if (bt != base_thread) return false;
      if (bubble_type != BubbleType.EXEC && bubble_type != BubbleType.USER) return false;
      int lvl = -1;
      for (int i = 0; i < stk.getNumFrames(); ++i) {
         if (stk.getFrame(i) == frm) {
            lvl = i;
            break;
          }
         else if (frm != null && stk.getFrame(i) != null && frm.getId() == stk.getFrame(i).getId()) {
            lvl = i;
            break;
         }
       }
      if (lvl != frame_level || stk.getNumFrames() != stack_depth &&
               bubble_type == BubbleType.EXEC)
         return false;
      return matchFrameMethod(frm,for_frame);
    }

   boolean matchUser(BumpThread bt,BumpThreadStack stk,BumpStackFrame frm) {
      if (base_thread != null) return false;
      if (bubble_type != BubbleType.USER) return false;
      if (for_bubble.getContentType() != BudaContentNameType.METHOD) return false;
      String s1 = for_bubble.getContentName();
      if (s1 == null) return false;
      int idx1 = s1.indexOf("(");
      String s1a = s1.substring(0,idx1);
      String s1b = s1.substring(idx1);
      if (!sameMethod(s1a,frm.getMethod())) return false;
      if (!BumpLocation.compareParameters(frm.getSignature(),s1b)) return false;

      int lvl = -1;
      for (int i = 0; i < stk.getNumFrames(); ++i) {
	 if (stk.getFrame(i) == frm) {
	    lvl = i;
	    break;
	  }
       }
      // take over user bubble here
      for_stack = stk;
      for_frame = frm;
      base_thread = stk.getThread();
      frame_level = lvl;
      return true;
    }

   boolean isBelow(BubbleData bd) {
      if (base_thread != bd.base_thread) return false;
      if (for_stack.getNumFrames() < bd.for_stack.getNumFrames()) return false;
      return true;
    }

   int aboveLevel(BumpThread bt,BumpThreadStack stk,BumpStackFrame frm) {
      if (base_thread != bt || frame_level < 0) return -1;

      int ct0 = for_stack.getNumFrames();
      int ct1 = stk.getNumFrames();
      for (int i = ct0-1; i >= frame_level; --i) {
	 int j = ct1 - (ct0 - i);
	 BumpStackFrame bsf0 = for_stack.getFrame(i);
	 BumpStackFrame bsf1 = stk.getFrame(j);
	 if (bsf1 == frm) return -1;
	 if (!matchFrameMethod(bsf0,bsf1)) return -1;
       }
      return ct0 - frame_level;
    }

}	// end of inner class BubbleData



}	// end of class BddtBubbleManager




/* end of BddtBreakpointBubble.java */

