/********************************************************************************/
/*										*/
/*		BaleBubbleStack.java						*/
/*										*/
/*	Bubble Annotated Language Editor bubble stack for bale editors		*/
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

import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaBubbleLink;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaDefaultPort;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpLocation;
import edu.brown.cs.bubbles.buss.BussBubble;
import edu.brown.cs.bubbles.buss.BussConstants;
import edu.brown.cs.bubbles.buss.BussFactory;

import javax.swing.text.Document;
import javax.swing.text.Position;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


class BaleBubbleStack implements BaleConstants, BudaConstants, BussConstants
{


/********************************************************************************/
/*										*/
/*	Methods to create new bubbles or bubble stack from location set 	*/
/*										*/
/********************************************************************************/

static void createBubbles(Component src,Position p,Point pt,boolean near,
      Collection<BumpLocation> locs,BudaLinkStyle link)
{
   createBubbles(src,p,pt,near,false,locs,link);
}


static void createBubbles(Component src,Position p,Point pt,boolean near,boolean dropsrc,
			     Collection<BumpLocation> locs,BudaLinkStyle link)
{
   if (locs == null) return;
   
   Map<File,Set<Integer>> used = new HashMap<>();
   for (BumpLocation bl : locs) {
      if (bl.getSymbolType() != BumpSymbolType.UNKNOWN) {
         File f = bl.getFile();
         try {
            f = f.getCanonicalFile();
          }
         catch (IOException e) { }
         Set<Integer> offs = used.get(f);
         if (offs == null) {
            offs = new HashSet<>();
            used.put(f,offs);
          }
         offs.add(bl.getOffset());
       }
    }

   // remove duplicate locations
   Map<String,List<BumpLocation>> keys = new HashMap<>();
   for (Iterator<BumpLocation> it = locs.iterator(); it.hasNext(); ) {
      BumpLocation bl = it.next();
      File f = bl.getFile();
      if (f == null || f.getName().endsWith(".jar")) {
	 it.remove();
	 continue;
       }
      if (!f.exists() && !f.getPath().startsWith("/REBUS/")) {
	 it.remove();
	 continue;
       }
      String key = null;
      switch (bl.getSymbolType()) {
	 default :
	    break;
	 case FUNCTION :
	 case CONSTRUCTOR :
	    key = bl.getKey();
	    break;
	 case FIELD :
	 case ENUM_CONSTANT :
	 case GLOBAL :
	    key = bl.getSymbolName();
	    int idx = key.lastIndexOf(".");
	    key = key.substring(0,idx+1) + ".<FIELDS>";
	    break;
	 case STATIC_INITIALIZER :
	    key = bl.getSymbolName();
	    idx = key.lastIndexOf(".");
	    key = key.substring(0,idx+1) + ".<INITIALIZER>";
	    break;
	 case MAIN_PROGRAM :
	    key = bl.getSymbolName();
	    idx = key.lastIndexOf(".");
	    key = key.substring(0,idx+1) + ".<MAIN>";
	    break;
	 case CLASS :
	 case INTERFACE :
	 case ENUM :
	 case THROWABLE :
	 case MODULE :
	    key = bl.getSymbolName();
	    key = key + ".<PREFIX>";
	    break;
	 case UNKNOWN :
            File f1 = f;
            try {
               f1 = f.getCanonicalFile();
             }
            catch (IOException e) { }
            Set<Integer> offs = used.get(f1);
            if (offs != null) {
               int off = bl.getOffset();
               if (offs.contains(off)) {
                  it.remove();
                  continue;
                }
             }
	    key = f.getPath() + ".<FILE>";
	    break;
       }
      if (key != null) {
	 List<BumpLocation> lbl = keys.get(key);
	 if (lbl != null) it.remove();
	 else {
	    lbl = new ArrayList<BumpLocation>();
	    keys.put(key,lbl);
	  }
	 lbl.add(bl);
       }
    }

   if (locs.size() == 2 && dropsrc && src != null) {
      BudaBubble bbl = BudaRoot.findBudaBubble(src);
      if (bbl != null) {
	 File bfl = bbl.getContentFile();
	 Document bbd = bbl.getContentDocument();
	 if (bfl != null && bbd instanceof BaleDocument) {
	    BaleDocument bd = (BaleDocument) bbd;
	    int off0 = bd.mapOffsetToEclipse(0);
	    int off1 = bd.mapOffsetToEclipse(bd.getLength()-1);
	    for (Iterator<BumpLocation> it = locs.iterator(); it.hasNext(); ) {
	       BumpLocation bloc = it.next();
	       if (bloc.getFile().equals(bfl)) {
		  int diff = Math.abs(bloc.getDefinitionOffset() - off0) +
		  Math.abs(bloc.getDefinitionEndOffset() - off1);
		  if (diff < 4) {
		     it.remove();
		     break;
		   }
		}
	     }
	  }
       }
    }

   if (locs.size() > 1) {
      BaleBubbleStack bs = new BaleBubbleStack(src,p,pt,near,link,keys);
      bs.setupStack(link);
      return;
    }

   for (BumpLocation bl : locs) {
      BudaBubble bb = createBubble(src,p,pt,near,bl,true,link);
      if (bb == null) continue;
      Component bepc = bb.getContentPane();
      if (bepc instanceof BaleFragmentEditor) {
	BaleFragmentEditor bfe = (BaleFragmentEditor) bepc;
	BaleEditorPane bep = bfe.getEditor();
	int locstart = bl.getDefinitionOffset();
	BaleDocument bd = bep.getBaleDocument();
	int bstart = bd.mapOffsetToJava(locstart);
	if (bstart >= 0) bep.setCaretPosition(bstart);
      }
    }
}



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Component			source_bubble;
private Position			source_position;
private Point				source_point;
private boolean 			place_near;
private int				title_width;
private Map<String,List<BumpLocation>>	location_set;
private BudaLinkStyle			link_style;
private BudaBubbleArea			bubble_area;

private static final int DEFAULT_TITLE_WIDTH = 150;
private static final int DEFAULT_CONTENT_WIDTH = 300;
private static final int MAX_ENTRIES = 40;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private BaleBubbleStack(Component src,Position p,Point pt,boolean near,BudaLinkStyle link,
			   Map<String,List<BumpLocation>> locs)
{
   source_bubble = src;
   source_position = p;
   source_point = pt;
   location_set = locs;
   title_width = 0;
   place_near = near;
   link_style = link;
   bubble_area = BudaRoot.findBudaBubbleArea(src);
}




/********************************************************************************/
/*										*/
/*	Methods to setup the bubble stack					*/
/*										*/
/********************************************************************************/

private void setupStack(BudaLinkStyle link)
{
   List<BussEntry> entries = new ArrayList<>();

   title_width = 0;
   int contentwidth = Integer.MIN_VALUE;

   title_width = DEFAULT_TITLE_WIDTH;

   for (List<BumpLocation> locs : location_set.values()) {
      BumpLocation loc0 = locs.get(0);
      if (entries.size() > MAX_ENTRIES) continue;
      switch (loc0.getSymbolType()) {
	 case FUNCTION :
	 case CONSTRUCTOR :
	    MethodStackEntry se = new MethodStackEntry(locs);
	    entries.add(se);
	    break;
	 case FIELD :
	 case ENUM_CONSTANT :
	 case GLOBAL :
	    FieldStackEntry fe = new FieldStackEntry(locs);
	    entries.add(fe);
	    break;
	 case STATIC_INITIALIZER :
	    InitializerStackEntry ie = new InitializerStackEntry(locs);
	    entries.add(ie);
	    break;
	 case MAIN_PROGRAM :
	    MainProgramStackEntry me = new MainProgramStackEntry(locs);
	    entries.add(me);
	    break;
	 case CLASS :
	 case INTERFACE :
	 case ENUM :
	 case THROWABLE :
	 case MODULE :
	    TypeStackEntry te = new TypeStackEntry(locs);
	    entries.add(te);
	    break;
	 case UNKNOWN :
	    FileStackEntry fileent = new FileStackEntry(locs);
	    entries.add(fileent);
	    break;
	 default :
	    createBubble(source_bubble,source_position,source_point,false,loc0,true,link);
	    break;
       }
    }

   contentwidth = DEFAULT_CONTENT_WIDTH;
   for(BussEntry entry : entries){
      BaleCompactFragment component = (BaleCompactFragment) entry.getCompactComponent();
      component.init(contentwidth);
    }

   if (entries.size() == 0) return;
   BussFactory bussf = BussFactory.getFactory();
   BussBubble bb = bussf.createBubbleStack(entries, contentwidth + title_width);
   bb.setLinkStyle(link_style);

   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(source_bubble);
   if (bba == null) return;
   int place = PLACEMENT_PREFER|PLACEMENT_MOVETO|PLACEMENT_NEW;
   if (place_near) place |= PLACEMENT_GROUPED;
   bba.addBubble(bb,source_bubble,source_point,place);

   if (source_bubble != null && source_position != null) {
      BudaConstants.LinkPort p0 = new BaleLinePort(source_bubble,source_position,null);
      BudaConstants.LinkPort p1 = new BudaDefaultPort(BudaPortPosition.BORDER_EW_TOP,true);
      BudaBubble obbl = BudaRoot.findBudaBubble(source_bubble);
      if (obbl != null) {
	 BudaBubbleLink lnk = new BudaBubbleLink(obbl,p0,bb,p1,true,link_style);
	 bba.addLink(lnk);
	 bb.setSourceBubbleInfomation(obbl, p0);
       }
    }

   // BudaRoot.addBubbleViewCallback(new EditorBubbleCallback(bb));
}




/********************************************************************************/
/*										*/
/*	Methods to create a bubble for a location				*/
/*										*/
/********************************************************************************/

private static BudaBubble createBubble(Component src,Position p,Point pt,boolean near,
					  BumpLocation bl,boolean add,BudaLinkStyle link)
{
   if (link == BudaLinkStyle.NONE || link == null)
      return BaleFactory.getFactory().createLocationEditorBubble(src,p,pt,near,bl,false,add,true);
   else
      return BaleFactory.getFactory().createLocationEditorBubble(src,p,pt,near,bl,true,add,true);
}




/********************************************************************************/
/*										*/
/*	Stack entry representation						*/
/*										*/
/********************************************************************************/

private abstract class GenericStackEntry implements BussEntry {

