/********************************************************************************/
/*										*/
/*		BconConstants.java						*/
/*										*/
/*	Bubbles Environment Context Viewer constants				*/
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

import edu.brown.cs.bubbles.banal.BanalConstants;

import javax.swing.JComponent;

import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Set;


public interface BconConstants {


/********************************************************************************/
/*										*/
/*	Region types								*/
/*										*/
/********************************************************************************/

/**
 *	Specify the different types of regions that can exist in a file.
 **/

enum RegionType {
   REGION_UNKNOWN,
   REGION_CLASS,
   REGION_METHOD,
   REGION_CONSTRUCTOR,
   REGION_FIELD,
   REGION_INITIALIZER,
   REGION_COMMENT,
   REGION_COPYRIGHT,
   REGION_PACKAGE,
   REGION_IMPORT,
   REGION_CLASS_HEADER
}



/********************************************************************************/
/*										*/
/*	Line Types								*/
/*										*/
/********************************************************************************/

/**
 *	Specify the different types that lines may have.  These are used for
 *	drawing the line appropriately.
 **/

enum LineType {
   LINE_UNKNOWN,
   LINE_EMPTY,
   LINE_COMMENT,
   LINE_CODE
}


/********************************************************************************/
/*										*/
/*	Scaling types								*/
/*										*/
/********************************************************************************/

/**
 *	The different types of scalings that the context viewer provides.  These
 *	affect how different file sizes are shown.
 **/

enum ScaleType {
   SCALE_EXPAND,			// expand to fill region
   SCALE_LINEAR,			// linear sizing
   SCALE_SQRT,				// square root sizing
   SCALE_LOG				// log sizing
}


/********************************************************************************/
/*										*/
/*	Layout types								*/
/*										*/
/********************************************************************************/

/**
 *	The different types of layouts that we support
 **/

enum LayoutType {
   CIRCLE,
   FORCE,
   MAP,
   GENERAL,
   SPRING,
   TREE,
   STACK,
   PSTACK,
   ESTACK,
   PARTITION,
}



/********************************************************************************/
/*										*/
/*	Drawing Constants							*/
/*										*/
/********************************************************************************/

/**
 *	The space between file regions
 **/
double	SEPARATION_SPACE = 5;		// space between file regions


/**
 *	The left and right margin space
 **/
double	LR_MARGIN_SPACE = 3;		// left, right margins


/**
 *	The top and bottom margin space
 **/

double	TB_MARGIN_SPACE = 3;		// top, bottom margins


/**
 *	The space (X) reserved for the class/file names
 **/

double	NAME_SPACE = 12;		// space for class name




/**
 *	The color for drawing code lines.
 **/
String	BCON_CODE_COLOR_PROP = "Bcon.code.color";


/**
 *	The color for drawing comment lines.
 **/
String	BCON_COMMENT_COLOR_PROP = "Bcon.comment.color";


/**
 *	The color for the rectangle outlining the region whose bubble has the focus.
 **/
String	BCON_FOCUS_COLOR_PROP = "Bcon.focus.color";


/**
 *	The color for drawing a rectangle around a region for which there is a bubble.
 **/
String	BCON_EXISTS_COLOR_PROP = "Bcon.exists.color";


/**
 *	The color for drawing a rectangle around a region where there is no current bubble.
 **/
String	BCON_NOTEXISTS_COLOR_PROP = "Bcon.notexists.color";


/**
 *	The color for labeling a file which doesn't have the focus.
 **/
String  LABEL_COLOR_PROP = "Bcon.LabelColor";


/**
 *	The color for labeling a file that does have the focus.
 **/
String  LABEL_FOCUS_COLOR_PROP = "Bcon.LabelFocusColor";



/**
 *	Color for classes
 **/

String	BCON_CLASS_CLASS_COLOR_PROP = "Bcon.class.class.color";


/**
 *	Color for methods
 **/

String	BCON_CLASS_METHOD_COLOR_PROP = "Bcon.class.method.color";


/**
 *	Color for fields
 **/

String	BCON_CLASS_FIELD_COLOR_PROP = "Bcon.class.field.color";


/**
 *	Color for static initializers
 **/

String	BCON_CLASS_INITIALIZER_COLOR_PROP = "Bcon.class.initializer.color";


/**
 *	Color for comments
 **/

String	BCON_CLASS_COMMENT_COLOR_PROP = "Bcon.class.comment.color";


/**
 *      Color for imports
 **/

String  BCON_CLASS_IMPORT_COLOR_PROP = "Bcon.class.import.color";



/**
 *	Color for header fileds
 **/

String	BCON_CLASS_HEADER_COLOR_PROP = "Bcon.class.header.color";



/**
 *	Font for class panel
 **/

String	BCON_CLASS_FONT_SIZE = "Bcon.class.font.size";



/********************************************************************************/
/*										*/
/*	Bubble constants							*/
/*										*/
/********************************************************************************/

/**
 *	The name of the context overview button in the root menu.
 **/

String BCON_BUTTON = "Bubble.Create File Overview";



/********************************************************************************/
/*										*/
/*	Generic panel type							*/
/*										*/
/********************************************************************************/

interface BconPanel {

