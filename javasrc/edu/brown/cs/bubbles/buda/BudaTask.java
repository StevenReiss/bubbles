/********************************************************************************/
/*										*/
/*		BudaTask.java							*/
/*										*/
/*	BUblles Display Area task (saved working set)				*/
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


package edu.brown.cs.bubbles.buda;

import edu.brown.cs.bubbles.board.BoardConstants;
import edu.brown.cs.bubbles.board.BoardLog;

import edu.brown.cs.ivy.xml.IvyXml;

import org.w3c.dom.Element;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


class BudaTask implements BudaConstants, BoardConstants {



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String task_name;
private Element task_xml;
private String task_text;
// private Image task_image;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public BudaTask(Element xml)
{
   task_name = IvyXml.getAttrString(xml,"NAME");
   task_xml = xml;
   task_text = null;
}



BudaTask(String nm,String txt)
{
   task_name = nm;
   task_xml = null;
   task_text = txt;
   //task_image = null;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getName()			{ return task_name; }

String getText()
{
   if (task_text == null) task_text = IvyXml.convertXmlToString(task_xml);
   return task_text;
}

Element getXml()
{
   if (task_xml == null) task_xml = IvyXml.convertStringToXml(task_text);
   return task_xml;
}


Date getDate()
{
   Element xml = getXml();
   Element wse = IvyXml.getChild(xml,"WORKINGSET");
   long ctime = IvyXml.getAttrLong(wse,"CREATE",0);

   return new Date(ctime);
}

//Image getImage() { return task_image; }




/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(BudaXmlWriter xw)
{
   xw.writeXml(getXml());
}



/********************************************************************************/
/*										*/
/*	Loading methods 							*/
/*										*/
/********************************************************************************/

BudaWorkingSetImpl loadTask(BudaBubbleArea bba,int offset)
{
   BudaRoot root = BudaRoot.findBudaRoot(bba);
   Rectangle arearect = bba.getBounds();

   Element te = getXml();
   Element wse = IvyXml.getChild(te,"WORKINGSET");
   long ctime = IvyXml.getAttrLong(wse,"CREATE",0);
   WorkingSetGrowth g = IvyXml.getAttrEnum(wse,"GROWTH",WorkingSetGrowth.NONE);

   Color c = IvyXml.getAttrColor(wse,"BORDERCOLOR");
   if (c == null) {
      BoardLog.logE("BUDA","Problem reading working set color " + IvyXml.getAttrString(wse,"BORDERCOLOR"));
    }

   Element rgn = IvyXml.getChild(wse,"REGION");
   int x0 = (int) IvyXml.getAttrDouble(rgn,"X",0);
   Rectangle r = new Rectangle(x0,(int) IvyXml.getAttrDouble(rgn,"Y",0),
				  (int) IvyXml.getAttrDouble(rgn,"WIDTH",0),
				  (int) IvyXml.getAttrDouble(rgn,"HEIGHT",0));
   r.x = offset - r.width/2;
   if (r.x < 0) r.x = 0;
   if (r.x + r.width >= arearect.width) r.x = arearect.width - r.width;
   r.height = arearect.height;
   int dx = r.x - x0;

   BudaWorkingSetImpl ws = bba.defineWorkingSet(task_name,r,g);
   if (ws == null) return null;

   ws.setColor(c);
   if (ctime > 0) ws.setCreateTime(ctime);

   Map<String,BudaBubble> bubblemap = new HashMap<String,BudaBubble>();
   Element bbls = IvyXml.getChild(te,"BUBBLES");
   for (Element bbl : IvyXml.children(bbls,"BUBBLE")) {
      BudaBubble bb = root.createBubble(bba,bbl,null,dx);
      if (bb != null) bubblemap.put(IvyXml.getAttrString(bbl,"ID"),bb);
    }

   Element grps = IvyXml.getChild(te,"GROUPS");
   for (Element egrp : IvyXml.children(grps,"GROUP")) {
      BudaBubbleGroup grp = null;
      for (Element ebbl : IvyXml.children(egrp,"BUBBLE")) {
	 String id = IvyXml.getAttrString(ebbl,"ID");
	 BudaBubble bbl = bubblemap.get(id);
	 if (bbl != null) {
	    grp = bbl.getGroup();
	    if (grp != null) break;
	  }
       }
      if (grp != null) {
	 Color lc = IvyXml.getAttrColor(egrp,"LEFTCOLOR");
	 Color rc = IvyXml.getAttrColor(egrp,"RIGHTCOLOR");
	 grp.setColor(lc,rc);
	 String ttl = IvyXml.getTextElement(egrp,"TITLE");
	 grp.setTitle(ttl);
       }
    }

   Element lnks = IvyXml.getChild(te,"LINKS");
   for (Element lnk : IvyXml.children(lnks,"LINK")) {
      boolean rect = IvyXml.getAttrBool(lnk,"RECT");
      Element flnk = IvyXml.getChild(lnk,"FROM");
      BudaBubble fbbl = bubblemap.get(IvyXml.getAttrString(flnk,"ID"));
      LinkPort fprt = root.createPort(fbbl,IvyXml.getChild(flnk,"PORT"));
      Element tlnk = IvyXml.getChild(lnk,"TO");
      BudaBubble tbbl = bubblemap.get(IvyXml.getAttrString(tlnk,"ID"));
      LinkPort tprt = root.createPort(tbbl,IvyXml.getChild(tlnk,"PORT"));
      BudaLinkStyle sty = IvyXml.getAttrEnum(lnk,"STYLE",BudaLinkStyle.STYLE_SOLID);
      if (fbbl != null && tbbl != null && fprt != null && tprt != null) {
	 BudaBubbleLink blnk = new BudaBubbleLink(fbbl,fprt,tbbl,tprt,rect,sty);
	 root.addLink(blnk);
       }
    }

   return ws;
}

boolean updateTask(BudaWorkingSetImpl ws)
{
   BudaBubbleArea bba = ws.getBubbleArea();
   BudaRoot br = BudaRoot.findBudaRoot(bba);
   Rectangle arearect = ws.getRegion();
   boolean chng = false;

   Element te = getXml();
   Element wse = IvyXml.getChild(te,"WORKINGSET");
   Element rgn = IvyXml.getChild(wse,"REGION");
   int w0 = (int) IvyXml.getAttrDouble(rgn,"WIDTH",0);
   if (w0 != arearect.width) {
      arearect.width = w0;
      ws.setRegion(arearect);
      chng = true;
    }
   if (ws.getLabel() == null || !ws.getLabel().equals(task_name)) {
      if (task_name != null) {
	 ws.setLabel(task_name);
	 chng = true;
       }
    }

   int x0 = (int) IvyXml.getAttrDouble(rgn,"X",0);
   Rectangle r = new Rectangle(arearect.x,(int) IvyXml.getAttrDouble(rgn,"Y",0),
	 (int) IvyXml.getAttrDouble(rgn,"WIDTH",0),arearect.height);
   int dx = r.x - x0;

   Collection<BudaBubble> bbs = bba.getBubblesInRegion(ws.getRegion());
   Element bbls = IvyXml.getChild(te,"BUBBLES");
   Map<String,BudaBubble> bubblemap = setupBubbleMap(br,bbls,bbs);
   for (Element bbl : IvyXml.children(bbls,"BUBBLE")) {
      String id = IvyXml.getAttrString(bbl,"ID");
      BudaBubble bb = bubblemap.get(id);
      if (bb == null) {
	 bb = br.createBubble(bba,bbl,null,dx);
	 if (bb != null) {
	    chng = true;
	    bubblemap.put(id,bb);
	    bba.fixupGroups(bb);
	 }
       }
      else {
	 Rectangle r0 = BudaRoot.findBudaLocation(bb);
	 if (r0 != null) {
	    int x = IvyXml.getAttrInt(bbl,"X") + dx;
	    int y = IvyXml.getAttrInt(bbl,"Y");
	    int w = IvyXml.getAttrInt(bbl,"W");
	    int h = IvyXml.getAttrInt(bbl,"H");
	    if (x != r0.x || y != r0.y || w != r0.width || h != r0.height) {
	       bb.setBounds(x,y,w,h);
	       bba.fixupGroups(bb);
               chng = true;
	     }
	  }
       }
    }
   for (BudaBubble bb : bbs) {
      if (bubblemap.values().contains(bb)) continue;
      if (bb.isFixed() || bb.isFloating() || bb.isTransient()) continue;
      bb.setVisible(false);
    }

   Element grps = IvyXml.getChild(te,"GROUPS");
   for (Element egrp : IvyXml.children(grps,"GROUP")) {
      BudaBubbleGroup grp = null;
      for (Element ebbl : IvyXml.children(egrp,"BUBBLE")) {
	 String id = IvyXml.getAttrString(ebbl,"ID");
	 BudaBubble bbl = bubblemap.get(id);
	 if (bbl != null) {
	    grp = bbl.getGroup();
	    if (grp != null) break;
	  }
       }
      if (grp != null) {
         String gttl = grp.getTitle();
	 String ttl = IvyXml.getTextElement(egrp,"TITLE");
         if (gttl == null && ttl == null) ;
         else if (ttl == null || !ttl.equals(gttl)) {
            grp.setTitle(ttl);
            chng = true;
          }
       }    
    }

   Element lnks = IvyXml.getChild(te,"LINKS");
   for (Element lnk : IvyXml.children(lnks,"LINK")) {
      boolean rect = IvyXml.getAttrBool(lnk,"RECT");
      BudaLinkStyle sty = IvyXml.getAttrEnum(lnk,"STYLE",BudaLinkStyle.STYLE_SOLID);
      Element flnk = IvyXml.getChild(lnk,"FROM");
      Element tlnk = IvyXml.getChild(lnk,"TO");
      BudaBubble fbbl = bubblemap.get(IvyXml.getAttrString(flnk,"ID"));
      BudaBubble tbbl = bubblemap.get(IvyXml.getAttrString(tlnk,"ID"));
      if (fbbl == null || tbbl == null) continue;
      boolean fnd = false;
      for (BudaBubbleLink blnk : bba.getAllLinks()) {
         if (blnk.getSource() == fbbl && blnk.getTarget() == tbbl) {
            fnd = true;
            break;
          }
       }
      if (!fnd) {      
         LinkPort fprt = br.createPort(fbbl,IvyXml.getChild(flnk,"PORT"));
         LinkPort tprt = br.createPort(tbbl,IvyXml.getChild(tlnk,"PORT"));
         if (fbbl != null && tbbl != null && fprt != null && tprt != null) {
            BudaBubbleLink blnk = new BudaBubbleLink(fbbl,fprt,tbbl,tprt,rect,sty);
            br.addLink(blnk);   
            chng = true;
          }
       }
    }

   return chng;
}




private static final int MAX_SCORE = 5;

private Map<String,BudaBubble> setupBubbleMap(BudaRoot br,Element bbls,Collection<BudaBubble> bbs)
{
   Map<String,BudaBubble> bubblemap = new HashMap<>();
   Set<Object> done = new HashSet<>();

   for (int i = MAX_SCORE; i > 0; --i) {
      for (Element bbl : IvyXml.children(bbls,"BUBBLE")) {
	 if (done.contains(bbl)) continue;
	 String key = IvyXml.getAttrString(bbl,"CONFIG");
	 if (key == null) {
	    done.add(bbl);
	    continue;
	  }
	 for (BudaBubble bb : bbs) {
	    if (done.contains(bb)) continue;
	    int s = compare(br,bb,bbl);
	    if (s == i) {
	       String id = IvyXml.getAttrString(bbl,"ID");
	       bubblemap.put(id,bb);
	       done.add(bbl);
	       done.add(bb);
	       break;
	     }
	  }
       }
    }


   return bubblemap;
}



private int compare(BudaRoot br,BudaBubble bb,Element e)
{
   int score = 0;

   String key = IvyXml.getAttrString(e,"CONFIG");
   if (key != null) {
      score += br.matchConfiguration(key,e,bb);
    }

   Rectangle r0 = BudaRoot.findBudaLocation(bb);
   if (r0 != null) {
      int x = IvyXml.getAttrInt(e,"X");
      int y = IvyXml.getAttrInt(e,"Y");
      int w = IvyXml.getAttrInt(e,"W");
      int h = IvyXml.getAttrInt(e,"H");
      if (x == r0.x && y == r0.y) ++score;
      if (w == r0.width) ++score;
      if (h == r0.height) ++score;
    }

   return score;
}



/********************************************************************************/
/*										*/
/*	String methods for menu display 					*/
/*										*/
/********************************************************************************/

@Override public String toString()
{
   StringBuffer buf = new StringBuffer();
   buf.append(task_name);

   return buf.toString();
}




}	// end of class BudaTask




/* end of BudaTask.java */
