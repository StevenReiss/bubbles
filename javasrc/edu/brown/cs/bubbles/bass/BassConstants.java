/********************************************************************************/
/*										*/
/*		BassConstants.java						*/
/*										*/
/*	Bubble Augmented Search Strategies constant definitions 		*/
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


package edu.brown.cs.bubbles.bass;

import edu.brown.cs.bubbles.board.BoardFont;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaConstants;
import edu.brown.cs.bubbles.bump.BumpConstants;

import javax.swing.Icon;
import javax.swing.JPopupMenu;

import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.EventListener;
import java.util.Stack;



/**
 *	Define the constants, interfaces, etc. associated with search bubbles
 *	of various forms.
 **/

public interface BassConstants extends BumpConstants, BudaConstants {


/**
 * bass properties
 */
static final BoardProperties bass_properties = BoardProperties.getProperties("Bass");


/********************************************************************************/
/*										*/
/*	Types of leaf nodes							*/
/*										*/
/********************************************************************************/

/**
 *	This enumeration lists the types of names that symbols can represent.
 *	STATICS refers to static initializers (python evaluations);
 *	HEADER refers to a class header (i.e. the imports, package, etc.
 *	information).
 **/

enum BassNameType {
   NONE,
   PROJECT,
   PACKAGE,
   METHOD,
   CONSTRUCTOR,
   FIELDS,  
   VARIABLES,                   // JS and Python variable declarations
   STATICS,
   MAIN_PROGRAM,
   CLASS,
   ENUM,
   INTERFACE,
   THROWABLE,
   HEADER,
   FILE,
   OTHER_CLASS,
   LAUNCH_CONFIGURATION,
   DEBUG_PROCESS,
   CHAT_BUDDY,
   CHAT_LOGIN,
   MODULE,
   IMPORTS,
   EXPORTS,
   CODE,
   ANNOTATION,
   UML,
   VIRTUAL_FILE,
   VIRTUAL_CODE,
   OTHER
}



/********************************************************************************/
/*										*/
/*	Types of branch nodes							*/
/*										*/
/********************************************************************************/

/***
 * This enumeration lists the types of branch nodes, which only effects
 * the displaying icon of this branch node.
 **/

enum BranchNodeType {
   NONE,
   PROJECT,
   PACKAGE,
   CLASS,
   INTERFACE,
   ENUM,
   THROWABLE,
   METHOD,
   ANNOTATION
}


/********************************************************************************/
/*										*/
/*	Control constants							*/
/*										*/
/********************************************************************************/

/**
 *	Maximum number of leafs for expand all.  If the number of leaf nodes is
 *	less than this, then the search box will automatically expand all tree
 *	nodes so that all leaves are visible.
 **/
int MAX_LEAF_FOR_EXPANDALL = 48;
String MAX_LEAF_FOR_EXPANDALL_PROP = "Bass.max.leaf.for.expand.all";


/**
 *	When doing auto expansion based on user input, this number specifies the
 *	maximum number of leaves that can be present.  If there are more leaves
 *	than this, automatic expansion on type-in is ignored.  This number is
 *	chosen both for practicality purposes and for efficiency considerations.
 **/
int MAX_LEAF_FOR_AUTO_EXPAND = 128;
String MAX_LEAF_FOR_AUTO_EXPAND_PROP = "Bass.max.leaf.for.auto.expand";


/**
 *	If fewer than this number of items, expand one level
 **/
int MIN_LEAF_FOR_AUTO_EXPAND = 10;
String MIN_LEAF_FOR_AUTO_EXPAND_PROP = "Bass.min.leaf.for.auto.expand";



/**
 *	Minimum number of keystrokes before auto expand all
 **/

int KEYSTROKES_FOR_AUTO_EXPAND = 3;
String KEYSTROKES_FOR_AUTO_EXPAND_PROP = "Bass.keystrokes.for.auto.expand";


/**
 *	Number of pixels that each tree level is indented.  This is used in place
 *	of icons to save screen real estate.
 **/
int INDENT_AMOUNT = 4;



/**
 *	Color at the top of a search bubble.
 **/
String	BASS_PANEL_TOP_COLOR_PROP = "Bass.panel.top.color";


/**
 *	Color at the bottom of a search bubble.
 **/
String	BASS_PANEL_BOTTOM_COLOR_PROP = "Bass.panel.bottom.color";

/**
 *	Color for selection background
 **/
String	BASS_PANEL_SELECT_BACKGROUND_PROP = "Bass.panel.select.background";


/**
 *	Font used in search bubbles.
 **/
Font	BASS_TEXT_FONT = BoardFont.getFont(Font.MONOSPACED,Font.PLAIN,10);
String BASS_TEXT_FONT_PROP = "Bass.font";


/**
 *	name for the buddy list
 */

String BASS_BUDDY_LIST_NAME = "@people";

/**
 *	Name for the document list
 */
String BASS_DOC_LIST_NAME = "@docs";

/**
 *	Name for the document list
 */
String BASS_CONFIG_LIST_NAME = "@Launch Configurations";

/**
 *	Name for the document list
 */
String BASS_PROCESS_LIST_NAME = "@Processes";

/**
 *	Name for the courses lsit
 */

String BASS_COURSE_LIST_NAME = "@Courses";

/**
 *	Determines whether on startup in a new workspace the package explorer comes up
 */
String BASS_PACK_ON_START_NAME = "Bass.pack.onstart";

/**
 *	Determines whether the hover bubble pops up when you hover over a name in the package explorer
 */
String BASS_HOVER_OPTION_NAME = "Bass.doeshover";

/**
 *	Determines whether the package explorer is shown in the overview or not
 */
String BASS_PACK_IN_OVERVIEW = "Bass.shown.in.overview";



/**
 *	Max initial height for a search box
 */
int MAX_SEARCH_HEIGHT = 1600;


/**
 *	Max initial width for a search box
 */
int MAX_SEARCH_WIDTH = 300;




/********************************************************************************/
/*										*/
/*	Repository abstraction							*/
/*										*/
/********************************************************************************/

/**
 *	Bass supports multiple repositories, i.e. a name repository and a documentation
 *	repository.  This interface is all that a new repository needs to implement.
 **/

interface BassRepository {

/**
 *	Return the set of names in this repository.
 **/
   Iterable<BassName> getAllNames();


/**
 *	Tell if this repository matches or includes another
 **/
   default boolean includesRepository(BassRepository br) {
      return br == this;
    }

/**
*       Check if repository is empty
**/
   default boolean isEmpty() {
      return false;
    }

}	// end of interface BassRepository


interface BassUpdatableRepository extends BassRepository {