   protected List<BumpLocation> entry_locations;
   protected BumpLocation def_location;
   protected BaleFragmentEditor full_fragment;
   protected BaleCompactFragment compact_fragment;
   protected BudaBubble item_bubble;

   GenericStackEntry(List<BumpLocation> locs) {
      entry_locations = new ArrayList<BumpLocation>(locs);
      def_location = locs.get(0);
      full_fragment = null;
      BaleFactory bf = BaleFactory.getFactory();
      BaleDocumentIde bd = bf.getDocument(def_location.getSymbolProject(),def_location.getFile());
      compact_fragment = new BaleCompactFragment(bd,entry_locations,title_width);
    }

   @Override public Component getCompactComponent()	{ return compact_fragment; }

   @Override public Component getExpandComponent() {
      if (full_fragment == null) full_fragment = createFullFragment();
      return full_fragment;
    }
   @Override public String getExpandText()		{ return null; }

   @Override public BudaBubble getBubble() {
      if (item_bubble == null) {
	 def_location.update();
	 Component c = source_bubble;
	 BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(source_bubble);
	 if (bba == null) c = bubble_area;
	 item_bubble = createBubble(c,source_position,source_point,false,def_location,false,BudaLinkStyle.NONE);
       }
      if (item_bubble != null && item_bubble.getContentPane() != null) {
	 item_bubble.getContentPane().repaint();
       }
      return item_bubble;
    }

