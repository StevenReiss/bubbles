/********************************************************************************/
/*										*/
/*		BassName.java							*/
/*										*/
/*	Bubble Augmented Search Strategies basic name for search		*/
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


package edu.brown.cs.bubbles.bass;

import edu.brown.cs.bubbles.bass.BassConstants.BassNameType;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaBubbleArea;
import edu.brown.cs.bubbles.buda.BudaBubbleGroup;
import edu.brown.cs.bubbles.bump.BumpLocation;

import java.util.Collection;

import javax.swing.Icon;


/**
 *	This interface represents a searchable name.  It needs to be implemented by
 *	any of the repositories that are searchable.  Names provide all the necessary
 *	information for displaying as part of a tree and for creating bubbles when
 *	they are selected.
 **/

public interface BassName {

/**
 *	Create a bubble corresponding to this name.  This might be a code bubble, a
 *	documentation bubble, or something else.
 **/
BudaBubble createBubble();

/**
 *      Create a group of bubbles for this name.  This is an alternative to creating
 *      a single bubble using createBubble().  One of these two routines should return
 *      null.
 **/

default BudaBubbleGroup createBubbleGroup(BudaBubbleArea bba)    { return null; }     




/**
 *	Create a preview bubble corresponding to this name.  This might be a code bubble, a
 *	documentation bubble, or something else.
 **/
BudaBubble createPreviewBubble();


/**
 *	Create the string to display for preview purposes.
 **/
String createPreviewString();



/**
 *	Return the project (if any) associated with the name.  Returning null implies
 *	there is no associated project.
 **/
String getProject();


/**
 *      Returns the project to display.  This is generally the project, but might differ
 *      if the project includes mutltiple top-level source directories
 **/

default String getSubProject()
{
   return null;
}


/**
 *	Return the package associated with this name.
 **/
String getPackageName();


/**
 *	Return the class associated with this name.
 **/
String getClassName();


/**
 *	Return the local name for this name.
 **/
String getName();


/**
 *	Return the fully qualified name.
 **/
String getFullName();


/**
 *	Return the fully qualified name and include the parameter types if the name is
 *	a method or constructor.
 **/
String getNameWithParameters();


/**
 *	Return the components comprising the name.  This is used to create name trees.	For
 *	example for a.b.c.field, this would return { "a", "b", "c", "field" }.
 **/
String [] getNameComponents();


/**
 *	Return the symbol type of this name.
 **/
BassNameType getNameType();


/**
 *	Return the Java modifiers associated with this name.
 **/
int getModifiers();


/**
 *	Return the sort priority.  Lower values are inserted before higher ones
 **/
int getSortPriority();


/**
 *	Return the icon if any to be displayed with this name.	This can return null
 *	if the name should be displayed without an icon.
 **/
Icon getDisplayIcon();


/**
 *	Return the text to be displayed in the tree for this name.
 **/
String getDisplayName();


/**
 *	Return the bump location associated with the name if there is one
 **/
BumpLocation getLocation();



/**
 *      Return the set of locations associated with the name if there is more than one
 **/
Collection<BumpLocation> getLocations();

/**
 *	Return the prefix for the name.
 **/
String getNameHead();


}	// end of interface BassName




/* end of BassName.java */