   void dispose();
   JComponent getComponent();
   void handlePopupMenu(MouseEvent e);

}	// end of interface BconPanel




/********************************************************************************/
/*										*/
/*	Access constants							*/
/*										*/
/********************************************************************************/

/**
 *	Return from getModifiers() for a symbol without modifiers (e.g. comment)
 **/

int BCON_MODIFIERS_UNDEFINED = -1;




/********************************************************************************/
/*										*/
/*	Tokenizer constants and interfaces					*/
/*										*/
/********************************************************************************/

enum BconTokenType {
   EOL, 			// end of line
   BLOCK_CMMT,			// /* ... */ comment; may only be part of a comment if split
   DOC_CMMT,			// /** ... comment for documentation
   LINE_CMMT,			// // comment
   OTHER,			// any other token
}





interface BconToken {
   int getStart();
   int getLength();
   BconTokenType getTokenType();
}



/********************************************************************************/
/*										*/
/*	Package graph definitions						*/
/*										*/
/********************************************************************************/

enum NodeType {
   METHOD,
   CLASS,
   INTERFACE,
   ENUM,
   ANNOTATION,
   THROWABLE,
   PACKAGE,
}



enum ArcType {
   NONE,			// no relation
   SUBCLASS,			// X is a subclass of Y
   IMPLEMENTED_BY,		// X is an instance of Y
   EXTENDED_BY, 		// X is an instance of Y
   INNERCLASS,			// X contains inner class Y
   ALLOCATES,			// allocates an instance of
   CALLS,			// calls a method in
   CATCHES,			// catches an exeception of
   ACCESSES,			// accesses a field of
   WRITES,			// writes a field of
   CONSTANT,			// access a constant from (enum or final static)
   FIELD,			// has a field of
   LOCAL,			// has a local of
   PACKAGE,			// in package
   MEMBER_OF			// is a member of
}



interface BconRelationData {

   ArcType getPrimaryRelationship();
   int getRelationshipCount();
}	// end of interface BconRelationData





interface BconGraphNode {

   String getFullName();
   String getLabelName();

   NodeType getNodeType();
   boolean isInnerClass();
   boolean isSubclass();
   Set<BanalConstants.ClassType> getClassType();
   boolean getIncludeChildren();
   void setIncludeChildren(boolean fg);

   boolean hasChildren();
   Set<BconGraphNode> getChildren();
   BconGraphNode getParent();

   int getArcCount();
   Collection<BconGraphArc> getOutArcs();
}



interface BconGraphArc {

   BconRelationData getRelationTypes();

   boolean isInnerArc();

   BconGraphNode getFromNode();
   BconGraphNode getToNode();
   boolean useSourceArrow();
   boolean useTargetArrow();

   String getLabel();

   void update();
}



}	// end of interface BconConstants



/* end of BconConstants.java */