   void reloadRepository();
}


interface BassUpdatingRepository extends BassRepository {

/**
 *	Add a dependent repository for update purposes
 **/
   void addUpdateRepository(BassUpdatableRepository br);

   void removeUpdateRepository(BassUpdatableRepository br);

}	// end of inner interface BassUpdatableRepository




/********************************************************************************/
/*										*/
/*	Constants for use with BassName nodes					*/
/*										*/
/********************************************************************************/

/**
 *	Value representing unknown modifiers for a BassName.
 **/
int	BASS_MODIFIERS_UNDEFINED = -1;



/**
 *	Default sort priority.	Values higher than this come last, values lower come first
 **/
int	BASS_DEFAULT_SORT_PRIORITY = 50;



/**
 *	Default sort priority for interior nodes.
 **/
int	BASS_DEFAULT_INTERIOR_PRIORITY = 200;




/********************************************************************************/
/*										*/
/*	Interfaces for handling popup menu options on a search box		*/
/*										*/
/********************************************************************************/

/**
 * This interface defines a routine for popping up a menu to create a new method or class
 */

public interface BassNewItemCreator {

   /**
    * show the menu for the component and treepath
    */
   public void showMenu(Component c, Stack<String> items, BassName bn, Rectangle rowrect, boolean killparent);
}


/**
 *	This interface allows components to add their own items to popup menus on the
 *	search box or package explorer.
 **/

interface BassPopupHandler extends EventListener {

   public void addButtons(BudaBubble bb,Point where,JPopupMenu menu,String fullname,BassName forname);

}	// end of inner interface BassPopupHandler



/********************************************************************************/
/*										*/
/*	BassFlagger -- callback to get flag information for a name		*/
/*										*/
/********************************************************************************/

interface BassFlagger extends EventListener {

   public BassFlag getFlagForName(BassName bnm,String fullname);

}	// end of interface BassFlagger


interface BassFlag {
   Icon getOverlayIcon();
   int getPriority();
}	// end of interface BassFlag





}	// end of interface BassConstants



/* end of BassConstants.java */
