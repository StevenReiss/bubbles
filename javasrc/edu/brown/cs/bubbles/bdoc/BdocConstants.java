/********************************************************************************/
/*										*/
/*		BdocConstants.java						*/
/*										*/
/*	Bubbles Environment Documentation bubbles constants			*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.bdoc;


import edu.brown.cs.bubbles.bass.BassConstants;
import edu.brown.cs.bubbles.board.BoardFont;

import java.awt.Font;
import java.net.URI;




public interface BdocConstants
{

/********************************************************************************/
/*										*/
/*	Enumerations for representing javadoc					*/
/*										*/
/********************************************************************************/

/**
 *	The different types of items that can occur inside javadoc elements.
 **/

enum ItemRelation {
   NONE,
   PACKAGE_INTERFACE,
   PACKAGE_CLASS,
   PACKAGE_EXCEPTION,
   PACKAGE_ENUM,
   PACKAGE_ERROR,
   SUPERTYPE,
   TYPE_PARAMETERS,
   IMPLEMENTS,
   NESTED_CLASS,
   FIELD,
   CONSTRUCTOR,
   METHOD,
   INHERITED_CLASS,
   INHERITED_FIELD,
   INHERITED_METHOD,
   PARAMETER,
   RETURN,
   THROW,
   OVERRIDE,
   SPECIFIED_BY,
   SEE_ALSO,
   SUBINTERFACES,
   IMPLEMENTING_CLASS,
   SUBCLASS
}



/********************************************************************************/
/*										*/
/*	Subitem interface							*/
/*										*/
/********************************************************************************/

/**
 *	The interface defining a item contained in the current JavaDoc item.  The
 *	various methods provide access to information about the subitem.
 **/

interface SubItem {

/**
 *	Return the name to display for the item
 **/
   String getName();


/**
 *	Return the URL that can be used to access the item.
 **/
   URI getItemUrl();


/**
 *	Return the URL relative to the enclosing item
 **/
   String getRelativeUrl();

/**
 *	Return a brief description of the item for display purposes.
 **/
   String getDescription();

}	// end of inner interface SubItem




/********************************************************************************/
/*										*/
/*	Colors and fonts for display						*/
/*										*/
/********************************************************************************/

/**
 *	Maximum initial height of a documentation bubble
 **/
int BUBBLE_HEIGHT = 350;


/**
 *	Default width of the description part of a documentation bubble
 **/
int DESCRIPTION_WIDTH = 400;


/**
 *	Default height of the description part of a documentation bubble
 **/
int DESCRIPTION_HEIGHT = 300;


/**
 *	Maximum height to automatically expand to on a tree expansion
 **/
int MAX_EXPAND_HEIGHT = 350;


/**
 *	Font for the title line containing the name of the element
 **/
Font CONTEXT_FONT = BoardFont.getFont(Font.SERIF,Font.BOLD,13);


/**
 *	Color of the title line containing the name of the element
 **/
String CONTEXT_COLOR_PROP = "Bdoc.ContextColor";


/**
 *	Font for the primary name of the item
 **/
Font NAME_FONT = BoardFont.getFont(Font.SERIF,Font.PLAIN,12);


/**
 *	Color for the primary name of the item.
 **/
String NAME_COLOR_PROP = "Bdoc.NameColor";


/**
 *	Font for the body of a panel option.
 **/
Font OPTION_FONT = BoardFont.getFont(Font.SERIF,Font.BOLD,10);



/**
 *	Font for the name of a panel option
 **/
Font ITEM_NAME_FONT = BoardFont.getFont(Font.SERIF,Font.BOLD,10);


/**
 *	Font for the description of a panel option
 **/
Font ITEM_DESC_FONT = BoardFont.getFont(Font.SERIF,Font.PLAIN,12);


/**
 *	Color at the top of the documentation panel.
 **/
String BDOC_TOP_COLOR_PROP = "Bdoc.TopColor";


/**
 *	Color at the bottom of the documentation panel.
 **/
String BDOC_BOTTOM_COLOR_PROP = "Bdoc.BottomColor";

/**
 *  Prefix of docs shown in search box
 */
String BDOC_DOC_PREFIX = "zzzz#@docs.";

/**
 *  Priority calculating constant for docs
 */
int BDOC_DOC_PRIORITY = BassConstants.BASS_DEFAULT_SORT_PRIORITY + 50;
int BDOC_DOC_PRIORITY_INHERIT = BDOC_DOC_PRIORITY + 1;


}	// end of interface BdocConstants




/* end of BdocConstants.java */




