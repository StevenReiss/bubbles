/********************************************************************************/
/*										*/
/*		BussConstants.java						*/
/*										*/
/*	BUbble Stack Strategies constant definitions				*/
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


package edu.brown.cs.bubbles.buss;


import edu.brown.cs.bubbles.board.BoardFont;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.bump.BumpLocation;

import java.awt.Component;
import java.awt.Font;
import java.util.Collection;
import java.util.EventListener;



/**
 *	Constants and interfaces for using bubble stacks.
 **/

public interface BussConstants {


/********************************************************************************/
/*										*/
/*	Drawing constants							*/
/*										*/
/********************************************************************************/

/**
 *	Maximum initial height of a bubble stack.
 **/
int	BUSS_MAXIMUM_HEIGHT = 400;


/**
 *	Minimum initial height of a bubble stack.
 **/
int	BUSS_MINIMUM_HEIGHT = 300;


/**
 *	Indentation in pixels for each tree level.  This is in lieu of using icons
 *	to save screen space.
 **/
int	BUSS_TREE_INDENT = 4;


/**
 *	Color at the top of the bubble stack
 **/
String  BUSS_STACK_TOP_COLOR_PROP = "Buss.StackTopColor";

/**
 *	Color at the bottom of the bubble stack
 **/
String  BUSS_STACK_BOTTOM_COLOR_PROP = "Buss.StackBottomColor";

/**
 *	Color for package labels in component
 **/
String  BUSS_PACKAGE_LABEL_COLOR_PROP = "Buss.PackageLabelColor";

/**
 *	Color for class labels in components
 **/
String  BUSS_CLASS_LABEL_COLOR_PROP = "Buss.ClassLabelColor";


/**
 *	Font to be used in the bubble stack
 **/
Font	BUSS_STACK_FONT = BoardFont.getFont(Font.MONOSPACED,Font.PLAIN,10);
String	BUSS_STACK_FONT_PROP = "Buss.stack.font";



/********************************************************************************/
/*										*/
/*	Callback to handle stack defintiions					*/
/*										*/
/********************************************************************************/

/**
 *	This interface represents what needs to be implemented to represent a
 *	component of the bubble stack.	Components need to know how to display
 *	themselves either compact (unselected) or expanded (selected).	They can
 *	optionally provide a hover view.  Components also need to know how to
 *	create a new bubble when chosen.
 **/

interface BussEntry {

/**
 *	Return the fully qualified name of the entry.  Dots (.) are used to separate
 *	name components which are then used to build a display tree and to order the
 *	elements of the bubble stack.
 **/
   String getEntryName();		// . separated name

/**
 *	Return the component to be used as a compact display.  If this returns null,
 *	then getCompactText should return a string and a corresponding label will
 *	be created.
 **/
   Component getCompactComponent();



/**
 *	Return the component to be used as an exapnded display.  If this returns null,
 *	then getExpandText should return a string and a corresponding label will be
 *	created.
 **/
   Component getExpandComponent();

/**
 *	Return the text to be used for an expanded display.  This is only called if
 *	getExpandComponent returns null.
 **/
   String getExpandText();

/**
 *	Create and return a new bubble for this entry.
 **/
   BudaBubble getBubble();

/**
 *      Return the base set of locations (can be null)
 **/
   Collection<BumpLocation> getLocations();
   
/**
 *	Dispose of the bubble if necessary & clean up in general
 **/
   void dispose();

}	// end of inner interface BussEntry



/********************************************************************************/
/*										*/
/*	Tree nodes								*/
/*										*/
/********************************************************************************/

/**
 *	This interface represents a generic tree node internally to Buss.
 **/

interface BussTreeNode {

   BussEntry getEntry();

}	// end of inner interface BussTreeNode



/********************************************************************************/
/*                                                                              */
/*      Callbacks for BUSS events                                               */
/*                                                                              */
/********************************************************************************/

interface BussListener extends EventListener {
   
   void entrySelected(BussEntry e);
   void entryExpanded(BussEntry e);
   void entryHovered(BussEntry e);
   
}

}	 // end in interface BussConstants




/* end of BussConstants.java */