   @Override abstract public String getEntryName();
   abstract protected BaleFragmentEditor createFullFragment();

   @Override public void dispose() {
      if (item_bubble != null) item_bubble.disposeBubble();
    }

}	// end of inner class GenericMethodEntry




private class MethodStackEntry extends GenericStackEntry {

   MethodStackEntry(List<BumpLocation> locs) {
      super(locs);
    }

   @Override public String getEntryName() {
      String nm = def_location.getSymbolName();
      nm = nm.replace('$','.');
      return nm;
    }

   @Override protected BaleFragmentEditor createFullFragment() {
      BaleFragmentEditor ed = BaleFactory.getFactory().createMethodFragmentEditor(def_location);
      ed.setInitialSize(new Dimension(BALE_STACK_INITIAL_WIDTH,BALE_STACK_INITIAL_HEIGHT));
      return ed;
    }

}	// end of inner class MethodStackEntry




private class FieldStackEntry extends GenericStackEntry {

   private String class_name;

   FieldStackEntry(List<BumpLocation> locs) {
      super(locs);
      String nm = def_location.getSymbolName();
      int idx = nm.lastIndexOf(".");
      class_name = nm.substring(0,idx);
    }

   @Override public String getEntryName() {
      return class_name.replace('$','.') + ".<FIELDS>";
    }

