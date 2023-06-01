/********************************************************************************/
/*										*/
/*		BconRegionLocation.java 					*/
/*										*/
/*	Bubbles Environment Context Viewer internal region from bump		*/
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
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.buda.BudaRoot;
import edu.brown.cs.bubbles.bump.BumpLocation;

import javax.swing.text.BadLocationException;
import javax.swing.text.Segment;

import java.awt.Component;



class BconRegionLocation extends BconRegion implements BconConstants
{


private BumpLocation	base_location;
private RegionType	region_type;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

BconRegionLocation(BaleConstants.BaleFileOverview fov,BumpLocation bl)
{
   super(fov);

   base_location = bl;

   switch (bl.getSymbolType()) {
      case UNKNOWN :
      case PACKAGE :
      case LOCAL :
      case MODULE :
      default :
	 region_type = RegionType.REGION_UNKNOWN;
	 break;
      case CLASS :
      case INTERFACE :
      case ENUM :
      case THROWABLE :
	 region_type = RegionType.REGION_CLASS;
	 break;
      case FIELD :
      case ENUM_CONSTANT :
      case GLOBAL :
	 region_type = RegionType.REGION_FIELD;
	 break;
      case FUNCTION :
	 region_type = RegionType.REGION_METHOD;
	 break;
      case CONSTRUCTOR :
	 region_type = RegionType.REGION_CONSTRUCTOR;
	 break;
      case STATIC_INITIALIZER :
      case MAIN_PROGRAM :
      case IMPORT :
      case EXPORT :
      case PROGRAM :
	 region_type = RegionType.REGION_INITIALIZER;
	 break;
    }

   int pos0 = fov.mapOffsetToJava(bl.getOffset());
   int pos1 = fov.mapOffsetToJava(bl.getEndOffset());

   Segment s = new Segment();
   try {
      fov.getText(0,fov.getLength(),s);
      int idx = pos0;
      while (idx >= 0 && idx < s.length() && Character.isWhitespace(s.charAt(idx))) {
	 if (s.charAt(idx) == '\n') {
	    pos0 = idx;
	    break;
	  }
	 --idx;
       }
      idx = pos1;
      while (idx < s.length()) {
	 if (s.charAt(idx) == '\n') {
	    pos1 = idx;
	    break;
	  }
	 ++idx;
       }
      while (idx < s.length() && Character.isWhitespace(s.charAt(idx))) {
	 if (s.charAt(idx) == '\n') pos1 = idx;
	 ++idx;
       }
    }
   catch (BadLocationException e) {
    }

   setPosition(pos0,pos1);
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override RegionType getRegionType()
{
   if (!isValid()) return RegionType.REGION_UNKNOWN;

   return region_type;
}



@Override String getRegionName()
{
   String nm = null;

   if (base_location != null) {
      nm = base_location.getSymbolName();
      String pm = base_location.getParameters();
      if (pm != null) nm += pm;
    }

   return nm;
}



@Override String getShortRegionName()
{
   String nm = null;
   if (base_location != null) {
      nm = base_location.getSymbolName();
      String pm = base_location.getParameters();
      if (pm != null) {
	 if (pm.length() < 8) nm += pm;
	 else nm += "(...)";
       }
    }

   return nm;
}


@Override int getModifiers()
{
   if (base_location == null) return BCON_MODIFIERS_UNDEFINED;

   return base_location.getModifiers();
}



@Override boolean isComment()			{ return base_location == null; }




/********************************************************************************/
/*										*/
/*	Bubble methods								*/
/*										*/
/********************************************************************************/

@Override boolean createBubble(Component src)
{
   BudaBubble bb = makeBubble();
   if (bb == null) return false;

   BudaBubbleArea bba = BudaRoot.findBudaBubbleArea(src);

   if (bba != null) {
      bba.addBubble(bb,src,null,BudaConstants.PLACEMENT_PREFER|BudaConstants.PLACEMENT_LOGICAL|
		       BudaConstants.PLACEMENT_NEW);
    }

   return true;
}




@Override BudaBubble makeBubble()
{
   if (base_location == null) return null;

   BudaBubble bb = null;
   BaleFactory bf = BaleFactory.getFactory();
   switch (base_location.getSymbolType()) {
      case UNKNOWN :
      case LOCAL :
      case PACKAGE :
      case MODULE :
	 break;
      case CLASS :
      case ENUM :
      case INTERFACE :
      case THROWABLE :
	 bb = bf.createClassBubble(base_location.getProject(),
				      base_location.getSymbolName());
	 break;
      case FUNCTION :
      case CONSTRUCTOR :
	 String prms = base_location.getParameters();
	 if (prms == null) prms = "(...)";
	 String mnm = base_location.getSymbolName() + prms;
	 bb = bf.createMethodBubble(base_location.getProject(),mnm);
	 break;
      case STATIC_INITIALIZER :
      case EXPORT :
      case IMPORT :
	 String inm = base_location.getSymbolName();
	 bb = bf.createStaticsBubble(base_location.getProject(),inm,base_location.getFile());
	 break;
      case MAIN_PROGRAM :
      case PROGRAM :
	 inm = base_location.getSymbolName();
	 bb = bf.createMainProgramBubble(base_location.getProject(),inm,base_location.getFile());
	 break;
      case FIELD :
      case GLOBAL :
      case ENUM_CONSTANT :
	 String fnm = base_location.getSymbolName();
	 int idx = fnm.lastIndexOf(".");
	 if (idx > 0) {
	    String cnm = fnm.substring(0,idx);
	    bb = bf.createFieldsBubble(base_location.getProject(),base_location.getFile(),cnm);
	  }
	 break;
      default:
	 break;
    }

   return bb;
}








/********************************************************************************/
/*										*/
/*	Matching methods							*/
/*										*/
/********************************************************************************/

@Override boolean nameMatch(String nm)
{
   if (base_location != null) return base_location.nameMatch(nm);

   return false;
}



/********************************************************************************/
/*										*/
/*	Debugging methods							*/
/*										*/
/********************************************************************************/

@Override public String toString()		{ return getRegionName(); }




}	// end of class BconRegionLocation




/* end of BconRegionLocation.java */