   @Override protected BaleFragmentEditor createFullFragment() {
      BaleFragmentEditor ed = BaleFactory.getFactory().createFieldFragmentEditor(
	 def_location.getSymbolProject(),def_location.getFile(),class_name);
      ed.setInitialSize(new Dimension(BALE_STACK_INITIAL_WIDTH,BALE_STACK_INITIAL_HEIGHT));
      return ed;
    }

}	// end of inner class FieldStackEntry




private class InitializerStackEntry extends GenericStackEntry {

   private String class_name;

   InitializerStackEntry(List<BumpLocation> locs) {
      super(locs);
      String nm = def_location.getSymbolName();
      int idx = nm.lastIndexOf(".");
      class_name = nm.substring(0,idx);
    }

   @Override public String getEntryName() {
      return class_name.replace('$','.') + ".<STATICS>";
    }

   @Override protected BaleFragmentEditor createFullFragment() {
      BaleFragmentEditor ed = BaleFactory.getFactory().createStaticsFragmentEditor(
	 def_location.getSymbolProject(),class_name,def_location.getFile());
      ed.setInitialSize(new Dimension(BALE_STACK_INITIAL_WIDTH,BALE_STACK_INITIAL_HEIGHT));
      return ed;
    }

}	// end of inner class InitializerStackEntry



private class FileStackEntry extends GenericStackEntry {

   private String file_name;

   FileStackEntry(List<BumpLocation> locs) {
      super(locs);
      file_name = def_location.getFile().getPath();
      int idx = file_name.lastIndexOf(".");
      if (idx > 0) file_name = file_name.substring(0,idx);
    }

   @Override public String getEntryName() {
      return file_name + ".<FILE>";
    }

   @Override protected BaleFragmentEditor createFullFragment() {
      BaleFragmentEditor ed = BaleFactory.getFactory().createFileEditor(
	 def_location.getProject(),def_location.getFile(),null);
      ed.setInitialSize(new Dimension(BALE_STACK_INITIAL_WIDTH,BALE_STACK_INITIAL_HEIGHT));
      return ed;
    }

}	// end of inner class FileStackEntry




private class MainProgramStackEntry extends GenericStackEntry {

   private String class_name;

   MainProgramStackEntry(List<BumpLocation> locs) {
      super(locs);
      String nm = def_location.getSymbolName();
      int idx = nm.lastIndexOf(".");
      class_name = nm.substring(0,idx);
    }

   @Override public String getEntryName() {
      return class_name.replace('$','.') + ".<MAIN>";
    }

   @Override protected BaleFragmentEditor createFullFragment() {
      BaleFragmentEditor ed = BaleFactory.getFactory().createStaticsFragmentEditor(
	    def_location.getSymbolProject(),class_name,def_location.getFile());
      ed.setInitialSize(new Dimension(BALE_STACK_INITIAL_WIDTH,BALE_STACK_INITIAL_HEIGHT));
      return ed;
    }

}	// end of inner class MainProgramStackEntry




private class TypeStackEntry extends GenericStackEntry {

   private String class_name;

   TypeStackEntry(List<BumpLocation> locs) {
      super(locs);
      class_name = def_location.getSymbolName();
    }

   @Override public String getEntryName() {
      return class_name.replace('$','.') + ".<PREFIX>";
    }

   @Override protected BaleFragmentEditor createFullFragment() {
      BaleFragmentEditor ed = BaleFactory.getFactory().createClassPrefixFragmentEditor(
	 def_location.getSymbolProject(),def_location.getFile(),class_name);
      ed.setInitialSize(new Dimension(BALE_STACK_INITIAL_WIDTH,BALE_STACK_INITIAL_HEIGHT));
      return ed;
    }

}	// end of inner class TypeStackEntry




}	// end of class BaleBubbleStack



/* end of BaleBubbleStack.java */

